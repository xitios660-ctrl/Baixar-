# Gfilmes Android / Android TV 2.7.2

A interface principal foi reconstruída a partir das três referências fornecidas pelo titular: entrada de filmes/séries com cartões de universos, catálogo com destaque e fileiras, e canais com guia. O desenho usa uma área lógica 1280 × 720 escalada para o visor, fontes empacotadas, foco vermelho, menu horizontal e conteúdo real. Os nomes, capas, quantidades, horários e progresso refletem os dados disponíveis, não os exemplos ilustrativos das referências.

O APK contém uma apresentação local em WebView e uma ponte nativa com operações restritas. Tokens e senhas não são entregues ao JavaScript. Login, assinatura da atualização, autorização de cliente e reprodução permanecem nativos. A ponte não permite URLs de API arbitrárias, acesso a arquivos nem navegação externa. O player existente e o controle do celular são preservados.

Filmes e séries usam a API autenticada. Animes usa a coleção já existente no cliente; indisponibilidade dessa fonte não impede abrir Filmes/Séries. O guia usa programação efetivamente disponível no cache nativo; sem dado válido, exibe “Programação não informada”. A arte do estádio é decorativa, não uma captura nem comprovação de uma transmissão.

Minha lista é guardada no aparelho por conta. Continue assistindo usa o progresso real do player e os títulos abertos pela nova interface na mesma conta. Na primeira abertura, tenta migrar até 12 títulos do histórico local anterior para a conta atualmente autenticada, somente quando há correspondência segura no catálogo; mantém o histórico original intacto e não repete essa migração para outras contas. Não inventa durações, posição assistida, qualidade, ranking ou programação. Séries e animes permitem selecionar temporadas e episódios.

Verificação: compilação Kotlin/Android; testes do arquivo real empacotado em Chromium e WebKit para geometria 1280 × 720, pesquisa, filtros, detalhes/episódios, comandos nativos, programação disponível/ausente, vínculo de conta, configurações e navegação por setas. As imagens de teste usam dados sintéticos identificados como teste. Capturas adicionais com conta de cliente e API de produção verificam a apresentação do catálogo no navegador. Isso não é execução numa TV Android nem teste de mídia físico.

Instale a edição correspondente ao pacote existente, por cima e sem desinstalar. A edição Connect mantém com.guhflix.tv.connect; a edição Android mantém com.guhflix.android. Ambas mantêm a chave de assinatura anterior. Compatibilidade física do WebView, controle remoto, decodificação, áudio, legendas e fontes depende do aparelho e deve ser distinguida da compilação e dos testes de navegador.

Não altera a aplicação Samsung no Seller Office, não publica política de privacidade nem envia análise para loja.
