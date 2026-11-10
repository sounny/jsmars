/**
 * @module features/shapes/ShapeTable
 * @description Interactive attribute table for vector shapes drawn on the map.
 * Displays shape type, geologic pattern badge, user-assigned name, custom GIS
 * attribute columns, and body-accurate geodesic measurements (Mars, Moon, Earth, Europa).
 * Supports row selection, inline double-click cell editing across all attribute columns,
 * custom column creation, geodesic buffering, and zoom-to-shape on click.
 */
import { EVENTS } from '../../constants.js';
import { jmarsState } from '../../jmars-state.js';
import { normalizeBodyKey } from '../../util/body.js';
import { haversineDistance, sphericalPolygonArea, getPlanetaryRadius } from '../../util/geo.js';
import { GEOLOGIC_PATTERNS } from './StyleEditor.js';

/**
 * @class ShapeTable
 * @description Renders an interactive GIS attribute table for shapes.
 */
export class ShapeTable {
  /**
   * @param {HTMLElement} container - Parent element
   * @param {import('./ShapeLayer.js').ShapeLayer} shapeLayer - Shape layer instance
   * @param {L.Map} map - Leaflet map
   */
  constructor(container, shapeLayer, map) {
    this.container = container;
    this.shapeLayer = shapeLayer;
    this.map = map;
    /** @type {Array<string>} Ordered list of user-added / discovered attribute column keys */
    this.customColumns = [];
    if (!this.container) return;
    this._build();
    this._bindEvents();
  }

  _build() {
    this.container.innerHTML = `
      <div class="shape-table-wrapper">
        <div class="shape-table-toolbar" style="flex-wrap:wrap; gap:4px;">
          <span id="shape-count" class="shape-count">0 shapes</span>
          <div class="shape-table-actions" style="display:flex; gap:4px; flex-wrap:wrap;">
            <button type="button" class="crater-action-btn shape-add-col-btn" title="Add custom attribute column">+ Column</button>
            <button type="button" class="crater-action-btn shape-buffer-btn" title="Create geodesic buffer around selected shape">Buffer</button>
            <button type="button" class="crater-action-btn shape-delete-btn" title="Delete selected shape">Delete</button>
          </div>
        </div>
        <div id="shape-inline-form" style="display:none; padding:6px; background:#1e293b; border-bottom:1px solid #334155; gap:4px; align-items:center;">
          <input type="text" id="shape-inline-input" style="flex:1; min-width:0; padding:4px 6px; font-size:11px; background:#0f172a; color:#f8fafc; border:1px solid #475569; border-radius:4px;" />
          <button type="button" id="shape-inline-submit" class="crater-action-btn" style="background:#0284c7; color:#fff;">Apply</button>
          <button type="button" id="shape-inline-cancel" class="crater-action-btn">&times;</button>
        </div>
        <div class="shape-table-scroll">
          <table class="shape-attr-table">
            <thead id="shape-table-head"><tr>
              <th>ID</th>
              <th>Type</th>
              <th>Name</th>
              <th>Measurement</th>
            </tr></thead>
            <tbody id="shape-table-body"></tbody>
          </table>
        </div>
      </div>
    `;

    this.countEl = this.container.querySelector('#shape-count');
    this.thead = this.container.querySelector('#shape-table-head');
    this.tbody = this.container.querySelector('#shape-table-body');
    this.deleteBtn = this.container.querySelector('.shape-delete-btn');
    this.addColBtn = this.container.querySelector('.shape-add-col-btn');
    this.bufferBtn = this.container.querySelector('.shape-buffer-btn');
    this.inlineForm = this.container.querySelector('#shape-inline-form');
    this.inlineInput = this.container.querySelector('#shape-inline-input');
    this.inlineSubmit = this.container.querySelector('#shape-inline-submit');
    this.inlineCancel = this.container.querySelector('#shape-inline-cancel');
    this._inlineMode = null; // 'column' | 'buffer'
  }

