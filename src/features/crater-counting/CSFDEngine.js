/**
 * @module CSFDEngine
 * @description Planetary Crater Size-Frequency Distribution (CSFD) and Chronology Isochron engine
 * based on Hartmann (2005), Neukum et al. (2001), and Ivanov (2001) Mars production and chronology functions.
 */
export class CSFDEngine {
  /**
   * Neukum Production Function polynomial coefficients for Mars (a0..a11)
   * log10(N(>D)) = sum(aj * (log10(D))^j) for 1 Ga reference isochron (D in km, N per km^2)
   */
  static NPF_COEFFS = [
    -3.0876,  // a0
    -3.5332,  // a1
     0.3541,  // a2
     0.8876,  // a3
    -0.1264,  // a4
    -0.2789,  // a5
     0.0384,  // a6
     0.0396,  // a7
    -0.0039,  // a8
    -0.0021,  // a9
     0.00014, // a10
     0.00004  // a11
  ];

  /**
   * Mars Chronology Function (Hartmann & Neukum 2001):
   * N(D > 1 km) / km^2 = 2.68e-14 * (exp(6.93 * t) - 1) + 4.13e-4 * t
   * where t is surface age in Ga (Giga-years).
   */
  static chronologyN1(ageGa) {
    if (ageGa <= 0) return 0;
    const a = 2.68e-14;
    const b = 6.93;
    const c = 4.13e-4;
    return a * (Math.exp(b * ageGa) - 1) + c * ageGa;
  }

  /**
   * Invert chronology function to estimate surface age from N(D > 1km) cumulative density (per km^2).
   * @param {number} n1DensityPerKm2 - Cumulative density of craters > 1km per km^2.
   * @returns {number} Model age in Ga (or Ma if < 0.1 Ga).
   */
  static estimateAgeFromN1(n1DensityPerKm2) {
    if (n1DensityPerKm2 <= 0) return 0;

    // Binary search / Newton iteration for age t in [0.001, 4.5] Ga
    let low = 0.001;
    let high = 4.5;
    for (let iter = 0; iter < 30; iter++) {
      const mid = (low + high) / 2;
      const val = this.chronologyN1(mid);
      if (val < n1DensityPerKm2) {
        low = mid;
      } else {
        high = mid;
      }
    }
    return (low + high) / 2;
  }

  /**
   * Compute cumulative isochron points across diameter range D = 0.1 km to 100 km.
   * @param {number} ageGa - Surface age in Ga.
   * @param {number} [areaKm2=1e6] - Reference area in km^2 (default 10^6 km^2).
   * @returns {Array<{diameterKm: number, cumulativeN: number}>}
   */
  static getIsochronCurve(ageGa, areaKm2 = 1e6) {
    const n1 = this.chronologyN1(ageGa);
    // Reference N1 for 1 Ga is NPF a0 in 10^a0
    const n1_1Ga = Math.pow(10, this.NPF_COEFFS[0]);
    const scaling = n1 / n1_1Ga;

    const points = [];
    const minLogD = -1.0; // 0.1 km
    const maxLogD = 2.0;  // 100 km
    const steps = 40;

    for (let i = 0; i <= steps; i++) {
      const logD = minLogD + (i / steps) * (maxLogD - minLogD);
      const D = Math.pow(10, logD);

      // Evaluate NPF polynomial
      let logN_1Ga = 0;
      for (let j = 0; j < this.NPF_COEFFS.length; j++) {
        logN_1Ga += this.NPF_COEFFS[j] * Math.pow(logD, j);
      }

      const N_per_km2 = Math.pow(10, logN_1Ga) * scaling;
      const N_normalized = N_per_km2 * areaKm2;

      points.push({
        diameterKm: parseFloat(D.toFixed(3)),
        cumulativeN: Math.max(1e-6, N_normalized)
      });
    }

    return points;
  }

