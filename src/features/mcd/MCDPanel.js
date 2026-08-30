import { MCDEngine } from './MCDEngine.js';
import { MCDChart } from './MCDChart.js';
import { EventBus } from '../../core/EventBus.js';
import { EVENTS } from '../../constants.js';

/**
 * @module MCDPanel
 * @description UI control panel for Mars Climate Database (MCD) atmospheric profiling.
 */
export class MCDPanel {
  /**
   * @param {HTMLElement|string} container - DOM element or ID
   * @param {L.Map} map - Leaflet map instance
   */
  constructor(container, map) {
    this.container = typeof container === 'string' ? document.getElementById(container) : container;
    this.map = map;
    this.chart = null;
    this.isPicking = false;
    this.lastProfile = null;

    if (this.container) {
      this.init();
    }
  }

  init() {
    this.container.innerHTML = `
      <div style="padding: 10px; font-size: 12px; color: #f8fafc;">
        <div style="margin-bottom: 8px;">
          <label style="font-size: 11px; color: #cbd5e1; font-weight: 500; display: block; margin-bottom: 3px;">Model Engine & Data Source</label>
          <select id="mcd-input-source" class="tool-select" style="width: 100%; box-sizing: border-box; font-size: 11px; background: #1e293b; color: #f8fafc; border: 1px solid #475569;">
            <option value="analytical" selected>🧪 1D Analytical Physics Model (Instant / Offline)</option>
            <option value="lmd_live">📡 LMD MCD v6.1 (Live GCM / Remote Server)</option>
          </select>
        </div>

        <div style="margin-bottom: 8px;">
          <label style="font-size: 11px; color: #cbd5e1; font-weight: 500; display: block; margin-bottom: 3px;">Climatology Scenario</label>
          <select id="mcd-input-scenario" class="tool-select" style="width: 100%; box-sizing: border-box; font-size: 11px; background: #1e293b; color: #f8fafc; border: 1px solid #475569;">
            <option value="1" selected>Climatology (Average Solar / TES Climatology)</option>
            <option value="2">Cold Scenario (Min Solar / Low Dust)</option>
            <option value="3">Warm Scenario (Max Solar / High Dust)</option>
            <option value="4">Global Dust Storm Scenario</option>
          </select>
        </div>

        <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 8px; margin-bottom: 8px;">
          <div>
            <label style="font-size: 11px; color: #cbd5e1; font-weight: 500; display: block; margin-bottom: 3px;">Latitude (°)</label>
            <input type="number" id="mcd-input-lat" class="tool-select" style="width: 100%; box-sizing: border-box; background: #1e293b; color: #f8fafc; border: 1px solid #475569;" value="0" min="-90" max="90" step="1">
          </div>
          <div>
            <label style="font-size: 11px; color: #cbd5e1; font-weight: 500; display: block; margin-bottom: 3px;">Longitude (°E)</label>
            <input type="number" id="mcd-input-lon" class="tool-select" style="width: 100%; box-sizing: border-box; background: #1e293b; color: #f8fafc; border: 1px solid #475569;" value="0" min="0" max="360" step="5">
          </div>
          <div>
            <label style="font-size: 11px; color: #cbd5e1; font-weight: 500; display: block; margin-bottom: 3px;">Solar Longitude L<sub>s</sub> (°)</label>
            <input type="number" id="mcd-input-ls" class="tool-select" style="width: 100%; box-sizing: border-box; background: #1e293b; color: #f8fafc; border: 1px solid #475569;" value="0" min="0" max="360" step="5">
          </div>
          <div>
            <label style="font-size: 11px; color: #cbd5e1; font-weight: 500; display: block; margin-bottom: 3px;">Local Hour (0-24h)</label>
            <input type="number" id="mcd-input-hour" class="tool-select" style="width: 100%; box-sizing: border-box; background: #1e293b; color: #f8fafc; border: 1px solid #475569;" value="12" min="0" max="24" step="1">
          </div>
          <div>
            <label style="font-size: 11px; color: #cbd5e1; font-weight: 500; display: block; margin-bottom: 3px;">Elevation (m)</label>
            <input type="number" id="mcd-input-elev" class="tool-select" style="width: 100%; box-sizing: border-box; background: #1e293b; color: #f8fafc; border: 1px solid #475569;" value="0" min="-8000" max="22000" step="500">
          </div>
          <div>
            <label style="font-size: 11px; color: #cbd5e1; font-weight: 500; display: block; margin-bottom: 3px;">Max Altitude (km)</label>
            <input type="number" id="mcd-input-maxalt" class="tool-select" style="width: 100%; box-sizing: border-box; background: #1e293b; color: #f8fafc; border: 1px solid #475569;" value="50" min="20" max="80" step="10">
          </div>
        </div>

        <div style="display: flex; gap: 6px; margin-bottom: 10px;">
          <button id="mcd-pick-btn" class="tool-btn" style="flex: 1; font-size: 11px; background: #334155;">📍 Pick Location</button>
          <button id="mcd-run-btn" class="tool-btn" style="flex: 1; font-size: 11px; background: #0284c7; font-weight: 600;">Calculate Profile</button>
        </div>

        <div id="mcd-summary-card" style="display: none; background: #0f172a; border: 1px solid #1e293b; border-radius: 4px; padding: 6px 8px; margin-bottom: 10px; font-size: 10px;">
          <div id="mcd-source-badge" style="font-size: 9px; color: #38bdf8; margin-bottom: 4px; font-weight: 600;">--</div>
          <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 4px;">
            <div>Surface P: <b id="mcd-res-p" style="color: #38bdf8;">--</b></div>
            <div>Surface T: <b id="mcd-res-t" style="color: #f97316;">--</b></div>
            <div>Scale Height H: <b id="mcd-res-h" style="color: #a855f7;">--</b></div>
            <div>Surface ρ: <b id="mcd-res-rho" style="color: #34d399;">--</b></div>
          </div>
        </div>

        <div id="mcd-chart-container" style="margin-bottom: 8px;"></div>

        <div style="display: flex; gap: 6px;">
          <button id="mcd-export-csv-btn" class="tool-btn" style="flex: 1; font-size: 11px;">Export CSV</button>
          <a id="mcd-portal-link" href="https://www-mars.lmd.jussieu.fr/mcd_python/" target="_blank" rel="noopener" class="tool-btn" style="flex: 1; font-size: 11px; text-align: center; text-decoration: none; display: flex; align-items: center; justify-content: center; background: #334155;">🔗 LMD Portal</a>
        </div>
      </div>
    `;

    this.sourceInput = this.container.querySelector('#mcd-input-source');
    this.scenarioInput = this.container.querySelector('#mcd-input-scenario');
    this.latInput = this.container.querySelector('#mcd-input-lat');
    this.lonInput = this.container.querySelector('#mcd-input-lon');
    this.lsInput = this.container.querySelector('#mcd-input-ls');
    this.hourInput = this.container.querySelector('#mcd-input-hour');
    this.elevInput = this.container.querySelector('#mcd-input-elev');
    this.maxAltInput = this.container.querySelector('#mcd-input-maxalt');
    this.pickBtn = this.container.querySelector('#mcd-pick-btn');
    this.runBtn = this.container.querySelector('#mcd-run-btn');
    this.exportCsvBtn = this.container.querySelector('#mcd-export-csv-btn');
    this.portalLink = this.container.querySelector('#mcd-portal-link');
    this.summaryCard = this.container.querySelector('#mcd-summary-card');
    this.sourceBadge = this.container.querySelector('#mcd-source-badge');

    this.chart = new MCDChart(this.container.querySelector('#mcd-chart-container'));

    this.bindEvents();

    EventBus.on(EVENTS.TIME_CHANGED, (detail) => {
      if (detail && typeof detail.Ls === 'number' && !this.isPicking) {
        this.lsInput.value = detail.Ls.toFixed(1);
      }
    });
  }

