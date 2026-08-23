import { EVENTS } from '../../constants.js';
import { computeBufferPolygon } from '../../util/geo.js';

/**
 * ShapeLayer provides full-featured vector drawing and editing.
 * Supports points, lines, polygons, circles, rectangles with
 * attribute dictionaries and style customization.
 */
export class ShapeLayer {
  constructor(map) {
    this.map = map;
    this.featureGroup = new L.FeatureGroup();
    this.isActive = false;
    this.shapes = []; // Array of { id, layer, type, attributes, style }
    this.selectedId = null;
    this.nextId = 1;
    this.drawHandler = null;
    this._onCreated = this._onCreated.bind(this);
    this._onEdited = this._onEdited.bind(this);
    this._onDeleted = this._onDeleted.bind(this);
  }

  /**
   * Activate the shape layer and enable draw controls.
   */
  activate() {
    if (this.isActive) return;
    this.isActive = true;
    this.featureGroup.addTo(this.map);
    this.map.on('draw:created', this._onCreated);
    this.map.on('draw:edited', this._onEdited);
    this.map.on('draw:deleted', this._onDeleted);
  }

  /**
   * Deactivate the shape layer.
   */
  deactivate() {
    if (!this.isActive) return;
    this.isActive = false;
    this.stopDrawing();
    this.map.off('draw:created', this._onCreated);
    this.map.off('draw:edited', this._onEdited);
    this.map.off('draw:deleted', this._onDeleted);
  }

  /**
   * Start drawing a specific shape type.
   * @param {string} type - 'marker', 'polyline', 'polygon', 'circle', 'rectangle'
   * @param {object} [options] - Leaflet draw options
   */
  startDrawing(type, options = {}) {
    this.stopDrawing();

    const defaultStyle = {
      color: '#4dabf7',
      weight: 2,
      opacity: 0.9,
      fillColor: '#4dabf7',
      fillOpacity: 0.2
    };

    const drawOptions = { ...defaultStyle, ...options };

    const DrawHandlers = {
      marker: L.Draw.Marker,
      polyline: L.Draw.Polyline,
      polygon: L.Draw.Polygon,
      circle: L.Draw.Circle,
      rectangle: L.Draw.Rectangle
    };

    const Handler = DrawHandlers[type];
    if (!Handler) {
      console.warn(`Unknown draw type: ${type}`);
      return;
    }

    this.drawHandler = new Handler(this.map, {
      shapeOptions: drawOptions
    });
    this.drawHandler.enable();
  }

  /**
   * Stop any active drawing.
   */
  stopDrawing() {
    if (this.drawHandler) {
      this.drawHandler.disable();
      this.drawHandler = null;
    }
  }

  /**
   * Handle draw:created event from Leaflet.Draw.
   * @param {object} e - Leaflet event
   */
  _onCreated(e) {
    const layer = e.layer;
    const type = e.layerType;
    const id = this.nextId++;

    const shape = {
      id,
      layer,
      type,
      attributes: {
        name: `Shape ${id}`,
        description: ''
      },
      style: this._getLayerStyle(layer)
    };

    // Store reference on the layer
    layer._shapeId = id;

    // Add click handler for selection
    layer.on('click', () => this.selectShape(id));
    layer.on('contextmenu', (ev) => {
      L.DomEvent.stopPropagation(ev);
      this.selectShape(id);
    });

    this.featureGroup.addLayer(layer);
    this.shapes.push(shape);

    document.dispatchEvent(new CustomEvent(EVENTS.SHAPE_CREATED, { detail: shape }));
  }

  /**
   * Handle draw:edited event.
   * @param {object} e
   */
  _onEdited(e) {
    e.layers.eachLayer(layer => {
      const shape = this.shapes.find(s => s.layer === layer);
      if (shape) {
        document.dispatchEvent(new CustomEvent(EVENTS.SHAPE_UPDATED, { detail: shape }));
      }
    });
  }

  /**
   * Handle draw:deleted event.
   * @param {object} e
   */
  _onDeleted(e) {
    e.layers.eachLayer(layer => {
      const idx = this.shapes.findIndex(s => s.layer === layer);
      if (idx >= 0) {
        const shape = this.shapes.splice(idx, 1)[0];
        document.dispatchEvent(new CustomEvent(EVENTS.SHAPE_DELETED, { detail: shape }));
      }
    });
  }

  /**
   * Select a shape by ID.
   * @param {number} id
   */
  selectShape(id) {
    // Deselect previous
    if (this.selectedId != null) {
      const prev = this.shapes.find(s => s.id === this.selectedId);
      if (prev && prev.layer.setStyle) {
        prev.layer.setStyle({ weight: prev.style?.weight || 2 });
      }
    }

    this.selectedId = id;
    const shape = this.shapes.find(s => s.id === id);
    if (shape && shape.layer.setStyle) {
      shape.layer.setStyle({ weight: 4 });
    }

    document.dispatchEvent(new CustomEvent(EVENTS.SHAPE_SELECTED, { detail: shape }));
  }

