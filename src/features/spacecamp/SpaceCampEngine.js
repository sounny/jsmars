/**
 * @module spacecamp/SpaceCampEngine
 * @description Core progression, geospatial target verification, and empirical physics
 * scoring engine for the 5-Round JSMARS Space Camp curriculum (600 max points).
 */

import { haversineDistance, to180 } from '../../util/geo.js';
import { loadLocalProgress, saveLocalProgress } from './SpaceCampCookies.js';

/**
 * Compute initial forward azimuth bearing (0..360 deg) and 8-wind compass sector.
 * @param {number} lat1
 * @param {number} lon1
 * @param {number} lat2
 * @param {number} lon2
 * @returns {{ bearingDeg: number, compass: string }}
 */
export function computeCompassBearing(lat1, lon1, lat2, lon2) {
  const toRad = Math.PI / 180;
  const phi1 = lat1 * toRad;
  const phi2 = lat2 * toRad;
  const dLon = (to180(lon2) - to180(lon1)) * toRad;
  const y = Math.sin(dLon) * Math.cos(phi2);
  const x = Math.cos(phi1) * Math.sin(phi2) - Math.sin(phi1) * Math.cos(phi2) * Math.cos(dLon);
  const brng = ((Math.atan2(y, x) * 180) / Math.PI + 360) % 360;
  const dirs = ['N', 'NE', 'E', 'SE', 'S', 'SW', 'W', 'NW'];
  const compass = dirs[Math.round(brng / 45) % 8];
  return { bearingDeg: Math.round(brng), compass };
}

/**
 * Verify whether the scanner reticle `(lat, lon)` on `currentBody` locks onto `target`.
 * @param {object} target
 * @param {string} currentBody
 * @param {number} lat
 * @param {number} lon
 * @param {number} [hintsUsed=0]
 * @returns {{ matched: boolean, wrongBody?: boolean, requiredBody?: string, distanceKm?: number, compass?: string, pointsAwarded: number }}
 */
export function verifyScanTarget(target, currentBody, lat, lon, hintsUsed = 0) {
  const reqBody = String(target.body || 'mars').toLowerCase();
  const actBody = String(currentBody || '').toLowerCase();
  if (actBody !== reqBody) {
    return {
      matched: false,
      wrongBody: true,
      requiredBody: reqBody,
      pointsAwarded: 0
    };
  }

  const distKm = haversineDistance(lat, to180(lon), target.lat, to180(target.lon), reqBody);
  const polarMatch = Number.isFinite(target.polarSouthLat) && lat <= target.polarSouthLat;

  if (distKm <= target.radiusKm || polarMatch) {
    const deduction = Math.max(0, hintsUsed) * (target.hintCost || 4);
    const floorPts = Math.max(10, Math.floor(target.points * 0.5));
    const pointsAwarded = Math.max(floorPts, target.points - deduction);
    return {
      matched: true,
      distanceKm: Math.round(distKm),
      pointsAwarded
    };
  }

  const { compass } = computeCompassBearing(lat, lon, target.lat, target.lon);
  return {
    matched: false,
    wrongBody: false,
    distanceKm: Math.round(distKm),
    compass,
    pointsAwarded: 0
  };
}

/**
 * Score Round 3 JSMARS Rover Mission Planning Tool choices (0..30 pts).
 * @param {string[]} selectedIds
 * @param {Array<{ id: string, correct: boolean }>} toolChoices
 * @returns {{ points: number, correctCount: number, wrongCount: number }}
 */
export function scoreRoverTools(selectedIds = [], toolChoices = []) {
  const picked = new Set(selectedIds);
  let correctCount = 0;
  let wrongCount = 0;
  for (const choice of toolChoices) {
    if (!picked.has(choice.id)) continue;
    if (choice.correct) correctCount += 1;
    else wrongCount += 1;
  }
  const raw = correctCount * 10 - wrongCount * 5;
  return {
    points: Math.max(0, Math.min(30, raw)),
    correctCount,
    wrongCount
  };
}

