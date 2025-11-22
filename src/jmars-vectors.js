import { StyleEditor } from './features/shapes/StyleEditor.js';

export class JMARSVectors {
  constructor(map, featureGroup) {
    this.map = map;
    this.featureGroup = featureGroup;
    this.drawControl = null;
    this.styleEditor = null;
  }

  init() {
    console.log('JMARSVectors initializing...');
    if (!L.Control.Draw) {
      console.error('Leaflet.Draw not found. Make sure to load it in index.html');
      return;
    }

    // Initialize the FeatureGroup to store editable layers
    this.map.addLayer(this.featureGroup);

    // Initialize StyleEditor
    this.styleEditor = new StyleEditor(this.map);

    // Initialize the draw control
    this.drawControl = new L.Control.Draw({
      position: 'topright',
      draw: {
        polyline: true,
        polygon: true,
        circle: true,
        rectangle: true,
        marker: true,
        circlemarker: true // Enabled for styling tests
      },
      edit: {
        featureGroup: this.featureGroup
      }
    });

    this.map.addControl(this.drawControl);

    // Handle created items
    this.map.on('draw:created', (e) => {
      console.log('draw:created event fired');
      const type = e.layerType;
      const layer = e.layer;

      if (type === 'marker') {
        layer.bindPopup('A popup!');
      }

      this.featureGroup.addLayer(layer);
      console.log('Created new vector shape:', type);
    });

    // Handle interactions
    this.featureGroup.on('contextmenu', (e) => {
        console.log('Context menu on shape');
        L.DomEvent.stopPropagation(e); // Prevent map context menu

        // Only open for layers with setStyle (vectors)
        if (typeof e.layer.setStyle === 'function') {
            this.styleEditor.open(e.layer, e.containerPoint);
        } else {
            console.log('Layer does not support styling');
        }
    });

    // Close editor on map click
    this.map.on('click', () => {
        this.styleEditor.close();
    });
  }
}
