# Local 1.7.0 — renderização e resposta das setas

As versões anteriores forçavam `LAYER_TYPE_SOFTWARE` por padrão na edição Local. Esse caminho desenha também os GIFs, sombras e transformações por software. A resposta do usuário confirmou que as otimizações de layout de 1.6.0/1.6.1 não resolveram a demora na TV. Não houve medição física que determine o peso exato de cada fator.

Na primeira abertura da 1.7.0, a edição Local tenta a renderização acelerada normal da WebView. Uma marca de migração separada evita manter silenciosamente o padrão de software salvo pelas versões anteriores. Depois dessa tentativa, a escolha manual ou a recuperação automática para software permanece salva e é respeitada nas aberturas seguintes. As demais edições não alteram seu padrão de renderização. A recuperação existente mantém uma tentativa automática limitada e o menu de recuperação pelo botão MENU; o botão agora se chama “Aceleração gráfica” ou “Modo compatível (software)”. Se a interface ficar sem desenhar apesar de sinalizar DOM pronto, use MENU e o modo compatível; não se afirma que o detector de prontidão consegue reconhecer toda falha visual de GPU.

Mover entre capas já existentes na mesma linha agora apenas transfere o foco. Não mede a posição da grade, não executa sua janela virtual e não itera sobre todas as capas nesse caminho. A passagem de linhas, rolagem, paginação e recuperação de posições continuam no caminho completo já testado. Setas dos perfis continuam usando a ordem direta da 1.6.1.

Nenhuma redução dos GIFs, personagens, visual, qualidade de vídeo, isolamento de perfis ou controles infantis. Catálogo e fontes têm as mesmas limitações documentadas. Nenhuma alteração em Render, banco, produção ou OTA.

18 testes Chromium passaram, incluindo passagem de quatro capas com zero leituras da geometria da grade, navegação entre perfis, GIFs locais, histórico e PIN infantil. Testes usam ponte nativa simulada. Testes JVM cobrem migração única do modo de renderização, persistência da recuperação e preservação das demais edições. Resultado final dos testes/build no diretório da entrega.

Sem dispositivo Android/TV conectado: não foram medidos FPS, latência real do controle, comportamento do driver GPU, instalação física ou reprodução real. A versão precisa ser validada na TCL. Instale por cima do Gfilmes Local sem desinstalar. A troca de renderização não remove dados.
