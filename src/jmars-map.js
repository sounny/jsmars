/**
 * @module jmars-map
 * @description Core map controller for jsMars.
 *
 * Manages the Leaflet map instance, layer lifecycle (add/remove/reorder),
 * multi-body switching with per-body state preservation, and WMS layer
 * discovery. Coordinates with jmarsState for reactive layer management.
 *
 * @requires leaflet (global L)
 * @requires jmars-config
 * @requires jmars-wms
 * @requires jmars-state
 * @requires constants
 */
import { JMARS_CONFIG } from './jmars-config.js';
import { JMARSWMS } from './jmars-wms.js';
import { layers as initialLayers, createLeafletLayer } from './layers/index.js';
import { JMARSVectors } from './jmars-vectors.js';
import { jmarsState } from './jmars-state.js';
import { EVENTS } from './constants.js';
import { URLStateEngine } from './util/URLStateEngine.js';
import { normalizeBodyKey, switchActiveBody } from './util/body.js';

/**
 * @class JMARSMap
 * @description Main map class managing Leaflet, layers, and multi-body switching.
 */
export class JMARSMap {
  /**
   * @param {string} elementId - DOM element ID for the map container.
   */
  constructor(elementId) {
    this.elementId = elementId;
    /** @type {L.Map|null} The Leaflet map instance. */
    this.map = null;
    /** @type {Object<string, L.Layer>} Currently active Leaflet layer instances, keyed by layer ID. */
    this.activeLayers = {};
    /** @type {Array<Object>} All available layer configs for the current body. */
    this.availableLayers = [...initialLayers];
    /** @type {HTMLElement|null} Loading spinner element. */
    this.loadingIndicator = document.getElementById('loading-indicator');
    /** @type {JMARSVectors|null} Vector drawing manager. */
    this.vectors = null;
    /** @type {Object<string, Object>} Saved per-body view states (center, zoom, activeLayers). */
    this.bodyStates = {};
    /** @type {string} Current active body key (lowercase). */
    this.currentBody = JMARS_CONFIG.body.toLowerCase();
    /** @type {Object<string, L.Layer>} Custom layer instances (e.g., GeoTIFF uploads). */
    this.customLayerInstances = {};
    /** @type {AbortController|null} Abort controller for in-flight WMS discovery. */
    this._discoveryAbort = null;
    /** @type {number|null} Debounced view sync timer. */
    this._viewSyncTimer = null;

    if (window.L) {
      // Initialize Leaflet map with Plate Carree projection (standard for planetary WMS)
      this.map = L.map(this.elementId, {
        center: [JMARS_CONFIG.initialView.lat, JMARS_CONFIG.initialView.lng],
        zoom: JMARS_CONFIG.initialView.zoom,
        crs: L.CRS.EPSG4326,
        attributionControl: true,
        zoomControl: false
      });

      // Move zoom control to top-right (less intrusive)
      L.control.zoom({ position: 'topright' }).addTo(this.map);

      // Initialize vector drawing support
      const vectorGroup = new L.FeatureGroup();
      this.vectors = new JMARSVectors(this.map, vectorGroup);
      this.vectors.init();

      // Track tile loading state for the loading indicator
      this.map.on('loading', () => this.setLoading(true));
      this.map.on('load', () => this.setLoading(false));
    } else {
      console.error('Leaflet (L) is not defined. Make sure to load it in index.html');
    }
  }

