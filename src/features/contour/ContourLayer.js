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

  // --- Terrain Morphometrics & Topographic Roughness ---

  /**
   * Compute Riley (1999) Topographic Roughness Index (TRI) from an elevation grid.
   * @param {Float32Array|Array<number>} elevGrid - Elevation grid in meters
   * @param {number} width - Grid width
   * @param {number} height - Grid height
   * @returns {{meanTRI: number, maxTRI: number, triGrid: Float32Array}}
   */
  static computeTopographicRoughnessIndex(elevGrid, width, height) {
    const size = width * height;
    const triGrid = new Float32Array(size);
    let sumTRI = 0;
    let maxTRI = 0;
    let valid = 0;

    for (let y = 1; y < height - 1; y++) {
      for (let x = 1; x < width - 1; x++) {
        const idx = y * width + x;
        const z0 = elevGrid[idx];

        let sumDiffSq = 0;
        for (let dy = -1; dy <= 1; dy++) {
          for (let dx = -1; dx <= 1; dx++) {
            if (dx === 0 && dy === 0) continue;
            const neighbor = elevGrid[(y + dy) * width + (x + dx)];
            const diff = neighbor - z0;
            sumDiffSq += diff * diff;
          }
        }

        const tri = Math.sqrt(sumDiffSq / 8.0);
        triGrid[idx] = tri;
        sumTRI += tri;
        if (tri > maxTRI) maxTRI = tri;
        valid++;
      }
    }

    return {
      meanTRI: valid > 0 ? sumTRI / valid : 0,
      maxTRI,
      triGrid
    };
  }

  /**
   * Calculate Hypsometric Integral (HI) and geomorphic maturity stage.
   * @param {Float32Array|Array<number>} elevGrid
   * @returns {{hi: number, minElev: number, maxElev: number, meanElev: number, stage: string}}
   */
  static computeHypsometricIntegral(elevGrid) {
    if (!elevGrid || elevGrid.length === 0) {
      return { hi: 0, minElev: 0, maxElev: 0, meanElev: 0, stage: 'Unknown' };
    }

    let min = Infinity;
    let max = -Infinity;
    let sum = 0;

    for (let i = 0; i < elevGrid.length; i++) {
      const z = elevGrid[i];
      if (z < min) min = z;
      if (z > max) max = z;
      sum += z;
    }

    const mean = sum / elevGrid.length;
    const range = max - min;
    const hi = range > 1e-4 ? (mean - min) / range : 0.5;

    let stage = 'Mature Landscape';
    if (hi >= 0.6) stage = 'Youthful / Inequilibrated (Volcanic / Cratered)';
    else if (hi <= 0.35) stage = 'Peneplain / Monadnock / Highly Eroded';

    return {
      hi: parseFloat(hi.toFixed(3)),
      minElev: min,
      maxElev: max,
      meanElev: mean,
      stage
    };
  }

  /**
   * Calculate positive terrain volume above a reference datum.
   * @param {Float32Array|Array<number>} elevGrid - Elevation grid in meters
   * @param {number} datumMeters - Datum height in meters
   * @param {number} pixelAreaM2 - Surface area of one cell in m^2
   * @returns {{volumeM3: number, volumeKm3: number, areaAboveM2: number}}
   */
  static computeContourVolume(elevGrid, datumMeters, pixelAreaM2) {
    let volumeM3 = 0;
    let areaAboveM2 = 0;

    for (let i = 0; i < elevGrid.length; i++) {
      const diff = elevGrid[i] - datumMeters;
      if (diff > 0) {
        volumeM3 += diff * pixelAreaM2;
        areaAboveM2 += pixelAreaM2;
      }
    }

    return {
      volumeM3,
      volumeKm3: volumeM3 / 1e9,
      areaAboveM2
    };
  }

  // --- Marching Squares Isocontour Generation Solvers ---

  /**
   * Extract vector line segments for a specific isocontour level using Marching Squares.
   * @param {Float32Array|Array<number>} elevGrid - 1D elevation array
   * @param {number} width - Grid columns
   * @param {number} height - Grid rows
   * @param {number} isovalue - Elevation threshold to extract
   * @returns {Array<[[number, number], [number, number]]>} Array of 2D line segments
   */
  static extractIsovalueSegments(elevGrid, width, height, isovalue) {
    const segments = [];

    // Helper: linear interpolation fraction
    const interp = (v0, v1) => {
      const denom = v1 - v0;
      return denom === 0 ? 0.5 : Math.max(0, Math.min(1, (isovalue - v0) / denom));
    };

    for (let y = 0; y < height - 1; y++) {
      for (let x = 0; x < width - 1; x++) {
        const vTopLeft = elevGrid[y * width + x];
        const vTopRight = elevGrid[y * width + (x + 1)];
        const vBotRight = elevGrid[(y + 1) * width + (x + 1)];
        const vBotLeft = elevGrid[(y + 1) * width + x];

        // 4-bit square index: TopLeft(8), TopRight(4), BotRight(2), BotLeft(1)
        let cellIndex = 0;
        if (vTopLeft >= isovalue) cellIndex |= 8;
        if (vTopRight >= isovalue) cellIndex |= 4;
        if (vBotRight >= isovalue) cellIndex |= 2;
        if (vBotLeft >= isovalue) cellIndex |= 1;

        if (cellIndex === 0 || cellIndex === 15) continue; // All below or all above

        // Edge crossing coordinates
        const top = [x + interp(vTopLeft, vTopRight), y];
        const right = [x + 1, y + interp(vTopRight, vBotRight)];
        const bottom = [x + interp(vBotLeft, vBotRight), y + 1];
        const left = [x, y + interp(vTopLeft, vBotLeft)];

        switch (cellIndex) {
          case 1:  case 14: segments.push([left, bottom]); break;
          case 2:  case 13: segments.push([bottom, right]); break;
          case 3:  case 12: segments.push([left, right]); break;
          case 4:  case 11: segments.push([top, right]); break;
          case 5:
            segments.push([left, top]);
            segments.push([bottom, right]);
            break;
          case 6:  case 9:  segments.push([top, bottom]); break;
          case 7:  case 8:  segments.push([left, top]); break;
          case 10:
            segments.push([top, right]);
            segments.push([left, bottom]);
            break;
        }
      }
    }

    return segments;
  }

  /**
   * Determine if an elevation is a major Index Contour line.
   * @param {number} elevation - Elevation in meters
   * @param {number} [interval=500] - Base contour interval
   * @param {number} [indexFactor=5] - Index multiplier (e.g. every 5th contour)
   * @returns {boolean} True if index contour
   */
  static isIndexContour(elevation, interval = 500, indexFactor = 5) {
    const majorInterval = interval * indexFactor;
    return Math.abs(elevation % majorInterval) < 1e-3;
  }
}


