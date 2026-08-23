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
}
