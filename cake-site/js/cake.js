// Процедурная 3D-модель торта.
// Каждый ярус разрезан на «тело» и «кусочек» (сектор WEDGE), чтобы кусочек
// можно было вынуть и показать слои бисквита и крема на срезе.
// Угол φ отсчитывается так же, как в THREE.CylinderGeometry: x = r·sinφ, z = r·cosφ
// (φ = 0 смотрит на камеру).

import * as THREE from 'three';
import { tween, Ease, lerp } from './tween.js';

const TAU = Math.PI * 2;
export const WEDGE = Math.PI / 4.2;

/* ------------------------------------------------------------------ каталог опций */

export const SPONGES = {
  vanilla: { name: 'Ванильный', color: '#f2d79b', price: 0 },
  chocolate: { name: 'Шоколадный', color: '#5b3322', price: 300 },
  redvelvet: { name: 'Красный бархат', color: '#a3263c', price: 450 },
  matcha: { name: 'Матча', color: '#9fb86b', price: 500 },
  carrot: { name: 'Морковный', color: '#cf8a46', price: 250 },
};

export const FILLINGS = {
  vanilla: { name: 'Ванильный крем', color: '#fff4e2', price: 0 },
  strawberry: { name: 'Клубника', color: '#f3a2b4', price: 350 },
  chocolate: { name: 'Ганаш', color: '#6a3a25', price: 300 },
  pistachio: { name: 'Фисташка', color: '#cbdc9f', price: 550 },
  caramel: { name: 'Солёная карамель', color: '#e2ad6e', price: 400 },
  lavender: { name: 'Лаванда-черника', color: '#b9a3e0', price: 450 },
};

export const FROSTINGS = [
  { name: 'Сливочный', color: '#fff6ea' },
  { name: 'Пудровый розовый', color: '#f5bccd' },
  { name: 'Мятный', color: '#cfeedd' },
  { name: 'Небесный', color: '#cfe4f7' },
  { name: 'Лавандовый', color: '#e2d6f6' },
  { name: 'Лимонный', color: '#fbe9a6' },
  { name: 'Шоколадный', color: '#4a2b1d' },
  { name: 'Чёрный бархат', color: '#25202a' },
];

export const DRIPS = {
  none: { name: 'Без подтёков', color: null, price: 0 },
  chocolate: { name: 'Тёмный шоколад', color: '#3a1e12', price: 600 },
  white: { name: 'Белый шоколад', color: '#fff3e0', price: 600 },
  caramel: { name: 'Карамель', color: '#c8873a', price: 650 },
  berry: { name: 'Ягодная', color: '#b02350', price: 650 },
};

export const TOPPINGS = {
  rosettes: { name: 'Кремовые розочки', icon: '🌸', price: 400 },
  strawberries: { name: 'Клубника', icon: '🍓', price: 700 },
  blueberries: { name: 'Голубика', icon: '🫐', price: 600 },
  cherries: { name: 'Вишня', icon: '🍒', price: 650 },
  macarons: { name: 'Макаруны', icon: '🍪', price: 900 },
  sprinkles: { name: 'Посыпка', icon: '✨', price: 250 },
};

export function tierLayout(n) {
  if (n === 1) return [{ r: 1.3, h: 1.0 }];
  if (n === 2) return [{ r: 1.45, h: 0.95 }, { r: 0.98, h: 0.85 }];
  return [{ r: 1.55, h: 0.9 }, { r: 1.12, h: 0.8 }, { r: 0.74, h: 0.72 }];
}

/* ------------------------------------------------------------------ кэш ресурсов */

const geoCache = new Map();
const matCache = new Map();

function cachedGeo(key, make) {
  if (!geoCache.has(key)) {
    const g = make();
    g.userData.cached = true;
    geoCache.set(key, g);
  }
  return geoCache.get(key);
}

function cachedMat(key, make) {
  if (!matCache.has(key)) {
    const m = make();
    m.userData.cached = true;
    matCache.set(key, m);
  }
  return matCache.get(key);
}

function canvasTexture(w, h, draw) {
  const c = document.createElement('canvas');
  c.width = w;
  c.height = h;
  draw(c.getContext('2d'), w, h);
  const t = new THREE.CanvasTexture(c);
  t.colorSpace = THREE.SRGBColorSpace;
  t.anisotropy = 4;
  return t;
}

// Детерминированный псевдослучайный генератор — чтобы декор не «прыгал» при каждой пересборке.
function rng(seed) {
  let s = seed >>> 0 || 1;
  return () => {
    s ^= s << 13;
    s ^= s >>> 17;
    s ^= s << 5;
    return ((s >>> 0) % 100000) / 100000;
  };
}

/* ---------- геометрии декора ---------- */

// Кремовая «розочка» из звёздчатой насадки: закрученные рёбра, сужающиеся к кончику.
const rosetteGeo = () =>
  cachedGeo('rosette', () => {
    const g = new THREE.CylinderGeometry(1, 1, 1, 64, 16, false);
    const p = g.attributes.position;
    const v = new THREE.Vector3();
    for (let i = 0; i < p.count; i++) {
      v.fromBufferAttribute(p, i);
      const t = v.y + 0.5;
      const len = Math.hypot(v.x, v.z);
      const ang = Math.atan2(v.x, v.z);
      const profile = Math.pow(1 - Math.pow(t, 1.7), 0.75);
      const ridge = 1 + 0.2 * Math.cos(8 * ang + t * 7.0);
      const r = len * profile * ridge;
      const a = ang + t * 0.9;
      p.setXYZ(i, Math.sin(a) * r, v.y, Math.cos(a) * r);
    }
    g.translate(0, 0.5, 0);
    g.computeVertexNormals();
    return g;
  });

