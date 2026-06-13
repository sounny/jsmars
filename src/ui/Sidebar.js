/**
 * @module Sidebar
 * @description Collapsible sidebar container for the jsMars application.
 * Manages the toggle button state and CSS class changes for
 * expanding/collapsing the sidebar panel.
 */
export class Sidebar {
    /**
     * Create a new Sidebar.
     * @param {string} containerId - DOM id of the sidebar container
     * @param {string} toggleBtnId - DOM id of the toggle button
     */
    constructor(containerId, toggleBtnId) {
        this.container = document.getElementById(containerId);
        this.toggleBtn = document.getElementById(toggleBtnId);
        /** @type {boolean} */
        this.isCollapsed = false;

        if (!this.container || !this.toggleBtn) {
            console.error('Sidebar elements not found');
            return;
        }

        this.init();
    }

    /**
     * Set initial ARIA attributes and bind the toggle click handler.
     * @private
     */
    init() {
        // Set initial ARIA state
        this.toggleBtn.setAttribute('aria-label', 'Collapse Sidebar');
        this.toggleBtn.setAttribute('aria-expanded', 'true');

        this.toggleBtn.addEventListener('click', () => {
            this.toggle();
        });
    }

    /**
     * Toggle sidebar visibility. Updates CSS classes, button icon,
     * title, and ARIA attributes.
     */
    toggle() {
        this.isCollapsed = !this.isCollapsed;
        this.container.classList.toggle('collapsed', this.isCollapsed);
        this.toggleBtn.classList.toggle('collapsed', this.isCollapsed);

        // Update button icon/text
        this.toggleBtn.innerHTML = this.isCollapsed ? '&#9776;' : '&times;'; // Hamburger or Close
        this.toggleBtn.title = this.isCollapsed ? 'Expand Sidebar' : 'Collapse Sidebar';

        // Sync ARIA attributes
        this.toggleBtn.setAttribute('aria-label', this.isCollapsed ? 'Expand Sidebar' : 'Collapse Sidebar');
        this.toggleBtn.setAttribute('aria-expanded', this.isCollapsed ? 'false' : 'true');
    }
}
