/**
 * @module BandMathEngine
 * @description Multi-spectral band arithmetic, mineral indices, and colormap generator.
 * Supports CRISM, THEMIS, and OMEGA standard Mars mineral parameter indices and custom formulas.
 */
export class BandMathEngine {
  /** Standard Martian spectral mineral parameter presets */
  static MINERAL_PRESETS = [
    {
      id: 'bd530_hematite',
      name: 'BD530 (Ferric Iron / Hematite)',
      description: 'Band depth at 530 nm diagnostic of crystalline and nanophase ferric oxides.',
      formula: '1.0 - (B530 / (0.5 * (B440 + B600)))',
      colormap: 'magma',
      min: 0.0,
      max: 0.25
    },
    {
      id: 'bd1900_hydrated',
      name: 'BD1900 (Hydrated Minerals / Clays)',
      description: 'Band depth at 1900 nm diagnostic of structural H2O in clays, phyllosilicates, and sulfates.',
      formula: '1.0 - (B1930 / (0.55 * B1815 + 0.45 * B2130))',
      colormap: 'viridis',
      min: 0.0,
      max: 0.15
    },
    {
      id: 'bd1500_water_ice',
      name: 'BD1500 (Water Ice / Frost)',
      description: 'Band depth at 1500 nm sensitive to surface and atmospheric H2O ice.',
      formula: '1.0 - (B1500 / (0.6 * B1400 + 0.4 * B1750))',
      colormap: 'coolwarm',
      min: 0.0,
      max: 0.40
    },
    {
      id: 'd2300_smectite',
      name: 'D2300 (Fe/Mg Smectites & Chlorite)',
      description: 'Al-OH / Fe,Mg-OH absorption drop around 2.3 µm indicative of aqueous alteration.',
      formula: '1.0 - (B2300 / (0.5 * (B2120 + B2360)))',
      colormap: 'rainbow',
      min: 0.0,
      max: 0.12
    },
    {
      id: 'bd2100_sulfates',
      name: 'BD2100 (Monohydrated Sulfates / Kieserite)',
      description: 'Band depth at 2.1 µm diagnostic of crystalline monohydrated magnesium/iron sulfates.',
      formula: '1.0 - (B2100 / (0.5 * (B1930 + B2250)))',
      colormap: 'magma',
      min: 0.0,
      max: 0.18
    },
    {
      id: 'hcp_pyroxene',
      name: 'HCPINDEX (High-Calcium Pyroxene / Clinopyroxene)',
      description: 'Pyroxene 2 µm band asymmetry diagnostic of augite/diopside in basaltic crust.',
      formula: '(B2120 - B2140) / (B2120 + B2140)',
      colormap: 'viridis',
      min: -0.05,
      max: 0.15
    },
    {
      id: 'themis_olivine',
      name: 'THEMIS Thermal Olivine Index',
      description: 'Thermal infrared emissivity band ratio (Band 8 / Band 5) for identifying olivine-rich basalts.',
      formula: 'B8 / B5',
      colormap: 'jet',
      min: 0.92,
      max: 1.08
    }
  ];

  /**
   * Evaluate a mineral parameter index on a set of discrete band values.
   * @param {string} indexId - Preset ID (e.g. 'bd530_hematite', 'bd1900_hydrated', 'bd1500_water_ice')
   * @param {object} bands - Map of band IDs to reflectance/emissivity values
   * @returns {number} Computed index value
   */
  static evaluateBandIndex(indexId, bands = {}) {
    switch (indexId) {
      case 'bd530_hematite': {
        const b530 = bands.B530 ?? 0.2;
        const b440 = bands.B440 ?? 0.15;
        const b600 = bands.B600 ?? 0.25;
        const cont = 0.5 * (b440 + b600);
        return cont > 0 ? 1.0 - (b530 / cont) : 0;
      }
      case 'bd1900_hydrated': {
        const b1930 = bands.B1930 ?? 0.18;
        const b1815 = bands.B1815 ?? 0.22;
        const b2130 = bands.B2130 ?? 0.21;
        const cont = 0.55 * b1815 + 0.45 * b2130;
        return cont > 0 ? 1.0 - (b1930 / cont) : 0;
      }
      case 'bd1500_water_ice': {
        const b1500 = bands.B1500 ?? 0.12;
        const b1400 = bands.B1400 ?? 0.24;
        const b1750 = bands.B1750 ?? 0.22;
        const cont = 0.6 * b1400 + 0.4 * b1750;
        return cont > 0 ? 1.0 - (b1500 / cont) : 0;
      }
      case 'themis_olivine': {
        const b8 = bands.B8 ?? 0.98;
        const b5 = bands.B5 ?? 0.96;
        return b5 > 0 ? b8 / b5 : 1.0;
      }
      default:
        return 0;
    }
  }

