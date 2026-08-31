/**
 * Cloudflare Worker - 合作咨询表单后端参考实现
 * 能力：校验 → 限流 → 发邮件（Resend）→ 返回统一 JSON
 * 部署：wrangler deploy / wrangler dev
 */

type Env = {
  RECEIVER_EMAIL: string;
  RESEND_API_KEY?: string;
  RESEND_FROM?: string;
  ALLOWED_ORIGINS?: string;
  RATE_LIMIT_SECONDS?: string;
  // 可选扩展：FEISHU_WEBHOOK_URL
  FEISHU_WEBHOOK_URL?: string;
};

type CooperationPayload = {
  company: string;
  contact: string;
  phone: string;
  type: string;
  typeLabel?: string;
  message?: string;
  website?: string;
  sourceUrl?: string;
  userAgent?: string;
  timestamp?: string;
};

const PHONE_RE = /^(1[3-9]\d{9}|0\d{2,3}-?\d{7,8})$/;
const ALLOWED_TYPES = new Set(["institution", "community", "home_government", "channel_oem"]);
const TYPE_LABEL_MAP: Record<string, string> = {
  institution: "养老机构合作",
  community: "社区智慧养老",
  home_government: "居家与政务合作",
  channel_oem: "渠道代理 & OEM/ODM",
};

// 内存级限流：IP -> 上次请求时间戳（ms）
const rateLimitStore = new Map<string, number>();

function getClientIp(request: Request): string {
  return (
    request.headers.get("cf-connecting-ip") ||
    request.headers.get("x-forwarded-for")?.split(",")[0]?.trim() ||
    "unknown"
  );
}

function parseAllowedOrigins(env: Env): string[] | null {
  const raw = (env.ALLOWED_ORIGINS || "").trim();
  if (!raw) return null; // 允许所有
  return raw
    .split(",")
    .map((s) => s.trim())
    .filter(Boolean);
}

function isOriginAllowed(origin: string | null, allowed: string[] | null): boolean {
  if (!allowed) return true;
  if (!origin) return false;
  return allowed.includes(origin) || allowed.includes("*");
}

function corsHeaders(origin: string | null, allowed: string[] | null): Record<string, string> {
  const allowOrigin = !allowed ? origin || "*" : isOriginAllowed(origin, allowed) ? origin! : "";
  const headers: Record<string, string> = {
    "Access-Control-Allow-Methods": "POST, OPTIONS",
    "Access-Control-Allow-Headers": "Content-Type",
    "Access-Control-Max-Age": "86400",
    Vary: "Origin",
  };
  if (allowOrigin) headers["Access-Control-Allow-Origin"] = allowOrigin;
  return headers;
}

function jsonResponse(
  body: unknown,
  status: number,
  origin: string | null,
  allowed: string[] | null,
): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: {
      "Content-Type": "application/json; charset=utf-8",
      ...corsHeaders(origin, allowed),
    },
  });
}

function validatePayload(payload: CooperationPayload): string | null {
  if (payload.website && payload.website.trim() !== "") {
    // 蜜罐：视为机器人，但上层静默成功，这里返回 null 让调用方直接成功
    return null;
  }
  const company = (payload.company || "").trim();
  if (!company) return "公司名称为必填";
  if (company.length < 2) return "公司名称至少 2 个字符";
  if (company.length > 50) return "公司名称不能超过 50 个字符";

  const contact = (payload.contact || "").trim();
  if (!contact) return "联系人为必填";
  if (contact.length < 2) return "联系人至少 2 个字符";
  if (contact.length > 20) return "联系人不能超过 20 个字符";

  const phone = (payload.phone || "").trim();
  if (!phone) return "联系电话为必填";
  if (!PHONE_RE.test(phone)) return "联系电话格式不正确";

  const type = (payload.type || "").trim();
  if (!type) return "合作类型为必填";
  if (!ALLOWED_TYPES.has(type)) return "合作类型不合法";

  const message = (payload.message || "").trim();
  if (message.length > 500) return "合作意向说明不能超过 500 个字符";

  return null;
}

