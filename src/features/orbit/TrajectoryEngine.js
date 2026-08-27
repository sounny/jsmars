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
}




