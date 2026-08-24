import { MarsTime } from '../slider/MarsTime.js';

/**
 * @module KRCEngine
 * @description Mars 1D Thermal Model simulation engine inspired by Hugh Kieffer's KRC.
 *
 * Solves the 1D transient heat conduction equation in the Martian subsurface
 * coupled with surface radiative energy balance, solar insolation, and atmospheric downwelling flux.
 */
export class KRCEngine {
  static STEFAN_BOLTZMANN = 5.670374419e-8; // W / (m^2 K^4)
  static MARS_EMISSIVITY = 0.95;
  static SPECIFIC_HEAT = 800.0; // J / (kg K)
  static DENSITY = 1500.0; // kg / m^3
  static MARS_SOL_SECONDS = 88775.244; // seconds in 1 Martian sol
  static CO2_FROST_POINT = 145.0; // Kelvin

  /**
   * Run a full diurnal simulation for given Mars location and atmospheric/surface properties.
   * @param {object} params - Model parameters
   * @param {number} [params.lat=0] - Latitude in degrees (-90 to +90)
   * @param {number} [params.elevation=0] - Elevation in meters (MOLA datum)
   * @param {number} [params.Ls=0] - Solar Longitude in degrees (0 to 360)
   * @param {number} [params.thermalInertia=250] - Thermal inertia in J m^-2 K^-1 s^-1/2 (SI units)
   * @param {number} [params.albedo=0.25] - Bolometric surface albedo (0.05 to 0.40)
   * @param {number} [params.tau=0.3] - Dust optical depth (0.1 to 3.0)
   * @param {number} [params.numLayers=20] - Number of subsurface layers
   * @param {number} [params.maxDepth=1.0] - Depth of simulation in meters
   * @returns {object} Simulation results including diurnal curve, depth profiles, and summary metrics.
   */
  static simulateDiurnal(params = {}) {
    const lat = params.lat ?? 0;
    const Ls = params.Ls ?? 0;
    const TI = Math.max(20, params.thermalInertia ?? 250);
    const albedo = Math.max(0.01, Math.min(0.99, params.albedo ?? 0.25));
    const tau = Math.max(0.05, params.tau ?? 0.3);
    const elevation = params.elevation ?? 0;
    const numLayers = params.numLayers ?? 24;
    const maxDepth = params.maxDepth ?? 1.2;

    // Derived physical properties
    const C_vol = this.DENSITY * this.SPECIFIC_HEAT; // Volumetric heat capacity ~ 1.2e6 J/(m^3 K)
    const k_cond = (TI * TI) / C_vol; // Thermal conductivity (W/(m K))
    const diffusivity = k_cond / C_vol; // Thermal diffusivity (m^2/s)
    const skinDepth = TI * Math.sqrt(this.MARS_SOL_SECONDS / Math.PI) / C_vol;

    // Compute orbital distance & insolation
    const e = MarsTime.ECCENTRICITY;
    const r_AU = (MarsTime.SEMI_MAJOR_AXIS * (1 - e * e)) / (1 + e * Math.cos(Ls * Math.PI / 180));
    const S_solar = MarsTime.SOLAR_CONSTANT_1AU / (r_AU * r_AU);

    // Atmospheric downwelling and surface pressure factor
    // Surface pressure approx 610 Pa at 0km datum, scale height ~ 11.1 km
    const surfacePressure = 610 * Math.exp(-elevation / 11100);
    const atmEmissionFactor = 0.12 + 0.20 * (1 - Math.exp(-tau)) + 0.05 * Math.min(1.0, surfacePressure / 610);

    // Discretize depth grid (geometric progression focused near surface)
    const z = new Float64Array(numLayers);
    const dz = new Float64Array(numLayers);
    let currentZ = 0;
    let baseDz = 0.003; // 3 mm first layer
    const growth = 1.25;

    for (let i = 0; i < numLayers; i++) {
      dz[i] = baseDz * Math.pow(growth, i);
      currentZ += dz[i];
      z[i] = currentZ;
    }

    // Time discretization (120 time steps per sol)
    const stepsPerSol = 120;
    const dt = this.MARS_SOL_SECONDS / stepsPerSol;

    // Initial temperature estimate
    // Simple radiative equilibrium guess: T_eq = ( (1-A) * S_solar * cos(lat) / (pi * sigma * eps) )^0.25
    const latRad = Math.abs(lat) * Math.PI / 180;
    const avgInsol = Math.max(10, (1 - albedo) * S_solar * Math.cos(latRad) / Math.PI);
    let T_mean = Math.pow(avgInsol / (this.STEFAN_BOLTZMANN * this.MARS_EMISSIVITY), 0.25);
    if (!Number.isFinite(T_mean) || T_mean < 100) T_mean = 210;

    // Temperature array across layers
    let T = new Float64Array(numLayers).fill(T_mean);
    let T_surf = T_mean;

    // Spin-up solver for 6 sols to reach dynamic thermal equilibrium
    const totalSols = 6;
    const diurnalCurve = [];

    for (let sol = 0; sol < totalSols; sol++) {
      for (let step = 0; step < stepsPerSol; step++) {
        const localHour = (step / stepsPerSol) * 24.0;
        const { cosZ, isDay } = MarsTime.getSolarZenith(lat, Ls, localHour);

        // Surface direct + diffuse solar flux
        let solarFlux = 0;
        if (isDay && cosZ > 0) {
          const airMass = 1.0 / Math.max(0.05, cosZ);
          const directFlux = S_solar * cosZ * Math.exp(-tau * airMass);
          const diffuseFlux = S_solar * cosZ * (1 - Math.exp(-tau * airMass)) * 0.4;
          solarFlux = (1 - albedo) * (directFlux + diffuseFlux);
        }

        // Atmospheric downward IR flux
        const downwellingIR = atmEmissionFactor * this.STEFAN_BOLTZMANN * Math.pow(T_mean, 4);

        // Solve surface boundary condition:
        // solarFlux + downwellingIR - eps*sigma*T_surf^4 + k * (T[0] - T_surf) / dz[0] = 0
        // Use Newton-Raphson iteration for T_surf
        const conductCoeff = k_cond / dz[0];
        let T_s = T_surf;
        for (let iter = 0; iter < 8; iter++) {
          const radLoss = this.MARS_EMISSIVITY * this.STEFAN_BOLTZMANN * Math.pow(T_s, 4);
          const f = solarFlux + downwellingIR - radLoss + conductCoeff * (T[0] - T_s);
          const df = -4 * this.MARS_EMISSIVITY * this.STEFAN_BOLTZMANN * Math.pow(T_s, 3) - conductCoeff;
          const delta = f / df;
          T_s -= delta;
          if (Math.abs(delta) < 1e-4) break;
        }
        
        // CO2 condensation check: surface temperature cannot drop below CO2 frost point
        let isCO2Frost = false;
        if (T_s < this.CO2_FROST_POINT) {
          T_s = this.CO2_FROST_POINT;
          isCO2Frost = true;
        }
        T_surf = T_s;

        // Subsurface diffusion step (explicit finite difference with stability cap)
        const nextT = new Float64Array(numLayers);
        for (let i = 0; i < numLayers; i++) {
          const T_prev = (i === 0) ? T_surf : T[i - 1];
          const T_curr = T[i];
          const T_next = (i === numLayers - 1) ? T[i] : T[i + 1];

          const dz_prev = (i === 0) ? dz[0] : (dz[i - 1] + dz[i]) * 0.5;
          const dz_next = (i === numLayers - 1) ? dz[i] : (dz[i] + dz[i + 1]) * 0.5;

          const flux_in = k_cond * (T_prev - T_curr) / dz_prev;
          const flux_out = k_cond * (T_curr - T_next) / dz_next;

          const dTemp = (flux_in - flux_out) * dt / (C_vol * dz[i]);
          nextT[i] = T_curr + Math.max(-10, Math.min(10, dTemp));
        }
        T = nextT;

        // Record the last sol
        if (sol === totalSols - 1) {
          diurnalCurve.push({
            hour: parseFloat(localHour.toFixed(2)),
            surfaceTemp: parseFloat(T_surf.toFixed(2)),
            subsurface10cm: parseFloat(T[3]?.toFixed(2) ?? T_surf.toFixed(2)),
            subsurface50cm: parseFloat(T[10]?.toFixed(2) ?? T_surf.toFixed(2)),
            solarFlux: parseFloat(solarFlux.toFixed(1)),
            isCO2Frost
          });
        }
      }
    }

    // Compute summary metrics
    const temps = diurnalCurve.map(d => d.surfaceTemp);
    const minTemp = Math.min(...temps);
    const maxTemp = Math.max(...temps);
    const meanTemp = temps.reduce((a, b) => a + b, 0) / temps.length;
    const diurnalRange = maxTemp - minTemp;

    // Construct depth profile snapshot at local noon and midnight
    const noonIdx = Math.floor(stepsPerSol * 0.5);
    const midnightIdx = 0;

    const depthProfile = Array.from(z).map((depthMeters, i) => ({
      depthCm: parseFloat((depthMeters * 100).toFixed(1)),
      temp: parseFloat(T[i].toFixed(2))
    }));

    return {
      params: {
        lat,
        Ls,
        thermalInertia: TI,
        albedo,
        tau,
        elevation,
        r_AU
      },
      summary: {
        minTemp: parseFloat(minTemp.toFixed(1)),
        maxTemp: parseFloat(maxTemp.toFixed(1)),
        meanTemp: parseFloat(meanTemp.toFixed(1)),
        diurnalRange: parseFloat(diurnalRange.toFixed(1)),
        skinDepthCm: parseFloat((skinDepth * 100).toFixed(1)),
        co2FrostOccurs: temps.some(t => t <= this.CO2_FROST_POINT + 0.5)
      },
      diurnalCurve,
      depthProfile
    };
  }

