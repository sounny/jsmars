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

  // --- Static GIS Serialization & WKT Solvers ---

  /**
   * Convert GeoJSON geometry or coordinate array to Well-Known Text (WKT).
   * @param {object} geojson - { type, coordinates }
   * @returns {string} WKT representation
   */
  static toWKT(geojson) {
    if (!geojson || !geojson.type) return '';
    const type = geojson.type.toUpperCase();
    const coords = geojson.coordinates;

    switch (type) {
      case 'POINT':
        return `POINT(${coords[0]} ${coords[1]})`;
      case 'LINESTRING':
        return `LINESTRING(${coords.map(p => `${p[0]} ${p[1]}`).join(', ')})`;
      case 'POLYGON':
        return `POLYGON((${coords[0].map(p => `${p[0]} ${p[1]}`).join(', ')}))`;
      default:
        return '';
    }
  }

  /**
   * Parse a Well-Known Text (WKT) string into a GeoJSON geometry object.
   * @param {string} wkt - WKT string
   * @returns {{type: string, coordinates: any}|null}
   */
  static parseWKT(wkt) {
    if (!wkt || typeof wkt !== 'string') return null;
    const clean = wkt.trim();
    const match = clean.match(/^([A-Za-z]+)\s*\((.*)\)$/s);
    if (!match) return null;

    const type = match[1].toUpperCase();
    const body = match[2].trim();

    if (type === 'POINT') {
      const parts = body.split(/\s+/).map(Number);
      return { type: 'Point', coordinates: [parts[0], parts[1]] };
    }

    if (type === 'LINESTRING') {
      const coords = body.split(',').map(pair => {
        const parts = pair.trim().split(/\s+/).map(Number);
        return [parts[0], parts[1]];
      });
      return { type: 'LineString', coordinates: coords };
    }

    if (type === 'POLYGON') {
      const ringBody = body.replace(/^\(/, '').replace(/\)$/, '').trim();
      const coords = ringBody.split(',').map(pair => {
        const parts = pair.trim().split(/\s+/).map(Number);
        return [parts[0], parts[1]];
      });
      return { type: 'Polygon', coordinates: [coords] };
    }

    return null;
  }

  // --- ESRI Shapefile Binary Header & Record Parsing Solvers ---

  /**
   * Parse the 100-byte header of an ESRI Shapefile (.shp).
   * @param {ArrayBuffer} buffer - Raw file buffer (at least 100 bytes)
   * @returns {{fileCode: number, fileLengthWords: number, fileLengthBytes: number, version: number, shapeType: number, shapeTypeName: string, bbox: {xMin: number, yMin: number, xMax: number, yMax: number}}}
   */
  static parseShapefileHeader(buffer) {
    if (!buffer || buffer.byteLength < 100) {
      throw new Error('Invalid Shapefile: buffer too small (less than 100 bytes)');
    }

    const view = new DataView(buffer);
    const fileCode = view.getInt32(0, false); // Big-endian 9994 (0x270a)
    if (fileCode !== 9994) {
      throw new Error(`Invalid Shapefile magic code: ${fileCode}, expected 9994`);
    }

    const fileLengthWords = view.getInt32(24, false); // Big-endian 16-bit words
    const version = view.getInt32(28, true); // Little-endian (1000)
    const shapeType = view.getInt32(32, true); // Little-endian

    const xMin = view.getFloat64(36, true);
    const yMin = view.getFloat64(44, true);
    const xMax = view.getFloat64(52, true);
    const yMax = view.getFloat64(60, true);

    const SHAPE_NAMES = {
      0: 'Null Shape',
      1: 'Point',
      3: 'PolyLine',
      5: 'Polygon',
      8: 'MultiPoint',
      11: 'PointZ',
      13: 'PolyLineZ',
      15: 'PolygonZ'
    };

    return {
      fileCode,
      fileLengthWords,
      fileLengthBytes: fileLengthWords * 2,
      version,
      shapeType,
      shapeTypeName: SHAPE_NAMES[shapeType] || `Unknown (${shapeType})`,
      bbox: {
        xMin: parseFloat(xMin.toFixed(6)),
        yMin: parseFloat(yMin.toFixed(6)),
        xMax: parseFloat(xMax.toFixed(6)),
        yMax: parseFloat(yMax.toFixed(6))
      }
    };
  }

  /**
   * Parse ShapeType 1 (Point) binary records from a Shapefile ArrayBuffer into GeoJSON Features.
   * @param {ArrayBuffer} buffer - Shapefile buffer
   * @returns {Array<object>} Array of GeoJSON Point feature objects
   */
  static parsePointRecords(buffer) {
    const header = this.parseShapefileHeader(buffer);
    if (header.shapeType !== 1) {
      throw new Error(`Expected Point Shapefile (Type 1), got ${header.shapeTypeName} (Type ${header.shapeType})`);
    }

    const view = new DataView(buffer);
    const features = [];
    let offset = 100; // Header size is 100 bytes

    while (offset + 8 <= buffer.byteLength) {
      const recordNumber = view.getInt32(offset, false);
      const contentLengthWords = view.getInt32(offset + 4, false);
      const contentLengthBytes = contentLengthWords * 2;
      offset += 8;

      if (offset + contentLengthBytes > buffer.byteLength) break;

      const recordShapeType = view.getInt32(offset, true);
      if (recordShapeType === 1) {
        const x = view.getFloat64(offset + 4, true);
        const y = view.getFloat64(offset + 12, true);

        features.push({
          type: 'Feature',
          id: recordNumber,
          geometry: {
            type: 'Point',
            coordinates: [parseFloat(x.toFixed(6)), parseFloat(y.toFixed(6))]
          },
          properties: { recordNumber }
        });
      }

      offset += contentLengthBytes;
    }

    return features;
  }

  /**
   * Create a binary ESRI Shapefile (.shp) buffer from an array of [lon, lat] coordinates.
   * @param {Array<[number, number]>} points - Array of [x, y] / [lon, lat] points
   * @returns {ArrayBuffer} Valid 100-byte header + records Shapefile binary buffer
   */
  static createShapefilePointBuffer(points = []) {
    const count = points.length;
    const recordHeaderBytes = 8;
    const pointRecordContentBytes = 20; // 4 (type) + 8 (x) + 8 (y)
    const totalRecordBytes = count * (recordHeaderBytes + pointRecordContentBytes);
    const totalFileBytes = 100 + totalRecordBytes;

    const buffer = new ArrayBuffer(totalFileBytes);
    const view = new DataView(buffer);

    // Compute bounding box
    let xMin = Infinity, yMin = Infinity, xMax = -Infinity, yMax = -Infinity;
    points.forEach(([x, y]) => {
      if (x < xMin) xMin = x;
      if (y < yMin) yMin = y;
      if (x > xMax) xMax = x;
      if (y > yMax) yMax = y;
    });

    if (count === 0) {
      xMin = 0; yMin = 0; xMax = 0; yMax = 0;
    }

    // Write 100-byte Header
    view.setInt32(0, 9994, false); // File Code
    view.setInt32(24, totalFileBytes / 2, false); // File Length in 16-bit words
    view.setInt32(28, 1000, true); // Version
    view.setInt32(32, 1, true); // ShapeType: Point
    view.setFloat64(36, xMin, true);
    view.setFloat64(44, yMin, true);
    view.setFloat64(52, xMax, true);
    view.setFloat64(60, yMax, true);

    // Write Records
    let offset = 100;
    points.forEach(([x, y], idx) => {
      view.setInt32(offset, idx + 1, false); // Record Number
      view.setInt32(offset + 4, pointRecordContentBytes / 2, false); // Content Length (words)
      view.setInt32(offset + 8, 1, true); // ShapeType: Point
      view.setFloat64(offset + 12, x, true);
      view.setFloat64(offset + 20, y, true);
      offset += 28;
    });

    return buffer;
  }

  // --- dBASE III (.dbf) Binary Attribute Table Parsing & Serialization ---

  /**
   * Parse the header and field descriptor table of a dBASE III (.dbf) file.
   * @param {ArrayBuffer} buffer
   * @returns {{version: number, recordCount: number, headerLength: number, recordLength: number, fields: Array<{name: string, type: string, length: number, decimals: number}>}}
   */
  static parseDBFHeader(buffer) {
    if (!buffer || buffer.byteLength < 32) {
      throw new Error('Invalid DBF: buffer too small');
    }

    const view = new DataView(buffer);
    const version = view.getUint8(0);
    const recordCount = view.getInt32(4, true);
    const headerLength = view.getInt16(8, true);
    const recordLength = view.getInt16(10, true);

    const fields = [];
    let offset = 32;

    while (offset < headerLength - 1 && view.getUint8(offset) !== 0x0D) {
      let name = '';
      for (let i = 0; i < 11; i++) {
        const charCode = view.getUint8(offset + i);
        if (charCode === 0) break;
        name += String.fromCharCode(charCode);
      }

      const type = String.fromCharCode(view.getUint8(offset + 11));
      const length = view.getUint8(offset + 16);
      const decimals = view.getUint8(offset + 17);

      fields.push({ name: name.trim(), type, length, decimals });
      offset += 32;
    }

    return {
      version,
      recordCount,
      headerLength,
      recordLength,
      fields
    };
  }

  /**
   * Parse all attribute rows from a dBASE III (.dbf) ArrayBuffer.
   * @param {ArrayBuffer} buffer
   * @returns {Array<object>} Array of record property objects
   */
  static parseDBFRecords(buffer) {
    const header = this.parseDBFHeader(buffer);
    const view = new DataView(buffer);
    const records = [];
    let offset = header.headerLength;

    const decoder = new TextDecoder('utf-8');

    for (let r = 0; r < header.recordCount; r++) {
      if (offset + header.recordLength > buffer.byteLength) break;

      const deletionFlag = String.fromCharCode(view.getUint8(offset));
      if (deletionFlag === '*') {
        // Record marked for deletion
        offset += header.recordLength;
        continue;
      }

      const row = {};
      let fieldOffset = offset + 1; // Skip deletion flag byte

      for (const field of header.fields) {
        const fieldBytes = new Uint8Array(buffer, fieldOffset, field.length);
        const rawStr = decoder.decode(fieldBytes).trim();

        if (field.type === 'N' || field.type === 'F') {
          row[field.name] = rawStr === '' ? null : Number(rawStr);
        } else if (field.type === 'L') {
          row[field.name] = (rawStr === 'T' || rawStr === 'Y' || rawStr === 't' || rawStr === 'y');
        } else {
          row[field.name] = rawStr;
        }

        fieldOffset += field.length;
      }

      records.push(row);
      offset += header.recordLength;
    }

    return records;
  }

  /**
   * Create a binary dBASE III (.dbf) buffer from field definitions and record rows.
   * @param {Array<{name: string, type: string, length: number, decimals: number}>} fields
   * @param {Array<object>} records
   * @returns {ArrayBuffer} Valid binary DBF buffer
   */
  static createDBFBuffer(fields, records = []) {
    const numFields = fields.length;
    const headerLength = 32 + numFields * 32 + 1; // +1 for 0x0D header terminator
    let recordLength = 1; // Deletion flag byte

    fields.forEach(f => {
      recordLength += f.length;
    });

    const totalBytes = headerLength + records.length * recordLength + 1; // +1 for 0x1A EOF marker
    const buffer = new ArrayBuffer(totalBytes);
    const view = new DataView(buffer);
    const uint8 = new Uint8Array(buffer);

    const now = new Date();
    view.setUint8(0, 0x03); // dBASE III without memo
    view.setUint8(1, now.getFullYear() % 100);
    view.setUint8(2, now.getMonth() + 1);
    view.setUint8(3, now.getDate());
    view.setInt32(4, records.length, true);
    view.setInt16(8, headerLength, true);
    view.setInt16(10, recordLength, true);

    // Write Field Descriptors
    let fieldOffset = 32;
    fields.forEach(f => {
      for (let i = 0; i < 11; i++) {
        view.setUint8(fieldOffset + i, i < f.name.length ? f.name.charCodeAt(i) : 0);
      }
      view.setUint8(fieldOffset + 11, f.type.charCodeAt(0));
      view.setUint8(fieldOffset + 16, f.length);
      view.setUint8(fieldOffset + 17, f.decimals || 0);
      fieldOffset += 32;
    });

    // Terminator
    view.setUint8(fieldOffset, 0x0D);

    // Write Records
    let rowOffset = headerLength;
    records.forEach(rec => {
      view.setUint8(rowOffset, 0x20); // Space = not deleted
      let colOffset = rowOffset + 1;

      fields.forEach(f => {
        const val = rec[f.name] != null ? String(rec[f.name]) : '';
        const padded = (f.type === 'N' || f.type === 'F')
          ? val.padStart(f.length, ' ')
          : val.padEnd(f.length, ' ');

        for (let i = 0; i < f.length; i++) {
          view.setUint8(colOffset + i, i < padded.length ? padded.charCodeAt(i) : 0x20);
        }
        colOffset += f.length;
      });

      rowOffset += recordLength;
    });

    // End-of-file marker
    view.setUint8(rowOffset, 0x1A);

    return buffer;
  }
}



