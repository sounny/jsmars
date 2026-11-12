/**
 * @module ViewModeToggle
 * @description 2D | 3D switch. 2D is the existing Leaflet map. 3D replaces
 * that view with a Cesium globe and is loaded only on the first switch.
 *
 * Chrome layout:
 * - Desktop (> 768px): Unobtrusive vertical dark-titanium icon rail docked at
 *   the right edge (`#globe-chrome`), contextual popovers anchored beside the
 *   rail only when opened, and a single-line corner attribution pill.
 * - Mobile (<= 768px): Compact single-row horizontal icon pill dock above the
 *   bottom sheet so the planetary disk remains unobstructed.
 */
import { EVENTS } from '../../constants.js';
import { JMARS_CONFIG } from '../../jmars-config.js';
import { jmarsState } from '../../jmars-state.js';
import { formatSubSolarCoord } from '../../util/solarEphemeris.js';
import { loadCesium } from './cesiumLoader.js';
import { CesiumGlobe } from './CesiumGlobe.js';
import {
  describeActiveImagery,
  formatGlobeProvenance,
  formatGlobeCompactSummary,
  TWO_D_ONLY_TOOL_IDS
} from './globeViewModel.js';

const SVG_NS = 'http://www.w3.org/2000/svg';

/**
 * Build a crisp 18x18 monoline SVG icon using DOM APIs (zero innerHTML).
 * @param {Array<{tag: string, attrs: Record<string, string>}>} primitives
 * @param {number} [size=18]
 * @returns {SVGSVGElement}
 */
function createMonolineIcon(primitives, size = 18) {
  const svg = document.createElementNS(SVG_NS, 'svg');
  svg.setAttribute('width', String(size));
  svg.setAttribute('height', String(size));
  svg.setAttribute('viewBox', '0 0 24 24');
  svg.setAttribute('fill', 'none');
  svg.setAttribute('stroke', 'currentColor');
  svg.setAttribute('stroke-width', '1.9');
  svg.setAttribute('stroke-linecap', 'round');
  svg.setAttribute('stroke-linejoin', 'round');
  svg.setAttribute('aria-hidden', 'true');
  svg.classList.add('globe-btn-icon');
  for (const item of primitives) {
    const node = document.createElementNS(SVG_NS, item.tag);
    for (const [k, v] of Object.entries(item.attrs)) {
      node.setAttribute(k, v);
    }
    svg.appendChild(node);
  }
  return svg;
}

const ICONS = {
  zoomIn: [
    { tag: 'line', attrs: { x1: '12', y1: '5', x2: '12', y2: '19' } },
    { tag: 'line', attrs: { x1: '5', y1: '12', x2: '19', y2: '12' } }
  ],
  zoomOut: [
    { tag: 'line', attrs: { x1: '5', y1: '12', x2: '19', y2: '12' } }
  ],
  reset: [
    { tag: 'circle', attrs: { cx: '12', cy: '12', r: '7' } },
    { tag: 'circle', attrs: { cx: '12', cy: '12', r: '1.6', fill: 'currentColor' } },
    { tag: 'line', attrs: { x1: '12', y1: '2', x2: '12', y2: '5' } },
    { tag: 'line', attrs: { x1: '12', y1: '19', x2: '12', y2: '22' } },
    { tag: 'line', attrs: { x1: '2', y1: '12', x2: '5', y2: '12' } },
    { tag: 'line', attrs: { x1: '19', y1: '12', x2: '22', y2: '12' } }
  ],
  spin: [
    { tag: 'path', attrs: { d: 'M21 12a9 9 0 1 1-2.64-6.36' } },
    { tag: 'polyline', attrs: { points: '21 3 21 8.5 15.5 8.5' } }
  ],
  pause: [
    { tag: 'line', attrs: { x1: '9', y1: '6', x2: '9', y2: '18' } },
    { tag: 'line', attrs: { x1: '15', y1: '6', x2: '15', y2: '18' } }
  ],
  grid: [
    { tag: 'circle', attrs: { cx: '12', cy: '12', r: '9' } },
    { tag: 'line', attrs: { x1: '3', y1: '12', x2: '21', y2: '12' } },
    { tag: 'path', attrs: { d: 'M12 3a14.5 14.5 0 0 0 0 18' } },
    { tag: 'path', attrs: { d: 'M12 3a14.5 14.5 0 0 1 0 18' } }
  ],
  terminator: [
    { tag: 'circle', attrs: { cx: '12', cy: '12', r: '9' } },
    { tag: 'path', attrs: { d: 'M12 3a9 9 0 0 0 0 18Z', fill: 'currentColor', 'fill-opacity': '0.32', stroke: 'none' } },
    { tag: 'line', attrs: { x1: '12', y1: '3', x2: '12', y2: '21' } }
  ],
  info: [
    { tag: 'circle', attrs: { cx: '12', cy: '12', r: '9' } },
    { tag: 'line', attrs: { x1: '12', y1: '16', x2: '12', y2: '11.5' } },
    { tag: 'circle', attrs: { cx: '12', cy: '8', r: '0.6', fill: 'currentColor' } }
  ],
  close: [
    { tag: 'line', attrs: { x1: '18', y1: '6', x2: '6', y2: '18' } },
    { tag: 'line', attrs: { x1: '6', y1: '6', x2: '18', y2: '18' } }
  ]
};

