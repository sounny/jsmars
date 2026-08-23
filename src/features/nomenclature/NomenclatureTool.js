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
}
