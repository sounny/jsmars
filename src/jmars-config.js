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
    earth_wms: 'https://gibs.earthdata.nasa.gov/wms/epsg4326/best/wms.cgi',
    // USGS Astrogeology Europa WMS
    europa_wms: 'https://planetarymaps.usgs.gov/cgi-bin/mapserv?map=/maps/jupiter/europa_simp_cyl.map'
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
    },
    europa: {
      name: "Europa",
      center: [0, 0],
      zoom: 2,
      defaultLayer: 'europa_galileo_voyager',
      layers: [
        {
          id: "europa_galileo_voyager",
          name: "Europa Global Mosaic (USGS WMS)",
          type: "wms",
          url: "https://planetarymaps.usgs.gov/cgi-bin/mapserv?map=/maps/jupiter/europa_simp_cyl.map",
          options: {
            layers: "GALILEO_VOYAGER",
            format: "image/png",
            transparent: true,
            attribution: "USGS Astrogeology / NASA / JPL"
          }
        },
        {
          id: "europa_trek_global_color",
          name: "Europa Global Color Blend 270m (NASA Trek)",
          type: "xyz",
          url: "https://trek.nasa.gov/tiles/Europa/EQ/20150218_europa_global_map_20000x10000/1.0.0//default/default028mm/{z}/{y}/{x}.png",
          options: {
            attribution: "NASA / JPL / DLR / Europa Trek",
            maxZoom: 5,
            tms: false
          }
        },
        {
          id: "europa_conamara_chaos",
          name: "Conamara Chaos 9m/px (Galileo SSI)",
          type: "xyz",
          url: "https://trek.nasa.gov/tiles/Europa/EQ/12ESCHAOS_01_GalileoSSI_Equi/1.0.0//default/default028mm/{z}/{y}/{x}.png",
          options: {
            attribution: "NASA / JPL / Galileo SSI / Europa Trek",
            maxZoom: 9,
            tms: false
          }
        },
        {
          id: "europa_pwyll_crater",
          name: "Pwyll Crater 55m/px (Galileo SSI)",
          type: "xyz",
          url: "https://trek.nasa.gov/tiles/Europa/EQ/GLL_SSI_Mosaic_55mpp_Pwyll/1.0.0//default/default028mm/{z}/{y}/{x}.png",
          options: {
            attribution: "NASA / JPL / Galileo SSI / Europa Trek",
            maxZoom: 9,
            tms: false
          }
        },
        {
          id: "europa_tyre_basin",
          name: "Tyre Multi-Ring Structure 35m/px (Galileo SSI)",
          type: "xyz",
          url: "https://trek.nasa.gov/tiles/Europa/EQ/GLL_SSI_Mosaic_35mpp_Tyre/1.0.0//default/default028mm/{z}/{y}/{x}.png",
          options: {
            attribution: "NASA / JPL / Galileo SSI / Europa Trek",
            maxZoom: 9,
            tms: false
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
    ],
    europa: [
      {
        id: "europa_galileo_voyager_mosaic",
        name: "Europa Global Mosaic (Voyager/Galileo)",
        description: "Global controlled mosaic of Europa synthesized from Galileo SSI and Voyager 1 & 2 spacecraft data (USGS Astrogeology).",
        type: "wms",
        url: "https://planetarymaps.usgs.gov/cgi-bin/mapserv?map=/maps/jupiter/europa_simp_cyl.map",
        options: {
          layers: "GALILEO_VOYAGER",
          format: "image/png",
          transparent: true,
          attribution: "USGS Astrogeology / NASA / JPL"
        },
        thumbnail: "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAusB9YpK4n8AAAAASUVORK5CYII="
      },
      {
        id: "europa_trek_color_mosaic",
        name: "Europa Global Color Blend (NASA Trek 270m)",
        description: "USGS/NASA Voyager and Galileo SSI global color blend mosaic at 270 m/px.",
        type: "xyz",
        url: "https://trek.nasa.gov/tiles/Europa/EQ/20150218_europa_global_map_20000x10000/1.0.0//default/default028mm/{z}/{y}/{x}.png",
        options: {
          attribution: "NASA / JPL / DLR / Europa Trek",
          maxZoom: 5
        },
        thumbnail: "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAusB9YpK4n8AAAAASUVORK5CYII="
      },
      {
        id: "europa_conamara_mosaic",
        name: "Conamara Chaos High-Res Mosaic (9m)",
        description: "Galileo SSI targeted flyby mosaic over Conamara Chaos fractured ice rafts at 9 m/px.",
        type: "xyz",
        url: "https://trek.nasa.gov/tiles/Europa/EQ/12ESCHAOS_01_GalileoSSI_Equi/1.0.0//default/default028mm/{z}/{y}/{x}.png",
        options: {
          attribution: "NASA / JPL / Galileo SSI / Europa Trek",
          maxZoom: 9
        },
        thumbnail: "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAusB9YpK4n8AAAAASUVORK5CYII="
      },
      {
        id: "europa_pwyll_mosaic",
        name: "Pwyll Crater High-Res Mosaic (55m)",
        description: "Galileo SSI mosaic of young impact crater Pwyll showing bright ray deposits at 55 m/px.",
        type: "xyz",
        url: "https://trek.nasa.gov/tiles/Europa/EQ/GLL_SSI_Mosaic_55mpp_Pwyll/1.0.0//default/default028mm/{z}/{y}/{x}.png",
        options: {
          attribution: "NASA / JPL / Galileo SSI / Europa Trek",
          maxZoom: 9
        },
        thumbnail: "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAusB9YpK4n8AAAAASUVORK5CYII="
      },
      {
        id: "europa_tyre_mosaic",
        name: "Tyre Multi-Ring Basin High-Res (35m)",
        description: "Galileo SSI mosaic of Tyre impact structure revealing sub-crustal impact excavation at 35 m/px.",
        type: "xyz",
        url: "https://trek.nasa.gov/tiles/Europa/EQ/GLL_SSI_Mosaic_35mpp_Tyre/1.0.0//default/default028mm/{z}/{y}/{x}.png",
        options: {
          attribution: "NASA / JPL / Galileo SSI / Europa Trek",
          maxZoom: 9
        },
        thumbnail: "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAusB9YpK4n8AAAAASUVORK5CYII="
      }
    ]
  }
};
