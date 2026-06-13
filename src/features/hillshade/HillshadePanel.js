/**
 * @module features/hillshade/HillshadePanel
 * @description UI panel for controlling real-time hillshade (relief shading)
 * derived from the MOLA digital elevation model. Exposes sliders for sun
 * azimuth, altitude, vertical exaggeration, and layer opacity, plus a toggle
 * button to enable or disable the effect.
 */

/**
 * @class HillshadePanel
 * @description Provides UI controls for the hillshade layer.
 * Controls azimuth, altitude, vertical exaggeration, and opacity.
 */
export class HillshadePanel {
  /**
   * @param {HTMLElement} container
   * @param {import('./HillshadeLayer.js').HillshadeLayer} hillshadeLayer
   */
  constructor(container, hillshadeLayer) {
    this.container = container;
    this.layer = hillshadeLayer;
    this._build();
  }

  _build() {
    this.container.innerHTML = `
      <div style="padding:10px">
        <div style="margin-bottom:8px; font-size:12px; color:#aaa">
          Real-time relief shading from MOLA DEM.
        </div>

        <button id="hillshade-toggle-btn" class="tool-btn">Enable Hillshade</button>

        <div id="hillshade-controls" style="margin-top:10px; display:none">
          <div class="style-field">
            <label>Azimuth: <span id="hs-azimuth-val">315</span>&deg;</label>
            <input type="range" id="hs-azimuth" min="0" max="360" value="315" step="15">
          </div>
          <div class="style-field">
            <label>Altitude: <span id="hs-altitude-val">45</span>&deg;</label>
            <input type="range" id="hs-altitude" min="5" max="90" value="45" step="5">
          </div>
          <div class="style-field">
            <label>Vertical Exaggeration: <span id="hs-zfactor-val">1.5</span>x</label>
            <input type="range" id="hs-zfactor" min="0.5" max="5" value="1.5" step="0.5">
          </div>
          <div class="style-field">
            <label>Opacity: <span id="hs-opacity-val">50</span>%</label>
            <input type="range" id="hs-opacity" min="10" max="100" value="50" step="5">
          </div>
        </div>
      </div>
    `;

    const toggleBtn = this.container.querySelector('#hillshade-toggle-btn');
    const controls = this.container.querySelector('#hillshade-controls');

    toggleBtn.addEventListener('click', () => {
      const active = this.layer.toggle();
      toggleBtn.textContent = active ? 'Disable Hillshade' : 'Enable Hillshade';
      toggleBtn.classList.toggle('active', active);
      controls.style.display = active ? 'block' : 'none';
    });

    // Sliders
    const azimuth = this.container.querySelector('#hs-azimuth');
    const altitude = this.container.querySelector('#hs-altitude');
    const zfactor = this.container.querySelector('#hs-zfactor');
    const opacity = this.container.querySelector('#hs-opacity');

    azimuth.addEventListener('input', (e) => {
      this.container.querySelector('#hs-azimuth-val').textContent = e.target.value;
      this.layer.setParams({ azimuth: parseInt(e.target.value) });
    });

    altitude.addEventListener('input', (e) => {
      this.container.querySelector('#hs-altitude-val').textContent = e.target.value;
      this.layer.setParams({ altitude: parseInt(e.target.value) });
    });

    zfactor.addEventListener('input', (e) => {
      this.container.querySelector('#hs-zfactor-val').textContent = e.target.value;
      this.layer.setParams({ zFactor: parseFloat(e.target.value) });
    });

    opacity.addEventListener('input', (e) => {
      this.container.querySelector('#hs-opacity-val').textContent = e.target.value;
      this.layer.setParams({ opacity: parseInt(e.target.value) / 100 });
    });
  }
}
