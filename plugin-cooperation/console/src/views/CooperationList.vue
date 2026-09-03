<template>
  <div class="cooperation-list p-4">
    <!-- 顶部筛选栏 -->
    <div class="mb-4 flex flex-wrap items-end gap-3 rounded border bg-white p-4">
      <div class="flex flex-col gap-1">
        <label class="text-sm text-gray-600">关键词</label>
        <input
          v-model="filters.keyword"
          placeholder="搜索 公司/联系人/电话"
          class="input input-sm w-52 rounded border px-3 py-1.5"
          @keyup.enter="handleSearch"
        />
      </div>
      <div class="flex flex-col gap-1">
        <label class="text-sm text-gray-600">合作类型</label>
        <select v-model="filters.type" class="min-w-[160px] rounded border px-3 py-1.5 text-sm">
          <option v-for="opt in typeOptions" :key="opt.value" :value="opt.value">
            {{ opt.label }}
          </option>
        </select>
      </div>
      <div class="flex flex-col gap-1">
        <label class="text-sm text-gray-600">处理状态</label>
        <select v-model="filters.handled" class="min-w-[120px] rounded border px-3 py-1.5 text-sm">
          <option v-for="opt in handledOptions" :key="opt.value" :value="opt.value">
            {{ opt.label }}
          </option>
        </select>
      </div>
      <div class="flex flex-col gap-1">
        <label class="text-sm text-gray-600">开始时间</label>
        <input v-model="filters.startTime" type="date" class="rounded border px-3 py-1.5 text-sm" />
      </div>
      <div class="flex flex-col gap-1">
        <label class="text-sm text-gray-600">结束时间</label>
        <input v-model="filters.endTime" type="date" class="rounded border px-3 py-1.5 text-sm" />
      </div>
      <div class="ml-auto flex gap-2">
        <button
          class="btn btn-primary rounded bg-[#4fc7b7] px-4 py-1.5 text-sm text-white"
          :disabled="loading"
          @click="handleSearch"
        >
          搜索
        </button>
        <button class="btn rounded border px-4 py-1.5 text-sm" @click="handleReset">重置</button>
        <button
          class="btn rounded border px-4 py-1.5 text-sm"
          :class="
            total === 0
              ? 'cursor-not-allowed bg-gray-100 text-gray-400'
              : 'bg-white hover:bg-gray-50'
          "
          :disabled="total === 0 || exporting"
          @click="handleExport"
        >
          {{ exporting ? "导出中..." : "导出 CSV" }}
        </button>
      </div>
    </div>

    <!-- 加载中 -->
    <div v-if="loading" class="py-16 text-center text-gray-500">
      <div class="inline-block h-8 w-8 animate-spin rounded-full border-b-2 border-[#4fc7b7]"></div>
      <p class="mt-2 text-sm">加载中...</p>
    </div>

    <!-- 空状态 -->
    <div v-else-if="list.length === 0" class="rounded border bg-white py-16 text-center">
      <div class="mb-3 text-4xl">📋</div>
      <p class="font-medium text-gray-700">暂无合作咨询数据</p>
      <p class="mt-1 text-sm text-gray-500">请检查合作表单是否已发布，或尝试调整筛选条件</p>
      <p class="mt-2 text-xs text-gray-400">
        表单提交地址：/apis/api.cooperation.vivid.run/v1alpha1/cooperations
      </p>
    </div>

    <!-- 表格 -->
    <div v-else class="overflow-hidden rounded border bg-white">
      <div class="overflow-x-auto">
        <table class="w-full text-sm">
          <thead class="bg-gray-50 text-gray-600">
            <tr>
              <th class="px-3 py-2.5 text-left font-medium whitespace-nowrap">公司名称</th>
              <th class="px-3 py-2.5 text-left font-medium whitespace-nowrap">联系人</th>
              <th class="px-3 py-2.5 text-left font-medium whitespace-nowrap">联系电话</th>
              <th class="px-3 py-2.5 text-left font-medium whitespace-nowrap">合作类型</th>
              <th class="min-w-[200px] px-3 py-2.5 text-left font-medium">合作意向</th>
              <th class="px-3 py-2.5 text-left font-medium whitespace-nowrap">提交时间</th>
              <th class="px-3 py-2.5 text-left font-medium whitespace-nowrap">IP</th>
              <th class="px-3 py-2.5 text-left font-medium whitespace-nowrap">来源页面</th>
              <th class="px-3 py-2.5 text-left font-medium whitespace-nowrap">状态</th>
              <th class="px-3 py-2.5 text-center font-medium whitespace-nowrap">操作</th>
            </tr>
          </thead>
          <tbody class="divide-y">
            <tr
              v-for="item in list"
              :key="item.metadata.name"
              class="cursor-pointer hover:bg-gray-50"
              @click="openDetail(item)"
            >
              <td class="max-w-[150px] truncate px-3 py-2.5 font-medium" :title="item.spec.company">
                {{ item.spec.company }}
              </td>
              <td class="px-3 py-2.5 whitespace-nowrap">{{ item.spec.contact }}</td>
              <td class="px-3 py-2.5 whitespace-nowrap">{{ item.spec.phone }}</td>
              <td class="px-3 py-2.5 whitespace-nowrap">
                <span
                  class="inline-flex rounded border border-[#d9ece9] bg-[#eef7f7] px-2 py-0.5 text-xs text-[#246d74]"
                  >{{ item.spec.typeLabel || typeLabel(item.spec.type) }}</span
                >
              </td>
              <td
                class="max-w-[200px] truncate px-3 py-2.5 text-gray-600"
                :title="item.spec.message"
              >
                {{ truncate(item.spec.message, 36) }}
              </td>
              <td class="px-3 py-2.5 text-xs whitespace-nowrap text-gray-600">
                {{ formatTime(item.metadata.creationTimestamp) }}
              </td>
              <td class="px-3 py-2.5 text-xs whitespace-nowrap">{{ item.spec.ip || "-" }}</td>
              <td class="max-w-[120px] truncate px-3 py-2.5" :title="item.spec.sourceUrl">
                <a
                  v-if="item.spec.sourceUrl"
                  :href="item.spec.sourceUrl"
                  target="_blank"
                  class="text-xs text-[#4fc7b7] hover:underline"
                  @click.stop
                  >来源</a
                >
                <span v-else class="text-xs text-gray-400">-</span>
              </td>
              <td class="px-3 py-2.5 whitespace-nowrap">
                <span
                  class="inline-flex rounded px-2 py-0.5 text-xs font-medium"
                  :class="
                    item.spec.handled
                      ? 'border border-green-200 bg-green-50 text-green-700'
                      : 'border border-amber-200 bg-amber-50 text-amber-700'
                  "
                >
                  {{ item.spec.handled ? "已处理" : "未处理" }}
                </span>
              </td>
              <td class="px-3 py-2.5 text-center whitespace-nowrap" @click.stop>
                <button
                  class="mr-1 rounded border bg-white px-2 py-1 text-xs hover:bg-gray-50"
                  @click="openDetail(item)"
                >
                  查看
                </button>
                <button
                  class="mr-1 rounded border px-2 py-1 text-xs"
                  :class="
                    item.spec.handled ? 'bg-gray-50' : 'border-[#4fc7b7] bg-[#4fc7b7] text-white'
                  "
                  @click="toggleHandled(item)"
                >
                  {{ item.spec.handled ? "标为未处理" : "标为已处理" }}
                </button>
                <button
                  class="rounded border border-red-200 bg-red-50 px-2 py-1 text-xs text-red-600 hover:bg-red-100"
                  @click="confirmDelete(item)"
                >
                  删除
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- 分页 -->
      <div class="flex items-center justify-between border-t bg-white px-4 py-3">
        <div class="text-sm text-gray-600">
          共 <span class="font-medium text-gray-900">{{ total }}</span> 条
          <span class="ml-3">
            每页
            <select
              v-model.number="pageSize"
              class="mx-1 rounded border px-2 py-1 text-sm"
              @change="handleSizeChange"
            >
              <option :value="10">10</option>
              <option :value="20">20</option>
              <option :value="50">50</option>
            </select>
            条
          </span>
        </div>
        <div class="flex items-center gap-1">
          <button
            class="rounded border px-3 py-1 text-sm"
            :disabled="page === 0"
            :class="page === 0 ? 'bg-gray-100 text-gray-400' : 'hover:bg-gray-50'"
            @click="goPage(page - 1)"
          >
            上一页
          </button>
          <span class="px-2 text-sm">第 {{ page + 1 }} / {{ totalPages || 1 }} 页</span>
          <button
            class="rounded border px-3 py-1 text-sm"
            :disabled="page + 1 >= totalPages"
            :class="page + 1 >= totalPages ? 'bg-gray-100 text-gray-400' : 'hover:bg-gray-50'"
            @click="goPage(page + 1)"
          >
            下一页
          </button>
        </div>
      </div>
    </div>

    <!-- 详情抽屉/弹窗 -->
    <div v-if="detailVisible" class="fixed inset-0 z-50 flex">
      <div class="flex-1 bg-black/30" @click="detailVisible = false"></div>
      <div class="w-[520px] max-w-[90vw] overflow-y-auto bg-white shadow-xl">
        <div class="sticky top-0 flex items-center justify-between border-b bg-white px-5 py-3">
          <h3 class="font-semibold">合作详情</h3>
          <button class="rounded p-1 hover:bg-gray-100" @click="detailVisible = false">✕</button>
        </div>
        <div v-if="current" class="space-y-4 p-5">
          <div class="flex gap-2">
            <span
              class="inline-flex rounded px-2.5 py-1 text-xs font-medium"
              :class="
                current.spec.handled
                  ? 'border border-green-200 bg-green-50 text-green-700'
                  : 'border border-amber-200 bg-amber-50 text-amber-700'
              "
              >{{ current.spec.handled ? "已处理" : "未处理" }}</span
            >
            <span
              class="inline-flex rounded border bg-[#eef7f7] px-2.5 py-1 text-xs text-[#246d74]"
              >{{ current.spec.typeLabel || typeLabel(current.spec.type) }}</span
            >
          </div>
          <div class="grid gap-3 text-sm">
            <div class="flex">
              <span class="w-24 shrink-0 text-gray-500">公司名称</span
              ><span class="flex-1 font-medium">{{ current.spec.company }}</span>
            </div>
            <div class="flex">
              <span class="w-24 shrink-0 text-gray-500">联系人</span
              ><span class="flex-1">{{ current.spec.contact }}</span>
            </div>
            <div class="flex">
              <span class="w-24 shrink-0 text-gray-500">联系电话</span
              ><span class="flex-1">{{ current.spec.phone }}</span>
            </div>
            <div class="flex">
              <span class="w-24 shrink-0 text-gray-500">合作类型</span
              ><span class="flex-1">{{ current.spec.type }}（{{ current.spec.typeLabel }}）</span>
            </div>
            <div class="flex">
              <span class="w-24 shrink-0 text-gray-500">合作意向</span
              ><span class="flex-1 rounded border bg-gray-50 p-3 break-words whitespace-pre-wrap">{{
                current.spec.message || "（未填写）"
              }}</span>
            </div>
            <div class="flex">
              <span class="w-24 shrink-0 text-gray-500">来源页面</span
              ><span class="flex-1 break-all"
                ><a
                  v-if="current.spec.sourceUrl"
                  :href="current.spec.sourceUrl"
                  target="_blank"
                  class="text-[#4fc7b7] hover:underline"
                  >{{ current.spec.sourceUrl }}</a
                ><span v-else class="text-gray-400">-</span></span
              >
            </div>
            <div class="flex">
              <span class="w-24 shrink-0 text-gray-500">UserAgent</span
              ><span class="flex-1 text-xs break-all text-gray-600">{{
                current.spec.userAgent || "-"
              }}</span>
            </div>
            <div class="flex">
              <span class="w-24 shrink-0 text-gray-500">提交 IP</span
              ><span class="flex-1">{{ current.spec.ip || "-" }}</span>
            </div>
            <div class="flex">
              <span class="w-24 shrink-0 text-gray-500">前端时间</span
              ><span class="flex-1">{{ current.spec.timestamp || "-" }}</span>
            </div>
            <div class="flex">
              <span class="w-24 shrink-0 text-gray-500">创建时间</span
              ><span class="flex-1">{{ formatTime(current.metadata.creationTimestamp) }}</span>
            </div>
            <div class="flex">
              <span class="w-24 shrink-0 text-gray-500">记录名称</span
              ><span class="flex-1 font-mono text-xs break-all">{{ current.metadata.name }}</span>
            </div>
          </div>
          <div class="flex gap-2 border-t pt-4">
            <button
              class="flex-1 rounded border py-2 text-sm font-medium"
              :class="
                current.spec.handled
                  ? 'bg-white hover:bg-gray-50'
                  : 'border-[#4fc7b7] bg-[#4fc7b7] text-white hover:opacity-90'
              "
              @click="toggleHandled(current)"
            >
              {{ current.spec.handled ? "标记为未处理" : "标记为已处理" }}
            </button>
            <button
              class="rounded border px-4 py-2 text-sm hover:bg-gray-50"
              @click="detailVisible = false"
            >
              关闭
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 删除二次确认 -->
    <div v-if="deleteVisible" class="fixed inset-0 z-[60] flex items-center justify-center">
      <div class="absolute inset-0 bg-black/40" @click="deleteVisible = false"></div>
      <div class="relative w-[420px] max-w-[90vw] rounded-lg bg-white p-6 shadow-xl">
        <h3 class="mb-2 font-semibold">确认删除</h3>
        <p class="text-sm text-gray-600">
          确定要删除
          <span class="font-medium text-gray-900">{{ deleteTarget?.spec.company }}</span>
          的合作记录吗？删除后不可恢复。
        </p>
        <div class="mt-6 flex justify-end gap-2">
          <button class="rounded border px-4 py-1.5 text-sm" @click="deleteVisible = false">
            取消
          </button>
          <button
            class="rounded bg-red-600 px-4 py-1.5 text-sm text-white hover:bg-red-700 disabled:opacity-50"
            :disabled="deleting"
            @click="handleDelete"
          >
            {{ deleting ? "删除中..." : "确认删除" }}
          </button>
        </div>
      </div>
    </div>

    <!-- Toast -->
    <div
      v-if="toast.visible"
      class="fixed bottom-6 left-1/2 z-[70] -translate-x-1/2 rounded-lg px-4 py-2.5 text-sm font-medium shadow-lg"
      :class="
        toast.type === 'success'
          ? 'bg-green-600 text-white'
          : toast.type === 'error'
            ? 'bg-red-600 text-white'
            : 'bg-gray-800 text-white'
      "
    >
      {{ toast.message }}
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch } from "vue";

