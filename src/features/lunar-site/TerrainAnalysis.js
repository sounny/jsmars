/**
 * @module lunar-site/TerrainAnalysis
 * @description Pure terrain analysis functions used by the Lunar Site Planner.
 * All operate on real LOLA-derived grids (see PolarGrid.RasterGrid); none
 * fabricate values. Unit-tested in tests/unit.js.
 */
import { R_MOON, xyToLatLon } from './PolarGrid.js';

/** Obliquity of the lunar equator to the ecliptic (deg). */
export const LUNAR_OBLIQUITY_DEG = 1.543;

/**
 * Exact 1D squared Euclidean distance transform (Felzenszwalb & Huttenlocher 2012).
 * @param {Float64Array} f input (0 at features, Infinity elsewhere)
 */
function edt1d(f, n, d, v, z) {
  let k = 0;
  v[0] = 0; z[0] = -Infinity; z[1] = Infinity;
  for (let q = 1; q < n; q++) {
    let s;
    do {
      const p = v[k];
      s = ((f[q] + q * q) - (f[p] + p * p)) / (2 * q - 2 * p);
      if (s <= z[k]) k--; else break;
    } while (k >= 0);
    k++;
    v[k] = q; z[k] = s; z[k + 1] = Infinity;
  }
  k = 0;
  for (let q = 0; q < n; q++) {
    while (z[k + 1] < q) k++;
    const p = v[k];
    d[q] = (q - p) * (q - p) + f[p];
  }
}

/**
 * Euclidean distance (in cells) from every cell to the nearest `true` cell of mask.
 * Cells with no feature anywhere get Infinity.
 * @param {Uint8Array} mask 1 = feature
 */
export function distanceTransform(mask, w, h) {
  const INF = 1e20;
  const g = new Float64Array(w * h);
  for (let i = 0; i < g.length; i++) g[i] = mask[i] ? 0 : INF;
  const n = Math.max(w, h);
  const f = new Float64Array(n), d = new Float64Array(n);
  const v = new Int32Array(n), z = new Float64Array(n + 1);
  for (let x = 0; x < w; x++) {
    for (let y = 0; y < h; y++) f[y] = g[y * w + x];
    edt1d(f, h, d, v, z);
    for (let y = 0; y < h; y++) g[y * w + x] = d[y];
  }
  const out = new Float32Array(w * h);
  for (let y = 0; y < h; y++) {
    for (let x = 0; x < w; x++) f[x] = g[y * w + x];
    edt1d(f, w, d, v, z);
    for (let x = 0; x < w; x++) out[y * w + x] = d[x] >= INF / 2 ? Infinity : Math.sqrt(d[x]);
  }
  return out;
}

/**
 * Find the largest level discs: centres whose distance to the nearest cell
 * exceeding `maxSlope` (or nodata / grid edge) is greatest.
 * @param {import('./PolarGrid.js').RasterGrid} slope slope grid (deg)
 * @returns {{x:number,y:number,radius_m:number,lat:number,lon:number}[]}
 */
export function findFlatAreas(slope, maxSlope, count = 5) {
  const { width: w, height: h, res } = slope;
  const W = w + 2, H = h + 2; // 1-cell border of "bad" so discs stay inside data
  const bad = new Uint8Array(W * H).fill(1);
  for (let r = 0; r < h; r++) {
    for (let c = 0; c < w; c++) {
      const s = slope.cell(c, r);
      bad[(r + 1) * W + c + 1] = (Number.isNaN(s) || s > maxSlope) ? 1 : 0;
    }
  }
  const dist = distanceTransform(bad, W, H);
  const results = [];
  const taken = [];
  const order = [];
  for (let i = 0; i < dist.length; i++) if (dist[i] > 1 && Number.isFinite(dist[i])) order.push(i);
  order.sort((a, b) => dist[b] - dist[a]);
  for (const i of order) {
    if (results.length >= count) break;
    const r = Math.floor(i / W) - 1, c = (i % W) - 1;
    const rad = (dist[i] - 0.5) * res; // conservative: distance to edge of nearest bad cell
    const x = slope.x0 + (c + 0.5) * res, y = slope.y0 - (r + 0.5) * res;
    if (taken.some(t => Math.hypot(t.x - x, t.y - y) < t.radius_m + rad)) continue;
    const ll = xyToLatLon(x, y);
    const item = { x, y, radius_m: rad, lat: ll.lat, lon: ll.lon };
    results.push(item); taken.push(item);
  }
  return results;
}

