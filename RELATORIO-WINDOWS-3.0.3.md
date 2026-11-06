# GUHFLIX Windows 3.0.3 — reprodução de filmes sem cair em canais

## Problema encontrado no código

O usuário selecionou A Odisseia e informou que apareceu Ao vivo. A inspeção
mostrou outro caminho além da falha de desenho corrigida na 3.0.2: ao esgotar
as fontes de um filme, proxima_fonte_vod removia tocando_vod. Como filmes e
canais compartilham o container de player Aba::Canais, a tela passava a
mostrar a lista de canais. A reprodução normal concluída também abria a lista.

Havia ainda retomada automática do último canal após receber o catálogo,
inclusive enquanto o usuário navegava por filmes.

## Alterações

- Falha de todas as fontes mantém título, URLs, episódio e temporada.
- Mostra uma mensagem persistente com Tentar novamente e Voltar ao catálogo.
- O estado de erro bloqueia tentativas automáticas, timeouts e eventos tardios
  até o usuário pedir uma nova tentativa.
- Abrir um filme fecha lista e guia de canais e impede retomada tardia do canal.
- Retomada do último canal não é executada enquanto o catálogo está aberto.
- Fim normal do filme volta ao catálogo e mantém o título concluído fora de
  Continuar assistindo. Não abre a lista de canais.
- Atalhos de trocar canal e números não substituem um filme em reprodução.
- Mantidas a proteção de desenho da 3.0.2, compilação MSVC x64 com runtime C
  estático, API de produção, autenticação e DLLs do player.

## Limites

Não foi reproduzida a reprodução real de A Odisseia no computador do usuário.
Não é alegado que as fontes desse título estejam funcionando. Nenhum stream,
autenticação, DRM ou restrição de acesso foi alterado. A correção trata a
transição incorreta para canais quando o vídeo não abre.

O CI sem OpenGL 2.0 suficiente verifica chegada ao main e tratamento de erro
gráfico; não valida vídeo, áudio ou interface no PC físico do usuário.

## Validação

Fonte: commit cb15423, branch guhflix-windows.
Testes de regressão cobrem A Odisseia com esgotamento de fontes, preservação
de episódio/temporada e título sem fontes. Não acessam streams reais.

Workflow executado: 42289015715; job 128137286852.
Resultado: 91 testes aprovados, 0 falhas, 8 ignorados.
Build release MSVC x64 e verificação de início concluídos com sucesso.
O executável registrou a versão 3.0.3, reservou a instância e alcançou
a criação do contexto gráfico; o runner gerou o erro controlado por não
dispor de OpenGL 2.0 suficiente. Importações PE/x64 conferidas.
Não foi executada reprodução real do título.
