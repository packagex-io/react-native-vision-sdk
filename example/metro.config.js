const path = require('path');
const { getDefaultConfig } = require('@react-native/metro-config');
const { getConfig } = require('react-native-builder-bob/metro-config');
const pkg = require('../package.json');

const root = path.resolve(__dirname, '..');
const dimensioningRoot = path.resolve(__dirname, '../../react-native-vision-sdk-dimensioning');

/**
 * Metro configuration
 * https://facebook.github.io/metro/docs/configuration
 *
 * @type {import('metro-config').MetroConfig}
 */
const config = getConfig(getDefaultConfig(__dirname), {
  root,
  pkg,
  project: __dirname,
});

// The dimensioning package is a sibling repo (not under the core package root).
// builder-bob's getConfig only adds `root` to watchFolders, so we extend it manually.
config.watchFolders = [...(config.watchFolders || []), dimensioningRoot];

module.exports = config;
