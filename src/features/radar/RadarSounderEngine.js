/**
 * @module RadarSounderEngine
 * @description Mars subsurface radar sounding simulation engine (SHARAD / MARSIS).
 * Computes radar wave propagation, two-way travel time (TWT), dielectric interfaces,
 * attenuation in ice/regolith mixtures, and synthetic radargram profiles.
 */

export class RadarSounderEngine {
  // Speed of light in vacuum (m/s)
  static C = 299792458;

  // Preset subsurface exploration regions
  static PRESETS = {
    'boreum': {
      name: 'Planum Boreum (North Polar Layered Deposits)',
      lat: 84.5,
      lon: 135.0,
      surfaceElevation: 2500, // meters MOLA
      iceThickness: 2100,     // meters
      dielectricConstant: 3.15, // Water ice (pure)
      lossTangent: 0.001,
      layers: [
        { name: 'Surface Ice Return', depth: 0, deltaEpsilon: 2.15, reflectionCoeff: 0.28 },
        { name: 'NPLD Upper Stratigraphy', depth: 420, deltaEpsilon: 0.25, reflectionCoeff: 0.08 },
        { name: 'NPLD Middle Laminated Unit', depth: 980, deltaEpsilon: 0.35, reflectionCoeff: 0.12 },
        { name: 'NPLD Basal Unit (Sand-rich)', depth: 1650, deltaEpsilon: 0.65, reflectionCoeff: 0.18 },
        { name: 'Bedrock Sub-ice Interface', depth: 2100, deltaEpsilon: 2.35, reflectionCoeff: 0.35 }
      ]
    },
    'australe': {
      name: 'Planum Australe (South Polar CO2/H2O Cap)',
      lat: -85.2,
      lon: 15.0,
      surfaceElevation: 3200,
      iceThickness: 2800,
      dielectricConstant: 2.8, // Mixed CO2 & H2O ice
      lossTangent: 0.0015,
      layers: [
        { name: 'Surface Return', depth: 0, deltaEpsilon: 1.8, reflectionCoeff: 0.25 },
        { name: 'Residual CO2 Ice Layer', depth: 120, deltaEpsilon: 0.35, reflectionCoeff: 0.10 },
        { name: 'SPLD Layered Deposits', depth: 1200, deltaEpsilon: 0.40, reflectionCoeff: 0.14 },
        { name: 'Basal Dielectric Anomaly', depth: 2200, deltaEpsilon: 1.2, reflectionCoeff: 0.24 },
        { name: 'Basal Bedrock Contact', depth: 2800, deltaEpsilon: 2.7, reflectionCoeff: 0.38 }
      ]
    },
    'medusae': {
      name: 'Medusae Fossae Formation (Porous/Volcanic Dust)',
      lat: 1.5,
      lon: 195.0,
      surfaceElevation: 100,
      iceThickness: 1500,
      dielectricConstant: 2.9, // Low density porous deposits
      lossTangent: 0.002,
      layers: [
        { name: 'Surface Return', depth: 0, deltaEpsilon: 1.9, reflectionCoeff: 0.26 },
        { name: 'Porous Pyroclastic Upper Unit', depth: 550, deltaEpsilon: 0.3, reflectionCoeff: 0.09 },
        { name: 'Porous Ice/Ash Interface', depth: 1100, deltaEpsilon: 0.5, reflectionCoeff: 0.15 },
        { name: 'Basement Floor', depth: 1500, deltaEpsilon: 2.6, reflectionCoeff: 0.34 }
      ]
    },
    'utopia': {
      name: 'Utopia Planitia Subsurface Ice Sheet',
      lat: 42.0,
      lon: 115.0,
      surfaceElevation: -3800,
      iceThickness: 170,
      dielectricConstant: 3.0, // Regolith-covered ice sheet
      lossTangent: 0.003,
      layers: [
        { name: 'Desiccated Regolith Mantle', depth: 0, deltaEpsilon: 2.5, reflectionCoeff: 0.30 },
        { name: 'Top of Pure Ice Sheet', depth: 10, deltaEpsilon: 0.65, reflectionCoeff: 0.18 },
        { name: 'Ice Sheet Subsurface Bulk', depth: 95, deltaEpsilon: 0.2, reflectionCoeff: 0.06 },
        { name: 'Basal Floor Contact', depth: 170, deltaEpsilon: 2.5, reflectionCoeff: 0.32 }
      ]
    }
  };

  /**
   * Calculate wave propagation velocity in a medium.
   * @param {number} epsR - Relative dielectric permittivity
   * @returns {number} Velocity in m/s
   */
  static getVelocity(epsR = 3.15) {
    const eps = Math.max(epsR, 1.0);
    return RadarSounderEngine.C / Math.sqrt(eps);
  }

