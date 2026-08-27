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

/**
 * Calculate spherical excess E and solid angle in steradians for a spherical polygon with interior vertex angles.
 * E = sum(alpha_i) - (n - 2) * pi
 * @param {Array<number>} anglesDegrees - Interior vertex angles in degrees
 * @param {string} [body='mars'] - Planetary body key
 * @returns {{sphericalExcessRad: number, sphericalExcessDeg: number, solidAngleSteradians: number, surfaceAreaKm2: number}}
 */
export function computeSphericalExcess(anglesDegrees = [], body = 'mars') {
  const n = anglesDegrees.length;
  if (n < 3) {
    return { sphericalExcessRad: 0, sphericalExcessDeg: 0, solidAngleSteradians: 0, surfaceAreaKm2: 0 };
  }

  const R = BODIES[body]?.meanRadius || 3389.5;
  const sumRad = anglesDegrees.reduce((acc, deg) => acc + (deg * Math.PI / 180.0), 0);
  const expectedRad = (n - 2) * Math.PI;
  const excessRad = Math.max(0, sumRad - expectedRad);
  const excessDeg = excessRad * 180.0 / Math.PI;

  const areaKm2 = excessRad * R * R;

  return {
    sphericalExcessRad: parseFloat(excessRad.toFixed(5)),
    sphericalExcessDeg: parseFloat(excessDeg.toFixed(4)),
    solidAngleSteradians: parseFloat(excessRad.toFixed(5)),
    surfaceAreaKm2: parseFloat(areaKm2.toFixed(2))
  };
}

/**
 * Calculate ellipsoidal geodesic distance using the Andoyer-Lambert second-order flattening correction.
 * @param {number} lat1 - Start latitude (degrees)
 * @param {number} lon1 - Start longitude (degrees)
 * @param {number} lat2 - End latitude (degrees)
 * @param {number} lon2 - End longitude (degrees)
 * @param {string} [body='mars'] - Target planetary body
 * @returns {{ellipsoidalDistanceKm: number, sphericalDistanceKm: number, flatteningCorrectionKm: number}}
 */
export function computeEllipsoidalGeodesicDistanceAndoyer(lat1, lon1, lat2, lon2, body = 'mars') {
  const b = BODIES[body] || BODIES.mars;
  const a = b.equatorialRadius;
  const f = b.flattening;

  // Reduced latitudes beta1, beta2: tan(beta) = (1 - f) * tan(phi)
  const phi1Rad = lat1 * Math.PI / 180.0;
  const phi2Rad = lat2 * Math.PI / 180.0;
  const beta1 = Math.atan((1.0 - f) * Math.tan(phi1Rad));
  const beta2 = Math.atan((1.0 - f) * Math.tan(phi2Rad));

  const dLam = ((lon2 - lon1) * Math.PI / 180.0);

  // Spherical distance sigma on auxiliary sphere
  const sinB1 = Math.sin(beta1), cosB1 = Math.cos(beta1);
  const sinB2 = Math.sin(beta2), cosB2 = Math.cos(beta2);
  const cosD = sinB1 * sinB2 + cosB1 * cosB2 * Math.cos(dLam);
  const sigma = Math.acos(Math.max(-1.0, Math.min(1.0, cosD)));

  if (sigma < 1e-7) {
    return { ellipsoidalDistanceKm: 0, sphericalDistanceKm: 0, flatteningCorrectionKm: 0 };
  }

  const sinSigma = Math.sin(sigma);
  const sinB_sum = sinB1 + sinB2;
  const cosB_sum = cosB1 + cosB2;
  const K = sinB_sum * sinB_sum;
  const L = cosB_sum * cosB_sum;

  const H = (K / (2.0 * Math.pow(Math.cos(sigma / 2.0), 2))) + (L / (2.0 * Math.pow(Math.sin(sigma / 2.0), 2)));
  const dSigma = (f / 8.0) * (H * (sigma - sinSigma) - (K - L) * (sigma + sinSigma));

  const distEllKm = a * (sigma + dSigma);
  const sSphKm = haversineDistance(lat1, lon1, lat2, lon2, body);
  const diffKm = distEllKm - sSphKm;

  return {
    ellipsoidalDistanceKm: parseFloat(distEllKm.toFixed(3)),
    sphericalDistanceKm: parseFloat(sSphKm.toFixed(3)),
    flatteningCorrectionKm: parseFloat(diffKm.toFixed(3))
  };
}

/**
 * Calculate vertex deflection angles along a planetary traverse / groundtrack polyline.
 * @param {Array<[number, number]>} points - Array of [lat, lon] coordinates
 * @returns {Array<{vertexIndex: number, deflectionAngleDeg: number, isRightTurn: boolean}>}
 */
export function computePolylineDeflectionAngles(points = []) {
  if (points.length < 3) return [];

  const deflections = [];
  for (let i = 1; i < points.length - 1; i++) {
    const pPrev = points[i - 1];
    const pCurr = points[i];
    const pNext = points[i + 1];

    const azIn = azimuth(pPrev[0], pPrev[1], pCurr[0], pCurr[1]);
    const azOut = azimuth(pCurr[0], pCurr[1], pNext[0], pNext[1]);

    let dAz = azOut - azIn;
    while (dAz > 180) dAz -= 360;
    while (dAz < -180) dAz += 360;

    deflections.push({
      vertexIndex: i,
      deflectionAngleDeg: parseFloat(Math.abs(dAz).toFixed(2)),
      isRightTurn: dAz > 0
    });
  }

  return deflections;
}

/**
 * Calculate the enclosing bounding circle (centroid and bounding radius in km) for a collection of points.
 * @param {Array<[number, number]>} points - Array of [lat, lon] coordinates
 * @param {string} [body='mars'] - Planetary body key
 * @returns {{centerLat: number, centerLon: number, radiusKm: number}}
 */