export class ViewModeToggle {
  /**
   * @param {object} options
   * @param {import('../../jmars-map.js').JMARSMap} options.jmars
   * @param {HTMLElement} options.toggleEl
   * @param {HTMLElement} options.globeEl
   * @param {HTMLElement} options.cesiumEl
   * @param {HTMLElement} options.noteEl
   * @param {HTMLElement} [options.errorEl]
   */
  constructor(options) {
    this.jmars = options.jmars;
    this.toggleEl = options.toggleEl;
    this.globeEl = options.globeEl;
    this.cesiumEl = options.cesiumEl;
    this.noteEl = options.noteEl;
    this.errorEl = options.errorEl || null;
    this.mode = '2d';
    this._switchGen = 0;
    this._ignoreMapMove = false;
    this._globe = new CesiumGlobe(this.cesiumEl);
    this._imagery = [];
    this._lightingMode = 'ephemeris';
    this._ephemerisDate = new Date();
    this._followLiveClock = true;
    this._lastCameraZoom = 2;
    this._resize = () => this._globe.resize();
    this._buildChrome();
    this._bind();
    this.setModeUi('2d');
  }

  _buildChrome() {
    // 1. Compact Right-Edge Vertical Action Rail (Mobile: Bottom Pill Dock)
    const chrome = document.createElement('div');
    chrome.className = 'globe-chrome';
    chrome.id = 'globe-chrome';

    const controls = document.createElement('div');
    controls.className = 'globe-chrome-controls';
    controls.setAttribute('role', 'toolbar');
    controls.setAttribute('aria-label', '3D globe controls');

    const zoomIn = this._iconButton({
      id: 'globe-zoom-in-btn',
      label: 'Zoom in',
      srText: 'Zoom in',
      tooltip: 'Zoom in',
      icon: ICONS.zoomIn,
      onClick: () => this._globe.zoomBy(1)
    });
    const zoomOut = this._iconButton({
      id: 'globe-zoom-out-btn',
      label: 'Zoom out',
      srText: 'Zoom out',
      tooltip: 'Zoom out',
      icon: ICONS.zoomOut,
      onClick: () => this._globe.zoomBy(-1)
    });
    const reset = this._iconButton({
      id: 'globe-reset-btn',
      label: 'Reset globe to the body overview',
      srText: 'Reset',
      tooltip: 'Reset view',
      icon: ICONS.reset,
      onClick: () => this.resetView()
    });

    this._spinBtn = this._iconButton({
      id: 'globe-spin-btn',
      label: 'Spin the globe',
      srText: 'Spin',
      tooltip: 'Spin globe',
      icon: ICONS.spin,
      onClick: () => {
        const next = !this._globe.autoSpin;
        this._globe.setAutoSpin(next);
        this._syncSpinLabel();
      }
    });
    this._spinBtn.setAttribute('aria-pressed', 'false');

    this._gridBtn = this._iconButton({
      id: 'globe-grid-btn',
      label: 'Toggle latitude and longitude grid',
      srText: 'Grid',
      tooltip: 'Lat/Lon grid',
      icon: ICONS.grid,
      onClick: () => {
        const next = this._gridBtn.getAttribute('aria-pressed') !== 'true';
        this._globe.setGraticule(next);
        this._gridBtn.setAttribute('aria-pressed', next ? 'true' : 'false');
      }
    });
    this._gridBtn.setAttribute('aria-pressed', 'false');

    this._lightBtn = this._iconButton({
      id: 'globe-terminator-btn',
      label: 'Toggle ellipsoid day/night solar terminator',
      srText: 'Terminator',
      tooltip: 'Solar terminator',
      icon: ICONS.terminator,
      onClick: () => {
        const next = this._lightBtn.getAttribute('aria-pressed') !== 'true';
        this._lightBtn.setAttribute('aria-pressed', next ? 'true' : 'false');
        this._solarPanel.hidden = !next;
        if (next && this._followLiveClock) {
          this._ephemerisDate = new Date();
        }
        this._applyCurrentLighting();
      }
    });
    this._lightBtn.setAttribute('aria-pressed', 'false');

    this._infoBtn = this._iconButton({
      id: 'globe-info-btn',
      label: 'Inspect 3D globe ellipsoid and imagery provenance',
      srText: 'Globe info',
      tooltip: 'Globe & data info',
      icon: ICONS.info,
      onClick: () => this.setInfoOpen(this._infoPopover.hidden)
    });
    this._infoBtn.setAttribute('aria-pressed', 'false');
    this._infoBtn.setAttribute('aria-expanded', 'false');
    this._infoBtn.setAttribute('aria-controls', 'globe-info-popover');

    controls.appendChild(zoomIn);
    controls.appendChild(zoomOut);
    controls.appendChild(reset);
    controls.appendChild(this._divider());
    controls.appendChild(this._spinBtn);
    controls.appendChild(this._gridBtn);
    controls.appendChild(this._lightBtn);
    controls.appendChild(this._divider());
    controls.appendChild(this._infoBtn);

    chrome.appendChild(controls);

    // 2. Contextual Floating Popover Stack (Anchored next to the rail, never over center)
    this._popoverStack = document.createElement('div');
    this._popoverStack.className = 'globe-popover-stack';
    this._popoverStack.id = 'globe-popover-stack';

    // 2a. Astronomical Terminator Popover (Live UTC Ephemeris vs Modeled Local Hour)
    this._solarPanel = document.createElement('div');
    this._solarPanel.className = 'globe-solar-panel';
    this._solarPanel.id = 'globe-solar-panel';
    this._solarPanel.hidden = true;

    const solarHeader = document.createElement('div');
    solarHeader.className = 'globe-popover-header';
    const solarTitle = document.createElement('span');
    solarTitle.className = 'globe-popover-title';
    solarTitle.textContent = 'Solar Terminator';
    const solarHideBtn = document.createElement('button');
    solarHideBtn.type = 'button';
    solarHideBtn.className = 'globe-popover-close';
    solarHideBtn.setAttribute('aria-label', 'Hide terminator settings panel');
    solarHideBtn.title = 'Hide panel (keep terminator active)';
    solarHideBtn.appendChild(createMonolineIcon(ICONS.close, 14));
    solarHideBtn.addEventListener('click', (e) => {
      e.stopPropagation();
      this._solarPanel.hidden = true;
    });
    solarHeader.appendChild(solarTitle);
    solarHeader.appendChild(solarHideBtn);

    const modeRow = document.createElement('div');
    modeRow.className = 'globe-solar-modes';

    this._ephemModeBtn = document.createElement('button');
    this._ephemModeBtn.type = 'button';
    this._ephemModeBtn.className = 'globe-solar-mode-btn is-active';
    this._ephemModeBtn.setAttribute('aria-pressed', 'true');
    this._ephemModeBtn.textContent = 'Live UTC Ephemeris';
    this._ephemModeBtn.addEventListener('click', () => {
      this._lightingMode = 'ephemeris';
      this._syncLightingModeUi();
      this._applyCurrentLighting();
    });

    this._localModeBtn = document.createElement('button');
    this._localModeBtn.type = 'button';
    this._localModeBtn.className = 'globe-solar-mode-btn';
    this._localModeBtn.setAttribute('aria-pressed', 'false');
    this._localModeBtn.textContent = 'Local Hour [Model]';
    this._localModeBtn.addEventListener('click', () => {
      this._lightingMode = 'local';
      this._syncLightingModeUi();
      this._applyCurrentLighting();
    });

    this._nowBtn = document.createElement('button');
    this._nowBtn.type = 'button';
    this._nowBtn.className = 'globe-solar-now-btn';
    this._nowBtn.title = 'Snap solar ephemeris back to current real-time UTC';
    this._nowBtn.textContent = 'Now';
    this._nowBtn.addEventListener('click', () => {
      this._followLiveClock = true;
      this._ephemerisDate = new Date();
      this._lightingMode = 'ephemeris';
      this._syncLightingModeUi();
      this._applyCurrentLighting();
    });

    modeRow.appendChild(this._ephemModeBtn);
    modeRow.appendChild(this._localModeBtn);
    modeRow.appendChild(this._nowBtn);

    this._hourWrap = document.createElement('label');
    this._hourWrap.className = 'globe-hour';
    this._hourWrap.hidden = true;
    const hourText = document.createElement('span');
    hourText.textContent = 'Local solar hour (at view lon)';
    this._hourValue = document.createElement('span');
    this._hourValue.textContent = '12:00';
    this._hourSlider = document.createElement('input');
    this._hourSlider.type = 'range';
    this._hourSlider.min = '0';
    this._hourSlider.max = '24';
    this._hourSlider.step = '0.5';
    this._hourSlider.value = '12';
    this._hourSlider.setAttribute('aria-label', 'Modeled local solar hour at viewed longitude');
    this._hourSlider.addEventListener('input', () => {
      const hour = Number(this._hourSlider.value);
      this._hourValue.textContent = formatHour(hour);
      this._applyCurrentLighting();
    });
    this._hourWrap.appendChild(hourText);
    this._hourWrap.appendChild(this._hourSlider);
    this._hourWrap.appendChild(this._hourValue);

    this._solarReadout = document.createElement('p');
    this._solarReadout.className = 'globe-solar-readout';
    this._solarReadout.textContent = '';

    const solarNote = document.createElement('p');
    solarNote.className = 'globe-solar-note';
    solarNote.textContent = 'Smooth ellipsoid day/night only — no 3D DEM shadows; local relief in tiles is fixed spacecraft illumination.';

    this._solarPanel.appendChild(solarHeader);
    this._solarPanel.appendChild(modeRow);
    this._solarPanel.appendChild(this._hourWrap);
    this._solarPanel.appendChild(this._solarReadout);
    this._solarPanel.appendChild(solarNote);

    // 2b. Globe & Data Provenance Popover (On-demand via ⓘ button or corner pill)
    this._infoPopover = document.createElement('div');
    this._infoPopover.className = 'globe-info-popover';
    this._infoPopover.id = 'globe-info-popover';
    this._infoPopover.setAttribute('role', 'region');
    this._infoPopover.setAttribute('aria-label', '3D globe data provenance');
    this._infoPopover.hidden = true;

    const infoHeader = document.createElement('div');
    infoHeader.className = 'globe-popover-header';
    const infoTitle = document.createElement('span');
    infoTitle.className = 'globe-popover-title';
    infoTitle.textContent = '3D Globe & Data Provenance';
    const infoCloseBtn = document.createElement('button');
    infoCloseBtn.type = 'button';
    infoCloseBtn.className = 'globe-popover-close';
    infoCloseBtn.id = 'globe-info-close';
    infoCloseBtn.setAttribute('aria-label', 'Close globe provenance');
    infoCloseBtn.appendChild(createMonolineIcon(ICONS.close, 14));
    infoCloseBtn.addEventListener('click', (e) => {
      e.stopPropagation();
      this.setInfoOpen(false);
    });
    infoHeader.appendChild(infoTitle);
    infoHeader.appendChild(infoCloseBtn);

    this._provenance = document.createElement('p');
    this._provenance.className = 'globe-provenance';

    const credit = document.createElement('p');
    credit.className = 'globe-credit';
    credit.textContent = '3D globe by CesiumJS (Apache-2.0).';

    this._infoPopover.appendChild(infoHeader);
    this._infoPopover.appendChild(this._provenance);
    this._infoPopover.appendChild(credit);

    this._popoverStack.appendChild(this._solarPanel);
    this._popoverStack.appendChild(this._infoPopover);

    // 3. Subtle Bottom-Right Single-Line Attribution & Provenance Pill
    this._microAttr = document.createElement('button');
    this._microAttr.type = 'button';
    this._microAttr.className = 'globe-micro-attribution';
    this._microAttr.id = 'globe-micro-attribution';
    this._microAttr.setAttribute('aria-label', 'Inspect 3D globe data provenance');
    this._microAttr.setAttribute('aria-controls', 'globe-info-popover');
    this._microAttr.setAttribute('aria-expanded', 'false');
    this._microAttrText = document.createElement('span');
    this._microAttrText.className = 'globe-micro-attribution-text';
    this._microAttr.appendChild(this._microAttrText);
    this._microAttr.appendChild(createMonolineIcon(ICONS.info, 13));
    this._microAttr.addEventListener('click', (e) => {
      e.stopPropagation();
      this.setInfoOpen(this._infoPopover.hidden);
    });

    this.globeEl.appendChild(chrome);
    this.globeEl.appendChild(this._popoverStack);
    this.globeEl.appendChild(this._microAttr);

    // Wire live 3D globe telemetry into the bottom StatusBar
    this._globe.onHover = (coords) => {
      if (!coords || this.mode !== '3d') return;
      document.dispatchEvent(new CustomEvent(EVENTS.GLOBE_HOVER, {
        detail: {
          lat: coords.lat,
          lng: coords.lng,
          zoom: this._lastCameraZoom
        }
      }));
    };
    this._globe.onCameraChanged = (camView) => {
      if (!camView || this.mode !== '3d') return;
      if (Number.isFinite(camView.zoom)) {
        this._lastCameraZoom = camView.zoom;
      }
      document.dispatchEvent(new CustomEvent(EVENTS.GLOBE_HOVER, {
        detail: {
          lat: camView.lat,
          lng: camView.lng,
          zoom: camView.zoom,
          isCenter: true
        }
      }));
    };
    this._globe.onSpinPaused = () => this._syncSpinLabel();
  }

