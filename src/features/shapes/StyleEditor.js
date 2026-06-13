/**
 * StyleEditor provides a floating panel for editing vector shape styles.
 * Enhanced with dash patterns, fill opacity, stroke opacity, and presets.
 */
export class StyleEditor {
  constructor(map) {
    this.map = map;
    this.container = null;
    this.currentLayer = null;
    this.init();
  }

  init() {
    this.container = document.createElement('div');
    this.container.id = 'style-editor';

    this.container.innerHTML = `
      <div style="margin-bottom: 8px; font-weight: bold; display: flex; justify-content: space-between; align-items: center;">
        <span>Edit Style</span>
        <span id="style-editor-close" style="cursor: pointer; font-size: 18px; line-height: 1">&times;</span>
      </div>

      <div class="style-presets">
        <button class="style-preset-btn" data-preset="red" style="background:#ff6b6b" title="Red"></button>
        <button class="style-preset-btn" data-preset="blue" style="background:#4dabf7" title="Blue"></button>
        <button class="style-preset-btn" data-preset="green" style="background:#51cf66" title="Green"></button>
        <button class="style-preset-btn" data-preset="yellow" style="background:#ffd43b" title="Yellow"></button>
        <button class="style-preset-btn" data-preset="purple" style="background:#da77f2" title="Purple"></button>
        <button class="style-preset-btn" data-preset="orange" style="background:#ff922b" title="Orange"></button>
        <button class="style-preset-btn" data-preset="white" style="background:#fff" title="White"></button>
      </div>

      <div class="style-field">
        <label>Stroke Color</label>
        <input type="color" id="style-stroke-color">
      </div>
      <div class="style-field">
        <label>Fill Color</label>
        <input type="color" id="style-fill-color">
      </div>
      <div class="style-field">
        <label>Stroke Width: <span id="style-weight-val">2</span></label>
        <input type="range" id="style-weight" min="1" max="10" step="1">
      </div>
      <div class="style-field">
        <label>Stroke Opacity: <span id="style-stroke-opacity-val">0.9</span></label>
        <input type="range" id="style-stroke-opacity" min="0" max="1" step="0.1">
      </div>
      <div class="style-field">
        <label>Fill Opacity: <span id="style-fill-opacity-val">0.2</span></label>
        <input type="range" id="style-fill-opacity" min="0" max="1" step="0.05">
      </div>
      <div class="style-field">
        <label>Dash Pattern</label>
        <select id="style-dash-pattern" class="stamp-select" style="width:100%">
          <option value="">Solid</option>
          <option value="5,5">Dashed</option>
          <option value="2,4">Dotted</option>
          <option value="10,5,2,5">Dash-Dot</option>
          <option value="15,5,5,5">Long Dash</option>
        </select>
      </div>
    `;

    document.body.appendChild(this.container);

    // Bind events
    this.container.querySelector('#style-editor-close').addEventListener('click', () => this.close());

    const strokeColor = this.container.querySelector('#style-stroke-color');
    const fillColor = this.container.querySelector('#style-fill-color');
    const weight = this.container.querySelector('#style-weight');
    const strokeOpacity = this.container.querySelector('#style-stroke-opacity');
    const fillOpacity = this.container.querySelector('#style-fill-opacity');
    const dashPattern = this.container.querySelector('#style-dash-pattern');

    strokeColor.addEventListener('input', (e) => this.updateStyle({ color: e.target.value }));
    fillColor.addEventListener('input', (e) => this.updateStyle({ fillColor: e.target.value }));
    weight.addEventListener('input', (e) => {
      this.container.querySelector('#style-weight-val').textContent = e.target.value;
      this.updateStyle({ weight: parseInt(e.target.value) });
    });
    strokeOpacity.addEventListener('input', (e) => {
      this.container.querySelector('#style-stroke-opacity-val').textContent = e.target.value;
      this.updateStyle({ opacity: parseFloat(e.target.value) });
    });
    fillOpacity.addEventListener('input', (e) => {
      this.container.querySelector('#style-fill-opacity-val').textContent = e.target.value;
      this.updateStyle({ fillOpacity: parseFloat(e.target.value) });
    });
    dashPattern.addEventListener('change', (e) => {
      const val = e.target.value;
      this.updateStyle({ dashArray: val || null });
    });

    // Preset buttons
    this.container.querySelectorAll('.style-preset-btn').forEach(btn => {
      btn.addEventListener('click', () => {
        const color = btn.style.background;
        this.updateStyle({ color, fillColor: color });
        strokeColor.value = this._rgbToHex(color);
        fillColor.value = this._rgbToHex(color);
      });
    });
  }

