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
 * Convert planetary spherical coordinates (lat, lon) to 3D Cartesian coordinates (X, Y, Z) in km.
 * @param {number} lat - Latitude in degrees
 * @param {number} lon - Longitude in degrees
 * @param {number} [altKm=0] - Altitude above mean radius
 * @param {string} [body='mars'] - Target planetary body
 * @returns {{x: number, y: number, z: number}} Cartesian coordinates in km
 */
export function sphericalToCartesian(lat, lon, altKm = 0, body = 'mars') {
  const R = (BODIES[body]?.meanRadius || 3389.5) + altKm;
  const phi = lat * Math.PI / 180;
  const lambda = lon * Math.PI / 180;

  const x = R * Math.cos(phi) * Math.cos(lambda);
  const y = R * Math.cos(phi) * Math.sin(lambda);
  const z = R * Math.sin(phi);

  return { x, y, z };
}

/**
 * Convert 3D Cartesian coordinates (X, Y, Z) in km to planetary spherical coordinates (lat, lon, altKm).
 * @param {number} x
 * @param {number} y
 * @param {number} z
 * @param {string} [body='mars']
 * @returns {{lat: number, lon: number, altKm: number}}
 */
export function cartesianToSpherical(x, y, z, body = 'mars') {
  const R_mean = BODIES[body]?.meanRadius || 3389.5;
  const r = Math.sqrt(x * x + y * y + z * z);
  const altKm = r - R_mean;

  const lat = Math.asin(z / r) * 180 / Math.PI;
  const lon = Math.atan2(y, x) * 180 / Math.PI;

  return { lat, lon: to180(lon), altKm };
}

/**
 * Interpolate along the great circle between two points (Spherical Linear Interpolation).
 * @param {number} lat1 - Point 1 latitude
 * @param {number} lon1 - Point 1 longitude
 * @param {number} lat2 - Point 2 latitude
 * @param {number} lon2 - Point 2 longitude
 * @param {number} fraction - Fraction between 0 (pt1) and 1 (pt2)
 * @returns {{lat: number, lon: number}} Interpolated coordinate
 */
export function interpolateGreatCircle(lat1, lon1, lat2, lon2, fraction) {
  const phi1 = lat1 * Math.PI / 180;
  const lambda1 = lon1 * Math.PI / 180;
  const phi2 = lat2 * Math.PI / 180;
  const lambda2 = lon2 * Math.PI / 180;

  // Angular distance d between points
  const cosD = Math.sin(phi1) * Math.sin(phi2) + Math.cos(phi1) * Math.cos(phi2) * Math.cos(lambda2 - lambda1);
  const d = Math.acos(Math.max(-1, Math.min(1, cosD)));

  if (d === 0) return { lat: lat1, lon: to180(lon1) };

  const A = Math.sin((1 - fraction) * d) / Math.sin(d);
  const B = Math.sin(fraction * d) / Math.sin(d);

  const x = A * Math.cos(phi1) * Math.cos(lambda1) + B * Math.cos(phi2) * Math.cos(lambda2);
  const y = A * Math.cos(phi1) * Math.sin(lambda1) + B * Math.cos(phi2) * Math.sin(lambda2);
  const z = A * Math.sin(phi1) + B * Math.sin(phi2);

  const lat = Math.atan2(z, Math.sqrt(x * x + y * y)) * 180 / Math.PI;
  const lon = Math.atan2(y, x) * 180 / Math.PI;

  return { lat, lon: to180(lon) };
}

/**
 * Compute the great-circle midpoint between two geographic coordinates.
 * @param {number} lat1
 * @param {number} lon1
 * @param {number} lat2
 * @param {number} lon2
 * @returns {{lat: number, lon: number}}
 */
export function computeMidpoint(lat1, lon1, lat2, lon2) {
  return interpolateGreatCircle(lat1, lon1, lat2, lon2, 0.5);
}

/**
 * Calculate area of a polygon on a sphere (spherical excess formula).
 * @param {Array<{lat: number, lng: number}|[number, number]>} points - Array of lat/lng points
 * @param {string} body - Body key
 * @returns {number} - Area in square kilometers
 */
