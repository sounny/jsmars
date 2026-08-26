/**
 * @module URLStateEngine
 * @description Deep-link URL state serializer and deserializer for JSMARS.
 * Encodes and decodes planetary body, map view center/zoom, active layers stack,
 * color stretch settings, Mars solar time (Ls), and point-of-interest (POI) markers.
 */

export class URLStateEngine {
  /**
   * Serialize active session state to a compact query string.
   * @param {Object} state - State options
   * @param {string} [state.body='mars'] - Planetary body key ('mars', 'moon', 'earth')
   * @param {number} [state.lat=0] - Center latitude in degrees
   * @param {number} [state.lon=0] - Center longitude in degrees
   * @param {number} [state.zoom=2] - Map zoom level
   * @param {Array<{id: string, opacity: number, visible: boolean}>} [state.activeLayers=[]] - Active layers stack
   * @param {Object} [state.colorStretch=null] - Color stretch settings { brightness, contrast, saturation, hueRotate, invert }
   * @param {number} [state.ls=null] - Solar longitude in degrees
   * @param {string} [state.poi=null] - Point of interest / placename search
   * @param {string} [baseUrl=''] - Base URL (defaults to current origin + pathname)
   * @returns {string} Fully formed deep-link URL
   */
  static serializeStateToURL(state = {}, baseUrl = '') {
    const params = new URLSearchParams();

    // Body
    if (state.body && state.body.toLowerCase() !== 'mars') {
      params.set('body', state.body.toLowerCase());
    }

    // View Coordinates
    if (typeof state.lat === 'number' && !isNaN(state.lat)) {
      params.set('lat', parseFloat(state.lat.toFixed(4)).toString());
    }
    if (typeof state.lon === 'number' && !isNaN(state.lon)) {
      params.set('lon', parseFloat(state.lon.toFixed(4)).toString());
    }
    if (typeof state.zoom === 'number' && !isNaN(state.zoom)) {
      params.set('z', Math.round(state.zoom).toString());
    }

    // Active Layers
    if (Array.isArray(state.activeLayers) && state.activeLayers.length > 0) {
      const layerTokens = state.activeLayers.map(l => {
        const op = typeof l.opacity === 'number' ? parseFloat(l.opacity.toFixed(2)) : 1;
        const vis = l.visible === false ? '0' : '1';
        return `${l.id}:${op}:${vis}`;
      });
      params.set('layers', layerTokens.join(','));
    }

    // Color Stretch
    if (state.colorStretch && typeof state.colorStretch === 'object') {
      const cs = state.colorStretch;
      const b = Math.round(cs.brightness ?? 100);
      const c = Math.round(cs.contrast ?? 100);
      const s = Math.round(cs.saturation ?? 100);
      const h = Math.round(cs.hueRotate ?? 0);
      const inv = cs.invert ? '1' : '0';
      // Only serialize if non-default
      if (b !== 100 || c !== 100 || s !== 100 || h !== 0 || inv !== '0') {
        params.set('stretch', `${b},${c},${s},${h},${inv}`);
      }
    }

    // Solar Longitude
    if (typeof state.ls === 'number' && !isNaN(state.ls)) {
      params.set('ls', parseFloat(state.ls.toFixed(2)).toString());
    }

    // Point of Interest
    if (state.poi && typeof state.poi === 'string' && state.poi.trim().length > 0) {
      params.set('poi', state.poi.trim());
    }

    const queryString = params.toString();
    const prefix = baseUrl || (typeof window !== 'undefined' ? `${window.location.origin}${window.location.pathname}` : '');

    return queryString ? `${prefix}?${queryString}` : prefix;
  }

