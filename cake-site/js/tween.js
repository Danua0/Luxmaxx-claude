// Минималистичный движок твинов. Время абсолютное (performance.now),
// поэтому update() можно безопасно вызывать из нескольких render-loop'ов.

const active = new Set();

export const Ease = {
  linear: (t) => t,
  outCubic: (t) => 1 - Math.pow(1 - t, 3),
  inOutCubic: (t) => (t < 0.5 ? 4 * t * t * t : 1 - Math.pow(-2 * t + 2, 3) / 2),
  outBack: (t) => {
    const c1 = 1.70158;
    const c3 = c1 + 1;
    return 1 + c3 * Math.pow(t - 1, 3) + c1 * Math.pow(t - 1, 2);
  },
  outElastic: (t) => {
    if (t === 0 || t === 1) return t;
    return Math.pow(2, -10 * t) * Math.sin((t * 10 - 0.75) * ((2 * Math.PI) / 3)) + 1;
  },
  outBounce: (t) => {
    const n1 = 7.5625;
    const d1 = 2.75;
    if (t < 1 / d1) return n1 * t * t;
    if (t < 2 / d1) return n1 * (t -= 1.5 / d1) * t + 0.75;
    if (t < 2.5 / d1) return n1 * (t -= 2.25 / d1) * t + 0.9375;
    return n1 * (t -= 2.625 / d1) * t + 0.984375;
  },
};

/**
 * tween({ duration, delay, ease, onUpdate(easedT, rawT), onComplete })
 * Возвращает { stop() }.
 */
export function tween({ duration = 600, delay = 0, ease = Ease.outCubic, onUpdate, onComplete } = {}) {
  const tw = { start: performance.now() + delay, duration, ease, onUpdate, onComplete };
  active.add(tw);
  return {
    stop() {
      active.delete(tw);
    },
  };
}

export function updateTweens(now = performance.now()) {
  for (const tw of active) {
    if (now < tw.start) continue;
    const raw = tw.duration <= 0 ? 1 : Math.min(1, (now - tw.start) / tw.duration);
    tw.onUpdate?.(tw.ease(raw), raw);
    if (raw >= 1) {
      active.delete(tw);
      tw.onComplete?.();
    }
  }
}

export const lerp = (a, b, t) => a + (b - a) * t;
