/**
 * @module CollapsibleGroup
 * @description Collapsible section headers for the tools/layers sidebar.
 * Each header toggles its adjacent content panel. Headers can also
 * display an info popover with tool descriptions from data-tool-info.
 *
 * Uses a CSS class ('collapsed') to track expanded/collapsed state
 * instead of reading getComputedStyle, which avoids forced reflow.
 */
export class CollapsibleGroup {
    /**
     * Create a new CollapsibleGroup.
     * @param {string} containerId - DOM id of the group container
     */
    constructor(containerId) {
        this.container = document.getElementById(containerId);
        if (!this.container) return;
        /** @type {HTMLDivElement|null} Currently visible popover */
        this.activePopover = null;
        /** @type {Function|null} Bound outside-click handler for popover dismissal */
        this.boundOutsideClick = null;
        this.init();
    }

    /**
     * Initialize all section headers: wrap titles safely, add icons,
     * create info buttons, and bind click handlers.
     * @private
     */
    init() {
        const headers = this.container.querySelectorAll('.layer-section-header');
        headers.forEach(header => {
            const content = header.nextElementSibling;

            // Ensure title span exists for clearer click targets.
            // Only create it if one does not already exist, and clear
            // the header's raw text nodes to prevent duplication.
            let title = header.querySelector('.layer-title');
            if (!title) {
                const rawText = header.textContent.trim();
                title = document.createElement('span');
                title.className = 'layer-title';
                title.textContent = rawText;

                // Remove existing text nodes to avoid duplicated text
                Array.from(header.childNodes)
                    .filter(n => n.nodeType === Node.TEXT_NODE)
                    .forEach(n => n.remove());

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
                    if (content && content.classList.contains('collapsed')) {
                        this.toggleSection(content, icon);
                    }
                    this.showInfo(header, title.textContent, infoText);
                });
                header.insertBefore(infoBtn, icon);
            }

            header.addEventListener('click', (event) => {
                if (event.target.closest('.tool-info-btn')) return;

                if (content && !content.classList.contains('collapsed') && infoText && event.target.closest('.layer-title')) {
                    event.stopPropagation();
                    this.showInfo(header, title.textContent, infoText);
                    return;
                }

                this.toggleSection(content, icon);
            });

            // Set initial state: all layer sections start collapsed by default
            if (content) {
                content.classList.add('collapsed');
                content.style.display = 'none';
            }
        });
    }

    /**
     * Toggle a content section between collapsed and expanded.
     * @param {HTMLElement|null} content - The content panel to toggle
     * @param {HTMLElement|null} icon - The +/- icon element
     */
    toggleSection(content, icon) {
        if (!content) return;

        const isCollapsed = content.classList.contains('collapsed') || content.style.display === 'none';

        if (isCollapsed) {
            content.classList.remove('collapsed');
            content.style.display = 'block';
            if (icon) {
                icon.textContent = '-';
            }
        } else {
            content.classList.add('collapsed');
            content.style.display = 'none';
            if (icon) {
                icon.textContent = '+';
            }
            this.hideInfo();
        }
    }

    /**
     * Show an info popover attached to a section header.
     * @param {HTMLElement} header - The header element to attach the popover to
     * @param {string} titleText - Title for the popover
     * @param {string} infoText - Body text for the popover
     */
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

    /**
     * Hide and remove the currently visible info popover.
     */
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
