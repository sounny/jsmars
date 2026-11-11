/**
 * @module solarEphemeris
 * @description Astronomical subsolar point and body-fixed solar vector calculator
 * for planetary globes across the Solar System.
 *
 * Combines:
 * 1. JPL J2000 Keplerian planetary heliocentric orbital elements (Standish 1992 /
 *    JPL Solar System Dynamics, plus Dawn IAU osculating elements for Ceres & Vesta).
 * 2. Official IAU Working Group on Cartographic Coordinates and Rotational Elements
 *    (WGCCRE, Archinal et al. 2018, Celestial Mechanics and Dynamical Astronomy 130:22)
 *    north-pole (alpha0, delta0) and prime-meridian (W0, Wdot) orientation models.
 *
 * Output coordinates are planetocentric latitude [-90, +90] degrees North and
 * body-fixed East longitude [-180, +180] degrees East, plus normalized body-fixed
 * ECEF Cartesian vectors:
 *   - `sunDirectionFixed`: unit vector from body center toward the Sun.
 *   - `lightDirectionFixed`: unit vector in the direction sunlight travels
 *     (`-sunDirectionFixed`), matching `Cesium.DirectionalLight({ direction })`.
 */

const DEG_TO_RAD = Math.PI / 180;
const RAD_TO_DEG = 180 / Math.PI;
const J2000_JD = 2451545.0;
const UNIX_EPOCH_JD = 2440587.5;
const TT_MINUS_UTC_SECONDS = 69.184;
const J2000_OBLIQUITY_DEG = 23.43929111;

/**
 * JPL J2000 heliocentric ecliptic Keplerian elements and centennial rates:
 * `[a_AU, e, I_deg, L_deg, longPeri_deg, longNode_deg]`
 * @type {Record<string, {el0: number[], rate: number[]}>}
 */
export const HELIOCENTRIC_ORBITS = Object.freeze({
  mercury: {
    el0: [0.38709927, 0.20563593, 7.00497902, 252.25032350, 77.45779628, 48.33076593],
    rate: [0.00000037, 0.00001906, -0.00594749, 149472.67411175, 0.16047689, -0.12534081]
  },
  venus: {
    el0: [0.72333566, 0.00677672, 3.39467605, 181.97909950, 131.60246718, 76.67984255],
    rate: [0.00000390, -0.00004107, -0.00078890, 58517.81538729, 0.00268329, -0.27769418]
  },
  earth: {
    el0: [1.00000261, 0.01671123, -0.00001531, 100.46457166, 102.93768193, 0.0],
    rate: [0.00000562, -0.00004392, -0.01294668, 35999.37244981, 0.32327364, 0.0]
  },
  mars: {
    el0: [1.52371034, 0.09339410, 1.84969142, -4.55343205, -23.94362959, 49.55953891],
    rate: [0.00001847, 0.00007882, -0.00813131, 19140.30268499, 0.44441088, -0.29257343]
  },
  vesta: {
    el0: [2.361418, 0.088740, 7.14177, 295.694, 254.901, 103.810],
    rate: [0.0, 0.0, 0.0, 9915.08, 0.0, 0.0]
  },
  ceres: {
    el0: [2.765956, 0.078375, 10.58862, 153.942, 151.599, 80.268],
    rate: [0.0, 0.0, 0.0, 7819.78, 0.0, 0.0]
  },
  jupiter: {
    el0: [5.20288700, 0.04838624, 1.30439695, 34.39644051, 14.72847983, 100.47390909],
    rate: [-0.00011607, -0.00013253, -0.00183714, 3034.74612775, 0.21252668, 0.20469106]
  },
  saturn: {
    el0: [9.53667594, 0.05386179, 2.48599187, 49.95424423, 92.59887831, 113.66242448],
    rate: [-0.00125060, -0.00050991, 0.00193609, 1222.49362201, -0.41897216, -0.28867794]
  },
  uranus: {
    el0: [19.18916464, 0.04725744, 0.77263783, 313.23810451, 170.95427630, 74.01692503],
    rate: [-0.00196176, -0.00004397, -0.00242939, 428.48202785, 0.40805281, 0.04240589]
  },
  neptune: {
    el0: [30.06992276, 0.00859048, 1.77004347, -55.12002969, 44.96476227, 131.78422574],
    rate: [0.00026291, 0.00005105, 0.00035372, 218.45945325, -0.32241464, -0.00508664]
  },
  pluto: {
    el0: [39.48211675, 0.24882730, 17.14001206, 238.92903833, 224.06891629, 110.30393684],
    rate: [-0.00031596, 0.00005170, 0.00004818, 145.20780515, -0.04062942, -0.01183482]
  }
});

