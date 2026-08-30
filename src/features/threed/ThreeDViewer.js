import { MarsTime } from '../slider/MarsTime.js';
import { ThreeDEngine } from './ThreeDEngine.js';
import { EventBus } from '../../core/EventBus.js';
import { EVENTS } from '../../constants.js';

/**
 * @module ThreeDViewer
 * @description Advanced 3D Interactive Globe and Terrain elevation viewer for jsMars.
 * Uses WebGL (Three.js) to render photorealistic planetary spheres with multi-body texture synthesis
 * (Mars, Moon, Earth, Phobos), atmospheric limb glow, interactive map center beacon, coordinate graticules,
 * auto-spin celestial rotation, solar ray-traced lighting, and click-to-pan map synchronization.
 */
export class ThreeDViewer {
  /**
   * @param {HTMLElement|string} container - Container DOM element or ID
   * @param {L.Map} map - Leaflet map instance
   */
  constructor(container, map) {
    this.container = typeof container === 'string' ? document.getElementById(container) : container;
    this.map = map;
    this.canvas = null;
    this.renderer = null;
    this.scene = null;
    this.camera = null;
    this.mesh = null;
    this.atmosphereMesh = null;
    this.graticuleMesh = null;
    this.targetMarker = null;
    this.sunLight = null;
    this.ambientLight = null;
    this.animId = null;

    this.mode = 'globe'; // 'globe' or 'terrain'
    this.body = 'mars';
    this.exaggeration = 5.0; // 1x to 25x
    this.wireframe = false;
    this.autoSpin = true;
    this.showAtmosphere = true;
    this.showGraticule = true;
    this.sunAngle = { Ls: 0, hour: 12 };

    // Mouse & Touch interaction state
    this.isDragging = false;
    this.previousMousePosition = { x: 0, y: 0 };
    this.rotation = { x: 0.35, y: 0.2 };
    this.targetRotation = { x: 0.35, y: 0.2 };
    this.zoom = 1.0;
    this.globeRadius = 22.0;

    // Raycaster for coordinate inspection and click navigation
    this.raycaster = null;
    this.mouse = null;
    this.hoverCoords = null;
    this.onCoordsChange = null;

    if (this.container) {
      this.init();
    }
  }

  init() {
    this.container.style.position = 'relative';
    this.container.style.width = '100%';
    this.container.style.height = '280px';
    this.container.style.background = '#060913';
    this.container.style.borderRadius = '8px';
    this.container.style.overflow = 'hidden';
    this.container.style.border = '1px solid #1e293b';
    this.container.style.boxShadow = 'inset 0 2px 8px rgba(0,0,0,0.6)';

    // Canvas element
    this.canvas = document.createElement('canvas');
    this.canvas.style.width = '100%';
    this.canvas.style.height = '100%';
    this.canvas.style.display = 'block';
    this.canvas.style.cursor = 'grab';
    this.container.appendChild(this.canvas);

    // Overlay Badge for Coordinates & Status
    this.coordBadge = document.createElement('div');
    this.coordBadge.className = 'threed-coord-badge';
    this.coordBadge.style.position = 'absolute';
    this.coordBadge.style.bottom = '8px';
    this.coordBadge.style.left = '8px';
    this.coordBadge.style.background = 'rgba(15, 23, 42, 0.85)';
    this.coordBadge.style.backdropFilter = 'blur(6px)';
    this.coordBadge.style.border = '1px solid rgba(255, 255, 255, 0.1)';
    this.coordBadge.style.borderRadius = '4px';
    this.coordBadge.style.padding = '3px 8px';
    this.coordBadge.style.fontSize = '10px';
    this.coordBadge.style.fontFamily = 'monospace';
    this.coordBadge.style.color = '#38bdf8';
    this.coordBadge.style.pointerEvents = 'none';
    this.coordBadge.style.zIndex = '5';
    this.coordBadge.innerText = 'Mars 3D Globe';
    this.container.appendChild(this.coordBadge);

    this.setupThree();
    this.bindEvents();
    this.render();
  }

