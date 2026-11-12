/**
 * @module app-init
 * @description Shared application bootstrap for JSMARS (`/` and `/spacecamp/`).
 * Initializes map, all scientific tools, sidebar, status bar, 3D Cesium toggle,
 * keyboard shortcuts, and PWA manager without duplicating inline script code.
 */
import { JMARSMap } from './jmars-map.js';
import { CraterLayer } from './features/crater-counting/CraterLayer.js';
import { CraterTable } from './features/crater-counting/CraterTable.js';
import { RadialProfileTool } from './features/profile/RadialProfileTool.js';
import { ProfileChart } from './features/profile/ProfileChart.js';
import { MeasureTool } from './features/measure/MeasureTool.js';
import { MeasurementTable } from './features/measure/MeasurementTable.js';
import { LayerManager } from './ui/layer-manager.js';
import { Accordion } from './ui/Accordion.js';
import { StatusBar } from './ui/StatusBar.js';
import { FixedOverlays } from './ui/FixedOverlays.js';
import { Sidebar } from './ui/Sidebar.js';
import { SearchBar } from './ui/SearchBar.js';
import { CollapsibleGroup } from './ui/CollapsibleGroup.js';
import { SessionManager } from './ui/SessionManager.js';
import { BodySelector } from './ui/BodySelector.js';
import { NomenclatureTool } from './features/nomenclature/NomenclatureTool.js';
import { InvestigateTool } from './features/investigate/InvestigateTool.js';
import { BookmarksTool } from './features/bookmarks/BookmarksTool.js';
import { EnhancedProfileTool } from './features/profile/EnhancedProfileTool.js';
import { SamplingTool } from './features/sampling/SamplingTool.js';
import { SampleTable } from './features/sampling/SampleTable.js';
import { EVENTS } from './constants.js';
import { JMARS_CONFIG } from './jmars-config.js';
import { switchActiveBody } from './util/body.js';
import { StampLayer } from './features/stamp/StampLayer.js';
import { StampQueryPanel } from './features/stamp/StampQueryPanel.js';
import { ShapeLayer } from './features/shapes/ShapeLayer.js';
import { ShapeTable } from './features/shapes/ShapeTable.js';
import { ShapeIO } from './features/shapes/ShapeIO.js';
import { LandingSitesLayer } from './features/landing/LandingSitesLayer.js';
import { GroundTrackLayer } from './features/groundtrack/GroundTrackLayer.js';
import { GroundTrackPanel } from './features/groundtrack/GroundTrackPanel.js';
import { ExportTool } from './features/export/ExportTool.js';
import { PlacesManager } from './features/places/PlacesManager.js';
import { KeyboardShortcuts } from './ui/KeyboardShortcuts.js';
import { QuickActions } from './ui/QuickActions.js';
import { MiniMap } from './ui/MiniMap.js';
import { HillshadeLayer } from './features/hillshade/HillshadeLayer.js';
import { HillshadePanel } from './features/hillshade/HillshadePanel.js';
import { ColorStretchControl } from './ui/ColorStretchControl.js';
import { InfoPanel } from './ui/InfoPanel.js';
import { ContourLayer } from './features/contour/ContourLayer.js';
import { ContourPanel } from './features/contour/ContourPanel.js';
import { CustomMapManager } from './features/custom-map/CustomMapManager.js';
import { TimeSlider } from './features/slider/TimeSlider.js';
import { KRCPanel } from './features/krc/KRCPanel.js';
import { MCDPanel } from './features/mcd/MCDPanel.js';
import { ViewModeToggle } from './features/globe/ViewModeToggle.js';
import { BandMathPanel } from './features/bands/BandMathPanel.js';
import { ProjectionManager } from './features/projections/ProjectionManager.js';
import { GridLayer } from './features/grid/GridLayer.js';
import { GridPanel } from './features/grid/GridPanel.js';
import { PlanetaryScaleBar } from './ui/PlanetaryScaleBar.js';
import { RadarPanel } from './features/radar/RadarPanel.js';
import { LunarSitePlanner } from './features/lunar-site/LunarSitePlanner.js';
import { TrajectoryPanel } from './features/orbit/TrajectoryPanel.js';
import { PWAManager } from './pwa/PWAManager.js';
import { MobileSheet } from './ui/MobileSheet.js';
import { InteractionLogger } from './util/InteractionLogger.js';