/**
 * Resample a multi-waypoint polyline into `sampleCount` evenly-spaced points for DEM profiling.
 * @param {Array<{ lat: number, lon: number }>} waypoints
 * @param {number} [sampleCount=32]
 * @returns {Array<{ lat: number, lon: number }>}
 */
export function interpolateWaypoints(waypoints, sampleCount = 32) {
  if (!Array.isArray(waypoints) || waypoints.length < 2) return waypoints || [];
  const segLengths = [];
  let totalKm = 0;
  for (let i = 0; i < waypoints.length - 1; i++) {
    const d = haversineDistance(
      waypoints[i].lat,
      to180(waypoints[i].lon),
      waypoints[i + 1].lat,
      to180(waypoints[i + 1].lon),
      'mars'
    );
    segLengths.push(d);
    totalKm += d;
  }
  if (totalKm <= 0.001) return waypoints.slice(0, 2);

  const samples = [];
  for (let s = 0; s < sampleCount; s++) {
    const targetDist = (s / (sampleCount - 1)) * totalKm;
    let acc = 0;
    let chosen = waypoints[waypoints.length - 1];
    for (let i = 0; i < segLengths.length; i++) {
      const seg = segLengths[i];
      if (acc + seg >= targetDist || i === segLengths.length - 1) {
        const t = seg > 0 ? Math.max(0, Math.min(1, (targetDist - acc) / seg)) : 0;
        const a = waypoints[i];
        const b = waypoints[i + 1];
        const lonA = to180(a.lon);
        let dLon = to180(b.lon) - lonA;
        if (dLon > 180) dLon -= 360;
        if (dLon < -180) dLon += 360;
        chosen = {
          lat: a.lat + (b.lat - a.lat) * t,
          lon: to180(lonA + dLon * t)
        };
        break;
      }
      acc += seg;
    }
    samples.push(chosen);
  }
  return samples;
}

/**
 * Score a student's Mars rover traverse path using sampled MOLA elevations (0..75 pts).
 * @param {Array<{ lat: number, lon: number }>} sampledPoints
 * @param {Array<number|null>} elevationsM
 * @param {{ lat: number, lon: number }} start
 * @param {{ lat: number, lon: number, captureRadiusKm?: number }} goal
 * @returns {{
 *   totalPoints: number,
 *   arrivalPoints: number,
 *   slopePoints: number,
 *   efficiencyPoints: number,
 *   distanceKm: number,
 *   goalDistKm: number,
 *   startDistKm: number,
 *   maxSlopeDeg: number,
 *   totalClimbM: number,
 *   reachedGoal: boolean,
 *   usedLiveDem: boolean
 * }}
 */
