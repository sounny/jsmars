import { EventBus } from '../../core/EventBus.js';
import { EVENTS } from '../../constants.js';
import { BODIES, to180 } from '../../util/geo.js';

/**
 * @module ProjectionManager
 * @description Map projection viewpoints and coordinate system management for jsMars.
 * Supports standard Equirectangular (Cylindrical), North/South Polar Stereographic,
 * Orthographic (3D Globe), and Sinusoidal Equal-Area projections with forward/inverse transforms.
 */
export class ProjectionManager {
  /**
   * @param {HTMLElement|string} container - Container element or ID
   * @param {L.Map} map - Leaflet map instance
   */
  constructor(container, map) {
    this.container = typeof container === 'string' ? document.getElementById(container) : container;
    this.map = map;
    this.currentProjection = 'cylindrical'; // 'cylindrical', 'north_polar', 'south_polar', 'orthographic', 'sinusoidal'
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
      if (this.map) this.map.setView([85, 0], 5);
    });

    this.btnSouth.addEventListener('click', () => {
      this.setProjection('south_polar');
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

  // --- Forward & Inverse Map Projection Solvers ---

  /**
   * Forward Equirectangular (Plate Carrée) projection.
   * @param {number} lat - Latitude in degrees
   * @param {number} lon - Longitude in degrees
   * @param {number} [lat0=0] - Standard parallel in degrees
   * @param {number} [lon0=0] - Central meridian in degrees
   * @param {string} [body='mars'] - Target planetary body
   * @returns {{x: number, y: number}} Projected coordinates in km
   */
  static forwardEquirectangular(lat, lon, lat0 = 0, lon0 = 0, body = 'mars') {
    const R = BODIES[body]?.meanRadius || 3389.5;
    const phi = lat * Math.PI / 180;
    const phi0 = lat0 * Math.PI / 180;
    const dLambda = (to180(lon - lon0)) * Math.PI / 180;

    const x = R * dLambda * Math.cos(phi0);
    const y = R * (phi - phi0);
    return { x, y };
  }

  /**
   * Inverse Equirectangular projection.
   */
  static inverseEquirectangular(x, y, lat0 = 0, lon0 = 0, body = 'mars') {
    const R = BODIES[body]?.meanRadius || 3389.5;
    const phi0 = lat0 * Math.PI / 180;

    const phi = phi0 + y / R;
    const dLambda = x / (R * Math.cos(phi0));
    return {
      lat: phi * 180 / Math.PI,
      lon: to180(lon0 + dLambda * 180 / Math.PI)
    };
  }

  /**
   * Forward Orthographic (3D View) projection.
   * @param {number} lat
   * @param {number} lon
   * @param {number} [centerLat=0]
   * @param {number} [centerLon=0]
   * @param {string} [body='mars']
   * @returns {{x: number, y: number, visible: boolean}}
   */
  static forwardOrthographic(lat, lon, centerLat = 0, centerLon = 0, body = 'mars') {
    const R = BODIES[body]?.meanRadius || 3389.5;
    const phi = lat * Math.PI / 180;
    const lambda = lon * Math.PI / 180;
    const phi0 = centerLat * Math.PI / 180;
    const lambda0 = centerLon * Math.PI / 180;

    const cosC = Math.sin(phi0) * Math.sin(phi) + Math.cos(phi0) * Math.cos(phi) * Math.cos(lambda - lambda0);
    const visible = cosC >= 0;

    const x = R * Math.cos(phi) * Math.sin(lambda - lambda0);
    const y = R * (Math.cos(phi0) * Math.sin(phi) - Math.sin(phi0) * Math.cos(phi) * Math.cos(lambda - lambda0));
    return { x, y, visible };
  }

  /**
   * Inverse Orthographic projection.
   */
  static inverseOrthographic(x, y, centerLat = 0, centerLon = 0, body = 'mars') {
    const R = BODIES[body]?.meanRadius || 3389.5;
    const rho = Math.sqrt(x * x + y * y);
    if (rho > R) return null; // Outside disk

    const phi0 = centerLat * Math.PI / 180;
    const lambda0 = centerLon * Math.PI / 180;

    if (rho === 0) {
      return { lat: centerLat, lon: to180(centerLon) };
    }

    const c = Math.asin(Math.min(1, rho / R));
    const sinC = Math.sin(c);
    const cosC = Math.cos(c);

    const lat = Math.asin(cosC * Math.sin(phi0) + (y * sinC * Math.cos(phi0)) / rho);
    const lon = lambda0 + Math.atan2(x * sinC, rho * Math.cos(phi0) * cosC - y * Math.sin(phi0) * sinC);

    return {
      lat: lat * 180 / Math.PI,
      lon: to180(lon * 180 / Math.PI)
    };
  }

  /**
   * Forward Sinusoidal (Sanson-Flamsteed equal-area) projection.
   */
  static forwardSinusoidal(lat, lon, lon0 = 0, body = 'mars') {
    const R = BODIES[body]?.meanRadius || 3389.5;
    const phi = lat * Math.PI / 180;
    const dLambda = (to180(lon - lon0)) * Math.PI / 180;

    const x = R * dLambda * Math.cos(phi);
    const y = R * phi;
    return { x, y };
  }

  /**
   * Inverse Sinusoidal projection.
   */
  static inverseSinusoidal(x, y, lon0 = 0, body = 'mars') {
    const R = BODIES[body]?.meanRadius || 3389.5;
    const phi = y / R;
    if (Math.abs(phi) > Math.PI / 2) return null;

    const cosPhi = Math.cos(phi);
    const dLambda = Math.abs(cosPhi) > 1e-7 ? x / (R * cosPhi) : 0;
    return {
      lat: phi * 180 / Math.PI,
      lon: to180(lon0 + dLambda * 180 / Math.PI)
    };
  }
}

