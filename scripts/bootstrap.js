// Import necessary modules using ES Module syntax
import os from 'os';
import path from 'path';
import { spawnSync } from 'child_process';
import { fileURLToPath } from 'url';
import { dirname } from 'path';

// Since __dirname is not available in ES Modules, recreate it
const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);

// Define root directory
const root = path.resolve(__dirname, '..');

// Extract command-line arguments
const args = process.argv.slice(2);

// Define options for child_process.spawnSync
const options = {
  cwd: process.cwd(),
  env: process.env,
  stdio: 'inherit',
  encoding: 'utf-8',
};

// Handle Windows-specific shell option
if (os.type() === 'Windows_NT') {
  options.shell = true;
}

let result;

// Determine whether to run `yarn` with provided arguments or perform bootstrap
if (process.cwd() !== root || args.length) {
  // Forward the command to `yarn` with provided arguments
  result = spawnSync('yarn', args, options);
} else {
  // If `yarn` is run without arguments, perform bootstrap
  result = spawnSync('yarn', ['bootstrap'], options);
}

// Exit the process with the appropriate status code
process.exitCode = result.status;
