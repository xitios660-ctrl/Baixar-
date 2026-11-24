# Local 1.8.1 — índice reutilizável de pesquisa

Cada novo texto digitado criava um plano que voltava a normalizar os títulos, ordenar resultados e reconstruir facetas sobre dezenas de milhares de registros. A nova `CatalogSearchIndex` prepara os títulos normalizados uma vez por escopo e guarda as ordens solicitadas. Consultas seguintes fazem correspondência de texto sobre esse índice, sem repetir normalização ou ordenação. Gêneros e anos também são reutilizados por tipo.

O cache de planos de resultados continua limitado a 12; índices de escopos são limitados a quatro. O escopo infantil inclui o hash da lista de aprovações. Uma atualização da fonte invalida tanto os índices quanto os planos. As consultas não acessam uma API de clientes para localizar títulos. O primeiro uso de uma ordem ainda precisa prepará-la; não se promete custo zero na primeira busca nem tempo medido na TCL.

Os resultados preservam as regras anteriores: tipo, título sem distinguir acentos/caixa, aliases de gêneros, ano, A–Z/Z–A, ano crescente/decrescente e desempates. Paginação permanece em janelas de 60 registros. Não muda o layout, os GIFs, a ativação, assinaturas ou as fontes.

Testes novos comparam combinações de filtros/ordens com o algoritmo existente; validam que digitação sucessiva lê o catálogo do escopo só uma vez e que a invalidação o relê; exercitam 20 consultas em 40 mil títulos com registro de tempo no computador de testes. Não é benchmark físico da TV. Resultados finais no diretório da entrega.

Inclui as correções parciais de conteúdo da 1.8.0: abrir série sem resolver fontes de vídeo e confirmar vídeo pela primeira imagem. Permanecem pendentes as sinopses reais e a confirmação física dos casos Os Simpsons, Alive, #AMFAD e Band. Encontrar um registro não confirma que a fonte dele reproduz. Instalar por cima do Gfilmes Local sem desinstalar. Produção, Render, banco e OTA inalterados.
