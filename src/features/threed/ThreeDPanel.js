import { ThreeDViewer } from './ThreeDViewer.js';

/**
 * @module ThreeDPanel
 * @description UI control panel for the advanced 3D Terrain & Globe visualization tool.
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
        <div style="display: flex; gap: 6px; margin-bottom: 8px;">
          <button id="threed-mode-globe" class="tool-btn" style="flex: 1; font-size: 11px; font-weight: 600; background: #0284c7; border: 1px solid #38bdf8;">🌍 3D Globe</button>
          <button id="threed-mode-terrain" class="tool-btn" style="flex: 1; font-size: 11px; background: #334155; border: 1px solid #475569;">⛰️ 3D Terrain</button>
        </div>

        <div id="threed-viewer-container" style="margin-bottom: 8px;"></div>

        <!-- Globe Quick Action Toolbar -->
        <div style="display: flex; gap: 4px; margin-bottom: 8px;">
          <button id="threed-spin-btn" class="tool-btn" style="flex: 1; font-size: 10px; padding: 4px; background: #1e293b;" title="Toggle celestial planetary rotation">⏸ Pause Spin</button>
          <button id="threed-focus-btn" class="tool-btn" style="flex: 1; font-size: 10px; padding: 4px; background: #1e293b;" title="Rotate globe to center on current 2D map view">📍 Focus Map</button>
          <button id="threed-reset-btn" class="tool-btn" style="flex: 0.8; font-size: 10px; padding: 4px; background: #1e293b;" title="Reset view rotation and zoom">↺ Reset</button>
        </div>

        <!-- Options Toggles -->
        <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; flex-wrap: wrap; gap: 6px; background: rgba(30,41,59,0.5); padding: 5px 8px; border-radius: 4px; border: 1px solid #334155;">
          <label style="font-size: 10px; color: #cbd5e1; display: flex; align-items: center; gap: 4px; cursor: pointer;">
            <input type="checkbox" id="threed-atmos-chk" checked style="accent-color: #38bdf8;"> ✨ Atmosphere
          </label>
          <label style="font-size: 10px; color: #cbd5e1; display: flex; align-items: center; gap: 4px; cursor: pointer;">
            <input type="checkbox" id="threed-graticule-chk" checked style="accent-color: #fbbf24;"> 🌐 Graticule
          </label>
          <label style="font-size: 10px; color: #cbd5e1; display: flex; align-items: center; gap: 4px; cursor: pointer;">
            <input type="checkbox" id="threed-wireframe-chk" style="accent-color: #f97316;"> 🕸️ Wireframe
          </label>
        </div>

        <!-- Sliders -->
        <div id="threed-exag-container" style="margin-bottom: 6px; display: none;">
          <div style="display: flex; justify-content: space-between; font-size: 10px; color: #94a3b8; margin-bottom: 2px;">
            <span>Terrain Exaggeration</span>
            <b id="threed-exag-val" style="color: #f97316;">5x</b>
          </div>
          <input type="range" id="threed-exag-slider" min="1" max="20" step="0.5" value="5" style="width: 100%; accent-color: #f97316; cursor: pointer;">
        </div>

        <div style="margin-bottom: 6px;">
          <div style="display: flex; justify-content: space-between; font-size: 10px; color: #94a3b8; margin-bottom: 2px;">
            <span>☀️ Solar Hour Angle</span>
            <b id="threed-sun-val" style="color: #facc15;">12:00</b>
          </div>
          <input type="range" id="threed-sun-slider" min="0" max="24" step="0.5" value="12" style="width: 100%; accent-color: #facc15; cursor: pointer;">
        </div>

        <div style="font-size: 9px; color: #64748b; text-align: center; margin-top: 4px;">
          💡 Tip: Drag to rotate &bull; Scroll to zoom &bull; Double-click globe to fly 2D map
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
    const spinBtn = this.container.querySelector('#threed-spin-btn');
    const focusBtn = this.container.querySelector('#threed-focus-btn');
    const resetBtn = this.container.querySelector('#threed-reset-btn');

    const atmosChk = this.container.querySelector('#threed-atmos-chk');
    const graticuleChk = this.container.querySelector('#threed-graticule-chk');
    const wireframeChk = this.container.querySelector('#threed-wireframe-chk');

    const exagContainer = this.container.querySelector('#threed-exag-container');
    const exagSlider = this.container.querySelector('#threed-exag-slider');
    const exagVal = this.container.querySelector('#threed-exag-val');

    const sunSlider = this.container.querySelector('#threed-sun-slider');
    const sunVal = this.container.querySelector('#threed-sun-val');

    globeBtn.addEventListener('click', () => {
      globeBtn.style.background = '#0284c7';
      globeBtn.style.borderColor = '#38bdf8';
      terrainBtn.style.background = '#334155';
      terrainBtn.style.borderColor = '#475569';
      exagContainer.style.display = 'none';
      this.viewer.setMode('globe');
    });

    terrainBtn.addEventListener('click', () => {
      terrainBtn.style.background = '#0284c7';
      terrainBtn.style.borderColor = '#38bdf8';
      globeBtn.style.background = '#334155';
      globeBtn.style.borderColor = '#475569';
      exagContainer.style.display = 'block';
      this.viewer.setMode('terrain');
    });

    spinBtn.addEventListener('click', () => {
      this.viewer.autoSpin = !this.viewer.autoSpin;
      spinBtn.innerText = this.viewer.autoSpin ? '⏸ Pause Spin' : '▶ Auto-Spin';
      spinBtn.style.color = this.viewer.autoSpin ? '#38bdf8' : '#e2e8f0';
    });

    focusBtn.addEventListener('click', () => {
      this.viewer.flyToMapCenter();
      spinBtn.innerText = '▶ Auto-Spin';
      spinBtn.style.color = '#e2e8f0';
    });

    resetBtn.addEventListener('click', () => {
      this.viewer.rotation = { x: 0.35, y: 0.2 };
      this.viewer.targetRotation = { x: 0.35, y: 0.2 };
      this.viewer.zoom = 1.0;
    });

    atmosChk.addEventListener('change', (e) => {
      this.viewer.setAtmosphere(e.target.checked);
    });

    graticuleChk.addEventListener('change', (e) => {
      this.viewer.setGraticule(e.target.checked);
    });

    wireframeChk.addEventListener('change', (e) => {
      this.viewer.setWireframe(e.target.checked);
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
  }
}

