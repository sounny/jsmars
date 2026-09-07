import { jmarsState } from '../../jmars-state.js';
import { EVENTS } from '../../constants.js';
import { normalizeBodyKey, switchActiveBody } from '../../util/body.js';

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
  constructor(mapOrController, containerOrId) {
    this.jmarsMap = mapOrController?.map ? mapOrController : null;
    this.map = this.jmarsMap?.map || mapOrController;
    this.container = typeof containerOrId === 'string'
      ? document.getElementById(containerOrId)
      : containerOrId;
    this.bookmarks = [];
    this.currentBody = normalizeBodyKey(jmarsState.get('body'));

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

  normalizeBookmark(bookmark) {
    if (!bookmark) return null;
    return {
      ...bookmark,
      body: normalizeBodyKey(bookmark.body)
    };
  }

  loadFromStorage() {
    const stored = localStorage.getItem('jmars_bookmarks');
    if (stored) {
      try {
        this.bookmarks = JSON.parse(stored).map(bookmark => this.normalizeBookmark(bookmark)).filter(Boolean);
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
      this.bookmarks.push(this.normalizeBookmark({
        id: crypto.randomUUID?.() || `${Date.now()}-${Math.random().toString(36).substring(2, 9)}`,
        name: name,
        lat: center.lat,
        lng: center.lng,
        zoom: zoom,
        body: this.currentBody
      }));
      this.saveToStorage();
      this.render();
    }
  }

  async goTo(bookmark) {
    if (!bookmark) return;
    const targetBody = normalizeBodyKey(bookmark.body);
    if (targetBody !== this.currentBody) {
      if (this.jmarsMap) {
        await Promise.resolve(switchActiveBody(this.jmarsMap, targetBody));
      } else {
        this.currentBody = targetBody;
      }
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

    const card = document.createElement('div');
    card.className = 'titanium-card';

    // Header with Title & Action Buttons
    const header = document.createElement('div');
    header.className = 'titanium-card-header';

    const title = document.createElement('span');
    title.className = 'titanium-card-title';
    title.innerHTML = `
      <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
        <path d="M19 21l-7-5-7 5V5a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2z"></path>
      </svg>
      Saved Views & ROIs
    `;

    const btnGroup = document.createElement('div');
    btnGroup.style.display = 'flex';
    btnGroup.style.gap = '4px';

    const addBtn = document.createElement('button');
    addBtn.type = 'button';
    addBtn.className = 'glass-pill-btn';
    addBtn.title = 'Save current map view as bookmark';
    addBtn.innerHTML = `
      <svg width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
        <line x1="12" y1="5" x2="12" y2="19"></line>
        <line x1="5" y1="12" x2="19" y2="12"></line>
      </svg>
      <span>Save</span>
    `;
    addBtn.onclick = () => this.addCurrentView();

    const exportBtn = document.createElement('button');
    exportBtn.type = 'button';
    exportBtn.className = 'glass-pill-btn';
    exportBtn.title = 'Export bookmarks as JSON';
    exportBtn.innerHTML = `
      <svg width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2">
        <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path>
        <polyline points="7 10 12 15 17 10"></polyline>
        <line x1="12" y1="15" x2="12" y2="3"></line>
      </svg>
      <span>Export</span>
    `;
    exportBtn.onclick = () => this.exportJSON();

    btnGroup.appendChild(addBtn);
    btnGroup.appendChild(exportBtn);

    header.appendChild(title);
    header.appendChild(btnGroup);
    card.appendChild(header);

    const list = document.createElement('div');
    list.className = 'bookmark-card-list custom-slim-scroll';

    this.bookmarks.forEach(b => {
      const item = document.createElement('div');
      item.className = 'bookmark-list-item';

      const info = document.createElement('div');
      info.className = 'bookmark-info';

      const pinIcon = document.createElement('span');
      pinIcon.className = 'bookmark-icon';
      pinIcon.innerHTML = `
        <svg width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"></path>
          <circle cx="12" cy="10" r="3"></circle>
        </svg>
      `;

      const textGroup = document.createElement('div');
      textGroup.className = 'bookmark-title-group';

      const nameSpan = document.createElement('span');
      nameSpan.className = 'bookmark-name';
      // Clean up any leading emoji if present, e.g. "🌋 Olympus Mons Summit" -> "Olympus Mons Summit"
      const cleanName = (b.name || 'Unnamed Bookmark').replace(/^[\p{Emoji}\s]+/u, '').trim() || b.name;
      nameSpan.textContent = cleanName;

      const coordsSpan = document.createElement('span');
      coordsSpan.className = 'bookmark-coords';
      if (Number.isFinite(b.lat) && Number.isFinite(b.lng)) {
        coordsSpan.textContent = `${b.lat.toFixed(2)}°, ${b.lng.toFixed(2)}°`;
      }

      textGroup.appendChild(nameSpan);
      if (coordsSpan.textContent) textGroup.appendChild(coordsSpan);

      info.appendChild(pinIcon);
      info.appendChild(textGroup);
      info.onclick = () => { void this.goTo(b); };

      const actions = document.createElement('div');
      actions.style.display = 'flex';
      actions.style.alignItems = 'center';
      actions.style.gap = '6px';

      if (b.body) {
        const bodyBadge = document.createElement('span');
        bodyBadge.className = 'bookmark-badge';
        bodyBadge.textContent = b.body.toUpperCase();
        actions.appendChild(bodyBadge);
      }

      const delBtn = document.createElement('button');
      delBtn.type = 'button';
      delBtn.className = 'bookmark-delete-btn';
      delBtn.title = 'Delete Bookmark';
      delBtn.innerHTML = `
        <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
          <line x1="18" y1="6" x2="6" y2="18"></line>
          <line x1="6" y1="6" x2="18" y2="18"></line>
        </svg>
      `;
      delBtn.onclick = (e) => {
        e.stopPropagation();
        this.remove(b.id);
      };
      actions.appendChild(delBtn);

      item.appendChild(info);
      item.appendChild(actions);
      list.appendChild(item);
    });

    if (this.bookmarks.length === 0) {
      const empty = document.createElement('div');
      empty.style.padding = '14px 10px';
      empty.style.textAlign = 'center';
      empty.style.color = '#64748b';
      empty.style.fontSize = '11px';
      empty.style.fontStyle = 'italic';
      empty.textContent = 'No saved views yet. Click "+ Save" to bookmark.';
      list.appendChild(empty);
    }

    card.appendChild(list);
    this.container.appendChild(card);
  }

  getData() {
    return this.bookmarks.map(bookmark => this.normalizeBookmark(bookmark));
  }

  loadData(data) {
    if (Array.isArray(data)) {
      this.bookmarks = data.map(bookmark => this.normalizeBookmark(bookmark)).filter(Boolean);
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
          body: normalizeBodyKey(b.body)
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
        body: normalizeBodyKey(f.properties?.body)
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
