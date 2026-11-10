/**
 * @module jmars-vectors
 * @description Vector drawing and editing support for jsMars.
 *
 * Wraps Leaflet.Draw to provide draw controls (polyline, polygon, circle,
 * rectangle, marker) and integrates the StyleEditor for right-click styling.
 * Coordinates cleanly with ShapeLayer and exclusive draw tools (Measure,
 * Sampling, Linear Profile) so temporary tool geometries are never hijacked.
 *
 * @requires leaflet (global L)
 * @requires leaflet.draw (L.Control.Draw)
 * @requires StyleEditor
 */
import { StyleEditor } from './features/shapes/StyleEditor.js';

/**
 * @class JMARSVectors
 * @description Manages vector layer drawing, editing, and styling.
 */
export class JMARSVectors {
  /**
   * @param {L.Map} map - The Leaflet map instance.
   * @param {L.FeatureGroup} featureGroup - Feature group to hold editable vector layers.
   */
  constructor(map, featureGroup) {
    /** @type {L.Map} */
    this.map = map;
    /** @type {L.FeatureGroup} */
    this.featureGroup = featureGroup;
    /** @type {L.Control.Draw|null} */
    this.drawControl = null;
    /** @type {StyleEditor|null} */
    this.styleEditor = null;
    /** @type {boolean} True when ShapeLayer manages shape creation on this featureGroup */
    this.delegatedToShapeLayer = false;
    /** @type {import('./features/shapes/ShapeLayer.js').ShapeLayer|null} */
    this.shapeLayer = null;
  }

  /**
   * Delegate vector lifecycle management to ShapeLayer so the Leaflet.Draw toolbar,
   * Shapes attribute table, right-click StyleEditor, and SessionManager share one source of truth.
   * @param {import('./features/shapes/ShapeLayer.js').ShapeLayer} shapeLayer
   */
  delegateToShapeLayer(shapeLayer) {
    this.delegatedToShapeLayer = true;
    this.shapeLayer = shapeLayer;
    if (this.styleEditor) {
      this.styleEditor.onStyleChange = (layer, styles) => {
        if (layer && layer._shapeId != null && this.shapeLayer) {
          this.shapeLayer.syncStyleFromLayer(layer._shapeId, styles);
        }
      };
    }
  }

  /**
   * Initialize the vector drawing system.
   * Sets up the Leaflet.Draw control, wires up draw:created events,
   * and initializes the right-click style editor.
   */
  init() {
    if (!L.Control.Draw) {
      console.error('Leaflet.Draw not found. Make sure to load it in index.html');
      return;
    }

    // Add the feature group to the map so drawn shapes are visible
    this.map.addLayer(this.featureGroup);

    // Initialize the style editor for right-click shape styling
    this.styleEditor = new StyleEditor(this.map);

    // Configure the draw control toolbar
    this.drawControl = new L.Control.Draw({
      position: 'topright',
      draw: {
        polyline: true,
        polygon: true,
        circle: true,
        rectangle: true,
        marker: true,
        circlemarker: true
      },
      edit: {
        featureGroup: this.featureGroup
      }
    });

    this.map.addControl(this.drawControl);

    // Handle newly created shapes ONLY when not delegated to ShapeLayer
    // and not owned by an exclusive measurement/sampling/profile tool.
    this.map.on('draw:created', (e) => {
      if (this.delegatedToShapeLayer || this.map._exclusiveDrawOwner || e._handledByTool) {
        return;
      }
      const type = e.layerType;
      const layer = e.layer;

      if (type === 'marker') {
        const latlng = layer.getLatLng();
        layer.bindPopup(`Marker at ${latlng.lat.toFixed(4)}, ${latlng.lng.toFixed(4)}`);
      }

      this.featureGroup.addLayer(layer);
    });

    // Open style editor on right-click (context menu) of vector shapes
    this.featureGroup.on('contextmenu', (e) => {
      L.DomEvent.stopPropagation(e);
      if (e.originalEvent) e.originalEvent.preventDefault();

      if (e.layer && e.layer._shapeId != null && this.shapeLayer) {
        this.shapeLayer.selectShape(e.layer._shapeId);
      }

      if (e.layer && typeof e.layer.setStyle === 'function') {
        this.styleEditor.open(e.layer, e.containerPoint);
      }
    });

    // Close style editor when clicking elsewhere on the map
    this.map.on('click', () => {
      this.styleEditor?.close();
    });
  }
}