const sphereGeo = () => cachedGeo('sphere', () => new THREE.SphereGeometry(1, 24, 16));
const capsuleGeo = () => cachedGeo('capsule', () => new THREE.CapsuleGeometry(0.5, 1, 4, 10));
const cylGeo = () => cachedGeo('cyl', () => new THREE.CylinderGeometry(1, 1, 1, 20));

const strawberryGeo = () =>
  cachedGeo('strawberry', () => {
    const pts = [
      [0, -0.55], [0.1, -0.5], [0.22, -0.38], [0.33, -0.18], [0.4, 0.04],
      [0.41, 0.2], [0.35, 0.34], [0.22, 0.43], [0.0, 0.46],
    ].map(([x, y]) => new THREE.Vector2(x, y));
    return new THREE.LatheGeometry(pts, 32);
  });

const leafGeo = () =>
  cachedGeo('leaf', () => {
    const g = new THREE.ConeGeometry(0.11, 0.34, 4, 1);
    g.translate(0, 0.17, 0);
    g.scale(1, 1, 0.3);
    return g;
  });

const halfSphereGeo = () =>
  cachedGeo('halfsphere', () => new THREE.SphereGeometry(1, 28, 10, 0, TAU, 0, Math.PI / 2));

const stemGeo = () =>
  cachedGeo('stem', () => {
    const curve = new THREE.QuadraticBezierCurve3(
      new THREE.Vector3(0, 0.85, 0),
      new THREE.Vector3(0.05, 1.8, 0),
      new THREE.Vector3(0.55, 2.4, 0.1),
    );
    return new THREE.TubeGeometry(curve, 16, 0.07, 6, false);
  });

/* ---------- текстуры ---------- */

const strawberryMat = () =>
  cachedMat('strawberry', () => {
    const map = canvasTexture(256, 256, (ctx, w, h) => {
      const grd = ctx.createLinearGradient(0, 0, 0, h);
      grd.addColorStop(0, '#f04a5c');
      grd.addColorStop(0.6, '#d41b36');
      grd.addColorStop(1, '#a50f27');
      ctx.fillStyle = grd;
      ctx.fillRect(0, 0, w, h);
      for (let row = 0; row < 11; row++) {
        for (let col = 0; col < 14; col++) {
          const x = (col + (row % 2) * 0.5) * (w / 14);
          const y = 14 + row * (h / 11.5);
          ctx.fillStyle = 'rgba(90,0,10,0.45)';
          ctx.beginPath();
          ctx.ellipse(x, y + 1.5, 4.5, 6, 0, 0, TAU);
          ctx.fill();
          ctx.fillStyle = '#f6d878';
          ctx.beginPath();
          ctx.ellipse(x, y, 2, 3.4, 0, 0, TAU);
          ctx.fill();
        }
      }
    });
    return new THREE.MeshPhysicalMaterial({ map, roughness: 0.32, clearcoat: 0.8, clearcoatRoughness: 0.2 });
  });

const candleMat = (i) =>
  cachedMat('candle' + i, () => {
    const palettes = [
      ['#ffffff', '#ff6b9a'],
      ['#ffffff', '#5aa9ff'],
      ['#fff7d6', '#ffb020'],
      ['#ffffff', '#45c49a'],
      ['#ffffff', '#a37bff'],
    ];
    const [a, b] = palettes[i % palettes.length];
    const map = canvasTexture(128, 256, (ctx, w, h) => {
      ctx.fillStyle = a;
      ctx.fillRect(0, 0, w, h);
      ctx.strokeStyle = b;
      ctx.lineWidth = 18;
      for (let k = -4; k < 8; k++) {
        ctx.beginPath();
        ctx.moveTo(0, k * 48);
        ctx.lineTo(w, k * 48 + 64);
        ctx.stroke();
      }
    });
    return new THREE.MeshStandardMaterial({ map, roughness: 0.45 });
  });

const glowTexture = () =>
  cachedMat('glowtex', () => {
    // кэшируем через фиктивный материал, чтобы держать текстуру в одном месте
    const map = canvasTexture(128, 128, (ctx, w, h) => {
      const g = ctx.createRadialGradient(w / 2, h / 2, 0, w / 2, h / 2, w / 2);
      g.addColorStop(0, 'rgba(255,220,150,1)');
      g.addColorStop(0.25, 'rgba(255,170,60,0.55)');
      g.addColorStop(1, 'rgba(255,120,20,0)');
      ctx.fillStyle = g;
      ctx.fillRect(0, 0, w, h);
    });
    return new THREE.SpriteMaterial({ map, blending: THREE.AdditiveBlending, depthWrite: false, transparent: true });
  });

const smokeTexture = () =>
  cachedMat('smoketex', () => {
    const map = canvasTexture(128, 128, (ctx, w, h) => {
      const g = ctx.createRadialGradient(w / 2, h / 2, 0, w / 2, h / 2, w / 2);
      g.addColorStop(0, 'rgba(235,235,235,0.9)');
      g.addColorStop(0.5, 'rgba(200,200,200,0.35)');
      g.addColorStop(1, 'rgba(200,200,200,0)');
      ctx.fillStyle = g;
      ctx.fillRect(0, 0, w, h);
    });
    return new THREE.SpriteMaterial({ map, depthWrite: false, transparent: true });
  });

const stdMat = (key, params) => cachedMat(key, () => new THREE.MeshPhysicalMaterial(params));

const SPRINKLE_COLORS = ['#ff5c8a', '#ffd23f', '#3ec7ff', '#7bd389', '#b28dff', '#ffffff', '#ff8f3f'].map(
  (c) => new THREE.Color(c),
);
const MACARON_COLORS = ['#f7a8c4', '#b9e4c9', '#d3c2f2', '#fde49c', '#a9d6f5'];

/* ------------------------------------------------------------------ утилиты */

function inWedge(phi) {
  const p = ((phi % TAU) + TAU) % TAU;
  return p > 0.0001 && p < WEDGE - 0.0001;
}

