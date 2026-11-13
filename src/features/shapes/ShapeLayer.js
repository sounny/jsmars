import { EVENTS } from '../../constants.js';
import { jmarsState } from '../../jmars-state.js';
import { normalizeBodyKey } from '../../util/body.js';
import { computeBufferPolygon, computeEllipsePolygon, getPlanetaryRadius } from '../../util/geo.js';
import { applyLeafletDrawPatch } from '../../util/leaflet-draw-patch.js';
import { applyGeologicPatternToLayer } from './StyleEditor.js';
import { ShapeIO } from './ShapeIO.js';

/**
 * ShapeLayer provides full-featured vector drawing, geologic unit styling,
 * multi-body geodesic buffering, attribute dictionaries, and GIS serialization.
 */
export class ShapeLayer {
  /**
   * @param {L.Map} map - Leaflet map instance
   * @param {object} [options]
   * @param {import('../../jmars-vectors.js').JMARSVectors} [options.vectors] - Shared JMARSVectors instance
   * @param {L.FeatureGroup} [options.featureGroup] - Optional explicit FeatureGroup
   * @param {import('./StyleEditor.js').StyleEditor} [options.styleEditor] - Optional explicit StyleEditor
   */
  constructor(map, options = {}) {
    this.map = map;
    if (options.vectors) {
      this.featureGroup = options.vectors.featureGroup;
      this.styleEditor = options.vectors.styleEditor || null;
      options.vectors.delegateToShapeLayer(this);
    } else {
      this.featureGroup = options.featureGroup || new L.FeatureGroup();
      this.styleEditor = options.styleEditor || null;
    }
    this.isActive = false;
    this.shapes = []; // Array of { id, layer, type, attributes, style }
    this.selectedId = null;
    this.nextId = 1;
    this.drawHandler = null;
    this.activeDrawType = null;
    this._onCreated = this._onCreated.bind(this);
    this._onEdited = this._onEdited.bind(this);
    this._onDeleted = this._onDeleted.bind(this);
    this._onDrawStop = this._onDrawStop.bind(this);
  }

  /**
   * Activate the shape layer and enable draw event listeners.
   */
  activate() {
    if (this.isActive) return;
    this.isActive = true;
    if (this.map && !this.map.hasLayer(this.featureGroup)) {
      this.featureGroup.addTo(this.map);
    }
    this.map.on('draw:created', this._onCreated);
    this.map.on('draw:edited', this._onEdited);
    this.map.on('draw:deleted', this._onDeleted);
    this.map.on('draw:drawstop', this._onDrawStop);
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
    this.map.off('draw:drawstop', this._onDrawStop);
  }

  /**
   * Clear active draw handler state when Leaflet.Draw completes or stops.
   * @private
   */
  _onDrawStop() {
    if (!this.drawHandler && !this.activeDrawType) return;
    this.drawHandler = null;
    this.activeDrawType = null;
    document.dispatchEvent(new CustomEvent(EVENTS.TOOL_DEACTIVATED, { detail: { tool: 'shapes' } }));
  }

  /**
   * Start drawing a specific shape type.
   * @param {string} type - 'marker', 'polyline', 'polygon', 'circle', 'rectangle'
   * @param {object} [options] - Leaflet draw shape options
   */
  startDrawing(type, options = {}) {
    applyLeafletDrawPatch();
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
      marker: L.Draw?.Marker,
      polyline: L.Draw?.Polyline,
      polygon: L.Draw?.Polygon,
      circle: L.Draw?.Circle,
      rectangle: L.Draw?.Rectangle
    };

    const Handler = DrawHandlers[type];
    if (!Handler) {
      console.warn(`Unknown draw type: ${type}`);
      return;
    }

