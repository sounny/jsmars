/**
 * ColorStretchControl provides a UI for adjusting WMS layer color rendering.
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
        <span id="color-stretch-close" style="cursor: pointer; font-size: 18px; line-height: 1">&times;</span>
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
   * @param {L.TileLayer|L.TileLayer.WMS} layer - Leaflet layer
   * @param {string} name - Layer name for display
   */
  open(layer, name) {
    this.currentLayer = layer;
    this.container.querySelector('#color-stretch-layer-name').textContent = name || 'Layer';

    // Read current filter values from the layer container
    const el = layer.getContainer?.();
    if (el) {
      // Could parse current filter string, but for simplicity just reset
    }

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
   * @param {object} opts
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
}
