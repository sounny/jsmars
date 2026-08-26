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
}














