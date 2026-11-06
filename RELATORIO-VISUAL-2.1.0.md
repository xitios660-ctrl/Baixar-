# GUHFLIX TV 2.1.0 — identidade cinematográfica

## Áreas atualizadas

- Início: assinatura editorial SELEÇÃO GUHFLIX, título de maior presença, metadados neutros, sinopse com maior entrelinha e fundo panorâmico com gradientes contínuos.
- Filmes, séries e páginas: destaque mais amplo, carrosséis com espaço para ampliação do foco e tipografia consistente.
- Busca: superfície própria em grafite, teclado separado do resultado, chamada editorial e melhor contraste do campo.
- Biblioteca, favoritos e grades: fundo preto com tonalidade vermelha discreta, título de seção ampliado e cartões com contorno branco no foco. Nomes e conteúdo das seções continuam dependentes do catálogo.
- Detalhes: título com maior peso, ação Assistir vermelha e branca no foco, sinopse legível e hierarquia separada de temporadas/episódios.
- Episódios: cartões grafite com foco branco, títulos ampliados e progresso vermelho preservado.
- Player: hierarquia de título/subtítulo refinada, relógio discreto e barra de progresso mais fina; motor e comandos de reprodução preservados.
- Conta e acesso: painel escuro próprio, assinatura GUHFLIX, campos de alto contraste e foco branco; autenticação existente mantida.
- Ajustes e elenco: superfície e tipografia alinhadas à identidade geral.

Identidade original GUHFLIX, inspirada em padrões de interfaces de streaming. Não é uma reprodução idêntica da Netflix. Não foram reutilizados logotipo, animações ou recursos proprietários da Netflix. Não foram adicionadas capas, trailers ou mídias inventadas.

## Continuidade técnica

Pacote com.guhflix.tv, versão 2.1.0, código 2010000. API Marley preservada. Mesmo certificado das versões anteriores; instalação como atualização. IDs e classes dos 69 layouts existentes preservados, sem alterar contratos do catálogo/player. O observador de foco agora é removido ao destruir a Activity; campos de acesso mantêm seu próprio tratamento de foco.

## Executado nesta versão

- Compilação Java, DEX, recursos Android e Smali: passou.
- Assinatura APK e certificado de atualização: conferidos.
- Verificador estático existente: passou; API correta e ausência de URL local de teste no DEX.
- Contratos de IDs/tipos de views dos 69 layouts antes/depois: preservados.
- XML: todos os arquivos analisados; recursos compilados pelo aapt2.

## Limites reais

A tentativa de iniciar um emulador Android 9 foi impedida pela capacidade de armazenamento disponível: o emulador exigiu aproximadamente 7,4 GB e havia cerca de 5 GB no volume temporário. Assim, esta versão visual não foi executada em aparelho/emulador nesta etapa. Não afirmar teste de navegação, encaixe visual, vídeo, atualização com dados reais, 4K ou 60 FPS como realizado nesta versão. Os testes de autenticação anteriores não substituem homologação desta interface na TCL.

Após instalar, conferir: login; iniciar/retomar filme; busca por controle; ficha; temporadas/episódios; Minha Lista; legenda/áudio; Voltar; conta; encerramento/reabertura. O conteúdo depende das fontes autorizadas e da assinatura ativa. Este trabalho não altera banco, usuários, vencimentos ou regras de pagamento.
