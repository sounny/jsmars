import { JMARS_CONFIG } from '../jmars-config.js';

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

export function createLeafletLayer(layerConfig) {
  if (layerConfig.type === 'wms') {
    return L.tileLayer.wms(layerConfig.url, layerConfig.options);
  } else if (layerConfig.type === 'xyz') {
    return L.tileLayer(layerConfig.url, layerConfig.options);
  }
  return null;
}
