export class StatusBar {
    constructor(map, containerId) {
        this.map = map;
        this.container = document.getElementById(containerId);
        if (!this.container) return;

        this.initUI();
        this.bindEvents();
        this.update();
    }

    initUI() {
        this.container.innerHTML = `
            <div class="status-item" id="status-coords">Lat: 0.0000, Lon: 0.0000</div>
            <div class="status-item" id="status-zoom">Zoom: 0</div>
            <div class="status-item" id="status-scale"></div>
        `;

        this.coordsEl = this.container.querySelector('#status-coords');
        this.zoomEl = this.container.querySelector('#status-zoom');
        this.scaleEl = this.container.querySelector('#status-scale');

        // Initialize Leaflet scale control inside our custom container
        // We can't easily move the standard L.control.scale into a specific div 
        // without some hacking, or we can just let Leaflet handle the scale logic 
        // and append its container to ours.

        this.scaleControl = L.control.scale({
            position: 'bottomleft', // Position doesn't matter if we move the container
            maxWidth: 200,
            metric: true,
            imperial: false
        });

        // We need to add it to the map first to initialize, then move the element
        this.scaleControl.addTo(this.map);
        const scaleContainer = this.scaleControl.getContainer();
        this.scaleEl.appendChild(scaleContainer);

        // Remove the default leaflet-bottom leaflet-left classes to avoid positioning conflicts
        scaleContainer.classList.remove('leaflet-bottom', 'leaflet-left', 'leaflet-control');
        scaleContainer.style.margin = '0';
    }

    bindEvents() {
        this.map.on('mousemove', (e) => this.updateCoords(e.latlng));
        this.map.on('zoomend', () => this.updateZoom());
        // Scale updates automatically by Leaflet
    }

    updateCoords(latlng) {
        // Normalize longitude to 0-360 if desired, or keep -180/180
        // JMARS usually uses 0-360 East positive, but Leaflet is -180/180.
        // Let's stick to standard Lat/Lon for now.
        this.coordsEl.innerText = `Lat: ${latlng.lat.toFixed(4)}, Lon: ${latlng.lng.toFixed(4)}`;
    }

    updateZoom() {
        this.zoomEl.innerText = `Zoom: ${this.map.getZoom()}`;
    }

    update() {
        this.updateZoom();
        // Initial coords will be 0,0 until mouse move
    }
}
