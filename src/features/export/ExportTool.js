/**
 * @module ExportTool
 * @description Provides map screenshot/export functionality.
 * Uses html2canvas for PNG capture. Loads the library from CDN on demand
 * (no npm required).
 *
 * NOTE: `allowTaint` is intentionally omitted because it contradicts
 * `useCORS`. When both are set, tainted canvases bypass CORS and can
 * cause `toBlob()` / `toDataURL()` to throw SecurityError.
 */
export class ExportTool {
  /**
   * Create an ExportTool.
   * @param {L.Map} map - The Leaflet map instance.
   */
  constructor(map) {
    this.map = map;
    this._html2canvasLoaded = false;
  }

  /**
   * Capture the current map view as a PNG image and trigger download.
   * @param {object} [options] - Export options.
   * @param {string} [options.filename] - Download filename.
   * @param {number} [options.scale] - Scale multiplier (1 = screen resolution).
   */
  async exportPNG(options = {}) {
    const filename = options.filename || `jsmars_map_${Date.now()}.png`;
    const scale = options.scale || 1;

    await this._ensureHtml2Canvas();

    const mapEl = this.map.getContainer();
    try {
      const canvas = await html2canvas(mapEl, {
        useCORS: true,
        scale: scale,
        backgroundColor: '#000',
        logging: false,
        // Ignore Leaflet controls
        ignoreElements: (el) => {
          return el.classList?.contains('leaflet-control-container');
        }
      });

      // Add attribution text
      this._addAttribution(canvas);

      canvas.toBlob(blob => {
        if (blob) {
          const url = URL.createObjectURL(blob);
          const a = document.createElement('a');
          a.href = url;
          a.download = filename;
          a.click();
          URL.revokeObjectURL(url);
        }
      }, 'image/png');
    } catch (err) {
      console.error('Export PNG failed:', err);
      alert('Export failed. This may be due to CORS restrictions on tile layers.');
    }
  }

  /**
   * Capture the current map view as a JPEG image.
   * @param {object} [options] - Export options.
   * @param {string} [options.filename] - Download filename.
   * @param {number} [options.quality] - JPEG quality (0 to 1).
   * @param {number} [options.scale] - Scale multiplier.
   */
  async exportJPEG(options = {}) {
    const filename = options.filename || `jsmars_map_${Date.now()}.jpg`;
    const quality = options.quality || 0.92;

    await this._ensureHtml2Canvas();

    const mapEl = this.map.getContainer();
    try {
      const canvas = await html2canvas(mapEl, {
        useCORS: true,
        scale: options.scale || 1,
        backgroundColor: '#000',
        logging: false
      });

      canvas.toBlob(blob => {
        if (blob) {
          const url = URL.createObjectURL(blob);
          const a = document.createElement('a');
          a.href = url;
          a.download = filename;
          a.click();
          URL.revokeObjectURL(url);
        }
      }, 'image/jpeg', quality);
    } catch (err) {
      console.error('Export JPEG failed:', err);
    }
  }

