/**
 * @module features/crater-counting/CraterTable
 * @description Interactive HTML table component for managing digitized crater
 * records. Displays crater coordinates and diameters, supports inline deletion,
 * and provides export to both CSV and GeoJSON formats. Listens for
 * {@link EVENTS.CRATER_ADDED} events and dispatches
 * {@link EVENTS.CRATER_REMOVE} and {@link EVENTS.CRATER_CLEAR} events.
 */
import { EVENTS } from '../../constants.js';
import { CSFDEngine } from './CSFDEngine.js';
import { CSFDChart } from './CSFDChart.js';

/**
 * @class CraterTable
 * @description Renders a sortable table of crater measurements with toolbar
 * buttons for CSV export, GeoJSON export, bulk clearing, and real-time CSFD isochron dating.
 */
export class CraterTable {
    /**
     * Creates a new CraterTable and renders it into the given container.
     * @param {string} containerId - DOM id of the element that will host the table.
     */
    constructor(containerId) {
        this.container = document.getElementById(containerId);
        this.craters = [];
        this.csfdChart = null;

        if (!this.container) {
            console.warn('CraterTable container not found');
            return;
        }

        this.render();

        // Listen for updates
        document.addEventListener(EVENTS.CRATER_ADDED, (e) => {
            this.addCrater(e.detail);
            this.updateCSFD();
        });

        document.addEventListener(EVENTS.CRATER_CLEAR, () => {
            this.craters = [];
            if (this.tbody) this.tbody.innerHTML = '';
            this.updateCSFD();
        });
    }

    /**
     * Builds the initial table markup, including the toolbar and column headers,
     * and wires up the export and clear button listeners.
     * @returns {void}
     */
    render() {
        this.container.innerHTML = `
      <div class="crater-btn-group">
        <button id="crater-export-btn" class="crater-action-btn" style="background: #333;">Export CSV</button>
        <button id="crater-export-json-btn" class="crater-action-btn" style="background: #333;">Export GeoJSON</button>
        <button id="crater-clear-btn" class="crater-action-btn" style="background: #500; border-color: #700;">Clear All</button>
      </div>
      <div class="crater-table-container" style="max-height: 120px; overflow-y: auto;">
        <table class="crater-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>Lat</th>
              <th>Lon</th>
              <th>Diam (km)</th>
              <th></th>
            </tr>
          </thead>
          <tbody id="crater-table-body">
          </tbody>
        </table>
      </div>
      <div id="crater-csfd-container" style="margin-top: 10px; border-top: 1px solid #334155; padding-top: 8px;"></div>
    `;
        this.tbody = this.container.querySelector('#crater-table-body');
        this.csfdChart = new CSFDChart(this.container.querySelector('#crater-csfd-container'));

        this.container.querySelector('#crater-export-btn').addEventListener('click', () => this.exportCSV());
        this.container.querySelector('#crater-export-json-btn').addEventListener('click', () => this.exportGeoJSON());
        this.container.querySelector('#crater-clear-btn').addEventListener('click', () => this.clearAll());
    }

    updateCSFD() {
        if (!this.csfdChart) return;
        const csfd = CSFDEngine.computeCSFD(this.craters);
        this.csfdChart.setCSFD(csfd);
        document.dispatchEvent(new CustomEvent(EVENTS.CSFD_UPDATED, { detail: csfd }));
    }

    /**
     * Appends a crater record to the table and internal list.
     * @param {object} crater - Crater data object.
     * @param {number} crater.id - Unique numeric identifier.
     * @param {number} crater.lat - Latitude in decimal degrees.
     * @param {number} crater.lng - Longitude in decimal degrees.
     * @param {number} crater.diameter - Diameter in meters.
     * @returns {void}
     */
    addCrater(crater) {
        if (!this.tbody) return;
        this.craters.push(crater);

        const tr = document.createElement('tr');
        tr.id = `crater-row-${crater.id}`;
        tr.style.borderBottom = '1px solid #333';
        tr.innerHTML = `
      <td>${crater.id.toString().slice(-4)}</td>
      <td>${crater.lat.toFixed(2)}</td>
      <td>${crater.lng.toFixed(2)}</td>
      <td>${(crater.diameter / 1000).toFixed(1)}</td>
      <td style="text-align: right;">
        <button class="delete-crater-btn" data-id="${crater.id}" style="background: none; border: none; color: #f55; cursor: pointer;">&times;</button>
      </td>
    `;

        tr.querySelector('.delete-crater-btn').addEventListener('click', (e) => {
            const id = parseInt(e.target.dataset.id) || e.target.dataset.id;
            this.removeCrater(id);
        });

        this.tbody.prepend(tr); // Add new at top
    }

    /**
     * Removes a single crater by id from the table and internal list,
     * then dispatches a {@link EVENTS.CRATER_REMOVE} event.
     * @param {number|string} id - The crater id to remove.
     * @returns {void}
     */
    removeCrater(id) {
        // Remove from local list
        this.craters = this.craters.filter(c => c.id != id);

        // Remove from DOM
        const row = this.tbody.querySelector(`#crater-row-${id}`);
        if (row) row.remove();

        this.updateCSFD();

        // Dispatch removal event
        const event = new CustomEvent(EVENTS.CRATER_REMOVE, { detail: { id } });
        document.dispatchEvent(event);
    }

    /**
     * Clears all crater records after user confirmation. Empties the table
     * body and dispatches a {@link EVENTS.CRATER_CLEAR} event.
     * @returns {void}
     */
    clearAll() {
        if (!confirm('Are you sure you want to clear all craters?')) return;

        this.craters = [];
        this.tbody.innerHTML = '';
        this.updateCSFD();

        // Dispatch clear event
        const event = new CustomEvent(EVENTS.CRATER_CLEAR);
        document.dispatchEvent(event);
    }

    /**
     * Exports all recorded craters as a downloadable CSV file
     * with columns: ID, Lat, Lon, Diameter_km.
     * @returns {void}
     */
    exportCSV() {
        if (this.craters.length === 0) {
            alert('No craters to export.');
            return;
        }

        const headers = ['ID,Lat,Lon,Diameter_km\n'];
        const rows = this.craters.map(c =>
            `${c.id},${c.lat.toFixed(5)},${c.lng.toFixed(5)},${(c.diameter / 1000).toFixed(3)}`
        );

        const csvContent = "data:text/csv;charset=utf-8," + headers.join('') + rows.join('\n');
        const encodedUri = encodeURI(csvContent);
        const link = document.createElement("a");
        link.setAttribute("href", encodedUri);
        link.setAttribute("download", "jmars_craters.csv");
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
    }

    /**
     * Exports all recorded craters as a downloadable GeoJSON FeatureCollection.
     * Each feature is a Point with diameter properties.
     * @returns {void}
     */
    exportGeoJSON() {
        if (this.craters.length === 0) {
            alert('No craters to export.');
            return;
        }

        const features = this.craters.map(c => ({
            type: "Feature",
            geometry: {
                type: "Point",
                coordinates: [c.lng, c.lat] // GeoJSON is Lon, Lat
            },
            properties: {
                id: c.id,
                diameter_m: c.diameter,
                diameter_km: c.diameter / 1000
            }
        }));

        const collection = {
            type: "FeatureCollection",
            features: features
        };

        const content = JSON.stringify(collection, null, 2);
        const encodedUri = "data:application/json;charset=utf-8," + encodeURIComponent(content);
        const link = document.createElement("a");
        link.setAttribute("href", encodedUri);
        link.setAttribute("download", "jmars_craters.geojson");
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
    }
}
