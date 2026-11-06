# GUHFLIX TV 2.2.0 — início e catálogo com motion

## Implementado

- Camada CinemaScene nativa sob o conteúdo do início e das páginas do catálogo: fundo preto com iluminação vermelha discreta, enquadramento panorâmico da arte do cartão selecionado, máscaras de contraste horizontal/vertical e dissolvência de 420ms quando a imagem muda.
- A arte vem das próprias capas já carregadas pelo catálogo. A camada não faz novas chamadas, não inicia vídeos, não inventa trailers nem altera título, sinopse ou seleção do provedor. A qualidade do fundo depende da resolução da capa disponível; o destaque nativo de alta resolução continua preservado quando fornecido.
- Entrada do bloco de destaque com opacidade/deslocamento, 460ms. Foco de cartões com ampliação discreta e sombra, 170ms. Sem animação infinita, partículas permanentes ou desfoque de alto custo.
- Respeito à escala de animação do Android: se desativada, troca direta e foco sem animação.
- Cartões reais de categorias (item_secao), antes fora do tratamento: largura reduzida, mosaico de capas preservado, cor do catálogo atenuada, ícones circulares ocultos, título editorial e foco branco.
- Abertura da página inicial com “Sua próxima história”, chamada mais curta, menos texto de instrução e títulos de carrosséis ampliados.
- Capas mais altas com legenda de uma linha e carrosséis permitindo projeção do foco. Cartões panorâmicos com imagem preenchida.
- Cabeçalho mais compacto, mantendo os destinos de navegação e Conta.
- Observador de desenho e animador removidos ao destruir a tela; imagens anteriores liberadas ao concluir a transição. Nenhuma alteração em autenticação, pagamentos, banco, streams ou motor do player.

## Verificações executadas

- Compilação Java8, DEX, Smali e recursos aapt2: passou.
- Verificador estático existente: passou.
- IDs/tipos dos componentes dos 69 layouts: preservados.
- CinemaScene, observador de desenho, remoção do observador e configuração de movimento: presentes no DEX assinado.
- Pacote com.guhflix.tv, versão2.2.0/código2020000, assinatura conferida e mesmo certificado de atualização.

## Limites

A versão2.2.0 não foi executada em TV/emulador nesta etapa. A tentativa de emulador na etapa anterior foi bloqueada por armazenamento insuficiente; não foi repetida sem resolver esse requisito. Fotos recebidas são referência para a alteração, não evidência visual da nova versão. Animações, foco, encaixe, reprodução e desempenho precisam de validação na TCL; 60FPS e4K não foram medidos. Não é uma réplica idêntica da Netflix, e não utiliza seus logotipos/animações proprietários.

## Instalação/validação

Instalar GUHFLIX-TV-2.2.0.apk como atualização, sem desinstalar a versão atual. Testar início, foco em Continuar assistindo, Séries/Filmes, transição de foco entre categorias, busca e reprodução/Voltar. A assinatura e as fontes autorizadas existentes continuam necessárias.