function polar(r, phi, y) {
  return new THREE.Vector3(Math.sin(phi) * r, y, Math.cos(phi) * r);
}

function disposeTree(root) {
  root.traverse((o) => {
    if (o.isInstancedMesh) o.dispose();
    if (o.geometry && !o.geometry.userData.cached) o.geometry.dispose();
    const mats = Array.isArray(o.material) ? o.material : o.material ? [o.material] : [];
    for (const m of mats) {
      if (m.userData.cached || m.userData.persistent) continue;
      m.map?.dispose();
      m.dispose();
    }
  });
}

function shadowed(obj) {
  obj.traverse((o) => {
    if (o.isMesh) {
      o.castShadow = true;
      o.receiveShadow = true;
    }
  });
  return obj;
}

/* ------------------------------------------------------------------ Cake */

export class Cake extends THREE.Group {
  constructor() {
    super();
    this.cfg = null;
    this.tiers = [];
    this.candles = [];
    this.smoke = [];
    this.sliced = false;
    this.sliceAnim = null;
    this.onCandlesChange = null;

    this.cutCanvas = document.createElement('canvas');
    this.cutCanvas.width = 512;
    this.cutCanvas.height = 320;
    this.cutTex = new THREE.CanvasTexture(this.cutCanvas);
    this.cutTex.colorSpace = THREE.SRGBColorSpace;
    this.cutTex.anisotropy = 8;

    const persistent = (m) => {
      m.userData.persistent = true;
      return m;
    };
    this.mats = {
      frosting: persistent(
        new THREE.MeshPhysicalMaterial({
          roughness: 0.6,
          sheen: 0.25,
          sheenRoughness: 0.5,
          sheenColor: new THREE.Color('#ffffff'),
          clearcoat: 0.12,
          clearcoatRoughness: 0.6,
        }),
      ),
      sponge: persistent(new THREE.MeshStandardMaterial({ roughness: 0.95 })),
      cut: persistent(new THREE.MeshStandardMaterial({ map: this.cutTex, roughness: 0.9, side: THREE.DoubleSide })),
      drip: persistent(
        new THREE.MeshPhysicalMaterial({ roughness: 0.16, clearcoat: 1, clearcoatRoughness: 0.08, metalness: 0.0 }),
      ),
    };

    this.root = new THREE.Group();
    this.add(this.root);

    this.flameLight = new THREE.PointLight('#ffae55', 0, 5, 2);
    this.add(this.flameLight);
  }

  /* ---------- публичное API ---------- */

  get topY() {
    const t = this.tiers[this.tiers.length - 1];
    return t ? t.y + t.h : 1;
  }

  build(cfg, { animate = true } = {}) {
    this.cfg = structuredClone(cfg);
    this.sliceAnim?.stop();
    this.sliced = false;
    this.sliceT = 0;
    this.clearSmoke();

    disposeTree(this.root);
    this.root.clear();
    this.tiers = [];

    this.mats.frosting.color.set(cfg.frosting);
    this.mats.sponge.color.set(SPONGES[cfg.sponge].color);
    this.drawCutTexture();

    const layout = tierLayout(cfg.tiers);
    this.stand = this.makeStand(layout[0].r);
    this.root.add(this.stand);

    let y = 0;
    layout.forEach((dim, i) => {
      const tier = this.makeTier(dim, y, i);
      this.tiers.push(tier);
      this.root.add(tier.group);
      y += dim.h;
    });

    this.buildDecor({ animate, cascade: animate });

    if (animate) {
      this.tiers.forEach((tier, i) => {
        const from = tier.y + 3.2 + i * 0.7;
        tier.group.position.y = from;
        tween({
          duration: 1000,
          delay: 120 + i * 220,
          ease: Ease.outBounce,
          onUpdate: (e) => (tier.group.position.y = lerp(from, tier.y, e)),
        });
      });
      this.stand.scale.setScalar(0.001);
      tween({ duration: 700, ease: Ease.outBack, onUpdate: (e) => this.stand.scale.setScalar(Math.max(0.001, e)) });
    }
  }

  /** Плавно применяет новую конфигурацию, пересобирая только то, что изменилось. */
  update(cfg) {
    const prev = this.cfg;
    if (!prev || prev.tiers !== cfg.tiers) return this.build(cfg, { animate: true });
    this.cfg = structuredClone(cfg);

    let wobble = false;
    if (prev.frosting !== cfg.frosting) {
      const from = this.mats.frosting.color.clone();
      const to = new THREE.Color(cfg.frosting);
      tween({
        duration: 650,
        ease: Ease.inOutCubic,
        onUpdate: (e) => this.mats.frosting.color.copy(from).lerp(to, e),
        onComplete: () => this.drawCutTexture(),
      });
      wobble = true;
    }
    if (prev.sponge !== cfg.sponge || prev.filling !== cfg.filling) {
      this.mats.sponge.color.set(SPONGES[cfg.sponge].color);
      this.drawCutTexture();
      wobble = true;
      if (!this.sliced) this.toggleSlice(); // показываем новый срез
    }
    const decorKey = (c) => JSON.stringify([c.toppings, c.candles, c.text, c.drip]);
    if (decorKey(prev) !== decorKey(cfg)) {
      const keepSliced = this.sliced;
      this.resetSliceInstant();
      this.buildDecor({ animate: true });
      if (keepSliced) this.toggleSlice();
    }
    if (wobble) this.wobble();
  }

  wobble() {
    tween({
      duration: 900,
      ease: Ease.linear,
      onUpdate: (_, t) => {
        const k = Math.sin(t * Math.PI * 4) * (1 - t) * 0.06;
        this.root.scale.set(1 - k * 0.5, 1 + k, 1 - k * 0.5);
      },
    });
  }