export function scoreMarsRoverTraverse(sampledPoints, elevationsM = [], start, goal) {
  if (!Array.isArray(sampledPoints) || sampledPoints.length < 2) {
    return {
      totalPoints: 0,
      arrivalPoints: 0,
      slopePoints: 0,
      efficiencyPoints: 0,
      distanceKm: 0,
      goalDistKm: 999,
      startDistKm: 999,
      maxSlopeDeg: 0,
      totalClimbM: 0,
      reachedGoal: false,
      usedLiveDem: false
    };
  }

  const first = sampledPoints[0];
  const last = sampledPoints[sampledPoints.length - 1];
  const startDistKm = haversineDistance(first.lat, to180(first.lon), start.lat, to180(start.lon), 'mars');
  const goalDistKm = haversineDistance(last.lat, to180(last.lon), goal.lat, to180(goal.lon), 'mars');
  const goalRadius = goal.captureRadiusKm || 22;

  let distanceKm = 0;
  let maxSlopeDeg = 0;
  let totalClimbM = 0;
  let validElevPairs = 0;

  for (let i = 0; i < sampledPoints.length - 1; i++) {
    const segKm = haversineDistance(
      sampledPoints[i].lat,
      to180(sampledPoints[i].lon),
      sampledPoints[i + 1].lat,
      to180(sampledPoints[i + 1].lon),
      'mars'
    );
    distanceKm += segKm;

    const z1 = elevationsM?.[i];
    const z2 = elevationsM?.[i + 1];
    if (Number.isFinite(z1) && Number.isFinite(z2) && segKm > 0.05) {
      validElevPairs += 1;
      const dz = z2 - z1;
      if (dz > 0) totalClimbM += dz;
      const slopeDeg = (Math.atan(Math.abs(dz) / (segKm * 1000)) * 180) / Math.PI;
      if (slopeDeg > maxSlopeDeg) maxSlopeDeg = slopeDeg;
    }
  }

  // 1. Arrival & Start/Goal Fidelity (0..25 pts)
  const reachedGoal = goalDistKm <= goalRadius && startDistKm <= 30;
  let arrivalPoints = 0;
  if (reachedGoal) {
    arrivalPoints = 25;
  } else {
    const goalScore = Math.max(0, Math.round(18 * (1 - Math.min(1, (goalDistKm - goalRadius) / 80))));
    const startScore = startDistKm <= 30 ? 7 : Math.max(0, Math.round(7 * (1 - Math.min(1, (startDistKm - 30) / 60))));
    arrivalPoints = Math.min(20, goalScore + startScore);
  }

  // 2. Slope & Climb Safety from MOLA DEM (0..35 pts)
  let slopePoints = 0;
  if (validElevPairs > 0) {
    let gradeBase = 35;
    if (maxSlopeDeg <= 5.0) {
      gradeBase = 35;
    } else if (maxSlopeDeg <= 10.0) {
      gradeBase = Math.round(35 - (maxSlopeDeg - 5.0) * 2.4); // 23..35
    } else if (maxSlopeDeg <= 18.0) {
      gradeBase = Math.round(23 - (maxSlopeDeg - 10.0) * 1.8); // 9..23
    } else {
      gradeBase = Math.max(3, Math.round(9 - (maxSlopeDeg - 18.0) * 0.5));
    }
    const climbPenalty = totalClimbM > 1250 ? Math.min(8, Math.round((totalClimbM - 1250) / 180)) : 0;
    slopePoints = Math.max(3, gradeBase - climbPenalty);
    if (!reachedGoal) slopePoints = Math.round(slopePoints * 0.6);
  } else {
    // Graceful partial score if remote MOLA S3 COG is unreachable on restricted school network
    slopePoints = reachedGoal ? 24 : 10;
  }

  // 3. Traverse Distance & Battery Efficiency (0..15 pts)
  const directKm = haversineDistance(start.lat, to180(start.lon), goal.lat, to180(goal.lon), 'mars');
  let efficiencyPoints = 0;
  if (distanceKm >= directKm * 0.85 && distanceKm <= directKm * 1.65) {
    efficiencyPoints = 15;
  } else if (distanceKm > directKm * 1.65 && distanceKm <= directKm * 2.8) {
    efficiencyPoints = Math.max(6, Math.round(15 - ((distanceKm / directKm - 1.65) * 8)));
  } else if (distanceKm > 5) {
    efficiencyPoints = 5;
  }
  if (!reachedGoal) efficiencyPoints = Math.min(efficiencyPoints, 6);

  const totalPoints = Math.max(0, Math.min(75, arrivalPoints + slopePoints + efficiencyPoints));

  return {
    totalPoints,
    arrivalPoints,
    slopePoints,
    efficiencyPoints,
    distanceKm: Number(distanceKm.toFixed(1)),
    goalDistKm: Number(goalDistKm.toFixed(1)),
    startDistKm: Number(startDistKm.toFixed(1)),
    maxSlopeDeg: Number(maxSlopeDeg.toFixed(2)),
    totalClimbM: Math.round(totalClimbM),
    reachedGoal,
    usedLiveDem: validElevPairs > 0
  };
}

