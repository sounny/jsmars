/**
 * @module jmars-wms
 * @description Utility class for interacting with WMS (Web Map Service) endpoints.
 *
 * Provides static methods to build GetCapabilities and GetFeatureInfo URLs,
 * fetch and parse WMS capabilities XML, and extract renderable layer metadata.
 *
 * All methods are static; this is effectively a namespace, not an instantiated class.
 */

/**
 * @class JMARSWMS
 * @description Static helper for WMS service interaction.
 */
export class JMARSWMS {
  /**
   * Construct a WMS GetCapabilities request URL.
   * @param {string} baseUrl - The base WMS service URL.
   * @param {string} [version='1.3.0'] - WMS protocol version.
   * @returns {string} The fully-qualified GetCapabilities URL.
   * @throws {TypeError} If baseUrl is not a valid URL.
   */
  static getCapabilitiesUrl(baseUrl, version = '1.3.0') {
    const url = new URL(baseUrl);
    url.searchParams.set('service', 'WMS');
    url.searchParams.set('request', 'GetCapabilities');
    url.searchParams.set('version', version);
    return url.toString();
  }

  /**
   * Fetch and parse WMS capabilities from a server.
   * @param {string} baseUrl - The base WMS service URL.
   * @returns {Promise<Array<{name: string, title: string, abstract: string, crs: string}>>}
   *   Array of renderable layer metadata objects.
   * @throws {Error} If the fetch fails or returns a non-OK status.
   */
  static async fetchCapabilities(baseUrl) {
    const url = this.getCapabilitiesUrl(baseUrl);
    try {
      const response = await fetch(url);
      if (!response.ok) {
        throw new Error(`WMS fetch failed: ${response.statusText}`);
      }
      const xmlText = await response.text();
      return this.parseCapabilities(xmlText);
    } catch (error) {
      console.warn('WMS capabilities fetch notice:', error.message || error);
      return [];
    }
  }

  /**
   * Parse WMS capabilities XML and extract renderable layers.
   *
   * In the WMS spec, layers with a `<Name>` element are renderable;
   * those without are merely organizational folders. This method
   * iterates only direct children of each `<Layer>` to avoid picking
   * up nested child element values by mistake.
   *
   * @param {string} xmlText - Raw XML string from GetCapabilities.
   * @returns {Array<{name: string, title: string, abstract: string, crs: string}>}
   *   Array of layer metadata objects.
   */
  static parseCapabilities(xmlText) {
    if (!xmlText || typeof xmlText !== 'string') return [];
    const parser = new DOMParser();
    const xmlDoc = parser.parseFromString(xmlText, 'text/xml');

    // Check for XML parse errors (DOMParser returns a <parsererror> element on failure)
    const parseError = xmlDoc.querySelector('parsererror');
    if (parseError) {
      console.warn('WMS capabilities XML parse notice:', parseError.textContent);
      return [];
    }

    const allLayers = Array.from(xmlDoc.getElementsByTagName('Layer'));
    const validLayers = [];

    allLayers.forEach(node => {
      // Iterate direct children only to extract Name, Title, Abstract
      // (getElementsByTagName would search descendants and find nested layers' values)
      let name = '';
      let title = '';
      let abstract = '';

      for (let i = 0; i < node.children.length; i++) {
        const child = node.children[i];
        // Strip namespace prefix if present (e.g., 'wms:Name' becomes 'Name')
        const nodeName = child.nodeName.split(':').pop();
        if (nodeName === 'Name') name = child.textContent;
        if (nodeName === 'Title') title = child.textContent;
        if (nodeName === 'Abstract') abstract = child.textContent;
      }

      // Only include layers that have both a Name (renderable) and Title (display label)
      if (name && title) {
        validLayers.push({
          name,
          title,
          abstract,
          crs: 'EPSG:4326' // Simplified; could be parsed from <CRS> elements
        });
      }
    });

    return validLayers;
  }

  /**
   * Construct a WMS GetFeatureInfo request URL.
   *
   * Handles differences between WMS 1.1.1 (uses SRS, x, y) and
   * WMS 1.3.0 (uses CRS, i, j).
   *
   * @param {string} baseUrl - Base WMS service URL.
   * @param {Object} params - Request parameters.
   * @param {string} params.layers - Comma-separated layer names.
   * @param {string} [params.query_layers] - Layers to query (defaults to params.layers).
   * @param {string} params.bbox - Bounding box string.
   * @param {number} params.width - Map width in pixels.
   * @param {number} params.height - Map height in pixels.
   * @param {number} params.x - Click position X (pixel).
   * @param {number} params.y - Click position Y (pixel).
   * @param {string} [params.crs='EPSG:4326'] - Coordinate reference system.
   * @param {string} [params.version='1.3.0'] - WMS version.
   * @param {string} [params.info_format='text/html'] - Response format.
   * @returns {string} The fully-qualified GetFeatureInfo URL.
   */
  static getFeatureInfoUrl(baseUrl, params) {
    const version = params.version || '1.3.0';
    const url = new URL(baseUrl);

    // Base WMS params
    url.searchParams.set('service', 'WMS');
    url.searchParams.set('version', version);
    url.searchParams.set('request', 'GetFeatureInfo');

    // Layers
    url.searchParams.set('layers', params.layers);
    url.searchParams.set('query_layers', params.query_layers || params.layers);

    // Spatial extent
    url.searchParams.set('bbox', params.bbox);
    url.searchParams.set('width', params.width);
    url.searchParams.set('height', params.height);

    // CRS vs SRS depends on WMS version
    url.searchParams.set(version === '1.3.0' ? 'crs' : 'srs', params.crs || 'EPSG:4326');

    // Click point: WMS 1.3.0 uses i/j, older versions use x/y
    if (version === '1.3.0') {
      url.searchParams.set('i', Math.round(params.x));
      url.searchParams.set('j', Math.round(params.y));
    } else {
      url.searchParams.set('x', Math.round(params.x));
      url.searchParams.set('y', Math.round(params.y));
    }

    // Response format
    url.searchParams.set('info_format', params.info_format || 'text/html');
    url.searchParams.set('styles', '');

    return url.toString();
  }
}
