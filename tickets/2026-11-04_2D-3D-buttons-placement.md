# Ticket: 2D/3D buttons need better placement

- **Date filed:** 2026-11-04
- **Status:** resolved (2026-11-10)
- **Source:** phone screenshot 2026-11-04 09:42
- **Page:** https://jsmars.sounny.com/index.html?body=europa&lat=0&lon=0&z=2&layers=europa_galileo_voyager%3A1%3A1
- **View:** Europa globe (USGS Astrogeology / NASA / JPL imagery, CesiumJS 3D globe)

## Issue

The 2D/3D toggle buttons sit at the top center of the screen and overlap the
status bar, hiding the coordinate readout (Lat/Lon) and other indicators
behind them.

## Ask

Move the 2D/3D buttons somewhere they don't cover the status bar or the
coordinate readout.

## Resolution (2026-11-10)

- Wrapped `#view-mode-toggle` and the Quick Command trigger (`#quick-cmd-trigger`) inside `#map-top-hud` (`style.css` section 32).
- **Mobile (`<= 768px`):** Positioned `.map-top-hud` at `top: calc(38px + max(8px, env(safe-area-inset-top, 0px)))` directly below the fixed `#status-bar` (`0..32px`), keeping 44x44px touch targets and fitting cleanly between left/right map controls even on 320px viewports. Also hid the redundant inline `#status-scale` bar inside `#status-bar` on mobile so `Lat/Lon`, `E180`, `Zoom`, and `Loaded` fit cleanly without clipping.
- **Desktop (`>= 769px`):** Centered `.map-top-hud` and `.globe-chrome` over the visible map canvas (`left: calc(50% + 150px)` when sidebar is open, transitioning to `left: 50%` when `body.sidebar-collapsed` is active) so neither overlaps `#controls` or `#sidebar-toggle` on narrow desktop/tablet viewports.
- Verified with automated bounding-box non-overlap assertions in Playwright across `1440x900`, `800x600`, `390x844` (exact Europa 3D globe URL), and `320x568`.

## Evidence

Screenshot saved alongside this ticket:
2026-11-04_JSMARS_Europa_2D-3D-buttons.png