/**
 * Score a Lunar South Pole landing site selection from real NASA LRO LOLA rasters (0..150 pts).
 * @param {{
 *   lat?: number,
 *   lon?: number,
 *   elev?: number,
 *   slopeMax?: number,
 *   slopeMean?: number,
 *   regSlopeDeg?: number,
 *   sun?: number,
 *   earth?: number,
 *   psr?: number,
 *   psrDist?: number
 * }} metrics
 * @returns {{
 *   totalPoints: number,
 *   slopePoints: number,
 *   sunPoints: number,
 *   earthPoints: number,
 *   icePoints: number,
 *   effectiveSlopeDeg: number,
 *   inPsr: boolean,
 *   ratingEn: string,
 *   ratingFr: string,
 *   adviceEn: string,
 *   adviceFr: string
 * }}
 */
export function scoreLunarLandingSite(metrics = {}) {
  const inPsr = Number.isFinite(metrics.psr) && metrics.psr > 0.5;
  const rawSlope = Number.isFinite(metrics.slopeMean)
    ? metrics.slopeMean
    : (Number.isFinite(metrics.slopeMax)
      ? metrics.slopeMax
      : (Number.isFinite(metrics.regSlopeDeg) ? metrics.regSlopeDeg : 9.0));

  // 1. Touchdown Slope Safety (0..45 pts)
  let slopePoints = 0;
  if (rawSlope <= 5.0) {
    slopePoints = 45;
  } else if (rawSlope <= 10.0) {
    slopePoints = Math.round(45 - (rawSlope - 5.0) * 3.0); // 30..45
  } else if (rawSlope <= 18.0) {
    slopePoints = Math.round(30 - (rawSlope - 10.0) * 2.5); // 10..30
  } else {
    slopePoints = Math.max(0, Math.round(10 - (rawSlope - 18.0) * 0.8));
  }

  // 2. Solar Power Illumination (0..45 pts)
  const sunFrac = Number.isFinite(metrics.sun) ? Math.max(0, Math.min(1, metrics.sun)) : 0;
  const sunPoints = inPsr ? 0 : Math.min(45, Math.round((sunFrac / 0.76) * 45));

  // 3. Direct-to-Earth Radio Visibility (0..30 pts)
  const earthFrac = Number.isFinite(metrics.earth) ? Math.max(0, Math.min(1, metrics.earth)) : 0;
  const earthPoints = Math.min(30, Math.round((earthFrac / 0.52) * 30));

  // 4. Water-Ice PSR Rover Proximity (0..30 pts)
  const psrDistM = Number.isFinite(metrics.psrDist) ? metrics.psrDist : 4000;
  let icePoints = 0;
  if (inPsr) {
    icePoints = 6; // Inside freezing PSR: ice underfoot, but fatal for solar power
  } else if (psrDistM >= 80 && psrDistM <= 2600) {
    icePoints = 30;
  } else if (psrDistM < 80) {
    icePoints = 25;
  } else if (psrDistM <= 7000) {
    icePoints = Math.round(30 - ((psrDistM - 2600) / 4400) * 14); // 16..30
  } else {
    icePoints = Math.max(6, Math.round(16 - (psrDistM - 7000) / 2500));
  }

  const totalPoints = Math.max(0, Math.min(150, slopePoints + sunPoints + earthPoints + icePoints));

  let ratingEn = 'Candidate Scouted';
  let ratingFr = 'Site Repéré';
  if (totalPoints >= 130) {
    ratingEn = 'Prime Artemis III Ridge!';
    ratingFr = 'Site Artemis III d’Excellence !';
  } else if (totalPoints >= 100) {
    ratingEn = 'Viable Lunar Outpost';
    ratingFr = 'Base Lunaire Viable';
  } else if (inPsr) {
    ratingEn = 'Hazard: Inside Freezing PSR Shadow!';
    ratingFr = 'Danger : Dans l’Ombre Permanente (-233 °C) !';
  } else {
    ratingEn = 'High Risk — Keep Refining!';
    ratingFr = 'Risque Élevé — Affinez Votre Choix !';
  }

  let adviceEn = 'Strong balance of flat terrain, polar sunlight, Earth comms, and nearby water ice.';
  let adviceFr = 'Excellent équilibre entre faible pente, soleil polaire, liaison Terre et glace proche.';
  if (inPsr) {
    adviceEn = 'You clicked inside a Permanently Shadowed crater! Move onto an adjacent sunlit ridge just outside the cyan PSR boundary.';
    adviceFr = 'Vous êtes dans un cratère d’ombre permanente ! Déplacez votre pin sur une crête éclairée juste au bord de la zone PSR.';
  } else if (rawSlope > 10) {
    adviceEn = `Slope is steep (${rawSlope.toFixed(1)}°). Select a high-res Artemis site (e.g. Connecting Ridge or Shackleton Rim) and switch Layer to 'Slope hazard' to find ≤ 5° terrain!`;
    adviceFr = `Pente trop forte (${rawSlope.toFixed(1)}°). Choisissez un site 20 m (Connecting Ridge ou Shackleton Rim) et affichez « Slope hazard » pour viser ≤ 5° !`;
  } else if (sunPoints < 25) {
    adviceEn = 'Low solar power! Switch Layer to "Avg solar visibility" and pick a bright yellow illuminated crest.';
    adviceFr = 'Ensoleillement faible ! Affichez « Avg solar visibility » et visez une crête jaune très éclairée.';
  } else if (earthPoints < 12) {
    adviceEn = 'Weak Earth radio visibility (far-side shadow). Switch Layer to "Avg Earth visibility" to restore direct comms!';
    adviceFr = 'Liaison radio Terre masquée ! Affichez « Avg Earth visibility » pour trouver une crête visible depuis la Terre.';
  }

  return {
    totalPoints,
    slopePoints,
    sunPoints,
    earthPoints,
    icePoints,
    effectiveSlopeDeg: Number(rawSlope.toFixed(1)),
    inPsr,
    ratingEn,
    ratingFr,
    adviceEn,
    adviceFr
  };
}

