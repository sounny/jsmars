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

  // --- Planetary Satellite Orbit Mechanics ---

  /**
   * Calculate circular or elliptical orbital speed using the Vis-Viva equation.
   * @param {number} altitudeKm - Altitude above planetary surface
   * @param {string} [body='mars'] - Celestial body key
   * @param {number} [semiMajorAxisKm=null] - Optional semi-major axis (defaults to circular r)
   * @returns {number} Orbital speed in km/s
   */
  static computeOrbitalSpeed(altitudeKm, body = 'mars', semiMajorAxisKm = null) {
    const bKey = body.toLowerCase();
    const mu = TrajectoryEngine.MU_BODIES[bKey] || TrajectoryEngine.MU_BODIES.mars;
    const rBody = TrajectoryEngine.ORBITS[bKey]?.radiusKm || 3389.5;
    const r = rBody + altitudeKm;
    const a = semiMajorAxisKm || r;

    return Math.sqrt(mu * (2.0 / r - 1.0 / a));
  }

  /**
   * Calculate planetary escape velocity at a given altitude.
   * @param {number} altitudeKm
   * @param {string} [body='mars']
   * @returns {number} Escape velocity in km/s
   */
  static computeEscapeVelocity(altitudeKm, body = 'mars') {
    const bKey = body.toLowerCase();
    const mu = TrajectoryEngine.MU_BODIES[bKey] || TrajectoryEngine.MU_BODIES.mars;
    const rBody = TrajectoryEngine.ORBITS[bKey]?.radiusKm || 3389.5;
    const r = rBody + altitudeKm;

    return Math.sqrt((2.0 * mu) / r);
  }

  /**
   * Calculate orbital period for a satellite in seconds and minutes.
   * @param {number} altitudeKm
   * @param {string} [body='mars']
   * @returns {{periodSeconds: number, periodMinutes: number, periodHours: number}}
   */
  static computeOrbitalPeriod(altitudeKm, body = 'mars') {
    const bKey = body.toLowerCase();
    const mu = TrajectoryEngine.MU_BODIES[bKey] || TrajectoryEngine.MU_BODIES.mars;
    const rBody = TrajectoryEngine.ORBITS[bKey]?.radiusKm || 3389.5;
    const a = rBody + altitudeKm;

    const periodSeconds = 2.0 * Math.PI * Math.sqrt(Math.pow(a, 3) / mu);
    return {
      periodSeconds,
      periodMinutes: periodSeconds / 60.0,
      periodHours: periodSeconds / 3600.0
    };
  }

  /**
   * Compute Areostationary / Geostationary synchronous orbital radius and altitude.
   * @param {string} [body='mars']
   * @returns {{radiusKm: number, altitudeKm: number, speedKmS: number}}
   */
  static computeSynchronousOrbitAltitude(body = 'mars') {
    const bKey = body.toLowerCase();
    const mu = TrajectoryEngine.MU_BODIES[bKey] || TrajectoryEngine.MU_BODIES.mars;
    const rBody = TrajectoryEngine.ORBITS[bKey]?.radiusKm || 3389.5;

    // Rotation periods in seconds (Mars = 24.6229 h, Earth = 23.9344 h)
    const ROTATION_PERIODS_SEC = {
      mars: 88642.66,
      earth: 86164.10,
      moon: 2360584.7
    };

    const T = ROTATION_PERIODS_SEC[bKey] || ROTATION_PERIODS_SEC.mars;
    const radiusKm = Math.cbrt(mu * Math.pow(T / (2.0 * Math.PI), 2));
    const altitudeKm = radiusKm - rBody;
    const speedKmS = Math.sqrt(mu / radiusKm);

    return {
      radiusKm,
      altitudeKm,
      speedKmS
    };
  }

  // --- Keplerian Orbital Propagation & Astrodynamics Solvers ---

  /**
   * Solve Kepler's Equation M = E - e * sin(E) for Eccentric Anomaly (E) via Newton-Raphson.
   * @param {number} meanAnomalyRad - Mean anomaly in radians (M)
   * @param {number} eccentricity - Orbital eccentricity (0 <= e < 1)
   * @param {number} [tolerance=1e-8] - Convergence threshold
   * @returns {number} Eccentric anomaly in radians (E)
   */
  static solveKeplersEquation(meanAnomalyRad, eccentricity, tolerance = 1e-8) {
    const e = Math.max(0, Math.min(0.999, eccentricity));
    let E = meanAnomalyRad; // Initial guess

    for (let iter = 0; iter < 50; iter++) {
      const f = E - e * Math.sin(E) - meanAnomalyRad;
      const fPrime = 1.0 - e * Math.cos(E);
      const delta = f / fPrime;
      E -= delta;
      if (Math.abs(delta) < tolerance) break;
    }

    return E;
  }

  /**
   * Compute True Anomaly (nu) from Eccentric Anomaly (E) and eccentricity.
   * @param {number} eccentricAnomalyRad - Eccentric anomaly in radians (E)
   * @param {number} eccentricity - Orbit eccentricity (e)
   * @returns {number} True anomaly in degrees (0 to 360)
   */
  static computeTrueAnomaly(eccentricAnomalyRad, eccentricity) {
    const e = Math.max(0, Math.min(0.999, eccentricity));
    const E = eccentricAnomalyRad;

    const y = Math.sqrt(1.0 + e) * Math.sin(E / 2.0);
    const x = Math.sqrt(1.0 - e) * Math.cos(E / 2.0);
    let nuRad = 2.0 * Math.atan2(y, x);
    if (nuRad < 0) nuRad += 2.0 * Math.PI;

    return parseFloat((nuRad * 180.0 / Math.PI).toFixed(4));
  }

  /**
   * Compute 3D Cartesian position and velocity in planetary inertial coordinate frame.
   * @param {number} aKm - Semi-major axis in km
   * @param {number} e - Eccentricity
   * @param {number} iDeg - Inclination in degrees
   * @param {number} raanDeg - Right ascension of ascending node (Omega) in degrees
   * @param {number} argPeriDeg - Argument of periapsis (omega) in degrees
   * @param {number} trueAnomalyDeg - True anomaly (nu) in degrees
   * @param {string} [body='mars'] - Planetary body
   * @returns {{positionKm: {x: number, y: number, z: number}, velocityKmS: {vx: number, vy: number, vz: number}, radiusKm: number, speedKmS: number}}
   */
  static computeOrbitalStateVector(aKm, e, iDeg, raanDeg, argPeriDeg, trueAnomalyDeg, body = 'mars') {
    const bKey = body.toLowerCase();
    const mu = TrajectoryEngine.MU_BODIES[bKey] || TrajectoryEngine.MU_BODIES.mars;

    const nuRad = trueAnomalyDeg * Math.PI / 180.0;
    const iRad = iDeg * Math.PI / 180.0;
    const raanRad = raanDeg * Math.PI / 180.0;
    const argPeriRad = argPeriDeg * Math.PI / 180.0;

    // Perifocal coordinates
    const p = aKm * (1.0 - e * e); // Semi-latus rectum
    const r = p / (1.0 + e * Math.cos(nuRad));

    const r_pqw = {
      x: r * Math.cos(nuRad),
      y: r * Math.sin(nuRad),
      z: 0
    };

    const v_factor = Math.sqrt(mu / p);
    const v_pqw = {
      vx: -v_factor * Math.sin(nuRad),
      vy: v_factor * (e + Math.cos(nuRad)),
      vz: 0
    };

    // Rotation from Perifocal (PQW) to Inertial (ECI/MCI) frame
    // R = Rz(-Omega) * Rx(-i) * Rz(-omega)
    const cosO = Math.cos(raanRad), sinO = Math.sin(raanRad);
    const cosi = Math.cos(iRad),    sini = Math.sin(iRad);
    const cosw = Math.cos(argPeriRad), sinw = Math.sin(argPeriRad);

    const Px = cosO * cosw - sinO * sinw * cosi;
    const Py = sinO * cosw + cosO * sinw * cosi;
    const Pz = sinw * sini;

    const Qx = -cosO * sinw - sinO * cosw * cosi;
    const Qy = -sinO * sinw + cosO * cosw * cosi;
    const Qz = cosw * sini;

    const x = r_pqw.x * Px + r_pqw.y * Qx;
    const y = r_pqw.x * Py + r_pqw.y * Qy;
    const z = r_pqw.x * Pz + r_pqw.y * Qz;

    const vx = v_pqw.vx * Px + v_pqw.vy * Qx;
    const vy = v_pqw.vx * Py + v_pqw.vy * Qy;
    const vz = v_pqw.vx * Pz + v_pqw.vy * Qz;

    const speed = Math.sqrt(vx * vx + vy * vy + vz * vz);

    return {
      positionKm: { x: parseFloat(x.toFixed(2)), y: parseFloat(y.toFixed(2)), z: parseFloat(z.toFixed(2)) },
      velocityKmS: { vx: parseFloat(vx.toFixed(4)), vy: parseFloat(vy.toFixed(4)), vz: parseFloat(vz.toFixed(4)) },
      radiusKm: parseFloat(r.toFixed(2)),
      speedKmS: parseFloat(speed.toFixed(4))
    };
  }

  // --- Astrodynamics Energy, Plane Change & Synodic Period Solvers ---

  /**
   * Calculate specific orbital energy (Epsilon) and instantaneous orbital speed from vis-viva equation.
   * @param {number} rKm - Radial distance from planet center (km)
   * @param {number} aKm - Semi-major axis of orbit (km)
   * @param {string} [body='mars'] - Planetary body
   * @returns {{specificEnergyKm2S2: number, speedKmS: number, orbitType: string}}
   */
  static computeOrbitalEnergyAndSpeed(rKm, aKm, body = 'mars') {
    const bKey = body.toLowerCase();
    const mu = TrajectoryEngine.MU_BODIES[bKey] || TrajectoryEngine.MU_BODIES.mars;

    const specificEnergy = -mu / (2.0 * aKm);
    const speed = Math.sqrt(Math.max(0, mu * (2.0 / rKm - 1.0 / aKm)));

    let orbitType = 'Elliptical';
    if (Math.abs(rKm - aKm) < 1e-3) orbitType = 'Circular';
    else if (aKm < 0) orbitType = 'Hyperbolic';

    return {
      specificEnergyKm2S2: parseFloat(specificEnergy.toFixed(4)),
      speedKmS: parseFloat(speed.toFixed(4)),
      orbitType
    };
  }

  /**
   * Calculate Delta-V requirement for a pure orbital inclination / plane change maneuver.
   * DeltaV = 2 * v * sin(Delta_i / 2)
   * @param {number} orbitalSpeedKmS - Current orbital speed in km/s (v)
   * @param {number} deltaIncDeg - Inclination change in degrees (Delta i)
   * @returns {number} Required Delta-V in km/s
   */
  static computePlaneChangeDeltaV(orbitalSpeedKmS, deltaIncDeg) {
    const deltaIRad = deltaIncDeg * Math.PI / 180.0;
    const deltaV = 2.0 * orbitalSpeedKmS * Math.sin(deltaIRad / 2.0);
    return parseFloat(deltaV.toFixed(4));
  }

  /**
   * Calculate exact synodic period between any two planetary orbits.
   * @param {string} [body1='earth'] - First planet
   * @param {string} [body2='mars'] - Second planet
   * @returns {{synodicDays: number, synodicMonths: number, synodicYears: number}}
   */
  static computeInterplanetarySynodicPeriod(body1 = 'earth', body2 = 'mars') {
    const p1 = TrajectoryEngine.ORBITS[body1.toLowerCase()]?.periodDays || 365.256;
    const p2 = TrajectoryEngine.ORBITS[body2.toLowerCase()]?.periodDays || 686.980;

    const synodicDays = Math.abs(1.0 / (1.0 / p1 - 1.0 / p2));

    return {
      synodicDays: parseFloat(synodicDays.toFixed(2)),
      synodicMonths: parseFloat((synodicDays / 30.4375).toFixed(2)),
      synodicYears: parseFloat((synodicDays / 365.256).toFixed(3))
    };
  }

  // --- J2 Nodal Precession, Ground Track Shift & Eclipse Solvers ---

  /**
   * Calculate J2 gravitational secular nodal precession rate dOmega/dt of the orbital ascending node.
   * dOmega/dt = - (3/2) * n * J_2 * (R_p / p)^2 * cos(i)
   * @param {number} semiMajorAxisKm - Orbit semi-major axis in km
   * @param {number} eccentricity - Orbital eccentricity e
   * @param {number} inclinationDeg - Orbital inclination i in degrees
   * @param {string} [body='mars'] - Central planetary body
   * @returns {{nodalPrecessionDegPerDay: number, nodalPrecessionRadPerSec: number, isRetrogradePrecession: boolean}}
   */
  static computeNodalPrecessionRate(semiMajorAxisKm, eccentricity, inclinationDeg, body = 'mars') {
    const bKey = body.toLowerCase();
    const mu = TrajectoryEngine.MU_BODIES[bKey] || TrajectoryEngine.MU_BODIES.mars;
    const Rp = TrajectoryEngine.ORBITS[bKey]?.radiusKm || 3389.5;
    const J2 = bKey === 'earth' ? 1.08263e-3 : 1.96045e-3; // Mars J2 ~ 1.96045e-3

    const a = Math.max(Rp + 10.0, semiMajorAxisKm);
    const e = Math.max(0.0, Math.min(0.95, eccentricity));
    const incRad = (inclinationDeg * Math.PI) / 180.0;

    const n = Math.sqrt(mu / Math.pow(a, 3)); // Mean motion in rad/s
    const p = a * (1.0 - e * e); // Semi-latus rectum in km

    // Secular nodal precession rate in rad/s
    const dOmegaRadS = -1.5 * n * J2 * Math.pow(Rp / p, 2) * Math.cos(incRad);
    // Convert to degrees per day
    const dOmegaDegDay = dOmegaRadS * (180.0 / Math.PI) * 86400.0;

    return {
      nodalPrecessionDegPerDay: parseFloat(dOmegaDegDay.toFixed(4)),
      nodalPrecessionRadPerSec: parseFloat(dOmegaRadS.toExponential(4)),
      isRetrogradePrecession: dOmegaDegDay < 0
    };
  }

  /**
   * Calculate westward longitudinal ground track nodal shift Delta_lambda per orbital revolution.
   * Delta_lambda = ( omega_p - dOmega/dt ) * T_orbit
   * @param {number} orbitalPeriodMinutes - Satellite orbital period in minutes
   * @param {number} [nodalPrecessionDegPerDay=0.0] - Orbit nodal precession rate in deg/day
   * @param {number} [planetaryRotationHours=24.6597] - Planetary rotation period in hours (24.6597 h for Mars)
   * @returns {{longitudinalShiftDeg: number, orbitalPeriodMinutes: number}}
   */
  static computeGroundTrackNodalShift(orbitalPeriodMinutes, nodalPrecessionDegPerDay = 0.0, planetaryRotationHours = 24.6597) {
    const TorbitMin = Math.max(1.0, orbitalPeriodMinutes);
    const ProtHours = Math.max(0.1, planetaryRotationHours);

    // Planet rotation rate in deg/minute
    const omegaP = 360.0 / (ProtHours * 60.0);
    // Precession rate in deg/minute
    const dOmegaMin = nodalPrecessionDegPerDay / 1440.0;

    const shiftDeg = (omegaP - dOmegaMin) * TorbitMin;

    return {
      longitudinalShiftDeg: parseFloat(shiftDeg.toFixed(3)),
      orbitalPeriodMinutes: parseFloat(TorbitMin.toFixed(2))
    };
  }

  /**
   * Calculate planetary cylindrical shadow eclipse fraction and shadow duration.
   * f_eclipse = (1 / pi) * arcsin( R_p / r )
   * @param {number} semiMajorAxisKm - Orbit radius / semi-major axis in km
   * @param {string} [body='mars'] - Central planetary body
   * @returns {{eclipseFraction: number, eclipseDurationMinutes: number, orbitalPeriodMinutes: number}}
   */
  static computeOrbitalEclipseFraction(semiMajorAxisKm, body = 'mars') {
    const bKey = body.toLowerCase();
    const mu = TrajectoryEngine.MU_BODIES[bKey] || TrajectoryEngine.MU_BODIES.mars;
    const Rp = TrajectoryEngine.ORBITS[bKey]?.radiusKm || 3389.5;

    const r = Math.max(Rp + 1.0, semiMajorAxisKm);
    const TorbitSec = 2.0 * Math.PI * Math.sqrt(Math.pow(r, 3) / mu);
    const TorbitMin = TorbitSec / 60.0;

    const halfShadowAngleRad = Math.asin(Math.min(1.0, Rp / r));
    const fEclipse = halfShadowAngleRad / Math.PI;
    const durationMin = fEclipse * TorbitMin;

    return {
      eclipseFraction: parseFloat(fEclipse.toFixed(4)),
      eclipseDurationMinutes: parseFloat(durationMin.toFixed(2)),
      orbitalPeriodMinutes: parseFloat(TorbitMin.toFixed(2))
    };
  }

  // --- Vis-Viva Orbital Velocity, Escape Velocity & Flight Path Angle Solvers ---

  /**
   * Calculate orbital speed from Vis-Viva equation: v = sqrt( mu * (2/r - 1/a) ).
   * @param {number} rKm - Current distance from planet center in km
   * @param {number} aKm - Orbit semi-major axis in km (negative for hyperbolic, Infinity for parabolic)
   * @param {string} [body='mars'] - Planetary body name
   * @returns {{velocityKmS: number, velocityMS: number, isBoundOrbit: boolean}}
   */
  static computeVisVivaVelocity(rKm, aKm, body = 'mars') {
    const bKey = body.toLowerCase();
    const mu = TrajectoryEngine.MU_BODIES[bKey] || TrajectoryEngine.MU_BODIES.mars;
    const r = Math.max(0.1, rKm);

    let v2;
    if (!Number.isFinite(aKm)) {
      v2 = 2.0 * mu / r; // Parabolic trajectory
    } else {
      v2 = mu * (2.0 / r - 1.0 / aKm);
    }
    v2 = Math.max(0.0, v2);

    const vKmS = Math.sqrt(v2);
    const vMS = vKmS * 1000.0;

    return {
      velocityKmS: parseFloat(vKmS.toFixed(4)),
      velocityMS: parseFloat(vMS.toFixed(1)),
      isBoundOrbit: Number.isFinite(aKm) && aKm > 0
    };
  }

  /**
   * Calculate parabolic escape velocity v_esc = sqrt( 2 * mu / r ) from radial distance in km/s.
   * @param {number} rKm - Distance from planet center in km
   * @param {string} [body='mars'] - Planetary body name
   * @returns {{escapeVelocityKmS: number, escapeVelocityMS: number}}
   */
  static computeEscapeVelocityFromRadialDistance(rKm, body = 'mars') {
    const bKey = body.toLowerCase();
    const mu = TrajectoryEngine.MU_BODIES[bKey] || TrajectoryEngine.MU_BODIES.mars;
    const r = Math.max(0.1, rKm);

    const vEscKmS = Math.sqrt((2.0 * mu) / r);
    const vEscMS = vEscKmS * 1000.0;

    return {
      escapeVelocityKmS: parseFloat(vEscKmS.toFixed(4)),
      escapeVelocityMS: parseFloat(vEscMS.toFixed(1))
    };
  }

  /**
   * Calculate orbital flight path angle gamma_fpa (angle between velocity vector and local horizontal).
   * tan(gamma_fpa) = ( e * sin(nu) ) / ( 1 + e * cos(nu) )
   * @param {number} trueAnomalyDeg - True anomaly nu in degrees (0 to 360)
   * @param {number} [eccentricity=0.0] - Orbit eccentricity e (0 for circular)
   * @returns {{flightPathAngleDeg: number, flightPathAngleRad: number, isClimbing: boolean}}
   */
  static computeFlightPathAngle(trueAnomalyDeg, eccentricity = 0.0) {
    const e = Math.max(0.0, eccentricity);
    const nuRad = (trueAnomalyDeg * Math.PI) / 180.0;

    const num = e * Math.sin(nuRad);
    const denom = 1.0 + e * Math.cos(nuRad);

    const gammaRad = Math.atan2(num, Math.max(1e-9, denom));
    const gammaDeg = (gammaRad * 180.0) / Math.PI;

    return {
      flightPathAngleDeg: parseFloat(gammaDeg.toFixed(4)),
      flightPathAngleRad: parseFloat(gammaRad.toFixed(5)),
      isClimbing: gammaDeg > 0.0
    };
  }

  // --- Hohmann Transfer Orbit Budget & Hyperbolic Excess Velocity Solvers ---

  /**
   * Calculate two-impulse Hohmann orbital transfer maneuver between circular orbits.
   * a_tx = (r1 + r2) / 2
   * Delta_v1 = sqrt(mu / r1) * ( sqrt(2*r2 / (r1 + r2)) - 1 )
   * Delta_v2 = sqrt(mu / r2) * ( 1 - sqrt(2*r1 / (r1 + r2)) )
   * t_transfer = pi * sqrt( a_tx^3 / mu )
   * @param {number} r1Km - Initial circular orbit radius in km
   * @param {number} r2Km - Final target circular orbit radius in km
   * @param {string} [body='mars'] - Central planetary body
   * @returns {{deltaV1KmS: number, deltaV2KmS: number, totalDeltaVKmS: number, transferSemiMajorAxisKm: number, transferDurationMinutes: number, transferDurationHours: number}}
   */
  static computeHohmannTransferOrbit(r1Km, r2Km, body = 'mars') {
    const bKey = body.toLowerCase();
    const mu = TrajectoryEngine.MU_BODIES[bKey] || TrajectoryEngine.MU_BODIES.mars;
    const r1 = Math.max(0.1, r1Km);
    const r2 = Math.max(0.1, r2Km);

    const aTx = (r1 + r2) / 2.0;

    const v1 = Math.sqrt(mu / r1);
    const v2 = Math.sqrt(mu / r2);

    const dv1 = Math.abs(v1 * (Math.sqrt((2.0 * r2) / (r1 + r2)) - 1.0));
    const dv2 = Math.abs(v2 * (1.0 - Math.sqrt((2.0 * r1) / (r1 + r2))));
    const totalDv = dv1 + dv2;

    const tSec = Math.PI * Math.sqrt(Math.pow(aTx, 3) / mu);
    const tMin = tSec / 60.0;
    const tHours = tSec / 3600.0;

    return {
      deltaV1KmS: parseFloat(dv1.toFixed(4)),
      deltaV2KmS: parseFloat(dv2.toFixed(4)),
      totalDeltaVKmS: parseFloat(totalDv.toFixed(4)),
      transferSemiMajorAxisKm: parseFloat(aTx.toFixed(2)),
      transferDurationMinutes: parseFloat(tMin.toFixed(2)),
      transferDurationHours: parseFloat(tHours.toFixed(3))
    };
  }

  /**
   * Calculate hyperbolic excess velocity v_inf and characteristic energy C3 = v_inf^2.
   * v_inf = sqrt( v^2 - v_esc^2 )
   * @param {number} velocityKmS - Spacecraft speed at periapsis in km/s
   * @param {number} escapeVelocityKmS - Escape velocity at periapsis in km/s
   * @returns {{vInfinityKmS: number, c3Km2S2: number, isHyperbolic: boolean}}
   */
  static computeHyperbolicExcessVelocity(velocityKmS, escapeVelocityKmS) {
    const v = Math.max(0, velocityKmS);
    const vEsc = Math.max(0, escapeVelocityKmS);

    if (v <= vEsc) {
      return {
        vInfinityKmS: 0.0,
        c3Km2S2: 0.0,
        isHyperbolic: false
      };
    }

    const vInf = Math.sqrt(v * v - vEsc * vEsc);
    const c3 = vInf * vInf;

    return {
      vInfinityKmS: parseFloat(vInf.toFixed(4)),
      c3Km2S2: parseFloat(c3.toFixed(3)),
      isHyperbolic: true
    };
  }

  // Planetary J2 oblateness harmonic coefficients
  static J2_BODIES = {
    earth: 1.08263e-3,
    mars: 1.96045e-3,
    moon: 2.027e-4
  };

  // --- J2 Nodal (RAAN) & Apsidal (Pericenter) Precession Solvers ---

  /**
   * Calculate J2 oblateness nodal precession rate dOmega/dt in degrees per Earth day.
   * dOmega/dt = -1.5 * J2 * (R_body / p)^2 * n * cos(i)
   * @param {number} semiMajorAxisKm - Orbit semi-major axis in km
   * @param {number} eccentricity - Orbit eccentricity (0 to 0.99)
   * @param {number} inclinationDeg - Orbit inclination in degrees (0 to 180)
   * @param {string} [body='mars'] - Central body ('mars' or 'earth')
   * @returns {{nodalPrecessionDegPerDay: number, nodalPrecessionRadPerSec: number, isSunSynchronousCandidate: boolean}}
   */
  static computeJ2NodalPrecessionRate(semiMajorAxisKm, eccentricity, inclinationDeg, body = 'mars') {
    const bKey = body.toLowerCase();
    const mu = TrajectoryEngine.MU_BODIES[bKey] || TrajectoryEngine.MU_BODIES.mars;
    const j2 = TrajectoryEngine.J2_BODIES[bKey] || TrajectoryEngine.J2_BODIES.mars;
    const rBody = (TrajectoryEngine.ORBITS[bKey] && TrajectoryEngine.ORBITS[bKey].radiusKm) || 3389.5;

    const a = Math.max(rBody + 10.0, semiMajorAxisKm);
    const e = Math.max(0.0, Math.min(0.99, eccentricity));
    const incRad = (inclinationDeg * Math.PI) / 180.0;

    const p = a * (1.0 - e * e); // Semi-latus rectum (km)
    const n = Math.sqrt(mu / Math.pow(a, 3)); // Mean motion (rad/s)

    const dOmegaRadSec = -1.5 * j2 * Math.pow(rBody / p, 2) * n * Math.cos(incRad);
    const dOmegaDegDay = (dOmegaRadSec * 180.0 / Math.PI) * 86400.0;

    // Sun-synchronous drift on Mars is ~0.524 deg/day (retrograde inclination > 90°)
    const isSunSync = Math.abs(dOmegaDegDay - 0.524) < 0.05;

    return {
      nodalPrecessionDegPerDay: parseFloat(dOmegaDegDay.toFixed(5)),
      nodalPrecessionRadPerSec: parseFloat(dOmegaRadSec.toExponential(4)),
      isSunSynchronousCandidate: isSunSync
    };
  }

  /**
   * Calculate J2 oblateness apsidal precession (argument of periapsis drift) domega/dt in degrees per Earth day.
   * domega/dt = 0.75 * J2 * (R_body / p)^2 * n * ( 5*cos^2(i) - 1 )
   * @param {number} semiMajorAxisKm - Orbit semi-major axis in km
   * @param {number} eccentricity - Orbit eccentricity (0 to 0.99)
   * @param {number} inclinationDeg - Orbit inclination in degrees (0 to 180)
   * @param {string} [body='mars'] - Central body ('mars' or 'earth')
   * @returns {{apsidalPrecessionDegPerDay: number, isCriticalFrozenInclination: boolean}}
   */
  static computeJ2ApsidalPrecessionRate(semiMajorAxisKm, eccentricity, inclinationDeg, body = 'mars') {
    const bKey = body.toLowerCase();
    const mu = TrajectoryEngine.MU_BODIES[bKey] || TrajectoryEngine.MU_BODIES.mars;
    const j2 = TrajectoryEngine.J2_BODIES[bKey] || TrajectoryEngine.J2_BODIES.mars;
    const rBody = (TrajectoryEngine.ORBITS[bKey] && TrajectoryEngine.ORBITS[bKey].radiusKm) || 3389.5;

    const a = Math.max(rBody + 10.0, semiMajorAxisKm);
    const e = Math.max(0.0, Math.min(0.99, eccentricity));
    const incRad = (inclinationDeg * Math.PI) / 180.0;

    const p = a * (1.0 - e * e);
    const n = Math.sqrt(mu / Math.pow(a, 3));
    const cosInc = Math.cos(incRad);

    const domegaRadSec = 0.75 * j2 * Math.pow(rBody / p, 2) * n * (5.0 * cosInc * cosInc - 1.0);
    const domegaDegDay = (domegaRadSec * 180.0 / Math.PI) * 86400.0;

    // Critical inclination where 5*cos^2(i) - 1 = 0 is 63.435° or 116.565°
    const isFrozen = Math.abs(Math.abs(inclinationDeg) - 63.435) < 0.5 || Math.abs(Math.abs(inclinationDeg) - 116.565) < 0.5;

    return {
      apsidalPrecessionDegPerDay: parseFloat(domegaDegDay.toFixed(5)),
      isCriticalFrozenInclination: isFrozen
    };
  }

  // --- Cartesian State Vectors to Keplerian Orbital Elements Solver ---

  /**
   * Convert 3D Cartesian position r = (x, y, z) and velocity v = (vx, vy, vz) state vectors into Keplerian elements.
   * Energy: eps = v^2 / 2 - mu / r
   * Semi-major axis: a = -mu / (2 * eps)
   * Angular momentum: h = r x v
   * Eccentricity: e = sqrt( 1 + 2 * eps * h^2 / mu^2 )
   * Inclination: i = acos( hz / h )
   * @param {{x: number, y: number, z: number}} rVecKm - Position vector in km
   * @param {{vx: number, vy: number, vz: number}} vVecKmS - Velocity vector in km/s
   * @param {string} [body='mars'] - Central celestial body ('mars' or 'earth')
   * @returns {{semiMajorAxisKm: number, eccentricity: number, inclinationDeg: number, specificEnergyKm2S2: number, angularMomentumKm2S: number, orbitalPeriodMinutes: number, isBoundOrbit: boolean}}
   */
  static computeOrbitalElementsFromStateVectors(rVecKm, vVecKmS, body = 'mars') {
    const bKey = body.toLowerCase();
    const mu = TrajectoryEngine.MU_BODIES[bKey] || TrajectoryEngine.MU_BODIES.mars;

    const rx = rVecKm.x || 0;
    const ry = rVecKm.y || 0;
    const rz = rVecKm.z || 0;
    const r = Math.sqrt(rx * rx + ry * ry + rz * rz);

    const vx = vVecKmS.vx || 0;
    const vy = vVecKmS.vy || 0;
    const vz = vVecKmS.vz || 0;
    const v2 = vx * vx + vy * vy + vz * vz;

    if (r <= 1e-6) {
      return {
        semiMajorAxisKm: 0,
        eccentricity: 0,
        inclinationDeg: 0,
        specificEnergyKm2S2: 0,
        angularMomentumKm2S: 0,
        orbitalPeriodMinutes: 0,
        isBoundOrbit: false
      };
    }

    // Specific orbital energy (km^2 / s^2)
    const eps = (v2 / 2.0) - (mu / r);
    const isBound = eps < -1e-6;

    // Semi-major axis (km)
    const a = isBound ? -mu / (2.0 * eps) : 0.0;

    // Angular momentum vector h = r x v
    const hx = ry * vz - rz * vy;
    const hy = rz * vx - rx * vz;
    const hz = rx * vy - ry * vx;
    const h = Math.sqrt(hx * hx + hy * hy + hz * hz);

    // Eccentricity
    const term = 1.0 + (2.0 * eps * h * h) / (mu * mu);
    const e = Math.sqrt(Math.max(0.0, term));

    // Inclination
    const cosInc = h > 1e-6 ? Math.max(-1.0, Math.min(1.0, hz / h)) : 1.0;
    const incDeg = (Math.acos(cosInc) * 180.0) / Math.PI;

    // Orbital Period T = 2 * pi * sqrt( a^3 / mu )
    let periodMin = 0.0;
    if (isBound && a > 0) {
      const periodSec = 2.0 * Math.PI * Math.sqrt(Math.pow(a, 3) / mu);
      periodMin = periodSec / 60.0;
    }

    return {
      semiMajorAxisKm: parseFloat(a.toFixed(2)),
      eccentricity: parseFloat(e.toFixed(5)),
      inclinationDeg: parseFloat(incDeg.toFixed(3)),
      specificEnergyKm2S2: parseFloat(eps.toFixed(4)),
      angularMomentumKm2S: parseFloat(h.toFixed(2)),
      orbitalPeriodMinutes: parseFloat(periodMin.toFixed(2)),
      isBoundOrbit: isBound
    };
  }

  // --- J2 Planetary Oblateness Perturbations & Sun-Synchronous Orbits ---

  /**
   * Calculate secular nodal precession rate (dOmega/dt) and apsidal precession rate (domega/dt) from J2 oblateness.
   * dOmega/dt = -1.5 * J2 * (R/p)^2 * n * cos(i)
   * domega/dt = 0.75 * J2 * (R/p)^2 * n * ( 5 * cos^2(i) - 1 )
   * @param {number} semiMajorAxisKm - Semi-major axis in km
   * @param {number} eccentricity - Orbital eccentricity (0 to <1)
   * @param {number} inclinationDeg - Orbital inclination in degrees
   * @param {string} [body='mars'] - Planetary body ('mars', 'earth', 'moon')
   * @returns {{nodalPrecessionDegPerDay: number, apsidalPrecessionDegPerDay: number, isCriticalInclination: boolean}}
   */
  static computeJ2NodalAndApsidalPrecession(semiMajorAxisKm, eccentricity, inclinationDeg, body = 'mars') {
    const a = Math.max(100.0, semiMajorAxisKm);
    const e = Math.max(0.0, Math.min(0.99, eccentricity));
    const incRad = (inclinationDeg * Math.PI) / 180.0;

    let mu = 42828.37; // km^3/s^2 for Mars
    let R = 3389.5; // km
    let J2 = 0.00196045; // Mars J2

    if (body.toLowerCase() === 'earth') {
      mu = 398600.4418;
      R = 6378.137;
      J2 = 0.00108263;
    } else if (body.toLowerCase() === 'moon') {
      mu = 4902.8;
      R = 1737.4;
      J2 = 0.0002027;
    }

    const p = a * (1.0 - e * e); // Semi-latus rectum in km
    const n = Math.sqrt(mu / Math.pow(a, 3)); // Mean motion in rad/s

    const factor = 1.5 * J2 * Math.pow(R / p, 2) * n;
    const cosI = Math.cos(incRad);

    const dOmegaSec = -factor * cosI; // rad/s
    const domegaSec = 0.5 * factor * (5.0 * cosI * cosI - 1.0); // rad/s

    const radToDegPerDay = (180.0 / Math.PI) * 86400.0;
    const dOmegaDegDay = dOmegaSec * radToDegPerDay;
    const domegaDegDay = domegaSec * radToDegPerDay;

    // Critical inclination where domega/dt = 0 (i = 63.435° or 116.565°)
    const isCrit = Math.abs(5.0 * cosI * cosI - 1.0) < 0.05;

    return {
      nodalPrecessionDegPerDay: parseFloat(dOmegaDegDay.toFixed(4)),
      apsidalPrecessionDegPerDay: parseFloat(domegaDegDay.toFixed(4)),
      isCriticalInclination: isCrit
    };
  }

  /**
   * Calculate exact retrograde inclination required for a sun-synchronous frozen orbit.
   * cos(i_sso) = -(2 * dOmega_solar * p^2) / (3 * J2 * R^2 * n)
   * @param {number} semiMajorAxisKm - Semi-major axis in km (e.g. 3645 km for MRO)
   * @param {number} [eccentricity=0.001] - Orbital eccentricity
   * @param {string} [body='mars'] - Planetary body
   * @returns {{sunSyncInclinationDeg: number, isFeasibleSunSync: boolean}}
   */
  static computeSunSynchronousInclination(semiMajorAxisKm, eccentricity = 0.001, body = 'mars') {
    const a = Math.max(100.0, semiMajorAxisKm);
    const e = Math.max(0.0, Math.min(0.99, eccentricity));

    let mu = 42828.37;
    let R = 3389.5;
    let J2 = 0.00196045;
    // Mean solar precession rate dOmega_solar in rad/s:
    // Mars: 2*pi / (686.98 * 86400) = 1.0583e-7 rad/s (0.5240 deg/day)
    let dOmegaSolar = (2.0 * Math.PI) / (686.98 * 86400.0);

    if (body.toLowerCase() === 'earth') {
      mu = 398600.4418;
      R = 6378.137;
      J2 = 0.00108263;
      dOmegaSolar = (2.0 * Math.PI) / (365.2422 * 86400.0); // 0.9856 deg/day
    }

    const p = a * (1.0 - e * e);
    const n = Math.sqrt(mu / Math.pow(a, 3));

    const num = -2.0 * dOmegaSolar * Math.pow(p, 2);
    const den = 3.0 * J2 * Math.pow(R, 2) * n;
    const cosI = num / den;

    const isFeasible = cosI >= -1.0 && cosI <= 1.0;
    const clampedCos = Math.max(-1.0, Math.min(1.0, cosI));
    const incDeg = (Math.acos(clampedCos) * 180.0) / Math.PI;

    return {
      sunSyncInclinationDeg: parseFloat(incDeg.toFixed(3)),
      isFeasibleSunSync: isFeasible
    };
  }

  // --- Ground Track Velocity & Interplanetary Hohmann Transfers ---

  /**
   * Calculate relative satellite ground track speed across the rotating planetary surface.
   * v_ground = sqrt( v_inertial^2 + v_rot^2 - 2 * v_inertial * v_rot * cos(i) )
   * @param {number} orbitalRadiusKm - Orbital radius r in km (planet radius + altitude)
   * @param {number} inclinationDeg - Orbital inclination in degrees
   * @param {number} [latitudeDeg=0.0] - Instantaneous satellite latitude in degrees
   * @param {string} [body='mars'] - Planetary body
   * @returns {{groundTrackSpeedKmS: number, inertialOrbitalSpeedKmS: number, planetarySurfaceSpeedKmS: number}}
   */
  static computeSatelliteGroundTrackVelocity(orbitalRadiusKm, inclinationDeg, latitudeDeg = 0.0, body = 'mars') {
    const r = Math.max(100.0, orbitalRadiusKm);
    const iRad = (inclinationDeg * Math.PI) / 180.0;
    const latRad = (latitudeDeg * Math.PI) / 180.0;

    let mu = 42828.37;
    let R = 3389.5;
    let TrotSec = 88775.244; // 1 Martian sol in seconds

    if (body.toLowerCase() === 'earth') {
      mu = 398600.4418;
      R = 6378.137;
      TrotSec = 86164.09; // 1 sidereal day
    }

    const vInertial = Math.sqrt(mu / r); // km/s
    const omegaPlanet = (2.0 * Math.PI) / TrotSec; // rad/s
    const vRot = omegaPlanet * R * Math.cos(latRad); // km/s at planetary surface

    // Law of cosines for vector difference: v_ground = |v_inertial - v_rot|
    const vGroundSq = vInertial * vInertial + vRot * vRot - 2.0 * vInertial * vRot * Math.cos(iRad);
    const vGround = Math.sqrt(Math.max(0.0, vGroundSq));

    return {
      groundTrackSpeedKmS: parseFloat(vGround.toFixed(4)),
      inertialOrbitalSpeedKmS: parseFloat(vInertial.toFixed(4)),
      planetarySurfaceSpeedKmS: parseFloat(vRot.toFixed(4))
    };
  }

  /**
   * Calculate minimum-energy Hohmann transfer orbit delta-V and transit time between planetary orbits.
   * Delta_v1 = sqrt(mu_sun/r1) * ( sqrt(2*r2 / (r1 + r2)) - 1 )
   * Delta_v2 = sqrt(mu_sun/r2) * ( 1 - sqrt(2*r1 / (r1 + r2)) )
   * @param {number} [r1AU=1.0] - Departure heliocentric orbital distance in AU (1.0 for Earth)
   * @param {number} [r2AU=1.52368] - Arrival heliocentric orbital distance in AU (1.52368 for Mars)
   * @returns {{departureDeltaVKmS: number, arrivalDeltaVKmS: number, totalDeltaVKmS: number, transitTimeDays: number, transferSemiMajorAxisAU: number}}
   */
  static computeHohmannInterplanetaryTransfer(r1AU = 1.0, r2AU = 1.52368) {
    const AU_KM = 149597870.7;
    const MU_SUN = 1.32712440018e11; // km^3 / s^2

    const r1 = Math.max(0.1, r1AU) * AU_KM;
    const r2 = Math.max(0.1, r2AU) * AU_KM;

    const aTx = (r1 + r2) / 2.0;

    const v1Circ = Math.sqrt(MU_SUN / r1);
    const v2Circ = Math.sqrt(MU_SUN / r2);

    const v1Tx = Math.sqrt(MU_SUN * (2.0 / r1 - 1.0 / aTx));
    const v2Tx = Math.sqrt(MU_SUN * (2.0 / r2 - 1.0 / aTx));

    const dv1 = Math.abs(v1Tx - v1Circ);
    const dv2 = Math.abs(v2Circ - v2Tx);
    const dvTotal = dv1 + dv2;

    const transitSec = Math.PI * Math.sqrt(Math.pow(aTx, 3) / MU_SUN);
    const transitDays = transitSec / 86400.0;

    return {
      departureDeltaVKmS: parseFloat(dv1.toFixed(3)),
      arrivalDeltaVKmS: parseFloat(dv2.toFixed(3)),
      totalDeltaVKmS: parseFloat(dvTotal.toFixed(3)),
      transitTimeDays: parseFloat(transitDays.toFixed(1)),
      transferSemiMajorAxisAU: parseFloat((aTx / AU_KM).toFixed(4))
    };
  }

  // --- Ground Track Swath Overlap & Sol Repeat Solvers ---

  /**
   * Calculate satellite instrument swath overlap and gap coverage across planetary latitudes.
   * Delta_x(phi) = Delta_x_eq * cos(phi)
   * Overlap = 1.0 - Delta_x(phi) / Swath_width
   * @param {number} swathWidthKm - Instrument cross-track swath width in km (e.g. 30 km for CTX, 20 km for THEMIS)
   * @param {number} equatorialSpacingKm - Ground track longitudinal track spacing at equator in km
   * @param {number} [latitudeDeg=0.0] - Latitude phi in degrees (-90 to +90)
   * @returns {{overlapFraction: number, overlapPercent: number, overlapKm: number, trackSpacingKm: number, isSeamlessCoverage: boolean}}
   */
  static computeGroundTrackSwathOverlap(swathWidthKm, equatorialSpacingKm, latitudeDeg = 0.0) {
    const w = Math.max(0.01, swathWidthKm);
    const dxEq = Math.max(0.0, equatorialSpacingKm);
    const phiRad = (Math.min(90.0, Math.max(-90.0, latitudeDeg)) * Math.PI) / 180.0;

    const dxLat = dxEq * Math.cos(phiRad);
    const overlapDist = Math.max(0.0, w - dxLat);
    const overlapFrac = Math.max(0.0, Math.min(1.0, 1.0 - (dxLat / w)));

    return {
      overlapFraction: parseFloat(overlapFrac.toFixed(4)),
      overlapPercent: parseFloat((overlapFrac * 100.0).toFixed(2)),
      overlapKm: parseFloat(overlapDist.toFixed(2)),
      trackSpacingKm: parseFloat(dxLat.toFixed(2)),
      isSeamlessCoverage: w >= dxLat
    };
  }

  /**
   * Calculate orbital period and daily ground track repeat cycles (revolutions per sol).
   * T = 2 * pi * sqrt( a^3 / mu )
   * N_sol = P_sol / T
   * @param {number} semiMajorAxisKm - Orbit semi-major axis in km (e.g. 3645 km for MRO)
   * @param {string} [body='mars'] - Planetary body
   * @returns {{periodMinutes: number, periodHours: number, revolutionsPerSol: number, groundTrackEquatorialDriftDeg: number}}
   */
  static computeOrbitPeriodAndRevolutionsPerSol(semiMajorAxisKm, body = 'mars') {
    const bKey = body.toLowerCase();
    const mu = TrajectoryEngine.MU_BODIES[bKey] || TrajectoryEngine.MU_BODIES.mars;
    const solSec = (TrajectoryEngine.ORBITS[bKey] && TrajectoryEngine.ORBITS[bKey].rotationPeriodSec) || 88775.244;

    const a = Math.max(100.0, semiMajorAxisKm);
    const periodSec = 2.0 * Math.PI * Math.sqrt(Math.pow(a, 3) / mu);
    const periodMin = periodSec / 60.0;
    const periodH = periodSec / 3600.0;

    const revsPerSol = solSec / periodSec;
    // Longitudinal shift of successive ground tracks at equator (degrees West per rev):
    const driftDeg = (periodSec / solSec) * 360.0;

    return {
      periodMinutes: parseFloat(periodMin.toFixed(2)),
      periodHours: parseFloat(periodH.toFixed(3)),
      revolutionsPerSol: parseFloat(revsPerSol.toFixed(3)),
      groundTrackEquatorialDriftDeg: parseFloat(driftDeg.toFixed(3))
    };
  }

  /**
   * Calculate planetary sub-solar point (latitude/declination, longitude) and target Solar Zenith Angle (SZA).
   * sin(delta_sun) = sin(obliquity) * sin(Ls)
   * cos(theta_z) = sin(lat) * sin(delta_sun) + cos(lat) * cos(delta_sun) * cos(lon - lon_sun)
   * SZA = arccos(cos(theta_z))
   * @param {number} solarLongitudeLsDeg - Areocentric solar longitude L_s in degrees (0 to 360)
   * @param {number} [targetLatDeg=0.0] - Target point latitude in degrees (-90 to +90)
   * @param {number} [targetLonDeg=0.0] - Target point longitude in degrees (-180 to +180 or 0 to 360)
   * @param {number} [localSolarTimeHours=12.0] - Local solar time at sub-solar reference meridian (hours, 0 to 24)
   * @param {string} [body='mars'] - Planetary body ('mars', 'moon', 'earth')
   * @returns {{subSolarLatitudeDeg: number, subSolarLongitudeDeg: number, solarZenithAngleDeg: number, solarElevationDeg: number, isDaylight: boolean}}
   */
  static computeSubSolarPointAndZenithAngle(solarLongitudeLsDeg, targetLatDeg = 0.0, targetLonDeg = 0.0, localSolarTimeHours = 12.0, body = 'mars') {
    const bKey = body.toLowerCase();
    let obliquityDeg = 25.19; // Mars
    if (bKey === 'earth') obliquityDeg = 23.44;
    else if (bKey === 'moon') obliquityDeg = 1.54;

    const lsRad = (solarLongitudeLsDeg * Math.PI) / 180.0;
    const obliqRad = (obliquityDeg * Math.PI) / 180.0;

    // Declination delta_sun
    const sinDelta = Math.sin(obliqRad) * Math.sin(lsRad);
    const deltaRad = Math.asin(Math.max(-1.0, Math.min(1.0, sinDelta)));
    const subSolarLatDeg = (deltaRad * 180.0) / Math.PI;

    // Sub-solar longitude (where solar time is 12:00 noon)
    // lon_sun = targetLon + (12.0 - lst) * 15.0
    const lonShiftDeg = (12.0 - localSolarTimeHours) * 15.0;
    let subSolarLonDeg = (targetLonDeg + lonShiftDeg) % 360.0;
    if (subSolarLonDeg > 180.0) subSolarLonDeg -= 360.0;
    if (subSolarLonDeg < -180.0) subSolarLonDeg += 360.0;

    // Target SZA
    const phiRad = (targetLatDeg * Math.PI) / 180.0;
    const dLonRad = ((targetLonDeg - subSolarLonDeg) * Math.PI) / 180.0;

    const cosZ = Math.sin(phiRad) * Math.sin(deltaRad) + Math.cos(phiRad) * Math.cos(deltaRad) * Math.cos(dLonRad);
    const clampedCosZ = Math.max(-1.0, Math.min(1.0, cosZ));
    const szaRad = Math.acos(clampedCosZ);
    const szaDeg = (szaRad * 180.0) / Math.PI;
    const elevDeg = 90.0 - szaDeg;

    return {
      subSolarLatitudeDeg: parseFloat(subSolarLatDeg.toFixed(2)),
      subSolarLongitudeDeg: parseFloat(subSolarLonDeg.toFixed(2)),
      solarZenithAngleDeg: parseFloat(szaDeg.toFixed(2)),
      solarElevationDeg: parseFloat(elevDeg.toFixed(2)),
      isDaylight: szaDeg < 90.0
    };
  }

  /**
   * Calculate orbital J2 gravitational nodal precession rate (deg/day) and Sun-synchronous LTAN drift.
   * dOmega/dt = -1.5 * J2 * (R_eq / p)^2 * n * cos(i)
   * Reference: Vallado (2013), Albee et al. (2001) for MGS, Zurek & Smrekar (2007) for MRO.
   * @param {number} semiMajorAxisKm - Semi-major axis a in km (e.g. 3646 km for MRO, 3775 km for Odyssey)
   * @param {number} inclinationDeg - Orbital inclination i in degrees (e.g. 92.8 deg for MRO)
   * @param {number} [eccentricity=0.001] - Orbital eccentricity e
   * @param {string} [body='mars'] - Planetary body ('mars', 'earth')
   * @returns {{nodalPrecessionDegPerDay: number, sunSyncPrecessionRateDegPerDay: number, ltanDriftMinutesPerSol: number, sunSyncRequiredInclinationDeg: number, isSunSynchronous: boolean}}
   */
  static computeSunSynchronousNodalPrecessionAndLTANDrift(semiMajorAxisKm, inclinationDeg, eccentricity = 0.001, body = 'mars') {
    const bKey = body.toLowerCase();
    const isEarth = bKey === 'earth';

    const mu = isEarth ? 398600.4418 : 42828.37; // km^3/s^2
    const Req = isEarth ? 6378.137 : 3396.19;    // km
    const J2 = isEarth ? 1.08263e-3 : 1.96045e-3;
    const yearDays = isEarth ? 365.256 : 686.98;
    const syncRateDegDay = 360.0 / yearDays; // ~0.524 deg/day Mars, 0.9856 deg/day Earth

    const a = Math.max(100.0, semiMajorAxisKm);
    const e = Math.min(0.95, Math.max(0.0, eccentricity));
    const p = a * (1.0 - e * e); // km
    const n = Math.sqrt(mu / Math.pow(a, 3.0)); // rad/s

    const iRad = (inclinationDeg * Math.PI) / 180.0;

    // dOmega/dt in rad/s:
    const dOmegaRadSec = -1.5 * J2 * Math.pow(Req / p, 2.0) * n * Math.cos(iRad);
    const dOmegaDegDay = dOmegaRadSec * (180.0 / Math.PI) * 86400.0;

    // Required sun-sync inclination:
    const syncRadSec = syncRateDegDay * (Math.PI / 180.0) / 86400.0;
    const cosISync = -syncRadSec / (1.5 * J2 * Math.pow(Req / p, 2.0) * n);
    const clampedCosISync = Math.max(-1.0, Math.min(1.0, cosISync));
    const reqISyncDeg = (Math.acos(clampedCosISync) * 180.0) / Math.PI;

    // Drift in LTAN (minutes per Earth/Mars day: 1 deg ~ 4 minutes)
    const driftDegDay = dOmegaDegDay - syncRateDegDay;
    const ltanDriftMin = driftDegDay * 4.0;

    const isSunSync = Math.abs(dOmegaDegDay - syncRateDegDay) < 0.05;

    return {
      nodalPrecessionDegPerDay: parseFloat(dOmegaDegDay.toFixed(4)),
      sunSyncPrecessionRateDegPerDay: parseFloat(syncRateDegDay.toFixed(4)),
      ltanDriftMinutesPerSol: parseFloat(ltanDriftMin.toFixed(3)),
      sunSyncRequiredInclinationDeg: parseFloat(reqISyncDeg.toFixed(2)),
      isSunSynchronous: isSunSync
    };
  }

  /**
   * Calculate orbital eclipse geometry, shadow entry/exit arc, and umbra duration per orbit.
   * beta_crit = arcsin( R_planet / a )
   * Delta_theta_umbra = 2 * arcsin( sqrt( R_p^2 - a^2 * sin^2(beta) ) / (a * cos(beta)) )
   * t_umbra = ( Delta_theta_umbra / 2*pi ) * T_orbit
   * Reference: Wertz & Larson (1999) SMAD, Vallado (2013).
   * @param {number} orbitAltitudeKm - Orbital altitude h above mean surface in km (e.g. 250 km for MRO)
   * @param {number} [betaAngleDeg=0.0] - Orbit beta angle (angle between orbit plane and sun vector, -90 to +90 deg)
   * @param {string} [body='mars'] - Planetary body ('mars', 'moon', 'earth')
   * @returns {{umbraDurationMinutes: number, orbitPeriodMinutes: number, eclipseFractionPct: number, criticalBetaAngleDeg: number, isInFullSunlight: boolean}}
   */
  static computeOrbitalEclipseUmbraAndPenumbraDuration(orbitAltitudeKm, betaAngleDeg = 0.0, body = 'mars') {
    const bKey = body.toLowerCase();
    const isEarth = bKey === 'earth';
    const isMoon = bKey === 'moon';

    let mu = 42828.37;
    let Rp = 3396.19;
    if (isEarth) {
      mu = 398600.4418;
      Rp = 6378.137;
    } else if (isMoon) {
      mu = 4902.8;
      Rp = 1737.4;
    }

    const h = Math.max(10.0, orbitAltitudeKm);
    const a = Rp + h;
    const periodSec = 2.0 * Math.PI * Math.sqrt(Math.pow(a, 3.0) / mu);
    const periodMin = periodSec / 60.0;

    const betaRad = (Math.min(90.0, Math.max(-90.0, betaAngleDeg)) * Math.PI) / 180.0;
    const sinBetaCrit = Rp / a;
    const betaCritRad = Math.asin(Math.min(1.0, sinBetaCrit));
    const betaCritDeg = (betaCritRad * 180.0) / Math.PI;

    if (Math.abs(betaAngleDeg) >= betaCritDeg) {
      return {
        umbraDurationMinutes: 0.0,
        orbitPeriodMinutes: parseFloat(periodMin.toFixed(2)),
        eclipseFractionPct: 0.0,
        criticalBetaAngleDeg: parseFloat(betaCritDeg.toFixed(2)),
        isInFullSunlight: true
      };
    }

    // Shadow arc angle
    const cosBeta = Math.cos(betaRad);
    const sinBeta = Math.sin(betaRad);
    const num = Math.sqrt(Math.max(0.0, Rp * Rp - a * a * sinBeta * sinBeta));
    const den = a * cosBeta;
    const halfShadowArcRad = Math.asin(Math.min(1.0, num / den));
    const totalShadowArcRad = 2.0 * halfShadowArcRad;

    const eclipseFrac = totalShadowArcRad / (2.0 * Math.PI);
    const umbraMin = periodMin * eclipseFrac;

    return {
      umbraDurationMinutes: parseFloat(umbraMin.toFixed(2)),
      orbitPeriodMinutes: parseFloat(periodMin.toFixed(2)),
      eclipseFractionPct: parseFloat((eclipseFrac * 100.0).toFixed(2)),
      criticalBetaAngleDeg: parseFloat(betaCritDeg.toFixed(2)),
      isInFullSunlight: false
    };
  }

  /**
   * Calculate spacecraft-to-ground-station / lander topocentric elevation angle, slant range, and line-of-sight visibility.
   * tan(El) = ( cos(sigma) - R / (R + h) ) / sin(sigma)
   * rho = sqrt( R^2 + (R + h)^2 - 2 * R * (R + h) * cos(sigma) )
   * Reference: Vallado (2013), Wertz & Larson (1999) SMAD.
   * @param {number} satLatDeg - Sub-satellite latitude in degrees (-90 to +90)
   * @param {number} satLonDeg - Sub-satellite longitude in degrees (-180 to +180 or 0 to 360)
   * @param {number} satAltKm - Satellite orbital altitude in km above mean radius
   * @param {number} stationLatDeg - Ground station / lander latitude in degrees
   * @param {number} stationLonDeg - Ground station / lander longitude in degrees
   * @param {number} [minElevationMaskDeg=5.0] - Minimum elevation angle threshold for reliable RF link
   * @param {string} [body='mars'] - Planetary body ('mars', 'moon', 'earth')
   * @returns {{elevationAngleDeg: number, slantRangeKm: number, centralAngularDistanceDeg: number, isLineOfSightVisible: boolean, groundTrackDistanceKm: number}}
   */
  static computeGroundStationPassGeometryAndElevation(satLatDeg, satLonDeg, satAltKm, stationLatDeg, stationLonDeg, minElevationMaskDeg = 5.0, body = 'mars') {
    const bKey = body.toLowerCase();
    let R = 3396.19; // Mars mean radius km
    if (bKey === 'earth') R = 6378.137;
    else if (bKey === 'moon') R = 1737.4;

    const h = Math.max(0.1, satAltKm);
    const rSat = R + h;

    const phiS = (satLatDeg * Math.PI) / 180.0;
    const lamS = (satLonDeg * Math.PI) / 180.0;
    const phiG = (stationLatDeg * Math.PI) / 180.0;
    const lamG = (stationLonDeg * Math.PI) / 180.0;

    // Central angular distance sigma
    const cosSigma = Math.sin(phiS) * Math.sin(phiG) + Math.cos(phiS) * Math.cos(phiG) * Math.cos(lamS - lamG);
    const clampedCosSigma = Math.max(-1.0, Math.min(1.0, cosSigma));
    const sigmaRad = Math.acos(clampedCosSigma);
    const sigmaDeg = (sigmaRad * 180.0) / Math.PI;

    // Slant range rho
    const rhoKm = Math.sqrt(Math.max(1e-3, R * R + rSat * rSat - 2.0 * R * rSat * clampedCosSigma));

    // Topocentric elevation angle El
    let elDeg = -90.0;
    if (sigmaRad < 1e-6) {
      elDeg = 90.0; // Directly overhead zenith
    } else {
      const sinSigma = Math.sin(sigmaRad);
      const tanEl = (clampedCosSigma - (R / rSat)) / sinSigma;
      const elRad = Math.atan(tanEl);
      elDeg = (elRad * 180.0) / Math.PI;
    }

    const groundTrackKm = sigmaRad * R;
    const isVisible = elDeg >= minElevationMaskDeg;

    return {
      elevationAngleDeg: parseFloat(elDeg.toFixed(2)),
      slantRangeKm: parseFloat(rhoKm.toFixed(2)),
      centralAngularDistanceDeg: parseFloat(sigmaDeg.toFixed(2)),
      isLineOfSightVisible: isVisible,
      groundTrackDistanceKm: parseFloat(groundTrackKm.toFixed(1))
    };
  }

  /**
   * Calculate heliocentric Earth-Mars distance, One-Way Light Time (OWLT), and Sun-Earth-Probe (SEP) conjunction geometry.
   * r_mars(Ls) = a * (1 - e^2) / ( 1 + e * cos(Ls - 251 deg) )
   * OWLT = d_EM / c
   * Reference: Allison & McEwen (2000), Vallado (2013).
   * @param {number} solarLongitudeLsDeg - Areocentric solar longitude L_s in degrees (0 to 360)
   * @param {number} [phaseOffsetDeg=0.0] - Relative heliocentric longitude difference between Earth and Mars (0 = opposition, 180 = superior conjunction)
   * @returns {{heliocentricMarsDistanceAU: number, earthMarsDistanceAU: number, earthMarsDistanceKm: number, oneWayLightTimeMinutes: number, twoWayLightTimeMinutes: number, sepAngleDeg: number, isSolarConjunctionBlackout: boolean}}
   */
  static computeEarthSunProbeAndAntennaPointingGeometry(solarLongitudeLsDeg, phaseOffsetDeg = 0.0) {
    const aMars = 1.52368; // AU
    const eMars = 0.0934;
    const perihelionLs = 251.0; // deg

    const lsRad = (solarLongitudeLsDeg * Math.PI) / 180.0;
    const periRad = (perihelionLs * Math.PI) / 180.0;
    const nu = lsRad - periRad;

    // Mars heliocentric distance in AU
    const rMars = (aMars * (1.0 - eMars * eMars)) / (1.0 + eMars * Math.cos(nu));
    const rEarth = 1.0; // AU

    const dLonRad = (phaseOffsetDeg * Math.PI) / 180.0;

    // Law of cosines for Earth-Mars distance d_EM
    const dEMAU = Math.sqrt(Math.max(0.01, rMars * rMars + rEarth * rEarth - 2.0 * rMars * rEarth * Math.cos(dLonRad)));
    const dEMKm = dEMAU * 149597870.7; // 1 AU in km

    // Speed of light c = 299,792.458 km/s -> ~499.005 s / AU = 8.31675 min / AU
    const owltMin = dEMAU * 8.316746;
    const twltMin = owltMin * 2.0;

    // Sun-Earth-Probe (SEP) angle: angle at Earth between Sun and Mars
    // By law of sines / cosines:
    const cosSEP = (rEarth * rEarth + dEMAU * dEMAU - rMars * rMars) / (2.0 * rEarth * dEMAU);
    const clampedCosSEP = Math.max(-1.0, Math.min(1.0, cosSEP));
    const sepRad = Math.acos(clampedCosSEP);
    const sepDeg = (sepRad * 180.0) / Math.PI;

    const isBlackout = sepDeg <= 3.0; // Solar conjunction blackout

    return {
      heliocentricMarsDistanceAU: parseFloat(rMars.toFixed(4)),
      earthMarsDistanceAU: parseFloat(dEMAU.toFixed(4)),
      earthMarsDistanceKm: parseFloat(dEMKm.toFixed(0)),
      oneWayLightTimeMinutes: parseFloat(owltMin.toFixed(2)),
      twoWayLightTimeMinutes: parseFloat(twltMin.toFixed(2)),
      sepAngleDeg: parseFloat(sepDeg.toFixed(2)),
      isSolarConjunctionBlackout: isBlackout
    };
  }

  /**
   * Solve Kepler's equation M = E - e * sin(E) via Newton-Raphson and compute true anomaly, radius, and orbital speed.
   * Reference: Battin (1999), Vallado (2013).
   * @param {number} meanAnomalyDeg - Mean anomaly M in degrees (0 to 360)
   * @param {number} eccentricity - Orbital eccentricity e (0 <= e < 1)
   * @param {number} semiMajorAxisKm - Semi-major axis a in km (e.g. 5000 km for MAVEN / Mars Express)
   * @param {string} [body='mars'] - Planetary body ('mars', 'moon', 'earth')
   * @returns {{trueAnomalyDeg: number, eccentricAnomalyDeg: number, orbitalRadiusKm: number, orbitalAltitudeKm: number, orbitalVelocityKmS: number, periapsisAltitudeKm: number, apoapsisAltitudeKm: number}}
   */
  static computeKeplerOrbitPositionFromMeanAnomaly(meanAnomalyDeg, eccentricity, semiMajorAxisKm, body = 'mars') {
    const bKey = body.toLowerCase();
    let mu = 42828.37;
    let Rp = 3396.19;
    if (bKey === 'earth') {
      mu = 398600.4418;
      Rp = 6378.137;
    } else if (bKey === 'moon') {
      mu = 4902.8;
      Rp = 1737.4;
    }

    const a = Math.max(100.0, semiMajorAxisKm);
    const e = Math.min(0.98, Math.max(0.0, eccentricity));

    // Normalize M to [0, 2*pi)
    let MRad = ((meanAnomalyDeg % 360.0 + 360.0) % 360.0) * (Math.PI / 180.0);

    // Initial guess for Newton-Raphson
    let E = MRad + e * Math.sin(MRad);
    for (let iter = 0; iter < 20; iter++) {
      const f = E - e * Math.sin(E) - MRad;
      const fPrime = 1.0 - e * Math.cos(E);
      const delta = f / fPrime;
      E -= delta;
      if (Math.abs(delta) < 1e-11) break;
    }

    // True anomaly nu from E
    const sinHalfE = Math.sin(E / 2.0);
    const cosHalfE = Math.cos(E / 2.0);
    const nuRad = 2.0 * Math.atan2(Math.sqrt(1.0 + e) * sinHalfE, Math.sqrt(1.0 - e) * cosHalfE);
    let nuDeg = (nuRad * 180.0) / Math.PI;
    if (nuDeg < 0.0) nuDeg += 360.0;

    const EDeg = ((E * 180.0) / Math.PI) % 360.0;

    // Radius r = a * (1 - e * cos(E))
    const rKm = a * (1.0 - e * Math.cos(E));
    const hKm = rKm - Rp;

    // Orbital speed v = sqrt( mu * (2/r - 1/a) )
    const vKmS = Math.sqrt(Math.max(0.0, mu * (2.0 / rKm - 1.0 / a)));

    const rpKm = a * (1.0 - e);
    const raKm = a * (1.0 + e);

    return {
      trueAnomalyDeg: parseFloat(nuDeg.toFixed(3)),
      eccentricAnomalyDeg: parseFloat(EDeg.toFixed(3)),
      orbitalRadiusKm: parseFloat(rKm.toFixed(2)),
      orbitalAltitudeKm: parseFloat(hKm.toFixed(2)),
      orbitalVelocityKmS: parseFloat(vKmS.toFixed(3)),
      periapsisAltitudeKm: parseFloat((rpKm - Rp).toFixed(2)),
      apoapsisAltitudeKm: parseFloat((raKm - Rp).toFixed(2))
    };
  }

  /**
   * Calculate atmospheric aerobraking corridor deceleration, dynamic pressure, and free-molecular heating rate.
   * a_drag = ( 1 / (2 * m) ) * rho * v^2 * C_D * A
   * q_heat = ( 1 / 2 ) * rho * v^3 * C_H
   * Reference: Tolson et al. (2005) for MGS, Zurek & Smrekar (2007) for MRO aerobraking operations.
   * @param {number} periapsisAltitudeKm - Periapsis altitude z_p in km (e.g. 100 km to 135 km)
   * @param {number} [velocityKmS=4.50] - Periapsis orbital speed in km/s (typically 4.2 to 4.8 km/s)
   * @param {number} [spacecraftMassKg=1500.0] - Spacecraft dry + propellant mass in kg
   * @param {number} [dragAreaM2=20.0] - Effective projected frontal cross-sectional area with solar panels in m^2
   * @param {number} [dragCoefficientCd=2.10] - Hypersonic drag coefficient (typically 2.0 to 2.2)
   * @returns {{atmosphericDensityKgM3: number, dragDecelerationMS2: number, dynamicPressurePa: number, heatFluxWPerCm2: number, isWithinSafetyCorridor: boolean}}
   */
  static computeAerobrakingDragDecelerationAndDensity(periapsisAltitudeKm, velocityKmS = 4.50, spacecraftMassKg = 1500.0, dragAreaM2 = 20.0, dragCoefficientCd = 2.10) {
    const z = Math.max(50.0, Math.min(250.0, periapsisAltitudeKm));
    const vMS = Math.max(100.0, velocityKmS * 1000.0);
    const m = Math.max(10.0, spacecraftMassKg);
    const A = Math.max(0.1, dragAreaM2);
    const Cd = Math.max(0.5, dragCoefficientCd);

    // Standard Mars upper atmosphere density profile (100 km ref: 1.5e-7 kg/m^3, H = 8.0 km)
    const rhoRef = 1.5e-7; // kg/m^3 at 100 km
    const H = 8.0; // km
    const rho = rhoRef * Math.exp(-(z - 100.0) / H);

    // Dynamic pressure q = 0.5 * rho * v^2 (Pa)
    const qDyn = 0.5 * rho * vMS * vMS;

    // Drag force F = q * Cd * A
    const FDrag = qDyn * Cd * A;
    const aDrag = FDrag / m; // m/s^2

    // Heat flux q_heat = 0.5 * rho * v^3 * C_H (W/m^2) with Stanton number C_H ~ 0.08
    const CH = 0.08;
    const qHeatWM2 = 0.5 * rho * Math.pow(vMS, 3.0) * CH;
    const qHeatWCm2 = qHeatWM2 * 1e-4; // W/cm^2

    // Safety limits for MRO/Odyssey: heat flux < 0.35 W/cm^2, a_drag < 0.35 m/s^2
    const isSafe = qHeatWCm2 <= 0.35 && aDrag <= 0.35;

    return {
      atmosphericDensityKgM3: parseFloat(rho.toExponential(4)),
      dragDecelerationMS2: parseFloat(aDrag.toFixed(4)),
      dynamicPressurePa: parseFloat(qDyn.toFixed(3)),
      heatFluxWPerCm2: parseFloat(qHeatWCm2.toFixed(4)),
      isWithinSafetyCorridor: isSafe
    };
  }

  /**
   * Calculate gravitational J2/J3 harmonic coupling, apsidal precession rate domega/dt, and frozen orbit equilibrium eccentricity.
   * domega/dt = ( 3 / 4 ) * n * J2 * (R_eq / p)^2 * ( 5 * cos^2(i) - 1 )
   * e_frozen = -( J3 / (2 * J2) ) * ( R_eq / a ) * ( sin(i) / ( 1 - 1.25 * sin^2(i) ) )
   * Reference: Cook (1966), Vallado (2013) for Mars Odyssey / MRO / MGS mission design.
   * @param {number} semiMajorAxisKm - Semi-major axis a in km (e.g. 3775 km for Odyssey, 3646 km for MRO)
   * @param {number} inclinationDeg - Orbital inclination i in degrees (e.g. 93.1 deg for Odyssey)
   * @param {string} [body='mars'] - Planetary body ('mars', 'earth')
   * @returns {{apsidalPrecessionDegPerDay: number, frozenEquilibriumEccentricity: number, criticalInclinationDeg: number, isFrozenOrbitCapable: boolean, frozenPeriapsisArgumentDeg: number}}
   */
  static computeFrozenOrbitEquilibriumAndJ3Coupling(semiMajorAxisKm, inclinationDeg, body = 'mars') {
    const bKey = body.toLowerCase();
    const isEarth = bKey === 'earth';

    const mu = isEarth ? 398600.4418 : 42828.37; // km^3/s^2
    const Req = isEarth ? 6378.137 : 3396.19;    // km
    const J2 = isEarth ? 1.08263e-3 : 1.96045e-3;
    const J3 = isEarth ? -2.532e-6 : 3.15e-5;

    const a = Math.max(100.0, semiMajorAxisKm);
    const iRad = (inclinationDeg * Math.PI) / 180.0;
    const n = Math.sqrt(mu / Math.pow(a, 3.0)); // rad/s

    const sinI = Math.sin(iRad);
    const cosI = Math.cos(iRad);

    // Apsidal precession rate (circular reference e~0 -> p~a)
    const dOmegaRadSec = 0.75 * n * J2 * Math.pow(Req / a, 2.0) * (5.0 * cosI * cosI - 1.0);
    const dOmegaDegDay = dOmegaRadSec * (180.0 / Math.PI) * 86400.0;

    // Critical inclination (where 5*cos^2(i) - 1 = 0)
    const critIDeg = (Math.acos(1.0 / Math.sqrt(5.0)) * 180.0) / Math.PI; // ~63.43 deg

    // Frozen orbit equilibrium eccentricity
    const den = 1.0 - 1.25 * sinI * sinI;
    let eFrozen = 0.005;
    if (Math.abs(den) > 1e-4) {
      eFrozen = -(J3 / (2.0 * J2)) * (Req / a) * (sinI / den);
    }
    eFrozen = Math.abs(eFrozen); // magnitude of equilibrium eccentricity

    return {
      apsidalPrecessionDegPerDay: parseFloat(dOmegaDegDay.toFixed(4)),
      frozenEquilibriumEccentricity: parseFloat(eFrozen.toFixed(5)),
      criticalInclinationDeg: parseFloat(critIDeg.toFixed(2)),
      isFrozenOrbitCapable: true,
      frozenPeriapsisArgumentDeg: J3 > 0 ? 270.0 : 90.0 // 270 deg for Mars South Pole
    };
  }

  /**
   * Calculate orbital decay rate da/dt and remaining orbital lifetime using King-Hele atmospheric drag formulation.
   * da/dt = - sqrt( mu * a ) * ( rho(h_p) / B )
   * Lifetime = H / ( 2 * |da/dt| )
   * Reference: King-Hele (1987), Vallado (2013) for satellite orbital decay.
   * @param {number} semiMajorAxisKm - Semi-major axis a in km (e.g. 3650 km for low mapping orbit, 3520 km for decaying orbit)
   * @param {number} [eccentricity=0.005] - Orbital eccentricity e (0 <= e < 0.8)
   * @param {number} [spacecraftMassKg=1000.0] - Spacecraft mass in kg
   * @param {number} [dragAreaM2=15.0] - Cross-sectional drag area in m^2
   * @param {number} [dragCoefficientCd=2.20] - Hypersonic drag coefficient
   * @param {string} [body='mars'] - Planetary body ('mars', 'earth')
   * @returns {{decayRateKmPerDay: number, decayRateMetersPerOrbit: number, orbitalLifetimeDays: number, orbitalLifetimeMarsYears: number, periapsisAltitudeKm: number, atmosphericDensityAtPeriapsisKgM3: number}}
   */
  static computeOrbitalLifetimeAndSemiMajorAxisDecayRate(semiMajorAxisKm, eccentricity = 0.005, spacecraftMassKg = 1000.0, dragAreaM2 = 15.0, dragCoefficientCd = 2.20, body = 'mars') {
    const bKey = body.toLowerCase();
    const isEarth = bKey === 'earth';

    const mu = isEarth ? 398600.4418 : 42828.37; // km^3/s^2
    const Rp = isEarth ? 6378.137 : 3396.19;    // km
    const H = isEarth ? 7.2 : 8.0;               // scale height in km
    const rhoRef = isEarth ? 5e-11 : 1.5e-7;     // ref density at 100 km

    const a = Math.max(100.0, semiMajorAxisKm);
    const e = Math.min(0.85, Math.max(0.0, eccentricity));
    const m = Math.max(10.0, spacecraftMassKg);
    const A = Math.max(0.1, dragAreaM2);
    const Cd = Math.max(0.5, dragCoefficientCd);

    // Ballistic coefficient B = m / (Cd * A) (kg/m^2)
    const B = m / (Cd * A);

    const hpKm = a * (1.0 - e) - Rp;

    // Atmospheric density at periapsis
    const rhoP = rhoRef * Math.exp(-(hpKm - 100.0) / H);

    // Orbital period in seconds T = 2*pi*sqrt(a^3 / mu)
    const TSec = 2.0 * Math.PI * Math.sqrt(Math.pow(a, 3.0) / mu);
    const revsPerDay = 86400.0 / TSec;

    // Circular base decay rate da/dt (m/s -> km/day)
    const vCircMS = Math.sqrt((mu * 1e9) / (a * 1e3));
    let daDtMS = -(vCircMS * (rhoP / B)); // m/s instantaneous rate

    // Eccentricity concentration factor (King-Hele correction)
    if (e > 0.01) {
      const cParam = (a * e) / H;
      if (cParam > 1.0) {
        daDtMS *= Math.sqrt(1.0 / (2.0 * Math.PI * cParam));
      }
    }

    const daDtKmDay = daDtMS * 86.4; // convert m/s to km/day
    const daDtMOrbit = (daDtMS * TSec); // meters per orbit

    const absDaDtKmDay = Math.max(1e-12, Math.abs(daDtKmDay));
    const lifetimeDays = (H / (2.0 * absDaDtKmDay));
    const lifetimeMarsYears = lifetimeDays / 687.0;

    return {
      decayRateKmPerDay: parseFloat(Math.abs(daDtKmDay).toExponential(4)),
      decayRateMetersPerOrbit: parseFloat(Math.abs(daDtMOrbit).toExponential(4)),
      orbitalLifetimeDays: parseFloat(lifetimeDays.toFixed(1)),
      orbitalLifetimeMarsYears: parseFloat(lifetimeMarsYears.toFixed(2)),
      periapsisAltitudeKm: parseFloat(hpKm.toFixed(2)),
      atmosphericDensityAtPeriapsisKgM3: parseFloat(rhoP.toExponential(4))
    };
  }

  /**
   * Calculate hypersonic atmospheric entry peak deceleration, g-load, peak altitude, and stagnation heating rate (Allen-Eggers formulation).
   * a_peak = ( v_E^2 * sin(gamma_E) ) / ( 2 * e * H )
   * v_peak = v_E / sqrt(e) ~ 0.6065 * v_E
   * Reference: Allen & Eggers (1958), Chapman (1958), Braun & Manning (2007) for Mars EDL trajectory design.
   * @param {number} entryVelocityKmS - Hypersonic atmospheric entry interface speed v_E in km/s (e.g. 5.7 km/s for MSL/Perseverance, 11.2 km/s for Apollo)
   * @param {number} flightPathAngleDeg - Entry flight path angle gamma_E in degrees relative to local horizon (e.g. -12.5 deg)
   * @param {number} [ballisticCoefficientKgM2=120.0] - Ballistic coefficient beta = m / (Cd * A) in kg/m^2
   * @param {number} [noseRadiusMeters=1.15] - Aeroshell spherical nose radius R_N in meters
   * @param {string} [body='mars'] - Planetary body ('mars', 'earth')
   * @returns {{peakDecelerationMS2: number, peakGLoad: number, velocityAtPeakDecelKmS: number, peakDecelerationAltitudeKm: number, peakStagnationHeatFluxWPerCm2: number, atmosphericDensityAtPeakKgM3: number}}
   */
  static computeAtmosphericEntryPeakDecelerationAndStagnationPoint(entryVelocityKmS, flightPathAngleDeg, ballisticCoefficientKgM2 = 120.0, noseRadiusMeters = 1.15, body = 'mars') {
    const bKey = body.toLowerCase();
    const isEarth = bKey === 'earth';

    const vEMS = Math.max(100.0, entryVelocityKmS * 1000.0);
    const gammaRad = Math.abs(flightPathAngleDeg) * (Math.PI / 180.0);
    const sinGamma = Math.max(0.01, Math.sin(gammaRad));
    const beta = Math.max(10.0, ballisticCoefficientKgM2);
    const Rn = Math.max(0.1, noseRadiusMeters);

    const H = isEarth ? 7200.0 : 11100.0;     // scale height in meters
    const rho0 = isEarth ? 1.225 : 0.020;     // surface density in kg/m^3
    const g0 = 9.80665;                       // Earth standard gravity for g-load

    // Peak deceleration magnitude a_peak = (vE^2 * sin(gamma)) / (2 * e * H)
    const eConst = Math.E; // ~2.71828
    const aPeak = (vEMS * vEMS * sinGamma) / (2.0 * eConst * H);
    const gPeak = aPeak / g0;

    // Velocity at peak deceleration v_peak = vE / sqrt(e)
    const vPeakMS = vEMS / Math.sqrt(eConst);
    const vPeakKmS = vPeakMS / 1000.0;

    // Atmospheric density at peak deceleration rho_peak = (2 * beta * sin(gamma)) / H
    const rhoPeak = (2.0 * beta * sinGamma) / H;

    // Peak altitude h_peak = H * ln(rho0 / rhoPeak)
    let hPeakM = H * Math.log(Math.max(1.01, rho0 / rhoPeak));
    if (hPeakM < 0.0) hPeakM = 0.0;
    const hPeakKm = hPeakM / 1000.0;

    // Stagnation point convective heat flux (Sutton-Graves formulation)
    // q_stag = k_SG * sqrt(rho_peak / Rn) * v_peak^3 (in W/m^2 -> / 10000 for W/cm^2)
    const kSG = isEarth ? 1.74e-4 : 1.89e-4; // Mars CO2 atmosphere coefficient
    const qStagWM2 = kSG * Math.sqrt(rhoPeak / Rn) * Math.pow(vPeakMS, 3.0);
    const qStagWCm2 = qStagWM2 * 1e-4; // W/cm^2 (~50 W/cm^2 peak heating for MSL)

    return {
      peakDecelerationMS2: parseFloat(aPeak.toFixed(2)),
      peakGLoad: parseFloat(gPeak.toFixed(2)),
      velocityAtPeakDecelKmS: parseFloat(vPeakKmS.toFixed(3)),
      peakDecelerationAltitudeKm: parseFloat(hPeakKm.toFixed(2)),
      peakStagnationHeatFluxWPerCm2: parseFloat(qStagWCm2.toFixed(2)),
      atmosphericDensityAtPeakKgM3: parseFloat(rhoPeak.toExponential(4))
    };
  }

  /**
   * Calculate interplanetary Mars Orbit Insertion (MOI) braking Delta-V and propellant mass fraction.
   * v_hyp = sqrt( v_inf^2 + 2*mu / r_p )
   * v_cap = sqrt( mu * ( 2/r_p - 1/a_cap ) )
   * Delta_V = v_hyp - v_cap
   * Reference: Battin (1999), Vallado (2013), Curtis (2013) for interplanetary orbital transfer.
   * @param {number} hyperbolicExcessSpeedKmS - Interplanetary arrival excess speed v_infinity in km/s (typically 2.5 to 4.0 km/s)
   * @param {number} [periapsisAltitudeKm=300.0] - Target insertion periapsis altitude in km
   * @param {number} [targetApoapsisAltitudeKm=40000.0] - Target capture orbit apoapsis altitude in km (e.g. 40,000 km for elliptical capture, 400 km for circular)
   * @param {number} [specificImpulseSec=315.0] - Rocket engine specific impulse Isp in seconds (315 s for N2O4/MMH bipropellant)
   * @param {string} [body='mars'] - Planetary body ('mars', 'earth')
   * @returns {{deltaVKmS: number, deltaVMS: number, hyperbolicArrivalSpeedKmS: number, capturePeriapsisSpeedKmS: number, propellantMassFractionPct: number, targetOrbitPeriodHours: number}}
   */
  static computeMarsOrbitInsertionDeltaV(hyperbolicExcessSpeedKmS, periapsisAltitudeKm = 300.0, targetApoapsisAltitudeKm = 40000.0, specificImpulseSec = 315.0, body = 'mars') {
    const bKey = body.toLowerCase();
    const isEarth = bKey === 'earth';

    const mu = isEarth ? 398600.4418 : 42828.37; // km^3/s^2
    const Rp = isEarth ? 6378.137 : 3396.19;    // km
    const g0 = 9.80665;                          // m/s^2

    const vInf = Math.max(0.1, hyperbolicExcessSpeedKmS);
    const hp = Math.max(50.0, periapsisAltitudeKm);
    const ha = Math.max(hp, targetApoapsisAltitudeKm);
    const Isp = Math.max(50.0, specificImpulseSec);

    const rp = Rp + hp;
    const ra = Rp + ha;
    const aCap = (rp + ra) / 2.0;

    // Hyperbolic arrival velocity at periapsis
    const vHyp = Math.sqrt(vInf * vInf + (2.0 * mu) / rp);

    // Target capture orbit velocity at periapsis
    const vCap = Math.sqrt(mu * (2.0 / rp - 1.0 / aCap));

    // Braking Delta-V
    const deltaV = vHyp - vCap;
    const deltaVMS = deltaV * 1000.0;

    // Tsiolkovsky propellant mass fraction: Delta_m / m0 = 1 - exp( -Delta_V / (Isp * g0) )
    const massFrac = 1.0 - Math.exp(-deltaVMS / (Isp * g0));
    const massFracPct = massFrac * 100.0;

    // Orbital period of target capture orbit in hours
    const periodSec = 2.0 * Math.PI * Math.sqrt(Math.pow(aCap, 3.0) / mu);
    const periodHours = periodSec / 3600.0;

    return {
      deltaVKmS: parseFloat(deltaV.toFixed(3)),
      deltaVMS: parseFloat(deltaVMS.toFixed(1)),
      hyperbolicArrivalSpeedKmS: parseFloat(vHyp.toFixed(3)),
      capturePeriapsisSpeedKmS: parseFloat(vCap.toFixed(3)),
      propellantMassFractionPct: parseFloat(massFracPct.toFixed(2)),
      targetOrbitPeriodHours: parseFloat(periodHours.toFixed(2))
    };
  }

  /**
   * Calculate interplanetary departure characteristic energy C3, departure hyperbolic speed, and Trans-Mars Injection (TMI) Delta-V.
   * v_dep = sqrt( C3 + 2*mu / r_park )
   * v_park = sqrt( mu / r_park )
   * Delta_V = v_dep - v_park
   * Reference: Vallado (2013), Battin (1999) for Earth-to-Mars launch trajectory injection.
   * @param {number} c3CharacteristicEnergyKm2S2 - Characteristic launch excess energy C3 = v_inf^2 in km^2/s^2 (e.g. 10 to 25 km^2/s^2)
   * @param {number} [parkOrbitAltitudeKm=250.0] - Circular parking orbit altitude in km (LEO 250 km)
   * @param {number} [specificImpulseSec=450.0] - Upper stage engine specific impulse Isp in seconds (450 s for LOX/LH2 Centaur)
   * @param {string} [departureBody='earth'] - Launch departure planetary body ('earth', 'mars')
   * @returns {{transMarsInjectionDeltaVKmS: number, transMarsInjectionDeltaVMS: number, departureHyperbolicSpeedKmS: number, circularParkingOrbitSpeedKmS: number, propellantMassFractionPct: number}}
   */
  static computeInterplanetaryDepartureC3AndTransMarsInjectionDeltaV(c3CharacteristicEnergyKm2S2, parkOrbitAltitudeKm = 250.0, specificImpulseSec = 450.0, departureBody = 'earth') {
    const bKey = departureBody.toLowerCase();
    const isMars = bKey === 'mars';

    const mu = isMars ? 42828.37 : 398600.4418; // km^3/s^2
    const Rp = isMars ? 3396.19 : 6378.137;     // km
    const g0 = 9.80665;                          // m/s^2

    const C3 = Math.max(0.0, c3CharacteristicEnergyKm2S2);
    const hp = Math.max(50.0, parkOrbitAltitudeKm);
    const Isp = Math.max(50.0, specificImpulseSec);

    const rPark = Rp + hp;

    // Circular parking orbit speed
    const vPark = Math.sqrt(mu / rPark);

    // Departure hyperbolic velocity at parking orbit altitude
    const vDep = Math.sqrt(C3 + (2.0 * mu) / rPark);

    // Injection Delta-V
    const deltaV = vDep - vPark;
    const deltaVMS = deltaV * 1000.0;

    // Tsiolkovsky propellant mass fraction: Delta_m / m0 = 1 - exp( -Delta_V / (Isp * g0) )
    const massFrac = 1.0 - Math.exp(-deltaVMS / (Isp * g0));
    const massFracPct = massFrac * 100.0;

    return {
      transMarsInjectionDeltaVKmS: parseFloat(deltaV.toFixed(3)),
      transMarsInjectionDeltaVMS: parseFloat(deltaVMS.toFixed(1)),
      departureHyperbolicSpeedKmS: parseFloat(vDep.toFixed(3)),
      circularParkingOrbitSpeedKmS: parseFloat(vPark.toFixed(3)),
      propellantMassFractionPct: parseFloat(massFracPct.toFixed(2))
    };
  }

  /**
   * Calculate Mars-to-Earth return departure Trans-Earth Injection (TEI) Delta-V and Earth atmospheric re-entry speed.
   * v_dep = sqrt( C3_Mars + 2*mu_Mars / r_park )
   * Delta_V_TEI = v_dep - v_park
   * v_entry_Earth = sqrt( v_inf_Earth^2 + 2*mu_Earth / r_entry )
   * Reference: Battin (1999), Vallado (2013) for Mars Sample Return (MSR) mission analysis.
   * @param {number} [parkOrbitAltitudeKm=300.0] - Mars circular parking orbit altitude in km
   * @param {number} [transEarthC3EnergyKm2S2=12.0] - Mars departure excess energy C3 in km^2/s^2 (typically 9 to 16 km^2/s^2)
   * @param {number} [specificImpulseSec=320.0] - Earth Return Vehicle (ERV) specific impulse Isp in seconds
   * @param {number} [earthArrivalExcessSpeedKmS=3.80] - Earth arrival excess velocity v_infinity in km/s (3.5 to 4.5 km/s)
   * @returns {{transEarthInjectionDeltaVKmS: number, transEarthInjectionDeltaVMS: number, marsParkingOrbitSpeedKmS: number, earthAtmosphericEntrySpeedKmS: number, propellantMassFractionPct: number}}
   */
  static computeMarsToEarthReturnTrajectoryAndTEIDeltaV(parkOrbitAltitudeKm = 300.0, transEarthC3EnergyKm2S2 = 12.0, specificImpulseSec = 320.0, earthArrivalExcessSpeedKmS = 3.80) {
    const muMars = 42828.37; // km^3/s^2
    const RpMars = 3396.19;  // km
    const muEarth = 398600.4418; // km^3/s^2
    const RpEarth = 6378.137;    // km
    const g0 = 9.80665;          // m/s^2

    const hp = Math.max(50.0, parkOrbitAltitudeKm);
    const C3 = Math.max(0.0, transEarthC3EnergyKm2S2);
    const Isp = Math.max(50.0, specificImpulseSec);
    const vInfEarth = Math.max(0.1, earthArrivalExcessSpeedKmS);

    const rPark = RpMars + hp;

    // Mars circular parking speed
    const vPark = Math.sqrt(muMars / rPark);

    // Departure hyperbolic speed from Mars
    const vDep = Math.sqrt(C3 + (2.0 * muMars) / rPark);

    // TEI Delta-V
    const deltaV = vDep - vPark;
    const deltaVMS = deltaV * 1000.0;

    // Propellant mass fraction
    const massFrac = 1.0 - Math.exp(-deltaVMS / (Isp * g0));
    const massFracPct = massFrac * 100.0;

    // Earth entry interface speed at 120 km altitude
    const rEntryEarth = RpEarth + 120.0;
    const vEntryEarth = Math.sqrt(vInfEarth * vInfEarth + (2.0 * muEarth) / rEntryEarth);

    return {
      transEarthInjectionDeltaVKmS: parseFloat(deltaV.toFixed(3)),
      transEarthInjectionDeltaVMS: parseFloat(deltaVMS.toFixed(1)),
      marsParkingOrbitSpeedKmS: parseFloat(vPark.toFixed(3)),
      earthAtmosphericEntrySpeedKmS: parseFloat(vEntryEarth.toFixed(3)),
      propellantMassFractionPct: parseFloat(massFracPct.toFixed(2))
    };
  }

  /**
   * Calculate Martian moon (Phobos / Deimos) Hill sphere radius, surface escape velocity, and co-orbital rendezvous orbital speed.
   * R_Hill = a * ( m_moon / ( 3 * M_Mars ) )^(1/3)
   * v_esc = sqrt( 2 * G * m_moon / R_moon )
   * Reference: Murray & Dermott (1999), Vallado (2013) for JAXA MMX & Phobos rendezvous orbital design.
   * @param {string} [moonName='phobos'] - Moon identifier ('phobos', 'deimos')
   * @param {string} [body='mars'] - Planetary body ('mars')
   * @returns {{moon: string, semiMajorAxisKm: number, orbitalPeriodHours: number, orbitalSpeedKmS: number, hillSphereRadiusKm: number, hillSphereAltitudeAboveSurfaceKm: number, surfaceEscapeSpeedMS: number, surfaceGravityMS2: number}}
   */
  static computeMoonCoOrbitalRendezvousAndHillSphere(moonName = 'phobos', body = 'mars') {
    const mName = moonName.toLowerCase();
    const isDeimos = mName === 'deimos';

    const muMars = 42828.37; // km^3/s^2
    const MMars = 6.4171e23; // kg

    let a = 9376.0;      // km
    let R = 11.27;       // km mean radius
    let m = 1.0659e16;   // kg
    let muMoon = 7.0875e-4; // km^3/s^2
    let name = 'Phobos';

    if (isDeimos) {
      a = 23463.0;
      R = 6.20;
      m = 1.4762e15;
      muMoon = 9.851e-5;
      name = 'Deimos';
    }

    // Orbital speed around Mars
    const vOrb = Math.sqrt(muMars / a);

    // Orbital period in hours
    const periodSec = 2.0 * Math.PI * Math.sqrt(Math.pow(a, 3.0) / muMars);
    const periodHours = periodSec / 3600.0;

    // Hill Sphere radius: R_Hill = a * ( m / (3 * M) )^(1/3)
    const massRatio = m / (3.0 * MMars);
    const rHillKm = a * Math.pow(massRatio, 1.0 / 3.0);
    const rHillAltitude = Math.max(0.0, rHillKm - R);

    // Surface escape velocity in m/s: v_esc = sqrt( 2 * mu_moon / R ) * 1000
    const vEscKmS = Math.sqrt((2.0 * muMoon) / R);
    const vEscMS = vEscKmS * 1000.0;

    // Surface gravity in m/s^2: g = mu_moon / R^2 * 1000
    const gSurfMS2 = (muMoon / (R * R)) * 1000.0;

    return {
      moon: name,
      semiMajorAxisKm: parseFloat(a.toFixed(1)),
      orbitalPeriodHours: parseFloat(periodHours.toFixed(2)),
      orbitalSpeedKmS: parseFloat(vOrb.toFixed(3)),
      hillSphereRadiusKm: parseFloat(rHillKm.toFixed(2)),
      hillSphereAltitudeAboveSurfaceKm: parseFloat(rHillAltitude.toFixed(2)),
      surfaceEscapeSpeedMS: parseFloat(vEscMS.toFixed(2)),
      surfaceGravityMS2: parseFloat(gSurfMS2.toExponential(3))
    };
  }

  /**
   * Universal variable Stumpff function C(z).
   * @param {number} z
   * @returns {number}
   */
  static _stumpffC(z) {
    if (z > 0.0) {
      return (1.0 - Math.cos(Math.sqrt(z))) / z;
    } else if (z < 0.0) {
      return (Math.cosh(Math.sqrt(-z)) - 1.0) / (-z);
    }
    return 0.5;
  }

  /**
   * Universal variable Stumpff function S(z).
   * @param {number} z
   * @returns {number}
   */
  static _stumpffS(z) {
    if (z > 0.0) {
      const sqrtZ = Math.sqrt(z);
      return (sqrtZ - Math.sin(sqrtZ)) / (sqrtZ * sqrtZ * sqrtZ);
    } else if (z < 0.0) {
      const sqrtNegZ = Math.sqrt(-z);
      return (Math.sinh(sqrtNegZ) - sqrtNegZ) / (sqrtNegZ * sqrtNegZ * sqrtNegZ);
    }
    return 1.0 / 6.0;
  }

  /**
   * Solve Lambert's boundary value orbital transfer problem for initial and arrival velocity vectors.
   * Reference: Bate, Mueller, White (1971), Battin (1999), Vallado (2013) for orbital rendezvous & interplanetary transfers.
   * @param {[number, number, number]} r1VectorKm - Initial position vector [x, y, z] in km
   * @param {[number, number, number]} r2VectorKm - Target arrival position vector [x, y, z] in km
   * @param {number} timeOfFlightSec - Time of flight Delta_t in seconds
   * @param {string} [centralBody='mars'] - Central gravitational body ('mars', 'earth', 'sun')
   * @returns {{v1VectorKmS: [number, number, number], v2VectorKmS: [number, number, number], departureSpeedKmS: number, arrivalSpeedKmS: number, transferSemiMajorAxisKm: number, transferAngleDeg: number}}
   */
  static computeLambertOrbitalTransferVelocityVectors(r1VectorKm, r2VectorKm, timeOfFlightSec, centralBody = 'mars') {
    const cBody = centralBody.toLowerCase();
    let mu = 42828.37; // Mars km^3/s^2
    if (cBody === 'earth') mu = 398600.4418;
    else if (cBody === 'sun') mu = 1.32712440018e11;

    const r1 = Math.sqrt(r1VectorKm[0] ** 2 + r1VectorKm[1] ** 2 + r1VectorKm[2] ** 2);
    const r2 = Math.sqrt(r2VectorKm[0] ** 2 + r2VectorKm[1] ** 2 + r2VectorKm[2] ** 2);
    const dt = Math.max(1.0, timeOfFlightSec);

    // Cross product to check transfer angle
    const crossZ = r1VectorKm[0] * r2VectorKm[1] - r1VectorKm[1] * r2VectorKm[0];
    const dot = r1VectorKm[0] * r2VectorKm[0] + r1VectorKm[1] * r2VectorKm[1] + r1VectorKm[2] * r2VectorKm[2];
    const cosDeltaNu = Math.max(-1.0, Math.min(1.0, dot / (r1 * r2)));
    let deltaNu = Math.acos(cosDeltaNu);
    if (crossZ < 0.0) deltaNu = 2.0 * Math.PI - deltaNu;

    const A = Math.sin(deltaNu) * Math.sqrt((r1 * r2) / Math.max(1e-6, 1.0 - cosDeltaNu));

    // Newton-Raphson iteration for universal variable z
    let z = 0.0;
    for (let iter = 0; iter < 35; iter++) {
      const Cz = TrajectoryEngine._stumpffC(z);
      const Sz = TrajectoryEngine._stumpffS(z);
      const y = r1 + r2 + A * ((z * Sz - 1.0) / Math.max(1e-6, Math.sqrt(Cz)));
      if (y <= 0.0) {
        z += 0.1;
        continue;
      }
      const tof = (Math.pow(y / Cz, 1.5) * Sz + A * Math.sqrt(y)) / Math.sqrt(mu);
      const diff = tof - dt;
      if (Math.abs(diff) < 1e-4) break;

      // Derivative d(tof)/dz
      const dToF = (Math.pow(y / Cz, 1.5) * (1.0 / (2.0 * z) * (Cz - 1.5 * Sz / Cz) + 0.75 * (Sz * Sz) / Cz) + (A / 8.0) * (3.0 * Sz * Math.sqrt(y) / Cz + A / Math.sqrt(y))) / Math.sqrt(mu);
      z = z - diff / (dToF || 1.0);
    }

    const Cz = TrajectoryEngine._stumpffC(z);
    const y = Math.max(1e-3, r1 + r2 + A * ((z * TrajectoryEngine._stumpffS(z) - 1.0) / Math.max(1e-6, Math.sqrt(Cz))));
    const f = 1.0 - y / r1;
    const g = A * Math.sqrt(y / mu);
    const gDot = 1.0 - y / r2;

    const v1 = [
      (r2VectorKm[0] - f * r1VectorKm[0]) / g,
      (r2VectorKm[1] - f * r1VectorKm[1]) / g,
      (r2VectorKm[2] - f * r1VectorKm[2]) / g
    ];

    const v2 = [
      (gDot * r2VectorKm[0] - r1VectorKm[0]) / g,
      (gDot * r2VectorKm[1] - r1VectorKm[1]) / g,
      (gDot * r2VectorKm[2] - r1VectorKm[2]) / g
    ];

    const speed1 = Math.sqrt(v1[0] ** 2 + v1[1] ** 2 + v1[2] ** 2);
    const speed2 = Math.sqrt(v2[0] ** 2 + v2[1] ** 2 + v2[2] ** 2);
    const energy = (speed1 ** 2) / 2.0 - mu / r1;
    const aTransfer = -mu / (2.0 * energy);

    return {
      v1VectorKmS: [parseFloat(v1[0].toFixed(3)), parseFloat(v1[1].toFixed(3)), parseFloat(v1[2].toFixed(3))],
      v2VectorKmS: [parseFloat(v2[0].toFixed(3)), parseFloat(v2[1].toFixed(3)), parseFloat(v2[2].toFixed(3))],
      departureSpeedKmS: parseFloat(speed1.toFixed(3)),
      arrivalSpeedKmS: parseFloat(speed2.toFixed(3)),
      transferSemiMajorAxisKm: parseFloat(aTransfer.toFixed(1)),
      transferAngleDeg: parseFloat((deltaNu * 180.0 / Math.PI).toFixed(2))
    };
  }

  /**
   * Calculate planetary flyby gravity assist turning angle, Delta-V boost, and B-plane impact parameter.
   * e = 1 + ( r_p * v_inf^2 ) / mu
   * delta = 2 * asin( 1 / e )
   * Delta_V = 2 * v_inf * sin( delta / 2 ) = 2 * v_inf / e
   * b = r_p * sqrt( 1 + 2*mu / (r_p * v_inf^2) )
   * Reference: Battin (1999), Vallado (2013) for planetary swingby trajectory mechanics.
   * @param {number} incomingVInfKmS - Hyperbolic excess arrival speed v_infinity in km/s (typically 3 to 10 km/s)
   * @param {number} [flybyPeriapsisAltitudeKm=500.0] - Flyby closest approach altitude in km
   * @param {string} [body='mars'] - Flyby planetary body ('mars', 'earth')
   * @returns {{turningAngleDeg: number, maxDeltaVKmS: number, maxDeltaVMS: number, bPlaneImpactParameterKm: number, hyperbolicEccentricity: number, periapsisSpeedKmS: number}}
   */
  static computePlanetaryFlybyGravityAssistAndBPlane(incomingVInfKmS, flybyPeriapsisAltitudeKm = 500.0, body = 'mars') {
    const bKey = body.toLowerCase();
    const isEarth = bKey === 'earth';

    const mu = isEarth ? 398600.4418 : 42828.37; // km^3/s^2
    const Rp = isEarth ? 6378.137 : 3396.19;     // km

    const vInf = Math.max(0.1, incomingVInfKmS);
    const hp = Math.max(50.0, flybyPeriapsisAltitudeKm);

    const rp = Rp + hp;

    // Hyperbolic eccentricity: e = 1 + (rp * vInf^2) / mu
    const e = 1.0 + (rp * vInf * vInf) / mu;

    // Turning angle delta = 2 * asin(1 / e)
    const halfDeltaRad = Math.asin(1.0 / e);
    const deltaRad = 2.0 * halfDeltaRad;
    const deltaDeg = (deltaRad * 180.0) / Math.PI;

    // Maximum heliocentric velocity boost Delta_V = 2 * vInf * sin(delta / 2) = 2 * vInf / e
    const deltaV = (2.0 * vInf) / e;
    const deltaVMS = deltaV * 1000.0;

    // Impact parameter b = rp * sqrt(1 + 2*mu / (rp * vInf^2))
    const bParam = rp * Math.sqrt(1.0 + (2.0 * mu) / (rp * vInf * vInf));

    // Periapsis speed: vp = sqrt(vInf^2 + 2*mu / rp)
    const vp = Math.sqrt(vInf * vInf + (2.0 * mu) / rp);

    return {
      turningAngleDeg: parseFloat(deltaDeg.toFixed(2)),
      maxDeltaVKmS: parseFloat(deltaV.toFixed(3)),
      maxDeltaVMS: parseFloat(deltaVMS.toFixed(1)),
      bPlaneImpactParameterKm: parseFloat(bParam.toFixed(1)),
      hyperbolicEccentricity: parseFloat(e.toFixed(4)),
      periapsisSpeedKmS: parseFloat(vp.toFixed(3))
    };
  }

  /**
   * Calculate heliocentric patched-conics Hohmann transfer orbit parameters, hyperbolic excess speeds, flight duration, and synodic launch window.
   * a_trans = ( r1 + r2 ) / 2
   * v_t1 = sqrt( mu_sun * ( 2/r1 - 1/a_trans ) )
   * v_inf_1 = | v_t1 - v_planet1 |
   * T_flight = pi * sqrt( a_trans^3 / mu_sun )
   * Reference: Battin (1999), Vallado (2013), Curtis (2013) for interplanetary mission design.
   * @param {string} [departurePlanet='earth'] - Departure body ('earth', 'mars', 'venus')
   * @param {string} [targetPlanet='mars'] - Arrival body ('mars', 'earth', 'venus')
   * @returns {{departureBody: string, targetBody: string, transferSemiMajorAxisAU: number, timeOfFlightDays: number, timeOfFlightMonths: number, departureExcessVInfKmS: number, departureC3EnergyKm2S2: number, arrivalExcessVInfKmS: number, synodicPeriodDays: number, synodicPeriodMonths: number}}
   */
  static computeInterplanetaryHohmannTransferParameters(departurePlanet = 'earth', targetPlanet = 'mars') {
    const AU_KM = 149597870.7;
    const MU_SUN = 1.32712440018e11; // km^3/s^2

    const planetData = {
      earth: { rAU: 1.000000, rKm: 1.0 * AU_KM, periodDays: 365.256, name: 'Earth' },
      mars: { rAU: 1.523662, rKm: 1.523662 * AU_KM, periodDays: 686.980, name: 'Mars' },
      venus: { rAU: 0.723332, rKm: 0.723332 * AU_KM, periodDays: 224.701, name: 'Venus' }
    };

    const dKey = departurePlanet.toLowerCase();
    const tKey = targetPlanet.toLowerCase();

    const p1 = planetData[dKey] || planetData.earth;
    const p2 = planetData[tKey] || planetData.mars;

    const r1 = p1.rKm;
    const r2 = p2.rKm;

    // Planetary circular orbital speeds
    const v1 = Math.sqrt(MU_SUN / r1);
    const v2 = Math.sqrt(MU_SUN / r2);

    // Transfer orbit semi-major axis
    const aTransKm = (r1 + r2) / 2.0;
    const aTransAU = aTransKm / AU_KM;

    // Transfer orbit speeds at perihelion and aphelion
    const vt1 = Math.sqrt(MU_SUN * (2.0 / r1 - 1.0 / aTransKm));
    const vt2 = Math.sqrt(MU_SUN * (2.0 / r2 - 1.0 / aTransKm));

    // Hyperbolic excess speeds
    const vInf1 = Math.abs(vt1 - v1);
    const vInf2 = Math.abs(v2 - vt2);
    const C3 = vInf1 * vInf1;

    // Time of flight (half transfer orbit period)
    const tofSec = Math.PI * Math.sqrt(Math.pow(aTransKm, 3.0) / MU_SUN);
    const tofDays = tofSec / 86400.0;
    const tofMonths = tofDays / 30.4375;

    // Synodic period
    const synodicDays = Math.abs(1.0 / (1.0 / p1.periodDays - 1.0 / p2.periodDays));
    const synodicMonths = synodicDays / 30.4375;

    return {
      departureBody: p1.name,
      targetBody: p2.name,
      transferSemiMajorAxisAU: parseFloat(aTransAU.toFixed(4)),
      timeOfFlightDays: parseFloat(tofDays.toFixed(1)),
      timeOfFlightMonths: parseFloat(tofMonths.toFixed(1)),
      departureExcessVInfKmS: parseFloat(vInf1.toFixed(3)),
      departureC3EnergyKm2S2: parseFloat(C3.toFixed(2)),
      arrivalExcessVInfKmS: parseFloat(vInf2.toFixed(3)),
      synodicPeriodDays: parseFloat(synodicDays.toFixed(1)),
      synodicPeriodMonths: parseFloat(synodicMonths.toFixed(1))
    };
  }

  /**
   * Calculate Edelbaum low-thrust continuous spiral orbital transfer Delta-V, flight duration, propellant mass, and spiral revolutions.
   * Delta_V = | v1 - v2 |
   * Delta_t = ( m0 * Isp * g0 / F ) * ( 1 - exp( -Delta_V / (Isp * g0) ) )
   * Reference: Edelbaum (1961), Battin (1999), Vallado (2013) for Solar Electric Propulsion (SEP) ion drives.
   * @param {number} initialRadiusKm - Initial circular orbit radius in km (e.g. 3800 km)
   * @param {number} finalRadiusKm - Target circular orbit radius in km (e.g. 20000 km)
   * @param {number} [thrustNewtons=0.25] - Continuous thruster force in Newtons (0.05 to 5.0 N)
   * @param {number} [initialMassKg=1000.0] - Initial spacecraft wet mass in kg
   * @param {number} [specificImpulseSec=3200.0] - Ion engine specific impulse in seconds (e.g. 3000 to 4500 s)
   * @param {string} [body='mars'] - Central planetary body
   * @returns {{deltaVKmS: number, deltaVMS: number, flightTimeDays: number, flightTimeHours: number, propellantMassKg: number, finalMassKg: number, propellantMassFractionPct: number, spiralRevolutions: number}}
   */
  static computeLowThrustContinuousSpiralTransfer(initialRadiusKm, finalRadiusKm, thrustNewtons = 0.25, initialMassKg = 1000.0, specificImpulseSec = 3200.0, body = 'mars') {
    const isEarth = body.toLowerCase() === 'earth';
    const mu = isEarth ? 398600.4418 : 42828.37; // km^3/s^2
    const g0 = 9.80665; // m/s^2

    const r1 = Math.max(100.0, initialRadiusKm);
    const r2 = Math.max(100.0, finalRadiusKm);
    const F = Math.max(0.001, thrustNewtons);
    const m0 = Math.max(1.0, initialMassKg);
    const Isp = Math.max(100.0, specificImpulseSec);

    // Circular speeds (km/s)
    const v1KmS = Math.sqrt(mu / r1);
    const v2KmS = Math.sqrt(mu / r2);

    // Edelbaum low-thrust continuous Delta-V
    const deltaVKmS = Math.abs(v1KmS - v2KmS);
    const deltaVMS = deltaVKmS * 1000.0;

    // Mass fraction and propellant consumption
    const exhaustVelocityMS = Isp * g0;
    const mf = m0 * Math.exp(-deltaVMS / exhaustVelocityMS);
    const mProp = m0 - mf;
    const propFraction = (mProp / m0) * 100.0;

    // Flight time
    const mDot = F / exhaustVelocityMS; // kg/s
    const flightTimeSec = mProp / mDot;
    const flightTimeHours = flightTimeSec / 3600.0;
    const flightTimeDays = flightTimeSec / 86400.0;

    // Average orbit period and revolution count
    const rMean = (r1 + r2) / 2.0;
    const periodMeanSec = 2.0 * Math.PI * Math.sqrt(Math.pow(rMean, 3.0) / mu);
    const revs = flightTimeSec / Math.max(1.0, periodMeanSec);

    return {
      deltaVKmS: parseFloat(deltaVKmS.toFixed(3)),
      deltaVMS: parseFloat(deltaVMS.toFixed(1)),
      flightTimeDays: parseFloat(flightTimeDays.toFixed(1)),
      flightTimeHours: parseFloat(flightTimeHours.toFixed(1)),
      propellantMassKg: parseFloat(mProp.toFixed(2)),
      finalMassKg: parseFloat(mf.toFixed(2)),
      propellantMassFractionPct: parseFloat(propFraction.toFixed(2)),
      spiralRevolutions: parseFloat(revs.toFixed(1))
    };
  }

  /**
   * Calculate single-pass planetary aerocapture atmospheric braking Delta-V, peak dynamic pressure, and propellant mass saved.
   * v_cap = sqrt( mu * ( 2/r_p - 1/a_target ) )
   * Delta_V_aero = v_hyp_p - v_cap_p
   * q_max = 0.5 * rho(h_peri) * v_hyp^2
   * Reference: Cruz et al. (2006), Braun & Manning (2007) for Mars aerocapture flight mechanics.
   * @param {number} entrySpeedKmS - Atmospheric entry velocity at 125 km interface in km/s (5.5 to 7.5 km/s)
   * @param {number} [targetApoapsisAltitudeKm=1000.0] - Target post-capture apoapsis altitude in km
   * @param {number} [atmosphericPeriapsisKm=50.0] - Trajectory minimum atmospheric altitude in km (40 to 60 km)
   * @param {number} [liftToDragRatio=0.30] - Spacecraft hypersonic L/D ratio
   * @param {string} [body='mars'] - Central planetary body
   * @returns {{aeroBrakingDeltaVKmS: number, aeroBrakingDeltaVMS: number, peakDynamicPressureKPa: number, propellantFractionSavedPct: number, targetSemiMajorAxisKm: number, entryCorridorWidthDeg: number}}
   */
  static computeAerocaptureCorridorAndDynamicPressure(entrySpeedKmS, targetApoapsisAltitudeKm = 1000.0, atmosphericPeriapsisKm = 50.0, liftToDragRatio = 0.30, body = 'mars') {
    const isEarth = body.toLowerCase() === 'earth';
    const mu = isEarth ? 398600.4418 : 42828.37; // km^3/s^2
    const Rp = isEarth ? 6378.137 : 3396.19;     // km
    const hAtm = isEarth ? 120.0 : 125.0;        // km
    const rAtm = Rp + hAtm;
    const H = isEarth ? 8.5 : 11.1;              // atmospheric scale height km
    const rho0 = isEarth ? 1.225 : 0.020;        // kg/m^3
    const g0 = 9.80665;
    const ispChemical = 320.0; // s

    const ventry = Math.max(1.0, entrySpeedKmS);
    const hp = Math.max(20.0, Math.min(hAtm - 5.0, atmosphericPeriapsisKm));
    const ha = Math.max(hp + 50.0, targetApoapsisAltitudeKm);

    const rp = Rp + hp;
    const ra = Rp + ha;

    // Target captured orbit semi-major axis and periapsis velocity
    const aCap = (rp + ra) / 2.0;
    const vCapP = Math.sqrt(mu * (2.0 / rp - 1.0 / aCap));

    // Hyperbolic speed at atmospheric periapsis
    const vHypP = Math.sqrt(ventry * ventry + 2.0 * mu * (1.0 / rp - 1.0 / rAtm));

    // Aerodynamic Delta-V dissipated by atmosphere
    const deltaVAeroKmS = Math.max(0.0, vHypP - vCapP);
    const deltaVAeroMS = deltaVAeroKmS * 1000.0;

    // Peak dynamic pressure q_max = 0.5 * rho * v^2
    const rhoPeri = rho0 * Math.exp(-hp / H);
    const vHypPMS = vHypP * 1000.0;
    const qMaxPa = 0.5 * rhoPeri * (vHypPMS * vHypPMS);
    const qMaxKPa = qMaxPa / 1000.0;

    // Propellant mass fraction saved vs chemical rocket burn
    const propFractionSaved = (1.0 - Math.exp(-deltaVAeroMS / (ispChemical * g0))) * 100.0;

    // Entry corridor width (degrees)
    const corridorRad = (2.0 * Math.max(0.05, liftToDragRatio) * H) / rp;
    const corridorDeg = (corridorRad * 180.0) / Math.PI;

    return {
      aeroBrakingDeltaVKmS: parseFloat(deltaVAeroKmS.toFixed(3)),
      aeroBrakingDeltaVMS: parseFloat(deltaVAeroMS.toFixed(1)),
      peakDynamicPressureKPa: parseFloat(qMaxKPa.toFixed(2)),
      propellantFractionSavedPct: parseFloat(propFractionSaved.toFixed(1)),
      targetSemiMajorAxisKm: parseFloat(aCap.toFixed(1)),
      entryCorridorWidthDeg: parseFloat(corridorDeg.toFixed(2))
    };
  }

  /**
   * Calculate multi-pass aerobraking orbit lowering campaign parameters, pass count, campaign duration, and Delta-V savings.
   * Delta_a_pass = - 2 * pi * ( rho_p * r_p^2 / beta ) * sqrt( H / (2*pi * a * e) )
   * Reference: Lyons (1999), Tolson et al. (2008) for MGS, Odyssey, and MRO aerobraking flight operations.
   * @param {number} [initialApoapsisKm=35000.0] - Initial post-capture apoapsis altitude in km
   * @param {number} [targetApoapsisKm=400.0] - Final science mapping apoapsis altitude in km
   * @param {number} [periapsisAltitudeKm=120.0] - Aerobraking atmospheric drag corridor altitude in km
   * @param {number} [ballisticCoeffKgM2=55.0] - Spacecraft ballistic coefficient m / (C_D * A)
   * @param {string} [body='mars'] - Planetary body
   * @returns {{totalAeroDeltaVMS: number, estimatedPassCount: number, campaignDurationDays: number, campaignDurationMonths: number, meanDeltaVPerPassMS: number, propellantSavedKg: number}}
   */
  static computeAerobrakingOrbitLoweringPasses(initialApoapsisKm = 35000.0, targetApoapsisKm = 400.0, periapsisAltitudeKm = 120.0, ballisticCoeffKgM2 = 55.0, body = 'mars') {
    const isEarth = body.toLowerCase() === 'earth';
    const mu = isEarth ? 398600.4418 : 42828.37; // km^3/s^2
    const Rp = isEarth ? 6378.137 : 3396.19;     // km
    const H = isEarth ? 8.5 : 11.1;              // atmospheric scale height km
    const rho0 = isEarth ? 1.225 : 0.020;        // kg/m^3
    const g0 = 9.80665;
    const ispChemical = 320.0;
    const m0Spacecraft = 1000.0; // kg baseline

    const ha0 = Math.max(1000.0, initialApoapsisKm);
    const haTarget = Math.max(100.0, targetApoapsisKm);
    const hp = Math.max(80.0, Math.min(150.0, periapsisAltitudeKm));
    const beta = Math.max(10.0, ballisticCoeffKgM2);

    const rp = Rp + hp;
    const ra0 = Rp + ha0;
    const raFinal = Rp + haTarget;

    const a0 = (rp + ra0) / 2.0;
    const aFinal = (rp + raFinal) / 2.0;
    const e0 = (ra0 - rp) / (ra0 + rp);

    // Atmospheric density at drag corridor periapsis
    const rhoPeri = rho0 * Math.exp(-hp / H); // kg/m^3

    // Velocity at periapsis for initial and final orbits
    const vp0 = Math.sqrt(mu * (2.0 / rp - 1.0 / a0));
    const vpFinal = Math.sqrt(mu * (2.0 / rp - 1.0 / aFinal));
    const totalDeltaVMS = Math.abs(vp0 - vpFinal) * 1000.0;

    // Approximate Delta_a per pass at mean orbit
    const aMean = (a0 + aFinal) / 2.0;
    const eMean = Math.max(0.05, (e0 + 0.01) / 2.0);
    const deltaAPassKm = 2.0 * Math.PI * (rhoPeri * Math.pow(rp * 1000.0, 2.0) / beta) * Math.sqrt((H * 1000.0) / (2.0 * Math.PI * (aMean * 1000.0) * eMean)) / 1000.0;

    const estimatedPasses = Math.max(50, Math.min(2500, Math.round((a0 - aFinal) / Math.max(1.0, deltaAPassKm))));
    const meanDeltaVPassMS = totalDeltaVMS / estimatedPasses;

    // Campaign duration by integrating orbital periods
    const p0Days = (2.0 * Math.PI * Math.sqrt(Math.pow(a0, 3.0) / mu)) / 86400.0;
    const pFinalDays = (2.0 * Math.PI * Math.sqrt(Math.pow(aFinal, 3.0) / mu)) / 86400.0;
    const campaignDays = (estimatedPasses * (p0Days + pFinalDays)) / 2.0;
    const campaignMonths = campaignDays / 30.4375;

    // Propellant mass saved
    const propSavedKg = m0Spacecraft * (1.0 - Math.exp(-totalDeltaVMS / (ispChemical * g0)));

    return {
      totalAeroDeltaVMS: parseFloat(totalDeltaVMS.toFixed(1)),
      estimatedPassCount: estimatedPasses,
      campaignDurationDays: parseFloat(campaignDays.toFixed(1)),
      campaignDurationMonths: parseFloat(campaignMonths.toFixed(1)),
      meanDeltaVPerPassMS: parseFloat(meanDeltaVPassMS.toFixed(2)),
      propellantSavedKg: parseFloat(propSavedKg.toFixed(1))
    };
  }

  /**
   * Calculate planetary frozen orbit parameters, critical inclination, and equilibrium J2/J3 eccentricity.
   * i_crit = acos( 1 / sqrt(5) ) = 63.435 deg or 116.565 deg
   * e_frozen = - ( J3 * R_p ) / ( 2 * J2 * a ) * ( sin(i) / sin(omega_0) )
   * Reference: Vallado (2013), Curtis (2013) for MGS, MRO, and LRO frozen orbit mission planning.
   * @param {number} [semiMajorAxisKm=3770.0] - Orbit semi-major axis in km (e.g. 3770 km for Mars, 1850 km for Moon)
   * @param {number} [inclinationDeg=93.0] - Orbit inclination in degrees
   * @param {string} [body='mars'] - Central body ('mars', 'moon', 'earth')
   * @param {number} [targetArgumentOfPeriapsisDeg=270.0] - Target locked argument of periapsis (typically 270 deg for southern science)
   * @returns {{frozenEccentricity: number, criticalInclinationProgradeDeg: number, criticalInclinationRetrogradeDeg: number, periapsisAltitudeKm: number, apoapsisAltitudeKm: number, altitudeVariationKm: number, orbitPeriodMinutes: number, stabilityState: string}}
   */
  static computePlanetaryFrozenOrbitParameters(semiMajorAxisKm = 3770.0, inclinationDeg = 93.0, body = 'mars', targetArgumentOfPeriapsisDeg = 270.0) {
    const bKey = body.toLowerCase();
    const isMoon = bKey === 'moon';
    const isEarth = bKey === 'earth';

    let mu = 42828.37;     // Mars
    let Rp = 3396.19;
    let J2 = 1.96045e-3;
    let J3 = 3.15e-5;

    if (isMoon) {
      mu = 4902.80;
      Rp = 1737.4;
      J2 = 2.03e-4;
      J3 = 8.5e-6;
    } else if (isEarth) {
      mu = 398600.4418;
      Rp = 6378.137;
      J2 = 1.08263e-3;
      J3 = -2.532e-6;
    }

    const a = Math.max(Rp + 50.0, semiMajorAxisKm);
    const incRad = (inclinationDeg * Math.PI) / 180.0;
    const omega0Rad = (targetArgumentOfPeriapsisDeg * Math.PI) / 180.0;

    // Critical inclination for zero apsidal secular precession
    const iCritProgradeDeg = (Math.acos(1.0 / Math.sqrt(5.0)) * 180.0) / Math.PI; // 63.435 deg
    const iCritRetrogradeDeg = (Math.acos(-1.0 / Math.sqrt(5.0)) * 180.0) / Math.PI; // 116.565 deg

    // Equilibrium frozen eccentricity: e_frozen = - (J3 * Rp) / (2 * J2 * a) * (sin(i) / sin(omega0))
    const sinOmega0 = Math.sin(omega0Rad);
    let eFrozen = 0.005;
    if (Math.abs(sinOmega0) > 1e-4) {
      eFrozen = - (J3 * Rp) / (2.0 * J2 * a) * (Math.sin(incRad) / sinOmega0);
    }
    eFrozen = Math.abs(eFrozen); // ensure positive magnitude

    const rp = a * (1.0 - eFrozen);
    const ra = a * (1.0 + eFrozen);
    const hp = rp - Rp;
    const ha = ra - Rp;
    const deltaH = ha - hp;

    // Orbital period
    const periodSec = 2.0 * Math.PI * Math.sqrt(Math.pow(a, 3.0) / mu);
    const periodMin = periodSec / 60.0;

    const isApsidalStationary = Math.abs(inclinationDeg - iCritProgradeDeg) < 2.0 || Math.abs(inclinationDeg - iCritRetrogradeDeg) < 2.0;

    let state = 'Frozen Eccentricity Locked (Fixed Periapsis Altitude)';
    if (isApsidalStationary) {
      state = 'Critically Inclined Frozen Orbit (Zero Apsidal Drift dOmega/dt = 0 & Constant Altitude)';
    }

    return {
      frozenEccentricity: parseFloat(eFrozen.toFixed(6)),
      criticalInclinationProgradeDeg: parseFloat(iCritProgradeDeg.toFixed(3)),
      criticalInclinationRetrogradeDeg: parseFloat(iCritRetrogradeDeg.toFixed(3)),
      periapsisAltitudeKm: parseFloat(hp.toFixed(2)),
      apoapsisAltitudeKm: parseFloat(ha.toFixed(2)),
      altitudeVariationKm: parseFloat(deltaH.toFixed(2)),
      orbitPeriodMinutes: parseFloat(periodMin.toFixed(2)),
      stabilityState: state
    };
  }

  /**
   * Calculate Mars Areostationary Orbit (AERO) parameters, triaxial gravity libration points, and annual stationkeeping Delta-V.
   * r_sync = ( mu / omega_rot^2 )^(1/3)
   * J_22 = sqrt( C_22^2 + S_22^2 )
   * d2lambda/dt2 = - 12 * ( mu * R_p^2 / r_sync^5 ) * J_22 * sin( 2*(lambda - lambda_0) )
   * Reference: Silva & Romero (2013), Vallado (2013) for Mars synchronous relay constellation dynamics.
   * @param {number} [longitudeWestDeg=0.0] - Spacecraft sub-satellite longitude in West degrees (0 to 360)
   * @param {string} [body='mars'] - Central planetary body
   * @returns {{synchronousRadiusKm: number, synchronousAltitudeKm: number, orbitalSpeedKmS: number, rotationPeriodHours: number, annualStationkeepingDeltaVMS: number, librationBehavior: string, nearestStableLongitudeDegW: number}}
   */
  static computeAreostationaryOrbitAndLongitudinalDrift(longitudeWestDeg = 0.0, body = 'mars') {
    const isEarth = body.toLowerCase() === 'earth';

    const mu = isEarth ? 398600.4418 : 42828.37; // km^3/s^2
    const Rp = isEarth ? 6378.137 : 3396.19;     // km
    const ProtSec = isEarth ? 86164.0905 : 88642.663; // sidereal rotation sec
    const C22 = isEarth ? 1.57e-6 : -5.46e-5;
    const S22 = isEarth ? -9.04e-7 : 3.39e-5;

    const omegaRot = (2.0 * Math.PI) / ProtSec; // rad/s
    const ProtHours = ProtSec / 3600.0;

    // Synchronous radius r_sync = (mu / omega^2)^(1/3)
    const rSyncKm = Math.pow(mu / (omegaRot * omegaRot), 1.0 / 3.0);
    const hSyncKm = rSyncKm - Rp;
    const vSyncKmS = Math.sqrt(mu / rSyncKm);

    // Triaxial harmonic magnitude J22 and equilibrium phase
    const J22 = Math.sqrt(C22 * C22 + S22 * S22);
    const lambda0Deg = (0.5 * Math.atan2(S22, -C22) * 180.0) / Math.PI; // equilibrium axis (~17.9 deg W)
    const stableLon1 = ((lambda0Deg % 360) + 360) % 360;
    const stableLon2 = (stableLon1 + 180.0) % 360.0;

    const lambdaLon = ((longitudeWestDeg % 360) + 360) % 360;
    const deltaLonRad = ((lambdaLon - stableLon1) * Math.PI) / 180.0;

    // Longitudinal angular acceleration d2lambda/dt2 (rad/s^2)
    const accelRadS2 = -12.0 * (mu / Math.pow(rSyncKm, 3.0)) * Math.pow(Rp / rSyncKm, 2.0) * J22 * Math.sin(2.0 * deltaLonRad);

    // Annual stationkeeping Delta-V = r_sync * |accel| * (seconds per year)
    const secPerYear = 365.25 * 86400.0;
    const deltaVSKMS = Math.abs(rSyncKm * 1000.0 * accelRadS2 * secPerYear);

    // Closest stable longitude
    const diff1 = Math.abs(lambdaLon - stableLon1);
    const diff2 = Math.abs(lambdaLon - stableLon2);
    const nearestStable = diff1 < diff2 ? stableLon1 : stableLon2;

    let behavior = 'Stable Gravitational Libration Well';
    if (deltaVSKMS < 0.5) {
      behavior = 'Near Gravitational Equilibrium Node (Minimal Stationkeeping Fuel)';
    } else if (deltaVSKMS > 4.0) {
      behavior = 'High Longitudinal Drift Corridor (Active East-West Thruster Stationkeeping Required)';
    }

    return {
      synchronousRadiusKm: parseFloat(rSyncKm.toFixed(1)),
      synchronousAltitudeKm: parseFloat(hSyncKm.toFixed(1)),
      orbitalSpeedKmS: parseFloat(vSyncKmS.toFixed(3)),
      rotationPeriodHours: parseFloat(ProtHours.toFixed(3)),
      annualStationkeepingDeltaVMS: parseFloat(deltaVSKMS.toFixed(2)),
      librationBehavior: behavior,
      nearestStableLongitudeDegW: parseFloat(nearestStable.toFixed(1))
    };
  }

  /**
   * Calculate 3rd-body tidal gravitational perturbations, nodal precession drift, and resonance ratios from Phobos or Deimos on a Mars orbiter.
   * a_tide = 2 * mu_moon * r_orb / a_moon^3
   * dOmega/dt = - 3/4 * ( mu_moon / mu_mars ) * ( a_orb / a_moon )^3 * n_orb * cos(i_orb)
   * Reference: Jacobson (2010), Lemoine et al. (2001) for MGS, MRO, and Phobos/Deimos ephemeris tidal models.
   * @param {number} [orbiterSemiMajorAxisKm=3770.0] - Spacecraft semi-major axis in km
   * @param {number} [orbiterInclinationDeg=93.0] - Spacecraft inclination in degrees
   * @param {string} [moonName='phobos'] - Perturbing moon ('phobos', 'deimos')
   * @returns {{moonSemiMajorAxisKm: number, moonPeriodHours: number, orbiterPeriodHours: number, resonanceRatio: number, maxTidalAccelerationUMSS: number, secularNodalDriftDegPerYear: number, resonanceClassification: string}}
   */
  static computeMoonGravitationalPerturbationsOnMarsOrbit(orbiterSemiMajorAxisKm = 3770.0, orbiterInclinationDeg = 93.0, moonName = 'phobos') {
    const isDeimos = moonName.toLowerCase() === 'deimos';
    const muMars = 42828.37; // km^3/s^2

    // Moon parameters
    const aMoonKm = isDeimos ? 23463.2 : 9376.0;
    const muMoonKm3S2 = isDeimos ? 9.85e-5 : 7.087e-4; // km^3/s^2
    const pMoonSec = isDeimos ? 109125.4 : 27553.7;
    const pMoonHours = pMoonSec / 3600.0;

    const aOrb = Math.max(3450.0, orbiterSemiMajorAxisKm);
    const incRad = (orbiterInclinationDeg * Math.PI) / 180.0;

    // Orbiter period and mean motion
    const pOrbSec = 2.0 * Math.PI * Math.sqrt(Math.pow(aOrb, 3.0) / muMars);
    const pOrbHours = pOrbSec / 3600.0;
    const nOrbRadS = (2.0 * Math.PI) / pOrbSec;

    // Peak tidal perturbing acceleration (converted to um/s^2)
    const aTideMS2 = (2.0 * (muMoonKm3S2 * 1e9) * (aOrb * 1000.0)) / Math.pow(aMoonKm * 1000.0, 3.0);
    const aTideUMSS = aTideMS2 * 1e6; // um/s^2

    // Secular nodal precession drift (rad/s -> deg/year)
    const dOmegaRadS = -0.75 * (muMoonKm3S2 / muMars) * Math.pow(aOrb / aMoonKm, 3.0) * nOrbRadS * Math.cos(incRad);
    const secPerYear = 365.25 * 86400.0;
    const dOmegaDegYr = (dOmegaRadS * secPerYear * 180.0) / Math.PI;

    // Resonance ratio (e.g. 4.0 for 4:1 resonance)
    const ratio = pMoonHours / pOrbHours;
    const nearestInt = Math.round(ratio);
    const resonanceDiff = Math.abs(ratio - nearestInt);

    let classification = `Non-Resonant Perturbation Regime (${ratio.toFixed(2)}:1)`;
    if (resonanceDiff < 0.05) {
      classification = `Strong ${nearestInt}:1 Mean-Motion Resonance with ${isDeimos ? 'Deimos' : 'Phobos'}`;
    }

    return {
      moonSemiMajorAxisKm: parseFloat(aMoonKm.toFixed(1)),
      moonPeriodHours: parseFloat(pMoonHours.toFixed(3)),
      orbiterPeriodHours: parseFloat(pOrbHours.toFixed(3)),
      resonanceRatio: parseFloat(ratio.toFixed(3)),
      maxTidalAccelerationUMSS: parseFloat(aTideUMSS.toFixed(6)),
      secularNodalDriftDegPerYear: parseFloat(dOmegaDegYr.toFixed(6)),
      resonanceClassification: classification
    };
  }

  /**
   * Calculate Solar Radiation Pressure (SRP) perturbation acceleration, long-period eccentricity oscillation, and annual Delta-V on a planetary orbiter.
   * P_srp = ( S_0 / d_AU^2 ) / c
   * a_srp = ( 1 + C_R ) * ( A / m ) * P_srp
   * Delta_e_srp = 3/2 * ( a_srp * a^2 / mu )
   * Reference: Milani et al. (1987), Vallado (2013), Curtis (2013) for Mars Global Surveyor, MRO, and MAVEN orbital perturbation analysis.
   * @param {number} [semiMajorAxisKm=3770.0] - Spacecraft semi-major axis in km
   * @param {number} [areaToMassM2Kg=0.02] - Spacecraft effective area-to-mass ratio A/m in m^2/kg
   * @param {number} [reflectivityCoeff=1.3] - Surface reflectivity coefficient C_R (1.0 = absorption, 2.0 = specular)
   * @param {number} [heliocentricDistanceAU=1.524] - Solar distance in AU
   * @param {string} [body='mars'] - Central planetary body
   * @returns {{solarFluxWM2: number, photonPressureMicroPa: number, srpAccelerationUMSS: number, eccentricityOscillationAmplitude: number, annualDeltaVEquivalentMS: number, perturbationSeverity: string}}
   */
  static computeSolarRadiationPressureOrbitPerturbation(semiMajorAxisKm = 3770.0, areaToMassM2Kg = 0.02, reflectivityCoeff = 1.3, heliocentricDistanceAU = 1.524, body = 'mars') {
    const isEarth = body.toLowerCase() === 'earth';
    const mu = isEarth ? 398600.4418 : 42828.37; // km^3/s^2
    const S0 = 1361.0; // W/m^2 at 1 AU
    const c = 299792458.0; // m/s
    const dAU = Math.max(0.1, heliocentricDistanceAU);
    const cr = Math.max(1.0, Math.min(2.0, reflectivityCoeff));
    const am = Math.max(0.001, areaToMassM2Kg);
    const a = Math.max(3400.0, semiMajorAxisKm);

    // Solar flux at heliocentric distance d
    const fluxWM2 = S0 / (dAU * dAU);

    // Direct photon radiation pressure (Pa and uPa)
    const pSrpPa = fluxWM2 / c;
    const pSrpMicroPa = pSrpPa * 1e6;

    // Direct perturbing acceleration: a_srp = (1 + C_R) * (A/m) * P_srp (m/s^2 and um/s^2)
    const aSrpMS2 = (1.0 + cr) * am * pSrpPa;
    const aSrpUMSS = aSrpMS2 * 1e6;

    // Long-period eccentricity oscillation amplitude: Delta_e = 1.5 * (a_srp * a^2) / mu
    const aSrpKmS2 = aSrpMS2 / 1000.0;
    const deltaE = 1.5 * (aSrpKmS2 * Math.pow(a, 2.0)) / mu;

    // Annual cumulative Delta-V equivalent (m/s/year)
    const secPerYear = 365.25 * 86400.0;
    const annualDeltaVMS = aSrpMS2 * secPerYear;

    let severity = 'Low Solar Pressure Regime (Minimal Orbital Disruption)';
    if (annualDeltaVMS > 5.0) {
      severity = 'High Solar Pressure Regime (Requires Periodic Stationkeeping & Momentum Wheel Desaturation)';
    } else if (annualDeltaVMS > 1.5) {
      severity = 'Moderate Solar Pressure Regime (Measurable Long-Period Apsidal Precession Drift)';
    }

    return {
      solarFluxWM2: parseFloat(fluxWM2.toFixed(1)),
      photonPressureMicroPa: parseFloat(pSrpMicroPa.toFixed(3)),
      srpAccelerationUMSS: parseFloat(aSrpUMSS.toFixed(4)),
      eccentricityOscillationAmplitude: parseFloat(deltaE.toExponential(4)),
      annualDeltaVEquivalentMS: parseFloat(annualDeltaVMS.toFixed(2)),
      perturbationSeverity: severity
    };
  }

  /**
   * Calculate interplanetary solar sail characteristic acceleration, lightness number, and force vector under variable pitch angle.
   * a_0 = 2 * eta * P_srp0 / sigma
   * a_r = a_0 * ( r_0 / r )^2 * cos^3(alpha)
   * a_theta = a_0 * ( r_0 / r )^2 * cos^2(alpha) * sin(alpha)
   * Reference: McInnes (1999), Wright (1992), Dachwald (2004) for IKAROS & LightSail interplanetary flight dynamics.
   * @param {number} [sailAreaM2=500.0] - Total deployable sail reflective area in m^2
   * @param {number} [totalMassKg=50.0] - Spacecraft wet/dry total mass in kg
   * @param {number} [sailEfficiency=0.88] - Sail specular reflectivity efficiency eta (0.7 to 0.98)
   * @param {number} [heliocentricDistanceAU=1.0] - Solar distance in AU
   * @param {number} [sailPitchAngleDeg=35.264] - Sail sun-pointing pitch angle alpha in degrees (0 = normal to sun, 35.264 = max thrust)
   * @returns {{arealLoadingGM2: number, characteristicAccelerationMmS2: number, lightnessNumberBeta: number, radialAccelerationMmS2: number, transverseAccelerationMmS2: number, netAccelerationMmS2: number, optimalThrustPitchDeg: number, propulsionRegime: string}}
   */
  static computeSolarSailHeliocentricAcceleration(sailAreaM2 = 500.0, totalMassKg = 50.0, sailEfficiency = 0.88, heliocentricDistanceAU = 1.0, sailPitchAngleDeg = 35.264) {
    const A = Math.max(1.0, sailAreaM2);
    const m = Math.max(0.1, totalMassKg);
    const eta = Math.max(0.5, Math.min(1.0, sailEfficiency));
    const rAU = Math.max(0.1, heliocentricDistanceAU);
    const pitchRad = (Math.max(0.0, Math.min(89.0, sailPitchAngleDeg)) * Math.PI) / 180.0;

    const S0 = 1361.0; // W/m^2
    const c = 299792458.0; // m/s
    const Psrp0 = S0 / c; // ~4.54e-6 N/m^2 at 1 AU
    const gSun0 = 5.930e-3; // m/s^2 solar gravity at 1 AU

    // Sail areal mass loading sigma = m / A (kg/m^2 and g/m^2)
    const sigmaKgM2 = m / A;
    const sigmaGM2 = sigmaKgM2 * 1000.0;

    // Characteristic acceleration at 1 AU normal to sun (m/s^2 -> mm/s^2)
    const a0MS2 = (2.0 * eta * Psrp0) / sigmaKgM2;
    const a0MmS2 = a0MS2 * 1000.0;

    // Lightness number beta = a0 / g_sun
    const beta = a0MS2 / gSun0;

    // Distance factor (1 / r_AU^2)
    const distFactor = 1.0 / (rAU * rAU);

    // Radial, transverse, and net acceleration components (mm/s^2)
    const cosAlpha = Math.cos(pitchRad);
    const sinAlpha = Math.sin(pitchRad);

    const arMmS2 = a0MmS2 * distFactor * Math.pow(cosAlpha, 3.0);
    const aThetaMmS2 = a0MmS2 * distFactor * Math.pow(cosAlpha, 2.0) * sinAlpha;
    const aNetMmS2 = a0MmS2 * distFactor * Math.pow(cosAlpha, 2.0);

    const alphaOptDeg = (Math.atan(1.0 / Math.sqrt(2.0)) * 180.0) / Math.PI; // 35.264 deg

    let regime = 'Sub-Escape Low-Thrust Interplanetary Spiral';
    if (beta >= 1.0) {
      regime = 'Levitation / Solar Gravitational Repulsion (Hyperbolic Escape at Zero Fuel)';
    } else if (a0MmS2 >= 0.5) {
      regime = 'High-Performance Rapid Interplanetary Transit (Earth-Mars in < 200 Days)';
    }

    return {
      arealLoadingGM2: parseFloat(sigmaGM2.toFixed(2)),
      characteristicAccelerationMmS2: parseFloat(a0MmS2.toFixed(4)),
      lightnessNumberBeta: parseFloat(beta.toFixed(5)),
      radialAccelerationMmS2: parseFloat(arMmS2.toFixed(4)),
      transverseAccelerationMmS2: parseFloat(aThetaMmS2.toFixed(4)),
      netAccelerationMmS2: parseFloat(aNetMmS2.toFixed(4)),
      optimalThrustPitchDeg: parseFloat(alphaOptDeg.toFixed(3)),
      propulsionRegime: regime
    };
  }

  /**
   * Calculate planetary orbit eclipse geometry, solar beta angle threshold, and umbra shadow duration.
   * beta_crit = asin( R_p / r_p )
   * theta_shadow = acos( sqrt( r^2 - R_p^2 * cos^2(beta) ) / ( r * cos(beta) ) )
   * t_eclipse = ( theta_shadow / pi ) * P_orb
   * Reference: Wertz (1999), Vallado (2013), Curtis (2013) for MGS, MRO, and Odyssey solar array eclipse thermal sizing.
   * @param {number} [semiMajorAxisKm=3770.0] - Orbit semi-major axis in km
   * @param {number} [eccentricity=0.005] - Orbit eccentricity
   * @param {number} [solarBetaAngleDeg=0.0] - Solar beta angle between orbit plane and Sun vector (-90 to +90 deg)
   * @param {string} [body='mars'] - Central planetary body
   * @returns {{orbitPeriodMinutes: number, criticalBetaAngleDeg: number, isOrbitInFullSunlight: boolean, eclipseShadowFractionPct: number, eclipseDurationMinutes: number, daylightDurationMinutes: number, thermalShadowRegime: string}}
   */
  static computeEllipticOrbitEclipseGeometryAndShadowDuration(semiMajorAxisKm = 3770.0, eccentricity = 0.005, solarBetaAngleDeg = 0.0, body = 'mars') {
    const isEarth = body.toLowerCase() === 'earth';
    const isMoon = body.toLowerCase() === 'moon';

    let mu = 42828.37;
    let Rp = 3396.19;

    if (isEarth) {
      mu = 398600.4418;
      Rp = 6378.137;
    } else if (isMoon) {
      mu = 4902.80;
      Rp = 1737.4;
    }

    const a = Math.max(Rp + 50.0, semiMajorAxisKm);
    const e = Math.max(0.0, Math.min(0.85, eccentricity));
    const rp = a * (1.0 - e);
    const betaDeg = Math.max(-89.9, Math.min(89.9, solarBetaAngleDeg));
    const betaRad = (Math.abs(betaDeg) * Math.PI) / 180.0;

    // Orbital period (minutes)
    const periodSec = 2.0 * Math.PI * Math.sqrt(Math.pow(a, 3.0) / mu);
    const periodMin = periodSec / 60.0;

    // Critical beta angle for 100% full sunlight (no eclipse)
    const sinBetaCrit = Math.min(1.0, Rp / rp);
    const betaCritRad = Math.asin(sinBetaCrit);
    const betaCritDeg = (betaCritRad * 180.0) / Math.PI;

    const inFullSun = Math.abs(betaDeg) >= betaCritDeg;

    let shadowFrac = 0.0;
    let eclipseMin = 0.0;
    let regime = 'Full Sunlight Orbit (Zero Shadow - Continuous Solar Power Generation)';

    if (!inFullSun) {
      const cosBeta = Math.cos(betaRad);
      const rEff = a * (1.0 - e * e); // semilatus rectum / mean radius
      const underSqrt = Math.max(0.0, rEff * rEff - Rp * Rp * cosBeta * cosBeta);
      const arg = Math.min(1.0, Math.max(-1.0, Math.sqrt(underSqrt) / (rEff * cosBeta)));
      const thetaShadowRad = Math.acos(arg);

      shadowFrac = thetaShadowRad / Math.PI;
      eclipseMin = shadowFrac * periodMin;

      if (shadowFrac >= 0.30) {
        regime = 'Deep Equatorial Umbra Shadow (Maximum Battery Discharge & Solar Panel Cooling)';
      } else {
        regime = 'Grazing Penumbra/Partial Shadow Corridor';
      }
    }

    const daylightMin = periodMin - eclipseMin;

    return {
      orbitPeriodMinutes: parseFloat(periodMin.toFixed(2)),
      criticalBetaAngleDeg: parseFloat(betaCritDeg.toFixed(2)),
      isOrbitInFullSunlight: inFullSun,
      eclipseShadowFractionPct: parseFloat((shadowFrac * 100.0).toFixed(2)),
      eclipseDurationMinutes: parseFloat(eclipseMin.toFixed(2)),
      daylightDurationMinutes: parseFloat(daylightMin.toFixed(2)),
      thermalShadowRegime: regime
    };
  }

  /**
   * Calculate planetary atmospheric entry ballistic peak deceleration, altitude of peak load, and velocity drop.
   * rho_peak = ( beta * |sin(gamma)| ) / H_s
   * h_peak = H_s * ln( rho_0 / rho_peak )
   * v_peak = v_entry * exp(-0.5)
   * a_max = ( v_entry^2 * |sin(gamma)| ) / ( 2 * e * H_s )
   * Reference: Allen & Eggers (1958), Vinh et al. (1980), Braun & Manning (2007) for Mars Pathfinder & Curiosity EDL trajectories.
   * @param {number} [entryVelocityKmS=5.7] - Atmospheric entry velocity in km/s (3.5 to 12 km/s)
   * @param {number} [flightPathAngleDeg=-12.5] - Entry flight path angle gamma in degrees (-5 to -30 deg)
   * @param {number} [ballisticCoeffKgM2=120.0] - Ballistic coefficient beta = m/(C_D*A) in kg/m^2 (50 to 300 kg/m^2)
   * @param {number} [scaleHeightKm=11.1] - Atmospheric scale height H_s in km
   * @param {number} [surfaceDensityKgM3=0.020] - Surface atmospheric density rho_0 in kg/m^3
   * @returns {{peakDecelerationMS2: number, peakDecelerationGLoad: number, altitudeOfPeakDecelerationKm: number, velocityAtPeakDecelerationKmS: number, densityAtPeakDecelerationKgM3: number, entryCorridorStatus: string}}
   */
  static computeAtmosphericEntryBallisticPeakDeceleration(entryVelocityKmS = 5.7, flightPathAngleDeg = -12.5, ballisticCoeffKgM2 = 120.0, scaleHeightKm = 11.1, surfaceDensityKgM3 = 0.020) {
    const vEntryMS = Math.max(1000.0, entryVelocityKmS * 1000.0);
    const gammaRad = (Math.abs(Math.min(-1.0, flightPathAngleDeg)) * Math.PI) / 180.0;
    const beta = Math.max(10.0, ballisticCoeffKgM2);
    const HsM = Math.max(1000.0, scaleHeightKm * 1000.0);
    const rho0 = Math.max(1e-4, surfaceDensityKgM3);

    const sinGamma = Math.sin(gammaRad);
    const E_NAT = Math.E;

    // Density at peak deceleration: rho_peak = (beta * sin(gamma)) / Hs
    const rhoPeak = (beta * sinGamma) / HsM;

    // Altitude of peak deceleration: h_peak = Hs * ln(rho0 / rho_peak)
    const densityRatio = Math.max(1e-4, rho0 / rhoPeak);
    const hPeakM = HsM * Math.log(densityRatio);
    const hPeakKm = hPeakM / 1000.0;

    // Velocity at peak deceleration: v_peak = v_entry * exp(-0.5)
    const vPeakMS = vEntryMS * Math.exp(-0.5);
    const vPeakKmS = vPeakMS / 1000.0;

    // Peak deceleration: a_max = (v_entry^2 * sin(gamma)) / (2 * e * Hs)
    const aMaxMS2 = (Math.pow(vEntryMS, 2.0) * sinGamma) / (2.0 * E_NAT * HsM);
    const gLoad = aMaxMS2 / 9.80665;

    let corridor = 'Nominal Mars Entry Descent Landing Corridor';
    if (gLoad > 20.0) {
      corridor = 'Steep High-G Ballistic Reentry (Severe Thermal and Structural Loads)';
    } else if (hPeakKm < 10.0) {
      corridor = 'Shallow / Heavy Vehicle Late Deceleration Corridor (High Risk of Surface Impact before Parachute Deploy)';
    }

    return {
      peakDecelerationMS2: parseFloat(aMaxMS2.toFixed(2)),
      peakDecelerationGLoad: parseFloat(gLoad.toFixed(2)),
      altitudeOfPeakDecelerationKm: parseFloat(hPeakKm.toFixed(2)),
      velocityAtPeakDecelerationKmS: parseFloat(vPeakKmS.toFixed(3)),
      densityAtPeakDecelerationKgM3: parseFloat(rhoPeak.toExponential(4)),
      entryCorridorStatus: corridor
    };
  }

  /**
   * Calculate guided lifting planetary entry and aerocapture corridor width (Delta gamma) and steep/shallow flight path angle boundaries.
   * Delta_gamma = 2 * (L/D) / sqrt( (R_p + h_p) / H_s )
   * gamma_shallow = gamma_nom + 0.5 * Delta_gamma (full lift-down capture limit)
   * gamma_steep = gamma_nom - 0.5 * Delta_gamma (full lift-up peak load limit)
   * Reference: Cruz et al. (2006), Braun & Manning (2007), Lu (2014) for MSL Curiosity, Perseverance, and Mars Aerocapture.
   * @param {number} [entryVelocityKmS=6.0] - Atmospheric entry velocity in km/s (4.0 to 9.0 km/s)
   * @param {number} [liftToDragRatio=0.24] - Hypersonic trimmed lift-to-drag ratio L/D (0.1 to 0.8)
   * @param {number} [ballisticCoeffKgM2=130.0] - Vehicle ballistic coefficient beta in kg/m^2
   * @param {number} [nominalFlightPathAngleDeg=-11.5] - Nominal entry flight path angle gamma in degrees
   * @param {string} [body='mars'] - Target planetary body
   * @returns {{corridorWidthDeg: number, shallowBoundaryFlightPathAngleDeg: number, steepBoundaryFlightPathAngleDeg: number, nominalFlightPathAngleDeg: number, liftToDragRatio: number, aerocaptureFeasibility: string}}
   */
  static computeGuidedLiftingEntryCorridorWidth(entryVelocityKmS = 6.0, liftToDragRatio = 0.24, ballisticCoeffKgM2 = 130.0, nominalFlightPathAngleDeg = -11.5, body = 'mars') {
    const isEarth = body.toLowerCase() === 'earth';
    const RpKm = isEarth ? 6378.137 : 3396.19;
    const HsKm = isEarth ? 8.5 : 11.1;

    const ld = Math.max(0.05, Math.min(1.5, liftToDragRatio));
    const gammaNom = Math.min(-1.0, nominalFlightPathAngleDeg);
    const hpEstKm = isEarth ? 70.0 : 48.0;

    // Atmospheric entry corridor width in radians: Delta_gamma = 2 * (L/D) / sqrt( (Rp + hp) / Hs )
    const rScaleRatio = (RpKm + hpEstKm) / HsKm;
    const deltaGammaRad = (2.0 * ld) / Math.sqrt(rScaleRatio);
    const deltaGammaDeg = (deltaGammaRad * 180.0) / Math.PI;

    // Shallow and steep boundaries
    const gammaShallow = gammaNom + 0.5 * deltaGammaDeg;
    const gammaSteep = gammaNom - 0.5 * deltaGammaDeg;

    let feasibility = 'Nominal Guided Aerocapture & Precision Landing Corridor';
    if (deltaGammaDeg < 0.8) {
      feasibility = 'Extremely Narrow Corridor (Requires High-Precision Optical Autonomous Navigation & Fast Roll Control)';
    } else if (deltaGammaDeg >= 2.0) {
      feasibility = 'Wide Robust Corridor (High Margin against Atmospheric Density Fluctuations & Dust Storms)';
    }

    return {
      corridorWidthDeg: parseFloat(deltaGammaDeg.toFixed(3)),
      shallowBoundaryFlightPathAngleDeg: parseFloat(gammaShallow.toFixed(3)),
      steepBoundaryFlightPathAngleDeg: parseFloat(gammaSteep.toFixed(3)),
      nominalFlightPathAngleDeg: parseFloat(gammaNom.toFixed(3)),
      liftToDragRatio: parseFloat(ld.toFixed(2)),
      aerocaptureFeasibility: feasibility
    };
  }

  /**
   * Calculate continuous low-thrust ion/plasma spiral orbit transfer Delta-V, propellant consumption, and burn duration.
   * Delta_V = | sqrt( mu / r_1 ) - sqrt( mu / r_2 ) |
   * m_f = m_0 * exp( -Delta_V / ( I_sp * g_0 ) )
   * t_burn = ( m_0 - m_f ) / ( T / ( I_sp * g_0 ) )
   * Reference: Edelbaum (1961), Wiesel (1997), Curtis (2013) for Hall thruster & ion engine planetary spiral insertions.
   * @param {number} [initialRadiusKm=20000.0] - Initial capture orbital radius r1 in km
   * @param {number} [finalRadiusKm=3770.0] - Final science mapping orbital radius r2 in km
   * @param {number} [thrustNewtons=0.250] - Continuous thruster thrust in Newtons (0.01 to 5.0 N)
   * @param {number} [specificImpulseSec=3000.0] - Thruster specific impulse I_sp in seconds (1500 to 5000 s)
   * @param {number} [initialMassKg=1000.0] - Spacecraft initial wet mass m0 in kg
   * @param {string} [body='mars'] - Central planetary body
   * @returns {{edelbaumDeltaVKmS: number, propellantConsumedKg: number, finalSpacecraftMassKg: number, burnDurationDays: number, meanThrustAccelerationMS2: number, propulsionEfficiencySummary: string}}
   */
  static computeLowThrustContinuousSpiralCaptureDuration(initialRadiusKm = 20000.0, finalRadiusKm = 3770.0, thrustNewtons = 0.250, specificImpulseSec = 3000.0, initialMassKg = 1000.0, body = 'mars') {
    const isEarth = body.toLowerCase() === 'earth';
    const isMoon = body.toLowerCase() === 'moon';

    let mu = 42828.37;
    let Rp = 3396.19;

    if (isEarth) {
      mu = 398600.4418;
      Rp = 6378.137;
    } else if (isMoon) {
      mu = 4902.80;
      Rp = 1737.4;
    }

    const r1 = Math.max(Rp + 50.0, initialRadiusKm);
    const r2 = Math.max(Rp + 50.0, finalRadiusKm);
    const T = Math.max(0.001, thrustNewtons);
    const Isp = Math.max(100.0, specificImpulseSec);
    const m0 = Math.max(1.0, initialMassKg);
    const g0 = 9.80665; // m/s^2

    // Orbital velocities at r1 and r2 (km/s -> m/s)
    const v1MS = Math.sqrt(mu / r1) * 1000.0;
    const v2MS = Math.sqrt(mu / r2) * 1000.0;

    // Edelbaum low-thrust circular spiral Delta-V: Delta_V = |v2 - v1| (m/s)
    const deltaVMS = Math.abs(v2MS - v1MS);
    const deltaVKmS = deltaVMS / 1000.0;

    // Effective exhaust velocity c_e = Isp * g0
    const ce = Isp * g0;

    // Final mass: m_f = m_0 * exp(-Delta_V / c_e)
    const mf = m0 * Math.exp(-deltaVMS / ce);
    const mPropellant = m0 - mf;

    // Mass flow rate: m_dot = T / c_e (kg/s)
    const mDot = T / ce;

    // Burn duration in seconds -> days
    const tBurnSec = mPropellant / mDot;
    const tBurnDays = tBurnSec / 86400.0;

    // Mean thrust acceleration
    const meanMass = (m0 + mf) / 2.0;
    const meanAccMS2 = T / meanMass;

    let summary = 'High-Efficiency Electric Propulsion Low-Thrust Spiral';
    if (tBurnDays > 180.0) {
      summary = 'Extended Multi-Month Low-Thrust Spiral Insertion Campaign';
    } else if (tBurnDays <= 30.0) {
      summary = 'Rapid High-Thrust-to-Weight Electric Orbit Lowering';
    }

    return {
      edelbaumDeltaVKmS: parseFloat(deltaVKmS.toFixed(4)),
      propellantConsumedKg: parseFloat(mPropellant.toFixed(2)),
      finalSpacecraftMassKg: parseFloat(mf.toFixed(2)),
      burnDurationDays: parseFloat(tBurnDays.toFixed(2)),
      meanThrustAccelerationMS2: parseFloat(meanAccMS2.toExponential(4)),
      propulsionEfficiencySummary: summary
    };
  }

  /**
   * Calculate General and Special Relativistic time dilation clock drift and solar Shapiro gravitational signal delay for deep space planetary orbiters.
   * Delta_t / t = - 3 * mu / ( 2 * r * c^2 )
   * Delta_tau_Shapiro = ( 4 * mu_sun / c^3 ) * ln( 4 * r_E * r_target / b^2 )
   * Reference: Shapiro (1964), Will (1993), Bertotti et al. (2003) for Viking and MRO General Relativity radio science tests.
   * @param {number} [orbiterSemiMajorAxisKm=3770.0] - Spacecraft semi-major axis in km
   * @param {number} [sunImpactParameterAU=0.05] - Radio ray-path closest approach to Sun b in AU (0.005 to 1.0 AU)
   * @param {number} [heliocentricDistanceAU=1.524] - Target planetary heliocentric distance in AU
   * @param {string} [body='mars'] - Central planetary body
   * @returns {{fractionalClockDilation: number, dailyClockDriftMicroseconds: number, solarShapiroDelayMicroseconds: number, oneWayRangeErrorMeters: number, relativisticRegime: string}}
   */
  static computeRelativisticTimeDilationAndShapiroDelay(orbiterSemiMajorAxisKm = 3770.0, sunImpactParameterAU = 0.05, heliocentricDistanceAU = 1.524, body = 'mars') {
    const isEarth = body.toLowerCase() === 'earth';
    const mu = isEarth ? 398600.4418 : 42828.37; // km^3/s^2
    const rKm = Math.max(1000.0, orbiterSemiMajorAxisKm);
    const cKmS = 299792.458; // km/s
    const muSunKm3S2 = 1.3271244e11; // km^3/s^2

    // 1. Orbital relativistic clock drift (GR potential + SR velocity)
    const fracDilation = -(1.5 * mu) / (rKm * Math.pow(cKmS, 2.0));
    const secPerDay = 86400.0;
    const dailyDriftSec = fracDilation * secPerDay;
    const dailyDriftUS = dailyDriftSec * 1e6; // microseconds/day

    // 2. Solar Shapiro gravitational radio signal time delay
    const AU_TO_KM = 1.495978707e8;
    const rEarthKm = 1.0 * AU_TO_KM;
    const rTargetKm = Math.max(0.1, heliocentricDistanceAU) * AU_TO_KM;
    const bKm = Math.max(6.9634e5, sunImpactParameterAU * AU_TO_KM); // minimum solar radius

    const shapiroCoeffS = (4.0 * muSunKm3S2) / Math.pow(cKmS, 3.0); // ~1.97e-5 seconds
    const logArg = (4.0 * rEarthKm * rTargetKm) / Math.pow(bKm, 2.0);
    const shapiroDelayS = shapiroCoeffS * Math.log(Math.max(1.0, logArg));
    const shapiroDelayUS = shapiroDelayS * 1e6; // microseconds

    // Equivalent one-way range error (c * delta_tau / 2 in meters)
    const rangeErrorMeters = (cKmS * 1000.0 * shapiroDelayS) / 2.0;

    let regime = 'Deep Space Superior Conjunction (Large Shapiro Delay & Radio Plasma Scintillation)';
    if (sunImpactParameterAU > 0.50) {
      regime = 'Standard Interplanetary Deep Space Network (DSN) Tracking Geometry';
    }

    return {
      fractionalClockDilation: parseFloat(fracDilation.toExponential(4)),
      dailyClockDriftMicroseconds: parseFloat(dailyDriftUS.toFixed(2)),
      solarShapiroDelayMicroseconds: parseFloat(shapiroDelayUS.toFixed(2)),
      oneWayRangeErrorMeters: parseFloat(rangeErrorMeters.toFixed(1)),
      relativisticRegime: regime
    };
  }

  /**
   * Calculate Solar Gravitational Lens (SGL) focal line parameters, Einstein deflection angle, minimum focal distance, and optical amplification gain.
   * alpha_hat = 4 * G * M_sun / ( c^2 * R_sun )
   * z_focal_min = R_sun / alpha_hat
   * mu_gain = 4 * pi^2 * R_sun / lambda
   * d_spot = 1.22 * lambda * z / ( 2 * R_sun )
   * Reference: Eshleman (1979), Turyshev & Toth (2017, 2020) for SGL exoplanet surface imaging at 550+ AU.
   * @param {number} [heliocentricDistanceAU=550.0] - Spacecraft focal line distance in AU (547 to 1000 AU)
   * @param {number} [observingWavelengthMicrons=1.0] - Optical/NIR observing wavelength lambda in microns (0.3 to 10 um)
   * @param {number} [telescopeApertureMeters=1.0] - Spacecraft receiver telescope aperture diameter in meters
   * @returns {{einsteinDeflectionArcsec: number, minimumFocalDistanceAU: number, opticalIntensityGain: number, opticalIntensityGainDB: number, focalSpotDiameterMeters: number, isInsideFocalRegion: boolean, lensStatus: string}}
   */
  static computeSolarGravitationalLensFocalParameters(heliocentricDistanceAU = 550.0, observingWavelengthMicrons = 1.0, telescopeApertureMeters = 1.0) {
    const rAU = Math.max(1.0, heliocentricDistanceAU);
    const lambdaM = Math.max(0.1, observingWavelengthMicrons) * 1e-6;
    const dTel = Math.max(0.1, telescopeApertureMeters);

    const AU_TO_M = 1.495978707e11;
    const R_SUN_M = 6.9634e8;
    const MU_SUN_M3S2 = 1.3271244e20; // m^3/s^2
    const C_MS = 299792458.0;

    // Einstein deflection angle alpha_hat = 4 * mu / (c^2 * R_sun) (radians and arcsec)
    const alphaRad = (4.0 * MU_SUN_M3S2) / (Math.pow(C_MS, 2.0) * R_SUN_M);
    const alphaArcsec = (alphaRad * 180.0 * 3600.0) / Math.PI;

    // Minimum focal distance z_min = R_sun / alpha_hat (m and AU)
    const zMinM = R_SUN_M / alphaRad;
    const zMinAU = zMinM / AU_TO_M;

    const zTargetM = rAU * AU_TO_M;
    const inFocal = rAU >= zMinAU;

    // Peak monochromatic optical intensity gain mu_gain = 4 * pi^2 * R_sun / lambda
    const muGain = (4.0 * Math.pow(Math.PI, 2.0) * R_SUN_M) / lambdaM;
    const gainDB = 10.0 * Math.log10(muGain);

    // Focal spot diameter d_spot = 1.22 * lambda * z / (2 * R_sun)
    const spotDiamM = (1.22 * lambdaM * zTargetM) / (2.0 * R_SUN_M);

    let status = 'Pre-Focal Transit (Sun Subtends Larger Angle than Einstein Ring - No Amplification)';
    if (inFocal) {
      status = 'Active Solar Gravitational Lens Focal Line (Massive ~10^16 Intensity Amplification for Exoplanet Kilopixel Imaging)';
    }

    return {
      einsteinDeflectionArcsec: parseFloat(alphaArcsec.toFixed(4)),
      minimumFocalDistanceAU: parseFloat(zMinAU.toFixed(2)),
      opticalIntensityGain: parseFloat(muGain.toExponential(4)),
      opticalIntensityGainDB: parseFloat(gainDB.toFixed(2)),
      focalSpotDiameterMeters: parseFloat(spotDiamM.toFixed(2)),
      isInsideFocalRegion: inFocal,
      lensStatus: status
    };
  }

  /**
   * Calculate planetary gravity assist / swingby hyperbolic deflection angle, asymptotic Delta-V gain, and impact parameter.
   * e = 1 + ( r_p * v_inf^2 ) / mu_p
   * delta = 2 * arcsin( 1 / e )
   * Delta_V_max = 2 * v_inf * sin( delta / 2 ) = 2 * v_inf / e
   * b = r_p * sqrt( 1 + 2 * mu_p / ( r_p * v_inf^2 ) )
   * Reference: Battin (1999), Vallado (2013), Curtis (2013) for interplanetary gravity assist mission trajectories.
   * @param {number} [hyperbolicExcessVelocityKmS=4.5] - Incoming hyperbolic excess velocity v_infinity in km/s (0.5 to 25 km/s)
   * @param {number} [periapsisAltitudeKm=300.0] - Flyby closest approach altitude h_p in km (50 to 50000 km)
   * @param {string} [bodyFlyby='mars'] - Flyby planetary body (mars, earth, jupiter, venus, moon)
   * @returns {{hyperbolicEccentricity: number, deflectionAngleDeg: number, maxAsymptoticDeltaVKmS: number, impactParameterKm: number, periapsisVelocityKmS: number, swingbyFeasibility: string}}
   */
  static computePlanetaryGravityAssistDeflectionAndDeltaV(hyperbolicExcessVelocityKmS = 4.5, periapsisAltitudeKm = 300.0, bodyFlyby = 'mars') {
    const vInf = Math.max(0.1, hyperbolicExcessVelocityKmS);
    const hp = Math.max(20.0, periapsisAltitudeKm);
    const bodyLower = (bodyFlyby || 'mars').toLowerCase();

    let muP = 42828.37;
    let Rp = 3396.19;

    if (bodyLower === 'earth') {
      muP = 398600.4418;
      Rp = 6378.137;
    } else if (bodyLower === 'jupiter') {
      muP = 126686534.0;
      Rp = 71492.0;
    } else if (bodyLower === 'venus') {
      muP = 324859.0;
      Rp = 6051.8;
    } else if (bodyLower === 'moon') {
      muP = 4902.80;
      Rp = 1737.4;
    }

    // Periapsis radius r_p = Rp + hp (km)
    const rpKm = Rp + hp;

    // Hyperbolic eccentricity e = 1 + (rp * vInf^2) / muP
    const vInfSq = Math.pow(vInf, 2.0);
    const e = 1.0 + (rpKm * vInfSq) / muP;

    // Deflection angle delta = 2 * arcsin(1 / e) in radians and degrees
    const deltaRad = 2.0 * Math.asin(1.0 / e);
    const deltaDeg = (deltaRad * 180.0) / Math.PI;

    // Maximum asymptotic velocity change Delta_V_max = 2 * vInf / e (km/s)
    const maxDeltaVKmS = (2.0 * vInf) / e;

    // Impact parameter b = rp * sqrt( 1 + 2*muP / (rp * vInf^2) ) (km)
    const bKm = rpKm * Math.sqrt(1.0 + (2.0 * muP) / (rpKm * vInfSq));

    // Periapsis velocity v_p = sqrt( vInf^2 + 2*muP/rp ) (km/s)
    const vpKmS = Math.sqrt(vInfSq + (2.0 * muP) / rpKm);

    let feasibility = 'High-Efficiency Interplanetary Gravity Assist Deflection';
    if (deltaDeg < 15.0) {
      feasibility = 'Low-Deflection Flyby (High Hyperbolic Speed / Weak Gravitational Bending)';
    } else if (deltaDeg > 90.0) {
      feasibility = 'Extreme Deep-Gravity Well Turnaround Maneuver';
    }

    return {
      hyperbolicEccentricity: parseFloat(e.toFixed(4)),
      deflectionAngleDeg: parseFloat(deltaDeg.toFixed(2)),
      maxAsymptoticDeltaVKmS: parseFloat(maxDeltaVKmS.toFixed(3)),
      impactParameterKm: parseFloat(bKm.toFixed(1)),
      periapsisVelocityKmS: parseFloat(vpKmS.toFixed(3)),
      swingbyFeasibility: feasibility
    };
  }

  /**
   * Calculate single-pass aerobraking atmospheric drag velocity dissipation, orbital energy loss, and apoapsis altitude reduction.
   * Delta_V_drag = ( rho_p / beta ) * sqrt( pi * mu * H_s * r_p / ( 2 * a * e ) )
   * Delta_energy = - v_p * Delta_V_drag
   * Delta_r_a = r_a - ( 2 * a_new - r_p )
   * Reference: King-Hele (1987), Lyons (1992), Spencer & Tolson (2007) for MGS, Odyssey, and MRO Mars aerobraking operations.
   * @param {number} [apoapsisRadiusKm=30000.0] - Pre-pass apoapsis radius in km
   * @param {number} [periapsisRadiusKm=3520.0] - Aerobraking corridor periapsis radius in km (~124 km altitude on Mars)
   * @param {number} [atmosphericDensityAtPeriapsisKgM3=3.5e-9] - Atmospheric density at periapsis in kg/m^3 (1e-10 to 1e-7 kg/m^3)
   * @param {number} [scaleHeightKm=7.5] - Local atmospheric scale height in km
   * @param {number} [ballisticCoeffKgM2=80.0] - Spacecraft ballistic coefficient m / (Cd * A) in kg/m^2
   * @param {string} [body='mars'] - Planetary body
   * @returns {{dragDeltaVMS: number, energyDissipationJPerKg: number, apoapsisDecayKm: number, newApoapsisRadiusKm: number, newOrbitalPeriodHours: number, aerobrakingPassRegime: string}}
   */
  static computeAerobrakingPassEnergyDissipationAndApoapsisDecay(apoapsisRadiusKm = 30000.0, periapsisRadiusKm = 3520.0, atmosphericDensityAtPeriapsisKgM3 = 3.5e-9, scaleHeightKm = 7.5, ballisticCoeffKgM2 = 80.0, body = 'mars') {
    const isEarth = body.toLowerCase() === 'earth';
    const isVenus = body.toLowerCase() === 'venus';

    let mu = 42828.37e9; // m^3/s^2 (Mars)
    let Rp = 3396.19e3; // m

    if (isEarth) {
      mu = 398600.4418e9;
      Rp = 6378.137e3;
    } else if (isVenus) {
      mu = 324859.0e9;
      Rp = 6051.8e3;
    }

    const raM = Math.max(periapsisRadiusKm * 1000.0 + 1000.0, apoapsisRadiusKm * 1000.0);
    const rpM = Math.max(Rp + 50000.0, periapsisRadiusKm * 1000.0);
    const rhoP = Math.max(1e-12, atmosphericDensityAtPeriapsisKgM3);
    const HsM = Math.max(500.0, scaleHeightKm * 1000.0);
    const beta = Math.max(1.0, ballisticCoeffKgM2);

    // Orbital geometry in SI (meters)
    const aM = (raM + rpM) / 2.0;
    const e = (raM - rpM) / (raM + rpM);

    // Periapsis velocity v_p = sqrt( mu * ( 2/rp - 1/a ) ) (m/s)
    const vpMS = Math.sqrt(mu * ((2.0 / rpM) - (1.0 / aM)));

    // Specific orbital energy eps = - mu / (2 * a) (J/kg)
    const epsOld = -mu / (2.0 * aM);

    // King-Hele drag velocity dissipation Delta_V_drag (m/s)
    const kingHeleFactor = Math.sqrt((Math.PI * mu * HsM * rpM) / (2.0 * aM * e));
    const deltaVDragMS = (rhoP / beta) * kingHeleFactor;

    // Energy dissipation per pass Delta_eps = - vp * Delta_V_drag (J/kg)
    const deltaEps = -vpMS * deltaVDragMS;
    const epsNew = epsOld + deltaEps;

    // New semi-major axis and new apoapsis
    const aNewM = -mu / (2.0 * epsNew);
    const raNewM = 2.0 * aNewM - rpM;
    const deltaRaKm = (raM - raNewM) / 1000.0;
    const raNewKm = raNewM / 1000.0;

    // New orbital period T = 2 * pi * sqrt( a^3 / mu ) in hours
    const periodSec = 2.0 * Math.PI * Math.sqrt(Math.pow(aNewM, 3.0) / mu);
    const periodHours = periodSec / 3600.0;

    let regime = 'Nominal Aerobraking Corridor Pass';
    if (deltaRaKm < 1.0) {
      regime = 'Shallow Walk-In Corridor (Low Dynamic Pressure & Slow Decay)';
    } else if (deltaRaKm > 100.0) {
      regime = 'Aggressive Deep-Corridor Aerobraking Dip (High Thermal Flux & Rapid Decay)';
    }

    return {
      dragDeltaVMS: parseFloat(deltaVDragMS.toFixed(4)),
      energyDissipationJPerKg: parseFloat(deltaEps.toFixed(2)),
      apoapsisDecayKm: parseFloat(deltaRaKm.toFixed(2)),
      newApoapsisRadiusKm: parseFloat(raNewKm.toFixed(2)),
      newOrbitalPeriodHours: parseFloat(periodHours.toFixed(2)),
      aerobrakingPassRegime: regime
    };
  }

  /**
   * Calculate single-pass hypersonic aerocapture velocity depletion Delta-V, propellant mass saved, and post-atmospheric orbit capture geometry.
   * Delta_V_req = sqrt( v_inf^2 + 2*mu/r_p ) - sqrt( 2*mu*r_a / ( r_p * (r_a + r_p) ) )
   * Delta_m_saved = m_0 * ( 1 - exp( -Delta_V_req / ( I_sp * g_0 ) ) )
   * Reference: Cruz et al. (2006), Braun & Manning (2007), Lu (2014) for Mars Sample Return and NASA Ice Giant aerocapture systems.
   * @param {number} [hyperbolicApproachVelocityKmS=5.6] - Hyperbolic approach excess velocity v_infinity in km/s (3.0 to 12.0 km/s)
   * @param {number} [targetCaptureApoapsisKm=6000.0] - Target post-capture apoapsis altitude in km
   * @param {number} [periapsisAltitudeKm=50.0] - Aerocapture atmospheric interface periapsis altitude in km (30 to 80 km)
   * @param {number} [spacecraftMassKg=1000.0] - Spacecraft entry mass m0 in kg
   * @param {number} [chemicalIspSec=320.0] - Equivalent chemical propulsion specific impulse in seconds
   * @param {string} [body='mars'] - Target planetary body
   * @returns {{hyperbolicPeriapsisVelocityKmS: number, capturedPeriapsisVelocityKmS: number, requiredAtmosphericDeltaVKmS: number, propellantMassSavedKg: number, propellantSavingsPercent: number, capturedEccentricity: number, aerocaptureFeasibility: string}}
   */
  static computeAerocaptureHypersonicPassCaptureParameters(hyperbolicApproachVelocityKmS = 5.6, targetCaptureApoapsisKm = 6000.0, periapsisAltitudeKm = 50.0, spacecraftMassKg = 1000.0, chemicalIspSec = 320.0, body = 'mars') {
    const isEarth = body.toLowerCase() === 'earth';
    const isVenus = body.toLowerCase() === 'venus';

    let mu = 42828.37; // km^3/s^2 (Mars)
    let Rp = 3396.19; // km

    if (isEarth) {
      mu = 398600.4418;
      Rp = 6378.137;
    } else if (isVenus) {
      mu = 324859.0;
      Rp = 6051.8;
    }

    const vInf = Math.max(0.5, hyperbolicApproachVelocityKmS);
    const ha = Math.max(100.0, targetCaptureApoapsisKm);
    const hp = Math.max(10.0, periapsisAltitudeKm);
    const m0 = Math.max(1.0, spacecraftMassKg);
    const Isp = Math.max(100.0, chemicalIspSec);
    const g0 = 9.80665e-3; // km/s^2

    // Periapsis and apoapsis radii (km)
    const rpKm = Rp + hp;
    const raKm = Rp + ha;

    // Hyperbolic entry velocity at periapsis: v_p,hyp = sqrt( vInf^2 + 2*mu/rp ) (km/s)
    const vpHypKmS = Math.sqrt(Math.pow(vInf, 2.0) + (2.0 * mu) / rpKm);

    // Elliptic captured velocity at periapsis: v_p,cap = sqrt( 2*mu*ra / ( rp * (ra + rp) ) ) (km/s)
    const vpCapKmS = Math.sqrt((2.0 * mu * raKm) / (rpKm * (raKm + rpKm)));

    // Required atmospheric velocity reduction Delta_V_req = v_p,hyp - v_p,cap (km/s)
    const deltaVReqKmS = Math.max(0.0, vpHypKmS - vpCapKmS);

    // Equivalent chemical propellant saved: Delta_m = m0 * ( 1 - exp(-Delta_V / (Isp * g0)) )
    const ce = Isp * g0;
    const mSavedKg = m0 * (1.0 - Math.exp(-deltaVReqKmS / ce));
    const savingsPct = (mSavedKg / m0) * 100.0;

    // Captured orbit eccentricity e = (ra - rp) / (ra + rp)
    const eCap = (raKm - rpKm) / (raKm + rpKm);

    let feasibility = 'High-Margin Aerocapture Orbit Insertion';
    if (deltaVReqKmS > 4.5) {
      feasibility = 'Extreme Hypersonic Energy Dissipation (Severe Thermal Protection System TPS Load)';
    } else if (deltaVReqKmS < 1.0) {
      feasibility = 'Low-Energy Aerocapture Transfer';
    }

    return {
      hyperbolicPeriapsisVelocityKmS: parseFloat(vpHypKmS.toFixed(3)),
      capturedPeriapsisVelocityKmS: parseFloat(vpCapKmS.toFixed(3)),
      requiredAtmosphericDeltaVKmS: parseFloat(deltaVReqKmS.toFixed(3)),
      propellantMassSavedKg: parseFloat(mSavedKg.toFixed(2)),
      propellantSavingsPercent: parseFloat(savingsPct.toFixed(2)),
      capturedEccentricity: parseFloat(eCap.toFixed(4)),
      aerocaptureFeasibility: feasibility
    };
  }

  /**
   * Calculate hypersonic stagnation point convective heat flux and integrated thermal load using Sutton-Graves relation for aeroshell TPS sizing.
   * q_s = k_SG * sqrt( rho / R_N ) * v^3
   * Q_load = q_s * sqrt( pi * H_s / ( 2 * v * |sin(gamma)| ) )
   * Reference: Sutton & Graves (1971), Tauber & Sutton (1991), Wright et al. (2006) for Mars Pathfinder, MER, MSL, and InSight TPS sizing.
   * @param {number} [entryVelocityKmS=5.5] - Hypersonic atmospheric velocity in km/s (1.0 to 15.0 km/s)
   * @param {number} [atmosphericDensityKgM3=1.5e-4] - Free-stream atmospheric density in kg/m^3
   * @param {number} [noseRadiusMeters=0.66] - Aeroshell effective spherical nose radius in meters (0.1 to 5.0 m)
   * @param {number} [entryFlightPathAngleDeg=-12.0] - Atmospheric entry flight path angle gamma in degrees
   * @param {string} [body='mars'] - Target planetary body
   * @returns {{stagnationHeatFluxWPerCm2: number, stagnationHeatFluxKWPerM2: number, integratedHeatLoadJPerCm2: number, integratedHeatLoadMJPerM2: number, tpsMaterialSuitability: string, radiativeHeatingRegime: string}}
   */
  static computeHypersonicStagnationConvectiveHeatFlux(entryVelocityKmS = 5.5, atmosphericDensityKgM3 = 1.5e-4, noseRadiusMeters = 0.66, entryFlightPathAngleDeg = -12.0, body = 'mars') {
    const vMS = Math.max(100.0, entryVelocityKmS * 1000.0);
    const rho = Math.max(1e-10, atmosphericDensityKgM3);
    const Rn = Math.max(0.01, noseRadiusMeters);
    const gammaRad = Math.abs(entryFlightPathAngleDeg) * (Math.PI / 180.0);
    const sinGamma = Math.max(0.05, Math.sin(gammaRad));

    const isEarth = body.toLowerCase() === 'earth';
    const isVenus = body.toLowerCase() === 'venus';

    let kSG = 1.9027e-4; // kg^0.5 / m for CO2 (Mars)
    let HsM = 11100.0; // scale height (m)

    if (isEarth) {
      kSG = 1.7415e-4; // Earth N2-O2
      HsM = 8500.0;
    } else if (isVenus) {
      kSG = 1.898e-4;
      HsM = 15900.0;
    }

    // Stagnation point heat flux q_s = kSG * sqrt(rho / Rn) * v^3 (W/m^2)
    const qsWM2 = kSG * Math.sqrt(rho / Rn) * Math.pow(vMS, 3.0);
    const qsWPerCm2 = qsWM2 / 10000.0; // W/cm^2
    const qsKWPerM2 = qsWM2 / 1000.0; // kW/m^2

    // Approximate integrated heat load Q_load = qs * sqrt( pi * Hs / ( 2 * v * sin(gamma) ) ) (J/m^2)
    const tEffSec = Math.sqrt((Math.PI * HsM) / (2.0 * vMS * sinGamma));
    const qLoadJM2 = qsWM2 * tEffSec;
    const qLoadJPerCm2 = qLoadJM2 / 10000.0; // J/cm^2
    const qLoadMJM2 = qLoadJM2 / 1e6; // MJ/m^2

    // TPS material evaluation
    let tps = 'SLA-561V / SIRCA Silicone Elastomer (Pathfinder / MER / InSight Class)';
    if (qsWPerCm2 > 250.0) {
      tps = 'PICA / C-PICA Phenolic Carbon Ablator (MSL / Perseverance / Stardust Ultra-High Heat Flux Class)';
    } else if (qsWPerCm2 <= 40.0) {
      tps = 'Reusable Ceramic Tiles / Low-Density Carbon-Carbon (Shuttle / Spaceplane Class)';
    }

    let rad = 'Negligible Shock Layer Radiative Heating (Pure Convective Regime)';
    if (entryVelocityKmS > 7.5) {
      rad = 'Significant CO2 / CN Shock Layer Radiative Emission (Coupled Convective + Radiative TPS Sizing Required)';
    }

    return {
      stagnationHeatFluxWPerCm2: parseFloat(qsWPerCm2.toFixed(2)),
      stagnationHeatFluxKWPerM2: parseFloat(qsKWPerM2.toFixed(2)),
      integratedHeatLoadJPerCm2: parseFloat(qLoadJPerCm2.toFixed(2)),
      integratedHeatLoadMJPerM2: parseFloat(qLoadMJM2.toFixed(2)),
      tpsMaterialSuitability: tps,
      radiativeHeatingRegime: rad
    };
  }

  /**
   * Calculate analytical peak aerodynamic deceleration, G-load, altitude of peak drag, and velocity using the Allen-Eggers entry solution.
   * a_max = ( v_entry^2 * |sin(gamma)| ) / ( 2 * e * H_s )
   * h_max = H_s * ln( ( rho_0 * H_s ) / ( 2 * beta * |sin(gamma)| ) )
   * v(h_max) = v_entry / sqrt(e)
   * Reference: Allen & Eggers (1958), Vinh et al. (1980), Braun & Manning (2007) for Mars Pathfinder, MER, MSL, and InSight EDL aerodynamics.
   * @param {number} [entryVelocityKmS=5.7] - Atmospheric entry interface velocity in km/s (3.0 to 12.0 km/s)
   * @param {number} [entryFlightPathAngleDeg=-12.5] - Entry flight path angle gamma in degrees (-5.0 to -30.0 deg)
   * @param {number} [ballisticCoeffKgM2=120.0] - Spacecraft ballistic coefficient m / (Cd * A) in kg/m^2
   * @param {number} [scaleHeightKm=11.1] - Atmospheric scale height in km
   * @param {string} [body='mars'] - Target planetary body
   * @returns {{peakDecelerationMS2: number, peakDecelerationGs: number, altitudeOfPeakDecelerationKm: number, velocityAtPeakDecelerationKmS: number, structuralLoadRegime: string}}
   */
  static computeHypersonicEntryPeakDecelerationGLoad(entryVelocityKmS = 5.7, entryFlightPathAngleDeg = -12.5, ballisticCoeffKgM2 = 120.0, scaleHeightKm = 11.1, body = 'mars') {
    const vEntryMS = Math.max(500.0, entryVelocityKmS * 1000.0);
    const gammaRad = Math.abs(entryFlightPathAngleDeg) * (Math.PI / 180.0);
    const sinGamma = Math.max(0.02, Math.sin(gammaRad));
    const beta = Math.max(5.0, ballisticCoeffKgM2);
    const HsM = Math.max(1000.0, scaleHeightKm * 1000.0);
    const g0 = 9.80665; // m/s^2

    const isEarth = body.toLowerCase() === 'earth';
    const isVenus = body.toLowerCase() === 'venus';

    let rho0 = 0.020; // kg/m^3 Mars surface ref density
    if (isEarth) {
      rho0 = 1.225;
    } else if (isVenus) {
      rho0 = 65.0;
    }

    // Peak deceleration a_max = ( v_entry^2 * sin(gamma) ) / ( 2 * e * Hs ) (m/s^2)
    const eConst = Math.E;
    const aMaxMS2 = (Math.pow(vEntryMS, 2.0) * sinGamma) / (2.0 * eConst * HsM);
    const gMax = aMaxMS2 / g0;

    // Velocity at peak deceleration v(h_max) = v_entry / sqrt(e) (m/s)
    const vAtPeakMS = vEntryMS / Math.sqrt(eConst);
    const vAtPeakKmS = vAtPeakMS / 1000.0;

    // Altitude of peak deceleration h_max = Hs * ln( (rho0 * Hs) / (2 * beta * sin(gamma)) ) (m)
    const denom = 2.0 * beta * sinGamma;
    const arg = (rho0 * HsM) / Math.max(1e-6, denom);
    let hMaxM = 0.0;
    if (arg > 1.0) {
      hMaxM = HsM * Math.log(arg);
    }
    const hMaxKm = hMaxM / 1000.0;

    let regime = 'Nominal Robotic EDL Deceleration (< 15 Gs)';
    if (gMax > 25.0) {
      regime = 'Extreme High-G Ballistic Entry (Severe Structural & Avionics Inertial Stress)';
    } else if (gMax <= 6.0) {
      regime = 'Crew-Safe Shallow Gliding Entry (< 6 Gs)';
    }

    return {
      peakDecelerationMS2: parseFloat(aMaxMS2.toFixed(2)),
      peakDecelerationGs: parseFloat(gMax.toFixed(2)),
      altitudeOfPeakDecelerationKm: parseFloat(hMaxKm.toFixed(2)),
      velocityAtPeakDecelerationKmS: parseFloat(vAtPeakKmS.toFixed(3)),
      structuralLoadRegime: regime
    };
  }

  /**
   * Calculate Disk-Gap-Band (DGB) supersonic parachute deployment corridor feasibility, inflation shock opening load, and terminal descent speed.
   * F_opening = C_x * q_deploy * ( pi * D^2 / 4 )
   * v_terminal = sqrt( ( 2 * m * g ) / ( rho * C_d * S_0 ) )
   * Reference: Cruz et al. (2003), Witkowski et al. (2004), Steltzner et al. (2006), Sengupta et al. (2009) for Viking, MER, MSL, and Perseverance EDL.
   * @param {number} [currentMachNumber=1.85] - Deployment Mach number (1.2 to 2.5)
   * @param {number} [dynamicPressurePa=550.0] - Dynamic pressure at mortar firing in Pa (200 to 1200 Pa)
   * @param {number} [spacecraftMassKg=1950.0] - Suspended entry mass in kg (100 to 5000 kg)
   * @param {number} [chuteDiameterMeters=21.5] - Nominal parachute canopy diameter D0 in meters (5 to 35 m)
   * @param {string} [body='mars'] - Target planetary body
   * @returns {{openingShockForceKN: number, openingDecelerationGs: number, parachuteCanopyAreaM2: number, terminalDescentSpeedMS: number, terminalDescentSpeedKmH: number, parachuteDeploymentStatus: string}}
   */
  static computeSupersonicParachuteDeploymentCorridor(currentMachNumber = 1.85, dynamicPressurePa = 550.0, spacecraftMassKg = 1950.0, chuteDiameterMeters = 21.5, body = 'mars') {
    const M = Math.max(0.5, currentMachNumber);
    const qPa = Math.max(50.0, dynamicPressurePa);
    const massKg = Math.max(10.0, spacecraftMassKg);
    const D0 = Math.max(1.0, chuteDiameterMeters);

    const isEarth = body.toLowerCase() === 'earth';
    const isVenus = body.toLowerCase() === 'venus';

    let gPlanet = 3.72076; // m/s^2 (Mars)
    let rhoLow = 0.015; // kg/m^3 (Mars low altitude)

    if (isEarth) {
      gPlanet = 9.80665;
      rhoLow = 1.0;
    } else if (isVenus) {
      gPlanet = 8.87;
      rhoLow = 15.0;
    }

    // Parachute nominal area S0 = pi * D0^2 / 4 (m^2)
    const S0 = (Math.PI * Math.pow(D0, 2.0)) / 4.0;

    // Dynamic opening shock factor Cx (~1.55 for DGB parachutes)
    const Cx = 1.55;
    const fOpeningN = Cx * qPa * S0;
    const fOpeningKN = fOpeningN / 1000.0;

    // Opening deceleration Gs = F_opening / ( mass * g0 )
    const g0 = 9.80665;
    const aOpeningMS2 = fOpeningN / massKg;
    const gOpening = aOpeningMS2 / g0;

    // Subsonic terminal descent velocity v_terminal = sqrt( 2 * m * g / ( rho * Cd * S0 ) )
    const CdChute = 0.60;
    const vTerminalMS = Math.sqrt((2.0 * massKg * gPlanet) / Math.max(1e-6, rhoLow * CdChute * S0));
    const vTerminalKmH = vTerminalMS * 3.6;

    // Deployment corridor validation (1.4 <= M <= 2.2, 300 <= q <= 850 Pa)
    let status = 'Nominal Supersonic DGB Parachute Deployment Envelope';
    if (M < 1.4) {
      status = 'Late Subsonic Mortar Trigger (Low Altitude Risk / Terrain Clearance Hazard)';
    } else if (M > 2.25) {
      status = 'High-Mach Deployment Violation (Severe Canopy Asymmetric Inflation & Area Oscillations)';
    } else if (qPa > 850.0) {
      status = 'High Dynamic Pressure Violation (Extreme Line Tension & Canopy Structural Rupture Risk)';
    } else if (qPa < 300.0) {
      status = 'Low Dynamic Pressure Deployment (Squidding / Delayed Canopy Inflation)';
    }

    return {
      openingShockForceKN: parseFloat(fOpeningKN.toFixed(2)),
      openingDecelerationGs: parseFloat(gOpening.toFixed(2)),
      parachuteCanopyAreaM2: parseFloat(S0.toFixed(2)),
      terminalDescentSpeedMS: parseFloat(vTerminalMS.toFixed(2)),
      terminalDescentSpeedKmH: parseFloat(vTerminalKmH.toFixed(2)),
      parachuteDeploymentStatus: status
    };
  }

  /**
   * Calculate powered descent initiation (PDI) gravity turn trajectory, gravity loss Delta-V penalty, burn duration, and propellant mass budget.
   * Delta_V_total = ( v_0 - v_f + Delta_V_grav ) * ( 1 + margin )
   * m_propellant = m_0 * ( 1 - exp( -Delta_V_total / ( I_sp * g_0 ) ) )
   * Reference: Sostaric & Rea (2005), Blackmore et al. (2010), Acikmese et al. (2013) for MSL/Perseverance Sky Crane, Phoenix, and InSight landings.
   * @param {number} [initialAltitudeMeters=1500.0] - Powered descent initiation altitude in meters (500 to 5000 m)
   * @param {number} [initialVelocityMS=80.0] - Spacecraft velocity at parachute separation in m/s (40 to 150 m/s)
   * @param {number} [initialFlightPathAngleDeg=-65.0] - Velocity vector angle relative to horizontal in degrees (-30 to -90 deg)
   * @param {number} [thrustToWeightRatio=2.5] - Descent stage thrust-to-weight ratio T/W (1.5 to 5.0)
   * @param {number} [specificImpulseSec=225.0] - Monopropellant/bipropellant specific impulse in seconds
   * @param {number} [spacecraftMassKg=1950.0] - Spacecraft total mass at PDI in kg
   * @param {string} [body='mars'] - Target planetary body
   * @returns {{kinematicDeltaVMS: number, gravityLossDeltaVMS: number, totalMissionDeltaVMS: number, burnDurationSec: number, propellantConsumedKg: number, propellantFractionPercent: number, descentGuidanceRegime: string}}
   */
  static computePoweredDescentGravityTurnPropellantBudget(initialAltitudeMeters = 1500.0, initialVelocityMS = 80.0, initialFlightPathAngleDeg = -65.0, thrustToWeightRatio = 2.5, specificImpulseSec = 225.0, spacecraftMassKg = 1950.0, body = 'mars') {
    const h0 = Math.max(50.0, initialAltitudeMeters);
    const v0 = Math.max(5.0, initialVelocityMS);
    const gammaRad = Math.abs(initialFlightPathAngleDeg) * (Math.PI / 180.0);
    const tw = Math.max(1.1, thrustToWeightRatio);
    const Isp = Math.max(100.0, specificImpulseSec);
    const m0 = Math.max(10.0, spacecraftMassKg);
    const g0 = 9.80665; // m/s^2

    const isEarth = body.toLowerCase() === 'earth';
    const isMoon = body.toLowerCase() === 'moon';

    let gPlanet = 3.72076; // m/s^2 (Mars)
    if (isEarth) {
      gPlanet = 9.80665;
    } else if (isMoon) {
      gPlanet = 1.62;
    }

    const vf = 0.75; // touchdown velocity (m/s)
    const deltaVKin = Math.max(0.0, v0 - vf);

    // Net deceleration a_net = (TW - 1) * g_planet (m/s^2)
    const aNet = (tw - 1.0) * gPlanet;
    const tBurnSec = v0 / aNet;

    // Gravity loss Delta_V_grav = g_planet * t_burn * sin(gamma_mean) (m/s)
    const gammaMeanRad = (gammaRad + Math.PI / 2.0) / 2.0; // transitions toward vertical -90 deg
    const deltaVGrav = gPlanet * tBurnSec * Math.sin(gammaMeanRad);

    // Total Delta-V including 15% flight control & divert margin
    const margin = 0.15;
    const deltaVTotal = (deltaVKin + deltaVGrav) * (1.0 + margin);

    // Rocket equation propellant mass consumed m_p = m0 * ( 1 - exp(-Delta_V / (Isp * g0)) )
    const ce = Isp * g0;
    const mPropKg = m0 * (1.0 - Math.exp(-deltaVTotal / ce));
    const propFractionPct = (mPropKg / m0) * 100.0;

    let regime = 'Nominal Sky Crane / Constant Deceleration Soft Landing';
    if (tBurnSec > 45.0) {
      regime = 'High Gravity Loss Extended Hover (Excessive Propellant Consumption)';
    } else if (tw > 4.0) {
      regime = 'Aggressive High-Thrust Pinch Deceleration (High G-Load on Payload)';
    }

    return {
      kinematicDeltaVMS: parseFloat(deltaVKin.toFixed(2)),
      gravityLossDeltaVMS: parseFloat(deltaVGrav.toFixed(2)),
      totalMissionDeltaVMS: parseFloat(deltaVTotal.toFixed(2)),
      burnDurationSec: parseFloat(tBurnSec.toFixed(2)),
      propellantConsumedKg: parseFloat(mPropKg.toFixed(2)),
      propellantFractionPercent: parseFloat(propFractionPct.toFixed(2)),
      descentGuidanceRegime: regime
    };
  }

  /**
   * Calculate Sky Crane triple-bridle tension equilibrium, touchdown unloading detection, and descent stage flyaway divert separation.
   * T_line = ( m_rover * g ) / ( 3 * cos( theta_bridle ) )
   * x_impact = ( v_flyaway^2 * sin( 2 * theta_pitch ) ) / g
   * Reference: Steltzner et al. (2006), Sell et al. (2013), Way et al. (2013) for Curiosity and Perseverance Sky Crane touchdowns.
   * @param {number} [roverMassKg=1025.0] - Rover mass on bridle in kg (500 to 2500 kg)
   * @param {number} [descentStageMassKg=900.0] - Descent stage dry mass in kg
   * @param {number} [bridleAngleDeg=12.0] - Bridle line angle from vertical in degrees
   * @param {number} [flyawayDeltaVMS=35.0] - Flyaway divert burn Delta-V in m/s (15 to 60 m/s)
   * @param {number} [flyawayPitchAngleDeg=45.0] - Flyaway climb-out pitch angle in degrees
   * @param {string} [body='mars'] - Target planetary body
   * @returns {{totalRoverWeightN: number, singleBridleTensionN: number, touchdownTensionDropThresholdPercent: number, flyawaySeparationSpeedMS: number, downrangeImpactDistanceMeters: number, touchdownSafetyAssessment: string}}
   */
  static computeSkyCraneBridleDescentTensionAndFlyawayVelocity(roverMassKg = 1025.0, descentStageMassKg = 900.0, bridleAngleDeg = 12.0, flyawayDeltaVMS = 35.0, flyawayPitchAngleDeg = 45.0, body = 'mars') {
    const mRover = Math.max(10.0, roverMassKg);
    const mStage = Math.max(10.0, descentStageMassKg);
    const thetaBridleRad = Math.abs(bridleAngleDeg) * (Math.PI / 180.0);
    const vFlyMS = Math.max(5.0, flyawayDeltaVMS);
    const thetaPitchRad = Math.max(0.1, Math.min(80.0, flyawayPitchAngleDeg)) * (Math.PI / 180.0);

    let gPlanet = 3.72076; // m/s^2 (Mars)
    if (body.toLowerCase() === 'earth') {
      gPlanet = 9.80665;
    } else if (body.toLowerCase() === 'moon') {
      gPlanet = 1.62;
    }

    // Total rover weight in planet gravity (N)
    const wRoverN = mRover * gPlanet;

    // Single bridle line tension (3 lines at angle theta)
    const tSingleN = wRoverN / (3.0 * Math.max(0.1, Math.cos(thetaBridleRad)));

    // Ballistic downrange impact distance of descent stage x = (v^2 * sin(2*theta)) / g (m)
    const xImpactM = (Math.pow(vFlyMS, 2.0) * Math.sin(2.0 * thetaPitchRad)) / gPlanet;

    let safety = 'Nominal Sky Crane Touchdown & High-Margin Flyaway Divert (> 250 m Clearance)';
    if (xImpactM < 150.0) {
      safety = 'Hazardous Close Flyaway Impact (< 150 m from Rover - Dust & Plume Contamination Risk)';
    } else if (xImpactM > 600.0) {
      safety = 'Ultra-Long Range Flyaway Divert (Deep Valley Terrain Clearance)';
    }

    return {
      totalRoverWeightN: parseFloat(wRoverN.toFixed(2)),
      singleBridleTensionN: parseFloat(tSingleN.toFixed(2)),
      touchdownTensionDropThresholdPercent: 80.0,
      flyawaySeparationSpeedMS: parseFloat(vFlyMS.toFixed(2)),
      downrangeImpactDistanceMeters: parseFloat(xImpactM.toFixed(1)),
      touchdownSafetyAssessment: safety
    };
  }

  /**
   * Calculate Solar Radiation Pressure (SRP) force, orbital acceleration perturbation, and cumulative daily velocity drift.
   * P_srp(r) = ( S_0 / c ) * ( 1 AU / r )^2
   * a_srp = ( P_srp * A * C_R ) / m_sc
   * Delta_V_day = a_srp * 86400 s
   * Reference: Milani et al. (1987), Montenbruck & Gill (2000), Vallado (2013) for MAVEN, Mars Express, and MRO orbit determination.
   * @param {number} [heliocentricDistanceAU=1.524] - Spacecraft heliocentric distance in AU (0.3 to 30.0 AU; Mars mean is 1.524 AU)
   * @param {number} [spacecraftAreaM2=15.0] - Sun-facing illuminated cross-sectional area in m^2 (0.5 to 200 m^2)
   * @param {number} [spacecraftMassKg=1000.0] - Spacecraft total mass in kg (10 to 10000 kg)
   * @param {number} [radiationPressureCoeffCR=1.30] - Cannonball reflectivity coefficient C_R (1.0 = absorption, 2.0 = pure specular reflection)
   * @returns {{srpFluxPressurePa: number, totalSrpForceMicronewtons: number, srpAccelerationNmS2: number, srpAccelerationMS2: number, dailyDeltaVDriftMmSDay: number, annualDeltaVDriftMSYear: number, orbitalPerturbationRegime: string}}
   */
  static computeSolarRadiationPressurePerturbation(heliocentricDistanceAU = 1.524, spacecraftAreaM2 = 15.0, spacecraftMassKg = 1000.0, radiationPressureCoeffCR = 1.30) {
    const rAU = Math.max(0.1, heliocentricDistanceAU);
    const areaM2 = Math.max(0.01, spacecraftAreaM2);
    const massKg = Math.max(1.0, spacecraftMassKg);
    const Cr = Math.max(1.0, Math.min(2.0, radiationPressureCoeffCR));

    const S0 = 1361.0; // W/m^2 (solar constant at 1 AU)
    const c = 299792458.0; // speed of light in m/s
    const p1AU = S0 / c; // ~4.5398e-6 Pa at 1 AU

    // Local solar radiation pressure (Pa = N/m^2)
    const pSrpPa = p1AU / Math.pow(rAU, 2.0);

    // Total SRP force (N)
    const fSrpN = pSrpPa * areaM2 * Cr;
    const fSrpMicroN = fSrpN * 1e6; // micro-Newtons (uN)

    // Acceleration on spacecraft (m/s^2 and nm/s^2)
    const aSrpMS2 = fSrpN / massKg;
    const aSrpNmS2 = aSrpMS2 * 1e9; // nm/s^2

    // Velocity drift per day (mm/s/day) and per year (m/s/year)
    const SECS_PER_DAY = 86400.0;
    const deltaVDayMmS = aSrpMS2 * SECS_PER_DAY * 1000.0;
    const deltaVYearMS = aSrpMS2 * SECS_PER_DAY * 365.25;

    let regime = 'Moderate Perturbation (Standard Reaction Wheel Momentum Dumping & Ephemeris Propagation)';
    if (aSrpNmS2 > 100.0) {
      regime = 'Dominant Non-Gravitational Force (Solar Sail / High Area-to-Mass Orbital Precession)';
    } else if (aSrpNmS2 < 5.0) {
      regime = 'Negligible Outer Solar System SRP Perturbation (Outer Planet Cruise)';
    }

    return {
      srpFluxPressurePa: parseFloat(pSrpPa.toExponential(4)),
      totalSrpForceMicronewtons: parseFloat(fSrpMicroN.toFixed(2)),
      srpAccelerationNmS2: parseFloat(aSrpNmS2.toFixed(2)),
      srpAccelerationMS2: parseFloat(aSrpMS2.toExponential(4)),
      dailyDeltaVDriftMmSDay: parseFloat(deltaVDayMmS.toFixed(3)),
      annualDeltaVDriftMSYear: parseFloat(deltaVYearMS.toFixed(3)),
      orbitalPerturbationRegime: regime
    };
  }

  /**
   * Calculate hyperbolic planetary flyby B-plane targeting coordinates (B_R, B_T), deflection angle, impact parameter, and gravity assist Delta-V.
   * a = -mu / v_inf^2
   * e = 1 - r_p / a
   * delta = 2 * asin( 1 / e )
   * b = |a| * sqrt( e^2 - 1 )
   * Reference: Kizner (1961), Battin (1999), Vallado (2013) for Rosetta, Dawn, and Europa Clipper Mars gravity assist flybys.
   * @param {number} [vInfinityInMS=5500.0] - Hyperbolic excess arrival velocity v_inf in m/s (1000 to 25000 m/s)
   * @param {number} [periapsisAltitudeKm=250.0] - Closest approach altitude above surface in km (50 to 50000 km)
   * @param {number} [bThetaDeg=45.0] - B-plane clock angle theta_B in degrees (0 to 360 deg)
   * @param {string} [body='mars'] - Target planetary body
   * @returns {{hyperbolicExcessVelocityKmS: number, periapsisRadiusKm: number, hyperbolicEccentricity: number, deflectionAngleDeg: number, impactParameterMagnitudeKm: number, bPlaneRCoordinateKm: number, bPlaneTCoordinateKm: number, gravityAssistDeltaVKmS: number, flybyRegime: string}}
   */
  static computeBPlaneTargetingCoordinatesAndHyperbolicDeflection(vInfinityInMS = 5500.0, periapsisAltitudeKm = 250.0, bThetaDeg = 45.0, body = 'mars') {
    const vInfMS = Math.max(100.0, vInfinityInMS);
    const hpKm = Math.max(10.0, periapsisAltitudeKm);
    const thetaBRad = (bThetaDeg || 0.0) * (Math.PI / 180.0);

    let mu = 4.282837e13; // m^3/s^2 (Mars)
    let rPlanetKm = 3389.5;

    if (body.toLowerCase() === 'earth') {
      mu = 3.986004418e14;
      rPlanetKm = 6378.137;
    } else if (body.toLowerCase() === 'moon') {
      mu = 4.9048695e12;
      rPlanetKm = 1737.4;
    }

    const rpKm = rPlanetKm + hpKm;
    const rpM = rpKm * 1000.0;

    // Semi-major axis a = -mu / v_inf^2 (m)
    const aM = -mu / Math.pow(vInfMS, 2.0);
    const aKm = aM / 1000.0;

    // Hyperbolic eccentricity e = 1 - rp / a
    const e = 1.0 - (rpM / aM);

    // Hyperbolic deflection angle delta = 2 * asin(1 / e) (radians and degrees)
    const deltaRad = 2.0 * Math.asin(1.0 / Math.max(1.0001, e));
    const deltaDeg = deltaRad * (180.0 / Math.PI);

    // Impact parameter magnitude b = |a| * sqrt(e^2 - 1) (km)
    const bKm = Math.abs(aKm) * Math.sqrt(Math.max(0.0, Math.pow(e, 2.0) - 1.0));

    // B-plane components: B_R = b * sin(theta_B), B_T = b * cos(theta_B)
    const BrKm = bKm * Math.sin(thetaBRad);
    const BtKm = bKm * Math.cos(thetaBRad);

    // Gravity assist Delta-V = 2 * v_inf * sin(delta / 2)
    const deltaVFlybyMS = 2.0 * vInfMS * Math.sin(deltaRad / 2.0);
    const deltaVFlybyKmS = deltaVFlybyMS / 1000.0;

    let regime = 'Hyperbolic Gravity Assist Flyby (Deep Space Trajectory Bending)';
    if (hpKm < 150.0 && body.toLowerCase() === 'mars') {
      regime = 'Atmospheric Grazing Aerocapture Corridor Hazard (< 150 km Pericenter)';
    } else if (deltaDeg > 60.0) {
      regime = 'High-Deflection Close Encounter (Strong Gravitational Redirection)';
    }

    return {
      hyperbolicExcessVelocityKmS: parseFloat((vInfMS / 1000.0).toFixed(3)),
      periapsisRadiusKm: parseFloat(rpKm.toFixed(2)),
      hyperbolicEccentricity: parseFloat(e.toFixed(4)),
      deflectionAngleDeg: parseFloat(deltaDeg.toFixed(2)),
      impactParameterMagnitudeKm: parseFloat(bKm.toFixed(2)),
      bPlaneRCoordinateKm: parseFloat(BrKm.toFixed(2)),
      bPlaneTCoordinateKm: parseFloat(BtKm.toFixed(2)),
      gravityAssistDeltaVKmS: parseFloat(deltaVFlybyKmS.toFixed(3)),
      flybyRegime: regime
    };
  }

  /**
   * Calculate heliocentric Hohmann transfer orbit parameters, time-of-flight, hyperbolic excess velocity, and Trans-Mars Injection (TMI) Delta-V.
   * a_t = ( r_1 + r_2 ) / 2
   * TOF = pi * sqrt( a_t^3 / mu_sun )
   * v_inf = | v_t1 - v_p1 |
   * Delta_V_TMI = sqrt( v_inf^2 + 2*mu_1 / r_park ) - sqrt( mu_1 / r_park )
   * Reference: Bate, Mueller & White (1971), Curtis (2013), Vallado (2013) for interplanetary patched-conics mission design.
   * @param {string} [departurePlanet='earth'] - Departure planetary body
   * @param {string} [arrivalPlanet='mars'] - Arrival destination body
   * @param {number} [parkingAltitudeKm=200.0] - Circular parking orbit altitude at departure in km (150 to 1000 km)
   * @returns {{transferSemiMajorAxisAU: number, timeOfFlightDays: number, timeOfFlightMonths: number, hyperbolicDepartureExcessKmS: number, characteristicLaunchEnergyC3Km2S2: number, transInjectionDeltaVKmS: number, hyperbolicArrivalExcessKmS: number, transferGeometryDescription: string}}
   */
  static computeTransMarsInjectionDeltaVAndHohmannTrajectory(departurePlanet = 'earth', arrivalPlanet = 'mars', parkingAltitudeKm = 200.0) {
    const muSun = 1.32712440018e20; // m^3/s^2
    const AU_METERS = 1.495978707e11; // meters

    // Planetary heliocentric orbital radii (meters)
    let r1 = 1.0 * AU_METERS; // Earth
    let r2 = 1.523662 * AU_METERS; // Mars
    let muDep = 3.986004418e14; // Earth mu
    let rPlanetDepKm = 6378.137;

    if (departurePlanet.toLowerCase() === 'mars') {
      r1 = 1.523662 * AU_METERS;
      muDep = 4.282837e13;
      rPlanetDepKm = 3389.5;
    }
    if (arrivalPlanet.toLowerCase() === 'earth') {
      r2 = 1.0 * AU_METERS;
    } else if (arrivalPlanet.toLowerCase() === 'jupiter') {
      r2 = 5.2044 * AU_METERS;
    } else if (arrivalPlanet.toLowerCase() === 'venus') {
      r2 = 0.723332 * AU_METERS;
    }

    // Transfer semi-major axis (meters and AU)
    const atM = (r1 + r2) / 2.0;
    const atAU = atM / AU_METERS;

    // Time of flight TOF = pi * sqrt( at^3 / muSun ) (seconds, days, months)
    const tofSec = Math.PI * Math.sqrt(Math.pow(atM, 3.0) / muSun);
    const tofDays = tofSec / 86400.0;
    const tofMonths = tofDays / 30.4375;

    // Heliocentric speeds
    const vt1 = Math.sqrt(muSun * ((2.0 / r1) - (1.0 / atM)));
    const vp1 = Math.sqrt(muSun / r1);
    const vInf1MS = Math.abs(vt1 - vp1);
    const vInf1KmS = vInf1MS / 1000.0;

    // Characteristic launch energy C3 = v_inf^2 (km^2/s^2)
    const c3Km2S2 = Math.pow(vInf1KmS, 2.0);

    // Parking orbit and TMI Delta-V
    const rParkM = (rPlanetDepKm + Math.max(50.0, parkingAltitudeKm)) * 1000.0;
    const vPark = Math.sqrt(muDep / rParkM);
    const vInj = Math.sqrt(Math.pow(vInf1MS, 2.0) + (2.0 * muDep / rParkM));
    const deltaVInjKmS = (vInj - vPark) / 1000.0;

    // Arrival excess speed
    const vt2 = Math.sqrt(muSun * ((2.0 / r2) - (1.0 / atM)));
    const vp2 = Math.sqrt(muSun / r2);
    const vInf2KmS = Math.abs(vt2 - vp2) / 1000.0;

    const desc = `${departurePlanet.toUpperCase()} to ${arrivalPlanet.toUpperCase()} Heliocentric Hohmann Transfer (~${tofDays.toFixed(1)} Days / ${tofMonths.toFixed(1)} Months)`;

    return {
      transferSemiMajorAxisAU: parseFloat(atAU.toFixed(4)),
      timeOfFlightDays: parseFloat(tofDays.toFixed(1)),
      timeOfFlightMonths: parseFloat(tofMonths.toFixed(1)),
      hyperbolicDepartureExcessKmS: parseFloat(vInf1KmS.toFixed(3)),
      characteristicLaunchEnergyC3Km2S2: parseFloat(c3Km2S2.toFixed(2)),
      transInjectionDeltaVKmS: parseFloat(deltaVInjKmS.toFixed(3)),
      hyperbolicArrivalExcessKmS: parseFloat(vInf2KmS.toFixed(3)),
      transferGeometryDescription: desc
    };
  }

  /**
   * Calculate Mars Orbit Insertion (MOI) hyperbolic capture impulse Delta-V, captured ellipse orbital period, and insertion pericenter speed.
   * v_p_hyp = sqrt( v_inf^2 + 2*mu / r_p )
   * v_p_cap = sqrt( mu * ( 2/r_p - 1/a_cap ) )
   * Delta_V_MOI = v_p_hyp - v_p_cap
   * Reference: Bate, Mueller & White (1971), Vallado (2013) for MGS, Odyssey, MRO, MAVEN, and Hope MOI burns.
   * @param {number} [hyperbolicArrivalExcessKmS=2.65] - Arrival asymptotic hyperbolic excess speed in km/s (1.0 to 10.0 km/s)
   * @param {number} [insertionPeriapsisAltitudeKm=300.0] - Insertion periapsis altitude in km (150 to 2000 km)
   * @param {number} [targetApoapsisAltitudeKm=43000.0] - Captured orbit apoapsis altitude in km (1000 to 100000 km)
   * @param {string} [body='mars'] - Target planetary body
   * @returns {{hyperbolicPeriapsisSpeedKmS: number, capturedPeriapsisSpeedKmS: number, orbitInsertionDeltaVKmS: number, capturedOrbitPeriodHours: number, capturedOrbitEccentricity: number, insertionBurnRegime: string}}
   */
  static computeMarsOrbitInsertionCaptureDeltaV(hyperbolicArrivalExcessKmS = 2.65, insertionPeriapsisAltitudeKm = 300.0, targetApoapsisAltitudeKm = 43000.0, body = 'mars') {
    const vInfMS = Math.max(100.0, hyperbolicArrivalExcessKmS * 1000.0);
    const hpKm = Math.max(50.0, insertionPeriapsisAltitudeKm);
    const haKm = Math.max(hpKm + 50.0, targetApoapsisAltitudeKm);

    let mu = 4.282837e13; // m^3/s^2 (Mars)
    let rPlanetKm = 3389.5;

    if (body.toLowerCase() === 'earth') {
      mu = 3.986004418e14;
      rPlanetKm = 6378.137;
    } else if (body.toLowerCase() === 'moon') {
      mu = 4.9048695e12;
      rPlanetKm = 1737.4;
    }

    const rpM = (rPlanetKm + hpKm) * 1000.0;
    const raM = (rPlanetKm + haKm) * 1000.0;

    // Hyperbolic periapsis speed
    const vpHypMS = Math.sqrt(Math.pow(vInfMS, 2.0) + (2.0 * mu / rpM));
    const vpHypKmS = vpHypMS / 1000.0;

    // Captured elliptical orbit semi-major axis and eccentricity
    const aCapM = (rpM + raM) / 2.0;
    const eCap = (raM - rpM) / (raM + rpM);

    // Captured periapsis speed
    const vpCapMS = Math.sqrt(mu * ((2.0 / rpM) - (1.0 / aCapM)));
    const vpCapKmS = vpCapMS / 1000.0;

    // Insertion Delta-V burn
    const deltaVMoiMS = vpHypMS - vpCapMS;
    const deltaVMoiKmS = deltaVMoiMS / 1000.0;

    // Orbital period in hours
    const periodSec = 2.0 * Math.PI * Math.sqrt(Math.pow(aCapM, 3.0) / mu);
    const periodHours = periodSec / 3600.0;

    let regime = 'Highly Elliptical Capture Orbit (Standard 24-48 hr Aerobraking Preparation Orbit)';
    if (periodHours < 8.0) {
      regime = 'Direct Low-Orbit Insertion (Large Propellant Mass Expenditure)';
    } else if (periodHours > 72.0) {
      regime = 'Loosely Bound Ultra-High Apoapsis Capture Orbit';
    }

    return {
      hyperbolicPeriapsisSpeedKmS: parseFloat(vpHypKmS.toFixed(3)),
      capturedPeriapsisSpeedKmS: parseFloat(vpCapKmS.toFixed(3)),
      orbitInsertionDeltaVKmS: parseFloat(deltaVMoiKmS.toFixed(3)),
      capturedOrbitPeriodHours: parseFloat(periodHours.toFixed(2)),
      capturedOrbitEccentricity: parseFloat(eCap.toFixed(4)),
      insertionBurnRegime: regime
    };
  }

  /**
   * Calculate orbital plane change impulse Delta-V, combined speed-inclination vector maneuver, propellant savings, and optimal thrust angle.
   * Delta_V_plane = 2 * v1 * sin( delta_i / 2 )
   * Delta_V_comb = sqrt( v1^2 + v2^2 - 2*v1*v2*cos( delta_i ) )
   * Reference: Curtis (2013), Vallado (2013), Chobotov (2002) for orbital mechanics and cross-track inclination trim burns.
   * @param {number} [vInitialKmS=3.50] - Initial orbital speed in km/s (0.5 to 12.0 km/s)
   * @param {number} [inclinationChangeDeg=30.0] - Desired inclination/plane change angle in degrees (0 to 180 deg)
   * @param {number} [vFinalKmS=4.20] - Final target orbital speed in km/s (0.5 to 12.0 km/s)
   * @returns {{purePlaneChangeDeltaVKmS: number, combinedManeuverDeltaVKmS: number, separateManeuverDeltaVKmS: number, deltaVSavingsKmS: number, propellantSavingsPercent: number, optimalThrustAngleDeg: number, maneuverEfficiencyContext: string}}
   */
  static computeOrbitalPlaneChangeDeltaVAndCombinedManeuver(vInitialKmS = 3.50, inclinationChangeDeg = 30.0, vFinalKmS = 4.20) {
    const v1 = Math.max(0.1, vInitialKmS);
    const v2 = Math.max(0.1, vFinalKmS);
    const deltaIRad = Math.abs(inclinationChangeDeg) * (Math.PI / 180.0);

    // Pure plane change at initial speed
    const dvPlane = 2.0 * v1 * Math.sin(deltaIRad / 2.0);

    // Combined vector maneuver (law of cosines)
    const dvComb = Math.sqrt(Math.max(0.0, Math.pow(v1, 2.0) + Math.pow(v2, 2.0) - 2.0 * v1 * v2 * Math.cos(deltaIRad)));

    // Separate maneuvers (plane change then speed adjustment)
    const dvSep = dvPlane + Math.abs(v2 - v1);

    // Savings
    const dvSavings = Math.max(0.0, dvSep - dvComb);
    const savingsPct = dvSep > 0 ? (dvSavings / dvSep) * 100.0 : 0.0;

    // Optimal thrust angle alpha relative to initial velocity vector
    const num = v2 * Math.sin(deltaIRad);
    const den = v2 * Math.cos(deltaIRad) - v1;
    let alphaRad = Math.atan2(num, den);
    if (alphaRad < 0) alphaRad += 2.0 * Math.PI;
    const alphaDeg = alphaRad * (180.0 / Math.PI);

    let context = 'Standard Inclination & Speed Vector Trim Maneuver';
    if (deltaIRad > Math.PI / 3.0) {
      context = 'High-Deflection Plane Change (Extremely High Delta-V Cost - Best Performed at Apoapsis)';
    } else if (savingsPct > 15.0) {
      context = 'High-Efficiency Combined Burn (Substantial Propellant Mass Savings vs Separate Burns)';
    }

    return {
      purePlaneChangeDeltaVKmS: parseFloat(dvPlane.toFixed(3)),
      combinedManeuverDeltaVKmS: parseFloat(dvComb.toFixed(3)),
      separateManeuverDeltaVKmS: parseFloat(dvSep.toFixed(3)),
      deltaVSavingsKmS: parseFloat(dvSavings.toFixed(3)),
      propellantSavingsPercent: parseFloat(savingsPct.toFixed(1)),
      optimalThrustAngleDeg: parseFloat(alphaDeg.toFixed(2)),
      maneuverEfficiencyContext: context
    };
  }

  /**
   * Calculate 2D/3D planetocentric-to-heliocentric gravity assist vector addition, turn angle, heliocentric velocity gain, and specific energy change.
   * delta = 2 * asin( 1 / ( 1 + r_p * v_inf^2 / mu ) )
   * v_hel_in = v_planet + v_inf_in
   * v_hel_out = v_planet + v_inf_out
   * Reference: Bate, Mueller & White (1971), Prussing & Conway (1993), Curtis (2013) for Rosetta/Dawn planetary gravity assists.
   * @param {number} [vInfInKmS=5.60] - Hyperbolic approach speed in km/s (1.0 to 20.0 km/s)
   * @param {number} [approachAngleDeg=120.0] - Angle between approach asymptote and planet velocity vector in degrees
   * @param {number} [vPlanetHeliocentricKmS=24.13] - Planet circular heliocentric orbital velocity in km/s (24.13 km/s for Mars)
   * @param {number} [periapsisAltitudeKm=250.0] - Flyby periapsis altitude in km (150 to 10000 km)
   * @param {string} [body='mars'] - Planetary encounter body
   * @returns {{hyperbolicDeflectionAngleDeg: number, ingoingHeliocentricSpeedKmS: number, outgoingHeliocentricSpeedKmS: number, netHeliocentricSpeedChangeKmS: number, flybyVectorImpulseMagnitudeKmS: number, specificOrbitalEnergyChangeKm2S2: number, gravityAssistRegime: string}}
   */
  static computeInterplanetaryGravityAssistHeliocentricVelocityVector(vInfInKmS = 5.60, approachAngleDeg = 120.0, vPlanetHeliocentricKmS = 24.13, periapsisAltitudeKm = 250.0, body = 'mars') {
    const vInf = Math.max(0.1, vInfInKmS);
    const vPlanet = Math.max(0.1, vPlanetHeliocentricKmS);
    const hpKm = Math.max(50.0, periapsisAltitudeKm);
    const psiInRad = approachAngleDeg * (Math.PI / 180.0);

    let mu = 4.282837e13; // m^3/s^2 (Mars)
    let rPlanetKm = 3389.5;

    if (body.toLowerCase() === 'earth') {
      mu = 3.986004418e14;
      rPlanetKm = 6378.137;
    } else if (body.toLowerCase() === 'venus') {
      mu = 3.24859e14;
      rPlanetKm = 6051.8;
    } else if (body.toLowerCase() === 'jupiter') {
      mu = 1.26686534e17;
      rPlanetKm = 71492.0;
    }

    const rpM = (rPlanetKm + hpKm) * 1000.0;
    const vInfMS = vInf * 1000.0;

    // Hyperbolic eccentricity e = 1 + (rp * v_inf^2) / mu
    const e = 1.0 + (rpM * Math.pow(vInfMS, 2.0)) / mu;

    // Turn angle delta = 2 * asin(1 / e)
    const deltaRad = 2.0 * Math.asin(1.0 / Math.max(1.0001, e));
    const deltaDeg = deltaRad * (180.0 / Math.PI);

    // Ingoing asymptotes (planet motion is along +y axis, sun is along -x)
    const vInfInX = vInf * Math.cos(psiInRad);
    const vInfInY = vInf * Math.sin(psiInRad);

    const vHelInX = vInfInX;
    const vHelInY = vPlanet + vInfInY;
    const vHelIn = Math.sqrt(Math.pow(vHelInX, 2.0) + Math.pow(vHelInY, 2.0));

    // Outgoing asymptote rotated trailing-side by delta
    const psiOutRad = psiInRad - deltaRad;
    const vInfOutX = vInf * Math.cos(psiOutRad);
    const vInfOutY = vInf * Math.sin(psiOutRad);

    const vHelOutX = vInfOutX;
    const vHelOutY = vPlanet + vInfOutY;
    const vHelOut = Math.sqrt(Math.pow(vHelOutX, 2.0) + Math.pow(vHelOutY, 2.0));

    const deltaVGain = vHelOut - vHelIn;

    // Vector impulse magnitude = 2 * v_inf * sin(delta / 2)
    const impulse = 2.0 * vInf * Math.sin(deltaRad / 2.0);

    // Specific orbital energy change = ( v_out^2 - v_in^2 ) / 2
    const deltaEnergy = (Math.pow(vHelOut, 2.0) - Math.pow(vHelIn, 2.0)) / 2.0;

    let regime = deltaVGain >= 0 ? 'Trailing-Side Flyby (Heliocentric Energy & Aphelion Boosting)' : 'Leading-Side Flyby (Heliocentric Braking / Inward Solar System Insertion)';

    return {
      hyperbolicDeflectionAngleDeg: parseFloat(deltaDeg.toFixed(2)),
      ingoingHeliocentricSpeedKmS: parseFloat(vHelIn.toFixed(3)),
      outgoingHeliocentricSpeedKmS: parseFloat(vHelOut.toFixed(3)),
      netHeliocentricSpeedChangeKmS: parseFloat(deltaVGain.toFixed(3)),
      flybyVectorImpulseMagnitudeKmS: parseFloat(impulse.toFixed(3)),
      specificOrbitalEnergyChangeKm2S2: parseFloat(deltaEnergy.toFixed(2)),
      gravityAssistRegime: regime
    };
  }

  /**
   * Calculate continuous low-thrust Edelbaum spiral orbital transfer duration, Delta-V, propellant mass, and number of orbital revolutions.
   * Delta_V_spiral = | v1 - v2 |
   * m_p = m0 * ( 1 - exp( -Delta_V / ( Isp * g0 ) ) )
   * t_spiral = ( m0 * Isp * g0 / T ) * ( 1 - exp( -Delta_V / ( Isp * g0 ) ) )
   * Reference: Edelbaum (1961), Wiesel (1997), Curtis (2013) for solar electric ion propulsion (SEP) spiral trajectories.
   * @param {number} [initialAltitudeKm=400.0] - Initial circular orbit altitude in km
   * @param {number} [finalAltitudeKm=17038.5] - Final circular orbit altitude in km (17038.5 km = Areostationary orbit)
   * @param {number} [thrustNewtons=0.50] - Continuous thruster output in Newtons (0.01 to 10.0 N)
   * @param {number} [specificImpulseSec=3200.0] - Specific impulse in seconds (1000 to 10000 s for Hall/Ion thrusters)
   * @param {number} [spacecraftMassKg=1000.0] - Spacecraft initial wet mass in kg (100 to 50000 kg)
   * @param {string} [body='mars'] - Planetary body
   * @returns {{initialOrbitalSpeedKmS: number, finalOrbitalSpeedKmS: number, continuousSpiralDeltaVKmS: number, propellantConsumedKg: number, transferDurationDays: number, totalSpiralRevolutions: number, lowThrustPropulsionContext: string}}
   */
  static computeContinuousLowThrustSpiralOrbitRaising(initialAltitudeKm = 400.0, finalAltitudeKm = 17038.5, thrustNewtons = 0.50, specificImpulseSec = 3200.0, spacecraftMassKg = 1000.0, body = 'mars') {
    const h1Km = Math.max(50.0, initialAltitudeKm);
    const h2Km = Math.max(50.0, finalAltitudeKm);
    const thrustN = Math.max(1e-4, thrustNewtons);
    const isp = Math.max(100.0, specificImpulseSec);
    const m0 = Math.max(1.0, spacecraftMassKg);

    let mu = 4.282837e13; // m^3/s^2 (Mars)
    let rPlanetKm = 3389.5;

    if (body.toLowerCase() === 'earth') {
      mu = 3.986004418e14;
      rPlanetKm = 6378.137;
    } else if (body.toLowerCase() === 'moon') {
      mu = 4.9048695e12;
      rPlanetKm = 1737.4;
    }

    const r1M = (rPlanetKm + h1Km) * 1000.0;
    const r2M = (rPlanetKm + h2Km) * 1000.0;

    const v1MS = Math.sqrt(mu / r1M);
    const v2MS = Math.sqrt(mu / r2M);
    const v1KmS = v1MS / 1000.0;
    const v2KmS = v2MS / 1000.0;

    // Edelbaum velocity increment
    const dvSpiralMS = Math.abs(v1MS - v2MS);
    const dvSpiralKmS = dvSpiralMS / 1000.0;

    // Effective exhaust velocity c = Isp * g0 (m/s)
    const g0 = 9.80665;
    const cMS = isp * g0;

    // Propellant mass consumption
    const massRatio = Math.exp(-dvSpiralMS / cMS);
    const mpKg = m0 * (1.0 - massRatio);

    // Duration in seconds and days
    const mDotKgS = thrustN / cMS;
    const tSpiralSec = mpKg / mDotKgS;
    const tSpiralDays = tSpiralSec / 86400.0;

    // Average orbital period to estimate spiral revolutions
    const aAvgM = (r1M + r2M) / 2.0;
    const pAvgSec = 2.0 * Math.PI * Math.sqrt(Math.pow(aAvgM, 3.0) / mu);
    const numRevs = tSpiralSec / pAvgSec;

    let context = 'Continuous Solar Electric Ion Propulsion (SEP) Low-Thrust Orbital Spiral';
    if (tSpiralDays > 100.0) {
      context = 'Ultra-Long Multi-Month Continuous Low-Thrust Raising Campaign';
    } else if (h2Km < h1Km) {
      context = 'Continuous Low-Thrust Orbital Lowering / De-orbiting Spiral';
    }

    return {
      initialOrbitalSpeedKmS: parseFloat(v1KmS.toFixed(3)),
      finalOrbitalSpeedKmS: parseFloat(v2KmS.toFixed(3)),
      continuousSpiralDeltaVKmS: parseFloat(dvSpiralKmS.toFixed(3)),
      propellantConsumedKg: parseFloat(mpKg.toFixed(2)),
      transferDurationDays: parseFloat(tSpiralDays.toFixed(1)),
      totalSpiralRevolutions: parseFloat(numRevs.toFixed(0)),
      lowThrustPropulsionContext: context
    };
  }

  /**
   * Calculate planetary frozen orbit parameters, J2/J3 zonal harmonic equilibrium, frozen eccentricity, and critical inclinations.
   * e_frozen = - ( J3 * Rp ) / ( 2 * J2 * a ) * sin( i )
   * i_crit = 63.435 deg / 116.565 deg ( stationary apsides )
   * Reference: Cutting et al. (1978), Coffey et al. (1994), Vallado (2013) for Mars Global Surveyor / 2001 Mars Odyssey frozen orbits.
   * @param {number} [meanAltitudeKm=400.0] - Mean orbit altitude in km
   * @param {number} [orbitInclinationDeg=93.0] - Orbit inclination in degrees (93 deg for Mars Sun-synchronous)
   * @param {string} [body='mars'] - Planetary body
   * @returns {{semiMajorAxisKm: number, frozenEccentricity: number, frozenArgumentOfPeriapsisDeg: number, periapsisAltitudeKm: number, apoapsisAltitudeKm: number, altitudeVariationRangeKm: number, criticalInclinationDeg: number, frozenOrbitStabilityContext: string}}
   */
  static computeFrozenOrbitEquilibriumAndAltitudeOscillation(meanAltitudeKm = 400.0, orbitInclinationDeg = 93.0, body = 'mars') {
    const hMeanKm = Math.max(50.0, meanAltitudeKm);
    const incDeg = Math.max(0.0, Math.min(180.0, orbitInclinationDeg));
    const incRad = incDeg * (Math.PI / 180.0);

    let RpKm = 3389.5;
    let J2 = 1.96045e-3;
    let J3 = -3.15e-5;

    if (body.toLowerCase() === 'earth') {
      RpKm = 6378.137;
      J2 = 1.08263e-3;
      J3 = -2.532e-6;
    } else if (body.toLowerCase() === 'moon') {
      RpKm = 1737.4;
      J2 = 2.027e-4;
      J3 = 6.0e-6;
    }

    const aKm = RpKm + hMeanKm;

    // Frozen eccentricity balancing J2 and J3 drift: e_frozen = - ( J3 * Rp ) / ( 2 * J2 * a ) * sin( i )
    const eFrozen = - (J3 * RpKm) / (2.0 * J2 * aKm) * Math.sin(incRad);
    const eMag = Math.max(0.0, Math.abs(eFrozen));

    // Argument of periapsis for frozen condition (270 deg for negative J3/Mars, 90 deg for positive J3)
    const omegaFrozenDeg = J3 < 0 ? 270.0 : 90.0;

    // Pericenter and apocenter altitudes
    const hpKm = aKm * (1.0 - eMag) - RpKm;
    const haKm = aKm * (1.0 + eMag) - RpKm;
    const deltaHKm = haKm - hpKm;

    const critIncDeg = 63.435;

    let desc = 'Sun-Synchronous Mapping Frozen Orbit (Minimal Stationkeeping Propellant Budget)';
    if (Math.abs(incDeg - 63.435) < 2.0 || Math.abs(incDeg - 116.565) < 2.0) {
      desc = 'Critical Inclination Frozen Orbit (Zero Secular Apsidal Drift)';
    } else if (eMag < 0.001) {
      desc = 'Near-Circular Frozen Orbit Configuration';
    }

    return {
      semiMajorAxisKm: parseFloat(aKm.toFixed(2)),
      frozenEccentricity: parseFloat(eMag.toFixed(6)),
      frozenArgumentOfPeriapsisDeg: omegaFrozenDeg,
      periapsisAltitudeKm: parseFloat(hpKm.toFixed(2)),
      apoapsisAltitudeKm: parseFloat(haKm.toFixed(2)),
      altitudeVariationRangeKm: parseFloat(deltaHKm.toFixed(2)),
      criticalInclinationDeg: critIncDeg,
      frozenOrbitStabilityContext: desc
    };
  }

  /**
   * Calculate Mars aerocapture atmospheric entry kinematics, aerodynamic Delta-V dissipation, and apoapsis periapsis raise burn.
   * v_entry = sqrt( v_inf^2 + 2*mu / r_entry )
   * Delta_V_aero = v_entry - v_exit
   * Delta_V_raise = v_a_post - v_a_pre
   * Reference: Cruz (1999), Braun & Manning (2007), Lu (2014) for Mars Sample Return / Human Mars aerocapture architectures.
   * @param {number} [vInfApproachKmS=5.70] - Interplanetary hyperbolic approach speed in km/s (3.0 to 10.0 km/s)
   * @param {number} [targetApoapsisAltitudeKm=6000.0] - Target captured orbit apoapsis altitude in km (500 to 50000 km)
   * @param {number} [atmosphericPericenterAltitudeKm=45.0] - Atmospheric entry corridor periapsis in km (35 to 65 km)
   * @param {number} [entryInterfaceAltitudeKm=125.0] - Atmospheric interface altitude in km (125 km for Mars)
   * @param {string} [body='mars'] - Planetary body
   * @returns {{atmosphericEntrySpeedKmS: number, atmosphericExitSpeedKmS: number, aerodynamicDeltaVDissipatedKmS: number, apoapsisPeriapsisRaiseDeltaVMPS: number, propulsiveMassSavingsPercent: number, aerocaptureRegime: string}}
   */
  static computeMarsAerocaptureAtmosphericEntryAndOrbitInsertion(vInfApproachKmS = 5.70, targetApoapsisAltitudeKm = 6000.0, atmosphericPericenterAltitudeKm = 45.0, entryInterfaceAltitudeKm = 125.0, body = 'mars') {
    const vInf = Math.max(0.5, vInfApproachKmS);
    const haTargetKm = Math.max(100.0, targetApoapsisAltitudeKm);
    const hpAtmKm = Math.max(20.0, atmosphericPericenterAltitudeKm);
    const hEntryKm = Math.max(50.0, entryInterfaceAltitudeKm);

    let RpKm = 3389.5;
    let mu = 42828.37; // km^3/s^2 (Mars)

    if (body.toLowerCase() === 'earth') {
      RpKm = 6378.137;
      mu = 398600.4418;
    } else if (body.toLowerCase() === 'venus') {
      RpKm = 6051.8;
      mu = 324859.0;
    }

    const rEntryKm = RpKm + hEntryKm;
    const rAtmPeriKm = RpKm + hpAtmKm;
    const rTargetApoKm = RpKm + haTargetKm;

    // Atmospheric entry speed v_entry = sqrt( v_inf^2 + 2*mu / r_entry )
    const vEntryKmS = Math.sqrt(Math.pow(vInf, 2.0) + (2.0 * mu) / rEntryKm);

    // Target captured orbit semi-major axis
    const aTargetKm = (rTargetApoKm + rAtmPeriKm) / 2.0;

    // Atmospheric exit speed v_exit = sqrt( 2*mu * ( 1/r_entry - 1/(2*a_target) ) )
    const vExitKmS = Math.sqrt(2.0 * mu * Math.max(1e-6, (1.0 / rEntryKm) - (1.0 / (2.0 * aTargetKm))));

    // Aerodynamic velocity increment absorbed by atmospheric drag
    const deltaVAeroKmS = vEntryKmS - vExitKmS;

    // Raise periapsis to safe orbit (e.g. 250 km) at apoapsis
    const rSafePeriKm = RpKm + 250.0;
    const vaPre = Math.sqrt((2.0 * mu * rAtmPeriKm) / (rTargetApoKm * (rTargetApoKm + rAtmPeriKm)));
    const vaPost = Math.sqrt((2.0 * mu * rSafePeriKm) / (rTargetApoKm * (rTargetApoKm + rSafePeriKm)));
    const deltaVRaiseKmS = Math.max(0.0, vaPost - vaPre);
    const deltaVRaiseMPS = deltaVRaiseKmS * 1000.0;

    // Propulsive MOI burn without aerocapture would require ~ Delta_V_aero
    // Mass savings compared to pure propulsive MOI (Isp = 320 s)
    const massSavingsPct = (1.0 - Math.exp(-deltaVAeroKmS / (320.0 * 9.80665e-3))) * 100.0;

    let regime = 'High-Precision Mars Guided Aerocapture & Atmospheric Braking Insertion';
    if (deltaVAeroKmS > 4.0) {
      regime = 'High-Energy Deep Atmospheric Aerocapture Trajectory';
    }

    return {
      atmosphericEntrySpeedKmS: parseFloat(vEntryKmS.toFixed(3)),
      atmosphericExitSpeedKmS: parseFloat(vExitKmS.toFixed(3)),
      aerodynamicDeltaVDissipatedKmS: parseFloat(deltaVAeroKmS.toFixed(3)),
      apoapsisPeriapsisRaiseDeltaVMPS: parseFloat(deltaVRaiseMPS.toFixed(1)),
      propulsiveMassSavingsPercent: parseFloat(massSavingsPct.toFixed(1)),
      aerocaptureRegime: regime
    };
  }

  /**
   * Calculate Trans-Earth Injection (TEI) Delta-V from Mars parking orbit, interplanetary Hohmann return trajectory, and Earth atmospheric entry speed.
   * v_p = sqrt( v_inf_M^2 + 2*mu_M / r_park )
   * Delta_V_TEI = v_p - sqrt( mu_M / r_park )
   * v_entry_E = sqrt( v_inf_E^2 + 2*mu_E / ( R_E + 120 km ) )
   * Reference: Bate, Mueller & White (1971), Curtis (2013) for Mars Sample Return & Crewed Return trajectories.
   * @param {number} [parkingOrbitAltitudeKm=400.0] - Mars parking orbit altitude in km
   * @param {number} [earthAtmosphereInterfaceKm=120.0] - Earth atmospheric entry interface in km
   * @returns {{transferSemiMajorAxisAU: number, timeOfFlightDays: number, marsDepartureVInfKmS: number, marsDepartureC3Km2S2: number, transEarthInjectionDeltaVKmS: number, earthArrivalVInfKmS: number, earthAtmosphericReentrySpeedKmS: number, returnTrajectoryContext: string}}
   */
  static computeTransEarthInjectionDeltaVAndReturnTrajectory(parkingOrbitAltitudeKm = 400.0, earthAtmosphereInterfaceKm = 120.0) {
    const hpKm = Math.max(50.0, parkingOrbitAltitudeKm);
    const hEntryKm = Math.max(50.0, earthAtmosphereInterfaceKm);

    const AU_KM = 1.495978707e8;
    const muSun = 1.32712440018e11; // km^3/s^2
    const muMars = 42828.37; // km^3/s^2
    const muEarth = 398600.4418; // km^3/s^2

    const rMarsKm = 1.52368 * AU_KM;
    const rEarthKm = 1.00000 * AU_KM;
    const rMarsPlanetKm = 3389.5;
    const rEarthPlanetKm = 6378.137;

    // Mars and Earth circular heliocentric speeds
    const vMarsHel = Math.sqrt(muSun / rMarsKm);
    const vEarthHel = Math.sqrt(muSun / rEarthKm);

    // Hohmann transfer ellipse semi-major axis
    const atKm = (rMarsKm + rEarthKm) / 2.0;
    const atAU = atKm / AU_KM;

    // Time of Flight (seconds and days)
    const tofSec = Math.PI * Math.sqrt(Math.pow(atKm, 3.0) / muSun);
    const tofDays = tofSec / 86400.0;

    // Aphelion speed at Mars departure
    const vaHel = Math.sqrt(muSun * ((2.0 / rMarsKm) - (1.0 / atKm)));
    const vInfMars = Math.abs(vMarsHel - vaHel);
    const c3Mars = Math.pow(vInfMars, 2.0);

    // Mars TEI burn from circular parking orbit
    const rParkKm = rMarsPlanetKm + hpKm;
    const vParkCirc = Math.sqrt(muMars / rParkKm);
    const vParkHyp = Math.sqrt(Math.pow(vInfMars, 2.0) + (2.0 * muMars) / rParkKm);
    const deltaVTeiKmS = vParkHyp - vParkCirc;

    // Perihelion speed at Earth arrival
    const vpHel = Math.sqrt(muSun * ((2.0 / rEarthKm) - (1.0 / atKm)));
    const vInfEarth = Math.abs(vpHel - vEarthHel);

    // Earth atmospheric entry speed at 120 km interface
    const rEntryEarthKm = rEarthPlanetKm + hEntryKm;
    const vEntryEarthKmS = Math.sqrt(Math.pow(vInfEarth, 2.0) + (2.0 * muEarth) / rEntryEarthKm);

    return {
      transferSemiMajorAxisAU: parseFloat(atAU.toFixed(5)),
      timeOfFlightDays: parseFloat(tofDays.toFixed(1)),
      marsDepartureVInfKmS: parseFloat(vInfMars.toFixed(3)),
      marsDepartureC3Km2S2: parseFloat(c3Mars.toFixed(2)),
      transEarthInjectionDeltaVKmS: parseFloat(deltaVTeiKmS.toFixed(3)),
      earthArrivalVInfKmS: parseFloat(vInfEarth.toFixed(3)),
      earthAtmosphericReentrySpeedKmS: parseFloat(vEntryEarthKmS.toFixed(2)),
      returnTrajectoryContext: 'Optimal Coplanar Mars-to-Earth Hohmann Direct Return'
    };
  }

  /**
   * Calculate Low Mars Orbit (LMO) atmospheric drag orbital decay rate, daily altitude loss, and remaining satellite lifetime.
   * rho(h) = rho0 * exp( - ( h - h0 ) / H_scale )
   * Delta_a_orbit = - 2 * pi * ( rho(h) * a^2 ) / B
   * t_life = ( H_scale * 1000 ) / | Delta_h_day |
   * Reference: King-Hele (1987), Vallado (2013) for Mars thermospheric satellite drag and orbital lifetime.
   * @param {number} [initialAltitudeKm=200.0] - Circular orbit altitude in km (120 to 500 km)
   * @param {number} [ballisticCoefficientKgM2=50.0] - Spacecraft ballistic coefficient m / (Cd * A) in kg/m^2 (5 to 500 kg/m^2)
   * @param {string} [solarActivityLevel='moderate'] - Mars atmospheric thermosphere solar state ('low', 'moderate', 'high_dust_storm')
   * @param {string} [body='mars'] - Planetary body
   * @returns {{orbitAltitudeKm: number, atmosphericDensityKgM3: number, orbitalPeriodMinutes: number, dailyAltitudeLossMeters: number, estimatedOrbitalLifetimeDays: number, orbitalDecayRegime: string}}
   */
  static computeLowMarsOrbitAtmosphericDecayAndLifetime(initialAltitudeKm = 200.0, ballisticCoefficientKgM2 = 50.0, solarActivityLevel = 'moderate', body = 'mars') {
    const hKm = Math.max(100.0, Math.min(1000.0, initialAltitudeKm));
    const B = Math.max(1.0, ballisticCoefficientKgM2);

    let RpM = 3389500.0;
    let mu = 4.282837e13; // m^3/s^2 (Mars)
    let rho0 = 1.5e-9; // kg/m^3 at 150 km
    let h0Km = 150.0;
    let HscaleKm = 10.5;

    const act = solarActivityLevel.toLowerCase();
    if (act.includes('low')) {
      HscaleKm = 8.5;
      rho0 = 1.0e-9;
    } else if (act.includes('high') || act.includes('dust')) {
      HscaleKm = 13.0;
      rho0 = 2.5e-9;
    }

    if (body.toLowerCase() === 'earth') {
      RpM = 6378137.0;
      mu = 3.986004418e14;
      rho0 = 2.0e-9;
      h0Km = 200.0;
      HscaleKm = 35.0;
    }

    // Atmospheric density at altitude h
    const rhoKgM3 = rho0 * Math.exp(-(hKm - h0Km) / HscaleKm);

    // Orbital radius and circular speed
    const aM = RpM + (hKm * 1000.0);
    const vMS = Math.sqrt(mu / aM);

    // Orbital period in seconds and minutes
    const pSec = (2.0 * Math.PI * aM) / vMS;
    const pMin = pSec / 60.0;
    const orbitsPerDay = 86400.0 / pSec;

    // Decay rate per orbit in meters
    const deltaAOrbitM = 2.0 * Math.PI * (rhoKgM3 * Math.pow(aM, 2.0)) / B;
    const deltaHDayM = deltaAOrbitM * orbitsPerDay;

    // Estimated lifetime in days
    const tLifeDays = Math.max(0.1, (HscaleKm * 1000.0) / Math.max(1e-4, deltaHDayM));

    let regime = 'Moderate Thermospheric Drag / Multi-Month Decaying Orbit';
    if (hKm < 150.0 || deltaHDayM > 1000.0) {
      regime = 'Rapid Re-entry Corridor / Severe Atmospheric Deceleration (< 10 Days Remaining)';
    } else if (tLifeDays > 365.0) {
      regime = 'Long-Duration Stable Orbit (> 1 Year Operational Lifetime)';
    }

    return {
      orbitAltitudeKm: parseFloat(hKm.toFixed(1)),
      atmosphericDensityKgM3: parseFloat(rhoKgM3.toExponential(4)),
      orbitalPeriodMinutes: parseFloat(pMin.toFixed(2)),
      dailyAltitudeLossMeters: parseFloat(deltaHDayM.toFixed(1)),
      estimatedOrbitalLifetimeDays: parseFloat(tLifeDays.toFixed(1)),
      orbitalDecayRegime: regime
    };
  }

  /**
   * Calculate Phobos / Deimos co-orbital rendezvous trajectory, Hohmann transfer Delta-V from LMO, Hill sphere radius, and QSO insertion delta-V.
   * r_Hill = a_moon * ( m_moon / ( 3 * M_mars ) )^(1/3)
   * Delta_V_Hohmann = Delta_V1 + Delta_V2
   * Reference: Szebehely (1967), Burns (1972), Murray & Dermott (1999), Canup & Salmon (2018) for JAXA MMX / Mars moon rendezvous.
   * @param {string} [moonTarget='phobos'] - Moon target ('phobos', 'deimos')
   * @param {number} [initialMarsOrbitAltitudeKm=400.0] - Initial circular LMO altitude in km
   * @returns {{targetMoon: string, moonSemiMajorAxisKm: number, moonOrbitalSpeedKmS: number, moonOrbitalPeriodHours: number, moonHillSphereRadiusKm: number, hohmannTransferDeltaVKmS: number, transferTimeOfFlightHours: number, qsoProximityInsertionDeltaVMPS: number, rendezvousMissionContext: string}}
   */
  static computeMartianMoonCoOrbitalRendezvousAndHillSphere(moonTarget = 'phobos', initialMarsOrbitAltitudeKm = 400.0) {
    const h1Km = Math.max(50.0, initialMarsOrbitAltitudeKm);
    const rMarsKm = 3389.5;
    const muMars = 42828.37; // km^3/s^2
    const mMarsKg = 6.4171e23;

    let aMoonKm = 9376.0;
    let mMoonKg = 1.0659e16;
    let name = 'Phobos (Martian Inner Moon)';
    let qsoDvMPS = 12.5;

    const tKey = moonTarget.toLowerCase();
    if (tKey.includes('deimos')) {
      aMoonKm = 23463.0;
      mMoonKg = 1.4762e15;
      name = 'Deimos (Martian Outer Moon)';
      qsoDvMPS = 6.5;
    }

    // Moon circular orbital speed & period
    const vMoonKmS = Math.sqrt(muMars / aMoonKm);
    const pMoonSec = 2.0 * Math.PI * Math.sqrt(Math.pow(aMoonKm, 3.0) / muMars);
    const pMoonHours = pMoonSec / 3600.0;

    // Moon Hill Sphere radius r_Hill = a * ( m_moon / (3 * M_mars) )^(1/3)
    const massRatio = mMoonKg / (3.0 * mMarsKg);
    const rHillKm = aMoonKm * Math.pow(massRatio, 1.0 / 3.0);

    // Hohmann transfer from LMO (r1) to Moon orbit (aMoon)
    const r1Km = rMarsKm + h1Km;
    const v1KmS = Math.sqrt(muMars / r1Km);
    const atKm = (r1Km + aMoonKm) / 2.0;

    const vTransPeri = Math.sqrt(muMars * ((2.0 / r1Km) - (1.0 / atKm)));
    const dv1 = vTransPeri - v1KmS;

    const vTransApo = Math.sqrt(muMars * ((2.0 / aMoonKm) - (1.0 / atKm)));
    const dv2 = vMoonKmS - vTransApo;

    const dvTotKmS = Math.abs(dv1) + Math.abs(dv2);
    const tofSec = Math.PI * Math.sqrt(Math.pow(atKm, 3.0) / muMars);
    const tofHours = tofSec / 3600.0;

    return {
      targetMoon: name,
      moonSemiMajorAxisKm: parseFloat(aMoonKm.toFixed(1)),
      moonOrbitalSpeedKmS: parseFloat(vMoonKmS.toFixed(3)),
      moonOrbitalPeriodHours: parseFloat(pMoonHours.toFixed(3)),
      moonHillSphereRadiusKm: parseFloat(rHillKm.toFixed(2)),
      hohmannTransferDeltaVKmS: parseFloat(dvTotKmS.toFixed(3)),
      transferTimeOfFlightHours: parseFloat(tofHours.toFixed(3)),
      qsoProximityInsertionDeltaVMPS: parseFloat(qsoDvMPS.toFixed(1)),
      rendezvousMissionContext: context
    };
  }

  /**
   * Calculate Mars Sun-Synchronous Orbit (SSO) required inclination, nodal precession rate, and repeat ground track swath spacing.
   * dot_Omega = - (3/2) * J2 * (Rp / a)^2 * n * cos(i) == dot_lambda_sun
   * Delta_lambda_eq = ( 2 * pi * Rp ) / N_orbits
   * Reference: Vallado (2013), Curtis (2013) for MRO & Mars Odyssey sun-synchronous mapping orbit design.
   * @param {number} [orbitAltitudeKm=300.0] - Circular mapping orbit altitude in km (150 to 800 km)
   * @param {number} [repeatOrbitsCount=187] - Number of integer orbits before repeat ground track closure
   * @param {number} [repeatDaysCount=14] - Number of Martian sols for ground track repeat cycle
   * @returns {{orbitAltitudeKm: number, sunSyncInclinationDeg: number, nodalPrecessionRateDegDay: number, orbitalPeriodMinutes: number, dailyOrbitsCount: number, equatorialInterTrackSpacingKm: number, mappingOrbitDesignContext: string}}
   */
  static computeMartianSunSynchronousAndRepeatGroundTrackOrbit(orbitAltitudeKm = 300.0, repeatOrbitsCount = 187, repeatDaysCount = 14) {
    const hKm = Math.max(100.0, Math.min(1500.0, orbitAltitudeKm));
    const Np = Math.max(1, repeatOrbitsCount);
    const Nd = Math.max(1, repeatDaysCount);

    const RpKm = 3389.5;
    const muMars = 42828.37; // km^3/s^2
    const J2 = 0.00196045; // Mars J2 harmonic

    // Sun-synchronous nodal precession rate for Mars (360 deg / 686.98 sols)
    const dotOmegaSSODegDay = 360.0 / 686.98; // ~0.52403 deg/day
    const dotOmegaSSORadS = (dotOmegaSSODegDay * (Math.PI / 180.0)) / 86400.0;

    // Semi-major axis and mean motion
    const aKm = RpKm + hKm;
    const nRadS = Math.sqrt(muMars / Math.pow(aKm, 3.0));

    // Nodal precession equation: dot_Omega = -1.5 * J2 * (Rp/a)^2 * n * cos(i)
    // cos(i) = - dot_Omega_SSO / ( 1.5 * J2 * (Rp/a)^2 * n )
    const j2Factor = 1.5 * J2 * Math.pow(RpKm / aKm, 2.0) * nRadS;
    const cosInc = -dotOmegaSSORadS / j2Factor;
    const incRad = Math.acos(Math.max(-1.0, Math.min(1.0, cosInc)));
    const incDeg = incRad * (180.0 / Math.PI);

    // Orbital period in seconds and minutes
    const pSec = (2.0 * Math.PI) / nRadS;
    const pMin = pSec / 60.0;
    const orbitsPerSol = (88775.244) / pSec; // 88775.244 s per Mars sol

    // Equatorial ground track swath spacing
    const deltaLambdaEqKm = (2.0 * Math.PI * RpKm) / Np;

    return {
      orbitAltitudeKm: parseFloat(hKm.toFixed(1)),
      sunSyncInclinationDeg: parseFloat(incDeg.toFixed(2)),
      nodalPrecessionRateDegDay: parseFloat(dotOmegaSSODegDay.toFixed(5)),
      orbitalPeriodMinutes: parseFloat(pMin.toFixed(2)),
      dailyOrbitsCount: parseFloat(orbitsPerSol.toFixed(2)),
      equatorialInterTrackSpacingKm: parseFloat(deltaLambdaEqKm.toFixed(2)),
      mappingOrbitDesignContext: `Mars Sun-Synchronous Frozen Mapping Orbit (${incDeg.toFixed(1)} deg Inclination, ${Np}/${Nd} Sol Repeat Pattern)`
    };
  }

  /**
   * Calculate Areostationary Orbit (AERO) altitude, orbital velocity, triaxial gravity (J22) longitudinal drift acceleration, and station-keeping Delta-V.
   * r_areo = ( mu / omega_mars^2 )^(1/3)
   * ddot_lambda = - 12 * ( mu * Rp^2 / r_areo^5 ) * J22 * sin( 2 * ( lambda - lambda_22 ) )
   * Reference: Szebehely (1967), Silva et al. (2008), Curtis (2013) for Mars synchronous orbit & constellation design.
   * @param {number} [targetLongitudeDeg=0.0] - Desired planetocentric subsatellite longitude in degrees (-180 to +180 deg)
   * @returns {{areostationaryAltitudeKm: number, areostationaryRadiusKm: number, areostationarySpeedKmS: number, orbitalPeriodHours: number, longitudinalDriftAccelerationDegDay2: number, annualStationKeepingDeltaVMPS: number, stableLibrationWells: string, areostationaryMissionContext: string}}
   */
  static computeAreostationaryOrbitAltitudeAndLongitudinalDrift(targetLongitudeDeg = 0.0) {
    const lonDeg = ((targetLongitudeDeg % 360.0) + 540.0) % 360.0 - 180.0;
    const lonRad = lonDeg * (Math.PI / 180.0);

    const RpKm = 3389.5;
    const muMars = 42828.37; // km^3/s^2
    const omegaMarsRadS = 7.077651e-5; // rad/s (88775.244 s sol)
    const J22 = -5.467e-5;
    const lambda22Deg = 75.0;
    const lambda22Rad = lambda22Deg * (Math.PI / 180.0);

    // Synchronous radius r = ( mu / omega^2 )^(1/3)
    const rAreoKm = Math.pow(muMars / Math.pow(omegaMarsRadS, 2.0), 1.0 / 3.0);
    const hAreoKm = rAreoKm - RpKm;

    // Circular orbital speed & period
    const vAreoKmS = Math.sqrt(muMars / rAreoKm);
    const pAreoSec = (2.0 * Math.PI) / omegaMarsRadS;
    const pAreoHours = pAreoSec / 3600.0;

    // Longitudinal acceleration ddot_lambda = - 12 * ( mu * Rp^2 / r^5 ) * J22 * sin( 2 * (lambda - lambda22) )
    const coeff = -12.0 * (muMars * Math.pow(RpKm, 2.0) / Math.pow(rAreoKm, 5.0)) * J22;
    const ddotLambdaRadS2 = coeff * Math.sin(2.0 * (lonRad - lambda22Rad));
    const SECS_PER_DAY = 86400.0;
    const RAD_TO_DEG = 180.0 / Math.PI;
    const ddotLambdaDegDay2 = ddotLambdaRadS2 * Math.pow(SECS_PER_DAY, 2.0) * RAD_TO_DEG;

    // Annual station-keeping Delta-V budget (m/s/year)
    const deltaVSkMPSYear = Math.abs(ddotLambdaRadS2) * (rAreoKm * 1000.0) * 3.15576e7;

    return {
      areostationaryAltitudeKm: parseFloat(hAreoKm.toFixed(1)),
      areostationaryRadiusKm: parseFloat(rAreoKm.toFixed(1)),
      areostationarySpeedKmS: parseFloat(vAreoKmS.toFixed(3)),
      orbitalPeriodHours: parseFloat(pAreoHours.toFixed(4)),
      longitudinalDriftAccelerationDegDay2: parseFloat(ddotLambdaDegDay2.toFixed(5)),
      annualStationKeepingDeltaVMPS: parseFloat(deltaVSkMPSYear.toFixed(2)),
      stableLibrationWells: 'Stable Libration Wells at 17.5 W (342.5 E) and 162.5 E; Unstable Saddles at 72.5 E and 107.5 W',
      areostationaryMissionContext: 'Areostationary Equatorial Relay & Continuous Planetary Disk Monitoring'
    };
  }

  /**
   * Calculate Mars-Earth Free Return unpowered flyby trajectory, hyperbolic bending angle, periapsis speed, and Earth return velocity.
   * sin( delta / 2 ) = 1 / ( 1 + ( rp * v_inf^2 ) / mu_mars )
   * v_peri = sqrt( v_inf^2 + 2 * mu_mars / rp )
   * Reference: Hollister (1969), Aldrin (1985), Byrnes et al. (1993), Tito et al. (2013) for Inspiration Mars & cycler architectures.
   * @param {number} [marsFlybyAltitudeKm=250.0] - Mars closest approach flyby altitude in km (100 to 5000 km)
   * @param {number} [marsVInfKmS=5.65] - Mars approach hyperbolic excess velocity in km/s (3.0 to 9.0 km/s)
   * @returns {{marsFlybyAltitudeKm: number, marsClosestApproachSpeedKmS: number, hyperbolicTurnAngleDeg: number, earthReturnVInfKmS: number, totalMissionDurationDays: number, freeReturnTrajectoryContext: string}}
   */
  static computeMarsFreeReturnCircumlunarInterplanetaryFlyby(marsFlybyAltitudeKm = 250.0, marsVInfKmS = 5.65) {
    const hpKm = Math.max(50.0, marsFlybyAltitudeKm);
    const vInf = Math.max(1.0, marsVInfKmS);

    const rMarsKm = 3389.5;
    const muMars = 42828.37; // km^3/s^2

    // Periapsis radius and speed at Mars
    const rpKm = rMarsKm + hpKm;
    const vPeriKmS = Math.sqrt(Math.pow(vInf, 2.0) + (2.0 * muMars) / rpKm);

    // Hyperbolic turn angle delta: sin(delta/2) = 1 / ( 1 + rp*vInf^2 / mu )
    const denom = 1.0 + (rpKm * Math.pow(vInf, 2.0)) / muMars;
    const sinHalfDelta = 1.0 / denom;
    const halfDeltaRad = Math.asin(Math.max(-1.0, Math.min(1.0, sinHalfDelta)));
    const deltaDeg = 2.0 * halfDeltaRad * (180.0 / Math.PI);

    // Free return duration (~501 days for Inspiration Mars fast flyby, ~730 days for 2:1 resonance)
    const missionDays = 501.0;
    const vInfEarthRet = Math.sqrt(Math.pow(vInf, 2.0) + 6.8); // Return excess

    return {
      marsFlybyAltitudeKm: parseFloat(hpKm.toFixed(1)),
      marsClosestApproachSpeedKmS: parseFloat(vPeriKmS.toFixed(3)),
      hyperbolicTurnAngleDeg: parseFloat(deltaDeg.toFixed(2)),
      earthReturnVInfKmS: parseFloat(vInfEarthRet.toFixed(3)),
      totalMissionDurationDays: parseFloat(missionDays.toFixed(0)),
      freeReturnTrajectoryContext: 'Unpowered Ballistic Mars-to-Earth Free Return Flyby (Inspiration Mars Architecture)'
    };
  }

  /**
   * Calculate Mars-to-Jupiter Interplanetary Hohmann Transfer trajectory, Trans-Jupiter Injection Delta-V, time of flight, and Asteroid Belt crossing.
   * a_t = ( r_Mars + r_Jupiter ) / 2
   * TOF = pi * sqrt( a_t^3 / mu_sun )
   * Delta_V_TJI = sqrt( v_inf_M^2 + 2*mu_M / r_park ) - v_circ
   * Reference: Bate, Mueller & White (1971), Curtis (2013) for outer planet exploration and Main Belt Asteroid corridor transit.
   * @param {number} [marsParkingAltitudeKm=400.0] - Mars parking orbit altitude in km
   * @param {number} [jupiterArrivalPerijoveAltitudeKm=500000.0] - Jupiter arrival perijove altitude in km
   * @returns {{transferSemiMajorAxisAU: number, timeOfFlightDays: number, timeOfFlightYears: number, marsDepartureVInfKmS: number, transJupiterInjectionDeltaVKmS: number, jupiterArrivalVInfKmS: number, asteroidBeltTransitContext: string}}
   */
  static computeMarsJupiterInterplanetaryHohmannTransfer(marsParkingAltitudeKm = 400.0, jupiterArrivalPerijoveAltitudeKm = 500000.0) {
    const hpKm = Math.max(50.0, marsParkingAltitudeKm);

    const AU_KM = 1.495978707e8;
    const muSun = 1.32712440018e11; // km^3/s^2
    const muMars = 42828.37; // km^3/s^2
    const rMarsPlanetKm = 3389.5;

    const rMarsKm = 1.52368 * AU_KM;
    const rJupKm = 5.20440 * AU_KM;

    // Mars and Jupiter circular speeds
    const vMarsHel = Math.sqrt(muSun / rMarsKm);
    const vJupHel = Math.sqrt(muSun / rJupKm);

    // Hohmann transfer semi-major axis
    const atKm = (rMarsKm + rJupKm) / 2.0;
    const atAU = atKm / AU_KM;

    // Time of flight
    const tofSec = Math.PI * Math.sqrt(Math.pow(atKm, 3.0) / muSun);
    const tofDays = tofSec / 86400.0;
    const tofYears = tofDays / 365.25;

    // Perihelion speed at Mars departure
    const vpHel = Math.sqrt(muSun * ((2.0 / rMarsKm) - (1.0 / atKm)));
    const vInfMars = Math.abs(vpHel - vMarsHel);

    // Trans-Jupiter Injection burn from LMO
    const rParkKm = rMarsPlanetKm + hpKm;
    const vParkCirc = Math.sqrt(muMars / rParkKm);
    const vParkHyp = Math.sqrt(Math.pow(vInfMars, 2.0) + (2.0 * muMars) / rParkKm);
    const dvTji = vParkHyp - vParkCirc;

    // Aphelion speed at Jupiter arrival
    const vaHel = Math.sqrt(muSun * ((2.0 / rJupKm) - (1.0 / atKm)));
    const vInfJup = Math.abs(vJupHel - vaHel);

    return {
      transferSemiMajorAxisAU: parseFloat(atAU.toFixed(4)),
      timeOfFlightDays: parseFloat(tofDays.toFixed(1)),
      timeOfFlightYears: parseFloat(tofYears.toFixed(2)),
      marsDepartureVInfKmS: parseFloat(vInfMars.toFixed(3)),
      transJupiterInjectionDeltaVKmS: parseFloat(dvTji.toFixed(3)),
      jupiterArrivalVInfKmS: parseFloat(vInfJup.toFixed(3)),
      asteroidBeltTransitContext: 'Main Belt Asteroid Crossing (Ceres 2.77 AU, Vesta 2.36 AU) & Outer Gas Giant Transfer'
    };
  }

  /**
   * Calculate Mars single-pass hypersonic aerocapture corridor, Sutton-Graves peak stagnation convective heat flux, post-capture apoapsis, and propulsive Delta-V savings.
   * q_stag = k_SG * sqrt( rho / R_N ) * v^3
   * epsilon_exit = v_exit^2 / 2 - mu_mars / r_EI < 0 (bound captured orbit)
   * Reference: Sutton & Graves (1971), Cruz et al. (2006), Braun & Manning (2007) for Mars robotic & crewed aerocapture missions.
   * @param {number} [entryVelocityKmS=6.0] - Atmospheric interface entry speed in km/s (5.5 to 8.5 km/s)
   * @param {number} [corridorPeriapsisAltitudeKm=52.0] - Target periapsis atmospheric pass altitude in km (40 to 70 km)
   * @param {number} [vehicleNoseRadiusMeters=0.75] - Aeroshell nose bluntness radius in meters (0.2 to 3.0 m)
   * @returns {{entryVelocityKmS: number, corridorPeriapsisAltitudeKm: number, peakStagnationHeatFluxKWm2: number, peakStagnationHeatFluxWcm2: number, atmosphericExitSpeedKmS: number, capturedOrbitApoapsisKm: number, propulsiveDeltaVSavedKmS: number, aerocaptureMissionContext: string}}
   */
  static computeMarsAerocaptureCorridorAndPeakStagnationHeatFlux(entryVelocityKmS = 6.0, corridorPeriapsisAltitudeKm = 52.0, vehicleNoseRadiusMeters = 0.75) {
    const vEntry = Math.max(5.0, Math.min(10.0, entryVelocityKmS));
    const hPeri = Math.max(35.0, Math.min(80.0, corridorPeriapsisAltitudeKm));
    const Rn = Math.max(0.1, vehicleNoseRadiusMeters);

    const rMarsKm = 3389.5;
    const muMars = 42828.37; // km^3/s^2
    const hEIKm = 125.0; // Mars atmospheric interface
    const rEIKm = rMarsKm + hEIKm;

    // Mars atmospheric density profile: rho(z) = rho0 * exp( - z / H_scale )
    const rho0 = 0.020; // kg/m^3 at datum
    const Hscale = 11.1; // km
    const rhoPeri = rho0 * Math.exp(-hPeri / Hscale); // kg/m^3

    // Velocity at atmospheric periapsis (m/s)
    const vPeriMS = (vEntry * 1000.0) * 0.975;

    // Sutton-Graves convective stagnation heat flux (k_SG = 1.9027e-4 for Mars CO2)
    const kSG = 1.9027e-4;
    const qStagWm2 = kSG * Math.sqrt(rhoPeri / Rn) * Math.pow(vPeriMS, 3.0);
    const qStagKWm2 = qStagWm2 / 1000.0;
    const qStagWcm2 = qStagWm2 / 10000.0;

    // Atmospheric velocity depletion Delta-V (energy loss during pass)
    const dvAeroKmS = 1.35 + (65.0 - hPeri) * 0.045;
    const vExitKmS = vEntry - dvAeroKmS;

    // Specific orbital energy at atmospheric exit
    const epsExit = (Math.pow(vExitKmS, 2.0) / 2.0) - (muMars / rEIKm);
    let apoapsisAltKm = 3000.0;
    if (epsExit < 0) {
      const aCapturedKm = -muMars / (2.0 * epsExit);
      const rApoKm = (2.0 * aCapturedKm) - (rMarsKm + hPeri);
      apoapsisAltKm = rApoKm - rMarsKm;
    }

    return {
      entryVelocityKmS: parseFloat(vEntry.toFixed(2)),
      corridorPeriapsisAltitudeKm: parseFloat(hPeri.toFixed(1)),
      peakStagnationHeatFluxKWm2: parseFloat(qStagKWm2.toFixed(1)),
      peakStagnationHeatFluxWcm2: parseFloat(qStagWcm2.toFixed(2)),
      atmosphericExitSpeedKmS: parseFloat(vExitKmS.toFixed(3)),
      capturedOrbitApoapsisKm: parseFloat(Math.max(150.0, apoapsisAltKm).toFixed(1)),
      propulsiveDeltaVSavedKmS: parseFloat(dvAeroKmS.toFixed(3)),
      aerocaptureMissionContext: 'Hypersonic Mars Aerocapture Direct Insertion (Propellantless Orbital Capture)'
    };
  }

  /**
   * Calculate multi-pass Mars aerobraking orbital decay, total atmospheric passes, campaign duration, and propulsive Delta-V savings.
   * Delta_v_pass = sqrt( 2 * pi * H_scale * rp ) * ( C_D * A / m ) * rho(hp) * v_p
   * N_passes = ( r_a_initial - r_a_target ) / Delta_r_a_mean
   * Reference: Lyons et al. (1999), Johnston et al. (2007), Curtis (2013) for Mars Global Surveyor & Odyssey aerobraking campaigns.
   * @param {number} [initialApoapsisAltitudeKm=35000.0] - High elliptic capture apoapsis altitude in km (10000 to 70000 km)
   * @param {number} [corridorPeriapsisAltitudeKm=115.0] - Aerobraking drag pass periapsis altitude in km (95 to 135 km)
   * @param {number} [targetScienceApoapsisKm=450.0] - Target circularized science orbit apoapsis in km (200 to 1000 km)
   * @param {number} [vehicleAreaToMassM2Kg=0.015] - Spacecraft drag area to mass ratio in m^2/kg (0.005 to 0.05 m^2/kg)
   * @returns {{initialApoapsisKm: number, corridorPeriapsisKm: number, targetApoapsisKm: number, estimatedAerobrakingPasses: number, campaignDurationSols: number, campaignDurationMonths: number, totalPropulsiveDeltaVSavedKmS: number, aerobrakingMissionContext: string}}
   */
  static computeMarsAerobrakingOrbitDecayPasses(initialApoapsisAltitudeKm = 35000.0, corridorPeriapsisAltitudeKm = 115.0, targetScienceApoapsisKm = 450.0, vehicleAreaToMassM2Kg = 0.015) {
    const haInit = Math.max(1000.0, initialApoapsisAltitudeKm);
    const hp = Math.max(90.0, Math.min(140.0, corridorPeriapsisAltitudeKm));
    const haTarget = Math.max(150.0, targetScienceApoapsisKm);
    const areaToMass = Math.max(0.001, vehicleAreaToMassM2Kg);

    const rMarsKm = 3389.5;
    const muMars = 42828.37; // km^3/s^2
    const HscaleKm = 11.1;

    // Atmospheric density at aerobraking corridor (kg/m^3)
    const rho0 = 0.020;
    const rhoPeri = rho0 * Math.exp(-hp / HscaleKm);

    // Initial and target orbital elements
    const rpKm = rMarsKm + hp;
    const raInitKm = rMarsKm + haInit;
    const aInitKm = (rpKm + raInitKm) / 2.0;

    const raTargKm = rMarsKm + haTarget;
    const aTargKm = (rpKm + raTargKm) / 2.0;

    // Mean velocity at periapsis (m/s)
    const vpInitMS = Math.sqrt(muMars * 1e9 * ((2.0 / (rpKm * 1000.0)) - (1.0 / (aInitKm * 1000.0))));

    // Velocity depletion per pass (m/s)
    const CD = 2.2;
    const factor = Math.sqrt(2.0 * Math.PI * (HscaleKm * 1000.0) * (rpKm * 1000.0));
    const dvPassMS = factor * (CD * areaToMass) * rhoPeri * vpInitMS;

    // Mean apoapsis reduction per pass (km)
    const draMeanKm = (4.0 * Math.pow(raInitKm, 1.2) / muMars) * (vpInitMS / 1000.0) * (dvPassMS / 1000.0);
    const totalPasses = Math.max(10, Math.round((haInit - haTarget) / Math.max(5.0, draMeanKm)));

    // Total campaign duration in sols (summing orbital periods)
    const pInitHours = (2.0 * Math.PI * Math.sqrt(Math.pow(aInitKm, 3.0) / muMars)) / 3600.0;
    const pTargHours = (2.0 * Math.PI * Math.sqrt(Math.pow(aTargKm, 3.0) / muMars)) / 3600.0;
    const pMeanHours = (pInitHours + pTargHours) / 2.0;
    const durationSols = (totalPasses * pMeanHours) / 24.6229;
    const durationMonths = durationSols / 30.4;

    // Total propulsive Delta-V saved
    const vApoInit = Math.sqrt(muMars * ((2.0 / raInitKm) - (1.0 / aInitKm)));
    const vCircTarg = Math.sqrt(muMars / aTargKm);
    const dvSavedKmS = Math.abs(vCircTarg - vApoInit) + 0.45;

    return {
      initialApoapsisKm: parseFloat(haInit.toFixed(1)),
      corridorPeriapsisKm: parseFloat(hp.toFixed(1)),
      targetApoapsisKm: parseFloat(haTarget.toFixed(1)),
      estimatedAerobrakingPasses: totalPasses,
      campaignDurationSols: parseFloat(durationSols.toFixed(1)),
      campaignDurationMonths: parseFloat(durationMonths.toFixed(1)),
      totalPropulsiveDeltaVSavedKmS: parseFloat(dvSavedKmS.toFixed(3)),
      aerobrakingMissionContext: `Multi-Pass Mars Aerobraking Campaign (${totalPasses} Drag Passes in ~${durationMonths.toFixed(1)} Months)`
    };
  }

  /**
   * Calculate Mars-Venus interplanetary gravity assist trajectory, hyperbolic turn angle, heliocentric energy pumping, and slingshot Delta-V.
   * sin( delta / 2 ) = 1 / ( 1 + ( rp * v_inf^2 ) / mu_venus )
   * Delta_v_hel = 2 * v_inf * sin( delta / 2 )
   * Reference: Broucke (1988), Labunsky et al. (1998), Curtis (2013) for inner planet gravity assist tour architectures.
   * @param {number} [venusFlybyAltitudeKm=300.0] - Venus closest approach flyby altitude in km (200 to 10000 km)
   * @param {number} [approachVInfKmS=5.50] - Venus approach hyperbolic excess speed in km/s (2.0 to 12.0 km/s)
   * @returns {{venusFlybyAltitudeKm: number, venusPeriapsisSpeedKmS: number, hyperbolicTurnAngleDeg: number, heliocentricDeltaVBoostKmS: number, timeOfFlightToVenusDays: number, gravityAssistMissionContext: string}}
   */
  static computeMarsVenusGravityAssistTrajectory(venusFlybyAltitudeKm = 300.0, approachVInfKmS = 5.50) {
    const hpKm = Math.max(150.0, venusFlybyAltitudeKm);
    const vInf = Math.max(1.0, approachVInfKmS);

    const rVenusKm = 6051.8;
    const muVenus = 324859.0; // km^3/s^2

    // Periapsis radius and speed at Venus
    const rpKm = rVenusKm + hpKm;
    const vPeriKmS = Math.sqrt(Math.pow(vInf, 2.0) + (2.0 * muVenus) / rpKm);

    // Hyperbolic turn angle delta: sin(delta/2) = 1 / ( 1 + rp*vInf^2 / mu )
    const denom = 1.0 + (rpKm * Math.pow(vInf, 2.0)) / muVenus;
    const sinHalfDelta = 1.0 / denom;
    const halfDeltaRad = Math.asin(Math.max(-1.0, Math.min(1.0, sinHalfDelta)));
    const deltaDeg = 2.0 * halfDeltaRad * (180.0 / Math.PI);

    // Heliocentric velocity boost Delta-V
    const dvHelKmS = 2.0 * vInf * sinHalfDelta;

    // Time of flight Mars to Venus (~217 days for Hohmann inward)
    const tofDays = 217.4;

    return {
      venusFlybyAltitudeKm: parseFloat(hpKm.toFixed(1)),
      venusPeriapsisSpeedKmS: parseFloat(vPeriKmS.toFixed(3)),
      hyperbolicTurnAngleDeg: parseFloat(deltaDeg.toFixed(2)),
      heliocentricDeltaVBoostKmS: parseFloat(dvHelKmS.toFixed(3)),
      timeOfFlightToVenusDays: parseFloat(tofDays.toFixed(1)),
      gravityAssistMissionContext: `Venus Gravity Assist Slingshot (${deltaDeg.toFixed(1)} deg Turn Angle, +${dvHelKmS.toFixed(2)} km/s Heliocentric Boost)`
    };
  }

  /**
   * Calculate Solar Electric Propulsion (SEP) low-thrust spiral transfer, continuous burn duration, and Xenon propellant budget.
   * dot_m = T / ( g_0 * I_sp )
   * m_p = m_0 * ( 1 - exp( - Delta_V / ( g_0 * I_sp ) ) )
   * t_burn = m_p / dot_m
   * Reference: Edelbaum (1961), Petropoulos & Longuski (2004), Curtis (2013) for Dawn & Mars Sample Return SEP trajectories.
   * @param {number} [spacecraftInitialMassKg=1200.0] - Wet launch mass in kg (200 to 10000 kg)
   * @param {number} [thrustNewtons=0.25] - Ion thruster total thrust in Newtons (0.05 to 5.0 N)
   * @param {number} [specificImpulseSec=3500.0] - Ion engine specific impulse in seconds (1500 to 5000 s)
   * @param {number} [targetDeltaVKmS=5.65] - Total heliocentric transfer Delta-V in km/s (1.0 to 15.0 km/s)
   * @returns {{initialMassKg: number, finalMassKg: number, xenonPropellantConsumedKg: number, continuousBurnTimeDays: number, meanThrustAccelerationMmS2: number, ionPropulsionContext: string}}
   */
  static computeLowThrustSEPMarsEarthTrajectory(spacecraftInitialMassKg = 1200.0, thrustNewtons = 0.25, specificImpulseSec = 3500.0, targetDeltaVKmS = 5.65) {
    const m0 = Math.max(50.0, spacecraftInitialMassKg);
    const T = Math.max(0.01, thrustNewtons);
    const Isp = Math.max(500.0, specificImpulseSec);
    const dvKmS = Math.max(0.1, targetDeltaVKmS);

    const g0 = 9.80665; // m/s^2
    const ceMS = g0 * Isp; // Effective exhaust velocity (m/s)
    const ceKmS = ceMS / 1000.0;

    // Mass flow rate (kg/s and kg/day)
    const mdotKgS = T / ceMS;

    // Propellant mass consumed: mp = m0 * (1 - exp(-dv / ce))
    const massRatio = Math.exp(- (dvKmS * 1000.0) / ceMS);
    const mpKg = m0 * (1.0 - massRatio);
    const mfKg = m0 - mpKg;

    // Continuous burn time (seconds and days)
    const tBurnSec = mpKg / mdotKgS;
    const tBurnDays = tBurnSec / 86400.0;

    // Mean acceleration (mm/s^2)
    const mMean = (m0 + mfKg) / 2.0;
    const aMeanMmS2 = (T / mMean) * 1000.0;

    return {
      initialMassKg: parseFloat(m0.toFixed(1)),
      finalMassKg: parseFloat(mfKg.toFixed(1)),
      xenonPropellantConsumedKg: parseFloat(mpKg.toFixed(2)),
      continuousBurnTimeDays: parseFloat(tBurnDays.toFixed(1)),
      meanThrustAccelerationMmS2: parseFloat(aMeanMmS2.toFixed(3)),
      ionPropulsionContext: `Solar Electric Low-Thrust Spiral (Isp ${Isp.toFixed(0)}s, ${mpKg.toFixed(1)} kg Xenon consumed over ${tBurnDays.toFixed(0)} Days)`
    };
  }

  /**
   * Calculate Clohessy-Wiltshire (CW) Hill linearized relative motion and two-burn rendezvous Delta-V for Phobos / Deimos proximity operations.
   * ddot_x - 2*n*dot_y - 3*n^2*x = 0
   * ddot_y + 2*n*dot_x = 0
   * Delta_V = || Delta_v1 || + || Delta_v2 ||
   * Reference: Clohessy & Wiltshire (1960), Wie (1998), Curtis (2013) for JAXA MMX & Phobos Sample Return rendezvous.
   * @param {string} [moonName='Phobos'] - Target moon name ('Phobos' or 'Deimos')
   * @param {number} [initialRelDistXKm=5.0] - Initial radial standoff distance in km (-50 to +50 km)
   * @param {number} [initialRelDistYKm=15.0] - Initial in-track standoff distance in km (-100 to +100 km)
   * @param {number} [rendezvousTimeHours=2.0] - Target transfer flight duration in hours (0.5 to 24.0 hours)
   * @returns {{targetMoon: string, orbitalMeanMotionRadS: number, transferDurationHours: number, departureBurnDeltaVMS: number, arrivalBrakingDeltaVMS: number, totalRendezvousDeltaVMS: number, relativeMotionContext: string}}
   */
  static computeMartianMoonClohessyWiltshireProximityManeuver(moonName = 'Phobos', initialRelDistXKm = 5.0, initialRelDistYKm = 15.0, rendezvousTimeHours = 2.0) {
    const isDeimos = moonName.toLowerCase().includes('deimos');
    const targetName = isDeimos ? 'Deimos' : 'Phobos';
    const aMoonKm = isDeimos ? 23463.0 : 9376.0;
    const muMars = 42828.37; // km^3/s^2

    // Mean motion n (rad/s)
    const n = Math.sqrt(muMars / Math.pow(aMoonKm, 3.0));

    const x0 = initialRelDistXKm;
    const y0 = initialRelDistYKm;
    const tSec = Math.max(300.0, rendezvousTimeHours * 3600.0);
    const tau = n * tSec;

    // CW State Transition Matrix elements
    const s = Math.sin(tau);
    const c = Math.cos(tau);

    // Phi_rr
    const phi11 = 4.0 - 3.0 * c;
    const phi12 = 0.0;
    const phi21 = 6.0 * (s - tau);
    const phi22 = 1.0;

    // Phi_rv (scaled by n)
    const m11 = s / n;
    const m12 = (2.0 * (1.0 - c)) / n;
    const m21 = (2.0 * (c - 1.0)) / n;
    const m22 = (4.0 * s - 3.0 * tau) / n;

    // Determinant of Phi_rv
    const detM = (m11 * m22) - (m12 * m21);

    let dv1MS = 12.5;
    let dv2MS = 10.2;

    if (Math.abs(detM) > 1e-7) {
      // Invert Phi_rv
      const inv11 = m22 / detM;
      const inv12 = -m12 / detM;
      const inv21 = -m21 / detM;
      const inv22 = m11 / detM;

      // Desired final position = (0, 0)
      const targetX = - (phi11 * x0 + phi12 * y0);
      const targetY = - (phi21 * x0 + phi22 * y0);

      const vx0KmS = inv11 * targetX + inv12 * targetY;
      const vy0KmS = inv21 * targetX + inv22 * targetY;

      dv1MS = Math.sqrt(Math.pow(vx0KmS, 2.0) + Math.pow(vy0KmS, 2.0)) * 1000.0;

      // Final velocity at arrival
      const vxArrivalKmS = (3.0 * n * s * x0) + (c * vx0KmS) + (2.0 * s * vy0KmS);
      const vyArrivalKmS = (6.0 * n * (c - 1.0) * x0) - (2.0 * s * vx0KmS) + ((4.0 * c - 3.0) * vy0KmS);
      dv2MS = Math.sqrt(Math.pow(vxArrivalKmS, 2.0) + Math.pow(vyArrivalKmS, 2.0)) * 1000.0;
    }

    const totalDvMS = dv1MS + dv2MS;

    return {
      targetMoon: targetName,
      orbitalMeanMotionRadS: parseFloat(n.toExponential(4)),
      transferDurationHours: parseFloat((tSec / 3600.0).toFixed(2)),
      departureBurnDeltaVMS: parseFloat(dv1MS.toFixed(2)),
      arrivalBrakingDeltaVMS: parseFloat(dv2MS.toFixed(2)),
      totalRendezvousDeltaVMS: parseFloat(totalDvMS.toFixed(2)),
      relativeMotionContext: `Clohessy-Wiltshire Co-Orbital Rendezvous with ${targetName} (${totalDvMS.toFixed(1)} m/s Total Delta-V in ${rendezvousTimeHours.toFixed(1)} Hours)`
    };
  }

  /**
   * Calculate interplanetary direct / high-energy transfer trajectory from Mars to Outer Ice Giants (Uranus / Neptune).
   * a_t = ( r_mars + r_target ) / 2
   * TOF = pi * sqrt( a_t^3 / mu_sun )
   * Delta_V_TII = sqrt( v_inf^2 + 2*mu_mars / r_park ) - sqrt( mu_mars / r_park )
   * Reference: Prussing & Conway (1993), Bate et al. (1971), Curtis (2013) for Outer Solar System Exploration.
   * @param {string} [targetPlanetName='Uranus'] - Destination planet ('Uranus' or 'Neptune')
   * @param {number} [parkingOrbitAltitudeKm=300.0] - Mars departure low orbit altitude in km (150 to 1000 km)
   * @returns {{destinationPlanet: string, targetSemiMajorAxisAU: number, transferSemiMajorAxisAU: number, timeOfFlightYears: number, timeOfFlightDays: number, transIceGiantInjectionDeltaVKmS: number, arrivalHyperbolicExcessKmS: number, outerSystemMissionContext: string}}
   */
  static computeMarsOuterIceGiantTrajectory(targetPlanetName = 'Uranus', parkingOrbitAltitudeKm = 300.0) {
    const isNeptune = targetPlanetName.toLowerCase().includes('neptune');
    const destName = isNeptune ? 'Neptune' : 'Uranus';
    const rTargetAU = isNeptune ? 30.07 : 19.22;
    const hpKm = Math.max(150.0, parkingOrbitAltitudeKm);

    const AU_KM = 1.495978707e8;
    const muSun = 1.32712440018e11; // km^3/s^2
    const muMars = 42828.37; // km^3/s^2
    const rMarsKm = 3389.5;

    const rMarsAU = 1.52368;
    const rMarsDistKm = rMarsAU * AU_KM;
    const rTargetDistKm = rTargetAU * AU_KM;

    // Transfer ellipse semi-major axis
    const aTransferAU = (rMarsAU + rTargetAU) / 2.0;
    const aTransferKm = aTransferAU * AU_KM;

    // Time of flight (seconds, days, years)
    const tofSec = Math.PI * Math.sqrt(Math.pow(aTransferKm, 3.0) / muSun);
    const tofDays = tofSec / 86400.0;
    const tofYears = tofDays / 365.25;

    // Heliocentric speeds at Mars
    const vMarsKmS = Math.sqrt(muSun / rMarsDistKm);
    const vPeriTransferKmS = Math.sqrt(muSun * ((2.0 / rMarsDistKm) - (1.0 / aTransferKm)));
    const vInfDepKmS = vPeriTransferKmS - vMarsKmS;

    // Trans-Ice-Giant Injection (TII) Delta-V from Mars orbit
    const rParkKm = rMarsKm + hpKm;
    const vParkCircKmS = Math.sqrt(muMars / rParkKm);
    const vTransDepHypKmS = Math.sqrt(Math.pow(vInfDepKmS, 2.0) + (2.0 * muMars / rParkKm));
    const dvTiiKmS = vTransDepHypKmS - vParkCircKmS;

    // Arrival speed at target planet
    const vTargetCircKmS = Math.sqrt(muSun / rTargetDistKm);
    const vApoTransferKmS = Math.sqrt(muSun * ((2.0 / rTargetDistKm) - (1.0 / aTransferKm)));
    const vInfArrKmS = Math.abs(vTargetCircKmS - vApoTransferKmS);

    return {
      destinationPlanet: destName,
      targetSemiMajorAxisAU: parseFloat(rTargetAU.toFixed(2)),
      transferSemiMajorAxisAU: parseFloat(aTransferAU.toFixed(2)),
      timeOfFlightYears: parseFloat(tofYears.toFixed(2)),
      timeOfFlightDays: parseFloat(tofDays.toFixed(1)),
      transIceGiantInjectionDeltaVKmS: parseFloat(dvTiiKmS.toFixed(3)),
      arrivalHyperbolicExcessKmS: parseFloat(vInfArrKmS.toFixed(3)),
      outerSystemMissionContext: `Mars-to-${destName} Interplanetary Transfer (TOF ${tofYears.toFixed(1)} Years, ${dvTiiKmS.toFixed(2)} km/s TII Delta-V)`
    };
  }

  /**
   * Calculate Mars-Sun Trojan co-orbital dynamics at Lagrange points L4 (leading +60 deg) and L5 (trailing -60 deg), Eureka family tadpole libration period, and annual stationkeeping Delta-V.
   * omega_lib = n * sqrt( 27/4 * mu_mass )
   * T_lib = 2 * pi / omega_lib
   * Reference: Murray & Dermott (1999), Connors et al. (2005), Christou et al. (2020) for Mars Trojan asteroids (5261 Eureka).
   * @param {string} [targetLagrangePoint='L5'] - Target Lagrange point ('L4' or 'L5')
   * @param {number} [initialOffsetDistanceKm=50000.0] - Standoff displacement from triangular libration point in km (1000 to 500000 km)
   * @param {number} [stationkeepingDurationYears=5.0] - Mission orbital duration in years (1 to 20 years)
   * @returns {{lagrangePoint: string, orbitalMeanMotionRadS: number, massRatioParameter: number, tadpoleLibrationPeriodYears: number, annualStationkeepingDeltaVMSYear: number, totalMissionStationkeepingDeltaVMS: number, trojanAsteroidContext: string}}
   */
  static computeMartianTrojanLagrangePointL4L5Stationkeeping(targetLagrangePoint = 'L5', initialOffsetDistanceKm = 50000.0, stationkeepingDurationYears = 5.0) {
    const isL4 = targetLagrangePoint.toUpperCase().includes('L4');
    const pointName = isL4 ? 'L4 (Leading +60 deg)' : 'L5 (Trailing -60 deg, Eureka Family)';
    const offsetKm = Math.max(100.0, initialOffsetDistanceKm);
    const durationYrs = Math.max(0.5, stationkeepingDurationYears);

    const rMarsKm = 2.279366e8;
    const muSun = 1.32712440018e11;
    const muMarsMass = 3.227e-7; // Mass ratio M_mars / (M_sun + M_mars)

    // Orbital mean motion (rad/s)
    const n = Math.sqrt(muSun / Math.pow(rMarsKm, 3.0));

    // Tadpole libration frequency (rad/s)
    const omegaLib = n * Math.sqrt(6.75 * muMarsMass);
    const tLibSec = (2.0 * Math.PI) / omegaLib;
    const tLibYears = tLibSec / 3.15576e7;

    // Annual stationkeeping budget (m/s/year) to counteract planetary perturbations and SRP
    const dvSkAnnualMS = 3.25 + (offsetKm / 50000.0) * 0.45;
    const dvSkTotalMS = dvSkAnnualMS * durationYrs;

    return {
      lagrangePoint: pointName,
      orbitalMeanMotionRadS: parseFloat(n.toExponential(4)),
      massRatioParameter: parseFloat(muMarsMass.toExponential(4)),
      tadpoleLibrationPeriodYears: parseFloat(tLibYears.toFixed(1)),
      annualStationkeepingDeltaVMSYear: parseFloat(dvSkAnnualMS.toFixed(2)),
      totalMissionStationkeepingDeltaVMS: parseFloat(dvSkTotalMS.toFixed(2)),
      trojanAsteroidContext: `Sun-Mars ${pointName} Co-Orbital Station (~${tLibYears.toFixed(0)}-yr Tadpole Libration, ${dvSkAnnualMS.toFixed(1)} m/s/yr Stationkeeping)`
    };
  }

  /**
   * Calculate Mars-to-Saturn interplanetary Hohmann transfer trajectory, Trans-Saturn Injection (TSI) Delta-V, and Titan gravity assist orbital capture.
   * a_t = ( r_mars + r_saturn ) / 2
   * TOF = pi * sqrt( a_t^3 / mu_sun )
   * Reference: Bate et al. (1971), Strange et al. (2002), Curtis (2013) for Outer Planet Cassini/Huygens exploration architectures.
   * @param {number} [parkingOrbitAltitudeKm=300.0] - Mars departure low orbit altitude in km (150 to 1000 km)
   * @param {number} [titanFlybyAltitudeKm=1000.0] - Titan closest approach flyby altitude in km (500 to 5000 km)
   * @returns {{departurePlanet: string, destinationPlanet: string, transferSemiMajorAxisAU: number, timeOfFlightYears: number, timeOfFlightDays: number, transSaturnInjectionDeltaVKmS: number, saturnArrivalHyperbolicExcessKmS: number, titanGravityAssistDeltaVKmS: number, outerSystemMissionContext: string}}
   */
  static computeMarsSaturnInterplanetaryTransferAndTitanSlingshot(parkingOrbitAltitudeKm = 300.0, titanFlybyAltitudeKm = 1000.0) {
    const hpKm = Math.max(150.0, parkingOrbitAltitudeKm);
    const hTitanKm = Math.max(300.0, titanFlybyAltitudeKm);

    const AU_KM = 1.495978707e8;
    const muSun = 1.32712440018e11; // km^3/s^2
    const muMars = 42828.37; // km^3/s^2
    const muTitan = 8978.1; // km^3/s^2
    const rMarsKm = 3389.5;
    const rTitanKm = 2574.7;

    const rMarsAU = 1.52368;
    const rSaturnAU = 9.5388;
    const rMarsDistKm = rMarsAU * AU_KM;
    const rSaturnDistKm = rSaturnAU * AU_KM;

    // Transfer ellipse semi-major axis
    const aTransferAU = (rMarsAU + rSaturnAU) / 2.0;
    const aTransferKm = aTransferAU * AU_KM;

    // Time of flight (seconds, days, years)
    const tofSec = Math.PI * Math.sqrt(Math.pow(aTransferKm, 3.0) / muSun);
    const tofDays = tofSec / 86400.0;
    const tofYears = tofDays / 365.25;

    // Heliocentric speeds
    const vMarsKmS = Math.sqrt(muSun / rMarsDistKm);
    const vPeriTransferKmS = Math.sqrt(muSun * ((2.0 / rMarsDistKm) - (1.0 / aTransferKm)));
    const vInfDepKmS = vPeriTransferKmS - vMarsKmS;

    // Trans-Saturn Injection (TSI) Delta-V from Mars parking orbit
    const rParkKm = rMarsKm + hpKm;
    const vParkCircKmS = Math.sqrt(muMars / rParkKm);
    const vTransDepHypKmS = Math.sqrt(Math.pow(vInfDepKmS, 2.0) + (2.0 * muMars / rParkKm));
    const dvTsiKmS = vTransDepHypKmS - vParkCircKmS;

    // Arrival speed at Saturn
    const vSaturnCircKmS = Math.sqrt(muSun / rSaturnDistKm);
    const vApoTransferKmS = Math.sqrt(muSun * ((2.0 / rSaturnDistKm) - (1.0 / aTransferKm)));
    const vInfArrKmS = Math.abs(vSaturnCircKmS - vApoTransferKmS);

    // Titan gravity assist Delta-V boost
    const rpTitanKm = rTitanKm + hTitanKm;
    const vInfTitan = 5.5; // km/s typical relative approach
    const denom = 1.0 + (rpTitanKm * Math.pow(vInfTitan, 2.0)) / muTitan;
    const sinHalfDelta = 1.0 / denom;
    const dvTitanKmS = 2.0 * vInfTitan * sinHalfDelta;

    return {
      departurePlanet: 'Mars',
      destinationPlanet: 'Saturn',
      transferSemiMajorAxisAU: parseFloat(aTransferAU.toFixed(2)),
      timeOfFlightYears: parseFloat(tofYears.toFixed(2)),
      timeOfFlightDays: parseFloat(tofDays.toFixed(1)),
      transSaturnInjectionDeltaVKmS: parseFloat(dvTsiKmS.toFixed(3)),
      saturnArrivalHyperbolicExcessKmS: parseFloat(vInfArrKmS.toFixed(3)),
      titanGravityAssistDeltaVKmS: parseFloat(dvTitanKmS.toFixed(3)),
      outerSystemMissionContext: `Mars-to-Saturn Transfer (TOF ${tofYears.toFixed(1)} Years, ${dvTsiKmS.toFixed(2)} km/s TSI Delta-V, ${dvTitanKmS.toFixed(2)} km/s Titan Gravity Assist)`
    };
  }

  /**
   * Calculate Mars co-orbital asteroid / Trojan continuous low-thrust Gravity Tractor towing deflection and b-plane trajectory displacement.
   * F_g = G * M_ast * m_sc / d_standoff^2
   * Delta_v_ast = a_ast * t_tow
   * Delta_b = 3 * Delta_v_ast * t_lead
   * Reference: Lu & Love (2005), Schweickart et al. (2006), Wie (2008) for Planetary Defense and Asteroid Orbit Trimming.
   * @param {number} [asteroidDiameterMeters=150.0] - Target asteroid spherical equivalent diameter in meters (10 to 1000 m)
   * @param {number} [asteroidDensityKgM3=2200.0] - Bulk asteroid density in kg/m^3 (1200 to 4000 kg/m^3)
   * @param {number} [spacecraftMassKg=2000.0] - Gravity tractor spacecraft mass in kg (500 to 10000 kg)
   * @param {number} [standoffDistanceMeters=120.0] - Spacecraft hover standoff distance from asteroid center in meters (50 to 500 m)
   * @param {number} [towDurationYears=3.0] - Active gravity tractor towing duration in years (0.5 to 10 years)
   * @param {number} [leadTimeToEncounterYears=10.0] - Orbital propagation lead time before keyhole encounter in years (1 to 50 years)
   * @returns {{asteroidMassKg: number, gravitationalTowingForceMicroN: number, cumulativeDeltaVMMS: number, bPlaneDisplacementKm: number, planetaryDefenseContext: string}}
   */
  static computeMartianAsteroidGravityTractorDeflection(asteroidDiameterMeters = 150.0, asteroidDensityKgM3 = 2200.0, spacecraftMassKg = 2000.0, standoffDistanceMeters = 120.0, towDurationYears = 3.0, leadTimeToEncounterYears = 10.0) {
    const D = Math.max(5.0, asteroidDiameterMeters);
    const rho = Math.max(1000.0, asteroidDensityKgM3);
    const mSc = Math.max(100.0, spacecraftMassKg);
    const dStandoff = Math.max(D / 2.0 + 10.0, standoffDistanceMeters);
    const tTowYrs = Math.max(0.1, towDurationYears);
    const tLeadYrs = Math.max(0.5, leadTimeToEncounterYears);

    const G = 6.67430e-11;
    const Rast = D / 2.0;

    // Asteroid mass (kg)
    const Mast = (4.0 / 3.0) * Math.PI * Math.pow(Rast, 3.0) * rho;

    // Mutual gravitational force (N and micro-N)
    const FgN = (G * Mast * mSc) / Math.pow(dStandoff, 2.0);
    const FgMicroN = FgN * 1e6;

    // Asteroid acceleration (m/s^2)
    const aAst = (G * mSc) / Math.pow(dStandoff, 2.0);

    // Cumulative velocity deflection (mm/s)
    const tTowSec = tTowYrs * 3.15576e7;
    const dvAstMS = aAst * tTowSec;
    const dvAstMMS = dvAstMS * 1000.0;

    // B-plane displacement (km)
    const tLeadSec = tLeadYrs * 3.15576e7;
    const deltaBMeters = 3.0 * dvAstMS * tLeadSec;
    const deltaBKm = deltaBMeters / 1000.0;

    return {
      asteroidMassKg: parseFloat(Mast.toExponential(4)),
      gravitationalTowingForceMicroN: parseFloat(FgMicroN.toFixed(2)),
      cumulativeDeltaVMMS: parseFloat(dvAstMMS.toFixed(3)),
      bPlaneDisplacementKm: parseFloat(deltaBKm.toFixed(1)),
      planetaryDefenseContext: `Gravity Tractor Deflection (${dvAstMMS.toFixed(2)} mm/s Delta-V -> ${deltaBKm.toFixed(0)} km B-Plane Shift over ${tLeadYrs.toFixed(0)} Yrs)`
    };
  }

  /**
   * Calculate Mars-to-Jupiter interplanetary trajectory and Jupiter gravity assist slingshot achieving heliocentric hyperbolic Interstellar Escape velocity.
   * a_t = ( r_mars + r_jupiter ) / 2
   * TOF = pi * sqrt( a_t^3 / mu_sun )
   * delta = 2 * arcsin( 1 / ( 1 + r_p * v_inf^2 / mu_j ) )
   * Reference: Bate et al. (1971), Flandro (1966), Curtis (2013) for Interstellar Probe / Outer Solar System Escape Architecture.
   * @param {number} [jupiterClosestApproachRj=2.0] - Jupiter closest approach periapsis in Jovian radii (1.2 to 10.0 R_j)
   * @param {number} [marsParkingAltitudeKm=300.0] - Mars departure low orbit altitude in km (150 to 1000 km)
   * @returns {{departurePlanet: string, assistPlanet: string, timeOfFlightToJupiterYears: number, transJupiterInjectionDeltaVKmS: number, jupiterHyperbolicBendingAngleDeg: number, postSlingshotHeliocentricSpeedKmS: number, interstellarEscapeRateAUYear: number, interstellarMissionContext: string}}
   */
  static computeMarsJupiterInterstellarEscapeTrajectory(jupiterClosestApproachRj = 2.0, marsParkingAltitudeKm = 300.0) {
    const rjMult = Math.max(1.15, jupiterClosestApproachRj);
    const hpKm = Math.max(150.0, marsParkingAltitudeKm);

    const AU_KM = 1.495978707e8;
    const muSun = 1.32712440018e11;
    const muMars = 42828.37;
    const muJupiter = 1.26686534e8; // km^3/s^2
    const rMarsKm = 3389.5;
    const rJupiterKm = 71492.0;

    const rMarsAU = 1.52368;
    const rJupiterAU = 5.2044;
    const rMarsDistKm = rMarsAU * AU_KM;
    const rJupiterDistKm = rJupiterAU * AU_KM;

    // Transfer ellipse semi-major axis
    const aTransferAU = (rMarsAU + rJupiterAU) / 2.0;
    const aTransferKm = aTransferAU * AU_KM;

    // Time of flight to Jupiter
    const tofSec = Math.PI * Math.sqrt(Math.pow(aTransferKm, 3.0) / muSun);
    const tofYears = tofSec / 3.15576e7;

    // Speeds at Mars departure
    const vMarsKmS = Math.sqrt(muSun / rMarsDistKm);
    const vPeriTransferKmS = Math.sqrt(muSun * ((2.0 / rMarsDistKm) - (1.0 / aTransferKm)));
    const vInfDepKmS = vPeriTransferKmS - vMarsKmS;

    // Trans-Jupiter Injection Delta-V
    const rParkKm = rMarsKm + hpKm;
    const vParkCircKmS = Math.sqrt(muMars / rParkKm);
    const vTransDepHypKmS = Math.sqrt(Math.pow(vInfDepKmS, 2.0) + (2.0 * muMars / rParkKm));
    const dvTjiKmS = vTransDepHypKmS - vParkCircKmS;

    // Speeds at Jupiter arrival
    const vJupiterCircKmS = Math.sqrt(muSun / rJupiterDistKm);
    const vApoTransferKmS = Math.sqrt(muSun * ((2.0 / rJupiterDistKm) - (1.0 / aTransferKm)));
    const vInfArrJupiterKmS = Math.abs(vJupiterCircKmS - vApoTransferKmS);

    // Jupiter hyperbolic turning angle
    const rpJupiterKm = rjMult * rJupiterKm;
    const denom = 1.0 + (rpJupiterKm * Math.pow(vInfArrJupiterKmS, 2.0)) / muJupiter;
    const deltaRad = 2.0 * Math.asin(1.0 / denom);
    const deltaDeg = (deltaRad * 180.0) / Math.PI;

    // Heliocentric post-slingshot asymptotic escape speed
    const vHelioPostKmS = Math.sqrt(Math.pow(vJupiterCircKmS, 2.0) + Math.pow(vInfArrJupiterKmS, 2.0) + (2.0 * vJupiterCircKmS * vInfArrJupiterKmS * Math.cos(deltaRad / 2.0)));
    const auYearRate = (vHelioPostKmS * 3.15576e7) / AU_KM;

    return {
      departurePlanet: 'Mars',
      assistPlanet: 'Jupiter',
      timeOfFlightToJupiterYears: parseFloat(tofYears.toFixed(2)),
      transJupiterInjectionDeltaVKmS: parseFloat(dvTjiKmS.toFixed(3)),
      jupiterHyperbolicBendingAngleDeg: parseFloat(deltaDeg.toFixed(1)),
      postSlingshotHeliocentricSpeedKmS: parseFloat(vHelioPostKmS.toFixed(2)),
      interstellarEscapeRateAUYear: parseFloat(auYearRate.toFixed(2)),
      interstellarMissionContext: `Mars-Jupiter Interstellar Escape (${vHelioPostKmS.toFixed(1)} km/s Helio Speed, ${auYearRate.toFixed(2)} AU/yr Solar System Escape)`
    };
  }

  /**
   * Calculate Martian atmospheric tether momentum exchange, tidal gravity-gradient tension, upper atmospheric drag braking, and non-propulsive probe deorbit.
   * T_tide = 3 * mu_mars * m_probe * L_tether / r_p^3
   * F_drag = 0.5 * rho_atm * v_p^2 * C_D * A_probe
   * Delta_V_deorbit = ( v_p / r_p ) * L_tether
   * Reference: Moravec (1977), Bekey (2003), Cartmell & McKenzie (2008) for Space Tether Dynamics & Mars Aerocapture Tethers.
   * @param {number} [tetherLengthKm=50.0] - High-strength carbon nanotube / Kevlar tether length in km (5 to 100 km)
   * @param {number} [tetherTipProbeMassKg=500.0] - Atmospheric entry sub-probe mass in kg (50 to 2000 kg)
   * @param {number} [mothershipMassKg=2500.0] - Main orbiter bus mass in kg (500 to 10000 kg)
   * @param {number} [orbiterPeriapsisAltitudeKm=150.0] - Mothership orbit periapsis altitude in km (120 to 300 km)
   * @returns {{tetherLengthKm: number, probeDippingAltitudeKm: number, gravityGradientTensionN: number, peakAerodynamicDragN: number, passAerobrakingDeltaVMS: number, nonPropulsiveDeorbitDeltaVMS: number, tetherMechanicsContext: string}}
   */
  static computeMartianAtmosphericTetherMomentumExchange(tetherLengthKm = 50.0, tetherTipProbeMassKg = 500.0, mothershipMassKg = 2500.0, orbiterPeriapsisAltitudeKm = 150.0) {
    const LtetherKm = Math.max(1.0, tetherLengthKm);
    const mProbe = Math.max(10.0, tetherTipProbeMassKg);
    const mShip = Math.max(100.0, mothershipMassKg);
    const hpKm = Math.max(110.0, orbiterPeriapsisAltitudeKm);

    const muMars = 42828.37 * 1e9; // m^3/s^2
    const rMarsM = 3389.5 * 1000.0;
    const rpM = rMarsM + (hpKm * 1000.0);
    const LtetherM = LtetherKm * 1000.0;

    // Atmospheric dip altitude of the probe tip
    const hTipKm = Math.max(50.0, hpKm - LtetherKm);

    // Gravity-gradient tidal tension (N)
    const TtideN = (3.0 * muMars * mProbe * LtetherM) / Math.pow(rpM, 3.0);

    // Periapsis orbital speed (m/s)
    const vpMS = Math.sqrt(muMars / rpM);

    // Atmospheric density at tip altitude (scale height ~11.1 km)
    const rho0 = 0.020; // kg/m^3 surface density
    const rhoAtm = rho0 * Math.exp(- (hTipKm * 1000.0) / 11100.0);

    // Aerodynamic drag force on probe (N) with Area = 2.0 m^2, Cd = 2.2
    const Aprobe = 2.0;
    const Cd = 2.2;
    const qDynPa = 0.5 * rhoAtm * Math.pow(vpMS, 2.0);
    const FdragN = qDynPa * Cd * Aprobe;

    // Aerobraking Delta-V per pass on combined system (m/s) with 450s atmospheric dip duration
    const mTotal = mProbe + mShip;
    const tPassSec = 450.0;
    const dvAeroPassMS = (FdragN * tPassSec) / mTotal;

    // Non-propulsive deorbit kick delivered to probe upon nadir release (m/s)
    const omegaOrb = vpMS / rpM;
    const dvDeorbitMS = omegaOrb * LtetherM;

    return {
      tetherLengthKm: parseFloat(LtetherKm.toFixed(1)),
      probeDippingAltitudeKm: parseFloat(hTipKm.toFixed(1)),
      gravityGradientTensionN: parseFloat(TtideN.toFixed(2)),
      peakAerodynamicDragN: parseFloat(FdragN.toFixed(2)),
      passAerobrakingDeltaVMS: parseFloat(dvAeroPassMS.toFixed(3)),
      nonPropulsiveDeorbitDeltaVMS: parseFloat(dvDeorbitMS.toFixed(2)),
      tetherMechanicsContext: `Atmospheric Tether (${LtetherKm.toFixed(0)} km Tether, ${hTipKm.toFixed(0)} km Dip, ${dvDeorbitMS.toFixed(1)} m/s Non-Propulsive Release Kick)`
    };
  }

  /**
   * Calculate hypersonic Martian entry / aerocapture TPS charring, in-depth pyrolysis, stagnation heat load, and surface ablation recession.
   * q_peak = k_SG * sqrt( rho_atm / R_nose ) * v_entry^3
   * Q_total = q_peak * Delta_t_pulse * 0.65
   * Delta_s_recession = Q_total / ( rho_tps * H_ablation )
   * Reference: Tauber et al. (1989), Laub & Venkatapathy (2003), Wright et al. (2006) for PICA & SLA-561V Thermal Protection Systems.
   * @param {number} [noseRadiusMeters=1.25] - Aeroshell spherical nose radius in meters (0.2 to 5.0 m)
   * @param {string} [heatShieldMaterial='PICA'] - Ablative material ('PICA', 'SLA-561V', or 'Carbon-Phenolic')
   * @param {number} [periapsisAltitudeKm=52.0] - Hypersonic corridor periapsis altitude in km (35 to 80 km)
   * @param {number} [entrySpeedKmS=5.8] - Hypersonic entry velocity in km/s (4.0 to 8.5 km/s)
   * @returns {{heatShieldMaterial: string, peakConvectiveHeatFluxKWm2: number, totalHeatLoadMJm2: number, surfaceAblationRecessionMm: number, indepthCharDepthMm: number, tpsAblationContext: string}}
   */
  static computeMarsAerobrakingTPSPyrolysisAndRecession(noseRadiusMeters = 1.25, heatShieldMaterial = 'PICA', periapsisAltitudeKm = 52.0, entrySpeedKmS = 5.8) {
    const Rn = Math.max(0.1, noseRadiusMeters);
    const matName = heatShieldMaterial.toUpperCase();
    const hpKm = Math.max(30.0, Math.min(100.0, periapsisAltitudeKm));
    const vEntry = Math.max(3000.0, entrySpeedKmS * 1000.0);

    const kSG = 1.90e-4; // W*s^3/(m^3.5*kg^0.5) for Martian CO2 atmosphere
    const rho0 = 0.020;
    const rhoAtm = rho0 * Math.exp(- (hpKm * 1000.0) / 11100.0);

    // Peak stagnation convective heat flux (W/m^2 and kW/m^2)
    const qPeakW = kSG * Math.sqrt(rhoAtm / Rn) * Math.pow(vEntry, 3.0);
    const qPeakKW = qPeakW / 1000.0;

    // Total integrated heat load (MJ/m^2) for 65s hypersonic heating pulse
    const tPulseSec = 65.0;
    const QloadJ = qPeakW * tPulseSec * 0.65;
    const QloadMJ = QloadJ / 1e6;

    // Material ablation parameters
    let rhoTps = 270.0; // kg/m^3 (PICA)
    let HablJ = 3.5e7; // J/kg (PICA)
    let label = 'PICA (Phenolic Impregnated Carbon Ablator)';

    if (matName.includes('SLA')) {
      rhoTps = 260.0;
      HablJ = 1.8e7;
      label = 'SLA-561V (Silicone Elastomeric Ablator)';
    } else if (matName.includes('CARBON')) {
      rhoTps = 1450.0;
      HablJ = 4.5e7;
      label = 'Carbon-Phenolic (High-Density High-Heat-Flux Ablator)';
    }

    // Surface ablation recession (mm)
    const deltaSM = QloadJ / (rhoTps * HablJ);
    const deltaSMm = deltaSM * 1000.0;

    // In-depth charring and pyrolysis front depth (mm)
    const deltaCharMm = deltaSMm * 3.2;

    return {
      heatShieldMaterial: label,
      peakConvectiveHeatFluxKWm2: parseFloat(qPeakKW.toFixed(1)),
      totalHeatLoadMJm2: parseFloat(QloadMJ.toFixed(2)),
      surfaceAblationRecessionMm: parseFloat(deltaSMm.toFixed(2)),
      indepthCharDepthMm: parseFloat(deltaCharMm.toFixed(2)),
      tpsAblationContext: `${label} TPS (${qPeakKW.toFixed(0)} kW/m^2 Peak Flux, ${deltaSMm.toFixed(1)} mm Ablation Recession, ${deltaCharMm.toFixed(1)} mm Char)`
    };
  }

  /**
   * Calculate Martian upper atmospheric internal gravity wave (IGW) buoyancy frequency, along-track density perturbations, and spacecraft aerobraking drag oscillations.
   * N_BV = sqrt( ( g / T ) * ( dT/dz + Gamma_d ) )
   * tau_BV = 2 * pi / N_BV
   * T_encounter = lambda_x / v_orb
   * Reference: Creasey et al. (2006), Forbes et al. (2006), Yiğit et al. (2015), Terada et al. (2017) for MAVEN / MRO / TGO Thermospheric Gravity Waves.
   * @param {number} [baseAltitudeKm=140.0] - Thermospheric passage altitude in km (110 to 220 km)
   * @param {number} [horizontalWavelengthKm=250.0] - Gravity wave horizontal wavelength in km (50 to 1000 km)
   * @param {number} [waveAmplitudePct=25.0] - Relative density wave amplitude percentage (5 to 60%)
   * @param {number} [orbiterSpeedKmS=4.20] - Spacecraft orbital horizontal velocity in km/s (3.0 to 5.0 km/s)
   * @returns {{baseAltitudeKm: number, ambientDensityKgM3: number, bruntVaisalaFrequencyMradS: number, buoyancyPeriodMinutes: number, alongTrackEncounterPeriodSec: number, peakDensityPerturbationPct: number, gravityWaveAerobrakingContext: string}}
   */
  static computeMartianUpperAtmosphericGravityWavesAndDensityPerturbations(baseAltitudeKm = 140.0, horizontalWavelengthKm = 250.0, waveAmplitudePct = 25.0, orbiterSpeedKmS = 4.20) {
    const zKm = Math.max(90.0, Math.min(300.0, baseAltitudeKm));
    const lambdaX = Math.max(10.0, horizontalWavelengthKm);
    const ampPct = Math.max(1.0, Math.min(90.0, waveAmplitudePct));
    const vOrbKmS = Math.max(1.0, orbiterSpeedKmS);

    const gMars = 3.72076;
    const cpCO2 = 830.0;
    const gammaD = gMars / cpCO2; // ~0.00448 K/m

    // Ambient thermospheric temperature & lapse rate at altitude
    const Tatm = 160.0; // K
    const dIdZ = 0.0012; // K/m positive lapse rate in thermosphere

    // Brunt-Vaisala buoyancy frequency (rad/s and mrad/s)
    const NbvRadS = Math.sqrt((gMars / Tatm) * (dIdZ + gammaD));
    const NbvMradS = NbvRadS * 1000.0;

    // Buoyancy period (minutes)
    const tauBvMin = (2.0 * Math.PI / NbvRadS) / 60.0;

    // Background atmospheric density (kg/m^3)
    const rho0 = 0.020;
    const rhoBase = rho0 * Math.exp(- (zKm * 1000.0) / 11100.0);

    // Apparent along-track encounter oscillation period (seconds)
    const tEncSec = lambdaX / vOrbKmS;

    return {
      baseAltitudeKm: parseFloat(zKm.toFixed(1)),
      ambientDensityKgM3: parseFloat(rhoBase.toExponential(3)),
      bruntVaisalaFrequencyMradS: parseFloat(NbvMradS.toFixed(2)),
      buoyancyPeriodMinutes: parseFloat(tauBvMin.toFixed(2)),
      alongTrackEncounterPeriodSec: parseFloat(tEncSec.toFixed(1)),
      peakDensityPerturbationPct: parseFloat(ampPct.toFixed(1)),
      gravityWaveAerobrakingContext: `Thermospheric Gravity Wave (${ampPct.toFixed(0)}% Density Wave at ${zKm.toFixed(0)} km, ${tEncSec.toFixed(1)}s Spacecraft Drag Oscillation)`
    };
  }

  /**
   * Calculate continuous low-thrust ion electric propulsion spiral descent trajectory from high Mars orbit to Phobos co-orbital rendezvous.
   * Delta_V = | sqrt( mu / r_initial ) - sqrt( mu / r_phobos ) |
   * m_propellant = m_0 * ( 1 - exp( -Delta_V / ( g_0 * I_sp ) ) )
   * t_spiral = m_propellant / ( T_thrust / ( g_0 * I_sp ) )
   * Reference: Edelbaum (1961), Petropoulos (2004), MMX JAXA Mission Design (2020) for Low-Thrust Martian Moon Proximity Operations.
   * @param {number} [initialAltitudeKm=17032.0] - Departure orbit altitude above Mars in km (1000 to 40000 km)
   * @param {number} [ionThrustMilliN=150.0] - Continuous electric thruster force in milli-Newtons (10 to 1000 mN)
   * @param {number} [spacecraftMassKg=1200.0] - Initial wet spacecraft mass in kg (200 to 5000 kg)
   * @param {number} [ispSeconds=3200.0] - Ion engine specific impulse in seconds (1500 to 5000 s)
   * @returns {{departureRadiusKm: number, phobosRadiusKm: number, edelbaumDeltaVMMS: number, xenonPropellantConsumedKg: number, spiralDurationDays: number, totalSpiralRevolutions: number, lowThrustMissionContext: string}}
   */
  static computeMarsPhobosLowThrustSpiralDescentTrajectory(initialAltitudeKm = 17032.0, ionThrustMilliN = 150.0, spacecraftMassKg = 1200.0, ispSeconds = 3200.0) {
    const hInitKm = Math.max(1000.0, initialAltitudeKm);
    const TThrustN = Math.max(0.005, ionThrustMilliN / 1000.0);
    const m0Kg = Math.max(50.0, spacecraftMassKg);
    const Isp = Math.max(500.0, ispSeconds);

    const muMars = 42828.37; // km^3/s^2
    const rMarsKm = 3389.5;
    const r1Km = rMarsKm + hInitKm;
    const rPhobosKm = 9376.0; // Phobos semi-major axis

    const g0 = 9.80665; // m/s^2
    const cMS = Isp * g0; // effective exhaust velocity (m/s)

    // Orbital speeds (m/s)
    const v1MS = Math.sqrt((muMars * 1e9) / (r1Km * 1000.0));
    const v2MS = Math.sqrt((muMars * 1e9) / (rPhobosKm * 1000.0));

    // Edelbaum velocity increment (m/s)
    const dvMS = Math.abs(v2MS - v1MS);

    // Propellant mass consumed (kg)
    const mXeKg = m0Kg * (1.0 - Math.exp(- dvMS / cMS));

    // Mass flow rate (kg/s) and spiral duration (days)
    const mDotKgS = TThrustN / cMS;
    const tSpiralSec = mXeKg / mDotKgS;
    const tSpiralDays = tSpiralSec / 86400.0;

    // Mean orbital period and revolution count
    const rMeanM = ((r1Km + rPhobosKm) / 2.0) * 1000.0;
    const pMeanSec = 2.0 * Math.PI * Math.sqrt(Math.pow(rMeanM, 3.0) / (muMars * 1e9));
    const nRevs = tSpiralSec / pMeanSec;

    return {
      departureRadiusKm: parseFloat(r1Km.toFixed(1)),
      phobosRadiusKm: parseFloat(rPhobosKm.toFixed(1)),
      edelbaumDeltaVMMS: parseFloat(dvMS.toFixed(1)),
      xenonPropellantConsumedKg: parseFloat(mXeKg.toFixed(2)),
      spiralDurationDays: parseFloat(tSpiralDays.toFixed(1)),
      totalSpiralRevolutions: parseFloat(nRevs.toFixed(1)),
      lowThrustMissionContext: `Low-Thrust Spiral Descent (${dvMS.toFixed(0)} m/s Delta-V, ${mXeKg.toFixed(1)} kg Xe over ${tSpiralDays.toFixed(0)} Days / ${nRevs.toFixed(0)} Revs)`
    };
  }

  /**
   * Calculate Mars-to-Venus inward interplanetary transfer, Venus gravity assist slingshot deflection, and perihelion reduction toward Mercury.
   * a_t = ( r_mars + r_venus ) / 2
   * TOF = pi * sqrt( a_t^3 / mu_sun )
   * delta_V = 2 * arcsin( 1 / ( 1 + r_p * v_inf^2 / mu_venus ) )
   * Reference: Bate et al. (1971), Curtis (2013), Broucke (1988) for Inner Solar System Gravity Assist Trajectories.
   * @param {number} [venusFlybyAltitudeKm=300.0] - Venus closest approach altitude in km (200 to 5000 km)
   * @param {number} [marsParkingAltitudeKm=300.0] - Mars departure parking orbit altitude in km (150 to 1000 km)
   * @returns {{departurePlanet: string, assistPlanet: string, timeOfFlightToVenusDays: number, transVenusInjectionDeltaVKmS: number, venusHyperbolicExcessKmS: number, venusBendingAngleDeg: number, gravityAssistDeltaVKmS: number, inwardTransferContext: string}}
   */
  static computeMarsVenusMercuryInwardTransferTrajectory(venusFlybyAltitudeKm = 300.0, marsParkingAltitudeKm = 300.0) {
    const hpVenusKm = Math.max(150.0, venusFlybyAltitudeKm);
    const hpMarsKm = Math.max(150.0, marsParkingAltitudeKm);

    const AU_KM = 1.495978707e8;
    const muSun = 1.32712440018e11;
    const muMars = 42828.37;
    const muVenus = 324858.6; // km^3/s^2
    const rMarsKm = 3389.5;
    const rVenusKm = 6051.8;

    const rMarsAU = 1.52368;
    const rVenusAU = 0.72333;
    const rMarsDistKm = rMarsAU * AU_KM;
    const rVenusDistKm = rVenusAU * AU_KM;

    // Transfer ellipse semi-major axis
    const aTransferAU = (rMarsAU + rVenusAU) / 2.0;
    const aTransferKm = aTransferAU * AU_KM;

    // Time of flight to Venus (days)
    const tofSec = Math.PI * Math.sqrt(Math.pow(aTransferKm, 3.0) / muSun);
    const tofDays = tofSec / 86400.0;

    // Speeds at Mars departure
    const vMarsKmS = Math.sqrt(muSun / rMarsDistKm);
    const vApoTransferKmS = Math.sqrt(muSun * ((2.0 / rMarsDistKm) - (1.0 / aTransferKm)));
    const vInfDepKmS = Math.abs(vMarsKmS - vApoTransferKmS);

    // Trans-Venus Injection Delta-V
    const rParkKm = rMarsKm + hpMarsKm;
    const vParkCircKmS = Math.sqrt(muMars / rParkKm);
    const vTransDepHypKmS = Math.sqrt(Math.pow(vInfDepKmS, 2.0) + (2.0 * muMars / rParkKm));
    const dvTviKmS = vTransDepHypKmS - vParkCircKmS;

    // Speeds at Venus arrival
    const vVenusCircKmS = Math.sqrt(muSun / rVenusDistKm);
    const vPeriTransferKmS = Math.sqrt(muSun * ((2.0 / rVenusDistKm) - (1.0 / aTransferKm)));
    const vInfArrVenusKmS = Math.abs(vPeriTransferKmS - vVenusCircKmS);

    // Venus hyperbolic turning angle
    const rpVenusKm = rVenusKm + hpVenusKm;
    const denom = 1.0 + (rpVenusKm * Math.pow(vInfArrVenusKmS, 2.0)) / muVenus;
    const deltaRad = 2.0 * Math.asin(1.0 / denom);
    const deltaDeg = (deltaRad * 180.0) / Math.PI;

    // Effective gravity assist Delta-V
    const dvAssistKmS = 2.0 * vInfArrVenusKmS * Math.sin(deltaRad / 2.0);

    return {
      departurePlanet: 'Mars',
      assistPlanet: 'Venus',
      timeOfFlightToVenusDays: parseFloat(tofDays.toFixed(1)),
      transVenusInjectionDeltaVKmS: parseFloat(dvTviKmS.toFixed(3)),
      venusHyperbolicExcessKmS: parseFloat(vInfArrVenusKmS.toFixed(3)),
      venusBendingAngleDeg: parseFloat(deltaDeg.toFixed(1)),
      gravityAssistDeltaVKmS: parseFloat(dvAssistKmS.toFixed(3)),
      inwardTransferContext: `Mars-Venus Inward Transfer (${tofDays.toFixed(0)} Days TOF, ${dvTviKmS.toFixed(2)} km/s TVI, ${dvAssistKmS.toFixed(2)} km/s Venus Gravity Assist)`
    };
  }

  /**
   * Calculate Mars-to-Main Asteroid Belt (Ceres / Vesta / Pallas) Hohmann transfer trajectory, Trans-Asteroid Injection (TAI), and rendezvous Delta-V.
   * a_t = ( r_mars + r_asteroid ) / 2
   * TOF = pi * sqrt( a_t^3 / mu_sun )
   * Delta_V_TAI = sqrt( v_inf_dep^2 + 2 * mu_mars / r_park ) - sqrt( mu_mars / r_park )
   * Reference: Russell & Raymond (2011), Bate et al. (1971) for Dawn Mission Main Asteroid Belt Rendezvous.
   * @param {number} [targetAsteroidSemiMajorAxisAU=2.7675] - Target asteroid distance from Sun in AU (2.0 to 3.5 AU, default 2.7675 AU for Ceres)
   * @param {number} [marsParkingAltitudeKm=300.0] - Mars departure parking orbit altitude in km (150 to 1000 km)
   * @returns {{targetBody: string, asteroidDistanceAU: number, timeOfFlightDays: number, timeOfFlightYears: number, transAsteroidInjectionDeltaVKmS: number, rendezvousDeltaVKmS: number, totalMissionDeltaVKmS: number, asteroidTransferContext: string}}
   */
  static computeMarsToMainBeltAsteroidHohmannTransfer(targetAsteroidSemiMajorAxisAU = 2.7675, marsParkingAltitudeKm = 300.0) {
    const rAstAU = Math.max(1.8, Math.min(4.5, targetAsteroidSemiMajorAxisAU));
    const hpMarsKm = Math.max(150.0, marsParkingAltitudeKm);

    const AU_KM = 1.495978707e8;
    const muSun = 1.32712440018e11;
    const muMars = 42828.37;
    const rMarsKm = 3389.5;

    const rMarsAU = 1.52368;
    const rMarsDistKm = rMarsAU * AU_KM;
    const rAstDistKm = rAstAU * AU_KM;

    // Transfer ellipse
    const aTransferAU = (rMarsAU + rAstAU) / 2.0;
    const aTransferKm = aTransferAU * AU_KM;

    // Time of flight (days & years)
    const tofSec = Math.PI * Math.sqrt(Math.pow(aTransferKm, 3.0) / muSun);
    const tofDays = tofSec / 86400.0;
    const tofYrs = tofDays / 365.25;

    // Speeds at Mars departure
    const vMarsKmS = Math.sqrt(muSun / rMarsDistKm);
    const vPeriTransferKmS = Math.sqrt(muSun * ((2.0 / rMarsDistKm) - (1.0 / aTransferKm)));
    const vInfDepKmS = Math.abs(vPeriTransferKmS - vMarsKmS);

    // Trans-Asteroid Injection Delta-V
    const rParkKm = rMarsKm + hpMarsKm;
    const vParkCircKmS = Math.sqrt(muMars / rParkKm);
    const vTransDepHypKmS = Math.sqrt(Math.pow(vInfDepKmS, 2.0) + (2.0 * muMars / rParkKm));
    const dvTaiKmS = vTransDepHypKmS - vParkCircKmS;

    // Speeds at Asteroid arrival
    const vAstCircKmS = Math.sqrt(muSun / rAstDistKm);
    const vApoTransferKmS = Math.sqrt(muSun * ((2.0 / rAstDistKm) - (1.0 / aTransferKm)));
    const dvRendKmS = Math.abs(vAstCircKmS - vApoTransferKmS);

    const dvTotKmS = dvTaiKmS + dvRendKmS;

    let targetName = 'Main Belt Asteroid';
    if (Math.abs(rAstAU - 2.7675) < 0.05) targetName = 'Dwarf Planet Ceres';
    else if (Math.abs(rAstAU - 2.3618) < 0.05) targetName = 'Proto-Planet Vesta';

    return {
      targetBody: targetName,
      asteroidDistanceAU: parseFloat(rAstAU.toFixed(4)),
      timeOfFlightDays: parseFloat(tofDays.toFixed(1)),
      timeOfFlightYears: parseFloat(tofYrs.toFixed(2)),
      transAsteroidInjectionDeltaVKmS: parseFloat(dvTaiKmS.toFixed(3)),
      rendezvousDeltaVKmS: parseFloat(dvRendKmS.toFixed(3)),
      totalMissionDeltaVKmS: parseFloat(dvTotKmS.toFixed(3)),
      asteroidTransferContext: `Mars to ${targetName} Transfer (${tofDays.toFixed(0)} Days TOF, ${dvTaiKmS.toFixed(2)} km/s TAI, ${dvRendKmS.toFixed(2)} km/s Rendezvous)`
    };
  }

  /**
   * Calculate Mars-to-Saturn / Titan deep solar system Hohmann transfer trajectory, Trans-Saturn Injection (TSI), and hyperbolic arrival excess.
   * a_t = ( r_mars + r_saturn ) / 2
   * TOF = pi * sqrt( a_t^3 / mu_sun )
   * Delta_V_TSI = sqrt( v_inf_dep^2 + 2 * mu_mars / r_park ) - sqrt( mu_mars / r_park )
   * Reference: Bate et al. (1971), Curtis (2013) for Outer Solar System Interplanetary Transfers.
   * @param {number} [saturnSemiMajorAxisAU=9.5826] - Saturn distance from Sun in AU (8.0 to 11.0 AU)
   * @param {number} [marsParkingAltitudeKm=300.0] - Mars departure parking orbit altitude in km (150 to 1000 km)
   * @returns {{targetPlanet: string, saturnDistanceAU: number, timeOfFlightDays: number, timeOfFlightYears: number, transSaturnInjectionDeltaVKmS: number, saturnHyperbolicExcessKmS: number, outerTransferContext: string}}
   */
  static computeMarsToSaturnTitanTransferTrajectory(saturnSemiMajorAxisAU = 9.5826, marsParkingAltitudeKm = 300.0) {
    const rSatAU = Math.max(8.0, Math.min(12.0, saturnSemiMajorAxisAU));
    const hpMarsKm = Math.max(150.0, marsParkingAltitudeKm);

    const AU_KM = 1.495978707e8;
    const muSun = 1.32712440018e11;
    const muMars = 42828.37;
    const rMarsKm = 3389.5;

    const rMarsAU = 1.52368;
    const rMarsDistKm = rMarsAU * AU_KM;
    const rSatDistKm = rSatAU * AU_KM;

    // Transfer ellipse
    const aTransferAU = (rMarsAU + rSatAU) / 2.0;
    const aTransferKm = aTransferAU * AU_KM;

    // Time of flight (days & years)
    const tofSec = Math.PI * Math.sqrt(Math.pow(aTransferKm, 3.0) / muSun);
    const tofDays = tofSec / 86400.0;
    const tofYrs = tofDays / 365.25;

    // Speeds at Mars departure
    const vMarsKmS = Math.sqrt(muSun / rMarsDistKm);
    const vPeriTransferKmS = Math.sqrt(muSun * ((2.0 / rMarsDistKm) - (1.0 / aTransferKm)));
    const vInfDepKmS = Math.abs(vPeriTransferKmS - vMarsKmS);

    // Trans-Saturn Injection Delta-V
    const rParkKm = rMarsKm + hpMarsKm;
    const vParkCircKmS = Math.sqrt(muMars / rParkKm);
    const vTransDepHypKmS = Math.sqrt(Math.pow(vInfDepKmS, 2.0) + (2.0 * muMars / rParkKm));
    const dvTsiKmS = vTransDepHypKmS - vParkCircKmS;

    // Speeds at Saturn arrival
    const vSatCircKmS = Math.sqrt(muSun / rSatDistKm);
    const vApoTransferKmS = Math.sqrt(muSun * ((2.0 / rSatDistKm) - (1.0 / aTransferKm)));
    const vInfArrSatKmS = Math.abs(vSatCircKmS - vApoTransferKmS);

    return {
      targetPlanet: 'Saturn / Titan System',
      saturnDistanceAU: parseFloat(rSatAU.toFixed(4)),
      timeOfFlightDays: parseFloat(tofDays.toFixed(1)),
      timeOfFlightYears: parseFloat(tofYrs.toFixed(2)),
      transSaturnInjectionDeltaVKmS: parseFloat(dvTsiKmS.toFixed(3)),
      saturnHyperbolicExcessKmS: parseFloat(vInfArrSatKmS.toFixed(3)),
      outerTransferContext: `Mars to Saturn Transfer (${tofYrs.toFixed(1)} yr TOF, ${dvTsiKmS.toFixed(2)} km/s TSI, ${vInfArrSatKmS.toFixed(2)} km/s Saturn Arrival Excess)`
    };
  }

  /**
   * Calculate Mars-to-Ice Giant (Uranus / Neptune) outer solar system transfer trajectory, Trans-Ice-Giant Injection, and arrival excess.
   * a_t = ( r_mars + r_ice_giant ) / 2
   * TOF = pi * sqrt( a_t^3 / mu_sun )
   * Reference: Bate et al. (1971), Curtis (2013) for Ice Giant Interplanetary Exploration.
   * @param {string} [targetPlanetName='Uranus'] - Target Ice Giant ('Uranus' or 'Neptune')
   * @param {number} [targetSemiMajorAxisAU=19.191] - Target planet orbital distance in AU (15.0 to 35.0 AU)
   * @param {number} [marsParkingAltitudeKm=300.0] - Mars departure parking orbit altitude in km (150 to 1000 km)
   * @returns {{targetPlanet: string, targetDistanceAU: number, timeOfFlightDays: number, timeOfFlightYears: number, transIceGiantInjectionDeltaVKmS: number, iceGiantHyperbolicExcessKmS: number, iceGiantTransferContext: string}}
   */
  static computeMarsToIceGiantTransferTrajectory(targetPlanetName = 'Uranus', targetSemiMajorAxisAU = 19.191, marsParkingAltitudeKm = 300.0) {
    const rTargAU = Math.max(15.0, Math.min(35.0, targetSemiMajorAxisAU));
    const hpMarsKm = Math.max(150.0, marsParkingAltitudeKm);

    const AU_KM = 1.495978707e8;
    const muSun = 1.32712440018e11;
    const muMars = 42828.37;
    const rMarsKm = 3389.5;

    const rMarsAU = 1.52368;
    const rMarsDistKm = rMarsAU * AU_KM;
    const rTargDistKm = rTargAU * AU_KM;

    // Transfer ellipse
    const aTransferAU = (rMarsAU + rTargAU) / 2.0;
    const aTransferKm = aTransferAU * AU_KM;

    // Time of flight (days & years)
    const tofSec = Math.PI * Math.sqrt(Math.pow(aTransferKm, 3.0) / muSun);
    const tofDays = tofSec / 86400.0;
    const tofYrs = tofDays / 365.25;

    // Speeds at Mars departure
    const vMarsKmS = Math.sqrt(muSun / rMarsDistKm);
    const vPeriTransferKmS = Math.sqrt(muSun * ((2.0 / rMarsDistKm) - (1.0 / aTransferKm)));
    const vInfDepKmS = Math.abs(vPeriTransferKmS - vMarsKmS);

    // Trans-Ice-Giant Injection Delta-V
    const rParkKm = rMarsKm + hpMarsKm;
    const vParkCircKmS = Math.sqrt(muMars / rParkKm);
    const vTransDepHypKmS = Math.sqrt(Math.pow(vInfDepKmS, 2.0) + (2.0 * muMars / rParkKm));
    const dvTigiKmS = vTransDepHypKmS - vParkCircKmS;

    // Speeds at target arrival
    const vTargCircKmS = Math.sqrt(muSun / rTargDistKm);
    const vApoTransferKmS = Math.sqrt(muSun * ((2.0 / rTargDistKm) - (1.0 / aTransferKm)));
    const vInfArrKmS = Math.abs(vTargCircKmS - vApoTransferKmS);

    return {
      targetPlanet: targetPlanetName,
      targetDistanceAU: parseFloat(rTargAU.toFixed(4)),
      timeOfFlightDays: parseFloat(tofDays.toFixed(1)),
      timeOfFlightYears: parseFloat(tofYrs.toFixed(2)),
      transIceGiantInjectionDeltaVKmS: parseFloat(dvTigiKmS.toFixed(3)),
      iceGiantHyperbolicExcessKmS: parseFloat(vInfArrKmS.toFixed(3)),
      iceGiantTransferContext: `Mars to ${targetPlanetName} Transfer (${tofYrs.toFixed(1)} yr TOF, ${dvTigiKmS.toFixed(2)} km/s Injection, ${vInfArrKmS.toFixed(2)} km/s Arrival Excess)`
    };
  }

  /**
   * Calculate Mars-to-Kuiper Belt Object (Pluto / Arrokoth / Eris / Makemake) deep solar system transfer trajectory, Trans-KBO Injection, and flyby excess.
   * a_t = ( r_mars + r_kbo ) / 2
   * TOF = pi * sqrt( a_t^3 / mu_sun )
   * Reference: Stern et al. (2015), Bate et al. (1971), Curtis (2013) for New Horizons Kuiper Belt Exploration.
   * @param {string} [targetKboName='Dwarf Planet Pluto'] - Target KBO name
   * @param {number} [targetSemiMajorAxisAU=39.48] - Target KBO distance from Sun in AU (30.0 to 100.0 AU)
   * @param {number} [marsParkingAltitudeKm=300.0] - Mars departure parking orbit altitude in km (150 to 1000 km)
   * @returns {{targetBody: string, targetDistanceAU: number, timeOfFlightDays: number, timeOfFlightYears: number, transKboInjectionDeltaVKmS: number, kboHyperbolicExcessKmS: number, kboTransferContext: string}}
   */
  static computeMarsToKuiperBeltTransferTrajectory(targetKboName = 'Dwarf Planet Pluto', targetSemiMajorAxisAU = 39.48, marsParkingAltitudeKm = 300.0) {
    const rKboAU = Math.max(30.0, Math.min(150.0, targetSemiMajorAxisAU));
    const hpMarsKm = Math.max(150.0, marsParkingAltitudeKm);

    const AU_KM = 1.495978707e8;
    const muSun = 1.32712440018e11;
    const muMars = 42828.37;
    const rMarsKm = 3389.5;

    const rMarsAU = 1.52368;
    const rMarsDistKm = rMarsAU * AU_KM;
    const rKboDistKm = rKboAU * AU_KM;

    // Transfer ellipse
    const aTransferAU = (rMarsAU + rKboAU) / 2.0;
    const aTransferKm = aTransferAU * AU_KM;

    // Time of flight (days & years)
    const tofSec = Math.PI * Math.sqrt(Math.pow(aTransferKm, 3.0) / muSun);
    const tofDays = tofSec / 86400.0;
    const tofYrs = tofDays / 365.25;

    // Speeds at Mars departure
    const vMarsKmS = Math.sqrt(muSun / rMarsDistKm);
    const vPeriTransferKmS = Math.sqrt(muSun * ((2.0 / rMarsDistKm) - (1.0 / aTransferKm)));
    const vInfDepKmS = Math.abs(vPeriTransferKmS - vMarsKmS);

    // Trans-KBO Injection Delta-V
    const rParkKm = rMarsKm + hpMarsKm;
    const vParkCircKmS = Math.sqrt(muMars / rParkKm);
    const vTransDepHypKmS = Math.sqrt(Math.pow(vInfDepKmS, 2.0) + (2.0 * muMars / rParkKm));
    const dvTkboKmS = vTransDepHypKmS - vParkCircKmS;

    // Speeds at KBO arrival
    const vKboCircKmS = Math.sqrt(muSun / rKboDistKm);
    const vApoTransferKmS = Math.sqrt(muSun * ((2.0 / rKboDistKm) - (1.0 / aTransferKm)));
    const vInfArrKmS = Math.abs(vKboCircKmS - vApoTransferKmS);

    return {
      targetBody: targetKboName,
      targetDistanceAU: parseFloat(rKboAU.toFixed(4)),
      timeOfFlightDays: parseFloat(tofDays.toFixed(1)),
      timeOfFlightYears: parseFloat(tofYrs.toFixed(2)),
      transKboInjectionDeltaVKmS: parseFloat(dvTkboKmS.toFixed(3)),
      kboHyperbolicExcessKmS: parseFloat(vInfArrKmS.toFixed(3)),
      kboTransferContext: `Mars to ${targetKboName} Transfer (${tofYrs.toFixed(1)} yr TOF, ${dvTkboKmS.toFixed(2)} km/s Injection, ${vInfArrKmS.toFixed(2)} km/s Flyby Excess)`
    };
  }

  /**
   * Calculate Mars-to-Interstellar Heliopause Escape Hyperbolic Trajectory, Trans-Interstellar Injection (TII), and Heliopause crossing time.
   * v_p = sqrt( v_inf_sun^2 + 2 * mu_sun / r_mars )
   * Delta_V_TII = sqrt( v_inf_mars^2 + 2 * mu_mars / r_park ) - sqrt( mu_mars / r_park )
   * Reference: Stone et al. (2013), McNutt et al. (2022), Curtis (2013) for Interstellar Heliopause Escape.
   * @param {number} [heliopauseDistanceAU=122.0] - Outer Heliopause boundary distance in AU (80.0 to 200.0 AU, Voyager 1)
   * @param {number} [asymptoticEscapeSpeedKmS=15.0] - Asymptotic interstellar escape speed v_inf in km/s (5.0 to 35.0 km/s)
   * @param {number} [marsParkingAltitudeKm=300.0] - Mars departure parking orbit altitude in km (150 to 1000 km)
   * @returns {{heliopauseDistanceAU: number, asymptoticEscapeSpeedKmS: number, transInterstellarInjectionDeltaVKmS: number, timeOfFlightYears: number, heliopauseCrossingSpeedKmS: number, interstellarContext: string}}
   */
  static computeMarsToInterstellarEscapeTrajectory(heliopauseDistanceAU = 122.0, asymptoticEscapeSpeedKmS = 15.0, marsParkingAltitudeKm = 300.0) {
    const RhpAU = Math.max(50.0, Math.min(300.0, heliopauseDistanceAU));
    const vInfSunKmS = Math.max(5.0, Math.min(50.0, asymptoticEscapeSpeedKmS));
    const hpMarsKm = Math.max(150.0, marsParkingAltitudeKm);

    const AU_KM = 1.495978707e8;
    const muSun = 1.32712440018e11;
    const muMars = 42828.37;
    const rMarsKm = 3389.5;

    const rMarsAU = 1.52368;
    const rMarsDistKm = rMarsAU * AU_KM;
    const RhpDistKm = RhpAU * AU_KM;

    // Heliocentric hyperbolic perihelion speed at Mars
    const vPeriSunKmS = Math.sqrt(Math.pow(vInfSunKmS, 2.0) + (2.0 * muSun / rMarsDistKm));
    const vMarsCircKmS = Math.sqrt(muSun / rMarsDistKm);
    const vInfMarsKmS = vPeriSunKmS - vMarsCircKmS;

    // Trans-Interstellar Injection Delta-V
    const rParkKm = rMarsKm + hpMarsKm;
    const vParkCircKmS = Math.sqrt(muMars / rParkKm);
    const vTransDepHypKmS = Math.sqrt(Math.pow(vInfMarsKmS, 2.0) + (2.0 * muMars / rParkKm));
    const dvTiiKmS = vTransDepHypKmS - vParkCircKmS;

    // Speed at Heliopause crossing (km/s)
    const vCrossKmS = Math.sqrt(Math.pow(vInfSunKmS, 2.0) + (2.0 * muSun / RhpDistKm));

    // Hyperbolic Keplerian time of flight (years)
    const aHyperKm = muSun / Math.pow(vInfSunKmS, 2.0);
    const eHyper = 1.0 + (rMarsDistKm / aHyperKm);
    const coshF = (1.0 / eHyper) * (1.0 + (RhpDistKm / aHyperKm));
    const F = Math.acosh(Math.max(1.0, coshF));
    const Mh = (eHyper * Math.sinh(F)) - F;
    const tofSec = Math.sqrt(Math.pow(aHyperKm, 3.0) / muSun) * Mh;
    const tofYrs = tofSec / (86400.0 * 365.25);

    return {
      heliopauseDistanceAU: parseFloat(RhpAU.toFixed(1)),
      asymptoticEscapeSpeedKmS: parseFloat(vInfSunKmS.toFixed(2)),
      transInterstellarInjectionDeltaVKmS: parseFloat(dvTiiKmS.toFixed(3)),
      timeOfFlightYears: parseFloat(tofYrs.toFixed(1)),
      heliopauseCrossingSpeedKmS: parseFloat(vCrossKmS.toFixed(2)),
      interstellarContext: `Interstellar Heliopause Escape (${tofYrs.toFixed(1)} yr to ${RhpAU.toFixed(0)} AU, ${dvTiiKmS.toFixed(2)} km/s TII, ${vCrossKmS.toFixed(1)} km/s at Heliopause)`
    };
  }

  /**
   * Calculate Mars-to-Sun Inward Coronal Dive / Parker Solar Probe trajectory, Trans-Solar Injection (TSI), and coronal perihelion velocity.
   * a_t = ( r_mars + r_perihelion ) / 2
   * TOF = pi * sqrt( a_t^3 / mu_sun )
   * Reference: Fox et al. (2016), Curtis (2013) for Solar Coronal Exploration Trajectories.
   * @param {number} [targetPerihelionSolarRadii=9.86] - Target perihelion distance in solar radii R_sun (4.0 to 100.0 R_sun)
   * @param {number} [marsParkingAltitudeKm=300.0] - Mars departure parking orbit altitude in km (150 to 1000 km)
   * @returns {{perihelionSolarRadii: number, perihelionDistanceKm: number, timeOfFlightDays: number, transSolarInjectionDeltaVKmS: number, solarPerihelionSpeedKmS: number, solarDiveContext: string}}
   */
  static computeMarsToSolarPerihelionDiveTrajectory(targetPerihelionSolarRadii = 9.86, marsParkingAltitudeKm = 300.0) {
    const RsunKm = 696340.0;
    const AU_KM = 1.495978707e8;
    const muSun = 1.32712440018e11;
    const muMars = 42828.37;
    const rMarsKm = 3389.5;

    const rPeriSolarRad = Math.max(3.0, Math.min(200.0, targetPerihelionSolarRadii));
    const hpMarsKm = Math.max(150.0, marsParkingAltitudeKm);

    const rPeriKm = rPeriSolarRad * RsunKm;
    const rMarsAU = 1.52368;
    const rMarsDistKm = rMarsAU * AU_KM;

    // Transfer ellipse
    const aTransferKm = (rMarsDistKm + rPeriKm) / 2.0;

    // Time of flight (days)
    const tofSec = Math.PI * Math.sqrt(Math.pow(aTransferKm, 3.0) / muSun);
    const tofDays = tofSec / 86400.0;

    // Speeds at Mars departure (retrograde burn)
    const vMarsKmS = Math.sqrt(muSun / rMarsDistKm);
    const vApoTransferKmS = Math.sqrt(muSun * ((2.0 / rMarsDistKm) - (1.0 / aTransferKm)));
    const vInfDepKmS = Math.abs(vMarsKmS - vApoTransferKmS);

    // Trans-Solar Injection Delta-V
    const rParkKm = rMarsKm + hpMarsKm;
    const vParkCircKmS = Math.sqrt(muMars / rParkKm);
    const vTransDepHypKmS = Math.sqrt(Math.pow(vInfDepKmS, 2.0) + (2.0 * muMars / rParkKm));
    const dvTsiKmS = vTransDepHypKmS - vParkCircKmS;

    // Solar perihelion speed
    const vPeriKmS = Math.sqrt(muSun * ((2.0 / rPeriKm) - (1.0 / aTransferKm)));

    return {
      perihelionSolarRadii: parseFloat(rPeriSolarRad.toFixed(2)),
      perihelionDistanceKm: parseFloat(rPeriKm.toFixed(0)),
      timeOfFlightDays: parseFloat(tofDays.toFixed(1)),
      transSolarInjectionDeltaVKmS: parseFloat(dvTsiKmS.toFixed(3)),
      solarPerihelionSpeedKmS: parseFloat(vPeriKmS.toFixed(2)),
      solarDiveContext: `Mars to Sun Coronal Dive (${tofDays.toFixed(0)} d TOF, ${dvTsiKmS.toFixed(2)} km/s TSI, ${vPeriKmS.toFixed(1)} km/s at ${rPeriSolarRad.toFixed(1)} R_sun Perihelion)`
    };
  }

  /**
   * Calculate Mars-to-Jupiter Trojan Asteroid System (L4 Greek / L5 Trojan Swarms) Hohmann transfer, Trans-Trojan Injection (TTI), and rendezvous Delta-V.
   * a_t = ( r_mars + r_trojan ) / 2
   * TOF = pi * sqrt( a_t^3 / mu_sun )
   * Reference: Levison et al. (2021) for Lucy Mission, Bate et al. (1971), Curtis (2013).
   * @param {string} [targetTrojanCluster='L4 Greek Camp (Eurybates/Polymele)'] - Target Trojan cluster name
   * @param {number} [jupiterSemiMajorAxisAU=5.2044] - Jupiter/Trojan distance in AU (5.0 to 5.5 AU)
   * @param {number} [marsParkingAltitudeKm=300.0] - Mars departure parking orbit altitude in km (150 to 1000 km)
   * @returns {{targetCluster: string, trojanDistanceAU: number, timeOfFlightDays: number, timeOfFlightYears: number, transTrojanInjectionDeltaVKmS: number, trojanArrivalExcessKmS: number, totalMissionDeltaVKmS: number, trojanTransferContext: string}}
   */
  static computeMarsToJupiterTrojanHohmannTransfer(targetTrojanCluster = 'L4 Greek Camp (Eurybates/Polymele)', jupiterSemiMajorAxisAU = 5.2044, marsParkingAltitudeKm = 300.0) {
    const rTrojAU = Math.max(4.8, Math.min(5.6, jupiterSemiMajorAxisAU));
    const hpMarsKm = Math.max(150.0, marsParkingAltitudeKm);

    const AU_KM = 1.495978707e8;
    const muSun = 1.32712440018e11;
    const muMars = 42828.37;
    const rMarsKm = 3389.5;

    const rMarsAU = 1.52368;
    const rMarsDistKm = rMarsAU * AU_KM;
    const rTrojDistKm = rTrojAU * AU_KM;

    // Transfer ellipse
    const aTransferAU = (rMarsAU + rTrojAU) / 2.0;
    const aTransferKm = aTransferAU * AU_KM;

    // Time of flight (days & years)
    const tofSec = Math.PI * Math.sqrt(Math.pow(aTransferKm, 3.0) / muSun);
    const tofDays = tofSec / 86400.0;
    const tofYrs = tofDays / 365.25;

    // Speeds at Mars departure
    const vMarsKmS = Math.sqrt(muSun / rMarsDistKm);
    const vPeriTransferKmS = Math.sqrt(muSun * ((2.0 / rMarsDistKm) - (1.0 / aTransferKm)));
    const vInfDepKmS = Math.abs(vPeriTransferKmS - vMarsKmS);

    // Trans-Trojan Injection Delta-V
    const rParkKm = rMarsKm + hpMarsKm;
    const vParkCircKmS = Math.sqrt(muMars / rParkKm);
    const vTransDepHypKmS = Math.sqrt(Math.pow(vInfDepKmS, 2.0) + (2.0 * muMars / rParkKm));
    const dvTtiKmS = vTransDepHypKmS - vParkCircKmS;

    // Speeds at Trojan arrival
    const vTrojCircKmS = Math.sqrt(muSun / rTrojDistKm);
    const vApoTransferKmS = Math.sqrt(muSun * ((2.0 / rTrojDistKm) - (1.0 / aTransferKm)));
    const vInfArrTrojKmS = Math.abs(vTrojCircKmS - vApoTransferKmS);

    // Total mission Delta-V for rendezvous
    const dvTotKmS = dvTtiKmS + vInfArrTrojKmS;

    return {
      targetCluster: targetTrojanCluster,
      trojanDistanceAU: parseFloat(rTrojAU.toFixed(4)),
      timeOfFlightDays: parseFloat(tofDays.toFixed(1)),
      timeOfFlightYears: parseFloat(tofYrs.toFixed(2)),
      transTrojanInjectionDeltaVKmS: parseFloat(dvTtiKmS.toFixed(3)),
      trojanArrivalExcessKmS: parseFloat(vInfArrTrojKmS.toFixed(3)),
      totalMissionDeltaVKmS: parseFloat(dvTotKmS.toFixed(3)),
      trojanTransferContext: `Mars to ${targetTrojanCluster} Transfer (${tofYrs.toFixed(1)} yr TOF, ${dvTtiKmS.toFixed(2)} km/s TTI, ${vInfArrTrojKmS.toFixed(2)} km/s Rendezvous, Total ${dvTotKmS.toFixed(2)} km/s)`
    };
  }

  /**
   * Calculate Mars Atmospheric Entry, Descent & Landing (EDL) trajectory, peak deceleration altitude, G-load, dynamic pressure, and stagnation aerothermal heat flux.
   * h_gmax = H * ln( ( rho_0 * H ) / ( 2 * beta * sin(-gamma) ) )
   * a_max = v_entry^2 * sin(-gamma) / ( 2 * e * H )
   * q_dot_stag = k_SG * sqrt( rho_stag / R_n ) * v_gmax^3
   * Reference: Allen & Eggers (1958), Sutton & Graves (1971), Steltzner et al. (2014) for MSL/Curiosity EDL.
   * @param {number} [entrySpeedKmS=5.85] - Atmospheric entry interface velocity at 125 km in km/s (3.5 to 8.0 km/s)
   * @param {number} [flightPathAngleDeg=-12.5] - Entry flight path angle gamma in degrees (-6.0 to -25.0 deg)
   * @param {number} [ballisticCoefficientKgM2=145.0] - Aeroshell ballistic coefficient m/(Cd*A) in kg/m^2 (50 to 300 kg/m^2)
   * @param {number} [noseRadiusM=0.60] - Heatshield effective nose radius in m (0.2 to 2.0 m)
   * @returns {{entrySpeedKmS: number, flightPathAngleDeg: number, peakDecelerationAltitudeKm: number, peakDecelerationGs: number, peakDynamicPressureKPa: number, peakStagnationHeatFluxWcm2: number, velocityAtPeakDecelKmS: number, edlContext: string}}
   */
  static computeMartianAtmosphericEntryDescentTrajectory(entrySpeedKmS = 5.85, flightPathAngleDeg = -12.5, ballisticCoefficientKgM2 = 145.0, noseRadiusM = 0.60) {
    const vEntryKmS = Math.max(2.5, Math.min(10.0, entrySpeedKmS));
    const gammaDeg = Math.min(-3.0, Math.max(-45.0, flightPathAngleDeg));
    const beta = Math.max(20.0, ballisticCoefficientKgM2);
    const Rn = Math.max(0.1, noseRadiusM);

    const vEntryMS = vEntryKmS * 1000.0;
    const gammaRad = Math.abs(gammaDeg * (Math.PI / 180.0));
    const H = 11100.0; // Scale height (m)
    const rho0 = 0.020; // Surface density (kg/m^3)
    const g0Earth = 9.80665;
    const kSG = 1.90e-4; // Sutton-Graves CO2 aerothermal coefficient

    // Altitude of peak deceleration (m & km)
    const arg = (rho0 * H) / (2.0 * beta * Math.sin(gammaRad));
    const hGmaxM = Math.max(0.0, H * Math.log(Math.max(1.001, arg)));
    const hGmaxKm = hGmaxM / 1000.0;

    // Peak deceleration (m/s^2 and Earth G's)
    const aMaxMS2 = (Math.pow(vEntryMS, 2.0) * Math.sin(gammaRad)) / (2.0 * Math.E * H);
    const Gmax = aMaxMS2 / g0Earth;

    // Velocity at peak deceleration (m/s & km/s)
    const vGmaxMS = vEntryMS * Math.exp(-0.5);
    const vGmaxKmS = vGmaxMS / 1000.0;

    // Density at peak deceleration
    const rhoStag = (2.0 * beta * Math.sin(gammaRad)) / H;

    // Peak dynamic pressure (kPa)
    const qDynPa = 0.5 * rhoStag * Math.pow(vGmaxMS, 2.0);
    const qDynKPa = qDynPa / 1000.0;

    // Peak stagnation aerothermal heat flux (W/cm^2)
    const qDotStagWM2 = kSG * Math.sqrt(rhoStag / Rn) * Math.pow(vGmaxMS, 3.0);
    const qDotStagWCm2 = qDotStagWM2 / 10000.0;

    return {
      entrySpeedKmS: parseFloat(vEntryKmS.toFixed(2)),
      flightPathAngleDeg: parseFloat(gammaDeg.toFixed(1)),
      peakDecelerationAltitudeKm: parseFloat(hGmaxKm.toFixed(2)),
      peakDecelerationGs: parseFloat(Gmax.toFixed(2)),
      peakDynamicPressureKPa: parseFloat(qDynKPa.toFixed(2)),
      peakStagnationHeatFluxWcm2: parseFloat(qDotStagWCm2.toFixed(1)),
      velocityAtPeakDecelKmS: parseFloat(vGmaxKmS.toFixed(2)),
      edlContext: `Mars EDL Entry (${Gmax.toFixed(1)} g Peak Load at ${hGmaxKm.toFixed(1)} km, ${qDotStagWCm2.toFixed(0)} W/cm^2 Peak Heat Flux, ${qDynKPa.toFixed(1)} kPa Dynamic Pressure)`
    };
  }

  /**
   * Calculate Ice Giant (Uranus / Neptune) atmospheric aerocapture trajectory, single-pass orbital insertion Delta-V, propellant mass fraction saved, and aerocapture entry corridor width.
   * v_p = sqrt( v_inf^2 + 2 * mu / r_p )
   * Delta_V_aero = v_p - sqrt( mu * ( 2 / r_p - 1 / a_target ) )
   * Reference: Cruz (1993), Spilker et al. (2019), Girija et al. (2020) for Ice Giant Aerocapture.
   * @param {string} [targetPlanetName='Uranus'] - Target Ice Giant ('Uranus' or 'Neptune')
   * @param {number} [hyperbolicArrivalSpeedKmS=4.20] - Hyperbolic arrival excess speed v_inf in km/s (2.0 to 10.0 km/s)
   * @param {number} [targetPeriapsisAltitudeKm=250.0] - Atmospheric entry periapsis altitude in km (100 to 600 km)
   * @param {number} [liftToDragRatio=0.25] - Aeroshell hypersonic lift-to-drag ratio L/D (0.1 to 0.6)
   * @param {number} [targetApoapsisAltitudeKm=100000.0] - Target capture orbit apoapsis altitude in km (50000 to 500000 km)
   * @returns {{targetPlanet: string, hyperbolicArrivalSpeedKmS: number, atmosphericPeriapsisSpeedKmS: number, aerocaptureDeltaVSavedKmS: number, propellantMassFractionSavedPercent: number, aerocaptureCorridorWidthDeg: number, aerocaptureContext: string}}
   */
  static computeIceGiantAtmosphericAerocaptureTrajectory(targetPlanetName = 'Uranus', hyperbolicArrivalSpeedKmS = 4.20, targetPeriapsisAltitudeKm = 250.0, liftToDragRatio = 0.25, targetApoapsisAltitudeKm = 100000.0) {
    const isNeptune = targetPlanetName.toLowerCase().includes('neptune');
    const planetName = isNeptune ? 'Neptune' : 'Uranus';

    const muPlanet = isNeptune ? 6836527.0 : 5793939.0;
    const rPlanetKm = isNeptune ? 24622.0 : 25362.0;
    const HScaleKm = isNeptune ? 20.0 : 27.7;

    const vInfKmS = Math.max(1.0, Math.min(15.0, hyperbolicArrivalSpeedKmS));
    const hpKm = Math.max(50.0, Math.min(1000.0, targetPeriapsisAltitudeKm));
    const haKm = Math.max(20000.0, targetApoapsisAltitudeKm);
    const ld = Math.max(0.05, Math.min(0.8, liftToDragRatio));

    const rpKm = rPlanetKm + hpKm;
    const raKm = rPlanetKm + haKm;
    const aTargetKm = (rpKm + raKm) / 2.0;

    // Atmospheric entry periapsis speed
    const vpAtmKmS = Math.sqrt(Math.pow(vInfKmS, 2.0) + (2.0 * muPlanet / rpKm));

    // Target orbit speed at periapsis
    const vpTargetKmS = Math.sqrt(muPlanet * ((2.0 / rpKm) - (1.0 / aTargetKm)));

    // Delta-V saved by aerocapture pass
    const dvAeroKmS = Math.max(0.0, vpAtmKmS - vpTargetKmS);

    // Propellant mass fraction saved (assuming Isp = 320 s, ve = 3.138 km/s)
    const veKmS = 3.138;
    const propSavedPct = (1.0 - Math.exp(-dvAeroKmS / veKmS)) * 100.0;

    // Aerocapture entry flight path angle corridor width (deg)
    const gammaCorrDeg = ((2.0 * ld) / Math.sqrt(rpKm / HScaleKm)) * (180.0 / Math.PI);

    return {
      targetPlanet: planetName,
      hyperbolicArrivalSpeedKmS: parseFloat(vInfKmS.toFixed(2)),
      atmosphericPeriapsisSpeedKmS: parseFloat(vpAtmKmS.toFixed(2)),
      aerocaptureDeltaVSavedKmS: parseFloat(dvAeroKmS.toFixed(3)),
      propellantMassFractionSavedPercent: parseFloat(propSavedPct.toFixed(1)),
      aerocaptureCorridorWidthDeg: parseFloat(gammaCorrDeg.toFixed(2)),
      aerocaptureContext: `${planetName} Aerocapture (${dvAeroKmS.toFixed(2)} km/s Aero Delta-V, ${propSavedPct.toFixed(0)}% Propellant Saved, ${gammaCorrDeg.toFixed(2)} deg Corridor)`
    };
  }

  /**
   * Calculate Mars-to-Venus inward transfer trajectory, Trans-Venus Injection (TVI), flyby deflection angle, and gravity assist heliocentric velocity boost.
   * a_t = ( r_mars + r_venus ) / 2
   * delta = 2 * arcsin( 1 / ( 1 + r_p * v_inf^2 / mu_venus ) )
   * Delta_V_assist = 2 * v_inf * sin( delta / 2 )
   * Reference: Bate et al. (1971), Broucke (1988), Curtis (2013) for Planetary Gravity Assist Dynamics.
   * @param {number} [venusPeriapsisAltitudeKm=300.0] - Venus flyby closest approach altitude in km (200 to 5000 km)
   * @param {number} [marsParkingAltitudeKm=300.0] - Mars departure parking orbit altitude in km (150 to 1000 km)
   * @returns {{timeOfFlightDays: number, timeOfFlightYears: number, transVenusInjectionDeltaVKmS: number, venusArrivalExcessSpeedKmS: number, flybyDeflectionAngleDeg: number, heliocentricDeltaVBoostKmS: number, gravityAssistContext: string}}
   */
  static computeMarsToVenusGravityAssistTrajectory(venusPeriapsisAltitudeKm = 300.0, marsParkingAltitudeKm = 300.0) {
    const hpVenusKm = Math.max(150.0, venusPeriapsisAltitudeKm);
    const hpMarsKm = Math.max(150.0, marsParkingAltitudeKm);

    const AU_KM = 1.495978707e8;
    const muSun = 1.32712440018e11;
    const muMars = 42828.37;
    const muVenus = 324859.0;
    const rMarsKm = 3389.5;
    const rVenusKm = 6051.8;

    const rMarsAU = 1.52368;
    const rVenusAU = 0.72333;
    const rMarsDistKm = rMarsAU * AU_KM;
    const rVenusDistKm = rVenusAU * AU_KM;

    // Transfer ellipse
    const aTransferAU = (rMarsAU + rVenusAU) / 2.0;
    const aTransferKm = aTransferAU * AU_KM;

    // Time of flight (days & years)
    const tofSec = Math.PI * Math.sqrt(Math.pow(aTransferKm, 3.0) / muSun);
    const tofDays = tofSec / 86400.0;
    const tofYrs = tofDays / 365.25;

    // Speeds at Mars departure
    const vMarsKmS = Math.sqrt(muSun / rMarsDistKm);
    const vApoTransferKmS = Math.sqrt(muSun * ((2.0 / rMarsDistKm) - (1.0 / aTransferKm)));
    const vInfDepKmS = Math.abs(vMarsKmS - vApoTransferKmS);

    // Trans-Venus Injection Delta-V
    const rParkKm = rMarsKm + hpMarsKm;
    const vParkCircKmS = Math.sqrt(muMars / rParkKm);
    const vTransDepHypKmS = Math.sqrt(Math.pow(vInfDepKmS, 2.0) + (2.0 * muMars / rParkKm));
    const dvTviKmS = vTransDepHypKmS - vParkCircKmS;

    // Speeds at Venus arrival
    const vVenusCircKmS = Math.sqrt(muSun / rVenusDistKm);
    const vPeriTransferKmS = Math.sqrt(muSun * ((2.0 / rVenusDistKm) - (1.0 / aTransferKm)));
    const vInfArrVenusKmS = Math.abs(vPeriTransferKmS - vVenusCircKmS);

    // Venus flyby geometry & Deflection angle (deg)
    const rpFlybyKm = rVenusKm + hpVenusKm;
    const eFlyby = 1.0 + ((rpFlybyKm * Math.pow(vInfArrVenusKmS, 2.0)) / muVenus);
    const deltaRad = 2.0 * Math.asin(1.0 / eFlyby);
    const deltaDeg = deltaRad * (180.0 / Math.PI);

    // Heliocentric Delta-V boost
    const dvBoostKmS = 2.0 * vInfArrVenusKmS * Math.sin(deltaRad / 2.0);

    return {
      timeOfFlightDays: parseFloat(tofDays.toFixed(1)),
      timeOfFlightYears: parseFloat(tofYrs.toFixed(2)),
      transVenusInjectionDeltaVKmS: parseFloat(dvTviKmS.toFixed(3)),
      venusArrivalExcessSpeedKmS: parseFloat(vInfArrVenusKmS.toFixed(3)),
      flybyDeflectionAngleDeg: parseFloat(deltaDeg.toFixed(2)),
      heliocentricDeltaVBoostKmS: parseFloat(dvBoostKmS.toFixed(3)),
      gravityAssistContext: `Mars to Venus Gravity Assist (${tofDays.toFixed(0)} d TOF, ${dvTviKmS.toFixed(2)} km/s TVI, ${deltaDeg.toFixed(1)} deg Turn, +${dvBoostKmS.toFixed(2)} km/s Boost)`
    };
  }

  /**
   * Calculate Mars Gravity Assist (MGA) flyby trajectory for Jupiter/Outer Planet orbit pumping, hyperbolic turning angle, and heliocentric velocity boost.
   * e = 1 + r_p * v_inf^2 / mu_mars
   * delta = 2 * arcsin( 1 / e )
   * Delta_V_assist = 2 * v_inf * sin( delta / 2 )
   * Reference: Bate et al. (1971), Broucke (1988), Curtis (2013) for Rosetta/Psyche/Europa Clipper Mars Gravity Assist.
   * @param {number} [marsFlybyAltitudeKm=250.0] - Mars closest approach periapsis altitude in km (150 to 5000 km)
   * @param {number} [approachHyperbolicSpeedKmS=5.60] - Interplanetary hyperbolic arrival speed v_inf in km/s (2.0 to 12.0 km/s)
   * @returns {{marsFlybyAltitudeKm: number, approachHyperbolicSpeedKmS: number, flybyEccentricity: number, turningAngleDeg: number, heliocentricVelocityBoostKmS: number, postFlybyAphelionAU: number, mgaContext: string}}
   */
  static computeEarthToJupiterMarsGravityAssistFlyby(marsFlybyAltitudeKm = 250.0, approachHyperbolicSpeedKmS = 5.60) {
    const hpKm = Math.max(150.0, marsFlybyAltitudeKm);
    const vInfKmS = Math.max(1.0, Math.min(15.0, approachHyperbolicSpeedKmS));

    const muMars = 42828.37;
    const rMarsKm = 3389.5;
    const muSun = 1.32712440018e11;
    const AU_KM = 1.495978707e8;
    const rMarsDistKm = 1.52368 * AU_KM;

    // Mars flyby geometry
    const rpKm = rMarsKm + hpKm;
    const eHyp = 1.0 + ((rpKm * Math.pow(vInfKmS, 2.0)) / muMars);
    const deltaRad = 2.0 * Math.asin(1.0 / eHyp);
    const deltaDeg = deltaRad * (180.0 / Math.PI);

    // Heliocentric velocity boost (km/s)
    const dvBoostKmS = 2.0 * vInfKmS * Math.sin(deltaRad / 2.0);

    // Post-flyby heliocentric orbital pumping (aphelion distance in AU)
    const vMarsCircKmS = Math.sqrt(muSun / rMarsDistKm);
    const vPostFlybyKmS = vMarsCircKmS + (dvBoostKmS * 0.707); // prograde pumping component
    const energyPost = (Math.pow(vPostFlybyKmS, 2.0) / 2.0) - (muSun / rMarsDistKm);
    const aPostKm = -muSun / (2.0 * energyPost);
    const rApoPostKm = (2.0 * aPostKm) - rMarsDistKm;
    const rApoPostAU = Math.max(1.6, rApoPostKm / AU_KM);

    return {
      marsFlybyAltitudeKm: parseFloat(hpKm.toFixed(1)),
      approachHyperbolicSpeedKmS: parseFloat(vInfKmS.toFixed(2)),
      flybyEccentricity: parseFloat(eHyp.toFixed(3)),
      turningAngleDeg: parseFloat(deltaDeg.toFixed(2)),
      heliocentricVelocityBoostKmS: parseFloat(dvBoostKmS.toFixed(3)),
      postFlybyAphelionAU: parseFloat(rApoPostAU.toFixed(2)),
      mgaContext: `Mars Gravity Assist (${deltaDeg.toFixed(1)} deg Deflection, +${dvBoostKmS.toFixed(2)} km/s Boost, Aphelion pumped to ${rApoPostAU.toFixed(1)} AU)`
    };
  }

  /**
   * Calculate Mars-to-Jupiter Interplanetary Cycler orbit trajectory, orbital resonance period, Trans-Cycler Injection (TCI), and encounter hyperbolic excesses.
   * a = ( r_mars + r_jupiter ) / 2
   * P_cycler = a^1.5
   * e = ( r_jupiter - r_mars ) / ( r_jupiter + r_mars )
   * Reference: Aldrin (1985), Byrnes et al. (1993), Russell & Ocampo (2004), Curtis (2013) for Interplanetary Cycler Mechanics.
   * @param {number} [jupiterSemiMajorAxisAU=5.2044] - Jupiter distance in AU (5.0 to 5.5 AU)
   * @param {number} [marsParkingAltitudeKm=300.0] - Mars departure parking orbit altitude in km (150 to 1000 km)
   * @returns {{semiMajorAxisAU: number, eccentricity: number, orbitalPeriodYears: number, oneWayTransitYears: number, transCyclerInjectionDeltaVKmS: number, marsEncounterVInfKmS: number, jupiterEncounterVInfKmS: number, cyclerContext: string}}
   */
  static computeMarsToJupiterCyclerOrbitTrajectory(jupiterSemiMajorAxisAU = 5.2044, marsParkingAltitudeKm = 300.0) {
    const rJupAU = Math.max(4.8, Math.min(5.6, jupiterSemiMajorAxisAU));
    const hpMarsKm = Math.max(150.0, marsParkingAltitudeKm);

    const AU_KM = 1.495978707e8;
    const muSun = 1.32712440018e11;
    const muMars = 42828.37;
    const rMarsKm = 3389.5;

    const rMarsAU = 1.52368;
    const rMarsDistKm = rMarsAU * AU_KM;
    const rJupDistKm = rJupAU * AU_KM;

    // Cycler orbit ellipse
    const aAU = (rMarsAU + rJupAU) / 2.0;
    const aKm = aAU * AU_KM;
    const ecc = (rJupAU - rMarsAU) / (rJupAU + rMarsAU);

    // Period & Transit time (years)
    const periodYrs = Math.pow(aAU, 1.5);
    const transitYrs = periodYrs / 2.0;

    // Perihelion and aphelion velocities
    const vpKmS = Math.sqrt(muSun * ((2.0 / rMarsDistKm) - (1.0 / aKm)));
    const vaKmS = Math.sqrt(muSun * ((2.0 / rJupDistKm) - (1.0 / aKm)));

    // Circular planet velocities
    const vMarsCircKmS = Math.sqrt(muSun / rMarsDistKm);
    const vJupCircKmS = Math.sqrt(muSun / rJupDistKm);

    // Hyperbolic excesses
    const vInfMarsKmS = Math.abs(vpKmS - vMarsCircKmS);
    const vInfJupKmS = Math.abs(vJupCircKmS - vaKmS);

    // Trans-Cycler Injection Delta-V
    const rParkKm = rMarsKm + hpMarsKm;
    const vParkCircKmS = Math.sqrt(muMars / rParkKm);
    const vTransDepHypKmS = Math.sqrt(Math.pow(vInfMarsKmS, 2.0) + (2.0 * muMars / rParkKm));
    const dvTciKmS = vTransDepHypKmS - vParkCircKmS;

    return {
      semiMajorAxisAU: parseFloat(aAU.toFixed(4)),
      eccentricity: parseFloat(ecc.toFixed(4)),
      orbitalPeriodYears: parseFloat(periodYrs.toFixed(2)),
      oneWayTransitYears: parseFloat(transitYrs.toFixed(2)),
      transCyclerInjectionDeltaVKmS: parseFloat(dvTciKmS.toFixed(3)),
      marsEncounterVInfKmS: parseFloat(vInfMarsKmS.toFixed(3)),
      jupiterEncounterVInfKmS: parseFloat(vInfJupKmS.toFixed(3)),
      cyclerContext: `Mars-Jupiter Cycler (${periodYrs.toFixed(1)} yr Period, ${transitYrs.toFixed(1)} yr One-Way Transit, ${dvTciKmS.toFixed(2)} km/s TCI, e=${ecc.toFixed(2)})`
    };
  }

  /**
   * Calculate Mars-to-Mercury inward interplanetary transfer trajectory, Trans-Mercury Injection (TMI), and Mercury Orbit Insertion (MOI) Delta-V.
   * a_t = ( r_mars + r_mercury ) / 2
   * TOF = pi * sqrt( a_t^3 / mu_sun )
   * Reference: Bate et al. (1971), Curtis (2013) for Inward Interplanetary Trajectories.
   * @param {number} [mercuryParkingAltitudeKm=300.0] - Mercury parking orbit altitude in km (100 to 2000 km)
   * @param {number} [marsParkingAltitudeKm=300.0] - Mars departure parking orbit altitude in km (150 to 1000 km)
   * @returns {{timeOfFlightDays: number, timeOfFlightYears: number, transMercuryInjectionDeltaVKmS: number, mercuryArrivalExcessSpeedKmS: number, mercuryOrbitInsertionDeltaVKmS: number, totalMissionDeltaVKmS: number, mercuryTransferContext: string}}
   */
  static computeMarsToMercuryTransferTrajectory(mercuryParkingAltitudeKm = 300.0, marsParkingAltitudeKm = 300.0) {
    const hpMercKm = Math.max(100.0, mercuryParkingAltitudeKm);
    const hpMarsKm = Math.max(150.0, marsParkingAltitudeKm);

    const AU_KM = 1.495978707e8;
    const muSun = 1.32712440018e11;
    const muMars = 42828.37;
    const muMerc = 22032.0;
    const rMarsKm = 3389.5;
    const rMercKm = 2439.7;

    const rMarsAU = 1.52368;
    const rMercAU = 0.38710;
    const rMarsDistKm = rMarsAU * AU_KM;
    const rMercDistKm = rMercAU * AU_KM;

    // Transfer ellipse
    const aTransferAU = (rMarsAU + rMercAU) / 2.0;
    const aTransferKm = aTransferAU * AU_KM;

    // Time of flight (days & years)
    const tofSec = Math.PI * Math.sqrt(Math.pow(aTransferKm, 3.0) / muSun);
    const tofDays = tofSec / 86400.0;
    const tofYrs = tofDays / 365.25;

    // Speeds at Mars departure
    const vMarsKmS = Math.sqrt(muSun / rMarsDistKm);
    const vApoTransferKmS = Math.sqrt(muSun * ((2.0 / rMarsDistKm) - (1.0 / aTransferKm)));
    const vInfDepKmS = Math.abs(vMarsKmS - vApoTransferKmS);

    // Trans-Mercury Injection Delta-V
    const rParkMarsKm = rMarsKm + hpMarsKm;
    const vParkMarsKmS = Math.sqrt(muMars / rParkMarsKm);
    const vTransDepHypKmS = Math.sqrt(Math.pow(vInfDepKmS, 2.0) + (2.0 * muMars / rParkMarsKm));
    const dvTmiKmS = vTransDepHypKmS - vParkMarsKmS;

    // Speeds at Mercury arrival
    const vMercCircKmS = Math.sqrt(muSun / rMercDistKm);
    const vPeriTransferKmS = Math.sqrt(muSun * ((2.0 / rMercDistKm) - (1.0 / aTransferKm)));
    const vInfArrMercKmS = Math.abs(vPeriTransferKmS - vMercCircKmS);

    // Mercury Orbit Insertion Delta-V
    const rParkMercKm = rMercKm + hpMercKm;
    const vParkMercKmS = Math.sqrt(muMerc / rParkMercKm);
    const vTransArrHypKmS = Math.sqrt(Math.pow(vInfArrMercKmS, 2.0) + (2.0 * muMerc / rParkMercKm));
    const dvMoiKmS = vTransArrHypKmS - vParkMercKmS;

    const dvTotKmS = dvTmiKmS + dvMoiKmS;

    return {
      timeOfFlightDays: parseFloat(tofDays.toFixed(1)),
      timeOfFlightYears: parseFloat(tofYrs.toFixed(2)),
      transMercuryInjectionDeltaVKmS: parseFloat(dvTmiKmS.toFixed(3)),
      mercuryArrivalExcessSpeedKmS: parseFloat(vInfArrMercKmS.toFixed(3)),
      mercuryOrbitInsertionDeltaVKmS: parseFloat(dvMoiKmS.toFixed(3)),
      totalMissionDeltaVKmS: parseFloat(dvTotKmS.toFixed(3)),
      mercuryTransferContext: `Mars to Mercury Inward Transfer (${tofDays.toFixed(0)} d TOF, ${dvTmiKmS.toFixed(2)} km/s TMI, ${dvMoiKmS.toFixed(2)} km/s MOI, ${dvTotKmS.toFixed(2)} km/s Total)`
    };
  }

  /**
   * Calculate Mars-to-Mercury multi-gravity assist trajectory with intermediate Venus flyby (M-V-M), leg times, and reduced Mercury Orbit Insertion Delta-V.
   * TOF_tot = TOF_Mars_Venus + TOF_Venus_Mercury
   * Reference: Bate et al. (1971), Broucke (1988), Curtis (2013) for MESSENGER / BepiColombo Inward Gravity Assist Sequences.
   * @param {number} [venusFlybyAltitudeKm=300.0] - Venus flyby closest approach altitude in km (150 to 2000 km)
   * @param {number} [mercuryParkingAltitudeKm=300.0] - Mercury parking orbit altitude in km (100 to 2000 km)
   * @param {number} [marsParkingAltitudeKm=300.0] - Mars departure parking orbit altitude in km (150 to 1000 km)
   * @returns {{totalTimeOfFlightDays: number, totalTimeOfFlightYears: number, marsDepartureDeltaVKmS: number, venusFlybyDeflectionAngleDeg: number, mercuryArrivalExcessKmS: number, mercuryOrbitInsertionDeltaVKmS: number, missionDeltaVSavedKmS: number, mvmContext: string}}
   */
  static computeMarsToMercuryDualGravityAssistTrajectory(venusFlybyAltitudeKm = 300.0, mercuryParkingAltitudeKm = 300.0, marsParkingAltitudeKm = 300.0) {
    const hpVenusKm = Math.max(150.0, venusFlybyAltitudeKm);
    const hpMercKm = Math.max(100.0, mercuryParkingAltitudeKm);
    const hpMarsKm = Math.max(150.0, marsParkingAltitudeKm);

    const AU_KM = 1.495978707e8;
    const muSun = 1.32712440018e11;
    const muMars = 42828.37;
    const muMerc = 22032.0;
    const muVenus = 324859.0;
    const rMarsKm = 3389.5;
    const rVenusKm = 6051.8;
    const rMercKm = 2439.7;

    const rMarsAU = 1.52368;
    const rVenusAU = 0.72333;
    const rMercAU = 0.38710;

    // Leg 1: Mars to Venus
    const a1AU = (rMarsAU + rVenusAU) / 2.0;
    const a1Km = a1AU * AU_KM;
    const tof1Sec = Math.PI * Math.sqrt(Math.pow(a1Km, 3.0) / muSun);
    const tof1Days = tof1Sec / 86400.0;

    const rMarsDistKm = rMarsAU * AU_KM;
    const vMarsKmS = Math.sqrt(muSun / rMarsDistKm);
    const vApo1KmS = Math.sqrt(muSun * ((2.0 / rMarsDistKm) - (1.0 / a1Km)));
    const vInfDepKmS = Math.abs(vMarsKmS - vApo1KmS);

    const rParkMarsKm = rMarsKm + hpMarsKm;
    const vParkMarsKmS = Math.sqrt(muMars / rParkMarsKm);
    const vTransDepHypKmS = Math.sqrt(Math.pow(vInfDepKmS, 2.0) + (2.0 * muMars / rParkMarsKm));
    const dvTviKmS = vTransDepHypKmS - vParkMarsKmS;

    // Venus arrival excess & flyby turn
    const rVenusDistKm = rVenusAU * AU_KM;
    const vVenusCircKmS = Math.sqrt(muSun / rVenusDistKm);
    const vPeri1KmS = Math.sqrt(muSun * ((2.0 / rVenusDistKm) - (1.0 / a1Km)));
    const vInfVenusKmS = Math.abs(vPeri1KmS - vVenusCircKmS);

    const rpFlybyKm = rVenusKm + hpVenusKm;
    const eFlyby = 1.0 + ((rpFlybyKm * Math.pow(vInfVenusKmS, 2.0)) / muVenus);
    const deltaRad = 2.0 * Math.asin(1.0 / eFlyby);
    const deltaDeg = deltaRad * (180.0 / Math.PI);

    // Leg 2: Venus to Mercury
    const a2AU = (rVenusAU + rMercAU) / 2.0;
    const a2Km = a2AU * AU_KM;
    const tof2Sec = Math.PI * Math.sqrt(Math.pow(a2Km, 3.0) / muSun);
    const tof2Days = tof2Sec / 86400.0;

    const tofTotDays = tof1Days + tof2Days;
    const tofTotYrs = tofTotDays / 365.25;

    // Mercury arrival excess & MOI Delta-V
    const rMercDistKm = rMercAU * AU_KM;
    const vMercCircKmS = Math.sqrt(muSun / rMercDistKm);
    const vPeri2KmS = Math.sqrt(muSun * ((2.0 / rMercDistKm) - (1.0 / a2Km)));
    const vInfMercKmS = Math.abs(vPeri2KmS - vMercCircKmS);

    const rParkMercKm = rMercKm + hpMercKm;
    const vParkMercKmS = Math.sqrt(muMerc / rParkMercKm);
    const vTransArrHypKmS = Math.sqrt(Math.pow(vInfMercKmS, 2.0) + (2.0 * muMerc / rParkMercKm));
    const dvMoiKmS = vTransArrHypKmS - vParkMercKmS;

    // Baseline direct Hohmann MOI was 10.372 km/s -> calculate savings
    const directMoiKmS = 10.372;
    const dvSavedKmS = Math.max(0.0, directMoiKmS - dvMoiKmS);

    return {
      totalTimeOfFlightDays: parseFloat(tofTotDays.toFixed(1)),
      totalTimeOfFlightYears: parseFloat(tofTotYrs.toFixed(2)),
      marsDepartureDeltaVKmS: parseFloat(dvTviKmS.toFixed(3)),
      venusFlybyDeflectionAngleDeg: parseFloat(deltaDeg.toFixed(2)),
      mercuryArrivalExcessKmS: parseFloat(vInfMercKmS.toFixed(3)),
      mercuryOrbitInsertionDeltaVKmS: parseFloat(dvMoiKmS.toFixed(3)),
      missionDeltaVSavedKmS: parseFloat(dvSavedKmS.toFixed(3)),
      mvmContext: `Mars-Venus-Mercury Gravity Assist (${tofTotDays.toFixed(0)} d Total TOF, ${deltaDeg.toFixed(1)} deg Venus Deflection, ${dvMoiKmS.toFixed(2)} km/s MOI, saved ${dvSavedKmS.toFixed(2)} km/s)`
    };
  }

  /**
   * Calculate continuous low-thrust ion/solar-electric propulsion heliocentric spiral trajectory from Mars to target solar orbit, burn time, and propellant mass.
   * Delta_V = | v_circ(r_2) - v_circ(r_1) |
   * m_f = m_0 * exp( -Delta_V / ( g_0 * I_sp ) )
   * t_burn = delta_m * ( g_0 * I_sp ) / Thrust
   * Reference: Edelbaum (1961), Larson & Wertz (1999), Curtis (2013) for Low-Thrust Continuous Trajectory Design.
   * @param {number} [initialVehicleMassKg=1500.0] - Spacecraft initial wet mass in kg (100 to 50000 kg)
   * @param {number} [thrustMillinewtons=250.0] - Continuous thruster thrust in mN (10 to 5000 mN)
   * @param {number} [specificImpulseSec=3500.0] - Ion engine specific impulse in seconds (1000 to 10000 s)
   * @param {number} [targetHeliocentricAU=1.000] - Destination heliocentric orbit in AU (0.2 to 5.5 AU)
   * @returns {{lowThrustDeltaVKmS: number, propellantConsumedKg: number, propellantFractionPercent: number, spiralDurationDays: number, spiralDurationYears: number, initialAccelerationMmS2: number, finalAccelerationMmS2: number, lowThrustContext: string}}
   */
  static computeMarsLowThrustContinuousSpiralTrajectory(initialVehicleMassKg = 1500.0, thrustMillinewtons = 250.0, specificImpulseSec = 3500.0, targetHeliocentricAU = 1.000) {
    const m0Kg = Math.max(10.0, initialVehicleMassKg);
    const ThrustN = Math.max(0.001, thrustMillinewtons / 1000.0);
    const Isp = Math.max(100.0, specificImpulseSec);
    const rTargAU = Math.max(0.1, targetHeliocentricAU);

    const AU_KM = 1.495978707e8;
    const muSun = 1.32712440018e11;
    const g0 = 9.80665;
    const cMs = g0 * Isp;
    const cKmS = cMs / 1000.0;

    const rMarsAU = 1.52368;
    const rMarsKm = rMarsAU * AU_KM;
    const rTargKm = rTargAU * AU_KM;

    // Heliocentric circular speeds
    const vMarsKmS = Math.sqrt(muSun / rMarsKm);
    const vTargKmS = Math.sqrt(muSun / rTargKm);
    const dvSpiralKmS = Math.abs(vTargKmS - vMarsKmS);
    const dvSpiralMs = dvSpiralKmS * 1000.0;

    // Rocket equation mass
    const mfKg = m0Kg * Math.exp(-dvSpiralMs / cMs);
    const deltaMKg = m0Kg - mfKg;
    const propPct = (deltaMKg / m0Kg) * 100.0;

    // Flow rate and duration
    const mdotKgS = ThrustN / cMs;
    const tBurnSec = deltaMKg / mdotKgS;
    const tBurnDays = tBurnSec / 86400.0;
    const tBurnYrs = tBurnDays / 365.25;

    // Accelerations (mm/s^2)
    const a0MmS2 = (ThrustN / m0Kg) * 1000.0;
    const afMmS2 = (ThrustN / mfKg) * 1000.0;

    return {
      lowThrustDeltaVKmS: parseFloat(dvSpiralKmS.toFixed(3)),
      propellantConsumedKg: parseFloat(deltaMKg.toFixed(2)),
      propellantFractionPercent: parseFloat(propPct.toFixed(1)),
      spiralDurationDays: parseFloat(tBurnDays.toFixed(1)),
      spiralDurationYears: parseFloat(tBurnYrs.toFixed(2)),
      initialAccelerationMmS2: parseFloat(a0MmS2.toFixed(3)),
      finalAccelerationMmS2: parseFloat(afMmS2.toFixed(3)),
      lowThrustContext: `Low-Thrust Continuous Spiral (${tBurnDays.toFixed(0)} d Spiral, ${dvSpiralKmS.toFixed(2)} km/s Delta-V, ${deltaMKg.toFixed(1)} kg Xe Fuel, ${propPct.toFixed(1)}% Fuel Mass)`
    };
  }

  /**
   * Calculate Mars-to-Sun Parker-type solar corona plunge trajectory, eccentricity, flight time, and perihelion coronal speed.
   * a = ( r_mars + r_perihelion ) / 2
   * e = ( r_mars - r_perihelion ) / ( r_mars + r_perihelion )
   * v_peri = sqrt( mu_sun * ( 2 / r_p - 1 / a ) )
   * Reference: Bate et al. (1971), Curtis (2013) for Parker Solar Probe & Helios Coronal Plunge Trajectories.
   * @param {number} [targetPerihelionSolarRadii=10.0] - Target solar closest approach in solar radii R_sun (4.0 to 100.0 R_sun)
   * @param {number} [marsParkingAltitudeKm=300.0] - Mars departure parking orbit altitude in km (150 to 1000 km)
   * @returns {{targetPerihelionSolarRadii: number, targetPerihelionAUKm: number, trajectoryEccentricity: number, timeOfFlightDays: number, timeOfFlightYears: number, transSolarInjectionDeltaVKmS: number, perihelionCoronalSpeedKmS: number, solarPlungeContext: string}}
   */
  static computeMarsToSolarCoronaPlungeTrajectory(targetPerihelionSolarRadii = 10.0, marsParkingAltitudeKm = 300.0) {
    const RsunMult = Math.max(2.0, Math.min(150.0, targetPerihelionSolarRadii));
    const hpMarsKm = Math.max(150.0, marsParkingAltitudeKm);

    const AU_KM = 1.495978707e8;
    const muSun = 1.32712440018e11;
    const muMars = 42828.37;
    const rMarsKm = 3389.5;
    const R_SUN_KM = 696340.0;

    const rMarsAU = 1.52368;
    const rMarsDistKm = rMarsAU * AU_KM;

    const rpKm = RsunMult * R_SUN_KM;
    const rpAU = rpKm / AU_KM;

    // Transfer ellipse
    const aAU = (rMarsAU + rpAU) / 2.0;
    const aKm = aAU * AU_KM;
    const ecc = (rMarsAU - rpAU) / (rMarsAU + rpAU);

    // Time of flight (days & years)
    const tofSec = Math.PI * Math.sqrt(Math.pow(aKm, 3.0) / muSun);
    const tofDays = tofSec / 86400.0;
    const tofYrs = tofDays / 365.25;

    // Speeds at Mars departure
    const vMarsKmS = Math.sqrt(muSun / rMarsDistKm);
    const vApoKmS = Math.sqrt(muSun * ((2.0 / rMarsDistKm) - (1.0 / aKm)));
    const vInfDepKmS = Math.abs(vMarsKmS - vApoKmS);

    // Trans-Solar Plunge Injection Delta-V
    const rParkKm = rMarsKm + hpMarsKm;
    const vParkCircKmS = Math.sqrt(muMars / rParkKm);
    const vTransDepHypKmS = Math.sqrt(Math.pow(vInfDepKmS, 2.0) + (2.0 * muMars / rParkKm));
    const dvTspiKmS = vTransDepHypKmS - vParkCircKmS;

    // Perihelion speed in solar corona (km/s)
    const vPeriKmS = Math.sqrt(muSun * ((2.0 / rpKm) - (1.0 / aKm)));

    return {
      targetPerihelionSolarRadii: parseFloat(RsunMult.toFixed(1)),
      targetPerihelionAUKm: parseFloat(rpAU.toFixed(4)),
      trajectoryEccentricity: parseFloat(ecc.toFixed(4)),
      timeOfFlightDays: parseFloat(tofDays.toFixed(1)),
      timeOfFlightYears: parseFloat(tofYrs.toFixed(2)),
      transSolarInjectionDeltaVKmS: parseFloat(dvTspiKmS.toFixed(3)),
      perihelionCoronalSpeedKmS: parseFloat(vPeriKmS.toFixed(2)),
      solarPlungeContext: `Solar Corona Plunge (${RsunMult.toFixed(0)} R_sun Perihelion, ${tofDays.toFixed(0)} d TOF, ${dvTspiKmS.toFixed(2)} km/s TSPI, ${vPeriKmS.toFixed(0)} km/s Coronal Speed, e=${ecc.toFixed(3)})`
    };
  }

  /**
   * Calculate Mars-to-Sun Bi-Elliptic solar drop trajectory with aphelion reverse impulse (Oberth via Outer Solar System), leg times, and total Delta-V savings.
   * TOF_tot = TOF_leg1 + TOF_leg2
   * Delta_V_tot = Delta_V_TAI + Delta_V_apo
   * Reference: Edelbaum (1959), Roth (1965), Curtis (2013) for Bi-Elliptic Heliocentric Solar Drop Trajectories.
   * @param {number} [intermediateAphelionAU=5.2044] - Outward intermediate aphelion in AU (2.5 to 30.0 AU)
   * @param {number} [targetPerihelionSolarRadii=10.0] - Target solar closest approach in solar radii R_sun (4.0 to 50.0 R_sun)
   * @param {number} [marsParkingAltitudeKm=300.0] - Mars departure parking orbit altitude in km (150 to 1000 km)
   * @returns {{totalTimeOfFlightDays: number, totalTimeOfFlightYears: number, marsDepartureDeltaVKmS: number, aphelionReverseDeltaVKmS: number, totalMissionDeltaVKmS: number, directHohmannDeltaVSavedKmS: number, biEllipticContext: string}}
   */
  static computeMarsToSunBiEllipticSolarDropTrajectory(intermediateAphelionAU = 5.2044, targetPerihelionSolarRadii = 10.0, marsParkingAltitudeKm = 300.0) {
    const raAU = Math.max(2.0, intermediateAphelionAU);
    const RsunMult = Math.max(2.0, Math.min(50.0, targetPerihelionSolarRadii));
    const hpMarsKm = Math.max(150.0, marsParkingAltitudeKm);

    const AU_KM = 1.495978707e8;
    const muSun = 1.32712440018e11;
    const muMars = 42828.37;
    const rMarsKm = 3389.5;
    const R_SUN_KM = 696340.0;

    const rMarsAU = 1.52368;
    const rMarsDistKm = rMarsAU * AU_KM;
    const raDistKm = raAU * AU_KM;

    const rpKm = RsunMult * R_SUN_KM;
    const rpAU = rpKm / AU_KM;

    // Leg 1: Mars to Aphelion
    const a1AU = (rMarsAU + raAU) / 2.0;
    const a1Km = a1AU * AU_KM;
    const tof1Sec = Math.PI * Math.sqrt(Math.pow(a1Km, 3.0) / muSun);
    const tof1Days = tof1Sec / 86400.0;

    const vMarsKmS = Math.sqrt(muSun / rMarsDistKm);
    const vPeri1KmS = Math.sqrt(muSun * ((2.0 / rMarsDistKm) - (1.0 / a1Km)));
    const vInfDepKmS = Math.abs(vPeri1KmS - vMarsKmS);

    const rParkKm = rMarsKm + hpMarsKm;
    const vParkCircKmS = Math.sqrt(muMars / rParkKm);
    const vTransDepHypKmS = Math.sqrt(Math.pow(vInfDepKmS, 2.0) + (2.0 * muMars / rParkKm));
    const dvTaiKmS = vTransDepHypKmS - vParkCircKmS;

    const va1KmS = Math.sqrt(muSun * ((2.0 / raDistKm) - (1.0 / a1Km)));

    // Leg 2: Aphelion to Solar Corona Plunge
    const a2AU = (raAU + rpAU) / 2.0;
    const a2Km = a2AU * AU_KM;
    const tof2Sec = Math.PI * Math.sqrt(Math.pow(a2Km, 3.0) / muSun);
    const tof2Days = tof2Sec / 86400.0;

    const va2KmS = Math.sqrt(muSun * ((2.0 / raDistKm) - (1.0 / a2Km)));
    const dvApoKmS = Math.abs(va1KmS - va2KmS);

    // Total metrics
    const tofTotDays = tof1Days + tof2Days;
    const tofTotYrs = tofTotDays / 365.25;
    const dvTotKmS = dvTaiKmS + dvApoKmS;

    // Baseline direct plunge TSPI was ~15.483 km/s
    const directTspiKmS = 15.483;
    const dvSavedKmS = Math.max(0.0, directTspiKmS - dvTotKmS);

    return {
      totalTimeOfFlightDays: parseFloat(tofTotDays.toFixed(1)),
      totalTimeOfFlightYears: parseFloat(tofTotYrs.toFixed(2)),
      marsDepartureDeltaVKmS: parseFloat(dvTaiKmS.toFixed(3)),
      aphelionReverseDeltaVKmS: parseFloat(dvApoKmS.toFixed(3)),
      totalMissionDeltaVKmS: parseFloat(dvTotKmS.toFixed(3)),
      directHohmannDeltaVSavedKmS: parseFloat(dvSavedKmS.toFixed(3)),
      biEllipticContext: `Bi-Elliptic Solar Drop via ${raAU.toFixed(1)} AU (${tofTotYrs.toFixed(1)} yr Total TOF, ${dvTotKmS.toFixed(2)} km/s Total Delta-V, saved ${dvSavedKmS.toFixed(2)} km/s)`
    };
  }

  /**
   * Calculate Mars-to-Pluto / Kuiper Belt Object (KBO) deep space interplanetary transfer trajectory, flight time, Trans-Pluto Injection Delta-V, and flyby mechanics.
   * a = ( r_mars + r_pluto ) / 2
   * TOF = pi * sqrt( a^3 / mu_sun )
   * Delta_V_TPI = sqrt( v_inf^2 + 2*mu/r_park ) - v_circ
   * Reference: Stern et al. (2015), Curtis (2013) for New Horizons Trans-Neptunian Trajectories.
   * @param {number} [targetPlutoDistanceAU=39.482] - Pluto/KBO heliocentric semi-major axis in AU (30.0 to 100.0 AU)
   * @param {number} [marsParkingAltitudeKm=300.0] - Mars departure parking orbit altitude in km (150 to 1000 km)
   * @param {number} [plutoFlybyAltitudeKm=1000.0] - Pluto flyby closest approach altitude in km (200 to 10000 km)
   * @returns {{transferSemiMajorAxisAU: number, trajectoryEccentricity: number, timeOfFlightDays: number, timeOfFlightYears: number, transPlutoInjectionDeltaVKmS: number, plutoArrivalExcessKmS: number, plutoFlybyDeflectionAngleDeg: number, plutoTransferContext: string}}
   */
  static computeMarsToPlutoDeepSpaceTransferTrajectory(targetPlutoDistanceAU = 39.482, marsParkingAltitudeKm = 300.0, plutoFlybyAltitudeKm = 1000.0) {
    const rPlutoAU = Math.max(25.0, Math.min(120.0, targetPlutoDistanceAU));
    const hpMarsKm = Math.max(150.0, marsParkingAltitudeKm);
    const hpPlutoKm = Math.max(100.0, plutoFlybyAltitudeKm);

    const AU_KM = 1.495978707e8;
    const muSun = 1.32712440018e11;
    const muMars = 42828.37;
    const rMarsKm = 3389.5;
    const muPluto = 869.6; // km^3/s^2
    const rPlutoKm = 1188.3;

    const rMarsAU = 1.52368;
    const rMarsDistKm = rMarsAU * AU_KM;
    const rPlutoDistKm = rPlutoAU * AU_KM;

    // Transfer ellipse
    const aAU = (rMarsAU + rPlutoAU) / 2.0;
    const aKm = aAU * AU_KM;
    const ecc = (rPlutoAU - rMarsAU) / (rPlutoAU + rMarsAU);

    // Time of flight (days & years)
    const tofSec = Math.PI * Math.sqrt(Math.pow(aKm, 3.0) / muSun);
    const tofDays = tofSec / 86400.0;
    const tofYrs = tofDays / 365.25;

    // Mars departure speeds
    const vMarsKmS = Math.sqrt(muSun / rMarsDistKm);
    const vPeriKmS = Math.sqrt(muSun * ((2.0 / rMarsDistKm) - (1.0 / aKm)));
    const vInfDepKmS = Math.abs(vPeriKmS - vMarsKmS);

    // Trans-Pluto Injection Delta-V
    const rParkKm = rMarsKm + hpMarsKm;
    const vParkCircKmS = Math.sqrt(muMars / rParkKm);
    const vTransDepHypKmS = Math.sqrt(Math.pow(vInfDepKmS, 2.0) + (2.0 * muMars / rParkKm));
    const dvTpiKmS = vTransDepHypKmS - vParkCircKmS;

    // Pluto arrival excess
    const vPlutoCircKmS = Math.sqrt(muSun / rPlutoDistKm);
    const vApoKmS = Math.sqrt(muSun * ((2.0 / rPlutoDistKm) - (1.0 / aKm)));
    const vInfArrKmS = Math.abs(vPlutoCircKmS - vApoKmS);

    // Pluto flyby deflection
    const rpFlybyKm = rPlutoKm + hpPlutoKm;
    const eFlyby = 1.0 + ((rpFlybyKm * Math.pow(vInfArrKmS, 2.0)) / muPluto);
    const deltaRad = 2.0 * Math.asin(1.0 / eFlyby);
    const deltaDeg = deltaRad * (180.0 / Math.PI);

    return {
      transferSemiMajorAxisAU: parseFloat(aAU.toFixed(3)),
      trajectoryEccentricity: parseFloat(ecc.toFixed(4)),
      timeOfFlightDays: parseFloat(tofDays.toFixed(1)),
      timeOfFlightYears: parseFloat(tofYrs.toFixed(2)),
      transPlutoInjectionDeltaVKmS: parseFloat(dvTpiKmS.toFixed(3)),
      plutoArrivalExcessKmS: parseFloat(vInfArrKmS.toFixed(3)),
      plutoFlybyDeflectionAngleDeg: parseFloat(deltaDeg.toFixed(2)),
      plutoTransferContext: `Trans-Pluto Transfer (${tofYrs.toFixed(1)} yr TOF to ${rPlutoAU.toFixed(1)} AU, ${dvTpiKmS.toFixed(2)} km/s TPI, ${vInfArrKmS.toFixed(2)} km/s Pluto Arrival, ${deltaDeg.toFixed(2)} deg Flyby Turn)`
    };
  }

  /**
   * Calculate theoretical absolute minimum Delta-V Bi-Parabolic solar drop trajectory (Solar Escape to Infinity, infinitesimal stop, radial inward drop).
   * v_esc = sqrt( 2 * mu_sun / r_mars )
   * v_inf = ( sqrt(2) - 1 ) * v_circ
   * Delta_V_tot = Delta_V_TSEI
   * Reference: Edelbaum (1959), Escobal (1965), Prussing & Conway (1993), Curtis (2013) for Bi-Parabolic Heliocentric Transfer Limits.
   * @param {number} [targetPerihelionSolarRadii=10.0] - Solar closest approach in solar radii R_sun (2.0 to 50.0 R_sun)
   * @param {number} [marsParkingAltitudeKm=300.0] - Mars departure parking orbit altitude in km (150 to 1000 km)
   * @returns {{solarEscapeSpeedAtMarsKmS: number, marsDepartureExcessKmS: number, transSolarEscapeInjectionDeltaVKmS: number, aphelionInfinityDeltaVKmS: number, totalMissionDeltaVKmS: number, coronalPerihelionSpeedKmS: number, hohmannDeltaVSavedKmS: number, biParabolicContext: string}}
   */
  static computeMarsToSunBiParabolicSolarDropTrajectory(targetPerihelionSolarRadii = 10.0, marsParkingAltitudeKm = 300.0) {
    const RsunMult = Math.max(2.0, Math.min(50.0, targetPerihelionSolarRadii));
    const hpMarsKm = Math.max(150.0, marsParkingAltitudeKm);

    const AU_KM = 1.495978707e8;
    const muSun = 1.32712440018e11;
    const muMars = 42828.37;
    const rMarsKm = 3389.5;
    const R_SUN_KM = 696340.0;

    const rMarsAU = 1.52368;
    const rMarsDistKm = rMarsAU * AU_KM;

    const rpKm = RsunMult * R_SUN_KM;

    // Solar circular and escape speeds at Mars
    const vMarsCircKmS = Math.sqrt(muSun / rMarsDistKm);
    const vMarsEscKmS = Math.sqrt(2.0) * vMarsCircKmS;
    const vInfDepKmS = (Math.sqrt(2.0) - 1.0) * vMarsCircKmS;

    // Trans-Solar Escape Injection Delta-V
    const rParkKm = rMarsKm + hpMarsKm;
    const vParkCircKmS = Math.sqrt(muMars / rParkKm);
    const vTransDepHypKmS = Math.sqrt(Math.pow(vInfDepKmS, 2.0) + (2.0 * muMars / rParkKm));
    const dvTseiKmS = vTransDepHypKmS - vParkCircKmS;

    // At r -> infinity, delta_v_apo is identically 0
    const dvApoKmS = 0.0;
    const dvTotKmS = dvTseiKmS + dvApoKmS;

    // Coronal perihelion speed on parabolic drop (km/s)
    const vPeriCoronalKmS = Math.sqrt((2.0 * muSun) / rpKm);

    // Direct Hohmann baseline was ~15.483 km/s
    const directHohmannKmS = 15.483;
    const dvSavedKmS = Math.max(0.0, directHohmannKmS - dvTotKmS);

    return {
      solarEscapeSpeedAtMarsKmS: parseFloat(vMarsEscKmS.toFixed(3)),
      marsDepartureExcessKmS: parseFloat(vInfDepKmS.toFixed(3)),
      transSolarEscapeInjectionDeltaVKmS: parseFloat(dvTseiKmS.toFixed(3)),
      aphelionInfinityDeltaVKmS: 0.0,
      totalMissionDeltaVKmS: parseFloat(dvTotKmS.toFixed(3)),
      coronalPerihelionSpeedKmS: parseFloat(vPeriCoronalKmS.toFixed(2)),
      hohmannDeltaVSavedKmS: parseFloat(dvSavedKmS.toFixed(3)),
      biParabolicContext: `Bi-Parabolic Solar Drop (${dvTotKmS.toFixed(2)} km/s Total Delta-V, ${vPeriCoronalKmS.toFixed(0)} km/s Coronal Entry at ${RsunMult.toFixed(0)} R_sun, saved ${dvSavedKmS.toFixed(2)} km/s)`
    };
  }

  /**
   * Calculate inward Mars-to-Inner Planet low-thrust continuous ion spiral trajectory with solar panel inverse-square power/thrust scaling (T ~ 1/r^2).
   * T(r) = T_1AU / r^2
   * T_mean = T_1AU / ( r_1 * r_2 )
   * t_burn = delta_m * c / T_mean
   * Reference: Sauer (1973), Williams & Coverstone (2000), Curtis (2013) for Solar-Electric Low-Thrust Scaling.
   * @param {number} [initialVehicleMassKg=1500.0] - Initial wet mass in kg (100 to 50000 kg)
   * @param {number} [thrustAt1AUmN=300.0] - Solar electric propulsion thrust at 1 AU in mN (10 to 5000 mN)
   * @param {number} [specificImpulseSec=3500.0] - Ion thruster specific impulse in seconds (1000 to 10000 s)
   * @param {number} [targetHeliocentricAU=1.000] - Destination heliocentric orbit in AU (0.3 to 1.4 AU)
   * @returns {{lowThrustDeltaVKmS: number, propellantConsumedKg: number, initialMarsThrustMN: number, finalArrivalThrustMN: number, averageThrustMN: number, spiralDurationDays: number, spiralDurationYears: number, initialAccelerationMmS2: number, finalAccelerationMmS2: number, solarSpiralContext: string}}
   */
  static computeMarsInwardSolarElectricIonSpiralWithSolarScaling(initialVehicleMassKg = 1500.0, thrustAt1AUmN = 300.0, specificImpulseSec = 3500.0, targetHeliocentricAU = 1.000) {
    const m0Kg = Math.max(10.0, initialVehicleMassKg);
    const T1auN = Math.max(0.001, thrustAt1AUmN / 1000.0);
    const Isp = Math.max(100.0, specificImpulseSec);
    const rTargAU = Math.max(0.2, targetHeliocentricAU);

    const AU_KM = 1.495978707e8;
    const muSun = 1.32712440018e11;
    const g0 = 9.80665;
    const cMs = g0 * Isp;

    const rMarsAU = 1.52368;
    const rMarsDistKm = rMarsAU * AU_KM;
    const rTargDistKm = rTargAU * AU_KM;

    // Speeds
    const vMarsKmS = Math.sqrt(muSun / rMarsDistKm);
    const vTargKmS = Math.sqrt(muSun / rTargDistKm);
    const dvSpiralKmS = Math.abs(vTargKmS - vMarsKmS);
    const dvSpiralMs = dvSpiralKmS * 1000.0;

    // Fuel consumed
    const mfKg = m0Kg * Math.exp(-dvSpiralMs / cMs);
    const deltaMKg = m0Kg - mfKg;

    // Thrust scaling (N)
    const TmarsN = T1auN / Math.pow(rMarsAU, 2.0);
    const TtargN = T1auN / Math.pow(rTargAU, 2.0);
    const TavgN = T1auN / (rMarsAU * rTargAU);

    // Duration with average solar-scaled thrust
    const mdotAvgKgS = TavgN / cMs;
    const tBurnSec = deltaMKg / mdotAvgKgS;
    const tBurnDays = tBurnSec / 86400.0;
    const tBurnYrs = tBurnDays / 365.25;

    // Accelerations (mm/s^2)
    const a0MmS2 = (TmarsN / m0Kg) * 1000.0;
    const afMmS2 = (TtargN / mfKg) * 1000.0;

    return {
      lowThrustDeltaVKmS: parseFloat(dvSpiralKmS.toFixed(3)),
      propellantConsumedKg: parseFloat(deltaMKg.toFixed(2)),
      initialMarsThrustMN: parseFloat((TmarsN * 1000.0).toFixed(1)),
      finalArrivalThrustMN: parseFloat((TtargN * 1000.0).toFixed(1)),
      averageThrustMN: parseFloat((TavgN * 1000.0).toFixed(1)),
      spiralDurationDays: parseFloat(tBurnDays.toFixed(1)),
      spiralDurationYears: parseFloat(tBurnYrs.toFixed(2)),
      initialAccelerationMmS2: parseFloat(a0MmS2.toFixed(3)),
      finalAccelerationMmS2: parseFloat(afMmS2.toFixed(3)),
      solarSpiralContext: `Solar-Scaled SEP Spiral (${tBurnDays.toFixed(0)} d to ${rTargAU.toFixed(2)} AU, ${deltaMKg.toFixed(1)} kg Xe, ${(TmarsN * 1000).toFixed(0)} mN -> ${(TtargN * 1000).toFixed(0)} mN)`
    };
  }

  /**
   * Calculate inward Mars-to-Inner Planet low-thrust continuous ion spiral trajectory coupled with outward Solar Radiation Pressure (SRP) perturbation and lightness parameter.
   * P_srp = ( Phi_0 / c ) * C_R
   * beta_light = F_srp / F_grav
   * mu_eff = mu_sun * ( 1 - beta_light )
   * Reference: McInnes (1999), Wright (1992), Curtis (2013) for Solar Radiation Pressure Perturbed Trajectories.
   * @param {number} [initialVehicleMassKg=1500.0] - Initial wet mass in kg (100 to 50000 kg)
   * @param {number} [thrustMillinewtons=250.0] - Continuous ion engine thrust in mN (10 to 5000 mN)
   * @param {number} [specificImpulseSec=3500.0] - Specific impulse in seconds (1000 to 10000 s)
   * @param {number} [solarSailAreaM2=100.0] - Solar array / sail projected area in m^2 (10 to 5000 m^2)
   * @param {number} [targetHeliocentricAU=1.000] - Target heliocentric orbit in AU (0.2 to 1.4 AU)
   * @returns {{lowThrustDeltaVKmS: number, propellantConsumedKg: number, srpForceAt1AUMillitewtons: number, solarLightnessBeta: number, effectiveMuRatio: number, spiralDurationDays: number, spiralDurationYears: number, srpSpiralContext: string}}
   */
  static computeMarsInwardIonSpiralWithSolarRadiationPressure(initialVehicleMassKg = 1500.0, thrustMillinewtons = 250.0, specificImpulseSec = 3500.0, solarSailAreaM2 = 100.0, targetHeliocentricAU = 1.000) {
    const m0Kg = Math.max(10.0, initialVehicleMassKg);
    const ThrustN = Math.max(0.001, thrustMillinewtons / 1000.0);
    const Isp = Math.max(100.0, specificImpulseSec);
    const AreaM2 = Math.max(1.0, solarSailAreaM2);
    const rTargAU = Math.max(0.1, targetHeliocentricAU);

    const AU_KM = 1.495978707e8;
    const AU_M = 1.495978707e11;
    const muSun = 1.32712440018e11; // km^3/s^2
    const g0 = 9.80665;
    const cMs = g0 * Isp;

    const cLight = 2.99792458e8; // m/s
    const Phi0 = 1361.0; // W/m^2 at 1 AU
    const CR = 1.85; // Reflection radiation coefficient

    // SRP Force at 1 AU (N and mN)
    const Psrp1au = (Phi0 / cLight) * CR; // N/m^2
    const Fsrp1auN = Psrp1au * AreaM2;
    const Fsrp1auMN = Fsrp1auN * 1000.0;

    // Solar lightness parameter beta = F_srp / F_grav
    const Fgrav1auN = (1.32712440018e20 * m0Kg) / Math.pow(AU_M, 2.0); // N
    const betaLight = Fsrp1auN / Fgrav1auN;
    const muRatio = Math.max(0.0, 1.0 - betaLight);
    const muEff = muSun * muRatio;

    // Speeds with effective gravity
    const rMarsAU = 1.52368;
    const rMarsKm = rMarsAU * AU_KM;
    const rTargKm = rTargAU * AU_KM;

    const vMarsEffKmS = Math.sqrt(muEff / rMarsKm);
    const vTargEffKmS = Math.sqrt(muEff / rTargKm);
    const dvSpiralKmS = Math.abs(vTargEffKmS - vMarsEffKmS);
    const dvSpiralMs = dvSpiralKmS * 1000.0;

    // Propellant mass & duration
    const mfKg = m0Kg * Math.exp(-dvSpiralMs / cMs);
    const deltaMKg = m0Kg - mfKg;

    const mdotKgS = ThrustN / cMs;
    const tBurnSec = deltaMKg / mdotKgS;
    const tBurnDays = tBurnSec / 86400.0;
    const tBurnYrs = tBurnDays / 365.25;

    return {
      lowThrustDeltaVKmS: parseFloat(dvSpiralKmS.toFixed(3)),
      propellantConsumedKg: parseFloat(deltaMKg.toFixed(2)),
      srpForceAt1AUMillitewtons: parseFloat(Fsrp1auMN.toFixed(3)),
      solarLightnessBeta: parseFloat(betaLight.toExponential(3)),
      effectiveMuRatio: parseFloat(muRatio.toFixed(6)),
      spiralDurationDays: parseFloat(tBurnDays.toFixed(1)),
      spiralDurationYears: parseFloat(tBurnYrs.toFixed(2)),
      srpSpiralContext: `SRP-Perturbed Inward Spiral (${tBurnDays.toFixed(0)} d to ${rTargAU.toFixed(2)} AU, ${dvSpiralKmS.toFixed(2)} km/s Delta-V, beta=${betaLight.toExponential(2)})`
    };
  }

  /**
   * Calculate continuous low-thrust ion engine inward spiral transit times, propellant consumption, and cumulative Delta-V to Earth, Venus, and Mercury.
   * Delta_V(r) = | v_circ(r) - v_circ(r_mars) |
   * m(r) = m_0 * exp( -Delta_V(r) / ( g_0 * I_sp ) )
   * t(r) = ( m_0 - m(r) ) / m_dot
   * Reference: Edelbaum (1961), Larson & Wertz (1999), Curtis (2013) for Low-Thrust Interplanetary Tour Design.
   * @param {number} [initialVehicleMassKg=1500.0] - Initial wet mass in kg (100 to 50000 kg)
   * @param {number} [thrustMillinewtons=250.0] - Continuous thruster thrust in mN (10 to 5000 mN)
   * @param {number} [specificImpulseSec=3500.0] - Ion engine specific impulse in seconds (1000 to 10000 s)
   * @returns {{earthTransitDays: number, earthTransitYears: number, earthDeltaVKmS: number, earthPropellantKg: number, venusTransitDays: number, venusTransitYears: number, venusDeltaVKmS: number, venusPropellantKg: number, mercuryTransitDays: number, mercuryTransitYears: number, mercuryDeltaVKmS: number, mercuryPropellantKg: number, multiPlanetContext: string}}
   */
  static computeMarsInwardLowThrustPlanetaryTransitTimes(initialVehicleMassKg = 1500.0, thrustMillinewtons = 250.0, specificImpulseSec = 3500.0) {
    const m0Kg = Math.max(10.0, initialVehicleMassKg);
    const ThrustN = Math.max(0.001, thrustMillinewtons / 1000.0);
    const Isp = Math.max(100.0, specificImpulseSec);

    const AU_KM = 1.495978707e8;
    const muSun = 1.32712440018e11;
    const g0 = 9.80665;
    const cMs = g0 * Isp;
    const mdotKgS = ThrustN / cMs;

    const rMarsKm = 1.52368 * AU_KM;
    const vMarsKmS = Math.sqrt(muSun / rMarsKm);

    // Planet orbits (Earth 1.0 AU, Venus 0.72333 AU, Mercury 0.38710 AU)
    const planets = [
      { name: 'Earth', rAU: 1.00000 },
      { name: 'Venus', rAU: 0.72333 },
      { name: 'Mercury', rAU: 0.38710 }
    ];

    const results = planets.map(p => {
      const rKm = p.rAU * AU_KM;
      const vCircKmS = Math.sqrt(muSun / rKm);
      const dvKmS = Math.abs(vCircKmS - vMarsKmS);
      const dvMs = dvKmS * 1000.0;

      const mfKg = m0Kg * Math.exp(-dvMs / cMs);
      const dMKg = m0Kg - mfKg;
      const tSec = dMKg / mdotKgS;
      const tDays = tSec / 86400.0;
      const tYrs = tDays / 365.25;

      return {
        name: p.name,
        dvKmS,
        dMKg,
        tDays,
        tYrs
      };
    });

    const [eRes, vRes, mRes] = results;

    return {
      earthTransitDays: parseFloat(eRes.tDays.toFixed(1)),
      earthTransitYears: parseFloat(eRes.tYrs.toFixed(2)),
      earthDeltaVKmS: parseFloat(eRes.dvKmS.toFixed(3)),
      earthPropellantKg: parseFloat(eRes.dMKg.toFixed(2)),
      venusTransitDays: parseFloat(vRes.tDays.toFixed(1)),
      venusTransitYears: parseFloat(vRes.tYrs.toFixed(2)),
      venusDeltaVKmS: parseFloat(vRes.dvKmS.toFixed(3)),
      venusPropellantKg: parseFloat(vRes.dMKg.toFixed(2)),
      mercuryTransitDays: parseFloat(mRes.tDays.toFixed(1)),
      mercuryTransitYears: parseFloat(mRes.tYrs.toFixed(2)),
      mercuryDeltaVKmS: parseFloat(mRes.dvKmS.toFixed(3)),
      mercuryPropellantKg: parseFloat(mRes.dMKg.toFixed(2)),
      multiPlanetContext: `Inward Low-Thrust Tour (Earth in ${eRes.tDays.toFixed(0)}d, Venus in ${vRes.tDays.toFixed(0)}d, Mercury in ${mRes.tDays.toFixed(0)}d, ${mRes.dMKg.toFixed(0)}kg Xe Total)`
    };
  }

  /**
   * Calculate inward Mars-to-Venus hyperbolic gravity assist flyby, deflection angle, Delta-V gain, and post-flyby resonant solar orbit pumping.
   * sin( delta / 2 ) = 1 / ( 1 + ( r_p * v_inf^2 / mu_V ) )
   * Delta_V_assist = 2 * v_inf * sin( delta / 2 )
   * a_post = 1 / ( 2/r_V - v_post^2/mu_sun )
   * Reference: Broucke (1988), Strange & Longuski (2002), Curtis (2013) for Planetary Gravity Assist Pumping.
   * @param {number} [flybyPeriapsisAltitudeKm=300.0] - Venus close approach altitude in km (150 to 5000 km)
   * @param {number} [marsParkingAltitudeKm=300.0] - Mars departure altitude in km (150 to 1000 km)
   * @returns {{transferTimeDays: number, venusArrivalHyperbolicExcessKmS: number, flybyDeflectionAngleDeg: number, gravityAssistDeltaVKmS: number, postFlybyPerihelionAU: number, postFlybySemiMajorAxisAU: number, postFlybyPeriodDays: number, gravityAssistContext: string}}
   */
  static computeMarsToVenusGravityAssistResonantOrbit(flybyPeriapsisAltitudeKm = 300.0, marsParkingAltitudeKm = 300.0) {
    const hpVKm = Math.max(150.0, flybyPeriapsisAltitudeKm);
    const hpMarsKm = Math.max(150.0, marsParkingAltitudeKm);

    const AU_KM = 1.495978707e8;
    const muSun = 1.32712440018e11;
    const muVenus = 324858.6;
    const rVenusKm = 6051.8;

    const rMarsAU = 1.52368;
    const rVenusAU = 0.72333;
    const rMarsKm = rMarsAU * AU_KM;
    const rVenusDistKm = rVenusAU * AU_KM;

    // Hohmann transfer to Venus
    const aTransKm = (rMarsKm + rVenusDistKm) / 2.0;
    const tofsSec = Math.PI * Math.sqrt(Math.pow(aTransKm, 3.0) / muSun);
    const tofsDays = tofsSec / 86400.0;

    // Velocities at Venus
    const vVenusArrKmS = Math.sqrt(muSun * ((2.0 / rVenusDistKm) - (1.0 / aTransKm)));
    const vVenusCircKmS = Math.sqrt(muSun / rVenusDistKm);
    const vInfVKmS = Math.abs(vVenusArrKmS - vVenusCircKmS);

    // Flyby deflection
    const rpVKm = rVenusKm + hpVKm;
    const sinHalfDelta = 1.0 / (1.0 + ((rpVKm * Math.pow(vInfVKmS, 2.0)) / muVenus));
    const deltaRad = 2.0 * Math.asin(Math.min(1.0, Math.max(0.0, sinHalfDelta)));
    const deltaDeg = deltaRad * (180.0 / Math.PI);

    const dvAssistKmS = 2.0 * vInfVKmS * Math.sin(deltaRad / 2.0);

    // Post-flyby heliocentric speed (inward bending)
    const vPostKmS = Math.sqrt(
      Math.pow(vVenusCircKmS, 2.0) +
      Math.pow(vInfVKmS, 2.0) -
      (2.0 * vVenusCircKmS * vInfVKmS * Math.sin(deltaRad / 2.0))
    );

    // Post-flyby heliocentric orbit
    const aPostKm = 1.0 / ((2.0 / rVenusDistKm) - (Math.pow(vPostKmS, 2.0) / muSun));
    const aPostAU = aPostKm / AU_KM;
    const rpPostKm = (2.0 * aPostKm) - rVenusDistKm;
    const rpPostAU = rpPostKm / AU_KM;

    const tPeriodSec = 2.0 * Math.PI * Math.sqrt(Math.pow(aPostKm, 3.0) / muSun);
    const tPeriodDays = tPeriodSec / 86400.0;

    return {
      transferTimeDays: parseFloat(tofsDays.toFixed(1)),
      venusArrivalHyperbolicExcessKmS: parseFloat(vInfVKmS.toFixed(3)),
      flybyDeflectionAngleDeg: parseFloat(deltaDeg.toFixed(2)),
      gravityAssistDeltaVKmS: parseFloat(dvAssistKmS.toFixed(3)),
      postFlybyPerihelionAU: parseFloat(rpPostAU.toFixed(3)),
      postFlybySemiMajorAxisAU: parseFloat(aPostAU.toFixed(3)),
      postFlybyPeriodDays: parseFloat(tPeriodDays.toFixed(1)),
      gravityAssistContext: `Venus Gravity Assist (${tofsDays.toFixed(0)} d Transfer, ${deltaDeg.toFixed(1)} deg Bend, ${dvAssistKmS.toFixed(2)} km/s Assist, Perihelion -> ${rpPostAU.toFixed(2)} AU)`
    };
  }

  /**
   * Calculate continuous radial low-thrust propulsion perturbation, effective gravity reduction, and apsidal line precession rate.
   * a_r = T_r / m
   * mu_eff = mu_sun * ( 1 - a_r / g_sun )
   * dot_omega = 2 * pi * a_r / g_sun (per revolution)
   * Reference: Kechichian (1997), Petropoulos & Longuski (2004), Curtis (2013) for Radial Low-Thrust Steering.
   * @param {number} [initialOrbitAU=1.52368] - Initial heliocentric circular orbit in AU (0.2 to 10.0 AU)
   * @param {number} [initialMassKg=1500.0] - Spacecraft mass in kg (100 to 50000 kg)
   * @param {number} [thrustMN=300.0] - Continuous radial thrust in mN (10 to 5000 mN)
   * @param {number} [specificImpulseSec=3500.0] - Thruster Isp in seconds (1000 to 10000 s)
   * @param {number} [burnDurationDays=180.0] - Radial burn duration in days (1 to 1000 days)
   * @returns {{radialAccelerationMmS2: number, solarGravityRatioPercent: number, effectiveCircularSpeedKmS: number, apsidalPrecessionDegPerYear: number, cumulativeApsidalShiftDeg: number, propellantConsumedKg: number, radialSteeringContext: string}}
   */
  static computeLowThrustRadialThrustOrbitModification(initialOrbitAU = 1.52368, initialMassKg = 1500.0, thrustMN = 300.0, specificImpulseSec = 3500.0, burnDurationDays = 180.0) {
    const rAU = Math.max(0.1, initialOrbitAU);
    const m0Kg = Math.max(10.0, initialMassKg);
    const ThrustN = Math.max(0.001, thrustMN / 1000.0);
    const Isp = Math.max(100.0, specificImpulseSec);
    const tDays = Math.max(1.0, burnDurationDays);

    const AU_M = 1.495978707e11;
    const muSunM = 1.32712440018e20; // m^3/s^2
    const g0 = 9.80665;
    const cMs = g0 * Isp;

    const rM = rAU * AU_M;

    // Solar gravity at orbit (m/s^2)
    const gSunMs2 = muSunM / Math.pow(rM, 2.0);

    // Radial acceleration (m/s^2 & mm/s^2)
    const arMs2 = ThrustN / m0Kg;
    const arMmS2 = arMs2 * 1000.0;

    // Gravity reduction ratio (%)
    const gravRatioFrac = Math.min(0.99, arMs2 / gSunMs2);
    const gravRatioPct = gravRatioFrac * 100.0;

    // Effective circular speed (km/s)
    const muEffM = muSunM * (1.0 - gravRatioFrac);
    const vCircEffMs = Math.sqrt(muEffM / rM);
    const vCircEffKmS = vCircEffMs / 1000.0;

    // Orbital period (years)
    const tPeriodYr = Math.pow(rAU, 1.5);

    // Apsidal precession rate (deg/yr and cumulative deg)
    const dotOmegaDegYr = (gravRatioFrac * 360.0) / tPeriodYr;
    const tYr = tDays / 365.25;
    const cumulativeShiftDeg = dotOmegaDegYr * tYr;

    // Propellant consumed (kg)
    const mdotKgS = ThrustN / cMs;
    const propKg = mdotKgS * (tDays * 86400.0);

    return {
      radialAccelerationMmS2: parseFloat(arMmS2.toFixed(3)),
      solarGravityRatioPercent: parseFloat(gravRatioPct.toFixed(2)),
      effectiveCircularSpeedKmS: parseFloat(vCircEffKmS.toFixed(3)),
      apsidalPrecessionDegPerYear: parseFloat(dotOmegaDegYr.toFixed(2)),
      cumulativeApsidalShiftDeg: parseFloat(cumulativeShiftDeg.toFixed(2)),
      propellantConsumedKg: parseFloat(propKg.toFixed(2)),
      radialSteeringContext: `Radial Low-Thrust (${gravRatioPct.toFixed(1)}% Gravity Offset, ${dotOmegaDegYr.toFixed(1)} deg/yr Precession, ${propKg.toFixed(1)} kg Xe)`
    };
  }

  /**
   * Calculate direct high-energy Hohmann plunge transfer from Mars to innermost planet Mercury, including departure and orbit insertion burns.
   * a_trans = ( r_mars + r_merc ) / 2
   * TOF = pi * sqrt( a_trans^3 / mu_sun )
   * Delta_V_tot = Delta_V_TMI + Delta_V_MOI
   * Reference: Curtis (2013), Larson & Wertz (1999) for Direct Interplanetary Hohmann Transfers.
   * @param {number} [marsParkingAltitudeKm=300.0] - Mars departure parking orbit altitude in km (150 to 1000 km)
   * @param {number} [mercuryParkingAltitudeKm=200.0] - Mercury arrival orbit altitude in km (100 to 2000 km)
   * @returns {{transferTimeDays: number, marsDepartureDeltaVKmS: number, mercuryArrivalExcessKmS: number, mercuryOrbitInsertionDeltaVKmS: number, totalMissionDeltaVKmS: number, transferEccentricity: number, transferSemiMajorAxisAU: number, directTransferContext: string}}
   */
  static computeMarsToMercuryDirectPlungeTransfer(marsParkingAltitudeKm = 300.0, mercuryParkingAltitudeKm = 200.0) {
    const hpMarsKm = Math.max(150.0, marsParkingAltitudeKm);
    const hpMercKm = Math.max(100.0, mercuryParkingAltitudeKm);

    const AU_KM = 1.495978707e8;
    const muSun = 1.32712440018e11;
    const muMars = 42828.37;
    const rMarsKm = 3389.5;
    const muMerc = 22032.09;
    const rMercKm = 2439.7;

    const rMarsAU = 1.52368;
    const rMercAU = 0.38710;
    const rMarsDistKm = rMarsAU * AU_KM;
    const rMercDistKm = rMercAU * AU_KM;

    // Hohmann transfer geometry
    const aTransKm = (rMarsDistKm + rMercDistKm) / 2.0;
    const aTransAU = aTransKm / AU_KM;
    const eTrans = (rMarsDistKm - rMercDistKm) / (rMarsDistKm + rMercDistKm);

    const tofsSec = Math.PI * Math.sqrt(Math.pow(aTransKm, 3.0) / muSun);
    const tofsDays = tofsSec / 86400.0;

    // Speeds at Mars departure
    const vMarsCircKmS = Math.sqrt(muSun / rMarsDistKm);
    const vDepKmS = Math.sqrt(muSun * ((2.0 / rMarsDistKm) - (1.0 / aTransKm)));
    const vInfMarsKmS = Math.abs(vMarsCircKmS - vDepKmS);

    const rParkMarsKm = rMarsKm + hpMarsKm;
    const vParkMarsKmS = Math.sqrt(muMars / rParkMarsKm);
    const vHypMarsKmS = Math.sqrt(Math.pow(vInfMarsKmS, 2.0) + ((2.0 * muMars) / rParkMarsKm));
    const dvTmiKmS = vHypMarsKmS - vParkMarsKmS;

    // Speeds at Mercury arrival
    const vMercCircKmS = Math.sqrt(muSun / rMercDistKm);
    const vArrKmS = Math.sqrt(muSun * ((2.0 / rMercDistKm) - (1.0 / aTransKm)));
    const vInfMercKmS = Math.abs(vArrKmS - vMercCircKmS);

    const rParkMercKm = rMercKm + hpMercKm;
    const vParkMercKmS = Math.sqrt(muMerc / rParkMercKm);
    const vHypMercKmS = Math.sqrt(Math.pow(vInfMercKmS, 2.0) + ((2.0 * muMerc) / rParkMercKm));
    const dvMoiKmS = vHypMercKmS - vParkMercKmS;

    const dvTotKmS = dvTmiKmS + dvMoiKmS;

    return {
      transferTimeDays: parseFloat(tofsDays.toFixed(1)),
      marsDepartureDeltaVKmS: parseFloat(dvTmiKmS.toFixed(3)),
      mercuryArrivalExcessKmS: parseFloat(vInfMercKmS.toFixed(3)),
      mercuryOrbitInsertionDeltaVKmS: parseFloat(dvMoiKmS.toFixed(3)),
      totalMissionDeltaVKmS: parseFloat(dvTotKmS.toFixed(3)),
      transferEccentricity: parseFloat(eTrans.toFixed(4)),
      transferSemiMajorAxisAU: parseFloat(aTransAU.toFixed(3)),
      directTransferContext: `Mars-to-Mercury Direct (${tofsDays.toFixed(0)} d Transfer, e=${eTrans.toFixed(2)}, ${dvTotKmS.toFixed(2)} km/s Total Delta-V)`
    };
  }

  /**
   * Calculate inward Mars-to-Venus low-thrust continuous ion spiral trajectory with optimal thrust pitch steering angle modulation.
   * Delta_V_opt = Delta_V_tang / sqrt( eta_steer )
   * m_f = m_0 * exp( -Delta_V_opt / c )
   * t_burn = delta_m / m_dot
   * Reference: Petropoulos & Longuski (2004), Betts (2010), Curtis (2013) for Optimal Low-Thrust Trajectories.
   * @param {number} [initialMassKg=1500.0] - Initial wet mass in kg (100 to 50000 kg)
   * @param {number} [thrustMN=300.0] - Continuous thruster thrust in mN (10 to 5000 mN)
   * @param {number} [specificImpulseSec=3500.0] - Thruster Isp in seconds (1000 to 10000 s)
   * @returns {{optimalLowThrustDeltaVKmS: number, propellantConsumedKg: number, spiralDurationDays: number, spiralDurationYears: number, meanPitchSteeringAngleDeg: number, steeringEfficiencyPercent: number, optimalSteeringContext: string}}
   */
  static computeMarsToVenusOptimumSteeringAngleIonSpiral(initialMassKg = 1500.0, thrustMN = 300.0, specificImpulseSec = 3500.0) {
    const m0Kg = Math.max(10.0, initialMassKg);
    const ThrustN = Math.max(0.001, thrustMN / 1000.0);
    const Isp = Math.max(100.0, specificImpulseSec);

    const AU_KM = 1.495978707e8;
    const muSun = 1.32712440018e11;
    const g0 = 9.80665;
    const cMs = g0 * Isp;
    const mdotKgS = ThrustN / cMs;

    const rMarsAU = 1.52368;
    const rVenusAU = 0.72333;
    const rMarsKm = rMarsAU * AU_KM;
    const rVenusKm = rVenusAU * AU_KM;

    const vMarsKmS = Math.sqrt(muSun / rMarsKm);
    const vVenusKmS = Math.sqrt(muSun / rVenusKm);
    const dvTangKmS = Math.abs(vVenusKmS - vMarsKmS);

    // Optimal steering parameterization
    const etaSteer = 0.945;
    const dvOptKmS = dvTangKmS / Math.sqrt(etaSteer);
    const dvOptMs = dvOptKmS * 1000.0;

    // Mass depletion
    const mfKg = m0Kg * Math.exp(-dvOptMs / cMs);
    const deltaMKg = m0Kg - mfKg;

    // Burn duration
    const tBurnSec = deltaMKg / mdotKgS;
    const tBurnDays = tBurnSec / 86400.0;
    const tBurnYrs = tBurnDays / 365.25;

    // Mean optimal pitch angle (deg)
    const betaMeanDeg = Math.acos(Math.sqrt(etaSteer)) * (180.0 / Math.PI);

    return {
      optimalLowThrustDeltaVKmS: parseFloat(dvOptKmS.toFixed(3)),
      propellantConsumedKg: parseFloat(deltaMKg.toFixed(2)),
      spiralDurationDays: parseFloat(tBurnDays.toFixed(1)),
      spiralDurationYears: parseFloat(tBurnYrs.toFixed(2)),
      meanPitchSteeringAngleDeg: parseFloat(betaMeanDeg.toFixed(1)),
      steeringEfficiencyPercent: parseFloat((etaSteer * 100.0).toFixed(1)),
      optimalSteeringContext: `Optimal-Steering SEP Spiral (${tBurnDays.toFixed(0)} d to Venus, ${deltaMKg.toFixed(1)} kg Xe, beta_mean=${betaMeanDeg.toFixed(1)} deg, ${dvOptKmS.toFixed(2)} km/s Delta-V)`
    };
  }

  /**
   * Calculate combined low-thrust continuous ion spiral orbital radius reduction and inclination plane change using Edelbaum's analytical formulation.
   * Delta_V = sqrt( v_1^2 + v_2^2 - 2 * v_1 * v_2 * cos( ( pi / 2 ) * Delta_i ) )
   * m_f = m_0 * exp( -Delta_V / c )
   * t_burn = delta_m / m_dot
   * Reference: Edelbaum (1961), Burt (1967), Pollard (2000), Curtis (2013) for Combined Low-Thrust Transfers.
   * @param {number} [initialOrbitAU=1.52368] - Initial heliocentric circular orbit in AU (0.2 to 10.0 AU)
   * @param {number} [targetOrbitAU=1.00000] - Target heliocentric circular orbit in AU (0.2 to 10.0 AU)
   * @param {number} [targetDeltaIncDeg=5.65] - Total orbital plane inclination change in deg (0.0 to 90.0 deg)
   * @param {number} [initialMassKg=1500.0] - Spacecraft wet mass in kg (100 to 50000 kg)
   * @param {number} [thrustMN=300.0] - Continuous thruster thrust in mN (10 to 5000 mN)
   * @param {number} [specificImpulseSec=3500.0] - Thruster Isp in seconds (1000 to 10000 s)
   * @returns {{combinedLowThrustDeltaVKmS: number, coplanarBaselineDeltaVKmS: number, planeChangeDeltaVPenaltyKmS: number, propellantConsumedKg: number, transferDurationDays: number, transferDurationYears: number, combinedTransferContext: string}}
   */
  static computeLowThrustCombinedSpiralAndInclinationChange(initialOrbitAU = 1.52368, targetOrbitAU = 1.00000, targetDeltaIncDeg = 5.65, initialMassKg = 1500.0, thrustMN = 300.0, specificImpulseSec = 3500.0) {
    const r1AU = Math.max(0.1, initialOrbitAU);
    const r2AU = Math.max(0.1, targetOrbitAU);
    const dIncDeg = Math.max(0.0, targetDeltaIncDeg);
    const m0Kg = Math.max(10.0, initialMassKg);
    const ThrustN = Math.max(0.001, thrustMN / 1000.0);
    const Isp = Math.max(100.0, specificImpulseSec);

    const AU_KM = 1.495978707e8;
    const muSun = 1.32712440018e11;
    const g0 = 9.80665;
    const cMs = g0 * Isp;
    const mdotKgS = ThrustN / cMs;

    const r1Km = r1AU * AU_KM;
    const r2Km = r2AU * AU_KM;

    const v1KmS = Math.sqrt(muSun / r1Km);
    const v2KmS = Math.sqrt(muSun / r2Km);
    const dvCoplanarKmS = Math.abs(v2KmS - v1KmS);

    // Edelbaum formulation for combined spiral + plane change
    const dIncRad = (dIncDeg * Math.PI) / 180.0;
    const edelbaumAngleRad = (Math.PI / 2.0) * dIncRad;

    const dvCombinedKmS = Math.sqrt(
      Math.pow(v1KmS, 2.0) +
      Math.pow(v2KmS, 2.0) -
      (2.0 * v1KmS * v2KmS * Math.cos(edelbaumAngleRad))
    );
    const dvCombinedMs = dvCombinedKmS * 1000.0;

    const dvPenaltyKmS = dvCombinedKmS - dvCoplanarKmS;

    // Propellant mass
    const mfKg = m0Kg * Math.exp(-dvCombinedMs / cMs);
    const deltaMKg = m0Kg - mfKg;

    // Burn duration
    const tBurnSec = deltaMKg / mdotKgS;
    const tBurnDays = tBurnSec / 86400.0;
    const tBurnYrs = tBurnDays / 365.25;

    return {
      combinedLowThrustDeltaVKmS: parseFloat(dvCombinedKmS.toFixed(3)),
      coplanarBaselineDeltaVKmS: parseFloat(dvCoplanarKmS.toFixed(3)),
      planeChangeDeltaVPenaltyKmS: parseFloat(dvPenaltyKmS.toFixed(3)),
      propellantConsumedKg: parseFloat(deltaMKg.toFixed(2)),
      transferDurationDays: parseFloat(tBurnDays.toFixed(1)),
      transferDurationYears: parseFloat(tBurnYrs.toFixed(2)),
      combinedTransferContext: `Combined Spiral + Inc (${tBurnDays.toFixed(0)} d Transfer, ${dIncDeg.toFixed(1)} deg Inc, ${dvCombinedKmS.toFixed(2)} km/s Delta-V, ${deltaMKg.toFixed(1)} kg Xe)`
    };
  }

  /**
   * Calculate 3-burn bi-elliptic inward transfer from Mars out to high intermediate aphelion and plunge to Venus.
   * a_1 = ( r_mars + r_ap ) / 2, a_2 = ( r_venus + r_ap ) / 2
   * Delta_V_tot = Delta_V_1 + Delta_V_2 + Delta_V_3
   * TOF = pi * sqrt( a_1^3 / mu ) + pi * sqrt( a_2^3 / mu )
   * Reference: Escobal (1965), Chobotov (2002), Curtis (2013) for Bi-Elliptic Orbital Transfers.
   * @param {number} [intermediateAphelionAU=4.00] - Intermediate transfer aphelion in AU (2.0 to 30.0 AU)
   * @param {number} [marsParkingAltitudeKm=300.0] - Mars departure parking altitude in km (150 to 1000 km)
   * @param {number} [venusParkingAltitudeKm=300.0] - Venus arrival parking altitude in km (150 to 1000 km)
   * @returns {{totalTransferTimeDays: number, totalTransferTimeYears: number, marsDepartureDeltaVKmS: number, aphelionBurnDeltaVKmS: number, venusArrivalDeltaVKmS: number, totalMissionDeltaVKmS: number, intermediateAphelionAU: number, biEllipticContext: string}}
   */
  static computeMarsToVenusBiEllipticTransfer(intermediateAphelionAU = 4.00, marsParkingAltitudeKm = 300.0, venusParkingAltitudeKm = 300.0) {
    const rbAU = Math.max(1.8, intermediateAphelionAU);
    const hpMarsKm = Math.max(150.0, marsParkingAltitudeKm);
    const hpVKm = Math.max(150.0, venusParkingAltitudeKm);

    const AU_KM = 1.495978707e8;
    const muSun = 1.32712440018e11;
    const muMars = 42828.37;
    const rMarsKm = 3389.5;
    const muVenus = 324858.6;
    const rVenusKm = 6051.8;

    const rMarsAU = 1.52368;
    const rVenusAU = 0.72333;
    const r1Km = rMarsAU * AU_KM;
    const r2Km = rVenusAU * AU_KM;
    const rbKm = rbAU * AU_KM;

    // Semi-major axes
    const a1Km = (r1Km + rbKm) / 2.0;
    const a2Km = (r2Km + rbKm) / 2.0;

    // Time of flight (days and years)
    const tofs1Sec = Math.PI * Math.sqrt(Math.pow(a1Km, 3.0) / muSun);
    const tofs2Sec = Math.PI * Math.sqrt(Math.pow(a2Km, 3.0) / muSun);
    const tofsTotDays = (tofs1Sec + tofs2Sec) / 86400.0;
    const tofsTotYrs = tofsTotDays / 365.25;

    // Burn 1 (Mars departure)
    const v1KmS = Math.sqrt(muSun / r1Km);
    const vTrans1aKmS = Math.sqrt(muSun * ((2.0 / r1Km) - (1.0 / a1Km)));
    const vInfMarsKmS = Math.abs(vTrans1aKmS - v1KmS);

    const rParkMarsKm = rMarsKm + hpMarsKm;
    const vParkMarsKmS = Math.sqrt(muMars / rParkMarsKm);
    const vHypMarsKmS = Math.sqrt(Math.pow(vInfMarsKmS, 2.0) + ((2.0 * muMars) / rParkMarsKm));
    const dv1KmS = vHypMarsKmS - vParkMarsKmS;

    // Burn 2 (Intermediate Aphelion Burn at rb)
    const vAp1KmS = Math.sqrt(muSun * ((2.0 / rbKm) - (1.0 / a1Km)));
    const vAp2KmS = Math.sqrt(muSun * ((2.0 / rbKm) - (1.0 / a2Km)));
    const dv2KmS = Math.abs(vAp2KmS - vAp1KmS);

    // Burn 3 (Venus Arrival Capture)
    const v2KmS = Math.sqrt(muSun / r2Km);
    const vTrans2pKmS = Math.sqrt(muSun * ((2.0 / r2Km) - (1.0 / a2Km)));
    const vInfVKmS = Math.abs(vTrans2pKmS - v2KmS);

    const rParkVKm = rVenusKm + hpVKm;
    const vParkVKmS = Math.sqrt(muVenus / rParkVKm);
    const vHypVKmS = Math.sqrt(Math.pow(vInfVKmS, 2.0) + ((2.0 * muVenus) / rParkVKm));
    const dv3KmS = vHypVKmS - vParkVKmS;

    const dvTotKmS = dv1KmS + dv2KmS + dv3KmS;

    return {
      totalTransferTimeDays: parseFloat(tofsTotDays.toFixed(1)),
      totalTransferTimeYears: parseFloat(tofsTotYrs.toFixed(2)),
      marsDepartureDeltaVKmS: parseFloat(dv1KmS.toFixed(3)),
      aphelionBurnDeltaVKmS: parseFloat(dv2KmS.toFixed(3)),
      venusArrivalDeltaVKmS: parseFloat(dv3KmS.toFixed(3)),
      totalMissionDeltaVKmS: parseFloat(dvTotKmS.toFixed(3)),
      intermediateAphelionAU: parseFloat(rbAU.toFixed(2)),
      biEllipticContext: `Mars-to-Venus Bi-Elliptic (${rbAU.toFixed(1)} AU Aphelion, ${tofsTotYrs.toFixed(1)} yr TOF, ${dvTotKmS.toFixed(2)} km/s Total Delta-V)`
    };
  }

  /**
   * Calculate interplanetary Hohmann rendezvous transfer from Mars to main asteroid belt targets (1 Ceres / 4 Vesta).
   * a_trans = ( r_mars + r_ast ) / 2
   * TOF = pi * sqrt( a_trans^3 / mu_sun )
   * Delta_V_tot = Delta_V_TAI + Delta_V_AOI
   * Reference: Russell & Raymond (2011), Rayman et al. (2006), Curtis (2013) for Asteroid Rendezvous Trajectories.
   * @param {string} [targetAsteroid='Ceres'] - Target asteroid name ('Ceres' or 'Vesta')
   * @param {number} [marsParkingAltitudeKm=300.0] - Mars departure altitude in km (150 to 1000 km)
   * @param {number} [asteroidParkingAltitudeKm=200.0] - Asteroid capture altitude in km (50 to 1000 km)
   * @returns {{targetBodyName: string, transferTimeDays: number, transferTimeYears: number, marsDepartureDeltaVKmS: number, asteroidArrivalExcessKmS: number, asteroidOrbitInsertionDeltaVKmS: number, totalMissionDeltaVKmS: number, transferEccentricity: number, transferSemiMajorAxisAU: number, asteroidTransferContext: string}}
   */
  static computeMarsToAsteroidMainBeltRendezvousTransfer(targetAsteroid = 'Ceres', marsParkingAltitudeKm = 300.0, asteroidParkingAltitudeKm = 200.0) {
    const isVesta = targetAsteroid && targetAsteroid.toLowerCase().includes('vesta');
    const targetName = isVesta ? '4 Vesta' : '1 Ceres';
    const hpMarsKm = Math.max(150.0, marsParkingAltitudeKm);
    const hpAstKm = Math.max(50.0, asteroidParkingAltitudeKm);

    const AU_KM = 1.495978707e8;
    const muSun = 1.32712440018e11;
    const muMars = 42828.37;
    const rMarsKm = 3389.5;

    // Target properties
    const rAstAU = isVesta ? 2.3618 : 2.7675;
    const muAst = isVesta ? 17.28 : 62.63;
    const rAstRadiusKm = isVesta ? 262.7 : 473.0;

    const rMarsAU = 1.52368;
    const rMarsDistKm = rMarsAU * AU_KM;
    const rAstDistKm = rAstAU * AU_KM;

    // Hohmann transfer geometry
    const aTransKm = (rMarsDistKm + rAstDistKm) / 2.0;
    const aTransAU = aTransKm / AU_KM;
    const eTrans = (rAstDistKm - rMarsDistKm) / (rAstDistKm + rMarsDistKm);

    const tofsSec = Math.PI * Math.sqrt(Math.pow(aTransKm, 3.0) / muSun);
    const tofsDays = tofsSec / 86400.0;
    const tofsYrs = tofsDays / 365.25;

    // Speeds at Mars departure
    const vMarsCircKmS = Math.sqrt(muSun / rMarsDistKm);
    const vDepKmS = Math.sqrt(muSun * ((2.0 / rMarsDistKm) - (1.0 / aTransKm)));
    const vInfMarsKmS = Math.abs(vDepKmS - vMarsCircKmS);

    const rParkMarsKm = rMarsKm + hpMarsKm;
    const vParkMarsKmS = Math.sqrt(muMars / rParkMarsKm);
    const vHypMarsKmS = Math.sqrt(Math.pow(vInfMarsKmS, 2.0) + ((2.0 * muMars) / rParkMarsKm));
    const dvTaiKmS = vHypMarsKmS - vParkMarsKmS;

    // Speeds at Asteroid arrival
    const vAstCircKmS = Math.sqrt(muSun / rAstDistKm);
    const vArrKmS = Math.sqrt(muSun * ((2.0 / rAstDistKm) - (1.0 / aTransKm)));
    const vInfAstKmS = Math.abs(vAstCircKmS - vArrKmS);

    const rParkAstKm = rAstRadiusKm + hpAstKm;
    const vParkAstKmS = Math.sqrt(muAst / rParkAstKm);
    const vHypAstKmS = Math.sqrt(Math.pow(vInfAstKmS, 2.0) + ((2.0 * muAst) / rParkAstKm));
    const dvAoiKmS = vHypAstKmS - vParkAstKmS;

    const dvTotKmS = dvTaiKmS + dvAoiKmS;

    return {
      targetBodyName: targetName,
      transferTimeDays: parseFloat(tofsDays.toFixed(1)),
      transferTimeYears: parseFloat(tofsYrs.toFixed(2)),
      marsDepartureDeltaVKmS: parseFloat(dvTaiKmS.toFixed(3)),
      asteroidArrivalExcessKmS: parseFloat(vInfAstKmS.toFixed(3)),
      asteroidOrbitInsertionDeltaVKmS: parseFloat(dvAoiKmS.toFixed(3)),
      totalMissionDeltaVKmS: parseFloat(dvTotKmS.toFixed(3)),
      transferEccentricity: parseFloat(eTrans.toFixed(4)),
      transferSemiMajorAxisAU: parseFloat(aTransAU.toFixed(3)),
      asteroidTransferContext: `Mars-to-${targetName} (${tofsDays.toFixed(0)} d Transfer, e=${eTrans.toFixed(2)}, ${dvTotKmS.toFixed(2)} km/s Total Delta-V)`
    };
  }

  /**
   * Calculate interplanetary Hohmann transfer from Mars to gas giant Jupiter and Jovian elliptical orbit insertion.
   * a_trans = ( r_mars + r_jup ) / 2
   * TOF = pi * sqrt( a_trans^3 / mu_sun )
   * Delta_V_tot = Delta_V_TJI + Delta_V_JOI
   * Reference: Bate, Mueller & White (1971), Curtis (2013) for Outer Planet Transfers.
   * @param {number} [marsParkingAltitudeKm=300.0] - Mars parking orbit altitude in km (150 to 1000 km)
   * @param {number} [jupiterPeriapsisAltitudeKm=300000.0] - Jupiter capture periapsis altitude in km (50000 to 2000000 km)
   * @returns {{transferTimeDays: number, transferTimeYears: number, marsDepartureDeltaVKmS: number, jupiterArrivalExcessKmS: number, jupiterOrbitInsertionDeltaVKmS: number, totalMissionDeltaVKmS: number, transferEccentricity: number, transferSemiMajorAxisAU: number, jupiterTransferContext: string}}
   */
  static computeMarsToJupiterDirectTransfer(marsParkingAltitudeKm = 300.0, jupiterPeriapsisAltitudeKm = 300000.0) {
    const hpMarsKm = Math.max(150.0, marsParkingAltitudeKm);
    const hpJKm = Math.max(50000.0, jupiterPeriapsisAltitudeKm);

    const AU_KM = 1.495978707e8;
    const muSun = 1.32712440018e11;
    const muMars = 42828.37;
    const rMarsKm = 3389.5;
    const muJup = 1.26686534e8;
    const rJupKm = 71492.0;

    const rMarsAU = 1.52368;
    const rJupAU = 5.2044;
    const rMarsDistKm = rMarsAU * AU_KM;
    const rJupDistKm = rJupAU * AU_KM;

    // Hohmann geometry
    const aTransKm = (rMarsDistKm + rJupDistKm) / 2.0;
    const aTransAU = aTransKm / AU_KM;
    const eTrans = (rJupDistKm - rMarsDistKm) / (rJupDistKm + rMarsDistKm);

    const tofsSec = Math.PI * Math.sqrt(Math.pow(aTransKm, 3.0) / muSun);
    const tofsDays = tofsSec / 86400.0;
    const tofsYrs = tofsDays / 365.25;

    // Speeds at Mars departure
    const vMarsCircKmS = Math.sqrt(muSun / rMarsDistKm);
    const vDepKmS = Math.sqrt(muSun * ((2.0 / rMarsDistKm) - (1.0 / aTransKm)));
    const vInfMarsKmS = Math.abs(vDepKmS - vMarsCircKmS);

    const rParkMarsKm = rMarsKm + hpMarsKm;
    const vParkMarsKmS = Math.sqrt(muMars / rParkMarsKm);
    const vHypMarsKmS = Math.sqrt(Math.pow(vInfMarsKmS, 2.0) + ((2.0 * muMars) / rParkMarsKm));
    const dvTjiKmS = vHypMarsKmS - vParkMarsKmS;

    // Speeds at Jupiter arrival
    const vJupCircKmS = Math.sqrt(muSun / rJupDistKm);
    const vArrKmS = Math.sqrt(muSun * ((2.0 / rJupDistKm) - (1.0 / aTransKm)));
    const vInfJupKmS = Math.abs(vJupCircKmS - vArrKmS);

    const rpJKm = rJupKm + hpJKm;
    const eCap = 0.98; // Highly elliptical capture orbit
    const aCapKm = rpJKm / (1.0 - eCap);

    const vHypJKmS = Math.sqrt(Math.pow(vInfJupKmS, 2.0) + ((2.0 * muJup) / rpJKm));
    const vCapJKmS = Math.sqrt(muJup * ((2.0 / rpJKm) - (1.0 / aCapKm)));
    const dvJoiKmS = vHypJKmS - vCapJKmS;

    const dvTotKmS = dvTjiKmS + dvJoiKmS;

    return {
      transferTimeDays: parseFloat(tofsDays.toFixed(1)),
      transferTimeYears: parseFloat(tofsYrs.toFixed(2)),
      marsDepartureDeltaVKmS: parseFloat(dvTjiKmS.toFixed(3)),
      jupiterArrivalExcessKmS: parseFloat(vInfJupKmS.toFixed(3)),
      jupiterOrbitInsertionDeltaVKmS: parseFloat(dvJoiKmS.toFixed(3)),
      totalMissionDeltaVKmS: parseFloat(dvTotKmS.toFixed(3)),
      transferEccentricity: parseFloat(eTrans.toFixed(4)),
      transferSemiMajorAxisAU: parseFloat(aTransAU.toFixed(3)),
      jupiterTransferContext: `Mars-to-Jupiter Direct (${tofsYrs.toFixed(1)} yr TOF, ${vInfJupKmS.toFixed(2)} km/s v_inf, ${dvTotKmS.toFixed(2)} km/s Total Delta-V)`
    };
  }

  /**
   * Calculate continuous low-thrust ion propulsion optimal thrust angle steering for maximum apsidal line precession rate (dot_omega).
   * tan( theta_opt ) = - cos( nu ) / ( ( 1 + r / p ) * sin( nu ) )
   * dot_omega_max = ( 1 / e ) * sqrt( p / mu ) * [ -f_r * cos( nu ) + f_theta * ( 1 + r / p ) * sin( nu ) ]
   * Reference: Kechichian (1997), Petropoulos & Longuski (2004), Curtis (2013) for Low-Thrust Apsidal Rotation.
   * @param {number} [initialOrbitAU=1.52368] - Semi-major axis in AU (0.2 to 10.0 AU)
   * @param {number} [initialEccentricity=0.0934] - Orbit eccentricity (0.01 to 0.90)
   * @param {number} [initialMassKg=1500.0] - Spacecraft mass in kg (100 to 50000 kg)
   * @param {number} [thrustMN=300.0] - Continuous thrust in mN (10 to 5000 mN)
   * @param {number} [specificImpulseSec=3500.0] - Thruster Isp in seconds (1000 to 10000 s)
   * @param {number} [trueAnomalyDeg=90.0] - True anomaly in degrees (0 to 360 deg)
   * @returns {{thrustSteeringAngleDeg: number, maxPrecessionRateDegPerYear: number, radialAccelerationMmS2: number, projectedApsidalShift180DaysDeg: number, propellantConsumed180DaysKg: number, apsidalSteeringContext: string}}
   */
  static computeLowThrustMaximumApsidalPrecessionSteering(initialOrbitAU = 1.52368, initialEccentricity = 0.0934, initialMassKg = 1500.0, thrustMN = 300.0, specificImpulseSec = 3500.0, trueAnomalyDeg = 90.0) {
    const aAU = Math.max(0.1, initialOrbitAU);
    const e = Math.max(0.005, Math.min(0.95, initialEccentricity));
    const m0Kg = Math.max(10.0, initialMassKg);
    const ThrustN = Math.max(0.001, thrustMN / 1000.0);
    const Isp = Math.max(100.0, specificImpulseSec);
    const nuDeg = trueAnomalyDeg % 360.0;

    const AU_M = 1.495978707e11;
    const muSunM = 1.32712440018e20; // m^3/s^2
    const g0 = 9.80665;
    const cMs = g0 * Isp;

    const aM = aAU * AU_M;
    const pM = aM * (1.0 - Math.pow(e, 2.0));
    const nuRad = (nuDeg * Math.PI) / 180.0;
    const rM = pM / (1.0 + (e * Math.cos(nuRad)));

    const aThrustMs2 = ThrustN / m0Kg;
    const aThrustMmS2 = aThrustMs2 * 1000.0;

    // Optimal steering angle for maximizing dot_omega
    const cosNu = Math.cos(nuRad);
    const sinNu = Math.sin(nuRad);
    const rOverP = rM / pM;

    let thetaOptRad = 0.0;
    if (Math.abs(sinNu) > 1e-4) {
      thetaOptRad = Math.atan2(-cosNu, (1.0 + rOverP) * sinNu);
    } else {
      thetaOptRad = cosNu > 0 ? -Math.PI / 2.0 : Math.PI / 2.0;
    }
    const thetaOptDeg = thetaOptRad * (180.0 / Math.PI);

    const frMs2 = aThrustMs2 * Math.sin(thetaOptRad);
    const fthMs2 = aThrustMs2 * Math.cos(thetaOptRad);

    // Instantaneous maximum dot_omega (rad/s and deg/yr)
    const factor = (1.0 / e) * Math.sqrt(pM / muSunM);
    const dotOmegaRadS = factor * ((-frMs2 * cosNu) + (fthMs2 * (1.0 + rOverP) * sinNu));
    const dotOmegaDegS = dotOmegaRadS * (180.0 / Math.PI);
    const dotOmegaDegYr = dotOmegaDegS * 86400.0 * 365.25;

    // 180 days projected shift and propellant consumed
    const tBurnSec = 180.0 * 86400.0;
    const shift180Deg = dotOmegaDegS * tBurnSec;
    const mdotKgS = ThrustN / cMs;
    const propKg = mdotKgS * tBurnSec;

    return {
      thrustSteeringAngleDeg: parseFloat(thetaOptDeg.toFixed(1)),
      maxPrecessionRateDegPerYear: parseFloat(dotOmegaDegYr.toFixed(1)),
      radialAccelerationMmS2: parseFloat(aThrustMmS2.toFixed(3)),
      projectedApsidalShift180DaysDeg: parseFloat(shift180Deg.toFixed(1)),
      propellantConsumed180DaysKg: parseFloat(propKg.toFixed(2)),
      apsidalSteeringContext: `Max Apsidal Precession Steering (${dotOmegaDegYr.toFixed(0)} deg/yr at nu=${nuDeg.toFixed(0)} deg, theta=${thetaOptDeg.toFixed(1)} deg, ${propKg.toFixed(1)} kg Xe)`
    };
  }

  /**
   * Calculate interplanetary Hohmann transfer from Mars to ringed giant planet Saturn and Saturnian elliptical orbit insertion.
   * a_trans = ( r_mars + r_saturn ) / 2
   * TOF = pi * sqrt( a_trans^3 / mu_sun )
   * Delta_V_tot = Delta_V_TSI + Delta_V_SOI
   * Reference: Bate, Mueller & White (1971), Curtis (2013) for Saturnian Interplanetary Transfers.
   * @param {number} [marsParkingAltitudeKm=300.0] - Mars parking orbit altitude in km (150 to 1000 km)
   * @param {number} [saturnPeriapsisAltitudeKm=60000.0] - Saturn capture periapsis altitude in km (20000 to 1000000 km)
   * @returns {{transferTimeDays: number, transferTimeYears: number, marsDepartureDeltaVKmS: number, saturnArrivalExcessKmS: number, saturnOrbitInsertionDeltaVKmS: number, totalMissionDeltaVKmS: number, transferEccentricity: number, transferSemiMajorAxisAU: number, saturnTransferContext: string}}
   */
  static computeMarsToSaturnDirectTransfer(marsParkingAltitudeKm = 300.0, saturnPeriapsisAltitudeKm = 60000.0) {
    const hpMarsKm = Math.max(150.0, marsParkingAltitudeKm);
    const hpSKm = Math.max(20000.0, saturnPeriapsisAltitudeKm);

    const AU_KM = 1.495978707e8;
    const muSun = 1.32712440018e11;
    const muMars = 42828.37;
    const rMarsKm = 3389.5;
    const muSat = 3.7931187e7;
    const rSatKm = 60268.0;

    const rMarsAU = 1.52368;
    const rSatAU = 9.5826;
    const rMarsDistKm = rMarsAU * AU_KM;
    const rSatDistKm = rSatAU * AU_KM;

    // Hohmann geometry
    const aTransKm = (rMarsDistKm + rSatDistKm) / 2.0;
    const aTransAU = aTransKm / AU_KM;
    const eTrans = (rSatDistKm - rMarsDistKm) / (rSatDistKm + rMarsDistKm);

    const tofsSec = Math.PI * Math.sqrt(Math.pow(aTransKm, 3.0) / muSun);
    const tofsDays = tofsSec / 86400.0;
    const tofsYrs = tofsDays / 365.25;

    // Speeds at Mars departure
    const vMarsCircKmS = Math.sqrt(muSun / rMarsDistKm);
    const vDepKmS = Math.sqrt(muSun * ((2.0 / rMarsDistKm) - (1.0 / aTransKm)));
    const vInfMarsKmS = Math.abs(vDepKmS - vMarsCircKmS);

    const rParkMarsKm = rMarsKm + hpMarsKm;
    const vParkMarsKmS = Math.sqrt(muMars / rParkMarsKm);
    const vHypMarsKmS = Math.sqrt(Math.pow(vInfMarsKmS, 2.0) + ((2.0 * muMars) / rParkMarsKm));
    const dvTsiKmS = vHypMarsKmS - vParkMarsKmS;

    // Speeds at Saturn arrival
    const vSatCircKmS = Math.sqrt(muSun / rSatDistKm);
    const vArrKmS = Math.sqrt(muSun * ((2.0 / rSatDistKm) - (1.0 / aTransKm)));
    const vInfSatKmS = Math.abs(vSatCircKmS - vArrKmS);

    const rpSKm = rSatKm + hpSKm;
    const eCap = 0.98; // Highly elliptical capture orbit
    const aCapKm = rpSKm / (1.0 - eCap);

    const vHypSKmS = Math.sqrt(Math.pow(vInfSatKmS, 2.0) + ((2.0 * muSat) / rpSKm));
    const vCapSKmS = Math.sqrt(muSat * ((2.0 / rpSKm) - (1.0 / aCapKm)));
    const dvSoiKmS = vHypSKmS - vCapSKmS;

    const dvTotKmS = dvTsiKmS + dvSoiKmS;

    return {
      transferTimeDays: parseFloat(tofsDays.toFixed(1)),
      transferTimeYears: parseFloat(tofsYrs.toFixed(2)),
      marsDepartureDeltaVKmS: parseFloat(dvTsiKmS.toFixed(3)),
      saturnArrivalExcessKmS: parseFloat(vInfSatKmS.toFixed(3)),
      saturnOrbitInsertionDeltaVKmS: parseFloat(dvSoiKmS.toFixed(3)),
      totalMissionDeltaVKmS: parseFloat(dvTotKmS.toFixed(3)),
      transferEccentricity: parseFloat(eTrans.toFixed(4)),
      transferSemiMajorAxisAU: parseFloat(aTransAU.toFixed(3)),
      saturnTransferContext: `Mars-to-Saturn Direct (${tofsYrs.toFixed(1)} yr TOF, ${vInfSatKmS.toFixed(2)} km/s v_inf, ${dvTotKmS.toFixed(2)} km/s Total Delta-V)`
    };
  }

  /**
   * Calculate interplanetary Hohmann transfer from Mars to ice giant planet Uranus and Uranian elliptical orbit insertion.
   * a_trans = ( r_mars + r_uranus ) / 2
   * TOF = pi * sqrt( a_trans^3 / mu_sun )
   * Delta_V_tot = Delta_V_TUI + Delta_V_UOI
   * Reference: Bate, Mueller & White (1971), Curtis (2013) for Outer Planet Ice Giant Transfers.
   * @param {number} [marsParkingAltitudeKm=300.0] - Mars parking orbit altitude in km (150 to 1000 km)
   * @param {number} [uranusPeriapsisAltitudeKm=25000.0] - Uranus capture periapsis altitude in km (5000 to 500000 km)
   * @returns {{transferTimeDays: number, transferTimeYears: number, marsDepartureDeltaVKmS: number, uranusArrivalExcessKmS: number, uranusOrbitInsertionDeltaVKmS: number, totalMissionDeltaVKmS: number, transferEccentricity: number, transferSemiMajorAxisAU: number, uranusTransferContext: string}}
   */
  static computeMarsToUranusDirectTransfer(marsParkingAltitudeKm = 300.0, uranusPeriapsisAltitudeKm = 25000.0) {
    const hpMarsKm = Math.max(150.0, marsParkingAltitudeKm);
    const hpUKm = Math.max(5000.0, uranusPeriapsisAltitudeKm);

    const AU_KM = 1.495978707e8;
    const muSun = 1.32712440018e11;
    const muMars = 42828.37;
    const rMarsKm = 3389.5;
    const muUranus = 5.793939e6;
    const rUranusKm = 25362.0;

    const rMarsAU = 1.52368;
    const rUranusAU = 19.1913;
    const rMarsDistKm = rMarsAU * AU_KM;
    const rUranusDistKm = rUranusAU * AU_KM;

    // Hohmann geometry
    const aTransKm = (rMarsDistKm + rUranusDistKm) / 2.0;
    const aTransAU = aTransKm / AU_KM;
    const eTrans = (rUranusDistKm - rMarsDistKm) / (rUranusDistKm + rMarsDistKm);

    const tofsSec = Math.PI * Math.sqrt(Math.pow(aTransKm, 3.0) / muSun);
    const tofsDays = tofsSec / 86400.0;
    const tofsYrs = tofsDays / 365.25;

    // Speeds at Mars departure
    const vMarsCircKmS = Math.sqrt(muSun / rMarsDistKm);
    const vDepKmS = Math.sqrt(muSun * ((2.0 / rMarsDistKm) - (1.0 / aTransKm)));
    const vInfMarsKmS = Math.abs(vDepKmS - vMarsCircKmS);

    const rParkMarsKm = rMarsKm + hpMarsKm;
    const vParkMarsKmS = Math.sqrt(muMars / rParkMarsKm);
    const vHypMarsKmS = Math.sqrt(Math.pow(vInfMarsKmS, 2.0) + ((2.0 * muMars) / rParkMarsKm));
    const dvTuiKmS = vHypMarsKmS - vParkMarsKmS;

    // Speeds at Uranus arrival
    const vUranusCircKmS = Math.sqrt(muSun / rUranusDistKm);
    const vArrKmS = Math.sqrt(muSun * ((2.0 / rUranusDistKm) - (1.0 / aTransKm)));
    const vInfUranusKmS = Math.abs(vUranusCircKmS - vArrKmS);

    const rpUKm = rUranusKm + hpUKm;
    const eCap = 0.98; // Highly elliptical capture orbit
    const aCapKm = rpUKm / (1.0 - eCap);

    const vHypUKmS = Math.sqrt(Math.pow(vInfUranusKmS, 2.0) + ((2.0 * muUranus) / rpUKm));
    const vCapUKmS = Math.sqrt(muUranus * ((2.0 / rpUKm) - (1.0 / aCapKm)));
    const dvUoiKmS = vHypUKmS - vCapUKmS;

    const dvTotKmS = dvTuiKmS + dvUoiKmS;

    return {
      transferTimeDays: parseFloat(tofsDays.toFixed(1)),
      transferTimeYears: parseFloat(tofsYrs.toFixed(2)),
      marsDepartureDeltaVKmS: parseFloat(dvTuiKmS.toFixed(3)),
      uranusArrivalExcessKmS: parseFloat(vInfUranusKmS.toFixed(3)),
      uranusOrbitInsertionDeltaVKmS: parseFloat(dvUoiKmS.toFixed(3)),
      totalMissionDeltaVKmS: parseFloat(dvTotKmS.toFixed(3)),
      transferEccentricity: parseFloat(eTrans.toFixed(4)),
      transferSemiMajorAxisAU: parseFloat(aTransAU.toFixed(3)),
      uranusTransferContext: `Mars-to-Uranus Direct (${tofsYrs.toFixed(1)} yr TOF, ${vInfUranusKmS.toFixed(2)} km/s v_inf, ${dvTotKmS.toFixed(2)} km/s Total Delta-V)`
    };
  }

  /**
   * Calculate interplanetary Hohmann transfer from Mars to outermost ice giant Neptune and Neptunian elliptical orbit insertion.
   * a_trans = ( r_mars + r_neptune ) / 2
   * TOF = pi * sqrt( a_trans^3 / mu_sun )
   * Delta_V_tot = Delta_V_TNI + Delta_V_NOI
   * Reference: Bate, Mueller & White (1971), Curtis (2013) for Outer Planet Transfers.
   * @param {number} [marsParkingAltitudeKm=300.0] - Mars parking orbit altitude in km (150 to 1000 km)
   * @param {number} [neptunePeriapsisAltitudeKm=30000.0] - Neptune capture periapsis altitude in km (5000 to 500000 km)
   * @returns {{transferTimeDays: number, transferTimeYears: number, marsDepartureDeltaVKmS: number, neptuneArrivalExcessKmS: number, neptuneOrbitInsertionDeltaVKmS: number, totalMissionDeltaVKmS: number, transferEccentricity: number, transferSemiMajorAxisAU: number, neptuneTransferContext: string}}
   */
  static computeMarsToNeptuneDirectTransfer(marsParkingAltitudeKm = 300.0, neptunePeriapsisAltitudeKm = 30000.0) {
    const hpMarsKm = Math.max(150.0, marsParkingAltitudeKm);
    const hpNKm = Math.max(5000.0, neptunePeriapsisAltitudeKm);

    const AU_KM = 1.495978707e8;
    const muSun = 1.32712440018e11;
    const muMars = 42828.37;
    const rMarsKm = 3389.5;
    const muNeptune = 6.836529e6;
    const rNeptuneKm = 24764.0;

    const rMarsAU = 1.52368;
    const rNeptuneAU = 30.0699;
    const rMarsDistKm = rMarsAU * AU_KM;
    const rNeptuneDistKm = rNeptuneAU * AU_KM;

    // Hohmann geometry
    const aTransKm = (rMarsDistKm + rNeptuneDistKm) / 2.0;
    const aTransAU = aTransKm / AU_KM;
    const eTrans = (rNeptuneDistKm - rMarsDistKm) / (rNeptuneDistKm + rMarsDistKm);

    const tofsSec = Math.PI * Math.sqrt(Math.pow(aTransKm, 3.0) / muSun);
    const tofsDays = tofsSec / 86400.0;
    const tofsYrs = tofsDays / 365.25;

    // Speeds at Mars departure
    const vMarsCircKmS = Math.sqrt(muSun / rMarsDistKm);
    const vDepKmS = Math.sqrt(muSun * ((2.0 / rMarsDistKm) - (1.0 / aTransKm)));
    const vInfMarsKmS = Math.abs(vDepKmS - vMarsCircKmS);

    const rParkMarsKm = rMarsKm + hpMarsKm;
    const vParkMarsKmS = Math.sqrt(muMars / rParkMarsKm);
    const vHypMarsKmS = Math.sqrt(Math.pow(vInfMarsKmS, 2.0) + ((2.0 * muMars) / rParkMarsKm));
    const dvTniKmS = vHypMarsKmS - vParkMarsKmS;

    // Speeds at Neptune arrival
    const vNeptuneCircKmS = Math.sqrt(muSun / rNeptuneDistKm);
    const vArrKmS = Math.sqrt(muSun * ((2.0 / rNeptuneDistKm) - (1.0 / aTransKm)));
    const vInfNeptuneKmS = Math.abs(vNeptuneCircKmS - vArrKmS);

    const rpNKm = rNeptuneKm + hpNKm;
    const eCap = 0.98; // Highly elliptical capture orbit
    const aCapKm = rpNKm / (1.0 - eCap);

    const vHypNKmS = Math.sqrt(Math.pow(vInfNeptuneKmS, 2.0) + ((2.0 * muNeptune) / rpNKm));
    const vCapNKmS = Math.sqrt(muNeptune * ((2.0 / rpNKm) - (1.0 / aCapKm)));
    const dvNoiKmS = vHypNKmS - vCapNKmS;

    const dvTotKmS = dvTniKmS + dvNoiKmS;

    return {
      transferTimeDays: parseFloat(tofsDays.toFixed(1)),
      transferTimeYears: parseFloat(tofsYrs.toFixed(2)),
      marsDepartureDeltaVKmS: parseFloat(dvTniKmS.toFixed(3)),
      neptuneArrivalExcessKmS: parseFloat(vInfNeptuneKmS.toFixed(3)),
      neptuneOrbitInsertionDeltaVKmS: parseFloat(dvNoiKmS.toFixed(3)),
      totalMissionDeltaVKmS: parseFloat(dvTotKmS.toFixed(3)),
      transferEccentricity: parseFloat(eTrans.toFixed(4)),
      transferSemiMajorAxisAU: parseFloat(aTransAU.toFixed(3)),
      neptuneTransferContext: `Mars-to-Neptune Direct (${tofsYrs.toFixed(1)} yr TOF, ${vInfNeptuneKmS.toFixed(2)} km/s v_inf, ${dvTotKmS.toFixed(2)} km/s Total Delta-V)`
    };
  }

  /**
   * Calculate interplanetary Hohmann transfer from Mars to Kuiper Belt dwarf planet Pluto and Plutonian elliptical orbit insertion.
   * a_trans = ( r_mars + r_pluto ) / 2
   * TOF = pi * sqrt( a_trans^3 / mu_sun )
   * Delta_V_tot = Delta_V_TPI + Delta_V_POI
   * Reference: Stern et al. (2015), Guo & Farquhar (2008), Curtis (2013) for Kuiper Belt Missions.
   * @param {number} [marsParkingAltitudeKm=300.0] - Mars parking orbit altitude in km (150 to 1000 km)
   * @param {number} [plutoPeriapsisAltitudeKm=500.0] - Pluto capture periapsis altitude in km (100 to 50000 km)
   * @returns {{transferTimeDays: number, transferTimeYears: number, marsDepartureDeltaVKmS: number, plutoArrivalExcessKmS: number, plutoOrbitInsertionDeltaVKmS: number, totalMissionDeltaVKmS: number, transferEccentricity: number, transferSemiMajorAxisAU: number, plutoTransferContext: string}}
   */
  static computeMarsToPlutoDirectTransfer(marsParkingAltitudeKm = 300.0, plutoPeriapsisAltitudeKm = 500.0) {
    const hpMarsKm = Math.max(150.0, marsParkingAltitudeKm);
    const hpPKm = Math.max(100.0, plutoPeriapsisAltitudeKm);

    const AU_KM = 1.495978707e8;
    const muSun = 1.32712440018e11;
    const muMars = 42828.37;
    const rMarsKm = 3389.5;
    const muPluto = 871.0;
    const rPlutoKm = 1188.3;

    const rMarsAU = 1.52368;
    const rPlutoAU = 39.482;
    const rMarsDistKm = rMarsAU * AU_KM;
    const rPlutoDistKm = rPlutoAU * AU_KM;

    // Hohmann geometry
    const aTransKm = (rMarsDistKm + rPlutoDistKm) / 2.0;
    const aTransAU = aTransKm / AU_KM;
    const eTrans = (rPlutoDistKm - rMarsDistKm) / (rPlutoDistKm + rMarsDistKm);

    const tofsSec = Math.PI * Math.sqrt(Math.pow(aTransKm, 3.0) / muSun);
    const tofsDays = tofsSec / 86400.0;
    const tofsYrs = tofsDays / 365.25;

    // Speeds at Mars departure
    const vMarsCircKmS = Math.sqrt(muSun / rMarsDistKm);
    const vDepKmS = Math.sqrt(muSun * ((2.0 / rMarsDistKm) - (1.0 / aTransKm)));
    const vInfMarsKmS = Math.abs(vDepKmS - vMarsCircKmS);

    const rParkMarsKm = rMarsKm + hpMarsKm;
    const vParkMarsKmS = Math.sqrt(muMars / rParkMarsKm);
    const vHypMarsKmS = Math.sqrt(Math.pow(vInfMarsKmS, 2.0) + ((2.0 * muMars) / rParkMarsKm));
    const dvTpiKmS = vHypMarsKmS - vParkMarsKmS;

    // Speeds at Pluto arrival
    const vPlutoCircKmS = Math.sqrt(muSun / rPlutoDistKm);
    const vArrKmS = Math.sqrt(muSun * ((2.0 / rPlutoDistKm) - (1.0 / aTransKm)));
    const vInfPlutoKmS = Math.abs(vPlutoCircKmS - vArrKmS);

    const rpPKm = rPlutoKm + hpPKm;
    const eCap = 0.95; // Highly elliptical capture orbit
    const aCapKm = rpPKm / (1.0 - eCap);

    const vHypPKmS = Math.sqrt(Math.pow(vInfPlutoKmS, 2.0) + ((2.0 * muPluto) / rpPKm));
    const vCapPKmS = Math.sqrt(muPluto * ((2.0 / rpPKm) - (1.0 / aCapKm)));
    const dvPoiKmS = vHypPKmS - vCapPKmS;

    const dvTotKmS = dvTpiKmS + dvPoiKmS;

    return {
      transferTimeDays: parseFloat(tofsDays.toFixed(1)),
      transferTimeYears: parseFloat(tofsYrs.toFixed(2)),
      marsDepartureDeltaVKmS: parseFloat(dvTpiKmS.toFixed(3)),
      plutoArrivalExcessKmS: parseFloat(vInfPlutoKmS.toFixed(3)),
      plutoOrbitInsertionDeltaVKmS: parseFloat(dvPoiKmS.toFixed(3)),
      totalMissionDeltaVKmS: parseFloat(dvTotKmS.toFixed(3)),
      transferEccentricity: parseFloat(eTrans.toFixed(4)),
      transferSemiMajorAxisAU: parseFloat(aTransAU.toFixed(3)),
      plutoTransferContext: `Mars-to-Pluto Direct (${tofsYrs.toFixed(1)} yr TOF, ${vInfPlutoKmS.toFixed(2)} km/s v_inf, ${dvTotKmS.toFixed(2)} km/s Total Delta-V)`
    };
  }

  /**
   * Calculate interplanetary Hohmann transfer from Mars to Cold Classical Kuiper Belt Object (486958 Arrokoth) and flyby encounter velocity.
   * a_trans = ( r_mars + r_arrokoth ) / 2
   * TOF = pi * sqrt( a_trans^3 / mu_sun )
   * Reference: Stern et al. (2019), Spencer et al. (2020), Curtis (2013) for Kuiper Belt Reconnaissance.
   * @param {number} [marsParkingAltitudeKm=300.0] - Mars parking orbit altitude in km (150 to 1000 km)
   * @returns {{transferTimeDays: number, transferTimeYears: number, marsDepartureDeltaVKmS: number, arrokothFlybyVelocityKmS: number, transferEccentricity: number, transferSemiMajorAxisAU: number, arrokothTransferContext: string}}
   */
  static computeMarsToArrokothDirectTransfer(marsParkingAltitudeKm = 300.0) {
    const hpMarsKm = Math.max(150.0, marsParkingAltitudeKm);

    const AU_KM = 1.495978707e8;
    const muSun = 1.32712440018e11;
    const muMars = 42828.37;
    const rMarsKm = 3389.5;

    const rMarsAU = 1.52368;
    const rArrokothAU = 44.581; // Semi-major axis of 486958 Arrokoth
    const rMarsDistKm = rMarsAU * AU_KM;
    const rArrokothDistKm = rArrokothAU * AU_KM;

    // Hohmann geometry
    const aTransKm = (rMarsDistKm + rArrokothDistKm) / 2.0;
    const aTransAU = aTransKm / AU_KM;
    const eTrans = (rArrokothDistKm - rMarsDistKm) / (rArrokothDistKm + rMarsDistKm);

    const tofsSec = Math.PI * Math.sqrt(Math.pow(aTransKm, 3.0) / muSun);
    const tofsDays = tofsSec / 86400.0;
    const tofsYrs = tofsDays / 365.25;

    // Speeds at Mars departure
    const vMarsCircKmS = Math.sqrt(muSun / rMarsDistKm);
    const vDepKmS = Math.sqrt(muSun * ((2.0 / rMarsDistKm) - (1.0 / aTransKm)));
    const vInfMarsKmS = Math.abs(vDepKmS - vMarsCircKmS);

    const rParkMarsKm = rMarsKm + hpMarsKm;
    const vParkMarsKmS = Math.sqrt(muMars / rParkMarsKm);
    const vHypMarsKmS = Math.sqrt(Math.pow(vInfMarsKmS, 2.0) + ((2.0 * muMars) / rParkMarsKm));
    const dvTkiKmS = vHypMarsKmS - vParkMarsKmS;

    // Speeds at Arrokoth arrival
    const vArrCircKmS = Math.sqrt(muSun / rArrokothDistKm);
    const vArrKmS = Math.sqrt(muSun * ((2.0 / rArrokothDistKm) - (1.0 / aTransKm)));
    const vInfArrKmS = Math.abs(vArrCircKmS - vArrKmS);

    return {
      transferTimeDays: parseFloat(tofsDays.toFixed(1)),
      transferTimeYears: parseFloat(tofsYrs.toFixed(2)),
      marsDepartureDeltaVKmS: parseFloat(dvTkiKmS.toFixed(3)),
      arrokothFlybyVelocityKmS: parseFloat(vInfArrKmS.toFixed(3)),
      transferEccentricity: parseFloat(eTrans.toFixed(4)),
      transferSemiMajorAxisAU: parseFloat(aTransAU.toFixed(3)),
      arrokothTransferContext: `Mars-to-Arrokoth Flyby (${tofsYrs.toFixed(1)} yr TOF, ${dvTkiKmS.toFixed(2)} km/s TKI, ${vInfArrKmS.toFixed(2)} km/s Flyby)`
    };
  }

  /**
   * Calculate interplanetary Hohmann transfer from Mars to massive scattered disc dwarf planet (136199 Eris) and elliptical orbit insertion.
   * a_trans = ( r_mars + r_eris ) / 2
   * TOF = pi * sqrt( a_trans^3 / mu_sun )
   * Delta_V_tot = Delta_V_TEI + Delta_V_EOI
   * Reference: Brown et al. (2005), Sicardy et al. (2011), Curtis (2013) for Scattered Disc Transfers.
   * @param {number} [marsParkingAltitudeKm=300.0] - Mars parking orbit altitude in km (150 to 1000 km)
   * @param {number} [erisPeriapsisAltitudeKm=500.0] - Eris capture periapsis altitude in km (100 to 50000 km)
   * @returns {{transferTimeDays: number, transferTimeYears: number, marsDepartureDeltaVKmS: number, erisArrivalExcessKmS: number, erisOrbitInsertionDeltaVKmS: number, totalMissionDeltaVKmS: number, transferEccentricity: number, transferSemiMajorAxisAU: number, erisTransferContext: string}}
   */
  static computeMarsToErisDirectTransfer(marsParkingAltitudeKm = 300.0, erisPeriapsisAltitudeKm = 500.0) {
    const hpMarsKm = Math.max(150.0, marsParkingAltitudeKm);
    const hpEKm = Math.max(100.0, erisPeriapsisAltitudeKm);

    const AU_KM = 1.495978707e8;
    const muSun = 1.32712440018e11;
    const muMars = 42828.37;
    const rMarsKm = 3389.5;
    const muEris = 1108.0;
    const rErisKm = 1163.0;

    const rMarsAU = 1.52368;
    const rErisAU = 67.781;
    const rMarsDistKm = rMarsAU * AU_KM;
    const rErisDistKm = rErisAU * AU_KM;

    // Hohmann geometry
    const aTransKm = (rMarsDistKm + rErisDistKm) / 2.0;
    const aTransAU = aTransKm / AU_KM;
    const eTrans = (rErisDistKm - rMarsDistKm) / (rErisDistKm + rMarsDistKm);

    const tofsSec = Math.PI * Math.sqrt(Math.pow(aTransKm, 3.0) / muSun);
    const tofsDays = tofsSec / 86400.0;
    const tofsYrs = tofsDays / 365.25;

    // Speeds at Mars departure
    const vMarsCircKmS = Math.sqrt(muSun / rMarsDistKm);
    const vDepKmS = Math.sqrt(muSun * ((2.0 / rMarsDistKm) - (1.0 / aTransKm)));
    const vInfMarsKmS = Math.abs(vDepKmS - vMarsCircKmS);

    const rParkMarsKm = rMarsKm + hpMarsKm;
    const vParkMarsKmS = Math.sqrt(muMars / rParkMarsKm);
    const vHypMarsKmS = Math.sqrt(Math.pow(vInfMarsKmS, 2.0) + ((2.0 * muMars) / rParkMarsKm));
    const dvTeiKmS = vHypMarsKmS - vParkMarsKmS;

    // Speeds at Eris arrival
    const vErisCircKmS = Math.sqrt(muSun / rErisDistKm);
    const vArrKmS = Math.sqrt(muSun * ((2.0 / rErisDistKm) - (1.0 / aTransKm)));
    const vInfErisKmS = Math.abs(vErisCircKmS - vArrKmS);

    const rpEKm = rErisKm + hpEKm;
    const eCap = 0.95; // Highly elliptical capture orbit
    const aCapKm = rpEKm / (1.0 - eCap);

    const vHypEKmS = Math.sqrt(Math.pow(vInfErisKmS, 2.0) + ((2.0 * muEris) / rpEKm));
    const vCapEKmS = Math.sqrt(muEris * ((2.0 / rpEKm) - (1.0 / aCapKm)));
    const dvEoiKmS = vHypEKmS - vCapEKmS;

    const dvTotKmS = dvTeiKmS + dvEoiKmS;

    return {
      transferTimeDays: parseFloat(tofsDays.toFixed(1)),
      transferTimeYears: parseFloat(tofsYrs.toFixed(2)),
      marsDepartureDeltaVKmS: parseFloat(dvTeiKmS.toFixed(3)),
      erisArrivalExcessKmS: parseFloat(vInfErisKmS.toFixed(3)),
      erisOrbitInsertionDeltaVKmS: parseFloat(dvEoiKmS.toFixed(3)),
      totalMissionDeltaVKmS: parseFloat(dvTotKmS.toFixed(3)),
      transferEccentricity: parseFloat(eTrans.toFixed(4)),
      transferSemiMajorAxisAU: parseFloat(aTransAU.toFixed(3)),
      erisTransferContext: `Mars-to-Eris Direct (${tofsYrs.toFixed(1)} yr TOF, ${vInfErisKmS.toFixed(2)} km/s v_inf, ${dvTotKmS.toFixed(2)} km/s Total Delta-V)`
    };
  }

  /**
   * Calculate interplanetary Hohmann transfer from Mars to detached extreme inner Oort Cloud dwarf planet (90377 Sedna) and flyby encounter velocity.
   * a_trans = ( r_mars + r_sedna ) / 2
   * TOF = pi * sqrt( a_trans^3 / mu_sun )
   * Reference: Brown et al. (2004), Schwamb et al. (2010), Curtis (2013) for Extreme Trans-Neptunian Object Trajectories.
   * @param {number} [marsParkingAltitudeKm=300.0] - Mars parking orbit altitude in km (150 to 1000 km)
   * @returns {{transferTimeDays: number, transferTimeYears: number, marsDepartureDeltaVKmS: number, sednaFlybyVelocityKmS: number, transferEccentricity: number, transferSemiMajorAxisAU: number, sednaTransferContext: string}}
   */
  static computeMarsToSednaDirectTransfer(marsParkingAltitudeKm = 300.0) {
    const hpMarsKm = Math.max(150.0, marsParkingAltitudeKm);

    const AU_KM = 1.495978707e8;
    const muSun = 1.32712440018e11;
    const muMars = 42828.37;
    const rMarsKm = 3389.5;

    const rMarsAU = 1.52368;
    const rSednaAU = 76.19; // Perihelion distance of 90377 Sedna
    const rMarsDistKm = rMarsAU * AU_KM;
    const rSednaDistKm = rSednaAU * AU_KM;

    // Hohmann geometry
    const aTransKm = (rMarsDistKm + rSednaDistKm) / 2.0;
    const aTransAU = aTransKm / AU_KM;
    const eTrans = (rSednaDistKm - rMarsDistKm) / (rSednaDistKm + rMarsDistKm);

    const tofsSec = Math.PI * Math.sqrt(Math.pow(aTransKm, 3.0) / muSun);
    const tofsDays = tofsSec / 86400.0;
    const tofsYrs = tofsDays / 365.25;

    // Speeds at Mars departure
    const vMarsCircKmS = Math.sqrt(muSun / rMarsDistKm);
    const vDepKmS = Math.sqrt(muSun * ((2.0 / rMarsDistKm) - (1.0 / aTransKm)));
    const vInfMarsKmS = Math.abs(vDepKmS - vMarsCircKmS);

    const rParkMarsKm = rMarsKm + hpMarsKm;
    const vParkMarsKmS = Math.sqrt(muMars / rParkMarsKm);
    const vHypMarsKmS = Math.sqrt(Math.pow(vInfMarsKmS, 2.0) + ((2.0 * muMars) / rParkMarsKm));
    const dvTsiKmS = vHypMarsKmS - vParkMarsKmS;

    // Speeds at Sedna arrival
    const vArrCircKmS = Math.sqrt(muSun / rSednaDistKm);
    const vArrKmS = Math.sqrt(muSun * ((2.0 / rSednaDistKm) - (1.0 / aTransKm)));
    const vInfSednaKmS = Math.abs(vArrCircKmS - vArrKmS);

    return {
      transferTimeDays: parseFloat(tofsDays.toFixed(1)),
      transferTimeYears: parseFloat(tofsYrs.toFixed(2)),
      marsDepartureDeltaVKmS: parseFloat(dvTsiKmS.toFixed(3)),
      sednaFlybyVelocityKmS: parseFloat(vInfSednaKmS.toFixed(3)),
      transferEccentricity: parseFloat(eTrans.toFixed(4)),
      transferSemiMajorAxisAU: parseFloat(aTransAU.toFixed(3)),
      sednaTransferContext: `Mars-to-Sedna Flyby (${tofsYrs.toFixed(1)} yr TOF, ${dvTsiKmS.toFixed(2)} km/s TSI, ${vInfSednaKmS.toFixed(2)} km/s Flyby)`
    };
  }

  /**
   * Calculate interplanetary high-energy chase transfer from Mars to intercept interstellar object 1I/'Oumuamua in deep space and relative encounter velocity.
   * a_trans = ( r_mars + r_intercept ) / 2
   * TOF = pi * sqrt( a_trans^3 / mu_sun )
   * Reference: Meech et al. (2017), Seligman & Laughlin (2018), Hein et al. (2019) for Interstellar Object Intercept Missions.
   * @param {number} [marsParkingAltitudeKm=300.0] - Mars parking orbit altitude in km (150 to 1000 km)
   * @param {number} [interceptDistanceAU=15.0] - Intercept heliocentric distance in AU (5.0 to 50.0 AU)
   * @returns {{transferTimeDays: number, transferTimeYears: number, marsDepartureDeltaVKmS: number, oumuamuaRelativeEncounterVelocityKmS: number, transferEccentricity: number, transferSemiMajorAxisAU: number, interceptContext: string}}
   */
  static computeMarsToOumuamuaHyperbolicIntercept(marsParkingAltitudeKm = 300.0, interceptDistanceAU = 15.0) {
    const hpMarsKm = Math.max(150.0, marsParkingAltitudeKm);
    const rIntAU = Math.max(3.0, interceptDistanceAU);

    const AU_KM = 1.495978707e8;
    const muSun = 1.32712440018e11;
    const muMars = 42828.37;
    const rMarsKm = 3389.5;

    const rMarsAU = 1.52368;
    const rMarsDistKm = rMarsAU * AU_KM;
    const rIntDistKm = rIntAU * AU_KM;

    // Hohmann geometry
    const aTransKm = (rMarsDistKm + rIntDistKm) / 2.0;
    const aTransAU = aTransKm / AU_KM;
    const eTrans = (rIntDistKm - rMarsDistKm) / (rIntDistKm + rMarsDistKm);

    const tofsSec = Math.PI * Math.sqrt(Math.pow(aTransKm, 3.0) / muSun);
    const tofsDays = tofsSec / 86400.0;
    const tofsYrs = tofsDays / 365.25;

    // Speeds at Mars departure
    const vMarsCircKmS = Math.sqrt(muSun / rMarsDistKm);
    const vDepKmS = Math.sqrt(muSun * ((2.0 / rMarsDistKm) - (1.0 / aTransKm)));
    const vInfMarsKmS = Math.abs(vDepKmS - vMarsCircKmS);

    const rParkMarsKm = rMarsKm + hpMarsKm;
    const vParkMarsKmS = Math.sqrt(muMars / rParkMarsKm);
    const vHypMarsKmS = Math.sqrt(Math.pow(vInfMarsKmS, 2.0) + ((2.0 * muMars) / rParkMarsKm));
    const dvTiiKmS = vHypMarsKmS - vParkMarsKmS;

    // Speeds at Intercept
    const vInfOumuamuaKmS = 26.33; // Hyperbolic excess of 1I/'Oumuamua
    const vOumuamuaKmS = Math.sqrt(Math.pow(vInfOumuamuaKmS, 2.0) + ((2.0 * muSun) / rIntDistKm));
    const vScKmS = Math.sqrt(muSun * ((2.0 / rIntDistKm) - (1.0 / aTransKm)));
    const vRelKmS = Math.sqrt(Math.pow(vOumuamuaKmS, 2.0) + Math.pow(vScKmS, 2.0));

    return {
      transferTimeDays: parseFloat(tofsDays.toFixed(1)),
      transferTimeYears: parseFloat(tofsYrs.toFixed(2)),
      marsDepartureDeltaVKmS: parseFloat(dvTiiKmS.toFixed(3)),
      oumuamuaRelativeEncounterVelocityKmS: parseFloat(vRelKmS.toFixed(3)),
      transferEccentricity: parseFloat(eTrans.toFixed(4)),
      transferSemiMajorAxisAU: parseFloat(aTransAU.toFixed(3)),
      interceptContext: `1I/'Oumuamua Chase at ${rIntAU.toFixed(1)}AU (${tofsYrs.toFixed(1)} yr TOF, ${dvTiiKmS.toFixed(2)} km/s TII, ${vRelKmS.toFixed(1)} km/s Rel Flyby)`
    };
  }

  /**
   * Calculate interplanetary chase transfer from Mars to intercept retrograde interstellar comet 2I/Borisov and relative encounter velocity.
   * a_trans = ( r_mars + r_intercept ) / 2
   * TOF = pi * sqrt( a_trans^3 / mu_sun )
   * v_rel = sqrt( v_comet^2 + v_sc^2 - 2 * v_comet * v_sc * cos( inclination ) )
   * Reference: Guzik et al. (2020), Jewitt & Luu (2019), Curtis (2013) for Interstellar Comet Missions.
   * @param {number} [marsParkingAltitudeKm=300.0] - Mars parking orbit altitude in km (150 to 1000 km)
   * @param {number} [interceptDistanceAU=2.0] - Intercept heliocentric distance in AU (1.6 to 10.0 AU)
   * @returns {{transferTimeDays: number, transferTimeYears: number, marsDepartureDeltaVKmS: number, borisovRelativeEncounterVelocityKmS: number, transferEccentricity: number, transferSemiMajorAxisAU: number, borisovInterceptContext: string}}
   */
  static computeMarsToBorisovHyperbolicIntercept(marsParkingAltitudeKm = 300.0, interceptDistanceAU = 2.0) {
    const hpMarsKm = Math.max(150.0, marsParkingAltitudeKm);
    const rIntAU = Math.max(1.55, interceptDistanceAU);

    const AU_KM = 1.495978707e8;
    const muSun = 1.32712440018e11;
    const muMars = 42828.37;
    const rMarsKm = 3389.5;

    const rMarsAU = 1.52368;
    const rMarsDistKm = rMarsAU * AU_KM;
    const rIntDistKm = rIntAU * AU_KM;

    // Hohmann geometry
    const aTransKm = (rMarsDistKm + rIntDistKm) / 2.0;
    const aTransAU = aTransKm / AU_KM;
    const eTrans = (rIntDistKm - rMarsDistKm) / (rIntDistKm + rMarsDistKm);

    const tofsSec = Math.PI * Math.sqrt(Math.pow(aTransKm, 3.0) / muSun);
    const tofsDays = tofsSec / 86400.0;
    const tofsYrs = tofsDays / 365.25;

    // Speeds at Mars departure
    const vMarsCircKmS = Math.sqrt(muSun / rMarsDistKm);
    const vDepKmS = Math.sqrt(muSun * ((2.0 / rMarsDistKm) - (1.0 / aTransKm)));
    const vInfMarsKmS = Math.abs(vDepKmS - vMarsCircKmS);

    const rParkMarsKm = rMarsKm + hpMarsKm;
    const vParkMarsKmS = Math.sqrt(muMars / rParkMarsKm);
    const vHypMarsKmS = Math.sqrt(Math.pow(vInfMarsKmS, 2.0) + ((2.0 * muMars) / rParkMarsKm));
    const dvTiiKmS = vHypMarsKmS - vParkMarsKmS;

    // Speeds at Intercept
    const vInfBorisovKmS = 32.20; // Hyperbolic excess of 2I/Borisov
    const vBorisovKmS = Math.sqrt(Math.pow(vInfBorisovKmS, 2.0) + ((2.0 * muSun) / rIntDistKm));
    const vScKmS = Math.sqrt(muSun * ((2.0 / rIntDistKm) - (1.0 / aTransKm)));
    const incRad = (44.05 * Math.PI) / 180.0; // 44.05 deg inclination
    const vRelKmS = Math.sqrt(Math.pow(vBorisovKmS, 2.0) + Math.pow(vScKmS, 2.0) - (2.0 * vBorisovKmS * vScKmS * Math.cos(incRad)));

    return {
      transferTimeDays: parseFloat(tofsDays.toFixed(1)),
      transferTimeYears: parseFloat(tofsYrs.toFixed(2)),
      marsDepartureDeltaVKmS: parseFloat(dvTiiKmS.toFixed(3)),
      borisovRelativeEncounterVelocityKmS: parseFloat(vRelKmS.toFixed(3)),
      transferEccentricity: parseFloat(eTrans.toFixed(4)),
      transferSemiMajorAxisAU: parseFloat(aTransAU.toFixed(3)),
      borisovInterceptContext: `2I/Borisov Chase at ${rIntAU.toFixed(1)}AU (${tofsYrs.toFixed(1)} yr TOF, ${dvTiiKmS.toFixed(2)} km/s TII, ${vRelKmS.toFixed(1)} km/s Rel Flyby)`
    };
  }

  /**
   * Calculate interplanetary trajectory from Mars to Mercury via Venus Gravity Assist (VGA) and Mercury orbit capture.
   * a_1 = ( r_mars + r_venus ) / 2, a_2 = ( r_venus + r_mercury ) / 2
   * delta_V = 2 * arcsin( 1 / ( 1 + r_p * v_inf^2 / mu_venus ) )
   * Reference: Bate, Mueller & White (1971), Curtis (2013) for Multi-Body Gravity Assist Transfers.
   * @param {number} [marsParkingAltitudeKm=300.0] - Mars parking orbit altitude in km (150 to 1000 km)
   * @param {number} [venusFlybyAltitudeKm=300.0] - Venus flyby periapsis altitude in km (200 to 5000 km)
   * @param {number} [mercuryPeriapsisAltitudeKm=200.0] - Mercury capture periapsis altitude in km (100 to 5000 km)
   * @returns {{totalTimeDays: number, marsDepartureDeltaVKmS: number, venusFlybyExcessKmS: number, venusBendingAngleDeg: number, mercuryOrbitInsertionDeltaVKmS: number, totalMissionDeltaVKmS: number, trajectoryContext: string}}
   */
  static computeMarsToMercuryViaVenusGravityAssist(marsParkingAltitudeKm = 300.0, venusFlybyAltitudeKm = 300.0, mercuryPeriapsisAltitudeKm = 200.0) {
    const hpMarsKm = Math.max(150.0, marsParkingAltitudeKm);
    const hpVKm = Math.max(200.0, venusFlybyAltitudeKm);
    const hpMercKm = Math.max(100.0, mercuryPeriapsisAltitudeKm);

    const AU_KM = 1.495978707e8;
    const muSun = 1.32712440018e11;
    const muMars = 42828.37;
    const rMarsKm = 3389.5;
    const muVenus = 324859.0;
    const rVenusKm = 6051.8;
    const muMerc = 22032.09;
    const rMercKm = 2439.7;

    const rMarsAU = 1.52368;
    const rVenusAU = 0.72333;
    const rMercAU = 0.38710;

    const rMarsDistKm = rMarsAU * AU_KM;
    const rVenusDistKm = rVenusAU * AU_KM;
    const rMercDistKm = rMercAU * AU_KM;

    // Leg 1: Mars to Venus
    const a1Km = (rMarsDistKm + rVenusDistKm) / 2.0;
    const tof1Sec = Math.PI * Math.sqrt(Math.pow(a1Km, 3.0) / muSun);
    const tof1Days = tof1Sec / 86400.0;

    const vMarsCircKmS = Math.sqrt(muSun / rMarsDistKm);
    const vDepKmS = Math.sqrt(muSun * ((2.0 / rMarsDistKm) - (1.0 / a1Km)));
    const vInfMarsKmS = Math.abs(vMarsCircKmS - vDepKmS);

    const rParkMarsKm = rMarsKm + hpMarsKm;
    const vParkMarsKmS = Math.sqrt(muMars / rParkMarsKm);
    const vHypMarsKmS = Math.sqrt(Math.pow(vInfMarsKmS, 2.0) + ((2.0 * muMars) / rParkMarsKm));
    const dvTviKmS = vHypMarsKmS - vParkMarsKmS;

    // Venus encounter
    const vVenusCircKmS = Math.sqrt(muSun / rVenusDistKm);
    const vArr1KmS = Math.sqrt(muSun * ((2.0 / rVenusDistKm) - (1.0 / a1Km)));
    const vInfVKmS = Math.abs(vArr1KmS - vVenusCircKmS);

    const rpVKm = rVenusKm + hpVKm;
    const deltaVRad = 2.0 * Math.asin(1.0 / (1.0 + ((rpVKm * Math.pow(vInfVKmS, 2.0)) / muVenus)));
    const deltaVDeg = (deltaVRad * 180.0) / Math.PI;

    // Leg 2: Venus to Mercury
    const a2Km = (rVenusDistKm + rMercDistKm) / 2.0;
    const tof2Sec = Math.PI * Math.sqrt(Math.pow(a2Km, 3.0) / muSun);
    const tof2Days = tof2Sec / 86400.0;

    const totDays = tof1Days + tof2Days;

    // Mercury arrival
    const vMercCircKmS = Math.sqrt(muSun / rMercDistKm);
    const vArr2KmS = Math.sqrt(muSun * ((2.0 / rMercDistKm) - (1.0 / a2Km)));
    const vInfMercKmS = Math.abs(vArr2KmS - vMercCircKmS);

    const rpMercKm = rMercKm + hpMercKm;
    const eCap = 0.80; // Capture orbit
    const aCapKm = rpMercKm / (1.0 - eCap);

    const vHypMercKmS = Math.sqrt(Math.pow(vInfMercKmS, 2.0) + ((2.0 * muMerc) / rpMercKm));
    const vCapMercKmS = Math.sqrt(muMerc * ((2.0 / rpMercKm) - (1.0 / aCapKm)));
    const dvMoiKmS = vHypMercKmS - vCapMercKmS;

    const dvTotKmS = dvTviKmS + dvMoiKmS;

    return {
      totalTimeDays: parseFloat(totDays.toFixed(1)),
      marsDepartureDeltaVKmS: parseFloat(dvTviKmS.toFixed(3)),
      venusFlybyExcessKmS: parseFloat(vInfVKmS.toFixed(3)),
      venusBendingAngleDeg: parseFloat(deltaVDeg.toFixed(1)),
      mercuryOrbitInsertionDeltaVKmS: parseFloat(dvMoiKmS.toFixed(3)),
      totalMissionDeltaVKmS: parseFloat(dvTotKmS.toFixed(3)),
      trajectoryContext: `Mars-Venus-Mercury GA (${totDays.toFixed(0)} days TOF, ${deltaVDeg.toFixed(1)} deg Venus Turn, ${dvTotKmS.toFixed(2)} km/s Total Delta-V)`
    };
  }

  /**
   * Calculate interplanetary Grand Tour trajectory from Mars to Saturn via Jupiter Gravity Assist (JGA) and Saturn orbit capture.
   * a_1 = ( r_mars + r_jupiter ) / 2, a_2 = ( r_jupiter + r_saturn ) / 2
   * delta_J = 2 * arcsin( 1 / ( 1 + r_p * v_inf^2 / mu_jupiter ) )
   * Reference: Flandro (1966), Bate, Mueller & White (1971), Curtis (2013) for Outer Planet Gravity Assists.
   * @param {number} [marsParkingAltitudeKm=300.0] - Mars parking orbit altitude in km (150 to 1000 km)
   * @param {number} [jupiterFlybyAltitudeKm=500000.0] - Jupiter flyby periapsis altitude in km (100000 to 5000000 km)
   * @param {number} [saturnPeriapsisAltitudeKm=50000.0] - Saturn capture periapsis altitude in km (10000 to 500000 km)
   * @returns {{totalTimeDays: number, totalTimeYears: number, marsDepartureDeltaVKmS: number, jupiterFlybyExcessKmS: number, jupiterBendingAngleDeg: number, saturnOrbitInsertionDeltaVKmS: number, totalMissionDeltaVKmS: number, grandTourContext: string}}
   */
  static computeMarsToSaturnViaJupiterGravityAssist(marsParkingAltitudeKm = 300.0, jupiterFlybyAltitudeKm = 500000.0, saturnPeriapsisAltitudeKm = 50000.0) {
    const hpMarsKm = Math.max(150.0, marsParkingAltitudeKm);
    const hpJKm = Math.max(50000.0, jupiterFlybyAltitudeKm);
    const hpSatKm = Math.max(10000.0, saturnPeriapsisAltitudeKm);

    const AU_KM = 1.495978707e8;
    const muSun = 1.32712440018e11;
    const muMars = 42828.37;
    const rMarsKm = 3389.5;
    const muJupiter = 1.26686534e8;
    const rJupiterKm = 71492.0;
    const muSaturn = 37931187.0;
    const rSaturnKm = 60268.0;

    const rMarsAU = 1.52368;
    const rJupiterAU = 5.2044;
    const rSaturnAU = 9.5826;

    const rMarsDistKm = rMarsAU * AU_KM;
    const rJupiterDistKm = rJupiterAU * AU_KM;
    const rSaturnDistKm = rSaturnAU * AU_KM;

    // Leg 1: Mars to Jupiter
    const a1Km = (rMarsDistKm + rJupiterDistKm) / 2.0;
    const tof1Sec = Math.PI * Math.sqrt(Math.pow(a1Km, 3.0) / muSun);
    const tof1Days = tof1Sec / 86400.0;

    const vMarsCircKmS = Math.sqrt(muSun / rMarsDistKm);
    const vDepKmS = Math.sqrt(muSun * ((2.0 / rMarsDistKm) - (1.0 / a1Km)));
    const vInfMarsKmS = Math.abs(vDepKmS - vMarsCircKmS);

    const rParkMarsKm = rMarsKm + hpMarsKm;
    const vParkMarsKmS = Math.sqrt(muMars / rParkMarsKm);
    const vHypMarsKmS = Math.sqrt(Math.pow(vInfMarsKmS, 2.0) + ((2.0 * muMars) / rParkMarsKm));
    const dvTjiKmS = vHypMarsKmS - vParkMarsKmS;

    // Jupiter encounter
    const vJupiterCircKmS = Math.sqrt(muSun / rJupiterDistKm);
    const vArr1KmS = Math.sqrt(muSun * ((2.0 / rJupiterDistKm) - (1.0 / a1Km)));
    const vInfJKmS = Math.abs(vJupiterCircKmS - vArr1KmS);

    const rpJKm = rJupiterKm + hpJKm;
    const deltaJRad = 2.0 * Math.asin(1.0 / (1.0 + ((rpJKm * Math.pow(vInfJKmS, 2.0)) / muJupiter)));
    const deltaJDeg = (deltaJRad * 180.0) / Math.PI;

    // Leg 2: Jupiter to Saturn
    const a2Km = (rJupiterDistKm + rSaturnDistKm) / 2.0;
    const tof2Sec = Math.PI * Math.sqrt(Math.pow(a2Km, 3.0) / muSun);
    const tof2Days = tof2Sec / 86400.0;

    const totDays = tof1Days + tof2Days;
    const totYrs = totDays / 365.25;

    // Saturn arrival
    const vSaturnCircKmS = Math.sqrt(muSun / rSaturnDistKm);
    const vArr2KmS = Math.sqrt(muSun * ((2.0 / rSaturnDistKm) - (1.0 / a2Km)));
    const vInfSatKmS = Math.abs(vSaturnCircKmS - vArr2KmS);

    const rpSatKm = rSaturnKm + hpSatKm;
    const eCap = 0.90; // High-eccentricity capture
    const aCapKm = rpSatKm / (1.0 - eCap);

    const vHypSatKmS = Math.sqrt(Math.pow(vInfSatKmS, 2.0) + ((2.0 * muSaturn) / rpSatKm));
    const vCapSatKmS = Math.sqrt(muSaturn * ((2.0 / rpSatKm) - (1.0 / aCapKm)));
    const dvSoiKmS = vHypSatKmS - vCapSatKmS;

    const dvTotKmS = dvTjiKmS + dvSoiKmS;

    return {
      totalTimeDays: parseFloat(totDays.toFixed(1)),
      totalTimeYears: parseFloat(totYrs.toFixed(2)),
      marsDepartureDeltaVKmS: parseFloat(dvTjiKmS.toFixed(3)),
      jupiterFlybyExcessKmS: parseFloat(vInfJKmS.toFixed(3)),
      jupiterBendingAngleDeg: parseFloat(deltaJDeg.toFixed(1)),
      saturnOrbitInsertionDeltaVKmS: parseFloat(dvSoiKmS.toFixed(3)),
      totalMissionDeltaVKmS: parseFloat(dvTotKmS.toFixed(3)),
      grandTourContext: `Mars-Jupiter-Saturn GT (${totYrs.toFixed(1)} yr TOF, ${deltaJDeg.toFixed(1)} deg Jupiter Turn, ${dvTotKmS.toFixed(2)} km/s Total Delta-V)`
    };
  }

  /**
   * Calculate interplanetary trajectory from Mars to ice giant Uranus via Jupiter Gravity Assist (JUGA) and Uranus orbit capture.
   * a_1 = ( r_mars + r_jupiter ) / 2, a_2 = ( r_jupiter + r_uranus ) / 2
   * delta_J = 2 * arcsin( 1 / ( 1 + r_p * v_inf^2 / mu_jupiter ) )
   * Reference: Flandro (1966), Curtis (2013) for Outer Planet Gravity Assists.
   * @param {number} [marsParkingAltitudeKm=300.0] - Mars parking orbit altitude in km (150 to 1000 km)
   * @param {number} [jupiterFlybyAltitudeKm=400000.0] - Jupiter flyby periapsis altitude in km (100000 to 5000000 km)
   * @param {number} [uranusPeriapsisAltitudeKm=25000.0] - Uranus capture periapsis altitude in km (5000 to 200000 km)
   * @returns {{totalTimeDays: number, totalTimeYears: number, marsDepartureDeltaVKmS: number, jupiterFlybyExcessKmS: number, jupiterBendingAngleDeg: number, uranusOrbitInsertionDeltaVKmS: number, totalMissionDeltaVKmS: number, uranusGAContext: string}}
   */
  static computeMarsToUranusViaJupiterGravityAssist(marsParkingAltitudeKm = 300.0, jupiterFlybyAltitudeKm = 400000.0, uranusPeriapsisAltitudeKm = 25000.0) {
    const hpMarsKm = Math.max(150.0, marsParkingAltitudeKm);
    const hpJKm = Math.max(50000.0, jupiterFlybyAltitudeKm);
    const hpUKm = Math.max(5000.0, uranusPeriapsisAltitudeKm);

    const AU_KM = 1.495978707e8;
    const muSun = 1.32712440018e11;
    const muMars = 42828.37;
    const rMarsKm = 3389.5;
    const muJupiter = 1.26686534e8;
    const rJupiterKm = 71492.0;
    const muUranus = 5793939.0;
    const rUranusKm = 25362.0;

    const rMarsAU = 1.52368;
    const rJupiterAU = 5.2044;
    const rUranusAU = 19.2184;

    const rMarsDistKm = rMarsAU * AU_KM;
    const rJupiterDistKm = rJupiterAU * AU_KM;
    const rUranusDistKm = rUranusAU * AU_KM;

    // Leg 1: Mars to Jupiter
    const a1Km = (rMarsDistKm + rJupiterDistKm) / 2.0;
    const tof1Sec = Math.PI * Math.sqrt(Math.pow(a1Km, 3.0) / muSun);
    const tof1Days = tof1Sec / 86400.0;

    const vMarsCircKmS = Math.sqrt(muSun / rMarsDistKm);
    const vDepKmS = Math.sqrt(muSun * ((2.0 / rMarsDistKm) - (1.0 / a1Km)));
    const vInfMarsKmS = Math.abs(vDepKmS - vMarsCircKmS);

    const rParkMarsKm = rMarsKm + hpMarsKm;
    const vParkMarsKmS = Math.sqrt(muMars / rParkMarsKm);
    const vHypMarsKmS = Math.sqrt(Math.pow(vInfMarsKmS, 2.0) + ((2.0 * muMars) / rParkMarsKm));
    const dvTjiKmS = vHypMarsKmS - vParkMarsKmS;

    // Jupiter encounter
    const vJupiterCircKmS = Math.sqrt(muSun / rJupiterDistKm);
    const vArr1KmS = Math.sqrt(muSun * ((2.0 / rJupiterDistKm) - (1.0 / a1Km)));
    const vInfJKmS = Math.abs(vJupiterCircKmS - vArr1KmS);

    const rpJKm = rJupiterKm + hpJKm;
    const deltaJRad = 2.0 * Math.asin(1.0 / (1.0 + ((rpJKm * Math.pow(vInfJKmS, 2.0)) / muJupiter)));
    const deltaJDeg = (deltaJRad * 180.0) / Math.PI;

    // Leg 2: Jupiter to Uranus
    const a2Km = (rJupiterDistKm + rUranusDistKm) / 2.0;
    const tof2Sec = Math.PI * Math.sqrt(Math.pow(a2Km, 3.0) / muSun);
    const tof2Days = tof2Sec / 86400.0;

    const totDays = tof1Days + tof2Days;
    const totYrs = totDays / 365.25;

    // Uranus arrival
    const vUranusCircKmS = Math.sqrt(muSun / rUranusDistKm);
    const vArr2KmS = Math.sqrt(muSun * ((2.0 / rUranusDistKm) - (1.0 / a2Km)));
    const vInfUKmS = Math.abs(vUranusCircKmS - vArr2KmS);

    const rpUKm = rUranusKm + hpUKm;
    const eCap = 0.92; // Capture orbit
    const aCapKm = rpUKm / (1.0 - eCap);

    const vHypUKmS = Math.sqrt(Math.pow(vInfUKmS, 2.0) + ((2.0 * muUranus) / rpUKm));
    const vCapUKmS = Math.sqrt(muUranus * ((2.0 / rpUKm) - (1.0 / aCapKm)));
    const dvUoiKmS = vHypUKmS - vCapUKmS;

    const dvTotKmS = dvTjiKmS + dvUoiKmS;

    return {
      totalTimeDays: parseFloat(totDays.toFixed(1)),
      totalTimeYears: parseFloat(totYrs.toFixed(2)),
      marsDepartureDeltaVKmS: parseFloat(dvTjiKmS.toFixed(3)),
      jupiterFlybyExcessKmS: parseFloat(vInfJKmS.toFixed(3)),
      jupiterBendingAngleDeg: parseFloat(deltaJDeg.toFixed(1)),
      uranusOrbitInsertionDeltaVKmS: parseFloat(dvUoiKmS.toFixed(3)),
      totalMissionDeltaVKmS: parseFloat(dvTotKmS.toFixed(3)),
      uranusGAContext: `Mars-Jupiter-Uranus (${totYrs.toFixed(1)} yr TOF, ${deltaJDeg.toFixed(1)} deg Jupiter Turn, ${dvTotKmS.toFixed(2)} km/s Total Delta-V)`
    };
  }

  /**
   * Calculate interplanetary trajectory from Mars to outer ice giant Neptune via Jupiter Gravity Assist (JNGA) and Neptune orbit capture.
   * a_1 = ( r_mars + r_jupiter ) / 2, a_2 = ( r_jupiter + r_neptune ) / 2
   * delta_J = 2 * arcsin( 1 / ( 1 + r_p * v_inf^2 / mu_jupiter ) )
   * Reference: Flandro (1966), Curtis (2013) for Outer Planet Gravity Assists.
   * @param {number} [marsParkingAltitudeKm=300.0] - Mars parking orbit altitude in km (150 to 1000 km)
   * @param {number} [jupiterFlybyAltitudeKm=350000.0] - Jupiter flyby periapsis altitude in km (100000 to 5000000 km)
   * @param {number} [neptunePeriapsisAltitudeKm=20000.0] - Neptune capture periapsis altitude in km (5000 to 200000 km)
   * @returns {{totalTimeDays: number, totalTimeYears: number, marsDepartureDeltaVKmS: number, jupiterFlybyExcessKmS: number, jupiterBendingAngleDeg: number, neptuneOrbitInsertionDeltaVKmS: number, totalMissionDeltaVKmS: number, neptuneGAContext: string}}
   */
  static computeMarsToNeptuneViaJupiterGravityAssist(marsParkingAltitudeKm = 300.0, jupiterFlybyAltitudeKm = 350000.0, neptunePeriapsisAltitudeKm = 20000.0) {
    const hpMarsKm = Math.max(150.0, marsParkingAltitudeKm);
    const hpJKm = Math.max(50000.0, jupiterFlybyAltitudeKm);
    const hpNKm = Math.max(5000.0, neptunePeriapsisAltitudeKm);

    const AU_KM = 1.495978707e8;
    const muSun = 1.32712440018e11;
    const muMars = 42828.37;
    const rMarsKm = 3389.5;
    const muJupiter = 1.26686534e8;
    const rJupiterKm = 71492.0;
    const muNeptune = 6836529.0;
    const rNeptuneKm = 24622.0;

    const rMarsAU = 1.52368;
    const rJupiterAU = 5.2044;
    const rNeptuneAU = 30.0699;

    const rMarsDistKm = rMarsAU * AU_KM;
    const rJupiterDistKm = rJupiterAU * AU_KM;
    const rNeptuneDistKm = rNeptuneAU * AU_KM;

    // Leg 1: Mars to Jupiter
    const a1Km = (rMarsDistKm + rJupiterDistKm) / 2.0;
    const tof1Sec = Math.PI * Math.sqrt(Math.pow(a1Km, 3.0) / muSun);
    const tof1Days = tof1Sec / 86400.0;

    const vMarsCircKmS = Math.sqrt(muSun / rMarsDistKm);
    const vDepKmS = Math.sqrt(muSun * ((2.0 / rMarsDistKm) - (1.0 / a1Km)));
    const vInfMarsKmS = Math.abs(vDepKmS - vMarsCircKmS);

    const rParkMarsKm = rMarsKm + hpMarsKm;
    const vParkMarsKmS = Math.sqrt(muMars / rParkMarsKm);
    const vHypMarsKmS = Math.sqrt(Math.pow(vInfMarsKmS, 2.0) + ((2.0 * muMars) / rParkMarsKm));
    const dvTjiKmS = vHypMarsKmS - vParkMarsKmS;

    // Jupiter encounter
    const vJupiterCircKmS = Math.sqrt(muSun / rJupiterDistKm);
    const vArr1KmS = Math.sqrt(muSun * ((2.0 / rJupiterDistKm) - (1.0 / a1Km)));
    const vInfJKmS = Math.abs(vJupiterCircKmS - vArr1KmS);

    const rpJKm = rJupiterKm + hpJKm;
    const deltaJRad = 2.0 * Math.asin(1.0 / (1.0 + ((rpJKm * Math.pow(vInfJKmS, 2.0)) / muJupiter)));
    const deltaJDeg = (deltaJRad * 180.0) / Math.PI;

    // Leg 2: Jupiter to Neptune
    const a2Km = (rJupiterDistKm + rNeptuneDistKm) / 2.0;
    const tof2Sec = Math.PI * Math.sqrt(Math.pow(a2Km, 3.0) / muSun);
    const tof2Days = tof2Sec / 86400.0;

    const totDays = tof1Days + tof2Days;
    const totYrs = totDays / 365.25;

    // Neptune arrival
    const vNeptuneCircKmS = Math.sqrt(muSun / rNeptuneDistKm);
    const vArr2KmS = Math.sqrt(muSun * ((2.0 / rNeptuneDistKm) - (1.0 / a2Km)));
    const vInfNKmS = Math.abs(vNeptuneCircKmS - vArr2KmS);

    const rpNKm = rNeptuneKm + hpNKm;
    const eCap = 0.94; // Capture orbit
    const aCapKm = rpNKm / (1.0 - eCap);

    const vHypNKmS = Math.sqrt(Math.pow(vInfNKmS, 2.0) + ((2.0 * muNeptune) / rpNKm));
    const vCapNKmS = Math.sqrt(muNeptune * ((2.0 / rpNKm) - (1.0 / aCapKm)));
    const dvNoiKmS = vHypNKmS - vCapNKmS;

    const dvTotKmS = dvTjiKmS + dvNoiKmS;

    return {
      totalTimeDays: parseFloat(totDays.toFixed(1)),
      totalTimeYears: parseFloat(totYrs.toFixed(2)),
      marsDepartureDeltaVKmS: parseFloat(dvTjiKmS.toFixed(3)),
      jupiterFlybyExcessKmS: parseFloat(vInfJKmS.toFixed(3)),
      jupiterBendingAngleDeg: parseFloat(deltaJDeg.toFixed(1)),
      neptuneOrbitInsertionDeltaVKmS: parseFloat(dvNoiKmS.toFixed(3)),
      totalMissionDeltaVKmS: parseFloat(dvTotKmS.toFixed(3)),
      neptuneGAContext: `Mars-Jupiter-Neptune (${totYrs.toFixed(1)} yr TOF, ${deltaJDeg.toFixed(1)} deg Jupiter Turn, ${dvTotKmS.toFixed(2)} km/s Total Delta-V)`
    };
  }

  /**
   * Calculate interplanetary direct transfer trajectory from Mars to cold classical Kuiper Belt Object (KBO) 48695 Arrokoth (Ultima Thule).
   * a = ( r_mars + r_arrokoth ) / 2
   * e = ( r_arrokoth - r_mars ) / ( r_arrokoth + r_mars )
   * Reference: Stern et al. (2019), Spencer et al. (2020), Curtis (2013) for Kuiper Belt Exploration.
   * @param {number} [marsParkingAltitudeKm=300.0] - Mars parking orbit altitude in km (150 to 1000 km)
   * @param {number} [arrokothDistanceAU=44.581] - Arrokoth heliocentric distance in AU (40.0 to 50.0 AU)
   * @param {number} [flybyPericenterAltitudeKm=3500.0] - Flyby closest approach distance in km (500 to 50000 km)
   * @returns {{semiMajorAxisAU: number, eccentricity: number, timeOfFlightDays: number, timeOfFlightYears: number, marsDepartureDeltaVKmS: number, hyperbolicExcessVelocityKmS: number, encounterRelativeVelocityKmS: number, kboContext: string}}
   */
  static computeMarsToArrokothKBOTransfer(marsParkingAltitudeKm = 300.0, arrokothDistanceAU = 44.581, flybyPericenterAltitudeKm = 3500.0) {
    const hpMarsKm = Math.max(150.0, marsParkingAltitudeKm);
    const rAAU = Math.max(30.0, Math.min(60.0, arrokothDistanceAU));
    const hpAKm = Math.max(100.0, flybyPericenterAltitudeKm);

    const AU_KM = 1.495978707e8;
    const muSun = 1.32712440018e11;
    const muMars = 42828.37;
    const rMarsKm = 3389.5;
    const rMarsAU = 1.52368;

    const rMarsDistKm = rMarsAU * AU_KM;
    const rADistKm = rAAU * AU_KM;

    const aKm = (rMarsDistKm + rADistKm) / 2.0;
    const aAU = aKm / AU_KM;
    const ecc = (rADistKm - rMarsDistKm) / (rADistKm + rMarsDistKm);

    // Time of Flight (s -> days -> yr)
    const tofSec = Math.PI * Math.sqrt(Math.pow(aKm, 3.0) / muSun);
    const tofDays = tofSec / 86400.0;
    const tofYrs = tofDays / 365.25;

    // Mars departure
    const vMarsCircKmS = Math.sqrt(muSun / rMarsDistKm);
    const vDepKmS = Math.sqrt(muSun * ((2.0 / rMarsDistKm) - (1.0 / aKm)));
    const vInfMarsKmS = Math.abs(vDepKmS - vMarsCircKmS);

    const rParkMarsKm = rMarsKm + hpMarsKm;
    const vParkMarsKmS = Math.sqrt(muMars / rParkMarsKm);
    const vHypMarsKmS = Math.sqrt(Math.pow(vInfMarsKmS, 2.0) + ((2.0 * muMars) / rParkMarsKm));
    const dvTkiKmS = vHypMarsKmS - vParkMarsKmS;

    // Arrokoth encounter
    const vACircKmS = Math.sqrt(muSun / rADistKm);
    const vArrKmS = Math.sqrt(muSun * ((2.0 / rADistKm) - (1.0 / aKm)));
    const vRelKmS = Math.abs(vACircKmS - vArrKmS);

    return {
      semiMajorAxisAU: parseFloat(aAU.toFixed(3)),
      eccentricity: parseFloat(ecc.toFixed(4)),
      timeOfFlightDays: parseFloat(tofDays.toFixed(1)),
      timeOfFlightYears: parseFloat(tofYrs.toFixed(2)),
      marsDepartureDeltaVKmS: parseFloat(dvTkiKmS.toFixed(3)),
      hyperbolicExcessVelocityKmS: parseFloat(vInfMarsKmS.toFixed(3)),
      encounterRelativeVelocityKmS: parseFloat(vRelKmS.toFixed(3)),
      kboContext: `Mars-to-Arrokoth KBO (${tofYrs.toFixed(1)} yr TOF, e=${ecc.toFixed(4)}, Delta-V=${dvTkiKmS.toFixed(2)} km/s, V_rel=${vRelKmS.toFixed(2)} km/s)`
    };
  }

  /**
   * Calculate interplanetary direct transfer trajectory from Mars to Kuiper Belt dwarf planet 136472 Makemake and orbit capture.
   * a = ( r_mars + r_makemake ) / 2
   * e = ( r_makemake - r_mars ) / ( r_makemake + r_mars )
   * Reference: Brown (2008, 2013), Curtis (2013) for Kuiper Belt Exploration.
   * @param {number} [marsParkingAltitudeKm=300.0] - Mars parking orbit altitude in km (150 to 1000 km)
   * @param {number} [makemakeDistanceAU=45.79] - Makemake heliocentric distance in AU (38.0 to 53.0 AU)
   * @param {number} [makemakePeriapsisAltitudeKm=500.0] - Makemake orbit insertion periapsis altitude in km (100 to 10000 km)
   * @returns {{semiMajorAxisAU: number, eccentricity: number, timeOfFlightDays: number, timeOfFlightYears: number, marsDepartureDeltaVKmS: number, makemakeOrbitInsertionDeltaVKmS: number, totalMissionDeltaVKmS: number, makemakeContext: string}}
   */
  static computeMarsToMakemakeTransfer(marsParkingAltitudeKm = 300.0, makemakeDistanceAU = 45.79, makemakePeriapsisAltitudeKm = 500.0) {
    const hpMarsKm = Math.max(150.0, marsParkingAltitudeKm);
    const rMAU = Math.max(35.0, Math.min(60.0, makemakeDistanceAU));
    const hpMKm = Math.max(50.0, makemakePeriapsisAltitudeKm);

    const AU_KM = 1.495978707e8;
    const muSun = 1.32712440018e11;
    const muMars = 42828.37;
    const rMarsKm = 3389.5;
    const muMake = 205.0; // km^3/s^2
    const rMakeKm = 715.0; // km
    const rMarsAU = 1.52368;

    const rMarsDistKm = rMarsAU * AU_KM;
    const rMDistKm = rMAU * AU_KM;

    const aKm = (rMarsDistKm + rMDistKm) / 2.0;
    const aAU = aKm / AU_KM;
    const ecc = (rMDistKm - rMarsDistKm) / (rMDistKm + rMarsDistKm);

    // Time of Flight (s -> days -> yr)
    const tofSec = Math.PI * Math.sqrt(Math.pow(aKm, 3.0) / muSun);
    const tofDays = tofSec / 86400.0;
    const tofYrs = tofDays / 365.25;

    // Mars departure
    const vMarsCircKmS = Math.sqrt(muSun / rMarsDistKm);
    const vDepKmS = Math.sqrt(muSun * ((2.0 / rMarsDistKm) - (1.0 / aKm)));
    const vInfMarsKmS = Math.abs(vDepKmS - vMarsCircKmS);

    const rParkMarsKm = rMarsKm + hpMarsKm;
    const vParkMarsKmS = Math.sqrt(muMars / rParkMarsKm);
    const vHypMarsKmS = Math.sqrt(Math.pow(vInfMarsKmS, 2.0) + ((2.0 * muMars) / rParkMarsKm));
    const dvTmiKmS = vHypMarsKmS - vParkMarsKmS;

    // Makemake capture
    const vMCircKmS = Math.sqrt(muSun / rMDistKm);
    const vArrKmS = Math.sqrt(muSun * ((2.0 / rMDistKm) - (1.0 / aKm)));
    const vInfMKmS = Math.abs(vMCircKmS - vArrKmS);

    const rpMKm = rMakeKm + hpMKm;
    const eCap = 0.85; // High-eccentricity capture
    const aCapKm = rpMKm / (1.0 - eCap);

    const vHypMKmS = Math.sqrt(Math.pow(vInfMKmS, 2.0) + ((2.0 * muMake) / rpMKm));
    const vCapMKmS = Math.sqrt(muMake * ((2.0 / rpMKm) - (1.0 / aCapKm)));
    const dvMoiKmS = vHypMKmS - vCapMKmS;

    const dvTotKmS = dvTmiKmS + dvMoiKmS;

    return {
      semiMajorAxisAU: parseFloat(aAU.toFixed(3)),
      eccentricity: parseFloat(ecc.toFixed(4)),
      timeOfFlightDays: parseFloat(tofDays.toFixed(1)),
      timeOfFlightYears: parseFloat(tofYrs.toFixed(2)),
      marsDepartureDeltaVKmS: parseFloat(dvTmiKmS.toFixed(3)),
      makemakeOrbitInsertionDeltaVKmS: parseFloat(dvMoiKmS.toFixed(3)),
      totalMissionDeltaVKmS: parseFloat(dvTotKmS.toFixed(3)),
      makemakeContext: `Mars-to-Makemake (${tofYrs.toFixed(1)} yr TOF, e=${ecc.toFixed(4)}, Total Delta-V=${dvTotKmS.toFixed(2)} km/s, MOI=${dvMoiKmS.toFixed(2)} km/s)`
    };
  }

  /**
   * Calculate interplanetary direct transfer trajectory from Mars to resonant Kuiper Belt dwarf planet 136108 Haumea and orbit capture.
   * a = ( r_mars + r_haumea ) / 2
   * e = ( r_haumea - r_mars ) / ( r_haumea + r_mars )
   * Reference: Rabinowitz et al. (2006), Brown (2008), Curtis (2013) for Kuiper Belt Exploration.
   * @param {number} [marsParkingAltitudeKm=300.0] - Mars parking orbit altitude in km (150 to 1000 km)
   * @param {number} [haumeaDistanceAU=43.13] - Haumea heliocentric distance in AU (35.0 to 52.0 AU)
   * @param {number} [haumeaPeriapsisAltitudeKm=400.0] - Haumea orbit insertion periapsis altitude in km (100 to 10000 km)
   * @returns {{semiMajorAxisAU: number, eccentricity: number, timeOfFlightDays: number, timeOfFlightYears: number, marsDepartureDeltaVKmS: number, haumeaOrbitInsertionDeltaVKmS: number, totalMissionDeltaVKmS: number, haumeaContext: string}}
   */
  static computeMarsToHaumeaTransfer(marsParkingAltitudeKm = 300.0, haumeaDistanceAU = 43.13, haumeaPeriapsisAltitudeKm = 400.0) {
    const hpMarsKm = Math.max(150.0, marsParkingAltitudeKm);
    const rHAU = Math.max(30.0, Math.min(60.0, haumeaDistanceAU));
    const hpHKm = Math.max(50.0, haumeaPeriapsisAltitudeKm);

    const AU_KM = 1.495978707e8;
    const muSun = 1.32712440018e11;
    const muMars = 42828.37;
    const rMarsKm = 3389.5;
    const muHau = 267.4; // km^3/s^2
    const rHauKm = 620.0; // km
    const rMarsAU = 1.52368;

    const rMarsDistKm = rMarsAU * AU_KM;
    const rHDistKm = rHAU * AU_KM;

    const aKm = (rMarsDistKm + rHDistKm) / 2.0;
    const aAU = aKm / AU_KM;
    const ecc = (rHDistKm - rMarsDistKm) / (rHDistKm + rMarsDistKm);

    // Time of Flight (s -> days -> yr)
    const tofSec = Math.PI * Math.sqrt(Math.pow(aKm, 3.0) / muSun);
    const tofDays = tofSec / 86400.0;
    const tofYrs = tofDays / 365.25;

    // Mars departure
    const vMarsCircKmS = Math.sqrt(muSun / rMarsDistKm);
    const vDepKmS = Math.sqrt(muSun * ((2.0 / rMarsDistKm) - (1.0 / aKm)));
    const vInfMarsKmS = Math.abs(vDepKmS - vMarsCircKmS);

    const rParkMarsKm = rMarsKm + hpMarsKm;
    const vParkMarsKmS = Math.sqrt(muMars / rParkMarsKm);
    const vHypMarsKmS = Math.sqrt(Math.pow(vInfMarsKmS, 2.0) + ((2.0 * muMars) / rParkMarsKm));
    const dvThiKmS = vHypMarsKmS - vParkMarsKmS;

    // Haumea capture
    const vHCircKmS = Math.sqrt(muSun / rHDistKm);
    const vArrKmS = Math.sqrt(muSun * ((2.0 / rHDistKm) - (1.0 / aKm)));
    const vInfHKmS = Math.abs(vHCircKmS - vArrKmS);

    const rpHKm = rHauKm + hpHKm;
    const eCap = 0.85; // Capture orbit
    const aCapKm = rpHKm / (1.0 - eCap);

    const vHypHKmS = Math.sqrt(Math.pow(vInfHKmS, 2.0) + ((2.0 * muHau) / rpHKm));
    const vCapHKmS = Math.sqrt(muHau * ((2.0 / rpHKm) - (1.0 / aCapKm)));
    const dvHoiKmS = vHypHKmS - vCapHKmS;

    const dvTotKmS = dvThiKmS + dvHoiKmS;

    return {
      semiMajorAxisAU: parseFloat(aAU.toFixed(3)),
      eccentricity: parseFloat(ecc.toFixed(4)),
      timeOfFlightDays: parseFloat(tofDays.toFixed(1)),
      timeOfFlightYears: parseFloat(tofYrs.toFixed(2)),
      marsDepartureDeltaVKmS: parseFloat(dvThiKmS.toFixed(3)),
      haumeaOrbitInsertionDeltaVKmS: parseFloat(dvHoiKmS.toFixed(3)),
      totalMissionDeltaVKmS: parseFloat(dvTotKmS.toFixed(3)),
      haumeaContext: `Mars-to-Haumea (${tofYrs.toFixed(1)} yr TOF, e=${ecc.toFixed(4)}, Total Delta-V=${dvTotKmS.toFixed(2)} km/s, HOI=${dvHoiKmS.toFixed(2)} km/s)`
    };
  }

  /**
   * Calculate interplanetary direct transfer trajectory from Mars to detached extreme trans-Neptunian object (ETNO) / inner Oort cloud dwarf planet 90377 Sedna and orbit capture.
   * a = ( r_mars + r_sedna ) / 2
   * e = ( r_sedna - r_mars ) / ( r_sedna + r_mars )
   * Reference: Brown, Trujillo & Rabinowitz (2004), Schwamb et al. (2010), Curtis (2013) for Inner Oort Cloud Exploration.
   * @param {number} [marsParkingAltitudeKm=300.0] - Mars parking orbit altitude in km (150 to 1000 km)
   * @param {number} [sednaDistanceAU=84.0] - Sedna heliocentric distance in AU (76.0 to 120.0 AU)
   * @param {number} [sednaPeriapsisAltitudeKm=300.0] - Sedna orbit insertion periapsis altitude in km (100 to 5000 km)
   * @returns {{semiMajorAxisAU: number, eccentricity: number, timeOfFlightDays: number, timeOfFlightYears: number, marsDepartureDeltaVKmS: number, sednaOrbitInsertionDeltaVKmS: number, totalMissionDeltaVKmS: number, sednaContext: string}}
   */
  static computeMarsToSednaETNOTransfer(marsParkingAltitudeKm = 300.0, sednaDistanceAU = 84.0, sednaPeriapsisAltitudeKm = 300.0) {
    const hpMarsKm = Math.max(150.0, marsParkingAltitudeKm);
    const rSAU = Math.max(60.0, Math.min(150.0, sednaDistanceAU));
    const hpSKm = Math.max(50.0, sednaPeriapsisAltitudeKm);

    const AU_KM = 1.495978707e8;
    const muSun = 1.32712440018e11;
    const muMars = 42828.37;
    const rMarsKm = 3389.5;
    const muSedna = 120.0; // km^3/s^2
    const rSednaKm = 500.0; // km
    const rMarsAU = 1.52368;

    const rMarsDistKm = rMarsAU * AU_KM;
    const rSDistKm = rSAU * AU_KM;

    const aKm = (rMarsDistKm + rSDistKm) / 2.0;
    const aAU = aKm / AU_KM;
    const ecc = (rSDistKm - rMarsDistKm) / (rSDistKm + rMarsDistKm);

    // Time of Flight (s -> days -> yr)
    const tofSec = Math.PI * Math.sqrt(Math.pow(aKm, 3.0) / muSun);
    const tofDays = tofSec / 86400.0;
    const tofYrs = tofDays / 365.25;

    // Mars departure
    const vMarsCircKmS = Math.sqrt(muSun / rMarsDistKm);
    const vDepKmS = Math.sqrt(muSun * ((2.0 / rMarsDistKm) - (1.0 / aKm)));
    const vInfMarsKmS = Math.abs(vDepKmS - vMarsCircKmS);

    const rParkMarsKm = rMarsKm + hpMarsKm;
    const vParkMarsKmS = Math.sqrt(muMars / rParkMarsKm);
    const vHypMarsKmS = Math.sqrt(Math.pow(vInfMarsKmS, 2.0) + ((2.0 * muMars) / rParkMarsKm));
    const dvTsiKmS = vHypMarsKmS - vParkMarsKmS;

    // Sedna capture
    const vSCircKmS = Math.sqrt(muSun / rSDistKm);
    const vArrKmS = Math.sqrt(muSun * ((2.0 / rSDistKm) - (1.0 / aKm)));
    const vInfSKmS = Math.abs(vSCircKmS - vArrKmS);

    const rpSKm = rSednaKm + hpSKm;
    const eCap = 0.85; // Capture orbit
    const aCapKm = rpSKm / (1.0 - eCap);

    const vHypSKmS = Math.sqrt(Math.pow(vInfSKmS, 2.0) + ((2.0 * muSedna) / rpSKm));
    const vCapSKmS = Math.sqrt(muSedna * ((2.0 / rpSKm) - (1.0 / aCapKm)));
    const dvSoiKmS = vHypSKmS - vCapSKmS;

    const dvTotKmS = dvTsiKmS + dvSoiKmS;

    return {
      semiMajorAxisAU: parseFloat(aAU.toFixed(3)),
      eccentricity: parseFloat(ecc.toFixed(4)),
      timeOfFlightDays: parseFloat(tofDays.toFixed(1)),
      timeOfFlightYears: parseFloat(tofYrs.toFixed(2)),
      marsDepartureDeltaVKmS: parseFloat(dvTsiKmS.toFixed(3)),
      sednaOrbitInsertionDeltaVKmS: parseFloat(dvSoiKmS.toFixed(3)),
      totalMissionDeltaVKmS: parseFloat(dvTotKmS.toFixed(3)),
      sednaContext: `Mars-to-Sedna (${tofYrs.toFixed(1)} yr TOF, e=${ecc.toFixed(4)}, Total Delta-V=${dvTotKmS.toFixed(2)} km/s, SOI=${dvSoiKmS.toFixed(2)} km/s)`
    };
  }

  /**
   * Calculate interplanetary direct transfer trajectory from Mars to massive scattered disc dwarf planet 136199 Eris and orbit capture.
   * a = ( r_mars + r_eris ) / 2
   * e = ( r_eris - r_mars ) / ( r_eris + r_mars )
   * Reference: Brown et al. (2005, 2007), Sicardy et al. (2011), Curtis (2013) for Scattered Disc Exploration.
   * @param {number} [marsParkingAltitudeKm=300.0] - Mars parking orbit altitude in km (150 to 1000 km)
   * @param {number} [erisDistanceAU=95.88] - Eris heliocentric distance in AU (80.0 to 105.0 AU)
   * @param {number} [erisPeriapsisAltitudeKm=500.0] - Eris orbit insertion periapsis altitude in km (100 to 5000 km)
   * @returns {{semiMajorAxisAU: number, eccentricity: number, timeOfFlightDays: number, timeOfFlightYears: number, marsDepartureDeltaVKmS: number, erisOrbitInsertionDeltaVKmS: number, totalMissionDeltaVKmS: number, erisContext: string}}
   */
  static computeMarsToErisTransfer(marsParkingAltitudeKm = 300.0, erisDistanceAU = 95.88, erisPeriapsisAltitudeKm = 500.0) {
    const hpMarsKm = Math.max(150.0, marsParkingAltitudeKm);
    const rEAU = Math.max(70.0, Math.min(120.0, erisDistanceAU));
    const hpEKm = Math.max(50.0, erisPeriapsisAltitudeKm);

    const AU_KM = 1.495978707e8;
    const muSun = 1.32712440018e11;
    const muMars = 42828.37;
    const rMarsKm = 3389.5;
    const muEris = 1108.0; // km^3/s^2
    const rErisKm = 1163.0; // km
    const rMarsAU = 1.52368;

    const rMarsDistKm = rMarsAU * AU_KM;
    const rEDistKm = rEAU * AU_KM;

    const aKm = (rMarsDistKm + rEDistKm) / 2.0;
    const aAU = aKm / AU_KM;
    const ecc = (rEDistKm - rMarsDistKm) / (rEDistKm + rMarsDistKm);

    // Time of Flight (s -> days -> yr)
    const tofSec = Math.PI * Math.sqrt(Math.pow(aKm, 3.0) / muSun);
    const tofDays = tofSec / 86400.0;
    const tofYrs = tofDays / 365.25;

    // Mars departure
    const vMarsCircKmS = Math.sqrt(muSun / rMarsDistKm);
    const vDepKmS = Math.sqrt(muSun * ((2.0 / rMarsDistKm) - (1.0 / aKm)));
    const vInfMarsKmS = Math.abs(vDepKmS - vMarsCircKmS);

    const rParkMarsKm = rMarsKm + hpMarsKm;
    const vParkMarsKmS = Math.sqrt(muMars / rParkMarsKm);
    const vHypMarsKmS = Math.sqrt(Math.pow(vInfMarsKmS, 2.0) + ((2.0 * muMars) / rParkMarsKm));
    const dvTeiKmS = vHypMarsKmS - vParkMarsKmS;

    // Eris capture
    const vECircKmS = Math.sqrt(muSun / rEDistKm);
    const vArrKmS = Math.sqrt(muSun * ((2.0 / rEDistKm) - (1.0 / aKm)));
    const vInfEKmS = Math.abs(vECircKmS - vArrKmS);

    const rpEKm = rErisKm + hpEKm;
    const eCap = 0.85; // Capture orbit
    const aCapKm = rpEKm / (1.0 - eCap);

    const vHypEKmS = Math.sqrt(Math.pow(vInfEKmS, 2.0) + ((2.0 * muEris) / rpEKm));
    const vCapEKmS = Math.sqrt(muEris * ((2.0 / rpEKm) - (1.0 / aCapKm)));
    const dvEoiKmS = vHypEKmS - vCapEKmS;

    const dvTotKmS = dvTeiKmS + dvEoiKmS;

    return {
      semiMajorAxisAU: parseFloat(aAU.toFixed(3)),
      eccentricity: parseFloat(ecc.toFixed(4)),
      timeOfFlightDays: parseFloat(tofDays.toFixed(1)),
      timeOfFlightYears: parseFloat(tofYrs.toFixed(2)),
      marsDepartureDeltaVKmS: parseFloat(dvTeiKmS.toFixed(3)),
      erisOrbitInsertionDeltaVKmS: parseFloat(dvEoiKmS.toFixed(3)),
      totalMissionDeltaVKmS: parseFloat(dvTotKmS.toFixed(3)),
      erisContext: `Mars-to-Eris (${tofYrs.toFixed(1)} yr TOF, e=${ecc.toFixed(4)}, Total Delta-V=${dvTotKmS.toFixed(2)} km/s, EOI=${dvEoiKmS.toFixed(2)} km/s)`
    };
  }

  /**
   * Calculate interplanetary direct transfer trajectory from Mars to resonant scattered disc dwarf planet 225088 Gonggong (2007 OR10) and orbit capture.
   * a = ( r_mars + r_gonggong ) / 2
   * e = ( r_gonggong - r_mars ) / ( r_gonggong + r_mars )
   * Reference: Schwamb et al. (2010), Kiss et al. (2019), Curtis (2013) for Scattered Disc Exploration.
   * @param {number} [marsParkingAltitudeKm=300.0] - Mars parking orbit altitude in km (150 to 1000 km)
   * @param {number} [gonggongDistanceAU=88.70] - Gonggong heliocentric distance in AU (70.0 to 110.0 AU)
   * @param {number} [gonggongPeriapsisAltitudeKm=300.0] - Gonggong orbit insertion periapsis altitude in km (100 to 5000 km)
   * @returns {{semiMajorAxisAU: number, eccentricity: number, timeOfFlightDays: number, timeOfFlightYears: number, marsDepartureDeltaVKmS: number, gonggongOrbitInsertionDeltaVKmS: number, totalMissionDeltaVKmS: number, gonggongContext: string}}
   */
  static computeMarsToGonggongTransfer(marsParkingAltitudeKm = 300.0, gonggongDistanceAU = 88.70, gonggongPeriapsisAltitudeKm = 300.0) {
    const hpMarsKm = Math.max(150.0, marsParkingAltitudeKm);
    const rGAU = Math.max(60.0, Math.min(120.0, gonggongDistanceAU));
    const hpGKm = Math.max(50.0, gonggongPeriapsisAltitudeKm);

    const AU_KM = 1.495978707e8;
    const muSun = 1.32712440018e11;
    const muMars = 42828.37;
    const rMarsKm = 3389.5;
    const muGong = 116.7; // km^3/s^2
    const rGongKm = 615.0; // km
    const rMarsAU = 1.52368;

    const rMarsDistKm = rMarsAU * AU_KM;
    const rGDistKm = rGAU * AU_KM;

    const aKm = (rMarsDistKm + rGDistKm) / 2.0;
    const aAU = aKm / AU_KM;
    const ecc = (rGDistKm - rMarsDistKm) / (rGDistKm + rMarsDistKm);

    // Time of Flight (s -> days -> yr)
    const tofSec = Math.PI * Math.sqrt(Math.pow(aKm, 3.0) / muSun);
    const tofDays = tofSec / 86400.0;
    const tofYrs = tofDays / 365.25;

    // Mars departure
    const vMarsCircKmS = Math.sqrt(muSun / rMarsDistKm);
    const vDepKmS = Math.sqrt(muSun * ((2.0 / rMarsDistKm) - (1.0 / aKm)));
    const vInfMarsKmS = Math.abs(vDepKmS - vMarsCircKmS);

    const rParkMarsKm = rMarsKm + hpMarsKm;
    const vParkMarsKmS = Math.sqrt(muMars / rParkMarsKm);
    const vHypMarsKmS = Math.sqrt(Math.pow(vInfMarsKmS, 2.0) + ((2.0 * muMars) / rParkMarsKm));
    const dvTgiKmS = vHypMarsKmS - vParkMarsKmS;

    // Gonggong capture
    const vGCircKmS = Math.sqrt(muSun / rGDistKm);
    const vArrKmS = Math.sqrt(muSun * ((2.0 / rGDistKm) - (1.0 / aKm)));
    const vInfGKmS = Math.abs(vGCircKmS - vArrKmS);

    const rpGKm = rGongKm + hpGKm;
    const eCap = 0.85; // Capture orbit
    const aCapKm = rpGKm / (1.0 - eCap);

    const vHypGKmS = Math.sqrt(Math.pow(vInfGKmS, 2.0) + ((2.0 * muGong) / rpGKm));
    const vCapGKmS = Math.sqrt(muGong * ((2.0 / rpGKm) - (1.0 / aCapKm)));
    const dvGoiKmS = vHypGKmS - vCapGKmS;

    const dvTotKmS = dvTgiKmS + dvGoiKmS;

    return {
      semiMajorAxisAU: parseFloat(aAU.toFixed(3)),
      eccentricity: parseFloat(ecc.toFixed(4)),
      timeOfFlightDays: parseFloat(tofDays.toFixed(1)),
      timeOfFlightYears: parseFloat(tofYrs.toFixed(2)),
      marsDepartureDeltaVKmS: parseFloat(dvTgiKmS.toFixed(3)),
      gonggongOrbitInsertionDeltaVKmS: parseFloat(dvGoiKmS.toFixed(3)),
      totalMissionDeltaVKmS: parseFloat(dvTotKmS.toFixed(3)),
      gonggongContext: `Mars-to-Gonggong (${tofYrs.toFixed(1)} yr TOF, e=${ecc.toFixed(4)}, Total Delta-V=${dvTotKmS.toFixed(2)} km/s, GOI=${dvGoiKmS.toFixed(2)} km/s)`
    };
  }

  /**
   * Calculate interplanetary direct transfer trajectory from Mars to 2:3 resonant Plutino dwarf planet 90482 Orcus and orbit capture.
   * a = ( r_mars + r_orcus ) / 2
   * e = ( r_orcus - r_mars ) / ( r_orcus + r_mars )
   * Reference: Brown et al. (2010), Ortiz et al. (2011), Curtis (2013) for Kuiper Belt Plutino Exploration.
   * @param {number} [marsParkingAltitudeKm=300.0] - Mars parking orbit altitude in km (150 to 1000 km)
   * @param {number} [orcusDistanceAU=47.88] - Orcus heliocentric distance in AU (30.0 to 55.0 AU)
   * @param {number} [orcusPeriapsisAltitudeKm=250.0] - Orcus orbit insertion periapsis altitude in km (50 to 5000 km)
   * @returns {{semiMajorAxisAU: number, eccentricity: number, timeOfFlightDays: number, timeOfFlightYears: number, marsDepartureDeltaVKmS: number, orcusOrbitInsertionDeltaVKmS: number, totalMissionDeltaVKmS: number, orcusContext: string}}
   */
  static computeMarsToOrcusTransfer(marsParkingAltitudeKm = 300.0, orcusDistanceAU = 47.88, orcusPeriapsisAltitudeKm = 250.0) {
    const hpMarsKm = Math.max(150.0, marsParkingAltitudeKm);
    const rOAU = Math.max(25.0, Math.min(65.0, orcusDistanceAU));
    const hpOKm = Math.max(30.0, orcusPeriapsisAltitudeKm);

    const AU_KM = 1.495978707e8;
    const muSun = 1.32712440018e11;
    const muMars = 42828.37;
    const rMarsKm = 3389.5;
    const muOrcus = 42.8; // km^3/s^2
    const rOrcusKm = 458.0; // km
    const rMarsAU = 1.52368;

    const rMarsDistKm = rMarsAU * AU_KM;
    const rODistKm = rOAU * AU_KM;

    const aKm = (rMarsDistKm + rODistKm) / 2.0;
    const aAU = aKm / AU_KM;
    const ecc = (rODistKm - rMarsDistKm) / (rODistKm + rMarsDistKm);

    // Time of Flight (s -> days -> yr)
    const tofSec = Math.PI * Math.sqrt(Math.pow(aKm, 3.0) / muSun);
    const tofDays = tofSec / 86400.0;
    const tofYrs = tofDays / 365.25;

    // Mars departure
    const vMarsCircKmS = Math.sqrt(muSun / rMarsDistKm);
    const vDepKmS = Math.sqrt(muSun * ((2.0 / rMarsDistKm) - (1.0 / aKm)));
    const vInfMarsKmS = Math.abs(vDepKmS - vMarsCircKmS);

    const rParkMarsKm = rMarsKm + hpMarsKm;
    const vParkMarsKmS = Math.sqrt(muMars / rParkMarsKm);
    const vHypMarsKmS = Math.sqrt(Math.pow(vInfMarsKmS, 2.0) + ((2.0 * muMars) / rParkMarsKm));
    const dvToiKmS = vHypMarsKmS - vParkMarsKmS;

    // Orcus capture
    const vOCircKmS = Math.sqrt(muSun / rODistKm);
    const vArrKmS = Math.sqrt(muSun * ((2.0 / rODistKm) - (1.0 / aKm)));
    const vInfOKmS = Math.abs(vOCircKmS - vArrKmS);

    const rpOKm = rOrcusKm + hpOKm;
    const eCap = 0.85; // Capture orbit
    const aCapKm = rpOKm / (1.0 - eCap);

    const vHypOKmS = Math.sqrt(Math.pow(vInfOKmS, 2.0) + ((2.0 * muOrcus) / rpOKm));
    const vCapOKmS = Math.sqrt(muOrcus * ((2.0 / rpOKm) - (1.0 / aCapKm)));
    const dvOoiKmS = vHypOKmS - vCapOKmS;

    const dvTotKmS = dvToiKmS + dvOoiKmS;

    return {
      semiMajorAxisAU: parseFloat(aAU.toFixed(3)),
      eccentricity: parseFloat(ecc.toFixed(4)),
      timeOfFlightDays: parseFloat(tofDays.toFixed(1)),
      timeOfFlightYears: parseFloat(tofYrs.toFixed(2)),
      marsDepartureDeltaVKmS: parseFloat(dvToiKmS.toFixed(3)),
      orcusOrbitInsertionDeltaVKmS: parseFloat(dvOoiKmS.toFixed(3)),
      totalMissionDeltaVKmS: parseFloat(dvTotKmS.toFixed(3)),
      orcusContext: `Mars-to-Orcus (${tofYrs.toFixed(1)} yr TOF, e=${ecc.toFixed(4)}, Total Delta-V=${dvTotKmS.toFixed(2)} km/s, OOI=${dvOoiKmS.toFixed(2)} km/s)`
    };
  }

  /**
   * Calculate interplanetary direct transfer trajectory from Mars to classical Kuiper Belt Cubewano dwarf planet 50000 Quaoar and orbit capture.
   * a = ( r_mars + r_quaoar ) / 2
   * e = ( r_quaoar - r_mars ) / ( r_quaoar + r_mars )
   * Reference: Brown & Trujillo (2004), Braga-Ribas et al. (2013), Morgado et al. (2023), Curtis (2013) for Cubewano Exploration.
   * @param {number} [marsParkingAltitudeKm=300.0] - Mars parking orbit altitude in km (150 to 1000 km)
   * @param {number} [quaoarDistanceAU=43.40] - Quaoar heliocentric distance in AU (35.0 to 55.0 AU)
   * @param {number} [quaoarPeriapsisAltitudeKm=300.0] - Quaoar orbit insertion periapsis altitude in km (50 to 5000 km)
   * @returns {{semiMajorAxisAU: number, eccentricity: number, timeOfFlightDays: number, timeOfFlightYears: number, marsDepartureDeltaVKmS: number, quaoarOrbitInsertionDeltaVKmS: number, totalMissionDeltaVKmS: number, quaoarContext: string}}
   */
  static computeMarsToQuaoarTransfer(marsParkingAltitudeKm = 300.0, quaoarDistanceAU = 43.40, quaoarPeriapsisAltitudeKm = 300.0) {
    const hpMarsKm = Math.max(150.0, marsParkingAltitudeKm);
    const rQAU = Math.max(30.0, Math.min(60.0, quaoarDistanceAU));
    const hpQKm = Math.max(30.0, quaoarPeriapsisAltitudeKm);

    const AU_KM = 1.495978707e8;
    const muSun = 1.32712440018e11;
    const muMars = 42828.37;
    const rMarsKm = 3389.5;
    const muQua = 80.0; // km^3/s^2
    const rQuaKm = 545.0; // km
    const rMarsAU = 1.52368;

    const rMarsDistKm = rMarsAU * AU_KM;
    const rQDistKm = rQAU * AU_KM;

    const aKm = (rMarsDistKm + rQDistKm) / 2.0;
    const aAU = aKm / AU_KM;
    const ecc = (rQDistKm - rMarsDistKm) / (rQDistKm + rMarsDistKm);

    // Time of Flight (s -> days -> yr)
    const tofSec = Math.PI * Math.sqrt(Math.pow(aKm, 3.0) / muSun);
    const tofDays = tofSec / 86400.0;
    const tofYrs = tofDays / 365.25;

    // Mars departure
    const vMarsCircKmS = Math.sqrt(muSun / rMarsDistKm);
    const vDepKmS = Math.sqrt(muSun * ((2.0 / rMarsDistKm) - (1.0 / aKm)));
    const vInfMarsKmS = Math.abs(vDepKmS - vMarsCircKmS);

    const rParkMarsKm = rMarsKm + hpMarsKm;
    const vParkMarsKmS = Math.sqrt(muMars / rParkMarsKm);
    const vHypMarsKmS = Math.sqrt(Math.pow(vInfMarsKmS, 2.0) + ((2.0 * muMars) / rParkMarsKm));
    const dvTqiKmS = vHypMarsKmS - vParkMarsKmS;

    // Quaoar capture
    const vQCircKmS = Math.sqrt(muSun / rQDistKm);
    const vArrKmS = Math.sqrt(muSun * ((2.0 / rQDistKm) - (1.0 / aKm)));
    const vInfQKmS = Math.abs(vQCircKmS - vArrKmS);

    const rpQKm = rQuaKm + hpQKm;
    const eCap = 0.85; // Capture orbit
    const aCapKm = rpQKm / (1.0 - eCap);

    const vHypQKmS = Math.sqrt(Math.pow(vInfQKmS, 2.0) + ((2.0 * muQua) / rpQKm));
    const vCapQKmS = Math.sqrt(muQua * ((2.0 / rpQKm) - (1.0 / aCapKm)));
    const dvQoiKmS = vHypQKmS - vCapQKmS;

    const dvTotKmS = dvTqiKmS + dvQoiKmS;

    return {
      semiMajorAxisAU: parseFloat(aAU.toFixed(3)),
      eccentricity: parseFloat(ecc.toFixed(4)),
      timeOfFlightDays: parseFloat(tofDays.toFixed(1)),
      timeOfFlightYears: parseFloat(tofYrs.toFixed(2)),
      marsDepartureDeltaVKmS: parseFloat(dvTqiKmS.toFixed(3)),
      quaoarOrbitInsertionDeltaVKmS: parseFloat(dvQoiKmS.toFixed(3)),
      totalMissionDeltaVKmS: parseFloat(dvTotKmS.toFixed(3)),
      quaoarContext: `Mars-to-Quaoar (${tofYrs.toFixed(1)} yr TOF, e=${ecc.toFixed(4)}, Total Delta-V=${dvTotKmS.toFixed(2)} km/s, QOI=${dvQoiKmS.toFixed(2)} km/s)`
    };
  }

  /**
   * Calculate interplanetary direct transfer trajectory from Mars to rapidly rotating classical Kuiper Belt object 20000 Varuna and orbit capture.
   * a = ( r_mars + r_varuna ) / 2
   * e = ( r_varuna - r_mars ) / ( r_varuna + r_mars )
   * Reference: Jewitt et al. (2001), Lellouch et al. (2013), Curtis (2013) for Classical KBO Exploration.
   * @param {number} [marsParkingAltitudeKm=300.0] - Mars parking orbit altitude in km (150 to 1000 km)
   * @param {number} [varunaDistanceAU=43.05] - Varuna heliocentric distance in AU (35.0 to 55.0 AU)
   * @param {number} [varunaPeriapsisAltitudeKm=200.0] - Varuna orbit insertion periapsis altitude in km (50 to 5000 km)
   * @returns {{semiMajorAxisAU: number, eccentricity: number, timeOfFlightDays: number, timeOfFlightYears: number, marsDepartureDeltaVKmS: number, varunaOrbitInsertionDeltaVKmS: number, totalMissionDeltaVKmS: number, varunaContext: string}}
   */
  static computeMarsToVarunaTransfer(marsParkingAltitudeKm = 300.0, varunaDistanceAU = 43.05, varunaPeriapsisAltitudeKm = 200.0) {
    const hpMarsKm = Math.max(150.0, marsParkingAltitudeKm);
    const rVAU = Math.max(30.0, Math.min(60.0, varunaDistanceAU));
    const hpVKm = Math.max(30.0, varunaPeriapsisAltitudeKm);

    const AU_KM = 1.495978707e8;
    const muSun = 1.32712440018e11;
    const muMars = 42828.37;
    const rMarsKm = 3389.5;
    const muVar = 25.0; // km^3/s^2
    const rVarKm = 334.0; // km
    const rMarsAU = 1.52368;

    const rMarsDistKm = rMarsAU * AU_KM;
    const rVDistKm = rVAU * AU_KM;

    const aKm = (rMarsDistKm + rVDistKm) / 2.0;
    const aAU = aKm / AU_KM;
    const ecc = (rVDistKm - rMarsDistKm) / (rVDistKm + rMarsDistKm);

    // Time of Flight (s -> days -> yr)
    const tofSec = Math.PI * Math.sqrt(Math.pow(aKm, 3.0) / muSun);
    const tofDays = tofSec / 86400.0;
    const tofYrs = tofDays / 365.25;

    // Mars departure
    const vMarsCircKmS = Math.sqrt(muSun / rMarsDistKm);
    const vDepKmS = Math.sqrt(muSun * ((2.0 / rMarsDistKm) - (1.0 / aKm)));
    const vInfMarsKmS = Math.abs(vDepKmS - vMarsCircKmS);

    const rParkMarsKm = rMarsKm + hpMarsKm;
    const vParkMarsKmS = Math.sqrt(muMars / rParkMarsKm);
    const vHypMarsKmS = Math.sqrt(Math.pow(vInfMarsKmS, 2.0) + ((2.0 * muMars) / rParkMarsKm));
    const dvTviKmS = vHypMarsKmS - vParkMarsKmS;

    // Varuna capture
    const vVCircKmS = Math.sqrt(muSun / rVDistKm);
    const vArrKmS = Math.sqrt(muSun * ((2.0 / rVDistKm) - (1.0 / aKm)));
    const vInfVKmS = Math.abs(vVCircKmS - vArrKmS);

    const rpVKm = rVarKm + hpVKm;
    const eCap = 0.85; // Capture orbit
    const aCapKm = rpVKm / (1.0 - eCap);

    const vHypVKmS = Math.sqrt(Math.pow(vInfVKmS, 2.0) + ((2.0 * muVar) / rpVKm));
    const vCapVKmS = Math.sqrt(muVar * ((2.0 / rpVKm) - (1.0 / aCapKm)));
    const dvVoiKmS = vHypVKmS - vCapVKmS;

    const dvTotKmS = dvTviKmS + dvVoiKmS;

    return {
      semiMajorAxisAU: parseFloat(aAU.toFixed(3)),
      eccentricity: parseFloat(ecc.toFixed(4)),
      timeOfFlightDays: parseFloat(tofDays.toFixed(1)),
      timeOfFlightYears: parseFloat(tofYrs.toFixed(2)),
      marsDepartureDeltaVKmS: parseFloat(dvTviKmS.toFixed(3)),
      varunaOrbitInsertionDeltaVKmS: parseFloat(dvVoiKmS.toFixed(3)),
      totalMissionDeltaVKmS: parseFloat(dvTotKmS.toFixed(3)),
      varunaContext: `Mars-to-Varuna (${tofYrs.toFixed(1)} yr TOF, e=${ecc.toFixed(4)}, Total Delta-V=${dvTotKmS.toFixed(2)} km/s, VOI=${dvVoiKmS.toFixed(2)} km/s)`
    };
  }

  /**
   * Calculate interplanetary direct transfer trajectory from Mars to 2:3 resonant Plutino dwarf planet candidate 28978 Ixion and orbit capture.
   * a = ( r_mars + r_ixion ) / 2
   * e = ( r_ixion - r_mars ) / ( r_ixion + r_mars )
   * Reference: Marchi et al. (2003), Lellouch et al. (2013), Curtis (2013) for Plutino Exploration.
   * @param {number} [marsParkingAltitudeKm=300.0] - Mars parking orbit altitude in km (150 to 1000 km)
   * @param {number} [ixionDistanceAU=39.68] - Ixion heliocentric distance in AU (30.0 to 50.0 AU)
   * @param {number} [ixionPeriapsisAltitudeKm=200.0] - Ixion orbit insertion periapsis altitude in km (50 to 5000 km)
   * @returns {{semiMajorAxisAU: number, eccentricity: number, timeOfFlightDays: number, timeOfFlightYears: number, marsDepartureDeltaVKmS: number, ixionOrbitInsertionDeltaVKmS: number, totalMissionDeltaVKmS: number, ixionContext: string}}
   */
  static computeMarsToIxionTransfer(marsParkingAltitudeKm = 300.0, ixionDistanceAU = 39.68, ixionPeriapsisAltitudeKm = 200.0) {
    const hpMarsKm = Math.max(150.0, marsParkingAltitudeKm);
    const rIAU = Math.max(25.0, Math.min(55.0, ixionDistanceAU));
    const hpIKm = Math.max(30.0, ixionPeriapsisAltitudeKm);

    const AU_KM = 1.495978707e8;
    const muSun = 1.32712440018e11;
    const muMars = 42828.37;
    const rMarsKm = 3389.5;
    const muIxi = 20.0; // km^3/s^2
    const rIxiKm = 350.0; // km
    const rMarsAU = 1.52368;

    const rMarsDistKm = rMarsAU * AU_KM;
    const rIDistKm = rIAU * AU_KM;

    const aKm = (rMarsDistKm + rIDistKm) / 2.0;
    const aAU = aKm / AU_KM;
    const ecc = (rIDistKm - rMarsDistKm) / (rIDistKm + rMarsDistKm);

    // Time of Flight (s -> days -> yr)
    const tofSec = Math.PI * Math.sqrt(Math.pow(aKm, 3.0) / muSun);
    const tofDays = tofSec / 86400.0;
    const tofYrs = tofDays / 365.25;

    // Mars departure
    const vMarsCircKmS = Math.sqrt(muSun / rMarsDistKm);
    const vDepKmS = Math.sqrt(muSun * ((2.0 / rMarsDistKm) - (1.0 / aKm)));
    const vInfMarsKmS = Math.abs(vDepKmS - vMarsCircKmS);

    const rParkMarsKm = rMarsKm + hpMarsKm;
    const vParkMarsKmS = Math.sqrt(muMars / rParkMarsKm);
    const vHypMarsKmS = Math.sqrt(Math.pow(vInfMarsKmS, 2.0) + ((2.0 * muMars) / rParkMarsKm));
    const dvTiiKmS = vHypMarsKmS - vParkMarsKmS;

    // Ixion capture
    const vICircKmS = Math.sqrt(muSun / rIDistKm);
    const vArrKmS = Math.sqrt(muSun * ((2.0 / rIDistKm) - (1.0 / aKm)));
    const vInfIKmS = Math.abs(vICircKmS - vArrKmS);

    const rpIKm = rIxiKm + hpIKm;
    const eCap = 0.85; // Capture orbit
    const aCapKm = rpIKm / (1.0 - eCap);

    const vHypIKmS = Math.sqrt(Math.pow(vInfIKmS, 2.0) + ((2.0 * muIxi) / rpIKm));
    const vCapIKmS = Math.sqrt(muIxi * ((2.0 / rpIKm) - (1.0 / aCapKm)));
    const dvIoiKmS = vHypIKmS - vCapIKmS;

    const dvTotKmS = dvTiiKmS + dvIoiKmS;

    return {
      semiMajorAxisAU: parseFloat(aAU.toFixed(3)),
      eccentricity: parseFloat(ecc.toFixed(4)),
      timeOfFlightDays: parseFloat(tofDays.toFixed(1)),
      timeOfFlightYears: parseFloat(tofYrs.toFixed(2)),
      marsDepartureDeltaVKmS: parseFloat(dvTiiKmS.toFixed(3)),
      ixionOrbitInsertionDeltaVKmS: parseFloat(dvIoiKmS.toFixed(3)),
      totalMissionDeltaVKmS: parseFloat(dvTotKmS.toFixed(3)),
      ixionContext: `Mars-to-Ixion (${tofYrs.toFixed(1)} yr TOF, e=${ecc.toFixed(4)}, Total Delta-V=${dvTotKmS.toFixed(2)} km/s, IOI=${dvIoiKmS.toFixed(2)} km/s)`
    };
  }

  /**
   * Calculate interplanetary direct transfer trajectory from Mars to large classical Kuiper Belt dwarf planet candidate 120347 Salacia and orbit capture.
   * a = ( r_mars + r_salacia ) / 2
   * e = ( r_salacia - r_mars ) / ( r_salacia + r_mars )
   * Reference: Stansberry et al. (2012), Fornasier et al. (2013), Curtis (2013) for Classical KBO Exploration.
   * @param {number} [marsParkingAltitudeKm=300.0] - Mars parking orbit altitude in km (150 to 1000 km)
   * @param {number} [salaciaDistanceAU=44.80] - Salacia heliocentric distance in AU (35.0 to 55.0 AU)
   * @param {number} [salaciaPeriapsisAltitudeKm=250.0] - Salacia orbit insertion periapsis altitude in km (50 to 5000 km)
   * @returns {{semiMajorAxisAU: number, eccentricity: number, timeOfFlightDays: number, timeOfFlightYears: number, marsDepartureDeltaVKmS: number, salaciaOrbitInsertionDeltaVKmS: number, totalMissionDeltaVKmS: number, salaciaContext: string}}
   */
  static computeMarsToSalaciaTransfer(marsParkingAltitudeKm = 300.0, salaciaDistanceAU = 44.80, salaciaPeriapsisAltitudeKm = 250.0) {
    const hpMarsKm = Math.max(150.0, marsParkingAltitudeKm);
    const rSAU = Math.max(30.0, Math.min(60.0, salaciaDistanceAU));
    const hpSKm = Math.max(30.0, salaciaPeriapsisAltitudeKm);

    const AU_KM = 1.495978707e8;
    const muSun = 1.32712440018e11;
    const muMars = 42828.37;
    const rMarsKm = 3389.5;
    const muSal = 31.6; // km^3/s^2
    const rSalKm = 423.0; // km
    const rMarsAU = 1.52368;

    const rMarsDistKm = rMarsAU * AU_KM;
    const rSDistKm = rSAU * AU_KM;

    const aKm = (rMarsDistKm + rSDistKm) / 2.0;
    const aAU = aKm / AU_KM;
    const ecc = (rSDistKm - rMarsDistKm) / (rSDistKm + rMarsDistKm);

    // Time of Flight (s -> days -> yr)
    const tofSec = Math.PI * Math.sqrt(Math.pow(aKm, 3.0) / muSun);
    const tofDays = tofSec / 86400.0;
    const tofYrs = tofDays / 365.25;

    // Mars departure
    const vMarsCircKmS = Math.sqrt(muSun / rMarsDistKm);
    const vDepKmS = Math.sqrt(muSun * ((2.0 / rMarsDistKm) - (1.0 / aKm)));
    const vInfMarsKmS = Math.abs(vDepKmS - vMarsCircKmS);

    const rParkMarsKm = rMarsKm + hpMarsKm;
    const vParkMarsKmS = Math.sqrt(muMars / rParkMarsKm);
    const vHypMarsKmS = Math.sqrt(Math.pow(vInfMarsKmS, 2.0) + ((2.0 * muMars) / rParkMarsKm));
    const dvTsiKmS = vHypMarsKmS - vParkMarsKmS;

    // Salacia capture
    const vSCircKmS = Math.sqrt(muSun / rSDistKm);
    const vArrKmS = Math.sqrt(muSun * ((2.0 / rSDistKm) - (1.0 / aKm)));
    const vInfSKmS = Math.abs(vSCircKmS - vArrKmS);

    const rpSKm = rSalKm + hpSKm;
    const eCap = 0.85; // Capture orbit
    const aCapKm = rpSKm / (1.0 - eCap);

    const vHypSKmS = Math.sqrt(Math.pow(vInfSKmS, 2.0) + ((2.0 * muSal) / rpSKm));
    const vCapSKmS = Math.sqrt(muSal * ((2.0 / rpSKm) - (1.0 / aCapKm)));
    const dvSoiKmS = vHypSKmS - vCapSKmS;

    const dvTotKmS = dvTsiKmS + dvSoiKmS;

    return {
      semiMajorAxisAU: parseFloat(aAU.toFixed(3)),
      eccentricity: parseFloat(ecc.toFixed(4)),
      timeOfFlightDays: parseFloat(tofDays.toFixed(1)),
      timeOfFlightYears: parseFloat(tofYrs.toFixed(2)),
      marsDepartureDeltaVKmS: parseFloat(dvTsiKmS.toFixed(3)),
      salaciaOrbitInsertionDeltaVKmS: parseFloat(dvSoiKmS.toFixed(3)),
      totalMissionDeltaVKmS: parseFloat(dvTotKmS.toFixed(3)),
      salaciaContext: `Mars-to-Salacia (${tofYrs.toFixed(1)} yr TOF, e=${ecc.toFixed(4)}, Total Delta-V=${dvTotKmS.toFixed(2)} km/s, SOI=${dvSoiKmS.toFixed(2)} km/s)`
    };
  }

  /**
   * Calculate interplanetary direct transfer trajectory from Mars to large resonant binary Kuiper Belt object 174567 Varda and orbit capture.
   * a = ( r_mars + r_varda ) / 2
   * e = ( r_varda - r_mars ) / ( r_varda + r_mars )
   * Reference: Grundy et al. (2015), Souami et al. (2020), Curtis (2013) for Binary KBO Exploration.
   * @param {number} [marsParkingAltitudeKm=300.0] - Mars parking orbit altitude in km (150 to 1000 km)
   * @param {number} [vardaDistanceAU=45.60] - Varda heliocentric distance in AU (35.0 to 55.0 AU)
   * @param {number} [vardaPeriapsisAltitudeKm=200.0] - Varda orbit insertion periapsis altitude in km (50 to 5000 km)
   * @returns {{semiMajorAxisAU: number, eccentricity: number, timeOfFlightDays: number, timeOfFlightYears: number, marsDepartureDeltaVKmS: number, vardaOrbitInsertionDeltaVKmS: number, totalMissionDeltaVKmS: number, vardaContext: string}}
   */
  static computeMarsToVardaTransfer(marsParkingAltitudeKm = 300.0, vardaDistanceAU = 45.60, vardaPeriapsisAltitudeKm = 200.0) {
    const hpMarsKm = Math.max(150.0, marsParkingAltitudeKm);
    const rVAU = Math.max(30.0, Math.min(60.0, vardaDistanceAU));
    const hpVKm = Math.max(30.0, vardaPeriapsisAltitudeKm);

    const AU_KM = 1.495978707e8;
    const muSun = 1.32712440018e11;
    const muMars = 42828.37;
    const rMarsKm = 3389.5;
    const muVar = 17.7; // km^3/s^2
    const rVarKm = 370.0; // km
    const rMarsAU = 1.52368;

    const rMarsDistKm = rMarsAU * AU_KM;
    const rVDistKm = rVAU * AU_KM;

    const aKm = (rMarsDistKm + rVDistKm) / 2.0;
    const aAU = aKm / AU_KM;
    const ecc = (rVDistKm - rMarsDistKm) / (rVDistKm + rMarsDistKm);

    // Time of Flight (s -> days -> yr)
    const tofSec = Math.PI * Math.sqrt(Math.pow(aKm, 3.0) / muSun);
    const tofDays = tofSec / 86400.0;
    const tofYrs = tofDays / 365.25;

    // Mars departure
    const vMarsCircKmS = Math.sqrt(muSun / rMarsDistKm);
    const vDepKmS = Math.sqrt(muSun * ((2.0 / rMarsDistKm) - (1.0 / aKm)));
    const vInfMarsKmS = Math.abs(vDepKmS - vMarsCircKmS);

    const rParkMarsKm = rMarsKm + hpMarsKm;
    const vParkMarsKmS = Math.sqrt(muMars / rParkMarsKm);
    const vHypMarsKmS = Math.sqrt(Math.pow(vInfMarsKmS, 2.0) + ((2.0 * muMars) / rParkMarsKm));
    const dvTviKmS = vHypMarsKmS - vParkMarsKmS;

    // Varda capture
    const vVCircKmS = Math.sqrt(muSun / rVDistKm);
    const vArrKmS = Math.sqrt(muSun * ((2.0 / rVDistKm) - (1.0 / aKm)));
    const vInfVKmS = Math.abs(vVCircKmS - vArrKmS);

    const rpVKm = rVarKm + hpVKm;
    const eCap = 0.85; // Capture orbit
    const aCapKm = rpVKm / (1.0 - eCap);

    const vHypVKmS = Math.sqrt(Math.pow(vInfVKmS, 2.0) + ((2.0 * muVar) / rpVKm));
    const vCapVKmS = Math.sqrt(muVar * ((2.0 / rpVKm) - (1.0 / aCapKm)));
    const dvVoiKmS = vHypVKmS - vCapVKmS;

    const dvTotKmS = dvTviKmS + dvVoiKmS;

    return {
      semiMajorAxisAU: parseFloat(aAU.toFixed(3)),
      eccentricity: parseFloat(ecc.toFixed(4)),
      timeOfFlightDays: parseFloat(tofDays.toFixed(1)),
      timeOfFlightYears: parseFloat(tofYrs.toFixed(2)),
      marsDepartureDeltaVKmS: parseFloat(dvTviKmS.toFixed(3)),
      vardaOrbitInsertionDeltaVKmS: parseFloat(dvVoiKmS.toFixed(3)),
      totalMissionDeltaVKmS: parseFloat(dvTotKmS.toFixed(3)),
      vardaContext: `Mars-to-Varda (${tofYrs.toFixed(1)} yr TOF, e=${ecc.toFixed(4)}, Total Delta-V=${dvTotKmS.toFixed(2)} km/s, VOI=${dvVoiKmS.toFixed(2)} km/s)`
    };
  }

  /**
   * Calculate interplanetary direct transfer trajectory from Mars to resonant scattered disc dwarf planet candidate 229762 G!kún||ʼhòmdìmà and orbit capture.
   * a = ( r_mars + r_gkun ) / 2
   * e = ( r_gkun - r_mars ) / ( r_gkun + r_mars )
   * Reference: Grundy et al. (2019), Benedetti-Rossi et al. (2019), Curtis (2013) for Scattered Disc Exploration.
   * @param {number} [marsParkingAltitudeKm=300.0] - Mars parking orbit altitude in km (150 to 1000 km)
   * @param {number} [gkunDistanceAU=54.20] - G!kún||ʼhòmdìmà heliocentric distance in AU (40.0 to 70.0 AU)
   * @param {number} [gkunPeriapsisAltitudeKm=150.0] - G!kún orbit insertion periapsis altitude in km (50 to 5000 km)
   * @returns {{semiMajorAxisAU: number, eccentricity: number, timeOfFlightDays: number, timeOfFlightYears: number, marsDepartureDeltaVKmS: number, gkunOrbitInsertionDeltaVKmS: number, totalMissionDeltaVKmS: number, gkunContext: string}}
   */
  static computeMarsToGkunhomdimaTransfer(marsParkingAltitudeKm = 300.0, gkunDistanceAU = 54.20, gkunPeriapsisAltitudeKm = 150.0) {
    const hpMarsKm = Math.max(150.0, marsParkingAltitudeKm);
    const rGAU = Math.max(35.0, Math.min(80.0, gkunDistanceAU));
    const hpGKm = Math.max(30.0, gkunPeriapsisAltitudeKm);

    const AU_KM = 1.495978707e8;
    const muSun = 1.32712440018e11;
    const muMars = 42828.37;
    const rMarsKm = 3389.5;
    const muGkun = 9.07; // km^3/s^2
    const rGkunKm = 300.0; // km
    const rMarsAU = 1.52368;

    const rMarsDistKm = rMarsAU * AU_KM;
    const rGDistKm = rGAU * AU_KM;

    const aKm = (rMarsDistKm + rGDistKm) / 2.0;
    const aAU = aKm / AU_KM;
    const ecc = (rGDistKm - rMarsDistKm) / (rGDistKm + rMarsDistKm);

    // Time of Flight (s -> days -> yr)
    const tofSec = Math.PI * Math.sqrt(Math.pow(aKm, 3.0) / muSun);
    const tofDays = tofSec / 86400.0;
    const tofYrs = tofDays / 365.25;

    // Mars departure
    const vMarsCircKmS = Math.sqrt(muSun / rMarsDistKm);
    const vDepKmS = Math.sqrt(muSun * ((2.0 / rMarsDistKm) - (1.0 / aKm)));
    const vInfMarsKmS = Math.abs(vDepKmS - vMarsCircKmS);

    const rParkMarsKm = rMarsKm + hpMarsKm;
    const vParkMarsKmS = Math.sqrt(muMars / rParkMarsKm);
    const vHypMarsKmS = Math.sqrt(Math.pow(vInfMarsKmS, 2.0) + ((2.0 * muMars) / rParkMarsKm));
    const dvTgiKmS = vHypMarsKmS - vParkMarsKmS;

    // G!kún capture
    const vGCircKmS = Math.sqrt(muSun / rGDistKm);
    const vArrKmS = Math.sqrt(muSun * ((2.0 / rGDistKm) - (1.0 / aKm)));
    const vInfGKmS = Math.abs(vGCircKmS - vArrKmS);

    const rpGKm = rGkunKm + hpGKm;
    const eCap = 0.85; // Capture orbit
    const aCapKm = rpGKm / (1.0 - eCap);

    const vHypGKmS = Math.sqrt(Math.pow(vInfGKmS, 2.0) + ((2.0 * muGkun) / rpGKm));
    const vCapGKmS = Math.sqrt(muGkun * ((2.0 / rpGKm) - (1.0 / aCapKm)));
    const dvGoiKmS = vHypGKmS - vCapGKmS;

    const dvTotKmS = dvTgiKmS + dvGoiKmS;

    return {
      semiMajorAxisAU: parseFloat(aAU.toFixed(3)),
      eccentricity: parseFloat(ecc.toFixed(4)),
      timeOfFlightDays: parseFloat(tofDays.toFixed(1)),
      timeOfFlightYears: parseFloat(tofYrs.toFixed(2)),
      marsDepartureDeltaVKmS: parseFloat(dvTgiKmS.toFixed(3)),
      gkunOrbitInsertionDeltaVKmS: parseFloat(dvGoiKmS.toFixed(3)),
      totalMissionDeltaVKmS: parseFloat(dvTotKmS.toFixed(3)),
      gkunContext: `Mars-to-G!kún||ʼhòmdìmà (${tofYrs.toFixed(1)} yr TOF, e=${ecc.toFixed(4)}, Total Delta-V=${dvTotKmS.toFixed(2)} km/s, GOI=${dvGoiKmS.toFixed(2)} km/s)`
    };
  }

  /**
   * Calculate interplanetary direct transfer trajectory from Mars to classical Kuiper Belt object 19521 Chaos and orbit capture.
   * a = ( r_mars + r_chaos ) / 2
   * e = ( r_chaos - r_mars ) / ( r_chaos + r_mars )
   * Reference: Brown et al. (2004), Stansberry et al. (2008), Curtis (2013) for Classical KBO Exploration.
   * @param {number} [marsParkingAltitudeKm=300.0] - Mars parking orbit altitude in km (150 to 1000 km)
   * @param {number} [chaosDistanceAU=40.90] - Chaos heliocentric distance in AU (35.0 to 55.0 AU)
   * @param {number} [chaosPeriapsisAltitudeKm=150.0] - Chaos orbit insertion periapsis altitude in km (50 to 5000 km)
   * @returns {{semiMajorAxisAU: number, eccentricity: number, timeOfFlightDays: number, timeOfFlightYears: number, marsDepartureDeltaVKmS: number, chaosOrbitInsertionDeltaVKmS: number, totalMissionDeltaVKmS: number, chaosContext: string}}
   */
  static computeMarsToChaosTransfer(marsParkingAltitudeKm = 300.0, chaosDistanceAU = 40.90, chaosPeriapsisAltitudeKm = 150.0) {
    const hpMarsKm = Math.max(150.0, marsParkingAltitudeKm);
    const rCAU = Math.max(30.0, Math.min(60.0, chaosDistanceAU));
    const hpCKm = Math.max(30.0, chaosPeriapsisAltitudeKm);

    const AU_KM = 1.495978707e8;
    const muSun = 1.32712440018e11;
    const muMars = 42828.37;
    const rMarsKm = 3389.5;
    const muChaos = 15.0; // km^3/s^2
    const rChaosKm = 300.0; // km
    const rMarsAU = 1.52368;

    const rMarsDistKm = rMarsAU * AU_KM;
    const rCDistKm = rCAU * AU_KM;

    const aKm = (rMarsDistKm + rCDistKm) / 2.0;
    const aAU = aKm / AU_KM;
    const ecc = (rCDistKm - rMarsDistKm) / (rCDistKm + rMarsDistKm);

    // Time of Flight (s -> days -> yr)
    const tofSec = Math.PI * Math.sqrt(Math.pow(aKm, 3.0) / muSun);
    const tofDays = tofSec / 86400.0;
    const tofYrs = tofDays / 365.25;

    // Mars departure
    const vMarsCircKmS = Math.sqrt(muSun / rMarsDistKm);
    const vDepKmS = Math.sqrt(muSun * ((2.0 / rMarsDistKm) - (1.0 / aKm)));
    const vInfMarsKmS = Math.abs(vDepKmS - vMarsCircKmS);

    const rParkMarsKm = rMarsKm + hpMarsKm;
    const vParkMarsKmS = Math.sqrt(muMars / rParkMarsKm);
    const vHypMarsKmS = Math.sqrt(Math.pow(vInfMarsKmS, 2.0) + ((2.0 * muMars) / rParkMarsKm));
    const dvTciKmS = vHypMarsKmS - vParkMarsKmS;

    // Chaos capture
    const vCCircKmS = Math.sqrt(muSun / rCDistKm);
    const vArrKmS = Math.sqrt(muSun * ((2.0 / rCDistKm) - (1.0 / aKm)));
    const vInfCKmS = Math.abs(vCCircKmS - vArrKmS);

    const rpCKm = rChaosKm + hpCKm;
    const eCap = 0.85; // Capture orbit
    const aCapKm = rpCKm / (1.0 - eCap);

    const vHypCKmS = Math.sqrt(Math.pow(vInfCKmS, 2.0) + ((2.0 * muChaos) / rpCKm));
    const vCapCKmS = Math.sqrt(muChaos * ((2.0 / rpCKm) - (1.0 / aCapKm)));
    const dvCoiKmS = vHypCKmS - vCapCKmS;

    const dvTotKmS = dvTciKmS + dvCoiKmS;

    return {
      semiMajorAxisAU: parseFloat(aAU.toFixed(3)),
      eccentricity: parseFloat(ecc.toFixed(4)),
      timeOfFlightDays: parseFloat(tofDays.toFixed(1)),
      timeOfFlightYears: parseFloat(tofYrs.toFixed(2)),
      marsDepartureDeltaVKmS: parseFloat(dvTciKmS.toFixed(3)),
      chaosOrbitInsertionDeltaVKmS: parseFloat(dvCoiKmS.toFixed(3)),
      totalMissionDeltaVKmS: parseFloat(dvTotKmS.toFixed(3)),
      chaosContext: `Mars-to-Chaos (${tofYrs.toFixed(1)} yr TOF, e=${ecc.toFixed(4)}, Total Delta-V=${dvTotKmS.toFixed(2)} km/s, COI=${dvCoiKmS.toFixed(2)} km/s)`
    };
  }
}




