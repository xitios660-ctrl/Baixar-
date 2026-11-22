# Gfilmes Local 1.3.0 — uma pessoa por perfil, sem senha

Esta versão corrige a edição Local para uso familiar. Perfis são apenas bibliotecas individuais; não são contas de login. Não há PIN, senha ou código de TV para criar, selecionar, editar ou excluir um perfil. Não altera a edição separada de clientes com licença/API periódica.

## Instalar e usar

1. Instalar o APK 1.3.0 por cima de **Gfilmes Local**, sem desinstalar nem limpar dados. Mesmo pacote `com.gfilmes.tv.local`, versão 1.3.0, código 1003000, assinatura Android original.
2. Na abertura, escolher a pessoa em **Quem vai assistir?**. Se necessário, escolher **Criar perfil**, informar o nome, escolher uma cor/avatar e salvar. São até seis perfis. Em uma instalação vazia, Assistir agora continua disponível para entrar com um perfil Minha TV, que pode ser renomeado depois.
3. Cada pessoa vê seu próprio Continue assistindo, favoritos e progresso nesta TV. Para outra pessoa, abrir Minha TV → Trocar perfil.
4. **Gerenciar perfis** permite editar nome/avatar ou excluir um perfil. Trocar o avatar não envia o formulário. Exclusão tem confirmação na própria tela. O botão Voltar cancela a edição/gerenciamento; no seletor principal, oferece saída do aplicativo.

Os PINs antigos dos perfis são removidos automaticamente, preservando IDs, nomes e avatares. Os arquivos de biblioteca continuam associados aos mesmos IDs: renomear ou remover o PIN não transfere o histórico para outra pessoa. Perfis e histórico são locais a cada instalação; esta versão não sincroniza televisões pela nuvem.

## Correções

- Seletor redesenhado para 1280×720 com avatares locais desenhados em CSS, foco para controle remoto e gerenciamento separado. O formulário contém somente nome e escolha de avatar; botões ficam dentro da tela no teste de 720p.
- Eliminados campos, bloqueios e verificações de senha de perfil. Não é necessário lembrar o PIN anterior para atualizar.
- Respostas atrasadas de Continue assistindo são descartadas quando muda a página, a geração da interface ou o perfil. Um pedido antigo de Ana não deve substituir a fileira de Bruno.
- O player captura o perfil que iniciou a reprodução no Intent. Salvamentos tardios do player continuam no armazenamento daquela pessoa, mesmo se o seletor já tiver mudado.
- Histórico de detalhes da sessão separado por perfil. A entrada de reprodução não abre tardiamente por cima de outro perfil.
- Coleções/animes passam a registrar entrada no histórico da interface, e a referência nativa de coleção é restaurada ao carregar esse histórico. Isso corrige futuras reproduções; não inventa entradas ausentes de versões anteriores.

## Referência visual enviada

O endereço `chatgpt.com/library/share/libfile_9e60014eb14c819195075fd08c1a750e` devolveu HTML da página geral ChatGPT, sem a imagem ou referência do arquivo. Portanto o visual implementado não foi apresentado como cópia dessa imagem. Foi solicitado que a pessoa anexe a referência diretamente. A prévia da entrega usa seis nomes fictícios exclusivamente para testar o layout; não são contas criadas no MongoDB.

## Validação e limites

A suíte Chromium cobre criação/seleção sem senha, botões de avatar sem envio acidental, histórico de duas pessoas, resposta antiga atravessando troca de perfil, seis cartões em 720p, setas do controle e edição/exclusão. É um catálogo sintético e uma bridge simulada, não execução de APK na TCL.

Os testes JVM verificam migração dos registros antigos sem trocar os IDs, persistência/reabertura, seleção sem credenciais, nome/capacidade, edição antes de entrar e exclusão sem afetar outro perfil. A prova física de salvamento do ExoPlayer, instalação na TCL, teclado/controle e reprodução audiovisual continua pendente do aparelho real.

Catálogo, imagens e vídeos online continuam dependendo da internet e das fontes configuradas. Não foram alterados clientes, pagamentos, MongoDB, serviços Render, site ou OTA de produção. Atualização manual desta edição local.