export function sphericalPolygonArea(points, body = 'mars') {
  const R = BODIES[body]?.meanRadius || 3389.5;
  const n = points.length;
  if (n < 3) return 0;

  const normalizePoint = p => Array.isArray(p) ? { lat: p[0], lng: p[1] } : p;

  let sum = 0;
  for (let i = 0; i < n; i++) {
    const j = (i + 1) % n;
    const p1 = normalizePoint(points[i]);
    const p2 = normalizePoint(points[j]);

    const lat1 = p1.lat * Math.PI / 180;
    const lon1 = p1.lng * Math.PI / 180;
    const lat2 = p2.lat * Math.PI / 180;
    const lon2 = p2.lng * Math.PI / 180;
    sum += (lon2 - lon1) * (2 + Math.sin(lat1) + Math.sin(lat2));
  }

  return Math.abs(sum * R * R / 2);
}

/**
 * Calculate cumulative geodesic distance of a polyline.
 * @param {Array<{lat: number, lng: number}|[number, number]>} points
 * @param {string} [body='mars']
 * @returns {number} Total distance in kilometers
 */
export function computePolylineLength(points, body = 'mars') {
  if (!points || points.length < 2) return 0;
  const normalizePoint = p => Array.isArray(p) ? { lat: p[0], lng: p[1] } : p;

  let total = 0;
  for (let i = 0; i < points.length - 1; i++) {
    const p1 = normalizePoint(points[i]);
    const p2 = normalizePoint(points[i + 1]);
    total += haversineDistance(p1.lat, p1.lng, p2.lat, p2.lng, body);
  }
  return total;
}

/**
 * Calculate perimeter of a closed polygon.
 * @param {Array<{lat: number, lng: number}|[number, number]>} points
 * @param {string} [body='mars']
 * @returns {number} Total perimeter in kilometers
 */
