/**
 * @module spacecamp/SpaceCampLeaderboardPage
 * @description Standalone 1080p/4K Live Leaderboard & Winner's Circle Podium dashboard
 * (`https://jsmars.sounny.com/spacecamp/leaderboard/`).
 * Streams live scores & telemetry feed from Firebase Realtime Database (`sounny-com`),
 * defaults to English with 1-click EN/FR toggle, works both for live workshops and
 * evergreen solo play (`All-Time` vs `Live Today`), and features a celebratory
 * Winner's Circle Podium + Specialist Mission Awards view.
 */

import { SpaceCampFirebase } from './SpaceCampFirebase.js';
import { ensurePlayerIdentity, persistPlayerHandle } from './SpaceCampCookies.js';
import { scIcon } from './SpaceCampIcons.js';

function h(tag, attrs = {}, ...children) {
  const el = document.createElement(tag);
  for (const [k, v] of Object.entries(attrs || {})) {
    if (v === null || v === undefined || v === false) continue;
    if (k === 'class') el.className = v;
    else if (k === 'text') el.textContent = String(v);
    else if (k === 'disabled') el.disabled = Boolean(v);
    else if (k.startsWith('on') && typeof v === 'function') {
      el.addEventListener(k.slice(2).toLowerCase(), v);
    } else {
      el.setAttribute(k, String(v));
    }
  }
  for (const child of children.flat()) {
    if (child === null || child === undefined || child === false) continue;
    if (typeof child === 'string' || typeof child === 'number') {
      el.appendChild(document.createTextNode(String(child)));
    } else {
      el.appendChild(child);
    }
  }
  return el;
}

const COPY = {
  en: {
    kicker: 'JSMARS SPACE CAMP · LIVE FIREBASE TELEMETRY',
    title: 'Mission JSMARS: Space Data Analysts',
    subtitle: "Atelier numérique de planification de missions et d'analyse en télédétection",
    joinBanner: 'jsmars.sounny.com/spacecamp',
    playBtn: 'Mission Console',
    raceModeBtn: 'Live Race Standings',
    podiumModeBtn: 'Winner’s Circle Podium',
    filterAll: 'All-Time',
    filterToday: 'Live Today (24h)',
    preset5m: '5m Trail',
    preset25m: '25m (5×5m)',
    preset60m: '60m Event',
    timerStart: 'Start',
    timerPause: 'Pause',
    timerReset: 'Reset',
    analystsCount: 'Active Operators',
    feedHeader: 'LIVE MISSION TELEMETRY FEED',
    feedEmpty: 'Awaiting telemetry packets from Space Data Analysts...',
    boardEmpty: 'No scores logged for this filter yet. Open Mission Console and claim a callsign to take #1.',
    podiumHeader: 'WINNER’S CIRCLE · TOP SPACE DATA ANALYSTS',
    awardsHeader: 'SPECIALIST MISSION COMMENDATIONS',
    awardLunar: 'Best Artemis Lunar Engineer (R4)',
    awardRover: 'Best Mars Rover Planner (R3)',
    awardEarth: 'Top Earth Observation Analyst (R1)',
    awardEuropa: 'Outer Solar System Pioneer (R5)'
  },
  fr: {
    kicker: 'JSMARS SPACE CAMP · TÉLÉMÉTRIE FIREBASE EN DIRECT',
    title: 'Mission JSMARS: Space Data Analysts',
    subtitle: "Atelier numérique de planification de missions et d'analyse en télédétection",
    joinBanner: 'jsmars.sounny.com/spacecamp',
    playBtn: 'Console Mission',
    raceModeBtn: 'Classement en Direct',
    podiumModeBtn: 'Podium des Lauréats',
    filterAll: 'Tous les Scores',
    filterToday: 'Session du Jour (24h)',
    preset5m: '5m Manche',
    preset25m: '25m (5×5m)',
    preset60m: '60m Atelier',
    timerStart: 'Démarrer',
    timerPause: 'Pause',
    timerReset: 'Réinitialiser',
    analystsCount: 'Opérateurs Actifs',
    feedHeader: 'FLUX DE TÉLÉMÉTRIE EN DIRECT',
    feedEmpty: 'En attente des premières mesures des analystes spatiaux...',
    boardEmpty: 'Aucun score enregistré pour ce filtre. Ouvrez la Console Mission pour prendre la 1re place.',
    podiumHeader: 'PODIUM DES LAURÉATS · MISSION JSMARS',
    awardsHeader: 'PRIX SPÉCIAUX PAR MISSION',
    awardLunar: 'Meilleur Ingénieur Base Lunaire Artemis (R4)',
    awardRover: 'Meilleur Planificateur Rover Mars (R3)',
    awardEarth: 'Meilleur Analyste Observation Terre (R1)',
    awardEuropa: 'Pionnier du Monde Océan Europe (R5)'
  }
};