  setupThree() {
    const THREE = window.THREE;
    const width = this.container.clientWidth || 280;
    const height = this.container.clientHeight || 280;

    if (THREE) {
      this.raycaster = new THREE.Raycaster();
      this.mouse = new THREE.Vector2();

      this.scene = new THREE.Scene();
      this.scene.background = new THREE.Color(0x060913);

      this.camera = new THREE.PerspectiveCamera(45, width / height, 0.1, 1000);
      this.camera.position.set(0, 0, 75);
      this.camera.lookAt(0, 0, 0);

      this.renderer = new THREE.WebGLRenderer({
        canvas: this.canvas,
        antialias: true,
        alpha: false,
        powerPreference: 'high-performance'
      });
      this.renderer.setSize(width, height);
      this.renderer.setPixelRatio(Math.min(window.devicePixelRatio || 1, 2));

      // Starfield background
      this.buildStarfield();

      // Lighting
      this.ambientLight = new THREE.AmbientLight(0xffffff, 0.28);
      this.scene.add(this.ambientLight);

      this.sunLight = new THREE.DirectionalLight(0xfff5e6, 1.4);
      this.sunLight.position.set(60, 40, 70);
      this.scene.add(this.sunLight);

      // Backlight rim illumination for dramatic limb contrast
      const rimLight = new THREE.DirectionalLight(0x38bdf8, 0.15);
      rimLight.position.set(-60, -30, -50);
      this.scene.add(rimLight);

      this.buildMesh();
    } else {
      // Fallback 2D canvas context if Three.js not yet loaded
      this.ctx = this.canvas.getContext('2d');
    }
  }

  buildStarfield() {
    const THREE = window.THREE;
    if (!THREE || !this.scene) return;

    const starCount = 300;
    const starGeo = new THREE.BufferGeometry();
    const starPos = new Float32Array(starCount * 3);

    for (let i = 0; i < starCount * 3; i += 3) {
      const r = 250 + Math.random() * 200;
      const theta = Math.random() * Math.PI * 2;
      const phi = Math.acos((Math.random() * 2) - 1);
      starPos[i] = r * Math.sin(phi) * Math.cos(theta);
      starPos[i + 1] = r * Math.sin(phi) * Math.sin(theta);
      starPos[i + 2] = r * Math.cos(phi);
    }

    starGeo.setAttribute('position', new THREE.BufferAttribute(starPos, 3));
    const starMat = new THREE.PointsMaterial({
      color: 0x94a3b8,
      size: 1.2,
      transparent: true,
      opacity: 0.6
    });

    const starField = new THREE.Points(starGeo, starMat);
    this.scene.add(starField);
  }

