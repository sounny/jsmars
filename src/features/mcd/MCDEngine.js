import { MarsTime } from '../slider/MarsTime.js';

/**
 * @module MCDEngine
 * @description Mars Climate Database (MCD) vertical atmospheric profiler engine.
 * Computes atmospheric temperature, pressure, density, dust, and wind speed profiles
 * from surface up to 50 km altitude based on LMD/Mars GCM parameterizations.
 */
export class MCDEngine {
  static R_SPECIFIC_CO2 = 188.92; // J / (kg K) for pure CO2 atmosphere
  static G_MARS = 3.72076; // m / s^2
  static BASE_SURFACE_PRESSURE = 610.0; // Pa at MOLA zero elevation datum

  /**
   * Compute vertical atmospheric profile at a given location and time.
   * @param {object} params - Profile parameters
   * @param {number} [params.lat=0] - Latitude (-90 to +90)
   * @param {number} [params.lon=0] - East Longitude (0 to 360)
   * @param {number} [params.elevation=0] - Surface elevation in meters
   * @param {number} [params.Ls=0] - Solar longitude (0 to 360)
   * @param {number} [params.localHour=12] - Local solar hour (0 to 24)
   * @param {number} [params.tau=0.3] - Column dust optical depth
   * @param {number} [params.maxAltitudeKm=50] - Maximum altitude in km
   * @returns {object} Full vertical profile data array and surface conditions.
   */
  static computeProfile(params = {}) {
    const lat = params.lat ?? 0;
    const lon = params.lon ?? 0;
    const elevation = params.elevation ?? 0;
    const Ls = params.Ls ?? 0;
    const localHour = params.localHour ?? 12;
    const tau = params.tau ?? 0.3;
    const maxAltKm = Math.min(80, Math.max(20, params.maxAltitudeKm ?? 50));

    // Surface pressure adjusted for elevation (scale height ~ 11 km)
    const P_surf = this.BASE_SURFACE_PRESSURE * Math.exp(-elevation / 11100);

    // Diurnal & latitude solar heating response
    const { cosZ } = MarsTime.getSolarZenith(lat, Ls, localHour);
    const latRad = Math.abs(lat) * Math.PI / 180;

    // Surface temperature estimate (K)
    const T_surf = Math.max(145, 215 + 40 * Math.cos(latRad) * Math.cos((localHour - 14) * Math.PI / 12) * Math.max(0.2, cosZ));

    // Tropopause / mesopause parameters
    // In Mars atmosphere, adiabatic lapse rate is Gamma = g / cp ~ 3.72 / 800 ~ 4.65 K/km
    const lapseRate = 4.5; // K / km in lower troposphere (0-15 km)
    const tropopauseAlt = 25.0; // km
    const mesosphereBaseT = Math.max(130, T_surf - lapseRate * tropopauseAlt * 0.7);

    // Mean atmospheric scale height (m)
    const T_mean = (T_surf + mesosphereBaseT) / 2;
    const scaleHeightMeters = (this.R_SPECIFIC_CO2 * T_mean) / this.G_MARS;

    const layers = [];
    const numSteps = 50; // 1 km resolution
    const dz = maxAltKm / numSteps;

    for (let i = 0; i <= numSteps; i++) {
      const altKm = parseFloat((i * dz).toFixed(1));
      const altM = altKm * 1000;

      // Vertical temperature structure T(z)
      let T_z;
      if (altKm < tropopauseAlt) {
        // Troposphere with boundary layer diurnal dampening
        const diurnalDamping = Math.exp(-altKm / 4.0);
        const baselineT = T_surf - lapseRate * altKm;
        T_z = baselineT + (T_surf - 200) * diurnalDamping * 0.3;
      } else {
        // Mesosphere (inversion / radiative plateau with thermal tides)
        const tidalOscillation = 8.0 * Math.sin((altKm - tropopauseAlt) * 0.3 + (localHour * Math.PI / 12));
        T_z = mesosphereBaseT + tidalOscillation;
      }
      T_z = Math.max(120, Math.min(290, T_z));

      // Barometric pressure P(z) = P_surf * exp(-z / H)
      const P_z = P_surf * Math.exp(-altM / scaleHeightMeters);

      // Ideal gas density rho(z) = P / (R_spec * T) in kg/m^3
      const rho_z = P_z / (this.R_SPECIFIC_CO2 * T_z);

      // Dust vertical distribution (Conrath profile: q(z) = q0 * exp(nu * (1 - exp(z/H))))
      const conrathNu = 0.007;
      const dustProfile = Math.max(0, Math.exp(conrathNu * (1 - Math.exp(altM / scaleHeightMeters))));
      const dustDensity = tau * 0.05 * dustProfile;

      // Approximate horizontal wind velocity (m/s)
      // Surface drag increases with altitude, peaking near 35-45 km jet stream
      const jetAlt = 35;
      const windSpeed = 5 + 35 * Math.sin(latRad) * Math.exp(-Math.pow((altKm - jetAlt) / 15, 2)) + (altKm / maxAltKm) * 15;

      layers.push({
        altitudeKm: altKm,
        temperatureK: parseFloat(T_z.toFixed(1)),
        pressurePa: parseFloat(P_z.toFixed(2)),
        densityKgM3: parseFloat(rho_z.toExponential(3)),
        densityRaw: rho_z,
        dustDensity: parseFloat(dustDensity.toFixed(4)),
        windSpeedMs: parseFloat(windSpeed.toFixed(1))
      });
    }

    return {
      location: { lat, lon, elevation, Ls, localHour, tau },
      surface: {
        pressurePa: parseFloat(P_surf.toFixed(1)),
        temperatureK: parseFloat(T_surf.toFixed(1)),
        scaleHeightKm: parseFloat((scaleHeightMeters / 1000).toFixed(2)),
        surfaceDensity: parseFloat((P_surf / (this.R_SPECIFIC_CO2 * T_surf)).toExponential(3))
      },
      layers
    };
  }

