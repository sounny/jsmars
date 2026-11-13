/**
 * @module spacecamp/SpaceCampIcons
 * @description Crisp, zero-dependency 1.6px monoline aerospace SVG icon factory
 * built strictly via W3C DOM APIs (no innerHTML, no emoji glyphs).
 */

const SVG_NS = 'http://www.w3.org/2000/svg';

const ICON_SHAPES = {
  trails: [
    ['polygon', { points: '3 6 9 3 15 6 21 3 21 18 15 21 9 18 3 21' }],
    ['line', { x1: '9', y1: '3', x2: '9', y2: '18' }],
    ['line', { x1: '15', y1: '6', x2: '15', y2: '21' }]
  ],
  capcom: [
    ['path', { d: 'M2 12h3l2.5-6 4 12 3-8 2.5 2H22' }]
  ],
  leaderboard: [
    ['path', { d: 'M18 20V10' }],
    ['path', { d: 'M12 20V4' }],
    ['path', { d: 'M6 20v-6' }]
  ],
  podium: [
    ['path', { d: 'M8 21h8' }],
    ['path', { d: 'M12 17v4' }],
    ['path', { d: 'M7 4h10v6a5 5 0 0 1-10 0V4z' }],
    ['path', { d: 'M7 6H4v2a3 3 0 0 0 3 3' }],
    ['path', { d: 'M17 6h3v2a3 3 0 0 1-3 3' }]
  ],
  slides: [
    ['rect', { x: '3', y: '4', width: '18', height: '12', rx: '1.5' }],
    ['line', { x1: '8', y1: '20', x2: '16', y2: '20' }],
    ['line', { x1: '12', y1: '16', x2: '12', y2: '20' }]
  ],
  crosshair: [
    ['circle', { cx: '12', cy: '12', r: '8' }],
    ['line', { x1: '12', y1: '2', x2: '12', y2: '6' }],
    ['line', { x1: '12', y1: '18', x2: '12', y2: '22' }],
    ['line', { x1: '2', y1: '12', x2: '6', y2: '12' }],
    ['line', { x1: '18', y1: '12', x2: '22', y2: '12' }]
  ],
  locate: [
    ['polygon', { points: '3 11 21 3 13 21 11 13 3 11' }]
  ],
  tool: [
    ['line', { x1: '4', y1: '21', x2: '4', y2: '14' }],
    ['line', { x1: '4', y1: '10', x2: '4', y2: '3' }],
    ['line', { x1: '12', y1: '21', x2: '12', y2: '12' }],
    ['line', { x1: '12', y1: '8', x2: '12', y2: '3' }],
    ['line', { x1: '20', y1: '21', x2: '20', y2: '16' }],
    ['line', { x1: '20', y1: '12', x2: '20', y2: '3' }],
    ['line', { x1: '1', y1: '14', x2: '7', y2: '14' }],
    ['line', { x1: '9', y1: '8', x2: '15', y2: '8' }],
    ['line', { x1: '17', y1: '16', x2: '23', y2: '16' }]
  ],
  lock: [
    ['rect', { x: '5', y: '11', width: '14', height: '10', rx: '1.5' }],
    ['path', { d: 'M8 11V7a4 4 0 0 1 8 0v4' }]
  ],
  key: [
    ['circle', { cx: '7.5', cy: '15.5', r: '4.5' }],
    ['path', { d: 'M10.7 12.3L21 2' }],
    ['path', { d: 'M17 6l3 3' }]
  ],
  check: [
    ['polyline', { points: '20 6 9 17 4 12' }]
  ],
  swap: [
    ['polyline', { points: '17 2 21 6 17 10' }],
    ['line', { x1: '3', y1: '6', x2: '21', y2: '6' }],
    ['polyline', { points: '7 22 3 18 7 14' }],
    ['line', { x1: '21', y1: '18', x2: '3', y2: '18' }]
  ],
  external: [
    ['path', { d: 'M18 13v6a1.5 1.5 0 0 1-1.5 1.5h-11A1.5 1.5 0 0 1 4 19V8a1.5 1.5 0 0 1 1.5-1.5h6' }],
    ['polyline', { points: '15 3 21 3 21 9' }],
    ['line', { x1: '10', y1: '14', x2: '21', y2: '3' }]
  ],
  chevronLeft: [
    ['polyline', { points: '15 18 9 12 15 6' }]
  ],
  chevronRight: [
    ['polyline', { points: '9 18 15 12 9 6' }]
  ],
  play: [
    ['polygon', { points: '6 4 20 12 6 20 6 4' }]
  ],
  pause: [
    ['rect', { x: '6', y: '4', width: '4', height: '16' }],
    ['rect', { x: '14', y: '4', width: '4', height: '16' }]
  ],
  reset: [
    ['path', { d: 'M3 12a9 9 0 1 0 3-6.7L3 8' }],
    ['polyline', { points: '3 3 3 8 8 8' }]
  ],
  sensor: [
    ['circle', { cx: '12', cy: '12', r: '3' }],
    ['path', { d: 'M19.4 4.6a10.5 10.5 0 0 1 0 14.8' }],
    ['path', { d: 'M4.6 19.4a10.5 10.5 0 0 1 0-14.8' }]
  ],
  globe: [
    ['circle', { cx: '12', cy: '12', r: '9' }],
    ['line', { x1: '3', y1: '12', x2: '21', y2: '12' }],
    ['path', { d: 'M12 3a14 14 0 0 1 0 18a14 14 0 0 1 0-18' }]
  ],
  close: [
    ['line', { x1: '18', y1: '6', x2: '6', y2: '18' }],
    ['line', { x1: '6', y1: '6', x2: '18', y2: '18' }]
  ]
};

/**
 * Create an inline monoline SVG icon element.
 * @param {keyof typeof ICON_SHAPES} name
 * @param {number} [size=13]
 * @returns {SVGSVGElement}
 */
export function scIcon(name, size = 13) {
  const svg = document.createElementNS(SVG_NS, 'svg');
  svg.setAttribute('viewBox', '0 0 24 24');
  svg.setAttribute('width', String(size));
  svg.setAttribute('height', String(size));
  svg.setAttribute('fill', 'none');
  svg.setAttribute('stroke', 'currentColor');
  svg.setAttribute('stroke-width', '1.75');
  svg.setAttribute('stroke-linecap', 'round');
  svg.setAttribute('stroke-linejoin', 'round');
  svg.setAttribute('aria-hidden', 'true');
  svg.classList.add('sc-icon');

  const shapes = ICON_SHAPES[name] || ICON_SHAPES.crosshair;
  for (const [tag, attrs] of shapes) {
    const node = document.createElementNS(SVG_NS, tag);
    for (const [k, v] of Object.entries(attrs)) {
      node.setAttribute(k, v);
    }
    svg.appendChild(node);
  }
  return svg;
}
