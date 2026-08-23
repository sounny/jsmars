import { MarsTime } from './MarsTime.js';
import { EventBus } from '../../core/EventBus.js';
import { EVENTS } from '../../constants.js';

/**
 * @module TimeSlider
 * @description Mars Solar Longitude (Ls), Mars Calendar, and temporal simulation slider.
 *
 * Allows users to scrub through solar longitude (0-360°), Mars Year, and Earth Date,
 * broadcasting EVENTS.TIME_CHANGED to synchronize thermal, atmospheric, ground track,
 * and 3D illumination systems.
 */
export class TimeSlider {
  /**
   * @param {HTMLElement|string} container - Container element or element ID.
   */
  constructor(container) {
    this.container = typeof container === 'string' ? document.getElementById(container) : container;
    this.currentState = MarsTime.computeState(new Date());
    this.isPlaying = false;
    this.animationTimer = null;
    this.playSpeed = 1.0; // Ls degrees per second in playback

    if (this.container) {
      this.init();
    }
  }

  init() {
    this.container.innerHTML = `
      <div class="time-slider-panel" style="padding: 10px; background: rgba(20,24,30,0.85); border-radius: 6px; border: 1px solid #334155; font-family: sans-serif; color: #e2e8f0; font-size: 12px;">
        <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px;">
          <span style="font-weight: 600; color: #f97316;">Mars Solar Time (L<sub>s</sub>)</span>
          <span id="ts-season-badge" style="background: #1e293b; padding: 2px 6px; border-radius: 4px; font-size: 10px; color: #94a3b8; border: 1px solid #475569;">
            ${this.currentState.season.north}
          </span>
        </div>

        <div style="margin-bottom: 8px;">
          <div style="display: flex; justify-content: space-between; margin-bottom: 4px; font-size: 11px; color: #cbd5e1;">
            <span>L<sub>s</sub>: <b id="ts-ls-val" style="color: #fb923c;">${this.currentState.Ls.toFixed(1)}°</b></span>
            <span>MY: <b id="ts-my-val">${this.currentState.MY}</b> | Sol: <b id="ts-sol-val">${this.currentState.msd.toFixed(1)}</b></span>
          </div>
          <input type="range" id="ts-ls-slider" min="0" max="360" step="0.5" value="${this.currentState.Ls.toFixed(1)}" style="width: 100%; accent-color: #f97316; cursor: pointer;">
          <div style="display: flex; justify-content: space-between; font-size: 9px; color: #64748b; margin-top: 2px;">
            <span>0° (Spring Eq.)</span>
            <span>90° (Solstice)</span>
            <span>180° (Autumn)</span>
            <span>270° (Perihelion)</span>
            <span>360°</span>
          </div>
        </div>

        <div style="background: #0f172a; padding: 6px 8px; border-radius: 4px; margin-bottom: 8px; font-size: 10px; display: grid; grid-template-columns: 1fr 1fr; gap: 4px; border: 1px solid #1e293b;">
          <div>Sun Dist: <span id="ts-dist-val" style="color:#38bdf8;">${this.currentState.r_AU.toFixed(3)} AU</span></div>
          <div>Subsolar Lat: <span id="ts-sublat-val" style="color:#38bdf8;">${this.currentState.subSolarLat.toFixed(1)}°</span></div>
          <div>Insolation: <span id="ts-insol-val" style="color:#38bdf8;">${Math.round(this.currentState.solarInsolation)} W/m²</span></div>
          <div>MTC (UTC): <span id="ts-mtc-val" style="color:#38bdf8;">${MarsTime.formatHours(this.currentState.mtc)}</span></div>
        </div>

        <div style="display: flex; gap: 6px; align-items: center;">
          <button id="ts-now-btn" class="tool-btn" style="flex: 1; padding: 4px; font-size: 11px;">Now</button>
          <button id="ts-play-btn" class="tool-btn" style="flex: 1; padding: 4px; font-size: 11px; background: #0369a1;">▶ Play</button>
          <select id="ts-speed-select" class="tool-select" style="flex: 0.8; padding: 3px 6px; font-size: 11px;">
            <option value="1">1x</option>
            <option value="5">5x</option>
            <option value="20">20x</option>
          </select>
        </div>
      </div>
    `;

    this.lsSlider = this.container.querySelector('#ts-ls-slider');
    this.lsVal = this.container.querySelector('#ts-ls-val');
    this.myVal = this.container.querySelector('#ts-my-val');
    this.solVal = this.container.querySelector('#ts-sol-val');
    this.seasonBadge = this.container.querySelector('#ts-season-badge');
    this.distVal = this.container.querySelector('#ts-dist-val');
    this.subLatVal = this.container.querySelector('#ts-sublat-val');
    this.insolVal = this.container.querySelector('#ts-insol-val');
    this.mtcVal = this.container.querySelector('#ts-mtc-val');
    this.nowBtn = this.container.querySelector('#ts-now-btn');
    this.playBtn = this.container.querySelector('#ts-play-btn');
    this.speedSelect = this.container.querySelector('#ts-speed-select');

    this.bindEvents();
    this.broadcast();
  }

