import { EVENTS } from '../../constants.js';

/**
 * @module MeasurementTable
 * @description Renders a table of geodesic measurements (distance, azimuth,
 * area, perimeter) and highlights or deletes rows interactively.
 */
export class MeasurementTable {
    /**
     * Create a MeasurementTable.
     * @param {string} containerId - DOM element ID for the table container.
     * @param {import('./MeasureTool.js').MeasureTool} measureTool - Associated MeasureTool instance.
     */
    constructor(containerId, measureTool) {
        this.container = document.getElementById(containerId);
        this.measureTool = measureTool;
        this.selectedId = null;

        if (!this.container) {
            console.error('MeasurementTable container not found');
            return;
        }

        this.render();

        document.addEventListener(EVENTS.MEASURE_UPDATED, (e) => {
            this.renderRows(e.detail || []);
        });

        document.addEventListener(EVENTS.MEASURE_HIGHLIGHT, (e) => {
            if (e.detail?.id) {
                this.highlightRow(e.detail.id);
            }
        });
    }

    /**
     * Build the initial table skeleton inside the container.
     */
    render() {
        this.container.textContent = '';

        const table = document.createElement('table');
        table.className = 'crater-table';
        table.style.cssText = 'width:100%; font-size:12px; color:#eee; border-collapse:collapse;';

        const thead = document.createElement('thead');
        const headRow = document.createElement('tr');
        headRow.style.cssText = 'border-bottom:1px solid #555; text-align:left;';

        const columns = ['Name', 'Type', 'Value', 'Az / Perim', ''];
        columns.forEach((label) => {
            const th = document.createElement('th');
            th.style.padding = '4px';
            th.textContent = label;
            headRow.appendChild(th);
        });
        thead.appendChild(headRow);

        const tbody = document.createElement('tbody');
        tbody.id = 'measure-table-body';

        table.appendChild(thead);
        table.appendChild(tbody);

        const emptyMsg = document.createElement('div');
        emptyMsg.id = 'measure-empty-msg';
        emptyMsg.style.cssText = 'padding:10px; text-align:center; color:#888; font-style:italic;';
        emptyMsg.textContent = 'No measurements yet.';

        this.container.appendChild(table);
        this.container.appendChild(emptyMsg);

        this.tbody = tbody;
        this.emptyMsg = emptyMsg;
    }

    /**
     * Populate table rows from the measurements array using XSS-safe DOM APIs.
     * @param {Array<object>} measurements - Array of measurement records.
     */
    renderRows(measurements) {
        if (!this.tbody || !this.emptyMsg) return;
        this.tbody.textContent = '';

        if (!Array.isArray(measurements) || measurements.length === 0) {
            this.emptyMsg.style.display = 'block';
            this.selectedId = null;
            return;
        }
        this.emptyMsg.style.display = 'none';

        measurements.forEach((m) => {
            const tr = document.createElement('tr');
            tr.id = `measure-row-${m.id}`;
            tr.style.borderBottom = '1px solid #333';
            tr.style.cursor = 'pointer';
            tr.title = `${m.vertices || 0} vertices`;

            // Editable Name cell
            const nameTd = document.createElement('td');
            nameTd.style.padding = '4px';
            const nameInput = document.createElement('input');
            nameInput.type = 'text';
            nameInput.value = m.name || '';
            nameInput.setAttribute('aria-label', `Measurement name for ${m.name || m.type}`);
            nameInput.style.cssText = 'background:transparent; border:none; color:#eee; width:100%; font-family:inherit; font-size:inherit;';
            nameInput.addEventListener('change', (e) => {
                this.measureTool.updateName(m.id, e.target.value);
            });
            nameInput.addEventListener('click', (e) => e.stopPropagation());
            nameTd.appendChild(nameInput);

            // Type cell (with vertex count badge)
            const typeTd = document.createElement('td');
            typeTd.style.cssText = 'padding:4px; white-space:nowrap; color:#cbd5e1;';
            typeTd.textContent = `${m.type} (${m.vertices ?? 0})`;

            // Primary Value cell
            const valueTd = document.createElement('td');
            valueTd.style.cssText = 'padding:4px; white-space:nowrap; font-variant-numeric:tabular-nums;';
            valueTd.textContent = m.valueStr || '';

            // Secondary Metric cell (Azimuth for Line, Perimeter for Area)
            const secTd = document.createElement('td');
            secTd.style.cssText = 'padding:4px; white-space:nowrap; color:#94a3b8; font-size:11px; font-variant-numeric:tabular-nums;';
            if (m.secondaryStr) {
                secTd.textContent = m.secondaryStr;
            } else if (m.type === 'Line' && Number.isFinite(m.azimuthDeg)) {
                secTd.textContent = `Az ${m.azimuthDeg.toFixed(1)}°`;
            } else if (m.type === 'Area' && m.perimeterStr) {
                secTd.textContent = `Perim ${m.perimeterStr}`;
            } else {
                secTd.textContent = '—';
            }

            // Delete Action cell
            const actionTd = document.createElement('td');
            actionTd.style.cssText = 'padding:2px 4px; text-align:right;';
            const delBtn = document.createElement('button');
            delBtn.type = 'button';
            delBtn.className = 'measure-delete-btn';
            delBtn.setAttribute('aria-label', `Delete measurement ${m.name || m.id}`);
            delBtn.title = 'Delete measurement';
            delBtn.style.cssText = 'background:transparent; border:none; color:#94a3b8; cursor:pointer; font-size:14px; line-height:1; padding:2px 5px; border-radius:4px;';
            delBtn.textContent = '×';
            delBtn.addEventListener('click', (e) => {
                e.stopPropagation();
                if (typeof this.measureTool.deleteMeasurement === 'function') {
                    if (this.selectedId === m.id) this.selectedId = null;
                    this.measureTool.deleteMeasurement(m.id);
                }
            });
            actionTd.appendChild(delBtn);

            tr.appendChild(nameTd);
            tr.appendChild(typeTd);
            tr.appendChild(valueTd);
            tr.appendChild(secTd);
            tr.appendChild(actionTd);

            tr.addEventListener('click', () => {
                this.measureTool.highlight(m.id);
            });

            this.tbody.appendChild(tr);
        });

        if (this.selectedId) {
            this.highlightRow(this.selectedId);
        }
    }

    /**
     * Highlight a specific table row by measurement ID.
     * @param {string} id - Measurement ID to highlight.
     */
    highlightRow(id) {
        if (this.selectedId) {
            const prevRow = document.getElementById(`measure-row-${this.selectedId}`);
            if (prevRow) prevRow.style.background = 'transparent';
        }

        this.selectedId = id;
        const row = document.getElementById(`measure-row-${id}`);
        if (row) {
            row.style.background = '#334155';
            if (typeof row.scrollIntoView === 'function') {
                row.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
            }
        }
    }
}