export function computeSphericalBoundingCircle(points = [], body = 'mars') {
  if (!points || points.length === 0) {
    return { centerLat: 0, centerLon: 0, radiusKm: 0 };
  }

  if (points.length === 1) {
    return { centerLat: points[0][0], centerLon: points[0][1], radiusKm: 0 };
  }

  // Centroid computation via 3D unit cartesian vectors
  let x = 0, y = 0, z = 0;
  for (const pt of points) {
    const latRad = pt[0] * Math.PI / 180.0;
    const lonRad = pt[1] * Math.PI / 180.0;
    x += Math.cos(latRad) * Math.cos(lonRad);
    y += Math.cos(latRad) * Math.sin(lonRad);
    z += Math.sin(latRad);
  }

  const n = points.length;
  x /= n; y /= n; z /= n;
  const hyp = Math.hypot(x, y);

  const cLat = Math.atan2(z, hyp) * 180.0 / Math.PI;
  const cLon = Math.atan2(y, x) * 180.0 / Math.PI;

  // Find maximum spherical distance from centroid to any vertex
  let maxDistKm = 0;
  for (const pt of points) {
    const d = haversineDistance(cLat, cLon, pt[0], pt[1], body);
    if (d > maxDistKm) maxDistKm = d;
  }

  return {
    centerLat: parseFloat(cLat.toFixed(4)),
    centerLon: parseFloat(to180(cLon).toFixed(4)),
    radiusKm: parseFloat(maxDistKm.toFixed(3))
  };
}

/**
 * Calculate the intersection coordinates between two great circles on a sphere.
 * (Circle 1 defined by points 1A and 1B, Circle 2 defined by points 2A and 2B).
 * @param {number} lat1A - Point 1A latitude
 * @param {number} lon1A - Point 1A longitude
 * @param {number} lat1B - Point 1B latitude
 * @param {number} lon1B - Point 1B longitude
 * @param {number} lat2A - Point 2A latitude
 * @param {number} lon2A - Point 2A longitude
 * @param {number} lat2B - Point 2B latitude
 * @param {number} lon2B - Point 2B longitude
 * @returns {{lat: number, lon: number, antipodalLat: number, antipodalLon: number}}
 */
export function computeGreatCircleIntersection(lat1A, lon1A, lat1B, lon1B, lat2A, lon2A, lat2B, lon2B) {
  const p1A = sphericalToCartesian(lat1A, lon1A, 1.0);
  const p1B = sphericalToCartesian(lat1B, lon1B, 1.0);
  const p2A = sphericalToCartesian(lat2A, lon2A, 1.0);
  const p2B = sphericalToCartesian(lat2B, lon2B, 1.0);

  // Normal vector to plane 1 = p1A x p1B
  const n1 = [
    p1A.y * p1B.z - p1A.z * p1B.y,
    p1A.z * p1B.x - p1A.x * p1B.z,
    p1A.x * p1B.y - p1A.y * p1B.x
  ];

  // Normal vector to plane 2 = p2A x p2B
  const n2 = [
    p2A.y * p2B.z - p2A.z * p2B.y,
    p2A.z * p2B.x - p2A.x * p2B.z,
    p2A.x * p2B.y - p2A.y * p2B.x
  ];

  // Line of intersection vector = n1 x n2
  const L = [
    n1[1] * n2[2] - n1[2] * n2[1],
    n1[2] * n2[0] - n1[0] * n2[2],
    n1[0] * n2[1] - n1[1] * n2[0]
  ];

  const mag = Math.hypot(L[0], L[1], L[2]);
  if (mag < 1e-10) {
    return { lat: 0, lon: 0, antipodalLat: 0, antipodalLon: 0 };
  }

  const pInt = cartesianToSpherical(L[0] / mag, L[1] / mag, L[2] / mag);

  return {
    lat: parseFloat(pInt.lat.toFixed(4)),
    lon: parseFloat(to180(pInt.lon).toFixed(4)),
    antipodalLat: parseFloat((-pInt.lat).toFixed(4)),
    antipodalLon: parseFloat(to180(pInt.lon + 180).toFixed(4))
  };
}

/**
 * Calculate surface area and eccentricity of an elliptical feature on a planetary surface.
 * @param {number} semiMajorKm - Semi-major axis in km
 * @param {number} semiMinorKm - Semi-minor axis in km
 * @returns {{surfaceAreaKm2: number, eccentricity: number, flattening: number}}
 */
export function computePlanetaryEllipseSurfaceArea(semiMajorKm, semiMinorKm) {
  const a = Math.max(0.001, semiMajorKm);
  const b = Math.max(0.001, Math.min(a, semiMinorKm));

  const area = Math.PI * a * b;
  const ecc = Math.sqrt(Math.max(0, 1.0 - (b * b) / (a * a)));
  const flat = (a - b) / a;

  return {
    surfaceAreaKm2: parseFloat(area.toFixed(2)),
    eccentricity: parseFloat(ecc.toFixed(4)),
    flattening: parseFloat(flat.toFixed(4))
  };
}

// --- Somigliana Theoretical Gravity, Latitude Conversion & Rhumb Line Solvers ---

/**
 * Calculate theoretical normal surface gravity g(phi) on an oblate planetary ellipsoid (Somigliana equation).
 * g(phi) = g_e * ( (1 + k * sin^2(phi)) / sqrt(1 - e^2 * sin^2(phi)) )
 * @param {number} latDeg - Observer latitude in degrees (-90 to +90)
 * @param {number} [equatorialGravity=3.71] - Equatorial surface gravity in m/s^2 (3.71 m/s^2 for Mars)
 * @param {number} [polarGravity=3.73] - Polar surface gravity in m/s^2 (3.73 m/s^2 for Mars)
 * @param {number} [semiMajorKm=3396.19] - Equatorial semi-major radius a in km
 * @param {number} [semiMinorKm=3376.20] - Polar semi-minor radius b in km
 * @returns {{normalGravityMps2: number, latitudeDeg: number, gravityRatio: number}}
 */
