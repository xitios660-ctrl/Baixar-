# Catálogo para navegadores de TV antigos

Entrada: /pwa/tv.html. A entrada principal /pwa/ inclui um script clássico que encaminha navegadores sem fetch, AbortController, crypto.randomUUID ou showModal para essa página. Não há detecção de marca nem coleta nova de identificação além do UUID de instalação já usado na autenticação.

O cliente usa JavaScript clássico, XMLHttpRequest e CSS sem variáveis, grid ou dialog. Login e renovação usam as mesmas rotas client=pwa, cookie de renovação HttpOnly, autorização de cliente e limites de tela. Access token fica em memória; senha não é persistida pelo cliente. Erros ficam visíveis na página. O catálogo oferece filtros, busca, paginação, detalhes, episódios e reprodução local por video nativo. Não oferece controle de outras TVs; esse recurso permanece no catálogo moderno.

O suporte de mídia depende do navegador/firmware da TV. Não há conversão de codec, transcodificação, contorno de DRM ou garantia de HLS nativo. Falha de fonte produz mensagem e nova tentativa sem voltar ao início; fechar encerra a concessão. Nenhum aplicativo NetRange foi instalado ou submetido à loja.

Verificação: build TypeScript e testes Chromium da autenticação, erro de senha, cookie após reload, filtros/busca/episódios, logout, redirecionamento sem APIs recentes, retenção da tela do player em erro e liberação da concessão. Os demais seis testes Chromium do catálogo moderno também são executados. Fixtures isoladas; playback incompatível é evento de erro simulado. TV Philco/NetReady física e reprodução real não foram testadas.