/** Lambertian hillshade (0 = nodata, 1..255 = shaded) of a DEM grid. Az/alt in degrees (az clockwise from grid +Y). */
export function hillshade(dem, azimuth = 315, altitude = 25) {
  const { width: w, height: h, res } = dem;
  const z = dem.toFloat();
  const out = new Uint8ClampedArray(w * h);
  const az = (azimuth * Math.PI) / 180, alt = (altitude * Math.PI) / 180;
  const lx = Math.sin(az) * Math.cos(alt), ly = Math.cos(az) * Math.cos(alt), lz = Math.sin(alt);
  const at = (c, r) => z[Math.min(h - 1, Math.max(0, r)) * w + Math.min(w - 1, Math.max(0, c))];
  for (let r = 0; r < h; r++) {
    for (let c = 0; c < w; c++) {
      const dzdx = (at(c + 1, r) - at(c - 1, r)) / (2 * res);
      const dzdy = (at(c, r - 1) - at(c, r + 1)) / (2 * res); // +y is up (north in grid)
      if (!Number.isFinite(dzdx) || !Number.isFinite(dzdy)) { out[r * w + c] = 0; continue; }
      const n = Math.hypot(dzdx, dzdy, 1);
      const v = (-dzdx * lx - dzdy * ly + lz) / n;
      out[r * w + c] = Math.max(1, Math.round(Math.max(0, v) * 254 + 1));
    }
  }
  return out;
}

/**
 * DEM roughness proxy: standard deviation of elevation residuals from the 3x3
 * best-fit plane, in metres. NOT boulder density.
 */
export function roughness(dem) {
  const { width: w, height: h } = dem;
  const z = dem.toFloat();
  const out = new Float32Array(w * h).fill(NaN);
  for (let r = 1; r < h - 1; r++) {
    for (let c = 1; c < w - 1; c++) {
      let sx = 0, sy = 0, s = 0, ok = true;
      const vals = [];
      for (let j = -1; j <= 1; j++) for (let i = -1; i <= 1; i++) {
        const v = z[(r + j) * w + c + i];
        if (!Number.isFinite(v)) ok = false;
        vals.push([i, j, v]);
        s += v; sx += i * v; sy += j * v;
      }
      if (!ok) continue;
      const m = s / 9, a = sx / 6, b = sy / 6; // plane fit on symmetric 3x3 stencil
      let ss = 0;
      for (const [i, j, v] of vals) { const e = v - (m + a * i + b * j); ss += e * e; }
      out[r * w + c] = Math.sqrt(ss / 9);
    }
  }
  return out;
}

/**
 * Sample an elevation profile along a polyline of map points.
 * @param {(x:number,y:number)=>number} elevAt elevation sampler (m)
 * @param {(x:number,y:number)=>number} [slopeAt] optional terrain-slope sampler (deg)
 */
export function sampleProfile(points, elevAt, step, slopeAt = null) {
  const samples = [];
  let total = 0;
  for (let i = 0; i < points.length - 1; i++) {
    const a = points[i], b = points[i + 1];
    const len = Math.hypot(b.x - a.x, b.y - a.y);
    const n = Math.max(1, Math.ceil(len / step));
    for (let k = (i === 0 ? 0 : 1); k <= n; k++) {
      const t = k / n;
      const x = a.x + (b.x - a.x) * t, y = a.y + (b.y - a.y) * t;
      samples.push({ d: total + len * t, x, y, z: elevAt(x, y), terrainSlope: slopeAt ? slopeAt(x, y) : NaN });
    }
    total += len;
  }
  let climb = 0, descent = 0, maxGrade = 0;
  for (let i = 1; i < samples.length; i++) {
    const dz = samples[i].z - samples[i - 1].z, dd = samples[i].d - samples[i - 1].d;
    if (!Number.isFinite(dz) || dd <= 0) { samples[i].grade = NaN; continue; }
    samples[i].grade = (Math.atan2(Math.abs(dz), dd) * 180) / Math.PI;
    if (dz > 0) climb += dz; else descent -= dz;
    maxGrade = Math.max(maxGrade, samples[i].grade);
  }
  if (samples.length) samples[0].grade = NaN;
  const zs = samples.map(s => s.z).filter(Number.isFinite);
  const ts = samples.map(s => s.terrainSlope).filter(Number.isFinite);
  return {
    samples, length: total, climb, descent, maxGrade,
    maxTerrainSlope: ts.length ? Math.max(...ts) : NaN,
    zmin: zs.length ? Math.min(...zs) : NaN, zmax: zs.length ? Math.max(...zs) : NaN
  };
}

/**
 * Terrain horizon elevation angle (deg) for each azimuth, accounting for lunar curvature.
 * Azimuth is measured clockwise from local geographic north (toward the pole is
 * "south"; at the pole itself, from grid +Y).
 * @param {(x:number,y:number)=>number} elevAt elevation sampler (m), NaN outside data
 */
