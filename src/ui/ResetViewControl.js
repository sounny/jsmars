import { JMARS_CONFIG } from '../jmars-config.js';

export class ResetViewControl {
    constructor(map, options = {}) {
        this.map = map;
        this.options = Object.assign({ position: 'topright' }, options);
        this.control = null;
    }

    add() {
        if (!this.map || this.control) return;

        const control = L.control({ position: this.options.position });
        control.onAdd = () => {
            const container = L.DomUtil.create('div', 'leaflet-bar leaflet-control reset-view-control');
            const button = L.DomUtil.create('a', 'reset-view-btn', container);
            button.href = '#';
            button.title = 'Reset View';
            button.role = 'button';
            button.setAttribute('aria-label', 'Reset View');

            // Little Mars Icon (Simple SVG)
            button.innerHTML = `
        <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2">
          <circle cx="12" cy="12" r="9" fill="#d6336c" stroke="none" />
          <path d="M12 3v18M3 12h18" stroke="rgba(255,255,255,0.3)" stroke-width="1" />
        </svg>
      `;

            L.DomEvent.on(button, 'click', (e) => {
                L.DomEvent.stop(e);
                this.resetView();
            });

            return container;
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
