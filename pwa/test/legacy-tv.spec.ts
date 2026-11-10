import {test,expect} from '@playwright/test';
test('basic TV catalog works without recent browser APIs and restores secure session',async({page})=>{
 await page.addInitScript(()=>{(window as any).fetch=undefined;(window as any).AbortController=undefined;(window.crypto as any).randomUUID=undefined;(HTMLDialogElement.prototype as any).showModal=undefined;});
 await page.goto('/pwa/');await expect(page).toHaveURL(/\/pwa\/tv\.html$/);await page.locator('#username').fill('fixture');await page.locator('#password').fill('wrong');await page.locator('#enter').click();await expect(page.locator('#notice')).toContainText('incorretos');
 await page.locator('#password').fill('fixture-password');await page.locator('#enter').click();await expect(page.locator('#library')).toBeVisible();await expect(page.locator('.card')).toHaveCount(3);await expect(page.locator('#password')).toHaveValue('');
 await page.reload();await expect(page.locator('#library')).toBeVisible();await page.locator('#series').click();await expect(page.locator('.card')).toHaveCount(1);await page.locator('.card').click();await expect(page.locator('#episodes button')).toHaveCount(2);
 await page.locator('#back').click();await page.locator('#query').fill('Cidade');await page.locator('#search').click();await expect(page.locator('.card')).toHaveCount(0);await page.locator('#movies').click();await expect(page.locator('.card')).toHaveCount(1);
 await page.locator('#logout').click();await expect(page.locator('#login')).toBeVisible();await page.reload();await expect(page.locator('#login')).toBeVisible();await expect(page.locator('#cards')).toBeEmpty();
});
test('unsupported TV media remains in player and closing releases the grant',async({page})=>{
 await page.goto('/pwa/tv.html');await page.locator('#username').fill('fixture');await page.locator('#password').fill('fixture-password');await page.locator('#enter').click();await expect(page.locator('#library')).toBeVisible();
 await page.locator('#movies').click();await expect(page.locator('.card')).toHaveCount(2);await page.locator('.card').first().click();await page.locator('#play').click();await expect(page.locator('#player')).toBeVisible();await expect(page.locator('#video')).toHaveAttribute('src',/\/api\/connect\/watch\//);
 await page.locator('#video').evaluate((v:HTMLVideoElement)=>v.dispatchEvent(new Event('error')));await expect(page.locator('#player-status')).toContainText('não abriu esta fonte');await expect(page.locator('#player')).toBeVisible();
 const stopped=page.waitForResponse(r=>r.url().includes('/stop')&&r.request().method()==='POST');await page.locator('#close').click();expect((await stopped).status()).toBe(200);await expect(page.locator('#details')).toBeVisible();await expect(page.locator('#video')).not.toHaveAttribute('src',/./);
});