/**
 * IAU WGCCRE (Archinal et al. 2018) rotational elements per celestial body:
 * `alpha0 = a0 + aRate * T`, `delta0 = d0 + dRate * T`, `W = w0 + wRate * d`.
 * Note: Earth uses UT1 (`useUt1ForW: true`) for Greenwich sidereal angle, and
 * Moon includes the dominant IAU nodal precession term `E1`.
 * @type {Record<string, {orbit: string, a0: number, aRate?: number, d0: number, dRate?: number, w0: number, wRate: number, useUt1ForW?: boolean, lunarLibration?: boolean}>}
 */
export const IAU_ROTATION_MODELS = Object.freeze({
  mercury:   { orbit: 'mercury', a0: 281.0103, aRate: -0.0328, d0: 61.4155, dRate: -0.0049, w0: 329.5988, wRate: 6.1385108 },
  venus:     { orbit: 'venus',   a0: 272.76,   aRate: 0.0,     d0: 67.16,   dRate: 0.0,     w0: 160.20,   wRate: -1.4813688 },
  earth:     { orbit: 'earth',   a0: 0.0,      aRate: -0.641,  d0: 90.0,    dRate: -0.557,  w0: 190.147,  wRate: 360.9856235, useUt1ForW: true },
  moon:      { orbit: 'earth',   a0: 269.9949, aRate: 0.0031,  d0: 66.5392, dRate: 0.0130,  w0: 38.3213,  wRate: 13.17635815, lunarLibration: true },
  mars:      { orbit: 'mars',    a0: 317.68143, aRate: -0.1061, d0: 52.88650, dRate: -0.0609, w0: 176.630, wRate: 350.89198226 },
  phobos:    { orbit: 'mars',    a0: 317.68,   aRate: -0.108,  d0: 52.90,   dRate: -0.061,  w0: 35.06,    wRate: 1128.8445850 },
  deimos:    { orbit: 'mars',    a0: 316.65,   aRate: -0.108,  d0: 53.52,   dRate: -0.061,  w0: 79.41,    wRate: 285.1618970 },
  ceres:     { orbit: 'ceres',   a0: 291.427,  aRate: 0.0,     d0: 66.764,  dRate: 0.0,     w0: 170.650,  wRate: 952.1532 },
  vesta:     { orbit: 'vesta',   a0: 309.031,  aRate: 0.0,     d0: 42.235,  dRate: 0.0,     w0: 285.39,   wRate: 1617.3329428 },
  jupiter:   { orbit: 'jupiter', a0: 268.056595, aRate: -0.006499, d0: 64.495303, dRate: 0.002413, w0: 284.95, wRate: 870.5360000 },
  io:        { orbit: 'jupiter', a0: 268.05,   aRate: -0.009,  d0: 64.50,   dRate: 0.003,   w0: 200.39,   wRate: 203.4889538 },
  europa:    { orbit: 'jupiter', a0: 268.08,   aRate: -0.009,  d0: 64.51,   dRate: 0.003,   w0: 36.022,   wRate: 101.3747235 },
  ganymede:  { orbit: 'jupiter', a0: 268.20,   aRate: -0.009,  d0: 64.57,   dRate: 0.003,   w0: 44.064,   wRate: 50.3176081 },
  callisto:  { orbit: 'jupiter', a0: 268.72,   aRate: -0.009,  d0: 64.83,   dRate: 0.003,   w0: 259.51,   wRate: 21.5710715 },
  saturn:    { orbit: 'saturn',  a0: 40.589,   aRate: -0.036,  d0: 83.537,  dRate: -0.004,  w0: 38.90,    wRate: 810.7939024 },
  mimas:     { orbit: 'saturn',  a0: 40.66,    aRate: -0.036,  d0: 83.52,   dRate: -0.004,  w0: 333.46,   wRate: 381.9945550 },
  enceladus: { orbit: 'saturn',  a0: 40.66,    aRate: -0.036,  d0: 83.52,   dRate: -0.004,  w0: 6.32,     wRate: 262.7318996 },
  tethys:    { orbit: 'saturn',  a0: 40.66,    aRate: -0.036,  d0: 83.52,   dRate: -0.004,  w0: 8.95,     wRate: 190.6979085 },
  dione:     { orbit: 'saturn',  a0: 40.66,    aRate: -0.036,  d0: 83.52,   dRate: -0.004,  w0: 357.00,   wRate: 131.5349316 },
  rhea:      { orbit: 'saturn',  a0: 40.38,    aRate: -0.036,  d0: 83.55,   dRate: -0.004,  w0: 235.16,   wRate: 79.6900478 },
  titan:     { orbit: 'saturn',  a0: 39.4827,  aRate: 0.0,     d0: 83.4279, dRate: 0.0,     w0: 186.5855, wRate: 22.5769768 },
  iapetus:   { orbit: 'saturn',  a0: 318.16,   aRate: -3.949,  d0: 75.03,   dRate: -1.143,  w0: 355.20,   wRate: 4.5379572 },
  phoebe:    { orbit: 'saturn',  a0: 356.90,   aRate: 0.0,     d0: 77.80,   dRate: 0.0,     w0: 304.70,   wRate: 930.8338720 },
  miranda:   { orbit: 'uranus',  a0: 257.43,   aRate: 0.0,     d0: -15.08,  dRate: 0.0,     w0: 30.70,    wRate: -254.6906892 },
  ariel:     { orbit: 'uranus',  a0: 257.43,   aRate: 0.0,     d0: -15.10,  dRate: 0.0,     w0: 156.22,   wRate: -142.8356681 },
  umbriel:   { orbit: 'uranus',  a0: 257.43,   aRate: 0.0,     d0: -15.10,  dRate: 0.0,     w0: 108.05,   wRate: -86.8688923 },
  titania:   { orbit: 'uranus',  a0: 257.43,   aRate: 0.0,     d0: -15.10,  dRate: 0.0,     w0: 77.74,    wRate: -41.3514316 },
  oberon:    { orbit: 'uranus',  a0: 257.43,   aRate: 0.0,     d0: -15.10,  dRate: 0.0,     w0: 6.77,     wRate: -26.7394932 },
  triton:    { orbit: 'neptune', a0: 299.36,   aRate: 0.0,     d0: 41.17,   dRate: 0.0,     w0: 296.53,   wRate: -61.2572637 },
  pluto:     { orbit: 'pluto',   a0: 132.993,  aRate: 0.0,     d0: -6.163,  dRate: 0.0,     w0: 302.695,  wRate: 56.3625225 },
  charon:    { orbit: 'pluto',   a0: 132.993,  aRate: 0.0,     d0: -6.163,  dRate: 0.0,     w0: 122.695,  wRate: 56.3625225 }
});

