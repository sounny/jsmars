/**
 * Centralized event constants for jsMars.
 * All cross-module communication uses these event names
 * dispatched via document CustomEvent.
 */
export const EVENTS = {
  // Body & Map
  BODY_CHANGED: 'jmars:body-changed',
  LAYERS_CHANGED: 'layers-changed',
  LAYERS_UPDATED: 'jmars-layers-updated',
  OVERLAYS_CHANGED: 'overlays-changed',
  UPDATE_VIEW: 'jmars:update-view',

  // Tools
  TOOL_ACTIVATED: 'jmars:tool-activated',
  TOOL_DEACTIVATED: 'jmars-tool-deactivated',

  // Crater Counting
  CRATER_ADDED: 'jmars-crater-added',
  CRATER_REMOVE: 'jmars-crater-remove-request',
  CRATER_CLEAR: 'jmars-crater-clear-request',

  // Measurements
  MEASURE_UPDATED: 'jmars-measurements-updated',
  MEASURE_HIGHLIGHT: 'jmars-measurement-highlight',

  // Sampling
  SAMPLE_UPDATED: 'jmars-samples-updated',
  SAMPLE_EXPORT_REQUEST: 'jmars-sample-export-request',
  SAMPLE_CLEAR_REQUEST: 'jmars-sample-clear-request',

  // Shapes
  SHAPE_CREATED: 'jmars:shape-created',
  SHAPE_UPDATED: 'jmars:shape-updated',
  SHAPE_DELETED: 'jmars:shape-deleted',
  SHAPE_SELECTED: 'jmars:shape-selected',
  SHAPES_IMPORTED: 'jmars:shapes-imported',

  // Stamps
  STAMP_QUERY_START: 'jmars:stamp-query-start',
  STAMP_QUERY_COMPLETE: 'jmars:stamp-query-complete',
  STAMP_SELECTED: 'jmars:stamp-selected',
  STAMP_IMAGE_LOADED: 'jmars:stamp-image-loaded',

  // Ground Track
  GROUNDTRACK_LOADED: 'jmars:groundtrack-loaded',
  GROUNDTRACK_TOGGLED: 'jmars:groundtrack-toggled',

  // Places
  PLACE_SELECTED: 'jmars:place-selected',
  PLACE_SAVED: 'jmars:place-saved',

  // Export
  EXPORT_REQUESTED: 'jmars:export-requested',

  // Landing Sites
  LANDING_SITES_TOGGLED: 'jmars:landing-sites-toggled',

  // Coordinate Format
  COORD_FORMAT_CHANGED: 'jmars:coord-format-changed'
};
