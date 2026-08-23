import { ThreeDViewer } from './ThreeDViewer.js';

/**
 * @module ThreeDPanel
 * @description UI control panel for the 3D Terrain & Globe visualization tool.
 */
export class ThreeDPanel {
  /**
   * @param {HTMLElement|string} container - Container DOM element or ID
   * @param {L.Map} map - Leaflet map instance
   */
  constructor(container, map) {
    this.container = typeof container === 'string' ? document.getElementById(container) : container;
    this.map = map;
    this.viewer = null;

    if (this.container) {
      this.init();
    }
  }

  init() {
    this.container.innerHTML = `
      <div style="padding: 10px; font-size: 12px; color: #e2e8f0;">
        <div style="display: flex; gap: 4px; margin-bottom: 8px;">
          <button id="threed-mode-terrain" class="tool-btn" style="flex: 1; font-size: 11px; background: #0284c7;">3D Terrain</button>
          <button id="threed-mode-globe" class="tool-btn" style="flex: 1; font-size: 11px; background: #334155;">3D Globe</button>
        </div>

        <div id="threed-viewer-container" style="margin-bottom: 8px;"></div>

        <div style="margin-bottom: 6px;">
          <div style="display: flex; justify-content: space-between; font-size: 10px; color: #94a3b8; margin-bottom: 2px;">
            <span>Vertical Exaggeration</span>
            <b id="threed-exag-val" style="color: #f97316;">5x</b>
          </div>
          <input type="range" id="threed-exag-slider" min="1" max="20" step="0.5" value="5" style="width: 100%; accent-color: #f97316; cursor: pointer;">
        </div>

        <div style="margin-bottom: 8px;">
          <div style="display: flex; justify-content: space-between; font-size: 10px; color: #94a3b8; margin-bottom: 2px;">
            <span>Sun Hour Angle</span>
            <b id="threed-sun-val" style="color: #facc15;">12:00</b>
          </div>
          <input type="range" id="threed-sun-slider" min="0" max="24" step="0.5" value="12" style="width: 100%; accent-color: #facc15; cursor: pointer;">
        </div>

        <div style="display: flex; justify-content: space-between; align-items: center;">
          <label style="font-size: 10px; color: #94a3b8; display: flex; align-items: center; gap: 4px; cursor: pointer;">
            <input type="checkbox" id="threed-wireframe-chk" style="accent-color: #f97316;"> Wireframe
          </label>
          <button id="threed-reset-btn" class="tool-btn" style="padding: 2px 8px; font-size: 10px;">Reset View</button>
        </div>
      </div>
    `;

    const viewerContainer = this.container.querySelector('#threed-viewer-container');
    this.viewer = new ThreeDViewer(viewerContainer, this.map);

    this.bindEvents();
  }

  bindEvents() {
    const terrainBtn = this.container.querySelector('#threed-mode-terrain');
    const globeBtn = this.container.querySelector('#threed-mode-globe');
    const exagSlider = this.container.querySelector('#threed-exag-slider');
    const exagVal = this.container.querySelector('#threed-exag-val');
    const sunSlider = this.container.querySelector('#threed-sun-slider');
    const sunVal = this.container.querySelector('#threed-sun-val');
    const wireframeChk = this.container.querySelector('#threed-wireframe-chk');
    const resetBtn = this.container.querySelector('#threed-reset-btn');

    terrainBtn.addEventListener('click', () => {
      terrainBtn.style.background = '#0284c7';
      globeBtn.style.background = '#334155';
      this.viewer.setMode('terrain');
    });

    globeBtn.addEventListener('click', () => {
      globeBtn.style.background = '#0284c7';
      terrainBtn.style.background = '#334155';
      this.viewer.setMode('globe');
    });

    exagSlider.addEventListener('input', (e) => {
      const v = parseFloat(e.target.value);
      exagVal.innerText = `${v}x`;
      this.viewer.setExaggeration(v);
    });

    sunSlider.addEventListener('input', (e) => {
      const h = parseFloat(e.target.value);
      const hours = Math.floor(h);
      const mins = Math.floor((h - hours) * 60);
      sunVal.innerText = `${String(hours).padStart(2, '0')}:${String(mins).padStart(2, '0')}`;
      this.viewer.updateSunAngle(0, h);
    });

    wireframeChk.addEventListener('change', (e) => {
      this.viewer.setWireframe(e.target.checked);
    });

    resetBtn.addEventListener('click', () => {
      this.viewer.rotation = { x: 0.6, y: 0.4 };
      this.viewer.zoom = 1.0;
    });
  }
}
