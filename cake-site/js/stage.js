// 3D-сцена: рендерер, свет, камера, управление и выбор объектов мышью/тачем.

import * as THREE from 'three';
import { OrbitControls } from 'three/addons/controls/OrbitControls.js';
import { RoomEnvironment } from 'three/addons/environments/RoomEnvironment.js';
import { Cake } from './cake.js';
import { updateTweens } from './tween.js';

export class Stage {
  /**
   * @param {HTMLCanvasElement} canvas
   * @param {{ controls?: boolean, interactive?: boolean, autoRotate?: number,
   *           parallax?: boolean, offscreen?: {width:number,height:number},
   *           onPick?: Function, tooltip?: HTMLElement }} opts
   */
  constructor(canvas, opts = {}) {
    this.canvas = canvas;
    this.opts = opts;
    this.visible = true;
    this.pointer = new THREE.Vector2(0, 0);
    this.parallax = new THREE.Vector2(0, 0);

    const renderer = (this.renderer = new THREE.WebGLRenderer({
      canvas,
      antialias: true,
      alpha: true,
      preserveDrawingBuffer: !!opts.offscreen,
    }));
    renderer.setPixelRatio(opts.offscreen ? 1.5 : Math.min(window.devicePixelRatio || 1, 2));
    renderer.toneMapping = THREE.ACESFilmicToneMapping;
    renderer.toneMappingExposure = 0.92;
    renderer.shadowMap.enabled = true;
    renderer.shadowMap.type = THREE.PCFSoftShadowMap;

    const scene = (this.scene = new THREE.Scene());
    const pmrem = new THREE.PMREMGenerator(renderer);
    scene.environment = pmrem.fromScene(new RoomEnvironment(renderer), 0.04).texture;
    pmrem.dispose();

    scene.add(new THREE.HemisphereLight('#fff4ec', '#e7c3b5', 0.7));
    const key = new THREE.DirectionalLight('#fff3e6', 2.4);
    key.position.set(4, 8, 5);
    key.castShadow = true;
    key.shadow.mapSize.set(2048, 2048);
    key.shadow.camera.left = -5;
    key.shadow.camera.right = 5;
    key.shadow.camera.top = 5;
    key.shadow.camera.bottom = -5;
    key.shadow.camera.near = 1;
    key.shadow.camera.far = 25;
    key.shadow.bias = -0.0004;
    key.shadow.normalBias = 0.02;
    key.shadow.radius = 4;
    scene.add(key);
    const rim = new THREE.DirectionalLight('#ffc6dc', 1.4);
    rim.position.set(-5, 3, -4);
    scene.add(rim);

    const ground = new THREE.Mesh(new THREE.PlaneGeometry(30, 30), new THREE.ShadowMaterial({ opacity: 0.16 }));
    ground.rotation.x = -Math.PI / 2;
    ground.position.y = -0.63;
    ground.receiveShadow = true;
    scene.add(ground);

    this.camera = new THREE.PerspectiveCamera(30, 1, 0.1, 100);
    this.baseCam = new THREE.Vector3(0, 3.2, 9.2);
    this.camera.position.copy(this.baseCam);
    this.target = new THREE.Vector3(0, 0.75, 0);
    this.camera.lookAt(this.target);

    this.cake = new Cake();
    this.pivot = new THREE.Group();
    this.pivot.add(this.cake);
    scene.add(this.pivot);

    if (opts.controls) {
      const c = (this.controls = new OrbitControls(this.camera, canvas));
      c.target.copy(this.target);
      c.enableDamping = true;
      c.dampingFactor = 0.08;
      c.enablePan = false;
      c.minDistance = 4.5;
      c.maxDistance = 14;
      c.minPolarAngle = 0.25;
      c.maxPolarAngle = 1.52;
      c.rotateSpeed = 0.8;
      c.autoRotate = !!opts.autoRotate;
      c.autoRotateSpeed = opts.autoRotate || 0;
      c.enableZoom = opts.zoom !== false;
      let resume;
      c.addEventListener('start', () => {
        c.autoRotate = false;
        clearTimeout(resume);
      });
      c.addEventListener('end', () => {
        clearTimeout(resume);
        if (opts.autoRotate) resume = setTimeout(() => (c.autoRotate = true), 5000);
      });
      c.update();
    }

    if (opts.offscreen) {
      this.setSize(opts.offscreen.width, opts.offscreen.height);
      return;
    }

    this.raycaster = new THREE.Raycaster();
    this.bindPointer();

    const ro = new ResizeObserver(() => this.resize());
    ro.observe(canvas.parentElement);
    this.resize();

    const io = new IntersectionObserver(([entry]) => (this.visible = entry.isIntersecting), { rootMargin: '100px' });
    io.observe(canvas);

    this.clock = new THREE.Clock();
    this.loop = this.loop.bind(this);
    requestAnimationFrame(this.loop);
  }

