/**
 * Centralized event bus for jsMars.
 * Wraps document CustomEvents for loose coupling between modules.
 */
export class EventBus {
  /**
   * Subscribe to an event.
   * @param {string} eventName - Event name from EVENTS constants
   * @param {Function} callback - Handler function receiving event.detail
   * @returns {Function} Unsubscribe function
   */
  static on(eventName, callback) {
    const handler = (e) => callback(e.detail);
    document.addEventListener(eventName, handler);
    return () => document.removeEventListener(eventName, handler);
  }

  /**
   * Emit an event.
   * @param {string} eventName - Event name
   * @param {*} detail - Event payload
   */
  static emit(eventName, detail = null) {
    document.dispatchEvent(new CustomEvent(eventName, { detail }));
  }

  /**
   * Subscribe to an event, but only fire once.
   * @param {string} eventName
   * @param {Function} callback
   */
  static once(eventName, callback) {
    const handler = (e) => {
      document.removeEventListener(eventName, handler);
      callback(e.detail);
    };
    document.addEventListener(eventName, handler);
  }
}
