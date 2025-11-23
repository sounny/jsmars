export class Accordion {
    constructor(containerId) {
        this.container = document.getElementById(containerId);
        if (!this.container) return;

        this.sections = this.container.querySelectorAll('.accordion-section');
        this.bindEvents();
    }

    bindEvents() {
        this.sections.forEach(section => {
            const header = section.querySelector('.accordion-header');
            if (header) {
                header.addEventListener('click', () => {
                    this.toggleSection(section);
                });
            }
        });
    }

    toggleSection(targetSection) {
        // If already expanded, do nothing (or toggle off if we want to allow all closed)
        // User asked for "expands to height... then when you click next it collapses"
        // implying exclusive mode.

        const isExpanded = targetSection.classList.contains('expanded');

        if (isExpanded) {
            // Optional: Allow collapsing the active one? 
            // Usually accordions keep one open. Let's allow collapsing for now if they click it again.
            targetSection.classList.remove('expanded');
        } else {
            // Collapse all others
            this.sections.forEach(s => s.classList.remove('expanded'));
            // Expand target
            targetSection.classList.add('expanded');
        }
    }
}
