const MOLA_TIFF_URL = 'https://asc-pds-services.s3.us-west-2.amazonaws.com/mosaic/mola128_88Nto88S_Simp_clon0.tif';

let geoTiffScriptPromise = null;
let molaContextPromise = null;

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

  return geoTiffScriptPromise;
}

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

  return molaContextPromise;
}

async function sampleElevations(points) {
  // points: [{ lat, lng|lon }]
  const ctx = await getMolaContext();
  const { image, bbox, width, height, resX, resY, noData, origin } = ctx;
  const [minLon, minLat, maxLon, maxLat] = bbox;
  const originLon = origin?.[0] ?? minLon;
  const originLat = origin?.[1] ?? maxLat;
  const lonSpan = resX * width;
  const latSpan = resY * height;

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

export const molaDem = {
  SOURCE_ID: 'mola_dem',
  SOURCE_NAME: 'MOLA DEM (USGS 128ppd)',
  URL: MOLA_TIFF_URL,
  ensureLoaded: getMolaContext,
  sampleElevations
};
