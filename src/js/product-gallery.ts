/**
 * 产品详情轻量交互：图库轮播 + 产品介绍卡片化 + 规格表搬运
 * 约定式内容：正文中的 ## 产品介绍 / ## 规格参数 / ## 图库 为结构化数据源
 * 若未按约定书写，对应区块由 Thymeleaf th:if 已隐藏，此脚本再做二次隐藏兜底
 */
export function initProductGallery(): void {
  const rich = document.querySelector(".rich-content") as HTMLElement | null;
  if (!rich) return;

  const isProduct = document.getElementById("product-gallery-block") !== null;
  if (!isProduct) return;

  // --- 1. 产品介绍：提取 ul -> 卡片 ---
  const highlightsSection = document.getElementById("product-highlights");
  const highlightsGrid = document.getElementById("product-highlights-grid");
  if (highlightsSection && highlightsGrid) {
    const h = findHeading(rich, "产品介绍");
    if (h) {
      const ul = findNextSiblingByTag(h, "UL");
      if (ul) {
        const items = Array.from(ul.querySelectorAll("li"));
        if (items.length > 0) {
          highlightsGrid.innerHTML = "";
          items.forEach((li) => {
            const card = document.createElement("div");
            card.className = "product-highlight-card text-ink-700 text-sm leading-6";
            card.textContent = (li.textContent || "").trim();
            highlightsGrid.appendChild(card);
          });
          // 隐藏原文的标题与列表，避免重复
          h.style.display = "none";
          ul.style.display = "none";
        } else {
          highlightsSection.style.display = "none";
        }
      } else {
        highlightsSection.style.display = "none";
      }
    } else {
      highlightsSection.style.display = "none";
    }
    // 若仍为空，隐藏整块
    if (highlightsGrid.children.length === 0) {
      highlightsSection.style.display = "none";
    }
  }

  // --- 2. 规格参数：提取 table -> 规格容器 ---
  const specsSection = document.getElementById("product-specs");
  const specsTableWrap = document.getElementById("product-specs-table");
  if (specsSection && specsTableWrap) {
    const h = findHeading(rich, "规格参数");
    if (h) {
      const table = findNextSiblingByTag(h, "TABLE");
      if (table) {
        // 克隆并搬运，保留原表格样式
        const clone = table.cloneNode(true) as HTMLElement;
        specsTableWrap.innerHTML = "";
        specsTableWrap.appendChild(clone);
        // 隐藏原文
        h.style.display = "none";
        table.style.display = "none";
      } else {
        specsSection.style.display = "none";
      }
    } else {
      specsSection.style.display = "none";
    }
    if (!specsTableWrap.firstElementChild) {
      specsSection.style.display = "none";
    }
  }

  // --- 3. 图库：收集 ## 图库 后的所有 img + cover，构建轮播 ---
  const galleryBlock = document.getElementById("product-gallery-block");
  const mainImg = document.getElementById("product-gallery-main") as HTMLImageElement | null;
  const thumbsWrap = document.getElementById("product-gallery-thumbs") as HTMLElement | null;
  const prevBtn = document.getElementById("product-gallery-prev") as HTMLButtonElement | null;
  const nextBtn = document.getElementById("product-gallery-next") as HTMLButtonElement | null;
  const placeholder = document.getElementById("product-gallery-placeholder") as HTMLElement | null;

  if (galleryBlock && mainImg && thumbsWrap) {
    const sources: string[] = [];
    const cover = mainImg.getAttribute("src");
    if (cover && cover.trim() !== "") sources.push(cover);

    const galleryHeading = findHeading(rich, "图库");
    if (galleryHeading) {
      // 收集该标题后、下一标题前的所有 img
      let el: Element | null = galleryHeading.nextElementSibling;
      while (el && !/^H[1-6]$/.test(el.tagName)) {
        const imgs =
          el.tagName === "IMG" ? [el as HTMLImageElement] : Array.from(el.querySelectorAll("img"));
        imgs.forEach((img) => {
          const src = img.getAttribute("src");
          if (src && !sources.includes(src)) sources.push(src);
        });
        el = el.nextElementSibling;
      }
      // 隐藏原文图库区块（标题 + 图片）
      galleryHeading.style.display = "none";
      let hideEl: Element | null = galleryHeading.nextElementSibling;
      // 先收集再隐藏，避免循环干扰；这里隐藏直到下一标题
      const toHide: Element[] = [];
      let cur: Element | null = galleryHeading.nextElementSibling;
      while (cur && !/^H[1-6]$/.test(cur.tagName)) {
        toHide.push(cur);
        cur = cur.nextElementSibling;
      }
      toHide.forEach((node) => {
        if (node.querySelector("img") || node.tagName === "IMG" || node.tagName === "P") {
          // 仅隐藏包含图片的段落，保留可能的文字说明
          const hasImg = node.tagName === "IMG" || node.querySelector("img");
          if (hasImg) (node as HTMLElement).style.display = "none";
        }
      });
      void hideEl;
    } else {
      // 无图库约定：若仅有 cover，保留单图；否则靠 cover 单图展示
      // 不额外隐藏
    }

    // 去重后构建缩略图
    thumbsWrap.innerHTML = "";
    sources.forEach((src, idx) => {
      const btn = document.createElement("button");
      btn.type = "button";
      btn.className =
        "product-thumb h-16 w-16 shrink-0 overflow-hidden rounded-[8px] border-2" +
        (idx === 0 ? " is-active border-primary" : " border-transparent");
      btn.dataset.src = src;
      const img = document.createElement("img");
      img.src = src;
      img.alt = "产品图 " + (idx + 1);
      img.className = "h-full w-full object-cover";
      img.loading = "lazy";
      btn.appendChild(img);
      btn.addEventListener("click", () => setActive(idx));
      thumbsWrap.appendChild(btn);
    });

    // 若有图库图片，显示主图并隐藏占位
    if (sources.length > 0) {
      mainImg.style.display = "";
      if (placeholder) placeholder.style.display = "none";
    }

    let active = 0;
    function setActive(idx: number) {
      active = idx;
      const src = sources[idx];
      if (src) {
        mainImg!.src = src;
        mainImg!.style.display = "";
        if (placeholder) placeholder.style.display = "none";
      }
      Array.from(thumbsWrap!.children).forEach((c, i) => {
        c.classList.toggle("is-active", i === idx);
        (c as HTMLElement).classList.toggle("border-primary", i === idx);
        (c as HTMLElement).classList.toggle("border-transparent", i !== idx);
      });
    }

    if (sources.length <= 1) {
      thumbsWrap.style.display = "none";
      if (prevBtn) prevBtn.style.display = "none";
      if (nextBtn) nextBtn.style.display = "none";
    } else {
      if (prevBtn) prevBtn.classList.remove("hidden");
      if (nextBtn) nextBtn.classList.remove("hidden");
      prevBtn?.addEventListener("click", () =>
        setActive((active - 1 + sources.length) % sources.length),
      );
      nextBtn?.addEventListener("click", () => setActive((active + 1) % sources.length));
    }

    // 若无任何图片（cover 也无），隐藏整个图库
    if (sources.length === 0 || (sources.length === 1 && !sources[0])) {
      galleryBlock.style.display = "none";
    }
  }

  // --- 4. 相关产品空状态兜底：过滤当前篇后若无可见卡片，隐藏整个相关区块标题 ---
  const relatedSection = document.getElementById("related-products");
  if (relatedSection) {
    const visibleCards = relatedSection.querySelectorAll("a[href]");
    // 统计实际渲染的可见卡片（th:if 过滤后未渲染的不在 DOM 中）
    if (visibleCards.length === 0) {
      relatedSection.style.display = "none";
    } else {
      // 若 card 数为 0 但标题仍在，标题已在 Thymeleaf 层有内容，此处确保 grid 不留空标题
      const grid = relatedSection.querySelector(".grid");
      if (grid && grid.children.length === 0) relatedSection.style.display = "none";
    }
  }

  // --- 5. 富文本内残留的约定标题，若上述已处理过则隐藏，避免重复 ---
  // 已在各自分支隐藏，此处兜底：若 isProduct 且包含该关键词但未成功提取，整块已隐藏
}

function findHeading(root: HTMLElement, keyword: string): HTMLElement | null {
  const headings = root.querySelectorAll("h1, h2, h3, h4, h5, h6");
  for (const h of Array.from(headings)) {
    if ((h.textContent || "").includes(keyword)) return h as HTMLElement;
  }
  return null;
}

function findNextSiblingByTag(start: Element, tag: string): HTMLElement | null {
  const target = tag.toUpperCase();
  let el: Element | null = start.nextElementSibling;
  while (el) {
    if (el.tagName === target) return el as HTMLElement;
    // 若中间有空文本或段落，继续找；遇到下一标题则停止
    if (/^H[1-6]$/.test(el.tagName)) return null;
    // 若 el 内部包含目标标签（如 p 包裹的 table  unlikely），也尝试
    const inner = el.querySelector(tag);
    if (inner) return inner as HTMLElement;
    el = el.nextElementSibling;
  }
  return null;
}
