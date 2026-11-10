/**
 * @module features/shapes/StyleEditor
 * @description Floating editor panel for modifying vector shape visual styles
 * in real time. Supports stroke and fill color, opacity, weight, dash patterns,
 * and planetary geologic mapping fill patterns (FGDC/USGS-inspired SVG patterns).
 */

const SVG_NS = 'http://www.w3.org/2000/svg';

/**
 * Authentic planetary geologic mapping fill patterns rendered via SVG <pattern>.
 * @type {Record<string, {id: string, label: string, description: string}>}
 */
export const GEOLOGIC_PATTERNS = {
  '': {
    id: '',
    label: 'Solid Fill',
    description: 'Uniform translucent color fill'
  },
  ejecta: {
    id: 'ejecta',
    label: 'Crater Ejecta Blanket',
    description: 'Radial hachures and ballistic clast stipple'
  },
  crater_material: {
    id: 'crater_material',
    label: 'Crater Floor / Impact Melt',
    description: 'Staggered impact melt vesicles and dots'
  },
  volcanic_plains: {
    id: 'volcanic_plains',
    label: 'Volcanic / Basaltic Plains',
    description: 'V-tick flood basalt / lunar mare symbol'
  },
  fracture_zone: {
    id: 'fracture_zone',
    label: 'Fracture / Graben Zone',
    description: 'Conjugate tectonic cross-hatch lattice'
  },
  layered_deposits: {
    id: 'layered_deposits',
    label: 'Layered / Polar Deposits',
    description: 'Stratified horizontal bedding and dashed interbeds'
  },
  chaos_breccia: {
    id: 'chaos_breccia',
    label: 'Chaos Terrain / Breccia',
    description: 'Angular disrupted block and clast fragments'
  }
};

/**
 * Create an SVG element in the SVG namespace with attributes.
 * @param {string} tag
 * @param {Record<string, string|number>} attrs
 * @returns {SVGElement}
 */
function svgEl(tag, attrs = {}) {
  const el = document.createElementNS(SVG_NS, tag);
  Object.entries(attrs).forEach(([k, v]) => el.setAttribute(k, String(v)));
  return el;
}

/**
 * Populate an SVG <pattern> element with geometry for the requested geologic unit.
 * @param {SVGPatternElement} pat
 * @param {string} patternId
 * @param {string} color
 */
function buildPatternNodes(pat, patternId, color) {
  while (pat.firstChild) pat.removeChild(pat.firstChild);

  // Subtle base tint so unit boundaries remain legible over dark/bright imagery
  pat.appendChild(svgEl('rect', {
    x: 0,
    y: 0,
    width: 16,
    height: 16,
    fill: color,
    'fill-opacity': '0.16'
  }));

  switch (patternId) {
    case 'ejecta': {
      pat.appendChild(svgEl('line', { x1: 1, y1: 15, x2: 7, y2: 9, stroke: color, 'stroke-width': 1.4, 'stroke-linecap': 'round' }));
      pat.appendChild(svgEl('line', { x1: 9, y1: 7, x2: 15, y2: 1, stroke: color, 'stroke-width': 1.4, 'stroke-linecap': 'round' }));
      pat.appendChild(svgEl('circle', { cx: 4, cy: 4, r: 1.3, fill: color }));
      pat.appendChild(svgEl('circle', { cx: 12.5, cy: 12, r: 1.1, fill: color }));
      break;
    }
    case 'crater_material': {
      pat.appendChild(svgEl('circle', { cx: 4.5, cy: 4.5, r: 2.2, fill: 'none', stroke: color, 'stroke-width': 1.2 }));
      pat.appendChild(svgEl('circle', { cx: 12, cy: 11.5, r: 1.9, fill: 'none', stroke: color, 'stroke-width': 1.2 }));
      pat.appendChild(svgEl('circle', { cx: 12.5, cy: 4, r: 1.0, fill: color }));
      pat.appendChild(svgEl('circle', { cx: 4, cy: 12.5, r: 1.0, fill: color }));
      break;
    }
    case 'volcanic_plains': {
      pat.appendChild(svgEl('path', { d: 'M2,4 L4.5,7.5 L7,4', fill: 'none', stroke: color, 'stroke-width': 1.3, 'stroke-linecap': 'round', 'stroke-linejoin': 'round' }));
      pat.appendChild(svgEl('path', { d: 'M10,11 L12.5,14.5 L15,11', fill: 'none', stroke: color, 'stroke-width': 1.3, 'stroke-linecap': 'round', 'stroke-linejoin': 'round' }));
      break;
    }
    case 'fracture_zone': {
      pat.appendChild(svgEl('path', { d: 'M0,0 L16,16 M16,0 L0,16', fill: 'none', stroke: color, 'stroke-width': 1.15, 'stroke-opacity': '0.9' }));
      break;
    }
    case 'layered_deposits': {
      pat.appendChild(svgEl('line', { x1: 0, y1: 4, x2: 16, y2: 4, stroke: color, 'stroke-width': 1.3 }));
      pat.appendChild(svgEl('line', { x1: 2, y1: 8, x2: 7, y2: 8, stroke: color, 'stroke-width': 1.1 }));
      pat.appendChild(svgEl('line', { x1: 10, y1: 8, x2: 15, y2: 8, stroke: color, 'stroke-width': 1.1 }));
      pat.appendChild(svgEl('line', { x1: 0, y1: 12, x2: 16, y2: 12, stroke: color, 'stroke-width': 1.3 }));
      break;
    }
    case 'chaos_breccia': {
      pat.appendChild(svgEl('polygon', { points: '2,6 6,2 7,7', fill: 'none', stroke: color, 'stroke-width': 1.2, 'stroke-linejoin': 'round' }));
      pat.appendChild(svgEl('polygon', { points: '9,13 13,9 15,14', fill: 'none', stroke: color, 'stroke-width': 1.2, 'stroke-linejoin': 'round' }));
      pat.appendChild(svgEl('circle', { cx: 12, cy: 4, r: 0.9, fill: color }));
      pat.appendChild(svgEl('circle', { cx: 4, cy: 13, r: 0.9, fill: color }));
      break;
    }
    default:
      break;
  }
}