  /** Обработка клика по объекту сцены. Возвращает тип объекта. */
  pick(object) {
    const target = this.findInteractive(object);
    if (!target) return null;
    const kind = target.userData.kind;
    if (kind === 'candle') target.userData.lit ? this.blowCandle(target) : this.lightCandle(target);
    else if (kind === 'cake') this.toggleSlice();
    else if (kind === 'stand') this.spin();
    else this.bounce(target);
    return kind;
  }

  findInteractive(object) {
    let o = object;
    while (o && o !== this) {
      if (o.userData.kind) return o;
      o = o.parent;
    }
    return null;
  }

  hint(object) {
    const t = this.findInteractive(object);
    if (!t) return null;
    switch (t.userData.kind) {
      case 'candle':
        return t.userData.lit ? 'Задуть свечу' : 'Зажечь свечу';
      case 'cake':
        return this.sliced ? 'Вернуть кусочек' : 'Отрезать кусочек';
      case 'stand':
        return 'Покрутить торт';
      case 'topper':
        return 'Ваша надпись';
      default:
        return 'Подбросить';
    }
  }

  blowAll() {
    this.candles.filter((c) => c.userData.lit).forEach((c, i) => setTimeout(() => this.blowCandle(c), i * 70));
  }

  lightAll() {
    this.candles.filter((c) => !c.userData.lit).forEach((c, i) => setTimeout(() => this.lightCandle(c), i * 120));
  }

  get litCount() {
    return this.candles.filter((c) => c.userData.lit).length;
  }

  toggleSlice() {
    if (!this.tiers.length) return;
    this.sliceAnim?.stop();
    this.sliced = !this.sliced;
    const target = this.sliced ? 1 : 0;
    const start = this.sliceT ?? 0;
    const dur = this.sliced ? 1500 : 1100;
    this.sliceAnim = tween({
      duration: dur * Math.abs(target - start) + 1,
      ease: Ease.inOutCubic,
      onUpdate: (e) => this.applySlice(lerp(start, target, e)),
    });
  }

  spin() {
    const from = this.root.rotation.y;
    tween({ duration: 1400, ease: Ease.outCubic, onUpdate: (e) => (this.root.rotation.y = from + e * TAU) });
  }

  /* ---------- анимация кадра ---------- */

  tick(dt, t) {
    let lit = 0;
    for (const c of this.candles) {
      const f = c.userData.flame;
      if (!c.userData.lit) continue;
      lit++;
      const s = c.userData.seed;
      const n = Math.sin(t * 17 + s) * 0.5 + Math.sin(t * 29 + s * 2.3) * 0.35 + Math.sin(t * 7 + s * 5) * 0.4;
      const k = c.userData.flameScale;
      f.scale.set(k * (1 - n * 0.04), k * (1 + n * 0.12), k * (1 - n * 0.04));
      f.rotation.z = Math.sin(t * 3 + s) * 0.06;
    }
    const top = this.topY;
    this.flameLight.position.set(0, top + 0.75, 0.3);
    const target = lit ? 0.6 + lit * 0.35 + Math.sin(t * 23) * 0.08 : 0;
    this.flameLight.intensity += (target - this.flameLight.intensity) * Math.min(1, dt * 10);

    for (let i = this.smoke.length - 1; i >= 0; i--) {
      const p = this.smoke[i];
      p.life += dt;
      const k = p.life / p.maxLife;
      if (k >= 1) {
        p.sprite.parent?.remove(p.sprite);
        p.sprite.material.dispose();
        this.smoke.splice(i, 1);
        continue;
      }
      p.sprite.position.y += dt * (0.45 + p.drift * 0.2);
      p.sprite.position.x += Math.sin(p.life * 3 + p.drift * 6) * dt * 0.08;
      const s = 0.06 + k * 0.32;
      p.sprite.scale.set(s, s, s);
      p.sprite.material.opacity = (1 - k) * 0.55 * Math.min(1, p.life * 6);
    }
  }

  /* ---------- построение ---------- */

  drawCutTexture() {
    const ctx = this.cutCanvas.getContext('2d');
    const { width: w, height: h } = this.cutCanvas;
    const sponge = SPONGES[this.cfg.sponge].color;
    const filling = FILLINGS[this.cfg.filling].color;
    const frosting = this.cfg.frosting;
    const random = rng(7);

    // Слои снизу вверх (доли высоты): бисквит, крем, бисквит, крем, бисквит, верхний слой крема
    const layers = [
      ['s', 0.26], ['f', 0.085], ['s', 0.24], ['f', 0.085], ['s', 0.24], ['top', 0.09],
    ];
    let y = h;
    for (const [type, frac] of layers) {
      const lh = frac * h;
      y -= lh;
      const color = type === 's' ? sponge : type === 'f' ? filling : frosting;
      ctx.fillStyle = color;
      ctx.fillRect(0, y, w, lh + 1);
      if (type === 's') {
        // «пористость» бисквита
        for (let k = 0; k < 900; k++) {
          const px = random() * w;
          const py = y + random() * lh;
          const r = 0.8 + random() * 2.6;
          ctx.fillStyle = random() > 0.5 ? 'rgba(0,0,0,0.10)' : 'rgba(255,255,255,0.12)';
          ctx.beginPath();
          ctx.arc(px, py, r, 0, TAU);
          ctx.fill();
        }
        const g = ctx.createLinearGradient(0, y, 0, y + lh);
        g.addColorStop(0, 'rgba(0,0,0,0.10)');
        g.addColorStop(0.15, 'rgba(0,0,0,0)');
        g.addColorStop(0.85, 'rgba(0,0,0,0)');
        g.addColorStop(1, 'rgba(0,0,0,0.12)');
        ctx.fillStyle = g;
        ctx.fillRect(0, y, w, lh);
      } else {
        const g = ctx.createLinearGradient(0, y, 0, y + lh);
        g.addColorStop(0, 'rgba(255,255,255,0.25)');
        g.addColorStop(1, 'rgba(0,0,0,0.06)');
        ctx.fillStyle = g;
        ctx.fillRect(0, y, w, lh);
        if (this.cfg.filling === 'strawberry' && type === 'f') {
          for (let k = 0; k < 14; k++) {
            ctx.fillStyle = 'rgba(200,20,60,0.65)';
            ctx.beginPath();
            ctx.ellipse(random() * w * 0.92, y + lh * (0.3 + random() * 0.4), 6 + random() * 8, lh * 0.25, 0, 0, TAU);
            ctx.fill();
          }
        }
      }
    }
    // Внешний слой крема (обмазка) у края среза
    ctx.fillStyle = frosting;
    ctx.fillRect(w * 0.955, 0, w * 0.045, h);
    this.cutTex.needsUpdate = true;
  }

