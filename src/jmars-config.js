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
    mars_basemap: 'https://cartocdn-gusc.global.ssl.fastly.net/opmbuilder/api/v1/map/named/opm-mars-basemap-v0-1/all/{z}/{x}/{y}.png',
    // USGS Astrogeology Mars WMS
    mars_wms: 'https://planetarymaps.usgs.gov/cgi-bin/mapserv?map=/maps/mars/mars_simp_cyl.map'
  },
  // Multi-body configurations
  bodies: {
    mars: {
      name: "Mars",
      center: [0, 0],
      zoom: 2,
      defaultLayer: 'mars_viking',
      layers: [
        {
          id: "mars_viking",
          name: "Mars Viking (OpenPlanetary)",
          type: "xyz",
          url: "https://cartocdn-gusc.global.ssl.fastly.net/opmbuilder/api/v1/map/named/opm-mars-basemap-v0-1/all/{z}/{x}/{y}.png",
          options: {
            attribution: "OpenPlanetary",
            maxZoom: 10
          }
        },
        {
          id: "mars_wms_viking",
          name: "Mars Viking MDIM2.1 (USGS WMS)",
          type: "wms",
          url: "https://planetarymaps.usgs.gov/cgi-bin/mapserv?map=/maps/mars/mars_simp_cyl.map",
          options: {
            layers: "MDIM21",
            format: "image/png",
            transparent: true,
            attribution: "USGS Astrogeology"
          }
        }
      ]
    },
    earth: {
      name: "Earth",
      center: [0, 0],
      zoom: 2,
      defaultLayer: 'earth_blue_marble',
      layers: [
        {
          id: "earth_blue_marble",
          name: "Blue Marble",
          type: "wms",
          url: "https://gibs.earthdata.nasa.gov/wms/epsg4326/best/wms.cgi",
          options: {
            layers: "BlueMarble_NextGeneration",
            format: "image/jpeg",
            transparent: true,
            attribution: "NASA GIBS"
          }
        }
      ]
    },
    moon: {
      name: "Moon",
      center: [0, 0],
      zoom: 2,
      defaultLayer: 'moon_opm_basemap',
      layers: [
        {
          id: "moon_opm_basemap",
          name: "Moon Basemap (OpenPlanetary)",
          type: "xyz",
          url: "https://cartocdn-gusc.global.ssl.fastly.net/opmbuilder/api/v1/map/named/opm-moon-basemap-v0-1/all/{z}/{x}/{y}.png",
          options: {
            attribution: "OpenPlanetary",
            maxZoom: 10
          }
        }
      ]
    }
  }
};
