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

Publicação deste complemento: deploy Render dep-dbnum929lscs73cffmp0 em estado live, commit 9dd01d227ec6d7449b458f904c2cc11b33dc0a8e. O HTML público contém a orientação de mesma conta e assets 20261108.1; WebKit abriu a versão HTTPS, confirmou service worker ativo e nenhum erro de página. Otimização release R8 também concluída com sucesso. Assinatura e instalação release continuam pendentes da chave original.

## Edição Connect com chave enviada — 2026-11-08

A chave recebida é válida (certificado fe13bf8634b76a4450b70833a7a6eb6aa01429729371a33dccc75c8eaa72b76b), mas diferente da 2.4.2. A edição independente com.guhflix.tv.connect evita conflito de assinatura e preserva os dados do app instalado. Requer novo login e vínculo; não migra os dados automaticamente.

Uma inspeção do APK detectou ausência das activities de abertura/login/conexão e do FileProvider de OTA no manifesto. O empacotamento foi corrigido antes da publicação. A auditoria do manifesto de origem agora encontra todas as 17 activities e o verificador do APK exige launcher Android TV, telas de acesso/conexão, provedores e esquema próprios. A primeira compilação incompleta não foi publicada.

Os seis testes do PWA passaram novamente, assim como os oito testes release da política OTA. Instalação e reprodução em TCL real permanecem pendentes; não são comprovadas pela compilação ou pelos simuladores de API. A assinatura e o SHA-256 da edição final serão conferidos também no arquivo público baixado.

Publicação confirmada: site de downloads live no deploy dep-dbnvej8ae00c73967udg; API/PWA live no deploy dep-dbnvela9lscs73cg18k0. O APK público de 5.016.888 bytes foi baixado e teve SHA-256 cdc9277ef5183bc42da0a8c76967a16c81a545a0a7882a8b65453c8b9dd9d919 e assinatura fe13bf8634b76a4450b70833a7a6eb6aa01429729371a33dccc75c8eaa72b76b conferidos. O CDN respondeu Range HTTP 206 (bytes 0-1023/5016888), permitindo retomada de download. O manifesto antigo segue 2.4.2/com.guhflix.tv, separado do release-connect.json. O PWA público retorna 200, usa assets 20261108.2 e aponta o download para connect.html. Nenhuma chave privada foi publicada. Reprodução, instalação e desempenho na TCL real permanecem pendentes.

## Conta administradora usada no aplicativo — 2026-11-08

Diagnóstico autenticado identificou uma conta role=admin, expiresAt=null, apresentada antes como período vencido. O servidor agora devolve CUSTOMER_REQUIRED com uma orientação para usar acesso cliente em /api/app/access. Não foram alterados papel, validade, assinatura, pagamentos ou senha do usuário. As sessões temporárias de diagnóstico foram encerradas após as consultas. Cinco testes de integração passaram, incluindo regressão que distingue administrador de cliente vencido e preserva autenticação, administração, pagamentos e limite de streams. Deploy dep-dbnvk14s728c739lu8h0 ficou live, commit a18fa0c9404c97fa3e44b8e47eedab2aa3a587b6.
