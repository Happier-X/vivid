import { haloThemePlugin } from "@halo-dev/vite-plugin-halo-theme";
import tailwindcss from "@tailwindcss/vite";
import { defineConfig } from "vite-plus";

export default defineConfig({
  plugins: [tailwindcss(), haloThemePlugin()],
  lint: {
    options: { typeAware: true, typeCheck: true },
    ignorePatterns: [".agents", ".pi", "plugin-cooperation"],
  },
  fmt: {
    printWidth: 100,
    tabWidth: 2,
    useTabs: false,
    endOfLine: "lf",
    sortPackageJson: true,
    insertFinalNewline: true,
    sortImports: {},
    sortTailwindcss: {},
    ignorePatterns: [".agents", ".pi", "plugin-cooperation"],
  },
  staged: {
    "*": ["vp check"],
  },
});
