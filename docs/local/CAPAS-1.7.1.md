# Local 1.7.1 — setas nas fileiras de filmes e séries

O usuário confirmou melhora na 1.7.0, mas ainda percebe demora nas capas. A navegação horizontal das fileiras da tela inicial ainda seguia o algoritmo espacial genérico, que media todos os controles visíveis em cada tecla. Agora esse caminho usa os irmãos da capa focada e apenas os offsets da própria fileira para manter a próxima capa visível. Setas verticais e outros controles continuam usando navegação espacial; a grade virtual mantém o caminho rápido da 1.7.0. Limites das fileiras não fazem o foco saltar para outro controle inesperado.

A borda das capas mantém espessura de um pixel ao receber foco e ganha contorno interno vermelho. Assim, a imagem não muda suas dimensões internas ao receber/perder foco. O brilho estático permanece, mas não há mais interpolação de sombra quadro a quadro na grade. Personagens, GIFs, perfis, qualidade do vídeo e aceleração gráfica da 1.7.0 preservados.

19 testes Chromium passaram. Novo teste: dez movimentos consecutivos numa fileira de vinte filmes, sem chamadas de medição de retângulos; foco correto e rolagem horizontal efetiva; dimensões e borda iguais entre capas focadas e não focadas. Demais testes incluem grade com milhares de resultados, séries, perfis, GIFs e controle infantil. Esses testes são de navegador com ponte simulada e não medem latência física na TCL. Nenhum código nativo funcional mudou; os 65 testes JVM da 1.7.0 não foram repetidos.

APK deve ser instalado por cima do Gfilmes Local, sem desinstalar. Assinatura e hash registrados na entrega. Nenhuma alteração na produção, OTA ou serviços. A fluidez real continua dependendo de validação na TV.