  // --- Atmospheric Thermodynamics & Aerodynamics ---

  /**
   * Calculate the local speed of sound in the Martian CO2 atmosphere.
   * @param {number} temperatureK - Atmospheric temperature (K)
   * @param {number} [gamma=1.29] - Adiabatic index for CO2
   * @returns {number} Speed of sound in m/s
   */
  static computeSpeedOfSound(temperatureK, gamma = 1.29) {
    return Math.sqrt(gamma * MCDEngine.R_SPECIFIC_CO2 * Math.max(1, temperatureK));
  }

  /**
   * Calculate dynamic viscosity using Sutherland's law for CO2.
   * @param {number} temperatureK - Temperature in K
   * @returns {number} Dynamic viscosity in Pa*s (N*s/m^2)
   */
  static computeDynamicViscosity(temperatureK) {
    const mu0 = 1.37e-5;
    const T0 = 273.15;
    const S = 222.0; // Sutherland constant for CO2 (K)

    return mu0 * Math.pow(temperatureK / T0, 1.5) * ((T0 + S) / (temperatureK + S));
  }

  /**
   * Calculate mean free path of CO2 molecules.
   * @param {number} pressurePa - Pressure in Pa
   * @param {number} temperatureK - Temperature in K
   * @returns {number} Mean free path in meters
   */
  static computeMeanFreePath(pressurePa, temperatureK) {
    const kB = 1.380649e-23; // Boltzmann constant (J/K)
    const d = 3.3e-10; // Effective molecular collision diameter for CO2 (m)
    const safeP = Math.max(0.001, pressurePa);

    return (kB * temperatureK) / (Math.SQRT2 * Math.PI * d * d * safeP);
  }

  /**
   * Calculate total atmospheric column mass per unit area.
   * @param {number} surfacePressurePa - Surface pressure (Pa)
   * @returns {number} Column mass in kg/m^2
   */
  static computeAtmosphericColumnMass(surfacePressurePa) {
    return surfacePressurePa / MCDEngine.G_MARS;
  }

  // --- Atmospheric Scale Height & Radiative Dust Extinction Solvers ---

  /**
   * Calculate atmospheric scale height H = (R_spec * T) / g.
   * @param {number} temperatureK - Atmospheric temperature (K)
   * @returns {number} Scale height in km
   */
  static computeAtmosphericScaleHeight(temperatureK) {
    const hMeters = (MCDEngine.R_SPECIFIC_CO2 * Math.max(10, temperatureK)) / MCDEngine.G_MARS;
    return parseFloat((hMeters / 1000).toFixed(3));
  }