  /**
   * Open the editor for a specific layer at a screen position.
   * @param {L.Layer} layer
   * @param {L.Point} point
   */
  open(layer, point) {
    this.currentLayer = layer;

    // Load current styles
    const options = layer.options;
    this.container.querySelector('#style-stroke-color').value = this._toHex(options.color || '#3388ff');
    this.container.querySelector('#style-fill-color').value = this._toHex(options.fillColor || options.color || '#3388ff');
    this.container.querySelector('#style-weight').value = options.weight || 3;
    this.container.querySelector('#style-weight-val').textContent = options.weight || 3;
    this.container.querySelector('#style-stroke-opacity').value = options.opacity !== undefined ? options.opacity : 0.9;
    this.container.querySelector('#style-stroke-opacity-val').textContent = options.opacity !== undefined ? options.opacity : 0.9;
    this.container.querySelector('#style-fill-opacity').value = options.fillOpacity !== undefined ? options.fillOpacity : 0.2;
    this.container.querySelector('#style-fill-opacity-val').textContent = options.fillOpacity !== undefined ? options.fillOpacity : 0.2;

    const dashSelect = this.container.querySelector('#style-dash-pattern');
    dashSelect.value = options.dashArray || '';

    // Position near the click but keep on screen
    const editorWidth = 220;
    const editorHeight = 380;
    let left = point.x + 10;
    let top = point.y + 10;

    if (left + editorWidth > window.innerWidth) left = point.x - editorWidth - 10;
    if (top + editorHeight > window.innerHeight) top = point.y - editorHeight - 10;

    this.container.style.display = 'block';
    this.container.style.left = Math.max(10, left) + 'px';
    this.container.style.top = Math.max(10, top) + 'px';
  }

  /**
   * Close the editor.
   */
  close() {
    this.container.style.display = 'none';
    this.currentLayer = null;
  }

  /**
   * Apply style updates to the current layer.
   * @param {object} styles
   */
  updateStyle(styles) {
    if (this.currentLayer && typeof this.currentLayer.setStyle === 'function') {
      this.currentLayer.setStyle(styles);
    }
  }

  /**
   * Attempt to convert a color string to hex.
   * @param {string} color
   * @returns {string}
   */
  _toHex(color) {
    if (color.startsWith('#')) {
      // Ensure 6-digit hex
      if (color.length === 4) {
        return '#' + color[1] + color[1] + color[2] + color[2] + color[3] + color[3];
      }
      return color;
    }
    return this._rgbToHex(color);
  }

  /**
   * Convert rgb(r,g,b) string to hex.
   * @param {string} rgb
   * @returns {string}
   */
  _rgbToHex(rgb) {
    if (typeof rgb !== 'string') return '#3388ff';
    if (rgb.startsWith('#')) return rgb;

    const match = rgb.match(/(\d+)/g);
    if (!match || match.length < 3) return '#3388ff';

    const r = parseInt(match[0]).toString(16).padStart(2, '0');
    const g = parseInt(match[1]).toString(16).padStart(2, '0');
    const b = parseInt(match[2]).toString(16).padStart(2, '0');
    return `#${r}${g}${b}`;
  }
}
