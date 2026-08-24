/**
 * HillshadeLayer generates real-time hillshade relief from DEM tiles.
 * Based on the JMARS ReliefShadeOp algorithm (Zhou 1992).
 *
 * Uses MOLA DEM WMS tiles, draws to an offscreen canvas,
 * computes shading per pixel, and renders as a Leaflet tile overlay.
 */
export class HillshadeLayer {
  /**
   * @param {L.Map} map
   */
  constructor(map) {
    this.map = map;
    this.isActive = false;
    this.canvasLayer = null;

    // Shading parameters (degrees)
    this.azimuth = 315;     // Light source compass direction
    this.altitude = 45;     // Light source elevation angle
    this.zFactor = 1.5;     // Vertical exaggeration
    this.opacity = 0.5;

    // DEM source URL (MOLA 128ppd grayscale)
    this.demWmsUrl = 'https://planetarymaps.usgs.gov/cgi-bin/mapserv?map=/maps/mars/mars_simp_cyl.map';
    this.demLayer = 'MOLA_elevation';
  }

  /**
   * Activate the hillshade layer.
   */
  activate() {
    if (this.isActive) return;
    this.isActive = true;

    // Use L.GridLayer with custom createTile
    this.canvasLayer = L.gridLayer({
      tileSize: 256,
      opacity: this.opacity,
      updateWhenZooming: false,
      updateWhenIdle: true,
      keepBuffer: 2,
      className: 'hillshade-tile',
      createTile: (coords, done) => this._createTile(coords, done)
    });

    this.canvasLayer.addTo(this.map);
  }

  /**
   * Deactivate the hillshade layer.
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
   * Toggle the hillshade layer.
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
   * Update shading parameters and refresh.
   * @param {object} params - { azimuth, altitude, zFactor, opacity }
   */
  setParams(params) {
    if (params.azimuth != null) this.azimuth = params.azimuth;
    if (params.altitude != null) this.altitude = params.altitude;
    if (params.zFactor != null) this.zFactor = params.zFactor;
    if (params.opacity != null) {
      this.opacity = params.opacity;
      if (this.canvasLayer) {
        this.canvasLayer.setOpacity(this.opacity);
      }
    }
    // Refresh tiles
    if (this.canvasLayer) {
      this.canvasLayer.redraw();
    }
  }

  /**
   * Create a hillshade tile from WMS DEM data.
   * @param {object} coords - Tile coordinates {x, y, z}
   * @param {Function} done - Callback when tile is ready
   * @returns {HTMLCanvasElement}
   */
  _createTile(coords, done) {
    const canvas = document.createElement('canvas');
    const size = 256;
    canvas.width = size;
    canvas.height = size;
    const ctx = canvas.getContext('2d');

    // Calculate bbox for this tile (need padding for 3x3 kernel)
    const bounds = this._tileBounds(coords);

    // Fetch DEM data as WMS GetMap
    const demUrl = this._buildDEMUrl(bounds, size + 2, size + 2);

    const img = new Image();
    img.crossOrigin = 'anonymous';
    img.onload = () => {
      try {
        // Draw to offscreen canvas (slightly larger for edge pixels)
        const offCanvas = document.createElement('canvas');
        offCanvas.width = size + 2;
        offCanvas.height = size + 2;
        const offCtx = offCanvas.getContext('2d');
        offCtx.drawImage(img, 0, 0, size + 2, size + 2);

        // Get pixel data
        const imageData = offCtx.getImageData(0, 0, size + 2, size + 2);
        const dem = this._extractElevation(imageData);

        // Compute hillshade
        const shade = this._computeHillshade(dem, size + 2, size + 2);

        // Draw shaded result (offset by 1 to trim padding)
        const output = ctx.createImageData(size, size);
        for (let y = 0; y < size; y++) {
          for (let x = 0; x < size; x++) {
            const srcIdx = (y + 1) * (size + 2) + (x + 1);
            const dstIdx = (y * size + x) * 4;
            const val = shade[srcIdx];
            output.data[dstIdx] = val;     // R
            output.data[dstIdx + 1] = val; // G
            output.data[dstIdx + 2] = val; // B
            output.data[dstIdx + 3] = 255; // A
          }
        }
        ctx.putImageData(output, 0, 0);
      } catch (err) {
        console.warn('Hillshade tile error:', err);
      }
      done(null, canvas);
    };
    img.onerror = () => {
      // Return empty transparent tile on error
      done(null, canvas);
    };
    img.src = demUrl;

    return canvas;
  }

  /**
   * Compute hillshade from elevation array using Horn's method.
   * @param {Float32Array} dem - Elevation values
   * @param {number} width
   * @param {number} height
   * @returns {Uint8Array} - Shade values (0-255)
   */
  _computeHillshade(dem, width, height) {
    const shade = new Uint8Array(width * height);

    // Convert angles to radians
    const azRad = (360 - this.azimuth + 90) * Math.PI / 180;
    const altRad = this.altitude * Math.PI / 180;

    // Cell size approximation (degrees to meters at equator of Mars)
    // Mars circumference ~21344 km, so 1 degree ~59.3 km
    const cellSize = 59.3 * 1000 / Math.pow(2, 8); // Approximate based on zoom

    for (let y = 1; y < height - 1; y++) {
      for (let x = 1; x < width - 1; x++) {
        // 3x3 kernel (Horn's method)
        const a = dem[(y - 1) * width + (x - 1)];
        const b = dem[(y - 1) * width + x];
        const c = dem[(y - 1) * width + (x + 1)];
        const d = dem[y * width + (x - 1)];
        // e is center pixel (not used directly)
        const f = dem[y * width + (x + 1)];
        const g = dem[(y + 1) * width + (x - 1)];
        const h = dem[(y + 1) * width + x];
        const i = dem[(y + 1) * width + (x + 1)];

        // Partial derivatives
        const dzdx = ((c + 2 * f + i) - (a + 2 * d + g)) / (8 * cellSize) * this.zFactor;
        const dzdy = ((g + 2 * h + i) - (a + 2 * b + c)) / (8 * cellSize) * this.zFactor;

        // Slope and aspect
        const slope = Math.atan(Math.sqrt(dzdx * dzdx + dzdy * dzdy));
        const aspect = Math.atan2(dzdy, -dzdx);

        // Hillshade calculation
        let hillshade = Math.cos(altRad) * Math.sin(slope) * Math.cos(azRad - aspect) +
                        Math.sin(altRad) * Math.cos(slope);

        // Clamp to 0-255
        hillshade = Math.max(0, Math.min(255, Math.round(hillshade * 255)));

        shade[y * width + x] = hillshade;
      }
    }

    return shade;
  }

