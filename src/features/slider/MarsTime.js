/**
 * @module MarsTime
 * @description Astronomical calculations for Mars based on Allison & McEwen (2000)
 * and NASA GISS Mars24 algorithm standards.
 *
 * Converts between Earth UTC, Mars Sol Date (MSD), Mars Year (MY),
 * Solar Longitude (Ls), and Local True Solar Time (LTST).
 */

export class MarsTime {
  /** Mars orbital eccentricity */
  static ECCENTRICITY = 0.09340;
  /** Mars semi-major axis (AU) */
  static SEMI_MAJOR_AXIS = 1.52368;
  /** Mars obliquity (degrees) */
  static OBLIQUITY = 25.19;
  /** Solar constant at 1 AU (W/m^2) */
  static SOLAR_CONSTANT_1AU = 1361.0;
  /** Mean Mars solar constant (W/m^2) */
  static SOLAR_CONSTANT_MARS = 588.6;

  /**
   * Convert JavaScript Date or timestamp to Julian Date (UT).
   * @param {Date|number} date - JS Date object or epoch milliseconds.
   * @returns {number} Julian Date (JD).
   */
  static dateToJD(date) {
    const epochMs = date instanceof Date ? date.getTime() : Number(date);
    return (epochMs / 86400000) + 2440587.5;
  }

  /**
   * Convert Julian Date to Terrestrial Time (TT) Julian Date.
   * @param {number} jd - Julian Date UT.
   * @returns {number} Julian Date TT.
   */
  static jdToJdTT(jd) {
    // Delta T approx 69.184 seconds
    return jd + (69.184 / 86400);
  }

  /**
   * Calculate Mars Sol Date (MSD) from Julian Date TT.
   * @param {number} jdTT - Julian Date Terrestrial Time.
   * @returns {number} Mars Sol Date.
   */
  static jdTTToMSD(jdTT) {
    return (jdTT - 2451549.5) / 1.027491252 + 44796.0 - 0.00096;
  }

  /**
   * Calculate Coordinated Mars Time (MTC) in hours [0, 24).
   * @param {number} msd - Mars Sol Date.
   * @returns {number} MTC hours.
   */
  static msdToMTC(msd) {
    let mtc = (24 * msd) % 24;
    return (mtc < 0) ? mtc + 24 : mtc;
  }

  /**
   * Compute full Mars astronomical state from a JavaScript Date.
   * @param {Date|number} [date=new Date()] - Earth date.
   * @returns {object} Mars celestial state.
   */
  static computeState(date = new Date()) {
    const jd = this.dateToJD(date);
    const jdTT = this.jdToJdTT(jd);
    const msd = this.jdTTToMSD(jdTT);
    const mtc = this.msdToMTC(msd);

    // Days since J2000.0 (2000-01-01 12:00:00 TT = JD 2451545.0)
    const d2000 = jdTT - 2451545.0;

    // Mars mean anomaly M (degrees)
    let M = (19.3870 + 0.52402075 * d2000) % 360;
    if (M < 0) M += 360;
    const M_rad = M * Math.PI / 180;

    // Angle of Fictitious Mean Sun alpha_FMS (degrees)
    let alpha_FMS = (270.3863 + 0.52403840 * d2000) % 360;
    if (alpha_FMS < 0) alpha_FMS += 360;

    // Equation of Center (degrees)
    const eoc = (10.691 + 3.0e-7 * d2000) * Math.sin(M_rad)
      + 0.623 * Math.sin(2 * M_rad)
      + 0.050 * Math.sin(3 * M_rad)
      + 0.005 * Math.sin(4 * M_rad);

    // Solar Longitude Ls (degrees)
    let Ls = (alpha_FMS + eoc) % 360;
    if (Ls < 0) Ls += 360;

    // True Anomaly nu (degrees)
    let nu = (M + eoc) % 360;
    if (nu < 0) nu += 360;
    const nu_rad = nu * Math.PI / 180;

    // Mars-Sun distance (AU)
    const e = this.ECCENTRICITY;
    const r_AU = (this.SEMI_MAJOR_AXIS * (1 - e * e)) / (1 + e * Math.cos(nu_rad));

    // Solar insolation at top of atmosphere (W/m^2)
    const solarInsolation = this.SOLAR_CONSTANT_1AU / (r_AU * r_AU);

    // Solar Declination delta_s (degrees)
    const delta_s_rad = Math.asin(Math.sin(this.OBLIQUITY * Math.PI / 180) * Math.sin(Ls * Math.PI / 180));
    const delta_s = delta_s_rad * 180 / Math.PI;

    // Sub-solar latitude is equal to solar declination
    const subSolarLat = delta_s;

    // Sub-solar longitude (East)
    const subSolarLon = ((alpha_FMS - mtc * 15) % 360 + 360) % 360;

    // Mars Year calculation (MY 1 started on 1955-04-11, MSD 28945.0)
    // Approximate Mars year duration = 668.6 sols
    const MY = Math.floor((msd - 28945.0) / 668.6) + 1;

    // Season description
    const season = this.getSeason(Ls);

    return {
      date: date instanceof Date ? date : new Date(date),
      jd,
      jdTT,
      msd,
      mtc,
      Ls,
      MY,
      r_AU,
      solarInsolation,
      subSolarLat,
      subSolarLon,
      season
    };
  }

  /**
   * Get Martian season description for northern/southern hemispheres.
   * @param {number} Ls - Solar Longitude in degrees.
   * @returns {{north: string, south: string, name: string}}
   */
  static getSeason(Ls) {
    if (Ls >= 0 && Ls < 90) {
      return { north: 'Spring', south: 'Autumn', name: 'Northern Spring / Southern Autumn' };
    } else if (Ls >= 90 && Ls < 180) {
      return { north: 'Summer', south: 'Winter', name: 'Northern Summer (Aphelion) / Southern Winter' };
    } else if (Ls >= 180 && Ls < 270) {
      return { north: 'Autumn', south: 'Spring', name: 'Northern Autumn / Southern Spring' };
    } else {
      return { north: 'Winter', south: 'Summer', name: 'Northern Winter / Southern Summer (Perihelion - Dust Storm Season)' };
    }
  }

  /**
   * Calculate Local True Solar Time (LTST) at a given Mars longitude.
   * @param {number} Ls - Solar Longitude (degrees).
   * @param {number} mtcHours - Coordinated Mars Time (0-24 h).
   * @param {number} eastLon - Longitude in East degrees (0-360).
   * @returns {number} LTST in decimal hours (0-24).
   */
  static computeLTST(Ls, mtcHours, eastLon) {
    // Equation of time approximation
    const Ls_rad = Ls * Math.PI / 180;
    const eotHours = (2.861 * Math.sin(2 * Ls_rad) - 0.071 * Math.sin(4 * Ls_rad)) / 15;
    let ltst = (mtcHours + (eastLon * 24 / 360) + eotHours) % 24;
    return (ltst < 0) ? ltst + 24 : ltst;
  }

  /**
   * Calculate solar zenith angle and incidence cosine.
   * @param {number} lat - Planetocentric latitude (degrees).
   * @param {number} Ls - Solar longitude (degrees).
   * @param {number} localSolarHour - Local solar time in decimal hours (0-24).
   * @returns {{cosZ: number, zenithAngleDeg: number, isDay: boolean}}
   */
  static getSolarZenith(lat, Ls, localSolarHour) {
    const lat_rad = lat * Math.PI / 180;
    const delta_rad = Math.asin(Math.sin(this.OBLIQUITY * Math.PI / 180) * Math.sin(Ls * Math.PI / 180));
    // Hour angle omega (0 at solar noon = 12h, 15 deg per hour)
    const omega_rad = (localSolarHour - 12) * 15 * Math.PI / 180;

    const cosZ = Math.sin(lat_rad) * Math.sin(delta_rad) + Math.cos(lat_rad) * Math.cos(delta_rad) * Math.cos(omega_rad);
    const zenithAngleDeg = Math.acos(Math.max(-1, Math.min(1, cosZ))) * 180 / Math.PI;

    return {
      cosZ: Math.max(0, cosZ),
      zenithAngleDeg,
      isDay: cosZ > 0
    };
  }