export function computeSomiglianaTheoreticalGravity(latDeg, equatorialGravity = 3.71, polarGravity = 3.73, semiMajorKm = 3396.19, semiMinorKm = 3376.20) {
  const phiRad = (Math.max(-90.0, Math.min(90.0, latDeg)) * Math.PI) / 180.0;
  const ge = Math.max(0.1, equatorialGravity);
  const gp = Math.max(0.1, polarGravity);
  const a = Math.max(1.0, semiMajorKm);
  const b = Math.max(1.0, Math.min(a, semiMinorKm));

  const k = (b * gp - a * ge) / (a * ge);
  const e2 = (a * a - b * b) / (a * a);
  const sin2Phi = Math.pow(Math.sin(phiRad), 2);

  const gPhi = ge * ((1.0 + k * sin2Phi) / Math.sqrt(Math.max(1e-6, 1.0 - e2 * sin2Phi)));

  return {
    normalGravityMps2: parseFloat(gPhi.toFixed(4)),
    latitudeDeg: parseFloat(latDeg.toFixed(2)),
    gravityRatio: parseFloat((gPhi / ge).toFixed(5))
  };
}

/**
 * Convert between Planetographic latitude and Planetocentric latitude on an oblate spheroid.
 * tan(phi_c) = (1 - f)^2 * tan(phi_g)
 * @param {number} planetographicLatDeg - Planetographic latitude in degrees (-90 to +90)
 * @param {number} [flattening=0.005886] - Ellipsoidal flattening f = (a - b) / a (0.005886 for Mars)
 * @returns {{planetocentricLatDeg: number, planetographicLatDeg: number, differenceDeg: number}}
 */
export function convertPlanetographicToPlanetocentricLatitude(planetographicLatDeg, flattening = 0.005886) {
  const phiG = Math.max(-90.0, Math.min(90.0, planetographicLatDeg));
  const f = Math.max(0.0, Math.min(0.5, flattening));

  if (Math.abs(Math.abs(phiG) - 90.0) < 1e-6) {
    return {
      planetocentricLatDeg: phiG,
      planetographicLatDeg: phiG,
      differenceDeg: 0.0
    };
  }

  const phiGRad = (phiG * Math.PI) / 180.0;
  const factor = Math.pow(1.0 - f, 2);
  const tanPhiC = factor * Math.tan(phiGRad);
  const phiCRad = Math.atan(tanPhiC);
  const phiCDeg = (phiCRad * 180.0) / Math.PI;

  return {
    planetocentricLatDeg: parseFloat(phiCDeg.toFixed(4)),
    planetographicLatDeg: parseFloat(phiG.toFixed(4)),
    differenceDeg: parseFloat((phiG - phiCDeg).toFixed(4))
  };
}

/**
 * Calculate constant-bearing Rhumb Line (loxodrome) heading azimuth angle between two planetary coordinates.
 * @param {number} lat1 - Starting latitude in degrees
 * @param {number} lon1 - Starting longitude in degrees
 * @param {number} lat2 - Ending latitude in degrees
 * @param {number} lon2 - Ending longitude in degrees
 * @returns {{bearingDeg: number, dLonDeg: number, isEastward: boolean}}
 */
export function computeGreatCircleRhumbLineHeading(lat1, lon1, lat2, lon2) {
  const phi1 = (lat1 * Math.PI) / 180.0;
  const phi2 = (lat2 * Math.PI) / 180.0;

  let dLon = lon2 - lon1;
  while (dLon > 180) dLon -= 360;
  while (dLon < -180) dLon += 360;
  const dLonRad = (dLon * Math.PI) / 180.0;

  const dPsi = Math.log(Math.tan(Math.PI / 4.0 + phi2 / 2.0) / Math.max(1e-10, Math.tan(Math.PI / 4.0 + phi1 / 2.0)));
  const thetaRad = Math.atan2(dLonRad, dPsi);
  let bearingDeg = (thetaRad * 180.0) / Math.PI;
  bearingDeg = (bearingDeg + 360.0) % 360.0;

  return {
    bearingDeg: parseFloat(bearingDeg.toFixed(2)),
    dLonDeg: parseFloat(dLon.toFixed(4)),
    isEastward: dLon > 0
  };
}

// --- Lambert Azimuthal Equal-Area (LAEA), Polar Stereographic & Grid Convergence Solvers ---

/**
 * Calculate forward Lambert Azimuthal Equal-Area (LAEA) projection coordinates (x, y) in km.
 * k' = sqrt( 2 / ( 1 + sin(phi0)*sin(phi) + cos(phi0)*cos(phi)*cos(lam - lam0) ) )
 * x = R * k' * cos(phi) * sin(lam - lam0)
 * y = R * k' * ( cos(phi0)*sin(phi) - sin(phi0)*cos(phi)*cos(lam - lam0) )
 * @param {number} latDeg - Point latitude in degrees
 * @param {number} lonDeg - Point longitude in degrees
 * @param {number} [centerLatDeg=0] - Projection center latitude phi0
 * @param {number} [centerLonDeg=0] - Projection center longitude lambda0
 * @param {number} [radiusKm=3389.5] - Mean spherical radius of the planetary body in km
 * @returns {{xKm: number, yKm: number, scaleFactor: number, isAntipodal: boolean}}
 */
export function computeLambertAzimuthalEqualArea(latDeg, lonDeg, centerLatDeg = 0, centerLonDeg = 0, radiusKm = 3389.5) {
  const phi = (latDeg * Math.PI) / 180.0;
  const lam = (lonDeg * Math.PI) / 180.0;
  const phi0 = (centerLatDeg * Math.PI) / 180.0;
  const lam0 = (centerLonDeg * Math.PI) / 180.0;
  const R = Math.max(1.0, radiusKm);

  let dLam = lam - lam0;
  while (dLam > Math.PI) dLam -= 2.0 * Math.PI;
  while (dLam < -Math.PI) dLam += 2.0 * Math.PI;

  const denom = 1.0 + Math.sin(phi0) * Math.sin(phi) + Math.cos(phi0) * Math.cos(phi) * Math.cos(dLam);
  if (denom <= 1e-10) {
    // Point is at the antipode of the projection center
    return { xKm: 0, yKm: 0, scaleFactor: 0, isAntipodal: true };
  }

  const kPrime = Math.sqrt(2.0 / denom);
  const x = R * kPrime * Math.cos(phi) * Math.sin(dLam);
  const y = R * kPrime * (Math.cos(phi0) * Math.sin(phi) - Math.sin(phi0) * Math.cos(phi) * Math.cos(dLam));

  return {
    xKm: parseFloat(x.toFixed(3)),
    yKm: parseFloat(y.toFixed(3)),
    scaleFactor: parseFloat(kPrime.toFixed(4)),
    isAntipodal: false
  };
}