  /**
   * Compute vertical dust optical depth extinction and direct solar beam transmission.
   * @param {number} altitudeKm - Altitude above surface in km
   * @param {number} [tauSurface=0.3] - Column dust optical depth
   * @param {number} [dustScaleHeightKm=10.0] - Dust vertical scale height in km
   * @param {number} [solarZenithDeg=0] - Solar zenith angle in degrees
   * @returns {{tauAbove: number, beamTransmission: number}}
   */
  static computeOpticalDepthExtinction(altitudeKm, tauSurface = 0.3, dustScaleHeightKm = 10.0, solarZenithDeg = 0) {
    const z = Math.max(0, altitudeKm);
    const tauAbove = tauSurface * Math.exp(-z / Math.max(1, dustScaleHeightKm));
    const cosZ = Math.max(0.05, Math.cos(solarZenithDeg * Math.PI / 180));
    const transmission = Math.exp(-tauAbove / cosZ);

    return {
      tauAbove: parseFloat(tauAbove.toFixed(4)),
      beamTransmission: parseFloat(transmission.toFixed(4))
    };
  }

  /**
   * Classify Martian atmospheric dust opacity regime.
   * @param {number} tau - Column dust optical depth (visible/IR equivalent)
   * @returns {{scenario: string, description: string}}
   */
  static classifyDustScenario(tau) {
    if (tau <= 0.2) {
      return {
        scenario: 'Clear / Low Dust',
        description: 'Aphelion season low-opacity atmosphere (Ls = 0° - 140°)'
      };
    } else if (tau <= 0.6) {
      return {
        scenario: 'Climatic Mean Background Dust',
        description: 'Standard baseline background dust opacity (MY24 Viking/MGS baseline)'
      };
    } else if (tau <= 1.5) {
      return {
        scenario: 'Regional Dust Storm',
        description: 'Perihelion elevated dust activity and localized regional storm'
      };
    } else {
      return {
        scenario: 'Global Dust Storm (GDS)',
        description: 'Planet-encircling global dust event with severe atmospheric heating (e.g. MY25 / MY34)'
      };
    }
  }

  // --- Atmospheric Dynamics, Coriolis & Optical Air Mass Solvers ---

  /**
   * Calculate planetary Coriolis parameter f = 2 * Omega * sin(lat).
   * @param {number} latitudeDeg - Latitude in degrees (-90 to +90)
   * @param {number} [rotationRateRadS=7.0882e-5] - Mars sidereal rotation rate (rad/s)
   * @returns {number} Coriolis parameter in s^-1
   */
  static computeCoriolisParameter(latitudeDeg, rotationRateRadS = 7.0882e-5) {
    const phiRad = latitudeDeg * Math.PI / 180.0;
    const f = 2.0 * rotationRateRadS * Math.sin(phiRad);
    return parseFloat(f.toExponential(4));
  }

  /**
   * Compute vertical thermal wind shear (du/dz) from meridional temperature gradient.
   * du/dz = -(g / (f * T)) * (dT/dy)
   * @param {number} meridionalTempGradKPerKm - North-South temperature gradient in K / 1000 km
   * @param {number} [meanTemperatureK=210] - Mean atmospheric layer temperature (K)
   * @param {number} [latitudeDeg=45] - Latitude in degrees
   * @returns {{windShearMsPerKm: number, coriolisF: number}}
   */
  static computeThermalWindShear(meridionalTempGradKPerKm, meanTemperatureK = 210, latitudeDeg = 45) {
    const f = Math.abs(2.0 * 7.0882e-5 * Math.sin(latitudeDeg * Math.PI / 180.0));
    if (f < 1e-6) return { windShearMsPerKm: 0, coriolisF: 0 };

    // Convert dT/dy to K / m
    const dTDy = (meridionalTempGradKPerKm / 1000.0) / 1000.0;
    const shearPerMeter = (MCDEngine.G_MARS / (f * Math.max(10, meanTemperatureK))) * dTDy;
    const shearPerKm = shearPerMeter * 1000.0;

    return {
      windShearMsPerKm: parseFloat(shearPerKm.toFixed(3)),
      coriolisF: parseFloat(f.toExponential(4))
    };
  }

