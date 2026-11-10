import { jmarsState } from '../../jmars-state.js';
import { normalizeBodyKey } from '../../util/body.js';
import { computeEllipsePolygon, getPlanetaryRadius } from '../../util/geo.js';

/**
 * ShapeIO provides file import/export utilities for the Shape Layer.
 * Handles file dialog, drag-and-drop, format detection, WKT, and
 * standards-compliant ESRI Shapefile (.shp/.shx/.dbf) binary serialization.
 */
export class ShapeIO {
  /**
   * @param {import('./ShapeLayer.js').ShapeLayer} shapeLayer
   */
  constructor(shapeLayer) {
    this.shapeLayer = shapeLayer;
  }

  /**
   * Open a file dialog and import the selected file(s).
   * Supports: .geojson, .json, .csv, .kml, .wkt, .shp, .dbf
   */
  importFile() {
    const input = document.createElement('input');
    input.type = 'file';
    input.multiple = true;
    input.accept = '.geojson,.json,.csv,.kml,.wkt,.txt,.shp,.dbf,.shx';
    input.style.display = 'none';

    input.addEventListener('change', async (e) => {
      const files = Array.from(e.target.files || []);
      if (files.length > 0) {
        await this.importFiles(files);
      }
      input.remove();
    });

    document.body.appendChild(input);
    input.click();
  }

  /**
   * Import one or more files, automatically pairing companion .shp and .dbf files.
   * @param {Array<File>|FileList} fileList
   */
  async importFiles(fileList) {
    const files = Array.from(fileList || []);
    if (files.length === 0) return;

    const dbfByStem = new Map();
    const primaryFiles = [];

    for (const file of files) {
      const parts = file.name.split('.');
      const ext = parts.pop().toLowerCase();
      const stem = parts.join('.').toLowerCase();
      if (ext === 'dbf') {
        dbfByStem.set(stem, file);
      } else if (ext === 'shx' || ext === 'prj' || ext === 'cpg') {
        continue;
      } else {
        primaryFiles.push({ file, ext, stem });
      }
    }

    for (const { file, ext, stem } of primaryFiles) {
      if (ext === 'shp') {
        const companionDbf = dbfByStem.get(stem) || (dbfByStem.size === 1 ? [...dbfByStem.values()][0] : null);
        await this._processShapefile(file, companionDbf);
      } else {
        await this._processFile(file);
      }
    }
  }

  /**
   * Parse and import a binary .shp file and optional companion .dbf attribute table.
   * @param {File} shpFile
   * @param {File|null} [dbfFile=null]
   */
  async _processShapefile(shpFile, dbfFile = null) {
    try {
      const shpBuf = await shpFile.arrayBuffer();
      const dbfBuf = dbfFile ? await dbfFile.arrayBuffer() : null;
      const collection = ShapeIO.parseShapefileAuto(shpBuf, dbfBuf);
      this.shapeLayer.fromGeoJSON(collection);
    } catch (err) {
      console.error('Shapefile import error:', err);
      this._notify(`Shapefile import failed: ${err.message}`, true);
    }
  }