  /**
   * Show or hide the Globe & Data Provenance popover cleanly.
   * @param {boolean} open
   */
  setInfoOpen(open) {
    const isOpen = !!open;
    this._infoPopover.hidden = !isOpen;
    this._infoBtn.setAttribute('aria-pressed', isOpen ? 'true' : 'false');
    this._infoBtn.setAttribute('aria-expanded', isOpen ? 'true' : 'false');
    this._microAttr.setAttribute('aria-expanded', isOpen ? 'true' : 'false');
  }

  _divider() {
    const div = document.createElement('span');
    div.className = 'globe-chrome-divider';
    div.setAttribute('role', 'separator');
    div.setAttribute('aria-hidden', 'true');
    return div;
  }

  _iconButton({ id, label, srText, tooltip, icon, onClick }) {
    const btn = document.createElement('button');
    btn.type = 'button';
    btn.className = 'globe-chrome-btn';
    if (id) btn.id = id;
    btn.setAttribute('aria-label', label);
    if (tooltip) btn.setAttribute('data-tooltip', tooltip);
    btn.appendChild(createMonolineIcon(icon, 18));
    const sr = document.createElement('span');
    sr.className = 'sr-only';
    sr.textContent = srText;
    btn.appendChild(sr);
    btn.addEventListener('click', (event) => {
      event.stopPropagation();
      onClick();
    });
    return btn;
  }

