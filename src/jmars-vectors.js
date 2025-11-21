export class JMARSVectors {
  constructor(map, featureGroup) {
    this.map = map;
    this.featureGroup = featureGroup;
    this.drawControl = null;
  }

  init() {
    if (!L.Control.Draw) {
      console.error('Leaflet.Draw not found. Make sure to load it in index.html');
      return;
    }

    // Initialize the FeatureGroup to store editable layers
    this.map.addLayer(this.featureGroup);

    // Initialize the draw control and pass it the FeatureGroup of editable layers
    this.drawControl = new L.Control.Draw({
      position: 'topright',
      draw: {
        polyline: true,
        polygon: true,
        circle: false, // Circles are less useful for planetary mapping (projection issues)
        rectangle: true,
        marker: true,
        circlemarker: false
      },
      edit: {
        featureGroup: this.featureGroup
      }
    });

    this.map.addControl(this.drawControl);

    // Handle created items
    this.map.on(L.Draw.Event.CREATED, (e) => {
      const type = e.layerType;
      const layer = e.layer;

      if (type === 'marker') {
        layer.bindPopup('A popup!');
      }

      this.featureGroup.addLayer(layer);
      console.log('Created new vector shape:', type);
    });
  }
}
