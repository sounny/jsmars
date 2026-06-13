/**
 * @module features/shapes/ShapeTable
 * @description Interactive attribute table for vector shapes drawn on the map.
 * Displays shape type, user-assigned name, and computed measurements (distance,
 * area, coordinates). Supports row selection, inline name editing, custom
 * column addition, and zoom-to-shape on click.
 */
import { EVENTS } from '../../constants.js';
import { haversineDistance, sphericalPolygonArea } from '../../util/geo.js';

/**
 * @class ShapeTable
 * @description Renders an interactive attribute table for shapes.
 * Click a row to select and zoom to the shape on the map.
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
    this._build();
    this._bindEvents();
  }

  _build() {
    this.container.innerHTML = `
      <div class="shape-table-wrapper">
        <div class="shape-table-toolbar">
          <span id="shape-count" class="shape-count">0 shapes</span>
          <div class="shape-table-actions">
            <button class="crater-action-btn shape-add-col-btn" title="Add column">+ Column</button>
            <button class="crater-action-btn shape-delete-btn" title="Delete selected">Delete</button>
          </div>
        </div>
        <div class="shape-table-scroll">
          <table class="shape-attr-table">
            <thead><tr>
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
    this.tbody = this.container.querySelector('#shape-table-body');
    this.deleteBtn = this.container.querySelector('.shape-delete-btn');
    this.addColBtn = this.container.querySelector('.shape-add-col-btn');
  }

  _bindEvents() {
    // Refresh table on shape events
    document.addEventListener(EVENTS.SHAPE_CREATED, () => this.refresh());
    document.addEventListener(EVENTS.SHAPE_UPDATED, () => this.refresh());
    document.addEventListener(EVENTS.SHAPE_DELETED, () => this.refresh());
    document.addEventListener(EVENTS.SHAPES_IMPORTED, () => this.refresh());
    document.addEventListener(EVENTS.SHAPE_SELECTED, (e) => {
      this._highlightRow(e.detail?.id);
    });

    this.deleteBtn.addEventListener('click', () => {
      if (this.shapeLayer.selectedId != null) {
        this.shapeLayer.deleteShape(this.shapeLayer.selectedId);
      }
    });

    this.addColBtn.addEventListener('click', () => this._addColumn());
  }

  /**
   * Refresh the table contents from the shape layer.
   */
  refresh() {
    const shapes = this.shapeLayer.shapes;
    this.countEl.textContent = `${shapes.length} shape${shapes.length !== 1 ? 's' : ''}`;

    if (shapes.length === 0) {
      this.tbody.innerHTML = '<tr><td colspan="4" style="color:#888;text-align:center">No shapes</td></tr>';
      return;
    }

    this.tbody.innerHTML = shapes.map(shape => {
      const measurement = this._getMeasurement(shape);
      const name = shape.attributes.name || '';
      return `
        <tr class="shape-table-row ${shape.id === this.shapeLayer.selectedId ? 'shape-row-selected' : ''}"
            data-id="${shape.id}">
          <td>${shape.id}</td>
          <td>${shape.type}</td>
          <td class="editable-cell" data-field="name">${this._escapeHtml(name)}</td>
          <td>${measurement}</td>
        </tr>
      `;
    }).join('');

    // Row click to select
    this.tbody.querySelectorAll('.shape-table-row').forEach(row => {
      row.addEventListener('click', () => {
        const id = parseInt(row.dataset.id, 10);
        this.shapeLayer.selectShape(id);
        this._zoomToShape(id);
      });

      // Double-click to edit name
      row.querySelector('.editable-cell')?.addEventListener('dblclick', (e) => {
        this._startEdit(e.target, parseInt(row.dataset.id, 10), 'name');
      });
    });
  }

  /**
   * Calculate measurement string for a shape.
   * @param {object} shape
   * @returns {string}
   */
  _getMeasurement(shape) {
    try {
      const layer = shape.layer;
      if (shape.type === 'marker') {
        const ll = layer.getLatLng();
        return `${ll.lat.toFixed(3)}, ${ll.lng.toFixed(3)}`;
      }
      if (shape.type === 'circle') {
        const r = layer.getRadius(); // meters in Leaflet
        return `r=${(r / 1000).toFixed(1)} km`;
      }
      if (shape.type === 'polyline') {
        const latlngs = layer.getLatLngs();
        let dist = 0;
        for (let i = 1; i < latlngs.length; i++) {
          dist += haversineDistance(
            latlngs[i - 1].lat, latlngs[i - 1].lng,
            latlngs[i].lat, latlngs[i].lng
          );
        }
        return `${dist.toFixed(1)} km`;
      }
      if (shape.type === 'polygon' || shape.type === 'rectangle') {
        const latlngs = layer.getLatLngs()[0] || [];
        const area = sphericalPolygonArea(latlngs);
        if (area > 1e6) return `${(area / 1e6).toFixed(2)} M km\u00b2`;
        return `${area.toFixed(1)} km\u00b2`;
      }
    } catch (err) {
      return '-';
    }
    return '-';
  }

  /**
   * Start inline editing of a cell.
   * @param {HTMLElement} cell
   * @param {number} shapeId
   * @param {string} field
   */
  _startEdit(cell, shapeId, field) {
    const current = cell.textContent;
    const input = document.createElement('input');
    input.type = 'text';
    input.value = current;
    input.style.cssText = 'width:100%;background:#333;color:#fff;border:1px solid #4dabf7;padding:2px 4px;font-size:12px;border-radius:2px';

    cell.textContent = '';
    cell.appendChild(input);
    input.focus();
    input.select();

    const finish = () => {
      const newVal = input.value.trim();
      cell.textContent = newVal || current;
      this.shapeLayer.updateAttributes(shapeId, { [field]: newVal || current });
    };

    input.addEventListener('blur', finish);
    input.addEventListener('keydown', (e) => {
      if (e.key === 'Enter') input.blur();
      if (e.key === 'Escape') {
        input.value = current;
        input.blur();
      }
    });
  }

  /**
   * Zoom to a shape on the map.
   * @param {number} id
   */
  _zoomToShape(id) {
    const shape = this.shapeLayer.shapes.find(s => s.id === id);
    if (!shape) return;

    if (shape.layer.getBounds) {
      this.map.fitBounds(shape.layer.getBounds(), { padding: [50, 50] });
    } else if (shape.layer.getLatLng) {
      this.map.setView(shape.layer.getLatLng(), 10);
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
   * Add a custom column to the attribute table (prompts user).
   */
  _addColumn() {
    const name = prompt('Column name:');
    if (!name || !name.trim()) return;
    const key = name.trim().toLowerCase().replace(/\s+/g, '_');

    // Add attribute to all shapes
    this.shapeLayer.shapes.forEach(shape => {
      if (!(key in shape.attributes)) {
        shape.attributes[key] = '';
      }
    });

    this.refresh();
  }

  _escapeHtml(str) {
    const div = document.createElement('div');
    div.textContent = str;
    return div.innerHTML;
  }
}