  _bindEvents() {
    document.addEventListener(EVENTS.SHAPE_CREATED, () => this.refresh());
    document.addEventListener(EVENTS.SHAPE_UPDATED, () => this.refresh());
    document.addEventListener(EVENTS.SHAPE_DELETED, () => this.refresh());
    document.addEventListener(EVENTS.SHAPES_IMPORTED, () => this.refresh());
    document.addEventListener(EVENTS.BODY_CHANGED, () => this.refresh());
    document.addEventListener(EVENTS.SHAPE_SELECTED, (e) => {
      this._highlightRow(e.detail?.id);
    });

    this.deleteBtn.addEventListener('click', () => {
      if (this.shapeLayer.selectedId != null) {
        this.shapeLayer.deleteShape(this.shapeLayer.selectedId);
      }
    });

    this.addColBtn.addEventListener('click', () => this._openInlineForm('column'));
    this.bufferBtn.addEventListener('click', () => {
      if (this.shapeLayer.selectedId == null && this.shapeLayer.shapes.length > 0) {
        this.shapeLayer.selectShape(this.shapeLayer.shapes[0].id);
      }
      if (this.shapeLayer.selectedId != null) {
        this._openInlineForm('buffer');
      }
    });

    this.inlineSubmit.addEventListener('click', () => this._submitInlineForm());
    this.inlineCancel.addEventListener('click', () => this._closeInlineForm());
    this.inlineInput.addEventListener('keydown', (e) => {
      if (e.key === 'Enter') {
        e.preventDefault();
        this._submitInlineForm();
      } else if (e.key === 'Escape') {
        this._closeInlineForm();
      }
    });
  }

  _openInlineForm(mode) {
    this._inlineMode = mode;
    this.inlineForm.style.display = 'flex';
    if (mode === 'column') {
      this.inlineInput.type = 'text';
      this.inlineInput.placeholder = 'Attribute column (e.g. unit, epoch)';
      this.inlineInput.value = '';
      this.inlineSubmit.textContent = 'Add';
    } else {
      this.inlineInput.type = 'number';
      this.inlineInput.placeholder = 'Buffer radius (km)';
      this.inlineInput.value = '10';
      this.inlineSubmit.textContent = 'Buffer';
    }
    this.inlineInput.focus();
  }

  _closeInlineForm() {
    this._inlineMode = null;
    this.inlineForm.style.display = 'none';
  }

  _submitInlineForm() {
    const val = this.inlineInput.value.trim();
    if (this._inlineMode === 'column' && val) {
      this.addColumn(val);
    } else if (this._inlineMode === 'buffer') {
      const km = parseFloat(val);
      if (Number.isFinite(km) && km > 0 && this.shapeLayer.selectedId != null) {
        this.shapeLayer.createBuffer(this.shapeLayer.selectedId, km);
      }
    }
    this._closeInlineForm();
  }

  /**
   * Return ordered list of extra attribute columns across user-added columns and imported shapes.
   * @returns {Array<string>}
   */
  getExtraColumns() {
    const seen = new Set(this.customColumns);
    this.shapeLayer.shapes.forEach(shape => {
      Object.keys(shape.attributes || {}).forEach(k => {
        if (k === 'name') return;
        if (k === 'description' && !shape.attributes.description && !this.customColumns.includes('description')) return;
        seen.add(k);
      });
    });
    return [...seen];
  }

