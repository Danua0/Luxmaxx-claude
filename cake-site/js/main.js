import { Stage } from './stage.js';
import { SPONGES, FILLINGS, FROSTINGS, DRIPS, TOPPINGS } from './cake.js';
import { confetti } from './confetti.js';

/* ------------------------------------------------------------------ данные */

const SIZES = {
  1: { label: '1 ярус', weight: 1.6, guests: '8–10 гостей', price: 3200 },
  2: { label: '2 яруса', weight: 3.2, guests: '16–20 гостей', price: 5900 },
  3: { label: '3 яруса', weight: 5.4, guests: '30–35 гостей', price: 9400 },
};

const noToppings = { rosettes: false, strawberries: false, blueberries: false, cherries: false, macarons: false, sprinkles: false };
const T = (...on) => ({ ...noToppings, ...Object.fromEntries(on.map((k) => [k, true])) });

const PRESETS = [
  {
    id: 'strawberry-cloud',
    name: 'Клубничное облако',
    desc: 'Ванильный бисквит, клубничный конфитюр и лёгкий крем-чиз.',
    tag: 'Хит',
    cfg: { tiers: 2, sponge: 'vanilla', filling: 'strawberry', frosting: '#f5bccd', drip: 'white', toppings: T('rosettes', 'strawberries'), candles: 3, text: 'С днём рождения!' },
  },
  {
    id: 'dark-chocolate',
    name: 'Тёмный шоколад',
    desc: 'Шоколадный бисквит, ганаш на бельгийском шоколаде 70%, вишня.',
    tag: 'Для шоколадоголиков',
    cfg: { tiers: 1, sponge: 'chocolate', filling: 'chocolate', frosting: '#4a2b1d', drip: 'chocolate', toppings: T('cherries', 'rosettes'), candles: 0, text: '' },
  },
  {
    id: 'red-velvet',
    name: 'Красный бархат',
    desc: 'Классика с крем-чизом, свежей клубникой и голубикой.',
    tag: 'Классика',
    cfg: { tiers: 1, sponge: 'redvelvet', filling: 'vanilla', frosting: '#fff6ea', drip: 'none', toppings: T('rosettes', 'strawberries', 'blueberries'), candles: 0, text: '' },
  },
  {
    id: 'pistachio',
    name: 'Фисташковый сад',
    desc: 'Бисквит матча, фисташковый крем и макаруны ручной работы.',
    tag: 'Новинка',
    cfg: { tiers: 2, sponge: 'matcha', filling: 'pistachio', frosting: '#cfeedd', drip: 'none', toppings: T('macarons', 'blueberries', 'rosettes'), candles: 0, text: '' },
  },
  {
    id: 'caramel-cascade',
    name: 'Карамельный каскад',
    desc: 'Три яруса морковного бисквита с солёной карамелью — свадебный бестселлер.',
    tag: 'Свадебный',
    cfg: { tiers: 3, sponge: 'carrot', filling: 'caramel', frosting: '#fff6ea', drip: 'caramel', toppings: T('rosettes', 'macarons'), candles: 0, text: 'Совет да любовь' },
  },
  {
    id: 'party',
    name: 'Праздник детства',
    desc: 'Ванильный бисквит, клубника, радужная посыпка и пять свечей.',
    tag: 'Детский',
    cfg: { tiers: 2, sponge: 'vanilla', filling: 'lavender', frosting: '#cfe4f7', drip: 'berry', toppings: T('sprinkles', 'macarons'), candles: 5, text: 'Ура, 5 лет!' },
  },
];

function priceOf(cfg) {
  let p = SIZES[cfg.tiers].price;
  const k = SIZES[cfg.tiers].weight / 3.2; // начинка/декор дороже на больших тортах
  p += (SPONGES[cfg.sponge].price + FILLINGS[cfg.filling].price + DRIPS[cfg.drip].price) * k;
  for (const [key, on] of Object.entries(cfg.toppings)) if (on) p += TOPPINGS[key].price * k;
  p += cfg.candles * 40;
  if (cfg.text.trim()) p += 350;
  return Math.round(p / 10) * 10;
}