export function computePolygonPerimeter(points, body = 'mars') {
  if (!points || points.length < 3) return 0;
  const normalizePoint = p => Array.isArray(p) ? { lat: p[0], lng: p[1] } : p;

  let total = 0;
  const n = points.length;
  for (let i = 0; i < n; i++) {
    const p1 = normalizePoint(points[i]);
    const p2 = normalizePoint(points[(i + 1) % n]);
    total += haversineDistance(p1.lat, p1.lng, p2.lat, p2.lng, body);
  }
  return total;
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
export function interpolateGreatCirclePath(lat1, lon1, lat2, lon2, numPoints = 100) {
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

/**
 * Compute rotated landing safety ellipse polygon vertices on a planetary sphere.
 * @param {number} centerLat - Center latitude (degrees)
 * @param {number} centerLon - Center longitude (degrees)
 * @param {number} semiMajorKm - Semi-major axis in kilometers
 * @param {number} semiMinorKm - Semi-minor axis in kilometers
 * @param {number} [azimuthDeg=0] - Azimuth orientation in degrees clockwise from North
 * @param {string} [bodyName='mars'] - Planetary body key
 * @param {number} [numPoints=64] - Number of polygon vertices
 * @returns {Array<[number, number]>} - Array of [lat, lon] coordinates
 */
export function computeEllipsePolygon(centerLat, centerLon, semiMajorKm, semiMinorKm, azimuthDeg = 0, bodyName = 'mars', numPoints = 64) {
  const radius = (BODIES[bodyName] || BODIES.mars).meanRadius;
  const azRad = azimuthDeg * Math.PI / 180;
  const phi0 = centerLat * Math.PI / 180;
  const cosPhi0 = Math.max(Math.cos(phi0), 0.001);
  const coords = [];

  for (let i = 0; i <= numPoints; i++) {
    const theta = (i / numPoints) * 2 * Math.PI;
    // Standard ellipse equation
    const x0 = semiMajorKm * Math.cos(theta);
    const y0 = semiMinorKm * Math.sin(theta);

    // Rotate by azimuth (clock-wise from North: y is North, x is East)
    const xRot = x0 * Math.sin(azRad) + y0 * Math.cos(azRad);
    const yRot = x0 * Math.cos(azRad) - y0 * Math.sin(azRad);

    const dLatDeg = (yRot / radius) * (180 / Math.PI);
    const dLonDeg = (xRot / (radius * cosPhi0)) * (180 / Math.PI);

    let lat = centerLat + dLatDeg;
    let lon = to180(centerLon + dLonDeg);

    coords.push([lat, lon]);
  }

  return coords;
}

/**
 * Compute geodesic buffer polygon around points, lines, or polygons.
 * @param {Array<[number, number]>|[number, number]} coords - Point [lat, lon] or array of points
 * @param {number} radiusKm - Buffer radius in kilometers
 * @param {string} [bodyName='mars'] - Planetary body key
 * @returns {Array<[number, number]>} - Closed buffer polygon coordinates
 */
export function computeBufferPolygon(coords, radiusKm, bodyName = 'mars') {
  if (!coords || radiusKm <= 0) return [];
  const radius = (BODIES[bodyName] || BODIES.mars).meanRadius;

  // 1. Single Point buffer -> Geodesic circle
  if (typeof coords[0] === 'number') {
    return computeEllipsePolygon(coords[0], coords[1], radiusKm, radiusKm, 0, bodyName, 48);
  }

  if (!Array.isArray(coords) || coords.length === 0) return [];

  if (coords.length === 1) {
    return computeEllipsePolygon(coords[0][0], coords[0][1], radiusKm, radiusKm, 0, bodyName, 48);
  }

  // 2. Polyline / Polygon buffer
  const leftSide = [];
  const rightSide = [];

  for (let i = 0; i < coords.length - 1; i++) {
    const p1 = coords[i];
    const p2 = coords[i + 1];

    const lat1 = p1[0];
    const lon1 = p1[1];
    const lat2 = p2[0];
    const lon2 = p2[1];

    const dLat = lat2 - lat1;
    const avgLat = (lat1 + lat2) / 2;
    const cosLat = Math.max(Math.cos(avgLat * Math.PI / 180), 0.001);
    const dLon = (lon2 - lon1) * cosLat;

    const len = Math.sqrt(dLat * dLat + dLon * dLon);
    if (len === 0) continue;

    // Normal vector perpendicular to segment
    const nx = -dLat / len;
    const ny = dLon / len;

    const dLatDist = (ny * radiusKm / radius) * (180 / Math.PI);
    const dLonDist = (nx * radiusKm / (radius * cosLat)) * (180 / Math.PI);

    leftSide.push([lat1 + dLatDist, to180(lon1 + dLonDist)]);
    leftSide.push([lat2 + dLatDist, to180(lon2 + dLonDist)]);

    rightSide.push([lat1 - dLatDist, to180(lon1 - dLonDist)]);
    rightSide.push([lat2 - dLatDist, to180(lon2 - dLonDist)]);
  }

  // Connect left side, then reversed right side to close polygon
  const bufferPoly = [...leftSide, ...rightSide.reverse()];
  if (bufferPoly.length > 0) {
    bufferPoly.push([bufferPoly[0][0], bufferPoly[0][1]]);
  }

  return bufferPoly;
}

/**
 * Test whether a geographic point is inside a closed polygon (Ray-casting PIP algorithm).
 * @param {number} lat - Point latitude
 * @param {number} lon - Point longitude
 * @param {Array<[number, number]>} polygonCoords - Array of [lat, lon] coordinates
 * @returns {boolean} True if point is contained inside polygon
 */
export function isPointInPolygon(lat, lon, polygonCoords) {
  if (!polygonCoords || polygonCoords.length < 3) return false;

  let inside = false;
  const x = to180(lon);
  const y = lat;

  for (let i = 0, j = polygonCoords.length - 1; i < polygonCoords.length; j = i++) {
    const xi = to180(polygonCoords[i][1]);
    const yi = polygonCoords[i][0];
    const xj = to180(polygonCoords[j][1]);
    const yj = polygonCoords[j][0];

    const intersect = ((yi > y) !== (yj > y)) &&
      (x < (xj - xi) * (y - yi) / (yj - yi) + xi);
    if (intersect) inside = !inside;
  }

  return inside;
}

/**
 * Compute bounding box from a collection of coordinates.
 * @param {Array<[number, number]>} coords - Array of [lat, lon]
 * @returns {{minLat: number, maxLat: number, minLon: number, maxLon: number, centerLat: number, centerLon: number}}
 */
export function computeBoundingBox(coords) {
  if (!coords || coords.length === 0) {
    return { minLat: 0, maxLat: 0, minLon: 0, maxLon: 0, centerLat: 0, centerLon: 0 };
  }

  let minLat = Infinity;
  let maxLat = -Infinity;
  let minLon = Infinity;
  let maxLon = -Infinity;

  coords.forEach(p => {
    const lat = p[0];
    const lon = to180(p[1]);
    if (lat < minLat) minLat = lat;
    if (lat > maxLat) maxLat = lat;
    if (lon < minLon) minLon = lon;
    if (lon > maxLon) maxLon = lon;
  });

  return {
    minLat,
    maxLat,
    minLon,
    maxLon,
    centerLat: (minLat + maxLat) / 2,
    centerLon: (minLon + maxLon) / 2
  };
}

/**
 * Calculate destination coordinate given a start coordinate, distance, and bearing.
 * @param {number} lat - Start latitude (degrees)
 * @param {number} lon - Start longitude (degrees)
 * @param {number} distanceKm - Geodesic distance (km)
 * @param {number} bearingDeg - Azimuth bearing (degrees, clockwise from north)
 * @param {string} [body='mars'] - Target planetary body
 * @returns {{lat: number, lon: number}} Destination coordinate
 */
export function computeDestinationPoint(lat, lon, distanceKm, bearingDeg, body = 'mars') {
  const R = BODIES[body]?.meanRadius || 3389.5;
  const delta = distanceKm / R; // angular distance in radians
  const theta = bearingDeg * Math.PI / 180;

  const phi1 = lat * Math.PI / 180;
  const lambda1 = lon * Math.PI / 180;

  const sinPhi2 = Math.sin(phi1) * Math.cos(delta) + Math.cos(phi1) * Math.sin(delta) * Math.cos(theta);
  const phi2 = Math.asin(Math.max(-1, Math.min(1, sinPhi2)));

  const y = Math.sin(theta) * Math.sin(delta) * Math.cos(phi1);
  const x = Math.cos(delta) - Math.sin(phi1) * Math.sin(phi2);
  const lambda2 = lambda1 + Math.atan2(y, x);

  return {
    lat: phi2 * 180 / Math.PI,
    lon: to180(lambda2 * 180 / Math.PI)
  };
}

/**
 * Calculate cross-track distance (perpendicular distance from a point to a great-circle path).
 * @param {number} lat - Point latitude
 * @param {number} lon - Point longitude
 * @param {number} pathLat1 - Path start latitude
 * @param {number} pathLon1 - Path start longitude
 * @param {number} pathLat2 - Path end latitude
 * @param {number} pathLon2 - Path end longitude
 * @param {string} [body='mars']
 * @returns {number} Cross-track distance in kilometers (positive = right of track, negative = left)
 */
export function computeCrossTrackDistance(lat, lon, pathLat1, pathLon1, pathLat2, pathLon2, body = 'mars') {
  const R = BODIES[body]?.meanRadius || 3389.5;
  const d13 = haversineDistance(pathLat1, pathLon1, lat, lon, body) / R;
  const theta13 = azimuth(pathLat1, pathLon1, lat, lon) * Math.PI / 180;
  const theta12 = azimuth(pathLat1, pathLon1, pathLat2, pathLon2) * Math.PI / 180;

  const dXt = Math.asin(Math.sin(d13) * Math.sin(theta13 - theta12));
  return dXt * R;
}

/**
 * Calculate along-track distance from path start to projection of point onto path.
 * @param {number} lat - Point latitude
 * @param {number} lon - Point longitude
 * @param {number} pathLat1 - Path start latitude
 * @param {number} pathLon1 - Path start longitude
 * @param {number} pathLat2 - Path end latitude
 * @param {number} pathLon2 - Path end longitude
 * @param {string} [body='mars']
 * @returns {number} Along-track distance in kilometers
 */
export function computeAlongTrackDistance(lat, lon, pathLat1, pathLon1, pathLat2, pathLon2, body = 'mars') {
  const R = BODIES[body]?.meanRadius || 3389.5;
  const d13 = haversineDistance(pathLat1, pathLon1, lat, lon, body) / R;
  const dXt = computeCrossTrackDistance(lat, lon, pathLat1, pathLon1, pathLat2, pathLon2, body) / R;

  const cosAt = Math.cos(d13) / Math.cos(dXt);
  const dAt = Math.acos(Math.max(-1, Math.min(1, cosAt)));
  return dAt * R;
}

/**
 * Calculate exact spherical midpoint coordinates between two planetary locations.
 * @param {number} lat1 - Latitude of point 1 (degrees)
 * @param {number} lon1 - Longitude of point 1 (degrees)
 * @param {number} lat2 - Latitude of point 2 (degrees)
 * @param {number} lon2 - Longitude of point 2 (degrees)
 * @returns {{lat: number, lon: number}} Midpoint coordinate in degrees
 */
export function computeGreatCircleMidpoint(lat1, lon1, lat2, lon2) {
  const phi1 = lat1 * Math.PI / 180.0;
  const lambda1 = lon1 * Math.PI / 180.0;
  const phi2 = lat2 * Math.PI / 180.0;
  const lambda2 = lon2 * Math.PI / 180.0;
  const dLam = lambda2 - lambda1;

  const Bx = Math.cos(phi2) * Math.cos(dLam);
  const By = Math.cos(phi2) * Math.sin(dLam);

  const phiM = Math.atan2(Math.sin(phi1) + Math.sin(phi2), Math.hypot(Math.cos(phi1) + Bx, By));
  const lambdaM = lambda1 + Math.atan2(By, Math.cos(phi1) + Bx);

  return {
    lat: parseFloat((phiM * 180.0 / Math.PI).toFixed(4)),
    lon: parseFloat((to180(lambdaM * 180.0 / Math.PI)).toFixed(4))
  };
}

/**
 * Calculate 3D interior Euclidean straight-line chord tunnel distance through the planetary interior.
 * d_chord = 2 * R * sin(delta_sigma / 2)
 * @param {number} lat1 - Latitude 1 (degrees)
 * @param {number} lon1 - Longitude 1 (degrees)
 * @param {number} lat2 - Latitude 2 (degrees)
 * @param {number} lon2 - Longitude 2 (degrees)
 * @param {string} [body='mars'] - Planetary body key
 * @returns {{chordDistanceKm: number, chordDistanceMeters: number, arcDifferenceKm: number}}
 */
export function computeTunnelChordDistance(lat1, lon1, lat2, lon2, body = 'mars') {
  const R = BODIES[body]?.meanRadius || 3389.5;
  const sArcKm = haversineDistance(lat1, lon1, lat2, lon2, body);
  const deltaSigma = sArcKm / R; // Angular distance in radians

  const chordKm = 2.0 * R * Math.sin(deltaSigma / 2.0);
  const diffKm = sArcKm - chordKm;

  return {
    chordDistanceKm: parseFloat(chordKm.toFixed(3)),
    chordDistanceMeters: parseFloat((chordKm * 1000.0).toFixed(1)),
    arcDifferenceKm: parseFloat(diffKm.toFixed(3))
  };
}

/**
 * Calculate constant-bearing Rhumb Line (Loxodrome) distance between two planetary coordinates.
 * @param {number} lat1 - Latitude 1 (degrees)
 * @param {number} lon1 - Longitude 1 (degrees)
 * @param {number} lat2 - Latitude 2 (degrees)
 * @param {number} lon2 - Longitude 2 (degrees)
 * @param {string} [body='mars'] - Planetary body key
 * @returns {{rhumbDistanceKm: number, isDirectEastWest: boolean}}
 */
export function computeSphericalRhumbLineDistance(lat1, lon1, lat2, lon2, body = 'mars') {
  const R = BODIES[body]?.meanRadius || 3389.5;
  const phi1 = lat1 * Math.PI / 180.0;
  const phi2 = lat2 * Math.PI / 180.0;
  const dPhi = (lat2 - lat1) * Math.PI / 180.0;

  let dLam = (lon2 - lon1) * Math.PI / 180.0;
  if (Math.abs(dLam) > Math.PI) {
    dLam = dLam > 0 ? -(2.0 * Math.PI - dLam) : (2.0 * Math.PI + dLam);
  }

  const dPsi = Math.log(Math.tan(Math.PI / 4.0 + phi2 / 2.0) / Math.tan(Math.PI / 4.0 + phi1 / 2.0));
  const q = Math.abs(dPsi) > 1e-10 ? dPhi / dPsi : Math.cos(phi1);

  const distKm = Math.sqrt(dPhi * dPhi + q * q * dLam * dLam) * R;

  return {
    rhumbDistanceKm: parseFloat(distKm.toFixed(3)),
    isDirectEastWest: Math.abs(lat1 - lat2) < 1e-4
  };
}