  /**
   * Delete a shape by ID.
   * @param {number} id
   */
  deleteShape(id) {
    const idx = this.shapes.findIndex(s => s.id === id);
    if (idx < 0) return;
    const shape = this.shapes.splice(idx, 1)[0];
    this.featureGroup.removeLayer(shape.layer);
    if (this.selectedId === id) this.selectedId = null;
    document.dispatchEvent(new CustomEvent(EVENTS.SHAPE_DELETED, { detail: shape }));
  }

  /**
   * Update attributes for a shape.
   * @param {number} id
   * @param {object} attrs - Key-value pairs to merge
   */
  updateAttributes(id, attrs) {
    const shape = this.shapes.find(s => s.id === id);
    if (shape) {
      Object.assign(shape.attributes, attrs);
      document.dispatchEvent(new CustomEvent(EVENTS.SHAPE_UPDATED, { detail: shape }));
    }
  }

  /**
   * Update style for a shape.
   * @param {number} id
   * @param {object} style
   */
  updateStyle(id, style) {
    const shape = this.shapes.find(s => s.id === id);
    if (shape) {
      shape.style = { ...shape.style, ...style };
      if (shape.layer.setStyle) {
        shape.layer.setStyle(style);
      }
    }
  }

  /**
   * Create a geodesic buffer polygon around an existing shape.
   * @param {number} shapeId - ID of source shape
   * @param {number} [radiusKm=10] - Buffer radius in km
   * @returns {object|null} - Created buffer shape
   */
  createBuffer(shapeId, radiusKm = 10) {
    const shape = this.shapes.find(s => s.id === shapeId);
    if (!shape || !shape.layer) return null;

    let rawCoords = [];
    if (shape.layer.getLatLng) {
      const ll = shape.layer.getLatLng();
      rawCoords = [ll.lat, ll.lng];
    } else if (shape.layer.getLatLngs) {
      const lls = shape.layer.getLatLngs();
      const flat = Array.isArray(lls[0]) ? lls[0] : lls;
      rawCoords = flat.map(p => [p.lat, p.lng]);
    }

    const bufferCoords = computeBufferPolygon(rawCoords, radiusKm, 'mars');
    if (bufferCoords.length === 0) return null;

    const bufferLayer = L.polygon(bufferCoords, {
      color: '#f59e0b',
      weight: 2,
      fillColor: '#f59e0b',
      fillOpacity: 0.2,
      dashArray: '3,3'
    });

    const newId = this.nextId++;
    const bufferShape = {
      id: newId,
      layer: bufferLayer,
      type: 'polygon',
      attributes: {
        name: `Buffer (${radiusKm} km) of ${shape.attributes?.name || 'Shape ' + shapeId}`,
        description: `Geodesic buffer zone of ${radiusKm} km`
      },
      style: this._getLayerStyle(bufferLayer)
    };

    bufferLayer._shapeId = newId;
    bufferLayer.on('click', () => this.selectShape(newId));
    this.featureGroup.addLayer(bufferLayer);
    this.shapes.push(bufferShape);

    document.dispatchEvent(new CustomEvent(EVENTS.SHAPE_CREATED, { detail: bufferShape }));
    return bufferShape;
  }

  /**
   * Clear all shapes.
   */
  clearAll() {
    this.shapes = [];
    this.selectedId = null;
    this.featureGroup.clearLayers();
  }

  /**
   * Export all shapes as GeoJSON.
   * @returns {object} - GeoJSON FeatureCollection
   */
  toGeoJSON() {
    const features = this.shapes.map(shape => {
      const gj = shape.layer.toGeoJSON();
      gj.properties = {
        id: shape.id,
        type: shape.type,
        ...shape.attributes,
        _style: shape.style
      };
      return gj;
    });

    return {
      type: 'FeatureCollection',
      features
    };
  }

