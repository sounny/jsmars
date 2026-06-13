import { JMARS_CONFIG } from '../jmars-config.js';

/**
 * @module layers
 * @description Default layer definitions for the jsMars application.
 * Provides a static list of pre-configured layers and a factory
 * function to create Leaflet tile layers from config objects.
 */

/**
 * Pre-configured layer definitions.
 * @type {Array<{id: string, name: string, type: string, url: string, options: object}>}
 */
export const layers = [
  {
    id: 'mars_viking',
    name: 'Mars Viking (OpenPlanetary)',
    type: 'xyz',
    url: JMARS_CONFIG.services.mars_basemap,
    options: {
      attribution: 'OpenPlanetary',
      maxZoom: 10
    }
  },
  {
    id: 'mars_wms_viking',
    name: 'Mars Viking MDIM2.1 (USGS WMS)',
    type: 'wms',
    url: JMARS_CONFIG.services.mars_wms,
    options: {
      layers: 'MDIM21',
      format: 'image/png',
      transparent: true,
      attribution: 'USGS Astrogeology',
      maxZoom: 10
    }
  }
];

/**
 * Create a Leaflet tile layer from a layer configuration object.
 * Supports 'wms' and 'xyz' types. Logs a warning and returns null
 * for unknown layer types.
 * @param {object} layerConfig - Layer configuration
 * @param {string} layerConfig.type - Layer type ('wms' or 'xyz')
 * @param {string} layerConfig.url - Tile service URL
 * @param {object} [layerConfig.options] - Leaflet layer options
 * @returns {L.TileLayer|L.TileLayer.WMS|null} The created layer, or null
 */
export function createLeafletLayer(layerConfig) {
  if (layerConfig.type === 'wms') {
    return L.tileLayer.wms(layerConfig.url, layerConfig.options);
  } else if (layerConfig.type === 'xyz') {
    return L.tileLayer(layerConfig.url, layerConfig.options);
  }
  console.warn(`createLeafletLayer: unknown layer type "${layerConfig.type}" for layer "${layerConfig.id || '(no id)'}"`);
  return null;
}
