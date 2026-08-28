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

  // --- Two-Layer Apparent TI, Fourier Harmonics & Geothermal Flux Solvers ---

  /**
   * Calculate apparent two-layer thermal inertia for a mantle layer of thickness L over bedrock.
   * Gamma_app = Gamma_1 * (Gamma_2 + Gamma_1 * tanh(mu)) / (Gamma_1 + Gamma_2 * tanh(mu))
   * where mu = L / delta_1
   * @param {number} topThermalInertia - Upper mantle thermal inertia (tiu)
   * @param {number} bottomThermalInertia - Deep substrate thermal inertia (tiu)
   * @param {number} topThicknessMeters - Upper layer thickness L in meters
   * @param {number} [periodSeconds=88775.244] - Thermal wave period (1 Sol)
   * @returns {{apparentThermalInertia: number, skinDepthRatio: number, isBedrockDominated: boolean}}
   */
  static computeTwoLayerApparentThermalInertia(topThermalInertia, bottomThermalInertia, topThicknessMeters, periodSeconds = 88775.244) {
    const g1 = Math.max(10, topThermalInertia);
    const g2 = Math.max(10, bottomThermalInertia);
    const L = Math.max(0, topThicknessMeters);

    const skin1 = this.computeSkinDepth(g1, periodSeconds).skinDepthMeters;
    const mu = L / Math.max(1e-5, skin1);
    const tanhMu = Math.tanh(mu);

    const numerator = g2 + g1 * tanhMu;
    const denominator = g1 + g2 * tanhMu;
    const gApp = g1 * (numerator / Math.max(1e-5, denominator));

    return {
      apparentThermalInertia: parseFloat(gApp.toFixed(1)),
      skinDepthRatio: parseFloat(mu.toFixed(3)),
      isBedrockDominated: mu < 0.5 && g2 > g1
    };
  }

  /**
   * Decompose a discrete diurnal temperature curve into its fundamental Fourier harmonics.
   * T(t) = T_mean + sum( C_k * cos(k * omega * t - phi_k) )
   * @param {Array<number>} temperatures - Equispaced diurnal temperature samples
   * @param {number} [numHarmonics=3] - Number of harmonic modes to extract
   * @returns {{meanTemp: number, harmonics: Array<{harmonic: number, amplitudeK: number, phaseRad: number, phaseHours: number}>}}
   */
  static decomposeFourierHarmonics(temperatures = [], numHarmonics = 3) {
    const N = temperatures.length;
    if (N === 0) return { meanTemp: 0, harmonics: [] };

    const mean = temperatures.reduce((a, b) => a + b, 0) / N;
    const harmonics = [];

    for (let k = 1; k <= numHarmonics; k++) {
      let sumCos = 0;
      let sumSin = 0;

      for (let j = 0; j < N; j++) {
        const theta = (2.0 * Math.PI * k * j) / N;
        sumCos += temperatures[j] * Math.cos(theta);
        sumSin += temperatures[j] * Math.sin(theta);
      }

      const A_k = (2.0 / N) * sumCos;
      const B_k = (2.0 / N) * sumSin;
      const C_k = Math.hypot(A_k, B_k);
      const phi_k = Math.atan2(B_k, A_k);
      const phaseHours = (phi_k / (2.0 * Math.PI * k)) * 24.0;

      harmonics.push({
        harmonic: k,
        amplitudeK: parseFloat(C_k.toFixed(2)),
        phaseRad: parseFloat(phi_k.toFixed(4)),
        phaseHours: parseFloat(phaseHours.toFixed(2))
      });
    }

    return {
      meanTemp: parseFloat(mean.toFixed(2)),
      harmonics
    };
  }

  /**
   * Calculate subsurface conductive geothermal heat flux from vertical temperature gradient.
   * q = k * (dT / dz)
   * @param {number} temperatureGradientK_PerM - Vertical temperature gradient (K/m)
   * @param {number} [thermalConductivityW_MK=2.0] - Rock/regolith thermal conductivity (W/(m K))
   * @returns {{heatFluxW_M2: number, heatFluxMw_M2: number}}
   */
  static computeSubsurfaceGeothermalFlux(temperatureGradientK_PerM, thermalConductivityW_MK = 2.0) {
    const k = Math.max(0.001, thermalConductivityW_MK);
    const grad = temperatureGradientK_PerM;
    const q_W = k * grad;
    const q_mW = q_W * 1000.0;

    return {
      heatFluxW_M2: parseFloat(q_W.toFixed(4)),
      heatFluxMw_M2: parseFloat(q_mW.toFixed(2))
    };
  }

  // --- IR Atmospheric Window, Seasonal Frost Cap Recession & Harmonic Phase Velocity Solvers ---

  /**
   * Calculate atmospheric thermal infrared window spectral transmission (8-12 µm and 17-25 µm).
   * @param {number} [dustTau=0.3] - Column dust optical depth
   * @param {number} [surfacePressurePa=610.0] - Surface atmospheric pressure in Pa
   * @returns {{windowTransmission: number, windowOpticalDepth: number}}
   */
  static computeAtmosphericInfraredWindowTransmission(dustTau = 0.3, surfacePressurePa = 610.0) {
    const tauDust = Math.max(0, dustTau);
    const pRatio = Math.max(0, surfacePressurePa) / 610.0;
    // In thermal window, gas absorption is small (~0.02) and dust cross-section is ~0.35 of visible
    const tauWin = tauDust * 0.35 + 0.02 * pRatio;
    const trans = Math.exp(-tauWin);

    return {
      windowTransmission: parseFloat(trans.toFixed(4)),
      windowOpticalDepth: parseFloat(tauWin.toFixed(4))
    };
  }

  /**
   * Calculate polar seasonal CO2 cap sublimation receding velocity in mm/sol.
   * v_recede = [ (1 - A)*S_sun - eps*sigma*T_frost^4 ] / (rho_ice * L_subl)
   * @param {number} solarInsolationW_M2 - Solar insolation incident on frost cap
   * @param {number} [albedo=0.65] - CO2 frost cap albedo (~0.65)
   * @param {number} [latentHeatSublimation=5.9e5] - Latent heat of sublimation (J/kg)
   * @returns {{recessionRateMmPerSol: number, isReceding: boolean}}
   */
  static computeFrostCapRecessionRate(solarInsolationW_M2, albedo = 0.65, latentHeatSublimation = 5.9e5) {
    const absorbed = (1.0 - Math.min(0.99, albedo)) * Math.max(0, solarInsolationW_M2);
    const radLoss = this.MARS_EMISSIVITY * this.STEFAN_BOLTZMANN * Math.pow(this.CO2_FROST_POINT, 4);
    const netFlux = absorbed - radLoss;

    const rhoIce = 1600.0; // kg/m^3 for dry ice
    const mRate = netFlux / latentHeatSublimation; // kg / (m^2 s)
    const vMps = mRate / rhoIce; // m/s
    const mmPerSol = vMps * this.MARS_SOL_SECONDS * 1000.0;

    return {
      recessionRateMmPerSol: parseFloat(mmPerSol.toFixed(2)),
      isReceding: mmPerSol > 0
    };
  }

  /**
   * Calculate frequency-dependent harmonic thermal wave propagation speed and wavelength.
   * v_thermal = sqrt(2 * omega * kappa),  lambda_thermal = 2 * pi * sqrt(2 * kappa / omega)
   * @param {number} thermalInertia - Thermal inertia (SI)
   * @param {number} [periodSeconds=88775.244] - Harmonic period in seconds
   * @returns {{thermalWaveSpeedMmPerSol: number, thermalWavelengthCm: number}}
   */
  static computeHarmonicPhaseLagDepth(thermalInertia, periodSeconds = 88775.244) {
    const cVol = this.DENSITY * this.SPECIFIC_HEAT;
    const k = (thermalInertia * thermalInertia) / cVol;
    const kappa = k / cVol; // Thermal diffusivity m^2/s

    const omega = (2.0 * Math.PI) / periodSeconds;
    const vMps = Math.sqrt(2.0 * omega * kappa);
    const lambdaM = 2.0 * Math.PI * Math.sqrt(2.0 * kappa / omega);

    return {
      thermalWaveSpeedMmPerSol: parseFloat((vMps * this.MARS_SOL_SECONDS * 1000.0).toFixed(2)),
      thermalWavelengthCm: parseFloat((lambdaM * 100.0).toFixed(2))
    };
  }

  // --- Atmospheric Thermal Backflux, Pore Ice Conductivity & Emission Contrast Solvers ---

  /**
   * Calculate atmospheric thermal infrared downwelling backflux with dust spectral emission weighting.
   * F_back = eps_atm * sigma * T_air^4 * (1 - exp(-tau_IR))
   * @param {number} [airTempK=210.0] - Effective atmospheric temperature (K)
   * @param {number} [dustTau=0.3] - Column dust optical depth
   * @param {number} [surfacePressurePa=610.0] - Surface atmospheric pressure (Pa)
   * @returns {{backfluxW_M2: number, effectiveIRemissivity: number}}
   */
  static computeAtmosphericThermalBackfluxSpectral(airTempK = 210.0, dustTau = 0.3, surfacePressurePa = 610.0) {
    const tauIR = Math.max(0.01, dustTau * 0.35); // Thermal IR dust cross section ~ 0.35 of visible
    const pRatio = Math.max(0, surfacePressurePa) / 610.0;
    const epsGas = 0.08 * pRatio;
    const epsDust = 1.0 - Math.exp(-tauIR);
    const epsTotal = Math.min(1.0, epsGas + epsDust);

    const flux = epsTotal * this.STEFAN_BOLTZMANN * Math.pow(Math.max(1, airTempK), 4);

    return {
      backfluxW_M2: parseFloat(flux.toFixed(2)),
      effectiveIRemissivity: parseFloat(epsTotal.toFixed(4))
    };
  }

  /**
   * Calculate effective thermal conductivity of porous regolith with subsurface pore ice cementation.
   * k_eff = k_matrix^(1 - phi) * k_pore^phi  (Woodside & Messmer geometric mean)
   * @param {number} matrixConductivity - Dry matrix thermal conductivity in W/(m K) (e.g. 0.05)
   * @param {number} [iceConductivity=2.2] - Pure water ice thermal conductivity in W/(m K)
   * @param {number} [porosity=0.35] - Volumetric pore fraction (0.0 to 1.0)
   * @param {number} [iceSaturation=0.8] - Pore space ice filling fraction (0.0 = dry, 1.0 = fully ice-cemented)
   * @returns {{effectiveConductivityW_MK: number, enhancementRatio: number}}
   */
  static computePoreIceThermalConductivity(matrixConductivity, iceConductivity = 2.2, porosity = 0.35, iceSaturation = 0.8) {
    const kMat = Math.max(1e-4, matrixConductivity);
    const phi = Math.max(0, Math.min(0.9, porosity));
    const sIce = Math.max(0, Math.min(1.0, iceSaturation));

    // Pore filling thermal conductivity: mixture of gas (0.015) and ice (2.2)
    const kPore = (1.0 - sIce) * 0.015 + sIce * Math.max(0.1, iceConductivity);

    // Geometric mean model
    const kEff = Math.pow(kMat, 1.0 - phi) * Math.pow(kPore, phi);
    const ratio = kEff / kMat;

    return {
      effectiveConductivityW_MK: parseFloat(kEff.toFixed(4)),
      enhancementRatio: parseFloat(ratio.toFixed(2))
    };
  }

  /**
   * Calculate analytical peak-to-trough diurnal surface temperature amplitude contrast.
   * Delta_T = (2 * (1 - A) * S0) / (sqrt(pi) * I * sqrt(omega))
   * @param {number} solarInsolationW_M2 - Peak noon solar insolation (W/m^2)
   * @param {number} [albedo=0.25] - Bolometric surface albedo
   * @param {number} [thermalInertia=250.0] - Thermal inertia in SI (tiu)
   * @returns {{diurnalAmplitudeK: number, estimatedMaxTempK: number, estimatedMinTempK: number}}
   */
  static computeDiurnalThermalEmissionContrast(solarInsolationW_M2, albedo = 0.25, thermalInertia = 250.0) {
    const s0 = Math.max(0, solarInsolationW_M2);
    const A = Math.max(0, Math.min(0.95, albedo));
    const I = Math.max(10, thermalInertia);
    const omega = (2.0 * Math.PI) / this.MARS_SOL_SECONDS;

    const deltaT = (2.0 * (1.0 - A) * s0) / (Math.sqrt(Math.PI) * I * Math.sqrt(omega));
    const tMean = Math.pow(((1.0 - A) * s0 / Math.PI) / (this.STEFAN_BOLTZMANN * this.MARS_EMISSIVITY), 0.25);

    return {
      diurnalAmplitudeK: parseFloat(deltaT.toFixed(1)),
      estimatedMaxTempK: parseFloat((tMean + deltaT * 0.5).toFixed(1)),
      estimatedMinTempK: parseFloat((tMean - deltaT * 0.5).toFixed(1))
    };
  }

  // --- Surface Radiative Energy Balance, CO2 Mass Balance & Deep Geotherm Solvers ---

  /**
   * Calculate closed surface energy balance solving equilibrium surface temperature and outgoing thermal flux.
   * F_net = F_abs_solar + F_down_IR + F_cond - eps * sigma * T_surf^4 = 0
   * @param {number} absorbedSolarW_M2 - Net absorbed solar flux (1 - A) * S
   * @param {number} downwellingIRW_M2 - Downward atmospheric thermal infrared flux
   * @param {number} [groundConductiveFluxW_M2=0] - Conductive heat flux from subsurface into surface
   * @param {number} [emissivity=0.95] - Broadband surface thermal emissivity
   * @returns {{equilibriumTempK: number, outgoingThermalFluxW_M2: number, totalEnergyInflowW_M2: number}}
   */
  static computeSurfaceRadiativeEnergyBalance(absorbedSolarW_M2, downwellingIRW_M2, groundConductiveFluxW_M2 = 0, emissivity = 0.95) {
    const totalInflow = Math.max(0, absorbedSolarW_M2) + Math.max(0, downwellingIRW_M2) + groundConductiveFluxW_M2;
    const eps = Math.max(0.01, Math.min(1.0, emissivity));
    const denom = eps * this.STEFAN_BOLTZMANN;

    const tEq = Math.pow(Math.max(1, totalInflow) / denom, 0.25);
    const fUp = eps * this.STEFAN_BOLTZMANN * Math.pow(tEq, 4);

    return {
      equilibriumTempK: parseFloat(tEq.toFixed(2)),
      outgoingThermalFluxW_M2: parseFloat(fUp.toFixed(2)),
      totalEnergyInflowW_M2: parseFloat(totalInflow.toFixed(2))
    };
  }

  /**
   * Calculate CO2 frost condensation mass and layer accumulation thickness from net radiative deficit.
   * Delta_m = (F_deficit * Delta_t) / L_subl,  Delta_z = Delta_m / rho_ice
   * @param {number} netEnergyDeficitW_M2 - Energy loss rate below frost point (W/m^2)
   * @param {number} [timeSeconds=88775.244] - Accumulation duration in seconds (defaults to 1 Sol)
   * @param {number} [latentHeatSublimation=5.9e5] - Latent heat of CO2 sublimation (J/kg)
   * @param {number} [co2IceDensityKgM3=1600.0] - Solid CO2 dry ice density
   * @returns {{accumulatedMassKg_M2: number, frostThicknessMm: number, condensationRateKg_M2_S: number}}
   */
  static computeCO2LatentHeatMassBalance(netEnergyDeficitW_M2, timeSeconds = 88775.244, latentHeatSublimation = 5.9e5, co2IceDensityKgM3 = 1600.0) {
    const fDeficit = Math.max(0, netEnergyDeficitW_M2);
    const mRate = fDeficit / latentHeatSublimation; // kg / (m^2 s)
    const totalMass = mRate * timeSeconds; // kg / m^2
    const thicknessM = totalMass / co2IceDensityKgM3;
    const thicknessMm = thicknessM * 1000.0;

    return {
      accumulatedMassKg_M2: parseFloat(totalMass.toFixed(3)),
      frostThicknessMm: parseFloat(thicknessMm.toFixed(3)),
      condensationRateKg_M2_S: parseFloat(mRate.toExponential(4))
    };
  }

  /**
   * Calculate deep subsurface geothermal equilibrium temperature at depth z.
   * T(z) = T_surf + (q_geo / k) * z
   * @param {number} meanSurfaceTempK - Mean annual surface temperature in Kelvin
   * @param {number} [geothermalFluxMwM2=30.0] - Basal geothermal heat flux in mW/m^2
   * @param {number} [crustConductivityW_MK=2.0] - Crustal rock thermal conductivity in W/(m K)
   * @param {number} [depthMeters=1000.0] - Subsurface depth in meters
   * @returns {{temperatureAtDepthK: number, geothermalGradientKPerKm: number}}
   */
  static computeDeepSubsurfaceGeothermEquilibrium(meanSurfaceTempK, geothermalFluxMwM2 = 30.0, crustConductivityW_MK = 2.0, depthMeters = 1000.0) {
    const q_W = geothermalFluxMwM2 * 1e-3;
    const k = Math.max(0.01, crustConductivityW_MK);
    const gradKPerM = q_W / k;
    const gradKPerKm = gradKPerM * 1000.0;

    const zM = Math.max(0, depthMeters);
    const tDepth = meanSurfaceTempK + gradKPerM * zM;

    return {
      temperatureAtDepthK: parseFloat(tDepth.toFixed(2)),
      geothermalGradientKPerKm: parseFloat(gradKPerKm.toFixed(2))
    };
  }

  // --- Subsurface Wave Attenuation, TI Inversion & Heat Diffusion Solvers ---

  /**
   * Calculate exact subsurface temperature wave exponential attenuation factor and phase delay.
   * A(z) = exp(-z / d_s),  phase(z) = z / d_s (radians)
   * @param {number} depthMeters - Depth below surface in meters (z)
   * @param {number} thermalInertia - Regolith thermal inertia (SI tiu)
   * @param {number} [periodSeconds=88775.244] - Thermal wave period (defaults to 1 Sol)
   * @returns {{attenuationFraction: number, phaseDelayRadians: number, phaseDelayHours: number, skinDepthMeters: number}}
   */
  static computeSubsurfaceAttenuationAndPhase(depthMeters, thermalInertia, periodSeconds = 88775.244) {
    const skin = this.computeSkinDepth(thermalInertia, periodSeconds);
    const ds = Math.max(1e-4, skin.skinDepthMeters);
    const z = Math.max(0, depthMeters);

    const atten = Math.exp(-z / ds);
    const phaseRad = z / ds;
    const phaseHours = (phaseRad / (2.0 * Math.PI)) * (periodSeconds / 3600.0);

    return {
      attenuationFraction: parseFloat(atten.toFixed(4)),
      phaseDelayRadians: parseFloat(phaseRad.toFixed(3)),
      phaseDelayHours: parseFloat(phaseHours.toFixed(2)),
      skinDepthMeters: parseFloat(ds.toFixed(4))
    };
  }

  /**
   * Invert regolith thermal inertia from diurnal surface temperature excursion amplitude.
   * I = (2 * (1 - A) * S0) / (sqrt(pi * omega) * Delta_T)
   * @param {number} deltaT - Diurnal temperature amplitude (T_max - T_min in Kelvin)
   * @param {number} [solarInsolationW_M2=500.0] - Noon solar insolation flux (W/m^2)
   * @param {number} [albedo=0.25] - Bolometric surface albedo
   * @returns {{thermalInertiaTIU: number, classification: string}}
   */
  static invertThermalInertiaFromAmplitude(deltaT, solarInsolationW_M2 = 500.0, albedo = 0.25) {
    const safeDelta = Math.max(1.0, deltaT);
    const s0 = Math.max(0, solarInsolationW_M2);
    const A = Math.max(0, Math.min(0.95, albedo));
    const omega = (2.0 * Math.PI) / this.MARS_SOL_SECONDS;

    const I = (2.0 * (1.0 - A) * s0) / (Math.sqrt(Math.PI * omega) * safeDelta);
    const roundedI = parseFloat(I.toFixed(1));
    const grainClass = this.classifyRegolithGrainSize(roundedI);

    return {
      thermalInertiaTIU: roundedI,
      classification: grainClass.classification
    };
  }

  /**
   * Calculate 1D Fourier conductive heat flux between surface and subsurface layer.
   * F_cond = k * (T_sub - T_surf) / dz
   * @param {number} tempSurfaceK - Surface temperature in Kelvin
   * @param {number} tempSubsurfaceK - Subsurface node temperature in Kelvin
   * @param {number} layerThicknessMeters - Distance between nodes in meters
   * @param {number} thermalConductivityW_MK - Bulk thermal conductivity in W/(m K)
   * @returns {{conductiveFluxW_M2: number, isHeatingSurface: boolean}}
   */
  static computeSubsurfaceHeatDiffusionFlux(tempSurfaceK, tempSubsurfaceK, layerThicknessMeters, thermalConductivityW_MK) {
    const dz = Math.max(1e-4, layerThicknessMeters);
    const k = Math.max(1e-4, thermalConductivityW_MK);
    const flux = k * (tempSubsurfaceK - tempSurfaceK) / dz;

    return {
      conductiveFluxW_M2: parseFloat(flux.toFixed(2)),
      isHeatingSurface: flux > 0
    };
  }

  // --- Annual Skin Depth Ratio, Equilibrium Surface Temperature & Geothermal Profile Solvers ---

  /**
   * Calculate exact annual-to-diurnal thermal skin depth amplification ratio.
   * ratio = sqrt( P_annual / P_diurnal ) = sqrt( 668.6 ) ~ 25.857
   * @param {number} [solsPerYear=668.6] - Number of Martian solar days (sols) per year
   * @returns {{skinDepthRatio: number, seasonalPenetrationFactor: number}}
   */
  static computeAnnualToDiurnalSkinDepthRatio(solsPerYear = 668.6) {
    const ratio = Math.sqrt(Math.max(1.0, solsPerYear));

    return {
      skinDepthRatio: parseFloat(ratio.toFixed(3)),
      seasonalPenetrationFactor: parseFloat(ratio.toFixed(2))
    };
  }

  /**
   * Calculate radiative equilibrium surface temperature including solar insolation and geothermal heat flow.
   * T_eq = [ ( (1 - A) * S0 * cos(i) + F_geo ) / (eps * sigma) ]^(1/4)
   * @param {number} [solarInsolationW_M2=500.0] - Solar flux at top of surface in W/m^2
   * @param {number} [incidenceAngleDeg=0.0] - Solar incidence angle in degrees
   * @param {number} [albedo=0.25] - Bolometric surface albedo
   * @param {number} [geothermalFluxMwM2=30.0] - Basal geothermal heat flux in mW/m^2
   * @param {number} [emissivity=0.95] - Thermal infrared emissivity
   * @returns {{equilibriumTempK: number, absorbedSolarFluxW_M2: number, geothermalFluxW_M2: number}}
   */
  static computeEquilibriumSurfaceTemperatureWithGeothermal(solarInsolationW_M2 = 500.0, incidenceAngleDeg = 0.0, albedo = 0.25, geothermalFluxMwM2 = 30.0, emissivity = 0.95) {
    const s0 = Math.max(0, solarInsolationW_M2);
    const incRad = Math.abs(incidenceAngleDeg) * Math.PI / 180.0;
    const cosI = Math.max(0, Math.cos(incRad));
    const A = Math.max(0, Math.min(0.99, albedo));
    const fGeo = Math.max(0, geothermalFluxMwM2) * 1e-3; // W/m^2
    const eps = Math.max(0.01, Math.min(1.0, emissivity));

    const absorbedSolar = (1.0 - A) * s0 * cosI;
    const totalInflow = absorbedSolar + fGeo;
    const denom = eps * this.STEFAN_BOLTZMANN;

    const tEq = Math.pow(totalInflow / denom, 0.25);

    return {
      equilibriumTempK: parseFloat(tEq.toFixed(2)),
      absorbedSolarFluxW_M2: parseFloat(absorbedSolar.toFixed(2)),
      geothermalFluxW_M2: parseFloat(fGeo.toFixed(5))
    };
  }

  /**
   * Calculate 1D steady-state subsurface geothermal temperature profile.
   * T(z) = T_surf + (q / k) * z
   * @param {number} [surfaceTempK=210.0] - Mean annual surface temperature in Kelvin
   * @param {number} [geothermalHeatFlowMwM2=30.0] - Basal geothermal heat flow in mW/m^2
   * @param {number} [crustThermalConductivity=2.0] - Crustal thermal conductivity in W/(m K)
   * @param {number} [depthKm=5.0] - Subsurface depth in km
   * @returns {{temperatureAtDepthK: number, geothermalGradientKPerKm: number, depthMeters: number}}
   */
  static computeSubsurfaceGeothermalTemperatureProfile(surfaceTempK = 210.0, geothermalHeatFlowMwM2 = 30.0, crustThermalConductivity = 2.0, depthKm = 5.0) {
    const q_W = geothermalHeatFlowMwM2 * 1e-3;
    const k = Math.max(0.01, crustThermalConductivity);
    const gradKPerM = q_W / k;
    const gradKPerKm = gradKPerM * 1000.0;
    const zM = Math.max(0, depthKm) * 1000.0;

    const tDepth = surfaceTempK + gradKPerM * zM;

    return {
      temperatureAtDepthK: parseFloat(tDepth.toFixed(2)),
      geothermalGradientKPerKm: parseFloat(gradKPerKm.toFixed(2)),
      depthMeters: parseFloat(zM.toFixed(1))
    };
  }

  // --- Downwelling Flux, Phase Lag & Effective Bolometric Temperature Solvers ---

  /**
   * Calculate atmospheric downward thermal infrared flux at surface.
   * F_atm = eps_atm * sigma * T_air^4
   * @param {number} [airTempK=210.0] - Effective near-surface air temperature in Kelvin
   * @param {number} [dustTau=0.3] - Column dust optical depth
   * @param {number} [surfacePressurePa=610.0] - Surface pressure in Pa
   * @returns {{downwellingFluxW_M2: number, atmosphericEmissivity: number}}
   */
  static computeAtmosphericDownwellingThermalFlux(airTempK = 210.0, dustTau = 0.3, surfacePressurePa = 610.0) {
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
   * Calculate diurnal subsurface thermal wave propagation phase lag.
   * Delta_t = (z / delta) * (P_sol / (2 * pi))
   * @param {number} depthMeters - Subsurface depth in meters
   * @param {number} thermalInertia - Regolith thermal inertia (tiu)
   * @param {number} [periodSeconds=88775.244] - Thermal period (1 Sol)
   * @returns {{phaseLagHours: number, phaseLagRadians: number, amplitudeDecayRatio: number}}
   */
  static computeThermalWavePhaseLag(depthMeters, thermalInertia, periodSeconds = 88775.244) {
    const skin = this.computeSkinDepth(thermalInertia, periodSeconds);
    const delta = Math.max(1e-4, skin.skinDepthMeters);
    const z = Math.max(0, depthMeters);

    const phaseRad = z / delta;
    const periodHours = periodSeconds / 3600.0;
    const phaseLagHours = (phaseRad / (2.0 * Math.PI)) * periodHours;
    const ampRatio = Math.exp(-z / delta);

    return {
      phaseLagHours: parseFloat(phaseLagHours.toFixed(2)),
      phaseLagRadians: parseFloat(phaseRad.toFixed(3)),
      amplitudeDecayRatio: parseFloat(ampRatio.toFixed(4))
    };
  }

  /**
   * Calculate effective bolometric radiating surface temperature.
   * T_eff = [ ((1 - A_B) * S) / (eps * sigma) ]^(1/4)
   * @param {number} solarFluxW_M2 - Direct incident solar flux in W/m^2
   * @param {number} [bondAlbedo=0.25] - Bolometric Bond albedo
   * @param {number} [emissivity=0.95] - Surface thermal infrared emissivity
   * @returns {{effectiveTempK: number, absorbedSolarFluxW_M2: number}}
   */
  static computeEffectiveBolometricTemperature(solarFluxW_M2, bondAlbedo = 0.25, emissivity = 0.95) {
    const s0 = Math.max(0, solarFluxW_M2);
    const A = Math.max(0, Math.min(0.99, bondAlbedo));
    const eps = Math.max(0.01, Math.min(1.0, emissivity));

    const absorbed = (1.0 - A) * s0;
    const tEff = Math.pow(absorbed / (eps * this.STEFAN_BOLTZMANN), 0.25);

    return {
      effectiveTempK: parseFloat(tEff.toFixed(2)),
      absorbedSolarFluxW_M2: parseFloat(absorbed.toFixed(2))
    };
  }

  // --- Temperature-Dependent Specific Heat, Damping Ratio & Net Longwave Loss Solvers ---

  /**
   * Calculate temperature-dependent specific heat capacity c_p(T) for Martian basaltic regolith.
   * c_p(T) = c_0 - c_1 * exp(-T / T_scale) (Ledlow et al. formulation for silicates)
   * @param {number} tempK - Temperature in Kelvin (50K to 400K)
   * @returns {{specificHeatJ_KgK: number, volumetricHeatCapacityJ_M3K: number}}
   */
  static computeTemperatureDependentSpecificHeat(tempK) {
    const T = Math.max(10.0, tempK);
    const cp = 890.0 - 450.0 * Math.exp(-T / 150.0);
    const cVol = this.DENSITY * cp;

    return {
      specificHeatJ_KgK: parseFloat(cp.toFixed(2)),
      volumetricHeatCapacityJ_M3K: parseFloat(cVol.toFixed(1))
    };
  }

  /**
   * Calculate subsurface thermal wave exponential amplitude damping ratio A(z) / A_0.
   * Ratio = exp(-z / delta)
   * @param {number} depthMeters - Subsurface depth in meters
   * @param {number} skinDepthMeters - Thermal skin depth in meters
   * @returns {{amplitudeRatio: number, percentOfSurfaceAmplitude: number}}
   */
  static computeSubsurfaceThermalDampingRatio(depthMeters, skinDepthMeters) {
    const z = Math.max(0, depthMeters);
    const delta = Math.max(1e-4, skinDepthMeters);

    const ratio = Math.exp(-z / delta);
    const pct = ratio * 100.0;

    return {
      amplitudeRatio: parseFloat(ratio.toFixed(4)),
      percentOfSurfaceAmplitude: parseFloat(pct.toFixed(2))
    };
  }

  /**
   * Calculate net outgoing longwave radiative energy loss from Martian surface.
   * F_net = eps * sigma * T_s^4 - F_down_atm
   * @param {number} surfaceTempK - Surface skin temperature in Kelvin
   * @param {number} atmosphericDownwellingW_M2 - Downwelling atmospheric thermal flux
   * @param {number} [emissivity=0.95] - Surface broadband thermal infrared emissivity
   * @returns {{netRadiativeLossW_M2: number, upwardEmittedFluxW_M2: number, isCooling: boolean}}
   */
  static computeSurfaceNetRadiativeLoss(surfaceTempK, atmosphericDownwellingW_M2, emissivity = 0.95) {
    const eps = Math.max(0.01, Math.min(1.0, emissivity));
    const t = Math.max(0, surfaceTempK);
    const fUp = eps * this.STEFAN_BOLTZMANN * Math.pow(t, 4);
    const fDown = Math.max(0, atmosphericDownwellingW_M2);
    const fNet = fUp - fDown;

    return {
      netRadiativeLossW_M2: parseFloat(fNet.toFixed(2)),
      upwardEmittedFluxW_M2: parseFloat(fUp.toFixed(2)),
      isCooling: fNet > 0
    };
  }

  // --- Interlayer Heat Flux, Seasonal TI Modulation & Downwelling Solvers ---

  /**
   * Calculate discrete Kieffer finite-difference conductive heat flux between subsurface layers.
   * F_{i+1/2} = -k * (T_{i+1} - T_i) / Delta_z
   * @param {number} tempUpperK - Upper layer temperature in Kelvin
   * @param {number} tempLowerK - Lower layer temperature in Kelvin
   * @param {number} layerThicknessMeters - Distance between layer centers in meters
   * @param {number} [thermalConductivityW_MK=0.05] - Intermediate layer thermal conductivity
   * @returns {{conductiveFluxW_M2: number, isHeatFlowingDownward: boolean}}
   */
  static computeSubsurfaceInterlayerHeatFlux(tempUpperK, tempLowerK, layerThicknessMeters, thermalConductivityW_MK = 0.05) {
    const dz = Math.max(1e-5, layerThicknessMeters);
    const k = Math.max(1e-4, thermalConductivityW_MK);
    const dT = tempLowerK - tempUpperK;
    const flux = -k * (dT / dz); // Positive when flowing upward, negative downward

    return {
      conductiveFluxW_M2: parseFloat(flux.toFixed(4)),
      isHeatFlowingDownward: flux < 0
    };
  }

  /**
   * Calculate seasonal modulation of apparent thermal inertia across eccentric Mars orbit.
   * I_app(Ls) = I_0 * (1 + beta * cos(Ls - Ls_peri))
   * @param {number} baseThermalInertia - Baseline thermal inertia in tiu
   * @param {number} LsDeg - Solar Longitude in degrees (0 to 360)
   * @param {number} [modulationFactor=0.15] - Seasonal modulation coefficient beta (~0.15 for Mars)
   * @returns {{apparentThermalInertia: number, percentModulation: number}}
   */
  static computeSeasonalApparentThermalInertiaModulation(baseThermalInertia, LsDeg, modulationFactor = 0.15) {
    const i0 = Math.max(10, baseThermalInertia);
    const beta = Math.max(0, Math.min(0.5, modulationFactor));
    const dLsRad = (LsDeg - 250.99) * Math.PI / 180.0; // Offset relative to perihelion Ls = 251 deg

    const factor = 1.0 + beta * Math.cos(dLsRad);
    const iApp = i0 * factor;
    const pctMod = (factor - 1.0) * 100.0;

    return {
      apparentThermalInertia: parseFloat(iApp.toFixed(2)),
      percentModulation: parseFloat(pctMod.toFixed(2))
    };
  }

  /**
   * Calculate downward atmospheric thermal infrared flux with CO2 gas band and dust opacity.
   * F_atm = sigma * T_air^4 * ( 1 - exp(-0.35 * tau) + 0.08 * (P / 610) )
   * @param {number} airTempK - Near-surface atmospheric air temperature in Kelvin
   * @param {number} dustOpticalDepthTau - Visible dust optical depth tau
   * @param {number} [surfacePressurePa=610.0] - Surface atmospheric pressure in Pa
   * @returns {{downwellingFluxW_M2: number, atmosphericEmissivity: number}}
   */
  static computeAtmosphericThermalInfraredDownwellingFlux(airTempK, dustOpticalDepthTau, surfacePressurePa = 610.0) {
    const tauIR = Math.max(0.001, dustOpticalDepthTau * 0.35);
    const pRatio = Math.max(0.01, surfacePressurePa) / 610.0;
    const epsDust = 1.0 - Math.exp(-tauIR);
    const epsGas = 0.08 * pRatio;
    const epsAtm = Math.min(1.0, epsDust + epsGas);

    const flux = epsAtm * this.STEFAN_BOLTZMANN * Math.pow(Math.max(10, airTempK), 4);

    return {
      downwellingFluxW_M2: parseFloat(flux.toFixed(2)),
      atmosphericEmissivity: parseFloat(epsAtm.toFixed(4))
    };
  }

  // --- Thermal Diffusion Time, Frost Albedo Feedback & Geothermal Offset Solvers ---

  /**
   * Calculate characteristic thermal diffusion timescale tau_d for a regolith layer of thickness dz.
   * tau_d = dz^2 / (2 * kappa) = (dz^2 * C_vol) / (2 * k)
   * @param {number} layerThicknessMeters - Layer thickness in meters
   * @param {number} thermalInertia - Thermal inertia in tiu
   * @param {number} [density=1500] - Regolith density in kg/m^3
   * @param {number} [specificHeat=800] - Specific heat in J/(kg K)
   * @returns {{diffusionTimeSeconds: number, diffusionTimeHours: number, thermalDiffusivityM2S: number}}
   */
  static computeSubsurfaceLayerThermalDiffusionTime(layerThicknessMeters, thermalInertia, density = 1500, specificHeat = 800) {
    const dz = Math.max(1e-5, layerThicknessMeters);
    const I = Math.max(10, thermalInertia);
    const cVol = density * specificHeat;
    const k = (I * I) / cVol;
    const kappa = k / cVol; // m^2 / s

    const tauSec = (dz * dz) / (2.0 * Math.max(1e-12, kappa));
    const tauHours = tauSec / 3600.0;

    return {
      diffusionTimeSeconds: parseFloat(tauSec.toFixed(1)),
      diffusionTimeHours: parseFloat(tauHours.toFixed(3)),
      thermalDiffusivityM2S: parseFloat(kappa.toExponential(4))
    };
  }

  /**
   * Calculate effective surface albedo with non-linear frost deposition / sublimation feedback.
   * A_eff = A_bare + (A_frost - A_bare) * min(1.0, sqrt(m_frost / m_crit))
   * @param {number} bareAlbedo - Regolith bare surface albedo (e.g. 0.25)
   * @param {number} frostAlbedo - Pure frost albedo (e.g. 0.65 for CO2 dry ice)
   * @param {number} accumulatedFrostMassKgM2 - Deposited frost mass in kg/m^2
   * @param {number} [criticalFrostMassKgM2=5.0] - Mass required for optical saturation (~5 kg/m^2)
   * @returns {{effectiveAlbedo: number, frostCoverageFraction: number, isFrostSaturated: boolean}}
   */
  static computeFrostAlbedoFeedbackTransition(bareAlbedo, frostAlbedo, accumulatedFrostMassKgM2, criticalFrostMassKgM2 = 5.0) {
    const aBare = Math.max(0.01, Math.min(0.99, bareAlbedo));
    const aFrost = Math.max(aBare, Math.min(0.99, frostAlbedo));
    const m = Math.max(0, accumulatedFrostMassKgM2);
    const mCrit = Math.max(0.1, criticalFrostMassKgM2);

    const coverage = Math.min(1.0, Math.sqrt(m / mCrit));
    const aEff = aBare + (aFrost - aBare) * coverage;

    return {
      effectiveAlbedo: parseFloat(aEff.toFixed(4)),
      frostCoverageFraction: parseFloat(coverage.toFixed(3)),
      isFrostSaturated: m >= mCrit
    };
  }

  /**
   * Calculate steady-state conductive temperature offset across a subsurface stratigraphic layer from geothermal heat flow.
   * Delta_T = (q_geo * dz) / k
   * @param {number} geothermalFluxMwM2 - Geothermal heat flux in mW/m^2 (e.g. 30 mW/m^2)
   * @param {number} thermalConductivityW_MK - Layer thermal conductivity in W/(m K)
   * @param {number} layerThicknessMeters - Layer physical thickness in meters
   * @returns {{temperatureOffsetK: number, thermalResistanceM2K_W: number}}
   */
  static computeSubsurfaceConductiveTemperatureOffset(geothermalFluxMwM2, thermalConductivityW_MK, layerThicknessMeters) {
    const qW = Math.max(0, geothermalFluxMwM2) * 1e-3;
    const k = Math.max(1e-4, thermalConductivityW_MK);
    const dz = Math.max(0, layerThicknessMeters);

    const rTherm = dz / k; // Thermal resistance in (m^2 K) / W
    const deltaT = qW * rTherm;

    return {
      temperatureOffsetK: parseFloat(deltaT.toFixed(4)),
      thermalResistanceM2K_W: parseFloat(rTherm.toFixed(3))
    };
  }

  // --- Macroscopic Roughness, Gas Conductivity & Volatile Sublimation Solvers ---

  /**
   * Calculate effective bolometric brightness temperature for a sub-pixel mixture of sunlit and shadowed facets.
   * T_eff = [ (1 - f_shad) * T_sun^4 + f_shad * T_shad^4 ]^(1/4)
   * @param {number} sunlitTempK - Sunlit facet surface temperature in Kelvin
   * @param {number} shadowedTempK - Shadowed facet surface temperature in Kelvin
   * @param {number} [shadowFraction=0.2] - Area fraction in shadow (0.0 to 1.0)
   * @returns {{effectiveTempK: number, thermalContrastK: number, meanLinearTempK: number}}
   */
  static computeSurfaceMacroscopicRoughnessEffectiveTemp(sunlitTempK, shadowedTempK, shadowFraction = 0.2) {
    const tSun = Math.max(10, sunlitTempK);
    const tShad = Math.max(10, shadowedTempK);
    const fShad = Math.max(0.0, Math.min(1.0, shadowFraction));
    const fSun = 1.0 - fShad;

    const radEmission = fSun * Math.pow(tSun, 4) + fShad * Math.pow(tShad, 4);
    const tEff = Math.pow(radEmission, 0.25);
    const tLinear = fSun * tSun + fShad * tShad;
    const contrast = tSun - tShad;

    return {
      effectiveTempK: parseFloat(tEff.toFixed(2)),
      thermalContrastK: parseFloat(contrast.toFixed(2)),
      meanLinearTempK: parseFloat(tLinear.toFixed(2))
    };
  }

  /**
   * Calculate atmospheric pressure-dependent porous regolith thermal conductivity in the Knudsen transition regime.
   * k(P) = k_solid + (k_gas0 * (P / P0)) / (1 + (P / P0))
   * @param {number} ambientPressurePa - Ambient atmospheric surface pressure in Pa
   * @param {number} [solidConductivityW_MK=0.03] - Solid contact conductivity in vacuum
   * @param {number} [gasConductivityDatum=0.015] - Maximum CO2 pore gas conductivity contribution at datum
   * @param {number} [p0Pa=610.0] - Reference Knudsen pressure parameter (~610 Pa)
   * @returns {{effectiveConductivityW_MK: number, gasContributionFraction: number}}
   */
  static computePorousRegolithGasConductivity(ambientPressurePa, solidConductivityW_MK = 0.03, gasConductivityDatum = 0.015, p0Pa = 610.0) {
    const P = Math.max(0, ambientPressurePa);
    const kSolid = Math.max(1e-4, solidConductivityW_MK);
    const kGas0 = Math.max(0, gasConductivityDatum);
    const P0 = Math.max(1.0, p0Pa);

    const pRatio = P / P0;
    const kGas = (kGas0 * pRatio) / (1.0 + pRatio);
    const kEff = kSolid + kGas;
    const gasFraction = kGas / kEff;

    return {
      effectiveConductivityW_MK: parseFloat(kEff.toFixed(5)),
      gasContributionFraction: parseFloat(gasFraction.toFixed(3))
    };
  }

  /**
   * Calculate instant volatile sublimation / condensation rate for CO2 dry ice or H2O frost.
   * dm/dt = (F_net - eps * sigma * T_frost^4) / L_sub
   * @param {number} netSurfaceFluxW_M2 - Net absorbed shortwave + downward longwave + conductive heat flux in W/m^2
   * @param {number} [frostTempK=148.0] - Volatile equilibrium temperature (148 K for CO2 at 610 Pa)
   * @param {number} [latentHeatSublimationJ_Kg=5.9e5] - Latent heat of sublimation (5.9e5 J/kg for CO2)
   * @param {number} [emissivity=0.95] - Frost thermal infrared emissivity
   * @returns {{sublimationRateKgM2S: number, sublimationRateUmPerHour: number, isSublimating: boolean}}
   */
  static computeVolatileSublimationRate(netSurfaceFluxW_M2, frostTempK = 148.0, latentHeatSublimationJ_Kg = 5.9e5, emissivity = 0.95) {
    const T = Math.max(10, frostTempK);
    const L = Math.max(1e3, latentHeatSublimationJ_Kg);
    const eps = Math.max(0.1, Math.min(1.0, emissivity));

    const emittedRadiation = eps * this.STEFAN_BOLTZMANN * Math.pow(T, 4);
    const netEnergyForPhaseChange = netSurfaceFluxW_M2 - emittedRadiation;

    // dm/dt in kg/(m^2 s): positive = sublimation (mass loss), negative = condensation
    const dm_dt = netEnergyForPhaseChange / L;

    // Solid CO2 density ~ 1600 kg/m^3 -> rate in m/s = (dm/dt) / 1600 -> um/hr = (rate * 1e6) * 3600
    const rhoSolid = 1600.0;
    const rateUmPerHour = (dm_dt / rhoSolid) * 1e6 * 3600.0;

    return {
      sublimationRateKgM2S: parseFloat(dm_dt.toExponential(4)),
      sublimationRateUmPerHour: parseFloat(rateUmPerHour.toFixed(2)),
      isSublimating: dm_dt > 0
    };
  }

  // --- Deep Geotherm, Specific Heat & Skin Depth Inversion Solvers ---

  /**
   * Calculate steady-state deep crustal geothermal temperature profile T(z) and gradient.
   * T(z) = T_surface_mean + (q_geo / k_rock) * z
   * @param {number} surfaceMeanTempK - Mean annual surface temperature in Kelvin
   * @param {number} depthMeters - Depth z in meters below surface
   * @param {number} [geothermalFluxW_M2=0.030] - Interior basal heat flux in W/m^2 (~30 mW/m^2 on Mars)
   * @param {number} [thermalConductivityW_MK=2.0] - Crustal rock thermal conductivity (~2.0 W/(m K) for basalt)
   * @returns {{temperatureAtDepthK: number, thermalGradientK_Km: number, depthKm: number}}
   */
  static computeDeepGeothermalTemperatureProfile(surfaceMeanTempK, depthMeters, geothermalFluxW_M2 = 0.030, thermalConductivityW_MK = 2.0) {
    const T0 = Math.max(10.0, surfaceMeanTempK);
    const z = Math.max(0, depthMeters);
    const qGeo = Math.max(0, geothermalFluxW_M2);
    const k = Math.max(0.01, thermalConductivityW_MK);

    const gradientK_M = qGeo / k; // K/m
    const gradientK_Km = gradientK_M * 1000.0; // K/km
    const Tz = T0 + gradientK_M * z;

    return {
      temperatureAtDepthK: parseFloat(Tz.toFixed(2)),
      thermalGradientK_Km: parseFloat(gradientK_Km.toFixed(2)),
      depthKm: parseFloat((z / 1000.0).toFixed(3))
    };
  }

  /**
   * Calculate surface diurnal temperature swing amplitude A_T under harmonic solar insolation forcing.
   * A_T = Delta_F / ( I * sqrt(omega) ) where omega = 2 * pi / P
   * @param {number} insolationAmplitudeW_M2 - Solar forcing amplitude Delta_F in W/m^2 (e.g. 250 W/m^2)
   * @param {number} thermalInertiaSI - Surface thermal inertia in J/(m^2 K s^0.5) (e.g. 250)
   * @param {number} [periodSeconds=88775.244] - Forcing period in seconds (1 Sol)
   * @returns {{temperatureAmplitudeK: number, peakToPeakDiurnalSwingK: number, angularFrequencyRadS: number}}
   */
  static computeSurfaceThermalHarmonicAmplitude(insolationAmplitudeW_M2, thermalInertiaSI, periodSeconds = 88775.244) {
    const dF = Math.max(0, insolationAmplitudeW_M2);
    const I = Math.max(1.0, thermalInertiaSI);
    const P = Math.max(1.0, periodSeconds);

    const omega = (2.0 * Math.PI) / P;
    const ampK = dF / (I * Math.sqrt(omega));
    const peakToPeakK = ampK * 2.0;

    return {
      temperatureAmplitudeK: parseFloat(ampK.toFixed(2)),
      peakToPeakDiurnalSwingK: parseFloat(peakToPeakK.toFixed(2)),
      angularFrequencyRadS: parseFloat(omega.toExponential(4))
    };
  }

  /**
   * Calculate thermal damping skin depth delta for diurnal (Sol) or annual (seasonal) waves.
   * delta = (I / (rho * c_p)) * sqrt(P / pi)
   * @param {number} thermalInertiaSI - Thermal inertia I in J/(m^2 K s^0.5) (e.g. 300 for Martian sand)
   * @param {number} [densityKgM3=1500] - Regolith bulk density in kg/m^3
   * @param {number} [specificHeatJ_KgK=800] - Heat capacity in J/(kg K)
   * @param {number} [periodSeconds=88775.244] - Thermal wave period P in seconds (1 Sol = 88775.244 s, 1 Mars Year = 5.935e7 s)
   * @returns {{skinDepthMeters: number, skinDepthCm: number, periodHours: number}}
   */
  static computeThermalSkinDepthInversion(thermalInertiaSI, densityKgM3 = 1500, specificHeatJ_KgK = 800, periodSeconds = 88775.244) {
    const I = Math.max(1.0, thermalInertiaSI);
    const rho = Math.max(100.0, densityKgM3);
    const cp = Math.max(100.0, specificHeatJ_KgK);
    const P = Math.max(1.0, periodSeconds);

    const deltaM = (I / (rho * cp)) * Math.sqrt(P / Math.PI);
    const deltaCm = deltaM * 100.0;

    return {
      skinDepthMeters: parseFloat(deltaM.toFixed(4)),
      skinDepthCm: parseFloat(deltaCm.toFixed(2)),
      periodHours: parseFloat((P / 3600.0).toFixed(2))
    };
  }

  // --- Subsolar Equilibrium, Conductive Flux & Daily Insolation Solvers ---

  /**
   * Calculate radiative equilibrium subsolar surface temperature T_ss on a planetary body.
   * T_ss = [ ( (1 - A) * S_0 ) / ( r_AU^2 * epsilon * sigma ) ]^(1/4)
   * @param {number} [albedo=0.25] - Bolometric Bond/Lambert albedo A (0.25 for Mars mean)
   * @param {number} [heliocentricDistanceAU=1.524] - Orbital distance r in AU
   * @param {number} [emissivity=0.95] - Thermal infrared emissivity epsilon (0.95 for silicate regolith)
   * @returns {{subsolarTemperatureK: number, solarFluxW_M2: number, heliocentricDistanceAU: number}}
   */
  static computeSubsolarEquilibriumTemperature(albedo = 0.25, heliocentricDistanceAU = 1.524, emissivity = 0.95) {
    const A = Math.max(0.0, Math.min(0.99, albedo));
    const rAU = Math.max(0.1, heliocentricDistanceAU);
    const eps = Math.max(0.1, Math.min(1.0, emissivity));

    const S0 = 1361.0; // Solar constant at 1 AU (W/m^2)
    const sigma = 5.670374419e-8; // Stefan-Boltzmann constant (W / (m^2 K^4))

    const solarFlux = S0 / (rAU * rAU);
    const absorbedFlux = (1.0 - A) * solarFlux;
    const T_ss = Math.pow(absorbedFlux / (eps * sigma), 0.25);

    return {
      subsolarTemperatureK: parseFloat(T_ss.toFixed(2)),
      solarFluxW_M2: parseFloat(solarFlux.toFixed(2)),
      heliocentricDistanceAU: parseFloat(rAU.toFixed(4))
    };
  }

  /**
   * Calculate 1D Fourier conductive heat flux F_cond across regolith subsurface layer.
   * F_cond = -k * ( (T_lower - T_upper) / dz )
   * @param {number} temperatureUpperK - Temperature of upper boundary in K
   * @param {number} temperatureLowerK - Temperature of lower boundary in K
   * @param {number} layerThicknessMeters - Vertical layer thickness dz in meters
   * @param {number} [thermalConductivityW_mK=0.05] - Regolith bulk thermal conductivity k in W/(m K)
   * @returns {{conductiveHeatFluxW_M2: number, temperatureGradientK_M: number, isUpwardFlux: boolean}}
   */
  static computeConductiveHeatFlux(temperatureUpperK, temperatureLowerK, layerThicknessMeters, thermalConductivityW_mK = 0.05) {
    const Tu = Math.max(1.0, temperatureUpperK);
    const Tl = Math.max(1.0, temperatureLowerK);
    const dz = Math.max(0.001, layerThicknessMeters);
    const k = Math.max(0.0001, thermalConductivityW_mK);

    const dT_dz = (Tl - Tu) / dz;
    const flux = -k * dT_dz; // Positive if heat flows downward (Tl < Tu), negative if upward (Tl > Tu)

    return {
      conductiveHeatFluxW_M2: parseFloat(flux.toFixed(4)),
      temperatureGradientK_M: parseFloat(dT_dz.toFixed(4)),
      isUpwardFlux: flux < 0
    };
  }

  /**
   * Calculate diurnal integrated solar insolation on a horizontal planetary surface (J/m^2 and kWh/m^2).
   * E_day = ( S_0 * P_sol / ( pi * r_AU^2 ) ) * [ H_ss * sin(phi) * sin(delta) + cos(phi) * cos(delta) * sin(H_ss) ]
   * @param {number} latitudeDeg - Observer latitude in degrees
   * @param {number} solarDeclinationDeg - Subsolar declination in degrees
   * @param {number} [heliocentricDistanceAU=1.524] - Orbital distance in AU
   * @param {number} [solDurationSeconds=88775.244] - Martian Sol duration in seconds
   * @returns {{dailyInsolationJ_M2: number, dailyInsolationKWh_M2: number, sunlitHours: number}}
   */
  static computeDailyInsolationIntegral(latitudeDeg, solarDeclinationDeg, heliocentricDistanceAU = 1.524, solDurationSeconds = 88775.244) {
    const phiRad = (latitudeDeg * Math.PI) / 180.0;
    const deltaRad = (solarDeclinationDeg * Math.PI) / 180.0;
    const rAU = Math.max(0.1, heliocentricDistanceAU);
    const Psol = Math.max(100.0, solDurationSeconds);

    const S0 = 1361.0;
    const S_mars = S0 / (rAU * rAU);

    // Sunset hour angle H_ss
    const cosHss = -Math.tan(phiRad) * Math.tan(deltaRad);
    let HssRad;
    if (cosHss >= 1.0) {
      HssRad = 0.0; // Polar night
    } else if (cosHss <= -1.0) {
      HssRad = Math.PI; // Polar day
    } else {
      HssRad = Math.acos(cosHss);
    }

    const integral = HssRad * Math.sin(phiRad) * Math.sin(deltaRad) + Math.cos(phiRad) * Math.cos(deltaRad) * Math.sin(HssRad);
    const E_day = (S_mars * Psol / Math.PI) * Math.max(0, integral);
    const E_kWh = E_day / 3.6e6;
    const daylightHours = (HssRad / Math.PI) * (Psol / 3600.0);

    return {
      dailyInsolationJ_M2: parseFloat(E_day.toFixed(1)),
      dailyInsolationKWh_M2: parseFloat(E_kWh.toFixed(3)),
      sunlitHours: parseFloat(daylightHours.toFixed(2))
    };
  }

  // --- Stefan-Boltzmann Surface Emission, Downwelling Longwave & Net Energy Balance Solvers ---

  /**
   * Calculate surface thermal emission radiation flux F_emit = eps * sigma * T^4 [W/m^2].
   * @param {number} temperatureK - Surface kinetic temperature in Kelvin
   * @param {number} [emissivity=0.95] - Surface broadband thermal infrared emissivity (0.90 to 0.98)
   * @returns {{emittedFluxW_M2: number, temperatureK: number, emissivity: number}}
   */
  static computeSurfaceThermalEmission(temperatureK, emissivity = 0.95) {
    const T = Math.max(1.0, temperatureK);
    const eps = Math.max(0.1, Math.min(1.0, emissivity));
    const sigma = 5.670374419e-8; // Stefan-Boltzmann constant W/(m^2 K^4)

    const fEmit = eps * sigma * Math.pow(T, 4);

    return {
      emittedFluxW_M2: parseFloat(fEmit.toFixed(2)),
      temperatureK: parseFloat(T.toFixed(2)),
      emissivity: parseFloat(eps.toFixed(3))
    };
  }

  /**
   * Calculate downwelling atmospheric longwave radiative flux F_down = (1 - exp(-tau)) * sigma * T_atm^4 [W/m^2].
   * @param {number} atmosphericTempK - Effective atmospheric temperature in Kelvin (e.g. 180 K)
   * @param {number} [opticalDepth=0.3] - Column visible/IR dust optical depth tau (0.1 to 3.0)
   * @returns {{downwellingFluxW_M2: number, atmosphericEmissivity: number}}
   */
  static computeAtmosphericDownwellingRadiativeFlux(atmosphericTempK, opticalDepth = 0.3) {
    const T_atm = Math.max(1.0, atmosphericTempK);
    const tau = Math.max(0.01, opticalDepth);
    const sigma = 5.670374419e-8;

    const epsAtm = 1.0 - Math.exp(-tau);
    const fDown = epsAtm * sigma * Math.pow(T_atm, 4);

    return {
      downwellingFluxW_M2: parseFloat(fDown.toFixed(2)),
      atmosphericEmissivity: parseFloat(epsAtm.toFixed(4))
    };
  }

  /**
   * Calculate instantaneous net surface radiative energy balance F_net = F_solar + F_down - F_emit.
   * Positive means surface is warming, negative means surface is cooling.
   * @param {number} absorbedSolarFluxW_M2 - Absorbed insolation (1 - A) * S_inc in W/m^2
   * @param {number} surfaceTempK - Surface temperature in Kelvin
   * @param {number} [atmosphericDownwellingW_M2=0.0] - Downwelling atmospheric longwave flux in W/m^2
   * @param {number} [emissivity=0.95] - Surface emissivity
   * @returns {{netRadiativeFluxW_M2: number, emittedFluxW_M2: number, isWarming: boolean}}
   */
  static computeSurfaceNetRadiativeHeatBalance(absorbedSolarFluxW_M2, surfaceTempK, atmosphericDownwellingW_M2 = 0.0, emissivity = 0.95) {
    const fSolar = Math.max(0, absorbedSolarFluxW_M2);
    const fDown = Math.max(0, atmosphericDownwellingW_M2);
    const fEmit = KRCEngine.computeSurfaceThermalEmission(surfaceTempK, emissivity).emittedFluxW_M2;

    const fNet = fSolar + fDown - fEmit;

    return {
      netRadiativeFluxW_M2: parseFloat(fNet.toFixed(2)),
      emittedFluxW_M2: fEmit,
      isWarming: fNet > 0
    };
  }

  // --- Diurnal Thermal Skin Depth & Regolith Heat Storage Solvers ---

  /**
   * Calculate diurnal thermal wave penetration depth (skin depth) in meters.
   * d_skin = ( I * sqrt(P_sol) ) / ( sqrt(pi) * rho * c_p )
   * @param {number} thermalInertiaSI - Thermal inertia in J m^-2 K^-1 s^-1/2 (e.g. 50 to 800)
   * @param {number} [heatCapacityJ_KgK=800.0] - Specific heat capacity c_p in J/(kg K)
   * @param {number} [densityKg_M3=1500.0] - Bulk density in kg/m^3
   * @param {number} [periodSeconds=88775.244] - Diurnal period P in seconds (1 sol = 88775.244 s)
   * @returns {{skinDepthMeters: number, skinDepthCm: number, thermalConductivityW_mK: number}}
   */
  static computeDiurnalThermalSkinDepth(thermalInertiaSI, heatCapacityJ_KgK = 800.0, densityKg_M3 = 1500.0, periodSeconds = 88775.244) {
    const I = Math.max(1.0, thermalInertiaSI);
    const cp = Math.max(10.0, heatCapacityJ_KgK);
    const rho = Math.max(10.0, densityKg_M3);
    const P = Math.max(1.0, periodSeconds);

    // Thermal conductivity k = I^2 / (rho * cp)
    const k = (I * I) / (rho * cp);

    // Skin depth d = I * sqrt(P) / (sqrt(pi) * rho * cp) = sqrt(k * P / (pi * rho * cp))
    const dMeters = (I * Math.sqrt(P)) / (Math.sqrt(Math.PI) * rho * cp);
    const dCm = dMeters * 100.0;

    return {
      skinDepthMeters: parseFloat(dMeters.toFixed(4)),
      skinDepthCm: parseFloat(dCm.toFixed(2)),
      thermalConductivityW_mK: parseFloat(k.toFixed(5))
    };
  }

  /**
   * Calculate bulk regolith dry density from grain density and packing porosity.
   * rho_bulk = rho_grain * (1 - phi)
   * @param {number} [grainDensityKg_M3=3000.0] - Mineral grain density in kg/m^3 (basalt ~ 3000)
   * @param {number} [porosity=0.5] - Volume void fraction (0 to 0.9)
   * @returns {{bulkDensityKg_M3: number, voidRatio: number}}
   */
  static computeRegolithBulkDensity(grainDensityKg_M3 = 3000.0, porosity = 0.5) {
    const rhoG = Math.max(100.0, grainDensityKg_M3);
    const phi = Math.max(0.0, Math.min(0.9, porosity));

    const rhoBulk = rhoG * (1.0 - phi);
    const voidRatio = phi / (1.0 - phi);

    return {
      bulkDensityKg_M3: parseFloat(rhoBulk.toFixed(1)),
      voidRatio: parseFloat(voidRatio.toFixed(3))
    };
  }

  /**
   * Calculate sensible heat energy stored in subsurface layer per unit area (J/m^2).
   * Delta_Q = rho * c_p * Delta_z * Delta_T
   * @param {number} layerThicknessMeters - Layer thickness in meters
   * @param {number} deltaTK - Temperature change in Kelvin
   * @param {number} [densityKg_M3=1500.0] - Bulk density in kg/m^3
   * @param {number} [heatCapacityJ_KgK=800.0] - Specific heat capacity in J/(kg K)
   * @returns {{sensibleHeatJ_M2: number, volumetricHeatCapacityJ_M3K: number}}
   */
  static computeSubsurfaceSensibleHeatStorage(layerThicknessMeters, deltaTK, densityKg_M3 = 1500.0, heatCapacityJ_KgK = 800.0) {
    const dz = Math.max(0, layerThicknessMeters);
    const rho = Math.max(1.0, densityKg_M3);
    const cp = Math.max(1.0, heatCapacityJ_KgK);

    const cVol = rho * cp;
    const dq = cVol * dz * deltaTK;

    return {
      sensibleHeatJ_M2: parseFloat(dq.toFixed(2)),
      volumetricHeatCapacityJ_M3K: parseFloat(cVol.toFixed(1))
    };
  }

  // --- CO2 Frost Phase Change & Microscopic Thermal Transport Solvers ---

  /**
   * Calculate CO2 seasonal frost sublimation or condensation rate from surface net energy balance.
   * dm/dt = F_net / L_sub  (kg / (m^2 s))
   * dz/dt = (dm/dt) / rho_frost * 88775.244 * 1000  (mm / sol)
   * @param {number} netSurfaceFluxW_M2 - Net surface energy flux (positive = sublimation/melting, negative = condensation/freezing)
   * @param {number} [latentHeatJ_Kg=5.9e5] - Latent heat of CO2 sublimation in J/kg
   * @param {number} [frostDensityKg_M3=1500.0] - Density of CO2 frost deposit (slab ice ~1500 kg/m^3, fresh snow ~1000 kg/m^3)
   * @returns {{sublimationRateKg_M2S: number, thicknessRateMmPerSol: number, isSublimating: boolean, isCondensing: boolean}}
   */
  static computeCO2SublimationFrostMassRate(netSurfaceFluxW_M2, latentHeatJ_Kg = 5.9e5, frostDensityKg_M3 = 1500.0) {
    const lSub = Math.max(1e4, latentHeatJ_Kg);
    const rho = Math.max(100.0, frostDensityKg_M3);
    const marsSolSec = 88775.244;

    const dmDt = netSurfaceFluxW_M2 / lSub; // kg / (m^2 s)
    const dzDtMmSol = (dmDt / rho) * marsSolSec * 1000.0; // mm / sol

    return {
      sublimationRateKg_M2S: parseFloat(dmDt.toExponential(4)),
      thicknessRateMmPerSol: parseFloat(dzDtMmSol.toFixed(3)),
      isSublimating: netSurfaceFluxW_M2 > 0,
      isCondensing: netSurfaceFluxW_M2 < 0
    };
  }

  /**
   * Derive microscopic thermal conductivity (k) and thermal diffusivity (kappa) from bulk Thermal Inertia (I).
   * k = I^2 / ( rho * c_p )  (W / (m K))
   * kappa = k / ( rho * c_p ) = ( I / (rho * c_p) )^2  (m^2 / s)
   * @param {number} thermalInertia - Thermal inertia in SI units (J m^-2 K^-1 s^-1/2, typically 50 - 800 on Mars)
   * @param {number} [densityKg_M3=1500.0] - Bulk density in kg/m^3
   * @param {number} [heatCapacityJ_KgK=800.0] - Specific heat capacity in J/(kg K)
   * @returns {{thermalConductivityW_MK: number, thermalDiffusivityM2_S: number, volumetricHeatCapacityJ_M3K: number}}
   */
  static computeThermalConductivityAndDiffusivity(thermalInertia, densityKg_M3 = 1500.0, heatCapacityJ_KgK = 800.0) {
    const I = Math.max(1.0, thermalInertia);
    const rho = Math.max(1.0, densityKg_M3);
    const cp = Math.max(1.0, heatCapacityJ_KgK);

    const cVol = rho * cp;
    const k = (I * I) / cVol;
    const kappa = k / cVol;

    return {
      thermalConductivityW_MK: parseFloat(k.toFixed(5)),
      thermalDiffusivityM2_S: parseFloat(kappa.toExponential(4)),
      volumetricHeatCapacityJ_M3K: parseFloat(cVol.toFixed(1))
    };
  }

  // --- Planck Blackbody Radiance & Thermal Emission Solvers ---

  /**
   * Calculate Planck blackbody spectral radiance B_lambda(T) in W / (m^2 sr µm).
   * B_lambda = c1 / ( lambda^5 * ( exp( c2 / (lambda * T) ) - 1 ) )
   * c1 = 2 * h * c^2 = 1.191042972e8 W µm^4 / (m^2 sr)
   * c2 = h * c / k_B = 14387.77 µm K
   * @param {number} temperatureK - Temperature in Kelvin
   * @param {number} wavelengthMicrons - Spectral wavelength in microns (e.g. 12.0 µm for THEMIS night IR)
   * @returns {{spectralRadianceW_M2SrUm: number, wavelengthMicrons: number, temperatureK: number}}
   */
  static computePlanckSpectralRadiance(temperatureK, wavelengthMicrons) {
    const T = Math.max(1.0, temperatureK);
    const lam = Math.max(0.1, wavelengthMicrons);

    const c1 = 1.191042972e8; // W µm^4 / (m^2 sr)
    const c2 = 14387.77; // µm K

    const exponent = c2 / (lam * T);
    if (exponent > 700) {
      return { spectralRadianceW_M2SrUm: 0.0, wavelengthMicrons: lam, temperatureK: T };
    }

    const b = c1 / (Math.pow(lam, 5) * (Math.exp(exponent) - 1.0));

    return {
      spectralRadianceW_M2SrUm: parseFloat(b.toFixed(4)),
      wavelengthMicrons: parseFloat(lam.toFixed(3)),
      temperatureK: parseFloat(T.toFixed(2))
    };
  }

  /**
   * Invert Planck spectral radiance to calculate radiometric Brightness Temperature (T_b) in Kelvin.
   * T_b = c2 / ( lambda * ln( 1 + c1 / (lambda^5 * B_lambda) ) )
   * @param {number} radianceW_M2SrUm - Spectral radiance in W / (m^2 sr µm)
   * @param {number} wavelengthMicrons - Spectral wavelength in microns
   * @returns {{brightnessTemperatureK: number, wavelengthMicrons: number}}
   */
  static computePlanckBrightnessTemperature(radianceW_M2SrUm, wavelengthMicrons) {
    const b = Math.max(1e-12, radianceW_M2SrUm);
    const lam = Math.max(0.1, wavelengthMicrons);

    const c1 = 1.191042972e8;
    const c2 = 14387.77;

    const denom = Math.log(1.0 + c1 / (Math.pow(lam, 5) * b));
    const tb = c2 / (lam * denom);

    return {
      brightnessTemperatureK: parseFloat(tb.toFixed(2)),
      wavelengthMicrons: parseFloat(lam.toFixed(3))
    };
  }

  /**
   * Calculate peak thermal emission wavelength via Wien's Displacement Law.
   * lambda_max = 2897.77 / T (microns)
   * @param {number} temperatureK - Temperature in Kelvin
   * @returns {{peakWavelengthMicrons: number, temperatureK: number}}
   */
  static computeWienPeakWavelength(temperatureK) {
    const T = Math.max(1.0, temperatureK);
    const lamPeak = 2897.77 / T;

    return {
      peakWavelengthMicrons: parseFloat(lamPeak.toFixed(3)),
      temperatureK: parseFloat(T.toFixed(2))
    };
  }

  // --- Subsurface Thermal Diffusion & Skin Depth Solvers ---

  /**
   * Calculate diurnal and annual thermal skin depth (penetration depth) and bulk thermal conductivity on Mars.
   * d_s = ( I / (rho * c_p) ) * sqrt( P_sol / pi )
   * d_annual = d_s * sqrt( N_sols_year )  [N_sols_year = 668.6 on Mars]
   * k = I^2 / (rho * c_p)  (W / (m K))
   * @param {number} thermalInertiaTiu - Thermal inertia in tiu (J m^-2 K^-1 s^-1/2) (typically 50-800 on Mars)
   * @param {number} [bulkDensityKg_M3=1500.0] - Regolith bulk density in kg/m^3
   * @param {number} [heatCapacityJ_KgK=800.0] - Specific heat capacity in J / (kg K)
   * @returns {{diurnalSkinDepthMeters: number, diurnalSkinDepthCm: number, annualSkinDepthMeters: number, thermalConductivityW_MK: number}}
   */
  static computeDiurnalAndAnnualSkinDepth(thermalInertiaTiu, bulkDensityKg_M3 = 1500.0, heatCapacityJ_KgK = 800.0) {
    const I = Math.max(1.0, thermalInertiaTiu);
    const rho = Math.max(100.0, bulkDensityKg_M3);
    const cp = Math.max(100.0, heatCapacityJ_KgK);

    const Psol = 88775.244; // seconds in 1 Martian sol
    const rhoCp = rho * cp;

    const ds = (I / rhoCp) * Math.sqrt(Psol / Math.PI);
    const dsCm = ds * 100.0;
    const dAnnual = ds * Math.sqrt(668.5991);
    const k = (I * I) / rhoCp;

    return {
      diurnalSkinDepthMeters: parseFloat(ds.toFixed(4)),
      diurnalSkinDepthCm: parseFloat(dsCm.toFixed(2)),
      annualSkinDepthMeters: parseFloat(dAnnual.toFixed(3)),
      thermalConductivityW_MK: parseFloat(k.toFixed(5))
    };
  }

  /**
   * Calculate damped diurnal subsurface temperature amplitude and phase lag at physical depth z.
   * Delta_T(z) = Delta_T_0 * exp( -z / d_s )
   * Lag = ( z / d_s ) * ( P_sol / (2*pi) ) (hours)
   * @param {number} surfaceAmplitudeK - Peak-to-peak surface diurnal temperature swing (Delta T_0) in Kelvin
   * @param {number} depthMeters - Subsurface physical depth z in meters
   * @param {number} skinDepthMeters - Diurnal thermal skin depth d_s in meters
   * @returns {{dampedAmplitudeK: number, phaseLagHours: number, phaseLagRadians: number}}
   */
  static computeSubsurfaceTemperatureDampingAndLag(surfaceAmplitudeK, depthMeters, skinDepthMeters) {
    const deltaT0 = Math.max(0.0, surfaceAmplitudeK);
    const z = Math.max(0.0, depthMeters);
    const ds = Math.max(1e-4, skinDepthMeters);

    const zRatio = z / ds;
    const dampedT = deltaT0 * Math.exp(-zRatio);
    const lagRad = zRatio;
    const lagHours = zRatio * (24.65979 / (2.0 * Math.PI)); // 24.65979 hours per sol

    return {
      dampedAmplitudeK: parseFloat(dampedT.toFixed(2)),
      phaseLagHours: parseFloat(lagHours.toFixed(2)),
      phaseLagRadians: parseFloat(lagRad.toFixed(3))
    };
  }

  // --- Rock Fraction Thermal Mixing & Sloped KRC Direct Insolation ---

  /**
   * Calculate Christensen (1986) non-linear two-component apparent thermal inertia for rock/fines sub-pixel mixtures.
   * I_apparent = [ (1 - f_rock) * I_fines^(3/4) + f_rock * I_rock^(3/4) ]^(4/3)
   * @param {number} fineThermalInertiaTiu - Thermal inertia of fine regolith (tiu) (e.g. 50 - 300)
   * @param {number} [rockThermalInertiaTiu=2200.0] - Thermal inertia of rocks/boulders (tiu) (e.g. 2000 - 2500)
   * @param {number} [rockAbundanceFraction=0.10] - Areal rock fraction (0.0 to 1.0)
   * @returns {{apparentThermalInertiaTiu: number, linearWeightedInertiaTiu: number, rockFractionPercent: number}}
   */
  static computeTwoComponentApparentThermalInertia(fineThermalInertiaTiu, rockThermalInertiaTiu = 2200.0, rockAbundanceFraction = 0.10) {
    const If = Math.max(1.0, fineThermalInertiaTiu);
    const Ir = Math.max(1.0, rockThermalInertiaTiu);
    const fRock = Math.max(0.0, Math.min(1.0, rockAbundanceFraction));
    const fFine = 1.0 - fRock;

    // 3/4 power law mixing from Planck radiance integration (Christensen 1986 / Nowicki & Christensen 2007)
    const term = fFine * Math.pow(If, 0.75) + fRock * Math.pow(Ir, 0.75);
    const Iapp = Math.pow(term, 4.0 / 3.0);

    const Ilin = fFine * If + fRock * Ir;

    return {
      apparentThermalInertiaTiu: parseFloat(Iapp.toFixed(1)),
      linearWeightedInertiaTiu: parseFloat(Ilin.toFixed(1)),
      rockFractionPercent: parseFloat((fRock * 100.0).toFixed(1))
    };
  }

  /**
   * Calculate direct KRC surface solar insolation factoring in slope orientation, distance, and dust extinction.
   * F_direct = (S_0 / r_sun^2) * cos(i_slope) * exp( -tau / cos(theta_z) )
   * @param {number} solarZenithDeg - Solar zenith angle in degrees (0 - 90)
   * @param {number} solarAzimuthDeg - Solar azimuth angle in degrees (0 - 360)
   * @param {number} slopeDeg - Surface terrain slope in degrees
   * @param {number} aspectDeg - Surface terrain aspect in degrees (0 - 360)
   * @param {number} [heliocentricDistanceAU=1.524] - Mars heliocentric distance in AU
   * @param {number} [opticalDepthTau=0.20] - Column dust optical depth tau
   * @returns {{directInsolationW_M2: number, topOfAtmosphereFluxW_M2: number, cosSlopeIncidence: number}}
   */
  static computeSlopeCorrectedDirectInsolation(solarZenithDeg, solarAzimuthDeg, slopeDeg, aspectDeg, heliocentricDistanceAU = 1.524, opticalDepthTau = 0.20) {
    const r = Math.max(0.5, heliocentricDistanceAU);
    const tau = Math.max(0.0, opticalDepthTau);

    const sToa = 1361.0 / (r * r);

    const zRad = (Math.min(90.0, Math.max(0.0, solarZenithDeg)) * Math.PI) / 180.0;
    const sRad = (Math.min(90.0, Math.max(0.0, slopeDeg)) * Math.PI) / 180.0;
    const dAzRad = ((solarAzimuthDeg - aspectDeg) * Math.PI) / 180.0;

    const cosZ = Math.cos(zRad);
    const cosI = Math.max(0.0, cosZ * Math.cos(sRad) + Math.sin(zRad) * Math.sin(sRad) * Math.cos(dAzRad));

    const airmass = 1.0 / Math.max(0.05, cosZ);
    const transmission = Math.exp(-tau * airmass);

    const flux = sToa * cosI * transmission;

    return {
      directInsolationW_M2: parseFloat(flux.toFixed(2)),
      topOfAtmosphereFluxW_M2: parseFloat(sToa.toFixed(2)),
      cosSlopeIncidence: parseFloat(cosI.toFixed(4))
    };
  }

  // --- Subsurface Thermal Wave & Surface Heat Balance Solvers ---

  /**
   * Calculate 1D subsurface damped temperature wave and phase lag at depth z.
   * T(z, t) = T_mean + DeltaT * exp(-z / z_skin) * cos(2*pi*t - z / z_skin)
   * @param {number} depthMeters - Subsurface depth z in meters
   * @param {number} [meanTemperatureK=210.0] - Diurnal mean temperature in Kelvin
   * @param {number} [surfaceAmplitudeK=45.0] - Diurnal surface temperature amplitude DeltaT in Kelvin
   * @param {number} [skinDepthMeters=0.05] - Diurnal thermal skin depth z_skin in meters
   * @param {number} [solFraction=0.0] - Time as fraction of sol (0.0 = solar noon peak, 0.5 = midnight)
   * @returns {{temperatureK: number, amplitudeDamping: number, phaseLagHours: number, localAmplitudeK: number}}
   */
  static computeSubsurface1DTemperatureProfile(depthMeters, meanTemperatureK = 210.0, surfaceAmplitudeK = 45.0, skinDepthMeters = 0.05, solFraction = 0.0) {
    const z = Math.max(0.0, depthMeters);
    const zSkin = Math.max(1e-4, skinDepthMeters);
    const tFrac = solFraction % 1.0;

    const zNorm = z / zSkin;
    const damping = Math.exp(-zNorm);
    const phaseRad = 2.0 * Math.PI * tFrac - zNorm;

    const temp = meanTemperatureK + surfaceAmplitudeK * damping * Math.cos(phaseRad);
    const phaseLagHours = (zNorm / (2.0 * Math.PI)) * 24.6597;

    return {
      temperatureK: parseFloat(temp.toFixed(2)),
      amplitudeDamping: parseFloat(damping.toFixed(4)),
      phaseLagHours: parseFloat(phaseLagHours.toFixed(2)),
      localAmplitudeK: parseFloat((surfaceAmplitudeK * damping).toFixed(2))
    };
  }

  /**
   * Calculate KRC instantaneous surface equilibrium temperature from net absorbed insolation and conductive flux.
   * T_surf = [ ( (1 - A) * F_solar - F_cond ) / ( epsilon * sigma ) ]^(1/4)
   * @param {number} [albedo=0.25] - Surface albedo A
   * @param {number} [emissivity=0.95] - Thermal IR emissivity epsilon
   * @param {number} [insolationW_M2=450.0] - Incident solar flux F_solar in W/m^2
   * @param {number} [subsurfaceConductiveFluxW_M2=0.0] - Conductive heat loss into subsurface F_cond in W/m^2
   * @returns {{surfaceTemperatureK: number, absorbedSolarFluxW_M2: number, netEmittedRadiativeFluxW_M2: number}}
   */
  static computeKRCRadiativeSurfaceEquilibriumIterative(albedo = 0.25, emissivity = 0.95, insolationW_M2 = 450.0, subsurfaceConductiveFluxW_M2 = 0.0) {
    const A = Math.max(0.0, Math.min(0.99, albedo));
    const eps = Math.max(0.1, Math.min(1.0, emissivity));
    const fSolar = Math.max(0.0, insolationW_M2);
    const fCond = subsurfaceConductiveFluxW_M2;

    const sigma = 5.670374419e-8;

    const absorbed = (1.0 - A) * fSolar;
    const netRad = Math.max(1e-4, absorbed - fCond);

    const tSurf = Math.pow(netRad / (eps * sigma), 0.25);

    return {
      surfaceTemperatureK: parseFloat(tSurf.toFixed(2)),
      absorbedSolarFluxW_M2: parseFloat(absorbed.toFixed(2)),
      netEmittedRadiativeFluxW_M2: parseFloat(netRad.toFixed(2))
    };
  }

  // --- CO2 Condensation Frost Point & Polar Cold Trap Solvers ---

  /**
   * Calculate CO2 saturation vapor condensation frost point temperature (Clausius-Clapeyron relation).
   * T_frost = -3148.0 / ( ln(P_mbar / 1000.0) - 23.102 )
   * @param {number} [surfacePressureMbar=6.1] - Ambient Martian atmospheric surface pressure in mbar
   * @returns {{frostPointK: number, frostPointC: number, surfacePressureMbar: number, isSummitVacuum: boolean}}
   */
  static computeCO2CondensationFrostPoint(surfacePressureMbar = 6.1) {
    const P = Math.max(0.01, surfacePressureMbar);
    const pPa = P * 100.0; // convert mbar to Pascals (Pa)

    // Clausius-Clapeyron for CO2 (Forget et al. 1998; Kieffer 2013): ln(P_Pa) = 27.60 - 3148.3 / T_frost
    const tFrostK = 3148.3 / (27.60 - Math.log(pPa));
    const tFrostC = tFrostK - 273.15;

    return {
      frostPointK: parseFloat(tFrostK.toFixed(2)),
      frostPointC: parseFloat(tFrostC.toFixed(2)),
      surfacePressureMbar: parseFloat(P.toFixed(2)),
      isSummitVacuum: P < 1.0
    };
  }

  /**
   * Calculate polar dry ice (CO2 frost) mass condensation rate and daily layer thickness accumulation.
   * dm/dt = F_net_deficit / L_sub * Sol_seconds
   * dz/dt = (dm/dt) / rho_frost
   * @param {number} netDeficitFluxWm2 - Net radiative cooling deficit in W/m^2 (e.g. 25.0 W/m^2 during polar night)
   * @param {number} [latentHeatJouleKg=5.9e5] - Latent heat of sublimation L_sub in J/kg (5.9e5 J/kg for dry ice)
   * @param {number} [frostDensityKgM3=1500.0] - Dry ice density in kg/m^3 (1500 kg/m^3 slab ice, 1000 kg/m^3 granular)
   * @returns {{frostAccumulationKgPerM2PerSol: number, frostGrowthMmPerSol: number, isCondensing: boolean}}
   */
  static computePolarFrostCondensationRate(netDeficitFluxWm2, latentHeatJouleKg = 5.9e5, frostDensityKgM3 = 1500.0) {
    const fNet = Math.max(0.0, netDeficitFluxWm2);
    const lSub = Math.max(1e3, latentHeatJouleKg);
    const rho = Math.max(100.0, frostDensityKgM3);

    const solSec = 88775.244; // seconds per Martian sol
    const massAccumKgSol = (fNet / lSub) * solSec;
    const thicknessGrowthMmSol = (massAccumKgSol / rho) * 1000.0;

    return {
      frostAccumulationKgPerM2PerSol: parseFloat(massAccumKgSol.toFixed(3)),
      frostGrowthMmPerSol: parseFloat(thicknessGrowthMmSol.toFixed(3)),
      isCondensing: fNet > 0.0
    };
  }

  /**
   * Calculate diurnal and annual thermal skin depths and subsurface temperature wave damping.
   * d_diurnal = ( I / (rho * c_p) ) * sqrt( P_sol / pi )
   * d_annual = d_diurnal * sqrt( P_year_sols )
   * Delta_T(z) = Delta_T_surface * exp( -z / d )
   * Phase_lag_hours = ( z / d ) * ( P_sol_hours / 2*pi )
   * Reference: Kieffer (2013), Mellon et al. (2000, 2008).
   * @param {number} thermalInertia - Thermal inertia I in J m^-2 K^-1 s^-1/2 (e.g. 50 dust, 300 sand, 2000 bedrock)
   * @param {number} [surfaceTempAmplitudeK=50.0] - Surface diurnal temperature swing half-amplitude Delta_T_0 in K
   * @param {number} [targetDepthMeters=0.10] - Subsurface measurement depth z in meters
   * @param {number} [densityKgM3=1500.0] - Regolith bulk density rho in kg/m^3
   * @param {number} [specificHeatJouleKgK=850.0] - Specific heat capacity c_p in J/(kg*K)
   * @returns {{diurnalSkinDepthCm: number, annualSkinDepthMeters: number, dampedTempAmplitudeK: number, phaseLagHours: number, amplitudeDampingRatio: number}}
   */
  static computeThermalSkinDepthAndHarmonicDamping(thermalInertia, surfaceTempAmplitudeK = 50.0, targetDepthMeters = 0.10, densityKgM3 = 1500.0, specificHeatJouleKgK = 850.0) {
    const I = Math.max(10.0, thermalInertia);
    const rho = Math.max(100.0, densityKgM3);
    const cp = Math.max(100.0, specificHeatJouleKgK);
    const deltaT0 = Math.max(0.0, surfaceTempAmplitudeK);
    const z = Math.max(0.0, targetDepthMeters);

    const solSeconds = 88775.244;
    const solHours = 24.65979;
    const martianYearSols = 668.599;

    // Volumetric heat capacity C_v = rho * c_p
    const volumetricHeatCapacity = rho * cp;
    // Thermal conductivity k = I^2 / (rho * c_p)
    // d_diurnal = sqrt( (k * P_sol) / (pi * rho * c_p) ) = ( I / (rho * c_p) ) * sqrt( P_sol / pi )
    const dDiurnalM = (I / volumetricHeatCapacity) * Math.sqrt(solSeconds / Math.PI);
    const dAnnualM = dDiurnalM * Math.sqrt(martianYearSols);

    const decayRatio = Math.exp(-z / dDiurnalM);
    const dampedAmpK = deltaT0 * decayRatio;
    const phaseLagHours = (z / dDiurnalM) * (solHours / (2.0 * Math.PI));

    return {
      diurnalSkinDepthCm: parseFloat((dDiurnalM * 100.0).toFixed(2)),
      annualSkinDepthMeters: parseFloat(dAnnualM.toFixed(3)),
      dampedTempAmplitudeK: parseFloat(dampedAmpK.toFixed(2)),
      phaseLagHours: parseFloat(phaseLagHours.toFixed(2)),
      amplitudeDampingRatio: parseFloat(decayRatio.toFixed(4))
    };
  }

  /**
   * Calculate planetary lithospheric flexural rigidity D, flexural parameter alpha, and estimated basal heat flow q.
   * D = ( E * T_e^3 ) / ( 12 * (1 - nu^2) )
   * alpha = ( (4 * D) / ( (rho_mantle - rho_infill) * g ) )^(1/4)
   * lambda_flexure = 2 * pi * alpha
   * q_basal = k_crust * ( (T_ductile - T_surface) / T_e )
   * Reference: Turcotte & Schubert (2014), McGovern et al. (2002, 2004) for Martian volcanic provinces.
   * @param {number} elasticThicknessKm - Effective elastic lithosphere thickness T_e in km (e.g. 15 km Noachian basins, 50-100 km Tharsis/Olympus Mons)
   * @param {number} [youngsModulusGPa=100.0] - Young's modulus E in GPa (typically 100 GPa for basaltic lithosphere)
   * @param {number} [poissonsRatio=0.25] - Poisson's ratio nu (typically 0.25)
   * @param {number} [crustThermalCondWmK=3.0] - Crustal thermal conductivity k in W/(m*K)
   * @returns {{flexuralRigidityNewtonMeters: number, flexuralParameterKm: number, flexuralWavelengthKm: number, basalHeatFlowMilliWattsM2: number, elasticThicknessKm: number}}
   */
  static computeLithosphericFlexuralRigidityAndElasticThickness(elasticThicknessKm, youngsModulusGPa = 100.0, poissonsRatio = 0.25, crustThermalCondWmK = 3.0) {
    const TeKm = Math.max(1.0, elasticThicknessKm);
    const TeM = TeKm * 1000.0; // meters
    const E = Math.max(1.0, youngsModulusGPa) * 1e9; // Pa (N/m^2)
    const nu = Math.min(0.49, Math.max(0.01, poissonsRatio));
    const k = Math.max(0.1, crustThermalCondWmK);

    const gMars = 3.72076; // m/s^2
    const rhoMantle = 3500.0; // kg/m^3
    const rhoInfill = 2900.0; // kg/m^3 (basaltic construct load)
    const deltaRho = rhoMantle - rhoInfill;

    // Flexural rigidity D (N*m)
    const D = (E * Math.pow(TeM, 3.0)) / (12.0 * (1.0 - nu * nu));

    // Flexural parameter alpha (meters)
    const alphaM = Math.pow((4.0 * D) / (deltaRho * gMars), 0.25);
    const alphaKm = alphaM / 1000.0;
    const lambdaKm = 2.0 * Math.PI * alphaKm;

    // Heat flow q = k * (Delta_T / T_e), with base of mechanical lithosphere isotherm ~ 600 C (873 K) vs mean surface ~ 215 K
    const deltaTK = 600.0; // K difference
    const qWattsM2 = k * (deltaTK / TeM);
    const qMilliWattsM2 = qWattsM2 * 1000.0;

    return {
      flexuralRigidityNewtonMeters: parseFloat(D.toExponential(4)),
      flexuralParameterKm: parseFloat(alphaKm.toFixed(2)),
      flexuralWavelengthKm: parseFloat(lambdaKm.toFixed(2)),
      basalHeatFlowMilliWattsM2: parseFloat(qMilliWattsM2.toFixed(2)),
      elasticThicknessKm: parseFloat(TeKm.toFixed(1))
    };
  }

  /**
   * Invert effective regolith particle grain size (microns/mm) and geological unit classification from thermal inertia.
   * Based on laboratory thermal conductivity experiments in CO2 gas at Martian ambient pressures.
   * k = I^2 / (rho * c_p)
   * d_eff ~ ( (k - k_rad) / (C * (P/100)^0.60) )^(2.5)
   * Reference: Presley & Christensen (1997), Kieffer (2013), Edwards et al. (2018) for THEMIS / TES.
   * @param {number} thermalInertia - Thermal inertia I in J m^-2 K^-1 s^-1/2 (e.g. 50 to 2200)
   * @param {number} [surfacePressurePa=610.0] - Surface pressure in Pascals
   * @returns {{effectiveGrainSizeMicrons: number, grainClass: string, regolithTexture: string, thermalConductivityWmK: number}}
   */
  static computeThermalInertiaEffectiveGrainSize(thermalInertia, surfacePressurePa = 610.0) {
    const I = Math.max(10.0, thermalInertia);
    const P = Math.max(10.0, surfacePressurePa);
    const rho = 1500.0; // kg/m^3
    const cp = 850.0;   // J/kg/K

    // Apparent bulk thermal conductivity k = I^2 / (rho * cp)
    const k = (I * I) / (rho * cp);

    let dMicrons = 10.0;
    let grainClass = 'Airborne / Settled Silicate Dust';
    let texture = 'Fine unconsolidated aeolian dust mantle';

    if (I < 120.0) {
      dMicrons = Math.max(2.0, Math.min(39.0, Math.pow(I / 50.0, 2.0) * 10.0));
      grainClass = 'Fine Airborne Dust';
      texture = 'Unconsolidated bright dust mantle (e.g. Tharsis / Arabia Terra)';
    } else if (I < 350.0) {
      dMicrons = 40.0 + ((I - 120.0) / 230.0) * 210.0; // 40 - 250 um
      grainClass = 'Fine to Medium Active Sand';
      texture = 'Saltating basaltic sand dunes and ripples (e.g. Nili Patera / Gale)';
    } else if (I < 700.0) {
      dMicrons = 250.0 + ((I - 350.0) / 350.0) * 1750.0; // 250 um - 2 mm
      grainClass = 'Coarse Sand / Granules / Duricrust';
      texture = 'Indurated soil, pebble pavement, or cemented duricrust';
    } else if (I < 1200.0) {
      dMicrons = 2000.0 + ((I - 700.0) / 500.0) * 20000.0; // 2 mm - 2.2 cm
      grainClass = 'Pebbles / Rocky Rubble / Fractured Outcrop';
      texture = 'High rock abundance field or partially exposed bedrock strata';
    } else {
      dMicrons = 100000.0; // > 10 cm
      grainClass = 'Dense Continuous Bedrock / Cohesive Basalt';
      texture = 'Solid volcanic lava flows or exposed crater central peaks';
    }

    return {
      effectiveGrainSizeMicrons: parseFloat(dMicrons.toFixed(1)),
      grainClass,
      regolithTexture: texture,
      thermalConductivityWmK: parseFloat(k.toFixed(5))
    };
  }

  /**
   * Calculate subsurface permafrost ground ice stability critical temperature and depth z_ice.
   * P_sat(T) = exp( 28.868 - 6132.9 / T ) (Pa)
   * T_crit = 6132.9 / ( 28.868 - ln(P_atm_H2O) )
   * Reference: Mellon & Phillips (2001), Mellon et al. (2004), Bandfield (2007), Smith (2008) for Phoenix Lander.
   * @param {number} meanAnnualSurfaceTempK - Mean annual surface temperature T_annual in Kelvin (e.g. 175 K polar, 215 K mid-lat, 240 K equatorial)
   * @param {number} [atmosphericWaterVaporPrUm=10.0] - Atmospheric water vapor column in precipitable microns (typically 5 to 30 pr-um)
   * @param {number} [thermalInertia=250.0] - Thermal inertia of dry overburden mantle
   * @returns {{isGroundIceStable: boolean, criticalStabilityTempK: number, waterVaporPartialPressurePa: number, groundIceDepthCm: number, stabilityZone: string}}
   */
  static computePermafrostGroundIceStabilityDepth(meanAnnualSurfaceTempK, atmosphericWaterVaporPrUm = 10.0, thermalInertia = 250.0) {
    const T = Math.max(100.0, Math.min(300.0, meanAnnualSurfaceTempK));
    const prUm = Math.max(0.1, atmosphericWaterVaporPrUm);
    const I = Math.max(20.0, thermalInertia);

    // Water vapor partial pressure P_H2O (Pa) ~ pr_um * 1e-6 * 1000 kg/m3 * 3.72 m/s2
    const pH2OPa = prUm * 0.00372076;

    // Critical ice stability temperature T_crit
    const lnP = Math.log(Math.max(1e-7, pH2OPa));
    const tCritK = 6132.9 / (28.868 - lnP);

    const isStable = T <= (tCritK + 2.0); // account for subsurface annual dampening

    let zIceCm = 200.0;
    let zone = 'Desiccated / Unstable in Upper Regolith (Equatorial/Tropical)';

    if (T <= tCritK - 15.0) {
      // High-latitude / Polar region (Phoenix Lander regime): 2-10 cm depth
      zIceCm = Math.max(2.0, (T - 160.0) * 0.40);
      zone = 'Shallow Stable Permafrost / Massive Ground Ice (< 10 cm)';
    } else if (T <= tCritK) {
      // Mid-latitude lobate debris apron / lineated valley fill regime: 15-80 cm
      zIceCm = 10.0 + ((T - (tCritK - 15.0)) / 15.0) * 70.0;
      zone = 'Deep Stable Permafrost (10 - 80 cm mantle)';
    } else if (T <= tCritK + 10.0) {
      zIceCm = 80.0 + ((T - tCritK) / 10.0) * 100.0;
      zone = 'Marginal / Metastable Ice at Multi-Meter Depth';
    }

    return {
      isGroundIceStable: isStable,
      criticalStabilityTempK: parseFloat(tCritK.toFixed(2)),
      waterVaporPartialPressurePa: parseFloat(pH2OPa.toExponential(4)),
      groundIceDepthCm: parseFloat(zIceCm.toFixed(1)),
      stabilityZone: zone
    };
  }

  /**
   * Calculate dual-component sub-pixel heterogeneous thermal inertia mixture (fine regolith + exposed rocks).
   * Reference: Christensen (1986), Putzig & Mellon (2007), Edwards et al. (2009) for THEMIS rock abundance.
   * @param {number} fineInertia - Thermal inertia of fine regolith component (e.g. 70 for dust, 250 for sand)
   * @param {number} [rockInertia=1800.0] - Thermal inertia of solid bedrock / boulders (e.g. 1500 to 2200)
   * @param {number} [rockFractionPct=15.0] - Fractional areal rock abundance f_rock in percent (0 to 100)
   * @returns {{rockFractionPct: number, fineFractionPct: number, apparentNightThermalInertia: number, apparentDayThermalInertia: number, thermalInertiaContrast: number, dominantRegime: string}}
   */
  static computeDualComponentThermalInertiaMix(fineInertia, rockInertia = 1800.0, rockFractionPct = 15.0) {
    const Ifine = Math.max(20.0, fineInertia);
    const Irock = Math.max(Ifine, rockInertia);
    const fRock = Math.min(100.0, Math.max(0.0, rockFractionPct)) / 100.0;
    const fFine = 1.0 - fRock;

    // Linear day-time apparent inertia
    const Iday = fFine * Ifine + fRock * Irock;

    // Non-linear night-time radiance bias (T^4 weighting amplifies warm rock emission)
    const Inight = fFine * Ifine + Math.pow(fRock, 0.70) * (Irock - Ifine);

    const contrast = Math.abs(Inight - Iday);

    let regime = 'Fine Regolith Dominated (Dust/Sand Mantle)';
    if (fRock >= 0.35) {
      regime = 'Rock/Bedrock Dominated Outcrop';
    } else if (fRock >= 0.10) {
      regime = 'Rocky Soil / High Bolder Concentration (Viking 2 / InSight Type)';
    }

    return {
      rockFractionPct: parseFloat((fRock * 100.0).toFixed(1)),
      fineFractionPct: parseFloat((fFine * 100.0).toFixed(1)),
      apparentNightThermalInertia: parseFloat(Inight.toFixed(1)),
      apparentDayThermalInertia: parseFloat(Iday.toFixed(1)),
      thermalInertiaContrast: parseFloat(contrast.toFixed(1)),
      dominantRegime: regime
    };
  }

  /**
   * Calculate transient surface frost (CO2 dry ice vs H2O water ice) condensation temperature, latent heat budget, and daily accumulation.
   * dm/dt = Q_deficit / L_sub
   * Reference: Kieffer (1979), Titus et al. (2003), Schorghofer & Aharonson (2005).
   * @param {number} surfaceTempK - Instantaneous nocturnal surface skin temperature in Kelvin
   * @param {number} [atmosphericPressurePa=610.0] - Ambient surface atmospheric pressure in Pa (610 Pa ~ 6.1 mbar)
   * @param {string} [frostType='co2'] - Volatile species ('co2' for dry ice or 'h2o' for water frost)
   * @param {number} [netRadiativeDeficitWm2=25.0] - Net radiative cooling deficit in W/m^2
   * @returns {{isCondensing: boolean, condensationTempK: number, condensationRateKgM2S: number, dailyAccumulationMicrons: number, latentHeatOfSublimationJkg: number, volatileSpecies: string}}
   */
  static computeTransientFrostCondensationBudget(surfaceTempK, atmosphericPressurePa = 610.0, frostType = 'co2', netRadiativeDeficitWm2 = 25.0) {
    const P = Math.max(10.0, atmosphericPressurePa);
    const Q = Math.max(0.0, netRadiativeDeficitWm2);
    const isCO2 = frostType.toLowerCase() === 'co2';

    let tCondK = 147.3;
    let Lsub = 5.9e5; // J/kg for CO2
    let rhoIce = 1600.0; // kg/m^3

    if (isCO2) {
      // CO2 Clausius-Clapeyron: ln(P_Pa) = 28.02 - 3182.48 / T (T ~ 147.3 K at 610 Pa)
      tCondK = 3182.48 / (28.02 - Math.log(P));
      Lsub = 5.9e5;
      rhoIce = 1600.0;
    } else {
      // H2O water frost (assuming ~10 pr-um water vapor -> P_H2O ~ 0.0372 Pa)
      const pH2O = 0.0372;
      tCondK = 6132.9 / (28.868 - Math.log(pH2O)); // ~193.1 K
      Lsub = 2.83e6;
      rhoIce = 920.0;
    }

    const isCondensing = surfaceTempK <= (tCondK + 0.1);

    let dmdt = 0.0;
    let accumMicrons = 0.0;

    if (isCondensing) {
      dmdt = Q / Lsub; // kg / (m^2 * s)
      // Accumulation over a 12-hour (43,200 s) night:
      const massPerSol = dmdt * 43200.0; // kg / m^2
      const depthMeters = massPerSol / rhoIce;
      accumMicrons = depthMeters * 1e6; // microns
    }

    return {
      isCondensing,
      condensationTempK: parseFloat(tCondK.toFixed(2)),
      condensationRateKgM2S: parseFloat(dmdt.toExponential(4)),
      dailyAccumulationMicrons: parseFloat(accumMicrons.toFixed(2)),
      latentHeatOfSublimationJkg: Lsub,
      volatileSpecies: isCO2 ? 'Carbon Dioxide (CO2 Dry Ice)' : 'Water Ice (H2O Frost)'
    };
  }

  /**
   * Calculate 1D crustal geothermal temperature profile, Moho basal boundary temperature, and liquid water melting isotherm depth.
   * T(z) = T_surf + ( q_base / k ) * z - ( H_radio / (2 * k) ) * z^2
   * Reference: Plesa et al. (2018), Smrekar et al. (2019) InSight HP3, Orosei et al. (2018) for SPLD basal lake.
   * @param {number} surfaceTempK - Mean annual surface temperature in Kelvin (e.g. 160 K polar, 215 K equatorial)
   * @param {number} [crustalThicknessKm=40.0] - Crustal Moho depth in km (20 to 80 km)
   * @param {number} [geothermalHeatFluxMwm2=25.0] - Surface geothermal heat flux in mW/m^2 (15 to 45 mW/m^2)
   * @param {number} [crustalThermalConductivityWmK=2.0] - Bulk crustal rock thermal conductivity in W/(m*K)
   * @param {number} [radiogenicHeatGenerationWPerM3=2.5e-10] - Crustal volumetric radioactive heat production rate in W/m^3
   * @returns {{mohoTemperatureK: number, mohoTemperatureC: number, depthToWaterMeltingKm: number, thermalGradientKPerKm: number, isBasalMeltingPossible: boolean}}
   */
  static computeLithosphericGeothermalBasalTemperature(surfaceTempK, crustalThicknessKm = 40.0, geothermalHeatFluxMwm2 = 25.0, crustalThermalConductivityWmK = 2.0, radiogenicHeatGenerationWPerM3 = 2.5e-10) {
    const Tsurf = Math.max(50.0, surfaceTempK);
    const D = Math.max(1.0, crustalThicknessKm) * 1000.0; // meters
    const qBase = Math.max(1.0, geothermalHeatFluxMwm2) * 1e-3; // W/m^2
    const k = Math.max(0.1, crustalThermalConductivityWmK);
    const H = Math.max(0.0, radiogenicHeatGenerationWPerM3);

    // Moho temperature
    const TMohoK = Tsurf + (qBase / k) * D - (H / (2.0 * k)) * D * D;
    const TMohoC = TMohoK - 273.15;

    // Linear thermal gradient at surface (K/km)
    const gradKPerKm = (qBase / k) * 1000.0;

    // Depth to 273.15 K (0 C) water melting isotherm (m)
    let zMeltKm = 100.0;
    if (273.15 >= Tsurf) {
      const deltaT = 273.15 - Tsurf;
      const zMeltM = (deltaT * k) / qBase;
      zMeltKm = zMeltM / 1000.0;
    } else {
      zMeltKm = 0.0;
    }

    const isBasalMelting = TMohoK >= 273.15;

    return {
      mohoTemperatureK: parseFloat(TMohoK.toFixed(1)),
      mohoTemperatureC: parseFloat(TMohoC.toFixed(1)),
      depthToWaterMeltingKm: parseFloat(zMeltKm.toFixed(2)),
      thermalGradientKPerKm: parseFloat(gradKPerKm.toFixed(2)),
      isBasalMeltingPossible: isBasalMelting
    };
  }
}

















