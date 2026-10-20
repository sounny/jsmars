/**
 * @module lunar-site/CadImport
 * @description Parse 2D CAD site plans (DXF ASCII, SVG) into simple vector
 * features and place them on the lunar polar grid.
 *
 * Supported DXF entities: LINE, LWPOLYLINE (incl. bulges), POLYLINE/VERTEX,
 * CIRCLE, ARC, ELLIPSE, SPLINE (fit or control points, polyline approximation),
 * POINT, and INSERT of block definitions (translation, scale, rotation; nested).
 */
import { scaleFactor } from './PolarGrid.js';

/** DXF $INSUNITS code -> metres per unit. */
export const DXF_UNITS = { 0: null, 1: 0.0254, 2: 0.3048, 4: 0.001, 5: 0.01, 6: 1, 7: 1000, 14: 0.1 };
export const UNIT_OPTIONS = { mm: 0.001, cm: 0.01, m: 1, in: 0.0254, ft: 0.3048 };

const ARC_SEG_DEG = 5;

function arcPoints(cx, cy, r, a0, a1) {
  // a0/a1 degrees, counter-clockwise
  let sweep = a1 - a0;
  while (sweep <= 0) sweep += 360;
  const n = Math.max(2, Math.ceil(sweep / ARC_SEG_DEG));
  const pts = [];
  for (let i = 0; i <= n; i++) {
    const a = ((a0 + (sweep * i) / n) * Math.PI) / 180;
    pts.push([cx + r * Math.cos(a), cy + r * Math.sin(a)]);
  }
  return pts;
}

function bulgeSegment(p, q, bulge) {
  if (!bulge) return [q];
  const theta = 4 * Math.atan(bulge); // signed sweep, + = counter-clockwise
  const dx = q[0] - p[0], dy = q[1] - p[1];
  const chord = Math.hypot(dx, dy);
  if (chord === 0) return [q];
  const radius = Math.abs(chord / (2 * Math.sin(theta / 2)));
  const d = chord / (2 * Math.tan(theta / 2)); // signed offset of centre along left normal
  const cx = (p[0] + q[0]) / 2 - (dy / chord) * d;
  const cy = (p[1] + q[1]) / 2 + (dx / chord) * d;
  const a0 = Math.atan2(p[1] - cy, p[0] - cx);
  const n = Math.max(2, Math.ceil(Math.abs((theta * 180) / Math.PI) / ARC_SEG_DEG));
  const out = [];
  for (let i = 1; i < n; i++) {
    const a = a0 + (theta * i) / n;
    out.push([cx + radius * Math.cos(a), cy + radius * Math.sin(a)]);
  }
  out.push(q);
  return out;
}

/** Tokenise DXF text into [code, value] pairs. */
function dxfPairs(text) {
  const lines = text.split(/\r?\n/);
  const pairs = [];
  for (let i = 0; i + 1 < lines.length; i += 2) {
    const rawCode = lines[i].trim();
    if (rawCode === '' && lines.slice(i).every(l => l.trim() === '')) break;
    const code = parseInt(rawCode, 10);
    if (Number.isNaN(code)) throw new Error(`Malformed DXF near line ${i + 1}`);
    const val = lines[i + 1].trim();
    pairs.push([code, val]);
    if (code === 0 && val === 'EOF') break;
  }
  return pairs;
}

/** Split a list of pairs into entity records starting at code 0. */
function records(pairs) {
  const out = [];
  let cur = null;
  for (const [c, v] of pairs) {
    if (c === 0) { cur = { type: v, codes: [] }; out.push(cur); }
    else if (cur) cur.codes.push([c, v]);
  }
  return out;
}

const num = (rec, code, def = 0) => {
  const p = rec.codes.find(([c]) => c === code);
  return p ? parseFloat(p[1]) : def;
};
const str = (rec, code, def = '') => {
  const p = rec.codes.find(([c]) => c === code);
  return p ? p[1] : def;
};

