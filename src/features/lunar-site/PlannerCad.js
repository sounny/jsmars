/**
 * @module lunar-site/PlannerCad
 * @description DXF/SVG site-plan import, placement and per-polygon terrain checks.
 */
import { el, fmt, toLL } from './LunarSitePlanner.js';
import { parseDXF, parseSVG, placeFeatures, featureBounds, UNIT_OPTIONS } from './CadImport.js';
import { scaleFactor } from './PolarGrid.js';

/** Distance from point p to segment ab (map coords). */
export function segmentDistance(p, a, b) {
  const dx = b.x - a.x, dy = b.y - a.y, L2 = dx * dx + dy * dy;
  const t = L2 ? Math.max(0, Math.min(1, ((p.x - a.x) * dx + (p.y - a.y) * dy) / L2)) : 0;
  return Math.hypot(p.x - (a.x + t * dx), p.y - (a.y + t * dy));
}

function pointInPolygon(x, y, pts) {
  let inside = false;
  for (let i = 0, j = pts.length - 1; i < pts.length; j = i++) {
    const a = pts[i], b = pts[j];
    if ((a.y > y) !== (b.y > y) && x < ((b.x - a.x) * (y - a.y)) / (b.y - a.y) + a.x) inside = !inside;
  }
  return inside;
}

/** Stats of a grid over cells whose centres fall inside a polygon. */
export function polygonStats(grid, pts) {
  if (!grid) return { n: 0 };
  const xs = pts.map(p => p.x), ys = pts.map(p => p.y);
  const c0 = Math.floor((Math.min(...xs) - grid.x0) / grid.res), c1 = Math.ceil((Math.max(...xs) - grid.x0) / grid.res);
  const r0 = Math.floor((grid.y0 - Math.max(...ys)) / grid.res), r1 = Math.ceil((grid.y0 - Math.min(...ys)) / grid.res);
  let n = 0, sum = 0, max = -Infinity, min = Infinity;
  for (let r = r0; r <= r1; r++) for (let c = c0; c <= c1; c++) {
    const x = grid.x0 + (c + 0.5) * grid.res, y = grid.y0 - (r + 0.5) * grid.res;
    if (!pointInPolygon(x, y, pts)) continue;
    const v = grid.cell(c, r);
    if (!Number.isFinite(v)) continue;
    n++; sum += v; if (v > max) max = v; if (v < min) min = v;
  }
  if (!n) { // polygon smaller than a cell: sample its centroid
    const cx = xs.reduce((a, b) => a + b, 0) / xs.length, cy = ys.reduce((a, b) => a + b, 0) / ys.length;
    const v = grid.sample(cx, cy);
    return Number.isFinite(v) ? { n: 0, min: v, max: v, mean: v, centroidOnly: true } : { n: 0 };
  }
  return { n, min, max, mean: sum / n };
}

/** Shoelace area in grid m², converted to ground m² with scale factor k. */
export function polygonArea(pts) {
  if (!pts || pts.length < 3) return 0;
  let a = 0;
  for (let i = 0, j = pts.length - 1; i < pts.length; j = i++) a += (pts[j].x + pts[i].x) * (pts[j].y - pts[i].y);
  const k = scaleFactor(pts[0].x, pts[0].y);
  return Math.abs(a / 2) / (k * k);
}

