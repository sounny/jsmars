import { RadarSounderEngine } from './RadarSounderEngine.js';
import { RadarChart } from './RadarChart.js';
import { EVENTS } from '../../constants.js';
import { jmarsState } from '../../jmars-state.js';

/**
 * @class RadarPanel
 * @description UI panel for probing subsurface ice layers, stratigraphy, and radar reflections
 * across planetary bodies (Mars SHARAD / MARSIS, Europa REASON / JUICE RIME).
 */
export class RadarPanel {
  /**
   * @param {HTMLElement|string} containerOrId
   * @param {L.Map} map
   */
  constructor(containerOrId, map) {
    this.container = typeof containerOrId === 'string'
      ? document.getElementById(containerOrId)
      : containerOrId;
    this.map = map;
    this.chart = null;
    this.currentBody = (jmarsState && jmarsState.get('body')) || 'mars';
    this.currentPreset = this.currentBody === 'europa' ? 'europa_thera' : 'boreum';
    this.groundTrackLayer = L.layerGroup();
    this.isActive = false;

    if (this.container) {
      this.init();
    }
  }

  init() {
    this.container.innerHTML = `
      <div class="radar-panel" style="padding:8px; display:flex; flex-direction:column; gap:8px;">
        <!-- Scientific Data Provenance & Simulation Notice (AGENTS.md Rule 9.D) -->
        <div style="background:rgba(15, 23, 42, 0.7); border:1px solid #334155; border-radius:4px; padding:6px; font-size:10px; line-height:1.4;">
          <div style="display:flex; align-items:center; justify-content:space-between; margin-bottom:3px;">
            <span style="font-weight:600; color:#38bdf8; display:flex; align-items:center; gap:4px;">
              📡 <span id="radar-instrument-label">SHARAD / MARSIS Model</span>
            </span>
            <span style="font-size:9px; background:#0369a1; color:#fff; padding:1px 4px; border-radius:2px; text-transform:uppercase; letter-spacing:0.5px;">Simulation</span>
          </div>
          <p style="color:#94a3b8; margin:0 0 4px 0;">
            Physically-based synthetic radargram using electrodynamic dielectric horizons, two-way travel time, and attenuation.
          </p>
          <div style="display:flex; gap:8px;">
            <a href="https://pds-geosciences.wustl.edu/missions/mro/sharad.htm" target="_blank" rel="noopener" style="color:#38bdf8; text-decoration:none; font-size:9px;">↗ PDS SHARAD Archive</a>
            <a href="https://trek.nasa.gov/europa" target="_blank" rel="noopener" style="color:#38bdf8; text-decoration:none; font-size:9px;">↗ Europa Trek</a>
          </div>
        </div>

        <label style="font-size:11px; color:#cbd5e1;">Subsurface Ground Track Region</label>
        <select id="radar-preset-select" class="stamp-select" style="padding:4px; background:#0f172a; color:#fff; border:1px solid #334155;">
          ${this.renderPresetOptions()}
        </select>

        <div style="display:flex; justify-content:space-between; gap:6px;">
          <div style="flex:1;">
            <label style="font-size:10px; color:#94a3b8;">Dielectric (ε_r)</label>
            <input type="number" id="radar-eps-input" value="3.15" step="0.05" min="1.0" max="90.0" style="width:100%; padding:3px; font-size:11px; background:#0f172a; color:#fff; border:1px solid #334155; border-radius:3px;">
          </div>
          <div style="flex:1;">
            <label style="font-size:10px; color:#94a3b8;">Loss Tangent (tan δ)</label>
            <input type="number" id="radar-loss-input" value="0.001" step="0.0001" min="0.0001" max="0.05" style="width:100%; padding:3px; font-size:11px; background:#0f172a; color:#fff; border:1px solid #334155; border-radius:3px;">
          </div>
        </div>

        <div style="display:flex; gap:4px;">
          <button id="radar-run-btn" class="tool-btn" style="flex:1; background:#0284c7;">Synthesize Radargram</button>
          <button id="radar-fly-btn" class="tool-btn" style="flex:0.6; font-size:11px;">Fly to Track</button>
        </div>

        <div id="radar-chart-container" style="margin-top:4px;"></div>

        <div style="display:flex; justify-content:space-between; gap:4px;">
          <button id="radar-export-btn" class="crater-action-btn" style="background:#1e293b; font-size:10px; flex:1;">Export Radar CSV</button>
        </div>
      </div>
    `;

    this.chart = new RadarChart(this.container.querySelector('#radar-chart-container'));

    this.instrumentLabel = this.container.querySelector('#radar-instrument-label');
    this.presetSelect = this.container.querySelector('#radar-preset-select');
    this.epsInput = this.container.querySelector('#radar-eps-input');
    this.lossInput = this.container.querySelector('#radar-loss-input');
    this.runBtn = this.container.querySelector('#radar-run-btn');
    this.flyBtn = this.container.querySelector('#radar-fly-btn');
    this.exportBtn = this.container.querySelector('#radar-export-btn');

    this.presetSelect.addEventListener('change', (e) => {
      this.currentPreset = e.target.value;
      const preset = RadarSounderEngine.PRESETS[this.currentPreset];
      if (preset) {
        this.epsInput.value = preset.dielectricConstant;
        this.lossInput.value = preset.lossTangent;
      }
      this.runSimulation();
    });

    this.runBtn.addEventListener('click', () => this.runSimulation());
    this.flyBtn.addEventListener('click', () => this.flyToTrack());
    this.exportBtn.addEventListener('click', () => this.exportCSV());

    // Listen for body changes to switch presets and instrument labeling
    document.addEventListener(EVENTS.BODY_CHANGED, (e) => {
      const body = (e.detail && e.detail.body) ? e.detail.body.toLowerCase() : 'mars';
      this.setBody(body);
    });

    // Sync to current initial preset values
    this.syncPresetInputs();

    // Initial simulation
    this.runSimulation();
  }