export class SpaceCampLeaderboardPage {
  constructor(rootEl) {
    this.root = rootEl;
    this.identity = ensurePlayerIdentity();
    this.lang = this.identity.lang || 'en';
    this.viewMode = new URLSearchParams(window.location.search).get('view') === 'podium' ? 'podium' : 'race';
    this.filterMode = 'all'; // 'all' | 'today'
    this.rows = [];
    this.feed = [];
    this.firebase = new SpaceCampFirebase();

    // Workshop countdown timer: defaults to 25:00 (5 trails × 5m) with 5m & 60m presets
    this.presetMin = 25;
    this.remainingSec = 25 * 60;
    this.timerRunning = false;
    this._timerHandle = null;
  }

  t(key) {
    return COPY[this.lang]?.[key] || COPY.en[key] || key;
  }

  init() {
    this.render();
    this.firebase.subscribeLeaderboard((rows) => {
      this.rows = rows;
      this.render();
    });
    this.firebase.subscribeFeed((events) => {
      this.feed = events;
      this.render();
    });
  }

  setLanguage(nextLang) {
    this.lang = nextLang === 'fr' ? 'fr' : 'en';
    persistPlayerHandle(this.identity.handle, this.lang);
    this.render();
  }

  setTimerPreset(minutes) {
    this.timerRunning = false;
    clearInterval(this._timerHandle);
    this._timerHandle = null;
    this.presetMin = minutes;
    this.remainingSec = minutes * 60;
    this.render();
  }

  toggleTimer() {
    if (this.timerRunning) {
      this.timerRunning = false;
      clearInterval(this._timerHandle);
      this._timerHandle = null;
    } else {
      if (this.remainingSec <= 0) {
        this.remainingSec = this.presetMin * 60;
      }
      this.timerRunning = true;
      this._timerHandle = setInterval(() => {
        if (this.remainingSec > 0) {
          this.remainingSec -= 1;
          const timerEl = document.getElementById('sc-proj-timer');
          if (timerEl) timerEl.textContent = this._formatClock(this.remainingSec);
        } else {
          this.timerRunning = false;
          clearInterval(this._timerHandle);
          this._timerHandle = null;
          if (this.presetMin >= 25) {
            this.viewMode = 'podium';
          }
          this.render();
        }
      }, 1000);
    }
    this.render();
  }

  resetTimer() {
    this.timerRunning = false;
    clearInterval(this._timerHandle);
    this._timerHandle = null;
    this.remainingSec = this.presetMin * 60;
    this.render();
  }

