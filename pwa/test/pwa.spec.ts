import {test,expect} from '@playwright/test';
test('iPhone catalog, pairing, TV command, remote, reload and revocation',async({page,request})=>{
 const errors:string[]=[];page.on('pageerror',e=>errors.push(e.message));
 await page.route('https://posters.example.invalid/**',route=>route.fulfill({contentType:'image/svg+xml',body:'<svg xmlns="http://www.w3.org/2000/svg" width="600" height="900"><defs><linearGradient id="g" x2="1" y2="1"><stop stop-color="#72081a"/><stop offset="1" stop-color="#080818"/></linearGradient></defs><rect width="600" height="900" fill="url(#g)"/><circle cx="350" cy="340" r="170" fill="#b82738" opacity=".3"/><text x="50" y="680" fill="white" font-size="64" font-family="sans-serif" font-weight="bold">A JORNADA</text><text x="50" y="730" fill="#e9a1ac" font-size="24">Uma história de teste</text></svg>'}));
 await page.goto('/pwa/');await page.getByLabel('Usuário da sua conta GUHFLIX').fill('fixture');await page.getByLabel('Senha',{exact:true}).fill('fixture-password');await page.getByRole('button',{name:'Entrar',exact:true}).click();
 await expect(page.locator('.card')).toHaveCount(3);await expect(page.locator('body')).not.toHaveJSProperty('scrollWidth',0);
 const dimensions=await page.evaluate(()=>({width:document.documentElement.clientWidth,scroll:document.documentElement.scrollWidth}));expect(dimensions.scroll).toBeLessThanOrEqual(dimensions.width+1);
 await expect(page.locator('#update-site')).not.toBeVisible();await page.screenshot({animations:'disabled',path:`test-results/${test.info().project.name}-catalog.png`,fullPage:true});
 await page.getByRole('button',{name:'Minhas televisões'}).click();const code=(await (await request.post('/fixture/pair')).json()).code;await page.getByLabel('Código da televisão').fill(code);await page.getByRole('button',{name:'Vincular televisão',exact:true}).click();await expect(page.locator('#devices')).not.toBeVisible();await expect(page.locator('#selected-tv')).toContainText('TCL');
 await page.getByRole('button',{name:'Ver A Jornada (2026)',exact:true}).click();await expect(page.locator('#detail-title')).toHaveText('A Jornada (2026)');await page.locator('#detail-play').click();await expect(page.locator('#remote')).toBeVisible();await expect(page.locator('#remote-title')).toHaveText('A Jornada (2026)',{timeout:12000});
 await page.getByRole('button',{name:'Pausar',exact:true}).click();await expect.poll(async()=>{const rows=await (await request.get('/fixture/commands')).json();return rows.some((r:any)=>r.command.type==='pause'&&r.state==='acked')}).toBe(true);
 await page.locator('[data-close="remote"]').click();await page.reload();await expect(page.locator('#library')).toBeVisible();await expect(page.locator('#selected-tv')).toContainText('TCL');
 const storage=await page.evaluate(()=>JSON.stringify({...localStorage}));expect(storage).not.toContain('fixture-password');expect(storage).not.toContain('refreshToken');expect(storage).not.toContain('accessToken');
 const cookies=await page.context().cookies();expect(cookies.some(c=>c.name==='guhflix_pwa_refresh'&&c.httpOnly)).toBe(true);
 await page.getByRole('button',{name:'Minhas televisões'}).click();await page.getByRole('button',{name:'Desvincular',exact:true}).click();await expect(page.locator('.device')).toHaveCount(0);
 expect(errors).toEqual([]);
});
test('PWA manifest, shell cache excludes APIs, empty search, episodes and source errors',async({page,request})=>{
 const manifest=await (await request.get('/pwa/manifest.webmanifest')).json();expect(manifest.display).toBe('standalone');expect(manifest.scope).toBe('/pwa/');
 await page.goto('/pwa/');await page.getByLabel('Usuário da sua conta GUHFLIX').fill('fixture');await page.getByLabel('Senha',{exact:true}).fill('fixture-password');await page.locator('#login-submit').click();await expect(page.locator('.card')).toHaveCount(3);
 await page.getByRole('button',{name:'Ver A Série',exact:true}).click();await expect(page.locator('#season')).toHaveValue('4');await expect(page.locator('#episode-list button')).toHaveCount(2);await page.locator('[data-close="details"]').click();
 await page.getByLabel('Buscar no catálogo').fill('nãoexisteumtituloassim');await expect(page.locator('#catalog-message')).toContainText('Nenhum título');
 await page.evaluate(async()=>{await navigator.serviceWorker.ready});const cached=await page.evaluate(async()=>{const keys=await caches.keys();const all=[];for(const k of keys)for(const r of await (await caches.open(k)).keys())all.push(r.url);return all});expect(cached.every(u=>!u.includes('/api/'))).toBe(true);
});