function wrap180(deg) {
  let out = ((deg + 180) % 360 + 360) % 360 - 180;
  if (Object.is(out, -0)) out = 0;
  return out;
}

function rotateFrameZ(v, angleRad) {
  const c = Math.cos(angleRad);
  const s = Math.sin(angleRad);
  return [
    v[0] * c + v[1] * s,
    -v[0] * s + v[1] * c,
    v[2]
  ];
}

function rotateFrameX(v, angleRad) {
  const c = Math.cos(angleRad);
  const s = Math.sin(angleRad);
  return [
    v[0],
    v[1] * c + v[2] * s,
    -v[1] * s + v[2] * c
  ];
}

/**
 * Compute heliocentric ICRF (J2000 equatorial) position vector `[x, y, z]` in AU.
 * @param {string} orbitKey
 * @param {number} centuriesT - Julian centuries TT from J2000.0
 * @returns {[number, number, number]}
 */
export function computeHeliocentricIcrfAU(orbitKey, centuriesT) {
  const spec = HELIOCENTRIC_ORBITS[orbitKey] || HELIOCENTRIC_ORBITS.mars;
  const el = spec.el0.map((v, i) => v + (spec.rate[i] || 0) * centuriesT);
  const [a, e, incDeg, meanLonDeg, longPeriDeg, longNodeDeg] = el;

  const argPeri = (longPeriDeg - longNodeDeg) * DEG_TO_RAD;
  const node = longNodeDeg * DEG_TO_RAD;
  const inc = incDeg * DEG_TO_RAD;
  const meanAnomaly = ((((meanLonDeg - longPeriDeg) % 360) + 360) % 360) * DEG_TO_RAD;

  // Solve Kepler's equation E - e sin(E) = M via Newton-Raphson
  let E = meanAnomaly + e * Math.sin(meanAnomaly);
  for (let iter = 0; iter < 8; iter++) {
    const f = E - e * Math.sin(E) - meanAnomaly;
    const fp = 1 - e * Math.cos(E);
    const step = f / fp;
    E -= step;
    if (Math.abs(step) < 1e-12) break;
  }

  const xOrb = a * (Math.cos(E) - e);
  const yOrb = a * Math.sqrt(Math.max(0, 1 - e * e)) * Math.sin(E);

  const cw = Math.cos(argPeri);
  const sw = Math.sin(argPeri);
  const cO = Math.cos(node);
  const sO = Math.sin(node);
  const cI = Math.cos(inc);
  const sI = Math.sin(inc);

  const xEcl = (cw * cO - sw * sO * cI) * xOrb + (-sw * cO - cw * sO * cI) * yOrb;
  const yEcl = (cw * sO + sw * cO * cI) * xOrb + (-sw * sO + cw * cO * cI) * yOrb;
  const zEcl = (sw * sI) * xOrb + (cw * sI) * yOrb;

  const eps = J2000_OBLIQUITY_DEG * DEG_TO_RAD;
  const ce = Math.cos(eps);
  const se = Math.sin(eps);

  return [
    xEcl,
    yEcl * ce - zEcl * se,
    yEcl * se + zEcl * ce
  ];
}

