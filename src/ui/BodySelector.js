import { JMARS_CONFIG } from '../jmars-config.js';
import { jmarsState } from '../jmars-state.js';
import { EVENTS } from '../constants.js';
import { switchActiveBody } from '../util/body.js';

/**
 * @module BodySelector
 * @description Dropdown selector for switching planetary bodies across all
 * JMARS planetary systems (Inner Planets & Moon, Mars Moons, Asteroids & Dwarf
 * Planets, Jupiter, Saturn, Neptune, and Pluto systems).
 * Organizes bodies into semantic `<optgroup>` sections when `body.group` is set.
 */
export class BodySelector {
    /**
     * Create a new BodySelector.
     * @param {string} containerId - DOM id of the container element
     * @param {import('../jmars-map.js').JMARSMap} jmarsMap
     */
    constructor(containerId, jmarsMap) {
        this.container = document.getElementById(containerId);
        this.jmarsMap = jmarsMap;
        if (!this.container) return;

        this.init();
    }

    /**
     * Build the select dropdown, populate grouped options from config, and bind events.
     * @private
     */
    init() {
        console.debug('BodySelector initializing...');

        const select = document.createElement('select');
        select.className = 'body-selector-dropdown';
        select.setAttribute('aria-label', 'Select planetary body');

        const bodies = JMARS_CONFIG.bodies;
        if (bodies) {
            const groups = new Map();
            Object.keys(bodies).forEach((key) => {
                const body = bodies[key];
                if (!body) return;
                const option = document.createElement('option');
                option.value = key;
                option.textContent = body.name || key;

                if (body.group) {
                    let optgroup = groups.get(body.group);
                    if (!optgroup) {
                        optgroup = document.createElement('optgroup');
                        optgroup.label = body.group;
                        groups.set(body.group, optgroup);
                        select.appendChild(optgroup);
                    }
                    optgroup.appendChild(option);
                } else {
                    select.appendChild(option);
                }
            });
        } else {
            console.error('JMARS_CONFIG.bodies is undefined');
        }

        const currentBody = (jmarsState.get('body') || 'mars').toLowerCase();
        select.value = currentBody;

        select.addEventListener('change', (e) => {
            const newBody = e.target.value;
            switchActiveBody(this.jmarsMap, newBody);
        });

        document.addEventListener(EVENTS.BODY_CHANGED, (e) => {
            if (e.detail && e.detail.body) {
                select.value = e.detail.body.toLowerCase();
            }
        });

        this.container.appendChild(select);
    }
}