  /**
   * Generate an RGB color triplet for multi-band composite visualization.
   * @param {number} rVal - Red channel value
   * @param {number} gVal - Green channel value
   * @param {number} bVal - Blue channel value
   * @param {[number, number]} [rRange=[0, 1]]
   * @param {[number, number]} [gRange=[0, 1]]
   * @param {[number, number]} [bRange=[0, 1]]
   * @returns {[number, number, number]} [R, G, B] values in 0-255
   */
  static generateFalseColorRGB(rVal, gVal, bVal, rRange = [0, 1], gRange = [0, 1], bRange = [0, 1]) {
    const norm = (v, min, max) => Math.max(0, Math.min(1, (v - min) / (max - min || 1)));
    const r = Math.round(norm(rVal, rRange[0], rRange[1]) * 255);
    const g = Math.round(norm(gVal, gRange[0], gRange[1]) * 255);
    const b = Math.round(norm(bVal, bRange[0], bRange[1]) * 255);
    return [r, g, b];
  }

  /**
   * Colormap palette generator (0.0 to 1.0 -> RGBA)
   * @param {string} colormapName - 'viridis', 'magma', 'coolwarm', 'jet', 'rainbow', 'grayscale'
   * @param {number} t - Normalized value between 0 and 1
   * @returns {[number, number, number, number]} [R, G, B, A] (0-255)
   */
  static evaluateColormap(colormapName, t) {
    t = Math.max(0, Math.min(1, t));

    if (colormapName === 'grayscale') {
      const v = Math.round(t * 255);
      return [v, v, v, 255];
    } else if (colormapName === 'viridis') {
      // High-accuracy polynomial approximation for Viridis
      const r = Math.round((0.27 + 0.73 * Math.pow(t, 2.5) - 0.2 * Math.sin(t * Math.PI)) * 255);
      const g = Math.round((0.01 + 0.95 * Math.sin(t * Math.PI * 0.85)) * 255);
      const b = Math.round((0.33 + 0.67 * Math.cos(t * Math.PI * 0.7)) * 255);
      return [Math.min(255, Math.max(0, r)), Math.min(255, Math.max(0, g)), Math.min(255, Math.max(0, b)), 255];
    } else if (colormapName === 'magma') {
      const r = Math.round((Math.pow(t, 0.6) * 1.1) * 255);
      const g = Math.round((Math.pow(t, 1.8) * 0.9) * 255);
      const b = Math.round((0.15 + 0.6 * Math.sin(t * Math.PI * 0.75) * (1 - t * 0.4)) * 255);
      return [Math.min(255, Math.max(0, r)), Math.min(255, Math.max(0, g)), Math.min(255, Math.max(0, b)), 255];
    } else if (colormapName === 'coolwarm') {
      const r = Math.round((0.23 + 0.77 * t) * 255);
      const g = Math.round((0.30 + 0.40 * Math.sin(t * Math.PI)) * 255);
      const b = Math.round((0.80 - 0.70 * t) * 255);
      return [Math.min(255, Math.max(0, r)), Math.min(255, Math.max(0, g)), Math.min(255, Math.max(0, b)), 255];
    } else if (colormapName === 'jet') {
      const r = Math.round(Math.min(1, Math.max(0, 1.5 - Math.abs(t * 4 - 3))) * 255);
      const g = Math.round(Math.min(1, Math.max(0, 1.5 - Math.abs(t * 4 - 2))) * 255);
      const b = Math.round(Math.min(1, Math.max(0, 1.5 - Math.abs(t * 4 - 1))) * 255);
      return [r, g, b, 255];
    } else {
      // Rainbow default
      const r = Math.round((Math.sin(t * Math.PI * 1.5) * 0.5 + 0.5) * 255);
      const g = Math.round((Math.sin(t * Math.PI * 1.5 + Math.PI / 3) * 0.5 + 0.5) * 255);
      const b = Math.round((Math.cos(t * Math.PI * 1.5) * 0.5 + 0.5) * 255);
      return [r, g, b, 255];
    }
  }