/**
 * Bootstraps the complete JSMARS application in the current document.
 * @param {object} [options={}]
 * @param {boolean} [options.suppressWelcomeModal=false] - Suppress default beta welcome modal (e.g. in Space Camp mode)
 * @returns {object} Initialized JSMARS map, UI, and tool handles
 */
export function initJSMARSApp(options = {}) {
  InteractionLogger.init();

  // Initialize Map
  const jmars = new JMARSMap('map');
  jmars.init();
  window.jmars = jmars;

  // Initialize PWA Manager (Service Worker, install prompts, offline status)
  const pwaManager = new PWAManager();
  pwaManager.init();
  window.pwaManager = pwaManager;

  const installActionBtn = document.getElementById('pwa-install-action-btn');
  if (installActionBtn) {
    installActionBtn.addEventListener('click', () => pwaManager.promptInstall());
  }
  const installDismissBtn = document.getElementById('pwa-install-dismiss-btn');
  if (installDismissBtn) {
    installDismissBtn.addEventListener('click', () => {
      const banner = document.getElementById('pwa-install-banner');
      if (banner) banner.classList.remove('visible');
    });
  }

  // Welcome modal (one-time per session unless suppressed by Space Camp overlay)
  const welcomeBackdrop = document.getElementById('welcome-modal');
  const welcomeDismiss = document.getElementById('welcome-dismiss');
  let previousActiveElement = null;

  if (welcomeBackdrop) {
    if (options.suppressWelcomeModal) {
      welcomeBackdrop.style.display = 'none';
    } else {
      const openWelcome = () => {
        previousActiveElement = document.activeElement;
        welcomeBackdrop.style.display = 'flex';
        welcomeDismiss?.focus();
      };

      const closeWelcome = () => {
        welcomeBackdrop.style.display = 'none';
        sessionStorage.setItem('jsmars_welcome_shown', '1');
        if (previousActiveElement && previousActiveElement.focus) {
          previousActiveElement.focus();
        }
      };

      if (!sessionStorage.getItem('jsmars_welcome_shown')) {
        openWelcome();
      }
      welcomeDismiss?.addEventListener('click', closeWelcome);
      welcomeBackdrop.addEventListener('click', (e) => {
        if (e.target === welcomeBackdrop) closeWelcome();
      });

      document.addEventListener('keydown', (e) => {
        if (welcomeBackdrop.style.display === 'flex') {
          if (e.key === 'Escape') {
            closeWelcome();
          } else if (e.key === 'Tab') {
            const focusable = welcomeBackdrop.querySelectorAll('button, [href], input, select, textarea, [tabindex]:not([tabindex="-1"])');
            if (focusable.length > 0) {
              const first = focusable[0];
              const last = focusable[focusable.length - 1];
              if (e.shiftKey && document.activeElement === first) {
                e.preventDefault();
                last.focus();
              } else if (!e.shiftKey && document.activeElement === last) {
                e.preventDefault();
                first.focus();
              }
            }
          }
        }
      });
    }
  }

  // Initialize UI Components
  const layerManager = new LayerManager('layer-list', jmars);
  const accordion = new Accordion('controls');
  const statusBar = new StatusBar(jmars.map, 'status-bar');
  const fixedOverlays = new FixedOverlays(jmars, 'fixed-overlays');
  const sidebar = new Sidebar('controls', 'sidebar-toggle');
  const mobileSheet = new MobileSheet('controls', jmars.map);
  window.mobileSheet = mobileSheet;
  const searchBar = new SearchBar(jmars, 'search-bar-container');
  const toolsGroup = new CollapsibleGroup('tools-list');
  const bodySelector = new BodySelector('body-selector-container', jmars);

  const openAccordionSection = (sectionTitle) => {
    const sections = document.querySelectorAll('.accordion-section');
    sections.forEach((section) => {
      const header = section.querySelector('.accordion-header');
      if (!header) return;
      const isMatch = header.textContent.trim().toLowerCase() === sectionTitle.toLowerCase();
      section.classList.toggle('expanded', isMatch);
      header.setAttribute('aria-expanded', isMatch ? 'true' : 'false');
    });
  };

  const openToolByTitle = (title) => {
    if (window.innerWidth > 768 && sidebar.isCollapsed) {
      sidebar.toggle();
    } else if (window.innerWidth <= 768 && mobileSheet?.expand) {
      mobileSheet.expand();
    }
    openAccordionSection('Tools');
    const headers = document.querySelectorAll('#tools-list .layer-section-header');
    headers.forEach((h) => {
      const t = h.querySelector('.layer-title')?.textContent?.trim() || '';
      if (t === title) {
        const content = h.nextElementSibling;
        if (content && content.style.display === 'none') {
          h.click();
        }
        h.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
      }
    });
  };

  // Initialize Tools
  const craterLayer = new CraterLayer(jmars.map);
  const craterTable = new CraterTable('crater-table-container');
  const craterBtn = document.getElementById('crater-tool-btn');

  const profileTool = new RadialProfileTool(jmars.map);
  const profileSource = document.getElementById('profile-source-select');
  profileTool.populateSourceDropdown(profileSource);
  profileSource.addEventListener('change', (e) => profileTool.setElevationSource(e.target.value));
  const profileChart = new ProfileChart('profile-chart-container');
  const profileBtn = document.getElementById('profile-tool-btn');
  const enhancedProfileBtn = document.getElementById('enhanced-profile-btn');

  const measureTool = new MeasureTool(jmars.map);
  const measureTable = new MeasurementTable('measure-table-container', measureTool);
  const measureDistBtn = document.getElementById('measure-dist-btn');
  const measureAreaBtn = document.getElementById('measure-area-btn');
  const measureClearBtn = document.getElementById('measure-clear-btn');
  const measureExportJsonBtn = document.getElementById('measure-export-json-btn');
  const measureExportCsvBtn = document.getElementById('measure-export-csv-btn');

  const nomenclatureTool = new NomenclatureTool(jmars.map, 'nomenclature-tool-container');
  const investigateTool = new InvestigateTool(jmars.map);
  const investigateBtn = document.getElementById('investigate-tool-btn');

  const bookmarksTool = new BookmarksTool(jmars, 'bookmarks-tool-container');

  const enhancedProfileTool = new EnhancedProfileTool(jmars.map);
  const enhancedProfileSource = document.getElementById('enhanced-profile-source');
  enhancedProfileTool.populateSourceDropdown(enhancedProfileSource);
  enhancedProfileSource.addEventListener('change', (e) => enhancedProfileTool.setElevationSource(e.target.value));
  document.addEventListener(EVENTS.BODY_CHANGED, () => {
    profileTool.populateSourceDropdown(profileSource);
    enhancedProfileTool.populateSourceDropdown(enhancedProfileSource);
  });
  document.getElementById('enhanced-profile-export-btn').addEventListener('click', () => enhancedProfileTool.exportCSV());

  // Stamp Layer
  const stampLayer = new StampLayer(jmars.map);
  const stampPanel = new StampQueryPanel(
    document.getElementById('stamp-panel-container'), stampLayer
  );

  // Shape Layer
  const shapeLayer = new ShapeLayer(jmars.map, { vectors: jmars.vectors });
  shapeLayer.activate();
  window.shapeLayer = shapeLayer;
  const shapeTable = new ShapeTable(
    document.getElementById('shape-table-container'), shapeLayer, jmars.map
  );
  const shapeIO = new ShapeIO(shapeLayer);
  shapeIO.enableDragDrop(document.getElementById('map'));

  // Landing Sites
  const landingSitesLayer = new LandingSitesLayer(jmars.map);
  const landingBtn = document.getElementById('landing-sites-btn');
  landingBtn.addEventListener('click', () => {
    const active = landingSitesLayer.toggle();
    landingBtn.textContent = active ? 'Hide Landing Sites' : 'Show Landing Sites';
    landingBtn.classList.toggle('active', active);
  });

  // Ground Track
  const groundTrackLayer = new GroundTrackLayer(jmars.map);
  const groundTrackPanel = new GroundTrackPanel(
    document.getElementById('groundtrack-panel-container'), groundTrackLayer
  );

  // Interplanetary Trajectory & Delta-V
  const trajectoryPanel = new TrajectoryPanel(
    document.getElementById('trajectory-panel-container')
  );

  // Export Tool
  const exportTool = new ExportTool(jmars.map);
  document.getElementById('export-pub-btn')?.addEventListener('click', () => {
    const title = document.getElementById('export-title-input')?.value || `${(jmars.currentBody || 'Mars').toUpperCase()} Exploration Map`;
    exportTool.exportPublicationMap({ title, scale: 2 });
  });
  document.getElementById('export-png-btn')?.addEventListener('click', () => exportTool.exportPNG());
  document.getElementById('export-jpeg-btn')?.addEventListener('click', () => exportTool.exportJPEG());
  document.getElementById('export-world-btn')?.addEventListener('click', () => exportTool.exportWorldFile());

  // Places Manager
  const placesManager = new PlacesManager(jmars.map);

  // MiniMap Overview
  const getMiniMapLayerConfig = (bodyKey) => {
    const key = (bodyKey || 'mars').toLowerCase();
    const bodyConfig = JMARS_CONFIG.bodies[key];
    if (!bodyConfig?.layers || bodyConfig.layers.length === 0) return null;
    const defId = bodyConfig.defaultLayer;
    return bodyConfig.layers.find(l => l.id === defId) || bodyConfig.layers[0];
  };

  const initialMiniLayerConfig = getMiniMapLayerConfig(jmars.currentBody) || {
    type: 'xyz',
    url: JMARS_CONFIG.services.mars_basemap
  };
  const miniMap = new MiniMap(jmars.map, initialMiniLayerConfig);
  miniMap.activate();

  document.addEventListener(EVENTS.BODY_CHANGED, (e) => {
    const body = e?.detail?.body;
    const layerConfig = getMiniMapLayerConfig(body);
    if (layerConfig) {
      miniMap.updateBaseMap(layerConfig);
    }
  });

  // Hillshade Layer
  const hillshadeLayer = new HillshadeLayer(jmars.map);
  const hillshadePanel = new HillshadePanel(
    document.getElementById('hillshade-panel-container'), hillshadeLayer
  );

  // Color Stretch Control
  const colorStretch = new ColorStretchControl(jmars.map);
  window.jsmarsColorStretch = colorStretch;

  // Layer Info Panel
  const infoPanel = new InfoPanel();
  window.jsmarsInfoPanel = infoPanel;

  // Contour Lines Layer
  const contourLayer = new ContourLayer(jmars.map);
  const contourPanel = new ContourPanel(
    document.getElementById('contour-panel-container'), contourLayer
  );

  // Lat/Lon Graticule Grid Layer
  const gridLayer = new GridLayer(jmars.map);
  const gridPanel = new GridPanel(
    document.getElementById('grid-panel-container'), gridLayer
  );

  // Planetary Scale Bar
  const scaleBar = new PlanetaryScaleBar(jmars.map, { position: 'bottomleft' });
  scaleBar.add();

  // Custom Map Manager (GeoTIFF upload)
  const customMapManager = new CustomMapManager(jmars.map);
  document.getElementById('custom-geotiff-btn')?.addEventListener('click', () => {
    document.getElementById('custom-geotiff-input').click();
  });
  document.getElementById('custom-geotiff-input').addEventListener('change', (e) => {
    const file = e.target.files[0];
    if (file) {
      customMapManager.loadGeoTIFF(file);
    }
    e.target.value = '';
  });

  // Mars Time & Solar Longitude Slider
  const timeSlider = new TimeSlider('time-slider-container');

  // Projection & Viewpoint Manager
  const projectionManager = new ProjectionManager('projection-panel-container', jmars.map);

  // KRC Mars Thermal Model Panel
  const krcPanel = new KRCPanel(
    document.getElementById('krc-panel-container'), jmars.map
  );

  // 2D Leaflet map | 3D Cesium globe
  const viewModeToggle = new ViewModeToggle({
    jmars,
    toggleEl: document.getElementById('view-mode-toggle'),
    globeEl: document.getElementById('globe-view'),
    cesiumEl: document.getElementById('cesium-container'),
    noteEl: document.getElementById('view-3d-tool-note'),
    errorEl: document.getElementById('globe-load-error')
  });
  window.jsmarsView = viewModeToggle;

  // Mars Climate Database (MCD) Profiler
  const mcdPanel = new MCDPanel(
    document.getElementById('mcd-panel-container'), jmars.map
  );

  // Educational mineral-index visualizer
  const bandMathPanel = new BandMathPanel(
    document.getElementById('bandmath-panel-container'), jmars.map
  );

  // Subsurface radar sounder
  const radarPanel = new RadarPanel(
    document.getElementById('radar-panel-container'), jmars.map
  );

  // Lunar Base Site Planner
  let lunarSitePlanner = null;
  try {
    lunarSitePlanner = new LunarSitePlanner(document.getElementById('lunar-site-panel-container'));
  } catch (err) {
    console.error('Lunar Site Planner failed to initialise', err);
  }

  const samplingTool = new SamplingTool(jmars.map);
  const samplingPointBtn = document.getElementById('sampling-point-btn');
  const samplingAreaBtn = document.getElementById('sampling-area-btn');
  const sampleTable = new SampleTable('sample-table-container');
  document.getElementById('sample-export-btn').addEventListener('click', () => samplingTool.exportCSV());
  document.getElementById('sample-clear-btn').addEventListener('click', () => samplingTool.clear());

  const resetProfileButtons = () => {
    profileBtn.textContent = 'Radial Profile';
    profileBtn.classList.remove('active');
    enhancedProfileBtn.classList.remove('active');
  };

  const deactivateProfiles = () => {
    profileTool.deactivate();
    enhancedProfileTool.deactivate();
    resetProfileButtons();
  };

  const deactivateInvestigate = () => {
    if (investigateTool.isActive) {
      investigateTool.deactivate();
      investigateBtn.classList.remove('active');
    }
  };

  const deactivateSampling = () => {
    if (samplingTool.isActive) {
      samplingTool.deactivate();
    }
    samplingPointBtn.classList.remove('active');
    samplingAreaBtn.classList.remove('active');
  };

  const deactivateAllTools = () => {
    craterLayer.deactivate();
    craterBtn.textContent = 'Start Crater Counting';
    craterBtn.classList.remove('active');
    deactivateProfiles();
    measureTool.deactivate();
    measureDistBtn.classList.remove('active');
    measureAreaBtn.classList.remove('active');
    deactivateInvestigate();
    deactivateSampling();
  };

  // Shape draw buttons
  ['marker', 'polyline', 'polygon', 'circle', 'rectangle'].forEach(type => {
    const btn = document.getElementById(`shape-draw-${type}`);
    if (btn) {
      btn.addEventListener('click', () => {
        deactivateAllTools();
        shapeLayer.startDrawing(type);
      });
    }
  });
  document.getElementById('shape-edit-btn')?.addEventListener('click', () => {
    shapeLayer.enableEditing();
  });
  document.getElementById('shape-import-btn')?.addEventListener('click', () => shapeIO.importFile());
  document.getElementById('shape-export-btn')?.addEventListener('click', () => {
    const fmt = document.getElementById('shape-export-format').value;
    shapeIO.exportFile(fmt);
  });

  document.addEventListener(EVENTS.VIEW_MODE_CHANGED, (event) => {
    if (event.detail?.mode === '3d') {
      deactivateAllTools();
      shapeLayer.stopDrawing();
    }
  });

  samplingPointBtn.addEventListener('click', () => {
    samplingTool.activate('point');
    samplingPointBtn.classList.add('active');
    samplingAreaBtn.classList.remove('active');
    deactivateInvestigate();
  });

  samplingAreaBtn.addEventListener('click', () => {
    samplingTool.activate('area');
    samplingAreaBtn.classList.add('active');
    samplingPointBtn.classList.remove('active');
    deactivateInvestigate();
  });

  investigateBtn.addEventListener('click', () => {
    if (investigateTool.isActive) {
      deactivateInvestigate();
    } else {
      craterLayer.deactivate();
      craterBtn.innerText = 'Start Crater Counting';
      craterBtn.classList.remove('active');
      deactivateProfiles();
      measureTool.deactivate();
      measureDistBtn.classList.remove('active');
      measureAreaBtn.classList.remove('active');
      deactivateSampling();
      investigateTool.activate();
      investigateBtn.classList.add('active');
    }
  });

  // Session Manager
  const sessionManager = new SessionManager(jmars, craterLayer, measureTool, bookmarksTool, shapeLayer);
  document.getElementById('session-save-btn').addEventListener('click', () => sessionManager.saveSession());
  const loadInput = document.getElementById('session-load-input');
  document.getElementById('session-load-btn').addEventListener('click', () => loadInput.click());
  loadInput.addEventListener('change', (e) => {
    if (e.target.files.length > 0) {
      sessionManager.loadSession(e.target.files[0]);
      e.target.value = '';
    }
  });

  const shareBtn = document.getElementById('share-link-btn');
  const shareToast = document.getElementById('share-toast');
  if (shareBtn) {
    shareBtn.addEventListener('click', async () => {
      try {
        await jmars.copyShareLink();
        if (shareToast) {
          shareToast.style.display = 'block';
          setTimeout(() => { shareToast.style.display = 'none'; }, 2500);
        }
      } catch (err) {
        console.warn('Share link copy failed:', err);
      }
    });
  }

  document.addEventListener(EVENTS.UPDATE_VIEW, (e) => {
    if (e.detail) {
      jmars.applyViewState(e.detail);
    }
  });

  craterBtn.addEventListener('click', () => {
    if (craterLayer.isActive) {
      craterLayer.deactivate();
      craterBtn.textContent = 'Start Crater Counting';
      craterBtn.classList.remove('active');
    } else {
      deactivateAllTools();
      craterLayer.activate();
      craterBtn.textContent = 'Stop Crater Counting';
      craterBtn.classList.add('active');
    }
  });

  profileBtn.addEventListener('click', () => {
    if (profileTool.isActive) {
      profileTool.deactivate();
      profileBtn.textContent = 'Radial Profile';
      profileBtn.classList.remove('active');
    } else {
      deactivateAllTools();
      enhancedProfileTool.deactivate();
      enhancedProfileBtn.classList.remove('active');
      profileTool.activate();
      profileBtn.textContent = 'Stop Profile';
      profileBtn.classList.add('active');
    }
  });

  enhancedProfileBtn.addEventListener('click', () => {
    if (enhancedProfileTool.isActive) {
      enhancedProfileTool.deactivate();
      enhancedProfileBtn.classList.remove('active');
    } else {
      deactivateAllTools();
      profileTool.deactivate();
      profileBtn.textContent = 'Radial Profile';
      profileBtn.classList.remove('active');
      enhancedProfileTool.activate();
      enhancedProfileBtn.classList.add('active');
    }
  });

  measureDistBtn.addEventListener('click', () => {
    if (measureTool.activeMode === 'distance') {
      measureTool.deactivate();
      measureDistBtn.classList.remove('active');
    } else {
      deactivateAllTools();
      measureTool.activate('distance');
      measureDistBtn.classList.add('active');
      measureAreaBtn.classList.remove('active');
    }
  });

  measureAreaBtn.addEventListener('click', () => {
    if (measureTool.activeMode === 'area') {
      measureTool.deactivate();
      measureAreaBtn.classList.remove('active');
    } else {
      deactivateAllTools();
      measureTool.activate('area');
      measureAreaBtn.classList.add('active');
      measureDistBtn.classList.remove('active');
    }
  });

  measureClearBtn.addEventListener('click', () => measureTool.clear());
  measureExportJsonBtn.addEventListener('click', () => measureTool.exportGeoJSON());
  measureExportCsvBtn.addEventListener('click', () => measureTool.exportCSV());

  document.addEventListener(EVENTS.TOOL_DEACTIVATED, (e) => {
    if (!e.detail) return;
    if (e.detail.tool === 'crater') {
      craterBtn.textContent = 'Start Crater Counting';
      craterBtn.classList.remove('active');
    }
    if (e.detail.tool === 'measure') {
      measureDistBtn.classList.remove('active');
      measureAreaBtn.classList.remove('active');
    }
    if (e.detail.tool === 'profile') {
      resetProfileButtons();
    }
    if (e.detail.tool === 'sampling') {
      samplingPointBtn.classList.remove('active');
      samplingAreaBtn.classList.remove('active');
    }
    if (e.detail.tool === 'investigate') {
      investigateBtn.classList.remove('active');
    }
  });

  const layerListContainer = document.getElementById('layer-list');
  window.addEventListener('unhandledrejection', (event) => {
    console.error('Unhandled rejection:', event.reason);
    if (layerListContainer && layerListContainer.innerText.includes('Loading')) {
      layerListContainer.innerHTML = '';
      const errDiv = document.createElement('div');
      errDiv.className = 'error-msg';
      errDiv.textContent = `Error loading layers: ${event.reason?.message || event.reason || 'Unknown error'}`;
      layerListContainer.appendChild(errDiv);
    }
  });

  // Quick Command & Navigation Palette (Ctrl+K)
  const quickActions = new QuickActions(null, {
    jmarsMap: jmars,
    map: jmars.map,
    getLandmarks: () => searchBar.getSearchableLandmarks(),
    getBookmarks: () => bookmarksTool.getData ? bookmarksTool.getData() : [],
    onSelectLandmark: (lm) => searchBar.selectLandmark(lm),
    onSelectBookmark: (bm) => bookmarksTool.goTo(bm),
    onSwitchBody: (bodyKey) => switchActiveBody(jmars, bodyKey),
    onSetViewMode: (mode) => viewModeToggle.setMode(mode),
    onSaveSession: () => sessionManager.saveSession(),
    onLoadSession: () => document.getElementById('session-load-input')?.click(),
    onShareLink: () => document.getElementById('share-link-btn')?.click(),
    onResetView: () => {
      if (viewModeToggle.mode === '3d') {
        viewModeToggle.resetView();
      } else {
        const body = JMARS_CONFIG.bodies[jmars.currentBody];
        if (body) jmars.map.setView(body.center, body.zoom);
      }
    },
    onOpenLayers: () => {
      if (window.innerWidth > 768 && sidebar.isCollapsed) sidebar.toggle();
      openAccordionSection('Layer Manager');
    },
    onOpenTools: () => {
      if (window.innerWidth > 768 && sidebar.isCollapsed) sidebar.toggle();
      openAccordionSection('Tools');
    },
    onToggleSidebar: () => sidebar.toggle(),
    onRunTool: async (toolId) => {
      const twoDTools = new Set(['measure-distance', 'measure-area', 'profile-linear', 'profile-radial', 'investigate', 'crater', 'shapes', 'stamps']);
      if (twoDTools.has(toolId) && viewModeToggle.mode === '3d') {
        await viewModeToggle.setMode('2d');
      }
      if (toolId === 'measure-distance') { openToolByTitle('Measurement'); if (measureTool.activeMode !== 'distance') measureDistBtn.click(); }
      else if (toolId === 'measure-area') { openToolByTitle('Measurement'); if (measureTool.activeMode !== 'area') measureAreaBtn.click(); }
      else if (toolId === 'profile-linear') { openToolByTitle('Profiles'); if (!enhancedProfileTool.isActive) enhancedProfileBtn.click(); }
      else if (toolId === 'profile-radial') { openToolByTitle('Profiles'); if (!profileTool.isActive) profileBtn.click(); }
      else if (toolId === 'investigate') { openToolByTitle('Investigate'); if (!investigateTool.isActive) investigateBtn.click(); }
      else if (toolId === 'crater') { openToolByTitle('Crater Counting'); if (!craterLayer.isActive) craterBtn.click(); }
      else if (toolId === 'shapes') { openToolByTitle('Shapes'); }
      else if (toolId === 'stamps') { openToolByTitle('Stamp (Footprints)'); }
      else if (toolId === 'krc') { openToolByTitle('KRC Thermal Model'); }
      else if (toolId === 'mcd') { openToolByTitle('MCD Atmospheric Profiler'); }
      else if (toolId === 'radar') { openToolByTitle('Subsurface Radar Sounder'); }
      else if (toolId === 'lunar-site') {
        openToolByTitle('Lunar Base Site Planner');
        document.querySelector('#lunar-site-panel-container button')?.click();
      }
    }
  });
  window.quickActions = quickActions;
  document.getElementById('quick-cmd-trigger')?.addEventListener('click', () => quickActions.toggle());

  // Keyboard Shortcuts
  const shortcuts = new KeyboardShortcuts(jmars.map, {
    openQuickActions: () => quickActions.toggle(),
    saveSession: () => sessionManager.saveSession(),
    loadSession: () => document.getElementById('session-load-input').click(),
    exportMap: () => exportTool.exportPNG(),
    cancelTool: () => {
      deactivateAllTools();
      shapeLayer.stopDrawing();
    },
    resetView: () => {
      if (viewModeToggle.mode === '3d') {
        viewModeToggle.resetView();
      } else {
        const body = JMARS_CONFIG.bodies[jmars.currentBody];
        if (body) jmars.map.setView(body.center, body.zoom);
      }
    }
  });

  return {
    jmars,
    layerManager,
    accordion,
    statusBar,
    fixedOverlays,
    sidebar,
    mobileSheet,
    searchBar,
    toolsGroup,
    bodySelector,
    craterLayer,
    craterTable,
    profileTool,
    profileChart,
    enhancedProfileTool,
    measureTool,
    measureTable,
    nomenclatureTool,
    investigateTool,
    bookmarksTool,
    samplingTool,
    sampleTable,
    stampLayer,
    stampPanel,
    shapeLayer,
    shapeTable,
    shapeIO,
    landingSitesLayer,
    groundTrackLayer,
    groundTrackPanel,
    trajectoryPanel,
    exportTool,
    placesManager,
    miniMap,
    hillshadeLayer,
    hillshadePanel,
    colorStretch,
    infoPanel,
    contourLayer,
    contourPanel,
    gridLayer,
    gridPanel,
    scaleBar,
    customMapManager,
    timeSlider,
    projectionManager,
    krcPanel,
    mcdPanel,
    bandMathPanel,
    radarPanel,
    lunarSitePlanner,
    viewModeToggle,
    sessionManager,
    quickActions,
    shortcuts,
    pwaManager,
    openAccordionSection,
    openToolByTitle,
    deactivateAllTools
  };
}
