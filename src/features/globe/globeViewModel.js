/**
 * @module globeViewModel
 * @description Pure helpers for the Cesium globe view.
 *
 * No Cesium import: these functions run in unit tests and on the 2D page
 * before the globe bundle is downloaded. Radii for Mars and the Moon match
 * the constants CesiumJS exposes as Ellipsoid.MARS (added in 1.133) and
 * Ellipsoid.MOON. Europa is not built into Cesium; it uses the same IAU
 * equatorial/polar radii as src/util/geo.js BODIES.europa.
 */

/** Pinned CesiumJS release. 1.133 added Ellipsoid.MARS; 1.145 is the current stable. */
export const CESIUM_VERSION = '1.145.0';

/** CDN root for Cesium.js, widgets.css, Workers, and Assets. Must end with a slash. */
export const CESIUM_CDN_BASE = `https://cdn.jsdelivr.net/npm/cesium@${CESIUM_VERSION}/Build/Cesium/`;

/**
 * Body ellipsoids in meters, [x, y, z] = [equatorial, equatorial, polar].
 * @type {Record<string, {cesiumName: string|null, radii: [number, number, number], source: string, baseColor: string}>}
 */
export const BODY_ELLIPSOIDS = {
  mars: {
    cesiumName: 'MARS',
    radii: [3396190.0, 3396190.0, 3376200.0],
    source: 'Cesium Ellipsoid.MARS (IAU / EPSG:104905)',
    baseColor: '#3a1d14'
  },
  moon: {
    cesiumName: 'MOON',
    radii: [1737400.0, 1737400.0, 1737400.0],
    source: 'Cesium Ellipsoid.MOON (mean lunar radius 1737.4 km)',
    baseColor: '#2c2c2c'
  },
  earth: {
    cesiumName: 'WGS84',
    radii: [6378137.0, 6378137.0, 6356752.3142451793],
    source: 'WGS84',
    baseColor: '#0b1d33'
  },
  europa: {
    cesiumName: null,
    radii: [1564130.0, 1564130.0, 1557400.0],
    source: 'IAU Europa radii used by JSMARS (1564.13 km equatorial, 1557.4 km polar)',
    baseColor: '#1a2430'
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
 * Equatorial radius in meters for a canonical body key.
 * @param {string} bodyKey
 * @returns {number}
 */
export function equatorialRadiusMeters(bodyKey) {
  const spec = BODY_ELLIPSOIDS[bodyKey] || BODY_ELLIPSOIDS.mars;
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
  return Math.min(Math.max(height, 1000), radius * 20);
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
  const height = Math.max(1000, Number.isFinite(heightMeters) ? heightMeters : radius);
  const visibleMeters = 2 * height * Math.tan(fovy / 2);
  const metersPerPixel = visibleMeters / Math.max(1, viewportHeightPx);
  const latPixels = (Math.PI * radius) / Math.max(metersPerPixel, 1e-6);
  const zoom = Math.log2(latPixels / 256);
  if (!Number.isFinite(zoom)) return 2;
  return Math.max(0, Math.min(18, zoom));
}

/**
 * Tile grid for a configured layer.
 * WMS endpoints in this app are simple-cylindrical / EPSG:4326.
 * XYZ slippy maps (OpenPlanetary, NASA Trek) publish one world tile at z=0,
 * which is a Web Mercator pyramid.
 * @param {{type?: string}} layerConfig
 * @returns {'geographic'|'webmercator'|null}
 */
export function tilingSchemeForLayer(layerConfig) {
  if (!layerConfig) return null;
  if (layerConfig.type === 'wms') return 'geographic';
  if (layerConfig.type === 'xyz') return 'webmercator';
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
  const maxZoom = Number(options.maxZoom);
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
 * Honest status line for the globe chrome. Does not claim elevation terrain.
 * @param {string} bodyKey
 * @param {Array<{name?: string, attribution?: string}>} descriptors
 * @returns {string}
 */
export function formatGlobeProvenance(bodyKey, descriptors) {
  const spec = BODY_ELLIPSOIDS[bodyKey] || BODY_ELLIPSOIDS.mars;
  const names = [];
  (descriptors || []).forEach((desc) => {
    const label = desc.attribution || desc.name;
    if (label && !names.includes(label)) names.push(label);
  });
  const imagery = names.length ? names.join(', ') : 'no visible imagery layer';
  return `Imagery: ${imagery}. Globe: ${spec.source}. Elevation terrain is not loaded.`;
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
