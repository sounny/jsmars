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
      layers: [
        {
          id: "mars_mola",
          name: "MOLA Shaded Relief",
          type: "xyz",
          url: "https://cartocdn-gusc.global.ssl.fastly.net/opmbuilder/api/v1/map/named/opm-mars-basemap-v0-1/all/{z}/{x}/{y}.png",
          attribution: "OpenPlanetary"
        }
      ]
    },
    earth: {
      name: "Earth",
      center: [0, 0],
      zoom: 2,
      layers: [
        {
          id: "earth_blue_marble",
          name: "Blue Marble",
          type: "wms",
          url: "https://gibs.earthdata.nasa.gov/wms/epsg4326/best/wms.cgi",
          layers: "BlueMarble_NextGeneration",
          format: "image/jpeg",
          attribution: "NASA GIBS"
        }
      ]
    },
    moon: {
      name: "Moon",
      center: [0, 0],
      zoom: 2,
      layers: [
        {
          id: "moon_lro_wac",
          name: "LRO WAC",
          type: "wms",
          url: "https://planetarymaps.usgs.gov/cgi-bin/mapserv?map=/maps/moon/moon_simp_cyl.map",
          layers: "LRO_WAC_Mosaic_Global_303m",
          attribution: "USGS Astrogeology"
        }
      ]
    }
  }
};
