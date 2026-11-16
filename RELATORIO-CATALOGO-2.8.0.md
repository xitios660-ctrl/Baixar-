# Gfilmes 2.8.0 — catálogo e busca

Código: branch guhflix-platform, commit 8a71f03d06bd8fadf20e290f9da1327a8f61230c. Deploy MARY dep-dbtb5f49v7es73a46af0 confirmado live.

## Entrega

Filmes e Séries têm bibliotecas exclusivas, gêneros reais, filtros combinados por título/tipo/gênero/ano e ordenação A–Z, Z–A ou ano. Limpar mantém o tipo. A lupa permite trocar o tipo preservando os filtros compatíveis. Resultados vazios não escondem a seleção vigente. Botão para voltar à escolha dos universos preservado da atualização concorrente do repositório.

Rolagem automática em páginas de 60, janela de capas próxima da tela, até oito páginas de metadados e duas requisições simultâneas por grade. Cache de até 12 consultas ordenadas por snapshot do servidor. Setas atravessam páginas, detalhes retornam à mesma capa, teclado físico e virtual disponíveis. Falhas apresentam repetição explícita e não provocam ciclos de solicitações. Temporadas/episódios e reprodução existentes preservados.

## Verificações

- API: build TypeScript e 18 testes aprovados, incluindo filtros combinados, validação de parâmetros, paginação, autenticação, expiração e limite de três reproduções.
- Interface autenticada HTTP: 4/4 em Chromium/WebKit.
- Apresentação empacotada: 4/4 em Chromium/WebKit; navegação por setas, retorno de detalhes, recuperação de erro, teclado e grade limitada. Sem depender de IntersectionObserver ou replaceChildren.
- Regressão PWA Chromium: 13/14 na execução conjunta. O teste restante de nomear duas TVs falhou ao receber um código ausente na fixture compartilhada; executado isoladamente com servidor novo, passou. A política de limites de produção não foi alterada.
- Desempenho sintético: 30.000 registros, 15.000 filmes e 250 páginas percorridas sem duplicação. Primeira consulta variou de 26 a 97 ms e percurso em cache de 6 a 10 ms nos últimos testes, sob carga local. São dados artificiais apenas de teste.
- Em produção: login da conta dedicada de QA, consulta e logout HTTP 200. 26.430 filmes, 8.325 séries, 20 gêneros por tipo. Segunda página distinta da primeira. Combinação título/gênero/ano/tipo de séries validada.
- Interface Chromium em produção: zero erros JavaScript, 48 capas no documento antes/depois da rolagem; cache passou de uma a três páginas. Busca em Séries manteve o escopo. Sessão de QA encerrada.
- Android Connect TV 2.8.0: compilação release aprovada; integridade ZIP, versão, identificador, assinatura original e igualdade dos assets empacotados com a fonte verificados. Nove testes OTA/retry aprovados.

## Limites reais

O índice de filmes atual não fornece anos estruturados: filtro de ano fica indisponível em Filmes. Séries fornece 61 anos. Não há ranking completo de popularidade nem data completa de estreia; nenhuma popularidade foi inventada e Lançamentos usa o ano disponível.

Não houve instalação física do APK 2.8.0 nem teste de reprodução desta versão na TCL. Testes de interface e compilação não comprovam reprodução de todos os títulos/fontes. Download manual não altera os manifestos OTA existentes. Contas, credenciais, banco de clientes, assinaturas, pagamentos e regras de acesso preservados.
