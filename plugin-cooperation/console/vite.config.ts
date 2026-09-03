import vue from "@vitejs/plugin-vue";
import { defineConfig } from "vite";

// 不依赖 ui-plugin-bundler-kit 的备用配置，确保在 kit 版本不匹配时仍可构建出 console 入口
// Halo 会加载 src/main/resources/console/main.js，并在运行时解析 definePlugin 导出
export default defineConfig({
  plugins: [vue()],
  build: {
    outDir: "../src/main/resources/console",
    emptyOutDir: true,
    lib: {
      entry: "src/index.ts",
      name: "cooperation-plugin",
      formats: ["iife"],
      fileName: () => "main.js",
    },
    rollupOptions: {
      external: [
        "vue",
        "vue-router",
        "@halo-dev/console-shared",
        "@halo-dev/api-client",
        "@halo-dev/components",
        "@halo-dev/shared",
      ],
      output: {
        globals: {
          vue: "Vue",
          "vue-router": "VueRouter",
          "@halo-dev/console-shared": "HaloConsoleShared",
          "@halo-dev/api-client": "HaloApiClient",
          "@halo-dev/components": "HaloComponents",
          "@halo-dev/shared": "HaloShared",
        },
        extend: true,
      },
    },
    cssCodeSplit: false,
  },
});
