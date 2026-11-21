/** @file 将 GLSL 算出的三通道位移交给浏览器合成器，折射实时页面背景。 */
import { Fragment, useEffect, useId, useRef } from "react";

const channels = ["r", "g", "b"] as const;
const matrices = [
  "1 0 0 0 0  0 0 0 0 0  0 0 0 0 0  0 0 0 0 1",
  "0 0 0 0 0  0 1 0 0 0  0 0 0 0 0  0 0 0 0 1",
  "0 0 0 0 0  0 0 0 0 0  0 0 1 0 0  0 0 0 0 1",
];

type BackdropFrame = {
  key: string;
  width: number;
  height: number;
  scale: number;
  draw: (canvas: HTMLCanvasElement, channel: number) => void;
};

/** 在浏览器内部组合三通道，Canvas 只传位移和轮廓，不传页面像素。 */
export function useBackdropGlass(enabled: boolean) {
  const filterId = "mui-segment-backdrop-" + useId();
  const filterRef = useRef<SVGFilterElement>(null);
  const surfaceRef = useRef<HTMLSpanElement>(null);
  const state = useRef({
    urls: [] as string[],
    canvas: null as HTMLCanvasElement | null,
    pending: false,
    active: false,
    key: "",
    generation: 0,
  });

  useEffect(() => {
    return () => {
      const current = state.current;
      current.generation++;
      current.pending = false;
      current.active = false;
      current.key = "";
      current.urls.forEach((url) => URL.revokeObjectURL(url));
      current.urls = [];
    };
  }, [enabled]);

  function clear() {
    state.current.active = false;
    if (surfaceRef.current?.style.backdropFilter) {
      surfaceRef.current.style.removeProperty("backdrop-filter");
    }
  }

  /** 等同一帧的三张位移图齐备后，再更新滤镜输入。 */
  function publish(frame: BackdropFrame) {
    const current = state.current;
    const filter = filterRef.current;
    const surface = surfaceRef.current;
    if (!filter || !surface) return;
    const cssFilter = 'url("#' + filterId + '")';
    if (!CSS.supports("backdrop-filter", cssFilter)) return;
    current.active = true;
    if (current.urls.length && !surface.style.backdropFilter) surface.style.backdropFilter = cssFilter;
    if (current.pending || frame.key === current.key) return;

    const canvas = (current.canvas ??= document.createElement("canvas"));
    if (canvas.width !== frame.width) canvas.width = frame.width;
    if (canvas.height !== frame.height) canvas.height = frame.height;
    const generation = current.generation;
    current.pending = true;
    current.key = frame.key;

    const snapshots = channels.map((_, channel) => {
      frame.draw(canvas, channel);
      return new Promise<Blob | null>((resolve) => canvas.toBlob(resolve));
    });
    void Promise.all(snapshots).then(async (blobs) => {
      if (generation !== current.generation) return;
      const maps = blobs.filter((blob): blob is Blob => blob !== null);
      const nextUrls = maps.map((blob) => URL.createObjectURL(blob));
      const ready = maps.length === channels.length && await Promise.all(nextUrls.map(async (url) => {
        const image = new Image();
        image.src = url;
        await image.decode();
      })).then(() => true, () => false);

      if (generation !== current.generation || !ready) {
        nextUrls.forEach((url) => URL.revokeObjectURL(url));
        if (generation === current.generation) {
          current.pending = false;
          current.key = "";
        }
        return;
      }

      const previousUrls = current.urls;
      current.urls = nextUrls;
      current.pending = false;
      filter.setAttribute("width", String(frame.width));
      filter.setAttribute("height", String(frame.height));
      filter.querySelectorAll("feImage").forEach((image, index) => {
        image.setAttribute("width", String(frame.width));
        image.setAttribute("height", String(frame.height));
        image.setAttribute("href", current.urls[index]);
      });
      filter.querySelectorAll("feDisplacementMap").forEach((map) => {
        map.setAttribute("scale", String(frame.scale));
      });
      if (current.active) surface.style.backdropFilter = cssFilter;
      previousUrls.forEach((url) => URL.revokeObjectURL(url));
    });
  }

  const layer = enabled && (
    <>
      <svg width={0} height={0} aria-hidden="true" className="absolute pointer-events-none">
        <defs>
          <filter ref={filterRef} id={filterId} x={0} y={0} filterUnits="userSpaceOnUse"
            primitiveUnits="userSpaceOnUse" colorInterpolationFilters="sRGB">
            {channels.map((channel, index) => (
              <Fragment key={channel}>
                <feImage x={0} y={0} preserveAspectRatio="none" result={"map-" + channel} />
                <feDisplacementMap in="SourceGraphic" in2={"map-" + channel} result={"warp-" + channel}
                  xChannelSelector="R" yChannelSelector="G" />
                <feColorMatrix in={"warp-" + channel} values={matrices[index]} result={channel} />
              </Fragment>
            ))}
            <feBlend in="r" in2="g" mode="screen" result="rg" />
            <feBlend in="rg" in2="b" mode="screen" result="rgb" />
            <feComposite in="rgb" in2="map-r" operator="in" />
          </filter>
        </defs>
      </svg>
      <span ref={surfaceRef} aria-hidden="true" className="absolute inset-0 pointer-events-none" />
    </>
  );
  return { layer, publish, clear };
}
