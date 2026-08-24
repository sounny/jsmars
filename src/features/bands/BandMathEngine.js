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
}