  _syncLightingModeUi() {
    const isEphem = this._lightingMode !== 'local';
    this._ephemModeBtn.classList.toggle('is-active', isEphem);
    this._ephemModeBtn.setAttribute('aria-pressed', isEphem ? 'true' : 'false');
    this._localModeBtn.classList.toggle('is-active', !isEphem);
    this._localModeBtn.setAttribute('aria-pressed', isEphem ? 'false' : 'true');
    this._hourWrap.hidden = isEphem;
    this._nowBtn.hidden = !isEphem;
  }

  _applyCurrentLighting() {
    const enabled = this._lightBtn.getAttribute('aria-pressed') === 'true';
    const viewLon = this._globe.ready
      ? (this._globe.getCameraView()?.lng ?? (this.jmars.getViewState()?.lng ?? 0))
      : (this.jmars.getViewState()?.lng ?? 0);
    const activeDate = this._ephemerisDate;

    const state = this._globe.setLighting({
      enabled,
      mode: this._lightingMode,
      body: this.jmars.currentBody,
      date: activeDate,
      hour: Number(this._hourSlider.value),
      referenceLon: viewLon
    });

    if (enabled && state && state.enabled) {
      const subText = formatSubSolarCoord(state.subSolarLatDeg, state.subSolarLonEastDeg);
      if (state.mode === 'local') {
        this._solarReadout.textContent = `[Model] ${formatHour(state.localSolarHour)} at ${formatLon(state.referenceLonEastDeg)} \u00b7 Subsolar ${subText} (IAU seasonal \u03b4\u2609)`;
      } else {
        const utcShort = state.utcIso.slice(0, 16).replace('T', ' ') + ' UTC';
        this._solarReadout.textContent = `[Ephemeris] ${utcShort} \u00b7 Subsolar ${subText} (IAU WGCCRE)`;
      }
    } else {
      this._solarReadout.textContent = '';
    }
    this._renderProvenance();
  }

