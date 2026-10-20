/**
 * @module lunar-site/Exporters
 * @description Export real terrain windows and site-plan geometry for Rhino /
 * Grasshopper / Isaac Sim / GIS. All CAD-style exports (OBJ, DXF) share one
 * local ENU frame: origin at the window centre, +X local east, +Y local north
 * (away from the pole), +Z elevation (m above the 1737.4 km reference sphere
 * minus the centre elevation), in ground metres.
 */
import { R_MOON, scaleFactor, xyToLatLon } from './PolarGrid.js';

/** Build the shared local frame transform at map point (cx, cy). */
export function localFrame(cx, cy, z0 = 0) {
  const rho = Math.hypot(cx, cy);
  const nx = rho > 1 ? cx / rho : 0, ny = rho > 1 ? cy / rho : 1;
  const ex = ny, ey = -nx;
  const k = scaleFactor(cx, cy);
  return {
    cx, cy, z0, k,
    toLocal(x, y, z = 0) {
      const dx = (x - cx) / k, dy = (y - cy) / k;
      return [dx * ex + dy * ey, dx * nx + dy * ny, z - z0];
    },
    describe() {
      const ll = xyToLatLon(cx, cy);
      return `Local ENU frame origin lat ${ll.lat.toFixed(5)}, lon ${ll.lon.toFixed(5)} ` +
        `(polar stereo x ${cx.toFixed(1)} m, y ${cy.toFixed(1)} m), Z0 ${z0.toFixed(2)} m rel. ${R_MOON} m sphere; ` +
        `+X east, +Y north (away from pole), +Z up; ground metres (grid scale k=${k.toFixed(6)}).`;
    }
  };
}

/**
 * Extract a sub-window of a RasterGrid.
 * @returns {{data:Float32Array,width:number,height:number,geo:number[]}}
 */
export function cropGrid(grid, xmin, ymin, xmax, ymax) {
  const c0 = Math.max(0, Math.floor((xmin - grid.x0) / grid.res));
  const c1 = Math.min(grid.width, Math.ceil((xmax - grid.x0) / grid.res));
  const r0 = Math.max(0, Math.floor((grid.y0 - ymax) / grid.res));
  const r1 = Math.min(grid.height, Math.ceil((grid.y0 - ymin) / grid.res));
  const w = c1 - c0, h = r1 - r0;
  if (w < 2 || h < 2) throw new Error('Selected window is outside the terrain data or too small');
  const data = new Float32Array(w * h);
  for (let r = 0; r < h; r++) for (let c = 0; c < w; c++) data[r * w + c] = grid.cell(c0 + c, r0 + r);
  return { data, width: w, height: h, geo: [grid.x0 + c0 * grid.res, grid.y0 - r0 * grid.res, grid.res] };
}

/**
 * Wavefront OBJ terrain mesh. NaN cells are skipped (faces touching them omitted).
 * @param {{data,width,height,geo}} win
 * @param {{yUp?:boolean, header?:string, stride?:number}} [opts]
 */
