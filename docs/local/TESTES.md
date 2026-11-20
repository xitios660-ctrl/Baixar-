# Testes da edição local

A edição local é habilitada somente por `-PgfilmesLocal=true`, em pacote separado. As outras modalidades mantêm seus controles existentes. As opções local/offline/recuperação/mobile são mutuamente exclusivas.

Testes JVM selecionados incluem o perfil local sem tokens/licença, abertura/reabertura/reprodução, rotas de login/renovação/admin/polling recusadas, identidade local estável, delegação das rotas de catálogo para o resolvedor nativo e política de reprodução sem timer. O teste da mesma política confirma que as modalidades anteriores continuam com lease limitado e vencimento offline respeitado.

Os testes em Chromium usam dados sintéticos e bridge nativa simulada. Validam o layout cinematográfico original, filmes/séries/TV ao vivo, encaminhamento para player, busca preservando o tipo, favoritos locais e ausência de login/vínculo. As requisições HTTP são interceptadas e o teste local exige nenhuma chamada para `/api/` ou `onrender.com`. Isso prova o fluxo da interface com a bridge de teste; não é captura de tráfego nem reprodução audiovisual de um APK instalado na TCL.

Regressões de interface anteriores também são executadas: conta/pareamento da edição original, grade grande de 3000 filmes, filtros combinados, paginação automática, limite de cartões/cache, DPAD e recuperação de inicialização.

```sh
# JDK/SDK e assinatura original configurados privadamente:
tv/gradlew -p tv :app:testReleaseUnitTest --tests 'com.guhflix.shell.*' --tests 'com.gfilmes.license.*' --tests 'br.com.saimo.tv.VodFormatoTest' --tests 'br.com.saimo.tv.PlaybackUrlTest' --tests 'br.com.saimo.tv.RemoteTest' :app:assembleRelease -PgfilmesLocal=true
# Interface sem servidor de teste:
pwa/node_modules/.bin/playwright test -c pwa/local-browser-test/playwright.config.ts
```

Pendentes físicos: instalar e reiniciar na TCL, reproduzir filmes/episódios/canais autorizados, verificar codecs e fluidez, confirmar favoritos/progresso após reiniciar e observar tráfego com a API MARY indisponível. Esses resultados não foram inventados. Não houve escrita no banco de produção nesta etapa nem substituição de downloads/OTA existentes.