  makeStand(R) {
    const g = new THREE.Group();
    const porcelain = stdMat('porcelain', {
      color: '#fbfaf8',
      roughness: 0.22,
      clearcoat: 1,
      clearcoatRoughness: 0.1,
    });
    const gold = stdMat('gold', { color: '#d9b26a', metalness: 1, roughness: 0.28 });

    const plateR = R + 0.38;
    const plate = new THREE.Mesh(new THREE.CylinderGeometry(plateR, plateR - 0.08, 0.08, 96), porcelain);
    plate.position.y = -0.04;
    const rim = new THREE.Mesh(new THREE.TorusGeometry(plateR, 0.022, 10, 120), gold);
    rim.rotation.x = Math.PI / 2;
    rim.position.y = 0;
    const stem = new THREE.Mesh(new THREE.CylinderGeometry(0.16, 0.26, 0.48, 40), porcelain);
    stem.position.y = -0.32;
    const ring = new THREE.Mesh(new THREE.TorusGeometry(0.2, 0.025, 10, 60), gold);
    ring.rotation.x = Math.PI / 2;
    ring.position.y = -0.2;
    const foot = new THREE.Mesh(new THREE.CylinderGeometry(0.62, 0.72, 0.07, 64), porcelain);
    foot.position.y = -0.595;
    g.add(plate, rim, stem, ring, foot);
    g.userData.kind = 'stand';
    return shadowed(g);
  }

  makeTier(dim, y, index) {
    const group = new THREE.Group();
    group.position.y = y;
    const body = new THREE.Group();
    const wedge = new THREE.Group();
    body.add(this.makeTierPiece(dim, WEDGE, TAU - WEDGE));
    wedge.add(this.makeTierPiece(dim, 0, WEDGE));
    const bodyDecor = new THREE.Group();
    const wedgeDecor = new THREE.Group();
    body.add(bodyDecor);
    wedge.add(wedgeDecor);
    group.add(body, wedge);
    return { group, body, wedge, bodyDecor, wedgeDecor, r: dim.r, h: dim.h, y, index };
  }

  makeTierPiece({ r, h }, start, len) {
    const g = new THREE.Group();
    const segs = Math.max(16, Math.round((128 * len) / TAU));
    const cyl = new THREE.CylinderGeometry(r, r, h, segs, 1, false, start, len);
    cyl.translate(0, h / 2, 0);
    const m = new THREE.Mesh(cyl, [this.mats.frosting, this.mats.frosting, this.mats.sponge]);
    g.add(m);
    for (const a of [start, start + len]) {
      const pg = new THREE.PlaneGeometry(r, h);
      pg.translate(r / 2, h / 2, 0);
      pg.rotateY(a - Math.PI / 2);
      g.add(new THREE.Mesh(pg, this.mats.cut));
    }
    g.userData.kind = 'cake';
    return shadowed(g);
  }

  /** Раскладывает инстансы по «телу» и «кусочку» яруса в зависимости от угла. */
  addInstanced(tier, geo, mat, items, { color = false } = {}) {
    const split = { body: [], wedge: [] };
    for (const it of items) split[inWedge(Math.atan2(it.pos.x, it.pos.z)) ? 'wedge' : 'body'].push(it);
    const m4 = new THREE.Matrix4();
    const q = new THREE.Quaternion();
    for (const key of ['body', 'wedge']) {
      const arr = split[key];
      if (!arr.length) continue;
      const im = new THREE.InstancedMesh(geo, mat, arr.length);
      arr.forEach((it, k) => {
        q.setFromEuler(it.rot ?? new THREE.Euler());
        m4.compose(it.pos, q, it.scale);
        im.setMatrixAt(k, m4);
        if (color) im.setColorAt(k, it.color);
      });
      im.instanceMatrix.needsUpdate = true;
      if (im.instanceColor) im.instanceColor.needsUpdate = true;
      im.computeBoundingSphere();
      im.castShadow = true;
      im.receiveShadow = true;
      im.userData.kind = 'cake';
      tier[key + 'Decor'].add(im);
    }
  }

  place(tier, obj, phi) {
    (inWedge(phi) ? tier.wedgeDecor : tier.bodyDecor).add(obj);
    return obj;
  }

