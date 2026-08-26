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

// 移动端菜单开合
const menuToggle = document.getElementById("menu-toggle");
const mobileMenu = document.getElementById("mobile-menu");

if (menuToggle && mobileMenu) {
  const iconOpen = document.getElementById("icon-open");
  const iconClose = document.getElementById("icon-close");
  let open = false;

  menuToggle.addEventListener("click", () => {
    open = !open;
    mobileMenu.classList.toggle("hidden", !open);
    iconOpen?.classList.toggle("hidden", open);
    iconClose?.classList.toggle("hidden", !open);
    menuToggle.setAttribute("aria-expanded", String(open));
  });

  // 跳转后自动收起移动端菜单
  mobileMenu.addEventListener("click", (event) => {
    if ((event.target as HTMLElement).closest("a") && open) {
      menuToggle.click();
    }
  });
}

// 导航吸顶阴影：页面滚动后为顶栏添加投影
const header = document.getElementById("site-header");
if (header) {
  const onScroll = () => {
    header.classList.toggle("shadow-md", window.scrollY > 8);
  };
  window.addEventListener("scroll", onScroll, { passive: true });
  onScroll();
}
