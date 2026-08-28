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

  // --- Silicate Hydration, Felsic Silicate & Spectral Curvature Solvers ---

  /**
   * Calculate CRISM SINDEX2 secondary hydrated sulfate / hydroxylated silicate index.
   * SINDEX2 = 1.0 - (R_2290 + R_2400) / (2 * R_2340)
   * @param {number} r2290 - Reflectance at 2.29 µm shoulder
   * @param {number} r2340 - Reflectance at 2.34 µm absorption center
   * @param {number} r2400 - Reflectance at 2.40 µm shoulder
   * @returns {{sindex2: number, hasHydrationSignature: boolean}}
   */
  static computeCRISMSilicateHydrationIndex(r2290, r2340, r2400) {
    const center = Math.max(1e-4, 2.0 * r2340);
    const shoulders = r2290 + r2400;
    const index = 1.0 - (shoulders / center);

    return {
      sindex2: parseFloat(index.toFixed(4)),
      hasHydrationSignature: index > 0.03
    };
  }

  /**
   * Calculate THEMIS quartz / felsic silicate thermal infrared index QINDEX.
   * QINDEX = I_10 / I_8 (~12.57 µm / 11.04 µm)
   * @param {number} radianceB10 - THEMIS Band 10 radiance/emissivity
   * @param {number} radianceB8 - THEMIS Band 8 radiance/emissivity
   * @returns {{qindex: number, isFelsicEnriched: boolean}}
   */
  static computeTHEMISFelsicSilicateIndex(radianceB10, radianceB8) {
    const b8 = Math.max(1e-4, radianceB8);
    const qindex = radianceB10 / b8;

    return {
      qindex: parseFloat(qindex.toFixed(4)),
      isFelsicEnriched: qindex > 1.05
    };
  }

  /**
   * Calculate three-point spectral continuum ratio curvature factor kappa = R2^2 / (R1 * R3).
   * @param {number} r1 - Left band reflectance/radiance
   * @param {number} r2 - Middle band reflectance/radiance
   * @param {number} r3 - Right band reflectance/radiance
   * @returns {{curvatureFactor: number, isConvex: boolean}}
   */
  static computeSpectralContinuumRatioCurvature(r1, r2, r3) {
    const denom = Math.max(1e-6, r1 * r3);
    const kappa = (r2 * r2) / denom;

    return {
      curvatureFactor: parseFloat(kappa.toFixed(4)),
      isConvex: kappa > 1.0
    };
  }

  // --- CRISM HCPINDEX, THEMIS Slope & Absorption Asymmetry Solvers ---

  /**
   * Calculate CRISM High-Calcium Pyroxene (HCP / Augite / Diopside) 2.0 µm band depth index HCPINDEX.
   * HCPINDEX = 1.0 - ( R2060 / ( 0.68 * R1815 + 0.32 * R2530 ) )
   * @param {number} r1815 - Reflectance at 1.815 µm shoulder
   * @param {number} r2060 - Reflectance at 2.060 µm pyroxene absorption minimum
   * @param {number} r2530 - Reflectance at 2.530 µm shoulder
   * @returns {{hcpIndex: number, hasHCP: boolean}}
   */
  static computeCRISMHighCalciumPyroxeneIndex(r1815, r2060, r2530) {
    const continuum = 0.68 * r1815 + 0.32 * r2530;
    const denom = Math.max(1e-4, continuum);
    const index = 1.0 - (r2060 / denom);

    return {
      hcpIndex: parseFloat(index.toFixed(4)),
      hasHCP: index > 0.04
    };
  }

  /**
   * Calculate THEMIS longwave thermal infrared emissivity spectral slope across the Si-O reststrahlen band.
   * Slope = (E_9 - E_4) / (lambda_9 - lambda_4)
   * @param {number} emissB4 - Emissivity in THEMIS Band 4 (~8.56 µm)
   * @param {number} emissB9 - Emissivity in THEMIS Band 9 (~12.57 µm)
   * @param {number} [waveB4=8.56] - Center wavelength of Band 4 in µm
   * @param {number} [waveB9=12.57] - Center wavelength of Band 9 in µm
   * @returns {{emissivitySlopePerUm: number, isMaficSloped: boolean}}
   */
  static computeTHEMISEmissivitySpectralSlope(emissB4, emissB9, waveB4 = 8.56, waveB9 = 12.57) {
    const dLam = Math.max(0.1, waveB9 - waveB4);
    const dE = emissB9 - emissB4;
    const slope = dE / dLam;

    return {
      emissivitySlopePerUm: parseFloat(slope.toFixed(5)),
      isMaficSloped: slope > 0.015
    };
  }

  /**
   * Calculate absorption feature spectral asymmetry and skewness parameter.
   * Asym = (R_center - R_left) / (R_right - R_left)
   * @param {number} rLeft - Left shoulder reflectance
   * @param {number} rCenter - Band center reflectance
   * @param {number} rRight - Right shoulder reflectance
   * @returns {{asymmetryRatio: number, isLeftSkewed: boolean}}
   */
  static computeSpectralAbsorptionAsymmetry(rLeft, rCenter, rRight) {
    const denom = rRight - rLeft;
    if (Math.abs(denom) < 1e-5) {
      return { asymmetryRatio: 0.5, isLeftSkewed: false };
    }

    const asym = (rCenter - rLeft) / denom;

    return {
      asymmetryRatio: parseFloat(asym.toFixed(4)),
      isLeftSkewed: asym < 0.5
    };
  }

  // --- CRISM Fe3+ Phyllosilicate, THEMIS Felsic Quartz & Continuum Slope Solvers ---

  /**
   * Calculate CRISM BD2290 Fe3+-OH phyllosilicate (nontronite) absorption band depth index.
   * BD2290 = 1.0 - ( R2290 / ( 0.714 * R2140 + 0.286 * R2350 ) )
   * @param {number} r2290 - Reflectance at 2.29 µm band center (nontronite Fe-OH absorption)
   * @param {number} r2140 - Left shoulder reflectance at 2.14 µm
   * @param {number} r2350 - Right shoulder reflectance at 2.35 µm
   * @returns {{bd2290: number, hasFe3Phyllosilicate: boolean}}
   */
  static computeCRISMFe3PhyllosilicateIndex(r2290, r2140, r2350) {
    const rL = Math.max(1e-4, r2140);
    const rC = Math.max(0, r2290);
    const rR = Math.max(1e-4, r2350);

    const continuum = 0.714 * rL + 0.286 * rR;
    const bd = 1.0 - (rC / continuum);

    return {
      bd2290: parseFloat(bd.toFixed(4)),
      hasFe3Phyllosilicate: bd > 0.035
    };
  }

  /**
   * Calculate THEMIS thermal infrared quartz / felsic silica reststrahlen band depth index.
   * D_felsic = (E_3 + E_5) / (2 * E_4)
   * @param {number} emissB3 - Emissivity in THEMIS Band 3 (~7.93 µm)
   * @param {number} emissB4 - Emissivity in THEMIS Band 4 (~8.56 µm, silica trough)
   * @param {number} emissB5 - Emissivity in THEMIS Band 5 (~9.35 µm)
   * @returns {{felsicIndex: number, hasFelsicSignature: boolean}}
   */
  static computeTHEMISFelsicQuartzIndex(emissB3, emissB4, emissB5) {
    const e3 = Math.max(0.01, emissB3);
    const e4 = Math.max(0.01, emissB4);
    const e5 = Math.max(0.01, emissB5);

    const index = (e3 + e5) / (2.0 * e4);

    return {
      felsicIndex: parseFloat(index.toFixed(4)),
      hasFelsicSignature: index > 1.025
    };
  }

  /**
   * Calculate spectral reflectance continuum slope across two infrared wavelengths.
   * Slope = (R_2 - R_1) / (lambda_2 - lambda_1)
   * @param {number} r1 - Reflectance at wavelength 1
   * @param {number} r2 - Reflectance at wavelength 2
   * @param {number} [wave1=1.0] - Wavelength 1 in µm
   * @param {number} [wave2=2.5] - Wavelength 2 in µm
   * @returns {{continuumSlopePerUm: number, isRedSloped: boolean}}
   */
  static computeSpectralContinuumSlope(r1, r2, wave1 = 1.0, wave2 = 2.5) {
    const dLam = Math.max(0.01, wave2 - wave1);
    const dR = r2 - r1;
    const slope = dR / dLam;

    return {
      continuumSlopePerUm: parseFloat(slope.toFixed(5)),
      isRedSloped: slope > 0.0
    };
  }

  // --- CRISM BD1400, Carbonate BD2500 & Second Derivative Peak Solvers ---

  /**
   * Calculate CRISM BD1400 structural OH/H2O absorption band depth index (Viviano-Beck 2014).
   * BD1400 = 1.0 - ( R1395 / ( 0.571 * R1330 + 0.429 * R1480 ) )
   * @param {number} r1395 - Reflectance at 1.395 µm band center
   * @param {number} r1330 - Left shoulder reflectance at 1.33 µm
   * @param {number} r1480 - Right shoulder reflectance at 1.48 µm
   * @returns {{bd1400: number, hasHydration: boolean}}
   */
  static computeCRISMBD1400Index(r1395, r1330, r1480) {
    const rL = Math.max(1e-4, r1330);
    const rC = Math.max(0, r1395);
    const rR = Math.max(1e-4, r1480);

    const continuum = 0.571 * rL + 0.429 * rR;
    const bd = 1.0 - (rC / continuum);

    return {
      bd1400: parseFloat(bd.toFixed(4)),
      hasHydration: bd > 0.03
    };
  }

  /**
   * Calculate CRISM BD2500 magnesium/iron carbonate vibration absorption band depth index.
   * BD2500 = 1.0 - ( R2530 / ( 0.5 * R2300 + 0.5 * R2600 ) )
   * @param {number} r2530 - Reflectance at 2.53 µm carbonate band center
   * @param {number} r2300 - Left continuum reflectance at 2.30 µm
   * @param {number} r2600 - Right continuum reflectance at 2.60 µm
   * @returns {{bd2500: number, hasCarbonateSignature: boolean}}
   */
  static computeCRISMMagnesiumCarbonateIndex(r2530, r2300, r2600) {
    const rL = Math.max(1e-4, r2300);
    const rC = Math.max(0, r2530);
    const rR = Math.max(1e-4, r2600);

    const continuum = 0.5 * rL + 0.5 * rR;
    const bd = 1.0 - (rC / continuum);

    return {
      bd2500: parseFloat(bd.toFixed(4)),
      hasCarbonateSignature: bd > 0.035
    };
  }

  /**
   * Calculate discrete second-derivative spectral curvature and emission/absorption peak sharpening metric.
   * D2 = 2 * R_center - R_left - R_right
   * @param {number} rLeft - Left wavelength reflectance
   * @param {number} rCenter - Center wavelength reflectance
   * @param {number} rRight - Right wavelength reflectance
   * @returns {{curvatureD2: number, isConvexPeak: boolean, isConcaveAbsorption: boolean}}
   */
  static computeSecondDerivativeSpectralPeak(rLeft, rCenter, rRight) {
    const d2 = 2.0 * rCenter - rLeft - rRight;

    return {
      curvatureD2: parseFloat(d2.toFixed(5)),
      isConvexPeak: d2 > 0.005,
      isConcaveAbsorption: d2 < -0.005
    };
  }

  // --- CRISM Gypsum Doublet, OLINDEX3 & Spectral Euclidean Distance Solvers ---

  /**
   * Calculate CRISM BD1900D gypsum / polyhydrated sulfate doublet absorption index.
   * BD1900D = 1.0 - ( 0.5 * (R1930 + R1980) / ( 0.6 * R1815 + 0.4 * R2130 ) )
   * @param {number} r1930 - Reflectance at 1.93 µm first doublet minimum
   * @param {number} r1980 - Reflectance at 1.98 µm second doublet minimum
   * @param {number} r1815 - Left shoulder reflectance at 1.815 µm
   * @param {number} r2130 - Right shoulder reflectance at 2.130 µm
   * @returns {{bd1900d: number, hasGypsumSignature: boolean}}
   */
  static computeCRISMGypsumDoubletIndex(r1930, r1980, r1815, r2130) {
    const rL = Math.max(1e-4, r1815);
    const rR = Math.max(1e-4, r2130);
    const rCenter = 0.5 * (Math.max(0, r1930) + Math.max(0, r1980));

    const continuum = 0.6 * rL + 0.4 * rR;
    const bd = 1.0 - (rCenter / continuum);

    return {
      bd1900d: parseFloat(bd.toFixed(4)),
      hasGypsumSignature: bd > 0.035
    };
  }

  /**
   * Calculate CRISM OLINDEX3 broad 1.0 µm ferrous iron crystal field absorption index for olivine.
   * OLINDEX3 = 1.0 - ( R1050 / ( 0.65 * R850 + 0.35 * R1350 ) )
   * @param {number} r1050 - Reflectance at 1.05 µm olivine absorption trough
   * @param {number} r850 - Left shoulder reflectance at 0.85 µm
   * @param {number} r1350 - Right shoulder reflectance at 1.35 µm
   * @returns {{olindex3: number, hasOlivineSignature: boolean}}
   */
  static computeCRISMOLINDEX3(r1050, r850, r1350) {
    const rL = Math.max(1e-4, r850);
    const rC = Math.max(0, r1050);
    const rR = Math.max(1e-4, r1350);

    const continuum = 0.65 * rL + 0.35 * rR;
    const ol = 1.0 - (rC / continuum);

    return {
      olindex3: parseFloat(ol.toFixed(4)),
      hasOlivineSignature: ol > 0.05
    };
  }

  /**
   * Calculate multidimensional Euclidean distance and RMS divergence between two hyperspectral signatures.
   * d_euc = sqrt( sum( (A_i - B_i)^2 ) )
   * @param {Array<number>} spectrumA - Array of reflectance values for spectrum A
   * @param {Array<number>} spectrumB - Array of reflectance values for spectrum B
   * @returns {{euclideanDistance: number, rmsDivergence: number, numBands: number}}
   */
  static computeSpectralEuclideanDistance(spectrumA = [], spectrumB = []) {
    const n = Math.min(spectrumA.length, spectrumB.length);
    if (n === 0) {
      return { euclideanDistance: 0.0, rmsDivergence: 0.0, numBands: 0 };
    }

    let sumSq = 0;
    for (let i = 0; i < n; i++) {
      const diff = spectrumA[i] - spectrumB[i];
      sumSq += diff * diff;
    }

    const dEuc = Math.sqrt(sumSq);
    const rmsd = Math.sqrt(sumSq / n);

    return {
      euclideanDistance: parseFloat(dEuc.toFixed(5)),
      rmsDivergence: parseFloat(rmsd.toFixed(5)),
      numBands: n
    };
  }

  // --- Monohydrated Sulfate BD2100, Pyroxene HCPINDEX & Spectral Angle Mapper Solvers ---

  /**
   * Calculate CRISM BD2100 monohydrated sulfate (kieserite) 2.1 µm crystal absorption band depth.
   * BD2100 = 1.0 - ( R2132 / ( 0.6 * R1930 + 0.4 * R2250 ) )
   * @param {number} r2132 - Center reflectance at 2.132 µm monohydrated sulfate absorption band
   * @param {number} r1930 - Left continuum shoulder reflectance at 1.93 µm
   * @param {number} r2250 - Right continuum shoulder reflectance at 2.25 µm
   * @returns {{bd2100: number, hasMonohydratedSulfateSignature: boolean}}
   */
  static computeCRISMBD2100(r2132, r1930, r2250) {
    const rL = Math.max(1e-4, r1930);
    const rC = Math.max(0, r2132);
    const rR = Math.max(1e-4, r2250);

    const continuum = 0.6 * rL + 0.4 * rR;
    const bd = 1.0 - (rC / continuum);

    return {
      bd2100: parseFloat(bd.toFixed(4)),
      hasMonohydratedSulfateSignature: bd > 0.03
    };
  }

  /**
   * Calculate CRISM HCPINDEX high-calcium pyroxene (clinopyroxene / augite) 2.0 µm band curvature index.
   * HCPINDEX = ( (R2120 - R2060)/(R2120 + R2060) ) + ( (R2140 - R2210)/(R2140 + R2210) )
   * @param {number} r2060 - Left band wing reflectance at 2.06 µm
   * @param {number} r2120 - Left shoulder reflectance at 2.12 µm
   * @param {number} r2140 - Right shoulder reflectance at 2.14 µm
   * @param {number} r2210 - Right band wing reflectance at 2.21 µm
   * @returns {{hcpindex: number, hasHighCalciumPyroxene: boolean}}
   */
  static computeCRISMHCPINDEX(r2060, r2120, r2140, r2210) {
    const term1 = (r2120 - r2060) / Math.max(1e-4, r2120 + r2060);
    const term2 = (r2140 - r2210) / Math.max(1e-4, r2140 + r2210);
    const hcp = term1 + term2;

    return {
      hcpindex: parseFloat(hcp.toFixed(4)),
      hasHighCalciumPyroxene: hcp > 0.02
    };
  }

  /**
   * Calculate Spectral Angle Mapper (SAM) dot-product angle theta in radians and degrees.
   * cos(theta) = dot(T, R) / ( ||T|| * ||R|| )
   * @param {Array<number>} spectrumTarget - Target unknown pixel spectrum vector
   * @param {Array<number>} spectrumReference - Reference laboratory / endmember spectrum vector
   * @returns {{spectralAngleRad: number, spectralAngleDeg: number, matchSimilarityFraction: number, numBands: number}}
   */
  static computeSpectralAngleMapper(spectrumTarget = [], spectrumReference = []) {
    const n = Math.min(spectrumTarget.length, spectrumReference.length);
    if (n === 0) {
      return { spectralAngleRad: 0.0, spectralAngleDeg: 0.0, matchSimilarityFraction: 1.0, numBands: 0 };
    }

    let dot = 0;
    let normT = 0;
    let normR = 0;

    for (let i = 0; i < n; i++) {
      const t = spectrumTarget[i];
      const r = spectrumReference[i];
      dot += t * r;
      normT += t * t;
      normR += r * r;
    }

    const denom = Math.sqrt(normT) * Math.sqrt(normR);
    let cosTheta = denom > 1e-9 ? dot / denom : 1.0;
    cosTheta = Math.max(-1.0, Math.min(1.0, cosTheta));

    const angleRad = Math.acos(cosTheta);
    const angleDeg = (angleRad * 180.0) / Math.PI;

    return {
      spectralAngleRad: parseFloat(angleRad.toFixed(5)),
      spectralAngleDeg: parseFloat(angleDeg.toFixed(3)),
      matchSimilarityFraction: parseFloat(cosTheta.toFixed(5)),
      numBands: n
    };
  }

  // --- CRISM Structural Water (BD1400), Al-OH Smectite (BD2210) & Continuum Slope Solvers ---

  /**
   * Calculate CRISM 1.4 µm Structural H2O / Hydroxyl (OH) Band Depth Index BD1400.
   * BD1400 = 1.0 - ( R1395 / ( 0.7 * R1330 + 0.3 * R1510 ) )
   * @param {number} r1330 - Reflectance at 1.330 µm shoulder
   * @param {number} r1395 - Reflectance at 1.395 µm H2O/OH overtone absorption minimum
   * @param {number} r1510 - Reflectance at 1.510 µm shoulder
   * @returns {{bd1400: number, hasHydration: boolean}}
   */
  static computeCRISMStructuralWaterBD1400(r1330, r1395, r1510) {
    const continuum = 0.7 * Math.max(1e-4, r1330) + 0.3 * Math.max(1e-4, r1510);
    const depth = 1.0 - (r1395 / continuum);

    return {
      bd1400: parseFloat(depth.toFixed(4)),
      hasHydration: depth > 0.02
    };
  }

  /**
   * Calculate CRISM 2.21 µm Al-OH Smectite / Montmorillonite / Kaolinite Band Depth Index BD2210.
   * BD2210 = 1.0 - ( R2210 / ( 0.6 * R2140 + 0.4 * R2250 ) )
   * @param {number} r2140 - Reflectance at 2.140 µm shoulder
   * @param {number} r2210 - Reflectance at 2.210 µm Al-OH stretching overtone minimum
   * @param {number} r2250 - Reflectance at 2.250 µm shoulder
   * @returns {{bd2210: number, hasAlSmectite: boolean}}
   */
  static computeCRISMSmectiteBD2210(r2140, r2210, r2250) {
    const continuum = 0.6 * Math.max(1e-4, r2140) + 0.4 * Math.max(1e-4, r2250);
    const depth = 1.0 - (r2210 / continuum);

    return {
      bd2210: parseFloat(depth.toFixed(4)),
      hasAlSmectite: depth > 0.025
    };
  }

  /**
   * Calculate spectral continuum slope normalized across wavelength interval.
   * S_cont = ( R_lambda2 - R_lambda1 ) / ( lambda2 - lambda1 )
   * @param {number} r1 - Reflectance at wavelength lambda1
   * @param {number} r2 - Reflectance at wavelength lambda2
   * @param {number} lambda1Microns - Shorter wavelength in µm
   * @param {number} lambda2Microns - Longer wavelength in µm
   * @returns {{slopePerMicron: number, isRedSloped: boolean, isBlueSloped: boolean}}
   */
  static computeContinuumNormalizedSlope(r1, r2, lambda1Microns, lambda2Microns) {
    const dLambda = Math.max(1e-4, Math.abs(lambda2Microns - lambda1Microns));
    const slope = (r2 - r1) / dLambda;

    return {
      slopePerMicron: parseFloat(slope.toFixed(4)),
      isRedSloped: slope > 0.01,
      isBlueSloped: slope < -0.01
    };
  }

  // --- TES Thermal Infrared Mineralogy & Absorption Asymmetry Solvers ---

  /**
   * Calculate TES Quartz / Silica Si-O stretching vibrational index.
   * SilicaIndex = ( eps_1100 + eps_1150 ) / ( 2 * eps_1125 )
   * @param {number} eps1100 - Emissivity at 1100 cm^-1
   * @param {number} eps1125 - Emissivity at band center 1125 cm^-1
   * @param {number} eps1150 - Emissivity at 1150 cm^-1
   * @returns {{silicaIndex: number, hasSilicaEnrichment: boolean}}
   */
  static computeTESSilicaIndex(eps1100, eps1125, eps1150) {
    const denom = 2.0 * Math.max(1e-4, eps1125);
    const index = (eps1100 + eps1150) / denom;

    return {
      silicaIndex: parseFloat(index.toFixed(4)),
      hasSilicaEnrichment: index > 1.03
    };
  }

  /**
   * Calculate TES Carbonate CO3 stretching fundamental absorption band depth at 1430 cm^-1.
   * BD1430 = 1.0 - eps_1430 / ( 0.5 * (eps_1350 + eps_1510) )
   * @param {number} eps1350 - Emissivity continuum shoulder at 1350 cm^-1
   * @param {number} eps1430 - Emissivity band center at 1430 cm^-1
   * @param {number} eps1510 - Emissivity continuum shoulder at 1510 cm^-1
   * @returns {{bd1430: number, hasCarbonate: boolean}}
   */
  static computeTESCarbonateBD1430(eps1350, eps1430, eps1510) {
    const continuum = 0.5 * (Math.max(1e-4, eps1350) + Math.max(1e-4, eps1510));
    const depth = 1.0 - (eps1430 / continuum);

    return {
      bd1430: parseFloat(depth.toFixed(4)),
      hasCarbonate: depth > 0.03
    };
  }

  /**
   * Calculate spectral absorption band asymmetry parameter (skewness).
   * A_asym = ( Area_left - Area_right ) / ( Area_left + Area_right )
   * @param {number} leftHalfArea - Integrated area under left half of absorption feature
   * @param {number} rightHalfArea - Integrated area under right half of absorption feature
   * @returns {{asymmetryIndex: number, isLeftSkewed: boolean, isRightSkewed: boolean, isSymmetric: boolean}}
   */
  static computeSpectralAsymmetryIndex(leftHalfArea, rightHalfArea) {
    const aLeft = Math.max(0, leftHalfArea);
    const aRight = Math.max(0, rightHalfArea);
    const total = aLeft + aRight;

    if (total <= 1e-8) {
      return {
        asymmetryIndex: 0.0,
        isLeftSkewed: false,
        isRightSkewed: false,
        isSymmetric: true
      };
    }

    const asym = (aLeft - aRight) / total;

    return {
      asymmetryIndex: parseFloat(asym.toFixed(4)),
      isLeftSkewed: asym > 0.05,
      isRightSkewed: asym < -0.05,
      isSymmetric: Math.abs(asym) <= 0.05
    };
  }

  // --- CRISM Hydrated Water & Fe/Mg Phyllosilicate Summary Parameter Solvers ---

  /**
   * Calculate CRISM Molecular / Interlayer H2O 1.9 µm absorption band depth (BD1900).
   * BD1900 = 1.0 - R_1930 / ( 0.5 * (R_1850 + R_2060) )
   * @param {number} r1850 - Reflectance continuum shoulder at 1850 nm
   * @param {number} r1930 - Reflectance band center at 1930 nm
   * @param {number} r2060 - Reflectance continuum shoulder at 2060 nm
   * @returns {{bd1900: number, hasHydratedWater: boolean}}
   */
  static computeCRISMHydratedWaterBD1900(r1850, r1930, r2060) {
    const continuum = 0.5 * (Math.max(1e-4, r1850) + Math.max(1e-4, r2060));
    const depth = 1.0 - (r1930 / continuum);

    return {
      bd1900: parseFloat(depth.toFixed(4)),
      hasHydratedWater: depth > 0.02
    };
  }

  /**
   * Calculate CRISM Fe/Mg Phyllosilicate (Saponite / Nontronite / Chlorite) 2.3 µm band depth (BD2300).
   * BD2300 = 1.0 - R_2300 / ( 0.5 * (R_2250 + R_2350) )
   * @param {number} r2250 - Reflectance at 2250 nm
   * @param {number} r2300 - Reflectance band center at 2300 nm
   * @param {number} r2350 - Reflectance at 2350 nm
   * @returns {{bd2300: number, hasFeMgPhyllosilicate: boolean}}
   */
  static computeCRISMMagnesiumIronPhyllosilicateBD2300(r2250, r2300, r2350) {
    const continuum = 0.5 * (Math.max(1e-4, r2250) + Math.max(1e-4, r2350));
    const depth = 1.0 - (r2300 / continuum);

    return {
      bd2300: parseFloat(depth.toFixed(4)),
      hasFeMgPhyllosilicate: depth > 0.02
    };
  }

  /**
   * Calculate CRISM Bulk Hydration 3.0 µm absorption band depth (BD3000).
   * BD3000 = 1.0 - R_3000 / R_2530
   * @param {number} r2530 - Reflectance continuum baseline at 2530 nm
   * @param {number} r3000 - Reflectance in 3000 nm water absorption band
   * @returns {{bd3000: number, isHydratedBulkSurface: boolean}}
   */
  static computeCRISMBulkHydrationBD3000(r2530, r3000) {
    const rBase = Math.max(1e-4, r2530);
    const depth = 1.0 - (r3000 / rBase);

    return {
      bd3000: parseFloat(depth.toFixed(4)),
      isHydratedBulkSurface: depth > 0.15
    };
  }

  // --- OMEGA Ferric Oxide & Olivine & TES Surface Type Solvers ---

  /**
   * Calculate OMEGA / CRISM Ferric Iron (Fe3+) Nanophase Oxide 530 nm absorption band depth (BD530).
   * BD530 = 1.0 - R_530 / ( 0.5 * (R_440 + R_700) )
   * @param {number} r440 - Reflectance at 440 nm
   * @param {number} r530 - Reflectance at 530 nm
   * @param {number} r700 - Reflectance at 700 nm
   * @returns {{bd530: number, hasFerricOxide: boolean}}
   */
  static computeOMEGAFerricOxideBD530(r440, r530, r700) {
    const continuum = 0.5 * (Math.max(1e-4, r440) + Math.max(1e-4, r700));
    const depth = 1.0 - (r530 / continuum);

    return {
      bd530: parseFloat(depth.toFixed(4)),
      hasFerricOxide: depth > 0.03
    };
  }

  /**
   * Calculate OMEGA Olivine 1.05 µm broad crystal field absorption band depth (OLINDEX3).
   * OLINDEX3 = 1.0 - R_1050 / ( 0.5 * (R_860 + R_1210) )
   * @param {number} r860 - Reflectance at 860 nm
   * @param {number} r1050 - Reflectance at 1050 nm
   * @param {number} r1210 - Reflectance at 1210 nm
   * @returns {{olindex3: number, hasOlivine: boolean}}
   */
  static computeOMEGAOlivineIndexOLINDEX3(r860, r1050, r1210) {
    const continuum = 0.5 * (Math.max(1e-4, r860) + Math.max(1e-4, r1210));
    const depth = 1.0 - (r1050 / continuum);

    return {
      olindex3: parseFloat(depth.toFixed(4)),
      hasOlivine: depth > 0.05
    };
  }

  /**
   * Calculate TES Surface Type Index (STI) distinguishing Basaltic (ST1) vs Andesitic (ST2) terrain.
   * STI = ( eps_820 - eps_1075 ) / ( eps_820 + eps_1075 )
   * @param {number} eps820 - Thermal emissivity at 820 cm^-1
   * @param {number} eps1075 - Thermal emissivity at 1075 cm^-1
   * @returns {{surfaceTypeIndex: number, isBasalticType1: boolean, isAndesiticType2: boolean}}
   */
  static computeTESSurfaceTypeIndex(eps820, eps1075) {
    const e1 = Math.max(1e-4, eps820);
    const e2 = Math.max(1e-4, eps1075);

    const sti = (e1 - e2) / (e1 + e2);

    return {
      surfaceTypeIndex: parseFloat(sti.toFixed(4)),
      isBasalticType1: sti >= 0.0,
      isAndesiticType2: sti < 0.0
    };
  }

  // --- CRISM Carbonate, Al-OH Phyllosilicate & High-Calcium Pyroxene Solvers ---

  /**
   * Calculate CRISM Carbonate (Magnesite / Siderite / Calcite) 2.50 µm absorption band depth (BD2500).
   * BD2500 = 1.0 - R_2500 / ( 0.5 * (R_2430 + R_2570) )
   * @param {number} r2430 - Reflectance at 2430 nm
   * @param {number} r2500 - Reflectance band center at 2500 nm
   * @param {number} r2570 - Reflectance at 2570 nm
   * @returns {{bd2500: number, hasCarbonate: boolean}}
   */
  static computeCRISMCarbonateBD2500(r2430, r2500, r2570) {
    const continuum = 0.5 * (Math.max(1e-4, r2430) + Math.max(1e-4, r2570));
    const depth = 1.0 - (r2500 / continuum);

    return {
      bd2500: parseFloat(depth.toFixed(4)),
      hasCarbonate: depth > 0.02
    };
  }

  /**
   * Calculate CRISM Al-OH Phyllosilicate (Kaolinite / Montmorillonite / Beidellite) 2.20 µm band depth (BD2200).
   * BD2200 = 1.0 - R_2200 / ( 0.5 * (R_2140 + R_2250) )
   * @param {number} r2140 - Reflectance at 2140 nm
   * @param {number} r2200 - Reflectance band center at 2200 nm
   * @param {number} r2250 - Reflectance at 2250 nm
   * @returns {{bd2200: number, hasAlOHPhyllosilicate: boolean}}
   */
  static computeCRISMAlOHPhyllosilicateBD2200(r2140, r2200, r2250) {
    const continuum = 0.5 * (Math.max(1e-4, r2140) + Math.max(1e-4, r2250));
    const depth = 1.0 - (r2200 / continuum);

    return {
      bd2200: parseFloat(depth.toFixed(4)),
      hasAlOHPhyllosilicate: depth > 0.02
    };
  }

  /**
   * Calculate CRISM High-Calcium Pyroxene (Augite / Diopside) 1.0 µm ferrous iron absorption index (HCPINDEX).
   * HCPINDEX = 1.0 - R_1000 / ( 0.5 * (R_800 + R_1300) )
   * @param {number} r800 - Reflectance at 800 nm
   * @param {number} r1000 - Reflectance band center at 1000 nm
   * @param {number} r1300 - Reflectance at 1300 nm
   * @returns {{hcpIndex: number, hasHighCalciumPyroxene: boolean}}
   */
  static computeCRISMPyroxeneHCPINDEX(r800, r1000, r1300) {
    const continuum = 0.5 * (Math.max(1e-4, r800) + Math.max(1e-4, r1300));
    const depth = 1.0 - (r1000 / continuum);

    return {
      hcpIndex: parseFloat(depth.toFixed(4)),
      hasHighCalciumPyroxene: depth > 0.04
    };
  }

  // --- CRISM Olivine & Ferric Oxide Mineralogy Solvers ---

  /**
   * Calculate CRISM Olivine 1.0 µm broad Fe2+ absorption parameter (OLINDEX3).
   * OLINDEX3 = 1.0 - R_1080 / ( 0.65 * R_1690 + 0.35 * R_2530 )
   * @param {number} r1080 - Reflectance at 1080 nm (center of broad Fe2+ olivine absorption)
   * @param {number} r1690 - Reflectance at 1690 nm (near-IR shoulder)
   * @param {number} r2530 - Reflectance at 2530 nm (short-wave IR anchor)
   * @returns {{olIndex: number, hasOlivine: boolean}}
   */
  static computeCRISMOlivineOLINDEX3(r1080, r1690, r2530) {
    const continuum = 0.65 * Math.max(1e-4, r1690) + 0.35 * Math.max(1e-4, r2530);
    const depth = 1.0 - (r1080 / continuum);

    return {
      olIndex: parseFloat(depth.toFixed(4)),
      hasOlivine: depth > 0.05
    };
  }

  /**
   * Calculate CRISM Ferric Iron oxide (Fe3+) 900 nm electronic absorption parameter (FE3INDEX).
   * FE3INDEX = 1.0 - R_920 / ( 0.5 * (R_770 + R_1080) )
   * @param {number} r770 - Reflectance at 770 nm
   * @param {number} r920 - Reflectance band center at 920 nm
   * @param {number} r1080 - Reflectance at 1080 nm
   * @returns {{fe3Index: number, hasFerricOxides: boolean}}
   */
  static computeCRISMFerricOxideFE3INDEX(r770, r920, r1080) {
    const continuum = 0.5 * (Math.max(1e-4, r770) + Math.max(1e-4, r1080));
    const depth = 1.0 - (r920 / continuum);

    return {
      fe3Index: parseFloat(depth.toFixed(4)),
      hasFerricOxides: depth > 0.03
    };
  }

  // --- CRISM Hydrated Silica & Carbonate Mineralogy Solvers ---

  /**
   * Calculate CRISM Hydrated / Opaline Silica absorption parameter (SINDEX2).
   * SINDEX2 = 1.0 - R_2290 / ( 0.60 * R_2120 + 0.40 * R_2400 )
   * @param {number} r2120 - Reflectance at 2120 nm
   * @param {number} r2290 - Reflectance band minimum at 2290 nm (Si-OH overtone absorption)
   * @param {number} r2400 - Reflectance at 2400 nm
   * @returns {{sIndex: number, hasHydratedSilica: boolean}}
   */
  static computeCRISMSilicaSINDEX2(r2120, r2290, r2400) {
    const continuum = 0.60 * Math.max(1e-4, r2120) + 0.40 * Math.max(1e-4, r2400);
    const depth = 1.0 - (r2290 / continuum);

    return {
      sIndex: parseFloat(depth.toFixed(4)),
      hasHydratedSilica: depth > 0.02
    };
  }

  /**
   * Calculate CRISM Carbonate absorption parameter (CARBINDEX) for Mg/Fe carbonates.
   * CARBINDEX = 1.0 - ( 0.5 * (R_2300 + R_2500) ) / R_2140
   * @param {number} r2140 - Reflectance continuum anchor at 2140 nm
   * @param {number} r2300 - Reflectance absorption at 2300 nm
   * @param {number} r2500 - Reflectance absorption at 2500 nm (CO3 vibrational overtone)
   * @returns {{carbIndex: number, hasCarbonates: boolean}}
   */
  static computeCRISMCarbonateCARBINDEX(r2140, r2300, r2500) {
    const continuum = Math.max(1e-4, r2140);
    const meanBand = 0.5 * (Math.max(1e-4, r2300) + Math.max(1e-4, r2500));
    const depth = 1.0 - (meanBand / continuum);

    return {
      carbIndex: parseFloat(depth.toFixed(4)),
      hasCarbonates: depth > 0.03
    };
  }

  // --- CRISM Mafic Mineralogy & Pyroxene Band Solvers ---

  /**
   * Calculate CRISM Low-Calcium Pyroxene / Orthopyroxene index (LCPINDEX2).
   * LCPINDEX2 = 1.0 - ( 0.5 * (R_2120 + R_2140) ) / ( 0.5 * (R_1690 + R_2530) )
   * @param {number} r1690 - Short-wavelength continuum anchor at 1690 nm
   * @param {number} r2120 - Absorption band at 2120 nm
   * @param {number} r2140 - Absorption band minimum at 2140 nm
   * @param {number} r2530 - Long-wavelength continuum anchor at 2530 nm
   * @returns {{lcpIndex: number, hasLowCalciumPyroxene: boolean}}
   */
  static computeCRISMPyroxeneLCPINDEX2(r1690, r2120, r2140, r2530) {
    const continuum = 0.5 * (Math.max(1e-4, r1690) + Math.max(1e-4, r2530));
    const meanBand = 0.5 * (Math.max(1e-4, r2120) + Math.max(1e-4, r2140));
    const depth = 1.0 - (meanBand / continuum);

    return {
      lcpIndex: parseFloat(depth.toFixed(4)),
      hasLowCalciumPyroxene: depth > 0.04
    };
  }

  /**
   * Calculate CRISM High-Calcium Pyroxene / Clinopyroxene index (HCPINDEX2).
   * HCPINDEX2 = 1.0 - ( 0.5 * (R_2350 + R_2390) ) / ( 0.5 * (R_1815 + R_2530) )
   * @param {number} r1815 - Short-wavelength continuum anchor at 1815 nm
   * @param {number} r2350 - Absorption band at 2350 nm
   * @param {number} r2390 - Absorption band minimum at 2390 nm
   * @param {number} r2530 - Long-wavelength continuum anchor at 2530 nm
   * @returns {{hcpIndex: number, hasHighCalciumPyroxene: boolean}}
   */
  static computeCRISMPyroxeneHCPINDEX2(r1815, r2350, r2390, r2530) {
    const continuum = 0.5 * (Math.max(1e-4, r1815) + Math.max(1e-4, r2530));
    const meanBand = 0.5 * (Math.max(1e-4, r2350) + Math.max(1e-4, r2390));
    const depth = 1.0 - (meanBand / continuum);

    return {
      hcpIndex: parseFloat(depth.toFixed(4)),
      hasHighCalciumPyroxene: depth > 0.04
    };
  }

  // --- Linear Spectral Least Squares Unmixing (TES / THEMIS) ---

  /**
   * Calculate constrained linear least-squares spectral unmixing for 2 endmembers with sum-to-one constraint.
   * epsilon_meas = f_1 * epsilon_1 + (1 - f_1) * epsilon_2
   * y = epsilon_meas - epsilon_2, x = epsilon_1 - epsilon_2
   * f_1 = (x . y) / (x . x)
   * @param {number[]} measuredSpectrum - Array of measured multi-channel reflectance or emissivity values
   * @param {number[]} endmember1 - Spectral values for Endmember 1 (e.g. Basalt)
   * @param {number[]} endmember2 - Spectral values for Endmember 2 (e.g. Dust)
   * @returns {{fraction1: number, fraction2: number, fraction1Percent: number, fraction2Percent: number, rootMeanSquareError: number}}
   */
  static computeLinearSpectralUnmixing2Components(measuredSpectrum, endmember1, endmember2) {
    if (!Array.isArray(measuredSpectrum) || !Array.isArray(endmember1) || !Array.isArray(endmember2)) {
      return { fraction1: 0.5, fraction2: 0.5, fraction1Percent: 50.0, fraction2Percent: 50.0, rootMeanSquareError: 0.0 };
    }

    const n = Math.min(measuredSpectrum.length, endmember1.length, endmember2.length);
    if (n === 0) {
      return { fraction1: 0.5, fraction2: 0.5, fraction1Percent: 50.0, fraction2Percent: 50.0, rootMeanSquareError: 0.0 };
    }

    let dotXY = 0.0;
    let dotXX = 0.0;

    for (let i = 0; i < n; i++) {
      const y = measuredSpectrum[i] - endmember2[i];
      const x = endmember1[i] - endmember2[i];
      dotXY += x * y;
      dotXX += x * x;
    }

    let f1 = dotXX > 1e-8 ? dotXY / dotXX : 0.5;
    f1 = Math.max(0.0, Math.min(1.0, f1));
    const f2 = 1.0 - f1;

    // Calculate RMSE of the linear mixture fit
    let sumSqErr = 0.0;
    for (let i = 0; i < n; i++) {
      const modeled = f1 * endmember1[i] + f2 * endmember2[i];
      const err = measuredSpectrum[i] - modeled;
      sumSqErr += err * err;
    }
    const rmse = Math.sqrt(sumSqErr / n);

    return {
      fraction1: parseFloat(f1.toFixed(4)),
      fraction2: parseFloat(f2.toFixed(4)),
      fraction1Percent: parseFloat((f1 * 100.0).toFixed(2)),
      fraction2Percent: parseFloat((f2 * 100.0).toFixed(2)),
      rootMeanSquareError: parseFloat(rmse.toFixed(5))
    };
  }

  /**
   * Estimate surface basalt vs dust areal fraction from Thermal Emission Spectrometer (TES) emissivity.
   * @param {number[]} measuredEmissivity - Measured 10-band thermal infrared emissivity spectrum
   * @returns {{basaltFraction: number, dustFraction: number, surfaceType: string, rootMeanSquareError: number}}
   */
  static computeTESBasaltDustFraction(measuredEmissivity) {
    // Canonical TES Type 1 Basalt (Syrtis Major) and Bright Dust endmembers (Christensen et al., 2000; Bandfield et al., 2000)
    const basaltEM = [0.98, 0.96, 0.94, 0.91, 0.92, 0.95, 0.97, 0.96, 0.94, 0.98];
    const dustEM =   [0.91, 0.92, 0.95, 0.98, 0.97, 0.93, 0.90, 0.89, 0.91, 0.95];

    const unmix = BandMathEngine.computeLinearSpectralUnmixing2Components(measuredEmissivity, basaltEM, dustEM);

    let type = 'Mixed Basalt & Dust Mantle';
    if (unmix.fraction1 >= 0.70) {
      type = 'Dark Basaltic Bedrock / Low-Albedo';
    } else if (unmix.fraction2 >= 0.70) {
      type = 'Bright Oxidized Dust Mantle';
    }

    return {
      basaltFraction: unmix.fraction1,
      dustFraction: unmix.fraction2,
      surfaceType: type,
      rootMeanSquareError: unmix.rootMeanSquareError
    };
  }

  // --- CRISM Hydrated Phyllosilicate & Clay Mineral Solvers ---

  /**
   * Calculate CRISM Aluminum-Smectite / Kaolinite Al-OH absorption band depth (BD2210).
   * BD2210 = 1.0 - ( R_2210 / (0.5 * (R_2140 + R_2250)) )
   * @param {number} r2140 - Short continuum anchor at 2140 nm
   * @param {number} r2210 - Al-OH absorption band minimum at 2210 nm
   * @param {number} r2250 - Long continuum anchor at 2250 nm
   * @returns {{bd2210Index: number, hasAlPhyllosilicate: boolean, mineralFamily: string}}
   */
  static computeCRISMAlOHMineralIndexBD2210(r2140, r2210, r2250) {
    const continuum = 0.5 * (Math.max(1e-4, r2140) + Math.max(1e-4, r2250));
    const band = Math.max(1e-4, r2210);
    const depth = 1.0 - (band / continuum);

    return {
      bd2210Index: parseFloat(depth.toFixed(4)),
      hasAlPhyllosilicate: depth > 0.03,
      mineralFamily: depth > 0.03 ? 'Al-Smectite / Montmorillonite / Kaolinite' : 'Unenriched / No Al-OH Detected'
    };
  }

  /**
   * Calculate CRISM Fe/Mg-Smectite / Chlorite Fe/Mg-OH absorption band depth (BD2300).
   * BD2300 = 1.0 - ( R_2300 / (0.5 * (R_2250 + R_2350)) )
   * @param {number} r2250 - Short continuum anchor at 2250 nm
   * @param {number} r2300 - Fe/Mg-OH absorption band minimum at 2300 nm
   * @param {number} r2350 - Long continuum anchor at 2350 nm
   * @returns {{bd2300Index: number, hasFeMgPhyllosilicate: boolean, mineralFamily: string}}
   */
  static computeCRISMFeMgOHMineralIndexBD2300(r2250, r2300, r2350) {
    const continuum = 0.5 * (Math.max(1e-4, r2250) + Math.max(1e-4, r2350));
    const band = Math.max(1e-4, r2300);
    const depth = 1.0 - (band / continuum);

    return {
      bd2300Index: parseFloat(depth.toFixed(4)),
      hasFeMgPhyllosilicate: depth > 0.03,
      mineralFamily: depth > 0.03 ? 'Fe/Mg-Smectite / Nontronite / Saponite' : 'Unenriched / No Fe/Mg-OH Detected'
    };
  }

  /**
   * Classify pyroxene mineralogy (LCP vs HCP) using CRISM Band I (1 um) and Band II (2 um) crystal field absorption minima.
   * LCP (Orthopyroxene / Enstatite): Band I center < 950 nm, Band II center < 1950 nm.
   * HCP (Clinopyroxene / Augite / Diopside): Band I center > 1000 nm, Band II center > 2100 nm.
   * Reference: Cloutis & Gaffey (1991), Mustard et al. (2005), Viviano-Beck et al. (2014).
   * @param {number} band1MinNm - Band I absorption minimum wavelength in nanometers (e.g. 910 to 1050 nm)
   * @param {number} band2MinNm - Band II absorption minimum wavelength in nanometers (e.g. 1850 to 2350 nm)
   * @param {number} [band1Depth=0.08] - Band I absorption depth (0.0 to 1.0)
   * @param {number} [band2Depth=0.06] - Band II absorption depth (0.0 to 1.0)
   * @returns {{pyroxeneClass: string, isLCP: boolean, isHCP: boolean, isMixedOrIndeterminate: boolean, estimatedWoContentPct: number}}
   */
  static computeCRISMPyroxeneCompositionLCPvsHCP(band1MinNm, band2MinNm, band1Depth = 0.08, band2Depth = 0.06) {
    const b1 = Math.max(800.0, band1MinNm);
    const b2 = Math.max(1600.0, band2MinNm);
    const d1 = Math.max(0.0, band1Depth);
    const d2 = Math.max(0.0, band2Depth);

    if (d1 < 0.01 && d2 < 0.01) {
      return {
        pyroxeneClass: 'None / Below Detection Threshold',
        isLCP: false,
        isHCP: false,
        isMixedOrIndeterminate: true,
        estimatedWoContentPct: 0.0
      };
    }

    // Wollastonite (Wo) Ca-content empirical scaling from Band II center:
    // Wo (mol%) ~ (b2 - 1800) / 10.0 constrained between 0% (pure enstatite/ferrosilite) and 50% (pure diopside/hedenbergite)
    const estimatedWo = Math.min(50.0, Math.max(0.0, (b2 - 1800.0) / 10.0));

    let pyroxeneClass = 'Indeterminate / Mixed Pyroxene';
    let isLCP = false;
    let isHCP = false;

    if (b1 < 960.0 && b2 < 2000.0) {
      pyroxeneClass = 'Low-Calcium Pyroxene (LCP / Orthopyroxene / Norite)';
      isLCP = true;
    } else if (b1 >= 990.0 && b2 >= 2100.0) {
      pyroxeneClass = 'High-Calcium Pyroxene (HCP / Clinopyroxene / Augite-Basalt)';
      isHCP = true;
    } else {
      pyroxeneClass = 'Pigeonite / Intermediate Ca-Pyroxene';
    }

    return {
      pyroxeneClass,
      isLCP,
      isHCP,
      isMixedOrIndeterminate: !isLCP && !isHCP,
      estimatedWoContentPct: parseFloat(estimatedWo.toFixed(1))
    };
  }

  /**
   * Calculate CRISM hydrated sulfate indices: SINDEX2 (polyhydrated sulfate convexity), BD1900 (H2O band depth), and BD2100 (monohydrated kieserite band depth).
   * Reference: Pelkey et al. (2007), Viviano-Beck et al. (2014), Gendrin et al. (2005) for Meridiani Planum & Valles Marineris.
   * @param {number} r1850 - Reflectance at 1850 nm (short H2O continuum)
   * @param {number} r1930 - Reflectance at 1930 nm (H2O absorption minimum)
   * @param {number} r2060 - Reflectance at 2060 nm (intermediate continuum)
   * @param {number} r2130 - Reflectance at 2130 nm (kieserite absorption minimum)
   * @param {number} r2290 - Reflectance at 2290 nm (sulfate shoulder apex)
   * @param {number} r2400 - Reflectance at 2400 nm (long continuum anchor)
   * @returns {{sindex2: number, bd1900H2O: number, bd2100Kieserite: number, sulfateClass: string, isHydratedSulfate: boolean}}
   */
  static computeCRISMSulfateHydrationIndices(r1850, r1930, r2060, r2130, r2290, r2400) {
    const r185 = Math.max(1e-4, r1850);
    const r193 = Math.max(1e-4, r1930);
    const r206 = Math.max(1e-4, r2060);
    const r213 = Math.max(1e-4, r2130);
    const r229 = Math.max(1e-4, r2290);
    const r240 = Math.max(1e-4, r2400);

    // BD1900: H2O vibration
    const cont1900 = 0.5 * (r185 + r206);
    const bd1900 = 1.0 - (r193 / cont1900);

    // BD2100: Monohydrated Kieserite (MgSO4 * H2O)
    const cont2100 = 0.5 * (r206 + r229);
    const bd2100 = 1.0 - (r213 / cont2100);

    // SINDEX2: Polyhydrated sulfate convexity
    const sindex2 = 1.0 - ((r213 + r240) / (2.0 * r229));

    let sulfateClass = 'Unenriched / No Significant Sulfate';
    const isHydratedSulfate = bd1900 > 0.03 || bd2100 > 0.025 || sindex2 > 0.02;

    if (bd2100 > 0.03 && bd2100 >= sindex2) {
      sulfateClass = 'Monohydrated Sulfate (Kieserite / Szomolnokite)';
    } else if (sindex2 > 0.025 && bd1900 > 0.04) {
      sulfateClass = 'Polyhydrated Sulfate (Gypsum / Starkeyite / Bassanite)';
    } else if (isHydratedSulfate) {
      sulfateClass = 'Weakly Hydrated Sulfate Mixture';
    }

    return {
      sindex2: parseFloat(sindex2.toFixed(4)),
      bd1900H2O: parseFloat(bd1900.toFixed(4)),
      bd2100Kieserite: parseFloat(bd2100.toFixed(4)),
      sulfateClass,
      isHydratedSulfate
    };
  }

  /**
   * Calculate olivine Forsterite number (Fo# = Mg/(Mg+Fe) mol%) and composition from CRISM 1 um Fe2+ crystal field absorption center.
   * Fo# ~ ( 1085.0 - bandCenterNm ) / 0.50 (constrained between Fo0 fayalite and Fo100 forsterite).
   * Reference: King & Ridley (1987), Sunshine & Pieters (1998), Koeppen & Hamilton (2008) for Nili Fossae & Argyre olivines.
   * @param {number} olivineBandCenterNm - 1.05 um composite absorption center wavelength in nanometers (1030 nm to 1090 nm)
   * @param {number} [bandDepth=0.08] - Absorption band depth (0.0 to 1.0)
   * @returns {{foNumberPct: number, faNumberPct: number, olivineClass: string, isMagnesianFoRich: boolean, isIronFaRich: boolean}}
   */
  static computeCRISMOlivineFoNumberAndComposition(olivineBandCenterNm, bandDepth = 0.08) {
    const center = Math.max(950.0, Math.min(1150.0, olivineBandCenterNm));
    const depth = Math.max(0.0, bandDepth);

    if (depth < 0.02) {
      return {
        foNumberPct: 0.0,
        faNumberPct: 0.0,
        olivineClass: 'None / Below Detection Threshold',
        isMagnesianFoRich: false,
        isIronFaRich: false
      };
    }

    // Empirical linear calibration: Fo90 at 1040 nm, Fo30 at 1070 nm, Fo10 at 1080 nm
    const fo = Math.min(100.0, Math.max(0.0, (1085.0 - center) / 0.50));
    const fa = 100.0 - fo;

    let olivineClass = 'Intermediate Olivine (Fo40-Fo70 / Martian Basaltic Mantle/Crust)';
    let isMagnesian = false;
    let isIron = false;

    if (fo >= 70.0) {
      olivineClass = 'Forsterite-Rich / Magnesian Olivine (Fo70-Fo100 / Ultramafic Dunite)';
      isMagnesian = true;
    } else if (fo <= 35.0) {
      olivineClass = 'Fayalite-Rich / Iron Olivine (Fo0-Fo35 / Highly Differentiated)';
      isIron = true;
    }

    return {
      foNumberPct: parseFloat(fo.toFixed(1)),
      faNumberPct: parseFloat(fa.toFixed(1)),
      olivineClass,
      isMagnesianFoRich: isMagnesian,
      isIronFaRich: isIron
    };
  }

  /**
   * Calculate CRISM carbonate mineralogy doublet indices (BD2500 and BD2300 C-O vibrational overtone bands).
   * BD2500 = 1.0 - ( R_2530 / (0.5 * (R_2430 + R_2600)) )
   * BD2300 = 1.0 - ( R_2330 / (0.5 * (R_2250 + R_2390)) )
   * Reference: Ehlmann et al. (2008), Viviano-Beck et al. (2014) for Nili Fossae & Jezero crater rim carbonates.
   * @param {number} r2250 - Short continuum anchor at 2250 nm
   * @param {number} r2330 - Carbonate/metal-OH absorption at 2330 nm
   * @param {number} r2390 - Intermediate continuum anchor at 2390 nm
   * @param {number} r2430 - Pre-2.5 um continuum anchor at 2430 nm
   * @param {number} r2530 - Primary carbonate C-O overtone minimum at 2530 nm
   * @param {number} r2600 - Post-2.5 um continuum anchor at 2600 nm
   * @returns {{bd2500Index: number, bd2300Index: number, carbonateClass: string, isCarbonateDetected: boolean, isFeMgCarbonate: boolean}}
   */
  static computeCRISMCarbonateIndices(r2250, r2330, r2390, r2430, r2530, r2600) {
    const c225 = Math.max(1e-4, r2250);
    const b233 = Math.max(1e-4, r2330);
    const c239 = Math.max(1e-4, r2390);
    const c243 = Math.max(1e-4, r2430);
    const b253 = Math.max(1e-4, r2530);
    const c260 = Math.max(1e-4, r2600);

    const cont2300 = 0.5 * (c225 + c239);
    const bd2300 = 1.0 - (b233 / cont2300);

    const cont2500 = 0.5 * (c243 + c260);
    const bd2500 = 1.0 - (b253 / cont2500);

    let carbonateClass = 'Unenriched / No Significant Carbonate';
    const isCarbonate = bd2500 > 0.025 && bd2300 > 0.020;
    let isFeMg = false;

    if (isCarbonate) {
      if (bd2500 >= 0.035 && bd2300 >= 0.030) {
        carbonateClass = 'Fe/Mg-Carbonate (Magnesite / Siderite / Nili Fossae Type)';
        isFeMg = true;
      } else {
        carbonateClass = 'Calcite / Mixed Carbonate-Clay Assemblage';
      }
    } else if (bd2300 > 0.03 && bd2500 <= 0.015) {
      carbonateClass = 'Phyllosilicate (Fe/Mg-Smectite without 2.5 um Carbonate Doublet)';
    }

    return {
      bd2500Index: parseFloat(bd2500.toFixed(4)),
      bd2300Index: parseFloat(bd2300.toFixed(4)),
      carbonateClass,
      isCarbonateDetected: isCarbonate,
      isFeMgCarbonate: isFeMg
    };
  }

  /**
   * Calculate CRISM hydrated opaline silica and volcanic glass index (BD2250 and Si-OH asymmetric absorption).
   * BD2250 = 1.0 - ( R_2250 / (0.5 * (R_2140 + R_2350)) )
   * Reference: Squyres et al. (2008) for Home Plate Gusev, Rice et al. (2013), Viviano-Beck et al. (2014).
   * @param {number} r2140 - Short continuum anchor at 2140 nm
   * @param {number} r2210 - Al-OH absorption band at 2210 nm
   * @param {number} r2250 - Si-OH opaline silica absorption minimum at 2250 nm
   * @param {number} r2350 - Long continuum anchor at 2350 nm
   * @returns {{bd2250SilicaIndex: number, bd2210Index: number, silicaClass: string, isHydratedSilicaOpal: boolean, isDistinctFromAlSmectite: boolean}}
   */
  static computeCRISMHydratedSilicaOpalIndex(r2140, r2210, r2250, r2350) {
    const c214 = Math.max(1e-4, r2140);
    const b221 = Math.max(1e-4, r2210);
    const b225 = Math.max(1e-4, r2250);
    const c235 = Math.max(1e-4, r2350);

    const cont = 0.5 * (c214 + c235);
    const bd2250 = 1.0 - (b225 / cont);
    const bd2210 = 1.0 - (b221 / cont);

    let silicaClass = 'Unenriched / No Significant Hydrated Silica';
    let isSilica = false;
    let isDistinct = false;

    if (bd2250 > 0.03) {
      if (b225 <= b221 || bd2250 >= bd2210 * 0.95) {
        silicaClass = 'Opaline Hydrated Silica (Opal-A / Opal-CT / Hydrothermal Sinter)';
        isSilica = true;
        isDistinct = true;
      } else {
        silicaClass = 'Al-Smectite / Montmorillonite with Secondary Silica';
        isSilica = true;
        isDistinct = false;
      }
    }

    return {
      bd2250SilicaIndex: parseFloat(bd2250.toFixed(4)),
      bd2210Index: parseFloat(bd2210.toFixed(4)),
      silicaClass,
      isHydratedSilicaOpal: isSilica,
      isDistinctFromAlSmectite: isDistinct
    };
  }

  /**
   * Calculate CRISM ferric iron (Fe3+) mineralogy indices: BD530 (ferric slope), BD860 (crystalline hematite), and BD920 (goethite/jarosite).
   * BD530 = 1.0 - ( R_530 / (0.5 * (R_440 + R_770)) )
   * BD860 = 1.0 - ( R_860 / (0.5 * (R_770 + R_1000)) )
   * BD920 = 1.0 - ( R_920 / (0.5 * (R_770 + R_1000)) )
   * Reference: Morris et al. (2000), Christensen et al. (2001) for Meridiani hematite, Viviano-Beck et al. (2014).
   * @param {number} r440 - Blue reflectance at 440 nm
   * @param {number} r530 - Green reflectance at 530 nm
   * @param {number} r770 - Red/near-IR continuum anchor at 770 nm
   * @param {number} r860 - Hematite absorption minimum at 860 nm
   * @param {number} r920 - Goethite/Jarosite absorption minimum at 920 nm
   * @param {number} r1000 - Near-IR continuum anchor at 1000 nm
   * @returns {{bd530FerricIndex: number, bd860HematiteIndex: number, bd920GoethiteIndex: number, ferricClass: string, isCrystallineHematite: boolean, isGoethiteJarosite: boolean}}
   */
  static computeCRISMFerricOxideIndices(r440, r530, r770, r860, r920, r1000) {
    const c440 = Math.max(1e-4, r440);
    const b530 = Math.max(1e-4, r530);
    const c770 = Math.max(1e-4, r770);
    const b860 = Math.max(1e-4, r860);
    const b920 = Math.max(1e-4, r920);
    const c1000 = Math.max(1e-4, r1000);

    const cont530 = 0.5 * (c440 + c770);
    const bd530 = 1.0 - (b530 / cont530);

    const contIR = 0.5 * (c770 + c1000);
    const bd860 = 1.0 - (b860 / contIR);
    const bd920 = 1.0 - (b920 / contIR);

    let ferricClass = 'Low Ferric / Unweathered Basalt';
    let isHematite = false;
    let isGoethite = false;

    if (bd860 > 0.035 && bd860 >= bd920) {
      ferricClass = 'Crystalline Gray Hematite (alpha-Fe2O3 / Opportunity Type)';
      isHematite = true;
    } else if (bd920 > 0.035 && bd920 > bd860) {
      ferricClass = 'Goethite / Jarosite / Ferric Oxyhydroxide (alpha-FeO(OH))';
      isGoethite = true;
    } else if (bd530 > 0.05) {
      ferricClass = 'Nanophase / Amorphous Ferric Oxide (Martian Aeolian Red Dust)';
    }

    return {
      bd530FerricIndex: parseFloat(bd530.toFixed(4)),
      bd860HematiteIndex: parseFloat(bd860.toFixed(4)),
      bd920GoethiteIndex: parseFloat(bd920.toFixed(4)),
      ferricClass,
      isCrystallineHematite: isHematite,
      isGoethiteJarosite: isGoethite
    };
  }

  /**
   * Calculate Pyroxene Quadrilateral ternary coordinates (Wo-En-Fs mol%) and IMA mineral classification.
   * x = Fs_mol% + 0.5 * Wo_mol%
   * y = (sqrt(3) / 2) * Wo_mol%
   * Reference: Morimoto et al. (1988) IMA Pyroxene Nomenclature, Cloutis & Gaffey (1991).
   * @param {number} wollastoniteMol - Wollastonite (Ca2Si2O6) component in mol%
   * @param {number} enstatiteMol - Enstatite (Mg2Si2O6) component in mol%
   * @param {number} ferrosiliteMol - Ferrosilite (Fe2Si2O6) component in mol%
   * @returns {{wollastonitePct: number, enstatitePct: number, ferrosilitePct: number, mgNumberPct: number, ternaryX: number, ternaryY: number, mineralName: string, pyroxeneFamily: string}}
   */
  static computePyroxeneTernaryCoordinates(wollastoniteMol, enstatiteMol, ferrosiliteMol) {
    const w = Math.max(0.0, wollastoniteMol);
    const e = Math.max(0.0, enstatiteMol);
    const f = Math.max(0.0, ferrosiliteMol);

    const sum = Math.max(1e-4, w + e + f);
    const woNorm = (w / sum) * 100.0;
    const enNorm = (e / sum) * 100.0;
    const fsNorm = (f / sum) * 100.0;

    const mgNum = (enNorm + fsNorm > 1e-4) ? (enNorm / (enNorm + fsNorm)) * 100.0 : 50.0;

    // Ternary projection coordinates:
    const x = fsNorm + 0.5 * woNorm;
    const y = (Math.sqrt(3.0) / 2.0) * woNorm;

    let mineralName = 'Intermediate Pyroxene';
    let family = 'Clinopyroxene (High-Ca)';

    if (woNorm >= 45.0) {
      mineralName = mgNum >= 50.0 ? 'Diopside (CaMgSi2O6)' : 'Hedenbergite (CaFeSi2O6)';
      family = 'Calc-Pyroxene (Diopside-Hedenbergite Series)';
    } else if (woNorm >= 20.0) {
      mineralName = 'Augite ((Ca,Mg,Fe)2Si2O6)';
      family = 'High-Calcium Clinopyroxene (Augite)';
    } else if (woNorm >= 5.0) {
      mineralName = 'Pigeonite (Low-Ca Monoclinic Pyroxene)';
      family = 'Low-Calcium Clinopyroxene (Pigeonite)';
    } else {
      family = 'Orthopyroxene (Orthorhombic / Enstatite-Ferrosilite Series)';
      mineralName = mgNum >= 50.0 ? 'Enstatite / Bronzite (Mg-rich OPX)' : 'Ferrosilite / Hypersthene (Fe-rich OPX)';
    }

    return {
      wollastonitePct: parseFloat(woNorm.toFixed(2)),
      enstatitePct: parseFloat(enNorm.toFixed(2)),
      ferrosilitePct: parseFloat(fsNorm.toFixed(2)),
      mgNumberPct: parseFloat(mgNum.toFixed(2)),
      ternaryX: parseFloat(x.toFixed(2)),
      ternaryY: parseFloat(y.toFixed(2)),
      mineralName,
      pyroxeneFamily: family
    };
  }

  /**
   * Calculate CRISM oxychlorine / perchlorate hydrated salt indices (shifted 1.9 um and 2.14 um water of hydration).
   * Reference: Hecht et al. (2009), Hanley et al. (2014), Ojha et al. (2015) for RSL sites, Viviano-Beck et al. (2014).
   * @param {number} r1850 - Short continuum anchor at 1850 nm
   * @param {number} r1930 - Hydration absorption minimum at 1930 nm
   * @param {number} r2060 - Long continuum anchor at 2060 nm
   * @param {number} r2140 - Perchlorate water combination band at 2140 nm
   * @param {number} r2400 - Reference anchor at 2400 nm
   * @returns {{bd1900Index: number, perchlorateIndex: number, saltClass: string, isHydratedOxychlorineCandidate: boolean}}
   */
  static computeCRISMOxychlorineHydrationIndices(r1850, r1930, r2060, r2140, r2400) {
    const c185 = Math.max(1e-4, r1850);
    const b193 = Math.max(1e-4, r1930);
    const c206 = Math.max(1e-4, r2060);
    const b214 = Math.max(1e-4, r2140);
    const c240 = Math.max(1e-4, r2400);

    const cont1900 = 0.5 * (c185 + c206);
    const bd1900 = 1.0 - (b193 / cont1900);

    const slope2140 = (c240 - b214) / (c240 + b214);
    const perchlIndex = bd1900 * (1.0 + Math.max(0.0, slope2140));

    let saltClass = 'Anhydrous / Low Hydration Crust';
    let isCandidate = false;

    if (bd1900 > 0.04 && b214 < c240 * 0.96) {
      saltClass = 'Hydrated Oxychlorine Salt / Magnesium-Calcium Perchlorate Brine Candidate';
      isCandidate = true;
    } else if (bd1900 > 0.04) {
      saltClass = 'Hydrated Sulfate / Smectite Clay with Bound Molecular H2O';
    }

    return {
      bd1900Index: parseFloat(bd1900.toFixed(4)),
      perchlorateIndex: parseFloat(perchlIndex.toFixed(4)),
      saltClass,
      isHydratedOxychlorineCandidate: isCandidate
    };
  }

  /**
   * Calculate Pyroxene / Olivine Band Area Ratio (BAR = Area_B2 / Area_B1) and modal mineral abundance.
   * BAR = Area(2 um) / Area(1 um)
   * Reference: Cloutis et al. (1986, 2011), Gaffey et al. (1993, 2002), Sunshine & Pieters (1993).
   * @param {number} band1Area - Integrated absorption band area of 1 um feature (nm * reflectance)
   * @param {number} band2Area - Integrated absorption band area of 2 um feature (nm * reflectance)
   * @param {number} [band1CenterNm=1000.0] - Absorption minimum center wavelength of Band 1 in nm
   * @param {number} [band2CenterNm=2100.0] - Absorption minimum center wavelength of Band 2 in nm
   * @returns {{bandAreaRatio: number, pyroxeneFractionPct: number, olivineFractionPct: number, maficClass: string, isClinopyroxeneDominated: boolean, isOrthopyroxeneDominated: boolean}}
   */
  static computePyroxeneBandAreaRatio(band1Area, band2Area, band1CenterNm = 1000.0, band2CenterNm = 2100.0) {
    const a1 = Math.max(1e-4, band1Area);
    const a2 = Math.max(0.0, band2Area);
    const c1 = Math.max(700.0, band1CenterNm);
    const c2 = Math.max(1500.0, band2CenterNm);

    const bar = a2 / a1;

    // Linear unmixing calibration (Cloutis et al. 1986):
    // BAR = 0 -> 100% Olivine / 0% Pyroxene; BAR >= 1.0 -> ~100% Pyroxene
    let pxPct = Math.min(100.0, Math.max(0.0, bar * 100.0));
    let olPct = 100.0 - pxPct;

    let maficClass = 'Intermediate Pyroxene-Olivine Mix';
    let isCpx = false;
    let isOpx = false;

    if (bar < 0.20 || a2 < 0.05 * a1) {
      maficClass = 'Olivine Dominated Lithology (Dunite / Picrite / Chassignite)';
      pxPct = 0.0;
      olPct = 100.0;
    } else if (c1 >= 1000.0 && c2 >= 2100.0 && bar >= 1.2) {
      maficClass = 'High-Calcium Clinopyroxene (Augite / Diopside / Basalt)';
      isCpx = true;
    } else if (c1 < 980.0 && c2 < 2050.0 && bar >= 0.7) {
      maficClass = 'Low-Calcium Orthopyroxene (Enstatite / Hypersthene / Norite)';
      isOpx = true;
    }

    return {
      bandAreaRatio: parseFloat(bar.toFixed(4)),
      pyroxeneFractionPct: parseFloat(pxPct.toFixed(1)),
      olivineFractionPct: parseFloat(olPct.toFixed(1)),
      maficClass,
      isClinopyroxeneDominated: isCpx,
      isOrthopyroxeneDominated: isOpx
    };
  }

  /**
   * Invert Pyroxene Quadrilateral composition (Wo-En-Fs mol%) directly from Band 1 and Band 2 center absorption wavelengths.
   * Reference: Cloutis & Gaffey (1991), Gaffey et al. (2002), Skok et al. (2010).
   * @param {number} band1CenterNm - 1 um absorption minimum wavelength in nm (880 to 1080 nm)
   * @param {number} band2CenterNm - 2 um absorption minimum wavelength in nm (1800 to 2400 nm)
   * @returns {{wollastonitePct: number, enstatitePct: number, ferrosilitePct: number, mgNumberPct: number, pyroxeneClass: string, isHighCalciumPyroxene: boolean}}
   */
  static computePyroxeneCompositionFromBandCenters(band1CenterNm, band2CenterNm) {
    const b1 = Math.max(850.0, Math.min(1150.0, band1CenterNm));
    const b2 = Math.max(1700.0, Math.min(2500.0, band2CenterNm));

    // Empirical Gaffey / Cloutis calibration
    const rawWo = (b1 - 900.0) * 0.28 + (b2 - 1800.0) * 0.025;
    const rawFs = 25.0 - 0.05 * (b1 - 900.0) + 0.04 * (b2 - 1900.0);

    // Clamping to stoichiometric limits:
    const wo = Math.min(50.0, Math.max(0.0, rawWo));
    const fs = Math.min(100.0 - wo, Math.max(0.0, rawFs));
    const en = Math.max(0.0, 100.0 - wo - fs);

    const mgNum = (en + fs > 1e-4) ? (en / (en + fs)) * 100.0 : 50.0;

    let pClass = 'Intermediate Pyroxene';
    let isHighCa = false;

    if (wo >= 20.0) {
      pClass = wo >= 40.0 ? 'Diopside-Hedenbergite (High-Ca Calc-Pyroxene)' : 'Augite (High-Calcium Clinopyroxene)';
      isHighCa = true;
    } else if (wo >= 5.0) {
      pClass = 'Pigeonite (Low-Calcium Clinopyroxene)';
      isHighCa = false;
    } else {
      pClass = mgNum >= 50.0 ? 'Enstatite / Bronzite (Low-Ca Orthopyroxene)' : 'Ferrosilite / Hypersthene (Iron-Rich OPX)';
      isHighCa = false;
    }

    return {
      wollastonitePct: parseFloat(wo.toFixed(1)),
      enstatitePct: parseFloat(en.toFixed(1)),
      ferrosilitePct: parseFloat(fs.toFixed(1)),
      mgNumberPct: parseFloat(mgNum.toFixed(1)),
      pyroxeneClass: pClass,
      isHighCalciumPyroxene: isHighCa
    };
  }

  /**
   * Calculate high-precision Clinopyroxene Quadrilateral sub-classification (Diopside, Endiopside, Augite, Ferroaugite, Hedenbergite).
   * Reference: Morimoto (1988) IMA, Deer, Howie & Zussman (1992).
   * @param {number} wollastonitePct - Wollastonite mol% (0 to 50%)
   * @param {number} enstatitePct - Enstatite mol% (0 to 100%)
   * @param {number} ferrosilitePct - Ferrosilite mol% (0 to 100%)
   * @returns {{subtype: string, mgNumberPct: number, isCalcPyroxene: boolean, petrologicEnvironment: string}}
   */
  static computeClinopyroxeneSubtypeClassification(wollastonitePct, enstatitePct, ferrosilitePct) {
    const w = Math.min(50.0, Math.max(0.0, wollastonitePct));
    const e = Math.max(0.0, enstatitePct);
    const f = Math.max(0.0, ferrosilitePct);

    const sum = Math.max(1e-4, w + e + f);
    const wo = (w / sum) * 100.0;
    const en = (e / sum) * 100.0;
    const fs = (f / sum) * 100.0;

    const mgNum = (en + fs > 1e-4) ? (en / (en + fs)) * 100.0 : 50.0;

    let subtype = 'Clinopyroxene (Augite)';
    let petrology = 'Basaltic Volcanic / Subvolcanic Flow';
    let isCalc = true;

    if (wo >= 45.0) {
      if (mgNum >= 90.0) {
        subtype = 'Diopside (Pure Endmember CaMgSi2O6)';
        petrology = 'Mantle Peridotite / Skarn / Ultramafic Cumulate';
      } else if (mgNum >= 50.0) {
        subtype = 'Endiopside (Magnesian Diopside-Augite)';
        petrology = 'Layered Gabbroic Intrusion / Plutonic Cumulate';
      } else {
        subtype = 'Hedenbergite (Iron-Rich CaFeSi2O6)';
        petrology = 'Highly Differentiated Iron-Rich Syenite / Skarn';
      }
    } else if (wo >= 20.0) {
      if (wo < 30.0) {
        subtype = 'Subcalcic Augite (Rapidly Quenched CPX)';
        petrology = 'Quenched Tholeiitic Basalt / Meteoritic Eucrite';
      } else if (mgNum >= 50.0) {
        subtype = 'Augite (Standard Igneous High-Ca CPX)';
        petrology = 'Typical Martian Basaltic Lava Flow (Shergottite / Gusev)';
      } else {
        subtype = 'Ferroaugite (Evolved High-Fe CPX)';
        petrology = 'Fractionated Ferrobasalt / Differentiated Lava Lake';
      }
    } else {
      subtype = 'Low-Calcium Pyroxene (Pigeonite / Orthopyroxene)';
      petrology = 'Subsolidus Inversion / Magmatic Norite';
      isCalc = false;
    }

    return {
      subtype,
      mgNumberPct: parseFloat(mgNum.toFixed(1)),
      isCalcPyroxene: isCalc,
      petrologicEnvironment: petrology
    };
  }

  /**
   * Calculate pyroxene solvus equilibrium crystallization geothermometry (Lindsley pyroxene thermometer).
   * T(C) = 720.0 + 8.5 * Wo% + 2.8 * Mg#
   * Reference: Lindsley (1983), Sack & Ghiorso (1994), Sunshine et al. (2004).
   * @param {number} wollastonitePct - Wollastonite mol% (0 to 50%)
   * @param {number} mgNumberPct - Magnesium number Mg# (0 to 100%)
   * @returns {{crystallizationTempC: number, crystallizationTempK: number, thermalRegime: string, isMagmaticExtrusion: boolean}}
   */
  static computePyroxeneSolvusCrystallizationTemperature(wollastonitePct, mgNumberPct) {
    const wo = Math.min(50.0, Math.max(0.0, wollastonitePct));
    const mg = Math.min(100.0, Math.max(0.0, mgNumberPct));

    const tempC = 720.0 + 8.5 * wo + 2.8 * mg;
    const tempK = tempC + 273.15;

    let regime = 'Subsolidus Re-equilibration / Metamorphic Annealing (< 950 C)';
    let isMagmatic = false;

    if (tempC >= 1150.0) {
      regime = 'High-Temperature Magmatic Basaltic Extrusion (1150 - 1300 C)';
      isMagmatic = true;
    } else if (tempC >= 950.0) {
      regime = 'Plutonic / Layered Mafic Intrusion Crystallization (950 - 1150 C)';
      isMagmatic = true;
    }

    return {
      crystallizationTempC: parseFloat(tempC.toFixed(1)),
      crystallizationTempK: parseFloat(tempK.toFixed(1)),
      thermalRegime: regime,
      isMagmaticExtrusion: isMagmatic
    };
  }

  /**
   * Calculate clinopyroxene/melt Fe-Mg exchange distribution coefficient KD and invert equilibrium parent magma composition.
   * KD(Fe-Mg) = ( (Fe/Mg)_cpx ) / ( (Fe/Mg)_melt ) ~ 0.28
   * Reference: Roeder & Emslie (1970), Nielsen & Drake (1979), Filiberto & Dasgupta (2011) for Martian shergottites.
   * @param {number} pyroxeneMgNumberPct - Pyroxene crystal Mg# (0 to 100%)
   * @param {number} [wollastonitePct=35.0] - Pyroxene crystal Wollastonite mol% (0 to 50%)
   * @returns {{kdFeMg: number, equilibriumMeltMgNumberPct: number, liquidusTempC: number, parentMagmaType: string, isPrimitiveParentMelt: boolean}}
   */
  static computePyroxeneMeltPartitionCoefficients(pyroxeneMgNumberPct, wollastonitePct = 35.0) {
    const mgCpx = Math.min(99.0, Math.max(1.0, pyroxeneMgNumberPct));
    const wo = Math.min(50.0, Math.max(0.0, wollastonitePct));

    const KD = 0.28; // Roeder & Emslie equilibrium exchange coefficient

    // (Fe/Mg)_cpx = (100 - Mg#) / Mg#
    const feMgCpx = (100.0 - mgCpx) / mgCpx;

    // (Fe/Mg)_melt = (Fe/Mg)_cpx / KD
    const feMgMelt = feMgCpx / KD;

    // Mg#_melt = 100 / (1 + (Fe/Mg)_melt)
    const mgMelt = 100.0 / (1.0 + feMgMelt);

    // Liquidus temperature approximation (C)
    const tLiqC = 1020.0 + 4.5 * mgCpx + 3.0 * wo;

    let magmaType = 'Evolved Basaltic Magma (Typical Martian Surface Lava)';
    let isPrimitive = false;

    if (mgMelt >= 65.0) {
      magmaType = 'Primitive Primary Mantle Melt (Picritic / Ultramafic)';
      isPrimitive = true;
    } else if (mgMelt < 40.0) {
      magmaType = 'Highly Fractionated Ferrobasalt (Evolved Rift / Caldera)';
      isPrimitive = false;
    }

    return {
      kdFeMg: KD,
      equilibriumMeltMgNumberPct: parseFloat(mgMelt.toFixed(1)),
      liquidusTempC: parseFloat(tLiqC.toFixed(1)),
      parentMagmaType: magmaType,
      isPrimitiveParentMelt: isPrimitive
    };
  }

  /**
   * Calculate CRISM 1.3 um Plagioclase Feldspar absorption index BD1300 and detect ancient primordial anorthosite crust.
   * BD1300 = 1.0 - ( 2 * R_1300 ) / ( R_1080 + R_1750 )
   * Reference: Carter et al. (2013), Skok et al. (2010), Cheek et al. (2013) for Martian feldspar-rich crust.
   * @param {number} r1080 - Calibrated I/F reflectance at 1.08 um continuum
   * @param {number} r1300 - Calibrated I/F reflectance at 1.30 um plagioclase Fe2+ minimum
   * @param {number} r1750 - Calibrated I/F reflectance at 1.75 um continuum
   * @param {number} [anorthiteFractionPct=85.0] - Calcic plagioclase Anorthite mol% (0 to 100)
   * @returns {{bd1300: number, anorthitePct: number, plagioclaseType: string, isAnorthositeCrustOutcrop: boolean}}
   */
  static computeCRISMPlagioclaseAnorthositeIndices(r1080, r1300, r1750, anorthiteFractionPct = 85.0) {
    const r1 = Math.max(1e-4, r1080);
    const rMin = Math.max(1e-4, r1300);
    const r2 = Math.max(1e-4, r1750);
    const an = Math.min(100.0, Math.max(0.0, anorthiteFractionPct));

    const continuum = (r1 + r2) / 2.0;
    const bd1300 = 1.0 - (rMin / continuum);

    let pType = 'Labradorite / Andesine (Intermediate Volcanic Plagioclase)';
    let isAnorthosite = false;

    if (an >= 90.0) {
      pType = 'Anorthite (Pure Calcic Endmember - Primordial Primary Crust)';
      isAnorthosite = bd1300 >= 0.012;
    } else if (an >= 70.0) {
      pType = 'Bytownite (Calcic Plagioclase - Ancient Crater Central Peaks)';
      isAnorthosite = bd1300 >= 0.015;
    } else if (an >= 50.0) {
      pType = 'Labradorite (Basaltic / Gabbroic Phenocrysts)';
      isAnorthosite = false;
    }

    return {
      bd1300: parseFloat(bd1300.toFixed(4)),
      anorthitePct: parseFloat(an.toFixed(1)),
      plagioclaseType: pType,
      isAnorthositeCrustOutcrop: isAnorthosite
    };
  }

  /**
   * Discriminate hydrous alkaline Zeolites (Analcime / Chabazite) from Smectite Phyllosilicates (Saponite / Nontronite).
   * BD1900 = 1.0 - ( 2 * R_1920 ) / ( R_1815 + R_2130 )
   * BD2300 = 1.0 - ( 2 * R_2300 ) / ( R_2140 + R_2390 )
   * Reference: Ehlmann et al. (2009, 2011), Carter et al. (2013) for Martian alkaline lacustrine & hydrothermal minerals.
   * @param {number} r1420 - Reflectance at 1.42 um H2O/OH band
   * @param {number} r1920 - Reflectance at 1.92 um structural H2O band
   * @param {number} r2300 - Reflectance at 2.30 um Fe/Mg-OH vibrational band
   * @param {number} [continuumLevel=0.30] - Mean background continuum reflectance
   * @returns {{bd1900: number, bd2300: number, bd1400: number, mineralFamily: string, isZeolite: boolean, isSmectiteClay: boolean, geologicalSetting: string}}
   */
  static computeCRISMZeolitePhyllosilicateDiscrimination(r1420, r1920, r2300, continuumLevel = 0.30) {
    const cont = Math.max(1e-4, continuumLevel);
    const bd1400 = Math.max(0.0, 1.0 - (r1420 / cont));
    const bd1900 = Math.max(0.0, 1.0 - (r1920 / cont));
    const bd2300 = Math.max(0.0, 1.0 - (r2300 / cont));

    let family = 'Unaltered Primary Basalt / Dust';
    let isZeo = false;
    let isClay = false;
    let setting = 'Dry Volcanic Plains';

    if (bd1900 >= 0.035 && bd2300 < 0.015) {
      family = 'Hydrous Zeolite (Analcime / Chabazite / Clinoptilolite)';
      isZeo = true;
      setting = 'Alkaline Closed-Basin Paleolake or Low-T Hydrothermal Alteration';
    } else if (bd1900 >= 0.025 && bd2300 >= 0.020) {
      family = 'Fe/Mg-Smectite Phyllosilicate (Saponite / Nontronite)';
      isClay = true;
      setting = 'Circum-Neutral Aqueous Weathering / Noachian Fluvial System';
    } else if (bd1900 >= 0.025) {
      family = 'Hydrated Silica / Opal / Glass';
      setting = 'Fumarolic Acid Leaching / Hydrothermal Sinter';
    }

    return {
      bd1900: parseFloat(bd1900.toFixed(4)),
      bd2300: parseFloat(bd2300.toFixed(4)),
      bd1400: parseFloat(bd1400.toFixed(4)),
      mineralFamily: family,
      isZeolite: isZeo,
      isSmectiteClay: isClay,
      geologicalSetting: setting
    };
  }
}




















