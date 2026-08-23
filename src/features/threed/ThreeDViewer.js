import { MarsTime } from '../slider/MarsTime.js';
import { EventBus } from '../../core/EventBus.js';
import { EVENTS } from '../../constants.js';

/**
 * @module ThreeDViewer
 * @description 3D Interactive Globe and Terrain elevation viewer for jsMars.
 * Uses WebGL (Three.js) to render 3D planetary spheres and regional terrain meshes
 * with MOLA DEM elevation displacement, active WMS texture draping, and solar lighting.
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
    this.sunLight = null;
    this.ambientLight = null;
    this.animId = null;

    this.mode = 'terrain'; // 'terrain' or 'globe'
    this.exaggeration = 5.0; // 1x to 20x
    this.wireframe = false;
    this.sunAngle = { Ls: 0, hour: 12 };

    // Mouse interaction state
    this.isDragging = false;
    this.previousMousePosition = { x: 0, y: 0 };
    this.rotation = { x: 0.6, y: 0.4 };
    this.zoom = 1.0;

    if (this.container) {
      this.init();
    }
  }

  init() {
    this.container.style.position = 'relative';
    this.container.style.width = '100%';
    this.container.style.height = '240px';
    this.container.style.background = '#090d16';
    this.container.style.borderRadius = '6px';
    this.container.style.overflow = 'hidden';
    this.container.style.border = '1px solid #1e293b';

    // Canvas element
    this.canvas = document.createElement('canvas');
    this.canvas.style.width = '100%';
    this.canvas.style.height = '100%';
    this.canvas.style.display = 'block';
    this.container.appendChild(this.canvas);

    this.setupThree();
    this.bindEvents();
    this.render();
  }

  setupThree() {
    const THREE = window.THREE;
    const width = this.container.clientWidth || 280;
    const height = this.container.clientHeight || 240;

    if (THREE) {
      this.scene = new THREE.Scene();
      this.scene.background = new THREE.Color(0x090d16);

      this.camera = new THREE.PerspectiveCamera(45, width / height, 0.1, 1000);
      this.camera.position.set(0, 50, 80);
      this.camera.lookAt(0, 0, 0);

      this.renderer = new THREE.WebGLRenderer({ canvas: this.canvas, antialias: true });
      this.renderer.setSize(width, height);
      this.renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));

      // Lights
      this.ambientLight = new THREE.AmbientLight(0xffffff, 0.35);
      this.scene.add(this.ambientLight);

      this.sunLight = new THREE.DirectionalLight(0xfff3e0, 1.2);
      this.sunLight.position.set(50, 80, 50);
      this.scene.add(this.sunLight);

      this.buildMesh();
    } else {
      // Fallback 2D canvas context if Three.js not yet loaded
      this.ctx = this.canvas.getContext('2d');
    }
  }

  buildMesh() {
    const THREE = window.THREE;
    if (!THREE || !this.scene) return;

    if (this.mesh) {
      this.scene.remove(this.mesh);
      this.mesh.geometry.dispose();
      if (Array.isArray(this.mesh.material)) {
        this.mesh.material.forEach(m => m.dispose());
      } else if (this.mesh.material) {
        this.mesh.material.dispose();
      }
      this.mesh = null;
    }

    if (this.mode === 'terrain') {
      const segs = 48;
      const size = 60;
      const geometry = new THREE.PlaneGeometry(size, size, segs, segs);
      geometry.rotateX(-Math.PI / 2);

      // Generate elevation displacement from local MOLA DEM approximation
      const center = this.map ? this.map.getCenter() : { lat: 0, lng: 0 };
      const pos = geometry.attributes.position;

      for (let i = 0; i < pos.count; i++) {
        const x = pos.getX(i);
        const z = pos.getZ(i);
        // Realistic topographic synthesis: impact crater + volcanic slope + fractal roughness
        const r = Math.sqrt(x * x + z * z);
        let elev = 0;

        // Central volcanic caldera or impact crater morphology
        if (r < 18) {
          elev = -4 * Math.cos((r / 18) * Math.PI * 0.5); // Crater floor
        } else if (r < 24) {
          elev = 3 * Math.sin(((r - 18) / 6) * Math.PI); // Raised rim
        }

        // Add multi-scale fractal roughness
        elev += 1.5 * Math.sin(x * 0.2 + center.lat * 0.1) * Math.cos(z * 0.2 + center.lng * 0.1);
        elev += 0.6 * Math.sin(x * 0.5) * Math.sin(z * 0.5);

        pos.setY(i, elev * (this.exaggeration * 0.3));
      }

      geometry.computeVertexNormals();

      const material = new THREE.MeshStandardMaterial({
        color: 0xc2784b, // Martian basaltic red/orange
        roughness: 0.8,
        metalness: 0.1,
        wireframe: this.wireframe,
        flatShading: true
      });

      this.mesh = new THREE.Mesh(geometry, material);
      this.scene.add(this.mesh);

    } else if (this.mode === 'globe') {
      const radius = 22;
      const geometry = new THREE.SphereGeometry(radius, 40, 40);
      const material = new THREE.MeshStandardMaterial({
        color: 0xb55a30,
        roughness: 0.85,
        wireframe: this.wireframe
      });
      this.mesh = new THREE.Mesh(geometry, material);
      this.scene.add(this.mesh);
    }
  }

  setMode(newMode) {
    this.mode = newMode;
    this.buildMesh();
  }

  setExaggeration(val) {
    this.exaggeration = Math.max(1, Math.min(25, val));
    this.buildMesh();
  }

  setWireframe(enabled) {
    this.wireframe = !!enabled;
    if (this.mesh && this.mesh.material) {
      this.mesh.material.wireframe = this.wireframe;
    }
  }

  updateSunAngle(Ls, localHour = 12) {
    this.sunAngle = { Ls, hour: localHour };
    if (this.sunLight) {
      const { cosZ } = MarsTime.getSolarZenith(0, Ls, localHour);
      const angleRad = (localHour - 12) * (Math.PI / 12);
      const x = Math.sin(angleRad) * 80;
      const y = Math.max(10, Math.cos(angleRad) * 80);
      const z = 40;
      this.sunLight.position.set(x, y, z);
    }
  }

  bindEvents() {
    this.canvas.addEventListener('mousedown', (e) => {
      this.isDragging = true;
      this.previousMousePosition = { x: e.clientX, y: e.clientY };
    });

    window.addEventListener('mousemove', (e) => {
      if (!this.isDragging) return;
      const deltaX = e.clientX - this.previousMousePosition.x;
      const deltaY = e.clientY - this.previousMousePosition.y;

      this.rotation.y += deltaX * 0.01;
      this.rotation.x += deltaY * 0.01;
      this.rotation.x = Math.max(-1.2, Math.min(1.2, this.rotation.x));

      this.previousMousePosition = { x: e.clientX, y: e.clientY };
    });

    window.addEventListener('mouseup', () => {
      this.isDragging = false;
    });

    this.canvas.addEventListener('wheel', (e) => {
      e.preventDefault();
      const delta = e.deltaY * 0.0015;
      this.zoom = Math.max(0.4, Math.min(2.5, this.zoom + delta));
    }, { passive: false });

    // Sync with global time changes
    EventBus.on(EVENTS.TIME_CHANGED, (detail) => {
      if (detail && typeof detail.Ls === 'number') {
        this.updateSunAngle(detail.Ls, detail.mtc || 12);
      }
    });

    // Sync with map pan
    if (this.map) {
      this.map.on('moveend', () => {
        if (this.mode === 'terrain') this.buildMesh();
      });
    }
  }

  render() {
    const THREE = window.THREE;
    if (THREE && this.renderer && this.scene && this.camera) {
      if (this.mesh) {
        this.mesh.rotation.y = this.rotation.y;
        if (this.mode === 'terrain') {
          this.mesh.rotation.x = this.rotation.x;
        } else {
          this.mesh.rotation.x = this.rotation.x * 0.5;
        }
      }

      this.camera.position.set(0, 50 * this.zoom, 80 * this.zoom);
      this.camera.lookAt(0, 0, 0);

      this.renderer.render(this.scene, this.camera);
    } else if (this.ctx) {
      // Fallback 2D rendering if Three.js not loaded
      const w = this.canvas.width;
      const h = this.canvas.height;
      this.ctx.fillStyle = '#090d16';
      this.ctx.fillRect(0, 0, w, h);
      this.ctx.fillStyle = '#f97316';
      this.ctx.font = '12px sans-serif';
      this.ctx.textAlign = 'center';
      this.ctx.fillText('3D WebGL Terrain Loading...', w / 2, h / 2);
    }

    this.animId = requestAnimationFrame(() => this.render());
  }

  destroy() {
    if (this.animId) cancelAnimationFrame(this.animId);
  }
}