  buildMesh() {
    const THREE = window.THREE;
    if (!THREE || !this.scene) return;

    // Clean up existing meshes
    if (this.mesh) {
      this.scene.remove(this.mesh);
      this.mesh.geometry.dispose();
      if (this.mesh.material) {
        if (this.mesh.material.map) this.mesh.material.map.dispose();
        this.mesh.material.dispose();
      }
      this.mesh = null;
    }

    if (this.atmosphereMesh) {
      this.scene.remove(this.atmosphereMesh);
      this.atmosphereMesh.geometry.dispose();
      this.atmosphereMesh.material.dispose();
      this.atmosphereMesh = null;
    }

    if (this.graticuleMesh) {
      this.scene.remove(this.graticuleMesh);
      this.graticuleMesh = null;
    }

    if (this.targetMarker) {
      this.scene.remove(this.targetMarker);
      this.targetMarker = null;
    }

    if (this.mode === 'terrain') {
      // --- 3D TERRAIN HEIGHTFIELD MODE ---
      const segs = 64;
      const size = 60;
      const geometry = new THREE.PlaneGeometry(size, size, segs, segs);
      geometry.rotateX(-Math.PI / 2);

      const center = this.map ? this.map.getCenter() : { lat: 0, lng: 0 };
      const pos = geometry.attributes.position;

      for (let i = 0; i < pos.count; i++) {
        const x = pos.getX(i);
        const z = pos.getZ(i);
        const elev = ThreeDEngine.synthesizeTerrainElevation(x, z, center.lat, center.lng, this.exaggeration);
        pos.setY(i, elev);
      }

      geometry.computeVertexNormals();

      const material = new THREE.MeshStandardMaterial({
        color: this.body === 'moon' ? 0x94a3b8 : (this.body === 'earth' ? 0x15803d : 0xc26a3e),
        roughness: 0.8,
        metalness: 0.1,
        wireframe: this.wireframe,
        flatShading: true
      });

      this.mesh = new THREE.Mesh(geometry, material);
      this.scene.add(this.mesh);

    } else {
      // --- 3D HIGH-FIDELITY PLANETARY GLOBE MODE ---
      const radius = this.globeRadius;
      const geometry = new THREE.SphereGeometry(radius, 64, 64);

      // Generate ultra-high resolution procedural planetary texture
      const texCanvas = ThreeDEngine.generatePlanetaryTexture(this.body, 1024, 512);
      const texture = new THREE.CanvasTexture(texCanvas);
      texture.wrapS = THREE.ClampToEdgeWrapping;
      texture.wrapT = THREE.ClampToEdgeWrapping;
      texture.minFilter = THREE.LinearFilter;
      texture.magFilter = THREE.LinearFilter;

      const material = new THREE.MeshStandardMaterial({
        map: texture,
        roughness: 0.85,
        metalness: 0.05,
        wireframe: this.wireframe
      });

      this.mesh = new THREE.Mesh(geometry, material);
      this.mesh.rotation.x = this.rotation.x;
      this.mesh.rotation.y = this.rotation.y;
      this.scene.add(this.mesh);

      // Atmosphere Glow Shell
      if (this.showAtmosphere && this.body !== 'moon') {
        const atmosGeo = new THREE.SphereGeometry(radius * 1.035, 64, 64);
        const atmosColor = this.body === 'earth' ? 0x38bdf8 : 0xf97316;
        const atmosOpacity = this.body === 'earth' ? 0.24 : 0.18;

        const atmosMat = new THREE.MeshBasicMaterial({
          color: atmosColor,
          transparent: true,
          opacity: atmosOpacity,
          side: THREE.BackSide,
          blending: THREE.AdditiveBlending
        });

        this.atmosphereMesh = new THREE.Mesh(atmosGeo, atmosMat);
        this.scene.add(this.atmosphereMesh);
      }

      // Coordinate Graticule Overlay Lines
      if (this.showGraticule) {
        this.buildGraticule(radius * 1.002);
      }

      // Interactive Map Center Beacon Pin / Crosshair Ring
      this.buildTargetMarker(radius * 1.008);
    }
  }

  buildGraticule(radius) {
    const THREE = window.THREE;
    if (!THREE || !this.scene) return;

    this.graticuleMesh = new THREE.Group();

    // Equator line (Warm Gold)
    const eqGeo = new THREE.BufferGeometry();
    const eqPts = [];
    for (let i = 0; i <= 64; i++) {
      const theta = (i / 64) * Math.PI * 2;
      eqPts.push(new THREE.Vector3(radius * Math.cos(theta), 0, radius * Math.sin(theta)));
    }
    eqGeo.setFromPoints(eqPts);
    const eqMat = new THREE.LineBasicMaterial({ color: 0xfbbf24, transparent: true, opacity: 0.65 });
    const eqLine = new THREE.Line(eqGeo, eqMat);
    this.graticuleMesh.add(eqLine);

    // Prime Meridian line (Cyan)
    const pmGeo = new THREE.BufferGeometry();
    const pmPts = [];
    for (let i = 0; i <= 64; i++) {
      const phi = (i / 64) * Math.PI * 2;
      pmPts.push(new THREE.Vector3(0, radius * Math.cos(phi), radius * Math.sin(phi)));
    }
    pmGeo.setFromPoints(pmPts);
    const pmMat = new THREE.LineBasicMaterial({ color: 0x38bdf8, transparent: true, opacity: 0.65 });
    const pmLine = new THREE.Line(pmGeo, pmMat);
    this.graticuleMesh.add(pmLine);

    // 30° and 60° Latitude Parallels
    [-60, -30, 30, 60].forEach(lat => {
      const latRad = (lat * Math.PI) / 180;
      const rLat = radius * Math.cos(latRad);
      const yLat = radius * Math.sin(latRad);

      const latGeo = new THREE.BufferGeometry();
      const latPts = [];
      for (let i = 0; i <= 48; i++) {
        const theta = (i / 48) * Math.PI * 2;
        latPts.push(new THREE.Vector3(rLat * Math.cos(theta), yLat, rLat * Math.sin(theta)));
      }
      latGeo.setFromPoints(latPts);
      const latMat = new THREE.LineBasicMaterial({ color: 0x94a3b8, transparent: true, opacity: 0.25 });
      this.graticuleMesh.add(new THREE.Line(latGeo, latMat));
    });

    this.mesh.add(this.graticuleMesh);
  }