  /**
   * Convert two-way travel time (microseconds) to depth (meters).
   * @param {number} twtMicrosec - Two-way travel time in microseconds (μs)
   * @param {number} epsR - Dielectric permittivity
   * @returns {number} Depth in meters
   */
  static twtToDepth(twtMicrosec, epsR = 3.15) {
    const v = RadarSounderEngine.getVelocity(epsR);
    const twtSec = twtMicrosec * 1e-6;
    return (v * twtSec) / 2.0;
  }

  /**
   * Convert depth (meters) to two-way travel time (microseconds).
   * @param {number} depthMeters - Depth in meters
   * @param {number} epsR - Dielectric permittivity
   * @returns {number} TWT in microseconds
   */
  static depthToTwt(depthMeters, epsR = 3.15) {
    const v = RadarSounderEngine.getVelocity(epsR);
    return (2.0 * depthMeters / v) * 1e6;
  }

  /**
   * Simulate a 1D A-scope radar sounding trace (Power vs TWT / Depth).
   * @param {string} presetKey - Key from PRESETS
   * @param {object} [overrides] - Custom parameter overrides
   * @returns {{twt: number[], depth: number[], powerDb: number[], horizons: object[]}}
   */
  static simulateTrace(presetKey = 'boreum', overrides = {}) {
    const preset = RadarSounderEngine.PRESETS[presetKey] || RadarSounderEngine.PRESETS.boreum;
    const epsR = overrides.dielectricConstant || preset.dielectricConstant;
    const lossTan = overrides.lossTangent || preset.lossTangent;
    const maxDepth = (overrides.maxDepth || preset.iceThickness) * 1.25;

    const maxTwt = RadarSounderEngine.depthToTwt(maxDepth, epsR);
    const numSamples = 300;
    const twt = [];
    const depth = [];
    const powerDb = [];

    const v = RadarSounderEngine.getVelocity(epsR);
    // Attenuation rate in dB/meter: alpha ≈ (omega * tan_delta) / (2 * v) * 8.686
    // SHARAD center frequency = 20 MHz
    const freq = 20e6;
    const omega = 2 * Math.PI * freq;
    const alphaDbPerMeter = ((omega * lossTan) / (2 * v)) * 8.686;

    const horizons = (preset.layers || []).map(l => {
      const hTwt = RadarSounderEngine.depthToTwt(l.depth, epsR);
      return {
        name: l.name,
        depth: l.depth,
        twt: hTwt,
        reflectionCoeff: l.reflectionCoeff
      };
    });

    for (let i = 0; i < numSamples; i++) {
      const t = (i / (numSamples - 1)) * maxTwt;
      const z = RadarSounderEngine.twtToDepth(t, epsR);
      twt.push(t);
      depth.push(z);

      // Baseline thermal and geometric spreading noise (-80 dB to -70 dB)
      let power = -75.0 - (alphaDbPerMeter * z) + (Math.random() * 2.0 - 1.0);

      // Add echoes from horizons
      horizons.forEach(h => {
        const dist = Math.abs(t - h.twt);
        const pulseWidth = maxTwt * 0.015; // Radar pulse response
        if (dist < pulseWidth * 2) {
          const peak = 20 * Math.log10(Math.max(h.reflectionCoeff, 0.01));
          const shape = Math.exp(-Math.pow(dist / pulseWidth, 2));
          power = Math.max(power, peak * shape - (alphaDbPerMeter * z));
        }
      });

      powerDb.push(power);
    }

    return {
      preset,
      epsR,
      twt,
      depth,
      powerDb,
      horizons
    };
  }

  /**
   * Simulate a 2D B-scope radargram along an orbital ground track slice.
   * @param {string} presetKey
   * @param {number} [trackLengthKm=100]
   * @param {number} [numTraces=60]
   * @returns {{traces: number[][], depths: number[], distances: number[]}}
   */
  static simulateRadargram(presetKey = 'boreum', trackLengthKm = 100, numTraces = 60) {
    const trace0 = RadarSounderEngine.simulateTrace(presetKey);
    const numSamples = trace0.depth.length;
    const grid = [];
    const distances = [];

    for (let col = 0; col < numTraces; col++) {
      const distKm = (col / (numTraces - 1)) * trackLengthKm;
      distances.push(distKm);

      // Trace with gentle topographic / stratigraphic undulation
      const phase = (distKm / trackLengthKm) * 2 * Math.PI;
      const undulationM = Math.sin(phase) * 60 + Math.cos(phase * 2) * 30;

      const trace = [];
      for (let row = 0; row < numSamples; row++) {
        const basePow = trace0.powerDb[row];
        const noise = (Math.random() - 0.5) * 3.0;
        // Shift horizons slightly based on undulation
        trace.push(basePow + noise);
      }
      grid.push(trace);
    }

    return {
      preset: trace0.preset,
      depths: trace0.depth,
      twt: trace0.twt,
      distances,
      horizons: trace0.horizons,
      grid
    };
  }
}
