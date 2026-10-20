/**
 * @module lunar-site/PlannerRenderers
 * @description Render analysis grids to canvases (colour-vision-deficiency
 * safe ramps; every ramp has a text legend in the UI).
 */

/** Perceptual ramp (dark blue -> teal -> yellow), similar to viridis. */
const VIRIDIS = [[68, 1, 84], [59, 82, 139], [33, 145, 140], [94, 201, 98], [253, 231, 37]];
/** Slope classes: blue (ok), yellow (caution), red-magenta (exceeds). */
export const SLOPE_COLORS = [[44, 123, 182], [253, 216, 53], [215, 25, 28]];

function ramp(stops, t) {
  if (!Number.isFinite(t)) return null;
  t = Math.min(1, Math.max(0, t)) * (stops.length - 1);
  const i = Math.min(stops.length - 2, Math.floor(t)), f = t - i;
  return stops[i].map((v, k) => v + (stops[i + 1][k] - v) * f);
}

function canvasFor(w, h) {
  const c = document.createElement('canvas');
  c.width = w; c.height = h;
  return c;
}

/** Greyscale canvas from Uint8 values (hillshade). */
export function renderGrey(values, w, h) {
  const c = canvasFor(w, h), ctx = c.getContext('2d'), img = ctx.createImageData(w, h);
  for (let i = 0; i < values.length; i++) {
    const v = values[i];
    img.data[i * 4] = img.data[i * 4 + 1] = img.data[i * 4 + 2] = v;
    img.data[i * 4 + 3] = v === 0 ? 0 : 255;
  }
  ctx.putImageData(img, 0, 0);
  return c;
}

/**
 * Generic colour render.
 * @param {(i:number)=>number[]|null} colorAt returns [r,g,b] or null (transparent)
 */
export function renderColor(w, h, colorAt, alpha = 255) {
  const c = canvasFor(w, h), ctx = c.getContext('2d'), img = ctx.createImageData(w, h);
  for (let i = 0; i < w * h; i++) {
    const col = colorAt(i);
    if (!col) continue;
    img.data[i * 4] = col[0]; img.data[i * 4 + 1] = col[1]; img.data[i * 4 + 2] = col[2]; img.data[i * 4 + 3] = alpha;
  }
  ctx.putImageData(img, 0, 0);
  return c;
}

export function renderSlopeClasses(grid, t1, t2) {
  return renderColor(grid.width, grid.height, (i) => {
    const s = grid.cell(i % grid.width, Math.floor(i / grid.width));
    if (!Number.isFinite(s)) return null;
    return s <= t1 ? SLOPE_COLORS[0] : s <= t2 ? SLOPE_COLORS[1] : SLOPE_COLORS[2];
  });
}

/** Render a fraction raster (0..1) with the perceptual ramp. */
export function renderFraction(grid) {
  return renderColor(grid.width, grid.height, (i) => {
    const v = grid.cell(i % grid.width, Math.floor(i / grid.width));
    return ramp(VIRIDIS, v);
  });
}

export function renderPSR(grid) {
  return renderColor(grid.width, grid.height, (i) => {
    const v = grid.cell(i % grid.width, Math.floor(i / grid.width));
    return v > 0.5 ? [0, 191, 255] : null;
  }, 200);
}

export function renderScalar(values, w, h, vmin, vmax) {
  return renderColor(w, h, (i) => ramp(VIRIDIS, (values[i] - vmin) / (vmax - vmin)));
}

/** CSS gradient string for legends. */
export const VIRIDIS_CSS = `linear-gradient(90deg, ${VIRIDIS.map(c => `rgb(${c.join(',')})`).join(',')})`;