  /**
   * Compute empirical cumulative CSFD from a list of crater objects.
   * @param {Array<{diameter: number}>} craters - Crater records with diameter in meters.
   * @param {number} [countAreaKm2=1e6] - Total counting area in km^2.
   * @returns {object} CSFD distribution bins, total count, and estimated model age.
   */
  static computeCSFD(craters = [], countAreaKm2 = 1e6) {
    if (!craters || craters.length === 0) {
      return {
        totalCraters: 0,
        areaKm2: countAreaKm2,
        bins: [],
        estimatedAgeGa: 0,
        epoch: 'Unknown'
      };
    }

    // Convert diameters to km and sort descending
    const diametersKm = craters
      .map(c => (typeof c.diameter === 'number' ? c.diameter / 1000 : 1.0))
      .filter(d => d > 0)
      .sort((a, b) => a - b);

    const N_total = diametersKm.length;
    const bins = [];

    // Logarithmic bin intervals
    const minD = Math.max(0.05, diametersKm[0] * 0.9);
    const maxD = Math.max(1.0, diametersKm[diametersKm.length - 1] * 1.1);

    const numBins = 15;
    const logMin = Math.log10(minD);
    const logMax = Math.log10(maxD);

    for (let i = 0; i < numBins; i++) {
      const dThreshold = Math.pow(10, logMin + (i / (numBins - 1)) * (logMax - logMin));
      // Count craters with diameter >= dThreshold
      const countGreater = diametersKm.filter(d => d >= dThreshold).length;
      const cumulativeDensityPer1M = (countGreater / countAreaKm2) * 1e6;

      bins.push({
        diameterKm: parseFloat(dThreshold.toFixed(2)),
        count: countGreater,
        cumulativeDensity: parseFloat(cumulativeDensityPer1M.toFixed(2))
      });
    }

    // Estimate age based on craters around 1 km
    const countAbove1Km = diametersKm.filter(d => d >= 1.0).length;
    const n1DensityPerKm2 = (countAbove1Km > 0 ? countAbove1Km : N_total * 0.1) / countAreaKm2;
    const estimatedAgeGa = this.estimateAgeFromN1(n1DensityPerKm2);

    // Geological epoch determination
    let epoch = 'Amazonian';
    if (estimatedAgeGa >= 3.7) epoch = 'Noachian (>3.7 Ga)';
    else if (estimatedAgeGa >= 3.0) epoch = 'Hesperian (3.0 - 3.7 Ga)';
    else epoch = 'Amazonian (<3.0 Ga)';

    return {
      totalCraters: N_total,
      areaKm2: countAreaKm2,
      bins,
      estimatedAgeGa: parseFloat(estimatedAgeGa.toFixed(3)),
      epoch
    };
  }

  // --- Planetary Geological R-Plots & Saturation Analysis ---

  /**
   * Compute standard Planetary Relative (R) Plot bins: R = (D_m^3 * N) / (A * deltaD).
   * @param {Array<{diameter: number}>} craters - Array of crater objects with diameter in meters
   * @param {number} [countAreaKm2=1e6] - Total counting area in km^2
   * @returns {Array<{dMin: number, dMax: number, dMean: number, count: number, rValue: number}>}
   */
  static computeRPlot(craters = [], countAreaKm2 = 1e6) {
    if (!craters || craters.length === 0) return [];

    const diametersKm = craters
      .map(c => (typeof c.diameter === 'number' ? c.diameter / 1000 : 1.0))
      .filter(d => d > 0);

    // Standard sqrt(2) diameter bins: [0.1, 0.141, 0.2, 0.282, 0.4, 0.565, 0.8, 1.13, 1.6, ...]
    const rBins = [];
    const factor = Math.SQRT2;
    let dMin = 0.1;

    for (let b = 0; b < 12; b++) {
      const dMax = dMin * factor;
      const dMean = Math.sqrt(dMin * dMax);
      const deltaD = dMax - dMin;

      const inBin = diametersKm.filter(d => d >= dMin && d < dMax).length;
      const rValue = inBin > 0 ? (Math.pow(dMean, 3) * inBin) / (countAreaKm2 * deltaD) : 0;

      rBins.push({
        dMin: parseFloat(dMin.toFixed(3)),
        dMax: parseFloat(dMax.toFixed(3)),
        dMean: parseFloat(dMean.toFixed(3)),
        count: inBin,
        rValue: parseFloat(rValue.toExponential(4)),
        rRaw: rValue
      });

      dMin = dMax;
    }

    return rBins;
  }