  /**
   * Compute spherical planetary optical air mass (Kasten & Young 1989 formulation).
   * M(theta) = 1 / (cos(theta) + 0.50572 * (96.07995 - theta)^-1.6364)
   * @param {number} solarZenithDeg - Solar zenith angle in degrees (0 to 90+)
   * @returns {number} Relative optical air mass
   */
  static computeAirMass(solarZenithDeg) {
    const theta = Math.min(89.9, Math.max(0, solarZenithDeg));
    const cosTheta = Math.cos(theta * Math.PI / 180.0);
    const denom = cosTheta + 0.50572 * Math.pow(96.07995 - theta, -1.6364);
    return parseFloat((1.0 / denom).toFixed(3));
  }

  // --- Planetary Boundary Layer (PBL), Ångström Dust & Static Stability ---

  /**
   * Calculate daytime convective Planetary Boundary Layer (PBL) height on Mars.
   * On Mars, the deep convective PBL grows up to 6 - 10 km during intense midday heating.
   * @param {number} sensibleHeatFluxW_M2 - Surface sensible heat flux (e.g. 20 W/m^2)
   * @param {number} [surfaceTempK=220] - Surface temperature in Kelvin
   * @param {number} [surfaceDensityKgM3=0.015] - Atmospheric surface density in kg/m^3
   * @returns {{pblHeightMeters: number, pblHeightKm: number, convectiveVelocityMs: number}}
   */
  static computePBLHeight(sensibleHeatFluxW_M2, surfaceTempK = 220, surfaceDensityKgM3 = 0.015) {
    const cp = 800.0; // J/(kg K)
    const g = MCDEngine.G_MARS;
    const flux = Math.max(0.1, sensibleHeatFluxW_M2);
    const rho = Math.max(1e-4, surfaceDensityKgM3);
    const T0 = Math.max(100, surfaceTempK);

    // Buoyancy flux B0 = (g / T0) * (H_sens / (rho * cp))
    const B0 = (g / T0) * (flux / (rho * cp));

    // Convective PBL height scaling: z_i ~ sqrt(2 * B0 * t_heating / gamma_theta)
    // Over a 6-hour midday heating sol duration (21600 s), with lapse deficit gamma ~ 0.003 K/m
    const tHeating = 21600.0;
    const gammaTheta = 0.003;
    const ziMeters = Math.sqrt((2.0 * B0 * tHeating) / gammaTheta);
    const clampedZi = Math.max(500, Math.min(12000, ziMeters));

    // Deardorff convective velocity scale w* = (B0 * z_i)^(1/3)
    const wStar = Math.pow(B0 * clampedZi, 1.0 / 3.0);

    return {
      pblHeightMeters: parseFloat(clampedZi.toFixed(1)),
      pblHeightKm: parseFloat((clampedZi / 1000.0).toFixed(2)),
      convectiveVelocityMs: parseFloat(wStar.toFixed(2))
    };
  }

  /**
   * Calculate wavelength-dependent dust optical depth using Ångström power-law exponent.
   * tau(lambda) = tau_ref * (lambda_ref / lambda)^alpha
   * @param {number} tauVisible - Reference visible optical depth (at 0.67 µm)
   * @param {number} targetWavelengthMicrons - Target observation wavelength in µm (e.g. 9.3 µm for thermal IR or 15 µm)
   * @param {number} [angstromAlpha=0.5] - Ångström exponent for Martian mineral dust (~0.5 - 0.9)
   * @returns {number} Extinction optical depth at target wavelength
   */
  static computeWavelengthDependentDustTau(tauVisible, targetWavelengthMicrons, angstromAlpha = 0.5) {
    const lambdaRef = 0.67; // µm (visible)
    const lambda = Math.max(0.1, targetWavelengthMicrons);
    const tau = tauVisible * Math.pow(lambdaRef / lambda, angstromAlpha);
    return parseFloat(tau.toFixed(4));
  }

