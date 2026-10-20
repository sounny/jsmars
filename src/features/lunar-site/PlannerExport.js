/**
 * @module lunar-site/PlannerExport
 * @description Terrain/plan exports (OBJ, GeoTIFF, DXF, CSV, plan JSON) and provenance.
 */
import { el, toLL } from './LunarSitePlanner.js';
import { RasterGrid, loadGeoTiffLib, xyToLatLon, scaleFactor } from './PolarGrid.js';
import { localFrame, cropGrid, terrainToOBJ, gridToGeoTIFF, featuresToDXF, toCSV, download } from './Exporters.js';

/** Barker et al. (2021) 87°S–90°S 5 m mosaic, hosted by USGS with CORS. */
export const LIVE_5M = {
  url: 'https://asc-pds-services.s3.us-west-2.amazonaws.com/mosaic/Lunar_safed/LMAP/SouthPolar/ldem_87s_5mpp_cog.tif',
  geo: [-100000, 100000, 5], size: 40000, maxCells: 2000,
  citation: 'Barker, M.K., et al. (2021), Planet. Space Sci. 203, 105119, doi:10.1016/j.pss.2020.105119 (LOLA 87S 5 m mosaic, USGS-hosted COG)'
};

/** Read a window of the live 5 m COG. Returns a RasterGrid. */
export async function readLive5m(xmin, ymin, xmax, ymax) {
  const [gx0, gy0, res] = LIVE_5M.geo;
  const c0 = Math.max(0, Math.floor((xmin - gx0) / res)), c1 = Math.min(LIVE_5M.size, Math.ceil((xmax - gx0) / res));
  const r0 = Math.max(0, Math.floor((gy0 - ymax) / res)), r1 = Math.min(LIVE_5M.size, Math.ceil((gy0 - ymin) / res));
  if (c1 - c0 < 2 || r1 - r0 < 2) throw new Error('Window is outside the 5 m mosaic (about 87.1°S–90°S, ±100 km).');
  if (c1 - c0 > LIVE_5M.maxCells || r1 - r0 > LIVE_5M.maxCells) throw new Error(`Window too large for 5 m (max ${LIVE_5M.maxCells * 5 / 1000} km per side).`);
  const GeoTIFF = await loadGeoTiffLib();
  const tiff = await GeoTIFF.fromUrl(LIVE_5M.url, { allowFullFile: false });
  const img = await tiff.getImage();
  const rawNodata = typeof img.getGDALNoData === 'function' ? img.getGDALNoData() : null;
  const nodata = rawNodata != null ? parseFloat(rawNodata) : -3.4028234663852886e+38;
  const [band] = await img.readRasters({ window: [c0, r0, c1, r1] });
  return new RasterGrid(band, c1 - c0, r1 - r0, [gx0 + c0 * res, gy0 - r0 * res, res], {
    name: 'elev5m',
    nodata: Number.isFinite(nodata) ? nodata : null,
    decode: v => (v <= -1e30 || v === -32768 ? NaN : v)
  });
}