import {
  listCooperations,
  deleteCooperation,
  updateHandled,
  exportCooperations,
} from "../api/cooperation";
import type { Cooperation } from "../types/cooperation";
import { TYPE_OPTIONS, HANDLED_OPTIONS, TYPE_LABEL_MAP } from "../types/cooperation";

const typeOptions = TYPE_OPTIONS;
const handledOptions = HANDLED_OPTIONS;

const filters = reactive({
  keyword: "",
  type: "all",
  handled: "all" as string,
  startTime: "",
  endTime: "",
});

const loading = ref(false);
const exporting = ref(false);
const list = ref<Cooperation[]>([]);
const total = ref(0);
const page = ref(0);
const pageSize = ref(20);

const totalPages = computed(() => Math.ceil(total.value / pageSize.value));

const detailVisible = ref(false);
const current = ref<Cooperation | null>(null);

const deleteVisible = ref(false);
const deleteTarget = ref<Cooperation | null>(null);
const deleting = ref(false);

const toast = reactive({
  visible: false,
  message: "",
  type: "info" as "success" | "error" | "info",
});
let toastTimer: number | null = null;
function showToast(msg: string, type: "success" | "error" | "info" = "info") {
  toast.message = msg;
  toast.type = type;
  toast.visible = true;
  if (toastTimer) window.clearTimeout(toastTimer);
  toastTimer = window.setTimeout(() => (toast.visible = false), 2800);
}