  buildTargetMarker(radius) {
    const THREE = window.THREE;
    if (!THREE || !this.mesh) return;

    if (this.targetMarker) {
      this.mesh.remove(this.targetMarker);
      this.targetMarker = null;
    }

    const center = this.map ? this.map.getCenter() : { lat: 0, lng: 0 };
    const pt = ThreeDEngine.convertLatLonToSpherePoint(center.lat, center.lng, radius);

    this.targetMarker = new THREE.Group();
    this.targetMarker.position.set(pt.x, pt.y, pt.z);

    // Orientation normal to surface
    const normal = new THREE.Vector3(pt.x, pt.y, pt.z).normalize();
    this.targetMarker.quaternion.setFromUnitVectors(new THREE.Vector3(0, 1, 0), normal);

    // Outer Target Pulse Ring
    const ringGeo = new THREE.RingGeometry(0.8, 1.1, 32);
    ringGeo.rotateX(Math.PI / 2);
    const ringMat = new THREE.MeshBasicMaterial({
      color: 0x38bdf8,
      side: THREE.DoubleSide,
      transparent: true,
      opacity: 0.85
    });
    const ringMesh = new THREE.Mesh(ringGeo, ringMat);
    this.targetMarker.add(ringMesh);

    // Center Crosshair Dot
    const dotGeo = new THREE.SphereGeometry(0.35, 16, 16);
    const dotMat = new THREE.MeshBasicMaterial({ color: 0xfacc15 });
    const dotMesh = new THREE.Mesh(dotGeo, dotMat);
    this.targetMarker.add(dotMesh);

    this.mesh.add(this.targetMarker);
  }

  updateTargetMarker() {
    if (!this.mesh || !this.targetMarker) return;
    const center = this.map ? this.map.getCenter() : { lat: 0, lng: 0 };
    const pt = ThreeDEngine.convertLatLonToSpherePoint(center.lat, center.lng, this.globeRadius * 1.008);
    this.targetMarker.position.set(pt.x, pt.y, pt.z);

    const normal = new THREE.Vector3(pt.x, pt.y, pt.z).normalize();
    this.targetMarker.quaternion.setFromUnitVectors(new THREE.Vector3(0, 1, 0), normal);
  }

  flyToMapCenter() {
    if (!this.map) return;
    const center = this.map.getCenter();
    const latRad = (center.lat * Math.PI) / 180;
    const lonRad = (center.lng * Math.PI) / 180;

    // Smoothly rotate globe to center the map coordinate
    this.targetRotation.x = -latRad;
    this.targetRotation.y = -lonRad - Math.PI / 2;
    this.autoSpin = false;
  }

  setMode(newMode) {
    this.mode = newMode;
    if (this.camera) {
      if (this.mode === 'terrain') {
        this.camera.position.set(0, 45, 70);
        this.camera.lookAt(0, 0, 0);
      } else {
        this.camera.position.set(0, 0, 75);
        this.camera.lookAt(0, 0, 0);
      }
    }
    this.buildMesh();
  }

