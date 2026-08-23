import { KRCEngine } from './KRCEngine.js';
import { KRCChart } from './KRCChart.js';
import { EventBus } from '../../core/EventBus.js';
import { EVENTS } from '../../constants.js';

/**
 * @module KRCPanel
 * @description UI control panel for KRC 1D Mars Thermal Model simulation.
 */
export class KRCPanel {
  /**
   * @param {HTMLElement|string} container - DOM element or ID
   * @param {L.Map} map - Leaflet map instance
   */
  constructor(container, map) {
    this.container = typeof container === 'string' ? document.getElementById(container) : container;
    this.map = map;
    this.chart = null;
    this.isPicking = false;
    this.lastResult = null;
    this.seasonalData = null;

    if (this.container) {
      this.init();
    }
  }

  init() {
    this.container.innerHTML = `
      <div style="padding: 10px; font-size: 12px; color: #e2e8f0;">
        <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 8px; margin-bottom: 8px;">
          <div>
            <label style="font-size: 10px; color: #94a3b8; display: block;">Latitude (°)</label>
            <input type="number" id="krc-input-lat" class="tool-select" style="width: 100%; box-sizing: border-box;" value="0" min="-90" max="90" step="1">
          </div>
          <div>
            <label style="font-size: 10px; color: #94a3b8; display: block;">Solar Longitude L<sub>s</sub> (°)</label>
            <input type="number" id="krc-input-ls" class="tool-select" style="width: 100%; box-sizing: border-box;" value="0" min="0" max="360" step="5">
          </div>
          <div>
            <label style="font-size: 10px; color: #94a3b8; display: block;">Thermal Inertia (SI)</label>
            <input type="number" id="krc-input-ti" class="tool-select" style="width: 100%; box-sizing: border-box;" value="250" min="30" max="1500" step="25" title="J m^-2 K^-1 s^-1/2">
          </div>
          <div>
            <label style="font-size: 10px; color: #94a3b8; display: block;">Albedo (0.05-0.4)</label>
            <input type="number" id="krc-input-albedo" class="tool-select" style="width: 100%; box-sizing: border-box;" value="0.25" min="0.05" max="0.45" step="0.01">
          </div>
          <div>
            <label style="font-size: 10px; color: #94a3b8; display: block;">Dust Opacity τ</label>
            <input type="number" id="krc-input-tau" class="tool-select" style="width: 100%; box-sizing: border-box;" value="0.3" min="0.1" max="3.0" step="0.1">
          </div>
          <div>
            <label style="font-size: 10px; color: #94a3b8; display: block;">Elevation (m)</label>
            <input type="number" id="krc-input-elev" class="tool-select" style="width: 100%; box-sizing: border-box;" value="0" min="-8000" max="22000" step="500">
          </div>
        </div>

        <div style="display: flex; gap: 6px; margin-bottom: 10px;">
          <button id="krc-pick-btn" class="tool-btn" style="flex: 1; font-size: 11px; background: #334155;">📍 Pick Location</button>
          <button id="krc-run-btn" class="tool-btn" style="flex: 1; font-size: 11px; background: #ea580c; font-weight: 600;">Calculate</button>
        </div>

        <div id="krc-summary-card" style="display: none; background: #0f172a; border: 1px solid #1e293b; border-radius: 4px; padding: 6px 8px; margin-bottom: 10px; font-size: 10px;">
          <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 4px;">
            <div>T<sub>max</sub>: <b id="krc-res-max" style="color: #f87171;">--</b></div>
            <div>T<sub>min</sub>: <b id="krc-res-min" style="color: #60a5fa;">--</b></div>
            <div>T<sub>mean</sub>: <b id="krc-res-mean" style="color: #facc15;">--</b></div>
            <div>ΔT (Range): <b id="krc-res-range" style="color: #e2e8f0;">--</b></div>
            <div>Skin Depth: <b id="krc-res-skindepth" style="color: #c084fc;">--</b></div>
            <div id="krc-res-frost" style="color: #38bdf8; font-weight: 600; display: none;">❄ CO₂ Frost</div>
          </div>
        </div>

        <div id="krc-chart-container" style="margin-bottom: 8px;"></div>

        <div style="display: flex; gap: 6px;">
          <button id="krc-export-csv-btn" class="tool-btn" style="flex: 1; font-size: 11px;">Export CSV</button>
        </div>
      </div>
    `;

    this.latInput = this.container.querySelector('#krc-input-lat');
    this.lsInput = this.container.querySelector('#krc-input-ls');
    this.tiInput = this.container.querySelector('#krc-input-ti');
    this.albedoInput = this.container.querySelector('#krc-input-albedo');
    this.tauInput = this.container.querySelector('#krc-input-tau');
    this.elevInput = this.container.querySelector('#krc-input-elev');
    this.pickBtn = this.container.querySelector('#krc-pick-btn');
    this.runBtn = this.container.querySelector('#krc-run-btn');
    this.exportCsvBtn = this.container.querySelector('#krc-export-csv-btn');
    this.summaryCard = this.container.querySelector('#krc-summary-card');

    this.chart = new KRCChart(this.container.querySelector('#krc-chart-container'));

    this.bindEvents();

    // Synchronize with global time changes
    EventBus.on(EVENTS.TIME_CHANGED, (detail) => {
      if (detail && typeof detail.Ls === 'number' && !this.isPicking) {
        this.lsInput.value = detail.Ls.toFixed(1);
      }
    });
  }

