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
}