  /**
   * Calculate Trask / Hartmann geometric crater saturation equilibrium density.
   * @param {number} diameterKm - Crater diameter in km
   * @returns {number} Saturation cumulative density N(>D) per km^2
   */
  static computeSaturationLimit(diameterKm) {
    const safeD = Math.max(0.01, diameterKm);
    return 0.15 * Math.pow(safeD, -2);
  }

  /**
   * Classify crater degradation / freshness state based on depth-to-diameter ratio.
   * @param {number} depthMeters - Crater depth in meters
   * @param {number} diameterMeters - Crater diameter in meters
   * @returns {{freshnessClass: number, name: string, ratio: number}}
   */
  static classifyCraterFreshness(depthMeters, diameterMeters) {
    if (diameterMeters <= 0) return { freshnessClass: 3, name: 'Eroded / Ghost', ratio: 0 };
    const ratio = depthMeters / diameterMeters;

    if (ratio >= 0.15) {
      return { freshnessClass: 1, name: 'Pristine / Fresh (Sharp Rim & Rays)', ratio: parseFloat(ratio.toFixed(3)) };
    } else if (ratio >= 0.08) {
      return { freshnessClass: 2, name: 'Moderate / Degraded (Rounded Rim)', ratio: parseFloat(ratio.toFixed(3)) };
    } else {
      return { freshnessClass: 3, name: 'Severely Eroded / Infilled Ghost Crater', ratio: parseFloat(ratio.toFixed(3)) };
    }
  }

  // --- Isochron Model Fitting & Poisson Uncertainty Solvers ---

  /**
   * Fit an empirical crater population to the Neukum Production Function to derive model age.
   * @param {Array<{diameter: number}>} craters - Array of craters (diameter in meters)
   * @param {number} [countAreaKm2=1e6] - Total counting area in km^2
   * @param {number} [dMinKm=1.0] - Minimum fitting diameter (km)
   * @param {number} [dMaxKm=50.0] - Maximum fitting diameter (km)
   * @returns {{ageGa: number, ageErrorGa: number, minAgeGa: number, maxAgeGa: number, count: number, epoch: string}}
   */
  static fitIsochronAge(craters = [], countAreaKm2 = 1e6, dMinKm = 1.0, dMaxKm = 50.0) {
    const validDiameters = craters
      .map(c => (typeof c.diameter === 'number' ? c.diameter / 1000 : 1.0))
      .filter(d => d >= dMinKm && d <= dMaxKm);

    const N = validDiameters.length;
    if (N === 0) {
      return { ageGa: 0, ageErrorGa: 0, minAgeGa: 0, maxAgeGa: 0, count: 0, epoch: 'Undetermined' };
    }

    const nDensity = N / countAreaKm2;
    const bestAgeGa = this.estimateAgeFromN1(nDensity);

    // Poisson 1-sigma bounds: N +/- sqrt(N)
    const sigmaN = Math.sqrt(N);
    const nLow = Math.max(0, N - sigmaN) / countAreaKm2;
    const nHigh = (N + sigmaN) / countAreaKm2;

    const minAgeGa = this.estimateAgeFromN1(nLow);
    const maxAgeGa = this.estimateAgeFromN1(nHigh);
    const ageErrorGa = (maxAgeGa - minAgeGa) / 2.0;

    return {
      ageGa: parseFloat(bestAgeGa.toFixed(3)),
      ageErrorGa: parseFloat(ageErrorGa.toFixed(3)),
      minAgeGa: parseFloat(minAgeGa.toFixed(3)),
      maxAgeGa: parseFloat(maxAgeGa.toFixed(3)),
      count: N,
      epoch: this.classifyEpoch(bestAgeGa)
    };
  }