  buildDecor({ animate = true, cascade = false } = {}) {
    const cfg = this.cfg;
    this.candles = [];
    this.flameLight.intensity = 0;
    for (const tier of this.tiers) {
      disposeTree(tier.bodyDecor);
      disposeTree(tier.wedgeDecor);
      tier.bodyDecor.clear();
      tier.wedgeDecor.clear();
    }

    const random = rng(42 + cfg.tiers * 13);
    const drip = DRIPS[cfg.drip];
    if (drip.color) this.mats.drip.color.set(drip.color);
    const n = this.tiers.length;
    const pops = [];

    this.tiers.forEach((tier, i) => {
      const { r, h } = tier;
      const isTop = i === n - 1;
      const upperR = isTop ? 0 : this.tiers[i + 1].r;
      const dripH = drip.color ? 0.024 : 0;
      const yTop = h + dripH;

      // Глазурь с подтёками
      if (drip.color) {
        for (const [s, l] of [[WEDGE, TAU - WEDGE], [0, WEDGE]]) {
          const geo = new THREE.CylinderGeometry(r + 0.014, r + 0.014, 0.036, Math.max(16, Math.round((128 * l) / TAU)), 1, false, s, l);
          geo.translate(0, h + dripH - 0.018 + 0.004, 0);
          const mesh = new THREE.Mesh(geo, this.mats.drip);
          mesh.castShadow = true;
          mesh.userData.kind = 'cake';
          (s === 0 ? tier.wedgeDecor : tier.bodyDecor).add(mesh);
        }
        const count = Math.round((TAU * r) / 0.105);
        const items = [];
        for (let k = 0; k < count; k++) {
          const phi = (k / count) * TAU + random() * 0.03;
          const L = 0.1 + Math.pow(random(), 2.2) * h * 0.45;
          const w = 0.085 + random() * 0.035;
          items.push({
            pos: polar(r + 0.006, phi, h + 0.01 - L / 2),
            rot: new THREE.Euler(0, phi, 0),
            scale: new THREE.Vector3(w, L / 2, w * 0.55),
          });
        }
        this.addInstanced(tier, capsuleGeo(), this.mats.drip, items);
      }

      // Бордюр из «жемчужин» по низу яруса — всегда
      {
        const count = Math.round((TAU * r) / 0.085);
        const items = [];
        for (let k = 0; k < count; k++) {
          const phi = (k / count) * TAU;
          items.push({ pos: polar(r + 0.005, phi, 0.04), scale: new THREE.Vector3(0.045, 0.042, 0.045) });
        }
        this.addInstanced(tier, sphereGeo(), this.mats.frosting, items);
      }

      // Кремовые розочки по верхнему краю
      if (cfg.toppings.rosettes) {
        const rr = r - 0.085;
        const count = Math.round((TAU * rr) / 0.17);
        const items = [];
        for (let k = 0; k < count; k++) {
          const phi = (k / count) * TAU + 0.05;
          items.push({
            pos: polar(rr, phi, yTop - 0.01),
            rot: new THREE.Euler(0, phi + k, 0),
            scale: new THREE.Vector3(0.078, 0.14, 0.078),
          });
        }
        this.addInstanced(tier, rosetteGeo(), this.mats.frosting, items);
      }

      // Посыпка
      if (cfg.toppings.sprinkles) {
        const inner = isTop ? 0 : upperR + 0.02;
        const outer = r - 0.05;
        const area = Math.PI * (outer * outer - inner * inner);
        const count = Math.round(area * 260);
        const items = [];
        for (let k = 0; k < count; k++) {
          const rr = Math.sqrt(inner * inner + random() * (outer * outer - inner * inner));
          const phi = random() * TAU;
          items.push({
            pos: polar(rr, phi, yTop + 0.008),
            rot: new THREE.Euler(Math.PI / 2, 0, random() * TAU),
            scale: new THREE.Vector3(0.018, 0.028, 0.018),
            color: SPRINKLE_COLORS[Math.floor(random() * SPRINKLE_COLORS.length)],
          });
        }
        this.addInstanced(tier, capsuleGeo(), stdMat('sprinkle', { color: '#ffffff', roughness: 0.4, clearcoat: 0.6 }), items, {
          color: true,
        });
      }

      // Ягоды и макаруны — по кольцу на верхнем ярусе и на «ступеньках» нижних
      const kinds = ['strawberries', 'macarons', 'blueberries', 'cherries'].filter((k) => cfg.toppings[k]);
      if (kinds.length) {
        const ringR = isTop ? r * 0.64 : (r + upperR) / 2 + 0.02;
        const spacing = isTop ? 0.27 : 0.3;
        const slots = Math.max(5, Math.round((TAU * ringR) / spacing));
        for (let k = 0; k < slots; k++) {
          const phi = (k / slots) * TAU + 0.12;
          const kind = kinds[k % kinds.length];
          const obj = this.makeFruit(kind, random, phi);
          obj.position.copy(polar(ringR, phi, yTop));
          obj.userData.baseY = yTop;
          this.place(tier, obj, phi);
          pops.push(obj);
        }
      }

      // Верхний ярус: свечи и топпер с надписью
      if (isTop) {
        const hasText = cfg.text && cfg.text.trim().length > 0;
        if (hasText) {
          const topper = this.makeTopper(cfg.text.trim(), r, yTop);
          this.place(tier, topper, Math.PI);
          pops.push(topper);
        }
        const count = cfg.candles;
        const cr = count === 1 ? 0 : Math.max(0.18, r * 0.36);
        const arc = hasText ? Math.PI * 1.15 : TAU;
        for (let k = 0; k < count; k++) {
          let phi;
          if (count === 1) phi = 0.6;
          else if (hasText) phi = -arc / 2 + (arc * (k + 0.5)) / count;
          else phi = (k / count) * TAU + 0.3;
          const c = this.makeCandle(k, random);
          c.position.copy(polar(cr, phi, yTop - 0.04));
          c.userData.baseY = yTop - 0.04;
          this.place(tier, c, phi);
          this.candles.push(c);
          pops.push(c);
        }
      }
    });

    if (animate) {
      this.tiers.forEach((tier, i) => {
        for (const d of [tier.bodyDecor, tier.wedgeDecor]) {
          d.position.y = 0.9;
          tween({
            duration: 800,
            delay: (cascade ? 900 + i * 220 : 0) + i * 90,
            ease: Ease.outBounce,
            onUpdate: (e) => (d.position.y = lerp(0.9, 0, e)),
          });
        }
      });
      pops.forEach((o, k) => {
        const s = o.scale.x;
        o.scale.setScalar(0.0001);
        tween({
          duration: 600,
          delay: (cascade ? 1300 : 250) + k * 25,
          ease: Ease.outBack,
          onUpdate: (e) => o.scale.setScalar(Math.max(0.0001, s * e)),
        });
      });
    }
    this.onCandlesChange?.(this.litCount, this.candles.length);
  }