  bindEvents() {
    this.runBtn.addEventListener('click', () => this.runProfile());

    this.pickBtn.addEventListener('click', () => {
      this.isPicking = !this.isPicking;
      this.pickBtn.style.background = this.isPicking ? '#0284c7' : '#334155';
      this.pickBtn.innerText = this.isPicking ? 'Click map...' : '📍 Pick Location';
      this.map.getContainer().style.cursor = this.isPicking ? 'crosshair' : '';
    });

    this.map.on('click', (e) => {
      if (!this.isPicking) return;
      this.latInput.value = e.latlng.lat.toFixed(2);
      this.lonInput.value = (((e.latlng.lng % 360) + 360) % 360).toFixed(2);
      this.isPicking = false;
      this.pickBtn.style.background = '#334155';
      this.pickBtn.innerText = '📍 Pick Location';
      this.map.getContainer().style.cursor = '';
      this.runProfile();
    });

    this.exportCsvBtn.addEventListener('click', () => this.exportCSV());
  }

  async runProfile() {
    const lat = parseFloat(this.latInput.value) || 0;
    const lon = parseFloat(this.lonInput.value) || 0;
    const Ls = parseFloat(this.lsInput.value) || 0;
    const localHour = parseFloat(this.hourInput.value) || 12;
    const elevation = parseFloat(this.elevInput.value) || 0;
    const maxAltitudeKm = parseFloat(this.maxAltInput.value) || 50;
    const source = this.sourceInput?.value || 'lmd_live';
    const dust = parseInt(this.scenarioInput?.value || '1', 10);

    const prevBtnText = this.runBtn.innerText;
    this.runBtn.disabled = true;
    this.runBtn.innerText = source === 'lmd_live' ? 'Fetching LMD GCM...' : 'Calculating...';

    let profile;
    if (source === 'lmd_live') {
      try {
        profile = await MCDEngine.fetchLMDProfile({
          lat,
          lon,
          Ls,
          localHour,
          dust,
          maxAltitudeKm
        });
      } catch (err) {
        console.warn('LMD MCD Live API fetch failed, falling back to analytical model:', err);
        profile = MCDEngine.computeProfile({
          lat,
          lon,
          Ls,
          localHour,
          elevation,
          maxAltitudeKm
        });
        profile.source = '1D Analytical Physics Model (Offline Fallback)';
      }
    } else {
      profile = MCDEngine.computeProfile({
        lat,
        lon,
        Ls,
        localHour,
        elevation,
        maxAltitudeKm
      });
      profile.source = '1D Analytical Physics Model';
    }

    this.runBtn.disabled = false;
    this.runBtn.innerText = prevBtnText;

    this.lastProfile = profile;

    this.summaryCard.style.display = 'block';
    this.sourceBadge.innerText = profile.source || 'LMD MCD v6.1 (CNRS/ESA)';
    this.container.querySelector('#mcd-res-p').innerText = `${profile.surface.pressurePa} Pa`;
    this.container.querySelector('#mcd-res-t').innerText = `${profile.surface.temperatureK} K`;
    this.container.querySelector('#mcd-res-h').innerText = `${profile.surface.scaleHeightKm} km`;
    this.container.querySelector('#mcd-res-rho').innerText = `${profile.surface.surfaceDensity} kg/m³`;

    if (profile.lmdWebUrl && this.portalLink) {
      this.portalLink.href = profile.lmdWebUrl;
    }

    this.chart.setProfile(profile);

    EventBus.emit(EVENTS.MCD_RESULT, profile);
  }

  exportCSV() {
    if (!this.lastProfile || !this.lastProfile.layers) {
      alert('Please calculate an atmospheric profile first.');
      return;
    }

    let csv = `Altitude_km,Temperature_K,Pressure_Pa,Density_kg_m3,WindSpeed_m_s,DustDensity\n`;
    this.lastProfile.layers.forEach(l => {
      csv += `${l.altitudeKm},${l.temperatureK},${l.pressurePa},${l.densityKgM3},${l.windSpeedMs},${l.dustDensity}\n`;
    });

    const blob = new Blob([csv], { type: 'text/csv' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `mcd_profile_lat${this.lastProfile.location.lat}_lon${this.lastProfile.location.lon}.csv`;
    a.click();
    URL.revokeObjectURL(url);
  }
}
