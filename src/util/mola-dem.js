/**
 * @module mola-dem
 * @description MOLA DEM elevation sampling via Cloud-Optimized GeoTIFF.
 * Lazy-loads the GeoTIFF library from CDN, then reads elevation values
 * from the USGS 128ppd MOLA dataset using windowed raster reads.
 *
 * Both the script loader and the MOLA context are cached as promises.
 * If either rejects, the cached promise is reset to null so that
 * subsequent calls can retry instead of permanently failing.
 */

/** @type {string} URL to the MOLA 128ppd Cloud-Optimized GeoTIFF */
const MOLA_TIFF_URL = 'https://asc-pds-services.s3.us-west-2.amazonaws.com/mosaic/mola128_88Nto88S_Simp_clon0.tif';

/**
 * Cached promise for the GeoTIFF script load.
 * Reset to null on failure so retries work.
 * @type {Promise<void>|null}
 */
let geoTiffScriptPromise = null;

/**
 * Cached promise for the MOLA context (image metadata).
 * Reset to null on failure so retries work.
 * @type {Promise<object>|null}
 */
let molaContextPromise = null;

/**
 * Lazy-load the GeoTIFF library from CDN.
 * Caches the loading promise; resets it on failure so retries work.
 * @returns {Promise<void>} Resolves when window.GeoTIFF is available
 */
function loadGeoTiffScript() {
  if (window.GeoTIFF) return Promise.resolve();
  if (geoTiffScriptPromise) return geoTiffScriptPromise;

  geoTiffScriptPromise = new Promise((resolve, reject) => {
    const script = document.createElement('script');
    script.src = 'https://cdn.jsdelivr.net/npm/geotiff@2.1.3/dist-browser/geotiff.min.js';
    script.async = true;
    script.crossOrigin = 'anonymous';
    script.onload = () => resolve();
    script.onerror = () => reject(new Error('Failed to load GeoTIFF library'));
    document.head.appendChild(script);
  });

  // Reset on rejection so the next call can retry
  geoTiffScriptPromise.catch(() => {
    geoTiffScriptPromise = null;
  });

  return geoTiffScriptPromise;
}

/**
 * Open the MOLA GeoTIFF and extract image metadata.
 * Caches the context promise; resets it on failure so retries work.
 * @returns {Promise<{image: object, bbox: number[], width: number, height: number, resX: number, resY: number, noData: number|null, origin: number[]}>}
 */
async function getMolaContext() {
  if (molaContextPromise) return molaContextPromise;

  molaContextPromise = (async () => {
    await loadGeoTiffScript();
    if (!window.GeoTIFF || !window.GeoTIFF.fromUrl) {
      throw new Error('GeoTIFF library not available');
    }

    const tiff = await window.GeoTIFF.fromUrl(MOLA_TIFF_URL, { allowFullFile: false });
    const image = await tiff.getImage();
    const bbox = image.getBoundingBox(); // [minLon, minLat, maxLon, maxLat]
    const width = image.getWidth();
    const height = image.getHeight();
    const resolution = image.getResolution();
    const resX = resolution?.[0] || ((bbox[2] - bbox[0]) / width);
    const resY = Math.abs(resolution?.[1] || ((bbox[3] - bbox[1]) / height));
    const origin = image.getOrigin ? image.getOrigin() : [bbox[0], bbox[3]];
    const noDataRaw = image.getGDALNoData();
    const noData = noDataRaw !== null && noDataRaw !== undefined ? Number(noDataRaw) : null;

    return { image, bbox, width, height, resX, resY, noData, origin };
  })();

  // Reset on rejection so the next call can retry
  molaContextPromise.catch(() => {
    molaContextPromise = null;
  });

  return molaContextPromise;
}

/**
 * Sample elevation values from the MOLA DEM for an array of points.
 * Uses windowed raster reads to minimize data transfer.
 *
 * @param {Array<{lat: number, lng?: number, lon?: number}>} points
 *   Array of point objects with lat and lng (or lon) properties.
 * @returns {Promise<Array<number|null>>} Elevation in meters for each point,
 *   or null if the point is outside the dataset or has noData.
 */
async function sampleElevations(points) {
  const ctx = await getMolaContext();
  const { image, bbox, width, height, resX, resY, noData, origin } = ctx;
  const [minLon, minLat, maxLon, maxLat] = bbox;
  const originLon = origin?.[0] ?? minLon;
  const originLat = origin?.[1] ?? maxLat;
  const lonSpan = resX * width;
  const latSpan = resY * height;

  /**
   * Normalize a longitude value into the dataset's coordinate range.
   * @param {number} lon - Input longitude
   * @returns {number} Adjusted longitude within dataset bounds
   */
  const normalizeLon = (lon) => {
    let adjusted = lon;
    // Pull into the nearest 360-degree wrap relative to origin.
    while (adjusted < originLon) adjusted += 360;
    while (adjusted > originLon + lonSpan) adjusted -= 360;
    // If still outside, clamp into the dataset span.
    if (adjusted < minLon) adjusted = minLon;
    if (adjusted > maxLon) adjusted = maxLon - resX * 0.5;
    return adjusted;
  };

  const coords = points.map((p) => {
    const lonWrapped = normalizeLon(p.lng ?? p.lon);
    const clampedLon = Math.min(originLon + lonSpan - resX * 0.5, Math.max(originLon, lonWrapped));
    const clampedLat = Math.min(originLat, Math.max(originLat - latSpan, p.lat));
    const x = Math.floor((clampedLon - originLon) / resX);
    const y = Math.floor((originLat - clampedLat) / resY);
    return { x, y };
  });

  const valid = coords
    .map((c, idx) => ({
      ...c,
      idx,
      ok: c.x >= 0 && c.y >= 0 && c.x < width && c.y < height
    }))
    .filter((c) => c.ok);

  if (valid.length === 0) {
    return points.map(() => null);
  }

  const xMin = Math.min(...valid.map((c) => c.x));
  const xMax = Math.max(...valid.map((c) => c.x));
  const yMin = Math.min(...valid.map((c) => c.y));
  const yMax = Math.max(...valid.map((c) => c.y));
  const winWidth = xMax - xMin + 1;
  const winHeight = yMax - yMin + 1;

  const raster = await image.readRasters({
    window: [xMin, yMin, xMax + 1, yMax + 1],
    width: winWidth,
    height: winHeight,
    interleave: true,
    samples: [0]
  });

  const data = Array.isArray(raster) ? raster[0] : raster;

  return coords.map((c) => {
    if (!(c.x >= 0 && c.y >= 0 && c.x < width && c.y < height)) return null;
    const idx = (c.y - yMin) * winWidth + (c.x - xMin);
    const raw = data[idx];
    if (raw === undefined || Number.isNaN(raw)) return null;
    if (noData !== null && raw === noData) return null;
    return raw;
  });
}

/**
 * Public API for MOLA DEM elevation queries.
 * @type {{SOURCE_ID: string, SOURCE_NAME: string, URL: string, ensureLoaded: Function, sampleElevations: Function}}
 */
export const molaDem = {
  SOURCE_ID: 'mola_dem',
  SOURCE_NAME: 'MOLA DEM (USGS 128ppd)',
  URL: MOLA_TIFF_URL,
  ensureLoaded: getMolaContext,
  sampleElevations
};
