/**
 * @module GridPanel
 * @description UI controls for configuring planetary Lat/Lon graticule lines.
 */
export class GridPanel {
  /**
   * @param {HTMLElement|string} containerOrId - Container element or ID
   * @param {import('./GridLayer.js').GridLayer} gridLayer - GridLayer instance
   */
  constructor(containerOrId, gridLayer) {
    this.container = typeof containerOrId === 'string'
      ? document.getElementById(containerOrId)
      : containerOrId;
    this.gridLayer = gridLayer;

    if (this.container) {
      this.render();
    }
  }

  render() {
    this.container.innerHTML = `
      <div class="grid-panel" style="padding:8px; display:flex; flex-direction:column; gap:8px;">
        <div style="display:flex; justify-content:space-between; align-items:center;">
          <button id="grid-toggle-btn" class="tool-btn" style="flex:1;">
            ${this.gridLayer.isActive ? 'Hide Lat/Lon Grid' : 'Show Lat/Lon Grid'}
          </button>
        </div>

        <div id="grid-settings" style="display:${this.gridLayer.isActive ? 'flex' : 'none'}; flex-direction:column; gap:6px; background:#1e293b; padding:8px; border-radius:4px;">
          <label style="font-size:11px; color:#94a3b8; display:flex; align-items:center; gap:6px; cursor:pointer;">
            <input type="checkbox" id="grid-auto-spacing" ${this.gridLayer.autoSpacing ? 'checked' : ''}>
            Adaptive Zoom Spacing
          </label>

          <div id="grid-fixed-spacing-row" style="display:${this.gridLayer.autoSpacing ? 'none' : 'flex'}; flex-direction:column; gap:4px;">
            <label style="font-size:11px; color:#cbd5e1;">Major Spacing (°)</label>
            <select id="grid-major-select" class="stamp-select" style="padding:3px; background:#0f172a; color:#fff; border:1px solid #334155;">
              <option value="45" ${this.gridLayer.majorInterval === 45 ? 'selected' : ''}>45°</option>
              <option value="30" ${this.gridLayer.majorInterval === 30 ? 'selected' : ''}>30°</option>
              <option value="15" ${this.gridLayer.majorInterval === 15 ? 'selected' : ''}>15°</option>
              <option value="10" ${this.gridLayer.majorInterval === 10 ? 'selected' : ''}>10°</option>
              <option value="5" ${this.gridLayer.majorInterval === 5 ? 'selected' : ''}>5°</option>
              <option value="1" ${this.gridLayer.majorInterval === 1 ? 'selected' : ''}>1°</option>
            </select>
          </div>

          <label style="font-size:11px; color:#cbd5e1;">Longitude Format</label>
          <select id="grid-lon-format" class="stamp-select" style="padding:3px; background:#0f172a; color:#fff; border:1px solid #334155;">
            <option value="east360" ${this.gridLayer.lonFormat === 'east360' ? 'selected' : ''}>0° - 360° East-positive</option>
            <option value="180" ${this.gridLayer.lonFormat === '180' ? 'selected' : ''}>-180° to +180°</option>
            <option value="west360" ${this.gridLayer.lonFormat === 'west360' ? 'selected' : ''}>0° - 360° West-positive</option>
          </select>

          <label style="font-size:11px; color:#94a3b8; display:flex; align-items:center; gap:6px; cursor:pointer;">
            <input type="checkbox" id="grid-show-minor" ${this.gridLayer.showMinor ? 'checked' : ''}>
            Show Minor Subdivision Lines
          </label>

          <label style="font-size:11px; color:#94a3b8; display:flex; align-items:center; gap:6px; cursor:pointer;">
            <input type="checkbox" id="grid-show-labels" ${this.gridLayer.showLabels ? 'checked' : ''}>
            Show Coordinate Edge Labels
          </label>
        </div>
      </div>
    `;

    const toggleBtn = this.container.querySelector('#grid-toggle-btn');
    const settingsDiv = this.container.querySelector('#grid-settings');
    const autoSpacingCb = this.container.querySelector('#grid-auto-spacing');
    const fixedRow = this.container.querySelector('#grid-fixed-spacing-row');
    const majorSelect = this.container.querySelector('#grid-major-select');
    const lonFormatSelect = this.container.querySelector('#grid-lon-format');
    const minorCb = this.container.querySelector('#grid-show-minor');
    const labelsCb = this.container.querySelector('#grid-show-labels');

    toggleBtn.addEventListener('click', () => {
      const active = this.gridLayer.toggle();
      toggleBtn.textContent = active ? 'Hide Lat/Lon Grid' : 'Show Lat/Lon Grid';
      toggleBtn.classList.toggle('active', active);
      settingsDiv.style.display = active ? 'flex' : 'none';
    });

    autoSpacingCb.addEventListener('change', (e) => {
      this.gridLayer.autoSpacing = e.target.checked;
      fixedRow.style.display = e.target.checked ? 'none' : 'flex';
      this.gridLayer.render();
    });

    majorSelect.addEventListener('change', (e) => {
      this.gridLayer.majorInterval = parseFloat(e.target.value);
      this.gridLayer.minorInterval = Math.max(this.gridLayer.majorInterval / 5, 0.2);
      this.gridLayer.render();
    });

    lonFormatSelect.addEventListener('change', (e) => {
      this.gridLayer.lonFormat = e.target.value;
      this.gridLayer.render();
    });

    minorCb.addEventListener('change', (e) => {
      this.gridLayer.showMinor = e.target.checked;
      this.gridLayer.render();
    });

    labelsCb.addEventListener('change', (e) => {
      this.gridLayer.showLabels = e.target.checked;
      this.gridLayer.render();
    });
  }
}