  /**
   * Extract elevation from RGBA pixel data.
   * Interprets grayscale value as relative elevation.
   * For true DEM tiles, RGB encodes elevation differently.
   * @param {ImageData} imageData
   * @returns {Float32Array}
   */
  _extractElevation(imageData) {
    const data = imageData.data;
    const count = imageData.width * imageData.height;
    const elev = new Float32Array(count);

    for (let i = 0; i < count; i++) {
      const pixIdx = i * 4;
      // Use luminance as elevation proxy from RGB-encoded DEM
      // Real MOLA DEM tiles encode elevation in the pixel value
      const r = data[pixIdx];
      const g = data[pixIdx + 1];
      const b = data[pixIdx + 2];

      // Simple grayscale interpretation (works for many USGS DEM styles)
      // Scale to approximate meters: 0=low, 255=high, mapped to Mars range
      elev[i] = (r * 256 + g + b / 256) * 0.1 - 8000;
    }

    return elev;
  }

  /**
   * Calculate the geographic bounds for a tile.
   * @param {object} coords - {x, y, z}
   * @returns {L.LatLngBounds}
   */
  _tileBounds(coords) {
    const tileSize = 256;
    const nw = this.map.unproject([coords.x * tileSize, coords.y * tileSize], coords.z);
    const se = this.map.unproject([(coords.x + 1) * tileSize, (coords.y + 1) * tileSize], coords.z);
    return L.latLngBounds(nw, se);
  }

  /**
   * Build WMS GetMap URL for DEM data.
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

  // --- Hillshade Photometry & Multidirectional Shading Solvers ---

  /**
   * Compute single-cell Horn hillshade illumination (0-255).
   * @param {number} dzdx - Partial x-derivative (rate of elevation change)
   * @param {number} dzdy - Partial y-derivative
   * @param {number} [azimuthDeg=315] - Light source compass direction in degrees
   * @param {number} [altitudeDeg=45] - Light source elevation angle above horizon
   * @param {number} [zFactor=1.0] - Vertical exaggeration
   * @returns {number} Shading value (0-255)
   */
  static computeSinglePixelHillshade(dzdx, dzdy, azimuthDeg = 315, altitudeDeg = 45, zFactor = 1.0) {
    const azRad = (360 - azimuthDeg + 90) * Math.PI / 180;
    const altRad = Math.max(0, Math.min(90, altitudeDeg)) * Math.PI / 180;

    const zx = dzdx * zFactor;
    const zy = dzdy * zFactor;

    const slope = Math.atan(Math.sqrt(zx * zx + zy * zy));
    const aspect = Math.atan2(zy, -zx);

    let val = Math.cos(altRad) * Math.sin(slope) * Math.cos(azRad - aspect) +
              Math.sin(altRad) * Math.cos(slope);

    return Math.max(0, Math.min(255, Math.round(val * 255)));
  }

  /**
   * Compute multidirectional Swiss hillshade combining 4 illumination azimuths (225°, 270°, 315°, 360°).
   * Eliminates shadowing bias in craters and canyon walls.
   * @param {number} dzdx - Partial x-derivative
   * @param {number} dzdy - Partial y-derivative
   * @param {number} [altitudeDeg=45] - Light source elevation
   * @param {number} [zFactor=1.0] - Vertical exaggeration
   * @returns {number} Multidirectional shaded relief (0-255)
   */
  static computeMultidirectionalHillshade(dzdx, dzdy, altitudeDeg = 45, zFactor = 1.0) {
    const azimuths = [225, 270, 315, 360];
    const weights = [0.25, 0.25, 0.25, 0.25];

    let combined = 0;
    for (let i = 0; i < azimuths.length; i++) {
      const shade = this.computeSinglePixelHillshade(dzdx, dzdy, azimuths[i], altitudeDeg, zFactor);
      combined += shade * weights[i];
    }

    return Math.max(0, Math.min(255, Math.round(combined)));
  }

  /**
   * Calculate topographic surface slope angle in degrees.
   * @param {number} dzdx - Partial x-derivative
   * @param {number} dzdy - Partial y-derivative
   * @param {number} [zFactor=1.0] - Vertical exaggeration
   * @returns {number} Slope in degrees (0 to 90)
   */
  static computeSlopeDegrees(dzdx, dzdy, zFactor = 1.0) {
    const zx = dzdx * zFactor;
    const zy = dzdy * zFactor;
    const slopeRad = Math.atan(Math.sqrt(zx * zx + zy * zy));
    return parseFloat((slopeRad * 180 / Math.PI).toFixed(2));
  }
}