  /**
   * Import shapes from GeoJSON.
   * @param {object} geojson - GeoJSON FeatureCollection or Feature
   */
  fromGeoJSON(geojson) {
    try {
      const geoLayer = L.geoJSON(geojson, {
        style: (feature) => {
          return feature.properties?._style || {
            color: '#4dabf7',
            weight: 2,
            fillOpacity: 0.2
          };
        },
        pointToLayer: (feature, latlng) => {
          return L.marker(latlng);
        },
        onEachFeature: (feature, layer) => {
          const id = this.nextId++;
          const shape = {
            id,
            layer,
            type: this._detectType(layer),
            attributes: { ...feature.properties },
            style: feature.properties?._style || this._getLayerStyle(layer)
          };

          // Remove internal props from attributes
          delete shape.attributes._style;
          delete shape.attributes.id;
          delete shape.attributes.type;

          layer._shapeId = id;
          layer.on('click', () => this.selectShape(id));
          this.featureGroup.addLayer(layer);
          this.shapes.push(shape);
        }
      });

      document.dispatchEvent(new CustomEvent(EVENTS.SHAPES_IMPORTED, {
        detail: { count: this.shapes.length }
      }));
    } catch (err) {
      console.error('GeoJSON import error:', err);
    }
  }

  /**
   * Import shapes from CSV (lat, lon, name, description).
   * @param {string} csvText - CSV content
   */
  fromCSV(csvText) {
    const lines = csvText.trim().split('\n');
    if (lines.length < 2) return;

    const headers = lines[0].split(',').map(h => h.trim().toLowerCase());
    const latIdx = headers.findIndex(h => h === 'lat' || h === 'latitude');
    const lonIdx = headers.findIndex(h => h === 'lon' || h === 'lng' || h === 'longitude');
    const nameIdx = headers.findIndex(h => h === 'name');

    if (latIdx < 0 || lonIdx < 0) {
      console.error('CSV must have lat/latitude and lon/longitude columns');
      return;
    }

    for (let i = 1; i < lines.length; i++) {
      const cols = lines[i].split(',').map(c => c.trim());
      const lat = parseFloat(cols[latIdx]);
      const lon = parseFloat(cols[lonIdx]);
      if (isNaN(lat) || isNaN(lon)) continue;

      const marker = L.marker([lat, lon]);
      const id = this.nextId++;
      const attrs = {};
      headers.forEach((h, j) => {
        if (j !== latIdx && j !== lonIdx) {
          attrs[h] = cols[j] || '';
        }
      });

      const shape = { id, layer: marker, type: 'marker', attributes: attrs, style: {} };
      marker._shapeId = id;
      marker.on('click', () => this.selectShape(id));
      this.featureGroup.addLayer(marker);
      this.shapes.push(shape);
    }

    document.dispatchEvent(new CustomEvent(EVENTS.SHAPES_IMPORTED, {
      detail: { count: this.shapes.length }
    }));
  }

  /**
   * Import from KML.
   * @param {string} kmlText - KML XML content
   */
  fromKML(kmlText) {
    try {
      const parser = new DOMParser();
      const doc = parser.parseFromString(kmlText, 'text/xml');
      const placemarks = doc.querySelectorAll('Placemark');

      placemarks.forEach(pm => {
        const name = pm.querySelector('name')?.textContent || '';
        const desc = pm.querySelector('description')?.textContent || '';

        // Points
        const point = pm.querySelector('Point coordinates');
        if (point) {
          const [lon, lat, alt] = point.textContent.trim().split(',').map(Number);
          if (!isNaN(lat) && !isNaN(lon)) {
            const marker = L.marker([lat, lon]);
            const id = this.nextId++;
            const shape = {
              id, layer: marker, type: 'marker',
              attributes: { name, description: desc },
              style: {}
            };
            marker._shapeId = id;
            marker.on('click', () => this.selectShape(id));
            this.featureGroup.addLayer(marker);
            this.shapes.push(shape);
          }
          return;
        }

        // LineStrings
        const lineCoords = pm.querySelector('LineString coordinates');
        if (lineCoords) {
          const latlngs = this._parseKMLCoords(lineCoords.textContent);
          if (latlngs.length > 0) {
            const polyline = L.polyline(latlngs, { color: '#4dabf7', weight: 2 });
            const id = this.nextId++;
            const shape = {
              id, layer: polyline, type: 'polyline',
              attributes: { name, description: desc },
              style: { color: '#4dabf7', weight: 2 }
            };
            polyline._shapeId = id;
            polyline.on('click', () => this.selectShape(id));
            this.featureGroup.addLayer(polyline);
            this.shapes.push(shape);
          }
          return;
        }

        // Polygons
        const polyCoords = pm.querySelector('Polygon outerBoundaryIs LinearRing coordinates');
        if (polyCoords) {
          const latlngs = this._parseKMLCoords(polyCoords.textContent);
          if (latlngs.length > 0) {
            const polygon = L.polygon(latlngs, { color: '#4dabf7', weight: 2, fillOpacity: 0.2 });
            const id = this.nextId++;
            const shape = {
              id, layer: polygon, type: 'polygon',
              attributes: { name, description: desc },
              style: { color: '#4dabf7', weight: 2, fillOpacity: 0.2 }
            };
            polygon._shapeId = id;
            polygon.on('click', () => this.selectShape(id));
            this.featureGroup.addLayer(polygon);
            this.shapes.push(shape);
          }
        }
      });

      document.dispatchEvent(new CustomEvent(EVENTS.SHAPES_IMPORTED, {
        detail: { count: this.shapes.length }
      }));
    } catch (err) {
      console.error('KML import error:', err);
    }
  }