  renderPresetOptions() {
    const marsPresets = Object.entries(RadarSounderEngine.PRESETS).filter(([_, v]) => !v.body || v.body === 'mars');
    const europaPresets = Object.entries(RadarSounderEngine.PRESETS).filter(([_, v]) => v.body === 'europa');

    return `
      <optgroup label="Mars (SHARAD / MARSIS)">
        ${marsPresets.map(([k, v]) =>
          `<option value="${k}" ${k === this.currentPreset ? 'selected' : ''}>${v.name}</option>`
        ).join('')}
      </optgroup>
      <optgroup label="Europa (REASON / JUICE RIME)">
        ${europaPresets.map(([k, v]) =>
          `<option value="${k}" ${k === this.currentPreset ? 'selected' : ''}>${v.name}</option>`
        ).join('')}
      </optgroup>
    `;
  }

  setBody(body) {
    this.currentBody = body;
    if (body === 'europa') {
      this.currentPreset = 'europa_thera';
      if (this.instrumentLabel) {
        this.instrumentLabel.textContent = 'Europa Clipper REASON / JUICE RIME Model';
      }
    } else {
      this.currentPreset = 'boreum';
      if (this.instrumentLabel) {
        this.instrumentLabel.textContent = 'SHARAD / MARSIS Model';
      }
    }

    if (this.presetSelect) {
      this.presetSelect.value = this.currentPreset;
    }
    this.syncPresetInputs();
    this.runSimulation();
  }

  syncPresetInputs() {
    const preset = RadarSounderEngine.PRESETS[this.currentPreset];
    if (preset) {
      if (this.epsInput) this.epsInput.value = preset.dielectricConstant;
      if (this.lossInput) this.lossInput.value = preset.lossTangent;
    }
  }

  runSimulation() {
    const epsR = parseFloat(this.epsInput.value) || 3.15;
    const lossTangent = parseFloat(this.lossInput.value) || 0.001;

    const preset = RadarSounderEngine.PRESETS[this.currentPreset] || RadarSounderEngine.PRESETS.boreum;
    // Adapt track length and trace count: for Europa ice shell (15-22 km thick), use 150 km track
    const trackKm = preset.body === 'europa' ? 150 : 120;

    const data = RadarSounderEngine.simulateRadargram(this.currentPreset, trackKm, 60);
    this.lastData = data;
    this.chart.setData(data);
  }

  flyToTrack() {
    const preset = RadarSounderEngine.PRESETS[this.currentPreset];
    if (preset && this.map) {
      this.map.flyTo([preset.lat, preset.lon], 5);
    }
  }

  exportCSV() {
    if (!this.lastData) return;
    const depths = this.lastData.depths;
    const twt = this.lastData.twt;
    const grid = this.lastData.grid;

    const header = ['Sample', 'TWT_microsec', 'Depth_meters', ...this.lastData.distances.map(d => `Dist_${d.toFixed(1)}km`)].join(',');
    const rows = [];

    for (let r = 0; r < depths.length; r++) {
      const colPowers = grid.map(c => c[r].toFixed(2));
      rows.push([r, twt[r].toFixed(4), depths[r].toFixed(1), ...colPowers].join(','));
    }

    const csvContent = [header, ...rows].join('\n');
    const blob = new Blob([csvContent], { type: 'text/csv' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    const prefix = (RadarSounderEngine.PRESETS[this.currentPreset]?.body === 'europa') ? 'reason_radargram' : 'sharad_radargram';
    a.download = `${prefix}_${this.currentPreset}_${Date.now()}.csv`;
    a.click();
    URL.revokeObjectURL(url);
  }
}
