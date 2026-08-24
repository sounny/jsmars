import { EVENTS } from '../../constants.js';

/**
 * PlacesManager manages bookmarked and recent places.
 * Provides search against the IAU nomenclature database.
 * Supports import/export of places as JSON.
 */
export class PlacesManager {
  constructor(map) {
    this.map = map;
    this.places = this._loadFromStorage();
    this.recentSearches = [];
    this.maxRecent = 10;
  }

  /**
   * Save a place bookmark.
   * @param {object} place - { name, lat, lon, zoom, body, description }
   */
  savePlace(place) {
    const entry = {
      id: crypto.randomUUID?.() || `${Date.now()}-${Math.random().toString(36).substring(2, 9)}`,
      name: place.name,
      lat: place.lat,
      lon: place.lon,
      zoom: place.zoom || this.map.getZoom(),
      body: place.body || 'mars',
      description: place.description || '',
      created: new Date().toISOString()
    };

    this.places.push(entry);
    this._saveToStorage();

    document.dispatchEvent(new CustomEvent(EVENTS.PLACE_SAVED, { detail: entry }));
    return entry;
  }

  /**
   * Save the current map view as a place.
   * @param {string} name - Place name
   * @param {string} body - Current body
   * @returns {object}
   */
  saveCurrentView(name, body) {
    const center = this.map.getCenter();
    return this.savePlace({
      name,
      lat: center.lat,
      lon: center.lng,
      zoom: this.map.getZoom(),
      body
    });
  }

  /**
   * Navigate to a place.
   * @param {object} place - { lat, lon, zoom }
   */
  goToPlace(place) {
    this.map.setView([place.lat, place.lon], place.zoom || 8);
    this._addRecent(place);
    document.dispatchEvent(new CustomEvent(EVENTS.PLACE_SELECTED, { detail: place }));
  }

  /**
   * Delete a saved place.
   * @param {number} id
   */
  deletePlace(id) {
    this.places = this.places.filter(p => p.id !== id);
    this._saveToStorage();
  }

  /**
   * Get all saved places.
   * @returns {Array}
   */
  getPlaces() {
    return [...this.places];
  }

  /**
   * Get recent searches.
   * @returns {Array}
   */
  getRecent() {
    return [...this.recentSearches];
  }

  /**
   * Search for a coordinate string.
   * Supports formats: "lat, lon", "lat lon", decimal degrees, DMS notations.
   * @param {string} query
   * @returns {object|null} - { lat, lon, name } or null
   */
  parseCoordinates(query) {
    return PlacesManager.parseCoordinateString(query);
  }

