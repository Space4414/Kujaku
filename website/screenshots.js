(() => {
  "use strict";

  /* ============  EDIT THIS PART  ============
     Images live in  website/screenshots/  (already compressed to WebP).
     List them here in the order you want them shown.
       src      file name inside website/screenshots/
       ratio    width/height of the image, so nothing gets cropped
       alt      short description (for screen readers)
       caption  text shown under the slider ("" for none)
       wide     true for landscape images such as the Discord card          */
  const BASE = "/screenshots/";
  const SHOTS = [
    { src: "1.webp", ratio: "720/1151", alt: "Kujaku home screen with profile stats and Continue Watching", caption: "Home: your stats and Continue Watching" },
    { src: "2.webp", ratio: "720/1149", alt: "Kujaku manga home with search, genres and top scores", caption: "Manga home: search, genres and top scores" },
    { src: "3.webp", ratio: "720/1162", alt: "Kujaku anime home with seasons and calendar", caption: "Anime home: seasons and calendar" },
    { src: "4.webp", ratio: "720/1280", alt: "Kujaku manga reader", caption: "The manga reader" },
    { src: "5.webp", ratio: "720/1147", alt: "Kujaku manga details page with reading progress", caption: "Details: reading progress and AniList tracking" },
    { src: "6.webp", ratio: "720/1139", alt: "Built-in Kotatsu sources list", caption: "Built-in Kotatsu sources, no extensions to install" },
    { src: "7.webp", ratio: "720/1138", alt: "Trending manga and manhwa in Kujaku", caption: "Trending manga and manhwa" },
    { src: "8.webp", ratio: "720/1140", alt: "AniList list editor in Kujaku", caption: "List editor synced with AniList" },
    { src: "9.webp", ratio: "1080/370", alt: "Kujaku Discord Rich Presence card", caption: "Discord Rich Presence shows what you're reading", wide: true }
  ];
  /* ========================================= */

  const section = document.getElementById("screenshots");
  if (!section) return;
  const track = section.querySelector(".shots-track");
  const prevBtn = section.querySelector(".shots-btn.prev");
  const nextBtn = section.querySelector(".shots-btn.next");
  const dotsHost = section.querySelector(".shots-dots");
  const captionEl = section.querySelector(".shots-caption");
  if (!track || !SHOTS.length) return;

  const reduceMotion =
    window.matchMedia && window.matchMedia("(prefers-reduced-motion: reduce)").matches;
  const slides = [];
  const dots = [];
  let active = 0;
  let ticking = false;
  let moved = false;

  SHOTS.forEach((shot, i) => {
    const fig = document.createElement("figure");
    fig.className = "shot" + (shot.wide ? " wide" : "");
    fig.setAttribute("role", "group");
    fig.setAttribute("aria-roledescription", "slide");
    fig.setAttribute("aria-label", i + 1 + " of " + SHOTS.length);

    if (shot.ratio) fig.style.aspectRatio = shot.ratio;

    const img = new Image();
    img.src = BASE + shot.src;
    img.alt = shot.alt || "Kujaku screenshot " + (i + 1);
    img.decoding = "async";
    img.loading = i < 3 ? "eager" : "lazy";
    img.draggable = false;
    img.addEventListener("error", () => fig.classList.add("is-missing"));
    fig.appendChild(img);
    fig.addEventListener("click", () => {
      if (!moved) goTo(i);
    });
    track.appendChild(fig);
    slides.push(fig);

    if (dotsHost) {
      const dot = document.createElement("button");
      dot.type = "button";
      dot.className = "shots-dot";
      dot.setAttribute("aria-label", "Go to screenshot " + (i + 1));
      dot.addEventListener("click", () => goTo(i));
      dotsHost.appendChild(dot);
      dots.push(dot);
    }
  });

  function goTo(index, smooth = true) {
    const i = Math.max(0, Math.min(slides.length - 1, index));
    const el = slides[i];
    const left = el.offsetLeft - (track.clientWidth - el.offsetWidth) / 2;
    track.scrollTo({
      left: Math.max(0, left),
      behavior: smooth && !reduceMotion ? "smooth" : "auto"
    });
  }

  function setActive(i) {
    active = i;
    slides.forEach((el, n) => el.classList.toggle("is-active", n === i));
    dots.forEach((d, n) => d.setAttribute("aria-current", n === i ? "true" : "false"));
    if (prevBtn) prevBtn.disabled = i === 0;
    if (nextBtn) nextBtn.disabled = i === slides.length - 1;
    if (captionEl) captionEl.textContent = SHOTS[i].caption || "";
  }

  function update() {
    ticking = false;
    const mid = track.scrollLeft + track.clientWidth / 2;
    let best = 0;
    let bestDist = Infinity;
    slides.forEach((el, i) => {
      const dist = Math.abs(el.offsetLeft + el.offsetWidth / 2 - mid);
      if (dist < bestDist) {
        bestDist = dist;
        best = i;
      }
    });
    setActive(best);
  }

  track.addEventListener(
    "scroll",
    () => {
      if (!ticking) {
        ticking = true;
        requestAnimationFrame(update);
      }
    },
    { passive: true }
  );

  if (prevBtn) prevBtn.addEventListener("click", () => goTo(active - 1));
  if (nextBtn) nextBtn.addEventListener("click", () => goTo(active + 1));

  // keyboard: arrows, Home, End while the slider is focused
  track.addEventListener("keydown", (e) => {
    if (e.key === "ArrowRight") { e.preventDefault(); goTo(active + 1); }
    else if (e.key === "ArrowLeft") { e.preventDefault(); goTo(active - 1); }
    else if (e.key === "Home") { e.preventDefault(); goTo(0); }
    else if (e.key === "End") { e.preventDefault(); goTo(slides.length - 1); }
  });

  // mouse drag (touch screens already swipe natively)
  let down = false;
  let startX = 0;
  let startLeft = 0;
  let captured = false;
  track.addEventListener("pointerdown", (e) => {
    if (e.pointerType !== "mouse" || e.button !== 0) return;
    down = true;
    moved = false;
    captured = false;
    startX = e.clientX;
    startLeft = track.scrollLeft;
  });
  track.addEventListener("pointermove", (e) => {
    if (!down) return;
    const dx = e.clientX - startX;
    if (!moved && Math.abs(dx) > 5) {
      moved = true;
      track.classList.add("is-dragging");
      try { track.setPointerCapture(e.pointerId); captured = true; } catch (_) {}
    }
    if (moved) track.scrollLeft = startLeft - dx;
  });
  const endDrag = (e) => {
    if (!down) return;
    down = false;
    track.classList.remove("is-dragging");
    if (captured) {
      try { track.releasePointerCapture(e.pointerId); } catch (_) {}
    }
    if (moved) {
      update();
      goTo(active);
    }
    setTimeout(() => { moved = false; }, 0);
  };
  track.addEventListener("pointerup", endDrag);
  track.addEventListener("pointercancel", endDrag);
  track.addEventListener("pointerleave", (e) => { if (down && moved) endDrag(e); });

  window.addEventListener("resize", () => goTo(active, false));
  setActive(0);
})();