  setSize(w, h) {
    this.renderer.setSize(w, h, false);
    this.camera.aspect = w / h;
    // на узких экранах отодвигаем камеру, чтобы торт помещался целиком
    const a = w / h;
    const fit = a < 1.25 ? Math.min(1.7, 1.25 / a) : 1;
    this.camera.position.copy(this.baseCam).multiplyScalar(fit);
    if (this.controls) this.controls.update();
    else this.camera.lookAt(this.target);
    this.camera.updateProjectionMatrix();
  }

  resize() {
    const el = this.canvas.parentElement;
    const w = el.clientWidth;
    const h = el.clientHeight;
    if (!w || !h) return;
    this.setSize(w, h);
  }

  bindPointer() {
    const el = this.canvas;
    let down = null;
    const ndc = (e) => {
      const r = el.getBoundingClientRect();
      return new THREE.Vector2(((e.clientX - r.left) / r.width) * 2 - 1, -((e.clientY - r.top) / r.height) * 2 + 1);
    };

    window.addEventListener(
      'pointermove',
      (e) => {
        const r = el.getBoundingClientRect();
        this.parallax.set(((e.clientX - r.left) / r.width) * 2 - 1, ((e.clientY - r.top) / r.height) * 2 - 1);
      },
      { passive: true },
    );

    if (!this.opts.interactive) return;

    el.addEventListener('pointerdown', (e) => (down = { x: e.clientX, y: e.clientY, t: performance.now() }));
    el.addEventListener('pointerup', (e) => {
      if (!down) return;
      const moved = Math.hypot(e.clientX - down.x, e.clientY - down.y);
      down = null;
      if (moved > 6) return;
      const hit = this.hitTest(ndc(e));
      if (!hit) return;
      const kind = this.cake.pick(hit.object);
      this.opts.onPick?.(kind, hit);
      this.updateHover(e);
    });
    el.addEventListener('pointermove', (e) => {
      if (e.pointerType !== 'mouse') return;
      this.updateHover(e);
    });
    el.addEventListener('pointerleave', () => this.showTip(null));
  }

  updateHover(e) {
    const r = this.canvas.getBoundingClientRect();
    const p = new THREE.Vector2(((e.clientX - r.left) / r.width) * 2 - 1, -((e.clientY - r.top) / r.height) * 2 + 1);
    const hit = this.hitTest(p);
    const tip = hit ? this.cake.hint(hit.object) : null;
    this.canvas.style.cursor = tip ? 'pointer' : 'grab';
    this.showTip(tip, e.clientX - r.left, e.clientY - r.top);
  }

  showTip(text, x, y) {
    const t = this.opts.tooltip;
    if (!t) return;
    if (!text) {
      t.classList.remove('show');
      return;
    }
    t.textContent = text;
    t.style.transform = `translate(${x + 16}px, ${y + 12}px)`;
    t.classList.add('show');
  }

  hitTest(ndc) {
    this.raycaster.setFromCamera(ndc, this.camera);
    const hits = this.raycaster.intersectObject(this.cake, true);
    return hits.find((h) => h.object.visible && this.cake.findInteractive(h.object)) || null;
  }

  loop() {
    requestAnimationFrame(this.loop);
    const dt = Math.min(0.05, this.clock.getDelta());
    const t = this.clock.elapsedTime;
    updateTweens();
    if (!this.visible || document.hidden) return;

    if (this.opts.parallax) {
      // лёгкий наклон за курсором + «парение»
      this.pivot.rotation.y += dt * 0.25;
      const tx = this.parallax.y * 0.08;
      const tz = -this.parallax.x * 0.06;
      this.pivot.rotation.x += (tx - this.pivot.rotation.x) * Math.min(1, dt * 3);
      this.pivot.rotation.z += (tz - this.pivot.rotation.z) * Math.min(1, dt * 3);
      this.pivot.position.y = Math.sin(t * 1.2) * 0.06;
    }
    this.controls?.update();
    this.cake.tick(dt, t);
    this.renderer.render(this.scene, this.camera);
  }

  /** Снимок сцены в data URL (рендерим и читаем в одном такте). */
  snapshot({ width = 360, type = 'image/jpeg', quality = 0.85, background = '#fff6ef' } = {}) {
    this.renderer.render(this.scene, this.camera);
    const src = this.renderer.domElement;
    const h = Math.round((width * src.height) / src.width);
    const c = document.createElement('canvas');
    c.width = width;
    c.height = h;
    const ctx = c.getContext('2d');
    if (background) {
      ctx.fillStyle = background;
      ctx.fillRect(0, 0, width, h);
    }
    ctx.drawImage(src, 0, 0, width, h);
    return c.toDataURL(type, quality);
  }

  dispose() {
    this.renderer.dispose();
    this.renderer.forceContextLoss();
  }
}
