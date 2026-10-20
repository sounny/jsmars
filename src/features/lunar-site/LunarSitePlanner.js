/**
 * @module lunar-site/LunarSitePlanner
 * @description Lunar South Pole Base Site Planner. Opens a full-screen polar
 * stereographic workspace (independent Leaflet map in CRS.Simple, kilometres)
 * over real NASA LOLA data packaged by scripts/build_lunar_south_pole_assets.py.
 */
import { loadBands, decodeFraction, xyToLatLon, latLonToXY } from './PolarGrid.js';
import { hillshade, roughness, distanceTransform } from './TerrainAnalysis.js';
import { renderGrey, renderSlopeClasses, renderFraction, renderPSR, renderScalar, VIRIDIS_CSS, SLOPE_COLORS } from './PlannerRenderers.js';
import { attachTools } from './PlannerTools.js';

const BASE = './assets/lunar-south-pole/';
export const toLL = (x, y) => L.latLng(y / 1000, x / 1000);
export const fromLL = (ll) => ({ x: ll.lng * 1000, y: ll.lat * 1000 });

function el(tag, attrs = {}, ...kids) {
  const e = document.createElement(tag);
  for (const [k, v] of Object.entries(attrs)) {
    if (k === 'class') e.className = v;
    else if (k === 'text') e.textContent = v;
    else if (k.startsWith('on')) e.addEventListener(k.slice(2), v);
    else e.setAttribute(k, v);
  }
  kids.flat().forEach(k => e.append(k));
  return e;
}
export { el };

const fmt = (v, d = 1, u = '') => (Number.isFinite(v) ? `${v.toFixed(d)}${u}` : '—');
export { fmt };

export class LunarSitePlanner {
  /**
   * @param {HTMLElement} container sidebar panel container
   */
  constructor(container) {
    this.container = container;
    this.manifest = null;
    this.site = null;          // manifest site entry
    this.grids = {};           // elev, slopeMax, slopeMean, sun, earth, psr (site); reg* (regional)
    this.layers = {};
    this.mode = 'inspect';
    this.thresholds = { t1: 10, t2: 20 };
    this.thematic = 'slope';
    this._renderPanel();
  }

  _renderPanel() {
    const btn = el('button', { class: 'tool-btn', type: 'button', text: 'Open Site Planner' });
    btn.addEventListener('click', () => this.open());
    this.container.append(
      el('p', { class: 'lsp-note', text: 'Plan a base at the lunar south pole on real LOLA data: illumination, Earth visibility, permanent shadow, slope hazards, flat-area finder, corridor profiles, blast buffers, DXF/SVG site plans, and OBJ / GeoTIFF / DXF export for Rhino and Isaac Sim.' }),
      btn,
      el('p', { class: 'lsp-note lsp-muted', text: 'Data: NASA LRO LOLA (Barker et al. 2021; Mazarico et al. 2011). Opens in its own polar view.' })
    );
  }

  async open() {
    this._lastFocus = document.activeElement;
    if (!this.root) this._buildWorkspace();
    this.root.hidden = false;
    document.body.classList.add('lsp-open');
    document.dispatchEvent(new CustomEvent('jmars:lunar-site-opened'));
    this.closeBtn.focus();
    setTimeout(() => this.map.invalidateSize(), 50);
    if (!this.manifest) await this._loadManifest();
  }

  close() {
    this.root.hidden = true;
    document.body.classList.remove('lsp-open');
    document.dispatchEvent(new CustomEvent('jmars:lunar-site-closed'));
    this._lastFocus?.focus?.();
  }