  /**
   * Generate a world file (.pgw or .jgw) for the current view.
   * This allows the exported image to be georeferenced in GIS software.
   * @param {string} [format='pgw'] - 'pgw' for PNG, 'jgw' for JPEG.
   */
  exportWorldFile(format = 'pgw') {
    const bounds = this.map.getBounds();
    const size = this.map.getSize();

    const content = ExportTool.generateWorldFileContent(
      bounds.getWest(),
      bounds.getEast(),
      bounds.getSouth(),
      bounds.getNorth(),
      size.x,
      size.y
    );

    const blob = new Blob([content], { type: 'text/plain' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `jsmars_map_${Date.now()}.${format}`;
    a.click();
    URL.revokeObjectURL(url);
  }

  /**
   * Generate a standard 6-line GIS World File (.pgw / .tfw / .jgw).
   * @param {number} west
   * @param {number} east
   * @param {number} south
   * @param {number} north
   * @param {number} widthPx
   * @param {number} heightPx
   * @returns {string} 6-line world file string
   */
  static generateWorldFileContent(west, east, south, north, widthPx, heightPx) {
    const pixelWidth = (east - west) / widthPx;
    const pixelHeight = (north - south) / heightPx;

    return [
      pixelWidth.toFixed(10),
      '0.0000000000',
      '0.0000000000',
      (-pixelHeight).toFixed(10),
      (west + pixelWidth / 2).toFixed(10),
      (north - pixelHeight / 2).toFixed(10)
    ].join('\n');
  }

  /**
   * Parse a standard 6-line GIS World File.
   * @param {string} content
   * @returns {{pixelWidth: number, pixelHeight: number, originX: number, originY: number}}
   */
  static parseWorldFileContent(content) {
    const lines = content.trim().split(/\r?\n/).map(Number);
    if (lines.length < 6) return null;

    return {
      pixelWidth: lines[0],
      rotY: lines[1],
      rotX: lines[2],
      pixelHeight: lines[3],
      originX: lines[4],
      originY: lines[5]
    };
  }

  /**
   * Export a publication-quality cartographic map layout with neatline border,
   * coordinates ticks, title header, and scale bar banner.
   * @param {object} [options]
   * @param {string} [options.title] - Map title
   * @param {string} [options.subtitle] - Map subtitle
   * @param {number} [options.scale=2] - High-DPI supersampling scale
   */
  async exportPublicationMap(options = {}) {
    const title = options.title || 'Planetary Map';
    const subtitle = options.subtitle || 'Generated by JSMARS';
    const scale = options.scale || 2;
    const filename = options.filename || `jsmars_publication_${Date.now()}.png`;

    await this._ensureHtml2Canvas();

    const mapEl = this.map.getContainer();
    const rawCanvas = await html2canvas(mapEl, {
      useCORS: true,
      scale: scale,
      backgroundColor: '#050505',
      logging: false,
      ignoreElements: (el) => el.classList?.contains('leaflet-control-container')
    });

    const borderPad = 40 * scale;
    const headerHeight = 50 * scale;
    const footerHeight = 40 * scale;

    const pubCanvas = document.createElement('canvas');
    pubCanvas.width = rawCanvas.width + borderPad * 2;
    pubCanvas.height = rawCanvas.height + borderPad * 2 + headerHeight + footerHeight;
    const ctx = pubCanvas.getContext('2d');

    // Canvas background
    ctx.fillStyle = '#0f172a';
    ctx.fillRect(0, 0, pubCanvas.width, pubCanvas.height);

    // Title banner
    ctx.fillStyle = '#ffffff';
    ctx.font = `bold ${18 * scale}px sans-serif`;
    ctx.textAlign = 'center';
    ctx.fillText(title, pubCanvas.width / 2, 28 * scale);

    ctx.fillStyle = '#94a3b8';
    ctx.font = `${11 * scale}px sans-serif`;
    ctx.fillText(subtitle, pubCanvas.width / 2, 44 * scale);

    // Draw Map Frame with neatline
    const mapX = borderPad;
    const mapY = borderPad + headerHeight;
    ctx.drawImage(rawCanvas, mapX, mapY);

    // Neatline border
    ctx.strokeStyle = '#38bdf8';
    ctx.lineWidth = 2 * scale;
    ctx.strokeRect(mapX, mapY, rawCanvas.width, rawCanvas.height);

    // Outer framing line
    ctx.strokeStyle = '#334155';
    ctx.lineWidth = 1 * scale;
    ctx.strokeRect(mapX - 4 * scale, mapY - 4 * scale, rawCanvas.width + 8 * scale, rawCanvas.height + 8 * scale);

    // Coordinate tick labels
    const bounds = this.map.getBounds();
    ctx.fillStyle = '#94a3b8';
    ctx.font = `${9 * scale}px monospace`;
    ctx.textAlign = 'center';

    // Longitude ticks (top and bottom)
    ctx.fillText(`${bounds.getWest().toFixed(2)}°`, mapX + 20 * scale, mapY - 8 * scale);
    ctx.fillText(`${((bounds.getWest() + bounds.getEast()) / 2).toFixed(2)}°`, pubCanvas.width / 2, mapY - 8 * scale);
    ctx.fillText(`${bounds.getEast().toFixed(2)}°`, mapX + rawCanvas.width - 20 * scale, mapY - 8 * scale);

    // Latitude ticks (left and right)
    ctx.textAlign = 'right';
    ctx.fillText(`${bounds.getNorth().toFixed(2)}°`, mapX - 8 * scale, mapY + 15 * scale);
    ctx.fillText(`${bounds.getSouth().toFixed(2)}°`, mapX - 8 * scale, mapY + rawCanvas.height - 5 * scale);

    // Footer info
    const footerY = mapY + rawCanvas.height + 25 * scale;
    ctx.textAlign = 'left';
    ctx.fillStyle = '#cbd5e1';
    ctx.font = `${10 * scale}px sans-serif`;
    ctx.fillText(`JSMARS Planetary Cartography | Center: ${this.map.getCenter().lat.toFixed(2)}°, ${this.map.getCenter().lng.toFixed(2)}°`, mapX, footerY);

    ctx.textAlign = 'right';
    ctx.fillStyle = '#64748b';
    ctx.fillText(`Scale ${scale}x | Data: USGS / NASA / ASU`, mapX + rawCanvas.width, footerY);

    pubCanvas.toBlob(blob => {
      if (blob) {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = filename;
        a.click();
        URL.revokeObjectURL(url);
      }
    }, 'image/png');
  }

  /**
   * Add attribution text overlay to a captured canvas.
   * @param {HTMLCanvasElement} canvas - The canvas to annotate.
   */
  _addAttribution(canvas) {
    const ctx = canvas.getContext('2d');
    const text = 'jsMars | Data: USGS/NASA';
    ctx.font = '12px sans-serif';
    ctx.fillStyle = 'rgba(255,255,255,0.7)';
    ctx.fillText(text, 10, canvas.height - 10);
  }

  /**
   * Lazy-load html2canvas from CDN (pinned version).
   * @returns {Promise<void>}
   */
  async _ensureHtml2Canvas() {
    if (this._html2canvasLoaded || window.html2canvas) {
      this._html2canvasLoaded = true;
      return;
    }

    return new Promise((resolve, reject) => {
      const script = document.createElement('script');
      script.src = 'https://cdnjs.cloudflare.com/ajax/libs/html2canvas/1.4.1/html2canvas.min.js';
      script.onload = () => {
        this._html2canvasLoaded = true;
        resolve();
      };
      script.onerror = () => reject(new Error('Failed to load html2canvas'));
      document.head.appendChild(script);
    });
  }
}
