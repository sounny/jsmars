import { jmarsState } from '../../jmars-state.js';
import { EVENTS } from '../../constants.js';

/**
 * @module BookmarksTool
 * @description Provides interactive management, planetary body switching,
 * built-in scientific POIs, and import/export of spatial bookmarks.
 */
export class BookmarksTool {
  static DEFAULT_POIS = [
    { id: 'poi-olympus', name: '🌋 Olympus Mons Summit', lat: 18.65, lng: -133.8, zoom: 6, body: 'mars' },
    { id: 'poi-jezero', name: '🚀 Jezero Crater Delta (Perseverance)', lat: 18.38, lng: 77.58, zoom: 9, body: 'mars' },
    { id: 'poi-valles', name: '🏜️ Valles Marineris (Melas Chasma)', lat: -9.8, lng: -76.4, zoom: 6, body: 'mars' },
    { id: 'poi-gale', name: '🔬 Gale Crater & Mt Sharp (Curiosity)', lat: -5.4, lng: 137.8, zoom: 8, body: 'mars' },
    { id: 'poi-korolev', name: '❄️ Korolev Water Ice Crater', lat: 72.77, lng: 164.58, zoom: 7, body: 'mars' },
    { id: 'poi-boreum', name: '🧊 Planum Boreum North Pole Cap', lat: 86.0, lng: 0.0, zoom: 5, body: 'mars' },
    { id: 'poi-apollo11', name: '🌕 Apollo 11 Tranquility Base', lat: 0.674, lng: 23.473, zoom: 8, body: 'moon' },
    { id: 'poi-tycho', name: '💥 Tycho Crater Peak & Rays', lat: -43.31, lng: -11.36, zoom: 7, body: 'moon' },
    { id: 'poi-shackleton', name: '❄️ Shackleton South Pole Ice', lat: -89.67, lng: 129.78, zoom: 7, body: 'moon' }
  ];

  /**
   * @param {L.Map} map - Leaflet map instance
   * @param {string|HTMLElement} containerOrId - DOM element ID or container
   */
  constructor(map, containerOrId) {
    this.map = map;
    this.container = typeof containerOrId === 'string'
      ? document.getElementById(containerOrId)
      : containerOrId;
    this.bookmarks = [];
    this.currentBody = (jmarsState.get('body') || 'mars').toLowerCase();

    if (this.container) {
      this.init();
    }

    document.addEventListener(EVENTS.BODY_CHANGED, (e) => {
      this.currentBody = (e?.detail?.body || 'mars').toLowerCase();
      this.render();
    });
  }

  init() {
    this.loadFromStorage();
    if (this.bookmarks.length === 0) {
      this.bookmarks = [...BookmarksTool.DEFAULT_POIS];
      this.saveToStorage();
    }
    this.render();
  }

  loadFromStorage() {
    const stored = localStorage.getItem('jmars_bookmarks');
    if (stored) {
      try {
        this.bookmarks = JSON.parse(stored);
      } catch (e) {
        console.error('Failed to parse bookmarks', e);
        this.bookmarks = [];
      }
    }
  }

  saveToStorage() {
    localStorage.setItem('jmars_bookmarks', JSON.stringify(this.bookmarks));
  }

  addCurrentView() {
    const center = this.map.getCenter();
    const zoom = this.map.getZoom();
    const name = prompt('Enter a name for this bookmark:', `ROI ${this.bookmarks.length + 1}`);

    if (name) {
      this.bookmarks.push({
        id: crypto.randomUUID?.() || `${Date.now()}-${Math.random().toString(36).substring(2, 9)}`,
        name: name,
        lat: center.lat,
        lng: center.lng,
        zoom: zoom,
        body: this.currentBody
      });
      this.saveToStorage();
      this.render();
    }
  }

  goTo(bookmark) {
    if (bookmark.body && bookmark.body.toLowerCase() !== this.currentBody) {
      jmarsState.set('body', bookmark.body);
    }
    this.map.setView([bookmark.lat, bookmark.lng], bookmark.zoom);
  }

  remove(id) {
    if (confirm('Delete this bookmark?')) {
      this.bookmarks = this.bookmarks.filter(b => b.id !== id);
      this.saveToStorage();
      this.render();
    }
  }

