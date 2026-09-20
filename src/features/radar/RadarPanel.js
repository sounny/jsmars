import { RadarSounderEngine } from './RadarSounderEngine.js';
import { RadarChart } from './RadarChart.js';
import { EVENTS } from '../../constants.js';
import { jmarsState } from '../../jmars-state.js';
import {
  PROVENANCE_KIND,
  SCIENCE_ARCHIVES,
  provenanceBannerHTML
} from '../../ui/ScienceProvenance.js';

const MARS_DISCLAIMER = 'Physically-based simulation using illustrative dielectric presets, NOT observed SHARAD or MARSIS radargrams. Horizons are hand-authored analog parameters, not measured reflectors.';
const EUROPA_DISCLAIMER = 'Physically-based simulation using illustrative ice-shell presets, NOT observed REASON or RIME radargrams. Clipper/JUICE sounding products are not ingested here.';
const MARS_SOURCE = 'Source: client-side physics model · illustrative SHARAD/MARSIS analog parameters';
const EUROPA_SOURCE = 'Source: client-side physics model · illustrative REASON/RIME analog parameters';

/**
 * @class RadarPanel
 * @description UI panel for a physically-based subsurface radar simulation
 * (SHARAD/MARSIS analog on Mars; REASON/RIME analog on Europa). Results are
 * synthetic radargrams, not PDS-measured returns.
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
    const isEuropa = this.currentBody === 'europa';
    this.container.innerHTML = `
      <div class="radar-panel" style="padding:8px; display:flex; flex-direction:column; gap:8px;">
        ${provenanceBannerHTML({
          kind: PROVENANCE_KIND.MODEL,
          titleId: 'radar-instrument-label',
          title: isEuropa
            ? 'Europa radar simulation (REASON / RIME analog)'
            : 'Mars radar simulation (SHARAD / MARSIS analog)',
          bodyId: 'radar-disclaimer',
          body: isEuropa ? EUROPA_DISCLAIMER : MARS_DISCLAIMER,
          sourceId: 'radar-source-string',
          source: isEuropa ? EUROPA_SOURCE : MARS_SOURCE,
          linksId: 'radar-archive-links',
          links: this.archiveLinksForBody(this.currentBody)
        })}

        <label style="font-size:11px; color:#cbd5e1;" for="radar-preset-select">Illustrative ground-track region (not a measured orbit track)</label>
        <select id="radar-preset-select" class="stamp-select" style="padding:4px; background:#0f172a; color:#fff; border:1px solid #334155;">
          ${this.renderPresetOptions()}
        </select>

        <div style="display:flex; justify-content:space-between; gap:6px;">
          <div style="flex:1;">
            <label style="font-size:10px; color:#94a3b8;" for="radar-eps-input">Dielectric (ε_r)</label>
            <input type="number" id="radar-eps-input" value="3.15" step="0.05" min="1.0" max="90.0" style="width:100%; padding:3px; font-size:11px; background:#0f172a; color:#fff; border:1px solid #334155; border-radius:3px;">
          </div>
          <div style="flex:1;">
            <label style="font-size:10px; color:#94a3b8;" for="radar-loss-input">Loss Tangent (tan δ)</label>
            <input type="number" id="radar-loss-input" value="0.001" step="0.0001" min="0.0001" max="0.05" style="width:100%; padding:3px; font-size:11px; background:#0f172a; color:#fff; border:1px solid #334155; border-radius:3px;">
          </div>
        </div>

        <div style="display:flex; gap:4px;">
          <button id="radar-run-btn" class="tool-btn" style="flex:1; background:#0284c7;">Synthesize Radargram</button>
          <button id="radar-fly-btn" class="tool-btn" style="flex:0.6; font-size:11px;">Fly to Track</button>
        </div>

        <div id="radar-chart-container" style="margin-top:4px;"></div>

        <div style="display:flex; justify-content:space-between; gap:4px;">
          <button id="radar-export-btn" class="crater-action-btn" style="background:#1e293b; font-size:10px; flex:1;">Export Synthetic Radar CSV</button>
        </div>
      </div>
    `;

    this.chart = new RadarChart(this.container.querySelector('#radar-chart-container'));

    this.instrumentLabel = this.container.querySelector('#radar-instrument-label');
    this.disclaimerEl = this.container.querySelector('#radar-disclaimer');
    this.sourceEl = this.container.querySelector('#radar-source-string');
    this.archiveLinksEl = this.container.querySelector('#radar-archive-links');
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

  archiveLinksForBody(body) {
    if (body === 'europa') {
      return [SCIENCE_ARCHIVES.EUROPA_REASON, SCIENCE_ARCHIVES.SHARAD, SCIENCE_ARCHIVES.MARSIS];
    }
    return [SCIENCE_ARCHIVES.SHARAD, SCIENCE_ARCHIVES.MARSIS];
  }

  renderPresetOptions() {
    const marsPresets = Object.entries(RadarSounderEngine.PRESETS).filter(([_, v]) => !v.body || v.body === 'mars');
    const europaPresets = Object.entries(RadarSounderEngine.PRESETS).filter(([_, v]) => v.body === 'europa');

    return `
      <optgroup label="Mars (illustrative SHARAD / MARSIS analog presets)">
        ${marsPresets.map(([k, v]) =>
          `<option value="${k}" ${k === this.currentPreset ? 'selected' : ''}>${v.name}</option>`
        ).join('')}
      </optgroup>
      <optgroup label="Europa (illustrative REASON / RIME analog presets)">
        ${europaPresets.map(([k, v]) =>
          `<option value="${k}" ${k === this.currentPreset ? 'selected' : ''}>${v.name}</option>`
        ).join('')}
      </optgroup>
    `;
  }

  setBody(body) {
    this.currentBody = body;
    const isEuropa = body === 'europa';
    if (isEuropa) {
      this.currentPreset = 'europa_thera';
      if (this.instrumentLabel) {
        this.instrumentLabel.textContent = 'Europa radar simulation (REASON / RIME analog)';
      }
      if (this.disclaimerEl) this.disclaimerEl.textContent = EUROPA_DISCLAIMER;
      if (this.sourceEl) this.sourceEl.textContent = EUROPA_SOURCE;
    } else {
      this.currentPreset = 'boreum';
      if (this.instrumentLabel) {
        this.instrumentLabel.textContent = 'Mars radar simulation (SHARAD / MARSIS analog)';
      }
      if (this.disclaimerEl) this.disclaimerEl.textContent = MARS_DISCLAIMER;
      if (this.sourceEl) this.sourceEl.textContent = MARS_SOURCE;
    }

    if (this.archiveLinksEl) {
      this.archiveLinksEl.replaceChildren();
      this.archiveLinksForBody(body).forEach((link) => {
        const a = document.createElement('a');
        a.className = 'science-provenance__link';
        a.href = link.href;
        a.target = '_blank';
        a.rel = 'noopener noreferrer';
        a.textContent = link.label;
        this.archiveLinksEl.appendChild(a);
      });
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
    const preset = RadarSounderEngine.PRESETS[this.currentPreset] || RadarSounderEngine.PRESETS.boreum;
    // Adapt track length and trace count: for Europa ice shell (15-22 km thick), use 150 km track
    const trackKm = preset.body === 'europa' ? 150 : 120;

    const data = RadarSounderEngine.simulateRadargram(this.currentPreset, trackKm, 60);
    data.modelLabel = preset.body === 'europa'
      ? 'Synthetic radargram (REASON / RIME analog model)'
      : 'Synthetic radargram (SHARAD / MARSIS analog model)';
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

    const csvContent = [
      '# JSMARS synthetic radargram (physics model, illustrative parameters — not observed SHARAD/MARSIS/REASON returns)',
      header,
      ...rows
    ].join('\n');
    const blob = new Blob([csvContent], { type: 'text/csv' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `synthetic_radargram_${this.currentPreset}_${Date.now()}.csv`;
    a.click();
    URL.revokeObjectURL(url);
  }
}