  bindEvents() {
    this.lsSlider.addEventListener('input', (e) => {
      this.setLs(parseFloat(e.target.value));
    });

    this.nowBtn.addEventListener('click', () => {
      this.stopPlayback();
      this.currentState = MarsTime.computeState(new Date());
      this.updateUI();
      this.broadcast();
    });

    this.playBtn.addEventListener('click', () => {
      this.togglePlayback();
    });

    this.speedSelect.addEventListener('change', (e) => {
      this.playSpeed = parseFloat(e.target.value);
    });
  }

  setLs(newLs) {
    newLs = ((newLs % 360) + 360) % 360;
    // Keep current MY and compute updated state
    const currentMY = this.currentState.MY;
    // Approximate sol of year from Ls
    const solOfYear = (newLs / 360) * 668.6;
    const approxMsd = 28352.0 + (currentMY - 1) * 668.6 + solOfYear;
    
    // Compute exact orbital coordinates from Ls
    const nu = newLs; // Approx anomaly for slider setting
    const e = MarsTime.ECCENTRICITY;
    const r_AU = (MarsTime.SEMI_MAJOR_AXIS * (1 - e * e)) / (1 + e * Math.cos(nu * Math.PI / 180));
    const solarInsolation = MarsTime.SOLAR_CONSTANT_1AU / (r_AU * r_AU);
    const delta_s = Math.asin(Math.sin(MarsTime.OBLIQUITY * Math.PI / 180) * Math.sin(newLs * Math.PI / 180)) * 180 / Math.PI;

    this.currentState = {
      ...this.currentState,
      Ls: newLs,
      msd: approxMsd,
      r_AU,
      solarInsolation,
      subSolarLat: delta_s,
      season: MarsTime.getSeason(newLs)
    };

    this.updateUI();
    this.broadcast();
  }

  updateUI() {
    if (!this.lsSlider) return;
    this.lsSlider.value = this.currentState.Ls.toFixed(1);
    this.lsVal.innerHTML = `${this.currentState.Ls.toFixed(1)}°`;
    this.myVal.innerText = this.currentState.MY;
    this.solVal.innerText = this.currentState.msd.toFixed(1);
    this.seasonBadge.innerText = this.currentState.season.north;
    this.distVal.innerText = `${this.currentState.r_AU.toFixed(3)} AU`;
    this.subLatVal.innerText = `${this.currentState.subSolarLat.toFixed(1)}°`;
    this.insolVal.innerText = `${Math.round(this.currentState.solarInsolation)} W/m²`;
    this.mtcVal.innerText = MarsTime.formatHours(this.currentState.mtc);
  }

  togglePlayback() {
    if (this.isPlaying) {
      this.stopPlayback();
    } else {
      this.startPlayback();
    }
  }

  startPlayback() {
    this.isPlaying = true;
    this.playBtn.innerHTML = '⏸ Pause';
    this.playBtn.style.background = '#e11d48';

    const interval = 50; // ms
    this.animationTimer = setInterval(() => {
      const step = (this.playSpeed * (interval / 1000) * 10);
      const nextLs = (this.currentState.Ls + step) % 360;
      this.setLs(nextLs);
    }, interval);
  }

  stopPlayback() {
    this.isPlaying = false;
    if (this.animationTimer) {
      clearInterval(this.animationTimer);
      this.animationTimer = null;
    }
    if (this.playBtn) {
      this.playBtn.innerHTML = '▶ Play';
      this.playBtn.style.background = '#0369a1';
    }
  }

  broadcast() {
    EventBus.emit(EVENTS.TIME_CHANGED, {
      ...this.currentState
    });
  }

  getState() {
    return { ...this.currentState };
  }
}
