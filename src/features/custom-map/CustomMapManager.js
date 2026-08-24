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

  // --- GIS Tile Coordinates & WMS Helpers ---

  /**
   * Convert between TMS and Slippy XYZ tile Y coordinates.
   * @param {number} tileY - Tile Y coordinate
   * @param {number} zoom - Map zoom level
   * @returns {number} Inverted Y coordinate
   */
  static tmsToXyz(tileY, zoom) {
    const numTiles = Math.pow(2, zoom);
    return numTiles - 1 - tileY;
  }

  /**
   * Convert tile coordinate (x, y, zoom) to a Quadkey string.
   * @param {number} tileX
   * @param {number} tileY
   * @param {number} zoom
   * @returns {string} Quadkey string (e.g. "03201")
   */
  static tileToQuadkey(tileX, tileY, zoom) {
    let quadkey = '';
    for (let i = zoom; i > 0; i--) {
      let digit = 0;
      const mask = 1 << (i - 1);
      if ((tileX & mask) !== 0) digit += 1;
      if ((tileY & mask) !== 0) digit += 2;
      quadkey += digit.toString();
    }
    return quadkey;
  }

  /**
   * Convert Quadkey string to tile coordinates (x, y, zoom).
   * @param {string} quadkey
   * @returns {{tileX: number, tileY: number, zoom: number}}
   */
  static quadkeyToTile(quadkey) {
    let tileX = 0;
    let tileY = 0;
    const zoom = quadkey.length;

    for (let i = zoom; i > 0; i--) {
      const mask = 1 << (i - 1);
      const digit = parseInt(quadkey[zoom - i], 10);

      if (digit === 1) tileX |= mask;
      else if (digit === 2) tileY |= mask;
      else if (digit === 3) {
        tileX |= mask;
        tileY |= mask;
      }
    }

    return { tileX, tileY, zoom };
  }

  /**
   * Build an OGC WMS GetCapabilities request URL.
   * @param {string} baseUrl - Base WMS endpoint
   * @param {string} [version='1.3.0'] - WMS protocol version
   * @returns {string} Fully qualified GetCapabilities URL
   */
  static buildWmsCapabilitiesUrl(baseUrl, version = '1.3.0') {
    const separator = baseUrl.includes('?') ? '&' : '?';
    return `${baseUrl}${separator}SERVICE=WMS&REQUEST=GetCapabilities&VERSION=${encodeURIComponent(version)}`;
  }

  /**
   * Validate tile URL template placeholders.
   * @param {string} url - Template string
   * @returns {{valid: boolean, hasZ: boolean, hasX: boolean, hasY: boolean}}
   */
  static validateTileUrlTemplate(url) {
    if (typeof url !== 'string') return { valid: false, hasZ: false, hasX: false, hasY: false };
    const hasZ = url.includes('{z}');
    const hasX = url.includes('{x}');
    const hasY = url.includes('{y}') || url.includes('{-y}');
    return {
      valid: hasZ && hasX && hasY,
      hasZ,
      hasX,
      hasY
    };
  }
}

