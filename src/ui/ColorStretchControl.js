/**
 * @module ColorStretchControl
 * @description Provides a UI for adjusting WMS layer color rendering.
 * In JMARS desktop, this is the "Color Stretch" panel.
 *
 * For WMS, we use SLD_BODY or STYLES parameters to request different renderings.
 * For local tile layers, we apply CSS filters (brightness, contrast, hue-rotate).
 */
export class ColorStretchControl {
  /**
   * @param {L.Map} map
   */
  constructor(map) {
    this.map = map;
    this.currentLayer = null;
    this.container = null;
    this._build();
  }

  _build() {
    this.container = document.createElement('div');
    this.container.id = 'color-stretch-panel';
    this.container.style.display = 'none';

    this.container.innerHTML = `
      <div style="margin-bottom: 8px; font-weight: bold; display: flex; justify-content: space-between; align-items: center;">
        <span>Color Stretch</span>
        <button id="color-stretch-close" type="button" style="cursor: pointer; font-size: 18px; line-height: 1; background: none; border: none; color: inherit" aria-label="Close color stretch panel">&times;</button>
      </div>

      <div class="info-row" id="color-stretch-layer-name" style="margin-bottom:8px; color:#4dabf7"></div>

      <div class="style-field">
        <label>Brightness: <span id="brightness-val">100</span>%</label>
        <input type="range" id="color-brightness" min="0" max="200" value="100" step="5">
      </div>
      <div class="style-field">
        <label>Contrast: <span id="contrast-val">100</span>%</label>
        <input type="range" id="color-contrast" min="0" max="300" value="100" step="5">
      </div>
      <div class="style-field">
        <label>Saturation: <span id="saturation-val">100</span>%</label>
        <input type="range" id="color-saturation" min="0" max="300" value="100" step="5">
      </div>
      <div class="style-field">
        <label>Hue Rotate: <span id="hue-val">0</span>deg</label>
        <input type="range" id="color-hue" min="0" max="360" value="0" step="15">
      </div>
      <div class="style-field">
        <label>Invert</label>
        <input type="checkbox" id="color-invert" style="width:auto">
      </div>

      <div style="margin-top:8px; display:flex; gap:5px">
        <button id="color-stretch-reset" class="crater-action-btn" style="background:#333; flex:1">Reset</button>
      </div>

      <div style="margin-top:8px">
        <label style="font-size:11px; color:#888">Presets</label>
        <div style="display:flex; gap:4px; flex-wrap:wrap; margin-top:4px">
          <button class="stretch-preset" data-preset="default">Default</button>
          <button class="stretch-preset" data-preset="enhanced">Enhanced</button>
          <button class="stretch-preset" data-preset="thermal">Thermal</button>
          <button class="stretch-preset" data-preset="night">Night</button>
          <button class="stretch-preset" data-preset="highcon">High Contrast</button>
        </div>
      </div>
    `;

    document.body.appendChild(this.container);

    // Bind controls
    const brightness = this.container.querySelector('#color-brightness');
    const contrast = this.container.querySelector('#color-contrast');
    const saturation = this.container.querySelector('#color-saturation');
    const hue = this.container.querySelector('#color-hue');
    const invert = this.container.querySelector('#color-invert');

    const applyFilter = () => {
      this.container.querySelector('#brightness-val').textContent = brightness.value;
      this.container.querySelector('#contrast-val').textContent = contrast.value;
      this.container.querySelector('#saturation-val').textContent = saturation.value;
      this.container.querySelector('#hue-val').textContent = hue.value;

      this._applyFilter({
        brightness: parseInt(brightness.value) / 100,
        contrast: parseInt(contrast.value) / 100,
        saturation: parseInt(saturation.value) / 100,
        hueRotate: parseInt(hue.value),
        invert: invert.checked
      });
    };

    brightness.addEventListener('input', applyFilter);
    contrast.addEventListener('input', applyFilter);
    saturation.addEventListener('input', applyFilter);
    hue.addEventListener('input', applyFilter);
    invert.addEventListener('change', applyFilter);

    // Reset
    this.container.querySelector('#color-stretch-reset').addEventListener('click', () => {
      brightness.value = 100;
      contrast.value = 100;
      saturation.value = 100;
      hue.value = 0;
      invert.checked = false;
      applyFilter();
    });

    // Close
    this.container.querySelector('#color-stretch-close').addEventListener('click', () => this.close());

    // Presets
    const presets = {
      default: { brightness: 100, contrast: 100, saturation: 100, hue: 0, invert: false },
      enhanced: { brightness: 110, contrast: 140, saturation: 120, hue: 0, invert: false },
      thermal: { brightness: 100, contrast: 120, saturation: 0, hue: 0, invert: true },
      night: { brightness: 60, contrast: 130, saturation: 0, hue: 0, invert: false },
      highcon: { brightness: 100, contrast: 250, saturation: 100, hue: 0, invert: false }
    };

    this.container.querySelectorAll('.stretch-preset').forEach(btn => {
      btn.addEventListener('click', () => {
        const p = presets[btn.dataset.preset];
        if (!p) return;
        brightness.value = p.brightness;
        contrast.value = p.contrast;
        saturation.value = p.saturation;
        hue.value = p.hue;
        invert.checked = p.invert;
        applyFilter();
      });
    });
  }

