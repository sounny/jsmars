/**
 * @module NomenclatureTool
 * @description UI tool for displaying and filtering planetary nomenclature labels on the map.
 */
import { NomenclatureLayer } from './NomenclatureLayer.js';
import { jmarsState } from '../../jmars-state.js';
import { EVENTS } from '../../constants.js';

/**
 * @class NomenclatureTool
 * @description Manages nomenclature label display with type-based filtering.
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
            this.updateVisibility();
        });
    }

    updateVisibility() {
        const section = this.container.closest('.layer-section');
        if (!section) return;

        if (this.currentBody !== 'mars') {
            // Force disable if active
            if (this.isActive) {
                this.toggleBtn.click(); // Deactivate
            }
            section.style.display = 'none';
        } else {
            section.style.display = 'block';
        }
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

        // Filters
        this.filterContainer = document.createElement('div');
        this.filterContainer.style.display = 'none';
        this.filterContainer.style.padding = '10px';
        this.filterContainer.style.background = '#222';
        this.filterContainer.style.marginTop = '5px';

        const types = ['Crater', 'Mons', 'Valles', 'Planitia'];
        
        types.forEach(type => {
            const label = document.createElement('label');
            label.style.display = 'block';
            label.style.marginBottom = '4px';
            label.style.cursor = 'pointer';

            const checkbox = document.createElement('input');
            checkbox.type = 'checkbox';
            checkbox.checked = true;
            checkbox.style.marginRight = '8px';
            checkbox.onchange = (e) => {
                this.layer.toggleType(type, e.target.checked);
            };

            label.appendChild(checkbox);
            label.appendChild(document.createTextNode(type));
            this.filterContainer.appendChild(label);
        });

        this.container.appendChild(this.filterContainer);
        
        // Initial check
        this.updateVisibility();
    }
}
