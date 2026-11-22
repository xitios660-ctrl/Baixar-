# Navegação dos perfis — Local 1.6.1

As setas na seleção de perfis agora utilizam a ordem dos cartões, sem medir a geometria de todos os botões e sem chamar o ajuste de rolagem a cada tecla. A tela comporta os seis cartões em uma linha. Esquerda/direita ficam na linha; para baixo vai ao gerenciamento e para cima retorna aos perfis. No editor e no PIN, a navegação espacial existente continua disponível. A navegação espacial reutiliza as medidas obtidas na mesma tecla.

Não altera GIFs, personagens, autenticação, isolamento do histórico ou regras infantis. Não muda renderização por software/hardware. Inclui as otimizações de catálogo da versão 1.6.0.

17 testes Chromium passaram, incluindo os GIFs locais, controle infantil, histórico separado, editor em resoluções de TV e novo teste com 16 movimentos laterais: zero medições de geometria pelo aplicativo e preservação dos mesmos elementos de avatar. Esses resultados são de navegador com ponte nativa simulada; não medem FPS nem comprovam fluidez na TCL física. Não houve alteração de código nativo de perfis; os 62 testes JVM da 1.6.0 não foram repetidos nesta alteração de interface.

Instalar por cima do Gfilmes Local, sem desinstalar. Produção e OTA permanecem inalterados.