  /**
   * Open the color stretch control for a layer.
   * Parses the layer's current CSS filter string and syncs sliders
   * to the actual values instead of resetting to defaults.
   * @param {L.TileLayer|L.TileLayer.WMS} layer - Leaflet layer
   * @param {string} name - Layer name for display
   */
  open(layer, name) {
    this.currentLayer = layer;
    this.container.querySelector('#color-stretch-layer-name').textContent = name || 'Layer';

    // Parse current CSS filter values from the layer container and sync sliders
    const el = layer.getContainer?.();
    const parsed = this._parseFilter(el?.style?.filter || '');

    const brightness = this.container.querySelector('#color-brightness');
    const contrast = this.container.querySelector('#color-contrast');
    const saturation = this.container.querySelector('#color-saturation');
    const hue = this.container.querySelector('#color-hue');
    const invert = this.container.querySelector('#color-invert');

    brightness.value = parsed.brightness;
    contrast.value = parsed.contrast;
    saturation.value = parsed.saturation;
    hue.value = parsed.hueRotate;
    invert.checked = parsed.invert;

    // Update the displayed value labels
    this.container.querySelector('#brightness-val').textContent = parsed.brightness;
    this.container.querySelector('#contrast-val').textContent = parsed.contrast;
    this.container.querySelector('#saturation-val').textContent = parsed.saturation;
    this.container.querySelector('#hue-val').textContent = parsed.hueRotate;

    this.container.style.display = 'block';
  }

  /**
   * Close the control.
   */
  close() {
    this.container.style.display = 'none';
    this.currentLayer = null;
  }

  /**
   * Apply CSS filter to the layer's DOM container.
   * @param {object} opts - Filter options
   * @param {number} opts.brightness - Brightness multiplier (1 = 100%)
   * @param {number} opts.contrast - Contrast multiplier (1 = 100%)
   * @param {number} opts.saturation - Saturation multiplier (1 = 100%)
   * @param {number} opts.hueRotate - Hue rotation in degrees
   * @param {boolean} opts.invert - Whether to invert colors
   * @private
   */
  _applyFilter(opts) {
    if (!this.currentLayer) return;

    const el = this.currentLayer.getContainer?.();
    if (!el) return;

    const parts = [
      `brightness(${opts.brightness})`,
      `contrast(${opts.contrast})`,
      `saturate(${opts.saturation})`,
      `hue-rotate(${opts.hueRotate}deg)`
    ];
    if (opts.invert) parts.push('invert(1)');

    el.style.filter = parts.join(' ');
  }

  /**
   * Parse a CSS filter string into slider-friendly numeric values.
   * @param {string} filterStr - CSS filter property value
   * @returns {{brightness: number, contrast: number, saturation: number, hueRotate: number, invert: boolean}}
   */
  static parseFilterString(filterStr) {
    const defaults = { brightness: 100, contrast: 100, saturation: 100, hueRotate: 0, invert: false };
    if (!filterStr) return defaults;

    const extract = (name) => {
      const match = filterStr.match(new RegExp(`${name}\\(([\\d.]+)`));
      return match ? parseFloat(match[1]) : null;
    };

    const b = extract('brightness');
    const c = extract('contrast');
    const s = extract('saturate');
    const h = extract('hue-rotate');

    return {
      brightness: b !== null ? Math.round(b * 100) : defaults.brightness,
      contrast: c !== null ? Math.round(c * 100) : defaults.contrast,
      saturation: s !== null ? Math.round(s * 100) : defaults.saturation,
      hueRotate: h !== null ? Math.round(h) : defaults.hueRotate,
      invert: filterStr.includes('invert(1)')
    };
  }

  /**
   * Build CSS filter string from options.
   * @param {object} opts
   * @returns {string}
   */
  static buildFilterString(opts = {}) {
    const b = opts.brightness !== undefined ? opts.brightness : 1.0;
    const c = opts.contrast !== undefined ? opts.contrast : 1.0;
    const s = opts.saturation !== undefined ? opts.saturation : 1.0;
    const h = opts.hueRotate !== undefined ? opts.hueRotate : 0;
    const inv = !!opts.invert;

    const parts = [
      `brightness(${b})`,
      `contrast(${c})`,
      `saturate(${s})`,
      `hue-rotate(${h}deg)`
    ];
    if (inv) parts.push('invert(1)');

    return parts.join(' ');
  }

  _parseFilter(filterStr) {
    return ColorStretchControl.parseFilterString(filterStr);
  }
}