const rub = (n) => new Intl.NumberFormat('ru-RU').format(Math.round(n)) + ' ₽';
const $ = (s, root = document) => root.querySelector(s);
const clone = (o) => structuredClone(o);

function describe(cfg) {
  const tops = Object.entries(cfg.toppings)
    .filter(([, v]) => v)
    .map(([k]) => TOPPINGS[k].name.toLowerCase());
  return [
    SIZES[cfg.tiers].label,
    SPONGES[cfg.sponge].name.toLowerCase() + ' бисквит',
    FILLINGS[cfg.filling].name.toLowerCase(),
    cfg.drip !== 'none' ? 'подтёки: ' + DRIPS[cfg.drip].name.toLowerCase() : null,
    tops.length ? tops.join(', ') : null,
    cfg.candles ? `свечи: ${cfg.candles}` : null,
    cfg.text.trim() ? `«${cfg.text.trim()}»` : null,
  ]
    .filter(Boolean)
    .join(' · ');
}

/* ------------------------------------------------------------------ тосты */

function toast(text, icon = '🍰') {
  const el = document.createElement('div');
  el.className = 'toast';
  el.innerHTML = `<span>${icon}</span><p></p>`;
  el.querySelector('p').textContent = text;
  $('#toasts').append(el);
  requestAnimationFrame(() => el.classList.add('in'));
  setTimeout(() => {
    el.classList.remove('in');
    setTimeout(() => el.remove(), 400);
  }, 2800);
}

/* ------------------------------------------------------------------ корзина */

const CART_KEY = 'sugar-cloud-cart';
let cart = [];
try {
  cart = JSON.parse(localStorage.getItem(CART_KEY) || '[]');
} catch {
  cart = [];
}

function saveCart() {
  try {
    localStorage.setItem(CART_KEY, JSON.stringify(cart));
  } catch {
    /* приватный режим или переполнение — корзина просто не сохранится */
  }
}

const cartTotal = () => cart.reduce((s, it) => s + it.price * it.qty, 0);

function renderCart() {
  const count = cart.reduce((s, it) => s + it.qty, 0);
  const badge = $('#cartCount');
  badge.textContent = count;
  badge.classList.toggle('has', count > 0);
  const list = $('#cartItems');
  list.innerHTML = '';
  if (!cart.length) {
    list.innerHTML = '<div class="cart-empty"><span>🧁</span><p>Корзина пока пуста.<br/>Самое время собрать торт!</p></div>';
  }
  cart.forEach((it, i) => {
    const row = document.createElement('div');
    row.className = 'cart-item';
    row.innerHTML = `
      <img alt="" />
      <div class="ci-body">
        <b></b>
        <small></small>
        <div class="ci-row">
          <div class="qty">
            <button aria-label="Меньше" data-d="-1">−</button><span>${it.qty}</span><button aria-label="Больше" data-d="1">+</button>
          </div>
          <span class="ci-price">${rub(it.price * it.qty)}</span>
        </div>
      </div>
      <button class="icon-btn ci-del" aria-label="Удалить">✕</button>`;
    row.querySelector('img').src = it.image || '';
    row.querySelector('b').textContent = it.name;
    row.querySelector('small').textContent = it.summary;
    row.querySelectorAll('.qty button').forEach((b) =>
      b.addEventListener('click', () => {
        it.qty = Math.max(1, it.qty + Number(b.dataset.d));
        saveCart();
        renderCart();
      }),
    );
    row.querySelector('.ci-del').addEventListener('click', () => {
      row.classList.add('out');
      setTimeout(() => {
        cart.splice(i, 1);
        saveCart();
        renderCart();
      }, 250);
    });
    list.append(row);
  });
  $('#cartTotal').textContent = rub(cartTotal());
  $('#checkout').disabled = !cart.length;
}

function addToCart(item, fromEl) {
  const same = cart.find((it) => it.key === item.key);
  if (same) same.qty++;
  else cart.push({ ...item, qty: 1 });
  saveCart();
  renderCart();
  flyToCart(fromEl, item.image);
  toast(`«${item.name}» в корзине`, '🛒');
}

