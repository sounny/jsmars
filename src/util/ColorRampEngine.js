/**
 * @module ColorRampEngine
 * @description Planetary hypsometric tinting and scientific colormap engine for jsMars.
 * Provides multi-stop color lookup tables (LUTs), continuous interpolation,
 * data-to-RGBA array colorization, and gradient legend generation matching Java JMARS color modules.
 */

export class ColorRampEngine {
  // Scientific color stop definitions [[t (0-1), [r, g, b]]]
  static PRESETS = {
    mola_rainbow: [
      { t: 0.0, rgb: [20, 30, 140] },    // -8000m Deep Blue (Hellas)
      { t: 0.2, rgb: [30, 160, 220] },   // -4000m Cyan
      { t: 0.4, rgb: [40, 180, 50] },    // 0m Green (Datum)
      { t: 0.6, rgb: [240, 220, 30] },   // +4000m Yellow
      { t: 0.8, rgb: [230, 80, 20] },    // +8000m Orange/Red
      { t: 1.0, rgb: [255, 255, 255] }   // +21000m White (Olympus)
    ],
    viridis: [
      { t: 0.0, rgb: [68, 1, 84] },
      { t: 0.25, rgb: [59, 82, 139] },
      { t: 0.5, rgb: [33, 145, 140] },
      { t: 0.75, rgb: [94, 201, 98] },
      { t: 1.0, rgb: [253, 231, 37] }
    ],
    magma: [
      { t: 0.0, rgb: [0, 0, 4] },
      { t: 0.25, rgb: [81, 18, 124] },
      { t: 0.5, rgb: [182, 54, 121] },
      { t: 0.75, rgb: [251, 136, 97] },
      { t: 1.0, rgb: [252, 253, 191] }
    ],
    coolwarm: [
      { t: 0.0, rgb: [59, 76, 192] },
      { t: 0.5, rgb: [221, 221, 221] },
      { t: 1.0, rgb: [180, 4, 38] }
    ],
    grayscale: [
      { t: 0.0, rgb: [0, 0, 0] },
      { t: 1.0, rgb: [255, 255, 255] }
    ],
    topographic: [
      { t: 0.0, rgb: [46, 74, 98] },     // Deep basins
      { t: 0.35, rgb: [120, 150, 100] }, // Lowlands
      { t: 0.65, rgb: [200, 140, 80] },  // Highlands
      { t: 0.85, rgb: [160, 90, 50] },   // Volcanic shields
      { t: 1.0, rgb: [240, 240, 240] }   // Frost/Ice caps
    ]
  };

  /**
   * Interpolate RGB color for a normalized parameter t in [0, 1].
   * @param {number} t - Normalized value between 0.0 and 1.0
   * @param {Array<object>} stops - Color stops
   * @returns {[number, number, number]} [R, G, B]
   */
  static interpolateColor(t, stops) {
    const clampedT = Math.max(0, Math.min(1, t));

    if (clampedT <= stops[0].t) return [...stops[0].rgb];
    if (clampedT >= stops[stops.length - 1].t) return [...stops[stops.length - 1].rgb];

    for (let i = 0; i < stops.length - 1; i++) {
      const s0 = stops[i];
      const s1 = stops[i + 1];

      if (clampedT >= s0.t && clampedT <= s1.t) {
        const span = s1.t - s0.t;
        const localT = span > 0 ? (clampedT - s0.t) / span : 0;

        const r = Math.round(s0.rgb[0] + localT * (s1.rgb[0] - s0.rgb[0]));
        const g = Math.round(s0.rgb[1] + localT * (s1.rgb[1] - s0.rgb[1]));
        const b = Math.round(s0.rgb[2] + localT * (s1.rgb[2] - s0.rgb[2]));

        return [r, g, b];
      }
    }

    return [255, 255, 255];
  }

  /**
   * Generate a 256-entry RGB Lookup Table (LUT).
   * @param {string} [presetName='mola_rainbow']
   * @param {number} [steps=256]
   * @returns {Uint8Array} Flat Uint8Array of length steps * 3 [R, G, B, R, G, B, ...]
   */
  static generateLUT(presetName = 'mola_rainbow', steps = 256) {
    const stops = ColorRampEngine.PRESETS[presetName] || ColorRampEngine.PRESETS.mola_rainbow;
    const lut = new Uint8Array(steps * 3);

    for (let i = 0; i < steps; i++) {
      const t = i / (steps - 1);
      const [r, g, b] = ColorRampEngine.interpolateColor(t, stops);
      lut[i * 3] = r;
      lut[i * 3 + 1] = g;
      lut[i * 3 + 2] = b;
    }

    return lut;
  }

  /**
   * Map a single numerical value to an RGB color.
   * @param {number} value
   * @param {number} minVal
   * @param {number} maxVal
   * @param {string} [presetName='mola_rainbow']
   * @returns {[number, number, number]} [R, G, B]
   */
  static mapValueToColor(value, minVal, maxVal, presetName = 'mola_rainbow') {
    const stops = ColorRampEngine.PRESETS[presetName] || ColorRampEngine.PRESETS.mola_rainbow;
    const span = maxVal - minVal;
    const t = span !== 0 ? (value - minVal) / span : 0.5;
    return ColorRampEngine.interpolateColor(t, stops);
  }

  /**
   * Colorize a 1D array of numerical elevation / physical values into RGBA pixel buffer.
   * @param {Float32Array|Array<number>} values - 1D array of values
   * @param {number} minVal - Minimum value
   * @param {number} maxVal - Maximum value
   * @param {string} [presetName='mola_rainbow'] - Colormap name
   * @param {number} [opacity=255] - Alpha channel (0-255)
   * @returns {Uint8ClampedArray} RGBA image buffer (length = values.length * 4)
   */
  static colorizeArray(values, minVal, maxVal, presetName = 'mola_rainbow', opacity = 255) {
    const lut = ColorRampEngine.generateLUT(presetName, 256);
    const span = maxVal - minVal;
    const len = values.length;
    const rgba = new Uint8ClampedArray(len * 4);

    for (let i = 0; i < len; i++) {
      const val = values[i];
      let t = span !== 0 ? (val - minVal) / span : 0;
      if (t < 0) t = 0;
      if (t > 1) t = 1;

      const lutIdx = Math.floor(t * 255) * 3;
      const pixIdx = i * 4;

      rgba[pixIdx] = lut[lutIdx];
      rgba[pixIdx + 1] = lut[lutIdx + 1];
      rgba[pixIdx + 2] = lut[lutIdx + 2];
      rgba[pixIdx + 3] = opacity;
    }

    return rgba;
  }
}