  /**
   * Initialize the map: switch to the default body, discover WMS layers,
   * restore deep-link URL state if present, and listen for body-change events.
   */
  init() {
    if (!this.map) return;

    // Check for Deep-Link URL State
    if (typeof window !== 'undefined' && window.location) {
      const urlState = URLStateEngine.parseURLToState(window.location.search || window.location.hash);
      if (urlState.hasState) {
        const targetBody = normalizeBodyKey(urlState.body || this.currentBody || 'mars');
        this.currentBody = targetBody;

        const bodyCfg = JMARS_CONFIG.bodies[targetBody] || JMARS_CONFIG.bodies.mars;
        const z = urlState.zoom !== null ? urlState.zoom : (bodyCfg?.zoom || JMARS_CONFIG.initialView.zoom);
        const lat = urlState.lat !== null ? urlState.lat : (bodyCfg?.center ? bodyCfg.center[0] : 0);
        const lon = urlState.lon !== null ? urlState.lon : (bodyCfg?.center ? bodyCfg.center[1] : 0);

        this.bodyStates[targetBody] = {
          center: [lat, lon],
          zoom: z,
          activeLayers: (urlState.activeLayers && urlState.activeLayers.length > 0) ? urlState.activeLayers : null
        };

        if (urlState.colorStretch) {
          document.dispatchEvent(new CustomEvent('jmars:color-stretch-changed', { detail: urlState.colorStretch }));
        }
      }
    }

    switchActiveBody(this, this.currentBody, { emitEvent: false, force: true });
    this.syncViewState({ updateUrl: false });
    this.addControls();
    this._initialized = true;

    const scheduleViewSync = () => this.scheduleViewStateSync();
    this.map.on('moveend', scheduleViewSync);
    this.map.on('zoomend', scheduleViewSync);

    // Auto-sync browser URL whenever layers are added, removed, reordered, or opacity changes
    document.addEventListener(EVENTS.LAYERS_CHANGED, () => {
      scheduleViewSync();
    });

    // Auto-sync color stretch adjustments
    document.addEventListener('jmars:color-stretch-changed', () => {
      scheduleViewSync();
    });

    // Listen for copy share link requests
    document.addEventListener('jmars:copy-share-link', async () => {
      await this.copyShareLink();
    });
  }

  /**
   * Copy the full deep-link shareable URL to clipboard.
   * @returns {Promise<string>}
   */
  async copyShareLink() {
    const view = this.getViewState();
    const state = {
      body: this.currentBody,
      lat: view.lat,
      lon: view.lng,
      zoom: view.zoom,
      activeLayers: jmarsState.get('activeLayers')
    };
    return await URLStateEngine.copyShareLink(state);
  }

  /**
   * Return the live Leaflet view as a serializable state object.
   * @returns {{lat:number, lng:number, zoom:number}|null}
   */
  getViewState() {
    if (!this.map) return null;
    const center = this.map.getCenter();
    return {
      lat: center.lat,
      lng: center.lng,
      zoom: this.map.getZoom()
    };
  }

  /**
   * Synchronize the live map view into app state and optionally the browser URL.
   * This is the single source-of-truth path for view -> state synchronization.
   * @param {{updateUrl?: boolean}} [options]
   * @returns {{lat:number, lng:number, zoom:number}|null}
   */
  syncViewState(options = {}) {
    const updateUrl = options.updateUrl !== false;
    const view = this.getViewState();
    if (!view) return null;

    jmarsState.set('view', view);

    if (updateUrl) {
      URLStateEngine.syncToBrowserURL({
        body: this.currentBody,
        lat: view.lat,
        lon: view.lng,
        zoom: view.zoom,
        activeLayers: jmarsState.get('activeLayers')
      });
    }

    return view;
  }

  /**
   * Debounce syncViewState so URL/state updates follow settled map interactions.
   * @param {{delay?: number, updateUrl?: boolean}} [options]
   */
  scheduleViewStateSync(options = {}) {
    const delay = typeof options.delay === 'number' ? options.delay : 400;
    clearTimeout(this._viewSyncTimer);
    this._viewSyncTimer = window.setTimeout(() => {
      this.syncViewState({ updateUrl: options.updateUrl !== false });
    }, delay);
  }

  /**
   * Apply a serialized view to the live map and immediately resync state.
   * @param {{lat:number, lng:number, zoom:number}} view
   * @param {{updateUrl?: boolean}} [options]
   * @returns {{lat:number, lng:number, zoom:number}|null}
   */
  applyViewState(view, options = {}) {
    if (!this.map || !view) return null;
    const lat = Number(view.lat);
    const lng = Number(view.lng);
    const zoom = Number(view.zoom);
    if (![lat, lng, zoom].every(Number.isFinite)) return null;

    this.map.setView([lat, lng], zoom);
    return this.syncViewState(options);
  }

