# GUHFLIX Windows 3.0.4 — autorização e abertura do vídeo

## Evidência

O usuário confirmou que GUHFLIX 3.0.3 falha nas quatro fontes enquanto o mesmo
filme funciona no Saímo no mesmo Windows. Não foi atribuída a falha ao conteúdo.

Comparado o fonte base SaimoTV/SaimoWin, commit
b22391e1a6a73902710ad330a15e5aa0ef2266cd, com a personalização:
- vod.rs e listas.rs idênticos ao fonte base;
- DLL libmpv-2.dll idêntica ao pacote original fornecido pelo usuário:
  d2d081547efea8a1530f4ba0bf58a0d72069b25c40d8b2a7071cea5aa82381c6;
- vulkan-1.dll também idêntica ao pacote original;
- mesmo User-Agent e opções de cabeçalhos Accept;
- diferença relevante: loadfile passa por autorização assíncrona no GUHFLIX.

Foi encontrado um defeito na contagem de prazo: o relógio de 14 segundos
começava antes de a autorização/fila HTTP terminar. Isso podia cancelar e
substituir uma fonte antes de ela receber o mesmo prazo de abertura do Saímo.
Esse defeito foi corrigido; não é alegada comprovação de que seja a única
causa da falha do filme no computador do usuário.

## Alterações

- O prazo de mídia começa apenas no despacho autorizado ao libmpv.
- Fila e autorização da conta não consomem o prazo do vídeo.
- Gerações antigas e cancelamentos não iniciam o relógio de outro pedido.
- Conta, assinatura, limite de telas, watchdog e autorização continuam obrigatórios.
- Erros retornados pelo comando loadfile deixam de ser ignorados.
- O erro nativo do libmpv é registrado e mostrado se todas as fontes falharem.
- Avisos de HTTP, rede e decodificação são reduzidos a categorias sem copiar
  URLs, cabeçalhos ou tokens para o diagnóstico.
- Mantidos player, fontes, cabeçalhos e proteção de navegação da 3.0.3.

## Teste de mídia

Criada uma fixture própria com barras de teste e tom sintético por FFmpeg.
O teste serve HLS local como manifest.txt e segmentos .js, conferindo os
cabeçalhos do aplicativo original. Usa a DLL original com vo=null, ao=null e
decodificação por software somente no teste, sem janela ou dispositivo físico.
Esse caminho não é oferecido como alternativa ao controle de acesso do catálogo.
Também é exercitada resposta HTTP 404 para verificar o erro capturado.

## Limites

Não foi executada a reprodução real de A Odisseia no PC do usuário. Teste de HLS
sintético não comprova a disponibilidade de um stream específico, nem valida
GPU, som e legendas no computador físico. Não foi alterado DRM, autenticação,
servidor de origem, cache do cliente, MongoDB ou API de produção.

## Resultado executado no Windows

Fonte f0440d8, workflow 42291866286, job 128147045223.
- 94 testes de lógica aprovados; 9 inicialmente ignorados.
- Executado separadamente o teste de mídia anteriormente ignorado: 1 aprovado.
- HLS .txt com segmentos .js carregou e avançou 1,42 s com a DLL original.
- Resposta HTTP 404 capturada como mensagem reduzida, sem URL ou token.
- Autorização atrasada/cancelada e relógio de mídia cobertos por testes.
- Build nativo MSVC x64 com runtime C estático concluído com sucesso.
- Entrada no main/contexto gráfico verificada; a janela normal do runner ainda
  encontra a limitação de OpenGL, tratada e registrada como erro controlado.
- 8 testes de rede restantes não executados.

Os testes de autorização usam um servidor local de teste; o teste de mídia
é independente e usa conteúdo sintético. Não houve reprodução do título real
com uma conta de cliente, nem teste de áudio/GPU físicos.
