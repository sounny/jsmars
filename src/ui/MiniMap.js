/**
 * @module MiniMap
 * @description Provides a small overview map in the corner,
 * similar to the JMARS desktop panner.
 * Uses the Leaflet.MiniMap plugin, lazy-loaded from CDN.
 */
export class MiniMap {
  /**
   * @param {L.Map} map - Main Leaflet map
   * @param {string|object} baseMapOrConfig - XYZ tile URL or layer configuration object
   */
  constructor(map, baseMapOrConfig) {
    this.map = map;
    this.baseMapConfig = baseMapOrConfig;
    this.miniMap = null;
    this.isActive = false;
    this._loaded = false;
  }

  /**
   * Create a Leaflet tile layer suitable for the minimap from a config or URL.
   * Properly distinguishes WMS (USGS Europa, NASA Earth) from XYZ (OpenPlanetary Mars, Moon).
   * @param {string|object} source - Layer config object or XYZ URL string
   * @returns {L.TileLayer|L.TileLayer.WMS|null}
   * @private
   */
  _createTileLayer(source) {
    if (!source) return null;

    // If source is a string URL
    if (typeof source === 'string') {
      const isWms = source.includes('wms') || !source.includes('{z}');
      if (isWms) {
        return L.tileLayer.wms(source, {
          layers: 'GALILEO_VOYAGER',
          format: 'image/png',
          transparent: false,
          styles: '',
          minZoom: 0,
          maxZoom: 8,
          attribution: ''
        });
      }
      return L.tileLayer(source, {
        minZoom: 0,
        maxZoom: 8,
        attribution: ''
      });
    }

    // If source is a layerConfig object
    if (source.type === 'wms') {
      return L.tileLayer.wms(source.url, {
        layers: source.options?.layers || source.layers || '',
        format: source.options?.format || 'image/png',
        transparent: false,
        styles: '',
        minZoom: 0,
        maxZoom: 8,
        attribution: ''
      });
    }

    // Default to XYZ tile layer
    return L.tileLayer(source.url, {
      minZoom: 0,
      maxZoom: 8,
      attribution: '',
      ...(source.options || {})
    });
  }

  /**
   * Initialize and show the minimap.
   */
  async activate() {
    if (this.isActive) return;

    await this._ensurePlugin();

    const miniLayer = this._createTileLayer(this.baseMapConfig);
    if (!miniLayer) return;

    this.miniMap = new L.Control.MiniMap(miniLayer, {
      toggleDisplay: true,
      minimized: true,
      position: 'bottomright',
      width: 155,
      height: 105,
      zoomLevelOffset: -4,
      zoomLevelFixed: false,
      centerFixed: false,
      zoomAnimation: false,
      autoToggleDisplay: true,
      aimingRectOptions: {
        color: '#38bdf8',
        weight: 2,
        fillColor: '#38bdf8',
        fillOpacity: 0.18,
        dashArray: '4,4'
      },
      shadowRectOptions: {
        color: '#64748b',
        weight: 1,
        fillOpacity: 0,
        dashArray: '4,4'
      }
    });

    this.miniMap.addTo(this.map);
    this.isActive = true;
  }

  /**
   * Remove the minimap.
   */
  deactivate() {
    if (!this.isActive || !this.miniMap) return;
    this.map.removeControl(this.miniMap);
    this.miniMap = null;
    this.isActive = false;
  }

  /**
   * Toggle minimap visibility.
   * Async because activate() loads the plugin from CDN on first call.
   * @returns {Promise<boolean>} New active state
   */
  async toggle() {
    if (this.isActive) {
      this.deactivate();
    } else {
      await this.activate();
    }
    return this.isActive;
  }

  /**
   * Update the minimap basemap when body changes.
   * Uses changeLayer if active to seamlessly swap layers without destroying the control.
   * @param {string|object} baseMapOrConfig - New XYZ tile URL or layer configuration object
   */
  async updateBaseMap(baseMapOrConfig) {
    this.baseMapConfig = baseMapOrConfig;
    if (this.isActive && this.miniMap) {
      const newLayer = this._createTileLayer(baseMapOrConfig);
      if (newLayer && typeof this.miniMap.changeLayer === 'function') {
        this.miniMap.changeLayer(newLayer);
        return;
      }
      this.deactivate();
      await this.activate();
    }
  }

  /**
   * Lazy-load the Leaflet.MiniMap plugin from CDN.
   */
  async _ensurePlugin() {
    if (this._loaded || window.L?.Control?.MiniMap) {
      this._loaded = true;
      return;
    }

    // Load CSS
    const link = document.createElement('link');
    link.rel = 'stylesheet';
    link.href = 'https://cdnjs.cloudflare.com/ajax/libs/leaflet-minimap/3.6.1/Control.MiniMap.min.css';
    document.head.appendChild(link);

    // Load JS
    return new Promise((resolve, reject) => {
      const script = document.createElement('script');
      script.src = 'https://cdnjs.cloudflare.com/ajax/libs/leaflet-minimap/3.6.1/Control.MiniMap.min.js';
      script.onload = () => {
        this._loaded = true;
        resolve();
      };
      script.onerror = () => reject(new Error('Failed to load Leaflet.MiniMap'));
      document.head.appendChild(script);
    });
  }
}
