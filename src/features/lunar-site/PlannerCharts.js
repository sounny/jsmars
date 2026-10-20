/**
 * @module lunar-site/PlannerCharts
 * @description Lightweight canvas charts (no dependencies) for the planner.
 */

const INK = '#e6e8ee', MUTED = '#9aa3b2', GRID = 'rgba(255,255,255,0.12)';

function setup(canvas) {
  const dpr = window.devicePixelRatio || 1;
  const w = canvas.clientWidth || 320, h = canvas.clientHeight || 180;
  canvas.width = w * dpr; canvas.height = h * dpr;
  const ctx = canvas.getContext('2d');
  ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
  ctx.clearRect(0, 0, w, h);
  ctx.font = '11px system-ui, sans-serif';
  return { ctx, w, h };
}

function niceTicks(min, max, n = 5) {
  const span = max - min || 1;
  const step0 = span / n, mag = Math.pow(10, Math.floor(Math.log10(step0)));
  const step = [1, 2, 5, 10].map(m => m * mag).find(s => s >= step0) || mag * 10;
  const out = [];
  for (let v = Math.ceil(min / step) * step; v <= max + 1e-9; v += step) out.push(v);
  return out;
}

/**
 * Elevation profile with grade-coloured segments.
 * @param {HTMLCanvasElement} canvas
 * @param {{samples:{d:number,z:number,grade:number}[]}} profile
 * @param {number} t1 lower threshold (deg), @param {number} t2 upper threshold (deg)
 */
export function drawProfile(canvas, profile, t1, t2) {
  const { ctx, w, h } = setup(canvas);
  const s = profile.samples.filter(p => Number.isFinite(p.z));
  if (s.length < 2) { ctx.fillStyle = MUTED; ctx.fillText('No elevation data along this corridor.', 10, 20); return; }
  const L = 48, R = 10, T = 10, B = 26;
  const dmax = s[s.length - 1].d, zmin = Math.min(...s.map(p => p.z)), zmax = Math.max(...s.map(p => p.z));
  const pad = Math.max(1, (zmax - zmin) * 0.08);
  const X = d => L + (d / dmax) * (w - L - R);
  const Y = z => T + (1 - (z - (zmin - pad)) / (zmax - zmin + 2 * pad)) * (h - T - B);
  ctx.strokeStyle = GRID; ctx.fillStyle = MUTED; ctx.lineWidth = 1;
  for (const z of niceTicks(zmin - pad, zmax + pad, 4)) {
    ctx.beginPath(); ctx.moveTo(L, Y(z)); ctx.lineTo(w - R, Y(z)); ctx.stroke();
    ctx.fillText(`${Math.round(z)} m`, 2, Y(z) + 4);
  }
  for (const d of niceTicks(0, dmax, 5)) {
    ctx.fillText(d >= 1000 ? `${(d / 1000).toFixed(d % 1000 ? 1 : 0)} km` : `${Math.round(d)} m`, X(d) - 12, h - 8);
  }
  ctx.lineWidth = 2.5;
  for (let i = 1; i < s.length; i++) {
    const g = s[i].grade;
    ctx.strokeStyle = !Number.isFinite(g) ? MUTED : g <= t1 ? '#2c7bb6' : g <= t2 ? '#fdd835' : '#d7191c';
    ctx.beginPath(); ctx.moveTo(X(s[i - 1].d), Y(s[i - 1].z)); ctx.lineTo(X(s[i].d), Y(s[i].z)); ctx.stroke();
  }
}

/**
 * Horizon elevation vs azimuth with the geometric Sun-path envelope.
 */
export function drawHorizon(canvas, horizon, envelope) {
  const { ctx, w, h } = setup(canvas);
  const L = 36, R = 8, T = 10, B = 24;
  const vals = horizon.profile.map(p => p.elev).concat(envelope.flatMap(e => [e.min, e.max])).filter(Number.isFinite);
  const emin = Math.min(-1, ...vals) - 0.5, emax = Math.max(3, ...vals) + 0.5;
  const X = az => L + (az / 360) * (w - L - R);
  const Y = e => T + (1 - (e - emin) / (emax - emin)) * (h - T - B);
  ctx.strokeStyle = GRID; ctx.fillStyle = MUTED; ctx.lineWidth = 1;
  for (const e of niceTicks(emin, emax, 5)) {
    ctx.beginPath(); ctx.moveTo(L, Y(e)); ctx.lineTo(w - R, Y(e)); ctx.stroke();
    ctx.fillText(`${e}°`, 2, Y(e) + 4);
  }
  ['N', 'E', 'S', 'W', 'N'].forEach((lab, i) => ctx.fillText(lab, X(i * 90) - 3, h - 6));
  // Sun envelope band
  ctx.fillStyle = 'rgba(253, 216, 53, 0.30)';
  ctx.beginPath();
  envelope.forEach((e, i) => (i ? ctx.lineTo : ctx.moveTo).call(ctx, X(e.az), Y(e.max)));
  [...envelope].reverse().forEach(e => ctx.lineTo(X(e.az), Y(e.min)));
  ctx.closePath(); ctx.fill();
  // Terrain horizon
  ctx.fillStyle = 'rgba(120, 130, 150, 0.55)'; ctx.strokeStyle = INK; ctx.lineWidth = 1.5;
  ctx.beginPath(); ctx.moveTo(X(0), Y(emin));
  horizon.profile.forEach(p => ctx.lineTo(X(p.az), Y(Math.max(emin, p.elev))));
  ctx.lineTo(X(360), Y(emin)); ctx.closePath(); ctx.fill();
  ctx.beginPath();
  horizon.profile.forEach((p, i) => (i ? ctx.lineTo : ctx.moveTo).call(ctx, X(p.az), Y(Math.max(emin, p.elev))));
  ctx.stroke();
}
