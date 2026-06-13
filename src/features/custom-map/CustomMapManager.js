/**
 * @module CustomMapManager
 * @description Handles client-side loading of GeoTIFF files.
 *
 * Uses georaster and georaster-layer-for-leaflet (loaded from CDN
 * with pinned versions) to let users drop a local GeoTIFF onto
 * the map without any server-side processing.
 */

import { jmarsState } from '../../jmars-state.js';

export class CustomMapManager {
  /**
   * Create a CustomMapManager.
   * @param {L.Map} map - The Leaflet map instance.
   */
  constructor(map) {
    this.map = map;
    this.customLayers = {}; // ID -> L.GeoRasterLayer
    this.layerCounter = 1;
  }

  /**
   * Load a local File object (GeoTIFF) and add it to the map.
   * @param {File} file - A GeoTIFF file selected or dropped by the user.
   */
  async loadGeoTIFF(file) {
    try {
      // 1. Ensure scripts are loaded
      await this._ensurePlugins();

      // 2. Read file as ArrayBuffer
      const arrayBuffer = await file.arrayBuffer();

      // 3. Parse GeoRaster
      // georaster is expected to be global
      const georaster = await parseGeoraster(arrayBuffer);

      // 4. Create Leaflet Layer
      // GeoRasterLayer is expected to be global
      const layer = new GeoRasterLayer({
        georaster: georaster,
        opacity: 0.8,
        resolution: 256
      });

      // 5. Generate ID and add to map
      const id = `custom_geotiff_${this.layerCounter++}`;
      const name = file.name;

      this.customLayers[id] = layer;
      
      // Register in state as a generic tile layer so layer-manager picks it up.
      // We need to inject it into jmarsMap so it can be toggled.
      const jmarsMapObj = window.jmars; // Exposed in index.html
      if (jmarsMapObj) {
        // Mock a config so layer manager knows about it
        const config = {
          id: id,
          name: name,
          type: 'geotiff',
          url: 'local-file'
        };
        
        jmarsMapObj.availableLayers.unshift(config);
        
        // Add to Leaflet directly since it's a custom layer type
        jmarsMapObj.customLayerInstances = jmarsMapObj.customLayerInstances || {};
        jmarsMapObj.customLayerInstances[id] = layer;
        
        // Force layer manager update
        jmarsState.addLayer(id);
        
        // Zoom to extent
        this.map.fitBounds(layer.getBounds());
      }

    } catch (err) {
      console.error('Failed to load GeoTIFF:', err);
      alert('Failed to load GeoTIFF. Check console for details.');
    }
  }

  /**
   * Dynamically load required CDN scripts (pinned versions).
   * @returns {Promise<void>}
   */
  async _ensurePlugins() {
    /**
     * Load a single script from a URL, skipping if already present.
     * @param {string} src - Script URL.
     * @returns {Promise<void>}
     */
    const loadScript = (src) => {
      return new Promise((resolve, reject) => {
        if (document.querySelector(`script[src="${src}"]`)) {
          resolve();
          return;
        }
        const script = document.createElement('script');
        script.src = src;
        script.onload = resolve;
        script.onerror = reject;
        document.head.appendChild(script);
      });
    };

    // Pinned to specific versions for reproducibility
    if (typeof parseGeoraster === 'undefined') {
      await loadScript('https://unpkg.com/georaster@1.6.0/georaster.browser.bundle.min.js');
    }
    if (typeof GeoRasterLayer === 'undefined') {
      await loadScript('https://unpkg.com/georaster-layer-for-leaflet@3.10.0/dist/georaster-layer-for-leaflet.min.js');
    }
  }
}
