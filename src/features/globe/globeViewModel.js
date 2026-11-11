/**
 * @module globeViewModel
 * @description Pure helpers for the Cesium 3D planetary globe view.
 *
 * No Cesium import: these functions run in unit tests and on the 2D page
 * before the globe bundle is downloaded. Radii match authoritative IAU WGCCRE
 * (Archinal et al. 2018) equatorial/intermediate/polar ellipsoids in meters
 * (`[x, y, z]`), preserving Cesium built-in constants for Mars, Moon, and Earth.
 */

/** Pinned CesiumJS release. 1.133 added Ellipsoid.MARS; 1.145 is the current stable. */
export const CESIUM_VERSION = '1.145.0';

/** CDN root for Cesium.js, widgets.css, Workers, and Assets. Must end with a slash. */
export const CESIUM_CDN_BASE = `https://cdn.jsdelivr.net/npm/cesium@${CESIUM_VERSION}/Build/Cesium/`;

/**
 * Body ellipsoids in meters, `[x, y, z]` = `[sub-planetary/equatorial a, along-orbit/equatorial b, polar c]`.
 * @type {Record<string, {cesiumName: string|null, radii: [number, number, number], source: string, baseColor: string}>}
 */
export const BODY_ELLIPSOIDS = {
  mercury: {
    cesiumName: null,
    radii: [2439700.0, 2439700.0, 2439700.0],
    source: 'IAU WGCCRE Mercury sphere (2439.7 km)',
    baseColor: '#2e2b28'
  },
  venus: {
    cesiumName: null,
    radii: [6051800.0, 6051800.0, 6051800.0],
    source: 'IAU WGCCRE Venus sphere (6051.8 km)',
    baseColor: '#3d2b14'
  },
  earth: {
    cesiumName: 'WGS84',
    radii: [6378137.0, 6378137.0, 6356752.3142451793],
    source: 'WGS84',
    baseColor: '#0b1d33'
  },
  moon: {
    cesiumName: 'MOON',
    radii: [1737400.0, 1737400.0, 1737400.0],
    source: 'Cesium Ellipsoid.MOON (mean lunar radius 1737.4 km)',
    baseColor: '#2c2c2c'
  },
  mars: {
    cesiumName: 'MARS',
    radii: [3396190.0, 3396190.0, 3376200.0],
    source: 'Cesium Ellipsoid.MARS (IAU / EPSG:104905)',
    baseColor: '#3a1d14'
  },
  phobos: {
    cesiumName: null,
    radii: [12200.0, 12200.0, 9100.0],
    source: 'IAU WGCCRE Phobos oblate spheroid (12.2 / 9.1 km; triaxial 13.0 × 11.4 × 9.1 km)',
    baseColor: '#292421'
  },
  deimos: {
    cesiumName: null,
    radii: [6900.0, 6900.0, 5100.0],
    source: 'IAU WGCCRE Deimos oblate spheroid (6.9 / 5.1 km; triaxial 7.8 × 6.0 × 5.1 km)',
    baseColor: '#2b2622'
  },
  ceres: {
    cesiumName: null,
    radii: [481600.0, 481600.0, 445600.0],
    source: 'IAU WGCCRE / Dawn Ceres oblate spheroid (481.6 / 445.6 km)',
    baseColor: '#262626'
  },
  vesta: {
    cesiumName: null,
    radii: [282450.0, 282450.0, 223200.0],
    source: 'IAU WGCCRE / Dawn Vesta oblate spheroid (282.45 / 223.2 km; triaxial 286.3 × 278.6 × 223.2 km)',
    baseColor: '#2d2b29'
  },
  jupiter: {
    cesiumName: null,
    radii: [71492000.0, 71492000.0, 66854000.0],
    source: 'IAU WGCCRE Jupiter 1-bar oblate spheroid (71492 / 66854 km)',
    baseColor: '#3a2b1e'
  },
  io: {
    cesiumName: null,
    radii: [1824350.0, 1824350.0, 1815700.0],
    source: 'IAU WGCCRE Io oblate spheroid (1824.35 / 1815.7 km)',
    baseColor: '#3f3416'
  },
  europa: {
    cesiumName: null,
    radii: [1564130.0, 1564130.0, 1557400.0],
    source: 'IAU Europa radii used by JSMARS (1564.13 km equatorial, 1557.4 km polar)',
    baseColor: '#1a2430'
  },
  ganymede: {
    cesiumName: null,
    radii: [2631200.0, 2631200.0, 2631200.0],
    source: 'IAU WGCCRE Ganymede sphere (2631.2 km)',
    baseColor: '#2a2622'
  },
  callisto: {
    cesiumName: null,
    radii: [2410300.0, 2410300.0, 2410300.0],
    source: 'IAU WGCCRE Callisto sphere (2410.3 km)',
    baseColor: '#23201d'
  },
  saturn: {
    cesiumName: null,
    radii: [60268000.0, 60268000.0, 54364000.0],
    source: 'IAU WGCCRE Saturn 1-bar oblate spheroid (60268 / 54364 km)',
    baseColor: '#3a3220'
  },
  mimas: {
    cesiumName: null,
    radii: [202250.0, 202250.0, 190600.0],
    source: 'IAU WGCCRE Mimas oblate spheroid (202.25 / 190.6 km)',
    baseColor: '#2b2d30'
  },
  enceladus: {
    cesiumName: null,
    radii: [254000.0, 254000.0, 248300.0],
    source: 'IAU WGCCRE Enceladus oblate spheroid (254.0 / 248.3 km)',
    baseColor: '#28323c'
  },
  tethys: {
    cesiumName: null,
    radii: [533350.0, 533350.0, 526300.0],
    source: 'IAU WGCCRE Tethys oblate spheroid (533.35 / 526.3 km)',
    baseColor: '#2c3035'
  },
  dione: {
    cesiumName: null,
    radii: [562350.0, 562350.0, 559600.0],
    source: 'IAU WGCCRE Dione oblate spheroid (562.35 / 559.6 km)',
    baseColor: '#2a2e33'
  },
  rhea: {
    cesiumName: null,
    radii: [764050.0, 764050.0, 762400.0],
    source: 'IAU WGCCRE Rhea oblate spheroid (764.05 / 762.4 km)',
    baseColor: '#2b2d31'
  },
  titan: {
    cesiumName: null,
    radii: [2574730.0, 2574730.0, 2574730.0],
    source: 'IAU WGCCRE Titan solid-surface sphere (2574.73 km)',
    baseColor: '#3a2a14'
  },
  iapetus: {
    cesiumName: null,
    radii: [746000.0, 746000.0, 712000.0],
    source: 'IAU WGCCRE Iapetus oblate spheroid (746.0 / 712.0 km)',
    baseColor: '#26221e'
  },
  phoebe: {
    cesiumName: null,
    radii: [108950.0, 108950.0, 101800.0],
    source: 'IAU WGCCRE Phoebe oblate spheroid (108.95 / 101.8 km)',
    baseColor: '#212121'
  },
  triton: {
    cesiumName: null,
    radii: [1353400.0, 1353400.0, 1353400.0],
    source: 'IAU WGCCRE Triton sphere (1353.4 km)',
    baseColor: '#2b282c'
  },
  pluto: {
    cesiumName: null,
    radii: [1188300.0, 1188300.0, 1188300.0],
    source: 'IAU WGCCRE / New Horizons Pluto sphere (1188.3 km)',
    baseColor: '#33251d'
  },
  charon: {
    cesiumName: null,
    radii: [606000.0, 606000.0, 606000.0],
    source: 'IAU WGCCRE / New Horizons Charon sphere (606.0 km)',
    baseColor: '#292728'
  }
};

