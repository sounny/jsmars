export class SearchBar {
    constructor(map, containerId) {
        this.map = map;
        this.container = document.getElementById(containerId);
        this.landmarks = [];
        this.resultsContainer = null;

        if (!this.container) {
            console.error(`SearchBar container '${containerId}' not found.`);
            return;
        }

        this.init();
    }

    async init() {
        await this.loadLandmarks();
        this.render();
    }

    async loadLandmarks() {
        try {
            const response = await fetch('./src/data/landmarks.json');
            if (!response.ok) throw new Error('Failed to load landmarks');
            this.landmarks = await response.json();
        } catch (error) {
            console.error('Error loading landmarks:', error);
            this.landmarks = [];
        }
    }

    render() {
        this.container.innerHTML = '';
        this.container.style.position = 'relative';

        // Input
        const input = document.createElement('input');
        input.type = 'text';
        input.placeholder = 'Search Mars...';
        input.style.width = '100%';
        input.style.padding = '8px';
        input.style.boxSizing = 'border-box';
        input.style.background = '#222';
        input.style.border = '1px solid #555';
        input.style.color = '#eee';
        input.style.borderRadius = '4px';

        input.addEventListener('input', (e) => this.handleInput(e.target.value));
        input.addEventListener('focus', (e) => this.handleInput(e.target.value)); // Show results on focus if text exists

        // Results Dropdown
        this.resultsContainer = document.createElement('div');
        this.resultsContainer.style.position = 'absolute';
        this.resultsContainer.style.top = '100%';
        this.resultsContainer.style.left = '0';
        this.resultsContainer.style.right = '0';
        this.resultsContainer.style.background = '#222';
        this.resultsContainer.style.border = '1px solid #555';
        this.resultsContainer.style.borderTop = 'none';
        this.resultsContainer.style.zIndex = '1000';
        this.resultsContainer.style.maxHeight = '200px';
        this.resultsContainer.style.overflowY = 'auto';
        this.resultsContainer.style.display = 'none';

        this.container.appendChild(input);
        this.container.appendChild(this.resultsContainer);

        // Close on click outside
        document.addEventListener('click', (e) => {
            if (!this.container.contains(e.target)) {
                this.resultsContainer.style.display = 'none';
            }
        });
    }

    handleInput(query) {
        if (!query || query.trim() === '') {
            this.resultsContainer.style.display = 'none';
            return;
        }

        const lowerQuery = query.toLowerCase();
        const matches = this.landmarks.filter(l =>
            l.name.toLowerCase().includes(lowerQuery)
        );

        this.renderResults(matches);
    }

    renderResults(matches) {
        this.resultsContainer.innerHTML = '';

        if (matches.length === 0) {
            const noRes = document.createElement('div');
            noRes.textContent = 'No results found';
            noRes.style.padding = '8px';
            noRes.style.color = '#888';
            noRes.style.fontStyle = 'italic';
            this.resultsContainer.appendChild(noRes);
        } else {
            matches.forEach(match => {
                const item = document.createElement('div');
                item.textContent = match.name;
                item.style.padding = '8px';
                item.style.cursor = 'pointer';
                item.style.borderBottom = '1px solid #333';

                item.addEventListener('mouseover', () => {
                    item.style.background = '#333';
                });
                item.addEventListener('mouseout', () => {
                    item.style.background = 'transparent';
                });

                item.addEventListener('click', () => {
                    this.selectLandmark(match);
                });

                this.resultsContainer.appendChild(item);
            });
        }

        this.resultsContainer.style.display = 'block';
    }

    selectLandmark(landmark) {
        // JMARS usually uses 0-360 East positive. Leaflet uses -180/180.
        // Our data:
        // Olympus Mons: Lon 226.2. In Leaflet -180/180: 226.2 - 360 = -133.8
        // Let's assume the map handles standard lat/lon. If the map is configured for 0-360, we might need conversion.
        // However, Leaflet's default CRS is EPSG:3857 which wraps.
        // Let's try direct usage first, but if it fails we might need normalization.

        // Simple normalization for Leaflet if needed:
        let targetLon = landmark.lon;
        if (targetLon > 180) targetLon -= 360;

        this.map.setView([landmark.lat, targetLon], 6); // Zoom level 6 for landmark
        this.resultsContainer.style.display = 'none';

        // Update input value
        const input = this.container.querySelector('input');
        if (input) input.value = landmark.name;
    }
}
