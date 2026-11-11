/**
 * @module ViewModeToggle
 * @description 2D | 3D switch. 2D is the existing Leaflet map. 3D replaces
 * that view with a Cesium globe and is loaded only on the first switch.
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
  TWO_D_ONLY_TOOL_IDS
} from './globeViewModel.js';

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
    this._resize = () => this._globe.resize();
    this._buildChrome();
    this._bind();
    this.setModeUi('2d');
  }

  _buildChrome() {
    const chrome = document.createElement('div');
    chrome.className = 'globe-chrome';
    chrome.id = 'globe-chrome';

    const controls = document.createElement('div');
    controls.className = 'globe-chrome-controls';

    const zoomIn = this._button('Zoom in', '+', () => this._globe.zoomBy(1));
    const zoomOut = this._button('Zoom out', '\u2212', () => this._globe.zoomBy(-1));
    this._spinBtn = this._button('Spin the globe', 'Spin', () => {
      const next = !this._globe.autoSpin;
      this._globe.setAutoSpin(next);
      this._syncSpinLabel();
    });
    this._gridBtn = this._button('Toggle latitude and longitude grid', 'Grid', () => {
      const next = this._gridBtn.getAttribute('aria-pressed') !== 'true';
      this._globe.setGraticule(next);
      this._gridBtn.setAttribute('aria-pressed', next ? 'true' : 'false');
    });
    this._gridBtn.setAttribute('aria-pressed', 'false');

    this._lightBtn = this._button(
      'Toggle ellipsoid day/night solar terminator',
      'Terminator',
      () => {
        const next = this._lightBtn.getAttribute('aria-pressed') !== 'true';
        this._lightBtn.setAttribute('aria-pressed', next ? 'true' : 'false');
        this._solarPanel.hidden = !next;
        if (next && this._followLiveClock) {
          this._ephemerisDate = new Date();
        }
        this._applyCurrentLighting();
      }
    );
    this._lightBtn.setAttribute('aria-pressed', 'false');

    const reset = this._button('Reset globe to the body overview', 'Reset', () => this.resetView());

    [zoomIn, zoomOut, this._spinBtn, this._gridBtn, this._lightBtn, reset].forEach((btn) => {
      controls.appendChild(btn);
    });

    // Astronomical Terminator Panel (Live UTC Ephemeris vs Modeled Local Hour)
    this._solarPanel = document.createElement('div');
    this._solarPanel.className = 'globe-solar-panel';
    this._solarPanel.hidden = true;

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

    this._solarPanel.appendChild(modeRow);
    this._solarPanel.appendChild(this._hourWrap);
    this._solarPanel.appendChild(this._solarReadout);

    this._provenance = document.createElement('p');
    this._provenance.className = 'globe-provenance';

    this._coords = document.createElement('p');
    this._coords.className = 'globe-coords';
    this._coords.textContent = '';

    const credit = document.createElement('p');
    credit.className = 'globe-credit';
    credit.textContent = '3D globe by CesiumJS (Apache-2.0).';

    chrome.appendChild(controls);
    chrome.appendChild(this._solarPanel);
    chrome.appendChild(this._provenance);
    chrome.appendChild(this._coords);
    chrome.appendChild(credit);
    this.globeEl.appendChild(chrome);

    this._globe.onHover = (coords) => {
      if (!coords) {
        this._coords.textContent = '';
        return;
      }
      const ns = coords.lat >= 0 ? 'N' : 'S';
      const ew = coords.lng >= 0 ? 'E' : 'W';
      this._coords.textContent = `${Math.abs(coords.lat).toFixed(2)}\u00b0${ns}, ${Math.abs(coords.lng).toFixed(2)}\u00b0${ew}`;
    };
    this._globe.onSpinPaused = () => this._syncSpinLabel();
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
    const activeDate = this._followLiveClock && this._lightingMode === 'ephemeris'
      ? this._ephemerisDate
      : this._ephemerisDate;

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

  _button(label, text, onClick) {
    const btn = document.createElement('button');
    btn.type = 'button';
    btn.className = 'globe-chrome-btn';
    btn.textContent = text;
    btn.setAttribute('aria-label', label);
    btn.title = label;
    btn.addEventListener('click', (event) => {
      event.stopPropagation();
      onClick();
    });
    return btn;
  }

  _syncSpinLabel() {
    const spinning = !!this._globe.autoSpin;
    this._spinBtn.textContent = spinning ? 'Pause' : 'Spin';
    this._spinBtn.setAttribute('aria-pressed', spinning ? 'true' : 'false');
    this._spinBtn.setAttribute('aria-label', spinning ? 'Pause globe spin' : 'Spin the globe');
  }

  _bind() {
    const btn2d = this.toggleEl.querySelector('#view-mode-2d');
    const btn3d = this.toggleEl.querySelector('#view-mode-3d');
    btn2d.addEventListener('click', () => this.setMode('2d'));
    btn3d.addEventListener('click', () => this.setMode('3d'));

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