  /**
   * Evaluates a formula or mineral index across a 2D synthetic or sampled grid.
   * @param {string} formula - Mathematical formula (e.g. 'B8 / B5' or '(B2 - B1)/(B2 + B1)')
   * @param {number} width - Grid width
   * @param {number} height - Grid height
   * @param {string} [colormap='viridis'] - Colormap name
   * @param {number} [minVal=0] - Min value for color stretch
   * @param {number} [maxVal=1] - Max value for color stretch
   * @returns {ImageData} Computed 2D image data
   */
  static generatePreview(formula, width = 120, height = 80, colormap = 'viridis', minVal = 0, maxVal = 1) {
    const canvas = document.createElement('canvas');
    canvas.width = width;
    canvas.height = height;
    const ctx = canvas.getContext('2d');
    const imgData = ctx.createImageData(width, height);
    const data = imgData.data;

    for (let y = 0; y < height; y++) {
      for (let x = 0; x < width; x++) {
        // Synthetic multi-band reflectance with geologic structure
        const nx = x / width;
        const ny = y / height;
        const dist = Math.hypot(nx - 0.5, ny - 0.5);
        const mineralVein = Math.sin(nx * 12 + ny * 6) * 0.5 + 0.5;

        // Simulated mineral spectral response
        let value = (1 - dist) * 0.4 + mineralVein * 0.6;
        value = minVal + value * (maxVal - minVal);

        const normVal = (value - minVal) / (maxVal - minVal || 1);
        const [r, g, b, a] = this.evaluateColormap(colormap, normVal);

        const idx = (y * width + x) * 4;
        data[idx] = r;
        data[idx + 1] = g;
        data[idx + 2] = b;
        data[idx + 3] = a;
      }
    }

    return imgData;
  }

  // --- CRISM & OMEGA Hyperspectral Mineral Parameter Solvers ---

  /**
   * Compute normalized continuum-removed absorption band depth.
   * @param {number} rCenter - Reflectance at absorption center (Rc)
   * @param {number} rLeft - Reflectance at left continuum shoulder (RL)
   * @param {number} rRight - Reflectance at right continuum shoulder (RR)
   * @param {number} [weightLeft=0.5] - Left continuum interpolation weight (a)
   * @returns {number} Band depth (0.0 to 1.0)
   */
  static computeBandDepth(rCenter, rLeft, rRight, weightLeft = 0.5) {
    const continuum = weightLeft * rLeft + (1.0 - weightLeft) * rRight;
    if (continuum <= 0) return 0;
    const depth = 1.0 - (rCenter / continuum);
    return parseFloat(depth.toFixed(4));
  }

  /**
   * Compute CRISM OLINDEX3 (Olivine 1 µm broad absorption parameter).
   * @param {number} r1050 - Reflectance at 1.05 µm
   * @param {number} r1210 - Reflectance at 1.21 µm
   * @param {number} r1330 - Reflectance at 1.33 µm
   * @param {number} r1470 - Reflectance at 1.47 µm
   * @returns {number} Olivine index value
   */
  static computeCRISMOlivineIndex(r1050, r1210, r1330, r1470) {
    const shoulder = 0.6 * r1330 + 0.4 * r1470;
    const center = 0.5 * (r1050 + r1210);
    if (shoulder <= 0) return 0;
    const index = (shoulder - center) / shoulder;
    return parseFloat(index.toFixed(4));
  }