export function terrainToOBJ(win, frame, { yUp = false, header = '', stride = 1 } = {}) {
  const { data, width: w, height: h, geo } = win;
  const [x0, y0, res] = geo;
  const cols = Math.floor((w - 1) / stride) + 1, rows = Math.floor((h - 1) / stride) + 1;
  const idx = new Int32Array(cols * rows).fill(0);
  const lines = [];
  header.split('\n').filter(Boolean).forEach(l => lines.push('# ' + l));
  lines.push('# ' + frame.describe());
  lines.push(`# Grid: ${cols} x ${rows} vertices, spacing ${(res * stride).toFixed(2)} m (grid), units metres`);
  lines.push('o lunar_terrain');
  let n = 0;
  for (let r = 0; r < rows; r++) for (let c = 0; c < cols; c++) {
    const z = data[(r * stride) * w + c * stride];
    if (!Number.isFinite(z)) continue;
    const [X, Y, Z] = frame.toLocal(x0 + (c * stride + 0.5) * res, y0 - (r * stride + 0.5) * res, z);
    lines.push(yUp ? `v ${X.toFixed(3)} ${Z.toFixed(3)} ${(-Y).toFixed(3)}` : `v ${X.toFixed(3)} ${Y.toFixed(3)} ${Z.toFixed(3)}`);
    idx[r * cols + c] = ++n;
  }
  for (let r = 0; r < rows - 1; r++) for (let c = 0; c < cols - 1; c++) {
    const a = idx[r * cols + c], b = idx[r * cols + c + 1], d = idx[(r + 1) * cols + c], e = idx[(r + 1) * cols + c + 1];
    if (a && e) {
      if (d) lines.push(`f ${a} ${d} ${e}`);
      if (b) lines.push(`f ${a} ${e} ${b}`);
    } else if (b && d) {
      if (a) lines.push(`f ${a} ${d} ${b}`);
      if (e) lines.push(`f ${b} ${d} ${e}`);
    }
  }
  return lines.join('\n') + '\n';
}

/** Minimal GeoTIFF writer: single-band float32, uncompressed, polar stereographic geokeys. */
export function gridToGeoTIFF(win, citation = '') {
  const { data, width: w, height: h, geo } = win;
  const cit = `Moon2000 south polar stereographic, sphere R=${R_MOON} m; ${citation}|`;
  const nodata = 'nan\0';
  const enc = new TextEncoder();
  const citB = enc.encode(cit + '\0'), nodB = enc.encode(nodata);
  const geoKeys = [1, 1, 0, 18,
    1024, 0, 1, 1, 1025, 0, 1, 1, 1026, 34737, citB.length, 0,
    2048, 0, 1, 32767, 2050, 0, 1, 32767, 2054, 0, 1, 9102, 2056, 0, 1, 32767,
    2057, 34736, 1, 0, 2058, 34736, 1, 1,
    3072, 0, 1, 32767, 3074, 0, 1, 32767, 3075, 0, 1, 15, 3076, 0, 1, 9001,
    3081, 34736, 1, 2, 3082, 34736, 1, 3, 3083, 34736, 1, 4, 3092, 34736, 1, 5, 3095, 34736, 1, 6];
  const doubles = [R_MOON, R_MOON, -90, 0, 0, 1, 0];
  // tag: [id, type, count, payload(Uint8Array) | inline number]
  const T = { SHORT: 3, LONG: 4, ASCII: 2, DOUBLE: 12 };
  const imgBytes = w * h * 4;
  const tags = [
    [256, T.LONG, 1, w], [257, T.LONG, 1, h], [258, T.SHORT, 1, 32], [259, T.SHORT, 1, 1],
    [262, T.SHORT, 1, 1], [273, T.LONG, 1, 0 /* patched */], [277, T.SHORT, 1, 1],
    [278, T.LONG, 1, h], [279, T.LONG, 1, imgBytes], [284, T.SHORT, 1, 1], [339, T.SHORT, 1, 3],
    [33550, T.DOUBLE, 3, f64([geo[2], geo[2], 0])],
    [33922, T.DOUBLE, 6, f64([0, 0, 0, geo[0], geo[1], 0])],
    [34735, T.SHORT, geoKeys.length, u16(geoKeys)],
    [34736, T.DOUBLE, doubles.length, f64(doubles)],
    [34737, T.ASCII, citB.length, citB],
    [42113, T.ASCII, nodB.length, nodB]
  ];
  const ifdSize = 2 + tags.length * 12 + 4;
  let extOff = 8 + ifdSize;
  const ext = [];
  for (const t of tags) {
    if (t[3] instanceof Uint8Array && t[3].length > 4) { t.push(extOff); ext.push(t[3]); extOff += t[3].length + (t[3].length & 1); }
  }
  const imgOff = (extOff + 3) & ~3;
  const buf = new ArrayBuffer(imgOff + imgBytes);
  const dv = new DataView(buf), u8 = new Uint8Array(buf);
  dv.setUint16(0, 0x4949); dv.setUint16(2, 42, true); dv.setUint32(4, 8, true);
  dv.setUint16(8, tags.length, true);
  tags.forEach((t, i) => {
    const o = 10 + i * 12;
    dv.setUint16(o, t[0], true); dv.setUint16(o + 2, t[1], true); dv.setUint32(o + 4, t[2], true);
    if (t[0] === 273) dv.setUint32(o + 8, imgOff, true);
    else if (t[3] instanceof Uint8Array) {
      if (t[3].length <= 4) u8.set(t[3], o + 8); else dv.setUint32(o + 8, t[4], true);
    } else if (t[1] === T.SHORT) dv.setUint16(o + 8, t[3], true);
    else dv.setUint32(o + 8, t[3], true);
  });
  dv.setUint32(10 + tags.length * 12, 0, true);
  let p = 8 + ifdSize;
  for (const e of ext) { u8.set(e, p); p += e.length + (e.length & 1); }
  const out = new Float32Array(buf, imgOff, w * h);
  out.set(data);
  return buf;
}

