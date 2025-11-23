import { JMARS_CONFIG } from './jmars-config.js';
import { JMARSWMS } from './jmars-wms.js';
import { layers as initialLayers, createLeafletLayer } from './layers/index.js';
import { JMARSVectors } from './jmars-vectors.js';

export class JMARSMap {
  constructor(elementId) {
    this.elementId = elementId;
    this.map = null;
    this.activeLayers = {};
    this.activeLayers = {};
    this.availableLayers = [...initialLayers]; // Start with hardcoded, append WMS later
    this.loadingIndicator = document.getElementById('loading-indicator');
    this.vectors = null;
  }

  init() {
    if (!window.L) {
      console.error('Leaflet (L) is not defined. Make sure to load it in index.html');
      return;
    }

    // Initialize Leaflet map
    // We use EPSG:4326 (Plate Carree) as it's standard for planetary WMS
    this.map = L.map(this.elementId, {
      center: [JMARS_CONFIG.initialView.lat, JMARS_CONFIG.initialView.lng],
      zoom: JMARS_CONFIG.initialView.zoom,
      crs: L.CRS.EPSG4326,
      attributionControl: true,
      zoomControl: false // Disable default zoom control to move it
    });

    // Add Zoom control to top-right
    L.control.zoom({ position: 'topright' }).addTo(this.map);

    // Add default layer
    this.addLayer('mars_viking');

    // Initialize Vectors
    const vectorGroup = new L.FeatureGroup();
    this.vectors = new JMARSVectors(this.map, vectorGroup);
    this.vectors.init();

    // Add vector layer to active layers so it shows up in manager (optional, but good for visibility)
    // For now, we just keep it on the map.

    // Loading events
    this.map.on('loading', () => this.setLoading(true));
    this.map.on('load', () => this.setLoading(false));
    // Also catch individual tile errors to stop loading spinner if everything else fails
    this.map.on('tileerror', () => {
      // Optional: maybe don't hide immediately, but good to know.
    });

    // Discover WMS layers
    this.discoverLayers();

    // Add controls
    this.addControls();
  }

  setLoading(isLoading) {
    if (this.loadingIndicator) {
      if (isLoading) this.loadingIndicator.classList.add('visible');
      else this.loadingIndicator.classList.remove('visible');
    }
  }

  async discoverLayers() {
    this.setLoading(true);
    const wmsUrl = JMARS_CONFIG.services.mars_wms;
    console.log(`Fetching capabilities from ${wmsUrl}...`);

    const wmsLayers = await JMARSWMS.fetchCapabilities(wmsUrl);
    console.log(`Discovered ${wmsLayers.length} layers.`);

    wmsLayers.forEach(l => {
      // Avoid duplicates if hardcoded layers exist
      if (this.availableLayers.find(existing => existing.id === l.name)) return;

      this.availableLayers.push({
        id: l.name,
        name: l.title,
        type: 'wms',
        url: wmsUrl,
        options: {
          layers: l.name,
          format: 'image/png',
          transparent: true,
          attribution: 'USGS Astrogeology'
        }
      });
    });

    // Trigger UI update
    const event = new CustomEvent('jmars-layers-updated', { detail: this.availableLayers });
    document.dispatchEvent(event);

    this.setLoading(false);
  }

  addLayer(layerId) {
    const layerConfig = this.availableLayers.find(l => l.id === layerId);
    if (!layerConfig) {
      console.warn(`Layer not found: ${layerId}`);
      return;
    }

    if (this.activeLayers[layerId]) return;

    const leafletLayer = createLeafletLayer(layerConfig);
    if (leafletLayer) {
      leafletLayer.addTo(this.map);
      this.activeLayers[layerId] = leafletLayer;
      console.log(`Added layer: ${layerId}`);
    }
  }

  removeLayer(layerId) {
    if (this.activeLayers[layerId]) {
      this.map.removeLayer(this.activeLayers[layerId]);
      delete this.activeLayers[layerId];
      console.log(`Removed layer: ${layerId}`);
    }
  }

  setLayerOpacity(layerId, opacity) {
    const layer = this.activeLayers[layerId];
    if (layer && typeof layer.setOpacity === 'function') {
      layer.setOpacity(opacity);
    }
  }

  updateLayerOrder(layerIds) {
    // layerIds is expected to be from Top (highest z-index) to Bottom (lowest z-index)
    const total = layerIds.length;
    layerIds.forEach((id, index) => {
      const layer = this.activeLayers[id];
      if (layer && typeof layer.setZIndex === 'function') {
        // Leaflet TileLayers support setZIndex.
        // Higher zIndex is on top.
        layer.setZIndex(total - index);
      } else if (layer && layer.setStyle) {
        // Vectors don't usually have setZIndex in the same way, but we can try bringToFront
        if (index === 0) layer.bringToFront();
        else if (index === total - 1) layer.bringToBack();
      }
    });
  }

  addControls() {
    // Controls are now handled by external UI components (StatusBar, etc.)
    // We can keep this method for future map-specific controls if needed.
  }
}