/**
 * Stateful Space Camp progression engine managing all 5 progressive rounds.
 */
export class SpaceCampEngine {
  /**
   * @param {object} deck - Parsed spacecamp-missions.json
   * @param {object} [savedState] - Optional restored state
   */
  constructor(deck, savedState = null) {
    this.deck = deck;
    const restored = savedState || loadLocalProgress() || {};
    this.state = {
      deckId: deck.deckId,
      startedAt: Number.isFinite(restored.startedAt) ? restored.startedAt : Date.now(),
      activeRound: Number.isFinite(restored.activeRound) ? restored.activeRound : 1,
      mysteryForceUnlocked: Boolean(restored.mysteryForceUnlocked),
      // Round 1: EO Land Cover
      r1Sites: restored.r1Sites || {}, // { [siteId]: { attempts: number, solved: boolean, points: number, chosen: string } }
      r1MeasurePts: Number(restored.r1MeasurePts) || 0,
      // Round 2: Mars Altimetry
      r2Targets: restored.r2Targets || {}, // { [targetId]: { hintsUsed: number, solved: boolean, points: number } }
      r2ProfilePts: Number(restored.r2ProfilePts) || 0,
      // Round 3: Mars Rover Traverse
      r3SelectedTools: Array.isArray(restored.r3SelectedTools) ? restored.r3SelectedTools : [],
      r3LastToolsCheck: restored.r3LastToolsCheck || null,
      r3BestToolsPts: Number(restored.r3BestToolsPts) || 0,
      r3BestRoutePts: Number(restored.r3BestRoutePts) || 0,
      r3LastRouteSummary: restored.r3LastRouteSummary || null,
      r3LabPts: Number(restored.r3LabPts) || 0,
      // Round 4: Lunar Landing Site
      r4BestSitePts: Number(restored.r4BestSitePts) || 0,
      r4LastSiteSummary: restored.r4LastSiteSummary || null,
      // Round 5: Europa Finale
      r5Scan: restored.r5Scan || { hintsUsed: 0, solved: false, points: 0 },
      r5MosaicPts: Number(restored.r5MosaicPts) || 0,
      r5CartoPts: Number(restored.r5CartoPts) || 0
    };
  }