  _syncSpinLabel() {
    const spinning = !!this._globe.autoSpin;
    const oldSvg = this._spinBtn.querySelector('svg');
    const nextSvg = createMonolineIcon(spinning ? ICONS.pause : ICONS.spin, 18);
    if (oldSvg) this._spinBtn.replaceChild(nextSvg, oldSvg);
    const sr = this._spinBtn.querySelector('.sr-only');
    if (sr) sr.textContent = spinning ? 'Pause' : 'Spin';
    this._spinBtn.setAttribute('aria-pressed', spinning ? 'true' : 'false');
    this._spinBtn.setAttribute('aria-label', spinning ? 'Pause globe spin' : 'Spin the globe');
    this._spinBtn.setAttribute('data-tooltip', spinning ? 'Pause spin' : 'Spin globe');
  }

  _bind() {
    const btn2d = this.toggleEl.querySelector('#view-mode-2d');
    const btn3d = this.toggleEl.querySelector('#view-mode-3d');
    btn2d.addEventListener('click', () => this.setMode('2d'));
    btn3d.addEventListener('click', () => this.setMode('3d'));

    // Dismiss info popover on Escape or when clicking directly on the Cesium canvas
    document.addEventListener('keydown', (e) => {
      if (e.key === 'Escape' && this.mode === '3d' && !this._infoPopover.hidden) {
        const cmdModal = document.getElementById('quick-command-backdrop');
        if (cmdModal && !cmdModal.hidden) return;
        this.setInfoOpen(false);
      }
    });
    this.cesiumEl.addEventListener('pointerdown', () => {
      if (!this._infoPopover.hidden) this.setInfoOpen(false);
    });

    this.jmars.map.on('moveend', () => {
      if (this.mode !== '3d' || this._ignoreMapMove || !this._globe.ready) return;
      const view = this.jmars.getViewState();
      if (view) {
        this._globe.setCameraView({ ...view, viewportHeightPx: this._viewportHeight() });
        if (this._lightBtn.getAttribute('aria-pressed') === 'true' && this._lightingMode === 'local') {
          this._applyCurrentLighting();
        }
      }
    });

    document.addEventListener(EVENTS.BODY_CHANGED, () => {
      if (this.mode === '3d') this._refreshFromMap({ recenter: true });
    });
    document.addEventListener(EVENTS.LAYERS_CHANGED, () => {
      if (this.mode === '3d' && this._globe.bodyKey === this.jmars.currentBody) {
        this._refreshFromMap({ recenter: false });
      }
    });
    document.addEventListener(EVENTS.TIME_CHANGED, (event) => {
      const incomingDate = event?.detail?.date ? new Date(event.detail.date) : null;
      if (incomingDate && !Number.isNaN(incomingDate.getTime())) {
        this._followLiveClock = false;
        this._ephemerisDate = incomingDate;
        if (this.mode === '3d' && this._lightBtn.getAttribute('aria-pressed') === 'true') {
          this._applyCurrentLighting();
        }
      }
    });
    window.addEventListener('resize', this._resize);
  }

