/**
 * Geographic utility functions for jsMars.
 * Includes planetary-specific calculations for Mars, Moon, and Earth.
 */

// Planetary radii (km)
export const BODIES = {
  mars: {
    name: 'Mars',
    equatorialRadius: 3396.2,
    polarRadius: 3376.2,
    meanRadius: 3389.5,
    flattening: 0.00589
  },
  moon: {
    name: 'Moon',
    equatorialRadius: 1738.1,
    polarRadius: 1736.0,
    meanRadius: 1737.4,
    flattening: 0.0012
  },
  earth: {
    name: 'Earth',
    equatorialRadius: 6378.137,
    polarRadius: 6356.752,
    meanRadius: 6371.0,
    // WGS84 exact: 1/298.257223563 = 0.003352811; using rounded approximation
    flattening: 0.003353
  }
};

/**
 * Normalize a longitude to the [0, 360) range.
 * @param {number} lon - Longitude in degrees.
 * @returns {number} - Normalized longitude.
 */
export function normalizeLon(lon) {
  let n = lon % 360;
  return (n < 0) ? n + 360 : n;
}

/**
 * Convert a longitude from [0, 360) to [-180, 180).
 * @param {number} lon - Longitude in degrees.
 * @returns {number} - Longitude in [-180, 180).
 */
export function to180(lon) {
  let n = normalizeLon(lon);
  return (n > 180) ? n - 360 : n;
}

/**
 * Convert a longitude from [-180, 180) to [0, 360).
 * @param {number} lon - Longitude in degrees.
 * @returns {number} - Longitude in [0, 360).
 */
export function to360(lon) {
  return normalizeLon(lon);
}

/**
 * Convert planetocentric latitude to planetographic latitude.
 * @param {number} lat - Planetocentric latitude in degrees
 * @param {string} body - Body key ('mars', 'moon', 'earth')
 * @returns {number} - Planetographic latitude in degrees
 */
export function toGraphic(lat, body = 'mars') {
  const b = BODIES[body];
  if (!b) return lat;
  const ratio = (b.equatorialRadius / b.polarRadius) ** 2;
  const radLat = lat * Math.PI / 180;
  return Math.atan(ratio * Math.tan(radLat)) * 180 / Math.PI;
}

/**
 * Convert planetographic latitude to planetocentric latitude.
 * @param {number} lat - Planetographic latitude in degrees
 * @param {string} body - Body key ('mars', 'moon', 'earth')
 * @returns {number} - Planetocentric latitude in degrees
 */
export function toCentric(lat, body = 'mars') {
  const b = BODIES[body];
  if (!b) return lat;
  const ratio = (b.polarRadius / b.equatorialRadius) ** 2;
  const radLat = lat * Math.PI / 180;
  return Math.atan(ratio * Math.tan(radLat)) * 180 / Math.PI;
}

/**
 * Calculate great-circle distance between two points on a sphere (Haversine formula).
 * @param {number} lat1 - Latitude of point 1 (degrees)
 * @param {number} lon1 - Longitude of point 1 (degrees)
 * @param {number} lat2 - Latitude of point 2 (degrees)
 * @param {number} lon2 - Longitude of point 2 (degrees)
 * @param {string} body - Body key ('mars', 'moon', 'earth')
 * @returns {number} - Distance in kilometers
 */
export function haversineDistance(lat1, lon1, lat2, lon2, body = 'mars') {
  const R = BODIES[body]?.meanRadius || 3389.5;
  const dLat = (lat2 - lat1) * Math.PI / 180;
  const dLon = (lon2 - lon1) * Math.PI / 180;
  const a = Math.sin(dLat / 2) ** 2 +
    Math.cos(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180) *
    Math.sin(dLon / 2) ** 2;
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
  return R * c;
}

/**
 * Calculate azimuth (initial bearing) from point 1 to point 2.
 * @param {number} lat1 - Latitude of point 1 (degrees)
 * @param {number} lon1 - Longitude of point 1 (degrees)
 * @param {number} lat2 - Latitude of point 2 (degrees)
 * @param {number} lon2 - Longitude of point 2 (degrees)
 * @returns {number} - Azimuth in degrees (0-360, clockwise from north)
 */
export function azimuth(lat1, lon1, lat2, lon2) {
  const phi1 = lat1 * Math.PI / 180;
  const phi2 = lat2 * Math.PI / 180;
  const dLambda = (lon2 - lon1) * Math.PI / 180;

  const y = Math.sin(dLambda) * Math.cos(phi2);
  const x = Math.cos(phi1) * Math.sin(phi2) -
    Math.sin(phi1) * Math.cos(phi2) * Math.cos(dLambda);

  let bearing = Math.atan2(y, x) * 180 / Math.PI;
  return (bearing + 360) % 360;
}

/**
 * Convert degrees to degrees/minutes/seconds string.
 * @param {number} dd - Decimal degrees
 * @param {boolean} isLat - True for latitude, false for longitude
 * @returns {string} - Formatted DMS string (e.g., "45\u00b030'15.5\"N")
 */