/**
 * Convert planetocentric subsolar `(latDeg, lonEastDeg)` into normalized body-fixed
 * ECEF vectors (`sunDirectionFixed` toward the Sun, `lightDirectionFixed` photon propagation).
 * @param {number} subSolarLatDeg
 * @param {number} subSolarLonEastDeg
 * @returns {{sunDirectionFixed: [number, number, number], lightDirectionFixed: [number, number, number]}}
 */
export function subSolarToFixedDirections(subSolarLatDeg, subSolarLonEastDeg) {
  const phi = Math.max(-90, Math.min(90, Number(subSolarLatDeg) || 0)) * DEG_TO_RAD;
  const lam = (Number(subSolarLonEastDeg) || 0) * DEG_TO_RAD;
  const cosPhi = Math.cos(phi);
  const sx = cosPhi * Math.cos(lam);
  const sy = cosPhi * Math.sin(lam);
  const sz = Math.sin(phi);
  const len = Math.hypot(sx, sy, sz) || 1;
  const sunDirectionFixed = [sx / len, sy / len, sz / len];
  const lightDirectionFixed = [-sunDirectionFixed[0], -sunDirectionFixed[1], -sunDirectionFixed[2]];
  return { sunDirectionFixed, lightDirectionFixed };
}

/**
 * Compute exact IAU WGCCRE + JPL Keplerian subsolar coordinates and body-fixed light
 * direction for any JSMARS celestial body at the requested UTC timestamp.
 *
 * @param {string} bodyKey - Canonical lowercase body key (e.g. `'mars'`, `'moon'`, `'mercury'`, `'titan'`)
 * @param {Date|number|string} [date=new Date()] - UTC Date, epoch ms, or ISO timestamp
 * @returns {{
 *   body: string,
 *   date: Date,
 *   utcIso: string,
 *   heliocentricDistanceAU: number,
 *   subSolarLatDeg: number,
 *   subSolarLonEastDeg: number,
 *   sunDirectionFixed: [number, number, number],
 *   lightDirectionFixed: [number, number, number],
 *   source: string,
 *   mode: 'ephemeris'
 * }}
 */