  /**
   * Approximate Earth Date from Mars Year and Solar Longitude (Ls).
   * Solves Kepler orbit iteratively.
   * @param {number} Ls - Solar Longitude (0-360 deg).
   * @param {number} [marsYear=37] - Mars Year number (e.g. MY 37).
   * @returns {Date} Earth Date approximation.
   */
  static lsToDate(Ls, marsYear = 37) {
    // MY 1 start: MSD = 28945.0 (1955-04-11)
    // Mean orbital period of Mars = 668.6 sols
    const myStartMsd = 28945.0 + (marsYear - 1) * 668.6;

    // Approximate fractional sol offset using non-uniform orbital speed
    const Ls_rad = Ls * Math.PI / 180;
    const e = this.ECCENTRICITY;
    // Mean anomaly approx from Ls
    const M_rad = Ls_rad - (2 * e - (Math.pow(e, 3) / 4)) * Math.sin(Ls_rad);
    const fraction = (M_rad / (2 * Math.PI) + 1.0) % 1.0;
    const msd = myStartMsd + fraction * 668.6;

    // Convert MSD to JD TT => JD UT => epoch ms
    const jdTT = (msd - 44796.0 + 0.00096) * 1.027491252 + 2451549.5;
    const jd = jdTT - (69.184 / 86400);
    const epochMs = (jd - 2440587.5) * 86400000;
    return new Date(epochMs);
  }

  /**
   * Calculate Mission Sol number for major Mars surface missions.
   * @param {Date|number} date
   * @param {string} [mission='perseverance']
   * @returns {{mission: string, sol: number, active: boolean}}
   */
  static getMissionSol(date, mission = 'perseverance') {
    const MISSIONS = {
      'perseverance': { name: 'Perseverance (Mars 2020)', landingMsd: 52303.88 },
      'curiosity': { name: 'Curiosity (MSL)', landingMsd: 49268.22 },
      'insight': { name: 'InSight Lander', landingMsd: 51511.83 },
      'opportunity': { name: 'Opportunity (MER-B)', landingMsd: 46216.0 },
      'spirit': { name: 'Spirit (MER-A)', landingMsd: 46195.0 },
      'viking1': { name: 'Viking 1', landingMsd: 36440.0 },
      'viking2': { name: 'Viking 2', landingMsd: 36484.0 }
    };

    const target = MISSIONS[mission.toLowerCase()] || MISSIONS.perseverance;
    const jd = this.dateToJD(date);
    const jdTT = this.jdToJdTT(jd);
    const currentMsd = this.jdTTToMSD(jdTT);

    const sol = Math.floor(currentMsd - target.landingMsd);
    return {
      mission: target.name,
      sol,
      active: sol >= 0
    };
  }

  /**
   * Format decimal hours as HH:MM:SS string.
   * @param {number} hours - Decimal hours (0-24).
   * @returns {string} Formatted time string.
   */
  static formatHours(hours) {
    const h = Math.floor(hours);
    const mFloat = (hours - h) * 60;
    const m = Math.floor(mFloat);
    const s = Math.floor((mFloat - m) * 60);
    return `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`;
  }

  /**
   * Calculate full solar position (elevation, azimuth, shadow factor) for any Martian surface point.
   * @param {number} lat - Latitude in degrees
   * @param {number} lon - Longitude (East degrees)
   * @param {number} Ls - Solar Longitude (degrees)
   * @param {number} localSolarHour - Local solar time (0-24 h)
   * @returns {{altitudeDeg: number, azimuthDeg: number, zenithDeg: number, isDay: boolean, shadowFactor: number}}
   */
  static getSolarPosition(lat, lon, Ls, localSolarHour) {
    const latRad = lat * Math.PI / 180;
    const deltaRad = Math.asin(Math.sin(this.OBLIQUITY * Math.PI / 180) * Math.sin(Ls * Math.PI / 180));
    const omegaRad = (localSolarHour - 12) * 15 * Math.PI / 180;

    // Solar elevation angle sin(alpha)
    const sinAlt = Math.sin(latRad) * Math.sin(deltaRad) + Math.cos(latRad) * Math.cos(deltaRad) * Math.cos(omegaRad);
    const altRad = Math.asin(Math.max(-1, Math.min(1, sinAlt)));
    const altitudeDeg = altRad * 180 / Math.PI;
    const zenithDeg = 90 - altitudeDeg;

    // Solar azimuth angle cos(Az)
    const cosAlt = Math.cos(altRad);
    let azimuthDeg = 180;

    if (cosAlt > 1e-6) {
      const cosAz = (Math.sin(deltaRad) * Math.cos(latRad) - Math.cos(deltaRad) * Math.sin(latRad) * Math.cos(omegaRad)) / cosAlt;
      const sinAz = -Math.cos(deltaRad) * Math.sin(omegaRad) / cosAlt;
      azimuthDeg = (Math.atan2(sinAz, cosAz) * 180 / Math.PI + 360) % 360;
    }

    const isDay = altitudeDeg > 0;
    const shadowFactor = isDay ? 1 / Math.tan(Math.max(0.01, altRad)) : Infinity;

    return {
      altitudeDeg,
      azimuthDeg,
      zenithDeg,
      isDay,
      shadowFactor
    };
  }

  /**
   * Calculate daylight duration in decimal hours for any Martian latitude and season.
   * @param {number} lat - Latitude in degrees
   * @param {number} Ls - Solar Longitude (0-360 deg)
   * @returns {{daylightHours: number, state: string}}
   */
  static getMartianDayLength(lat, Ls) {
    const latRad = lat * Math.PI / 180;
    const deltaRad = Math.asin(Math.sin(this.OBLIQUITY * Math.PI / 180) * Math.sin(Ls * Math.PI / 180));

    const tanProduct = Math.tan(latRad) * Math.tan(deltaRad);
    const cosOmega0 = -tanProduct;

    if (cosOmega0 <= -1) {
      return { daylightHours: 24.0, state: 'Polar Day (24h Sun)' };
    }
    if (cosOmega0 >= 1) {
      return { daylightHours: 0.0, state: 'Polar Night (24h Dark)' };
    }

    const omega0Rad = Math.acos(cosOmega0);
    const daylightHours = 24.0 * (omega0Rad / Math.PI);

    return {
      daylightHours,
      state: 'Normal Day/Night Cycle'
    };
  }

  // --- Equation of Time & Seasonal Sol Calendars ---

  /**
   * Compute the Equation of Time (EOT) on Mars in minutes and hours.
   * Represents the difference between Local True Solar Time (LTST) and Local Mean Solar Time (LMST).
   * @param {number} Ls - Solar Longitude in degrees (0-360)
   * @returns {{eotMinutes: number, eotHours: number, eotSeconds: number}}
   */
  static computeEquationOfTime(Ls) {
    const LsRad = Ls * Math.PI / 180;
    const eotHours = (2.861 * Math.sin(2 * LsRad) - 0.071 * Math.sin(4 * LsRad)) / 15.0;
    const eotMinutes = eotHours * 60.0;
    const eotSeconds = eotMinutes * 60.0;

    return {
      eotMinutes: parseFloat(eotMinutes.toFixed(2)),
      eotHours: parseFloat(eotHours.toFixed(4)),
      eotSeconds: parseFloat(eotSeconds.toFixed(1))
    };
  }

  /**
   * Get astronomical sol durations for the 4 Martian seasons (668.6 total sols).
   * @returns {{springSols: number, summerSols: number, autumnSols: number, winterSols: number, totalSols: number}}
   */
  static computeSeasonalSolDurations() {
    return {
      springSols: 193.3, // Ls 0 to 90
      summerSols: 178.5, // Ls 90 to 180 (Aphelion)
      autumnSols: 142.7, // Ls 180 to 270
      winterSols: 154.1, // Ls 270 to 360 (Perihelion)
      totalSols: 668.6
    };
  }

  /**
   * Calculate precise Mars Sol Date (MSD) and MTC for a given Date.
   * @param {Date|number} [date=new Date()]
   * @returns {{msd: number, mtc: string, mtcHours: number}}
   */
  static computeMarsSolDate(date = new Date()) {
    const jd = this.dateToJD(date);
    const jdTT = this.jdToJdTT(jd);
    const msd = this.jdTTToMSD(jdTT);
    const mtcHours = this.msdToMTC(msd);

    return {
      msd: parseFloat(msd.toFixed(5)),
      mtc: this.formatHours(mtcHours),
      mtcHours: parseFloat(mtcHours.toFixed(4))
    };
  }

  // --- Solar Hour Angle, Equatorial Coordinates & Precession Solvers ---

  /**
   * Calculate Solar Hour Angle (H) from Local True Solar Time (LTST).
   * H = (LTST - 12) * 15 degrees (-180 to +180)
   * @param {number} ltstHours - Local True Solar Time in decimal hours (0-24)
   * @returns {number} Hour angle in degrees
   */
  static computeSolarHourAngle(ltstHours) {
    let h = (ltstHours - 12.0) * 15.0;
    while (h > 180) h -= 360;
    while (h < -180) h += 360;
    return parseFloat(h.toFixed(2));
  }

