import js from "@eslint/js";
import tseslint from "typescript-eslint";
import globals from "globals";
import prettier from "eslint-config-prettier";
import path from "node:path";
import { fileURLToPath } from "node:url";

const rootDir = path.dirname(fileURLToPath(import.meta.url));

export default tseslint.config(
  { ignores: ["**/dist/**", "**/node_modules/**", "apps/api/**"] },
  js.configs.recommended,
  ...tseslint.configs.recommended,
  {
    languageOptions: {
      parserOptions: {
        tsconfigRootDir: rootDir,
      },
      globals: { ...globals.browser, ...globals.node },
    },
  },
  prettier
);
