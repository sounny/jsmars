/**
 * @module jmars-config
 * @description Application configuration for jsMars.
 *
 * Defines the default body, initial view, service endpoints, multi-body
 * layer configurations, and mosaic catalogs. This is the single source
 * of truth for all server URLs and body-specific settings.
 *
 * @typedef {Object} LayerConfig
 * @property {string} id - Unique layer identifier.
 * @property {string} name - Human-readable display name.
 * @property {string} type - Layer type: 'wms' or 'xyz'.
 * @property {string} url - Service URL (WMS endpoint or XYZ tile template).
 * @property {Object} options - Leaflet tile layer options (layers, format, attribution, etc.).
 *
 * @typedef {Object} BodyConfig
 * @property {string} name - Human-readable body name (e.g., "Mars").
 * @property {[number, number]} center - Default map center [lat, lng].
 * @property {number} zoom - Default zoom level.
 * @property {string} defaultLayer - ID of the layer shown on first load.
 * @property {LayerConfig[]} layers - Available base layers for this body.
 *
 * @typedef {Object} MosaicConfig
 * @property {string} id - Unique mosaic identifier.
 * @property {string} name - Display name.
 * @property {string} description - Human-readable description.
 * @property {string} type - Layer type.
 * @property {string} url - Service URL.
 * @property {Object} options - Leaflet tile layer options.
 * @property {string} thumbnail - Base64-encoded thumbnail image.
 */
export const JMARS_CONFIG = {
  /** @type {string} Default body shown on startup (canonical lowercase key). */
  body: 'mars',
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
    mars_wms: 'https://planetarymaps.usgs.gov/cgi-bin/mapserv?map=/maps/mars/mars_simp_cyl.map',
    // USGS Astrogeology Moon WMS
    moon_wms: 'https://planetarymaps.usgs.gov/cgi-bin/mapserv?map=/maps/moon/moon_simp_cyl.map',
    // NASA GIBS Earth WMS
    earth_wms: 'https://gibs.earthdata.nasa.gov/wms/epsg4326/best/wms.cgi'
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
        },
        {
          id: "earth_bluemarble_shaded_relief",
          name: "Blue Marble Shaded Relief",
          type: "wms",
          url: "https://gibs.earthdata.nasa.gov/wms/epsg4326/best/wms.cgi",
          options: {
            layers: "BlueMarble_ShadedRelief",
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
  },
  // Optional mosaic catalog per body
  mosaics: {
    mars: [
      {
        id: "mars_mdim21_mosaic",
        name: "MDIM 2.1 Global Mosaic",
        description: "Viking MDIM 2.1 global mosaic (256 px/deg)",
        type: "wms",
        url: "https://planetarymaps.usgs.gov/cgi-bin/mapserv?map=/maps/mars/mars_simp_cyl.map",
        options: {
          layers: "MDIM21",
          format: "image/png",
          transparent: true,
          attribution: "USGS Astrogeology"
        },
        thumbnail: "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAusB9YpK4n8AAAAASUVORK5CYII="
      },
      {
        id: "mars_themis_day_mosaic",
        name: "THEMIS IR Day 100m",
        description: "Mars Odyssey THEMIS IR Day global mosaic (100m)",
        type: "wms",
        url: "https://planetarymaps.usgs.gov/cgi-bin/mapserv?map=/maps/mars/mars_simp_cyl.map",
        options: {
          layers: "THEMIS",
          format: "image/png",
          transparent: true,
          attribution: "USGS Astrogeology"
        },
        thumbnail: "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAusB9YpK4n8AAAAASUVORK5CYII="
      },
      {
        id: "mars_mola_hillshade",
        name: "MOLA Hillshade",
        description: "MOLA shaded relief (global)",
        type: "wms",
        url: "https://planetarymaps.usgs.gov/cgi-bin/mapserv?map=/maps/mars/mars_simp_cyl.map",
        options: {
          layers: "MOLA_bw",
          format: "image/png",
          transparent: true,
          attribution: "USGS Astrogeology"
        },
        thumbnail: "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAusB9YpK4n8AAAAASUVORK5CYII="
      }
    ]
  }
};
