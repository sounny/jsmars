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

  /**
   * Forward Mollweide pseudocylindrical equal-area projection.
   * @param {number} lat - Latitude in degrees
   * @param {number} lon - Longitude in degrees
   * @param {number} [lon0=0] - Central meridian
   * @param {string} [body='mars']
   * @returns {{x: number, y: number}} Coordinates in km
   */
  static forwardMollweide(lat, lon, lon0 = 0, body = 'mars') {
    const R = BODIES[body]?.meanRadius || 3389.5;
    const phi = lat * Math.PI / 180;
    const dLambda = to180(lon - lon0) * Math.PI / 180;

    // Solve 2*theta + sin(2*theta) = pi * sin(phi) via Newton-Raphson
    let theta = phi;
    const target = Math.PI * Math.sin(phi);
    for (let iter = 0; iter < 10; iter++) {
      const f = 2 * theta + Math.sin(2 * theta) - target;
      const df = 2 + 2 * Math.cos(2 * theta);
      const delta = f / df;
      theta -= delta;
      if (Math.abs(delta) < 1e-7) break;
    }

    const x = (2 * Math.SQRT2 / Math.PI) * R * dLambda * Math.cos(theta);
    const y = Math.SQRT2 * R * Math.sin(theta);
    return { x, y };
  }

  /**
   * Inverse Mollweide projection.
   */
  static inverseMollweide(x, y, lon0 = 0, body = 'mars') {
    const R = BODIES[body]?.meanRadius || 3389.5;
    const sinTheta = y / (Math.SQRT2 * R);
    if (Math.abs(sinTheta) > 1) return null;

    const theta = Math.asin(sinTheta);
    const cosTheta = Math.cos(theta);
    if (Math.abs(cosTheta) < 1e-7) {
      return { lat: y > 0 ? 90 : -90, lon: to180(lon0) };
    }

    const phi = Math.asin((2 * theta + Math.sin(2 * theta)) / Math.PI);
    const dLambda = (Math.PI * x) / (2 * Math.SQRT2 * R * cosTheta);

    return {
      lat: phi * 180 / Math.PI,
      lon: to180(lon0 + dLambda * 180 / Math.PI)
    };
  }

  /**
   * Forward Lambert Azimuthal Equal-Area (LAEA) projection.
   * @param {number} lat - Latitude in degrees
   * @param {number} lon - Longitude in degrees
   * @param {number} [centerLat=90] - Center latitude (defaults to North Pole)
   * @param {number} [centerLon=0] - Center longitude
   * @param {string} [body='mars']
   * @returns {{x: number, y: number, visible: boolean}}
   */
  static forwardLambertAzimuthal(lat, lon, centerLat = 90, centerLon = 0, body = 'mars') {
    const R = BODIES[body]?.meanRadius || 3389.5;
    const phi = lat * Math.PI / 180;
    const lambda = lon * Math.PI / 180;
    const phi1 = centerLat * Math.PI / 180;
    const lambda0 = centerLon * Math.PI / 180;

    const cosC = Math.sin(phi1) * Math.sin(phi) + Math.cos(phi1) * Math.cos(phi) * Math.cos(lambda - lambda0);
    if (cosC < -1 + 1e-7) return { x: 0, y: 0, visible: false }; // Antipodal point

    const kPrime = Math.sqrt(2 / (1 + cosC));
    const x = R * kPrime * Math.cos(phi) * Math.sin(lambda - lambda0);
    const y = R * kPrime * (Math.cos(phi1) * Math.sin(phi) - Math.sin(phi1) * Math.cos(phi) * Math.cos(lambda - lambda0));

    return { x, y, visible: cosC >= 0 };
  }

  /**
   * Inverse Lambert Azimuthal Equal-Area projection.
   */
  static inverseLambertAzimuthal(x, y, centerLat = 90, centerLon = 0, body = 'mars') {
    const R = BODIES[body]?.meanRadius || 3389.5;
    const rho = Math.hypot(x, y);
    if (rho > 2 * R) return null;

    if (rho < 1e-6) {
      return { lat: centerLat, lon: to180(centerLon) };
    }

    const phi1 = centerLat * Math.PI / 180;
    const lambda0 = centerLon * Math.PI / 180;

    const c = 2 * Math.asin(rho / (2 * R));
    const sinC = Math.sin(c);
    const cosC = Math.cos(c);

    const phi = Math.asin(cosC * Math.sin(phi1) + (y * sinC * Math.cos(phi1)) / rho);
    let lambda;

    if (Math.abs(centerLat) >= 89.999) {
      // Polar aspect
      lambda = centerLat > 0 ? lambda0 + Math.atan2(x, -y) : lambda0 + Math.atan2(x, y);
    } else {
      lambda = lambda0 + Math.atan2(x * sinC, rho * Math.cos(phi1) * cosC - y * Math.sin(phi1) * sinC);
    }

    return {
      lat: phi * 180 / Math.PI,
      lon: to180(lambda * 180 / Math.PI)
    };
  }

  // --- Polar Stereographic & Cartographic Distortion Solvers ---

  /**
   * Forward Polar Stereographic (Conformal) projection.
   * @param {number} lat - Latitude in degrees
   * @param {number} lon - Longitude in degrees
   * @param {'north'|'south'} [hemisphere='north'] - Target polar aspect
   * @param {number} [lon0=0] - Central meridian
   * @param {string} [body='mars'] - Planetary body
   * @returns {{x: number, y: number, scaleFactor: number}} Coordinates in km
   */
  static forwardPolarStereographic(lat, lon, hemisphere = 'north', lon0 = 0, body = 'mars') {
    const R = BODIES[body]?.meanRadius || 3389.5;
    const isNorth = hemisphere.toLowerCase() === 'north';

    const phi = Math.abs(lat) * Math.PI / 180;
    const dLambda = to180(lon - lon0) * Math.PI / 180;

    // Rho = 2 * R * tan(pi/4 - phi/2)
    const rho = 2 * R * Math.tan(Math.PI / 4 - phi / 2);
    const k = 2 / (1 + Math.sin(phi)); // Conformal scale factor

    let x, y;
    if (isNorth) {
      x = rho * Math.sin(dLambda);
      y = -rho * Math.cos(dLambda);
    } else {
      x = rho * Math.sin(dLambda);
      y = rho * Math.cos(dLambda);
    }

    return {
      x: parseFloat(x.toFixed(3)),
      y: parseFloat(y.toFixed(3)),
      scaleFactor: parseFloat(k.toFixed(4))
    };
  }

  /**
   * Inverse Polar Stereographic projection.
   * @param {number} x - Projected x in km
   * @param {number} y - Projected y in km
   * @param {'north'|'south'} [hemisphere='north'] - Target polar aspect
   * @param {number} [lon0=0] - Central meridian
   * @param {string} [body='mars'] - Planetary body
   * @returns {{lat: number, lon: number}}
   */
  static inversePolarStereographic(x, y, hemisphere = 'north', lon0 = 0, body = 'mars') {
    const R = BODIES[body]?.meanRadius || 3389.5;
    const isNorth = hemisphere.toLowerCase() === 'north';

    const rho = Math.hypot(x, y);
    if (rho < 1e-7) {
      return { lat: isNorth ? 90 : -90, lon: to180(lon0) };
    }

    const phi = Math.PI / 2 - 2 * Math.atan(rho / (2 * R));
    let lambda;
    if (isNorth) {
      lambda = lon0 + Math.atan2(x, -y) * 180 / Math.PI;
    } else {
      lambda = lon0 + Math.atan2(x, y) * 180 / Math.PI;
    }

    return {
      lat: parseFloat(((isNorth ? phi : -phi) * 180 / Math.PI).toFixed(4)),
      lon: parseFloat(to180(lambda).toFixed(4))
    };
  }

  /**
   * Calculate local areal distortion ratio for given projection.
   * @param {number} lat - Latitude in degrees
   * @param {string} projName - 'sinusoidal', 'mollweide', 'laea', 'equirectangular', 'stereographic'
   * @returns {number} Areal distortion ratio (1.0 = equal-area)
   */
  static computeArealDistortion(lat, projName = 'sinusoidal') {
    const name = projName.toLowerCase();
    if (name === 'sinusoidal' || name === 'mollweide' || name === 'laea' || name === 'lambert') {
      return 1.0; // Strictly equal-area projections
    }

    const phi = Math.abs(lat) * Math.PI / 180;
    if (name === 'equirectangular' || name === 'cylindrical') {
      // Area scale = 1 / cos(phi)
      return parseFloat((1 / Math.max(0.01, Math.cos(phi))).toFixed(3));
    }
    if (name === 'stereographic' || name === 'polar') {
      // Area scale = k^2 = [2 / (1 + sin(phi))]^2
      const k = 2 / (1 + Math.sin(phi));
      return parseFloat((k * k).toFixed(3));
    }

    return 1.0;
  }

  // --- Geodetic Latitude & Planetary Longitude Transformation Solvers ---

  /**
   * Convert Planetocentric Latitude to Planetographic Latitude on an oblate planetary ellipsoid.
   * tan(phi_g) = tan(phi_c) / (1 - f)^2
   * @param {number} latCentricDeg - Planetocentric latitude (-90 to +90)
   * @param {number} [flattening=0.00589] - Planetary polar flattening f = (a - b) / a (Mars f ~ 0.00589)
   * @returns {number} Planetographic latitude in degrees
   */
  static convertPlanetocentricToPlanetographic(latCentricDeg, flattening = 0.00589) {
    if (Math.abs(latCentricDeg) >= 89.999) return latCentricDeg;

    const phiC = latCentricDeg * Math.PI / 180;
    const factor = 1.0 / Math.pow(1.0 - flattening, 2);
    const tanPhiG = factor * Math.tan(phiC);
    const phiG = Math.atan(tanPhiG) * 180 / Math.PI;

    return parseFloat(phiG.toFixed(4));
  }

  /**
   * Convert Planetographic Latitude to Planetocentric Latitude on an oblate planetary ellipsoid.
   * tan(phi_c) = (1 - f)^2 * tan(phi_g)
   * @param {number} latGraphicDeg - Planetographic latitude (-90 to +90)
   * @param {number} [flattening=0.00589] - Planetary polar flattening
   * @returns {number} Planetocentric latitude in degrees
   */
  static convertPlanetographicToPlanetocentric(latGraphicDeg, flattening = 0.00589) {
    if (Math.abs(latGraphicDeg) >= 89.999) return latGraphicDeg;

    const phiG = latGraphicDeg * Math.PI / 180;
    const factor = Math.pow(1.0 - flattening, 2);
    const tanPhiC = factor * Math.tan(phiG);
    const phiC = Math.atan(tanPhiC) * 180 / Math.PI;

    return parseFloat(phiC.toFixed(4));
  }

  /**
   * Convert longitude between IAU Martian cartographic conventions ('east360', 'east180', 'west360').
   * @param {number} lonDeg - Input longitude
   * @param {'east360'|'east180'|'west360'} [fromConvention='east360']
   * @param {'east360'|'east180'|'west360'} [toConvention='west360']
   * @returns {number} Converted longitude in degrees
   */
  static convertLongitudeConvention(lonDeg, fromConvention = 'east360', toConvention = 'west360') {
    // 1. Normalize input to standard East 360 [0, 360)
    let east360 = lonDeg;
    if (fromConvention === 'east180') {
      east360 = (lonDeg % 360 + 360) % 360;
    } else if (fromConvention === 'west360') {
      east360 = (360 - (lonDeg % 360)) % 360;
    } else {
      east360 = (lonDeg % 360 + 360) % 360;
    }

    // 2. Convert from East 360 to target convention
    if (toConvention === 'east180') {
      return parseFloat((east360 > 180 ? east360 - 360 : east360).toFixed(4));
    } else if (toConvention === 'west360') {
      const west360 = (360 - east360) % 360;
      return parseFloat(west360.toFixed(4));
    } else {
      return parseFloat(east360.toFixed(4));
    }
  }

  // --- Tissot's Indicatrix, Antipodal Points & True Map Scale Solvers ---

  /**
   * Calculate Tissot's Indicatrix distortion ellipse parameters (a, b, s, 2*theta).
   * @param {number} latDeg - Latitude in degrees
   * @param {string} [projName='mercator'] - Projection name ('mercator', 'equirectangular', 'stereographic', 'sinusoidal')
   * @returns {{a: number, b: number, areaScale: number, maxAngularDistortionDeg: number}}
   */
  static computeTissotIndicatrix(latDeg, projName = 'mercator') {
    const phi = Math.abs(latDeg) * Math.PI / 180.0;
    const name = projName.toLowerCase();

    let h = 1.0;
    let k = 1.0;

    if (name === 'mercator') {
      const secPhi = 1.0 / Math.max(0.01, Math.cos(phi));
      h = secPhi;
      k = secPhi;
    } else if (name === 'equirectangular' || name === 'cylindrical') {
      h = 1.0;
      k = 1.0 / Math.max(0.01, Math.cos(phi));
    } else if (name === 'stereographic' || name === 'polar') {
      const scale = 2.0 / (1.0 + Math.sin(phi));
      h = scale;
      k = scale;
    } else if (name === 'sinusoidal' || name === 'mollweide' || name === 'laea') {
      // Equal area: a * b = 1.0
      const secPhi = 1.0 / Math.max(0.01, Math.cos(phi));
      h = Math.cos(phi);
      k = secPhi;
    }

    const a = Math.max(h, k);
    const b = Math.min(h, k);
    const areaScale = h * k;

    // Maximum angular distortion 2*theta = 2 * asin((a - b) / (a + b))
    const sinTheta = (a - b) / (a + b);
    const maxAngularDistortionDeg = 2.0 * Math.asin(Math.max(0, Math.min(1.0, sinTheta))) * 180.0 / Math.PI;

    return {
      a: parseFloat(a.toFixed(4)),
      b: parseFloat(b.toFixed(4)),
      areaScale: parseFloat(areaScale.toFixed(4)),
      maxAngularDistortionDeg: parseFloat(maxAngularDistortionDeg.toFixed(2))
    };
  }

  /**
   * Compute exact planetary antipode coordinates.
   * @param {number} latDeg - Input latitude (-90 to +90)
   * @param {number} lonDeg - Input longitude (0 to 360 East)
   * @returns {{lat: number, lon: number}} Antipodal coordinates
   */
  static computeAntipode(latDeg, lonDeg) {
    const antiLat = -latDeg;
    const antiLon = (lonDeg + 180.0) % 360.0;

    return {
      lat: parseFloat(antiLat.toFixed(4)),
      lon: parseFloat(antiLon.toFixed(4))
    };
  }

  /**
   * Compute local true ground pixel resolution at given latitude.
   * @param {number} nominalScaleMPerPixel - Scale at equator in m/pixel
   * @param {number} latDeg - Latitude in degrees
   * @returns {number} True scale in meters/pixel
   */
  static computeTrueScaleAtLatitude(nominalScaleMPerPixel, latDeg) {
    const phi = Math.abs(latDeg) * Math.PI / 180.0;
    const trueScale = nominalScaleMPerPixel * Math.cos(phi);
    return parseFloat(trueScale.toFixed(2));
  }
}





