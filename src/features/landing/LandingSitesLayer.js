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
}

