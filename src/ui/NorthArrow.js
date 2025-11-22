import { JMARS_CONFIG } from '../jmars-config.js';

/**
 * North arrow overlay inspired by JMARS compass rose.
 * Rendered as a Leaflet control so it stays anchored in the viewport and responds to sidebar interactions.
 */
export class NorthArrow {
  constructor(map, options = {}) {
    this.map = map;
    this.options = Object.assign({ position: 'bottomright', size: 72 }, options);
    this.control = null;
  }

  static ensureStyles() {
    if (document.getElementById('north-arrow-styles')) return;

    const style = document.createElement('style');
    style.id = 'north-arrow-styles';
    style.textContent = `
      .north-arrow-control {
        width: var(--north-arrow-size, 72px);
        height: var(--north-arrow-size, 72px);
        background: radial-gradient(circle at 30% 30%, rgba(255,255,255,0.2), rgba(0,0,0,0.15));
        border: 1px solid #555;
        border-radius: 8px;
        display: grid;
        place-items: center;
        box-shadow: 0 2px 8px rgba(0,0,0,0.35);
        backdrop-filter: blur(2px);
        margin-bottom: 6px;
        margin-right: 6px;
      }

      .north-arrow-rose {
        width: 80%;
        height: 80%;
      }

      .north-arrow-needle {
        fill: #f03e3e;
      }

      .north-arrow-ring {
        stroke: #ddd;
        stroke-width: 2;
        fill: rgba(0,0,0,0.25);
      }

      .north-arrow-cardinal {
        font: 700 14px 'Segoe UI', sans-serif;
        fill: #fff;
        text-shadow: 0 1px 2px rgba(0,0,0,0.6);
      }

      .north-arrow-reset {
        margin-top: 6px;
        font: 12px 'Segoe UI', sans-serif;
        color: #ddd;
        background: rgba(255,255,255,0.05);
        border: 1px solid #555;
        border-radius: 6px;
        padding: 4px 8px;
        cursor: pointer;
      }

      .north-arrow-reset:hover {
        background: rgba(255,255,255,0.12);
        color: #fff;
      }
    `;

    document.head.appendChild(style);
  }

  add() {
    if (!this.map || this.control) return;

    NorthArrow.ensureStyles();

    const control = L.control({ position: this.options.position });
    control.onAdd = () => {
      const wrapper = L.DomUtil.create('div', 'north-arrow-control leaflet-bar');
      wrapper.style.setProperty('--north-arrow-size', `${this.options.size}px`);
      wrapper.setAttribute('aria-label', 'North arrow indicator');
      wrapper.setAttribute('role', 'group');

      const svgNS = 'http://www.w3.org/2000/svg';
      const svg = document.createElementNS(svgNS, 'svg');
      svg.setAttribute('viewBox', '0 0 100 120');
      svg.classList.add('north-arrow-rose');

      const circle = document.createElementNS(svgNS, 'circle');
      circle.setAttribute('cx', '50');
      circle.setAttribute('cy', '50');
      circle.setAttribute('r', '42');
      circle.classList.add('north-arrow-ring');
      svg.appendChild(circle);

      const needle = document.createElementNS(svgNS, 'polygon');
      needle.setAttribute('points', '50,8 60,50 50,45 40,50');
      needle.classList.add('north-arrow-needle');
      svg.appendChild(needle);

      const southNeedle = document.createElementNS(svgNS, 'polygon');
      southNeedle.setAttribute('points', '50,92 60,50 50,55 40,50');
      southNeedle.setAttribute('fill', '#2b8a3e');
      svg.appendChild(southNeedle);

      const northLabel = document.createElementNS(svgNS, 'text');
      northLabel.setAttribute('x', '50');
      northLabel.setAttribute('y', '20');
      northLabel.setAttribute('text-anchor', 'middle');
      northLabel.classList.add('north-arrow-cardinal');
      northLabel.textContent = 'N';
      svg.appendChild(northLabel);

      const southLabel = document.createElementNS(svgNS, 'text');
      southLabel.setAttribute('x', '50');
      southLabel.setAttribute('y', '92');
      southLabel.setAttribute('text-anchor', 'middle');
      southLabel.classList.add('north-arrow-cardinal');
      southLabel.textContent = 'S';
      svg.appendChild(southLabel);

      const bodyLabel = document.createElementNS(svgNS, 'text');
      bodyLabel.setAttribute('x', '50');
      bodyLabel.setAttribute('y', '60');
      bodyLabel.setAttribute('text-anchor', 'middle');
      bodyLabel.classList.add('north-arrow-cardinal');
      bodyLabel.textContent = JMARS_CONFIG.body || 'Mars';
      svg.appendChild(bodyLabel);

      wrapper.appendChild(svg);

      const resetBtn = document.createElement('button');
      resetBtn.type = 'button';
      resetBtn.className = 'north-arrow-reset';
      resetBtn.innerText = 'Reset view';
      resetBtn.title = 'Recenter to the default JMARS view';
      L.DomEvent.on(resetBtn, 'click', (e) => {
        L.DomEvent.stopPropagation(e);
        this.resetView();
      });

      wrapper.appendChild(resetBtn);

      // Prevent scroll zoom while hovering over control
      L.DomEvent.disableScrollPropagation(wrapper);
      L.DomEvent.disableClickPropagation(wrapper);

      return wrapper;
    };

    control.addTo(this.map);
    this.control = control;
  }

  remove() {
    if (this.control) {
      this.control.remove();
      this.control = null;
    }
  }

  toggle(isActive) {
    if (isActive) this.add();
    else this.remove();
  }

  resetView() {
    if (!this.map) return;
    const { lat, lng, zoom } = JMARS_CONFIG.initialView;
    this.map.flyTo([lat, lng], zoom, { duration: 0.75 });
  }
}