  /**
   * Calculate exact solar altitude and azimuth from spherical celestial triangle.
   * @param {number} latDeg - Surface latitude
   * @param {number} LsDeg - Solar Longitude (0-360)
   * @param {number} ltstHours - Local True Solar Time (0-24)
   * @returns {{altitudeDeg: number, azimuthDeg: number, isDay: boolean}}
   */
  static computeSolarAzimuthAltitude(latDeg, LsDeg, ltstHours) {
    const latRad = latDeg * Math.PI / 180.0;
    const deltaRad = Math.asin(Math.sin(this.OBLIQUITY * Math.PI / 180.0) * Math.sin(LsDeg * Math.PI / 180.0));
    const hRad = this.computeSolarHourAngle(ltstHours) * Math.PI / 180.0;

    const sinAlt = Math.sin(latRad) * Math.sin(deltaRad) + Math.cos(latRad) * Math.cos(deltaRad) * Math.cos(hRad);
    const altRad = Math.asin(Math.max(-1.0, Math.min(1.0, sinAlt)));
    const altitudeDeg = altRad * 180.0 / Math.PI;

    const cosAlt = Math.cos(altRad);
    let azimuthDeg = 180.0;

    if (cosAlt > 1e-6) {
      const cosAz = (Math.sin(deltaRad) * Math.cos(latRad) - Math.cos(deltaRad) * Math.sin(latRad) * Math.cos(hRad)) / cosAlt;
      const sinAz = -Math.cos(deltaRad) * Math.sin(hRad) / cosAlt;
      azimuthDeg = (Math.atan2(sinAz, cosAz) * 180.0 / Math.PI + 360.0) % 360.0;
    }

    return {
      altitudeDeg: parseFloat(altitudeDeg.toFixed(2)),
      azimuthDeg: parseFloat(azimuthDeg.toFixed(2)),
      isDay: altitudeDeg > 0
    };
  }

  /**
   * Compute Mars orbital perihelion longitude (Ls_p) accounting for secular apsidal precession.
   * Ls_perihelion approx 250.99° at J2000.0 with 0.00184°/yr precession.
   * @param {number} [targetYear=2026] - Gregorian calendar year
   * @returns {{perihelionLs: number, aphelionLs: number}}
   */
  static computeMartianApsidalPrecession(targetYear = 2026) {
    const yearsSinceJ2000 = targetYear - 2000.0;
    const perihelionLs = (250.99 + 0.00184 * yearsSinceJ2000) % 360.0;
    const aphelionLs = (perihelionLs + 180.0) % 360.0;

    return {
      perihelionLs: parseFloat(perihelionLs.toFixed(3)),
      aphelionLs: parseFloat(aphelionLs.toFixed(3))
    };
  }

  // --- Sub-Solar Coordinates, Instantaneous Insolation & Analemma Solvers ---

  /**
   * Calculate exact sub-solar point coordinates (planetocentric latitude and East longitude).
   * @param {number} Ls - Solar Longitude in degrees (0-360)
   * @param {number} [mtcHours=12.0] - Coordinated Mars Time (0-24 h)
   * @returns {{subSolarLatDeg: number, subSolarLonDeg: number, solarDeclinationDeg: number}}
   */
  static computeSubSolarPoint(Ls, mtcHours = 12.0) {
    const deltaRad = Math.asin(Math.sin(this.OBLIQUITY * Math.PI / 180.0) * Math.sin(Ls * Math.PI / 180.0));
    const subSolarLatDeg = deltaRad * 180.0 / Math.PI;

    // Subsolar longitude: where local solar time is 12:00
    // Longitude = (12 - MTC - EoT) * 15 deg
    const eot = this.computeEquationOfTime(Ls);
    let lonDeg = (12.0 - mtcHours - eot.eotHours) * 15.0;
    lonDeg = ((lonDeg % 360.0) + 360.0) % 360.0;

    return {
      subSolarLatDeg: parseFloat(subSolarLatDeg.toFixed(3)),
      subSolarLonDeg: parseFloat(lonDeg.toFixed(3)),
      solarDeclinationDeg: parseFloat(subSolarLatDeg.toFixed(3))
    };
  }

  /**
   * Compute instantaneous Mars-Sun distance and Top-of-Atmosphere (TOA) solar insolation.
   * S = S_1AU / (r_AU)^2
   * @param {number} Ls - Solar Longitude in degrees (0-360)
   * @returns {{distanceAU: number, distanceKm: number, solarFluxW_M2: number, ratioToMean: number}}
   */
  static computeInstantaneousSolarFlux(Ls) {
    const e = this.ECCENTRICITY;
    const a = this.SEMI_MAJOR_AXIS;

    // True anomaly nu approx from Ls
    const LsRad = Ls * Math.PI / 180.0;
    const nuRad = LsRad - (250.99 * Math.PI / 180.0); // Offset relative to perihelion Ls ~ 251 deg

    const rAU = (a * (1.0 - e * e)) / (1.0 + e * Math.cos(nuRad));
    const rKm = rAU * 149597870.7; // 1 AU in km
    const solarFlux = this.SOLAR_CONSTANT_1AU / (rAU * rAU);
    const ratioToMean = solarFlux / this.SOLAR_CONSTANT_MARS;

    return {
      distanceAU: parseFloat(rAU.toFixed(5)),
      distanceKm: parseFloat(rKm.toFixed(0)),
      solarFluxW_M2: parseFloat(solarFlux.toFixed(2)),
      ratioToMean: parseFloat(ratioToMean.toFixed(3))
    };
  }

  /**
   * Generate Martian Analemma curve data (Solar Declination vs Equation of Time).
   * Unlike Earth's figure-8, Mars' analemma is a pronounced teardrop shape due to high orbital eccentricity.
   * @param {number} [samples=24] - Number of points along orbit
   * @returns {Array<{Ls: number, declinationDeg: number, eotMinutes: number}>}
   */
  static computeAnalemmaCoordinates(samples = 24) {
    const points = [];
    const step = 360.0 / samples;

    for (let i = 0; i < samples; i++) {
      const Ls = i * step;
      const sub = this.computeSubSolarPoint(Ls, 12.0);
      const eot = this.computeEquationOfTime(Ls);

      points.push({
        Ls: parseFloat(Ls.toFixed(1)),
        declinationDeg: sub.subSolarLatDeg,
        eotMinutes: eot.eotMinutes
      });
    }

    return points;
  }

  // --- True Solar Sol Duration, Kepler Anomaly & Seasonal Calendar Solvers ---

  /**
   * Calculate variable duration of true solar day (sol) at a given season (Ls).
   * @param {number} Ls - Solar Longitude in degrees (0-360)
   * @returns {{solDurationSeconds: number, solDurationMinutes: number, diffFromMeanSeconds: number}}
   */
  static computeTrueSolarSolDuration(Ls) {
    const e = this.ECCENTRICITY;
    const LsRad = Ls * Math.PI / 180.0;
    const nuRad = LsRad - (250.99 * Math.PI / 180.0);

    const baseSolSec = 88775.244;
    // Fractional rate of true solar motion change ~ (1 + 2*e*cos(nu))
    const solSec = baseSolSec * (1.0 + (2.0 * e * Math.cos(nuRad)) / (1.0 - e * e));
    const diffSec = solSec - baseSolSec;

    return {
      solDurationSeconds: parseFloat(solSec.toFixed(2)),
      solDurationMinutes: parseFloat((solSec / 60.0).toFixed(2)),
      diffFromMeanSeconds: parseFloat(diffSec.toFixed(2))
    };
  }

  /**
   * Solve Kepler's equation M = E - e*sin(E) and compute true anomaly nu.
   * @param {number} meanAnomalyDeg - Mean anomaly in degrees (0-360)
   * @param {number} [eccentricity=0.0934] - Orbit eccentricity
   * @returns {{eccentricAnomalyDeg: number, trueAnomalyDeg: number, radiusRatio: number}}
   */
  static computeKeplerOrbitTrueAnomaly(meanAnomalyDeg, eccentricity = 0.0934) {
    const MRad = (meanAnomalyDeg % 360.0) * Math.PI / 180.0;
    const e = Math.max(0, Math.min(0.99, eccentricity));

    // Newton-Raphson iteration for Eccentric Anomaly E
    let E = MRad;
    for (let iter = 0; iter < 15; iter++) {
      const f = E - e * Math.sin(E) - MRad;
      const df = 1.0 - e * Math.cos(E);
      const delta = f / df;
      E -= delta;
      if (Math.abs(delta) < 1e-8) break;
    }

    // True anomaly nu = 2 * atan(sqrt((1+e)/(1-e)) * tan(E/2))
    const sqrtFactor = Math.sqrt((1.0 + e) / (1.0 - e));
    const nuRad = 2.0 * Math.atan2(sqrtFactor * Math.sin(E / 2.0), Math.cos(E / 2.0));
    let nuDeg = nuRad * 180.0 / Math.PI;
    if (nuDeg < 0) nuDeg += 360.0;

    const radiusRatio = (1.0 - e * e) / (1.0 + e * Math.cos(nuRad));

    return {
      eccentricAnomalyDeg: parseFloat(((E * 180.0 / Math.PI + 360.0) % 360.0).toFixed(4)),
      trueAnomalyDeg: parseFloat(nuDeg.toFixed(4)),
      radiusRatio: parseFloat(radiusRatio.toFixed(5))
    };
  }

