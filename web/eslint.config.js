import pluginVue from 'eslint-plugin-vue'
import vueParser from 'vue-eslint-parser'
import tseslint from 'typescript-eslint'
import prettier from 'eslint-config-prettier'

export default [
  {
    ignores: ['dist', 'dist-ssr', 'node_modules'],
  },
  // TS 规则在前（其 base 会全局设置 parser）……
  ...tseslint.configs.recommended,
  // ……Vue 配置在后，确保 .vue 文件最终由 vue-eslint-parser 解析
  ...pluginVue.configs['flat/recommended'],
  {
    files: ['**/*.vue'],
    languageOptions: {
      parser: vueParser,
      parserOptions: {
        // <script lang="ts"> 交由 TS parser 处理
        parser: tseslint.parser,
        extraFileExtensions: ['.vue'],
      },
    },
  },
  prettier,
  {
    rules: {
      // 页面组件与路由 name 同名，允许单词组件名（契约 2.2）
      'vue/multi-word-component-names': 'off',
      // 契约第 1 章：禁止 any（DTO 反序列化边界除外），先警告收敛
      '@typescript-eslint/no-explicit-any': 'warn',
    },
  },
]
