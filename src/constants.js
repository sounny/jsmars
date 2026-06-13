/**
 * @module constants
 * @description Centralized event constants for jsMars.
 *
 * All cross-module communication uses these event names,
 * dispatched via document CustomEvent or jmarsState.emit().
 *
 * Naming convention: all events use the 'jmars:' prefix with
 * colon separator (e.g., 'jmars:body-changed').
 *
 * IMPORTANT: This object is frozen. Do not attempt to add
 * properties at runtime; instead, add them here.
 */
export const EVENTS = Object.freeze({
  // ── Body & Map ──────────────────────────────────────────────
  /** Fired when the user switches planetary bodies (Mars/Moon/Earth). */
  BODY_CHANGED: 'jmars:body-changed',
  /** Fired when the active layer stack changes (add/remove/reorder/opacity). */
  LAYERS_CHANGED: 'jmars:layers-changed',
  /** Fired when the list of available layers is updated (e.g., after WMS discovery). */
  LAYERS_UPDATED: 'jmars:layers-updated',
  /** Fired when overlay visibility toggles (graticule, scalebar, etc.). */
  OVERLAYS_CHANGED: 'jmars:overlays-changed',
  /** Fired to programmatically pan/zoom the map. */
  UPDATE_VIEW: 'jmars:update-view',

  // ── Tools ───────────────────────────────────────────────────
  /** Fired when a tool is activated (payload: { tool: string }). */
  TOOL_ACTIVATED: 'jmars:tool-activated',
  /** Fired when a tool is deactivated (payload: { tool: string }). */
  TOOL_DEACTIVATED: 'jmars:tool-deactivated',

  // ── Crater Counting ─────────────────────────────────────────
  /** Fired when a new crater is logged. */
  CRATER_ADDED: 'jmars:crater-added',
  /** Request to remove a specific crater by ID. */
  CRATER_REMOVE: 'jmars:crater-remove-request',
  /** Request to clear all craters. */
  CRATER_CLEAR: 'jmars:crater-clear-request',

  // ── Measurements ────────────────────────────────────────────
  /** Fired when the measurements list changes. */
  MEASURE_UPDATED: 'jmars:measurements-updated',
  /** Fired to highlight a specific measurement on the map. */
  MEASURE_HIGHLIGHT: 'jmars:measurement-highlight',

  // ── Sampling ────────────────────────────────────────────────
  /** Fired when the sample list changes. */
  SAMPLE_UPDATED: 'jmars:samples-updated',
  /** Request to export samples as CSV. */
  SAMPLE_EXPORT_REQUEST: 'jmars:sample-export-request',
  /** Request to clear all samples. */
  SAMPLE_CLEAR_REQUEST: 'jmars:sample-clear-request',

  // ── Profiles ────────────────────────────────────────────────
  /** Fired when a new elevation profile is generated (radial or linear). */
  PROFILE_GENERATED: 'jmars:profile-generated',

  // ── Shapes ──────────────────────────────────────────────────
  /** Fired when a new shape is drawn or added. */
  SHAPE_CREATED: 'jmars:shape-created',
  /** Fired when a shape's geometry or attributes change. */
  SHAPE_UPDATED: 'jmars:shape-updated',
  /** Fired when a shape is deleted. */
  SHAPE_DELETED: 'jmars:shape-deleted',
  /** Fired when a shape is selected/clicked. */
  SHAPE_SELECTED: 'jmars:shape-selected',
  /** Fired after shapes are imported from a file. */
  SHAPES_IMPORTED: 'jmars:shapes-imported',

  // ── Stamps (Footprints) ─────────────────────────────────────
  /** Fired when a stamp query begins. */
  STAMP_QUERY_START: 'jmars:stamp-query-start',
  /** Fired when stamp query results arrive. */
  STAMP_QUERY_COMPLETE: 'jmars:stamp-query-complete',
  /** Fired when a stamp footprint is selected. */
  STAMP_SELECTED: 'jmars:stamp-selected',
  /** Fired when a stamp image overlay is loaded. */
  STAMP_IMAGE_LOADED: 'jmars:stamp-image-loaded',

  // ── Ground Track ────────────────────────────────────────────
  /** Fired when ground track data is loaded. */
  GROUNDTRACK_LOADED: 'jmars:groundtrack-loaded',
  /** Fired when a ground track is toggled on/off. */
  GROUNDTRACK_TOGGLED: 'jmars:groundtrack-toggled',

  // ── Places & Bookmarks ──────────────────────────────────────
  /** Fired when a place is selected from the list. */
  PLACE_SELECTED: 'jmars:place-selected',
  /** Fired when a place is saved. */
  PLACE_SAVED: 'jmars:place-saved',
  /** Fired when bookmarks list changes. */
  BOOKMARKS_UPDATED: 'jmars:bookmarks-updated',

  // ── Export ──────────────────────────────────────────────────
  /** Fired when a map export is requested. */
  EXPORT_REQUESTED: 'jmars:export-requested',

  // ── Landing Sites ───────────────────────────────────────────
  /** Fired when landing sites layer is toggled. */
  LANDING_SITES_TOGGLED: 'jmars:landing-sites-toggled',

  // ── Coordinate Format ───────────────────────────────────────
  /** Fired when the user changes coordinate display format. */
  COORD_FORMAT_CHANGED: 'jmars:coord-format-changed'
});