/** Leaflet-like vertical field of view used to match zoom and camera height. */
export const DEFAULT_FOV_Y = Math.PI / 3;

/** Fallback viewport height when the map container has not been measured. */
export const DEFAULT_VIEWPORT_HEIGHT_PX = 800;

/**
 * Sidebar / map tools that draw or query on the Leaflet map.
 * While the Cesium view is showing, these controls are disabled.
 * Calculators that do not need a map click (time, trajectory, radar,
 * KRC/MCD typed inputs) stay available.
 */
export const TWO_D_ONLY_TOOL_IDS = Object.freeze([
  'crater-counting',
  'profiles',
  'measurement',
  'nomenclature',
  'graticule',
  'investigate',
  'sampling',
  'stamps',
  'shapes',
  'landing-sites',
  'ground-tracks',
  'export',
  'hillshade',
  'contours',
  'geotiff',
  'band-math',
  'projection',
  'overlays'
]);

/**
 * Minimum nadir camera altitude in meters for a body of equatorial radius `radiusMeters`.
 * Preserves 1000 m for major worlds (`R >= 50 km`) while scaling down smoothly for
 * small moons and asteroids (e.g. Phobos 260 m, Deimos 156 m).
 * @param {number} radiusMeters
 * @returns {number}
 */
export function minCameraHeightMeters(radiusMeters) {
  const r = Number.isFinite(radiusMeters) && radiusMeters > 0 ? radiusMeters : 3396190;
  return Math.min(1000, Math.max(10, r * 0.02));
}