  /**
   * @param {'2d'|'3d'} mode
   */
  async setMode(mode) {
    if (mode === this.mode) return;
    this._clearError();
    if (mode === '3d') await this._enter3d();
    else this._enter2d();
  }

  async _enter3d() {
    const gen = ++this._switchGen;
    this.mode = '3d';
    this.setModeUi('3d');
    this._setToolsEnabled(false);
    this._dispatch('3d');
    this.globeEl.classList.add('is-open');
    this.globeEl.removeAttribute('hidden');
    this.jmars.map.getContainer().classList.add('is-under-globe');
    this._setBusy(true);
    try {
      const Cesium = await loadCesium();
      if (gen !== this._switchGen || this.mode !== '3d') return;
      this._mount(Cesium);
    } catch (err) {
      console.warn('3D globe unavailable:', err);
      if (gen !== this._switchGen) return;
      this._globe.destroyViewer();
      this.mode = '2d';
      this.globeEl.classList.remove('is-open');
      this.globeEl.setAttribute('hidden', '');
      this.jmars.map.getContainer().classList.remove('is-under-globe');
      this.setModeUi('2d');
      this._setToolsEnabled(true);
      this._dispatch('2d');
      this._showError('Could not load the 3D globe. The 2D map is unchanged.');
    } finally {
      if (this.mode !== '3d' || gen === this._switchGen) this._setBusy(false);
    }
  }

