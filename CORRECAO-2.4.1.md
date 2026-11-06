# GUHFLIX2.4.1 — correção de sobreposição

O usuário validou visualmente a candidata2.4.0 na TV e relatou os botões Abrir seleção/Explorar catálogo aparecendo por trás das capas ao descer. A faixa acrescentada em ReferenceHome podia exceder o bloco de altura fixa com clipping desativado.

Removida integralmente a faixa redundante e seus elementos de foco/handlers. A ação OK dos cartões originais continua responsável por abrir categorias/detalhes/conteúdo. Ativado clipping do destaque para conter texto dentro da área; altura reduzida de194 para164dp após retirar a faixa, devolvendo30dp às fileiras. Mantidos os cartões, cabeçalho, arte e animações2.4.0.

Executado: compilação Java/DEX/Smali/recursos, verificador estático e assinatura. Confirmada ausência das duas legendas no DEX assinado; inicialização do foco no catálogo preservada. Sem dispositivo Android conectado para repetir a navegação nesta versão: correção de runtime ainda requer confirmação na TV. Não alegar testes de vídeo/FPS/perfis. Perfis isolados continuam pendentes, sem relação com esta correção.

Instalar2.4.1 como atualização e descer/subir várias fileiras: não deve existir mais essa faixa de botões.