/**
 * Calculate Polar Stereographic conformal projection coordinates (x, y) in km.
 * For North Pole: rho = 2 * R * tan(pi/4 - phi/2),  x = rho * sin(lam - lam0),  y = -rho * cos(lam - lam0)
 * @param {number} latDeg - Latitude in degrees
 * @param {number} lonDeg - Longitude in degrees
 * @param {number} [centerLonDeg=0] - Central meridian lambda0 in degrees
 * @param {boolean} [isNorthPole=true] - True for North Polar Stereographic, false for South Polar
 * @param {number} [radiusKm=3389.5] - Planetary radius in km
 * @returns {{xKm: number, yKm: number, radialDistanceKm: number}}
 */
export function computePolarStereographic(latDeg, lonDeg, centerLonDeg = 0, isNorthPole = true, radiusKm = 3389.5) {
  const R = Math.max(1.0, radiusKm);
  const phi = (latDeg * Math.PI) / 180.0;
  const lam = (lonDeg * Math.PI) / 180.0;
  const lam0 = (centerLonDeg * Math.PI) / 180.0;

  let dLam = lam - lam0;
  while (dLam > Math.PI) dLam -= 2.0 * Math.PI;
  while (dLam < -Math.PI) dLam += 2.0 * Math.PI;

  let rho;
  let x, y;
  if (isNorthPole) {
    rho = 2.0 * R * Math.tan(Math.PI / 4.0 - phi / 2.0);
    x = rho * Math.sin(dLam);
    y = -rho * Math.cos(dLam);
  } else {
    rho = 2.0 * R * Math.tan(Math.PI / 4.0 + phi / 2.0);
    x = rho * Math.sin(dLam);
    y = rho * Math.cos(dLam);
  }

  return {
    xKm: parseFloat(x.toFixed(3)),
    yKm: parseFloat(y.toFixed(3)),
    radialDistanceKm: parseFloat(rho.toFixed(3))
  };
}

/**
 * Calculate grid meridian convergence angle gamma (angle between Grid North and True Geographic North).
 * gamma = (lambda - lambda0) * sin(phi)
 * @param {number} latDeg - Point latitude in degrees
 * @param {number} lonDeg - Point longitude in degrees
 * @param {number} [centerLonDeg=0] - Central reference meridian in degrees
 * @returns {{convergenceAngleDeg: number, convergenceAngleRad: number, isWestOfMeridian: boolean}}
 */
export function computeMeridianConvergenceAngle(latDeg, lonDeg, centerLonDeg = 0) {
  const phiRad = (latDeg * Math.PI) / 180.0;
  let dLon = lonDeg - centerLonDeg;
  while (dLon > 180) dLon -= 360;
  while (dLon < -180) dLon += 360;

  const gammaDeg = dLon * Math.sin(phiRad);
  const gammaRad = (gammaDeg * Math.PI) / 180.0;

  return {
    convergenceAngleDeg: parseFloat(gammaDeg.toFixed(4)),
    convergenceAngleRad: parseFloat(gammaRad.toFixed(5)),
    isWestOfMeridian: dLon < 0
  };
}

// --- Sinusoidal (Sanson-Flamsteed) Equal-Area & Mercator Scale Distortion Solvers ---

/**
 * Calculate forward Sinusoidal (Sanson-Flamsteed) equal-area cartographic projection (x, y) in km.
 * x = R * (lam - lam0) * cos(phi),  y = R * phi
 * @param {number} latDeg - Point latitude in degrees (-90 to +90)
 * @param {number} lonDeg - Point longitude in degrees
 * @param {number} [centerLonDeg=0] - Central reference meridian in degrees
 * @param {number} [radiusKm=3389.5] - Mean spherical planetary radius in km
 * @returns {{xKm: number, yKm: number, radiusKm: number}}
 */
export function computeSinusoidalProjection(latDeg, lonDeg, centerLonDeg = 0, radiusKm = 3389.5) {
  const R = Math.max(1.0, radiusKm);
  const phi = (latDeg * Math.PI) / 180.0;
  let dLon = lonDeg - centerLonDeg;
  while (dLon > 180) dLon -= 360;
  while (dLon < -180) dLon += 360;
  const dLam = (dLon * Math.PI) / 180.0;

  const x = R * dLam * Math.cos(phi);
  const y = R * phi;

  return {
    xKm: parseFloat(x.toFixed(3)),
    yKm: parseFloat(y.toFixed(3)),
    radiusKm: parseFloat(R.toFixed(1))
  };
}

/**
 * Calculate inverse Sinusoidal projection from Cartesian (x, y) coordinates back to (lat, lon).
 * phi = y / R,  lam = lam0 + x / (R * cos(phi))
 * @param {number} xKm - Projected X coordinate in km
 * @param {number} yKm - Projected Y coordinate in km
 * @param {number} [centerLonDeg=0] - Central meridian in degrees
 * @param {number} [radiusKm=3389.5] - Planetary radius in km
 * @returns {{latDeg: number, lonDeg: number}}
 */
export function computeSinusoidalInverse(xKm, yKm, centerLonDeg = 0, radiusKm = 3389.5) {
  const R = Math.max(1.0, radiusKm);
  const phi = yKm / R;
  const latDeg = (phi * 180.0) / Math.PI;

  const cosPhi = Math.cos(phi);
  let dLamDeg = 0;
  if (Math.abs(cosPhi) > 1e-6) {
    const dLam = xKm / (R * cosPhi);
    dLamDeg = (dLam * 180.0) / Math.PI;
  }

  let lonDeg = centerLonDeg + dLamDeg;
  while (lonDeg > 180) lonDeg -= 360;
  while (lonDeg < -180) lonDeg += 360;

  return {
    latDeg: parseFloat(latDeg.toFixed(4)),
    lonDeg: parseFloat(lonDeg.toFixed(4))
  };
}

/**
 * Calculate conformal Mercator scale distortion factor k = sec(phi) = 1 / cos(phi).
 * @param {number} latDeg - Latitude in degrees (-85 to +85)
 * @returns {{scaleFactor: number, areaScaleFactor: number, latitudeDeg: number}}
 */