  _buildWorkspace() {
    this.status = el('div', { class: 'lsp-status', role: 'status', 'aria-live': 'polite' });
    this.siteSelect = el('select', { 'aria-label': 'Site', class: 'lsp-select' });
    this.siteSelect.addEventListener('change', () => this.selectSite(this.siteSelect.value));
    this.closeBtn = el('button', { class: 'lsp-close', type: 'button', 'aria-label': 'Close site planner', text: '×' });
    this.closeBtn.addEventListener('click', () => this.close());
    this.mapEl = el('div', { class: 'lsp-map', role: 'application', 'aria-label': 'Polar stereographic site map' });
    this.side = el('div', { class: 'lsp-side' });
    this.root = el('div', { class: 'lsp-workspace', role: 'dialog', 'aria-modal': 'true', 'aria-label': 'Lunar Base Site Planner' },
      el('header', { class: 'lsp-header' },
        el('strong', { text: 'Lunar Base Site Planner' }),
        el('label', { class: 'lsp-inline' }, el('span', { text: 'Site ' }), this.siteSelect),
        this.status, this.closeBtn),
      el('div', { class: 'lsp-body' }, this.mapEl, this.side));
    this.root.addEventListener('keydown', (e) => { if (e.key === 'Escape') this.close(); });
    document.body.append(this.root);

    this.map = L.map(this.mapEl, { crs: L.CRS.Simple, minZoom: -4, maxZoom: 10, zoomSnap: 0.25, attributionControl: true });
    this.map.attributionControl.setPrefix(false);
    this.map.attributionControl.addAttribution('NASA LRO LOLA / GSFC PGDA');
    this.map.setView([0, 0], -1);
    if ('ResizeObserver' in window) new ResizeObserver(() => { if (!this.root.hidden) this.map.invalidateSize(); }).observe(this.mapEl);
    this.map.on('click', (e) => this._onClick(fromLL(e.latlng)));
    this.map.on('mousemove', (e) => this._onMove(fromLL(e.latlng)));
    this.cursor = el('div', { class: 'lsp-cursor' });
    this.mapEl.append(this.cursor);
    this.plan = L.layerGroup().addTo(this.map);

    this._buildSide();
    attachTools(this);
  }

  _section(title, ...kids) {
    const d = el('details', { class: 'lsp-sec' }, el('summary', { text: title }), ...kids);
    this.side.append(d);
    return d;
  }

  _buildSide() {
    // Layers
    const themSel = el('select', { 'aria-label': 'Thematic layer' },
      ...[['slope', 'Slope hazard (5 m max per 20 m cell)'], ['slopeMean', 'Mean slope'], ['sun', 'Avg solar visibility'],
        ['earth', 'Avg Earth visibility'], ['psr', 'Permanently shadowed regions'], ['rough', 'Roughness proxy (DEM)'],
        ['elev', 'Elevation'], ['none', 'Hillshade only']].map(([v, t]) => el('option', { value: v, text: t })));
    themSel.addEventListener('change', () => { this.thematic = themSel.value; this.renderThematic(); });
    const opacity = el('input', { type: 'range', min: '0', max: '100', value: '65', 'aria-label': 'Layer opacity' });
    opacity.addEventListener('input', () => this.layers.thematic?.setOpacity(opacity.value / 100));
    this.opacity = opacity;
    const t1 = el('input', { type: 'number', min: '0', max: '60', step: '0.5', value: '10', 'aria-label': 'Caution slope threshold' });
    const t2 = el('input', { type: 'number', min: '0', max: '60', step: '0.5', value: '20', 'aria-label': 'Limit slope threshold' });
    const onT = () => { this.thresholds = { t1: +t1.value, t2: Math.max(+t1.value, +t2.value) }; this.renderThematic(); this.onThresholds?.(); };
    t1.addEventListener('change', onT); t2.addEventListener('change', onT);
    this.legend = el('div', { class: 'lsp-legend' });
    const sec = this._section('Layers & slope thresholds',
      el('label', {}, el('span', { text: 'Layer' }), themSel),
      el('label', {}, el('span', { text: 'Opacity' }), opacity),
      el('div', { class: 'lsp-row' },
        el('label', {}, el('span', { text: 'Caution above (°)' }), t1),
        el('label', {}, el('span', { text: 'Limit above (°)' }), t2)),
      this.legend);
    sec.open = true;

    // Inspect
    this.readout = el('dl', { class: 'lsp-readout' });
    this._section('Inspect point', el('p', { class: 'lsp-muted', text: 'Click the map (Inspect mode) for values at a point.' }), this.readout).open = true;
  }

  setStatus(msg, isError = false) {
    this.status.textContent = msg;
    this.status.classList.toggle('lsp-error', isError);
  }

