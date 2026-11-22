# Gfilmes Local 1.5.0 — catálogo salvo antes da atualização

Instalar por cima de **Gfilmes Local**, sem desinstalar/limpar dados. Mesmo pacote `com.gfilmes.tv.local`, versão 1.5.0, código 1005000, Android 6.0+, assinatura original. Perfis, histórico, progresso, preferências, controle infantil e os 12 avatares permanecem.

A abertura deixa de esperar índice de fontes, fichas e normalização do player. Monta os títulos salvos com IDs compatíveis e atualiza em segundo plano, sem esvaziar a home em caso de erro. Transferências têm trava por arquivo e proteção contra sobrescrever cache com respostas inválidas.

**Bloqueio real encontrado:** as consultas normais às três URLs configuradas receberam HTTP 403 neste ambiente; no detalhe de busca, Cloudflare `error code: 1010`. Não foi confirmado que seja a mesma resposta da TV. Não houve mudança de identidade/headers, proxy, host ou rota para contornar o bloqueio.

Para uma instalação sem cache, a fonte padrão pode usar metadados históricos reais já preservados no projeto: **28.362 filmes e 9.854 séries**, arquivo local de 07/11/2026. É um catálogo salvo, não uma captura atual. Não inclui servidores de vídeo, filmes, credenciais ou clientes. Fontes personalizadas/desativadas não são substituídas pelo snapshot padrão.

**Esta versão recupera a listagem; não comprova reprodução.** Vídeos/capas continuam dependendo das fontes e da internet. Se o provedor recusar os arquivos necessários à reprodução, exibir os títulos não libera o acesso. A liberação depende do responsável pela fonte.

57 testes JVM e 15 Chromium passaram. A cópia histórica real foi carregada no teste JVM sem rede; a UI usa bridge simulada. Testes cobrem IDs antigos, resposta HTTP 403 mantendo a home, cache intacto, concorrência por arquivo, avatares, foco e perfis. Instalação/abertura/reprodução reais na TCL ainda dependem de teste físico.

[Manifesto, assinatura e SHA-256](APK-MANIFEST.json) · [Resultados e tempos medidos](TEST-RESULTS.json) · [Código desta correção](https://github.com/xitios660-ctrl/Baixar-/tree/73794ad) · [Detalhes de diagnóstico e limites](../../docs/local/CATALOGO-SALVO-1.5.md)

API MARY, MongoDB, pagamentos, Render, downloads/OTA de produção e a edição de clientes com licença mensal não foram alterados.
