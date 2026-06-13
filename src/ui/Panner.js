import { JMARS_CONFIG } from '../jmars-config.js';
import { EVENTS } from '../constants.js';

/**
 * @module Panner
 * @description A small overview mini-map that shows the main map's
 * current viewport as a rectangle overlay. Clicking the panner
 * re-centers the main map at the clicked location.
 *
 * Listens for body changes to swap its base layer. Uses 'moveend'
 * instead of 'move' to avoid continuous redraws during panning.
 */
export class Panner {
  /**
   * Create a new Panner.
   * @param {object} jmarsMap - The main JmarsMap instance
   * @param {L.Map} jmarsMap.map - The Leaflet map
   * @param {string} [jmarsMap.currentBody] - Current body key
   */
  constructor(jmarsMap) {
    this.mainMap = jmarsMap.map;
    this.currentBody = jmarsMap.currentBody || (JMARS_CONFIG.body || 'mars').toLowerCase();
    /** @type {HTMLDivElement|null} */
    this.container = null;
    /** @type {L.Map|null} */
    this.miniMap = null;
    /** @type {L.Rectangle|null} */
    this.rect = null;
    /**
     * Whether the panner is visible. Starts true so the initial
     * update() call in init() actually renders the viewport rect.
     * @type {boolean}
     */
    this.isOpen = true;
    /** @type {L.TileLayer|null} */
    this.baseLayer = null;

    this.init();
  }

  /**
   * Build the panner DOM and mini-map, add event listeners.
   * @private
   */
  init() {
    // Create container
    this.container = document.createElement('div');
    this.container.id = 'jmars-panner';
    // Styles are now handled in style.css
    document.body.appendChild(this.container);

    // Initialize MiniMap
    this.miniMap = L.map(this.container, {
      attributionControl: false,
      zoomControl: false,
      crs: L.CRS.EPSG4326,
      center: [0, 0],
      zoom: 0,
      dragging: false,
      touchZoom: false,
      scrollWheelZoom: false,
      doubleClickZoom: false,
      boxZoom: false,
      keyboard: false
    });

    // Add Base Layer
    this.setBaseLayer(this.currentBody);

    // Add View Rect
    this.rect = L.rectangle(this.mainMap.getBounds(), { color: "#d6336c", weight: 1, fillOpacity: 0.2 }).addTo(this.miniMap);

    // Sync on moveend (not 'move') to avoid continuous redraws while dragging
    this.mainMap.on('moveend', () => this.update());
    this.mainMap.on('zoomend', () => this.update());

    // Clicking panner recenters main map at same zoom
    this.miniMap.on('click', (e) => {
      const targetZoom = this.mainMap.getZoom();
      this.mainMap.setView(e.latlng, targetZoom);
    });

    // Listen for body change to swap basemap
    document.addEventListener(EVENTS.BODY_CHANGED, (e) => {
      const body = e?.detail?.body;
      if (!body) return;
      this.currentBody = body.toLowerCase();
      this.setBaseLayer(this.currentBody);
      this.update(true);
    });

    // Initial update runs because isOpen defaults to true
    this.update();
  }

  /**
   * Sync the viewport rectangle and center the mini-map.
   * No-op if the panner is not visible.
   * @param {boolean} [_force] - Unused; kept for call-site compat
   */
  update(_force) {
    if (!this.isOpen) return;

    const bounds = this.mainMap.getBounds();
    this.rect.setBounds(bounds);

    // Center on current body defaults
    const bodyCfg = JMARS_CONFIG.bodies[this.currentBody] || JMARS_CONFIG.bodies.mars;
    const center = bodyCfg?.center || [0, 0];
    const zoom = 0; // show full extent
    this.miniMap.setView(center, zoom);
  }

  /**
   * Set the base tile layer for the mini-map based on the body key.
   * @param {string} bodyKey - Body identifier (e.g. 'mars', 'moon')
   */
  setBaseLayer(bodyKey) {
    const layerCfg = this.getDefaultLayerConfig(bodyKey);
    if (!layerCfg) return;

    // Remove previous
    if (this.baseLayer) {
      this.miniMap.removeLayer(this.baseLayer);
      this.baseLayer = null;
    }

    const layer = this.createLeafletLayer(layerCfg);
    if (layer) {
      layer.addTo(this.miniMap);
      this.baseLayer = layer;

      // Recenter to body default view for context
      const body = JMARS_CONFIG.bodies[bodyKey];
      if (body && body.center) {
        this.miniMap.setView(body.center, Math.max(0, (body.zoom || 2) - 1));
      }
    }
  }

  /**
   * Look up the default layer config for a given body.
   * @param {string} bodyKey - Body identifier
   * @returns {object|null} Layer config object, or null
   * @private
   */
  getDefaultLayerConfig(bodyKey) {
    const body = JMARS_CONFIG.bodies[bodyKey];
    if (!body || !Array.isArray(body.layers) || body.layers.length === 0) return null;
    const defaultId = body.defaultLayer;
    return body.layers.find(l => l.id === defaultId) || body.layers[0];
  }

  /**
   * Create a Leaflet tile layer from a config object.
   * @param {object} layerConfig - Layer configuration
   * @returns {L.TileLayer|null}
   * @private
   */
  createLeafletLayer(layerConfig) {
    if (!layerConfig) return null;
    if (layerConfig.type === 'wms') {
      return L.tileLayer.wms(layerConfig.url, layerConfig.options || {});
    }
    return L.tileLayer(layerConfig.url, layerConfig.options || {});
  }

  /**
   * Show or hide the panner.
   * @param {boolean} show - Whether to show the panner
   */
  toggle(show) {
    this.isOpen = show;
    if (show) {
      this.container.classList.add('visible');
      this.miniMap.invalidateSize();
      this.update();
    } else {
      this.container.classList.remove('visible');
    }
  }
}
