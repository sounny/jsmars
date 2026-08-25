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

  // --- 3D Ellipsoidal Geodesy & Coordinate Transformations ---

  /**
   * Convert Geographic coordinates (lat, lon, altitude) to 3D Cartesian coordinates on planetary ellipsoid.
   * @param {number} latDeg - Planetocentric / Geodetic latitude (-90 to +90)
   * @param {number} lonDeg - Longitude (0 to 360 East)
   * @param {number} [altKm=0] - Altitude above reference ellipsoid in km
   * @param {string} [body='mars'] - Planetary body
   * @returns {{x: number, y: number, z: number, radiusKm: number}}
   */
  static geographicToCartesian(latDeg, lonDeg, altKm = 0, body = 'mars') {
    const isMars = body.toLowerCase() === 'mars';
    const a = isMars ? 3396.19 : (body.toLowerCase() === 'moon' ? 1737.4 : 6378.14);
    const b = isMars ? 3376.20 : (body.toLowerCase() === 'moon' ? 1737.4 : 6356.75);

    const f = (a - b) / a;
    const e2 = 2 * f - f * f;

    const phi = latDeg * Math.PI / 180.0;
    const lambda = lonDeg * Math.PI / 180.0;

    const sinPhi = Math.sin(phi);
    const cosPhi = Math.cos(phi);
    const sinLam = Math.sin(lambda);
    const cosLam = Math.cos(lambda);

    const N = a / Math.sqrt(1.0 - e2 * sinPhi * sinPhi);

    const x = (N + altKm) * cosPhi * cosLam;
    const y = (N + altKm) * cosPhi * sinLam;
    const z = (N * (1.0 - e2) + altKm) * sinPhi;
    const radius = Math.hypot(x, y, z);

    return {
      x: parseFloat(x.toFixed(3)),
      y: parseFloat(y.toFixed(3)),
      z: parseFloat(z.toFixed(3)),
      radiusKm: parseFloat(radius.toFixed(3))
    };
  }

  /**
   * Convert 3D Cartesian coordinates to Geographic (lat, lon, alt) on planetary ellipsoid via Bowring's method.
   * @param {number} x - Cartesian X in km
   * @param {number} y - Cartesian Y in km
   * @param {number} z - Cartesian Z in km
   * @param {string} [body='mars'] - Planetary body
   * @returns {{lat: number, lon: number, altKm: number}}
   */
  static cartesianToGeographic(x, y, z, body = 'mars') {
    const isMars = body.toLowerCase() === 'mars';
    const a = isMars ? 3396.19 : (body.toLowerCase() === 'moon' ? 1737.4 : 6378.14);
    const b = isMars ? 3376.20 : (body.toLowerCase() === 'moon' ? 1737.4 : 6356.75);

    const f = (a - b) / a;
    const e2 = 2 * f - f * f;
    const ePrime2 = (a * a - b * b) / (b * b);

    const p = Math.hypot(x, y);
    if (p < 1e-6) {
      return {
        lat: z >= 0 ? 90.0 : -90.0,
        lon: 0.0,
        altKm: parseFloat((Math.abs(z) - b).toFixed(3))
      };
    }

    let lon = Math.atan2(y, x) * 180.0 / Math.PI;
    if (lon < 0) lon += 360.0;

    const theta = Math.atan2(z * a, p * b);
    const phi = Math.atan2(
      z + ePrime2 * b * Math.pow(Math.sin(theta), 3),
      p - e2 * a * Math.pow(Math.cos(theta), 3)
    );

    const sinPhi = Math.sin(phi);
    const N = a / Math.sqrt(1.0 - e2 * sinPhi * sinPhi);
    const alt = p / Math.cos(phi) - N;

    return {
      lat: parseFloat((phi * 180.0 / Math.PI).toFixed(4)),
      lon: parseFloat(lon.toFixed(4)),
      altKm: parseFloat(alt.toFixed(3))
    };
  }

  /**
   * Calculate solar elevation angle (altitude above horizon) in degrees.
   * @param {number} latDeg - Surface latitude
   * @param {number} lonDeg - Surface longitude
   * @param {number} subSolarLatDeg - Subsolar point latitude
   * @param {number} subSolarLonDeg - Subsolar point longitude
   * @returns {number} Solar elevation angle in degrees (-90 to +90)
   */
  static computeSolarHorizonElevation(latDeg, lonDeg, subSolarLatDeg, subSolarLonDeg) {
    const inc = this.computeSolarIncidenceAngle(latDeg, lonDeg, subSolarLatDeg, subSolarLonDeg);
    const elevation = 90.0 - inc.incidenceAngleDeg;
    return parseFloat(elevation.toFixed(2));
  }

  // --- Line-of-Sight Horizon, Solar Phase Angle & Ray-Ellipsoid Solvers ---

  /**
   * Calculate geometric line-of-sight horizon distance and maximum intervisibility range.
   * d = sqrt(2 * R * h + h^2)
   * @param {number} h1Km - Height of observer 1 above datum in km (e.g. rover mast or orbital camera)
   * @param {number} [h2Km=0] - Height of target 2 above datum in km (e.g. relay orbiter or lander)
   * @param {string} [body='mars'] - Planetary body
   * @returns {{horizonDist1Km: number, horizonDist2Km: number, maxIntervisibleDistKm: number}}
   */
  static computeLineOfSightHorizon(h1Km, h2Km = 0, body = 'mars') {
    const R = body.toLowerCase() === 'moon' ? 1737.4 : 3389.5;
    const h1 = Math.max(0, h1Km);
    const h2 = Math.max(0, h2Km);

    const d1 = Math.sqrt(2.0 * R * h1 + h1 * h1);
    const d2 = Math.sqrt(2.0 * R * h2 + h2 * h2);
    const totalDist = d1 + d2;

    return {
      horizonDist1Km: parseFloat(d1.toFixed(3)),
      horizonDist2Km: parseFloat(d2.toFixed(3)),
      maxIntervisibleDistKm: parseFloat(totalDist.toFixed(3))
    };
  }

  /**
   * Calculate solar phase angle (alpha) from Sun, Target, and Observer 3D vectors.
   * alpha = arccos(v_sun . v_obs)
   * @param {{x: number, y: number, z: number}} sunPos - Sun position in km
   * @param {{x: number, y: number, z: number}} obsPos - Observer position in km
   * @param {{x: number, y: number, z: number}} [targetPos={x:0, y:0, z:0}] - Planetary target center
   * @returns {{phaseAngleDeg: number, phaseAngleRad: number, illuminationFraction: number}}
   */
  static computeSolarPhaseAngle(sunPos, obsPos, targetPos = { x: 0, y: 0, z: 0 }) {
    const vSun = { x: sunPos.x - targetPos.x, y: sunPos.y - targetPos.y, z: sunPos.z - targetPos.z };
    const vObs = { x: obsPos.x - targetPos.x, y: obsPos.y - targetPos.y, z: obsPos.z - targetPos.z };

    const lenSun = Math.hypot(vSun.x, vSun.y, vSun.z);
    const lenObs = Math.hypot(vObs.x, vObs.y, vObs.z);

    if (lenSun === 0 || lenObs === 0) {
      return { phaseAngleDeg: 0, phaseAngleRad: 0, illuminationFraction: 1.0 };
    }

    const dot = (vSun.x * vObs.x + vSun.y * vObs.y + vSun.z * vObs.z) / (lenSun * lenObs);
    const clampedDot = Math.max(-1.0, Math.min(1.0, dot));
    const phaseRad = Math.acos(clampedDot);
    const phaseDeg = phaseRad * 180.0 / Math.PI;

    // Illumination fraction k = (1 + cos(alpha)) / 2
    const k = (1.0 + clampedDot) / 2.0;

    return {
      phaseAngleDeg: parseFloat(phaseDeg.toFixed(2)),
      phaseAngleRad: parseFloat(phaseRad.toFixed(4)),
      illuminationFraction: parseFloat(k.toFixed(4))
    };
  }

  /**
   * Test 3D ray-ellipsoid intersection quadratic equation for planetary line-of-sight.
   * @param {{x: number, y: number, z: number}} rayOrigin - Ray start position in km
   * @param {{x: number, y: number, z: number}} rayDir - Unit direction vector
   * @param {string} [body='mars'] - Planetary body
   * @returns {{intersects: boolean, tNear: number, tFar: number}}
   */
  static testRayEllipsoidIntersection(rayOrigin, rayDir, body = 'mars') {
    const isMars = body.toLowerCase() === 'mars';
    const a = isMars ? 3396.19 : 1737.4;
    const b = isMars ? 3396.19 : 1737.4;
    const c = isMars ? 3376.20 : 1737.4;

    const ox = rayOrigin.x, oy = rayOrigin.y, oz = rayOrigin.z;
    const dx = rayDir.x, dy = rayDir.y, dz = rayDir.z;

    const A = (dx * dx) / (a * a) + (dy * dy) / (b * b) + (dz * dz) / (c * c);
    const B = 2.0 * ((ox * dx) / (a * a) + (oy * dy) / (b * b) + (oz * dz) / (c * c));
    const C = (ox * ox) / (a * a) + (oy * oy) / (b * b) + (oz * oz) / (c * c) - 1.0;

    const discriminant = B * B - 4.0 * A * C;
    if (discriminant < 0) {
      return { intersects: false, tNear: -1, tFar: -1 };
    }

    const sqrtDisc = Math.sqrt(discriminant);
    const tNear = (-B - sqrtDisc) / (2.0 * A);
    const tFar = (-B + sqrtDisc) / (2.0 * A);

    return {
      intersects: tFar >= 0,
      tNear: parseFloat(tNear.toFixed(3)),
      tFar: parseFloat(tFar.toFixed(3))
    };
  }

  // --- Topographic Self-Shadowing, Horizon Dip & Nodal Precession Solvers ---

  /**
   * Calculate local terrain facet illumination incidence and self-shadowing.
   * cos(i) = sin(h_sun) * cos(beta) + cos(h_sun) * sin(beta) * cos(az_sun - az_aspect)
   * @param {number} slopeDeg - Local terrain slope angle beta (0 = horizontal)
   * @param {number} aspectDeg - Downhill aspect azimuth (0 = North, 90 = East, 180 = South, 270 = West)
   * @param {number} solarElevationDeg - Solar altitude angle h_sun (-90 to +90)
   * @param {number} solarAzimuthDeg - Solar azimuth angle phi_sun (0 to 360)
   * @returns {{isIlluminated: boolean, cosIncidence: number, localIncidenceDeg: number}}
   */
  static computeTopographicSelfShadow(slopeDeg, aspectDeg, solarElevationDeg, solarAzimuthDeg) {
    if (solarElevationDeg <= 0) {
      return { isIlluminated: false, cosIncidence: 0, localIncidenceDeg: 90.0 };
    }

    const beta = slopeDeg * Math.PI / 180.0;
    const hSun = solarElevationDeg * Math.PI / 180.0;
    const dAz = (solarAzimuthDeg - aspectDeg) * Math.PI / 180.0;

    const cosI = Math.sin(hSun) * Math.cos(beta) + Math.cos(hSun) * Math.sin(beta) * Math.cos(dAz);
    const clampedCos = Math.max(-1.0, Math.min(1.0, cosI));
    const incDeg = Math.acos(clampedCos) * 180.0 / Math.PI;

    return {
      isIlluminated: clampedCos > 0,
      cosIncidence: parseFloat(Math.max(0, clampedCos).toFixed(4)),
      localIncidenceDeg: parseFloat(incDeg.toFixed(2))
    };
  }

  /**
   * Calculate astronomical horizon dip (depression) angle for an elevated camera or orbiter.
   * theta_dip = arccos(R / (R + h))
   * @param {number} altitudeKm - Observer height above planetary surface in km
   * @param {string} [body='mars'] - Planetary body
   * @returns {{dipAngleDeg: number, dipAngleRad: number, apparentHorizonArcDeg: number}}
   */
  static computeHorizonDipAngle(altitudeKm, body = 'mars') {
    const R = body.toLowerCase() === 'moon' ? 1737.4 : 3389.5;
    const h = Math.max(0, altitudeKm);

    const cosDip = R / (R + h);
    const dipRad = Math.acos(Math.max(0, Math.min(1.0, cosDip)));
    const dipDeg = dipRad * 180.0 / Math.PI;

    return {
      dipAngleDeg: parseFloat(dipDeg.toFixed(3)),
      dipAngleRad: parseFloat(dipRad.toFixed(5)),
      apparentHorizonArcDeg: parseFloat((dipDeg * 2.0).toFixed(3))
    };
  }

  /**
   * Calculate orbital plane nodal precession rate due to planetary J2 oblateness.
   * dOmega/dt = -1.5 * J2 * (R / p)^2 * n * cos(i)
   * @param {number} [semiMajorAxisKm=3790] - Orbit semi-major axis (e.g. 3790 km for 400 km altitude MRO)
   * @param {number} [eccentricity=0.001] - Orbit eccentricity
   * @param {number} [inclinationDeg=92.8] - Orbit inclination in degrees (e.g. 92.8° for Sun-synchronous MRO)
   * @param {string} [body='mars'] - Planetary body
   * @returns {{precessionDegPerDay: number, isSunSynchronous: boolean}}
   */
  static computeOrbitalPrecessionRate(semiMajorAxisKm = 3790, eccentricity = 0.001, inclinationDeg = 92.8, body = 'mars') {
    const R = body.toLowerCase() === 'moon' ? 1737.4 : 3396.19; // km
    const GM = body.toLowerCase() === 'moon' ? 4902.8 : 42828.37; // km^3/s^2
    const J2 = body.toLowerCase() === 'moon' ? 2.03e-4 : 1.96045e-3; // Mars J2 harmonic

    const a = Math.max(R + 50, semiMajorAxisKm);
    const e = Math.max(0, Math.min(0.9, eccentricity));
    const incRad = inclinationDeg * Math.PI / 180.0;

    const p = a * (1.0 - e * e); // Semi-latus rectum
    const n = Math.sqrt(GM / Math.pow(a, 3)); // Mean motion (rad/s)

    // Precession rate in rad/s
    const dOmega_dt = -1.5 * J2 * Math.pow(R / p, 2) * n * Math.cos(incRad);
    const degPerDay = dOmega_dt * (180.0 / Math.PI) * 86400.0;

    // Mars heliocentric orbital motion is ~0.524 deg/day (360 deg / 686.98 days)
    const isSunSync = Math.abs(degPerDay - 0.524) < 0.05;

    return {
      precessionDegPerDay: parseFloat(degPerDay.toFixed(4)),
      isSunSynchronous: isSunSync
    };
  }

  // --- Ground Swath Width, Triangle Facet Normal & Perspective Camera Solvers ---

  /**
   * Calculate camera or spectrometer ground swath footprint width across track.
   * W = 2 * h * tan(FOV / 2)
   * @param {number} altitudeKm - Spacecraft altitude above surface in km
   * @param {number} [fovDegrees=30.0] - Camera cross-track Field of View in degrees
   * @returns {{swathWidthKm: number, halfSwathWidthKm: number}}
   */
  static computeGroundSwathWidth(altitudeKm, fovDegrees = 30.0) {
    const h = Math.max(0, altitudeKm);
    const halfFovRad = (fovDegrees * Math.PI / 180.0) / 2.0;
    const halfSwath = h * Math.tan(halfFovRad);

    return {
      swathWidthKm: parseFloat((halfSwath * 2.0).toFixed(3)),
      halfSwathWidthKm: parseFloat(halfSwath.toFixed(3))
    };
  }

  /**
   * Calculate unit surface normal vector for a 3D triangle facet [p1, p2, p3].
   * @param {[number, number, number]} p1 - Vertex 1 [x, y, z]
   * @param {[number, number, number]} p2 - Vertex 2 [x, y, z]
   * @param {[number, number, number]} p3 - Vertex 3 [x, y, z]
   * @returns {{nx: number, ny: number, nz: number, area: number}}
   */
  static computeTriangleFacetNormal(p1, p2, p3) {
    const v1 = [p2[0] - p1[0], p2[1] - p1[1], p2[2] - p1[2]];
    const v2 = [p3[0] - p1[0], p3[1] - p1[1], p3[2] - p1[2]];

    // Cross product v1 x v2
    const cx = v1[1] * v2[2] - v1[2] * v2[1];
    const cy = v1[2] * v2[0] - v1[0] * v2[2];
    const cz = v1[0] * v2[1] - v1[1] * v2[0];

    const len = Math.hypot(cx, cy, cz);
    if (len < 1e-12) {
      return { nx: 0, ny: 1, nz: 0, area: 0 };
    }

    return {
      nx: parseFloat((cx / len).toFixed(6)),
      ny: parseFloat((cy / len).toFixed(6)),
      nz: parseFloat((cz / len).toFixed(6)),
      area: parseFloat((len / 2.0).toFixed(6))
    };
  }

  /**
   * Pinhole perspective projection of a 3D world coordinate to 2D normalized screen space.
   * @param {[number, number, number]} point3D - Target [x, y, z] in camera frame
   * @param {number} [focalLength=1.0] - Camera focal length
   * @returns {{screenX: number, screenY: number, inFrontOfCamera: boolean}}
   */
  static computePerspectiveProjection(point3D, focalLength = 1.0) {
    const [x, y, z] = point3D;
    const inFront = z > 0;
    const safeZ = Math.max(1e-4, Math.abs(z));

    const screenX = (focalLength * x) / safeZ;
    const screenY = (focalLength * y) / safeZ;

    return {
      screenX: parseFloat(screenX.toFixed(4)),
      screenY: parseFloat(screenY.toFixed(4)),
      inFrontOfCamera: inFront
    };
  }

  // --- Hapke Photometry, Ground Sampling Distance (GSD) & Camera Look Solvers ---

  /**
   * Calculate Hapke (1981/1993) bidirectional reflectance for particulate planetary surfaces.
   * r = (w / (4*pi)) * (mu0 / (mu0 + mu)) * [ (1 + B(g))*p(g) + H(mu0)*H(mu) - 1 ]
   * @param {number} cosIncidence - Cosine of incidence angle (mu0 = cos(i))
   * @param {number} cosEmission - Cosine of emission angle (mu = cos(e))
   * @param {number} phaseAngleDeg - Solar phase angle in degrees (g)
   * @param {number} [singleScatteringAlbedo=0.25] - Single-scattering albedo w (0.25 for Mars dust)
   * @returns {number} Bidirectional reflectance factor
   */
  static computeHapkePhotometricReflectance(cosIncidence, cosEmission, phaseAngleDeg, singleScatteringAlbedo = 0.25) {
    const mu0 = Math.max(1e-4, cosIncidence);
    const mu = Math.max(1e-4, cosEmission);
    const w = Math.max(0.01, Math.min(0.99, singleScatteringAlbedo));
    const gRad = phaseAngleDeg * Math.PI / 180.0;

    // Opposition surge B(g) ~ B0 / (1 + tan(g/2)/h_surge)
    const B0 = 1.0;
    const hSurge = 0.05;
    const Bg = B0 / (1.0 + Math.tan(gRad / 2.0) / hSurge);

    // Isotropic particle phase function p(g) ~ 1.0
    const pg = 1.0;

    // Chandrasekhar H-functions approximation: H(x) = (1 + 2*x) / (1 + 2*x*sqrt(1 - w))
    const gamma = Math.sqrt(1.0 - w);
    const H_mu0 = (1.0 + 2.0 * mu0) / (1.0 + 2.0 * mu0 * gamma);
    const H_mu = (1.0 + 2.0 * mu) / (1.0 + 2.0 * mu * gamma);

    const term = (1.0 + Bg) * pg + H_mu0 * H_mu - 1.0;
    const r = (w / (4.0 * Math.PI)) * (mu0 / (mu0 + mu)) * term;

    return parseFloat(Math.max(0, r).toFixed(5));
  }

  /**
   * Calculate camera Ground Sampling Distance (GSD) / native pixel resolution in meters/pixel.
   * GSD = (h * p) / f
   * @param {number} altitudeKm - Spacecraft orbital altitude in km (e.g. 250 km)
   * @param {number} [sensorPixelPitchMicrons=6.0] - Physical pixel pitch on detector in µm (e.g. 12 µm for HiRISE)
   * @param {number} [focalLengthMm=500.0] - Camera focal length in mm (e.g. 12000 mm for HiRISE)
   * @returns {{gsdMetersPerPixel: number, gsdCmPerPixel: number}}
   */
  static computePixelGroundResolution(altitudeKm, sensorPixelPitchMicrons = 6.0, focalLengthMm = 500.0) {
    const hM = altitudeKm * 1000.0;
    const pM = sensorPixelPitchMicrons * 1e-6;
    const fM = focalLengthMm * 1e-3;

    const gsdM = (hM * pM) / Math.max(1e-4, fM);
    const gsdCm = gsdM * 100.0;

    return {
      gsdMetersPerPixel: parseFloat(gsdM.toFixed(3)),
      gsdCmPerPixel: parseFloat(gsdCm.toFixed(1))
    };
  }

  /**
   * Calculate 3D unit camera line-of-sight look vector towards surface coordinate.
   * @param {number} latDeg - Target latitude
   * @param {number} lonDeg - Target longitude
   * @param {number} [cameraLatDeg=0] - Camera sub-spacecraft latitude
   * @param {number} [cameraLonDeg=0] - Camera sub-spacecraft longitude
   * @returns {{vx: number, vy: number, vz: number}} Unit line-of-sight direction vector
   */
  static computeCameraLookVector(latDeg, lonDeg, cameraLatDeg = 0, cameraLonDeg = 0) {
    const pTarget = this.geographicToCartesian(latDeg, lonDeg, 0);
    const pCam = this.geographicToCartesian(cameraLatDeg, cameraLonDeg, 300); // 300 km altitude

    const dx = pTarget.x - pCam.x;
    const dy = pTarget.y - pCam.y;
    const dz = pTarget.z - pCam.z;
    const len = Math.hypot(dx, dy, dz);

    return {
      vx: parseFloat((dx / len).toFixed(5)),
      vy: parseFloat((dy / len).toFixed(5)),
      vz: parseFloat((dz / len).toFixed(5))
    };
  }
}