  /**
   * Process a single file and import shapes.
   * @param {File} file
   */
  async _processFile(file) {
    try {
      const ext = file.name.split('.').pop().toLowerCase();
      if (ext === 'shp') {
        await this._processShapefile(file, null);
        return;
      }

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
        case 'wkt':
        case 'txt':
          this.shapeLayer.fromWKT(text);
          break;
        default:
          console.warn(`Unsupported file format: .${ext}`);
          this._notify(`Unsupported file format: .${ext} (use .geojson, .csv, .kml, .wkt, or .shp)`, true);
      }
    } catch (err) {
      console.error('Import error:', err);
      this._notify(`Import failed: ${err.message}`, true);
    }
  }

  /**
   * Export shapes in the selected format.
   * @param {string} format - 'geojson', 'csv', 'kml', 'wkt', 'shp'
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
      case 'wkt':
        this.shapeLayer.downloadWKT();
        break;
      case 'shp':
        this._downloadShapefile();
        break;
      default:
        console.warn(`Unsupported export format: ${format}`);
    }
  }

  /**
   * Download ESRI Shapefile triad (.shp, .shx, .dbf) for all shapes in the layer.
   */
  _downloadShapefile() {
    const shapes = this.shapeLayer.shapes;
    if (!shapes || shapes.length === 0) return;

    const body = normalizeBodyKey(jmarsState.get('body') || 'mars');
    const bundles = ShapeIO.exportShapefileBundles(shapes, body);
    const stamp = Date.now();

    bundles.forEach(bundle => {
      const prefix = bundles.length > 1 ? `shapes_${bundle.family}_${stamp}` : `shapes_${stamp}`;
      this._triggerBinaryDownload(bundle.shpBuffer, `${prefix}.shp`);
      this._triggerBinaryDownload(bundle.shxBuffer, `${prefix}.shx`);
      this._triggerBinaryDownload(bundle.dbfBuffer, `${prefix}.dbf`);
    });
  }

  /**
   * Trigger a binary ArrayBuffer download.
   * @param {ArrayBuffer} buffer
   * @param {string} filename
   * @private
   */
  _triggerBinaryDownload(buffer, filename) {
    const blob = new Blob([buffer], { type: 'application/octet-stream' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    a.click();
    URL.revokeObjectURL(url);
  }

  /**
   * Non-blocking accessible toast notification for import/export feedback.
   * @param {string} message
   * @param {boolean} [isError=false]
   * @private
   */
  _notify(message, isError = false) {
    if (typeof document === 'undefined' || !document.body) return;
    let toast = document.getElementById('jsmars-io-toast');
    if (!toast) {
      toast = document.createElement('div');
      toast.id = 'jsmars-io-toast';
      toast.setAttribute('role', 'status');
      toast.setAttribute('aria-live', 'polite');
      toast.style.cssText = 'position:fixed;bottom:44px;left:50%;transform:translateX(-50%);z-index:4500;padding:8px 14px;border-radius:8px;font-size:12px;font-weight:500;box-shadow:0 6px 20px rgba(0,0,0,0.5);transition:opacity 0.2s ease;';
      document.body.appendChild(toast);
    }
    toast.style.background = isError ? '#7f1d1d' : '#0f172a';
    toast.style.color = '#f8fafc';
    toast.style.border = isError ? '1px solid #f87171' : '1px solid #38bdf8';
    toast.textContent = message;
    toast.style.display = 'block';
    clearTimeout(this._toastTimer);
    this._toastTimer = setTimeout(() => {
      if (toast) toast.style.display = 'none';
    }, 3500);
  }

  /**
   * Export shapes as CSV.
   */
  _downloadCSV() {
    const shapes = this.shapeLayer.shapes;
    if (shapes.length === 0) return;

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
    if (!target) return;
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

      const files = e.dataTransfer?.files;
      if (files && files.length > 0) {
        await this.importFiles(files);
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
      case 'MULTIPOINT':
        return `MULTIPOINT(${coords.map(p => `(${p[0]} ${p[1]})`).join(', ')})`;
      case 'MULTILINESTRING':
        return `MULTILINESTRING(${coords.map(line => `(${line.map(p => `${p[0]} ${p[1]}`).join(', ')})`).join(', ')})`;
      case 'MULTIPOLYGON':
        return `MULTIPOLYGON(${coords.map(poly => `((${poly[0].map(p => `${p[0]} ${p[1]}`).join(', ')}))`).join(', ')})`;
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
    const clean = wkt.trim().replace(/^SRID=\d+;/i, '').trim();
    const match = clean.match(/^([A-Za-z]+)\s*\((.*)\)$/s);
    if (!match) return null;

    const type = match[1].toUpperCase();
    const body = match[2].trim();

    const parsePair = (pairStr) => {
      const parts = pairStr.replace(/[()]/g, '').trim().split(/\s+/).map(Number);
      return [parts[0], parts[1]];
    };

    if (type === 'POINT') {
      const pt = parsePair(body);
      if (Number.isNaN(pt[0]) || Number.isNaN(pt[1])) return null;
      return { type: 'Point', coordinates: pt };
    }

    if (type === 'LINESTRING') {
      const coords = body.split(',').map(parsePair);
      return { type: 'LineString', coordinates: coords };
    }

    if (type === 'POLYGON') {
      const rings = body
        .replace(/^\(/, '')
        .replace(/\)$/, '')
        .split(/\)\s*,\s*\(/)
        .map(ringStr => ringStr.replace(/^\(/, '').replace(/\)$/, '').split(',').map(parsePair));
      return { type: 'Polygon', coordinates: rings };
    }

    if (type === 'MULTIPOINT') {
      const coords = body.split(',').map(parsePair);
      return { type: 'MultiPoint', coordinates: coords };
    }

    if (type === 'MULTILINESTRING') {
      const lines = body
        .replace(/^\(/, '')
        .replace(/\)$/, '')
        .split(/\)\s*,\s*\(/)
        .map(lineStr => lineStr.replace(/^\(/, '').replace(/\)$/, '').split(',').map(parsePair));
      return { type: 'MultiLineString', coordinates: lines };
    }

    if (type === 'MULTIPOLYGON') {
      const polys = body
        .replace(/^\(\(/, '')
        .replace(/\)\)$/, '')
        .split(/\)\)\s*,\s*\(\(/)
        .map(polyStr => [polyStr.replace(/^\(/, '').replace(/\)$/, '').split(',').map(parsePair)]);
      return { type: 'MultiPolygon', coordinates: polys };
    }

    return null;
  }

  /**
   * Auto-detect ESRI Shapefile geometry type (Point=1, PolyLine=3, Polygon=5),
   * parse all records, and merge optional dBASE III (.dbf) attributes into a GeoJSON FeatureCollection.
   * @param {ArrayBuffer} shpBuffer
   * @param {ArrayBuffer|null} [dbfBuffer=null]
   * @returns {{type: 'FeatureCollection', features: Array<object>}}
   */
  static parseShapefileAuto(shpBuffer, dbfBuffer = null) {
    const header = this.parseShapefileHeader(shpBuffer);
    let features = [];

    if (header.shapeType === 1) {
      features = this.parsePointRecords(shpBuffer);
    } else if (header.shapeType === 3 || header.shapeType === 5) {
      features = this.parsePolygonRecords(shpBuffer).map(f => {
        if (header.shapeType === 3 && f.geometry.type === 'MultiLineString' && f.geometry.coordinates.length === 1) {
          return {
            ...f,
            geometry: {
              type: 'LineString',
              coordinates: f.geometry.coordinates[0]
            }
          };
        }
        return f;
      });
    } else {
      throw new Error(`Unsupported Shapefile geometry type: ${header.shapeTypeName} (${header.shapeType})`);
    }

    if (dbfBuffer) {
      const dbfRows = this.parseDBFRecords(dbfBuffer);
      features.forEach((feat, idx) => {
        if (dbfRows[idx]) {
          feat.properties = { ...feat.properties, ...dbfRows[idx] };
          if (!feat.properties.name && feat.properties.NAME) {
            feat.properties.name = feat.properties.NAME;
          }
        }
      });
    }

    return {
      type: 'FeatureCollection',
      features
    };
  }

  /**
   * Build standards-compliant ESRI Shapefile (.shp, .shx, .dbf) binary bundles
   * grouped by ESRI geometry family ('point', 'polyline', 'polygon').
   * @param {Array<object>} shapes - ShapeLayer shape objects
   * @param {string} [body='mars'] - Active planetary body for circle approximation
   * @returns {Array<{family: string, shapeType: number, recordCount: number, shpBuffer: ArrayBuffer, shxBuffer: ArrayBuffer, dbfBuffer: ArrayBuffer}>}
   */
  static exportShapefileBundles(shapes = [], body = 'mars') {
    const groups = {
      point: [],
      polyline: [],
      polygon: []
    };

    shapes.forEach(shape => {
      if (!shape || !shape.layer) return;
      if (shape.type === 'marker') {
        const ll = shape.layer.getLatLng();
        groups.point.push({ shape, coords: [ll.lng, ll.lat] });
      } else if (shape.type === 'polyline') {
        const gj = shape.layer.toGeoJSON();
        const lines = gj.geometry.type === 'MultiLineString'
          ? gj.geometry.coordinates
          : [gj.geometry.coordinates];
        groups.polyline.push({ shape, parts: lines });
      } else if (shape.type === 'circle' && shape.layer.getLatLng && shape.layer.getRadius) {
        const ll = shape.layer.getLatLng();
        const radiusKm = (shape.layer.getRadius() / 1000) * (getPlanetaryRadius(body, 'mean') / 6371.0);
        const ringLatLon = computeEllipsePolygon(ll.lat, ll.lng, radiusKm, radiusKm, 0, body, 48);
        const ringLonLat = ringLatLon.map(([lat, lon]) => [Number(lon.toFixed(6)), Number(lat.toFixed(6))]);
        groups.polygon.push({ shape, parts: [ringLonLat] });
      } else if (shape.layer.toGeoJSON) {
        const gj = shape.layer.toGeoJSON();
        if (gj.geometry?.type === 'Polygon') {
          groups.polygon.push({ shape, parts: gj.geometry.coordinates });
        } else if (gj.geometry?.type === 'Point') {
          groups.point.push({ shape, coords: gj.geometry.coordinates });
        }
      }
    });

    const bundles = [];

    const buildDbfForItems = (items) => {
      const attrKeySet = new Set(['name']);
      items.forEach(({ shape }) => {
        Object.keys(shape.attributes || {}).forEach(k => attrKeySet.add(k));
        if (shape.style?.pattern) attrKeySet.add('pattern');
      });

      const fields = [
        { name: 'ID', type: 'N', length: 8, decimals: 0 }
      ];
      const keyMapping = [];
      const usedDbfNames = new Set(['ID']);

      [...attrKeySet].forEach(origKey => {
        let dbfName = origKey.toUpperCase().replace(/[^A-Z0-9_]/g, '').slice(0, 10) || 'ATTR';
        let suffix = 1;
        while (usedDbfNames.has(dbfName)) {
          const base = dbfName.slice(0, 8);
          dbfName = `${base}_${suffix++}`;
        }
        usedDbfNames.add(dbfName);
        fields.push({ name: dbfName, type: 'C', length: 64, decimals: 0 });
        keyMapping.push({ origKey, dbfName });
      });

      const records = items.map(({ shape }) => {
        const row = { ID: shape.id };
        keyMapping.forEach(({ origKey, dbfName }) => {
          if (origKey === 'pattern' && !(origKey in (shape.attributes || {}))) {
            row[dbfName] = shape.style?.pattern || '';
          } else {
            row[dbfName] = shape.attributes?.[origKey] != null ? String(shape.attributes[origKey]).slice(0, 64) : '';
          }
        });
        return row;
      });

      return this.createDBFBuffer(fields, records);
    };

    const buildShxBuffer = (shapeType, bbox, indexEntries) => {
      const totalBytes = 100 + indexEntries.length * 8;
      const shx = new ArrayBuffer(totalBytes);
      const view = new DataView(shx);
      view.setInt32(0, 9994, false);
      view.setInt32(24, totalBytes / 2, false);
      view.setInt32(28, 1000, true);
      view.setInt32(32, shapeType, true);
      view.setFloat64(36, bbox.xMin, true);
      view.setFloat64(44, bbox.yMin, true);
      view.setFloat64(52, bbox.xMax, true);
      view.setFloat64(60, bbox.yMax, true);
      let off = 100;
      indexEntries.forEach(({ offsetWords, contentWords }) => {
        view.setInt32(off, offsetWords, false);
        view.setInt32(off + 4, contentWords, false);
        off += 8;
      });
      return shx;
    };

    if (groups.point.length > 0) {
      const pts = groups.point.map(g => g.coords);
      const shpBuffer = this.createShapefilePointBuffer(pts);
      const hdr = this.parseShapefileHeader(shpBuffer);
      const indexEntries = pts.map((_, idx) => ({
        offsetWords: (100 + idx * 28) / 2,
        contentWords: 10
      }));
      bundles.push({
        family: 'point',
        shapeType: 1,
        recordCount: pts.length,
        shpBuffer,
        shxBuffer: buildShxBuffer(1, hdr.bbox, indexEntries),
        dbfBuffer: buildDbfForItems(groups.point)
      });
    }

    for (const [family, shapeType] of [['polyline', 3], ['polygon', 5]]) {
      const items = groups[family];
      if (items.length === 0) continue;

      let xMin = Infinity, yMin = Infinity, xMax = -Infinity, yMax = -Infinity;
      let recordsBytes = 0;
      const prepared = items.map(({ parts }) => {
        let rxMin = Infinity, ryMin = Infinity, rxMax = -Infinity, ryMax = -Infinity;
        let numPts = 0;
        parts.forEach(ring => {
          numPts += ring.length;
          ring.forEach(([x, y]) => {
            if (x < rxMin) rxMin = x;
            if (y < ryMin) ryMin = y;
            if (x > rxMax) rxMax = x;
            if (y > ryMax) ryMax = y;
          });
        });
        if (rxMin < xMin) xMin = rxMin;
        if (ryMin < yMin) yMin = ryMin;
        if (rxMax > xMax) xMax = rxMax;
        if (ryMax > yMax) yMax = ryMax;
        const contentBytes = 44 + parts.length * 4 + numPts * 16;
        recordsBytes += 8 + contentBytes;
        return { parts, numPts, contentBytes, bbox: { xMin: rxMin, yMin: ryMin, xMax: rxMax, yMax: ryMax } };
      });

      const totalFileBytes = 100 + recordsBytes;
      const shpBuffer = new ArrayBuffer(totalFileBytes);
      const view = new DataView(shpBuffer);
      view.setInt32(0, 9994, false);
      view.setInt32(24, totalFileBytes / 2, false);
      view.setInt32(28, 1000, true);
      view.setInt32(32, shapeType, true);
      view.setFloat64(36, xMin, true);
      view.setFloat64(44, yMin, true);
      view.setFloat64(52, xMax, true);
      view.setFloat64(60, yMax, true);

      let offset = 100;
      const indexEntries = [];
      prepared.forEach((rec, idx) => {
        indexEntries.push({ offsetWords: offset / 2, contentWords: rec.contentBytes / 2 });
        view.setInt32(offset, idx + 1, false);
        view.setInt32(offset + 4, rec.contentBytes / 2, false);
        view.setInt32(offset + 8, shapeType, true);
        view.setFloat64(offset + 12, rec.bbox.xMin, true);
        view.setFloat64(offset + 20, rec.bbox.yMin, true);
        view.setFloat64(offset + 28, rec.bbox.xMax, true);
        view.setFloat64(offset + 36, rec.bbox.yMax, true);
        view.setInt32(offset + 44, rec.parts.length, true);
        view.setInt32(offset + 48, rec.numPts, true);

        let partOff = offset + 52;
        let ptStart = 0;
        rec.parts.forEach(ring => {
          view.setInt32(partOff, ptStart, true);
          partOff += 4;
          ptStart += ring.length;
        });

        let ptOff = partOff;
        rec.parts.forEach(ring => {
          ring.forEach(([x, y]) => {
            view.setFloat64(ptOff, x, true);
            view.setFloat64(ptOff + 8, y, true);
            ptOff += 16;
          });
        });

        offset += 8 + rec.contentBytes;
      });

      bundles.push({
        family,
        shapeType,
        recordCount: items.length,
        shpBuffer,
        shxBuffer: buildShxBuffer(shapeType, { xMin, yMin, xMax, yMax }, indexEntries),
        dbfBuffer: buildDbfForItems(items)
      });
    }

    return bundles;
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

  // --- Binary Shapefile Polygon Parsing & Spatial Intersections ---

  /**
   * Parse ShapeType 5 (Polygon) binary records from a Shapefile ArrayBuffer into GeoJSON Features.
   * @param {ArrayBuffer} buffer - Shapefile buffer
   * @returns {Array<object>} Array of GeoJSON Polygon feature objects
   */
  static parsePolygonRecords(buffer) {
    const header = this.parseShapefileHeader(buffer);
    if (header.shapeType !== 5 && header.shapeType !== 3) {
      throw new Error(`Expected Polygon (Type 5) or Polyline (Type 3), got ${header.shapeTypeName} (Type ${header.shapeType})`);
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
      if (recordShapeType === 5 || recordShapeType === 3) {
        const numParts = view.getInt32(offset + 36, true);
        const numPoints = view.getInt32(offset + 40, true);

        const parts = [];
        for (let p = 0; p < numParts; p++) {
          parts.push(view.getInt32(offset + 44 + p * 4, true));
        }

        const pointsOffset = offset + 44 + numParts * 4;
        const allPoints = [];
        for (let pt = 0; pt < numPoints; pt++) {
          const px = view.getFloat64(pointsOffset + pt * 16, true);
          const py = view.getFloat64(pointsOffset + pt * 16 + 8, true);
          allPoints.push([parseFloat(px.toFixed(6)), parseFloat(py.toFixed(6))]);
        }

        const rings = [];
        for (let p = 0; p < numParts; p++) {
          const start = parts[p];
          const end = (p === numParts - 1) ? numPoints : parts[p + 1];
          rings.push(allPoints.slice(start, end));
        }

        features.push({
          type: 'Feature',
          id: recordNumber,
          geometry: {
            type: recordShapeType === 5 ? 'Polygon' : 'MultiLineString',
            coordinates: rings
          },
          properties: { recordNumber, numParts, numPoints }
        });
      }

      offset += contentLengthBytes;
    }

    return features;
  }

  /**
   * Create a binary ESRI Shapefile (.shp) buffer from an array of Polygon rings.
   * @param {Array<Array<[number, number]>>} rings - Array of coordinate rings [[ [x, y], ... ]]
   * @returns {ArrayBuffer} Valid 100-byte header + ShapeType 5 binary buffer
   */
  static createShapefilePolygonBuffer(rings = []) {
    const numParts = rings.length;
    let totalPoints = 0;
    let xMin = Infinity, yMin = Infinity, xMax = -Infinity, yMax = -Infinity;

    rings.forEach(ring => {
      totalPoints += ring.length;
      ring.forEach(([x, y]) => {
        if (x < xMin) xMin = x;
        if (y < yMin) yMin = y;
        if (x > xMax) xMax = x;
        if (y > yMax) yMax = y;
      });
    });

    if (totalPoints === 0) {
      xMin = 0; yMin = 0; xMax = 0; yMax = 0;
    }

    const recordHeaderBytes = 8;
    const contentBytes = 44 + numParts * 4 + totalPoints * 16;
    const totalRecordBytes = recordHeaderBytes + contentBytes;
    const totalFileBytes = 100 + totalRecordBytes;

    const buffer = new ArrayBuffer(totalFileBytes);
    const view = new DataView(buffer);

    // 100-byte Header
    view.setInt32(0, 9994, false); // Magic code
    view.setInt32(24, totalFileBytes / 2, false); // Length in words
    view.setInt32(28, 1000, true); // Version
    view.setInt32(32, 5, true); // ShapeType: Polygon
    view.setFloat64(36, xMin, true);
    view.setFloat64(44, yMin, true);
    view.setFloat64(52, xMax, true);
    view.setFloat64(60, yMax, true);

    // Write Record
    let offset = 100;
    view.setInt32(offset, 1, false); // Record 1
    view.setInt32(offset + 4, contentBytes / 2, false);
    view.setInt32(offset + 8, 5, true); // Polygon
    view.setFloat64(offset + 12, xMin, true);
    view.setFloat64(offset + 20, yMin, true);
    view.setFloat64(offset + 28, xMax, true);
    view.setFloat64(offset + 36, yMax, true);
    view.setInt32(offset + 44, numParts, true);
    view.setInt32(offset + 48, totalPoints, true);

    let partOffset = offset + 52;
    let pointIndex = 0;
    rings.forEach(ring => {
      view.setInt32(partOffset, pointIndex, true);
      partOffset += 4;
      pointIndex += ring.length;
    });

    let ptOffset = partOffset;
    rings.forEach(ring => {
      ring.forEach(([x, y]) => {
        view.setFloat64(ptOffset, x, true);
        view.setFloat64(ptOffset + 8, y, true);
        ptOffset += 16;
      });
    });

    return buffer;
  }

  /**
   * Calculate 2D Bounding Box spatial overlap and intersection test.
   * @param {{xMin: number, yMin: number, xMax: number, yMax: number}} b1
   * @param {{xMin: number, yMin: number, xMax: number, yMax: number}} b2
   * @returns {{intersects: boolean, overlapArea: number}}
   */
  static computeBBoxOverlap(b1, b2) {
    const xOverlap = Math.max(0, Math.min(b1.xMax, b2.xMax) - Math.max(b1.xMin, b2.xMin));
    const yOverlap = Math.max(0, Math.min(b1.yMax, b2.yMax) - Math.max(b1.yMin, b2.yMin));
    const overlapArea = xOverlap * yOverlap;

    return {
      intersects: overlapArea > 0,
      overlapArea: parseFloat(overlapArea.toFixed(4))
    };
  }

  // --- Polygon Centroid, Polyline Binary Serialization & Decimation Solvers ---

  /**
   * Calculate exact 2D planar/geodetic polygon centroid and signed shoelace area.
   * Cx = (1 / 6A) * sum((xi + xi+1) * (xi*yi+1 - xi+1*yi))
   * Cy = (1 / 6A) * sum((yi + yi+1) * (xi*yi+1 - xi+1*yi))
   * @param {Array<[number, number]>} ring - Polygon coordinate ring [[x, y], ...]
   * @returns {{centroidX: number, centroidY: number, area: number, isClockwise: boolean}}
   */
  static computePolygonCentroid(ring = []) {
    if (!ring || ring.length < 3) {
      return { centroidX: 0, centroidY: 0, area: 0, isClockwise: false };
    }

    let signedArea2 = 0;
    let cxSum = 0;
    let cySum = 0;
    const n = ring.length;

    for (let i = 0; i < n; i++) {
      const p1 = ring[i];
      const p2 = ring[(i + 1) % n];
      const cross = p1[0] * p2[1] - p2[0] * p1[1];

      signedArea2 += cross;
      cxSum += (p1[0] + p2[0]) * cross;
      cySum += (p1[1] + p2[1]) * cross;
    }

    const area = signedArea2 / 2.0;
    if (Math.abs(signedArea2) < 1e-10) {
      return { centroidX: ring[0][0], centroidY: ring[0][1], area: 0, isClockwise: false };
    }

    const cx = cxSum / (3.0 * signedArea2);
    const cy = cySum / (3.0 * signedArea2);

    return {
      centroidX: parseFloat(cx.toFixed(6)),
      centroidY: parseFloat(cy.toFixed(6)),
      area: parseFloat(Math.abs(area).toFixed(6)),
      isClockwise: signedArea2 < 0
    };
  }

  /**
   * Create a binary ESRI Shapefile (.shp) buffer from an array of Polyline segments (ShapeType 3).
   * @param {Array<Array<[number, number]>>} lines - Array of coordinate lines [[ [x, y], ... ]]
   * @returns {ArrayBuffer} Valid 100-byte header + ShapeType 3 binary buffer
   */
  static createShapefilePolylineBuffer(lines = []) {
    const numParts = lines.length;
    let totalPoints = 0;
    let xMin = Infinity, yMin = Infinity, xMax = -Infinity, yMax = -Infinity;

    lines.forEach(line => {
      totalPoints += line.length;
      line.forEach(([x, y]) => {
        if (x < xMin) xMin = x;
        if (y < yMin) yMin = y;
        if (x > xMax) xMax = x;
        if (y > yMax) yMax = y;
      });
    });

    if (totalPoints === 0) {
      xMin = 0; yMin = 0; xMax = 0; yMax = 0;
    }

    const recordHeaderBytes = 8;
    const contentBytes = 44 + numParts * 4 + totalPoints * 16;
    const totalRecordBytes = recordHeaderBytes + contentBytes;
    const totalFileBytes = 100 + totalRecordBytes;

    const buffer = new ArrayBuffer(totalFileBytes);
    const view = new DataView(buffer);

    // 100-byte Header
    view.setInt32(0, 9994, false); // File code
    view.setInt32(24, totalFileBytes / 2, false);
    view.setInt32(28, 1000, true);
    view.setInt32(32, 3, true); // ShapeType: PolyLine (3)
    view.setFloat64(36, xMin, true);
    view.setFloat64(44, yMin, true);
    view.setFloat64(52, xMax, true);
    view.setFloat64(60, yMax, true);

    // Write Record
    let offset = 100;
    view.setInt32(offset, 1, false);
    view.setInt32(offset + 4, contentBytes / 2, false);
    view.setInt32(offset + 8, 3, true); // PolyLine
    view.setFloat64(offset + 12, xMin, true);
    view.setFloat64(offset + 20, yMin, true);
    view.setFloat64(offset + 28, xMax, true);
    view.setFloat64(offset + 36, yMax, true);
    view.setInt32(offset + 44, numParts, true);
    view.setInt32(offset + 48, totalPoints, true);

    let partOffset = offset + 52;
    let pointIndex = 0;
    lines.forEach(line => {
      view.setInt32(partOffset, pointIndex, true);
      partOffset += 4;
      pointIndex += line.length;
    });

    let ptOffset = partOffset;
    lines.forEach(line => {
      line.forEach(([x, y]) => {
        view.setFloat64(ptOffset, x, true);
        view.setFloat64(ptOffset + 8, y, true);
        ptOffset += 16;
      });
    });

    return buffer;
  }

  /**
   * Decimate dense polyline/polygon vertices using radial distance thresholding.
   * @param {Array<[number, number]>} points - Array of [x, y] coordinates
   * @param {number} [tolerance=0.01] - Minimum Euclidean distance between successive vertices
   * @returns {Array<[number, number]>} Simplified array of coordinates
   */
  static simplifyRadialDistance(points = [], tolerance = 0.01) {
    if (points.length <= 2) return [...points];

    const tolSq = tolerance * tolerance;
    const simplified = [points[0]];
    let prev = points[0];

    for (let i = 1; i < points.length; i++) {
      const curr = points[i];
      const dx = curr[0] - prev[0];
      const dy = curr[1] - prev[1];
      const distSq = dx * dx + dy * dy;

      if (distSq >= tolSq || i === points.length - 1) {
        simplified.push(curr);
        prev = curr;
      }
    }

    return simplified;
  }

  // --- Visvalingam-Whyatt Simplification, Ear Clipping & Point-in-Polygon Solvers ---

  /**
   * Visvalingam-Whyatt area-based polyline decimation.
   * Eliminates vertices forming triangles with effective area less than minArea.
   * @param {Array<[number, number]>} points - Array of [x, y] coordinates
   * @param {number} [minArea=0.001] - Minimum effective triangle area threshold
   * @returns {Array<[number, number]>} Simplified points array
   */
  static simplifyVisvalingamWhyatt(points = [], minArea = 0.001) {
    if (!points || points.length <= 2) return [...(points || [])];

    let pts = points.map(p => [...p]);

    const computeTriangleArea = (p1, p2, p3) => {
      return 0.5 * Math.abs(p1[0] * (p2[1] - p3[1]) + p2[0] * (p3[1] - p1[1]) + p3[0] * (p1[1] - p2[1]));
    };

    while (pts.length > 2) {
      let minIdx = -1;
      let smallestArea = Infinity;

      for (let i = 1; i < pts.length - 1; i++) {
        const area = computeTriangleArea(pts[i - 1], pts[i], pts[i + 1]);
        if (area < smallestArea) {
          smallestArea = area;
          minIdx = i;
        }
      }

      if (minIdx !== -1 && smallestArea < minArea) {
        pts.splice(minIdx, 1);
      } else {
        break;
      }
    }

    return pts;
  }

  /**
   * Test whether a 2D point [x, y] lies inside a polygon ring using Ray-Casting.
   * @param {[number, number]} point - [x, y] test coordinate
   * @param {Array<[number, number]>} ring - Polygon vertices [[x, y], ...]
   * @returns {boolean} True if point is inside polygon
   */
  static isPointInPolygon(point, ring = []) {
    if (!point || !ring || ring.length < 3) return false;
    const [px, py] = point;
    let inside = false;
    const n = ring.length;

    for (let i = 0, j = n - 1; i < n; j = i++) {
      const [xi, yi] = ring[i];
      const [xj, yj] = ring[j];

      const intersect = ((yi > py) !== (yj > py)) &&
        (px < (xj - xi) * (py - yi) / (yj - yi) + xi);

      if (intersect) inside = !inside;
    }

    return inside;
  }

  /**
   * Triangulate a simple non-self-intersecting 2D polygon ring using Ear Clipping.
   * @param {Array<[number, number]>} ring - Polygon vertices [[x, y], ...]
   * @returns {Array<[[number, number], [number, number], [number, number]]>} Array of triangle triplets
   */
  static triangulatePolygonEarClipping(ring = []) {
    if (!ring || ring.length < 3) return [];
    if (ring.length === 3) return [[ring[0], ring[1], ring[2]]];

    // Ensure CCW orientation
    let pts = ring.map((p, idx) => ({ p, idx }));
    let signedArea = 0;
    for (let i = 0; i < pts.length; i++) {
      const p1 = pts[i].p;
      const p2 = pts[(i + 1) % pts.length].p;
      signedArea += p1[0] * p2[1] - p2[0] * p1[1];
    }
    if (signedArea < 0) {
      pts.reverse();
    }

    const isConvex = (a, b, c) => {
      return (b[0] - a[0]) * (c[1] - a[1]) - (b[1] - a[1]) * (c[0] - a[0]) > 0;
    };

    const isPointInTriangle = (pt, a, b, c) => {
      const v0 = [c[0] - a[0], c[1] - a[1]];
      const v1 = [b[0] - a[0], b[1] - a[1]];
      const v2 = [pt[0] - a[0], pt[1] - a[1]];

      const dot00 = v0[0] * v0[0] + v0[1] * v0[1];
      const dot01 = v0[0] * v1[0] + v0[1] * v1[1];
      const dot02 = v0[0] * v2[0] + v0[1] * v2[1];
      const dot11 = v1[0] * v1[0] + v1[1] * v1[1];
      const dot12 = v1[0] * v2[0] + v1[1] * v2[1];

      const denom = dot00 * dot11 - dot01 * dot01;
      if (Math.abs(denom) < 1e-10) return false;
      const u = (dot11 * dot02 - dot01 * dot12) / denom;
      const v = (dot00 * dot12 - dot01 * dot02) / denom;

      return (u >= 0) && (v >= 0) && (u + v <= 1);
    };

    const triangles = [];
    let count = pts.length;

    while (count > 3) {
      let earFound = false;

      for (let i = 0; i < count; i++) {
        const prev = pts[(i - 1 + count) % count].p;
        const curr = pts[i].p;
        const next = pts[(i + 1) % count].p;

        if (!isConvex(prev, curr, next)) continue;

        let hasPointInside = false;
        for (let j = 0; j < count; j++) {
          if (j === (i - 1 + count) % count || j === i || j === (i + 1) % count) continue;
          if (isPointInTriangle(pts[j].p, prev, curr, next)) {
            hasPointInside = true;
            break;
          }
        }

        if (!hasPointInside) {
          triangles.push([prev, curr, next]);
          pts.splice(i, 1);
          count--;
          earFound = true;
          break;
        }
      }

      if (!earFound) {
        // Fallback break to prevent infinite loop on degenerate polygons
        break;
      }
    }

    if (pts.length === 3) {
      triangles.push([pts[0].p, pts[1].p, pts[2].p]);
    }

    return triangles;
  }
}






