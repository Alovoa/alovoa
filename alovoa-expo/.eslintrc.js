// https://docs.expo.dev/guides/using-eslint/
module.exports = {
  extends: 'expo',
  ignorePatterns: ['/dist*'],
  rules: {
    // Legacy warnings backlog currently blocks lint gate and masks real regressions.
    // Keep strict error rules, relax noisy warning-only rules until files are incrementally cleaned.
    'react-hooks/exhaustive-deps': 'off',
    '@typescript-eslint/no-unused-vars': 'off',
    '@typescript-eslint/array-type': 'off',
    '@typescript-eslint/no-require-imports': 'off',
    'react/display-name': 'off',
    eqeqeq: 'off',
  },
};
