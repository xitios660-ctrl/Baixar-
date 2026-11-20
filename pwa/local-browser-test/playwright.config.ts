import {defineConfig} from '@playwright/test';
export default defineConfig({testDir:'../test',testMatch:['cinema-local.spec.ts','cinema-native.spec.ts','cinema-library.spec.ts','cinema-startup.spec.ts'],timeout:60000,workers:1,use:{browserName:'chromium',viewport:{width:1280,height:720},trace:'retain-on-failure'},outputDir:'../test-results/local'});