function entityToFeatures(rec, ctx) {
  const layer = str(rec, 8, '0');
  switch (rec.type) {
    case 'LINE':
      return [{ layer, closed: false, points: [[num(rec, 10), num(rec, 20)], [num(rec, 11), num(rec, 21)]] }];
    case 'LWPOLYLINE': {
      const closed = (num(rec, 70) & 1) === 1;
      const verts = [];
      for (const [c, v] of rec.codes) {
        if (c === 10) verts.push({ x: parseFloat(v), y: 0, b: 0 });
        else if (c === 20 && verts.length) verts[verts.length - 1].y = parseFloat(v);
        else if (c === 42 && verts.length) verts[verts.length - 1].b = parseFloat(v);
      }
      return [polyFromVerts(verts, closed, layer)];
    }
    case 'CIRCLE': {
      const cx = num(rec, 10), cy = num(rec, 20), r = num(rec, 40);
      return [{ layer, closed: true, points: arcPoints(cx, cy, r, 0, 360).slice(0, -1) }];
    }
    case 'ARC':
      return [{ layer, closed: false, points: arcPoints(num(rec, 10), num(rec, 20), num(rec, 40), num(rec, 50), num(rec, 51)) }];
    case 'ELLIPSE': {
      const cx = num(rec, 10), cy = num(rec, 20), mx = num(rec, 11), my = num(rec, 21);
      const ratio = num(rec, 40, 1), t0 = num(rec, 41, 0), t1 = num(rec, 42, 2 * Math.PI);
      const a = Math.hypot(mx, my), rot = Math.atan2(my, mx), b = a * ratio;
      let sweep = t1 - t0; while (sweep <= 0) sweep += 2 * Math.PI;
      const n = Math.max(8, Math.ceil((sweep * 180) / Math.PI / ARC_SEG_DEG));
      const pts = [];
      for (let i = 0; i <= n; i++) {
        const t = t0 + (sweep * i) / n;
        const ex = a * Math.cos(t), ey = b * Math.sin(t);
        pts.push([cx + ex * Math.cos(rot) - ey * Math.sin(rot), cy + ex * Math.sin(rot) + ey * Math.cos(rot)]);
      }
      const full = Math.abs(sweep - 2 * Math.PI) < 1e-6;
      return [{ layer, closed: full, points: full ? pts.slice(0, -1) : pts }];
    }
    case 'SPLINE': {
      const fit = [], ctrl = [];
      for (const [c, v] of rec.codes) {
        if (c === 11) fit.push([parseFloat(v), 0]);
        else if (c === 21 && fit.length) fit[fit.length - 1][1] = parseFloat(v);
        else if (c === 10) ctrl.push([parseFloat(v), 0]);
        else if (c === 20 && ctrl.length) ctrl[ctrl.length - 1][1] = parseFloat(v);
      }
      ctx.warnings.add('SPLINE entities approximated by their fit/control polygon');
      return [{ layer, closed: (num(rec, 70) & 1) === 1, points: fit.length >= 2 ? fit : ctrl }];
    }
    case 'POINT':
      return [{ layer, closed: false, point: true, points: [[num(rec, 10), num(rec, 20)]] }];
    case 'INSERT': {
      const block = ctx.blocks[str(rec, 2)];
      if (!block) { ctx.warnings.add(`INSERT of unknown block "${str(rec, 2)}" skipped`); return []; }
      if (ctx.depth > 8) { ctx.warnings.add('Block nesting deeper than 8 skipped'); return []; }
      const ix = num(rec, 10), iy = num(rec, 20), sx = num(rec, 41, 1), sy = num(rec, 42, 1);
      const rot = (num(rec, 50) * Math.PI) / 180, cos = Math.cos(rot), sin = Math.sin(rot);
      ctx.depth++;
      const feats = block.entities.flatMap(e => entityToFeatures(e, ctx));
      ctx.depth--;
      return feats.map(f => ({
        ...f,
        layer: f.layer === '0' ? layer : f.layer,
        points: f.points.map(([x, y]) => {
          const px = (x - block.bx) * sx, py = (y - block.by) * sy;
          return [ix + px * cos - py * sin, iy + px * sin + py * cos];
        })
      }));
    }
    default:
      if (!['TEXT', 'MTEXT', 'DIMENSION', 'HATCH', 'ATTRIB', 'ATTDEF', 'VIEWPORT', 'SEQEND', 'VERTEX'].includes(rec.type)) {
        ctx.warnings.add(`Unsupported entity ${rec.type} skipped`);
      }
      return [];
  }
}

function polyFromVerts(verts, closed, layer) {
  const pts = [];
  verts.forEach((v, i) => {
    if (i === 0) { pts.push([v.x, v.y]); return; }
    const p = verts[i - 1];
    pts.push(...bulgeSegment([p.x, p.y], [v.x, v.y], p.b));
  });
  if (closed && verts.length > 1) {
    const last = verts[verts.length - 1];
    pts.push(...bulgeSegment([last.x, last.y], [verts[0].x, verts[0].y], last.b).slice(0, -1));
  }
  return { layer, closed, points: pts };
}