function flyToCart(fromEl, image) {
  if (!fromEl || matchMedia('(prefers-reduced-motion: reduce)').matches) return;
  const a = fromEl.getBoundingClientRect();
  const b = $('#cartOpen').getBoundingClientRect();
  const img = document.createElement('img');
  img.src = image;
  img.className = 'fly';
  img.style.left = a.left + a.width / 2 - 40 + 'px';
  img.style.top = a.top + a.height / 2 - 40 + 'px';
  document.body.append(img);
  const dx = b.left + b.width / 2 - (a.left + a.width / 2);
  const dy = b.top + b.height / 2 - (a.top + a.height / 2);
  img
    .animate(
      [
        { transform: 'translate(0,0) scale(1) rotate(0)', opacity: 1 },
        { transform: `translate(${dx * 0.5}px, ${dy - 120}px) scale(0.8) rotate(-12deg)`, opacity: 1, offset: 0.55 },
        { transform: `translate(${dx}px, ${dy}px) scale(0.2) rotate(10deg)`, opacity: 0.4 },
      ],
      { duration: 800, easing: 'cubic-bezier(.5,0,.3,1)' },
    )
    .finished.then(() => {
      img.remove();
      $('#cartOpen').animate([{ transform: 'scale(1)' }, { transform: 'scale(1.18)' }, { transform: 'scale(1)' }], { duration: 350 });
    });
}

function openCart(open) {
  $('#cart').classList.toggle('open', open);
  $('#cart').setAttribute('aria-hidden', String(!open));
  $('#overlay').hidden = !open;
  document.body.classList.toggle('lock', open);
}

$('#cartOpen').addEventListener('click', () => openCart(true));
$('#cartClose').addEventListener('click', () => openCart(false));
$('#overlay').addEventListener('click', () => openCart(false));
document.addEventListener('keydown', (e) => e.key === 'Escape' && openCart(false));

/* ------------------------------------------------------------------ оформление */

const modal = $('#orderModal');
const orderForm = $('#orderForm');

function pickupDiscount() {
  return orderForm.delivery.value === 'pickup' ? 0.95 : 1;
}
function updateOrderTotal() {
  $('#orderTotal').textContent = rub(cartTotal() * pickupDiscount());
  $('.addr', orderForm).classList.toggle('hide', orderForm.delivery.value === 'pickup');
}

$('#checkout').addEventListener('click', () => {
  if (!cart.length) return;
  openCart(false);
  const d = new Date(Date.now() + 2 * 864e5);
  const date = $('#orderDate');
  date.min = d.toISOString().slice(0, 10);
  if (!date.value) date.value = date.min;
  $('.order-body', orderForm).hidden = false;
  $('.order-done', orderForm).hidden = true;
  updateOrderTotal();
  modal.showModal();
});
orderForm.addEventListener('change', updateOrderTotal);
orderForm.phone.addEventListener('input', (e) => {
  const digits = e.target.value.replace(/\D/g, '').replace(/^8/, '7').slice(0, 11);
  const d = digits.startsWith('7') ? digits.slice(1) : digits;
  let out = '+7';
  if (d.length) out += ' (' + d.slice(0, 3);
  if (d.length >= 3) out += ') ' + d.slice(3, 6);
  if (d.length >= 6) out += '-' + d.slice(6, 8);
  if (d.length >= 8) out += '-' + d.slice(8, 10);
  e.target.value = out;
});
$('#orderSubmit').addEventListener('click', (e) => {
  e.preventDefault();
  if (!orderForm.reportValidity()) return;
  if (orderForm.phone.value.replace(/\D/g, '').length < 11) {
    orderForm.phone.setCustomValidity('Введите номер полностью');
    orderForm.phone.reportValidity();
    orderForm.phone.setCustomValidity('');
    return;
  }
  $('#orderNum').textContent = '№ ' + String(Math.floor(100000 + Math.random() * 899999));
  $('.order-body', orderForm).hidden = true;
  $('.order-done', orderForm).hidden = false;
  cart = [];
  saveCart();
  renderCart();
  confetti();
});

/* ------------------------------------------------------------------ 3D: витрина */