  /**
   * Compute 1-sigma Poisson counting uncertainty for a crater population.
   * @param {number} count - Observed crater count
   * @param {number} [countAreaKm2=1e6] - Counting area in km^2
   * @returns {{count: number, sigmaCount: number, density: number, sigmaDensity: number, fractionalError: number}}
   */
  static computePoissonUncertainty(count, countAreaKm2 = 1e6) {
    const n = Math.max(0, count);
    const sigmaN = Math.sqrt(n);
    const density = n / countAreaKm2;
    const sigmaDensity = sigmaN / countAreaKm2;
    const fractionalError = n > 0 ? sigmaN / n : 0;

    return {
      count: n,
      sigmaCount: parseFloat(sigmaN.toFixed(2)),
      density: parseFloat(density.toExponential(4)),
      sigmaDensity: parseFloat(sigmaDensity.toExponential(4)),
      fractionalError: parseFloat(fractionalError.toFixed(4))
    };
  }

  /**
   * Classify planetary geological epoch based on model age.
   * @param {number} ageGa - Surface age in Ga
   * @returns {string} Geological epoch classification
   */
  static classifyEpoch(ageGa) {
    if (ageGa >= 3.95) return 'Early Noachian (>3.95 Ga)';
    if (ageGa >= 3.7) return 'Middle/Late Noachian (3.7 - 3.95 Ga)';
    if (ageGa >= 3.4) return 'Early Hesperian (3.4 - 3.7 Ga)';
    if (ageGa >= 3.0) return 'Late Hesperian (3.0 - 3.4 Ga)';
    if (ageGa >= 2.0) return 'Early Amazonian (2.0 - 3.0 Ga)';
    if (ageGa >= 0.3) return 'Middle Amazonian (0.3 - 2.0 Ga)';
    return 'Late Amazonian (<0.3 Ga)';
  }

  // --- Differential CSFD & Area Slope Correction Solvers ---

  /**
   * Compute Differential Crater Size-Frequency Distribution (DFD): d(D) = deltaN / (deltaD * Area).
   * @param {Array<{diameter: number}>} craters - Crater records with diameter in meters
   * @param {number} [countAreaKm2=1e6] - Total counting area in km^2
   * @param {number} [binFactor=Math.SQRT2] - Logarithmic bin step factor (sqrt(2) standard)
   * @returns {Array<{dMin: number, dMax: number, dCenter: number, count: number, differentialDensity: number}>}
   */
  static computeDifferentialCSFD(craters = [], countAreaKm2 = 1e6, binFactor = Math.SQRT2) {
    if (!craters || craters.length === 0) return [];

    const diametersKm = craters
      .map(c => (typeof c.diameter === 'number' ? c.diameter / 1000 : 1.0))
      .filter(d => d > 0);

    const diffBins = [];
    let dMin = 0.1;

    for (let b = 0; b < 12; b++) {
      const dMax = dMin * binFactor;
      const dCenter = Math.sqrt(dMin * dMax);
      const deltaD = dMax - dMin;

      const inBin = diametersKm.filter(d => d >= dMin && d < dMax).length;
      const dDensity = inBin / (deltaD * countAreaKm2);

      diffBins.push({
        dMin: parseFloat(dMin.toFixed(3)),
        dMax: parseFloat(dMax.toFixed(3)),
        dCenter: parseFloat(dCenter.toFixed(3)),
        count: inBin,
        differentialDensity: parseFloat(dDensity.toExponential(4))
      });

      dMin = dMax;
    }

    return diffBins;
  }

  /**
   * Correct projected counting area for local terrain slope angle.
   * A_true = A_proj / cos(theta)
   * @param {number} projectedAreaKm2 - Map-projected 2D area in km^2
   * @param {number} meanSlopeDeg - Mean slope of the counting region in degrees
   * @returns {{trueAreaKm2: number, areaExpansionFactor: number}}
   */
  static computeSlopeCorrectedArea(projectedAreaKm2, meanSlopeDeg = 0) {
    const slopeRad = Math.abs(meanSlopeDeg) * Math.PI / 180.0;
    const cosSlope = Math.max(0.1, Math.cos(slopeRad));
    const trueArea = projectedAreaKm2 / cosSlope;

    return {
      trueAreaKm2: parseFloat(trueArea.toFixed(3)),
      areaExpansionFactor: parseFloat((1.0 / cosSlope).toFixed(4))
    };
  }
}



