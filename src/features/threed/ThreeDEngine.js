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

  // --- Planetary Photometry & Shaded Relief Rendering ---

  /**
   * Compute Lambertian shaded relief intensity (0.0 to 1.0).
   * @param {{nx: number, ny: number, nz: number}} normal - Surface normal vector
   * @param {{x: number, y: number, z: number}} sunVector - Solar illumination unit vector
   * @param {number} [ambient=0.15] - Ambient light coefficient
   * @returns {number} Shaded illumination intensity in [0, 1]
   */
  static computeHillshade(normal, sunVector, ambient = 0.15) {
    const dot = normal.nx * sunVector.x + normal.ny * sunVector.y + normal.nz * sunVector.z;
    const direct = Math.max(0, dot);
    return ambient + (1.0 - ambient) * direct;
  }

  /**
   * Compute Lommel-Seeliger scattering reflectance for low-albedo regolith.
   * @param {number} cosIncidence - Cosine of incidence angle (mu0 = cos(i))
   * @param {number} cosEmission - Cosine of emission/view angle (mu = cos(e))
   * @returns {number} Relative reflectance
   */
  static computeLommelSeeligerReflectance(cosIncidence, cosEmission) {
    const mu0 = Math.max(0, cosIncidence);
    const mu = Math.max(0, cosEmission);
    if (mu0 + mu === 0) return 0;
    return mu0 / (mu0 + mu);
  }

  /**
   * Compute Minnaert photometric function for planetary disk rendering.
   * @param {number} cosIncidence - Cosine of incidence angle (mu0)
   * @param {number} cosEmission - Cosine of emission angle (mu)
   * @param {number} [k=0.65] - Minnaert limb-darkening exponent for Mars
   * @returns {number} Minnaert reflectance
   */
  static computeMinnaertReflectance(cosIncidence, cosEmission, k = 0.65) {
    const mu0 = Math.max(1e-4, cosIncidence);
    const mu = Math.max(1e-4, cosEmission);
    return Math.pow(mu0, k) * Math.pow(mu, k - 1.0);
  }

  /**
   * Compute Ground Field of View (GFOV) diameter from camera altitude and FOV angle.
   * @param {number} altitudeKm - Camera altitude in km
   * @param {number} fovDegrees - Camera Field of View in degrees
   * @returns {number} GFOV ground footprint in km
   */
  static computeGroundFOV(altitudeKm, fovDegrees = 45) {
    const fovRad = fovDegrees * Math.PI / 180;
    return 2.0 * Math.max(0, altitudeKm) * Math.tan(fovRad / 2.0);
  }

  // --- Solar Ephemeris, Incidence & Day/Night Terminator Solvers ---

  /**
   * Compute subsolar latitude (solar declination) from Solar Longitude (Ls).
   * @param {number} solarLongitudeLs - Solar longitude in degrees (0-360)
   * @param {number} [obliquityDeg=25.19] - Planetary axial tilt (25.19° for Mars)
   * @returns {number} Subsolar latitude in degrees (-obliquity to +obliquity)
   */
  static computeSolarDeclination(solarLongitudeLs, obliquityDeg = 25.19) {
    const oblRad = obliquityDeg * Math.PI / 180;
    const lsRad = solarLongitudeLs * Math.PI / 180;
    const declRad = Math.asin(Math.sin(oblRad) * Math.sin(lsRad));
    return parseFloat((declRad * 180 / Math.PI).toFixed(3));
  }

  /**
   * Compute solar incidence angle (i) at a surface coordinate.
   * @param {number} latDeg - Surface latitude
   * @param {number} lonDeg - Surface longitude
   * @param {number} subSolarLatDeg - Subsolar point latitude
   * @param {number} subSolarLonDeg - Subsolar point longitude
   * @returns {{incidenceAngleDeg: number, cosIncidence: number, isSunlit: boolean}}
   */
  static computeSolarIncidenceAngle(latDeg, lonDeg, subSolarLatDeg, subSolarLonDeg) {
    const phi = latDeg * Math.PI / 180;
    const delta = subSolarLatDeg * Math.PI / 180;
    const dLon = (lonDeg - subSolarLonDeg) * Math.PI / 180;

    const cosI = Math.sin(phi) * Math.sin(delta) + Math.cos(phi) * Math.cos(delta) * Math.cos(dLon);
    const clampedCos = Math.max(-1.0, Math.min(1.0, cosI));
    const incDeg = Math.acos(clampedCos) * 180 / Math.PI;

    return {
      incidenceAngleDeg: parseFloat(incDeg.toFixed(2)),
      cosIncidence: parseFloat(clampedCos.toFixed(4)),
      isSunlit: incDeg <= 90.0
    };
  }

  /**
   * Compute polar day/night terminator boundary latitudes for a given solar declination.
   * @param {number} subSolarLatDeg - Solar declination in degrees
   * @returns {{polarDayLat: number, polarNightLat: number}}
   */
  static computeTerminatorLatitudes(subSolarLatDeg) {
    const absDecl = Math.abs(subSolarLatDeg);
    const polarDayBoundary = 90.0 - absDecl;
    const polarNightBoundary = -polarDayBoundary;

    return {
      polarDayLat: parseFloat(polarDayBoundary.toFixed(2)),
      polarNightLat: parseFloat(polarNightBoundary.toFixed(2))
    };
  }
}