const heroStage = new Stage($('#heroCanvas'), {
  parallax: true,
  controls: matchMedia('(pointer: fine)').matches,
  zoom: false,
});
let heroIndex = 0;
let heroTimer;
const dots = $('#heroDots');
PRESETS.forEach((p, i) => {
  const b = document.createElement('button');
  b.setAttribute('role', 'tab');
  b.setAttribute('aria-label', p.name);
  b.addEventListener('click', () => showHero(i));
  dots.append(b);
});

function showHero(i, first = false) {
  heroIndex = (i + PRESETS.length) % PRESETS.length;
  const p = PRESETS[heroIndex];
  if (first) heroStage.cake.build(p.cfg, { animate: true });
  else heroStage.cake.update(p.cfg);
  const name = $('#heroName');
  name.classList.remove('swap');
  void name.offsetWidth;
  name.textContent = p.name;
  name.classList.add('swap');
  [...dots.children].forEach((d, k) => d.setAttribute('aria-selected', String(k === heroIndex)));
  clearInterval(heroTimer);
  heroTimer = setInterval(() => showHero(heroIndex + 1), 6500);
}

/* ------------------------------------------------------------------ 3D: конструктор */

const DEFAULT_CFG = clone(PRESETS[0].cfg);
let cfg = clone(DEFAULT_CFG);

const builder = new Stage($('#builderCanvas'), {
  controls: true,
  interactive: true,
  autoRotate: 0.8,
  tooltip: $('#stageTip'),
  onPick(kind) {
    hideHint();
    if (kind === 'cake') updateSliceBtn();
  },
});

builder.cake.onCandlesChange = (lit, total) => {
  const st = $('#candleStatus');
  if (!total) {
    st.classList.remove('show');
    return;
  }
  st.textContent = lit ? `🕯 Горит ${lit} из ${total}` : '✨ Загадайте желание!';
  st.classList.add('show');
  $('#actBlow span').textContent = lit ? 'Задуть свечи' : 'Зажечь свечи';
  if (!lit && wishArmed) {
    wishArmed = false;
    confetti({ count: 120 });
  }
  if (lit === total) wishArmed = true;
};
let wishArmed = true;

function hideHint() {
  $('#stageHint').classList.add('hide');
}
builder.canvas.addEventListener('pointerdown', hideHint, { once: true });

function updateSliceBtn() {
  $('#actSlice span').textContent = builder.cake.sliced ? 'Вернуть кусочек' : 'Отрезать кусочек';
}

$('#actBlow').addEventListener('click', () => {
  if (builder.cake.litCount) builder.cake.blowAll();
  else builder.cake.lightAll();
});
$('#actSlice').addEventListener('click', () => {
  builder.cake.toggleSlice();
  updateSliceBtn();
});
$('#actSpin').addEventListener('click', () => builder.cake.spin());
$('#actShot').addEventListener('click', () => {
  const url = builder.snapshot({ width: 1400, type: 'image/png', background: '#fff6ef' });
  const a = document.createElement('a');
  a.href = url;
  a.download = 'my-cake.png';
  a.click();
  toast('Снимок торта сохранён', '📷');
});

// «Задуть по-настоящему»: слушаем микрофон и гасим свечи при сильном потоке воздуха
let micBusy = false;
$('#actMic').addEventListener('click', async () => {
  if (micBusy) return;
  if (!builder.cake.litCount) builder.cake.lightAll();
  if (!navigator.mediaDevices?.getUserMedia) return toast('Микрофон недоступен в этом браузере', '🎤');
  micBusy = true;
  const btn = $('#actMic');
  let stream;
  try {
    stream = await navigator.mediaDevices.getUserMedia({ audio: true });
  } catch {
    micBusy = false;
    return toast('Нет доступа к микрофону — нажмите на свечи мышкой', '🎤');
  }
  btn.classList.add('listening');
  toast('Подуйте на экран!', '💨');
  const ctx = new AudioContext();
  const analyser = ctx.createAnalyser();
  analyser.fftSize = 1024;
  ctx.createMediaStreamSource(stream).connect(analyser);
  const data = new Uint8Array(analyser.fftSize);
  let strong = 0;
  const started = performance.now();
  const stop = () => {
    stream.getTracks().forEach((t) => t.stop());
    ctx.close();
    btn.classList.remove('listening');
    micBusy = false;
  };
  const listen = () => {
    analyser.getByteTimeDomainData(data);
    let sum = 0;
    for (const v of data) sum += ((v - 128) / 128) ** 2;
    const rms = Math.sqrt(sum / data.length);
    strong = rms > 0.18 ? strong + 1 : Math.max(0, strong - 1);
    if (strong > 6) {
      builder.cake.blowAll();
      return stop();
    }
    if (performance.now() - started > 12000) {
      toast('Не услышали — попробуйте подуть сильнее', '🎤');
      return stop();
    }
    requestAnimationFrame(listen);
  };
  listen();
});

