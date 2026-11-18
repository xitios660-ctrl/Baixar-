import {test,expect} from '@playwright/test';
import {readFileSync} from 'node:fs';import {resolve} from 'node:path';
const html=readFileSync(resolve(__dirname,'../../tv/app/src/main/assets/gfilmes/cinema.html'),'utf8');
test('packaged UI signals DOM readiness without waiting for the network and offers retry after timeout',async({page})=>{
 await page.clock.install();await page.evaluate(()=>{(window as any).GfilmesNative={request:()=>{}}});await page.setContent(html);await expect.poll(()=>page.evaluate(()=>(window as any).gfilmesUiReady)).toBe(true);await expect(page.locator('#nav button')).toHaveCount(6);await expect(page.locator('#loading')).toBeVisible();await page.clock.fastForward(36000);await expect(page.getByRole('button',{name:'Tentar novamente',exact:true})).toBeVisible();expect(await page.locator('body').evaluate(el=>getComputedStyle(el).backgroundColor)).toBe('rgb(7, 9, 13)');
});
test('a failed bootstrap never sends a false readiness signal',async({page})=>{
 await page.evaluate(()=>{(window as any).GfilmesNative={request:()=>{}}});await page.setContent(html.replace('id="nav"','id="missing-nav"'));expect(await page.evaluate(()=>(window as any).gfilmesUiReady===true)).toBe(false);
});