/**
 * Equatorial radius in meters for a canonical body key.
 * @param {string} bodyKey
 * @returns {number}
 */
export function equatorialRadiusMeters(bodyKey) {
  const key = String(bodyKey || 'mars').toLowerCase();
  const spec = BODY_ELLIPSOIDS[key] || BODY_ELLIPSOIDS.mars;
  return spec.radii[0];
}

/**
 * Camera height above the ellipsoid that frames a similar ground extent to a
 * Leaflet EPSG:4326 view.
 *
 * Leaflet maps 180° of latitude to 256 * 2^zoom pixels. The height is the
 * nadir distance whose vertical FOV covers that many pixels.
 *
 * @param {number} zoom
 * @param {{radiusMeters?: number, body?: string, viewportHeightPx?: number, fovyRadians?: number}} [options]
 * @returns {number} Height in meters
 */
export function zoomToCameraHeightMeters(zoom, options = {}) {
  const radius = options.radiusMeters ?? equatorialRadiusMeters(options.body || 'mars');
  const viewportHeightPx = options.viewportHeightPx > 0
    ? options.viewportHeightPx
    : DEFAULT_VIEWPORT_HEIGHT_PX;
  const fovy = options.fovyRadians > 0 ? options.fovyRadians : DEFAULT_FOV_Y;
  const z = Number.isFinite(zoom) ? zoom : 2;
  const latPixels = 256 * Math.pow(2, z);
  const metersPerPixel = (Math.PI * radius) / latPixels;
  const visibleMeters = metersPerPixel * viewportHeightPx;
  const height = (visibleMeters / 2) / Math.tan(fovy / 2);
  const minHeight = minCameraHeightMeters(radius);
  return Math.min(Math.max(height, minHeight), radius * 20);
}

/**
 * Invert {@link zoomToCameraHeightMeters}.
 * @param {number} heightMeters
 * @param {{radiusMeters?: number, body?: string, viewportHeightPx?: number, fovyRadians?: number}} [options]
 * @returns {number} Leaflet zoom, clamped to 0–18
 */
export function cameraHeightToZoom(heightMeters, options = {}) {
  const radius = options.radiusMeters ?? equatorialRadiusMeters(options.body || 'mars');
  const viewportHeightPx = options.viewportHeightPx > 0
    ? options.viewportHeightPx
    : DEFAULT_VIEWPORT_HEIGHT_PX;
  const fovy = options.fovyRadians > 0 ? options.fovyRadians : DEFAULT_FOV_Y;
  const minHeight = minCameraHeightMeters(radius);
  const height = Math.max(minHeight, Number.isFinite(heightMeters) ? heightMeters : radius);
  const visibleMeters = 2 * height * Math.tan(fovy / 2);
  const metersPerPixel = visibleMeters / Math.max(1, viewportHeightPx);
  const latPixels = (Math.PI * radius) / Math.max(metersPerPixel, 1e-6);
  const zoom = Math.log2(latPixels / 256);
  if (!Number.isFinite(zoom)) return 2;
  return Math.max(0, Math.min(18, zoom));
}

/**
 * Tile grid for a configured layer.
 * WMS endpoints in this app are simple-cylindrical / EPSG:4326 (`geographic`).
 * NASA Solar System Treks `.../EQ/...` WMTS pyramids also use a 2x1 root
 * `EPSG:4326` GeographicTilingScheme (`default028mm`), whereas standard slippy
 * XYZ maps (OpenPlanetary, NASA GIBS EPSG:3857) use Web Mercator.
 * @param {{type?: string, url?: string, scheme?: string, options?: object}} layerConfig
 * @returns {'geographic'|'webmercator'|null}
 */
