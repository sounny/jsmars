/**
 * @module BookmarksTool
 * @description Provides a UI for saving, listing, navigating to,
 * and deleting map view bookmarks.
 *
 * Bookmarks are persisted in localStorage under the key 'jmars_bookmarks'
 * (matching the key used by the search/Bookmarks.js module).
 */
export class BookmarksTool {
    /**
     * Create a BookmarksTool.
     * @param {L.Map} map - The Leaflet map instance.
     * @param {string} containerId - DOM element ID for the bookmarks panel.
     */
    constructor(map, containerId) {
        this.map = map;
        this.container = document.getElementById(containerId);
        this.bookmarks = [];
        
        if (this.container) {
            this.init();
        }
    }

    /**
     * Load stored bookmarks and render the initial UI.
     */
    init() {
        this.loadFromStorage();
        this.render();
    }

    /**
     * Load bookmarks from localStorage.
     */
    loadFromStorage() {
        const stored = localStorage.getItem('jmars_bookmarks');
        if (stored) {
            try {
                this.bookmarks = JSON.parse(stored);
            } catch (e) {
                console.error('Failed to parse bookmarks', e);
                this.bookmarks = [];
            }
        }
    }

    /**
     * Persist current bookmarks to localStorage.
     */
    saveToStorage() {
        localStorage.setItem('jmars_bookmarks', JSON.stringify(this.bookmarks));
    }

    /**
     * Save the current map view as a new bookmark.
     * Prompts the user for a name.
     */
    addCurrentView() {
        const center = this.map.getCenter();
        const zoom = this.map.getZoom();
        const name = prompt("Enter a name for this bookmark:", `View ${this.bookmarks.length + 1}`);
        
        if (name) {
            this.bookmarks.push({
                id: crypto.randomUUID?.() || `${Date.now()}-${Math.random().toString(36).substring(2, 9)}`,
                name: name,
                lat: center.lat,
                lng: center.lng,
                zoom: zoom
            });
            this.saveToStorage();
            this.render();
        }
    }

    /**
     * Pan and zoom the map to a saved bookmark.
     * @param {object} bookmark - Bookmark record with lat, lng, zoom.
     */
    goTo(bookmark) {
        this.map.setView([bookmark.lat, bookmark.lng], bookmark.zoom);
    }

    /**
     * Delete a bookmark by ID after confirmation.
     * @param {number} id - Bookmark ID.
     */
    remove(id) {
        if (confirm('Delete this bookmark?')) {
            this.bookmarks = this.bookmarks.filter(b => b.id !== id);
            this.saveToStorage();
            this.render();
        }
    }

    /**
     * Render the bookmarks list UI.
     */
    render() {
        this.container.innerHTML = '';
        
        const wrapper = document.createElement('div');
        wrapper.className = 'bookmarks-block';

        const header = document.createElement('h4');
        header.textContent = 'Bookmarks';
        wrapper.appendChild(header);

        const addBtn = document.createElement('button');
        addBtn.className = 'tool-btn';
        addBtn.textContent = '+ Add Bookmark';
        addBtn.type = 'button';
        addBtn.onclick = () => this.addCurrentView();
        wrapper.appendChild(addBtn);

        const list = document.createElement('div');
        list.style.marginTop = '10px';
        list.style.maxHeight = '150px';
        list.style.overflowY = 'auto';

        this.bookmarks.forEach(b => {
            const item = document.createElement('div');
            item.style.display = 'flex';
            item.style.justifyContent = 'space-between';
            item.style.alignItems = 'center';
            item.style.padding = '5px';
            item.style.borderBottom = '1px solid #333';
            item.style.fontSize = '12px';

            const link = document.createElement('span');
            link.textContent = b.name;
            link.style.cursor = 'pointer';
            link.style.color = '#eee';
            link.onclick = () => this.goTo(b);

            const delBtn = document.createElement('span');
            delBtn.innerHTML = '&times;';
            delBtn.style.color = '#f55';
            delBtn.style.cursor = 'pointer';
            delBtn.style.marginLeft = '10px';
            delBtn.onclick = () => this.remove(b.id);

            item.appendChild(link);
            item.appendChild(delBtn);
            list.appendChild(item);
        });

        if (this.bookmarks.length === 0) {
            const empty = document.createElement('div');
            empty.textContent = 'No bookmarks saved.';
            empty.style.color = '#888';
            empty.style.fontStyle = 'italic';
            empty.style.padding = '5px';
            list.appendChild(empty);
        }

        wrapper.appendChild(list);
        this.container.appendChild(wrapper);
    }
    
    /**
     * Get serializable bookmark data for session export.
     * @returns {Array<object>} Array of bookmark records.
     */
    getData() {
        return this.bookmarks;
    }

    /**
     * Load bookmarks from serialized data (session restore).
     * @param {Array<object>} data - Array of bookmark records.
     */
    loadData(data) {
        if (Array.isArray(data)) {
            this.bookmarks = data;
            this.saveToStorage();
            this.render();
        }
    }
}
