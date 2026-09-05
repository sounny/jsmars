import { JMARS_CONFIG } from '../jmars-config.js';
import { jmarsState } from '../jmars-state.js';
import { EVENTS } from '../constants.js';
import { switchActiveBody } from '../util/body.js';

/**
 * @module BodySelector
 * @description Dropdown selector for switching planetary bodies (Mars, Moon, Earth).
 * Reads available bodies from JMARS_CONFIG and switches via the canonical
 * JMARSMap body-switch path. Also listens for external body changes to keep
 * the dropdown in sync.
 */
export class BodySelector {
    /**
     * Create a new BodySelector.
     * @param {string} containerId - DOM id of the container element
     */
    constructor(containerId, jmarsMap) {
        this.container = document.getElementById(containerId);
        this.jmarsMap = jmarsMap;
        if (!this.container) return;

        this.init();
    }

    /**
     * Build the select dropdown, populate options from config, and bind events.
     * @private
     */
    init() {
        console.debug('BodySelector initializing...');

        // Create dropdown
        const select = document.createElement('select');
        select.className = 'body-selector-dropdown';
        select.setAttribute('aria-label', 'Select planetary body');

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
        const currentBody = (jmarsState.get('body') || 'mars').toLowerCase();
        select.value = currentBody;
        console.debug('BodySelector initial value:', currentBody);

        // Event listener
        select.addEventListener('change', (e) => {
            const newBody = e.target.value;
            console.debug('BodySelector changed to:', newBody);
            switchActiveBody(this.jmarsMap, newBody);
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