  _enter2d() {
    this._switchGen += 1;
    const view = this._globe.ready
      ? this._globe.getCameraView({ viewportHeightPx: this._viewportHeight() })
      : null;
    this.mode = '2d';
    this.setInfoOpen(false);
    this._globe.setAutoSpin(false);
    this._syncSpinLabel();
    this._globe.destroyViewer();
    this.globeEl.classList.remove('is-open');
    this.globeEl.setAttribute('hidden', '');
    this.jmars.map.getContainer().classList.remove('is-under-globe');
    this.setModeUi('2d');
    this._setToolsEnabled(true);
    this._setBusy(false);
    if (this.jmars.map) this.jmars.map.invalidateSize();
    if (view) {
      this._ignoreMapMove = true;
      this.jmars.applyViewState({ lat: view.lat, lng: view.lng, zoom: view.zoom });
      window.setTimeout(() => {
        this._ignoreMapMove = false;
      }, 0);
    }
    this._dispatch('2d');
  }

  _mount(Cesium) {
    const view = this.jmars.getViewState() || { lat: 0, lng: 0, zoom: 2 };
    this._lastCameraZoom = Number.isFinite(view.zoom) ? view.zoom : 2;
    this._imagery = this._currentImagery();
    this._globe.mount(Cesium, {
      body: this.jmars.currentBody,
      lat: view.lat,
      lng: view.lng,
      zoom: view.zoom,
      imagery: this._imagery,
      viewportHeightPx: this._viewportHeight()
    });
    this._globe.setGraticule(this._gridBtn.getAttribute('aria-pressed') === 'true');
    this._globe.setAutoSpin(false);
    this._syncSpinLabel();
    this._applyCurrentLighting();
  }

  _refreshFromMap(options) {
    if (!this._globe.ready || !this._globe.Cesium) return;
    const view = this.jmars.getViewState() || { lat: 0, lng: 0, zoom: 2 };
    const imagery = this._currentImagery();
    const bodyChanged = this._globe.bodyKey !== this.jmars.currentBody;
    if (bodyChanged) {
      const keepSpin = !!this._globe.autoSpin;
      this._imagery = imagery;
      this._globe.mount(this._globe.Cesium, {
        body: this.jmars.currentBody,
        lat: view.lat,
        lng: view.lng,
        zoom: view.zoom,
        imagery,
        viewportHeightPx: this._viewportHeight()
      });
      this._globe.setGraticule(this._gridBtn.getAttribute('aria-pressed') === 'true');
      this._globe.setAutoSpin(keepSpin);
      this._syncSpinLabel();
      this._applyCurrentLighting();
      return;
    }
    this._imagery = imagery;
    this._globe.setImagery(imagery);
    if (options.recenter) {
      this._globe.setCameraView({ ...view, viewportHeightPx: this._viewportHeight() });
    }
    this._applyCurrentLighting();
  }