  /**
   * Compute exact Gregorian start dates for all 4 astronomical seasons for given Mars Year.
   * @param {number} [marsYear=37] - Mars Year number
   * @returns {{springDate: Date, summerDate: Date, autumnDate: Date, winterDate: Date}}
   */
  static computeSeasonalCalendarDates(marsYear = 37) {
    return {
      springDate: this.lsToDate(0, marsYear),
      summerDate: this.lsToDate(90, marsYear),
      autumnDate: this.lsToDate(180, marsYear),
      winterDate: this.lsToDate(270, marsYear)
    };
  }

  // --- Heliocentric Orbital Speed, Mean Solar Time & Apparent Sun Diameter Solvers ---

  /**
   * Calculate instantaneous heliocentric orbital velocity of Mars along its eccentric orbit.
   * v = sqrt(GM_sun * (2/r - 1/a))
   * @param {number} Ls - Solar Longitude in degrees (0-360)
   * @returns {{orbitalSpeedKmS: number, orbitalSpeedMps: number, isNearPerihelion: boolean}}
   */
  static computeHeliocentricOrbitalSpeed(Ls) {
    const GM_Sun = 1.32712440018e11; // km^3 / s^2
    const aKm = this.SEMI_MAJOR_AXIS * 149597870.7; // ~227.9M km
    const fluxRes = this.computeInstantaneousSolarFlux(Ls);
    const rKm = fluxRes.distanceKm;

    const vKmS = Math.sqrt(GM_Sun * (2.0 / rKm - 1.0 / aKm));

    return {
      orbitalSpeedKmS: parseFloat(vKmS.toFixed(3)),
      orbitalSpeedMps: parseFloat((vKmS * 1000.0).toFixed(1)),
      isNearPerihelion: Math.abs(Ls - 251.0) < 45.0
    };
  }

  /**
   * Compute Local Mean Solar Time (LMST) at a given Mars longitude.
   * LMST = (MTC + eastLon / 15) % 24
   * @param {number} eastLonDeg - Longitude in degrees East (0-360)
   * @param {number} [mtcHours=12.0] - Coordinated Mars Time (MTC)
   * @returns {{lmstHours: number, lmstFormatted: string}}
   */
  static computeMeanSolarTimeOffset(eastLonDeg, mtcHours = 12.0) {
    let lmst = (mtcHours + eastLonDeg / 15.0) % 24.0;
    if (lmst < 0) lmst += 24.0;

    return {
      lmstHours: parseFloat(lmst.toFixed(4)),
      lmstFormatted: this.formatHours(lmst)
    };
  }

  /**
   * Calculate apparent angular diameter of the Sun viewed from Martian surface.
   * theta = 2 * asin(R_sun / r)
   * @param {number} Ls - Solar Longitude in degrees (0-360)
   * @returns {{angularDiameterDeg: number, angularDiameterArcmin: number}}
   */
  static computeMartianSunDiameter(Ls) {
    const rSunKm = 696340.0; // Sun radius in km
    const fluxRes = this.computeInstantaneousSolarFlux(Ls);
    const rKm = fluxRes.distanceKm;

    const angRad = 2.0 * Math.asin(rSunKm / rKm);
    const angDeg = angRad * 180.0 / Math.PI;
    const angArcmin = angDeg * 60.0;

    return {
      angularDiameterDeg: parseFloat(angDeg.toFixed(4)),
      angularDiameterArcmin: parseFloat(angArcmin.toFixed(2))
    };
  }

  // --- Aerocentric Subsolar Coordinates, Earth-Mars Distance & Darian Calendar Solvers ---

  /**
   * Calculate aerocentric celestial right ascension and declination of the Sun.
   * alpha_sun = atan2(cos(eps) * sin(Ls), cos(Ls)), delta_sun = asin(sin(eps) * sin(Ls))
   * @param {number} Ls - Solar Longitude in degrees (0-360)
   * @returns {{rightAscensionDeg: number, declinationDeg: number, rightAscensionHours: number}}
   */
  static computeAerocentricSubsolarCoordinates(Ls) {
    const epsRad = this.OBLIQUITY * Math.PI / 180.0;
    const LsRad = (Ls % 360.0) * Math.PI / 180.0;

    const y = Math.cos(epsRad) * Math.sin(LsRad);
    const x = Math.cos(LsRad);
    let raDeg = Math.atan2(y, x) * 180.0 / Math.PI;
    if (raDeg < 0) raDeg += 360.0;

    const decRad = Math.asin(Math.sin(epsRad) * Math.sin(LsRad));
    const decDeg = decRad * 180.0 / Math.PI;

    return {
      rightAscensionDeg: parseFloat(raDeg.toFixed(3)),
      declinationDeg: parseFloat(decDeg.toFixed(3)),
      rightAscensionHours: parseFloat((raDeg / 15.0).toFixed(4))
    };
  }

  /**
   * Calculate Earth-Mars heliocentric distance and communication One-Way Light Time (OWLT).
   * d = sqrt(r_earth^2 + r_mars^2 - 2 * r_earth * r_mars * cos(delta_lambda))
   * @param {number} earthHelioLonDeg - Earth heliocentric longitude (0-360)
   * @param {number} marsLsDeg - Mars Solar Longitude Ls (0-360)
   * @returns {{distanceKm: number, distanceAU: number, oneWayLightTimeMinutes: number, oneWayLightTimeSeconds: number}}
   */
  static computeEarthMarsDistanceAndOWLT(earthHelioLonDeg, marsLsDeg) {
    const cKmS = 299792.458; // speed of light in km/s
    const rEarthKm = 149597870.7; // ~ 1 AU
    const fluxRes = this.computeInstantaneousSolarFlux(marsLsDeg);
    const rMarsKm = fluxRes.distanceKm;

    const dLamRad = (earthHelioLonDeg - marsLsDeg) * Math.PI / 180.0;
    const dKm = Math.sqrt(
      rEarthKm * rEarthKm + rMarsKm * rMarsKm - 2.0 * rEarthKm * rMarsKm * Math.cos(dLamRad)
    );
    const dAU = dKm / rEarthKm;

    const owltSec = dKm / cKmS;
    const owltMin = owltSec / 60.0;

    return {
      distanceKm: parseFloat(dKm.toFixed(0)),
      distanceAU: parseFloat(dAU.toFixed(4)),
      oneWayLightTimeMinutes: parseFloat(owltMin.toFixed(2)),
      oneWayLightTimeSeconds: parseFloat(owltSec.toFixed(1))
    };
  }

  /**
   * Determine Darian Mars calendar month and sol interval from Solar Longitude (Ls).
   * The Darian calendar divides the 668-sol Martian year into 24 months of 27-28 sols each.
   * @param {number} Ls - Solar Longitude in degrees (0-360)
   * @returns {{monthNumber: number, monthName: string, quarter: string}}
   */
  static computeDarianMonth(Ls) {
    const DARIAN_MONTHS = [
      'Sagittarius', 'Dhanus', 'Capricornus', 'Makara', 'Aquarius', 'Kumbha',
      'Pisces', 'Mina', 'Aries', 'Mesha', 'Taurus', 'Rishabha',
      'Gemini', 'Mithuna', 'Cancer', 'Karka', 'Leo', 'Simha',
      'Virgo', 'Kanya', 'Libra', 'Tula', 'Scorpius', 'Vrishika'
    ];

    const safeLs = ((Ls % 360.0) + 360.0) % 360.0;
    const monthIdx = Math.min(23, Math.floor(safeLs / 15.0));
    const quarterIdx = Math.floor(monthIdx / 6);
    const quarters = ['Spring', 'Summer', 'Autumn', 'Winter'];

    return {
      monthNumber: monthIdx + 1,
      monthName: DARIAN_MONTHS[monthIdx],
      quarter: quarters[quarterIdx]
    };
  }

  // --- Vis-Viva Velocity, Equation of Center Series & Insolation Fluctuation Solvers ---

