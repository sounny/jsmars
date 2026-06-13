/**
 * ShapeIO provides file import/export utilities for the Shape Layer.
 * Handles file dialog, drag-and-drop, and format detection.
 */
export class ShapeIO {
  /**
   * @param {import('./ShapeLayer.js').ShapeLayer} shapeLayer
   */
  constructor(shapeLayer) {
    this.shapeLayer = shapeLayer;
  }

  /**
   * Open a file dialog and import the selected file.
   * Supports: .geojson, .json, .csv, .kml
   */
  importFile() {
    const input = document.createElement('input');
    input.type = 'file';
    input.accept = '.geojson,.json,.csv,.kml';
    input.style.display = 'none';

    input.addEventListener('change', async (e) => {
      const file = e.target.files[0];
      if (!file) return;
      await this._processFile(file);
      input.remove();
    });

    document.body.appendChild(input);
    input.click();
  }

  /**
   * Process a file and import shapes.
   * @param {File} file
   */
  async _processFile(file) {
    try {
      const ext = file.name.split('.').pop().toLowerCase();
      const text = await file.text();

      switch (ext) {
        case 'geojson':
        case 'json':
          this.shapeLayer.fromGeoJSON(JSON.parse(text));
          break;
        case 'csv':
          this.shapeLayer.fromCSV(text);
          break;
        case 'kml':
          this.shapeLayer.fromKML(text);
          break;
        default:
          console.warn(`Unsupported file format: .${ext}`);
          alert(`Unsupported file format: .${ext}\nSupported: .geojson, .json, .csv, .kml`);
      }
    } catch (err) {
      console.error('Import error:', err);
      alert(`Import failed: ${err.message}`);
    }
  }

  /**
   * Export shapes in the selected format.
   * @param {string} format - 'geojson', 'csv', 'kml'
   */
  exportFile(format) {
    switch (format) {
      case 'geojson':
        this.shapeLayer.downloadGeoJSON();
        break;
      case 'kml':
        this.shapeLayer.downloadKML();
        break;
      case 'csv':
        this._downloadCSV();
        break;
      default:
        console.warn(`Unsupported export format: ${format}`);
    }
  }

  /**
   * Export marker shapes as CSV.
   */
  _downloadCSV() {
    const shapes = this.shapeLayer.shapes;
    if (shapes.length === 0) return;

    // Collect all unique attribute keys
    const allKeys = new Set();
    shapes.forEach(s => Object.keys(s.attributes).forEach(k => allKeys.add(k)));
    const attrKeys = [...allKeys];

    const headers = ['id', 'type', 'latitude', 'longitude', ...attrKeys];

    const rows = shapes.map(shape => {
      let lat = '', lon = '';
      if (shape.layer.getLatLng) {
        const ll = shape.layer.getLatLng();
        lat = ll.lat.toFixed(6);
        lon = ll.lng.toFixed(6);
      } else if (shape.layer.getBounds) {
        const center = shape.layer.getBounds().getCenter();
        lat = center.lat.toFixed(6);
        lon = center.lng.toFixed(6);
      }

      const attrs = attrKeys.map(k => {
        const val = (shape.attributes[k] || '').toString();
        // Escape CSV per RFC 4180: wrap in quotes if the value
        // contains commas, double-quotes, or newlines.
        if (val.includes(',') || val.includes('"') || val.includes('\n')) {
          return `"${val.replace(/"/g, '""')}"`;
        }
        return val;
      });

      return [shape.id, shape.type, lat, lon, ...attrs].join(',');
    });

    const csv = [headers.join(','), ...rows].join('\n');
    const blob = new Blob([csv], { type: 'text/csv' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `shapes_${Date.now()}.csv`;
    a.click();
    URL.revokeObjectURL(url);
  }

  /**
   * Setup drag-and-drop import on a target element.
   * @param {HTMLElement} target - Drop target element (e.g., the map container)
   */
  enableDragDrop(target) {
    target.addEventListener('dragover', (e) => {
      e.preventDefault();
      e.dataTransfer.dropEffect = 'copy';
      target.classList.add('drag-over');
    });

    target.addEventListener('dragleave', () => {
      target.classList.remove('drag-over');
    });

    target.addEventListener('drop', async (e) => {
      e.preventDefault();
      target.classList.remove('drag-over');

      const files = e.dataTransfer.files;
      for (const file of files) {
        await this._processFile(file);
      }
    });
  }
}