  _formatClock(sec) {
    const m = Math.floor(sec / 60);
    const s = sec % 60;
    return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`;
  }

  _filteredRows() {
    if (this.filterMode === 'today') {
      const cutoff = Date.now() - 24 * 60 * 60 * 1000;
      return this.rows.filter((r) => (r.updatedAt || 0) >= cutoff);
    }
    return this.rows;
  }

  _renderRoundBars(rs = {}) {
    const defs = [
      ['R1', rs.r1 || 0, 60],
      ['R2', rs.r2 || 0, 90],
      ['R3', rs.r3 || 0, 120],
      ['R4', rs.r4 || 0, 150],
      ['R5', rs.r5 || 0, 180]
    ];
    return h('div', { class: 'sc-round-bars' },
      ...defs.map(([lbl, val, max]) => {
        const pct = Math.max(0, Math.min(100, Math.round((val / max) * 100)));
        return h('div', { class: 'sc-rbar' },
          h('div', { class: 'sc-rbar-track' },
            h('div', { class: 'sc-rbar-fill', style: `width:${pct}%` })
          ),
          h('span', { class: 'sc-rbar-lbl', text: `${lbl} ${val}/${max}` })
        );
      })
    );
  }

  _findSpecialistWinner(rows, roundKey) {
    let best = null;
    let bestVal = 0;
    for (const r of rows) {
      const v = Number(r.roundScores?.[roundKey]) || 0;
      if (v > bestVal) {
        bestVal = v;
        best = r;
      }
    }
    return best ? { handle: best.handle, pts: bestVal } : null;
  }

  _renderPodiumView(rows) {
    const medals = [
      { idx: 0, badge: '01 // 1ST PLACE · GOLD COMMANDER', cls: 'rank-1' },
      { idx: 1, badge: '02 // 2ND PLACE · SILVER NAVIGATOR', cls: 'rank-2' },
      { idx: 2, badge: '03 // 3RD PLACE · BRONZE ANALYST', cls: 'rank-3' }
    ];

    const podiumGrid = h('div', { class: 'sc-podium-grid', style: 'margin-top: 8px;' },
      ...medals.map((m) => {
        const p = rows[m.idx];
        if (!p) {
          return h('div', { class: `sc-podium-card ${m.cls}`, style: 'padding: 22px 18px; gap: 10px; min-height: 180px; justify-content:flex-start;' },
            h('div', { class: 'sc-kicker', style: 'font-size:11px;', text: m.badge }),
            h('div', { style: 'color:var(--sc-subtle); font-family:var(--sc-mono); font-size:13px; margin-top:6px;', text: 'AWAITING TELEMETRY' })
          );
        }
        return h('div', { class: `sc-podium-card ${m.cls}`, style: 'padding: 22px 18px; gap: 10px;' },
          h('div', { class: 'sc-kicker', style: 'font-size:11px;', text: m.badge }),
          h('div', { style: 'font-family: var(--sc-mono); font-size: 24px; font-weight: 700; color: #f8fafc;', text: `@${p.handle}` }),
          h('div', { style: 'font-family: var(--sc-mono); font-variant-numeric: tabular-nums; font-size: 28px; font-weight: 700; color: var(--sc-emerald-bright);', text: `${p.score} / 600 PTS` }),
          h('div', { style: 'font-family: var(--sc-mono); font-size: 11.5px; color: var(--sc-subtle-light);', text: `${p.completedRounds || 0} / 5 Trails Verified` }),
          this._renderRoundBars(p.roundScores || {})
        );
      })
    );

    const awards = [
      { label: this.t('awardLunar'), max: 150, winner: this._findSpecialistWinner(rows, 'r4') },
      { label: this.t('awardRover'), max: 120, winner: this._findSpecialistWinner(rows, 'r3') },
      { label: this.t('awardEarth'), max: 60, winner: this._findSpecialistWinner(rows, 'r1') },
      { label: this.t('awardEuropa'), max: 180, winner: this._findSpecialistWinner(rows, 'r5') }
    ];

    const awardsGrid = h('div', {
      style: 'display:grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); gap: 12px; margin-top: 8px;'
    },
      ...awards.map((a) =>
        h('div', { class: 'sc-step-card', style: 'padding: 14px;' },
          h('div', { class: 'sc-kicker', text: a.label }),
          a.winner
            ? h('div', { style: 'font-family: var(--sc-mono); font-variant-numeric: tabular-nums; font-size: 17px; font-weight:700; color:#f8fafc; margin-top:4px;', text: `@${a.winner.handle} · ${a.winner.pts}/${a.max} PTS` })
            : h('div', { style: 'font-family: var(--sc-mono); font-size: 12px; color:var(--sc-subtle); margin-top:4px;', text: 'PENDING' })
        )
      )
    );

    return h('div', { style: 'display:flex; flex-direction:column; gap:18px;' },
      h('div', { class: 'sc-kicker', style: 'font-size:12px;', text: this.t('podiumHeader') }),
      podiumGrid,
      h('div', { class: 'sc-kicker', style: 'font-size:12px; margin-top:8px;', text: this.t('awardsHeader') }),
      awardsGrid
    );
  }

  _renderRaceView(rows) {
    const tableHead = rows.length > 0
      ? h('div', { class: 'sc-lb-table-head', style: 'padding: 4px 14px;' },
          h('span', { text: 'RANK' }),
          h('span', { text: this.lang === 'fr' ? 'OPÉRATEUR' : 'OPERATOR' }),
          h('span', { text: this.lang === 'fr' ? 'SECTEUR' : 'SECTOR' }),
          h('span', { text: this.lang === 'fr' ? 'TÉLÉMÉTRIE PARCOURS (R1–R5)' : 'TRAIL TELEMETRY (R1–R5)' }),
          h('span', { style: 'text-align:right;', text: 'TOTAL' })
        )
      : null;

    const leftBoard = h('div', { style: 'display:flex; flex-direction:column; gap:6px;' },
      tableHead,
      ...(rows.length > 0
        ? rows.map((r, idx) =>
            h('div', { class: `sc-lb-row ${r.playerId === this.identity.playerId ? 'is-self' : ''}`, style: 'padding: 10px 14px;' },
              h('strong', { style: 'font-family: var(--sc-mono); font-variant-numeric: tabular-nums; color:var(--sc-subtle-light); font-size:15px;', text: `#${String(idx + 1).padStart(2, '0')}` }),
              h('div', {},
                h('div', { style: 'font-weight:700; color:#f8fafc; font-family: var(--sc-mono); font-size:15px;', text: `@${r.handle}` }),
                h('div', { style: 'font-family: var(--sc-mono); font-size:10.5px; color:var(--sc-subtle);', text: `T${r.currentRound || 1}/5 · ${r.completedRounds || 0} verified` })
              ),
              h('span', { class: 'sc-body-tag', style: 'font-family: var(--sc-mono); font-size:11px; font-weight:700; color:var(--sc-cyan); text-transform:uppercase;', text: r.currentBody || 'earth' }),
              this._renderRoundBars(r.roundScores || {}),
              h('strong', { style: 'font-family: var(--sc-mono); font-variant-numeric: tabular-nums; color:var(--sc-emerald-bright); font-size:16px; text-align:right;', text: `${r.score} PTS` })
            )
          )
        : [h('div', { class: 'sc-feedback', style: 'font-size:13.5px; padding:16px;', text: this.t('boardEmpty') })])
    );

    const feedPanel = h('aside', {
      style: 'background: var(--sc-bg-card); border: 1px solid var(--sc-border); border-radius: 6px; padding: 14px; display:flex; flex-direction:column; gap:8px; height: fit-content;'
    },
      h('div', { class: 'sc-kicker', text: this.t('feedHeader') }),
      ...(this.feed.length > 0
        ? this.feed.slice(0, 14).map((ev) =>
            h('div', {
              style: 'padding: 7px 10px; border-radius: 4px; background: var(--sc-bg-recessed); border: 1px solid var(--sc-border-subtle); font-size: 12px;'
            },
              h('div', { style: 'display:flex; justify-content:space-between; font-family: var(--sc-mono); font-weight:700;' },
                h('span', { style: 'color:var(--sc-cyan);', text: `@${ev.handle}` }),
                h('span', { style: 'color:var(--sc-emerald-bright);', text: `+${ev.points} PTS` })
              ),
              h('div', { style: 'color:var(--sc-muted); margin-top:2px; font-size:11.5px;', text: `R${ev.roundNum || 1} · ${this.lang === 'fr' ? (ev.labelFr || ev.labelEn) : (ev.labelEn || ev.labelFr)}` })
            )
          )
        : [h('div', { style: 'font-size:12px; color:var(--sc-subtle-light);', text: this.t('feedEmpty') })])
    );

    return h('div', { class: 'sc-proj-grid' }, leftBoard, feedPanel);
  }

  render() {
    const rows = this._filteredRows();
    const gameHref = new URL('../', window.location.href).href;

    const header = h('header', {
      style: 'display:flex; flex-wrap:wrap; justify-content:space-between; align-items:center; gap:14px; padding-bottom: 16px; border-bottom: 1px solid var(--sc-border);'
    },
      h('div', {},
        h('div', { class: 'sc-brand-status', style: 'margin-bottom:4px;' },
          h('span', { class: 'sc-status-led', 'aria-hidden': 'true' }),
          h('span', { class: 'sc-kicker', text: this.t('kicker') })
        ),
        h('h1', { style: 'margin: 2px 0; font-family: var(--sc-display); font-size: 25px; letter-spacing: -0.01em; color:#f8fafc;', text: this.t('title') }),
        h('div', { style: 'font-size: 13px; color: var(--sc-subtle-light);', text: this.t('subtitle') })
      ),
      h('div', { style: 'display:flex; flex-wrap:wrap; align-items:center; gap:8px;' },
        h('span', {
          style: 'display:inline-flex; align-items:center; gap:6px; padding:5px 10px; border-radius:4px; background:var(--sc-bg-recessed); border:1px solid var(--sc-border); font-family:var(--sc-mono); font-size:11.5px; color:var(--sc-muted);'
        }, scIcon('globe', 12), this.t('joinBanner')),
        h('span', {
          style: 'display:inline-flex; align-items:center; gap:6px; padding:5px 10px; border-radius:4px; background:var(--sc-bg-recessed); border:1px solid var(--sc-border); font-family:var(--sc-mono); font-size:11.5px; font-weight:700; color:var(--sc-emerald-bright);',
          text: `${rows.length} ${this.t('analystsCount')}`
        }),
        h('a', { href: gameHref, class: 'sc-btn sc-btn-primary' }, scIcon('chevronLeft', 13), this.t('playBtn')),
        h('div', { class: 'sc-lang-toggle' },
          h('button', {
            type: 'button',
            class: `sc-lang-opt ${this.lang === 'en' ? 'is-active' : ''}`,
            text: 'EN',
            onclick: () => this.setLanguage('en')
          }),
          h('button', {
            type: 'button',
            class: `sc-lang-opt ${this.lang === 'fr' ? 'is-active' : ''}`,
            text: 'FR',
            onclick: () => this.setLanguage('fr')
          })
        )
      )
    );

    const toolbar = h('div', {
      style: 'display:flex; flex-wrap:wrap; justify-content:space-between; align-items:center; gap:10px; margin: 16px 0;'
    },
      h('div', { class: 'sc-btn-row' },
        h('button', {
          type: 'button',
          id: 'sc-proj-race-btn',
          class: `sc-btn ${this.viewMode === 'race' ? 'sc-btn-primary' : ''}`,
          onclick: () => {
            this.viewMode = 'race';
            this.render();
          }
        }, scIcon('leaderboard', 13), this.t('raceModeBtn')),
        h('button', {
          type: 'button',
          id: 'sc-proj-podium-btn',
          class: `sc-btn ${this.viewMode === 'podium' ? 'sc-btn-amber' : ''}`,
          onclick: () => {
            this.viewMode = 'podium';
            this.render();
          }
        }, scIcon('podium', 13), this.t('podiumModeBtn')),
        h('button', {
          type: 'button',
          class: `sc-btn ${this.filterMode === 'all' ? 'sc-btn-primary' : ''}`,
          text: this.t('filterAll'),
          onclick: () => {
            this.filterMode = 'all';
            this.render();
          }
        }),
        h('button', {
          type: 'button',
          class: `sc-btn ${this.filterMode === 'today' ? 'sc-btn-primary' : ''}`,
          text: this.t('filterToday'),
          onclick: () => {
            this.filterMode = 'today';
            this.render();
          }
        })
      ),
      h('div', { class: 'sc-btn-row', style: 'align-items:center;' },
        h('button', {
          type: 'button',
          class: `sc-btn ${this.presetMin === 5 ? 'sc-btn-primary' : ''}`,
          text: this.t('preset5m'),
          onclick: () => this.setTimerPreset(5)
        }),
        h('button', {
          type: 'button',
          class: `sc-btn ${this.presetMin === 25 ? 'sc-btn-primary' : ''}`,
          text: this.t('preset25m'),
          onclick: () => this.setTimerPreset(25)
        }),
        h('button', {
          type: 'button',
          class: `sc-btn ${this.presetMin === 60 ? 'sc-btn-primary' : ''}`,
          text: this.t('preset60m'),
          onclick: () => this.setTimerPreset(60)
        }),
        h('span', {
          id: 'sc-proj-timer',
          style: 'font-family: var(--sc-mono); font-variant-numeric: tabular-nums; font-size: 18px; font-weight:700; color:var(--sc-cyan); padding: 4px 11px; border-radius:4px; background:var(--sc-bg-recessed); border:1px solid var(--sc-border-strong);',
          text: this._formatClock(this.remainingSec)
        }),
        h('button', {
          type: 'button',
          class: 'sc-btn sc-btn-emerald',
          onclick: () => this.toggleTimer()
        }, scIcon(this.timerRunning ? 'pause' : 'play', 12), this.timerRunning ? this.t('timerPause') : this.t('timerStart')),
        h('button', {
          type: 'button',
          class: 'sc-btn',
          onclick: () => this.resetTimer()
        }, scIcon('reset', 12), this.t('timerReset'))
      )
    );

    const bodyView = this.viewMode === 'podium'
      ? this._renderPodiumView(rows)
      : this._renderRaceView(rows);

    this.root.replaceChildren(header, toolbar, bodyView);
  }
}