export function tilingSchemeForLayer(layerConfig) {
  if (!layerConfig) return null;
  if (layerConfig.type === 'wms') return 'geographic';
  if (layerConfig.type === 'xyz') {
    const explicit = layerConfig.scheme || layerConfig.options?.scheme;
    if (explicit === 'geographic' || explicit === 'webmercator') return explicit;
    if (/(?:trek\.nasa\.gov\/tiles|trektiles\.jpl\.nasa\.gov\/trektiles)\/.*\/EQ\//i.test(String(layerConfig.url || ''))) {
      return 'geographic';
    }
    return 'webmercator';
  }
  return null;
}

/**
 * Serializable imagery description. CesiumGlobe turns this into a provider.
 * @param {object} layerConfig
 * @returns {object|null}
 */
export function describeImageryLayer(layerConfig) {
  const scheme = tilingSchemeForLayer(layerConfig);
  if (!scheme || !layerConfig.url) return null;
  const options = layerConfig.options || {};
  if (layerConfig.type === 'wms' && !options.layers) return null;
  const maxZoom = Number(options.maxNativeZoom ?? options.maxZoom);
  return {
    id: layerConfig.id,
    name: layerConfig.name || layerConfig.id,
    type: layerConfig.type,
    url: layerConfig.url,
    scheme,
    layers: options.layers || '',
    format: options.format || 'image/png',
    transparent: options.transparent !== false,
    attribution: options.attribution || '',
    maxZoom: Number.isFinite(maxZoom) ? maxZoom : undefined,
    tms: options.tms === true
  };
}

/**
 * Merge layer catalogs without duplicating ids. Earlier entries win.
 * @param {Array<object>} availableLayers
 * @param {Array<object>} [bodyLayers]
 * @returns {Array<object>}
 */
export function collectLayerConfigs(availableLayers, bodyLayers) {
  const merged = [];
  const seen = new Set();
  [...(availableLayers || []), ...(bodyLayers || [])].forEach((layer) => {
    if (!layer || !layer.id || seen.has(layer.id)) return;
    seen.add(layer.id);
    merged.push(layer);
  });
  return merged;
}

/**
 * Visible imagery stack, bottom to top, matching Leaflet draw order.
 * @param {Array<{id: string, opacity?: number, visible?: boolean}>} activeLayers
 * @param {Array<object>} layerConfigs
 * @returns {Array<object>}
 */
export function describeActiveImagery(activeLayers, layerConfigs) {
  const byId = new Map();
  collectLayerConfigs(layerConfigs).forEach((cfg) => byId.set(cfg.id, cfg));
  const out = [];
  (activeLayers || []).forEach((state) => {
    if (!state || state.visible === false) return;
    const desc = describeImageryLayer(byId.get(state.id));
    if (!desc) return;
    const opacity = typeof state.opacity === 'number' && Number.isFinite(state.opacity)
      ? Math.max(0, Math.min(1, state.opacity))
      : 1;
    out.push({ ...desc, opacity });
  });
  return out;
}

/**
 * Honest status line for the globe chrome. Does not claim elevation terrain,
 * and explicitly clarifies smooth-ellipsoid terminator shading when enabled.
 * @param {string} bodyKey
 * @param {Array<{name?: string, attribution?: string}>} descriptors
 * @param {{enabled?: boolean, mode?: 'ephemeris'|'local'}} [lightingState]
 * @returns {string}
 */
export function formatGlobeProvenance(bodyKey, descriptors, lightingState) {
  const key = String(bodyKey || 'mars').toLowerCase();
  const spec = BODY_ELLIPSOIDS[key] || BODY_ELLIPSOIDS.mars;
  const names = [];
  (descriptors || []).forEach((desc) => {
    const label = desc.attribution || desc.name;
    if (label && !names.includes(label)) names.push(label);
  });
  const imagery = names.length ? names.join(', ') : 'no visible imagery layer';
  let text = `Imagery: ${imagery}. Globe: ${spec.source}. Elevation terrain is not loaded.`;
  if (lightingState && lightingState.enabled) {
    text += ' Terminator shades day/night on the smooth ellipsoid only (no 3D DEM shadows; local crater relief in tiles is fixed spacecraft illumination).';
  }
  return text;
}

/**
 * True when a URL is the Cesium CDN bundle, a worker, or a Cesium asset.
 * The service worker must not precache or runtime-cache these.
 * @param {string} url
 * @returns {boolean}
 */
export function isCesiumCdnUrl(url) {
  const value = String(url || '');
  return /cesium@/i.test(value) || /\/cesium\//i.test(value) || /cesium\.com/i.test(value);
}