/**
 * Ensure a Leaflet path layer preserves and renders its assigned geologic SVG fill pattern
 * across style updates, selection highlights, and map re-mounts.
 * @param {L.Path} layer
 * @param {string} [patternId='']
 * @param {string} [fillColorOverride]
 */
export function applyGeologicPatternToLayer(layer, patternId = '', fillColorOverride = undefined) {
  if (!layer || !layer.options) return;

  const normalizedPattern = patternId && GEOLOGIC_PATTERNS[patternId] ? patternId : '';
  layer.options.pattern = normalizedPattern;
  if (fillColorOverride) {
    layer.options.fillColor = fillColorOverride;
  }

  // Wrap setStyle once so future setStyle calls (selection weight, color changes) preserve the pattern
  if (typeof layer.setStyle === 'function' && !layer._jsmarsOrigSetStyle) {
    layer._jsmarsOrigSetStyle = layer.setStyle.bind(layer);
    layer.setStyle = (style = {}) => {
      if (Object.prototype.hasOwnProperty.call(style, 'pattern')) {
        layer.options.pattern = style.pattern && GEOLOGIC_PATTERNS[style.pattern] ? style.pattern : '';
      }
      const res = layer._jsmarsOrigSetStyle(style);
      if (layer.options.pattern) {
        renderPatternOnPath(layer);
      } else if (Object.prototype.hasOwnProperty.call(style, 'pattern') && layer._path) {
        const solid = layer.options.fillColor || layer.options.color || '#4dabf7';
        layer._path.setAttribute('fill', solid);
      }
      return res;
    };
  }

  if (!layer._jsmarsPatternAddHooked && typeof layer.on === 'function') {
    layer._jsmarsPatternAddHooked = true;
    layer.on('add', () => {
      if (layer.options?.pattern) {
        renderPatternOnPath(layer);
      }
    });
  }

  if (normalizedPattern) {
    renderPatternOnPath(layer);
  } else if (layer._path) {
    const solid = layer.options.fillColor || layer.options.color || '#4dabf7';
    layer._path.setAttribute('fill', solid);
  }
}

/**
 * Inject/update the <pattern> definition in the layer's owning <svg> and bind fill="url(#...)".
 * @param {L.Path} layer
 */
function renderPatternOnPath(layer) {
  const pathEl = layer?._path;
  if (!pathEl || typeof document === 'undefined') return;
  const svgRoot = pathEl.ownerSVGElement;
  if (!svgRoot) return;

  const patternId = layer.options?.pattern;
  if (!patternId || !GEOLOGIC_PATTERNS[patternId]) return;

  const color = layer.options.fillColor || layer.options.color || '#4dabf7';
  const safeHex = String(color).replace(/[^a-zA-Z0-9]/g, '').toLowerCase() || '4dabf7';
  const domPatternId = `jsmars-geopat-${patternId}-${safeHex}`;

  let defs = svgRoot.querySelector('defs.jsmars-geologic-defs');
  if (!defs) {
    defs = svgEl('defs', { class: 'jsmars-geologic-defs' });
    svgRoot.insertBefore(defs, svgRoot.firstChild);
  }

  let pat = defs.querySelector(`#${domPatternId}`);
  if (!pat) {
    pat = svgEl('pattern', {
      id: domPatternId,
      patternUnits: 'userSpaceOnUse',
      width: 16,
      height: 16
    });
    buildPatternNodes(pat, patternId, color);
    defs.appendChild(pat);
  }

  pathEl.setAttribute('fill', `url(#${domPatternId})`);
}