  /**
   * Simulate seasonal temperature variations across full Mars orbit (Ls = 0 to 360 deg).
   * @param {object} params - Model parameters
   * @returns {Array<object>} Seasonal curve points.
   */
  static simulateSeasonal(params = {}) {
    const points = [];
    for (let Ls = 0; Ls <= 360; Ls += 15) {
      const res = this.simulateDiurnal({ ...params, Ls: Ls % 360 });
      points.push({
        Ls,
        minTemp: res.summary.minTemp,
        maxTemp: res.summary.maxTemp,
        meanTemp: res.summary.meanTemp
      });
    }
    return points;
  }

  // --- Regolith Thermodynamics & Thermal Inertia ---

  /**
   * Calculate thermal skin depth for diurnal or annual orbital periods.
   * @param {number} thermalInertia - Thermal inertia (J m^-2 K^-1 s^-1/2)
   * @param {number} [periodSeconds=88775.244] - Period in seconds (defaults to 1 Sol)
   * @returns {{skinDepthMeters: number, skinDepthCm: number}}
   */
  static computeSkinDepth(thermalInertia, periodSeconds = 88775.244) {
    const C_vol = KRCEngine.DENSITY * KRCEngine.SPECIFIC_HEAT;
    const deltaMeters = (thermalInertia * Math.sqrt(periodSeconds / Math.PI)) / C_vol;

    return {
      skinDepthMeters: deltaMeters,
      skinDepthCm: deltaMeters * 100.0
    };
  }