  save(extraIdentity = {}) {
    saveLocalProgress({
      ...this.state,
      ...extraIdentity
    });
  }

  getRoundScores() {
    const r1SiteSum = Object.values(this.state.r1Sites).reduce((acc, s) => acc + (s?.points || 0), 0);
    const r1 = Math.min(60, r1SiteSum + (this.state.r1MeasurePts || 0));

    const r2TargetSum = Object.values(this.state.r2Targets).reduce((acc, t) => acc + (t?.points || 0), 0);
    const r2 = Math.min(90, r2TargetSum + (this.state.r2ProfilePts || 0));

    const r3 = Math.min(
      120,
      (this.state.r3BestToolsPts || 0) + (this.state.r3BestRoutePts || 0) + (this.state.r3LabPts || 0)
    );

    const r4 = Math.min(150, this.state.r4BestSitePts || 0);

    const r5 = Math.min(
      180,
      (this.state.r5Scan?.points || 0) + (this.state.r5MosaicPts || 0) + (this.state.r5CartoPts || 0)
    );

    return { r1, r2, r3, r4, r5, total: r1 + r2 + r3 + r4 + r5 };
  }

  getCompletedRoundsCount() {
    const s = this.getRoundScores();
    let done = 0;
    if (s.r1 >= 30) done += 1;
    if (s.r2 >= 50) done += 1;
    if (s.r3 >= 45) done += 1;
    if (s.r4 >= 60) done += 1;
    if (s.r5 >= 110) done += 1;
    return done;
  }

  getCompletedStepCount() {
    let steps = 0;
    steps += Object.values(this.state.r1Sites).filter((s) => s?.solved).length;
    if (this.state.r1MeasurePts > 0) steps += 1;
    steps += Object.values(this.state.r2Targets).filter((t) => t?.solved).length;
    if (this.state.r2ProfilePts > 0) steps += 1;
    if (this.state.r3BestToolsPts > 0) steps += 1;
    if (this.state.r3BestRoutePts > 0) steps += 1;
    if (this.state.r3LabPts > 0) steps += 1;
    if (this.state.r4BestSitePts > 0) steps += 1;
    if (this.state.r5Scan?.solved) steps += 1;
    if (this.state.r5MosaicPts > 0) steps += 1;
    if (this.state.r5CartoPts > 0) steps += 1;
    return steps;
  }

  isRound5Unlocked(nowMs = Date.now()) {
    if (this.state.mysteryForceUnlocked) return true;
    const s = this.getRoundScores();
    const roundsScored = [s.r1, s.r2, s.r3, s.r4].filter((v) => v > 0).length;
    const reqRounds = this.deck.mysteryUnlock?.requiredCompletedRounds || 3;
    const autoMs = (this.deck.mysteryUnlock?.autoUnlockMinutes || 15) * 60 * 1000;
    if (roundsScored >= reqRounds || nowMs - this.state.startedAt >= autoMs) {
      this.state.mysteryForceUnlocked = true;
      return true;
    }
    return false;
  }

  getSpoilerHiddenSet() {
    const hidden = new Set();
    const r2 = this.deck.rounds.find((r) => r.roundNumber === 2);
    if (r2) {
      if (!this.state.r2Targets?.r2_olympus?.solved) hidden.add('olympus mons');
      if (!this.state.r2Targets?.r2_valles?.solved) {
        ['valles marineris', 'melas chasma', 'candor chasma', 'coprates chasma'].forEach((n) => hidden.add(n));
      }
    }
    if (!this.state.r5Scan?.solved) {
      hidden.add('conamara chaos');
    }
    return hidden;
  }

