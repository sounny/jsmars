import { JMARS_CONFIG } from './jmars-config.js';
import { JMARSWMS } from './jmars-wms.js';
import { layers as initialLayers, createLeafletLayer } from './layers/index.js';
import { JMARSVectors } from './jmars-vectors.js';
import { jmarsState } from './jmars-state.js';
import { EVENTS } from './constants.js';

export class JMARSMap {
  constructor(elementId) {
    this.elementId = elementId;
    this.map = null;
    this.activeLayers = {};
    this.availableLayers = [...initialLayers]; // Start with hardcoded, append WMS later
    this.loadingIndicator = document.getElementById('loading-indicator');
    this.vectors = null;
    this.bodyStates = {}; // Store state for each body
    this.currentBody = JMARS_CONFIG.body.toLowerCase();

    if (window.L) {
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
    
        // Initialize Vectors
        const vectorGroup = new L.FeatureGroup();
        this.vectors = new JMARSVectors(this.map, vectorGroup);
        this.vectors.init();

        // Loading events
        this.map.on('loading', () => this.setLoading(true));
        this.map.on('load', () => this.setLoading(false));
        this.map.on('tileerror', () => { /* Optional */ });
    } else {
        console.error('Leaflet (L) is not defined. Make sure to load it in index.html');
    }
  }

  init() {
    if (!this.map) return;

    // Initialize Body Context
    this.switchBody(this.currentBody);

    // Discover WMS layers
    this.discoverLayers();

    // Add controls
    this.addControls();

    // Listen for body changes
    document.addEventListener(EVENTS.BODY_CHANGED, (e) => {
      const body = e?.detail?.body;
      if (!body) return;
      this.switchBody(body);
    });
  }

  switchBody(bodyKey) {
    const bodyConfig = JMARS_CONFIG.bodies[bodyKey];
    if (!bodyConfig) return;

    console.log(`Switching to body: ${bodyConfig.name}`);

    // 1. Save current state (from jmarsState, not map internals)
    if (this.currentBody) {
      this.bodyStates[this.currentBody] = {
        center: this.map.getCenter(),
        zoom: this.map.getZoom(),
        activeLayers: [...jmarsState.get('activeLayers')]
      };
    }

    // 2. Update Context
    this.currentBody = bodyKey;
    // Normalize layer configs so Leaflet always has options objects
    this.availableLayers = (bodyConfig.layers || []).map(l => {
      if (l.type === 'wms' && !l.options) {
        return {
          ...l,
          options: {
            layers: l.layers,
            format: l.format || 'image/png',
            transparent: l.transparent !== false,
            attribution: l.attribution
          }
        };
      }
      if (l.type === 'xyz' && !l.options) {
        return {
          ...l,
          options: {
            attribution: l.attribution,
            maxZoom: l.maxZoom || 10
          }
        };
      }
      return { ...l };
    });

    // Clear current map layers immediately to avoid cross-body leftovers
    Object.keys(this.activeLayers).forEach(id => this.removeLayer(id));
    this.activeLayers = {};

    // 3. Announce new layers (So LayerManager knows about them before we try to activate)
    const event = new CustomEvent(EVENTS.LAYERS_UPDATED, { detail: this.availableLayers });
    document.dispatchEvent(event);

    // 4. Restore or Default State
    const savedState = this.bodyStates[bodyKey];
    let newActiveLayers = [];

    if (savedState) {
      // Restore saved view
      this.map.setView(savedState.center, savedState.zoom);
      newActiveLayers = savedState.activeLayers;
    } else {
      // Default view
      this.map.setView(bodyConfig.center, bodyConfig.zoom);
      // Default layers
      const defaultId = bodyConfig.defaultLayer;
      const defaultLayer = defaultId 
          ? this.availableLayers.find(l => l.id === defaultId) 
          : this.availableLayers[0];

      if (defaultLayer) {
          newActiveLayers = [{
            id: defaultLayer.id,
            opacity: 1,
            visible: true
          }];
      }
    }

    // If saved state had stale layers, prune to current body's availability
    if (newActiveLayers.length > 0) {
      newActiveLayers = newActiveLayers
        .map(l => this.availableLayers.find(al => al.id === l.id) ? l : null)
        .filter(Boolean);
    }

    // Ensure we always have at least one visible layer
    if (newActiveLayers.length === 0 && this.availableLayers.length > 0) {
      newActiveLayers = [{
        id: this.availableLayers[0].id,
        opacity: 1,
        visible: true
      }];
    }

    // 5. Update State (This triggers LayerManager to update the map)
    console.debug('SwitchBody: Setting active layers to:', newActiveLayers);
    jmarsState.setActiveLayers(newActiveLayers);
  }

  setLoading(isLoading) {
    if (this.loadingIndicator) {
      if (isLoading) this.loadingIndicator.classList.add('visible');
      else this.loadingIndicator.classList.remove('visible');
    }
  }

  async discoverLayers() {
    if (this.currentBody !== 'mars') return;

    this.setLoading(true);
    const wmsUrl = JMARS_CONFIG.services.mars_wms;
    console.debug(`Fetching capabilities from ${wmsUrl}...`);

    try {
        const wmsLayers = await JMARSWMS.fetchCapabilities(wmsUrl);
        console.debug(`Discovered ${wmsLayers.length} layers.`);

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
        const event = new CustomEvent(EVENTS.LAYERS_UPDATED, { detail: this.availableLayers });
        document.dispatchEvent(event);
    } catch (e) {
        console.error('Error discovering layers:', e);
    } finally {
        this.setLoading(false);
    }
  }

  addLayer(layerId) {
    console.debug('JMARSMap.addLayer:', layerId);
    const layerConfig = this.availableLayers.find(l => l.id === layerId);
    if (!layerConfig) {
      console.warn(`Layer not found: ${layerId}`);
      return;
    }

    if (this.activeLayers[layerId]) {
        console.debug('Layer already active:', layerId);
        return;
    }

    const leafletLayer = createLeafletLayer(layerConfig);
    if (leafletLayer) {
      leafletLayer.addTo(this.map);
      this.activeLayers[layerId] = leafletLayer;
      console.debug(`Added layer to map: ${layerId}`);
    } else {
        console.error('Failed to create leaflet layer for:', layerId);
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