/* ---------- панель опций ---------- */

function chip(label, { pressed, onClick, color, title } = {}) {
  const b = document.createElement('button');
  b.type = 'button';
  b.className = 'chip';
  b.setAttribute('aria-pressed', String(!!pressed));
  if (color) b.innerHTML = `<i style="background:${color}"></i>`;
  b.append(document.createTextNode(label));
  if (title) b.title = title;
  b.addEventListener('click', onClick);
  return b;
}

const priceTag = (p) => (p ? ` +${p}` : '');

function renderPanel() {
  const tiers = $('#optTiers');
  tiers.innerHTML = '';
  for (const n of [1, 2, 3]) {
    const s = SIZES[n];
    const b = document.createElement('button');
    b.type = 'button';
    b.setAttribute('aria-pressed', String(cfg.tiers === n));
    b.innerHTML = `<span class="tier-ico">${'▬'.repeat(n)}</span><b>${s.label}</b><small>${String(s.weight).replace('.', ',')} кг</small>`;
    b.addEventListener('click', () => set({ tiers: n }));
    tiers.append(b);
  }

  const fill = (id, dict, key) => {
    const box = $(id);
    box.innerHTML = '';
    for (const [k, v] of Object.entries(dict)) {
      box.append(chip(v.name, { pressed: cfg[key] === k, color: v.color, title: v.price ? `+${v.price} ₽` : 'Входит в цену', onClick: () => set({ [key]: k }) }));
    }
  };
  fill('#optSponge', SPONGES, 'sponge');
  fill('#optFilling', FILLINGS, 'filling');
  fill('#optDrip', DRIPS, 'drip');

  const sw = $('#optFrosting');
  sw.innerHTML = '';
  for (const f of FROSTINGS) {
    const b = document.createElement('button');
    b.type = 'button';
    b.className = 'swatch';
    b.style.setProperty('--c', f.color);
    b.title = f.name;
    b.setAttribute('aria-label', f.name);
    b.setAttribute('aria-pressed', String(cfg.frosting.toLowerCase() === f.color));
    b.addEventListener('click', () => set({ frosting: f.color }));
    sw.append(b);
  }
  const custom = document.createElement('label');
  custom.className = 'swatch swatch-custom';
  custom.title = 'Свой цвет';
  const isCustom = !FROSTINGS.some((f) => f.color === cfg.frosting.toLowerCase());
  custom.setAttribute('aria-pressed', String(isCustom));
  custom.innerHTML = '<input type="color" aria-label="Свой цвет покрытия" />';
  const input = custom.querySelector('input');
  input.value = cfg.frosting;
  input.addEventListener('input', () => set({ frosting: input.value }, { soft: true }));
  input.addEventListener('change', () => renderPanel());
  sw.append(custom);

  const tops = $('#optToppings');
  tops.innerHTML = '';
  for (const [k, v] of Object.entries(TOPPINGS)) {
    tops.append(
      chip(`${v.icon} ${v.name}`, {
        pressed: cfg.toppings[k],
        title: `+${v.price} ₽`,
        onClick: () => set({ toppings: { ...cfg.toppings, [k]: !cfg.toppings[k] } }),
      }),
    );
  }

  $('#optCandles').value = cfg.candles;
  $('#candleOut').textContent = cfg.candles;
  if (document.activeElement !== $('#optText')) $('#optText').value = cfg.text;
  updateSummary();
}