  /**
   * Refresh the table headers and rows from the shape layer using safe DOM construction.
   */
  refresh() {
    const shapes = this.shapeLayer.shapes;
    this.countEl.textContent = `${shapes.length} shape${shapes.length !== 1 ? 's' : ''}`;

    const extraCols = this.getExtraColumns();
    const totalCols = 4 + extraCols.length;

    // Render dynamic table header safely
    const headerRow = document.createElement('tr');
    ['ID', 'Type', 'Name', ...extraCols, 'Measurement'].forEach(colLabel => {
      const th = document.createElement('th');
      th.textContent = colLabel;
      headerRow.appendChild(th);
    });
    this.thead.innerHTML = '';
    this.thead.appendChild(headerRow);

    this.tbody.innerHTML = '';
    if (shapes.length === 0) {
      const emptyTr = document.createElement('tr');
      const emptyTd = document.createElement('td');
      emptyTd.colSpan = totalCols;
      emptyTd.style.color = '#888';
      emptyTd.style.textAlign = 'center';
      emptyTd.textContent = 'No shapes';
      emptyTr.appendChild(emptyTd);
      this.tbody.appendChild(emptyTr);
      return;
    }

    shapes.forEach(shape => {
      const tr = document.createElement('tr');
      tr.className = `shape-table-row${shape.id === this.shapeLayer.selectedId ? ' shape-row-selected' : ''}`;
      tr.dataset.id = String(shape.id);

      const idTd = document.createElement('td');
      idTd.textContent = String(shape.id);

      const typeTd = document.createElement('td');
      const patKey = shape.style?.pattern;
      if (patKey && GEOLOGIC_PATTERNS[patKey]) {
        typeTd.textContent = `${shape.type} (${GEOLOGIC_PATTERNS[patKey].label.split(' ')[0]})`;
        typeTd.title = `${shape.type} — ${GEOLOGIC_PATTERNS[patKey].label}`;
      } else {
        typeTd.textContent = shape.type;
      }

      const nameTd = document.createElement('td');
      nameTd.className = 'editable-cell';
      nameTd.dataset.field = 'name';
      nameTd.title = 'Double-click to edit';
      nameTd.textContent = shape.attributes?.name || '';

      tr.appendChild(idTd);
      tr.appendChild(typeTd);
      tr.appendChild(nameTd);

      extraCols.forEach(colKey => {
        const colTd = document.createElement('td');
        colTd.className = 'editable-cell';
        colTd.dataset.field = colKey;
        colTd.title = `Double-click to edit ${colKey}`;
        colTd.textContent = shape.attributes?.[colKey] != null ? String(shape.attributes[colKey]) : '';
        tr.appendChild(colTd);
      });

      const measTd = document.createElement('td');
      measTd.textContent = this._getMeasurement(shape);
      tr.appendChild(measTd);

      tr.addEventListener('click', () => {
        this.shapeLayer.selectShape(shape.id);
        this._zoomToShape(shape.id);
      });

      tr.querySelectorAll('.editable-cell').forEach(cell => {
        cell.addEventListener('dblclick', (e) => {
          e.stopPropagation();
          this._startEdit(cell, shape.id, cell.dataset.field);
        });
      });

      this.tbody.appendChild(tr);
    });
  }

  /**
   * Calculate planetary-body-accurate geodesic measurement string for a shape.
   * @param {object} shape
   * @returns {string}
   */
  _getMeasurement(shape) {
    try {
      const body = normalizeBodyKey(jmarsState.get('body') || 'mars');
      const layer = shape.layer;

      if (shape.type === 'marker') {
        const ll = layer.getLatLng();
        return `${ll.lat.toFixed(3)}, ${ll.lng.toFixed(3)}`;
      }
      if (shape.type === 'circle') {
        const earthMeters = layer.getRadius();
        const bodyRadiusKm = (earthMeters / 1000.0) * (getPlanetaryRadius(body, 'mean') / 6371.0);
        return `r=${bodyRadiusKm.toFixed(1)} km`;
      }
      if (shape.type === 'polyline') {
        const raw = layer.getLatLngs();
        const latlngs = Array.isArray(raw[0]) && typeof raw[0].lat !== 'number' ? raw[0] : raw;
        let dist = 0;
        for (let i = 1; i < latlngs.length; i++) {
          dist += haversineDistance(
            latlngs[i - 1].lat, latlngs[i - 1].lng,
            latlngs[i].lat, latlngs[i].lng,
            body
          );
        }
        return `${dist.toFixed(1)} km`;
      }
      if (shape.type === 'polygon' || shape.type === 'rectangle') {
        const raw = layer.getLatLngs();
        const latlngs = Array.isArray(raw[0]) ? raw[0] : raw;
        const area = sphericalPolygonArea(latlngs, body);
        if (area > 1e6) return `${(area / 1e6).toFixed(2)} M km\u00b2`;
        return `${area.toFixed(1)} km\u00b2`;
      }
    } catch {
      return '-';
    }
    return '-';
  }