export function computeMercatorScaleDistortionFactor(latDeg) {
  const clampedLat = Math.max(-85.0, Math.min(85.0, latDeg));
  const phiRad = (clampedLat * Math.PI) / 180.0;
  const cosPhi = Math.cos(phiRad);

  const k = 1.0 / Math.max(1e-4, cosPhi);
  const kArea = k * k;

  return {
    scaleFactor: parseFloat(k.toFixed(4)),
    areaScaleFactor: parseFloat(kArea.toFixed(4)),
    latitudeDeg: parseFloat(clampedLat.toFixed(2))
  };
}

// --- Orthographic (True 3D Globe Perspective) Forward & Inverse Projections ---

/**
 * Calculate forward Orthographic (Globe View) projection coordinates (x, y) in km.
 * x = R * cos(phi) * sin(lam - lam0)
 * y = R * ( cos(phi0)*sin(phi) - sin(phi0)*cos(phi)*cos(lam - lam0) )
 * @param {number} latDeg - Point latitude in degrees
 * @param {number} lonDeg - Point longitude in degrees
 * @param {number} [centerLatDeg=0] - Center latitude of projection in degrees
 * @param {number} [centerLonDeg=0] - Center longitude in degrees
 * @param {number} [radiusKm=3389.5] - Mean spherical radius in km
 * @returns {{xKm: number, yKm: number, isVisible: boolean, cosAngularDistance: number}}
 */
export function computeOrthographicProjection(latDeg, lonDeg, centerLatDeg = 0, centerLonDeg = 0, radiusKm = 3389.5) {
  const R = Math.max(1.0, radiusKm);
  const phi = (latDeg * Math.PI) / 180.0;
  const phi0 = (centerLatDeg * Math.PI) / 180.0;

  let dLon = lonDeg - centerLonDeg;
  while (dLon > 180) dLon -= 360;
  while (dLon < -180) dLon += 360;
  const dLam = (dLon * Math.PI) / 180.0;

  const sinPhi = Math.sin(phi);
  const cosPhi = Math.cos(phi);
  const sinPhi0 = Math.sin(phi0);
  const cosPhi0 = Math.cos(phi0);
  const cosDLam = Math.cos(dLam);
  const sinDLam = Math.sin(dLam);

  // Angular distance cos(c) from center
  const cosC = sinPhi0 * sinPhi + cosPhi0 * cosPhi * cosDLam;
  const isVisible = cosC >= -1e-6;

  const x = R * cosPhi * sinDLam;
  const y = R * (cosPhi0 * sinPhi - sinPhi0 * cosPhi * cosDLam);

  return {
    xKm: parseFloat(x.toFixed(3)),
    yKm: parseFloat(y.toFixed(3)),
    isVisible: isVisible,
    cosAngularDistance: parseFloat(cosC.toFixed(4))
  };
}

/**
 * Calculate inverse Orthographic projection from planar (x, y) coordinates in km back to (lat, lon).
 * @param {number} xKm - Projected X coordinate in km
 * @param {number} yKm - Projected Y coordinate in km
 * @param {number} [centerLatDeg=0] - Center latitude of projection in degrees
 * @param {number} [centerLonDeg=0] - Center longitude in degrees
 * @param {number} [radiusKm=3389.5] - Mean spherical radius in km
 * @returns {{latDeg: number, lonDeg: number, isInsideGlobeDisk: boolean}}
 */
export function computeOrthographicInverse(xKm, yKm, centerLatDeg = 0, centerLonDeg = 0, radiusKm = 3389.5) {
  const R = Math.max(1.0, radiusKm);
  const phi0 = (centerLatDeg * Math.PI) / 180.0;

  const rho = Math.sqrt(xKm * xKm + yKm * yKm);
  if (rho > R) {
    return {
      latDeg: 0.0,
      lonDeg: 0.0,
      isInsideGlobeDisk: false
    };
  }

  if (rho <= 1e-8) {
    return {
      latDeg: parseFloat(centerLatDeg.toFixed(4)),
      lonDeg: parseFloat(centerLonDeg.toFixed(4)),
      isInsideGlobeDisk: true
    };
  }

  const c = Math.asin(Math.min(1.0, rho / R));
  const sinC = Math.sin(c);
  const cosC = Math.cos(c);
  const sinPhi0 = Math.sin(phi0);
  const cosPhi0 = Math.cos(phi0);

  const sinPhi = cosC * sinPhi0 + (yKm * sinC * cosPhi0) / rho;
  const phi = Math.asin(Math.max(-1.0, Math.min(1.0, sinPhi)));
  const latDeg = (phi * 180.0) / Math.PI;

  const num = xKm * sinC;
  const denom = rho * cosPhi0 * cosC - yKm * sinPhi0 * sinC;
  let dLamRad = Math.atan2(num, denom);
  let lonDeg = centerLonDeg + (dLamRad * 180.0) / Math.PI;
  while (lonDeg > 180) lonDeg -= 360;
  while (lonDeg < -180) lonDeg += 360;

  return {
    latDeg: parseFloat(latDeg.toFixed(4)),
    lonDeg: parseFloat(lonDeg.toFixed(4)),
    isInsideGlobeDisk: true
  };
}

// --- Gnomonic (Central Perspective / Great Circle Straight Line) Forward & Inverse Projections ---

/**
 * Calculate forward Gnomonic projection coordinates (x, y) in km.
 * In Gnomonic projection, ALL Great Circles map strictly to straight lines.
 * x = R * cos(phi) * sin(lam - lam0) / cos(c)
 * y = R * ( cos(phi0)*sin(phi) - sin(phi0)*cos(phi)*cos(lam - lam0) ) / cos(c)
 * @param {number} latDeg - Point latitude in degrees
 * @param {number} lonDeg - Point longitude in degrees
 * @param {number} [centerLatDeg=0] - Center latitude of projection in degrees
 * @param {number} [centerLonDeg=0] - Center longitude in degrees
 * @param {number} [radiusKm=3389.5] - Mean spherical radius in km
 * @returns {{xKm: number, yKm: number, isVisible: boolean, cosAngularDistance: number}}
 */
