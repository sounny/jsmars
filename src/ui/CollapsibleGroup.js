export class CollapsibleGroup {
    constructor(containerId) {
        this.container = document.getElementById(containerId);
        if (!this.container) return;
        this.init();
    }

    init() {
        const headers = this.container.querySelectorAll('.layer-section-header');
        headers.forEach(header => {
            // Ensure icon exists if not present
            let icon = header.querySelector('.icon');
            if (!icon) {
                icon = document.createElement('span');
                icon.className = 'icon';
                icon.textContent = '+';
                header.appendChild(icon);
            }

            header.addEventListener('click', () => {
                const content = header.nextElementSibling;
                if (!content) return;

                const isHidden = content.style.display === 'none';
                content.style.display = isHidden ? 'block' : 'none';
                icon.textContent = isHidden ? '-' : '+';
            });
        });
    }
}
