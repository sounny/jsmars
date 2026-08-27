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

  // --- Synthetic Aperture SAR, Ionospheric Dispersion & Multi-Layer TWT Solvers ---

  /**
   * Calculate along-track synthetic aperture radar (SAR) azimuth resolution.
   * Delta_x = v_orbit / (2 * B_Doppler)
   * @param {number} [orbitVelocityMs=3400] - Spacecraft orbital ground track speed in m/s (MRO ~ 3.4 km/s)
   * @param {number} [dopplerBandwidthHz=200] - Synthetic aperture processed Doppler bandwidth in Hz
   * @returns {number} Along-track SAR spatial resolution in meters
   */
  static computeSARResolution(orbitVelocityMs = 3400, dopplerBandwidthHz = 200) {
    const resMeters = orbitVelocityMs / (2.0 * Math.max(1, dopplerBandwidthHz));
    return parseFloat(resMeters.toFixed(2));
  }

  /**
   * Calculate Martian ionospheric pulse dispersion group delay.
   * Delta_tau = (40.3 * TEC) / (c * f^2)
   * @param {number} [totalElectronContentTecU=1.0] - Total Electron Content (1 TECU = 1e16 electrons/m^2)
   * @param {number} [freqHz=20e6] - Center frequency in Hz (20 MHz for SHARAD, 4 MHz for MARSIS)
   * @returns {{delayMicrosec: number, apparentHeightShiftMeters: number}}
   */
  static computeIonosphericDispersionDelay(paramA = 1.0, paramB = 20e6) {
    let tecu = 1.0;
    let f = 20e6;

    if (paramA > 1e4 && paramB <= 1e4) {
      f = paramA;
      tecu = paramB;
    } else {
      tecu = paramA;
      f = paramB;
    }

    const tecM2 = tecu * 1e16;
    const f2 = Math.max(1e3, f) * Math.max(1e3, f);
    const delaySec = (40.3 * tecM2) / (RadarSounderEngine.C * f2);
    const delayMicrosec = delaySec * 1e6;
    const delayNanosec = delaySec * 1e9;
    const shiftMeters = (RadarSounderEngine.C * delaySec) / 2.0;

    return {
      delayMicrosec: parseFloat(delayMicrosec.toFixed(4)),
      apparentHeightShiftMeters: parseFloat(shiftMeters.toFixed(2)),
      ionosphericDelaySeconds: parseFloat(delaySec.toExponential(4)),
      ionosphericDelayNanoseconds: parseFloat(delayNanosec.toFixed(3)),
      rangeErrorMeters: parseFloat(shiftMeters.toFixed(2))
    };
  }

  /**
   * Compute cumulative two-way travel time (TWT) and depth across a multi-layer stratigraphy.
   * @param {Array<{thicknessMeters: number, dielectricConstant: number}>} layers - Array of layer strata
   * @returns {{totalDepthMeters: number, totalTwtMicrosec: number, layerIntervals: Array<object>}}
   */
  static computeMultiLayerTWT(layers = []) {
    let totalZ = 0;
    let totalTwt = 0;
    const intervals = [];

    layers.forEach((l, i) => {
      const z = l.thicknessMeters || 0;
      const eps = l.dielectricConstant || 3.15;
      const twt = this.depthToTwt(z, eps);

      totalZ += z;
      totalTwt += twt;

      intervals.push({
        layerIndex: i + 1,
        thicknessMeters: z,
        dielectricConstant: eps,
        intervalTwtMicrosec: parseFloat(twt.toFixed(3)),
        cumulativeDepthMeters: parseFloat(totalZ.toFixed(1)),
        cumulativeTwtMicrosec: parseFloat(totalTwt.toFixed(3))
      });
    });

    return {
      totalDepthMeters: parseFloat(totalZ.toFixed(1)),
      totalTwtMicrosec: parseFloat(totalTwt.toFixed(3)),
      layerIntervals: intervals
    };
  }

  // --- Surface-to-Basal Power Ratio, EM Skin Depth & Doppler Solvers ---

  /**
   * Calculate surface-to-basal radar power ratio in dB.
   * P_ratio = 10*log10(R_surf / (R_basal * T_surf^2)) + 2 * alpha * z
   * @param {number} [epsSurface=3.15] - Dielectric constant of surface medium (ice)
   * @param {number} [epsBasal=7.5] - Dielectric constant of basal substrate (bedrock)
   * @param {number} [iceThicknessMeters=1000] - Ice sheet thickness
   * @param {number} [lossTangent=0.001] - Ice dielectric loss tangent
   * @param {number} [freqHz=20e6] - Radar frequency
   * @returns {{powerRatioDb: number, attenuationLossDb: number, basalReflectivityDb: number}}
   */
  static computeSurfaceBasalPowerRatio(epsSurface = 3.15, epsBasal = 7.5, iceThicknessMeters = 1000, lossTangent = 0.001, freqHz = 20e6) {
    const rSurf = this.computeFresnelReflectivity(1.0, epsSurface);
    const rBasal = this.computeFresnelReflectivity(epsSurface, epsBasal);
    const alpha = this.computeAttenuationRate(freqHz, lossTangent, epsSurface);

    const twoWayLossDb = 2.0 * alpha * Math.max(0, iceThicknessMeters);
    const tSurfPower = rSurf.transmissivityLinear;
    const geometricFactor = rSurf.reflectivityLinear / (rBasal.reflectivityLinear * tSurfPower * tSurfPower);
    const reflectionDbDiff = 10.0 * Math.log10(Math.max(1e-6, geometricFactor));

    const totalRatioDb = reflectionDbDiff + twoWayLossDb;

    return {
      powerRatioDb: parseFloat(totalRatioDb.toFixed(2)),
      attenuationLossDb: parseFloat(twoWayLossDb.toFixed(2)),
      basalReflectivityDb: parseFloat(rBasal.reflectivityDb.toFixed(2))
    };
  }

  /**
   * Calculate electromagnetic skin depth (1/e amplitude penetration depth) in lossy medium.
   * delta_EM = 1 / alpha_Np
   * @param {number} [freqHz=20e6] - Center frequency in Hz
   * @param {number} [lossTangent=0.001] - Loss tangent tan(delta)
   * @param {number} [epsR=3.15] - Relative dielectric permittivity
   * @returns {number} EM skin depth in meters
   */
  static computeSkinDepthEM(freqHz = 20e6, lossTangent = 0.001, epsR = 3.15) {
    const alphaDb = this.computeAttenuationRate(freqHz, lossTangent, epsR);
    if (alphaDb <= 0) return Infinity;

    // Convert dB/m to Nepers/m (1 Np = 8.686 dB)
    const alphaNp = alphaDb / 8.686;
    const skinDepth = 1.0 / alphaNp;

    return parseFloat(skinDepth.toFixed(1));
  }

  /**
   * Calculate radar carrier Doppler frequency shift.
   * Delta_f = (2 * v_r * f0) / c
   * @param {number} relativeVelocityMs - Relative velocity along line of sight in m/s
   * @param {number} [centerFreqHz=20e6] - Center frequency (20 MHz for SHARAD)
   * @returns {number} Doppler frequency shift in Hz
   */
  static computeDopplerShift(relativeVelocityMs, centerFreqHz = 20e6) {
    const shift = (2.0 * relativeVelocityMs * centerFreqHz) / RadarSounderEngine.C;
    return parseFloat(shift.toFixed(2));
  }

  // --- Temperature-Dependent Permittivity, Looyenga & Birchak Dielectric Mixing Solvers ---

  /**
   * Calculate temperature-dependent relative dielectric permittivity for pure water ice on Mars.
   * eps_r(T) = 3.15 * (1 + 0.0003 * (T - 200))
   * @param {number} tempK - Ice temperature in Kelvin (e.g. 150K to 240K)
   * @returns {number} Temperature-corrected real permittivity
   */
  static computeWaterIceTemperaturePermittivity(tempK) {
    const T = Math.max(50, Math.min(273.15, tempK));
    const eps = 3.15 * (1.0 + 0.0003 * (T - 200.0));
    return parseFloat(eps.toFixed(4));
  }

  /**
   * Calculate bulk effective dielectric permittivity for ice-dust mixtures using Looyenga (1/3 power) model.
   * eps_mix^(1/3) = (1 - phi_d) * eps_ice^(1/3) + phi_d * eps_dust^(1/3)
   * @param {number} volFractionDust - Volumetric fraction of silicate dust (0.0 to 1.0)
   * @param {number} [epsIce=3.15] - Dielectric permittivity of pure water ice
   * @param {number} [epsDust=7.5] - Dielectric permittivity of silicate dust/basalt
   * @returns {{effectivePermittivity: number, refractiveIndex: number}}
   */
  static computeLooyengaDielectricMixing(volFractionDust, epsIce = 3.15, epsDust = 7.5) {
    const phi = Math.max(0, Math.min(1.0, volFractionDust));
    const termIce = (1.0 - phi) * Math.cbrt(Math.max(1, epsIce));
    const termDust = phi * Math.cbrt(Math.max(1, epsDust));

    const epsCbrt = termIce + termDust;
    const epsEff = Math.pow(epsCbrt, 3);

    return {
      effectivePermittivity: parseFloat(epsEff.toFixed(3)),
      refractiveIndex: parseFloat(Math.sqrt(epsEff).toFixed(3))
    };
  }

  /**
   * Calculate bulk effective dielectric permittivity using Birchak / CRIM (1/2 power) model.
   * eps_mix^(1/2) = (1 - phi_d) * eps_ice^(1/2) + phi_d * eps_dust^(1/2)
   * @param {number} volFractionDust - Volumetric fraction of dust (0.0 to 1.0)
   * @param {number} [epsIce=3.15] - Dielectric constant of ice
   * @param {number} [epsDust=7.5] - Dielectric constant of dust
   * @returns {{effectivePermittivity: number, waveVelocityMs: number}}
   */
  static computeBirchakDielectricMixing(volFractionDust, epsIce = 3.15, epsDust = 7.5) {
    const phi = Math.max(0, Math.min(1.0, volFractionDust));
    const sqrtIce = (1.0 - phi) * Math.sqrt(Math.max(1, epsIce));
    const sqrtDust = phi * Math.sqrt(Math.max(1, epsDust));

    const sqrtEff = sqrtIce + sqrtDust;
    const epsEff = Math.pow(sqrtEff, 2);
    const v = RadarSounderEngine.C / sqrtEff;

    return {
      effectivePermittivity: parseFloat(epsEff.toFixed(3)),
      waveVelocityMs: parseFloat(v.toFixed(0))
    };
  }

  // --- Thin-Film Resonance Fringes, Basal Transmission & Radar Equation Solvers ---

  /**
   * Calculate thin-film quarter-wave constructive/destructive radar interference fringe layer thickness.
   * Delta_z = lambda_medium / 4 = c / (4 * f0 * sqrt(eps_r))
   * @param {number} [centerFreqHz=20e6] - Center frequency (20 MHz for SHARAD)
   * @param {number} [epsR=3.15] - Dielectric permittivity of layer
   * @returns {{quarterWaveFringeMeters: number, halfWaveFringeMeters: number}}
   */
  static computeInterferenceFringeSpacing(centerFreqHz = 20e6, epsR = 3.15) {
    const v = this.getVelocity(epsR);
    const lambdaM = v / Math.max(1e3, centerFreqHz);

    const quarterWaveM = lambdaM / 4.0;
    const halfWaveM = lambdaM / 2.0;

    return {
      quarterWaveFringeMeters: parseFloat(quarterWaveM.toFixed(2)),
      halfWaveFringeMeters: parseFloat(halfWaveM.toFixed(2))
    };
  }

  /**
   * Calculate two-way radar power transmission efficiency across a dielectric boundary.
   * T_two_way = (1 - R)^2
   * @param {number} eps1 - Relative permittivity of upper medium
   * @param {number} eps2 - Relative permittivity of lower medium
   * @returns {{oneWayTransmissivity: number, twoWayTransmissionLossDb: number}}
   */
  static computeBasalDielectricContrastLoss(eps1, eps2) {
    const fresnel = this.computeFresnelReflectivity(eps1, eps2);
    const tOneWay = fresnel.transmissivityLinear;
    const tTwoWay = tOneWay * tOneWay;
    const lossDb = 10.0 * Math.log10(Math.max(1e-9, tTwoWay));

    return {
      oneWayTransmissivity: parseFloat(tOneWay.toFixed(4)),
      twoWayTransmissionLossDb: parseFloat(lossDb.toFixed(3))
    };
  }

  /**
   * Calculate received radar echo power using the planetary radar equation.
   * P_rx = (P_tx * G^2 * lambda^2 * sigma) / ((4 * pi)^3 * R^4)
   * @param {number} [ptWatts=10.0] - Transmitter peak power in Watts (10W for SHARAD)
   * @param {number} [gainDbi=0.0] - Antenna isotropic gain in dBi (0 dBi for dipole)
   * @param {number} [freqHz=20e6] - Radar frequency (20 MHz)
   * @param {number} [altitudeKm=250.0] - Spacecraft orbit altitude in km
   * @param {number} [sigmaTargetM2=100.0] - Radar backscatter cross-section in m^2
   * @returns {{receivedPowerWatts: number, receivedPowerDbm: number}}
   */
  static computeRadarEquationReceivedPower(ptWatts = 10.0, gainDbi = 0.0, freqHz = 20e6, altitudeKm = 250.0, sigmaTargetM2 = 100.0) {
    const lambda = RadarSounderEngine.C / Math.max(1e3, freqHz);
    const GLinear = Math.pow(10, gainDbi / 10.0);
    const RMeters = Math.max(1000, altitudeKm * 1000.0);

    const numerator = ptWatts * GLinear * GLinear * lambda * lambda * sigmaTargetM2;
    const denominator = Math.pow(4.0 * Math.PI, 3) * Math.pow(RMeters, 4);

    const pRxWatts = numerator / denominator;
    // P_dBm = 10 * log10(P_watts / 1e-3) = 10 * log10(P_watts) + 30
    const pRxDbm = 10.0 * Math.log10(Math.max(1e-25, pRxWatts)) + 30.0;

    return {
      receivedPowerWatts: parseFloat(pRxWatts.toExponential(4)),
      receivedPowerDbm: parseFloat(pRxDbm.toFixed(2))
    };
  }

  // --- Subsurface Range Resolution, SAR Doppler Sharpening & Basal Attenuation Inversion Solvers ---

  /**
   * Calculate vertical subsurface range resolution after matched-filter chirp dechirping/compression.
   * Delta_r = c / (2 * B * sqrt(eps_r))
   * @param {number} [chirpBandwidthHz=10e6] - Chirp bandwidth (10 MHz for SHARAD, 1 MHz for MARSIS)
   * @param {number} [dielectricPermittivity=3.15] - Subsurface relative permittivity
   * @returns {{rangeResolutionMeters: number, rangeResolutionAirMeters: number}}
   */
  static computeSubsurfaceRangeResolution(chirpBandwidthHz = 10e6, dielectricPermittivity = 3.15) {
    const B = Math.max(1e4, chirpBandwidthHz);
    const eps = Math.max(1.0, dielectricPermittivity);

    const deltaAir = RadarSounderEngine.C / (2.0 * B);
    const deltaMedium = deltaAir / Math.sqrt(eps);

    return {
      rangeResolutionMeters: parseFloat(deltaMedium.toFixed(2)),
      rangeResolutionAirMeters: parseFloat(deltaAir.toFixed(2))
    };
  }

  /**
   * Calculate along-track Doppler SAR synthetic aperture focusing sharpening factor.
   * L_Doppler = (lambda * H) / (2 * L_synth)
   * @param {number} [orbitAltitudeKm=250.0] - Spacecraft orbit altitude in km
   * @param {number} [centerFreqHz=20e6] - Center frequency in Hz (20 MHz for SHARAD)
   * @param {number} [syntheticApertureLengthM=5000.0] - Synthetic aperture integration length in meters
   * @returns {{dopplerFootprintMeters: number, unfocusedFresnelDiameterMeters: number, sharpeningFactor: number}}
   */
  static computeDopplerFresnelSharpening(orbitAltitudeKm = 250.0, centerFreqHz = 20e6, syntheticApertureLengthM = 5000.0) {
    const lambda = RadarSounderEngine.C / Math.max(1e3, centerFreqHz);
    const H = Math.max(1000, orbitAltitudeKm * 1000.0);
    const LSynth = Math.max(100, syntheticApertureLengthM);

    const lDoppler = (lambda * H) / (2.0 * LSynth);
    const fresnelRadius = Math.sqrt(lambda * H / 2.0);
    const unfocusedDiameter = fresnelRadius * 2.0;
    const sharpening = unfocusedDiameter / Math.max(1.0, lDoppler);

    return {
      dopplerFootprintMeters: parseFloat(lDoppler.toFixed(1)),
      unfocusedFresnelDiameterMeters: parseFloat(unfocusedDiameter.toFixed(1)),
      sharpeningFactor: parseFloat(sharpening.toFixed(2))
    };
  }

  /**
   * Invert bulk two-way volumetric radar attenuation rate from surface and basal echo powers.
   * alpha_2way = (P_surf_dB - P_basal_dB - 2*Loss_interface_dB) / (2 * z_km)
   * @param {number} surfPowerDb - Surface reflection echo power in dB
   * @param {number} basalPowerDb - Basal interface reflection echo power in dB
   * @param {number} iceThicknessMeters - Physical thickness of ice sheet in meters
   * @param {number} [interfaceLossDb=1.0] - Transmission loss at surface interface in dB
   * @returns {{twoWayAttenuationDbPerKm: number, oneWayAttenuationDbPerM: number, lossTangentEstimate: number}}
   */
  static invertTwoWayAttenuationFromReflectivity(surfPowerDb, basalPowerDb, iceThicknessMeters, interfaceLossDb = 1.0) {
    const zKm = Math.max(0.01, iceThicknessMeters / 1000.0);
    const pDiff = surfPowerDb - basalPowerDb;
    const netLossDb = Math.max(0, pDiff - 2.0 * interfaceLossDb);

    const twoWayAlphaDbKm = netLossDb / (2.0 * zKm);
    const oneWayAlphaDbM = twoWayAlphaDbKm / 2000.0;

    // Estimate loss tangent: alpha_dB/m ≈ (omega * tan_delta) / (2 * v) * 8.686
    const vIce = RadarSounderEngine.getVelocity(3.15);
    const omega = 2.0 * Math.PI * 20e6;
    const tanDelta = (oneWayAlphaDbM * 2.0 * vIce) / (omega * 8.686);

    return {
      twoWayAttenuationDbPerKm: parseFloat(twoWayAlphaDbKm.toFixed(2)),
      oneWayAttenuationDbPerM: parseFloat(oneWayAlphaDbM.toFixed(4)),
      lossTangentEstimate: parseFloat(tanDelta.toFixed(5))
    };
  }

  // --- Free-Space Path Loss, Refraction Angle & Clutter-to-Signal Ratio Solvers ---

  /**
   * Calculate radar geometric free-space spherical spreading path loss in dB.
   * L_fs = (4 * pi * R / lambda)^2
   * @param {number} rangeKm - Two-way or one-way range distance in km (e.g. 250 km for MRO)
   * @param {number} [freqHz=20e6] - Center frequency in Hz (20 MHz for SHARAD => lambda = 15m)
   * @returns {{pathLossDb: number, wavelengthMeters: number}}
   */
  static computeFreeSpacePathLoss(rangeKm, freqHz = 20e6) {
    const lambda = RadarSounderEngine.C / Math.max(1e3, freqHz);
    const rMeters = Math.max(100, rangeKm * 1000.0);

    const lossRatio = (4.0 * Math.PI * rMeters) / lambda;
    const lossDb = 20.0 * Math.log10(Math.max(1.0, lossRatio));

    return {
      pathLossDb: parseFloat(lossDb.toFixed(2)),
      wavelengthMeters: parseFloat(lambda.toFixed(3))
    };
  }

  /**
   * Calculate subsurface electromagnetic wave refraction angle using Snell's Law.
   * sin(theta_2) = sin(theta_1) / sqrt(eps_r)
   * @param {number} incidenceAngleDeg - Surface incidence angle theta_1 in degrees
   * @param {number} [dielectricPermittivity=3.15] - Subsurface relative permittivity
   * @returns {{refractionAngleDeg: number, criticalAngleDeg: number, isTotalInternalReflection: boolean}}
   */
  static computeSubsurfaceRefractionAngle(incidenceAngleDeg, dielectricPermittivity = 3.15) {
    const eps = Math.max(1.0, dielectricPermittivity);
    const n = Math.sqrt(eps);
    const theta1Rad = Math.abs(incidenceAngleDeg) * Math.PI / 180.0;

    const sinTheta2 = Math.sin(theta1Rad) / n;
    const theta2Rad = Math.asin(Math.max(-1.0, Math.min(1.0, sinTheta2)));
    const theta2Deg = theta2Rad * 180.0 / Math.PI;

    const criticalAngleDeg = Math.asin(1.0 / n) * 180.0 / Math.PI;

    return {
      refractionAngleDeg: parseFloat(theta2Deg.toFixed(2)),
      criticalAngleDeg: parseFloat(criticalAngleDeg.toFixed(2)),
      isTotalInternalReflection: false // Entering denser medium from vacuum/air cannot undergo TIR
    };
  }

  /**
   * Calculate Clutter-to-Signal Ratio (CSR) and subsurface echo detection margin.
   * CSR = P_clutter_dB - P_nadir_dB
   * @param {number} clutterPowerDb - Off-nadir surface clutter return power in dB
   * @param {number} nadirEchoPowerDb - In-nadir subsurface reflection power in dB
   * @returns {{clutterToSignalRatioDb: number, isEchoDetectable: boolean, qualityMarginDb: number}}
   */
  static computeClutterToSignalRatio(clutterPowerDb, nadirEchoPowerDb) {
    const csr = clutterPowerDb - nadirEchoPowerDb;
    const margin = nadirEchoPowerDb - clutterPowerDb;
    const detectable = margin >= 3.0; // At least +3 dB Signal-to-Clutter margin

    return {
      clutterToSignalRatioDb: parseFloat(csr.toFixed(2)),
      isEchoDetectable: detectable,
      qualityMarginDb: parseFloat(margin.toFixed(2))
    };
  }

  // --- Two-Way Attenuation, Point Target Radar Equation & Reflectivity Permittivity Inversion Solvers ---

  /**
   * Calculate two-way radar power attenuation rate in dB per meter and dB per km.
   * alpha_2way = (2 * pi * f * sqrt(eps_r) * tan_delta) / c * 8.686
   * @param {number} freqHz - Radar center frequency in Hz (e.g. 20 MHz for SHARAD)
   * @param {number} lossTangent - Dielectric loss tangent tan(delta)
   * @param {number} [dielectricPermittivity=3.15] - Relative permittivity
   * @returns {{twoWayAttenuationDbPerM: number, twoWayAttenuationDbPerKm: number, skinDepthMeters: number}}
   */
  static computeTwoWaySignalAttenuationRate(freqHz, lossTangent, dielectricPermittivity = 3.15) {
    const f = Math.max(1e3, freqHz);
    const tanDelta = Math.max(1e-6, lossTangent);
    const eps = Math.max(1.0, dielectricPermittivity);

    const v = RadarSounderEngine.C / Math.sqrt(eps);
    const omega = 2.0 * Math.PI * f;
    const alphaOneWayDbM = ((omega * tanDelta) / (2.0 * v)) * 8.686;
    const alphaTwoWayDbM = alphaOneWayDbM * 2.0;
    const alphaTwoWayDbKm = alphaTwoWayDbM * 1000.0;

    const alphaNp = alphaOneWayDbM / 8.686;
    const skinDepth = 1.0 / Math.max(1e-8, alphaNp);

    return {
      twoWayAttenuationDbPerM: parseFloat(alphaTwoWayDbM.toFixed(5)),
      twoWayAttenuationDbPerKm: parseFloat(alphaTwoWayDbKm.toFixed(3)),
      skinDepthMeters: parseFloat(skinDepth.toFixed(1))
    };
  }

  /**
   * Calculate point target received echo power using the radar range equation.
   * P_rx = (P_tx * G^2 * lambda^2 * sigma) / ((4 * pi)^3 * R^4)
   * @param {number} [transmitterPowerW=10.0] - Transmit peak power in Watts
   * @param {number} [antennaGainLinear=1.0] - Power gain of antenna
   * @param {number} [wavelengthMeters=15.0] - Free-space radar wavelength in meters
   * @param {number} [rangeMeters=250000.0] - Spacecraft range to target in meters
   * @param {number} [radarCrossSectionM2=100.0] - Target radar cross section in m^2
   * @returns {{receivedPowerWatts: number, receivedPowerDbm: number}}
   */
  static computeRadarEquationPointTargetPower(transmitterPowerW = 10.0, antennaGainLinear = 1.0, wavelengthMeters = 15.0, rangeMeters = 250000.0, radarCrossSectionM2 = 100.0) {
    const Pt = Math.max(0.001, transmitterPowerW);
    const G = Math.max(0.1, antennaGainLinear);
    const lam = Math.max(0.1, wavelengthMeters);
    const R = Math.max(100.0, rangeMeters);
    const sigma = Math.max(0.01, radarCrossSectionM2);

    const numerator = Pt * G * G * lam * lam * sigma;
    const denominator = Math.pow(4.0 * Math.PI, 3) * Math.pow(R, 4);

    const PrxWatts = numerator / denominator;
    const PrxDbm = 10.0 * Math.log10(Math.max(1e-30, PrxWatts)) + 30.0;

    return {
      receivedPowerWatts: parseFloat(PrxWatts.toExponential(4)),
      receivedPowerDbm: parseFloat(PrxDbm.toFixed(2))
    };
  }

  /**
   * Invert relative dielectric permittivity from normal power reflectivity R.
   * eps_r = [ (1 + sqrt(R)) / (1 - sqrt(R)) ]^2
   * @param {number} reflectivityLinear - Linear power reflection coefficient (0.0 to <1.0)
   * @returns {{dielectricPermittivity: number, refractiveIndex: number, medium: string}}
   */
  static invertDielectricFromPowerReflectivity(reflectivityLinear) {
    const R = Math.max(1e-6, Math.min(0.999, reflectivityLinear));
    const sqrtR = Math.sqrt(R);
    const n = (1.0 + sqrtR) / (1.0 - sqrtR);
    const eps = n * n;

    const classification = this.classifySubsurfaceMedium(eps);

    return {
      dielectricPermittivity: parseFloat(eps.toFixed(3)),
      refractiveIndex: parseFloat(n.toFixed(3)),
      medium: classification.medium
    };
  }

  // --- Cross-Track Clutter Delay, Minimum Detectable Contrast & Resolution Volume Solvers ---

  /**
   * Calculate off-nadir topographic surface clutter excess time delay and apparent false depth.
   * Delta_t = 2 / c * ( sqrt(H^2 + y^2) - H )
   * @param {number} [orbitAltitudeKm=250.0] - Spacecraft altitude H in km
   * @param {number} [crossTrackOffsetKm=10.0] - Off-nadir horizontal distance y in km
   * @param {number} [epsIce=3.15] - Dielectric permittivity for false depth conversion
   * @returns {{excessDelayMicrosec: number, apparentDepthMeters: number, slantRangeKm: number}}
   */
  static computeCrossTrackClutterHorizonDelay(orbitAltitudeKm = 250.0, crossTrackOffsetKm = 10.0, epsIce = 3.15) {
    const H = orbitAltitudeKm * 1000.0;
    const y = crossTrackOffsetKm * 1000.0;

    const slantRangeM = Math.hypot(H, y);
    const deltaTM = 2.0 * (slantRangeM - H) / RadarSounderEngine.C;
    const delayMicrosec = deltaTM * 1e6;

    const vIce = RadarSounderEngine.getVelocity(epsIce);
    const apparentDepthM = (vIce * deltaTM) / 2.0;

    return {
      excessDelayMicrosec: parseFloat(delayMicrosec.toFixed(4)),
      apparentDepthMeters: parseFloat(apparentDepthM.toFixed(1)),
      slantRangeKm: parseFloat((slantRangeM / 1000.0).toFixed(3))
    };
  }

  /**
   * Calculate minimum detectable dielectric step contrast Delta_epsilon given radar SNR.
   * Delta_eps_min = sqrt(eps1) * 4 * 10^(-SNR_dB / 20)
   * @param {number} [topPermittivity=3.15] - Dielectric permittivity of overlying layer
   * @param {number} [minDetectableSnrDb=10.0] - Receiver minimum signal-to-noise ratio in dB
   * @returns {{minDetectableDeltaEps: number, minReflectivityDb: number}}
   */
  static computeMinimumDetectableDielectricContrast(topPermittivity = 3.15, minDetectableSnrDb = 10.0) {
    const eps1 = Math.max(1.0, topPermittivity);
    const snrLinear = Math.pow(10, minDetectableSnrDb / 20.0);
    const deltaEps = (4.0 * Math.sqrt(eps1)) / snrLinear;

    return {
      minDetectableDeltaEps: parseFloat(deltaEps.toFixed(4)),
      minReflectivityDb: parseFloat((-minDetectableSnrDb).toFixed(1))
    };
  }

  /**
   * Calculate 3D cylindrical pulse resolution volume (sounding voxel).
   * V_res = pi * r_fresnel^2 * Delta_z_vert
   * @param {number} [fresnelRadiusM=1500.0] - 1st Fresnel zone footprint radius in meters
   * @param {number} [verticalResolutionM=15.0] - Vertical range resolution in meters
   * @returns {{resolutionVolumeM3: number, resolutionVolumeKm3: number}}
   */
  static computeRadarResolutionVolume(fresnelRadiusM = 1500.0, verticalResolutionM = 15.0) {
    const r = Math.max(1.0, fresnelRadiusM);
    const dz = Math.max(0.1, verticalResolutionM);

    const volumeM3 = Math.PI * r * r * dz;
    const volumeKm3 = volumeM3 * 1e-9;

    return {
      resolutionVolumeM3: parseFloat(volumeM3.toExponential(4)),
      resolutionVolumeKm3: parseFloat(volumeKm3.toFixed(6))
    };
  }

  // --- Chirp Bandwidth, Basal Reflectivity & Specific Attenuation Solvers ---

  /**
   * Calculate uncompressed chirp pulse duration from bandwidth.
   * B = 1 / tau_pulse
   * @param {number} pulseDurationMicrosec - Chirp pulse length in microseconds (e.g. 85 µs for SHARAD)
   * @returns {{equivalentBandwidthMhz: number, equivalentBandwidthHz: number}}
   */
  static computeChirpCompressionBandwidth(pulseDurationMicrosec) {
    const tauSec = Math.max(1e-3, pulseDurationMicrosec) * 1e-6;
    const bHz = 1.0 / tauSec;
    const bMhz = bHz * 1e-6;

    return {
      equivalentBandwidthMhz: parseFloat(bMhz.toFixed(4)),
      equivalentBandwidthHz: parseFloat(bHz.toFixed(1))
    };
  }

  /**
   * Calculate normal-incidence basal interface Fresnel power reflectivity contrast.
   * R_int = [ (sqrt(eps1) - sqrt(eps2)) / (sqrt(eps1) + sqrt(eps2)) ]^2
   * @param {number} [epsTop=3.15] - Dielectric permittivity of overlying layer (e.g. ice = 3.15)
   * @param {number} [epsBottom=7.5] - Dielectric permittivity of underlying layer (e.g. basalt = 7.5)
   * @returns {{reflectivityLinear: number, reflectivityDb: number, reflectionLossDb: number}}
   */
  static computeBasalDielectricReflectivityContrast(epsTop = 3.15, epsBottom = 7.5) {
    const n1 = Math.sqrt(Math.max(1.0, epsTop));
    const n2 = Math.sqrt(Math.max(1.0, epsBottom));

    const rAmp = (n1 - n2) / (n1 + n2);
    const rPower = rAmp * rAmp;
    const rDb = 10.0 * Math.log10(Math.max(1e-12, rPower));
    const lossDb = -rDb;

    return {
      reflectivityLinear: parseFloat(rPower.toFixed(5)),
      reflectivityDb: parseFloat(rDb.toFixed(2)),
      reflectionLossDb: parseFloat(lossDb.toFixed(2))
    };
  }

  /**
   * Calculate specific radar power attenuation rate in dB per kilometer.
   * alpha_dB_km = 8.686 * (pi * f / c) * sqrt(eps_r) * tan(delta) * 1000
   * @param {number} [freqMhz=20.0] - Radar center frequency in MHz (e.g. 20 MHz for SHARAD)
   * @param {number} [relativePermittivity=3.15] - Relative dielectric permittivity
   * @param {number} [lossTangent=0.001] - Dielectric loss tangent
   * @returns {{attenuationDbPerKm: number, attenuationDbPerMeter: number}}
   */
  static computeSpecificRadarAttenuationRate(freqMhz = 20.0, relativePermittivity = 3.15, lossTangent = 0.001) {
    const fHz = Math.max(1e3, freqMhz * 1e6);
    const eps = Math.max(1.0, relativePermittivity);
    const tanDelta = Math.max(1e-6, lossTangent);

    // omega = 2 * pi * f, v = c / sqrt(eps)
    const v = RadarSounderEngine.C / Math.sqrt(eps);
    const omega = 2.0 * Math.PI * fHz;

    const alphaM = ((omega * tanDelta) / (2.0 * v)) * 8.686;
    const alphaKm = alphaM * 1000.0;

    return {
      attenuationDbPerKm: parseFloat(alphaKm.toFixed(3)),
      attenuationDbPerMeter: parseFloat(alphaM.toFixed(6))
    };
  }

  // --- Clutter Look Angle, Fresnel Transmission Matrix & Pulse Width Solvers ---

  /**
   * Calculate off-nadir radar clutter look angle and ground offset from excess delay.
   * theta = arccos( H / (H + c * Delta_tau / 2) )
   * @param {number} excessDelayMicrosec - Excess delay past nadir surface return (μs)
   * @param {number} [orbitAltitudeKm=250.0] - Spacecraft orbit altitude in km
   * @returns {{lookAngleDeg: number, lookAngleRad: number, groundOffsetKm: number}}
   */
  static computeClutterAngleFromExcessDelay(excessDelayMicrosec, orbitAltitudeKm = 250.0) {
    const HMeters = Math.max(1000, orbitAltitudeKm * 1000.0);
    const delaySec = Math.max(0, excessDelayMicrosec * 1e-6);
    const slantRangeM = HMeters + (RadarSounderEngine.C * delaySec) / 2.0;

    const cosTheta = Math.min(1.0, HMeters / slantRangeM);
    const thetaRad = Math.acos(cosTheta);
    const thetaDeg = thetaRad * 180.0 / Math.PI;
    const groundOffsetM = HMeters * Math.tan(thetaRad);

    return {
      lookAngleDeg: parseFloat(thetaDeg.toFixed(2)),
      lookAngleRad: parseFloat(thetaRad.toFixed(4)),
      groundOffsetKm: parseFloat((groundOffsetM / 1000.0).toFixed(2))
    };
  }

  /**
   * Calculate complete normal-incidence Fresnel amplitude & power transmission matrix.
   * r = (n1 - n2) / (n1 + n2), t = 2 * n1 / (n1 + n2), R = r^2, T = 1 - R
   * @param {number} eps1 - Dielectric permittivity of top layer
   * @param {number} eps2 - Dielectric permittivity of bottom layer
   * @returns {{amplitudeReflection: number, amplitudeTransmission: number, powerReflectivity: number, powerTransmissivity: number}}
   */
  static computeSubsurfaceFresnelTransmissionMatrix(eps1, eps2) {
    const n1 = Math.sqrt(Math.max(1.0, eps1));
    const n2 = Math.sqrt(Math.max(1.0, eps2));

    const rAmp = (n1 - n2) / (n1 + n2);
    const tAmp = (2.0 * n1) / (n1 + n2);
    const rPow = rAmp * rAmp;
    const tPow = 1.0 - rPow;

    return {
      amplitudeReflection: parseFloat(rAmp.toFixed(4)),
      amplitudeTransmission: parseFloat(tAmp.toFixed(4)),
      powerReflectivity: parseFloat(rPow.toFixed(4)),
      powerTransmissivity: parseFloat(tPow.toFixed(4))
    };
  }

  /**
   * Calculate matched-filter compressed pulse duration and vertical resolution in dielectric medium.
   * tau_p = 1 / B,  Delta_z = c / (2 * B * sqrt(eps_r))
   * @param {number} [chirpBandwidthMhz=10.0] - Chirp bandwidth in MHz (10 MHz for SHARAD)
   * @param {number} [epsR=3.15] - Dielectric permittivity of medium
   * @returns {{pulseDurationNs: number, verticalResolutionMeters: number, freeSpaceResolutionMeters: number}}
   */
  static computeRadarCompressedPulseWidth(chirpBandwidthMhz = 10.0, epsR = 3.15) {
    const BHz = Math.max(1e3, chirpBandwidthMhz * 1e6);
    const eps = Math.max(1.0, epsR);

    const tauSec = 1.0 / BHz;
    const deltaAir = RadarSounderEngine.C / (2.0 * BHz);
    const deltaMed = deltaAir / Math.sqrt(eps);

    return {
      pulseDurationNs: parseFloat((tauSec * 1e9).toFixed(1)),
      verticalResolutionMeters: parseFloat(deltaMed.toFixed(2)),
      freeSpaceResolutionMeters: parseFloat(deltaAir.toFixed(2))
    };
  }

  // --- Clutter Discrimination, Snell's Refraction & Slant Path Solvers ---

  /**
   * Calculate subsurface radar clutter-to-signal ratio (CSR) in decibels.
   * CSR = 10 * log10(P_clutter / P_signal)
   * @param {number} clutterPowerWatts - Off-nadir surface clutter echo power in Watts
   * @param {number} signalPowerWatts - True nadir subsurface echo power in Watts
   * @returns {{clutterToSignalRatioDb: number, isClutterDominant: boolean}}
   */
  static computeSubsurfaceClutterToSignalRatio(clutterPowerWatts, signalPowerWatts) {
    const pClutter = Math.max(1e-25, clutterPowerWatts);
    const pSignal = Math.max(1e-25, signalPowerWatts);

    const ratio = pClutter / pSignal;
    const csrDb = 10.0 * Math.log10(ratio);

    return {
      clutterToSignalRatioDb: parseFloat(csrDb.toFixed(2)),
      isClutterDominant: csrDb > 0
    };
  }

  /**
   * Calculate exact dielectric refraction angle and critical angle at subsurface boundaries using Snell's Law.
   * theta_2 = arcsin( sqrt(eps_1 / eps_2) * sin(theta_1) )
   * @param {number} incidenceAngleDeg - Incidence angle theta_1 in degrees
   * @param {number} epsUpper - Relative dielectric permittivity of upper medium
   * @param {number} epsLower - Relative dielectric permittivity of lower medium
   * @returns {{refractionAngleDeg: number, criticalAngleDeg: number, totalInternalReflection: boolean}}
   */
  static computeDielectricSnellsRefraction(incidenceAngleDeg, epsUpper, epsLower) {
    const eps1 = Math.max(1.0, epsUpper);
    const eps2 = Math.max(1.0, epsLower);
    const n1 = Math.sqrt(eps1);
    const n2 = Math.sqrt(eps2);

    const theta1Rad = (Math.max(0, Math.min(90, incidenceAngleDeg)) * Math.PI) / 180.0;
    const sinTheta2 = (n1 / n2) * Math.sin(theta1Rad);

    const isTIR = sinTheta2 > 1.0;
    const theta2Rad = isTIR ? Math.PI / 2.0 : Math.asin(sinTheta2);
    const theta2Deg = (theta2Rad * 180.0) / Math.PI;

    const criticalAngleDeg = n1 > n2 ? (Math.asin(n2 / n1) * 180.0) / Math.PI : 90.0;

    return {
      refractionAngleDeg: parseFloat(theta2Deg.toFixed(2)),
      criticalAngleDeg: parseFloat(criticalAngleDeg.toFixed(2)),
      totalInternalReflection: isTIR
    };
  }

  /**
   * Calculate true physical vertical depth taking into account dielectric wave speed and refracted slant ray angle.
   * Delta_z = (c * Delta_t) / (2 * sqrt(eps_r)) * cos(theta_r)
   * @param {number} twoWayDelayMicrosec - Two-way travel time delay in microseconds (μs)
   * @param {number} [epsR=3.15] - Dielectric permittivity of medium
   * @param {number} [refractionAngleDeg=0.0] - Refracted ray angle theta_r in degrees
   * @returns {{trueVerticalDepthMeters: number, slantPathDistanceMeters: number}}
   */
  static computeRefractedDepthDelayCorrection(twoWayDelayMicrosec, epsR = 3.15, refractionAngleDeg = 0.0) {
    const twtSec = Math.max(0, twoWayDelayMicrosec * 1e-6);
    const eps = Math.max(1.0, epsR);
    const v = RadarSounderEngine.C / Math.sqrt(eps);

    const slantDistM = (v * twtSec) / 2.0;
    const thetaRad = (Math.max(0, Math.min(89.9, refractionAngleDeg)) * Math.PI) / 180.0;
    const vertDepthM = slantDistM * Math.cos(thetaRad);

    return {
      trueVerticalDepthMeters: parseFloat(vertDepthM.toFixed(2)),
      slantPathDistanceMeters: parseFloat(slantDistM.toFixed(2))
    };
  }

  // --- Quality Factor, Ice-Dust Inversion & Rough Interface Scattering Solvers ---

  /**
   * Calculate dielectric quality factor Q = 1 / tan(delta) and RF loss regime.
   * @param {number} lossTangent - Dielectric loss tangent tan(delta)
   * @returns {{qualityFactorQ: number, lossRegime: string}}
   */
  static computeDielectricQualityFactor(lossTangent) {
    const tanD = Math.max(1e-6, lossTangent);
    const Q = 1.0 / tanD;

    let regime = 'Extremely Low Loss / Transparent (Pure Ice)';
    if (Q < 50) regime = 'High Loss / Strongly Attenuating (Conductive Regolith)';
    else if (Q < 250) regime = 'Moderate Loss (Dirty Ice / Permafrost)';
    else if (Q < 1000) regime = 'Low Loss (Polar Ice Cap)';

    return {
      qualityFactorQ: parseFloat(Q.toFixed(1)),
      lossRegime: regime
    };
  }

  /**
   * Invert pure water ice volumetric fraction phi_ice from observed bulk radar permittivity.
   * phi_ice = (sqrt(eps_dust) - sqrt(eps_bulk)) / (sqrt(eps_dust) - sqrt(eps_ice))
   * @param {number} bulkPermittivity - Observed bulk dielectric permittivity (e.g. 3.25 for NPLD)
   * @param {number} [epsIce=3.15] - Pure ice permittivity
   * @param {number} [epsDust=7.5] - Silicate dust / basalt grain permittivity
   * @returns {{iceFraction: number, dustFraction: number, icePercentage: number, dustPercentage: number}}
   */
  static invertIceDustVolumeFraction(bulkPermittivity, epsIce = 3.15, epsDust = 7.5) {
    const nBulk = Math.sqrt(Math.max(1.0, bulkPermittivity));
    const nIce = Math.sqrt(Math.max(1.0, epsIce));
    const nDust = Math.sqrt(Math.max(1.0, epsDust));

    const denom = nDust - nIce;
    let phiIce = denom > 0 ? (nDust - nBulk) / denom : 1.0;
    phiIce = Math.max(0.0, Math.min(1.0, phiIce));
    const phiDust = 1.0 - phiIce;

    return {
      iceFraction: parseFloat(phiIce.toFixed(4)),
      dustFraction: parseFloat(phiDust.toFixed(4)),
      icePercentage: parseFloat((phiIce * 100.0).toFixed(2)),
      dustPercentage: parseFloat((phiDust * 100.0).toFixed(2))
    };
  }

  /**
   * Calculate coherent radar power reflectivity reduction due to interface surface RMS roughness (Rayleigh criterion).
   * R_rough = R_smooth * exp( -4 * k_m^2 * sigma_h^2 * cos^2(theta) )
   * @param {number} smoothReflectivityDb - Fresnel smooth interface reflectivity in dB
   * @param {number} surfaceRmsRoughnessMeters - Root-mean-square surface roughness height sigma_h (meters)
   * @param {number} [freqHz=20e6] - Center frequency (20 MHz for SHARAD)
   * @param {number} [epsMedium=3.15] - Relative permittivity of overlying medium
   * @param {number} [incidenceAngleDeg=0.0] - Incidence angle in degrees
   * @returns {{roughReflectivityDb: number, roughnessScatteringLossDb: number, rayleighParameter: number}}
   */
  static computeRoughInterfaceScatteringLoss(smoothReflectivityDb, surfaceRmsRoughnessMeters, freqHz = 20e6, epsMedium = 3.15, incidenceAngleDeg = 0.0) {
    const sigmaH = Math.max(0, surfaceRmsRoughnessMeters);
    const eps = Math.max(1.0, epsMedium);
    const v = RadarSounderEngine.C / Math.sqrt(eps);
    const lambdaM = v / Math.max(1e3, freqHz);
    const km = (2.0 * Math.PI) / lambdaM; // Wavenumber in medium

    const thetaRad = (Math.max(0, Math.min(89.9, incidenceAngleDeg)) * Math.PI) / 180.0;
    const cosTheta = Math.cos(thetaRad);

    const gRayleigh = 4.0 * Math.pow(km * sigmaH * cosTheta, 2);
    // Attenuation factor: exp(-g) => in dB: -g * 10 * log10(e) = -g * 4.3429
    const lossDb = gRayleigh * 4.3429448;
    const roughDb = smoothReflectivityDb - lossDb;

    return {
      roughReflectivityDb: parseFloat(roughDb.toFixed(2)),
      roughnessScatteringLossDb: parseFloat(lossDb.toFixed(2)),
      rayleighParameter: parseFloat(gRayleigh.toFixed(4))
    };
  }

  // --- CRIM Multi-Phase Mixture, Fresnel Zone & Transmission Loss Solvers ---

  /**
   * Calculate effective dielectric permittivity of a multi-phase mixture using Complex Refractive Index Model (CRIM).
   * sqrt(eps_eff) = sum( f_i * sqrt(eps_i) )
   * @param {Object} volFractions - Volumetric fractions { ice: 0.8, rock: 0.15, void: 0.05 }
   * @param {Object} [epsComponents={ ice: 3.15, rock: 7.5, void: 1.0 }] - Component relative permittivities
   * @returns {{effectivePermittivity: number, effectiveRefractiveIndex: number, phaseVelocityKmS: number}}
   */
  static computeComplexRefractiveIndexMixture(volFractions = {}, epsComponents = { ice: 3.15, rock: 7.5, void: 1.0 }) {
    let sumN = 0;
    let totalFraction = 0;

    for (const key of Object.keys(volFractions)) {
      const f = Math.max(0, volFractions[key] || 0);
      const eps = Math.max(1.0, epsComponents[key] || 1.0);
      const n = Math.sqrt(eps);
      sumN += f * n;
      totalFraction += f;
    }

    // Normalize if fractions don't sum exactly to 1.0
    const nEff = totalFraction > 0 ? sumN / totalFraction : 1.0;
    const epsEff = Math.pow(nEff, 2);
    const vPhaseKmS = (RadarSounderEngine.C * 1e-3) / nEff;

    return {
      effectivePermittivity: parseFloat(epsEff.toFixed(4)),
      effectiveRefractiveIndex: parseFloat(nEff.toFixed(4)),
      phaseVelocityKmS: parseFloat(vPhaseKmS.toFixed(2))
    };
  }

  /**
   * Calculate first Fresnel zone footprint diameter d_F at depth z inside medium.
   * d_F = 2 * sqrt( (lambda_m * z) / 2 + (lambda_m^2) / 16 )
   * @param {number} depthMeters - Depth z inside medium in meters
   * @param {number} [freqHz=20e6] - Radar carrier frequency (20 MHz for SHARAD)
   * @param {number} [epsMedium=3.15] - Dielectric permittivity of medium
   * @returns {{fresnelDiameterMeters: number, wavelengthInMediumMeters: number}}
   */
  static computeFresnelZoneFootprintDiameter(depthMeters, freqHz = 20e6, epsMedium = 3.15) {
    const z = Math.max(0, depthMeters);
    const eps = Math.max(1.0, epsMedium);
    const v = RadarSounderEngine.C / Math.sqrt(eps);
    const lambdaM = v / Math.max(1e3, freqHz);

    const term = (lambdaM * z) / 2.0 + Math.pow(lambdaM, 2) / 16.0;
    const dFresnelM = 2.0 * Math.sqrt(term);

    return {
      fresnelDiameterMeters: parseFloat(dFresnelM.toFixed(2)),
      wavelengthInMediumMeters: parseFloat(lambdaM.toFixed(2))
    };
  }

  /**
   * Calculate cumulative two-way transmission power loss through an overlying stack of dielectric boundaries.
   * T_total = prod_{i=1}^n (1 - 10^(R_i / 10))^2
   * @param {Array<number>} reflectivityDbList - Array of Fresnel interface power reflectivities in dB (e.g. [-10, -15])
   * @returns {{twoWayTransmissionFraction: number, totalTransmissionLossDb: number}}
   */
  static computeTwoWayInterfaceTransmissionLoss(reflectivityDbList = []) {
    let tTotal = 1.0;

    for (const rDb of reflectivityDbList) {
      const rLinear = Math.min(0.999, Math.pow(10, rDb / 10.0));
      const tOneWay = 1.0 - rLinear;
      const tTwoWay = tOneWay * tOneWay;
      tTotal *= tTwoWay;
    }

    const lossDb = tTotal > 0 ? -10.0 * Math.log10(tTotal) : 999.0;

    return {
      twoWayTransmissionFraction: parseFloat(tTotal.toFixed(4)),
      totalTransmissionLossDb: parseFloat(lossDb.toFixed(3))
    };
  }

  // --- Range Resolution, Ionospheric Delay & Specific Attenuation Solvers ---

  /**
   * Calculate vertical range resolution Delta_r in vacuum and dielectric subsurface medium.
   * Delta_r_vac = c / (2 * B)
   * Delta_r_med = c / (2 * B * sqrt(eps_r))
   * @param {number} [bandwidthHz=10e6] - Chirp frequency bandwidth (10 MHz for SHARAD, 1 MHz for MARSIS)
   * @param {number} [epsReal=3.15] - Dielectric permittivity of subsurface medium (3.15 for water ice)
   * @returns {{rangeResolutionVacuumMeters: number, rangeResolutionMediumMeters: number, bandwidthMHz: number}}
   */
  static computeRadarVerticalRangeResolution(bandwidthHz = 10e6, epsReal = 3.15) {
    const B = Math.max(1e3, bandwidthHz);
    const eps = Math.max(1.0, epsReal);

    const deltaRVac = RadarSounderEngine.C / (2.0 * B);
    const deltaRMed = deltaRVac / Math.sqrt(eps);

    return {
      rangeResolutionVacuumMeters: parseFloat(deltaRVac.toFixed(2)),
      rangeResolutionMediumMeters: parseFloat(deltaRMed.toFixed(2)),
      bandwidthMHz: parseFloat((B / 1e6).toFixed(2))
    };
  }

  /**
   * Calculate specific one-way and two-way radar power attenuation rates in dB/m and dB/km.
   * alpha_dB_per_m = 8.686 * pi * f * sqrt(eps_r) * tan_delta / c
   * @param {number} carrierFreqHz - Radar carrier frequency in Hz (e.g. 20 MHz for SHARAD)
   * @param {number} epsReal - Real dielectric permittivity
   * @param {number} lossTangent - Dielectric loss tangent tan(delta)
   * @returns {{oneWayAttenuationDbPerMeter: number, twoWayAttenuationDbPerMeter: number, twoWayAttenuationDbPerKm: number}}
   */
  static computeMediumSpecificAttenuationRate(carrierFreqHz, epsReal, lossTangent) {
    const f = Math.max(1e3, carrierFreqHz);
    const eps = Math.max(1.0, epsReal);
    const tanD = Math.max(0, lossTangent);

    // alpha [Np/m] = pi * f * sqrt(eps) * tanD / c -> alpha [dB/m] = 8.6858896 * alpha [Np/m]
    const alphaOneWayDbM = (8.6858896 * Math.PI * f * Math.sqrt(eps) * tanD) / RadarSounderEngine.C;
    const alphaTwoWayDbM = alphaOneWayDbM * 2.0;
    const alphaTwoWayDbKm = alphaTwoWayDbM * 1000.0;

    return {
      oneWayAttenuationDbPerMeter: parseFloat(alphaOneWayDbM.toFixed(5)),
      twoWayAttenuationDbPerMeter: parseFloat(alphaTwoWayDbM.toFixed(5)),
      twoWayAttenuationDbPerKm: parseFloat(alphaTwoWayDbKm.toFixed(3))
    };
  }

  // --- Subsurface Radar Travel Time, Apparent Thickness & Critical Angle Solvers ---

  /**
   * Calculate two-way radar travel time (TWT) across a subsurface dielectric stratum.
   * Delta_t_twt = ( 2 * Delta_z * sqrt(eps_r) ) / c
   * @param {number} layerThicknessMeters - True stratum thickness in meters
   * @param {number} [dielectricPermittivity=3.15] - Relative dielectric permittivity eps_r (3.15 for water ice)
   * @returns {{twtMicroseconds: number, twtNanoseconds: number, propagationVelocityKmS: number}}
   */
  static computeLayerTwoWayTravelTime(layerThicknessMeters, dielectricPermittivity = 3.15) {
    const dz = Math.max(0.1, layerThicknessMeters);
    const eps = Math.max(1.0, dielectricPermittivity);

    const v = RadarSounderEngine.C / Math.sqrt(eps); // Phase speed in m/s
    const twtSeconds = (2.0 * dz) / v;
    const twtUs = twtSeconds * 1e6;
    const twtNs = twtSeconds * 1e9;

    return {
      twtMicroseconds: parseFloat(twtUs.toFixed(4)),
      twtNanoseconds: parseFloat(twtNs.toFixed(2)),
      propagationVelocityKmS: parseFloat((v / 1000.0).toFixed(2))
    };
  }

  /**
   * Calculate apparent radar free-space thickness (time-delay stretched thickness) for radargram visualization.
   * Delta_z_apparent = Delta_z_true * sqrt(eps_r)
   * @param {number} trueThicknessMeters - Physical stratum thickness in meters
   * @param {number} [dielectricPermittivity=3.15] - Subsurface relative permittivity
   * @returns {{apparentThicknessMeters: number, stretchRatio: number}}
   */
  static computeLayerApparentThicknessInFreeSpace(trueThicknessMeters, dielectricPermittivity = 3.15) {
    const dz = Math.max(0.1, trueThicknessMeters);
    const eps = Math.max(1.0, dielectricPermittivity);

    const sqrtEps = Math.sqrt(eps);
    const zApparent = dz * sqrtEps;

    return {
      apparentThicknessMeters: parseFloat(zApparent.toFixed(2)),
      stretchRatio: parseFloat(sqrtEps.toFixed(4))
    };
  }

  /**
   * Calculate Snell's law critical angle of total internal reflection for subsurface radar waves.
   * theta_c = arcsin( sqrt(eps2 / eps1) ) when eps1 > eps2 (e.g. ice-to-CO2 transition)
   * @param {number} epsUpper - Permittivity of incident medium eps1
   * @param {number} epsLower - Permittivity of transmitted medium eps2
   * @returns {{hasCriticalAngle: boolean, criticalAngleDeg: number, criticalAngleRad: number}}
   */
  static computeCriticalAngleOfRefraction(epsUpper, epsLower) {
    const eps1 = Math.max(1.0, epsUpper);
    const eps2 = Math.max(1.0, epsLower);

    if (eps1 <= eps2) {
      return {
        hasCriticalAngle: false,
        criticalAngleDeg: 90.0,
        criticalAngleRad: Math.PI / 2.0
      };
    }

    const sinThetaC = Math.sqrt(eps2 / eps1);
    const thetaCRad = Math.asin(Math.min(1.0, sinThetaC));
    const thetaCDeg = (thetaCRad * 180.0) / Math.PI;

    return {
      hasCriticalAngle: true,
      criticalAngleDeg: parseFloat(thetaCDeg.toFixed(3)),
      criticalAngleRad: parseFloat(thetaCRad.toFixed(5))
    };
  }

  // --- Synthetic Aperture Radar (SAR) Doppler & PRF Timing Solvers ---

  /**
   * Calculate theoretical along-track SAR azimuth spatial resolution Delta_x_az = L_antenna / 2.
   * @param {number} antennaLengthMeters - Physical antenna aperture length along-track in meters
   * @returns {{azimuthResolutionMeters: number, antennaLengthMeters: number}}
   */
  static computeSARAzimuthResolution(antennaLengthMeters) {
    const L = Math.max(0.1, antennaLengthMeters);
    const res = L / 2.0;

    return {
      azimuthResolutionMeters: parseFloat(res.toFixed(3)),
      antennaLengthMeters: parseFloat(L.toFixed(2))
    };
  }

  /**
   * Calculate radar Doppler frequency shift f_d for moving spacecraft sounder.
   * f_d = (2 * v / lambda) * sin(theta_along) * cos(theta_cross)
   * @param {number} velocityMS - Spacecraft ground speed in m/s
   * @param {number} frequencyHz - Radar center frequency in Hz
   * @param {number} [alongTrackAngleDeg=0] - Forward/backward squint angle along-track
   * @param {number} [crossTrackAngleDeg=0] - Cross-track off-nadir angle
   * @returns {{dopplerShiftHz: number, wavelengthMeters: number}}
   */
  static computeDopplerFrequencyShift(velocityMS, frequencyHz, alongTrackAngleDeg = 0, crossTrackAngleDeg = 0) {
    const v = velocityMS;
    const f = Math.max(1e3, frequencyHz);
    const lambda = RadarSounderEngine.C / f;

    const thAlongRad = (alongTrackAngleDeg * Math.PI) / 180.0;
    const thCrossRad = (crossTrackAngleDeg * Math.PI) / 180.0;

    const fd = ((2.0 * v) / lambda) * Math.sin(thAlongRad) * Math.cos(thCrossRad);

    return {
      dopplerShiftHz: parseFloat(fd.toFixed(3)),
      wavelengthMeters: parseFloat(lambda.toFixed(4))
    };
  }

  /**
   * Calculate unambiguous radar pulse repetition frequency (PRF) lower and upper bounds.
   * PRF_min = 2 * v / L_antenna (Doppler sampling)
   * PRF_max = c / (2 * R_max) (Range unambiguous swath)
   * @param {number} velocityMS - Spacecraft ground speed in m/s
   * @param {number} antennaLengthMeters - Physical antenna length in meters
   * @param {number} maxRangeKm - Maximum radar sounding range in km
   * @returns {{prfMinHz: number, prfMaxHz: number, hasValidPRFWrap: boolean}}
   */
  static computeRadarPulseRepetitionFrequencyBounds(velocityMS, antennaLengthMeters, maxRangeKm) {
    const v = Math.max(1.0, velocityMS);
    const L = Math.max(0.1, antennaLengthMeters);
    const rMax = Math.max(1.0, maxRangeKm * 1000.0);

    const prfMin = (2.0 * v) / L;
    const prfMax = RadarSounderEngine.C / (2.0 * rMax);

    return {
      prfMinHz: parseFloat(prfMin.toFixed(2)),
      prfMaxHz: parseFloat(prfMax.toFixed(2)),
      hasValidPRFWrap: prfMin <= prfMax
    };
  }

  // --- Fresnel Reflection & Two-Way Subsurface Attenuation Solvers ---

  /**
   * Calculate normal incidence Fresnel power reflection (R) and transmission (T) coefficients across dielectric interfaces.
   * R = ( (sqrt(eps1) - sqrt(eps2)) / (sqrt(eps1) + sqrt(eps2)) )^2
   * T = 1 - R
   * R_dB = 10 * log10(R)
   * @param {number} [eps1=1.0] - Upper medium dielectric real permittivity (e.g. 1.0 for vacuum/air)
   * @param {number} [eps2=3.15] - Lower medium dielectric real permittivity (e.g. 3.15 for water ice, ~8.0 for basalt)
   * @returns {{powerReflectionCoeff: number, powerTransmissionCoeff: number, reflectionCoeffDb: number}}
   */
  static computeFresnelReflectionAndTransmissionCoefficients(eps1 = 1.0, eps2 = 3.15) {
    const e1 = Math.max(1.0, eps1);
    const e2 = Math.max(1.0, eps2);

    const n1 = Math.sqrt(e1);
    const n2 = Math.sqrt(e2);

    const num = n1 - n2;
    const den = n1 + n2;
    const r = (num * num) / (den * den);
    const t = 1.0 - r;
    const rDb = r > 1e-12 ? 10.0 * Math.log10(r) : -120.0;

    return {
      powerReflectionCoeff: parseFloat(r.toFixed(5)),
      powerTransmissionCoeff: parseFloat(t.toFixed(5)),
      reflectionCoeffDb: parseFloat(rDb.toFixed(2))
    };
  }

  /**
   * Calculate two-way subsurface radar sounding attenuation loss in dB.
   * alpha_dB/m = 8.686 * (pi * f * sqrt(eps_r) * tan(delta)) / c
   * Loss_2way_dB = 2 * alpha_dB/m * depthMeters
   * @param {number} frequencyHz - Radar center frequency in Hz (e.g. 20 MHz = 20e6 for SHARAD, 4 MHz for MARSIS)
   * @param {number} dielectricPermittivity - Subsurface real relative permittivity (e.g. 3.15 for pure ice)
   * @param {number} lossTangent - Subsurface dielectric loss tangent tan(delta) (e.g. 0.001 for cold pure ice)
   * @param {number} depthMeters - Penetration sounding depth in meters
   * @returns {{twoWayLossDb: number, attenuationRateDbPerM: number, attenuationRateDbPerKm: number}}
   */
  static computeTwoWayRadarSubsurfaceAttenuation(frequencyHz, dielectricPermittivity, lossTangent, depthMeters) {
    const f = Math.max(1e3, frequencyHz);
    const er = Math.max(1.0, dielectricPermittivity);
    const tanD = Math.max(1e-7, lossTangent);
    const z = Math.max(0.0, depthMeters);

    // Attenuation constant alpha in Np/m
    const alphaNp = (Math.PI * f * Math.sqrt(er) * tanD) / RadarSounderEngine.C;
    const alphaDbM = 8.685889638 * alphaNp; // 1 Np = 8.685889638 dB
    const alphaDbKm = alphaDbM * 1000.0;

    const twoWayLoss = 2.0 * alphaDbM * z;

    return {
      twoWayLossDb: parseFloat(twoWayLoss.toFixed(3)),
      attenuationRateDbPerM: parseFloat(alphaDbM.toExponential(4)),
      attenuationRateDbPerKm: parseFloat(alphaDbKm.toFixed(3))
    };
  }

  // --- Subsurface True Depth & Interface Echo Power Solvers ---

  /**
   * Calculate true physical depth (meters and km) from two-way radar travel time delay.
   * d = ( c * delta_t ) / ( 2 * sqrt(eps_r) )
   * @param {number} timeDelayMicroseconds - Two-way travel time delay in microseconds (µs)
   * @param {number} [dielectricPermittivity=3.15] - Relative dielectric permittivity (e.g. 3.15 for water ice, 5.5 for basaltic regolith)
   * @returns {{depthMeters: number, depthKm: number, phaseVelocityKmS: number}}
   */
  static computeSubsurfaceTrueDepth(timeDelayMicroseconds, dielectricPermittivity = 3.15) {
    const dtSec = Math.max(0.0, timeDelayMicroseconds) * 1e-6;
    const er = Math.max(1.0, dielectricPermittivity);
    const n = Math.sqrt(er);

    const vPhase = RadarSounderEngine.C / n; // m/s
    const depthM = (vPhase * dtSec) / 2.0;

    return {
      depthMeters: parseFloat(depthM.toFixed(2)),
      depthKm: parseFloat((depthM / 1000.0).toFixed(4)),
      phaseVelocityKmS: parseFloat((vPhase / 1000.0).toFixed(2))
    };
  }

  /**
   * Calculate net received subsurface interface echo power in dB given initial power, reflection coefficient, and two-way path loss.
   * P_rx = P_tx + R_dB - Loss_2way_dB
   * @param {number} initialPowerDb - Transmitted / surface reference radar power in dB
   * @param {number} reflectionCoeffDb - Interface Fresnel power reflection coefficient in dB (negative value, e.g. -15 dB)
   * @param {number} twoWayLossDb - Total two-way dielectric attenuation loss in dB (positive value, e.g. 12 dB)
   * @returns {{receivedEchoPowerDb: number, netEchoAttenuationDb: number, isDetectableEcho: boolean}}
   */
  static computeSubsurfaceInterfaceReturnPower(initialPowerDb, reflectionCoeffDb, twoWayLossDb) {
    const pTx = initialPowerDb;
    const rDb = Math.min(0.0, reflectionCoeffDb);
    const lossDb = Math.max(0.0, twoWayLossDb);

    const pRx = pTx + rDb - lossDb;
    const netAtten = Math.abs(rDb) + lossDb;

    // Detectable echo if above noise floor (~ -90 dB relative to transmitted power)
    const isDet = (pRx - pTx) > -90.0;

    return {
      receivedEchoPowerDb: parseFloat(pRx.toFixed(2)),
      netEchoAttenuationDb: parseFloat(netAtten.toFixed(2)),
      isDetectableEcho: isDet
    };
  }

  // --- Complex Refractive Index & Sounding Vertical Range Resolution ---

  /**
   * Calculate complex refractive index (n + i*kappa) and electromagnetic 1/e penetration skin depth.
   * eps'' = eps' * tan(delta)
   * n = sqrt( ( eps' + sqrt(eps'^2 + eps''^2) ) / 2 )
   * kappa = sqrt( ( -eps' + sqrt(eps'^2 + eps''^2) ) / 2 )
   * delta_skin = c / ( 2 * pi * f * kappa )  (meters)
   * @param {number} frequencyHz - Radar carrier frequency in Hz (e.g. 20 MHz for SHARAD, 4 MHz for MARSIS)
   * @param {number} epsReal - Real relative dielectric permittivity (e.g. 3.15 for ice)
   * @param {number} lossTangent - Dielectric loss tangent tan(delta) (e.g. 0.001 for cold pure ice)
   * @returns {{refractiveIndexN: number, extinctionCoeffKappa: number, skinDepthMeters: number, phaseVelocityKmS: number}}
   */
  static computeComplexRefractiveIndexAndSkinDepth(frequencyHz, epsReal, lossTangent) {
    const f = Math.max(1e3, frequencyHz);
    const er = Math.max(1.0, epsReal);
    const tanD = Math.max(1e-7, lossTangent);

    const ei = er * tanD;
    const hyp = Math.sqrt(er * er + ei * ei);

    const n = Math.sqrt((er + hyp) / 2.0);
    const kappa = Math.sqrt((-er + hyp) / 2.0);

    const vPhase = (RadarSounderEngine.C / n) / 1000.0; // km/s
    const skinDepth = kappa > 0 ? RadarSounderEngine.C / (2.0 * Math.PI * f * kappa) : 99999.0;

    return {
      refractiveIndexN: parseFloat(n.toFixed(5)),
      extinctionCoeffKappa: parseFloat(kappa.toExponential(4)),
      skinDepthMeters: parseFloat(skinDepth.toFixed(1)),
      phaseVelocityKmS: parseFloat(vPhase.toFixed(2))
    };
  }

  /**
   * Calculate vertical sounding range resolution in free space and within a subsurface dielectric medium.
   * delta_z_air = c / ( 2 * Bandwidth )
   * delta_z_medium = c / ( 2 * Bandwidth * sqrt(eps_r) )
   * @param {number} bandwidthHz - Radar chirp bandwidth in Hz (e.g. 10 MHz = 10e6 for SHARAD, 1 MHz for MARSIS)
   * @param {number} [dielectricPermittivity=3.15] - Medium dielectric constant (3.15 for water ice, 5.5 for basalt)
   * @returns {{verticalResolutionAirMeters: number, verticalResolutionMediumMeters: number, mediumWavelengthMeters: number}}
   */
  static computeSubsurfaceLayerVerticalResolution(bandwidthHz, dielectricPermittivity = 3.15) {
    const B = Math.max(1e3, bandwidthHz);
    const er = Math.max(1.0, dielectricPermittivity);
    const n = Math.sqrt(er);

    const resAir = RadarSounderEngine.C / (2.0 * B);
    const resMed = resAir / n;

    return {
      verticalResolutionAirMeters: parseFloat(resAir.toFixed(2)),
      verticalResolutionMediumMeters: parseFloat(resMed.toFixed(2)),
      mediumWavelengthMeters: parseFloat((RadarSounderEngine.C / (20e6 * n)).toFixed(3)) // for 20 MHz reference
    };
  }

  // --- Fresnel Zone Footprint & Normal-Incidence Power Reflection ---

  /**
   * Calculate unfocused First Fresnel Zone radius and diameter on the planetary surface.
   * lambda = c / f
   * r_fresnel = sqrt( (lambda * h) / 2 )
   * @param {number} spacecraftAltitudeKm - Spacecraft altitude above surface h in km (e.g. 250 - 320 km for MRO)
   * @param {number} [radarFrequencyMhz=20.0] - Carrier frequency in MHz (20 MHz for SHARAD, 4 MHz for MARSIS)
   * @returns {{wavelengthMeters: number, fresnelRadiusMeters: number, fresnelDiameterKm: number}}
   */
  static computeRadarFirstFresnelZoneRadius(spacecraftAltitudeKm, radarFrequencyMhz = 20.0) {
    const h = Math.max(10.0, spacecraftAltitudeKm) * 1000.0; // meters
    const f = Math.max(0.1, radarFrequencyMhz) * 1e6; // Hz

    const lambda = RadarSounderEngine.C / f; // meters
    const rFresnel = Math.sqrt((lambda * h) / 2.0);
    const dFresnelKm = (2.0 * rFresnel) / 1000.0;

    return {
      wavelengthMeters: parseFloat(lambda.toFixed(3)),
      fresnelRadiusMeters: parseFloat(rFresnel.toFixed(1)),
      fresnelDiameterKm: parseFloat(dFresnelKm.toFixed(3))
    };
  }

  /**
   * Calculate normal-incidence Fresnel power reflection coefficient and reflection loss in decibels.
   * R_pwr = ( (sqrt(eps_r) - 1) / (sqrt(eps_r) + 1) )^2
   * Loss_dB = 10 * log10(R_pwr)
   * @param {number} dielectricPermittivityEpsilon - Real relative permittivity (e.g. 3.15 for ice, 7.5 for basalt)
   * @returns {{powerReflectionCoeff: number, reflectionLossDb: number, powerTransmissionCoeff: number}}
   */
  static computeRadarSurfacePowerReflectionLoss(dielectricPermittivityEpsilon) {
    const er = Math.max(1.0, dielectricPermittivityEpsilon);
    const n = Math.sqrt(er);

    const rField = (n - 1.0) / (n + 1.0);
    const rPwr = rField * rField;
    const lossDb = rPwr > 0 ? 10.0 * Math.log10(rPwr) : -99.0;
    const transPwr = 1.0 - rPwr;

    return {
      powerReflectionCoeff: parseFloat(rPwr.toFixed(4)),
      reflectionLossDb: parseFloat(lossDb.toFixed(2)),
      powerTransmissionCoeff: parseFloat(transPwr.toFixed(4))
    };
  }

  // --- Radar Clutter Simulation & Doppler Solvers ---

  /**
   * Calculate off-nadir surface clutter two-way travel time delay relative to nadir return.
   * R_slant = sqrt( x^2 + y^2 + (h - z)^2 )
   * Delta_t = 2 * (R_slant - h) / c
   * @param {number} crossTrackOffsetKm - Across-track distance x from ground track in km
   * @param {number} alongTrackOffsetKm - Along-track distance y from spacecraft nadir in km
   * @param {number} [spacecraftAltitudeKm=300.0] - Spacecraft altitude above datum h in km
   * @param {number} [facetElevationKm=0.0] - Surface topographic feature elevation z in km
   * @returns {{slantRangeKm: number, nadirRangeKm: number, excessSlantRangeKm: number, clutterExcessDelayMicrosec: number, nadirTravelTimeMicrosec: number}}
   */
  static computeRadarOffNadirClutterTimeDelay(crossTrackOffsetKm, alongTrackOffsetKm, spacecraftAltitudeKm = 300.0, facetElevationKm = 0.0) {
    const x = crossTrackOffsetKm;
    const y = alongTrackOffsetKm;
    const h = Math.max(1.0, spacecraftAltitudeKm);
    const z = facetElevationKm;

    const deltaZ = h - z;
    const slantRange = Math.sqrt(x * x + y * y + deltaZ * deltaZ);
    const excessRange = slantRange - h;

    const cKmS = RadarSounderEngine.C / 1000.0; // ~299792.458 km/s
    const tNadir = (2.0 * h) / cKmS; // seconds
    const tExcess = (2.0 * excessRange) / cKmS; // seconds

    return {
      slantRangeKm: parseFloat(slantRange.toFixed(3)),
      nadirRangeKm: parseFloat(h.toFixed(3)),
      excessSlantRangeKm: parseFloat(excessRange.toFixed(3)),
      clutterExcessDelayMicrosec: parseFloat((tExcess * 1e6).toFixed(3)),
      nadirTravelTimeMicrosec: parseFloat((tNadir * 1e6).toFixed(3))
    };
  }

  /**
   * Calculate radar Doppler frequency shift for off-nadir surface facets.
   * f_Doppler = (2 * v_sc * f_0 / c) * ( y_along / R_slant )
   * @param {number} [spacecraftVelocityKmS=3.448] - Spacecraft along-track speed v_sc in km/s (3.448 km/s for MRO)
   * @param {number} [radarCarrierFreqMHz=20.0] - Carrier frequency f_0 in MHz (20 MHz for SHARAD)
   * @param {number} [alongTrackOffsetKm=10.0] - Along-track position y in km (+ahead, -behind)
   * @param {number} [crossTrackOffsetKm=0.0] - Across-track position x in km
   * @param {number} [spacecraftAltitudeKm=300.0] - Spacecraft altitude h in km
   * @returns {{dopplerShiftHz: number, wavelengthMeters: number, maxZeroDopplerAngleDeg: number}}
   */
  static computeRadarDopplerFrequencyShift(spacecraftVelocityKmS = 3.448, radarCarrierFreqMHz = 20.0, alongTrackOffsetKm = 10.0, crossTrackOffsetKm = 0.0, spacecraftAltitudeKm = 300.0) {
    const v = spacecraftVelocityKmS * 1000.0; // m/s
    const f0 = Math.max(0.1, radarCarrierFreqMHz) * 1e6; // Hz
    const lambda = RadarSounderEngine.C / f0; // m

    const x = crossTrackOffsetKm * 1000.0; // m
    const y = alongTrackOffsetKm * 1000.0; // m
    const h = Math.max(1.0, spacecraftAltitudeKm) * 1000.0; // m

    const slantRange = Math.sqrt(x * x + y * y + h * h);
    const cosThetaV = y / slantRange;

    const fDoppler = ((2.0 * v) / lambda) * cosThetaV;
    const angleDeg = (Math.asin(Math.min(1.0, Math.max(-1.0, cosThetaV))) * 180.0) / Math.PI;

    return {
      dopplerShiftHz: parseFloat(fDoppler.toFixed(2)),
      wavelengthMeters: parseFloat(lambda.toFixed(3)),
      maxZeroDopplerAngleDeg: parseFloat(angleDeg.toFixed(3))
    };
  }
}


