  /**
   * Parse a deep-link URL or search query string into a structured state object.
   * @param {string} urlOrSearchString - Full URL or query string (e.g. "?lat=-14.5&lon=175.4&z=6&body=mars")
   * @returns {{hasState: boolean, body: string|null, lat: number|null, lon: number|null, zoom: number|null, activeLayers: Array<{id:string, opacity:number, visible:boolean}>, colorStretch: Object|null, ls: number|null, poi: string|null}}
   */
  static parseURLToState(urlOrSearchString = '') {
    let search = urlOrSearchString;
    if (search.includes('?')) {
      search = search.substring(search.indexOf('?'));
    } else if (search.includes('#')) {
      search = search.substring(search.indexOf('#') + 1);
    }

    const params = new URLSearchParams(search);
    let hasState = false;

    // Body
    let body = null;
    if (params.has('body')) {
      const b = params.get('body').toLowerCase();
      if (['mars', 'moon', 'earth'].includes(b)) {
        body = b;
        hasState = true;
      }
    }

    // View Coordinates
    let lat = null;
    let lon = null;
    let zoom = null;

    if (params.has('lat')) {
      const pLat = parseFloat(params.get('lat'));
      if (!isNaN(pLat) && pLat >= -90 && pLat <= 90) {
        lat = pLat;
        hasState = true;
      }
    }

    if (params.has('lon') || params.has('lng')) {
      const pLon = parseFloat(params.get('lon') || params.get('lng'));
      if (!isNaN(pLon)) {
        lon = pLon;
        hasState = true;
      }
    }

    if (params.has('z') || params.has('zoom')) {
      const pZ = parseInt(params.get('z') || params.get('zoom'), 10);
      if (!isNaN(pZ) && pZ >= 0 && pZ <= 20) {
        zoom = pZ;
        hasState = true;
      }
    }

    // Active Layers
    const activeLayers = [];
    if (params.has('layers')) {
      const rawLayers = params.get('layers').split(',');
      for (const token of rawLayers) {
        const parts = token.trim().split(':');
        if (parts[0]) {
          const id = parts[0];
          const opacity = parts.length > 1 ? parseFloat(parts[1]) : 1.0;
          const visible = parts.length > 2 ? parts[2] !== '0' : true;
          activeLayers.push({
            id,
            opacity: isNaN(opacity) ? 1.0 : Math.max(0, Math.min(1, opacity)),
            visible
          });
        }
      }
      if (activeLayers.length > 0) hasState = true;
    }

    // Color Stretch
    let colorStretch = null;
    if (params.has('stretch')) {
      const parts = params.get('stretch').split(',').map(s => s.trim());
      if (parts.length >= 4) {
        const brightness = parseInt(parts[0], 10);
        const contrast = parseInt(parts[1], 10);
        const saturation = parseInt(parts[2], 10);
        const hueRotate = parseInt(parts[3], 10);
        const invert = parts.length > 4 ? parts[4] === '1' : false;

        if (!isNaN(brightness) && !isNaN(contrast) && !isNaN(saturation) && !isNaN(hueRotate)) {
          colorStretch = {
            brightness: Math.max(0, Math.min(300, brightness)),
            contrast: Math.max(0, Math.min(300, contrast)),
            saturation: Math.max(0, Math.min(300, saturation)),
            hueRotate: Math.max(0, Math.min(360, hueRotate)),
            invert
          };
          hasState = true;
        }
      }
    }

    // Solar Longitude
    let ls = null;
    if (params.has('ls')) {
      const pLs = parseFloat(params.get('ls'));
      if (!isNaN(pLs) && pLs >= 0 && pLs <= 360) {
        ls = pLs;
        hasState = true;
      }
    }

    // Point of Interest
    let poi = null;
    if (params.has('poi')) {
      try {
        poi = decodeURIComponent(params.get('poi')).trim();
        if (poi) hasState = true;
      } catch (e) {
        poi = params.get('poi');
      }
    }

    return {
      hasState,
      body,
      lat,
      lon,
      zoom,
      activeLayers,
      colorStretch,
      ls,
      poi
    };
  }

  /**
   * Synchronize current state to browser address bar without triggering a page reload.
   * @param {Object} state - State to synchronize
   * @param {boolean} [replace=true] - Use history.replaceState (true) or pushState (false)
   */
  static syncToBrowserURL(state = {}, replace = true) {
    if (typeof window === 'undefined' || !window.history) return;
    const url = URLStateEngine.serializeStateToURL(state, window.location.origin + window.location.pathname);
    if (replace) {
      window.history.replaceState(null, '', url);
    } else {
      window.history.pushState(null, '', url);
    }
  }

  /**
   * Copy the serialized deep-link URL for current state to the system clipboard.
   * @param {Object} state - State to serialize
   * @returns {Promise<string>} The generated URL copied to clipboard
   */
  static async copyShareLink(state = {}) {
    const url = URLStateEngine.serializeStateToURL(state);
    if (typeof navigator !== 'undefined' && navigator.clipboard && navigator.clipboard.writeText) {
      await navigator.clipboard.writeText(url);
    }
    return url;
  }
}