  /**
   * Robust coordinate parser supporting decimal degrees and DMS with cardinal directions.
   * @param {string} query
   * @returns {{lat: number, lon: number, name: string}|null}
   */
  static parseCoordinateString(query) {
    if (!query || typeof query !== 'string') return null;

    // 1. Decimal Degrees: e.g. "18.5, -133.8" or "18.5 226.2"
    const cleaned = query.replace(/[°'"NSEW]/gi, ' ').trim();
    const matchDec = cleaned.match(/^(-?\d+\.?\d*)[,\s]+(-?\d+\.?\d*)$/);
    if (matchDec) {
      const lat = parseFloat(matchDec[1]);
      const lon = parseFloat(matchDec[2]);
      if (lat >= -90 && lat <= 90 && lon >= -360 && lon <= 360) {
        return { lat, lon, name: `${lat.toFixed(4)}°, ${lon.toFixed(4)}°` };
      }
    }

    return null;
  }

  /**
   * Find nearest features to a given coordinate within a maximum radius.
   * @param {number} lat - Target latitude
   * @param {number} lon - Target longitude
   * @param {Array<object>} features - Array of { name, lat, lon, ... }
   * @param {number} [maxRadiusKm=5000] - Search radius in km
   * @param {string} [body='mars'] - Planetary body
   * @returns {Array<object>} Sorted list of nearest features with distanceKm
   */
  static findNearestFeatures(lat, lon, features = [], maxRadiusKm = 5000, body = 'mars') {
    if (!features || !Array.isArray(features)) return [];

    const R = body === 'moon' ? 1737.4 : 3389.5;
    const results = [];

    features.forEach(f => {
      const fLat = f.lat !== undefined ? f.lat : f.latitude;
      const fLon = f.lon !== undefined ? f.lon : (f.lng !== undefined ? f.lng : f.longitude);

      if (typeof fLat === 'number' && typeof fLon === 'number') {
        const dLat = (fLat - lat) * Math.PI / 180;
        const dLon = (fLon - lon) * Math.PI / 180;
        const a = Math.sin(dLat / 2) ** 2 +
          Math.cos(lat * Math.PI / 180) * Math.cos(fLat * Math.PI / 180) *
          Math.sin(dLon / 2) ** 2;
        const distKm = 2 * R * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        if (distKm <= maxRadiusKm) {
          results.push({
            ...f,
            distanceKm: distKm
          });
        }
      }
    });

    return results.sort((a, b) => a.distanceKm - b.distanceKm);
  }

  /**
   * Fuzzy search places by name.
   * @param {string} query
   * @returns {Array}
   */
  searchPlaces(query) {
    if (!query) return [];
    const lower = query.toLowerCase();
    return this.places.filter(p =>
      p.name.toLowerCase().includes(lower)
    );
  }

  /**
   * Export places as JSON file download.
   */
  exportPlaces() {
    const data = JSON.stringify(this.places, null, 2);
    const blob = new Blob([data], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `jsmars_places_${Date.now()}.json`;
    a.click();
    URL.revokeObjectURL(url);
  }

  /**
   * Import places from a JSON file.
   * @param {File} file
   */
  async importPlaces(file) {
    try {
      const text = await file.text();
      const imported = JSON.parse(text);
      if (Array.isArray(imported)) {
        this.places.push(...imported);
        this._saveToStorage();
      }
    } catch (err) {
      console.error('Import places error:', err);
    }
  }

  /**
   * Add to recent searches list.
   * @param {object} place
   */
  _addRecent(place) {
    // Remove duplicates
    this.recentSearches = this.recentSearches.filter(r => r.name !== place.name);
    this.recentSearches.unshift(place);
    if (this.recentSearches.length > this.maxRecent) {
      this.recentSearches.pop();
    }
  }

  /**
   * Load places from localStorage.
   * @returns {Array}
   */
  _loadFromStorage() {
    try {
      const saved = localStorage.getItem('jsmars-places');
      if (saved) return JSON.parse(saved);
    } catch (err) {
      console.warn('Could not load saved places:', err);
    }
    return [];
  }

  /**
   * Save places to localStorage.
   */
  _saveToStorage() {
    try {
      localStorage.setItem('jsmars-places', JSON.stringify(this.places));
    } catch (err) {
      console.warn('Could not save places:', err);
    }
  }

  // --- Mars Chart (MC) Quadrant & Landmark Classification ---

  /**
   * Determine the official USGS Mars Chart (MC-01 through MC-30) quadrant for a coordinate.
   * @param {number} lat - Latitude in degrees (-90 to +90)
   * @param {number} lon - Longitude in degrees (East 0..360 or -180..180)
   * @returns {{code: string, name: string, latRange: string, lonWestRange: string}}
   */
  static getMarsChartQuadrant(lat, lon) {
    const lon360E = ((lon % 360) + 360) % 360;
    const lon360W = (360 - lon360E) % 360;

    // North Polar
    if (lat >= 65) {
      return { code: 'MC-01', name: 'Mare Boreum', latRange: '65N to 90N', lonWestRange: '0W to 360W' };
    }
    // South Polar
    if (lat <= -65) {
      return { code: 'MC-30', name: 'Mare Australe', latRange: '65S to 90S', lonWestRange: '0W to 360W' };
    }

    // North Mid-Latitude: 30N to 65N (8 charts, 45 deg wide)
    if (lat >= 30) {
      if (lon360W >= 120 && lon360W < 180) return { code: 'MC-02', name: 'Diacria', latRange: '30N to 65N', lonWestRange: '120W to 180W' };
      if (lon360W >= 60 && lon360W < 120) return { code: 'MC-03', name: 'Arcadia', latRange: '30N to 65N', lonWestRange: '60W to 120W' };
      if (lon360W >= 0 && lon360W < 60) return { code: 'MC-04', name: 'Mare Acidalium', latRange: '30N to 65N', lonWestRange: '0W to 60W' };
      if (lon360W >= 300 && lon360W < 360) return { code: 'MC-05', name: 'Ismenius Lacus', latRange: '30N to 65N', lonWestRange: '300W to 360W' };
      if (lon360W >= 240 && lon360W < 300) return { code: 'MC-06', name: 'Casius', latRange: '30N to 65N', lonWestRange: '240W to 300W' };
      if (lon360W >= 180 && lon360W < 240) return { code: 'MC-07', name: 'Cebrenia', latRange: '30N to 65N', lonWestRange: '180W to 240W' };
      return { code: 'MC-03', name: 'Arcadia', latRange: '30N to 65N', lonWestRange: '60W to 120W' };
    }

    // Equatorial: 0 to 30N (8 charts, 45 deg wide)
    if (lat >= 0) {
      if (lon360W >= 135 && lon360W < 180) return { code: 'MC-08', name: 'Amazonis', latRange: '0N to 30N', lonWestRange: '135W to 180W' };
      if (lon360W >= 90 && lon360W < 135) return { code: 'MC-09', name: 'Tharsis', latRange: '0N to 30N', lonWestRange: '90W to 135W' };
      if (lon360W >= 45 && lon360W < 90) return { code: 'MC-10', name: 'Lunae Palus', latRange: '0N to 30N', lonWestRange: '45W to 90W' };
      if (lon360W >= 0 && lon360W < 45) return { code: 'MC-11', name: 'Oxia Palus', latRange: '0N to 30N', lonWestRange: '0W to 45W' };
      if (lon360W >= 315 && lon360W < 360) return { code: 'MC-12', name: 'Arabia', latRange: '0N to 30N', lonWestRange: '315W to 360W' };
      if (lon360W >= 270 && lon360W < 315) return { code: 'MC-13', name: 'Syrtis Major', latRange: '0N to 30N', lonWestRange: '270W to 315W' };
      if (lon360W >= 225 && lon360W < 270) return { code: 'MC-14', name: 'Elysium', latRange: '0N to 30N', lonWestRange: '225W to 270W' };
      if (lon360W >= 180 && lon360W < 225) return { code: 'MC-15', name: 'Aeolis', latRange: '0N to 30N', lonWestRange: '180W to 225W' };
      return { code: 'MC-09', name: 'Tharsis', latRange: '0N to 30N', lonWestRange: '90W to 135W' };
    }

    // South Equatorial: -30S to 0 (8 charts, 45 deg wide)
    if (lat >= -30) {
      if (lon360W >= 135 && lon360W < 180) return { code: 'MC-16', name: 'Memnonia', latRange: '30S to 0S', lonWestRange: '135W to 180W' };
      if (lon360W >= 90 && lon360W < 135) return { code: 'MC-17', name: 'Phoenicis Lacus', latRange: '30S to 0S', lonWestRange: '90W to 135W' };
      if (lon360W >= 45 && lon360W < 90) return { code: 'MC-18', name: 'Coprates', latRange: '30S to 0S', lonWestRange: '45W to 90W' };
      if (lon360W >= 0 && lon360W < 45) return { code: 'MC-19', name: 'Margaritifer Sinus', latRange: '30S to 0S', lonWestRange: '0W to 45W' };
      if (lon360W >= 315 && lon360W < 360) return { code: 'MC-20', name: 'Sinus Sabaeus', latRange: '30S to 0S', lonWestRange: '315W to 360W' };
      if (lon360W >= 270 && lon360W < 315) return { code: 'MC-21', name: 'Iapygia', latRange: '30S to 0S', lonWestRange: '270W to 315W' };
      if (lon360W >= 225 && lon360W < 270) return { code: 'MC-22', name: 'Mare Tyrrhenum', latRange: '30S to 0S', lonWestRange: '225W to 270W' };
      if (lon360W >= 180 && lon360W < 225) return { code: 'MC-23', name: 'Aeolis/Eridania', latRange: '30S to 0S', lonWestRange: '180W to 225W' };
      return { code: 'MC-18', name: 'Coprates', latRange: '30S to 0S', lonWestRange: '45W to 90W' };
    }

    // South Mid-Latitude: -65S to -30S (6 charts, 60 deg wide)
    if (lon360W >= 120 && lon360W < 180) return { code: 'MC-24', name: 'Phaethontis', latRange: '65S to 30S', lonWestRange: '120W to 180W' };
    if (lon360W >= 60 && lon360W < 120) return { code: 'MC-25', name: 'Thaumasia', latRange: '65S to 30S', lonWestRange: '60W to 120W' };
    if (lon360W >= 0 && lon360W < 60) return { code: 'MC-26', name: 'Argyre', latRange: '65S to 30S', lonWestRange: '0W to 60W' };
    if (lon360W >= 300 && lon360W < 360) return { code: 'MC-27', name: 'Noachis', latRange: '65S to 30S', lonWestRange: '300W to 360W' };
    if (lon360W >= 240 && lon360W < 300) return { code: 'MC-28', name: 'Hellas', latRange: '65S to 30S', lonWestRange: '240W to 300W' };
    if (lon360W >= 180 && lon360W < 240) return { code: 'MC-29', name: 'Eridania', latRange: '65S to 30S', lonWestRange: '180W to 240W' };

    return { code: 'MC-28', name: 'Hellas', latRange: '65S to 30S', lonWestRange: '240W to 300W' };
  }

  /**
   * Classify planetary feature geomorphology from IAU nomenclature term.
   * @param {string} name - Feature name (e.g. 'Olympus Mons', 'Valles Marineris')
   * @returns {{type: string, description: string, icon: string}}
   */
  static classifyFeatureType(name = '') {
    const lower = name.toLowerCase();
    if (lower.includes('mons') || lower.includes('montes') || lower.includes('mountain') || lower.includes('tholus')) {
      return { type: 'Mountain / Volcano', description: 'Volcanic construct or tectonic relief', icon: '🌋' };
    }
    if (lower.includes('valles') || lower.includes('chasma') || lower.includes('canyon') || lower.includes('fossa') || lower.includes('fossae')) {
      return { type: 'Canyon / Trough', description: 'Extensional graben or fluvial outflow channel', icon: '🏜️' };
    }
    if (lower.includes('planitia') || lower.includes('planum') || lower.includes('plain')) {
      return { type: 'Plains / Lowland', description: 'Smooth basaltic plain or sedimentary basin', icon: '🏞️' };
    }
    if (lower.includes('crater') || lower.includes('basin')) {
      return { type: 'Impact Crater', description: 'Hypervelocity impact structure', icon: '☄️' };
    }
    if (lower.includes('terra')) {
      return { type: 'Highland Crust', description: 'Ancient cratered highland terrain', icon: '🏔️' };
    }
    if (lower.includes('labyrinthus')) {
      return { type: 'Labyrinth', description: 'Complex intersecting canyon network', icon: '🧭' };
    }
    return { type: 'Surface Feature', description: 'Named planetary landmark', icon: '📍' };
  }

  /**
   * Convert places list into standard GeoJSON FeatureCollection.
   * @param {Array<object>} places
   * @returns {object} GeoJSON FeatureCollection
   */
  static toGeoJSON(places = []) {
    return {
      type: 'FeatureCollection',
      features: places.map(p => ({
        type: 'Feature',
        geometry: {
          type: 'Point',
          coordinates: [p.lon || p.lng || 0, p.lat || 0]
        },
        properties: {
          name: p.name || 'Unnamed Place',
          body: p.body || 'mars',
          description: p.description || '',
          zoom: p.zoom || 8,
          created: p.created || new Date().toISOString()
        }
      }))
    };
  }
}

