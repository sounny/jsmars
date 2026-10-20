/**
 * @module lunar-site/PolarGrid
 * @description Moon south polar stereographic projection (sphere R = 1737.4 km,
 * true scale at the pole, lon0 = 0) and a georeferenced raster grid wrapper.
 * Matches the projection of the NASA LOLA polar products (PGDA / PDS).
 */

export const R_MOON = 1737400;

/** Lat/lon (deg) -> polar stereographic x/y (m). */
export function latLonToXY(lat, lon) {
  const rho = 2 * R_MOON * Math.tan(Math.PI / 4 + (lat * Math.PI) / 360);
  const l = (lon * Math.PI) / 180;
  return { x: rho * Math.sin(l), y: rho * Math.cos(l) };
}

/** Polar stereographic x/y (m) -> lat/lon (deg, lon in -180..180). */
export function xyToLatLon(x, y) {
  const rho = Math.hypot(x, y);
  const lat = -(90 - (2 * Math.atan(rho / (2 * R_MOON)) * 180) / Math.PI);
  const lon = (Math.atan2(x, y) * 180) / Math.PI;
  return { lat, lon };
}

/** Local map scale factor k at a point (stereographic, true at pole). */
export function scaleFactor(x, y) {
  const rho = Math.hypot(x, y);
  return 1 + (rho * rho) / (4 * R_MOON * R_MOON);
}

/**
 * A single-band georeferenced raster. geo = [x0 (left edge), y0 (top edge), res].
 */
export class RasterGrid {
  constructor(data, width, height, geo, { nodata = null, decode = null, name = '' } = {}) {
    this.data = data;
    this.width = width;
    this.height = height;
    this.x0 = geo[0];
    this.y0 = geo[1];
    this.res = geo[2];
    this.nodata = nodata;
    this.decode = decode;
    this.name = name;
  }

  get bounds() {
    return {
      xmin: this.x0, xmax: this.x0 + this.width * this.res,
      ymin: this.y0 - this.height * this.res, ymax: this.y0
    };
  }

  /** Raw decoded value at integer cell, or NaN. */
  cell(c, r) {
    if (c < 0 || r < 0 || c >= this.width || r >= this.height) return NaN;
    const v = this.data[r * this.width + c];
    if (v === this.nodata || Number.isNaN(v)) return NaN;
    return this.decode ? this.decode(v) : v;
  }

  /** Nearest-neighbour sample at map x/y. */
  sample(x, y) {
    return this.cell(Math.floor((x - this.x0) / this.res), Math.floor((this.y0 - y) / this.res));
  }

  /** Bilinear sample at map x/y (pixel centres), NaN if any neighbour missing. */
  bilinear(x, y) {
    const fc = (x - this.x0) / this.res - 0.5;
    const fr = (this.y0 - y) / this.res - 0.5;
    const c = Math.floor(fc), r = Math.floor(fr);
    const dx = fc - c, dy = fr - r;
    const a = this.cell(c, r), b = this.cell(c + 1, r);
    const d = this.cell(c, r + 1), e = this.cell(c + 1, r + 1);
    if ([a, b, d, e].some(Number.isNaN)) return this.sample(x, y);
    return a * (1 - dx) * (1 - dy) + b * dx * (1 - dy) + d * (1 - dx) * dy + e * dx * dy;
  }

  /** Decoded copy as Float32Array with NaN nodata. */
  toFloat() {
    const out = new Float32Array(this.width * this.height);
    for (let i = 0; i < out.length; i++) {
      const v = this.data[i];
      out[i] = (v === this.nodata || Number.isNaN(v)) ? NaN : (this.decode ? this.decode(v) : v);
    }
    return out;
  }
}

let geotiffPromise = null;
/** Lazy-load geotiff.js (same pinned version as mola-dem.js). */
export function loadGeoTiffLib() {
  if (window.GeoTIFF) return Promise.resolve(window.GeoTIFF);
  if (geotiffPromise) return geotiffPromise;
  geotiffPromise = new Promise((resolve, reject) => {
    const s = document.createElement('script');
    s.src = 'https://cdn.jsdelivr.net/npm/geotiff@2.1.3/dist-browser/geotiff.min.js';
    s.async = true;
    s.crossOrigin = 'anonymous';
    s.onload = () => resolve(window.GeoTIFF);
    s.onerror = () => { geotiffPromise = null; reject(new Error('Failed to load GeoTIFF library')); };
    document.head.appendChild(s);
  });
  return geotiffPromise;
}

/**
 * Load all bands of a same-origin GeoTIFF described by a manifest entry.
 * @returns {Promise<RasterGrid[]>}
 */
export async function loadBands(url, geo, { nodata = null, decode = null, names = [] } = {}) {
  const GeoTIFF = await loadGeoTiffLib();
  const resp = await fetch(url);
  if (!resp.ok) throw new Error(`HTTP ${resp.status} loading ${url}`);
  const tiff = await GeoTIFF.fromArrayBuffer(await resp.arrayBuffer());
  const img = await tiff.getImage();
  const rasters = await img.readRasters();
  const w = img.getWidth(), h = img.getHeight();
  return Array.from({ length: rasters.length }, (_, i) =>
    new RasterGrid(rasters[i], w, h, geo, { nodata, decode, name: names[i] || `band${i + 1}` }));
}

/** Decoder for uint8 half-percent fraction encoding (value/200). */
export const decodeFraction = (v) => v / 200;
