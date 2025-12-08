export class CollapsibleGroup {
    constructor(containerId) {
        this.container = document.getElementById(containerId);
        if (!this.container) return;
        this.activePopover = null;
        this.boundOutsideClick = null;
        this.init();
    }

    init() {
        const headers = this.container.querySelectorAll('.layer-section-header');
        headers.forEach(header => {
            const content = header.nextElementSibling;

            // Ensure title span exists for clearer click targets
            let title = header.querySelector('.layer-title');
            if (!title) {
                title = document.createElement('span');
                title.className = 'layer-title';
                title.textContent = header.textContent.trim();
                header.insertBefore(title, header.firstChild);
            }

            // Ensure icon exists if not present
            let icon = header.querySelector('.icon');
            if (!icon) {
                icon = document.createElement('span');
                icon.className = 'icon';
                icon.textContent = '+';
                header.appendChild(icon);
            }

            const infoText = header.dataset.toolInfo;
            if (infoText) {
                const infoBtn = document.createElement('button');
                infoBtn.type = 'button';
                infoBtn.className = 'tool-info-btn';
                infoBtn.setAttribute('aria-label', `${title.textContent.trim()} info`);
                infoBtn.textContent = 'i';
                infoBtn.addEventListener('click', (event) => {
                    event.stopPropagation();
                    if (content && content.style.display === 'none') {
                        this.toggleSection(content, icon);
                    }
                    this.showInfo(header, title.textContent, infoText);
                });
                header.insertBefore(infoBtn, icon);
            }

            header.addEventListener('click', (event) => {
                if (event.target.closest('.tool-info-btn')) return;

                if (content && content.style.display !== 'none' && infoText && event.target.closest('.layer-title')) {
                    event.stopPropagation();
                    this.showInfo(header, title.textContent, infoText);
                    return;
                }

                this.toggleSection(content, icon);
            });
        });
    }

    toggleSection(content, icon) {
        if (!content) return;
        const isHidden = window.getComputedStyle(content).display === 'none';
        content.style.display = isHidden ? 'block' : 'none';
        if (icon) {
            icon.textContent = isHidden ? '-' : '+';
        }
        if (!isHidden) {
            this.hideInfo();
        }
    }

    showInfo(header, titleText, infoText) {
        if (!infoText) return;
        this.hideInfo();

        const popover = document.createElement('div');
        popover.className = 'tool-info-popover';

        const titleEl = document.createElement('div');
        titleEl.className = 'tool-info-title';
        titleEl.textContent = titleText;

        const bodyEl = document.createElement('div');
        bodyEl.className = 'tool-info-body';
        bodyEl.textContent = infoText;

        const closeBtn = document.createElement('button');
        closeBtn.type = 'button';
        closeBtn.className = 'tool-info-close';
        closeBtn.textContent = 'Close';
        closeBtn.addEventListener('click', (e) => {
            e.stopPropagation();
            this.hideInfo();
        });

        popover.appendChild(titleEl);
        popover.appendChild(bodyEl);
        popover.appendChild(closeBtn);
        popover.addEventListener('click', (e) => e.stopPropagation());

        header.appendChild(popover);
        requestAnimationFrame(() => popover.classList.add('visible'));

        this.activePopover = popover;
        this.boundOutsideClick = (evt) => {
            if (!header.contains(evt.target)) {
                this.hideInfo();
            }
        };
        document.addEventListener('click', this.boundOutsideClick);
    }

    hideInfo() {
        if (this.activePopover && this.activePopover.parentNode) {
            this.activePopover.parentNode.removeChild(this.activePopover);
        }
        this.activePopover = null;
        if (this.boundOutsideClick) {
            document.removeEventListener('click', this.boundOutsideClick);
            this.boundOutsideClick = null;
        }
    }
}