  /**
   * Switch the map to a different planetary body.
   * Saves current body state, clears layers, restores (or defaults) the
   * new body's state, and triggers WMS discovery for the new body.
   * @param {string} bodyKey - Lowercase body key (e.g., 'mars', 'moon', 'earth').
   */
  switchBody(bodyKey) {
    const key = normalizeBodyKey(bodyKey);
    const bodyConfig = JMARS_CONFIG.bodies[key];
    if (!bodyConfig) return;

    console.log(`Switching to body: ${bodyConfig.name}`);

    // 1. Only save previous body state if actively switching from a different loaded body
    if (this._initialized && this.currentBody && this.currentBody !== key) {
      this.bodyStates[this.currentBody] = {
        center: this.map.getCenter(),
        zoom: this.map.getZoom(),
        activeLayers: jmarsState.get('activeLayers').map(l => ({ ...l }))
      };
    }

    // 2. Update context
    this.currentBody = key;

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

    // 3. Clear current map layers (Object.keys creates a snapshot, safe during deletion)
    Object.keys(this.activeLayers).forEach(id => this.removeLayer(id));
    this.activeLayers = {};

    // 4. Announce available layers so LayerManager can rebuild its list
    const event = new CustomEvent(EVENTS.LAYERS_UPDATED, { detail: this.availableLayers });
    document.dispatchEvent(event);

    // 5. Restore saved state or set defaults
    const savedState = this.bodyStates[key];
    let newActiveLayers = [];

    if (savedState) {
      this.map.setView(savedState.center, savedState.zoom);
      if (savedState.activeLayers && savedState.activeLayers.length > 0) {
        newActiveLayers = savedState.activeLayers;
      }
    }
    if (newActiveLayers.length === 0) {
      this.map.setView(savedState?.center || bodyConfig.center, savedState?.zoom || bodyConfig.zoom);
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

    // Prune stale layers that no longer exist in this body's available set
    if (newActiveLayers.length > 0) {
      newActiveLayers = newActiveLayers
        .map(l => this.availableLayers.find(al => al.id === l.id) ? l : null)
        .filter(Boolean);
    }

    // Ensure at least one visible layer
    if (newActiveLayers.length === 0 && this.availableLayers.length > 0) {
      newActiveLayers = [{
        id: this.availableLayers[0].id,
        opacity: 1,
        visible: true
      }];
    }

    // 6. Update state (triggers LayerManager to sync the map)
    console.debug('SwitchBody: Setting active layers to:', newActiveLayers);
    jmarsState.setActiveLayers(newActiveLayers);

    // 7. Discover WMS layers for the new body
    this.discoverLayers();
  }

  /**
   * Show or hide the loading indicator.
   * @param {boolean} isLoading - Whether loading is in progress.
   */
  setLoading(isLoading) {
    if (this.loadingIndicator) {
      if (isLoading) this.loadingIndicator.classList.add('visible');
      else this.loadingIndicator.classList.remove('visible');
    }
  }

  /**
   * Fetch WMS capabilities for the current body and append discovered
   * layers to the available layers list.
   *
   * Uses an AbortController guard to prevent race conditions when
   * switching bodies rapidly (old discovery results won't contaminate
   * a new body's layer list).
   */
  async discoverLayers() {
    const wmsKey = `${this.currentBody}_wms`;
    const wmsUrl = JMARS_CONFIG.services[wmsKey];
    if (!wmsUrl) {
      console.debug(`No WMS endpoint configured for body: ${this.currentBody}`);
      return;
    }

    // Cancel any in-flight discovery to prevent race conditions
    if (this._discoveryAbort) {
      this._discoveryAbort.abort();
    }
    this._discoveryAbort = new AbortController();
    const currentBody = this.currentBody; // Capture for closure comparison

    this.setLoading(true);
    console.debug(`Fetching capabilities from ${wmsUrl} for ${this.currentBody}...`);

    try {
      const wmsLayers = await JMARSWMS.fetchCapabilities(wmsUrl);

      // Guard: if body changed while we were fetching, discard results
      if (this.currentBody !== currentBody) {
        console.debug('Discovery results discarded (body changed during fetch).');
        return;
      }

      console.debug(`Discovered ${wmsLayers.length} layers for ${this.currentBody}.`);

      wmsLayers.forEach(l => {
        // Skip duplicates (hardcoded layers may already exist)
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

      // Notify LayerManager of updated available layers
      const event = new CustomEvent(EVENTS.LAYERS_UPDATED, { detail: this.availableLayers });
      document.dispatchEvent(event);
    } catch (e) {
      if (e.name === 'AbortError') return; // Expected on body switch
      console.error('Error discovering layers:', e);
    } finally {
      this.setLoading(false);
    }
  }

  /**
   * Add a layer to the Leaflet map by its config ID.
   * Checks custom layer instances first (for GeoTIFF uploads), then
   * falls back to creating a standard WMS/XYZ tile layer.
   * @param {string} layerId - The layer config ID.
   */
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

    // Check for custom layer instances (e.g., uploaded GeoTIFFs) first
    let leafletLayer;
    if (this.customLayerInstances[layerId]) {
      leafletLayer = this.customLayerInstances[layerId];
    } else {
      leafletLayer = createLeafletLayer(layerConfig);
    }

    if (leafletLayer) {
      leafletLayer.addTo(this.map);
      this.activeLayers[layerId] = leafletLayer;
      console.log(`%c[JSMARS:Map] %cAdded layer to map: %c${layerId}`, 'color: #10b981; font-weight: bold;', 'color: #f8fafc;', 'color: #38bdf8; font-weight: 600;');
    } else {
      console.error('Failed to create leaflet layer for:', layerId);
    }
  }

  /**
   * Remove a layer from the Leaflet map.
   * @param {string} layerId - The layer config ID to remove.
   */
  removeLayer(layerId) {
    if (this.activeLayers[layerId]) {
      this.map.removeLayer(this.activeLayers[layerId]);
      delete this.activeLayers[layerId];
      console.log(`%c[JSMARS:Map] %cRemoved layer from map: %c${layerId}`, 'color: #ef4444; font-weight: bold;', 'color: #f8fafc;', 'color: #f87171; font-weight: 600;');
    }
  }

  /**
   * Set the opacity of an active layer.
   * @param {string} layerId - The layer ID.
   * @param {number} opacity - Opacity value (0 to 1).
   */
  setLayerOpacity(layerId, opacity) {
    const layer = this.activeLayers[layerId];
    if (layer && typeof layer.setOpacity === 'function') {
      layer.setOpacity(opacity);
      console.log(`%c[JSMARS:Map] %cSet opacity of "${layerId}" to ${(opacity * 100).toFixed(0)}%`, 'color: #ec4899; font-weight: bold;', 'color: #f8fafc;');
    }
  }

  /**
   * Reorder active layers by z-index.
   * activeLayers state array is ordered [Bottom, ..., Top].
   * @param {string[]} layerIds - Layer IDs ordered from bottom to top.
   */
  updateLayerOrder(layerIds) {
    const total = layerIds.length;
    layerIds.forEach((id, index) => {
      const layer = this.activeLayers[id];
      if (layer && typeof layer.setZIndex === 'function') {
        // Higher index in array = higher zIndex (rendered on top)
        layer.setZIndex(index + 1);
      } else if (layer && typeof layer.bringToFront === 'function') {
        if (index === total - 1) layer.bringToFront();
        else if (index === 0 && typeof layer.bringToBack === 'function') layer.bringToBack();
      }
    });
  }

  /**
   * Placeholder for adding map-specific Leaflet controls.
   * Controls are currently handled by external UI modules.
   */
  addControls() {
    // Controls are now handled by external UI components (StatusBar, etc.)
  }
}
