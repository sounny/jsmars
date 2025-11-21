/**
 * Geographic utility functions for JSMARS.
 */

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
 * Formats a lat/lon pair into a string.
 * @param {number} lat - Latitude
 * @param {number} lon - Longitude
 * @param {number} precision - Number of decimal places
 * @returns {string} - Formatted string
 */
export function formatLatLon(lat, lon, precision = 4) {
  return `${lat.toFixed(precision)}, ${lon.toFixed(precision)}`;
}