  /**
   * Calculate orbital velocity using Vis-Viva equation v = sqrt(GM_sun * (2/r - 1/a)).
   * @param {number} radialDistanceKm - Heliocentric distance r in km
   * @returns {{orbitalVelocityKmS: number, orbitalVelocityMps: number}}
   */
  static computeVisVivaVelocity(radialDistanceKm) {
    const GM_Sun = 1.32712440018e11; // km^3 / s^2
    const aKm = this.SEMI_MAJOR_AXIS * 149597870.7; // ~227.94M km
    const rKm = Math.max(1e6, radialDistanceKm);

    const vKmS = Math.sqrt(GM_Sun * (2.0 / rKm - 1.0 / aKm));

    return {
      orbitalVelocityKmS: parseFloat(vKmS.toFixed(3)),
      orbitalVelocityMps: parseFloat((vKmS * 1000.0).toFixed(1))
    };
  }

  /**
   * Calculate high-order analytic series expansion of Mars Equation of the Center (nu - M).
   * nu - M = (2e - e^3/4)*sin(M) + (5/4 * e^2)*sin(2M) + (13/12 * e^3)*sin(3M)
   * @param {number} meanAnomalyDeg - Mean anomaly M in degrees (0-360)
   * @returns {{equationOfCenterDeg: number, trueAnomalyDeg: number}}
   */
  static computeEquationOfCenterSeries(meanAnomalyDeg) {
    const e = this.ECCENTRICITY;
    const MRad = (meanAnomalyDeg % 360.0) * Math.PI / 180.0;

    const term1 = (2.0 * e - Math.pow(e, 3) / 4.0) * Math.sin(MRad);
    const term2 = (1.25 * Math.pow(e, 2)) * Math.sin(2.0 * MRad);
    const term3 = (13.0 / 12.0 * Math.pow(e, 3)) * Math.sin(3.0 * MRad);

    const eocRad = term1 + term2 + term3;
    const eocDeg = eocRad * 180.0 / Math.PI;
    let nuDeg = (meanAnomalyDeg + eocDeg) % 360.0;
    if (nuDeg < 0) nuDeg += 360.0;

    return {
      equationOfCenterDeg: parseFloat(eocDeg.toFixed(4)),
      trueAnomalyDeg: parseFloat(nuDeg.toFixed(4))
    };
  }

  /**
   * Calculate normalized seasonal solar flux ratio S(Ls) / S_mean.
   * Ratio = [ (1 + e * cos(Ls - Ls_peri)) / (1 - e^2) ]^2
   * @param {number} Ls - Solar Longitude in degrees (0-360)
   * @returns {{insolationRatio: number, percentDeviationFromMean: number, isPerihelionSeason: boolean}}
   */
  static computeInsolationFluctuationRatio(Ls) {
    const e = this.ECCENTRICITY;
    const LsRad = (Ls % 360.0) * Math.PI / 180.0;
    const periRad = 250.99 * Math.PI / 180.0;

    const numerator = 1.0 + e * Math.cos(LsRad - periRad);
    const denominator = 1.0 - e * e;
    const factor = numerator / denominator;
    const ratio = factor * factor;
    const deviation = (ratio - 1.0) * 100.0;

    return {
      insolationRatio: parseFloat(ratio.toFixed(4)),
      percentDeviationFromMean: parseFloat(deviation.toFixed(2)),
      isPerihelionSeason: ratio > 1.10
    };
  }

  // --- Synodic Cycle, Mean Motion & Eccentric-to-True Anomaly Solvers ---

  /**
   * Calculate Earth-Mars synodic period and launch window recurrence interval.
   * S = (P_earth * P_mars) / (P_mars - P_earth)
   * @param {number} [earthPeriodDays=365.25636] - Earth sidereal orbital period in days
   * @param {number} [marsPeriodDays=686.9796] - Mars sidereal orbital period in days
   * @returns {{synodicDays: number, synodicSols: number, synodicEarthYears: number}}
   */
  static computeSynodicCyclePeriod(earthPeriodDays = 365.25636, marsPeriodDays = 686.9796) {
    const P1 = earthPeriodDays;
    const P2 = marsPeriodDays;
    const S_days = (P1 * P2) / Math.abs(P2 - P1);
    const S_sols = S_days * (86400.0 / this.MARS_SOL_SECONDS || 86400.0 / 88775.244);
    const S_years = S_days / 365.25636;

    return {
      synodicDays: parseFloat(S_days.toFixed(2)),
      synodicSols: parseFloat(S_sols.toFixed(2)),
      synodicEarthYears: parseFloat(S_years.toFixed(3))
    };
  }

  /**
   * Calculate Mars orbital mean motion (average angular velocity n = 2*pi / P).
   * @param {number} [semiMajorAxisAU=1.52368] - Semi-major axis in AU
   * @returns {{meanMotionDegPerDay: number, meanMotionDegPerSol: number, meanMotionRadPerSec: number}}
   */
  static computeOrbitalMeanMotion(semiMajorAxisAU = 1.52368) {
    const a = Math.max(0.1, semiMajorAxisAU);
    // Kepler 3rd law: P in years = a^(3/2)
    const pYears = Math.pow(a, 1.5);
    const pDays = pYears * 365.25636;
    const pSec = pDays * 86400.0;

    const nRadS = (2.0 * Math.PI) / pSec;
    const nDegDay = 360.0 / pDays;
    const nDegSol = nDegDay * (88775.244 / 86400.0);

    return {
      meanMotionDegPerDay: parseFloat(nDegDay.toFixed(5)),
      meanMotionDegPerSol: parseFloat(nDegSol.toFixed(5)),
      meanMotionRadPerSec: parseFloat(nRadS.toExponential(5))
    };
  }

  /**
   * Calculate exact true anomaly (nu) and orbital distance (r) from eccentric anomaly (E).
   * tan(nu / 2) = sqrt((1 + e) / (1 - e)) * tan(E / 2),  r = a * (1 - e * cos(E))
   * @param {number} eccentricAnomalyDeg - Eccentric anomaly E in degrees
   * @param {number} [eccentricity=0.0934] - Orbit eccentricity
   * @returns {{trueAnomalyDeg: number, radialDistanceAU: number, radialDistanceKm: number}}
   */
  static computeTrueAnomalyFromEccentricAnomaly(eccentricAnomalyDeg, eccentricity = 0.0934) {
    const ERad = (eccentricAnomalyDeg % 360.0) * Math.PI / 180.0;
    const e = Math.max(0, Math.min(0.99, eccentricity));
    const a = this.SEMI_MAJOR_AXIS;

    const sqrtFactor = Math.sqrt((1.0 + e) / (1.0 - e));
    const nuRad = 2.0 * Math.atan2(sqrtFactor * Math.sin(ERad / 2.0), Math.cos(ERad / 2.0));
    let nuDeg = nuRad * 180.0 / Math.PI;
    if (nuDeg < 0) nuDeg += 360.0;

    const rAU = a * (1.0 - e * Math.cos(ERad));
    const rKm = rAU * 149597870.7;

    return {
      trueAnomalyDeg: parseFloat(nuDeg.toFixed(4)),
      radialDistanceAU: parseFloat(rAU.toFixed(5)),
      radialDistanceKm: parseFloat(rKm.toFixed(0))
    };
  }

  // --- Radial Orbital Velocity, Specific Orbital Energy & Solar Zenith Vector Solvers ---

  /**
   * Calculate heliocentric radial velocity of Mars (dr/dt) along its orbit in km/s.
   * dr/dt = (n * a * e * sin(nu)) / sqrt(1 - e^2)
   * @param {number} trueAnomalyDeg - True anomaly nu in degrees (0 to 360)
   * @param {number} [semiMajorAxisAU=1.52368] - Semi-major axis in AU
   * @param {number} [eccentricity=0.0934] - Orbit eccentricity
   * @returns {{radialVelocityKmS: number, radialVelocityMps: number, isMovingAwayFromSun: boolean}}
   */
  static computeRadialOrbitalVelocity(trueAnomalyDeg, semiMajorAxisAU = 1.52368, eccentricity = 0.0934) {
    const GM_Sun = 1.32712440018e11; // km^3 / s^2
    const aKm = semiMajorAxisAU * 149597870.7;
    const e = eccentricity;
    const nuRad = trueAnomalyDeg * Math.PI / 180.0;

    // Mean motion n in rad/s
    const n = Math.sqrt(GM_Sun / Math.pow(aKm, 3));
    const rDot = (n * aKm * e * Math.sin(nuRad)) / Math.sqrt(1.0 - e * e);

    return {
      radialVelocityKmS: parseFloat(rDot.toFixed(4)),
      radialVelocityMps: parseFloat((rDot * 1000.0).toFixed(2)),
      isMovingAwayFromSun: rDot > 0
    };
  }