  // ── Round 1 Actions ──────────────────────────────────────────────
  submitLandCoverGuess(siteId, chosenClass) {
    const round1 = this.deck.rounds.find((r) => r.roundNumber === 1);
    const site = round1?.sites?.find((s) => s.id === siteId);
    if (!site) return { ok: false };

    const prev = this.state.r1Sites[siteId] || { attempts: 0, solved: false, points: 0, chosen: '' };
    if (prev.solved) {
      return { ok: true, correct: true, alreadySolved: true, pointsAwarded: 0, totalSitePoints: prev.points };
    }

    const attempts = prev.attempts + 1;
    if (chosenClass === site.correctClass) {
      const points = attempts === 1 ? 12 : (attempts === 2 ? 8 : 5);
      this.state.r1Sites[siteId] = { attempts, solved: true, points, chosen: chosenClass };
      return { ok: true, correct: true, attempts, pointsAwarded: points, totalSitePoints: points };
    }

    this.state.r1Sites[siteId] = { attempts, solved: false, points: 0, chosen: chosenClass };
    return { ok: true, correct: false, attempts, pointsAwarded: 0, totalSitePoints: 0 };
  }

  awardRound1Measure() {
    if (this.state.r1MeasurePts > 0) return 0;
    this.state.r1MeasurePts = 12;
    return 12;
  }

  // ── Round 2 Actions ──────────────────────────────────────────────
  unlockRound2Hint(targetId, hintLevel) {
    const cur = this.state.r2Targets[targetId] || { hintsUsed: 0, solved: false, points: 0 };
    if (!cur.solved && hintLevel > cur.hintsUsed) {
      cur.hintsUsed = hintLevel;
      this.state.r2Targets[targetId] = cur;
    }
    return cur.hintsUsed;
  }

  scanRound2Target(targetId, currentBody, lat, lon) {
    const round2 = this.deck.rounds.find((r) => r.roundNumber === 2);
    const target = round2?.targets?.find((t) => t.id === targetId);
    if (!target) return { matched: false };

    const cur = this.state.r2Targets[targetId] || { hintsUsed: 0, solved: false, points: 0 };
    if (cur.solved) {
      return { matched: true, alreadySolved: true, pointsAwarded: 0 };
    }

    const res = verifyScanTarget(target, currentBody, lat, lon, cur.hintsUsed);
    if (res.matched) {
      this.state.r2Targets[targetId] = {
        hintsUsed: cur.hintsUsed,
        solved: true,
        points: res.pointsAwarded
      };
    }
    return res;
  }

  awardRound2Profile() {
    if (this.state.r2ProfilePts > 0) return 0;
    this.state.r2ProfilePts = 30;
    return 30;
  }

  // ── Round 3 Actions ──────────────────────────────────────────────
  evaluateRound3Tools(selectedToolIds) {
    const round3 = this.deck.rounds.find((r) => r.roundNumber === 3);
    const prevR3 = this.getRoundScores().r3;

    this.state.r3SelectedTools = Array.isArray(selectedToolIds) ? selectedToolIds.slice() : [];
    const toolRes = scoreRoverTools(this.state.r3SelectedTools, round3.toolChoices);
    if (toolRes.points > this.state.r3BestToolsPts) {
      this.state.r3BestToolsPts = toolRes.points;
    }

    const picked = new Set(this.state.r3SelectedTools);
    const essentialChoices = (round3.toolChoices || []).filter((c) => c.correct);
    const missingChoices = essentialChoices.filter((c) => !picked.has(c.id));
    const wrongChoices = (round3.toolChoices || []).filter((c) => !c.correct && picked.has(c.id));

    this.state.r3LastToolsCheck = {
      points: toolRes.points,
      maxPoints: 30,
      correctCount: toolRes.correctCount,
      wrongCount: toolRes.wrongCount,
      essentialTotal: essentialChoices.length,
      missingCount: missingChoices.length,
      missingIds: missingChoices.map((c) => c.id),
      wrongIds: wrongChoices.map((c) => c.id),
      checkedAt: Date.now()
    };

    const nextR3 = this.getRoundScores().r3;
    return {
      toolRes,
      check: this.state.r3LastToolsCheck,
      deltaPoints: Math.max(0, nextR3 - prevR3),
      roundTotal: nextR3
    };
  }

