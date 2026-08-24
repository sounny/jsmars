/**
 * @module NomenclatureTool
 * @description UI tool for displaying, searching, and filtering IAU planetary nomenclature labels on the map.
 */
import { NomenclatureLayer } from './NomenclatureLayer.js';
import { jmarsState } from '../../jmars-state.js';
import { EVENTS } from '../../constants.js';

/**
 * @class NomenclatureTool
 * @description Manages nomenclature label display with type-based filtering and instant search.
 */
export class NomenclatureTool {
  /**
   * @param {L.Map} map - The Leaflet map instance.
   * @param {string} containerId - DOM element ID for the tool's UI container.
   */
  constructor(map, containerId) {
    this.layer = new NomenclatureLayer(map);
    this.container = document.getElementById(containerId);
    this.isActive = false;
    this.currentBody = (jmarsState.get('body') || 'mars').toLowerCase();

    if (this.container) {
      this.renderUI();
    }

    // Listen for body changes
    document.addEventListener(EVENTS.BODY_CHANGED, (e) => {
      this.currentBody = e.detail.body.toLowerCase();
      if (this.isActive) {
        this.layer.render();
      }
    });
  }

  renderUI() {
    this.container.innerHTML = '';

    // Toggle Button
    this.toggleBtn = document.createElement('button');
    this.toggleBtn.className = 'tool-btn';
    this.toggleBtn.textContent = 'Show Nomenclature';
    this.toggleBtn.onclick = () => {
      this.isActive = !this.isActive;
      this.toggleBtn.classList.toggle('active', this.isActive);
      this.toggleBtn.textContent = this.isActive ? 'Hide Nomenclature' : 'Show Nomenclature';
      this.layer.toggle(this.isActive);
      this.filterContainer.style.display = this.isActive ? 'block' : 'none';
    };
    this.container.appendChild(this.toggleBtn);

    // Filters & Search container
    this.filterContainer = document.createElement('div');
    this.filterContainer.style.display = 'none';
    this.filterContainer.style.padding = '8px';
    this.filterContainer.style.background = '#1e293b';
    this.filterContainer.style.borderRadius = '4px';
    this.filterContainer.style.marginTop = '6px';

    // Search bar
    const searchInput = document.createElement('input');
    searchInput.type = 'text';
    searchInput.placeholder = 'Search IAU features (e.g. Gale, Olympus)...';
    searchInput.style.width = '100%';
    searchInput.style.padding = '4px 6px';
    searchInput.style.marginBottom = '8px';
    searchInput.style.fontSize = '11px';
    searchInput.style.background = '#0f172a';
    searchInput.style.color = '#fff';
    searchInput.style.border = '1px solid #334155';
    searchInput.style.borderRadius = '3px';

    searchInput.addEventListener('input', (e) => {
      this.layer.setSearchQuery(e.target.value);
    });

    this.filterContainer.appendChild(searchInput);

    // Type Checkboxes grid
    const typeGrid = document.createElement('div');
    typeGrid.style.display = 'grid';
    typeGrid.style.gridTemplateColumns = '1fr 1fr';
    typeGrid.style.gap = '4px';

    const types = [
      { id: 'Crater', color: '#38bdf8' },
      { id: 'Mons', color: '#fbbf24' },
      { id: 'Valles', color: '#2dd4bf' },
      { id: 'Chasma', color: '#34d399' },
      { id: 'Planitia', color: '#a3e635' },
      { id: 'Planum', color: '#c084fc' },
      { id: 'Chaos', color: '#f43f5e' },
      { id: 'Fossa', color: '#fb923c' }
    ];

    types.forEach(t => {
      const label = document.createElement('label');
      label.style.display = 'flex';
      label.style.alignItems = 'center';
      label.style.gap = '4px';
      label.style.fontSize = '11px';
      label.style.color = t.color;
      label.style.cursor = 'pointer';

      const checkbox = document.createElement('input');
      checkbox.type = 'checkbox';
      checkbox.checked = true;
      checkbox.onchange = (e) => {
        this.layer.toggleType(t.id, e.target.checked);
      };

      label.appendChild(checkbox);
      label.appendChild(document.createTextNode(t.id));
      typeGrid.appendChild(label);
    });

    this.filterContainer.appendChild(typeGrid);
    this.container.appendChild(this.filterContainer);
  }