let shownPrice = 0;
function updateSummary() {
  const s = SIZES[cfg.tiers];
  $('#sumWeight').textContent = String(s.weight).replace('.', ',') + ' кг';
  $('#sumGuests').textContent = s.guests;
  const target = priceOf(cfg);
  const from = shownPrice;
  const t0 = performance.now();
  const el = $('#sumPrice');
  const step = (now) => {
    const k = Math.min(1, (now - t0) / 500);
    shownPrice = from + (target - from) * (1 - Math.pow(1 - k, 3));
    el.textContent = new Intl.NumberFormat('ru-RU').format(Math.round(shownPrice));
    if (k < 1) requestAnimationFrame(step);
  };
  requestAnimationFrame(step);
  el.parentElement.classList.remove('bump');
  void el.offsetWidth;
  el.parentElement.classList.add('bump');
}

function set(patch, { soft = false } = {}) {
  cfg = { ...cfg, ...clone(patch) };
  builder.cake.update(cfg);
  updateSliceBtn();
  if (soft) updateSummary();
  else renderPanel();
}

$('#optCandles').addEventListener('input', (e) => set({ candles: Number(e.target.value) }, { soft: true }));
$('#optCandles').addEventListener('input', (e) => ($('#candleOut').textContent = e.target.value));
let textTimer;
$('#optText').addEventListener('input', (e) => {
  clearTimeout(textTimer);
  textTimer = setTimeout(() => set({ text: e.target.value }, { soft: true }), 350);
});
$('#resetBuilder').addEventListener('click', () => {
  cfg = clone(DEFAULT_CFG);
  builder.cake.build(cfg, { animate: true });
  updateSliceBtn();
  renderPanel();
});

$('#addCustom').addEventListener('click', (e) => {
  const preset = PRESETS.find((p) => JSON.stringify(p.cfg) === JSON.stringify(cfg));
  addToCart(
    {
      key: 'custom:' + JSON.stringify(cfg),
      name: preset ? preset.name : 'Авторский торт',
      summary: describe(cfg),
      price: priceOf(cfg),
      image: builder.snapshot({ width: 240 }),
      cfg: clone(cfg),
    },
    e.currentTarget,
  );
});

function loadIntoBuilder(preset) {
  cfg = clone(preset.cfg);
  document.getElementById('builder').scrollIntoView({ behavior: 'smooth' });
  setTimeout(() => {
    builder.cake.build(cfg, { animate: true });
    updateSliceBtn();
    renderPanel();
  }, 450);
}

/* ------------------------------------------------------------------ каталог */

function renderCatalog() {
  const grid = $('#catalogGrid');
  // Превью рендерим один раз оффскрин-сценой — каталог сам по себе не держит WebGL-контексты
  const canvas = document.createElement('canvas');
  const shot = new Stage(canvas, { offscreen: { width: 520, height: 440 } });
  shot.camera.position.set(0, 3.0, 8.6);
  shot.camera.lookAt(0, 0.85, 0);

  PRESETS.forEach((p, i) => {
    shot.cake.build(p.cfg, { animate: false });
    shot.cake.rotation.y = -0.35;
    shot.cake.tick(0.016, 1);
    p.image = shot.snapshot({ width: 520, type: 'image/png', background: null });
    const price = priceOf(p.cfg);
    const card = document.createElement('article');
    card.className = 'card reveal';
    card.style.setProperty('--d', i * 70 + 'ms');
    card.innerHTML = `
      <div class="card-media tilt">
        <span class="card-tag"></span>
        <img alt="" loading="lazy" width="520" height="440" />
        <span class="shine"></span>
      </div>
      <div class="card-body">
        <h3></h3>
        <p></p>
        <div class="card-meta"><span>${SIZES[p.cfg.tiers].label} · ${String(SIZES[p.cfg.tiers].weight).replace('.', ',')} кг</span><b>${rub(price)}</b></div>
        <div class="card-actions">
          <button class="btn btn-ghost btn-sm" data-act="edit">Открыть в 3D</button>
          <button class="btn btn-primary btn-sm" data-act="buy">В корзину</button>
        </div>
      </div>`;
    card.querySelector('.card-tag').textContent = p.tag;
    card.querySelector('img').src = p.image;
    card.querySelector('img').alt = p.name;
    card.querySelector('h3').textContent = p.name;
    card.querySelector('p').textContent = p.desc;
    card.querySelector('[data-act=edit]').addEventListener('click', () => loadIntoBuilder(p));
    card.querySelector('[data-act=buy]').addEventListener('click', (e) =>
      addToCart({ key: 'preset:' + p.id, name: p.name, summary: describe(p.cfg), price, image: p.image, cfg: clone(p.cfg) }, e.currentTarget),
    );
    grid.append(card);
  });
  shot.dispose();
}

