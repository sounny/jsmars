import { JMARS_CONFIG } from './jmars-config.js';

/**
 * Helper for interacting with WMS services.
 */
export class JMARSWMS {
  /**
   * Constructs a GetCapabilities URL.
   * @param {string} baseUrl - The base URL of the WMS service.
   * @param {string} version - WMS version (default 1.3.0).
   * @returns {string} - The GetCapabilities URL.
   */
  static getCapabilitiesUrl(baseUrl, version = '1.3.0') {
    const url = new URL(baseUrl);
    url.searchParams.set('service', 'WMS');
    url.searchParams.set('request', 'GetCapabilities');
    url.searchParams.set('version', version);
    return url.toString();
  }

  /**
   * Fetches WMS capabilities (XML).
   * @param {string} baseUrl - The base URL.
   * @returns {Promise<object[]>} - A list of layer objects parsed from XML.
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
      console.error('Error fetching capabilities:', error);
      // Propagate error so UI can show it
      throw error;
    }
  }

  /**
   * Parses the Capabilities XML and extracts layers.
   * @param {string} xmlText - Raw XML string.
   * @returns {object[]} - Array of layer info objects.
   */
  static parseCapabilities(xmlText) {
    const parser = new DOMParser();
    const xmlDoc = parser.parseFromString(xmlText, "text/xml");

    // Handle namespaces: WMS 1.3.0 usually has a default namespace.
    // Using getElementsByTagName is safer than querySelector for namespaced XML if we don't care about structure depth.

    // First, try to find the main Capability Layer
    // The structure is usually <WMS_Capabilities><Capability><Layer>... and then nested <Layer>s

    // We want leaf layers or at least layers with a Name.
    const allLayers = Array.from(xmlDoc.getElementsByTagName('Layer'));
    const validLayers = [];

    allLayers.forEach(node => {
      // In WMS, layers with a Name are renderable. Layers without a Name are just folders.
      // However, we need to check children.

      const nameNode = node.getElementsByTagName('Name')[0];
      const titleNode = node.getElementsByTagName('Title')[0];
      const abstractNode = node.getElementsByTagName('Abstract')[0];

      // Note: getElementsByTagName searches descendants, so if we are at a Folder Layer,
      // finding 'Name' might find the child's name.
      // We need direct children check or rely on the fact that 'Name' usually comes first.
      // Better approach: Check if 'Name' is a direct child or close enough.

      // Actually, let's just iterate and look for direct children if possible, or use careful selection.
      // Converting to loop over children is safer.

      let name = '';
      let title = '';
      let abstract = '';

      for (let i = 0; i < node.children.length; i++) {
          const child = node.children[i];
          // Strip namespace prefix if any
          const nodeName = child.nodeName.split(':').pop();
          if (nodeName === 'Name') name = child.textContent;
          if (nodeName === 'Title') title = child.textContent;
          if (nodeName === 'Abstract') abstract = child.textContent;
      }

      if (name && title) {
        validLayers.push({
          name: name,
          title: title,
          abstract: abstract,
          crs: 'EPSG:4326' // Simplified
        });
      }
    });

    return validLayers;
  }

  /**
   * Constructs a GetFeatureInfo URL.
   * @param {string} baseUrl - Base WMS URL.
   * @param {object} params - { layers, query_layers, bbox, width, height, x, y, crs, version }
   * @returns {string}
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
    
    // Spatial
    url.searchParams.set('bbox', params.bbox);
    url.searchParams.set('width', params.width);
    url.searchParams.set('height', params.height);
    url.searchParams.set(version === '1.3.0' ? 'crs' : 'srs', params.crs || 'EPSG:4326');
    
    // Point
    if (version === '1.3.0') {
        url.searchParams.set('i', Math.round(params.x));
        url.searchParams.set('j', Math.round(params.y));
    } else {
        url.searchParams.set('x', Math.round(params.x));
        url.searchParams.set('y', Math.round(params.y));
    }
    
    // Format
    url.searchParams.set('info_format', params.info_format || 'text/html');
    url.searchParams.set('styles', '');

    return url.toString();
  }
}