  _currentImagery() {
    const body = JMARS_CONFIG.bodies[this.jmars.currentBody];
    const configs = [
      ...(this.jmars.availableLayers || []),
      ...((body && body.layers) || [])
    ];
    return describeActiveImagery(jmarsState.get('activeLayers'), configs);
  }

  _renderProvenance() {
    this._provenance.textContent = formatGlobeProvenance(
      this.jmars.currentBody,
      this._imagery,
      this._globe.lightingState
    );
    if (this._microAttrText) {
      this._microAttrText.textContent = formatGlobeCompactSummary(
        this.jmars.currentBody,
        this._imagery,
        this._globe.lightingState
      );
    }
  }

  resetView() {
    const body = JMARS_CONFIG.bodies[this.jmars.currentBody] || JMARS_CONFIG.bodies.mars;
    const center = body.center || [0, 0];
    this._globe.setAutoSpin(false);
    this._syncSpinLabel();
    this._globe.setCameraView({
      lat: center[0],
      lng: center[1],
      zoom: body.zoom ?? 2,
      viewportHeightPx: this._viewportHeight()
    });
    if (this._lightBtn.getAttribute('aria-pressed') === 'true' && this._lightingMode === 'local') {
      this._applyCurrentLighting();
    }
  }

  _viewportHeight() {
    const height = this.cesiumEl?.clientHeight || this.jmars.map?.getContainer()?.clientHeight;
    return height > 0 ? height : undefined;
  }

  setModeUi(mode) {
    const btn2d = this.toggleEl.querySelector('#view-mode-2d');
    const btn3d = this.toggleEl.querySelector('#view-mode-3d');
    const is3d = mode === '3d';
    btn2d.classList.toggle('is-active', !is3d);
    btn3d.classList.toggle('is-active', is3d);
    btn2d.setAttribute('aria-pressed', is3d ? 'false' : 'true');
    btn3d.setAttribute('aria-pressed', is3d ? 'true' : 'false');
    document.body.classList.toggle('view-3d', is3d);
    if (this.noteEl) this.noteEl.hidden = !is3d;
  }

  _setBusy(busy) {
    const btn3d = this.toggleEl.querySelector('#view-mode-3d');
    btn3d.setAttribute('aria-busy', busy ? 'true' : 'false');
    btn3d.disabled = !!busy;
  }

  _setToolsEnabled(enabled) {
    document.querySelectorAll('[data-requires-2d]').forEach((section) => {
      section.classList.toggle('tool-unavailable-3d', !enabled);
      section.querySelectorAll('button, select, input, textarea').forEach((el) => {
        if (!enabled) {
          if (!el.hasAttribute('data-prev-disabled')) {
            el.setAttribute('data-prev-disabled', el.disabled ? '1' : '0');
          }
          el.disabled = true;
          el.setAttribute('aria-disabled', 'true');
        } else if (el.hasAttribute('data-prev-disabled')) {
          el.disabled = el.getAttribute('data-prev-disabled') === '1';
          el.removeAttribute('data-prev-disabled');
          el.removeAttribute('aria-disabled');
        }
      });
    });
  }

  _dispatch(mode) {
    document.dispatchEvent(new CustomEvent(EVENTS.VIEW_MODE_CHANGED, { detail: { mode } }));
  }

  _showError(message) {
    if (!this.errorEl) return;
    this.errorEl.hidden = false;
    this.errorEl.textContent = message;
  }

  _clearError() {
    if (!this.errorEl) return;
    this.errorEl.hidden = true;
    this.errorEl.textContent = '';
  }
}

function formatHour(hour) {
  const hours = Math.floor(hour);
  const mins = Math.floor((hour - hours) * 60);
  return `${String(hours).padStart(2, '0')}:${String(mins).padStart(2, '0')}`;
}

function formatLon(lonEast) {
  const hemi = lonEast >= 0 ? 'E' : 'W';
  return `${Math.abs(lonEast).toFixed(1)}\u00b0${hemi}`;
}

export { TWO_D_ONLY_TOOL_IDS };
