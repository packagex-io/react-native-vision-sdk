/**
 * App configuration.
 *
 * For local development, create example/src/config.local.ts (gitignored) with:
 *   export const LOCAL_API_KEY = 'your_api_key_here';
 *
 * That file is not committed. Falls back to empty string, which shows
 * a visible in-app warning banner.
 */

let localApiKey = '';
try {
  // eslint-disable-next-line @typescript-eslint/no-var-requires
  const local = require('./config.local');
  localApiKey = local.LOCAL_API_KEY ?? '';
} catch {
  // config.local.ts does not exist — expected in CI / fresh checkout
}

export const API_KEY: string = localApiKey;

/**
 * SDK environment.  Matches iOS demo default.
 */
export const ENVIRONMENT: 'sandbox' | 'staging' | 'dev' | 'qa' | 'production' = 'sandbox';

/**
 * True when no API key is configured — ScannerScreen shows a banner.
 */
export const NO_API_KEY = API_KEY.length === 0;
