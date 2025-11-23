export class Bookmarks {
    constructor() {
        this.storageKey = 'jmars_bookmarks';
        this.bookmarks = this.load();
    }

    load() {
        const data = localStorage.getItem(this.storageKey);
        return data ? JSON.parse(data) : [];
    }

    save(name, view) {
        // view = { lat, lng, zoom }
        const newBookmark = { name, view, id: Date.now() };
        this.bookmarks.push(newBookmark);
        this.persist();
        return newBookmark;
    }

    remove(id) {
        this.bookmarks = this.bookmarks.filter(b => b.id !== id);
        this.persist();
    }

    persist() {
        localStorage.setItem(this.storageKey, JSON.stringify(this.bookmarks));
        document.dispatchEvent(new CustomEvent('jmars-bookmarks-updated', { detail: this.bookmarks }));
    }

    getAll() {
        return this.bookmarks;
    }
}