export function computeGnomonicProjection(latDeg, lonDeg, centerLatDeg = 0, centerLonDeg = 0, radiusKm = 3389.5) {
  const R = Math.max(1.0, radiusKm);
  const phi = (latDeg * Math.PI) / 180.0;
  const phi0 = (centerLatDeg * Math.PI) / 180.0;

  let dLon = lonDeg - centerLonDeg;
  while (dLon > 180) dLon -= 360;
  while (dLon < -180) dLon += 360;
  const dLam = (dLon * Math.PI) / 180.0;

  const sinPhi = Math.sin(phi);
  const cosPhi = Math.cos(phi);
  const sinPhi0 = Math.sin(phi0);
  const cosPhi0 = Math.cos(phi0);
  const cosDLam = Math.cos(dLam);
  const sinDLam = Math.sin(dLam);

  // Angular distance cos(c) from projection center
  const cosC = sinPhi0 * sinPhi + cosPhi0 * cosPhi * cosDLam;
  const isVisible = cosC > 0.001; // Front-hemisphere interior only (< 90° from center)

  if (!isVisible) {
    return {
      xKm: 0.0,
      yKm: 0.0,
      isVisible: false,
      cosAngularDistance: parseFloat(cosC.toFixed(4))
    };
  }

  const x = (R * cosPhi * sinDLam) / cosC;
  const y = (R * (cosPhi0 * sinPhi - sinPhi0 * cosPhi * cosDLam)) / cosC;

  return {
    xKm: parseFloat(x.toFixed(3)),
    yKm: parseFloat(y.toFixed(3)),
    isVisible: true,
    cosAngularDistance: parseFloat(cosC.toFixed(4))
  };
}

/**
 * Calculate inverse Gnomonic projection from planar (x, y) coordinates in km back to (lat, lon).
 * @param {number} xKm - Projected X coordinate in km
 * @param {number} yKm - Projected Y coordinate in km
 * @param {number} [centerLatDeg=0] - Center latitude of projection in degrees
 * @param {number} [centerLonDeg=0] - Center longitude in degrees
 * @param {number} [radiusKm=3389.5] - Mean spherical radius in km
 * @returns {{latDeg: number, lonDeg: number}}
 */
export function computeGnomonicInverse(xKm, yKm, centerLatDeg = 0, centerLonDeg = 0, radiusKm = 3389.5) {
  const R = Math.max(1.0, radiusKm);
  const phi0 = (centerLatDeg * Math.PI) / 180.0;

  const rho = Math.sqrt(xKm * xKm + yKm * yKm);

  if (rho <= 1e-8) {
    return {
      latDeg: parseFloat(centerLatDeg.toFixed(4)),
      lonDeg: parseFloat(centerLonDeg.toFixed(4))
    };
  }

  const c = Math.atan(rho / R);
  const sinC = Math.sin(c);
  const cosC = Math.cos(c);
  const sinPhi0 = Math.sin(phi0);
  const cosPhi0 = Math.cos(phi0);

  const sinPhi = cosC * sinPhi0 + (yKm * sinC * cosPhi0) / rho;
  const phi = Math.asin(Math.max(-1.0, Math.min(1.0, sinPhi)));
  const latDeg = (phi * 180.0) / Math.PI;

  const num = xKm * sinC;
  const denom = rho * cosPhi0 * cosC - yKm * sinPhi0 * sinC;
  let dLamRad = Math.atan2(num, denom);
  let lonDeg = centerLonDeg + (dLamRad * 180.0) / Math.PI;
  while (lonDeg > 180) lonDeg -= 360;
  while (lonDeg < -180) lonDeg += 360;

  return {
    latDeg: parseFloat(latDeg.toFixed(4)),
    lonDeg: parseFloat(lonDeg.toFixed(4))
  };
}

// --- Equidistant Cylindrical / Plate Carrée Forward & Inverse Projections ---

/**
 * Calculate forward Equidistant Cylindrical (Plate Carrée when standardParallelDeg = 0) projection coordinates.
 * x = R * (lambda - lambda0) * cos(phi1)
 * y = R * phi
 * Scale along meridian: h = 1.0 (true to scale)
 * Scale along parallel: k = cos(phi) / cos(phi1)
 * @param {number} latDeg - Point latitude in degrees
 * @param {number} lonDeg - Point longitude in degrees
 * @param {number} [standardParallelDeg=0.0] - Standard parallel phi1 in degrees
 * @param {number} [centerLonDeg=0.0] - Central meridian lambda0 in degrees
 * @param {number} [radiusKm=3389.5] - Mean spherical planetary radius in km
 * @returns {{xKm: number, yKm: number, parallelScaleFactor: number, arealDistortion: number}}
 */
export function computeEquidistantCylindricalProjection(latDeg, lonDeg, standardParallelDeg = 0.0, centerLonDeg = 0.0, radiusKm = 3389.5) {
  const R = Math.max(1.0, radiusKm);
  const phi = (latDeg * Math.PI) / 180.0;
  const phi1 = (standardParallelDeg * Math.PI) / 180.0;

  let dLon = lonDeg - centerLonDeg;
  while (dLon > 180) dLon -= 360;
  while (dLon < -180) dLon += 360;
  const dLam = (dLon * Math.PI) / 180.0;

  const cosPhi1 = Math.max(1e-6, Math.cos(phi1));
  const cosPhi = Math.cos(phi);

  const x = R * dLam * cosPhi1;
  const y = R * phi;

  const k = cosPhi / cosPhi1;

  return {
    xKm: parseFloat(x.toFixed(3)),
    yKm: parseFloat(y.toFixed(3)),
    parallelScaleFactor: parseFloat(k.toFixed(4)),
    arealDistortion: parseFloat(k.toFixed(4))
  };
}

/**
 * Calculate inverse Equidistant Cylindrical projection from planar (x, y) coordinates in km back to (lat, lon).
 * @param {number} xKm - Projected X coordinate in km
 * @param {number} yKm - Projected Y coordinate in km
 * @param {number} [standardParallelDeg=0.0] - Standard parallel phi1 in degrees
 * @param {number} [centerLonDeg=0.0] - Central meridian lambda0 in degrees
 * @param {number} [radiusKm=3389.5] - Mean spherical planetary radius in km
 * @returns {{latDeg: number, lonDeg: number}}
 */