  // --- IAU Gazetteer Filtering & Spatial Metadata Solvers ---

  /**
   * Filter an array of IAU nomenclature features based on multi-attribute criteria.
   * @param {Array<object>} features - Array of feature records
   * @param {object} criteria - Filtering options
   * @param {string} [criteria.search=''] - Name or origin search query
   * @param {Array<string>} [criteria.types] - Allowed feature types (e.g. ['Crater', 'Mons'])
   * @param {number} [criteria.minDiameterKm=0] - Minimum feature diameter
   * @param {number} [criteria.maxDiameterKm=Infinity] - Maximum feature diameter
   * @param {'all'|'north'|'south'} [criteria.hemisphere='all'] - Hemisphere filter
   * @returns {Array<object>} Filtered features
   */
  static filterFeatures(features = [], criteria = {}) {
    const search = (criteria.search || '').trim().toLowerCase();
    const allowedTypes = criteria.types ? new Set(criteria.types.map(t => t.toLowerCase())) : null;
    const minDiam = criteria.minDiameterKm || 0;
    const maxDiam = criteria.maxDiameterKm != null ? criteria.maxDiameterKm : Infinity;
    const hemisphere = (criteria.hemisphere || 'all').toLowerCase();

    return features.filter(f => {
      // 1. Search Query
      if (search) {
        const nameMatch = (f.name || '').toLowerCase().includes(search);
        const originMatch = (f.origin || '').toLowerCase().includes(search);
        const typeMatch = (f.type || '').toLowerCase().includes(search);
        if (!nameMatch && !originMatch && !typeMatch) return false;
      }

      // 2. Type Filter
      if (allowedTypes && allowedTypes.size > 0) {
        const fType = (f.type || '').toLowerCase();
        if (!allowedTypes.has(fType)) return false;
      }

      // 3. Diameter Filter
      const diam = f.diameter || f.diameterKm || 0;
      if (diam < minDiam || diam > maxDiam) return false;

      // 4. Hemisphere Filter
      const lat = f.lat != null ? f.lat : (f.latitude || 0);
      if (hemisphere === 'north' && lat < 0) return false;
      if (hemisphere === 'south' && lat > 0) return false;

      return true;
    });
  }

  /**
   * Extract standardized metadata record from an IAU feature.
   * @param {object} feature
   * @returns {{name: string, type: string, diameterKm: number, lat: number, lon: number, origin: string, approvalYear: number|null}}
   */
  static extractFeatureMetadata(feature = {}) {
    return {
      name: feature.name || 'Unnamed',
      type: feature.type || 'Surface Feature',
      diameterKm: parseFloat((feature.diameter || feature.diameterKm || 0).toFixed(1)),
      lat: parseFloat((feature.lat != null ? feature.lat : (feature.latitude || 0)).toFixed(4)),
      lon: parseFloat((feature.lon != null ? feature.lon : (feature.lng || feature.longitude || 0)).toFixed(4)),
      origin: feature.origin || 'IAU Nomenclature Database',
      approvalYear: feature.approvalYear || feature.year || null
    };
  }

  /**
   * Calculate approximate bounding box around an IAU feature from center and diameter.
   * @param {object} feature
   * @param {string} [body='mars']
   * @returns {{south: number, west: number, north: number, east: number}}
   */
  static computeFeatureBoundingBox(feature, body = 'mars') {
    const R = (body.toLowerCase() === 'moon') ? 1737.4 : 3389.5;
    const lat = feature.lat != null ? feature.lat : (feature.latitude || 0);
    const lon = feature.lon != null ? feature.lon : (feature.lng || feature.longitude || 0);
    const diam = Math.max(1, feature.diameter || feature.diameterKm || 10);

    const radiusKm = diam / 2.0;
    const kmPerDegLat = (Math.PI * R) / 180;
    const dLat = radiusKm / kmPerDegLat;

    const cosLat = Math.max(0.01, Math.cos(lat * Math.PI / 180));
    const dLon = radiusKm / (kmPerDegLat * cosLat);

    return {
      south: parseFloat(Math.max(-90, lat - dLat).toFixed(4)),
      north: parseFloat(Math.min(90, lat + dLat).toFixed(4)),
      west: parseFloat((((lon - dLon) % 360 + 360) % 360).toFixed(4)),
      east: parseFloat((((lon + dLon) % 360 + 360) % 360).toFixed(4))
    };
  }
}

