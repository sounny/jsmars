import { JMARS_CONFIG } from '../jmars-config.js';
import { jmarsState } from '../jmars-state.js';
import { EVENTS } from '../constants.js';

export class BodySelector {
    constructor(containerId) {
        this.container = document.getElementById(containerId);
        if (!this.container) return;
        
        this.init();
    }

    init() {
        console.debug('BodySelector initializing...');
        if (!this.container) {
            console.error('BodySelector container not found!');
            return;
        }

        // Create dropdown
        const select = document.createElement('select');
        select.className = 'body-selector-dropdown';
        
        // Populate options
        const bodies = JMARS_CONFIG.bodies;
        if (bodies) {
            Object.keys(bodies).forEach(key => {
                const body = bodies[key];
                const option = document.createElement('option');
                option.value = key;
                option.text = body.name;
                select.appendChild(option);
            });
        } else {
            console.error('JMARS_CONFIG.bodies is undefined');
        }

        // Set initial value
        const currentBody = (jmarsState.get('body') || 'Mars').toLowerCase();
        select.value = currentBody;
        console.debug('BodySelector initial value:', currentBody);

        // Event listener
        select.addEventListener('change', (e) => {
            const newBody = e.target.value;
            console.debug('BodySelector changed to:', newBody);
            jmarsState.set('body', newBody);
            const event = new CustomEvent(EVENTS.BODY_CHANGED, { detail: { body: newBody } });
            document.dispatchEvent(event);
        });

        // Listen for external changes (e.g. loaded session)
        document.addEventListener(EVENTS.BODY_CHANGED, (e) => {
            if (e.detail && e.detail.body) {
                select.value = e.detail.body.toLowerCase();
            }
        });

        this.container.appendChild(select);
        console.debug('BodySelector appended to container');
    }
}