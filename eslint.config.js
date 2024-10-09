// eslint.config.js
import unusedImportsPlugin from 'eslint-plugin-unused-imports';

export default [
  {
    files: ['*.js', '*.jsx', '*.ts', '*.tsx'],
    plugins: {
      // The key should be the name of the plugin (used in the rules), not the variable name
      'unused-imports': unusedImportsPlugin,
    },
    rules: {
      'no-console': 'warn',
      'semi': ['error', 'always'],
      'quotes': ['error', 'single'],
      'no-unused-vars': 'off', // Turn off the core rule if using the plugin
      'unused-imports/no-unused-imports': 'error',
      'unused-imports/no-unused-vars': [
        'error',
        {
          vars: 'all',
          varsIgnorePattern: '^_',
          args: 'after-used',
          argsIgnorePattern: '^_',
        },
      ],
      // Add other rules as needed
    },
  },
];