export function computeEquidistantCylindricalInverse(xKm, yKm, standardParallelDeg = 0.0, centerLonDeg = 0.0, radiusKm = 3389.5) {
  const R = Math.max(1.0, radiusKm);
  const phi1 = (standardParallelDeg * Math.PI) / 180.0;
  const cosPhi1 = Math.max(1e-6, Math.cos(phi1));

  const phi = yKm / R;
  const dLam = xKm / (R * cosPhi1);

  let latDeg = (phi * 180.0) / Math.PI;
  let lonDeg = centerLonDeg + (dLam * 180.0) / Math.PI;

  latDeg = Math.max(-90.0, Math.min(90.0, latDeg));
  while (lonDeg > 180) lonDeg -= 360;
  while (lonDeg < -180) lonDeg += 360;

  return {
    latDeg: parseFloat(latDeg.toFixed(4)),
    lonDeg: parseFloat(lonDeg.toFixed(4))
  };
}

// --- Lambert Conformal Conic (LCC) Forward & Inverse Projections ---

/**
 * Calculate forward Lambert Conformal Conic (LCC) projection coordinates and scale factor.
 * Ideal for mid-latitude east-west regional maps (e.g. Valles Marineris, Arabia Terra, landing corridors).
 * @param {number} latDeg - Point latitude in degrees
 * @param {number} lonDeg - Point longitude in degrees
 * @param {number} stdParallel1Deg - First standard parallel phi1 in degrees
 * @param {number} stdParallel2Deg - Second standard parallel phi2 in degrees
 * @param {number} [originLatDeg=0.0] - Latitude of grid origin phi0 in degrees
 * @param {number} [centerLonDeg=0.0] - Central meridian lambda0 in degrees
 * @param {number} [radiusKm=3389.5] - Mean spherical planetary radius in km
 * @returns {{xKm: number, yKm: number, scaleFactor: number, coneConstantN: number}}
 */
export function computeLambertConformalConicProjection(latDeg, lonDeg, stdParallel1Deg, stdParallel2Deg, originLatDeg = 0.0, centerLonDeg = 0.0, radiusKm = 3389.5) {
  const R = Math.max(1.0, radiusKm);
  const phi = (latDeg * Math.PI) / 180.0;
  const phi1 = (stdParallel1Deg * Math.PI) / 180.0;
  const phi2 = (stdParallel2Deg * Math.PI) / 180.0;
  const phi0 = (originLatDeg * Math.PI) / 180.0;

  let dLon = lonDeg - centerLonDeg;
  while (dLon > 180) dLon -= 360;
  while (dLon < -180) dLon += 360;
  const dLam = (dLon * Math.PI) / 180.0;

  let n = 0;
  if (Math.abs(phi1 - phi2) < 1e-6) {
    n = Math.sin(phi1);
  } else {
    const numN = Math.log(Math.cos(phi1) / Math.cos(phi2));
    const denN = Math.log(Math.tan(Math.PI / 4.0 + phi2 / 2.0) / Math.tan(Math.PI / 4.0 + phi1 / 2.0));
    n = numN / denN;
  }

  const F = (Math.cos(phi1) * Math.pow(Math.tan(Math.PI / 4.0 + phi1 / 2.0), n)) / n;

  const t = 1.0 / Math.tan(Math.PI / 4.0 + phi / 2.0);
  const t0 = 1.0 / Math.tan(Math.PI / 4.0 + phi0 / 2.0);

  const rho = R * F * Math.pow(t, n);
  const rho0 = R * F * Math.pow(t0, n);

  const theta = n * dLam;
  const x = rho * Math.sin(theta);
  const y = rho0 - rho * Math.cos(theta);

  const cosPhi = Math.max(1e-6, Math.cos(phi));
  const k = (rho * n) / (R * cosPhi);

  return {
    xKm: parseFloat(x.toFixed(3)),
    yKm: parseFloat(y.toFixed(3)),
    scaleFactor: parseFloat(k.toFixed(4)),
    coneConstantN: parseFloat(n.toFixed(4))
  };
}

/**
 * Calculate inverse Lambert Conformal Conic (LCC) projection from planar coordinates back to (lat, lon).
 * @param {number} xKm - Projected X coordinate in km
 * @param {number} yKm - Projected Y coordinate in km
 * @param {number} stdParallel1Deg - First standard parallel phi1 in degrees
 * @param {number} stdParallel2Deg - Second standard parallel phi2 in degrees
 * @param {number} [originLatDeg=0.0] - Latitude of grid origin phi0 in degrees
 * @param {number} [centerLonDeg=0.0] - Central meridian lambda0 in degrees
 * @param {number} [radiusKm=3389.5] - Mean spherical planetary radius in km
 * @returns {{latDeg: number, lonDeg: number}}
 */
export function computeLambertConformalConicInverse(xKm, yKm, stdParallel1Deg, stdParallel2Deg, originLatDeg = 0.0, centerLonDeg = 0.0, radiusKm = 3389.5) {
  const R = Math.max(1.0, radiusKm);
  const phi1 = (stdParallel1Deg * Math.PI) / 180.0;
  const phi2 = (stdParallel2Deg * Math.PI) / 180.0;
  const phi0 = (originLatDeg * Math.PI) / 180.0;

  let n = 0;
  if (Math.abs(phi1 - phi2) < 1e-6) {
    n = Math.sin(phi1);
  } else {
    const numN = Math.log(Math.cos(phi1) / Math.cos(phi2));
    const denN = Math.log(Math.tan(Math.PI / 4.0 + phi2 / 2.0) / Math.tan(Math.PI / 4.0 + phi1 / 2.0));
    n = numN / denN;
  }

  const F = (Math.cos(phi1) * Math.pow(Math.tan(Math.PI / 4.0 + phi1 / 2.0), n)) / n;
  const t0 = 1.0 / Math.tan(Math.PI / 4.0 + phi0 / 2.0);
  const rho0 = R * F * Math.pow(t0, n);

  const signN = n >= 0 ? 1 : -1;
  const rhoPrime = Math.sqrt(xKm * xKm + (rho0 - yKm) * (rho0 - yKm)) * signN;
  const thetaPrime = Math.atan2(xKm * signN, (rho0 - yKm) * signN);

  const tPrime = Math.pow(rhoPrime / (R * F), 1.0 / n);
  const phiPrime = 2.0 * Math.atan(1.0 / tPrime) - Math.PI / 2.0;

  let latDeg = (phiPrime * 180.0) / Math.PI;
  let lonDeg = centerLonDeg + (thetaPrime / n) * (180.0 / Math.PI);

  latDeg = Math.max(-90.0, Math.min(90.0, latDeg));
  while (lonDeg > 180) lonDeg -= 360;
  while (lonDeg < -180) lonDeg += 360;

  return {
    latDeg: parseFloat(latDeg.toFixed(4)),
    lonDeg: parseFloat(lonDeg.toFixed(4))
  };
}

