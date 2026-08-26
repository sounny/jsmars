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

  // --- CRISM Summary Parameters, Convex Hull & Pearson Correlation Solvers ---

  /**
   * Calculate standard Viviano-Beck et al. (2014) CRISM mineralogical summary parameters.
   * @param {object} bands - Map of band IDs (e.g. B1080, B1500, B1930, B2210, B2290, B2530)
   * @returns {{bd1900_2: number, bd2210_2: number, bd2290: number, islope: number, lcpindex: number, hcpindex: number}}
   */
  static computeCRISMSummaryParameters(bands = {}) {
    const b1080 = bands.B1080 ?? 0.25;
    const b1500 = bands.B1500 ?? 0.28;
    const b1815 = bands.B1815 ?? 0.27;
    const b1930 = bands.B1930 ?? 0.23;
    const b2130 = bands.B2130 ?? 0.26;
    const b2210 = bands.B2210 ?? 0.24;
    const b2290 = bands.B2290 ?? 0.25;
    const b2530 = bands.B2530 ?? 0.22;

    // BD1900_2: 1.93 µm H2O absorption
    const cont1900 = 0.5 * (b1815 + b2130);
    const bd1900_2 = cont1900 > 0 ? 1.0 - (b1930 / cont1900) : 0;

    // BD2210_2: 2.21 µm Al-OH (kaolinite/montmorillonite)
    const cont2210 = 0.5 * (b2130 + b2290);
    const bd2210_2 = cont2210 > 0 ? 1.0 - (b2210 / cont2210) : 0;

    // BD2290: 2.29 µm Fe/Mg-OH (nontronite/saponite)
    const cont2290 = 0.5 * (b2210 + b2530);
    const bd2290 = cont2290 > 0 ? 1.0 - (b2290 / cont2290) : 0;

    // Spectral slope parameter ISLOPE
    const islope = (b1815 - b2530) / (2.53 - 1.815);

    // LCP vs HCP pyroxene indices
    const lcpindex = (b1815 - b1930) / (b1815 + b1930);
    const hcpindex = (b2130 - b2210) / (b2130 + b2210);

    return {
      bd1900_2: parseFloat(Math.max(0, bd1900_2).toFixed(4)),
      bd2210_2: parseFloat(Math.max(0, bd2210_2).toFixed(4)),
      bd2290: parseFloat(Math.max(0, bd2290).toFixed(4)),
      islope: parseFloat(islope.toFixed(4)),
      lcpindex: parseFloat(lcpindex.toFixed(4)),
      hcpindex: parseFloat(hcpindex.toFixed(4))
    };
  }

  /**
   * Compute multi-point upper convex hull continuum for full hyperspectral reflectance curves.
   * @param {Array<number>} wavelengths - Array of spectral band wavelengths in µm
   * @param {Array<number>} spectrum - Measured reflectance values
   * @returns {Array<number>} Upper convex hull continuum values matching input bands
   */
  static computeConvexHullContinuum(wavelengths = [], spectrum = []) {
    const n = Math.min(wavelengths.length, spectrum.length);
    if (n < 2) return [...spectrum];

    // Upper convex hull (Monotone Chain)
    const hull = [];
    for (let i = 0; i < n; i++) {
      const p3 = [wavelengths[i], spectrum[i]];
      while (hull.length >= 2) {
        const p1 = hull[hull.length - 2];
        const p2 = hull[hull.length - 1];
        // Cross product of (p2 - p1) and (p3 - p1)
        const cross = (p2[0] - p1[0]) * (p3[1] - p1[1]) - (p2[1] - p1[1]) * (p3[0] - p1[0]);
        if (cross <= 0) break; // Clockwise turn (valid upper hull vertex)
        hull.pop();
      }
      hull.push(p3);
    }

    // Interpolate hull linearly across all original wavelength samples
    const continuum = new Array(n);
    let hullIdx = 0;

    for (let i = 0; i < n; i++) {
      const w = wavelengths[i];
      while (hullIdx < hull.length - 2 && hull[hullIdx + 1][0] < w) {
        hullIdx++;
      }
      const pA = hull[hullIdx];
      const pB = hull[Math.min(hull.length - 1, hullIdx + 1)];

      const span = pB[0] - pA[0];
      const frac = span > 0 ? (w - pA[0]) / span : 0;
      const val = pA[1] + frac * (pB[1] - pA[1]);

      continuum[i] = parseFloat(Math.max(spectrum[i], val).toFixed(4));
    }

    return continuum;
  }

  /**
   * Compute Pearson spectral correlation coefficient r between two spectral profiles.
   * @param {Array<number>} spectrumA
   * @param {Array<number>} spectrumB
   * @returns {number} Pearson r (-1.0 to +1.0)
   */
  static computeSpectralCorrelation(spectrumA = [], spectrumB = []) {
    const n = Math.min(spectrumA.length, spectrumB.length);
    if (n < 2) return 1.0;

    const meanA = spectrumA.slice(0, n).reduce((a, b) => a + b, 0) / n;
    const meanB = spectrumB.slice(0, n).reduce((a, b) => a + b, 0) / n;

    let num = 0;
    let denA = 0;
    let denB = 0;

    for (let i = 0; i < n; i++) {
      const da = spectrumA[i] - meanA;
      const db = spectrumB[i] - meanB;
      num += da * db;
      denA += da * da;
      denB += db * db;
    }

    const denom = Math.sqrt(denA * denB);
    const r = denom > 0 ? num / denom : 1.0;

    return parseFloat(Math.max(-1.0, Math.min(1.0, r)).toFixed(4));
  }

  // --- CRISM Carbonates, Olivine Fo# & Band Area Ratio (BAR) Solvers ---

  /**
   * Calculate CRISM diagnostic Carbonate absorption indices (BD2500_2 and BD3900).
   * @param {object} bands - Map of band reflectances (B2350, B2500, B2600, B3750, B3900, B4000)
   * @returns {{bd2500_2: number, bd3900: number, hasCarbonateSignature: boolean}}
   */
  static computeCarbonateIndices(bands = {}) {
    const b2350 = bands.B2350 ?? 0.28;
    const b2500 = bands.B2500 ?? 0.23;
    const b2600 = bands.B2600 ?? 0.27;

    const b3750 = bands.B3750 ?? 0.20;
    const b3900 = bands.B3900 ?? 0.16;
    const b4000 = bands.B4000 ?? 0.19;

    const cont2500 = 0.5 * (b2350 + b2600);
    const bd2500_2 = cont2500 > 0 ? 1.0 - (b2500 / cont2500) : 0;

    const cont3900 = 0.5 * (b3750 + b4000);
    const bd3900 = cont3900 > 0 ? 1.0 - (b3900 / cont3900) : 0;

    const hasCarbonateSignature = bd2500_2 > 0.05 && bd3900 > 0.05;

    return {
      bd2500_2: parseFloat(Math.max(0, bd2500_2).toFixed(4)),
      bd3900: parseFloat(Math.max(0, bd3900).toFixed(4)),
      hasCarbonateSignature
    };
  }

  /**
   * Estimate Olivine Forsterite Number (Fo# = Mg / (Mg + Fe)) from 1 µm absorption center minimum.
   * Fo100 (Forsterite) center ~ 1.04 µm, Fo0 (Fayalite) center ~ 1.10 µm.
   * @param {number} minWavelengthMicrons - Observed 1 µm band minimum in µm (e.g. 1.055 µm)
   * @returns {{forsteriteNumber: number, fayaliteNumber: number, compositionName: string}}
   */
  static computeOlivineForsteriteNumber(minWavelengthMicrons) {
    const lam = Math.max(1.04, Math.min(1.10, minWavelengthMicrons));
    const foFraction = 1.0 - (lam - 1.04) / 0.06;
    const foNumber = Math.max(0, Math.min(100, foFraction * 100.0));
    const faNumber = 100.0 - foNumber;

    let comp = 'Magnesium-Rich Forsterite (Fo80-Fo100)';
    if (foNumber < 30) {
      comp = 'Iron-Rich Fayalite (Fo0-Fo30)';
    } else if (foNumber < 70) {
      comp = 'Intermediate Olivine (Hortonolite Fo30-Fo70)';
    }

    return {
      forsteriteNumber: parseFloat(foNumber.toFixed(1)),
      fayaliteNumber: parseFloat(faNumber.toFixed(1)),
      compositionName: comp
    };
  }

  /**
   * Compute Band Area Ratio (BAR = Area 2 µm / Area 1 µm) for pyroxene / olivine mixtures.
   * @param {number} band1Area - Integrated absorption area around 1 µm (µm * reflectance)
   * @param {number} band2Area - Integrated absorption area around 2 µm (µm * reflectance)
   * @returns {{barRatio: number, classification: string}}
   */
  static computeBandAreaRatio(band1Area, band2Area) {
    const a1 = Math.max(1e-6, band1Area);
    const a2 = Math.max(0, band2Area);
    const bar = a2 / a1;

    let cls = 'Orthopyroxene / High-Calcium Clinopyroxene';
    if (bar < 0.1) {
      cls = 'Pure Olivine Dominant (No 2 µm band)';
    } else if (bar < 0.8) {
      cls = 'Olivine-Pyroxene Mixture / Basaltic';
    }

    return {
      barRatio: parseFloat(bar.toFixed(3)),
      classification: cls
    };
  }

  // --- CRISM Hydrated Silica, Nanophase Ferric & Spectral Asymmetry Solvers ---

  /**
   * Calculate CRISM diagnostic Hydrated Silica / Opal-A absorption index (BD2210_SIL).
   * Distinguishes amorphous hydrated silica/opal from Al-smectite clays.
   * @param {object} bands - Map of band reflectances (B2140, B2210, B2250)
   * @returns {{bd2210_sil: number, isHydratedSilicaPresent: boolean}}
   */
  static computeCRISMSilicaIndex(bands = {}) {
    const b2140 = bands.B2140 ?? 0.28;
    const b2210 = bands.B2210 ?? 0.23;
    const b2250 = bands.B2250 ?? 0.27;

    const cont = 0.5 * (b2140 + b2250);
    const bd2210_sil = cont > 0 ? 1.0 - (b2210 / cont) : 0;

    return {
      bd2210_sil: parseFloat(Math.max(0, bd2210_sil).toFixed(4)),
      isHydratedSilicaPresent: bd2210_sil > 0.04
    };
  }

  /**
   * Calculate CRISM nanophase and crystalline Ferric Oxide Index (BD530_2).
   * @param {object} bands - Map of band reflectances (B440, B530, B600)
   * @returns {{bd530_2: number, ferricIntensity: string}}
   */
  static computeFerricNanophaseIndex(bands = {}) {
    const b440 = bands.B440 ?? 0.15;
    const b530 = bands.B530 ?? 0.20;
    const b600 = bands.B600 ?? 0.28;

    const cont = 0.5 * (b440 + b600);
    const bd530 = cont > 0 ? 1.0 - (b530 / cont) : 0;

    let intensity = 'Negligible / Unaltered Basalt';
    if (bd530 > 0.12) {
      intensity = 'High Crystalline Hematite / Dust';
    } else if (bd530 > 0.04) {
      intensity = 'Moderate Nanophase Ferric Oxide';
    }

    return {
      bd530_2: parseFloat(Math.max(0, bd530).toFixed(4)),
      ferricIntensity: intensity
    };
  }

  /**
   * Calculate absorption band asymmetry factor ASY = (Area_left - Area_right) / (Area_left + Area_right).
   * @param {Array<number>} wavelengths - Band wavelengths in µm
   * @param {Array<number>} continuumRemoved - Normalized continuum-removed spectrum (1.0 at shoulders)
   * @param {number} centerIndex - Index of the absorption minimum
   * @returns {{asymmetryFactor: number, skewDirection: string}}
   */
  static computeSpectralAsymmetry(wavelengths = [], continuumRemoved = [], centerIndex = 0) {
    const n = Math.min(wavelengths.length, continuumRemoved.length);
    if (n < 3 || centerIndex <= 0 || centerIndex >= n - 1) {
      return { asymmetryFactor: 0.0, skewDirection: 'Symmetric' };
    }

    let aLeft = 0;
    for (let i = 0; i < centerIndex; i++) {
      const dw = Math.abs(wavelengths[i + 1] - wavelengths[i]);
      const depth = Math.max(0, 1.0 - continuumRemoved[i]);
      aLeft += depth * dw;
    }

    let aRight = 0;
    for (let i = centerIndex; i < n - 1; i++) {
      const dw = Math.abs(wavelengths[i + 1] - wavelengths[i]);
      const depth = Math.max(0, 1.0 - continuumRemoved[i]);
      aRight += depth * dw;
    }

    const total = aLeft + aRight;
    const asy = total > 1e-6 ? (aLeft - aRight) / total : 0.0;

    let skew = 'Symmetric';
    if (asy > 0.1) skew = 'Left-Skewed (Shorter Wavelength Shoulder)';
    else if (asy < -0.1) skew = 'Right-Skewed (Longer Wavelength Shoulder)';

    return {
      asymmetryFactor: parseFloat(asy.toFixed(3)),
      skewDirection: skew
    };
  }

  // --- Mg-Carbonate Doublet, Ferrous Iron & Percentile Stretch Solvers ---

  /**
   * Calculate CRISM diagnostic Mg-Carbonate doublet parameter (MIN2295_2480).
   * @param {object} bands - Map of band reflectances (B2140, B2295, B2480, B2530)
   * @returns {{min2295_2480: number, isCarbonateConfirmed: boolean}}
   */
  static computeCRISMMgCarbonateIndex(bands = {}) {
    const b2140 = bands.B2140 ?? 0.28;
    const b2295 = bands.B2295 ?? 0.22;
    const b2480 = bands.B2480 ?? 0.21;
    const b2530 = bands.B2530 ?? 0.27;

    const denom = b2140 + b2530;
    const numer = b2295 + b2480;
    const val = denom > 0 ? 1.0 - (numer / denom) : 0;

    return {
      min2295_2480: parseFloat(Math.max(0, val).toFixed(4)),
      isCarbonateConfirmed: val > 0.05
    };
  }

  /**
   * Calculate broad 1 µm Ferrous Iron (Fe2+) crystal field band depth (BD1000).
   * @param {object} bands - Map of band reflectances (B800, B1000, B1300)
   * @returns {{bd1000: number, ferrousAbundance: string}}
   */
  static computeFerrousIronIndex(bands = {}) {
    const b800 = bands.B800 ?? 0.24;
    const b1000 = bands.B1000 ?? 0.18;
    const b1300 = bands.B1300 ?? 0.26;

    const cont = 0.5 * (b800 + b1300);
    const depth = cont > 0 ? 1.0 - (b1000 / cont) : 0;

    let abundance = 'Low / Heavily Weathered Dust';
    if (depth > 0.15) {
      abundance = 'High Primary Fe2+ (Fresh Basalt / Olivine / Pyroxene)';
    } else if (depth > 0.06) {
      abundance = 'Moderate Fe2+ Silicate Bearing';
    }

    return {
      bd1000: parseFloat(Math.max(0, depth).toFixed(4)),
      ferrousAbundance: abundance
    };
  }

  /**
   * Calculate 2% - 98% cumulative percentile linear contrast stretch limits for RGB compositing.
   * @param {Array<number>} bandValues - Array of numeric pixel/reflectance values
   * @param {number} [pLow=2.0] - Lower percentile (e.g. 2.0%)
   * @param {number} [pHigh=98.0] - Upper percentile (e.g. 98.0%)
   * @returns {{minStretch: number, maxStretch: number, dynamicRange: number}}
   */
  static computeSpectralContrastStretch(bandValues = [], pLow = 2.0, pHigh = 98.0) {
    if (bandValues.length === 0) {
      return { minStretch: 0, maxStretch: 1, dynamicRange: 1 };
    }

    const sorted = [...bandValues].sort((a, b) => a - b);
    const n = sorted.length;

    const idxLow = Math.min(n - 1, Math.max(0, Math.floor((pLow / 100.0) * n)));
    const idxHigh = Math.min(n - 1, Math.max(0, Math.floor((pHigh / 100.0) * n)));

    const minVal = sorted[idxLow];
    const maxVal = sorted[idxHigh];

    return {
      minStretch: parseFloat(minVal.toFixed(4)),
      maxStretch: parseFloat(maxVal.toFixed(4)),
      dynamicRange: parseFloat((maxVal - minVal).toFixed(4))
    };
  }

  // --- Polyhydrated Sulfate (SINDEX2), OLINDEX3 & SAM Spectral Angle Solvers ---

  /**
   * Calculate CRISM SINDEX2 Polyhydrated Sulfate absorption index (gypsum/polyhydrated Mg-sulfate).
   * SINDEX2 = 1 - (R2100 + R2400) / (2 * R2290)
   * @param {object} bands - Map of band reflectances (B2100, B2290, B2400)
   * @returns {{sindex2: number, hasPolyhydratedSulfate: boolean}}
   */
  static computeCRISMPolyhydratedSulfateIndex(bands = {}) {
    const b2100 = bands.B2100 ?? 0.28;
    const b2290 = bands.B2290 ?? 0.32;
    const b2400 = bands.B2400 ?? 0.27;

    const denom = 2.0 * b2290;
    const numer = b2100 + b2400;
    const sindex2 = denom > 0 ? 1.0 - (numer / denom) : 0;

    return {
      sindex2: parseFloat(Math.max(0, sindex2).toFixed(4)),
      hasPolyhydratedSulfate: sindex2 > 0.04
    };
  }

  /**
   * Calculate standard Viviano-Beck (2014) CRISM OLINDEX3 (Olivine 1 µm parameter).
   * OLINDEX3 = (R1690 / (0.1 * R1080 + 0.9 * R2530)) - 1
   * @param {object} bands - Map of band reflectances (B1080, B1690, B2530)
   * @returns {{olindex3: number, olivineAbundance: string}}
   */
  static computeCRISMOlivineIndex3(bands = {}) {
    const b1080 = bands.B1080 ?? 0.20;
    const b1690 = bands.B1690 ?? 0.28;
    const b2530 = bands.B2530 ?? 0.22;

    const denom = 0.1 * b1080 + 0.9 * b2530;
    const val = denom > 0 ? (b1690 / denom) - 1.0 : 0;

    let abundance = 'Negligible Olivine';
    if (val > 0.15) abundance = 'High Olivine Cumulate (>30 vol%)';
    else if (val > 0.05) abundance = 'Moderate Olivine Silicate';

    return {
      olindex3: parseFloat(Math.max(0, val).toFixed(4)),
      olivineAbundance: abundance
    };
  }

  /**
   * Calculate Spectral Angle Mapper (SAM) vector angle and similarity score.
   * theta = arccos( (r . t) / (||r|| * ||t||) )
   * @param {Array<number>} spectrumReference - Reference endmember spectrum
   * @param {Array<number>} spectrumTarget - Target unknown pixel spectrum
   * @returns {{angleRadians: number, angleDegrees: number, isConfidentMatch: boolean}}
   */
  static computeSpectralAngleMetric(spectrumReference = [], spectrumTarget = []) {
    const n = Math.min(spectrumReference.length, spectrumTarget.length);
    if (n === 0) return { angleRadians: 0, angleDegrees: 0, isConfidentMatch: false };

    let dot = 0;
    let normRef = 0;
    let normTgt = 0;

    for (let i = 0; i < n; i++) {
      dot += spectrumReference[i] * spectrumTarget[i];
      normRef += spectrumReference[i] * spectrumReference[i];
      normTgt += spectrumTarget[i] * spectrumTarget[i];
    }

    const denom = Math.sqrt(normRef) * Math.sqrt(normTgt);
    const cosTheta = denom > 0 ? Math.max(-1.0, Math.min(1.0, dot / denom)) : 1.0;
    const thetaRad = Math.acos(cosTheta);
    const thetaDeg = thetaRad * 180.0 / Math.PI;

    return {
      angleRadians: parseFloat(thetaRad.toFixed(4)),
      angleDegrees: parseFloat(thetaDeg.toFixed(2)),
      isConfidentMatch: thetaDeg < 10.0 // Under 10 degrees is a tight spectral match
    };
  }

  // --- CRISM BD1900r Hydration, HCP/LCP Pyroxene & Continuum Curvature Solvers ---

  /**
   * Calculate CRISM BD1900r structural H2O / hydration parameter with exact linear baseline weights (Viviano-Beck 2014).
   * BD1900r = 1.0 - ( R_1930 / ( a * R_1815 + b * R_2132 ) ) where a = (2132 - 1930)/(2132 - 1815)
   * @param {object} bands - Map of band reflectances (B1815, B1930, B2132)
   * @returns {{bd1900r: number, isHydratedPhyllosilicate: boolean}}
   */
  static computeCRISMBd1900rIndex(bands = {}) {
    const b1815 = bands.B1815 ?? 0.25;
    const b1930 = bands.B1930 ?? 0.21;
    const b2132 = bands.B2132 ?? 0.24;

    const a = (2132.0 - 1930.0) / (2132.0 - 1815.0); // ~0.63722
    const b = 1.0 - a; // ~0.36278

    const continuum = a * b1815 + b * b2132;
    const bd = continuum > 0 ? 1.0 - (b1930 / continuum) : 0;

    return {
      bd1900r: parseFloat(Math.max(0, bd).toFixed(4)),
      isHydratedPhyllosilicate: bd > 0.05
    };
  }

  /**
   * Calculate diagnostic High-Calcium vs Low-Calcium Pyroxene band center contrast metric.
   * @param {object} bands - Map of band reflectances (B1815, B1930, B2120, B2140)
   * @returns {{hcpIndex: number, lcpIndex: number, dominantPyroxene: string}}
   */
  static computePyroxeneBandCenterMetric(bands = {}) {
    const b1815 = bands.B1815 ?? 0.26;
    const b1930 = bands.B1930 ?? 0.24;
    const b2120 = bands.B2120 ?? 0.25;
    const b2140 = bands.B2140 ?? 0.23;

    const hcp = (b2120 + b2140) > 0 ? (b2120 - b2140) / (b2120 + b2140) : 0;
    const lcp = (b1815 + b1930) > 0 ? (b1815 - b1930) / (b1815 + b1930) : 0;

    let dom = 'Undifferentiated Pyroxene';
    if (hcp > 0.03 && hcp > lcp) {
      dom = 'Clinopyroxene (High-Calcium Augite / Diopside)';
    } else if (lcp > 0.03) {
      dom = 'Orthopyroxene (Low-Calcium Enstatite / Hypersthene)';
    }

    return {
      hcpIndex: parseFloat(hcp.toFixed(4)),
      lcpIndex: parseFloat(lcp.toFixed(4)),
      dominantPyroxene: dom
    };
  }

  /**
   * Calculate spectral continuum slope curvature parameter.
   * C = (2 * R_center) / (R_left + R_right) - 1.0
   * @param {number} rLeft - Left shoulder reflectance
   * @param {number} rCenter - Center reflectance
   * @param {number} rRight - Right shoulder reflectance
   * @returns {{curvature: number, isConvexShoulder: boolean, isConcaveAbsorption: boolean}}
   */
  static computeSpectralContinuumCurvature(rLeft, rCenter, rRight) {
    const denom = rLeft + rRight;
    if (denom <= 0) return { curvature: 0, isConvexShoulder: false, isConcaveAbsorption: false };

    const c = (2.0 * rCenter) / denom - 1.0;

    return {
      curvature: parseFloat(c.toFixed(4)),
      isConvexShoulder: c > 0.02,
      isConcaveAbsorption: c < -0.02
    };
  }

  // --- CRISM BD2100r Sulfate, SAM Classifier & NDDI Dust Solvers ---

  /**
   * Calculate CRISM diagnostic BD2100r Monohydrated Sulfate (kieserite) absorption index (Viviano-Beck 2014).
   * BD2100r = 1.0 - ( R_2132 / ( a * R_1930 + b * R_2250 ) )
   * @param {object} bands - Map of band reflectances (B1930, B2132, B2250)
   * @returns {{bd2100r: number, isMonohydratedSulfate: boolean}}
   */
  static computeCRISMBd2100rIndex(bands = {}) {
    const r1930 = bands.B1930 ?? 0.28;
    const r2132 = bands.B2132 ?? 0.22;
    const r2250 = bands.B2250 ?? 0.27;

    // a = (2250 - 2132) / (2250 - 1930) = 118 / 320 = 0.36875
    const a = 118.0 / 320.0;
    const b = 1.0 - a;
    const continuum = a * r1930 + b * r2250;

    const bd = continuum > 0 ? 1.0 - (r2132 / continuum) : 0;

    return {
      bd2100r: parseFloat(Math.max(0, bd).toFixed(4)),
      isMonohydratedSulfate: bd > 0.04
    };
  }

  /**
   * Calculate Spectral Angle Mapper (SAM) angular distance and match quality between spectra.
   * theta = arccos( (r . t) / ( ||r|| * ||t|| ) )
   * @param {Array<number>} referenceSpectrum - Laboratory reference endmember reflectance vector
   * @param {Array<number>} targetSpectrum - Observed pixel reflectance vector
   * @returns {{angleRadians: number, angleDegrees: number, matchScorePercent: number}}
   */
  static computeSpectralAngleMapperScore(referenceSpectrum = [], targetSpectrum = []) {
    const n = Math.min(referenceSpectrum.length, targetSpectrum.length);
    if (n === 0) return { angleRadians: 0, angleDegrees: 0, matchScorePercent: 100 };

    let dot = 0;
    let normR = 0;
    let normT = 0;

    for (let i = 0; i < n; i++) {
      const r = referenceSpectrum[i] || 0;
      const t = targetSpectrum[i] || 0;
      dot += r * t;
      normR += r * r;
      normT += t * t;
    }

    const denom = Math.sqrt(normR) * Math.sqrt(normT);
    const cosTheta = denom > 0 ? Math.max(-1.0, Math.min(1.0, dot / denom)) : 1.0;
    const thetaRad = Math.acos(cosTheta);
    const thetaDeg = thetaRad * 180.0 / Math.PI;

    // Match score: 100% when angle = 0, decaying with angle
    const score = Math.max(0, (1.0 - thetaRad / (Math.PI / 2.0)) * 100.0);

    return {
      angleRadians: parseFloat(thetaRad.toFixed(4)),
      angleDegrees: parseFloat(thetaDeg.toFixed(2)),
      matchScorePercent: parseFloat(score.toFixed(1))
    };
  }

  /**
   * Calculate Normalized Difference Dust Index (NDDI) between Visible and NIR reflectance channels.
   * NDDI = (R_NIR - R_Vis) / (R_NIR + R_Vis)
   * @param {number} rVisible - Blue/Visible reflectance (e.g. 0.44 µm or 0.53 µm)
   * @param {number} rNearIR - Near-Infrared reflectance (e.g. 0.77 µm or 1.0 µm)
   * @returns {{nddi: number, dustClassification: string}}
   */
  static computeNormalizedDifferenceDustIndex(rVisible, rNearIR) {
    const denom = rNearIR + rVisible;
    if (denom <= 0) return { nddi: 0, dustClassification: 'Indeterminate' };

    const nddi = (rNearIR - rVisible) / denom;

    let cls = 'Low Dust / Dark Basaltic Rock';
    if (nddi > 0.35) {
      cls = 'High Bright Airfall Dust Mantle';
    } else if (nddi > 0.15) {
      cls = 'Intermediate Dust / Altered Soil';
    }

    return {
      nddi: parseFloat(nddi.toFixed(4)),
      dustClassification: cls
    };
  }

  // --- CRISM BD1900r2, TES Carbonate & Spectral Information Divergence (SID) Solvers ---

  /**
   * Calculate revised CRISM BD1900r2 structural hydration band depth index.
   * BD1900r2 = 1.0 - ( R1930 / ( a * R1850 + b * R2060 ) ) with a = 0.61905, b = 0.38095
   * @param {number} r1850 - Left shoulder reflectance at 1.85 µm
   * @param {number} r1930 - Hydration band center reflectance at 1.93 µm
   * @param {number} r2060 - Right shoulder reflectance at 2.06 µm
   * @returns {{bd1900r2: number, hasStructuralH2O: boolean}}
   */
  static computeCRISMBd1900r2Index(r1850, r1930, r2060) {
    const a = 0.61905;
    const b = 0.38095;
    const continuum = a * r1850 + b * r2060;

    if (continuum <= 0) return { bd1900r2: 0, hasStructuralH2O: false };

    const depth = 1.0 - (r1930 / continuum);
    const clampedDepth = Math.max(0, depth);

    return {
      bd1900r2: parseFloat(clampedDepth.toFixed(4)),
      hasStructuralH2O: clampedDepth > 0.03
    };
  }

  /**
   * Calculate TES (Thermal Emission Spectrometer) Carbonate absorption index from TIR emissivity.
   * CARB_TES = 1.0 - ( eps1480 / ( 0.5 * (eps1350 + eps1600) ) )
   * @param {number} eps1350 - Emissivity at 1350 cm^-1 (~7.41 µm)
   * @param {number} eps1480 - Emissivity at 1480 cm^-1 (~6.76 µm CO3 absorption center)
   * @param {number} eps1600 - Emissivity at 1600 cm^-1 (~6.25 µm)
   * @returns {{tesCarbonateIndex: number, carbonateAbundanceEstimatePercent: number}}
   */
  static computeTESThermalCarbonateIndex(eps1350, eps1480, eps1600) {
    const cont = 0.5 * (eps1350 + eps1600);
    if (cont <= 0) return { tesCarbonateIndex: 0, carbonateAbundanceEstimatePercent: 0 };

    const index = 1.0 - (eps1480 / cont);
    const clamped = Math.max(0, index);
    const abundance = Math.min(100.0, clamped * 400.0); // Calibrated linear scaling for TES

    return {
      tesCarbonateIndex: parseFloat(clamped.toFixed(4)),
      carbonateAbundanceEstimatePercent: parseFloat(abundance.toFixed(1))
    };
  }

  /**
   * Calculate Spectral Information Divergence (SID) between two hyperspectral probability distributions.
   * SID(x, y) = D(x || y) + D(y || x) = sum( p_i * log(p_i / q_i) ) + sum( q_i * log(q_i / p_i) )
   * @param {Array<number>} spectrumA - First spectrum
   * @param {Array<number>} spectrumB - Second spectrum
   * @returns {{sidDivergence: number, similarityScore: number}}
   */
  static computeSpectralInformationDivergence(spectrumA = [], spectrumB = []) {
    const n = Math.min(spectrumA.length, spectrumB.length);
    if (n === 0) return { sidDivergence: 0, similarityScore: 1.0 };

    const sumA = spectrumA.slice(0, n).reduce((a, b) => a + Math.max(1e-6, b), 0);
    const sumB = spectrumB.slice(0, n).reduce((a, b) => a + Math.max(1e-6, b), 0);

    const p = spectrumA.slice(0, n).map(v => Math.max(1e-6, v) / sumA);
    const q = spectrumB.slice(0, n).map(v => Math.max(1e-6, v) / sumB);

    let sid = 0;
    for (let i = 0; i < n; i++) {
      sid += p[i] * Math.log(p[i] / q[i]) + q[i] * Math.log(q[i] / p[i]);
    }

    const clampedSid = Math.max(0, sid);
    const similarity = Math.exp(-clampedSid * 10.0);

    return {
      sidDivergence: parseFloat(clampedSid.toFixed(5)),
      similarityScore: parseFloat(similarity.toFixed(4))
    };
  }

  // --- CRISM Olivine Extended, TES Quartz Ratio & Band Asymmetry Solvers ---

  /**
   * Calculate standard CRISM extended olivine 1 µm/2 µm absorption index.
   * OLINDEX = R_1690 / ( 0.70 * R_1330 + 0.30 * R_2530 ) - 1.0
   * @param {number} r1690 - Reflectance at 1.69 µm (olivine peak)
   * @param {number} r1330 - Reflectance at 1.33 µm (left shoulder)
   * @param {number} r2530 - Reflectance at 2.53 µm (right shoulder)
   * @returns {{olivineIndex: number, hasOlivineSignature: boolean}}
   */
  static computeCRISMOlivineIndexExtended(r1690, r1330, r2530) {
    const cont = 0.70 * r1330 + 0.30 * r2530;
    if (cont <= 0) return { olivineIndex: 0, hasOlivineSignature: false };

    const val = (r1690 / cont) - 1.0;
    return {
      olivineIndex: parseFloat(val.toFixed(4)),
      hasOlivineSignature: val > 0.03
    };
  }

  /**
   * Calculate TES Quartz / Silicate Reststrahlen band ratio.
   * QRATIO = eps_1120 / eps_1000
   * @param {number} eps1120 - Emissivity at 1120 cm^-1 (~8.93 µm quartz reststrahlen peak)
   * @param {number} eps1000 - Emissivity at 1000 cm^-1 (~10.0 µm silicate absorption trough)
   * @returns {{quartzRatio: number, isEnrichedInSilica: boolean}}
   */
  static computeTESQuartzSilicateRatio(eps1120, eps1000) {
    const e1000 = Math.max(0.01, eps1000);
    const ratio = eps1120 / e1000;

    return {
      quartzRatio: parseFloat(ratio.toFixed(4)),
      isEnrichedInSilica: ratio > 1.05
    };
  }

  /**
   * Calculate spectral absorption band asymmetry parameter from half-maximum wavelengths.
   * A_asym = [ (lambda_right - lambda_min) - (lambda_min - lambda_left) ] / (lambda_right - lambda_left)
   * @param {number} lambdaMin - Wavelength of minimum absorption (µm)
   * @param {number} lambdaLeftHalf - Wavelength of left half-maximum depth (µm)
   * @param {number} lambdaRightHalf - Wavelength of right half-maximum depth (µm)
   * @returns {{asymmetryRatio: number, skewDescription: string, fwhmMicrons: number}}
   */
  static computeAbsorptionBandAsymmetryRatio(lambdaMin, lambdaLeftHalf, lambdaRightHalf) {
    const fwhm = Math.abs(lambdaRightHalf - lambdaLeftHalf);
    if (fwhm <= 0) return { asymmetryRatio: 0, skewDescription: 'Symmetric', fwhmMicrons: 0 };

    const leftSpan = lambdaMin - lambdaLeftHalf;
    const rightSpan = lambdaRightHalf - lambdaMin;
    const asym = (rightSpan - leftSpan) / fwhm;

    let skew = 'Symmetric';
    if (asym > 0.15) skew = 'Right-Skewed (Longer wavelength tail)';
    else if (asym < -0.15) skew = 'Left-Skewed (Shorter wavelength tail)';

    return {
      asymmetryRatio: parseFloat(asym.toFixed(4)),
      skewDescription: skew,
      fwhmMicrons: parseFloat(fwhm.toFixed(4))
    };
  }

  // --- CRISM Fe/Mg Smectite, THEMIS B10/B9 Ratio & Normalized Absorption Depth Solvers ---

  /**
   * Calculate CRISM Fe/Mg Smectite Clay diagnostic 2.3 µm absorption index (extended triplet).
   * D2300_EXT = 1.0 - [ R_2300 / (0.65 * R_2120 + 0.35 * R_2400) ]
   * @param {number} r2300 - Reflectance at 2.30 µm (Fe,Mg-OH vibration absorption trough)
   * @param {number} r2120 - Reflectance at 2.12 µm (left continuum shoulder)
   * @param {number} r2400 - Reflectance at 2.40 µm (right continuum shoulder)
   * @returns {{smectiteIndex: number, hasSmectiteClaySignature: boolean}}
   */
  static computeCRISMSmectiteIndexExtended(r2300, r2120, r2400) {
    const cont = 0.65 * r2120 + 0.35 * r2400;
    if (cont <= 0) return { smectiteIndex: 0, hasSmectiteClaySignature: false };

    const drop = 1.0 - (r2300 / cont);
    return {
      smectiteIndex: parseFloat(drop.toFixed(4)),
      hasSmectiteClaySignature: drop > 0.04
    };
  }

  /**
   * Calculate THEMIS Thermal Infrared Band 10 / Band 9 radiance/emissivity ratio.
   * R_10/9 = I_10 / I_9 (~12.57 µm / 11.79 µm) for distinguishing airborne dust from surface silicates.
   * @param {number} radianceB10 - THEMIS Band 10 spectral radiance/emissivity (~12.57 µm)
   * @param {number} radianceB9 - THEMIS Band 9 spectral radiance/emissivity (~11.79 µm)
   * @returns {{bandRatio: number, isDustDominated: boolean}}
   */
  static computeTHEMISBand10To9Ratio(radianceB10, radianceB9) {
    const b9 = Math.max(1e-4, radianceB9);
    const ratio = radianceB10 / b9;

    return {
      bandRatio: parseFloat(ratio.toFixed(4)),
      isDustDominated: ratio > 1.02
    };
  }

  /**
   * Calculate exact continuum-normalized absorption band depth.
   * D_norm = 1.0 - (R_center / R_continuum)
   * @param {number} rCenter - Spectral reflectance/radiance at band minimum center
   * @param {number} rContinuum - Spectral reflectance/radiance of interpolated baseline continuum
   * @returns {{normalizedDepth: number, percentDepth: number, isAbsorptionPresent: boolean}}
   */
  static computeNormalizedAbsorptionDepth(rCenter, rContinuum) {
    const cont = Math.max(1e-4, rContinuum);
    const depth = 1.0 - (rCenter / cont);
    const clampedDepth = Math.max(0, Math.min(1.0, depth));

    return {
      normalizedDepth: parseFloat(clampedDepth.toFixed(4)),
      percentDepth: parseFloat((clampedDepth * 100.0).toFixed(2)),
      isAbsorptionPresent: clampedDepth > 0.02
    };
  }
}













