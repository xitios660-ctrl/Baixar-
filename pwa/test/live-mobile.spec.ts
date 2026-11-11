import {test,expect} from '@playwright/test';
test('Gfilmes mobile live catalog opens a live scoped grant and releases it',async({page})=>{
 await page.setExtraHTTPHeaders({'X-Forwarded-For':'198.51.100.248'});
 await page.goto('/pwa/');await expect(page.locator('.wordmark')).toHaveText('Gfilmes');
 await page.locator('#username').fill('fixture');await page.locator('#password').fill('fixture-password');await page.locator('#login-submit').click();await expect(page.locator('#library')).toBeVisible();
 await page.locator('[data-kind=live]').click();await expect(page.locator('#cards .card')).toHaveCount(1);await expect(page.locator('#section-title')).toHaveText('Agora, ao vivo');await page.locator('#cards .card').click();await expect(page.locator('#detail-kind')).toHaveText('TV AO VIVO');await expect(page.locator('#detail-favorite')).toBeHidden();
 const grant=page.waitForResponse(r=>r.url().endsWith('/api/connect/watch')&&r.request().method()==='POST');await page.locator('#detail-play').click();const response=await grant;expect(response.request().postDataJSON().live).toBe(true);expect(response.status()).toBe(200);await expect(page.locator('#local-player')).toBeVisible();
 const stop=page.waitForResponse(r=>r.url().endsWith('/stop'));await page.locator('#local-close').click();expect((await stop).status()).toBe(200);
 await page.locator('[data-kind=movie]').click();await expect(page.locator('#cards .card')).toHaveCount(2);
});
