# Relatório de validação — 2026-11-07

## Executado

- API: build TypeScript e cinco testes de integração com MongoDB temporário. Regressões de autenticação/admin/assinatura, renovação e bloqueio; limite de três streams; vínculo de código único, rejeição de outra conta, comandos isolados/idempotentes, redelivery/ack, expiração, renovação de sessão, revogação pelo telefone e TV sem afetar outra TV; metadados restritos/cache/credencial somente no servidor; prontidão HTTP.
- PWA: quatro testes de navegador, dois WebKit com viewport iPhone 13 e dois Chromium móvel. Catálogo, layout sem transbordamento, busca vazia, temporadas/episódios, vínculo, comando/ack, reconexão por cookie HttpOnly, ausência de senha/tokens em localStorage, revogação, manifesto e cache sem /api. A TV nesses testes é um simulador explícito da fila; não comprova decodificação de vídeo Android.
- Catálogo real autorizado: 38.216 títulos e 42 categorias lidos; resolução de fontes de A Odisseia e de episódio de A Academia. Nenhum stream comercial foi reproduzido neste teste.
- Metadados autorizados: requisição real HTTP 200, sinopse presente. Credencial não impressa nem incluída no APK.
- Android: APK debug compilado; oito testes JVM da política OTA passaram (pacote/URL/hash/tamanho, intervalo de retomada e falta de espaço). Os testes completos herdados executaram 65 casos: três falharam em conexões externas de guia/capas e um foi ignorado; não são declarados aprovados. Os testes determinísticos novos passaram separadamente.
- Android release: compilação Kotlin/Java e otimização R8 concluídas com sucesso; assinatura/publicação release não executadas sem a chave original.
- Publicação: gerador de manifesto foi executado contra o APK debug e recusou corretamente o certificado diferente do APK 2.4.2; nenhum manifesto/apk debug foi publicado.

## Ainda pendente

- Chave original do usuário: ele confirmou que a possui, mas o arquivo ainda não foi recebido. Assinatura release, confirmação do certificado e instalação por cima da 2.4.2 com preservação efetiva de dados dependem dela.
- Fluxo OTA completo no Android: permissão de fontes externas, instalador, cancelamento/download real e atualização aplicada.
- Reprodução nativa real com comando do iPhone, navegação D-pad e desempenho/memória na TCL.
- Emulador Android API 28 instalado, mas a inicialização foi recusada pelo verificador de espaço disponível do ambiente. Portanto não há teste de execução Android aprovado até que a sessão de emulador seja realizada.
- Testes num iPhone físico/Safari instalado e na TCL real, indisponíveis nesta execução.

A compilação demonstra consistência de código; não substitui esses testes de dispositivo. Consulte notas posteriores para a publicação HTTPS e otimização release.

## Publicação HTTPS confirmada

Render deploy dep-dbn8ohqvcj2c7386ivjg, commit 91217396cbd52b9e4cadc0ae794d4735628d5e24, estado live. Endpoints públicos /pwa/, sw.js e manifest.webmanifest retornaram 200. /api/connect/health informou catalog.available=true. /api/connect/catalog sem autenticação retornou 401. WebKit abriu a página HTTPS pública, service worker ficou ativo e não houve erro de página. Não foi feito login com uma conta real de cliente neste smoke test. A versão Windows e os arquivos de download anteriores permanecem intocados.

## Complemento — 2026-11-08 — conta e destino da reprodução

Site e TV agora exibem o usuário conectado. A seleção de televisão é guardada separadamente por ID de conta. Sem uma seleção válida, o site pede que o usuário escolha a TV; não troca automaticamente para a primeira da lista. Logout limpa o destino em memória; respostas antigas de listagem são descartadas quando a conta muda. O código de vínculo continua exigindo a mesma conta nas duas pontas, com orientação mais clara em caso de erro.

API: testes adicionais verificam que outra conta não lista TVs vinculadas, não lê o comando e não recebe esse comando no poll de sua TV. Navegadores: teste com duas televisões confirma ausência de comando antes de selecionar e entrega somente ao destino escolhido; troca de conta mostra lista vazia, sem herdar as TVs anteriores. Os cinco testes de integração da API e os seis testes de navegador (três WebKit e três Chromium) passaram nesta execução. As TVs do teste de navegador são simuladas; não é um teste de reprodução física na TCL.

APK: recompilação debug e oito testes de política OTA aprovados. A tela Conta / iPhone ganhou a identificação da conta da TV e o endereço do site integrado ao fluxo de vínculo. A publicação do APK de atualização permanece pendente do arquivo original .jks/.keystore, não recebido. Não foi distribuída assinatura de teste como atualização.