  makeFruit(kind, random, phi) {
    const g = new THREE.Group();
    g.userData.kind = kind;
    if (kind === 'strawberries') {
      const berry = new THREE.Mesh(strawberryGeo(), strawberryMat());
      berry.rotation.x = Math.PI;
      berry.position.y = 0.48;
      g.add(berry);
      const leafMat = stdMat('leaf', { color: '#3f8f3a', roughness: 0.5 });
      for (let k = 0; k < 6; k++) {
        const leaf = new THREE.Mesh(leafGeo(), leafMat);
        leaf.position.y = 0.08;
        leaf.rotation.set(0, (k / 6) * TAU, 0);
        leaf.rotateX(1.95);
        g.add(leaf);
      }
      g.scale.setScalar(0.2 + random() * 0.03);
      g.rotation.set(0, phi, 0);
      g.rotateX(0.35);
    } else if (kind === 'blueberries') {
      const mat = stdMat('blueberry', {
        color: '#2f3a6b',
        roughness: 0.5,
        sheen: 1,
        sheenColor: new THREE.Color('#9fb0e6'),
        sheenRoughness: 0.4,
      });
      const offs = [[0, 0], [0.11, 0.06], [0.02, 0.12], [-0.1, 0.07]];
      for (const [x, z] of offs) {
        const b = new THREE.Mesh(sphereGeo(), mat);
        b.scale.setScalar(0.055 + random() * 0.01);
        b.position.set(x - 0.01, 0.05, z - 0.06);
        g.add(b);
      }
      const top = new THREE.Mesh(sphereGeo(), mat);
      top.scale.setScalar(0.055);
      top.position.set(0.01, 0.13, 0.02);
      g.add(top);
      g.rotation.y = phi;
    } else if (kind === 'cherries') {
      const cherry = new THREE.Mesh(
        sphereGeo(),
        stdMat('cherry', { color: '#7d0a1c', roughness: 0.15, clearcoat: 1, clearcoatRoughness: 0.05 }),
      );
      cherry.scale.set(1, 0.92, 1);
      cherry.position.y = 0.9;
      const stem = new THREE.Mesh(stemGeo(), stdMat('stem', { color: '#6b5a2a', roughness: 0.7 }));
      g.add(cherry, stem);
      g.scale.setScalar(0.09);
      g.rotation.y = phi + random();
    } else if (kind === 'macarons') {
      const color = MACARON_COLORS[Math.floor(random() * MACARON_COLORS.length)];
      const shell = stdMat('mac' + color, { color, roughness: 0.75, sheen: 0.4, sheenColor: new THREE.Color('#ffffff') });
      const fill = stdMat('macfill', { color: '#fff7ee', roughness: 0.6 });
      const top = new THREE.Mesh(halfSphereGeo(), shell);
      top.scale.set(1, 0.42, 1);
      top.position.y = 0.17;
      const bottom = top.clone();
      bottom.rotation.x = Math.PI;
      bottom.position.y = -0.17;
      const filling = new THREE.Mesh(cylGeo(), fill);
      filling.scale.set(0.9, 0.34, 0.9);
      const inner = new THREE.Group();
      inner.add(top, bottom, filling);
      inner.rotation.x = -1.05; // стоит «на ребре», наклонён наружу
      inner.position.y = 0.72;
      g.add(inner);
      g.scale.setScalar(0.15);
      g.rotation.y = phi;
    }
    return shadowed(g);
  }

  makeCandle(i, random) {
    const g = new THREE.Group();
    g.userData.kind = 'candle';
    g.userData.lit = true;
    g.userData.seed = random() * 100;
    const height = 0.38 + random() * 0.06;
    const body = new THREE.Mesh(cylGeo(), candleMat(i));
    body.scale.set(0.028, height, 0.028);
    body.position.y = height / 2;
    const wick = new THREE.Mesh(cylGeo(), stdMat('wick', { color: '#2a2018', roughness: 1 }));
    wick.scale.set(0.005, 0.05, 0.005);
    wick.position.y = height + 0.025;
    g.add(shadowed(body), wick);

    const flame = new THREE.Group();
    flame.position.y = height + 0.05;
    const outer = new THREE.Mesh(
      sphereGeo(),
      cachedMat('flameOuter', () => new THREE.MeshBasicMaterial({ color: '#ffae3d', transparent: true, opacity: 0.92 })),
    );
    outer.scale.set(0.03, 0.07, 0.03);
    outer.position.y = 0.045;
    const inner = new THREE.Mesh(sphereGeo(), cachedMat('flameInner', () => new THREE.MeshBasicMaterial({ color: '#fff6d8' })));
    inner.scale.set(0.014, 0.035, 0.014);
    inner.position.y = 0.03;
    const glow = new THREE.Sprite(glowTexture());
    glow.scale.setScalar(0.34);
    glow.position.y = 0.05;
    flame.add(outer, inner, glow);
    g.add(flame);
    g.userData.flame = flame;
    g.userData.flameScale = 1;
    return g;
  }

