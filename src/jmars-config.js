export const JMARS_CONFIG = {
  // Default to Mars
  body: 'Mars',
  // Initial view
  initialView: {
    lat: 0,
    lng: 0,
    zoom: 2
  },
  // Service endpoints
  services: {
    // OpenPlanetary Mars Basemap (XYZ)
    // We use this because public WMS endpoints for Mars are currently unstable or 404ing.
    mars_basemap: 'https://cartocdn-gusc.global.ssl.fastly.net/opmbuilder/api/v1/map/named/opm-mars-basemap-v0-1/all/{z}/{x}/{y}.png',

    // USGS Astrogeology Mars WMS
    // Found via search: https://planetarymaps.usgs.gov/cgi-bin/mapserv?map=/maps/mars/mars_simp_cyl.map
    mars_wms: 'https://planetarymaps.usgs.gov/cgi-bin/mapserv?map=/maps/mars/mars_simp_cyl.map'
  }
};
