# GUHFLIX · testes de quatro horas, player web e Roku

## Acesso de teste

No painel, Novo cliente oferece Teste gratuito de 4 horas como opção inicial. Não registra pagamento nem concede 30 dias. Para clientes pendentes ou vencidos, TESTE 4H concede um teste único de quatro horas; chamadas repetidas com a mesma chave não prolongam o prazo. Um acesso ainda ativo não é substituído e um bloqueio manual permanece. O servidor verifica o mesmo vencimento em todos os dispositivos.

## Reprodução no celular

Sem uma TV selecionada e online, Assistir abre o player local. Em Minhas televisões, Neste aparelho remove o destino remoto. Com uma TV selecionada e online, o filme continua sendo enviado exclusivamente à TV escolhida. MP4 compatível utiliza o player do navegador; HLS usa o Safari ou HLS.js 1.7.4 nos navegadores com MediaSource.

A reprodução web usa o mesmo limite transacional de três telas. URLs de mídia são concessões temporárias vinculadas ao usuário, sessão e reprodução. A cada solicitação de mídia o servidor confere acesso, revogação e concessão ativa. O relay só abre fontes resolvidas do catálogo e referências autenticadas de seus manifests; não aceita URLs arbitrárias do cliente. Verifica DNS público, fixa o endereço da conexão, rejeita redes locais/reservadas e verifica cada redirecionamento. Reescreve HLS e permite Range em MP4. Não remove DRM nem converte codecs incompatíveis. Legendas e faixas dependem do formato e do navegador.

O player mantém o título e o progresso ao alternar fontes e repetir com espera de até 30 segundos. O usuário pode fechar ou tentar novamente. O servidor atual usa transferência de mídia para o navegador: disponibilidade e capacidade de tráfego dependem do provedor e da hospedagem.

## Identidade

TV Connect/Android 2.6.1 e Windows 3.1.1 revisam os nomes de conexão, mensagens, avisos e metadados de mídia visíveis ao cliente para GUHFLIX. Identificadores internos herdados, dados persistidos e integrações autorizadas continuam compatíveis; não se afirma remover todas as palavras dos arquivos binários ou do código-fonte. Créditos e licenças de bibliotecas são preservados.

## Roku

Novo canal nativo SceneGraph/BrightScript: login e renovação de sessão, catálogo paginado, busca, detalhes, temporadas/episódios, recomendações, player HLS/MP4, retentativas, limite de telas e vínculo com o celular. A credencial de acesso é digitada na TV; só o token de renovação é persistido no registro do canal. A senha não é salva.

O compilador BrighterScript 0.73.8 validou o projeto e gerou o ZIP de desenvolvedor. O pacote foi inspecionado quanto à estrutura e ausência de credenciais. Não houve instalação em Roku física. Para clientes instalarem pela loja, falta validar no aparelho, gerar o pacote assinado com a identidade de desenvolvedor Roku e publicar o canal. O ZIP não equivale a um canal aprovado pela loja.

Referências oficiais: [instalação de desenvolvimento Roku](https://developer.roku.com/dev/docs/developer-setup), [player Roku](https://developer.roku.com/dev/docs/video-player), [HLS.js 1.7.4](https://github.com/video-dev/hls.js/releases/tag/v1.7.4).

## Verificação

- API: compilação e dez testes, incluindo vencimento de quatro horas, ausência de cobrança, idempotência, limites de telas, URLs autenticadas, revogação, Range, HLS, isolamento de contas e endereços privados.
- Web: oito cenários de catálogo/controle já aprovados nas duas engines; dois cenários adicionais reproduziram MP4 e HLS sintéticos em WebKit/iPhone e Chromium/Android, sem TV selecionada.
- Painel: dois testes em desktop/iPhone aprovaram criação de cliente com teste de quatro horas e zero pagamentos.
- Transporte de mídia: arquivo público de teste respondeu HTTP 206 via relay validado.
- Windows: CI 42450447854, 96 testes aprovados, nove ignorados e um HLS sintético adicional aprovado. A criação da janela chega ao limite gráfico do runner, sem OpenGL 2.0 disponível.
- Android: releases assinados e nove testes JVM por edição; assinatura, launcher, componentes e atualização verificados.

As contas e os vídeos dos testes são fixtures. Nenhum cliente real foi renovado. Não foi executada reprodução em Roku, TCL ou celular físico, nem verificado todo o catálogo comercial.
