# Gfilmes Local 1.5.0 — corrigir abertura vazia/timeout do catálogo

## Sintoma e evidência

O titular enviou fotos da home vazia, primeiro com “Carregando…” e depois “O catálogo demorou para responder. Tente novamente.”. Não há logs/ADB da TV disponíveis; não foi afirmado que o status HTTP observado neste ambiente seja o mesmo recebido por aquela TV.

Foram encontrados dois problemas concretos no caminho de carregamento:

- `LocalCatalog.index` esperava a atualização de `indice.txt`, carregamento de `fichas.txt` e a normalização do índice completo do player antes de responder home/catalog. Eram dependências desnecessárias para listar títulos. Todas as operações da bridge compartilham um executor: uma abertura lenta também atrasava os pedidos seguintes e os retries.
- O hash de cada título usava um `Formatter` por byte: mais de um milhão de formatações para cerca de 38 mil entradas, além das normalizações de busca. Não é necessário normalizar todos os títulos para apenas mostrar a lista.

Na consulta HTTPS normal das três URLs configuradas (`indice.txt`, `busca.txt`, `fichas.txt`) em 22/11/2026, este ambiente recebeu **HTTP 403**, corpo **`error code: 1010`**, cabeçalhos Cloudflare. Nenhuma URL, identidade, cabeçalho de navegador, proxy ou rota alternativa foi usada para contornar esse bloqueio. Não houve troca de servidor de API ou de fonte.

## Correção

1. O primeiro catálogo usa metadados salvos por fonte. Sem cache e apenas para a fonte padrão já existente, usa uma cópia histórica de metadados que já estava preservada no projeto.
2. Essa cópia contém **28.362 filmes e 9.854 séries**, e foi preservada em arquivos locais com mtime de **07/11/2026**. É um catálogo histórico real, **não** uma coleta atual nem títulos fictícios. Manifesto com hashes e proveniência em `assets/gfilmes/catalog-snapshot/manifest.json`.
3. Os arquivos incorporados são `busca.txt.gz` e `fichas.txt.gz`: nomes, tipos, letras, anos disponíveis, gêneros e referências públicas de pôster/TMDB. Não foi incorporado `indice.txt` com servidores de reprodução, vídeos, credenciais de provedor ou dados de clientes.
4. A nova montagem é Kotlin puro, sem requisições de rede, normalização para busca ou dependência de metadados remotos. O hexadecimal SHA-256 foi otimizado mantendo exatamente a fórmula/IDs antigos; favoritos e aprovações infantis não são reidentificados.
5. Atualização da fonte ocorre em thread própria, depois de o catálogo estar disponível. Metadados de capa/gênero não bloqueiam a lista inicial. A falha preserva o catálogo já mostrado.
6. O cache tem travas por arquivo: uma transferência lenta de metadados não monopoliza outro arquivo salvo. Atualizações são atômicas e páginas de bloqueio/índices inválidos são rejeitados antes de substituir dados válidos. Cache continua isolado pelo hash da origem.
7. Transferências usam timeouts, verificação de prazo e fechamento da conexão. Respostas 403 são apresentadas como recusa da fonte. Não foi simplesmente aumentado o timeout da interface.
8. A interface mostra **Catálogo salvo** e o resultado da atualização, com botão **Atualizar fonte**. O botão não esvazia os títulos ou abre outro serviço. Há intervalo de um minuto para novas tentativas manuais; status é consultado localmente, sem chamadas à API MARY.
9. Fonte personalizada/desativada continua sendo respeitada. O catálogo padrão incorporado não é usado para substituir silenciosamente uma lista personalizada ou desativada.

## Instalação e limites reais

Mesma edição e pacote: **Gfilmes Local**, `com.gfilmes.tv.local`, versão **1.5.0**, código **1005000**. Instalar por cima da versão anterior, **sem desinstalar ou limpar os dados**. Assinatura Android original preservada. Os 12 avatares animados, perfis, PIN infantil, histórico, progresso e preferências foram mantidos.

**Esta correção recupera a listagem pelo catálogo salvo. Não é confirmação de reprodução.** Capas e vídeos online continuam dependendo da internet e das fontes. Se a fonte recusar os arquivos necessários à reprodução, mostrar os títulos não libera esse acesso. A restauração de acesso ao provedor depende de sua autorização/disponibilidade; não foi criada uma rota para contorná-lo. Se o provedor responder normalmente na TV, a atualização da listagem poderá concluir em segundo plano.

Os dados históricos podem não conter títulos lançados/adicionados depois de 07/11 e podem conter títulos cujas fontes já mudaram. A condição é sinalizada como salva; não foi prometido um catálogo atual validado. Perfis infantis continuam mostrando somente itens aprovados pelo responsável, inclusive no snapshot e nos filtros.

Não houve alteração de pagamentos, banco MongoDB, Render, API MARY, credenciais, serviços de produção, site ou OTA. O download é um artefato manual da branch de correção.

## Verificação

- **57 testes JVM passaram**, zero falhas/erros/ignorados. Incluem leitura dos metadados históricos reais incorporados, IDs compatíveis com a fórmula anterior, metadados opcionais, rejeição de HTML/texto de bloqueio, manutenção do cache sob HTTP 403, isolamento por fonte e transferência lenta em um arquivo sem bloquear outro.
- A montagem histórica real foi medida em **875–1262 ms neste computador**, sem requisições de rede. Não é tempo medido na TCL. Os testes também preservam a validação de perfis/progresso/preferências/controle infantil e licenças existentes.
- **15 testes Chromium passaram**, incluindo a regressão “fonte retorna 403, catálogo salvo continua visível, sem loading vazio”. Catálogo/bridge desses testes de UI são simulados; a medição de montagem JVM usa os arquivos históricos reais. Testes de avatares, DPAD e 720p/768p/1080p continuam passando.
- Build release e assinatura, integridade, pacote/versão, GIFs e metadados incorporados são conferidos no manifesto da entrega.
- **Pendente físico:** instalar na TCL, medir a primeira abertura real, confirmar as respostas da fonte naquela rede, reproduzir filme/série/canal autorizado e medir codecs/desempenho. Não há TV/ADB conectada. A fonte recusou as consultas deste ambiente, portanto não foi declarado streaming funcional.