  /**
   * Compute atmospheric Brunt-Väisälä buoyancy frequency (N) and static stability.
   * N^2 = (g / theta) * (d_theta / dz)
   * @param {number} potentialTempK - Layer potential temperature in Kelvin
   * @param {number} dThetaDzKPerM - Vertical potential temperature gradient (K/m)
   * @returns {{frequencyRadS: number, periodSeconds: number, isStable: boolean}}
   */
  static computeBruntVaisalaFrequency(potentialTempK, dThetaDzKPerM) {
    const theta = Math.max(50, potentialTempK);
    const N2 = (MCDEngine.G_MARS / theta) * dThetaDzKPerM;

    if (N2 <= 0) {
      return { frequencyRadS: 0, periodSeconds: Infinity, isStable: false };
    }

    const N = Math.sqrt(N2);
    const period = (2.0 * Math.PI) / N;

    return {
      frequencyRadS: parseFloat(N.toFixed(5)),
      periodSeconds: parseFloat(period.toFixed(1)),
      isStable: true
    };
  }

  // --- Surface Friction Velocity, Dust Specific Extinction & Potential Temperature Solvers ---

  /**
   * Calculate atmospheric surface boundary layer friction velocity (u*) and surface shear stress.
   * u* = (kappa * u) / ln(z / z0),  tau_w = rho * u*^2
   * @param {number} windSpeedMs - Wind speed at height z in m/s (e.g. 10 m/s at 10m height)
   * @param {number} [measurementHeightM=10.0] - Measurement altitude in meters
   * @param {number} [roughnessLengthM=0.01] - Aerodynamic surface roughness length z0 (1 cm typical for Mars rocks)
   * @param {number} [surfaceDensityKgM3=0.015] - Near-surface atmospheric density
   * @returns {{frictionVelocityMs: number, shearStressPa: number, thresholdExceeded: boolean}}
   */
  static computeSurfaceFrictionVelocity(windSpeedMs, measurementHeightM = 10.0, roughnessLengthM = 0.01, surfaceDensityKgM3 = 0.015) {
    const kappa = 0.40; // Von Kármán constant
    const z = Math.max(roughnessLengthM * 2.0, measurementHeightM);
    const z0 = Math.max(1e-5, roughnessLengthM);

    const logRatio = Math.log(z / z0);
    const uStar = (kappa * Math.max(0, windSpeedMs)) / Math.max(0.1, logRatio);
    const shearStress = surfaceDensityKgM3 * uStar * uStar;

    // Saltation / dust lifting threshold on Mars is approximately u* >= 1.5 m/s
    const thresholdExceeded = uStar >= 1.5;

    return {
      frictionVelocityMs: parseFloat(uStar.toFixed(3)),
      shearStressPa: parseFloat(shearStress.toExponential(3)),
      thresholdExceeded
    };
  }

  /**
   * Calculate specific dust mass extinction cross-section.
   * sigma_ext = (3 * Q_ext) / (4 * rho * r_eff)
   * @param {number} [extinctionEfficiencyQ=2.5] - Extinction efficiency factor Q_ext (~2.5 in visible)
   * @param {number} [particleDensityKgM3=2500.0] - Mineral grain density in kg/m^3
   * @param {number} [effectiveRadiusMicrons=1.5] - Mean cross-sectional particle radius in µm
   * @returns {{massExtinctionM2PerKg: number, massExtinctionM2PerGram: number}}
   */
  static computeDustSpecificExtinctionCrossSection(extinctionEfficiencyQ = 2.5, particleDensityKgM3 = 2500.0, effectiveRadiusMicrons = 1.5) {
    const rM = effectiveRadiusMicrons * 1e-6;
    const sigmaKg = (3.0 * extinctionEfficiencyQ) / (4.0 * particleDensityKgM3 * rM);
    const sigmaG = sigmaKg * 1e-3;

    return {
      massExtinctionM2PerKg: parseFloat(sigmaKg.toFixed(1)),
      massExtinctionM2PerGram: parseFloat(sigmaG.toFixed(4))
    };
  }