/* ------------------------------------------------------------------ эффекты страницы */

function setupReveal() {
  const io = new IntersectionObserver(
    (entries) =>
      entries.forEach((e) => {
        if (e.isIntersecting) {
          e.target.classList.add('in');
          io.unobserve(e.target);
        }
      }),
    { threshold: 0.12 },
  );
  document.querySelectorAll('.reveal').forEach((el) => io.observe(el));
}

function setupTilt() {
  if (!matchMedia('(pointer: fine)').matches) return;
  document.addEventListener('pointermove', (e) => {
    const el = e.target.closest?.('.tilt');
    document.querySelectorAll('.tilt.active').forEach((t) => t !== el && resetTilt(t));
    if (!el) return;
    const r = el.getBoundingClientRect();
    const x = (e.clientX - r.left) / r.width - 0.5;
    const y = (e.clientY - r.top) / r.height - 0.5;
    el.classList.add('active');
    el.style.setProperty('--rx', `${-y * 12}deg`);
    el.style.setProperty('--ry', `${x * 14}deg`);
    el.style.setProperty('--mx', `${(x + 0.5) * 100}%`);
    el.style.setProperty('--my', `${(y + 0.5) * 100}%`);
  });
  const resetTilt = (t) => {
    t.classList.remove('active');
    t.style.setProperty('--rx', '0deg');
    t.style.setProperty('--ry', '0deg');
  };
}

function setupMagnetic() {
  if (!matchMedia('(pointer: fine)').matches) return;
  document.querySelectorAll('.magnetic').forEach((el) => {
    el.addEventListener('pointermove', (e) => {
      const r = el.getBoundingClientRect();
      el.style.transform = `translate(${(e.clientX - r.left - r.width / 2) * 0.18}px, ${(e.clientY - r.top - r.height / 2) * 0.25}px)`;
    });
    el.addEventListener('pointerleave', () => (el.style.transform = ''));
  });
}

function setupCounters() {
  const io = new IntersectionObserver((entries) =>
    entries.forEach((e) => {
      if (!e.isIntersecting) return;
      io.unobserve(e.target);
      const target = Number(e.target.dataset.count);
      const t0 = performance.now();
      const step = (now) => {
        const k = Math.min(1, (now - t0) / 1600);
        e.target.textContent = new Intl.NumberFormat('ru-RU').format(Math.round(target * (1 - Math.pow(1 - k, 4)))) + (k === 1 ? '+' : '');
        if (k < 1) requestAnimationFrame(step);
      };
      requestAnimationFrame(step);
    }),
  );
  document.querySelectorAll('[data-count]').forEach((el) => io.observe(el));
}

function setupHeader() {
  const header = $('.header');
  const onScroll = () => header.classList.toggle('scrolled', scrollY > 10);
  addEventListener('scroll', onScroll, { passive: true });
  onScroll();
}

/* ------------------------------------------------------------------ старт */

$('#year').textContent = new Date().getFullYear();

// Шрифт нужен до отрисовки надписей на топперах
await Promise.race([document.fonts.load('italic 700 64px "Playfair Display"'), new Promise((r) => setTimeout(r, 1500))]);

renderCatalog();
showHero(0, true);
builder.cake.build(cfg, { animate: true });
renderPanel();
renderCart();
setupReveal();
setupTilt();
setupMagnetic();
setupCounters();
setupHeader();
document.body.classList.add('ready');
