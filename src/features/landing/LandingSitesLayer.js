import { EVENTS } from '../../constants.js';
import { computeEllipsePolygon } from '../../util/geo.js';

/**
 * LandingSitesLayer displays markers and landing dispersion ellipses for
 * spacecraft landing sites on the current planetary body (Mars, Moon).
 */
export class LandingSitesLayer {
  constructor(map) {
    this.map = map;
    this.markerGroup = L.layerGroup();
    this.isActive = false;
    this.currentBody = 'mars';
    this.showEllipses = true;
    this.sites = [];
    this._onBodyChanged = this._onBodyChanged.bind(this);
    this._loadData();
  }

  /**
   * Load landing site data from JSON.
   */
  async _loadData() {
    try {
      const response = await fetch('./src/data/landing-sites.json');
      if (!response.ok) throw new Error(`HTTP ${response.status}`);
      this.sites = await response.json();
      if (this.isActive) this._render();
    } catch (err) {
      console.error('Failed to load landing sites:', err);
    }
  }

  /**
   * Activate the landing sites layer and show markers.
   */
  activate() {
    if (this.isActive) return;
    this.isActive = true;
    this.markerGroup.addTo(this.map);
    document.addEventListener(EVENTS.BODY_CHANGED, this._onBodyChanged);
    this._render();
  }

  /**
   * Deactivate the landing sites layer and remove markers.
   */
  deactivate() {
    if (!this.isActive) return;
    this.isActive = false;
    this.map.removeLayer(this.markerGroup);
    document.removeEventListener(EVENTS.BODY_CHANGED, this._onBodyChanged);
  }

  /**
   * Toggle layer visibility.
   * @returns {boolean} New active state
   */
  toggle() {
    if (this.isActive) {
      this.deactivate();
    } else {
      this.activate();
    }
    return this.isActive;
  }

  /**
   * Handle body change events.
   * @param {CustomEvent} e
   */
  _onBodyChanged(e) {
    const body = e?.detail?.body;
    if (body) {
      this.currentBody = body;
      this._render();
    }
  }

  /**
   * Render markers and landing ellipses for the current body.
   */
  _render() {
    this.markerGroup.clearLayers();

    const filtered = this.sites.filter(s => s.body === this.currentBody);
    if (filtered.length === 0) return;

    // Agency colors
    const agencyColors = {
      'NASA': '#4dabf7',
      'ESA': '#ffd43b',
      'ESA/UK': '#ffd43b',
      'CNSA': '#ff6b6b',
      'ISRO': '#ff922b',
      'JAXA': '#a9e34b',
      'Roscosmos': '#da77f2'
    };

    filtered.forEach(site => {
      const color = agencyColors[site.agency] || '#aaa';

      // 1. Landing Error / Dispersion Ellipse
      if (this.showEllipses && site.ellipse) {
        const coords = computeEllipsePolygon(
          site.lat,
          site.lon,
          site.ellipse.aKm,
          site.ellipse.bKm,
          site.ellipse.azimuthDeg || 0,
          site.body || this.currentBody
        );

        const ellipsePolygon = L.polygon(coords, {
          color: color,
          weight: 1.5,
          opacity: 0.8,
          fillColor: color,
          fillOpacity: 0.15,
          dashArray: '4,4',
          className: 'landing-ellipse'
        });

        ellipsePolygon.bindTooltip(`${site.name} Landing Ellipse (${site.ellipse.aKm * 2} × ${site.ellipse.bKm * 2} km)`, {
          sticky: true
        });

        this.markerGroup.addLayer(ellipsePolygon);
      }

      // 2. Landing Site Marker
      const icon = L.divIcon({
        className: 'landing-site-marker',
        html: `<div class="landing-marker-dot" style="background:${color}; box-shadow: 0 0 6px ${color}80"></div>
               <div class="landing-marker-label">${site.name}</div>`,
        iconSize: [120, 30],
        iconAnchor: [8, 8]
      });

      const marker = L.marker([site.lat, site.lon], { icon });
      const ellipseInfo = site.ellipse ? `<div style="font-size:11px; color:#38bdf8; margin-top:4px;"><b>Landing Ellipse:</b> ${site.ellipse.aKm * 2} × ${site.ellipse.bKm * 2} km (${site.ellipse.azimuthDeg || 0}° az)</div>` : '';

      marker.bindPopup(`
        <div class="landing-popup">
          <h3 style="margin:0 0 6px; color:${color}">${site.name}</h3>
          <div style="font-size:12px; color:#ccc; margin-bottom:6px">
            <strong>${site.agency}</strong> | ${site.year}
          </div>
          <p style="margin:0; font-size:12px; color:#bbb; line-height:1.4">${site.description}</p>
          ${ellipseInfo}
          <div style="margin-top:8px; font-size:11px; color:#888">
            ${site.lat.toFixed(3)}\u00b0, ${site.lon.toFixed(3)}\u00b0
          </div>
        </div>
      `, {
        className: 'landing-popup-container',
        maxWidth: 280
      });

      this.markerGroup.addLayer(marker);
    });
  }

