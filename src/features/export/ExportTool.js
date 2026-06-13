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

    // Pixel size in degrees
    const pixelWidth = (bounds.getEast() - bounds.getWest()) / size.x;
    const pixelHeight = (bounds.getNorth() - bounds.getSouth()) / size.y;

    // World file format (6 lines):
    // Line 1: pixel size in x direction (map units/pixel)
    // Line 2: rotation about y axis
    // Line 3: rotation about x axis
    // Line 4: pixel size in y direction (negative for top-to-bottom)
    // Line 5: x coordinate of center of upper left pixel
    // Line 6: y coordinate of center of upper left pixel
    const content = [
      pixelWidth.toFixed(10),
      '0.0000000000',
      '0.0000000000',
      (-pixelHeight).toFixed(10),
      (bounds.getWest() + pixelWidth / 2).toFixed(10),
      (bounds.getNorth() - pixelHeight / 2).toFixed(10)
    ].join('\n');

    const blob = new Blob([content], { type: 'text/plain' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `jsmars_map_${Date.now()}.${format}`;
    a.click();
    URL.revokeObjectURL(url);
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
