/**
 * @module TrajectoryEngine
 * @description Astrodynamics and interplanetary orbital trajectory calculator.
 * Computes Hohmann transfer orbits, patched conics, C3 launch energy,
 * Trans-Mars Injection (TMI) and Mars Orbit Insertion (MOI) Delta-V budgets,
 * transfer flight times, and synodic launch windows.
 */

export class TrajectoryEngine {
  // Heliocentric gravitational parameter (km^3 / s^2)
  static MU_SUN = 1.32712440018e11;

  // Planetary gravitational parameters (km^3 / s^2)
  static MU_BODIES = {
    earth: 398600.4418,
    mars: 42828.3752,
    moon: 4902.8000,
    sun: 1.32712440018e11
  };

  // Heliocentric semi-major axes in AU and km (1 AU = 149,597,870.7 km)
  static AU_KM = 149597870.7;

  static ORBITS = {
    earth: { a_AU: 1.000, a_km: 149597870.7, periodDays: 365.256, radiusKm: 6378.14 },
    mars: { a_AU: 1.524, a_km: 227939200.0, periodDays: 686.980, radiusKm: 3389.5 },
    moon: { a_km: 384400.0, periodDays: 27.321, radiusKm: 1737.4 },
    venus: { a_AU: 0.723, a_km: 108208000.0, periodDays: 224.701, radiusKm: 6051.8 },
    jupiter: { a_AU: 5.204, a_km: 778570000.0, periodDays: 4332.59, radiusKm: 69911.0 }
  };

  /**
   * Compute a heliocentric Hohmann transfer orbit between two celestial bodies.
   * @param {string} fromBody - Departure body (e.g. 'earth')
   * @param {string} toBody - Target body (e.g. 'mars')
   * @param {number} [parkAltFrom=300] - Departure parking orbit altitude in km
   * @param {number} [parkAltTo=300] - Target parking orbit altitude in km
   * @returns {object} Full astrodynamics Delta-V and trajectory solution
   */
  static computeHohmannTransfer(fromBody = 'earth', toBody = 'mars', parkAltFrom = 300, parkAltTo = 300) {
    const origin = TrajectoryEngine.ORBITS[fromBody.toLowerCase()] || TrajectoryEngine.ORBITS.earth;
    const target = TrajectoryEngine.ORBITS[toBody.toLowerCase()] || TrajectoryEngine.ORBITS.mars;

    const r1 = origin.a_km;
    const r2 = target.a_km;
    const muSun = TrajectoryEngine.MU_SUN;

    // Semi-major axis of transfer ellipse (km)
    const a_tx = (r1 + r2) / 2.0;

    // Transfer time (half orbital period) in seconds and days
    const tofSeconds = Math.PI * Math.sqrt(Math.pow(a_tx, 3) / muSun);
    const tofDays = tofSeconds / 86400.0;
    const tofMonths = tofDays / 30.4375;

    // Heliocentric orbital velocities
    const v1_circ = Math.sqrt(muSun / r1);
    const v2_circ = Math.sqrt(muSun / r2);

    // Heliocentric velocities on transfer ellipse
    const v_tx_depart = Math.sqrt(muSun * (2.0 / r1 - 1.0 / a_tx));
    const v_tx_arrive = Math.sqrt(muSun * (2.0 / r2 - 1.0 / a_tx));

    // Hyperbolic excess velocities (v_infinity) in km/s
    const v_inf_depart = Math.abs(v_tx_depart - v1_circ);
    const v_inf_arrive = Math.abs(v_tx_arrive - v2_circ);

    // C3 launch energy (km^2 / s^2)
    const c3 = v_inf_depart * v_inf_depart;

    // Parking orbits (circular velocity and injection delta-V)
    const muOrigin = TrajectoryEngine.MU_BODIES[fromBody.toLowerCase()] || TrajectoryEngine.MU_BODIES.earth;
    const muTarget = TrajectoryEngine.MU_BODIES[toBody.toLowerCase()] || TrajectoryEngine.MU_BODIES.mars;

    const r_park_depart = origin.radiusKm + parkAltFrom;
    const r_park_arrive = target.radiusKm + parkAltTo;

    const v_park_depart = Math.sqrt(muOrigin / r_park_depart);
    const v_park_arrive = Math.sqrt(muTarget / r_park_arrive);

    // Departure burn (e.g. Trans-Mars Injection) from parking orbit
    const v_inj_depart = Math.sqrt(v_inf_depart * v_inf_depart + 2.0 * muOrigin / r_park_depart);
    const deltaV_depart = v_inj_depart - v_park_depart;

    // Arrival insertion burn (e.g. Mars Orbit Insertion) into parking orbit
    const v_inj_arrive = Math.sqrt(v_inf_arrive * v_inf_arrive + 2.0 * muTarget / r_park_arrive);
    const deltaV_arrive = v_inj_arrive - v_park_arrive;

    const totalDeltaV = deltaV_depart + deltaV_arrive;

    // Phase angle required at departure (degrees)
    const targetAngularVel = (2.0 * Math.PI) / (target.periodDays * 86400.0);
    const targetAngleTravelled = targetAngularVel * tofSeconds;
    let phaseAngleDeg = (180.0 - (targetAngleTravelled * 180.0 / Math.PI)) % 360.0;
    if (phaseAngleDeg < 0) phaseAngleDeg += 360.0;

    // Synodic period between origin and target (days)
    const p1 = origin.periodDays;
    const p2 = target.periodDays;
    const synodicPeriodDays = Math.abs(1.0 / (1.0 / p1 - 1.0 / p2));

    return {
      fromBody,
      toBody,
      semiMajorAxisKm: a_tx,
      tofDays,
      tofMonths,
      vInfDepartKmS: v_inf_depart,
      vInfArriveKmS: v_inf_arrive,
      c3LaunchEnergy: c3,
      deltaVDepartKmS: deltaV_depart,
      deltaVArriveKmS: deltaV_arrive,
      totalDeltaVKmS: totalDeltaV,
      departurePhaseAngleDeg: phaseAngleDeg,
      synodicPeriodDays,
      synodicPeriodYears: synodicPeriodDays / 365.25
    };
  }

  /**
   * Calculate future Earth-Mars launch window opportunities.
   * @param {number} [startYear=2024]
   * @param {number} [count=5]
   * @returns {Array<object>} Launch window list
   */
  static getUpcomingMarsLaunchWindows(startYear = 2024, count = 5) {
    // Historical reference launch window: July 2020 (Perseverance / Tianwen-1 / Hope)
    const refEpochMs = new Date('2020-07-20T00:00:00Z').getTime();
    const synodicMs = 779.94 * 86400000; // ~26 months

    const windows = [];
    let currentMs = refEpochMs;

    while (windows.length < count) {
      const winDate = new Date(currentMs);
      if (winDate.getUTCFullYear() >= startYear) {
        const arrivalDate = new Date(currentMs + (259.0 * 86400000));
        windows.push({
          windowIndex: windows.length + 1,
          departureDate: winDate.toISOString().split('T')[0],
          arrivalDate: arrivalDate.toISOString().split('T')[0],
          flightDurationDays: 259,
          c3_typical: 14.5 + Math.sin(windows.length * 1.3) * 3.2,
          tmi_deltaV_km_s: 3.61
        });
      }
      currentMs += synodicMs;
    }

    return windows;
  }
}
