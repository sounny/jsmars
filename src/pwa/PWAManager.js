/**
 * @module PWAManager
 * @description Progressive Web App (PWA) installation, update lifecycle,
 * service worker registration, and network connectivity state management.
 */
export class PWAManager {
  constructor() {
    /** @type {Event|null} Captured beforeinstallprompt event */
    this.deferredPrompt = null;
    /** @type {ServiceWorkerRegistration|null} */
    this.registration = null;
    /** @type {boolean} */
    this.isInstalled = false;
    /** @type {boolean} */
    this.isOnline = typeof navigator !== 'undefined' ? navigator.onLine : true;

    this.installBanner = null;
    this.updateToast = null;
    this.statusBadge = null;
  }

  /**
   * Initialize PWA handlers, register Service Worker, and bind UI events.
   */
  async init() {
    this._bindNetworkEvents();
    this._bindInstallPrompt();
    this._initUIElements();

    if ('serviceWorker' in navigator && (window.location.protocol === 'https:' || window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1')) {
      await this._registerServiceWorker();
    }
  }

  _initUIElements() {
    this.installBanner = document.getElementById('pwa-install-banner');
    this.updateToast = document.getElementById('pwa-update-toast');

    // Add offline status badge to status bar
    const statusBar = document.getElementById('status-bar');
    if (statusBar && !document.getElementById('pwa-offline-badge')) {
      this.statusBadge = document.createElement('span');
      this.statusBadge.id = 'pwa-offline-badge';
      this.statusBadge.className = `offline-badge ${this.isOnline ? 'online' : 'offline'}`;
      this.statusBadge.innerHTML = this.isOnline ? '&#9679; Online' : '&#9679; Offline Mode';
      this.statusBadge.title = this.isOnline ? 'Connected to live planetary data feeds' : 'Using cached application shell and offline data';
      this.statusBadge.style.marginRight = '8px';
      this.statusBadge.style.display = 'inline-flex';
      statusBar.insertBefore(this.statusBadge, statusBar.firstChild);
    }
  }

  async _registerServiceWorker() {
    try {
      this.registration = await navigator.serviceWorker.register('./sw.js', {
        scope: './'
      });
      console.log('[PWA] Service Worker registered with scope:', this.registration.scope);

      // Check for updates
      this.registration.addEventListener('updatefound', () => {
        const newWorker = this.registration.installing;
        if (!newWorker) return;

        newWorker.addEventListener('statechange', () => {
          if (newWorker.state === 'installed' && navigator.serviceWorker.controller) {
            // New version installed and waiting
            this._showUpdatePrompt(newWorker);
          }
        });
      });

      // Handle controller change (when new SW takes over)
      navigator.serviceWorker.addEventListener('controllerchange', () => {
        console.log('[PWA] Controller changed, application updated.');
      });

    } catch (err) {
      console.warn('[PWA] Service Worker registration failed (normal in file:// or unsupported context):', err);
    }
  }

  _bindInstallPrompt() {
    window.addEventListener('beforeinstallprompt', (e) => {
      // Prevent automatic browser mini-infobar
      e.preventDefault();
      this.deferredPrompt = e;

      // Show in-app install banner
      if (this.installBanner) {
        this.installBanner.classList.add('visible');
      }

      // Dispatch custom event for menu buttons
      document.dispatchEvent(new CustomEvent('jmars:pwa-installable', { detail: { canInstall: true } }));
    });

    window.addEventListener('appinstalled', () => {
      this.isInstalled = true;
      this.deferredPrompt = null;
      if (this.installBanner) {
        this.installBanner.classList.remove('visible');
      }
      console.log('[PWA] JSMARS app installed successfully!');
      document.dispatchEvent(new CustomEvent('jmars:pwa-installed'));
    });
  }

  _bindNetworkEvents() {
    window.addEventListener('online', () => {
      this.isOnline = true;
      this._updateNetworkBadge();
      console.log('[PWA] Network status: Online');
    });

    window.addEventListener('offline', () => {
      this.isOnline = false;
      this._updateNetworkBadge();
      console.warn('[PWA] Network status: Offline. Operating from cached application shell.');
    });
  }

  _updateNetworkBadge() {
    if (!this.statusBadge) {
      this.statusBadge = document.getElementById('pwa-offline-badge');
    }
    if (this.statusBadge) {
      this.statusBadge.className = `offline-badge ${this.isOnline ? 'online' : 'offline'}`;
      this.statusBadge.innerHTML = this.isOnline ? '&#9679; Online' : '&#9679; Offline Mode';
      this.statusBadge.title = this.isOnline ? 'Connected to live planetary data feeds' : 'Using cached application shell and offline data';
    }
  }

  /**
   * Trigger the native installation prompt if available.
   * @returns {Promise<boolean>} True if user accepted installation
   */
  async promptInstall() {
    if (!this.deferredPrompt) {
      // If on iOS Safari, show custom instructions
      const isIOS = /iPad|iPhone|iPod/.test(navigator.userAgent) && !window.MSStream;
      if (isIOS) {
        alert('To install JSMARS on iOS:\n1. Tap the Share button (square with arrow)\n2. Scroll down and tap "Add to Home Screen"');
      } else {
        alert('Installation is not currently prompted by this browser, or JSMARS is already installed.');
      }
      return false;
    }

    this.deferredPrompt.prompt();
    const { outcome } = await this.deferredPrompt.userChoice;
    this.deferredPrompt = null;
    if (this.installBanner) {
      this.installBanner.classList.remove('visible');
    }
    return outcome === 'accepted';
  }

  _showUpdatePrompt(waitingWorker) {
    if (!this.updateToast) return;

    this.updateToast.classList.add('visible');
    const reloadBtn = this.updateToast.querySelector('.pwa-reload-btn');
    if (reloadBtn) {
      reloadBtn.onclick = () => {
        if (waitingWorker) {
          waitingWorker.postMessage({ type: 'SKIP_WAITING' });
        }
        window.location.reload();
      };
    }
  }
}