/**
 * @class StyleEditor
 * @description Provides a floating panel for editing vector shape styles,
 * including dash patterns, opacities, color presets, and geologic unit fills.
 */
export class StyleEditor {
  /**
   * Creates the StyleEditor and appends it to the document body.
   * @param {L.Map} map - The Leaflet map instance associated with the shapes.
   * @param {object} [options]
   * @param {Function} [options.onStyleChange] - Optional callback `(layer, styles) => void`
   */
  constructor(map, options = {}) {
    this.map = map;
    this.onStyleChange = options.onStyleChange || null;
    this.container = null;
    this.currentLayer = null;
    this.init();
  }

  init() {
    if (typeof document === 'undefined') return;

    this.container = document.createElement('div');
    this.container.id = 'style-editor';

    const patternOptionsHtml = Object.values(GEOLOGIC_PATTERNS)
      .map(p => `<option value="${p.id}">${p.label}</option>`)
      .join('');

    this.container.innerHTML = `
      <div style="margin-bottom: 8px; font-weight: bold; display: flex; justify-content: space-between; align-items: center;">
        <span>Edit Shape &amp; Geologic Style</span>
        <button type="button" id="style-editor-close" aria-label="Close style editor" style="background:transparent;border:none;color:#e2e8f0;cursor:pointer;font-size:18px;line-height:1;padding:2px 4px;">&times;</button>
      </div>

      <div class="style-presets">
        <button type="button" class="style-preset-btn" data-preset="#ff6b6b" style="background:#ff6b6b" title="Red"></button>
        <button type="button" class="style-preset-btn" data-preset="#4dabf7" style="background:#4dabf7" title="Blue"></button>
        <button type="button" class="style-preset-btn" data-preset="#51cf66" style="background:#51cf66" title="Green"></button>
        <button type="button" class="style-preset-btn" data-preset="#ffd43b" style="background:#ffd43b" title="Yellow"></button>
        <button type="button" class="style-preset-btn" data-preset="#da77f2" style="background:#da77f2" title="Purple"></button>
        <button type="button" class="style-preset-btn" data-preset="#ff922b" style="background:#ff922b" title="Orange"></button>
        <button type="button" class="style-preset-btn" data-preset="#ffffff" style="background:#ffffff" title="White"></button>
      </div>

      <div class="style-field">
        <label for="style-stroke-color">Stroke Color</label>
        <input type="color" id="style-stroke-color">
      </div>
      <div class="style-field">
        <label for="style-fill-color">Fill Color</label>
        <input type="color" id="style-fill-color">
      </div>
      <div class="style-field">
        <label for="style-fill-pattern">Geologic Fill Pattern</label>
        <select id="style-fill-pattern" class="stamp-select" style="width:100%">
          ${patternOptionsHtml}
        </select>
      </div>
      <div class="style-field">
        <label for="style-weight">Stroke Width: <span id="style-weight-val">2</span></label>
        <input type="range" id="style-weight" min="1" max="10" step="1">
      </div>
      <div class="style-field">
        <label for="style-stroke-opacity">Stroke Opacity: <span id="style-stroke-opacity-val">0.9</span></label>
        <input type="range" id="style-stroke-opacity" min="0" max="1" step="0.1">
      </div>
      <div class="style-field">
        <label for="style-fill-opacity">Fill Opacity: <span id="style-fill-opacity-val">0.2</span></label>
        <input type="range" id="style-fill-opacity" min="0" max="1" step="0.05">
      </div>
      <div class="style-field">
        <label for="style-dash-pattern">Dash Pattern</label>
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

    // Prevent clicks inside the floating editor from bubbling to map
    this.container.addEventListener('click', (e) => e.stopPropagation());

    this.container.querySelector('#style-editor-close').addEventListener('click', () => this.close());

    const strokeColor = this.container.querySelector('#style-stroke-color');
    const fillColor = this.container.querySelector('#style-fill-color');
    const fillPattern = this.container.querySelector('#style-fill-pattern');
    const weight = this.container.querySelector('#style-weight');
    const strokeOpacity = this.container.querySelector('#style-stroke-opacity');
    const fillOpacity = this.container.querySelector('#style-fill-opacity');
    const dashPattern = this.container.querySelector('#style-dash-pattern');

    strokeColor.addEventListener('input', (e) => this.updateStyle({ color: e.target.value }));
    fillColor.addEventListener('input', (e) => this.updateStyle({ fillColor: e.target.value }));
    fillPattern.addEventListener('change', (e) => {
      const pat = e.target.value || '';
      const updates = { pattern: pat };
      // Ensure pattern hatch is clearly visible if opacity was still at default 0.2
      const currentFillOp = parseFloat(fillOpacity.value);
      if (pat && currentFillOp <= 0.25) {
        updates.fillOpacity = 0.65;
        fillOpacity.value = '0.65';
        this.container.querySelector('#style-fill-opacity-val').textContent = '0.65';
      }
      this.updateStyle(updates);
    });
    weight.addEventListener('input', (e) => {
      this.container.querySelector('#style-weight-val').textContent = e.target.value;
      this.updateStyle({ weight: parseInt(e.target.value, 10) });
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
        const hex = btn.dataset.preset || this._rgbToHex(btn.style.background);
        this.updateStyle({ color: hex, fillColor: hex });
        strokeColor.value = hex;
        fillColor.value = hex;
      });
    });
  }

  /**
   * Open the editor for a specific layer at a screen position.
   * @param {L.Layer} layer
   * @param {L.Point|{x: number, y: number}} point
   */
  open(layer, point = { x: 80, y: 80 }) {
    if (!this.container || !layer) return;
    this.currentLayer = layer;

    const options = layer.options || {};
    this.container.querySelector('#style-stroke-color').value = this._toHex(options.color || '#4dabf7');
    this.container.querySelector('#style-fill-color').value = this._toHex(options.fillColor || options.color || '#4dabf7');
    this.container.querySelector('#style-fill-pattern').value = options.pattern || '';
    this.container.querySelector('#style-weight').value = options.weight || 2;
    this.container.querySelector('#style-weight-val').textContent = options.weight || 2;
    this.container.querySelector('#style-stroke-opacity').value = options.opacity !== undefined ? options.opacity : 0.9;
    this.container.querySelector('#style-stroke-opacity-val').textContent = options.opacity !== undefined ? options.opacity : 0.9;
    this.container.querySelector('#style-fill-opacity').value = options.fillOpacity !== undefined ? options.fillOpacity : 0.2;
    this.container.querySelector('#style-fill-opacity-val').textContent = options.fillOpacity !== undefined ? options.fillOpacity : 0.2;

    const dashSelect = this.container.querySelector('#style-dash-pattern');
    dashSelect.value = options.dashArray || '';

    const editorWidth = 230;
    const editorHeight = 420;
    const px = point?.x ?? 80;
    const py = point?.y ?? 80;
    let left = px + 10;
    let top = py + 10;

    if (typeof window !== 'undefined') {
      if (left + editorWidth > window.innerWidth) left = px - editorWidth - 10;
      if (top + editorHeight > window.innerHeight) top = py - editorHeight - 10;
    }

    this.container.style.display = 'block';
    this.container.style.left = Math.max(10, left) + 'px';
    this.container.style.top = Math.max(44, top) + 'px';
  }

  /**
   * Close the editor.
   */
  close() {
    if (this.container) {
      this.container.style.display = 'none';
    }
    this.currentLayer = null;
  }

  /**
   * Apply style updates to the current layer and sync any geologic fill pattern.
   * @param {object} styles
   */
  updateStyle(styles) {
    if (!this.currentLayer || typeof this.currentLayer.setStyle !== 'function') return;

    this.currentLayer.setStyle(styles);
    const activePattern = Object.prototype.hasOwnProperty.call(styles, 'pattern')
      ? styles.pattern
      : (this.currentLayer.options?.pattern || '');
    applyGeologicPatternToLayer(this.currentLayer, activePattern, styles.fillColor);

    if (typeof this.onStyleChange === 'function') {
      this.onStyleChange(this.currentLayer, styles);
    }
  }

  /**
   * Attempt to convert a color string to hex.
   * @param {string} color
   * @returns {string}
   */
  _toHex(color) {
    if (typeof color !== 'string') return '#4dabf7';
    if (color.startsWith('#')) {
      if (color.length === 4) {
        return '#' + color[1] + color[1] + color[2] + color[2] + color[3] + color[3];
      }
      return color.slice(0, 7);
    }
    return this._rgbToHex(color);
  }

  /**
   * Convert rgb(r,g,b) string to hex.
   * @param {string} rgb
   * @returns {string}
   */
  _rgbToHex(rgb) {
    if (typeof rgb !== 'string') return '#4dabf7';
    if (rgb.startsWith('#')) return this._toHex(rgb);

    const match = rgb.match(/(\d+)/g);
    if (!match || match.length < 3) return '#4dabf7';

    const r = parseInt(match[0], 10).toString(16).padStart(2, '0');
    const g = parseInt(match[1], 10).toString(16).padStart(2, '0');
    const b = parseInt(match[2], 10).toString(16).padStart(2, '0');
    return `#${r}${g}${b}`;
  }
}