export function attachCad(P) {
  const file = el('input', { type: 'file', accept: '.dxf,.svg', 'aria-label': 'Choose DXF or SVG site plan' });
  const unitSel = el('select', { 'aria-label': 'Drawing unit' }, ...Object.keys(UNIT_OPTIONS).map(u => el('option', { value: u, text: u })));
  unitSel.value = 'm';
  const scale = el('input', { type: 'number', step: 'any', min: '0', value: '1', 'aria-label': 'Drawing scale multiplier' });
  const rot = el('input', { type: 'number', step: '0.5', value: '0', 'aria-label': 'Rotation in degrees clockwise from local north' });
  const originSel = el('select', { 'aria-label': 'Drawing origin' },
    el('option', { value: 'origin', text: 'Drawing origin (0,0)' }), el('option', { value: 'center', text: 'Drawing bounding-box centre' }));
  const info = el('div', { class: 'lsp-muted' });
  const layersBox = el('div', { class: 'lsp-layers' });
  const report = el('div');
  const cadLayer = L.layerGroup().addTo(P.plan);
  P.cad = null; P.cadPlaced = []; P.cadAnchor = null;
  const hidden = new Set();

  const metresPerUnit = () => (P.cad?.unitsFromFile && unitSel.value === 'file' ? P.cad.unitsFromFile : UNIT_OPTIONS[unitSel.value]) * (+scale.value || 1);

  const redraw = () => {
    cadLayer.clearLayers();
    report.replaceChildren();
    if (!P.cad || !P.cadAnchor) { P.cadPlaced = []; P.redrawPads?.(); return; }
    const b = featureBounds(P.cad.features);
    const origin = originSel.value === 'center' ? { x: (b.xmin + b.xmax) / 2, y: (b.ymin + b.ymax) / 2 } : { x: 0, y: 0 };
    const feats = P.cad.features.filter(f => !hidden.has(f.layer));
    P.cadPlaced = placeFeatures(feats, {
      anchorX: P.cadAnchor.x, anchorY: P.cadAnchor.y, originX: origin.x, originY: origin.y,
      unitsToMeters: metresPerUnit(), rotationDeg: +rot.value || 0
    });
    const { t2 } = P.thresholds;
    const rows = [];
    P.cadPlaced.forEach((f, i) => {
      const ll = f.points.map(p => toLL(p.x, p.y));
      if (f.point) { L.circleMarker(ll[0], { radius: 3, color: '#e040fb' }).addTo(cadLayer); return; }
      const shape = f.closed ? L.polygon(ll, { color: '#e040fb', weight: 1.5, fillOpacity: 0.15 }) : L.polyline(ll, { color: '#e040fb', weight: 1.5 });
      shape.addTo(cadLayer);
      if (f.closed && f.points.length >= 3) {
        const s = polygonStats(P.grids.slopeMax, f.points), sun = polygonStats(P.grids.sun || P.grids.regSun, f.points);
        const psr = polygonStats(P.grids.psr || P.grids.regPsr, f.points);
        const area = polygonArea(f.points);
        const flag = s.max > t2;
        if (flag) shape.setStyle({ color: '#d7191c', weight: 2.5 });
        shape.bindTooltip(`${f.layer}: ${area.toFixed(0)} m², max slope ${fmt(s.max, 1, '°')}`);
        rows.push(el('tr', { class: flag ? 'lsp-flag' : '' },
          el('td', { text: f.layer }), el('td', { text: area.toFixed(0) }),
          el('td', { text: fmt(s.max, 1) + (s.centroidOnly ? '*' : '') }), el('td', { text: fmt(s.mean, 1) }),
          el('td', { text: fmt(sun.min * 100, 0) }), el('td', { text: Number.isFinite(psr.max) ? (psr.max > 0.5 ? 'yes' : 'no') : '—' })));
      }
    });
    if (rows.length) {
      report.append(el('table', { class: 'lsp-table' },
        el('tr', {}, ...['Layer', 'Area m²', 'Max slope°', 'Mean°', 'Min sun %', 'PSR'].map(h => el('th', { text: h }))), ...rows),
        el('p', { class: 'lsp-muted', text: `Closed polygons only. Red rows/outlines exceed your ${t2}° limit. * polygon smaller than a 20 m cell (centroid value). Sun/PSR at 60 m.` }));
    }
    P.redrawPads?.();
    document.dispatchEvent(new CustomEvent('jmars:lunar-site-cad-placed', { detail: { features: P.cadPlaced.length } }));
  };
  P.redrawCad = redraw;

  file.addEventListener('change', async () => {
    const f = file.files[0];
    if (!f) return;
    try {
      const text = await f.text();
      const ext = f.name.split('.').pop().toLowerCase();
      const parsed = ext === 'svg' ? parseSVG(text) : parseDXF(text);
      if (!parsed.features.length) throw new Error('No supported geometry found');
      P.cad = { name: f.name, features: parsed.features, unitsFromFile: parsed.unitsToMeters };
      [...unitSel.querySelectorAll('option[value="file"]')].forEach(o => o.remove());
      if (parsed.unitsToMeters) {
        unitSel.prepend(el('option', { value: 'file', text: `from file (${parsed.unitsToMeters} m/unit)` }));
        unitSel.value = 'file';
      }
      hidden.clear();
      layersBox.replaceChildren(...parsed.layers.map(name => {
        const cb = el('input', { type: 'checkbox', checked: '' });
        cb.addEventListener('change', () => { cb.checked ? hidden.delete(name) : hidden.add(name); redraw(); });
        return el('label', { class: 'lsp-inline' }, cb, el('span', { text: ' ' + name }));
      }));
      const b = featureBounds(parsed.features);
      info.textContent = `${f.name}: ${parsed.features.length} features, ${parsed.layers.length} layers, extent ${(b.xmax - b.xmin).toPrecision(4)} × ${(b.ymax - b.ymin).toPrecision(4)} units.` +
        (parsed.warnings.length ? ' Notes: ' + parsed.warnings.join('; ') : '') + ' Now choose Place CAD and click the anchor point.';
      P.setMode('cad');
      redraw();
    } catch (e) {
      info.textContent = `Import failed: ${e.message}`;
    } finally {
      file.value = '';
    }
  });
  [unitSel, scale, rot, originSel].forEach(i => i.addEventListener('change', redraw));
  P.toolHooks.cad = (p) => {
    if (!P.cad) { info.textContent = 'Import a DXF or SVG first.'; return; }
    P.cadAnchor = p; redraw();
  };
  P._section('Site plan import (DXF / SVG)',
    file,
    el('div', { class: 'lsp-row' }, el('label', {}, el('span', { text: 'Unit' }), unitSel), el('label', {}, el('span', { text: 'Scale ×' }), scale)),
    el('div', { class: 'lsp-row' }, el('label', {}, el('span', { text: 'Rotation (° cw from north)' }), rot), el('label', {}, el('span', { text: 'Anchor' }), originSel)),
    info, layersBox, report,
    el('p', { class: 'lsp-muted', text: 'Drawing +Y is aligned to local north (away from the pole) at the anchor, then rotated. Lengths are true ground metres (stereographic scale factor applied). ASCII DXF from Rhino: File › Export › .dxf.' }));
}
