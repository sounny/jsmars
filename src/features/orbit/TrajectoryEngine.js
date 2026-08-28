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
}