function f64(arr) { return new Uint8Array(new Float64Array(arr).buffer); }
function u16(arr) { return new Uint8Array(new Uint16Array(arr).buffer); }

/**
 * ASCII DXF (R12-compatible) of polylines in the local frame.
 * @param {{layer:string, closed:boolean, points:{x:number,y:number,z?:number}[]}[]} features map coords
 */
export function featuresToDXF(features, frame, comment = '') {
  const out = ['999', (comment || 'JSMARS Lunar Site Planner').replace(/\n/g, ' '),
    '0', 'SECTION', '2', 'HEADER', '9', '$INSUNITS', '70', '6', '0', 'ENDSEC',
    '0', 'SECTION', '2', 'ENTITIES'];
  for (const f of features) {
    const layer = String(f.layer || '0').replace(/[<>/\\":;?*|=`]/g, '_').slice(0, 64) || '0';
    if (f.points.length === 1) {
      const zVal = Number.isFinite(f.points[0].z) ? f.points[0].z : frame.z0;
      const [X, Y, Z] = frame.toLocal(f.points[0].x, f.points[0].y, zVal);
      out.push('0', 'POINT', '8', layer, '10', X.toFixed(4), '20', Y.toFixed(4), '30', Z.toFixed(4));
      continue;
    }
    out.push('0', 'POLYLINE', '8', layer, '66', '1', '70', String((f.closed ? 1 : 0) | 8), '10', '0', '20', '0', '30', '0');
    for (const pt of f.points) {
      const zVal = Number.isFinite(pt.z) ? pt.z : frame.z0;
      const [X, Y, Z] = frame.toLocal(pt.x, pt.y, zVal);
      out.push('0', 'VERTEX', '8', layer, '10', X.toFixed(4), '20', Y.toFixed(4), '30', Z.toFixed(4), '70', '32');
    }
    out.push('0', 'SEQEND', '8', layer);
  }
  out.push('0', 'ENDSEC', '0', 'EOF');
  return out.join('\n') + '\n';
}

/** RFC 4180 CSV. */
export function toCSV(headers, rows) {
  const esc = (v) => {
    const s = v === null || v === undefined || (typeof v === 'number' && !Number.isFinite(v)) ? '' : String(v);
    return /[",\n]/.test(s) ? `"${s.replace(/"/g, '""')}"` : s;
  };
  return [headers, ...rows].map(r => r.map(esc).join(',')).join('\n') + '\n';
}

/** Trigger a browser download. */
export function download(content, filename, mime = 'application/octet-stream') {
  const blob = content instanceof Blob ? content : new Blob([content], { type: mime });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url; a.download = filename;
  document.body.appendChild(a); a.click(); a.remove();
  setTimeout(() => URL.revokeObjectURL(url), 2000);
}
