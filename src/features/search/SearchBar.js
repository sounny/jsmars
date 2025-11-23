import { searchLandmarks } from './LandmarkDB.js';
import { Bookmarks } from './Bookmarks.js';

export class SearchBar {
    constructor(map, containerId) {
        this.map = map;
        this.container = document.getElementById(containerId);
        this.bookmarks = new Bookmarks();
        this.results = [];

        if (!this.container) {
            console.warn('SearchBar container not found');
            return;
        }

        this.render();
        this.bindEvents();
    }

    render() {
        this.container.innerHTML = `
            <div class="search-bar-wrapper">
                <input type="text" id="jmars-search-input" placeholder="Search Mars...">
                <button id="jmars-bookmark-btn" title="Bookmark View">★</button>
                
                <div id="jmars-search-results"></div>
            </div>
        `;

        this.input = this.container.querySelector('#jmars-search-input');
        this.resultsContainer = this.container.querySelector('#jmars-search-results');
    }

    bindEvents() {
        // Search Input
        this.input.addEventListener('input', (e) => {
            const query = e.target.value;
            if (query.length > 1) {
                this.showResults(query);
            } else {
                this.hideResults();
            }
        });

        // Bookmark Button
        this.container.querySelector('#jmars-bookmark-btn').addEventListener('click', () => {
            const name = prompt("Name this bookmark:", "My View");
            if (name) {
                const center = this.map.getCenter();
                this.bookmarks.save(name, {
                    lat: center.lat,
                    lng: center.lng,
                    zoom: this.map.getZoom()
                });
                alert("Bookmark saved!");
            }
        });

        // Hide results on click outside
        document.addEventListener('click', (e) => {
            if (!this.container.contains(e.target)) {
                this.hideResults();
            }
        });
    }

    showResults(query) {
        const landmarks = searchLandmarks(query);
        const savedBookmarks = this.bookmarks.getAll().filter(b => b.name.toLowerCase().includes(query.toLowerCase()));

        this.resultsContainer.innerHTML = '';

        if (landmarks.length === 0 && savedBookmarks.length === 0) {
            this.resultsContainer.style.display = 'none';
            return;
        }

        // Render Landmarks
        if (landmarks.length > 0) {
            const header = document.createElement('div');
            header.className = 'search-result-header';
            header.innerText = 'LANDMARKS';
            this.resultsContainer.appendChild(header);

            landmarks.forEach(l => {
                const div = document.createElement('div');
                div.className = 'search-result-item';
                div.innerHTML = `<strong>${l.name}</strong> <span style="color: #888; font-size: 11px;">${l.type}</span>`;
                div.addEventListener('click', () => {
                    this.map.flyTo([l.lat, l.lng], 6);
                    this.hideResults();
                    this.input.value = l.name;
                });
                this.resultsContainer.appendChild(div);
            });
        }

        // Render Bookmarks
        if (savedBookmarks.length > 0) {
            const header = document.createElement('div');
            header.className = 'search-result-header';
            header.innerText = 'BOOKMARKS';
            this.resultsContainer.appendChild(header);

            savedBookmarks.forEach(b => {
                const div = document.createElement('div');
                div.className = 'search-result-item';
                div.innerHTML = `<strong>${b.name}</strong> <span style="color: #888; font-size: 11px;">Bookmark</span>`;
                div.addEventListener('click', () => {
                    this.map.flyTo([b.view.lat, b.view.lng], b.view.zoom);
                    this.hideResults();
                    this.input.value = b.name;
                });
                this.resultsContainer.appendChild(div);
            });
        }

        this.resultsContainer.style.display = 'block';
    }

    hideResults() {
        this.resultsContainer.style.display = 'none';
    }
}
