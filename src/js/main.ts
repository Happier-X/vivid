import "../css/main.css";

// 标记 JS 已启用：滚动入场动画仅在 .js 前缀下隐藏元素（无 JS 降级可见）
document.documentElement.classList.add("js");

// 滚动入场动画：进入视口的 .reveal / .reveal-left / .reveal-right 加 .is-visible
const revealSelector = ".reveal, .reveal-left, .reveal-right";
const prefersReducedMotion = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
if (!prefersReducedMotion) {
  const observer = new IntersectionObserver(
    (entries) => {
      for (const entry of entries) {
        if (entry.isIntersecting) {
          entry.target.classList.add("is-visible");
          observer.unobserve(entry.target);
        }
      }
    },
    { threshold: 0.15 },
  );
  document.querySelectorAll(revealSelector).forEach((el) => {
    observer.observe(el);
  });
}

// 移动端菜单开合 — 同步切换 header 的 is-menu-open（与 mikapu 行为一致）
const menuToggle = document.getElementById("menu-toggle") as HTMLButtonElement | null;
const mobileMenu = document.getElementById("mobile-menu");
const header = document.getElementById("site-header");

if (menuToggle && mobileMenu) {
  let open = false;
  const syncMenu = () => {
    mobileMenu.classList.toggle("hidden", !open);
    menuToggle.setAttribute("aria-expanded", String(open));
    header?.classList.toggle("is-menu-open", open);
  };
  menuToggle.addEventListener("click", () => {
    open = !open;
    syncMenu();
  });
  mobileMenu.addEventListener("click", (event) => {
    if ((event.target as HTMLElement).closest("a") && open) {
      open = false;
      syncMenu();
    }
  });
}

// 固定头滚动态：滚动 >8px 时加 is-scrolled（复刻 mikapu 的透明->毛玻璃切换）
if (header) {
  const onScroll = () => {
    header.classList.toggle("is-scrolled", window.scrollY > 8);
  };
  window.addEventListener("scroll", onScroll, { passive: true });
  onScroll();
}