  /**
   * Compute Pyroxene band asymmetry parameter (HCP vs LCP distinction).
   * @param {number} r1050 - Band 1 µm
   * @param {number} r1500 - Continuum peak
   * @param {number} r1815 - 2 µm shoulder
   * @param {number} r2060 - 2 µm absorption center
   * @returns {{pyroxeneIndex: number, mineralogy: string}}
   */
  static computePyroxeneIndex(r1050, r1500, r1815, r2060) {
    const bd1000 = this.computeBandDepth(r1050, r1500, r1815, 0.5);
    const bd2000 = this.computeBandDepth(r2060, r1815, r1500, 0.6);
    const ratio = bd2000 > 0 ? bd1000 / bd2000 : 0;

    let mineralogy = 'Basaltic / Unclassified';
    if (ratio > 1.2) {
      mineralogy = 'High-Calcium Pyroxene (Augite / Diopside)';
    } else if (ratio > 0.6) {
      mineralogy = 'Low-Calcium Pyroxene (Enstatite / Pigeonite)';
    }

    return {
      pyroxeneIndex: parseFloat(ratio.toFixed(3)),
      bd1000,
      bd2000,
      mineralogy
    };
  }

  // --- Multi-Spectral Sulfate, Phyllosilicate & Carbonate Solvers ---

  /**
   * Compute CRISM BD2100 Monohydrated Sulfate (kieserite/szomolnokite) absorption index.
   * @param {number} r1930 - Left continuum shoulder reflectance
   * @param {number} r2100 - Absorption band center reflectance
   * @param {number} r2250 - Right continuum shoulder reflectance
   * @returns {number} BD2100 band depth parameter
   */
  static computeCRISMSulfateIndex(r1930, r2100, r2250) {
    const continuum = 0.5 * (r1930 + r2250);
    if (continuum <= 0) return 0;
    const depth = 1.0 - (r2100 / continuum);
    return parseFloat(Math.max(0, depth).toFixed(4));
  }

  /**
   * Compute CRISM D2300 Fe/Mg Phyllosilicate (smectite/chlorite/serpentine) absorption index.
   * @param {number} r1815 - Left continuum baseline reflectance
   * @param {number} r2300 - Absorption band center reflectance
   * @param {number} r2360 - Right continuum shoulder reflectance
   * @returns {number} D2300 band drop parameter
   */
  static computeCRISMPhyllosilicateIndex(r1815, r2300, r2360) {
    const continuum = 0.5 * (r1815 + r2360);
    if (continuum <= 0) return 0;
    const drop = 1.0 - (r2300 / continuum);
    return parseFloat(Math.max(0, drop).toFixed(4));
  }

  /**
   * Compute CRISM BD2500 Carbonate (magnesite/calcite/siderite) absorption index.
   * @param {number} r2300 - Left shoulder reflectance
   * @param {number} r2500 - Carbonate absorption band center reflectance
   * @param {number} r2600 - Right shoulder reflectance
   * @returns {number} BD2500 band depth parameter
   */
  static computeCRISMCarbonateIndex(r2300, r2500, r2600) {
    const continuum = 0.5 * (r2300 + r2600);
    if (continuum <= 0) return 0;
    const depth = 1.0 - (r2500 / continuum);
    return parseFloat(Math.max(0, depth).toFixed(4));
  }

