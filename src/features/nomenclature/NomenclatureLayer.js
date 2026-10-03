import { EVENTS } from '../../constants.js';
import { BODIES, to180 } from '../../util/geo.js';
import { jmarsState } from '../../jmars-state.js';

/**
 * @class NomenclatureLayer
 * @description Renders IAU named surface features (craters, mountains, canyons, plains, etc.)
 * with category styling, search filter, and interactive detail popups.
 */
export class NomenclatureLayer {
  /**
   * @param {L.Map} map - The Leaflet map instance.
   */
  constructor(map) {
    this.map = map;
    this.layerGroup = L.layerGroup();
    this.landmarks = [];
    this.currentBody = (jmarsState.get('body') || 'mars').toLowerCase();
    this.searchQuery = '';

    /** @type {Object<string, boolean>} Visibility state for each feature type. */
    this.visibleTypes = {
      'Crater': true,
      'Mons': true,
      'Valles': true,
      'Planitia': true,
      'Chaos': true,
      'Chasma': true,
      'Planum': true,
      'Fossa': true,
      'Linea': true,
      'Macula': true,
      'Corona': true,
      'Other': true
    };
    this.isActive = false;

    // Color theme per IAU category
    this.typeColors = {
      'Mons': '#fbbf24',       // Amber gold
      'Crater': '#38bdf8',     // Sky blue
      'Valles': '#2dd4bf',     // Teal
      'Chasma': '#34d399',     // Emerald
      'Planitia': '#a3e635',   // Lime
      'Planum': '#c084fc',     // Purple
      'Chaos': '#f43f5e',      // Rose
      'Fossa': '#fb923c',      // Orange
      'Linea': '#06b6d4',      // Cyan
      'Macula': '#f59e0b',     // Warm Amber
      'Corona': '#ec4899',     // Pink
      'Other': '#e2e8f0'       // Slate white
    };

    document.addEventListener(EVENTS.BODY_CHANGED, (e) => {
      const body = e?.detail?.body?.toLowerCase();
      if (body) {
        this.currentBody = body;
        if (this.isActive) this.render();
      }
    });
  }

  /**
   * Load landmark data from the local JSON file.
   * @returns {Promise<void>}
   */
  async load() {
    try {
      const response = await fetch('./src/data/landmarks.json');
      if (!response.ok) {
        throw new Error(`Failed to fetch landmarks: ${response.statusText}`);
      }
      this.landmarks = await response.json();
      if (this.isActive) this.render();
    } catch (e) {
      console.error('Failed to load landmarks', e);
    }
  }

  toggleType(type, isVisible) {
    this.visibleTypes[type] = isVisible;
    this.render();
  }

  setSearchQuery(query) {
    this.searchQuery = (query || '').trim().toLowerCase();
    if (!this.searchQuery) this._lastFlyKey = '';
    this.render();
  }

  /**
   * A feature matches when the query is a substring of its name, type, or origin,
   * or when every word of at least 3 letters appears somewhere in those fields.
   * @param {object} feature
   * @param {string} query
   * @returns {boolean}
   */
  featureMatchesQuery(feature, query) {
    const name = (feature.name || '').toLowerCase();
    const type = (feature.type || '').toLowerCase();
    const origin = (feature.origin || '').toLowerCase();
    const blob = `${name} ${type} ${origin}`;
    if (blob.includes(query)) return true;
    const tokens = query.split(/\s+/).filter(token => token.length >= 3);
    return tokens.length > 1 && tokens.every(token => blob.includes(token));
  }

  /**
   * Fly only for an exact name, or when a query of 3+ characters leaves one feature.
   * A broad query such as "Mons" keeps filtering and does not move the map.
   * @param {Array<object>} filtered
   * @returns {object|null}
   */
  pickFlyTarget(filtered) {
    const query = this.searchQuery;
    if (!query || query.length < 3 || !filtered.length) return null;
    const exact = filtered.filter(feature => (feature.name || '').toLowerCase() === query);
    if (exact.length === 1) return exact[0];
    if (filtered.length === 1) return filtered[0];
    return null;
  }

