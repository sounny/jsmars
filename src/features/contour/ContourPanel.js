/**
 * @module features/contour/ContourPanel
 * @description UI panel for controlling elevation contour lines derived from
 * the MOLA digital elevation model. Exposes controls for contour interval,
 * line color, line width, and layer opacity, along with a toggle button to
 * enable or disable contour rendering.
 */

/**
 * @class ContourPanel
 * @description Provides UI controls for the contour layer.
 * Controls step interval, line color, and opacity.
 */
export class ContourPanel {
  /**
   * @param {HTMLElement} container
   * @param {import('./ContourLayer.js').ContourLayer} contourLayer
   */
  constructor(container, contourLayer) {
    this.container = container;
    this.layer = contourLayer;
    this._build();
  }

  _build() {
    this.container.innerHTML = `
      <div style="padding:10px">
        <div style="margin-bottom:8px; font-size:12px; color:#aaa">
          Elevation contour lines from MOLA DEM.
        </div>

        <button id="contour-toggle-btn" class="tool-btn">Enable Contours</button>

        <div id="contour-controls" style="margin-top:10px; display:none">
          <div class="style-field">
            <label>Interval: <span id="ct-step-val">1000</span> m</label>
            <input type="range" id="ct-step" min="100" max="5000" value="1000" step="100">
          </div>
          <div class="style-field">
            <label>Line Color</label>
            <input type="color" id="ct-color" value="#4dabf7" style="width:50px; height:24px; padding:0; border:none; cursor:pointer">
          </div>
          <div class="style-field">
            <label>Opacity: <span id="ct-opacity-val">70</span>%</label>
            <input type="range" id="ct-opacity" min="10" max="100" value="70" step="5">
          </div>
          <div class="style-field">
            <label>Line Width</label>
            <select id="ct-width" class="stamp-select" style="width:80px">
              <option value="0.5">Thin</option>
              <option value="1" selected>Normal</option>
              <option value="1.5">Thick</option>
              <option value="2">Extra</option>
            </select>
          </div>
        </div>
      </div>
    `;

    const toggleBtn = this.container.querySelector('#contour-toggle-btn');
    const controls = this.container.querySelector('#contour-controls');

    toggleBtn.addEventListener('click', () => {
      const active = this.layer.toggle();
      toggleBtn.textContent = active ? 'Disable Contours' : 'Enable Contours';
      toggleBtn.classList.toggle('active', active);
      controls.style.display = active ? 'block' : 'none';
    });

    const step = this.container.querySelector('#ct-step');
    const color = this.container.querySelector('#ct-color');
    const opacity = this.container.querySelector('#ct-opacity');
    const width = this.container.querySelector('#ct-width');

    step.addEventListener('input', (e) => {
      this.container.querySelector('#ct-step-val').textContent = e.target.value;
      this.layer.setParams({ step: parseInt(e.target.value) });
    });

    color.addEventListener('input', (e) => {
      this.layer.setParams({ lineColor: e.target.value });
    });

    opacity.addEventListener('input', (e) => {
      this.container.querySelector('#ct-opacity-val').textContent = e.target.value;
      this.layer.setParams({ opacity: parseInt(e.target.value) / 100 });
    });

    width.addEventListener('change', (e) => {
      this.layer.setParams({ lineWidth: parseFloat(e.target.value) });
    });
  }
}