  setMode(mode) {
    this.mode = mode;
    this.mapEl.dataset.mode = mode;
    this.root.querySelectorAll('[data-mode-btn]').forEach(b => b.setAttribute('aria-pressed', String(b.dataset.modeBtn === mode)));
  }

  async _loadManifest() {
    this.setStatus('Loading data manifest…');
    try {
      const r = await fetch(BASE + 'manifest.json');
      if (!r.ok) throw new Error(`HTTP ${r.status}`);
      this.manifest = await r.json();
    } catch (e) {
      this.setStatus(`Could not load site data (${e.message}). Run scripts/build_lunar_south_pole_assets.py.`, true);
      return;
    }
    await this._loadRegional();
    this.siteSelect.replaceChildren(el('option', { value: '', text: 'Regional overview (85°S–90°S)' }),
      ...this.manifest.sites.map(s => el('option', { value: s.id, text: `${s.name} (${s.id})` })));
    this.siteSelect.disabled = false;
    this.selectSite('');
  }

  async _loadRegional() {
    const m = this.manifest.regional;
    this.setStatus('Loading regional LOLA data…');
    const [dem] = await loadBands(BASE + m.dem.file, m.dem.geo, { nodata: m.dem.nodata, names: ['elev'] });
    const ill = await loadBands(BASE + m.illumination.file, m.illumination.geo, { nodata: 255, decode: decodeFraction, names: m.illumination.bands });
    this.grids.regElev = dem;
    [this.grids.regSun, this.grids.regEarth, this.grids.regPsr] = ill;
    // Distance-to-PSR field (120 m), computed once.
    const psr = this.grids.regPsr, mask = new Uint8Array(psr.width * psr.height);
    for (let i = 0; i < mask.length; i++) mask[i] = psr.data[i] !== 255 && psr.data[i] / 200 > 0.5 ? 1 : 0;
    this.grids.regPsrDist = { dist: distanceTransform(mask, psr.width, psr.height), grid: psr };
    const hs = hillshade(dem);
    this.layers.regHill = this._overlay(renderGrey(hs, dem.width, dem.height), dem, 1).addTo(this.map);
    this.siteFootprints = L.layerGroup(this.manifest.sites.map(s => {
      const [xmin, ymin, xmax, ymax] = s.bounds;
      return L.rectangle([toLL(xmin, ymin), toLL(xmax, ymax)], { color: '#fdd835', weight: 1.5, fill: false })
        .bindTooltip(`${s.name} (${s.id})`).on('click', (ev) => { L.DomEvent.stop(ev); this.siteSelect.value = s.id; this.selectSite(s.id); });
    })).addTo(this.map);
    this.setStatus('Regional data loaded.');
  }

  _overlay(canvas, grid, opacity) {
    const b = grid.bounds;
    return L.imageOverlay(canvas.toDataURL('image/png'), [toLL(b.xmin, b.ymin), toLL(b.xmax, b.ymax)], { opacity, interactive: false, className: 'lsp-pixelated' });
  }

