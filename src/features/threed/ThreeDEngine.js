/**
 * @module ThreeDEngine
 * @description Planetary 3D geometry, terrain displacement mesh synthesis,
 * and solar terminator ray vector computations.
 */

export class ThreeDEngine {
  /**
   * Compute 3D subsolar position vector on unit sphere from solar declination and subsolar longitude.
   * @param {number} subSolarLat - Subsolar latitude (degrees).
   * @param {number} subSolarLon - Subsolar longitude (degrees).
   * @returns {{x: number, y: number, z: number}} Unit vector pointing towards the Sun.
   */
  static computeSunVector(subSolarLat, subSolarLon) {
    const latRad = subSolarLat * Math.PI / 180;
    const lonRad = subSolarLon * Math.PI / 180;

    const x = Math.cos(latRad) * Math.cos(lonRad);
    const y = Math.sin(latRad);
    const z = Math.cos(latRad) * Math.sin(lonRad);

    return { x, y, z };
  }

  /**
   * Synthesize elevation displacement height for a grid vertex (x, z) given center coordinates.
   * @param {number} x - Local mesh X (-30 to 30)
   * @param {number} z - Local mesh Z (-30 to 30)
   * @param {number} centerLat - Latitude of center
   * @param {number} centerLon - Longitude of center
   * @param {number} [exaggeration=5.0] - Vertical exaggeration multiplier
   * @returns {number} Displaced elevation height in local 3D units
   */
  static synthesizeTerrainElevation(x, z, centerLat, centerLon, exaggeration = 5.0) {
    const r = Math.sqrt(x * x + z * z);
    let elev = 0;

    // Primary central morphology (e.g. crater or volcanic caldera)
    if (r < 18) {
      elev = -4 * Math.cos((r / 18) * Math.PI * 0.5); // Crater floor
    } else if (r < 24) {
      elev = 3 * Math.sin(((r - 18) / 6) * Math.PI); // Crater rim
    }

    // Regional structural slope and fractal harmonic roughness
    elev += 1.5 * Math.sin(x * 0.2 + centerLat * 0.1) * Math.cos(z * 0.2 + centerLon * 0.1);
    elev += 0.6 * Math.sin(x * 0.5) * Math.sin(z * 0.5);
    elev += 0.25 * Math.sin(x * 1.2 + 0.5) * Math.cos(z * 1.2);

    return elev * (exaggeration * 0.3);
  }

  /**
   * Compute normal vector for a displaced terrain height map.
   * @param {number} x
   * @param {number} z
   * @param {number} centerLat
   * @param {number} centerLon
   * @param {number} exaggeration
   * @returns {{nx: number, ny: number, nz: number}} Unit surface normal vector
   */
  static computeSurfaceNormal(x, z, centerLat, centerLon, exaggeration = 5.0) {
    const eps = 0.1;
    const h0 = this.synthesizeTerrainElevation(x, z, centerLat, centerLon, exaggeration);
    const hx = this.synthesizeTerrainElevation(x + eps, z, centerLat, centerLon, exaggeration);
    const hz = this.synthesizeTerrainElevation(x, z + eps, centerLat, centerLon, exaggeration);

    const dx = (hx - h0) / eps;
    const dz = (hz - h0) / eps;

    // Normal vector: (-dx, 1, -dz) normalized
    const len = Math.sqrt(dx * dx + 1.0 + dz * dz);
    return {
      nx: -dx / len,
      ny: 1.0 / len,
      nz: -dz / len
    };
  }
}
