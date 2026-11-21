import { useEffect, useId, useState } from "react";

/**
 * @file 单选切换组的液态玻璃表面。
 * @brief Canvas 只生成折射位移图，浏览器合成器提供透镜后的实时像素。
 */

interface BackdropGlassProps {
  width: number;
  height: number;
  pressed: boolean;
}

interface DisplacementMap {
  width: number;
  height: number;
  url: string;
}

/** 以 R、G 通道分别编码水平和垂直位移。胶囊边缘向内取样，中部保持原位。 */
function createDisplacementMap(width: number, height: number) {
  const canvas = document.createElement("canvas");
  canvas.width = width;
  canvas.height = height;
  const context = canvas.getContext("2d");
  if (!context) return null;

  const pixels = context.createImageData(width, height);
  const radius = Math.min(width, height) / 2;
  const coreX = width / 2 - radius;
  const coreY = height / 2 - radius;
  const rimWidth = radius * 0.45;

  for (let y = 0; y < height; y++) {
    const offsetY = y + 0.5 - height / 2;
    const normalY = Math.sign(offsetY) * Math.max(0, Math.abs(offsetY) - coreY);
    for (let x = 0; x < width; x++) {
      const offsetX = x + 0.5 - width / 2;
      const normalX = Math.sign(offsetX) * Math.max(0, Math.abs(offsetX) - coreX);
      const length = Math.hypot(normalX, normalY);
      const edge = Math.min(1, Math.max(0, (length - radius + rimWidth) / rimWidth));
      const displacement = length > 0 ? 127 * edge ** 2 / length : 0;

      const index = (y * width + x) * 4;
      pixels.data[index] = 128 - normalX * displacement;
      pixels.data[index + 1] = 128 - normalY * displacement;
      pixels.data[index + 3] = 255;
    }
  }
  context.putImageData(pixels, 0, 0);
  return canvas;
}

/** 按下时对实时背景应用折射，松开时保留原有半透明灰档。 */
export function BackdropGlass({ width, height, pressed }: BackdropGlassProps) {
  const filterId = "mui-segment-lens-" + useId();
  const [map, setMap] = useState<DisplacementMap | null>(null);

  useEffect(() => {
    if (width <= 0 || height <= 0) return;
    const canvas = createDisplacementMap(width, height);
    if (!canvas) return;

    let disposed = false;
    let url: string | undefined;
    canvas.toBlob((blob) => {
      if (!blob || disposed) return;
      url = URL.createObjectURL(blob);
      setMap({ width, height, url });
    });

    return () => {
      disposed = true;
      if (url) URL.revokeObjectURL(url);
    };
  }, [width, height]);

  const displacement = map?.width === width && map.height === height ? map.url : null;
  const refraction = "url(\"#" + filterId + "\")";
  const fallback = "blur(0.75px)";
  const filter = displacement && CSS.supports("backdrop-filter", refraction) ? refraction : fallback;

  return (
    <>
      {displacement && (
        <svg aria-hidden="true" className="pointer-events-none absolute size-0" width={0} height={0}>
          <defs>
            <filter
              id={filterId}
              x={0}
              y={0}
              width={width}
              height={height}
              filterUnits="userSpaceOnUse"
              primitiveUnits="userSpaceOnUse"
              colorInterpolationFilters="sRGB"
            >
              <feImage href={displacement} width={width} height={height} preserveAspectRatio="none" result="displacement" />
              <feDisplacementMap
                in="SourceGraphic"
                in2="displacement"
                scale={Math.min(width, height) * 0.28}
                xChannelSelector="R"
                yChannelSelector="G"
              />
            </filter>
          </defs>
        </svg>
      )}
      <span
        data-slot="toggle-group-lens"
        aria-hidden="true"
        className="pointer-events-none absolute inset-0 rounded-full transition-[scale,background-color,box-shadow] duration-[var(--glass-press-duration)] ease-out motion-reduce:transition-none"
        style={{
          scale: pressed
            ? "calc(1 + var(--glass-press-swell) * " + Math.min(1, height / width) + ") calc(1 + var(--glass-press-swell))"
            : "1",
          backdropFilter: pressed ? filter : undefined,
          WebkitBackdropFilter: pressed ? filter : undefined,
          backgroundColor: pressed
            ? "color-mix(in oklab, var(--glass-body-color) 10%, transparent)"
            : "color-mix(in oklab, var(--glass-gray-color) calc(var(--glass-gray-alpha) * 100%), transparent)",
          boxShadow: pressed
            ? "var(--shadow-glass-sm), inset 0 1px 1px color-mix(in oklab, var(--glass-rim-light) 55%, transparent), inset 0 -1px 2px color-mix(in oklab, var(--glass-rim-dark) 15%, transparent)"
            : undefined,
        }}
      />
    </>
  );
}