  async selectSite(id) {
    this._loadToken = (this._loadToken || 0) + 1;
    const token = this._loadToken;
    this.site = this.manifest.sites.find(s => s.id === id) || null;
    ['hill', 'thematic'].forEach(k => { if (this.layers[k]) { this.map.removeLayer(this.layers[k]); this.layers[k] = null; } });
    ['elev', 'slopeMax', 'slopeMean', 'sun', 'earth', 'psr', 'rough'].forEach(k => delete this.grids[k]);
    if (!this.site) {
      const b = this.grids.regElev?.bounds;
      if (b) this.map.fitBounds([toLL(b.xmin, b.ymin), toLL(b.xmax, b.ymax)]);
      this.renderThematic();
      document.dispatchEvent(new CustomEvent('jmars:lunar-site-changed', { detail: { site: null } }));
      return;
    }
    const s = this.site;
    this.setStatus(`Loading ${s.name}…`);
    try {
      const sc = s.terrain.scale ?? 1;
      const t = await loadBands(BASE + s.terrain.file, s.terrain.geo, { nodata: s.terrain.nodata, decode: v => v * sc, names: s.terrain.bands });
      if (token !== this._loadToken) return;
      // Materialise decoded float grids for fast analysis.
      const toFloat = (g) => Object.assign(Object.create(Object.getPrototypeOf(g)), g, { data: g.toFloat(), decode: null, nodata: null });
      [this.grids.elev, this.grids.slopeMax, this.grids.slopeMean] = t.map(toFloat);
      const ill = await loadBands(BASE + s.illumination.file, s.illumination.geo, { nodata: 255, decode: decodeFraction, names: s.illumination.bands });
      if (token !== this._loadToken) return;
      [this.grids.sun, this.grids.earth, this.grids.psr] = ill;
      const e = this.grids.elev;
      if (!e || !e.width || !e.height) {
        throw new Error('Invalid elevation grid returned from terrain raster');
      }
      this.layers.hill = this._overlay(renderGrey(hillshade(e), e.width, e.height), e, 1).addTo(this.map);
      this.grids.rough = roughness(e);
      const b = e.bounds;
      this.map.fitBounds([toLL(b.xmin, b.ymin), toLL(b.xmax, b.ymax)]);
      this.renderThematic();
      this.setStatus(`${s.name}: ${e.width}×${e.height} cells at ${e.res} m.`);
      document.dispatchEvent(new CustomEvent('jmars:lunar-site-changed', { detail: { site: s.id } }));
    } catch (err) {
      if (token === this._loadToken) {
        console.error(err);
        this.setStatus(`Failed to load ${s.name}: ${err.message}`, true);
      }
    }
  }

  renderThematic() {
    if (this.layers.thematic) { this.map.removeLayer(this.layers.thematic); this.layers.thematic = null; }
    const g = this.grids, site = !!this.site, th = this.thematic;
    const { t1, t2 } = this.thresholds;
    let canvas = null, grid = null, legend = '';
    if (th === 'slope' && site) { grid = g.slopeMax; canvas = renderSlopeClasses(grid, t1, t2); }
    else if (th === 'slopeMean' && site) { grid = g.slopeMean; canvas = renderSlopeClasses(grid, t1, t2); }
    else if (th === 'sun') { grid = site ? g.sun : g.regSun; canvas = renderFraction(grid); }
    else if (th === 'earth') { grid = site ? g.earth : g.regEarth; canvas = renderFraction(grid); }
    else if (th === 'psr') { grid = site ? g.psr : g.regPsr; canvas = renderPSR(grid); }
    else if (th === 'rough' && site) { grid = g.elev; canvas = renderScalar(g.rough, grid.width, grid.height, 0, 2); }
    else if (th === 'elev') {
      grid = site ? g.elev : g.regElev;
      const vals = grid.toFloat ? grid.toFloat() : grid.data;
      let mn = Infinity, mx = -Infinity; for (const v of vals) if (Number.isFinite(v)) { mn = Math.min(mn, v); mx = Math.max(mx, v); }
      canvas = renderScalar(vals, grid.width, grid.height, mn, mx); legend = `${Math.round(mn)} m → ${Math.round(mx)} m`;
    }
    if (canvas) this.layers.thematic = this._overlay(canvas, grid, this.opacity.value / 100).addTo(this.map);
    this._renderLegend(th, legend, site);
  }

