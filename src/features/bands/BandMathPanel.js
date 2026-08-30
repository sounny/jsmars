import { BandMathEngine } from "./BandMathEngine.js";
import { BandMathLayer } from "./BandMathLayer.js";
import { EventBus } from "../../core/EventBus.js";
import { EVENTS } from "../../constants.js";

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
    this.container = typeof container === "string" ? document.getElementById(container) : container;
    this.map = map;
    this.layer = new BandMathLayer(map);
    this.canvas = null;
    this.ctx = null;

    if (this.container) {
      this.init();
    }
  }

  init() {
    this.container.innerHTML = `
      <div style="padding: 10px; font-size: 12px; color: #f8fafc;">
        <div style="margin-bottom: 8px;">
          <label style="font-size: 11px; color: #cbd5e1; font-weight: 500; display: block; margin-bottom: 3px;">Mineral Index Preset</label>
          <select id="bm-preset-select" class="tool-select" style="width: 100%; box-sizing: border-box; background: #1e293b; color: #f8fafc; border: 1px solid #475569;">
            ${BandMathEngine.MINERAL_PRESETS.map(p => `<option value="${p.id}">${p.name}</option>`).join("")}
            <option value="custom">-- Custom Band Math Formula --</option>
          </select>
        </div>

        <div id="bm-formula-group" style="margin-bottom: 8px;">
          <label style="font-size: 11px; color: #cbd5e1; font-weight: 500; display: block; margin-bottom: 3px;">Spectral Formula</label>
          <input type="text" id="bm-formula-input" class="tool-select" style="width: 100%; box-sizing: border-box; font-family: monospace; font-size: 11px; background: #1e293b; color: #f8fafc; border: 1px solid #475569;" value="1.0 - (B530 / (0.5 * (B440 + B600)))">
          <div id="bm-desc-text" style="font-size: 11px; color: #94a3b8; margin-top: 3px; line-height: 1.4;">
            Band depth at 530 nm diagnostic of crystalline ferric oxides.
          </div>
        </div>

        <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 6px; margin-bottom: 8px;">
          <div>
            <label style="font-size: 11px; color: #cbd5e1; font-weight: 500; display: block; margin-bottom: 3px;">Colormap</label>
            <select id="bm-colormap-select" class="tool-select" style="width: 100%; background: #1e293b; color: #f8fafc; border: 1px solid #475569;">
              <option value="magma">Magma</option>
              <option value="viridis">Viridis</option>
              <option value="coolwarm">Coolwarm</option>
              <option value="jet">Jet</option>
              <option value="rainbow">Rainbow</option>
              <option value="grayscale">Grayscale</option>
            </select>
          </div>
          <div>
            <label style="font-size: 11px; color: #cbd5e1; font-weight: 500; display: block; margin-bottom: 3px;">Min / Max Stretch</label>
            <div style="display: flex; gap: 4px;">
              <input type="number" id="bm-min-val" class="tool-select" style="width: 50%; background: #1e293b; color: #f8fafc; border: 1px solid #475569;" value="0.0" step="0.05">
              <input type="number" id="bm-max-val" class="tool-select" style="width: 50%; background: #1e293b; color: #f8fafc; border: 1px solid #475569;" value="0.25" step="0.05">
            </div>
          </div>
        </div>

        <div style="margin-bottom: 8px;">
          <div style="display: flex; justify-content: space-between; font-size: 11px; color: #cbd5e1; font-weight: 500; margin-bottom: 3px;">
            <span>Overlay Opacity</span>
            <span id="bm-opacity-val" style="color: #38bdf8; font-weight: 600;">65%</span>
          </div>
          <input type="range" id="bm-opacity-slider" min="10" max="100" value="65" style="width: 100%; cursor: pointer;">
        </div>

        <div style="margin-bottom: 8px; text-align: center;">
          <canvas id="bm-preview-canvas" width="220" height="50" style="border-radius: 4px; border: 1px solid #334155; background: #0f172a; width: 100%; height: 50px; display: block;"></canvas>
        </div>

        <div id="bm-status-banner" style="display: none; padding: 6px 8px; margin-bottom: 8px; border-radius: 4px; background: rgba(16, 185, 129, 0.15); border: 1px solid #10b981; font-size: 11px; color: #6ee7b7; text-align: center;">
          ● Active Mineral Colormap Overlay
        </div>

        <div style="display: flex; gap: 6px;">
          <button id="bm-apply-btn" class="tool-btn" style="flex: 1; font-size: 11px; background: #0284c7; font-weight: 600;">Apply Color Stretch</button>
          <button id="bm-clear-btn" class="tool-btn" style="display: none; width: 70px; font-size: 11px; background: #475569;">Clear</button>
        </div>
      </div>
    `;

    this.presetSelect = this.container.querySelector("#bm-preset-select");
    this.formulaInput = this.container.querySelector("#bm-formula-input");
    this.descText = this.container.querySelector("#bm-desc-text");
    this.colormapSelect = this.container.querySelector("#bm-colormap-select");
    this.minInput = this.container.querySelector("#bm-min-val");
    this.maxInput = this.container.querySelector("#bm-max-val");
    this.opacitySlider = this.container.querySelector("#bm-opacity-slider");
    this.opacityVal = this.container.querySelector("#bm-opacity-val");
    this.canvas = this.container.querySelector("#bm-preview-canvas");
    this.ctx = this.canvas.getContext("2d");
    this.statusBanner = this.container.querySelector("#bm-status-banner");
    this.applyBtn = this.container.querySelector("#bm-apply-btn");
    this.clearBtn = this.container.querySelector("#bm-clear-btn");

    this.bindEvents();
    this.updatePreview();
  }

  bindEvents() {
    this.presetSelect.addEventListener("change", (e) => {
      const presetId = e.target.value;
      if (presetId === "custom") {
        this.descText.innerText = "Custom band arithmetic: use standard band labels (e.g. B1, B2) and math operators.";
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
      if (this.layer.isActive) {
        this.applyLayer();
      }
    });

    this.opacitySlider.addEventListener("input", (e) => {
      const val = parseInt(e.target.value, 10);
      this.opacityVal.innerText = `${val}%`;
      this.layer.setParams({ opacity: val / 100 });
    });

    [this.formulaInput, this.colormapSelect, this.minInput, this.maxInput].forEach(el => {
      el.addEventListener("input", () => {
        this.updatePreview();
        if (this.layer.isActive) {
          this.applyLayer();
        }
      });
      el.addEventListener("change", () => {
        this.updatePreview();
        if (this.layer.isActive) {
          this.applyLayer();
        }
      });
    });

    this.applyBtn.addEventListener("click", () => {
      this.applyLayer();
    });

    this.clearBtn.addEventListener("click", () => {
      this.clearLayer();
    });

    EventBus.on(EVENTS.BODY_CHANGED, () => {
      if (this.layer.isActive) {
        this.clearLayer();
      }
    });
  }

  applyLayer() {
    const presetName = this.presetSelect.options[this.presetSelect.selectedIndex].text;
    const colormap = this.colormapSelect.value;
    const opacity = parseInt(this.opacitySlider.value, 10) / 100;

    const detail = {
      preset: this.presetSelect.value,
      formula: this.formulaInput.value,
      colormap: colormap,
      min: parseFloat(this.minInput.value) || 0,
      max: parseFloat(this.maxInput.value) || 1,
      opacity: opacity
    };

    this.layer.setParams(detail);
    this.layer.activate();

    // Update UI state
    this.statusBanner.style.display = "block";
    this.statusBanner.innerText = `● Active: ${presetName.split(" ")[0]} (${colormap.toUpperCase()})`;
    this.applyBtn.innerText = "✓ Update Stretch";
    this.applyBtn.style.background = "#059669";
    this.clearBtn.style.display = "block";

    EventBus.emit(EVENTS.BAND_MATH_APPLIED, detail);
  }

  clearLayer() {
    this.layer.deactivate();
    this.statusBanner.style.display = "none";
    this.applyBtn.innerText = "Apply Color Stretch";
    this.applyBtn.style.background = "#0284c7";
    this.clearBtn.style.display = "none";
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
