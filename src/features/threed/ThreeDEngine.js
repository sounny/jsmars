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
    let R = 3389.5;
    if (typeof body === 'string') {
      if (body.toLowerCase() === 'moon') R = 1737.4;
      else if (body.toLowerCase() === 'earth') R = 6371.0;
    } else if (typeof body === 'number' && Number.isFinite(body)) {
      R = Math.max(1.0, body);
    }
    const h = Math.max(0, altitudeKm);

    const cosDip = R / (R + h);
    const dipRad = Math.acos(Math.max(0, Math.min(1.0, cosDip)));
    const dipDeg = dipRad * 180.0 / Math.PI;
    const distKm = Math.sqrt(2.0 * R * h + h * h);

    return {
      dipAngleDeg: parseFloat(dipDeg.toFixed(3)),
      horizonDipAngleDeg: parseFloat(dipDeg.toFixed(3)),
      dipAngleRad: parseFloat(dipRad.toFixed(5)),
      apparentHorizonArcDeg: parseFloat((dipDeg * 2.0).toFixed(3)),
      horizonDistanceKm: parseFloat(distKm.toFixed(2))
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

  // --- Atmospheric Limb Tangent Altitude, Horn DEM Slope & Horizon Culling Solvers ---

  /**
   * Calculate atmospheric limb grazing tangent line-of-sight altitude for limb sounding.
   * h_tangent = (R + h_craft) * sin(offNadirAngle) - R
   * @param {number} [spacecraftAltitudeKm=300] - Spacecraft altitude above surface in km
   * @param {number} [offNadirAngleDeg=65] - Look angle away from nadir in degrees
   * @param {string} [body='mars'] - Planetary body
   * @returns {{tangentAltitudeKm: number, isGrazingAtmosphere: boolean, isHittingGround: boolean}}
   */
  static computeAtmosphericLimbTangentHeight(spacecraftAltitudeKm = 300, offNadirAngleDeg = 65, body = 'mars') {
    const R = body.toLowerCase() === 'moon' ? 1737.4 : 3389.5;
    const rCraft = R + Math.max(0, spacecraftAltitudeKm);
    const thetaRad = offNadirAngleDeg * Math.PI / 180.0;

    const rTangent = rCraft * Math.sin(thetaRad);
    const hTangent = rTangent - R;

    return {
      tangentAltitudeKm: parseFloat(hTangent.toFixed(2)),
      isGrazingAtmosphere: hTangent >= 0 && hTangent <= 150.0,
      isHittingGround: hTangent < 0
    };
  }

  /**
   * Calculate Horn (1981) 4-neighbor / central-difference slope angle and aspect azimuth from DEM grid.
   * @param {number} zTop - Elevation of North neighbor cell (meters)
   * @param {number} zBottom - Elevation of South neighbor cell (meters)
   * @param {number} zLeft - Elevation of West neighbor cell (meters)
   * @param {number} zRight - Elevation of East neighbor cell (meters)
   * @param {number} [cellSizeMeters=100.0] - Grid horizontal cell spacing (dx = dy)
   * @returns {{slopeDeg: number, aspectDeg: number, slopePercent: number}}
   */
  static computeDEMGridSlopeAspect(zTop, zBottom, zLeft, zRight, cellSizeMeters = 100.0) {
    const dx = 2.0 * Math.max(1, cellSizeMeters);
    const dy = 2.0 * Math.max(1, cellSizeMeters);

    const dz_dx = (zRight - zLeft) / dx;
    const dz_dy = (zTop - zBottom) / dy;

    const grad = Math.hypot(dz_dx, dz_dy);
    const slopeRad = Math.atan(grad);
    const slopeDeg = slopeRad * 180.0 / Math.PI;

    // Aspect: compass direction of steepest downhill gradient (0 = North, 90 = East, 180 = South, 270 = West)
    let aspectDeg = Math.atan2(-dz_dx, dz_dy) * 180.0 / Math.PI;
    if (aspectDeg < 0) aspectDeg += 360.0;

    return {
      slopeDeg: parseFloat(slopeDeg.toFixed(2)),
      aspectDeg: parseFloat(aspectDeg.toFixed(1)),
      slopePercent: parseFloat((grad * 100.0).toFixed(2))
    };
  }

  /**
   * Test whether a 3D surface feature is occluded below the planetary horizon.
   * @param {number} cameraAltitudeKm - Observer camera altitude in km
   * @param {number} featureAltitudeKm - Feature elevation in km
   * @param {number} angularSeparationDeg - Great-circle angular distance between camera and feature
   * @param {string} [body='mars'] - Planetary body
   * @returns {{isOccluded: boolean, maxVisibleAngularArcDeg: number}}
   */
  static computeHorizonOcclusionCulling(cameraAltitudeKm, featureAltitudeKm, angularSeparationDeg, body = 'mars') {
    const R = body.toLowerCase() === 'moon' ? 1737.4 : 3389.5;
    const hCam = Math.max(0, cameraAltitudeKm);
    const hFeat = Math.max(0, featureAltitudeKm);

    // Horizon angular distances
    const arcCamRad = Math.acos(R / (R + hCam));
    const arcFeatRad = Math.acos(R / (R + hFeat));

    const maxArcRad = arcCamRad + arcFeatRad;
    const maxArcDeg = maxArcRad * 180.0 / Math.PI;

    const isOccluded = angularSeparationDeg > maxArcDeg;

    return {
      isOccluded,
      maxVisibleAngularArcDeg: parseFloat(maxArcDeg.toFixed(3))
    };
  }

  // --- Rectilinear Ground Footprint, Planetary Dip Horizon & Surface Radiance Solvers ---

  /**
   * Calculate 2D rectangular camera sensor ground swath footprint dimensions.
   * W_x = 2 * H * tan(FOV_x / 2),  W_y = 2 * H * tan(FOV_y / 2)
   * @param {number} altitudeKm - Spacecraft altitude above datum in km
   * @param {number} [fovHorizontalDeg=20.0] - Horizontal Field of View (degrees)
   * @param {number} [fovVerticalDeg=15.0] - Vertical Field of View (degrees)
   * @returns {{swathWidthXKm: number, swathHeightYKm: number, groundFootprintAreaKm2: number}}
   */
  static computeRectilinearGroundFootprint(altitudeKm, fovHorizontalDeg = 20.0, fovVerticalDeg = 15.0) {
    const H = Math.max(0, altitudeKm);
    const radX = (fovHorizontalDeg * Math.PI / 180.0) / 2.0;
    const radY = (fovVerticalDeg * Math.PI / 180.0) / 2.0;

    const wx = 2.0 * H * Math.tan(radX);
    const wy = 2.0 * H * Math.tan(radY);
    const area = wx * wy;

    return {
      swathWidthXKm: parseFloat(wx.toFixed(3)),
      swathHeightYKm: parseFloat(wy.toFixed(3)),
      groundFootprintAreaKm2: parseFloat(area.toFixed(2))
    };
  }

  /**
   * Calculate planetary horizon dip depression angle and visible spherical cap surface area.
   * theta_dip = arccos(R / (R + H)),  A_cap = 2 * pi * R^2 * (1 - cos(theta_dip))
   * @param {number} altitudeKm - Observer height above surface in km
   * @param {string} [body='mars'] - Planetary body
   * @returns {{dipAngleDeg: number, visibleCapAreaKm2: number, planetFractionPercent: number}}
   */
  static computePlanetaryDipHorizonViewingAngle(altitudeKm, body = 'mars') {
    const R = body.toLowerCase() === 'moon' ? 1737.4 : 3389.5;
    const H = Math.max(0, altitudeKm);

    const cosDip = R / (R + H);
    const dipRad = Math.acos(Math.max(0, Math.min(1.0, cosDip)));
    const dipDeg = dipRad * 180.0 / Math.PI;

    const totalArea = 4.0 * Math.PI * R * R;
    const capArea = 2.0 * Math.PI * R * R * (1.0 - cosDip);
    const fracPct = (capArea / totalArea) * 100.0;

    return {
      dipAngleDeg: parseFloat(dipDeg.toFixed(3)),
      visibleCapAreaKm2: parseFloat(capArea.toFixed(1)),
      planetFractionPercent: parseFloat(fracPct.toFixed(2))
    };
  }

  /**
   * Calculate Lambertian surface reflected radiance in W / (m^2 sr).
   * L = (A * F_sun * cos(i)) / pi
   * @param {number} solarFluxW_M2 - Incident top-of-atmosphere/surface solar flux
   * @param {number} solarIncidenceDeg - Solar incidence angle in degrees
   * @param {number} [surfaceAlbedo=0.25] - Bolometric surface albedo
   * @returns {{radianceW_M2_Sr: number, isIlluminated: boolean}}
   */
  static computeLambertianSurfaceRadiance(solarFluxW_M2, solarIncidenceDeg, surfaceAlbedo = 0.25) {
    const incRad = Math.abs(solarIncidenceDeg) * Math.PI / 180.0;
    const cosI = Math.max(0, Math.cos(incRad));
    const A = Math.max(0, Math.min(1.0, surfaceAlbedo));
    const F0 = Math.max(0, solarFluxW_M2);

    const L = (A * F0 * cosI) / Math.PI;

    return {
      radianceW_M2_Sr: parseFloat(L.toFixed(3)),
      isIlluminated: cosI > 0
    };
  }

  // --- Horn Analytical Hillshade, Perspective GSD & Normal From Slope/Aspect Solvers ---

  /**
   * Calculate Horn (1981) shaded relief hillshade intensity from surface slope and aspect.
   * cos(i) = sin(alt_sun) * cos(slope) + cos(alt_sun) * sin(slope) * cos(az_sun - aspect)
   * @param {number} slopeDeg - Terrain slope in degrees (0 = flat)
   * @param {number} aspectDeg - Downhill aspect direction in degrees (0 = North, 90 = East, 180 = South, 270 = West)
   * @param {number} sunAltitudeDeg - Solar elevation angle above horizon in degrees
   * @param {number} sunAzimuthDeg - Solar azimuth angle in degrees (0 to 360)
   * @param {number} [ambient=0.15] - Ambient lighting factor (0.0 to 1.0)
   * @returns {{hillshadeIntensity: number, cosIncidence: number, isShadowed: boolean}}
   */
  static computeHornHillshadeValue(slopeDeg, aspectDeg, sunAltitudeDeg, sunAzimuthDeg, ambient = 0.15) {
    if (sunAltitudeDeg <= 0) {
      return { hillshadeIntensity: ambient, cosIncidence: 0, isShadowed: true };
    }

    const slopeRad = slopeDeg * Math.PI / 180.0;
    const aspectRad = aspectDeg * Math.PI / 180.0;
    const sunAltRad = sunAltitudeDeg * Math.PI / 180.0;
    const sunAzRad = sunAzimuthDeg * Math.PI / 180.0;

    const cosI = Math.sin(sunAltRad) * Math.cos(slopeRad) +
                 Math.cos(sunAltRad) * Math.sin(slopeRad) * Math.cos(sunAzRad - aspectRad);

    const clampedCos = Math.max(0, cosI);
    const intensity = ambient + (1.0 - ambient) * clampedCos;

    return {
      hillshadeIntensity: parseFloat(intensity.toFixed(4)),
      cosIncidence: parseFloat(clampedCos.toFixed(4)),
      isShadowed: cosI <= 0
    };
  }

  /**
   * Calculate perspective camera Ground Sample Distance (GSD).
   * GSD = (H * pixelPitch) / focalLength
   * @param {number} altitudeKm - Spacecraft or camera altitude in km
   * @param {number} focalLengthMm - Camera optical focal length in mm
   * @param {number} [pixelPitchMicrons=12.0] - Physical pixel pitch on sensor in microns
   * @returns {{gsdMetersPerPixel: number, gsdCmPerPixel: number}}
   */
  static computePerspectiveGSD(altitudeKm, focalLengthMm, pixelPitchMicrons = 12.0) {
    const H = Math.max(0, altitudeKm) * 1000.0; // meters
    const f = Math.max(1e-3, focalLengthMm) * 1e-3; // meters
    const p = Math.max(1e-6, pixelPitchMicrons) * 1e-6; // meters

    const gsdM = (H * p) / f;
    const gsdCm = gsdM * 100.0;

    return {
      gsdMetersPerPixel: parseFloat(gsdM.toFixed(4)),
      gsdCmPerPixel: parseFloat(gsdCm.toFixed(2))
    };
  }

  /**
   * Convert surface slope and aspect angles to a 3D unit surface normal vector.
   * @param {number} slopeDeg - Surface slope in degrees (0 = horizontal)
   * @param {number} aspectDeg - Downhill aspect direction in degrees (0 = North, 90 = East, 180 = South, 270 = West)
   * @returns {{nx: number, ny: number, nz: number}} Unit normal vector where +Y is up, +X is East, +Z is South
   */
  static computeSurfaceNormalFromSlopeAspect(slopeDeg, aspectDeg) {
    const beta = slopeDeg * Math.PI / 180.0;
    const psi = aspectDeg * Math.PI / 180.0;

    const nx = -Math.sin(beta) * Math.sin(psi);
    const ny = Math.cos(beta);
    const nz = -Math.sin(beta) * Math.cos(psi);

    return {
      nx: parseFloat(nx.toFixed(5)),
      ny: parseFloat(ny.toFixed(5)),
      nz: parseFloat(nz.toFixed(5))
    };
  }

  // --- Geometric Horizon Distance, Perspective Swath Width & Diffuse Radiance Solvers ---

  /**
   * Calculate geometric line-of-sight horizon distance and surface arc distance from observer altitude.
   * d = sqrt(2 * R * h + h^2),  s = R * arccos(R / (R + h))
   * @param {number} altitudeMeters - Observer height above planetary datum in meters
   * @param {string} [body='mars'] - Planetary body (default 'mars')
   * @returns {{horizonDistanceKm: number, horizonDistanceMeters: number, surfaceArcKm: number}}
   */
  static computeGeometricHorizonDistance(altitudeMeters, body = 'mars') {
    const R = (body.toLowerCase() === 'moon' ? 1737.4 : 3389.5) * 1000.0; // in meters
    const h = Math.max(0, altitudeMeters);

    const dMeters = Math.sqrt(2.0 * R * h + h * h);
    const dKm = dMeters / 1000.0;

    const cosTheta = R / (R + h);
    const thetaRad = Math.acos(Math.max(0, Math.min(1.0, cosTheta)));
    const arcKm = (R * thetaRad) / 1000.0;

    return {
      horizonDistanceKm: parseFloat(dKm.toFixed(3)),
      horizonDistanceMeters: parseFloat(dMeters.toFixed(1)),
      surfaceArcKm: parseFloat(arcKm.toFixed(3))
    };
  }

  /**
   * Calculate perspective camera ground footprint swath width.
   * W = 2 * H * tan(FOV / 2)
   * @param {number} altitudeKm - Orbital altitude in km
   * @param {number} [fovDegrees=20.0] - Camera full Field of View in degrees
   * @returns {{swathWidthKm: number, swathWidthMeters: number, halfSwathKm: number}}
   */
  static computePerspectiveSwathWidth(altitudeKm, fovDegrees = 20.0) {
    const H = Math.max(0, altitudeKm);
    const halfFovRad = (fovDegrees * Math.PI / 180.0) / 2.0;
    const halfSwathKm = H * Math.tan(halfFovRad);
    const swathKm = halfSwathKm * 2.0;

    return {
      swathWidthKm: parseFloat(swathKm.toFixed(3)),
      swathWidthMeters: parseFloat((swathKm * 1000.0).toFixed(1)),
      halfSwathKm: parseFloat(halfSwathKm.toFixed(3))
    };
  }

  /**
   * Calculate diffuse Lambertian reflected surface radiance.
   * L = (A * F_sun * cos(i)) / pi
   * @param {number} [incidentSolarFluxW_M2=590.0] - Top-of-atmosphere/surface solar irradiance in W/m^2
   * @param {number} [solarIncidenceAngleDeg=45.0] - Solar incidence angle in degrees
   * @param {number} [surfaceAlbedo=0.25] - Surface Lambertian albedo
   * @returns {{reflectedRadianceW_M2_Sr: number, isDirectlyIlluminated: boolean}}
   */
  static computeDiffusePhotometricRadiance(incidentSolarFluxW_M2 = 590.0, solarIncidenceAngleDeg = 45.0, surfaceAlbedo = 0.25) {
    const F0 = Math.max(0, incidentSolarFluxW_M2);
    const incRad = Math.abs(solarIncidenceAngleDeg) * Math.PI / 180.0;
    const cosI = Math.max(0, Math.cos(incRad));
    const A = Math.max(0, Math.min(1.0, surfaceAlbedo));

    const L = (A * F0 * cosI) / Math.PI;

    return {
      reflectedRadianceW_M2_Sr: parseFloat(L.toFixed(4)),
      isDirectlyIlluminated: cosI > 0
    };
  }

  // --- Lommel-Seeliger Scattering, Swath Slant Range & Globe Angular Radius Solvers ---

  /**
   * Calculate Lommel-Seeliger single-scattering radiance factor (I/F) for particulate surfaces.
   * I/F = (w0 / (4 * pi)) * (mu0 / (mu + mu0))
   * @param {number} cosIncidence - Cosine of solar incidence angle (mu0 = cos(i))
   * @param {number} cosEmission - Cosine of camera emission angle (mu = cos(e))
   * @param {number} [singleScatteringAlbedo=0.25] - Single-scattering albedo w0
   * @returns {{radianceFactorIoF: number, isDirectlyIlluminated: boolean}}
   */
  static computeLommelSeeligerScattering(cosIncidence, cosEmission, singleScatteringAlbedo = 0.25) {
    const mu0 = Math.max(0, cosIncidence);
    const mu = Math.max(0, cosEmission);
    const w0 = Math.max(0.001, Math.min(1.0, singleScatteringAlbedo));

    if (mu0 + mu <= 0) {
      return { radianceFactorIoF: 0, isDirectlyIlluminated: false };
    }

    const iof = (w0 / (4.0 * Math.PI)) * (mu0 / (mu + mu0));

    return {
      radianceFactorIoF: parseFloat(iof.toFixed(5)),
      isDirectlyIlluminated: mu0 > 0
    };
  }

  /**
   * Calculate perspective camera ground swath edge slant range.
   * R_slant = H / cos(FOV / 2)
   * @param {number} altitudeKm - Orbital altitude in km
   * @param {number} [fovDegrees=20.0] - Full camera Field of View in degrees
   * @returns {{slantRangeKm: number, slantRangeMeters: number, rangeExpansionRatio: number}}
   */
  static computePerspectiveSwathSlantRange(altitudeKm, fovDegrees = 20.0) {
    const H = Math.max(0.001, altitudeKm);
    const halfFovRad = (fovDegrees * Math.PI / 180.0) / 2.0;
    const cosHalfFov = Math.max(0.001, Math.cos(halfFovRad));

    const rSlantKm = H / cosHalfFov;
    const ratio = rSlantKm / H;

    return {
      slantRangeKm: parseFloat(rSlantKm.toFixed(3)),
      slantRangeMeters: parseFloat((rSlantKm * 1000.0).toFixed(1)),
      rangeExpansionRatio: parseFloat(ratio.toFixed(4))
    };
  }

  /**
   * Calculate apparent angular radius of planetary globe viewed from orbit.
   * theta_globe = arcsin( R / (R + h) )
   * @param {number} altitudeKm - Spacecraft altitude above datum in km
   * @param {string} [body='mars'] - Target planetary body
   * @returns {{angularRadiusDeg: number, angularRadiusRad: number, apparentDiameterDeg: number}}
   */
  static computeApparentGlobeAngularRadius(altitudeKm, body = 'mars') {
    const R = body.toLowerCase() === 'moon' ? 1737.4 : 3389.5;
    const h = Math.max(0, altitudeKm);

    const sinTheta = R / (R + h);
    const thetaRad = Math.asin(Math.max(0, Math.min(1.0, sinTheta)));
    const thetaDeg = thetaRad * 180.0 / Math.PI;

    return {
      angularRadiusDeg: parseFloat(thetaDeg.toFixed(3)),
      angularRadiusRad: parseFloat(thetaRad.toFixed(5)),
      apparentDiameterDeg: parseFloat((thetaDeg * 2.0).toFixed(3))
    };
  }

  // --- Hapke Opposition Surge, Footprint Bounds & Horizon Depression Solvers ---

  /**
   * Calculate Hapke Shadow-Hiding Opposition Surge (SHOS) parameter.
   * B_SH(g) = B0 / (1 + tan(g / 2) / h_s)
   * @param {number} phaseAngleDeg - Solar phase angle in degrees (g)
   * @param {number} [amplitudeB0=1.0] - Opposition surge amplitude parameter
   * @param {number} [widthParamHs=0.05] - Angular half-width parameter h_s (~0.05 for Mars regolith)
   * @returns {{oppositionSurgeFactor: number, surgeEnhancementPercent: number}}
   */
  static computeHapkeShadowHidingSurge(phaseAngleDeg, amplitudeB0 = 1.0, widthParamHs = 0.05) {
    const gRad = Math.abs(phaseAngleDeg) * Math.PI / 180.0;
    const hs = Math.max(1e-4, widthParamHs);
    const b0 = Math.max(0, amplitudeB0);

    const bSh = b0 / (1.0 + Math.tan(gRad / 2.0) / hs);
    const pct = bSh * 100.0;

    return {
      oppositionSurgeFactor: parseFloat(bSh.toFixed(4)),
      surgeEnhancementPercent: parseFloat(pct.toFixed(2))
    };
  }

  /**
   * Calculate rectangular camera ground swath footprint vertices relative to sub-satellite nadir.
   * @param {number} altitudeKm - Spacecraft altitude above datum in km
   * @param {number} [fovHorizontalDeg=20.0] - Horizontal cross-track FOV in degrees
   * @param {number} [fovVerticalDeg=15.0] - Vertical along-track FOV in degrees
   * @returns {{widthKm: number, lengthKm: number, areaKm2: number, cornersKm: Array<[number, number]>}}
   */
  static computeOrbitalGroundFootprintPolygon(altitudeKm, fovHorizontalDeg = 20.0, fovVerticalDeg = 15.0) {
    const H = Math.max(0, altitudeKm);
    const halfFovX = (fovHorizontalDeg * Math.PI / 180.0) / 2.0;
    const halfFovY = (fovVerticalDeg * Math.PI / 180.0) / 2.0;

    const halfW = H * Math.tan(halfFovX);
    const halfL = H * Math.tan(halfFovY);

    const widthKm = halfW * 2.0;
    const lengthKm = halfL * 2.0;
    const areaKm2 = widthKm * lengthKm;

    const corners = [
      [-halfW, -halfL],
      [halfW, -halfL],
      [halfW, halfL],
      [-halfW, halfL]
    ];

    return {
      widthKm: parseFloat(widthKm.toFixed(3)),
      lengthKm: parseFloat(lengthKm.toFixed(3)),
      areaKm2: parseFloat(areaKm2.toFixed(2)),
      cornersKm: corners.map(([x, y]) => [parseFloat(x.toFixed(3)), parseFloat(y.toFixed(3))])
    };
  }

  /**
   * Calculate orbital horizon depression / dip angle.
   * theta_dep = arccos( R / (R + H) )
   * @param {number} spacecraftAltitudeKm - Orbital altitude above surface in km
   * @param {string} [body='mars'] - Planetary body
   * @returns {{depressionAngleDeg: number, depressionAngleRad: number}}
   */
  static computeHorizonDepressionAngle(spacecraftAltitudeKm, body = 'mars') {
    const R = body.toLowerCase() === 'moon' ? 1737.4 : 3389.5;
    const H = Math.max(0, spacecraftAltitudeKm);

    const cosDip = R / (R + H);
    const dipRad = Math.acos(Math.max(0, Math.min(1.0, cosDip)));
    const dipDeg = dipRad * 180.0 / Math.PI;

    return {
      depressionAngleDeg: parseFloat(dipDeg.toFixed(3)),
      depressionAngleRad: parseFloat(dipRad.toFixed(5))
    };
  }

  // --- Lommel-Seeliger Reflectance, Camera GSD & FOV Angles Solvers ---

  /**
   * Calculate Lommel-Seeliger diffuse surface reflectance for planetary regolith.
   * r_LS = mu0 / (mu0 + mu)
   * @param {number} cosIncidence - Cosine of incidence angle (mu0 = cos(i))
   * @param {number} cosEmission - Cosine of emission angle (mu = cos(e))
   * @returns {{lommelSeeligerReflectance: number, isIlluminated: boolean}}
   */
  static computeLommelSeeligerDiskReflectance(cosIncidence, cosEmission) {
    const mu0 = Math.max(0, cosIncidence);
    const mu = Math.max(0, cosEmission);

    if (mu0 + mu <= 1e-7) {
      return { lommelSeeligerReflectance: 0.0, isIlluminated: false };
    }

    const rLS = mu0 / (mu0 + mu);

    return {
      lommelSeeligerReflectance: parseFloat(rLS.toFixed(4)),
      isIlluminated: mu0 > 0
    };
  }

  /**
   * Calculate camera native Ground Sampling Distance (GSD) at sub-spacecraft nadir.
   * GSD = (H * p) / f
   * @param {number} altitudeKm - Spacecraft altitude above surface in km
   * @param {number} [pixelPitchUm=12.0] - Detector physical pixel pitch in microns (e.g. 12 µm for HiRISE)
   * @param {number} [focalLengthMm=12000.0] - Telescope focal length in mm (e.g. 12,000 mm for HiRISE)
   * @returns {{gsdMeters: number, gsdCm: number}}
   */
  static computeCameraGroundSamplingDistance(altitudeKm, pixelPitchUm = 12.0, focalLengthMm = 12000.0) {
    const H = Math.max(0, altitudeKm * 1000.0);
    const p = Math.max(1e-9, pixelPitchUm * 1e-6);
    const f = Math.max(1e-6, focalLengthMm * 1e-3);

    const gsdM = (H * p) / f;
    const gsdCm = gsdM * 100.0;

    return {
      gsdMeters: parseFloat(gsdM.toFixed(4)),
      gsdCm: parseFloat(gsdCm.toFixed(2))
    };
  }

  /**
   * Calculate camera horizontal, vertical, and diagonal Field of View (FOV) angles from sensor dimensions.
   * FOV = 2 * arctan(dimension / (2 * f))
   * @param {number} sensorWidthMm - Detector width in mm
   * @param {number} sensorHeightMm - Detector height in mm
   * @param {number} focalLengthMm - Telescope focal length in mm
   * @returns {{horizontalFovDeg: number, verticalFovDeg: number, diagonalFovDeg: number}}
   */
  static computeSensorFieldOfViewAngles(sensorWidthMm, sensorHeightMm, focalLengthMm) {
    const w = Math.max(0.1, sensorWidthMm);
    const h = Math.max(0.1, sensorHeightMm);
    const f = Math.max(0.1, focalLengthMm);

    const diag = Math.hypot(w, h);

    const fovHRad = 2.0 * Math.atan2(w, 2.0 * f);
    const fovVRad = 2.0 * Math.atan2(h, 2.0 * f);
    const fovDRad = 2.0 * Math.atan2(diag, 2.0 * f);

    return {
      horizontalFovDeg: parseFloat((fovHRad * 180.0 / Math.PI).toFixed(3)),
      verticalFovDeg: parseFloat((fovVRad * 180.0 / Math.PI).toFixed(3)),
      diagonalFovDeg: parseFloat((fovDRad * 180.0 / Math.PI).toFixed(3))
    };
  }

  // --- 3D Ellipsoid Cartesian, Ray-Ellipsoid Picking & Horizon Solvers ---

  /**
   * Calculate 3D Cartesian coordinates (X, Y, Z) on a triaxial/oblate planetary ellipsoid with topography offset.
   * X = (a + h) * cos(phi) * cos(lambda)
   * Y = (b + h) * cos(phi) * sin(lambda)
   * Z = (c + h) * sin(phi)
   * @param {number} latDeg - Planetocentric latitude in degrees
   * @param {number} lonDeg - East Longitude in degrees
   * @param {number} [altitudeMeters=0] - Topography elevation offset in meters
   * @param {number} [aKm=3396.19] - Semi-major equatorial X-axis radius in km (Mars IAU ~ 3396.19 km)
   * @param {number} [bKm=3396.19] - Semi-major equatorial Y-axis radius in km
   * @param {number} [cKm=3376.20] - Polar Z-axis radius in km (Mars IAU ~ 3376.20 km)
   * @returns {{xKm: number, yKm: number, zKm: number, radiusKm: number}}
   */
  static computeTriaxialEllipsoidCartesian3D(latDeg, lonDeg, altitudeMeters = 0, aKm = 3396.19, bKm = 3396.19, cKm = 3376.20) {
    const phiRad = (latDeg * Math.PI) / 180.0;
    const lamRad = (lonDeg * Math.PI) / 180.0;
    const hKm = altitudeMeters / 1000.0;

    const a = Math.max(1.0, aKm) + hKm;
    const b = Math.max(1.0, bKm) + hKm;
    const c = Math.max(1.0, cKm) + hKm;

    const x = a * Math.cos(phiRad) * Math.cos(lamRad);
    const y = b * Math.cos(phiRad) * Math.sin(lamRad);
    const z = c * Math.sin(phiRad);
    const r = Math.hypot(x, y, z);

    return {
      xKm: parseFloat(x.toFixed(3)),
      yKm: parseFloat(y.toFixed(3)),
      zKm: parseFloat(z.toFixed(3)),
      radiusKm: parseFloat(r.toFixed(3))
    };
  }

  /**
   * Calculate exact 3D ray-ellipsoid intersection for 3D mouse picking and camera line-of-sight targeting.
   * Solves: (Ox + t*Dx)^2/a^2 + (Oy + t*Dy)^2/a^2 + (Oz + t*Dz)^2/c^2 = 1
   * @param {{x: number, y: number, z: number}} rayOrigin - Ray start position in km (e.g. camera eye)
   * @param {{x: number, y: number, z: number}} rayDirection - Normalized ray direction vector
   * @param {number} [aKm=3396.19] - Equatorial radius in km
   * @param {number} [cKm=3376.20] - Polar radius in km
   * @returns {{hasHit: boolean, hitDistanceKm: number, hitPoint: {x: number, y: number, z: number}|null}}
   */
  static computeRayEllipsoidIntersection(rayOrigin, rayDirection, aKm = 3396.19, cKm = 3376.20) {
    const Ox = rayOrigin.x ?? 0;
    const Oy = rayOrigin.y ?? 0;
    const Oz = rayOrigin.z ?? 0;

    const Dx = rayDirection.x ?? 0;
    const Dy = rayDirection.y ?? 0;
    const Dz = rayDirection.z ?? 0;

    const a2 = aKm * aKm;
    const c2 = cKm * cKm;

    // Quadratic coefficients: A*t^2 + B*t + C = 0
    const A = ((Dx * Dx + Dy * Dy) / a2) + ((Dz * Dz) / c2);
    const B = 2.0 * (((Ox * Dx + Oy * Dy) / a2) + ((Oz * Dz) / c2));
    const C = ((Ox * Ox + Oy * Oy) / a2) + ((Oz * Oz) / c2) - 1.0;

    const disc = B * B - 4.0 * A * C;
    if (disc < 0 || A <= 1e-12) {
      return { hasHit: false, hitDistanceKm: 0, hitPoint: null };
    }

    const sqrtDisc = Math.sqrt(disc);
    const t1 = (-B - sqrtDisc) / (2.0 * A);
    const t2 = (-B + sqrtDisc) / (2.0 * A);

    let t = t1 > 0 ? t1 : (t2 > 0 ? t2 : -1);
    if (t < 0) {
      return { hasHit: false, hitDistanceKm: 0, hitPoint: null };
    }

    const hx = Ox + t * Dx;
    const hy = Oy + t * Dy;
    const hz = Oz + t * Dz;

    return {
      hasHit: true,
      hitDistanceKm: parseFloat(t.toFixed(3)),
      hitPoint: {
        x: parseFloat(hx.toFixed(3)),
        y: parseFloat(hy.toFixed(3)),
        z: parseFloat(hz.toFixed(3))
      }
    };
  }

  // --- 3D Camera Footprint, Parallax Relief & ENU Normal Vector Solvers ---

  /**
   * Calculate camera ground footprint dimensions (width, height, area) on tangent surface.
   * W = 2 * h * tan( fov_h / 2 ),  H = 2 * h * tan( fov_v / 2 )
   * @param {number} altitudeKm - Observer/spacecraft altitude above surface in km
   * @param {number} fovHorizontalDeg - Horizontal field-of-view in degrees
   * @param {number} [fovVerticalDeg=fovHorizontalDeg] - Vertical field-of-view in degrees
   * @returns {{footprintWidthKm: number, footprintHeightKm: number, groundAreaKm2: number}}
   */
  static computeCameraGroundFootprint(altitudeKm, fovHorizontalDeg, fovVerticalDeg = fovHorizontalDeg) {
    const h = Math.max(0.01, altitudeKm);
    const fovHRad = (fovHorizontalDeg * Math.PI) / 180.0;
    const fovVRad = (fovVerticalDeg * Math.PI) / 180.0;

    const wKm = 2.0 * h * Math.tan(fovHRad / 2.0);
    const hKm = 2.0 * h * Math.tan(fovVRad / 2.0);
    const areaKm2 = wKm * hKm;

    return {
      footprintWidthKm: parseFloat(wKm.toFixed(3)),
      footprintHeightKm: parseFloat(hKm.toFixed(3)),
      groundAreaKm2: parseFloat(areaKm2.toFixed(3))
    };
  }

  /**
   * Calculate geometric parallax relief displacement Delta_r for elevated topographic peaks.
   * Delta_r = h_relief * tan( theta_look )
   * @param {number} featureElevationMeters - Peak or crater rim elevation above base plane in meters
   * @param {number} lookAngleOffNadirDeg - Camera look angle off-nadir in degrees (0 to 60)
   * @returns {{parallaxDisplacementMeters: number, displacementRatio: number}}
   */
  static computeParallaxReliefDisplacement(featureElevationMeters, lookAngleOffNadirDeg) {
    const h = Math.max(0, featureElevationMeters);
    const thetaRad = (Math.min(85.0, Math.max(0, lookAngleOffNadirDeg)) * Math.PI) / 180.0;

    const tanTheta = Math.tan(thetaRad);
    const dr = h * tanTheta;

    return {
      parallaxDisplacementMeters: parseFloat(dr.toFixed(2)),
      displacementRatio: parseFloat(tanTheta.toFixed(4))
    };
  }

  /**
   * Calculate 3D unit surface normal vector in Topocentric East-North-Up (ENU) coordinates.
   * n_east = -sin(s) * sin(a),  n_north = -sin(s) * cos(a),  n_up = cos(s)
   * @param {number} slopeDeg - Surface slope in degrees (0 = horizontal, 90 = vertical cliff)
   * @param {number} aspectDeg - Azimuth of downhill slope direction (0 = North, 90 = East, 180 = South, 270 = West)
   * @returns {{nEast: number, nNorth: number, nUp: number, isFlat: boolean}}
   */
  static computeTerrainNormalUnitVector3D(slopeDeg, aspectDeg) {
    const sRad = (slopeDeg * Math.PI) / 180.0;
    const aRad = (aspectDeg * Math.PI) / 180.0;

    const sinS = Math.sin(sRad);
    const cosS = Math.cos(sRad);

    const nEast = -sinS * Math.sin(aRad);
    const nNorth = -sinS * Math.cos(aRad);
    const nUp = cosS;

    return {
      nEast: parseFloat(nEast.toFixed(4)),
      nNorth: parseFloat(nNorth.toFixed(4)),
      nUp: parseFloat(nUp.toFixed(4)),
      isFlat: slopeDeg < 0.1
    };
  }

  /**
   * Invert 3D Topocentric East-North-Up (ENU) normal vector back into topographic slope and aspect.
   * s = acos( n_up ),  a = atan2( -n_east, -n_north )
   * @param {number} nEast - East component of normal vector
   * @param {number} nNorth - North component of normal vector
   * @param {number} nUp - Up component of normal vector
   * @returns {{slopeDeg: number, aspectDeg: number, isFlat: boolean}}
   */
  static computeSlopeAndAspectFromNormalVector(nEast, nNorth, nUp) {
    const len = Math.sqrt(nEast * nEast + nNorth * nNorth + nUp * nUp);
    if (len <= 1e-8) {
      return { slopeDeg: 0.0, aspectDeg: 0.0, isFlat: true };
    }

    const normUp = Math.max(-1.0, Math.min(1.0, nUp / len));
    const sRad = Math.acos(normUp);
    const slopeDeg = (sRad * 180.0) / Math.PI;

    if (slopeDeg < 0.01) {
      return { slopeDeg: 0.0, aspectDeg: 0.0, isFlat: true };
    }

    let aRad = Math.atan2(-nEast, -nNorth);
    if (aRad < 0) aRad += 2.0 * Math.PI;
    const aspectDeg = (aRad * 180.0) / Math.PI;

    return {
      slopeDeg: parseFloat(slopeDeg.toFixed(3)),
      aspectDeg: parseFloat(aspectDeg.toFixed(3)),
      isFlat: false
    };
  }

  /**
   * Calculate 3D topographic relief surface area inflation factor f_area = sec(s) = 1 / cos(s).
   * @param {number} slopeDeg - Surface slope in degrees
   * @returns {{areaInflationFactor: number, trueSurfaceAreaKm2: Function}}
   */
  static computeTopographicAreaCorrectionFactor(slopeDeg) {
    const s = Math.max(0.0, Math.min(88.0, slopeDeg));
    const sRad = (s * Math.PI) / 180.0;
    const factor = 1.0 / Math.cos(sRad);

    return {
      areaInflationFactor: parseFloat(factor.toFixed(4)),
      slopeDeg: parseFloat(s.toFixed(2))
    };
  }

  /**
   * Calculate local solar illumination incidence angle cosine (cos i) on tilted 3D terrain.
   * cos(i) = n_east * sin(Z)*sin(A) + n_north * sin(Z)*cos(A) + n_up * cos(Z)
   * @param {number} nEast - Surface normal East component
   * @param {number} nNorth - Surface normal North component
   * @param {number} nUp - Surface normal Up component
   * @param {number} solarZenithDeg - Solar zenith angle in degrees (0 = overhead, 90 = horizon)
   * @param {number} solarAzimuthDeg - Solar azimuth angle in degrees (0 = North, 90 = East, 180 = South, 270 = West)
   * @returns {{cosIncidence: number, incidenceAngleDeg: number, isIlluminated: boolean}}
   */
  static computeSolarIncidenceCosineFromNormal(nEast, nNorth, nUp, solarZenithDeg, solarAzimuthDeg) {
    const zRad = (solarZenithDeg * Math.PI) / 180.0;
    const aRad = (solarAzimuthDeg * Math.PI) / 180.0;

    const sEast = Math.sin(zRad) * Math.sin(aRad);
    const sNorth = Math.sin(zRad) * Math.cos(aRad);
    const sUp = Math.cos(zRad);

    const cosI = nEast * sEast + nNorth * sNorth + nUp * sUp;
    const clampedCos = Math.max(-1.0, Math.min(1.0, cosI));
    const iDeg = (Math.acos(clampedCos) * 180.0) / Math.PI;

    return {
      cosIncidence: parseFloat(clampedCos.toFixed(4)),
      incidenceAngleDeg: parseFloat(iDeg.toFixed(2)),
      isIlluminated: clampedCos > 0
    };
  }

  // --- Stereo Photogrammetry & Lambertian Radiance Solvers ---

  /**
   * Calculate stereo camera Base-to-Height ratio (B/H), convergence angle, and vertical elevation precision.
   * B/H = | tan(theta1) - tan(theta2) |
   * sigma_z = (H / B) * GSD * sigma_px
   * @param {number} lookAngle1Deg - Look angle of image 1 in degrees (signed: negative for backward/left, positive for forward/right)
   * @param {number} lookAngle2Deg - Look angle of image 2 in degrees
   * @param {number} [gsdMeters=0.25] - Ground Sample Distance in meters (e.g. 0.25 m for HiRISE)
   * @param {number} [subpixelPrecision=0.2] - Stereo matching subpixel precision in pixels (typically 0.2 px)
   * @returns {{baseToHeightRatio: number, convergenceAngleDeg: number, heightPrecisionMeters: number, isGoodStereoGeometry: boolean}}
   */
  static computeStereoParallaxBaseToHeightRatio(lookAngle1Deg, lookAngle2Deg, gsdMeters = 0.25, subpixelPrecision = 0.2) {
    const th1Rad = (lookAngle1Deg * Math.PI) / 180.0;
    const th2Rad = (lookAngle2Deg * Math.PI) / 180.0;

    const tan1 = Math.tan(th1Rad);
    const tan2 = Math.tan(th2Rad);

    const bh = Math.abs(tan1 - tan2);
    const convAngle = Math.abs(lookAngle1Deg - lookAngle2Deg);

    const gsd = Math.max(1e-4, gsdMeters);
    const spx = Math.max(0.01, subpixelPrecision);

    // Height precision sigma_z = (1 / (B/H)) * GSD * sigma_px
    const sigmaZ = bh > 0.01 ? (1.0 / bh) * gsd * spx : 999.9;

    return {
      baseToHeightRatio: parseFloat(bh.toFixed(4)),
      convergenceAngleDeg: parseFloat(convAngle.toFixed(2)),
      heightPrecisionMeters: parseFloat(sigmaZ.toFixed(3)),
      isGoodStereoGeometry: bh >= 0.2 && bh <= 1.2
    };
  }

  /**
   * Calculate standard Lambertian diffuse reflectance and ambient-corrected shading intensity (0 - 1).
   * R = ambient + (1 - ambient) * albedo * max(0, cos i)
   * @param {number} cosIncidence - Cosine of solar illumination incidence angle (cos i)
   * @param {number} [albedo=0.25] - Planetary surface albedo (0 to 1)
   * @param {number} [ambient=0.05] - Diffuse ambient sky background illumination
   * @returns {{radianceFactor: number, isDirectlyIlluminated: boolean}}
   */
  static computeLambertianReflectanceAndShading(cosIncidence, albedo = 0.25, ambient = 0.05) {
    const cosI = Math.max(0.0, Math.min(1.0, cosIncidence));
    const A = Math.max(0.0, Math.min(1.0, albedo));
    const amb = Math.max(0.0, Math.min(0.5, ambient));

    const r = amb + (1.0 - amb) * A * cosI;

    return {
      radianceFactor: parseFloat(r.toFixed(4)),
      isDirectlyIlluminated: cosIncidence > 0
    };
  }

  // --- Hapke Photometry & Opposition Surge Solvers ---

  /**
   * Calculate solar illumination phase angle (alpha) from incidence, emission, and azimuth difference angles.
   * cos(alpha) = cos(i) * cos(e) + sin(i) * sin(e) * cos(delta_phi)
   * @param {number} incidenceDeg - Solar incidence angle in degrees
   * @param {number} emissionDeg - Spacecraft emission / viewing angle in degrees
   * @param {number} [azimuthDiffDeg=0.0] - Azimuth difference angle between sun and observer in degrees
   * @returns {{phaseAngleDeg: number, cosPhaseAngle: number}}
   */
  static computePhaseAngleFromAngles(incidenceDeg, emissionDeg, azimuthDiffDeg = 0.0) {
    const iRad = (incidenceDeg * Math.PI) / 180.0;
    const eRad = (emissionDeg * Math.PI) / 180.0;
    const dPhiRad = (azimuthDiffDeg * Math.PI) / 180.0;

    const cosAlpha = Math.cos(iRad) * Math.cos(eRad) + Math.sin(iRad) * Math.sin(eRad) * Math.cos(dPhiRad);
    const clampedCos = Math.max(-1.0, Math.min(1.0, cosAlpha));
    const alphaRad = Math.acos(clampedCos);
    const alphaDeg = (alphaRad * 180.0) / Math.PI;

    return {
      phaseAngleDeg: parseFloat(alphaDeg.toFixed(3)),
      cosPhaseAngle: parseFloat(clampedCos.toFixed(5))
    };
  }

  /**
   * Calculate Hapke shadow-hiding opposition surge enhancement factor B(g).
   * B(g) = 1.0 + B_0 / ( 1.0 + (1 / h) * tan(g / 2) )
   * @param {number} phaseAngleDeg - Solar phase angle in degrees
   * @param {number} [amplitudeB0=1.0] - Opposition surge amplitude parameter B_0 (0 to 1)
   * @param {number} [widthH=0.05] - Angular width parameter h (typically 0.02 - 0.08 for lunar/martian regolith)
   * @returns {{oppositionSurgeFactor: number, isOppositionSpike: boolean}}
   */
  static computeHapkeOppositionSurgeFactor(phaseAngleDeg, amplitudeB0 = 1.0, widthH = 0.05) {
    const gDeg = Math.max(0.0, Math.min(180.0, phaseAngleDeg));
    const gRad = (gDeg * Math.PI) / 180.0;
    const B0 = Math.max(0.0, amplitudeB0);
    const h = Math.max(1e-4, widthH);

    const tanHalfG = Math.tan(gRad / 2.0);
    const bg = 1.0 + B0 / (1.0 + (1.0 / h) * tanHalfG);

    return {
      oppositionSurgeFactor: parseFloat(bg.toFixed(4)),
      isOppositionSpike: gDeg <= 5.0
    };
  }

  // --- Hapke Photometric Multiple Scattering & Bidirectional Reflectance ---

  /**
   * Calculate Chandrasekhar / Hapke isotropic multiple-scattering H-function.
   * H(mu, w) = ( 1 + 2*mu ) / ( 1 + 2*mu * sqrt(1 - w) )
   * @param {number} cosAngleMu - Cosine of incidence or emission angle (0 to 1)
   * @param {number} singleScatteringAlbedoW - Single scattering albedo w (0 to 1)
   * @returns {number}
   */
  static computeHapkeMultipleScatteringHFunction(cosAngleMu, singleScatteringAlbedoW) {
    const mu = Math.max(0.0, Math.min(1.0, cosAngleMu));
    const w = Math.max(0.0, Math.min(0.9999, singleScatteringAlbedoW));

    const gamma = Math.sqrt(1.0 - w);
    const H = (1.0 + 2.0 * mu) / (1.0 + 2.0 * mu * gamma);

    return parseFloat(H.toFixed(4));
  }

  /**
   * Calculate Hapke Bidirectional Reflectance Factor (I/F) for planetary surfaces.
   * I/F = ( w / (4 * (mu0 + mu)) ) * [ p(g) * B(g) + H(mu0, w) * H(mu, w) - 1 ]
   * @param {number} incidenceDeg - Solar incidence angle in degrees
   * @param {number} emissionDeg - Observer emission angle in degrees
   * @param {number} phaseAngleDeg - Solar phase angle in degrees
   * @param {number} [singleScatteringAlbedoW=0.5] - Single scattering albedo (0.2 for dark basalt, 0.6 for bright dust)
   * @param {number} [asymmetryXi=-0.2] - Henyey-Greenstein asymmetry factor (-1 backscatter to +1 forward scatter)
   * @param {number} [amplitudeB0=1.0] - Opposition surge amplitude parameter
   * @param {number} [widthH=0.05] - Opposition surge width parameter
   * @returns {{reflectanceIOF: number, singleScatteringPart: number, multipleScatteringPart: number}}
   */
  static computeHapkeBidirectionalReflectance(incidenceDeg, emissionDeg, phaseAngleDeg, singleScatteringAlbedoW = 0.5, asymmetryXi = -0.2, amplitudeB0 = 1.0, widthH = 0.05) {
    const iRad = (Math.min(89.9, Math.max(0.0, incidenceDeg)) * Math.PI) / 180.0;
    const eRad = (Math.min(89.9, Math.max(0.0, emissionDeg)) * Math.PI) / 180.0;
    const gRad = (Math.min(180.0, Math.max(0.0, phaseAngleDeg)) * Math.PI) / 180.0;

    const mu0 = Math.cos(iRad);
    const mu = Math.cos(eRad);
    const w = Math.max(0.01, Math.min(0.9999, singleScatteringAlbedoW));
    const xi = Math.max(-0.99, Math.min(0.99, asymmetryXi));

    // Henyey-Greenstein single-particle phase function p(g)
    const cosG = Math.cos(gRad);
    const denom = Math.pow(1.0 + 2.0 * xi * cosG + xi * xi, 1.5);
    const pg = (1.0 - xi * xi) / Math.max(1e-6, denom);

    // Opposition surge factor B(g)
    const tanHalfG = Math.tan(gRad / 2.0);
    const bg = 1.0 + amplitudeB0 / (1.0 + (1.0 / Math.max(1e-4, widthH)) * tanHalfG);

    // Multiple scattering H-functions
    const H0 = ThreeDEngine.computeHapkeMultipleScatteringHFunction(mu0, w);
    const H = ThreeDEngine.computeHapkeMultipleScatteringHFunction(mu, w);

    const singlePart = pg * bg;
    const multiPart = (H0 * H) - 1.0;

    const iof = (w / (4.0 * (mu0 + mu))) * (singlePart + multiPart);

    return {
      reflectanceIOF: parseFloat(iof.toFixed(5)),
      singleScatteringPart: parseFloat(singlePart.toFixed(4)),
      multipleScatteringPart: parseFloat(multiPart.toFixed(4))
    };
  }

  // --- Lommel-Seeliger & Minnaert Regolith Photometric Solvers ---

  /**
   * Calculate Lommel-Seeliger photometric scattering factor for low-albedo particulate regolith.
   * f_LS = mu_0 / (mu_0 + mu) = cos(i) / (cos(i) + cos(e))
   * @param {number} solarIncidenceDeg - Solar incidence angle i in degrees (0 - 90)
   * @param {number} emissionAngleDeg - Emission / viewing angle e in degrees (0 - 90)
   * @returns {{lommelSeeligerFactor: number, cosIncidence: number, cosEmission: number}}
   */
  static computeLommelSeeligerPhotometry(solarIncidenceDeg, emissionAngleDeg) {
    const iRad = (Math.min(90.0, Math.max(0.0, solarIncidenceDeg)) * Math.PI) / 180.0;
    const eRad = (Math.min(90.0, Math.max(0.0, emissionAngleDeg)) * Math.PI) / 180.0;

    const mu0 = Math.cos(iRad);
    const mu = Math.cos(eRad);

    const denom = mu0 + mu;
    const fLS = denom > 0 ? mu0 / denom : 0.0;

    return {
      lommelSeeligerFactor: parseFloat(fLS.toFixed(4)),
      cosIncidence: parseFloat(mu0.toFixed(4)),
      cosEmission: parseFloat(mu.toFixed(4))
    };
  }

  /**
   * Calculate Minnaert planetary empirical limb-darkening / photometric factor.
   * f_Minnaert = mu_0^k * mu^(k - 1)
   * @param {number} solarIncidenceDeg - Solar incidence angle i in degrees
   * @param {number} emissionAngleDeg - Emission angle e in degrees
   * @param {number} [minnaertExponentK=0.65] - Minnaert limb parameter k (1.0 = Lambert, 0.5 = Lunar/Martian)
   * @returns {{minnaertFactor: number, minnaertExponent: number}}
   */
  static computeMinnaertReflectanceFactor(solarIncidenceDeg, emissionAngleDeg, minnaertExponentK = 0.65) {
    const iRad = (Math.min(89.99, Math.max(0.0, solarIncidenceDeg)) * Math.PI) / 180.0;
    const eRad = (Math.min(89.99, Math.max(0.0, emissionAngleDeg)) * Math.PI) / 180.0;

    const mu0 = Math.cos(iRad);
    const mu = Math.cos(eRad);
    const k = Math.max(0.01, minnaertExponentK);

    const fMinnaert = Math.pow(mu0, k) * Math.pow(mu, k - 1.0);

    return {
      minnaertFactor: parseFloat(fMinnaert.toFixed(4)),
      minnaertExponent: parseFloat(k.toFixed(3))
    };
  }

  // --- Hapke Regolith Photometry & Opposition Surge Solvers ---

  /**
   * Calculate Henyey-Greenstein single particle scattering phase function p(g) (Hapke 1981, 1993).
   * p(g) = (1 - xi^2) / (1 + 2*xi*cos(g) + xi^2)^(1.5)
   * @param {number} phaseAngleDeg - Phase angle g (sun-target-observer) in degrees (0 to 180)
   * @param {number} [asymmetryParam=-0.25] - Asymmetry factor xi (-1 for backscattering, +1 for forward scattering)
   * @returns {{phaseFunctionValue: number, asymmetryParam: number, isBackscattering: boolean}}
   */
  static computeHapkeSingleParticlePhaseFunction(phaseAngleDeg, asymmetryParam = -0.25) {
    const gRad = (Math.min(180.0, Math.max(0.0, phaseAngleDeg)) * Math.PI) / 180.0;
    const xi = Math.min(0.99, Math.max(-0.99, asymmetryParam));

    const num = 1.0 - xi * xi;
    const den = Math.pow(1.0 + 2.0 * xi * Math.cos(gRad) + xi * xi, 1.5);
    const pG = den > 0 ? num / den : 1.0;

    return {
      phaseFunctionValue: parseFloat(pG.toFixed(4)),
      asymmetryParam: parseFloat(xi.toFixed(3)),
      isBackscattering: xi < 0
    };
  }

  /**
   * Calculate Hapke Shadow-Hiding Opposition Effect (SHOE) surge factor B_SH(g).
   * B_SH(g) = 1.0 + B_0 / ( 1.0 + tan(g / 2) / h_s )
   * @param {number} phaseAngleDeg - Phase angle g in degrees (0 to 180)
   * @param {number} [oppositionAmplitudeB0=1.0] - Opposition amplitude B_0 (typically 1.0)
   * @param {number} [oppositionWidthHs=0.06] - Angular half-width h_s (typically 0.04 to 0.08)
   * @returns {{oppositionSurgeMultiplier: number, phaseAngleDeg: number, isOppositionSpike: boolean}}
   */
  static computeHapkeOppositionSurgeMultiplier(phaseAngleDeg, oppositionAmplitudeB0 = 1.0, oppositionWidthHs = 0.06) {
    const gRad = (Math.min(180.0, Math.max(0.0, phaseAngleDeg)) * Math.PI) / 180.0;
    const b0 = Math.max(0.0, oppositionAmplitudeB0);
    const hs = Math.max(1e-4, oppositionWidthHs);

    const tanHalfG = Math.tan(gRad / 2.0);
    const bSH = 1.0 + (b0 / (1.0 + (tanHalfG / hs)));

    return {
      oppositionSurgeMultiplier: parseFloat(bSH.toFixed(4)),
      phaseAngleDeg: parseFloat(phaseAngleDeg.toFixed(2)),
      isOppositionSpike: phaseAngleDeg <= (2.0 * (Math.atan(hs) * 180.0 / Math.PI))
    };
  }

  /**
   * Calculate Lommel-Seeliger single-scattering photometric reflectance for particulate regolith.
   * I/F = ( w_0 / 4*pi ) * ( mu_0 / (mu_0 + mu) ) * p(g)
   * Reference: Hapke (1981, 1993), McEwen (1991).
   * @param {number} solarIncidenceDeg - Solar incidence angle i (0 - 90 deg)
   * @param {number} emissionAngleDeg - Emission angle e (0 - 90 deg)
   * @param {number} phaseAngleDeg - Phase angle g in degrees
   * @param {number} [singleScatteringAlbedo=0.25] - Single-scattering albedo w_0 (0.12 Moon, 0.25 Mars)
   * @returns {{lommelSeeligerReflectance: number, mu0: number, mu: number}}
   */
  static computeLommelSeeligerLunarReflectance(solarIncidenceDeg, emissionAngleDeg, phaseAngleDeg, singleScatteringAlbedo = 0.25) {
    const iRad = (Math.min(89.99, Math.max(0.0, solarIncidenceDeg)) * Math.PI) / 180.0;
    const eRad = (Math.min(89.99, Math.max(0.0, emissionAngleDeg)) * Math.PI) / 180.0;

    const mu0 = Math.cos(iRad);
    const mu = Math.cos(eRad);
    const w0 = Math.min(1.0, Math.max(0.0, singleScatteringAlbedo));
    const pG = ThreeDEngine.computeHapkeSingleParticlePhaseFunction(phaseAngleDeg).phaseFunctionValue;

    const ls = (w0 / (4.0 * Math.PI)) * (mu0 / (mu0 + mu)) * pG;

    return {
      lommelSeeligerReflectance: parseFloat(ls.toFixed(6)),
      mu0: parseFloat(mu0.toFixed(4)),
      mu: parseFloat(mu.toFixed(4))
    };
  }

  /**
   * Calculate McEwen Lunar-Lambert photometric weighting function.
   * Combines Lommel-Seeliger (rough regolith) and Lambertian (smooth isotropic) laws.
   * f_LL = 2.0 * L(g) * ( mu_0 / (mu_0 + mu) ) + (1.0 - L(g)) * mu_0
   * @param {number} solarIncidenceDeg - Incidence angle i in degrees
   * @param {number} emissionAngleDeg - Emission angle e in degrees
   * @param {number} [lunarWeightL=0.60] - Lunar-Lambert weighting factor L(g) (1.0 = Lommel-Seeliger, 0.0 = Lambert)
   * @returns {{lunarLambertFactor: number, lunarWeight: number, isDominantlyLommelSeeliger: boolean}}
   */
  static computeLunarLambertPhotometricWeighting(solarIncidenceDeg, emissionAngleDeg, lunarWeightL = 0.60) {
    const iRad = (Math.min(89.99, Math.max(0.0, solarIncidenceDeg)) * Math.PI) / 180.0;
    const eRad = (Math.min(89.99, Math.max(0.0, emissionAngleDeg)) * Math.PI) / 180.0;

    const mu0 = Math.cos(iRad);
    const mu = Math.cos(eRad);
    const L = Math.min(1.0, Math.max(0.0, lunarWeightL));

    const fLL = 2.0 * L * (mu0 / (mu0 + mu)) + (1.0 - L) * mu0;

    return {
      lunarLambertFactor: parseFloat(fLL.toFixed(4)),
      lunarWeight: parseFloat(L.toFixed(3)),
      isDominantlyLommelSeeliger: L > 0.5
    };
  }

  /**
   * Calculate Hapke sub-resolution macroscopic surface roughness shadowing correction factor S(i, e, g, theta_bar).
   * Reference: Hapke (1984, 1993), McEwen (1991), Shepard & Campbell (1998).
   * @param {number} solarIncidenceDeg - Solar incidence angle i in degrees (0 to 89.9)
   * @param {number} emissionAngleDeg - Emission angle e in degrees (0 to 89.9)
   * @param {number} phaseAngleDeg - Phase angle g in degrees (0 to 180)
   * @param {number} [meanRoughnessSlopeDeg=20.0] - Mean sub-resolution facet slope angle theta_bar (0 = smooth, 20-30 = rough regolith)
   * @returns {{roughnessCorrectionFactor: number, meanSlopeDeg: number, isRoughSurface: boolean}}
   */
  static computeHapkeRoughnessSurfaceCorrection(solarIncidenceDeg, emissionAngleDeg, phaseAngleDeg, meanRoughnessSlopeDeg = 20.0) {
    const thetaBarDeg = Math.min(60.0, Math.max(0.0, meanRoughnessSlopeDeg));
    if (thetaBarDeg < 0.5) {
      return { roughnessCorrectionFactor: 1.0, meanSlopeDeg: 0.0, isRoughSurface: false };
    }

    const iRad = (Math.min(89.9, Math.max(0.0, solarIncidenceDeg)) * Math.PI) / 180.0;
    const eRad = (Math.min(89.9, Math.max(0.0, emissionAngleDeg)) * Math.PI) / 180.0;
    const gRad = (Math.min(180.0, Math.max(0.0, phaseAngleDeg)) * Math.PI) / 180.0;
    const thRad = (thetaBarDeg * Math.PI) / 180.0;

    const tanTh = Math.tan(thRad);
    const chi = 1.0 / Math.sqrt(1.0 + Math.PI * tanTh * tanTh);

    // Azimuth angle psi between illumination and viewing planes:
    const sinISinE = Math.max(1e-5, Math.sin(iRad) * Math.sin(eRad));
    const cosPsi = Math.max(-1.0, Math.min(1.0, (Math.cos(gRad) - Math.cos(iRad) * Math.cos(eRad)) / sinISinE));
    const psiRad = Math.acos(cosPsi);
    const fPsi = Math.sin(psiRad / 2.0) * Math.sin(psiRad / 2.0);

    // Shadowing correction factor S(i,e,g)
    const mu0 = Math.cos(iRad);
    const mu = Math.cos(eRad);
    const effRatio = (mu0 + mu > 1e-4) ? (2.0 * mu0 * mu) / (mu0 + mu) : 1.0;
    const S = chi / (1.0 - fPsi + fPsi * chi * (1.0 + (1.0 - chi) * (1.0 - effRatio)));

    const clampedS = Math.min(2.0, Math.max(0.1, S));

    return {
      roughnessCorrectionFactor: parseFloat(clampedS.toFixed(4)),
      meanSlopeDeg: parseFloat(thetaBarDeg.toFixed(1)),
      isRoughSurface: thetaBarDeg >= 15.0
    };
  }

  /**
   * Convert latitude and longitude to 3D Cartesian coordinates on a sphere of radius R.
   * @param {number} latDeg - Latitude in degrees (-90 to 90)
   * @param {number} lonDeg - Longitude in degrees (-180 to 180 or 0 to 360)
   * @param {number} [radius=22.0] - Sphere radius
   * @returns {{x: number, y: number, z: number}} 3D point on sphere
   */
  static convertLatLonToSpherePoint(latDeg, lonDeg, radius = 22.0) {
    const latRad = (Math.max(-90.0, Math.min(90.0, latDeg)) * Math.PI) / 180.0;
    const normLon = ((lonDeg % 360) + 540) % 360 - 180;
    const lonRad = (normLon * Math.PI) / 180.0;

    const phi = Math.PI / 2.0 - latRad;
    const theta = lonRad + Math.PI;

    const x = -radius * Math.sin(phi) * Math.cos(theta);
    const y = radius * Math.cos(phi);
    const z = radius * Math.sin(phi) * Math.sin(theta);

    return {
      x: parseFloat(x.toFixed(4)),
      y: parseFloat(y.toFixed(4)),
      z: parseFloat(z.toFixed(4))
    };
  }

  /**
   * Convert a 3D point on a sphere of radius R to latitude and longitude in degrees.
   * @param {{x: number, y: number, z: number}} point - 3D vector point
   * @param {number} [radius=22.0] - Sphere radius
   * @returns {{lat: number, lon: number}} Geographic coordinates in degrees
   */
  static convertSpherePointToLatLon(point, radius = 22.0) {
    const pLen = Math.sqrt(point.x * point.x + point.y * point.y + point.z * point.z);
    const r = pLen > 0 ? pLen : radius;

    const ny = Math.max(-1.0, Math.min(1.0, point.y / r));
    const latRad = Math.asin(ny);
    const latDeg = (latRad * 180.0) / Math.PI;

    let lonRad = Math.atan2(point.z, -point.x) - Math.PI;
    let lonDeg = (lonRad * 180.0) / Math.PI;
    while (lonDeg < -180.0) lonDeg += 360.0;
    while (lonDeg > 180.0) lonDeg -= 360.0;

    return {
      lat: parseFloat(latDeg.toFixed(3)),
      lon: parseFloat(lonDeg.toFixed(3))
    };
  }

  /**
   * Generate an ultra-high-resolution procedural planetary equirectangular texture map.
   * Generates realistic albedo variations, polar caps, canyons, volcanoes, and maria for Mars, Moon, Earth, Phobos, and Deimos.
   * @param {string} [body='mars'] - Target planetary body ('mars', 'moon', 'earth', 'phobos', 'deimos')
   * @param {number} [width=1024] - Texture width in pixels
   * @param {number} [height=512] - Texture height in pixels
   * @returns {HTMLCanvasElement} Rendered canvas element ready for WebGL texture mapping
   */
  static generatePlanetaryTexture(body = 'mars', width = 1024, height = 512) {
    const canvas = (typeof document !== 'undefined' && document.createElement)
      ? document.createElement('canvas')
      : { width, height, getContext: () => null };

    canvas.width = width;
    canvas.height = height;
    const ctx = canvas.getContext ? canvas.getContext('2d') : null;
    if (!ctx) return canvas;

    const b = (body || 'mars').toLowerCase();

    if (b === 'moon') {
      // --- LUNAR HIGHLANDS & MARE TEXTURE ---
      ctx.fillStyle = '#6b7280';
      ctx.fillRect(0, 0, width, height);

      // Highlands bright grain
      for (let i = 0; i < 400; i++) {
        const x = Math.random() * width;
        const y = Math.random() * height;
        const r = 10 + Math.random() * 40;
        const radGrd = ctx.createRadialGradient(x, y, 0, x, y, r);
        radGrd.addColorStop(0, 'rgba(209, 213, 219, 0.25)');
        radGrd.addColorStop(1, 'rgba(107, 114, 128, 0)');
        ctx.fillStyle = radGrd;
        ctx.beginPath();
        ctx.arc(x, y, r, 0, Math.PI * 2);
        ctx.fill();
      }

      // Dark basaltic maria
      const maria = [
        { name: 'Oceanus Procellarum', x: 0.35 * width, y: 0.40 * height, rx: 0.14 * width, ry: 0.20 * height },
        { name: 'Mare Imbrium', x: 0.45 * width, y: 0.30 * height, rx: 0.10 * width, ry: 0.12 * height },
        { name: 'Mare Serenitatis', x: 0.55 * width, y: 0.35 * height, rx: 0.08 * width, ry: 0.09 * height },
        { name: 'Mare Tranquillitatis', x: 0.58 * width, y: 0.45 * height, rx: 0.09 * width, ry: 0.09 * height },
        { name: 'Mare Crisium', x: 0.68 * width, y: 0.38 * height, rx: 0.05 * width, ry: 0.06 * height },
        { name: 'Mare Fecunditatis', x: 0.65 * width, y: 0.55 * height, rx: 0.07 * width, ry: 0.08 * height }
      ];

      maria.forEach(m => {
        const mg = ctx.createRadialGradient(m.x, m.y, 0, m.x, m.y, Math.max(m.rx, m.ry));
        mg.addColorStop(0, '#26282b');
        mg.addColorStop(0.7, '#374151');
        mg.addColorStop(1, 'rgba(75, 85, 99, 0)');
        ctx.fillStyle = mg;
        ctx.beginPath();
        ctx.ellipse(m.x, m.y, m.rx, m.ry, 0, 0, Math.PI * 2);
        ctx.fill();
      });

      // Bright ray craters (Tycho & Copernicus)
      const rayCraters = [
        { x: 0.47 * width, y: 0.75 * height, r: 8, rays: 18 }, // Tycho
        { x: 0.44 * width, y: 0.40 * height, r: 6, rays: 12 }  // Copernicus
      ];

      rayCraters.forEach(c => {
        ctx.strokeStyle = 'rgba(243, 244, 246, 0.4)';
        ctx.lineWidth = 1;
        for (let a = 0; a < c.rays; a++) {
          const angle = (a * Math.PI * 2) / c.rays;
          const len = 30 + Math.random() * 60;
          ctx.beginPath();
          ctx.moveTo(c.x, c.y);
          ctx.lineTo(c.x + Math.cos(angle) * len, c.y + Math.sin(angle) * len);
          ctx.stroke();
        }
        ctx.fillStyle = '#ffffff';
        ctx.beginPath();
        ctx.arc(c.x, c.y, c.r, 0, Math.PI * 2);
        ctx.fill();
      });

    } else if (b === 'earth') {
      // --- EARTH CONTINENTS, OCEANS & CLOUDS ---
      // Deep blue ocean background
      const oceanGrad = ctx.createLinearGradient(0, 0, 0, height);
      oceanGrad.addColorStop(0, '#0f2b48');
      oceanGrad.addColorStop(0.5, '#1e40af');
      oceanGrad.addColorStop(1, '#0f2b48');
      ctx.fillStyle = oceanGrad;
      ctx.fillRect(0, 0, width, height);

      // Continents (Africa, Eurasia, Americas, Australia, Antarctica)
      ctx.fillStyle = '#15803d'; // Green/vegetation & tan
      // Africa & Eurasia
      ctx.beginPath();
      ctx.ellipse(0.55 * width, 0.45 * height, 0.12 * width, 0.22 * height, 0.2, 0, Math.PI * 2);
      ctx.ellipse(0.65 * width, 0.30 * height, 0.20 * width, 0.15 * height, -0.1, 0, Math.PI * 2);
      ctx.fill();

      // Americas
      ctx.fillStyle = '#166534';
      ctx.beginPath();
      ctx.ellipse(0.25 * width, 0.32 * height, 0.10 * width, 0.14 * height, 0.3, 0, Math.PI * 2);
      ctx.ellipse(0.30 * width, 0.65 * height, 0.08 * width, 0.18 * height, 0.1, 0, Math.PI * 2);
      ctx.fill();

      // Australia
      ctx.fillStyle = '#b45309'; // Desert gold
      ctx.beginPath();
      ctx.ellipse(0.82 * width, 0.65 * height, 0.06 * width, 0.06 * height, 0, 0, Math.PI * 2);
      ctx.fill();

      // Antarctica & Arctic ice
      ctx.fillStyle = '#f8fafc';
      ctx.fillRect(0, 0, width, 0.08 * height);
      ctx.fillRect(0, 0.90 * height, width, 0.10 * height);

      // Swirling atmospheric clouds
      ctx.fillStyle = 'rgba(255, 255, 255, 0.35)';
      for (let i = 0; i < 25; i++) {
        const cx = Math.random() * width;
        const cy = 0.2 * height + Math.random() * 0.6 * height;
        ctx.beginPath();
        ctx.ellipse(cx, cy, 50 + Math.random() * 80, 10 + Math.random() * 20, 0.2, 0, Math.PI * 2);
        ctx.fill();
      }

    } else {
      // --- MARS (DEFAULT) HIGH-FIDELITY PLANETARY TEXTURE ---
      // Base reddish/ochre Martian crust
      const marsGrad = ctx.createLinearGradient(0, 0, 0, height);
      marsGrad.addColorStop(0, '#e5804e');
      marsGrad.addColorStop(0.2, '#c25a2b');
      marsGrad.addColorStop(0.5, '#a8431b');
      marsGrad.addColorStop(0.8, '#c25a2b');
      marsGrad.addColorStop(1, '#e5804e');
      ctx.fillStyle = marsGrad;
      ctx.fillRect(0, 0, width, height);

      // Multi-octave crustal roughness & albedo patches
      for (let i = 0; i < 300; i++) {
        const x = Math.random() * width;
        const y = Math.random() * height;
        const r = 20 + Math.random() * 60;
        const radGrd = ctx.createRadialGradient(x, y, 0, x, y, r);
        radGrd.addColorStop(0, 'rgba(115, 38, 14, 0.3)');
        radGrd.addColorStop(1, 'rgba(168, 67, 27, 0)');
        ctx.fillStyle = radGrd;
        ctx.beginPath();
        ctx.arc(x, y, r, 0, Math.PI * 2);
        ctx.fill();
      }

      // Major Albedo Markings (Dark Volcanic Basaltic Sands)
      // 1. Syrtis Major Planum (near 70 deg E, 10 deg N):
      const syrtisGrd = ctx.createRadialGradient(0.69 * width, 0.44 * height, 0, 0.69 * width, 0.44 * height, 0.09 * width);
      syrtisGrd.addColorStop(0, '#2e1208');
      syrtisGrd.addColorStop(0.7, '#421a0d');
      syrtisGrd.addColorStop(1, 'rgba(66, 26, 13, 0)');
      ctx.fillStyle = syrtisGrd;
      ctx.beginPath();
      ctx.moveTo(0.69 * width, 0.35 * height);
      ctx.lineTo(0.74 * width, 0.50 * height);
      ctx.lineTo(0.64 * width, 0.50 * height);
      ctx.closePath();
      ctx.fill();

      // 2. Acidalia Planitia & Mare Acidalium (northern dark swath, 300-360 deg E, 35-60 deg N):
      const acidaliaGrd = ctx.createRadialGradient(0.92 * width, 0.28 * height, 0, 0.92 * width, 0.28 * height, 0.12 * width);
      acidaliaGrd.addColorStop(0, '#291007');
      acidaliaGrd.addColorStop(0.8, '#3d180b');
      acidaliaGrd.addColorStop(1, 'rgba(61, 24, 11, 0)');
      ctx.fillStyle = acidaliaGrd;
      ctx.beginPath();
      ctx.ellipse(0.92 * width, 0.28 * height, 0.11 * width, 0.08 * height, 0.1, 0, Math.PI * 2);
      ctx.fill();

      // 3. Sinus Meridiani & Mare Erythraeum (southern lowlands belt):
      ctx.fillStyle = 'rgba(45, 17, 8, 0.65)';
      ctx.beginPath();
      ctx.ellipse(0.02 * width, 0.55 * height, 0.12 * width, 0.06 * height, -0.1, 0, Math.PI * 2);
      ctx.ellipse(0.88 * width, 0.58 * height, 0.10 * width, 0.07 * height, 0.1, 0, Math.PI * 2);
      ctx.fill();

      // 4. Bright Dust-Filled Impact Basins (Hellas & Argyre):
      // Hellas Basin (42 deg S, 70 deg E):
      const hellasGrd = ctx.createRadialGradient(0.70 * width, 0.73 * height, 0, 0.70 * width, 0.73 * height, 0.09 * width);
      hellasGrd.addColorStop(0, '#f59e0b');
      hellasGrd.addColorStop(0.6, '#ea580c');
      hellasGrd.addColorStop(1, 'rgba(234, 88, 12, 0)');
      ctx.fillStyle = hellasGrd;
      ctx.beginPath();
      ctx.ellipse(0.70 * width, 0.73 * height, 0.08 * width, 0.06 * height, 0, 0, Math.PI * 2);
      ctx.fill();

      // Argyre Basin (50 deg S, 316 deg E):
      const argyreGrd = ctx.createRadialGradient(0.88 * width, 0.77 * height, 0, 0.88 * width, 0.77 * height, 0.06 * width);
      argyreGrd.addColorStop(0, '#f97316');
      argyreGrd.addColorStop(0.7, '#c2410c');
      argyreGrd.addColorStop(1, 'rgba(194, 65, 12, 0)');
      ctx.fillStyle = argyreGrd;
      ctx.beginPath();
      ctx.ellipse(0.88 * width, 0.77 * height, 0.05 * width, 0.04 * height, 0, 0, Math.PI * 2);
      ctx.fill();

      // 5. Valles Marineris Canyon System (270-320 deg E, 5-15 deg S):
      ctx.strokeStyle = '#1f0903';
      ctx.lineWidth = 4;
      ctx.beginPath();
      ctx.moveTo(0.75 * width, 0.54 * height);
      ctx.bezierCurveTo(0.80 * width, 0.56 * height, 0.85 * width, 0.55 * height, 0.90 * width, 0.53 * height);
      ctx.stroke();

      // 6. Tharsis Volcanoes & Olympus Mons (226 deg E, 18 deg N):
      const volcanoes = [
        { name: 'Olympus Mons', x: 0.63 * width, y: 0.40 * height, r: 12 },
        { name: 'Ascraeus Mons', x: 0.71 * width, y: 0.43 * height, r: 8 },
        { name: 'Pavonis Mons', x: 0.69 * width, y: 0.50 * height, r: 8 },
        { name: 'Arsia Mons', x: 0.66 * width, y: 0.55 * height, r: 8 },
        { name: 'Elysium Mons', x: 0.41 * width, y: 0.36 * height, r: 9 }
      ];

      volcanoes.forEach(v => {
        const vg = ctx.createRadialGradient(v.x, v.y, 0, v.x, v.y, v.r);
        vg.addColorStop(0, '#fb923c');
        vg.addColorStop(0.5, '#7c2d12');
        vg.addColorStop(1, 'rgba(124, 45, 18, 0)');
        ctx.fillStyle = vg;
        ctx.beginPath();
        ctx.arc(v.x, v.y, v.r, 0, Math.PI * 2);
        ctx.fill();

        // Caldera pit
        ctx.fillStyle = '#1c0702';
        ctx.beginPath();
        ctx.arc(v.x, v.y, v.r * 0.25, 0, Math.PI * 2);
        ctx.fill();
      });

      // 7. North & South Polar Ice Caps (Planum Boreum & Planum Australe):
      // North Polar Cap (bright white/ice blue with spiral chasmata cuts)
      const npGrad = ctx.createRadialGradient(0.50 * width, 0, 0, 0.50 * width, 0, 0.14 * height);
      npGrad.addColorStop(0, '#ffffff');
      npGrad.addColorStop(0.7, '#e0f2fe');
      npGrad.addColorStop(1, 'rgba(224, 242, 254, 0)');
      ctx.fillStyle = npGrad;
      ctx.fillRect(0, 0, width, 0.12 * height);

      // South Polar Cap (bright dry ice cap)
      const spGrad = ctx.createRadialGradient(0.50 * width, height, 0, 0.50 * width, height, 0.10 * height);
      spGrad.addColorStop(0, '#ffffff');
      spGrad.addColorStop(0.8, '#f1f5f9');
      spGrad.addColorStop(1, 'rgba(241, 245, 249, 0)');
      ctx.fillStyle = spGrad;
      ctx.fillRect(0, 0.88 * height, width, 0.12 * height);
    }

    return canvas;
  }
}















