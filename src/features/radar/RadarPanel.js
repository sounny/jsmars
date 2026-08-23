import { RadarSounderEngine } from './RadarSounderEngine.js';
import { RadarChart } from './RadarChart.js';
import { EVENTS } from '../../constants.js';

/**
 * @class RadarPanel
 * @description UI panel for probing Mars subsurface ice layers, stratigraphy, and radar reflections.
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
    this.currentPreset = 'boreum';
    this.groundTrackLayer = L.layerGroup();
    this.isActive = false;

    if (this.container) {
      this.init();
    }
  }

  init() {
    this.container.innerHTML = `
      <div class="radar-panel" style="padding:8px; display:flex; flex-direction:column; gap:8px;">
        <label style="font-size:11px; color:#cbd5e1;">Subsurface Ground Track Region</label>
        <select id="radar-preset-select" class="stamp-select" style="padding:4px; background:#0f172a; color:#fff; border:1px solid #334155;">
          ${Object.entries(RadarSounderEngine.PRESETS).map(([k, v]) =>
            `<option value="${k}" ${k === 'boreum' ? 'selected' : ''}>${v.name}</option>`
          ).join('')}
        </select>

        <div style="display:flex; justify-content:space-between; gap:6px;">
          <div style="flex:1;">
            <label style="font-size:10px; color:#94a3b8;">Dielectric (ε_r)</label>
            <input type="number" id="radar-eps-input" value="3.15" step="0.05" min="1.0" max="9.0" style="width:100%; padding:3px; font-size:11px; background:#0f172a; color:#fff; border:1px solid #334155; border-radius:3px;">
          </div>
          <div style="flex:1;">
            <label style="font-size:10px; color:#94a3b8;">Loss Tangent (tan δ)</label>
            <input type="number" id="radar-loss-input" value="0.001" step="0.0005" min="0.0001" max="0.05" style="width:100%; padding:3px; font-size:11px; background:#0f172a; color:#fff; border:1px solid #334155; border-radius:3px;">
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

    // Initial simulation
    this.runSimulation();
  }

  runSimulation() {
    const epsR = parseFloat(this.epsInput.value) || 3.15;
    const lossTangent = parseFloat(this.lossInput.value) || 0.001;

    const data = RadarSounderEngine.simulateRadargram(this.currentPreset, 120, 60);
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
    a.download = `sharad_radargram_${this.currentPreset}_${Date.now()}.csv`;
    a.click();
    URL.revokeObjectURL(url);
  }
}
