import { memo, useId, useMemo } from 'react';

/**
 * 상품 아트워크.
 *
 * 스톡 사진 대신 상품의 컬러웨이·카테고리로 비즈 가닥을 직접 그립니다.
 *  · 외부 이미지 요청이 0 → LCP/CLS에 유리하고 이미지 깨짐이 없습니다.
 *  · 상품 id를 시드로 쓰기 때문에 같은 상품은 언제나 같은 모양으로 그려집니다.
 *
 * 실제 상품 사진이 준비되면 이 컴포넌트를 <img>로 교체하되,
 * media-frame(고정 비율 래퍼)은 그대로 두어 레이아웃 흔들림을 막으세요.
 */

/** 문자열 → 32bit 시드 */
function hashSeed(str) {
  let h = 2166136261;
  for (let i = 0; i < str.length; i += 1) {
    h ^= str.charCodeAt(i);
    h = Math.imul(h, 16777619);
  }
  return h >>> 0;
}

/** 시드 기반 난수 (렌더마다 동일한 결과 보장) */
function mulberry32(seed) {
  let a = seed;
  return () => {
    a |= 0;
    a = (a + 0x6d2b79f5) | 0;
    let t = Math.imul(a ^ (a >>> 15), 1 | a);
    t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t;
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
  };
}

const quad = (p0, p1, p2, t) => {
  const u = 1 - t;
  return {
    x: u * u * p0[0] + 2 * u * t * p1[0] + t * t * p2[0],
    y: u * u * p0[1] + 2 * u * t * p1[1] + t * t * p2[1],
  };
};

const VB = 400; // viewBox 한 변 (정사각 좌표계)

const clamp255 = (n) => Math.max(0, Math.min(255, Math.round(n)));

/** 두 색을 ratio(0~1)만큼 섞습니다. */
function mix(hexA, hexB, ratio) {
  const parse = (h) => [1, 3, 5].map((i) => parseInt(h.slice(i, i + 2), 16));
  const [r1, g1, b1] = parse(hexA);
  const [r2, g2, b2] = parse(hexB);
  const to = (a, b) => clamp255(a + (b - a) * ratio).toString(16).padStart(2, '0');
  return `#${to(r1, r2)}${to(g1, g2)}${to(b1, b2)}`;
}

/**
 * 스와치 한 색에서 4단계 팔레트를 만듭니다.
 * 컬러 옵션을 바꿨을 때 배경·하이라이트·줄까지 함께 변해야 실제로 "다른 색"으로 보입니다.
 */
export function paletteFromHex(hex) {
  return [hex, mix(hex, '#FFFFFF', 0.62), mix(hex, '#4A3324', 0.34), mix(hex, '#FFFFFF', 0.34)];
}

function buildBeads(category, rand) {
  const beads = [];
  const push = (x, y, r, tone) => beads.push({ x, y, r, tone });
  const jitter = (n) => (rand() - 0.5) * n;

  switch (category) {
    case 'necklace':
    case 'anklet': {
      const shallow = category === 'anklet';
      const p0 = [shallow ? 62 : 82, shallow ? 168 : 132];
      const p1 = [200, shallow ? 330 : 392];
      const p2 = [shallow ? 338 : 318, shallow ? 168 : 132];
      const count = shallow ? 26 : 32;
      for (let i = 0; i <= count; i += 1) {
        const t = i / count;
        const { x, y } = quad(p0, p1, p2, t);
        const mid = 1 - Math.abs(t - 0.5) * 2; // 가운데로 갈수록 알이 커짐
        push(x + jitter(1.2), y + jitter(1.2), 7.5 + mid * 4.5 + jitter(1.4), i % 4);
      }
      const center = quad(p0, p1, p2, 0.5);
      push(center.x, center.y + 30, 21, 2); // 펜던트
      push(center.x, center.y + 30, 7, 1);
      break;
    }

    case 'bracelet': {
      const count = 22;
      for (let i = 0; i < count; i += 1) {
        const a = (i / count) * Math.PI * 2 - Math.PI / 2;
        push(
          200 + Math.cos(a) * 112 + jitter(1.5),
          208 + Math.sin(a) * 112 + jitter(1.5),
          i % 3 === 0 ? 15 : 12 + jitter(1.6),
          i % 4,
        );
      }
      push(200, 352, 17, 2); // 참(charm)
      break;
    }

    case 'ring': {
      const count = 20;
      for (let i = 0; i < count; i += 1) {
        const a = (i / count) * Math.PI * 2 - Math.PI / 2;
        push(200 + Math.cos(a) * 92, 212 + Math.sin(a) * 92, 9.5 + jitter(1), i % 3);
      }
      push(200, 120, 24, 0); // 포컬 비즈
      push(200, 120, 8, 1);
      break;
    }

    case 'earring': {
      [136, 264].forEach((cx, side) => {
        for (let i = 0; i < 6; i += 1) {
          const y = 168 + i * 33;
          const r = 15 - i * 1.1;
          push(cx + jitter(2) + (side ? -1 : 1) * i * 1.5, y, r, i % 4);
        }
        push(cx, 168 + 6 * 33 + 6, 9, 2);
      });
      break;
    }

    default: {
      // strap / keyring — 세로로 흐르는 한 가닥
      for (let i = 0; i < 20; i += 1) {
        const t = i / 19;
        push(200 + Math.sin(t * Math.PI * 2.2) * 46, 132 + t * 246, 13 + jitter(2), i % 4);
      }
      break;
    }
  }
  return beads;
}