async function sendEmailResend(
  env: Env,
  payload: CooperationPayload,
): Promise<{ ok: boolean; error?: string }> {
  const apiKey = env.RESEND_API_KEY;
  if (!apiKey) return { ok: false, error: "RESEND_API_KEY 未配置" };
  const from = env.RESEND_FROM || "万椿官网 <noreply@wanchunsmart.com>";
  const to = env.RECEIVER_EMAIL;
  const typeLabel = payload.typeLabel || TYPE_LABEL_MAP[payload.type] || payload.type;
  const subject = `【万椿官网】合作咨询 - ${payload.company} - ${typeLabel}`;
  const text = [
    `公司名称：${payload.company}`,
    `联系人：${payload.contact}`,
    `联系电话：${payload.phone}`,
    `合作类型：${typeLabel} (${payload.type})`,
    `合作意向：${payload.message || "（未填写）"}`,
    `来源页面：${payload.sourceUrl || "-"}`,
    `User-Agent：${payload.userAgent || "-"}`,
    `提交时间：${payload.timestamp || new Date().toISOString()}`,
  ].join("\n");
  const html = text.replace(/\n/g, "<br>");

  const res = await fetch("https://api.resend.com/emails", {
    method: "POST",
    headers: {
      Authorization: `Bearer ${apiKey}`,
      "Content-Type": "application/json",
    },
    body: JSON.stringify({
      from,
      to: [to],
      subject,
      text,
      html,
    }),
  });
  if (!res.ok) {
    const body = await res.text().catch(() => "");
    return { ok: false, error: `Resend 发送失败: ${res.status} ${body}` };
  }
  return { ok: true };
}

export default {
  async fetch(request: Request, env: Env): Promise<Response> {
    const origin = request.headers.get("Origin");
    const allowed = parseAllowedOrigins(env);
    const cors = corsHeaders(origin, allowed);

    if (request.method === "OPTIONS") {
      if (origin && !isOriginAllowed(origin, allowed)) {
        return new Response(null, { status: 403, headers: cors });
      }
      return new Response(null, { status: 204, headers: cors });
    }

    if (request.method !== "POST") {
      return jsonResponse({ success: false, message: "仅支持 POST" }, 405, origin, allowed);
    }

    if (origin && !isOriginAllowed(origin, allowed)) {
      return jsonResponse({ success: false, message: "Origin 不在白名单" }, 403, origin, allowed);
    }

    const contentType = request.headers.get("content-type") || "";
    if (!contentType.includes("application/json")) {
      return jsonResponse(
        { success: false, message: "Content-Type 需为 application/json" },
        400,
        origin,
        allowed,
      );
    }

    let payload: CooperationPayload;
    try {
      payload = (await request.json()) as CooperationPayload;
    } catch {
      return jsonResponse({ success: false, message: "JSON 解析失败" }, 400, origin, allowed);
    }

    // 蜜罐：有值直接返回成功，不发邮件
    if (payload.website && payload.website.trim() !== "") {
      return jsonResponse({ success: true, message: "提交成功" }, 200, origin, allowed);
    }

    const validationError = validatePayload(payload);
    if (validationError) {
      return jsonResponse({ success: false, message: validationError }, 400, origin, allowed);
    }

    // 限流
    const ip = getClientIp(request);
    const intervalSec = parseInt(env.RATE_LIMIT_SECONDS || "60", 10) || 60;
    const now = Date.now();
    const last = rateLimitStore.get(ip);
    if (last && now - last < intervalSec * 1000) {
      const retryAfter = Math.ceil((intervalSec * 1000 - (now - last)) / 1000);
      return jsonResponse(
        { success: false, message: `提交过于频繁，请 ${retryAfter} 秒后再试` },
        429,
        origin,
        allowed,
      );
    }
    rateLimitStore.set(ip, now);

    if (!env.RECEIVER_EMAIL) {
      return jsonResponse(
        { success: false, message: "服务端未配置接收邮箱" },
        500,
        origin,
        allowed,
      );
    }

    // 发邮件（Resend 优先）
    if (env.RESEND_API_KEY) {
      const result = await sendEmailResend(env, payload);
      if (!result.ok) {
        // eslint-disable-next-line no-console
        console.error(result.error);
        return jsonResponse(
          { success: false, message: "邮件发送失败，请稍后重试" },
          500,
          origin,
          allowed,
        );
      }
    } else {
      // 未配置 Resend 时，仅记录日志（便于调试），返回成功避免阻塞演示
      // 如需 SMTP，请改用自建 Node 服务或在下方扩展 nodemailer 逻辑
      // eslint-disable-next-line no-console
      console.log("Cooperation payload (no email configured):", JSON.stringify(payload));
      // 若要求严格，可返回 500： return jsonResponse({ success: false, message: "邮件服务未配置" }, 500, origin, allowed);
    }

    // 可选：飞书 webhook 扩展（默认注释）
    // if (env.FEISHU_WEBHOOK_URL) {
    //   await fetch(env.FEISHU_WEBHOOK_URL, {
    //     method: "POST",
    //     headers: { "Content-Type": "application/json" },
    //     body: JSON.stringify({ msg_type: "text", content: { text: `新合作咨询：${payload.company} - ${payload.contact} ${payload.phone}` } }),
    //   }).catch(() => {});
    // }

    return jsonResponse({ success: true, message: "提交成功" }, 200, origin, allowed);
  },
};
