import "../css/main.css";

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