function BeadArt({ product, className = '', decorative = false }) {
  const uid = useId().replace(/:/g, '');
  const palette = product.palette ?? ['#E8DFD4', '#F7F2EA', '#D3C5B5', '#FBF8F4'];

  const { beads, cord } = useMemo(() => {
    const rand = mulberry32(hashSeed(product.id ?? product.name ?? 'hoonshop'));
    const list = buildBeads(product.category, rand);

    let path = null;
    if (product.category === 'necklace') path = 'M82,132 Q200,392 318,132';
    else if (product.category === 'anklet') path = 'M62,168 Q200,330 338,168';
    else if (product.category === 'bracelet') path = 'M200,96 A112,112 0 1,1 199,96';
    else if (product.category === 'ring') path = 'M200,120 A92,92 0 1,1 199,120';
    else if (product.category === 'strap')
      path = 'M200,132 C246,190 154,254 200,314 C246,374 200,378 200,378';

    return { beads: list, cord: path };
  }, [product.id, product.name, product.category]);

  return (
    <svg
      viewBox={`0 0 ${VB} ${VB}`}
      className={className}
      role={decorative ? 'presentation' : 'img'}
      aria-hidden={decorative || undefined}
      aria-label={decorative ? undefined : `${product.name} 이미지`}
      preserveAspectRatio="xMidYMid meet"
    >
      <defs>
        <linearGradient id={`bg-${uid}`} x1="0" y1="0" x2="1" y2="1">
          <stop offset="0%" stopColor={palette[1]} stopOpacity="0.55" />
          <stop offset="100%" stopColor={palette[3] ?? palette[1]} stopOpacity="0.2" />
        </linearGradient>

        {palette.map((color, i) => (
          <radialGradient key={color + i} id={`bead-${uid}-${i}`} cx="34%" cy="30%" r="72%">
            <stop offset="0%" stopColor="#FFFFFF" stopOpacity="0.9" />
            <stop offset="34%" stopColor={color} />
            <stop offset="100%" stopColor={palette[2] ?? color} />
          </radialGradient>
        ))}

        <filter id={`soft-${uid}`} x="-20%" y="-20%" width="140%" height="140%">
          <feDropShadow dx="0" dy="3" stdDeviation="4" floodColor="#3C2A1E" floodOpacity="0.18" />
        </filter>
      </defs>

      {/* viewBox 밖까지 칠해 4:5·16:10 등 비정사각 프레임에서 레터박스가 생기지 않게 합니다 */}
      <rect x={-VB} y={-VB} width={VB * 3} height={VB * 3} fill={`url(#bg-${uid})`} />
      {/* 배경 링: 주얼리를 놓은 트레이 느낌 */}
      <circle cx="200" cy="205" r="150" fill="none" stroke="#FFFFFF" strokeOpacity="0.5" strokeWidth="1.5" />

      <g filter={`url(#soft-${uid})`}>
        {cord && (
          <path d={cord} fill="none" stroke={palette[2] ?? '#D3C5B5'} strokeOpacity="0.75" strokeWidth="2" />
        )}
        {beads.map((b, i) => (
          <g key={i}>
            <circle cx={b.x} cy={b.y} r={b.r} fill={`url(#bead-${uid}-${b.tone})`} />
            <circle
              cx={b.x - b.r * 0.3}
              cy={b.y - b.r * 0.34}
              r={b.r * 0.24}
              fill="#FFFFFF"
              fillOpacity="0.75"
            />
          </g>
        ))}
      </g>
    </svg>
  );
}

export default memo(BeadArt);
