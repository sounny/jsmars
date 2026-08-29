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

  /**
   * Calculate multi-harmonic subsurface thermal skin depth spectrum across diurnal, seasonal, and obliquity Milankovitch cycles.
   * d_th = ( I / (rho * c_p) ) * sqrt( P / pi )
   * Reference: Kieffer (2013), Mellon et al. (2004), Laskar et al. (2004) for KRC layered thermal evolution.
   * @param {number} thermalInertia - Bulk regolith thermal inertia in tiu (e.g. 50 dust, 250 sand, 1200 bedrock)
   * @param {number} [bulkDensityKgM3=1500.0] - Regolith bulk density in kg/m^3
   * @param {number} [specificHeatJkgK=800.0] - Specific heat capacity in J/(kg*K)
   * @param {string} [body='mars'] - Planetary body ('mars', 'moon', 'earth')
   * @returns {{diurnalSkinDepthCm: number, seasonalSkinDepthMeters: number, obliquitySkinDepthMeters: number, volumetricHeatCapacityJPerM3K: number, thermalConductivityWmK: number}}
   */
  static computeMultiHarmonicThermalSkinDepthSpectrum(thermalInertia, bulkDensityKgM3 = 1500.0, specificHeatJkgK = 800.0, body = 'mars') {
    const I = Math.max(10.0, thermalInertia);
    const rho = Math.max(100.0, bulkDensityKgM3);
    const cp = Math.max(100.0, specificHeatJkgK);

    const bKey = body.toLowerCase();
    const isMars = bKey === 'mars';
    const isMoon = bKey === 'moon';

    // Periods in seconds
    let pDiurnal = 88775.244; // 1 sol for Mars
    let pSeasonal = 5.935e7;  // 668.6 sols (687 Earth days)
    let pObliquity = 3.787e12; // 120,000 year Milankovitch cycle

    if (isMoon) {
      pDiurnal = 29.53 * 86400.0; // 2.551e6 s
      pSeasonal = 3.156e7;
      pObliquity = 5.86e11; // 18.6 yr precession
    } else if (!isMars) {
      pDiurnal = 86400.0;
      pSeasonal = 3.156e7;
      pObliquity = 1.3e12; // 41,000 yr Earth obliquity
    }

    const Cv = rho * cp; // Volumetric heat capacity (J / (m^3 * K))

    // Thermal conductivity k = I^2 / (rho * cp)
    const k = (I * I) / Cv;

    // Skin depths d = (I / Cv) * sqrt(P / pi)
    const coeff = I / Cv;

    const dDiurnalM = coeff * Math.sqrt(pDiurnal / Math.PI);
    const dSeasonalM = coeff * Math.sqrt(pSeasonal / Math.PI);
    const dObliquityM = coeff * Math.sqrt(pObliquity / Math.PI);

    return {
      diurnalSkinDepthCm: parseFloat((dDiurnalM * 100.0).toFixed(2)),
      seasonalSkinDepthMeters: parseFloat(dSeasonalM.toFixed(3)),
      obliquitySkinDepthMeters: parseFloat(dObliquityM.toFixed(1)),
      volumetricHeatCapacityJPerM3K: parseFloat(Cv.toFixed(0)),
      thermalConductivityWmK: parseFloat(k.toFixed(4))
    };
  }

  /**
   * Calculate porous regolith dry lag vapor diffusion resistance, ice sublimation loss rate, and ice sheet preservation timescale.
   * J = ( D_eff * M / ( R * T * L_lag ) ) * ( P_sat(T_ice) - P_atm )
   * Reference: Schorghofer (2007), Mellon et al. (2004, 2008), Hudson et al. (2009) for buried ground ice stability.
   * @param {number} lagThicknessCm - Thickness of desiccated porous regolith lag mantle in cm (e.g. 5 to 50 cm)
   * @param {number} [meanSubsurfaceTempK=200.0] - Mean annual ground ice interface temperature in Kelvin
   * @param {number} [atmosphericVaporPressurePa=0.030] - Ambient atmospheric water vapor partial pressure in Pa (~10 pr-um)
   * @param {number} [regolithPorosity=0.40] - Regolith volume porosity fraction (0 to 1)
   * @param {number} [poreRadiusMicrons=5.0] - Mean regolith pore radius in microns
   * @returns {{sublimationFluxKgM2S: number, annualIceRetreatMmPerYear: number, iceSheetPreservationMyr: number, vaporSaturationPressurePa: number, lagDiffusionResistanceCoeff: number}}
   */
  static computePorousRegolithIceSublimationLagRetardation(lagThicknessCm, meanSubsurfaceTempK = 200.0, atmosphericVaporPressurePa = 0.030, regolithPorosity = 0.40, poreRadiusMicrons = 5.0) {
    const L = Math.max(0.1, lagThicknessCm) * 1e-2; // meters
    const T = Math.max(100.0, meanSubsurfaceTempK);
    const Patm = Math.max(0.0, atmosphericVaporPressurePa);
    const eps = Math.min(0.9, Math.max(0.1, regolithPorosity));
    const rp = Math.max(0.1, poreRadiusMicrons) * 1e-6; // meters

    const R = 8.314;     // J / (mol * K)
    const M = 0.018015;  // kg / mol for H2O
    const rhoIce = 920.0; // kg / m^3
    const tau = 2.5;     // tortuosity

    // Saturation vapor pressure over ice (Clausius-Clapeyron Pa)
    const pSat = Math.exp(28.868 - 6132.9 / T);
    const deltaP = Math.max(0.0, pSat - Patm);

    // Knudsen diffusion coefficient: D_K = (2/3) * r_p * sqrt( 8*R*T / (pi * M) )
    const meanThermalSpeed = Math.sqrt((8.0 * R * T) / (Math.PI * M));
    const Dk = (2.0 / 3.0) * rp * meanThermalSpeed;

    // Effective porous diffusion coefficient D_eff = (eps / tau) * D_k
    const Deff = (eps / tau) * Dk;

    // Sublimation mass flux J = (Deff * M / (R * T * L)) * deltaP (kg / (m^2 * s))
    const J = (Deff * M / (R * T * L)) * deltaP;

    // Annual ice table retreat rate (mm / Mars year, 687 days * 86400 s)
    const secPerMarsYear = 687.0 * 86400.0;
    const annualMassLossKg = J * secPerMarsYear;
    const retreatRateMPerYear = annualMassLossKg / rhoIce;
    const retreatRateMmPerYear = retreatRateMPerYear * 1000.0;

    // Time to sublimate a 5-meter ice layer in Million Years (Myr)
    const timeFor5MetersYears = retreatRateMPerYear > 1e-15 ? (5.0 / retreatRateMPerYear) : 1e12;
    const timeFor5MetersMyr = timeFor5MetersYears / 1e6;

    return {
      sublimationFluxKgM2S: parseFloat(J.toExponential(4)),
      annualIceRetreatMmPerYear: parseFloat(retreatRateMmPerYear.toExponential(4)),
      iceSheetPreservationMyr: parseFloat(timeFor5MetersMyr.toFixed(2)),
      vaporSaturationPressurePa: parseFloat(pSat.toFixed(4)),
      lagDiffusionResistanceCoeff: parseFloat((L / Deff).toFixed(1))
    };
  }

  /**
   * Calculate subsurface CO2 Clathrate Hydrate (CO2 * 5.75 H2O) phase equilibrium stability boundary and gas storage capacity.
   * ln( P_dissoc_MPa ) = 30.12 - 6030.0 / T_hydrate
   * Reference: Miller & Smythe (1970), Sloan (1998), Mousis et al. (2013), Chassefière et al. (2013) for Martian polar cryosphere.
   * @param {number} depthMeters - Subsurface depth in meters (e.g. 500 to 3000 m in polar layered deposits)
   * @param {number} [subsurfaceTempK=190.0] - In-situ geothermal temperature at depth in Kelvin
   * @param {number} [overburdenDensityKgM3=1100.0] - Bulk density of overlying polar ice/dust mantle in kg/m^3
   * @returns {{isClathrateStable: boolean, lithostaticPressureMPa: number, dissociationPressureMPa: number, maxStableTempK: number, co2GasEquivalentDensityKgM3: number, cryosphereRegime: string}}
   */
  static computeCO2ClathrateHydrateStabilityBoundary(depthMeters, subsurfaceTempK = 190.0, overburdenDensityKgM3 = 1100.0) {
    const z = Math.max(10.0, depthMeters);
    const T = Math.max(100.0, Math.min(273.15, subsurfaceTempK));
    const rho = Math.max(500.0, overburdenDensityKgM3);
    const g = 3.72; // m/s^2 Mars gravity

    // Lithostatic overburden pressure P = rho * g * z (Pa -> MPa)
    const pLithPa = rho * g * z;
    const pLithMPa = pLithPa * 1e-6;

    // Dissociation pressure of CO2 hydrate at temperature T (MPa)
    const pDissocMPa = Math.exp(30.12 - 6030.0 / T);

    // Maximum stable temperature at lithostatic pressure P_lith (K)
    let tMaxStableK = 273.15;
    if (pLithMPa > 0.001) {
      tMaxStableK = 6030.0 / (30.12 - Math.log(pLithMPa));
    }

    const isStable = pLithMPa >= pDissocMPa;

    // Storage capacity: ~165 kg of CO2 per m^3 of clathrate hydrate
    const co2StorageKgM3 = isStable ? 165.0 : 0.0;

    let regime = 'Unstable: Clathrate Dissociates to Free CO2 Gas & H2O Ice';
    if (isStable) {
      if (z >= 1000.0) {
        regime = 'Deep Polar Layered Cryosphere: Gigaton CO2 Clathrate Paleoclimate Reservoir';
      } else {
        regime = 'Shallow Metastable Cryogenic CO2 Clathrate Deposit';
      }
    }

    return {
      isClathrateStable: isStable,
      lithostaticPressureMPa: parseFloat(pLithMPa.toFixed(3)),
      dissociationPressureMPa: parseFloat(pDissocMPa.toFixed(4)),
      maxStableTempK: parseFloat(tMaxStableK.toFixed(1)),
      co2GasEquivalentDensityKgM3: parseFloat(co2StorageKgM3.toFixed(1)),
      cryosphereRegime: regime
    };
  }

  /**
   * Calculate subsurface Methane Clathrate Hydrate (CH4 * 5.75 H2O) phase equilibrium stability boundary and gas storage capacity.
   * ln( P_dissoc_MPa ) = 38.98 - 8533.8 / T_hydrate
   * Reference: Sloan (1998), Chassefière et al. (2013), Mousis et al. (2016), Webster et al. (2015) for MSL methane plume sources.
   * @param {number} depthMeters - Subsurface depth in meters (e.g. 50 to 2000 m in permafrost cryosphere)
   * @param {number} [subsurfaceTempK=210.0] - In-situ geothermal temperature at depth in Kelvin
   * @param {number} [overburdenDensityKgM3=2000.0] - Bulk density of overlying regolith/basalt in kg/m^3
   * @returns {{isMethaneHydrateStable: boolean, lithostaticPressureMPa: number, dissociationPressureMPa: number, maxStableTempK: number, ch4GasEquivalentDensityKgM3: number, outgassingVulnerability: string}}
   */
  static computeMethaneClathrateHydrateStabilityBoundary(depthMeters, subsurfaceTempK = 210.0, overburdenDensityKgM3 = 2000.0) {
    const z = Math.max(10.0, depthMeters);
    const T = Math.max(100.0, Math.min(273.15, subsurfaceTempK));
    const rho = Math.max(500.0, overburdenDensityKgM3);
    const g = 3.72; // m/s^2 Mars gravity

    // Lithostatic overburden pressure P = rho * g * z (Pa -> MPa)
    const pLithPa = rho * g * z;
    const pLithMPa = pLithPa * 1e-6;

    // Dissociation pressure of CH4 hydrate at temperature T (MPa)
    const pDissocMPa = Math.exp(38.98 - 8533.8 / T);

    // Maximum stable temperature at lithostatic pressure P_lith (K)
    let tMaxStableK = 273.15;
    if (pLithMPa > 0.0001) {
      tMaxStableK = 8533.8 / (38.98 - Math.log(pLithMPa));
    }

    const isStable = pLithMPa >= pDissocMPa;

    // Storage capacity: ~115 kg of CH4 per m^3 of clathrate hydrate
    const ch4StorageKgM3 = isStable ? 115.0 : 0.0;

    let vulnerability = 'Destabilized: Active Episodic Methane Outgassing / Atmospheric Plume Source';
    if (isStable) {
      if (z >= 300.0) {
        vulnerability = 'Secure Deep Permafrost Cryosphere Trap (Stable Sequestration)';
      } else {
        vulnerability = 'Shallow Metastable Cryotrap: Sensitive to Diurnal / Seasonal Thermal Pulses';
      }
    }

    return {
      isMethaneHydrateStable: isStable,
      lithostaticPressureMPa: parseFloat(pLithMPa.toFixed(3)),
      dissociationPressureMPa: parseFloat(pDissocMPa.toFixed(4)),
      maxStableTempK: parseFloat(tMaxStableK.toFixed(1)),
      ch4GasEquivalentDensityKgM3: parseFloat(ch4StorageKgM3.toFixed(1)),
      outgassingVulnerability: vulnerability
    };
  }

  /**
   * Calculate subsurface radar sounder dielectric permittivity, loss tangent, two-way attenuation rate, and penetration depth.
   * sqrt( eps_r ) = sum( f_i * sqrt( eps_i ) ) (CRIM mixing formula)
   * alpha = ( 8.686 * pi * f / c ) * sqrt( eps_r ) * tan_delta (dB/m)
   * Reference: Grimm et al. (2006), Stillman & Olhoeft (2008), Heggy et al. (2006) for SHARAD & MARSIS radar propagation.
   * @param {number} [regolithTempK=200.0] - Cryospheric subsurface temperature in Kelvin (150 to 260 K)
   * @param {number} [volumeFractionIce=0.80] - Pore ice volume fraction (0 to 1)
   * @param {number} [volumeFractionDust=0.15] - Silicate dust / basalt volume fraction (0 to 1)
   * @param {number} [frequencyMHz=20.0] - Radar carrier frequency in MHz (20 MHz for SHARAD, 4 MHz for MARSIS)
   * @returns {{bulkPermittivity: number, bulkLossTangent: number, attenuationRateDBPerKm: number, penetrationDepthMeters: number, waveVelocityKmPerMicrosec: number, radarRegime: string}}
   */
  static computeSubsurfaceRadarAttenuationAndLossTangent(regolithTempK = 200.0, volumeFractionIce = 0.80, volumeFractionDust = 0.15, frequencyMHz = 20.0) {
    const T = Math.max(100.0, Math.min(273.15, regolithTempK));
    const fIce = Math.min(1.0, Math.max(0.0, volumeFractionIce));
    const fDust = Math.min(1.0 - fIce, Math.max(0.0, volumeFractionDust));
    const fVoid = Math.max(0.0, 1.0 - fIce - fDust);
    const fHz = Math.max(0.1, frequencyMHz) * 1e6; // Hz
    const c = 2.99792458e8; // m/s

    // Pure constituent properties
    const epsIce = 3.15;
    const epsDust = 5.50;
    const epsVoid = 1.00;

    // Temperature-dependent ice loss tangent (Stillman & Olhoeft 2008)
    const tanDeltaIce = 1.0e-4 * Math.exp((T - 200.0) / 20.0);
    const tanDeltaDust = 0.010; // dry volcanic basalt dust

    // CRIM mixing rule for real permittivity
    const sqrtEpsBulk = fIce * Math.sqrt(epsIce) + fDust * Math.sqrt(epsDust) + fVoid * Math.sqrt(epsVoid);
    const epsBulk = sqrtEpsBulk * sqrtEpsBulk;

    // Volume-weighted dielectric loss tangent
    const epsLossBulk = fIce * epsIce * tanDeltaIce + fDust * epsDust * tanDeltaDust;
    const tanDeltaBulk = epsLossBulk / epsBulk;

    // Attenuation rate: alpha = (8.686 * pi * f / c) * sqrt(epsBulk) * tanDeltaBulk (dB/m)
    const alphaDBPerM = ((8.686 * Math.PI * fHz) / c) * Math.sqrt(epsBulk) * tanDeltaBulk;
    const alphaDBPerKm = alphaDBPerM * 1000.0;

    // Radar propagation speed in medium (km / microsecond)
    const vPropMPerS = c / Math.sqrt(epsBulk);
    const vPropKmPerMicrosec = (vPropMPerS / 1000.0) / 1e6;

    // Penetration depth (1/e power decay or ~8.686 dB attenuation)
    const penDepthM = alphaDBPerM > 1e-8 ? (8.686 / alphaDBPerM) : 10000.0;

    let regime = 'High Transparency Subsurface Ice Sheet (SHARAD Deep Penetration > 1 km)';
    if (alphaDBPerKm > 25.0) {
      regime = 'High-Loss Conductive / Warm Basaltic Regolith (Shallow Radar Penetration)';
    } else if (alphaDBPerKm > 8.0) {
      regime = 'Moderate Attenuation Pore Ice / Dusty Layered Deposit';
    }

    return {
      bulkPermittivity: parseFloat(epsBulk.toFixed(3)),
      bulkLossTangent: parseFloat(tanDeltaBulk.toExponential(4)),
      attenuationRateDBPerKm: parseFloat(alphaDBPerKm.toFixed(2)),
      penetrationDepthMeters: parseFloat(penDepthM.toFixed(1)),
      waveVelocityKmPerMicrosec: parseFloat((vPropKmPerMicrosec * 1e3).toFixed(3)), // m/microsecond or km/s
      radarRegime: regime
    };
  }

  /**
   * Calculate perchlorate/chlorate salt deliquescence relative humidity, eutectic freezing limit, and liquid brine stability.
   * DRH(T) = DRH_0 - k_T * (T - T_eutectic)
   * Reference: Hecht et al. (2009), Gough et al. (2011), Toner et al. (2014), Rivera-Valentín et al. (2020) for Phoenix & RSL brines.
   * @param {number} surfaceTempK - Regolith surface temperature in Kelvin (180 to 290 K)
   * @param {number} relativeHumidityPct - Near-surface atmospheric relative humidity % (0 to 100)
   * @param {string} [saltType='Mg(ClO4)2'] - Oxychlorine salt ('Mg(ClO4)2', 'Ca(ClO4)2', 'NaClO4', 'Mg(ClO3)2')
   * @returns {{isLiquidBrineStable: boolean, isDeliquescenceActive: boolean, eutecticTemperatureK: number, deliquescenceRHPct: number, waterActivity: number, brinePhaseState: string, planetaryProtectionHabitability: boolean}}
   */
  static computePerchlorateDeliquescenceAndLiquidBrineStability(surfaceTempK, relativeHumidityPct, saltType = 'Mg(ClO4)2') {
    const T = Math.max(100.0, Math.min(350.0, surfaceTempK));
    const rh = Math.min(100.0, Math.max(0.0, relativeHumidityPct));
    const sType = saltType.toLowerCase();

    let tEutectic = 206.0; // Mg(ClO4)2: 206 K (-67 C)
    let drh0 = 45.0;
    let kSlope = 0.12;
    let saltName = 'Magnesium Perchlorate Mg(ClO4)2';

    if (sType.includes('ca')) {
      tEutectic = 198.0; // Ca(ClO4)2: 198 K (-75 C)
      drh0 = 38.0;
      kSlope = 0.10;
      saltName = 'Calcium Perchlorate Ca(ClO4)2';
    } else if (sType.includes('na')) {
      tEutectic = 239.0; // NaClO4: 239 K (-34 C)
      drh0 = 52.0;
      kSlope = 0.15;
      saltName = 'Sodium Perchlorate NaClO4';
    } else if (sType.includes('clo3')) {
      tEutectic = 204.0; // Mg(ClO3)2: 204 K (-69 C)
      drh0 = 40.0;
      kSlope = 0.11;
      saltName = 'Magnesium Chlorate Mg(ClO3)2';
    }

    // Deliquescence relative humidity threshold at temperature T
    const drh = Math.max(15.0, Math.min(95.0, drh0 - kSlope * (T - tEutectic)));

    // Deliquescence occurs if RH >= DRH
    const isDeliquescing = rh >= drh;

    // Thermodynamic liquid brine stability
    const isAboveEutectic = T >= tEutectic;
    const isLiquid = isAboveEutectic && isDeliquescing;

    // Water activity a_w ~ RH / 100
    const aw = Math.min(1.0, Math.max(0.0, rh / 100.0));

    // NASA Special Region habitability criteria: T >= 255 K and a_w >= 0.60
    const isHabitable = T >= 255.0 && aw >= 0.60 && isLiquid;

    let phase = 'Dry Solid Crystalline Salt (RH < DRH)';
    if (isLiquid) {
      phase = `Transient Liquid Aqueous ${saltName} Brine Solution`;
    } else if (isDeliquescing && !isAboveEutectic) {
      phase = 'Metastable Supercooled Hydrated Perchlorate Slush';
    } else if (isAboveEutectic && !isDeliquescing) {
      phase = 'Warm Dry Desiccated Regolith';
    }

    return {
      isLiquidBrineStable: isLiquid,
      isDeliquescenceActive: isDeliquescing,
      eutecticTemperatureK: parseFloat(tEutectic.toFixed(1)),
      deliquescenceRHPct: parseFloat(drh.toFixed(1)),
      waterActivity: parseFloat(aw.toFixed(3)),
      brinePhaseState: phase,
      planetaryProtectionHabitability: isHabitable
    };
  }

  /**
   * Invert two-layer thermal inertia for shallow buried ice table depth and lag thickness.
   * I_app = I_lag + ( I_ice - I_lag ) * exp( -2 * z_ice / d_th )
   * z_ice = - ( d_th / 2 ) * ln( ( I_app - I_lag ) / ( I_ice - I_lag ) )
   * Reference: Bandfield (2007), Putzig et al. (2005), Titus et al. (2003) for THEMIS & TES ground ice mapping.
   * @param {number} apparentThermalInertiaSI - Measured apparent diurnal thermal inertia in J m^-2 s^-1/2 K^-1 (e.g. 150 to 800)
   * @param {number} [dryLagInertiaSI=80.0] - Dry desiccated dust/sand mantle thermal inertia
   * @param {number} [iceSaturatedInertiaSI=1800.0] - Ice-cemented pore-ice substrate thermal inertia
   * @param {number} [diurnalSkinDepthCm=4.5] - Diurnal thermal skin depth in cm
   * @returns {{iceTableDepthCm: number, iceTableDepthMeters: number, isIceTableWithinDiurnalReach: boolean, groundIcePresence: string, iceVolumeFractionEstimate: number}}
   */
  static computeTwoLayerThermalInertiaIceTableDepth(apparentThermalInertiaSI, dryLagInertiaSI = 80.0, iceSaturatedInertiaSI = 1800.0, diurnalSkinDepthCm = 4.5) {
    const Iapp = Math.max(10.0, apparentThermalInertiaSI);
    const Ilag = Math.max(10.0, dryLagInertiaSI);
    const Iice = Math.max(Ilag + 50.0, iceSaturatedInertiaSI);
    const dThCm = Math.max(0.5, diurnalSkinDepthCm);

    let zCm = 0.0;
    let presence = 'Exposed Surface Ice / Frost';
    let isWithinReach = true;

    if (Iapp >= Iice) {
      zCm = 0.0;
      presence = 'Exposed Massive Ground Ice Sheet';
    } else if (Iapp <= Ilag) {
      zCm = dThCm * 3.0; // beyond diurnal skin depth
      presence = 'Dry Deep Regolith (No Shallow Ice Detected in Diurnal Horizon)';
      isWithinReach = false;
    } else {
      // Analytical two-layer inversion
      const ratio = (Iapp - Ilag) / (Iice - Ilag);
      zCm = - (dThCm / 2.0) * Math.log(Math.max(1e-4, ratio));
      if (zCm < dThCm * 1.5) {
        presence = 'Shallow Buried Ground Ice Table (Phoenix / High-Latitude Permafrost)';
      } else {
        presence = 'Deep Transition Layer (Marginal Diurnal Detection)';
        isWithinReach = false;
      }
    }

    const zM = zCm / 100.0;
    const iceFrac = Math.min(1.0, Math.max(0.0, (Iapp - Ilag) / (Iice - Ilag)));

    return {
      iceTableDepthCm: parseFloat(zCm.toFixed(2)),
      iceTableDepthMeters: parseFloat(zM.toFixed(4)),
      isIceTableWithinDiurnalReach: isWithinReach,
      groundIcePresence: presence,
      iceVolumeFractionEstimate: parseFloat(iceFrac.toFixed(3))
    };
  }

  /**
   * Calculate pure liquid water thermodynamic metastability window, boiling temperature, and evaporation lifespan.
   * T_boil(P) = 5380.0 / ( 23.19 - ln(P_Pa) )
   * Reference: Ingersoll (1970), Haberle et al. (2001), Sears & Chittenden (2005) for Martian liquid water stability.
   * @param {number} surfaceTempK - Surface temperature in Kelvin (240 to 310 K)
   * @param {number} atmosphericPressurePa - Ambient surface atmospheric pressure in Pa (300 to 1200 Pa)
   * @param {number} [relativeHumidityPct=20.0] - Ambient relative humidity %
   * @returns {{isLiquidWaterMetastable: boolean, isAboveTriplePointPressure: boolean, boilingTemperatureK: number, meltingTemperatureK: number, evaporationRateMmPerHour: number, droplet1mmLifespanMinutes: number, thermodynamicRegime: string}}
   */
  static computeTransientLiquidWaterMetastabilityWindow(surfaceTempK, atmosphericPressurePa, relativeHumidityPct = 20.0) {
    const T = Math.max(150.0, Math.min(350.0, surfaceTempK));
    const P = Math.max(50.0, atmosphericPressurePa);
    const rh = Math.min(100.0, Math.max(0.0, relativeHumidityPct));

    const P_TRIPLE = 611.73; // Pa (6.117 mbar)
    const T_MELT = 273.15;   // K (0 C)

    const isAboveP = P >= P_TRIPLE;

    // Boiling temperature as a function of pressure P (Pa)
    // ln(P_Pa) = 25.485 - 5208.7 / T -> T_boil = 5208.7 / (25.485 - ln(P_Pa))
    let tBoilK = 273.15;
    if (P > 100.0) {
      tBoilK = 5208.7 / (25.485 - Math.log(P));
    }

    // Liquid metastability condition: P >= 611.7 Pa AND 273.15 K <= T <= T_boil(P)
    const isMetastable = isAboveP && (T >= T_MELT) && (T <= tBoilK);

    // Saturation vapor pressure over liquid (Antoine/Clausius-Clapeyron Pa)
    const pSat = Math.exp(25.485 - 5208.7 / T);
    const pAmbient = (rh / 100.0) * pSat;
    const deltaP = Math.max(10.0, pSat - pAmbient);

    // Evaporation rate (Ingersoll free convection model: ~ 0.5 to 5.0 mm/hr)
    const evapRateMmPerHr = Math.min(50.0, Math.max(0.01, 0.005 * deltaP * Math.sqrt(Math.max(0.01, (T - 250.0) / 20.0))));
    const lifespanMin = evapRateMmPerHr > 0.001 ? (1.0 / evapRateMmPerHr) * 60.0 : 999.0;

    let regime = 'Sub-Triple Point: Direct Ice-Vapor Sublimation (No Liquid Phase Possible)';
    if (!isAboveP) {
      regime = 'High Altitude / Low Pressure: Sublimation Only (P < 6.12 mbar)';
    } else if (T < T_MELT) {
      regime = 'Sub-Freezing: Solid H2O Ice Stable Under Ambient Pressure';
    } else if (T > tBoilK) {
      regime = 'Superheated: Spontaneous Boiling / Flash Vaporization';
    } else if (isMetastable) {
      regime = 'Transient Metastable Pure Liquid Water Window (Hellas / Deep Chasma Basin)';
    }

    return {
      isLiquidWaterMetastable: isMetastable,
      isAboveTriplePointPressure: isAboveP,
      boilingTemperatureK: parseFloat(tBoilK.toFixed(2)),
      meltingTemperatureK: 273.15,
      evaporationRateMmPerHour: parseFloat(evapRateMmPerHr.toFixed(2)),
      droplet1mmLifespanMinutes: parseFloat(lifespanMin.toFixed(1)),
      thermodynamicRegime: regime
    };
  }

  /**
   * Calculate seasonal polar CO2 slab solid-state greenhouse basal sublimation gas overpressure, rupture threshold, and supersonic geyser jet speed.
   * F_basal = F0 * (1 - A) * exp( -kappa * L_slab )
   * m_dot = F_basal / L_sub
   * v_jet = sqrt( (2*gamma/(gamma-1)) * (R*T/M) * ( 1 - (P_amb/P_rupt)^((gamma-1)/gamma) ) )
   * Reference: Kieffer (2007), Thomas et al. (2009), Hansen et al. (2010) for araneiform "spider" terrain and dark dust fans.
   * @param {number} [slabThicknessMeters=1.0] - Seasonal translucent CO2 ice slab thickness in meters
   * @param {number} [solarInsolationWM2=450.0] - Surface solar irradiance F0 in W/m^2
   * @param {number} [slabAlbedo=0.65] - Slab surface albedo
   * @param {number} [extinctionCoeffM=2.0] - Slab optical extinction coefficient kappa in m^-1
   * @param {number} [ambientPressurePa=600.0] - Ambient atmospheric pressure in Pa
   * @returns {{basalSolarFluxWM2: number, basalSublimationRateGPerM2Sec: number, ruptureOverpressureKPa: number, geyserEjectionSpeedMS: number, isSupersonicEruption: boolean, activeGeyserTerrain: string, timeToRuptureHours: number}}
   */
  static computeSpringGeyserBasalSublimationOverpressure(slabThicknessMeters = 1.0, solarInsolationWM2 = 450.0, slabAlbedo = 0.65, extinctionCoeffM = 2.0, ambientPressurePa = 600.0) {
    const L = Math.max(0.05, Math.min(5.0, slabThicknessMeters));
    const F0 = Math.max(10.0, solarInsolationWM2);
    const A = Math.max(0.1, Math.min(0.95, slabAlbedo));
    const kappa = Math.max(0.1, extinctionCoeffM);
    const Pamb = Math.max(50.0, ambientPressurePa);

    const L_SUB_CO2 = 5.9e5; // J/kg latent heat of sublimation
    const R_GAS = 8.31446;
    const M_CO2 = 0.04401;   // kg/mol
    const GAMMA_CO2 = 1.30;  // adiabatic heat capacity ratio
    const T_SUB = 145.0;     // K basal CO2 sublimation temperature
    const RHO_ICE = 1600.0;  // kg/m^3 slab density
    const G_MARS = 3.72076;  // m/s^2
    const SIGMA_TENSILE = 8.0e4; // Pa (80 kPa slab tensile strength)

    // Solar flux transmitted through slab to dark ground
    const Fbasal = F0 * (1.0 - A) * Math.exp(-kappa * L);

    // Sublimation rate (kg / (m^2 * s))
    const mDotKgM2S = Fbasal / L_SUB_CO2;
    const mDotGM2S = mDotKgM2S * 1000.0;

    // Rupture pressure threshold: tensile strength + slab overburden
    const Poverburden = RHO_ICE * G_MARS * L;
    const Prupture = SIGMA_TENSILE + Poverburden;
    const PruptureKPa = Prupture / 1000.0;

    // Time to rupture assuming 2 cm basal gas cavity
    const hCavity = 0.02; // m
    const gasDensityRupture = (Prupture * M_CO2) / (R_GAS * T_SUB);
    const timeToRuptureSec = (gasDensityRupture * hCavity) / Math.max(1e-8, mDotKgM2S);
    const timeToRuptureHours = timeToRuptureSec / 3600.0;

    // Isentropic expansion nozzle jet velocity
    const expansionRatio = Math.pow(Pamb / Prupture, (GAMMA_CO2 - 1.0) / GAMMA_CO2);
    const vJet = Math.sqrt((2.0 * GAMMA_CO2 / (GAMMA_CO2 - 1.0)) * (R_GAS * T_SUB / M_CO2) * (1.0 - expansionRatio));

    // Sound speed in CO2 at 145 K: c = sqrt(gamma * R * T / M) ~ 189 m/s
    const soundSpeed = Math.sqrt(GAMMA_CO2 * R_GAS * T_SUB / M_CO2);
    const isSupersonic = vJet >= soundSpeed;

    return {
      basalSolarFluxWM2: parseFloat(Fbasal.toFixed(2)),
      basalSublimationRateGPerM2Sec: parseFloat(mDotGM2S.toFixed(4)),
      ruptureOverpressureKPa: parseFloat(PruptureKPa.toFixed(2)),
      geyserEjectionSpeedMS: parseFloat(vJet.toFixed(1)),
      isSupersonicEruption: isSupersonic,
      activeGeyserTerrain: 'South Polar Araneiform "Spider" Cryptic Terrain (Dark Dust Fans)',
      timeToRuptureHours: parseFloat(timeToRuptureHours.toFixed(2))
    };
  }

  /**
   * Calculate microscale surface frost condensation, optical albedo brightening, and thermal radiative feedback.
   * A_eff = A_bare + ( A_frost - A_bare ) * ( 1 - exp( -mu * L_frost ) )
   * eps_eff = eps_bare + ( eps_frost - eps_bare ) * ( 1 - exp( -kappa * L_frost ) )
   * Reference: Kieffer et al. (2000), Vincendon et al. (2010) for Phoenix & Viking early morning frost dynamics.
   * @param {number} frostThicknessMicrons - Surface condensed frost thickness in microns (0.1 to 100 um)
   * @param {number} [bareGroundAlbedo=0.20] - Uncoated background basaltic regolith albedo
   * @param {string} [frostType='H2O'] - Condensed volatile type ('H2O', 'CO2')
   * @param {number} [solarInsolationWM2=350.0] - Morning solar irradiance in W/m^2
   * @returns {{effectiveAlbedo: number, effectiveEmissivity: number, albedoIncreasePct: number, absorbedSolarFluxWM2: number, frostBurnoffMinutes: number, frostCoverState: string}}
   */
  static computeTransientFrostCondensationAndAlbedoFeedback(frostThicknessMicrons, bareGroundAlbedo = 0.20, frostType = 'H2O', solarInsolationWM2 = 350.0) {
    const L = Math.max(0.0, frostThicknessMicrons);
    const Abare = Math.max(0.05, Math.min(0.40, bareGroundAlbedo));
    const isCO2 = frostType.toUpperCase() === 'CO2';

    const Afrost = isCO2 ? 0.72 : 0.62;
    const epsBare = 0.92;
    const epsFrost = isCO2 ? 0.99 : 0.97;
    const L_sub = isCO2 ? 5.9e5 : 2.83e6; // J/kg
    const rhoFrost = isCO2 ? 1500.0 : 900.0; // kg/m^3

    // Effective visual albedo via Beer-Lambert scattering
    const muOptical = isCO2 ? 0.08 : 0.05; // um^-1
    const Aeff = Abare + (Afrost - Abare) * (1.0 - Math.exp(-muOptical * L));

    // Effective infrared thermal emissivity
    const epsEff = epsBare + (epsFrost - epsBare) * (1.0 - Math.exp(-0.03 * L));

    const albedoInc = ((Aeff - Abare) / Abare) * 100.0;
    const absorbedSolar = Math.max(0.0, solarInsolationWM2 * (1.0 - Aeff));

    // Frost mass per unit area: m = rho * L (kg/m^2)
    const frostMassKgM2 = rhoFrost * (L * 1e-6);
    const burnoffSec = absorbedSolar > 1.0 ? (frostMassKgM2 * L_sub) / absorbedSolar : 9999.0;
    const burnoffMin = burnoffSec / 60.0;

    let state = 'Bare Regolith (No Condensate)';
    if (L > 0.1 && L < 5.0) {
      state = `Sub-Micron Transient Morning ${frostType} Frost Film (High Evaporation)`;
    } else if (L >= 5.0 && L < 30.0) {
      state = `Bright Optically Thick ${frostType} Frost Mantle`;
    } else if (L >= 30.0) {
      state = `Continuous Seasonal ${frostType} Snow / Perennial Ice Slab`;
    }

    return {
      effectiveAlbedo: parseFloat(Aeff.toFixed(4)),
      effectiveEmissivity: parseFloat(epsEff.toFixed(4)),
      albedoIncreasePct: parseFloat(albedoInc.toFixed(1)),
      absorbedSolarFluxWM2: parseFloat(absorbedSolar.toFixed(1)),
      frostBurnoffMinutes: parseFloat(burnoffMin.toFixed(1)),
      frostCoverState: state
    };
  }

  /**
   * Calculate 1D subsurface thermal wave propagation, exponential amplitude attenuation, and phase lag delay in layered Martian regolith.
   * d_s = sqrt( alpha * P / pi )
   * Delta_T(z) = Delta_T0 * exp( -z / d_s )
   * phase_lag = z / d_s
   * Reference: Christensen (1986), Mellon et al. (2004), Bandfield (2007) for THEMIS & InSight SEIS heat flow thermal sounding.
   * @param {number} depthMeters - Subsurface probe depth in meters (0 to 2.5 m)
   * @param {number} [surfaceAmplitudeK=40.0] - Surface diurnal/seasonal peak-to-peak temperature amplitude Delta_T0 in K
   * @param {string} [periodCycle='diurnal'] - Thermal cycle ('diurnal', 'annual')
   * @param {number} [thermalDiffusivityM2S=3.5e-8] - Regolith thermal diffusivity alpha in m^2/s
   * @returns {{thermalSkinDepthCm: number, thermalSkinDepthMeters: number, dampedAmplitudeK: number, amplitudeAttenuationPct: number, phaseDelayHours: number, phaseDelaySols: number, thermalPenetrationHorizon: string}}
   */
  static computeSubsurfaceThermalWaveAttenuation(depthMeters, surfaceAmplitudeK = 40.0, periodCycle = 'diurnal', thermalDiffusivityM2S = 3.5e-8) {
    const z = Math.max(0.0, depthMeters);
    const dT0 = Math.max(0.1, surfaceAmplitudeK);
    const alpha = Math.max(1e-9, thermalDiffusivityM2S);
    const isAnnual = periodCycle.toLowerCase() === 'annual';

    const MARS_SOL_SEC = 88775.244;
    const MARS_YEAR_SOLS = 668.6;
    const P = isAnnual ? MARS_YEAR_SOLS * MARS_SOL_SEC : MARS_SOL_SEC;

    // Thermal skin depth d_s = sqrt(alpha * P / pi)
    const dsMeters = Math.sqrt((alpha * P) / Math.PI);
    const dsCm = dsMeters * 100.0;

    // Damped amplitude at depth z
    const dTZ = dT0 * Math.exp(-z / dsMeters);
    const attPct = (1.0 - dTZ / dT0) * 100.0;

    // Phase delay
    const phaseRad = z / dsMeters;
    const delaySec = (phaseRad * P) / (2.0 * Math.PI);
    const delayHours = delaySec / 3600.0;
    const delaySols = delaySec / MARS_SOL_SEC;

    let horizon = 'Active Dynamic Thermal Boundary Layer (Sub-Skin Depth)';
    if (z === 0.0) {
      horizon = 'Exposed Surface Skin Interface';
    } else if (z > dsMeters * 3.0) {
      horizon = 'Isothermal Deep Subsurface (> 3 Skin Depths: Constant Mean Annual Temperature)';
    } else if (z >= dsMeters) {
      horizon = 'Attenuated Deep Thermal Zone (> 1 Skin Depth: Weak Damped Signal)';
    }

    return {
      thermalSkinDepthCm: parseFloat(dsCm.toFixed(2)),
      thermalSkinDepthMeters: parseFloat(dsMeters.toFixed(4)),
      dampedAmplitudeK: parseFloat(dTZ.toFixed(2)),
      amplitudeAttenuationPct: parseFloat(attPct.toFixed(1)),
      phaseDelayHours: parseFloat(delayHours.toFixed(2)),
      phaseDelaySols: parseFloat(delaySols.toFixed(3)),
      thermalPenetrationHorizon: horizon
    };
  }

  /**
   * Calculate steady-state geothermal gradient, cryosphere thickness, and basal ice melting depth for pure water and perchlorate brines.
   * dT/dz = Q_geo / k_th
   * z_melt = ( T_melt - T_surf ) / ( dT/dz )
   * Reference: Clifford (1993), Hanna & Phillips (2005), Orosei et al. (2018) for MARSIS South Polar subglacial liquid lake stability.
   * @param {number} meanSurfaceTempK - Mean annual surface temperature in Kelvin (150 to 220 K)
   * @param {number} [geothermalHeatFluxMWM2=25.0] - Basal geothermal heat flux in mW/m^2 (15 to 45 mW/m^2)
   * @param {number} [thermalConductivityWMK=2.5] - Bulk rock/ice thermal conductivity in W/(m*K)
   * @param {string} [poreFluidType='brine'] - Subsurface pore fluid ('pure_water', 'brine')
   * @returns {{geothermalGradientKPerKm: number, cryosphereThicknessKm: number, cryosphereThicknessMeters: number, basalMeltingTemperatureK: number, isSubglacialBasalMeltingPossible: boolean, basalPorePressureMPa: number, subglacialSetting: string}}
   */
  static computeBasalMeltingAndCryosphereThickness(meanSurfaceTempK, geothermalHeatFluxMWM2 = 25.0, thermalConductivityWMK = 2.5, poreFluidType = 'brine') {
    const Tsurf = Math.max(100.0, Math.min(260.0, meanSurfaceTempK));
    const QgeoW = Math.max(1.0, geothermalHeatFluxMWM2) * 1e-3; // W/m^2
    const kth = Math.max(0.1, thermalConductivityWMK);
    const isBrine = poreFluidType.toLowerCase().includes('brine');

    const T_MELT = isBrine ? 205.0 : 273.15; // K (Perchlorate eutectic vs Pure H2O)
    const RHO_OVERBURDEN = 1800.0; // kg/m^3
    const G_MARS = 3.72076; // m/s^2

    // Geothermal gradient dT/dz (K/m and K/km)
    const dTDzKPerM = QgeoW / kth;
    const dTDzKPerKm = dTDzKPerM * 1000.0;

    // Basal melting depth z_melt (m and km)
    let zMeltMeters = (T_MELT - Tsurf) / dTDzKPerM;
    zMeltMeters = Math.max(10.0, zMeltMeters);
    const zMeltKm = zMeltMeters / 1000.0;

    // Basal lithostatic/hydrostatic pressure (MPa)
    const pBasePa = RHO_OVERBURDEN * G_MARS * zMeltMeters;
    const pBaseMPa = pBasePa / 1e6;

    let setting = 'Thick Permafrost Cryosphere (Solid Subsurface Ice)';
    let isPossible = false;

    if (isBrine && zMeltKm <= 5.0) {
      setting = 'Subglacial Perchlorate Brine Horizon (MARSIS Ultimi Scopuli South Polar Liquid Body Analogue)';
      isPossible = true;
    } else if (!isBrine && zMeltKm <= 12.0) {
      setting = 'Deep Basal Pure Water Cryosphere Melting Interface (Regional Hydrothermal Aquifer)';
      isPossible = true;
    }

    return {
      geothermalGradientKPerKm: parseFloat(dTDzKPerKm.toFixed(2)),
      cryosphereThicknessKm: parseFloat(zMeltKm.toFixed(2)),
      cryosphereThicknessMeters: parseFloat(zMeltMeters.toFixed(1)),
      basalMeltingTemperatureK: parseFloat(T_MELT.toFixed(2)),
      isSubglacialBasalMeltingPossible: isPossible,
      basalPorePressureMPa: parseFloat(pBaseMPa.toFixed(2)),
      subglacialSetting: setting
    };
  }

  /**
   * Calculate subsurface ground ice thermodynamic equilibrium stability and desiccation retreat depth under atmospheric water vapor pressure.
   * T_frost = 6141.9 / ( 28.90 - ln(P_vapor) )
   * Reference: Mellon & Phillips (2001), Schorghofer & Aharonson (2005), Chamberlain & Boynton (2007) for Phoenix & Odyssey GRS ground ice stability.
   * @param {number} meanAnnualSurfaceTempK - Mean annual surface regolith temperature in K (170 to 225 K)
   * @param {number} [atmosphericVaporPressurePa=0.25] - Column atmospheric water vapor partial pressure in Pa (0.05 to 1.5 Pa)
   * @param {number} [latitudeDeg=65.0] - Target latitude in degrees
   * @returns {{frostPointTempK: number, isGroundIceStableAtSurface: boolean, equilibriumIceTableDepthCm: number, equilibriumIceTableDepthMeters: number, iceStabilityRegime: string, vaporEquilibriumStatus: string}}
   */
  static computeSubsurfaceIceTableEquilibriumRetreatDepth(meanAnnualSurfaceTempK, atmosphericVaporPressurePa = 0.25, latitudeDeg = 65.0) {
    const Tsurf = Math.max(120.0, Math.min(260.0, meanAnnualSurfaceTempK));
    const Pv = Math.max(0.01, atmosphericVaporPressurePa);
    const latAbs = Math.abs(latitudeDeg);

    // Frost point temperature for water vapor
    const Tfrost = 6141.9 / (28.90 - Math.log(Pv));

    const isStable = Tsurf <= Tfrost;

    let zDepthCm = 0.0;
    let regime = 'Perennial Surface Frost & Ground Ice Table (< 5 cm Depth)';
    let status = 'Thermodynamically Stable in Vapor Equilibrium with Atmosphere';

    if (isStable) {
      // Stable near surface with thin dry lag
      zDepthCm = Math.max(1.0, (Tsurf / Tfrost) * 3.5);
      if (latAbs >= 60.0) {
        regime = 'High-Latitude Permafrost Excess Ground Ice (Phoenix / Utopia Planitia Type)';
      }
    } else {
      // Unstable: retreats to deep desiccation horizon
      const deltaT = Tsurf - Tfrost;
      zDepthCm = Math.min(500.0, 5.0 + deltaT * 8.5); // cm
      status = 'Metastable / Actively Sublimating (Requires Thick Regolith Dust Mantle to Retard Loss)';
      if (zDepthCm > 100.0) {
        regime = 'Desiccated Equatorial Regolith (Ice Table Discontinuous or Deep > 1 m)';
      } else {
        regime = 'Mid-Latitude Buried Glacial Ice (Protected by Decimeter Regolith Mantle)';
      }
    }

    return {
      frostPointTempK: parseFloat(Tfrost.toFixed(2)),
      isGroundIceStableAtSurface: isStable,
      equilibriumIceTableDepthCm: parseFloat(zDepthCm.toFixed(1)),
      equilibriumIceTableDepthMeters: parseFloat((zDepthCm / 100.0).toFixed(3)),
      iceStabilityRegime: regime,
      vaporEquilibriumStatus: status
    };
  }

  /**
   * Calculate 2-layer stratified regolith thermal conduction, interface temperatures, and thermal resistance across dust mantle/bedrock boundaries.
   * dT1/dz = Q_geo / k1
   * T_int = T_surf + (dT1/dz) * L
   * dT2/dz = Q_geo / k2
   * Reference: Putzig & Mellon (2007), Bandfield (2007), Kieffer (2013) for THEMIS multi-layer thermal inertia inversions.
   * @param {number} surfaceTempK - Surface temperature in Kelvin (150 to 260 K)
   * @param {number} [topLayerThicknessMeters=0.05] - Top dry dust lag mantle thickness L in meters
   * @param {number} [topThermalConductivityWMK=0.03] - Top fine regolith thermal conductivity k1 in W/(m*K)
   * @param {number} [bottomThermalConductivityWMK=2.0] - Bottom bedrock/ice thermal conductivity k2 in W/(m*K)
   * @param {number} [targetDepthMeters=0.50] - Evaluation depth z in meters
   * @param {number} [geothermalHeatFluxMWM2=25.0] - Geothermal heat flux in mW/m^2
   * @returns {{interfaceTempK: number, targetDepthTempK: number, topLayerGradientKPerKm: number, bottomLayerGradientKPerKm: number, totalThermalResistanceM2KW: number, stratigraphyContext: string}}
   */
  static computeStratifiedRegolithThermalProfile(surfaceTempK, topLayerThicknessMeters = 0.05, topThermalConductivityWMK = 0.03, bottomThermalConductivityWMK = 2.0, targetDepthMeters = 0.50, geothermalHeatFluxMWM2 = 25.0) {
    const Tsurf = Math.max(100.0, Math.min(300.0, surfaceTempK));
    const L = Math.max(0.001, topLayerThicknessMeters);
    const k1 = Math.max(0.005, topThermalConductivityWMK);
    const k2 = Math.max(0.05, bottomThermalConductivityWMK);
    const z = Math.max(L, targetDepthMeters);
    const QgeoW = Math.max(1.0, geothermalHeatFluxMWM2) * 1e-3; // W/m^2

    // Upper layer gradient and interface temperature
    const grad1KPerM = QgeoW / k1;
    const grad1KPerKm = grad1KPerM * 1000.0;
    const Tint = Tsurf + grad1KPerM * L;

    // Lower substrate gradient and target depth temperature
    const grad2KPerM = QgeoW / k2;
    const grad2KPerKm = grad2KPerM * 1000.0;
    const Tz = Tint + grad2KPerM * (z - L);

    // Thermal resistance in series: R = L/k1 + (z-L)/k2
    const rTh = (L / k1) + ((z - L) / k2);

    let context = 'Thin High-Insulation Dust Mantle over Conductive Basalt Bedrock';
    if (k2 >= 2.5) {
      context = 'Loose Regolith Mantle over Massive Subsurface Pore-Filling Ground Ice';
    }

    return {
      interfaceTempK: parseFloat(Tint.toFixed(3)),
      targetDepthTempK: parseFloat(Tz.toFixed(3)),
      topLayerGradientKPerKm: parseFloat(grad1KPerKm.toFixed(1)),
      bottomLayerGradientKPerKm: parseFloat(grad2KPerKm.toFixed(1)),
      totalThermalResistanceM2KW: parseFloat(rTh.toFixed(3)),
      stratigraphyContext: context
    };
  }

  /**
   * Calculate glacial ice basal driving shear stress, temperature-dependent Glen flow creep deformation, and flow velocity for Martian polar caps and debris aprons.
   * tau_b = rho_ice * g * sin(alpha) * H
   * A(T) = A_0 * exp( -Q / (R * T) )
   * u_def = 2 * A(T) / (n + 1) * tau_b^n * H
   * Reference: Glen (1955), Paterson (1994), Fastook et al. (2008), Karlsson et al. (2015) for North Polar Layered Deposits (NPLD) flow.
   * @param {number} iceThicknessMeters - Total ice sheet or lobate apron thickness H in meters (100 to 3500 m)
   * @param {number} [surfaceSlopeDeg=1.5] - Surface slope angle alpha in degrees (0.1 to 10 deg)
   * @param {number} [basalTempK=210.0] - Ice bed/basal temperature in Kelvin (160 to 273 K)
   * @param {number} [dustFractionPct=5.0] - Embedded lithic dust fraction (0 to 30 %)
   * @returns {{basalShearStressKPa: number, arrheniusGlenRateFactor: number, internalDeformationSpeedMmPerYear: number, isBasalSlidingActive: boolean, glacialFlowRegime: string}}
   */
  static computeGlacialIceFlowAndBasalShearStress(iceThicknessMeters, surfaceSlopeDeg = 1.5, basalTempK = 210.0, dustFractionPct = 5.0) {
    const H = Math.max(10.0, iceThicknessMeters);
    const alphaRad = (Math.max(0.01, surfaceSlopeDeg) * Math.PI) / 180.0;
    const Tbasal = Math.max(120.0, Math.min(273.15, basalTempK));
    const dustFrac = Math.max(0.0, Math.min(0.50, dustFractionPct / 100.0));

    const RHO_ICE = 917.0 * (1.0 - dustFrac) + 2600.0 * dustFrac; // kg/m^3
    const G_MARS = 3.72076; // m/s^2
    const R_GAS = 8.31446; // J/(mol*K)
    const Q_ACTIVATION = 60000.0; // J/mol for T < 263 K
    const A0 = 3.615e-13; // Pa^-3 s^-1
    const n = 3.0; // Glen exponent

    // Basal driving shear stress tau_b = rho * g * sin(alpha) * H (Pa and kPa)
    const tauBPa = RHO_ICE * G_MARS * Math.sin(alphaRad) * H;
    const tauBKPa = tauBPa / 1000.0;

    // Arrhenius temperature-dependent rate factor A(T)
    const AT = A0 * Math.exp(-Q_ACTIVATION / (R_GAS * Tbasal));

    // Internal deformation velocity u_def = (2 * A(T) / (n+1)) * tau_b^n * H (m/s -> mm/year)
    const uDefMS = (2.0 * AT / (n + 1.0)) * Math.pow(tauBPa, n) * H;
    const secPerYear = 31557600.0;
    const uDefMmYr = uDefMS * secPerYear * 1000.0;

    const isSliding = Tbasal >= 250.0;

    let regime = 'Cold-Based Stagnant Ice Sheet (Frozen to Bedrock - Negligible Creep Flow)';
    if (uDefMmYr > 10.0) {
      regime = 'Active Viscoplastic Glacial Creep (Debris-Covered Glacier / Lobate Debris Apron Flow)';
    } else if (uDefMmYr > 0.01) {
      regime = 'Slow Polar Viscous Relaxation (North Polar Layered Deposit Chasma Infill)';
    }

    return {
      basalShearStressKPa: parseFloat(tauBKPa.toFixed(2)),
      arrheniusGlenRateFactor: parseFloat(AT.toExponential(4)),
      internalDeformationSpeedMmPerYear: parseFloat(uDefMmYr.toFixed(5)),
      isBasalSlidingActive: isSliding,
      glacialFlowRegime: regime
    };
  }

  /**
   * Calculate 2-layer polar cap thermal stratigraphy, thermal insulation by CO2 dry ice slabs, and basal interface temperatures.
   * dT_CO2/dz = Q_geo / k_CO2
   * T_int = T_surf + (dT_CO2/dz) * L_CO2
   * dT_H2O/dz = Q_geo / k_H2O
   * T_bed = T_int + (dT_H2O/dz) * L_H2O
   * Reference: Thomas et al. (2000), Byrne & Ingersoll (2003), Phillips et al. (2011) for SHARAD buried massive CO2 ice deposits.
   * @param {number} surfaceTempK - Surface temperature at top of dry ice slab in Kelvin (140 to 160 K)
   * @param {number} [co2SlabThicknessMeters=300.0] - Upper CO2 dry ice slab thickness in meters (1 to 1000 m)
   * @param {number} [h2oIceThicknessMeters=1500.0] - Lower H2O polar layered deposit thickness in meters (100 to 3000 m)
   * @param {number} [geothermalHeatFluxMWM2=25.0] - Geothermal heat flux in mW/m^2 (15 to 40 mW/m^2)
   * @returns {{co2H2OInterfaceTempK: number, bedrockBasalTempK: number, co2LayerGradientKPerKm: number, h2oLayerGradientKPerKm: number, co2ThermalBlanketingDeltaTK: number, polarStratigraphyContext: string}}
   */
  static computeLayeredPolarCapThermalProfile(surfaceTempK, co2SlabThicknessMeters = 300.0, h2oIceThicknessMeters = 1500.0, geothermalHeatFluxMWM2 = 25.0) {
    const Tsurf = Math.max(100.0, Math.min(180.0, surfaceTempK));
    const Lco2 = Math.max(0.1, co2SlabThicknessMeters);
    const Lh2o = Math.max(10.0, h2oIceThicknessMeters);
    const QgeoW = Math.max(1.0, geothermalHeatFluxMWM2) * 1e-3; // W/m^2

    // Thermal conductivities (W/(m*K))
    const kCO2 = 0.50; // dry ice is 5x more insulating than water ice
    const kH2O = 2.50;

    // Thermal gradients (K/m -> K/km)
    const gradCO2KPerM = QgeoW / kCO2;
    const gradCO2KPerKm = gradCO2KPerM * 1000.0;

    const gradH2OKPerM = QgeoW / kH2O;
    const gradH2OKPerKm = gradH2OKPerM * 1000.0;

    // Interface and bedrock temperatures (K)
    const deltaTCO2 = gradCO2KPerM * Lco2;
    const Tint = Tsurf + deltaTCO2;
    const deltaTH2O = gradH2OKPerM * Lh2o;
    const Tbed = Tint + deltaTH2O;

    let context = 'Thin Seasonal CO2 Frost over Perennial H2O Polar Layered Deposits';
    if (Lco2 >= 100.0) {
      context = 'Massive Buried CO2 Ice Package with Strong Basal Thermal Blanketing (SHARAD SPLD Unit)';
    }

    return {
      co2H2OInterfaceTempK: parseFloat(Tint.toFixed(2)),
      bedrockBasalTempK: parseFloat(Tbed.toFixed(2)),
      co2LayerGradientKPerKm: parseFloat(gradCO2KPerKm.toFixed(1)),
      h2oLayerGradientKPerKm: parseFloat(gradH2OKPerKm.toFixed(1)),
      co2ThermalBlanketingDeltaTK: parseFloat(deltaTCO2.toFixed(2)),
      polarStratigraphyContext: context
    };
  }

  /**
   * Calculate 2-layer permafrost ground ice to fractured basalt bedrock conductive thermal discontinuity and geothermal profile.
   * dT_ice/dz = Q_geo / k_ice
   * T_bed = T_surf + (dT_ice/dz) * L_ice
   * dT_rock/dz = Q_geo / k_rock
   * T(z) = T_bed + (dT_rock/dz) * ( z - L_ice )
   * Reference: Mellon et al. (2004), Clifford et al. (2010), Kieffer (2013) for Martian sub-permafrost hydrothermal gradients.
   * @param {number} meanSurfaceTempK - Mean annual ground surface temperature in K (150 to 220 K)
   * @param {number} [iceLayerThicknessMeters=50.0] - Permafrost / massive ice sheet thickness L_ice in meters (5 to 1000 m)
   * @param {number} [targetDepthMeters=500.0] - Deep crustal evaluation depth in meters (z >= L_ice)
   * @param {number} [geothermalHeatFluxMWM2=25.0] - Basal geothermal heat flux in mW/m^2
   * @returns {{bedrockInterfaceTempK: number, targetDepthTempK: number, iceLayerGradientKPerKm: number, bedrockLayerGradientKPerKm: number, thermalConductivityRatio: number, permafrostContext: string}}
   */
  static computePermafrostBedrockThermalDiscontinuity(meanSurfaceTempK, iceLayerThicknessMeters = 50.0, targetDepthMeters = 500.0, geothermalHeatFluxMWM2 = 25.0) {
    const Tsurf = Math.max(100.0, Math.min(260.0, meanSurfaceTempK));
    const Lice = Math.max(1.0, iceLayerThicknessMeters);
    const zTarget = Math.max(Lice, targetDepthMeters);
    const QgeoW = Math.max(1.0, geothermalHeatFluxMWM2) * 1e-3; // W/m^2

    // Thermal conductivities
    const kIce = 2.50; // W/(m*K)
    const kRock = 1.80; // W/(m*K) fractured basalt bedrock

    const gradIceKPerM = QgeoW / kIce;
    const gradIceKPerKm = gradIceKPerM * 1000.0;

    const gradRockKPerM = QgeoW / kRock;
    const gradRockKPerKm = gradRockKPerM * 1000.0;

    const Tbed = Tsurf + gradIceKPerM * Lice;
    const Tz = Tbed + gradRockKPerM * (zTarget - Lice);

    const ratioK = kIce / kRock;

    let context = 'Shallow Permafrost Table over Fractured Basaltic Basement';
    if (Lice >= 200.0) {
      context = 'Thick Glacial Ice Sheet / Lobate Debris Apron over Crystalline Bedrock Contact';
    }

    return {
      bedrockInterfaceTempK: parseFloat(Tbed.toFixed(2)),
      targetDepthTempK: parseFloat(Tz.toFixed(2)),
      iceLayerGradientKPerKm: parseFloat(gradIceKPerKm.toFixed(1)),
      bedrockLayerGradientKPerKm: parseFloat(gradRockKPerKm.toFixed(2)),
      thermalConductivityRatio: parseFloat(ratioK.toFixed(2)),
      permafrostContext: context
    };
  }

  /**
   * Calculate porous regolith thermal conductivity, bulk density, and thermal inertia jump as a function of pore ice saturation.
   * k_eff = k_lithic^(1-phi) * k_ice^(phi * S_ice) * k_gas^(phi * (1 - S_ice))
   * rho_bulk = (1 - phi) * rho_rock + phi * S_ice * rho_ice
   * I = sqrt( k_eff * rho_bulk * C_p )
   * Reference: Mellon et al. (2008), Schorghofer & Aharonson (2005), Piqueux & Christensen (2009) for Phoenix Lander ground ice.
   * @param {number} [porosityPct=40.0] - Soil porosity phi in percent (10 to 70 %)
   * @param {number} [icePoreSaturationPct=80.0] - Fraction of pore space filled with ground ice (0 to 100 %)
   * @param {number} [lithicConductivityWMK=2.0] - Matrix grain lithic conductivity in W/(m*K)
   * @param {number} [poreGasConductivityWMK=0.015] - Pore gas thermal conductivity in W/(m*K)
   * @returns {{effectiveThermalConductivityWMK: number, bulkDensityKgM3: number, apparentThermalInertiaTIU: number, conductivityEnhancementFactor: number, groundIceState: string}}
   */
  static computePoreIceSaturationThermalConductivity(porosityPct = 40.0, icePoreSaturationPct = 80.0, lithicConductivityWMK = 2.0, poreGasConductivityWMK = 0.015) {
    const phi = Math.max(0.05, Math.min(0.80, porosityPct / 100.0));
    const Sice = Math.max(0.0, Math.min(1.0, icePoreSaturationPct / 100.0));
    const kLithic = Math.max(0.1, lithicConductivityWMK);
    const kGas = Math.max(0.001, poreGasConductivityWMK);
    const kIce = 2.50; // W/(m*K) at 200 K

    const RHO_ROCK = 2800.0; // kg/m^3
    const RHO_ICE = 917.0; // kg/m^3
    const CP_SOIL = 800.0; // J/(kg*K)

    // Dry baseline conductivity (S_ice = 0)
    const kDry = Math.pow(kLithic, 1.0 - phi) * Math.pow(kGas, phi);

    // Saturated effective conductivity
    const kEff = Math.pow(kLithic, 1.0 - phi) * Math.pow(kIce, phi * Sice) * Math.pow(kGas, phi * (1.0 - Sice));

    // Bulk density
    const rhoBulk = (1.0 - phi) * RHO_ROCK + phi * Sice * RHO_ICE;

    // Apparent thermal inertia I = sqrt( k * rho * Cp ) (J m^-2 K^-1 s^-1/2)
    const thermalInertia = Math.sqrt(kEff * rhoBulk * CP_SOIL);

    // Enhancement over dry soil
    const enhancementRatio = kEff / kDry;

    let state = 'Dry Uncemented Regolith Mantle (Low Thermal Inertia)';
    if (Sice >= 0.70) {
      state = 'Massive Pore-Filling Ground Ice / Cryolithosphere (High Thermal Inertia THEMIS Signature)';
    } else if (Sice > 0.05) {
      state = 'Partially Cemented Subsurface Ice-Dust Duricrust';
    }

    return {
      effectiveThermalConductivityWMK: parseFloat(kEff.toFixed(4)),
      bulkDensityKgM3: parseFloat(rhoBulk.toFixed(1)),
      apparentThermalInertiaTIU: parseFloat(thermalInertia.toFixed(1)),
      conductivityEnhancementFactor: parseFloat(enhancementRatio.toFixed(2)),
      groundIceState: state
    };
  }

  /**
   * Calculate subsurface ground ice sublimation front retreat rate, Knudsen vapor diffusion through porous dry lag, and desiccation velocity.
   * D_eff = ( phi / tau^2 ) * (2/3) * r_pore * v_th
   * J_vapor = ( D_eff / z_ice ) * ( rho_sat(T_ice) - rho_surf )
   * dz/dt = J_vapor / ( phi * S_ice * rho_ice )
   * Reference: Fanale et al. (1986), Mellon & Jakosky (1993), Schorghofer (2008) for Martian equatorial and mid-latitude ground ice retreat.
   * @param {number} [iceFrontDepthMeters=0.25] - Depth of buried ice table below dry lag mantle in meters (0.01 to 5.0 m)
   * @param {number} [iceTempK=205.0] - Temperature of ground ice front in Kelvin (170 to 240 K)
   * @param {number} [porosityPct=40.0] - Dry lag mantle porosity in percent (20 to 60 %)
   * @param {number} [tortuosityFactor=2.0] - Pore tortuosity factor tau (1.5 to 3.0)
   * @param {number} [poreRadiusMicrons=5.0] - Mean pore throat radius in microns (1 to 50 um)
   * @returns {{knudsenDiffusivityM2S: number, vaporMassFluxKgM2S: number, retreatRateMicronsPerYear: number, timeToRetreatOneMeterYears: number, desiccationRegime: string}}
   */
  static computePoreIceSublimationFrontRetreatRate(iceFrontDepthMeters = 0.25, iceTempK = 205.0, porosityPct = 40.0, tortuosityFactor = 2.0, poreRadiusMicrons = 5.0) {
    const zIce = Math.max(0.005, iceFrontDepthMeters);
    const Tice = Math.max(140.0, Math.min(273.15, iceTempK));
    const phi = Math.max(0.10, Math.min(0.70, porosityPct / 100.0));
    const tau = Math.max(1.0, tortuosityFactor);
    const rPoreM = Math.max(0.1, poreRadiusMicrons) * 1e-6;

    const R_GAS = 8.31446;
    const M_H2O = 0.018015; // kg/mol
    const RHO_ICE = 917.0; // kg/m^3
    const S_ICE = 0.80; // 80% pore saturation

    // Mean thermal molecular velocity v_th = sqrt( 8 * R * T / (pi * M) )
    const vTh = Math.sqrt((8.0 * R_GAS * Tice) / (Math.PI * M_H2O));

    // Effective Knudsen diffusion coefficient D_eff = (phi / tau^2) * (2/3) * r_pore * v_th
    const dEffM2S = (phi / (tau * tau)) * (2.0 / 3.0) * rPoreM * vTh;

    // Saturation vapor pressure P_sat = 611.65 * exp( -51058/R * (1/T - 1/273.16) )
    const L_SUB = 51058.0; // J/mol
    const pSatPa = 611.65 * Math.exp(-(L_SUB / R_GAS) * ((1.0 / Tice) - (1.0 / 273.16)));

    // Saturated water vapor density rho_sat = (P_sat * M) / (R * T)
    const rhoSatKgM3 = (pSatPa * M_H2O) / (R_GAS * Tice);

    // Assume dry surface ambient vapor density is ~10% of saturation
    const rhoSurfKgM3 = rhoSatKgM3 * 0.10;

    // Vapor mass flux J = (D_eff / z_ice) * (rho_sat - rho_surf)
    const fluxKgM2S = (dEffM2S / zIce) * (rhoSatKgM3 - rhoSurfKgM3);

    // Ice retreat speed dz/dt = J / (phi * S_ice * rho_ice) (m/s -> um/year)
    const retreatSpeedMS = fluxKgM2S / (phi * S_ICE * RHO_ICE);
    const secPerYear = 31557600.0;
    const retreatMicronsYr = retreatSpeedMS * secPerYear * 1e6;

    // Time to retreat 1 meter (years)
    const yearsPerMeter = retreatMicronsYr > 0.0 ? 1e6 / retreatMicronsYr : 1e9;

    let regime = 'Active Desiccation Retreat (Equatorial Unstable Ground Ice Table)';
    if (retreatMicronsYr < 0.1) {
      regime = 'Thermodynamically Preserved Ground Ice (Perennial High-Latitude Stability Table)';
    } else if (retreatMicronsYr < 10.0) {
      regime = 'Slow Sublimation Diffusion (Mid-Latitude Mantled Glacial Ice Retention)';
    }

    return {
      knudsenDiffusivityM2S: parseFloat(dEffM2S.toExponential(4)),
      vaporMassFluxKgM2S: parseFloat(fluxKgM2S.toExponential(4)),
      retreatRateMicronsPerYear: parseFloat(retreatMicronsYr.toFixed(3)),
      timeToRetreatOneMeterYears: parseFloat(yearsPerMeter.toFixed(0)),
      desiccationRegime: regime
    };
  }

  /**
   * Calculate multi-harmonic planetary thermal penetration skin depth, diurnal/annual damping ratios, and subsurface phase lag.
   * d_s = ( I / (rho * C_p) ) * sqrt( P / pi )
   * A(z) / A_0 = exp( -z / d_s )
   * Delta_phi = z / d_s
   * Reference: Kieffer et al. (1977), Mellon et al. (2000), Kieffer (2013) for KRC diurnal and seasonal regolith grids.
   * @param {number} [thermalInertiaTIU=250.0] - Surface thermal inertia in J m^-2 K^-1 s^-1/2 (50 to 2000 tiu)
   * @param {number} [bulkDensityKgM3=1500.0] - Regolith bulk density in kg/m^3 (1000 to 3000 kg/m^3)
   * @param {number} [specificHeatJPerKgK=800.0] - Regolith specific heat capacity in J/(kg*K)
   * @param {number} [evaluationDepthMeters=0.10] - Evaluation depth z in meters
   * @returns {{thermalDiffusivityM2S: number, diurnalSkinDepthCm: number, seasonalSkinDepthMeters: number, diurnalAmplitudeDampingFraction: number, seasonalAmplitudeDampingFraction: number, diurnalPhaseLagHours: number, thermalRegimeDescription: string}}
   */
  static computeMultiHarmonicThermalPenetrationDepth(thermalInertiaTIU = 250.0, bulkDensityKgM3 = 1500.0, specificHeatJPerKgK = 800.0, evaluationDepthMeters = 0.10) {
    const I = Math.max(10.0, thermalInertiaTIU);
    const rho = Math.max(500.0, bulkDensityKgM3);
    const Cp = Math.max(200.0, specificHeatJPerKgK);
    const z = Math.max(0.001, evaluationDepthMeters);

    const P_SOL_SEC = 88775.2; // 1 Mars sol in seconds (24h 39m 35s)
    const SOLS_PER_YEAR = 668.6;
    const P_YEAR_SEC = P_SOL_SEC * SOLS_PER_YEAR;

    // Volumetric heat capacity C_v = rho * Cp
    const Cv = rho * Cp;

    // Thermal diffusivity kappa = (I / Cv)^2 (m^2/s)
    const kappaM2S = Math.pow(I / Cv, 2.0);

    // Diurnal and seasonal skin depths d_s = (I / Cv) * sqrt(P / pi) (m)
    const dsDiurnalM = (I / Cv) * Math.sqrt(P_SOL_SEC / Math.PI);
    const dsDiurnalCm = dsDiurnalM * 100.0;

    const dsSeasonalM = (I / Cv) * Math.sqrt(P_YEAR_SEC / Math.PI);

    // Attenuation fractions: exp(-z / ds)
    const dampDiurnal = Math.exp(-z / dsDiurnalM);
    const dampSeasonal = Math.exp(-z / dsSeasonalM);

    // Diurnal phase lag in hours: (z / ds) / (2*pi) * 24.66 hours
    const phaseLagRad = z / dsDiurnalM;
    const lagHours = (phaseLagRad / (2.0 * Math.PI)) * 24.6598;

    let regime = 'Fine Dust Mantle (Extremely Shallow Diurnal Penetration < 3 cm)';
    if (I >= 1200.0) {
      regime = 'Exposed Bedrock / Cemented Ground Ice (Deep Thermal Penetration > 10 cm Diurnal / > 2.5 m Annual)';
    } else if (I >= 400.0) {
      regime = 'Coarse Sand / Duricrust Regolith (Intermediate Thermal Inertia)';
    }

    return {
      thermalDiffusivityM2S: parseFloat(kappaM2S.toExponential(4)),
      diurnalSkinDepthCm: parseFloat(dsDiurnalCm.toFixed(2)),
      seasonalSkinDepthMeters: parseFloat(dsSeasonalM.toFixed(3)),
      diurnalAmplitudeDampingFraction: parseFloat(dampDiurnal.toFixed(4)),
      seasonalAmplitudeDampingFraction: parseFloat(dampSeasonal.toFixed(4)),
      diurnalPhaseLagHours: parseFloat(lagHours.toFixed(2)),
      thermalRegimeDescription: regime
    };
  }

  /**
   * Calculate 2-layer salt duricrust mantle over pore-filling ground ice conductive thermal gradient and interface stability.
   * dT_salt/dz = Q_geo / k_salt
   * T_int = T_surf + (dT_salt/dz) * L_salt
   * dT_ice/dz = Q_geo / k_ice
   * T(z) = T_int + (dT_ice/dz) * ( z - L_salt )
   * Reference: Osterloo et al. (2008), Glotch et al. (2010), Bandfield et al. (2011) for THEMIS chloride evaporite basins.
   * @param {number} surfaceTempK - Surface ground temperature in K (170 to 240 K)
   * @param {number} [saltMantleThicknessMeters=0.10] - Salt duricrust thickness L_salt in meters (0.01 to 2.0 m)
   * @param {number} [saltThermalConductivityWMK=0.60] - Salt duricrust thermal conductivity in W/(m*K) (0.3 to 1.2 W/m/K)
   * @param {number} [iceSubstrateConductivityWMK=2.50] - Ice substrate thermal conductivity in W/(m*K)
   * @param {number} [targetDepthMeters=1.00] - Target evaluation depth in meters (z >= L_salt)
   * @param {number} [geothermalHeatFluxMWM2=25.0] - Geothermal heat flux in mW/m^2
   * @returns {{interfaceTempK: number, targetDepthTempK: number, saltLayerGradientKPerKm: number, iceLayerGradientKPerKm: number, totalThermalResistanceM2KW: number, saltDepositContext: string}}
   */
  static computeSaltDuricrustThermalProfile(surfaceTempK, saltMantleThicknessMeters = 0.10, saltThermalConductivityWMK = 0.60, iceSubstrateConductivityWMK = 2.50, targetDepthMeters = 1.00, geothermalHeatFluxMWM2 = 25.0) {
    const Tsurf = Math.max(100.0, Math.min(270.0, surfaceTempK));
    const Lsalt = Math.max(0.005, saltMantleThicknessMeters);
    const kSalt = Math.max(0.1, saltThermalConductivityWMK);
    const kIce = Math.max(0.5, iceSubstrateConductivityWMK);
    const zTarget = Math.max(Lsalt, targetDepthMeters);
    const QgeoW = Math.max(1.0, geothermalHeatFluxMWM2) * 1e-3; // W/m^2

    // Thermal gradients
    const gradSaltKPerM = QgeoW / kSalt;
    const gradSaltKPerKm = gradSaltKPerM * 1000.0;

    const gradIceKPerM = QgeoW / kIce;
    const gradIceKPerKm = gradIceKPerM * 1000.0;

    // Interface and target depth temperatures
    const Tint = Tsurf + gradSaltKPerM * Lsalt;
    const Tz = Tint + gradIceKPerM * (zTarget - Lsalt);

    // Thermal resistance in series R = L_salt/k_salt + (z - L_salt)/k_ice
    const rTh = (Lsalt / kSalt) + ((zTarget - Lsalt) / kIce);

    let context = 'Chloride Salt Evaporite Duricrust Mantle over Massive Subsurface Ground Ice';
    if (kSalt >= 1.0) {
      context = 'Dense Halite / Anhydrite Consolidated Salt Bed over Bedrock';
    }

    return {
      interfaceTempK: parseFloat(Tint.toFixed(4)),
      targetDepthTempK: parseFloat(Tz.toFixed(4)),
      saltLayerGradientKPerKm: parseFloat(gradSaltKPerKm.toFixed(2)),
      iceLayerGradientKPerKm: parseFloat(gradIceKPerKm.toFixed(2)),
      totalThermalResistanceM2KW: parseFloat(rTh.toFixed(4)),
      saltDepositContext: context
    };
  }

  /**
   * Calculate non-linear subsurface thermal conduction profile with temperature-dependent thermal conductivity k(T) = k0 * (T0/T)^n.
   * For n = 1: T(z) = T_surf * exp( Q_geo * z / (k0 * T0) )
   * For n != 1: T(z) = [ T_surf^(1-n) + (1-n) * Q_geo * z / (k0 * T0^n) ]^( 1 / (1-n) )
   * Reference: Ross et al. (1978), Klinger (1980), Clifford (1993), Kieffer (2013) for phonon-scattering cryosphere thermal profiles.
   * @param {number} surfaceTempK - Surface ground/ice temperature in K (120 to 250 K)
   * @param {number} [targetDepthMeters=1000.0] - Target crustal evaluation depth in meters (10 to 5000 m)
   * @param {number} [geothermalHeatFluxMWM2=25.0] - Geothermal heat flux in mW/m^2 (10 to 50 mW/m^2)
   * @param {number} [conductivityRefK0=2.22] - Reference thermal conductivity k0 in W/(m*K) at T0
   * @param {number} [tempExponentN=1.0] - Temperature exponent n (1.0 for crystalline H2O ice, 0.5-0.7 for basalt)
   * @returns {{nonLinearTargetTempK: number, linearModelTargetTempK: number, nonLinearMeanGradientKPerKm: number, thermalNonLinearityDeltaTK: number, effectiveConductivityAtDepthWMK: number, lithosphereMediumContext: string}}
   */
  static computeTemperatureDependentConductivityThermalProfile(surfaceTempK, targetDepthMeters = 1000.0, geothermalHeatFluxMWM2 = 25.0, conductivityRefK0 = 2.22, tempExponentN = 1.0) {
    const Tsurf = Math.max(80.0, Math.min(270.0, surfaceTempK));
    const zM = Math.max(1.0, targetDepthMeters);
    const QgeoW = Math.max(1.0, geothermalHeatFluxMWM2) * 1e-3; // W/m^2
    const k0 = Math.max(0.01, conductivityRefK0);
    const n = Math.max(0.0, Math.min(2.0, tempExponentN));
    const T0 = 273.15; // K

    let TzNonLinear = Tsurf;

    if (Math.abs(n - 1.0) < 1e-4) {
      // Pure water ice (n = 1.0): T(z) = Tsurf * exp( (Q_geo * z) / (k0 * T0) )
      const arg = (QgeoW * zM) / (k0 * T0);
      TzNonLinear = Tsurf * Math.exp(arg);
    } else {
      // General power law (n != 1.0): T(z) = [ Tsurf^(1-n) + (1-n)*Q_geo*z / (k0 * T0^n) ]^(1/(1-n))
      const term1 = Math.pow(Tsurf, 1.0 - n);
      const term2 = ((1.0 - n) * QgeoW * zM) / (k0 * Math.pow(T0, n));
      TzNonLinear = Math.pow(Math.max(1.0, term1 + term2), 1.0 / (1.0 - n));
    }

    // Linear model baseline using surface conductivity k(Tsurf): Tz_linear = Tsurf + (Q_geo / kSurf) * z
    const kSurf = k0 * Math.pow(T0 / Tsurf, n);
    const TzLinear = Tsurf + (QgeoW / kSurf) * zM;

    const deltaT = TzNonLinear - TzLinear;
    const meanGradKPerKm = ((TzNonLinear - Tsurf) / zM) * 1000.0;

    // Effective conductivity at depth k(Tz) = k0 * (T0 / Tz)^n
    const kDepth = k0 * Math.pow(T0 / TzNonLinear, n);

    let medium = 'Pure Crystalline H2O Ice Sheet (Umklapp Phonon Scattering k ~ 1/T)';
    if (n < 0.8) {
      medium = 'Dense Basaltic / Anorthositic Igneous Crust (Intermediate Phonon Dispersion)';
    }

    return {
      nonLinearTargetTempK: parseFloat(TzNonLinear.toFixed(3)),
      linearModelTargetTempK: parseFloat(TzLinear.toFixed(3)),
      nonLinearMeanGradientKPerKm: parseFloat(meanGradKPerKm.toFixed(2)),
      thermalNonLinearityDeltaTK: parseFloat(deltaT.toFixed(3)),
      effectiveConductivityAtDepthWMK: parseFloat(kDepth.toFixed(3)),
      lithosphereMediumContext: medium
    };
  }

  /**
   * Calculate subsurface Methane Clathrate Hydrate Stability Zone (MHSZ) upper and lower depth boundaries and reservoir thickness.
   * ln( P_eq / 1 MPa ) = 38.980 - 8533.80 / T
   * P(z) = P_surf + rho_reg * g_mars * z
   * T(z) = T_surf + ( Q_geo / k_reg ) * z
   * Reference: Sloan (1998), Max & Clifford (2000), Chastain & Chevrier (2007), Mousis et al. (2013) for Martian cryosphere gas hydrates.
   * @param {number} [surfaceTempK=180.0] - Mean annual surface ground temperature in K (150 to 220 K)
   * @param {number} [surfacePressurePa=610.0] - Atmospheric surface pressure in Pa (400 to 1200 Pa)
   * @param {number} [geothermalHeatFluxMWM2=25.0] - Geothermal heat flux in mW/m^2 (15 to 40 mW/m^2)
   * @param {number} [regolithThermalConductivityWMK=2.0] - Bulk thermal conductivity in W/(m*K)
   * @param {number} [regolithBulkDensityKgM3=1800.0] - Cryosphere bulk density in kg/m^3
   * @returns {{topDepthMeters: number, bottomDepthMeters: number, mhszThicknessMeters: number, maxStabilityTempK: number, clathrateTrappingPotential: string}}
   */
  static computeMethaneClathrateHydrateStabilityZone(surfaceTempK = 180.0, surfacePressurePa = 610.0, geothermalHeatFluxMWM2 = 25.0, regolithThermalConductivityWMK = 2.0, regolithBulkDensityKgM3 = 1800.0) {
    const Tsurf = Math.max(120.0, Math.min(240.0, surfaceTempK));
    const Psurf = Math.max(100.0, surfacePressurePa);
    const Qgeo = Math.max(1.0, geothermalHeatFluxMWM2) * 1e-3; // W/m^2
    const kReg = Math.max(0.1, regolithThermalConductivityWMK);
    const rho = Math.max(500.0, regolithBulkDensityKgM3);

    const gMars = 3.72076; // m/s^2
    const thermalGradKPerM = Qgeo / kReg;

    // Search for upper stability boundary z_top and lower boundary z_bottom (0 to 10000 m in 5m steps)
    let zTop = -1;
    let zBottom = -1;
    let maxT = Tsurf;

    for (let z = 1; z <= 10000; z += 5) {
      const PzPa = Psurf + rho * gMars * z;
      const PzMPa = PzPa / 1e6;
      const TzK = Tsurf + thermalGradKPerM * z;

      // Phase equilibrium temperature Teq(P): Teq = 8533.80 / ( 38.980 - ln(P_MPa) )
      const denom = 38.980 - Math.log(PzMPa);
      const TeqK = denom > 0.0 ? 8533.80 / denom : 0.0;

      // Hydrate is stable when actual temperature Tz <= Teq(P) and pressure >= equilibrium pressure
      const isStable = (TzK <= TeqK) && (TzK < 273.15);

      if (isStable) {
        if (zTop < 0) {
          zTop = z;
        }
        zBottom = z;
        if (TzK > maxT) {
          maxT = TzK;
        }
      }
    }

    const topMeters = zTop > 0 ? zTop : 0;
    const bottomMeters = zBottom > 0 ? zBottom : 0;
    const thicknessM = Math.max(0, bottomMeters - topMeters);

    let potential = 'Massive Planetary Methane & Trace Gas Clathrate Reservoir';
    if (thicknessM < 500) {
      potential = 'Marginal Shallow Hydrate Stability Zone (Restricted to High-Latitude Permafrost)';
    }

    return {
      topDepthMeters: topMeters,
      bottomDepthMeters: bottomMeters,
      mhszThicknessMeters: thicknessM,
      maxStabilityTempK: parseFloat(maxT.toFixed(2)),
      clathrateTrappingPotential: potential
    };
  }

  /**
   * Calculate subsurface Carbon Dioxide Clathrate Hydrate Stability Zone (CHSZ) upper and lower depth boundaries and polar reservoir thickness.
   * ln( P_eq / 1 MPa ) = 33.910 - 6895.0 / T
   * P(z) = P_surf + rho_ice * g_mars * z
   * T(z) = T_surf + ( Q_geo / k_ice ) * z
   * Reference: Miller & Smythe (1970), Sloan (1998), Longhi (2006), Mousis et al. (2012) for Martian polar clathrate sequestration.
   * @param {number} [surfaceTempK=150.0] - Mean annual polar/permafrost surface temperature in K (120 to 200 K)
   * @param {number} [surfacePressurePa=610.0] - Atmospheric surface pressure in Pa
   * @param {number} [geothermalHeatFluxMWM2=25.0] - Geothermal heat flux in mW/m^2
   * @param {number} [iceThermalConductivityWMK=2.5] - Polar ice sheet thermal conductivity in W/(m*K)
   * @param {number} [iceBulkDensityKgM3=1200.0] - Polar ice/dust bulk density in kg/m^3
   * @returns {{topDepthMeters: number, bottomDepthMeters: number, chszThicknessMeters: number, maxStabilityTempK: number, co2TrappingPotential: string}}
   */
  static computeCarbonDioxideClathrateHydrateStabilityZone(surfaceTempK = 150.0, surfacePressurePa = 610.0, geothermalHeatFluxMWM2 = 25.0, iceThermalConductivityWMK = 2.5, iceBulkDensityKgM3 = 1200.0) {
    const Tsurf = Math.max(100.0, Math.min(220.0, surfaceTempK));
    const Psurf = Math.max(100.0, surfacePressurePa);
    const Qgeo = Math.max(1.0, geothermalHeatFluxMWM2) * 1e-3; // W/m^2
    const kIce = Math.max(0.1, iceThermalConductivityWMK);
    const rho = Math.max(500.0, iceBulkDensityKgM3);

    const gMars = 3.72076; // m/s^2
    const thermalGradKPerM = Qgeo / kIce;

    let zTop = -1;
    let zBottom = -1;
    let maxT = Tsurf;

    for (let z = 1; z <= 10000; z += 5) {
      const PzPa = Psurf + rho * gMars * z;
      const PzMPa = PzPa / 1e6;
      const TzK = Tsurf + thermalGradKPerM * z;

      // Phase equilibrium temperature Teq(P): Teq = 6895.0 / ( 33.910 - ln(P_MPa) )
      const denom = 33.910 - Math.log(PzMPa);
      const TeqK = denom > 0.0 ? 6895.0 / denom : 0.0;

      const isStable = (TzK <= TeqK) && (TzK < 273.15);

      if (isStable) {
        if (zTop < 0) {
          zTop = z;
        }
        zBottom = z;
        if (TzK > maxT) {
          maxT = TzK;
        }
      }
    }

    const topMeters = zTop > 0 ? zTop : 0;
    const bottomMeters = zBottom > 0 ? zBottom : 0;
    const thicknessM = Math.max(0, bottomMeters - topMeters);

    let potential = 'Massive Polar CO2 Clathrate Sequestration Reservoir';
    if (thicknessM < 500) {
      potential = 'Marginal Shallow CO2 Clathrate Stability Zone';
    }

    return {
      topDepthMeters: topMeters,
      bottomDepthMeters: bottomMeters,
      chszThicknessMeters: thicknessM,
      maxStabilityTempK: parseFloat(maxT.toFixed(2)),
      co2TrappingPotential: potential
    };
  }

  /**
   * Calculate non-isothermal seasonal thermal wave pore ice sublimation kinetics, Clausius-Clapeyron non-linear enhancement, and annual vapor flux.
   * T_ice(t) = T_mean + Delta_T * exp( -z/d_s ) * cos( omega*t - z/d_s )
   * <rho_sat> = ( 1 / 2*pi ) * integral( rho_sat( T_ice(t) ) dt )
   * J_annual = ( D_eff / z_ice ) * ( <rho_sat> - rho_atm )
   * Reference: Mellon & Jakosky (1993, 1995), Schorghofer & Aharonson (2005), Chamberlain & Boynton (2007) for Martian permafrost vapor dynamics.
   * @param {number} [meanSurfaceTempK=210.0] - Annual mean surface temperature in K (150 to 240 K)
   * @param {number} [annualTempAmplitudeK=30.0] - Annual seasonal temperature oscillation amplitude in K (5 to 60 K)
   * @param {number} [iceFrontDepthMeters=0.20] - Ice table depth in meters (0.02 to 2.0 m)
   * @param {number} [porosityPct=40.0] - Regolith porosity percentage
   * @param {number} [thermalInertiaTIU=250.0] - Regolith thermal inertia in tiu
   * @returns {{meanIceFrontTempK: number, iceTempAmplitudeK: number, annualMeanVaporDensityKgM3: number, isothermalVaporDensityKgM3: number, nonLinearThermalEnhancementFactor: number, annualVaporMassFluxKgM2S: number, iceStabilityAssessment: string}}
   */
  static computeSeasonalHarmonicSublimationPoreIceDiffusion(meanSurfaceTempK = 210.0, annualTempAmplitudeK = 30.0, iceFrontDepthMeters = 0.20, porosityPct = 40.0, thermalInertiaTIU = 250.0) {
    const Tmean = Math.max(120.0, Math.min(260.0, meanSurfaceTempK));
    const deltaTAnn = Math.max(0.0, Math.min(80.0, annualTempAmplitudeK));
    const zIce = Math.max(0.01, iceFrontDepthMeters);
    const phi = Math.max(0.05, Math.min(0.80, porosityPct / 100.0));
    const I = Math.max(20.0, thermalInertiaTIU);

    const R_GAS = 8.314462;
    const M_H2O = 0.018015;
    const L_SUB = 51058.0; // J/mol
    const P_SOL_SEC = 88775.2;
    const SOLS_YEAR = 668.6;
    const P_YEAR_SEC = P_SOL_SEC * SOLS_YEAR;
    const Cv = 1500.0 * 800.0; // typical Cv = 1.2e6 J/m^3/K

    // Seasonal skin depth ds (m)
    const dsM = (I / Cv) * Math.sqrt(P_YEAR_SEC / Math.PI);

    // Thermal amplitude at ice front
    const ampIce = deltaTAnn * Math.exp(-zIce / dsM);
    const phaseLag = zIce / dsM;

    // Numerical integration across seasonal cycle (120 steps)
    const STEPS = 120;
    let sumRhoSat = 0.0;

    for (let i = 0; i < STEPS; i++) {
      const omegaT = (2.0 * Math.PI * i) / STEPS;
      const Tice = Tmean + ampIce * Math.cos(omegaT - phaseLag);

      // Saturation vapor pressure Psat(Tice)
      const pSatPa = 611.65 * Math.exp(-(L_SUB / R_GAS) * ((1.0 / Tice) - (1.0 / 273.16)));
      const rhoSat = (pSatPa * M_H2O) / (R_GAS * Tice);
      sumRhoSat += rhoSat;
    }

    const meanRhoSat = sumRhoSat / STEPS;

    // Isothermal baseline at Tmean
    const pSatMean = 611.65 * Math.exp(-(L_SUB / R_GAS) * ((1.0 / Tmean) - (1.0 / 273.16)));
    const isoRhoSat = (pSatMean * M_H2O) / (R_GAS * Tmean);

    // Non-linear enhancement factor due to Clausius-Clapeyron exponential curvature
    const enhanceFactor = isoRhoSat > 0.0 ? meanRhoSat / isoRhoSat : 1.0;

    // Effective Knudsen diffusivity D_eff (m^2/s)
    const vTh = Math.sqrt((8.0 * R_GAS * Tmean) / (Math.PI * M_H2O));
    const rPoreM = 5.0e-6; // 5 microns
    const tau = 2.0;
    const dEffM2S = (phi / (tau * tau)) * (2.0 / 3.0) * rPoreM * vTh;

    // Net vapor flux (assuming atmospheric background is 10% of mean saturation)
    const rhoAtm = meanRhoSat * 0.10;
    const netFluxKgM2S = (dEffM2S / zIce) * (meanRhoSat - rhoAtm);

    let assessment = 'Active Seasonal Sublimation Loss (Warm Mid-Latitude Ground Ice)';
    if (enhanceFactor > 2.0) {
      assessment = 'Severe Summer Thermal Wave Sublimation Pumping (High Non-Linear Vapor Outgassing)';
    } else if (enhanceFactor <= 1.2 && Tmean < 190.0) {
      assessment = 'Perennially Stable Cryosphere (Sub-Surface Ice Stable over Milankovitch Cycles)';
    }

    return {
      meanIceFrontTempK: parseFloat(Tmean.toFixed(2)),
      iceTempAmplitudeK: parseFloat(ampIce.toFixed(2)),
      annualMeanVaporDensityKgM3: parseFloat(meanRhoSat.toExponential(4)),
      isothermalVaporDensityKgM3: parseFloat(isoRhoSat.toExponential(4)),
      nonLinearThermalEnhancementFactor: parseFloat(enhanceFactor.toFixed(3)),
      annualVaporMassFluxKgM2S: parseFloat(netFluxKgM2S.toExponential(4)),
      iceStabilityAssessment: assessment
    };
  }

  /**
   * Calculate ground ice equilibrium stability depth and paleoclimate migration under 120-kyr Milankovitch planetary obliquity oscillations.
   * F_mean(theta, eps) = ( S_0 / pi ) * [ cos(theta)*sin(eps) + (pi/2 - |theta|)*sin(theta)*cos(eps) ]
   * T_surf = ( (1 - A) * F_mean / ( eps_th * sigma ) )^(1/4)
   * Reference: Ward (1974), Laskar et al. (2004), Head et al. (2003), Schorghofer (2007) for tropical glaciation & polar cap desiccation.
   * @param {number} [obliquityDeg=25.2] - Planetary spin-axis obliquity epsilon in degrees (10.0 to 55.0 deg)
   * @param {number} [latitudeDeg=45.0] - Target planetocentric latitude theta in degrees (-90 to +90 deg)
   * @param {number} [thermalInertiaTIU=250.0] - Regolith thermal inertia in tiu
   * @param {number} [atmosphericHumidityPpm=150.0] - Atmospheric water vapor column in pr-um / ppm
   * @returns {{annualMeanInsolationWM2: number, equilibriumSurfaceTempK: number, iceTableStabilityDepthMeters: number, paleoclimateGlacialRegime: string, tropicalGlaciationPotential: string}}
   */
  static computeMilankovitchObliquityIceStabilityDepth(obliquityDeg = 25.2, latitudeDeg = 45.0, thermalInertiaTIU = 250.0, atmosphericHumidityPpm = 150.0) {
    const eps = Math.max(5.0, Math.min(65.0, obliquityDeg)) * (Math.PI / 180.0);
    const theta = Math.max(-89.9, Math.min(89.9, latitudeDeg)) * (Math.PI / 180.0);
    const absLat = Math.abs(theta);

    const S0 = 590.0; // W/m^2 at Mars mean heliocentric distance (1.524 AU)
    const A = 0.25; // mean Bond albedo
    const emiss = 0.95;
    const sigma = 5.670374e-8; // W/m^2/K^4

    // Approximate annual mean top-of-atmosphere insolation
    const term1 = Math.cos(absLat) * Math.sin(eps);
    const term2 = (Math.PI / 2.0 - absLat) * Math.sin(absLat) * Math.cos(eps);
    const fMean = (S0 / Math.PI) * Math.max(0.1, term1 + term2);

    // Equilibrium surface temperature
    const Tsurf = Math.pow(((1.0 - A) * fMean) / (emiss * sigma), 0.25);

    // Frost point temperature based on atmospheric water vapor (~195 K for 150 ppm)
    const Tfrost = 195.0 + Math.log(Math.max(10.0, atmosphericHumidityPpm) / 150.0) * 2.5;

    // Equilibrium ice table depth (m)
    let zIceM = 0.0;
    if (Tsurf > Tfrost) {
      zIceM = Math.min(5.0, ((Tsurf - Tfrost) / Tfrost) * 2.5);
    }

    let regime = 'Mid-Latitude Permafrost Ice Table (Current Astronomical Epoch)';
    let glaciation = 'Subsurface Cryosphere Stable';

    if (obliquityDeg >= 35.0) {
      regime = 'High-Obliquity Tropical Glacial Epoch (Polar Desiccation & Equatorial Snowpack Deposition)';
      if (Math.abs(latitudeDeg) <= 30.0) {
        glaciation = 'Active Tropical Valley Glaciation & Tharsis Fan-Shaped Glacial Aprons';
        zIceM = 0.0; // massive surface ice sheets at equator!
      } else {
        glaciation = 'Polar Ice Sheet Sublimation Deflation';
      }
    } else if (obliquityDeg <= 15.0) {
      regime = 'Low-Obliquity Polar Cold-Trap Freeze-Out (Deep Equatorial Regolith Desiccation)';
      if (Math.abs(latitudeDeg) <= 45.0) {
        glaciation = 'Complete Low-Latitude Cryosphere Desiccation (> 1 m lag)';
      }
    }

    return {
      annualMeanInsolationWM2: parseFloat(fMean.toFixed(2)),
      equilibriumSurfaceTempK: parseFloat(Tsurf.toFixed(2)),
      iceTableStabilityDepthMeters: parseFloat(zIceM.toFixed(3)),
      paleoclimateGlacialRegime: regime,
      tropicalGlaciationPotential: glaciation
    };
  }

  /**
   * Calculate porous regolith gas permeability, Klinkenberg slip correction, barometric penetration skin depth, and diurnal atmospheric pumping volume.
   * k_perm = ( phi^3 / ( 180 * (1 - phi)^2 ) ) * d_grain^2
   * delta_baro = sqrt( ( 2 * k_gas * P_0 ) / ( phi * mu_gas * omega_diurnal ) )
   * V_pump = phi * delta_baro * ( Delta_P / P_0 )
   * Reference: Schorghofer & Aharonson (2005), Massé et al. (2014), Hudson et al. (2007) for Martian atmospheric gas-soil exchange.
   * @param {number} [meanGrainRadiusMicrons=50.0] - Effective regolith grain radius in microns (5 to 1000 um)
   * @param {number} [porosityPct=40.0] - Bulk porosity percentage (10 to 70%)
   * @param {number} [diurnalPressureAmplitudePa=30.0] - Diurnal thermal tide pressure variation in Pa (5 to 100 Pa)
   * @param {number} [meanSurfacePressurePa=610.0] - Mean ambient atmospheric pressure in Pa
   * @returns {{intrinsicPermeabilityM2: number, permeabilityInDarcies: number, klinkenbergPermeabilityM2: number, barometricSkinDepthMeters: number, diurnalGasExchangeVolumeM3PerM2: number, regolithPoreVentilationRegime: string}}
   */
  static computeSubsurfaceBarometricPumpingAndPermeability(meanGrainRadiusMicrons = 50.0, porosityPct = 40.0, diurnalPressureAmplitudePa = 30.0, meanSurfacePressurePa = 610.0) {
    const rGrainM = Math.max(1.0, meanGrainRadiusMicrons) * 1e-6;
    const dGrainM = 2.0 * rGrainM;
    const phi = Math.max(0.05, Math.min(0.80, porosityPct / 100.0));
    const deltaP = Math.max(1.0, diurnalPressureAmplitudePa);
    const P0 = Math.max(100.0, meanSurfacePressurePa);

    const P_SOL_SEC = 88775.2; // Mars sol (seconds)
    const omegaDiurnal = (2.0 * Math.PI) / P_SOL_SEC;
    const muCO2 = 1.2e-5; // dynamic viscosity of CO2 at 210 K (Pa*s)
    const DARCY_M2 = 9.869233e-13; // 1 Darcy in m^2

    // Kozeny-Carman intrinsic permeability (m^2)
    const kozenyFactor = Math.pow(phi, 3.0) / (180.0 * Math.pow(1.0 - phi, 2.0));
    const kPermM2 = kozenyFactor * Math.pow(dGrainM, 2.0);
    const kDarcies = kPermM2 / DARCY_M2;

    // Klinkenberg slip correction: k_gas = k_perm * (1 + b_k / P0)
    const bKlinkenbergPa = 450.0;
    const slipFactor = 1.0 + (bKlinkenbergPa / P0);
    const kGasM2 = kPermM2 * slipFactor;

    // Barometric pressure penetration skin depth delta_baro = sqrt( (2 * k_gas * P0) / (phi * mu * omega) ) (m)
    const denom = phi * muCO2 * omegaDiurnal;
    const deltaBaroM = Math.sqrt((2.0 * kGasM2 * P0) / Math.max(1e-20, denom));

    // Diurnal atmospheric forced exchange volume per unit surface area V_pump = phi * delta_baro * (deltaP / P0) (m^3/m^2)
    const vPumpM3M2 = phi * deltaBaroM * (deltaP / P0);

    let regime = 'Moderate Gas Permeability & Diurnal Subsurface Breathing';
    if (deltaBaroM > 15.0) {
      regime = 'High-Permeability Coarse Megaregolith (Deep Gas Advection & Rapid Pore Flushing)';
    } else if (deltaBaroM < 1.0) {
      regime = 'Tight Fine-Grained Dust Seal (Diffusion-Dominated Subsurface)';
    }

    return {
      intrinsicPermeabilityM2: parseFloat(kPermM2.toExponential(4)),
      permeabilityInDarcies: parseFloat(kDarcies.toFixed(2)),
      klinkenbergPermeabilityM2: parseFloat(kGasM2.toExponential(4)),
      barometricSkinDepthMeters: parseFloat(deltaBaroM.toFixed(2)),
      diurnalGasExchangeVolumeM3PerM2: parseFloat(vPumpM3M2.toFixed(4)),
      regolithPoreVentilationRegime: regime
    };
  }

  /**
   * Calculate transient liquid brine flow metastability, evaporative boiling flux, Stefan freezing front kinetics, and liquid survival lifetime.
   * E_evap = 0.17 * ( ( P_sat - P_atm ) / P_atm ) * rho_atm * v_wind
   * t_freeze = ( rho_brine * L_f * z^2 ) / ( 2 * k_ice * ( T_eut - T_ambient ) )
   * Reference: Ingersoll (1970), Sears & Chittenden (2005), Chevrier et al. (2007, 2009), Toner & Catling (2016) for Martian RSL and gullies.
   * @param {number} [surfaceTempK=230.0] - Surface/regolith temperature in K (180 to 280 K)
   * @param {number} [ambientPressurePa=610.0] - Atmospheric ambient pressure in Pa
   * @param {number} [brineLayerThicknessCm=1.0] - Brine flow or pore layer thickness in cm (0.1 to 20 cm)
   * @param {string} [brineSaltType='mg_perchlorate'] - Brine electrolyte: 'mg_perchlorate', 'ca_perchlorate', 'cacl2', 'nacl', or 'mgso4'
   * @param {number} [windSpeedMS=5.0] - Surface wind speed in m/s
   * @returns {{eutecticTempK: number, isLiquidThermodynamicallyStable: boolean, evaporativeBoilingFluxKgM2S: number, freezingLifetimeHours: number, evaporationLifetimeHours: number, liquidPersistenceLifetimeHours: number, rslBrineSurvivalRegime: string}}
   */
  static computeTransientBrineMetastabilityAndFreezingLifetime(surfaceTempK = 230.0, ambientPressurePa = 610.0, brineLayerThicknessCm = 1.0, brineSaltType = 'mg_perchlorate', windSpeedMS = 5.0) {
    const T = Math.max(150.0, Math.min(300.0, surfaceTempK));
    const Patm = Math.max(100.0, ambientPressurePa);
    const zM = Math.max(0.001, brineLayerThicknessCm * 0.01);
    const vWind = Math.max(0.1, windSpeedMS);

    // Eutectic temperatures for Martian brines
    let Teut = 206.0; // Mg(ClO4)2
    let rhoBrine = 1350.0; // kg/m^3
    const type = (brineSaltType || 'mg_perchlorate').toLowerCase();

    if (type.includes('ca_perchlorate')) {
      Teut = 198.0;
      rhoBrine = 1400.0;
    } else if (type.includes('cacl2')) {
      Teut = 223.0;
      rhoBrine = 1300.0;
    } else if (type.includes('nacl')) {
      Teut = 252.0;
      rhoBrine = 1200.0;
    } else if (type.includes('mgso4')) {
      Teut = 269.0;
      rhoBrine = 1250.0;
    }

    const isLiquid = T >= Teut;

    // Saturation vapor pressure of water over brine (Raoult's law factor ~0.65)
    const pSatPurePa = 611.65 * Math.exp(-(51058.0 / 8.314462) * ((1.0 / T) - (1.0 / 273.16)));
    const pSatBrinePa = pSatPurePa * 0.65;

    // Evaporative / boiling mass flux E_evap (kg/(m^2*s))
    const rhoAtm = (Patm * 0.044) / (8.314462 * T);
    let eFluxKgM2S = 1e-7;
    if (pSatBrinePa > Patm) {
      // Rapid boiling regime
      eFluxKgM2S = 0.17 * ((pSatBrinePa - Patm) / Patm) * rhoAtm * vWind;
    } else {
      // Sub-boiling diffusion & wind-driven evaporation
      eFluxKgM2S = 0.05 * (pSatBrinePa / Patm) * rhoAtm * vWind;
    }
    eFluxKgM2S = Math.max(1e-8, Math.min(0.01, eFluxKgM2S));

    // Evaporation lifetime t_evap = ( rhoBrine * z ) / E_evap (seconds)
    const tEvapSec = (rhoBrine * zM) / eFluxKgM2S;
    const tEvapHours = tEvapSec / 3600.0;

    // Stefan freezing front kinetics (if T < Teut or thermal quenching)
    const Lf = 3.34e5; // J/kg
    const kIce = 2.2; // W/(m*K)
    let tFreezeHours = 99999.0; // indefinitely liquid if T >= Teut

    if (T < Teut) {
      const deltaT = Math.max(0.5, Teut - T);
      const tFreezeSec = (rhoBrine * Lf * Math.pow(zM, 2.0)) / (2.0 * kIce * deltaT);
      tFreezeHours = tFreezeSec / 3600.0;
    }

    const tLiquidHours = isLiquid ? tEvapHours : Math.min(tEvapHours, tFreezeHours);

    let regime = 'Metastable Liquid Brine Flow Active (Perchlorate Low Eutectic)';
    if (!isLiquid) {
      regime = 'Cryogenic Freezing Lock (Sub-Eutectic Solidified Salt Crust)';
    } else if (tEvapHours < 2.0) {
      regime = 'Violent Evaporative Boiling & Effervescence (Desiccation in < 2 Hours)';
    }

    return {
      eutecticTempK: parseFloat(Teut.toFixed(1)),
      isLiquidThermodynamicallyStable: isLiquid,
      evaporativeBoilingFluxKgM2S: parseFloat(eFluxKgM2S.toExponential(4)),
      freezingLifetimeHours: parseFloat(tFreezeHours.toFixed(2)),
      evaporationLifetimeHours: parseFloat(tEvapHours.toFixed(2)),
      liquidPersistenceLifetimeHours: parseFloat(tLiquidHours.toFixed(2)),
      rslBrineSurvivalRegime: regime
    };
  }

  /**
   * Calculate 51-kyr precession of perihelion insolation asymmetry, hemispheric summer temperature extremes, and polar water ice cap stability.
   * F_summer(theta) = S_0 * ( ( 1 - e^2 ) / ( 1 + e*cos(nu) ) )^2 * sin(eps)
   * T_max = ( (1 - A) * F_summer / ( eps_th * sigma ) )^(1/4)
   * Reference: Laskar et al. (2004), Byrne (2009), Fastook et al. (2008) for Planum Boreum vs Planum Australe polar layered deposits.
   * @param {number} [obliquityDeg=25.2] - Planetary obliquity in degrees
   * @param {number} [orbitalEccentricity=0.0934] - Martian orbital eccentricity e (0.00 to 0.14)
   * @param {number} [longitudeOfPerihelionDeg=251.0] - Longitude of perihelion varpi in degrees (0 to 360 deg; currently 251 deg)
   * @param {number} [northPolarAlbedo=0.45] - North polar water ice cap summer albedo
   * @param {number} [southPolarAlbedo=0.70] - South polar CO2/dust ice cap summer albedo
   * @returns {{northPeakSummerInsolationWM2: number, southPeakSummerInsolationWM2: number, northPeakSummerTempK: number, southPeakSummerTempK: number, polarInsolationContrastPercent: number, dominantWaterIceAccumulationPole: string, precessionPhaseDescription: string}}
   */
  static computePrecessionInsolationAsymmetryAndPolarIceMoundGrowth(obliquityDeg = 25.2, orbitalEccentricity = 0.0934, longitudeOfPerihelionDeg = 251.0, northPolarAlbedo = 0.45, southPolarAlbedo = 0.70) {
    const eps = Math.max(10.0, Math.min(45.0, obliquityDeg)) * (Math.PI / 180.0);
    const e = Math.max(0.001, Math.min(0.20, orbitalEccentricity));
    const varpiRad = (longitudeOfPerihelionDeg || 251.0) * (Math.PI / 180.0);
    const Anorth = Math.max(0.1, Math.min(0.9, northPolarAlbedo));
    const Asouth = Math.max(0.1, Math.min(0.9, southPolarAlbedo));

    const S0 = 590.0; // W/m^2 at 1.524 AU
    const emiss = 0.95;
    const sigma = 5.670374e-8;

    // True anomaly at North summer solstice (Ls = 90 deg): nu_north = 90 - varpi
    const nuNorth = (90.0 * (Math.PI / 180.0)) - varpiRad;
    const rNormNorth = (1.0 - Math.pow(e, 2.0)) / (1.0 + e * Math.cos(nuNorth));
    const fNorth = (S0 / Math.pow(rNormNorth, 2.0)) * Math.sin(eps);

    // True anomaly at South summer solstice (Ls = 270 deg): nu_south = 270 - varpi
    const nuSouth = (270.0 * (Math.PI / 180.0)) - varpiRad;
    const rNormSouth = (1.0 - Math.pow(e, 2.0)) / (1.0 + e * Math.cos(nuSouth));
    const fSouth = (S0 / Math.pow(rNormSouth, 2.0)) * Math.sin(eps);

    // Peak summer surface temperatures
    const Tnorth = Math.pow(((1.0 - Anorth) * fNorth) / (emiss * sigma), 0.25);
    const Tsouth = Math.pow(((1.0 - Asouth) * fSouth) / (emiss * sigma), 0.25);

    const contrastPct = (Math.abs(fSouth - fNorth) / Math.min(fNorth, fSouth)) * 100.0;

    let pole = 'North Pole (Planum Boreum Perennial Massive Water Ice Mound)';
    let phase = 'Current Epoch: Southern Summer at Perihelion (Hot South Summer vs Cool North Summer)';

    if (fNorth > fSouth) {
      pole = 'South Pole (Planum Australe Massive Water Ice Accumulation)';
      phase = 'Opposite Precession Phase: Northern Summer at Perihelion (Water Ice Transferred to South Pole)';
    }

    return {
      northPeakSummerInsolationWM2: parseFloat(fNorth.toFixed(2)),
      southPeakSummerInsolationWM2: parseFloat(fSouth.toFixed(2)),
      northPeakSummerTempK: parseFloat(Tnorth.toFixed(2)),
      southPeakSummerTempK: parseFloat(Tsouth.toFixed(2)),
      polarInsolationContrastPercent: parseFloat(contrastPct.toFixed(2)),
      dominantWaterIceAccumulationPole: pole,
      precessionPhaseDescription: phase
    };
  }

  /**
   * Calculate diurnal nighttime water frost condensation onset, frost point temperature, nocturnal deposition thickness, and sunrise sublimation.
   * T_frost = [ (1 / 273.16) - ( R / L_sub ) * ln( P_vap / 611.65 ) ]^(-1)
   * m_frost = integral( alpha * sqrt( M / (2*pi*R*T) ) * ( P_vap - P_sat(T) ) dt )
   * Reference: Jakosky & Haberle (1992), Savijärvi et al. (2005), Piqueux et al. (2016) for Viking 2, Phoenix, and InSight diurnal frost cycles.
   * @param {number} [minNighttimeTempK=185.0] - Diurnal minimum surface temperature at dawn in K (150 to 220 K)
   * @param {number} [maxDaytimeTempK=240.0] - Diurnal maximum surface temperature in K (200 to 290 K)
   * @param {number} [atmosphericWaterColumnPrUm=20.0] - Column water vapor in precipitable microns (1 to 100 pr-um)
   * @param {number} [surfaceThermalInertiaTIU=250.0] - Regolith thermal inertia in tiu
   * @returns {{frostPointTempK: number, isNighttimeFrostFormed: boolean, frostDepositionDurationHours: number, peakFrostThicknessMicrons: number, morningSublimationTimeHours: number, diurnalHydrationRegime: string}}
   */
  static computeDiurnalFrostCondensationAndDewPointOnset(minNighttimeTempK = 185.0, maxDaytimeTempK = 240.0, atmosphericWaterColumnPrUm = 20.0, surfaceThermalInertiaTIU = 250.0) {
    const Tmin = Math.max(120.0, Math.min(230.0, minNighttimeTempK));
    const Tmax = Math.max(Tmin + 5.0, Math.min(300.0, maxDaytimeTempK));
    const prUm = Math.max(0.5, atmosphericWaterColumnPrUm);

    const R_GAS = 8.314462;
    const M_H2O = 0.018015;
    const L_SUB = 51058.0; // J/mol
    const alphaSticking = 0.80; // condensation sticking coefficient
    const rhoFrostKgM3 = 250.0; // porous microcrystalline surface frost

    // Surface partial pressure of water vapor P_vap (Pa) ~ prUm * 0.025 Pa / pr-um
    const pVapPa = prUm * 0.025;

    // Clausius-Clapeyron frost point temperature
    const denom = (1.0 / 273.16) - (R_GAS / L_SUB) * Math.log(pVapPa / 611.65);
    const Tfrost = denom > 0.0 ? 1.0 / denom : 180.0;

    const isFrostFormed = Tmin < Tfrost;

    let frostHours = 0.0;
    let totalFrostMassKgM2 = 0.0;
    const P_SOL_HOURS = 24.66;
    const STEPS = 100;
    const dtSec = (P_SOL_HOURS * 3600.0) / STEPS;

    // Approximate diurnal sinusoidal temperature profile
    for (let i = 0; i < STEPS; i++) {
      const frac = i / STEPS;
      const Tsurf = ((Tmax + Tmin) / 2.0) + ((Tmax - Tmin) / 2.0) * Math.sin(2.0 * Math.PI * frac - Math.PI / 2.0);

      if (Tsurf < Tfrost) {
        frostHours += P_SOL_HOURS / STEPS;

        // Saturation pressure at current surface temperature
        const pSatPa = 611.65 * Math.exp(-(L_SUB / R_GAS) * ((1.0 / Tsurf) - (1.0 / 273.16)));
        const deltaP = Math.max(0.0, pVapPa - pSatPa);

        // Hertz-Knudsen condensation flux (kg/(m^2*s))
        const fluxKgM2S = alphaSticking * Math.sqrt(M_H2O / (2.0 * Math.PI * R_GAS * Tsurf)) * deltaP;
        totalFrostMassKgM2 += fluxKgM2S * dtSec;
      }
    }

    // Peak frost thickness in microns
    const thicknessMicrons = (totalFrostMassKgM2 / rhoFrostKgM3) * 1e6;

    // Morning sublimation duration (typically 1-2 hours after sunrise)
    const morningSubHours = isFrostFormed ? Math.min(3.0, 0.5 + thicknessMicrons * 0.25) : 0.0;

    let regime = 'Nocturnal Surface Frost Cycle Active (Morning White Frost & Flash Vaporization)';
    if (!isFrostFormed) {
      regime = 'Arid Sub-Saturation (Nighttime Temperature Remains Above Frost Point)';
    } else if (thicknessMicrons > 10.0) {
      regime = 'Heavy Polar / High-Latitude Frost Sheet (Multi-Micron Crystalline Snowpack)';
    }

    return {
      frostPointTempK: parseFloat(Tfrost.toFixed(2)),
      isNighttimeFrostFormed: isFrostFormed,
      frostDepositionDurationHours: parseFloat(frostHours.toFixed(2)),
      peakFrostThicknessMicrons: parseFloat(thicknessMicrons.toFixed(3)),
      morningSublimationTimeHours: parseFloat(morningSubHours.toFixed(2)),
      diurnalHydrationRegime: regime
    };
  }

  /**
   * Calculate cryogenic interfacial premelted unfrozen liquid water film thickness, gravimetric liquid content, and water activity.
   * d_film = lambda_vdw * ( T_m / (T_m - T) )^(1/3) + lambda_elec * ( T_m / (T_m - T) )^(1/2)
   * W_unfrozen = S_spec * d_film * rho_water
   * Reference: Dash et al. (1995, 2006), Wettlaufer & Worster (2006), Sizemore et al. (2015), Mohapatra et al. (2021) for astrobiological habitability.
   * @param {number} [subsurfaceTempK=260.0] - Subsurface soil temperature in K (180 to 273.15 K)
   * @param {number} [specificSurfaceAreaM2G=25.0] - Regolith specific surface area in m^2/g (5 to 150 m^2/g)
   * @param {number} [soilSalinityMolar=0.05] - Pore water dissolved ionic salinity in mol/L
   * @returns {{interfacialFilmThicknessNm: number, molecularMonolayersCount: number, unfrozenWaterContentWtPercent: number, unfrozenWaterMgPerGSoil: number, waterActivityAw: number, habitabilityBiochemicalRegime: string}}
   */
  static computeInterfacialPremeltedUnfrozenWaterFilmThickness(subsurfaceTempK = 260.0, specificSurfaceAreaM2G = 25.0, soilSalinityMolar = 0.05) {
    const Tm = 273.15; // Bulk water melting point in K
    const T = Math.max(150.0, Math.min(273.10, subsurfaceTempK));
    const Sspec = Math.max(1.0, specificSurfaceAreaM2G);
    const deltaT = Math.max(0.05, Tm - T);

    const lambdaVdw = 0.65; // nm (Van der Waals dispersion coefficient)
    const lambdaElec = 0.40; // nm (electrostatic double layer coefficient)

    // Interfacial liquid film thickness d_film in nanometers
    const dVdw = lambdaVdw * Math.pow(Tm / deltaT, 1.0 / 3.0);
    const dElec = lambdaElec * Math.pow(Tm / deltaT, 0.5);
    const dFilmNm = dVdw + dElec;

    // Number of molecular water monolayers (1 monolayer ~ 0.30 nm)
    const monolayers = dFilmNm / 0.30;

    // Gravimetric unfrozen liquid water content (mg H2O / g soil and wt%)
    const wUnfrozenMgG = Sspec * dFilmNm; // mg/g
    const wUnfrozenWtPct = wUnfrozenMgG / 10.0; // 1000 mg = 100% -> /10 = wt%

    // Thermodynamic water activity aw = exp( - (Lf * deltaT) / (R * Tm * T) )
    const Lf = 6010.0; // J/mol latent heat of fusion
    const R_GAS = 8.314462;
    const aw = Math.max(0.10, Math.min(1.0, Math.exp(-(Lf * deltaT) / (R_GAS * Tm * T))));

    let habitability = 'Astrobiologically Permissive Interfacial Liquid Water (aw >= 0.60 Terrestrial Extremophile Limit)';
    if (aw < 0.60) {
      habitability = 'Cryogenic Thermodynamic Desiccation (aw < 0.60 Incompatible with Active Metabolism)';
    } else if (monolayers > 10.0) {
      habitability = 'Bulk-Like Mobile Interfacial Brine Layer (Active Solute Diffusion & Biomineralization)';
    }

    return {
      interfacialFilmThicknessNm: parseFloat(dFilmNm.toFixed(2)),
      molecularMonolayersCount: parseFloat(monolayers.toFixed(1)),
      unfrozenWaterContentWtPercent: parseFloat(wUnfrozenWtPct.toFixed(2)),
      unfrozenWaterMgPerGSoil: parseFloat(wUnfrozenMgG.toFixed(1)),
      waterActivityAw: parseFloat(aw.toFixed(3)),
      habitabilityBiochemicalRegime: habitability
    };
  }

  /**
   * Calculate Martian cryosphere basal melting depth, geothermal temperature gradient, and pore ice equivalent global water layer (GEL).
   * z_base = ( k_crust * ( T_base - T_surf ) ) / Q_geo
   * GEL = phi_0 * h_phi * ( 1 - exp( -z_base / h_phi ) )
   * Reference: Clifford (1993), Clifford et al. (2010), Lasue et al. (2013), Grimm et al. (2017) for MARSIS/SHARAD deep basal aquifers.
   * @param {number} [meanSurfaceTempK=215.0] - Mean annual surface temperature in K (150 to 240 K)
   * @param {number} [geothermalHeatFluxMWm2=25.0] - Geothermal heat flux in mW/m^2 (10 to 80 mW/m^2; present Mars is ~25 mW/m^2)
   * @param {number} [crustalThermalConductivityWMK=2.0] - Bulk crustal conductivity in W/(m*K) (1.5 to 3.0 W/(m*K))
   * @param {number} [surfacePorosityFraction=0.20] - Surface megaregolith porosity phi_0 (0.05 to 0.40)
   * @param {string} [poreFluidSalinity='pure_water'] - Pore fluid type ('pure_water', 'eutectic_brine')
   * @returns {{thermalGradientKPerKm: number, basalMeltingTempK: number, cryosphereThicknessKm: number, cryosphereThicknessMeters: number, poreIceGELMeters: number, subsurfaceAquiferStatus: string}}
   */
  static computeCryosphereBasalMeltingDepthAndGeothermalHeatFlux(meanSurfaceTempK = 215.0, geothermalHeatFluxMWm2 = 25.0, crustalThermalConductivityWMK = 2.0, surfacePorosityFraction = 0.20, poreFluidSalinity = 'pure_water') {
    const Tsurf = Math.max(120.0, Math.min(260.0, meanSurfaceTempK));
    const QgeoW = Math.max(5.0, geothermalHeatFluxMWm2) * 1e-3; // W/m^2
    const kCrust = Math.max(0.5, crustalThermalConductivityWMK);
    const phi0 = Math.max(0.01, Math.min(0.50, surfacePorosityFraction));

    // Geothermal gradient dT/dz = Q_geo / k_crust (K/m and K/km)
    const dTDzKPerM = QgeoW / kCrust;
    const dTDzKPerKm = dTDzKPerM * 1000.0;

    // Basal melting temperature (270 K for pure water with pressure depression, 206 K for eutectic perchlorate)
    let Tbase = 270.0;
    if (poreFluidSalinity.toLowerCase().includes('brine') || poreFluidSalinity.toLowerCase().includes('perchlorate')) {
      Tbase = 206.0;
    }

    const deltaT = Math.max(1.0, Tbase - Tsurf);
    const zBaseM = deltaT / dTDzKPerM;
    const zBaseKm = zBaseM / 1000.0;

    // Global Equivalent Layer (GEL) of pore ice: GEL = phi0 * h_phi * ( 1 - exp(-z_base / h_phi) )
    const hPhiM = 2800.0; // porosity decay scale depth in meters
    const gelMeters = phi0 * hPhiM * (1.0 - Math.exp(-zBaseM / hPhiM));

    let status = 'Deep Subpermafrost Basal Liquid Aquifer Feasible (MARSIS Analogue)';
    if (zBaseKm > 7.0) {
      status = 'Thick Cryogenic Permafrost Lock (Deep Cryosphere Seal > 7 km)';
    } else if (zBaseKm < 2.0) {
      status = 'Shallow Hydrothermal / Elevated Heat Flux Liquefaction Zone';
    }

    return {
      thermalGradientKPerKm: parseFloat(dTDzKPerKm.toFixed(2)),
      basalMeltingTempK: parseFloat(Tbase.toFixed(1)),
      cryosphereThicknessKm: parseFloat(zBaseKm.toFixed(2)),
      cryosphereThicknessMeters: parseFloat(zBaseM.toFixed(1)),
      poreIceGELMeters: parseFloat(gelMeters.toFixed(1)),
      subsurfaceAquiferStatus: status
    };
  }

  /**
   * Calculate impact crater hydrothermal system convective circulation lifetime, Rayleigh number, and mineral alteration duration.
   * V_melt = 0.0002 * D^3.83
   * Ra = ( rho^2 * c_p * g * alpha * deltaT * k_perm * L ) / ( mu_fluid * k_m )
   * t_hydro = tau_cond / Nu
   * Reference: Abramov & Kring (2005), Schwenzer & Kring (2009), Rathbun & Squyres (2002), Barnhart et al. (2010) for Gale/Jezero impact hydrothermal systems.
   * @param {number} [craterDiameterKm=100.0] - Impact crater rim-to-rim diameter in km (10 to 500 km)
   * @param {number} [hostRockPermeabilityMilliDarcies=10.0] - Fractured basement permeability in mD (0.1 to 1000 mD)
   * @param {number} [initialMeltTempK=1473.0] - Initial impact melt pool temperature in K (1000 to 1800 K)
   * @returns {{impactMeltVolumeKm3: number, centralMeltThicknessMeters: number, rayleighNumber: number, isConvectiveHydrothermalActive: boolean, conductiveDiffusiveLifetimeYears: number, activeHydrothermalLifetimeYears: number, astrobiologicalHabitabilityWindow: string}}
   */
  static computeImpactHydrothermalSystemCoolingLifetime(craterDiameterKm = 100.0, hostRockPermeabilityMilliDarcies = 10.0, initialMeltTempK = 1473.0) {
    const Dkm = Math.max(5.0, craterDiameterKm);
    const kMilliDarcies = Math.max(0.01, hostRockPermeabilityMilliDarcies);
    const Tmelt = Math.max(800.0, initialMeltTempK);
    const Tambient = 220.0; // K

    // Impact melt volume V_melt (km^3)
    const vMeltKm3 = 0.0002 * Math.pow(Dkm, 3.83);

    // Central melt sheet / uplift thickness L (meters)
    const Lm = Math.min(15000.0, Math.max(500.0, 0.05 * Dkm * 1000.0));

    // Permeability in m^2 (1 Darcy = 9.869233e-13 m^2 -> 1 mD = 9.869e-16 m^2)
    const kPermM2 = kMilliDarcies * 9.869233e-16;

    // Fluid & rock properties for hydrothermal water
    const rhoFluid = 950.0; // kg/m^3
    const cpFluid = 4200.0; // J/(kg*K)
    const gMars = 3.72076; // m/s^2
    const alphaExp = 1e-3; // 1/K thermal expansivity
    const deltaT = Math.max(50.0, Tmelt - Tambient);
    const muFluid = 2e-4; // Pa*s dynamic viscosity of hydrothermal fluid
    const kmBulk = 2.5; // W/(m*K) thermal conductivity
    const kappaDiff = 1e-6; // m^2/s thermal diffusivity

    // Rayleigh number Ra = ( rho * cp * g * alpha * deltaT * k_perm * L ) / ( mu * kappa_diff )
    const ra = (rhoFluid * cpFluid * gMars * alphaExp * deltaT * kPermM2 * Lm) / (muFluid * kmBulk);

    const isConvective = ra > 40.0; // Critical Rayleigh number for porous convection

    // Nusselt number Nu
    const nu = isConvective ? Math.max(1.0, 0.04 * Math.pow(ra, 0.7) + 1.0) : 1.0;

    // Pure conductive timescale tau_cond = L^2 / ( 4 * kappa ) (seconds & years)
    const SECS_PER_YEAR = 3.15576e7;
    const tauCondSec = Math.pow(Lm, 2.0) / (4.0 * kappaDiff);
    const tauCondYears = tauCondSec / SECS_PER_YEAR;

    // Active convective hydrothermal lifetime
    const tHydroYears = tauCondYears / nu;

    let habitability = 'Long-Lived Post-Impact Hydrothermal Habitable System (> 100,000 Years)';
    if (tHydroYears < 10000.0) {
      habitability = 'Short-Lived Ephemeral Hydrothermal Venting (< 10,000 Years)';
    } else if (tHydroYears > 300000.0) {
      habitability = 'Giant Basin Hydrothermal Province (Mega-Crater Prolonged Geothermal Bioreactor)';
    }

    return {
      impactMeltVolumeKm3: parseFloat(vMeltKm3.toFixed(1)),
      centralMeltThicknessMeters: parseFloat(Lm.toFixed(1)),
      rayleighNumber: parseFloat(ra.toFixed(1)),
      isConvectiveHydrothermalActive: isConvective,
      conductiveDiffusiveLifetimeYears: parseFloat(tauCondYears.toFixed(0)),
      activeHydrothermalLifetimeYears: parseFloat(tHydroYears.toFixed(0)),
      astrobiologicalHabitabilityWindow: habitability
    };
  }

  /**
   * Calculate radioactive secular decay of Martian geothermal heat flux, ancient Noachian cryosphere thinning, and basal aquifer overpressurization.
   * Q_geo(t) = Q_present * ( 1 + 1.6 * ( t / 4.0 )^1.2 )
   * z_base_paleo = ( k_crust * ( T_melt - T_paleo ) ) / Q_geo(t)
   * Reference: Hauck & Phillips (2002), Grott & Breuer (2010), Plesa et al. (2018) for 4-Gyr thermal evolution of Mars.
   * @param {number} [geologicalLookbackAgeGyr=3.8] - Time before present in Gyr (0.0 to 4.5 Gyr; 3.8 Gyr = Noachian-Hesperian boundary)
   * @param {number} [paleoSurfaceTempK=225.0] - Ancient mean surface temperature in K (180 to 260 K)
   * @param {number} [crustalThermalConductivityWMK=2.0] - Bulk crustal conductivity in W/(m*K)
   * @param {number} [presentGeothermalHeatFluxMWm2=25.0] - Present-day geothermal heat flux in mW/m^2
   * @returns {{geologicalEpoch: string, paleoGeothermalFluxMWm2: number, paleoCryosphereThicknessKm: number, presentCryosphereThicknessKm: number, cryosphereThinningFactor: number, hydrologicDischargePotential: string}}
   */
  static computePaleoGeothermalCryosphereThinningAndNoachianMelting(geologicalLookbackAgeGyr = 3.8, paleoSurfaceTempK = 225.0, crustalThermalConductivityWMK = 2.0, presentGeothermalHeatFluxMWm2 = 25.0) {
    const tGyr = Math.max(0.0, Math.min(4.5, geologicalLookbackAgeGyr));
    const Tpaleo = Math.max(150.0, Math.min(270.0, paleoSurfaceTempK));
    const kCrust = Math.max(0.5, crustalThermalConductivityWMK);
    const Q0 = Math.max(10.0, presentGeothermalHeatFluxMWm2);

    const Tmelt = 273.15; // K

    // Geothermal flux scaling over 4.5 Gyr mantle radioactive cooling (mW/m^2)
    const qPaleoMWm2 = Q0 * (1.0 + 1.6 * Math.pow(tGyr / 4.0, 1.2));
    const qPaleoW = qPaleoMWm2 * 1e-3;
    const q0W = Q0 * 1e-3;

    // Cryosphere basal melting depths (km)
    const zPaleoM = (kCrust * (Tmelt - Tpaleo)) / qPaleoW;
    const zPaleoKm = zPaleoM / 1000.0;

    const TpresentSurf = 215.0; // present average Mars surface temperature
    const zPresentM = (kCrust * (Tmelt - TpresentSurf)) / q0W;
    const zPresentKm = zPresentM / 1000.0;

    const thinningRatio = zPaleoKm / zPresentKm;

    let epoch = 'Early Noachian Epoch (~3.8 - 4.1 Ga)';
    if (tGyr < 1.0) {
      epoch = 'Late Amazonian Modern Epoch (< 1.0 Ga)';
    } else if (tGyr < 3.0) {
      epoch = 'Hesperian Post-Volcanic Epoch (1.0 - 3.0 Ga)';
    }

    let discharge = 'Widespread Sub-Ice Aquifer Overpressure & Valley Network Carving';
    if (zPaleoKm > 3.5) {
      discharge = 'Thick Cryogenic Seal (Confined Deep Subsurface Groundwater)';
    } else if (zPaleoKm < 1.0) {
      discharge = 'Ultra-Thin Permafrost Breach & Continuous Hydrothermal Surface Spring Outflow';
    }

    return {
      geologicalEpoch: epoch,
      paleoGeothermalFluxMWm2: parseFloat(qPaleoMWm2.toFixed(1)),
      paleoCryosphereThicknessKm: parseFloat(zPaleoKm.toFixed(2)),
      presentCryosphereThicknessKm: parseFloat(zPresentKm.toFixed(2)),
      cryosphereThinningFactor: parseFloat(thinningRatio.toFixed(3)),
      hydrologicDischargePotential: discharge
    };
  }

  /**
   * Calculate subsurface cryopeg hypersaline brine freezing point depression, liquid permafrost column thickness, and water activity.
   * z_top = ( T_eutectic - T_surf ) / ( dT / dz )
   * z_base = ( T_pure_ice - T_surf ) / ( dT / dz )
   * Reference: Fairén et al. (2009), Chevrier et al. (2020), Orosei et al. (2018) for South Pole basal perchlorate/chloride cryopegs.
   * @param {string} [saltType='mg_perchlorate'] - Salt chemistry ('mg_perchlorate', 'na_perchlorate', 'cacl2', 'nacl', 'mgso4')
   * @param {number} [meanSurfaceTempK=195.0] - Mean annual ground surface temperature in K (150 to 240 K; 195 K = South Polar Planum Australe)
   * @param {number} [geothermalHeatFluxMWm2=25.0] - Regional crustal geothermal heat flux in mW/m^2
   * @param {number} [crustalThermalConductivityWMK=2.0] - Thermal conductivity in W/(m*K)
   * @param {number} [porosityFraction=0.20] - Subsurface regolith porosity fraction (0.05 to 0.40)
   * @returns {{saltComposition: string, eutecticFreezingTempK: number, eutecticFreezingTempC: number, waterActivityAw: number, cryopegTopDepthKm: number, cryopegBaseDepthKm: number, cryopegColumnThicknessKm: number, astrobiologicalHabitabilityAssessment: string}}
   */
  static computeCryopegSubsurfaceFreezingPointDepressionAndBrinePoreVolume(saltType = 'mg_perchlorate', meanSurfaceTempK = 195.0, geothermalHeatFluxMWm2 = 25.0, crustalThermalConductivityWMK = 2.0, porosityFraction = 0.20) {
    const Tsurf = Math.max(120.0, Math.min(270.0, meanSurfaceTempK));
    const QgeoW = Math.max(5.0, geothermalHeatFluxMWm2) * 1e-3;
    const kCrust = Math.max(0.5, crustalThermalConductivityWMK);
    const phi = Math.max(0.02, Math.min(0.50, porosityFraction));

    const sKey = saltType.toLowerCase();
    let Teutc = 206.0; // Mg(ClO4)2 eutectic (K)
    let saltName = 'Magnesium Perchlorate (Mg(ClO4)2)';
    let aw = 0.50; // water activity

    if (sKey.includes('na_per') || sKey.includes('sodium_per')) {
      Teutc = 239.0;
      saltName = 'Sodium Perchlorate (NaClO4)';
      aw = 0.65;
    } else if (sKey.includes('cacl2') || sKey.includes('calcium')) {
      Teutc = 221.0;
      saltName = 'Calcium Chloride (CaCl2)';
      aw = 0.55;
    } else if (sKey.includes('nacl') || sKey.includes('halite')) {
      Teutc = 252.0;
      saltName = 'Sodium Chloride / Halite (NaCl)';
      aw = 0.75;
    } else if (sKey.includes('mgso4') || sKey.includes('sulfate') || sKey.includes('epsom')) {
      Teutc = 269.0;
      saltName = 'Magnesium Sulfate / Epsomite (MgSO4)';
      aw = 0.85;
    }

    const dTDzKPerM = QgeoW / kCrust;
    const dTDzKPerKm = dTDzKPerM * 1000.0;

    // Top of cryopeg liquid horizon
    let zTopM = 0.0;
    if (Tsurf < Teutc) {
      zTopM = (Teutc - Tsurf) / dTDzKPerM;
    }
    const zTopKm = zTopM / 1000.0;

    // Base of permafrost / transition to fresh liquid water at 273.15 K
    const zBaseM = Math.max(zTopM + 10.0, (273.15 - Tsurf) / dTDzKPerM);
    const zBaseKm = zBaseM / 1000.0;

    const columnThicknessKm = zBaseKm - zTopKm;

    let habitability = 'Extreme Hypersaline Subsurface Cryo-Habitability (Active Liquid Permafrost Horizon)';
    if (columnThicknessKm > 4.0 && aw < 0.60) {
      habitability = 'Extreme Hypersaline Subsurface Cryopeg Aquifer (Planum Australe Analogue / Chaotropic Water Activity a_w < 0.60)';
    } else if (aw < 0.60) {
      habitability = 'Extreme Hypersaline Permafrost Brine (Sub-Thermodynamic Water Activity a_w < 0.60)';
    } else if (columnThicknessKm > 4.0) {
      habitability = 'Extensive Deep Cryopeg System (South Polar Planum Australe Radar Analogue)';
    }

    return {
      saltComposition: saltName,
      eutecticFreezingTempK: parseFloat(Teutc.toFixed(1)),
      eutecticFreezingTempC: parseFloat((Teutc - 273.15).toFixed(2)),
      waterActivityAw: parseFloat(aw.toFixed(2)),
      cryopegTopDepthKm: parseFloat(zTopKm.toFixed(2)),
      cryopegBaseDepthKm: parseFloat(zBaseKm.toFixed(2)),
      cryopegColumnThicknessKm: parseFloat(columnThicknessKm.toFixed(2)),
      astrobiologicalHabitabilityAssessment: habitability
    };
  }

  /**
   * Calculate subsurface methane clathrate hydrate (CH4 * 5.75H2O) thermodynamic dissociation, overpressure, and Darcy atmospheric plume flux.
   * ln( P_eq / kPa ) = 38.98 - 4438.0 / T
   * F_CH4 = ( k_perm / mu_gas ) * ( delta_P / z )
   * Reference: Chassefière et al. (2013), Lasue et al. (2015), Webster et al. (2018) for MSL Curiosity Gale Crater methane spikes.
   * @param {number} [subsurfaceDepthMeters=150.0] - Depth of methane clathrate pocket in meters (10 to 2000 m)
   * @param {number} [geothermalThermalPulseDeltaTK=15.0] - Transient subsurface thermal anomaly / pulse in K (0 to 50 K)
   * @param {number} [hostRockPermeabilityMilliDarcies=50.0] - Overlying fractured rock permeability in mD (0.1 to 1000 mD)
   * @param {number} [meanSurfaceTempK=215.0] - Mean ambient ground temperature in K
   * @returns {{lithostaticPorePressureKPa: number, clathrateEquilibriumTempK: number, isClathrateThermallyDestabilized: boolean, gasOverpressureKPa: number, surfaceMethaneFluxNmolM2S: number, atmosphericColumnSpikePpbv: number, atmosphericPlumeSignature: string}}
   */
  static computeSubsurfaceMethaneClathrateDissociationAndPlumeFlux(subsurfaceDepthMeters = 150.0, geothermalThermalPulseDeltaTK = 15.0, hostRockPermeabilityMilliDarcies = 50.0, meanSurfaceTempK = 215.0) {
    const zM = Math.max(5.0, subsurfaceDepthMeters);
    const deltaT = Math.max(0.0, geothermalThermalPulseDeltaTK);
    const kMD = Math.max(0.01, hostRockPermeabilityMilliDarcies);
    const Tsurf = Math.max(150.0, Math.min(270.0, meanSurfaceTempK));

    const gMars = 3.72076; // m/s^2
    const rhoRock = 2500.0; // kg/m^3
    const muGas = 1.2e-5; // Pa*s CH4 gas viscosity at Martian temperatures

    // Lithostatic / hydrostatic confining pressure at depth z (kPa)
    const pLithPa = 610.0 + (rhoRock * gMars * zM);
    const pLithKPa = pLithPa / 1000.0;

    // Background cryosphere temperature at depth z (assuming dT/dz = 12.5 K/km)
    const Tbg = Tsurf + (0.0125 * zM);
    const Tlocal = Tbg + deltaT;

    // Clathrate equilibrium dissociation temp at confining pressure pLithKPa
    const Teq = 4438.0 / (38.98 - Math.log(Math.max(1.0, pLithKPa)));

    // Clathrate equilibrium vapor pressure at Tlocal (kPa)
    const pEqKPa = Math.exp(38.98 - (4438.0 / Math.max(100.0, Tlocal)));

    const isDestabilized = Tlocal >= Teq || pEqKPa > pLithKPa;
    const overpressureKPa = Math.max(0.0, pEqKPa - pLithKPa);
    const overpressurePa = overpressureKPa * 1000.0;

    // Darcy diffusive volumetric gas flux (m/s)
    const kPermM2 = kMD * 9.869233e-16;
    const dPdz = overpressurePa / zM;
    const qDarcyMS = (kPermM2 / muGas) * dPdz;

    // Molar density of CH4 gas at Martian boundary layer (mol/m^3)
    const R_GAS = 8.314;
    const rhoMolar = 610.0 / (R_GAS * 215.0); // ~0.34 mol/m^3
    const fluxMolM2S = qDarcyMS * rhoMolar;
    const fluxNmolM2S = fluxMolM2S * 1e9;

    // Atmospheric column mixing ratio enhancement (ppbv)
    const H_ATM_SCALE_M = 11100.0; // Mars atmospheric scale height (m)
    const columnAirMolM2 = (610.0 / (gMars * 0.044)); // ~3700 mol/m^2
    const deltaChiPpbv = Math.min(500.0, (fluxMolM2S * 86400.0 / columnAirMolM2) * 1e9);

    let plumeDesc = 'Quiescent Background Methane Seepage (< 0.5 ppbv / MSL Background Level)';
    if (deltaChiPpbv > 10.0) {
      plumeDesc = 'High-Concentration Methane Plume Outburst (> 10 ppbv / MSL Curiosity Gale Crater Spike Analogue)';
    } else if (isDestabilized) {
      plumeDesc = 'Active Subsurface Clathrate Thermal Destabilization & Micro-Seepage';
    }

    return {
      lithostaticPorePressureKPa: parseFloat(pLithKPa.toFixed(2)),
      clathrateEquilibriumTempK: parseFloat(Teq.toFixed(1)),
      isClathrateThermallyDestabilized: isDestabilized,
      gasOverpressureKPa: parseFloat(overpressureKPa.toFixed(1)),
      surfaceMethaneFluxNmolM2S: parseFloat(fluxNmolM2S.toFixed(4)),
      atmosphericColumnSpikePpbv: parseFloat(deltaChiPpbv.toFixed(2)),
      atmosphericPlumeSignature: plumeDesc
    };
  }

  /**
   * Calculate subsurface magma sill crystallization timescale, Stefan latent heat kinetics, and thermal metamorphic halo width.
   * tau_solid = b^2 / ( 4 * kappa * lambda_s^2 )
   * w_halo = 2 * sqrt( kappa * tau_solid )
   * Reference: Jaeger (1968), Turcotte & Schubert (2002), Michalski & Niles (2012) for Elysium Planitia / Syrtis Major volcanic intrusions.
   * @param {number} [sillThicknessMeters=500.0] - Total vertical thickness of magma sheet in meters (50 to 5000 m)
   * @param {number} [intrusionDepthMeters=3000.0] - Depth below Martian surface in meters (500 to 20000 m)
   * @param {number} [magmaTempK=1473.15] - Initial basaltic magma temperature in K (1200 C)
   * @param {number} [thermalDiffusivityM2S=1e-6] - Crustal thermal diffusivity in m^2/s
   * @returns {{sillHalfThicknessMeters: number, solidificationTimeYears: number, thermalHaloWidthMeters: number, peakWallrockContactTempK: number, peakWallrockContactTempC: number, hydrothermalContactZoneMetamorphism: string}}
   */
  static computeSubsurfaceMagmaSillCoolingAndThermalHalo(sillThicknessMeters = 500.0, intrusionDepthMeters = 3000.0, magmaTempK = 1473.15, thermalDiffusivityM2S = 1e-6) {
    const H = Math.max(10.0, sillThicknessMeters);
    const zM = Math.max(50.0, intrusionDepthMeters);
    const Tmagma = Math.max(1000.0, magmaTempK);
    const kappa = Math.max(1e-7, thermalDiffusivityM2S);

    const b = H / 2.0; // half thickness (m)
    const Tsolidus = 1273.15; // 1000 C (K)

    // Ambient host rock temperature at depth z (dT/dz = 12.5 K/km)
    const Tambient = 215.0 + (0.0125 * zM);

    const cp = 1000.0; // J/(kg*K)
    const Lf = 4.0e5; // J/kg latent heat of fusion

    // Stefan dimensionless latent heat parameter lambda_s
    const lambdaS = Math.max(0.2, Math.sqrt((cp * Math.max(10.0, Tsolidus - Tambient)) / (2.0 * Lf * Math.sqrt(Math.PI))));

    // Solidification timescale tau_solid (seconds & years)
    const SECS_PER_YEAR = 3.15576e7;
    const tauSolidSec = Math.pow(b, 2.0) / (4.0 * kappa * Math.pow(lambdaS, 2.0));
    const tauSolidYears = tauSolidSec / SECS_PER_YEAR;

    // Metamorphic aureole / thermal halo width
    const wHaloM = 2.0 * Math.sqrt(kappa * tauSolidSec);

    // Peak contact wallrock temperature
    const TcontactK = Tambient + ((Tmagma - Tambient) / 2.0);
    const TcontactC = TcontactK - 273.15;

    let metaDesc = 'High-Temperature Hydrothermal Skarn & Pyroxene/Epidote Facies Halo';
    if (TcontactC < 300.0) {
      metaDesc = 'Low-Grade Zeolite/Prehnite Hydrothermal Aureole';
    } else if (TcontactC > 700.0) {
      metaDesc = 'High-Grade Pyroxene Hornfels / Magmatic Partial Melting Contact Zone';
    }

    return {
      sillHalfThicknessMeters: parseFloat(b.toFixed(1)),
      solidificationTimeYears: parseFloat(tauSolidYears.toFixed(0)),
      thermalHaloWidthMeters: parseFloat(wHaloM.toFixed(1)),
      peakWallrockContactTempK: parseFloat(TcontactK.toFixed(1)),
      peakWallrockContactTempC: parseFloat(TcontactC.toFixed(1)),
      hydrothermalContactZoneMetamorphism: metaDesc
    };
  }

  /**
   * Calculate diurnal perchlorate/chloride salt deliquescence relative humidity threshold, efflorescence hysteresis, and liquid brine stability window.
   * DRH(T) = DRH_eut * exp( -0.005 * ( T - T_eut ) )
   * Reference: Gough et al. (2011), Nuding et al. (2014), Rivera-Valentín et al. (2020) for Phoenix / MSL Gale perchlorate deliquescence.
   * @param {number} [relativeHumidityPercent=65.0] - Ambient surface boundary layer relative humidity in % (0 to 100%)
   * @param {number} [regolithTempK=225.0] - Regolith temperature in K (150 to 300 K)
   * @param {string} [saltPhase='ca_perchlorate'] - Salt chemistry ('ca_perchlorate', 'mg_perchlorate', 'nacl')
   * @param {number} [saltWeightPercent=1.0] - Salt abundance in soil in wt% (0.1 to 10%)
   * @returns {{saltType: string, eutecticTempK: number, deliquescenceHumidityThresholdPct: number, isDeliquescenceActive: boolean, adsorbedWaterMassGramsPerKgSoil: number, dailyLiquidBrineWindowHours: number, deliquescenceThermodynamicState: string}}
   */
  static computePerchlorateSaltDeliquescenceDiurnalKinetics(relativeHumidityPercent = 65.0, regolithTempK = 225.0, saltPhase = 'ca_perchlorate', saltWeightPercent = 1.0) {
    const rh = Math.max(0.0, Math.min(100.0, relativeHumidityPercent));
    const T = Math.max(120.0, Math.min(320.0, regolithTempK));
    const wSalt = Math.max(0.01, Math.min(50.0, saltWeightPercent)) / 100.0;

    let Teutc = 221.0; // Ca(ClO4)2 eutectic
    let drhEutc = 50.0; // % at eutectic
    let erhEutc = 20.0;
    let name = 'Calcium Perchlorate (Ca(ClO4)2)';

    const sKey = saltPhase.toLowerCase();
    if (sKey.includes('mg') || sKey.includes('magnesium')) {
      Teutc = 206.0;
      drhEutc = 55.0;
      erhEutc = 22.0;
      name = 'Magnesium Perchlorate (Mg(ClO4)2)';
    } else if (sKey.includes('nacl') || sKey.includes('halite')) {
      Teutc = 252.0;
      drhEutc = 75.0;
      erhEutc = 45.0;
      name = 'Sodium Chloride (NaCl)';
    }

    // Temperature-dependent DRH(T)
    const drhT = Math.max(15.0, Math.min(95.0, drhEutc * Math.exp(-0.005 * (T - Teutc))));

    // Deliquescence occurs when T >= Teutc AND RH >= DRH(T)
    const isAboveEutectic = T >= Teutc;
    const isDeliquescent = isAboveEutectic && rh >= drhT;

    // Adsorbed / absorbed liquid water content (g H2O / kg soil)
    let waterAdsorbedGramsPerKg = 0.0;
    if (isDeliquescent) {
      waterAdsorbedGramsPerKg = (wSalt * 1000.0) * (1.2 + (rh / drhT));
    } else if (rh > erhEutc) {
      waterAdsorbedGramsPerKg = (wSalt * 1000.0) * 0.25 * (rh / drhT);
    }

    // Estimated daily window in hours where conditions are met (empirical diurnal model)
    let windowHours = 0.0;
    if (isDeliquescent) {
      windowHours = Math.min(8.0, 2.0 + (rh - drhT) * 0.15);
    }

    let state = 'Crystalline Anhydrous / Effloresced Dry Salt';
    if (isDeliquescent) {
      state = 'Active Liquid Aqueous Brine (Deliquesced Solution Layer)';
    } else if (rh >= erhEutc) {
      state = 'Metastable Hydrated Solid Solution (Hysteresis Efflorescence Regime)';
    }

    return {
      saltType: name,
      eutecticTempK: parseFloat(Teutc.toFixed(1)),
      deliquescenceHumidityThresholdPct: parseFloat(drhT.toFixed(1)),
      isDeliquescenceActive: isDeliquescent,
      adsorbedWaterMassGramsPerKgSoil: parseFloat(waterAdsorbedGramsPerKg.toFixed(2)),
      dailyLiquidBrineWindowHours: parseFloat(windowHours.toFixed(1)),
      deliquescenceThermodynamicState: state
    };
  }

  /**
   * Calculate ancient Martian paleolake/ocean wind-generated surface wave height, wavelength, wave power flux, and coastal cliff notch retreat rate.
   * Hs = 0.283 * ( U^2 / g ) * tanh( 0.0125 * ( g*F / U^2 )^0.42 )
   * P_wave = (1/16) * rho * g * Hs^2 * c_g
   * Reference: Clifford & Parker (2001), Lorenz et al. (2005), Banfield et al. (2015), DiBiase et al. (2013) for Jezero / Gale / Vastitas Borealis coastal geomorphology.
   * @param {number} [fetchDistanceKm=50.0] - Wind fetch distance across lake in km (1.0 to 1500.0 km)
   * @param {number} [windSpeedMS=15.0] - 10-meter surface wind speed in m/s (2.0 to 40.0 m/s)
   * @param {number} [paleolakeDepthMeters=100.0] - Mean water depth in meters (5.0 to 2000.0 m)
   * @param {number} [atmosphericPressureBars=0.50] - Paleoclimate atmospheric pressure in bars (0.01 to 2.0 bars)
   * @returns {{significantWaveHeightMeters: number, peakWavePeriodSec: number, wavelengthMeters: number, wavePowerFluxKWMeter: number, coastalCliffRetreatRateMPerKyr: number, lacustrineWaveRegime: string}}
   */
  static computeAncientMartianPaleolakeWaveEnergyAndCoastalErosion(fetchDistanceKm = 50.0, windSpeedMS = 15.0, paleolakeDepthMeters = 100.0, atmosphericPressureBars = 0.50) {
    const Fkm = Math.max(0.5, fetchDistanceKm);
    const FM = Fkm * 1000.0;
    const U = Math.max(1.0, windSpeedMS);
    const dM = Math.max(2.0, paleolakeDepthMeters);
    const pAtm = Math.max(0.006, atmosphericPressureBars);

    const gMars = 3.72076;
    const rhoWater = 1000.0; // kg/m^3

    // Dimensionless fetch
    const fetchDim = (gMars * FM) / Math.pow(U, 2.0);

    // Significant wave height (CEM / SPM fetch-limited model adapted for Mars gravity)
    const HsM = 0.283 * (Math.pow(U, 2.0) / gMars) * Math.tanh(0.0125 * Math.pow(fetchDim, 0.42));

    // Peak wave period (SPM / CEM fetch-limited tanh formulation)
    const TpSec = 7.54 * (U / gMars) * Math.tanh(0.040 * Math.pow(fetchDim, 0.25));

    // Deep/intermediate wavelength
    const lambdaM = (gMars * Math.pow(TpSec, 2.0)) / (2.0 * Math.PI);

    // Group velocity (deep water c_g = c / 2)
    const cgMS = (gMars * TpSec) / (4.0 * Math.PI);

    // Wave power flux density (kW / m shoreline)
    const PwaveWM = (1.0 / 16.0) * rhoWater * gMars * Math.pow(HsM, 2.0) * cgMS;
    const PwaveKWM = PwaveWM / 1000.0;

    // Coastal notch erosion retreat rate (m / 1000 years for deltaic/lacustrine sediment)
    // k_erod ~ 2e-4 (m/kyr per kW/m)
    const kErod = 2.0e-4 * Math.min(2.0, pAtm / 0.5);
    const RretreatMPerKyr = PwaveKWM * kErod * 1000.0;

    let regime = 'Moderate Lacustrine Wave Action & Deltaic Shoreline Notch Formation';
    if (HsM > 4.0) {
      regime = 'Severe Storm Wave Regime / High-Energy Ocean Coastline Erosion';
    } else if (HsM < 0.5) {
      regime = 'Quiescent Low-Energy Play Lake / Minimal Shoreline Reworking';
    }

    return {
      significantWaveHeightMeters: parseFloat(HsM.toFixed(2)),
      peakWavePeriodSec: parseFloat(TpSec.toFixed(1)),
      wavelengthMeters: parseFloat(lambdaM.toFixed(1)),
      wavePowerFluxKWMeter: parseFloat(PwaveKWM.toFixed(2)),
      coastalCliffRetreatRateMPerKyr: parseFloat(RretreatMPerKyr.toFixed(2)),
      lacustrineWaveRegime: regime
    };
  }

  /**
   * Calculate ancient Martian glacial flow velocity, Glen's law internal deformation creep, basal shear stress, and ice discharge flux.
   * tau_b = rho * g * H * sin( alpha )
   * u_def = ( 2 * A(T) / (n + 1) ) * tau_b^n * H
   * Reference: Nye (1952), Paterson (1994), Fastook et al. (2008), Head et al. (2010) for LDA / CCF glacial dynamics.
   * @param {number} [iceThicknessMeters=400.0] - Glacial ice sheet thickness in meters (50 to 3000 m)
   * @param {number} [surfaceSlopeDeg=3.0] - Surface topographic slope in degrees (0.1 to 30 deg)
   * @param {number} [meanIceTempK=230.0] - Mean ice column temperature in K (170 to 273 K)
   * @param {boolean} [isBasalSlidingActive=false] - Whether basal wet melting lubricant is present
   * @returns {{basalShearStressKPa: number, glenFlowRateFactorS1Pa3: number, internalDeformationSpeedMmYr: number, basalSlidingSpeedMmYr: number, surfaceFlowSpeedMmYr: number, annualIceFluxM2Yr: number, glacialDynamicRegime: string}}
   */
  static computeAncientMartianGlacialFlowVelocityAndBasalShearStress(iceThicknessMeters = 400.0, surfaceSlopeDeg = 3.0, meanIceTempK = 230.0, isBasalSlidingActive = false) {
    const H = Math.max(10.0, iceThicknessMeters);
    const slopeDeg = Math.max(0.05, Math.min(45.0, surfaceSlopeDeg));
    const alphaRad = slopeDeg * (Math.PI / 180.0);
    const T = Math.max(150.0, Math.min(273.15, meanIceTempK));

    const rhoIce = 920.0; // kg/m^3
    const gMars = 3.72076; // m/s^2

    // Basal driving shear stress (Pa & kPa)
    const tauBPa = rhoIce * gMars * H * Math.sin(alphaRad);
    const tauBKPa = tauBPa / 1000.0;

    // Glen's flow law parameter A(T) = A0 * exp( -Q / ( R * T ) )
    const A0 = 3.985e-13; // s^-1 Pa^-3
    const Q = 6.0e4; // J/mol activation energy
    const R_GAS = 8.314;
    const AT = A0 * Math.exp(-Q / (R_GAS * T));

    // Internal deformation velocity u_def (m/s and mm/year)
    const n = 3.0;
    const uDefMS = (2.0 * AT / (n + 1.0)) * Math.pow(tauBPa, n) * H;
    const SECS_PER_YEAR = 3.15576e7;
    const uDefMmYr = uDefMS * SECS_PER_YEAR * 1000.0;

    // Basal sliding velocity
    let uSlideMmYr = 0.0;
    if (isBasalSlidingActive) {
      uSlideMmYr = Math.max(10.0, uDefMmYr * 2.5);
    }

    const uTotalMmYr = uDefMmYr + uSlideMmYr;
    const uMeanMYr = ((4.0 / 5.0) * (uDefMmYr / 1000.0)) + (uSlideMmYr / 1000.0);
    const QiceM2Yr = uMeanMYr * H;

    let regime = 'Cold-Based Polythermal Glacial Creep (Slow Internal Deformation)';
    if (isBasalSlidingActive) {
      regime = 'Warm-Based Fast Glacial Surge with Subglacial Meltwater Sliding';
    } else if (uTotalMmYr > 50.0) {
      regime = 'Active High-Obliquity Mountain Valley Glacial Flow';
    }

    return {
      basalShearStressKPa: parseFloat(tauBKPa.toFixed(2)),
      glenFlowRateFactorS1Pa3: parseFloat(AT.toExponential(4)),
      internalDeformationSpeedMmYr: parseFloat(uDefMmYr.toFixed(2)),
      basalSlidingSpeedMmYr: parseFloat(uSlideMmYr.toFixed(2)),
      surfaceFlowSpeedMmYr: parseFloat(uTotalMmYr.toFixed(2)),
      annualIceFluxM2Yr: parseFloat(QiceM2Yr.toFixed(2)),
      glacialDynamicRegime: regime
    };
  }

  /**
   * Calculate subsurface salt cryohydrate phase stability (hydrohalite, meridianiite, perchlorates), eutectic melting depth, and brine viscosity.
   * z_brine = ( T_eut - T_surf ) / ( Q_geo / k_crust )
   * Reference: Kargel (1991), Peterson & Wang (2006), Toner et al. (2014) for Martian permafrost cryohydrate phase equilibria.
   * @param {string} [saltSpecies='hydrohalite'] - Cryohydrate mineral phase ('meridianiite', 'hydrohalite', 'sodium_perchlorate', 'calcium_perchlorate')
   * @param {number} [meanSurfaceTempK=215.0] - Mean annual ground surface temperature in K (150 to 250 K)
   * @param {number} [geothermalFluxMWM2=25.0] - Planetary geothermal heat flux in mW/m^2 (10 to 60 mW/m^2)
   * @param {number} [crustalConductivityWMK=2.0] - Regolith/crust thermal conductivity in W/(m*K) (1.0 to 4.0 W/m*K)
   * @returns {{cryohydrateMineralogy: string, chemicalFormula: string, eutecticMeltingTempK: number, eutecticMeltingTempC: number, depthToLiquidBrineKm: number, relativeBrineViscosityVsWater: number, astrobiologicalPoreStability: string}}
   */
  static computeSubsurfaceCryohydrateSaltFreezingDepressionAndBrineMobility(saltSpecies = 'hydrohalite', meanSurfaceTempK = 215.0, geothermalFluxMWM2 = 25.0, crustalConductivityWMK = 2.0) {
    const Tsurf = Math.max(120.0, Math.min(270.0, meanSurfaceTempK));
    const Qgeo = Math.max(5.0, geothermalFluxMWM2) / 1000.0; // W/m^2
    const kCrust = Math.max(0.5, crustalConductivityWMK);

    let Teutc = 252.0; // Hydrohalite NaCl*2H2O
    let name = 'Hydrohalite (Sodium Chloride Dihydrate)';
    let formula = 'NaCl * 2H2O';
    let viscRatio = 3.2;

    const sKey = saltSpecies.toLowerCase();
    if (sKey.includes('meridiani') || sKey.includes('mgso4')) {
      Teutc = 269.2;
      name = 'Meridianiite (Magnesium Sulfate Undecahydrate)';
      formula = 'MgSO4 * 11H2O';
      viscRatio = 4.8;
    } else if (sKey.includes('na_perchlorate') || sKey.includes('nacio4') || sKey.includes('sodium_perchlorate')) {
      Teutc = 236.0;
      name = 'Sodium Perchlorate Dihydrate';
      formula = 'NaClO4 * 2H2O';
      viscRatio = 2.6;
    } else if (sKey.includes('ca_perchlorate') || sKey.includes('calcium_perchlorate')) {
      Teutc = 221.0;
      name = 'Calcium Perchlorate Tetrahydrate';
      formula = 'Ca(ClO4)2 * 4H2O';
      viscRatio = 5.6;
    }

    const dTdZ = Qgeo / kCrust; // K/m
    const zBrineM = Math.max(0.0, (Teutc - Tsurf) / dTdZ);
    const zBrineKm = zBrineM / 1000.0;

    let habDesc = 'Deep Hypersaline Subglacial Liquefaction Horizon (Subsurface Astrobiological Refuge)';
    if (zBrineKm < 1.0) {
      habDesc = 'Shallow Cryopeg Horizon Accessible to In-Situ Subsurface Drilling';
    } else if (zBrineKm > 5.0) {
      habDesc = 'Deep Basement Aquifer / Thick Basal Permafrost Seal';
    }

    return {
      cryohydrateMineralogy: name,
      chemicalFormula: formula,
      eutecticMeltingTempK: parseFloat(Teutc.toFixed(1)),
      eutecticMeltingTempC: parseFloat((Teutc - 273.15).toFixed(2)),
      depthToLiquidBrineKm: parseFloat(zBrineKm.toFixed(2)),
      relativeBrineViscosityVsWater: parseFloat(viscRatio.toFixed(1)),
      astrobiologicalPoreStability: habDesc
    };
  }

  /**
   * Calculate ancient Martian ocean/paleolake shoreline lithospheric flexural warping, GIA rebound, and non-equipotential elevation distortion.
   * D = E * Te^3 / ( 12 * ( 1 - nu^2 ) )
   * alpha = ( 4 * D / ( ( rho_m - rho_w ) * g ) )^(1/4)
   * Reference: Perron et al. (2007), Citron et al. (2018), Head et al. (2019) for Vastitas Borealis / Arabia paleoshoreline flexural deformation.
   * @param {number} [elasticThicknessKm=40.0] - Lithospheric effective elastic thickness Te in km (15 to 100 km)
   * @param {number} [oceanWaterDepthMeters=1500.0] - Mean ocean depth in meters (200 to 4000 m)
   * @param {number} [oceanBasinRadiusKm=2500.0] - Ocean basin radius in km (500 to 4000 km)
   * @returns {{elasticThicknessKm: number, flexuralRigidityNm: number, flexuralParameterAlphaKm: number, centralIsostaticDeflectionMeters: number, shorelineElevationWarpingMeters: number, paleoshorelineDeformationContext: string}}
   */
  static computeAncientMartianPaleoshorelineFlexureAndGIADeformation(elasticThicknessKm = 40.0, oceanWaterDepthMeters = 1500.0, oceanBasinRadiusKm = 2500.0) {
    const TeKm = Math.max(5.0, Math.min(150.0, elasticThicknessKm));
    const TeM = TeKm * 1000.0;
    const hwM = Math.max(50.0, oceanWaterDepthMeters);
    const RKm = Math.max(100.0, oceanBasinRadiusKm);
    const RM = RKm * 1000.0;

    const E = 1.0e11; // Pa Young's modulus
    const nu = 0.25; // Poisson's ratio
    const rhoMantle = 3500.0; // kg/m^3
    const rhoWater = 1000.0; // kg/m^3
    const gMars = 3.72076; // m/s^2

    // Flexural rigidity D = ( E * Te^3 ) / ( 12 * ( 1 - nu^2 ) )
    const D = (E * Math.pow(TeM, 3.0)) / (12.0 * (1.0 - Math.pow(nu, 2.0)));

    // Flexural parameter alpha = ( 4*D / ( (rho_m - rho_w)*g ) )^(1/4)
    const deltaRho = rhoMantle - rhoWater;
    const alphaM = Math.pow((4.0 * D) / (deltaRho * gMars), 0.25);
    const alphaKm = alphaM / 1000.0;

    // Central isostatic deflection
    const wcM = (rhoWater * hwM) / deltaRho;

    // Shoreline flexural rebound deflection & elevation warping across coastal hinge zone
    const rOverAlpha = RM / alphaM;
    const flexWave = Math.exp(-Math.min(20.0, rOverAlpha)) * Math.cos(rOverAlpha);
    const wShoreM = wcM * (1.0 - flexWave);
    // Peak-to-trough coastal flexural warping across ocean-land dichotomy margin (Turcotte & Schubert 2002; Perron et al. 2007)
    const deltaZWarpM = wcM * (1.0 + Math.exp(-Math.PI / 2.0));

    let context = 'Major Shoreline Elevation Warping (Explains Non-Equipotential Arabia / Deuteronilus Contacts)';
    if (TeKm > 60.0) {
      context = 'Thick Rigid Lithosphere with Broad Regional Flexural Wavelength';
    } else if (TeKm < 25.0) {
      context = 'Thin Noachian Lithosphere with Steep Localized Shoreline Tilting';
    }

    return {
      elasticThicknessKm: parseFloat(TeKm.toFixed(1)),
      flexuralRigidityNm: parseFloat(D.toExponential(4)),
      flexuralParameterAlphaKm: parseFloat(alphaKm.toFixed(1)),
      centralIsostaticDeflectionMeters: parseFloat(wcM.toFixed(1)),
      shorelineElevationWarpingMeters: parseFloat(deltaZWarpM.toFixed(1)),
      paleoshorelineDeformationContext: context
    };
  }

  /**
   * Calculate subsurface hydrothermal Rayleigh-Darcy convection, upwelling plume Darcy flux, Nusselt number, and thermal lifetime.
   * Ra = ( rho_f^2 * c_f * g * alpha_T * k_p * Delta_T * H ) / ( mu_f * k_m )
   * q_z = ( k_p * rho_f * g * alpha_T * Delta_T ) / mu_f
   * Reference: Elder (1967), Turcotte & Schubert (2002), Abramov & Kring (2005) for Jezero / Gusev impact hydrothermal circulation.
   * @param {number} [intrusionDepthKm=3.0] - Depth of magmatic/impact heat source in km (0.5 to 10.0 km)
   * @param {number} [temperatureDifferentialC=400.0] - Temperature excess above ambient in deg C (50 to 900 C)
   * @param {number} [permeabilityM2=1.0e-13] - Crustal fractured rock permeability in m^2 (1e-16 to 1e-11 m^2)
   * @returns {{rayleighDarcyNumber: number, isConvectionActive: boolean, nusseltNumber: number, upwellingDarcySpeedMmDay: number, annualUpwellingFluidFluxMYr: number, hydrothermalLifespanYears: number, hydrothermalAstrobiologyContext: string}}
   */
  static computeSubsurfaceHydrothermalConvectionAndBoilingPlume(intrusionDepthKm = 3.0, temperatureDifferentialC = 400.0, permeabilityM2 = 1.0e-13) {
    const HKm = Math.max(0.2, Math.min(20.0, intrusionDepthKm));
    const HM = HKm * 1000.0;
    const deltaT = Math.max(10.0, Math.min(1200.0, temperatureDifferentialC));
    const kp = Math.max(1.0e-18, Math.min(1.0e-9, permeabilityM2));

    const gMars = 3.72076; // m/s^2
    const rhoFluid = 900.0; // kg/m^3
    const cFluid = 4200.0; // J/(kg*K)
    const alphaT = 1.0e-3; // 1/K
    const muFluid = 1.5e-4; // Pa*s
    const kMatrix = 2.5; // W/(m*K)
    const rhoRock = 2800.0; // kg/m^3
    const cRock = 900.0; // J/(kg*K)

    // Rayleigh-Darcy Number
    const numerator = Math.pow(rhoFluid, 2.0) * cFluid * gMars * alphaT * kp * deltaT * HM;
    const denominator = muFluid * kMatrix;
    const Ra = numerator / denominator;
    const RaCrit = 4.0 * Math.pow(Math.PI, 2.0); // ~39.48

    const isConv = Ra >= RaCrit;

    // Upwelling Darcy flux q_z (m/s, mm/day, m/year)
    const qzMS = (kp * rhoFluid * gMars * alphaT * deltaT) / muFluid;
    const qzMmDay = qzMS * 86400.0 * 1000.0;
    const qzMYr = qzMS * 3.15576e7;

    // Nusselt number (effective convective vs conductive heat transfer ratio)
    let Nu = 1.0;
    if (isConv) {
      Nu = 1.0 + (0.05 * Math.pow(Ra, 0.8));
    }

    // Hydrothermal circulation lifespan (years)
    const tauSec = (Math.pow(HM, 2.0) * rhoRock * cRock) / (4.0 * kMatrix * Nu);
    const tauYears = tauSec / 3.15576e7;

    let habitability = 'Vigorous Hydrothermal Upwelling Plume (Sustains Astrobiological Hot Spring Sinters & Hydrothermal Veins)';
    if (!isConv) {
      habitability = 'Sub-Critical Conduction-Dominated Thermal Halo (Minimal Fluid Circulation)';
    } else if (tauYears > 100000.0) {
      habitability = 'Long-Lived Post-Impact Hydrothermal System (> 100 kyr Habitability Window)';
    }

    return {
      rayleighDarcyNumber: parseFloat(Ra.toFixed(1)),
      isConvectionActive: isConv,
      nusseltNumber: parseFloat(Nu.toFixed(2)),
      upwellingDarcySpeedMmDay: parseFloat(qzMmDay.toFixed(2)),
      annualUpwellingFluidFluxMYr: parseFloat(qzMYr.toFixed(1)),
      hydrothermalLifespanYears: parseFloat(tauYears.toFixed(0)),
      hydrothermalAstrobiologyContext: habitability
    };
  }

  /**
   * Calculate CO2 atmospheric frost condensation temperature, regolith adsorption capacity, and runaway climatic atmospheric collapse threshold.
   * T_frost = 3148.0 / ln( 1.325e11 / P )
   * M_ads = A_ads * ( P / P0 )^gamma * exp( Q_ads / ( R * T ) )
   * Reference: Fanale et al. (1982), Forget & Pierrehumbert (1997), Haberle et al. (2001), Manning et al. (2019) for Mars atmospheric collapse.
   * @param {number} [atmosphericPressurePa=610.0] - Ambient Martian surface atmospheric pressure in Pascals (10 to 100,000 Pa)
   * @param {number} [polarWinterTempK=145.0] - Winter polar surface ground temperature in K (120 to 220 K)
   * @param {number} [regolithSpecificAreaM2G=50.0] - Basaltic regolith specific surface area in m^2/g (5 to 200 m^2/g)
   * @returns {{atmosphericPressurePa: number, co2FrostCondensationTempK: number, co2FrostCondensationTempC: number, regolithCO2AdsorptionKgM3: number, isAtmosphericCollapseTriggered: boolean, climaticCollapseRegime: string}}
   */
  static computeCryovolcanicCO2FrostDesorptionAndAtmosphericCollapse(atmosphericPressurePa = 610.0, polarWinterTempK = 145.0, regolithSpecificAreaM2G = 50.0) {
    const P = Math.max(5.0, Math.min(200000.0, atmosphericPressurePa));
    const Tpole = Math.max(100.0, Math.min(250.0, polarWinterTempK));
    const Sarea = Math.max(1.0, regolithSpecificAreaM2G);

    // Clausius-Clapeyron CO2 frost point temperature (K)
    const TfrostK = 3148.0 / Math.log(1.325e11 / P);
    const TfrostC = TfrostK - 273.15;

    // Regolith CO2 adsorption isotherm
    const R_GAS = 8.314;
    const Qads = 28000.0; // J/mol heat of adsorption
    const P0 = 1000.0; // Pa reference pressure
    const gamma = 0.35;
    const Aads = 1.2e-4 * (Sarea / 50.0);
    const MadsKgM3 = Aads * Math.pow(P / P0, gamma) * Math.exp(Qads / (R_GAS * Tpole));

    // Runaway atmospheric collapse condition
    const isCollapse = Tpole < TfrostK;

    let regime = 'Stable Gaseous Atmosphere (Polar Solar Heating Prevents Permanent Ice Cap Sequestration)';
    if (isCollapse) {
      regime = 'Runaway Climatic Atmospheric Collapse (CO2 Frost Deposition Exceeds Sublimation -> Thin 600 Pa Equil)';
    } else if (MadsKgM3 > 50.0) {
      regime = 'Heavy Regolith CO2 Adsorption Buffering (Subsurface Reservoir Traps Massive Paleopressure)';
    }

    return {
      atmosphericPressurePa: parseFloat(P.toFixed(1)),
      co2FrostCondensationTempK: parseFloat(TfrostK.toFixed(2)),
      co2FrostCondensationTempC: parseFloat(TfrostC.toFixed(2)),
      regolithCO2AdsorptionKgM3: parseFloat(MadsKgM3.toFixed(2)),
      isAtmosphericCollapseTriggered: isCollapse,
      climaticCollapseRegime: regime
    };
  }

  /**
   * Calculate subsurface megaregolith compaction porosity decay, depth-dependent thermal diffusivity, and annual thermal wave phase lag.
   * phi(z) = phi_0 * exp( - z / H_pore )
   * delta_skin = sqrt( kappa * P / pi )
   * phase_lag = z / delta_skin
   * Reference: Clifford (1993), Hanna & Phillips (2005), Kieffer (2013) for Martian megaregolith cryosphere thermal structure.
   * @param {number} [subsurfaceDepthMeters=5.0] - Target subsurface depth in meters (0.1 to 5000 m)
   * @param {number} [surfacePorosityPct=40.0] - Unconsolidated ground surface porosity percentage (10 to 60%)
   * @param {number} [eFoldingDepthKm=3.5] - Porosity compaction e-folding depth in km (1.5 to 8.0 km)
   * @param {number} [poreIceSaturationPct=80.0] - Pore space ice volume filling percentage (0 to 100%)
   * @returns {{subsurfaceDepthMeters: number, megaregolithPorosityPct: number, bulkCrustalDensityKgM3: number, thermalConductivityWMK: number, annualThermalSkinDepthMeters: number, annualPhaseLagSols: number, megaregolithThermalContext: string}}
   */
  static computeSubsurfaceMegaregolithPorosityDecayAndThermalPhaseLag(subsurfaceDepthMeters = 5.0, surfacePorosityPct = 40.0, eFoldingDepthKm = 3.5, poreIceSaturationPct = 80.0) {
    const zM = Math.max(0.05, subsurfaceDepthMeters);
    const phi0 = Math.max(0.05, Math.min(0.70, surfacePorosityPct / 100.0));
    const HporeM = Math.max(500.0, eFoldingDepthKm * 1000.0);
    const Sice = Math.max(0.0, Math.min(1.0, poreIceSaturationPct / 100.0));

    // Porosity decay with lithostatic overburden compaction
    const phiZ = phi0 * Math.exp(-zM / HporeM);
    const phiZPct = phiZ * 100.0;

    // Density and Thermal properties
    const rhoGrain = 2900.0; // kg/m^3
    const rhoIce = 920.0; // kg/m^3
    const rhoBulk = (rhoGrain * (1.0 - phiZ)) + (rhoIce * phiZ * Sice);

    const kSolid = 2.5; // W/(m*K)
    const kIce = 2.2; // W/(m*K)
    const kEff = 0.05 + (kSolid * Math.pow(1.0 - phiZ, 1.5)) + (kIce * phiZ * Sice);

    const cp = 850.0; // J/(kg*K)
    const kappa = kEff / (rhoBulk * cp); // m^2/s

    // Annual thermal wave skin depth and phase lag (P = 668.6 sols = 5.9355e7 s)
    const pAnnualSec = 5.9355e7;
    const deltaSkinM = Math.sqrt((kappa * pAnnualSec) / Math.PI);

    const phaseLagRad = zM / deltaSkinM;
    const phaseLagSols = (phaseLagRad / (2.0 * Math.PI)) * 668.6;

    let regime = 'Active Cryospheric Annual Thermal Layer / Seasonal Skin Depth Horizon';
    if (zM > 20.0) {
      regime = 'Deep Thermally Damped Megaregolith Basement / Geothermal Dominance';
    } else if (zM < 1.0) {
      regime = 'Diurnal Active Boundary Skin Layer / Rapid Diurnal Temperature Fluctuations';
    }

    return {
      subsurfaceDepthMeters: parseFloat(zM.toFixed(2)),
      megaregolithPorosityPct: parseFloat(phiZPct.toFixed(2)),
      bulkCrustalDensityKgM3: parseFloat(rhoBulk.toFixed(1)),
      thermalConductivityWMK: parseFloat(kEff.toFixed(3)),
      annualThermalSkinDepthMeters: parseFloat(deltaSkinM.toFixed(2)),
      annualPhaseLagSols: parseFloat(phaseLagSols.toFixed(1)),
      megaregolithThermalContext: regime
    };
  }

  /**
   * Calculate volcanic lava tube basalt roof thermal insulation, cavity microclimate stability, and radiation shielding.
   * Delta_T_cavity = Delta_T_surf * exp( - h_roof / delta_skin )
   * T_cavity = T_annual + ( Q_geo / k_roof ) * h_roof
   * Reference: Cushing et al. (2007), Titus et al. (2021), Williams et al. (2024) for Arsia Mons & Elysium lava tube thermal shelters.
   * @param {number} [roofThicknessMeters=15.0] - Basalt cave roof thickness in meters (1.0 to 100.0 m)
   * @param {number} [surfaceDiurnalAmplitudeK=100.0] - Surface diurnal temperature swing in K (40 to 140 K)
   * @param {number} [meanAnnualSurfaceTempK=218.0] - Mean annual ground surface temperature in K (150 to 250 K)
   * @returns {{roofThicknessMeters: number, cavityMeanTempK: number, cavityMeanTempC: number, diurnalCavityFluctuationK: number, annualCavityFluctuationK: number, radiationShieldingPercent: number, cavernHabitatShelterContext: string}}
   */
  static computeVolcanicLavaTubeThermalInsulationAndShelter(roofThicknessMeters = 15.0, surfaceDiurnalAmplitudeK = 100.0, meanAnnualSurfaceTempK = 218.0) {
    const hRoofM = Math.max(0.5, Math.min(200.0, roofThicknessMeters));
    const dtSurfDiurnal = Math.max(10.0, surfaceDiurnalAmplitudeK);
    const Tann = Math.max(120.0, Math.min(270.0, meanAnnualSurfaceTempK));

    const kRoof = 1.8; // W/(m*K) solid basalt
    const rhoRoof = 2800.0; // kg/m^3
    const cp = 850.0; // J/(kg*K)
    const kappa = kRoof / (rhoRoof * cp); // m^2/s

    // Diurnal skin depth (88775.2 s sol) and Annual skin depth (5.9355e7 s)
    const deltaDiurnalM = Math.sqrt((kappa * 88775.2) / Math.PI);
    const deltaAnnualM = Math.sqrt((kappa * 5.9355e7) / Math.PI);

    // Cavity thermal fluctuations
    const dtCavityDiurnal = dtSurfDiurnal * Math.exp(-hRoofM / deltaDiurnalM);
    const dtSurfAnnual = dtSurfDiurnal * 0.30;
    const dtCavityAnnual = dtSurfAnnual * Math.exp(-hRoofM / deltaAnnualM);

    // Geothermal offset at roof base
    const Qgeo = 0.025; // W/m^2
    const TcavityK = Tann + (Qgeo / kRoof) * hRoofM;
    const TcavityC = TcavityK - 273.15;

    // Mass overburden shielding (g/cm^2)
    const massThicknessGCm2 = (rhoRoof * hRoofM) / 10.0;
    const radShieldPct = Math.min(99.99, 100.0 * (1.0 - Math.exp(-massThicknessGCm2 / 150.0)));

    let context = 'Ideal Subterranean Human Base Habitat & Biomarker Cold Trap Shelter';
    if (hRoofM < 2.0) {
      context = 'Thin Roof Skylight Zone (Partial Radiation & Moderate Thermal Fluctuations)';
    } else if (hRoofM > 30.0) {
      context = 'Deep Pyromajor Cavity / Complete Thermal & Cosmic Ray Isolation';
    }

    return {
      roofThicknessMeters: parseFloat(hRoofM.toFixed(1)),
      cavityMeanTempK: parseFloat(TcavityK.toFixed(2)),
      cavityMeanTempC: parseFloat(TcavityC.toFixed(2)),
      diurnalCavityFluctuationK: parseFloat(dtCavityDiurnal.toFixed(4)),
      annualCavityFluctuationK: parseFloat(dtCavityAnnual.toFixed(2)),
      radiationShieldingPercent: parseFloat(radShieldPct.toFixed(2)),
      cavernHabitatShelterContext: context
    };
  }

  /**
   * Calculate impact crater melt pool sheet thickness, Stefan phase-change crystallization time, and post-impact hydrothermal thermal lifetime.
   * h_melt = 0.025 * D_crater^1.3
   * t_solid = h_melt^2 / ( 4 * kappa * lambda_stefan^2 )
   * Reference: Turcotte & Schubert (2002), Abramov & Kring (2005), Keil (2012) for Gale & Jezero impact melt sheets.
   * @param {number} [craterDiameterKm=150.0] - Complex impact crater diameter in km (10 to 2000 km)
   * @param {number} [initialMeltTempC=1350.0] - Superheated impact melt pool temperature in Celsius (1100 to 2000 C)
   * @returns {{craterDiameterKm: number, meltSheetThicknessMeters: number, crystallizationTimeYears: number, hydrothermalActiveLifespanYears: number, impactMeltPetrogeneticContext: string}}
   */
  static computeImpactMeltPoolSolidificationAndGeothermalCooling(craterDiameterKm = 150.0, initialMeltTempC = 1350.0) {
    const Dkm = Math.max(5.0, Math.min(2500.0, craterDiameterKm));
    const Tinit = Math.max(1050.0, initialMeltTempC);

    // Complex crater melt sheet thickness (m): h = 0.25 * D^1.3
    const hMeltM = 0.25 * Math.pow(Dkm, 1.3);

    // Thermal properties
    const kMelt = 2.2; // W/(m*K)
    const rhoMelt = 2800.0; // kg/m^3
    const cpMelt = 1000.0; // J/(kg*K)
    const kappa = kMelt / (rhoMelt * cpMelt); // m^2/s (~7.857e-7)

    // Latent heat of crystallization & Stefan parameter
    const Lcryst = 4.0e5; // J/kg
    const Tsolidus = 1000.0;
    const deltaT = Tinit - Tsolidus;
    const lambdaStefan2 = (cpMelt * deltaT) / (2.0 * Lcryst);

    // Solidification time (seconds and years)
    const tSolidSec = Math.pow(hMeltM, 2.0) / (4.0 * kappa * Math.max(0.1, lambdaStefan2));
    const tSolidYears = tSolidSec / 3.15576e7;

    // Hydrothermal circulation lifespan (cooling to 100 C)
    const tHydroYears = tSolidYears * 5.0;

    let context = 'Major Basin-Scale Melt Pool (Differentiated Cumulate Melt Sheet & Long-Lived Hydrothermal System)';
    if (Dkm < 30.0) {
      context = 'Minor Impact Crater Melt Veneer / Rapid Glass Quenching';
    }

    return {
      craterDiameterKm: parseFloat(Dkm.toFixed(1)),
      meltSheetThicknessMeters: parseFloat(hMeltM.toFixed(1)),
      crystallizationTimeYears: parseFloat(tSolidYears.toFixed(1)),
      hydrothermalActiveLifespanYears: parseFloat(tHydroYears.toFixed(0)),
      impactMeltPetrogeneticContext: context
    };
  }

  /**
   * Calculate ancient Martian valley network precipitation runoff, peak fluvial discharge, Manning flow velocity, and bedload sediment competency.
   * Q_peak = ( C_runoff * I * A ) / 3.6
   * u_channel = ( 1 / n ) * R_h^(2/3) * S_0^(1/2)
   * tau_b = rho_water * g_mars * d_flow * S_0
   * Reference: Irwin et al. (2005), Fassett & Head (2008), Howard (2009) for Noachian/Hesperian valley network paleohydrology.
   * @param {number} [drainageBasinAreaKm2=5000.0] - Watershed catchment area in km^2 (50 to 500,000 km^2)
   * @param {number} [precipitationRateMmDay=15.0] - Effective rain/snowmelt precipitation intensity in mm/day (1 to 100 mm/day)
   * @param {number} [channelBedSlope=0.0035] - Longitudinal stream bed slope (0.0001 to 0.05)
   * @param {number} [manningRoughness=0.040] - Manning hydraulic roughness coefficient (0.025 to 0.080)
   * @returns {{drainageBasinAreaKm2: number, peakFluvialDischargeM3S: number, meanChannelFlowVelocityMS: number, basalBedShearStressPa: number, maxTransportableGrainDiameterCm: number, paleohydrologyFluvialContext: string}}
   */
  static computeAncientMartianValleyNetworkRunoffAndDischarge(drainageBasinAreaKm2 = 5000.0, precipitationRateMmDay = 15.0, channelBedSlope = 0.0035, manningRoughness = 0.040) {
    const Abasin = Math.max(10.0, drainageBasinAreaKm2);
    const ImmDay = Math.max(0.5, precipitationRateMmDay);
    const S0 = Math.max(0.0001, Math.min(0.10, channelBedSlope));
    const nRough = Math.max(0.015, Math.min(0.12, manningRoughness));

    const gMars = 3.72076; // m/s^2
    const rhoWater = 1000.0; // kg/m^3
    const rhoSed = 2800.0; // kg/m^3
    const Crunoff = 0.35; // Runoff fraction

    // Peak discharge Q (m^3/s)
    const ImmHr = ImmDay / 24.0;
    const QpeakM3S = (Crunoff * ImmHr * Abasin) / 3.6;

    // Channel geometry scaling from discharge (Leopold & Maddock empirical hydraulic geometry)
    const WchanM = 2.5 * Math.pow(Math.max(1.0, QpeakM3S), 0.48);
    const dFlowM = 0.35 * Math.pow(Math.max(1.0, QpeakM3S), 0.34);
    const RhM = (WchanM * dFlowM) / (WchanM + 2.0 * dFlowM);

    // Manning velocity (m/s)
    const uFlowMS = (1.0 / nRough) * Math.pow(RhM, 2.0 / 3.0) * Math.sqrt(S0);

    // Basal bed shear stress (Pa = N/m^2)
    const tauBPa = rhoWater * gMars * dFlowM * S0;

    // Shields maximum transportable grain diameter (cm)
    const thetaCrit = 0.045;
    const DgrainMaxM = tauBPa / (thetaCrit * (rhoSed - rhoWater) * gMars);
    const DgrainMaxCm = DgrainMaxM * 100.0;

    let regime = 'Perennial Cobble-Gravel Bedload Stream (Sustained Fluvial Valley Incision)';
    if (DgrainMaxCm > 30.0) {
      regime = 'High-Energy Megaflood Catastrophic Outflow (Boulder-Carrying Torrential Breaching)';
    } else if (DgrainMaxCm < 2.0) {
      regime = 'Low-Energy Silt/Sand Suspended-Load Meandering Channel';
    }

    return {
      drainageBasinAreaKm2: parseFloat(Abasin.toFixed(1)),
      peakFluvialDischargeM3S: parseFloat(QpeakM3S.toFixed(1)),
      meanChannelFlowVelocityMS: parseFloat(uFlowMS.toFixed(2)),
      basalBedShearStressPa: parseFloat(tauBPa.toFixed(2)),
      maxTransportableGrainDiameterCm: parseFloat(DgrainMaxCm.toFixed(1)),
      paleohydrologyFluvialContext: regime
    };
  }

  /**
   * Calculate ancient Martian Northern Ocean impact-generated tsunami wave speed, shoaling amplification, and inland coastal runup inundation.
   * c_wave = sqrt( g_mars * d_ocean )
   * H_coast = H_deep * ( d_deep / d_shelf )^(1/4)
   * R_runup = 1.05 * H_coast^0.9 * S_coast^(-0.2)
   * Reference: Costard et al. (2017), Rodriguez et al. (2019), Williams et al. (2024) for Oceanus Borealis & Deuteronilus paleoshoreline megatsunamis.
   * @param {number} [initialWaveHeightMeters=300.0] - Initial bolide impact cavity collapse wave amplitude in meters (50 to 1000 m)
   * @param {number} [oceanDepthMeters=1500.0] - Mean depth of Northern Ocean in meters (200 to 4000 m)
   * @param {number} [distanceToCoastKm=800.0] - Propagation distance from impact center to coastline in km (50 to 3000 km)
   * @param {number} [coastalTopographicSlope=0.005] - Regional coastal lowland slope (0.0005 to 0.05)
   * @returns {{openOceanWaveSpeedKmH: number, openOceanWaveSpeedMS: number, coastalShoalingWaveHeightMeters: number, maxInlandRunupElevationMeters: number, inlandInundationDistanceKm: number, tsunamiGeomorphologyContext: string}}
   */
  static computeAncientMartianOceanTsunamiPropagationAndRunup(initialWaveHeightMeters = 300.0, oceanDepthMeters = 1500.0, distanceToCoastKm = 800.0, coastalTopographicSlope = 0.005) {
    const H0 = Math.max(10.0, initialWaveHeightMeters);
    const dOcean = Math.max(50.0, oceanDepthMeters);
    const distKm = Math.max(10.0, distanceToCoastKm);
    const Sslope = Math.max(0.0001, Math.min(0.10, coastalTopographicSlope));

    const gMars = 3.72076; // m/s^2
    const r0Km = 50.0;

    // Deep ocean wave speed (m/s and km/h)
    const cWaveMS = Math.sqrt(gMars * dOcean);
    const cWaveKmH = cWaveMS * 3.6;

    // Geometric spreading and dissipation
    const alphaDiss = 0.0001; // 1/km
    const spreadFactor = Math.sqrt(r0Km / distKm);
    const dissFactor = Math.exp(-alphaDiss * Math.max(0.0, distKm - r0Km));
    const HdeepM = H0 * spreadFactor * dissFactor;

    // Green's law coastal shoaling (shelf depth = 100 m)
    const dShelf = 100.0;
    const shoalingFactor = Math.pow(dOcean / dShelf, 0.25);
    const HcoastM = HdeepM * shoalingFactor;

    // Maximum inland runup elevation and inundation distance
    const RrunupM = 1.05 * Math.pow(HcoastM, 0.9) * Math.pow(Sslope, -0.2);
    const XinundationKm = (RrunupM / Sslope) / 1000.0;

    let context = 'Catastrophic Megatsunami Inundation (Deposition of Widespread Lobate Boulder Fields)';
    if (RrunupM < 50.0) {
      context = 'Moderate Coastal Surge Event (Localized Shoreline Scour)';
    }

    return {
      openOceanWaveSpeedKmH: parseFloat(cWaveKmH.toFixed(1)),
      openOceanWaveSpeedMS: parseFloat(cWaveMS.toFixed(2)),
      coastalShoalingWaveHeightMeters: parseFloat(HcoastM.toFixed(1)),
      maxInlandRunupElevationMeters: parseFloat(RrunupM.toFixed(1)),
      inlandInundationDistanceKm: parseFloat(XinundationKm.toFixed(1)),
      tsunamiGeomorphologyContext: context
    };
  }

  /**
   * Calculate Martian North Polar Layered Deposits (NPLD) firn compaction, pore close-off bubble sealing depth, and Delta-age gas-ice chronological offset.
   * z_close = ( 1 / ( rho_ice * k_0 ) ) * ln( ( rho_crit / (rho_ice - rho_crit) ) / ( rho_0 / (rho_ice - rho_0) ) ) * sqrt( b_ice )
   * Delta_age = z_close / b_ice
   * Reference: Herron & Langway (1980), Sori et al. (2016), Becerra et al. (2021) for Martian polar paleoclimatic ice core stratigraphy.
   * @param {number} [iceAccumulationRateMmYr=0.55] - Polar water ice annual accumulation rate in mm/year (0.05 to 5.0 mm/yr)
   * @param {number} [meanSurfaceTempK=165.0] - Mean annual polar surface temperature in K (140 to 190 K)
   * @param {number} [dustVolumetricFractionPct=5.0] - Refractory silicate dust volume percentage in firn (0 to 30%)
   * @returns {{iceAccumulationRateMmYr: number, poreCloseOffDepthMeters: number, gasIceAgeOffsetYears: number, gasIceAgeOffsetKyr: number, firnColumnBulkDensityKgM3: number, polarPaleoclimateContext: string}}
   */
  static computeMartianPolarFirnCompactionAndGasAgeTrap(iceAccumulationRateMmYr = 0.55, meanSurfaceTempK = 165.0, dustVolumetricFractionPct = 5.0) {
    const bMmYr = Math.max(0.01, iceAccumulationRateMmYr);
    const Tsurf = Math.max(120.0, Math.min(220.0, meanSurfaceTempK));
    const dustPct = Math.max(0.0, Math.min(50.0, dustVolumetricFractionPct));

    const bMYr = bMmYr / 1000.0;
    const rho0 = 350.0; // Surface snow density (kg/m^3)
    const rhoCrit = 830.0; // Bubble close-off density (kg/m^3)
    const rhoIce = 920.0; // Pure solid ice density (kg/m^3)

    // Herron-Langway rate constant adapted for Mars gravity (g_mars/g_earth = 0.379)
    const R_GAS = 8.314;
    const Eact = 10160.0; // J/mol activation energy
    const gRatio = 0.379;
    const k0 = 11.0 * Math.exp(-Eact / (R_GAS * Tsurf)) * gRatio;

    // Pore close-off depth z_close (m)
    const term1 = Math.log(rhoCrit / (rhoIce - rhoCrit));
    const term2 = Math.log(rho0 / (rhoIce - rho0));
    const zCloseM = (1.0 / (rhoIce * Math.max(1e-6, k0))) * (term1 - term2) / Math.sqrt(bMYr);

    // Delta-Age gas-ice chronological offset
    const deltaAgeYears = zCloseM / bMYr;
    const deltaAgeKyr = deltaAgeYears / 1000.0;

    // Bulk firn density accounting for dust loading
    const rhoDust = 2800.0;
    const dustFrac = dustPct / 100.0;
    const rhoBulk = ((1.0 - dustFrac) * ((rho0 + rhoCrit) / 2.0)) + (dustFrac * rhoDust);

    let context = 'High-Resolution Orbital Paleoclimatic Ice Core Stratigraphy (NPLD Planum Boreum)';
    if (deltaAgeKyr > 100.0) {
      context = 'Ultra-Slow Accumulation Cold Desert Horizon (Large Gas-Ice Age Decoupling)';
    }

    return {
      iceAccumulationRateMmYr: parseFloat(bMmYr.toFixed(2)),
      poreCloseOffDepthMeters: parseFloat(zCloseM.toFixed(1)),
      gasIceAgeOffsetYears: parseFloat(deltaAgeYears.toFixed(0)),
      gasIceAgeOffsetKyr: parseFloat(deltaAgeKyr.toFixed(1)),
      firnColumnBulkDensityKgM3: parseFloat(rhoBulk.toFixed(1)),
      polarPaleoclimateContext: context
    };
  }

  /**
   * Calculate Martian subsurface pore ice sublimation loss, Knudsen vapor diffusion, and dry regolith desiccation front retreat over geological time.
   * D_eff = ( phi / tau ) * ( 2/3 * r_pore * v_bar )
   * z_lag = sqrt( ( 2 * D_eff * Delta_P * t ) / ( phi * rho_ice * R_v * T ) )
   * Reference: Fanale et al. (1986), Mellon & Jakosky (1993), Schorghofer (2007), Sizemore et al. (2015) for Martian ground ice stability.
   * @param {number} [meanAnnualGroundTempK=195.0] - Mean annual ground temperature in K (150 to 230 K)
   * @param {number} [poreRadiusMicrons=15.0] - Regolith mean pore radius in microns (1 to 100 um)
   * @param {number} [timeSpanMyr=2.5] - Sublimation diffusion duration in million years (0.1 to 50 Myr)
   * @param {number} [porosityFrac=0.35] - Regolith volumetric porosity (0.15 to 0.60)
   * @returns {{meanAnnualTempK: number, effectivePoreDiffusivityM2S: number, saturatedVaporPressurePa: number, desiccationLagDepthMeters: number, isGroundIceStableShallow: boolean, groundIcePreservationContext: string}}
   */
  static computeMartianSubsurfaceIceSublimationAndDesiccationFront(meanAnnualGroundTempK = 195.0, poreRadiusMicrons = 15.0, timeSpanMyr = 2.5, porosityFrac = 0.35) {
    const T = Math.max(130.0, Math.min(250.0, meanAnnualGroundTempK));
    const rPoreUm = Math.max(0.5, poreRadiusMicrons);
    const tMyr = Math.max(0.01, timeSpanMyr);
    const phi = Math.max(0.10, Math.min(0.70, porosityFrac));

    const tau = 2.5; // Regolith tortuosity
    const Rv = 461.5; // J/(kg*K)
    const rhoIce = 920.0; // kg/m^3
    const tSec = tMyr * 1e6 * 3.15576e7;

    // Mean thermal velocity v_bar (m/s)
    const vBar = Math.sqrt((8.0 * Rv * T) / Math.PI);

    // Knudsen diffusivity in pores: D_K = 2/3 * r_pore * v_bar
    const rPoreM = rPoreUm * 1e-6;
    const Dk = (2.0 / 3.0) * rPoreM * vBar;
    const Deff = (phi / tau) * Dk;

    // Saturated vapor pressure (Pa)
    const Psat = 611.65 * Math.exp(22.5 * (1.0 - 273.15 / T));
    const Patm = 0.005; // Pa ambient humidity
    const deltaP = Math.max(1e-6, Psat - Patm);

    // Desiccation front depth z_lag (m)
    let zLagM = Math.sqrt((2.0 * Deff * deltaP * tSec) / (phi * rhoIce * Rv * T));
    const isStable = T < 175.0;
    if (isStable) {
      zLagM = Math.min(0.05, zLagM);
    }

    let context = 'Mid-Latitude Subsurface Ice Decoupling (Dry Lag Overburden Layer)';
    if (isStable) {
      context = 'High-Latitude Stable Permafrost Ground Ice (Phoenix / Arcadia Planitia Type)';
    } else if (zLagM > 30.0) {
      context = 'Equatorial Hyper-Arid Complete Ice Desiccation (Deep Dessicated Megaregolith)';
    }

    return {
      meanAnnualTempK: parseFloat(T.toFixed(1)),
      effectivePoreDiffusivityM2S: parseFloat(Deff.toExponential(4)),
      saturatedVaporPressurePa: parseFloat(Psat.toFixed(5)),
      desiccationLagDepthMeters: parseFloat(zLagM.toFixed(2)),
      isGroundIceStableShallow: isStable,
      groundIcePreservationContext: context
    };
  }

  /**
   * Calculate South Polar Layered Deposits (SPLD) basal ice temperature, cryostatic overburden pressure, and hyper-saline perchlorate brine lake stability.
   * P_base = rho_ice * g_mars * H_ice
   * T_base = T_surf + ( q_geo * H_ice ) / k_ice
   * Reference: Orosei et al. (2018), Lauro et al. (2021), Sori & Bramson (2019) for MARSIS subglacial liquid water detection in Ultimi Scopuli.
   * @param {number} [iceThicknessMeters=1500.0] - Polar ice cap column thickness in meters (200 to 4000 m)
   * @param {number} [basalGeothermalFluxMWm2=45.0] - Basal crustal geothermal heat flux in mW/m^2 (15 to 150 mW/m^2)
   * @param {number} [surfaceTempK=160.0] - Mean annual polar surface ice temperature in K (140 to 180 K)
   * @param {number} [perchlorateBrineSalinityPct=35.0] - Magnesium/Calcium perchlorate salt mass percentage (0 to 45%)
   * @returns {{iceThicknessMeters: number, basalOverburdenPressureMPa: number, basalIceTempK: number, eutecticFreezingTempK: number, isSubglacialLiquidBrineStable: boolean, dielectricReflectivityContext: string}}
   */
  static computeMartianBasalIceMeltingAndSubglacialLakePressure(iceThicknessMeters = 1500.0, basalGeothermalFluxMWm2 = 45.0, surfaceTempK = 160.0, perchlorateBrineSalinityPct = 35.0) {
    const Hice = Math.max(50.0, iceThicknessMeters);
    const qGeoW = Math.max(0.005, basalGeothermalFluxMWm2 / 1000.0);
    const Tsurf = Math.max(120.0, Math.min(200.0, surfaceTempK));
    const saltPct = Math.max(0.0, Math.min(50.0, perchlorateBrineSalinityPct));

    const gMars = 3.72076; // m/s^2
    const rhoIce = 920.0; // kg/m^3
    const kIce = 2.1; // W/(m*K) bulk conductivity of dusty polar ice

    // Cryostatic overburden pressure (MPa)
    const PbasePa = rhoIce * gMars * Hice;
    const PbaseMPa = PbasePa / 1e6;

    // Basal temperature from steady-state conduction (K)
    const deltaT = (qGeoW * Hice) / kIce;
    const TbaseK = Tsurf + deltaT;

    // Eutectic freezing point depression of perchlorate brines (Mg(ClO4)2 eutectic at 205 K)
    const TeffEutecticK = 273.15 - (saltPct * 1.95);

    const isLiquid = TbaseK >= TeffEutecticK;

    let context = 'Frozen Cold-Based Basal Ice Sheet (Low Basal Radar Reflectivity)';
    if (isLiquid) {
      context = 'Stable Subglacial Hyper-Saline Perchlorate Brine Lake (High Dielectric Permittivity MARSIS Bright Reflector)';
    } else if (TbaseK >= 190.0) {
      context = 'Warm Near-Basal Clathrate Hydrate Ductile Creep Horizon';
    }

    return {
      iceThicknessMeters: parseFloat(Hice.toFixed(1)),
      basalOverburdenPressureMPa: parseFloat(PbaseMPa.toFixed(3)),
      basalIceTempK: parseFloat(TbaseK.toFixed(1)),
      eutecticFreezingTempK: parseFloat(TeffEutecticK.toFixed(1)),
      isSubglacialLiquidBrineStable: isLiquid,
      dielectricReflectivityContext: context
    };
  }

  /**
   * Calculate Martian cryovolcanic brine-ice slush effusion discharge, Poiseuille conduit flow, and viscous cryolava dome emplacement dimensions.
   * Q_eff = ( pi * R_vent^4 * Delta_P ) / ( 8 * mu_eff * L_conduit )
   * R_dome(t) = 0.85 * ( ( g_mars * rho_fluid * Q_eff^3 ) / mu_eff )^(1/8) * t^(1/2)
   * Reference: Fagents (2003), Kargel (2004), Brož et al. (2020) for Cerberus Fossae & Elysium Planitia cryomagmatism and mud volcanism.
   * @param {number} [ventRadiusMeters=15.0] - Cryovolcanic fissure/vent radius in meters (2 to 100 m)
   * @param {number} [conduitDepthMeters=2000.0] - Cryomagma source chamber depth in meters (500 to 10000 m)
   * @param {number} [slurryViscosityPaS=100000.0] - Dynamic viscosity of brine-ice slush in Pa*s (100 to 1e7 Pa*s)
   * @param {number} [eruptionDurationDays=30.0] - Continuous effusion duration in days (1 to 365 days)
   * @returns {{ventRadiusMeters: number, effusionDischargeRateM3S: number, totalExtrudedVolumeKm3: number, domeSpreadingRadiusKm: number, meanDomeThicknessMeters: number, cryovolcanicGeomorphologyContext: string}}
   */
  static computeMartianCryovolcanicEffusionAndDomeEmplacement(ventRadiusMeters = 15.0, conduitDepthMeters = 2000.0, slurryViscosityPaS = 100000.0, eruptionDurationDays = 30.0) {
    const Rvent = Math.max(1.0, ventRadiusMeters);
    const Lconduit = Math.max(100.0, conduitDepthMeters);
    const muEff = Math.max(1.0, slurryViscosityPaS);
    const durDays = Math.max(0.1, eruptionDurationDays);

    const gMars = 3.72076; // m/s^2
    const rhoFluid = 1200.0; // kg/m^3
    const deltaRho = 200.0; // Overpressure driving buoyancy density
    const tSec = durDays * 86400.0;

    // Chamber overpressure (Pa)
    const deltaPPa = Math.max(1e4, deltaRho * gMars * Lconduit);

    // Poiseuille conduit volumetric discharge (m^3/s)
    const QeffM3S = (Math.PI * Math.pow(Rvent, 4.0) * deltaPPa) / (8.0 * muEff * Lconduit);

    // Total extruded volume (m^3 and km^3)
    const VtotalM3 = QeffM3S * tSec;
    const VtotalKm3 = VtotalM3 / 1e9;

    // Viscous dome spreading radius R_dome (m and km)
    const factor = (gMars * rhoFluid * Math.pow(QeffM3S, 3.0)) / muEff;
    const RdomeM = 0.85 * Math.pow(factor, 0.125) * Math.sqrt(tSec);
    const RdomeKm = RdomeM / 1000.0;

    // Mean dome thickness (m)
    const HdomeM = VtotalM3 / (Math.PI * Math.pow(RdomeM, 2.0) / 2.0);

    let context = 'Viscous Cryomagmatic Slush Dome / Muck Volcano (Cerberus Fossae Type)';
    if (muEff < 1000.0) {
      context = 'Low-Viscosity Effusive Cryolava Sheet Flow (High-Mobility Brine Flooding)';
    } else if (HdomeM > 50.0) {
      context = 'Steep Cryovolcanic Spine / Endogenous Slurry Extrusion';
    }

    return {
      ventRadiusMeters: parseFloat(Rvent.toFixed(1)),
      effusionDischargeRateM3S: parseFloat(QeffM3S.toFixed(1)),
      totalExtrudedVolumeKm3: parseFloat(VtotalKm3.toFixed(4)),
      domeSpreadingRadiusKm: parseFloat(RdomeKm.toFixed(2)),
      meanDomeThicknessMeters: parseFloat(HdomeM.toFixed(1)),
      cryovolcanicGeomorphologyContext: context
    };
  }

  /**
   * Calculate ancient Martian volcanic mega-lahar debris flow dynamics, Bingham yield stress, mean flow velocity, and maximum runout distance.
   * tau_y = tau_0 * exp( b * ( C_v - 0.30 ) )
   * u_lahar = sqrt( 8 * g_mars * h_flow * S_0 / f_Darcy )
   * L_runout = 1.25 * V_km3^0.42 / S_0^0.5
   * Reference: Major (1997), Russell et al. (2003), Cagnoli et al. (2021) for Hecates Tholus & Hadriaca Patera volcano-ice lahar deposits.
   * @param {number} [laharVolumeKm3=25.0] - Total volcano-ice outburst lahar volume in km^3 (0.1 to 500 km^3)
   * @param {number} [channelBedSlope=0.015] - Flank slope of the volcanic edifice (0.001 to 0.15)
   * @param {number} [volcanicAshVolFractionPct=55.0] - Volcanic ash/lithic sediment volume percentage (30 to 70%)
   * @param {number} [flowDepthMeters=8.0] - Bankfull lahar flow depth in meters (1 to 50 m)
   * @returns {{laharVolumeKm3: number, slurryBulkDensityKgM3: number, binghamYieldStressPa: number, peakFlowVelocityMS: number, peakFlowVelocityKmH: number, maxRunoutDistanceKm: number, laharSedimentologyContext: string}}
   */
  static computeMartianVolcanicLaharDebrisFlowRunout(laharVolumeKm3 = 25.0, channelBedSlope = 0.015, volcanicAshVolFractionPct = 55.0, flowDepthMeters = 8.0) {
    const Vkm3 = Math.max(0.01, laharVolumeKm3);
    const S0 = Math.max(0.0005, Math.min(0.20, channelBedSlope));
    const CvPct = Math.max(25.0, Math.min(75.0, volcanicAshVolFractionPct));
    const hFlowM = Math.max(0.5, flowDepthMeters);

    const gMars = 3.72076; // m/s^2
    const rhoWater = 1000.0; // kg/m^3
    const rhoAsh = 2600.0; // kg/m^3
    const Cv = CvPct / 100.0;

    // Slurry bulk density (kg/m^3)
    const rhoSlurry = (Cv * rhoAsh) + ((1.0 - Cv) * rhoWater);

    // Bingham yield stress (Pa)
    const tauYPa = 25.0 * Math.exp(8.5 * (Cv - 0.30));

    // Peak flow velocity with friction Darcy factor f = 0.05 (m/s and km/h)
    const fDarcy = 0.055;
    const uFlowMS = Math.sqrt((8.0 * gMars * hFlowM * S0) / fDarcy);
    const uFlowKmH = uFlowMS * 3.6;

    // Maximum runout distance (km)
    const LrunoutKm = 1.25 * Math.pow(Vkm3, 0.42) / Math.sqrt(S0);

    let context = 'Catastrophic Volcano-Ice Mega-Lahar (Widespread Outwash Apron & Boulder Lobes)';
    if (LrunoutKm < 15.0) {
      context = 'Localized Flank Mudflow / Hyperconcentrated Torrential Splay';
    } else if (Vkm3 > 100.0) {
      context = 'Basin-Scale Volcanogenic Debris Avalanche / Regional Caldera-Breach Megaflood';
    }

    return {
      laharVolumeKm3: parseFloat(Vkm3.toFixed(1)),
      slurryBulkDensityKgM3: parseFloat(rhoSlurry.toFixed(1)),
      binghamYieldStressPa: parseFloat(tauYPa.toFixed(1)),
      peakFlowVelocityMS: parseFloat(uFlowMS.toFixed(2)),
      peakFlowVelocityKmH: parseFloat(uFlowKmH.toFixed(1)),
      maxRunoutDistanceKm: parseFloat(LrunoutKm.toFixed(1)),
      laharSedimentologyContext: context
    };
  }

  /**
   * Calculate Martian glacial basal thermal regime (cold-based vs warm-based), basal strain heating, and bedrock abrasion erosion rate.
   * T_base = T_surf + ( ( q_geo + q_strain ) * H_ice ) / k_ice
   * T_pmt = 273.15 - 0.074 * P_base_MPa
   * Reference: Fastook et al. (2012), Head & Marchant (2003), Brough et al. (2016) for Amazonian Lobate Debris Aprons (LDA) & non-erosive cold-based ice.
   * @param {number} [iceThicknessMeters=800.0] - Glacial ice column thickness in meters (100 to 4000 m)
   * @param {number} [iceSurfaceVelocityMYr=5.0] - Surface downslope flow velocity in m/year (0.1 to 100 m/yr)
   * @param {number} [geothermalHeatFluxMWm2=35.0] - Basal geothermal heat flux in mW/m^2 (10 to 120 mW/m^2)
   * @param {number} [surfaceMeanTempK=190.0] - Mean annual glacier surface temperature in K (150 to 220 K)
   * @returns {{iceThicknessMeters: number, basalTemperatureK: number, pressureMeltingPointK: number, isGlacierColdBased: boolean, basalSlidingVelocityMYr: number, bedrockErosionRateMmMyr: number, glacialGeomorphologyContext: string}}
   */
  static computeMartianGlacialThermalRegimeAndBedrockErosionRate(iceThicknessMeters = 800.0, iceSurfaceVelocityMYr = 5.0, geothermalHeatFluxMWm2 = 35.0, surfaceMeanTempK = 190.0) {
    const Hice = Math.max(50.0, iceThicknessMeters);
    const UsMYr = Math.max(0.01, iceSurfaceVelocityMYr);
    const qGeoW = Math.max(0.005, geothermalHeatFluxMWm2 / 1000.0);
    const Tsurf = Math.max(130.0, Math.min(230.0, surfaceMeanTempK));

    const gMars = 3.72076; // m/s^2
    const rhoIce = 920.0; // kg/m^3
    const kIce = 2.2; // W/(m*K)
    const slope = 0.02; // 2% mean flank gradient

    // Cryostatic pressure (MPa)
    const PbasePa = rhoIce * gMars * Hice;
    const PbaseMPa = PbasePa / 1e6;

    // Basal shear stress (Pa)
    const tauBPa = rhoIce * gMars * Hice * slope;

    // Strain heating flux (W/m^2)
    const usMS = UsMYr / 3.15576e7;
    const qStrainW = tauBPa * usMS;

    // Basal temperature (K)
    const deltaT = ((qGeoW + qStrainW) * Hice) / kIce;
    const TbaseK = Tsurf + deltaT;

    // Pressure melting point (K)
    const TpmtK = 273.15 - (0.074 * PbaseMPa);

    const isColdBased = TbaseK < TpmtK;

    let uSlideMYr = 0.0;
    let erosionRateMmMyr = 0.001; // Extremely low cold-based protective ice rate (1 mm / Gyr)
    let context = 'Cold-Based Non-Erosive Glaciation (Preserves Ancient Noachian Cratered Topography)';

    if (!isColdBased) {
      uSlideMYr = 0.75 * UsMYr;
      erosionRateMmMyr = 1500.0 * Math.pow(uSlideMYr / 5.0, 2.0); // Wet-based glacial quarrying
      context = 'Warm-Based Polythermal Glacier (Active Basal Sliding, Cirque Scouring & Glacial Grooving)';
    }

    return {
      iceThicknessMeters: parseFloat(Hice.toFixed(1)),
      basalTemperatureK: parseFloat(TbaseK.toFixed(1)),
      pressureMeltingPointK: parseFloat(TpmtK.toFixed(2)),
      isGlacierColdBased: isColdBased,
      basalSlidingVelocityMYr: parseFloat(uSlideMYr.toFixed(2)),
      bedrockErosionRateMmMyr: parseFloat(erosionRateMmMyr.toFixed(3)),
      glacialGeomorphologyContext: context
    };
  }

  /**
   * Calculate Martian subsurface magma chamber conductive cooling timescale, Stefan problem latent heat crystallization, and contact metamorphic hydrothermal aureole dimensions.
   * t_diff = R_chamber^2 / ( 4 * kappa )
   * Ste = c_p * Delta_T / L_m
   * t_solid = t_diff * ( 1 + 1 / Ste )
   * Reference: Jaeger (1968), Delaney (1987), Turcotte & Schubert (2002) for Tharsis & Elysium subvolcanic plutons and hydrothermal mineralization.
   * @param {number} [chamberRadiusKm=5.0] - Plutonic magma chamber equivalent spherical radius in km (0.5 to 50 km)
   * @param {number} [emplacementDepthKm=8.0] - Intrusion depth below Martian surface in km (1 to 30 km)
   * @param {number} [initialMagmaTempK=1450.0] - Initial basaltic magma liquidus temperature in K (1200 to 1600 K)
   * @param {number} [countryRockTempK=350.0] - Ambient host rock geothermal temperature in K (200 to 600 K)
   * @returns {{chamberRadiusKm: number, chamberVolumeKm3: number, conductiveCoolingTimeKyr: number, totalSolidificationTimeKyr: number, hydrothermalAureoleThicknessKm: number, degassedVolatileMassGigatons: number, plutonicMetamorphicContext: string}}
   */
  static computeMartianMagmaChamberCoolingAndContactAureole(chamberRadiusKm = 5.0, emplacementDepthKm = 8.0, initialMagmaTempK = 1450.0, countryRockTempK = 350.0) {
    const Rkm = Math.max(0.1, chamberRadiusKm);
    const Zkm = Math.max(0.5, emplacementDepthKm);
    const Tmagma = Math.max(1000.0, Math.min(1800.0, initialMagmaTempK));
    const Trock = Math.max(150.0, Math.min(800.0, countryRockTempK));

    const kappa = 1.0e-6; // m^2/s thermal diffusivity
    const cp = 1100.0; // J/(kg*K)
    const Lm = 4.0e5; // J/kg latent heat of basalt crystallization
    const rhoMagma = 2800.0; // kg/m^3
    const Rm = Rkm * 1000.0;

    // Chamber volume (km^3)
    const Vkm3 = (4.0 / 3.0) * Math.PI * Math.pow(Rkm, 3.0);

    // Conductive cooling diffusion timescale (seconds and kyr)
    const tDiffSec = Math.pow(Rm, 2.0) / (4.0 * kappa);
    const tDiffKyr = tDiffSec / (3.15576e7 * 1000.0);

    // Stefan number and latent heat crystallization correction
    const deltaT = Math.max(50.0, Tmagma - Trock);
    const ste = (cp * deltaT) / Lm;
    const fLatent = 1.0 + (1.0 / ste);
    const tSolidKyr = tDiffKyr * fLatent;

    // Hydrothermal aureole thickness (km)
    const WaureoleKm = 0.55 * Rkm;

    // Degassed volatile mass (Gigatons: 1 Gt = 1e9 kg) assuming 1.5 wt% dissolved H2O/CO2/SO2
    const totalMassKg = rhoMagma * (Vkm3 * 1e9);
    const degassedGt = (0.015 * totalMassKg) / 1e9;

    return {
      chamberRadiusKm: parseFloat(Rkm.toFixed(1)),
      chamberVolumeKm3: parseFloat(Vkm3.toFixed(1)),
      conductiveCoolingTimeKyr: parseFloat(tDiffKyr.toFixed(1)),
      totalSolidificationTimeKyr: parseFloat(tSolidKyr.toFixed(1)),
      hydrothermalAureoleThicknessKm: parseFloat(WaureoleKm.toFixed(2)),
      degassedVolatileMassGigatons: parseFloat(degassedGt.toFixed(1)),
      plutonicMetamorphicContext: `Plutonic Magma Chamber (${Vkm3.toFixed(0)} km^3, ~${tSolidKyr.toFixed(0)} kyr Solidification, ${degassedGt.toFixed(1)} Gt Volatile Outgassing)`
    };
  }

  /**
   * Calculate Martian sub-zero unfrozen cryopeg brine lens stability, multi-electrolyte freezing point depression, and confined artesian spring overpressure.
   * T_eutectic = 273.15 - 1.95 * S_salt_pct
   * Delta_P_artesian = ( rho_overburden - rho_brine ) * g_mars * z_depth
   * Reference: Gilichinsky et al. (2005), Marion et al. (2010), Heinz et al. (2016) for Permafrost Cryopegs & RSL Hypersaline Aquifers.
   * @param {number} [permafrostTempK=225.0] - Ambient permafrost bedrock temperature in K (170 to 270 K)
   * @param {number} [brineSalinityPct=30.0] - Magnesium/Calcium perchlorate and chloride salt mass percentage (5 to 45%)
   * @param {number} [regolithPorosityFrac=0.35] - Volumetric sediment pore fraction (0.10 to 0.60)
   * @param {number} [aquiferDepthMeters=250.0] - Confined cryopeg depth below surface in meters (20 to 2000 m)
   * @returns {{permafrostTempK: number, eutecticMeltingTempK: number, isLiquidBrineStableSubzero: boolean, unfrozenWaterVolumeFrac: number, artesianSpringOverpressureKPa: number, cryopegHydrologyContext: string}}
   */
  static computeMartianCryopegFreezingDepressionAndBrineHydrology(permafrostTempK = 225.0, brineSalinityPct = 30.0, regolithPorosityFrac = 0.35, aquiferDepthMeters = 250.0) {
    const Tperma = Math.max(150.0, Math.min(273.0, permafrostTempK));
    const saltPct = Math.max(1.0, Math.min(50.0, brineSalinityPct));
    const phi = Math.max(0.05, Math.min(0.65, regolithPorosityFrac));
    const zDepthM = Math.max(10.0, aquiferDepthMeters);

    const gMars = 3.72076; // m/s^2
    const rhoOverburden = 1900.0; // kg/m^3 frozen regolith
    const rhoBrine = 1250.0; // kg/m^3 dense perchlorate brine

    // Eutectic freezing depression (K)
    const TeutecticK = 273.15 - (saltPct * 1.95);
    const isLiquid = Tperma >= TeutecticK;

    // Unfrozen water fraction
    let thetaU = 0.0;
    if (isLiquid) {
      const tempRatio = Math.max(0.1, (273.15 - TeutecticK) / Math.max(1.0, 273.15 - Tperma));
      thetaU = Math.min(phi, phi * Math.pow(tempRatio, 0.65));
    }

    // Artesian confinement overpressure (kPa)
    const deltaRho = Math.max(100.0, rhoOverburden - rhoBrine);
    const deltaPPa = deltaRho * gMars * zDepthM;
    const deltaPKPa = deltaPPa / 1000.0;

    let context = 'Completely Frozen Solid Cryo-Aquitard (Impermeable Ice-Cemented Permafrost)';
    if (isLiquid) {
      context = 'Unfrozen Sub-Zero Hypersaline Cryopeg Lens (Confined Pressurized Aquifer & RSL Seepage Source)';
    } else if (Tperma >= TeutecticK - 10.0) {
      context = 'Metastable Supercooled Interfacial Water Films along Mineral Grains';
    }

    return {
      permafrostTempK: parseFloat(Tperma.toFixed(1)),
      eutecticMeltingTempK: parseFloat(TeutecticK.toFixed(1)),
      isLiquidBrineStableSubzero: isLiquid,
      unfrozenWaterVolumeFrac: parseFloat(thetaU.toFixed(3)),
      artesianSpringOverpressureKPa: parseFloat(deltaPKPa.toFixed(1)),
      cryopegHydrologyContext: context
    };
  }

  /**
   * Calculate volcanic acid fog (SO2/HCl) condensation, basaltic cation leaching, and residual amorphous siliceous hardpan duricrust formation.
   * J_acid = F_SO2 * ( RH / 100 ) * exp( -E_a / ( R * T ) ) * 1e3
   * w_SiO2 = 45.0 + 45.0 * ( 1 - exp( -M_acid / 150 ) )
   * h_crust = 0.05 * sqrt( M_acid )
   * Reference: Banin et al. (1997), Tosca et al. (2004), Yen et al. (2005) for Gusev Columbia Hills & Paso Robles high-silica duricrusts.
   * @param {number} [so2FluxMicrogM2S=50.0] - Volcanic fumarolic SO2 gas flux in micro-g/(m^2*s) (1 to 500 ug/m^2*s)
   * @param {number} [atmosphericRelativeHumidityPct=65.0] - Nocturnal relative humidity percentage (10 to 100%)
   * @param {number} [meanGroundTempK=210.0] - Mean diurnal surface temperature in K (170 to 260 K)
   * @param {number} [weatheringDurationYears=10000.0] - Duration of acid fog weathering exposure in years (100 to 500000 yrs)
   * @returns {{so2DepositionFluxMgM2Yr: number, cumulativeAcidLoadKgM2: number, residualSilicaWeightPct: number, hardpanDuricrustThicknessCm: number, acidWeatheringContext: string}}
   */
  static computeMartianAcidFogLeachingAndSiliceousHardpan(so2FluxMicrogM2S = 50.0, atmosphericRelativeHumidityPct = 65.0, meanGroundTempK = 210.0, weatheringDurationYears = 10000.0) {
    const Fso2 = Math.max(0.1, so2FluxMicrogM2S);
    const rh = Math.max(5.0, Math.min(100.0, atmosphericRelativeHumidityPct));
    const T = Math.max(140.0, Math.min(280.0, meanGroundTempK));
    const durYrs = Math.max(10.0, weatheringDurationYears);
    const Ea = 12000.0; // J/mol activation energy
    const R = 8.314; // J/(mol*K)

    // Acid deposition flux (mg/(m^2*yr)) converting micro-g/s to mg/yr
    const JacidMgYr = Fso2 * (rh / 100.0) * Math.exp(- Ea / (R * T)) * 31557.6;

    // Cumulative acid load (kg/m^2)
    const MacidKgM2 = (JacidMgYr * durYrs) / 1e6;

    // Residual amorphous silica enrichment (wt%) from cation stripping
    const wSiO2 = Math.min(95.0, 45.0 + 45.0 * (1.0 - Math.exp(- MacidKgM2 / 5.0)));

    // Hardpan duricrust thickness (cm)
    const hCrustCm = Math.max(0.1, 2.5 * Math.sqrt(MacidKgM2));

    let context = 'Siliceous Hardpan Duricrust / Acid-Leached Paso Robles Sinter (High Silica & Hydrated Sulfate Matrix)';
    if (wSiO2 < 55.0) {
      context = 'Incipient Acid Fog Condensation Crust / Incipient Surface Patina';
    } else if (wSiO2 >= 85.0) {
      context = 'Extreme Hydrothermal Acid Fog Leaching (Pristine Amorphous Silica Residue > 85 wt%)';
    }

    return {
      so2DepositionFluxMgM2Yr: parseFloat(JacidMgYr.toFixed(2)),
      cumulativeAcidLoadKgM2: parseFloat(MacidKgM2.toFixed(2)),
      residualSilicaWeightPct: parseFloat(wSiO2.toFixed(1)),
      hardpanDuricrustThicknessCm: parseFloat(hCrustCm.toFixed(1)),
      acidWeatheringContext: context
    };
  }

  /**
   * Calculate hydrothermal silica sinter maturation kinetics (Opal-A -> Opal-CT -> Quartz), diagenetic ostwald ripening, and porosity densification.
   * k_mat = A_0 * exp( -E_a / ( R * T ) ) * ( 1 + 1.2 * [Salinity] )
   * alpha_CT = 1 - exp( -k_mat * t )
   * alpha_Qtz = 1 - exp( - ( k_mat / 4.5 ) * t )
   * Reference: Herdianita et al. (2000), Lynne et al. (2007), Rodgers et al. (2004), Ruff et al. (2011) for Home Plate Gusev & Nili Fossae sinter beds.
   * @param {number} [initialPorosityFrac=0.55] - Fresh porous opal-A sinter porosity (0.20 to 0.80)
   * @param {number} [hydrothermalTempK=340.0] - Geothermal fluid temperature in K (275 to 450 K)
   * @param {number} [poreFluidSalinityMolar=0.5] - Brine salinity concentration in Molar (0.0 to 3.0 M)
   * @param {number} [exposureDurationYears=50000.0] - Thermal hydrothermal duration in years (100 to 2000000 yrs)
   * @returns {{dominantSilicaPhase: string, opalAWeightPct: number, opalCTWeightPct: number, microcrystallineQuartzWeightPct: number, evolvedPorosityFrac: number, sinterDiagenesisContext: string}}
   */
  static computeMartianSilicaSinteringKineticsAndQuartzMaturation(initialPorosityFrac = 0.55, hydrothermalTempK = 340.0, poreFluidSalinityMolar = 0.5, exposureDurationYears = 50000.0) {
    const phi0 = Math.max(0.1, Math.min(0.85, initialPorosityFrac));
    const T = Math.max(270.0, Math.min(500.0, hydrothermalTempK));
    const saltM = Math.max(0.0, Math.min(5.0, poreFluidSalinityMolar));
    const tYrs = Math.max(10.0, exposureDurationYears);

    const Ea = 68000.0; // J/mol activation energy
    const R = 8.314;
    const A0 = 1.25e5; // yr^-1

    // Maturation rate constant (yr^-1)
    const kMat = A0 * Math.exp(- Ea / (R * T)) * (1.0 + (1.2 * saltM));

    // Conversion fractions
    const alphaCT = 1.0 - Math.exp(- kMat * tYrs);
    const alphaQtz = 1.0 - Math.exp(- (kMat / 4.5) * tYrs);

    // Phase weight percentages
    const qtzPct = Math.min(100.0, alphaQtz * 100.0);
    const ctPct = Math.max(0.0, Math.min(100.0 - qtzPct, (alphaCT - alphaQtz) * 100.0));
    const opalAPct = Math.max(0.0, 100.0 - ctPct - qtzPct);

    // Porosity evolution
    const phiEvol = phi0 * Math.exp(- 0.25 * kMat * tYrs);

    let dominant = 'Amorphous Opal-A (Fresh Exhalative Sinter)';
    let context = 'Fresh Primary Hydrothermal Sinter / Columnar Spicules (Home Plate Type)';

    if (qtzPct >= 50.0) {
      dominant = 'Microcrystalline / Crystalline Quartz';
      context = 'Fully Matured Ancient Sinter / Hydrothermal Quartzite (Extensive Post-Depositional Diagenesis)';
    } else if (ctPct >= 40.0 || (ctPct + qtzPct) >= 50.0) {
      dominant = 'Paracrystalline Opal-CT';
      context = 'Diagenetically Matured Disordered Sinter / Lepispheres (Noctis Labyrinthus Type)';
    }

    return {
      dominantSilicaPhase: dominant,
      opalAWeightPct: parseFloat(opalAPct.toFixed(1)),
      opalCTWeightPct: parseFloat(ctPct.toFixed(1)),
      microcrystallineQuartzWeightPct: parseFloat(qtzPct.toFixed(1)),
      evolvedPorosityFrac: parseFloat(phiEvol.toFixed(3)),
      sinterDiagenesisContext: context
    };
  }

  /**
   * Calculate deep crustal hydrothermal convection cells, Rayleigh-Darcy stability number, upwelling Darcy velocity, and Nusselt convective heat flux in ancient impact basins.
   * Ra = ( rho_f^2 * g_mars * beta_T * c_f * k_perm * H * Delta_T ) / ( mu_f * K_m )
   * Ra_crit = 4 * pi^2
   * Nu = Ra / Ra_crit
   * u_z = ( k_perm * rho_f * g * beta_T * Delta_T ) / mu_f
   * Reference: Rathbun & Squyres (2002), Abramov & Kring (2005), Solomon et al. (2005) for Hellas, Isidis, & Argyre impact hydrothermal systems.
   * @param {number} [crustalPermeabilityM2=1.0e-13] - Fractured basalt aquifer permeability in m^2 (1e-16 to 1e-11 m^2)
   * @param {number} [permeableAquiferDepthKm=4.0] - Hydrothermal basin aquifer thickness in km (1.0 to 10.0 km)
   * @param {number} [basalHeatFluxMWm2=150.0] - Post-impact / plutonic basal heat flux in mW/m^2 (30 to 500 mW/m^2)
   * @param {number} [fluidViscosityPaS=2.5e-4] - Hot hydrothermal brine dynamic viscosity in Pa*s (1e-4 to 1e-3 Pa*s)
   * @returns {{rayleighDarcyNumber: number, criticalRayleighNumber: number, isHydrothermalConvectionActive: boolean, nusseltConvectiveMultiplier: number, upwellingFluidVelocityMYr: number, convectiveHeatDischargeWM2: number, hydrothermalConvectionContext: string}}
   */
  static computeMartianDeepHydrothermalConvectionAndRayleighDarcy(crustalPermeabilityM2 = 1.0e-13, permeableAquiferDepthKm = 4.0, basalHeatFluxMWm2 = 150.0, fluidViscosityPaS = 2.5e-4) {
    const kPerm = Math.max(1.0e-18, crustalPermeabilityM2);
    const HKm = Math.max(0.5, permeableAquiferDepthKm);
    const qGeoW = Math.max(0.01, basalHeatFluxMWm2 / 1000.0);
    const muF = Math.max(1.0e-5, fluidViscosityPaS);

    const gMars = 3.72076;
    const HM = HKm * 1000.0;
    const Km = 2.0; // W/(m*K) rock thermal conductivity
    const rhoF = 1000.0; // kg/m^3
    const betaT = 5.0e-4; // K^-1
    const cf = 4184.0; // J/(kg*K)

    // Conductive temperature difference across aquifer (K)
    const deltaT = (qGeoW * HM) / Km;

    // Rayleigh-Darcy dimensionless number
    const num = Math.pow(rhoF, 2.0) * gMars * betaT * cf * kPerm * HM * deltaT;
    const den = muF * Km;
    const Ra = num / den;

    const RaCrit = 4.0 * Math.pow(Math.PI, 2.0); // ~39.48
    const isConvecting = Ra > RaCrit;

    let Nu = 1.0;
    let uzMYr = 0.0;
    let qConvW = qGeoW;

    if (isConvecting) {
      Nu = Ra / RaCrit;
      const uzMS = (kPerm * rhoF * gMars * betaT * deltaT) / muF;
      uzMYr = uzMS * 3.15576e7;
      qConvW = qGeoW * Nu;
    }

    let context = 'Purely Conductive Crustal Regime (Permeability Insufficient for Hydrothermal Convection)';
    if (isConvecting) {
      context = `Vigorous Crustal Hydrothermal Convection (Ra/Ra_crit = ${Nu.toFixed(1)}, ~${uzMYr.toFixed(1)} m/yr Upwelling Darcy Circulation)`;
    }

    return {
      rayleighDarcyNumber: parseFloat(Ra.toFixed(1)),
      criticalRayleighNumber: parseFloat(RaCrit.toFixed(2)),
      isHydrothermalConvectionActive: isConvecting,
      nusseltConvectiveMultiplier: parseFloat(Nu.toFixed(1)),
      upwellingFluidVelocityMYr: parseFloat(uzMYr.toFixed(2)),
      convectiveHeatDischargeWM2: parseFloat(qConvW.toFixed(2)),
      hydrothermalConvectionContext: context
    };
  }

  /**
   * Calculate Martian impact shock Hugoniot equation of state, peak pressure attenuation, impact melt sheet volume, and thermal crystallization timescale.
   * P_0 = rho_0 * ( C_0 + S * u_p ) * u_p
   * V_melt = 0.0125 * E_k / ( rho_0 * ( c_p * Delta_T + L_m ) )
   * t_solid = ( H_sheet^2 / ( 4 * kappa ) ) * ( 1 + 1 / Ste )
   * Reference: Melosh (1989), Pierazzo et al. (1997), Grieve & Cintala (1992) for Planetary Impact Cratering & Shock Metamorphism.
   * @param {number} [impactorDiameterKm=5.0] - Spherical impactor diameter in km (0.1 to 50 km)
   * @param {number} [impactVelocityKmS=10.0] - Asteroid impact velocity in km/s (5 to 30 km/s)
   * @param {number} [targetBasaltDensityKgM3=2900.0] - Martian target crustal density in kg/m^3 (2200 to 3300 kg/m^3)
   * @param {number} [meltSheetThicknessMeters=120.0] - Central crater floor melt sheet thickness in meters (10 to 1000 m)
   * @returns {{peakHugoniotShockPressureGPa: number, impactKineticEnergyJoules: number, impactMeltVolumeKm3: number, meltSheetThicknessMeters: number, meltSheetSolidificationYears: number, shockMetamorphismContext: string}}
   */
  static computeMartianImpactShockAttenuationAndMeltSheet(impactorDiameterKm = 5.0, impactVelocityKmS = 10.0, targetBasaltDensityKgM3 = 2900.0, meltSheetThicknessMeters = 120.0) {
    const DimpKm = Math.max(0.05, impactorDiameterKm);
    const vImpKmS = Math.max(2.0, impactVelocityKmS);
    const rho0 = Math.max(1500.0, targetBasaltDensityKgM3);
    const HsheetM = Math.max(5.0, meltSheetThicknessMeters);

    const aM = (DimpKm * 1000.0) / 2.0;
    const vImpMS = vImpKmS * 1000.0;
    const upMS = vImpMS / 2.0; // symmetric 1D planar impact

    // Hugoniot parameters for basalt
    const C0 = 3200.0; // m/s
    const S = 1.45;
    const UsMS = C0 + (S * upMS);

    // Peak Hugoniot shock pressure (GPa)
    const P0Pa = rho0 * UsMS * upMS;
    const P0GPa = P0Pa / 1e9;

    // Impactor mass (kg) and kinetic energy (J)
    const MimpKg = (4.0 / 3.0) * Math.PI * Math.pow(aM, 3.0) * rho0;
    const EkJ = 0.5 * MimpKg * Math.pow(vImpMS, 2.0);

    // Melt generation (km^3)
    const cp = 1100.0;
    const Lm = 4.0e5;
    const deltaTMelt = 1400.0;
    const enthMelt = (cp * deltaTMelt) + Lm;
    const VmeltM3 = 0.0125 * (EkJ / (rho0 * enthMelt));
    const VmeltKm3 = VmeltM3 / 1e9;

    // Melt sheet solidification timescale (years)
    const kappa = 1.0e-6; // m^2/s
    const ste = (cp * 1200.0) / Lm;
    const tDiffSec = Math.pow(HsheetM, 2.0) / (4.0 * kappa);
    const tSolidSec = tDiffSec * (1.0 + (1.0 / ste));
    const tSolidYears = tSolidSec / 3.15576e7;

    return {
      peakHugoniotShockPressureGPa: parseFloat(P0GPa.toFixed(1)),
      impactKineticEnergyJoules: parseFloat(EkJ.toExponential(3)),
      impactMeltVolumeKm3: parseFloat(VmeltKm3.toFixed(2)),
      meltSheetThicknessMeters: parseFloat(HsheetM.toFixed(1)),
      meltSheetSolidificationYears: parseFloat(tSolidYears.toFixed(1)),
      shockMetamorphismContext: `Impact Shock Melt (${P0GPa.toFixed(0)} GPa Peak Shock, ${VmeltKm3.toFixed(1)} km^3 Melt, ~${tSolidYears.toFixed(0)} yr Crystallization)`
    };
  }

  /**
   * Calculate subglacial volcanic basal ice melting rate, subglacial cavity hydrostatic overpressure, and catastrophic outburst flood (Jokulhlaup) peak discharge.
   * h_dot_melt = q_volc / ( rho_ice * ( L_f + c_ice * Delta_T ) )
   * Delta_P_over = ( rho_w - rho_ice ) * g_mars * H_ice
   * Q_peak = 75.0 * ( V_cavity_m3 / 1e6 )^0.75
   * Reference: Head & Wilson (2002), Chapman et al. (2000), Burr et al. (2002) for Martian Subglacial Volcanism & Outflow Channel Megafloods.
   * @param {number} [iceCapThicknessKm=2.0] - Glacial ice sheet thickness in km (0.2 to 5.0 km)
   * @param {number} [subglacialVolcanicHeatFluxWM2=2500.0] - Volcanic fissure basal heat flux in W/m^2 (500 to 10000 W/m^2)
   * @param {number} [subglacialCavityVolumeKm3=25.0] - Subglacial melted water reservoir volume in km^3 (0.5 to 200 km^3)
   * @param {number} [iceTemperatureK=210.0] - Mean ice sheet temperature in K (150 to 260 K)
   * @returns {{basalIceMeltRateMYr: number, subglacialHydrostaticOverpressureKPa: number, peakJokulhlaupDischargeM3S: number, unitStreamPowerKWm: number, subglacialVolcanismContext: string}}
   */
  static computeMartianSubglacialVolcanicBasalMeltingAndJokulhlaup(iceCapThicknessKm = 2.0, subglacialVolcanicHeatFluxWM2 = 2500.0, subglacialCavityVolumeKm3 = 25.0, iceTemperatureK = 210.0) {
    const HiceKm = Math.max(0.1, iceCapThicknessKm);
    const qVolcW = Math.max(100.0, subglacialVolcanicHeatFluxWM2);
    const VcavityKm3 = Math.max(0.1, subglacialCavityVolumeKm3);
    const TiceK = Math.max(140.0, Math.min(270.0, iceTemperatureK));

    const gMars = 3.72076;
    const rhoIce = 920.0; // kg/m^3
    const rhoW = 1000.0; // kg/m^3
    const Lf = 3.34e5; // J/kg
    const cIce = 2090.0; // J/(kg*K)
    const TmeltK = 273.15;

    // Enthalpy to melt ice (J/kg)
    const deltaH = Lf + (cIce * (TmeltK - TiceK));

    // Basal melt rate (m/yr)
    const hDotMS = qVolcW / (rhoIce * deltaH);
    const hDotMYr = hDotMS * 3.15576e7;

    // Hydrostatic overpressure (kPa)
    const HiceM = HiceKm * 1000.0;
    const deltaPPa = (rhoW - rhoIce) * gMars * HiceM;
    const deltaPKPa = deltaPPa / 1000.0;

    // Peak jökulhlaup discharge (m^3/s)
    const VcavityM3 = VcavityKm3 * 1e9;
    const VmilM3 = VcavityM3 / 1e6;
    const QpeakM3S = 75.0 * Math.pow(VmilM3, 0.75);

    // Unit stream power (kW/m) at 0.005 regional slope
    const slope = 0.005;
    const omegaW = rhoW * gMars * QpeakM3S * slope;
    const omegaKW = omegaW / 1000.0;

    return {
      basalIceMeltRateMYr: parseFloat(hDotMYr.toFixed(1)),
      subglacialHydrostaticOverpressureKPa: parseFloat(deltaPKPa.toFixed(1)),
      peakJokulhlaupDischargeM3S: parseFloat(QpeakM3S.toFixed(0)),
      unitStreamPowerKWm: parseFloat(omegaKW.toFixed(1)),
      subglacialVolcanismContext: `Subglacial Jokulhlaup (${hDotMYr.toFixed(0)} m/yr Basal Melt, ${QpeakM3S.toFixed(0)} m^3/s Peak Megaflood Discharge)`
    };
  }

  /**
   * Calculate primordial Martian magma ocean volatile degassing, runaway steam atmosphere surface pressure, condensed ocean Global Equivalent Layer (GEL), and radiative cooling collapse timescale.
   * M_magma = 4/3 * pi * ( R_M^3 - ( R_M - d_magma )^3 ) * rho_sil
   * P_surf = ( M_degas * g_mars ) / ( 4 * pi * R_M^2 )
   * d_GEL = M_H2O / ( rho_w * A_surf )
   * t_collapse = Q_cryst / ( A_surf * F_simpson )
   * Reference: Elkins-Tanton (2008), Abe & Matsui (1988), Zahnle et al. (2007), Lammer et al. (2018) for Early Martian Magma Ocean Solidification.
   * @param {number} [magmaOceanDepthKm=1000.0] - Basal depth of primordial magma ocean in km (200 to 2000 km)
   * @param {number} [mantleWaterPpm=500.0] - Dissolved mantle water volatile concentration in ppm (100 to 2000 ppm)
   * @param {number} [mantleCo2Ppm=200.0] - Dissolved mantle carbon dioxide concentration in ppm (50 to 1000 ppm)
   * @returns {{magmaOceanMassKg: number, steamSurfacePressureBar: number, co2SurfacePressureBar: number, totalPrimordialPressureBar: number, oceanGELMeters: number, oceanCondensationTimescaleMyr: number, primordialClimateContext: string}}
   */
  static computeMartianMagmaOceanDegassingAndAtmosphereCollapse(magmaOceanDepthKm = 1000.0, mantleWaterPpm = 500.0, mantleCo2Ppm = 200.0) {
    const dMagmaKm = Math.max(50.0, Math.min(2500.0, magmaOceanDepthKm));
    const cH2O = Math.max(10.0, mantleWaterPpm) * 1e-6;
    const cCO2 = Math.max(10.0, mantleCo2Ppm) * 1e-6;

    const gMars = 3.72076;
    const RM = 3.3895e6; // Mars volumetric mean radius (m)
    const rhoSil = 3500.0; // kg/m^3
    const rhoW = 1000.0;
    const dMagmaM = dMagmaKm * 1000.0;

    // Magma volume & mass
    const rCoreM = Math.max(0.0, RM - dMagmaM);
    const vMagmaM3 = (4.0 / 3.0) * Math.PI * (Math.pow(RM, 3.0) - Math.pow(rCoreM, 3.0));
    const mMagmaKg = vMagmaM3 * rhoSil;

    // Degassed volatile masses (80% degassing efficiency)
    const etaDegas = 0.80;
    const mH2OKg = mMagmaKg * cH2O * etaDegas;
    const mCO2Kg = mMagmaKg * cCO2 * etaDegas;

    // Surface area
    const AsurfM2 = 4.0 * Math.PI * Math.pow(RM, 2.0);

    // Surface partial pressures (bar)
    const pH2OPa = (mH2OKg * gMars) / AsurfM2;
    const pCO2Pa = (mCO2Kg * gMars) / AsurfM2;
    const pH2OBar = pH2OPa / 1e5;
    const pCO2Bar = pCO2Pa / 1e5;
    const pTotBar = pH2OBar + pCO2Bar;

    // Global Equivalent Layer of condensed water (m)
    const dGELM = mH2OKg / (rhoW * AsurfM2);

    // Radiative cooling timescale (Myr)
    const Flimit = 280.0; // W/m^2 Simpson-Nakajima limit
    const enthCryst = (1200.0 * 800.0) + 4.0e5; // sensible + latent heat (J/kg)
    const QcrystJ = mMagmaKg * enthCryst;
    const tCondSec = QcrystJ / (AsurfM2 * Flimit);
    const tCondMyr = tCondSec / (3.15576e7 * 1e6);

    return {
      magmaOceanMassKg: parseFloat(mMagmaKg.toExponential(3)),
      steamSurfacePressureBar: parseFloat(pH2OBar.toFixed(1)),
      co2SurfacePressureBar: parseFloat(pCO2Bar.toFixed(1)),
      totalPrimordialPressureBar: parseFloat(pTotBar.toFixed(1)),
      oceanGELMeters: parseFloat(dGELM.toFixed(1)),
      oceanCondensationTimescaleMyr: parseFloat(tCondMyr.toFixed(2)),
      primordialClimateContext: `Magma Ocean Degassing (${pTotBar.toFixed(0)} bar Atmosphere, ${dGELM.toFixed(0)} m GEL Ocean, ~${tCondMyr.toFixed(2)} Myr Solidification)`
    };
  }

  /**
   * Calculate primordial Martian core thermal convection, adiabatic core heat flux limit, convective buoyancy flux, and dynamo magnetic surface field strength.
   * q_ad = k_core * ( alpha_T * g_cmb * T_cmb ) / c_p
   * F_B = ( alpha_T * g_cmb / ( rho_core * c_p ) ) * ( q_cmb - q_ad )
   * B_surf = B_core * ( R_core / R_M )^3
   * Reference: Stevenson (2001), Nimmo & Stevenson (2000), Connerney et al. (2004), Mittelholz et al. (2020) for Ancient Martian Core Dynamo.
   * @param {number} [coreRadiusKm=1830.0] - Metallic core radius in km (1500 to 2000 km, InSight SEIS)
   * @param {number} [coreMantleBoundaryHeatFluxMWm2=35.0] - Heat flow across CMB in mW/m^2 (5 to 100 mW/m^2)
   * @param {number} [coreTemperatureK=2000.0] - Core-mantle boundary temperature in K (1700 to 2400 K)
   * @returns {{adiabaticCoreHeatFluxMWm2: number, isThermalDynamoActive: boolean, convectiveHeatFluxMWm2: number, coreMagneticFieldMicroTesla: number, surfaceDipoleFieldMicroTesla: number, coreDynamoContext: string}}
   */
  static computeMartianCoreDynamoAndThermalConvection(coreRadiusKm = 1830.0, coreMantleBoundaryHeatFluxMWm2 = 35.0, coreTemperatureK = 2000.0) {
    const RcoreKm = Math.max(1000.0, Math.min(2500.0, coreRadiusKm));
    const qCmbMW = Math.max(1.0, coreMantleBoundaryHeatFluxMWm2);
    const TcmbK = Math.max(1500.0, coreTemperatureK);

    const RM = 3389.5; // km
    const RcoreM = RcoreKm * 1000.0;
    const G = 6.6743e-11;
    const rhoCore = 6500.0; // kg/m^3
    const kCore = 35.0; // W/(m*K)
    const alphaT = 3.5e-5; // K^-1
    const cp = 750.0; // J/(kg*K)
    const mu0 = 4.0 * Math.PI * 1e-7;

    // Gravity at CMB
    const gCmb = (4.0 / 3.0) * Math.PI * G * rhoCore * RcoreM;

    // Core adiabatic temperature gradient & heat flux (mW/m^2)
    const dTdRAd = (alphaT * gCmb * TcmbK) / cp;
    const qAdW = kCore * dTdRAd;
    const qAdMW = qAdW * 1000.0;

    // Superadiabatic convective excess
    const isDynamo = qCmbMW > qAdMW;
    const qConvMW = Math.max(0.0, qCmbMW - qAdMW);
    const qConvW = qConvMW / 1000.0;

    let BcoreUT = 0.0;
    let BsurfUT = 0.0;

    if (isDynamo) {
      // Convective buoyancy flux (m^2/s^3)
      const FB = ((alphaT * gCmb) / (rhoCore * cp)) * qConvW;

      // Christensen-Aubert dynamo scaling (Tesla)
      const BcoreT = 0.9 * Math.sqrt(mu0 * rhoCore) * Math.pow(FB * RcoreM, 1.0 / 3.0);
      BcoreUT = BcoreT * 1e6;

      // Dipole field at Martian surface (microTesla)
      const geoFactor = Math.pow(RcoreKm / RM, 3.0);
      BsurfUT = BcoreUT * geoFactor;
    }

    let context = 'Extinct Geodynamo (CMB Heat Flux Below Adiabat, Subcritical Convection)';
    if (isDynamo) {
      context = `Active Core Geodynamo (${BsurfUT.toFixed(0)} uT Surface Dipole Field, Strong Crustal Magnetization Regime)`;
    }

    return {
      adiabaticCoreHeatFluxMWm2: parseFloat(qAdMW.toFixed(2)),
      isThermalDynamoActive: isDynamo,
      convectiveHeatFluxMWm2: parseFloat(qConvMW.toFixed(2)),
      coreMagneticFieldMicroTesla: parseFloat(BcoreUT.toFixed(1)),
      surfaceDipoleFieldMicroTesla: parseFloat(BsurfUT.toFixed(1)),
      coreDynamoContext: context
    };
  }

  /**
   * Calculate thin-plate lithospheric flexure, flexural rigidity, central crustal deflection, and peripheral moat surrounding giant Martian volcanoes.
   * D_e = E * T_e^3 / ( 12 * ( 1 - nu^2 ) )
   * alpha = ( 4 * D_e / ( rho_mantle * g_mars ) )^(1/4)
   * w_0 = q_0 / ( rho_mantle * g_mars + D_e / R_volc^4 )
   * Reference: Turcotte & Schubert (2002), McGovern et al. (2002), Zuber et al. (2000), Belleguic et al. (2005) for Olympus Mons & Tharsis Lithosphere.
   * @param {number} [volcanoDiameterKm=600.0] - Shield volcano base diameter in km (50 to 1000 km, e.g. Olympus Mons)
   * @param {number} [volcanoHeightKm=21.0] - Volcano edifice height above datum in km (2 to 25 km)
   * @param {number} [lithosphericElasticThicknessKm=80.0] - Elastic lithosphere thickness Te in km (20 to 150 km)
   * @returns {{flexuralRigidityN_m: number, flexuralParameterKm: number, centralDeflectionKm: number, peripheralBulgeRadiusKm: number, peripheralMoatDepthKm: number, flexureContext: string}}
   */
  static computeMartianMantlePlumeLithosphericFlexure(volcanoDiameterKm = 600.0, volcanoHeightKm = 21.0, lithosphericElasticThicknessKm = 80.0) {
    const DvolcKm = Math.max(10.0, volcanoDiameterKm);
    const HvolcKm = Math.max(0.5, volcanoHeightKm);
    const TeKm = Math.max(5.0, lithosphericElasticThicknessKm);

    const gMars = 3.72076;
    const E = 1.0e11; // Young's modulus (Pa)
    const nu = 0.25; // Poisson's ratio
    const rhoVolc = 2900.0; // Basalt density (kg/m^3)
    const rhoMantle = 3500.0; // Mantle density (kg/m^3)

    const TeM = TeKm * 1000.0;
    const RvolcM = (DvolcKm * 1000.0) / 2.0;
    const HvolcM = HvolcKm * 1000.0;

    // Flexural rigidity D_e (N*m)
    const De = (E * Math.pow(TeM, 3.0)) / (12.0 * (1.0 - Math.pow(nu, 2.0)));

    // Flexural parameter alpha (m & km)
    const alphaM = Math.pow((4.0 * De) / (rhoMantle * gMars), 0.25);
    const alphaKm = alphaM / 1000.0;

    // Peak vertical load q_0 (Pa)
    const q0 = rhoVolc * gMars * HvolcM;

    // Central deflection w_0 (m & km)
    const kEffective = (rhoMantle * gMars) + (De / Math.pow(RvolcM, 4.0));
    const w0M = q0 / kEffective;
    const w0Km = w0M / 1000.0;

    // Peripheral flexural bulge radius (km)
    const rBulgeKm = Math.PI * alphaKm;

    // Peripheral annular depression moat depth (km)
    const xMoat = RvolcM / alphaM;
    const wMoatM = w0M * Math.exp(-xMoat) * Math.cos(xMoat);
    const wMoatKm = Math.abs(wMoatM / 1000.0);

    return {
      flexuralRigidityN_m: parseFloat(De.toExponential(3)),
      flexuralParameterKm: parseFloat(alphaKm.toFixed(1)),
      centralDeflectionKm: parseFloat(w0Km.toFixed(2)),
      peripheralBulgeRadiusKm: parseFloat(rBulgeKm.toFixed(1)),
      peripheralMoatDepthKm: parseFloat(wMoatKm.toFixed(2)),
      flexureContext: `Lithospheric Flexure (Te=${TeKm.toFixed(0)} km, w0=${w0Km.toFixed(1)} km Crustal Sag, alpha=${alphaKm.toFixed(0)} km)`
    };
  }

  /**
   * Calculate Rayleigh-Taylor gravitational overturn timescale of dense Fe-Ti cumulates, Stokes diapir sinking velocity, and CMB basal melt initiation.
   * lambda_RT = 2.56 * h_c
   * tau_RT = 4 * pi * eta / ( Delta_rho * g_mars * lambda_RT )
   * u_sink = 2 * Delta_rho * g_mars * R_diapir^2 / ( 9 * eta )
   * Reference: Hess & Parmentier (1995), Elkins-Tanton et al. (2005), Scheinberg et al. (2014) for Martian Mantle Overturn & Tharsis Plume Initiation.
   * @param {number} [cumulateLayerThicknessKm=50.0] - Dense Fe-Ti-rich ilmenite cumulate layer thickness in km (10 to 150 km)
   * @param {number} [densityInversionKgM3=300.0] - Density contrast Delta_rho in kg/m^3 (50 to 800 kg/m^3)
   * @param {number} [mantleViscosityPaS=1.0e20] - Dynamic mantle viscosity in Pa*s (1e19 to 1e22 Pa*s)
   * @returns {{rayleighTaylorWavelengthKm: number, overturnTimescaleMyr: number, diapirSinkingVelocityCmYr: number, cmbTransitTimescaleMyr: number, basalPlumeVolumeKm3: number, mantleOverturnContext: string}}
   */
  static computeMartianMantleOverturnAndBasalMagmaCrystallization(cumulateLayerThicknessKm = 50.0, densityInversionKgM3 = 300.0, mantleViscosityPaS = 1.0e20) {
    const hcKm = Math.max(5.0, cumulateLayerThicknessKm);
    const dRho = Math.max(10.0, densityInversionKgM3);
    const eta = Math.max(1e18, mantleViscosityPaS);

    const gMars = 3.72076;
    const hcM = hcKm * 1000.0;
    const dMantleM = 1500.0 * 1000.0; // 1500 km mantle depth to CMB

    // Dominant Rayleigh-Taylor wavelength (km & m)
    const lambdaM = 2.56 * hcM;
    const lambdaKm = lambdaM / 1000.0;

    // Instability growth timescale (Myr)
    const tauSec = (4.0 * Math.PI * eta) / (dRho * gMars * lambdaM);
    const tauMyr = tauSec / (3.15576e7 * 1e6);

    // Diapir sinking radius (m)
    const RdiapM = lambdaM / 4.0;
    const RdiapKm = RdiapM / 1000.0;

    // Stokes sinking velocity (m/s & cm/yr)
    const uSinkMS = (2.0 * dRho * gMars * Math.pow(RdiapM, 2.0)) / (9.0 * eta);
    const uSinkCmYr = (uSinkMS * 3.15576e7) * 100.0;

    // Transit time to CMB (Myr)
    const tCmbSec = dMantleM / uSinkMS;
    const tCmbMyr = tCmbSec / (3.15576e7 * 1e6);

    // Basal melt volume (km^3)
    const VmeltKm3 = (4.0 / 3.0) * Math.PI * Math.pow(RdiapKm, 3.0);

    return {
      rayleighTaylorWavelengthKm: parseFloat(lambdaKm.toFixed(1)),
      overturnTimescaleMyr: parseFloat(tauMyr.toFixed(3)),
      diapirSinkingVelocityCmYr: parseFloat(uSinkCmYr.toFixed(2)),
      cmbTransitTimescaleMyr: parseFloat(tCmbMyr.toFixed(1)),
      basalPlumeVolumeKm3: parseFloat(VmeltKm3.toFixed(0)),
      mantleOverturnContext: `Mantle Cumulate Overturn (~${tauMyr.toFixed(2)} Myr Instability, ${uSinkCmYr.toFixed(1)} cm/yr Sinking, ~${tCmbMyr.toFixed(0)} Myr to CMB)`
    };
  }

  /**
   * Calculate post-impact hydrothermal circulation, conductive melt sheet cooling timescale, Rayleigh-Darcy convection, and serpentinization H2 production in giant impact basins.
   * tau_cond = h_melt^2 / ( 4 * kappa )
   * Ra = rho_f * g_mars * alpha_f * k * Delta_T * h_melt / ( mu_f * kappa )
   * M_H2 = M_rock * X_ol * r_serp
   * Reference: Abramov & Kring (2005), Rathbun & Squyres (2002), Barnhart et al. (2010) for Impact Basin Hydrothermal Systems.
   * @param {number} [basinDiameterKm=1200.0] - Impact basin diameter in km (100 to 2500 km, e.g. Isidis/Argyre/Hellas)
   * @param {number} [meltSheetThicknessKm=5.0] - Impact melt sheet / central thermal anomaly thickness in km (1 to 20 km)
   * @param {number} [rockPermeabilityM2=1.0e-13] - Aquifer fractured breccia permeability in m^2 (1e-15 to 1e-11 m^2)
   * @param {number} [temperatureAnomalyK=400.0] - Hydrothermal fluid temperature excess above background in K (100 to 800 K)
   * @returns {{conductiveCoolingTimescaleKyr: number, rayleighDarcyNumber: number, isHydrothermalConvectionActive: boolean, nusseltHeatTransportNumber: number, hydrothermalLifespanKyr: number, hydrogenProductionTg: number, basinHydrothermalContext: string}}
   */
  static computeMartianBasinHydrothermalCoolingAndSerpentinization(basinDiameterKm = 1200.0, meltSheetThicknessKm = 5.0, rockPermeabilityM2 = 1.0e-13, temperatureAnomalyK = 400.0) {
    const DbasinKm = Math.max(50.0, basinDiameterKm);
    const HmeltKm = Math.max(0.5, meltSheetThicknessKm);
    const kPerm = Math.max(1e-17, rockPermeabilityM2);
    const dTK = Math.max(50.0, temperatureAnomalyK);

    const gMars = 3.72076;
    const kappa = 1.0e-6; // Rock thermal diffusivity (m^2/s)
    const HmeltM = HmeltKm * 1000.0;
    const rhoF = 950.0; // Water density at ~200 C (kg/m^3)
    const alphaF = 5.0e-4; // Water thermal expansion (K^-1)
    const muF = 2.0e-4; // Water dynamic viscosity at ~200 C (Pa*s)
    const rhoRock = 2900.0; // kg/m^3

    // Conductive cooling timescale (Kyr)
    const tauCondSec = Math.pow(HmeltM, 2.0) / (4.0 * kappa);
    const tauCondKyr = tauCondSec / (3.15576e7 * 1000.0);

    // Rayleigh-Darcy number
    const Ra = (rhoF * gMars * alphaF * kPerm * dTK * HmeltM) / (muF * kappa);
    const isConvective = Ra > 39.48; // Critical Rayleigh-Darcy threshold

    // Nusselt number and convective lifetime
    const Nu = isConvective ? Math.max(1.0, 0.025 * Ra) : 1.0;
    const tauHydroKyr = tauCondKyr / Nu;

    // Hydrogen production from serpentinization of ultramafic breccia (Tg = 10^9 kg)
    const RringM = (DbasinKm * 1000.0) / 6.0; // inner ring radius
    const MrockKg = Math.PI * Math.pow(RringM, 2.0) * HmeltM * rhoRock;
    const Xol = 0.35; // 35% olivine
    const rSerpH2 = 0.010; // 10 g H2 per kg peridotite
    const MH2Kg = MrockKg * Xol * rSerpH2;
    const MH2Tg = MH2Kg / 1e9;

    return {
      conductiveCoolingTimescaleKyr: parseFloat(tauCondKyr.toFixed(1)),
      rayleighDarcyNumber: parseFloat(Ra.toFixed(1)),
      isHydrothermalConvectionActive: isConvective,
      nusseltHeatTransportNumber: parseFloat(Nu.toFixed(1)),
      hydrothermalLifespanKyr: parseFloat(tauHydroKyr.toFixed(1)),
      hydrogenProductionTg: parseFloat(MH2Tg.toFixed(1)),
      basinHydrothermalContext: `Basin Hydrothermal System (${tauHydroKyr.toFixed(1)} kyr Active Circulation, Ra=${Ra.toFixed(0)}, ${MH2Tg.toFixed(0)} Tg H2 Produced)`
    };
  }

  /**
   * Calculate crustal magma chamber crystallization timescale, Stefan phase front propagation, latent heat release, and metamorphic aureole thickness.
   * Ste = c_p * ( T_sol - T_host ) / L_c
   * t_solid = ( H_ch / 2 )^2 / ( 4 * lambda_stefan^2 * kappa )
   * Q_latent = rho_magma * V_ch * L_c
   * Reference: Jaeger (1968), Marsh (1989), Ghiorso & Sack (1995) for Crustal Pluton Solidification.
   * @param {number} [chamberThicknessKm=3.0] - Magma sill / chamber vertical thickness in km (0.5 to 15.0 km)
   * @param {number} [chamberDepthKm=8.0] - Pluton emplacement depth in crust in km (2.0 to 30.0 km)
   * @param {number} [chamberRadiusKm=10.0] - Pluton horizontal radius in km (2.0 to 50.0 km)
   * @param {number} [magmaLiquidTempC=1200.0] - Magma liquidus intrusion temperature in deg C (900 to 1400 C)
   * @returns {{solidificationTimescaleKyr: number, stefanNumber: number, latentHeatEnergyExajoules: number, metamorphicAureoleThicknessKm: number, hostRockTempC: number, magmaSolidificationContext: string}}
   */
  static computeMartianMagmaChamberSolidificationAndCooling(chamberThicknessKm = 3.0, chamberDepthKm = 8.0, chamberRadiusKm = 10.0, magmaLiquidTempC = 1200.0) {
    const HchKm = Math.max(0.2, chamberThicknessKm);
    const DchKm = Math.max(1.0, chamberDepthKm);
    const RchKm = Math.max(0.5, chamberRadiusKm);
    const TliqC = Math.max(800.0, magmaLiquidTempC);

    const kappa = 1.0e-6; // Rock thermal diffusivity (m^2/s)
    const cp = 1200.0; // J/(kg*K)
    const Lc = 4.0e5; // Latent heat of crystallization (J/kg)
    const rhoM = 2800.0; // kg/m^3
    const geothermGrad = 15.0; // K/km geothermal gradient
    const TsurfC = -50.0; // Mean Martian surface temp (C)

    const HchM = HchKm * 1000.0;
    const bM = HchM / 2.0;
    const RchM = RchKm * 1000.0;

    // Host rock initial temperature at depth
    const ThostC = TsurfC + (DchKm * geothermGrad);
    const TsolC = TliqC - 200.0; // Solidus temperature

    // Stefan number
    const Ste = (cp * (TsolC - ThostC)) / Lc;
    const lambdaStefan = Math.sqrt(Ste / (2.0 * (1.0 + Ste / 3.0)));

    // Solidification timescale (Kyr)
    const tSolidSec = Math.pow(bM, 2.0) / (4.0 * Math.pow(lambdaStefan, 2.0) * kappa);
    const tSolidKyr = tSolidSec / (3.15576e7 * 1000.0);

    // Chamber volume & Latent heat release (EJ = 10^18 J)
    const VchM3 = Math.PI * Math.pow(RchM, 2.0) * HchM;
    const QlatentJ = rhoM * VchM3 * Lc;
    const QlatentEJ = QlatentJ / 1e18;

    // Contact metamorphic baking aureole thickness (km)
    const wAureoleKm = 0.6 * HchKm;

    return {
      solidificationTimescaleKyr: parseFloat(tSolidKyr.toFixed(1)),
      stefanNumber: parseFloat(Ste.toFixed(2)),
      latentHeatEnergyExajoules: parseFloat(QlatentEJ.toFixed(1)),
      metamorphicAureoleThicknessKm: parseFloat(wAureoleKm.toFixed(2)),
      hostRockTempC: parseFloat(ThostC.toFixed(1)),
      magmaSolidificationContext: `Magma Chamber Solidification (${tSolidKyr.toFixed(1)} kyr Solidification, ${QlatentEJ.toFixed(0)} EJ Latent Heat, ${wAureoleKm.toFixed(1)} km Aureole)`
    };
  }

  /**
   * Calculate Transition State Theory (TST) basaltic bedrock dissolution kinetics, weathering front penetration timescale, and clay neoformation regime.
   * r_diss = k_0 * exp( -E_a / ( R * T ) ) * a_H+^n_H+
   * t_weather = 1 / ( r_diss * A_spec * M_w )
   * Reference: Lasaga (1984), Bandfield et al. (2000), Zolotov & Mironenko (2007), Ehlmann et al. (2011) for Noachian Aqueous Weathering.
   * @param {number} [waterRockRatio=50.0] - Water-to-rock mass ratio W/R (0.1 to 1000.0)
   * @param {number} [fluidPH=6.5] - Weathering fluid pH (2.0 to 11.0)
   * @param {number} [weatheringTempC=25.0] - Weathering fluid temperature in deg C (0.0 to 90.0 C)
   * @param {number} [specificSurfaceAreaM2Kg=5000.0] - Rock specific surface area in m^2/kg (500 to 50000 m^2/kg)
   * @returns {{dissolutionRateMolM2S: number, weatheringFrontTimescaleKyrPerMeter: number, dominantNeoformedPhyllosilicate: string, geochemicalRegime: string, weatheringKineticsContext: string}}
   */
  static computeMartianBasaltWeatheringAndClayFormationKinetics(waterRockRatio = 50.0, fluidPH = 6.5, weatheringTempC = 25.0, specificSurfaceAreaM2Kg = 5000.0) {
    const wrRatio = Math.max(0.1, waterRockRatio);
    const pH = Math.max(1.0, Math.min(13.0, fluidPH));
    const tempC = Math.max(0.0, Math.min(150.0, weatheringTempC));
    const Aspec = Math.max(100.0, specificSurfaceAreaM2Kg);

    const R = 8.31446;
    const Ea = 60000.0; // J/mol activation energy
    const k0 = 1.0e-2; // mol/(m^2*s)
    const nH = 0.45; // reaction order
    const Mw = 0.100; // kg/mol mean basalt molar mass
    const TK = 273.15 + tempC;

    // Proton activity and dissolution rate (mol / (m^2 * s))
    const aH = Math.pow(10.0, -pH);
    const rDiss = k0 * Math.exp(-Ea / (R * TK)) * Math.pow(aH, nH);

    // Weathering timescale per meter of bedrock (Kyr/m)
    const tWeatherSec = 1.0 / (rDiss * Aspec * Mw);
    const tWeatherKyr = tWeatherSec / (3.15576e7 * 1000.0);

    // Geochemical regime & neoformed clay mineral
    let clayMineral = 'Fe/Mg Smectite (Nontronite / Saponite)';
    let regime = 'Stagnant Closed-Basin / Alkaline Diagenetic Regime';

    if (wrRatio >= 20.0 && pH <= 6.5) {
      clayMineral = 'Kaolinite / Halloysite (Al-Phyllosilicates)';
      regime = 'Open-System Intensive Leaching / Topset Weathering Profile';
    } else if (wrRatio >= 10.0 && pH <= 7.5) {
      clayMineral = 'Montmorillonite / Al-Smectite';
      regime = 'Moderate Leaching / Fluvial Alteration Horizon';
    } else if (pH > 8.5) {
      clayMineral = 'Saponite + Carbonate + Zeolite Assemblage';
      regime = 'Hyper-Alkaline Closed Paleolake Evaporation';
    }

    return {
      dissolutionRateMolM2S: rDiss,
      weatheringFrontTimescaleKyrPerMeter: parseFloat(tWeatherKyr.toFixed(1)),
      dominantNeoformedPhyllosilicate: clayMineral,
      geochemicalRegime: regime,
      weatheringKineticsContext: `Aqueous Weathering (${tWeatherKyr.toFixed(0)} kyr/m Alteration Rate, ${clayMineral}, ${regime})`
    };
  }

  /**
   * Calculate Smectite-to-Illite diagenetic transformation kinetics, illite fraction in mixed-layer I/S, and Reichweite ordering in deep crustal basins.
   * T_depth = T_surf + ( d * dT_dz )
   * k_ill = A * exp( -E_a / ( R * T ) ) * [K+]^0.5
   * X_ill = 1 - exp( -k_ill * t )
   * Reference: Hower et al. (1976), Pytte & Reynolds (1989), Michalski et al. (2017) for Martian Burial Diagenesis & Clay Dehydration.
   * @param {number} [burialDepthKm=4.0] - Crustal basin burial depth in km (0.5 to 15.0 km)
   * @param {number} [geothermalGradientKPerKm=30.0] - Geothermal gradient in K/km (10 to 60 K/km)
   * @param {number} [durationMyr=50.0] - Diagenetic hydrothermal reaction duration in Myr (1 to 500 Myr)
   * @param {number} [potassiumConcentrationMolar=0.010] - Porewater [K+] concentration in mol/L (0.001 to 0.5 mol/L)
   * @returns {{burialDepthKm: number, inSituTempC: number, illiteFractionPercent: number, orderingStructureClass: string, isIlliteDominated: boolean, smectiteIllitizationContext: string}}
   */
  static computeMartianSmectiteToIlliteTransformationKinetics(burialDepthKm = 4.0, geothermalGradientKPerKm = 30.0, durationMyr = 50.0, potassiumConcentrationMolar = 0.010) {
    const dKm = Math.max(0.2, burialDepthKm);
    const grad = Math.max(5.0, geothermalGradientKPerKm);
    const tMyr = Math.max(0.1, durationMyr);
    const concK = Math.max(1e-4, potassiumConcentrationMolar);

    const TsurfC = -50.0;
    const ThostC = TsurfC + (dKm * grad);
    const TK = 273.15 + ThostC;

    const R = 8.31446;
    const Ea = 110000.0; // J/mol activation energy
    const A = 5.0e7; // s^-1 pre-exponential factor

    // Reaction rate constant (s^-1 & Myr^-1)
    const kIllSec = A * Math.exp(-Ea / (R * TK)) * Math.sqrt(concK);
    const kIllMyr = kIllSec * (3.15576e7 * 1e6);

    // Illite fraction (0.0 to 1.0)
    const Xill = 1.0 - Math.exp(-kIllMyr * tMyr);
    const XillPct = Math.min(100.0, Math.max(0.0, Xill * 100.0));

    // Structural Reichweite ordering
    let orderClass = 'Randomly Interstratified I/S (R=0 Smectite Dominant)';
    if (XillPct >= 85.0) {
      orderClass = 'High-Grade Illite / Muscovite (R>=3 Reichweite Ordered)';
    } else if (XillPct >= 65.0) {
      orderClass = 'Ordered I/S (R=1 Kalkberg-Type Ordering)';
    } else if (XillPct >= 20.0) {
      orderClass = 'Interstratified Mixed-Layer I/S (R=0 Partial Illitization)';
    }

    return {
      burialDepthKm: parseFloat(dKm.toFixed(2)),
      inSituTempC: parseFloat(ThostC.toFixed(1)),
      illiteFractionPercent: parseFloat(XillPct.toFixed(1)),
      orderingStructureClass: orderClass,
      isIlliteDominated: XillPct >= 50.0,
      smectiteIllitizationContext: `Smectite Illitization (${XillPct.toFixed(0)}% Illite at ${ThostC.toFixed(0)} C / ${dKm.toFixed(1)} km Depth, ${orderClass})`
    };
  }

  /**
   * Calculate hydrothermal siliceous sinter mound precipitation rate, daily silica mass flux, and mound vertical accretion timescale.
   * log10( C_sat ) = 4.52 - 731 / T_K
   * M_dot = Q * ( C_fluid - C_sat_out )
   * h_dot = M_dot / ( pi * R^2 * rho_sinter * ( 1 - phi ) )
   * Reference: Fournier (1985), Campbell et al. (2015), Ruff & Farmer (2016) for Home Plate Hydrothermal Sinter Deposits.
   * @param {number} [springDischargeM3PerDay=100.0] - Thermal spring flow discharge in m^3/day (1.0 to 2000.0 m^3/d)
   * @param {number} [silicaConcentrationPpm=500.0] - Dissolved SiO2 in fluid in ppm (100.0 to 1500.0 ppm)
   * @param {number} [springTemperatureC=90.0] - Spring emergence temperature in deg C (30.0 to 120.0 C)
   * @param {number} [moundRadiusM=25.0] - Sinter apron deposit radius in m (5.0 to 200.0 m)
   * @param {number} [targetMoundHeightM=3.0] - Target total deposit height in m (0.5 to 20.0 m)
   * @returns {{dailySilicaMassFluxKg: number, annualSilicaTons: number, verticalAccretionRateMmPerYear: number, timeToAccreteYears: number, sinterDepositContext: string}}
   */
  static computeMartianHydrothermalSinterMoundAccretion(springDischargeM3PerDay = 100.0, silicaConcentrationPpm = 500.0, springTemperatureC = 90.0, moundRadiusM = 25.0, targetMoundHeightM = 3.0) {
    const Qm3d = Math.max(0.1, springDischargeM3PerDay);
    const Cppm = Math.max(50.0, silicaConcentrationPpm);
    const TspringC = Math.max(20.0, Math.min(150.0, springTemperatureC));
    const Rm = Math.max(2.0, moundRadiusM);
    const HtargetM = Math.max(0.1, targetMoundHeightM);

    const TsurfC = 10.0; // Ambient outflow temperature
    const TKout = 273.15 + TsurfC;
    const rhoSinter = 1800.0; // kg/m^3 dry sinter density
    const porosity = 0.25; // 25% opal sinter porosity

    // Amorphous silica saturation solubility at outflow temp (ppm)
    const logCsat = 4.52 - (731.0 / TKout);
    const CsatOutPpm = Math.pow(10.0, logCsat);

    // Excess precipitable silica concentration (kg/m^3)
    const deltaCPpm = Math.max(10.0, Cppm - CsatOutPpm);
    const deltaCKgM3 = deltaCPpm / 1000.0;

    // Daily silica mass flux (kg/day)
    const MdotDayKg = Qm3d * deltaCKgM3;
    const MdotYrKg = MdotDayKg * 365.25;
    const MdotYrTons = MdotYrKg / 1000.0;

    // Sinter mound deposit area (m^2)
    const AmoundM2 = Math.PI * Math.pow(Rm, 2.0);
    const rhoEff = rhoSinter * (1.0 - porosity);

    // Vertical accretion rate (m/yr and mm/yr)
    const hDotMYr = MdotYrKg / (AmoundM2 * rhoEff);
    const hDotMmYr = hDotMYr * 1000.0;

    // Time to accrete target mound height (years)
    const tAccreteYrs = (HtargetM * 1000.0) / hDotMmYr;

    return {
      dailySilicaMassFluxKg: parseFloat(MdotDayKg.toFixed(2)),
      annualSilicaTons: parseFloat(MdotYrTons.toFixed(2)),
      verticalAccretionRateMmPerYear: parseFloat(hDotMmYr.toFixed(2)),
      timeToAccreteYears: parseFloat(tAccreteYrs.toFixed(1)),
      sinterDepositContext: `Siliceous Sinter Mound (${hDotMmYr.toFixed(2)} mm/yr Accretion, ${MdotYrTons.toFixed(1)} t/yr SiO2, ${tAccreteYrs.toFixed(0)} yr for ${HtargetM.toFixed(1)}m Mound)`
    };
  }

  /**
   * Calculate volcanic acid-fog SO2 atmospheric fallout flux, top-down basaltic glass neutralization, and sulfate duricrust accretion rate.
   * J_SO2 = F_SO2 / ( 4 * pi * R_mars^2 )
   * M_dot_sulfate = n_dot_H2SO4 * eta * M_w
   * h_dot_crust = M_dot_sulfate / ( rho_crust * ( 1 - phi ) )
   * Reference: Tosca et al. (2004), Settle (1979), Zolotov & Mironenko (2007), Niles et al. (2017) for Burns Formation Sulfate Duricrust.
   * @param {number} [volcanicSo2FluxTgPerYr=100.0] - Global volcanic SO2 outgassing flux in Tg/yr (1.0 to 1000.0 Tg/yr)
   * @param {number} [neutralizationEfficiency=0.85] - Acid-basalt cation neutralization efficiency (0.1 to 1.0)
   * @param {number} [exposureDurationKyr=100.0] - Acid-fog episodic exposure duration in kyr (1.0 to 1000.0 kyr)
   * @returns {{acidFogDepositionFluxGM2Yr: number, annualSulfatePrecipitateGM2Yr: number, duricrustAccretionRateMmPerKyr: number, totalDuricrustThicknessCm: number, dominantSulfateAssemblage: string, acidFogContext: string}}
   */
  static computeMartianAcidFogBasaltWeatheringAndSulfateCrust(volcanicSo2FluxTgPerYr = 100.0, neutralizationEfficiency = 0.85, exposureDurationKyr = 100.0) {
    const FSo2Tg = Math.max(0.1, volcanicSo2FluxTgPerYr);
    const eta = Math.max(0.05, Math.min(1.0, neutralizationEfficiency));
    const tKyr = Math.max(0.1, exposureDurationKyr);

    const RmarsM = 3389.5 * 1000.0;
    const AmarsM2 = 4.0 * Math.PI * Math.pow(RmarsM, 2.0);
    const FSo2KgYr = FSo2Tg * 1e9; // 1 Tg = 10^9 kg

    // Global acid-fog deposition flux (kg/(m^2*yr) and g/(m^2*yr))
    const JSo2KgM2Yr = FSo2KgYr / AmarsM2;
    const JSo2GM2Yr = JSo2KgM2Yr * 1000.0;

    // Equivalent H2SO4 molar flux (mol/(m^2*yr))
    const nH2SO4MolM2Yr = JSo2KgM2Yr / 0.064066;

    // Sulfate salt precipitate mass flux (kg/(m^2*yr) and g/(m^2*yr))
    const MwSulfate = 0.150; // kg/mol (mean kieserite-gypsum)
    const MdotSulfateKgM2Yr = nH2SO4MolM2Yr * eta * MwSulfate;
    const MdotSulfateGM2Yr = MdotSulfateKgM2Yr * 1000.0;

    // Duricrust growth rate (mm/kyr)
    const rhoCrust = 2100.0; // kg/m^3
    const porosity = 0.30;
    const rhoEff = rhoCrust * (1.0 - porosity);
    const hDotMYr = MdotSulfateKgM2Yr / rhoEff;
    const hDotMmKyr = hDotMYr * 1000.0 * 1000.0; // mm per 1000 yr

    // Total duricrust thickness (cm)
    const hTotMm = hDotMmKyr * (tKyr / 1.0);
    const hTotCm = hTotMm / 10.0;

    // Mineralogical assemblage
    let sulfateClass = 'Mg-Fe-Ca Sulfate Duricrust (Kieserite + Jarosite + Gypsum)';
    if (eta >= 0.75) {
      sulfateClass = 'Polyhydrated Mg-Sulfate + Gypsum + Fe-Oxyhydroxide Duricrust';
    } else {
      sulfateClass = 'Acidic Jarosite + Alunite + Amorphous Silica Residue';
    }

    return {
      acidFogDepositionFluxGM2Yr: parseFloat(JSo2GM2Yr.toFixed(4)),
      annualSulfatePrecipitateGM2Yr: parseFloat(MdotSulfateGM2Yr.toFixed(4)),
      duricrustAccretionRateMmPerKyr: parseFloat(hDotMmKyr.toFixed(3)),
      totalDuricrustThicknessCm: parseFloat(hTotCm.toFixed(2)),
      dominantSulfateAssemblage: sulfateClass,
      acidFogContext: `Volcanic Acid-Fog Weathering (${hDotMmKyr.toFixed(2)} mm/kyr Duricrust Growth, ${hTotCm.toFixed(1)} cm Crust in ${tKyr.toFixed(0)} kyr)`
    };
  }

  /**
   * Calculate acidic hypersaline paleolake evaporative concentration, brine pH evolution, and sequential evaporite (Jarosite/Alunite/Kieserite/Halite) precipitation.
   * tau_dry = h_lake / E_evap
   * pH_brine = pH_0 - log10( CF )
   * M_salts = V_0 * C_salts
   * Reference: Tosca & McLennan (2006), Marion et al. (2008), Ehlmann et al. (2016) for Columbus Crater Acidic Paleolakes.
   * @param {number} [lakeVolumeKm3=50.0] - Initial paleolake water volume in km^3 (1.0 to 1000.0 km^3)
   * @param {number} [meanLakeDepthM=50.0] - Initial average lake depth in m (5.0 to 500.0 m)
   * @param {number} [initialPH=2.8] - Initial lake fluid pH (1.5 to 6.0)
   * @param {number} [evaporationRateMmYr=500.0] - Annual net evaporation rate in mm/yr (100 to 2000 mm/yr)
   * @returns {{desiccationTimescaleYears: number, finalBrinePH: number, totalEvaporiteMassTg: number, evaporiteBedThicknessCm: number, dominantPrecipitateStage: string, lakeEvaporiteContext: string}}
   */
  static computeMartianAcidicLakeEvaporitePrecipitation(lakeVolumeKm3 = 50.0, meanLakeDepthM = 50.0, initialPH = 2.8, evaporationRateMmYr = 500.0) {
    const V0Km3 = Math.max(0.1, lakeVolumeKm3);
    const h0M = Math.max(1.0, meanLakeDepthM);
    const pH0 = Math.max(0.5, Math.min(7.0, initialPH));
    const EevapMmYr = Math.max(50.0, evaporationRateMmYr);

    const EevapMYr = EevapMmYr / 1000.0;
    const rhoEvap = 2200.0; // kg/m^3 mean evaporite density

    // Lake drying timescale (years)
    const tDryYrs = h0M / EevapMYr;

    // 90% desiccation concentration factor CF = 10
    const CF = 10.0;
    const finalPH = Math.max(0.5, pH0 - Math.log10(CF));

    // Lake surface area (m^2)
    const V0M3 = V0Km3 * 1e9;
    const AlakeM2 = V0M3 / h0M;

    // Total precipitated salt mass (Tg = 10^9 kg, ~35 kg/m^3 dissolved salts in acidic sulfate brine)
    const saltConcKgM3 = 35.0;
    const MsaltsKg = V0M3 * saltConcKgM3;
    const MsaltsTg = MsaltsKg / 1e9;

    // Evaporite bed average thickness (cm)
    const hBedM = MsaltsKg / (AlakeM2 * rhoEvap);
    const hBedCm = hBedM * 100.0;

    // Mineralogical precipitation stage
    let prepStage = 'Jarosite + Alunite + Gypsum + Fe-Sulfate Sequence';
    if (finalPH <= 1.5) {
      prepStage = 'Hyper-Acidic Jarosite + Alunite + Kieserite + Bitter Halite Crust';
    } else if (pH0 >= 4.0) {
      prepStage = 'Neutral-to-Weakly Acidic Polyhydrated Mg-Sulfate + Gypsum Sequence';
    }

    return {
      desiccationTimescaleYears: parseFloat(tDryYrs.toFixed(1)),
      finalBrinePH: parseFloat(finalPH.toFixed(2)),
      totalEvaporiteMassTg: parseFloat(MsaltsTg.toFixed(1)),
      evaporiteBedThicknessCm: parseFloat(hBedCm.toFixed(1)),
      dominantPrecipitateStage: prepStage,
      lakeEvaporiteContext: `Acidic Paleolake Evaporite (${tDryYrs.toFixed(0)} yr Desiccation, ${hBedCm.toFixed(1)} cm Bed, pH ${pH0.toFixed(1)} -> ${finalPH.toFixed(1)}, ${prepStage})`
    };
  }

  /**
   * Calculate subsurface hydrothermal reservoir fluid temperature using quartz, chalcedony, and amorphous silica geothermometry equations.
   * T_quartz = 1309 / ( 5.19 - log10( C_ppm ) ) - 273.15
   * T_chalcedony = 1032 / ( 4.69 - log10( C_ppm ) ) - 273.15
   * T_opal = 731 / ( 4.52 - log10( C_ppm ) ) - 273.15
   * Reference: Fournier & Potter (1982), Verma (2000), Ruff et al. (2011) for Home Plate Hydrothermal Reservoirs.
   * @param {number} [dissolvedSilicaPpm=300.0] - Fluid dissolved silica concentration in ppm (10.0 to 1200.0 ppm)
   * @param {string} [silicaPolymorph='Quartz'] - Mineral polymorph ('Quartz', 'Chalcedony', 'Amorphous Silica / Opal')
   * @returns {{dissolvedSilicaPpm: number, silicaPolymorph: string, estimatedReservoirTempC: number, hydrothermalEnthalpyKjKg: number, reservoirRegime: string, geothermometerContext: string}}
   */
  static computeMartianSilicaGeothermometerFluidTemperature(dissolvedSilicaPpm = 300.0, silicaPolymorph = 'Quartz') {
    const Cppm = Math.max(5.0, Math.min(1500.0, dissolvedSilicaPpm));
    const poly = silicaPolymorph.toLowerCase();
    const logC = Math.log10(Cppm);

    let tempC = 0.0;
    let polyName = 'Quartz';

    if (poly.includes('chalcedon')) {
      polyName = 'Chalcedony';
      const denom = 4.69 - logC;
      tempC = denom > 0.1 ? (1032.0 / denom) - 273.15 : 250.0;
    } else if (poly.includes('opal') || poly.includes('amorph')) {
      polyName = 'Amorphous Silica (Opal-A)';
      const denom = 4.52 - logC;
      tempC = denom > 0.1 ? (731.0 / denom) - 273.15 : 150.0;
    } else {
      polyName = 'Quartz (Conductive No-Steam)';
      const denom = 5.19 - logC;
      tempC = denom > 0.1 ? (1309.0 / denom) - 273.15 : 300.0;
    }

    tempC = Math.max(0.0, Math.min(450.0, tempC));

    // Liquid water enthalpy approximation (kJ/kg: h ~= 4.184 * T_C)
    const enthalpyKjKg = 4.184 * tempC;

    let regime = 'Low-Temperature Hydrothermal Spring';
    if (tempC >= 220.0) {
      regime = 'High-Enthalpy Deep Magmatic-Hydrothermal Reservoir';
    } else if (tempC >= 150.0) {
      regime = 'Intermediate-Temperature Convective Hydrothermal System';
    } else if (tempC >= 80.0) {
      regime = 'Sub-Boiling Thermal Spring / Geyser Pool';
    }

    return {
      dissolvedSilicaPpm: parseFloat(Cppm.toFixed(1)),
      silicaPolymorph: polyName,
      estimatedReservoirTempC: parseFloat(tempC.toFixed(1)),
      hydrothermalEnthalpyKjKg: parseFloat(enthalpyKjKg.toFixed(1)),
      reservoirRegime: regime,
      geothermometerContext: `Silica Geothermometer (${polyName}: ${tempC.toFixed(0)} C / ${enthalpyKjKg.toFixed(0)} kJ/kg at ${Cppm.toFixed(0)} ppm SiO2 - ${regime})`
    };
  }

  /**
   * Calculate subsurface ground-ice permafrost sublimation retreat, Fickian vapor diffusion, and desiccation layer growth over orbital timescales.
   * z_dry(t) = sqrt( z_0^2 + 2 * D_eff * delta_rho_v * t / ( rho_ice * phi ) )
   * z_dot = D_eff * delta_rho_v / ( z_dry * rho_ice * phi )
   * Reference: Mellon & Phillips (2001), Schorghofer & Aharonson (2005), Dundas et al. (2014) for Martian Ground Ice Table Stability.
   * @param {number} [iceTableTemperatureK=205.0] - Mean subsurface ice table temperature in Kelvin (160 to 240 K)
   * @param {number} [atmosphericTemperatureK=190.0] - Mean atmospheric frost point temperature in Kelvin (150 to 220 K)
   * @param {number} [regolithDiffusivityM2S=2.0e-5] - Effective porous regolith vapor diffusivity in m^2/s (1e-6 to 1e-4 m^2/s)
   * @param {number} [icePoreFraction=0.35] - Volumetric ice pore fraction (0.10 to 0.90)
   * @param {number} [durationKyr=100.0] - Sublimation duration in kyr (1.0 to 1000.0 kyr)
   * @returns {{initialDryLayerCm: number, finalDryLayerThicknessM: number, finalDryLayerThicknessCm: number, instantaneousRetreatRateMmPerKyr: number, iceStabilityClass: string, permafrostContext: string}}
   */
  static computeMartianPermafrostSublimationRetreat(iceTableTemperatureK = 205.0, atmosphericTemperatureK = 190.0, regolithDiffusivityM2S = 2.0e-5, icePoreFraction = 0.35, durationKyr = 100.0) {
    const TiceK = Math.max(150.0, Math.min(250.0, iceTableTemperatureK));
    const TatmK = Math.max(140.0, Math.min(TiceK, atmosphericTemperatureK));
    const DeffM2S = Math.max(1e-7, regolithDiffusivityM2S);
    const phi = Math.max(0.05, Math.min(1.0, icePoreFraction));
    const tKyr = Math.max(0.1, durationKyr);

    const Rv = 461.5; // J/(kg*K)
    const Lsub = 2.834e6; // J/kg
    const rhoIce = 920.0; // kg/m^3
    const z0M = 0.05; // 5 cm initial protective lag

    // Clausius-Clapeyron saturation vapor pressures (Pa)
    const esatIce = 611.2 * Math.exp((Lsub / Rv) * ((1.0 / 273.15) - (1.0 / TiceK)));
    const esatAtm = 611.2 * Math.exp((Lsub / Rv) * ((1.0 / 273.15) - (1.0 / TatmK)));

    // Vapor densities (kg/m^3)
    const rhoVIce = esatIce / (Rv * TiceK);
    const rhoVAtm = esatAtm / (Rv * TatmK);
    const deltaRhoV = Math.max(1e-10, rhoVIce - rhoVAtm);

    // Diffusivity in m^2/yr
    const DeffM2Yr = DeffM2S * 3.15576e7;
    const tYr = tKyr * 1000.0;

    // Desiccation front depth (m)
    const rhoEffIce = rhoIce * phi;
    const zDrySq = Math.pow(z0M, 2.0) + ((2.0 * DeffM2Yr * deltaRhoV * tYr) / rhoEffIce);
    const zDryM = Math.sqrt(zDrySq);
    const zDryCm = zDryM * 100.0;

    // Instantaneous retreat rate (mm/kyr)
    const zDotMYr = (DeffM2Yr * deltaRhoV) / (zDryM * rhoEffIce);
    const zDotMmKyr = zDotMYr * 1000.0 * 1000.0;

    let stabClass = 'Metastable Deep Ground Ice (Lag-Protected)';
    if (zDryM <= 0.20) {
      stabClass = 'Shallow Stable Permafrost Table (Phoenix Landing Site Type)';
    } else if (zDryM >= 2.5) {
      stabClass = 'Deeply Desiccated Dry Regolith Column';
    }

    return {
      initialDryLayerCm: 5.0,
      finalDryLayerThicknessM: parseFloat(zDryM.toFixed(3)),
      finalDryLayerThicknessCm: parseFloat(zDryCm.toFixed(1)),
      instantaneousRetreatRateMmPerKyr: parseFloat(zDotMmKyr.toFixed(2)),
      iceStabilityClass: stabClass,
      permafrostContext: `Ground Ice Sublimation (${zDryCm.toFixed(0)} cm Desiccated Overburden in ${tKyr.toFixed(0)} kyr, ${zDotMmKyr.toFixed(1)} mm/kyr Retreat Rate)`
    };
  }

  /**
   * Calculate volcanic lava tube subsurface thermal attenuation, diurnal and seasonal skin depths, and cave microclimate temperature stability.
   * delta_diurn = sqrt( kappa * P_sol / pi )
   * delta_season = sqrt( kappa * P_year / pi )
   * A(z) = A_0 * exp( -z / delta )
   * Reference: Williams et al. (2010), Titus et al. (2021), Cushing (2012) for Martian Cave & Lava Tube Microclimates.
   * @param {number} [depthBelowSurfaceM=15.0] - Lava tube ceiling depth below surface in meters (1.0 to 100.0 m)
   * @param {number} [rockThermalDiffusivityM2S=8.0e-7] - Host basalt rock thermal diffusivity in m^2/s (1e-7 to 2e-6 m^2/s)
   * @param {number} [meanSurfaceTempK=210.0] - Annual mean surface temperature in Kelvin (150 to 260 K)
   * @param {number} [surfaceDiurnalAmplitudeK=45.0] - Surface day-night temperature amplitude in Kelvin (10 to 80 K)
   * @param {number} [surfaceSeasonalAmplitudeK=30.0] - Surface summer-winter temperature amplitude in Kelvin (5 to 60 K)
   * @returns {{diurnalSkinDepthCm: number, seasonalSkinDepthM: number, caveDiurnalAmplitudeK: number, caveSeasonalAmplitudeK: number, caveMeanTempC: number, caveMinTempC: number, caveMaxTempC: number, thermalBufferingClass: string, caveMicroclimateContext: string}}
   */
  static computeMartianLavaTubeMicroclimateThermalDamping(depthBelowSurfaceM = 15.0, rockThermalDiffusivityM2S = 8.0e-7, meanSurfaceTempK = 210.0, surfaceDiurnalAmplitudeK = 45.0, surfaceSeasonalAmplitudeK = 30.0) {
    const zM = Math.max(0.2, depthBelowSurfaceM);
    const kappa = Math.max(1e-8, rockThermalDiffusivityM2S);
    const TmeanK = Math.max(120.0, Math.min(300.0, meanSurfaceTempK));
    const Adiurn0 = Math.max(0.0, surfaceDiurnalAmplitudeK);
    const Aseas0 = Math.max(0.0, surfaceSeasonalAmplitudeK);

    const PsolSec = 88775.2; // 1 sol in seconds
    const PyearSec = 668.6 * PsolSec; // 1 Martian year in seconds

    // Skin depths
    const deltaDiurnM = Math.sqrt((kappa * PsolSec) / Math.PI);
    const deltaDiurnCm = deltaDiurnM * 100.0;

    const deltaSeasM = Math.sqrt((kappa * PyearSec) / Math.PI);

    // Amplitudes at depth z
    const AdiurnZ = Adiurn0 * Math.exp(-zM / deltaDiurnM);
    const AseasZ = Aseas0 * Math.exp(-zM / deltaSeasM);

    // Temperatures in Celsius
    const TmeanC = TmeanK - 273.15;
    const TminC = TmeanC - AseasZ - AdiurnZ;
    const TmaxC = TmeanC + AseasZ + AdiurnZ;

    let bufClass = 'Isothermal Cave Interior (Ultra-Stable Microclimate)';
    if (AseasZ >= 5.0) {
      bufClass = 'Shallow Pit Crater / Partially Damped Microclimate';
    } else if (AseasZ >= 1.0) {
      bufClass = 'Thermally Buffered Subsurface Cavity';
    }

    return {
      diurnalSkinDepthCm: parseFloat(deltaDiurnCm.toFixed(1)),
      seasonalSkinDepthM: parseFloat(deltaSeasM.toFixed(2)),
      caveDiurnalAmplitudeK: parseFloat(AdiurnZ.toFixed(4)),
      caveSeasonalAmplitudeK: parseFloat(AseasZ.toFixed(3)),
      caveMeanTempC: parseFloat(TmeanC.toFixed(1)),
      caveMinTempC: parseFloat(TminC.toFixed(2)),
      caveMaxTempC: parseFloat(TmaxC.toFixed(2)),
      thermalBufferingClass: bufClass,
      caveMicroclimateContext: `Lava Tube Microclimate (${TmeanC.toFixed(1)} C Mean, +/-${AseasZ.toFixed(2)} K Annual Oscillation at ${zM.toFixed(0)}m Depth, ${bufClass})`
    };
  }

  /**
   * Calculate volcanic fumarolic acid-gas leaching of host basalt, cation stripping, and residual pure silica halo formation timescale.
   * M_basalt = pi * R_halo^2 * H * rho_basalt * ( 1 - phi )
   * M_cation = M_basalt * 0.52
   * t_leach = M_cation / ( M_dot_gas * eta )
   * Reference: Ruff et al. (2011), Squyres et al. (2008), Morris et al. (2008) for Home Plate Fumarolic Silica Haloes.
   * @param {number} [fumaroleGasTempC=250.0] - Fumarolic vent gas temperature in deg C (80 to 600 C)
   * @param {number} [so2GasFluxKgPerDay=500.0] - Daily acid gas SO2+H2S flux in kg/day (10 to 10000 kg/d)
   * @param {number} [haloRadiusM=5.0] - Siliceous alteration halo radius in meters (1.0 to 50.0 m)
   * @param {number} [haloConduitHeightM=10.0] - Alteration conduit column height in meters (2.0 to 100.0 m)
   * @returns {{annualAcidGasFluxTons: number, totalHostBasaltMassTons: number, leachedCationMassTons: number, residualSilicaMassTons: number, completeSilicificationTimescaleYears: number, alterationGradeClass: string, fumaroleContext: string}}
   */
  static computeMartianFumarolicAcidSulfateAlteration(fumaroleGasTempC = 250.0, so2GasFluxKgPerDay = 500.0, haloRadiusM = 5.0, haloConduitHeightM = 10.0) {
    const TgasC = Math.max(50.0, Math.min(800.0, fumaroleGasTempC));
    const QgasDayKg = Math.max(1.0, so2GasFluxKgPerDay);
    const RhaloM = Math.max(0.5, haloRadiusM);
    const HhaloM = Math.max(1.0, haloConduitHeightM);

    const rhoBasalt = 2800.0; // kg/m^3
    const porosity = 0.15;
    const etaAcid = 0.85; // 85% reaction efficiency

    // Annual gas flux (tons/yr)
    const QgasYrKg = QgasDayKg * 365.25;
    const QgasYrTons = QgasYrKg / 1000.0;

    // Host basalt volume and mass (tons)
    const VhaloM3 = Math.PI * Math.pow(RhaloM, 2.0) * HhaloM;
    const MbasaltKg = VhaloM3 * rhoBasalt * (1.0 - porosity);
    const MbasaltTons = MbasaltKg / 1000.0;

    // Chemical leaching fractions (52 wt% cations stripped: Fe, Mg, Ca, Al, Na; 48 wt% SiO2 residue)
    const McationKg = MbasaltKg * 0.52;
    const McationTons = McationKg / 1000.0;

    const MsilicaKg = MbasaltKg * 0.48;
    const MsilicaTons = MsilicaKg / 1000.0;

    // Timescale to completely silicify halo (years)
    const tSilicifyYrs = McationKg / (QgasYrKg * etaAcid);

    let gradeClass = 'Extreme Acid-Leached Siliceous Residue (>90 wt% Opal-A Silica)';
    if (TgasC >= 300.0) {
      gradeClass = 'High-Temperature Acid-Sulfate Fumarolic Silicification Halo';
    } else if (TgasC < 120.0) {
      gradeClass = 'Low-Temperature Solfataric Acid-Leached Sinter Alteration';
    }

    return {
      annualAcidGasFluxTons: parseFloat(QgasYrTons.toFixed(1)),
      totalHostBasaltMassTons: parseFloat(MbasaltTons.toFixed(1)),
      leachedCationMassTons: parseFloat(McationTons.toFixed(1)),
      residualSilicaMassTons: parseFloat(MsilicaTons.toFixed(1)),
      completeSilicificationTimescaleYears: parseFloat(tSilicifyYrs.toFixed(2)),
      alterationGradeClass: gradeClass,
      fumaroleContext: `Fumarolic Silica Halo (${tSilicifyYrs.toFixed(1)} yr Silicification, ${MsilicaTons.toFixed(0)} t Opal-A Residue in ${RhaloM.toFixed(1)}m Halo, ${gradeClass})`
    };
  }

  /**
   * Calculate ice-covered Martian paleolake thermal equilibrium, conductive ice lid heat loss, sub-ice Rayleigh convection, and bottom water persistence.
   * q_cond = k_ice * ( T_freeze - T_surf ) / h_ice
   * h_ice_eq = k_ice * ( T_freeze - T_surf ) / q_basal
   * Ra = g * alpha * delta_T * H_water^3 / ( nu * kappa )
   * Reference: McKay et al. (1985), Fastook et al. (2012), Wordsworth (2016) for Gale & Jezero Ice-Covered Paleolakes.
   * @param {number} [totalLakeDepthM=100.0] - Total paleolake basin depth in meters (10 to 2000 m)
   * @param {number} [surfaceAirTempK=230.0] - Annual mean surface atmosphere temperature in Kelvin (180 to 265 K)
   * @param {number} [activeIceThicknessM=15.0] - Current ice lid thickness in meters (1 to 500 m)
   * @param {number} [basalHeatFlowMwM2=50.0] - Geothermal basal heat flow in mW/m^2 (20 to 200 mW/m^2)
   * @returns {{conductiveIceHeatFluxWM2: number, equilibriumIceLidThicknessM: number, subIceLiquidWaterDepthM: number, rayleighNumber: number, bottomWaterTempC: number, lakeThermalRegime: string, paleolakeContext: string}}
   */
  static computeMartianIceCoveredPaleolakeThermalEquilibrium(totalLakeDepthM = 100.0, surfaceAirTempK = 230.0, activeIceThicknessM = 15.0, basalHeatFlowMwM2 = 50.0) {
    const HtotM = Math.max(5.0, totalLakeDepthM);
    const TsurfK = Math.max(150.0, Math.min(270.0, surfaceAirTempK));
    const HiceM = Math.max(0.5, Math.min(HtotM - 1.0, activeIceThicknessM));
    const qBasalWM2 = Math.max(10.0, basalHeatFlowMwM2) / 1000.0;

    const kIce = 2.22; // W/(m*K)
    const TfreezeK = 273.15;
    const deltaTIce = TfreezeK - TsurfK;

    // Upward conductive heat loss through active ice lid (W/m^2)
    const qCondWM2 = (kIce * deltaTIce) / HiceM;

    // Theoretical equilibrium ice thickness under purely basal + convective heat flux (m)
    const qTotBasal = qBasalWM2 + 0.020; // 20 mW/m^2 convective support
    const hIceEqM = (kIce * deltaTIce) / qTotBasal;

    // Liquid water column depth (m)
    const HwaterM = Math.max(1.0, HtotM - HiceM);

    // Rayleigh number of sub-ice water
    const gMars = 3.72; // m/s^2
    const alpha = 2.0e-4; // 1/K
    const nu = 1.0e-6; // m^2/s
    const kappa = 1.4e-7; // m^2/s
    const deltaTWater = 4.0; // 4 K temperature gradient to 4 C bottom water
    const Ra = (gMars * alpha * deltaTWater * Math.pow(HwaterM, 3.0)) / (nu * kappa);

    let regime = 'Vigorously Convecting Perennial Sub-Ice Lake (Liquid Habitability)';
    if (HwaterM <= 5.0) {
      regime = 'Nearly Frozen Basal Cryolake Horizon';
    } else if (TsurfK >= 260.0) {
      regime = 'Seasonally Ice-Covered High-Latitude Lacustrine System';
    }

    return {
      conductiveIceHeatFluxWM2: parseFloat(qCondWM2.toFixed(3)),
      equilibriumIceLidThicknessM: parseFloat(hIceEqM.toFixed(1)),
      subIceLiquidWaterDepthM: parseFloat(HwaterM.toFixed(1)),
      rayleighNumber: parseFloat(Ra.toExponential(3)),
      bottomWaterTempC: 4.0,
      lakeThermalRegime: regime,
      paleolakeContext: `Ice-Covered Paleolake (${HiceM.toFixed(0)}m Ice Lid, ${HwaterM.toFixed(0)}m Sub-Ice Water at 4.0 C, q_cond=${qCondWM2.toFixed(2)} W/m2, ${regime})`
    };
  }

  /**
   * Calculate subsurface cryovolcanic brine chamber freezing expansion, tensile rupture overpressure, and cryolava vent exit velocity.
   * P_lith = rho_crust * g * z
   * Delta_P_over = (4/3) * mu_crust * ( Delta_V / V ) * X_ice
   * v_exit = sqrt( 2 * Delta_P_drive / rho_brine )
   * Reference: Fagents (2003), Quick et al. (2019), Bowling et al. (2019) for Martian & Cerian Cryovolcanic Conduit Mechanics.
   * @param {number} [brineChamberDepthKm=5.0] - Cryomagma chamber depth in km (1.0 to 30.0 km)
   * @param {number} [chamberRadiusM=2000.0] - Spherical chamber radius in meters (100 to 10000 m)
   * @param {number} [frozenFraction=0.30] - Crystallized ice fraction in chamber (0.05 to 0.90)
   * @param {number} [crustShearModulusGPa=3.5] - Host crust shear modulus in GPa (1.0 to 15.0 GPa)
   * @returns {{lithostaticPressureMPa: number, chamberOverpressureMPa: number, totalChamberPressureMPa: number, isTensileFractureInitiated: boolean, cryolavaVentExitVelocityMs: number, eruptionMechanismClass: string, cryovolcanismContext: string}}
   */
  static computeMartianCryovolcanicEruptionOverpressure(brineChamberDepthKm = 5.0, chamberRadiusM = 2000.0, frozenFraction = 0.30, crustShearModulusGPa = 3.5) {
    const zKm = Math.max(0.5, brineChamberDepthKm);
    const zM = zKm * 1000.0;
    const RchM = Math.max(50.0, chamberRadiusM);
    const Xice = Math.max(0.01, Math.min(0.95, frozenFraction));
    const muGPa = Math.max(0.5, crustShearModulusGPa);
    const muPa = muGPa * 1.0e9;

    const gMars = 3.72; // m/s^2
    const rhoCrust = 2500.0; // kg/m^3
    const rhoBrine = 1150.0; // kg/m^3
    const sigmaTensile = 10.0e6; // 10 MPa rock tensile strength
    const deltaVFrac = 0.09; // +9% water-ice volume expansion

    // Lithostatic pressure (Pa & MPa)
    const PlithPa = rhoCrust * gMars * zM;
    const PlithMPa = PlithPa / 1.0e6;

    // Overpressure from crystallization expansion (Pa & MPa)
    const PoverPa = (4.0 / 3.0) * muPa * deltaVFrac * Xice;
    const PoverMPa = PoverPa / 1.0e6;

    const PtotMPa = PlithMPa + PoverMPa;
    const isFracture = PoverPa >= sigmaTensile;

    // Driving pressure for ascent and vent eruption (Pa)
    const PdrivePa = Math.max(0.0, PoverPa - sigmaTensile);
    const vExitMs = isFracture ? Math.sqrt((2.0 * PdrivePa) / rhoBrine) : 0.0;

    let mechClass = 'Explosive Sub-Surface Cryovolcanic Venting (Vigorous Effusion)';
    if (!isFracture) {
      mechClass = 'Confined Cryomagma Intrusion (Sub-Tensile Elastic Storage)';
    } else if (vExitMs >= 500.0) {
      mechClass = 'Supersonic Cryomagma Plume Eruption (Ballistic Dome Building)';
    }

    return {
      lithostaticPressureMPa: parseFloat(PlithMPa.toFixed(2)),
      chamberOverpressureMPa: parseFloat(PoverMPa.toFixed(2)),
      totalChamberPressureMPa: parseFloat(PtotMPa.toFixed(2)),
      isTensileFractureInitiated: isFracture,
      cryolavaVentExitVelocityMs: parseFloat(vExitMs.toFixed(1)),
      eruptionMechanismClass: mechClass,
      cryovolcanismContext: `Cryovolcanic Chamber at ${zKm.toFixed(1)}km (${PoverMPa.toFixed(0)} MPa Overpressure, ${(Xice * 100).toFixed(0)}% Frozen, ${vExitMs.toFixed(0)} m/s Vent Speed, ${mechClass})`
    };
  }

  /**
   * Calculate subsurface perchlorate/halogen brine thermodynamic eutectic freezing, liquidus concentration, unfrozen liquid brine fraction, and water activity.
   * C_liq(T) = C_eut * ( ( 273.15 - T ) / ( 273.15 - T_eut ) )^0.85
   * w_liq = C_0 / C_liq(T)
   * a_w = exp( - ( 273.15 - T ) / 103.3 )
   * Reference: Chevrier et al. (2009), Toner & Catling (2016), Rivera-Valentin et al. (2020) for Martian Perchlorate Cryogenic Brines.
   * @param {number} [ambientTemperatureK=225.0] - Subsurface regolith temperature in Kelvin (160 to 280 K)
   * @param {number} [perchlorateWeightPercent=10.0] - Initial bulk salt concentration in wt% (0.5 to 50.0 wt%)
   * @param {string} [saltCationType='Mg(ClO4)2'] - Cation species: 'Mg(ClO4)2', 'Ca(ClO4)2', 'NaClO4', 'NaCl'
   * @returns {{saltSpecies: string, eutecticTemperatureK: number, eutecticTemperatureC: number, isLiquidBrineThermodynamicallyStable: boolean, liquidusSaltConcentrationWtPct: number, equilibriumLiquidBrineFractionPercent: number, waterActivityAw: number, habitabilityStatus: string, brineEquilibriumContext: string}}
   */
  static computeMartianSubsurfacePerchlorateEutecticEquilibrium(ambientTemperatureK = 225.0, perchlorateWeightPercent = 10.0, saltCationType = 'Mg(ClO4)2') {
    const TambK = Math.max(140.0, Math.min(300.0, ambientTemperatureK));
    const C0 = Math.max(0.1, Math.min(50.0, perchlorateWeightPercent));

    let TeutK = 204.65; // Mg(ClO4)2 eutectic (-68.5 C)
    let Ceut = 44.0;
    let name = 'Magnesium Perchlorate (Mg(ClO4)2)';

    const st = String(saltCationType || '').toLowerCase();
    if (st.includes('ca')) {
      TeutK = 198.55; // Ca(ClO4)2 (-74.6 C)
      Ceut = 52.0;
      name = 'Calcium Perchlorate (Ca(ClO4)2)';
    } else if (st.includes('na') && st.includes('cl') && !st.includes('clo4')) {
      TeutK = 252.05; // NaCl (-21.1 C)
      Ceut = 23.3;
      name = 'Sodium Chloride (NaCl)';
    } else if (st.includes('na')) {
      TeutK = 239.15; // NaClO4 (-34.0 C)
      Ceut = 52.0;
      name = 'Sodium Perchlorate (NaClO4)';
    }

    const TeutC = TeutK - 273.15;
    const isLiquid = TambK >= TeutK;

    let Cliq = Ceut;
    let wLiqPct = 0.0;
    let aw = 0.0;
    let habit = 'Sub-Eutectic Completely Frozen Solid Ice + Salt Hydrate';

    if (isLiquid) {
      const deltaT = Math.max(0.1, 273.15 - TambK);
      const deltaTeut = 273.15 - TeutK;
      Cliq = Math.min(Ceut, Ceut * Math.pow(deltaT / deltaTeut, 0.85));
      Cliq = Math.max(C0, Cliq);

      const wLiqFrac = Math.min(1.0, C0 / Cliq);
      wLiqPct = wLiqFrac * 100.0;

      // Water activity
      aw = Math.exp(-deltaT / 103.3);
      aw = Math.max(0.20, Math.min(1.0, aw));

      if (aw >= 0.605) {
        habit = 'Metabolically Permissive Liquid Brine (aw >= 0.605, Terrestrial Halophile Candidate)';
      } else {
        habit = 'Hypersaline Cryogenic Liquid Brine (aw < 0.605, Severe Osmotic Water-Activity Stress)';
      }
    }

    return {
      saltSpecies: name,
      eutecticTemperatureK: parseFloat(TeutK.toFixed(2)),
      eutecticTemperatureC: parseFloat(TeutC.toFixed(1)),
      isLiquidBrineThermodynamicallyStable: isLiquid,
      liquidusSaltConcentrationWtPct: parseFloat(Cliq.toFixed(2)),
      equilibriumLiquidBrineFractionPercent: parseFloat(wLiqPct.toFixed(1)),
      waterActivityAw: parseFloat(aw.toFixed(3)),
      habitabilityStatus: habit,
      brineEquilibriumContext: `${name} at ${TambK.toFixed(1)}K (${isLiquid ? 'Liquid Stable' : 'Frozen Solid'}, ${wLiqPct.toFixed(1)}% Liquid Brine, aw=${aw.toFixed(3)}, ${habit})`
    };
  }

  /**
   * Calculate deep crustal aquifer geothermal hydrothermal plume upwelling, buoyancy driving head, Darcy discharge flux, and surface spring temperature.
   * T_deep = T_surf + Gamma_geo * z_aq
   * Delta_rho = rho_0 * beta * ( T_deep - T_freeze )
   * q_Darcy = k_fault * Delta_rho * g / mu_water
   * Reference: Clifford (1993), Andrews-Hanna et al. (2007), Harrison & Grimm (2008) for Martian Deep Hydrothermal Groundwater Circulation.
   * @param {number} [aquiferDepthKm=6.0] - Deep confined aquifer depth in km (1.0 to 15.0 km)
   * @param {number} [regionalGeothermalGradientKPerKm=20.0] - Geothermal gradient in K/km (10 to 60 K/km)
   * @param {number} [faultPermeabilityM2=1.0e-11] - Fault damage zone permeability in m^2 (1e-14 to 1e-10 m^2)
   * @param {number} [faultConduitWidthM=50.0] - Fracture zone width in meters (5 to 500 m)
   * @param {number} [faultStrikeLengthM=1000.0] - Along-strike fault length in meters (100 to 20000 m)
   * @returns {{deepAquiferTemperatureC: number, deepAquiferTemperatureK: number, buoyancyDensityDeficitKgM3: number, darcyUpwellingVelocityMPerDay: number, dailySpringDischargeM3Day: number, exitSpringTemperatureC: number, hydrothermalSpringClass: string, hydrothermalPlumeContext: string}}
   */
  static computeMartianDeepAquiferHydrothermalPlumeUpwelling(aquiferDepthKm = 6.0, regionalGeothermalGradientKPerKm = 20.0, faultPermeabilityM2 = 1.0e-11, faultConduitWidthM = 50.0, faultStrikeLengthM = 1000.0) {
    const zKm = Math.max(0.5, aquiferDepthKm);
    const gammaGeo = Math.max(5.0, regionalGeothermalGradientKPerKm);
    const kFault = Math.max(1e-16, faultPermeabilityM2);
    const Wfault = Math.max(1.0, faultConduitWidthM);
    const Lfault = Math.max(10.0, faultStrikeLengthM);

    const TsurfK = 215.0; // Mean surface temperature (-58.15 C)
    const TfreezeK = 273.15;
    const gMars = 3.72; // m/s^2
    const rho0 = 1000.0; // kg/m^3
    const betaExp = 3.0e-4; // 1/K
    const muWater = 4.7e-4; // Pa*s at ~60 C

    // Deep reservoir temperature (K and C)
    const TdeepK = TsurfK + (gammaGeo * zKm);
    const TdeepC = TdeepK - 273.15;

    // Buoyant density deficit (kg/m^3)
    const deltaTBuoy = Math.max(0.0, TdeepK - TfreezeK);
    const deltaRho = rho0 * betaExp * deltaTBuoy;

    // Darcy upwelling velocity (m/s & m/day)
    const qDarcyMs = (kFault * deltaRho * gMars) / muWater;
    const qDarcyMDay = qDarcyMs * 86400.0;

    // Volumetric discharge rate (m^3/day)
    const Afault = Wfault * Lfault;
    const QdayM3 = qDarcyMs * Afault * 86400.0;

    // Exit spring temperature at surface (conductive maturation factor 0.75)
    const etaTherm = 0.75;
    const TspringC = (TsurfK - 273.15) + (etaTherm * (TdeepK - TsurfK));

    let springClass = 'Warm Hydrothermal Fault Spring (Oasis Candidate)';
    if (TspringC >= 45.0) {
      springClass = 'High-Temperature Geothermal Spring / Hydrothermal Mound Source';
    } else if (TspringC < 10.0) {
      springClass = 'Low-Temperature Chilled Subsurface Groundwater Seep';
    }

    return {
      deepAquiferTemperatureC: parseFloat(TdeepC.toFixed(1)),
      deepAquiferTemperatureK: parseFloat(TdeepK.toFixed(1)),
      buoyancyDensityDeficitKgM3: parseFloat(deltaRho.toFixed(3)),
      darcyUpwellingVelocityMPerDay: parseFloat(qDarcyMDay.toFixed(4)),
      dailySpringDischargeM3Day: parseFloat(QdayM3.toFixed(1)),
      exitSpringTemperatureC: parseFloat(TspringC.toFixed(1)),
      hydrothermalSpringClass: springClass,
      hydrothermalPlumeContext: `Deep Hydrothermal Plume from ${zKm.toFixed(1)}km (${TdeepC.toFixed(0)} C Aquifer, ${TspringC.toFixed(1)} C Exit Spring, ${QdayM3.toFixed(0)} m3/d Discharge, ${springClass})`
    };
  }

  /**
   * Calculate primordial Martian Magma Ocean (MMO) solidification timescale, cumulate density stratification, and Rayleigh-Taylor mantle overturn dynamics.
   * t_solid = rho_magma * ( L_cryst + C_p * Delta_T ) * H_mo / F_cool
   * tau_overturn = 4 * pi * eta_mantle / ( Delta_rho * g * lambda )
   * Reference: Elkins-Tanton et al. (2005), Scheinberg et al. (2014), Breuer et al. (2010) for Early Mars Magma Ocean Solidification.
   * @param {number} [magmaOceanDepthKm=1500.0] - Initial basal magma ocean depth in km (500 to 2000 km)
   * @param {number} [surfaceCoolingHeatFluxWM2=100.0] - Radiative/atmospheric cooling flux in W/m^2 (20 to 1000 W/m^2)
   * @param {number} [mantleOverturnDensityContrastKgM3=200.0] - Density inversion contrast in kg/m^3 (50 to 500 kg/m^3)
   * @param {number} [mantleDynamicViscosityPaS=1.0e20] - Hot cumulate mantle viscosity in Pa*s (1e18 to 1e22 Pa*s)
   * @returns {{solidificationTimescaleMyr: number, mantleOverturnTimescaleKyr: number, totalCrystallizedVolumeKm3: number, cumulateStratigraphyClass: string, coreDynamoInitiationContext: string}}
   */
  static computeMartianMagmaOceanCrystallizationAndOverturn(magmaOceanDepthKm = 1500.0, surfaceCoolingHeatFluxWM2 = 100.0, mantleOverturnDensityContrastKgM3 = 200.0, mantleDynamicViscosityPaS = 1.0e20) {
    const HmoKm = Math.max(100.0, magmaOceanDepthKm);
    const HmoM = HmoKm * 1000.0;
    const FcoolWM2 = Math.max(1.0, surfaceCoolingHeatFluxWM2);
    const deltaRho = Math.max(10.0, mantleOverturnDensityContrastKgM3);
    const etaMantle = Math.max(1e17, mantleDynamicViscosityPaS);

    const rhoMagma = 3400.0; // kg/m^3
    const Lcryst = 4.0e5; // J/kg
    const Cp = 1200.0; // J/(kg*K)
    const deltaT = 400.0; // K
    const gMars = 3.72; // m/s^2
    const rMarsM = 3389.5e3;

    // Solidification timescale (Myr)
    const Qvol = rhoMagma * (Lcryst + (Cp * deltaT)); // J/m^3
    const tSolidSec = (Qvol * HmoM) / FcoolWM2;
    const tSolidMyr = tSolidSec / (3.15576e7 * 1.0e6);

    // Total crystallized volume (km^3)
    const rBottomM = rMarsM - HmoM;
    const VmoM3 = (4.0 / 3.0) * Math.PI * (Math.pow(rMarsM, 3.0) - Math.pow(Math.max(0.0, rBottomM), 3.0));
    const VmoKm3 = VmoM3 / 1.0e9;

    // Rayleigh-Taylor overturn timescale (kyr)
    const lambdaM = HmoM;
    const tauOverturnSec = (4.0 * Math.PI * etaMantle) / (deltaRho * gMars * lambdaM);
    const tauOverturnKyr = tauOverturnSec / (3.15576e7 * 1000.0);

    let stratClass = 'Layered Cumulate Mantle (Basal Dunite -> Pyroxenite -> Dense Ilmenite Crustal Lid)';
    if (HmoKm >= 1800.0) {
      stratClass = 'Whole-Mantle Deep Magma Ocean Cumulate Column with Majorite Garnet Base';
    }

    return {
      solidificationTimescaleMyr: parseFloat(tSolidMyr.toFixed(2)),
      mantleOverturnTimescaleKyr: parseFloat(tauOverturnKyr.toFixed(1)),
      totalCrystallizedVolumeKm3: parseFloat(VmoKm3.toFixed(1)),
      cumulateStratigraphyClass: stratClass,
      coreDynamoInitiationContext: `MMO Crystallization (${tSolidMyr.toFixed(1)} Myr Solidification, ${tauOverturnKyr.toFixed(0)} kyr Overturn, launched Early Martian Dynamo)`
    };
  }

  /**
   * Calculate volcanic acid-fog condensation, reactive diffusion, and basaltic boulder weathering rind thickness growth over geological time.
   * x(t) = sqrt( x_0^2 + ( 2 * D_eff * C_acid * t ) / N_rxn )
   * Reference: Tosca et al. (2004), Hurowitz et al. (2006), Settle (1979) for Gusev Crater Adirondack Weathering Rinds.
   * @param {number} [atmosphericH2SO4VaporPpm=50.0] - Atmospheric acid-fog / SO2 concentration in ppm (1 to 1000 ppm)
   * @param {number} [exposureDurationKyr=100.0] - Surface exposure duration in kyr (1 to 5000 kyr)
   * @param {number} [rockPorosity=0.10] - Basaltic boulder pore fraction (0.01 to 0.40)
   * @param {number} [acidDiffusivityM2S=5.0e-14] - Effective acid diffusion coefficient in m^2/s (1e-15 to 1e-12 m^2/s)
   * @returns {{finalRindThicknessMm: number, instantaneousGrowthRateMmPerKyr: number, cationDepletedVolumeCm3PerM2: number, alterationCrustClass: string, acidFogContext: string}}
   */
  static computeMartianAcidFogWeatheringRindGrowth(atmosphericH2SO4VaporPpm = 50.0, exposureDurationKyr = 100.0, rockPorosity = 0.10, acidDiffusivityM2S = 5.0e-14) {
    const CatmPpm = Math.max(0.1, atmosphericH2SO4VaporPpm);
    const tKyr = Math.max(0.1, exposureDurationKyr);
    const phi = Math.max(0.01, Math.min(0.50, rockPorosity));
    const DeffM2S = Math.max(1e-16, acidDiffusivityM2S);

    const x0M = 5.0e-5; // 0.05 mm initial roughness rind
    const rhoRock = 2800.0; // kg/m^3
    const Nrxn = 0.25 * rhoRock * (1.0 - phi); // kg/m^3 stoichiometric reaction capacity
    const CacidKgM3 = CatmPpm / 1000.0; // Normalized reactive acid loading

    const DeffM2Yr = DeffM2S * 3.15576e7;
    const tYr = tKyr * 1000.0;

    // Rind thickness (m & mm)
    const xRindSq = Math.pow(x0M, 2.0) + ((2.0 * DeffM2Yr * CacidKgM3 * tYr) / Nrxn);
    const xRindM = Math.sqrt(xRindSq);
    const xRindMm = xRindM * 1000.0;

    // Growth rate (mm/kyr)
    const xDotMYr = (DeffM2Yr * CacidKgM3) / (xRindM * Nrxn);
    const xDotMmKyr = xDotMYr * 1000.0 * 1000.0;

    // Volume of depleted cation rind per m^2 surface area (cm^3/m^2)
    const VdepletedCm3M2 = xRindM * 1.0e6;

    let rindClass = 'Thin Millimeter-Scale Acid-Weathered Basaltic Rind (Gusev Adirondack Type)';
    if (xRindMm >= 10.0) {
      rindClass = 'Extensively Pervasive Acid-Leached Outer Crust with Sulfate Matrix';
    } else if (xRindMm < 1.0) {
      rindClass = 'Sub-Millimeter Incipient Acid Frost Alteration Layer';
    }

    return {
      finalRindThicknessMm: parseFloat(xRindMm.toFixed(2)),
      instantaneousGrowthRateMmPerKyr: parseFloat(xDotMmKyr.toFixed(3)),
      cationDepletedVolumeCm3PerM2: parseFloat(VdepletedCm3M2.toFixed(1)),
      alterationCrustClass: rindClass,
      acidFogContext: `Acid-Fog Alteration (${xRindMm.toFixed(2)} mm Rind in ${tKyr.toFixed(0)} kyr, ${xDotMmKyr.toFixed(3)} mm/kyr Growth, ${rindClass})`
    };
  }

  /**
   * Calculate burial diagenesis, Arrhenius reaction kinetics, and structural dewatering overpressure during the Smectite-to-Illite clay mineral transition in ancient Noachian stratigraphy.
   * T_burial = T_surf + Gamma_geo * z
   * k = A * exp( -E_a / ( R * T ) )
   * f_illite = 1 - exp( -k * [K+] * t )
   * Reference: Cuadros (2006), Bethke & Altaner (1986), Ehlmann et al. (2011) for Deep Martian Clay Diagenesis.
   * @param {number} [burialDepthKm=4.0] - Crustal burial depth in km (0.5 to 12.0 km)
   * @param {number} [regionalGeothermalGradientKPerKm=25.0] - Geothermal gradient in K/km (10 to 60 K/km)
   * @param {number} [burialDurationMyr=20.0] - Geological heating duration in Myr (1 to 200 Myr)
   * @param {number} [potassiumConcentrationPpm=200.0] - Pore fluid K+ activity in ppm (10 to 2000 ppm)
   * @returns {{burialTemperatureC: number, burialTemperatureK: number, illitePercentInClay: number, releasedStructuralWaterWtPct: number, dewateringOverpressureMPa: number, clayDiageneticZoneClass: string, diagenesisContext: string}}
   */
  static computeMartianSmectiteToIlliteTransitionKinetics(burialDepthKm = 4.0, regionalGeothermalGradientKPerKm = 25.0, burialDurationMyr = 20.0, potassiumConcentrationPpm = 200.0) {
    const zKm = Math.max(0.2, burialDepthKm);
    const gammaGeo = Math.max(5.0, regionalGeothermalGradientKPerKm);
    const tMyr = Math.max(0.1, burialDurationMyr);
    const Kppm = Math.max(5.0, potassiumConcentrationPpm);

    const TsurfK = 215.0; // Mean Martian surface temp
    const Rgas = 8.314; // J/(mol*K)
    const Ea = 1.15e5; // 115 kJ/mol activation energy
    const AfreqYr = 1.57788e12; // 1/yr pre-exponential factor

    // Burial temperature
    const TburialK = TsurfK + (gammaGeo * zKm);
    const TburialC = TburialK - 273.15;

    // Arrhenius rate constant (1/yr)
    const kYr = AfreqYr * Math.exp(-Ea / (Rgas * TburialK));

    // Illite fraction in mixed-layer I/S
    const Kfactor = Kppm / 100.0;
    const tYr = tMyr * 1.0e6;
    const exponent = Math.min(50.0, kYr * Kfactor * tYr);
    const fIllite = 1.0 - Math.exp(-exponent);
    const illitePct = fIllite * 100.0;

    // Dewatering: 15 wt% smectite down to 4.5 wt% illite
    const waterReleasedWtPct = fIllite * (15.0 - 4.5);

    // Dewatering fluid overpressure (MPa)
    const overpressureMPa = (waterReleasedWtPct / 10.5) * 45.0;

    let zoneClass = 'Expandable Smectite Dominant (Diagenetically Immature Nontronite/Saponite)';
    if (illitePct >= 80.0) {
      zoneClass = 'High-Grade Diagenetic Illite / Mica (Structural Water Expelled, Hydrofractured)';
    } else if (illitePct >= 30.0) {
      zoneClass = 'Mixed-Layer Illite-Smectite (I/S Transition Zone, Intermediate Dewatering)';
    }

    return {
      burialTemperatureC: parseFloat(TburialC.toFixed(1)),
      burialTemperatureK: parseFloat(TburialK.toFixed(1)),
      illitePercentInClay: parseFloat(illitePct.toFixed(1)),
      releasedStructuralWaterWtPct: parseFloat(waterReleasedWtPct.toFixed(2)),
      dewateringOverpressureMPa: parseFloat(overpressureMPa.toFixed(1)),
      clayDiageneticZoneClass: zoneClass,
      diagenesisContext: `Clay Diagenesis at ${zKm.toFixed(1)}km (${TburialC.toFixed(0)} C, ${illitePct.toFixed(0)}% Illite, ${waterReleasedWtPct.toFixed(1)} wt% H2O Dewatered, ${zoneClass})`
    };
  }

  /**
   * Calculate hydrothermal silica sinter maturation, dissolution-reprecipitation Ostwald ripening, porosity reduction, and thermal inertia evolution.
   * k_sinter = A * exp( -E_a / ( R * T ) ) * 10^( pH - 7 )
   * phi(t) = phi_inf + ( phi_0 - phi_inf ) * exp( -k_sinter * t )
   * I = sqrt( k_therm * rho_bulk * C )
   * Reference: Lynne et al. (2005), Ruff et al. (2011), Rice et al. (2013) for Silica Sinter Compaction & Thermal Inertia.
   * @param {number} [ambientTemperatureC=75.0] - Hydrothermal vent / fluid temperature in C (10 to 250 C)
   * @param {number} [initialPorosity=0.55] - Fresh spicular sinter porosity (0.20 to 0.85)
   * @param {number} [durationYears=100.0] - Geothermal activity duration in years (1 to 100000 yr)
   * @param {number} [pHLevel=8.0] - Fluid pH level (2.0 to 11.0)
   * @returns {{finalPorosityPercent: number, bulkDensityKgM3: number, thermalConductivityWMK: number, thermalInertiaTIU: number, sinterMaturationStageClass: string, silicaSinterContext: string}}
   */
  static computeMartianHydrothermalSilicaSinteringKinetics(ambientTemperatureC = 75.0, initialPorosity = 0.55, durationYears = 100.0, pHLevel = 8.0) {
    const TfluidC = Math.max(5.0, ambientTemperatureC);
    const TfluidK = TfluidC + 273.15;
    const phi0 = Math.max(0.10, Math.min(0.90, initialPorosity));
    const tYr = Math.max(0.1, durationYears);
    const pH = Math.max(1.0, Math.min(13.0, pHLevel));

    const Rgas = 8.314;
    const Ea = 6.5e4; // 65 kJ/mol
    const A = 1.0e7; // 1/yr
    const phiInf = 0.05; // Irreducible residual micro-porosity
    const rhoMatrix = 2200.0; // kg/m^3 (amorphous opal)
    const kMatrix = 1.50; // W/(m*K)
    const Cspec = 800.0; // J/(kg*K)

    // Dissolution/reprecipitation rate constant (1/yr)
    const pHFactor = Math.pow(10.0, Math.min(3.0, Math.max(-3.0, pH - 7.0)));
    const kSinterYr = A * Math.exp(-Ea / (Rgas * TfluidK)) * pHFactor;

    // Porosity evolution
    const phiFinal = phiInf + ((phi0 - phiInf) * Math.exp(-kSinterYr * tYr));
    const phiPct = phiFinal * 100.0;

    // Bulk density (kg/m^3)
    const rhoBulk = rhoMatrix * (1.0 - phiFinal);

    // Thermal conductivity (W/(m*K))
    const kTherm = kMatrix * Math.pow(1.0 - phiFinal, 2.5);

    // Thermal Inertia (J m^-2 K^-1 s^-1/2 = tiu)
    const TIU = Math.sqrt(kTherm * rhoBulk * Cspec);

    let stageClass = 'Porous Spicular Opal-A Sinter (Frutexites / Geyser Mound)';
    if (phiPct <= 15.0) {
      stageClass = 'Dense Recrystallized Chalcedonic Chert / Massive Quartzite';
    } else if (phiPct <= 35.0) {
      stageClass = 'Compacted Vitreous Opal-A / Opal-CT Sinter Terrace';
    }

    return {
      finalPorosityPercent: parseFloat(phiPct.toFixed(1)),
      bulkDensityKgM3: parseFloat(rhoBulk.toFixed(1)),
      thermalConductivityWMK: parseFloat(kTherm.toFixed(3)),
      thermalInertiaTIU: parseFloat(TIU.toFixed(1)),
      sinterMaturationStageClass: stageClass,
      silicaSinterContext: `Silica Sintering at ${TfluidC.toFixed(0)} C (${phiPct.toFixed(1)}% Porosity, TIU=${TIU.toFixed(0)}, ${stageClass})`
    };
  }

  /**
   * Calculate geothermal hydrothermal serpentinization reaction kinetics, abiotic molecular hydrogen (H2) generation, and Fischer-Tropsch-Type (FTT) methane (CH4) outgassing flux.
   * T_crust = T_surf + Gamma_geo * z
   * eta_T = exp( - ( T - T_opt )^2 / ( 2 * sigma_T^2 ) )
   * M_H2 = M_ol_reacted * yield_H2_per_kg
   * M_CH4 = 0.15 * ( 16 / 8 ) * M_H2
   * Reference: Martin & Fyfe (1970), McCollom & Seewald (2007), Oze & Sharma (2005), Klein et al. (2013) for Martian Abiotic Methane Generation.
   * @param {number} [crustalDepthKm=5.0] - Serpentinizing aquifer depth in km (1.0 to 15.0 km)
   * @param {number} [geothermalGradientKPerKm=50.0] - Local geothermal gradient in K/km (10 to 80 K/km)
   * @param {number} [olivineMassFraction=0.60] - Ultramafic rock olivine fraction (0.10 to 0.95)
   * @param {number} [rockVolumeM3=1.0e6] - Reaction reservoir rock volume in m^3 (1e3 to 1e9 m^3)
   * @param {number} [hydrothermalDurationYr=5000.0] - Hydrothermal circulation duration in yr (100 to 100000 yr)
   * @returns {{crustalTemperatureC: number, reactedOlivineTons: number, hydrogenProducedTons: number, methaneProducedTons: number, reactionEfficiencyPercent: number, serpentinizationRegimeClass: string, serpentinizationContext: string}}
   */
  static computeMartianSerpentinizationHydrogenMethaneProduction(crustalDepthKm = 5.0, geothermalGradientKPerKm = 50.0, olivineMassFraction = 0.60, rockVolumeM3 = 1.0e6, hydrothermalDurationYr = 5000.0) {
    const zKm = Math.max(0.5, crustalDepthKm);
    const gammaGeo = Math.max(5.0, geothermalGradientKPerKm);
    const wOl = Math.max(0.05, Math.min(1.0, olivineMassFraction));
    const VrockM3 = Math.max(10.0, rockVolumeM3);
    const tYr = Math.max(1.0, hydrothermalDurationYr);

    const TsurfK = 215.0; // Surface mean
    const rhoRock = 3000.0; // kg/m^3
    const TcrustK = TsurfK + (gammaGeo * zKm);
    const TcrustC = TcrustK - 273.15;

    // Thermal kinetic efficiency (peak at 275 C = 548.15 K, sigma = 60 K)
    const ToptK = 548.15;
    const sigmaT = 60.0;
    const deltaT = TcrustK - ToptK;
    const etaT = Math.exp(-Math.pow(deltaT, 2.0) / (2.0 * Math.pow(sigmaT, 2.0)));

    // Reaction rate constant (1/yr)
    const kPeak = 5.0e-4; // 1/yr at optimal T
    const kEff = kPeak * etaT;

    // Hydration fraction
    const fRxn = 1.0 - Math.exp(-kEff * tYr);

    // Reacted olivine mass (tons)
    const MolTotKg = VrockM3 * rhoRock * wOl;
    const MolRxnKg = MolTotKg * fRxn;
    const MolRxnTons = MolRxnKg / 1000.0;

    // Hydrogen production: ~0.7056 g H2 per kg olivine (Fo85Fa15)
    const H2YieldKgPerKg = 0.7056e-3;
    const MH2Kg = MolRxnKg * H2YieldKgPerKg;
    const MH2Tons = MH2Kg / 1000.0;

    // FTT Methane conversion (~15% catalytic reduction)
    const etaFTT = 0.15;
    const MCH4Kg = (16.042 / (4.0 * 2.016)) * etaFTT * MH2Kg;
    const MCH4Tons = MCH4Kg / 1000.0;

    let regimeClass = 'Active Hydrothermal Serpentinization Zone (High Abiotic H2 + CH4 Generation)';
    if (TcrustC < 100.0) {
      regimeClass = 'Kinetically Sluggish Low-Temperature Serpentinization';
    } else if (TcrustC > 400.0) {
      regimeClass = 'Super-Critical Dehydrated Mantle (Above Serpentine Stability Limit)';
    }

    return {
      crustalTemperatureC: parseFloat(TcrustC.toFixed(1)),
      reactedOlivineTons: parseFloat(MolRxnTons.toFixed(1)),
      hydrogenProducedTons: parseFloat(MH2Tons.toFixed(2)),
      methaneProducedTons: parseFloat(MCH4Tons.toFixed(2)),
      reactionEfficiencyPercent: parseFloat((fRxn * 100.0).toFixed(1)),
      serpentinizationRegimeClass: regimeClass,
      serpentinizationContext: `Serpentinization at ${zKm.toFixed(1)}km (${TcrustC.toFixed(0)} C, ${MH2Tons.toFixed(1)} t H2, ${MCH4Tons.toFixed(1)} t CH4 Abiotic Outgassing, ${regimeClass})`
    };
  }

  /**
   * Calculate hydrothermal mineral vein precipitation, fracture aperture narrowing, cubic-law permeability decay, and crack-seal timescale.
   * v_growth = k_0 * exp( -E_a / ( R * T ) ) * ( Omega - 1 )^2
   * w(t) = w_0 - 2 * v_growth * t
   * k / k_0 = ( w(t) / w_0 )^3
   * Reference: Rimstidt & Barnes (1980), Lowell et al. (1993), Schwenzer & Kring (2009) for Hydrothermal Vein Sealing.
   * @param {number} [initialApertureMm=5.0] - Initial fracture opening in mm (0.5 to 50.0 mm)
   * @param {number} [silicaSupersaturationRatio=3.5] - Fluid saturation ratio Omega (1.1 to 10.0)
   * @param {number} [hydrothermalTemperatureC=150.0] - Vein fluid temperature in C (50 to 350 C)
   * @param {number} [elapsedDurationYr=25.0] - Hydrothermal active precipitation time in yr (0.1 to 1000 yr)
   * @returns {{finalApertureMm: number, inwardGrowthVelocityMmPerYr: number, completeSealingTimescaleYr: number, residualPermeabilityPercent: number, veinPrecipitationStageClass: string, veinSealingContext: string}}
   */
  static computeMartianHydrothermalVeinCloggingKinetics(initialApertureMm = 5.0, silicaSupersaturationRatio = 3.5, hydrothermalTemperatureC = 150.0, elapsedDurationYr = 25.0) {
    const w0Mm = Math.max(0.1, initialApertureMm);
    const Omega = Math.max(1.05, silicaSupersaturationRatio);
    const TfluidC = Math.max(10.0, hydrothermalTemperatureC);
    const tYr = Math.max(0.01, elapsedDurationYr);

    const TfluidK = TfluidC + 273.15;
    const Rgas = 8.314;
    const Ea = 7.5e4; // 75 kJ/mol
    const k0 = 1.20e4; // m/yr

    // Inward crystal growth velocity (m/yr and mm/yr)
    const vGrowthMYr = k0 * Math.exp(-Ea / (Rgas * TfluidK)) * Math.pow(Omega - 1.0, 2.0);
    const vGrowthMmYr = vGrowthMYr * 1000.0;

    // Sealing timescale (yr)
    const tSealYr = w0Mm / (2.0 * Math.max(1e-6, vGrowthMmYr));

    // Remaining aperture (mm)
    const wFinalMm = Math.max(0.0, w0Mm - (2.0 * vGrowthMmYr * tYr));

    // Residual permeability fraction (Cubic Law)
    const kRatio = Math.pow(wFinalMm / w0Mm, 3.0);
    const kRatioPct = kRatio * 100.0;

    let stageClass = 'Partially Occluded Conduit (Active Hydrothermal Fracture)';
    if (wFinalMm === 0.0 || kRatioPct < 1.0) {
      stageClass = 'Completely Sealed Mineral Vein (Fibrous Crack-Seal Microtexture)';
    } else if (kRatioPct >= 75.0) {
      stageClass = 'Open Highly Permeable Hydrothermal Flow Channel';
    }

    return {
      finalApertureMm: parseFloat(wFinalMm.toFixed(2)),
      inwardGrowthVelocityMmPerYr: parseFloat(vGrowthMmYr.toFixed(4)),
      completeSealingTimescaleYr: parseFloat(tSealYr.toFixed(1)),
      residualPermeabilityPercent: parseFloat(kRatioPct.toFixed(1)),
      veinPrecipitationStageClass: stageClass,
      veinSealingContext: `Vein Clogging at ${TfluidC.toFixed(0)} C (${wFinalMm.toFixed(2)} mm Aperture, ${tSealYr.toFixed(0)} yr Sealing, ${kRatioPct.toFixed(1)}% Permeability, ${stageClass})`
    };
  }

  /**
   * Calculate progressive evaporative concentration of acidic Martian paleolakes, geochemical saturation indices, fractional sulfate crystallization sequence, and lake desiccation lifespan.
   * CF = h_0 / ( h_0 - E_evap * t )
   * Sequence: Gypsum (CF > 2) -> Jarosite/Alunite (CF > 4.5) -> Mg-Sulfates (CF > 12) -> Halite (CF > 30)
   * Reference: Tosca et al. (2005), McLennan et al. (2005), Squyres et al. (2004) for Meridiani Planum Acid Lake Chemistry.
   * @param {number} [initialLakeDepthM=50.0] - Initial paleolake depth in m (5.0 to 500.0 m)
   * @param {number} [evaporationRateMmPerYr=200.0] - Annual net evaporation rate in mm/yr (10 to 2000 mm/yr)
   * @param {number} [elapsedEvaporationYr=200.0] - Elapsed evaporation time in yr (1 to 10000 yr)
   * @param {number} [initialPH=2.50] - Initial brine pH (1.0 to 6.0)
   * @returns {{residualLakeDepthM: number, concentrationFactor: number, lakeDesiccationLifespanYr: number, activePrecipitatingPhaseClass: string, dominantMinerals: string, evaporiteSequenceContext: string}}
   */
  static computeMartianAcidLakeEvaporitePrecipitationSequence(initialLakeDepthM = 50.0, evaporationRateMmPerYr = 200.0, elapsedEvaporationYr = 200.0, initialPH = 2.50) {
    const h0M = Math.max(1.0, initialLakeDepthM);
    const EEvapMmYr = Math.max(1.0, evaporationRateMmPerYr);
    const tYr = Math.max(0.1, elapsedEvaporationYr);
    const pH0 = Math.max(0.5, Math.min(8.0, initialPH));

    const h0Mm = h0M * 1000.0;
    const tDryYr = h0Mm / EEvapMmYr;

    // Remaining water depth (m)
    const evaporatedDepthMm = Math.min(h0Mm, EEvapMmYr * tYr);
    const residualDepthMm = Math.max(0.0, h0Mm - evaporatedDepthMm);
    const residualDepthM = residualDepthMm / 1000.0;

    // Concentration factor
    const CF = residualDepthMm > 0.0 ? h0Mm / residualDepthMm : 100.0;

    let phaseClass = 'Dilute Acidic Lacustrine Water (Pre-Saturation)';
    let minerals = 'Dissolved Fe-Mg-Al-Ca-SO4 Ions';

    if (CF >= 30.0 || residualDepthM === 0.0) {
      phaseClass = 'Terminal Playa Desiccation (Hypersaline Halite + Kieserite Crust)';
      minerals = 'Halite (NaCl) + Kieserite (MgSO4 * H2O) + Anhydrite';
    } else if (CF >= 12.0) {
      phaseClass = 'Late-Stage Hypersaline Evaporite (Polyhydrated Mg-Sulfates)';
      minerals = 'Epsomite / Starkeyite (MgSO4 * nH2O) + Jarosite';
    } else if (CF >= 4.5) {
      phaseClass = 'Intermediate Acid-Sulfate Evaporite (Jarosite + Alunite Plateau)';
      minerals = 'Jarosite (KFe3(SO4)2(OH)6) + Alunite (KAl3(SO4)2(OH)6) + Gypsum';
    } else if (CF >= 2.0) {
      phaseClass = 'Early Calcium Sulfate Evaporite (Basal Gypsum Bed)';
      minerals = 'Gypsum (CaSO4 * 2H2O) + Bassanite';
    }

    return {
      residualLakeDepthM: parseFloat(residualDepthM.toFixed(2)),
      concentrationFactor: parseFloat(CF.toFixed(2)),
      lakeDesiccationLifespanYr: parseFloat(tDryYr.toFixed(1)),
      activePrecipitatingPhaseClass: phaseClass,
      dominantMinerals: minerals,
      evaporiteSequenceContext: `Acid Lake Evaporation (${CF.toFixed(1)}x Conc, ${residualDepthM.toFixed(1)}m Left, ${tDryYr.toFixed(0)} yr Lifespan, ${phaseClass})`
    };
  }

  /**
   * Calculate crustal hydraulic fracture vein opening, Sneddon elastic crack aperture, and hydrothermal fluid discharge flux.
   * w_max = 4 * ( 1 - nu^2 ) * Delta_P * L / ( pi * E )
   * w_mean = ( pi / 4 ) * w_max
   * v_fluid = ( w_mean^2 / ( 12 * mu ) ) * ( dP / dz )
   * Reference: Sneddon (1946), Pollard & Segall (1987), Rubin (1995), Ehlmann et al. (2011) for Crustal Hydrofracture Veins.
   * @param {number} [crustalDepthKm=4.0] - Vein depth in km (0.5 to 15.0 km)
   * @param {number} [fluidOverpressureMPa=25.0] - Fluid overpressure Delta P in MPa (1.0 to 100.0 MPa)
   * @param {number} [fractureLengthM=50.0] - Crack trace length in m (5.0 to 500.0 m)
   * @param {number} [hostRockYoungsModulusGPa=45.0] - Basalt Young's modulus in GPa (10.0 to 100.0 GPa)
   * @param {number} [hostRockPoissonsRatio=0.25] - Rock Poisson's ratio (0.15 to 0.35)
   * @returns {{maximumApertureMm: number, meanHydraulicApertureMm: number, fluidDischargeVelocityMS: number, dailyVolumetricDischargeM3Day: number, hydrofractureRegimeClass: string, hydrofractureContext: string}}
   */
  static computeMartianCrustalHydrofractureApertureAndFlux(crustalDepthKm = 4.0, fluidOverpressureMPa = 25.0, fractureLengthM = 50.0, hostRockYoungsModulusGPa = 45.0, hostRockPoissonsRatio = 0.25) {
    const zKm = Math.max(0.2, crustalDepthKm);
    const dPMPa = Math.max(0.5, fluidOverpressureMPa);
    const LM = Math.max(1.0, fractureLengthM);
    const EGPa = Math.max(5.0, hostRockYoungsModulusGPa);
    const nu = Math.max(0.10, Math.min(0.40, hostRockPoissonsRatio));

    const zM = zKm * 1000.0;
    const dPPa = dPMPa * 1.0e6;
    const EPa = EGPa * 1.0e9;
    const muFluidPaS = 3.0e-4; // Pa*s at 100 C
    const fractureWidthM = 10.0; // Standard 10 m strike width

    // Sneddon Griffith crack maximum opening (m & mm)
    const wMaxM = (4.0 * (1.0 - Math.pow(nu, 2.0)) * dPPa * LM) / (Math.PI * EPa);
    const wMaxMm = wMaxM * 1000.0;

    // Mean hydraulic aperture (m & mm)
    const wMeanM = (Math.PI / 4.0) * wMaxM;
    const wMeanMm = wMeanM * 1000.0;

    // Hydraulic gradient (Pa/m)
    const gradPPaM = dPPa / zM;

    // Darcy-Weisbach / Poiseuille discharge velocity (m/s)
    const vDischargeMS = (Math.pow(wMeanM, 2.0) / (12.0 * muFluidPaS)) * gradPPaM;

    // Volumetric discharge rate (m^3/s and m^3/day)
    const areaM2 = wMeanM * fractureWidthM;
    const qM3S = vDischargeMS * areaM2;
    const qM3Day = qM3S * 86400.0;

    let regimeClass = 'High-Overpressure Hydraulic Fracture Opening (Rapid Hydrothermal Vein Injection)';
    if (wMaxMm < 5.0) {
      regimeClass = 'Microfracture Crack-Seal Network (Slow Perched Aquifer Leakage)';
    } else if (wMaxMm >= 50.0) {
      regimeClass = 'Catastrophic Mega-Hydrofracture (Basalt Dike / Megabreccia Conduit)';
    }

    return {
      maximumApertureMm: parseFloat(wMaxMm.toFixed(2)),
      meanHydraulicApertureMm: parseFloat(wMeanMm.toFixed(2)),
      fluidDischargeVelocityMS: parseFloat(vDischargeMS.toFixed(1)),
      dailyVolumetricDischargeM3Day: parseFloat(qM3Day.toFixed(0)),
      hydrofractureRegimeClass: regimeClass,
      hydrofractureContext: `Hydrofracture at ${zKm.toFixed(1)}km (${wMeanMm.toFixed(1)} mm Aperture, ${dPMPa.toFixed(0)} MPa Overpressure, ${qM3Day.toExponential(2)} m3/d Discharge)`
    };
  }

  /**
   * Calculate geothermal/hydrothermal conversion kinetics of trioctahedral smectite (saponite) to mixed-layer corrensite and chlorite in deep Martian crust.
   * T_burial = T_surf + Gamma_geo * z
   * k = A * exp( -E_a / ( R * T ) ) * ( [Mg2+] / 100 )
   * f_chlorite = 1 - exp( -k * t )
   * Reference: Beaufort et al. (2015), Ehlmann et al. (2011), Carter et al. (2013) for Trioctahedral Clay Metamorphism.
   * @param {number} [burialDepthKm=6.0] - Crustal burial depth in km (1.0 to 15.0 km)
   * @param {number} [geothermalGradientKPerKm=35.0] - Geothermal gradient in K/km (10 to 80 K/km)
   * @param {number} [magnesiumActivityPpm=300.0] - Pore fluid Mg2+ activity in ppm (10 to 2000 ppm)
   * @param {number} [heatingDurationMyr=5.0] - Thermal alteration duration in Myr (0.1 to 100 Myr)
   * @returns {{burialTemperatureC: number, burialTemperatureK: number, chloriteFractionPercent: number, metamorphicGradeClass: string, expelledFluidWtPct: number, clayMetamorphismContext: string}}
   */
  static computeMartianSmectiteToChloriteMetamorphicKinetics(burialDepthKm = 6.0, geothermalGradientKPerKm = 35.0, magnesiumActivityPpm = 300.0, heatingDurationMyr = 5.0) {
    const zKm = Math.max(0.5, burialDepthKm);
    const gammaGeo = Math.max(5.0, geothermalGradientKPerKm);
    const MgPpm = Math.max(5.0, magnesiumActivityPpm);
    const tMyr = Math.max(0.01, heatingDurationMyr);

    const TsurfK = 215.0;
    const Rgas = 8.314;
    const Ea = 9.5e4; // 95 kJ/mol
    const A = 2.5e8; // 1/yr

    // Burial temperature
    const TburialK = TsurfK + (gammaGeo * zKm);
    const TburialC = TburialK - 273.15;

    // Reaction rate constant (1/yr)
    const MgFactor = MgPpm / 100.0;
    const kYr = A * Math.exp(-Ea / (Rgas * TburialK)) * MgFactor;

    // Chlorite fraction
    const tYr = tMyr * 1.0e6;
    const exponent = Math.min(50.0, kYr * tYr);
    const fChlorite = 1.0 - Math.exp(-exponent);
    const chloritePct = fChlorite * 100.0;

    // Expelled fluid (wt%)
    const expelledFluid = fChlorite * 8.5;

    let gradeClass = 'Trioctahedral Smectite (Diagenetically Immature Saponite)';
    if (TburialC >= 200.0 || chloritePct >= 85.0) {
      gradeClass = 'Greenschist Facies Chlorite (Clinochlore / Chamosite)';
    } else if (TburialC >= 100.0 || chloritePct >= 30.0) {
      gradeClass = 'Mixed-Layer Corrensite (50:50 Chlorite-Smectite Transition)';
    }

    return {
      burialTemperatureC: parseFloat(TburialC.toFixed(1)),
      burialTemperatureK: parseFloat(TburialK.toFixed(1)),
      chloriteFractionPercent: parseFloat(chloritePct.toFixed(1)),
      metamorphicGradeClass: gradeClass,
      expelledFluidWtPct: parseFloat(expelledFluid.toFixed(2)),
      clayMetamorphismContext: `Chlorite Metamorphism at ${zKm.toFixed(1)}km (${TburialC.toFixed(0)} C, ${chloritePct.toFixed(0)}% Chlorite, ${gradeClass})`
    };
  }

  /**
   * Calculate extreme acid-sulfate hydrothermal leaching of basaltic cations, secondary sieve/vuggy porosity formation, residual silica enrichment, and thermal inertia drop.
   * f_extract = 1 - exp( -k_leach * t )
   * SiO2_wt% = SiO2_0 / ( SiO2_0 + Cations_0 * ( 1 - f_extract ) )
   * phi_sieve = phi_0 + Cations_0 * f_extract * ( 1 - phi_0 )
   * Reference: Squyres et al. (2008), Morris et al. (2008), Ruff et al. (2011), Tosca & McLennan (2006) for Home Plate Acid Leached Silica.
   * @param {number} [fluidPH=1.50] - Hydrothermal fluid pH (0.5 to 5.0)
   * @param {number} [hydrothermalTemperatureC=80.0] - Leaching fluid temperature in C (10 to 200 C)
   * @param {number} [leachingDurationYr=50.0] - Acid circulation duration in yr (0.1 to 1000 yr)
   * @param {number} [initialPorosity=0.10] - Unaltered basalt bedrock porosity (0.02 to 0.30)
   * @returns {{residualSilicaWtPercent: number, cationExtractionPercent: number, finalSievePorosityPercent: number, bulkDensityKgM3: number, thermalInertiaTIU: number, silicaAlterationClass: string, acidLeachingContext: string}}
   */
  static computeMartianAcidLeachingSilicaEnrichmentPorosity(fluidPH = 1.50, hydrothermalTemperatureC = 80.0, leachingDurationYr = 50.0, initialPorosity = 0.10) {
    const pH = Math.max(0.2, Math.min(6.0, fluidPH));
    const TfluidC = Math.max(5.0, hydrothermalTemperatureC);
    const tYr = Math.max(0.01, leachingDurationYr);
    const phi0 = Math.max(0.01, Math.min(0.50, initialPorosity));

    const TfluidK = TfluidC + 273.15;
    const Rgas = 8.314;
    const Ea = 5.5e4; // 55 kJ/mol
    const k0 = 5.0e7; // yr^-1

    const HplusMol = Math.pow(10.0, -pH);
    const kLeachYr = k0 * Math.sqrt(HplusMol) * Math.exp(-Ea / (Rgas * TfluidK));

    // Cation extraction fraction
    const fExtract = 1.0 - Math.exp(-kLeachYr * tYr);
    const fExtractPct = fExtract * 100.0;

    // Basalt initial fractions: 48 wt% SiO2, 52 wt% cations
    const wSiO2_0 = 0.48;
    const wCations_0 = 0.52;
    const wCationsRemaining = wCations_0 * (1.0 - fExtract);
    const residualSiO2Pct = (wSiO2_0 / (wSiO2_0 + wCationsRemaining)) * 100.0;

    // Sieve/vuggy porosity created by cation leaching
    const deltaPhi = wCations_0 * fExtract * (1.0 - phi0);
    const phiFinal = Math.min(0.85, phi0 + deltaPhi);
    const phiFinalPct = phiFinal * 100.0;

    // Bulk density & thermal inertia
    const rhoMatrix = 2200.0; // kg/m^3
    const rhoBulk = rhoMatrix * (1.0 - phiFinal);
    const kTherm = 0.040 * Math.pow(1.0 - phiFinal, 2.0); // W/(m*K)
    const Cspec = 750.0; // J/(kg*K)
    const TIU = Math.sqrt(kTherm * rhoBulk * Cspec);

    let alterClass = 'Unaltered Basalt / Incipient Cation Leaching';
    if (residualSiO2Pct >= 90.0) {
      alterClass = 'High-Purity Vesicular Opaline Silica Residue (Fumarolic Acid Fog Sieve)';
    } else if (residualSiO2Pct >= 70.0) {
      alterClass = 'Moderate Leached Basalt (Alunite-Kaolinite-Silica Horizon)';
    }

    return {
      residualSilicaWtPercent: parseFloat(residualSiO2Pct.toFixed(1)),
      cationExtractionPercent: parseFloat(fExtractPct.toFixed(1)),
      finalSievePorosityPercent: parseFloat(phiFinalPct.toFixed(1)),
      bulkDensityKgM3: parseFloat(rhoBulk.toFixed(1)),
      thermalInertiaTIU: parseFloat(TIU.toFixed(1)),
      silicaAlterationClass: alterClass,
      acidLeachingContext: `Acid Leaching at pH ${pH.toFixed(1)} (${residualSiO2Pct.toFixed(1)}% SiO2, ${phiFinalPct.toFixed(1)}% Porosity, TIU=${TIU.toFixed(0)}, ${alterClass})`
    };
  }

  /**
   * Calculate contact metamorphic thermal dehydroxylation, structural lattice collapse, and recrystallization kinetics of baked clay phyllosilicates.
   * k = A * exp( -E_a / ( R * T ) )
   * f_dehydrox = 1 - exp( -k * t )
   * m_steam = rho_clay * w_OH_0 * f_dehydrox
   * Reference: Guggenheim et al. (1987), Bish (1993), Chemtob et al. (2010), Ehlmann et al. (2011) for Baked Martian Phyllosilicates.
   * @param {number} [contactTemperatureC=650.0] - Intrusive dike contact baking temperature in C (100 to 1200 C)
   * @param {number} [initialClayWaterContentWtPct=14.0] - Unbaked phyllosilicate structural OH content in wt% (5 to 25 wt%)
   * @param {number} [thermalEventDurationYr=100.0] - Thermal intrusion duration in yr (0.1 to 10000 yr)
   * @param {number} [activationEnergyKJPerMol=180.0] - Dehydroxylation activation energy in kJ/mol (100 to 300 kJ/mol)
   * @returns {{dehydroxylationFractionPercent: number, residualWaterContentWtPct: number, expelledSteamKgPerM3: number, bakedThermalInertiaTIU: number, metamorphicPhaseClass: string, thermalBakingContext: string}}
   */
  static computeMartianClayDehydroxylationRecrystallizationKinetics(contactTemperatureC = 650.0, initialClayWaterContentWtPct = 14.0, thermalEventDurationYr = 100.0, activationEnergyKJPerMol = 180.0) {
    const TcontactC = Math.max(50.0, contactTemperatureC);
    const wOH0 = Math.max(1.0, Math.min(30.0, initialClayWaterContentWtPct));
    const tYr = Math.max(0.01, thermalEventDurationYr);
    const EaKJ = Math.max(50.0, activationEnergyKJPerMol);

    const TcontactK = TcontactC + 273.15;
    const Rgas = 8.314;
    const EaJ = EaKJ * 1000.0;
    const A = 1.0e10; // yr^-1

    const kDehydroxYr = A * Math.exp(-EaJ / (Rgas * TcontactK));
    const exponent = Math.min(50.0, kDehydroxYr * tYr);
    const fDehydrox = 1.0 - Math.exp(-exponent);
    const dehydroxPct = fDehydrox * 100.0;

    const wOHFinal = wOH0 * (1.0 - fDehydrox);
    const rhoClay = 2400.0; // kg/m^3
    const expelledSteamKgM3 = rhoClay * (wOH0 / 100.0) * fDehydrox;

    // Metamorphic recrystallization phase & thermal properties
    let phaseClass = 'Pristine Hydrated Smectite Clay (Unbaked)';
    let kTherm = 0.40; // W/(m*K)
    let rhoBulk = 2400.0;

    if (TcontactC >= 600.0 && dehydroxPct >= 80.0) {
      phaseClass = 'High-Grade Baked Hornfels (Anhydrous Pyroxene + Spinel + Cristobalite Recrystallization)';
      kTherm = 2.10;
      rhoBulk = 2750.0;
    } else if (TcontactC >= 400.0 && dehydroxPct >= 30.0) {
      phaseClass = 'Partially Dehydroxylated Amorphous Phase (Lattice Collapse / Hydroxyl Depleted)';
      kTherm = 1.10;
      rhoBulk = 2550.0;
    }

    const Cspec = 850.0; // J/(kg*K)
    const TIU = Math.sqrt(kTherm * rhoBulk * Cspec);

    return {
      dehydroxylationFractionPercent: parseFloat(dehydroxPct.toFixed(1)),
      residualWaterContentWtPct: parseFloat(wOHFinal.toFixed(2)),
      expelledSteamKgPerM3: parseFloat(expelledSteamKgM3.toFixed(1)),
      bakedThermalInertiaTIU: parseFloat(TIU.toFixed(1)),
      metamorphicPhaseClass: phaseClass,
      thermalBakingContext: `Contact Baking at ${TcontactC.toFixed(0)} C (${dehydroxPct.toFixed(0)}% Dehydroxylated, ${expelledSteamKgM3.toFixed(0)} kg/m3 Steam, TIU=${TIU.toFixed(0)})`
    };
  }

  /**
   * Calculate alkaline hydrothermal spring clay-carbonate co-precipitation kinetics, saturation state, mineral mass fractions, and thermal inertia.
   * IAP = [Mg2+] * [CO3^2-]
   * Omega = IAP / K_sp
   * w_carb = 0.15 + 0.05 * ln( Omega / 100 )
   * Reference: Morris et al. (2010), Ruff et al. (2014), Ehlmann et al. (2008), Horgan et al. (2020) for Martian Comanche & Jezero Carbonates.
   * @param {number} [fluidPH=9.50] - Hydrothermal spring fluid pH (7.0 to 12.0)
   * @param {number} [dissolvedCO2ActivityMol=0.050] - Dissolved inorganic carbon activity in mol/L (0.001 to 0.50 mol/L)
   * @param {number} [hydrothermalTempC=60.0] - Spring emergence temperature in C (10 to 120 C)
   * @param {number} [calciumMagnesiumRatio=0.20] - Fluid Ca2+/Mg2+ molar ratio (0.01 to 2.0)
   * @returns {{carbonateSaturationState: number, carbonateWeightPercent: number, saponiteClayWeightPercent: number, magnesiteMolarPercent: number, compositeThermalInertiaTIU: number, alkalineSpringRegimeClass: string, clayCarbonateContext: string}}
   */
  static computeMartianClayCarbonateCoPrecipitationKinetics(fluidPH = 9.50, dissolvedCO2ActivityMol = 0.050, hydrothermalTempC = 60.0, calciumMagnesiumRatio = 0.20) {
    const pH = Math.max(6.5, Math.min(13.0, fluidPH));
    const DIC = Math.max(0.0001, dissolvedCO2ActivityMol);
    const TspringC = Math.max(5.0, hydrothermalTempC);
    const CaMg = Math.max(0.01, calciumMagnesiumRatio);

    // Carbonate ion fraction at alkaline pH
    let carbFrac = 0.01;
    if (pH >= 10.0) {
      carbFrac = 0.50;
    } else if (pH >= 9.0) {
      carbFrac = 0.22;
    } else if (pH >= 8.0) {
      carbFrac = 0.05;
    }
    const CO3Mol = DIC * carbFrac;
    const MgMol = 0.020; // 0.02 M Mg2+

    const IAP = MgMol * CO3Mol;
    const Ksp = 3.5e-8;
    const Omega = Math.max(1.0, IAP / Ksp);

    // Carbonate vs Saponite mass fraction
    const wCarb = Math.max(0.05, Math.min(0.60, 0.15 + (0.05 * Math.log(Omega / 100.0))));
    const wCarbPct = wCarb * 100.0;
    const wClayPct = (1.0 - wCarb) * 100.0;

    // Magnesite molar fraction vs calcite
    const magnesitePct = (1.0 / (1.0 + CaMg)) * 100.0;

    // Physical properties & thermal inertia
    const rhoGrain = (wCarb * 3000.0) + ((1.0 - wCarb) * 2400.0);
    const phi = 0.18;
    const rhoBulk = rhoGrain * (1.0 - phi);
    const kTherm = (0.45 * (1.0 - phi)) + (1.20 * wCarb);
    const Cspec = 800.0;
    const TIU = Math.sqrt(kTherm * rhoBulk * Cspec);

    let springClass = 'Low-Yield Neutral Hot Spring';
    if (wCarbPct >= 30.0 && pH >= 9.0) {
      springClass = 'Alkaline Magnesite-Saponite Hydrothermal Travertine (Comanche Outcrop / Jezero Margin Carbonate)';
    } else if (wCarbPct >= 15.0) {
      springClass = 'Sublacustrine Carbonate-Bearing Smectite Clay Mudstone';
    }

    return {
      carbonateSaturationState: parseFloat(Omega.toFixed(1)),
      carbonateWeightPercent: parseFloat(wCarbPct.toFixed(1)),
      saponiteClayWeightPercent: parseFloat(wClayPct.toFixed(1)),
      magnesiteMolarPercent: parseFloat(magnesitePct.toFixed(1)),
      compositeThermalInertiaTIU: parseFloat(TIU.toFixed(1)),
      alkalineSpringRegimeClass: springClass,
      clayCarbonateContext: `Alkaline Spring pH ${pH.toFixed(1)} (${wCarbPct.toFixed(0)}% Carbonate, ${wClayPct.toFixed(0)}% Saponite, TIU=${TIU.toFixed(0)}, ${springClass})`
    };
  }

  /**
   * Calculate subsurface Methane Clathrate Hydrate (CH4 * 5.75 H2O) Stability Zone (MHSZ) depth extent, dissociation boundary, and storage capacity in Martian cryosphere.
   * T(z) = T_surf + Gamma_geo * z
   * T_diss(P) = T_0 + A * ln( P / P_ref ) - Delta_T_salinity
   * Reference: Max & Clifford (2000), Chastain & Chevrier (2007), Gainey & Elwood Madden (2012) for Martian Clathrate Stability.
   * @param {number} [surfaceTemperatureK=215.0] - Mean surface temperature in K (150 to 240 K)
   * @param {number} [geothermalGradientKPerKm=20.0] - Geothermal gradient in K/km (10 to 50 K/km)
   * @param {number} [poreWaterSalinityGPerKg=0.0] - Pore water salinity in g/kg (0 to 100 g/kg)
   * @param {number} [regolithPorosity=0.25] - Cryosphere regolith porosity (0.05 to 0.50)
   * @returns {{mhszTopDepthM: number, mhszBaseDepthM: number, mhszThicknessM: number, dissociationTemperatureAtBaseK: number, volumetricGasStorageM3STPPerM2: number, clathrateStabilityClass: string, clathrateContext: string}}
   */
  static computeMartianMethaneClathrateHydrateStabilityZone(surfaceTemperatureK = 215.0, geothermalGradientKPerKm = 20.0, poreWaterSalinityGPerKg = 0.0, regolithPorosity = 0.25) {
    const TsurfK = Math.max(130.0, Math.min(250.0, surfaceTemperatureK));
    const gammaGeo = Math.max(5.0, geothermalGradientKPerKm);
    const Sppt = Math.max(0.0, Math.min(200.0, poreWaterSalinityGPerKg));
    const phi = Math.max(0.02, Math.min(0.60, regolithPorosity));

    const gammaGeoKm = gammaGeo / 1000.0; // K/m

    // Salinity depression of dissociation temperature
    const deltaTsal = 0.58 * (Sppt / 10.0);

    // Top depth where cryosphere ice/gas sealing allows clathrate stability
    const zTopM = 15.0;

    // Numerical / Analytical solve for base depth where T(z) = T_diss(P(z))
    // P(z) = 0.006 + 0.0558 * z (bar)
    // T_diss(P) = 271.85 + 13.5 * ln( P / 25.6 ) - deltaTsal
    let zBaseM = 3000.0;
    for (let iter = 0; iter < 10; iter++) {
      const Pbar = Math.max(1.0, 0.006 + (0.0558 * zBaseM));
      const TdissK = 271.85 + (13.5 * Math.log(Pbar / 25.6)) - deltaTsal;
      zBaseM = (TdissK - TsurfK) / gammaGeoKm;
    }
    zBaseM = Math.max(zTopM + 100.0, zBaseM);

    const thicknessM = zBaseM - zTopM;
    const PbaseBar = 0.006 + (0.0558 * zBaseM);
    const TdissBaseK = 271.85 + (13.5 * Math.log(PbaseBar / 25.6)) - deltaTsal;

    // Volumetric STP methane storage per m^2 column
    const hydrateSaturation = 0.40; // 40% pore volume filled with hydrate
    const stpYieldM3PerM3 = 164.0;
    const stpM3M2 = stpYieldM3PerM3 * phi * hydrateSaturation * thicknessM;

    let stabilityClass = 'Deep Stable Cryospheric Methane Clathrate Reservoir';
    if (thicknessM >= 3000.0) {
      stabilityClass = 'Vast Subsurface Polar/Equatorial Cryosphere Clathrate Hydrate Shield';
    } else if (thicknessM < 1000.0) {
      stabilityClass = 'Thin Marginal Clathrate Stability Horizon (Vulnerable to Thermal Plumes)';
    }

    return {
      mhszTopDepthM: parseFloat(zTopM.toFixed(1)),
      mhszBaseDepthM: parseFloat(zBaseM.toFixed(0)),
      mhszThicknessM: parseFloat(thicknessM.toFixed(0)),
      dissociationTemperatureAtBaseK: parseFloat(TdissBaseK.toFixed(1)),
      volumetricGasStorageM3STPPerM2: parseFloat(stpM3M2.toFixed(0)),
      clathrateStabilityClass: stabilityClass,
      clathrateContext: `MHSZ (${zTopM.toFixed(0)}m to ${zBaseM.toFixed(0)}m Depth, ${thicknessM.toFixed(0)}m Thick, ${stpM3M2.toExponential(2)} m3 STP/m2 CH4)`
    };
  }

  /**
   * Calculate subsurface liquefied sediment / mud volcanism conduit flow, low-pressure flash-boiling eruption, plume height, and flow runout length.
   * v_ascent = ( r_pipe^2 * Delta_P ) / ( 8 * mu * z )
   * Q = pi * r_pipe^2 * v_ascent
   * h_plume = v_ascent^2 / ( 2 * g_mars )
   * Reference: Komatsu et al. (2016), Broz et al. (2020), Skinner & Mazzini (2009) for Martian Mud Volcanism.
   * @param {number} [conduitRadiusM=2.5] - Feeder conduit pipe radius in m (0.2 to 20.0 m)
   * @param {number} [reservoirDepthKm=3.0] - Liquefied sediment reservoir depth in km (0.5 to 10.0 km)
   * @param {number} [fluidOverpressureMPa=15.0] - Driving pore overpressure in MPa (1.0 to 100.0 MPa)
   * @param {number} [mudViscosityPaS=50.0] - Dynamic mud slurry viscosity in Pa*s (1.0 to 1000.0 Pa*s)
   * @param {number} [mudDensityKgM3=1750.0] - Mud density in kg/m^3 (1200 to 2400 kg/m^3)
   * @returns {{ascentVelocityMS: number, volumetricDischargeM3S: number, dailyEruptedVolumeM3Day: number, flashBoilingPlumeHeightM: number, mudFlowRunoutLengthKm: number, mudVolcanoEdificeClass: string, mudEruptionContext: string}}
   */
  static computeMartianMudVolcanismEruptionDynamics(conduitRadiusM = 2.5, reservoirDepthKm = 3.0, fluidOverpressureMPa = 15.0, mudViscosityPaS = 50.0, mudDensityKgM3 = 1750.0) {
    const rPipe = Math.max(0.1, conduitRadiusM);
    const zKm = Math.max(0.2, reservoirDepthKm);
    const dPMPa = Math.max(0.2, fluidOverpressureMPa);
    const muMud = Math.max(0.5, mudViscosityPaS);
    const rhoMud = Math.max(1000.0, mudDensityKgM3);

    const zM = zKm * 1000.0;
    const dPPa = dPMPa * 1.0e6;
    const gMars = 3.72; // m/s^2

    // Hagen-Poiseuille laminar pipe flow ascent velocity (m/s)
    const vAscentMS = (Math.pow(rPipe, 2.0) * dPPa) / (8.0 * muMud * zM);

    // Volumetric discharge rate (m^3/s and m^3/day)
    const pipeAreaM2 = Math.PI * Math.pow(rPipe, 2.0);
    const qM3S = pipeAreaM2 * vAscentMS;
    const qM3Day = qM3S * 86400.0;

    // Flash-boiling ballistic jet plume height (m)
    const hPlumeM = Math.pow(vAscentMS, 2.0) / (2.0 * gMars);

    // Cryo-mud flow runout length (km)
    const runoutKm = Math.sqrt(qM3Day / 20.0) / 1000.0 * 7.0;

    let edificeClass = 'Small Extrusive Mud Conette / Gryphon';
    if (qM3Day >= 5.0e7) {
      edificeClass = 'Catastrophic Mega-Mud Volcano (Km-Scale Shield with Central Caldera Pit / Chryse Planitia)';
    } else if (qM3Day >= 5.0e5) {
      edificeClass = 'Moderate Sedimentary Mud Cone / Diapir Field (Utopia Planitia Mounds)';
    }

    return {
      ascentVelocityMS: parseFloat(vAscentMS.toFixed(2)),
      volumetricDischargeM3S: parseFloat(qM3S.toFixed(1)),
      dailyEruptedVolumeM3Day: parseFloat(qM3Day.toFixed(0)),
      flashBoilingPlumeHeightM: parseFloat(hPlumeM.toFixed(1)),
      mudFlowRunoutLengthKm: parseFloat(runoutKm.toFixed(1)),
      mudVolcanoEdificeClass: edificeClass,
      mudEruptionContext: `Mud Volcano at ${zKm.toFixed(1)}km (${vAscentMS.toFixed(1)} m/s Exit, ${hPlumeM.toFixed(0)}m Flash-Boil Plume, ${runoutKm.toFixed(1)}km Runout)`
    };
  }

  /**
   * Calculate burial diagenetic illitization kinetics of dioctahedral smectite (montmorillonite), Reichweite ordering (R0->R1->R3), and paleothermometry.
   * T_burial = T_surf + Gamma_geo * z
   * k = A * ( [K+] / 100 ) * exp( -E_a / ( R * T ) )
   * %I = ( 1 - exp( -k * t ) ) * 100
   * Reference: Pytte & Reynolds (1989), Essene & Peacor (1995), Ehlmann et al. (2011) for Dioctahedral Clay Illitization.
   * @param {number} [burialDepthKm=5.8] - Crustal burial depth in km (1.0 to 15.0 km)
   * @param {number} [geothermalGradientKPerKm=30.0] - Geothermal gradient in K/km (10 to 60 K/km)
   * @param {number} [potassiumActivityPpm=250.0] - Pore fluid K+ activity in ppm (10 to 2000 ppm)
   * @param {number} [burialDurationMyr=25.0] - Burial diagenesis duration in Myr (0.1 to 100 Myr)
   * @returns {{burialTemperatureC: number, burialTemperatureK: number, illiteLayerPercent: number, reichweiteOrderingClass: string, expelledInterlayerWaterWtPct: number, diageneticGeothermometerContext: string}}
   */
  static computeMartianSmectiteToIlliteDiagenesisKinetics(burialDepthKm = 5.8, geothermalGradientKPerKm = 30.0, potassiumActivityPpm = 250.0, burialDurationMyr = 25.0) {
    const zKm = Math.max(0.5, burialDepthKm);
    const gammaGeo = Math.max(5.0, geothermalGradientKPerKm);
    const Kppm = Math.max(5.0, potassiumActivityPpm);
    const tMyr = Math.max(0.01, burialDurationMyr);

    const TsurfK = 215.0;
    const Rgas = 8.314;
    const Ea = 1.17e5; // 117 kJ/mol
    const A = 5.2e8; // 1/yr

    // Burial temperature
    const TburialK = TsurfK + (gammaGeo * zKm);
    const TburialC = TburialK - 273.15;

    // Rate constant (1/yr)
    const KFactor = Kppm / 100.0;
    const kYr = A * KFactor * Math.exp(-Ea / (Rgas * TburialK));

    // Illite layer percentage
    const tYr = tMyr * 1.0e6;
    const exponent = Math.min(50.0, kYr * tYr);
    const fIllite = 1.0 - Math.exp(-exponent);
    const illitePct = fIllite * 100.0;

    // Interlayer water loss (wt%)
    const lostWater = fIllite * 8.0;

    let orderClass = 'R0 Random Mixed-Layer Illite/Smectite (Smectite-Dominant)';
    if (illitePct >= 85.0) {
      orderClass = 'R3 Highly Ordered Illite / Sericite (ISII Metamorphic Precursor)';
    } else if (illitePct >= 50.0) {
      orderClass = 'R1 Regularly Ordered Illite/Smectite (IS-Type Intermediate)';
    }

    return {
      burialTemperatureC: parseFloat(TburialC.toFixed(1)),
      burialTemperatureK: parseFloat(TburialK.toFixed(1)),
      illiteLayerPercent: parseFloat(illitePct.toFixed(1)),
      reichweiteOrderingClass: orderClass,
      expelledInterlayerWaterWtPct: parseFloat(lostWater.toFixed(2)),
      diageneticGeothermometerContext: `Illite Diagenesis at ${zKm.toFixed(1)}km (${TburialC.toFixed(0)} C, ${illitePct.toFixed(0)}% Illite, ${orderClass})`
    };
  }

  /**
   * Calculate hyper-acidic groundwater jarosite (KFe3(SO4)2(OH)6) precipitation kinetics, evaporite mass fractions, and sulfate sandstone thermal inertia.
   * IAP = [K+] * [Fe3+]^3 * [SO4 2-]^2 * [OH-]^6
   * Omega = IAP / K_sp
   * w_jarosite = 0.10 + 0.05 * ln( Omega / 100 )
   * Reference: Klingelhofer et al. (2004), Squyres et al. (2004), Madden et al. (2004), Papike et al. (2006) for Meridiani Burns Formation.
   * @param {number} [fluidPH=2.00] - Groundwater brine pH (0.5 to 5.0)
   * @param {number} [sulfateConcentrationMol=0.50] - SO4 2- molar concentration in mol/L (0.01 to 2.0 mol/L)
   * @param {number} [ferricIronConcentrationMol=0.10] - Fe3+ molar concentration in mol/L (0.001 to 1.0 mol/L)
   * @param {number} [fluidTempC=15.0] - Groundwater emergence temperature in C (0 to 60 C)
   * @returns {{jarositeSaturationState: number, jarositeWeightPercent: number, polyhydratedSulfateWeightPercent: number, hematiteWeightPercent: number, evaporiteThermalInertiaTIU: number, acidSulfateFaciesClass: string, jarositeParagenesisContext: string}}
   */
  static computeMartianJarositePrecipitationKinetics(fluidPH = 2.00, sulfateConcentrationMol = 0.50, ferricIronConcentrationMol = 0.10, fluidTempC = 15.0) {
    const pH = Math.max(0.5, Math.min(6.0, fluidPH));
    const SO4Mol = Math.max(0.005, sulfateConcentrationMol);
    const Fe3Mol = Math.max(0.0005, ferricIronConcentrationMol);
    const TC = Math.max(0.0, fluidTempC);

    const KMol = 0.020; // 0.020 M K+
    const OHMol = Math.pow(10.0, -(14.0 - pH));

    // Ion activity product
    const IAP = KMol * Math.pow(Fe3Mol, 3.0) * Math.pow(SO4Mol, 2.0) * Math.pow(OHMol, 6.0);
    const Ksp = 1.0e-98; // Nominal solubility product
    const Omega = Math.max(1.0, IAP / Ksp);

    // Jarosite mass fraction in evaporite matrix
    let wJarosite = 0.05;
    if (pH <= 3.5) {
      wJarosite = Math.max(0.10, Math.min(0.45, 0.25 - (0.04 * (pH - 2.0))));
    } else {
      wJarosite = 0.02; // Jarosite hydrolyzes to goethite/hematite at pH > 3.5
    }
    const wJarositePct = wJarosite * 100.0;

    // Associated evaporite minerals (Burns Formation model)
    const wPolySulfatePct = (1.0 - wJarosite) * 55.0;
    const wHematitePct = (1.0 - wJarosite) * 15.0;
    const wSilicatePct = 100.0 - wJarositePct - wPolySulfatePct - wHematitePct;

    // Thermal inertia of porous sulfate sandstone
    const rhoGrain = (wJarosite * 3150.0) + ((wPolySulfatePct / 100.0) * 2320.0) + ((wHematitePct / 100.0) * 5260.0) + ((wSilicatePct / 100.0) * 2650.0);
    const phi = 0.30; // 30% porosity
    const rhoBulk = rhoGrain * (1.0 - phi);
    const kTherm = 0.15; // W/(m K)
    const Cspec = 780.0; // J/(kg K)
    const TIU = Math.sqrt(kTherm * rhoBulk * Cspec);

    let faciesClass = 'Neutral Hydrated Sulfate Playa';
    if (pH <= 2.5 && wJarositePct >= 20.0) {
      faciesClass = 'Hyper-Acidic Jarosite-Rich Evaporite Sandstone (Meridiani Planum Burns Formation / Noctis Labyrinthus)';
    } else if (pH <= 3.5) {
      faciesClass = 'Acid-Sulfate Groundwater Leached Bedrock';
    }

    return {
      jarositeSaturationState: parseFloat(Omega.toExponential(2)),
      jarositeWeightPercent: parseFloat(wJarositePct.toFixed(1)),
      polyhydratedSulfateWeightPercent: parseFloat(wPolySulfatePct.toFixed(1)),
      hematiteWeightPercent: parseFloat(wHematitePct.toFixed(1)),
      evaporiteThermalInertiaTIU: parseFloat(TIU.toFixed(1)),
      acidSulfateFaciesClass: faciesClass,
      jarositeParagenesisContext: `Acid-Sulfate pH ${pH.toFixed(1)} (${wJarositePct.toFixed(0)}% Jarosite, ${wPolySulfatePct.toFixed(0)}% Sulfate, TIU=${TIU.toFixed(0)}, ${faciesClass})`
    };
  }

  /**
   * Calculate subsurface evaporite salt (halite/anhydrite) diapirism, dislocation creep halokinesis rheology, dome uplift rate, and halite thermal inertia.
   * Delta_P = Delta_rho * g_mars * z
   * eps_dot = A * sigma^n * exp( -Q / ( R * T ) )
   * eta_eff = sigma / ( 2 * eps_dot )
   * v_diapir = ( 2 * Delta_rho * g_mars * r^2 ) / ( 9 * eta_eff )
   * Reference: Baioni & Tramontana (2016), Jackson et al. (2008), Urai et al. (2008) for Valles Marineris Salt Domes.
   * @param {number} [sedimentOverburdenThicknessKm=4.0] - Basaltic sediment overburden depth in km (1.0 to 12.0 km)
   * @param {number} [saltBedThicknessM=500.0] - Evaporite salt source layer thickness in m (50 to 3000 m)
   * @param {number} [geothermalGradientKPerKm=25.0] - Geothermal gradient in K/km (10 to 50 K/km)
   * @param {number} [differentialStressMPa=8.8] - Driving tectonic/buoyancy differential stress in MPa (0.5 to 50.0 MPa)
   * @returns {{burialTemperatureC: number, effectiveSaltViscosityPaS: number, strainRatePerSec: number, diapiricAscentRateMmPerYr: number, haliteThermalInertiaTIU: number, halokinesisStructuralClass: string, diapirismContext: string}}
   */
  static computeMartianSaltDiapirismHalokinesisKinetics(sedimentOverburdenThicknessKm = 4.0, saltBedThicknessM = 500.0, geothermalGradientKPerKm = 25.0, differentialStressMPa = 8.8) {
    const zKm = Math.max(0.5, sedimentOverburdenThicknessKm);
    const hSaltM = Math.max(20.0, saltBedThicknessM);
    const gammaGeo = Math.max(5.0, geothermalGradientKPerKm);
    const sigmaMPa = Math.max(0.1, differentialStressMPa);

    const TsurfK = 215.0;
    const Rgas = 8.314;
    const Qcreep = 1.05e5; // 105 kJ/mol
    const A = 1.6e-4; // MPa^-n s^-1
    const n = 4.5; // Dislocation creep stress exponent
    const gMars = 3.72; // m/s^2
    const deltaRho = 2750.0 - 2160.0; // 590 kg/m^3 buoyancy density contrast

    // Salt source bed temperature
    const TburialK = TsurfK + (gammaGeo * zKm);
    const TburialC = TburialK - 273.15;

    // Dislocation creep strain rate (1/s)
    const epsDot = A * Math.pow(sigmaMPa, n) * Math.exp(-Qcreep / (Rgas * TburialK));
    const sigmaPa = sigmaMPa * 1.0e6;

    // Effective dynamic viscosity (Pa*s)
    const etaEff = Math.max(1.0e15, sigmaPa / (2.0 * Math.max(1.0e-25, epsDot)));

    // Stokes diapiric ascent rate (m/s and mm/yr)
    const rDiapirM = Math.min(2000.0, hSaltM);
    const vAscentMS = (2.0 * deltaRho * gMars * Math.pow(rDiapirM, 2.0)) / (9.0 * etaEff);
    const vAscentMmYr = vAscentMS * 3.15576e10; // m/s to mm/yr

    // Thermal inertia of crystalline halite
    const kTherm = 5.50; // W/(m K)
    const rhoBulk = 2160.0 * 0.98;
    const Cspec = 860.0; // J/(kg K)
    const TIU = Math.sqrt(kTherm * rhoBulk * Cspec);

    let structClass = 'Incipient Salt Pillow / Low-Relief Swell';
    if (vAscentMmYr >= 0.10) {
      structClass = 'Active Piercement Salt Diapir / Extrusive Salt Glacier (Namakier / Candor Chasma)';
    } else if (vAscentMmYr >= 0.001) {
      structClass = 'Mature Salt Dome / Bulging Sedimentary Anticlinal Core (Juventae Chasma)';
    }

    return {
      burialTemperatureC: parseFloat(TburialC.toFixed(1)),
      effectiveSaltViscosityPaS: parseFloat(etaEff.toExponential(2)),
      strainRatePerSec: parseFloat(epsDot.toExponential(2)),
      diapiricAscentRateMmPerYr: parseFloat(vAscentMmYr.toExponential(2)),
      haliteThermalInertiaTIU: parseFloat(TIU.toFixed(1)),
      halokinesisStructuralClass: structClass,
      diapirismContext: `Salt Diapirism at ${zKm.toFixed(1)}km (${TburialC.toFixed(0)} C, Viscosity=${etaEff.toExponential(1)} Pa*s, TIU=${TIU.toFixed(0)}, ${structClass})`
    };
  }

  /**
   * Calculate subsurface magnesium perchlorate (Mg(ClO4)2) eutectic brine rheology, low-temperature viscosity divergence, Darcy seepage flow, and RSL creep velocity.
   * eta(T) = eta_0 * exp( B / ( T - T_0 ) )
   * K = ( k_perm * rho_brine * g_mars ) / eta
   * v_pore = ( K * i_gradient ) / phi
   * Reference: Hecht et al. (2009), Chevrier et al. (2009), Toner & Catling (2016) for Martian Perchlorate Brines.
   * @param {number} [magnesiumPerchlorateWeightPercent=44.0] - Mg(ClO4)2 salt concentration in wt% (5.0 to 55.0 wt%)
   * @param {number} [soilPorosity=0.30] - Regolith porosity (0.10 to 0.50)
   * @param {number} [soilTempK=230.0] - Subsurface soil temperature in K (190 to 280 K)
   * @param {number} [hydraulicGradient=0.35] - Downslope hydraulic gradient / slope tangent (0.01 to 0.80)
   * @returns {{liquidBrineState: boolean, brineViscosityCP: number, brineDensityKgM3: number, darcyHydraulicConductivityMS: number, poreSeepageVelocityCmPerDay: number, brineSaturatedThermalInertiaTIU: number, perchloratePhaseClass: string, perchlorateFlowContext: string}}
   */
  static computeMartianPerchlorateEutecticBrineDynamics(magnesiumPerchlorateWeightPercent = 44.0, soilPorosity = 0.30, soilTempK = 230.0, hydraulicGradient = 0.35) {
    const wPct = Math.max(5.0, Math.min(60.0, magnesiumPerchlorateWeightPercent));
    const phi = Math.max(0.05, Math.min(0.60, soilPorosity));
    const TK = Math.max(180.0, Math.min(300.0, soilTempK));
    const iGrad = Math.max(0.01, Math.min(1.0, hydraulicGradient));

    const TeutcK = 206.0; // Mg(ClO4)2 eutectic freezing point (206 K = -67.15 C)
    const isLiquid = TK >= TeutcK;

    // VTF viscosity model (Pa*s)
    const eta0 = 1.0e-4;
    const Bvtf = 650.0;
    const T0vtf = 145.0;
    const etaPaS = eta0 * Math.exp(Bvtf / Math.max(10.0, TK - T0vtf));
    const etaCP = etaPaS * 1000.0;

    // Brine density (kg/m^3)
    const rhoBrine = 1000.0 + (10.5 * wPct);
    const gMars = 3.72; // m/s^2

    // Darcy hydraulic conductivity (m/s)
    const kPermM2 = 1.0e-11; // Medium basaltic sand permeability
    const KConductivityMS = (kPermM2 * rhoBrine * gMars) / etaPaS;

    // Pore seepage velocity (cm/day)
    const vPoreMS = (KConductivityMS * iGrad) / phi;
    const vPoreCmDay = vPoreMS * 86400.0 * 100.0;

    // Brine-saturated thermal inertia
    const kTherm = (0.05 * (1.0 - phi)) + (0.55 * phi);
    const rhoBulk = (2900.0 * (1.0 - phi)) + (rhoBrine * phi);
    const Cspec = 1050.0;
    const TIU = Math.sqrt(kTherm * rhoBulk * Cspec);

    let phaseClass = 'Sub-Eutectic Frozen Ice-Perchlorate Aggregate';
    if (isLiquid) {
      if (vPoreCmDay >= 1.0) {
        phaseClass = 'Active Mobile Liquid Perchlorate Brine (Recurring Slope Lineae Flow / Palikir Crater)';
      } else {
        phaseClass = 'Viscous Cryogenic Subsurface Brine Seep (Phoenix Lander Soil Liquefaction)';
      }
    }

    return {
      liquidBrineState: isLiquid,
      brineViscosityCP: parseFloat(etaCP.toFixed(1)),
      brineDensityKgM3: parseFloat(rhoBrine.toFixed(1)),
      darcyHydraulicConductivityMS: parseFloat(KConductivityMS.toExponential(2)),
      poreSeepageVelocityCmPerDay: parseFloat(vPoreCmDay.toFixed(2)),
      brineSaturatedThermalInertiaTIU: parseFloat(TIU.toFixed(1)),
      perchloratePhaseClass: phaseClass,
      perchlorateFlowContext: `Mg(ClO4)2 Brine at ${TK.toFixed(0)}K (${vPoreCmDay.toFixed(1)} cm/day, ${etaCP.toFixed(0)} cP, TIU=${TIU.toFixed(0)}, ${phaseClass})`
    };
  }

  /**
   * Calculate South Polar Layered Deposits (SPLD) subglacial basal melting equilibrium, MARSIS radar reflector liquid lake stability, and thermal state.
   * T_base = T_surf + ( q_geo * H_ice ) / k_ice
   * T_melt = 273.15 - Delta_T_press - Delta_T_salinity
   * Reference: Orosei et al. (2018), Lauro et al. (2021), Sori & Bramson (2019) for Martian Subglacial Lakes.
   * @param {number} [iceSheetThicknessKm=1.5] - Polar ice sheet thickness in km (0.2 to 4.0 km)
   * @param {number} [surfaceTempK=160.0] - Mean polar surface temperature in K (140 to 190 K)
   * @param {number} [geothermalHeatFluxMWM2=75.0] - Geothermal heat flux in mW/m^2 (20 to 120 mW/m^2)
   * @param {number} [perchlorateSalinityGPerKg=300.0] - Dissolved perchlorate salt concentration in g/kg (0 to 450 g/kg)
   * @returns {{isBasalMeltingOccurring: boolean, basalTemperatureK: number, basalTemperatureC: number, basalMeltingPointK: number, basalThermalMarginK: number, subglacialHydrologyClass: string, subglacialLakeContext: string}}
   */
  static computeMartianSubglacialBasalMeltingEquilibrium(iceSheetThicknessKm = 1.5, surfaceTempK = 160.0, geothermalHeatFluxMWM2 = 75.0, perchlorateSalinityGPerKg = 300.0) {
    const HKm = Math.max(0.1, iceSheetThicknessKm);
    const TsurfK = Math.max(120.0, Math.min(220.0, surfaceTempK));
    const qGeomW = Math.max(10.0, geothermalHeatFluxMWM2);
    const Sppt = Math.max(0.0, Math.min(450.0, perchlorateSalinityGPerKg));

    const HM = HKm * 1000.0;
    const qGeoW = qGeomW / 1000.0; // W/m^2
    const kIce = 2.00; // W/(m K) effective ice-dust conductivity

    // Basal steady-state temperature
    const TbaseK = TsurfK + ((qGeoW * HM) / kIce);
    const TbaseC = TbaseK - 273.15;

    // Freezing point depression
    const deltaTpress = 0.074 * HKm; // K
    const deltaTsal = 0.22 * Sppt; // K depression from Mg/Ca perchlorates
    const TmeltK = 273.15 - deltaTpress - deltaTsal;

    // Basal margin
    const marginK = TbaseK - TmeltK;
    const isMelting = marginK >= 0.0;

    let hydroClass = 'Frozen Cold-Based Glacial Bed (Basal Radar Attenuation)';
    if (isMelting) {
      if (Sppt >= 200.0) {
        hydroClass = 'Stable Hypersaline Subglacial Liquid Water Lake (Planum Australe MARSIS Radar Anomaly)';
      } else {
        hydroClass = 'Active Basal Hydrothermal Melting & Subglacial Drainage Network';
      }
    } else if (marginK >= -10.0) {
      hydroClass = 'Near-Melting Polythermal Glacier Bed (Metastable Under Local Magmatic Plumes)';
    }

    return {
      isBasalMeltingOccurring: isMelting,
      basalTemperatureK: parseFloat(TbaseK.toFixed(2)),
      basalTemperatureC: parseFloat(TbaseC.toFixed(2)),
      basalMeltingPointK: parseFloat(TmeltK.toFixed(2)),
      basalThermalMarginK: parseFloat(marginK.toFixed(2)),
      subglacialHydrologyClass: hydroClass,
      subglacialLakeContext: `Subglacial Bed at ${HKm.toFixed(1)}km (T_base=${TbaseC.toFixed(1)} C, T_melt=${(TmeltK - 273.15).toFixed(1)} C, Margin=${marginK.toFixed(1)}K, ${hydroClass})`
    };
  }

  /**
   * Calculate impact/volcanic thermal dehydrogenation and oxidation kinetics of Fe(II)-smectite clays into nanophase hematite and thermally altered regolith.
   * k = A * exp( -E_a / ( R * T ) )
   * alpha = 1 - exp( -( k * t )^n )
   * w_npHm = alpha * w_clay * 0.22
   * Reference: Gavin et al. (2013), Chemtob et al. (2017), Morris et al. (2008) for Thermally Altered Martian Clays.
   * @param {number} [smectiteClayMassFraction=0.80] - Initial Fe-smectite mass fraction (0.10 to 1.0)
   * @param {number} [thermalPulseTempC=550.0] - Thermal baking / impact melt temperature in C (200 to 900 C)
   * @param {number} [durationHours=2.0] - Heating duration in hours (0.01 to 24.0 hours)
   * @returns {{dehydrogenationFraction: number, nanophaseHematiteWeightPercent: number, residualClayWeightPercent: number, alteredThermalInertiaTIU: number, thermalAlterationFaciesClass: string, thermalClayContext: string}}
   */
  static computeMartianClayThermalDehydrogenationKinetics(smectiteClayMassFraction = 0.80, thermalPulseTempC = 550.0, durationHours = 2.0) {
    const wClay = Math.max(0.05, Math.min(1.0, smectiteClayMassFraction));
    const TC = Math.max(100.0, Math.min(1100.0, thermalPulseTempC));
    const tHrs = Math.max(0.001, durationHours);

    const TK = TC + 273.15;
    const tSec = tHrs * 3600.0;
    const Rgas = 8.314;
    const Ea = 1.65e5; // 165 kJ/mol
    const A = 1.2e11; // 1/s
    const n = 1.5; // Avrami exponent

    // Kinetic rate constant (1/s)
    const kRate = A * Math.exp(-Ea / (Rgas * TK));

    // Reaction extent alpha (0 to 1)
    const kt = kRate * tSec;
    const alpha = 1.0 - Math.exp(-Math.pow(Math.min(25.0, kt), n));

    // Mineral mass fractions
    const wNpHmPct = alpha * wClay * 22.0; // wt% nanophase hematite
    const wResClayPct = (wClay * (1.0 - (0.22 * alpha))) * 100.0;
    const wHostPct = (1.0 - wClay) * 100.0;

    // Thermal inertia of baked clay-hematite aggregate
    const rhoGrain = ((wNpHmPct / 100.0) * 5260.0) + ((wResClayPct / 100.0) * 2600.0) + ((wHostPct / 100.0) * 2900.0);
    const phi = 0.18;
    const rhoBulk = rhoGrain * (1.0 - phi);
    const kTherm = (0.35 * (1.0 - phi)) + (0.90 * (wNpHmPct / 100.0));
    const Cspec = 820.0;
    const TIU = Math.sqrt(kTherm * rhoBulk * Cspec);

    let faciesClass = 'Unaltered Low-Temperature Smectite Clay';
    if (alpha >= 0.85) {
      faciesClass = 'High-Grade Thermally Dehydrogenated Red Clay / Nanophase Hematite Bloom (Impact Melt Sheet / Lava Contact)';
    } else if (alpha >= 0.25) {
      faciesClass = 'Partially Dehydroxylated Brown Smectite Clay (Sub-Magmatic Hydrothermal Aureole)';
    }

    return {
      dehydrogenationFraction: parseFloat(alpha.toFixed(3)),
      nanophaseHematiteWeightPercent: parseFloat(wNpHmPct.toFixed(1)),
      residualClayWeightPercent: parseFloat(wResClayPct.toFixed(1)),
      alteredThermalInertiaTIU: parseFloat(TIU.toFixed(1)),
      thermalAlterationFaciesClass: faciesClass,
      thermalClayContext: `Thermal Baking at ${TC.toFixed(0)} C (${(alpha * 100).toFixed(0)}% Dehydrogenated, ${wNpHmPct.toFixed(1)}% np-Hm, TIU=${TIU.toFixed(0)}, ${faciesClass})`
    };
  }

  /**
   * Calculate hydrothermal acid leaching kinetics of Fe/Mg-smectites, residual amorphous silica (Opal-A) precipitation, and kaolinitization.
   * r_leach = k_0 * 10^( -0.5 * pH ) * exp( -E_a / ( R * T ) )
   * alpha = 1 - exp( -r_leach * t )
   * w_silica = alpha * w_clay * 55.0
   * Reference: Squyres et al. (2008), Ruff et al. (2011), Ehlmann et al. (2009), Altheide et al. (2010) for Martian Opaline Sinters.
   * @param {number} [initialFeMgClayMassFraction=0.85] - Initial smectite clay fraction (0.10 to 1.0)
   * @param {number} [fluidPH=2.00] - Hydrothermal acid fluid pH (0.5 to 5.0)
   * @param {number} [fluidTempC=95.0] - Hydrothermal fluid temperature in C (20 to 250 C)
   * @param {number} [leachingDurationYears=250.0] - Active leaching duration in years (1.0 to 100000 years)
   * @returns {{leachingFraction: number, amorphousSilicaWeightPercent: number, kaoliniteWeightPercent: number, residualSmectiteWeightPercent: number, opalineSinterThermalInertiaTIU: number, hydrothermalSilicaFaciesClass: string, silicaParagenesisContext: string}}
   */
  static computeMartianAcidLeachingSilicaKaoliniteKinetics(initialFeMgClayMassFraction = 0.85, fluidPH = 2.00, fluidTempC = 95.0, leachingDurationYears = 250.0) {
    const wClay = Math.max(0.05, Math.min(1.0, initialFeMgClayMassFraction));
    const pH = Math.max(0.5, Math.min(6.0, fluidPH));
    const TC = Math.max(10.0, Math.min(300.0, fluidTempC));
    const tYr = Math.max(0.1, leachingDurationYears);

    const TK = TC + 273.15;
    const Rgas = 8.314;
    const Ea = 6.50e4; // 65 kJ/mol
    const k0 = 1.5e8; // 1/yr

    // Leaching rate constant (1/yr)
    const pHFactor = Math.pow(10.0, -0.5 * pH);
    const rLeach = k0 * pHFactor * Math.exp(-Ea / (Rgas * TK));

    // Leaching progress alpha (0 to 1)
    const exponent = Math.min(25.0, rLeach * tYr);
    const alpha = 1.0 - Math.exp(-exponent);

    // Secondary mineral mass fractions
    const wSilicaPct = alpha * wClay * 55.0; // wt% amorphous Opal-A silica
    const wKaolPct = alpha * wClay * 35.0; // wt% kaolinite
    const wResClayPct = (1.0 - alpha) * wClay * 100.0;
    const wHostPct = (1.0 - wClay) * 100.0;

    // Thermal inertia of porous opaline hydrothermal sinter
    const rhoGrain = ((wSilicaPct / 100.0) * 2100.0) + ((wKaolPct / 100.0) * 2600.0) + ((wResClayPct / 100.0) * 2600.0) + ((wHostPct / 100.0) * 2900.0);
    const phi = 0.38; // 38% opaline sinter porosity
    const rhoBulk = rhoGrain * (1.0 - phi);
    const kTherm = 0.12; // W/(m K) porous silica sinter
    const Cspec = 850.0; // J/(kg K)
    const TIU = Math.sqrt(kTherm * rhoBulk * Cspec);

    let faciesClass = 'Unaltered Smectite-Rich Bedrock';
    if (alpha >= 0.75 && wSilicaPct >= 35.0) {
      faciesClass = 'High-Purity Hydrated Opal-A Silica Sinter / Acid-Sulfate Leached Cap (Home Plate / Gusev Crater)';
    } else if (alpha >= 0.30) {
      faciesClass = 'Silicified Smectite-Kaolinite Hydrothermal Leached Residue';
    }

    return {
      leachingFraction: parseFloat(alpha.toFixed(3)),
      amorphousSilicaWeightPercent: parseFloat(wSilicaPct.toFixed(1)),
      kaoliniteWeightPercent: parseFloat(wKaolPct.toFixed(1)),
      residualSmectiteWeightPercent: parseFloat(wResClayPct.toFixed(1)),
      opalineSinterThermalInertiaTIU: parseFloat(TIU.toFixed(1)),
      hydrothermalSilicaFaciesClass: faciesClass,
      silicaParagenesisContext: `Acid Leaching pH ${pH.toFixed(1)} (${wSilicaPct.toFixed(0)}% Opal-A, ${wKaolPct.toFixed(0)}% Kaolinite, TIU=${TIU.toFixed(0)}, ${faciesClass})`
    };
  }

  /**
   * Calculate high-temperature contact metamorphic dehydroxylation kinetics of serpentine into regenerated forsterite olivine, enstatite pyroxene, and water vapor.
   * k = A * exp( -E_a / ( R * T ) )
   * alpha = 1 - exp( -k * t )
   * w_ol = alpha * w_serp * 51.0
   * Reference: Ehlmann et al. (2009, 2010), Amador et al. (2018), Brown et al. (2010) for Metamorphic Serpentine on Mars.
   * @param {number} [initialSerpentineMassFraction=0.80] - Initial serpentine mass fraction (0.10 to 1.0)
   * @param {number} [contactMeltTempC=700.0] - Thermal contact metamorphism temperature in C (300 to 1000 C)
   * @param {number} [durationHours=12.0] - Metamorphic heating duration in hours (0.1 to 1000 hours)
   * @returns {{dehydroxylationFraction: number, regeneratedOlivineWeightPercent: number, enstatitePyroxeneWeightPercent: number, releasedWaterVaporWeightPercent: number, recrystallizedThermalInertiaTIU: number, metamorphicFaciesClass: string, serpentineMetamorphicContext: string}}
   */
  static computeMartianSerpentineThermalDehydroxylationKinetics(initialSerpentineMassFraction = 0.80, contactMeltTempC = 700.0, durationHours = 12.0) {
    const wSerp = Math.max(0.05, Math.min(1.0, initialSerpentineMassFraction));
    const TC = Math.max(200.0, Math.min(1200.0, contactMeltTempC));
    const tHrs = Math.max(0.01, durationHours);

    const TK = TC + 273.15;
    const tSec = tHrs * 3600.0;
    const Rgas = 8.314;
    const Ea = 2.80e5; // 280 kJ/mol
    const A = 1.0e15; // 1/s

    // Kinetic rate constant (1/s)
    const kRate = A * Math.exp(-Ea / (Rgas * TK));

    // Reaction extent alpha (0 to 1)
    const kt = kRate * tSec;
    const alpha = 1.0 - Math.exp(-Math.min(25.0, kt));

    // Product yields
    const wOlPct = alpha * wSerp * 51.0; // wt% regenerated forsterite olivine
    const wPxPct = alpha * wSerp * 36.0; // wt% enstatite
    const wH2OPct = alpha * wSerp * 13.0; // wt% released H2O vapor
    const wResSerpPct = (1.0 - alpha) * wSerp * 100.0;
    const wHostPct = (1.0 - wSerp) * 100.0;

    // Thermal inertia of dense recrystallized contact metamorphic hornfels
    const rhoGrain = ((wOlPct / 100.0) * 3270.0) + ((wPxPct / 100.0) * 3200.0) + ((wResSerpPct / 100.0) * 2550.0) + ((wHostPct / 100.0) * 2900.0);
    const phi = 0.05 * (1.0 - (0.6 * alpha)); // Recrystallization closes pores
    const rhoBulk = rhoGrain * (1.0 - phi);
    const kTherm = (2.20 * (1.0 - phi)) + (1.20 * (wOlPct / 100.0));
    const Cspec = 850.0;
    const TIU = Math.sqrt(kTherm * rhoBulk * Cspec);

    let faciesClass = 'Unaltered Hydrothermal Serpentine';
    if (alpha >= 0.85) {
      faciesClass = 'High-Temperature Metamorphic Hornfels / Regenerated Forsterite Olivine (Impact Melt Sheet / Dike Contact)';
    } else if (alpha >= 0.25) {
      faciesClass = 'Partially Dehydroxylated Talc-Serpentine Contact Aureole';
    }

    return {
      dehydroxylationFraction: parseFloat(alpha.toFixed(3)),
      regeneratedOlivineWeightPercent: parseFloat(wOlPct.toFixed(1)),
      enstatitePyroxeneWeightPercent: parseFloat(wPxPct.toFixed(1)),
      releasedWaterVaporWeightPercent: parseFloat(wH2OPct.toFixed(1)),
      recrystallizedThermalInertiaTIU: parseFloat(TIU.toFixed(1)),
      metamorphicFaciesClass: faciesClass,
      serpentineMetamorphicContext: `Metamorphic Contact at ${TC.toFixed(0)} C (${(alpha * 100).toFixed(0)}% Dehydroxylated, ${wOlPct.toFixed(1)}% Recryst Olivine, TIU=${TIU.toFixed(0)}, ${faciesClass})`
    };
  }

  /**
   * Calculate subsurface methane clathrate hydrate (CH4 * 5.75H2O) phase equilibrium, Kim-Bishnoi dissociation kinetics, and atmospheric outgassing volume.
   * ln( P_eq / 1 kPa ) = 38.98 - 8533.8 / T
   * J_diss = k_d * exp( -E_a / ( R * T ) ) * ( P_eq - P_pore )
   * V_CH4 = phi * S_clathrate * 164.0
   * Reference: Chassefiere et al. (2013), Webster et al. (2015, 2021), Mousis et al. (2015) for Martian Clathrates.
   * @param {number} [burialDepthM=15.0] - Cryosphere burial depth in meters (0.5 to 2000 m)
   * @param {number} [subsurfaceTempK=225.0] - Cryosphere temperature in K (170 to 270 K)
   * @param {number} [porePressureKPa=0.60] - Ambient pore gas pressure in kPa (0.1 to 10000 kPa)
   * @param {number} [clathrateSaturationFraction=0.20] - Pore space clathrate saturation (0.01 to 0.90)
   * @returns {{isClathrateStable: boolean, equilibriumPressureKPa: number, dissociationDrivingForceKPa: number, methaneOutgassingVolumeM3PerM3: number, clathrateCryosphereThermalInertiaTIU: number, clathrateRegimeClass: string, clathrateStabilityContext: string}}
   */
  static computeMartianMethaneClathrateStabilityKinetics(burialDepthM = 15.0, subsurfaceTempK = 225.0, porePressureKPa = 0.60, clathrateSaturationFraction = 0.20) {
    const zM = Math.max(0.1, burialDepthM);
    const TK = Math.max(160.0, Math.min(273.15, subsurfaceTempK));
    const Ppore = Math.max(0.05, porePressureKPa);
    const Sclath = Math.max(0.01, Math.min(0.95, clathrateSaturationFraction));

    // Phase equilibrium pressure (kPa)
    const lnPeq = 38.98 - (8533.8 / TK);
    const PeqKPa = Math.exp(lnPeq);

    const isStable = Ppore >= PeqKPa;
    const deltaPKPa = Math.max(0.0, PeqKPa - Ppore);

    // Methane gas yield per m^3 soil (STP m^3)
    const phi = 0.35; // 35% regolith porosity
    const VgasM3 = phi * Sclath * 164.0; // 164 m^3 CH4 per m^3 clathrate

    // Thermal inertia of clathrate-cemented permafrost
    const kTherm = (0.55 * (1.0 - phi)) + (0.50 * phi * Sclath);
    const rhoBulk = (2800.0 * (1.0 - phi)) + (920.0 * phi * Sclath);
    const Cspec = 1200.0;
    const TIU = Math.sqrt(kTherm * rhoBulk * Cspec);

    let regimeClass = 'Stable Subsurface Methane Clathrate Cryosphere Reservoir';
    if (!isStable) {
      if (deltaPKPa >= 1.0) {
        regimeClass = 'Active Thermal Dissociation & Vigorous Methane Outgassing Plume (Gale Crater / Seasonal TLS Detection)';
      } else {
        regimeClass = 'Slow Metastable Clathrate Degassing / Microseepage';
      }
    }

    return {
      isClathrateStable: isStable,
      equilibriumPressureKPa: parseFloat(PeqKPa.toFixed(2)),
      dissociationDrivingForceKPa: parseFloat(deltaPKPa.toFixed(2)),
      methaneOutgassingVolumeM3PerM3: parseFloat(VgasM3.toFixed(2)),
      clathrateCryosphereThermalInertiaTIU: parseFloat(TIU.toFixed(1)),
      clathrateRegimeClass: regimeClass,
      clathrateStabilityContext: `CH4 Clathrate at ${TK.toFixed(0)}K (${VgasM3.toFixed(1)} m^3 CH4/m^3, ${regimeClass})`
    };
  }

  /**
   * Calculate post-impact hydrothermal convective circulation system lifetime, thermal cooling duration, water volume throughput, and habitability window.
   * t_hydro = 65000 * ( D_crater / 45 )^1.8 * ( k_perm / 1e-13 )^-0.35 * ( H_melt / 250 )^0.5
   * Reference: Abramov & Kring (2005), Rathbun & Squyres (2002), Barnhart et al. (2010), Schwenzer & Kring (2009) for Impact Hydrothermal Systems.
   * @param {number} [craterDiameterKm=45.0] - Impact crater diameter in km (5.0 to 200.0 km)
   * @param {number} [meltSheetThicknessM=250.0] - Impact melt sheet thickness in meters (20 to 2000 m)
   * @param {number} [hostRockPermeabilityM2=1.0e-13] - Fractured basalt host rock permeability in m^2 (1e-15 to 1e-11 m^2)
   * @param {number} [ambientSurfaceTempK=215.0] - Ambient surface temperature in K (180 to 260 K)
   * @returns {{hydrothermalLifetimeYears: number, activeVentingDurationYears: number, cumulativeWaterThroughputKm3: number, alteredBrecciaThermalInertiaTIU: number, hydrothermalHabitabilityClass: string, impactHydrothermalContext: string}}
   */
  static computeMartianImpactHydrothermalSystemLifetime(craterDiameterKm = 45.0, meltSheetThicknessM = 250.0, hostRockPermeabilityM2 = 1.0e-13, ambientSurfaceTempK = 215.0) {
    const DCrater = Math.max(2.0, craterDiameterKm);
    const HMelt = Math.max(10.0, meltSheetThicknessM);
    const kPerm = Math.max(1.0e-16, Math.min(1.0e-10, hostRockPermeabilityM2));
    const TsurfK = Math.max(150.0, Math.min(280.0, ambientSurfaceTempK));

    // Scaling law for total convective hydrothermal system lifetime (yr)
    const dFactor = Math.pow(DCrater / 45.0, 1.8);
    const kFactor = Math.pow(kPerm / 1.0e-13, -0.35);
    const hFactor = Math.pow(HMelt / 250.0, 0.5);
    const tTotalYrs = 65000.0 * dFactor * kFactor * hFactor;

    // High-temperature (> 150 C) active surface boiling / geyser venting phase (yr)
    const tVentingYrs = tTotalYrs * 0.18;

    // Cumulative hydrothermal fluid mass and volume throughput (km^3)
    const volThroughputKm3 = 450.0 * Math.pow(DCrater / 45.0, 2.2);

    // Thermal inertia of hydrothermal altered breccia / smectite-carbonate-silica core
    const kTherm = 1.65; // W/(m K)
    const rhoBulk = 2550.0; // kg/m^3
    const Cspec = 880.0; // J/(kg K)
    const TIU = Math.sqrt(kTherm * rhoBulk * Cspec);

    let habClass = 'Short-Lived Local Epithermal Seep (< 10 kyr)';
    if (tTotalYrs >= 100000.0) {
      habClass = 'Long-Lived Planetary Hydrothermal Habitable Oasis (> 100 kyr, Sustained Lake/Deep Biosphere in Jezero/Gale/Holden)';
    } else if (tTotalYrs >= 25000.0) {
      habClass = 'Substantial Post-Impact Hydrothermal System (25-100 kyr Habitable Warm Spring Window)';
    }

    return {
      hydrothermalLifetimeYears: parseFloat(tTotalYrs.toFixed(0)),
      activeVentingDurationYears: parseFloat(tVentingYrs.toFixed(0)),
      cumulativeWaterThroughputKm3: parseFloat(volThroughputKm3.toFixed(1)),
      alteredBrecciaThermalInertiaTIU: parseFloat(TIU.toFixed(1)),
      hydrothermalHabitabilityClass: habClass,
      impactHydrothermalContext: `Impact Hydrothermal System D=${DCrater.toFixed(0)}km (${(tTotalYrs / 1000).toFixed(0)} kyr lifetime, ${volThroughputKm3.toFixed(0)} km^3 H2O, ${habClass})`
    };
  }

  /**
   * Calculate burial diagenetic illitization kinetics of smectite clay into ordered mixed-layer illite-smectite (I/S) and illite.
   * 1/S - 1/S_0 = A * exp( -E_a / ( R * T ) ) * [K+]^0.5 * t
   * Reference: Ehlmann & Edwards (2014), Bristow et al. (2018), Cuadros et al. (2013) for Martian Clay Diagenesis.
   * @param {number} [initialSmectiteFraction=0.90] - Initial smectite layer fraction in clay (0.10 to 1.0)
   * @param {number} [burialTempC=120.0] - Geothermal / hydrothermal burial temperature in C (40 to 300 C)
   * @param {number} [poreFluidPotassiumMolar=0.05] - Pore fluid [K+] concentration in mol/L (0.001 to 2.0 M)
   * @param {number} [durationMyr=10.0] - Thermal duration in million years (0.01 to 500 Myr)
   * @returns {{illiteLayerFraction: number, residualSmectiteFraction: number, reichweiteOrderingClass: string, illitizedThermalInertiaTIU: number, metamorphicGradeClass: string, illitizationContext: string}}
   */
  static computeMartianSmectiteIllitizationKinetics(initialSmectiteFraction = 0.90, burialTempC = 120.0, poreFluidPotassiumMolar = 0.05, durationMyr = 10.0) {
    const S0 = Math.max(0.10, Math.min(1.0, initialSmectiteFraction));
    const TC = Math.max(20.0, Math.min(400.0, burialTempC));
    const kMolar = Math.max(0.0001, poreFluidPotassiumMolar);
    const tMyr = Math.max(0.001, durationMyr);

    const TK = TC + 273.15;
    const tSec = tMyr * 1.0e6 * 365.25 * 86400.0;
    const Rgas = 8.314;
    const Ea = 1.17e5; // 117 kJ/mol
    const A = 8.08e4; // M^-0.5 s^-1

    const kRate = A * Math.exp(-Ea / (Rgas * TK)) * Math.sqrt(kMolar);
    const invS = (1.0 / S0) + (kRate * tSec);
    const Scurr = Math.max(0.001, Math.min(S0, 1.0 / invS));
    const illiteFrac = 1.0 - Scurr;

    let reichweiteClass = 'Random R0 Mixed-Layer Illite-Smectite (< 50% Illite)';
    let gradeClass = 'Low-Temperature Diagenetic Smectite (Zeolite / Early Diagenesis Facies)';

    if (illiteFrac >= 0.85) {
      reichweiteClass = 'Highly Ordered R3 (ISII) Mixed-Layer / Pure Illite-Muscovite (> 85% Illite)';
      gradeClass = 'Anchizone to Epizone Low-Grade Metamorphic Horizon (Deep Noachian Basement)';
    } else if (illiteFrac >= 0.50) {
      reichweiteClass = 'Regularly Ordered R1 (IS) Mixed-Layer Illite-Smectite (50-85% Illite)';
      gradeClass = 'Mesodiagenetic Burial Horizon (T > 80-100 C, Gale Crater Deep Strata)';
    }

    // Thermal inertia of compacted illitized mudstone
    const phi = 0.15 * (1.0 - (0.6 * illiteFrac));
    const rhoBulk = 2600.0 * (1.0 - phi);
    const kTherm = (1.95 * (1.0 - phi)) + (0.45 * illiteFrac);
    const Cspec = 850.0;
    const TIU = Math.sqrt(kTherm * rhoBulk * Cspec);

    return {
      illiteLayerFraction: parseFloat(illiteFrac.toFixed(3)),
      residualSmectiteFraction: parseFloat(Scurr.toFixed(3)),
      reichweiteOrderingClass: reichweiteClass,
      illitizedThermalInertiaTIU: parseFloat(TIU.toFixed(1)),
      metamorphicGradeClass: gradeClass,
      illitizationContext: `Clay Illitization at ${TC.toFixed(0)} C (${(illiteFrac * 100).toFixed(1)}% Illite, ${reichweiteClass}, TIU=${TIU.toFixed(0)})`
    };
  }

  /**
   * Calculate hydrothermal / fumarolic acid sulfate weathering kinetics, pyrite oxidation, and jarosite/alunite paragenetic yield.
   * FeS2 + 3.5 O2 + H2O -> Fe2+ + 2 SO4(2-) + 2 H+
   * KFe3(SO4)2(OH)6 (Jarosite) + KAl3(SO4)2(OH)6 (Alunite) Precipitation
   * Reference: Squyres et al. (2004), Klingelhofer et al. (2004), Farrand et al. (2009), Ehlmann et al. (2016) for Acid Sulfate Alteration.
   * @param {number} [initialFeS2MassFraction=0.15] - Initial sulfide mass fraction in basalt (0.01 to 0.50)
   * @param {number} [solutionPH=1.8] - Acid pore fluid pH (0.5 to 5.0)
   * @param {number} [reactionTempC=65.0] - Reaction temperature in C (10 to 180 C)
   * @param {number} [durationYears=50.0] - Weathering duration in years (0.1 to 10000 yr)
   * @returns {{pyriteOxidationFraction: number, jarositePrecipitatedWeightPercent: number, alunitePrecipitatedWeightPercent: number, sulfateDuricrustThermalInertiaTIU: number, acidSulfateAlterationClass: string, acidWeatheringContext: string}}
   */
  static computeMartianAcidSulfateAluniteJarositeKinetics(initialFeS2MassFraction = 0.15, solutionPH = 1.8, reactionTempC = 65.0, durationYears = 50.0) {
    const wFeS2 = Math.max(0.01, Math.min(0.80, initialFeS2MassFraction));
    const pH = Math.max(0.2, Math.min(6.0, solutionPH));
    const TC = Math.max(0.0, Math.min(250.0, reactionTempC));
    const tYrs = Math.max(0.01, durationYears);

    const TK = TC + 273.15;
    const tSec = tYrs * 365.25 * 86400.0;
    const Rgas = 8.314;
    const Ea = 5.70e4; // 57 kJ/mol

    // Pyrite oxidation rate constant (1/s)
    const kRate = 2.5e4 * Math.exp(-Ea / (Rgas * TK)) * Math.pow(10.0, -0.11 * (pH - 2.0));
    const alphaOx = 1.0 - Math.exp(-Math.min(25.0, kRate * tSec));

    // Jarosite and Alunite precipitation yields (wt%)
    const wJarositePct = alphaOx * wFeS2 * 145.0; // wt% Jarosite
    const wAlunitePct = alphaOx * wFeS2 * 45.0; // wt% Alunite

    // Thermal inertia of cemented acid sulfate duricrust
    const phi = 0.18 * (1.0 - (0.4 * alphaOx));
    const rhoBulk = 2450.0 * (1.0 - phi);
    const kTherm = (1.25 * (1.0 - phi)) + (0.35 * (wJarositePct / 100.0));
    const Cspec = 880.0;
    const TIU = Math.sqrt(kTherm * rhoBulk * Cspec);

    let altClass = 'Mild Surface Acid Fog Oxidation';
    if (wJarositePct >= 10.0 && wAlunitePct >= 3.0) {
      altClass = 'Intense Fumarolic Acid Sulfate Alteration / Solfatara Horizon (Meridiani / Mawrth / Columbus)';
    } else if (wJarositePct >= 3.0) {
      altClass = 'Moderate Acid Groundwater Jarosite-Evaporite Duricrust';
    }

    return {
      pyriteOxidationFraction: parseFloat(alphaOx.toFixed(3)),
      jarositePrecipitatedWeightPercent: parseFloat(wJarositePct.toFixed(1)),
      alunitePrecipitatedWeightPercent: parseFloat(wAlunitePct.toFixed(1)),
      sulfateDuricrustThermalInertiaTIU: parseFloat(TIU.toFixed(1)),
      acidSulfateAlterationClass: altClass,
      acidWeatheringContext: `Acid Sulfate at pH ${pH.toFixed(1)}, ${TC.toFixed(0)} C (${wJarositePct.toFixed(1)}% Jarosite, ${wAlunitePct.toFixed(1)}% Alunite, TIU=${TIU.toFixed(0)}, ${altClass})`
    };
  }

  /**
   * Calculate 1D conductive cooling, Stefan moving-boundary crystallization, solidification time, and contact metamorphic aureole thickness of a basaltic magma sill.
   * t_solid = ( D_sill / 2 )^2 / ( 4 * lambda^2 * kappa )
   * W_halo = 0.85 * D_sill
   * Reference: Jaeger (1957), Turcotte & Schubert (2014), Michalski et al. (2017) for Subsurface Magmatism.
   * @param {number} [sillThicknessM=100.0] - Magma sill thickness in meters (10.0 to 1000.0 m)
   * @param {number} [intrusionTempC=1200.0] - Magma liquidus intrusion temperature in C (900 to 1400 C)
   * @param {number} [hostRockTempC=100.0] - Country rock ambient temperature in C (0 to 400 C)
   * @param {number} [latentHeatKJPerKg=400.0] - Latent heat of crystallization in kJ/kg (250 to 500 kJ/kg)
   * @returns {{solidificationTimeYears: number, metamorphicAureoleWidthMeters: number, totalCoolingToHostTempYears: number, crystallizedSillThermalInertiaTIU: number, intrusionRegimeClass: string, sillCoolingContext: string}}
   */
  static computeMartianBasalticSillCoolingSolidification(sillThicknessM = 100.0, intrusionTempC = 1200.0, hostRockTempC = 100.0, latentHeatKJPerKg = 400.0) {
    const D = Math.max(5.0, sillThicknessM);
    const Tint = Math.max(800.0, Math.min(1500.0, intrusionTempC));
    const Thost = Math.max(-50.0, Math.min(600.0, hostRockTempC));
    const L = Math.max(200.0, latentHeatKJPerKg);

    const kappa = 25.23; // Thermal diffusivity m^2/yr (8.0e-7 m^2/s)
    const lambda = 0.707; // Stefan solidification parameter

    // Time to complete core solidification (yr)
    const tSolidYrs = Math.pow(D / 2.0, 2.0) / (4.0 * Math.pow(lambda, 2.0) * kappa);

    // Total time to cool down to within 10% of host temperature (yr)
    const tTotalCoolYrs = tSolidYrs * 4.5;

    // Contact metamorphic halo width (pyrometamorphic/hornfels aureole)
    const wHaloM = 0.85 * D;

    // Thermal inertia of crystallized microgabbro / diabase sill
    const kTherm = 2.45; // W/(m K)
    const rhoBulk = 2950.0; // kg/m^3
    const Cspec = 950.0; // J/(kg K)
    const TIU = Math.sqrt(kTherm * rhoBulk * Cspec);

    let regimeClass = 'Minor Subsurface Dike / Thin Sheet Intrusion';
    if (D >= 200.0) {
      regimeClass = 'Major Subvolcanic Magma Chamber / Thick Plutonic Sill (Sustained Hydrothermal Engine in Syrtis Major/Elysium)';
    } else if (D >= 50.0) {
      regimeClass = 'Substantial Basaltic Sill / Sheet Complex (Decadal High-T Thermal Aureole)';
    }

    return {
      solidificationTimeYears: parseFloat(tSolidYrs.toFixed(1)),
      metamorphicAureoleWidthMeters: parseFloat(wHaloM.toFixed(1)),
      totalCoolingToHostTempYears: parseFloat(tTotalCoolYrs.toFixed(1)),
      crystallizedSillThermalInertiaTIU: parseFloat(TIU.toFixed(1)),
      intrusionRegimeClass: regimeClass,
      sillCoolingContext: `Basaltic Sill D=${D.toFixed(0)}m (${tSolidYrs.toFixed(1)} yr solidification, ${wHaloM.toFixed(0)}m halo, TIU=${TIU.toFixed(0)}, ${regimeClass})`
    };
  }

  /**
   * Calculate 1D thermal wave attenuation, diurnal/seasonal temperature damping, and microclimate stability inside a Martian volcanic lava tube cave.
   * delta_skin = sqrt( kappa * tau / pi )
   * Delta_T(z) = Delta_T_0 * exp( -z / delta_skin )
   * Reference: Williams et al. (2010), Cushing et al. (2007), Titus et al. (2021) for Martian Lava Tubes and Caves.
   * @param {number} [roofThicknessM=10.0] - Lava tube basalt roof thickness in meters (1.0 to 100.0 m)
   * @param {number} [surfaceDiurnalTempAmpK=45.0] - Surface diurnal temperature oscillation amplitude in K (10 to 80 K)
   * @param {number} [surfaceAnnualTempAmpK=25.0] - Surface seasonal temperature oscillation amplitude in K (5 to 50 K)
   * @param {number} [meanSurfaceTempK=210.0] - Mean surface temperature in K (150 to 260 K)
   * @returns {{diurnalSkinDepthMeters: number, annualSkinDepthMeters: number, interiorDiurnalAmplitudeK: number, interiorAnnualAmplitudeK: number, caveMinTempK: number, caveMaxTempK: number, basaltRoofThermalInertiaTIU: number, habitatMicroclimateClass: string, lavaTubeContext: string}}
   */
  static computeMartianLavaTubeThermalInsulation(roofThicknessM = 10.0, surfaceDiurnalTempAmpK = 45.0, surfaceAnnualTempAmpK = 25.0, meanSurfaceTempK = 210.0) {
    const zRoof = Math.max(0.5, roofThicknessM);
    const ampDiurnal = Math.max(0.0, surfaceDiurnalTempAmpK);
    const ampAnnual = Math.max(0.0, surfaceAnnualTempAmpK);
    const Tmean = Math.max(120.0, Math.min(280.0, meanSurfaceTempK));

    const kappa = 8.0e-7; // m^2/s thermal diffusivity of dense vesicular basalt
    const tauSolSec = 88775.0; // 1 Martian Sol in seconds
    const tauYrSec = 668.6 * tauSolSec; // 1 Martian Year in seconds

    // Skin depths (m)
    const deltaDiurnal = Math.sqrt((kappa * tauSolSec) / Math.PI);
    const deltaAnnual = Math.sqrt((kappa * tauYrSec) / Math.PI);

    // Damped amplitudes at cave roof ceiling depth (K)
    const caveAmpDiurnal = ampDiurnal * Math.exp(-zRoof / deltaDiurnal);
    const caveAmpAnnual = ampAnnual * Math.exp(-zRoof / deltaAnnual);

    const minTemp = Tmean - caveAmpAnnual - caveAmpDiurnal;
    const maxTemp = Tmean + caveAmpAnnual + caveAmpDiurnal;

    // Thermal inertia of basalt roof bedrock
    const kTherm = 1.50; // W/(m K)
    const rhoBulk = 2500.0; // kg/m^3
    const Cspec = 850.0; // J/(kg K)
    const TIU = Math.sqrt(kTherm * rhoBulk * Cspec);

    let habitClass = 'Thin Roof with Substantial Seasonal Temperature Fluctuation';
    if (zRoof >= 8.0) {
      habitClass = 'Ultra-Stable Thermal Oasis (Near-Zero Diurnal & < 2K Annual Fluctuation, Radiation Shielded Human Base Candidate)';
    } else if (zRoof >= 3.0) {
      habitClass = 'Thermally Damped Cave Environment (Zero Diurnal Fluctuation)';
    }

    return {
      diurnalSkinDepthMeters: parseFloat(deltaDiurnal.toFixed(3)),
      annualSkinDepthMeters: parseFloat(deltaAnnual.toFixed(3)),
      interiorDiurnalAmplitudeK: parseFloat(caveAmpDiurnal.toFixed(4)),
      interiorAnnualAmplitudeK: parseFloat(caveAmpAnnual.toFixed(2)),
      caveMinTempK: parseFloat(minTemp.toFixed(1)),
      caveMaxTempK: parseFloat(maxTemp.toFixed(1)),
      basaltRoofThermalInertiaTIU: parseFloat(TIU.toFixed(1)),
      habitatMicroclimateClass: habitClass,
      lavaTubeContext: `Lava Tube Roof ${zRoof.toFixed(0)}m (T_cave=${Tmean.toFixed(0)}+/-${caveAmpAnnual.toFixed(1)}K, TIU=${TIU.toFixed(0)}, ${habitClass})`
    };
  }

  /**
   * Calculate Fickian water vapor diffusion through porous regolith, sublimation lag desiccation front retreat rate, and thermal inertia contrast.
   * J_vapor = D_eff * ( P_sat - P_atm ) / ( R_spec * T * z_lag )
   * dz_dt = J_vapor / ( rho_ice * phi_ice )
   * Reference: Mellon et al. (2004, 2009), Schorghofer & Aharonson (2005), Dundas et al. (2018) for Martian Ground Ice.
   * @param {number} [lagThicknessM=0.10] - Porous desiccated dust/soil lag thickness in meters (0.01 to 5.0 m)
   * @param {number} [surfaceRelativeHumidity=0.20] - Near-surface atmospheric relative humidity (0.0 to 1.0)
   * @param {number} [groundTempK=210.0] - Ice table subsurface temperature in K (160 to 240 K)
   * @param {number} [regolithPorosity=0.40] - Regolith volumetric pore fraction (0.15 to 0.60)
   * @returns {{sublimationFluxKgPerM2S: number, iceRetreatRateMmPerYear: number, timeToRetreat1MeterKyr: number, desiccatedLagThermalInertiaTIU: number, iceCementedThermalInertiaTIU: number, cryosphericStabilityClass: string, iceLagContext: string}}
   */
  static computeMartianSubsurfaceIceLagDesiccationRate(lagThicknessM = 0.10, surfaceRelativeHumidity = 0.20, groundTempK = 210.0, regolithPorosity = 0.40) {
    const zLag = Math.max(0.005, lagThicknessM);
    const rh = Math.max(0.0, Math.min(1.0, surfaceRelativeHumidity));
    const TK = Math.max(140.0, Math.min(270.0, groundTempK));
    const phi = Math.max(0.10, Math.min(0.70, regolithPorosity));

    const Rspec = 461.5; // J/(kg K)
    const tauTort = 2.5; // Tortuosity
    const Dmol = 2.0e-4; // Molecular diffusion coefficient in low-pressure CO2 (m^2/s)
    const Deff = (phi / tauTort) * Dmol;

    // Saturated vapor pressure over ice (Pa)
    const Psat = Math.exp(28.87 - (6140.0 / TK));
    const Patm = rh * Psat;
    const deltaP = Math.max(0.0, Psat - Patm);

    // Sublimation mass flux (kg/(m^2 s))
    const Jvapor = (Deff * deltaP) / (Rspec * TK * zLag);

    // Ice front retreat rate (m/s -> mm/yr)
    const rhoIce = 920.0;
    const phiIce = phi;
    const dzDtMS = Jvapor / (rhoIce * phiIce);
    const dzDtMmYr = dzDtMS * (365.25 * 86400.0 * 1000.0);

    // Time to desiccate 1 meter of ice-rich ground (kyr)
    const t1mKyr = dzDtMmYr > 1e-8 ? 1000.0 / (dzDtMmYr * 1000.0) : 1e6;

    // Thermal inertia contrast
    const TIULag = Math.sqrt(0.025 * 1400.0 * 700.0);
    const TIUIce = Math.sqrt(2.10 * 1950.0 * 1200.0);

    let stabClass = 'Metastable Rapid Ice Sublimation Front (> 1 mm/yr)';
    if (dzDtMmYr < 0.05) {
      stabClass = 'Ultra-Stable Perennial Cryosphere (Millennial Ice Preservation in Utopia / Arcadia / Phoenix Site)';
    } else if (dzDtMmYr < 0.50) {
      stabClass = 'Slowly Retreating Ice Table Protected by Protective Sublimation Lag';
    }

    return {
      sublimationFluxKgPerM2S: parseFloat(Jvapor.toExponential(3)),
      iceRetreatRateMmPerYear: parseFloat(dzDtMmYr.toFixed(3)),
      timeToRetreat1MeterKyr: parseFloat(t1mKyr.toFixed(1)),
      desiccatedLagThermalInertiaTIU: parseFloat(TIULag.toFixed(1)),
      iceCementedThermalInertiaTIU: parseFloat(TIUIce.toFixed(1)),
      cryosphericStabilityClass: stabClass,
      iceLagContext: `Ice Table at ${TK.toFixed(0)}K under ${zLag.toFixed(2)}m Lag (${dzDtMmYr.toFixed(3)} mm/yr retreat, Lag TIU=${TIULag.toFixed(0)}, Ice TIU=${TIUIce.toFixed(0)}, ${stabClass})`
    };
  }

  /**
   * Calculate subsurface brine cryomagma chamber freezing, volumetric expansion overpressure, hydrofracture dike ascent, and cryovolcanic eruption threshold.
   * Delta_P = ( Delta_V / V ) / ( 1 / K_fluid + 3 / ( 4 * G_rock ) )
   * Reference: Quick et al. (2019), Lesage et al. (2020), Fagents (2003) for Planetary Cryovolcanism.
   * @param {number} [chamberRadiusM=250.0] - Cryomagma pocket spherical radius in meters (50 to 2000 m)
   * @param {number} [initialSalinityWtPct=15.0] - Brine salinity in wt% (1.0 to 30.0 wt%)
   * @param {number} [chamberDepthM=2500.0] - Burial depth beneath surface in meters (500 to 10000 m)
   * @param {number} [hostCryosphereTempK=210.0] - Ambient country rock cryosphere temperature in K (170 to 260 K)
   * @returns {{volumetricExpansionFraction: number, hydraulicOverpressureMPa: number, lithostaticStressMPa: number, isCryodikeErupting: boolean, frozenShellThermalInertiaTIU: number, cryovolcanicRegimeClass: string, cryochamberContext: string}}
   */
  static computeMartianCryochamberFreezingPressurization(chamberRadiusM = 250.0, initialSalinityWtPct = 15.0, chamberDepthM = 2500.0, hostCryosphereTempK = 210.0) {
    const Rch = Math.max(10.0, chamberRadiusM);
    const S0 = Math.max(0.5, Math.min(32.0, initialSalinityWtPct));
    const zCh = Math.max(100.0, chamberDepthM);
    const Thost = Math.max(150.0, Math.min(270.0, hostCryosphereTempK));

    // Fractional freezing crystallization to eutectic (fraction of liquid remaining)
    const chiEutectic = Math.max(0.10, Math.min(0.85, S0 / 30.0));
    const dVOverV = 0.09 * (1.0 - chiEutectic); // 9% volume expansion of pure water component

    const Kfluid = 2.2e9; // Pa
    const Grock = 8.0e9; // Pa

    // Hydrostatic / hydraulic overpressure build-up (Pa -> MPa)
    const deltaPPa = dVOverV / ((1.0 / Kfluid) + (3.0 / (4.0 * Grock)));
    const deltaPMPa = deltaPPa / 1.0e6;

    // Lithostatic overburden stress and failure criterion (MPa)
    const rhoRock = 2700.0;
    const gMars = 3.72;
    const sigmaLithMPa = (rhoRock * gMars * zCh) / 1.0e6;
    const sigmaTensileMPa = 5.0; // Basalt tensile strength
    const sigmaCritMPa = sigmaLithMPa + sigmaTensileMPa;

    const isErupting = deltaPMPa >= sigmaCritMPa;

    // Thermal inertia of crystallized eutectic salt-ice shell
    const kTherm = 2.25; // W/(m K)
    const rhoBulk = 1850.0; // kg/m^3
    const Cspec = 1350.0; // J/(kg K)
    const TIU = Math.sqrt(kTherm * rhoBulk * Cspec);

    let regimeClass = 'Contained Subsurface Cryointrusion (Stable Freezing without Surface Rupture)';
    if (isErupting) {
      if (deltaPMPa >= sigmaCritMPa * 2.0) {
        regimeClass = 'Catastrophic Cryovolcanic Dike Hydrofracturing & Explosive Effusive Cryolava Eruption (Cerberus Fossae / Occator-Scale)';
      } else {
        regimeClass = 'Active Cryovolcanic Dike Propagation & Surface Brine Spring Venting';
      }
    }

    return {
      volumetricExpansionFraction: parseFloat(dVOverV.toFixed(4)),
      hydraulicOverpressureMPa: parseFloat(deltaPMPa.toFixed(1)),
      lithostaticStressMPa: parseFloat(sigmaLithMPa.toFixed(1)),
      isCryodikeErupting: isErupting,
      frozenShellThermalInertiaTIU: parseFloat(TIU.toFixed(1)),
      cryovolcanicRegimeClass: regimeClass,
      cryochamberContext: `Cryomagma Chamber R=${Rch.toFixed(0)}m at z=${zCh.toFixed(0)}m (P_over=${deltaPMPa.toFixed(1)}MPa vs P_lith=${sigmaLithMPa.toFixed(1)}MPa, Erupting=${isErupting})`
    };
  }

  /**
   * Calculate hydrothermal serpentinization kinetics of ultramafic olivine, H2 generation, Fischer-Tropsch Type (FTT) methane yield, and rock thermal inertia.
   * 6 (Mg,Fe)2SiO4 + 7 H2O -> 3 Mg3Si2O5(OH)4 (Serpentine) + Fe3O4 (Magnetite) + H2
   * CO2 + 4 H2 -> CH4 + 2 H2O (FTT Methanogenesis)
   * Reference: Ehlmann et al. (2010), Oze & Sharma (2005), McCollom (2013), Etiope et al. (2013) for Martian Serpentinization.
   * @param {number} [olivineMassFraction=0.40] - Ultramafic olivine mass fraction in protolith (0.05 to 0.90)
   * @param {number} [reactionTempC=250.0] - Hydrothermal fluid temperature in C (50 to 400 C)
   * @param {number} [waterRockMassRatio=0.50] - Hydrothermal fluid/rock mass ratio (0.05 to 5.0)
   * @param {number} [durationYears=100.0] - Reaction duration in years (0.1 to 10000 yr)
   * @returns {{serpentinizationFraction: number, hydrogenYieldMolesPerKg: number, methaneYieldNmolPerKg: number, serpentinePrecipitatedWeightPercent: number, serpentinizedBasementThermalInertiaTIU: number, serpentinizationRegimeClass: string, serpentinizationContext: string}}
   */
  static computeMartianOlivineSerpentinizationMethaneYield(olivineMassFraction = 0.40, reactionTempC = 250.0, waterRockMassRatio = 0.50, durationYears = 100.0) {
    const wOl = Math.max(0.01, Math.min(0.95, olivineMassFraction));
    const TC = Math.max(20.0, Math.min(450.0, reactionTempC));
    const wrRatio = Math.max(0.01, Math.min(10.0, waterRockMassRatio));
    const tYrs = Math.max(0.01, durationYears);

    const TK = TC + 273.15;
    const tSec = tYrs * 365.25 * 86400.0;
    const Rgas = 8.314;
    const Ea = 6.80e4; // 68 kJ/mol

    // Temperature optimum bell-curve factor (peak serpentinization around 250-300 C)
    const tempBell = Math.exp(-Math.pow(TC - 260.0, 2.0) / (2.0 * Math.pow(60.0, 2.0)));
    const kRate = 1.5e-1 * Math.exp(-Ea / (Rgas * TK)) * tempBell * Math.min(2.0, wrRatio);

    const alphaSerp = 1.0 - Math.exp(-Math.min(20.0, kRate * tSec));

    // Hydrogen and Serpentine yield
    const molesH2PerKg = alphaSerp * wOl * 0.052; // mol H2 / kg rock
    const wSerpPct = alphaSerp * wOl * 115.0; // wt% Serpentine precipitated

    // Fischer-Tropsch Type (FTT) Methane synthesis (Sabatier reaction with dissolved CO2)
    const fttEfficiency = 0.065 * tempBell;
    const nmolCH4PerKg = (molesH2PerKg / 4.0) * fttEfficiency * 1.0e9;

    // Thermal inertia of serpentinized basement rock
    const kTherm = 2.10; // W/(m K)
    const rhoBulk = 2600.0; // kg/m^3
    const Cspec = 1050.0; // J/(kg K)
    const TIU = Math.sqrt(kTherm * rhoBulk * Cspec);

    let regClass = 'Low-Temperature Sluggish Serpentinization';
    if (alphaSerp >= 0.60 && nmolCH4PerKg >= 1.0e4) {
      regClass = 'Active High-Yield Hydrothermal Serpentinization & Methanogenesis Engine (Nili Fossae / Claritas Fossae Analogue)';
    } else if (alphaSerp >= 0.20) {
      regClass = 'Moderate Hydrothermal Olivine Carbonation and Serpentinization';
    }

    return {
      serpentinizationFraction: parseFloat(alphaSerp.toFixed(3)),
      hydrogenYieldMolesPerKg: parseFloat(molesH2PerKg.toFixed(4)),
      methaneYieldNmolPerKg: parseFloat(nmolCH4PerKg.toFixed(1)),
      serpentinePrecipitatedWeightPercent: parseFloat(wSerpPct.toFixed(1)),
      serpentinizedBasementThermalInertiaTIU: parseFloat(TIU.toFixed(1)),
      serpentinizationRegimeClass: regClass,
      serpentinizationContext: `Serpentinization at ${TC.toFixed(0)} C (${(alphaSerp * 100).toFixed(1)}% reacted, ${nmolCH4PerKg.toFixed(0)} nmol CH4/kg, TIU=${TIU.toFixed(0)}, ${regClass})`
    };
  }

  /**
   * Calculate thermal dissociation of subsurface methane clathrate hydrate driven by magmatic heating, outgassing flux, and atmospheric plume release.
   * v_diss = ( q_magma - q_bg ) / ( rho_clath * Delta_H_diss )
   * F_CH4 = v_diss * rho_CH4_cage
   * Reference: Chassefiere et al. (2013), Webster et al. (2015, 2021), Mousis et al. (2015) for Martian Methane Plumes.
   * @param {number} [clathrateThicknessM=100.0] - Clathrate hydrate layer thickness in meters (10 to 1000 m)
   * @param {number} [geothermalHeatFluxMwM2=150.0] - Magmatic / geothermal basal heat flux in mW/m^2 (40 to 500 mW/m^2)
   * @param {number} [initialClathrateTempK=220.0] - Pre-heating clathrate stability zone temperature in K (180 to 260 K)
   * @param {number} [durationYears=50.0] - Heating duration in years (0.1 to 1000 yr)
   * @returns {{dissociationRateMmPerYear: number, dissociatedLayerThicknessMeters: number, methaneFluxKgPerM2S: number, dailySeepageKgPerSol100Km2: number, dissociatedSpongeThermalInertiaTIU: number, methanePlumeRegimeClass: string, clathrateContext: string}}
   */
  static computeMartianClathrateHydrateDissociationPlume(clathrateThicknessM = 100.0, geothermalHeatFluxMwM2 = 150.0, initialClathrateTempK = 220.0, durationYears = 50.0) {
    const Hclath = Math.max(5.0, clathrateThicknessM);
    const qMw = Math.max(35.0, geothermalHeatFluxMwM2);
    const TK = Math.max(160.0, Math.min(270.0, initialClathrateTempK));
    const tYrs = Math.max(0.01, durationYears);

    const qBgW = 0.030; // 30 mW/m^2 background crustal heat flux
    const qMagmaW = qMw / 1000.0;
    const deltaQ = Math.max(0.005, qMagmaW - qBgW); // W/m^2

    const rhoClath = 910.0; // kg/m^3
    const deltaHDiss = 4.5e5; // J/kg clathrate

    // Dissociation front velocity (m/s -> mm/yr)
    const vDissMS = deltaQ / (rhoClath * deltaHDiss);
    const vDissMmYr = vDissMS * (365.25 * 86400.0 * 1000.0);

    // Total dissociated thickness in duration (m)
    const zDissM = Math.min(Hclath, (vDissMmYr / 1000.0) * tYrs);

    // Methane release flux (kg/(m^2 s))
    const rhoCH4Cage = 117.0; // kg CH4 / m^3 clathrate
    const JCH4 = vDissMS * rhoCH4Cage;

    // Daily seepage over 100 km^2 area (kg CH4 / sol)
    const solSec = 88775.0;
    const areaM2 = 1.0e8; // 100 km^2
    const QSolKg = JCH4 * areaM2 * solSec;

    // Thermal inertia of dissociated porous cryo-sponge
    const kTherm = 0.45; // W/(m K)
    const rhoBulk = 1600.0; // kg/m^3
    const Cspec = 780.0; // J/(kg K)
    const TIU = Math.sqrt(kTherm * rhoBulk * Cspec);

    let regClass = 'Minor Subsurface Clathrate Outgassing';
    if (QSolKg >= 200.0) {
      regClass = 'Active Magmatically-Driven Methane Plume Outburst (TLS Curiosity / PFS Atmospheric Spikes in Gale / Nili Fossae)';
    } else if (QSolKg >= 50.0) {
      regClass = 'Moderate Chronic Fault Seepage & Micro-Seepage Regime';
    }

    return {
      dissociationRateMmPerYear: parseFloat(vDissMmYr.toFixed(3)),
      dissociatedLayerThicknessMeters: parseFloat(zDissM.toFixed(3)),
      methaneFluxKgPerM2S: parseFloat(JCH4.toExponential(3)),
      dailySeepageKgPerSol100Km2: parseFloat(QSolKg.toFixed(1)),
      dissociatedSpongeThermalInertiaTIU: parseFloat(TIU.toFixed(1)),
      methanePlumeRegimeClass: regClass,
      clathrateContext: `Clathrate Dissociation at ${qMw.toFixed(0)} mW/m^2 (${vDissMmYr.toFixed(2)} mm/yr, ${QSolKg.toFixed(0)} kg CH4/sol per 100km2, TIU=${TIU.toFixed(0)}, ${regClass})`
    };
  }

  /**
   * Calculate hydrothermal silica sinter precipitation kinetics, supersaturation upon cooling, mound accretion rate, and deposit thermal inertia.
   * log10( C_sat ) = 4.52 - 731 / T_K
   * M_sinter = Q_fluid * ( C_SiO2 - C_sat )
   * Reference: Rimstidt & Barnes (1980), Ruff et al. (2011), Skok et al. (2010), Squyres et al. (2008) for Martian Opaline Sinters.
   * @param {number} [dissolvedSilicaPpm=450.0] - Hydrothermal fluid dissolved silica in ppm (50 to 1200 ppm)
   * @param {number} [dischargeTempC=120.0] - Thermal spring orifice temperature in C (40 to 300 C)
   * @param {number} [ambientSurfaceTempC=0.0] - Ambient surface environment temperature in C (-50 to 50 C)
   * @param {number} [dischargeRateKgPerS=5.0] - Hydrothermal spring mass discharge in kg/s (0.1 to 100 kg/s)
   * @returns {{supersaturationRatio: number, annualSilicaYieldTonnesPerYear: number, sinterMoundAccretionRateMmPerYear: number, opalineSinterThermalInertiaTIU: number, silicaHydrothermalClass: string, sinterContext: string}}
   */
  static computeMartianHydrothermalSilicificationSinterPrecipitation(dissolvedSilicaPpm = 450.0, dischargeTempC = 120.0, ambientSurfaceTempC = 0.0, dischargeRateKgPerS = 5.0) {
    const cSiO2 = Math.max(10.0, dissolvedSilicaPpm);
    const TdisC = Math.max(20.0, Math.min(350.0, dischargeTempC));
    const TambC = Math.max(-60.0, Math.min(60.0, ambientSurfaceTempC));
    const qKgS = Math.max(0.01, dischargeRateKgPerS);

    const TambK = TambC + 273.15;

    // Amorphous silica solubility at ambient temperature (ppm)
    const logCsat = 4.52 - (731.0 / TambK);
    const cSatPpm = Math.pow(10.0, logCsat);

    // Supersaturation ratio
    const sRatio = cSiO2 / cSatPpm;
    const deltaCPpm = Math.max(0.0, cSiO2 - cSatPpm);

    // Annual silica mass precipitated (tonnes/yr)
    const secPerYear = 365.25 * 86400.0;
    const mSinterKgYr = qKgS * (deltaCPpm * 1.0e-6) * secPerYear;
    const mSinterTonnesYr = mSinterKgYr / 1000.0;

    // Sinter mound vertical accumulation rate over 250 m^2 vent apron (mm/yr)
    const rhoSinter = 1900.0; // kg/m^3
    const ventAreaM2 = 250.0;
    const dzDtMmYr = (mSinterKgYr / (rhoSinter * ventAreaM2)) * 1000.0;

    // Thermal inertia of cemented opaline silica sinter
    const kTherm = 1.85; // W/(m K)
    const rhoBulk = 2100.0; // kg/m^3
    const Cspec = 850.0; // J/(kg K)
    const TIU = Math.sqrt(kTherm * rhoBulk * Cspec);

    let hydClass = 'Sub-Saturated Thermal Spring (No Sinter Precipitation)';
    if (sRatio >= 3.0 && mSinterTonnesYr >= 10.0) {
      hydClass = 'Vigorous Silica Sinter-Building Geyser / Hot Spring Apron (Gusev Home Plate / Nili Patera Analogue)';
    } else if (sRatio >= 1.2) {
      hydClass = 'Moderate Opaline Sinter Precipitation & Bedrock Silicification';
    }

    return {
      supersaturationRatio: parseFloat(sRatio.toFixed(2)),
      annualSilicaYieldTonnesPerYear: parseFloat(mSinterTonnesYr.toFixed(1)),
      sinterMoundAccretionRateMmPerYear: parseFloat(dzDtMmYr.toFixed(1)),
      opalineSinterThermalInertiaTIU: parseFloat(TIU.toFixed(1)),
      silicaHydrothermalClass: hydClass,
      sinterContext: `Silica Sinter (${sRatio.toFixed(1)}x supersat, ${mSinterTonnesYr.toFixed(1)} t/yr SiO2, ${dzDtMmYr.toFixed(1)} mm/yr accretion, TIU=${TIU.toFixed(0)}, ${hydClass})`
    };
  }

  /**
   * Calculate hydrothermal zeolitization kinetics of volcanic glass, bound crystal water sequestration, thermal dehydration, and rock thermal inertia.
   * Volcanic Glass + Alkaline Hydrothermal Fluid (pH 8.5-10.5) -> Analcime / Clinoptilolite + Smectite
   * Reference: Ehlmann et al. (2009, 2011), Wray et al. (2016), Viviano-Beck et al. (2014) for Martian Zeolites.
   * @param {number} [volcanicGlassFraction=0.50] - Protolith volcanic glass fraction (0.05 to 0.95)
   * @param {number} [hydrothermalFluidTempC=140.0] - Fluid alteration temperature in C (40 to 300 C)
   * @param {number} [fluidPh=9.5] - Fluid pH (7.0 to 12.0)
   * @param {number} [durationYears=200.0] - Reaction duration in years (0.1 to 10000 yr)
   * @returns {{zeolitizationFraction: number, boundWaterWeightPercent: number, isDehydrating: boolean, zeolitizedTuffThermalInertiaTIU: number, zeoliteAlterationClass: string, zeoliteContext: string}}
   */
  static computeMartianZeoliteHydrothermalAlterationDehydration(volcanicGlassFraction = 0.50, hydrothermalFluidTempC = 140.0, fluidPh = 9.5, durationYears = 200.0) {
    const wGlass = Math.max(0.01, Math.min(0.95, volcanicGlassFraction));
    const TC = Math.max(20.0, Math.min(350.0, hydrothermalFluidTempC));
    const pH = Math.max(6.0, Math.min(13.0, fluidPh));
    const tYrs = Math.max(0.01, durationYears);

    const TK = TC + 273.15;
    const tSec = tYrs * 365.25 * 86400.0;
    const Rgas = 8.314;
    const Ea = 5.50e4; // 55 kJ/mol

    // Kinetic dissolution-precipitation rate of zeolite formation
    const pHFactor = Math.pow(10.0, (pH - 7.0) * 0.30);
    const kRate = 2.0e-3 * Math.exp(-Ea / (Rgas * TK)) * pHFactor;

    const alphaZeo = 1.0 - Math.exp(-Math.min(20.0, kRate * tSec));

    // Bound structural H2O in zeolite channels (wt%)
    const wBoundH2O = alphaZeo * wGlass * 12.5;

    // Thermal dehydration threshold (T >= 180 C causes lattice collapse & water vaporization)
    const isDehyd = TC >= 180.0;

    // Thermal inertia of zeolitized volcanic tuff
    const kTherm = 1.10; // W/(m K)
    const rhoBulk = 1750.0; // kg/m^3
    const Cspec = 900.0; // J/(kg K)
    const TIU = Math.sqrt(kTherm * rhoBulk * Cspec);

    let altClass = 'Incipient Glass Hydration / Slow Zeolitization';
    if (isDehyd) {
      altClass = 'High-Temperature Metamorphic Dehydration (Wairakite / Feldspar Metasomatism)';
    } else if (alphaZeo >= 0.60 && pH >= 8.5) {
      altClass = 'Pervasive Alkaline Hydrothermal Zeolitization (Analcime / Clinoptilolite in Mawrth / Terby Crater)';
    } else if (alphaZeo >= 0.20) {
      altClass = 'Moderate Zeolitic Alteration of Volcanic Ash';
    }

    return {
      zeolitizationFraction: parseFloat(alphaZeo.toFixed(3)),
      boundWaterWeightPercent: parseFloat(wBoundH2O.toFixed(2)),
      isDehydrating: isDehyd,
      zeolitizedTuffThermalInertiaTIU: parseFloat(TIU.toFixed(1)),
      zeoliteAlterationClass: altClass,
      zeoliteContext: `Zeolite at ${TC.toFixed(0)} C, pH ${pH.toFixed(1)} (${(alphaZeo * 100).toFixed(1)}% altered, ${wBoundH2O.toFixed(1)}% H2O, TIU=${TIU.toFixed(0)}, ${altClass})`
    };
  }

  /**
   * Calculate hydrothermal/burial diagenesis kinetics of smectite-to-illite conversion, interlayer water expulsion, and consolidated shale thermal inertia.
   * Smectite + K+ + Al3+ -> Illite + Quartz + H2O (Interlayer Water Release)
   * Reference: Eberl & Hower (1976), Huang et al. (1993), Tosca et al. (2008), Ehlmann et al. (2011) for Martian Clay Mineral Diagenesis.
   * @param {number} [initialSmectiteFraction=1.0] - Initial expandable smectite fraction in clay matrix (0.10 to 1.0)
   * @param {number} [burialTempC=130.0] - Deep burial or hydrothermal fluid temperature in C (40 to 300 C)
   * @param {number} [poreFluidPotassiumPpm=250.0] - Pore fluid K+ ion concentration in ppm (10 to 2000 ppm)
   * @param {number} [durationKyr=100.0] - Diagenesis duration in kiloyears (0.1 to 10000 kyr)
   * @returns {{illiteFractionInClay: number, smectiteFractionRemaining: number, expelledInterlayerWaterWeightPercent: number, illiticShaleThermalInertiaTIU: number, clayDiagenesisGradeClass: string, diagenesisContext: string}}
   */
  static computeMartianSmectiteIlliteDiagenesisKinetics(initialSmectiteFraction = 1.0, burialTempC = 130.0, poreFluidPotassiumPpm = 250.0, durationKyr = 100.0) {
    const S0 = Math.max(0.05, Math.min(1.0, initialSmectiteFraction));
    const TC = Math.max(20.0, Math.min(350.0, burialTempC));
    const kPpm = Math.max(5.0, poreFluidPotassiumPpm);
    const tKyr = Math.max(0.01, durationKyr);

    const TK = TC + 273.15;
    const tYrs = tKyr * 1000.0;
    const Rgas = 8.314;
    const Ea = 7.80e4; // 78 kJ/mol for hydrothermal potassium fixation

    // 2nd-order kinetic rate constant of illitization
    const kEff = 1.5e6 * Math.exp(-Ea / (Rgas * TK)) * Math.pow(kPpm / 100.0, 0.25);

    // Smectite remaining and Illite formed
    const SRemain = S0 / (1.0 + (kEff * tYrs * S0));
    const IFraction = S0 - SRemain;
    const illitePercentInClay = (IFraction / S0) * 100.0;

    // Expelled interlayer pore water (wt% of clay sediment)
    const wExpelledH2O = (illitePercentInClay / 100.0) * 10.5;

    // Thermal inertia of compacted illitized claystone/shale
    const kTherm = 1.65; // W/(m K)
    const rhoBulk = 2200.0; // kg/m^3
    const Cspec = 880.0; // J/(kg K)
    const TIU = Math.sqrt(kTherm * rhoBulk * Cspec);

    let diagClass = 'Unaltered Expandable Smectite Clay Matrix (< 20% Illite)';
    if (illitePercentInClay >= 80.0) {
      diagClass = 'Deep Anchizone Metamorphic / High-Grade Illite Shale (Overpressure Hydrofracturing & Dehydroxylation)';
    } else if (illitePercentInClay >= 50.0) {
      diagClass = 'Mixed-Layer Illite/Smectite (I/S) Ordered Interstratified Diagenetic Clay';
    } else if (illitePercentInClay >= 20.0) {
      diagClass = 'Incipient Randomly Interstratified Illite/Smectite (I/S)';
    }

    return {
      illiteFractionInClay: parseFloat((illitePercentInClay / 100.0).toFixed(3)),
      smectiteFractionRemaining: parseFloat((SRemain / S0).toFixed(3)),
      expelledInterlayerWaterWeightPercent: parseFloat(wExpelledH2O.toFixed(2)),
      illiticShaleThermalInertiaTIU: parseFloat(TIU.toFixed(1)),
      clayDiagenesisGradeClass: diagClass,
      diagenesisContext: `Clay Diagenesis at ${TC.toFixed(0)} C (${illitePercentInClay.toFixed(1)}% Illite in ${tKyr.toFixed(0)} kyr, ${wExpelledH2O.toFixed(1)}% H2O expelled, TIU=${TIU.toFixed(0)}, ${diagClass})`
    };
  }

  /**
   * Calculate hydrothermal metamorphism kinetics of kaolinite to dickite/pyrophyllite, silica metasomatism, dehydroxylation water release, and hornfels thermal inertia.
   * Al2Si2O5(OH)4 (Kaolinite) + 2 SiO2(aq) -> Al2Si4O10(OH)2 (Pyrophyllite) + H2O
   * Reference: Hemley et al. (1980), Ehlmann et al. (2009), Marzo et al. (2010), Sun & Milliken (2015) for High-Temperature Martian Hydrothermal Phyllosilicates.
   * @param {number} [kaoliniteFraction=0.50] - Initial kaolinite mass fraction in host protolith (0.05 to 0.95)
   * @param {number} [hydrothermalTempC=260.0] - Hydrothermal fluid temperature in C (100 to 400 C)
   * @param {number} [dissolvedSilicaActivity=1.20] - Dissolved silica chemical activity (0.1 to 3.0)
   * @param {number} [durationYears=500.0] - Hydrothermal circulation duration in years (0.1 to 10000 yr)
   * @returns {{pyrophylliteConversionFraction: number, dickitePolymorphFraction: number, dehydroxylationWaterWeightPercent: number, metamorphicHornfelsThermalInertiaTIU: number, hydrothermalMetamorphismClass: string, metamorphismContext: string}}
   */
  static computeMartianKaolinitePyrophylliteHydrothermalMetamorphism(kaoliniteFraction = 0.50, hydrothermalTempC = 260.0, dissolvedSilicaActivity = 1.20, durationYears = 500.0) {
    const wKaol = Math.max(0.01, Math.min(0.95, kaoliniteFraction));
    const TC = Math.max(50.0, Math.min(450.0, hydrothermalTempC));
    const aSiO2 = Math.max(0.05, Math.min(5.0, dissolvedSilicaActivity));
    const tYrs = Math.max(0.01, durationYears);

    const TK = TC + 273.15;
    const tSec = tYrs * 365.25 * 86400.0;
    const Rgas = 8.314;
    const Ea = 6.20e4; // 62 kJ/mol for pyrophyllitization

    // Reaction rate constant
    const kRate = 5.0e-3 * Math.exp(-Ea / (Rgas * TK)) * Math.sqrt(aSiO2);
    const alphaPyro = 1.0 - Math.exp(-Math.min(20.0, kRate * tSec));

    // Polymorph partitioning: Dickite/Nacrite forms between 180-250 C, Pyrophyllite dominates > 250 C with high silica
    let alphaDickite = 0.0;
    if (TC >= 180.0 && TC < 280.0) {
      alphaDickite = (1.0 - alphaPyro) * 0.75;
    }

    // Water released during pyrophyllitization (wt% of host rock)
    const wWaterReleased = alphaPyro * wKaol * 6.98;

    // Thermal inertia of dense silicified pyrophyllite hornfels
    const kTherm = 2.45; // W/(m K)
    const rhoBulk = 2450.0; // kg/m^3
    const Cspec = 890.0; // J/(kg K)
    const TIU = Math.sqrt(kTherm * rhoBulk * Cspec);

    let metaClass = 'Low-Temperature Sedimentary Kaolinite Regolith (T < 180 C)';
    if (alphaPyro >= 0.60 && TC >= 250.0) {
      metaClass = 'High-Temperature Hydrothermal Pyrophyllite-Quartz Hornfels (Toro Crater / Nili Fossae Central Peaks)';
    } else if (TC >= 180.0) {
      metaClass = 'Moderate Hydrothermal Dickite / Nacrite High-Temperature Kaolin Polymorph Alteration';
    }

    return {
      pyrophylliteConversionFraction: parseFloat(alphaPyro.toFixed(3)),
      dickitePolymorphFraction: parseFloat(alphaDickite.toFixed(3)),
      dehydroxylationWaterWeightPercent: parseFloat(wWaterReleased.toFixed(2)),
      metamorphicHornfelsThermalInertiaTIU: parseFloat(TIU.toFixed(1)),
      hydrothermalMetamorphismClass: metaClass,
      metamorphismContext: `Kaolin Metamorphism at ${TC.toFixed(0)} C (${(alphaPyro * 100).toFixed(1)}% Pyrophyllite, ${(alphaDickite * 100).toFixed(1)}% Dickite, TIU=${TIU.toFixed(0)}, ${metaClass})`
    };
  }

  /**
   * Calculate acid sulfate hydrothermal weathering kinetics, alunite vs jarosite speciation, and bleached basalt thermal inertia.
   * FeS2 + O2 + H2O -> H2SO4; Basalt + H2SO4 -> Alunite (T > 140 C) / Jarosite (T < 120 C)
   * Reference: Swayze et al. (2008), Ehlmann et al. (2011), Sowe et al. (2012), Thollot et al. (2012) for Martian Acid-Sulfate Alteration.
   * @param {number} [sulfideMassFraction=0.15] - Sulfide/pyrite mass fraction in protolith (0.01 to 0.50)
   * @param {number} [hydrothermalTempC=180.0] - Fluid alteration temperature in C (20 to 350 C)
   * @param {number} [phLevel=2.0] - Hyperacidic pore fluid pH (0.5 to 5.0)
   * @param {number} [durationYears=100.0] - Hydrothermal alteration duration in years (0.1 to 5000 yr)
   * @returns {{alterationFraction: number, sulfatePrecipitatedWeightPercent: number, dominantSulfateSpecies: string, acidSulfateThermalInertiaTIU: number, acidHydrothermalClass: string, acidSulfateContext: string}}
   */
  static computeMartianAcidSulfateAluniteJarositeWeathering(sulfideMassFraction = 0.15, hydrothermalTempC = 180.0, phLevel = 2.0, durationYears = 100.0) {
    const wSulf = Math.max(0.005, Math.min(0.80, sulfideMassFraction));
    const TC = Math.max(10.0, Math.min(400.0, hydrothermalTempC));
    const pH = Math.max(0.2, Math.min(6.0, phLevel));
    const tYrs = Math.max(0.01, durationYears);

    const TK = TC + 273.15;
    const tSec = tYrs * 365.25 * 86400.0;
    const Rgas = 8.314;
    const Ea = 5.80e4; // 58 kJ/mol

    // Kinetic leaching rate
    const acidFactor = Math.pow(10.0, (4.0 - pH) * 0.40);
    const kRate = 3.5e-3 * Math.exp(-Ea / (Rgas * TK)) * acidFactor;

    const alphaSulf = 1.0 - Math.exp(-Math.min(20.0, kRate * tSec));

    // Sulfate precipitate mass fraction (wt% of altered rock)
    const wSulfatePct = alphaSulf * wSulf * 2.80 * 100.0;

    // Thermal inertia of bleached porous sulfate rock
    const kTherm = 1.35; // W/(m K)
    const rhoBulk = 1950.0; // kg/m^3
    const Cspec = 850.0; // J/(kg K)
    const TIU = Math.sqrt(kTherm * rhoBulk * Cspec);

    let species = 'Basaltic Matrix';
    let altClass = 'Incipient Acid Leaching';

    if (TC >= 140.0 && pH <= 3.5) {
      species = 'Alunite (KAl3(SO4)2(OH)6)';
      altClass = 'High-Temperature Hydrothermal Alunite Fumarolic Leaching (Noctis Labyrinthus / Cross Crater)';
    } else if (TC < 130.0 && pH <= 3.0) {
      species = 'Jarosite (KFe3(SO4)2(OH)6)';
      altClass = 'Low-Temperature Evaporitic / Groundwater Acid Jarosite Precipitation (Meridiani Planum / Mawrth Vallis)';
    } else {
      species = 'Al-Hydroxysulfate (Basaluminite)';
      altClass = 'Neutralizing Acid Sulfate Spring Precipitation';
    }

    return {
      alterationFraction: parseFloat(alphaSulf.toFixed(3)),
      sulfatePrecipitatedWeightPercent: parseFloat(wSulfatePct.toFixed(1)),
      dominantSulfateSpecies: species,
      acidSulfateThermalInertiaTIU: parseFloat(TIU.toFixed(1)),
      acidHydrothermalClass: altClass,
      acidSulfateContext: `Acid Sulfate at ${TC.toFixed(0)} C, pH ${pH.toFixed(1)} (${(alphaSulf * 100).toFixed(1)}% altered, ${species}, TIU=${TIU.toFixed(0)}, ${altClass})`
    };
  }

  /**
   * Calculate sub-greenschist facies hydrothermal metamorphism of basaltic crust to prehnite-pumpellyite-chlorite assemblage, porosity reduction, and crystalline metabasalt thermal inertia.
   * Basalt + H2O (200-320 C, 50-200 MPa) -> Prehnite + Pumpellyite + Chlorite + Quartz
   * Reference: Ehlmann et al. (2009, 2011), Marzo et al. (2010), Viviano-Beck et al. (2014) for Martian Low-Grade Metamorphic Megabreccia.
   * @param {number} [basalticCrustPorosity=0.15] - Initial basaltic crust porosity (0.02 to 0.35)
   * @param {number} [metamorphicTempC=250.0] - Deep crustal metamorphic temperature in C (150 to 400 C)
   * @param {number} [lithostaticPressureMPa=120.0] - Overburden lithostatic pressure in MPa (20 to 400 MPa)
   * @param {number} [durationYears=1000.0] - Metamorphic heating duration in years (0.1 to 50000 yr)
   * @returns {{metamorphicConversionFraction: number, compactedResidualPorosity: number, metabasaltBulkDensityKgM3: number, crystallineMetabasaltThermalInertiaTIU: number, metamorphicFaciesClass: string, metamorphismContext: string}}
   */
  static computeMartianPrehnitePumpellyiteMetamorphism(basalticCrustPorosity = 0.15, metamorphicTempC = 250.0, lithostaticPressureMPa = 120.0, durationYears = 1000.0) {
    const phi0 = Math.max(0.01, Math.min(0.40, basalticCrustPorosity));
    const TC = Math.max(100.0, Math.min(450.0, metamorphicTempC));
    const PMPa = Math.max(10.0, Math.min(600.0, lithostaticPressureMPa));
    const tYrs = Math.max(0.01, durationYears);

    const TK = TC + 273.15;
    const tSec = tYrs * 365.25 * 86400.0;
    const Rgas = 8.314;
    const Ea = 6.50e4; // 65 kJ/mol

    // Metamorphic conversion rate
    const pFactor = Math.pow(PMPa / 100.0, 0.30);
    const kRate = 4.0e-3 * Math.exp(-Ea / (Rgas * TK)) * pFactor;

    const alphaMeta = 1.0 - Math.exp(-Math.min(20.0, kRate * tSec));

    // Porosity compaction and pore occlusion
    const phiResidual = phi0 * (1.0 - (0.75 * alphaMeta));

    // Metabasalt bulk density (kg/m^3)
    const rhoGrain = 2950.0;
    const rhoBulk = ((1.0 - phiResidual) * rhoGrain) + (phiResidual * 1000.0);

    // Thermal inertia of crystalline metabasalt
    const kTherm = 2.75; // W/(m K)
    const Cspec = 870.0; // J/(kg K)
    const TIU = Math.sqrt(kTherm * rhoBulk * Cspec);

    let faciesClass = 'Zeolite / Incipient Low-Grade Metamorphism';
    if (alphaMeta >= 0.50 && TC >= 220.0 && TC <= 320.0) {
      faciesClass = 'Prehnite-Pumpellyite Sub-Greenschist Facies (Nili Fossae / Toro Crater Deep Megabreccia)';
    } else if (TC > 320.0) {
      faciesClass = 'Greenschist Facies (Chlorite-Epidote-Actinolite Assemblage)';
    } else if (alphaMeta >= 0.20) {
      faciesClass = 'Moderate Sub-Greenschist Metamorphism';
    }

    return {
      metamorphicConversionFraction: parseFloat(alphaMeta.toFixed(3)),
      compactedResidualPorosity: parseFloat(phiResidual.toFixed(4)),
      metabasaltBulkDensityKgM3: parseFloat(rhoBulk.toFixed(1)),
      crystallineMetabasaltThermalInertiaTIU: parseFloat(TIU.toFixed(1)),
      metamorphicFaciesClass: faciesClass,
      metamorphismContext: `Prehnite-Pumpellyite Metamorphism at ${TC.toFixed(0)} C, ${PMPa.toFixed(0)} MPa (${(alphaMeta * 100).toFixed(1)}% altered, phi=${(phiResidual * 100).toFixed(1)}%, TIU=${TIU.toFixed(0)}, ${faciesClass})`
    };
  }

  /**
   * Calculate hydrothermal talc-carbonate (soapstone) alteration kinetics of serpentinized ultramafic crust, CO2 carbon sequestration yield, and rock thermal inertia.
   * 2 Mg3Si2O5(OH)4 (Serpentine) + 3 CO2 -> Mg3Si4O10(OH)2 (Talc) + 3 MgCO3 (Magnesite) + 3 H2O
   * Reference: Ehlmann et al. (2008), Viviano-Beck et al. (2014), Brown et al. (2020) for Martian Hydrothermal Carbon Sequestration.
   * @param {number} [ultramaficSerpentiniteFraction=0.60] - Initial serpentinite mass fraction in ultramafic protolith (0.05 to 0.95)
   * @param {number} [hydrothermalTempC=220.0] - Hydrothermal fluid temperature in C (80 to 380 C)
   * @param {number} [co2PartialPressureBar=25.0] - Dissolved CO2 partial pressure in bar (1 to 200 bar)
   * @param {number} [durationYears=500.0] - Hydrothermal carbonation duration in years (0.1 to 10000 yr)
   * @returns {{carbonationConversionFraction: number, sequesteredCO2KgPerM3: number, magnesiteYieldWeightPercent: number, soapstoneThermalInertiaTIU: number, carbonationRegimeClass: string, sequestrationContext: string}}
   */
  static computeMartianTalcCarbonateAlterationCarbonSequestration(ultramaficSerpentiniteFraction = 0.60, hydrothermalTempC = 220.0, co2PartialPressureBar = 25.0, durationYears = 500.0) {
    const wSerp = Math.max(0.01, Math.min(0.95, ultramaficSerpentiniteFraction));
    const TC = Math.max(50.0, Math.min(450.0, hydrothermalTempC));
    const PCO2 = Math.max(0.5, Math.min(500.0, co2PartialPressureBar));
    const tYrs = Math.max(0.01, durationYears);

    const TK = TC + 273.15;
    const tSec = tYrs * 365.25 * 86400.0;
    const Rgas = 8.314;
    const Ea = 6.00e4; // 60 kJ/mol for serpentine carbonation

    // Reaction rate constant
    const kRate = 2.5e-3 * Math.exp(-Ea / (Rgas * TK)) * Math.sqrt(PCO2 / 10.0);
    const alphaCarb = 1.0 - Math.exp(-Math.min(20.0, kRate * tSec));

    // CO2 sequestered per m^3 of rock (kg CO2 / m^3)
    const rhoRock = 2700.0; // kg/m^3
    const stoichiometricCO2Ratio = 0.2382; // (3 * 44.01) / (2 * 277.11)
    const kgCO2SeqM3 = alphaCarb * wSerp * rhoRock * stoichiometricCO2Ratio;

    // Magnesite precipitate yield (wt% of altered rock)
    const wMagnesitePct = alphaCarb * wSerp * 45.6;

    // Thermal inertia of dense talc-magnesite soapstone
    const kTherm = 1.95; // W/(m K)
    const rhoBulk = 2750.0; // kg/m^3
    const Cspec = 890.0; // J/(kg K)
    const TIU = Math.sqrt(kTherm * rhoBulk * Cspec);

    let carbClass = 'Incipient Serpentine Carbonation';
    if (alphaCarb >= 0.50 && TC >= 180.0 && TC <= 300.0) {
      carbClass = 'Pervasive Hydrothermal Talc-Magnesite Carbonation (Nili Fossae / Jezero Deep Basement)';
    } else if (TC > 300.0) {
      carbClass = 'High-Temperature Metamorphic Decarbonation Equilibrium';
    } else if (alphaCarb >= 0.20) {
      carbClass = 'Moderate Carbonate Veining in Serpentinized Ultramafics';
    }

    return {
      carbonationConversionFraction: parseFloat(alphaCarb.toFixed(3)),
      sequesteredCO2KgPerM3: parseFloat(kgCO2SeqM3.toFixed(1)),
      magnesiteYieldWeightPercent: parseFloat(wMagnesitePct.toFixed(1)),
      soapstoneThermalInertiaTIU: parseFloat(TIU.toFixed(1)),
      carbonationRegimeClass: carbClass,
      sequestrationContext: `Talc-Carbonate at ${TC.toFixed(0)} C, P_CO2=${PCO2.toFixed(0)} bar (${(alphaCarb * 100).toFixed(1)}% converted, ${kgCO2SeqM3.toFixed(0)} kg CO2/m3 sequestered, TIU=${TIU.toFixed(0)}, ${carbClass})`
    };
  }

  /**
   * Calculate high-temperature pneumatolytic fluorine-rich greisen metamorphism of felsic plutonic crust, topaz-quartz crystallization, and high greisen thermal inertia.
   * K-Feldspar + HF + H2O (300-500 C) -> Topaz (Al2SiO4F2) + Quartz + Fluor-Muscovite + Fluorite
   * Reference: Wray et al. (2013), Carter et al. (2013), Viviano-Beck et al. (2014) for Martian Felsic Pneumatolytic Greisens.
   * @param {number} [felsicGranitePorosity=0.10] - Initial felsic granite porosity (0.01 to 0.30)
   * @param {number} [pneumatolyticTempC=380.0] - Pneumatolytic/magmatic fluid temperature in C (200 to 600 C)
   * @param {number} [fluorineActivity=1.50] - Hydrothermal fluid HF/F- chemical activity (0.1 to 5.0)
   * @param {number} [durationYears=200.0] - Pneumatolytic alteration duration in years (0.1 to 5000 yr)
   * @returns {{greisenConversionFraction: number, topazYieldWeightPercent: number, greisenThermalConductivityWMK: number, crystallineGreisenThermalInertiaTIU: number, greisenAlterationClass: string, greisenContext: string}}
   */
  static computeMartianFluorineRichGreisenMetamorphism(felsicGranitePorosity = 0.10, pneumatolyticTempC = 380.0, fluorineActivity = 1.50, durationYears = 200.0) {
    const phi0 = Math.max(0.005, Math.min(0.35, felsicGranitePorosity));
    const TC = Math.max(150.0, Math.min(650.0, pneumatolyticTempC));
    const aF = Math.max(0.05, Math.min(10.0, fluorineActivity));
    const tYrs = Math.max(0.01, durationYears);

    const TK = TC + 273.15;
    const tSec = tYrs * 365.25 * 86400.0;
    const Rgas = 8.314;
    const Ea = 7.00e4; // 70 kJ/mol for greisenization

    // Reaction rate constant
    const kRate = 6.0e-3 * Math.exp(-Ea / (Rgas * TK)) * Math.pow(aF, 0.60);
    const alphaGreisen = 1.0 - Math.exp(-Math.min(20.0, kRate * tSec));

    // Topaz mass fraction in greisen (wt%)
    const wTopazPct = alphaGreisen * 42.5;

    // Thermal properties of highly conductive quartz-topaz greisen
    const kTherm = 4.10; // W/(m K)
    const rhoBulk = 2800.0; // kg/m^3
    const Cspec = 880.0; // J/(kg K)
    const TIU = Math.sqrt(kTherm * rhoBulk * Cspec);

    let gClass = 'Incipient Fluorine Metasomatism';
    if (alphaGreisen >= 0.50 && TC >= 300.0 && TC <= 500.0) {
      gClass = 'Pervasive High-Temperature Quartz-Topaz Greisen (Syrtis Major / Apollinaris Caldera Fumaroles)';
    } else if (TC > 500.0) {
      gClass = 'Magmatic-Hydrothermal Pegmatitic Transition';
    } else if (alphaGreisen >= 0.20) {
      gClass = 'Moderate Fluor-Muscovite Greisenization';
    }

    return {
      greisenConversionFraction: parseFloat(alphaGreisen.toFixed(3)),
      topazYieldWeightPercent: parseFloat(wTopazPct.toFixed(1)),
      greisenThermalConductivityWMK: parseFloat(kTherm.toFixed(2)),
      crystallineGreisenThermalInertiaTIU: parseFloat(TIU.toFixed(1)),
      greisenAlterationClass: gClass,
      greisenContext: `Fluorine Greisen at ${TC.toFixed(0)} C, a_F=${aF.toFixed(1)} (${(alphaGreisen * 100).toFixed(1)}% greisenized, ${wTopazPct.toFixed(1)}% Topaz, TIU=${TIU.toFixed(0)}, ${gClass})`
    };
  }

  /**
   * Calculate high-temperature halogen-carbonate scapolitization kinetics of plagioclase in basaltic/felsic crust, chlorine sequestration, and skarn thermal inertia.
   * 3 Albite + NaCl -> Na4Al3Si9O24Cl (Marialite); 3 Anorthite + CaCO3 -> Ca4Al6Si6O24CO3 (Meionite)
   * Reference: Clark et al. (1990), Swayze et al. (2008), Filiberto et al. (2014), Viviano-Beck et al. (2014) for Martian Scapolite Aureoles.
   * @param {number} [plagioclaseMassFraction=0.50] - Initial plagioclase feldspar fraction in protolith (0.05 to 0.95)
   * @param {number} [metasomaticTempC=420.0] - Contact metamorphic fluid temperature in C (200 to 650 C)
   * @param {number} [naclBrineSalinityWtPct=15.0] - Hydrothermal brine salinity in wt% NaCl (0.5 to 35.0 wt%)
   * @param {number} [durationYears=300.0] - Metasomatic circulation duration in years (0.1 to 5000 yr)
   * @returns {{scapolitizationConversionFraction: number, sequesteredChlorineWeightPercent: number, scapoliteEndmemberClass: string, calcSilicateThermalInertiaTIU: number, metasomaticFaciesClass: string, scapolitizationContext: string}}
   */
  static computeMartianScapoliteHalogenMetasomatism(plagioclaseMassFraction = 0.50, metasomaticTempC = 420.0, naclBrineSalinityWtPct = 15.0, durationYears = 300.0) {
    const wPlag = Math.max(0.01, Math.min(0.95, plagioclaseMassFraction));
    const TC = Math.max(150.0, Math.min(700.0, metasomaticTempC));
    const sal = Math.max(0.1, Math.min(40.0, naclBrineSalinityWtPct));
    const tYrs = Math.max(0.01, durationYears);

    const TK = TC + 273.15;
    const tSec = tYrs * 365.25 * 86400.0;
    const Rgas = 8.314;
    const Ea = 6.80e4; // 68 kJ/mol for scapolitization

    // Reaction rate constant
    const kRate = 4.5e-3 * Math.exp(-Ea / (Rgas * TK)) * Math.sqrt(sal / 10.0);
    const alphaScap = 1.0 - Math.exp(-Math.min(20.0, kRate * tSec));

    // Chlorine sequestered in scapolite crystal lattice (wt% of host rock)
    const wClSeq = alphaScap * wPlag * 4.10;

    // Thermal inertia of crystalline calc-silicate scapolite skarn
    const kTherm = 2.55; // W/(m K)
    const rhoBulk = 2680.0; // kg/m^3
    const Cspec = 870.0; // J/(kg K)
    const TIU = Math.sqrt(kTherm * rhoBulk * Cspec);

    let endmember = 'Sodic-Chloride Marialite (Na4Al3Si9O24Cl)';
    let facies = 'Incipient Halogen Metasomatism';

    if (sal >= 10.0 && TC <= 480.0) {
      endmember = 'Sodic-Chloride Marialite Scapolite (Na4Al3Si9O24Cl)';
      facies = 'High-Temperature Hypersaline Marialite Metasomatism (Tyrrhena Patera / Nili Fossae Aureoles)';
    } else if (TC > 480.0) {
      endmember = 'Calcic-Carbonate/Sulfate Meionite Scapolite (Ca4Al6Si6O24CO3/SO4)';
      facies = 'Deep Pyrometamorphic Granulite / Meionite Skarn Facies';
    } else {
      endmember = 'Intermediate Dipyre / Mizzonite Solid Solution';
      facies = 'Moderate Contact Metasomatic Aureole';
    }

    return {
      scapolitizationConversionFraction: parseFloat(alphaScap.toFixed(3)),
      sequesteredChlorineWeightPercent: parseFloat(wClSeq.toFixed(2)),
      scapoliteEndmemberClass: endmember,
      calcSilicateThermalInertiaTIU: parseFloat(TIU.toFixed(1)),
      metasomaticFaciesClass: facies,
      scapolitizationContext: `Scapolitization at ${TC.toFixed(0)} C, ${sal.toFixed(1)}% NaCl (${(alphaScap * 100).toFixed(1)}% converted, ${wClSeq.toFixed(2)}% Cl sequestered, TIU=${TIU.toFixed(0)}, ${endmember})`
    };
  }

  /**
   * Calculate hydrothermal borosilicate metasomatism of calcic basaltic/skarn crust, datolite-danburite crystallization, boron sequestration, and vein thermal inertia.
   * Calcite + Quartz + H3BO3 (250-450 C) -> Datolite (CaBSiO4(OH)) + CO2 + H2O
   * Reference: Gasda et al. (2017), Frydenvang et al. (2017), Viviano-Beck et al. (2014) for Martian Groundwater/Hydrothermal Boron Fixation.
   * @param {number} [calcicBasaltPorosity=0.12] - Initial calcic basalt protolith porosity (0.01 to 0.35)
   * @param {number} [hydrothermalTempC=320.0] - Boron-rich fluid temperature in C (150 to 550 C)
   * @param {number} [boronActivity=1.80] - Fluid boron/H3BO3 chemical activity (0.1 to 6.0)
   * @param {number} [durationYears=300.0] - Hydrothermal circulation duration in years (0.1 to 5000 yr)
   * @returns {{borosilicateConversionFraction: number, sequesteredBoronOxideWeightPercent: number, dominantBorosilicateSpecies: string, borosilicateThermalInertiaTIU: number, boronMineralizationClass: string, borosilicateContext: string}}
   */
  static computeMartianBorosilicateMetasomatism(calcicBasaltPorosity = 0.12, hydrothermalTempC = 320.0, boronActivity = 1.80, durationYears = 300.0) {
    const phi0 = Math.max(0.005, Math.min(0.35, calcicBasaltPorosity));
    const TC = Math.max(100.0, Math.min(600.0, hydrothermalTempC));
    const aB = Math.max(0.05, Math.min(10.0, boronActivity));
    const tYrs = Math.max(0.01, durationYears);

    const TK = TC + 273.15;
    const tSec = tYrs * 365.25 * 86400.0;
    const Rgas = 8.314;
    const Ea = 6.40e4; // 64 kJ/mol for borosilicate crystallization

    // Reaction rate constant
    const kRate = 3.5e-3 * Math.exp(-Ea / (Rgas * TK)) * Math.sqrt(aB);
    const alphaBoro = 1.0 - Math.exp(-Math.min(20.0, kRate * tSec));

    // Boron oxide sequestered in veins (wt% of mineralized rock)
    const wB2O3Pct = alphaBoro * 5.80;

    // Thermal inertia of dense crystalline borosilicate vein
    const kTherm = 2.40; // W/(m K)
    const rhoBulk = 2720.0; // kg/m^3
    const Cspec = 870.0; // J/(kg K)
    const TIU = Math.sqrt(kTherm * rhoBulk * Cspec);

    let species = 'Datolite (CaBSiO4(OH))';
    let bClass = 'Incipient Boron Metasomatism';

    if (TC <= 350.0 && alphaBoro >= 0.40) {
      species = 'Datolite (CaBSiO4(OH))';
      bClass = 'Hydrothermal Datolite-Calcite Fracture Vein Mineralization (Gale Crater / Nili Fossae)';
    } else if (TC > 350.0 && alphaBoro >= 0.40) {
      species = 'Danburite (CaB2Si2O8)';
      bClass = 'High-Temperature Pyrometasomatic Danburite-Feldspar Skarn';
    } else {
      species = 'Dumortierite / Tourmaline Precursor';
      bClass = 'Pneumatolytic Boron-Silicate Metasomatism';
    }

    return {
      borosilicateConversionFraction: parseFloat(alphaBoro.toFixed(3)),
      sequesteredBoronOxideWeightPercent: parseFloat(wB2O3Pct.toFixed(2)),
      dominantBorosilicateSpecies: species,
      borosilicateThermalInertiaTIU: parseFloat(TIU.toFixed(1)),
      boronMineralizationClass: bClass,
      borosilicateContext: `Borosilicate Metasomatism at ${TC.toFixed(0)} C, a_B=${aB.toFixed(1)} (${(alphaBoro * 100).toFixed(1)}% converted, ${wB2O3Pct.toFixed(2)}% B2O3, TIU=${TIU.toFixed(0)}, ${species})`
    };
  }

  /**
   * Calculate high-temperature calcium-metasomatic rodingitization of gabbroic protolith in serpentinized crust, vesuvianite-grossular skarn crystallization, and high thermal inertia.
   * Plagioclase + Pyroxene + Ca2+(fluid) (250-450 C) -> Vesuvianite + Grossular + Diopside + Clinozoisite
   * Reference: Ehlmann et al. (2009, 2011), Viviano-Beck et al. (2014) for Martian Rodingitized Calc-Silicate Complexes.
   * @param {number} [maficGabbroPorosity=0.08] - Initial gabbroic protolith porosity (0.01 to 0.25)
   * @param {number} [rodingitizationTempC=350.0] - Calcium-rich fluid temperature in C (180 to 550 C)
   * @param {number} [caMgFluidRatio=4.50] - Fluid Ca2+/Mg2+ chemical activity ratio (0.5 to 15.0)
   * @param {number} [durationYears=400.0] - Rodingitization duration in years (0.1 to 5000 yr)
   * @returns {{rodingitizationConversionFraction: number, vesuvianiteYieldWeightPercent: number, rodingiteBulkDensityKgM3: number, crystallineRodingiteThermalInertiaTIU: number, rodingiteFaciesClass: string, rodingiteContext: string}}
   */
  static computeMartianRodingiteCalcSilicateMetasomatism(maficGabbroPorosity = 0.08, rodingitizationTempC = 350.0, caMgFluidRatio = 4.50, durationYears = 400.0) {
    const phi0 = Math.max(0.005, Math.min(0.30, maficGabbroPorosity));
    const TC = Math.max(120.0, Math.min(600.0, rodingitizationTempC));
    const rCaMg = Math.max(0.2, Math.min(25.0, caMgFluidRatio));
    const tYrs = Math.max(0.01, durationYears);

    const TK = TC + 273.15;
    const tSec = tYrs * 365.25 * 86400.0;
    const Rgas = 8.314;
    const Ea = 6.60e4; // 66 kJ/mol for calc-silicate rodingitization

    // Reaction rate constant
    const kRate = 5.0e-3 * Math.exp(-Ea / (Rgas * TK)) * Math.sqrt(rCaMg / 2.0);
    const alphaRoding = 1.0 - Math.exp(-Math.min(20.0, kRate * tSec));

    // Vesuvianite mass fraction in rodingite (wt%)
    const wVesuvPct = alphaRoding * 38.0;

    // Porosity compaction and densification
    const phiResidual = phi0 * (1.0 - (0.85 * alphaRoding));
    const rhoGrain = 3350.0;
    const rhoBulk = ((1.0 - phiResidual) * rhoGrain) + (phiResidual * 1000.0);

    // Thermal inertia of dense crystalline rodingite skarn
    const kTherm = 2.90; // W/(m K)
    const Cspec = 840.0; // J/(kg K)
    const TIU = Math.sqrt(kTherm * rhoBulk * Cspec);

    let rClass = 'Incipient Calcium Metasomatism';
    if (alphaRoding >= 0.50 && TC >= 250.0 && TC <= 450.0) {
      rClass = 'Pervasive Vesuvianite-Grossular-Diopside Rodingite Skarn (Nili Fossae / Claritas Contact Aureoles)';
    } else if (TC > 450.0) {
      rClass = 'High-Temperature Pyrometamorphic Granulite Aureole';
    } else if (alphaRoding >= 0.20) {
      rClass = 'Moderate Clinozoisite-Prehnite Rodingitization';
    }

    return {
      rodingitizationConversionFraction: parseFloat(alphaRoding.toFixed(3)),
      vesuvianiteYieldWeightPercent: parseFloat(wVesuvPct.toFixed(1)),
      rodingiteBulkDensityKgM3: parseFloat(rhoBulk.toFixed(1)),
      crystallineRodingiteThermalInertiaTIU: parseFloat(TIU.toFixed(1)),
      rodingiteFaciesClass: rClass,
      rodingiteContext: `Rodingitization at ${TC.toFixed(0)} C, Ca/Mg=${rCaMg.toFixed(1)} (${(alphaRoding * 100).toFixed(1)}% converted, ${wVesuvPct.toFixed(1)}% Vesuvianite, TIU=${TIU.toFixed(0)}, ${rClass})`
    };
  }

  /**
   * Calculate hydrothermal/burial metamorphism of calcic crust into epidote-supergroup polymorphs (clinozoisite vs zoisite), crystal densification, and hornfels thermal inertia.
   * 4 Anorthite + H2O (280-520 C) -> 2 Clinozoisite (Ca2Al3(SiO4)3(OH)) + 2 Kyanite + Quartz
   * Reference: Ehlmann et al. (2009, 2011), Viviano-Beck et al. (2014) for Martian Epidote-Supergroup Metamorphic Terranes.
   * @param {number} [maficCrustPorosity=0.10] - Initial mafic basalt/anorthosite protolith porosity (0.01 to 0.30)
   * @param {number} [metamorphicTempC=380.0] - Subsurface hydrothermal/burial metamorphic temperature in C (200 to 600 C)
   * @param {number} [fe3AlRatio=0.20] - Molar Fe3+/(Fe3+ + Al) cation substitution ratio (0.0 to 0.80)
   * @param {number} [durationYears=500.0] - Metamorphic alteration duration in years (0.1 to 5000 yr)
   * @returns {{zoisiteConversionFraction: number, zoisitePolymorphYieldWeightPercent: number, dominantPolymorphSpecies: string, metamorphicHornfelsThermalInertiaTIU: number, metamorphicFaciesClass: string, zoisiteContext: string}}
   */
  static computeMartianClinozoisiteZoisiteMetamorphism(maficCrustPorosity = 0.10, metamorphicTempC = 380.0, fe3AlRatio = 0.20, durationYears = 500.0) {
    const phi0 = Math.max(0.005, Math.min(0.35, maficCrustPorosity));
    const TC = Math.max(150.0, Math.min(650.0, metamorphicTempC));
    const rFeAl = Math.max(0.0, Math.min(0.90, fe3AlRatio));
    const tYrs = Math.max(0.01, durationYears);

    const TK = TC + 273.15;
    const tSec = tYrs * 365.25 * 86400.0;
    const Rgas = 8.314;
    const Ea = 6.70e4; // 67 kJ/mol for epidote-group crystallization

    // Reaction rate constant
    const kRate = 4.2e-3 * Math.exp(-Ea / (Rgas * TK)) * (1.0 + rFeAl);
    const alphaZoisite = 1.0 - Math.exp(-Math.min(20.0, kRate * tSec));

    // Polymorph mass fraction (wt%)
    const wZoisitePct = alphaZoisite * 45.0;

    // Compaction and high thermal inertia
    const phiResidual = phi0 * (1.0 - (0.80 * alphaZoisite));
    const rhoGrain = 3400.0;
    const rhoBulk = ((1.0 - phiResidual) * rhoGrain) + (phiResidual * 1000.0);

    const kTherm = 3.00; // W/(m K)
    const Cspec = 830.0; // J/(kg K)
    const TIU = Math.sqrt(kTherm * rhoBulk * Cspec);

    let species = 'Clinozoisite (Monoclinic Ca2Al3(SiO4)3(OH))';
    let fClass = 'Incipient Epidote-Facies Metamorphism';

    if (rFeAl <= 0.15 && TC >= 300.0 && TC <= 500.0) {
      species = 'Low-Fe Clinozoisite (Ca2Al3(SiO4)3(OH))';
      fClass = 'Hydrothermal Low-Fe Clinozoisite Metasomatism (Valles Marineris Wall Strata)';
    } else if (rFeAl > 0.35) {
      species = 'Fe-Rich Epidote (Pistacite Ca2(Al,Fe)3(SiO4)3(OH))';
      fClass = 'Greenschist-Facies Fe-Epidote Alteration';
    } else {
      species = 'Orthorhombic Zoisite (Ca2Al3(SiO4)3(OH))';
      fClass = 'High-Pressure Amphibolite / Zoisite Hornfels Facies';
    }

    return {
      zoisiteConversionFraction: parseFloat(alphaZoisite.toFixed(3)),
      zoisitePolymorphYieldWeightPercent: parseFloat(wZoisitePct.toFixed(1)),
      dominantPolymorphSpecies: species,
      metamorphicHornfelsThermalInertiaTIU: parseFloat(TIU.toFixed(1)),
      metamorphicFaciesClass: fClass,
      zoisiteContext: `Epidote-Group at ${TC.toFixed(0)} C, Fe/Al=${rFeAl.toFixed(2)} (${(alphaZoisite * 100).toFixed(1)}% converted, ${wZoisitePct.toFixed(1)}% ${species.split(' ')[0]}, TIU=${TIU.toFixed(0)}, ${fClass})`
    };
  }

  /**
   * Calculate extreme acid-sulfate-fluorine fumarolic condensation on volcanic pyroclastics, topaz-alunite sinter crystallization, and indurated thermal inertia.
   * Al-Silicate Ash + HF + H2SO4 (200-450 C) -> Topaz (Al2SiO4F2) + Alunite (KAl3(SO4)2(OH)6) + Quartz
   * Reference: Wray et al. (2013), Carter et al. (2013), Viviano-Beck et al. (2014) for Martian Acid-Sulfate Fumarolic Fields.
   * @param {number} [pyroclasticAshPorosity=0.25] - Initial volcanic ash/tuff porosity (0.05 to 0.50)
   * @param {number} [fumarolicTempC=340.0] - Fumarolic vapor condensation temperature in C (150 to 550 C)
   * @param {number} [hfH2So4Ratio=0.80] - Fluid HF/H2SO4 acid vapor activity ratio (0.1 to 5.0)
   * @param {number} [durationYears=250.0] - Fumarolic condensation duration in years (0.1 to 5000 yr)
   * @returns {{fumarolicConversionFraction: number, topazAluniteYieldWeightPercent: number, induratedSinterThermalInertiaTIU: number, fumarolicAlterationClass: string, acidVaporContext: string}}
   */
  static computeMartianAcidVaporTopazAluniteCondensation(pyroclasticAshPorosity = 0.25, fumarolicTempC = 340.0, hfH2So4Ratio = 0.80, durationYears = 250.0) {
    const phi0 = Math.max(0.02, Math.min(0.60, pyroclasticAshPorosity));
    const TC = Math.max(120.0, Math.min(600.0, fumarolicTempC));
    const rAcid = Math.max(0.05, Math.min(10.0, hfH2So4Ratio));
    const tYrs = Math.max(0.01, durationYears);

    const TK = TC + 273.15;
    const tSec = tYrs * 365.25 * 86400.0;
    const Rgas = 8.314;
    const Ea = 6.50e4; // 65 kJ/mol for acid-vapor condensation

    // Reaction rate constant
    const kRate = 4.8e-3 * Math.exp(-Ea / (Rgas * TK)) * Math.sqrt(1.0 + rAcid);
    const alphaFum = 1.0 - Math.exp(-Math.min(20.0, kRate * tSec));

    // Yield of co-crystallized Topaz + Alunite (wt%)
    const wTopazAlunitePct = alphaFum * 52.0;

    // Sinter induration and pore compaction
    const phiResidual = phi0 * (1.0 - (0.70 * alphaFum));
    const rhoGrain = 2850.0;
    const rhoBulk = ((1.0 - phiResidual) * rhoGrain) + (phiResidual * 1000.0);

    const kTherm = 2.80; // W/(m K)
    const Cspec = 890.0; // J/(kg K)
    const TIU = Math.sqrt(kTherm * rhoBulk * Cspec);

    let fClass = 'Incipient Acid Fumarolic Leaching';
    if (alphaFum >= 0.50 && TC >= 250.0 && TC <= 450.0) {
      fClass = 'High-Temperature Topaz-Alunite Acid Sinter (Syrtis Major / Elysium Fumarolic Fields)';
    } else if (TC > 450.0) {
      fClass = 'Magmatic Vapor-Plume Greisen Transition';
    } else if (alphaFum >= 0.20) {
      fClass = 'Moderate Jarosite-Kaolinite Solfatara Condensation';
    }

    return {
      fumarolicConversionFraction: parseFloat(alphaFum.toFixed(3)),
      topazAluniteYieldWeightPercent: parseFloat(wTopazAlunitePct.toFixed(1)),
      induratedSinterThermalInertiaTIU: parseFloat(TIU.toFixed(1)),
      fumarolicAlterationClass: fClass,
      acidVaporContext: `Acid Vapor at ${TC.toFixed(0)} C, HF/H2SO4=${rAcid.toFixed(1)} (${(alphaFum * 100).toFixed(1)}% converted, ${wTopazAlunitePct.toFixed(1)}% Topaz+Alunite, TIU=${TIU.toFixed(0)}, ${fClass})`
    };
  }

  /**
   * Calculate low-grade hydrothermal/burial pumpellyite-epidote-chlorite facies metamorphism of basaltic crust, structural hydration, and metabasalt thermal inertia.
   * Pyroxene + Plagioclase + H2O (180-320 C) -> Pumpellyite (Ca4(Mg,Fe)Al5O(Si2O7)2(SiO4)2(OH)3*2H2O) + Epidote + Chlorite
   * Reference: Ehlmann et al. (2009, 2011), Carter et al. (2013), Viviano-Beck et al. (2014) for Martian Sub-Greenschist Metamorphism.
   * @param {number} [basaltPorosity=0.15] - Initial vesicular basalt protolith porosity (0.02 to 0.35)
   * @param {number} [metasomaticTempC=260.0] - Sub-greenschist metamorphic fluid temperature in C (150 to 400 C)
   * @param {number} [fluidMgFeRatio=1.80] - Metasomatic fluid Mg/Fe activity ratio (0.2 to 10.0)
   * @param {number} [durationYears=600.0] - Metamorphic alteration duration in years (0.1 to 5000 yr)
   * @returns {{pumpellyiteConversionFraction: number, boundWaterYieldWeightPercent: number, pumpellyiteFaciesClass: string, crystallineMetabasaltThermalInertiaTIU: number, subGreenschistContext: string}}
   */
  static computeMartianPumpellyiteEpidoteMetasomatism(basaltPorosity = 0.15, metasomaticTempC = 260.0, fluidMgFeRatio = 1.80, durationYears = 600.0) {
    const phi0 = Math.max(0.01, Math.min(0.40, basaltPorosity));
    const TC = Math.max(100.0, Math.min(500.0, metasomaticTempC));
    const rMgFe = Math.max(0.1, Math.min(15.0, fluidMgFeRatio));
    const tYrs = Math.max(0.01, durationYears);

    const TK = TC + 273.15;
    const tSec = tYrs * 365.25 * 86400.0;
    const Rgas = 8.314;
    const Ea = 6.10e4; // 61 kJ/mol for pumpellyite crystallization

    // Reaction rate constant
    const kRate = 3.6e-3 * Math.exp(-Ea / (Rgas * TK)) * Math.sqrt(rMgFe);
    const alphaPump = 1.0 - Math.exp(-Math.min(20.0, kRate * tSec));

    // Bound structural H2O yield (wt%)
    const wH2OPct = alphaPump * 6.20;

    // Vesicle infilling and pore reduction
    const phiResidual = phi0 * (1.0 - (0.75 * alphaPump));
    const rhoGrain = 3250.0;
    const rhoBulk = ((1.0 - phiResidual) * rhoGrain) + (phiResidual * 1000.0);

    const kTherm = 2.45; // W/(m K)
    const Cspec = 810.0; // J/(kg K)
    const TIU = Math.sqrt(kTherm * rhoBulk * Cspec);

    let pClass = 'Incipient Sub-Greenschist Metasomatism';
    if (alphaPump >= 0.50 && TC >= 200.0 && TC <= 320.0) {
      pClass = 'Pervasive Pumpellyite-Epidote-Chlorite Sub-Greenschist Facies (Mawrth Vallis / Nili Deep Basement)';
    } else if (TC > 320.0) {
      pClass = 'Greenschist Actinolite-Epidote Transition';
    } else if (alphaPump >= 0.20) {
      pClass = 'Zeolite-Pumpellyite Facies Boundary';
    }

    return {
      pumpellyiteConversionFraction: parseFloat(alphaPump.toFixed(3)),
      boundWaterYieldWeightPercent: parseFloat(wH2OPct.toFixed(2)),
      pumpellyiteFaciesClass: pClass,
      crystallineMetabasaltThermalInertiaTIU: parseFloat(TIU.toFixed(1)),
      subGreenschistContext: `Pumpellyite Facies at ${TC.toFixed(0)} C, Mg/Fe=${rMgFe.toFixed(1)} (${(alphaPump * 100).toFixed(1)}% converted, ${wH2OPct.toFixed(2)}% bound H2O, TIU=${TIU.toFixed(0)}, ${pClass})`
    };
  }

  /**
   * Calculate high-pressure/low-temperature (HP-LT) lawsonite-glaucophane blueschist facies metamorphism, crystal densification, and suture zone thermal inertia.
   * Calcic Plagioclase + Pyroxene + H2O (150-350 C, 0.6-1.8 GPa) -> Lawsonite (CaAl2Si2O7(OH)2*H2O) + Glaucophane + Aragonite
   * Reference: Ehlmann et al. (2011), Viviano-Beck et al. (2014) for Martian High-Pressure Blueschist Metamorphic Belts.
   * @param {number} [oceanicBasaltPorosity=0.12] - Initial oceanic basalt protolith porosity (0.01 to 0.30)
   * @param {number} [subductionTempC=280.0] - HP-LT metamorphic temperature in C (120 to 450 C)
   * @param {number} [fluidPressureGigaPa=1.20] - Subsurface lithostatic/fluid pressure in GPa (0.3 to 3.0 GPa)
   * @param {number} [durationYears=500.0] - Metamorphic alteration duration in years (0.1 to 5000 yr)
   * @returns {{blueschistConversionFraction: number, boundWaterYieldWeightPercent: number, dominantBlueschistMineral: string, crystallineBlueschistThermalInertiaTIU: number, metamorphicFaciesClass: string, blueschistContext: string}}
   */
  static computeMartianLawsoniteBlueschistMetamorphism(oceanicBasaltPorosity = 0.12, subductionTempC = 280.0, fluidPressureGigaPa = 1.20, durationYears = 500.0) {
    const phi0 = Math.max(0.005, Math.min(0.35, oceanicBasaltPorosity));
    const TC = Math.max(100.0, Math.min(500.0, subductionTempC));
    const PGPa = Math.max(0.2, Math.min(4.0, fluidPressureGigaPa));
    const tYrs = Math.max(0.01, durationYears);

    const TK = TC + 273.15;
    const tSec = tYrs * 365.25 * 86400.0;
    const Rgas = 8.314;
    const Ea = 6.30e4; // 63 kJ/mol for lawsonite crystallization

    // Reaction rate constant
    const kRate = 3.2e-3 * Math.exp(-Ea / (Rgas * TK)) * Math.pow(PGPa, 0.60);
    const alphaBlue = 1.0 - Math.exp(-Math.min(20.0, kRate * tSec));

    // Bound structural H2O yield (wt%) - Lawsonite contains 11.5 wt% H2O
    const wH2OPct = alphaBlue * 11.50;

    // High pressure compaction and pore elimination
    const phiResidual = phi0 * (1.0 - (0.85 * alphaBlue));
    const rhoGrain = 3180.0;
    const rhoBulk = ((1.0 - phiResidual) * rhoGrain) + (phiResidual * 1000.0);

    const kTherm = 2.95; // W/(m K)
    const Cspec = 870.0; // J/(kg K)
    const TIU = Math.sqrt(kTherm * rhoBulk * Cspec);

    let mineral = 'Lawsonite + Glaucophane';
    let bClass = 'Incipient Blueschist Metamorphism';

    if (PGPa >= 0.80 && TC >= 200.0 && TC <= 350.0) {
      bClass = 'Pervasive Lawsonite-Blueschist HP-LT Facies (Ancient Noachian Suture Zones)';
      mineral = 'Lawsonite (CaAl2Si2O7(OH)2·H2O)';
    } else if (TC > 350.0) {
      bClass = 'Epidote-Amphibolite / Eclogite Transition';
      mineral = 'Omphacite + Garnet';
    } else if (PGPa < 0.80) {
      bClass = 'Sub-Greenschist Prehnite-Pumpellyite Facies';
      mineral = 'Prehnite + Pumpellyite';
    }

    return {
      blueschistConversionFraction: parseFloat(alphaBlue.toFixed(3)),
      boundWaterYieldWeightPercent: parseFloat(wH2OPct.toFixed(2)),
      dominantBlueschistMineral: mineral,
      crystallineBlueschistThermalInertiaTIU: parseFloat(TIU.toFixed(1)),
      metamorphicFaciesClass: bClass,
      blueschistContext: `Blueschist Facies at ${TC.toFixed(0)} C, P=${PGPa.toFixed(2)} GPa (${(alphaBlue * 100).toFixed(1)}% converted, ${wH2OPct.toFixed(2)}% bound H2O, TIU=${TIU.toFixed(0)}, ${bClass})`
    };
  }

  /**
   * Calculate high-temperature hydrothermal kaolinite-to-dickite polymorphic maturation, crystal stacking ordering, and clay sinter thermal inertia.
   * Disordered Kaolinite + H+ (140-280 C, pH 2.5-4.5) -> Ordered Dickite (Al2Si2O5(OH)4) + Quartz Sinter
   * Reference: Wray et al. (2009), Ehlmann et al. (2009), Viviano-Beck et al. (2014) for Martian Hydrothermal Argillic Clay Deposits.
   * @param {number} [aluminousAshPorosity=0.30] - Initial aluminous ash/clay protolith porosity (0.05 to 0.50)
   * @param {number} [hydrothermalTempC=210.0] - Hydrothermal fluid temperature in C (100 to 350 C)
   * @param {number} [fluidAcidityPH=3.2] - Fluid acidity pH (1.5 to 7.0)
   * @param {number} [durationYears=300.0] - Hydrothermal alteration duration in years (0.1 to 5000 yr)
   * @returns {{dickiteConversionFraction: number, orderedDickiteYieldWeightPercent: number, induratedClayThermalInertiaTIU: number, argillicMaturationClass: string, dickiteContext: string}}
   */
  static computeMartianDickiteKaoliniteArgillicMaturation(aluminousAshPorosity = 0.30, hydrothermalTempC = 210.0, fluidAcidityPH = 3.2, durationYears = 300.0) {
    const phi0 = Math.max(0.02, Math.min(0.60, aluminousAshPorosity));
    const TC = Math.max(80.0, Math.min(450.0, hydrothermalTempC));
    const pH = Math.max(1.0, Math.min(8.0, fluidAcidityPH));
    const tYrs = Math.max(0.01, durationYears);

    const TK = TC + 273.15;
    const tSec = tYrs * 365.25 * 86400.0;
    const Rgas = 8.314;
    const Ea = 5.80e4; // 58 kJ/mol for kaolin-to-dickite ordering

    const aHPlus = Math.pow(10.0, -pH);
    // Reaction rate constant
    const kRate = 4.5e-3 * Math.exp(-Ea / (Rgas * TK)) * Math.pow(aHPlus * 1000.0, 0.40);
    const alphaDick = 1.0 - Math.exp(-Math.min(20.0, kRate * tSec));

    // Ordered Dickite yield (wt%)
    const wDickitePct = alphaDick * 78.0;

    // Pore cementation and compaction
    const phiResidual = phi0 * (1.0 - (0.65 * alphaDick));
    const rhoGrain = 2650.0;
    const rhoBulk = ((1.0 - phiResidual) * rhoGrain) + (phiResidual * 1000.0);

    const kTherm = 2.30; // W/(m K)
    const Cspec = 940.0; // J/(kg K)
    const TIU = Math.sqrt(kTherm * rhoBulk * Cspec);

    let aClass = 'Incipient Kaolinitic Weathering';
    if (alphaDick >= 0.50 && TC >= 140.0 && TC <= 280.0) {
      aClass = 'High-Temperature Ordered Dickite Hydrothermal Facies (Mawrth / Nili / Toro Crater)';
    } else if (TC > 280.0) {
      aClass = 'Pyrophyllite-Quartz Advanced Argillic Transition';
    } else if (alphaDick >= 0.20) {
      aClass = 'Disordered Kaolinite-Halloysite Weathering Crust';
    }

    return {
      dickiteConversionFraction: parseFloat(alphaDick.toFixed(3)),
      orderedDickiteYieldWeightPercent: parseFloat(wDickitePct.toFixed(1)),
      induratedClayThermalInertiaTIU: parseFloat(TIU.toFixed(1)),
      argillicMaturationClass: aClass,
      dickiteContext: `Dickite Facies at ${TC.toFixed(0)} C, pH=${pH.toFixed(1)} (${(alphaDick * 100).toFixed(1)}% converted, ${wDickitePct.toFixed(1)}% Dickite, TIU=${TIU.toFixed(0)}, ${aClass})`
    };
  }

  /**
   * Calculate hydrothermal/pedogenic dioctahedral smectite (beidellite vs nontronite) solid solution crystallization, interlayer hydration, and clay bed thermal inertia.
   * Basaltic Ash + Al3+ + Fe3+ + H2O (40-180 C) -> Beidellite (Al2(Si3.67Al0.33)O10(OH)2) - Nontronite (Fe2Si4O10(OH)2)
   * Reference: Ehlmann et al. (2011), Carter et al. (2013), Viviano-Beck et al. (2014) for Martian Dioctahedral Smectite Strata.
   * @param {number} [initialBasaltPorosity=0.20] - Initial basaltic ash/glass porosity (0.05 to 0.45)
   * @param {number} [hydrothermalTempC=85.0] - Alteration fluid temperature in C (20 to 220 C)
   * @param {number} [alFeRatio=1.40] - Fluid Al/Fe cation activity ratio (0.1 to 10.0)
   * @param {number} [durationYears=400.0] - Smectite crystallization duration in years (0.1 to 5000 yr)
   * @returns {{smectiteConversionFraction: number, interlayerWaterYieldWeightPercent: number, dominantSmectiteSpecies: string, hydratedClayThermalInertiaTIU: number, smectiteFaciesClass: string, smectiteContext: string}}
   */
  static computeMartianBeidelliteNontroniteSmectiteKinetics(initialBasaltPorosity = 0.20, hydrothermalTempC = 85.0, alFeRatio = 1.40, durationYears = 400.0) {
    const phi0 = Math.max(0.02, Math.min(0.50, initialBasaltPorosity));
    const TC = Math.max(15.0, Math.min(300.0, hydrothermalTempC));
    const rAlFe = Math.max(0.05, Math.min(15.0, alFeRatio));
    const tYrs = Math.max(0.01, durationYears);

    const TK = TC + 273.15;
    const tSec = tYrs * 365.25 * 86400.0;
    const Rgas = 8.314;
    const Ea = 4.80e4; // 48 kJ/mol for smectite crystallization

    // Reaction rate constant
    const kRate = 3.8e-3 * Math.exp(-Ea / (Rgas * TK)) * Math.sqrt(rAlFe);
    const alphaSmec = 1.0 - Math.exp(-Math.min(20.0, kRate * tSec));

    // Interlayer hydration water yield (wt%)
    const wH2OPct = alphaSmec * 14.50;

    // Swelling pore evolution
    const phiResidual = phi0 * (1.0 - (0.50 * alphaSmec));
    const rhoGrain = 2450.0;
    const rhoBulk = ((1.0 - phiResidual) * rhoGrain) + (phiResidual * 1000.0);

    const kTherm = 1.45; // W/(m K)
    const Cspec = 1010.0; // J/(kg K)
    const TIU = Math.sqrt(kTherm * rhoBulk * Cspec);

    let species = 'Intermediate Al-Fe Beidellite-Nontronite';
    let sClass = 'Dioctahedral Smectite Clay Weathering';

    if (rAlFe >= 2.0 && TC <= 140.0) {
      species = 'Al-Rich Beidellite (Al2(Si3.67Al0.33)O10(OH)2·nH2O)';
      sClass = 'Al-Smectite Leached Strata (Mawrth Vallis / Claritas Upper Clay Unit)';
    } else if (rAlFe <= 0.60) {
      species = 'Fe-Rich Nontronite (Fe3+2Si4O10(OH)2·nH2O)';
      sClass = 'Alkaline Nontronite Clay Deposits (Oxia Planum / Nili Fossae)';
    } else {
      species = 'Mixed Beidellite-Nontronite Solid Solution';
      sClass = 'Neutral Hydrothermal/Pedogenic Smectite Sequence';
    }

    return {
      smectiteConversionFraction: parseFloat(alphaSmec.toFixed(3)),
      interlayerWaterYieldWeightPercent: parseFloat(wH2OPct.toFixed(2)),
      dominantSmectiteSpecies: species,
      hydratedClayThermalInertiaTIU: parseFloat(TIU.toFixed(1)),
      smectiteFaciesClass: sClass,
      smectiteContext: `Smectite Facies at ${TC.toFixed(0)} C, Al/Fe=${rAlFe.toFixed(1)} (${(alphaSmec * 100).toFixed(1)}% converted, ${wH2OPct.toFixed(2)}% interlayer H2O, TIU=${TIU.toFixed(0)}, ${sClass})`
    };
  }

  /**
   * Calculate hydrothermal/acid-sulfate alunite-jarosite solid solution crystallization, sulfate cementation, and indurated crust thermal inertia.
   * Volcanic Ash + K+ + Al3+ + Fe3+ + H2SO4 (80-240 C) -> K(AlxFe1-x)3(SO4)2(OH)6 + Silica Sinter
   * Reference: Swayze et al. (2008), Ehlmann et al. (2011), Viviano-Beck et al. (2014) for Martian Acid-Sulfate Alunite-Jarosite Deposits.
   * @param {number} [pyroclasticPorosity=0.25] - Initial pyroclastic ash/basalt porosity (0.05 to 0.45)
   * @param {number} [hydrothermalTempC=160.0] - Hydrothermal fluid temperature in C (60 to 300 C)
   * @param {number} [alFeCationRatio=1.20] - Fluid Al/Fe cation activity ratio (0.1 to 10.0)
   * @param {number} [durationYears=350.0] - Acid-sulfate alteration duration in years (0.1 to 5000 yr)
   * @returns {{acidSulfateConversionFraction: number, sulfateMineralYieldWeightPercent: number, dominantSulfateSpecies: string, induratedSulfateThermalInertiaTIU: number, acidSulfateFaciesClass: string, acidSulfateContext: string}}
   */
  static computeMartianAluniteJarositeSolidSolutionKinetics(pyroclasticPorosity = 0.25, hydrothermalTempC = 160.0, alFeCationRatio = 1.20, durationYears = 350.0) {
    const phi0 = Math.max(0.02, Math.min(0.50, pyroclasticPorosity));
    const TC = Math.max(40.0, Math.min(350.0, hydrothermalTempC));
    const rAlFe = Math.max(0.05, Math.min(15.0, alFeCationRatio));
    const tYrs = Math.max(0.01, durationYears);

    const TK = TC + 273.15;
    const tSec = tYrs * 365.25 * 86400.0;
    const Rgas = 8.314;
    const Ea = 5.40e4; // 54 kJ/mol for acid-sulfate crystallization

    // Reaction rate constant
    const kRate = 4.2e-3 * Math.exp(-Ea / (Rgas * TK)) * Math.sqrt(rAlFe);
    const alphaSulfate = 1.0 - Math.exp(-Math.min(20.0, kRate * tSec));

    // Sulfate mineral yield (wt%)
    const wSulfatePct = alphaSulfate * 62.0;

    // Sulfate cementation and pore reduction
    const phiResidual = phi0 * (1.0 - (0.60 * alphaSulfate));
    const rhoGrain = 2880.0;
    const rhoBulk = ((1.0 - phiResidual) * rhoGrain) + (phiResidual * 1000.0);

    const kTherm = 1.95; // W/(m K)
    const Cspec = 840.0; // J/(kg K)
    const TIU = Math.sqrt(kTherm * rhoBulk * Cspec);

    let species = 'Intermediate Alunite-Jarosite Solid Solution';
    let sClass = 'Acid-Sulfate Fumarolic/Lagoon Alteration';

    if (rAlFe >= 2.5 && TC >= 120.0) {
      species = 'Potassium Alunite (KAl3(SO4)2(OH)6)';
      sClass = 'High-Temperature Acid-Sulfate Alunite Cap (Mawrth Vallis / Columbus Crater)';
    } else if (rAlFe <= 0.40) {
      species = 'Potassium Jarosite (KFe3(SO4)2(OH)6)';
      sClass = 'Low-to-Moderate Temperature Ferric Sulfate Strata (Meridiani / Candor Chasma)';
    } else {
      species = 'Mixed Al-Jarosite Solid Solution (K(Al,Fe)3(SO4)2(OH)6)';
      sClass = 'Transitional Alunite-Jarosite Acid-Sulfate Sequence';
    }

    return {
      acidSulfateConversionFraction: parseFloat(alphaSulfate.toFixed(3)),
      sulfateMineralYieldWeightPercent: parseFloat(wSulfatePct.toFixed(1)),
      dominantSulfateSpecies: species,
      induratedSulfateThermalInertiaTIU: parseFloat(TIU.toFixed(1)),
      acidSulfateFaciesClass: sClass,
      acidSulfateContext: `Acid Sulfate Facies at ${TC.toFixed(0)} C, Al/Fe=${rAlFe.toFixed(1)} (${(alphaSulfate * 100).toFixed(1)}% converted, ${wSulfatePct.toFixed(1)}% Sulfate, TIU=${TIU.toFixed(0)}, ${sClass})`
    };
  }

  /**
   * Calculate low-temperature alkaline/hydrothermal celadonite-glauconite green mica metasomatism of basalt vesicles, void infilling, and indurated metabasalt thermal inertia.
   * Basalt Voids + K+ + Fe3+ + Mg2+ + SiO2 + H2O (30-120 C) -> Celadonite (K(Mg,Fe2+)Fe3+(Si4O10)(OH)2) + Quartz
   * Reference: Ehlmann et al. (2011), Michalski et al. (2017), Viviano-Beck et al. (2014) for Martian Green Mica Subsurface Metasomatism.
   * @param {number} [vesicularBasaltPorosity=0.18] - Initial vesicular basalt porosity (0.02 to 0.40)
   * @param {number} [hydrothermalTempC=70.0] - Alteration fluid temperature in C (15 to 200 C)
   * @param {number} [fluidFeMgRatio=1.30] - Fluid Fe/Mg cation activity ratio (0.1 to 10.0)
   * @param {number} [durationYears=500.0] - Mica alteration duration in years (0.1 to 5000 yr)
   * @returns {{celadoniteConversionFraction: number, greenMicaYieldWeightPercent: number, dominantMicaSpecies: string, induratedMetabasaltThermalInertiaTIU: number, micaFaciesClass: string, celadoniteContext: string}}
   */
  static computeMartianCeladoniteGlauconiteMetasomatism(vesicularBasaltPorosity = 0.18, hydrothermalTempC = 70.0, fluidFeMgRatio = 1.30, durationYears = 500.0) {
    const phi0 = Math.max(0.01, Math.min(0.45, vesicularBasaltPorosity));
    const TC = Math.max(10.0, Math.min(250.0, hydrothermalTempC));
    const rFeMg = Math.max(0.05, Math.min(15.0, fluidFeMgRatio));
    const tYrs = Math.max(0.01, durationYears);

    const TK = TC + 273.15;
    const tSec = tYrs * 365.25 * 86400.0;
    const Rgas = 8.314;
    const Ea = 4.60e4; // 46 kJ/mol for celadonite mica crystallization

    // Reaction rate constant
    const kRate = 3.5e-3 * Math.exp(-Ea / (Rgas * TK)) * Math.sqrt(rFeMg);
    const alphaCel = 1.0 - Math.exp(-Math.min(20.0, kRate * tSec));

    // Green mica yield (wt%)
    const wMicaPct = alphaCel * 48.0;

    // Vesicle pore sealing and matrix induration
    const phiResidual = phi0 * (1.0 - (0.70 * alphaCel));
    const rhoGrain = 2950.0;
    const rhoBulk = ((1.0 - phiResidual) * rhoGrain) + (phiResidual * 1000.0);

    const kTherm = 2.15; // W/(m K)
    const Cspec = 820.0; // J/(kg K)
    const TIU = Math.sqrt(kTherm * rhoBulk * Cspec);

    let species = 'Ferric Celadonite Mica';
    let mClass = 'Vesicular Green Mica Infilling';

    if (rFeMg >= 1.0 && TC <= 100.0) {
      species = 'High-Fe Celadonite (K(Mg,Fe2+)Fe3+(Si4O10)(OH)2)';
      mClass = 'Low-Temperature Hydrothermal Celadonite Facies (Nili Fossae / Mawrth Basement)';
    } else if (rFeMg < 1.0 && TC <= 60.0) {
      species = 'Marine/Playa Glauconite Mica (K(Fe3+,Al,Mg)2(Si,Al)4O10(OH)2)';
      mClass = 'Authigenic Lacustrine/Playa Glauconite Strata (Eridania Basin / Gale Crater)';
    } else {
      species = 'Aluminous Illite-Smectite-Celadonite Transition';
      mClass = 'Intermediate Subsurface Hydrothermal Phyllosilicate Cap';
    }

    return {
      celadoniteConversionFraction: parseFloat(alphaCel.toFixed(3)),
      greenMicaYieldWeightPercent: parseFloat(wMicaPct.toFixed(1)),
      dominantMicaSpecies: species,
      induratedMetabasaltThermalInertiaTIU: parseFloat(TIU.toFixed(1)),
      micaFaciesClass: mClass,
      celadoniteContext: `Celadonite Mica at ${TC.toFixed(0)} C, Fe/Mg=${rFeMg.toFixed(1)} (${(alphaCel * 100).toFixed(1)}% converted, ${wMicaPct.toFixed(1)}% Mica, TIU=${TIU.toFixed(0)}, ${mClass})`
    };
  }

  /**
   * Calculate extreme acid-sulfate-phosphate hydrothermal alteration of aluminous crust into aluminium-phosphate-sulfate (APS) woodhouseite, quartz sinter, and indurated sinter crust thermal inertia.
   * Aluminous Crust + Ca2+ + H2PO4- + SO4(2-) + H2O (140-280 C) -> Woodhouseite (CaAl3(PO4)(SO4)(OH)6) + Quartz Sinter
   * Reference: Ehlmann et al. (2016), Viviano-Beck et al. (2014) for Martian APS (Alunite-Woodhouseite) Hydrothermal Formations.
   * @param {number} [porousRhyolitePorosity=0.22] - Initial rhyolitic/aluminous ash porosity (0.05 to 0.45)
   * @param {number} [hydrothermalTempC=210.0] - Hydrothermal fluid temperature in C (100 to 350 C)
   * @param {number} [phosphateSulfateRatio=1.10] - Fluid PO4/SO4 activity ratio (0.1 to 10.0)
   * @param {number} [durationYears=300.0] - APS alteration duration in years (0.1 to 5000 yr)
   * @returns {{apsConversionFraction: number, woodhouseiteYieldWeightPercent: number, dominantAPSMineralSpecies: string, induratedAPSThermalInertiaTIU: number, apsFaciesClass: string, apsContext: string}}
   */
  static computeMartianWoodhouseitePhosphateSulfateKinetics(porousRhyolitePorosity = 0.22, hydrothermalTempC = 210.0, phosphateSulfateRatio = 1.10, durationYears = 300.0) {
    const phi0 = Math.max(0.02, Math.min(0.50, porousRhyolitePorosity));
    const TC = Math.max(80.0, Math.min(400.0, hydrothermalTempC));
    const rPS = Math.max(0.05, Math.min(15.0, phosphateSulfateRatio));
    const tYrs = Math.max(0.01, durationYears);

    const TK = TC + 273.15;
    const tSec = tYrs * 365.25 * 86400.0;
    const Rgas = 8.314;
    const Ea = 5.60e4; // 56 kJ/mol for APS woodhouseite crystallization

    // Reaction rate constant
    const kRate = 4.8e-3 * Math.exp(-Ea / (Rgas * TK)) * Math.pow(rPS, 0.40);
    const alphaAPS = 1.0 - Math.exp(-Math.min(20.0, kRate * tSec));

    // Woodhouseite yield (wt%)
    const wAPSPct = alphaAPS * 58.0;

    // Pore reduction and silica-APS cementation
    const phiResidual = phi0 * (1.0 - (0.65 * alphaAPS));
    const rhoGrain = 3050.0;
    const rhoBulk = ((1.0 - phiResidual) * rhoGrain) + (phiResidual * 1000.0);

    const kTherm = 2.35; // W/(m K)
    const Cspec = 830.0; // J/(kg K)
    const TIU = Math.sqrt(kTherm * rhoBulk * Cspec);

    let species = 'Woodhouseite APS Phase';
    let aClass = 'Pervasive Aluminium-Phosphate-Sulfate (APS) Sinter';

    if (rPS >= 1.5 && TC >= 160.0) {
      species = 'Woodhouseite-Svanbergite (Ca,Sr)Al3(PO4)(SO4)(OH)6';
      aClass = 'High-Temperature Hydrothermal APS Sinter Facies (Mawrth / Columbus Crater)';
    } else if (rPS < 0.50) {
      species = 'Phosphate-Bearing Alunite (KAl3(SO4,PO4)2(OH)6)';
      aClass = 'Transitional Alunite-Woodhouseite Acid-Sulfate Cap';
    } else {
      species = 'Equilibrated Woodhouseite-Alunite Solid Solution';
      aClass = 'Acid Magmatic Volatiles Hydrothermal Sequence';
    }

    return {
      apsConversionFraction: parseFloat(alphaAPS.toFixed(3)),
      woodhouseiteYieldWeightPercent: parseFloat(wAPSPct.toFixed(1)),
      dominantAPSMineralSpecies: species,
      induratedAPSThermalInertiaTIU: parseFloat(TIU.toFixed(1)),
      apsFaciesClass: aClass,
      apsContext: `Woodhouseite APS at ${TC.toFixed(0)} C, P/S=${rPS.toFixed(1)} (${(alphaAPS * 100).toFixed(1)}% converted, ${wAPSPct.toFixed(1)}% Woodhouseite, TIU=${TIU.toFixed(0)}, ${aClass})`
    };
  }

  /**
   * Calculate hydrothermal CO2 metasomatism of ultramafic serpentinite into talc-magnesite soapstone assemblage, void occlusion, and indurated soapstone thermal inertia.
   * 2 Antigorite + 3 CO2 (180-360 C) -> Talc (Mg3Si4O10(OH)2) + 3 Magnesite (MgCO3) + 3 H2O
   * Reference: Brown et al. (2010), Ehlmann et al. (2010), Viviano-Beck et al. (2014) for Martian Talc-Carbonate Metasomatism in Nili Fossae.
   * @param {number} [ultramaficSerpentinitePorosity=0.15] - Initial serpentinized ultramafic rock porosity (0.02 to 0.40)
   * @param {number} [hydrothermalTempC=270.0] - Hydrothermal fluid temperature in C (140 to 420 C)
   * @param {number} [co2FluidMoleFraction=0.12] - Dissolved CO2 fluid mole fraction (0.01 to 0.50)
   * @param {number} [durationYears=600.0] - Metasomatic alteration duration in years (0.1 to 5000 yr)
   * @returns {{talcCarbonateConversionFraction: number, soapstoneYieldWeightPercent: number, dominantSoapstoneSpecies: string, induratedSoapstoneThermalInertiaTIU: number, soapstoneFaciesClass: string, talcCarbonateContext: string}}
   */
  static computeMartianTalcCarbonateMetasomatism(ultramaficSerpentinitePorosity = 0.15, hydrothermalTempC = 270.0, co2FluidMoleFraction = 0.12, durationYears = 600.0) {
    const phi0 = Math.max(0.01, Math.min(0.45, ultramaficSerpentinitePorosity));
    const TC = Math.max(100.0, Math.min(450.0, hydrothermalTempC));
    const xCO2 = Math.max(0.005, Math.min(0.80, co2FluidMoleFraction));
    const tYrs = Math.max(0.01, durationYears);

    const TK = TC + 273.15;
    const tSec = tYrs * 365.25 * 86400.0;
    const Rgas = 8.314;
    const Ea = 6.20e4; // 62 kJ/mol for talc-carbonate carbonation

    // Reaction rate constant
    const kRate = 5.2e-3 * Math.exp(-Ea / (Rgas * TK)) * Math.sqrt(xCO2);
    const alphaTC = 1.0 - Math.exp(-Math.min(20.0, kRate * tSec));

    // Soapstone yield (wt%)
    const wSoapPct = alphaTC * 74.0;

    // Void occlusion and soapstone matrix consolidation
    const phiResidual = phi0 * (1.0 - (0.80 * alphaTC));
    const rhoGrain = 2980.0;
    const rhoBulk = ((1.0 - phiResidual) * rhoGrain) + (phiResidual * 1000.0);

    const kTherm = 2.65; // W/(m K)
    const Cspec = 870.0; // J/(kg K)
    const TIU = Math.sqrt(kTherm * rhoBulk * Cspec);

    let species = 'Talc-Magnesite Soapstone Assemblage';
    let sClass = 'Pervasive Ultramafic Talc-Carbonate Metasomatism';

    if (xCO2 >= 0.08 && TC >= 220.0 && TC <= 340.0) {
      species = 'Talc + Magnesite + Quartz (Soapstone)';
      sClass = 'Pervasive Talc-Carbonate Hydrothermal Facies (Nili Fossae / Isidis Rim)';
    } else if (TC > 340.0) {
      species = 'Anthophyllite-Magnesite High-T Facies';
      sClass = 'High-Grade Metamorphic Ultramafic Transition';
    } else {
      species = 'Incipient Serpentine-Talc Carbonation';
      sClass = 'Low-X(CO2) Serpentinite Weathering Boundary';
    }

    return {
      talcCarbonateConversionFraction: parseFloat(alphaTC.toFixed(3)),
      soapstoneYieldWeightPercent: parseFloat(wSoapPct.toFixed(1)),
      dominantSoapstoneSpecies: species,
      induratedSoapstoneThermalInertiaTIU: parseFloat(TIU.toFixed(1)),
      soapstoneFaciesClass: sClass,
      talcCarbonateContext: `Talc-Carbonate Soapstone at ${TC.toFixed(0)} C, X(CO2)=${xCO2.toFixed(2)} (${(alphaTC * 100).toFixed(1)}% converted, ${wSoapPct.toFixed(1)}% Soapstone, TIU=${TIU.toFixed(0)}, ${sClass})`
    };
  }

  /**
   * Calculate high-temperature pneumatolytic greisenization of evolved felsic crust into topaz-quartz greisen, matrix induration, and thermal inertia.
   * Felsic Crust + HF + H2O (350-550 C) -> Topaz (Al2SiO4(F,OH)2) + Zinnwaldite Mica + Quartz Greisen
   * Reference: Viviano-Beck et al. (2014), Ehlmann et al. (2016), Carter et al. (2013) for Martian High-Temperature Pneumatolytic Alteration.
   * @param {number} [leucogranitePorosity=0.14] - Initial evolved granitic/felsic crust porosity (0.02 to 0.40)
   * @param {number} [pneumatolyticTempC=420.0] - Metasomatic fluid temperature in C (250 to 650 C)
   * @param {number} [fluorineActivityHF=0.15] - Fluid HF activity / fluorine fugacity ratio (0.01 to 0.80)
   * @param {number} [durationYears=400.0] - Pneumatolytic alteration duration in years (0.1 to 5000 yr)
   * @returns {{greisenConversionFraction: number, topazYieldWeightPercent: number, dominantGreisenSpecies: string, induratedGreisenThermalInertiaTIU: number, greisenFaciesClass: string, topazContext: string}}
   */
  static computeMartianTopazGreisenMetasomatism(leucogranitePorosity = 0.14, pneumatolyticTempC = 420.0, fluorineActivityHF = 0.15, durationYears = 400.0) {
    const phi0 = Math.max(0.01, Math.min(0.45, leucogranitePorosity));
    const TC = Math.max(200.0, Math.min(700.0, pneumatolyticTempC));
    const aHF = Math.max(0.005, Math.min(1.0, fluorineActivityHF));
    const tYrs = Math.max(0.01, durationYears);

    const TK = TC + 273.15;
    const tSec = tYrs * 365.25 * 86400.0;
    const Rgas = 8.314;
    const Ea = 6.40e4; // 64 kJ/mol for greisen topaz crystallization

    // Reaction rate constant
    const kRate = 5.5e-3 * Math.exp(-Ea / (Rgas * TK)) * Math.sqrt(aHF);
    const alphaGreisen = 1.0 - Math.exp(-Math.min(20.0, kRate * tSec));

    // Topaz yield (wt%)
    const wTopazPct = alphaGreisen * 52.0;

    // Quartz-topaz greisen void infilling
    const phiResidual = phi0 * (1.0 - (0.80 * alphaGreisen));
    const rhoGrain = 3100.0;
    const rhoBulk = ((1.0 - phiResidual) * rhoGrain) + (phiResidual * 1000.0);

    const kTherm = 2.95; // W/(m K)
    const Cspec = 840.0; // J/(kg K)
    const TIU = Math.sqrt(kTherm * rhoBulk * Cspec);

    let species = 'Fluor-Topaz + Quartz Greisen';
    let gClass = 'Pneumatolytic Topaz Greisen Facies';

    if (aHF >= 0.10 && TC >= 350.0 && TC <= 550.0) {
      species = 'Fluor-Topaz (Al2SiO4(F,OH)2) + Quartz';
      gClass = 'High-Temperature Pneumatolytic Topaz Greisen (Syrtis Major / Terra Sirenum)';
    } else if (TC > 550.0) {
      species = 'Andalusite-Corundum Pyrometamorphic Facies';
      gClass = 'Magmatic Contact Skarn Transition';
    } else {
      species = 'Incipient Sericitic / Muscovite Greisen';
      gClass = 'Low-Temperature Hydrothermal Greisen Margin';
    }

    return {
      greisenConversionFraction: parseFloat(alphaGreisen.toFixed(3)),
      topazYieldWeightPercent: parseFloat(wTopazPct.toFixed(1)),
      dominantGreisenSpecies: species,
      induratedGreisenThermalInertiaTIU: parseFloat(TIU.toFixed(1)),
      greisenFaciesClass: gClass,
      topazContext: `Topaz Greisen at ${TC.toFixed(0)} C, a(HF)=${aHF.toFixed(2)} (${(alphaGreisen * 100).toFixed(1)}% converted, ${wTopazPct.toFixed(1)}% Topaz, TIU=${TIU.toFixed(0)}, ${gClass})`
    };
  }

  /**
   * Calculate high-temperature acid-hydrothermal advanced argillic alteration of aluminous crust into pyrophyllite-quartz sinter, silica induration, and thermal inertia.
   * Kaolinite + 2 SiO2 (280-450 C, pH 1.5-3.5) -> Pyrophyllite (Al2Si4O10(OH)2) + Quartz Sinter + H2O
   * Reference: Ehlmann et al. (2009), Viviano-Beck et al. (2014), Carter et al. (2013) for Martian Advanced Argillic Hydrothermal Systems.
   * @param {number} [aluminousBasaltPorosity=0.20] - Initial aluminous ash/crust porosity (0.02 to 0.45)
   * @param {number} [hydrothermalTempC=320.0] - Hydrothermal fluid temperature in C (200 to 550 C)
   * @param {number} [fluidAcidityPH=2.5] - Fluid acidity pH (1.0 to 6.0)
   * @param {number} [durationYears=450.0] - Hydrothermal alteration duration in years (0.1 to 5000 yr)
   * @returns {{pyrophylliteConversionFraction: number, pyrophylliteYieldWeightPercent: number, dominantArgillicSpecies: string, induratedArgillicThermalInertiaTIU: number, advancedArgillicFaciesClass: string, pyrophylliteContext: string}}
   */
  static computeMartianPyrophylliteArgillicMetasomatism(aluminousBasaltPorosity = 0.20, hydrothermalTempC = 320.0, fluidAcidityPH = 2.5, durationYears = 450.0) {
    const phi0 = Math.max(0.01, Math.min(0.50, aluminousBasaltPorosity));
    const TC = Math.max(150.0, Math.min(600.0, hydrothermalTempC));
    const pH = Math.max(0.5, Math.min(7.0, fluidAcidityPH));
    const tYrs = Math.max(0.01, durationYears);

    const TK = TC + 273.15;
    const tSec = tYrs * 365.25 * 86400.0;
    const Rgas = 8.314;
    const Ea = 6.00e4; // 60 kJ/mol for pyrophyllite crystallization

    const aHPlus = Math.pow(10.0, -pH);
    // Reaction rate constant
    const kRate = 5.0e-3 * Math.exp(-Ea / (Rgas * TK)) * Math.pow(aHPlus * 1000.0, 0.35);
    const alphaPyro = 1.0 - Math.exp(-Math.min(20.0, kRate * tSec));

    // Pyrophyllite yield (wt%)
    const wPyroPct = alphaPyro * 65.0;

    // Quartz sinter cementation and pore compaction
    const phiResidual = phi0 * (1.0 - (0.70 * alphaPyro));
    const rhoGrain = 2840.0;
    const rhoBulk = ((1.0 - phiResidual) * rhoGrain) + (phiResidual * 1000.0);

    const kTherm = 2.75; // W/(m K)
    const Cspec = 880.0; // J/(kg K)
    const TIU = Math.sqrt(kTherm * rhoBulk * Cspec);

    let species = 'Pyrophyllite + Quartz Sinter';
    let aClass = 'Advanced Argillic Pyrophyllite Facies';

    if (alphaPyro >= 0.50 && TC >= 280.0 && TC <= 450.0 && pH <= 3.5) {
      species = 'Pyrophyllite (Al2Si4O10(OH)2) + Diaspore + Quartz';
      aClass = 'High-Temperature Advanced Argillic Hydrothermal Facies (Nili Fossae / Toro Crater)';
    } else if (TC > 450.0) {
      species = 'Andalusite-Quartz High-T Metamorphic Facies';
      aClass = 'High-Temperature Metamorphic Hornfels Transition';
    } else {
      species = 'Dickite-Kaolinite Intermediate Argillic Sinter';
      aClass = 'Moderate-Temperature Acid Argillic Boundary';
    }

    return {
      pyrophylliteConversionFraction: parseFloat(alphaPyro.toFixed(3)),
      pyrophylliteYieldWeightPercent: parseFloat(wPyroPct.toFixed(1)),
      dominantArgillicSpecies: species,
      induratedArgillicThermalInertiaTIU: parseFloat(TIU.toFixed(1)),
      advancedArgillicFaciesClass: aClass,
      pyrophylliteContext: `Pyrophyllite Facies at ${TC.toFixed(0)} C, pH=${pH.toFixed(1)} (${(alphaPyro * 100).toFixed(1)}% converted, ${wPyroPct.toFixed(1)}% Pyrophyllite, TIU=${TIU.toFixed(0)}, ${aClass})`
    };
  }

  /**
   * Calculate high-temperature hydrothermal metasomatism of calcic anorthositic crust into brittle calcium-mica margarite, recrystallization, and thermal inertia.
   * Anorthite + Corundum + H2O (300-480 C) -> Margarite (CaAl2(Al2Si2O10)(OH)2) + Quartz Sinter
   * Reference: Carter et al. (2013), Viviano-Beck et al. (2014), Ehlmann et al. (2016) for Martian Calcic Mica Metasomatism.
   * @param {number} [anorthositePorosity=0.15] - Initial anorthositic/calcic crust porosity (0.02 to 0.45)
   * @param {number} [hydrothermalTempC=360.0] - Hydrothermal fluid temperature in C (220 to 580 C)
   * @param {number} [calciumActivityRatio=0.18] - Dissolved Ca2+/H+ activity ratio (0.01 to 0.80)
   * @param {number} [durationYears=500.0] - Metasomatic alteration duration in years (0.1 to 5000 yr)
   * @returns {{margariteConversionFraction: number, margariteYieldWeightPercent: number, dominantMicaSpecies: string, induratedMicaThermalInertiaTIU: number, micaFaciesClass: string, margariteContext: string}}
   */
  static computeMartianMargariteMetasomatism(anorthositePorosity = 0.15, hydrothermalTempC = 360.0, calciumActivityRatio = 0.18, durationYears = 500.0) {
    const phi0 = Math.max(0.01, Math.min(0.50, anorthositePorosity));
    const TC = Math.max(180.0, Math.min(650.0, hydrothermalTempC));
    const aCa = Math.max(0.005, Math.min(1.0, calciumActivityRatio));
    const tYrs = Math.max(0.01, durationYears);

    const TK = TC + 273.15;
    const tSec = tYrs * 365.25 * 86400.0;
    const Rgas = 8.314;
    const Ea = 6.20e4; // 62 kJ/mol for margarite crystallization

    // Reaction rate constant
    const kRate = 5.2e-3 * Math.exp(-Ea / (Rgas * TK)) * Math.pow(aCa, 0.35);
    const alphaMarg = 1.0 - Math.exp(-Math.min(20.0, kRate * tSec));

    // Margarite yield (wt%)
    const wMargPct = alphaMarg * 58.0;

    // Metasomatic recrystallization and porosity reduction
    const phiResidual = phi0 * (1.0 - (0.75 * alphaMarg));
    const rhoGrain = 2980.0;
    const rhoBulk = ((1.0 - phiResidual) * rhoGrain) + (phiResidual * 1000.0);

    const kTherm = 2.90; // W/(m K)
    const Cspec = 870.0; // J/(kg K)
    const TIU = Math.sqrt(kTherm * rhoBulk * Cspec);

    let species = 'Margarite + Quartz Assemblage';
    let mClass = 'Calcic Brittle-Mica Margarite Facies';

    if (alphaMarg >= 0.50 && TC >= 300.0 && TC <= 480.0 && aCa >= 0.10) {
      species = 'Margarite (CaAl2(Al2Si2O10)(OH)2) + Corundum';
      mClass = 'High-Temperature Calcic Mica Hydrothermal Facies (Nili Fossae / Claritas Fossae)';
    } else if (TC > 480.0) {
      species = 'Anorthite-Corundum Pyrometamorphic Facies';
      mClass = 'Granulite-Grade Pyrometamorphic Hornfels';
    } else {
      species = 'Incipient Prehnite-Margarite Boundary';
      mClass = 'Moderate-Temperature Sub-Greenschist Boundary';
    }

    return {
      margariteConversionFraction: parseFloat(alphaMarg.toFixed(3)),
      margariteYieldWeightPercent: parseFloat(wMargPct.toFixed(1)),
      dominantMicaSpecies: species,
      induratedMicaThermalInertiaTIU: parseFloat(TIU.toFixed(1)),
      micaFaciesClass: mClass,
      margariteContext: `Margarite Facies at ${TC.toFixed(0)} C, a(Ca)=${aCa.toFixed(2)} (${(alphaMarg * 100).toFixed(1)}% converted, ${wMargPct.toFixed(1)}% Margarite, TIU=${TIU.toFixed(0)}, ${mClass})`
    };
  }

  /**
   * Calculate low-temperature alkaline/neutral zeolitization of basaltic glass into high-silica stilbite-heulandite zeolite, channel hydration, and zeolitic tuff thermal inertia.
   * Basaltic Glass + Ca2+ + Na+ + SiO2(aq) + H2O (60-150 C) -> Stilbite (NaCa4(Si27Al9)O72·28H2O) + Heulandite
   * Reference: Viviano-Beck et al. (2014), Ehlmann et al. (2011), Carter et al. (2013) for Martian Zeolitic Hydrothermal Terranes.
   * @param {number} [basalticTuffPorosity=0.28] - Initial volcanic ash/tuff porosity (0.05 to 0.50)
   * @param {number} [zeoliticTempC=95.0] - Low-temperature hydrothermal fluid temperature in C (40 to 220 C)
   * @param {number} [silicaActivityRatio=0.16] - Dissolved silica activity / aqueous SiO2 ratio (0.01 to 0.80)
   * @param {number} [durationYears=300.0] - Zeolitic alteration duration in years (0.1 to 5000 yr)
   * @returns {{zeoliteConversionFraction: number, channelWaterYieldWeightPercent: number, dominantZeoliteSpecies: string, zeoliticTuffThermalInertiaTIU: number, zeoliteFaciesClass: string, stilbiteContext: string}}
   */
  static computeMartianStilbiteZeoliteMetasomatism(basalticTuffPorosity = 0.28, zeoliticTempC = 95.0, silicaActivityRatio = 0.16, durationYears = 300.0) {
    const phi0 = Math.max(0.01, Math.min(0.55, basalticTuffPorosity));
    const TC = Math.max(30.0, Math.min(260.0, zeoliticTempC));
    const aSiO2 = Math.max(0.005, Math.min(1.0, silicaActivityRatio));
    const tYrs = Math.max(0.01, durationYears);

    const TK = TC + 273.15;
    const tSec = tYrs * 365.25 * 86400.0;
    const Rgas = 8.314;
    const Ea = 4.80e4; // 48 kJ/mol for stilbite zeolite crystallization

    // Reaction rate constant
    const kRate = 4.2e-3 * Math.exp(-Ea / (Rgas * TK)) * Math.pow(aSiO2, 0.35);
    const alphaZ = 1.0 - Math.exp(-Math.min(20.0, kRate * tSec));

    // Channel water yield (wt%)
    const wH2OPct = alphaZ * 16.50;

    // Zeolitic pore infilling and hydrous matrix consolidation
    const phiResidual = phi0 * (1.0 - (0.65 * alphaZ));
    const rhoGrain = 2280.0;
    const rhoBulk = ((1.0 - phiResidual) * rhoGrain) + (phiResidual * 1000.0);

    const kTherm = 1.85; // W/(m K)
    const Cspec = 940.0; // J/(kg K)
    const TIU = Math.sqrt(kTherm * rhoBulk * Cspec);

    let species = 'Stilbite + Heulandite Zeolite Assemblage';
    let zClass = 'High-Silica Zeolite Facies';

    if (alphaZ >= 0.50 && TC >= 60.0 && TC <= 150.0 && aSiO2 >= 0.10) {
      species = 'Stilbite (NaCa4(Si27Al9)O72·28H2O)';
      zClass = 'Low-Temperature High-Silica Zeolite Facies (Claritas Fossae / Terra Sirenum)';
    } else if (TC > 150.0) {
      species = 'Analcime-Wairakite High-T Zeolite Facies';
      zClass = 'Moderate-Temperature Zeolite Transition';
    } else {
      species = 'Chabazite-Clinoptilolite Incipient Zeolite';
      zClass = 'Low-Temperature Diagenetic Zeolitic Boundary';
    }

    return {
      zeoliteConversionFraction: parseFloat(alphaZ.toFixed(3)),
      channelWaterYieldWeightPercent: parseFloat(wH2OPct.toFixed(2)),
      dominantZeoliteSpecies: species,
      zeoliticTuffThermalInertiaTIU: parseFloat(TIU.toFixed(1)),
      zeoliteFaciesClass: zClass,
      stilbiteContext: `Stilbite Zeolite at ${TC.toFixed(0)} C, a(SiO2)=${aSiO2.toFixed(2)} (${(alphaZ * 100).toFixed(1)}% converted, ${wH2OPct.toFixed(2)}% channel H2O, TIU=${TIU.toFixed(0)}, ${zClass})`
    };
  }

  /**
   * Calculate atmospheric desiccation/dehydration kinetics of ferrous polyhydrate sulfates into szomolnokite monohydrate, evaporite cementation, and thermal inertia.
   * FeSO4·4H2O (Rozenite) -> FeSO4·H2O (Szomolnokite) + 3 H2O (g) (15-45 C, low RH)
   * Reference: Bishop et al. (2009), Roach et al. (2010), Viviano-Beck et al. (2014) for Martian Monohydrate Sulfate Deposits in Valles Marineris.
   * @param {number} [initialRozenitePorosity=0.25] - Initial polyhydrate sulfate evaporite porosity (0.05 to 0.50)
   * @param {number} [surfaceDesiccationTempC=25.0] - Surface/diurnal desiccation temperature in C (0 to 65 C)
   * @param {number} [relativeHumidityFraction=0.08] - Near-surface atmospheric relative humidity (0.001 to 0.80)
   * @param {number} [durationYears=200.0] - Desiccation exposure duration in years (0.1 to 5000 yr)
   * @returns {{szomolnokiteConversionFraction: number, monohydrateYieldWeightPercent: number, dominantSulfateSpecies: string, induratedMonohydrateThermalInertiaTIU: number, monohydrateFaciesClass: string, szomolnokiteContext: string}}
   */
  static computeMartianSzomolnokiteKinetics(initialRozenitePorosity = 0.25, surfaceDesiccationTempC = 25.0, relativeHumidityFraction = 0.08, durationYears = 200.0) {
    const phi0 = Math.max(0.01, Math.min(0.55, initialRozenitePorosity));
    const TC = Math.max(-10.0, Math.min(80.0, surfaceDesiccationTempC));
    const rh = Math.max(0.001, Math.min(0.95, relativeHumidityFraction));
    const tYrs = Math.max(0.01, durationYears);

    const TK = TC + 273.15;
    const tSec = tYrs * 365.25 * 86400.0;
    const Rgas = 8.314;
    const Ea = 4.20e4; // 42 kJ/mol for rozenite dehydration

    // Reaction rate constant
    const kRate = 6.5e-3 * Math.exp(-Ea / (Rgas * TK)) * Math.sqrt(1.0 - Math.min(0.90, rh));
    const alphaSzom = 1.0 - Math.exp(-Math.min(20.0, kRate * tSec));

    // Monohydrate yield (wt%)
    const wSzomPct = alphaSzom * 76.0;

    // Structural shrinkage and evaporite matrix cementation
    const phiResidual = phi0 * (1.0 - (0.60 * alphaSzom));
    const rhoGrain = 2820.0;
    const rhoBulk = ((1.0 - phiResidual) * rhoGrain) + (phiResidual * 1000.0);

    const kTherm = 2.10; // W/(m K)
    const Cspec = 820.0; // J/(kg K)
    const TIU = Math.sqrt(kTherm * rhoBulk * Cspec);

    let species = 'Szomolnokite Monohydrate (FeSO4·H2O)';
    let sClass = 'Ferrous Monohydrate Sulfate Strata';

    if (alphaSzom >= 0.50 && rh <= 0.20) {
      species = 'Szomolnokite (FeSO4·H2O) + Kieserite';
      sClass = 'Indurated Monohydrate Sulfate Facies (Juventae / Candor Chasma / Aram Chaos)';
    } else if (rh > 0.40) {
      species = 'Rozenite-Melanterite Polyhydrate Equilibrium';
      sClass = 'Polyhydrate-Rich Hydrated Sulfate Sequence';
    } else {
      species = 'Incipient Rozenite Dehydration Layer';
      sClass = 'Partially Dehydrated Sulfate Crust';
    }

    return {
      szomolnokiteConversionFraction: parseFloat(alphaSzom.toFixed(3)),
      monohydrateYieldWeightPercent: parseFloat(wSzomPct.toFixed(1)),
      dominantSulfateSpecies: species,
      induratedMonohydrateThermalInertiaTIU: parseFloat(TIU.toFixed(1)),
      monohydrateFaciesClass: sClass,
      szomolnokiteContext: `Szomolnokite at ${TC.toFixed(0)} C, RH=${rh.toFixed(2)} (${(alphaSzom * 100).toFixed(1)}% converted, ${wSzomPct.toFixed(1)}% Szomolnokite, TIU=${TIU.toFixed(0)}, ${sClass})`
    };
  }

  /**
   * Calculate atmospheric desiccation and phase transition kinetics of sodium sulfate decahydrate (mirabilite) into anhydrous thenardite, volume collapse, and thermal inertia.
   * Na2SO4·10H2O (Mirabilite) -> Na2SO4 (Thenardite) + 10 H2O (g) (5-35 C, low RH)
   * Reference: Vaniman et al. (2004), Rodriguez et al. (2014), Viviano-Beck et al. (2014) for Martian Sodium Sulfate Evaporites.
   * @param {number} [initialMirabilitePorosity=0.32] - Initial mirabilite evaporite sediment porosity (0.05 to 0.55)
   * @param {number} [evaporiteTempC=20.0] - Evaporite surface temperature in C (-5 to 55 C)
   * @param {number} [relativeHumidityFraction=0.05] - Near-surface atmospheric relative humidity (0.001 to 0.80)
   * @param {number} [durationYears=150.0] - Exposure duration in years (0.1 to 5000 yr)
   * @returns {{thenarditeConversionFraction: number, waterLossWeightPercent: number, dominantSulfateSpecies: string, desiccatedThenarditeThermalInertiaTIU: number, sulfateFaciesClass: string, thenarditeContext: string}}
   */
  static computeMartianMirabiliteThenarditeKinetics(initialMirabilitePorosity = 0.32, evaporiteTempC = 20.0, relativeHumidityFraction = 0.05, durationYears = 150.0) {
    const phi0 = Math.max(0.01, Math.min(0.60, initialMirabilitePorosity));
    const TC = Math.max(-15.0, Math.min(70.0, evaporiteTempC));
    const rh = Math.max(0.001, Math.min(0.95, relativeHumidityFraction));
    const tYrs = Math.max(0.01, durationYears);

    const TK = TC + 273.15;
    const tSec = tYrs * 365.25 * 86400.0;
    const Rgas = 8.314;
    const Ea = 4.00e4; // 40 kJ/mol for mirabilite dehydration

    // Reaction rate constant
    const kRate = 7.2e-3 * Math.exp(-Ea / (Rgas * TK)) * Math.pow(1.0 - Math.min(0.90, rh), 0.45);
    const alphaThen = 1.0 - Math.exp(-Math.min(20.0, kRate * tSec));

    // Water loss (wt%)
    const wLossPct = alphaThen * 55.9;

    // Structural collapse, pulverization, and microporosity generation
    const phiResidual = phi0 + (0.12 * alphaThen);
    const rhoGrain = 2660.0;
    const rhoBulk = ((1.0 - phiResidual) * rhoGrain) + (phiResidual * 1000.0);

    const kTherm = 1.65; // W/(m K)
    const Cspec = 890.0; // J/(kg K)
    const TIU = Math.sqrt(kTherm * rhoBulk * Cspec);

    let species = 'Anhydrous Thenardite (Na2SO4)';
    let sClass = 'Desiccated Sodium Sulfate Evaporite';

    if (alphaThen >= 0.50 && rh <= 0.15) {
      species = 'Anhydrous Thenardite (Na2SO4) Powder';
      sClass = 'Anhydrous Sodium Sulfate Facies (Columbus Crater / Noctis Labyrinthus / Juventae)';
    } else if (rh > 0.35) {
      species = 'Mirabilite Decahydrate (Na2SO4·10H2O) Equilibrium';
      sClass = 'Hydrated Mirabilite Cryogenic Evaporite Crust';
    } else {
      species = 'Incipient Mirabilite-Thenardite Transition';
      sClass = 'Partially Dehydrated Mirabilite Sequence';
    }

    return {
      thenarditeConversionFraction: parseFloat(alphaThen.toFixed(3)),
      waterLossWeightPercent: parseFloat(wLossPct.toFixed(1)),
      dominantSulfateSpecies: species,
      desiccatedThenarditeThermalInertiaTIU: parseFloat(TIU.toFixed(1)),
      sulfateFaciesClass: sClass,
      thenarditeContext: `Thenardite at ${TC.toFixed(0)} C, RH=${rh.toFixed(2)} (${(alphaThen * 100).toFixed(1)}% converted, ${wLossPct.toFixed(1)}% H2O loss, TIU=${TIU.toFixed(0)}, ${sClass})`
    };
  }

  /**
   * Calculate hydrothermal alteration and nanotubular crystallization kinetics of weathered volcanic ash into hydrated halloysite (10 A / 7 A), lumen porosity, and claystone thermal inertia.
   * Volcanic Ash + Al3+ + SiO2(aq) + H2O (80-180 C) -> Halloysite (Al2Si2O5(OH)4·2H2O) (Nanotubes)
   * Reference: Ehlmann et al. (2011), Bishop et al. (2013), Viviano-Beck et al. (2014) for Martian Kaolin-Group Nanominerals.
   * @param {number} [initialPorousAshPorosity=0.35] - Initial volcanic ash/glass porosity (0.05 to 0.55)
   * @param {number} [hydrothermalTempC=120.0] - Hydrothermal alteration temperature in C (50 to 240 C)
   * @param {number} [interlayerHydrationRatio=0.85] - Interlayer aqueous activity / hydration ratio (0.05 to 1.0)
   * @param {number} [durationYears=250.0] - Alteration duration in years (0.1 to 5000 yr)
   * @returns {{halloysiteConversionFraction: number, boundWaterYieldWeightPercent: number, dominantKaolinSpecies: string, claystoneThermalInertiaTIU: number, kaolinFaciesClass: string, halloysiteContext: string}}
   */
  static computeMartianHalloysiteKinetics(initialPorousAshPorosity = 0.35, hydrothermalTempC = 120.0, interlayerHydrationRatio = 0.85, durationYears = 250.0) {
    const phi0 = Math.max(0.01, Math.min(0.60, initialPorousAshPorosity));
    const TC = Math.max(40.0, Math.min(260.0, hydrothermalTempC));
    const aH2O = Math.max(0.01, Math.min(1.0, interlayerHydrationRatio));
    const tYrs = Math.max(0.01, durationYears);

    const TK = TC + 273.15;
    const tSec = tYrs * 365.25 * 86400.0;
    const Rgas = 8.314;
    const Ea = 4.60e4; // 46 kJ/mol for halloysite crystallization

    // Reaction rate constant
    const kRate = 5.6e-3 * Math.exp(-Ea / (Rgas * TK)) * Math.pow(aH2O, 0.40);
    const alphaHal = 1.0 - Math.exp(-Math.min(20.0, kRate * tSec));

    // Bound water yield (wt%)
    const wH2OPct = alphaHal * 13.90;

    // Nanotube lumen formation and intraparticle claystone porosity
    const phiResidual = (phi0 * (1.0 - (0.45 * alphaHal))) + (0.08 * alphaHal);
    const rhoGrain = 2180.0;
    const rhoBulk = ((1.0 - phiResidual) * rhoGrain) + (phiResidual * 1000.0);

    const kTherm = 1.25; // W/(m K)
    const Cspec = 960.0; // J/(kg K)
    const TIU = Math.sqrt(kTherm * rhoBulk * Cspec);

    let species = 'Nanotubular Hydrated Halloysite-10A';
    let kClass = 'Hydrated Kaolin Nanomaterial Facies';

    if (alphaHal >= 0.50 && TC >= 80.0 && TC <= 180.0 && aH2O >= 0.50) {
      species = 'Hydrated Halloysite-10A (Al2Si2O5(OH)4·2H2O)';
      kClass = 'Nanotubular Halloysite Kaolin Facies (Mawrth Vallis / Nili Fossae / Terby)';
    } else if (TC > 180.0 || aH2O < 0.30) {
      species = 'Meta-Halloysite-7A / Kaolinite Polytype';
      kClass = 'Dehydrated Platy Kaolinite Sequence';
    } else {
      species = 'Amorphous Aluminosilicate Precursor';
      kClass = 'Allophane-Halloysite Incipient Transition';
    }

    return {
      halloysiteConversionFraction: parseFloat(alphaHal.toFixed(3)),
      boundWaterYieldWeightPercent: parseFloat(wH2OPct.toFixed(2)),
      dominantKaolinSpecies: species,
      claystoneThermalInertiaTIU: parseFloat(TIU.toFixed(1)),
      kaolinFaciesClass: kClass,
      halloysiteContext: `Halloysite at ${TC.toFixed(0)} C, a(H2O)=${aH2O.toFixed(2)} (${(alphaHal * 100).toFixed(1)}% converted, ${wH2OPct.toFixed(2)}% bound H2O, TIU=${TIU.toFixed(0)}, ${kClass})`
    };
  }

  /**
   * Calculate high-temperature hydrothermal metasomatism and polytype ordering of felsic crust into well-crystallized dickite, microcrystalline densification, and thermal inertia.
   * Felsic Crust + Al3+ + SiO2(aq) + H2O (140-270 C) -> Dickite (Al2Si2O5(OH)4) (High-T Polytype)
   * Reference: Ehlmann et al. (2011), Michalski et al. (2013), Viviano-Beck et al. (2014) for Martian High-T Kaolin Deposits.
   * @param {number} [initialDacitePorosity=0.22] - Initial fractured dacite/felsite porosity (0.02 to 0.45)
   * @param {number} [hydrothermalTempC=210.0] - Hydrothermal alteration temperature in C (100 to 340 C)
   * @param {number} [aqueousSilicaActivity=0.45] - Dissolved silica activity (0.01 to 1.0)
   * @param {number} [durationYears=400.0] - Hydrothermal alteration duration in years (0.1 to 5000 yr)
   * @returns {{dickiteConversionFraction: number, boundHydroxylYieldWeightPercent: number, dominantPolytypeSpecies: string, induratedDickiteThermalInertiaTIU: number, polytypeFaciesClass: string, dickiteContext: string}}
   */
  static computeMartianDickiteMetasomatism(initialDacitePorosity = 0.22, hydrothermalTempC = 210.0, aqueousSilicaActivity = 0.45, durationYears = 400.0) {
    const phi0 = Math.max(0.01, Math.min(0.50, initialDacitePorosity));
    const TC = Math.max(80.0, Math.min(380.0, hydrothermalTempC));
    const aSiO2 = Math.max(0.01, Math.min(1.0, aqueousSilicaActivity));
    const tYrs = Math.max(0.01, durationYears);

    const TK = TC + 273.15;
    const tSec = tYrs * 365.25 * 86400.0;
    const Rgas = 8.314;
    const Ea = 5.20e4; // 52 kJ/mol for dickite crystallization

    // Reaction rate constant
    const kRate = 4.9e-3 * Math.exp(-Ea / (Rgas * TK)) * Math.pow(aSiO2, 0.35);
    const alphaDck = 1.0 - Math.exp(-Math.min(20.0, kRate * tSec));

    // Bound hydroxyl yield (wt%)
    const wH2OPct = alphaDck * 13.96;

    // Recrystallization and microcrystalline porosity reduction
    const phiResidual = phi0 * (1.0 - (0.70 * alphaDck));
    const rhoGrain = 2600.0;
    const rhoBulk = ((1.0 - phiResidual) * rhoGrain) + (phiResidual * 1000.0);

    const kTherm = 2.15; // W/(m K)
    const Cspec = 880.0; // J/(kg K)
    const TIU = Math.sqrt(kTherm * rhoBulk * Cspec);

    let species = 'Well-Crystallized Dickite Polytype';
    let dClass = 'High-Temperature Kaolin Polytype Facies';

    if (alphaDck >= 0.50 && TC >= 140.0 && TC <= 270.0 && aSiO2 >= 0.20) {
      species = 'Dickite Polytype (Al2Si2O5(OH)4)';
      dClass = 'High-Temperature Hydrothermal Dickite Facies (Nili Fossae / Toro / McLaughlin)';
    } else if (TC > 270.0) {
      species = 'Pyrophyllite-Quartz Metasomatic Assemblage';
      dClass = 'Advanced Argillic Metasomatic Facies';
    } else {
      species = 'Disordered Kaolinite / Halloysite Polytype';
      dClass = 'Low-Temperature Kaolin Alteration Crust';
    }

    return {
      dickiteConversionFraction: parseFloat(alphaDck.toFixed(3)),
      boundHydroxylYieldWeightPercent: parseFloat(wH2OPct.toFixed(2)),
      dominantPolytypeSpecies: species,
      induratedDickiteThermalInertiaTIU: parseFloat(TIU.toFixed(1)),
      polytypeFaciesClass: dClass,
      dickiteContext: `Dickite at ${TC.toFixed(0)} C, a(SiO2)=${aSiO2.toFixed(2)} (${(alphaDck * 100).toFixed(1)}% converted, ${wH2OPct.toFixed(2)}% OH, TIU=${TIU.toFixed(0)}, ${dClass})`
    };
  }
}

