  /**
   * Start inline editing of an attribute cell.
   * @param {HTMLElement} cell
   * @param {number} shapeId
   * @param {string} field
   */
  _startEdit(cell, shapeId, field) {
    if (cell.querySelector('input')) return;
    const current = cell.textContent;
    const input = document.createElement('input');
    input.type = 'text';
    input.value = current;
    input.style.cssText = 'width:100%;background:#0f172a;color:#fff;border:1px solid #38bdf8;padding:2px 4px;font-size:12px;border-radius:2px;box-sizing:border-box;';

    cell.textContent = '';
    cell.appendChild(input);
    input.focus();
    input.select();

    let committed = false;
    const finish = (cancelled = false) => {
      if (committed) return;
      committed = true;
      const nextVal = cancelled ? current : input.value.trim();
      const finalVal = field === 'name' ? (nextVal || current) : nextVal;
      cell.textContent = finalVal;
      this.shapeLayer.updateAttributes(shapeId, { [field]: finalVal });
    };

    input.addEventListener('click', (e) => e.stopPropagation());
    input.addEventListener('blur', () => finish(false));
    input.addEventListener('keydown', (e) => {
      if (e.key === 'Enter') {
        e.preventDefault();
        finish(false);
      } else if (e.key === 'Escape') {
        e.preventDefault();
        finish(true);
      }
    });
  }

  /**
   * Zoom to a shape on the map.
   * @param {number} id
   */
  _zoomToShape(id) {
    if (!this.map) return;
    const shape = this.shapeLayer.shapes.find(s => s.id === id);
    if (!shape) return;

    if (shape.layer.getBounds) {
      this.map.fitBounds(shape.layer.getBounds(), { padding: [50, 50] });
    } else if (shape.layer.getLatLng) {
      this.map.setView(shape.layer.getLatLng(), Math.max(this.map.getZoom?.() || 6, 8));
    }
  }

  /**
   * Highlight a row by shape ID.
   * @param {number} id
   */
  _highlightRow(id) {
    this.tbody.querySelectorAll('.shape-table-row').forEach(row => {
      row.classList.toggle('shape-row-selected', parseInt(row.dataset.id, 10) === id);
    });
  }

  /**
   * Add a custom attribute column to the table and initialize empty string values on existing shapes.
   * @param {string} [columnName] - Column name; if omitted, opens the inline input form
   * @returns {string|null} Normalized column key added
   */
  addColumn(columnName) {
    if (!columnName || !columnName.trim()) {
      this._openInlineForm('column');
      return null;
    }
    const key = columnName.trim().toLowerCase().replace(/[^a-z0-9_]+/g, '_').replace(/^_+|_+$/g, '');
    if (!key || key === 'id' || key === 'type' || key === 'name' || key === 'measurement') {
      return null;
    }
    if (!this.customColumns.includes(key)) {
      this.customColumns.push(key);
    }

    this.shapeLayer.shapes.forEach(shape => {
      if (!(key in shape.attributes)) {
        shape.attributes[key] = '';
      }
    });

    this.refresh();
    return key;
  }

  /**
   * Legacy alias preserved for compatibility.
   * @param {string} [columnName]
   */
  _addColumn(columnName) {
    return this.addColumn(columnName);
  }
}
