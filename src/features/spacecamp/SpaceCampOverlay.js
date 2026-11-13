/**
 * @module spacecamp/SpaceCampOverlay
 * @description Docked wide-screen Right Side Panel for "Mission JSMARS: Space Data Analysts".
 * Preserves 100% of core JSMARS scientific tools while guiding students through 5 progressive
 * Earth Observation & Planetary GIS trails synced live to Firebase Realtime Database, with
 * an integrated Live Leaderboard & Podium view and optional Gemini 3.8 Flash AI CapCom.
 */

import { EVENTS } from '../../constants.js';
import { jmarsState } from '../../jmars-state.js';
import { switchActiveBody } from '../../util/body.js';
import { molaDem } from '../../util/mola-dem.js';
import {
  ensurePlayerIdentity,
  persistPlayerHandle,
  resetPlayerIdentity,
  validateHandle
} from './SpaceCampCookies.js';
import { SpaceCampFirebase } from './SpaceCampFirebase.js';
import {
  SpaceCampEngine,
  interpolateWaypoints,
  scoreLunarLandingSite
} from './SpaceCampEngine.js';
import { SpaceCampCopilot } from './SpaceCampCopilot.js';
import { scIcon } from './SpaceCampIcons.js';

function h(tag, attrs = {}, ...children) {
  const el = document.createElement(tag);
  for (const [k, v] of Object.entries(attrs || {})) {
    if (v === null || v === undefined || v === false) continue;
    if (k === 'class') el.className = v;
    else if (k === 'text') el.textContent = String(v);
    else if (k === 'disabled') el.disabled = Boolean(v);
    else if (k === 'checked') el.checked = Boolean(v);
    else if (k === 'hidden') el.hidden = Boolean(v);
    else if (k === 'value') el.value = String(v);
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

export class SpaceCampOverlay {
  /**
   * @param {object} app - Returned handles from initJSMARSApp()
   */
  constructor(app) {
    this.app = app;
    this.jmars = app.jmars;
    this.map = app.jmars.map;
    this.identity = ensurePlayerIdentity();
    this.lang = this.identity.lang || 'en';
    this.firebase = new SpaceCampFirebase();
    this.copilot = new SpaceCampCopilot();
    this.deck = null;
    this.engine = null;

    this.drawerMode = 'missions'; // 'missions' | 'ai'
    this.drawerCollapsed = false;
    this.activeModal = null; // null | 'briefing' | 'leaderboard'
    this.lbFilter = 'all'; // 'all' | 'today'
    this.slideIndex = 0;

    this.leaderboardRows = [];
    this.feedEvents = [];
    this.aiMessages = [];
    this.aiKeyPanelOpen = false;
    this.aiBusy = false;

    this.r1Feedback = {};
    this.scanFeedback = {};

    // Round 1 Earth Observation orbital pins
    this.eoGroup = L.layerGroup();

    // Round 3 Mars Rover map layer & drawing state
    this.roverGroup = L.layerGroup();
    this.roverDrawing = false;
    this.roverWaypoints = [];
    this.lastSampledElevations = [];
    this._onMapClickRover = this._onMapClickRover.bind(this);

    // Round 4 latest inspected lunar site metrics
    this.lastInspectedLunar = null;
    this.initialized = false;
  }

  async init() {
    const deckUrl = new URL('../../data/spacecamp-missions.json', import.meta.url);
    const resp = await fetch(deckUrl);
    this.deck = await resp.json();
    this.engine = new SpaceCampEngine(this.deck);

    this._setupSpoilerFilter();
    this._buildReticle();
    this._buildSidePanel();
    this._buildModal();
    this._attachJSMARSEvents();
    this._applyPanelLayout();

    // Subscribe to live Firebase leaderboard & discovery feed
    this.unsubLb = this.firebase.subscribeLeaderboard((rows) => {
      this.leaderboardRows = rows;
      this._refreshHeader();
      if (this.activeModal === 'leaderboard') {
        this._renderLeaderboardModalContent();
      }
    });

    this.unsubFeed = this.firebase.subscribeFeed((events) => {
      this.feedEvents = events;
      if (this.activeModal === 'leaderboard') {
        this._renderLeaderboardModalContent();
      }
    });

    // Periodic check for automatic 25-minute Round 5 unlock (solo/event independent!)
    this._unlockTimer = setInterval(() => {
      const wasUnlocked = this.engine.state.mysteryForceUnlocked;
      if (!wasUnlocked && this.engine.isRound5Unlocked()) {
        this.engine.save(this.identity);
        this._showToast(this.t('mysteryUnlockedToast'));
        this._renderRoundTabs();
        this._renderDrawerBody();
      }
    }, 8000);

    // Initial body setup for Round 1 if starting fresh without URL query override
    const hasBodyParam = new URLSearchParams(window.location.search).has('body');
    if (!hasBodyParam && this.engine.getRoundScores().total === 0 && this.engine.state.activeRound === 1) {
      await this._flyTo('earth', 32.2, -6.5, 6);
    } else if ((this.jmars.currentBody || '').toLowerCase() === 'earth') {
      this._ensureSentinel2Layer();
    }

    // Re-sync existing cookie handle silently if present (no forced blocking modal!)
    if (this.identity.handle && validateHandle(this.identity.handle).valid) {
      this.firebase.claimHandle(
        this.identity.handle,
        this.identity,
        this.engine.buildFirebaseSummary(this.jmars.currentBody)
      );
    }

    this._syncMapOverlays();
    this._refreshAll();
    this.initialized = true;
  }

  t(key) {
    return this.deck?.ui?.[this.lang]?.[key] || this.deck?.ui?.en?.[key] || key;
  }

  setLanguage(nextLang) {
    this.lang = nextLang === 'fr' ? 'fr' : 'en';
    this.identity.lang = this.lang;
    persistPlayerHandle(this.identity.handle, this.lang);
    this.engine.save(this.identity);
    this._refreshAll();
    if (this.activeModal === 'briefing') {
      this._renderBriefingModalContent();
    } else if (this.activeModal === 'leaderboard') {
      this._renderLeaderboardModalContent();
    }
    this._renderLunarPlannerScorer();
  }

  _applyPanelLayout() {
    document.body.classList.toggle('sc-panel-open', !this.drawerCollapsed);
    setTimeout(() => {
      try {
        this.map?.invalidateSize({ pan: false });
      } catch {
        // Ignore during teardown
      }
    }, 260);
  }

  togglePanelCollapse(forceState) {
    this.drawerCollapsed = typeof forceState === 'boolean' ? forceState : !this.drawerCollapsed;
    this._applyPanelLayout();
    this._refreshAll();
  }

  _setupSpoilerFilter() {
    if (!this.app.searchBar?.setSpoilerFilter) return;
    this.app.searchBar.setSpoilerFilter((lm) => {
      const hidden = this.engine.getSpoilerHiddenSet();
      const name = String(lm?.name || '').trim().toLowerCase();
      return !hidden.has(name);
    });
  }

  _ensureSentinel2Layer() {
    const layers = jmarsState.get('activeLayers') || [];
    if (!layers.some((l) => l.id === 'earth_s2_cloudless')) {
      const next = [
        ...layers,
        { id: 'earth_s2_cloudless', opacity: 1.0, visible: true }
      ];
      jmarsState.setActiveLayers(next);
    }
  }

  async _flyTo(body, lat, lon, zoom = 6) {
    const targetBody = String(body || 'mars').toLowerCase();
    if ((this.jmars.currentBody || '').toLowerCase() !== targetBody) {
      await switchActiveBody(this.jmars, targetBody);
    }
    if (targetBody === 'earth') {
      this._ensureSentinel2Layer();
    }
    if (this.app.viewModeToggle?.mode === '3d') {
      await this.app.viewModeToggle.setMode('2d');
    }
    this.map.setView([lat, lon], zoom, { animate: true });
  }

  // ── UI Construction: Docked Wide-Screen Right Side Panel ─────────────────
  _buildReticle() {
    this.reticleEl = h('div', { class: 'sc-reticle', 'aria-hidden': 'true' });
    const svgNS = 'http://www.w3.org/2000/svg';
    const svg = document.createElementNS(svgNS, 'svg');
    svg.setAttribute('viewBox', '0 0 108 108');
    svg.setAttribute('width', '108');
    svg.setAttribute('height', '108');

    const ring = document.createElementNS(svgNS, 'circle');
    ring.setAttribute('cx', '54');
    ring.setAttribute('cy', '54');
    ring.setAttribute('r', '34');
    ring.setAttribute('fill', 'none');
    ring.setAttribute('stroke', 'rgba(56, 189, 248, 0.72)');
    ring.setAttribute('stroke-width', '1.1');

    const brackets = [
      'M 16 28 V 16 H 28',
      'M 80 16 H 92 V 28',
      'M 92 80 V 92 H 80',
      'M 28 92 H 16 V 80'
    ];
    brackets.forEach((d) => {
      const path = document.createElementNS(svgNS, 'path');
      path.setAttribute('d', d);
      path.setAttribute('fill', 'none');
      path.setAttribute('stroke', 'rgba(241, 245, 249, 0.72)');
      path.setAttribute('stroke-width', '1.3');
      svg.appendChild(path);
    });

    const dot = document.createElementNS(svgNS, 'circle');
    dot.setAttribute('cx', '54');
    dot.setAttribute('cy', '54');
    dot.setAttribute('r', '2');
    dot.setAttribute('fill', '#38bdf8');

    const lines = [
      [54, 10, 54, 24],
      [54, 84, 54, 98],
      [10, 54, 24, 54],
      [84, 54, 98, 54]
    ];
    lines.forEach(([x1, y1, x2, y2]) => {
      const ln = document.createElementNS(svgNS, 'line');
      ln.setAttribute('x1', String(x1));
      ln.setAttribute('y1', String(y1));
      ln.setAttribute('x2', String(x2));
      ln.setAttribute('y2', String(y2));
      ln.setAttribute('stroke', '#38bdf8');
      ln.setAttribute('stroke-width', '1.4');
      svg.appendChild(ln);
    });

    svg.appendChild(ring);
    svg.appendChild(dot);
    this.reticleEl.appendChild(svg);

    // Mount inside #map so 50%/50% always matches map.getCenter() regardless of side panel width!
    const mapContainer = this.map?.getContainer?.() || document.getElementById('map') || document.body;
    mapContainer.appendChild(this.reticleEl);
  }

  _buildSidePanel() {
    // Collapse / Expand edge tab
    this.edgeToggleBtn = h('button', {
      type: 'button',
      id: 'sc-edge-toggle-btn',
      class: 'sc-edge-toggle',
      title: 'Collapse or expand Space Camp panel',
      'aria-label': 'Toggle Space Camp side panel',
      onclick: () => this.togglePanelCollapse()
    });

    // Header elements
    this.kickerEl = h('span', { class: 'sc-brand-kicker' });
    this.briefingBtn = h('button', {
      type: 'button',
      id: 'sc-open-briefing-btn',
      class: 'sc-mini-btn',
      onclick: () => this.openBriefingModal()
    });

    this.langEnBtn = h('button', {
      type: 'button',
      id: 'sc-lang-en-btn',
      class: 'sc-lang-opt',
      text: 'EN',
      onclick: () => this.setLanguage('en')
    });
    this.langFrBtn = h('button', {
      type: 'button',
      id: 'sc-lang-fr-btn',
      class: 'sc-lang-opt',
      text: 'FR',
      onclick: () => this.setLanguage('fr')
    });
    const langGroup = h('div', { class: 'sc-lang-toggle', role: 'group', 'aria-label': 'Language' },
      this.langEnBtn,
      this.langFrBtn
    );

    this.mainTitleEl = h('h1', { class: 'sc-main-title' });
    this.mainSubtitleEl = h('p', { class: 'sc-main-subtitle' });

    this.playerStripEl = h('div', { class: 'sc-player-strip' });

    // 3-Segment Console Mode & Leaderboard Bar
    this.missionsBtn = h('button', {
      type: 'button',
      id: 'sc-mode-missions-btn',
      class: 'sc-mode-btn is-active',
      onclick: () => {
        this.drawerMode = 'missions';
        this._refreshAll();
      }
    });

    this.aiBtn = h('button', {
      type: 'button',
      id: 'sc-mode-ai-btn',
      class: 'sc-mode-btn',
      onclick: () => {
        this.drawerMode = 'ai';
        this._refreshAll();
      }
    });

    this.leaderboardBtn = h('button', {
      type: 'button',
      id: 'sc-open-leaderboard-btn',
      class: 'sc-mode-btn is-leaderboard',
      onclick: () => this.openLeaderboardModal()
    });

    const modeBar = h('div', { class: 'sc-mode-bar' },
      this.missionsBtn,
      this.aiBtn,
      this.leaderboardBtn
    );

    const headerEl = h('header', { class: 'sc-panel-header' },
      h('div', { class: 'sc-header-top-row' },
        h('div', { class: 'sc-brand-status' },
          h('span', { class: 'sc-status-led', 'aria-hidden': 'true' }),
          this.kickerEl
        ),
        h('div', { class: 'sc-header-actions' }, this.briefingBtn, langGroup)
      ),
      h('div', { class: 'sc-title-block' }, this.mainTitleEl, this.mainSubtitleEl),
      this.playerStripEl,
      modeBar
    );

    // Inline Callsign Join Card (shown cleanly inside panel when handle is empty)
    this.inlineJoinWrap = h('div', {});

    // 5-Round Progressive Tab Strip
    this.roundTabsEl = h('div', { class: 'sc-round-tabs', role: 'tablist' });

    // Scrollable Drawer Body
    this.drawerBodyEl = h('div', { class: 'sc-drawer-body' });

    this.panelEl = h('aside', {
      id: 'sc-side-panel',
      class: 'sc-side-panel',
      'aria-label': 'Mission JSMARS Space Camp Side Panel'
    },
      headerEl,
      this.inlineJoinWrap,
      this.roundTabsEl,
      this.drawerBodyEl
    );

    document.body.appendChild(this.edgeToggleBtn);
    document.body.appendChild(this.panelEl);
  }

  _buildModal() {
    this.modalBackdrop = h('div', {
      class: 'sc-modal-backdrop',
      role: 'dialog',
      'aria-modal': 'true',
      hidden: true
    });
    this.modalEl = h('div', { class: 'sc-modal' });
    this.modalBackdrop.appendChild(this.modalEl);
    this.modalBackdrop.addEventListener('click', (e) => {
      if (e.target === this.modalBackdrop) {
        this.closeModal();
      }
    });
    document.addEventListener('keydown', (e) => {
      if (e.key === 'Escape' && !this.modalBackdrop.hidden) {
        this.closeModal();
      }
    });
    document.body.appendChild(this.modalBackdrop);
  }

  closeModal() {
    this.activeModal = null;
    this.modalBackdrop.hidden = true;
    this.modalEl.classList.remove('is-wide', 'is-briefing');
  }

  openBriefingModal() {
    this.activeModal = 'briefing';
    this.modalEl.classList.remove('is-wide');
    this.modalEl.classList.add('is-briefing');
    this.modalBackdrop.hidden = false;
    this._renderBriefingModalContent();
  }

  openLeaderboardModal() {
    this.activeModal = 'leaderboard';
    this.modalEl.classList.remove('is-briefing');
    this.modalEl.classList.add('is-wide');
    this.modalBackdrop.hidden = false;
    this._renderLeaderboardModalContent();
  }

  // ── Inline Callsign Claiming & Player Switching ──────────────────────────
  _buildCallsignClaimUI(compact = true) {
    const handleInput = h('input', {
      type: 'text',
      id: compact ? 'sc-handle-input' : 'sc-modal-handle-input',
      class: 'sc-handle-input',
      maxlength: '20',
      autocomplete: 'off',
      placeholder: this.t('handlePlaceholder'),
      value: this.identity.handle || ''
    });

    const statusMsg = h('div', {
      id: compact ? 'sc-handle-status' : 'sc-modal-handle-status',
      style: 'font-size: 11px; color: #cbd5e1;'
    }, this.t('handleRules'));

    const suggestionWrap = h('div', {
      class: 'sc-suggestions',
      id: compact ? 'sc-handle-suggestions' : 'sc-modal-handle-suggestions'
    });

    const claimBtn = h('button', {
      type: 'button',
      id: compact ? 'sc-handle-claim-btn' : 'sc-modal-handle-claim-btn',
      class: 'sc-btn sc-btn-emerald',
      text: this.t('handleClaimBtn')
    });

    const submitClaim = async (desired) => {
      const candidate = String(desired ?? handleInput.value ?? '').trim();
      const check = validateHandle(candidate);
      suggestionWrap.replaceChildren();
      if (!check.valid) {
        statusMsg.style.color = '#fbbf24';
        statusMsg.textContent = this.t('handleInvalid');
        return;
      }

      claimBtn.disabled = true;
      statusMsg.style.color = '#38bdf8';
      statusMsg.textContent = this.t('handleChecking');

      const res = await this.firebase.claimHandle(
        check.handle,
        this.identity,
        this.engine.buildFirebaseSummary(this.jmars.currentBody)
      );
      claimBtn.disabled = false;

      if (res.ok) {
        this.identity.handle = res.handle;
        persistPlayerHandle(res.handle, this.lang);
        this.engine.save(this.identity);
        if (this.activeModal === 'briefing') this.closeModal();
        this._refreshAll();
        this._showToast(
          this.lang === 'fr'
            ? `@${res.handle} connecté au classement en direct !`
            : `@${res.handle} connected to live leaderboard!`
        );
      } else if (res.taken) {
        statusMsg.style.color = '#fbbf24';
        statusMsg.textContent = this.t('handleTaken');
        (res.suggestions || []).forEach((sug) => {
          suggestionWrap.appendChild(
            h('button', {
              type: 'button',
              class: 'sc-suggestion-chip',
              text: `@${sug}`,
              onclick: () => {
                handleInput.value = sug;
                submitClaim(sug);
              }
            })
          );
        });
      } else {
        // Offline / restricted network fallback
        this.identity.handle = check.handle;
        persistPlayerHandle(check.handle, this.lang);
        this.engine.save(this.identity);
        this._refreshAll();
      }
    };

    claimBtn.addEventListener('click', () => submitClaim());
    handleInput.addEventListener('keydown', (e) => {
      if (e.key === 'Enter') {
        e.preventDefault();
        submitClaim();
      }
    });

    return h('div', { class: 'sc-inline-join', id: compact ? 'sc-inline-join-card' : 'sc-modal-join-card' },
      h('div', { class: 'sc-inline-join-header' },
        h('span', { text: this.t('handleTitle') }),
        h('span', { style: 'color: var(--sc-subtle-light);', text: 'LIVE SYNC' })
      ),
      h('div', { class: 'sc-handle-row' }, handleInput, claimBtn),
      statusMsg,
      suggestionWrap
    );
  }

  _switchPlayer() {
    this.identity = resetPlayerIdentity(this.lang);
    this.firebase = new SpaceCampFirebase();
    this.copilot.resetConversation();
    this.engine = new SpaceCampEngine(this.deck, {});
    this.roverWaypoints = [];
    this.lastSampledElevations = [];
    this.r1Feedback = {};
    this.scanFeedback = {};
    this.unsubLb?.();
    this.unsubLb = this.firebase.subscribeLeaderboard((rows) => {
      this.leaderboardRows = rows;
      this._refreshHeader();
      if (this.activeModal === 'leaderboard') {
        this._renderLeaderboardModalContent();
      }
    });
    this._setupSpoilerFilter();
    this._syncMapOverlays();
    this._refreshAll();
    this.initialized = true;
  }

  // ── Briefing Slides Modal (Optional, Instructor / Student Triggered) ─────
  _renderBriefingModalContent() {
    const cards = this.deck.introCards || [];
    const card = cards[this.slideIndex] || cards[0];
    const copy = card[this.lang] || card.en;
    const hasCoCreators = Array.isArray(card.coCreators) && card.coCreators.length > 0;
    const avatarUrl = (!hasCoCreators && card.avatar)
      ? new URL(`../../../${card.avatar}`, import.meta.url).href
      : null;
    const slideImageUrl = card.image
      ? new URL(`../../../${card.image}`, import.meta.url).href
      : null;
    const slideImageCaption = card.imageCaption
      ? (card.imageCaption[this.lang] || card.imageCaption.en || '')
      : '';

    const topRow = h('div', { class: 'sc-modal-top' },
      h('div', {},
        h('div', { class: 'sc-kicker', text: this.t('eventBadge') }),
        h('div', { style: 'font-family: var(--sc-display); font-size: 18px; font-weight: 700; margin-top: 2px;', text: 'Mission JSMARS: Space Data Analysts' }),
        h('div', { style: 'font-size: 12px; color: #94a3b8; margin-top: 1px;', text: "Atelier numérique de planification de missions et d'analyse en télédétection" })
      ),
      h('div', { class: 'sc-btn-row' },
        h('button', {
          type: 'button',
          class: 'sc-btn',
          text: this.lang === 'en' ? 'FRANÇAIS' : 'ENGLISH',
          onclick: () => this.setLanguage(this.lang === 'en' ? 'fr' : 'en')
        }),
        h('button', {
          type: 'button',
          class: 'sc-btn',
          'aria-label': 'Close slides',
          onclick: () => this.closeModal()
        }, scIcon('close', 13))
      )
    );

    const coCreatorNodes = hasCoCreators
      ? h('div', { class: 'sc-cocreators-grid' },
          ...card.coCreators.map((person) => {
            const pCopy = person[this.lang] || person.en || {};
            const pAvatar = person.avatar
              ? new URL(`../../../${person.avatar}`, import.meta.url).href
              : null;
            return h('div', { class: 'sc-cocreator-card' },
              h('div', { class: 'sc-cocreator-head' },
                pAvatar
                  ? h('img', {
                      class: 'sc-avatar sc-avatar-sm',
                      src: pAvatar,
                      alt: person.name,
                      style: person.avatarPosition ? `object-position: ${person.avatarPosition};` : undefined
                    })
                  : h('div', { class: 'sc-monogram-badge', 'aria-hidden': 'true', text: person.monogram || 'JB' }),
                h('div', { class: 'sc-cocreator-meta' },
                  h('div', { class: 'sc-cocreator-name', text: person.name }),
                  h('div', { class: 'sc-cocreator-role', text: pCopy.role || '' })
                )
              ),
              h('p', { class: 'sc-cocreator-bio', text: pCopy.bio || '' })
            );
          })
        )
      : null;

    const slideFigure = slideImageUrl
      ? h('figure', { class: 'sc-slide-figure' },
          h('img', {
            class: 'sc-slide-image',
            src: slideImageUrl,
            alt: slideImageCaption || copy.title
          }),
          slideImageCaption
            ? h('figcaption', { class: 'sc-slide-caption', text: slideImageCaption })
            : null
        )
      : null;

    const hero = h('div', { class: 'sc-card-hero' },
      avatarUrl ? h('img', { class: 'sc-avatar', src: avatarUrl, alt: copy.title }) : null,
      h('div', { style: 'flex:1; min-width:0;' },
        h('div', { class: 'sc-kicker', text: `${card.step} · ${copy.kicker}` }),
        h('h2', { style: 'margin: 4px 0 8px; font-family: var(--sc-display); font-size: 19px;', text: copy.title }),
        coCreatorNodes,
        slideFigure,
        h('p', { style: 'margin: 0; font-size: 13px; line-height: 1.55; color: #cbd5e1;', text: copy.body }),
        h('ul', { class: 'sc-highlights' },
          ...(copy.highlights || []).map((item) => h('li', { text: item }))
        ),
        card.action ? h('div', { style: 'margin-top: 10px;' },
          h('button', {
            type: 'button',
            class: 'sc-btn sc-btn-primary',
            onclick: async () => {
              await this._flyTo(card.action.body, card.action.lat, card.action.lon, card.action.zoom || 10);
              this.closeModal();
            }
          }, scIcon('locate', 12), card.action[this.lang] || card.action.en)
        ) : null
      )
    );

    const navRow = h('div', { class: 'sc-slide-nav' },
      h('button', {
        type: 'button',
        class: 'sc-btn',
        disabled: this.slideIndex === 0,
        onclick: () => {
          this.slideIndex = Math.max(0, this.slideIndex - 1);
          this._renderBriefingModalContent();
        }
      }, scIcon('chevronLeft', 12), this.lang === 'fr' ? 'Précédent' : 'Previous'),
      h('div', { class: 'sc-dots' },
        ...cards.map((_, idx) => h('button', {
          type: 'button',
          class: `sc-dot ${idx === this.slideIndex ? 'is-active' : ''}`,
          'aria-label': `Slide ${idx + 1}`,
          onclick: () => {
            this.slideIndex = idx;
            this._renderBriefingModalContent();
          }
        }))
      ),
      h('button', {
        type: 'button',
        class: 'sc-btn',
        disabled: this.slideIndex >= cards.length - 1,
        onclick: () => {
          this.slideIndex = Math.min(cards.length - 1, this.slideIndex + 1);
          this._renderBriefingModalContent();
        }
      }, this.lang === 'fr' ? 'Suivant' : 'Next', scIcon('chevronRight', 12))
    );

    this.modalEl.replaceChildren(topRow, hero, navRow);
  }

  // ── In-App Live Leaderboard & Winner's Circle Modal View ─────────────────
  _getFilteredLeaderboardRows() {
    if (this.lbFilter === 'today') {
      const cutoff = Date.now() - 24 * 60 * 60 * 1000;
      return this.leaderboardRows.filter((r) => (r.updatedAt || 0) >= cutoff);
    }
    return this.leaderboardRows;
  }

  _renderRoundBarsNode(roundScores = {}) {
    const maxes = [
      ['R1', roundScores.r1 || 0, 60],
      ['R2', roundScores.r2 || 0, 90],
      ['R3', roundScores.r3 || 0, 120],
      ['R4', roundScores.r4 || 0, 150],
      ['R5', roundScores.r5 || 0, 180]
    ];
    return h('div', { class: 'sc-round-bars' },
      ...maxes.map(([lbl, val, max]) => {
        const pct = Math.max(0, Math.min(100, Math.round((val / max) * 100)));
        return h('div', { class: 'sc-rbar', title: `${lbl}: ${val}/${max} pts` },
          h('div', { class: 'sc-rbar-track' },
            h('div', { class: 'sc-rbar-fill', style: `width:${pct}%` })
          ),
          h('span', { class: 'sc-rbar-lbl', text: `${lbl} ${val}` })
        );
      })
    );
  }

  _renderLeaderboardModalContent() {
    const rows = this._getFilteredLeaderboardRows();
    const leaderboardHref = new URL('./leaderboard/', window.location.href).href;

    const topRow = h('div', { class: 'sc-modal-top' },
      h('div', {},
        h('div', { class: 'sc-kicker', text: 'LIVE FIREBASE TELEMETRY · REAL-TIME SCOREBOARD' }),
        h('div', {
          style: 'font-family: var(--sc-display); font-size: 19px; font-weight: 700; margin-top: 2px;',
          text: this.lang === 'fr'
            ? 'Classement en Direct & Podium · Mission JSMARS'
            : 'Live Leaderboard & Winner’s Circle · Mission JSMARS'
        })
      ),
      h('div', { class: 'sc-btn-row' },
        h('button', {
          type: 'button',
          class: `sc-btn ${this.lbFilter === 'today' ? 'sc-btn-primary' : ''}`,
          text: this.lang === 'fr' ? 'Session (24h)' : 'Live Today (24h)',
          onclick: () => {
            this.lbFilter = 'today';
            this._renderLeaderboardModalContent();
          }
        }),
        h('button', {
          type: 'button',
          class: `sc-btn ${this.lbFilter === 'all' ? 'sc-btn-primary' : ''}`,
          text: this.lang === 'fr' ? 'Tous les Scores' : 'All-Time',
          onclick: () => {
            this.lbFilter = 'all';
            this._renderLeaderboardModalContent();
          }
        }),
        h('a', {
          href: leaderboardHref,
          target: '_blank',
          rel: 'noopener',
          class: 'sc-btn sc-btn-amber'
        }, this.t('projectorLink'), scIcon('external', 11)),
        h('button', {
          type: 'button',
          class: 'sc-btn',
          'aria-label': 'Close leaderboard',
          onclick: () => this.closeModal()
        }, scIcon('close', 13))
      )
    );

    const medals = ['01 // 1ST PLACE', '02 // 2ND PLACE', '03 // 3RD PLACE'];
    const podiumNodes = rows.length > 0
      ? h('div', { class: 'sc-podium-grid' },
          ...[0, 1, 2].map((idx) => {
            const p = rows[idx];
            if (!p) {
              return h('div', { class: `sc-podium-card rank-${idx + 1}` },
                h('div', { class: 'sc-kicker', text: medals[idx] }),
                h('div', { style: 'font-size:12.5px; color:var(--sc-subtle-light); margin-top:8px;', text: this.lang === 'fr' ? 'Place Ouverte' : 'Open Podium Slot' })
              );
            }
            return h('div', { class: `sc-podium-card rank-${idx + 1}` },
              h('div', { class: 'sc-kicker', text: medals[idx] }),
              h('div', { style: 'font-family: var(--sc-mono); font-size: 16.5px; font-weight: 700; color: #f8fafc;', text: `@${p.handle}` }),
              h('div', { style: 'font-family: var(--sc-mono); font-size: 20px; font-weight: 700; font-variant-numeric: tabular-nums; color: var(--sc-emerald-bright);', text: `${p.score} / 600 PTS` }),
              this._renderRoundBarsNode(p.roundScores || {})
            );
          })
        )
      : null;

    const tableHead = rows.length > 0
      ? h('div', { class: 'sc-lb-table-head' },
          h('span', { text: '#' }),
          h('span', { text: this.lang === 'fr' ? 'OPÉRATEUR' : 'OPERATOR' }),
          h('span', { text: this.lang === 'fr' ? 'SECTEUR' : 'SECTOR' }),
          h('span', { text: this.lang === 'fr' ? 'TÉLÉMÉTRIE PARCOURS' : 'TRAIL TELEMETRY' }),
          h('span', { style: 'text-align:right;', text: 'SCORE' })
        )
      : null;

    const listNodes = rows.length > 0
      ? h('div', { style: 'display:flex; flex-direction:column; gap:5px; max-height: 300px; overflow-y:auto;' },
          tableHead,
          ...rows.map((r, idx) =>
            h('div', { class: `sc-lb-row ${r.playerId === this.identity.playerId ? 'is-self' : ''}` },
              h('strong', { style: 'font-family: var(--sc-mono); font-variant-numeric: tabular-nums; color:var(--sc-subtle-light); font-size:12.5px;', text: `#${String(idx + 1).padStart(2, '0')}` }),
              h('div', {},
                h('div', { style: 'font-weight:700; color:#f8fafc; font-family: var(--sc-mono); font-size:12.5px;', text: `@${r.handle}` }),
                h('div', { style: 'font-size:10px; font-family: var(--sc-mono); color:var(--sc-subtle);', text: `${r.completedRounds || 0}/5 ${this.lang === 'fr' ? 'parcours' : 'trails'}` })
              ),
              h('span', { class: 'sc-body-tag', style: 'font-family: var(--sc-mono); font-size:10.5px; font-weight:600; color:var(--sc-cyan); text-transform:uppercase;', text: r.currentBody || 'earth' }),
              this._renderRoundBarsNode(r.roundScores || {}),
              h('strong', { style: 'font-family: var(--sc-mono); font-variant-numeric: tabular-nums; color:var(--sc-emerald-bright); font-size:13.5px; text-align:right;', text: `${r.score} PTS` })
            )
          )
        )
      : h('div', { class: 'sc-feedback', text: this.lang === 'fr'
          ? 'Aucun analyste enregistré pour ce filtre. Entrez votre pseudo dans le panneau de droite pour prendre la 1re place.'
          : 'No analysts on the board for this filter yet. Claim your callsign in the side panel to take #1.'
        });

    const recentFeed = this.feedEvents.slice(0, 5);
    const feedSection = recentFeed.length > 0
      ? h('div', { style: 'border-top: 1px solid var(--sc-border); padding-top: 10px;' },
          h('div', { class: 'sc-kicker', style: 'margin-bottom:6px;', text: this.lang === 'fr' ? 'FLUX DE TÉLÉMÉTRIE EN DIRECT' : 'LIVE MISSION TELEMETRY FEED' }),
          h('div', { style: 'display:flex; flex-wrap:wrap; gap:6px;' },
            ...recentFeed.map((ev) =>
              h('span', {
                style: 'padding:3px 8px; border-radius:4px; background:var(--sc-bg-recessed); border:1px solid var(--sc-border); font-family:var(--sc-mono); font-size:10.5px; color:#e2e8f0;',
                text: `@${ev.handle} · +${ev.points}p (${this.lang === 'fr' ? (ev.labelFr || ev.labelEn) : (ev.labelEn || ev.labelFr)})`
              })
            )
          )
        )
      : null;

    this.modalEl.replaceChildren(...[topRow, podiumNodes, listNodes, feedSection].filter(Boolean));
  }

  // ── JSMARS Scientific Event Listeners ────────────────────────────────────
  _attachJSMARSEvents() {
    document.addEventListener(EVENTS.BODY_CHANGED, (e) => {
      const body = (e?.detail?.body || this.jmars.currentBody || '').toLowerCase();
      if (body === 'earth') {
        this._ensureSentinel2Layer();
      } else if (body === 'mars') {
        molaDem.ensureLoaded().catch(() => {});
      }
      this._syncMapOverlays();
      this._syncToFirebase(null);
    });

    document.addEventListener(EVENTS.MEASURE_UPDATED, (e) => {
      const list = Array.isArray(e?.detail)
        ? e.detail
        : (Array.isArray(e?.detail?.measurements) ? e.detail.measurements : []);
      const body = (this.jmars.currentBody || '').toLowerCase();
      if (list.length > 0 && body === 'earth') {
        const pts = this.engine.awardRound1Measure();
        if (pts > 0) {
          this._onPointsEarned(1, pts, 'Earth Geodesic Measurement', 'Mesure Géodésique sur Terre');
        }
      }
    });

    document.addEventListener(EVENTS.PROFILE_GENERATED, (e) => {
      const profiles = e?.detail?.profiles;
      if (Array.isArray(profiles) && profiles.length === 0) return;
      const body = (this.jmars.currentBody || '').toLowerCase();
      if (body === 'mars') {
        const pts = this.engine.awardRound2Profile();
        if (pts > 0) {
          this._onPointsEarned(2, pts, 'MOLA Laser Elevation Profile', 'Profil Topographique Laser MOLA');
        }
      }
    });

    document.addEventListener(EVENTS.LANDING_SITES_TOGGLED, (e) => {
      const body = (this.jmars.currentBody || '').toLowerCase();
      if (e?.detail?.active && body === 'mars') {
        const pts = this.engine.awardRound3Lab();
        if (pts > 0) {
          this._onPointsEarned(3, pts, 'Mars Landing Dispersion Ellipses', 'Ellipses d’Atterrissage Martiennes');
        }
      }
    });

    document.addEventListener(EVENTS.CRATER_ADDED, () => {
      const body = (this.jmars.currentBody || '').toLowerCase();
      if (body === 'mars') {
        const pts = this.engine.awardRound3Lab();
        if (pts > 0) {
          this._onPointsEarned(3, pts, 'Mars Hazard Crater Logged', 'Cratère Martien Cartographié');
        }
      }
    });

    document.addEventListener('jmars:lunar-site-opened', () => {
      setTimeout(() => this._renderLunarPlannerScorer(), 80);
    });

    document.addEventListener('jmars:lunar-site-inspected', (e) => {
      if (e?.detail) {
        this.lastInspectedLunar = e.detail;
        this._renderLunarPlannerScorer();
        if (this.drawerMode === 'missions' && this.engine.state.activeRound === 4) {
          this._renderDrawerBody();
        }
      }
    });

    document.addEventListener(EVENTS.LAYERS_CHANGED, () => {
      if (!this.engine.isRound5Unlocked()) return;
      const body = (this.jmars.currentBody || '').toLowerCase();
      const active = jmarsState.get('activeLayers') || [];
      const hasConamara = active.some((l) => l.id === 'europa_conamara_chaos' && l.visible !== false);
      if (body === 'europa' && hasConamara) {
        const pts = this.engine.awardRound5Mosaic();
        if (pts > 0) {
          this._onPointsEarned(5, pts, 'Galileo 9m/px Mosaic Activated', 'Mosaïque Galileo 9m/px Activée');
        }
      }
    });

    document.addEventListener(EVENTS.SHAPE_CREATED, () => {
      if (!this.engine.isRound5Unlocked()) return;
      const body = (this.jmars.currentBody || '').toLowerCase();
      if (body === 'europa') {
        const pts = this.engine.awardRound5Cartography();
        if (pts > 0) {
          this._onPointsEarned(5, pts, 'Europa Ice Raft Mapped', 'Iceberg d’Europe Cartographié');
        }
      }
    });

    document.addEventListener(EVENTS.VIEW_MODE_CHANGED, (e) => {
      if (!this.engine.isRound5Unlocked()) return;
      const body = (this.jmars.currentBody || '').toLowerCase();
      if (e?.detail?.mode === '3d' && body === 'europa') {
        const pts = this.engine.awardRound5Cartography();
        if (pts > 0) {
          this._onPointsEarned(5, pts, 'Europa 3D Planetary Globe', 'Globe 3D d’Europe');
        }
      }
    });
  }

  _onPointsEarned(roundNum, deltaPts, labelEn, labelFr) {
    this._setupSpoilerFilter();
    const unlockedBefore = this.engine.state.mysteryForceUnlocked;
    const nowUnlocked = this.engine.isRound5Unlocked();
    this.engine.save(this.identity);
    this._syncToFirebase({
      kind: 'score',
      roundNum,
      points: deltaPts,
      labelEn,
      labelFr
    });

    const msg = this.lang === 'fr'
      ? `+${deltaPts} PTS · ${labelFr}`
      : `+${deltaPts} PTS · ${labelEn}`;
    this._showToast(msg);

    if (!unlockedBefore && nowUnlocked) {
      setTimeout(() => this._showToast(this.t('mysteryUnlockedToast')), 1800);
    }

    this._syncMapOverlays();
    this._refreshAll();
    this.initialized = true;
  }

  async _syncToFirebase(feedEvent = null) {
    if (!this.identity.handle) return;
    const summary = this.engine.buildFirebaseSummary(this.jmars.currentBody);
    await this.firebase.syncProgress(this.identity, summary, feedEvent);
  }

  _showToast(text) {
    if (this._activeToast) this._activeToast.remove();
    const toast = h('div', { class: 'sc-toast', role: 'status', text });
    document.body.appendChild(toast);
    this._activeToast = toast;
    setTimeout(() => {
      if (this._activeToast === toast) {
        toast.remove();
        this._activeToast = null;
      }
    }, 3400);
  }

  // ── Refresh Cycle ────────────────────────────────────────────────────────
  _refreshAll() {
    this._refreshHeader();
    this._renderInlineJoin();
    this._renderRoundTabs();
    this._renderDrawerBody();
    this._updateReticleVisibility();
    this._syncMapOverlays();
  }

  _refreshHeader() {
    const scores = this.engine.getRoundScores();
    this.kickerEl.textContent = this.t('eventBadge');
    this.briefingBtn.replaceChildren(scIcon('slides', 12), this.t('briefingBtn'));
    this.langEnBtn.classList.toggle('is-active', this.lang === 'en');
    this.langFrBtn.classList.toggle('is-active', this.lang === 'fr');

    this.mainTitleEl.textContent = this.t('title');
    this.mainSubtitleEl.textContent = this.t('subtitle');

    const myIdx = this.leaderboardRows.findIndex((r) => r.playerId === this.identity.playerId);
    const rankText = myIdx >= 0
      ? `#${myIdx + 1} / ${this.leaderboardRows.length}`
      : `T${this.engine.state.activeRound}/5`;

    const callsignNode = this.identity.handle
      ? h('span', { class: 'sc-callsign-badge' },
          h('span', { class: 'sc-callsign-text', text: `@${this.identity.handle}` }),
          h('button', {
            type: 'button',
            class: 'sc-switch-user-btn',
            title: this.lang === 'fr' ? 'Changer de joueur' : 'Switch player / New callsign',
            onclick: () => this._switchPlayer()
          }, scIcon('swap', 11))
        )
      : h('span', { class: 'sc-unassigned-label', text: this.lang === 'fr' ? 'NON ASSIGNÉ' : 'UNASSIGNED' });

    this.playerStripEl.replaceChildren(
      h('div', { class: 'sc-telem-cell' },
        h('span', { class: 'sc-telem-lbl', text: this.lang === 'fr' ? 'OPÉRATEUR' : 'OPERATOR' }),
        callsignNode
      ),
      h('div', { class: 'sc-telem-cell' },
        h('span', { class: 'sc-telem-lbl', text: 'TELEMETRY' }),
        h('span', { class: 'sc-score-pill', text: `${scores.total} / ${this.deck.maxScore} ${this.t('scoreLabel')}` })
      ),
      h('div', { class: 'sc-telem-cell' },
        h('span', { class: 'sc-telem-lbl', text: this.lang === 'fr' ? 'RANG' : 'STANDING' }),
        h('span', { class: 'sc-rank-pill', text: rankText })
      )
    );

    this.missionsBtn.replaceChildren(scIcon('trails', 13), this.t('missionsBtn'));
    this.aiBtn.replaceChildren(scIcon('capcom', 13), this.t('aiBtn'));
    this.leaderboardBtn.replaceChildren(
      scIcon('leaderboard', 13),
      this.lang === 'fr' ? 'Classement' : 'Leaderboard'
    );

    this.missionsBtn.classList.toggle('is-active', this.drawerMode === 'missions');
    this.aiBtn.classList.toggle('is-active', this.drawerMode === 'ai');

    this.panelEl.classList.toggle('is-collapsed', this.drawerCollapsed);
    this.edgeToggleBtn.classList.toggle('is-collapsed', this.drawerCollapsed);
    if (this.drawerCollapsed) {
      this.edgeToggleBtn.replaceChildren(scIcon('chevronLeft', 13), `${scores.total} PTS`);
    } else {
      this.edgeToggleBtn.replaceChildren(scIcon('chevronRight', 14));
    }
  }

  _renderInlineJoin() {
    if (this.identity.handle) {
      this.inlineJoinWrap.replaceChildren();
    } else if (!this.inlineJoinWrap.firstChild) {
      this.inlineJoinWrap.replaceChildren(this._buildCallsignClaimUI(true));
    }
  }

  _updateReticleVisibility() {
    const r = this.engine.state.activeRound;
    const show = this.drawerMode === 'missions' && (r === 2 || (r === 5 && this.engine.isRound5Unlocked()));
    this.reticleEl.classList.toggle('is-visible', show);
  }

  _switchToRound(nextRound) {
    const targetRound = Number(nextRound) || 1;
    if (targetRound !== 3 && this.roverDrawing) {
      this._stopRoverDrawing();
    }
    this.app.deactivateAllTools?.();
    this.app.shapeLayer?.stopDrawing?.();
    if (targetRound === 2 || targetRound === 3) {
      molaDem.ensureLoaded().catch(() => {});
    }
    this.engine.state.activeRound = targetRound;
    this.engine.save(this.identity);
    this._syncToFirebase(null);
    this._refreshAll();
    if (this.drawerBodyEl) {
      this.drawerBodyEl.scrollTop = 0;
    }
  }

  _renderRoundTabs() {
    this.roundTabsEl.hidden = this.drawerMode !== 'missions';
    if (this.drawerMode !== 'missions') return;

    const scores = this.engine.getRoundScores();
    const r5Open = this.engine.isRound5Unlocked();

    const tabs = this.deck.rounds.map((round) => {
      const n = round.roundNumber;
      const earned = scores[`r${n}`] || 0;
      const locked = n === 5 && !r5Open;
      const isComplete = earned >= round.maxPoints;
      const isActive = this.engine.state.activeRound === n;

      const ptsNode = locked
        ? h('span', { class: 'sc-round-tab-pts' }, scIcon('lock', 10), `${round.maxPoints}`)
        : h('span', { class: 'sc-round-tab-pts', text: `${earned}/${round.maxPoints}` });

      return h('button', {
        type: 'button',
        role: 'tab',
        'aria-selected': isActive ? 'true' : 'false',
        class: `sc-round-tab ${isActive ? 'is-active' : ''} ${isComplete ? 'is-complete' : ''} ${locked ? 'is-locked' : ''}`,
        onclick: () => this._switchToRound(n)
      },
        h('span', { class: 'sc-round-tab-num', text: `R${n}` }),
        ptsNode
      );
    });

    this.roundTabsEl.replaceChildren(...tabs);
  }

  _renderDrawerBody() {
    if (this.drawerMode === 'ai') {
      this._renderAICapComPanel();
      return;
    }

    const roundNum = this.engine.state.activeRound;
    const round = this.deck.rounds.find((r) => r.roundNumber === roundNum) || this.deck.rounds[0];
    const copy = round[this.lang] || round.en;
    const scores = this.engine.getRoundScores();
    const curRoundPts = scores[`r${roundNum}`] || 0;

    if (roundNum === 5 && !this.engine.isRound5Unlocked()) {
      this.drawerBodyEl.replaceChildren(
        h('div', { class: 'sc-step-card' },
          h('div', { class: 'sc-kicker', text: 'OUTER SOLAR SYSTEM · ENCRYPTED' }),
          h('h3', { class: 'sc-round-title', text: this.t('mysteryLockedTitle') }),
          h('p', { class: 'sc-round-summary', text: this.t('mysteryLockedDesc') })
        )
      );
      return;
    }

    const header = h('div', { class: 'sc-round-header' },
      h('div', { class: 'sc-kicker', text: `${this.t('roundLabel')} 0${roundNum} / 05 · ${curRoundPts} / ${round.maxPoints} ${this.t('scoreLabel')}` }),
      h('h3', { class: 'sc-round-title', text: copy.title }),
      h('p', { class: 'sc-round-summary', text: copy.summary })
    );

    const specAccordion = h('details', { class: 'sc-spec-panel' },
      h('summary', { class: 'sc-spec-summary' },
        h('span', { class: 'sc-spec-summary-left' },
          scIcon('sensor', 12),
          h('span', { text: copy.sensorTitle })
        ),
        h('span', { class: 'sc-spec-chevron' }, scIcon('chevronRight', 12))
      ),
      h('div', { class: 'sc-spec-body' },
        h('div', { class: 'sc-spec-skill-line', text: `${this.t('jsmarsSkillHeader')}: ${copy.jsmarsSkill}` }),
        h('div', { text: copy.sensorBody })
      )
    );

    let contentNodes = [];
    if (round.type === 'eo_classification') {
      contentNodes = this._renderRound1EO(round, copy);
    } else if (round.type === 'scan_and_profile') {
      contentNodes = this._renderRound2Altimetry(round);
    } else if (round.type === 'mars_rover_planner') {
      contentNodes = this._renderRound3Rover(round, copy);
    } else if (round.type === 'lunar_landing_scorer') {
      contentNodes = this._renderRound4Lunar(round, copy);
    } else if (round.type === 'ocean_world_finale') {
      contentNodes = this._renderRound5Europa(round);
    }

    const nextBtn = roundNum < 5
      ? h('button', {
          type: 'button',
          class: 'sc-btn sc-btn-primary',
          style: 'width: 100%; margin-top: 4px;',
          text: this.t('nextRoundBtn'),
          onclick: () => this._switchToRound(roundNum + 1)
        })
      : h('button', {
          type: 'button',
          class: 'sc-btn sc-btn-amber',
          style: 'width: 100%; margin-top: 4px;',
          onclick: () => this.openLeaderboardModal()
        },
          scIcon('podium', 13),
          this.lang === 'fr' ? 'Voir le Classement & Podium en Direct' : 'View Live Leaderboard & Winner’s Circle'
        );

    this.drawerBodyEl.replaceChildren(header, specAccordion, ...contentNodes, nextBtn);
  }

  // ── Map Overlays Sync (Round 1 EO Pins + Round 3 Mars Rover Pins) ────────
  _syncMapOverlays() {
    const body = (this.jmars.currentBody || '').toLowerCase();
    const r = this.engine?.state?.activeRound;

    // Round 1 Earth Observation orbital pins
    if (r === 1 && body === 'earth') {
      if (!this.map.hasLayer(this.eoGroup)) this.eoGroup.addTo(this.map);
      this._redrawEOPins();
    } else if (this.map.hasLayer(this.eoGroup)) {
      this.map.removeLayer(this.eoGroup);
    }

    // Round 3 Mars Rover overlay
    if (r === 3 && body === 'mars') {
      if (!this.map.hasLayer(this.roverGroup)) this.roverGroup.addTo(this.map);
      this._redrawRoverLayer();
    } else {
      if (this.roverDrawing) this._stopRoverDrawing();
      if (this.map.hasLayer(this.roverGroup)) this.map.removeLayer(this.roverGroup);
    }
  }

  _redrawEOPins() {
    this.eoGroup.clearLayers();
    const round1 = this.deck?.rounds?.find((r) => r.roundNumber === 1);
    if (!round1?.sites) return;

    round1.sites.forEach((site) => {
      const solved = Boolean(this.engine.state.r1Sites?.[site.id]?.solved);
      const marker = L.circleMarker([site.lat, site.lon], {
        radius: 9,
        color: solved ? '#34d399' : '#38bdf8',
        weight: 2.2,
        fillColor: solved ? '#059669' : '#0284c7',
        fillOpacity: 0.45
      }).bindTooltip(site.code);
      marker.on('click', () => {
        this._flyTo('earth', site.lat, site.lon, site.zoom);
      });
      marker.addTo(this.eoGroup);
    });
  }

  // ── Round 1 Renderer: Moroccan EO Land Cover Classification ──────────────
  _renderRound1EO(round, copy) {
    const cards = round.sites.map((site) => {
      const siteState = this.engine.state.r1Sites[site.id] || { attempts: 0, solved: false, points: 0, chosen: '' };
      const siteCopy = site[this.lang] || site.en;
      const fb = this.r1Feedback[site.id];

      const chips = round.landCoverClasses.map((cls) => {
        const isCorrectPick = siteState.solved && cls.id === site.correctClass;
        const isWrongLast = !siteState.solved && siteState.chosen === cls.id;
        return h('button', {
          type: 'button',
          class: `sc-class-chip ${isCorrectPick ? 'is-correct' : ''} ${isWrongLast ? 'is-wrong' : ''}`,
          disabled: siteState.solved,
          text: cls[this.lang] || cls.en,
          onclick: () => {
            const res = this.engine.submitLandCoverGuess(site.id, cls.id);
            if (res.correct) {
              this.r1Feedback[site.id] = { good: true, text: `+${res.totalSitePoints} pts · ${siteCopy.reveal}` };
              if (res.pointsAwarded > 0) {
                this._onPointsEarned(1, res.pointsAwarded, `EO Classified: ${site.code}`, `OT Classifiée : ${site.code}`);
              } else {
                this._renderDrawerBody();
              }
            } else {
              this.engine.save(this.identity);
              this.r1Feedback[site.id] = {
                good: false,
                text: this.lang === 'fr'
                  ? 'Pas tout à fait ! Cliquez sur « Voler vers la Zone », zoomez à 10 m et réessayez.'
                  : 'Not quite! Click "Fly to Coordinates", zoom in to 10 m Sentinel-2 pixels, and try again.'
              };
              this._renderDrawerBody();
            }
          }
        });
      });

      return h('div', { class: `sc-step-card ${siteState.solved ? 'is-solved' : ''}` },
        h('div', { class: 'sc-step-top' },
          h('span', { class: 'sc-step-title', text: site.code }),
          siteState.solved ? h('span', { class: 'sc-badge-done', text: `+${siteState.points} PTS` }) : null
        ),
        h('p', { class: 'sc-step-text', text: siteCopy.prompt }),
        h('div', { class: 'sc-btn-row' },
          h('button', {
            type: 'button',
            class: 'sc-btn sc-btn-primary',
            onclick: () => this._flyTo('earth', site.lat, site.lon, site.zoom)
          }, scIcon('locate', 12), this.t('flyBtn'))
        ),
        h('div', { class: 'sc-class-grid' }, ...chips),
        (siteState.solved || fb)
          ? h('div', {
              class: `sc-feedback ${siteState.solved ? 'is-good' : 'is-warn'}`,
              text: siteState.solved ? siteCopy.reveal : fb.text
            })
          : null
      );
    });

    const measDone = this.engine.state.r1MeasurePts > 0;
    const measureCard = h('div', { class: `sc-step-card ${measDone ? 'is-solved' : ''}` },
      h('div', { class: 'sc-step-top' },
        h('span', { class: 'sc-step-title', text: 'JSMARS Measurement Lab (+12 pts)' }),
        measDone ? h('span', { class: 'sc-badge-done', text: '+12 PTS' }) : null
      ),
      h('p', { class: 'sc-step-text', text: copy.measurePrompt }),
      h('div', { class: 'sc-btn-row' },
        h('button', {
          type: 'button',
          class: 'sc-btn sc-btn-emerald',
          onclick: async () => {
            await this._flyTo('earth', this.map.getCenter().lat, this.map.getCenter().lng, Math.max(8, this.map.getZoom()));
            this.app.quickActions?.options?.onRunTool?.('measure-area');
          }
        }, scIcon('tool', 12), this.lang === 'fr' ? 'Mesurer Surface (Polygone)' : 'Measure Area (Polygon)'),
        h('button', {
          type: 'button',
          class: 'sc-btn sc-btn-primary',
          onclick: async () => {
            await this._flyTo('earth', this.map.getCenter().lat, this.map.getCenter().lng, Math.max(8, this.map.getZoom()));
            this.app.quickActions?.options?.onRunTool?.('measure-distance');
          }
        }, scIcon('tool', 12), this.lang === 'fr' ? 'Mesurer Distance (Ligne)' : 'Measure Distance (Line)')
      )
    );

    return [...cards, measureCard];
  }

  // ── Round 2 Renderer: Mars Laser Altimetry & Megastructures ──────────────
  _renderRound2Altimetry(round) {
    const switchMarsCard = h('div', { class: 'sc-btn-row' },
      h('button', {
        type: 'button',
        class: 'sc-btn sc-btn-primary',
        style: 'width: 100%;',
        onclick: async () => {
          await switchActiveBody(this.jmars, 'mars');
          molaDem.ensureLoaded().catch(() => {});
          const layers = jmarsState.get('activeLayers') || [];
          if (!layers.some((l) => l.id === 'mars_mola_color')) {
            const next = [...layers, { id: 'mars_mola_color', opacity: 0.78, visible: true }];
            jmarsState.setActiveLayers(next);
          }
        }
      }, scIcon('globe', 12), this.lang === 'fr' ? 'Activer Mars + Relief MOLA Colorisé' : 'Switch to Mars + Enable MOLA Elevation')
    );

    const targetCards = round.targets.map((target) => {
      const tState = this.engine.state.r2Targets[target.id] || { hintsUsed: 0, solved: false, points: 0 };
      const tCopy = target[this.lang] || target.en;
      const fb = this.scanFeedback[target.id];

      return h('div', { class: `sc-step-card ${tState.solved ? 'is-solved' : ''}` },
        h('div', { class: 'sc-step-top' },
          h('span', { class: 'sc-step-title', text: tCopy.title }),
          tState.solved ? h('span', { class: 'sc-badge-done', text: `+${tState.points} PTS` }) : null
        ),
        h('p', { class: 'sc-step-text', text: tCopy.clue }),
        tState.hintsUsed >= 1 ? h('div', { class: 'sc-feedback', text: `HINT 01 // ${tCopy.hint1}` }) : null,
        tState.hintsUsed >= 2 ? h('div', { class: 'sc-feedback', text: `HINT 02 // ${tCopy.hint2}` }) : null,
        !tState.solved ? h('div', { class: 'sc-btn-row' },
          h('button', {
            type: 'button',
            class: 'sc-btn sc-btn-emerald',
            onclick: () => {
              const center = this.map.getCenter();
              const res = this.engine.scanRound2Target(target.id, this.jmars.currentBody, center.lat, center.lng);
              if (res.matched) {
                this.scanFeedback[target.id] = null;
                this._onPointsEarned(2, res.pointsAwarded, tCopy.title, tCopy.title);
              } else if (res.wrongBody) {
                this.scanFeedback[target.id] = `${this.t('wrongBodyPrefix')} ${res.requiredBody.toUpperCase()}!`;
                this._renderDrawerBody();
              } else {
                this.scanFeedback[target.id] = `${this.t('offTargetPrefix')} ${res.distanceKm} km (${res.compass}) ${this.t('offTargetSuffix')}`;
                this._renderDrawerBody();
              }
            }
          }, scIcon('crosshair', 12), this.t('scanBtn')),
          tState.hintsUsed < 1 ? h('button', {
            type: 'button',
            class: 'sc-btn',
            text: `${this.t('hint1Btn')} (-${target.hintCost}p)`,
            onclick: () => {
              this.engine.unlockRound2Hint(target.id, 1);
              this.engine.save(this.identity);
              this._renderDrawerBody();
            }
          }) : null,
          tState.hintsUsed < 2 ? h('button', {
            type: 'button',
            class: 'sc-btn',
            text: `${this.t('hint2Btn')} (-${target.hintCost * 2}p)`,
            onclick: () => {
              this.engine.unlockRound2Hint(target.id, 2);
              this.engine.save(this.identity);
              this._renderDrawerBody();
            }
          }) : null
        ) : null,
        fb ? h('div', { class: 'sc-feedback is-warn', text: fb }) : null
      );
    });

    const labCopy = round.lab[this.lang] || round.lab.en;
    const profDone = this.engine.state.r2ProfilePts > 0;
    const labCard = h('div', { class: `sc-step-card ${profDone ? 'is-solved' : ''}` },
      h('div', { class: 'sc-step-top' },
        h('span', { class: 'sc-step-title', text: labCopy.title }),
        profDone ? h('span', { class: 'sc-badge-done', text: '+30 PTS' }) : null
      ),
      h('p', { class: 'sc-step-text', text: labCopy.prompt }),
      h('div', { class: 'sc-btn-row' },
        h('button', {
          type: 'button',
          class: 'sc-btn sc-btn-primary',
          text: this.t('launchToolBtn'),
          onclick: () => this.app.quickActions?.options?.onRunTool?.('profile-linear')
        })
      )
    );

    return [switchMarsCard, ...targetCards, labCard];
  }

  // ── Round 3 Renderer: Mars Rover Traverse & Payload Tool Planning ────────
  _startRoverDrawing() {
    const round3 = this.deck.rounds.find((r) => r.roundNumber === 3);
    this.app.deactivateAllTools?.();
    this.roverDrawing = true;
    if (this.roverWaypoints.length === 0) {
      this.roverWaypoints = [{ lat: round3.start.lat, lon: round3.start.lon }];
    }
    this.map.getContainer().style.cursor = 'crosshair';
    this.map.on('click', this._onMapClickRover);
    this._redrawRoverLayer();
    this._renderDrawerBody();
  }

  _stopRoverDrawing() {
    this.roverDrawing = false;
    this.map.getContainer().style.cursor = '';
    this.map.off('click', this._onMapClickRover);
  }

  _onMapClickRover(e) {
    if (!this.roverDrawing || !e?.latlng) return;
    if (this.roverWaypoints.length >= 14) return;
    this.roverWaypoints.push({ lat: e.latlng.lat, lon: e.latlng.lng });
    this._redrawRoverLayer();
    this._renderDrawerBody();
  }

  _redrawRoverLayer() {
    this.roverGroup.clearLayers();
    const round3 = this.deck?.rounds?.find((r) => r.roundNumber === 3);
    if (!round3) return;

    L.circleMarker([round3.start.lat, round3.start.lon], {
      radius: 8,
      color: '#ecfdf5',
      weight: 2,
      fillColor: '#10b981',
      fillOpacity: 0.95
    }).bindTooltip('START: Jezero Landing Zone').addTo(this.roverGroup);

    const degRadius = (round3.goal.captureRadiusKm || 22) / 59.27;
    L.circle([round3.goal.lat, round3.goal.lon], {
      radius: degRadius * 111319.5,
      color: '#fbbf24',
      weight: 1.8,
      dashArray: '5 5',
      fillColor: '#fbbf24',
      fillOpacity: 0.12
    }).addTo(this.roverGroup);

    L.circleMarker([round3.goal.lat, round3.goal.lon], {
      radius: 8,
      color: '#fef3c7',
      weight: 2,
      fillColor: '#f59e0b',
      fillOpacity: 0.95
    }).bindTooltip('GOAL: Nili Carbonate Highlands').addTo(this.roverGroup);

    if (this.roverWaypoints.length >= 2) {
      L.polyline(
        this.roverWaypoints.map((p) => [p.lat, p.lon]),
        { color: '#38bdf8', weight: 3.5, dashArray: '8 6' }
      ).addTo(this.roverGroup);
    }

    this.roverWaypoints.forEach((pt, idx) => {
      if (idx === 0) return;
      L.circleMarker([pt.lat, pt.lon], {
        radius: 5,
        color: '#fff',
        weight: 1.5,
        fillColor: '#0284c7',
        fillOpacity: 1
      }).bindTooltip(`WP ${idx}`).addTo(this.roverGroup);
    });
  }

  _renderRoverSparkline(elevations = []) {
    const clean = elevations.filter((z) => Number.isFinite(z));
    if (clean.length < 3) return null;
    const minZ = Math.min(...clean);
    const maxZ = Math.max(...clean);
    const span = Math.max(20, maxZ - minZ);
    const w = 310;
    const hPx = 48;

    const svgNS = 'http://www.w3.org/2000/svg';
    const svg = document.createElementNS(svgNS, 'svg');
    svg.setAttribute('viewBox', `0 0 ${w} ${hPx}`);
    svg.setAttribute('width', '100%');
    svg.setAttribute('height', '48');
    svg.style.display = 'block';
    svg.style.marginTop = '6px';

    const pts = clean.map((z, i) => {
      const x = (i / (clean.length - 1)) * (w - 8) + 4;
      const y = hPx - 6 - ((z - minZ) / span) * (hPx - 12);
      return `${x.toFixed(1)},${y.toFixed(1)}`;
    }).join(' ');

    const poly = document.createElementNS(svgNS, 'polyline');
    poly.setAttribute('fill', 'none');
    poly.setAttribute('stroke', '#38bdf8');
    poly.setAttribute('stroke-width', '2.2');
    poly.setAttribute('points', pts);
    svg.appendChild(poly);

    return h('div', { style: 'padding:4px 6px; border-radius:6px; background:rgba(2,6,23,0.7); border:1px solid rgba(56,189,248,0.22); margin-top:4px;' },
      h('div', { style: 'display:flex; justify-content:space-between; font-size:10px; color:#94a3b8;' },
        h('span', { text: `MOLA Min: ${Math.round(minZ)} m` }),
        h('span', { text: `Max: ${Math.round(maxZ)} m` })
      ),
      svg
    );
  }

  _renderRound3Rover(round, copy) {
    const selectedSet = new Set(this.engine.state.r3SelectedTools || []);

    const checklist = h('div', { class: 'sc-check-list' },
      ...round.toolChoices.map((choice) => {
        const cb = h('input', {
          type: 'checkbox',
          checked: selectedSet.has(choice.id),
          onchange: () => {
            if (cb.checked) selectedSet.add(choice.id);
            else selectedSet.delete(choice.id);
            this.engine.state.r3SelectedTools = Array.from(selectedSet);
            this.engine.save(this.identity);
          }
        });
        return h('label', { class: 'sc-check-item' }, cb, h('span', { text: choice[this.lang] || choice.en }));
      })
    );

    const toolsCard = h('div', { class: `sc-step-card ${this.engine.state.r3BestToolsPts >= 25 ? 'is-solved' : ''}` },
      h('div', { class: 'sc-step-top' },
        h('span', { class: 'sc-step-title', text: 'Step 3A · Mission Tool Selection (30 pts)' }),
        this.engine.state.r3BestToolsPts > 0
          ? h('span', { class: 'sc-badge-done', text: `${this.engine.state.r3BestToolsPts}/30 PTS` })
          : null
      ),
      h('p', { class: 'sc-step-text', text: copy.toolsPrompt }),
      checklist
    );

    const evalStatus = h('div', { style: 'font-size: 11px; color: #38bdf8;' });

    const scoreBtn = h('button', {
      type: 'button',
      id: 'sc-score-rover-btn',
      class: 'sc-btn sc-btn-emerald',
      style: 'width:100%;',
      text: copy.evaluateRouteBtn,
      onclick: async () => {
        if (this.roverDrawing) this._stopRoverDrawing();
        const pts = this.roverWaypoints.length >= 2
          ? this.roverWaypoints
          : [{ lat: round.start.lat, lon: round.start.lon }];
        scoreBtn.disabled = true;
        evalStatus.textContent = this.lang === 'fr'
          ? 'Échantillonnage des altitudes laser USGS MOLA le long du tracé...'
          : 'Sampling live USGS MOLA laser elevations along your rover route...';

        const sampled = pts.length >= 2 ? interpolateWaypoints(pts, 32) : pts;
        let elevations = [];
        if (sampled.length >= 2) {
          try {
            elevations = await Promise.race([
              molaDem.sampleElevations(sampled.map((p) => ({ lat: p.lat, lng: p.lon }))),
              new Promise((resolve) => setTimeout(() => resolve([]), 9000))
            ]);
          } catch {
            elevations = [];
          }
        }
        this.lastSampledElevations = elevations;

        const result = this.engine.evaluateRound3Plan(
          this.engine.state.r3SelectedTools || [],
          sampled,
          elevations
        );
        scoreBtn.disabled = false;

        if (result.deltaPoints > 0) {
          this._onPointsEarned(3, result.deltaPoints, 'Jezero Rover Traverse Plan', 'Plan de Traverse Rover Jezero');
        } else {
          this.engine.save(this.identity);
          this._syncToFirebase(null);
          this._renderDrawerBody();
        }
      }
    });

    const summary = this.engine.state.r3LastRouteSummary;
    const summaryNode = summary
      ? h('div', { class: `sc-feedback ${summary.reachedGoal ? 'is-good' : 'is-warn'}` },
          h('div', { style: 'font-weight:700; margin-bottom:3px;', text: `Tools: ${summary.toolScore}/30 pts · Route: ${summary.totalPoints}/75 pts` }),
          h('div', {
            text: summary.reachedGoal
              ? `Goal Reached! Length: ${summary.distanceKm} km · Max MOLA Slope: ${summary.maxSlopeDeg}° · Climb: +${summary.totalClimbM} m`
              : `Goal is still ${summary.goalDistKm} km away! Extend your rover waypoints into the Gold Nili Highlands circle and re-score.`
          }),
          this._renderRoverSparkline(this.lastSampledElevations)
        )
      : null;

    const routeCard = h('div', { class: `sc-step-card ${this.engine.state.r3BestRoutePts >= 50 ? 'is-solved' : ''}` },
      h('div', { class: 'sc-step-top' },
        h('span', { class: 'sc-step-title', text: `Step 3B · Rover Route (${this.roverWaypoints.length} pts)` }),
        this.engine.state.r3BestRoutePts > 0
          ? h('span', { class: 'sc-badge-done', text: `${this.engine.state.r3BestRoutePts}/75 PTS` })
          : null
      ),
      h('p', { class: 'sc-step-text', text: copy.routePrompt }),
      h('div', { class: 'sc-btn-row' },
        h('button', {
          type: 'button',
          class: 'sc-btn sc-btn-primary',
          text: copy.flyJezeroBtn,
          onclick: async () => {
            await this._flyTo('mars', 18.56, 77.26, 8);
            molaDem.ensureLoaded().catch(() => {});
            const layers = (jmarsState.get('activeLayers') || []).filter((l) => l.id !== 'mars_mola_color');
            if (!layers.some((l) => l.id === 'mars_mola_themis_blend')) {
              const next = [...layers, { id: 'mars_mola_themis_blend', opacity: 0.90, visible: true }];
              jmarsState.setActiveLayers(next);
            } else {
              jmarsState.setActiveLayers(layers);
            }
            this._syncMapOverlays();
          }
        }),
        h('button', {
          type: 'button',
          id: 'sc-draw-rover-btn',
          class: `sc-btn ${this.roverDrawing ? 'sc-btn-emerald' : ''}`,
          text: this.roverDrawing ? (this.lang === 'fr' ? '✓ Tracé Actif (Cliquez Carte)' : '✓ Clicking Map...') : copy.drawRouteBtn,
          onclick: async () => {
            if ((this.jmars.currentBody || '').toLowerCase() !== 'mars') {
              await this._flyTo('mars', 18.56, 77.26, 8);
            }
            molaDem.ensureLoaded().catch(() => {});
            if (this.roverDrawing) {
              this._stopRoverDrawing();
              this._renderDrawerBody();
            } else {
              this._startRoverDrawing();
            }
          }
        }),
        h('button', {
          type: 'button',
          class: 'sc-btn',
          text: copy.clearRouteBtn,
          onclick: () => {
            this.roverWaypoints = [];
            this.lastSampledElevations = [];
            this._redrawRoverLayer();
            this._renderDrawerBody();
          }
        })
      ),
      scoreBtn,
      evalStatus,
      summaryNode
    );

    const labDone = this.engine.state.r3LabPts > 0;
    const labCard = h('div', { class: `sc-step-card ${labDone ? 'is-solved' : ''}` },
      h('div', { class: 'sc-step-top' },
        h('span', { class: 'sc-step-title', text: 'Step 3C · Landing Sites / Craters (+15 pts)' }),
        labDone ? h('span', { class: 'sc-badge-done', text: '+15 PTS' }) : null
      ),
      h('p', { class: 'sc-step-text', text: copy.labPrompt }),
      h('div', { class: 'sc-btn-row' },
        h('button', {
          type: 'button',
          class: 'sc-btn',
          text: 'Toggle Landing Sites',
          onclick: () => document.getElementById('landing-sites-btn')?.click()
        })
      )
    );

    return [toolsCard, routeCard, labCard];
  }

  // ── Round 4 Renderer: Artemis Lunar South Pole Landing Scorer ────────────
  _renderMeter(label, val, max) {
    const pct = Math.max(0, Math.min(100, Math.round((val / max) * 100)));
    return h('div', { class: 'sc-meter-row' },
      h('div', { class: 'sc-meter-label' },
        h('span', { text: label }),
        h('strong', { text: `${val} / ${max}` })
      ),
      h('div', { class: 'sc-meter-track' },
        h('div', { class: 'sc-meter-fill', style: `width:${pct}%` })
      )
    );
  }

  _commitInspectedLunarSite() {
    if (!this.lastInspectedLunar) return;
    const live = scoreLunarLandingSite(this.lastInspectedLunar);
    const res = this.engine.evaluateRound4LunarSite(
      this.lastInspectedLunar,
      this.lastInspectedLunar.siteName || ''
    );
    if (res.deltaPoints > 0) {
      this._onPointsEarned(4, res.deltaPoints, `Artemis Site (${res.bestPoints}/150)`, `Site Artemis (${res.bestPoints}/150)`);
    } else {
      this.engine.save(this.identity);
      this._syncToFirebase(null);
      this._showToast(
        this.lang === 'fr'
          ? `Site évalué : ${live.totalPoints}/150 pts`
          : `Site evaluated: ${live.totalPoints}/150 pts`
      );
      this._refreshAll();
    }
    this._renderLunarPlannerScorer();
  }

  _renderRound4Lunar(round, copy) {
    const last = this.lastInspectedLunar
      ? scoreLunarLandingSite(this.lastInspectedLunar)
      : this.engine.state.r4LastSiteSummary;
    const labels = copy.criteriaLabels;

    const launchCard = h('div', { class: `sc-step-card ${this.engine.state.r4BestSitePts >= 110 ? 'is-solved' : ''}` },
      h('div', { class: 'sc-step-top' },
        h('span', { class: 'sc-step-title', text: `Best Artemis Site: ${this.engine.state.r4BestSitePts} / 150 PTS` }),
        this.engine.state.r4BestSitePts > 0
          ? h('span', { class: 'sc-badge-done', text: `${this.engine.state.r4BestSitePts} PTS` })
          : null
      ),
      h('div', { class: 'sc-btn-row' },
        h('button', {
          type: 'button',
          id: 'sc-open-lunar-planner-btn',
          class: 'sc-btn sc-btn-emerald',
          style: 'width:100%;',
          text: copy.openPlannerBtn,
          onclick: async () => {
            if ((this.jmars.currentBody || '').toLowerCase() !== 'moon') {
              await switchActiveBody(this.jmars, 'moon');
            }
            if (this.app.lunarSitePlanner) {
              await this.app.lunarSitePlanner.open();
              this._renderLunarPlannerScorer();
            }
          }
        })
      ),
      last ? h('div', { class: 'sc-meter-list', style: 'margin-top:6px;' },
        h('div', {
          style: 'font-family: Space Grotesk, sans-serif; font-weight:700; color:#38bdf8; font-size:12.5px;',
          text: `${last.totalPoints}/150 PTS · ${this.lang === 'fr' ? last.ratingFr : last.ratingEn}`
        }),
        this._renderMeter(labels.slope, last.slopePoints, 45),
        this._renderMeter(labels.sun, last.sunPoints, 45),
        this._renderMeter(labels.earth, last.earthPoints, 30),
        this._renderMeter(labels.ice, last.icePoints, 30),
        h('div', { class: 'sc-feedback', text: this.lang === 'fr' ? last.adviceFr : last.adviceEn }),
        this.lastInspectedLunar ? h('button', {
          type: 'button',
          class: 'sc-btn sc-btn-emerald',
          style: 'width:100%; margin-top:4px;',
          text: this.lang === 'fr'
            ? `Valider ce Site (${last.totalPoints} / 150 pts)`
            : `Commit & Sync Landing Site (${last.totalPoints} / 150 pts)`,
          onclick: () => this._commitInspectedLunarSite()
        }) : null
      ) : null
    );

    return [launchCard];
  }

  _renderLunarPlannerScorer() {
    const planner = this.app.lunarSitePlanner;
    if (!planner?.side) return;

    let box = planner.side.querySelector('#sc-lsp-scorer-box');
    if (!box) {
      box = h('div', { id: 'sc-lsp-scorer-box', class: 'sc-lsp-scorer' });
      planner.side.prepend(box);
    }

    const round4 = this.deck?.rounds?.find((r) => r.roundNumber === 4);
    const copy = round4?.[this.lang] || round4?.en;
    if (!copy) return;

    if (!this.lastInspectedLunar) {
      box.replaceChildren(
        h('div', { class: 'sc-kicker', text: 'SPACE CAMP ROUND 4 · 150 MAX PTS' }),
        h('div', { style: 'font-size: 12px; line-height:1.45;', text: this.lang === 'fr'
          ? '1. Choisissez un site Artemis ci-dessus (ex. Connecting ridge ou Shackleton rim). 2. Cliquez sur la carte polaire pour évaluer et noter votre site d’alunissage !'
          : '1. Select an Artemis candidate site in the top bar (e.g. Connecting ridge or Shackleton rim). 2. Click any ridge on the polar map to evaluate and score your landing pin!'
        })
      );
      return;
    }

    const live = scoreLunarLandingSite(this.lastInspectedLunar);
    const labels = copy.criteriaLabels;

    const commitBtn = h('button', {
      type: 'button',
      id: 'sc-commit-lunar-site-btn',
      class: 'sc-btn sc-btn-emerald',
      text: this.lang === 'fr'
        ? `Valider ce Site (${live.totalPoints} / 150 pts)`
        : `Commit & Sync Site (${live.totalPoints} / 150 pts)`,
      onclick: () => this._commitInspectedLunarSite()
    });

    const commitAndExitBtn = h('button', {
      type: 'button',
      id: 'sc-commit-exit-lunar-btn',
      class: 'sc-btn sc-btn-primary',
      text: this.lang === 'fr'
        ? 'Valider & Retour aux Manches →'
        : 'Save & Return to Trails →',
      onclick: () => {
        this._commitInspectedLunarSite();
        planner.close?.();
      }
    });

    box.replaceChildren(
      h('div', { class: 'sc-kicker', text: `ROUND 4 LIVE LOLA SCORE · BEST: ${this.engine.state.r4BestSitePts}/150` }),
      h('div', { style: 'font-family: Space Grotesk, sans-serif; font-size: 14.5px; font-weight:700; color:#34d399;', text: `${live.totalPoints} / 150 PTS · ${this.lang === 'fr' ? live.ratingFr : live.ratingEn}` }),
      this._renderMeter(labels.slope, live.slopePoints, 45),
      this._renderMeter(labels.sun, live.sunPoints, 45),
      this._renderMeter(labels.earth, live.earthPoints, 30),
      this._renderMeter(labels.ice, live.icePoints, 30),
      h('div', { class: 'sc-feedback', text: this.lang === 'fr' ? live.adviceFr : live.adviceEn }),
      h('div', { class: 'sc-btn-row' }, commitBtn, commitAndExitBtn)
    );
  }

  // ── Round 5 Renderer: Europa Ocean World Finale ──────────────────────────
  _renderRound5Europa(round) {
    const switchEuropaCard = h('div', { class: 'sc-btn-row' },
      h('button', {
        type: 'button',
        class: 'sc-btn sc-btn-primary',
        style: 'width: 100%;',
        onclick: async () => {
          await this._flyTo('europa', 0, 60, 3);
        }
      }, scIcon('globe', 12), this.lang === 'fr' ? 'Passer sur Europe (Système de Jupiter)' : 'Switch to Europa (Jupiter System)')
    );

    const t = round.target;
    const tCopy = t[this.lang] || t.en;
    const scanState = this.engine.state.r5Scan;
    const fb = this.scanFeedback.r5;

    const scanCard = h('div', { class: `sc-step-card ${scanState.solved ? 'is-solved' : ''}` },
      h('div', { class: 'sc-step-top' },
        h('span', { class: 'sc-step-title', text: tCopy.title }),
        scanState.solved ? h('span', { class: 'sc-badge-done', text: `+${scanState.points} PTS` }) : null
      ),
      h('p', { class: 'sc-step-text', text: tCopy.clue }),
      scanState.hintsUsed >= 1 ? h('div', { class: 'sc-feedback', text: `HINT 01 // ${tCopy.hint1}` }) : null,
      scanState.hintsUsed >= 2 ? h('div', { class: 'sc-feedback', text: `HINT 02 // ${tCopy.hint2}` }) : null,
      !scanState.solved ? h('div', { class: 'sc-btn-row' },
        h('button', {
          type: 'button',
          class: 'sc-btn sc-btn-emerald',
          onclick: () => {
            const c = this.map.getCenter();
            const res = this.engine.scanRound5Target(this.jmars.currentBody, c.lat, c.lng);
            if (res.matched) {
              this.scanFeedback.r5 = null;
              this._onPointsEarned(5, res.pointsAwarded, 'Conamara Chaos Decoded', 'Conamara Chaos Localisé');
            } else if (res.wrongBody) {
              this.scanFeedback.r5 = `${this.t('wrongBodyPrefix')} EUROPA!`;
              this._renderDrawerBody();
            } else {
              this.scanFeedback.r5 = `${this.t('offTargetPrefix')} ${res.distanceKm} km (${res.compass}) ${this.t('offTargetSuffix')}`;
              this._renderDrawerBody();
            }
          }
        }, scIcon('crosshair', 12), this.t('scanBtn')),
        scanState.hintsUsed < 1 ? h('button', {
          type: 'button',
          class: 'sc-btn',
          text: `${this.t('hint1Btn')} (-5p)`,
          onclick: () => {
            this.engine.unlockRound5Hint(1);
            this.engine.save(this.identity);
            this._renderDrawerBody();
          }
        }) : null,
        scanState.hintsUsed < 2 ? h('button', {
          type: 'button',
          class: 'sc-btn',
          text: `${this.t('hint2Btn')} (-10p)`,
          onclick: () => {
            this.engine.unlockRound5Hint(2);
            this.engine.save(this.identity);
            this._renderDrawerBody();
          }
        }) : null
      ) : null,
      fb ? h('div', { class: 'sc-feedback is-warn', text: fb }) : null
    );

    const mCopy = round.mosaicStep[this.lang] || round.mosaicStep.en;
    const mosaicDone = this.engine.state.r5MosaicPts > 0;
    const mosaicCard = h('div', { class: `sc-step-card ${mosaicDone ? 'is-solved' : ''}` },
      h('div', { class: 'sc-step-top' },
        h('span', { class: 'sc-step-title', text: mCopy.title }),
        mosaicDone ? h('span', { class: 'sc-badge-done', text: '+55 PTS' }) : null
      ),
      h('p', { class: 'sc-step-text', text: mCopy.prompt }),
      h('div', { class: 'sc-btn-row' },
        h('button', {
          type: 'button',
          class: 'sc-btn sc-btn-primary',
          text: mCopy.btn,
          onclick: async () => {
            await this._flyTo('europa', 8.85, 86.15, 8);
            const layers = jmarsState.get('activeLayers') || [];
            if (!layers.some((l) => l.id === 'europa_conamara_chaos')) {
              const next = [...layers, { id: 'europa_conamara_chaos', opacity: 1.0, visible: true }];
              jmarsState.setActiveLayers(next);
            } else {
              const pts = this.engine.awardRound5Mosaic();
              if (pts > 0) this._onPointsEarned(5, pts, 'Galileo 9m/px Mosaic', 'Mosaïque Galileo 9m/px');
            }
          }
        })
      )
    );

    const cCopy = round.cartographyStep[this.lang] || round.cartographyStep.en;
    const cartoDone = this.engine.state.r5CartoPts > 0;
    const cartoCard = h('div', { class: `sc-step-card ${cartoDone ? 'is-solved' : ''}` },
      h('div', { class: 'sc-step-top' },
        h('span', { class: 'sc-step-title', text: cCopy.title }),
        cartoDone ? h('span', { class: 'sc-badge-done', text: '+55 PTS' }) : null
      ),
      h('p', { class: 'sc-step-text', text: cCopy.prompt }),
      h('div', { class: 'sc-btn-row' },
        h('button', {
          type: 'button',
          class: 'sc-btn',
          text: 'Open Shapes Tool',
          onclick: () => this.app.openToolByTitle?.('Shapes')
        }),
        h('button', {
          type: 'button',
          class: 'sc-btn sc-btn-primary',
          text: 'Switch to 3D Globe',
          onclick: () => this.app.viewModeToggle?.setMode?.('3d')
        })
      )
    );

    return [switchEuropaCard, scanCard, mosaicCard, cartoCard];
  }

  // ── AI CapCom Panel (Gemini 3.8 Flash + Live JSMARS Telemetry) ───────────
  _buildTelemetryContext() {
    const c = this.map?.getCenter?.() || { lat: 0, lng: 0 };
    const activeLayers = (jmarsState.get('activeLayers') || [])
      .filter((l) => l.visible !== false)
      .map((l) => l.id);
    const roundNum = this.engine.state.activeRound;
    const round = this.deck.rounds.find((r) => r.roundNumber === roundNum);
    const scores = this.engine.getRoundScores();

    return {
      body: this.jmars.currentBody || 'earth',
      lat: c.lat,
      lon: c.lng,
      zoom: this.map?.getZoom?.() || 4,
      activeLayers,
      roundNumber: roundNum,
      roundTitle: round?.[this.lang]?.title || round?.en?.title || '',
      totalScore: scores.total,
      roverSummary: this.engine.state.r3LastRouteSummary,
      lunarSummary: this.lastInspectedLunar
        ? scoreLunarLandingSite(this.lastInspectedLunar)
        : this.engine.state.r4LastSiteSummary
    };
  }

  async _sendCapComPrompt(promptText) {
    if (this.aiBusy) return;
    const q = String(promptText || '').trim();
    if (!q) return;

    this.aiMessages.push({ role: 'user', text: q });
    this.aiBusy = true;
    this._renderDrawerBody();

    const ctx = this._buildTelemetryContext();
    const res = await this.copilot.ask(q, ctx, this.lang);

    this.aiMessages.push({
      role: 'ai',
      text: res.text,
      source: res.source,
      model: res.model
    });
    this.aiBusy = false;
    this._renderDrawerBody();
  }

  _renderAICapComPanel() {
    const hasKey = this.copilot.hasApiKey();
    const header = h('div', { class: 'sc-round-header' },
      h('div', { style: 'display:flex; justify-content:space-between; align-items:center;' },
        h('span', { class: 'sc-kicker', text: hasKey ? 'GEMINI 3.8 FLASH · LIVE CAPCOM' : 'TELEMETRY CAPCOM · READY' }),
        h('button', {
          type: 'button',
          class: 'sc-mini-btn',
          onclick: () => {
            this.aiKeyPanelOpen = !this.aiKeyPanelOpen;
            this._renderDrawerBody();
          }
        }, scIcon('key', 11), hasKey ? 'Gemini Active' : 'Set Gemini Key')
      ),
      h('h3', {
        class: 'sc-round-title',
        text: this.lang === 'fr' ? 'Directeur de Vol IA CapCom' : 'AI Flight Director CapCom'
      }),
      h('p', {
        class: 'sc-round-summary',
        text: this.lang === 'fr'
          ? 'Analyse en direct vos coordonnées JSMARS, vos pentes MOLA et vos mesures lunaires LOLA sans dévoiler les réponses directes.'
          : 'Reads your live JSMARS coordinates, MOLA rover slopes, and NASA LOLA lunar metrics to give Socratic mission guidance.'
      })
    );

    let keyConfigCard = null;
    if (this.aiKeyPanelOpen) {
      const keyInput = h('input', {
        type: 'password',
        id: 'sc-gemini-key-input',
        class: 'sc-handle-input',
        placeholder: 'AIza... (Google AI Studio API Key)',
        value: this.copilot.getApiKey()
      });
      keyConfigCard = h('div', { class: 'sc-step-card' },
        h('div', { class: 'sc-step-title', text: this.lang === 'fr' ? 'Clé API Gemini (Optionnelle)' : 'Optional Gemini API Key' }),
        h('p', { class: 'sc-step-text', text: this.lang === 'fr'
          ? 'Stockée uniquement dans votre navigateur local (sessionStorage) pour activer Gemini 3.8 Flash en conversation libre.'
          : 'Stored strictly in your local browser session to upgrade CapCom to live multi-turn Gemini 3.8 Flash.'
        }),
        h('div', { class: 'sc-handle-row' },
          keyInput,
          h('button', {
            type: 'button',
            class: 'sc-btn sc-btn-emerald',
            text: this.lang === 'fr' ? 'Activer' : 'Save',
            onclick: () => {
              this.copilot.setApiKey(keyInput.value, false);
              this.aiKeyPanelOpen = false;
              this._renderDrawerBody();
            }
          }),
          hasKey ? h('button', {
            type: 'button',
            class: 'sc-btn',
            text: this.lang === 'fr' ? 'Effacer' : 'Clear',
            onclick: () => {
              this.copilot.setApiKey('', false);
              this.aiKeyPanelOpen = false;
              this._renderDrawerBody();
            }
          }) : null
        )
      );
    }

    const quickRow = h('div', { class: 'sc-btn-row' },
      h('button', {
        type: 'button',
        class: 'sc-btn sc-btn-primary',
        disabled: this.aiBusy,
        onclick: () => this._sendCapComPrompt(
          this.lang === 'fr'
            ? 'Analyse les capteurs et le terrain à mes coordonnées actuelles.'
            : 'Analyze the spacecraft sensors and terrain at my current map coordinates.'
        )
      }, scIcon('crosshair', 12), this.lang === 'fr' ? 'Analyser ma Vue' : 'Analyze Current View'),
      h('button', {
        type: 'button',
        class: 'sc-btn',
        disabled: this.aiBusy,
        onclick: () => this._sendCapComPrompt(
          this.lang === 'fr'
            ? 'Évalue mes mesures d’ingénierie (pente rover MOLA ou site lunaire LOLA) et propose une amélioration.'
            : 'Critique my current engineering telemetry (MOLA rover path or LOLA lunar site) and suggest how to score higher.'
        )
      }, scIcon('trails', 12), this.lang === 'fr' ? 'Évaluer Rover / Site' : 'Critique Rover / Site'),
      h('button', {
        type: 'button',
        class: 'sc-btn',
        disabled: this.aiBusy,
        onclick: () => this._sendCapComPrompt(
          this.lang === 'fr'
            ? 'Donne-moi un indice physique de télédétection pour ma manche en cours sans spoiler la réponse.'
            : 'Give me a remote-sensing physics clue for my active round without spoiling the answer.'
        )
      }, scIcon('sensor', 12), this.lang === 'fr' ? 'Indice Télédétection' : 'Sensor Clue')
    );

    const chatLog = h('div', { class: 'sc-chat-log' },
      ...(this.aiMessages.length === 0
        ? [
            h('div', { class: 'sc-feedback', text: this.lang === 'fr'
              ? 'Cliquez sur une analyse rapide ci-dessus ou posez une question sur votre carte actuelle !'
              : 'Click a quick telemetry check above or ask a question about the terrain on your screen!'
            })
          ]
        : this.aiMessages.map((m) =>
            m.role === 'user'
              ? h('div', { class: 'sc-chat-msg is-user', text: m.text })
              : h('div', { class: 'sc-chat-msg is-ai' },
                  h('span', {
                    class: `sc-provenance-pill ${m.source === 'gemini' ? 'is-gemini' : 'is-telemetry'}`,
                    text: m.source === 'gemini' ? 'Gemini 3.8 Flash' : 'Live JSMARS Telemetry'
                  }),
                  h('div', { text: m.text })
                )
          )),
      this.aiBusy
        ? h('div', { class: 'sc-feedback', text: this.lang === 'fr' ? 'Transmission CapCom en cours...' : 'CapCom analyzing telemetry...' })
        : null
    );

    const chatInput = h('input', {
      type: 'text',
      id: 'sc-capcom-input',
      class: 'sc-handle-input',
      placeholder: this.lang === 'fr' ? 'Question à CapCom...' : 'Ask CapCom about this terrain...'
    });
    const sendBtn = h('button', {
      type: 'button',
      id: 'sc-capcom-send-btn',
      class: 'sc-btn sc-btn-emerald',
      disabled: this.aiBusy,
      text: this.lang === 'fr' ? 'Envoyer' : 'Ask',
      onclick: () => {
        const val = chatInput.value;
        chatInput.value = '';
        this._sendCapComPrompt(val);
      }
    });
    chatInput.addEventListener('keydown', (e) => {
      if (e.key === 'Enter') {
        e.preventDefault();
        const val = chatInput.value;
        chatInput.value = '';
        this._sendCapComPrompt(val);
      }
    });

    const inputRow = h('div', { class: 'sc-handle-row', style: 'margin-top:auto;' }, chatInput, sendBtn);

    this.drawerBodyEl.replaceChildren(...[header, keyConfigCard, quickRow, chatLog, inputRow].filter(Boolean));
  }
}
