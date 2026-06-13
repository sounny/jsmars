/**
 * @module SearchBar
 * @description Provides a search input for Mars landmarks.
 * Loads landmark data from a local JSON file and shows
 * a filtered dropdown as the user types. Selecting a
 * result pans the map to that landmark.
 */
export class SearchBar {
    /**
     * Create a new SearchBar.
     * @param {L.Map} map - Leaflet map instance
     * @param {string} containerId - DOM id of the search bar container
     */
    constructor(map, containerId) {
        this.map = map;
        this.container = document.getElementById(containerId);
        /** @type {Array<{name: string, lat: number, lon: number}>} */
        this.landmarks = [];
        /** @type {HTMLDivElement|null} */
        this.resultsContainer = null;
        /** @type {number|null} Debounce timer id */
        this._debounceTimer = null;

        if (!this.container) {
            console.error(`SearchBar container '${containerId}' not found.`);
            return;
        }

        // Register the global click-outside listener ONCE in the constructor,
        // not inside render(), to avoid duplicate listeners on re-render.
        this._boundOutsideClick = (e) => {
            if (this.resultsContainer && !this.container.contains(e.target)) {
                this.resultsContainer.style.display = 'none';
            }
        };
        document.addEventListener('click', this._boundOutsideClick);

        this.init();
    }

    /**
     * Initialize the search bar: load data then render.
     * @private
     */
    async init() {
        await this.loadLandmarks();
        this.render();
    }

    /**
     * Fetch landmark data from the local JSON file.
     * @private
     */
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

    /**
     * Build the search input and results dropdown DOM.
     * Does NOT register a global click listener (that is done once in the constructor).
     * @private
     */
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

        // Debounced input handler (200ms)
        input.addEventListener('input', (e) => {
            clearTimeout(this._debounceTimer);
            this._debounceTimer = setTimeout(() => this.handleInput(e.target.value), 200);
        });
        input.addEventListener('focus', (e) => this.handleInput(e.target.value));

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
    }

    /**
     * Filter landmarks by query and render matching results.
     * @param {string} query - Current input value
     */
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

    /**
     * Render the filtered results dropdown.
     * @param {Array<{name: string, lat: number, lon: number}>} matches - Matching landmarks
     * @private
     */
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

    /**
     * Pan the map to the selected landmark.
     * Normalizes longitude from 0-360 to -180/180 if needed.
     * @param {{name: string, lat: number, lon: number}} landmark - Selected landmark
     */
    selectLandmark(landmark) {
        // Normalize longitude for Leaflet's -180/180 range
        let targetLon = landmark.lon;
        if (targetLon > 180) targetLon -= 360;

        this.map.setView([landmark.lat, targetLon], 6);
        this.resultsContainer.style.display = 'none';

        // Update input value
        const input = this.container.querySelector('input');
        if (input) input.value = landmark.name;
    }
}
