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

  // --- Seasonal Thermal Penetration & Grain Size Classification ---

  /**
   * Calculate seasonal/annual thermal skin depth across Martian year (668.6 sols).
   * @param {number} thermalInertia - Thermal inertia in J m^-2 K^-1 s^-1/2
   * @param {number} [solsPerYear=668.6] - Number of sols in Mars year
   * @returns {{annualSkinDepthMeters: number, annualSkinDepthCm: number, diurnalSkinDepthMeters: number}}
   */
  static computeAnnualSkinDepth(thermalInertia, solsPerYear = 668.6) {
    const diurnal = this.computeSkinDepth(thermalInertia);
    const annualMeters = diurnal.skinDepthMeters * Math.sqrt(solsPerYear);

    return {
      annualSkinDepthMeters: parseFloat(annualMeters.toFixed(3)),
      annualSkinDepthCm: parseFloat((annualMeters * 100).toFixed(1)),
      diurnalSkinDepthMeters: parseFloat(diurnal.skinDepthMeters.toFixed(3))
    };
  }

  /**
   * Classify Martian regolith physical state and estimated grain size from thermal inertia (TI).
   * @param {number} thermalInertia - Thermal inertia in tiu
   * @returns {{classification: string, grainSizeEstimate: string, description: string}}
   */
  static classifyRegolithGrainSize(thermalInertia) {
    const ti = Math.max(1, thermalInertia);

    if (ti < 100) {
      return {
        classification: 'Fine Atmospheric Dust Mantle',
        grainSizeEstimate: '< 40 µm (Silt / Micron Dust)',
        description: 'Thick settling mantles of airfall dust (e.g. Tharsis, Arabia Terra)'
      };
    } else if (ti < 250) {
      return {
        classification: 'Fine to Medium Eolian Sand',
        grainSizeEstimate: '100 - 250 µm (Active Dunes)',
        description: 'Saltating sand sheets, ripples, and dune fields'
      };
    } else if (ti < 500) {
      return {
        classification: 'Coarse Sand & Indurated Duricrust',
        grainSizeEstimate: '300 µm - 2 mm + Cemented Clasts',
        description: 'Coarse sand drifts and salt-cemented regolith crusts'
      };
    } else if (ti < 1200) {
      return {
        classification: 'Rocky Regolith / Cobbles / Patchy Rock',
        grainSizeEstimate: '> 5 mm + Bedrock Clasts',
        description: 'Gravel pavements, rocky impact ejecta, and fragmented duricrust'
      };
    } else {
      return {
        classification: 'Massive Bedrock / Pure Water Ice',
        grainSizeEstimate: 'Continuous Solid Rock / Ice',
        description: 'Intact volcanic basalt lava flows or dense polar ice sheet'
      };
    }
  }

  /**
   * Compute emitted thermal infrared radiance using Stefan-Boltzmann law.
   * @param {number} temperatureK - Surface temperature in Kelvin
   * @param {number} [emissivity=0.95] - Surface broadband emissivity
   * @returns {number} Emitted thermal flux in W / m^2
   */
  static computeStefanBoltzmannFlux(temperatureK, emissivity = 0.95) {
    const eps = Math.max(0.01, Math.min(1.0, emissivity));
    const t = Math.max(0, temperatureK);
    const flux = eps * KRCEngine.STEFAN_BOLTZMANN * Math.pow(t, 4);
    return parseFloat(flux.toFixed(2));
  }

  // --- Atmospheric Pressure-Dependent Conduction & CO2 Sublimation ---

  /**
   * Calculate effective thermal conductivity accounting for Smoluchowski gas-pore conduction in Martian regolith.
   * K_eff(P) = K_solid + (K_gas0 * (P / P_ref)) / (1 + P / P_trans)
   * @param {number} kSolid - Solid particle contact conductivity (W/(m K))
   * @param {number} pressurePa - Ambient atmospheric pressure in Pascals
   * @param {number} [pTrans=120] - Transition Knudsen pressure in Pa
   * @param {number} [kGas0=0.015] - CO2 gas thermal conductivity at 1 bar reference
   * @returns {number} Effective bulk thermal conductivity in W / (m K)
   */
  static computePressureDependentConductivity(kSolid, pressurePa, pTrans = 120, kGas0 = 0.015) {
    const p = Math.max(0, pressurePa);
    const gasContribution = (kGas0 * (p / 610.0)) / (1.0 + p / pTrans);
    return parseFloat((kSolid + gasContribution).toFixed(5));
  }

  /**
   * Calculate CO2 frost/sublimation equilibrium temperature as a function of atmospheric pressure (Clausius-Clapeyron).
   * T_frost = -3148.0 / ln(P / 1.055e12)
   * @param {number} pressurePa - CO2 partial pressure in Pascals (e.g. 610 Pa datum)
   * @returns {number} CO2 frost condensation temperature in Kelvin (~148.0 K at 610 Pa)
   */
  static computeCO2CondensationTemperature(pressurePa) {
    const p = Math.max(0.01, pressurePa);
    const tFrost = -3148.0 / Math.log(p / 1.055e12);
    return parseFloat(tFrost.toFixed(2));
  }

  /**
   * Compute surface radiative cooling rate in Kelvin per hour.
   * @param {number} temperatureK - Current surface temperature
   * @param {number} [layerThicknessMeters=0.01] - Top layer thickness (1 cm)
   * @param {number} [emissivity=0.95] - Surface emissivity
   * @returns {number} Radiative cooling rate in K/hour
   */
  static computeRadiativeCoolingRate(temperatureK, layerThicknessMeters = 0.01, emissivity = 0.95) {
    const flux = this.computeStefanBoltzmannFlux(temperatureK, emissivity);
    const cVol = KRCEngine.DENSITY * KRCEngine.SPECIFIC_HEAT;
    const ratePerSec = flux / (cVol * layerThicknessMeters);
    return parseFloat((ratePerSec * 3600.0).toFixed(2));
  }

  // --- Latent Heat of Sublimation & Subsurface Thermal Wave Damping ---

  /**
   * Calculate CO2 dry ice sublimation or condensation mass flux from net surface energy imbalance.
   * dm/dt = F_net / L_subl
   * @param {number} netEnergyFluxW_M2 - Net absorbed minus emitted energy flux (W/m^2)
   * @param {number} [latentHeatSublimationJ_Kg=5.9e5] - Latent heat of CO2 sublimation (J/kg)
   * @param {number} [co2IceDensityKgM3=1600.0] - Dry ice density (kg/m^3)
   * @returns {{massRateKg_M2_S: number, thicknessRateMmPerSol: number, isSublimating: boolean}}
   */
  static computeCO2SublimationRate(netEnergyFluxW_M2, latentHeatSublimationJ_Kg = 5.9e5, co2IceDensityKgM3 = 1600.0) {
    const massRateKg_M2_S = netEnergyFluxW_M2 / latentHeatSublimationJ_Kg;
    const volumeRateM3_M2_S = massRateKg_M2_S / co2IceDensityKgM3;
    const thicknessRateMmPerSol = volumeRateM3_M2_S * this.MARS_SOL_SECONDS * 1000.0;

    return {
      massRateKg_M2_S: parseFloat(massRateKg_M2_S.toExponential(4)),
      thicknessRateMmPerSol: parseFloat(thicknessRateMmPerSol.toFixed(3)),
      isSublimating: netEnergyFluxW_M2 > 0
    };
  }

  /**
   * Calculate harmonic subsurface thermal wave exponential amplitude damping and phase lag at depth z.
   * A(z) = A_0 * exp(-z / delta),  phase(z) = z / delta
   * @param {number} depthMeters - Depth below surface in meters
   * @param {number} thermalInertia - Regolith thermal inertia (SI)
   * @param {number} [periodSeconds=88775.244] - Thermal period (1 Sol)
   * @returns {{amplitudeRatio: number, phaseLagRadians: number, phaseLagHours: number}}
   */
  static computeThermalDampingDepth(depthMeters, thermalInertia, periodSeconds = 88775.244) {
    const skin = this.computeSkinDepth(thermalInertia, periodSeconds);
    const delta = Math.max(1e-4, skin.skinDepthMeters);
    const z = Math.max(0, depthMeters);

    const ampRatio = Math.exp(-z / delta);
    const phaseRad = z / delta;
    const periodHours = periodSeconds / 3600.0;
    const phaseLagHours = (phaseRad / (2.0 * Math.PI)) * periodHours;

    return {
      amplitudeRatio: parseFloat(ampRatio.toFixed(4)),
      phaseLagRadians: parseFloat(phaseRad.toFixed(3)),
      phaseLagHours: parseFloat(phaseLagHours.toFixed(2))
    };
  }

  /**
   * Compute cumulative thermal capacitance and integrated heat capacity across a multi-layer stratigraphy.
   * @param {Array<number>} layerThicknessesMeters - Array of thickness for each layer (m)
   * @param {Array<number>} [layerDensities=[]] - Optional density per layer (kg/m^3)
   * @returns {{totalThicknessMeters: number, totalHeatCapacityJ_M2_K: number}}
   */
  static computeSubsurfaceHeatCapacityLayered(layerThicknessesMeters = [], layerDensities = []) {
    let totalZ = 0;
    let totalCap = 0;

    layerThicknessesMeters.forEach((dz, i) => {
      const rho = layerDensities[i] || KRCEngine.DENSITY;
      const cVol = rho * KRCEngine.SPECIFIC_HEAT;
      totalZ += dz;
      totalCap += cVol * dz;
    });

    return {
      totalThicknessMeters: parseFloat(totalZ.toFixed(3)),
      totalHeatCapacityJ_M2_K: parseFloat(totalCap.toFixed(1))
    };
  }

  // --- Downwelling IR, Skin Depth Amplification & Radiative Equilibrium Solvers ---

  /**
   * Calculate atmospheric downward thermal infrared flux at Martian surface.
   * F_IR = eps_atm * sigma * T_air^4
   * @param {number} [airTempK=210.0] - Near-surface atmospheric air temperature in Kelvin
   * @param {number} [dustTau=0.3] - Dust optical depth
   * @param {number} [surfacePressurePa=610.0] - Surface atmospheric pressure in Pa
   * @returns {{downwellingFluxW_M2: number, atmosphericEmissivity: number}}
   */
  static computeAtmosphericDownwellingIR(airTempK = 210.0, dustTau = 0.3, surfacePressurePa = 610.0) {
    const tau = Math.max(0.01, dustTau);
    const p = Math.max(0, surfacePressurePa);
    const epsAtm = 0.12 + 0.20 * (1.0 - Math.exp(-tau)) + 0.05 * Math.min(1.0, p / 610.0);
    const flux = epsAtm * this.STEFAN_BOLTZMANN * Math.pow(Math.max(1, airTempK), 4);

    return {
      downwellingFluxW_M2: parseFloat(flux.toFixed(2)),
      atmosphericEmissivity: parseFloat(epsAtm.toFixed(4))
    };
  }

  /**
   * Calculate exact thermal skin depth amplification ratio between annual and diurnal cycles.
   * ratio = sqrt(solsPerYear)
   * @param {number} [solsPerYear=668.6] - Number of sols in Mars year
   * @returns {number} Ratio of annual to diurnal skin depth
   */
  static computeSkinDepthRatio(solsPerYear = 668.6) {
    const ratio = Math.sqrt(Math.max(1, solsPerYear));
    return parseFloat(ratio.toFixed(3));
  }

  /**
   * Calculate steady-state radiative equilibrium surface temperature.
   * T_eq = [ ((1 - A) * F_sun + F_IR) / (eps * sigma) ]^(1/4)
   * @param {number} solarFluxW_M2 - Top-of-atmosphere/surface solar flux
   * @param {number} [albedo=0.25] - Bolometric surface albedo
   * @param {number} [downwellingIrW_M2=25.0] - Downward atmospheric IR flux
   * @param {number} [emissivity=0.95] - Surface infrared emissivity
   * @returns {number} Equilibrium surface temperature in Kelvin
   */
  static computeEquilibriumSurfaceTemperature(solarFluxW_M2, albedo = 0.25, downwellingIrW_M2 = 25.0, emissivity = 0.95) {
    const absorbedSolar = (1.0 - Math.max(0, Math.min(1.0, albedo))) * Math.max(0, solarFluxW_M2);
    const totalInflow = absorbedSolar + Math.max(0, downwellingIrW_M2);
    const denom = Math.max(0.01, emissivity) * this.STEFAN_BOLTZMANN;

    const tEq = Math.pow(totalInflow / denom, 0.25);
    return parseFloat(tEq.toFixed(2));
  }
}





