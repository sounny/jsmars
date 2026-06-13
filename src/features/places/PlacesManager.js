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
   * Supports formats: "lat, lon", "lat lon", decimal degrees.
   * @param {string} query
   * @returns {object|null} - { lat, lon } or null
   */
  parseCoordinates(query) {
    // Pattern: "lat, lon" or "lat lon" with optional degree symbols
    const cleaned = query.replace(/[°'"NSEW]/gi, ' ').trim();
    const match = cleaned.match(/^(-?\d+\.?\d*)[,\s]+(-?\d+\.?\d*)$/);
    if (match) {
      const lat = parseFloat(match[1]);
      const lon = parseFloat(match[2]);
      if (lat >= -90 && lat <= 90 && lon >= -360 && lon <= 360) {
        return { lat, lon, name: `${lat.toFixed(4)}, ${lon.toFixed(4)}` };
      }
    }
    return null;
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