  /**
   * Estimate Apparent Thermal Inertia (ATI) from diurnal temperature amplitude.
   * @param {number} deltaT - Diurnal temperature range (T_max - T_min in K)
   * @param {number} [albedo=0.25] - Bolometric albedo
   * @param {number} [solarInsolation=588.6] - Solar insolation in W/m^2
   * @returns {number} Apparent Thermal Inertia (ATI) in SI units
   */
  static computeApparentThermalInertia(deltaT, albedo = 0.25, solarInsolation = 588.6) {
    const omega = (2 * Math.PI) / KRCEngine.MARS_SOL_SECONDS;
    const safeDeltaT = Math.max(1.0, deltaT);
    const absorbedFlux = (1.0 - albedo) * solarInsolation;

    return (absorbedFlux / safeDeltaT) / Math.sqrt(omega);
  }

  /**
   * Calculate regolith bulk thermal conductivity from thermal inertia.
   * @param {number} thermalInertia - Thermal inertia (SI)
   * @param {number} [density=1500] - Density in kg/m^3
   * @param {number} [specificHeat=800] - Specific heat in J/(kg K)
   * @returns {number} Thermal conductivity in W / (m K)
   */
  static computeRegolithConductivity(thermalInertia, density = 1500, specificHeat = 800) {
    const cVol = density * specificHeat;
    return (thermalInertia * thermalInertia) / cVol;
  }
}

