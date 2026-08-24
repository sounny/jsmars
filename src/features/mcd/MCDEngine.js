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
}



