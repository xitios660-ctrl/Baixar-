# GUHFLIX2.4.0 — candidata de recomposição

Não é homologação final da identidade enviada. Alteração estrutural sobre2.3, não somente logo/cores:

- Navegação existente reapresentada como blocos com ícones acima dos rótulos e estados de foco/seleção. Preservados tags/destinos/click handlers.
- Categorias passam do mosaico horizontal para cartões verticais174x206, usando a primeira capa autorizada do próprio catálogo; outras capas continuam componentes exigidos pelo adapter, ocultos.
- Destaque recomposto em194dp com título/sinopse de uma linha, preservando metadados reais, mais linha de ações nativas.
- “Abrir seleção” executa o click handler do cartão selecionado. Não se chama “Assistir” porque alguns cartões abrem categorias ou detalhes.
- “Explorar catálogo” devolve foco ao primeiro cartão disponível. Seleção inicial solicita foco no primeiro cartão carregado para acionar os listeners originais de título/arte e deixar a saudação genérica.
- Fundo cinematográfico/transições2.2 e marca2.3 preservados.

Compilação/assinatura e verificador estático passaram. Não executado nesta versão no Android/TV; nenhuma captura nova, fidelidade integral ou navegação sem regressão afirmada. Validar alturas/recorte, controle remoto, Voltar, foco inicial, ações da seleção e erro de catálogo. Perfis com histórico separado seguem ausentes.

Candidata publicada em URL própria para avaliação, sem substituir alias/release estável2.3. Fontes nativos e nota incluídos no ZIP. Não altera dados/assinatura de conta, banco ou API.