  /**
   * Zoom so the feature spans roughly 220 px, staying between 3 and 7.
   * @param {object} feature
   * @returns {number}
   */
  zoomForFeature(feature) {
    const body = (feature.body || this.currentBody || 'mars').toLowerCase();
    const radius = BODIES[body]?.meanRadius || BODIES.mars.meanRadius;
    const diameterKm = Math.max(feature.diameterKm || 20, 5);
    const degrees = diameterKm / (Math.PI * radius / 180);
    const zoom = Math.log2((220 / degrees) * (360 / 256));
    return Math.max(3, Math.min(7, Math.round(zoom)));
  }

  /**
   * Center the map on a search hit and open its popup. Repeated renders of the
   * same hit do not fly again.
   * @param {object|null} feature
   * @param {L.Marker|null} marker
   */
  flyToFeature(feature, marker) {
    if (!feature || !this.map) return;
    const key = `${feature.body || this.currentBody}|${feature.name}|${feature.lat}|${feature.lon}`;
    if (this._lastFlyKey === key) return;
    this._lastFlyKey = key;
    const lon = to180(feature.lon);
    const zoom = this.zoomForFeature(feature);
    this.map.flyTo([feature.lat, lon], zoom, { duration: 0.6 });
    if (marker) {
      const open = () => marker.openPopup();
      this.map.once('moveend', open);
      window.setTimeout(() => {
        if (!marker.isPopupOpen()) marker.openPopup();
      }, 800);
    }
  }

  toggle(isActive) {
    this.isActive = isActive;
    if (isActive) {
      this.map.addLayer(this.layerGroup);
      if (this.landmarks.length === 0) {
        this.load();
      } else {
        this.render();
      }
    } else {
      this.map.removeLayer(this.layerGroup);
    }
  }

  render() {
    this.layerGroup.clearLayers();
    if (!this.isActive) return;

    const filtered = this.landmarks.filter(l => {
      // Body filter
      const b = (l.body || 'mars').toLowerCase();
      if (b !== this.currentBody) return false;

      // Type filter
      const type = this.visibleTypes[l.type] !== undefined ? l.type : 'Other';
      if (!this.visibleTypes[type]) return false;

      if (this.searchQuery && !this.featureMatchesQuery(l, this.searchQuery)) return false;

      return true;
    });

    const flyTarget = this.pickFlyTarget(filtered);
    let flyMarker = null;

    filtered.forEach(l => {
      const lon = to180(l.lon);
      const color = this.typeColors[l.type] || this.typeColors['Other'];

      const icon = L.divIcon({
        className: 'nomenclature-label-container',
        html: `<div style="display:flex; align-items:center; gap:4px; transform:translate(-50%, -50%); cursor:pointer;">
                 <div style="width:6px; height:6px; border-radius:50%; background:${color}; box-shadow:0 0 4px ${color};"></div>
                 <span style="font-size:11px; font-weight:bold; color:${color}; text-shadow:0 1px 3px rgba(0,0,0,0.9); white-space:nowrap; pointer-events:auto;">${l.name}</span>
               </div>`,
        iconSize: [1, 1],
        iconAnchor: [0, 0]
      });

      const marker = L.marker([l.lat, lon], { icon: icon });

      const diamStr = l.diameterKm ? `<div><b>Diameter:</b> ${l.diameterKm} km</div>` : '';
      const originStr = l.origin ? `<div style="margin-top:4px; font-size:11px; color:#94a3b8; font-style:italic;">${l.origin}</div>` : '';

      marker.bindPopup(`
        <div style="padding:4px; font-family:sans-serif; color:#f8fafc;">
          <h3 style="margin:0 0 4px; font-size:14px; color:${color};">${l.name}</h3>
          <div style="font-size:11px; color:#cbd5e1; margin-bottom:4px;">
            <b>Type:</b> ${l.type || 'Feature'}
          </div>
          ${diamStr}
          <div style="font-size:11px; color:#94a3b8; margin-top:2px;">
            <b>Coordinates:</b> ${l.lat.toFixed(2)}°, ${lon.toFixed(2)}°
          </div>
          ${originStr}
        </div>
      `, {
        className: 'nomenclature-popup',
        maxWidth: 260
      });

      this.layerGroup.addLayer(marker);
      if (flyTarget && l === flyTarget) flyMarker = marker;
    });

    this.flyToFeature(flyTarget, flyMarker);
  }
}