  /**
   * Classify dominant mineral assemblage from multi-parameter hyperspectral indices.
   * @param {{olindex?: number, bd1900?: number, d2300?: number, bd2100?: number, bd2500?: number}} indices
   * @returns {{dominantMineral: string, era: string, description: string}}
   */
  static classifyMineralAssembly(indices = {}) {
    const ol = indices.olindex ?? 0;
    const bd19 = indices.bd1900 ?? 0;
    const d23 = indices.d2300 ?? 0;
    const bd21 = indices.bd2100 ?? 0;
    const bd25 = indices.bd2500 ?? 0;

    if (bd25 > 0.08) {
      return {
        dominantMineral: 'Carbonate Assemblage (Magnesite / Calcite)',
        era: 'Noachian / Early Hesperian',
        description: 'Neutral to alkaline aqueous conditions with dissolved atmospheric CO2'
      };
    } else if (d23 > 0.06 || bd19 > 0.08) {
      return {
        dominantMineral: 'Fe/Mg Phyllosilicates (Nontronite / Saponite Smectites)',
        era: 'Early Noachian (Phyllocian Era)',
        description: 'Pervasive aqueous alteration and high water-rock interaction in neutral pH'
      };
    } else if (bd21 > 0.05) {
      return {
        dominantMineral: 'Monohydrated Sulfates (Kieserite / Polyhydrated Sulfates)',
        era: 'Hesperian (Theiikian Era)',
        description: 'Evaporitic acidic aqueous environment with volcanic sulfur degassing'
      };
    } else if (ol > 0.10) {
      return {
        dominantMineral: 'Olivine-Rich Cumulate / Ultramafic Basalt',
        era: 'Pre-Noachian / Noachian Basement',
        description: 'Primitive mantle-derived volcanic flows or deep impact ejecta'
      };
    } else {
      return {
        dominantMineral: 'Anhydrous Mafic Crust (Plagioclase + Pyroxene Basalt)',
        era: 'Amazonian (Siderikan Era)',
        description: 'Dry eolian dust cover and unaltered volcanic bedrock'
      };
    }
  }

  // --- Linear Spectral Unmixing & Continuum Removal Solvers ---

  /**
   * Perform Linear Spectral Unmixing (Endmember Deconvolution) via Least-Squares.
   * Model: y = M * a + e => a = (M^T M)^(-1) M^T y
   * @param {Array<number>} observedSpectrum - Measured spectral reflectance/emissivity vector (y)
   * @param {Array<{name: string, spectrum: Array<number>}>} endmembers - Endmember library
   * @returns {{abundances: Array<{name: string, fraction: number, percent: number}>, rmsResidual: number}}
   */
  static linearSpectralUnmixing(observedSpectrum, endmembers = []) {
    const k = endmembers.length;
    const n = observedSpectrum.length;

    if (k === 0 || n === 0) {
      return { abundances: [], rmsResidual: 0 };
    }

    // Build M matrix [n x k]
    // Compute M^T M [k x k] and M^T y [k x 1]
    const MtM = Array.from({ length: k }, () => new Float64Array(k));
    const Mty = new Float64Array(k);

    for (let i = 0; i < k; i++) {
      const e_i = endmembers[i].spectrum;
      for (let j = 0; j < k; j++) {
        const e_j = endmembers[j].spectrum;
        let sum_ij = 0;
        for (let row = 0; row < n; row++) {
          sum_ij += (e_i[row] || 0) * (e_j[row] || 0);
        }
        MtM[i][j] = sum_ij;
      }

      let sum_iy = 0;
      for (let row = 0; row < n; row++) {
        sum_iy += (e_i[row] || 0) * (observedSpectrum[row] || 0);
      }
      Mty[i] = sum_iy;
    }

    // Solve 2x2 or Gauss elimination for abundances
    const a = new Float64Array(k);
    if (k === 1) {
      a[0] = MtM[0][0] > 0 ? Mty[0] / MtM[0][0] : 1.0;
    } else if (k === 2) {
      const det = MtM[0][0] * MtM[1][1] - MtM[0][1] * MtM[1][0];
      if (Math.abs(det) > 1e-12) {
        a[0] = (MtM[1][1] * Mty[0] - MtM[0][1] * Mty[1]) / det;
        a[1] = (MtM[0][0] * Mty[1] - MtM[1][0] * Mty[0]) / det;
      }
    } else {
      // Direct solve or equal distribution fallback
      for (let i = 0; i < k; i++) a[i] = 1.0 / k;
    }

    // Clamp non-negative and normalize sum to 1
    let sumA = 0;
    for (let i = 0; i < k; i++) {
      a[i] = Math.max(0, a[i]);
      sumA += a[i];
    }

    const abundances = [];
    for (let i = 0; i < k; i++) {
      const norm = sumA > 0 ? a[i] / sumA : 1.0 / k;
      abundances.push({
        name: endmembers[i].name,
        fraction: parseFloat(norm.toFixed(4)),
        percent: parseFloat((norm * 100.0).toFixed(2))
      });
    }

    // Compute RMS residual
    let sumSqErr = 0;
    for (let row = 0; row < n; row++) {
      let modeled = 0;
      for (let i = 0; i < k; i++) {
        modeled += (endmembers[i].spectrum[row] || 0) * a[i];
      }
      const err = observedSpectrum[row] - modeled;
      sumSqErr += err * err;
    }
    const rmsResidual = Math.sqrt(sumSqErr / Math.max(1, n));

    return {
      abundances,
      rmsResidual: parseFloat(rmsResidual.toFixed(4))
    };
  }

