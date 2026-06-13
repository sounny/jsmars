/**
 * @module Accordion
 * @description Exclusive accordion component for sidebar sections.
 * Only one section can be expanded at a time (clicking a new header
 * collapses the previous one). Supports keyboard activation via
 * Enter and Space keys.
 */
export class Accordion {
    /**
     * Create a new Accordion.
     * @param {string} containerId - DOM id of the accordion container
     */
    constructor(containerId) {
        this.container = document.getElementById(containerId);
        if (!this.container) return;

        /** @type {NodeListOf<Element>} */
        this.sections = this.container.querySelectorAll('.accordion-section');
        this.bindEvents();
    }

    /**
     * Bind click and keyboard events to all accordion headers.
     * Adds ARIA attributes for accessibility.
     * @private
     */
    bindEvents() {
        this.sections.forEach(section => {
            const header = section.querySelector('.accordion-header');
            if (!header) return;

            // Accessibility: make headers behave as interactive buttons
            header.setAttribute('role', 'button');
            header.setAttribute('tabindex', '0');
            header.setAttribute('aria-expanded', section.classList.contains('expanded') ? 'true' : 'false');

            header.addEventListener('click', () => {
                this.toggleSection(section);
            });

            // Keyboard: Enter and Space activate the header
            header.addEventListener('keydown', (e) => {
                if (e.key === 'Enter' || e.key === ' ') {
                    e.preventDefault();
                    this.toggleSection(section);
                }
            });
        });
    }

    /**
     * Toggle a section open or closed.
     * In exclusive mode, expanding one section collapses all others.
     * Updates aria-expanded on all headers.
     * @param {Element} targetSection - The accordion section element to toggle
     */
    toggleSection(targetSection) {
        const isExpanded = targetSection.classList.contains('expanded');

        if (isExpanded) {
            // Allow collapsing the active section by clicking it again
            targetSection.classList.remove('expanded');
        } else {
            // Collapse all others
            this.sections.forEach(s => s.classList.remove('expanded'));
            // Expand target
            targetSection.classList.add('expanded');
        }

        // Sync aria-expanded on all section headers
        this.sections.forEach(s => {
            const h = s.querySelector('.accordion-header');
            if (h) {
                h.setAttribute('aria-expanded', s.classList.contains('expanded') ? 'true' : 'false');
            }
        });
    }
}