  bindEvents() {
    this.runBtn.addEventListener('click', () => this.runSimulation());

    this.pickBtn.addEventListener('click', () => {
      this.isPicking = !this.isPicking;
      this.pickBtn.style.background = this.isPicking ? '#0284c7' : '#334155';
      this.pickBtn.innerText = this.isPicking ? 'Click map...' : '📍 Pick Location';
      this.map.getContainer().style.cursor = this.isPicking ? 'crosshair' : '';
    });

    this.map.on('click', (e) => {
      if (!this.isPicking) return;
      this.latInput.value = e.latlng.lat.toFixed(2);
      this.isPicking = false;
      this.pickBtn.style.background = '#334155';
      this.pickBtn.innerText = '📍 Pick Location';
      this.map.getContainer().style.cursor = '';
      this.runSimulation();
    });

    this.exportCsvBtn.addEventListener('click', () => this.exportCSV());
  }

  runSimulation() {
    const lat = parseFloat(this.latInput.value) || 0;
    const Ls = parseFloat(this.lsInput.value) || 0;
    const thermalInertia = parseFloat(this.tiInput.value) || 250;
    const albedo = parseFloat(this.albedoInput.value) || 0.25;
    const tau = parseFloat(this.tauInput.value) || 0.3;
    const elevation = parseFloat(this.elevInput.value) || 0;

    const result = KRCEngine.simulateDiurnal({
      lat,
      Ls,
      thermalInertia,
      albedo,
      tau,
      elevation
    });

    const seasonal = KRCEngine.simulateSeasonal({
      lat,
      thermalInertia,
      albedo,
      tau,
      elevation
    });

    this.lastResult = result;
    this.seasonalData = seasonal;

    this.summaryCard.style.display = 'block';
    this.container.querySelector('#krc-res-max').innerText = `${result.summary.maxTemp} K (${(result.summary.maxTemp - 273.15).toFixed(1)}°C)`;
    this.container.querySelector('#krc-res-min').innerText = `${result.summary.minTemp} K (${(result.summary.minTemp - 273.15).toFixed(1)}°C)`;
    this.container.querySelector('#krc-res-mean').innerText = `${result.summary.meanTemp} K`;
    this.container.querySelector('#krc-res-range').innerText = `${result.summary.diurnalRange} K`;
    this.container.querySelector('#krc-res-skindepth').innerText = `${result.summary.skinDepthCm} cm`;
    
    const frostEl = this.container.querySelector('#krc-res-frost');
    frostEl.style.display = result.summary.co2FrostOccurs ? 'block' : 'none';

    this.chart.setResult(result, seasonal);

    EventBus.emit(EVENTS.KRC_RESULT, result);
  }

  exportCSV() {
    if (!this.lastResult || !this.lastResult.diurnalCurve) {
      alert('Please run a simulation first.');
      return;
    }

    let csv = 'LocalHour_LTST,SurfaceTemp_K,Subsurface10cm_K,Subsurface50cm_K,SolarFlux_Wm2,CO2Frost\n';
    this.lastResult.diurnalCurve.forEach(d => {
      csv += `${d.hour},${d.surfaceTemp},${d.subsurface10cm},${d.subsurface50cm},${d.solarFlux},${d.isCO2Frost ? 1 : 0}\n`;
    });

    const blob = new Blob([csv], { type: 'text/csv' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `krc_thermal_lat${this.lastResult.params.lat}_Ls${this.lastResult.params.Ls}.csv`;
    a.click();
    URL.revokeObjectURL(url);
  }
}
