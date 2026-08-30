/**
 * @module InteractionLogger
 * @description Global user interaction and UI telemetry tracer for JSMARS.
 */
export class InteractionLogger {
  static init() {
    if (this._initialized) return;
    this._initialized = true;
    console.log('%c🚀 [JSMARS] UI Interaction & Event Tracer Active', 'color: #38bdf8; font-weight: bold; font-size: 12px;');

    document.addEventListener('click', (e) => {
      const target = e.target;
      const button = target.closest('button, .tool-btn, .accordion-header, .crater-action-btn, .stretch-preset, a.btn');
      if (button) {
        const id = button.id ? '#' + button.id : '';
        const text = (button.innerText || button.getAttribute('aria-label') || button.title || '(icon)').trim().replace(/\\s+/g, ' ');
        const role = button.className ? '.' + button.className.split(' ').join('.') : '';
        console.log('%c[JSMARS:UI:Click] %c' + text + ' %c' + id + ' (' + role + ')', 'color: #10b981; font-weight: bold;', 'color: #f8fafc; font-weight: 600;', 'color: #94a3b8;');
        return;
      }
      if (target.type === 'checkbox' || target.type === 'radio') {
        const id = target.id ? '#' + target.id : '';
        console.log('%c[JSMARS:UI:Toggle] %c' + id + ' -> ' + (target.checked ? 'CHECKED' : 'UNCHECKED'), 'color: #a855f7; font-weight: bold;', 'color: #f8fafc;');
      }
    }, true);

    document.addEventListener('change', (e) => {
      const target = e.target;
      if (target.tagName === 'SELECT') {
        const id = target.id ? '#' + target.id : '';
        const selectedText = target.options[target.selectedIndex] ? target.options[target.selectedIndex].text : target.value;
        console.log('%c[JSMARS:UI:Select] %c' + id + ' -> ' + selectedText + ' (value: ' + target.value + ')', 'color: #f59e0b; font-weight: bold;', 'color: #f8fafc;');
      } else if (target.tagName === 'INPUT' && target.type !== 'checkbox' && target.type !== 'radio') {
        const id = target.id ? '#' + target.id : '';
        console.log('%c[JSMARS:UI:Input] %c' + id + ' -> ' + target.value, 'color: #f59e0b; font-weight: bold;', 'color: #f8fafc;');
      }
    }, true);

    let rangeTimeout = null;
    document.addEventListener('input', (e) => {
      const target = e.target;
      if (target.type === 'range') {
        clearTimeout(rangeTimeout);
        rangeTimeout = setTimeout(() => {
          const id = target.id ? '#' + target.id : '';
          console.log('%c[JSMARS:UI:Slider] %c' + id + ' -> ' + target.value, 'color: #ec4899; font-weight: bold;', 'color: #f8fafc;');
        }, 120);
      }
    }, true);
  }
}
