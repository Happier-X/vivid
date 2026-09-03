# Design: Halo合作咨询插件后台管理页

## 架构与边界

### 目标架构

```
主题 page_cooperation.html POST /apis/api.cooperation.vivid.run/v1alpha1/cooperations (匿名)
   ↓
plugin-cooperation (Halo 进程内, ReactiveExtensionClient)
   ├─ Extension: Cooperation (api.cooperation.vivid.run/v1alpha1, kind=Cooperation)
   │   └─ spec: company, contact, phone, type, typeLabel, message, website, sourceUrl, userAgent, timestamp, ip, handled(boolean, 新增)
   ├─ RouterFunction: POST(匿名) / GET(list/get, ROLE_ADMIN) / DELETE / PATCH handled
   ├─ Service: RateLimiter(内存), EmailService(JavaMailSender)
   └─ Console 前端: console/ (Vue3+Vite, @halo-dev/ui-plugin-bundler-kit)
        └─ 打包至 src/main/resources/console/main.js + style.css, plugin.yaml 注册
   ↓
Halo Console 左侧菜单“合作咨询” → 列表/筛选/详情/删除/导出
```

### 废弃边界

- `server/cooperation`（FastAPI）不再作为推荐路径，保留目录但文档标注 `Deprecated`，`settings.yaml` 的 `endpoint` 固定为插件路径
- `worker/cooperation.ts`（Cloudflare Worker）同样廢棄
- 主题与插件强耦合同域同端口，无 CORS，白名单 `ALLOWED_ORIGINS` 不再使用

### 技术选型

- 后端：Halo 2.20.11, Spring Boot 3.2.5, Java 17, `ReactiveExtensionClient`, `RouterFunction`, `@GVK` 自动生成 Extension 存储
- 前端：`console/` 独立 Vite 项目，`create-halo-plugin` 脚手架生成，依赖 `@halo-dev/console-shared`, `@halo-dev/ui`, `vue-router`, `@halo-dev/ui-plugin-bundler-kit`
- 打包：`console` 执行 `pnpm build` 输出至 `src/main/resources/console/`，随 `gradle build` 一并打入 Jar

## 数据流与契约

### Extension 变更

```java
@GVK(group="api.cooperation.vivid.run", version="v1alpha1", kind="Cooperation", plural="cooperations")
public class Cooperation extends AbstractExtension {
  CooperationSpec spec;
  static class CooperationSpec {
    String company, contact, phone, type, typeLabel, message, website, sourceUrl, userAgent, timestamp, ip;
    Boolean handled = false; // 新增，默认 false，历史数据 null 视为 false
  }
}
```

- 兼容：历史数据无 `handled` 字段，读取时 `null -> false`
- 索引：无需新增索引，筛选（type, handled, keyword）均在后端内存过滤后分页，或前端过滤；若 Halo `fieldSelector` 支持 `spec.type` 可直接透传

### API 契约

- `POST /apis/api.cooperation.vivid.run/v1alpha1/cooperations` 匿名，入参不变，创建时 `handled=false`
- `GET /apis/.../cooperations?page=0&size=20&keyword=&type=&handled=` 需 `ROLE_ADMIN`，新增查询参数 `keyword`（匹配 company/contact/phone）、`type` 单值、`handled` (all/true/false)，返回 `ListResult<Cooperation>`
- `GET /apis/.../cooperations/{name}` 需 `ROLE_ADMIN`
- `DELETE /apis/.../cooperations/{name}` 需 `ROLE_ADMIN`，调用 `extensionClient.delete`
- `PATCH /apis/.../cooperations/{name}/handled` 或 `PUT .../handled` 需 `ROLE_ADMIN`，Body `{"handled": true/false}`，更新 Extension
- `GET /apis/.../cooperations/export?keyword=&type=&handled=` 需 `ROLE_ADMIN`，返回 `text/csv; charset=UTF-8`，含 BOM，字段全量

### Console 路由

```
plugin.yaml 中：
spec:
  console:
    entry: console/main.js
    style: console/style.css
```

前端注册：

```ts
definePlugin({
  routes: [
    {
      path: "/cooperations",
      component: CooperationList,
      meta: { title: "合作咨询", permissions: ["plugin:cooperation:view"] },
    },
  ],
  extensionPoints: {
    "console:menu": () => [{ name: "合作咨询", path: "/cooperations", icon: "MessageCircle" }],
  },
});
```

### 导出

- 前端调用 export 接口，Blob 下载，文件名 `cooperations-YYYYMMDD-HHmmss.csv`
- 后端流式生成：表头 `公司名称,联系人,电话,合作类型,合作类型标签,意向说明,来源页面,UA,IP,提交时间,处理状态`，行数据直接取 Extension spec + metadata.creationTimestamp

## 兼容与迁移

- 新增字段 `handled` 默认为 `false`，旧数据读取容错，无需 DB 迁移脚本
- 主题 `settings.yaml` 已为插件路径，无需改主题
- 插件 Jar 版本保持 `1.0.0`，或升级至 `1.1.0` 以区分功能增量（推荐 `1.1.0`）
- 回滚：停用新插件，回退至旧 Jar 即可，旧数据仍可读（多出的 handled 字段被忽略）

## 关键权衡

- 选中 `handled` 二态而非多状态工作流：最小增量满足“是否已处理”核心诉求，避免首版引入状态机复杂度
- 筛选在后端内存过滤而非数据库索引：Halo Extension 的 `fieldSelector` 对自定义 spec 字段支持有限，前端传递参数后端过滤实现最稳，数据量（千级）性能可接受
- 导出走后端流式而非前端内存拼接：避免前端大数据 OOM，且可复用权限校验

## 运维与回滚

- 构建：`console/pnpm install && pnpm build` → `gradle :plugin-cooperation:build -x test`，产物 `build/libs/cooperation-plugin-1.1.0.jar`
- 部署：1Panel 文件管理覆盖 `/opt/halo/plugins/` 下 Jar，重启 Halo，检查日志 `合作咨询插件启动成功`
- 回滚点：保留旧 Jar `cooperation-plugin-1.0.0.jar`，出问题直接替换重启
- 风险：Console 打包路径错误导致菜单不显示 → 验证 `src/main/resources/console/main.js` 是否被打入 Jar（`jar tf` 检查）
