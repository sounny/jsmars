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

  // --- Radar Geophysical Equations ---

  /**
   * Calculate normal-incidence Fresnel power reflectivity at a dielectric interface.
   * @param {number} eps1 - Relative permittivity of top layer
   * @param {number} eps2 - Relative permittivity of bottom layer
   * @returns {{reflectivityLinear: number, reflectivityDb: number, transmissivityLinear: number}}
   */
  static computeFresnelReflectivity(eps1, eps2) {
    const n1 = Math.sqrt(Math.max(1, eps1));
    const n2 = Math.sqrt(Math.max(1, eps2));

    const rAmp = (n1 - n2) / (n1 + n2);
    const rPower = rAmp * rAmp;
    const rDb = 10 * Math.log10(Math.max(1e-12, rPower));
    const tPower = 1.0 - rPower;

    return {
      reflectivityLinear: rPower,
      reflectivityDb: rDb,
      transmissivityLinear: tPower
    };
  }

  /**
   * Calculate vertical range resolution in medium.
   * @param {number} [bandwidthHz=10e6] - Chirp bandwidth (e.g. 10 MHz for SHARAD)
   * @param {number} [epsR=3.15] - Dielectric permittivity of medium
   * @returns {number} Vertical range resolution in meters
   */
  static computeRangeResolution(bandwidthHz = 10e6, epsR = 3.15) {
    const v = RadarSounderEngine.getVelocity(epsR);
    return v / (2.0 * bandwidthHz);
  }

  /**
   * Calculate one-way radar power attenuation rate in dB per meter.
   * @param {number} [freqHz=20e6] - Radar center frequency (e.g. 20 MHz for SHARAD)
   * @param {number} [lossTangent=0.001] - Loss tangent (tan delta)
   * @param {number} [epsR=3.15] - Relative permittivity
   * @returns {number} Attenuation rate in dB/meter
   */
  static computeAttenuationRate(freqHz = 20e6, lossTangent = 0.001, epsR = 3.15) {
    const v = RadarSounderEngine.getVelocity(epsR);
    const omega = 2 * Math.PI * freqHz;
    return ((omega * lossTangent) / (2 * v)) * 8.686;
  }

  // --- Dielectric Permittivity Inversion & Radar Geophysics Solvers ---

  /**
   * Invert relative dielectric permittivity from known layer thickness and radar two-way travel time.
   * @param {number} layerThicknessMeters - True layer physical depth/thickness in meters (z)
   * @param {number} twtMicroseconds - Observed two-way travel time in microseconds (μs)
   * @returns {number} Inverted relative dielectric permittivity (epsilon_r)
   */
  static invertDielectricPermittivity(layerThicknessMeters, twtMicroseconds) {
    if (layerThicknessMeters <= 0 || twtMicroseconds <= 0) return 1.0;
    const twtSec = twtMicroseconds * 1e-6;
    // z = (c * twt) / (2 * sqrt(eps)) => sqrt(eps) = (c * twt) / (2 * z) => eps = ((c * twt) / (2 * z))^2
    const sqrtEps = (RadarSounderEngine.C * twtSec) / (2.0 * layerThicknessMeters);
    const eps = Math.pow(sqrtEps, 2);
    return parseFloat(eps.toFixed(3));
  }

  /**
   * Classify geological subsurface composition based on real dielectric permittivity (epsilon_r).
   * @param {number} epsR - Relative dielectric constant
   * @returns {{medium: string, description: string, typicalLossTan: number}}
   */
  static classifySubsurfaceMedium(epsR) {
    if (epsR < 2.5) {
      return {
        medium: 'Porous Regolith / Volcanic Ash / Pyroclastics',
        description: 'Low-density friable dust mantle or porous deposit (e.g. Medusae Fossae Formation)',
        typicalLossTan: 0.002
      };
    } else if (epsR <= 3.3) {
      return {
        medium: 'Pure Water & CO2 Ice / Polar Layered Deposits',
        description: 'Massive volatile ice sheet with low dust contamination (<5% lithic fraction)',
        typicalLossTan: 0.001
      };
    } else if (epsR <= 5.2) {
      return {
        medium: 'Dirty Ice / Permafrost / Cryolithosphere',
        description: 'Ice-dust mixture or cemented permafrost with 15-30% silicate volume fraction',
        typicalLossTan: 0.004
      };
    } else {
      return {
        medium: 'Dense Basaltic Bedrock / Solid Lava Flows',
        description: 'Consolidated volcanic basalt or anhydrous mafic crustal basement',
        typicalLossTan: 0.015
      };
    }
  }

  /**
   * Compute horizontal 1st Fresnel zone footprint radius for a subsurface radar echo.
   * @param {number} satelliteAltitudeKm - Spacecraft orbit altitude in km (e.g. 250 km for MRO)
   * @param {number} [centerFreqHz=20e6] - Center frequency in Hz (20 MHz for SHARAD => lambda = 15m)
   * @param {number} [targetDepthMeters=0] - Subsurface target depth in meters
   * @param {number} [epsR=3.15] - Dielectric permittivity of medium
   * @returns {number} First Fresnel zone radius in meters (RF)
   */
  static computeFresnelZoneRadius(satelliteAltitudeKm, centerFreqHz = 20e6, targetDepthMeters = 0, epsR = 3.15) {
    const lambda0 = RadarSounderEngine.C / centerFreqHz;
    const hMeters = Math.max(1000, satelliteAltitudeKm * 1000);
    const zApparent = targetDepthMeters / Math.sqrt(Math.max(1, epsR));
    const totalDist = hMeters + zApparent;

    const rFresnel = Math.sqrt(lambda0 * totalDist / 2.0);
    return parseFloat(rFresnel.toFixed(1));
  }

  // --- Penetration Depth & Uncertainty Solvers ---

  /**
   * Calculate maximum radar signal penetration depth given instrument dynamic range and dielectric loss.
   * @param {number} [dynamicRangeDb=60] - Instrument dynamic range (dB)
   * @param {number} [freqHz=20e6] - Radar center frequency (Hz)
   * @param {number} [lossTangent=0.001] - Subsurface medium loss tangent
   * @param {number} [epsR=3.15] - Dielectric permittivity
   * @returns {number} Maximum penetration depth in meters
   */
  static computeSignalPenetrationDepth(dynamicRangeDb = 60, freqHz = 20e6, lossTangent = 0.001, epsR = 3.15) {
    const alpha = this.computeAttenuationRate(freqHz, lossTangent, epsR);
    if (alpha <= 0) return Infinity;
    // Two-way attenuation = 2 * alpha * z <= dynamicRangeDb => z <= dynamicRangeDb / (2 * alpha)
    const maxDepth = dynamicRangeDb / (2.0 * alpha);
    return parseFloat(maxDepth.toFixed(1));
  }

  /**
   * Propagate statistical uncertainty in radar depth estimation from uncertainties in TWT and permittivity.
   * @param {number} twtMicroseconds - Two-way travel time (μs)
   * @param {number} [epsR=3.15] - Relative dielectric permittivity
   * @param {number} [sigmaTwtMicroseconds=0.05] - Standard error in TWT (μs)
   * @param {number} [sigmaEps=0.2] - Standard error in permittivity
   * @returns {{nominalDepthMeters: number, sigmaDepthMeters: number, relativeUncertaintyPercent: number}}
   */
  static computeDepthUncertainty(twtMicroseconds, epsR = 3.15, sigmaTwtMicroseconds = 0.05, sigmaEps = 0.2) {
    const zNominal = this.twtToDepth(twtMicroseconds, epsR);
    const twtSec = twtMicroseconds * 1e-6;
    const sigmaTwtSec = sigmaTwtMicroseconds * 1e-6;

    // Partial derivatives:
    // dz/dt = c / (2 * sqrt(eps))
    // dz/deps = - c * t / (4 * eps^(3/2)) = - z / (2 * eps)
    const dz_dt = RadarSounderEngine.C / (2.0 * Math.sqrt(epsR));
    const dz_deps = - zNominal / (2.0 * epsR);

    const varZ = Math.pow(dz_dt * sigmaTwtSec, 2) + Math.pow(dz_deps * sigmaEps, 2);
    const sigmaZ = Math.sqrt(varZ);

    return {
      nominalDepthMeters: parseFloat(zNominal.toFixed(2)),
      sigmaDepthMeters: parseFloat(sigmaZ.toFixed(2)),
      relativeUncertaintyPercent: parseFloat(((sigmaZ / Math.max(1, zNominal)) * 100).toFixed(2))
    };
  }

  // --- Surface Clutter, CRIM Porosity Inversion & Basal Contacts ---

  /**
   * Calculate off-nadir topographic surface clutter round-trip travel time.
   * tau_clutter = (2 * sqrt(H^2 + d^2)) / c
   * @param {number} orbitAltitudeKm - Spacecraft orbit altitude in km (e.g. 250 km for MRO)
   * @param {number} crossTrackDistanceKm - Perpendicular distance to off-nadir topographic feature (e.g. crater rim)
   * @returns {{nadirTwtMicrosec: number, clutterTwtMicrosec: number, excessDelayMicrosec: number, apparentDepthMetersInIce: number}}
   */
  static computeSurfaceClutterDelay(orbitAltitudeKm, crossTrackDistanceKm) {
    const hM = orbitAltitudeKm * 1000.0;
    const dM = crossTrackDistanceKm * 1000.0;

    const slantRange = Math.hypot(hM, dM);
    const nadirTwtSec = (2.0 * hM) / RadarSounderEngine.C;
    const clutterTwtSec = (2.0 * slantRange) / RadarSounderEngine.C;
    const excessDelaySec = clutterTwtSec - nadirTwtSec;

    // Apparent false subsurface depth if interpreted as an in-nadir subsurface ice reflector (eps = 3.15)
    const vIce = RadarSounderEngine.getVelocity(3.15);
    const apparentDepthM = (vIce * excessDelaySec) / 2.0;

    return {
      nadirTwtMicrosec: parseFloat((nadirTwtSec * 1e6).toFixed(3)),
      clutterTwtMicrosec: parseFloat((clutterTwtSec * 1e6).toFixed(3)),
      excessDelayMicrosec: parseFloat((excessDelaySec * 1e6).toFixed(3)),
      apparentDepthMetersInIce: parseFloat(apparentDepthM.toFixed(1))
    };
  }

  /**
   * Estimate volumetric regolith porosity using the Complex Refractive Index Model (CRIM).
   * sqrt(eps_bulk) = (1 - phi) * sqrt(eps_matrix) + phi * sqrt(eps_pore)
   * @param {number} bulkEps - Measured bulk relative permittivity (e.g. 2.9 for Medusae Fossae)
   * @param {number} [matrixEps=7.5] - Solid rock grain permittivity (basalt ~ 7.5)
   * @param {number} [poreEps=1.0] - Pore filler permittivity (vacuum/gas = 1.0, water ice = 3.15)
   * @returns {{porosityFraction: number, porosityPercent: number}}
   */
  static estimatePorosityFromPermittivity(bulkEps, matrixEps = 7.5, poreEps = 1.0) {
    const sqrtBulk = Math.sqrt(Math.max(1, bulkEps));
    const sqrtMatrix = Math.sqrt(Math.max(1, matrixEps));
    const sqrtPore = Math.sqrt(Math.max(1, poreEps));

    const denom = sqrtPore - sqrtMatrix;
    if (Math.abs(denom) < 1e-4) return { porosityFraction: 0, porosityPercent: 0 };

    const phi = (sqrtBulk - sqrtMatrix) / denom;
    const clampedPhi = Math.max(0, Math.min(1.0, phi));

    return {
      porosityFraction: parseFloat(clampedPhi.toFixed(4)),
      porosityPercent: parseFloat((clampedPhi * 100.0).toFixed(2))
    };
  }

  /**
   * Calculate normal-incidence Fresnel reflectivity at sub-ice basal contacts.
   * @param {number} [epsIce=3.15] - Dielectric permittivity of overlying ice
   * @param {number} [epsBasement=7.5] - Dielectric permittivity of underlying basement
   * @returns {{reflectivityPower: number, reflectivityDb: number, contactType: string}}
   */
  static computeBasalInterfaceReflectivity(epsIce = 3.15, epsBasement = 7.5) {
    const fresnel = this.computeFresnelReflectivity(epsIce, epsBasement);
    let contactType = 'Basaltic Bedrock';

    if (epsBasement > 40) {
      contactType = 'Subglacial Liquid Water / Brine Body';
    } else if (epsBasement > 12) {
      contactType = 'Hydrated Smectite Clay / Saline Permafrost';
    } else if (epsBasement >= 6) {
      contactType = 'Dry Basaltic Basement Floor';
    } else {
      contactType = 'Basal Sediment / Porous Ash';
    }

    return {
      reflectivityPower: parseFloat(fresnel.reflectivityLinear.toFixed(4)),
      reflectivityDb: parseFloat(fresnel.reflectivityDb.toFixed(2)),
      contactType
    };
  }
}




