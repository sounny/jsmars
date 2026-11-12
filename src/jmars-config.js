/**
 * @module jmars-config
 * @description Application configuration for jsMars.
 *
 * Defines the default body, initial view, service endpoints, multi-body
 * layer configurations across 26 Solar System worlds (matching ASU JMARS
 * planetary systems with verified HTTPS + CORS spacecraft basemaps),
 * and mosaic catalogs.
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
 * @property {string} [group] - Planetary system category label.
 * @property {[number, number]} center - Default map center [lat, lng].
 * @property {number} zoom - Default zoom level.
 * @property {string} defaultLayer - ID of the layer shown on first load.
 * @property {LayerConfig[]} layers - Available base layers for this body.
 */

const THUMB_1PX = 'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAusB9YpK4n8AAAAASUVORK5CYII=';

export const JMARS_CONFIG = {
  /** @type {string} Default body shown on startup (canonical lowercase key). */
  body: 'mars',
  // Initial view
  initialView: {
    lat: 0,
    lng: 0,
    zoom: 2
  },
  // Verified HTTPS + CORS OGC Web Map Service & XYZ endpoints
  services: {
    mars_basemap: 'https://cartocdn-gusc.global.ssl.fastly.net/opmbuilder/api/v1/map/named/opm-mars-basemap-v0-1/all/{z}/{x}/{y}.png',
    mercury_wms: 'https://planetarymaps.usgs.gov/cgi-bin/mapserv?map=/maps/mercury/mercury_simp_cyl.map',
    venus_wms: 'https://planetarymaps.usgs.gov/cgi-bin/mapserv?map=/maps/venus/venus_simp_cyl.map',
    earth_wms: 'https://tiles.maps.eox.at/wms',
    moon_wms: 'https://planetarymaps.usgs.gov/cgi-bin/mapserv?map=/maps/earth/moon_simp_cyl.map',
    mars_wms: 'https://planetarymaps.usgs.gov/cgi-bin/mapserv?map=/maps/mars/mars_simp_cyl.map',
    phobos_wms: 'https://planetarymaps.usgs.gov/cgi-bin/mapserv?map=/maps/mars/phobos_simp_cyl.map',
    deimos_wms: 'https://planetarymaps.usgs.gov/cgi-bin/mapserv?map=/maps/mars/deimos_simp_cyl.map',
    jupiter_wms: 'https://planetarymaps.usgs.gov/cgi-bin/mapserv?map=/maps/jupiter/jupiter_simp_cyl.map',
    io_wms: 'https://planetarymaps.usgs.gov/cgi-bin/mapserv?map=/maps/jupiter/io_simp_cyl.map',
    europa_wms: 'https://planetarymaps.usgs.gov/cgi-bin/mapserv?map=/maps/jupiter/europa_simp_cyl.map',
    ganymede_wms: 'https://planetarymaps.usgs.gov/cgi-bin/mapserv?map=/maps/jupiter/ganymede_simp_cyl.map',
    callisto_wms: 'https://planetarymaps.usgs.gov/cgi-bin/mapserv?map=/maps/jupiter/callisto_simp_cyl.map',
    saturn_wms: 'https://planetarymaps.usgs.gov/cgi-bin/mapserv?map=/maps/saturn/saturn_simp_cyl.map',
    mimas_wms: 'https://planetarymaps.usgs.gov/cgi-bin/mapserv?map=/maps/saturn/mimas_simp_cyl.map',
    enceladus_wms: 'https://planetarymaps.usgs.gov/cgi-bin/mapserv?map=/maps/saturn/enceladus_simp_cyl.map',
    tethys_wms: 'https://planetarymaps.usgs.gov/cgi-bin/mapserv?map=/maps/saturn/tethys_simp_cyl.map',
    dione_wms: 'https://planetarymaps.usgs.gov/cgi-bin/mapserv?map=/maps/saturn/dione_simp_cyl.map',
    rhea_wms: 'https://planetarymaps.usgs.gov/cgi-bin/mapserv?map=/maps/saturn/rhea_simp_cyl.map',
    titan_wms: 'https://planetarymaps.usgs.gov/cgi-bin/mapserv?map=/maps/saturn/titan_simp_cyl.map',
    iapetus_wms: 'https://planetarymaps.usgs.gov/cgi-bin/mapserv?map=/maps/saturn/iapetus_simp_cyl.map',
    phoebe_wms: 'https://planetarymaps.usgs.gov/cgi-bin/mapserv?map=/maps/saturn/phoebe_simp_cyl.map',
    triton_wms: 'https://planetarymaps.usgs.gov/cgi-bin/mapserv?map=/maps/neptune/triton_simp_cyl.map',
    pluto_wms: 'https://planetarymaps.usgs.gov/cgi-bin/mapserv?map=/maps/pluto/pluto_simp_cyl.map',
    charon_wms: 'https://planetarymaps.usgs.gov/cgi-bin/mapserv?map=/maps/pluto/charon_simp_cyl.map'
  },

  // Multi-body configurations (26 verified Solar System worlds)
  bodies: {
    // ── Inner Planets & Moon ────────────────────────────────────────────────
    mars: {
      name: 'Mars',
      group: 'Inner Planets & Moon',
      center: [0, 0],
      zoom: 2,
      defaultLayer: 'mars_viking',
      layers: [
        {
          id: 'mars_viking',
          name: 'Mars Viking (OpenPlanetary)',
          type: 'xyz',
          url: 'https://cartocdn-gusc.global.ssl.fastly.net/opmbuilder/api/v1/map/named/opm-mars-basemap-v0-1/all/{z}/{x}/{y}.png',
          options: {
            attribution: 'OpenPlanetary',
            maxZoom: 10
          }
        },
        {
          id: 'mars_wms_viking',
          name: 'Mars Viking MDIM2.1 (USGS WMS)',
          type: 'wms',
          url: 'https://planetarymaps.usgs.gov/cgi-bin/mapserv?map=/maps/mars/mars_simp_cyl.map',
          options: {
            layers: 'MDIM21',
            format: 'image/png',
            transparent: true,
            attribution: 'USGS Astrogeology'
          }
        }
      ]
    },
    moon: {
      name: 'Moon',
      group: 'Inner Planets & Moon',
      center: [0, 0],
      zoom: 2,
      defaultLayer: 'moon_lro_wac',
      layers: [
        {
          id: 'moon_lro_wac',
          name: 'LRO WAC Global Mosaic 100m (NASA Trek)',
          type: 'xyz',
          url: 'https://trek.nasa.gov/tiles/Moon/EQ/LRO_WAC_Mosaic_Global_303ppd_v02/1.0.0/default/default028mm/{z}/{y}/{x}.jpg',
          options: {
            attribution: 'NASA / GSFC / ASU / Moon Trek',
            maxNativeZoom: 8,
            maxZoom: 12,
            tms: false
          }
        },
        {
          id: 'moon_opm_basemap',
          name: 'Moon Basemap (OpenPlanetary)',
          type: 'xyz',
          url: 'https://cartocdn-gusc.global.ssl.fastly.net/opmbuilder/api/v1/map/named/opm-moon-basemap-v0-1/all/{z}/{x}/{y}.png',
          options: {
            attribution: 'OpenPlanetary',
            maxZoom: 10
          }
        },
        {
          id: 'moon_lola_color',
          name: 'LOLA Elevation Color Relief (USGS WMS)',
          type: 'wms',
          url: 'https://planetarymaps.usgs.gov/cgi-bin/mapserv?map=/maps/earth/moon_simp_cyl.map',
          options: {
            layers: 'LOLA_color',
            format: 'image/png',
            transparent: true,
            attribution: 'USGS Astrogeology / NASA / GSFC'
          }
        },
        {
          id: 'moon_lola_bw',
          name: 'LOLA Shaded Relief Grayscale (USGS WMS)',
          type: 'wms',
          url: 'https://planetarymaps.usgs.gov/cgi-bin/mapserv?map=/maps/earth/moon_simp_cyl.map',
          options: {
            layers: 'LOLA_bw',
            format: 'image/png',
            transparent: true,
            attribution: 'USGS Astrogeology / NASA / GSFC'
          }
        },
        {
          id: 'moon_kaguya_tc',
          name: 'Kaguya TC Orthomosaic 10m (USGS WMS)',
          type: 'wms',
          url: 'https://planetarymaps.usgs.gov/cgi-bin/mapserv?map=/maps/earth/moon_simp_cyl.map',
          options: {
            layers: 'KaguyaTC_Ortho',
            format: 'image/png',
            transparent: true,
            attribution: 'USGS Astrogeology / JAXA / SELENE'
          }
        },
        {
          id: 'moon_clementine_uvvis',
          name: 'Clementine UVVIS Ratio (USGS WMS)',
          type: 'wms',
          url: 'https://planetarymaps.usgs.gov/cgi-bin/mapserv?map=/maps/earth/moon_simp_cyl.map',
          options: {
            layers: 'uv_v2',
            format: 'image/png',
            transparent: true,
            attribution: 'USGS Astrogeology / NRL / BMDO'
          }
        }
      ]
    },
    earth: {
      name: 'Earth',
      group: 'Inner Planets & Moon',
      center: [0, 0],
      zoom: 2,
      defaultLayer: 'earth_blue_marble',
      layers: [
        {
          id: 'earth_s2_cloudless',
          name: 'Sentinel-2 Cloudless Global 10m (ESA Copernicus / EOX)',
          type: 'wms',
          url: 'https://tiles.maps.eox.at/wms',
          options: {
            layers: 's2cloudless-2020',
            format: 'image/jpeg',
            transparent: false,
            maxZoom: 14,
            attribution: 'Sentinel-2 cloudless - https://s2maps.eu by EOX IT Services GmbH (Contains modified Copernicus Sentinel data 2020)'
          }
        },
        {
          id: 'earth_blue_marble',
          name: 'Blue Marble Next Generation (NASA / EOX WMS)',
          type: 'wms',
          url: 'https://tiles.maps.eox.at/wms',
          options: {
            layers: 'bluemarble',
            format: 'image/jpeg',
            transparent: false,
            maxZoom: 13,
            attribution: 'Blue Marble © NASA Earth Observatory; hosted by EOX IT Services GmbH'
          }
        },
        {
          id: 'earth_bluemarble_shaded_relief',
          name: 'Global Terrain & Shaded Relief (EOX / Copernicus)',
          type: 'wms',
          url: 'https://tiles.maps.eox.at/wms',
          options: {
            layers: 'terrain',
            format: 'image/jpeg',
            transparent: false,
            maxZoom: 13,
            attribution: 'Terrain © EOX IT Services GmbH (Copernicus DEM / SRTM)'
          }
        },
        {
          id: 'earth_black_marble',
          name: 'Black Marble Nighttime Lights (NASA / EOX WMS)',
          type: 'wms',
          url: 'https://tiles.maps.eox.at/wms',
          options: {
            layers: 'blackmarble',
            format: 'image/jpeg',
            transparent: false,
            maxZoom: 13,
            attribution: 'Black Marble © NASA Earth Observatory; hosted by EOX IT Services GmbH'
          }
        }
      ]
    },
    mercury: {
      name: 'Mercury',
      group: 'Inner Planets & Moon',
      center: [0, 0],
      zoom: 2,
      defaultLayer: 'mercury_messenger',
      layers: [
        {
          id: 'mercury_messenger',
          name: 'MESSENGER Global Mosaic 166m (USGS WMS)',
          type: 'wms',
          url: 'https://planetarymaps.usgs.gov/cgi-bin/mapserv?map=/maps/mercury/mercury_simp_cyl.map',
          options: {
            layers: 'MESSENGER',
            format: 'image/png',
            transparent: true,
            attribution: 'USGS Astrogeology / NASA / JHU APL / CIW'
          }
        },
        {
          id: 'mercury_messenger_color',
          name: 'MESSENGER MDIS Enhanced Color v3 (USGS WMS)',
          type: 'wms',
          url: 'https://planetarymaps.usgs.gov/cgi-bin/mapserv?map=/maps/mercury/mercury_simp_cyl.map',
          options: {
            layers: 'MESSENGER_Color',
            format: 'image/png',
            transparent: true,
            attribution: 'USGS Astrogeology / NASA / JHU APL / CIW'
          }
        },
        {
          id: 'mercury_trek_enhanced_color',
          name: 'MESSENGER MDIS Enhanced Color 665m (NASA Trek)',
          type: 'xyz',
          url: 'https://trek.nasa.gov/tiles/Mercury/EQ/Mercury_MESSENGER_MDIS_Basemap_EnhancedColor_Mosaic_Global_665m/1.0.0/default/default028mm/{z}/{y}/{x}.jpg',
          options: {
            attribution: 'NASA / JHU APL / CIW / Mercury Trek',
            maxNativeZoom: 7,
            maxZoom: 12,
            tms: false
          }
        },
        {
          id: 'mercury_mariner10',
          name: 'Mariner 10 Global Mosaic (USGS WMS)',
          type: 'wms',
          url: 'https://planetarymaps.usgs.gov/cgi-bin/mapserv?map=/maps/mercury/mercury_simp_cyl.map',
          options: {
            layers: 'MARINER',
            format: 'image/png',
            transparent: true,
            attribution: 'USGS Astrogeology / NASA / JPL'
          }
        }
      ]
    },
    venus: {
      name: 'Venus',
      group: 'Inner Planets & Moon',
      center: [0, 0],
      zoom: 2,
      defaultLayer: 'venus_magellan_color',
      layers: [
        {
          id: 'venus_magellan_color',
          name: 'Magellan Colorized Global SAR Mosaic (USGS WMS)',
          type: 'wms',
          url: 'https://planetarymaps.usgs.gov/cgi-bin/mapserv?map=/maps/venus/venus_simp_cyl.map',
          options: {
            layers: 'MAGELLAN_color',
            format: 'image/png',
            transparent: true,
            attribution: 'USGS Astrogeology / NASA / JPL Magellan'
          }
        },
        {
          id: 'venus_magellan_sar',
          name: 'Magellan Left-Look SAR Mosaic 75m (USGS WMS)',
          type: 'wms',
          url: 'https://planetarymaps.usgs.gov/cgi-bin/mapserv?map=/maps/venus/venus_simp_cyl.map',
          options: {
            layers: 'MAGELLAN',
            format: 'image/png',
            transparent: true,
            attribution: 'USGS Astrogeology / NASA / JPL Magellan'
          }
        },
        {
          id: 'venus_magellan_topo',
          name: 'Magellan Colorized Radar Topography (USGS WMS)',
          type: 'wms',
          url: 'https://planetarymaps.usgs.gov/cgi-bin/mapserv?map=/maps/venus/venus_simp_cyl.map',
          options: {
            layers: 'MAGELLAN_topography',
            format: 'image/png',
            transparent: true,
            attribution: 'USGS Astrogeology / NASA / JPL / MIT'
          }
        }
      ]
    },

    // ── Mars Moons ──────────────────────────────────────────────────────────
    phobos: {
      name: 'Phobos',
      group: 'Mars Moons',
      center: [0, 0],
      zoom: 2,
      defaultLayer: 'phobos_viking',
      layers: [
        {
          id: 'phobos_viking',
          name: 'Phobos Viking Global Shaded Relief (USGS WMS)',
          type: 'wms',
          url: 'https://planetarymaps.usgs.gov/cgi-bin/mapserv?map=/maps/mars/phobos_simp_cyl.map',
          options: {
            layers: 'VIKING',
            format: 'image/png',
            transparent: true,
            attribution: 'USGS Astrogeology / NASA Viking'
          }
        }
      ]
    },
    deimos: {
      name: 'Deimos',
      group: 'Mars Moons',
      center: [0, 0],
      zoom: 2,
      defaultLayer: 'deimos_viking',
      layers: [
        {
          id: 'deimos_viking',
          name: 'Deimos Viking Global Mosaic (USGS WMS)',
          type: 'wms',
          url: 'https://planetarymaps.usgs.gov/cgi-bin/mapserv?map=/maps/mars/deimos_simp_cyl.map',
          options: {
            layers: 'VIKING',
            format: 'image/png',
            transparent: true,
            attribution: 'USGS Astrogeology / NASA Viking'
          }
        }
      ]
    },

    // ── Asteroids & Dwarf Planets ───────────────────────────────────────────
    ceres: {
      name: 'Ceres',
      group: 'Asteroids & Dwarf Planets',
      center: [0, 0],
      zoom: 2,
      defaultLayer: 'ceres_dawn_hamo_shade',
      layers: [
        {
          id: 'ceres_dawn_hamo_shade',
          name: 'Dawn FC HAMO Shaded Relief 140m (NASA Trek)',
          type: 'xyz',
          url: 'https://trek.nasa.gov/tiles/Ceres/EQ/Ceres_Dawn_FC_HAMO_Shade_DLR_Global_60ppd_Oct2016/1.0.0/default/default028mm/{z}/{y}/{x}.jpg',
          options: {
            attribution: 'NASA / JPL-Caltech / UCLA / MPS / DLR / IDA / Ceres Trek',
            maxNativeZoom: 5,
            maxZoom: 11,
            tms: false
          }
        },
        {
          id: 'ceres_dawn_hamo_clrshade',
          name: 'Dawn FC HAMO Colorized Elevation 140m (NASA Trek)',
          type: 'xyz',
          url: 'https://trek.nasa.gov/tiles/Ceres/EQ/Ceres_Dawn_FC_HAMO_ClrShade_DLR_Global_60ppd_Oct2016/1.0.0/default/default028mm/{z}/{y}/{x}.jpg',
          options: {
            attribution: 'NASA / JPL-Caltech / UCLA / MPS / DLR / IDA / Ceres Trek',
            maxNativeZoom: 5,
            maxZoom: 11,
            tms: false
          }
        }
      ]
    },
    vesta: {
      name: 'Vesta',
      group: 'Asteroids & Dwarf Planets',
      center: [0, 0],
      zoom: 2,
      defaultLayer: 'vesta_dawn_blend',
      layers: [
        {
          id: 'vesta_dawn_blend',
          name: 'Dawn FC HAMO/LAMO Global Mosaic (NASA Trek)',
          type: 'xyz',
          url: 'https://trek.nasa.gov/tiles/Vesta/EQ/VestaHAMOLAMOBlend.eq/1.0.0/default/default028mm/{z}/{y}/{x}.png',
          options: {
            attribution: 'NASA / JPL-Caltech / UCLA / MPS / DLR / IDA / Vesta Trek',
            maxNativeZoom: 7,
            maxZoom: 12,
            tms: false
          }
        },
        {
          id: 'vesta_dawn_true_color',
          name: 'Dawn FC HAMO True Color 74ppd (NASA Trek)',
          type: 'xyz',
          url: 'https://trek.nasa.gov/tiles/Vesta/EQ/Vesta_Dawn_HAMO_TrueClr_DLR_global_74ppd_IAU/1.0.0/default/default028mm/{z}/{y}/{x}.jpg',
          options: {
            attribution: 'NASA / JPL-Caltech / UCLA / MPS / DLR / IDA / Vesta Trek',
            maxNativeZoom: 6,
            maxZoom: 12,
            tms: false
          }
        },
        {
          id: 'vesta_dawn_clrshade',
          name: 'Dawn HAMO Colorized Topography 48ppd (NASA Trek)',
          type: 'xyz',
          url: 'https://trek.nasa.gov/tiles/Vesta/EQ/Vesta_Dawn_HAMO_ClrShade_DLR_Global_48ppd/1.0.0/default/default028mm/{z}/{y}/{x}.png',
          options: {
            attribution: 'NASA / JPL-Caltech / UCLA / MPS / DLR / IDA / Vesta Trek',
            maxNativeZoom: 5,
            maxZoom: 11,
            tms: false
          }
        },
        {
          id: 'vesta_dawn_geology',
          name: 'Dawn Global Geologic Map 32ppd (NASA Trek)',
          type: 'xyz',
          url: 'https://trek.nasa.gov/tiles/Vesta/EQ/Vesta_Dawn_Geology_Global_32ppd_IAU/1.0.0/default/default028mm/{z}/{y}/{x}.png',
          options: {
            attribution: 'NASA / JPL / ASU / USGS / Vesta Trek',
            maxNativeZoom: 4,
            maxZoom: 10,
            tms: false
          }
        }
      ]
    },

    // ── Jupiter System ──────────────────────────────────────────────────────
    jupiter: {
      name: 'Jupiter',
      group: 'Jupiter System',
      center: [0, 0],
      zoom: 2,
      defaultLayer: 'jupiter_cassini',
      layers: [
        {
          id: 'jupiter_cassini',
          name: 'Jupiter Cassini ISS Global Mosaic (USGS WMS)',
          type: 'wms',
          url: 'https://planetarymaps.usgs.gov/cgi-bin/mapserv?map=/maps/jupiter/jupiter_simp_cyl.map',
          options: {
            layers: 'CASSINI',
            format: 'image/png',
            transparent: true,
            attribution: 'USGS Astrogeology / NASA / JPL / SSI Cassini'
          }
        }
      ]
    },
    io: {
      name: 'Io',
      group: 'Jupiter System',
      center: [0, 0],
      zoom: 2,
      defaultLayer: 'io_galileo_voyager_color',
      layers: [
        {
          id: 'io_galileo_voyager_color',
          name: 'Io Galileo SSI + Voyager Color Global 1km (USGS WMS)',
          type: 'wms',
          url: 'https://planetarymaps.usgs.gov/cgi-bin/mapserv?map=/maps/jupiter/io_simp_cyl.map',
          options: {
            layers: 'SSI_VGR_color',
            format: 'image/png',
            transparent: true,
            attribution: 'USGS Astrogeology / NASA / JPL Galileo & Voyager'
          }
        },
        {
          id: 'io_ssi_color',
          name: 'Io Galileo SSI Color Mosaic (USGS WMS)',
          type: 'wms',
          url: 'https://planetarymaps.usgs.gov/cgi-bin/mapserv?map=/maps/jupiter/io_simp_cyl.map',
          options: {
            layers: 'SSI_color',
            format: 'image/png',
            transparent: true,
            attribution: 'USGS Astrogeology / NASA / JPL Galileo'
          }
        },
        {
          id: 'io_galileo_voyager_bw',
          name: 'Io Merged Grayscale Global Mosaic (USGS WMS)',
          type: 'wms',
          url: 'https://planetarymaps.usgs.gov/cgi-bin/mapserv?map=/maps/jupiter/io_simp_cyl.map',
          options: {
            layers: 'SSI_VGR_bw',
            format: 'image/png',
            transparent: true,
            attribution: 'USGS Astrogeology / NASA / JPL Galileo & Voyager'
          }
        }
      ]
    },
    europa: {
      name: 'Europa',
      group: 'Jupiter System',
      center: [0, 0],
      zoom: 2,
      defaultLayer: 'europa_galileo_voyager',
      layers: [
        {
          id: 'europa_galileo_voyager',
          name: 'Europa Global Mosaic (USGS WMS)',
          type: 'wms',
          url: 'https://planetarymaps.usgs.gov/cgi-bin/mapserv?map=/maps/jupiter/europa_simp_cyl.map',
          options: {
            layers: 'GALILEO_VOYAGER',
            format: 'image/png',
            transparent: true,
            attribution: 'USGS Astrogeology / NASA / JPL',
            maxNativeZoom: 5
          }
        },
        {
          id: 'europa_trek_global_color',
          name: 'Europa Global Color Blend 270m (NASA Trek)',
          type: 'xyz',
          url: 'https://trek.nasa.gov/tiles/Europa/EQ/20150218_europa_global_map_20000x10000/1.0.0//default/default028mm/{z}/{y}/{x}.png',
          options: {
            attribution: 'NASA / JPL / DLR / Europa Trek',
            maxNativeZoom: 5,
            maxZoom: 5,
            tms: false
          }
        },
        {
          id: 'europa_conamara_chaos',
          name: 'Conamara Chaos 9m/px (Galileo SSI)',
          type: 'xyz',
          url: 'https://trek.nasa.gov/tiles/Europa/EQ/12ESCHAOS_01_GalileoSSI_Equi/1.0.0//default/default028mm/{z}/{y}/{x}.png',
          options: {
            attribution: 'NASA / JPL / Galileo SSI / Europa Trek',
            maxZoom: 9,
            tms: false
          }
        },
        {
          id: 'europa_pwyll_crater',
          name: 'Pwyll Crater 55m/px (Galileo SSI)',
          type: 'xyz',
          url: 'https://trek.nasa.gov/tiles/Europa/EQ/GLL_SSI_Mosaic_55mpp_Pwyll/1.0.0//default/default028mm/{z}/{y}/{x}.png',
          options: {
            attribution: 'NASA / JPL / Galileo SSI / Europa Trek',
            maxZoom: 9,
            tms: false
          }
        },
        {
          id: 'europa_tyre_basin',
          name: 'Tyre Multi-Ring Structure 35m/px (Galileo SSI)',
          type: 'xyz',
          url: 'https://trek.nasa.gov/tiles/Europa/EQ/GLL_SSI_Mosaic_35mpp_Tyre/1.0.0//default/default028mm/{z}/{y}/{x}.png',
          options: {
            attribution: 'NASA / JPL / Galileo SSI / Europa Trek',
            maxZoom: 9,
            tms: false
          }
        }
      ]
    },
    ganymede: {
      name: 'Ganymede',
      group: 'Jupiter System',
      center: [0, 0],
      zoom: 2,
      defaultLayer: 'ganymede_galileo_voyager',
      layers: [
        {
          id: 'ganymede_galileo_voyager',
          name: 'Ganymede Galileo + Voyager Global Mosaic (USGS WMS)',
          type: 'wms',
          url: 'https://planetarymaps.usgs.gov/cgi-bin/mapserv?map=/maps/jupiter/ganymede_simp_cyl.map',
          options: {
            layers: 'GALILEO_VOYAGER',
            format: 'image/png',
            transparent: true,
            attribution: 'USGS Astrogeology / NASA / JPL Galileo & Voyager'
          }
        }
      ]
    },
    callisto: {
      name: 'Callisto',
      group: 'Jupiter System',
      center: [0, 0],
      zoom: 2,
      defaultLayer: 'callisto_galileo_voyager',
      layers: [
        {
          id: 'callisto_galileo_voyager',
          name: 'Callisto Galileo + Voyager Global Mosaic (USGS WMS)',
          type: 'wms',
          url: 'https://planetarymaps.usgs.gov/cgi-bin/mapserv?map=/maps/jupiter/callisto_simp_cyl.map',
          options: {
            layers: 'GALILEO_VOYAGER',
            format: 'image/png',
            transparent: true,
            attribution: 'USGS Astrogeology / NASA / JPL Galileo & Voyager'
          }
        }
      ]
    },

    // ── Saturn System ───────────────────────────────────────────────────────
    saturn: {
      name: 'Saturn',
      group: 'Saturn System',
      center: [0, 0],
      zoom: 2,
      defaultLayer: 'saturn_cassini',
      layers: [
        {
          id: 'saturn_cassini',
          name: 'Saturn Cassini ISS Global Mosaic (USGS WMS)',
          type: 'wms',
          url: 'https://planetarymaps.usgs.gov/cgi-bin/mapserv?map=/maps/saturn/saturn_simp_cyl.map',
          options: {
            layers: 'CASSINI',
            format: 'image/png',
            transparent: true,
            attribution: 'USGS Astrogeology / NASA / JPL / SSI Cassini'
          }
        }
      ]
    },
    mimas: {
      name: 'Mimas',
      group: 'Saturn System',
      center: [0, 0],
      zoom: 2,
      defaultLayer: 'mimas_cassini',
      layers: [
        {
          id: 'mimas_cassini',
          name: 'Mimas Cassini ISS Global Mosaic (USGS WMS)',
          type: 'wms',
          url: 'https://planetarymaps.usgs.gov/cgi-bin/mapserv?map=/maps/saturn/mimas_simp_cyl.map',
          options: {
            layers: 'CASSINI_MIMAS_MOSAIC',
            format: 'image/png',
            transparent: true,
            attribution: 'USGS Astrogeology / NASA / JPL / SSI Cassini'
          }
        },
        {
          id: 'mimas_cassini_voyager',
          name: 'Mimas Cassini & Voyager Global Mosaic (USGS WMS)',
          type: 'wms',
          url: 'https://planetarymaps.usgs.gov/cgi-bin/mapserv?map=/maps/saturn/mimas_simp_cyl.map',
          options: {
            layers: 'CASSINI_VOYAGER',
            format: 'image/png',
            transparent: true,
            attribution: 'USGS Astrogeology / NASA / JPL Cassini & Voyager'
          }
        }
      ]
    },
    enceladus: {
      name: 'Enceladus',
      group: 'Saturn System',
      center: [0, 0],
      zoom: 2,
      defaultLayer: 'enceladus_cassini_100m',
      layers: [
        {
          id: 'enceladus_cassini_100m',
          name: 'Enceladus Cassini ISS 100m Global Mosaic (USGS WMS)',
          type: 'wms',
          url: 'https://planetarymaps.usgs.gov/cgi-bin/mapserv?map=/maps/saturn/enceladus_simp_cyl.map',
          options: {
            layers: 'CASSINI_100m',
            format: 'image/png',
            transparent: true,
            attribution: 'USGS Astrogeology / NASA / JPL / SSI Cassini'
          }
        },
        {
          id: 'enceladus_cassini_global',
          name: 'Enceladus Cassini Global Mosaic (USGS WMS)',
          type: 'wms',
          url: 'https://planetarymaps.usgs.gov/cgi-bin/mapserv?map=/maps/saturn/enceladus_simp_cyl.map',
          options: {
            layers: 'CASSINI',
            format: 'image/png',
            transparent: true,
            attribution: 'USGS Astrogeology / NASA / JPL / SSI Cassini'
          }
        }
      ]
    },
    tethys: {
      name: 'Tethys',
      group: 'Saturn System',
      center: [0, 0],
      zoom: 2,
      defaultLayer: 'tethys_cassini',
      layers: [
        {
          id: 'tethys_cassini',
          name: 'Tethys Cassini Global Mosaic (USGS WMS)',
          type: 'wms',
          url: 'https://planetarymaps.usgs.gov/cgi-bin/mapserv?map=/maps/saturn/tethys_simp_cyl.map',
          options: {
            layers: 'CASSINI',
            format: 'image/png',
            transparent: true,
            attribution: 'USGS Astrogeology / NASA / JPL / SSI Cassini'
          }
        }
      ]
    },
    dione: {
      name: 'Dione',
      group: 'Saturn System',
      center: [0, 0],
      zoom: 2,
      defaultLayer: 'dione_cassini_voyager',
      layers: [
        {
          id: 'dione_cassini_voyager',
          name: 'Dione Cassini & Voyager Global Mosaic (USGS WMS)',
          type: 'wms',
          url: 'https://planetarymaps.usgs.gov/cgi-bin/mapserv?map=/maps/saturn/dione_simp_cyl.map',
          options: {
            layers: 'CASSINI_VOYAGER',
            format: 'image/png',
            transparent: true,
            attribution: 'USGS Astrogeology / NASA / JPL Cassini & Voyager'
          }
        }
      ]
    },
    rhea: {
      name: 'Rhea',
      group: 'Saturn System',
      center: [0, 0],
      zoom: 2,
      defaultLayer: 'rhea_cassini_voyager',
      layers: [
        {
          id: 'rhea_cassini_voyager',
          name: 'Rhea Cassini & Voyager Global Mosaic (USGS WMS)',
          type: 'wms',
          url: 'https://planetarymaps.usgs.gov/cgi-bin/mapserv?map=/maps/saturn/rhea_simp_cyl.map',
          options: {
            layers: 'CASSINI_VOYAGER',
            format: 'image/png',
            transparent: true,
            attribution: 'USGS Astrogeology / NASA / JPL Cassini & Voyager'
          }
        }
      ]
    },
    titan: {
      name: 'Titan',
      group: 'Saturn System',
      center: [-10.25, 167.68],
      zoom: 2,
      defaultLayer: 'titan_vims_color_ratio',
      layers: [
        {
          id: 'titan_cassini_iss',
          name: 'Titan Cassini ISS Global Mosaic (USGS WMS)',
          type: 'wms',
          url: 'https://planetarymaps.usgs.gov/cgi-bin/mapserv?map=/maps/saturn/titan_simp_cyl.map',
          options: {
            layers: 'CASSINI',
            format: 'image/png',
            transparent: true,
            attribution: 'USGS Astrogeology / NASA / JPL / SSI Cassini'
          }
        },
        {
          id: 'titan_cassini_hisar',
          name: 'Titan Cassini HiSAR Radar Global Mosaic (USGS WMS)',
          type: 'wms',
          url: 'https://planetarymaps.usgs.gov/cgi-bin/mapserv?map=/maps/saturn/titan_simp_cyl.map',
          options: {
            layers: 'Titan_HiSAR_Mosaic',
            format: 'image/png',
            transparent: true,
            attribution: 'USGS Astrogeology / NASA / JPL Cassini RADAR'
          }
        },
        {
          id: 'titan_vims_color_ratio',
          name: 'Titan Cassini VIMS Global Color Ratio (NASA Trek)',
          type: 'xyz',
          url: 'https://trek.nasa.gov/tiles/Titan/EQ/Titan_global_32ppd_ColorRatio_v2/1.0.0/default/default028mm/{z}/{y}/{x}.png',
          options: {
            attribution: 'NASA / JPL / Univ. Arizona / Nantes / Titan Trek',
            maxNativeZoom: 4,
            maxZoom: 10,
            tms: false
          }
        }
      ]
    },
    iapetus: {
      name: 'Iapetus',
      group: 'Saturn System',
      center: [0, 0],
      zoom: 2,
      defaultLayer: 'iapetus_cassini_voyager',
      layers: [
        {
          id: 'iapetus_cassini_voyager',
          name: 'Iapetus Cassini & Voyager Global Mosaic (USGS WMS)',
          type: 'wms',
          url: 'https://planetarymaps.usgs.gov/cgi-bin/mapserv?map=/maps/saturn/iapetus_simp_cyl.map',
          options: {
            layers: 'CASSINI_VOYAGER',
            format: 'image/png',
            transparent: true,
            attribution: 'USGS Astrogeology / NASA / JPL Cassini & Voyager'
          }
        }
      ]
    },
    phoebe: {
      name: 'Phoebe',
      group: 'Saturn System',
      center: [0, 0],
      zoom: 2,
      defaultLayer: 'phoebe_cassini',
      layers: [
        {
          id: 'phoebe_cassini',
          name: 'Phoebe Cassini ISS Global Mosaic (USGS WMS)',
          type: 'wms',
          url: 'https://planetarymaps.usgs.gov/cgi-bin/mapserv?map=/maps/saturn/phoebe_simp_cyl.map',
          options: {
            layers: 'CASSINI',
            format: 'image/png',
            transparent: true,
            attribution: 'USGS Astrogeology / NASA / JPL / SSI Cassini'
          }
        }
      ]
    },

    // ── Neptune System ──────────────────────────────────────────────────────
    triton: {
      name: 'Triton',
      group: 'Neptune System',
      center: [0, 0],
      zoom: 2,
      defaultLayer: 'triton_voyager2',
      layers: [
        {
          id: 'triton_voyager2',
          name: 'Triton Voyager 2 Global Color Mosaic (USGS WMS)',
          type: 'wms',
          url: 'https://planetarymaps.usgs.gov/cgi-bin/mapserv?map=/maps/neptune/triton_simp_cyl.map',
          options: {
            layers: 'TRITON_VOYAGER2',
            format: 'image/png',
            transparent: true,
            attribution: 'USGS Astrogeology / NASA / JPL Voyager 2'
          }
        }
      ]
    },

    // ── Pluto System ────────────────────────────────────────────────────────
    pluto: {
      name: 'Pluto',
      group: 'Pluto System',
      center: [18, 178],
      zoom: 2,
      defaultLayer: 'pluto_newhorizons',
      layers: [
        {
          id: 'pluto_newhorizons',
          name: 'Pluto New Horizons Global Mosaic 300m (USGS WMS)',
          type: 'wms',
          url: 'https://planetarymaps.usgs.gov/cgi-bin/mapserv?map=/maps/pluto/pluto_simp_cyl.map',
          options: {
            layers: 'NEWHORIZONS_PLUTO_MOSAIC',
            format: 'image/png',
            transparent: true,
            attribution: 'USGS Astrogeology / NASA / JHU APL / SwRI New Horizons'
          }
        },
        {
          id: 'pluto_newhorizons_clrshade',
          name: 'Pluto New Horizons Colorized Shaded Relief (USGS WMS)',
          type: 'wms',
          url: 'https://planetarymaps.usgs.gov/cgi-bin/mapserv?map=/maps/pluto/pluto_simp_cyl.map',
          options: {
            layers: 'NEWHORIZONS_PLUTO_ClrSHADE',
            format: 'image/png',
            transparent: true,
            attribution: 'USGS Astrogeology / NASA / JHU APL / SwRI / LPI'
          }
        }
      ]
    },
    charon: {
      name: 'Charon',
      group: 'Pluto System',
      center: [0, 0],
      zoom: 2,
      defaultLayer: 'charon_newhorizons',
      layers: [
        {
          id: 'charon_newhorizons',
          name: 'Charon New Horizons Global Mosaic 300m (USGS WMS)',
          type: 'wms',
          url: 'https://planetarymaps.usgs.gov/cgi-bin/mapserv?map=/maps/pluto/charon_simp_cyl.map',
          options: {
            layers: 'NEWHORIZONS_CHARON_MOSAIC',
            format: 'image/png',
            transparent: true,
            attribution: 'USGS Astrogeology / NASA / JHU APL / SwRI New Horizons'
          }
        },
        {
          id: 'charon_newhorizons_clrshade',
          name: 'Charon New Horizons Colorized Shaded Relief (USGS WMS)',
          type: 'wms',
          url: 'https://planetarymaps.usgs.gov/cgi-bin/mapserv?map=/maps/pluto/charon_simp_cyl.map',
          options: {
            layers: 'NEWHORIZONS_CHARON_ClrSHADE',
            format: 'image/png',
            transparent: true,
            attribution: 'USGS Astrogeology / NASA / JHU APL / SwRI / LPI'
          }
        }
      ]
    }
  },

  // Curated mosaic catalog per body
  mosaics: {
    mars: [
      {
        id: 'mars_mdim21_mosaic',
        name: 'MDIM 2.1 Global Mosaic',
        description: 'Viking MDIM 2.1 global mosaic (256 px/deg)',
        type: 'wms',
        url: 'https://planetarymaps.usgs.gov/cgi-bin/mapserv?map=/maps/mars/mars_simp_cyl.map',
        options: {
          layers: 'MDIM21',
          format: 'image/png',
          transparent: true,
          attribution: 'USGS Astrogeology'
        },
        thumbnail: THUMB_1PX
      },
      {
        id: 'mars_themis_day_mosaic',
        name: 'THEMIS IR Day 100m',
        description: 'Mars Odyssey THEMIS IR Day global mosaic (100m)',
        type: 'wms',
        url: 'https://planetarymaps.usgs.gov/cgi-bin/mapserv?map=/maps/mars/mars_simp_cyl.map',
        options: {
          layers: 'THEMIS',
          format: 'image/png',
          transparent: true,
          attribution: 'USGS Astrogeology'
        },
        thumbnail: THUMB_1PX
      },
      {
        id: 'mars_mola_hillshade',
        name: 'MOLA Hillshade',
        description: 'MOLA shaded relief (global)',
        type: 'wms',
        url: 'https://planetarymaps.usgs.gov/cgi-bin/mapserv?map=/maps/mars/mars_simp_cyl.map',
        options: {
          layers: 'MOLA_bw',
          format: 'image/png',
          transparent: true,
          attribution: 'USGS Astrogeology'
        },
        thumbnail: THUMB_1PX
      }
    ],
    moon: [
      {
        id: 'moon_lro_wac_mosaic',
        name: 'LRO WAC Global Mosaic (100m)',
        description: 'Lunar Reconnaissance Orbiter Camera (LROC) Wide Angle Camera global monochrome mosaic at 303 pixels/degree (~100m/pixel).',
        type: 'xyz',
        url: 'https://trek.nasa.gov/tiles/Moon/EQ/LRO_WAC_Mosaic_Global_303ppd_v02/1.0.0/default/default028mm/{z}/{y}/{x}.jpg',
        options: {
          attribution: 'NASA / GSFC / ASU / Moon Trek',
          maxNativeZoom: 8,
          maxZoom: 12
        },
        thumbnail: THUMB_1PX
      },
      {
        id: 'moon_lola_color_mosaic',
        name: 'LOLA Topographic Elevation (Color)',
        description: 'Lunar Orbiter Laser Altimeter (LOLA) global color-coded digital elevation model showing absolute surface elevations.',
        type: 'wms',
        url: 'https://planetarymaps.usgs.gov/cgi-bin/mapserv?map=/maps/earth/moon_simp_cyl.map',
        options: {
          layers: 'LOLA_color',
          format: 'image/png',
          transparent: true,
          attribution: 'USGS Astrogeology / NASA / GSFC'
        },
        thumbnail: THUMB_1PX
      },
      {
        id: 'moon_kaguya_tc_mosaic',
        name: 'Kaguya TC Orthomosaic (10m)',
        description: 'JAXA SELENE (Kaguya) Terrain Camera high-resolution 10 m/pixel seamless global orthomosaic.',
        type: 'wms',
        url: 'https://planetarymaps.usgs.gov/cgi-bin/mapserv?map=/maps/earth/moon_simp_cyl.map',
        options: {
          layers: 'KaguyaTC_Ortho',
          format: 'image/png',
          transparent: true,
          attribution: 'USGS Astrogeology / JAXA / SELENE'
        },
        thumbnail: THUMB_1PX
      }
    ],
    mercury: [
      {
        id: 'mercury_messenger_color_mosaic',
        name: 'MESSENGER MDIS Enhanced Color v3',
        description: 'Global principal-component enhanced color mosaic highlighting pyroclastic vents, hollows, and compositional plains.',
        type: 'wms',
        url: 'https://planetarymaps.usgs.gov/cgi-bin/mapserv?map=/maps/mercury/mercury_simp_cyl.map',
        options: {
          layers: 'MESSENGER_Color',
          format: 'image/png',
          transparent: true,
          attribution: 'USGS Astrogeology / NASA / JHU APL / CIW'
        },
        thumbnail: THUMB_1PX
      }
    ],
    venus: [
      {
        id: 'venus_magellan_topo_mosaic',
        name: 'Magellan Colorized Radar Altimetry',
        description: 'Global Magellan radar altimetry blended with synthetic aperture radar backscatter across Venus.',
        type: 'wms',
        url: 'https://planetarymaps.usgs.gov/cgi-bin/mapserv?map=/maps/venus/venus_simp_cyl.map',
        options: {
          layers: 'MAGELLAN_topography',
          format: 'image/png',
          transparent: true,
          attribution: 'USGS Astrogeology / NASA / JPL / MIT'
        },
        thumbnail: THUMB_1PX
      }
    ],
    ceres: [
      {
        id: 'ceres_clrshade_mosaic',
        name: 'Dawn HAMO Colorized Topography (140m)',
        description: 'DLR Dawn Framing Camera HAMO stereo digital terrain model colorized over shaded relief.',
        type: 'xyz',
        url: 'https://trek.nasa.gov/tiles/Ceres/EQ/Ceres_Dawn_FC_HAMO_ClrShade_DLR_Global_60ppd_Oct2016/1.0.0/default/default028mm/{z}/{y}/{x}.jpg',
        options: {
          attribution: 'NASA / JPL-Caltech / UCLA / MPS / DLR / IDA',
          maxNativeZoom: 5,
          maxZoom: 11
        },
        thumbnail: THUMB_1PX
      }
    ],
    vesta: [
      {
        id: 'vesta_geology_mosaic',
        name: 'Dawn Global Geologic Map (32ppd)',
        description: 'Global chronostratigraphic geologic map of asteroid 4 Vesta mapped from Dawn Framing Camera and VIR data.',
        type: 'xyz',
        url: 'https://trek.nasa.gov/tiles/Vesta/EQ/Vesta_Dawn_Geology_Global_32ppd_IAU/1.0.0/default/default028mm/{z}/{y}/{x}.png',
        options: {
          attribution: 'NASA / JPL / ASU / USGS / Vesta Trek',
          maxNativeZoom: 4,
          maxZoom: 10
        },
        thumbnail: THUMB_1PX
      }
    ],
    europa: [
      {
        id: 'europa_galileo_voyager_mosaic',
        name: 'Europa Global Mosaic (Voyager/Galileo)',
        description: 'Global controlled mosaic of Europa synthesized from Galileo SSI and Voyager 1 & 2 spacecraft data (USGS Astrogeology).',
        type: 'wms',
        url: 'https://planetarymaps.usgs.gov/cgi-bin/mapserv?map=/maps/jupiter/europa_simp_cyl.map',
        options: {
          layers: 'GALILEO_VOYAGER',
          format: 'image/png',
          transparent: true,
          attribution: 'USGS Astrogeology / NASA / JPL',
          maxNativeZoom: 5
        },
        thumbnail: THUMB_1PX
      },
      {
        id: 'europa_trek_color_mosaic',
        name: 'Europa Global Color Blend (NASA Trek 270m)',
        description: 'USGS/NASA Voyager and Galileo SSI global color blend mosaic at 270 m/px.',
        type: 'xyz',
        url: 'https://trek.nasa.gov/tiles/Europa/EQ/20150218_europa_global_map_20000x10000/1.0.0//default/default028mm/{z}/{y}/{x}.png',
        options: {
          attribution: 'NASA / JPL / DLR / Europa Trek',
          maxZoom: 5
        },
        thumbnail: THUMB_1PX
      },
      {
        id: 'europa_conamara_mosaic',
        name: 'Conamara Chaos High-Res Mosaic (9m)',
        description: 'Galileo SSI targeted flyby mosaic over Conamara Chaos fractured ice rafts at 9 m/px.',
        type: 'xyz',
        url: 'https://trek.nasa.gov/tiles/Europa/EQ/12ESCHAOS_01_GalileoSSI_Equi/1.0.0//default/default028mm/{z}/{y}/{x}.png',
        options: {
          attribution: 'NASA / JPL / Galileo SSI / Europa Trek',
          maxZoom: 9
        },
        thumbnail: THUMB_1PX
      },
      {
        id: 'europa_pwyll_mosaic',
        name: 'Pwyll Crater High-Res Mosaic (55m)',
        description: 'Galileo SSI mosaic of young impact crater Pwyll showing bright ray deposits at 55 m/px.',
        type: 'xyz',
        url: 'https://trek.nasa.gov/tiles/Europa/EQ/GLL_SSI_Mosaic_55mpp_Pwyll/1.0.0//default/default028mm/{z}/{y}/{x}.png',
        options: {
          attribution: 'NASA / JPL / Galileo SSI / Europa Trek',
          maxZoom: 9
        },
        thumbnail: THUMB_1PX
      },
      {
        id: 'europa_tyre_mosaic',
        name: 'Tyre Multi-Ring Basin High-Res (35m)',
        description: 'Galileo SSI mosaic of Tyre impact structure revealing sub-crustal impact excavation at 35 m/px.',
        type: 'xyz',
        url: 'https://trek.nasa.gov/tiles/Europa/EQ/GLL_SSI_Mosaic_35mpp_Tyre/1.0.0//default/default028mm/{z}/{y}/{x}.png',
        options: {
          attribution: 'NASA / JPL / Galileo SSI / Europa Trek',
          maxZoom: 9
        },
        thumbnail: THUMB_1PX
      }
    ],
    titan: [
      {
        id: 'titan_hisar_mosaic',
        name: 'Cassini HiSAR Global Radar Mosaic',
        description: 'Cassini synthetic aperture radar (SAR/HiSAR) global mosaic piercing Titan haze to reveal hydrocarbon seas and dune fields.',
        type: 'wms',
        url: 'https://planetarymaps.usgs.gov/cgi-bin/mapserv?map=/maps/saturn/titan_simp_cyl.map',
        options: {
          layers: 'Titan_HiSAR_Mosaic',
          format: 'image/png',
          transparent: true,
          attribution: 'USGS Astrogeology / NASA / JPL Cassini RADAR'
        },
        thumbnail: THUMB_1PX
      },
      {
        id: 'titan_vims_mosaic',
        name: 'Cassini VIMS Global Color Ratio',
        description: 'Cassini Visual and Infrared Mapping Spectrometer (VIMS) global surface compositional color ratio mosaic.',
        type: 'xyz',
        url: 'https://trek.nasa.gov/tiles/Titan/EQ/Titan_global_32ppd_ColorRatio_v2/1.0.0/default/default028mm/{z}/{y}/{x}.png',
        options: {
          attribution: 'NASA / JPL / Univ. Arizona / Nantes / Titan Trek',
          maxNativeZoom: 4,
          maxZoom: 10
        },
        thumbnail: THUMB_1PX
      }
    ],
    pluto: [
      {
        id: 'pluto_clrshade_mosaic',
        name: 'New Horizons Colorized Elevation',
        description: 'Pluto LORRI/MVIC stereo topography colorized over global shaded relief, highlighting Sputnik Planitia nitrogen-ice basin.',
        type: 'wms',
        url: 'https://planetarymaps.usgs.gov/cgi-bin/mapserv?map=/maps/pluto/pluto_simp_cyl.map',
        options: {
          layers: 'NEWHORIZONS_PLUTO_ClrSHADE',
          format: 'image/png',
          transparent: true,
          attribution: 'USGS Astrogeology / NASA / JHU APL / SwRI / LPI'
        },
        thumbnail: THUMB_1PX
      }
    ]
  }
};
