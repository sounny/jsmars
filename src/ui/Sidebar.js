/**
 * @module Sidebar
 * @description Collapsible sidebar container for the jsMars application.
 * Manages the toggle button state, body layout class, and ARIA attributes for
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
        this.isCollapsed = this.container.classList.contains('collapsed');
        if (typeof document !== 'undefined' && document.body) {
            document.body.classList.toggle('sidebar-collapsed', this.isCollapsed);
        }
        this.toggleBtn.setAttribute('aria-label', this.isCollapsed ? 'Expand Sidebar' : 'Collapse Sidebar');
        this.toggleBtn.setAttribute('aria-expanded', this.isCollapsed ? 'false' : 'true');

        this.toggleBtn.addEventListener('click', () => {
            this.toggle();
        });
    }

    /**
     * Toggle sidebar visibility. Updates CSS classes, body layout state,
     * button icon, title, and ARIA attributes.
     */
    toggle() {
        this.isCollapsed = !this.isCollapsed;
        this.container.classList.toggle('collapsed', this.isCollapsed);
        this.toggleBtn.classList.toggle('collapsed', this.isCollapsed);
        if (typeof document !== 'undefined' && document.body) {
            document.body.classList.toggle('sidebar-collapsed', this.isCollapsed);
        }

        this.toggleBtn.textContent = this.isCollapsed ? '\u2630' : '\u00d7';
        this.toggleBtn.title = this.isCollapsed ? 'Expand Sidebar' : 'Collapse Sidebar';

        this.toggleBtn.setAttribute('aria-label', this.isCollapsed ? 'Expand Sidebar' : 'Collapse Sidebar');
        this.toggleBtn.setAttribute('aria-expanded', this.isCollapsed ? 'false' : 'true');
    }
}
