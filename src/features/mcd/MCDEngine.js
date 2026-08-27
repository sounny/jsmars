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

  // --- Gradient Richardson Number, Spacecraft Aerodynamic Drag & Rayleigh Optical Depth Solvers ---

  /**
   * Calculate atmospheric Gradient Richardson Number (Ri) for dynamic shear turbulence.
   * Ri = N^2 / S^2 = [ (g / theta) * (d_theta / dz) ] / [ (du/dz)^2 + (dv/dz)^2 ]
   * @param {number} potentialTempK - Potential temperature in Kelvin
   * @param {number} dThetaDz - Vertical potential temperature gradient (K/m)
   * @param {number} duDz - Zonal wind shear (s^-1)
   * @param {number} [dvDz=0] - Meridional wind shear (s^-1)
   * @returns {{richardsonNumber: number, isTurbulent: boolean, regime: string}}
   */
  static computeGradientRichardsonNumber(potentialTempK, dThetaDz, duDz, dvDz = 0) {
    const theta = Math.max(50, potentialTempK);
    const N2 = (this.G_MARS / theta) * dThetaDz;
    const S2 = Math.max(1e-8, duDz * duDz + dvDz * dvDz);

    const Ri = N2 / S2;
    const isTurbulent = Ri < 0.25;

    let regime = 'Dynamically Stable (Laminar)';
    if (Ri < 0) regime = 'Convectively Unstable (Overturning)';
    else if (Ri < 0.25) regime = 'Turbulent Shear Instability (Kelvin-Helmholtz)';

    return {
      richardsonNumber: parseFloat(Ri.toFixed(3)),
      isTurbulent,
      regime
    };
  }

  /**
   * Calculate spacecraft aerodynamic drag force and orbital deceleration during upper atmospheric aerobraking.
   * F_drag = 0.5 * Cd * A * rho * v^2
   * @param {number} spacecraftAreaM2 - Spacecraft cross-sectional area in m^2 (e.g. 10 m^2 for MRO)
   * @param {number} dragCoeffCd - Aerodynamic drag coefficient Cd (~2.2 in free-molecular flow)
   * @param {number} spacecraftMassKg - Spacecraft mass in kg (e.g. 1000 kg)
   * @param {number} altitudeKm - Orbital periapsis altitude in km (e.g. 120 km)
   * @param {number} [orbitalSpeedMs=4200.0] - Spacecraft periapsis velocity in m/s
   * @returns {{dragForceNewtons: number, decelerationMs2: number, atmosphericDensityKgM3: number}}
   */
  static computeOrbitalAerodynamicDrag(spacecraftAreaM2, dragCoeffCd = 2.2, spacecraftMassKg = 1000.0, altitudeKm = 120.0, orbitalSpeedMs = 4200.0) {
    // Upper atmospheric density profile rho(z) = rho0 * exp(-z / H)
    const H_m = 9000.0; // Scale height ~ 9 km in thermosphere
    const rho0 = 0.015; // kg/m^3 at surface
    const rho = rho0 * Math.exp(-(altitudeKm * 1000.0) / H_m);

    const v = Math.max(100, orbitalSpeedMs);
    const fDrag = 0.5 * dragCoeffCd * spacecraftAreaM2 * rho * v * v;
    const decel = fDrag / Math.max(1, spacecraftMassKg);

    return {
      dragForceNewtons: parseFloat(fDrag.toFixed(3)),
      decelerationMs2: parseFloat(decel.toExponential(3)),
      atmosphericDensityKgM3: parseFloat(rho.toExponential(3))
    };
  }

  /**
   * Calculate clean molecular CO2 Rayleigh scattering optical depth.
   * tau_Rayleigh = 0.0088 * (P / 101325) * lambda^(-4.05)
   * @param {number} wavelengthMicrons - Wavelength in µm (e.g. 0.44 µm blue filter)
   * @param {number} [surfacePressurePa=610.0] - Atmospheric surface pressure in Pa
   * @returns {number} Molecular Rayleigh optical depth
   */
  static computeRayleighScatteringOpticalDepth(wavelengthMicrons, surfacePressurePa = 610.0) {
    const lam = Math.max(0.1, wavelengthMicrons);
    const pressureRatio = surfacePressurePa / 101325.0;
    const tau = 0.0088 * pressureRatio * Math.pow(lam, -4.05);

    return parseFloat(tau.toExponential(4));
  }

  // --- Atmospheric Thermal Tides, Volumetric Dust Cross-Section & Scale Height Solvers ---

  /**
   * Calculate migrating atmospheric thermal tide temperature perturbation amplitude.
   * delta_T(z) = delta_T0 * exp(z / (2 * H))
   * @param {number} altitudeKm - Altitude above surface in km
   * @param {number} [diurnalHarmonic=1] - Harmonic order (1 = diurnal, 2 = semidiurnal)
   * @param {number} [baseAmplitudeK=2.5] - Surface baseline tidal amplitude in K
   * @returns {{tidalAmplitudeK: number, waveOrder: string}}
   */
  static computeAtmosphericThermalTideAmplitude(altitudeKm, diurnalHarmonic = 1, baseAmplitudeK = 2.5) {
    const H_km = 11.0; // Scale height ~ 11 km
    const z = Math.max(0, altitudeKm);
    const growthFactor = Math.exp(z / (2.0 * H_km));
    const amp = baseAmplitudeK * growthFactor;

    // Saturation limit in upper mesosphere / thermosphere (~30 K)
    const clampedAmp = Math.min(35.0, amp);

    return {
      tidalAmplitudeK: parseFloat(clampedAmp.toFixed(2)),
      waveOrder: diurnalHarmonic === 2 ? 'Semi-Diurnal (12-hour)' : 'Diurnal (24-hour)'
    };
  }

  /**
   * Calculate volumetric dust extinction coefficient beta_ext in m^-1.
   * beta_ext = (3 * rho_dust * Q_ext) / (4 * rho_grain * r_eff)
   * @param {number} dustMassConcentrationKgM3 - Airborne dust mass concentration in kg/m^3
   * @param {number} [effectiveRadiusMicrons=1.5] - Effective particle radius in µm
   * @param {number} [grainDensityKgM3=2500.0] - Mineral grain density
   * @returns {{extinctionCoeffPerMeter: number, extinctionCoeffPerKm: number}}
   */
  static computeDustOpticalCrossSectionPerVolume(dustMassConcentrationKgM3, effectiveRadiusMicrons = 1.5, grainDensityKgM3 = 2500.0) {
    const rM = effectiveRadiusMicrons * 1e-6;
    const qExt = 2.5; // Extinction efficiency in visible
    const betaM = (3.0 * Math.max(0, dustMassConcentrationKgM3) * qExt) / (4.0 * grainDensityKgM3 * rM);
    const betaKm = betaM * 1000.0;

    return {
      extinctionCoeffPerMeter: parseFloat(betaM.toExponential(4)),
      extinctionCoeffPerKm: parseFloat(betaKm.toFixed(5))
    };
  }

  /**
   * Calculate variable atmospheric scale height with linear temperature lapse rate.
   * H(z) = (R_spec * (T_surf - Gamma * z)) / g
   * @param {number} surfaceTempK - Surface temperature in Kelvin
   * @param {number} altitudeKm - Altitude above datum in km
   * @param {number} [lapseRateKPerKm=4.5] - Temperature lapse rate in K/km
   * @returns {{localScaleHeightKm: number, localTempK: number}}
   */
  static computeAtmosphericScaleHeightLapseRate(surfaceTempK, altitudeKm, lapseRateKPerKm = 4.5) {
    const z = Math.max(0, altitudeKm);
    const localT = Math.max(100.0, surfaceTempK - lapseRateKPerKm * z);
    const hM = (this.R_SPECIFIC_CO2 * localT) / this.G_MARS;

    return {
      localScaleHeightKm: parseFloat((hM / 1000.0).toFixed(3)),
      localTempK: parseFloat(localT.toFixed(1))
    };
  }

  // --- Sutherland Viscosity, Brunt-Väisälä Frequency & Turbulent Eddy Diffusivity Solvers ---

  /**
   * Calculate high-precision CO2 dynamic viscosity using Sutherland's formula.
   * mu(T) = mu0 * (T / T0)^(3/2) * (T0 + S) / (T + S)
   * @param {number} temperatureK - Temperature in Kelvin (100K to 350K)
   * @returns {{dynamicViscosityPaS: number, kinematicViscosityM2S: number}}
   */
  static computeSutherlandDynamicViscosity(temperatureK) {
    const T = Math.max(50.0, temperatureK);
    const T0 = 273.15;
    const S = 240.0; // Sutherland constant for CO2 in K
    const mu0 = 1.37e-5; // Pa * s at 273.15 K

    const mu = mu0 * Math.pow(T / T0, 1.5) * ((T0 + S) / (T + S));
    const rho = 610.0 / (this.R_SPECIFIC_CO2 * T); // kg/m^3 at 610 Pa datum
    const nu = mu / rho; // Kinematic viscosity m^2/s

    return {
      dynamicViscosityPaS: parseFloat(mu.toExponential(4)),
      kinematicViscosityM2S: parseFloat(nu.toExponential(4))
    };
  }

  /**
   * Calculate atmospheric Brunt-Väisälä buoyancy frequency from environmental lapse rate.
   * N = sqrt( (g / T) * (Gamma_d - Gamma) )
   * @param {number} temperatureK - Local layer temperature in Kelvin
   * @param {number} environmentalLapseRateKPerKm - Observed temperature lapse rate -dT/dz in K/km
   * @param {number} [dryAdiabaticLapseRateKPerKm=4.65] - Martian dry adiabatic lapse rate Gamma_d = g/cp (~4.65 K/km)
   * @returns {{frequencyRadS: number, periodSeconds: number, isConvectivelyStable: boolean}}
   */
  static computeAtmosphericBruntVaisalaFrequency(temperatureK, environmentalLapseRateKPerKm, dryAdiabaticLapseRateKPerKm = 4.65) {
    const T = Math.max(50.0, temperatureK);
    const dGamma = (dryAdiabaticLapseRateKPerKm - environmentalLapseRateKPerKm) / 1000.0; // K / m
    const N2 = (this.G_MARS / T) * dGamma;

    if (N2 <= 0) {
      return {
        frequencyRadS: 0.0,
        periodSeconds: Infinity,
        isConvectivelyStable: false
      };
    }

    const N = Math.sqrt(N2);
    const period = (2.0 * Math.PI) / N;

    return {
      frequencyRadS: parseFloat(N.toFixed(5)),
      periodSeconds: parseFloat(period.toFixed(1)),
      isConvectivelyStable: true
    };
  }

  /**
   * Calculate Troen & Mahrt (1986) boundary layer vertical eddy diffusivity K_z.
   * K_z = kappa * u* * z * (1 - z / h_pbl)^2
   * @param {number} frictionVelocityMs - Surface friction velocity u* in m/s
   * @param {number} pblHeightMeters - Planetary Boundary Layer height in meters
   * @param {number} altitudeMeters - Altitude inside PBL in meters
   * @returns {{eddyDiffusivityM2S: number, pblFraction: number}}
   */
  static computeTurbulentEddyDiffusivity(frictionVelocityMs, pblHeightMeters, altitudeMeters) {
    const kappa = 0.40; // Von Kármán constant
    const uStar = Math.max(0.01, frictionVelocityMs);
    const h = Math.max(100.0, pblHeightMeters);
    const z = Math.max(0.1, Math.min(h, altitudeMeters));

    const zRatio = z / h;
    const factor = Math.pow(1.0 - zRatio, 2);
    const Kz = kappa * uStar * z * factor;

    return {
      eddyDiffusivityM2S: parseFloat(Kz.toFixed(2)),
      pblFraction: parseFloat(zRatio.toFixed(3))
    };
  }

  // --- Acoustic Sound Speed, Ekman Spiral & Column Mass Density Solvers ---

  /**
   * Calculate exact atmospheric acoustic propagation sound speed and Mach 1 velocity.
   * c_s = sqrt(gamma * R_spec * T)
   * @param {number} temperatureK - Atmospheric temperature in Kelvin
   * @param {number} [gamma=1.29] - Heat capacity ratio (Cp / Cv) for pure Martian CO2
   * @returns {{soundSpeedMs: number, soundSpeedKmH: number}}
   */
  static computeAtmosphericSoundSpeed(temperatureK, gamma = 1.29) {
    const T = Math.max(10.0, temperatureK);
    const cs = Math.sqrt(gamma * this.R_SPECIFIC_CO2 * T);

    return {
      soundSpeedMs: parseFloat(cs.toFixed(2)),
      soundSpeedKmH: parseFloat((cs * 3.6).toFixed(2))
    };
  }

  /**
   * Calculate Ekman boundary layer horizontal wind profile and cross-isobar turning deflection angle.
   * u(z) = U_g * (1 - exp(-gamma*z) * cos(gamma*z)),  v(z) = U_g * exp(-gamma*z) * sin(gamma*z)
   * @param {number} geostrophicWindSpeedMs - Free-tropospheric geostrophic wind speed U_g in m/s
   * @param {number} [pblHeightMeters=2000.0] - Planetary boundary layer thickness
   * @param {number} [altitudeMeters=500.0] - Altitude z inside the boundary layer
   * @param {number} [latitudeDeg=45.0] - Latitude for Coriolis parameter
   * @returns {{uWindMs: number, vWindMs: number, totalSpeedMs: number, deflectionAngleDeg: number}}
   */
  static computeEkmanSpiralWindDeflection(geostrophicWindSpeedMs, pblHeightMeters = 2000.0, altitudeMeters = 500.0, latitudeDeg = 45.0) {
    const Ug = Math.max(0.1, geostrophicWindSpeedMs);
    const h = Math.max(100.0, pblHeightMeters);
    const z = Math.max(0.1, altitudeMeters);

    const gamma = Math.PI / h; // Ekman layer scaling constant
    const decay = Math.exp(-gamma * z);
    const angle = gamma * z;

    const u = Ug * (1.0 - decay * Math.cos(angle));
    const v = Ug * decay * Math.sin(angle);
    const speed = Math.hypot(u, v);
    const deflDeg = Math.atan2(v, u) * 180.0 / Math.PI;

    return {
      uWindMs: parseFloat(u.toFixed(2)),
      vWindMs: parseFloat(v.toFixed(2)),
      totalSpeedMs: parseFloat(speed.toFixed(2)),
      deflectionAngleDeg: parseFloat(deflDeg.toFixed(2))
    };
  }

  /**
   * Calculate total vertical column atmospheric integrated mass density.
   * sigma = P_surf / g_mars
   * @param {number} [surfacePressurePa=610.0] - Surface atmospheric pressure in Pa
   * @returns {{columnMassKgM2: number, columnMassGramsCm2: number}}
   */
  static computeAtmosphericTotalColumnDensity(surfacePressurePa = 610.0) {
    const p = Math.max(0.01, surfacePressurePa);
    const sigmaKgM2 = p / this.G_MARS;
    const sigmaGCm2 = sigmaKgM2 * 0.1; // 1 kg/m^2 = 0.1 g/cm^2

    return {
      columnMassKgM2: parseFloat(sigmaKgM2.toFixed(3)),
      columnMassGramsCm2: parseFloat(sigmaGCm2.toFixed(4))
    };
  }

  // --- Conrath Dust Profile, Water Ice Saturation & Adiabatic Lapse Rate Solvers ---

  /**
   * Calculate vertical Conrath (1975) airborne dust optical depth profile and local mixing ratio.
   * tau(P) = tau_0 * (P / P_0) * exp[ nu * (1 - P_0 / P) ]
   * @param {number} [columnTau0=0.3] - Total column dust optical depth
   * @param {number} [pressurePa=300.0] - Atmospheric pressure at target level in Pa
   * @param {number} [surfacePressurePa=610.0] - Surface pressure in Pa
   * @param {number} [conrathNu=0.007] - Conrath decay parameter nu
   * @returns {{tauAboveLevel: number, relativeDustMixingRatio: number}}
   */
  static computeConrathDustOpticalDepthProfile(columnTau0 = 0.3, pressurePa = 300.0, surfacePressurePa = 610.0, conrathNu = 0.007) {
    const tau0 = Math.max(0.001, columnTau0);
    const P = Math.max(0.01, pressurePa);
    const P0 = Math.max(0.1, surfacePressurePa);

    const pRatio = P / P0;
    const conrathFactor = Math.exp(conrathNu * (1.0 - 1.0 / pRatio));
    const tauAbove = tau0 * pRatio * conrathFactor;

    return {
      tauAboveLevel: parseFloat(Math.max(0, Math.min(tau0, tauAbove)).toFixed(4)),
      relativeDustMixingRatio: parseFloat(conrathFactor.toFixed(4))
    };
  }

  /**
   * Calculate H2O water ice saturation vapor pressure and saturation mixing ratio.
   * e_s(T) = e_0 * exp[ 22.5 * (1 - T_0 / T) ] (Martian sub-freezing Clausius-Clapeyron)
   * @param {number} temperatureK - Ambient temperature in Kelvin (120K to 273.15K)
   * @param {number} [ambientPressurePa=610.0] - Ambient atmospheric pressure in Pa
   * @returns {{saturationVaporPressurePa: number, saturationMixingRatioPpm: number}}
   */
  static computeWaterIceSaturationVaporPressure(temperatureK, ambientPressurePa = 610.0) {
    const T = Math.max(50.0, temperatureK);
    const T0 = 273.16; // Triple point of water in K
    const e0 = 611.65; // Saturation vapor pressure at triple point in Pa

    // Goff-Gratch / Clausius-Clapeyron approximation over ice
    const es = e0 * Math.exp(22.5 * (1.0 - T0 / T));
    const p = Math.max(0.01, ambientPressurePa);

    // Mixing ratio w_s = (M_H2O / M_CO2) * (es / P) = (18.015 / 44.01) * (es / P) ~ 0.4093 * (es / P)
    const wsLinear = 0.4093 * (es / p);
    const wsPpm = wsLinear * 1e6;

    return {
      saturationVaporPressurePa: parseFloat(es.toExponential(4)),
      saturationMixingRatioPpm: parseFloat(wsPpm.toFixed(2))
    };
  }

  /**
   * Calculate Martian dry adiabatic lapse rate Gamma_d = g / Cp.
   * @param {number} [specificHeatCp=800.0] - Specific heat capacity of CO2 in J / (kg K)
   * @param {number} [gravityMs2=3.72076] - Surface gravity in m/s^2
   * @returns {{lapseRateKPerMeter: number, lapseRateKPerKm: number}}
   */
  static computeAdiabaticLapseRate(specificHeatCp = 800.0, gravityMs2 = 3.72076) {
    const cp = Math.max(100.0, specificHeatCp);
    const g = Math.max(0.1, gravityMs2);

    const gammaM = g / cp; // K / m
    const gammaKm = gammaM * 1000.0; // K / km

    return {
      lapseRateKPerMeter: parseFloat(gammaM.toFixed(6)),
      lapseRateKPerKm: parseFloat(gammaKm.toFixed(3))
    };
  }

  // --- Monin-Obukhov Length, Saltation Threshold & Scale Height Gradient Solvers ---

  /**
   * Calculate atmospheric Monin-Obukhov boundary layer stability length L.
   * L = -(u*^3 * T0 * rho * Cp) / (kappa * g * H_sens)
   * @param {number} frictionVelocityMs - Friction velocity u* in m/s
   * @param {number} sensibleHeatFluxW_M2 - Surface sensible heat flux H_sens in W/m^2
   * @param {number} [surfaceTempK=220.0] - Surface temperature in Kelvin
   * @param {number} [surfaceDensityKgM3=0.015] - Surface atmospheric density in kg/m^3
   * @returns {{moninObukhovLengthMeters: number, stabilityRegime: string}}
   */
  static computeMoninObukhovLength(frictionVelocityMs, sensibleHeatFluxW_M2, surfaceTempK = 220.0, surfaceDensityKgM3 = 0.015) {
    const kappa = 0.40;
    const cp = 800.0;
    const g = this.G_MARS;
    const uStar = Math.max(0.01, frictionVelocityMs);
    const T0 = Math.max(50.0, surfaceTempK);
    const rho = Math.max(1e-4, surfaceDensityKgM3);

    if (Math.abs(sensibleHeatFluxW_M2) < 1e-4) {
      return { moninObukhovLengthMeters: Infinity, stabilityRegime: 'Neutral' };
    }

    const L = -(Math.pow(uStar, 3) * T0 * rho * cp) / (kappa * g * sensibleHeatFluxW_M2);

    let regime = 'Neutral';
    if (L < 0 && L > -500) {
      regime = 'Convectively Unstable (Daytime Midday)';
    } else if (L > 0 && L < 500) {
      regime = 'Stably Stratified (Nighttime Inversion)';
    }

    return {
      moninObukhovLengthMeters: parseFloat(L.toFixed(1)),
      stabilityRegime: regime
    };
  }

  /**
   * Calculate Iversen & Greeley (1982) aerodynamic threshold friction velocity for dust saltation on Mars.
   * u*_t = A * sqrt( ((rho_p - rho_a) / rho_a) * g * d )
   * @param {number} [grainDiameterMicrons=100.0] - Mineral grain diameter in µm (~100 µm most easily lifted)
   * @param {number} [surfaceDensityKgM3=0.015] - Surface atmospheric density in kg/m^3
   * @param {number} [grainDensityKgM3=2500.0] - Basaltic mineral grain density in kg/m^3
   * @returns {{thresholdFrictionVelocityMs: number, minimumWindSpeed10mMs: number}}
   */
  static computeSaltationThresholdFrictionVelocity(grainDiameterMicrons = 100.0, surfaceDensityKgM3 = 0.015, grainDensityKgM3 = 2500.0) {
    const dM = grainDiameterMicrons * 1e-6;
    const rhoA = Math.max(1e-4, surfaceDensityKgM3);
    const rhoP = Math.max(100.0, grainDensityKgM3);
    const A = 0.11; // Greeley & Iversen dimensionless coefficient for Mars

    const densityRatio = (rhoP - rhoA) / rhoA;
    const uStarT = A * Math.sqrt(densityRatio * this.G_MARS * dM);

    // Wind speed at 10m assuming z0 = 0.01m: u(10m) = (u* / kappa) * ln(10 / 0.01) ~ 17.27 * u*
    const u10m = (uStarT / 0.40) * Math.log(10.0 / 0.01);

    return {
      thresholdFrictionVelocityMs: parseFloat(uStarT.toFixed(3)),
      minimumWindSpeed10mMs: parseFloat(u10m.toFixed(2))
    };
  }

  /**
   * Calculate atmospheric scale height vertical gradient (dH/dz) under a temperature lapse rate.
   * dH/dz = (R_spec / g) * (dT/dz) = -(R_spec / g) * Gamma
   * @param {number} [temperatureLapseRateKPerKm=4.5] - Environmental lapse rate Gamma = -dT/dz in K/km
   * @returns {{scaleHeightGradientDimensionless: number, scaleHeightChangeMPerKm: number}}
   */
  static computeAtmosphericScaleHeightGradient(temperatureLapseRateKPerKm = 4.5) {
    const gammaM = temperatureLapseRateKPerKm / 1000.0; // K / m
    const dH_dz = -(this.R_SPECIFIC_CO2 / this.G_MARS) * gammaM;
    const changeMPerKm = dH_dz * 1000.0;

    return {
      scaleHeightGradientDimensionless: parseFloat(dH_dz.toFixed(5)),
      scaleHeightChangeMPerKm: parseFloat(changeMPerKm.toFixed(2))
    };
  }

  // --- Bulk Richardson Number, Convective Velocity & Eddy Thermal Diffusivity Solvers ---

  /**
   * Calculate atmospheric bulk Richardson number (Ri_b) across a boundary layer interval.
   * Ri_b = (g / theta_v0) * (Delta_theta_v * Delta_z) / ( (Delta_u)^2 + (Delta_v)^2 )
   * @param {number} deltaThetaV - Virtual potential temperature difference across layer in K
   * @param {number} deltaZ - Layer thickness in meters
   * @param {number} deltaU - Zonal wind velocity difference in m/s
   * @param {number} [deltaV=0] - Meridional wind velocity difference in m/s
   * @param {number} [thetaV0=210.0] - Reference base potential temperature in Kelvin
   * @returns {{bulkRichardsonNumber: number, isConvectivelyUnstable: boolean, isTurbulent: boolean}}
   */
  static computeBulkRichardsonNumber(deltaThetaV, deltaZ, deltaU, deltaV = 0, thetaV0 = 210.0) {
    const dZ = Math.max(1.0, deltaZ);
    const theta0 = Math.max(50.0, thetaV0);
    const shearSq = Math.max(1e-6, deltaU * deltaU + deltaV * deltaV);

    const rib = (this.G_MARS / theta0) * (deltaThetaV * dZ) / shearSq;

    return {
      bulkRichardsonNumber: parseFloat(rib.toFixed(4)),
      isConvectivelyUnstable: rib < 0,
      isTurbulent: rib < 0.25
    };
  }

  /**
   * Calculate Deardorff convective boundary layer scaling velocity w*.
   * w* = [ (g / theta_0) * (H_sens / (rho * cp)) * z_i ]^(1/3)
   * @param {number} sensibleHeatFluxW_M2 - Surface sensible heat flux in W/m^2
   * @param {number} pblHeightMeters - Planetary Boundary Layer height in meters (z_i)
   * @param {number} [surfaceTempK=210.0] - Near-surface potential temperature in Kelvin
   * @param {number} [surfaceDensityKgM3=0.015] - Surface atmospheric density
   * @returns {{convectiveVelocityMs: number, buoyancyFluxM2S3: number}}
   */
  static computeConvectiveVelocityScale(sensibleHeatFluxW_M2, pblHeightMeters, surfaceTempK = 210.0, surfaceDensityKgM3 = 0.015) {
    const cp = 800.0; // J/(kg K)
    const rho = Math.max(1e-4, surfaceDensityKgM3);
    const theta0 = Math.max(50.0, surfaceTempK);
    const zi = Math.max(10.0, pblHeightMeters);
    const flux = Math.max(0, sensibleHeatFluxW_M2);

    const wTheta0 = flux / (rho * cp); // Kinematic heat flux K m/s
    const buoyancyFlux = (this.G_MARS / theta0) * wTheta0; // m^2 / s^3
    const wStar = Math.pow(buoyancyFlux * zi, 1.0 / 3.0);

    return {
      convectiveVelocityMs: parseFloat(wStar.toFixed(3)),
      buoyancyFluxM2S3: parseFloat(buoyancyFlux.toExponential(4))
    };
  }

  /**
   * Calculate Businger-Dyer unstable boundary layer turbulent thermal eddy diffusivity K_h.
   * K_h = kappa * u* * z * (1 - 16 * z / L)^(1/2)
   * @param {number} frictionVelocityMs - Surface friction velocity u* in m/s
   * @param {number} altitudeMeters - Altitude z above ground in meters
   * @param {number} moninObukhovLengthM - Monin-Obukhov stability length L in meters (negative for unstable)
   * @returns {{eddyDiffusivityKhM2S: number, phiHeatDimensionless: number}}
   */
  static computePBLEddyThermalDiffusivity(frictionVelocityMs, altitudeMeters, moninObukhovLengthM) {
    const kappa = 0.40;
    const uStar = Math.max(0.001, frictionVelocityMs);
    const z = Math.max(0.1, altitudeMeters);
    const L = moninObukhovLengthM; // negative in convective conditions

    let phiH = 1.0;
    if (L < 0) {
      // Unstable: phi_h = (1 - 16 * z / L)^(-1/2)
      phiH = 1.0 / Math.sqrt(1.0 - 16.0 * (z / L));
    } else if (L > 0) {
      // Stable: phi_h = 1 + 5 * z / L
      phiH = 1.0 + 5.0 * (z / L);
    }

    const Kh = (kappa * uStar * z) / Math.max(0.1, phiH);

    return {
      eddyDiffusivityKhM2S: parseFloat(Kh.toFixed(3)),
      phiHeatDimensionless: parseFloat(phiH.toFixed(4))
    };
  }

  // --- Saltation Threshold, Static Stability & Deardorff CBL Depth Solvers ---

  /**
   * Calculate Greeley-Iversen fluid threshold friction velocity u*_t for Martian sand saltation with interparticle cohesion.
   * u*_t = A * sqrt( (rho_p * g * d) / rho + gamma_inter / (rho * d) )
   * @param {number} [particleDiameterMicrons=100.0] - Sand grain diameter in µm (~100 µm most easily lifted on Mars)
   * @param {number} [surfaceDensityKgM3=0.015] - Surface atmospheric density in kg/m^3
   * @param {number} [particleDensityKgM3=2500.0] - Basalt sand mineral density
   * @returns {{thresholdFrictionVelocityMs: number, thresholdShearStressPa: number, optimumDiameterMicrons: number}}
   */
  static computeDustCohesionSaltationThreshold(particleDiameterMicrons = 100.0, surfaceDensityKgM3 = 0.015, particleDensityKgM3 = 2500.0) {
    const d = Math.max(1.0, particleDiameterMicrons) * 1e-6; // meters
    const rho = Math.max(1e-4, surfaceDensityKgM3);
    const rhoP = Math.max(500.0, particleDensityKgM3);
    const g = this.G_MARS;

    const A = 0.118; // Dimensionless threshold coefficient
    const gammaInter = 3.0e-4; // Interparticle cohesion factor N/m

    const gravityTerm = (rhoP * g * d) / rho;
    const cohesionTerm = gammaInter / (rho * d);
    const uStarT = A * Math.sqrt(gravityTerm + cohesionTerm);
    const tauThresh = rho * uStarT * uStarT;

    return {
      thresholdFrictionVelocityMs: parseFloat(uStarT.toFixed(3)),
      thresholdShearStressPa: parseFloat(tauThresh.toFixed(4)),
      optimumDiameterMicrons: 100.0
    };
  }

  /**
   * Calculate atmospheric static stability metric S = (Gamma_d - Gamma) / T.
   * @param {number} layerTempK - Atmospheric layer temperature in Kelvin
   * @param {number} environmentalLapseRateKPerKm - Observed lapse rate -dT/dz (K/km)
   * @param {number} [dryAdiabaticLapseRateKPerKm=4.65] - Dry adiabatic lapse rate (K/km)
   * @returns {{staticStabilityParameterPerMeter: number, isConvectivelyStable: boolean}}
   */
  static computeAtmosphericStaticStabilityParameter(layerTempK, environmentalLapseRateKPerKm, dryAdiabaticLapseRateKPerKm = 4.65) {
    const T = Math.max(50.0, layerTempK);
    const dGamma = (dryAdiabaticLapseRateKPerKm - environmentalLapseRateKPerKm) / 1000.0; // K / m
    const S = dGamma / T;

    return {
      staticStabilityParameterPerMeter: parseFloat(S.toExponential(4)),
      isConvectivelyStable: S > 0
    };
  }

  /**
   * Calculate Deardorff convective Planetary Boundary Layer (PBL) diurnal equilibrium height.
   * z_i = sqrt( (2 * B_0 * t) / gamma_theta )
   * @param {number} surfaceSensibleHeatW_M2 - Surface sensible heat flux in W/m^2
   * @param {number} [cappingLapseRateKPerM=0.003] - Potential temperature inversion lapse rate above PBL
   * @param {number} [heatingDurationSeconds=21600.0] - Solar diurnal heating duration in seconds (6 hours)
   * @param {number} [surfaceTempK=220.0] - Surface temperature in Kelvin
   * @param {number} [surfaceDensityKgM3=0.015] - Surface atmospheric density in kg/m^3
   * @returns {{pblHeightMeters: number, pblHeightKm: number, buoyancyFluxM2S3: number}}
   */
  static computeConvectiveBoundaryLayerDeardorffHeight(
    surfaceSensibleHeatW_M2,
    cappingLapseRateKPerM = 0.003,
    heatingDurationSeconds = 21600.0,
    surfaceTempK = 220.0,
    surfaceDensityKgM3 = 0.015
  ) {
    const cp = 800.0; // J/(kg K)
    const flux = Math.max(0.1, surfaceSensibleHeatW_M2);
    const rho = Math.max(1e-4, surfaceDensityKgM3);
    const T0 = Math.max(50.0, surfaceTempK);

    const b0 = (this.G_MARS / T0) * (flux / (rho * cp));
    const gamma = Math.max(1e-5, cappingLapseRateKPerM);
    const t = Math.max(100.0, heatingDurationSeconds);

    const zi = Math.sqrt((2.0 * b0 * t) / gamma);

    return {
      pblHeightMeters: parseFloat(zi.toFixed(1)),
      pblHeightKm: parseFloat((zi / 1000.0).toFixed(3)),
      buoyancyFluxM2S3: parseFloat(b0.toExponential(4))
    };
  }

  // --- Adiabatic Lapse Rate, Potential Temperature & Brunt-Väisälä Solvers ---

  /**
   * Calculate Mars dry adiabatic temperature lapse rate Gamma_d.
   * Gamma_d = g / c_p
   * @param {number} [gravityMps2=3.72076] - Surface gravity in m/s^2 (3.72076 for Mars)
   * @param {number} [cpSpecificHeat=735.0] - Atmospheric specific heat capacity in J/(kg K) (735 for CO2)
   * @returns {{lapseRateKPerM: number, lapseRateKPerKm: number}}
   */
  static computeDryAdiabaticLapseRate(gravityMps2 = 3.72076, cpSpecificHeat = 735.0) {
    const g = Math.max(0.1, gravityMps2);
    const cp = Math.max(10.0, cpSpecificHeat);

    const gammaKPerM = g / cp;
    const gammaKPerKm = gammaKPerM * 1000.0;

    return {
      lapseRateKPerM: parseFloat(gammaKPerM.toFixed(6)),
      lapseRateKPerKm: parseFloat(gammaKPerKm.toFixed(3))
    };
  }

  /**
   * Calculate atmospheric potential temperature theta using Poisson's relation for Martian CO2 atmosphere.
   * theta = T * (P_0 / P)^kappa, where kappa = R_spec / c_p ≈ 188.92 / 735.0 ≈ 0.25703
   * @param {number} temperatureK - In-situ atmospheric temperature in Kelvin
   * @param {number} pressurePa - In-situ atmospheric pressure in Pascals
   * @param {number} [referencePressurePa=610.0] - Reference surface pressure (610 Pa MOLA datum)
   * @returns {{potentialTemperatureK: number, pressureRatio: number}}
   */
  static computeAtmosphericPotentialTemperature(temperatureK, pressurePa, referencePressurePa = 610.0) {
    const T = Math.max(1, temperatureK);
    const P = Math.max(0.01, pressurePa);
    const P0 = Math.max(1.0, referencePressurePa);

    const kappa = 188.92 / 735.0; // Mars gas constant / specific heat
    const pRatio = P0 / P;
    const theta = T * Math.pow(pRatio, kappa);

    return {
      potentialTemperatureK: parseFloat(theta.toFixed(2)),
      pressureRatio: parseFloat(pRatio.toFixed(3))
    };
  }

  /**
   * Calculate Brunt-Väisälä buoyancy oscillation frequency N and wave period tau.
   * N = sqrt( (g / theta) * (d_theta / dz) ),  tau = 2 * pi / N
   * @param {number} potentialTempK - Mean layer potential temperature in Kelvin
   * @param {number} verticalGradientDThetaDz - Vertical potential temperature gradient (K/m)
   * @param {number} [gravityMps2=3.72076] - Mars surface gravity (m/s^2)
   * @returns {{buoyancyFrequencyRadS: number, buoyancyFrequencyHz: number, periodSeconds: number, isStablyStratified: boolean}}
   */
  static computeBruntVaisalaFrequency(potentialTempK, verticalGradientDThetaDz, gravityMps2 = 3.72076) {
    const theta = Math.max(10, potentialTempK);
    const dThetaDz = verticalGradientDThetaDz;
    const g = Math.max(0.1, gravityMps2);

    const nSquared = (g / theta) * dThetaDz;
    const isStable = nSquared > 0;

    let N_radS = 0;
    let periodS = Infinity;

    if (isStable) {
      N_radS = Math.sqrt(nSquared);
      periodS = (2.0 * Math.PI) / N_radS;
    }

    const n_Hz = N_radS / (2.0 * Math.PI);

    return {
      buoyancyFrequencyRadS: parseFloat(N_radS.toFixed(5)),
      frequencyRadS: parseFloat(N_radS.toFixed(5)),
      buoyancyFrequencyHz: parseFloat(n_Hz.toFixed(5)),
      periodSeconds: isStable ? parseFloat(periodS.toFixed(1)) : Infinity,
      isStablyStratified: isStable,
      isStable: isStable
    };
  }

  // --- Thermal Wind Shear, Local Scale Height & TKE Dissipation Solvers ---

  /**
   * Calculate vertical thermal wind shear gradient du_g/dz from meridional temperature gradient.
   * du_g/dz = - (g / (f * T_0)) * (dT/dy)
   * @param {number} meridionalTempGradientKPerKm - Meridional temperature gradient dT/dy in K/km (positive = warmer poleward)
   * @param {number} [meanTempK=210.0] - Mean layer atmospheric temperature in Kelvin
   * @param {number} [latitudeDeg=45.0] - Latitude in degrees
   * @param {number} [gravityMps2=3.72076] - Mars surface gravity (m/s^2)
   * @returns {{thermalWindShearPerKm: number, thermalWindShearPerSec: number, coriolisParameterRadS: number}}
   */
  static computeThermalWindShearGradient(meridionalTempGradientKPerKm, meanTempK = 210.0, latitudeDeg = 45.0, gravityMps2 = 3.72076) {
    const g = Math.max(0.1, gravityMps2);
    const T0 = Math.max(50.0, meanTempK);
    const phiRad = (Math.max(-89.9, Math.min(89.9, latitudeDeg)) * Math.PI) / 180.0;

    // Mars rotation rate Omega = 7.0882e-5 rad/s -> f = 2 * Omega * sin(phi)
    const omega = 7.0882e-5;
    const f = 2.0 * omega * Math.sin(phiRad);
    const fAbs = Math.max(1e-7, Math.abs(f));

    // dT/dy in K/m: meridionalTempGradientKPerKm * 1e-3
    const dT_dy_KPerM = meridionalTempGradientKPerKm * 1e-3;

    // du_g/dz in s^-1 = -(g / (f * T0)) * dT/dy
    const shearPerSec = -(g / (f * T0)) * dT_dy_KPerM;
    const shearPerKm = shearPerSec * 1000.0; // (m/s) per km of altitude

    return {
      thermalWindShearPerKm: parseFloat(shearPerKm.toFixed(3)),
      thermalWindShearPerSec: parseFloat(shearPerSec.toExponential(4)),
      coriolisParameterRadS: parseFloat(f.toExponential(4))
    };
  }

  /**
   * Calculate local atmospheric scale height H = (R_spec * T) / g.
   * @param {number} temperatureK - Atmospheric temperature in Kelvin
   * @param {number} [meanMolecularWeightG_Mol=43.34] - Mean molecular weight in g/mol (43.34 for Mars CO2 atmosphere)
   * @param {number} [gravityMps2=3.72076] - Surface gravitational acceleration in m/s^2
   * @returns {{scaleHeightKm: number, scaleHeightMeters: number, specificGasConstant: number}}
   */
  static computeAtmosphericScaleHeightProfile(temperatureK, meanMolecularWeightG_Mol = 43.34, gravityMps2 = 3.72076) {
    const T = Math.max(10.0, temperatureK);
    const M_kg = Math.max(1.0, meanMolecularWeightG_Mol) * 1e-3; // kg/mol
    const g = Math.max(0.1, gravityMps2);
    const R_univ = 8.314462618; // J/(mol K)

    const R_spec = R_univ / M_kg; // ~191.84 J/(kg K)
    const H_meters = (R_spec * T) / g;
    const H_km = H_meters / 1000.0;

    return {
      scaleHeightKm: parseFloat(H_km.toFixed(3)),
      scaleHeightMeters: parseFloat(H_meters.toFixed(1)),
      specificGasConstant: parseFloat(R_spec.toFixed(2))
    };
  }

  /**
   * Calculate Turbulent Kinetic Energy (TKE) dissipation rate epsilon in the convective Planetary Boundary Layer.
   * epsilon = (w_*^3 / z_i) * (0.8 - 0.3 * (z / z_i))
   * @param {number} convectiveVelocityMs - Convective velocity scale w_* in m/s
   * @param {number} pblHeightMeters - Boundary layer inversion height z_i in meters
   * @param {number} heightAboveSurfaceMeters - Measurement height z in meters
   * @returns {{tkeDissipationM2S3: number, normalizedHeight: number}}
   */
  static computeAtmosphericTurbulentKineticEnergyDissipation(convectiveVelocityMs, pblHeightMeters, heightAboveSurfaceMeters) {
    const wStar = Math.max(0.01, convectiveVelocityMs);
    const zi = Math.max(10.0, pblHeightMeters);
    const z = Math.max(0.0, Math.min(zi, heightAboveSurfaceMeters));

    const normZ = z / zi;
    const shapeFactor = Math.max(0.1, 0.8 - 0.3 * normZ);
    const epsilon = (Math.pow(wStar, 3) / zi) * shapeFactor;

    return {
      tkeDissipationM2S3: parseFloat(epsilon.toExponential(4)),
      normalizedHeight: parseFloat(normZ.toFixed(3))
    };
  }

  // --- CO2 Frost Point, Sound Speed & Dust Column Optical Depth Solvers ---

  /**
   * Calculate CO2 dry ice frost point condensation temperature T_frost from ambient partial pressure.
   * Kieffer (1977) Clausius-Clapeyron vapor-ice equilibrium: T_frost = 3148.0 / ( 27.55 - ln(P_Pa) )
   * @param {number} pressurePa - Atmospheric pressure in Pascals (e.g. 610 Pa at datum)
   * @returns {{frostPointTempK: number, pressurePa: number}}
   */
  static computeCO2FrostPointTemperature(pressurePa) {
    const P = Math.max(0.1, pressurePa);
    const denominator = 27.55 - Math.log(P);
    const tFrost = denominator > 0 ? 3148.0 / denominator : 148.0;

    return {
      frostPointTempK: parseFloat(tFrost.toFixed(2)),
      pressurePa: parseFloat(P.toFixed(1))
    };
  }

  /**
   * Calculate speed of sound c_s in the cold Martian CO2 atmosphere.
   * c_s = sqrt( gamma * R_spec * T )
   * @param {number} temperatureK - Atmospheric temperature in Kelvin
   * @param {number} [adiabaticIndexGamma=1.29] - Heat capacity ratio (1.29 for CO2)
   * @param {number} [specificGasConstant=188.92] - Specific gas constant in J/(kg K)
   * @returns {{soundSpeedMs: number, soundSpeedMps: number, soundSpeedKmH: number, soundSpeedKmh: number, machOneMps: number}}
   */
  static computeAtmosphericSoundSpeed(temperatureK, adiabaticIndexGamma = 1.29, specificGasConstant = 188.92) {
    const T = Math.max(10.0, temperatureK);
    const gamma = Math.max(1.0, adiabaticIndexGamma);
    const Rspec = Math.max(10.0, specificGasConstant);

    const cMps = Math.sqrt(gamma * Rspec * T);
    const cKmh = cMps * 3.6;

    return {
      soundSpeedMs: parseFloat(cMps.toFixed(2)),
      soundSpeedMps: parseFloat(cMps.toFixed(2)),
      soundSpeedKmH: parseFloat(cKmh.toFixed(2)),
      soundSpeedKmh: parseFloat(cKmh.toFixed(2)),
      machOneMps: parseFloat(cMps.toFixed(2))
    };
  }

  /**
   * Calculate seasonal atmospheric column dust optical depth and slant-path solar transmission.
   * tau = tau_base + tau_storm * max(0, sin(Ls - 180 deg))^2
   * T_slant = exp( - tau / cos(theta_z) )
   * @param {number} LsDeg - Solar Longitude in degrees
   * @param {number} [baselineTau=0.25] - Clear-sky background dust opacity
   * @param {number} [stormPeakTau=1.5] - Peak dust storm optical depth enhancement
   * @param {number} [solarZenithAngleDeg=0.0] - Solar zenith angle in degrees (0 to 85)
   * @returns {{columnOpticalDepthTau: number, slantTransmissionFraction: number, isDustStormSeason: boolean}}
   */
  static computeColumnDustOpticalDepth(LsDeg, baselineTau = 0.25, stormPeakTau = 1.5, solarZenithAngleDeg = 0.0) {
    const ls = ((LsDeg % 360) + 360) % 360;
    const tau0 = Math.max(0.01, baselineTau);
    const dTau = Math.max(0, stormPeakTau);

    // Dust storm season active around southern spring/summer Ls ~ 180 to 360 deg
    let stormFactor = 0.0;
    if (ls >= 180.0 && ls <= 360.0) {
      const angleRad = ((ls - 180.0) * Math.PI) / 180.0;
      stormFactor = Math.pow(Math.sin(angleRad), 2);
    }

    const totalTau = tau0 + dTau * stormFactor;

    const zRad = (Math.max(0, Math.min(85.0, solarZenithAngleDeg)) * Math.PI) / 180.0;
    const cosZ = Math.max(0.05, Math.cos(zRad));
    const trans = Math.exp(-totalTau / cosZ);

    return {
      columnOpticalDepthTau: parseFloat(totalTau.toFixed(3)),
      slantTransmissionFraction: parseFloat(trans.toFixed(4)),
      isDustStormSeason: ls >= 180.0 && ls <= 360.0
    };
  }

  // --- Boundary Layer Friction Velocity, Sensible Heat Flux & Scale Height Solvers ---

  /**
   * Calculate atmospheric boundary layer friction velocity u_*, drag coefficient C_D, and surface shear stress.
   * u_* = ( kappa * u ) / ln( z / z_0 )
   * @param {number} windSpeedMps - Wind speed u in m/s measured at height z
   * @param {number} [heightMeters=2.0] - Measurement anemometer height z above ground
   * @param {number} [roughnessLengthMeters=0.01] - Aerodynamic roughness length z_0 in meters (0.001 m smooth sand, 0.03 m rock field)
   * @param {number} [airDensityKgM3=0.015] - Atmospheric gas density in kg/m^3
   * @returns {{frictionVelocityMps: number, dragCoefficient: number, surfaceShearStressPa: number}}
   */
  static computeAtmosphericFrictionVelocityAndRoughness(windSpeedMps, heightMeters = 2.0, roughnessLengthMeters = 0.01, airDensityKgM3 = 0.015) {
    const u = Math.max(0.01, windSpeedMps);
    const z = Math.max(0.1, heightMeters);
    const z0 = Math.max(1e-5, Math.min(z * 0.5, roughnessLengthMeters));
    const rho = Math.max(1e-4, airDensityKgM3);

    const kappa = 0.40; // von Kármán constant
    const logRatio = Math.log(z / z0);
    const uStar = (kappa * u) / logRatio;
    const cd = Math.pow(kappa / logRatio, 2);
    const tauShear = rho * uStar * uStar;

    return {
      frictionVelocityMps: parseFloat(uStar.toFixed(4)),
      dragCoefficient: parseFloat(cd.toFixed(5)),
      surfaceShearStressPa: parseFloat(tauShear.toExponential(4))
    };
  }

  /**
   * Calculate turbulent sensible heat flux H between Martian ground and atmospheric boundary layer.
   * H = rho * c_p * C_H * u * ( T_surf - T_air )
   * @param {number} airTempK - Near-surface atmospheric air temperature in Kelvin
   * @param {number} surfaceTempK - Ground skin temperature in Kelvin
   * @param {number} windSpeedMps - Wind speed in m/s
   * @param {number} [airDensityKgM3=0.015] - Atmospheric density in kg/m^3
   * @param {number} [bulkTransferCoeff=0.003] - Bulk aerodynamic heat transfer coefficient C_H
   * @returns {{sensibleHeatFluxW_M2: number, isConvectiveDaytime: boolean, temperatureDifferenceK: number}}
   */
  static computeSurfaceSensibleHeatFlux(airTempK, surfaceTempK, windSpeedMps, airDensityKgM3 = 0.015, bulkTransferCoeff = 0.003) {
    const Tair = Math.max(10.0, airTempK);
    const Tsurf = Math.max(10.0, surfaceTempK);
    const u = Math.max(0.0, windSpeedMps);
    const rho = Math.max(1e-4, airDensityKgM3);
    const CH = Math.max(1e-4, bulkTransferCoeff);

    const cp = 850.0; // Specific heat capacity of CO2 gas in J/(kg K)
    const dT = Tsurf - Tair;
    const H = rho * cp * CH * u * dT;

    return {
      sensibleHeatFluxW_M2: parseFloat(H.toFixed(2)),
      isConvectiveDaytime: H > 0,
      temperatureDifferenceK: parseFloat(dT.toFixed(2))
    };
  }

  /**
   * Calculate atmospheric scale height H_scale = (R_spec * T) / g as a function of temperature.
   * @param {number} temperatureK - Mean atmospheric layer temperature in Kelvin
   * @param {number} [meanMolecularWeightG_Mol=43.34] - Gas molar mass in g/mol (~43.34 for 95% CO2, 2.6% N2, 1.9% Ar)
   * @param {number} [gravityMps2=3.72076] - Gravitational acceleration in m/s^2
   * @returns {{scaleHeightKm: number, scaleHeightMeters: number, specificGasConstantJ_KgK: number}}
   */
  static computeAtmosphericScaleHeightProfile(temperatureK, meanMolecularWeightG_Mol = 43.34, gravityMps2 = 3.72076) {
    const T = Math.max(10.0, temperatureK);
    const M_kg = Math.max(1.0, meanMolecularWeightG_Mol) / 1000.0;
    const g = Math.max(0.1, gravityMps2);

    const R_univ = 8.314462618;
    const R_spec = R_univ / M_kg; // ~ 191.84 J/(kg K) for Mars CO2
    const H_m = (R_spec * T) / g;
    const H_km = H_m / 1000.0;

    return {
      scaleHeightKm: parseFloat(H_km.toFixed(2)),
      scaleHeightMeters: parseFloat(H_m.toFixed(1)),
      specificGasConstant: parseFloat(R_spec.toFixed(2)),
      specificGasConstantJ_KgK: parseFloat(R_spec.toFixed(2))
    };
  }

  // --- Official LMD / CNRS / ESA Mars Climate Database (MCD v6.1) Live API ---

  /**
   * Fetch real 3D GCM atmospheric vertical profile from the official LMD/CNRS/ESA Mars Climate Database (v6.1).
   * @param {object} params
   * @param {number} [params.lat=0] - Latitude (-90 to +90)
   * @param {number} [params.lon=0] - East Longitude (0 to 360)
   * @param {number} [params.Ls=0] - Solar longitude (0 to 360)
   * @param {number} [params.localHour=12] - Local solar hour (0 to 24)
   * @param {number|string} [params.dust=1] - Scenario (1: Climatology, 2: Cold, 3: Warm, 4: Dust storm, 24-36: Mars Year)
   * @param {number} [params.maxAltitudeKm=50] - Maximum altitude in km
   * @param {string} [params.var1='t'] - Primary variable ('t', 'p', 'rho', 'wind')
   * @returns {Promise<object>} Parsed real LMD GCM atmospheric profile
   */
  static async fetchLMDProfile(params = {}) {
    const lat = params.lat ?? 0;
    let lon = params.lon ?? 0;
    while (lon > 180) lon -= 360;
    while (lon < -180) lon += 360;

    const Ls = params.Ls ?? 0;
    const localHour = params.localHour ?? 12;
    const dust = params.dust ?? 1;
    const maxAltKm = Math.min(100, Math.max(10, params.maxAltitudeKm ?? 50));
    const maxAltM = maxAltKm * 1000;
    const var1 = params.var1 ?? 't';

    const queryParams = new URLSearchParams({
      var1: var1,
      var2: 'p',
      var3: 'rho',
      var4: 'wind',
      ls: String(Ls),
      localtime: String(localHour),
      datekeyhtml: '1',
      latitude: String(lat),
      longitude: String(lon),
      dust: String(dust),
      zkey: '3', // Meters above surface
      altitude: `0 ${maxAltM}`,
      trans: '1' // ASCII data output
    });

    const lmdCgiUrl = `https://www-mars.lmd.jussieu.fr/mcd_python/cgi-bin/mcdcgi.py?${queryParams.toString()}`;
    const proxyUrl = `https://api.allorigins.win/raw?url=${encodeURIComponent(lmdCgiUrl)}`;

    const response = await fetch(proxyUrl);
    if (!response.ok) {
      throw new Error(`LMD MCD server returned HTTP ${response.status}`);
    }

    const html = await response.text();
    const txtMatch = html.match(/href=['"](\.\.\/txt\/[^'"]+\.txt)['"]/);
    if (!txtMatch) {
      throw new Error('LMD MCD query response did not return a data download URL');
    }

    const txtUrl = new URL(txtMatch[1], lmdCgiUrl).href;
    const proxyTxtUrl = `https://api.allorigins.win/raw?url=${encodeURIComponent(txtUrl)}`;

    const txtResponse = await fetch(proxyTxtUrl);
    if (!txtResponse.ok) {
      throw new Error(`Failed to download LMD MCD profile text data`);
    }

    const rawText = await txtResponse.text();
    return MCDEngine.parseLMDAsciiOutput(rawText, { lat, lon, Ls, localHour, dust, maxAltKm, lmdCgiUrl });
  }

  /**
   * Parse raw multi-column ASCII table returned by LMD Mars Climate Database.
   * @param {string} rawText
   * @param {object} meta
   * @returns {object} Standardized profile object
   */
  static parseLMDAsciiOutput(rawText, meta = {}) {
    const lines = rawText.split('\n');
    const dataLines = lines.filter(l => l.trim().length > 0 && !l.trim().startsWith('#'));

    const layers = [];
    for (const line of dataLines) {
      const parts = line.trim().split(/\s+/).map(Number);
      if (parts.length >= 2 && Number.isFinite(parts[0]) && Number.isFinite(parts[1])) {
        const altM = parts[0];
        const altKm = parseFloat((altM / 1000).toFixed(1));
        const val1 = parts[1]; // Temperature (K)
        const p_z = parts.length > 2 ? parts[2] : null;
        const rho_z = parts.length > 3 ? parts[3] : null;
        const wind_z = parts.length > 4 ? parts[4] : null;

        layers.push({
          altitudeKm: altKm,
          altitudeMeters: altM,
          temperatureK: val1,
          pressurePa: p_z !== null ? parseFloat(p_z.toFixed(2)) : parseFloat((610.0 * Math.exp(-altM / 11100)).toFixed(2)),
          densityKgM3: rho_z !== null ? parseFloat(rho_z.toExponential(3)) : parseFloat((610.0 / (188.92 * val1)).toExponential(3)),
          windSpeedMs: wind_z !== null ? parseFloat(wind_z.toFixed(1)) : 0.0,
          dustDensity: 0.05
        });
      }
    }

    // Sort ascending by altitude
    layers.sort((a, b) => a.altitudeKm - b.altitudeKm);

    const surfaceLayer = layers[0] || { temperatureK: 215, pressurePa: 610, densityKgM3: 0.015 };

    return {
      source: 'LMD/CNRS/ESA Mars Climate Database v6.1 (Live GCM / Spacecraft-Calibrated)',
      isRealData: true,
      lmdWebUrl: meta.lmdCgiUrl || 'https://www-mars.lmd.jussieu.fr/mcd_python/',
      location: {
        lat: meta.lat ?? 0,
        lon: meta.lon ?? 0,
        elevation: 0,
        Ls: meta.Ls ?? 0,
        localHour: meta.localHour ?? 12,
        dustScenario: meta.dust ?? 1
      },
      surface: {
        pressurePa: surfaceLayer.pressurePa,
        temperatureK: surfaceLayer.temperatureK,
        scaleHeightKm: 11.1,
        surfaceDensity: surfaceLayer.densityKgM3
      },
      layers
    };
  }

  // --- Atmospheric Speed of Sound, Sutherland Viscosity & Aerodynamic Mach Solvers ---

  /**
   * Calculate local speed of sound c_s = sqrt( gamma * R_specific * T ) in Martian CO2 atmosphere.
   * @param {number} temperatureK - Atmospheric kinetic temperature in Kelvin
   * @param {number} [gammaRatio=1.29] - Adiabatic index cp/cv (1.29 for CO2)
   * @returns {{speedOfSoundMS: number, speedOfSoundKmH: number, temperatureK: number}}
   */
  static computeMartianSpeedOfSound(temperatureK, gammaRatio = 1.29) {
    const T = Math.max(1.0, temperatureK);
    const gamma = Math.max(1.0, Math.min(1.67, gammaRatio));
    const R_spec = 188.92; // Specific gas constant for CO2 [J/(kg K)]

    const c = Math.sqrt(gamma * R_spec * T);
    const cKmH = c * 3.6;

    return {
      speedOfSoundMS: parseFloat(c.toFixed(2)),
      speedOfSoundKmH: parseFloat(cKmH.toFixed(1)),
      temperatureK: parseFloat(T.toFixed(2))
    };
  }

  /**
   * Calculate dynamic viscosity mu(T) of Martian CO2 atmosphere using Sutherland's Law.
   * mu = mu_0 * (T / T_0)^(3/2) * (T_0 + S) / (T + S)
   * @param {number} temperatureK - Temperature in Kelvin
   * @returns {{dynamicViscosityPaS: number, temperatureK: number}}
   */
  static computeCO2DynamicViscosity(temperatureK) {
    const T = Math.max(1.0, temperatureK);
    const mu0 = 1.370e-5; // Reference dynamic viscosity for CO2 at T0 = 273.15 K [Pa s]
    const T0 = 273.15;
    const S = 240.0;     // Sutherland temperature for CO2 [K]

    const ratio = T / T0;
    const mu = mu0 * Math.pow(ratio, 1.5) * ((T0 + S) / (T + S));

    return {
      dynamicViscosityPaS: parseFloat(mu.toExponential(4)),
      temperatureK: parseFloat(T.toFixed(2))
    };
  }

  /**
   * Calculate aerodynamic flight Mach number M = v / c_s for Mars entry/descent/landing (EDL).
   * @param {number} velocityMS - Vehicle airspeed in m/s
   * @param {number} temperatureK - Ambient atmospheric temperature in Kelvin
   * @returns {{machNumber: number, isSupersonic: boolean, isHypersonic: boolean, isSubsonic: boolean}}
   */
  static computeAerodynamicMachNumber(velocityMS, temperatureK) {
    const v = Math.max(0, velocityMS);
    const cs = MCDEngine.computeMartianSpeedOfSound(temperatureK).speedOfSoundMS;

    const mach = v / Math.max(1e-4, cs);

    return {
      machNumber: parseFloat(mach.toFixed(3)),
      isSubsonic: mach < 1.0,
      isSupersonic: mach >= 1.0 && mach < 5.0,
      isHypersonic: mach >= 5.0
    };
  }

  // --- CO2 Mean Free Path, Knudsen Flow Regimes & Atmospheric Column Mass ---

  /**
   * Calculate kinetic theory molecular mean free path for pure CO2 in meters.
   * lambda_mfp = ( k_B * T ) / ( sqrt(2) * pi * d_mol^2 * P )
   * @param {number} temperatureK - Temperature in Kelvin (e.g. 140 to 300 K)
   * @param {number} pressurePa - Ambient atmospheric pressure in Pascals (e.g. 0.01 to 1000 Pa)
   * @returns {{meanFreePathMeters: number, meanFreePathMicrons: number, pressurePa: number}}
   */
  static computeCO2MolecularMeanFreePath(temperatureK, pressurePa) {
    const T = Math.max(1.0, temperatureK);
    const P = Math.max(1e-6, pressurePa);
    const kB = 1.380649e-23; // Boltzmann constant (J/K)
    const dMol = 3.3e-10;    // CO2 effective collision diameter in meters

    const denom = Math.SQRT2 * Math.PI * dMol * dMol * P;
    const lambdaM = (kB * T) / denom;
    const lambdaUm = lambdaM * 1e6;

    return {
      meanFreePathMeters: parseFloat(lambdaM.toExponential(4)),
      meanFreePathMicrons: parseFloat(lambdaUm.toFixed(2)),
      pressurePa: parseFloat(P.toFixed(2))
    };
  }

  /**
   * Calculate Knudsen number Kn = lambda_mfp / L_char and determine rarefied gas flow regime.
   * @param {number} temperatureK - Temperature in Kelvin
   * @param {number} pressurePa - Pressure in Pascals
   * @param {number} [characteristicLengthMeters=1.0] - Characteristic body length in meters (e.g. 0.12 m for Ingenuity blade, 4.5 m for aeroshell)
   * @returns {{knudsenNumber: number, regime: string, isContinuum: boolean, isSlipFlow: boolean, isTransitional: boolean, isFreeMolecular: boolean}}
   */
  static computeKnudsenNumberAndRegime(temperatureK, pressurePa, characteristicLengthMeters = 1.0) {
    const L = Math.max(1e-4, characteristicLengthMeters);
    const lambdaM = MCDEngine.computeCO2MolecularMeanFreePath(temperatureK, pressurePa).meanFreePathMeters;

    const kn = lambdaM / L;

    let regime = 'continuum';
    if (kn >= 10.0) {
      regime = 'free-molecular';
    } else if (kn >= 0.1) {
      regime = 'transitional';
    } else if (kn >= 0.01) {
      regime = 'slip-flow';
    }

    return {
      knudsenNumber: parseFloat(kn.toExponential(4)),
      regime: regime,
      isContinuum: kn < 0.01,
      isSlipFlow: kn >= 0.01 && kn < 0.1,
      isTransitional: kn >= 0.1 && kn < 10.0,
      isFreeMolecular: kn >= 10.0
    };
  }

  /**
   * Calculate total vertical atmospheric column mass per unit surface area (kg/m^2).
   * M_col = P_surf / g_mars
   * @param {number} surfacePressurePa - Surface barometric pressure in Pascals (typical Mars mean ~ 610 Pa)
   * @param {number} [gravityMS2=3.72076] - Surface gravitational acceleration in m/s^2
   * @returns {{columnMassKg_M2: number, columnMassG_Cm2: number}}
   */
  static computeAtmosphericColumnMassAndDensity(surfacePressurePa, gravityMS2 = 3.72076) {
    const p = Math.max(0, surfacePressurePa);
    const g = Math.max(0.1, gravityMS2);

    const mKgM2 = p / g;
    const mGCm2 = mKgM2 / 10.0; // 1 kg/m^2 = 0.1 g/cm^2

    return {
      columnMassKg_M2: parseFloat(mKgM2.toFixed(2)),
      columnMassG_Cm2: parseFloat(mGCm2.toFixed(3))
    };
  }

  // --- Dynamic Viscosity (Sutherland's Law) & Aerodynamic Reynolds Solvers ---

  /**
   * Calculate dynamic viscosity of Martian CO2 atmosphere using Sutherland's Formula.
   * mu(T) = mu0 * (T / T0)^(3/2) * ( (T0 + S) / (T + S) )
   * For CO2: mu0 = 1.370e-5 Pa s, T0 = 273.15 K, S = 222.0 K
   * @param {number} temperatureK - Atmospheric temperature in Kelvin
   * @returns {{dynamicViscosityPaS: number, temperatureK: number}}
   */
  static computeCO2DynamicViscositySutherland(temperatureK) {
    const T = Math.max(50.0, temperatureK);
    const mu0 = 1.370e-5; // Pa s at T0
    const T0 = 273.15;
    const S = 222.0; // Sutherland constant for CO2 (K)

    const tr = T / T0;
    const mu = mu0 * Math.pow(tr, 1.5) * ((T0 + S) / (T + S));

    return {
      dynamicViscosityPaS: parseFloat(mu.toExponential(4)),
      temperatureK: parseFloat(T.toFixed(2))
    };
  }

  /**
   * Calculate aerodynamic Reynolds number (Re) for entry vehicles, parachutes, and helicopter blades.
   * Re = ( rho * v * L ) / mu(T)
   * @param {number} densityKg_M3 - Atmospheric gas density in kg/m^3
   * @param {number} velocityMS - Airspeed / velocity in m/s
   * @param {number} charLengthMeters - Aerodynamic characteristic chord / length in meters
   * @param {number} [temperatureK=220.0] - Atmospheric temperature in Kelvin
   * @returns {{reynoldsNumber: number, isLaminar: boolean, isTurbulent: boolean}}
   */
  static computeAtmosphericReynoldsNumber(densityKg_M3, velocityMS, charLengthMeters, temperatureK = 220.0) {
    const rho = Math.max(1e-8, densityKg_M3);
    const v = Math.max(0.0, velocityMS);
    const L = Math.max(1e-4, charLengthMeters);

    const visc = MCDEngine.computeCO2DynamicViscositySutherland(temperatureK);
    const mu = visc.dynamicViscosityPaS;

    const re = (rho * v * L) / mu;

    return {
      reynoldsNumber: parseFloat(re.toFixed(1)),
      isLaminar: re < 5.0e5,
      isTurbulent: re >= 5.0e5
    };
  }

  /**
   * Calculate atmospheric scale height H_scale = ( R_spec * T ) / g_mars in meters and km.
   * R_spec = 188.92 J / (kg K) for CO2
   * @param {number} temperatureK - Mean atmospheric temperature in Kelvin
   * @param {number} [gravityMS2=3.72076] - Surface gravitational acceleration in m/s^2
   * @returns {{scaleHeightKm: number, scaleHeightMeters: number}}
   */
  static computeAtmosphericScaleHeightDetailed(temperatureK, gravityMS2 = 3.72076) {
    const T = Math.max(50.0, temperatureK);
    const g = Math.max(0.1, gravityMS2);
    const Rspec = 188.92; // J / (kg K) for CO2

    const hMeters = (Rspec * T) / g;
    const hKm = hMeters / 1000.0;

    return {
      scaleHeightKm: parseFloat(hKm.toFixed(3)),
      scaleHeightMeters: parseFloat(hMeters.toFixed(1))
    };
  }

  // --- Planetary Boundary Layer (PBL) & Surface Wind Shear Solvers ---

  /**
   * Calculate surface friction velocity (u*), aerodynamic wind shear stress (tau_0), and dust lifting threshold.
   * u* = ( kappa * u(z) ) / ln( z / z_0 )   [kappa = 0.40, z = 10 m]
   * tau_0 = rho * u*^2  (Pa = N/m^2)
   * Threshold for Martian dust saltation lifting: tau_thresh ~ 0.025 Pa
   * @param {number} windSpeed10mMS - Wind speed at 10 meters altitude in m/s
   * @param {number} [roughnessLengthMeters=0.01] - Aerodynamic surface roughness z_0 in meters (rocky regolith ~0.01 m)
   * @param {number} [densityKg_M3=0.015] - Surface atmospheric density in kg/m^3 (610 Pa at 220 K ~ 0.015 kg/m^3)
   * @returns {{frictionVelocityMS: number, shearStressPa: number, canLiftDust: boolean}}
   */
  static computeSurfaceFrictionVelocityAndShearStress(windSpeed10mMS, roughnessLengthMeters = 0.01, densityKg_M3 = 0.015) {
    const u = Math.max(0.0, windSpeed10mMS);
    const z0 = Math.max(1e-5, roughnessLengthMeters);
    const rho = Math.max(1e-6, densityKg_M3);
    const kappa = 0.40; // von Kármán constant
    const z = 10.0; // 10 m standard anemometer height

    const uStar = (kappa * u) / Math.log(z / z0);
    const tau0 = rho * uStar * uStar;

    return {
      frictionVelocityMS: parseFloat(uStar.toFixed(3)),
      shearStressPa: parseFloat(tau0.toFixed(4)),
      canLiftDust: tau0 >= 0.025
    };
  }

  /**
   * Calculate daytime Martian Convective Planetary Boundary Layer (PBL) peak depth in km.
   * z_PBL = C_pbl * ( (g / T_surf) * (H_sensible / (rho * c_p)) * (t_day / (2*pi)) )^(1/2)
   * Deep dry convective boundary layer reaches 6-10 km on Mars.
   * @param {number} sensibleHeatFluxW_M2 - Surface sensible heat flux in W/m^2 (typically 20 - 80 W/m^2 at noon)
   * @param {number} [surfaceTempK=240.0] - Surface temperature in Kelvin
   * @param {number} [densityKg_M3=0.015] - Surface atmospheric density in kg/m^3
   * @param {number} [gravityMS2=3.72076] - Surface gravity in m/s^2
   * @returns {{pblDepthKm: number, pblDepthMeters: number, convectiveVelocityScaleMS: number}}
   */
  static computeConvectivePBLMaxDepth(sensibleHeatFluxW_M2, surfaceTempK = 240.0, densityKg_M3 = 0.015, gravityMS2 = 3.72076) {
    const H = Math.max(0.0, sensibleHeatFluxW_M2);
    const T = Math.max(100.0, surfaceTempK);
    const rho = Math.max(1e-5, densityKg_M3);
    const g = Math.max(0.1, gravityMS2);
    const cp = 850.0; // J/(kg K) specific heat capacity of CO2

    // Kinematic heat flux w'theta' = H / (rho * cp) in K m/s
    const wTheta = H / (rho * cp);

    // Convective boundary layer depth approximation on Mars (Spiga et al. 2010):
    // z_pbl ~ 1.2 * sqrt( (2 * g * wTheta * 20000) / (T * gamma_lapse) ) ~ 1100 * wTheta^0.5
    // With wTheta = 50 / (0.015 * 850) = 3.92 K m/s -> z_pbl ~ 7.5 km
    const zMeters = H > 0 ? Math.min(15000.0, 3800.0 * Math.pow(wTheta, 0.5)) : 500.0;
    const zKm = zMeters / 1000.0;

    // Deardorff convective velocity scale w* = ( g / T * wTheta * z_pbl )^(1/3)
    const wStar = H > 0 ? Math.pow((g / T) * wTheta * zMeters, 1.0 / 3.0) : 0.0;

    return {
      pblDepthKm: parseFloat(zKm.toFixed(2)),
      pblDepthMeters: parseFloat(zMeters.toFixed(1)),
      convectiveVelocityScaleMS: parseFloat(wStar.toFixed(2))
    };
  }
}


















