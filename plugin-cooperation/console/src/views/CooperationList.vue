<template>
  <div class="cooperation-admin min-h-[calc(100vh-64px)] bg-[#eef7f7] p-4 sm:p-6">
    <!-- 页头 -->
    <div class="mb-4 flex items-start justify-between sm:mb-5 sm:items-center">
      <div>
        <h1 class="text-lg font-black tracking-tight text-[#1f2d38]">合作咨询</h1>
        <p class="mt-1 text-xs text-[#687783] sm:text-sm">集中管理来自万椿微卡合作表单的线索，支持筛选、标记、导出与追溯</p>
      </div>
      <div
        class="hidden items-center gap-2 rounded-full border border-[rgba(217,236,233,0.9)] bg-white px-3 py-1.5 text-xs text-[#687783] shadow-[0_2px_10px_rgba(36,109,116,0.06)] sm:inline-flex"
      >
        <span class="inline-flex h-2 w-2 rounded-full bg-[#4fc7b7]"></span>
        共 <span class="font-semibold text-[#1f2d38]">{{ total }}</span> 条记录
      </div>
    </div>

    <!-- 筛选栏 -->
    <VCard
      class="!rounded-[12px] !border-[rgba(217,236,233,0.9)] !shadow-[0_8px_30px_rgba(36,109,116,0.06)] transition-shadow hover:!shadow-[0_18px_45px_rgba(36,109,116,0.10)]"
      :body-class="['!p-5']"
    >
      <div class="grid grid-cols-1 gap-4 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-5">
        <!-- 关键词 -->
        <div class="flex flex-col gap-1.5">
          <label class="text-xs font-medium text-[#1f2d38]">关键词</label>
          <div class="relative">
            <span
              class="pointer-events-none absolute left-2.5 top-1/2 -translate-y-1/2 text-[#687783]"
            >
              <IconSearch class="h-4 w-4" />
            </span>
            <input
              v-model="filters.keyword"
              placeholder="搜索 公司/联系人/电话"
              class="h-9 w-full rounded-lg border border-[rgba(217,236,233,0.9)] bg-white py-2 pl-8 pr-3 text-sm placeholder:text-gray-400 focus:border-[#4fc7b7] focus:outline-none focus:ring-2 focus:ring-[#4fc7b7]/20"
              @keyup.enter="handleSearch"
            />
          </div>
        </div>

        <!-- 合作类型 -->
        <div class="flex flex-col gap-1.5">
          <label class="text-xs font-medium text-[#1f2d38]">合作类型</label>
          <div class="relative">
            <select
              v-model="filters.type"
              class="h-9 w-full appearance-none rounded-lg border border-[rgba(217,236,233,0.9)] bg-white px-3 pr-8 text-sm text-[#1f2d38] focus:border-[#4fc7b7] focus:outline-none focus:ring-2 focus:ring-[#4fc7b7]/20"
            >
              <option v-for="opt in typeOptions" :key="opt.value" :value="opt.value">
                {{ opt.label }}
              </option>
            </select>
            <IconArrowDown class="pointer-events-none absolute right-2.5 top-1/2 h-4 w-4 -translate-y-1/2 text-[#687783]" />
          </div>
        </div>

        <!-- 处理状态 -->
        <div class="flex flex-col gap-1.5">
          <label class="text-xs font-medium text-[#1f2d38]">处理状态</label>
          <div class="relative">
            <select
              v-model="filters.handled"
              class="h-9 w-full appearance-none rounded-lg border border-[rgba(217,236,233,0.9)] bg-white px-3 pr-8 text-sm text-[#1f2d38] focus:border-[#4fc7b7] focus:outline-none focus:ring-2 focus:ring-[#4fc7b7]/20"
            >
              <option v-for="opt in handledOptions" :key="opt.value" :value="opt.value">
                {{ opt.label }}
              </option>
            </select>
            <IconArrowDown class="pointer-events-none absolute right-2.5 top-1/2 h-4 w-4 -translate-y-1/2 text-[#687783]" />
          </div>
        </div>

        <!-- 开始时间 -->
        <div class="flex flex-col gap-1.5">
          <label class="text-xs font-medium text-[#1f2d38]">开始时间</label>
          <div class="relative">
            <input
              v-model="filters.startTime"
              type="date"
              class="h-9 w-full rounded-lg border border-[rgba(217,236,233,0.9)] bg-white px-3 py-2 text-sm text-[#1f2d38] focus:border-[#4fc7b7] focus:outline-none focus:ring-2 focus:ring-[#4fc7b7]/20"
            />
          </div>
        </div>

        <!-- 结束时间 -->
        <div class="flex flex-col gap-1.5">
          <label class="text-xs font-medium text-[#1f2d38]">结束时间</label>
          <div class="relative">
            <input
              v-model="filters.endTime"
              type="date"
              class="h-9 w-full rounded-lg border border-[rgba(217,236,233,0.9)] bg-white px-3 py-2 text-sm text-[#1f2d38] focus:border-[#4fc7b7] focus:outline-none focus:ring-2 focus:ring-[#4fc7b7]/20"
            />
          </div>
        </div>
      </div>

      <!-- 按钮组 -->
      <div class="mt-4 flex flex-wrap items-center gap-2 sm:mt-5 sm:justify-end">
        <VButton
          type="primary"
          :loading="loading"
          class="!bg-[#4fc7b7] !border-[#4fc7b7] hover:!bg-[#3db8a5] hover:!border-[#3db8a5]"
          @click="handleSearch"
        >
          <template #icon>
            <IconSearch />
          </template>
          搜索
        </VButton>
        <VButton type="default" @click="handleReset">
          <template #icon>
            <IconRefreshLine />
          </template>
          重置
        </VButton>
        <VButton
          :disabled="total === 0 || exporting"
          @click="handleExport"
        >
          <template #icon>
            <IconUpload />
          </template>
          {{ exporting ? "导出中..." : "导出 CSV" }}
        </VButton>
      </div>
    </VCard>

    <!-- 加载态 -->
    <VCard
      v-if="loading"
      class="mt-4 !rounded-[12px] !border-[rgba(217,236,233,0.9)] !shadow-[0_8px_30px_rgba(36,109,116,0.06)]"
      :body-class="['!p-0']"
    >
      <div class="flex flex-col items-center justify-center py-20">
        <VLoading />
        <p class="mt-3 text-sm font-medium text-[#687783]">正在加载合作咨询…</p>
        <p class="mt-1 text-xs text-[#a0b8b5]">请稍候，正在获取最新线索</p>
      </div>
    </VCard>

    <!-- 空状态 -->
    <VCard
      v-else-if="list.length === 0"
      class="mt-4 !rounded-[12px] !border-[rgba(217,236,233,0.9)] !shadow-[0_8px_30px_rgba(36,109,116,0.06)]"
      :body-class="['!p-0']"
    >
      <div class="flex flex-col items-center px-6 py-16 text-center sm:py-20">
        <div
          class="flex h-16 w-16 items-center justify-center rounded-full bg-[#eef6f5] text-5xl shadow-inner"
        >
          📋
        </div>
        <h3 class="mt-4 text-base font-black tracking-tight text-[#1f2d38]">暂无合作咨询数据</h3>
        <p class="mt-1.5 max-w-[420px] text-sm leading-5 text-[#687783]">
          请检查合作表单是否已发布，或尝试调整筛选条件后重新搜索
        </p>
        <p class="mt-1 max-w-[420px] text-sm leading-5 text-[#687783]">
          若刚发布表单，提交新数据后会自动出现在这里
        </p>
        <code
          class="mt-4 max-w-full break-all rounded-lg border border-[rgba(217,236,233,0.9)] bg-[#eef6f5] px-3 py-2 font-mono text-xs text-[#246d74]"
        >
          /apis/api.cooperation.vivid.run/v1alpha1/cooperations
        </code>
        <div class="mt-5 flex gap-2">
          <VButton type="primary" class="!bg-[#4fc7b7] !border-[#4fc7b7]" @click="handleReset">
            调整筛选条件
          </VButton>
          <VButton @click="handleSearch">刷新</VButton>
        </div>
      </div>
    </VCard>

    <!-- 表格 -->
    <VCard
      v-else
      class="mt-4 overflow-hidden !rounded-[12px] !border-[rgba(217,236,233,0.9)] !shadow-[0_8px_30px_rgba(36,109,116,0.06)]"
      :body-class="['!p-0']"
    >
      <div class="overflow-x-auto">
        <table class="w-full text-sm">
          <thead class="bg-[#eef6f5] text-[#246d74]">
            <tr>
              <th class="px-3 py-3 text-left text-xs font-bold whitespace-nowrap">公司名称</th>
              <th class="px-3 py-3 text-left text-xs font-bold whitespace-nowrap">联系人</th>
              <th class="px-3 py-3 text-left text-xs font-bold whitespace-nowrap">联系电话</th>
              <th class="px-3 py-3 text-left text-xs font-bold whitespace-nowrap">合作类型</th>
              <th class="min-w-[200px] px-3 py-3 text-left text-xs font-bold">合作意向</th>
              <th class="px-3 py-3 text-left text-xs font-bold whitespace-nowrap">提交时间</th>
              <th class="px-3 py-3 text-left text-xs font-bold whitespace-nowrap">IP</th>
              <th class="px-3 py-3 text-left text-xs font-bold whitespace-nowrap">来源页面</th>
              <th class="px-3 py-3 text-left text-xs font-bold whitespace-nowrap">状态</th>
              <th class="px-3 py-3 text-center text-xs font-bold whitespace-nowrap">操作</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-[rgba(217,236,233,0.6)]">
            <tr
              v-for="item in list"
              :key="item.metadata.name"
              class="cursor-pointer transition-colors hover:bg-[#f8fbfb]"
              @click="openDetail(item)"
            >
              <td
                class="max-w-[150px] truncate px-3 py-3 font-medium text-[#1f2d38]"
                :title="item.spec.company"
              >
                {{ item.spec.company }}
              </td>
              <td class="px-3 py-3 whitespace-nowrap text-[#1f2d38]">{{ item.spec.contact }}</td>
              <td class="px-3 py-3 whitespace-nowrap text-[#1f2d38]">{{ item.spec.phone }}</td>
              <td class="px-3 py-3 whitespace-nowrap">
                <VTag class="!border-[#d9ece9] !bg-[#eef6f5] !text-[#246d74]">{{ item.spec.typeLabel || typeLabel(item.spec.type) }}</VTag>
              </td>
              <td
                class="max-w-[240px] truncate px-3 py-3 text-[#687783]"
                :title="item.spec.message"
              >
                {{ truncate(item.spec.message, 36) }}
              </td>
              <td class="px-3 py-3 text-xs whitespace-nowrap text-[#687783]">
                {{ formatTime(item.metadata.creationTimestamp) }}
              </td>
              <td class="px-3 py-3 text-xs whitespace-nowrap text-[#687783]">{{ item.spec.ip || "-" }}</td>
              <td class="max-w-[120px] truncate px-3 py-3" :title="item.spec.sourceUrl">
                <a
                  v-if="item.spec.sourceUrl"
                  :href="item.spec.sourceUrl"
                  target="_blank"
                  class="inline-flex items-center gap-1 text-xs font-medium text-[#4fc7b7] hover:text-[#3db8a5] hover:underline"
                  @click.stop
                >
                  <IconExternalLinkLine class="h-3.5 w-3.5" />
                  来源
                </a>
                <span v-else class="text-xs text-gray-400">-</span>
              </td>
              <td class="px-3 py-3 whitespace-nowrap">
                <span
                  class="inline-flex items-center rounded-full border px-2.5 py-1 text-xs font-medium"
                  :class="
                    item.spec.handled
                      ? 'border-green-200 bg-green-50 text-green-700'
                      : 'border-amber-200 bg-amber-50 text-amber-700'
                  "
                >
                  <VStatusDot
                    :state="item.spec.handled ? 'success' : 'warning'"
                    :text="item.spec.handled ? '已处理' : '未处理'"
                  />
                </span>
              </td>
              <td class="px-3 py-3 text-center whitespace-nowrap" @click.stop>
                <div class="flex items-center justify-center gap-1">
                  <VButton size="xs" @click="openDetail(item)">
                    <template #icon>
                      <IconEye />
                    </template>
                    查看
                  </VButton>
                  <VButton
                    size="xs"
                    :type="item.spec.handled ? 'default' : 'primary'"
                    :class="!item.spec.handled ? '!bg-[#4fc7b7] !border-[#4fc7b7]' : ''"
                    @click="toggleHandled(item)"
                  >
                    <template #icon>
                      <IconCheckboxCircle />
                    </template>
                    {{ item.spec.handled ? "未处理" : "已处理" }}
                  </VButton>
                  <VButton size="xs" type="danger" @click="confirmDelete(item)">
                    <template #icon>
                      <IconDeleteBin />
                    </template>
                    删除
                  </VButton>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- 分页 -->
      <div
        class="flex flex-col gap-3 border-t border-[rgba(217,236,233,0.6)] bg-white px-4 py-3 sm:flex-row sm:items-center sm:justify-between"
      >
        <div class="flex items-center gap-2 text-sm text-[#687783]">
          <span>
            共 <span class="font-semibold text-[#1f2d38]">{{ total }}</span> 条
          </span>
          <span class="hidden h-3 w-px bg-[rgba(217,236,233,0.9)] sm:inline-block"></span>
          <span class="inline-flex items-center gap-1">
            每页
            <select
              v-model.number="pageSize"
              class="rounded-lg border border-[rgba(217,236,233,0.9)] bg-white px-2 py-1 text-sm text-[#1f2d38] focus:border-[#4fc7b7] focus:outline-none"
              @change="handleSizeChange"
            >
              <option :value="10">10</option>
              <option :value="20">20</option>
              <option :value="50">50</option>
            </select>
            条
          </span>
        </div>
        <div class="flex justify-center sm:justify-end">
          <VPagination
            :page="page + 1"
            :size="pageSize"
            :total="total"
            :size-options="[10, 20, 50]"
            @change="handlePaginationChange"
          />
        </div>
      </div>
    </VCard>

    <!-- 详情抽屉 -->
    <div v-if="detailVisible" class="fixed inset-0 z-50 flex">
      <div class="flex-1 bg-black/30 backdrop-blur-[1px]" @click="detailVisible = false"></div>
      <div
        class="flex w-[560px] max-w-[90vw] flex-col overflow-hidden bg-white shadow-xl sm:rounded-l-[12px]"
      >
        <div
          class="sticky top-0 flex items-center justify-between border-b border-[rgba(217,236,233,0.9)] bg-white px-6 py-4"
        >
          <div class="flex items-center gap-3">
            <h3 class="text-base font-black tracking-tight text-[#1f2d38]">合作详情</h3>
            <span
              v-if="current"
              class="inline-flex items-center gap-1.5 rounded-full border px-2.5 py-1 text-xs font-medium"
              :class="
                current.spec.handled
                  ? 'border-green-200 bg-green-50 text-green-700'
                  : 'border-amber-200 bg-amber-50 text-amber-700'
              "
            >
              <span
                class="h-1.5 w-1.5 rounded-full"
                :class="current.spec.handled ? 'bg-green-500' : 'bg-amber-500'"
              ></span>
              {{ current.spec.handled ? "已处理" : "未处理" }}
            </span>
            <VTag
              v-if="current"
              class="!border-[#d9ece9] !bg-[#eef6f5] !text-[#246d74]"
              >{{ current.spec.typeLabel || typeLabel(current.spec.type) }}</VTag
            >
          </div>
          <button
            class="inline-flex h-8 w-8 items-center justify-center rounded-lg text-[#687783] hover:bg-[#eef6f5] hover:text-[#1f2d38]"
            @click="detailVisible = false"
          >
            <IconClose class="h-5 w-5" />
          </button>
        </div>

        <div v-if="current" class="flex-1 space-y-4 overflow-y-auto p-6">
          <!-- 基础信息卡片 -->
          <VCard
            class="!rounded-[12px] !border-[rgba(217,236,233,0.9)] !shadow-[0_8px_30px_rgba(36,109,116,0.06)]"
            :body-class="['!p-4']"
          >
            <h4 class="mb-3 text-sm font-bold text-[#1f2d38]">基础信息</h4>
            <div class="grid gap-3 text-sm">
              <div class="flex">
                <span class="w-28 shrink-0 text-xs font-medium text-[#687783]">公司名称</span
                ><span class="flex-1 font-medium text-[#1f2d38]">{{ current.spec.company }}</span>
              </div>
              <div class="flex">
                <span class="w-28 shrink-0 text-xs font-medium text-[#687783]">联系人</span
                ><span class="flex-1 text-[#1f2d38]">{{ current.spec.contact }}</span>
              </div>
              <div class="flex">
                <span class="w-28 shrink-0 text-xs font-medium text-[#687783]">联系电话</span
                ><span class="flex-1 text-[#1f2d38]">{{ current.spec.phone }}</span>
              </div>
              <div class="flex">
                <span class="w-28 shrink-0 text-xs font-medium text-[#687783]">合作类型</span
                ><span class="flex-1 text-[#1f2d38]">{{ current.spec.type }}（{{ current.spec.typeLabel }}）</span>
              </div>
            </div>
          </VCard>

          <!-- 合作意向 -->
          <VCard
            class="!rounded-[12px] !border-[rgba(217,236,233,0.9)] !shadow-[0_8px_30px_rgba(36,109,116,0.06)]"
            :body-class="['!p-4']"
          >
            <h4 class="mb-3 text-sm font-bold text-[#1f2d38]">合作意向</h4>
            <div
              class="rounded-lg border border-[rgba(217,236,233,0.9)] bg-[#f8fbfb] p-4 text-sm leading-6 break-words whitespace-pre-wrap text-[#1f2d38]"
            >
              {{ current.spec.message || "（未填写）" }}
            </div>
          </VCard>

          <!-- 来源追溯 -->
          <VCard
            class="!rounded-[12px] !border-[rgba(217,236,233,0.9)] !shadow-[0_8px_30px_rgba(36,109,116,0.06)]"
            :body-class="['!p-4']"
          >
            <h4 class="mb-3 text-sm font-bold text-[#1f2d38]">来源追溯</h4>
            <div class="grid gap-3 text-sm">
              <div class="flex">
                <span class="w-28 shrink-0 text-xs font-medium text-[#687783]">来源页面</span
                ><span class="flex-1 break-all"
                  ><a
                    v-if="current.spec.sourceUrl"
                    :href="current.spec.sourceUrl"
                    target="_blank"
                    class="inline-flex items-center gap-1 font-medium text-[#4fc7b7] hover:text-[#3db8a5] hover:underline"
                    ><IconExternalLinkLine class="h-4 w-4" />{{ current.spec.sourceUrl }}</a
                  ><span v-else class="text-gray-400">-</span></span
                >
              </div>
              <div class="flex">
                <span class="w-28 shrink-0 text-xs font-medium text-[#687783]">UserAgent</span
                ><span class="flex-1 text-xs break-all text-[#687783]">{{
                  current.spec.userAgent || "-"
                }}</span>
              </div>
              <div class="flex">
                <span class="w-28 shrink-0 text-xs font-medium text-[#687783]">提交 IP</span
                ><span class="flex-1 text-[#1f2d38]">{{ current.spec.ip || "-" }}</span>
              </div>
              <div class="flex">
                <span class="w-28 shrink-0 text-xs font-medium text-[#687783]">前端时间</span
                ><span class="flex-1 text-[#1f2d38]">{{ current.spec.timestamp || "-" }}</span>
              </div>
              <div class="flex">
                <span class="w-28 shrink-0 text-xs font-medium text-[#687783]">创建时间</span
                ><span class="flex-1 text-[#1f2d38]">{{ formatTime(current.metadata.creationTimestamp) }}</span>
              </div>
              <div class="flex">
                <span class="w-28 shrink-0 text-xs font-medium text-[#687783]">记录名称</span
                ><span class="flex-1 font-mono text-xs break-all text-[#687783]">{{ current.metadata.name }}</span>
              </div>
            </div>
          </VCard>
        </div>

        <div
          v-if="current"
          class="flex gap-2 border-t border-[rgba(217,236,233,0.9)] bg-white p-4"
        >
          <VButton
            class="flex-1 !bg-[#4fc7b7] !border-[#4fc7b7] hover:!bg-[#3db8a5]"
            :type="current.spec.handled ? 'default' : 'primary'"
            :class="!current.spec.handled ? '!text-white' : ''"
            @click="toggleHandled(current)"
          >
            <template #icon>
              <IconCheckboxCircle />
            </template>
            {{ current.spec.handled ? "标记为未处理" : "标记为已处理" }}
          </VButton>
          <VButton @click="detailVisible = false">关闭</VButton>
        </div>
      </div>
    </div>

    <!-- 删除二次确认 -->
    <VModal
      :visible="deleteVisible"
      title="确认删除"
      :width="420"
      :centered="true"
      @close="deleteVisible = false"
      @update:visible="(v: boolean) => (deleteVisible = v)"
    >
      <div class="flex gap-4">
        <div
          class="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-red-50 text-red-600"
        >
          <IconErrorWarning class="h-5 w-5" />
        </div>
        <div class="flex-1">
          <h3 class="text-sm font-bold text-[#1f2d38]">确认删除</h3>
          <p class="mt-2 text-sm leading-5 text-[#687783]">
            确定要删除
            <span class="font-semibold text-[#1f2d38]">{{ deleteTarget?.spec.company }}</span>
            的合作记录吗？删除后不可恢复，请谨慎操作。
          </p>
        </div>
      </div>
      <template #footer>
        <div class="flex justify-end gap-2">
          <VButton @click="deleteVisible = false">取消</VButton>
          <VButton type="danger" :loading="deleting" @click="handleDelete">
            确认删除
          </VButton>
        </div>
      </template>
    </VModal>

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
  VCard,
  VButton,
  VLoading,
  VModal,
  VPagination,
  VTag,
  VStatusDot,
  IconSearch,
  IconEye,
  IconDeleteBin,
  IconCheckboxCircle,
  IconUpload,
  IconRefreshLine,
  IconExternalLinkLine,
  IconErrorWarning,
  IconClose,
  IconArrowDown,
} from "@halo-dev/components";

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
    if (typeof res.page === "number") page.value = res.page;
  } catch (e: unknown) {
    const err = e as { status?: number; message?: string };
    if (err.status === 401) {
      showToast("未登录，请先登录", "error");
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

function handlePaginationChange(val: { page: number; size: number }) {
  const nextPage = val.page - 1;
  const nextSize = val.size;
  const sizeChanged = nextSize !== pageSize.value;
  page.value = sizeChanged ? 0 : nextPage;
  pageSize.value = nextSize;
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
    if (current.value?.metadata.name === deleteTarget.value.metadata.name)
      detailVisible.value = false;
    await fetchList();
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
    item.spec.handled = updated.spec.handled;
    if (current.value && current.value.metadata.name === item.metadata.name) {
      current.value.spec.handled = updated.spec.handled;
    }
    showToast(next ? "已标记为已处理" : "已标记为未处理", "success");
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
  page.value = 0;
  fetchList();
});
</script>

<style scoped>
.cooperation-admin :deep(.card-wrapper) {
  border-radius: 12px;
}

/* 日期输入统一样式 */
input[type="date"]::-webkit-calendar-picker-indicator {
  cursor: pointer;
  opacity: 0.6;
}
input[type="date"]::-webkit-calendar-picker-indicator:hover {
  opacity: 1;
}
</style>