    this.activeDrawType = type;
    this.drawHandler = new Handler(this.map, {
      shapeOptions: drawOptions
    });
    this.drawHandler.enable();
    document.dispatchEvent(new CustomEvent(EVENTS.TOOL_ACTIVATED, {
      detail: { tool: 'shapes', shapeType: type }
    }));
  }

  /**
   * Stop any active drawing.
   */
  stopDrawing() {
    const hadHandler = Boolean(this.drawHandler || this.activeDrawType);
    if (this.drawHandler) {
      const hdl = this.drawHandler;
      this.drawHandler = null;
      this.activeDrawType = null;
      hdl.disable();
    } else {
      this.activeDrawType = null;
    }
    if (hadHandler) {
      document.dispatchEvent(new CustomEvent(EVENTS.TOOL_DEACTIVATED, { detail: { tool: 'shapes' } }));
    }
  }

  /**
   * Register interaction and pattern hooks for a shape and add it to the FeatureGroup.
   * @param {object} shape
   * @private
   */
  _registerShape(shape) {
    const { id, layer } = shape;
    layer._shapeId = id;

    if (shape.style?.pattern) {
      applyGeologicPatternToLayer(layer, shape.style.pattern, shape.style.fillColor);
    } else if (typeof layer.setStyle === 'function') {
      applyGeologicPatternToLayer(layer, '');
    }

    layer.on('click', () => this.selectShape(id));
    layer.on('contextmenu', (ev) => {
      if (L.DomEvent) L.DomEvent.stopPropagation(ev);
      if (ev?.originalEvent) ev.originalEvent.preventDefault();
      this.selectShape(id);
      if (this.styleEditor && typeof layer.setStyle === 'function') {
        this.styleEditor.open(layer, ev.containerPoint || { x: 100, y: 100 });
      }
    });

    if (!this.featureGroup.hasLayer(layer)) {
      this.featureGroup.addLayer(layer);
    }
    // Re-apply pattern once mounted in SVG DOM
    if (shape.style?.pattern) {
      applyGeologicPatternToLayer(layer, shape.style.pattern, shape.style.fillColor);
    }
    this.shapes.push(shape);
  }

  /**
   * Handle draw:created event from Leaflet.Draw.
   * Ignores geometries created by exclusive measurement, sampling, or profile tools.
   * @param {object} e - Leaflet event
   */
  _onCreated(e) {
    if (this.map?._exclusiveDrawOwner || e?._handledByTool) {
      return;
    }
    const layer = e.layer;
    const type = e.layerType || this._detectType(layer);
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

    this._registerShape(shape);
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
        if (this.selectedId === shape.id) this.selectedId = null;
        document.dispatchEvent(new CustomEvent(EVENTS.SHAPE_DELETED, { detail: shape }));
      }
    });
  }

  /**
   * Select a shape by ID.
   * @param {number} id
   */
  selectShape(id) {
    if (this.selectedId != null) {
      const prev = this.shapes.find(s => s.id === this.selectedId);
      if (prev && prev.layer.setStyle) {
        prev.layer.setStyle({ weight: prev.style?.weight || 2 });
      }
    }

    this.selectedId = id;
    const shape = this.shapes.find(s => s.id === id);
    if (shape && shape.layer.setStyle) {
      shape.layer.setStyle({ weight: Math.max(4, (shape.style?.weight || 2) + 2) });
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
   * Synchronize stored style metadata after external StyleEditor changes.
   * @param {number} id
   * @param {object} styles
   */
  syncStyleFromLayer(id, styles) {
    const shape = this.shapes.find(s => s.id === id);
    if (!shape) return;
    shape.style = { ...shape.style, ...styles };
    document.dispatchEvent(new CustomEvent(EVENTS.SHAPE_UPDATED, { detail: shape }));
  }

  /**
   * Update style (including optional geologic fill pattern) for a shape.
   * @param {number} id
   * @param {object} style
   */
  updateStyle(id, style) {
    const shape = this.shapes.find(s => s.id === id);
    if (!shape) return;

    shape.style = { ...shape.style, ...style };
    if (shape.layer.setStyle) {
      shape.layer.setStyle(style);
      applyGeologicPatternToLayer(
        shape.layer,
        shape.style.pattern || '',
        shape.style.fillColor
      );
    }
    document.dispatchEvent(new CustomEvent(EVENTS.SHAPE_UPDATED, { detail: shape }));
  }

  /**
   * Create a geodesic buffer polygon around an existing shape using the active planetary body's radius.
   * @param {number} shapeId - ID of source shape
   * @param {number} [radiusKm=10] - Buffer radius in km
   * @param {string|null} [bodyOverride=null] - Optional body key override
   * @returns {object|null} - Created buffer shape
   */
  createBuffer(shapeId, radiusKm = 10, bodyOverride = null) {
    const shape = this.shapes.find(s => s.id === shapeId);
    if (!shape || !shape.layer || !(radiusKm > 0)) return null;

    const body = normalizeBodyKey(bodyOverride || jmarsState.get('body') || 'mars');
    let effectiveRadiusKm = radiusKm;
    let rawCoords = [];

    if (shape.type === 'circle' && shape.layer.getLatLng && shape.layer.getRadius) {
      const ll = shape.layer.getLatLng();
      rawCoords = [ll.lat, ll.lng];
      const circleRadiusKm = (shape.layer.getRadius() / 1000) * (getPlanetaryRadius(body, 'mean') / 6371.0);
      effectiveRadiusKm = circleRadiusKm + radiusKm;
    } else if (shape.layer.getLatLng) {
      const ll = shape.layer.getLatLng();
      rawCoords = [ll.lat, ll.lng];
    } else if (shape.layer.getLatLngs) {
      const lls = shape.layer.getLatLngs();
      const flat = Array.isArray(lls[0]) ? lls[0] : lls;
      rawCoords = flat.map(p => [p.lat, p.lng]);
    }

    const bufferCoords = computeBufferPolygon(rawCoords, effectiveRadiusKm, body);
    if (bufferCoords.length === 0) return null;

    const bufferLayer = L.polygon(bufferCoords, {
      color: '#f59e0b',
      weight: 2,
      opacity: 0.9,
      fillColor: '#f59e0b',
      fillOpacity: 0.2,
      dashArray: '4,4'
    });

    const newId = this.nextId++;
    const bufferShape = {
      id: newId,
      layer: bufferLayer,
      type: 'polygon',
      attributes: {
        name: `Buffer (${radiusKm} km) of ${shape.attributes?.name || 'Shape ' + shapeId}`,
        description: `Geodesic buffer zone of ${radiusKm} km (${body})`
      },
      style: this._getLayerStyle(bufferLayer)
    };

    this._registerShape(bufferShape);
    document.dispatchEvent(new CustomEvent(EVENTS.SHAPE_CREATED, { detail: bufferShape }));
    return bufferShape;
  }

  /**
   * Clear all shapes and reset state cleanly.
   */
  clearAll() {
    this.shapes = [];
    this.selectedId = null;
    this.featureGroup.clearLayers();
    document.dispatchEvent(new CustomEvent(EVENTS.SHAPES_IMPORTED, { detail: { count: 0 } }));
  }

  /**
   * Export all shapes as GeoJSON (preserving attributes, styles, geologic patterns, and circle radii).
   * @returns {object} - GeoJSON FeatureCollection
   */
  toGeoJSON() {
    const features = this.shapes.map(shape => {
      const gj = shape.layer.toGeoJSON();
      const props = {
        id: shape.id,
        type: shape.type,
        ...shape.attributes,
        _style: shape.style
      };
      if (shape.type === 'circle' && typeof shape.layer.getRadius === 'function') {
        props._circleRadiusMeters = shape.layer.getRadius();
      }
      gj.properties = props;
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
    if (!geojson) return;
    try {
      L.geoJSON(geojson, {
        style: (feature) => {
          return feature.properties?._style || {
            color: '#4dabf7',
            weight: 2,
            opacity: 0.9,
            fillColor: '#4dabf7',
            fillOpacity: 0.2
          };
        },
        pointToLayer: (feature, latlng) => {
          if (feature.properties?._circleRadiusMeters > 0) {
            return L.circle(latlng, {
              radius: feature.properties._circleRadiusMeters,
              ...(feature.properties._style || { color: '#4dabf7', weight: 2, fillOpacity: 0.2 })
            });
          }
          return L.marker(latlng);
        },
        onEachFeature: (feature, layer) => {
          const id = this.nextId++;
          const savedStyle = feature.properties?._style || this._getLayerStyle(layer);
          const shape = {
            id,
            layer,
            type: feature.properties?.type === 'circle' && layer instanceof L.Circle
              ? 'circle'
              : this._detectType(layer),
            attributes: { ...(feature.properties || {}) },
            style: { ...savedStyle }
          };

          // Remove internal serialization properties from user attributes
          delete shape.attributes._style;
          delete shape.attributes._circleRadiusMeters;
          delete shape.attributes.id;
          delete shape.attributes.type;
          if (!shape.attributes.name) {
            shape.attributes.name = shape.attributes.NAME || `Shape ${id}`;
          }

          this._registerShape(shape);
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
   * Import shapes from Well-Known Text (.wkt), supporting single or multi-line WKT geometries.
   * @param {string} wktText
   */
  fromWKT(wktText) {
    if (!wktText || typeof wktText !== 'string') return;
    const lines = wktText
      .split(/\r?\n/)
      .map(l => l.trim())
      .filter(l => l && !l.startsWith('#') && !l.startsWith('--'));

    const features = [];
    lines.forEach((line, idx) => {
      let rawWkt = line;
      let label = '';
      // Support optional "name\tWKT" or "WKT;name" conventions
      if (line.includes('\t')) {
        const parts = line.split('\t');
        const geomCandidate = parts.find(p => /^(MULTI)?(POINT|LINESTRING|POLYGON)/i.test(p.trim()));
        if (geomCandidate) {
          rawWkt = geomCandidate.trim();
          label = parts.find(p => p.trim() !== rawWkt)?.trim() || '';
        }
      }
      const geom = ShapeIO.parseWKT(rawWkt);
      if (geom) {
        features.push({
          type: 'Feature',
          geometry: geom,
          properties: {
            name: label || `WKT Feature ${idx + 1}`,
            description: ''
          }
        });
      }
    });

    if (features.length > 0) {
      this.fromGeoJSON({ type: 'FeatureCollection', features });
    }
  }

  /**
   * Serialize all current shapes as newline-delimited WKT strings.
   * Circles are approximated as 48-vertex geodesic polygons on the active body.
   * @returns {string}
   */
  toWKTString() {
    const body = normalizeBodyKey(jmarsState.get('body') || 'mars');
    return this.shapes
      .map(shape => {
        if (shape.type === 'circle' && shape.layer.getLatLng && shape.layer.getRadius) {
          const ll = shape.layer.getLatLng();
          const radiusKm = (shape.layer.getRadius() / 1000) * (getPlanetaryRadius(body, 'mean') / 6371.0);
          const ringLatLon = computeEllipsePolygon(ll.lat, ll.lng, radiusKm, radiusKm, 0, body, 48);
          const ringLonLat = ringLatLon.map(([lat, lon]) => [Number(lon.toFixed(6)), Number(lat.toFixed(6))]);
          return ShapeIO.toWKT({ type: 'Polygon', coordinates: [ringLonLat] });
        }
        const gj = shape.layer.toGeoJSON();
        return ShapeIO.toWKT(gj.geometry);
      })
      .filter(Boolean)
      .join('\n');
  }

  /**
   * Export current shapes to a .wkt file download.
   */
  downloadWKT() {
    const wkt = this.toWKTString();
    if (!wkt) return;
    const blob = new Blob([wkt], { type: 'text/plain;charset=utf-8' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `shapes_${Date.now()}.wkt`;
    a.click();
    URL.revokeObjectURL(url);
  }

  /**
   * Import shapes from CSV (lat, lon, name, description, and arbitrary extra columns).
   * @param {string} csvText - CSV content
   */
  fromCSV(csvText) {
    const lines = csvText.trim().split(/\r?\n/);
    if (lines.length < 2) return;

    const headers = lines[0].split(',').map(h => h.trim().toLowerCase());
    const latIdx = headers.findIndex(h => h === 'lat' || h === 'latitude');
    const lonIdx = headers.findIndex(h => h === 'lon' || h === 'lng' || h === 'longitude');

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
      if (!attrs.name) attrs.name = `Point ${id}`;

      const shape = { id, layer: marker, type: 'marker', attributes: attrs, style: {} };
      this._registerShape(shape);
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

        const point = pm.querySelector('Point coordinates');
        if (point) {
          const [lon, lat] = point.textContent.trim().split(',').map(Number);
          if (!isNaN(lat) && !isNaN(lon)) {
            const marker = L.marker([lat, lon]);
            const id = this.nextId++;
            const shape = {
              id, layer: marker, type: 'marker',
              attributes: { name: name || `Placemark ${id}`, description: desc },
              style: {}
            };
            this._registerShape(shape);
          }
          return;
        }

        const lineCoords = pm.querySelector('LineString coordinates');
        if (lineCoords) {
          const latlngs = this._parseKMLCoords(lineCoords.textContent);
          if (latlngs.length > 0) {
            const polyline = L.polyline(latlngs, { color: '#4dabf7', weight: 2 });
            const id = this.nextId++;
            const shape = {
              id, layer: polyline, type: 'polyline',
              attributes: { name: name || `Line ${id}`, description: desc },
              style: { color: '#4dabf7', weight: 2 }
            };
            this._registerShape(shape);
          }
          return;
        }

        const polyCoords = pm.querySelector('Polygon outerBoundaryIs LinearRing coordinates');
        if (polyCoords) {
          const latlngs = this._parseKMLCoords(polyCoords.textContent);
          if (latlngs.length > 0) {
            const polygon = L.polygon(latlngs, { color: '#4dabf7', weight: 2, fillOpacity: 0.2 });
            const id = this.nextId++;
            const shape = {
              id, layer: polygon, type: 'polygon',
              attributes: { name: name || `Polygon ${id}`, description: desc },
              style: { color: '#4dabf7', weight: 2, fillOpacity: 0.2 }
            };
            this._registerShape(shape);
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
    const escapeXml = (str) =>
      String(str ?? '')
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;');

    let kml = '<?xml version="1.0" encoding="UTF-8"?>\n';
    kml += '<kml xmlns="http://www.opengis.net/kml/2.2">\n<Document>\n';
    kml += '<name>jsMars Shapes</name>\n';

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
    if (layer?.options) {
      return {
        color: layer.options.color,
        weight: layer.options.weight,
        opacity: layer.options.opacity,
        fillColor: layer.options.fillColor,
        fillOpacity: layer.options.fillOpacity,
        dashArray: layer.options.dashArray || null,
        pattern: layer.options.pattern || ''
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