  /**
   * Compute dry potential temperature theta = T * (P0 / P)^(R / cp).
   * @param {number} tempK - Atmospheric temperature in Kelvin
   * @param {number} pressurePa - Layer atmospheric pressure in Pa
   * @param {number} [referencePressurePa=610.0] - Reference datum surface pressure (610 Pa)
   * @returns {number} Potential temperature in Kelvin
   */
  static computePotentialTemperature(tempK, pressurePa, referencePressurePa = 610.0) {
    const cp = 800.0; // J/(kg K)
    const exponent = this.R_SPECIFIC_CO2 / cp; // ~ 0.23615
    const pSafe = Math.max(0.01, pressurePa);

    const theta = Math.max(1, tempK) * Math.pow(referencePressurePa / pSafe, exponent);
    return parseFloat(theta.toFixed(2));
  }

  // --- Atmospheric Thermal Diffusivity, CO2 Condensation & Dust Settling Velocity Solvers ---

  /**
   * Calculate atmospheric gas thermal diffusivity alpha_atm = k_gas / (rho * cp).
   * @param {number} tempK - Atmospheric temperature in Kelvin
   * @param {number} pressurePa - Layer atmospheric pressure in Pa
   * @returns {number} Thermal diffusivity in m^2 / s
   */
  static computeAtmosphericThermalDiffusivity(tempK, pressurePa) {
    const cp = 800.0; // J/(kg K)
    const rho = Math.max(1e-6, pressurePa / (this.R_SPECIFIC_CO2 * Math.max(10, tempK)));
    // CO2 gas thermal conductivity ~ 0.015 W/(m K) at 210K
    const kGas = 0.015 * Math.pow(tempK / 273.15, 0.8);
    const alpha = kGas / (rho * cp);

    return parseFloat(alpha.toExponential(4));
  }

  /**
   * Calculate atmospheric CO2 vapor supersaturation ratio and frost status.
   * S = P / P_sat(T)
   * @param {number} tempK - Air temperature in Kelvin
   * @param {number} pressurePa - Ambient atmospheric pressure in Pa
   * @returns {{supersaturationRatio: number, isCondensing: boolean, satPressurePa: number}}
   */
  static computeCO2CondensationFlux(tempK, pressurePa) {
    const T = Math.max(50, tempK);
    // Clausius-Clapeyron: P_sat = 1.055e12 * exp(-3148 / T)
    const pSat = 1.055e12 * Math.exp(-3148.0 / T);
    const S = pressurePa / Math.max(1e-6, pSat);

    return {
      supersaturationRatio: parseFloat(S.toFixed(3)),
      isCondensing: S >= 1.0,
      satPressurePa: parseFloat(pSat.toFixed(2))
    };
  }

  /**
   * Calculate Stokes-Cunningham terminal sedimentation settling velocity for dust grains in Mars atmosphere.
   * v_term = (2 * rho_p * r^2 * g / (9 * mu)) * (1 + 1.257 * Kn)
   * @param {number} [effectiveRadiusMicrons=1.5] - Particle radius in µm
   * @param {number} [tempK=210.0] - Temperature in Kelvin
   * @param {number} [pressurePa=610.0] - Pressure in Pa
   * @param {number} [particleDensityKgM3=2500.0] - Mineral grain density in kg/m^3
   * @returns {{settlingVelocityMmS: number, knudsenNumber: number}}
   */
  static computeDustDepositionVelocity(effectiveRadiusMicrons = 1.5, tempK = 210.0, pressurePa = 610.0, particleDensityKgM3 = 2500.0) {
    const rM = effectiveRadiusMicrons * 1e-6;
    const mu = this.computeDynamicViscosity(tempK);
    const lambda = this.computeMeanFreePath(pressurePa, tempK);
    const Kn = lambda / Math.max(1e-9, rM); // Knudsen number

    // Cunningham slip correction factor
    const cunninghamFactor = 1.0 + 1.257 * Kn;

    // Stokes settling velocity
    const vStokes = (2.0 * particleDensityKgM3 * rM * rM * this.G_MARS) / (9.0 * mu);
    const vTerm = vStokes * cunninghamFactor; // m/s
    const vMmS = vTerm * 1000.0; // mm/s

    return {
      settlingVelocityMmS: parseFloat(vMmS.toFixed(3)),
      knudsenNumber: parseFloat(Kn.toFixed(2))
    };
  }
}