  setBody(bodyName) {
    this.body = (bodyName || 'mars').toLowerCase();
    if (this.coordBadge) {
      const cap = this.body.charAt(0).toUpperCase() + this.body.slice(1);
      this.coordBadge.innerText = `${cap} 3D Globe`;
    }
    this.buildMesh();
  }

  setExaggeration(val) {
    this.exaggeration = Math.max(1, Math.min(25, val));
    if (this.mode === 'terrain') this.buildMesh();
  }

  setWireframe(enabled) {
    this.wireframe = !!enabled;
    if (this.mesh && this.mesh.material) {
      this.mesh.material.wireframe = this.wireframe;
    }
  }

  setAutoSpin(enabled) {
    this.autoSpin = !!enabled;
  }

  setAtmosphere(enabled) {
    this.showAtmosphere = !!enabled;
    if (this.mode === 'globe') this.buildMesh();
  }

  setGraticule(enabled) {
    this.showGraticule = !!enabled;
    if (this.mode === 'globe') this.buildMesh();
  }

  updateSunAngle(Ls, localHour = 12) {
    this.sunAngle = { Ls, hour: localHour };
    if (this.sunLight) {
      const angleRad = (localHour - 12) * (Math.PI / 12);
      const x = Math.sin(angleRad) * 80;
      const y = Math.cos(angleRad) * 40;
      const z = Math.max(20, Math.cos(angleRad) * 70);
      this.sunLight.position.set(x, y, z);
    }
  }

  bindEvents() {
    // Mouse Drag Rotation
    this.canvas.addEventListener('mousedown', (e) => {
      this.isDragging = true;
      this.autoSpin = false;
      this.canvas.style.cursor = 'grabbing';
      this.previousMousePosition = { x: e.clientX, y: e.clientY };
    });

    window.addEventListener('mousemove', (e) => {
      // Raycasting for coordinate tooltip
      if (this.mode === 'globe' && this.camera && this.mesh && this.raycaster) {
        const rect = this.canvas.getBoundingClientRect();
        if (e.clientX >= rect.left && e.clientX <= rect.right && e.clientY >= rect.top && e.clientY <= rect.bottom) {
          this.mouse.x = ((e.clientX - rect.left) / rect.width) * 2 - 1;
          this.mouse.y = -((e.clientY - rect.top) / rect.height) * 2 + 1;
          this.raycaster.setFromCamera(this.mouse, this.camera);
          const intersects = this.raycaster.intersectObject(this.mesh, false);

          if (intersects.length > 0) {
            const localPt = this.mesh.worldToLocal(intersects[0].point.clone());
            const coords = ThreeDEngine.convertSpherePointToLatLon(localPt, this.globeRadius);
            this.hoverCoords = coords;
            if (this.coordBadge) {
              const latDir = coords.lat >= 0 ? 'N' : 'S';
              const lonDir = coords.lon >= 0 ? 'E' : 'W';
              this.coordBadge.innerText = `${Math.abs(coords.lat).toFixed(2)}°${latDir}, ${Math.abs(coords.lon).toFixed(2)}°${lonDir}`;
            }
          }
        }
      }

      if (!this.isDragging) return;
      const deltaX = e.clientX - this.previousMousePosition.x;
      const deltaY = e.clientY - this.previousMousePosition.y;

      this.rotation.y += deltaX * 0.008;
      this.rotation.x += deltaY * 0.008;
      this.rotation.x = Math.max(-1.4, Math.min(1.4, this.rotation.x));
      this.targetRotation.x = this.rotation.x;
      this.targetRotation.y = this.rotation.y;

      this.previousMousePosition = { x: e.clientX, y: e.clientY };
    });

    window.addEventListener('mouseup', () => {
      this.isDragging = false;
      this.canvas.style.cursor = 'grab';
    });

    // Click on 3D Globe to Navigate 2D Map
    this.canvas.addEventListener('dblclick', (e) => {
      if (this.mode === 'globe' && this.hoverCoords && this.map) {
        this.map.flyTo([this.hoverCoords.lat, this.hoverCoords.lon], Math.max(this.map.getZoom(), 5), {
          duration: 1.2
        });
      }
    });

    // Touch support
    this.canvas.addEventListener('touchstart', (e) => {
      if (e.touches.length === 1) {
        this.isDragging = true;
        this.autoSpin = false;
        this.previousMousePosition = { x: e.touches[0].clientX, y: e.touches[0].clientY };
      }
    }, { passive: true });

    this.canvas.addEventListener('touchmove', (e) => {
      if (!this.isDragging || e.touches.length !== 1) return;
      const deltaX = e.touches[0].clientX - this.previousMousePosition.x;
      const deltaY = e.touches[0].clientY - this.previousMousePosition.y;

      this.rotation.y += deltaX * 0.01;
      this.rotation.x += deltaY * 0.01;
      this.rotation.x = Math.max(-1.4, Math.min(1.4, this.rotation.x));

      this.previousMousePosition = { x: e.touches[0].clientX, y: e.touches[0].clientY };
    }, { passive: true });

    this.canvas.addEventListener('touchend', () => {
      this.isDragging = false;
    });

    // Wheel Zoom
    this.canvas.addEventListener('wheel', (e) => {
      e.preventDefault();
      const delta = e.deltaY * 0.0015;
      this.zoom = Math.max(0.5, Math.min(2.5, this.zoom + delta));
    }, { passive: false });

    // Sync with global time changes
    EventBus.on(EVENTS.TIME_CHANGED, (detail) => {
      if (detail && typeof detail.Ls === 'number') {
        this.updateSunAngle(detail.Ls, detail.mtc || 12);
      }
    });

    // Sync with body changes
    EventBus.on(EVENTS.BODY_CHANGED, (detail) => {
      if (detail && detail.body) {
        this.setBody(detail.body);
      }
    });

    // Sync target marker with 2D map pan
    if (this.map) {
      this.map.on('move', () => {
        if (this.mode === 'globe') {
          this.updateTargetMarker();
        }
      });
      this.map.on('moveend', () => {
        if (this.mode === 'terrain') {
          this.buildMesh();
        }
      });
    }
  }