  /**
   * Calculate specific Keplerian mechanical energy (vis-viva energy epsilon) in MJ/kg (km^2/s^2).
   * epsilon = v^2 / 2 - GM / r = -GM / (2 * a)
   * @param {number} radialDistanceKm - Distance to Sun center in km
   * @param {number} orbitalVelocityKmS - Instantaneous orbital velocity in km/s
   * @returns {{specificEnergyMjPerKg: number, semiMajorAxisEquivalentKm: number}}
   */
  static computeVisVivaSpecificOrbitalEnergy(radialDistanceKm, orbitalVelocityKmS) {
    const GM_Sun = 1.32712440018e11; // km^3 / s^2
    const r = Math.max(1e6, radialDistanceKm);
    const v = orbitalVelocityKmS;

    const epsilon = (v * v) / 2.0 - GM_Sun / r; // km^2 / s^2
    const aEquiv = -GM_Sun / (2.0 * epsilon);

    return {
      specificEnergyMjPerKg: parseFloat(epsilon.toFixed(3)),
      semiMajorAxisEquivalentKm: parseFloat(aEquiv.toFixed(0))
    };
  }

  /**
   * Calculate 3D topocentric unit solar vector (East, North, Zenith).
   * @param {number} latitudeDeg - Latitude in degrees
   * @param {number} solarDeclinationDeg - Solar declination in degrees (delta)
   * @param {number} hourAngleDeg - Solar hour angle in degrees (H)
   * @returns {{sEast: number, sNorth: number, sZenith: number, cosZenith: number}}
   */
  static computePlanetocentricSolarZenithVector(latitudeDeg, solarDeclinationDeg, hourAngleDeg) {
    const phi = latitudeDeg * Math.PI / 180.0;
    const delta = solarDeclinationDeg * Math.PI / 180.0;
    const H = hourAngleDeg * Math.PI / 180.0;

    const sEast = -Math.cos(delta) * Math.sin(H);
    const sNorth = Math.sin(delta) * Math.cos(phi) - Math.cos(delta) * Math.sin(phi) * Math.cos(H);
    const sZenith = Math.sin(phi) * Math.sin(delta) + Math.cos(phi) * Math.cos(delta) * Math.cos(H);

    return {
      sEast: parseFloat(sEast.toFixed(5)),
      sNorth: parseFloat(sNorth.toFixed(5)),
      sZenith: parseFloat(sZenith.toFixed(5)),
      cosZenith: parseFloat(Math.max(0, sZenith).toFixed(5))
    };
  }

  // --- Perihelion/Aphelion Distances, Topocentric Solar Azimuth & Mean Motion Solvers ---

  /**
   * Calculate exact Mars perihelion and aphelion orbital distances in AU and km.
   * q = a * (1 - e),  Q = a * (1 + e)
   * @param {number} [semiMajorAxisAU=1.52368] - Mars semi-major axis in AU
   * @param {number} [eccentricity=0.09340] - Mars orbital eccentricity
   * @returns {{perihelionAU: number, perihelionKm: number, aphelionAU: number, aphelionKm: number, orbitalRangeKm: number}}
   */
  static computePerihelionAphelionDistances(semiMajorAxisAU = 1.52368, eccentricity = 0.09340) {
    const a = semiMajorAxisAU;
    const e = eccentricity;
    const AU_TO_KM = 149597870.7;

    const qAU = a * (1.0 - e);
    const QAU = a * (1.0 + e);
    const qKm = qAU * AU_TO_KM;
    const QKm = QAU * AU_TO_KM;

    return {
      perihelionAU: parseFloat(qAU.toFixed(5)),
      perihelionKm: parseFloat(qKm.toFixed(0)),
      aphelionAU: parseFloat(QAU.toFixed(5)),
      aphelionKm: parseFloat(QKm.toFixed(0)),
      orbitalRangeKm: parseFloat((QKm - qKm).toFixed(0))
    };
  }

  /**
   * Calculate topocentric solar azimuth angle from local latitude, solar declination, and hour angle.
   * psi = atan2( sin(H), cos(H) * sin(phi) - tan(delta) * cos(phi) )
   * @param {number} latitudeDeg - Observer latitude in degrees
   * @param {number} solarDeclinationDeg - Solar declination in degrees
   * @param {number} hourAngleDeg - Solar hour angle in degrees
   * @returns {{solarAzimuthDeg: number, isWestOfMeridian: boolean}}
   */
  static computeTopocentricSolarAzimuthAngle(latitudeDeg, solarDeclinationDeg, hourAngleDeg) {
    const phi = latitudeDeg * Math.PI / 180.0;
    const delta = solarDeclinationDeg * Math.PI / 180.0;
    const H = hourAngleDeg * Math.PI / 180.0;

    const y = Math.sin(H);
    const x = Math.cos(H) * Math.sin(phi) - Math.tan(delta) * Math.cos(phi);
    let azDeg = Math.atan2(y, x) * 180.0 / Math.PI;
    azDeg = (azDeg + 180.0) % 360.0; // Azimuth measured from North

    return {
      solarAzimuthDeg: parseFloat(azDeg.toFixed(2)),
      isWestOfMeridian: hourAngleDeg > 0
    };
  }

  /**
   * Calculate Martian orbital mean motion (average angular velocity n).
   * n = sqrt( GM_sun / a^3 ) in rad/s and deg/sol
   * @param {number} [semiMajorAxisAU=1.52368] - Semi-major axis in AU
   * @returns {{meanMotionRadPerSec: number, meanMotionDegPerSol: number, orbitalPeriodSols: number}}
   */
  static computeMartianMeanMotion(semiMajorAxisAU = 1.52368) {
    const aMeters = semiMajorAxisAU * 149597870700.0; // AU in meters
    const GM_Sun = 1.32712440018e20; // m^3 / s^2

    const nRadS = Math.sqrt(GM_Sun / Math.pow(aMeters, 3));
    const solSec = 88775.244;
    const nDegSol = (nRadS * solSec) * 180.0 / Math.PI;
    const periodSols = 360.0 / nDegSol;

    return {
      meanMotionRadPerSec: parseFloat(nRadS.toExponential(4)),
      meanMotionDegPerSol: parseFloat(nDegSol.toFixed(4)),
      orbitalPeriodSols: parseFloat(periodSols.toFixed(1))
    };
  }

  // --- True Anomaly from Eccentric, Distance from Ls & Hour Angle Solvers ---

  /**
   * Calculate exact orbital true anomaly nu from eccentric anomaly E.
   * nu = 2 * atan( sqrt((1 + e) / (1 - e)) * tan(E / 2) )
   * @param {number} eccentricAnomalyDeg - Eccentric anomaly in degrees
   * @param {number} [eccentricity=0.0934] - Orbital eccentricity
   * @returns {{trueAnomalyDeg: number, trueAnomalyRad: number}}
   */
  static computeTrueAnomalyFromEccentric(eccentricAnomalyDeg, eccentricity = 0.0934) {
    const e = Math.max(0, Math.min(0.99, eccentricity));
    const ERad = eccentricAnomalyDeg * Math.PI / 180.0;

    const sqrtFactor = Math.sqrt((1.0 + e) / (1.0 - e));
    const nuRad = 2.0 * Math.atan2(sqrtFactor * Math.sin(ERad / 2.0), Math.cos(ERad / 2.0));
    let nuDeg = nuRad * 180.0 / Math.PI;
    if (nuDeg < 0) nuDeg += 360.0;

    return {
      trueAnomalyDeg: parseFloat(nuDeg.toFixed(4)),
      trueAnomalyRad: parseFloat(((nuDeg * Math.PI / 180.0)).toFixed(5))
    };
  }

  /**
   * Calculate Mars-Sun radial distance directly from Solar Longitude (Ls).
   * r = (a * (1 - e^2)) / (1 + e * cos(Ls - Ls_peri))
   * @param {number} LsDeg - Solar Longitude in degrees (0 to 360)
   * @param {number} [semiMajorAxisAU=1.52368] - Semi-major axis in AU
   * @param {number} [eccentricity=0.0934] - Orbit eccentricity
   * @param {number} [perihelionLsDeg=250.99] - Perihelion solar longitude
   * @returns {{distanceAU: number, distanceKm: number, solarFluxW_M2: number}}
   */
  static computeMarsSunDistanceFromLs(LsDeg, semiMajorAxisAU = 1.52368, eccentricity = 0.0934, perihelionLsDeg = 250.99) {
    const a = semiMajorAxisAU;
    const e = eccentricity;
    const dLamRad = (LsDeg - perihelionLsDeg) * Math.PI / 180.0;

    const rAU = (a * (1.0 - e * e)) / (1.0 + e * Math.cos(dLamRad));
    const rKm = rAU * 149597870.7;
    const flux = this.SOLAR_CONSTANT_1AU / (rAU * rAU);

    return {
      distanceAU: parseFloat(rAU.toFixed(5)),
      distanceKm: parseFloat(rKm.toFixed(0)),
      solarFluxW_M2: parseFloat(flux.toFixed(2))
    };
  }

