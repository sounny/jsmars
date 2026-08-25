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

  // --- Bilinear Sub-Pixel Interpolation & Dynamic Contour Intervals ---

  /**
   * Evaluate sub-pixel continuous elevation on DEM grid via Bilinear Interpolation.
   * @param {Float32Array|Array<number>} elevGrid - 1D elevation array
   * @param {number} width - Grid width
   * @param {number} height - Grid height
   * @param {number} x - Float X coordinate (0 to width - 1)
   * @param {number} y - Float Y coordinate (0 to height - 1)
   * @returns {number} Interpolated elevation in meters
   */
  static bilinearInterpolateElevation(elevGrid, width, height, x, y) {
    const x0 = Math.max(0, Math.min(width - 2, Math.floor(x)));
    const y0 = Math.max(0, Math.min(height - 2, Math.floor(y)));
    const x1 = x0 + 1;
    const y1 = y0 + 1;

    const u = x - x0;
    const v = y - y0;

    const z00 = elevGrid[y0 * width + x0];
    const z10 = elevGrid[y0 * width + x1];
    const z01 = elevGrid[y1 * width + x0];
    const z11 = elevGrid[y1 * width + x1];

    const z = (1 - u) * (1 - v) * z00 +
              u * (1 - v) * z10 +
              (1 - u) * v * z01 +
              u * v * z11;

    return parseFloat(z.toFixed(2));
  }

  /**
   * Determine optimal cartographic contour interval given local terrain relief span.
   * @param {number} minElevation - Minimum elevation in meters
   * @param {number} maxElevation - Maximum elevation in meters
   * @param {number} [targetLevels=10] - Desired number of contour levels
   * @returns {{interval: number, numLevels: number, baseLevel: number}}
   */
  static computeOptimalContourInterval(minElevation, maxElevation, targetLevels = 10) {
    const span = Math.max(1, maxElevation - minElevation);
    const rawStep = span / Math.max(2, targetLevels);

    // Standard 1-2-2.5-5 nice numbers
    const exponent = Math.floor(Math.log10(rawStep));
    const fraction = rawStep / Math.pow(10, exponent);

    let niceFraction;
    if (fraction <= 1.5) niceFraction = 1;
    else if (fraction <= 2.2) niceFraction = 2;
    else if (fraction <= 3.5) niceFraction = 2.5;
    else if (fraction <= 7.5) niceFraction = 5;
    else niceFraction = 10;

    const interval = niceFraction * Math.pow(10, exponent);
    const baseLevel = Math.floor(minElevation / interval) * interval;
    const numLevels = Math.ceil((maxElevation - baseLevel) / interval);

    return {
      interval: parseFloat(interval.toFixed(1)),
      numLevels,
      baseLevel: parseFloat(baseLevel.toFixed(1))
    };
  }

  /**
   * Generate standard MOLA rainbow hypsometric hex color for any elevation.
   * @param {number} elevation - Elevation in meters
   * @param {number} [minElev=-8000] - Mars datum low (Hellas Basin)
   * @param {number} [maxElev=21000] - Mars summit (Olympus Mons)
   * @returns {string} Hex color string (e.g. #38bdf8)
   */
  static generateElevationColor(elevation, minElev = -8000, maxElev = 21000) {
    const norm = Math.max(0, Math.min(1.0, (elevation - minElev) / (maxElev - minElev)));
    // Hue from 240 (blue) to 0 (red/magenta)
    const hue = (1.0 - norm) * 240.0;
    return `hsl(${hue.toFixed(1)}, 85%, 50%)`;
  }

  // --- Horn 3x3 Slope & Aspect, Terrain Curvature & Hypsometry Solvers ---

  /**
   * Calculate precise 8-neighbor Horn (1981) slope and aspect for a 3x3 elevation patch.
   * [ z00, z10, z20 ]
   * [ z01, z11, z21 ]
   * [ z02, z12, z22 ]
   * @param {Array<number>} patch3x3 - 9 elevation values in row-major order
   * @param {number} [pixelSpacingMeters=100] - Horizontal cell resolution
   * @returns {{slopeDeg: number, slopePercent: number, aspectDeg: number, compassDirection: string}}
   */
  static computeHornSlopeAspect(patch3x3, pixelSpacingMeters = 100) {
    if (!patch3x3 || patch3x3.length < 9) {
      return { slopeDeg: 0, slopePercent: 0, aspectDeg: 0, compassDirection: 'Flat' };
    }

    const [z00, z10, z20, z01, z11, z21, z02, z12, z22] = patch3x3;
    const dx = pixelSpacingMeters;
    const dy = pixelSpacingMeters;

    // Horn weighted gradient
    const dz_dx = ((z20 + 2 * z21 + z22) - (z00 + 2 * z01 + z02)) / (8.0 * dx);
    const dz_dy = ((z02 + 2 * z12 + z22) - (z00 + 2 * z10 + z20)) / (8.0 * dy);

    const grad = Math.hypot(dz_dx, dz_dy);
    const slopeRad = Math.atan(grad);
    const slopeDeg = slopeRad * 180.0 / Math.PI;
    const slopePercent = grad * 100.0;

    let aspectDeg = 0;
    let compass = 'Flat';

    if (grad > 1e-5) {
      const aspectRad = Math.atan2(dz_dy, -dz_dx);
      aspectDeg = (aspectRad * 180.0 / Math.PI + 90.0 + 360.0) % 360.0;

      const directions = ['N', 'NE', 'E', 'SE', 'S', 'SW', 'W', 'NW'];
      const dirIdx = Math.round(aspectDeg / 45.0) % 8;
      compass = directions[dirIdx];
    }

    return {
      slopeDeg: parseFloat(slopeDeg.toFixed(2)),
      slopePercent: parseFloat(slopePercent.toFixed(2)),
      aspectDeg: parseFloat(aspectDeg.toFixed(1)),
      compassDirection: compass
    };
  }

  /**
   * Compute 3x3 profile and planform terrain curvature (second spatial derivatives).
   * @param {Array<number>} patch3x3 - 9 elevation values in row-major order
   * @param {number} [pixelSpacingMeters=100] - Cell size
   * @returns {{profileCurvature: number, planformCurvature: number, generalCurvature: number}}
   */
  static computeTerrainCurvature(patch3x3, pixelSpacingMeters = 100) {
    if (!patch3x3 || patch3x3.length < 9) {
      return { profileCurvature: 0, planformCurvature: 0, generalCurvature: 0 };
    }

    const [z00, z10, z20, z01, z11, z21, z02, z12, z22] = patch3x3;
    const L = pixelSpacingMeters;
    const L2 = L * L;

    // Second derivatives
    const d2z_dx2 = (z01 - 2 * z11 + z21) / L2;
    const d2z_dy2 = (z10 - 2 * z11 + z12) / L2;
    const d2z_dxdy = ((z22 - z02) - (z20 - z00)) / (4.0 * L2);

    const dz_dx = (z21 - z01) / (2.0 * L);
    const dz_dy = (z12 - z10) / (2.0 * L);
    const p = dz_dx * dz_dx + dz_dy * dz_dy;

    const profCurv = p > 0 ? -((dz_dx * dz_dx * d2z_dx2 + 2 * dz_dx * dz_dy * d2z_dxdy + dz_dy * dz_dy * d2z_dy2) / (p * Math.pow(1 + p, 1.5))) : 0;
    const planCurv = p > 0 ? -((dz_dy * dz_dy * d2z_dx2 - 2 * dz_dx * dz_dy * d2z_dxdy + dz_dx * dz_dx * d2z_dy2) / Math.pow(p, 1.5)) : 0;
    const genCurv = -(d2z_dx2 + d2z_dy2);

    return {
      profileCurvature: parseFloat(profCurv.toFixed(6)),
      planformCurvature: parseFloat(planCurv.toFixed(6)),
      generalCurvature: parseFloat(genCurv.toFixed(6))
    };
  }

  /**
   * Calculate cumulative hypsometric area distribution curve across elevation bins.
   * @param {Array<number>} elevations - Array of elevation samples
   * @param {number} [numBins=10] - Number of histogram bins
   * @returns {Array<{elevation: number, cumulativeFraction: number, count: number}>}
   */
  static computeHypsometricAreaDistribution(elevations = [], numBins = 10) {
    if (elevations.length === 0) return [];

    const min = Math.min(...elevations);
    const max = Math.max(...elevations);
    const range = Math.max(1, max - min);
    const binWidth = range / numBins;

    const bins = new Array(numBins).fill(0);
    elevations.forEach(z => {
      const idx = Math.min(numBins - 1, Math.max(0, Math.floor((z - min) / binWidth)));
      bins[idx]++;
    });

    let cumCount = 0;
    const total = elevations.length;
    const curve = [];

    for (let i = 0; i < numBins; i++) {
      cumCount += bins[i];
      const binElev = min + (i + 0.5) * binWidth;
      curve.push({
        elevation: parseFloat(binElev.toFixed(1)),
        cumulativeFraction: parseFloat((cumCount / total).toFixed(4)),
        count: bins[i]
      });
    }

    return curve;
  }
}




