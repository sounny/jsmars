/**
 * ContourLayer generates contour lines from DEM tiles.
 * Based on the JMARS ContourStage algorithm.
 *
 * Uses the same MOLA DEM WMS source as HillshadeLayer,
 * applies edge detection on binned elevation data to draw contour lines.
 */
export class ContourLayer {
  /**
   * @param {L.Map} map
   */
  constructor(map) {
    this.map = map;
    this.isActive = false;
    this.canvasLayer = null;

    // Contour parameters
    this.baseElevation = -8000; // meters (Mars deepest: Hellas ~-8200m)
    this.step = 1000;           // contour interval in meters
    this.lineColor = '#4dabf7';
    this.lineWidth = 1;
    this.opacity = 0.7;

    // DEM source (same as hillshade)
    this.demWmsUrl = 'https://planetarymaps.usgs.gov/cgi-bin/mapserv?map=/maps/mars/mars_simp_cyl.map';
    this.demLayer = 'MOLA_elevation';
  }

  /**
   * Activate the contour layer.
   */
  activate() {
    if (this.isActive) return;
    this.isActive = true;

    this.canvasLayer = L.gridLayer({
      tileSize: 256,
      opacity: this.opacity,
      updateWhenZooming: false,
      updateWhenIdle: true,
      keepBuffer: 2,
      createTile: (coords, done) => this._createTile(coords, done)
    });

    this.canvasLayer.addTo(this.map);
  }

  /**
   * Deactivate the contour layer.
   */
  deactivate() {
    if (!this.isActive) return;
    this.isActive = false;
    if (this.canvasLayer) {
      this.map.removeLayer(this.canvasLayer);
      this.canvasLayer = null;
    }
  }

  /**
   * Toggle contour layer.
   * @returns {boolean} New active state
   */
  toggle() {
    if (this.isActive) {
      this.deactivate();
    } else {
      this.activate();
    }
    return this.isActive;
  }

  /**
   * Update contour parameters and refresh.
   * @param {object} params
   */
  setParams(params) {
    if (params.step != null) this.step = params.step;
    if (params.baseElevation != null) this.baseElevation = params.baseElevation;
    if (params.lineColor != null) this.lineColor = params.lineColor;
    if (params.lineWidth != null) this.lineWidth = params.lineWidth;
    if (params.opacity != null) {
      this.opacity = params.opacity;
      if (this.canvasLayer) this.canvasLayer.setOpacity(this.opacity);
    }
    if (this.canvasLayer) this.canvasLayer.redraw();
  }

  /**
   * Create a contour tile from DEM data.
   * @param {object} coords
   * @param {Function} done
   * @returns {HTMLCanvasElement}
   */
  _createTile(coords, done) {
    const canvas = document.createElement('canvas');
    const size = 256;
    canvas.width = size;
    canvas.height = size;
    const ctx = canvas.getContext('2d');

    const bounds = this._tileBounds(coords);
    const demUrl = this._buildDEMUrl(bounds, size, size);

    const img = new Image();
    img.crossOrigin = 'anonymous';
    img.onload = () => {
      try {
        const offCanvas = document.createElement('canvas');
        offCanvas.width = size;
        offCanvas.height = size;
        const offCtx = offCanvas.getContext('2d');
        offCtx.drawImage(img, 0, 0, size, size);

        const imageData = offCtx.getImageData(0, 0, size, size);
        const dem = this._extractElevation(imageData);

        // Draw contour lines
        this._drawContours(ctx, dem, size, size);
      } catch (err) {
        console.warn('Contour tile error:', err);
      }
      done(null, canvas);
    };
    img.onerror = () => done(null, canvas);
    img.src = demUrl;

    return canvas;
  }

  /**
   * Draw contour lines using edge detection on binned elevation data.
   * @param {CanvasRenderingContext2D} ctx
   * @param {Float32Array} dem
   * @param {number} width
   * @param {number} height
   */
  _drawContours(ctx, dem, width, height) {
    if (this.step <= 0) return;

    // Bin each pixel to its contour level
    const bins = new Int32Array(width * height);
    for (let i = 0; i < dem.length; i++) {
      bins[i] = Math.floor((dem[i] - this.baseElevation) / this.step);
    }

    // Detect edges: where adjacent bins differ
    ctx.strokeStyle = this.lineColor;
    ctx.lineWidth = this.lineWidth;
    ctx.beginPath();

    for (let y = 0; y < height - 1; y++) {
      for (let x = 0; x < width - 1; x++) {
        const idx = y * width + x;
        const current = bins[idx];

        // Check right neighbor
        if (bins[idx + 1] !== current) {
          ctx.moveTo(x + 0.5, y);
          ctx.lineTo(x + 0.5, y + 1);
        }

        // Check bottom neighbor
        if (bins[idx + width] !== current) {
          ctx.moveTo(x, y + 0.5);
          ctx.lineTo(x + 1, y + 0.5);
        }
      }
    }

    ctx.stroke();
  }

