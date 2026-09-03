import type { Cooperation, ListResult } from "../types/cooperation";

const BASE = "/apis/api.cooperation.vivid.run/v1alpha1/cooperations";

function buildQuery(params: Record<string, string | number | undefined>) {
  const sp = new URLSearchParams();
  Object.entries(params).forEach(([k, v]) => {
    if (v !== undefined && v !== "" && v !== null) sp.set(k, String(v));
  });
  const q = sp.toString();
  return q ? `?${q}` : "";
}

async function request<T>(url: string, opts: RequestInit = {}): Promise<T> {
  const res = await fetch(url, {
    credentials: "include",
    headers: {
      "Content-Type": "application/json",
      ...((opts.headers as Record<string, string>) || {}),
    },
    ...opts,
  });
  if (res.status === 401) {
    throw Object.assign(new Error("未登录"), { status: 401 });
  }
  if (res.status === 403) {
    throw Object.assign(new Error("无权限"), { status: 403 });
  }
  if (!res.ok) {
    const text = await res.text().catch(() => "");
    throw Object.assign(new Error(text || `请求失败 ${res.status}`), { status: res.status });
  }
  const ct = res.headers.get("content-type") || "";
  if (ct.includes("application/json")) {
    return (await res.json()) as T;
  }
  return (await res.text()) as unknown as T;
}

export interface ListParams {
  page: number;
  size: number;
  keyword?: string;
  type?: string;
  handled?: string;
  startTime?: string;
  endTime?: string;
}

export function listCooperations(params: ListParams) {
  const q = buildQuery({
    page: params.page,
    size: params.size,
    keyword: params.keyword,
    type: params.type,
    handled: params.handled,
    startTime: params.startTime,
    endTime: params.endTime,
  });
  return request<ListResult<Cooperation>>(`${BASE}${q}`);
}

export function getCooperation(name: string) {
  return request<Cooperation>(`${BASE}/${encodeURIComponent(name)}`);
}

export function deleteCooperation(name: string) {
  return request<{ success: boolean; message: string }>(`${BASE}/${encodeURIComponent(name)}`, {
    method: "DELETE",
  });
}

export function updateHandled(name: string, handled: boolean) {
  return request<Cooperation>(`${BASE}/${encodeURIComponent(name)}/handled`, {
    method: "PUT",
    body: JSON.stringify({ handled }),
  });
}

export async function exportCooperations(params: Omit<ListParams, "page" | "size">) {
  const q = buildQuery({
    keyword: params.keyword,
    type: params.type,
    handled: params.handled,
    startTime: params.startTime,
    endTime: params.endTime,
  });
  const url = `${BASE}/export${q}`;
  const res = await fetch(url, { credentials: "include" });
  if (res.status === 401) throw Object.assign(new Error("未登录"), { status: 401 });
  if (res.status === 403) throw Object.assign(new Error("无权限"), { status: 403 });
  if (!res.ok) throw new Error(`导出失败 ${res.status}`);
  return await res.blob();
}
