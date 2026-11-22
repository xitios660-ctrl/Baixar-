# Gfilmes Local 1.6.0 — reduzir lentidão ao navegar pelas capas

O titular confirmou que a lentidão ocorre **na navegação pelas capas**. Esta atualização é da mesma edição Local, pacote `com.gfilmes.tv.local`, versão 1.6.0, código 1006000, assinatura original. Instalar por cima, sem desinstalar nem limpar dados. Não altera a aparência, os 12 GIFs, os perfis ou as preferências escolhidas pelo usuário.

## Gargalos encontrados e alterações

1. **Prévia de série chamava detalhe completo.** O foco disparava `api('detail')`; no nativo, o detalhe de uma série resolve temporadas/episódios. A 1.6 usa uma operação local `preview`, que retorna somente os metadados do título sem resolver fontes ou episódios. Para atores antigos que não oferecem `fastPreview`, o protocolo anterior foi preservado.
2. **A entrada Filme/Série esperava os animes.** O hub aguardava `Promise.all` com `api('anime')`, que pode baixar fichas/coleções. Agora a edição Local mostra Filmes/Séries sem essa espera. Animes são carregados quando a categoria é aberta; o hub não inventa uma contagem e mostra “Explorar catálogo” enquanto ela não foi consultada.
3. **Detalhes pesados dividiam a fila com controles.** `CinemaWorkQueue` mantém navegação e mutações de perfil em fila serial própria, e detalhes/animes/ao vivo/play em dois workers separados. IO de episódios não monopoliza a fila de busca/controles. Guardas de perfil antes/depois do trabalho e no lançamento do player impedem respostas/reproduções antigas de aparecer em outra pessoa.
4. **Redesenho da grade a cada seta.** A grade já era virtualizada, mas reatribuía geometria/estilos a todos os cartões a cada desenho. Agora memoiza dimensões e versão geométrica, mantendo os nós/imagens e posicionando somente os cartões novos ou redimensionados.
5. **Filtros e facetas repetidos por página.** A seleção, gêneros e anos ficam em planos imutáveis de cache LRU (limite 12). O escopo infantil integra a chave e o cache é invalidado na atualização dos metadados. A paginação usa `subList`, sem copiar o restante de dezenas de milhares de títulos para devolver 60.
6. **Resolução duplicada ao começar série.** A resposta local de play reutiliza os episódios da resolução que já ocorreu. `CloudRemote` só pede detalhes extras quando o servidor não enviou esses dados; o registro do histórico local usa metadados leves.
7. **Contenção de CPU na atualização de catálogo.** A thread de atualização recebe prioridade Java baixa. A troca visual de destaque aguarda 350 ms na edição local para não disputar trabalho com uma sequência de setas; o foco em si continua imediato. Falhas de uma navegação abandonada não pintam erro sobre a página nova.

## Preservação

Não houve redução dos GIFs para imagens estáticas por padrão, mudança de personagens ou de identidade visual. O comportamento de pausa fora do seletor continua. Não houve alteração automática de bitrate, qualidade do vídeo, legendas, áudio, fontes, PIN infantil, MongoDB, Render ou cobrança. O modo de renderização compatível da TCL foi mantido; não foi forçado um modo de hardware sem teste físico.

A infraestrutura de cache e o snapshot histórico da 1.5.0 continuam iguais. Esta versão não libera fontes bloqueadas nem afirma resolver buffering de rede. As variantes de clientes com autenticação/API periódica não foram distribuídas nem modificadas em produção.

## Validação

- **62 testes JVM passaram**, zero falhas/erros/ignorados. Os novos casos verificam navegação/controle atendidos enquanto o worker de episódios está bloqueado, ordem das mutações de perfil, reutilização de um plano por 21 páginas de uma biblioteca de 40 mil itens, escopos infantil/adulto e invalidação do cache. A janela de paginação lê somente os itens solicitados, sem percorrer o restante da lista.
- **16 testes Chromium passaram.** O caso novo abre a biblioteca de séries sem consultar animes, move o foco sem pedir detalhes completos, preserva os nós dos cartões e registra **zero reposicionamentos** dos cartões já visíveis em quatro movimentos horizontais. Em seguida, bloqueia deliberadamente o detalhe solicitado com OK, navega para Busca e confirma que a resposta antiga não reabre o detalhe.
- O primeiro ensaio detectou um detalhe completo que ainda era pedido para o fundo do hub; esse caminho foi convertido para o mesmo cache de prévias. A suíte completa passou depois dessa correção, sem retirar a exigência de zero pedidos de episódios no foco.
- Regressões de histórico, cache/fonte 403, perfis, PIN infantil, GIFs e 720p/768p/1080p continuam passando. UI usa dados/bridge sintéticos; os testes JVM incluem o snapshot histórico real e accessors de armazenamento em memória.
- Build release, método `fastPreview` e helpers presentes no DEX, assets idênticos, conteúdo comprimido do catálogo e certificado original verificados no manifesto da entrega. Download final verificado por tamanho/hash/assinatura.

**Pendente físico:** instalar na TCL, avaliar a resposta do controle e medir frame time/FPS/RAM na navegação real e em uso prolongado. Não há TV/ADB conectada. As contagens de trabalho evitado são evidência de código/teste, não uma porcentagem de melhora ou benchmark de FPS na TV. Reprodução/qualidade de transmissão não foi declarada validada nesta correção.
