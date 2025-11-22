import { JMARS_CONFIG } from '../jmars-config.js';

export class Panner {
  constructor(jmarsMap) {
    this.mainMap = jmarsMap.map;
    this.container = null;
    this.miniMap = null;
    this.rect = null;
    this.isOpen = false;

    this.init();
  }

  init() {
    // Create container
    this.container = document.createElement('div');
    this.container.id = 'jmars-panner';
    this.container.style.position = 'absolute';
    this.container.style.bottom = '30px'; // Higher to avoid attribution
    this.container.style.right = '20px';  // Bottom Right
    this.container.style.width = '200px';
    this.container.style.height = '100px';
    this.container.style.border = '2px solid #555';
    this.container.style.borderRadius = '4px';
    this.container.style.zIndex = '1000';
    this.container.style.display = 'none'; // Hidden by default
    this.container.style.background = '#000';
    this.container.style.boxShadow = '0 0 10px rgba(0,0,0,0.5)';

    document.body.appendChild(this.container);

    // Initialize MiniMap
    this.miniMap = L.map(this.container, {
      attributionControl: false,
      zoomControl: false,
      crs: L.CRS.EPSG4326,
      center: [0, 0],
      zoom: 0,
      dragging: false,
      touchZoom: false,
      scrollWheelZoom: false,
      doubleClickZoom: false,
      boxZoom: false,
      keyboard: false
    });

    // Add Base Layer
    L.tileLayer(JMARS_CONFIG.services.mars_basemap, { maxZoom: 5 }).addTo(this.miniMap);

    // Add View Rect
    this.rect = L.rectangle(this.mainMap.getBounds(), { color: "#d6336c", weight: 1, fillOpacity: 0.2 }).addTo(this.miniMap);

    // Sync logic
    this.mainMap.on('move', () => this.update());
    this.mainMap.on('zoomend', () => this.update());

    // Initial update
    this.update();
  }

  update() {
    if (!this.isOpen) return;

    const bounds = this.mainMap.getBounds();

    // Update Rect
    this.rect.setBounds(bounds);

    // Keep global view
    this.miniMap.setView([0, 0], 0);
  }

  toggle(show) {
    this.isOpen = show;
    this.container.style.display = show ? 'block' : 'none';
    if (show) {
      this.miniMap.invalidateSize();
      this.update();
    }
  }
}
