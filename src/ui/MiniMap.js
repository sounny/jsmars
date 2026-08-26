/**
 * @module MiniMap
 * @description Provides a small overview map in the corner,
 * similar to the JMARS desktop panner.
 * Uses the Leaflet.MiniMap plugin, lazy-loaded from CDN.
 */
export class MiniMap {
  /**
   * @param {L.Map} map - Main Leaflet map
   * @param {string} baseMapUrl - XYZ tile URL for the minimap
   */
  constructor(map, baseMapUrl) {
    this.map = map;
    this.baseMapUrl = baseMapUrl;
    this.miniMap = null;
    this.isActive = false;
    this._loaded = false;
  }

  /**
   * Initialize and show the minimap.
   */
  async activate() {
    if (this.isActive) return;

    await this._ensurePlugin();

    const miniLayer = L.tileLayer(this.baseMapUrl, {
      minZoom: 0,
      maxZoom: 8,
      attribution: ''
    });

    this.miniMap = new L.Control.MiniMap(miniLayer, {
      toggleDisplay: true,
      minimized: true,
      position: 'bottomright',
      width: 140,
      height: 95,
      zoomLevelOffset: -4,
      zoomLevelFixed: false,
      centerFixed: false,
      zoomAnimation: false,
      autoToggleDisplay: true,
      aimingRectOptions: {
        color: '#4dabf7',
        weight: 1.5,
        fillOpacity: 0.1,
        dashArray: '3,3'
      },
      shadowRectOptions: {
        color: '#888',
        weight: 1,
        fillOpacity: 0,
        dashArray: '5,5'
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
   * Async because re-activation loads the plugin if needed.
   * @param {string} baseMapUrl - New XYZ tile URL
   */
  async updateBaseMap(baseMapUrl) {
    this.baseMapUrl = baseMapUrl;
    if (this.isActive) {
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