function typeLabel(t: string) {
  return TYPE_LABEL_MAP[t] || t;
}
function truncate(s: string | undefined, n: number) {
  if (!s) return "-";
  return s.length > n ? s.slice(0, n) + "…" : s;
}
function formatTime(iso?: string) {
  if (!iso) return "-";
  try {
    const d = new Date(iso);
    return d.toLocaleString("zh-CN", { hour12: false });
  } catch {
    return iso;
  }
}

async function fetchList() {
  loading.value = true;
  try {
    const res = await listCooperations({
      page: page.value,
      size: pageSize.value,
      keyword: filters.keyword || undefined,
      type: filters.type !== "all" ? filters.type : undefined,
      handled: filters.handled !== "all" ? filters.handled : undefined,
      startTime: filters.startTime || undefined,
      endTime: filters.endTime || undefined,
    });
    list.value = res.items || [];
    total.value = res.total || 0;
    // 后端返回的 page 可能与请求不一致，校正
    if (typeof res.page === "number") page.value = res.page;
  } catch (e: unknown) {
    const err = e as { status?: number; message?: string };
    if (err.status === 401) {
      showToast("未登录，请先登录", "error");
      // 跳转登录：Halo 的登录页为 /login
      setTimeout(() => (window.location.href = "/login"), 1200);
    } else if (err.status === 403) {
      showToast("无权限，需要管理员角色", "error");
    } else {
      showToast(err.message || "加载失败", "error");
    }
  } finally {
    loading.value = false;
  }
}

