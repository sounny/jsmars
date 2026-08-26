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

  // --- Standard Parallel Scaling, Grid Convergence & Heading Departure Solvers ---

  /**
   * Calculate exact conformal scale factor for Secant Polar Stereographic projection with standard parallel phi0.
   * k(phi) = (1 + sin(phi0)) / (1 + sin(phi))
   * @param {number} latDeg - Point latitude (-90 to +90)
   * @param {number} [standardParallelLatDeg=70] - Standard parallel of true scale (e.g. 70° for Mars polar maps)
   * @returns {number} Scale factor k
   */
  static computeStandardParallelScale(latDeg, standardParallelLatDeg = 70) {
    const phi = Math.abs(latDeg) * Math.PI / 180.0;
    const phi0 = Math.abs(standardParallelLatDeg) * Math.PI / 180.0;

    const k = (1.0 + Math.sin(phi0)) / (1.0 + Math.sin(phi));
    return parseFloat(k.toFixed(4));
  }

  /**
   * Calculate Grid Convergence angle (gamma) between True Geodetic North and Grid North.
   * gamma = (lambda - lambda0) * sin(phi)
   * @param {number} latDeg - Latitude in degrees
   * @param {number} lonDeg - Longitude in degrees
   * @param {number} [centralMeridianLonDeg=0] - Central meridian longitude
   * @returns {number} Grid convergence angle in degrees
   */
  static computeGridConvergence(latDeg, lonDeg, centralMeridianLonDeg = 0) {
    const phi = latDeg * Math.PI / 180.0;
    const dLambda = to180(lonDeg - centralMeridianLonDeg);

    const gamma = dLambda * Math.sin(phi);
    return parseFloat(gamma.toFixed(3));
  }

  /**
   * Calculate departure between Great-Circle Initial Heading and Constant-Bearing Rhumb Line Heading.
   * @param {number} lat1 - Start latitude
   * @param {number} lon1 - Start longitude
   * @param {number} lat2 - End latitude
   * @param {number} lon2 - End longitude
   * @returns {{greatCircleAzimuthDeg: number, rhumbLineAzimuthDeg: number, departureDeg: number}}
   */
  static computeGreatCircleAzimuthDistortion(lat1, lon1, lat2, lon2) {
    const phi1 = lat1 * Math.PI / 180.0;
    const phi2 = lat2 * Math.PI / 180.0;
    const dLam = to180(lon2 - lon1) * Math.PI / 180.0;

    // Great circle initial bearing
    const y = Math.sin(dLam) * Math.cos(phi2);
    const x = Math.cos(phi1) * Math.sin(phi2) - Math.sin(phi1) * Math.cos(phi2) * Math.cos(dLam);
    let gcAz = (Math.atan2(y, x) * 180.0 / Math.PI + 360.0) % 360.0;

    // Rhumb line bearing
    const dPsi = Math.log(Math.tan(Math.PI / 4.0 + phi2 / 2.0) / Math.tan(Math.PI / 4.0 + phi1 / 2.0));
    let rhumbAz = (Math.atan2(dLam, dPsi) * 180.0 / Math.PI + 360.0) % 360.0;

    let departure = Math.abs(gcAz - rhumbAz);
    if (departure > 180) departure = 360 - departure;

    return {
      greatCircleAzimuthDeg: parseFloat(gcAz.toFixed(2)),
      rhumbLineAzimuthDeg: parseFloat(rhumbAz.toFixed(2)),
      departureDeg: parseFloat(departure.toFixed(2))
    };
  }

  // --- Albers Equal-Area Conic & Cartographic Scale Solvers ---

  /**
   * Forward Albers Equal-Area Conic projection.
   * @param {number} lat - Latitude in degrees
   * @param {number} lon - Longitude in degrees
   * @param {number} [lat1=20] - Standard parallel 1
   * @param {number} [lat2=60] - Standard parallel 2
   * @param {number} [lon0=0] - Central meridian
   * @param {string} [body='mars'] - Planetary body
   * @returns {{x: number, y: number}} Projected coordinates in km
   */
  static forwardAlbersEqualArea(lat, lon, lat1 = 20, lat2 = 60, lon0 = 0, body = 'mars') {
    const R = BODIES[body]?.meanRadius || 3389.5;
    const phi = lat * Math.PI / 180.0;
    const phi1 = lat1 * Math.PI / 180.0;
    const phi2 = lat2 * Math.PI / 180.0;
    const dLam = to180(lon - lon0) * Math.PI / 180.0;

    const n = 0.5 * (Math.sin(phi1) + Math.sin(phi2));
    const C = Math.pow(Math.cos(phi1), 2) + 2.0 * n * Math.sin(phi1);
    const rho = (R / n) * Math.sqrt(Math.max(0, C - 2.0 * n * Math.sin(phi)));
    const rho0 = (R / n) * Math.sqrt(Math.max(0, C - 2.0 * n * Math.sin(0))); // Origin at equator

    const theta = n * dLam;
    const x = rho * Math.sin(theta);
    const y = rho0 - rho * Math.cos(theta);

    return {
      x: parseFloat(x.toFixed(3)),
      y: parseFloat(y.toFixed(3))
    };
  }

  /**
   * Inverse Albers Equal-Area Conic projection.
   * @param {number} x - Projected x in km
   * @param {number} y - Projected y in km
   * @param {number} [lat1=20] - Standard parallel 1
   * @param {number} [lat2=60] - Standard parallel 2
   * @param {number} [lon0=0] - Central meridian
   * @param {string} [body='mars'] - Planetary body
   * @returns {{lat: number, lon: number}} Unprojected latitude and longitude in degrees
   */
  static inverseAlbersEqualArea(x, y, lat1 = 20, lat2 = 60, lon0 = 0, body = 'mars') {
    const R = BODIES[body]?.meanRadius || 3389.5;
    const phi1 = lat1 * Math.PI / 180.0;
    const phi2 = lat2 * Math.PI / 180.0;

    const n = 0.5 * (Math.sin(phi1) + Math.sin(phi2));
    const C = Math.pow(Math.cos(phi1), 2) + 2.0 * n * Math.sin(phi1);
    const rho0 = (R / n) * Math.sqrt(Math.max(0, C - 2.0 * n * Math.sin(0)));

    const rho = Math.sign(n) * Math.hypot(x, rho0 - y);
    const sinPhi = (C - Math.pow(rho * n / R, 2)) / (2.0 * n);
    const phi = Math.asin(Math.max(-1.0, Math.min(1.0, sinPhi)));
    const theta = Math.atan2(x, rho0 - y);
    const dLam = theta / n;

    return {
      lat: parseFloat((phi * 180.0 / Math.PI).toFixed(4)),
      lon: parseFloat(to180(lon0 + dLam * 180.0 / Math.PI).toFixed(4))
    };
  }

  /**
   * Calculate Equirectangular cylindrical areal expansion scale factor (secant of latitude).
   * @param {number} latDeg - Latitude in degrees
   * @returns {number} Areal scale factor s
   */
  static computeEquirectangularArealScale(latDeg) {
    const phi = Math.abs(latDeg) * Math.PI / 180.0;
    const s = 1.0 / Math.max(0.001, Math.cos(phi));
    return parseFloat(s.toFixed(3));
  }

  // --- Stereographic Point Scale, Parallel Length & Conic Constant Solvers ---

  /**
   * Calculate exact conformal point scale factor for general Oblique or Polar Stereographic projection.
   * k = 2 / (1 + sin(phi0)*sin(phi) + cos(phi0)*cos(phi)*cos(lambda - lambda0))
   * @param {number} latDeg - Point latitude
   * @param {number} lonDeg - Point longitude
   * @param {number} [centerLatDeg=90] - Projection center latitude (90 = North Pole)
   * @param {number} [centerLonDeg=0] - Projection center longitude
   * @returns {number} Conformal scale factor k (1.0 at center)
   */
  static computeStereographicPointScale(latDeg, lonDeg, centerLatDeg = 90, centerLonDeg = 0) {
    const phi = latDeg * Math.PI / 180.0;
    const lam = lonDeg * Math.PI / 180.0;
    const phi0 = centerLatDeg * Math.PI / 180.0;
    const lam0 = centerLonDeg * Math.PI / 180.0;

    const cosC = Math.sin(phi0) * Math.sin(phi) + Math.cos(phi0) * Math.cos(phi) * Math.cos(lam - lam0);
    const k = 2.0 / Math.max(1e-6, 1.0 + cosC);

    return parseFloat(k.toFixed(4));
  }

  /**
   * Calculate physical circumference length of a latitude parallel in km.
   * L(phi) = 2 * pi * R * cos(phi)
   * @param {number} latDeg - Latitude in degrees
   * @param {string} [body='mars'] - Planetary body
   * @returns {number} Parallel circumference in km
   */
  static computeSinusoidalParallelLength(latDeg, body = 'mars') {
    const R = BODIES[body]?.meanRadius || 3389.5;
    const phi = Math.abs(latDeg) * Math.PI / 180.0;
    const len = 2.0 * Math.PI * R * Math.cos(phi);

    return parseFloat(len.toFixed(2));
  }

  /**
   * Calculate cone constant n and apical opening half-angle for conic projections between standard parallels.
   * n = (sin(phi1) + sin(phi2)) / 2
   * @param {number} lat1Deg - Standard parallel 1
   * @param {number} lat2Deg - Standard parallel 2
   * @returns {{coneConstantN: number, apicalHalfAngleDeg: number}}
   */
  static computeConicConeConstant(lat1Deg, lat2Deg) {
    const phi1 = lat1Deg * Math.PI / 180.0;
    const phi2 = lat2Deg * Math.PI / 180.0;
    const n = 0.5 * (Math.sin(phi1) + Math.sin(phi2));
    const halfAngle = Math.asin(Math.max(-1.0, Math.min(1.0, n))) * 180.0 / Math.PI;

    return {
      coneConstantN: parseFloat(n.toFixed(4)),
      apicalHalfAngleDeg: parseFloat(halfAngle.toFixed(2))
    };
  }

  // --- Authalic Radius, Wagner IV Equal-Area & Meridional Arc Solvers ---

  /**
   * Calculate authalic (equal-surface-area) sphere radius R_q of an oblate planetary ellipsoid.
   * R_q = sqrt( [ a^2 + (b^2 / (2*e)) * ln((1+e)/(1-e)) ] / 2 )
   * @param {number} [semiMajorAxisKm=3396.19] - Equatorial radius a in km
   * @param {number} [flattening=0.005886] - Ellipsoidal flattening f = (a - b) / a
   * @returns {{authalicRadiusKm: number, surfaceAreaKm2: number}}
   */
  static computeAuthalicRadius(semiMajorAxisKm = 3396.19, flattening = 0.005886) {
    const a = semiMajorAxisKm;
    const f = flattening;
    const b = a * (1.0 - f);
    const e2 = 2.0 * f - f * f;
    const e = Math.sqrt(e2);

    let Rq = a;
    if (e > 1e-6) {
      const term = (b * b / (2.0 * e)) * Math.log((1.0 + e) / (1.0 - e));
      const area = 2.0 * Math.PI * (a * a + term);
      Rq = Math.sqrt(area / (4.0 * Math.PI));
    }

    const totalArea = 4.0 * Math.PI * Rq * Rq;

    return {
      authalicRadiusKm: parseFloat(Rq.toFixed(3)),
      surfaceAreaKm2: parseFloat(totalArea.toFixed(1))
    };
  }

  /**
   * Forward Wagner IV pseudocylindrical equal-area projection with pole line.
   * @param {number} lat - Latitude in degrees
   * @param {number} lon - Longitude in degrees
   * @param {number} [lon0=0] - Central meridian
   * @param {string} [body='mars'] - Planetary body
   * @returns {{x: number, y: number}} Projected coordinates in km
   */
  static computeWagnerIVElliptical(lat, lon, lon0 = 0, body = 'mars') {
    const R = BODIES[body]?.meanRadius || 3389.5;
    const phi = lat * Math.PI / 180.0;
    const dLam = to180(lon - lon0) * Math.PI / 180.0;

    // Solve 2*theta + sin(2*theta) = (4 + pi) * sin(phi) / 2
    let theta = phi;
    const target = ((4.0 + Math.PI) / 2.0) * Math.sin(phi);
    for (let iter = 0; iter < 10; iter++) {
      const fVal = 2.0 * theta + Math.sin(2.0 * theta) - target;
      const df = 2.0 + 2.0 * Math.cos(2.0 * theta);
      const delta = fVal / df;
      theta -= delta;
      if (Math.abs(delta) < 1e-7) break;
    }

    const x = (4.0 * R / (Math.PI * Math.sqrt(3.0))) * dLam * Math.cos(theta);
    const y = (2.0 * R / Math.sqrt(3.0)) * Math.sin(theta);

    return {
      x: parseFloat(x.toFixed(3)),
      y: parseFloat(y.toFixed(3))
    };
  }

  /**
   * Calculate precise ellipsoidal meridional arc distance between two latitudes.
   * M = a * (1 - e^2) * integral( (1 - e^2*sin^2(phi))^(-3/2) dphi )
   * @param {number} lat1Deg - Start latitude
   * @param {number} lat2Deg - End latitude
   * @param {string} [body='mars'] - Planetary body
   * @returns {number} Meridional distance along meridian in km
   */
  static computeMeridianDistance(lat1Deg, lat2Deg, body = 'mars') {
    const isMars = body.toLowerCase() === 'mars';
    const a = isMars ? 3396.19 : (body.toLowerCase() === 'moon' ? 1737.4 : 6378.14);
    const f = isMars ? 0.005886 : (body.toLowerCase() === 'moon' ? 0.0001 : 0.0033528);
    const e2 = 2.0 * f - f * f;

    const phi1 = Math.min(lat1Deg, lat2Deg) * Math.PI / 180.0;
    const phi2 = Math.max(lat1Deg, lat2Deg) * Math.PI / 180.0;

    // Simpson numerical integration with 50 steps
    const n = 50;
    const h = (phi2 - phi1) / n;
    let sum = 0;

    const integrand = (phi) => {
      const sinP = Math.sin(phi);
      return Math.pow(1.0 - e2 * sinP * sinP, -1.5);
    };

    for (let i = 0; i <= n; i++) {
      const p = phi1 + i * h;
      const weight = (i === 0 || i === n) ? 1 : (i % 2 === 1 ? 4 : 2);
      sum += weight * integrand(p);
    }

    const integral = (h / 3.0) * sum;
    const distanceKm = a * (1.0 - e2) * integral;

    return parseFloat(distanceKm.toFixed(3));
  }

  // --- Gnomonic Projection, Transverse Mercator Convergence & Sinusoidal Distortion Solvers ---

  /**
   * Forward Gnomonic perspective projection from planetary center (maps all great-circle geodesics to straight lines).
   * @param {number} lat - Latitude in degrees
   * @param {number} lon - Longitude in degrees
   * @param {number} [centerLat=0] - Center latitude
   * @param {number} [centerLon=0] - Center longitude
   * @param {string} [body='mars'] - Planetary body
   * @returns {{x: number, y: number, visible: boolean}} Projected coordinates in km
   */
  static forwardGnomonic(lat, lon, centerLat = 0, centerLon = 0, body = 'mars') {
    const R = BODIES[body]?.meanRadius || 3389.5;
    const phi = lat * Math.PI / 180.0;
    const lam = lon * Math.PI / 180.0;
    const phi0 = centerLat * Math.PI / 180.0;
    const lam0 = centerLon * Math.PI / 180.0;
    const dLam = lam - lam0;

    const cosC = Math.sin(phi0) * Math.sin(phi) + Math.cos(phi0) * Math.cos(phi) * Math.cos(dLam);
    if (cosC <= 0) {
      return { x: 0, y: 0, visible: false }; // Beyond hemisphere horizon
    }

    const x = (R * Math.cos(phi) * Math.sin(dLam)) / cosC;
    const y = (R * (Math.cos(phi0) * Math.sin(phi) - Math.sin(phi0) * Math.cos(phi) * Math.cos(dLam))) / cosC;

    return {
      x: parseFloat(x.toFixed(3)),
      y: parseFloat(y.toFixed(3)),
      visible: true
    };
  }

  /**
   * Calculate Transverse Mercator meridian convergence angle gamma.
   * gamma = atan(tan(lambda - lambda0) * sin(phi))
   * @param {number} latDeg - Latitude in degrees
   * @param {number} lonDeg - Longitude in degrees
   * @param {number} [centralMeridianLonDeg=0] - Central meridian
   * @returns {number} Convergence angle in degrees
   */
  static computeTransverseMercatorConvergence(latDeg, lonDeg, centralMeridianLonDeg = 0) {
    const phi = latDeg * Math.PI / 180.0;
    const dLam = to180(lonDeg - centralMeridianLonDeg) * Math.PI / 180.0;

    const tanGamma = Math.tan(dLam) * Math.sin(phi);
    const gammaDeg = Math.atan(tanGamma) * 180.0 / Math.PI;

    return parseFloat(gammaDeg.toFixed(3));
  }

  /**
   * Calculate Sinusoidal equal-area angular shear distortion angle theta'.
   * cos(theta') = -sin(phi) * sin(lambda - lambda0)
   * @param {number} latDeg - Latitude in degrees
   * @param {number} [dLonDeg=30.0] - Longitude distance from central meridian in degrees
   * @returns {{shearAngleDeg: number, maxShearDeg: number}}
   */
  static computeSinusoidalDistortionMetrics(latDeg, dLonDeg = 30.0) {
    const phi = latDeg * Math.PI / 180.0;
    const dLam = dLonDeg * Math.PI / 180.0;

    const cosThetaPrime = -Math.sin(phi) * Math.sin(dLam);
    const thetaPrimeRad = Math.acos(Math.max(-1.0, Math.min(1.0, cosThetaPrime)));
    const shearDeg = Math.abs(90.0 - thetaPrimeRad * 180.0 / Math.PI);

    return {
      shearAngleDeg: parseFloat(shearDeg.toFixed(2)),
      maxShearDeg: parseFloat((shearDeg * 2.0).toFixed(2))
    };
  }

  // --- Lambert Conformal Conic (LCC) Forward, Inverse & Point Scale Solvers ---

  /**
   * Forward Lambert Conformal Conic (LCC) secant projection (Snyder 1987).
   * @param {number} lat - Latitude in degrees
   * @param {number} lon - Longitude in degrees
   * @param {number} [lat1=20.0] - First standard parallel in degrees
   * @param {number} [lat2=60.0] - Second standard parallel in degrees
   * @param {number} [lon0=0] - Central meridian in degrees
   * @param {string} [body='mars'] - Planetary body
   * @returns {{x: number, y: number, scaleFactor: number}} Projected coordinates in km
   */
  static forwardLambertConformalConic(lat, lon, lat1 = 20.0, lat2 = 60.0, lon0 = 0, body = 'mars') {
    const R = BODIES[body]?.meanRadius || 3389.5;
    const phi = lat * Math.PI / 180.0;
    const phi1 = lat1 * Math.PI / 180.0;
    const phi2 = lat2 * Math.PI / 180.0;
    const dLam = to180(lon - lon0) * Math.PI / 180.0;

    const t = (p) => Math.tan(Math.PI / 4.0 - p / 2.0);
    const m = (p) => Math.cos(p);

    const t1 = t(phi1);
    const t2 = t(phi2);
    const m1 = m(phi1);
    const m2 = m(phi2);

    const n = Math.log(m1 / m2) / Math.log(t1 / t2);
    const F = m1 / (n * Math.pow(t1, n));

    const t_phi = t(phi);
    const rho = R * F * Math.pow(t_phi, n);
    const rho0 = R * F * Math.pow(t(0), n); // Origin at equator

    const theta = n * dLam;
    const x = rho * Math.sin(theta);
    const y = rho0 - rho * Math.cos(theta);

    const k = (rho * n) / (R * Math.max(1e-6, Math.cos(phi)));

    return {
      x: parseFloat(x.toFixed(3)),
      y: parseFloat(y.toFixed(3)),
      scaleFactor: parseFloat(k.toFixed(4))
    };
  }

  /**
   * Inverse Lambert Conformal Conic (LCC) projection.
   * @param {number} x - Projected X in km
   * @param {number} y - Projected Y in km
   * @param {number} [lat1=20.0] - Standard parallel 1
   * @param {number} [lat2=60.0] - Standard parallel 2
   * @param {number} [lon0=0] - Central meridian
   * @param {string} [body='mars'] - Planetary body
   * @returns {{lat: number, lon: number}} Unprojected latitude and longitude in degrees
   */
  static inverseLambertConformalConic(x, y, lat1 = 20.0, lat2 = 60.0, lon0 = 0, body = 'mars') {
    const R = BODIES[body]?.meanRadius || 3389.5;
    const phi1 = lat1 * Math.PI / 180.0;
    const phi2 = lat2 * Math.PI / 180.0;

    const t = (p) => Math.tan(Math.PI / 4.0 - p / 2.0);
    const m = (p) => Math.cos(p);

    const t1 = t(phi1);
    const t2 = t(phi2);
    const m1 = m(phi1);
    const m2 = m(phi2);

    const n = Math.log(m1 / m2) / Math.log(t1 / t2);
    const F = m1 / (n * Math.pow(t1, n));
    const rho0 = R * F * Math.pow(t(0), n);

    const rho = Math.sign(n) * Math.hypot(x, rho0 - y);
    const theta = Math.atan2(Math.sign(n) * x, Math.sign(n) * (rho0 - y));

    const t_val = Math.pow(rho / (R * F), 1.0 / n);
    const phi = Math.PI / 2.0 - 2.0 * Math.atan(t_val);
    const dLam = theta / n;

    return {
      lat: parseFloat((phi * 180.0 / Math.PI).toFixed(4)),
      lon: parseFloat(to180(lon0 + dLam * 180.0 / Math.PI).toFixed(4))
    };
  }

  /**
   * Calculate exact conformal point scale factor for Lambert Conformal Conic.
   * @param {number} latDeg - Point latitude in degrees
   * @param {number} [lat1Deg=20.0] - Standard parallel 1
   * @param {number} [lat2Deg=60.0] - Standard parallel 2
   * @returns {number} Scale factor (1.0 at standard parallels)
   */
  static computeLCCScaleFactor(latDeg, lat1Deg = 20.0, lat2Deg = 60.0) {
    const res = this.forwardLambertConformalConic(latDeg, 0, lat1Deg, lat2Deg, 0, 'mars');
    return res.scaleFactor;
  }

  // --- Spherical Midpoint, Tissot Area Ratio & Gnomonic Scale Solvers ---

  /**
   * Calculate exact spherical great-circle midpoint coordinates between two planetary points.
   * @param {number} lat1Deg - Latitude of point 1 in degrees
   * @param {number} lon1Deg - Longitude of point 1 in degrees
   * @param {number} lat2Deg - Latitude of point 2 in degrees
   * @param {number} lon2Deg - Longitude of point 2 in degrees
   * @returns {{lat: number, lon: number}} Midpoint coordinates in degrees
   */
  static computeSphericalMidpoint(lat1Deg, lon1Deg, lat2Deg, lon2Deg) {
    const phi1 = lat1Deg * Math.PI / 180.0;
    const phi2 = lat2Deg * Math.PI / 180.0;
    const lam1 = lon1Deg * Math.PI / 180.0;
    const dLam = to180(lon2Deg - lon1Deg) * Math.PI / 180.0;

    const Bx = Math.cos(phi2) * Math.cos(dLam);
    const By = Math.cos(phi2) * Math.sin(dLam);

    const phiM = Math.atan2(Math.sin(phi1) + Math.sin(phi2), Math.hypot(Math.cos(phi1) + Bx, By));
    const lamM = lam1 + Math.atan2(By, Math.cos(phi1) + Bx);

    return {
      lat: parseFloat((phiM * 180.0 / Math.PI).toFixed(4)),
      lon: parseFloat(to180(lamM * 180.0 / Math.PI).toFixed(4))
    };
  }

  /**
   * Calculate Tissot indicatrix area distortion ratio s = h * k * cos(theta').
   * @param {number} scaleH - Meridian scale factor h
   * @param {number} scaleK - Parallel scale factor k
   * @param {number} [angularShearDeg=0] - Angular shear theta' in degrees (0 for orthogonal graticules)
   * @returns {{areaDistortionRatio: number, isAreaPreserving: boolean}}
   */
  static computeTissotIndicatrixAreaRatio(scaleH, scaleK, angularShearDeg = 0) {
    const h = Math.max(0.001, scaleH);
    const k = Math.max(0.001, scaleK);
    const thetaRad = Math.abs(angularShearDeg) * Math.PI / 180.0;

    const s = h * k * Math.cos(thetaRad);
    const isAreaPreserving = Math.abs(s - 1.0) < 0.001;

    return {
      areaDistortionRatio: parseFloat(s.toFixed(4)),
      isAreaPreserving
    };
  }

  /**
   * Calculate Gnomonic radial point scale factor k = sec^2(c) = 1 + (rho / R)^2.
   * @param {number} distanceFromCenterKm - Projected radial distance from projection center in km
   * @param {string} [body='mars'] - Target planetary body
   * @returns {{radialScaleFactor: number, angularDistanceDeg: number}}
   */
  static computeGnomonicProjectionScale(distanceFromCenterKm, body = 'mars') {
    const R = BODIES[body]?.meanRadius || 3389.5;
    const rho = Math.max(0, distanceFromCenterKm);

    const cRad = Math.atan(rho / R);
    const cDeg = cRad * 180.0 / Math.PI;
    const k = 1.0 + Math.pow(rho / R, 2);

    return {
      radialScaleFactor: parseFloat(k.toFixed(4)),
      angularDistanceDeg: parseFloat(cDeg.toFixed(2))
    };
  }
}












