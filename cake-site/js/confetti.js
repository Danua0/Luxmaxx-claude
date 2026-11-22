// Лёгкое 2D-конфетти поверх страницы.
const COLORS = ['#ff5c8a', '#ffd23f', '#3ec7ff', '#7bd389', '#b28dff', '#ff8f3f', '#ffffff'];

export function confetti({ count = 180 } = {}) {
  if (matchMedia('(prefers-reduced-motion: reduce)').matches) return;
  const canvas = document.getElementById('confetti');
  const ctx = canvas.getContext('2d');
  const dpr = Math.min(devicePixelRatio || 1, 2);
  canvas.width = innerWidth * dpr;
  canvas.height = innerHeight * dpr;
  ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
  // модальное окно живёт в top layer — переносим холст внутрь, иначе конфетти окажется под ним
  const host = document.querySelector('dialog[open]') || document.body;
  if (canvas.parentElement !== host) host.append(canvas);
  canvas.classList.add('on');

  const parts = Array.from({ length: count }, () => ({
    x: innerWidth / 2 + (Math.random() - 0.5) * innerWidth * 0.3,
    y: innerHeight * 0.55,
    vx: (Math.random() - 0.5) * 16,
    vy: -8 - Math.random() * 14,
    w: 6 + Math.random() * 6,
    h: 8 + Math.random() * 10,
    rot: Math.random() * Math.PI,
    vr: (Math.random() - 0.5) * 0.35,
    color: COLORS[(Math.random() * COLORS.length) | 0],
    round: Math.random() < 0.3,
  }));

  let last = performance.now();
  const frame = (now) => {
    const dt = Math.min(2, (now - last) / 16.7);
    last = now;
    ctx.clearRect(0, 0, innerWidth, innerHeight);
    let alive = 0;
    for (const p of parts) {
      p.vy += 0.38 * dt;
      p.vx *= 0.99;
      p.x += p.vx * dt;
      p.y += p.vy * dt;
      p.rot += p.vr * dt;
      if (p.y < innerHeight + 40) alive++;
      ctx.save();
      ctx.translate(p.x, p.y);
      ctx.rotate(p.rot);
      ctx.scale(1, Math.cos(p.rot * 2));
      ctx.fillStyle = p.color;
      if (p.round) {
        ctx.beginPath();
        ctx.arc(0, 0, p.w / 2, 0, Math.PI * 2);
        ctx.fill();
      } else ctx.fillRect(-p.w / 2, -p.h / 2, p.w, p.h);
      ctx.restore();
    }
    if (alive) requestAnimationFrame(frame);
    else {
      ctx.clearRect(0, 0, innerWidth, innerHeight);
      canvas.classList.remove('on');
    }
  };
  requestAnimationFrame(frame);
}
