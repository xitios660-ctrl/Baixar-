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

## Resultado da compilação entregue

Fonte `e2987ee`: build release concluído; **38 testes Android passaram, zero falhas/erros/ignorados**, e **5 testes Chromium passaram**. O teste adicional do atualizador confirma retorno antes de acessar contexto ou rede na edição local. APK v2 e certificado original verificados; pacote e versão conferidos com aapt; assets conferidos byte a byte. Consulte `deliveries/local/APK-MANIFEST.json` e `TEST-RESULTS.json`. Reprodução audiovisual e instalação físicas permanecem pendentes.

## Atualização 1.1.0 — perfis e vínculo offline

46 testes JVM selecionados passaram, zero falhas, erros ou ignorados. Além dos anteriores: persistência de perfis sem persistir sessão aberta, PIN com salt/PBKDF2 e sem exposição na listagem, edição/exclusão com PIN atual, máximo de seis perfis, erros de PIN com espera, assinatura da autorização, referência opaca, cópia para outra TV, assinatura adulterada/outra chave e impossibilidade de exceder a assinatura existente.

7 testes Chromium passaram. Os novos testes verificam código e catálogo bloqueado sem autorização; criação do perfil, confirmação de PIN, rejeição de PIN incorreto, entrada e troca de perfil. A prontidão do DOM mantém a navegação criada mesmo com o cabeçalho oculto, para o watchdog nativo não tratar a tela de perfis como falha de inicialização. Catálogo e bridge são simulados; não é execução de APK em TV.

6 verificações da política de emissão offline passaram com registros sintéticos: acesso válido, bloqueado, administrador, vencido, confirmação de estado desatualizada e código de TV inválido. Nenhum cliente foi criado ou alterado no MongoDB.

O APK público desta versão é **de preparação**, sem autorização real embarcada. Ainda faltam código real da TV e consulta atual do cliente correspondente para emitir e testar o APK personalizado. Acesso real, PIN/teclado no controle remoto, reinicialização da TCL, vencimento/alteração de data no aparelho, isolamento do player e reprodução audiovisual continuam pendentes físicos. Não afirmar que clientes existentes já foram migrados para o APK personalizado.