export function horizonProfile(x0, y0, elevAt, { observerHeight = 2, azStep = 2, maxDist = 100000, step0 = 20 } = {}) {
  const h0 = elevAt(x0, y0) + observerHeight;
  const { lat, lon } = xyToLatLon(x0, y0);
  // Local north direction in grid coords: away from pole is north for the south polar grid.
  const rho = Math.hypot(x0, y0);
  const nx = rho > 1 ? x0 / rho : 0, ny = rho > 1 ? y0 / rho : 1;
  const ex = ny, ey = -nx; // east = d/dlon of (sin lon, cos lon) = (cos lon, -sin lon)
  const out = [];
  for (let az = 0; az < 360; az += azStep) {
    const a = (az * Math.PI) / 180;
    const dx = Math.sin(a) * ex + Math.cos(a) * nx;
    const dy = Math.sin(a) * ey + Math.cos(a) * ny;
    let best = -90, d = step0;
    while (d <= maxDist) {
      const z = elevAt(x0 + dx * d, y0 + dy * d);
      if (Number.isFinite(z)) {
        const drop = (d * d) / (2 * R_MOON);
        const ang = (Math.atan2(z - h0 - drop, d) * 180) / Math.PI;
        if (ang > best) best = ang;
      }
      d += Math.max(step0, d * 0.01);
    }
    out.push({ az, elev: best });
  }
  return { lat, lon, ground: h0 - observerHeight, profile: out };
}

/**
 * Geometric envelope of possible Sun positions (azimuth vs elevation) at a site,
 * over all hour angles, for subsolar latitudes ±obliquity. Pure geometry.
 */
export function sunPathEnvelope(lat, lon, azStep = 2) {
  const φ = (lat * Math.PI) / 180, λ = (lon * Math.PI) / 180;
  const up = [Math.cos(φ) * Math.cos(λ), Math.cos(φ) * Math.sin(λ), Math.sin(φ)];
  const east = [-Math.sin(λ), Math.cos(λ), 0];
  const north = [-Math.sin(φ) * Math.cos(λ), -Math.sin(φ) * Math.sin(λ), Math.cos(φ)];
  const bins = 360 / azStep;
  const lo = new Array(bins).fill(Infinity), hi = new Array(bins).fill(-Infinity);
  for (let dd = -LUNAR_OBLIQUITY_DEG; dd <= LUNAR_OBLIQUITY_DEG + 1e-9; dd += LUNAR_OBLIQUITY_DEG / 8) {
    const δ = (dd * Math.PI) / 180;
    for (let ls = 0; ls < 360; ls += 0.25) {
      const L = (ls * Math.PI) / 180;
      const s = [Math.cos(δ) * Math.cos(L), Math.cos(δ) * Math.sin(L), Math.sin(δ)];
      const u = s[0] * up[0] + s[1] * up[1] + s[2] * up[2];
      const e = s[0] * east[0] + s[1] * east[1] + s[2] * east[2];
      const n = s[0] * north[0] + s[1] * north[1] + s[2] * north[2];
      const el = (Math.asin(Math.max(-1, Math.min(1, u))) * 180) / Math.PI;
      let az = (Math.atan2(e, n) * 180) / Math.PI; if (az < 0) az += 360;
      const b = Math.floor(az / azStep) % bins;
      lo[b] = Math.min(lo[b], el); hi[b] = Math.max(hi[b], el);
    }
  }
  return lo.map((l, i) => ({ az: i * azStep, min: l, max: hi[i] }));
}

/** Summary statistics of a grid inside a disc. */
export function discStats(grid, x, y, radius) {
  const vals = [];
  const r0 = Math.floor((grid.y0 - (y + radius)) / grid.res), r1 = Math.ceil((grid.y0 - (y - radius)) / grid.res);
  const c0 = Math.floor((x - radius - grid.x0) / grid.res), c1 = Math.ceil((x + radius - grid.x0) / grid.res);
  for (let r = r0; r <= r1; r++) for (let c = c0; c <= c1; c++) {
    const cx = grid.x0 + (c + 0.5) * grid.res, cy = grid.y0 - (r + 0.5) * grid.res;
    if (Math.hypot(cx - x, cy - y) > radius) continue;
    const v = grid.cell(c, r);
    if (Number.isFinite(v)) vals.push(v);
  }
  if (!vals.length) return { n: 0 };
  vals.sort((a, b) => a - b);
  return { n: vals.length, min: vals[0], max: vals[vals.length - 1],
    mean: vals.reduce((a, b) => a + b, 0) / vals.length, p95: vals[Math.floor(0.95 * (vals.length - 1))] };
}