  makeTopper(text, r, yTop) {
    const g = new THREE.Group();
    g.userData.kind = 'topper';
    const w = Math.min(1.1, Math.max(0.8, r * 1.3));
    const hh = 0.4;
    const tex = canvasTexture(1024, Math.round((1024 * hh) / w), (ctx, W, H) => {
      const grd = ctx.createLinearGradient(0, 0, 0, H);
      grd.addColorStop(0, '#fff6ea');
      grd.addColorStop(1, '#f6e3cc');
      ctx.fillStyle = grd;
      ctx.fillRect(0, 0, W, H);
      ctx.strokeStyle = '#d4a85c';
      ctx.lineWidth = 10;
      ctx.strokeRect(22, 22, W - 44, H - 44);
      ctx.fillStyle = '#3b1d12';
      ctx.textAlign = 'center';
      ctx.textBaseline = 'middle';
      const fit = (lines) => {
        for (let size = 210; size >= 40; size -= 4) {
          ctx.font = `italic 700 ${size}px "Playfair Display", Georgia, serif`;
          const ok = lines.every((l) => ctx.measureText(l).width < W * 0.84) && size * 1.15 * lines.length < H * 0.78;
          if (ok) return size;
        }
        return 40;
      };
      let lines = [text];
      let size = fit(lines);
      if (size < 90 && text.includes(' ')) {
        const mid = text.length / 2;
        let best = -1;
        for (let k = 0; k < text.length; k++) if (text[k] === ' ' && (best < 0 || Math.abs(k - mid) < Math.abs(best - mid))) best = k;
        const two = [text.slice(0, best), text.slice(best + 1)];
        const size2 = fit(two);
        if (size2 > size) {
          lines = two;
          size = size2;
        }
      }
      ctx.font = `italic 700 ${size}px "Playfair Display", Georgia, serif`;
      const lh = size * 1.12;
      lines.forEach((l, k) => ctx.fillText(l, W / 2, H / 2 + (k - (lines.length - 1) / 2) * lh + size * 0.04));
    });
    const front = new THREE.MeshStandardMaterial({ map: tex, roughness: 0.5 });
    const side = stdMat('whitechoc', { color: '#f6e3cc', roughness: 0.5 });
    const board = new THREE.Mesh(new THREE.BoxGeometry(w, hh, 0.035), [side, side, side, side, front, side]);
    const boardY = 0.56 + hh / 2;
    board.position.y = boardY;
    const gold = stdMat('gold', {});
    for (const x of [-w * 0.3, w * 0.3]) {
      const stick = new THREE.Mesh(cylGeo(), gold);
      stick.scale.set(0.012, 0.66, 0.012);
      stick.position.set(x, 0.29, -0.005);
      g.add(stick);
    }
    g.add(board);
    g.position.set(0, yTop - 0.04, -r * 0.42);
    g.userData.baseY = yTop - 0.04;
    return shadowed(g);
  }

  /* ---------- интерактив ---------- */

  blowCandle(c) {
    if (!c.userData.lit) return;
    c.userData.lit = false;
    const flame = c.userData.flame;
    tween({
      duration: 180,
      onUpdate: (e) => flame.scale.setScalar(Math.max(0.001, 1 - e)),
      onComplete: () => (flame.visible = false),
    });
    const pos = new THREE.Vector3();
    flame.getWorldPosition(pos);
    this.worldToLocal(pos);
    for (let k = 0; k < 9; k++) {
      const s = new THREE.Sprite(smokeTexture().clone());
      s.material.opacity = 0;
      s.position.copy(pos).add(new THREE.Vector3((Math.random() - 0.5) * 0.03, k * 0.02, (Math.random() - 0.5) * 0.03));
      s.scale.setScalar(0.05);
      this.add(s);
      this.smoke.push({ sprite: s, life: -k * 0.08, maxLife: 1.6 + Math.random() * 0.6, drift: Math.random() });
    }
    this.onCandlesChange?.(this.litCount, this.candles.length);
  }

  lightCandle(c) {
    if (c.userData.lit) return;
    c.userData.lit = true;
    const flame = c.userData.flame;
    flame.visible = true;
    c.userData.flameScale = 0.001;
    tween({
      duration: 500,
      ease: Ease.outElastic,
      onUpdate: (e) => (c.userData.flameScale = Math.max(0.001, e)),
      onComplete: () => (c.userData.flameScale = 1),
    });
    this.onCandlesChange?.(this.litCount, this.candles.length);
  }

  bounce(obj) {
    if (obj.userData.bouncing) return;
    obj.userData.bouncing = true;
    const baseY = obj.userData.baseY ?? obj.position.y;
    const rotY = obj.rotation.y;
    tween({
      duration: 900,
      ease: Ease.linear,
      onUpdate: (_, t) => {
        const jump = 4 * t * (1 - t);
        obj.position.y = baseY + jump * 0.45;
        obj.rotation.y = rotY + Ease.outCubic(t) * TAU;
      },
      onComplete: () => {
        obj.position.y = baseY;
        obj.rotation.y = rotY;
        obj.userData.bouncing = false;
      },
    });
  }

  clearSmoke() {
    for (const p of this.smoke) {
      p.sprite.parent?.remove(p.sprite);
      p.sprite.material.dispose();
    }
    this.smoke = [];
  }

  resetSliceInstant() {
    this.sliceAnim?.stop();
    this.sliced = false;
    if (this.tiers.length) this.applySlice(0);
  }

  /** k ∈ [0,1]: 0 — кусочек на месте, 1 — вынут и парит рядом с тортом срезом к зрителю. */
  applySlice(k) {
    this.sliceT = k;
    const mid = WEDGE / 2;
    const e = Ease.inOutCubic(k);
    // сначала выдвигаем вдоль биссектрисы, затем уводим в сторону и разворачиваем срезом к камере
    const pull = Math.min(1, k / 0.45);
    const dir = lerp(mid, 0.78, e);
    const dist = Ease.outCubic(pull) * 0.45 + e * 0.6;
    const pos = polar(dist, dir, e * 0.18 + Math.sin(pull * Math.PI) * 0.12);
    for (const t of this.tiers) {
      t.wedge.position.copy(pos);
      t.wedge.rotation.y = e * 0.5;
    }
  }
}
