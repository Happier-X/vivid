type CooperationType = "institution" | "community" | "home_government" | "channel_oem";

const PHONE_RE = /^(1[3-9]\d{9}|0\d{2,3}-?\d{7,8})$/;

const TYPE_LABEL_MAP: Record<CooperationType, string> = {
  institution: "养老机构合作",
  community: "社区智慧养老",
  home_government: "居家与政务合作",
  channel_oem: "渠道代理 & OEM/ODM",
};

const DEFAULT_RECEIVER = "contact@wanchunsmart.com";
const DEFAULT_SUCCESS_TITLE = "提交成功";
const DEFAULT_SUCCESS_DESC = "我们已收到您的合作意向，商务团队将在 24 小时内与您联系。";

function isHttpUrl(value: string): boolean {
  return /^https?:\/\//.test(value);
}

export function initCooperationForm(): void {
  const root = document.getElementById("cooperation-form-root") as HTMLElement | null;
  if (!root) return;
  // 幂等：避免重复绑定
  if (root.dataset["initialized"] === "1") return;
  root.dataset["initialized"] = "1";

  const form = document.getElementById("cooperation-form") as HTMLFormElement | null;
  const errorBar = document.getElementById("cooperation-error") as HTMLElement | null;
  const noEndpointEl = document.getElementById("cooperation-no-endpoint") as HTMLElement | null;
  const successEl = document.getElementById("cooperation-success") as HTMLElement | null;
  const successTitleEl = document.getElementById("cooperation-success-title") as HTMLElement | null;
  const successDescEl = document.getElementById("cooperation-success-desc") as HTMLElement | null;
  const submitBtn = document.getElementById("cooperation-submit") as HTMLButtonElement | null;
  const resetBtn = document.getElementById("cooperation-reset") as HTMLButtonElement | null;
  const mailtoLink = document.getElementById("cooperation-mailto-link") as HTMLAnchorElement | null;

  if (!form || !submitBtn) return;
  const formEl = form;
  const submitBtnEl = submitBtn;

  const companyInput = formEl.querySelector<HTMLInputElement>('input[name="company"]');
  const contactInput = formEl.querySelector<HTMLInputElement>('input[name="contact"]');
  const phoneInput = formEl.querySelector<HTMLInputElement>('input[name="phone"]');
  const messageInput = formEl.querySelector<HTMLTextAreaElement>('textarea[name="message"]');
  const websiteInput = formEl.querySelector<HTMLInputElement>('input[name="website"]');

  const endpoint = (root.dataset["endpoint"] || "").trim();
  const receiverEmail = (root.dataset["receiverEmail"] || "").trim() || DEFAULT_RECEIVER;
  const successTitle = (root.dataset["successTitle"] || "").trim() || DEFAULT_SUCCESS_TITLE;
  const successDesc = (root.dataset["successDesc"] || "").trim() || DEFAULT_SUCCESS_DESC;

  const hasEndpoint = endpoint.length > 0 && isHttpUrl(endpoint);

  // 同步 mailto 链接
  if (mailtoLink) {
    mailtoLink.href = `mailto:${receiverEmail}`;
    mailtoLink.textContent = `发送邮件至 ${receiverEmail}`;
  }

  // 未配置 endpoint 时的提示处理：显示提示但不阻止表单展示，点击提交时拦截
  const showNoEndpoint = () => {
    if (noEndpointEl) noEndpointEl.classList.remove("hidden");
    if (errorBar) {
      errorBar.textContent = "表单提交功能未配置，请通过右侧电话/邮箱联系";
      errorBar.classList.remove("hidden");
    }
    // 同步 mailto
    if (mailtoLink) mailtoLink.href = `mailto:${receiverEmail}`;
  };

  const hideNoEndpoint = () => {
    if (noEndpointEl) noEndpointEl.classList.add("hidden");
  };

  // 初始：若未配置 endpoint，显示提示（但表单仍可见，提交时拦截）
  if (!hasEndpoint) {
    // 不默认显示，提交时显示；但若想常显可取消注释下一行
    // showNoEndpoint();
  }

  function getSelectedType(): string {
    const checked = formEl.querySelector<HTMLInputElement>('input[name="type"]:checked');
    return checked ? checked.value : "";
  }

  function validateField(name: string, value: string): string | null {
    const trimmed = value.trim();
    switch (name) {
      case "company":
        if (!trimmed) return "请输入公司名称";
        if (trimmed.length < 2) return "公司名称至少 2 个字符";
        if (trimmed.length > 50) return "公司名称不能超过 50 个字符";
        return null;
      case "contact":
        if (!trimmed) return "请输入联系人";
        if (trimmed.length < 2) return "联系人至少 2 个字符";
        if (trimmed.length > 20) return "联系人不能超过 20 个字符";
        return null;
      case "phone":
        if (!trimmed) return "请输入联系电话";
        if (!PHONE_RE.test(trimmed)) return "请输入正确的手机号或座机号";
        return null;
      case "type":
        if (!trimmed) return "请选择合作类型";
        if (!["institution", "community", "home_government", "channel_oem"].includes(trimmed))
          return "合作类型不合法";
        return null;
      case "message":
        if (trimmed.length > 500) return "合作意向说明不能超过 500 个字符";
        return null;
      case "website":
        if (trimmed) return "bot";
        return null;
      default:
        return null;
    }
  }

  function showFieldError(name: string, message: string): void {
    const errEl = formEl.querySelector<HTMLElement>(`[data-error-for="${name}"]`);
    if (errEl) {
      errEl.textContent = message;
      errEl.classList.remove("hidden");
    }
    const input = formEl.querySelector<HTMLElement>(`[name="${name}"]`);
    if (input && name !== "type") {
      input.classList.add("!border-red-400", "border-red-400");
      input.setAttribute("aria-invalid", "true");
    }
    // type 错误高亮整个 fieldset
    if (name === "type") {
      const fieldset = formEl.querySelector("fieldset");
      if (fieldset) fieldset.classList.add("ring-1", "ring-red-200");
    }
  }

  function clearFieldError(name: string): void {
    const errEl = formEl.querySelector<HTMLElement>(`[data-error-for="${name}"]`);
    if (errEl) {
      errEl.textContent = "";
      errEl.classList.add("hidden");
    }
    const input = formEl.querySelector<HTMLElement>(`[name="${name}"]`);
    if (input) {
      input.classList.remove("!border-red-400", "border-red-400");
      input.removeAttribute("aria-invalid");
    }
    if (name === "type") {
      const fieldset = formEl.querySelector("fieldset");
      if (fieldset) fieldset.classList.remove("ring-1", "ring-red-200");
    }
  }

  function clearAllErrors(): void {
    ["company", "contact", "phone", "type", "message"].forEach(clearFieldError);
    if (errorBar) {
      errorBar.textContent = "";
      errorBar.classList.add("hidden");
    }
  }

  function showTopError(message: string): void {
    if (errorBar) {
      errorBar.textContent = message;
      errorBar.classList.remove("hidden");
      errorBar.scrollIntoView({ behavior: "smooth", block: "nearest" });
    }
  }

  function validateAll(): boolean {
    clearAllErrors();
    let valid = true;
    const checks: Array<[string, string]> = [
      ["company", companyInput?.value ?? ""],
      ["contact", contactInput?.value ?? ""],
      ["phone", phoneInput?.value ?? ""],
      ["type", getSelectedType()],
      ["message", messageInput?.value ?? ""],
    ];
    for (const [name, val] of checks) {
      const err = validateField(name, val);
      if (err) {
        showFieldError(name, err);
        valid = false;
      }
    }
    return valid;
  }

  function setLoading(loading: boolean): void {
    submitBtnEl.disabled = loading;
    const textEl = submitBtnEl.querySelector<HTMLElement>('[data-role="btn-text"]');
    const loadingEl = submitBtnEl.querySelector<HTMLElement>('[data-role="btn-loading"]');
    if (loading) {
      textEl?.classList.add("hidden");
      loadingEl?.classList.remove("hidden");
      submitBtnEl.setAttribute("aria-busy", "true");
    } else {
      textEl?.classList.remove("hidden");
      loadingEl?.classList.add("hidden");
      submitBtnEl.removeAttribute("aria-busy");
    }
  }

  function showSuccess(): void {
    if (successTitleEl) successTitleEl.textContent = successTitle;
    if (successDescEl) successDescEl.textContent = successDesc;
    formEl.classList.add("hidden");
    if (errorBar) errorBar.classList.add("hidden");
    hideNoEndpoint();
    successEl?.classList.remove("hidden");
    successEl?.scrollIntoView({ behavior: "smooth", block: "nearest" });
    // 3秒内不自动重置（按 PRD）
  }

  function resetToForm(): void {
    formEl.reset();
    // 恢复默认选中
    const defaultRadio = formEl.querySelector<HTMLInputElement>(
      'input[name="type"][value="institution"]',
    );
    if (defaultRadio) defaultRadio.checked = true;
    clearAllErrors();
    hideNoEndpoint();
    successEl?.classList.add("hidden");
    formEl.classList.remove("hidden");
    // 键盘焦点回到首个输入
    companyInput?.focus();
  }

  // 失焦校验
  companyInput?.addEventListener("blur", () => {
    const err = validateField("company", companyInput.value);
    if (err) showFieldError("company", err);
    else clearFieldError("company");
  });
  contactInput?.addEventListener("blur", () => {
    const err = validateField("contact", contactInput.value);
    if (err) showFieldError("contact", err);
    else clearFieldError("contact");
  });
  phoneInput?.addEventListener("blur", () => {
    const err = validateField("phone", phoneInput.value);
    if (err) showFieldError("phone", err);
    else clearFieldError("phone");
  });
  messageInput?.addEventListener("blur", () => {
    const err = validateField("message", messageInput.value);
    if (err) showFieldError("message", err);
    else clearFieldError("message");
  });
  // 输入时清除错误
  companyInput?.addEventListener("input", () => clearFieldError("company"));
  contactInput?.addEventListener("input", () => clearFieldError("contact"));
  phoneInput?.addEventListener("input", () => clearFieldError("phone"));
  messageInput?.addEventListener("input", () => clearFieldError("message"));
  formEl.querySelectorAll<HTMLInputElement>('input[name="type"]').forEach((radio) => {
    radio.addEventListener("change", () => clearFieldError("type"));
  });

  formEl.addEventListener("submit", async (e) => {
    e.preventDefault();
    clearAllErrors();
    hideNoEndpoint();

    // 蜜罐：有值静默成功
    if (websiteInput && websiteInput.value.trim() !== "") {
      showSuccess();
      return;
    }

    if (!validateAll()) {
      const firstErr = formEl.querySelector<HTMLElement>("[data-error-for]:not(.hidden)");
      firstErr?.scrollIntoView({ behavior: "smooth", block: "nearest" });
      return;
    }

    if (!hasEndpoint) {
      showNoEndpoint();
      return;
    }

    const typeVal = getSelectedType() as CooperationType;
    const payload = {
      company: (companyInput?.value ?? "").trim(),
      contact: (contactInput?.value ?? "").trim(),
      phone: (phoneInput?.value ?? "").trim(),
      type: typeVal,
      typeLabel: TYPE_LABEL_MAP[typeVal] ?? typeVal,
      message: (messageInput?.value ?? "").trim(),
      website: websiteInput?.value ?? "",
      sourceUrl: window.location.href,
      userAgent: navigator.userAgent,
      timestamp: new Date().toISOString(),
    };

    setLoading(true);
    try {
      const res = await fetch(endpoint, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload),
        signal: AbortSignal.timeout(10000),
      });
      let data: { success?: boolean; message?: string } = {};
      try {
        data = (await res.json()) as typeof data;
      } catch {
        data = {};
      }
      if (res.ok && data.success !== false) {
        showSuccess();
      } else {
        showTopError(data.message || "提交失败，请稍后重试或通过电话/邮箱联系");
      }
    } catch (err) {
      const isTimeout =
        (err instanceof DOMException && err.name === "TimeoutError") ||
        (err instanceof Error && err.name === "AbortError");
      if (isTimeout) {
        showTopError("请求超时，请检查网络后重试或通过电话/邮箱联系");
      } else {
        showTopError("网络异常，请检查网络后重试或通过电话/邮箱联系");
      }
    } finally {
      setLoading(false);
    }
  });

  resetBtn?.addEventListener("click", () => {
    resetToForm();
  });

  // Esc 关闭成功态回到表单
  document.addEventListener("keydown", (e) => {
    if (e.key === "Escape" && successEl && !successEl.classList.contains("hidden")) {
      resetToForm();
    }
  });

  // 无 JS 降级已由 noscript 处理；有 JS 时确保表单可见
  formEl.classList.remove("hidden");
}