export function toDMS(dd, isLat) {
  const dir = isLat ? (dd >= 0 ? 'N' : 'S') : (dd >= 0 ? 'E' : 'W');
  const abs = Math.abs(dd);
  const d = Math.floor(abs);
  const mFloat = (abs - d) * 60;
  const m = Math.floor(mFloat);
  const s = ((mFloat - m) * 60).toFixed(1);
  return `${d}\u00b0${String(m).padStart(2, '0')}'${String(s).padStart(4, '0')}"${dir}`;
}

/**
 * Calculate area of a polygon on a sphere (spherical excess formula).
 * @param {Array<{lat: number, lng: number}>} points - Array of lat/lng points
 * @param {string} body - Body key
 * @returns {number} - Area in square kilometers
 */
export function sphericalPolygonArea(points, body = 'mars') {
  const R = BODIES[body]?.meanRadius || 3389.5;
  const n = points.length;
  if (n < 3) return 0;

  let sum = 0;
  for (let i = 0; i < n; i++) {
    const j = (i + 1) % n;
    const lat1 = points[i].lat * Math.PI / 180;
    const lon1 = points[i].lng * Math.PI / 180;
    const lat2 = points[j].lat * Math.PI / 180;
    const lon2 = points[j].lng * Math.PI / 180;
    sum += (lon2 - lon1) * (2 + Math.sin(lat1) + Math.sin(lat2));
  }

  return Math.abs(sum * R * R / 2);
}

/**
 * Formats a lat/lon pair into a string.
 * @param {number} lat - Latitude
 * @param {number} lon - Longitude
 * @param {object} options - Formatting options
 * @param {number} [options.precision=4] - Decimal places
 * @param {string} [options.lonFormat='east180'] - 'east180' (-180 to 180), 'east360' (0 to 360), 'west360' (0W to 360W)
 * @param {string} [options.latFormat='centric'] - 'centric' or 'graphic'
 * @param {string} [options.notation='decimal'] - 'decimal' or 'dms'
 * @param {string} [options.body='mars'] - Body for conversions
 * @returns {string} - Formatted string
 */
export function formatLatLon(lat, lon, options = {}) {
  const {
    precision = 4,
    lonFormat = 'east180',
    latFormat = 'centric',
    notation = 'decimal',
    body = 'mars'
  } = options;

  // Apply latitude conversion
  let displayLat = lat;
  if (latFormat === 'graphic') {
    displayLat = toGraphic(lat, body);
  }

  // Apply longitude conversion
  let displayLon = lon;
  if (lonFormat === 'east360') {
    displayLon = to360(lon);
  } else if (lonFormat === 'west360') {
    displayLon = 360 - to360(lon);
    if (displayLon < 0) displayLon += 360;
    if (displayLon >= 360) displayLon -= 360; // Avoid displaying "360.0000"
  } else {
    displayLon = to180(lon);
  }

  // Format output
  if (notation === 'dms') {
    return `${toDMS(displayLat, true)} ${toDMS(displayLon, false)}`;
  }
  return `${displayLat.toFixed(precision)}, ${displayLon.toFixed(precision)}`;
}

/**
 * Interpolate points along a great circle path.
 * @param {number} lat1 - Start latitude (degrees)
 * @param {number} lon1 - Start longitude (degrees)
 * @param {number} lat2 - End latitude (degrees)
 * @param {number} lon2 - End longitude (degrees)
 * @param {number} numPoints - Number of intermediate points
 * @returns {Array<{lat: number, lng: number}>} - Array of interpolated points
 */
export function interpolateGreatCircle(lat1, lon1, lat2, lon2, numPoints = 100) {
  const phi1 = lat1 * Math.PI / 180;
  const lambda1 = lon1 * Math.PI / 180;
  const phi2 = lat2 * Math.PI / 180;
  const lambda2 = lon2 * Math.PI / 180;

  const d = 2 * Math.asin(
    Math.sqrt(
      Math.sin((phi2 - phi1) / 2) ** 2 +
      Math.cos(phi1) * Math.cos(phi2) * Math.sin((lambda2 - lambda1) / 2) ** 2
    )
  );

  if (d < 1e-10) return [{ lat: lat1, lng: lon1 }];

  const points = [];
  for (let i = 0; i <= numPoints; i++) {
    const f = i / numPoints;
    const A = Math.sin((1 - f) * d) / Math.sin(d);
    const B = Math.sin(f * d) / Math.sin(d);
    const x = A * Math.cos(phi1) * Math.cos(lambda1) + B * Math.cos(phi2) * Math.cos(lambda2);
    const y = A * Math.cos(phi1) * Math.sin(lambda1) + B * Math.cos(phi2) * Math.sin(lambda2);
    const z = A * Math.sin(phi1) + B * Math.sin(phi2);
    const lat = Math.atan2(z, Math.sqrt(x * x + y * y)) * 180 / Math.PI;
    const lng = Math.atan2(y, x) * 180 / Math.PI;
    points.push({ lat, lng });
  }
  return points;
}
