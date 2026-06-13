/**
 * @module jmars-vectors
 * @description Vector drawing and editing support for jsMars.
 *
 * Wraps Leaflet.Draw to provide draw controls (polyline, polygon, circle,
 * rectangle, marker) and integrates the StyleEditor for right-click styling.
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
  }

  /**
   * Initialize the vector drawing system.
   * Sets up the Leaflet.Draw control, wires up draw:created events,
   * and initializes the right-click style editor.
   */
  init() {
    console.debug('JMARSVectors initializing...');
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

    // Handle newly created shapes
    this.map.on('draw:created', (e) => {
      console.debug('draw:created event fired');
      const type = e.layerType;
      const layer = e.layer;

      // Bind a default popup showing coordinates for markers
      if (type === 'marker') {
        const latlng = layer.getLatLng();
        layer.bindPopup(`Marker at ${latlng.lat.toFixed(4)}, ${latlng.lng.toFixed(4)}`);
      }

      this.featureGroup.addLayer(layer);
      console.debug('Created new vector shape:', type);
    });

    // Open style editor on right-click (context menu) of vector shapes
    this.featureGroup.on('contextmenu', (e) => {
      console.debug('Context menu on shape');
      L.DomEvent.stopPropagation(e); // Prevent map context menu
      // Also prevent the browser's native context menu
      if (e.originalEvent) e.originalEvent.preventDefault();

      // Only open for layers with setStyle (not markers)
      if (typeof e.layer.setStyle === 'function') {
        this.styleEditor.open(e.layer, e.containerPoint);
      } else {
        console.warn('Layer does not support styling');
      }
    });

    // Close style editor when clicking elsewhere on the map
    this.map.on('click', () => {
      this.styleEditor.close();
    });
  }
}