/** Merge legacy POLYLINE + VERTEX... + SEQEND sequences into single records. */
function mergePolylines(recs) {
  const out = [];
  for (let i = 0; i < recs.length; i++) {
    const r = recs[i];
    if (r.type !== 'POLYLINE') { out.push(r); continue; }
    const verts = [];
    let j = i + 1;
    for (; j < recs.length && recs[j].type === 'VERTEX'; j++) {
      verts.push({ x: num(recs[j], 10), y: num(recs[j], 20), b: num(recs[j], 42) });
    }
    if (recs[j]?.type === 'SEQEND') j++;
    out.push({ type: '_POLY', codes: r.codes, verts, closed: (num(r, 70) & 1) === 1 });
    i = j - 1;
  }
  return out;
}

/**
 * Parse ASCII DXF text.
 * @returns {{features:{layer:string,closed:boolean,points:number[][]}[], unitsToMeters:number|null, layers:string[], warnings:string[]}}
 */
export function parseDXF(text) {
  if (text.startsWith('AutoCAD Binary DXF')) throw new Error('Binary DXF is not supported; save as ASCII DXF.');
  const pairs = dxfPairs(text);
  const ctx = { blocks: {}, warnings: new Set(), depth: 0 };
  let unitsToMeters = null;
  // Sections
  const sections = {};
  for (let i = 0; i < pairs.length; i++) {
    if (pairs[i][0] === 0 && pairs[i][1] === 'SECTION' && pairs[i + 1]?.[0] === 2) {
      const name = pairs[i + 1][1];
      let j = i + 2;
      while (j < pairs.length && !(pairs[j][0] === 0 && pairs[j][1] === 'ENDSEC')) j++;
      sections[name] = pairs.slice(i + 2, j);
      i = j;
    }
  }
  const header = sections.HEADER || [];
  for (let i = 0; i < header.length; i++) {
    if (header[i][0] === 9 && header[i][1] === '$INSUNITS') {
      const code = parseInt(header[i + 1]?.[1], 10);
      unitsToMeters = DXF_UNITS[code] ?? null;
    }
  }
  // Blocks
  const brecs = mergePolylines(records(sections.BLOCKS || []));
  let cur = null;
  for (const r of brecs) {
    if (r.type === 'BLOCK') { cur = { name: str(r, 2), bx: num(r, 10), by: num(r, 20), entities: [] }; ctx.blocks[cur.name] = cur; }
    else if (r.type === 'ENDBLK') cur = null;
    else if (cur) cur.entities.push(r);
  }
  const conv = (r) => r.type === '_POLY'
    ? [polyFromVerts(r.verts, r.closed, str(r, 8, '0'))]
    : entityToFeatures(r, ctx);
  for (const b of Object.values(ctx.blocks)) {
    // pre-convert legacy polylines inside blocks so INSERT can reuse entityToFeatures
    b.entities = b.entities.map(e => e.type === '_POLY'
      ? { type: 'LWPOLYLINE', codes: [[8, str(e, 8, '0')], [70, e.closed ? '1' : '0'], ...e.verts.flatMap(v => [[10, String(v.x)], [20, String(v.y)], [42, String(v.b)]])] }
      : e);
  }
  if (!sections.ENTITIES) throw new Error('DXF has no ENTITIES section');
  const features = mergePolylines(records(sections.ENTITIES)).flatMap(conv)
    .filter(f => f.points.length >= (f.point ? 1 : 2) && f.points.every(p => Number.isFinite(p[0]) && Number.isFinite(p[1])));
  return { features, unitsToMeters, layers: [...new Set(features.map(f => f.layer))], warnings: [...ctx.warnings] };
}

/**
 * Parse SVG text. Geometry is resolved in the root user coordinate system
 * (viewBox units) with the y axis flipped so +y points up like CAD.
 * Requires a DOM (browser).
 */