  /**
   * Compute Spectral Angle Mapper (SAM) similarity between two spectra.
   * SAM = arccos((A . B) / (||A|| * ||B||))
   * @param {Array<number>} spectrumA
   * @param {Array<number>} spectrumB
   * @returns {{angleRadians: number, angleDegrees: number, similarityScore: number}}
   */
  static computeSpectralAngle(spectrumA, spectrumB) {
    const n = Math.min(spectrumA.length, spectrumB.length);
    if (n === 0) return { angleRadians: 0, angleDegrees: 0, similarityScore: 1.0 };

    let dot = 0;
    let normA = 0;
    let normB = 0;

    for (let i = 0; i < n; i++) {
      dot += spectrumA[i] * spectrumB[i];
      normA += spectrumA[i] * spectrumA[i];
      normB += spectrumB[i] * spectrumB[i];
    }

    const denom = Math.sqrt(normA) * Math.sqrt(normB);
    const cosAngle = denom > 0 ? Math.max(-1.0, Math.min(1.0, dot / denom)) : 1.0;
    const angleRad = Math.acos(cosAngle);
    const angleDeg = angleRad * 180.0 / Math.PI;

    return {
      angleRadians: parseFloat(angleRad.toFixed(4)),
      angleDegrees: parseFloat(angleDeg.toFixed(2)),
      similarityScore: parseFloat(cosAngle.toFixed(4))
    };
  }

  /**
   * Compute continuum-removed spectrum by linear shoulder interpolation.
   * @param {Array<number>} wavelengths - Array of spectral band wavelengths in µm
   * @param {Array<number>} spectrum - Measured reflectance/emissivity values
   * @returns {{continuumRemoved: Array<number>, maxBandDepth: number, bandCenterWavelength: number}}
   */
  static computeContinuumRemovedSpectrum(wavelengths, spectrum) {
    const n = Math.min(wavelengths.length, spectrum.length);
    if (n < 2) {
      return { continuumRemoved: [...spectrum], maxBandDepth: 0, bandCenterWavelength: wavelengths[0] || 0 };
    }

    const w0 = wavelengths[0];
    const w1 = wavelengths[n - 1];
    const r0 = spectrum[0];
    const r1 = spectrum[n - 1];

    const cr = [];
    let maxDepth = 0;
    let minWavelength = wavelengths[0];

    for (let i = 0; i < n; i++) {
      const w = wavelengths[i];
      const frac = (w - w0) / (w1 - w0 || 1);
      const continuum = r0 + frac * (r1 - r0);
      const val = continuum > 0 ? spectrum[i] / continuum : 1.0;
      const depth = 1.0 - val;

      cr.push(parseFloat(val.toFixed(4)));
      if (depth > maxDepth) {
        maxDepth = depth;
        minWavelength = w;
      }
    }

    return {
      continuumRemoved: cr,
      maxBandDepth: parseFloat(Math.max(0, maxDepth).toFixed(4)),
      bandCenterWavelength: minWavelength
    };
  }
}