export function computeSubSolarPointForBody(bodyKey = 'mars', date = new Date()) {
  const key = String(bodyKey || 'mars').trim().toLowerCase();
  const model = IAU_ROTATION_MODELS[key] || IAU_ROTATION_MODELS.mars;
  const resolvedDate = date instanceof Date && !Number.isNaN(date.getTime())
    ? new Date(date.getTime())
    : new Date(date);
  const safeDate = Number.isNaN(resolvedDate.getTime()) ? new Date() : resolvedDate;

  const jdUtc = UNIX_EPOCH_JD + safeDate.getTime() / 86400000;
  const jdTt = jdUtc + TT_MINUS_UTC_SECONDS / 86400;
  const dTt = jdTt - J2000_JD;
  const dUt1 = jdUtc - J2000_JD;
  const T = dTt / 36525;

  const [rx, ry, rz] = computeHeliocentricIcrfAU(model.orbit, T);
  const rNorm = Math.hypot(rx, ry, rz) || 1;
  const sunIcrf = [-rx / rNorm, -ry / rNorm, -rz / rNorm];

  let alpha0 = model.a0 + (model.aRate || 0) * T;
  let delta0 = model.d0 + (model.dRate || 0) * T;
  let W = model.w0 + model.wRate * (model.useUt1ForW ? dUt1 : dTt);

  if (model.lunarLibration) {
    const E1 = (125.045 - 0.0529921 * dTt) * DEG_TO_RAD;
    alpha0 -= 3.8787 * Math.sin(E1);
    delta0 += 1.5419 * Math.cos(E1);
    W += 3.5610 * Math.sin(E1);
  }

  let bf = rotateFrameZ(sunIcrf, (90 + alpha0) * DEG_TO_RAD);
  bf = rotateFrameX(bf, (90 - delta0) * DEG_TO_RAD);
  bf = rotateFrameZ(bf, W * DEG_TO_RAD);

  const bfLen = Math.hypot(bf[0], bf[1], bf[2]) || 1;
  const sx = bf[0] / bfLen;
  const sy = bf[1] / bfLen;
  const sz = bf[2] / bfLen;

  const subSolarLatDeg = Math.asin(Math.max(-1, Math.min(1, sz))) * RAD_TO_DEG;
  const subSolarLonEastDeg = wrap180(Math.atan2(sy, sx) * RAD_TO_DEG);

  return {
    body: IAU_ROTATION_MODELS[key] ? key : 'mars',
    date: safeDate,
    utcIso: safeDate.toISOString(),
    heliocentricDistanceAU: rNorm,
    subSolarLatDeg,
    subSolarLonEastDeg,
    sunDirectionFixed: [sx, sy, sz],
    lightDirectionFixed: [-sx, -sy, -sz],
    source: 'IAU WGCCRE (Archinal et al. 2018) + JPL J2000 Ephemeris',
    mode: 'ephemeris'
  };
}

/**
 * Compute synthetic local-solar-hour illumination anchored to a target longitude
 * while preserving the body's real seasonal solar declination at `date`.
 * Explicitly tagged `mode: 'local'` (`[Model]`) so it is never confused with live UTC ephemeris.
 *
 * @param {string} bodyKey
 * @param {number} localSolarHour - Local solar hour [0, 24] at `referenceLonEastDeg` (`12` = local noon)
 * @param {number} [referenceLonEastDeg=0] - Target East longitude where `localSolarHour` applies
 * @param {Date|number|string} [date=new Date()] - Reference date for seasonal solar declination
 */
export function computeModeledLocalSolarLight(
  bodyKey = 'mars',
  localSolarHour = 12,
  referenceLonEastDeg = 0,
  date = new Date()
) {
  const ephemeris = computeSubSolarPointForBody(bodyKey, date);
  const clampedHour = Math.max(0, Math.min(24, Number.isFinite(localSolarHour) ? localSolarHour : 12));
  const refLon = Number.isFinite(referenceLonEastDeg) ? referenceLonEastDeg : 0;
  const subSolarLonEastDeg = wrap180(refLon + (12 - clampedHour) * 15);
  const subSolarLatDeg = ephemeris.subSolarLatDeg;
  const { sunDirectionFixed, lightDirectionFixed } = subSolarToFixedDirections(subSolarLatDeg, subSolarLonEastDeg);

  return {
    body: ephemeris.body,
    date: ephemeris.date,
    utcIso: ephemeris.utcIso,
    localSolarHour: clampedHour,
    referenceLonEastDeg: wrap180(refLon),
    subSolarLatDeg,
    subSolarLonEastDeg,
    sunDirectionFixed,
    lightDirectionFixed,
    source: 'Modeled Local Solar Hour (seasonal IAU declination)',
    mode: 'local'
  };
}

/**
 * Format subsolar coordinates concisely for the 3D globe HUD.
 * @param {number} latDeg
 * @param {number} lonEastDeg
 * @returns {string} e.g. `"20.4°S, 149.5°W"`
 */
export function formatSubSolarCoord(latDeg, lonEastDeg) {
  const latHemi = latDeg >= 0 ? 'N' : 'S';
  const lonWrapped = wrap180(lonEastDeg);
  const lonHemi = lonWrapped >= 0 ? 'E' : 'W';
  return `${Math.abs(latDeg).toFixed(1)}\u00b0${latHemi}, ${Math.abs(lonWrapped).toFixed(1)}\u00b0${lonHemi}`;
}