  _renderLegend(th, extra, site) {
    const L_ = this.legend; L_.replaceChildren();
    const sw = (c, t) => el('span', { class: 'lsp-sw' }, el('i', { style: `background:rgb(${c.join(',')})` }), t);
    const { t1, t2 } = this.thresholds;
    if ((th === 'slope' || th === 'slopeMean' || th === 'rough') && !site) {
      L_.append(el('p', { class: 'lsp-muted', text: 'Select a site to view 20 m terrain layers.' })); return;
    }
    if (th === 'slope' || th === 'slopeMean') {
      L_.append(sw(SLOPE_COLORS[0], `≤ ${t1}°`), sw(SLOPE_COLORS[1], `${t1}–${t2}°`), sw(SLOPE_COLORS[2], `> ${t2}°`));
    } else if (th === 'sun' || th === 'earth') {
      L_.append(el('div', { class: 'lsp-ramp', style: `background:${VIRIDIS_CSS}` }), el('span', { class: 'lsp-muted', text: '0 % → 100 % of time (18.6-yr average, at surface)' }));
    } else if (th === 'psr') {
      L_.append(sw([0, 191, 255], 'Permanent shadow (Mazarico et al. 2011)'));
    } else if (th === 'rough') {
      L_.append(el('div', { class: 'lsp-ramp', style: `background:${VIRIDIS_CSS}` }), el('span', { class: 'lsp-muted', text: '0 → 2 m RMS residual from 3×3 plane (60 m window). Proxy only, not boulder density.' }));
    } else if (th === 'elev') {
      L_.append(el('div', { class: 'lsp-ramp', style: `background:${VIRIDIS_CSS}` }), el('span', { class: 'lsp-muted', text: extra + ' (rel. 1737.4 km sphere)' }));
    }
  }

  /** Best available elevation (site 20 m, else regional 120 m). */
  elevAt(x, y) {
    const v = this.grids.elev ? this.grids.elev.bilinear(x, y) : NaN;
    return Number.isFinite(v) ? v : (this.grids.regElev ? this.grids.regElev.bilinear(x, y) : NaN);
  }

  valuesAt(x, y) {
    const g = this.grids;
    const pick = (a, b) => { const v = a ? a.sample(x, y) : NaN; return Number.isFinite(v) ? v : (b ? b.sample(x, y) : NaN); };
    let psrDist = NaN;
    if (g.regPsrDist) {
      const { dist, grid } = g.regPsrDist;
      const c = Math.floor((x - grid.x0) / grid.res), r = Math.floor((grid.y0 - y) / grid.res);
      if (c >= 0 && r >= 0 && c < grid.width && r < grid.height) psrDist = dist[r * grid.width + c] * grid.res;
    }
    const ll = xyToLatLon(x, y);
    return {
      ...ll, x, y,
      elev: this.elevAt(x, y),
      slopeMax: g.slopeMax ? g.slopeMax.sample(x, y) : NaN,
      slopeMean: g.slopeMean ? g.slopeMean.sample(x, y) : NaN,
      sun: pick(g.sun, g.regSun), earth: pick(g.earth, g.regEarth), psr: pick(g.psr, g.regPsr), psrDist
    };
  }

  _onMove(p) {
    const { lat, lon } = xyToLatLon(p.x, p.y);
    this.cursor.textContent = `${lat.toFixed(4)}°, ${lon.toFixed(4)}°  |  x ${(p.x / 1000).toFixed(3)} km, y ${(p.y / 1000).toFixed(3)} km`;
  }

  _onClick(p) {
    if (this.mode === 'inspect') return this.inspect(p);
    this.toolClick?.(p);
  }

  inspect(p) {
    const v = this.valuesAt(p.x, p.y);
    const rows = [
      ['Lat / Lon', `${v.lat.toFixed(5)}°, ${v.lon.toFixed(5)}°`],
      ['Elevation', fmt(v.elev, 1, ' m')],
      ['Max slope (5 m)', fmt(v.slopeMax, 1, '°')],
      ['Mean slope', fmt(v.slopeMean, 1, '°')],
      ['Solar visibility', fmt(v.sun * 100, 1, ' %')],
      ['Earth visibility', fmt(v.earth * 100, 1, ' %')],
      ['In PSR', Number.isFinite(v.psr) ? (v.psr > 0.5 ? 'Yes' : 'No') : '—'],
      ['Distance to PSR', Number.isFinite(v.psrDist) ? `${(v.psrDist / 1000).toFixed(2)} km (±0.12)` : '—']
    ];
    this.readout.replaceChildren(...rows.flatMap(([k, val]) => [el('dt', { text: k }), el('dd', { text: val })]));
    if (this.inspectMarker) this.plan.removeLayer(this.inspectMarker);
    this.inspectMarker = L.circleMarker(toLL(p.x, p.y), { radius: 5, color: '#fff', weight: 2, fillOpacity: 0 }).addTo(this.plan);
    this.readout.closest('details').open = true;
    return v;
  }
}

export { latLonToXY };
