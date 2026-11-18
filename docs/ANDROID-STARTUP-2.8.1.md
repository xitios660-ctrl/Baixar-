# Gfilmes Connect 2.8.1 — recuperação da abertura

Relato: após a abertura com a marca, a TV passa a uma tela branca antes do catálogo. O serviço MARY e o catálogo web responderam normalmente durante o diagnóstico; isso não identifica sozinho a causa no aparelho. Nenhuma TV física/ADB estava conectada.

## Mudança

A Activity mostra uma cobertura nativa escura enquanto a apresentação local inicia. A cobertura só sai depois de o JavaScript sinalizar que criou a navegação e de o WebView confirmar o estado visual. A prontidão da interface não depende da resposta do catálogo na rede: falha de rede continua mostrando o erro e a opção de tentar novamente na interface.

A inicialização tem prazo de 15 segundos. Falha de bootstrap, carregamento principal ou morte do renderizador permite uma repetição automática em renderização por software. Uma segunda falha mostra recuperação nativa, sem ciclo ilimitado. Os botões permitem tentar novamente, alternar entre modo padrão/compatível e voltar à entrada. O botão Menu do controle também abre a recuperação. Na edição Connect para TV, o primeiro uso desta versão começa em modo compatível; uma escolha local anterior é respeitada. O modo padrão continua selecionável pelo botão Menu. A preferência de compatibilidade é local; contas, senhas e assinaturas ficam preservadas.

Respostas pendentes de uma página descartada são ignoradas por geração, para não preencher uma interface recriada com dados de uma solicitação antiga. Callbacks atrasados de prontidão não escondem a tela de recuperação depois de uma falha. O estilo/animação genérico de Activities não é mais aplicado ao WebView, cuja apresentação e navegação já são próprias.

## Validação

- 8 testes de apresentação em Chromium/WebKit: filtros, grade limitada, navegação e retorno dos detalhes; prontidão com rede sem resposta; falha no bootstrap sem falso sinal de prontidão.
- 4 testes JVM da política: uma repetição automática, descarte de callbacks antigos, repetição manual e morte do renderizador após prontidão.
- 9 testes JVM existentes de OTA/retry preservados.
- Build release aprovado. A integridade, assinatura original e igualdade dos assets empacotados com a fonte são verificadas antes da publicação manual.

Limite: não houve reprodução do defeito no aparelho do titular nem instalação física da 2.8.1. A causa exata da tela branca permanece sem logs do dispositivo; a versão acrescenta recuperação e uma alternativa de renderização para ser testada nessa TV. Testes de navegador/JVM não equivalem a ensaio do WebView Android real.

## Teste no aparelho

Instalar o APK Connect por cima do Connect existente, com a mesma assinatura. Abrir após encerrar completamente o app. Se não entrar no catálogo, usar Modo compatível na recuperação. Conferir login, home, Filmes/Séries, TV ao vivo e retorno do player. Se houver novo erro, registrar a mensagem/versão exibida pela recuperação e o modelo da TV, sem senha.

A publicação é download manual; não modifica os manifestos OTA, não muda endpoints nem exige troca de conta. A edição Android para celular continua com seu download anterior.
