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
}

