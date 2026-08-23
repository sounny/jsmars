import { EventBus } from '../../core/EventBus.js';
import { EVENTS } from '../../constants.js';

/**
 * @module ProjectionManager
 * @description Map projection viewpoints and coordinate system management for jsMars.
 * Supports standard Equirectangular (Cylindrical), North Polar Stereographic, and South Polar Stereographic viewpoints.
 */
export class ProjectionManager {
  /**
   * @param {HTMLElement|string} container - Container element or ID
   * @param {L.Map} map - Leaflet map instance
   */
  constructor(container, map) {
    this.container = typeof container === 'string' ? document.getElementById(container) : container;
    this.map = map;
    this.currentProjection = 'cylindrical'; // 'cylindrical', 'north_polar', 'south_polar'
    this.latConvention = 'centric'; // 'centric' or 'graphic'
    this.lonConvention = 'east360'; // 'east360', 'east180', 'west360'

    if (this.container) {
      this.init();
    }
  }

  init() {
    this.container.innerHTML = `
      <div style="padding: 10px; font-size: 12px; color: #e2e8f0;">
        <div style="margin-bottom: 8px;">
          <label style="font-size: 10px; color: #94a3b8; display: block; margin-bottom: 2px;">Map Viewpoint / Region</label>
          <div style="display: flex; gap: 4px;">
            <button id="proj-btn-cyl" class="tool-btn" style="flex: 1; font-size: 10px; background: #0284c7;">Global</button>
            <button id="proj-btn-north" class="tool-btn" style="flex: 1; font-size: 10px; background: #334155;">North Pole</button>
            <button id="proj-btn-south" class="tool-btn" style="flex: 1; font-size: 10px; background: #334155;">South Pole</button>
          </div>
        </div>

        <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 6px;">
          <div>
            <label style="font-size: 10px; color: #94a3b8; display: block; margin-bottom: 2px;">Latitude</label>
            <select id="proj-lat-select" class="tool-select" style="width: 100%; font-size: 11px;">
              <option value="centric">Planetocentric</option>
              <option value="graphic">Planetographic</option>
            </select>
          </div>
          <div>
            <label style="font-size: 10px; color: #94a3b8; display: block; margin-bottom: 2px;">Longitude</label>
            <select id="proj-lon-select" class="tool-select" style="width: 100%; font-size: 11px;">
              <option value="east360">0° – 360° East</option>
              <option value="east180">-180° – +180°</option>
              <option value="west360">0° – 360° West</option>
            </select>
          </div>
        </div>
      </div>
    `;

    this.btnCyl = this.container.querySelector('#proj-btn-cyl');
    this.btnNorth = this.container.querySelector('#proj-btn-north');
    this.btnSouth = this.container.querySelector('#proj-btn-south');
    this.latSelect = this.container.querySelector('#proj-lat-select');
    this.lonSelect = this.container.querySelector('#proj-lon-select');

    this.bindEvents();
  }

  bindEvents() {
    this.btnCyl.addEventListener('click', () => {
      this.setProjection('cylindrical');
      if (this.map) this.map.setView([0, 0], 2);
    });

    this.btnNorth.addEventListener('click', () => {
      this.setProjection('north_polar');
      // Pan to Planum Boreum North Pole
      if (this.map) this.map.setView([85, 0], 5);
    });

    this.btnSouth.addEventListener('click', () => {
      this.setProjection('south_polar');
      // Pan to Planum Australe South Pole
      if (this.map) this.map.setView([-85, 0], 5);
    });

    this.latSelect.addEventListener('change', (e) => {
      this.latConvention = e.target.value;
      this.broadcastCoordFormat();
    });

    this.lonSelect.addEventListener('change', (e) => {
      this.lonConvention = e.target.value;
      this.broadcastCoordFormat();
    });
  }

  setProjection(proj) {
    this.currentProjection = proj;
    [this.btnCyl, this.btnNorth, this.btnSouth].forEach(b => b.style.background = '#334155');
    if (proj === 'cylindrical') this.btnCyl.style.background = '#0284c7';
    else if (proj === 'north_polar') this.btnNorth.style.background = '#0284c7';
    else if (proj === 'south_polar') this.btnSouth.style.background = '#0284c7';

    EventBus.emit(EVENTS.PROJECTION_CHANGED, { projection: proj });
  }

  broadcastCoordFormat() {
    EventBus.emit(EVENTS.COORD_FORMAT_CHANGED, {
      latFormat: this.latConvention,
      lonFormat: this.lonConvention
    });
  }
}
