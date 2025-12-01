import { jmarsState } from '../../jmars-state.js';

export class BookmarksTool {
    constructor(map, containerId) {
        this.map = map;
        this.container = document.getElementById(containerId);
        this.bookmarks = [];
        
        if (this.container) {
            this.init();
        }
    }

    init() {
        this.loadFromStorage();
        this.render();
    }

    loadFromStorage() {
        const stored = localStorage.getItem('jsmars_bookmarks');
        if (stored) {
            try {
                this.bookmarks = JSON.parse(stored);
            } catch (e) {
                console.error('Failed to parse bookmarks', e);
                this.bookmarks = [];
            }
        }
    }

    saveToStorage() {
        localStorage.setItem('jsmars_bookmarks', JSON.stringify(this.bookmarks));
    }

    addCurrentView() {
        const center = this.map.getCenter();
        const zoom = this.map.getZoom();
        const name = prompt("Enter a name for this bookmark:", `View ${this.bookmarks.length + 1}`);
        
        if (name) {
            this.bookmarks.push({
                id: Date.now(),
                name: name,
                lat: center.lat,
                lng: center.lng,
                zoom: zoom
            });
            this.saveToStorage();
            this.render();
        }
    }

    goTo(bookmark) {
        this.map.setView([bookmark.lat, bookmark.lng], bookmark.zoom);
    }

    remove(id) {
        if (confirm('Delete this bookmark?')) {
            this.bookmarks = this.bookmarks.filter(b => b.id !== id);
            this.saveToStorage();
            this.render();
        }
    }

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
    
    // Session Integration
    getData() {
        return this.bookmarks;
    }

    loadData(data) {
        if (Array.isArray(data)) {
            this.bookmarks = data;
            this.saveToStorage(); // Sync with local storage too? Or just session?
            // Let's keep local storage as "global favorites" and session as "workspace".
            // If we load session, should we overwrite local bookmarks? 
            // JMARS usually treats bookmarks as global. 
            // Let's merge? Or just replace. 
            // For now, replace and save.
            this.render();
        }
    }
}