  evaluateRound3Plan(selectedToolIds, sampledPoints, elevationsM) {
    const round3 = this.deck.rounds.find((r) => r.roundNumber === 3);
    const prevR3 = this.getRoundScores().r3;

    this.state.r3SelectedTools = selectedToolIds.slice();
    const toolRes = scoreRoverTools(selectedToolIds, round3.toolChoices);
    if (toolRes.points > this.state.r3BestToolsPts) {
      this.state.r3BestToolsPts = toolRes.points;
    }

    const routeRes = scoreMarsRoverTraverse(sampledPoints, elevationsM, round3.start, round3.goal);
    this.state.r3LastRouteSummary = {
      ...routeRes,
      toolScore: toolRes.points
    };
    if (routeRes.totalPoints > this.state.r3BestRoutePts) {
      this.state.r3BestRoutePts = routeRes.totalPoints;
    }

    const nextR3 = this.getRoundScores().r3;
    return {
      toolRes,
      routeRes,
      deltaPoints: Math.max(0, nextR3 - prevR3),
      roundTotal: nextR3
    };
  }

  awardRound3Lab() {
    if (this.state.r3LabPts > 0) return 0;
    this.state.r3LabPts = 15;
    return 15;
  }

  // ── Round 4 Actions ──────────────────────────────────────────────
  evaluateRound4LunarSite(metrics, siteLabel = '') {
    const prevR4 = this.getRoundScores().r4;
    const scored = scoreLunarLandingSite(metrics);
    this.state.r4LastSiteSummary = {
      ...scored,
      siteLabel,
      lat: metrics.lat,
      lon: metrics.lon
    };
    if (scored.totalPoints > this.state.r4BestSitePts) {
      this.state.r4BestSitePts = scored.totalPoints;
    }
    const nextR4 = this.getRoundScores().r4;
    return {
      scored,
      deltaPoints: Math.max(0, nextR4 - prevR4),
      bestPoints: nextR4
    };
  }

  // ── Round 5 Actions ──────────────────────────────────────────────
  unlockRound5Hint(hintLevel) {
    if (!this.state.r5Scan.solved && hintLevel > this.state.r5Scan.hintsUsed) {
      this.state.r5Scan.hintsUsed = hintLevel;
    }
    return this.state.r5Scan.hintsUsed;
  }

  scanRound5Target(currentBody, lat, lon) {
    const round5 = this.deck.rounds.find((r) => r.roundNumber === 5);
    if (!round5?.target) return { matched: false };
    if (this.state.r5Scan.solved) {
      return { matched: true, alreadySolved: true, pointsAwarded: 0 };
    }
    const res = verifyScanTarget(round5.target, currentBody, lat, lon, this.state.r5Scan.hintsUsed);
    if (res.matched) {
      this.state.r5Scan.solved = true;
      this.state.r5Scan.points = res.pointsAwarded;
    }
    return res;
  }

  awardRound5Mosaic() {
    if (this.state.r5MosaicPts > 0) return 0;
    this.state.r5MosaicPts = 55;
    return 55;
  }

  awardRound5Cartography() {
    if (this.state.r5CartoPts > 0) return 0;
    this.state.r5CartoPts = 55;
    return 55;
  }

  buildFirebaseSummary(currentBody = 'earth') {
    const scores = this.getRoundScores();
    return {
      score: scores.total,
      currentRound: this.state.activeRound,
      completedRounds: this.getCompletedRoundsCount(),
      completedCount: this.getCompletedStepCount(),
      currentBody: String(currentBody || 'earth').toLowerCase(),
      roundScores: {
        r1: scores.r1,
        r2: scores.r2,
        r3: scores.r3,
        r4: scores.r4,
        r5: scores.r5
      }
    };
  }
}
