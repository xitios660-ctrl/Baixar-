import {defineConfig} from '@playwright/test';
export default defineConfig({testDir:'test',testMatch:'cinema-native.spec.ts',timeout:45000,workers:1,use:{viewport:{width:1280,height:720},trace:'retain-on-failure'},projects:[{name:'Cinema-Chromium',use:{browserName:'chromium'}},{name:'Cinema-WebKit',use:{browserName:'webkit'}}]});