  // --- Planetary Entry, Descent & Landing (EDL) Solvers ---

  /**
   * Calculate spacecraft ballistic coefficient (beta).
   * @param {number} massKg - Spacecraft entry mass in kg
   * @param {number} [dragCoeff=1.4] - Hypersonic drag coefficient (Cd)
   * @param {number} [crossSectionAreaM2=15.9] - Aeroshell frontal area (pi * r^2)
   * @returns {number} Ballistic coefficient (kg/m^2)
   */
  static computeBallisticCoefficient(massKg, dragCoeff = 1.4, crossSectionAreaM2 = 15.9) {
    const cdA = Math.max(0.01, dragCoeff * crossSectionAreaM2);
    return massKg / cdA;
  }

  /**
   * Calculate hypersonic dynamic pressure (q).
   * @param {number} densityKgM3 - Atmospheric mass density (kg/m^3)
   * @param {number} velocityMs - Entry velocity in m/s
   * @returns {number} Dynamic pressure in Pascals (N/m^2)
   */
  static computeDynamicPressure(densityKgM3, velocityMs) {
    return 0.5 * Math.max(0, densityKgM3) * (velocityMs * velocityMs);
  }

  /**
   * Find the nearest landing site from arbitrary planetary coordinates.
   * @param {number} lat - Latitude in degrees
   * @param {number} lon - Longitude in degrees
   * @param {Array<object>} sites - Array of landing site objects
   * @param {string} [body='mars'] - Target body
   * @returns {object|null} Nearest landing site with distance in km
   */
  static findNearestLandingSite(lat, lon, sites = [], body = 'mars') {
    const filtered = sites.filter(s => (s.body || 'mars').toLowerCase() === body.toLowerCase());
    if (filtered.length === 0) return null;

    let nearest = null;
    let minDistance = Infinity;

    filtered.forEach(s => {
      // Haversine distance
      const R = body.toLowerCase() === 'moon' ? 1737.4 : 3389.5;
      const dLat = (s.lat - lat) * Math.PI / 180;
      const dLon = (s.lon - lon) * Math.PI / 180;
      const a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(lat * Math.PI / 180) * Math.cos(s.lat * Math.PI / 180) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2);
      const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(Math.max(0, 1 - a)));
      const dist = R * c;

      if (dist < minDistance) {
        minDistance = dist;
        nearest = { ...s, distanceKm: parseFloat(dist.toFixed(2)) };
      }
    });

    return nearest;
  }

  // --- Entry Deceleration, Parachute Dynamics & Site Filters ---

  /**
   * Calculate peak atmospheric entry deceleration (g-load) using analytical Chapman approximation.
   * a_max = (v_entry^2 * sin(gamma)) / (2 * e * H)
   * @param {number} entryVelocityMs - Atmospheric entry velocity in m/s (e.g. 5800 m/s for Mars)
   * @param {number} flightPathAngleDeg - Entry flight path angle in degrees (e.g. 12 deg)
   * @param {number} [scaleHeightMeters=11100] - Atmospheric scale height (H)
   * @returns {{peakDecelM_S2: number, peakDecelGLoad: number}}
   */
  static computePeakDecelerationG(entryVelocityMs, flightPathAngleDeg, scaleHeightMeters = 11100) {
    const gammaRad = Math.abs(flightPathAngleDeg) * Math.PI / 180.0;
    const aMax = (Math.pow(entryVelocityMs, 2) * Math.sin(gammaRad)) / (2.0 * Math.E * scaleHeightMeters);
    const gLoad = aMax / 9.80665;

    return {
      peakDecelM_S2: parseFloat(aMax.toFixed(1)),
      peakDecelGLoad: parseFloat(gLoad.toFixed(2))
    };
  }

  /**
   * Calculate parachute terminal descent equilibrium velocity.
   * v_term = sqrt((2 * m * g) / (rho * Cd * A))
   * @param {number} massKg - Descent stage mass in kg (e.g. 1025 kg for Perseverance/Curiosity)
   * @param {number} [surfaceDensityKgM3=0.015] - Atmospheric density at parachute deployment
   * @param {number} [parachuteAreaM2=360] - Parachute canopy reference area (e.g. 21.5 m disk-gap-band)
   * @param {number} [parachuteCd=2.0] - Parachute drag coefficient
   * @param {number} [gMars=3.72076] - Mars gravitational acceleration
   * @returns {number} Terminal descent speed in m/s
   */
  static computeTerminalDescentVelocity(massKg, surfaceDensityKgM3 = 0.015, parachuteAreaM2 = 360, parachuteCd = 2.0, gMars = 3.72076) {
    const denom = surfaceDensityKgM3 * parachuteCd * parachuteAreaM2;
    if (denom <= 0) return Infinity;
    const vTerm = Math.sqrt((2.0 * massKg * gMars) / denom);
    return parseFloat(vTerm.toFixed(2));
  }

  /**
   * Filter landing sites by space agency, body, and operational status.
   * @param {Array<object>} sites
   * @param {{agency?: string, body?: string, minYear?: number, maxYear?: number}} [filters={}]
   * @returns {Array<object>}
   */
  static filterSitesByAgencyOrYear(sites = [], filters = {}) {
    return sites.filter(s => {
      if (filters.body && (s.body || 'mars').toLowerCase() !== filters.body.toLowerCase()) return false;
      if (filters.agency && s.agency && !s.agency.toUpperCase().includes(filters.agency.toUpperCase())) return false;
      if (filters.minYear && s.year && s.year < filters.minYear) return false;
      if (filters.maxYear && s.year && s.year > filters.maxYear) return false;
      return true;
    });
  }

  // --- Aerodynamic Stagnation Heat Flux & Dispersion Ellipse Solvers ---

  /**
   * Calculate Sutton-Graves stagnation point convective heat flux in Martian CO2 atmosphere.
   * q_stag = k * sqrt(rho / Rn) * v^3
   * @param {number} entryVelocityMs - Entry speed in m/s (e.g. 5800 m/s)
   * @param {number} densityKgM3 - Atmospheric density at peak heating in kg/m^3 (e.g. 1.5e-4 kg/m^3)
   * @param {number} [noseRadiusMeters=0.6] - Aeroshell spherical nose radius in meters (e.g. 0.6 m for Mars 2020)
   * @returns {{heatFluxW_M2: number, heatFluxW_Cm2: number}}
   */
  static computeStagnationPointHeatFlux(entryVelocityMs, densityKgM3, noseRadiusMeters = 0.6) {
    const kMars = 1.898e-4; // Sutton-Graves constant for pure CO2 (kg^0.5 / m)
    const rn = Math.max(0.01, noseRadiusMeters);
    const rho = Math.max(0, densityKgM3);

    const qFluxW_M2 = kMars * Math.sqrt(rho / rn) * Math.pow(entryVelocityMs, 3);
    const qFluxW_Cm2 = qFluxW_M2 / 10000.0; // 1 W/cm^2 = 10,000 W/m^2

    return {
      heatFluxW_M2: parseFloat(qFluxW_M2.toFixed(1)),
      heatFluxW_Cm2: parseFloat(qFluxW_Cm2.toFixed(2))
    };
  }

  /**
   * Calculate surface footprint area of a landing dispersion ellipse.
   * A = pi * a * b
   * @param {number} aKm - Semi-major axis in km
   * @param {number} bKm - Semi-minor axis in km
   * @returns {{areaKm2: number, perimeterKm: number}}
   */
  static computeEllipseSurfaceArea(aKm, bKm) {
    const a = Math.max(0, aKm);
    const b = Math.max(0, bKm);
    const area = Math.PI * a * b;

    // Ramanujan's perimeter approximation: pi * [3(a+b) - sqrt((3a+b)(a+3b))]
    const perimeter = Math.PI * (3 * (a + b) - Math.sqrt((3 * a + b) * (a + 3 * b)));

    return {
      areaKm2: parseFloat(area.toFixed(2)),
      perimeterKm: parseFloat(perimeter.toFixed(2))
    };
  }

  /**
   * Check if a geographic coordinate is within a landing dispersion ellipse.
   * @param {number} lat - Target point latitude
   * @param {number} lon - Target point longitude
   * @param {number} centerLat - Ellipse center latitude
   * @param {number} centerLon - Ellipse center longitude
   * @param {number} aKm - Semi-major axis in km
   * @param {number} bKm - Semi-minor axis in km
   * @param {number} [azimuthDeg=0] - Ellipse major axis azimuth in degrees from North
   * @returns {boolean} True if point lies inside ellipse
   */
  static isPointInsideEllipse(lat, lon, centerLat, centerLon, aKm, bKm, azimuthDeg = 0) {
    const R_MARS = 3389.5;
    const dLatKm = (lat - centerLat) * (Math.PI / 180.0) * R_MARS;
    const dLonKm = (lon - centerLon) * (Math.PI / 180.0) * R_MARS * Math.cos(centerLat * Math.PI / 180.0);

    // Rotate into ellipse coordinate system
    const thetaRad = -azimuthDeg * Math.PI / 180.0;
    const xRot = dLonKm * Math.cos(thetaRad) - dLatKm * Math.sin(thetaRad);
    const yRot = dLonKm * Math.sin(thetaRad) + dLatKm * Math.cos(thetaRad);

    const normDistSq = Math.pow(xRot / Math.max(0.01, bKm), 2) + Math.pow(yRot / Math.max(0.01, aKm), 2);
    return normDistSq <= 1.0;
  }
}