  /**
   * Parse KML coordinate string.
   * @param {string} coordStr
   * @returns {Array<[number, number]>}
   */
  _parseKMLCoords(coordStr) {
    return coordStr.trim().split(/\s+/).map(c => {
      const [lon, lat] = c.split(',').map(Number);
      return [lat, lon];
    }).filter(([lat, lon]) => !isNaN(lat) && !isNaN(lon));
  }

  /**
   * Export current shapes to GeoJSON file download.
   */
  downloadGeoJSON() {
    const gj = this.toGeoJSON();
    const blob = new Blob([JSON.stringify(gj, null, 2)], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `shapes_${Date.now()}.geojson`;
    a.click();
    URL.revokeObjectURL(url);
  }

  /**
   * Export current shapes to KML file download.
   */
  downloadKML() {
    /**
     * Escape XML special characters to produce valid KML.
     * @param {string} str - Raw string.
     * @returns {string} XML-safe string.
     */
    const escapeXml = (str) =>
      str.replace(/&/g, '&amp;')
         .replace(/</g, '&lt;')
         .replace(/>/g, '&gt;')
         .replace(/"/g, '&quot;');

    let kml = '<?xml version="1.0" encoding="UTF-8"?>\n';
    kml += '<kml xmlns="http://www.opengis.net/kml/2.2">\n<Document>\n';
    kml += `<name>jsMars Shapes</name>\n`;

    this.shapes.forEach(shape => {
      const gj = shape.layer.toGeoJSON();
      const safeName = escapeXml(shape.attributes.name || '');
      const safeDesc = escapeXml(shape.attributes.description || '');
      kml += '<Placemark>\n';
      kml += `  <name>${safeName}</name>\n`;
      kml += `  <description>${safeDesc}</description>\n`;

      if (gj.geometry.type === 'Point') {
        const [lon, lat] = gj.geometry.coordinates;
        kml += `  <Point><coordinates>${lon},${lat},0</coordinates></Point>\n`;
      } else if (gj.geometry.type === 'LineString') {
        const coords = gj.geometry.coordinates.map(c => `${c[0]},${c[1]},0`).join(' ');
        kml += `  <LineString><coordinates>${coords}</coordinates></LineString>\n`;
      } else if (gj.geometry.type === 'Polygon') {
        const coords = gj.geometry.coordinates[0].map(c => `${c[0]},${c[1]},0`).join(' ');
        kml += `  <Polygon><outerBoundaryIs><LinearRing><coordinates>${coords}</coordinates></LinearRing></outerBoundaryIs></Polygon>\n`;
      }

      kml += '</Placemark>\n';
    });

    kml += '</Document>\n</kml>';

    const blob = new Blob([kml], { type: 'application/vnd.google-earth.kml+xml' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `shapes_${Date.now()}.kml`;
    a.click();
    URL.revokeObjectURL(url);
  }

  /**
   * Detect layer type from Leaflet layer.
   * @param {L.Layer} layer
   * @returns {string}
   */
  _detectType(layer) {
    if (layer instanceof L.Marker) return 'marker';
    if (layer instanceof L.Circle) return 'circle';
    if (layer instanceof L.Rectangle) return 'rectangle';
    if (layer instanceof L.Polygon) return 'polygon';
    if (layer instanceof L.Polyline) return 'polyline';
    return 'unknown';
  }

  /**
   * Get the current style of a layer.
   * @param {L.Layer} layer
   * @returns {object}
   */
  _getLayerStyle(layer) {
    if (layer.options) {
      return {
        color: layer.options.color,
        weight: layer.options.weight,
        opacity: layer.options.opacity,
        fillColor: layer.options.fillColor,
        fillOpacity: layer.options.fillOpacity
      };
    }
    return {};
  }

  /**
   * Get the Leaflet.Draw edit handler for inline editing.
   * @returns {L.EditToolbar.Edit}
   */
  enableEditing() {
    const editHandler = new L.EditToolbar.Edit(this.map, {
      featureGroup: this.featureGroup
    });
    editHandler.enable();
    return editHandler;
  }

  /**
   * Get the Leaflet.Draw delete handler.
   * @returns {L.EditToolbar.Delete}
   */
  enableDeleting() {
    const deleteHandler = new L.EditToolbar.Delete(this.map, {
      featureGroup: this.featureGroup
    });
    deleteHandler.enable();
    return deleteHandler;
  }
}
