/**
 * @module MobileSheet
 * @description Coordinates mobile bottom-sheet behavior, touch gestures,
 * drag-to-expand/collapse, and full-height tool modal sheet display.
 */
export class MobileSheet {
  /**
   * @param {HTMLElement|string} controlsEl - The #controls sidebar/sheet element
   * @param {L.Map} map - Leaflet map instance
   */
  constructor(controlsEl, map) {
    this.controlsEl = typeof controlsEl === 'string' ? document.getElementById(controlsEl) : controlsEl;
    this.map = map;
    this.state = 'peek'; // 'peek', 'expanded'
    this.isTouchDevice = false;
    this.startY = 0;
    this.currentY = 0;
    this.isDragging = false;

    if (this.controlsEl) {
      this.init();
    }
  }

  init() {
    this.isTouchDevice = 'ontouchstart' in window || navigator.maxTouchPoints > 0;
    this._injectMobileDragHandle();
    this._bindTouchGestures();
    this._handleResize();
    window.addEventListener('resize', () => this._handleResize());
  }

  _injectMobileDragHandle() {
    if (!this.controlsEl) return;

    // Check if handle already exists
    if (this.controlsEl.querySelector('.mobile-sheet-handle-wrap')) return;

    const handleWrap = document.createElement('div');
    handleWrap.className = 'mobile-sheet-handle-wrap';
    handleWrap.setAttribute('role', 'button');
    handleWrap.setAttribute('aria-label', 'Drag to open/close menu');
    handleWrap.innerHTML = `
      <div class="mobile-sheet-pill"></div>
      <div class="mobile-sheet-peek-summary">
        <span class="mobile-sheet-title">JSMARS Menu & Tools</span>
        <span class="mobile-sheet-chevron">&#9650;</span>
      </div>
    `;

    handleWrap.addEventListener('click', (e) => {
      e.stopPropagation();
      this.toggleSheet();
    });

    this.controlsEl.insertBefore(handleWrap, this.controlsEl.firstChild);
  }

  _bindTouchGestures() {
    if (!this.controlsEl) return;

    const handleWrap = this.controlsEl.querySelector('.mobile-sheet-handle-wrap');
    if (!handleWrap) return;

    // Prevent touch gestures inside sheet from propagating and panning Leaflet map
    this.controlsEl.addEventListener('touchstart', (e) => {
      e.stopPropagation();
    }, { passive: true });

    handleWrap.addEventListener('touchstart', (e) => {
      if (window.innerWidth > 768) return;
      this.isDragging = true;
      this.startY = e.touches[0].clientY;
      this.currentY = this.startY;
      this.controlsEl.style.transition = 'none';
    }, { passive: true });

    window.addEventListener('touchmove', (e) => {
      if (!this.isDragging || window.innerWidth > 768) return;
      this.currentY = e.touches[0].clientY;
      const deltaY = this.currentY - this.startY;

      // When dragging downwards from expanded state or upwards from peek
      if (this.state === 'peek' && deltaY < 0) {
        const offset = Math.max(-window.innerHeight * 0.75, deltaY);
        this.controlsEl.style.transform = `translateY(calc(100% - 64px + ${offset}px))`;
      } else if (this.state === 'expanded' && deltaY > 0) {
        const offset = Math.min(window.innerHeight * 0.75, deltaY);
        this.controlsEl.style.transform = `translateY(${offset}px)`;
      }
    }, { passive: true });

    window.addEventListener('touchend', (e) => {
      if (!this.isDragging || window.innerWidth > 768) return;
      this.isDragging = false;
      this.controlsEl.style.transition = 'transform 0.3s cubic-bezier(0.16, 1, 0.3, 1)';
      const deltaY = this.currentY - this.startY;

      if (this.state === 'peek' && deltaY < -40) {
        this.expandSheet();
      } else if (this.state === 'expanded' && deltaY > 40) {
        this.collapseSheet();
      } else {
        // Snap back to current state
        if (this.state === 'expanded') {
          this.expandSheet();
        } else {
          this.collapseSheet();
        }
      }
    });
  }

  toggleSheet() {
    if (this.state === 'expanded') {
      this.collapseSheet();
    } else {
      this.expandSheet();
    }
  }

  expandSheet() {
    this.state = 'expanded';
    this.controlsEl.classList.add('mobile-sheet-expanded');
    this.controlsEl.classList.remove('mobile-sheet-peek');
    this.controlsEl.style.transform = '';
    const chevron = this.controlsEl.querySelector('.mobile-sheet-chevron');
    if (chevron) chevron.innerHTML = '&#9660;';
  }

  collapseSheet() {
    this.state = 'peek';
    this.controlsEl.classList.remove('mobile-sheet-expanded');
    this.controlsEl.classList.add('mobile-sheet-peek');
    this.controlsEl.style.transform = '';
    const chevron = this.controlsEl.querySelector('.mobile-sheet-chevron');
    if (chevron) chevron.innerHTML = '&#9650;';
  }

  _handleResize() {
    const isMobile = window.innerWidth <= 768;
    if (isMobile) {
      this.controlsEl.classList.add('is-mobile-sheet');
      if (this.state === 'peek') {
        this.collapseSheet();
      }
    } else {
      this.controlsEl.classList.remove('is-mobile-sheet', 'mobile-sheet-expanded', 'mobile-sheet-peek');
      this.controlsEl.style.transform = '';
    }
  }
}
