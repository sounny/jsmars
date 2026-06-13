import { EVENTS } from '../../constants.js';

/**
 * @module Bookmarks
 * @description Lightweight bookmark store for the search panel.
 *
 * Saves and loads bookmarks from localStorage and dispatches
 * BOOKMARKS_UPDATED when the list changes.
 */
export class Bookmarks {
    /**
     * Create a Bookmarks instance, loading any previously stored data.
     */
    constructor() {
        this.storageKey = 'jmars_bookmarks';
        this.bookmarks = this.load();
    }

    /**
     * Load bookmarks from localStorage.
     * @returns {Array<object>} Parsed bookmarks, or empty array on failure.
     */
    load() {
        const data = localStorage.getItem(this.storageKey);
        if (!data) return [];
        try {
            return JSON.parse(data);
        } catch (e) {
            console.error('Failed to parse bookmarks from localStorage', e);
            return [];
        }
    }

    /**
     * Save a new bookmark.
     * @param {string} name - Display name for the bookmark.
     * @param {object} view - View state with lat, lng, zoom.
     * @returns {object} The newly created bookmark record.
     */
    save(name, view) {
        // view = { lat, lng, zoom }
        const newBookmark = { name, view, id: crypto.randomUUID?.() || `${Date.now()}-${Math.random().toString(36).substring(2, 9)}` };
        this.bookmarks.push(newBookmark);
        this.persist();
        return newBookmark;
    }

    /**
     * Remove a bookmark by ID.
     * @param {number} id - Bookmark ID.
     */
    remove(id) {
        this.bookmarks = this.bookmarks.filter(b => b.id !== id);
        this.persist();
    }

    /**
     * Persist bookmarks to localStorage and dispatch an update event.
     */
    persist() {
        localStorage.setItem(this.storageKey, JSON.stringify(this.bookmarks));
        document.dispatchEvent(new CustomEvent(EVENTS.BOOKMARKS_UPDATED, { detail: this.bookmarks }));
    }

    /**
     * Get all bookmarks.
     * @returns {Array<object>} Current bookmarks array.
     */
    getAll() {
        return this.bookmarks;
    }
}