  render() {
    const THREE = window.THREE;
    if (THREE && this.renderer && this.scene && this.camera) {
      // Auto celestial rotation
      if (this.autoSpin && this.mode === 'globe' && !this.isDragging) {
        this.rotation.y += 0.0025;
      }

      if (this.mesh) {
        this.mesh.rotation.y = this.rotation.y;
        if (this.mode === 'terrain') {
          this.mesh.rotation.x = this.rotation.x;
        } else {
          this.mesh.rotation.x = this.rotation.x;
        }

        // Pulse the target center beacon
        if (this.targetMarker) {
          const pulse = 1.0 + 0.12 * Math.sin(Date.now() * 0.006);
          this.targetMarker.scale.set(pulse, pulse, pulse);
        }
      }

      if (this.mode === 'terrain') {
        this.camera.position.set(0, 45 * this.zoom, 70 * this.zoom);
      } else {
        this.camera.position.set(0, 0, 75 * this.zoom);
      }
      this.camera.lookAt(0, 0, 0);

      this.renderer.render(this.scene, this.camera);
    } else if (this.ctx) {
      // Fallback 2D rendering if Three.js not loaded
      const w = this.canvas.width;
      const h = this.canvas.height;
      this.ctx.fillStyle = '#060913';
      this.ctx.fillRect(0, 0, w, h);
      this.ctx.fillStyle = '#f97316';
      this.ctx.font = '12px sans-serif';
      this.ctx.textAlign = 'center';
      this.ctx.fillText('3D WebGL Globe & Terrain Loading...', w / 2, h / 2);
    }

    this.animId = requestAnimationFrame(() => this.render());
  }

  destroy() {
    if (this.animId) cancelAnimationFrame(this.animId);
  }
}