export function parseSVG(text) {
  const doc = new DOMParser().parseFromString(text, 'image/svg+xml');
  const err = doc.querySelector('parsererror');
  if (err) throw new Error('Invalid SVG: ' + err.textContent.slice(0, 120));
  const svg = document.importNode(doc.documentElement, true);
  const host = document.createElement('div');
  host.style.cssText = 'position:absolute;left:-99999px;top:0;visibility:hidden;';
  host.appendChild(svg);
  document.body.appendChild(host);
  const features = [];
  const warnings = new Set();
  try {
    const rootInv = svg.getScreenCTM()?.inverse();
    const sel = 'path,polyline,polygon,line,rect,circle,ellipse';
    for (const el of svg.querySelectorAll(sel)) {
      if (typeof el.getTotalLength !== 'function') continue;
      const m = rootInv ? rootInv.multiply(el.getScreenCTM()) : null;
      const len = el.getTotalLength();
      if (!(len > 0)) continue;
      const n = Math.min(4000, Math.max(2, Math.ceil(len / 0.5)));
      const tag = el.tagName.toLowerCase();
      const isLinear = tag === 'polyline' || tag === 'polygon' || tag === 'line' || tag === 'rect';
      let pts = [];
      if (isLinear && tag !== 'rect') {
        const raw = tag === 'line'
          ? [[+el.getAttribute('x1') || 0, +el.getAttribute('y1') || 0], [+el.getAttribute('x2') || 0, +el.getAttribute('y2') || 0]]
          : Array.from(el.points || [], p => [p.x, p.y]);
        pts = raw;
      } else {
        for (let i = 0; i <= n; i++) { const p = el.getPointAtLength((len * i) / n); pts.push([p.x, p.y]); }
      }
      pts = pts.map(([x, y]) => {
        if (!m) return [x, -y];
        return [m.a * x + m.c * y + m.e, -(m.b * x + m.d * y + m.f)];
      });
      const closed = tag === 'polygon' || tag === 'rect' || tag === 'circle' || tag === 'ellipse' || /z\s*$/i.test(el.getAttribute('d') || '');
      if (closed && pts.length > 2) {
        const a = pts[0], b = pts[pts.length - 1];
        if (Math.hypot(a[0] - b[0], a[1] - b[1]) < 1e-9) pts.pop();
      }
      const layer = el.closest('g[id]')?.id || el.getAttribute('id') || tag;
      features.push({ layer, closed, points: pts });
    }
  } finally {
    host.remove();
  }
  // Physical units from width attribute, if any (e.g. "420mm").
  let unitsToMeters = null;
  const wAttr = doc.documentElement.getAttribute('width') || '';
  const vb = (doc.documentElement.getAttribute('viewBox') || '').trim().split(/[\s,]+/).map(Number);
  const mm = wAttr.match(/^([\d.]+)\s*(mm|cm|m|in)$/);
  if (mm && vb.length === 4 && vb[2] > 0) {
    unitsToMeters = (parseFloat(mm[1]) * UNIT_OPTIONS[mm[2]]) / vb[2];
  } else {
    warnings.add('SVG has no physical units; choose the drawing unit and scale manually.');
  }
  return { features, unitsToMeters, layers: [...new Set(features.map(f => f.layer))], warnings: [...warnings] };
}

/** Bounding box of features in drawing units. */
export function featureBounds(features) {
  let xmin = Infinity, ymin = Infinity, xmax = -Infinity, ymax = -Infinity;
  for (const f of features) for (const [x, y] of f.points) {
    if (x < xmin) xmin = x; if (x > xmax) xmax = x;
    if (y < ymin) ymin = y; if (y > ymax) ymax = y;
  }
  return { xmin, ymin, xmax, ymax };
}

/**
 * Place drawing features on the polar grid.
 * @param {object} p placement
 * @param {number} p.anchorX anchor map x (m) the drawing origin is placed at
 * @param {number} p.anchorY anchor map y (m)
 * @param {number} p.originX drawing-unit x of the drawing point placed at the anchor
 * @param {number} p.originY drawing-unit y
 * @param {number} p.unitsToMeters metres per drawing unit
 * @param {number} p.rotationDeg clockwise rotation of drawing +Y from local geographic north
 *   (away from the pole). At the exact pole, from grid +Y.
 * @returns {{layer, closed, points:{x,y}[]}[]}
 */
export function placeFeatures(features, p) {
  const rho = Math.hypot(p.anchorX, p.anchorY);
  const nx = rho > 1 ? p.anchorX / rho : 0, ny = rho > 1 ? p.anchorY / rho : 1;
  const ex = ny, ey = -nx; // local east
  const r = (p.rotationDeg * Math.PI) / 180;
  // drawing +Y -> north rotated clockwise by r; drawing +X -> east rotated clockwise by r
  const yx = Math.sin(r) * ex + Math.cos(r) * nx, yy = Math.sin(r) * ey + Math.cos(r) * ny;
  const xx = Math.cos(r) * ex - Math.sin(r) * nx, xy = Math.cos(r) * ey - Math.sin(r) * ny;
  const k = scaleFactor(p.anchorX, p.anchorY); // ground metres -> grid metres
  const s = p.unitsToMeters * k;
  return features.map(f => ({
    layer: f.layer, closed: f.closed, point: !!f.point,
    points: f.points.map(([u, v]) => {
      const du = (u - p.originX) * s, dv = (v - p.originY) * s;
      return { x: p.anchorX + du * xx + dv * yx, y: p.anchorY + du * xy + dv * yy };
    })
  }));
}
