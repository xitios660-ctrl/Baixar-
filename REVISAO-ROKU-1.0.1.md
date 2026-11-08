# GUHFLIX Roku 1.0.1 · revisão para Philco Roku OS 16.0

A foto do usuário identifica uma Philco Roku TV, hardware H54X, com Roku OS 16.0. Esta plataforma usa um canal SceneGraph/BrightScript próprio. O APK Connect/Android 2.6.2 é destinado à TV Android das fotos anteriores.

Revisão 1.0.1: o catálogo usa uma fileira visível com rolagem entre categorias, capas grandes e título/metadados do foco na parte inferior, dentro da área HD 1280×720. A configuração anterior pedia duas fileiras de 330 pixels abaixo do cabeçalho, ultrapassando a área visível. O novo limite foi conferido a partir das dimensões do XML.

A entrada Main agora recebe argumentos de abertura e eventos roInput enquanto o canal está aberto, conforme o suporte declarado no manifesto. Somente IDs do catálogo no formato esperado são aceitos; login e acesso continuam obrigatórios. O pedido de reprodução aguarda a abertura animada e, em séries sem episódio explícito, abre a escolha de temporada. A integração de rede continua em Tasks, com estado final em minúsculas conforme a documentação oficial.

Validação executada: compilador BrighterScript 0.73.8; XMLs e recursos pkg:/ no ZIP; dimensões do catálogo HD; vínculo entre roInput e a Scene; pacote sem arquivos de credenciais. O pacote não foi instalado nem reproduziu vídeo na Philco física. Não se declara aprovação na loja Roku.

Para testar: habilitar o instalador de desenvolvedor na própria Roku e enviar GUHFLIX-Roku-1.0.1-desenvolvedor.zip pelo computador na mesma rede. A documentação de instalação está em https://guhflix-tv-download.onrender.com/roku.html. A chave de assinatura Android não assina canais Roku; distribuição pela loja ainda requer a identidade e o processo de publicação Roku.

Referências oficiais: [Task](https://developer.roku.com/dev/docs/task), [RowList](https://developer.roku.com/dev/docs/rowlist), [roInput](https://developer.roku.com/dev/docs/roinput), [instalação para desenvolvimento](https://developer.roku.com/dev/docs/developer-setup).
