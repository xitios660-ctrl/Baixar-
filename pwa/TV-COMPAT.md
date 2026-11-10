# Catálogo para navegadores de TV antigos

Entrada: /pwa/tv.html. A entrada principal /pwa/ inclui um script clássico que encaminha navegadores sem fetch, AbortController, crypto.randomUUID ou showModal para essa página. Não há detecção de marca nem coleta nova de identificação além do UUID de instalação já usado na autenticação.

O cliente usa JavaScript clássico, XMLHttpRequest e CSS sem variáveis, grid ou dialog. Login e renovação usam as mesmas rotas client=pwa, cookie de renovação HttpOnly, autorização de cliente e limites de tela. Access token fica em memória; senha não é persistida pelo cliente. Erros ficam visíveis na página. O catálogo oferece filtros, busca, paginação, detalhes, episódios e reprodução local por video nativo. Não oferece controle de outras TVs; esse recurso permanece no catálogo moderno.

O suporte de mídia depende do navegador/firmware da TV. Não há conversão de codec, transcodificação, contorno de DRM ou garantia de HLS nativo. Falha de fonte produz mensagem e nova tentativa sem voltar ao início; fechar encerra a concessão. Nenhum aplicativo NetRange foi instalado ou submetido à loja.

Verificação: build TypeScript e testes Chromium da autenticação, erro de senha, cookie após reload, filtros/busca/episódios, logout, redirecionamento sem APIs recentes, retenção da tela do player em erro e liberação da concessão. Os demais seis testes Chromium do catálogo moderno também são executados. Fixtures isoladas; playback incompatível é evento de erro simulado. TV Philco/NetReady física e reprodução real não foram testadas.

## Atualização de navegação e player

Abas Filmes, Séries e TV ao vivo limpam a busca anterior. Catálogo renderiza 18 capas por página; séries agrupam temporada e paginam episódios para limitar DOM/memória. Fontes de TV ao vivo vêm do catalogo.txt configurado no servidor em LIVE_CATALOG_BASE_URL (padrão da integração já usada no Android); somente HTTPS, sem DRM/DASH ou exigência declarada de cabeçalhos especiais. URLs de reprodução não são expostas em lista/detalhes: início passa pela mesma concessão/autorização e limite de telas do vídeo sob demanda.

Player preenche o viewport e solicita fullscreen padrão/vendor no gesto de reprodução quando disponível. O navegador pode recusar fullscreen nativo; nesse caso permanece a área de reprodução preenchida via CSS. Controles se ocultam durante reprodução; Back fecha, teclas de mídia ou botões pausam e setas no vídeo permitem buscar. Sem corte/distorção intencional da proporção original.

Qualidade Leve tenta escolher uma variante HLS até 720p e 2,5 Mbps; Mais leve até 480p. Quando não há variante elegível, seleciona a de menor banda declarada. Sem master multivariante o conteúdo permanece inalterado: não há transcodificação nem promessa de resolução diferente. A opção automática preserva o master original. Áudio/legendas e URLs seladas continuam preservados; verificação de concessão/revogação não foi removida.

Erros e ausência de avanço por 25s durante reprodução acionam tentativa de fonte com posição preservada no VOD; um player pausado não é reiniciado pelo detector. A retomada depende de a fonte aceitar seek. Reaparecimento de reprodução cancela a tentativa pendente. Não elimina limitações de conexão, fonte ou hardware.

Verificações executadas: 12 testes API aprovados; 10 testes Chromium aprovados (incluem fixtures de TV ao vivo, viewport do player, retomada sintética aos 55s, login com APIs modernas desativadas e regressões do catálogo moderno). Teste integrado de acesso/revogação repetido após acrescentar verificação de rotas live não autenticadas. Fontes de produção só foram analisadas por catálogo; isso não comprova abertura de todos os canais nem ausência de travadas na TV física.

## Vínculo pelo catálogo do celular

A versão web de TV agora tem Conectar celular, código de cinco minutos, estado de vínculo e receptor de comandos. Uma sessão de cliente PWA pode ser explicitamente ativada como receptora por POST /api/connect/browser-tv/enable, autenticado e limitado à própria sessão/conta ativa; a sessão mantém o cookie HttpOnly, os demais mecanismos de acesso e o vencimento existentes. Não é concedida administração. PWA normal continua sem permissão de polling de TV até ativação explícita. Nome da TV é escolhido no catálogo do celular ao vincular.

O receptor usa os mesmos pares de conta/sessão, fila, ACK, expiração e revogação do aplicativo nativo. Confirmações significam recebimento/aceitação do comando; não garantem imagem ou compatibilidade de codec. Comandos incluem seleção de filme/episódio, pausa, retomada, busca e parada. O seletor de canais ao vivo permanece na versão TV; o catálogo moderno do celular não passou a listar canais nesta alteração. Duplicatas recentes de comando são confirmadas sem reexecutar; logout limpa o estado local do receptor. O navegador pode exigir um gesto local para permitir autoplay/fullscreen.

Testes adicionais verificam ativação autenticada, recusa de outra origem/conta, vínculo entre duas sessões reais do navegador com backend isolado, envio de MP4 sintético do catálogo, reprodução no navegador, pausa/parada e logout. Isso não substitui o teste físico na Philco com o celular do usuário.
