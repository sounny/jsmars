import { BandMathEngine } from './BandMathEngine.js';
import { EventBus } from '../../core/EventBus.js';
import { EVENTS } from '../../constants.js';

/**
 * @module BandMathPanel
 * @description UI control panel for Spectral Band Math, Mineral Indices, and False-Color Ratios.
 */
export class BandMathPanel {
  /**
   * @param {HTMLElement|string} container - Container DOM element or ID
   * @param {L.Map} map - Leaflet map instance
   */
  constructor(container, map) {
    this.container = typeof container === 'string' ? document.getElementById(container) : container;
    this.map = map;
    this.canvas = null;
    this.ctx = null;

    if (this.container) {
      this.init();
    }
  }

  init() {
    this.container.innerHTML = `
      <div style="padding: 10px; font-size: 12px; color: #e2e8f0;">
        <div style="margin-bottom: 8px;">
          <label style="font-size: 10px; color: #94a3b8; display: block; margin-bottom: 2px;">Mineral Index Preset</label>
          <select id="bm-preset-select" class="tool-select" style="width: 100%; box-sizing: border-box;">
            ${BandMathEngine.MINERAL_PRESETS.map(p => `<option value="${p.id}">${p.name}</option>`).join('')}
            <option value="custom">-- Custom Band Math Formula --</option>
          </select>
        </div>

        <div id="bm-formula-group" style="margin-bottom: 8px;">
          <label style="font-size: 10px; color: #94a3b8; display: block; margin-bottom: 2px;">Spectral Formula</label>
          <input type="text" id="bm-formula-input" class="tool-select" style="width: 100%; box-sizing: border-box; font-family: monospace; font-size: 11px;" value="1.0 - (B530 / (0.5 * (B440 + B600)))">
          <div id="bm-desc-text" style="font-size: 10px; color: #64748b; margin-top: 2px;">
            Band depth at 530 nm diagnostic of crystalline ferric oxides.
          </div>
        </div>

        <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 6px; margin-bottom: 8px;">
          <div>
            <label style="font-size: 10px; color: #94a3b8; display: block;">Colormap</label>
            <select id="bm-colormap-select" class="tool-select" style="width: 100%;">
              <option value="magma">Magma</option>
              <option value="viridis">Viridis</option>
              <option value="coolwarm">Coolwarm</option>
              <option value="jet">Jet</option>
              <option value="rainbow">Rainbow</option>
              <option value="grayscale">Grayscale</option>
            </select>
          </div>
          <div>
            <label style="font-size: 10px; color: #94a3b8; display: block;">Min / Max Stretch</label>
            <div style="display: flex; gap: 4px;">
              <input type="number" id="bm-min-val" class="tool-select" style="width: 50%;" value="0.0" step="0.05">
              <input type="number" id="bm-max-val" class="tool-select" style="width: 50%;" value="0.25" step="0.05">
            </div>
          </div>
        </div>

        <div style="margin-bottom: 8px; text-align: center;">
          <canvas id="bm-preview-canvas" width="220" height="70" style="border-radius: 4px; border: 1px solid #334155; background: #0f172a; width: 100%; height: 70px; display: block;"></canvas>
        </div>

        <div style="display: flex; gap: 6px;">
          <button id="bm-apply-btn" class="tool-btn" style="flex: 1; font-size: 11px; background: #0284c7; font-weight: 600;">Apply Color Stretch</button>
        </div>
      </div>
    `;

    this.presetSelect = this.container.querySelector('#bm-preset-select');
    this.formulaInput = this.container.querySelector('#bm-formula-input');
    this.descText = this.container.querySelector('#bm-desc-text');
    this.colormapSelect = this.container.querySelector('#bm-colormap-select');
    this.minInput = this.container.querySelector('#bm-min-val');
    this.maxInput = this.container.querySelector('#bm-max-val');
    this.canvas = this.container.querySelector('#bm-preview-canvas');
    this.ctx = this.canvas.getContext('2d');
    this.applyBtn = this.container.querySelector('#bm-apply-btn');

    this.bindEvents();
    this.updatePreview();
  }

  bindEvents() {
    this.presetSelect.addEventListener('change', (e) => {
      const presetId = e.target.value;
      if (presetId === 'custom') {
        this.descText.innerText = 'Custom band arithmetic: use standard band labels (e.g. B1, B2) and math operators.';
      } else {
        const preset = BandMathEngine.MINERAL_PRESETS.find(p => p.id === presetId);
        if (preset) {
          this.formulaInput.value = preset.formula;
          this.descText.innerText = preset.description;
          this.colormapSelect.value = preset.colormap;
          this.minInput.value = preset.min;
          this.maxInput.value = preset.max;
        }
      }
      this.updatePreview();
    });

    [this.formulaInput, this.colormapSelect, this.minInput, this.maxInput].forEach(el => {
      el.addEventListener('input', () => this.updatePreview());
      el.addEventListener('change', () => this.updatePreview());
    });

    this.applyBtn.addEventListener('click', () => {
      const detail = {
        preset: this.presetSelect.value,
        formula: this.formulaInput.value,
        colormap: this.colormapSelect.value,
        min: parseFloat(this.minInput.value) || 0,
        max: parseFloat(this.maxInput.value) || 1
      };
      EventBus.emit(EVENTS.BAND_MATH_APPLIED, detail);
      alert(`Applied mineral index: ${this.presetSelect.options[this.presetSelect.selectedIndex].text}`);
    });
  }

  updatePreview() {
    if (!this.canvas) return;
    const formula = this.formulaInput.value;
    const colormap = this.colormapSelect.value;
    const minVal = parseFloat(this.minInput.value) || 0;
    const maxVal = parseFloat(this.maxInput.value) || 1;

    const imgData = BandMathEngine.generatePreview(formula, this.canvas.width, this.canvas.height, colormap, minVal, maxVal);
    this.ctx.putImageData(imgData, 0, 0);
  }
}