function handleSearch() {
  page.value = 0;
  fetchList();
}
function handleReset() {
  filters.keyword = "";
  filters.type = "all";
  filters.handled = "all";
  filters.startTime = "";
  filters.endTime = "";
  page.value = 0;
  fetchList();
}
function handleSizeChange() {
  page.value = 0;
  fetchList();
}
function goPage(p: number) {
  if (p < 0 || p >= totalPages.value) return;
  page.value = p;
  fetchList();
}

function openDetail(item: Cooperation) {
  current.value = item;
  detailVisible.value = true;
}
function confirmDelete(item: Cooperation) {
  deleteTarget.value = item;
  deleteVisible.value = true;
}
async function handleDelete() {
  if (!deleteTarget.value) return;
  deleting.value = true;
  try {
    await deleteCooperation(deleteTarget.value.metadata.name);
    showToast("删除成功", "success");
    deleteVisible.value = false;
    // 若当前详情是被删除项，关闭详情
    if (current.value?.metadata.name === deleteTarget.value.metadata.name)
      detailVisible.value = false;
    await fetchList();
    // 若当前页已空且不是第一页，回退一页
    if (list.value.length === 0 && page.value > 0) {
      page.value = Math.max(0, page.value - 1);
      await fetchList();
    }
  } catch (e: unknown) {
    const err = e as { message?: string };
    showToast(err.message || "删除失败", "error");
  } finally {
    deleting.value = false;
  }
}
async function toggleHandled(item: Cooperation) {
  const next = !item.spec.handled;
  try {
    const updated = await updateHandled(item.metadata.name, next);
    // 本地更新
    item.spec.handled = updated.spec.handled;
    if (current.value && current.value.metadata.name === item.metadata.name) {
      current.value.spec.handled = updated.spec.handled;
    }
    showToast(next ? "已标记为已处理" : "已标记为未处理", "success");
    // 若当前筛选为 handled 过滤，可能需要刷新列表
    if (filters.handled !== "all") {
      await fetchList();
    }
  } catch (e: unknown) {
    const err = e as { message?: string };
    showToast(err.message || "状态更新失败", "error");
  }
}
async function handleExport() {
  if (total.value === 0) return;
  exporting.value = true;
  try {
    const blob = await exportCooperations({
      keyword: filters.keyword || undefined,
      type: filters.type !== "all" ? filters.type : undefined,
      handled: filters.handled !== "all" ? filters.handled : undefined,
      startTime: filters.startTime || undefined,
      endTime: filters.endTime || undefined,
    });
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    const ts = new Date().toISOString().slice(0, 19).replace(/[-T:]/g, "");
    // 后端已通过 Content-Disposition 指定文件名，这里提供兜底
    a.href = url;
    a.download = `cooperations-${ts}.csv`;
    document.body.appendChild(a);
    a.click();
    a.remove();
    URL.revokeObjectURL(url);
    showToast("导出成功", "success");
  } catch (e: unknown) {
    const err = e as { message?: string; status?: number };
    if (err.status === 401) showToast("未登录", "error");
    else if (err.status === 403) showToast("无权限", "error");
    else showToast(err.message || "导出失败", "error");
  } finally {
    exporting.value = false;
  }
}

onMounted(fetchList);
watch([() => filters.type, () => filters.handled], () => {
  // 类型/状态切换自动搜索并刷新
  page.value = 0;
  fetchList();
});
</script>

<style scoped>
.input:focus {
  outline: none;
  border-color: #4fc7b7;
  box-shadow: 0 0 0 2px rgba(79, 199, 183, 0.2);
}
</style>
