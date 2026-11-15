# Interface de cinema compartilhada

A apresentação de Início, Filme/Série, categorias, detalhes, episódios, TV ao vivo, busca e conta usa `tv/app/src/main/assets/gfilmes/cinema.html` como fonte visual. Execute `python scripts/build-cinema-web.py` após alterações nessa fonte para gerar o HTML/CSS/JS do catálogo web. Nenhuma credencial ou endereço de mídia é embutido nessa apresentação.

O navegador autentica pela API da mesma origem e usa cookie HttpOnly para renovação; o token de acesso fica somente em memória. O adaptador web mantém reprodução local com concessões de mídia e limites de telas existentes, favoritos e progresso separados por conta, vínculo com TVs, seleção de várias TVs e controle de reprodução. A interface pode gerar um código para atuar como receptor no navegador. O cliente simplificado de TVs antigas continua em `/pwa/tv.html` e o cliente web anterior em `/pwa/index.html`.

Animes são lidos da coleção real já usada pelo Android (`redeflix/links-animes.txt`); não se simula essa categoria por uma busca textual. Listas e detalhes não divulgam endereços do provedor. Concessões passam pelo fluxo normal de conta ativa, limite de telas e relay com URLs restritas.

A composição de referência em 1280×720 é compartilhada com o Android. As capas, nomes, quantidades e programas vêm do catálogo efetivamente disponível. Não são substituídos pelos títulos fictícios das referências. Programação ausente é indicada como não informada. O navegador tem adaptação para telas estreitas e mantém controles reais, sem mostrar conteúdo demonstrativo como produção.

Validação local: geometria e navegação do pacote em Chromium/WebKit; cliente web com API de teste e banco temporário em ambos; suíte API de autenticação, cobrança, isolamento e três telas; cliente anterior e receptor antigo preservados. Não equivale a instalação física Android TV. APK 2.7.5 permanece candidato até testar no aparelho, sem alteração do manifesto OTA público.