  exportJSON() {
    const blob = new Blob([JSON.stringify(this.bookmarks, null, 2)], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `jmars_bookmarks_${Date.now()}.json`;
    a.click();
    URL.revokeObjectURL(url);
  }

  render() {
    if (!this.container) return;
    this.container.innerHTML = '';

    const wrapper = document.createElement('div');
    wrapper.className = 'bookmarks-block';
    wrapper.style.display = 'flex';
    wrapper.style.flexDirection = 'column';
    wrapper.style.gap = '8px';
    wrapper.style.padding = '8px';

    const btnRow = document.createElement('div');
    btnRow.style.display = 'flex';
    btnRow.style.gap = '4px';

    const addBtn = document.createElement('button');
    addBtn.className = 'tool-btn';
    addBtn.textContent = '+ Save View';
    addBtn.style.flex = '1';
    addBtn.type = 'button';
    addBtn.onclick = () => this.addCurrentView();
    btnRow.appendChild(addBtn);

    const exportBtn = document.createElement('button');
    exportBtn.className = 'tool-btn';
    exportBtn.textContent = 'Export JSON';
    exportBtn.style.flex = '1';
    exportBtn.type = 'button';
    exportBtn.onclick = () => this.exportJSON();
    btnRow.appendChild(exportBtn);

    wrapper.appendChild(btnRow);

    const list = document.createElement('div');
    list.style.maxHeight = '180px';
    list.style.overflowY = 'auto';
    list.style.background = '#0f172a';
    list.style.border = '1px solid #1e293b';
    list.style.borderRadius = '4px';

    this.bookmarks.forEach(b => {
      const item = document.createElement('div');
      item.style.display = 'flex';
      item.style.justifyContent = 'space-between';
      item.style.alignItems = 'center';
      item.style.padding = '6px 8px';
      item.style.borderBottom = '1px solid #1e293b';
      item.style.fontSize = '11px';
      item.style.cursor = 'pointer';

      const bodyBadge = b.body ? `<span style="font-size:9px; color:#38bdf8; background:#1e293b; padding:1px 4px; border-radius:2px; margin-right:4px;">${b.body.toUpperCase()}</span>` : '';

      const link = document.createElement('div');
      link.innerHTML = `${bodyBadge}<span style="color:#f8fafc;">${b.name}</span>`;
      link.style.flex = '1';
      link.onclick = () => this.goTo(b);

      const delBtn = document.createElement('span');
      delBtn.innerHTML = '&times;';
      delBtn.style.color = '#f43f5e';
      delBtn.style.fontSize = '14px';
      delBtn.style.cursor = 'pointer';
      delBtn.style.padding = '0 4px';
      delBtn.title = 'Delete Bookmark';
      delBtn.onclick = (e) => {
        e.stopPropagation();
        this.remove(b.id);
      };

      item.appendChild(link);
      item.appendChild(delBtn);
      list.appendChild(item);
    });

    if (this.bookmarks.length === 0) {
      const empty = document.createElement('div');
      empty.textContent = 'No bookmarks saved.';
      empty.style.color = '#94a3b8';
      empty.style.fontStyle = 'italic';
      empty.style.padding = '8px';
      list.appendChild(empty);
    }

    wrapper.appendChild(list);
    this.container.appendChild(wrapper);
  }

  getData() {
    return this.bookmarks;
  }

  loadData(data) {
    if (Array.isArray(data)) {
      this.bookmarks = data;
      this.saveToStorage();
      this.render();
    }
  }

  // --- GIS Serialization & Spatial Analysis ---

  /**
   * Convert bookmarks array to a GeoJSON FeatureCollection.
   * @param {Array<object>} [bookmarks] - Bookmarks list (defaults to instance bookmarks)
   * @returns {object} GeoJSON FeatureCollection
   */
  static exportGeoJSON(bookmarks = []) {
    return {
      type: 'FeatureCollection',
      features: bookmarks.map(b => ({
        type: 'Feature',
        id: b.id,
        geometry: {
          type: 'Point',
          coordinates: [b.lng, b.lat]
        },
        properties: {
          name: b.name,
          zoom: b.zoom,
          body: b.body || 'mars'
        }
      }))
    };
  }

  /**
   * Parse a GeoJSON FeatureCollection into bookmarks array.
   * @param {object} geojson - GeoJSON FeatureCollection or Feature
   * @returns {Array<object>} Array of bookmark objects
   */
  static parseGeoJSON(geojson) {
    if (!geojson) return [];
    const features = geojson.type === 'FeatureCollection' ? (geojson.features || []) : [geojson];

    return features
      .filter(f => f?.geometry?.type === 'Point' && Array.isArray(f.geometry.coordinates))
      .map(f => ({
        id: f.id || crypto.randomUUID?.() || `${Date.now()}-${Math.random().toString(36).substring(2, 7)}`,
        name: f.properties?.name || 'Imported ROI',
        lat: f.geometry.coordinates[1],
        lng: f.geometry.coordinates[0],
        zoom: f.properties?.zoom || 6,
        body: f.properties?.body || 'mars'
      }));
  }

  /**
   * Compute geographic bounding box [minLat, minLon, maxLat, maxLon] for a set of bookmarks.
   * @param {Array<object>} bookmarks
   * @returns {{minLat: number, minLng: number, maxLat: number, maxLng: number, centerLat: number, centerLng: number}}
   */
  static computeBoundingBox(bookmarks = []) {
    if (!bookmarks || bookmarks.length === 0) {
      return { minLat: 0, minLng: 0, maxLat: 0, maxLng: 0, centerLat: 0, centerLng: 0 };
    }

    const lats = bookmarks.map(b => b.lat);
    const lngs = bookmarks.map(b => b.lng);

    const minLat = Math.min(...lats);
    const maxLat = Math.max(...lats);
    const minLng = Math.min(...lngs);
    const maxLng = Math.max(...lngs);

    return {
      minLat,
      minLng,
      maxLat,
      maxLng,
      centerLat: (minLat + maxLat) / 2.0,
      centerLng: (minLng + maxLng) / 2.0
    };
  }
}

