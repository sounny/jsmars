/**
 * @module lunar-site/PlannerTools
 * @description Interactive tools for the Lunar Site Planner: mode bar,
 * flat-area finder, corridor cross-section, landing pad + blast buffers,
 * horizon / Sun-path analysis. CAD import and exports live in PlannerCadExport.js.
 */
import { el, fmt, toLL } from './LunarSitePlanner.js';
import { findFlatAreas, sampleProfile, horizonProfile, sunPathEnvelope, discStats } from './TerrainAnalysis.js';
import { drawProfile, drawHorizon } from './PlannerCharts.js';
import { scaleFactor } from './PolarGrid.js';
import { attachCadExport, segmentDistance } from './PlannerCadExport.js';

export function attachTools(P) {
  // ---------------- mode bar
  const modes = [['inspect', 'Inspect'], ['corridor', 'Corridor'], ['pad', 'Place pad'], ['horizon', 'Horizon'],
    ['cad', 'Place CAD'], ['window', 'Export window']];
  const bar = el('div', { class: 'lsp-modes', role: 'toolbar', 'aria-label': 'Map tools' },
    ...modes.map(([m, t]) => {
      const b = el('button', { type: 'button', class: 'tool-btn', 'data-mode-btn': m, 'aria-pressed': 'false', text: t });
      b.addEventListener('click', () => P.setMode(m));
      return b;
    }));
  P.side.prepend(bar);
  P.setMode('inspect');
  const hooks = {};
  P.toolClick = (p) => hooks[P.mode]?.(p);
  P.toolHooks = hooks;
  P.state = { corridor: [], pads: [], horizon: null };

  // ---------------- flat-area finder
  const flatThr = el('input', { type: 'number', min: '0.5', max: '30', step: '0.5', value: '5', 'aria-label': 'Maximum slope for level ground' });
  const flatN = el('input', { type: 'number', min: '1', max: '20', step: '1', value: '5', 'aria-label': 'Number of candidates' });
  const flatOut = el('div');
  const flatLayer = L.layerGroup().addTo(P.plan);
  const flatBtn = el('button', { type: 'button', class: 'tool-btn', text: 'Find largest level areas' });
  flatBtn.addEventListener('click', () => {
    if (!P.grids.slopeMax) { flatOut.textContent = 'Select a site first.'; return; }
    flatLayer.clearLayers();
    const res = findFlatAreas(P.grids.slopeMax, +flatThr.value, +flatN.value);
    if (!res.length) { flatOut.textContent = `No 20 m cell has max slope ≤ ${flatThr.value}°.`; return; }
    const tbl = el('table', { class: 'lsp-table' }, el('tr', {}, ...['#', 'Diameter', 'Lat, Lon', 'Sun', 'Earth'].map(h => el('th', { text: h }))));
    res.forEach((r, i) => {
      const v = P.valuesAt(r.x, r.y), k = scaleFactor(r.x, r.y);
      const diam = (2 * r.radius_m) / k;
      L.circle(toLL(r.x, r.y), { radius: r.radius_m / 1000, color: '#00e5ff', weight: 2, fillOpacity: 0.08 })
        .bindTooltip(`Level area ${i + 1}: Ø ${diam.toFixed(0)} m`).addTo(flatLayer);
      const row = el('tr', {}, el('td', { text: String(i + 1) }), el('td', { text: `${diam.toFixed(0)} m` }),
        el('td', { text: `${r.lat.toFixed(4)}, ${r.lon.toFixed(4)}` }), el('td', { text: fmt(v.sun * 100, 0, '%') }), el('td', { text: fmt(v.earth * 100, 0, '%') }));
      row.tabIndex = 0;
      const focusCandidate = () => P.map.setView(toLL(r.x, r.y), Math.max(P.map.getZoom(), 4));
      row.addEventListener('click', focusCandidate);
      row.addEventListener('keydown', (ev) => {
        if (ev.key === 'Enter' || ev.key === ' ') { ev.preventDefault(); focusCandidate(); }
      });
      tbl.append(row);
    });
    flatOut.replaceChildren(tbl, el('p', { class: 'lsp-muted', text: 'Largest discs whose every 20 m cell has a 5 m-derived max slope at or below the threshold (exact Euclidean distance transform). Accuracy ±20 m; LOLA slope RMS error ≈1.5–2.5° (Barker et al. 2021).' }));
    P.flatResults = res;
  });
  document.addEventListener('jmars:lunar-site-changed', () => {
    flatLayer.clearLayers();
    flatOut.replaceChildren();
    P.flatResults = [];
    if (P.state.corridor.length > 1) P.redrawCorridor?.();
    if (P.state.pads.length) P.redrawPads?.();
  });
  P._section('Flat-area finder (landing pad)',
    el('div', { class: 'lsp-row' }, el('label', {}, el('span', { text: 'Max slope (°)' }), flatThr), el('label', {}, el('span', { text: 'Candidates' }), flatN)),
    flatBtn, flatOut);

  // ---------------- corridor
  const corrLayer = L.layerGroup().addTo(P.plan);
  const corrCanvas = el('canvas', { class: 'lsp-chart', role: 'img', 'aria-label': 'Corridor elevation profile' });
  const corrStats = el('div');
  const redrawCorridor = () => {
    corrLayer.clearLayers();
    const pts = P.state.corridor;
    pts.forEach(p => L.circleMarker(toLL(p.x, p.y), { radius: 4, color: '#fff', weight: 2 }).addTo(corrLayer));
    if (pts.length < 2) { corrStats.textContent = pts.length ? 'Click the next corridor point.' : 'Choose Corridor mode, then click points from pad to habitat.'; corrCanvas.getContext('2d').clearRect(0, 0, corrCanvas.width, corrCanvas.height); return; }
    L.polyline(pts.map(p => toLL(p.x, p.y)), { color: '#fff', weight: 2, dashArray: '4 4' }).addTo(corrLayer);
    const step = P.grids.elev ? P.grids.elev.res : 120;
    const prof = sampleProfile(pts, (x, y) => P.elevAt(x, y), step, P.grids.slopeMax ? (x, y) => P.grids.slopeMax.sample(x, y) : null);
    // Ground distances: divide grid distances by local scale factor.
    const k = scaleFactor(pts[0].x, pts[0].y);
    prof.samples.forEach(s => { s.d /= k; });
    prof.length /= k;
    P.state.profile = prof;
    const { t1, t2 } = P.thresholds;
    // recompute grades with ground distances
    let maxGrade = 0, over = 0;
    for (let i = 1; i < prof.samples.length; i++) {
      const a = prof.samples[i - 1], b = prof.samples[i];
      const dd = b.d - a.d;
      b.grade = (Number.isFinite(b.z - a.z) && dd > 0) ? (Math.atan2(Math.abs(b.z - a.z), dd) * 180) / Math.PI : NaN;
      if (b.grade > maxGrade) maxGrade = b.grade;
      if (b.grade > t2 && dd > 0) over += dd;
    }
    prof.maxGrade = maxGrade;
    drawProfile(corrCanvas, prof, t1, t2);
    corrStats.replaceChildren(el('dl', { class: 'lsp-readout' },
      ...[['Length', `${(prof.length / 1000).toFixed(3)} km`], ['Climb / descent', `${prof.climb.toFixed(0)} m / ${prof.descent.toFixed(0)} m`],
        ['Max along-track grade', fmt(prof.maxGrade, 1, '°')], ['Max terrain slope crossed', fmt(prof.maxTerrainSlope, 1, '°')],
        [`Length steeper than ${t2}°`, `${over.toFixed(0)} m`]].flatMap(([a, b]) => [el('dt', { text: a }), el('dd', { text: b })])));
  };
  hooks.corridor = (p) => { P.state.corridor.push(p); redrawCorridor(); };
  P.onThresholds = () => { if (P.state.corridor.length > 1) redrawCorridor(); };
  const undo = el('button', { type: 'button', class: 'tool-btn', text: 'Undo point' });
  undo.addEventListener('click', () => { P.state.corridor.pop(); redrawCorridor(); });
  const clear = el('button', { type: 'button', class: 'tool-btn', text: 'Clear' });
  clear.addEventListener('click', () => { P.state.corridor = []; redrawCorridor(); });
  P._section('Corridor cross-section', el('div', { class: 'lsp-row' }, undo, clear), corrCanvas, corrStats,
    el('p', { class: 'lsp-muted', text: 'Profile sampled from the 20 m site DEM (120 m regional DEM outside it). Grade colours use your slope thresholds.' }));
  P.redrawCorridor = redrawCorridor;
  redrawCorridor();

  // ---------------- pads & blast buffers
  const padDiam = el('input', { type: 'number', min: '1', step: '1', value: '50', 'aria-label': 'Pad diameter in metres' });
  const rings = el('input', { type: 'text', value: '', placeholder: 'e.g. 100, 500, 1500', 'aria-label': 'Buffer ring radii in metres' });
  const padOut = el('div');
  const padLayer = L.layerGroup().addTo(P.plan);
  const parseRings = () => rings.value.split(/[,\s]+/).map(Number).filter(v => v > 0).sort((a, b) => a - b);
  const redrawPads = () => {
    padLayer.clearLayers();
    padOut.replaceChildren();
    P.state.pads.forEach((pad, i) => {
      const k = scaleFactor(pad.x, pad.y);
      L.circle(toLL(pad.x, pad.y), { radius: (pad.diameter / 2) * k / 1000, color: '#ff9800', weight: 2, fillOpacity: 0.25 }).addTo(padLayer);
      pad.rings.forEach(r => L.circle(toLL(pad.x, pad.y), { radius: (r * k) / 1000, color: '#ff9800', weight: 1.5, dashArray: '6 4', fill: false })
        .bindTooltip(`Buffer ${r} m`).addTo(padLayer));
      const padSlope = P.grids.slopeMax ? discStats(P.grids.slopeMax, pad.x, pad.y, (pad.diameter / 2) * k) : { n: 0 };
      const padElev = P.grids.elev ? discStats(P.grids.elev, pad.x, pad.y, (pad.diameter / 2) * k) : { n: 0 };
      const ringRows = pad.rings.map(r => {
        const R = r * k;
        const hits = (P.cadPlaced || []).filter(f => f.points.some((pt, j, pts) =>
          Math.hypot(pt.x - pad.x, pt.y - pad.y) <= R ||
          (j > 0 && segmentDistance(pad, pts[j - 1], pt) <= R) ||
          (f.closed && pts.length > 2 && j === pts.length - 1 && segmentDistance(pad, pt, pts[0]) <= R)));
        const corr = P.state.corridor.length > 1 && P.state.corridor.some((pt, j) => j > 0 && segmentDistance(pad, P.state.corridor[j - 1], pt) <= R);
        return el('li', { text: `${r} m: ${hits.length} CAD feature(s)${hits.length ? ' [' + [...new Set(hits.map(h => h.layer))].join(', ') + ']' : ''}${corr ? '; corridor crosses' : ''}` });
      });
      const del = el('button', { type: 'button', class: 'tool-btn', text: 'Remove' });
      del.addEventListener('click', () => { P.state.pads.splice(i, 1); redrawPads(); });
      padOut.append(el('div', { class: 'lsp-card' },
        el('strong', { text: `Pad ${i + 1} (Ø ${pad.diameter} m)` }),
        el('p', { text: `Max 5 m slope in pad: ${fmt(padSlope.max, 1, '°')} (p95 ${fmt(padSlope.p95, 1, '°')}); relief ${padElev.n ? (padElev.max - padElev.min).toFixed(1) + ' m' : '—'}; sun ${fmt(P.valuesAt(pad.x, pad.y).sun * 100, 0, '%')}` }),
        pad.rings.length ? el('ul', {}, ...ringRows) : el('p', { class: 'lsp-muted', text: 'No buffer radii set.' }), del));
    });
    P.onPadsChanged?.();
  };
  hooks.pad = (p) => { P.state.pads.push({ x: p.x, y: p.y, diameter: +padDiam.value || 50, rings: parseRings() }); redrawPads(); };
  rings.addEventListener('change', () => { P.state.pads.forEach(pd => { pd.rings = parseRings(); }); redrawPads(); });
  P.redrawPads = redrawPads;
  P._section('Landing pad & blast safety buffers',
    el('div', { class: 'lsp-row' }, el('label', {}, el('span', { text: 'Pad Ø (m)' }), padDiam), el('label', {}, el('span', { text: 'Buffer radii (m)' }), rings)),
    el('p', { class: 'lsp-muted', text: 'Enter keep-out radii from your lander\'s plume-surface-interaction requirement. No default radii are supplied: there is no single published standard, so values must come from the vehicle provider or your program. Buffers report CAD features and corridor segments inside each ring.' }),
    padOut);

  // ---------------- horizon & Sun
  const hHeight = el('input', { type: 'number', min: '0', max: '200', step: '0.5', value: '2', 'aria-label': 'Observer height above ground in metres' });
  const hCanvas = el('canvas', { class: 'lsp-chart', role: 'img', 'aria-label': 'Horizon and Sun path chart' });
  const hOut = el('div');
  hooks.horizon = (p) => {
    hOut.textContent = 'Computing horizon…';
    setTimeout(() => {
      const hz = horizonProfile(p.x, p.y, (x, y) => P.elevAt(x, y), { observerHeight: +hHeight.value, azStep: 2, maxDist: 150000 });
      if (!Number.isFinite(hz.ground)) {
        hOut.textContent = 'No elevation data at clicked point.';
        return;
      }
      const env = sunPathEnvelope(hz.lat, hz.lon, 2);
      P.state.horizon = { p, hz, env };
      drawHorizon(hCanvas, hz, env);
      const blocked = env.filter((e, i) => hz.profile[i] && hz.profile[i].elev >= e.max).length;
      const clear = env.filter((e, i) => hz.profile[i] && hz.profile[i].elev < e.min).length;
      hOut.replaceChildren(el('p', { text: `${hz.lat.toFixed(4)}°, ${hz.lon.toFixed(4)}°, ground ${hz.ground.toFixed(0)} m, eye +${hHeight.value} m. Of 180 azimuth bins, the Sun is always above the terrain horizon in ${clear} and always below it in ${blocked}.` }),
        el('p', { class: 'lsp-muted', text: 'Yellow band: every Sun elevation geometrically possible at this site (subsolar latitude ±1.543°). Grey: terrain horizon from LOLA DEMs (20 m site, 120 m regional to 150 km) with lunar curvature. This is a geometric horizon analysis, not a time-series illumination simulation; for % lit use the Mazarico solar-visibility layer.' }));
      if (P.horizonMarker) P.plan.removeLayer(P.horizonMarker);
      P.horizonMarker = L.circleMarker(toLL(p.x, p.y), { radius: 6, color: '#fdd835', weight: 2 }).addTo(P.plan);
    }, 20);
  };
  P._section('Horizon & Sun path (solar orientation)',
    el('label', {}, el('span', { text: 'Observer / mast height (m)' }), hHeight),
    el('p', { class: 'lsp-muted', text: 'Choose Horizon mode and click a point (e.g. a solar mast location).' }), hCanvas, hOut);

  attachCadExport(P);
}
