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

## Atualização 1.2.0 — versão genérica com catálogo liberado

Por nova instrução do titular, o vínculo obrigatório foi removido. Não há `LocalDeviceAccess`, `DeviceGrant`, campo de autorização no BuildConfig ou operação `deviceStatus` no cliente desta versão. A edição local não usa heartbeat/timer de autorização de conta. Escolher/criar um perfil continua separando favoritos e progresso e aplicando somente o PIN opcional do próprio perfil.

**42 testes JVM selecionados passaram**, com zero falhas/erros/ignorados (os quatro testes do vínculo descontinuado foram retirados). **7 testes Chromium passaram**, incluindo Assistir agora → perfil local → catálogo sintético → comando ao player nativo, sem código/autorização. Uma primeira execução encontrou uma falha de sincronização na medição imediata do hero durante sua substituição pelo preview. O teste foi corrigido para aguardar a mesma geometria exigida (57px/218px), sem relaxar as medidas; a suíte completa passou em seguida. Nenhuma alteração do layout foi necessária.

As quatro fontes existentes consultadas responderam HTTP 200 sem prefixo HTML, lendo no máximo 2048 bytes por arquivo. Isso não é teste de reprodução ou varredura completa do catálogo. Build release, assinatura, integridade e download do APK são registrados no manifesto de entrega. Instalação real na TCL, teclado/controle remoto e reprodução audiovisual continuam pendentes de teste físico.

## Atualização 1.3.0 — perfis sem senha e histórico por pessoa

43 testes JVM selecionados passaram, zero falhas/erros/ignorados. Os cinco testes de FamilyProfiles verificam seleção sem credenciais, persistência, retirada do PIN antigo sem substituir IDs, edição antes de entrar, validação/capacidade, exclusão que preserva outro perfil e falha de gravação visível.

9 testes Chromium passaram. A regressão de histórico atrasa deliberadamente a resposta de Ana, troca para Bruno e depois libera a resposta antiga: a fileira continua mostrando Bruno. Também foram testados criação sem qualquer campo de senha, avatar que não envia o formulário, gerenciamento de seis perfis, geometria em 720p, navegação por setas, renomeação e exclusão confirmada. Cinco testes anteriores de catálogo/layout/inicialização também passaram. Dados e bridge desses testes são sintéticos.

A correção nativa fixa o ID do perfil no Intent do player, e o armazenamento de progresso lê esse ID antes de consultar a seleção global. Coleções recebem registro de histórico da interface. Estas alterações compilaram e passaram nos checks disponíveis; a persistência real no ExoPlayer e a reprodução na TCL ainda dependem de teste físico, não foram apresentadas como medidas no aparelho.

O link de referência fornecido retornou a página geral ChatGPT sem imagem; foi solicitada a imagem como anexo. O layout mostrado na prévia desta entrega é uma implementação provisória consistente com o Gfilmes e não uma reprodução validada daquela referência inacessível.

## Atualização 1.4.0 — avatares animados

49 testes JVM e 14 Chromium passaram, incluindo persistência dos 12 avatares, controle infantil, preferências/progresso reais usando SharedPreferences em memória, movimentos dos 12 GIFs e navegação em 720p/768p/1080p. Fonte e limitações em [PERFIS-ANIMADOS-1.4.md](PERFIS-ANIMADOS-1.4.md). Reprodução e desempenho em TCL física permanecem pendentes.

## Atualização 1.5.0 — cache e montagem do catálogo

57 testes JVM e 15 Chromium passaram. O parser leu a cópia real histórica de 28.362 filmes e 9.854 séries sem rede, em 875–1262 ms no computador. Foram verificadas manutenção do cache sob HTTP 403, rejeição de resposta inválida antes da gravação, travas por arquivo, IDs antigos e home visível durante falha da fonte. As três URLs consultadas receberam HTTP 403 / Cloudflare 1010 neste ambiente. Reprodução não confirmada e teste físico TCL pendente. Consulte [CATALOGO-SALVO-1.5.md](CATALOGO-SALVO-1.5.md).