  /**
   * Extract elevation from RGBA pixel data.
   * @param {ImageData} imageData
   * @returns {Float32Array}
   */
  _extractElevation(imageData) {
    const data = imageData.data;
    const count = imageData.width * imageData.height;
    const elev = new Float32Array(count);

    for (let i = 0; i < count; i++) {
      const pixIdx = i * 4;
      const r = data[pixIdx];
      const g = data[pixIdx + 1];
      const b = data[pixIdx + 2];
      // Decode RGB to elevation (approximate)
      elev[i] = (r * 256 + g + b / 256) * 0.1 - 8000;
    }

    return elev;
  }

  /**
   * Calculate tile bounds.
   * @param {object} coords
   * @returns {L.LatLngBounds}
   */
  _tileBounds(coords) {
    const tileSize = 256;
    const nw = this.map.unproject([coords.x * tileSize, coords.y * tileSize], coords.z);
    const se = this.map.unproject([(coords.x + 1) * tileSize, (coords.y + 1) * tileSize], coords.z);
    return L.latLngBounds(nw, se);
  }

  /**
   * Build WMS URL for DEM data.
   * @param {L.LatLngBounds} bounds
   * @param {number} width
   * @param {number} height
   * @returns {string}
   */
  _buildDEMUrl(bounds, width, height) {
    const params = new URLSearchParams({
      SERVICE: 'WMS',
      VERSION: '1.1.1',
      REQUEST: 'GetMap',
      LAYERS: this.demLayer,
      STYLES: '',
      SRS: 'EPSG:4326',
      BBOX: `${bounds.getSouth()},${bounds.getWest()},${bounds.getNorth()},${bounds.getEast()}`,
      WIDTH: width,
      HEIGHT: height,
      FORMAT: 'image/png',
      TRANSPARENT: 'true'
    });
    return `${this.demWmsUrl}&${params.toString()}`;
  }

  /**
   * Compute numerical terrain slope (deg), aspect (deg), and hazard classification from elevation grid.
   * @param {Float32Array|Array<number>} elevGrid - 1D array of elevation values in meters
   * @param {number} width - Grid columns
   * @param {number} height - Grid rows
   * @param {number} [pixelSizeMeters=100] - Physical spacing between grid samples in meters
   * @returns {{meanSlopeDeg: number, maxSlopeDeg: number, slopeGrid: Float32Array, aspectGrid: Float32Array, hazardRatio: object}}
   */
  static computeTerrainSlopeAndAspect(elevGrid, width, height, pixelSizeMeters = 100) {
    const size = width * height;
    const slopeGrid = new Float32Array(size);
    const aspectGrid = new Float32Array(size);

    let sumSlope = 0;
    let maxSlope = 0;
    let validCount = 0;

    let safeCount = 0;
    let moderateCount = 0;
    let steepCount = 0;
    let criticalCount = 0;

    for (let y = 1; y < height - 1; y++) {
      for (let x = 1; x < width - 1; x++) {
        const idx = y * width + x;

        // Sobel / Central differences
        const dz_dx = (elevGrid[idx + 1] - elevGrid[idx - 1]) / (2 * pixelSizeMeters);
        const dz_dy = (elevGrid[idx + width] - elevGrid[idx - width]) / (2 * pixelSizeMeters);

        const grad = Math.sqrt(dz_dx * dz_dx + dz_dy * dz_dy);
        const slopeDeg = Math.atan(grad) * 180 / Math.PI;
        let aspectDeg = (Math.atan2(dz_dy, -dz_dx) * 180 / Math.PI + 360) % 360;

        slopeGrid[idx] = slopeDeg;
        aspectGrid[idx] = aspectDeg;

        sumSlope += slopeDeg;
        if (slopeDeg > maxSlope) maxSlope = slopeDeg;
        validCount++;

        if (slopeDeg < 5) safeCount++;
        else if (slopeDeg < 15) moderateCount++;
        else if (slopeDeg < 25) steepCount++;
        else criticalCount++;
      }
    }

    const total = validCount || 1;
    return {
      meanSlopeDeg: validCount > 0 ? sumSlope / validCount : 0,
      maxSlopeDeg: maxSlope,
      slopeGrid,
      aspectGrid,
      hazardRatio: {
        safe: safeCount / total,
        moderate: moderateCount / total,
        steep: steepCount / total,
        critical: criticalCount / total
      }
    };
  }
}
