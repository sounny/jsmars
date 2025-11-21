export class Sidebar {
    constructor(containerId, toggleBtnId) {
        this.container = document.getElementById(containerId);
        this.toggleBtn = document.getElementById(toggleBtnId);
        this.isCollapsed = false;

        if (!this.container || !this.toggleBtn) {
            console.error('Sidebar elements not found');
            return;
        }

        this.init();
    }

    init() {
        this.toggleBtn.addEventListener('click', () => {
            this.toggle();
        });
    }

    toggle() {
        this.isCollapsed = !this.isCollapsed;
        this.container.classList.toggle('collapsed', this.isCollapsed);
        this.toggleBtn.classList.toggle('collapsed', this.isCollapsed);

        // Update button icon/text
        this.toggleBtn.innerHTML = this.isCollapsed ? '&#9776;' : '&times;'; // Hamburger or Close
        this.toggleBtn.title = this.isCollapsed ? 'Expand Sidebar' : 'Collapse Sidebar';
    }
}
