// eslint.config.js
export default [
  {
    ignores: ['lib/**', 'node_modules/**', 'example/**', 'coverage/**'],
  },
  {
    files: ['*.js', '*.jsx', '*.ts', '*.tsx'],
    rules: {
      'no-console': 'warn',
      'semi': ['error', 'always'],
      'quotes': ['error', 'single'],
    },
  },
];