  /**
   * Calculate signed solar hour angle H in degrees and radians from Local True Solar Time (LTST).
   * H = (LTST - 12) * 15 degrees
   * @param {number} ltstHours - Local True Solar Time in hours (0 to 24)
   * @returns {{hourAngleDeg: number, hourAngleRad: number, isAfternoon: boolean}}
   */
  static computeHourAngleFromLTST(ltstHours) {
    let hDeg = (ltstHours - 12.0) * 15.0;
    while (hDeg > 180) hDeg -= 360;
    while (hDeg < -180) hDeg += 360;
    const hRad = hDeg * Math.PI / 180.0;

    return {
      hourAngleDeg: parseFloat(hDeg.toFixed(2)),
      hourAngleRad: parseFloat(hRad.toFixed(4)),
      isAfternoon: hDeg > 0
    };
  }

  // --- Radial Velocity, Declination Rate & Sol-to-Day Solvers ---

  /**
   * Calculate orbital radial velocity dr/dt of Mars relative to the Sun.
   * dr/dt = (n * a * e * sin(nu)) / sqrt(1 - e^2)
   * @param {number} trueAnomalyDeg - True anomaly in degrees (0 to 360)
   * @param {number} [semiMajorAxisAU=1.52368] - Semi-major axis in AU
   * @param {number} [eccentricity=0.0934] - Orbit eccentricity
   * @returns {{radialVelocityKmS: number, radialVelocityMps: number, isRecedingFromSun: boolean}}
   */
  static computeRadialDistanceRateOfChange(trueAnomalyDeg, semiMajorAxisAU = 1.52368, eccentricity = 0.0934) {
    const aKm = semiMajorAxisAU * 149597870.7; // ~227.94M km
    const e = eccentricity;
    const nuRad = trueAnomalyDeg * Math.PI / 180.0;

    // Mars mean motion n = 2*pi / T_orbit (T_orbit = 686.98 * 86400 s)
    const tOrbitSec = 686.98 * 86400.0;
    const nRadS = (2.0 * Math.PI) / tOrbitSec;

    const dr_dt_KmS = (nRadS * aKm * e * Math.sin(nuRad)) / Math.sqrt(1.0 - e * e);
    const dr_dt_Mps = dr_dt_KmS * 1000.0;

    return {
      radialVelocityKmS: parseFloat(dr_dt_KmS.toFixed(4)),
      radialVelocityMps: parseFloat(dr_dt_Mps.toFixed(2)),
      isRecedingFromSun: dr_dt_KmS > 0
    };
  }

  /**
   * Calculate rate of change of solar declination d(delta)/dt in degrees per sol.
   * d(delta)/dLs = (sin(epsilon) * cos(Ls)) / sqrt(1 - sin^2(epsilon) * sin^2(Ls))
   * @param {number} LsDeg - Solar Longitude in degrees (0 to 360)
   * @param {number} [obliquityDeg=25.19] - Mars planetary obliquity
   * @returns {{declinationRateDegPerSol: number, isApproachingSolstice: boolean}}
   */
  static computeSolarDeclinationRateOfChange(LsDeg, obliquityDeg = 25.19) {
    const epsRad = obliquityDeg * Math.PI / 180.0;
    const LsRad = LsDeg * Math.PI / 180.0;

    const sinEps = Math.sin(epsRad);
    const sinLs = Math.sin(LsRad);
    const cosLs = Math.cos(LsRad);

    const cosDec = Math.sqrt(Math.max(1e-4, 1.0 - sinEps * sinEps * sinLs * sinLs));
    const dDec_dLs = (sinEps * cosLs) / cosDec;

    // Mean motion dLs/dt ~ 360 deg / 668.6 sols ~ 0.5384 deg/sol
    const dLs_dt_degPerSol = 0.5384;
    const rateDegPerSol = dDec_dLs * dLs_dt_degPerSol;

    return {
      declinationRateDegPerSol: parseFloat(rateDegPerSol.toFixed(4)),
      isApproachingSolstice: Math.abs(rateDegPerSol) < 0.1
    };
  }

  /**
   * Convert Martian Sols into Earth standard SI calendar solar days.
   * 1 Sol = 88775.244 seconds = 1.02749125 Earth days
   * @param {number} numSols - Number of Martian sols
   * @returns {{earthDays: number, earthHours: number, totalSeconds: number}}
   */
  static convertMarsSolsToEarthDays(numSols) {
    const s = Math.max(0, numSols);
    const totalSec = s * 88775.244;
    const earthDays = totalSec / 86400.0;
    const earthHours = totalSec / 3600.0;

    return {
      earthDays: parseFloat(earthDays.toFixed(4)),
      earthHours: parseFloat(earthHours.toFixed(2)),
      totalSeconds: parseFloat(totalSec.toFixed(1))
    };
  }

  // --- True Anomaly Angular Rate, Sub-Solar Zenith & Day-to-Sol Solvers ---

  /**
   * Calculate instantaneous orbital true anomaly angular velocity d(nu)/dt in degrees per sol.
   * d(nu)/dt = (h / r^2) = sqrt(GM_sun * a * (1 - e^2)) / r^2
   * @param {number} trueAnomalyDeg - Mars orbital true anomaly in degrees
   * @param {number} [semiMajorAxisAU=1.52368] - Semi-major axis in AU
   * @param {number} [eccentricity=0.09340] - Orbital eccentricity
   * @returns {{trueAnomalyRateDegPerSol: number, trueAnomalyRateRadPerSec: number, distanceKm: number}}
   */
  static computeTrueAnomalyAngularRate(trueAnomalyDeg, semiMajorAxisAU = 1.52368, eccentricity = 0.09340) {
    const GM_Sun = 1.32712440018e11; // km^3 / s^2
    const aKm = semiMajorAxisAU * 149597870.7;
    const e = Math.max(0, Math.min(0.9, eccentricity));
    const nuRad = (trueAnomalyDeg % 360.0) * Math.PI / 180.0;

    // Specific angular momentum h = sqrt(GM * a * (1 - e^2)) in km^2 / s
    const h = Math.sqrt(GM_Sun * aKm * (1.0 - e * e));

    // Distance r = a * (1 - e^2) / (1 + e * cos(nu))
    const rKm = (aKm * (1.0 - e * e)) / (1.0 + e * Math.cos(nuRad));

    // Angular rate d(nu)/dt = h / r^2 (rad/s)
    const dNu_dt_radS = h / (rKm * rKm);
    const dNu_dt_degS = dNu_dt_radS * 180.0 / Math.PI;
    const dNu_dt_degSol = dNu_dt_degS * 88775.244; // deg per sol

    return {
      trueAnomalyRateDegPerSol: parseFloat(dNu_dt_degSol.toFixed(4)),
      trueAnomalyRateRadPerSec: parseFloat(dNu_dt_radS.toExponential(4)),
      distanceKm: parseFloat(rKm.toFixed(0))
    };
  }

  /**
   * Calculate local solar zenith angle directly from target coordinates and sub-solar coordinates.
   * cos(Z) = sin(phi) * sin(phi_sun) + cos(phi) * cos(phi_sun) * cos(lambda - lambda_sun)
   * @param {number} surfaceLatDeg - Surface point planetocentric latitude
   * @param {number} surfaceLonDeg - Surface point East longitude
   * @param {number} subSolarLatDeg - Sub-solar latitude
   * @param {number} subSolarLonDeg - Sub-solar East longitude
   * @returns {{zenithAngleDeg: number, cosZenith: number, isDaylight: boolean}}
   */
  static computeSubSolarZenithAngle(surfaceLatDeg, surfaceLonDeg, subSolarLatDeg, subSolarLonDeg) {
    const phi = surfaceLatDeg * Math.PI / 180.0;
    const phiSun = subSolarLatDeg * Math.PI / 180.0;
    const dLam = (surfaceLonDeg - subSolarLonDeg) * Math.PI / 180.0;

    const cosZ = Math.sin(phi) * Math.sin(phiSun) + Math.cos(phi) * Math.cos(phiSun) * Math.cos(dLam);
    const clampedCosZ = Math.max(-1.0, Math.min(1.0, cosZ));
    const zRad = Math.acos(clampedCosZ);
    const zDeg = zRad * 180.0 / Math.PI;

    return {
      zenithAngleDeg: parseFloat(zDeg.toFixed(3)),
      cosZenith: parseFloat(clampedCosZ.toFixed(4)),
      isDaylight: clampedCosZ > 0
    };
  }

