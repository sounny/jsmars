/**
 * @module EventBus
 * @description Centralized event bus for jsMars using DOM CustomEvents.
 *
 * Provides a thin wrapper over `document.addEventListener` and
 * `document.dispatchEvent` for loose coupling between modules.
 *
 * All event names should use EVENTS constants from `constants.js`
 * to prevent typos and ensure consistency.
 *
 * @example
 * import { EventBus } from './EventBus.js';
 * import { EVENTS } from '../constants.js';
 *
 * // Subscribe
 * const unsub = EventBus.on(EVENTS.BODY_CHANGED, (detail) => {
 *   console.log('Body changed to:', detail.body);
 * });
 *
 * // Emit
 * EventBus.emit(EVENTS.BODY_CHANGED, { body: 'moon' });
 *
 * // Unsubscribe
 * unsub();
 */
export class EventBus {
  /**
   * Subscribe to an event.
   * @param {string} eventName - Event name (use EVENTS constants).
   * @param {Function} callback - Handler receiving the event's `detail` payload.
   * @returns {Function} Unsubscribe function; call it to remove the listener.
   */
  static on(eventName, callback) {
    const handler = (e) => callback(e.detail);
    document.addEventListener(eventName, handler);
    return () => document.removeEventListener(eventName, handler);
  }

  /**
   * Emit a custom event on the document.
   * @param {string} eventName - Event name (use EVENTS constants).
   * @param {*} [detail=null] - Event payload accessible via `event.detail`.
   */
  static emit(eventName, detail = null) {
    document.dispatchEvent(new CustomEvent(eventName, { detail }));
  }

  /**
   * Subscribe to an event, firing the callback only once.
   * The listener auto-removes after the first invocation.
   * @param {string} eventName - Event name (use EVENTS constants).
   * @param {Function} callback - Handler receiving the event's `detail` payload.
   * @returns {Function} Unsubscribe function; call it to cancel before the event fires.
   */
  static once(eventName, callback) {
    const handler = (e) => {
      document.removeEventListener(eventName, handler);
      callback(e.detail);
    };
    document.addEventListener(eventName, handler);
    // Return unsubscribe for consistency with on()
    return () => document.removeEventListener(eventName, handler);
  }
}