export function attachExport(P) {
  const winLayer = L.layerGroup().addTo(P.plan);
  P.state.window = null;
  let firstCorner = null;
  const winInfo = el('p', { class: 'lsp-muted' });
  const updateInfo = () => {
    const w = P.state.window;
    winInfo.textContent = w
      ? `Window ${((w.xmax - w.xmin) / 1000).toFixed(2)} × ${((w.ymax - w.ymin) / 1000).toFixed(2)} km (grid).`
      : 'No window: exports use the whole site. Choose Export window and click two corners to set one.';
  };
  P.toolHooks.window = (p) => {
    if (!firstCorner) { firstCorner = p; winInfo.textContent = 'Click the opposite corner.'; return; }
    const w = { xmin: Math.min(firstCorner.x, p.x), xmax: Math.max(firstCorner.x, p.x), ymin: Math.min(firstCorner.y, p.y), ymax: Math.max(firstCorner.y, p.y) };
    firstCorner = null;
    P.state.window = w;
    P.lastFrame = null;
    winLayer.clearLayers();
    L.rectangle([toLL(w.xmin, w.ymin), toLL(w.xmax, w.ymax)], { color: '#fff', weight: 1.5, dashArray: '3 3', fill: false }).addTo(winLayer);
    updateInfo();
  };
  document.addEventListener('jmars:lunar-site-changed', () => {
    P.lastFrame = null;
  });
  const src = el('select', { 'aria-label': 'Terrain source' },
    el('option', { value: 'site', text: 'Site DEM, 20 m (bundled)' }), el('option', { value: 'live', text: 'LOLA 5 m mosaic, live (87°S–90°S)' }));
  const stride = el('select', { 'aria-label': 'Mesh decimation' }, ...[1, 2, 4, 8].map(s => el('option', { value: String(s), text: s === 1 ? 'full' : `every ${s === 2 ? '2nd' : s + 'th'} cell` })));
  const yUp = el('input', { type: 'checkbox', 'aria-label': 'Y-up axis for OBJ' });
  const out = el('p', { class: 'lsp-status-inline', role: 'status' });

  const windowBounds = () => {
    if (P.state.window) return P.state.window;
    const g = P.grids.elev;
    if (!g) throw new Error('Select a site or draw an export window first.');
    return g.bounds;
  };
  const getTerrain = async () => {
    const w = windowBounds();
    if (src.value === 'live') {
      out.textContent = 'Reading 5 m LOLA tiles…';
      const g = await readLive5m(w.xmin, w.ymin, w.xmax, w.ymax);
      return { win: { data: g.toFloat(), width: g.width, height: g.height, geo: [g.x0, g.y0, g.res] }, citation: LIVE_5M.citation, tag: '5m' };
    }
    if (!P.grids.elev) throw new Error('Select a site first.');
    return { win: cropGrid(P.grids.elev, w.xmin, w.ymin, w.xmax, w.ymax), citation: P.site.terrain.citation, tag: '20m' };
  };
  const baseName = () => `jsmars_${P.site ? P.site.id : 'south_pole'}`;
  const frameFor = (win) => {
    const [x0, y0, res] = win.geo, cx = x0 + (win.width * res) / 2, cy = y0 - (win.height * res) / 2;
    return localFrame(cx, cy, P.elevAt(cx, cy) || 0);
  };
  const run = (label, fn) => async () => {
    out.textContent = `${label}…`;
    try { await fn(); out.textContent = `${label}: done.`; } catch (e) { console.error(e); out.textContent = `${label} failed: ${e.message}`; }
  };

  const btn = (t, f) => { const b = el('button', { type: 'button', class: 'tool-btn', text: t }); b.addEventListener('click', run(t, f)); return b; };
  P.lastFrame = null;
  const objBtn = btn('Terrain OBJ', async () => {
    const t = await getTerrain(); const frame = frameFor(t.win); P.lastFrame = frame;
    const obj = terrainToOBJ(t.win, frame, { yUp: yUp.checked, stride: +stride.value,
      header: `JSMARS Lunar Site Planner terrain export\nSource: ${t.citation}\nGenerated ${new Date().toISOString()}` });
    download(obj, `${baseName()}_terrain_${t.tag}.obj`, 'text/plain');
    download(frame.describe() + '\n', `${baseName()}_terrain_${t.tag}_frame.txt`, 'text/plain');
  });
  const tifBtn = btn('Elevation GeoTIFF', async () => {
    const t = await getTerrain();
    download(gridToGeoTIFF(t.win, t.citation), `${baseName()}_elevation_${t.tag}.tif`, 'image/tiff');
  });
  const slopeBtn = btn('Max-slope GeoTIFF (20 m)', async () => {
    if (!P.grids.slopeMax) throw new Error('Select a site first.');
    const w = windowBounds();
    download(gridToGeoTIFF(cropGrid(P.grids.slopeMax, w.xmin, w.ymin, w.xmax, w.ymax), 'max slope deg per 20 m cell from 5 m slope; ' + P.site.terrain.citation),
      `${baseName()}_maxslope_20m.tif`, 'image/tiff');
  });
  const dxfBtn = btn('Site plan DXF (same frame)', async () => {
    const w = windowBounds();
    const cx = (w.xmin + w.xmax) / 2, cy = (w.ymin + w.ymax) / 2;
    const frame = P.lastFrame || localFrame(cx, cy, P.elevAt(cx, cy) || 0);
    const feats = [];
    const z = (p) => ({ ...p, z: P.elevAt(p.x, p.y) });
    (P.cadPlaced || []).forEach(f => feats.push({ layer: 'CAD_' + f.layer, closed: f.closed, points: f.points.map(z) }));
    if (P.state.corridor.length > 1 && P.state.profile) feats.push({ layer: 'CORRIDOR_3D', closed: false, points: P.state.profile.samples.filter(s => Number.isFinite(s.z)).map(s => ({ x: s.x, y: s.y, z: s.z })) });
    const circle = (x, y, r, layer) => ({ layer, closed: true, points: Array.from({ length: 72 }, (_, i) => z({ x: x + r * Math.cos(i * Math.PI / 36), y: y + r * Math.sin(i * Math.PI / 36) })) });
    P.state.pads.forEach((pd, i) => {
      const k = scaleFactor(pd.x, pd.y);
      feats.push(circle(pd.x, pd.y, (pd.diameter / 2) * k, `PAD_${i + 1}`));
      pd.rings.forEach(r => feats.push(circle(pd.x, pd.y, r * k, `BUFFER_${i + 1}_${r}m`)));
    });
    (P.flatResults || []).forEach((f, i) => feats.push(circle(f.x, f.y, f.radius_m, `LEVEL_AREA_${i + 1}`)));
    if (!feats.length) throw new Error('Nothing to export yet (place CAD, pads, corridor or level areas).');
    download(featuresToDXF(feats, frame, frame.describe()), `${baseName()}_siteplan.dxf`, 'application/dxf');
  });
  const csvBtn = btn('Corridor profile CSV', async () => {
    const prof = P.state.profile;
    if (!prof || P.state.corridor.length < 2) throw new Error('Draw a corridor first.');
    const rows = prof.samples.map(s => { const ll = xyToLatLon(s.x, s.y); return [s.d.toFixed(2), ll.lat.toFixed(6), ll.lon.toFixed(6), s.x.toFixed(2), s.y.toFixed(2), Number.isFinite(s.z) ? s.z.toFixed(2) : '', Number.isFinite(s.grade) ? s.grade.toFixed(2) : '', Number.isFinite(s.terrainSlope) ? s.terrainSlope.toFixed(2) : '']; });
    download(toCSV(['ground_distance_m', 'lat', 'lon', 'ps_x_m', 'ps_y_m', 'elevation_m', 'along_track_grade_deg', 'terrain_max_slope_deg'], rows), `${baseName()}_corridor.csv`, 'text/csv');
  });
  const saveBtn = btn('Save plan (JSON)', async () => {
    const plan = { schema: 'jsmars-lunar-site-plan/1', saved: new Date().toISOString(), site: P.site?.id || null,
      thresholds: P.thresholds, corridor: P.state.corridor, pads: P.state.pads, window: P.state.window,
      cad: P.cad ? { name: P.cad.name, anchor: P.cadAnchor, features: P.cad.features } : null,
      geojson: planGeoJSON(P) };
    download(JSON.stringify(plan, null, 1), `${baseName()}_plan.json`, 'application/json');
  });
  const loadInput = el('input', { type: 'file', accept: '.json', 'aria-label': 'Load plan JSON', hidden: '' });
  loadInput.addEventListener('change', run('Load plan', async () => {
    try {
      const file = loadInput.files[0];
      if (!file) return;
      const plan = JSON.parse(await file.text());
      if (plan.schema !== 'jsmars-lunar-site-plan/1') throw new Error('Not a JSMARS lunar site plan');
      if (plan.site !== (P.site?.id || null)) { P.siteSelect.value = plan.site || ''; await P.selectSite(plan.site || ''); }
      P.state.corridor = plan.corridor || []; P.state.pads = plan.pads || []; P.state.window = plan.window || null;
      P.lastFrame = null;
      winLayer.clearLayers();
      if (P.state.window) {
        const w = P.state.window;
        L.rectangle([toLL(w.xmin, w.ymin), toLL(w.xmax, w.ymax)], { color: '#fff', weight: 1.5, dashArray: '3 3', fill: false }).addTo(winLayer);
      }
      if (plan.cad) { P.cad = { name: plan.cad.name, features: plan.cad.features }; P.cadAnchor = plan.cad.anchor; }
      else { P.cad = null; P.cadAnchor = null; }
      P.redrawCorridor(); P.redrawPads(); P.redrawCad(); updateInfo();
    } finally {
      loadInput.value = '';
    }
  }));
  const loadBtn = el('button', { type: 'button', class: 'tool-btn', text: 'Load plan' });
  loadBtn.addEventListener('click', () => loadInput.click());

  P._section('Export (Rhino / Grasshopper / Isaac Sim / GIS)',
    winInfo,
    el('div', { class: 'lsp-row' }, el('label', {}, el('span', { text: 'Terrain source' }), src), el('label', {}, el('span', { text: 'Mesh density' }), stride)),
    el('label', { class: 'lsp-inline' }, yUp, el('span', { text: ' OBJ Y-up (default Z-up, metres)' })),
    el('div', { class: 'lsp-btns' }, objBtn, tifBtn, slopeBtn, dxfBtn, csvBtn, saveBtn, loadBtn, loadInput), out,
    el('p', { class: 'lsp-muted', text: 'OBJ and DXF share one local frame (origin at window centre, +X east, +Y north, +Z up, ground metres) so the terrain and your plan align when both are imported. Export the OBJ first; the DXF then reuses its frame. GeoTIFFs keep the polar stereographic georeference for GIS.' }));
  updateInfo();

  // ---------------- provenance
  const prov = el('div');
  P._section('Data sources & limitations', prov);
  const fill = () => {
    const m = P.manifest; if (!m) return;
    const li = (t) => el('li', { text: t });
    prov.replaceChildren(
      el('p', { text: `Data package built ${m.built}.` }),
      el('ul', {},
        li(`Site terrain: ${m.sites[0]?.terrain.citation || ''} (5 m slope and DEM, block-resampled to 20 m).`),
        li(`Illumination, Earth visibility and permanent shadow: ${m.regional.illumination.citation}.`),
        li(`Regional DEM: ${m.regional.dem.citation}.`),
        li(`Live 5 m terrain: ${LIVE_5M.citation}.`)),
      el('strong', { text: 'Limitations' }), el('ul', {}, ...m.limitations.map(li)));
  };
  const origLoad = P._loadManifest.bind(P);
  P._loadManifest = async () => { await origLoad(); fill(); };
}

/** Plan as GeoJSON in lat/lon (Moon), for GIS. */
export function planGeoJSON(P) {
  const ll = (p) => { const q = xyToLatLon(p.x, p.y); return [q.lon, q.lat]; };
  const features = [];
  if (P.state.corridor.length > 1) features.push({ type: 'Feature', properties: { kind: 'corridor' }, geometry: { type: 'LineString', coordinates: P.state.corridor.map(ll) } });
  P.state.pads.forEach((pd, i) => features.push({ type: 'Feature', properties: { kind: 'pad', index: i + 1, diameter_m: pd.diameter, buffer_radii_m: pd.rings }, geometry: { type: 'Point', coordinates: ll(pd) } }));
  (P.cadPlaced || []).forEach(f => features.push({ type: 'Feature', properties: { kind: 'cad', layer: f.layer },
    geometry: f.point ? { type: 'Point', coordinates: ll(f.points[0]) } : f.closed
      ? { type: 'Polygon', coordinates: [[...f.points.map(ll), ll(f.points[0])]] }
      : { type: 'LineString', coordinates: f.points.map(ll) } }));
  return { type: 'FeatureCollection', crs_note: 'Moon IAU 2000 sphere R=1737.4 km, planetocentric lat/lon', features };
}