  /**
   * Convert Earth solar days (86400s) into Martian sols (88775.244s).
   * N_sol = N_earth * (86400 / 88775.244)
   * @param {number} numEarthDays - Earth standard days
   * @returns {{marsSols: number, solsFormatted: string}}
   */
  static convertEarthDaysToMarsSols(numEarthDays) {
    const d = Math.max(0, numEarthDays);
    const sols = d * (86400.0 / 88775.244);

    return {
      marsSols: parseFloat(sols.toFixed(4)),
      solsFormatted: `${sols.toFixed(2)} Sols`
    };
  }

  // --- Equation of Center, Sub-Solar Declination & Equation of Time Solvers ---

  /**
   * Calculate Mars orbital Equation of the Center C = nu - M using elliptic Fourier series expansion.
   * C = (2e - e^3/4) * sin(M) + (5/4 * e^2) * sin(2M) + (13/12 * e^3) * sin(3M)
   * @param {number} meanAnomalyDeg - Mean anomaly M in degrees (0 to 360)
   * @param {number} [eccentricity=0.0934] - Orbit eccentricity (0.0934 for Mars)
   * @returns {{equationOfCenterDeg: number, equationOfCenterRad: number, trueAnomalyDeg: number}}
   */
  static computeMarsEquationOfCenter(meanAnomalyDeg, eccentricity = 0.0934) {
    const e = Math.max(0, Math.min(0.5, eccentricity));
    const mRad = meanAnomalyDeg * Math.PI / 180.0;

    const term1 = (2.0 * e - (1.0 / 4.0) * Math.pow(e, 3)) * Math.sin(mRad);
    const term2 = (5.0 / 4.0) * Math.pow(e, 2) * Math.sin(2.0 * mRad);
    const term3 = (13.0 / 12.0) * Math.pow(e, 3) * Math.sin(3.0 * mRad);

    const cRad = term1 + term2 + term3;
    const cDeg = cRad * 180.0 / Math.PI;
    let nuDeg = meanAnomalyDeg + cDeg;
    while (nuDeg < 0) nuDeg += 360;
    while (nuDeg >= 360) nuDeg -= 360;

    return {
      equationOfCenterDeg: parseFloat(cDeg.toFixed(4)),
      equationOfCenterRad: parseFloat(cRad.toFixed(5)),
      trueAnomalyDeg: parseFloat(nuDeg.toFixed(4))
    };
  }

  /**
   * Calculate exact solar declination / sub-solar planetocentric latitude delta_sun from Solar Longitude (Ls).
   * sin(delta_sun) = sin(obliquity) * sin(Ls)
   * @param {number} LsDeg - Solar Longitude in degrees (0 to 360)
   * @param {number} [obliquityDeg=25.19] - Mars axial tilt / obliquity in degrees
   * @returns {{subSolarLatitudeDeg: number, subSolarLatitudeRad: number, isNorthernSummer: boolean}}
   */
  static computeSubSolarDeclination(LsDeg, obliquityDeg = 25.19) {
    const epsRad = obliquityDeg * Math.PI / 180.0;
    const lsRad = LsDeg * Math.PI / 180.0;

    const sinDec = Math.sin(epsRad) * Math.sin(lsRad);
    const decRad = Math.asin(Math.max(-1.0, Math.min(1.0, sinDec)));
    const decDeg = decRad * 180.0 / Math.PI;

    return {
      subSolarLatitudeDeg: parseFloat(decDeg.toFixed(4)),
      subSolarLatitudeRad: parseFloat(decRad.toFixed(5)),
      isNorthernSummer: decDeg > 0
    };
  }

  /**
   * Calculate Mars Equation of Time (EoT = LTST - LMST) in Martian minutes.
   * EoT = (nu - M) * (24 * 60 / 360) = C * 4 min/deg
   * @param {number} LsDeg - Solar Longitude in degrees
   * @param {number} [meanAnomalyDeg=null] - Optional mean anomaly (if null, calculated from Ls - 250.99)
   * @returns {{equationOfTimeMinutes: number, equationOfTimeHours: number, isSunFast: boolean}}
   */
  static computeEquationOfTimeMinutes(LsDeg, meanAnomalyDeg = null) {
    const mDeg = meanAnomalyDeg !== null ? meanAnomalyDeg : (LsDeg - 250.99 + 360) % 360;
    const eqCenter = this.computeMarsEquationOfCenter(mDeg);
    const eotMin = eqCenter.equationOfCenterDeg * 4.0; // 4 minutes per degree of longitude
    const eotHours = eotMin / 60.0;

    return {
      equationOfTimeMinutes: parseFloat(eotMin.toFixed(2)),
      equationOfTimeHours: parseFloat(eotHours.toFixed(4)),
      isSunFast: eotMin > 0
    };
  }

  // --- Right Ascension, LMST/LTST Conversion & Season Solvers ---

  /**
   * Calculate True Solar Right Ascension alpha_sun on the celestial sphere from solar longitude Ls.
   * alpha = atan2( cos(epsilon) * sin(Ls), cos(Ls) )
   * @param {number} LsDeg - Solar Longitude in degrees (0 to 360)
   * @param {number} [obliquityDeg=25.19] - Mars planetary obliquity
   * @returns {{rightAscensionDeg: number, rightAscensionHours: number, rightAscensionRad: number}}
   */
  static computeTrueSolarRightAscension(LsDeg, obliquityDeg = 25.19) {
    const epsRad = obliquityDeg * Math.PI / 180.0;
    const lsRad = LsDeg * Math.PI / 180.0;

    const y = Math.cos(epsRad) * Math.sin(lsRad);
    const x = Math.cos(lsRad);

    let alphaRad = Math.atan2(y, x);
    if (alphaRad < 0) alphaRad += 2.0 * Math.PI;

    const alphaDeg = (alphaRad * 180.0) / Math.PI;
    const alphaHours = alphaDeg / 15.0; // 15 degrees per hour

    return {
      rightAscensionDeg: parseFloat(alphaDeg.toFixed(3)),
      rightAscensionHours: parseFloat(alphaHours.toFixed(4)),
      rightAscensionRad: parseFloat(alphaRad.toFixed(5))
    };
  }

  /**
   * Convert Local Mean Solar Time (LMST) to Local True Solar Time (LTST) in Sol-hours.
   * LTST = LMST + EoT_hours
   * @param {number} lmstHours - Local Mean Solar Time in decimal hours (0 to 24)
   * @param {number} LsDeg - Solar Longitude in degrees (0 to 360)
   * @returns {{ltstHours: number, eotMinutes: number, formattedLTST: string}}
   */
  static convertLMSTtoLTST(lmstHours, LsDeg) {
    const eot = this.computeEquationOfTimeMinutes(LsDeg);
    let ltst = (lmstHours + eot.equationOfTimeHours + 24.0) % 24.0;

    const hh = Math.floor(ltst);
    const mm = Math.floor((ltst - hh) * 60.0);
    const ss = Math.floor(((ltst - hh) * 60.0 - mm) * 60.0);
    const formatted = `${String(hh).padStart(2, '0')}:${String(mm).padStart(2, '0')}:${String(ss).padStart(2, '0')}`;

    return {
      ltstHours: parseFloat(ltst.toFixed(4)),
      eotMinutes: eot.equationOfTimeMinutes,
      formattedLTST: formatted
    };
  }

  /**
   * Retrieve astronomical Mars season metadata, northern and southern seasons, and quadrant progression.
   * @param {number} LsDeg - Solar Longitude in degrees (0 to 360)
   * @returns {{seasonIndex: number, northernSeason: string, southernSeason: string, seasonProgressPercent: number, solQuadrant: string}}
   */
  static getMarsSeasonMetadata(LsDeg) {
    let ls = LsDeg % 360.0;
    if (ls < 0) ls += 360.0;

    let idx = 0;
    let nSeason = 'Spring';
    let sSeason = 'Autumn';
    let quad = 'Q1 (0 - 90 deg)';
    let degInSeason = ls;

    if (ls >= 270.0) {
      idx = 3;
      nSeason = 'Winter';
      sSeason = 'Summer';
      quad = 'Q4 (270 - 360 deg)';
      degInSeason = ls - 270.0;
    } else if (ls >= 180.0) {
      idx = 2;
      nSeason = 'Autumn';
      sSeason = 'Spring';
      quad = 'Q3 (180 - 270 deg)';
      degInSeason = ls - 180.0;
    } else if (ls >= 90.0) {
      idx = 1;
      nSeason = 'Summer';
      sSeason = 'Winter';
      quad = 'Q2 (90 - 180 deg)';
      degInSeason = ls - 90.0;
    }

    const progressPct = (degInSeason / 90.0) * 100.0;

    return {
      seasonIndex: idx,
      northernSeason: nSeason,
      southernSeason: sSeason,
      seasonProgressPercent: parseFloat(progressPct.toFixed(2)),
      solQuadrant: quad
    };
  }
}















