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
    return (jdTT - 2405522.0028779) / 1.027491252 + 44796.0 - 0.00096;
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

    // Mars Year calculation (MY 1 started on 1955-04-11, MSD ~ 28352.0)
    // Approximate Mars year duration = 668.6 sols
    const MY = Math.floor((msd - 28352.0) / 668.6) + 1;

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
}