// --- Polar Stereographic Cartographic Projections ---

/**
 * Calculate forward Polar Stereographic cartographic projection coordinates (x, y) in km.
 * Used for JMARS North/South Polar stereographic views (Snyder 1987).
 * North: rho = 2*R*k0 * tan(pi/4 - phi/2), x = rho*sin(lam - lam0), y = -rho*cos(lam - lam0)
 * South: rho = 2*R*k0 * tan(pi/4 + phi/2), x = rho*sin(lam - lam0), y = rho*cos(lam - lam0)
 * @param {number} latDeg - Planetocentric latitude in degrees (-90 to +90)
 * @param {number} lonDeg - Longitude in degrees (-180 to +180)
 * @param {boolean} [isNorthPole=true] - True for North Polar Stereographic, false for South
 * @param {number} [centerLonDeg=0.0] - Central meridian lambda0 in degrees
 * @param {number} [k0=1.0] - Scale factor at pole
 * @param {number} [radiusKm=3389.5] - Planetary spherical radius in km
 * @returns {{xKm: number, yKm: number, radialDistanceKm: number, scaleFactorK: number}}
 */
export function computePolarStereographicProjection(latDeg, lonDeg, isNorthPole = true, centerLonDeg = 0.0, k0 = 1.0, radiusKm = 3389.5) {
  const R = Math.max(1.0, radiusKm);
  const phi = (latDeg * Math.PI) / 180.0;
  const dLon = ((lonDeg - centerLonDeg) * Math.PI) / 180.0;

  let rho = 0.0;
  let x = 0.0;
  let y = 0.0;
  let k = 1.0;

  if (isNorthPole) {
    if (latDeg >= 90.0) {
      return { xKm: 0.0, yKm: 0.0, radialDistanceKm: 0.0, scaleFactorK: parseFloat(k0.toFixed(4)) };
    }
    const clampedLat = Math.min(89.99999, Math.max(-89.99999, latDeg));
    const phiClamped = (clampedLat * Math.PI) / 180.0;
    rho = 2.0 * R * k0 * Math.tan(Math.PI / 4.0 - phiClamped / 2.0);
    x = rho * Math.sin(dLon);
    y = -rho * Math.cos(dLon);
    k = (2.0 * k0) / (1.0 + Math.sin(phiClamped));
  } else {
    if (latDeg <= -90.0) {
      return { xKm: 0.0, yKm: 0.0, radialDistanceKm: 0.0, scaleFactorK: parseFloat(k0.toFixed(4)) };
    }
    const clampedLat = Math.min(89.99999, Math.max(-89.99999, latDeg));
    const phiClamped = (clampedLat * Math.PI) / 180.0;
    rho = 2.0 * R * k0 * Math.tan(Math.PI / 4.0 + phiClamped / 2.0);
    x = rho * Math.sin(dLon);
    y = rho * Math.cos(dLon);
    k = (2.0 * k0) / (1.0 - Math.sin(phiClamped));
  }

  return {
    xKm: parseFloat(x.toFixed(3)),
    yKm: parseFloat(y.toFixed(3)),
    radialDistanceKm: parseFloat(rho.toFixed(3)),
    scaleFactorK: parseFloat(k.toFixed(4))
  };
}

/**
 * Calculate inverse Polar Stereographic cartographic projection coordinates (lat, lon).
 * @param {number} xKm - Projected X coordinate in km
 * @param {number} yKm - Projected Y coordinate in km
 * @param {boolean} [isNorthPole=true] - True for North Polar, false for South Polar
 * @param {number} [centerLonDeg=0.0] - Central meridian lambda0 in degrees
 * @param {number} [k0=1.0] - Scale factor at pole
 * @param {number} [radiusKm=3389.5] - Planetary spherical radius in km
 * @returns {{latDeg: number, lonDeg: number}}
 */
export function computePolarStereographicInverse(xKm, yKm, isNorthPole = true, centerLonDeg = 0.0, k0 = 1.0, radiusKm = 3389.5) {
  const R = Math.max(1.0, radiusKm);
  const rho = Math.sqrt(xKm * xKm + yKm * yKm);

  if (rho === 0.0) {
    return {
      latDeg: isNorthPole ? 90.0 : -90.0,
      lonDeg: centerLonDeg
    };
  }

  const c = 2.0 * Math.atan(rho / (2.0 * R * k0));

  let latRad = 0.0;
  let lonRad = 0.0;

  if (isNorthPole) {
    latRad = Math.PI / 2.0 - c;
    lonRad = (centerLonDeg * Math.PI) / 180.0 + Math.atan2(xKm, -yKm);
  } else {
    latRad = -Math.PI / 2.0 + c;
    lonRad = (centerLonDeg * Math.PI) / 180.0 + Math.atan2(xKm, yKm);
  }

  let latDeg = (latRad * 180.0) / Math.PI;
  let lonDeg = (lonRad * 180.0) / Math.PI;

  while (lonDeg > 180) lonDeg -= 360;
  while (lonDeg < -180) lonDeg += 360;

  return {
    latDeg: parseFloat(latDeg.toFixed(4)),
    lonDeg: parseFloat(lonDeg.toFixed(4))
  };
}






