# GUHFLIX Windows3.0.0 — entrega nativa

## Aplicação

Programa Rust/egui/OpenGL/libmpv recompilado a partir da base pública SaimoWin2.6.7, commit upstream b22391e1a6a73902710ad330a15e5aa0ef2266cd. Código personalizado autorizado pelo usuário. Créditos e documentação originais preservados. Fonte GUHFLIX: https://github.com/xitios660-ctrl/Baixar-/tree/guhflix-windows, revisão44bbc9f. Não é o pacote experimental de launcher/CMD: o novo GUHFLIX.exe contém a interface e a integração.

## Implementado

- Identidade preta/vermelha, marca GUHFLIX, ícone próprio da janela/EXE, abertura nativa própria de1,4s com opção de pular, início no catálogo e Conta no cabeçalho.
- Busca, categorias, favoritos, continuar assistindo, detalhes, temporadas/episódios, ao vivo, rádio, player, áudio/legendas e configurações da base preservados; disponibilidade e comportamento de mídia ainda dependem das fontes e do aparelho.
- Login com a mesma conta de CLIENTE ativa da TV no servidor Marley. A interface e os comandos ficam bloqueados até validar acesso. Conta mostra vencimento, sessões conectadas, confirmação para revogação e logout.
- Refresh salvo com DPAPI por usuário do Windows, token de acesso em memória, identificação de instalação e proteção contra duas cópias concorrentes renovando o mesmo token.
- Cada player recebe autorização antes do comando loadfile. Solicitações e refresh são serializados; início de vídeo ocorre fora da thread de desenho e o cancelamento de uma solicitação impede início tardio. Todos os players de multitelas contam separadamente.
- Heartbeat30s, watchdog monotônico105s na thread do player, independente da interface/requisição de rede. Servidor mantém lease120s. Ao fechar, para a reprodução e tenta liberar as vagas por até2s; sem rede o servidor expira a reserva automaticamente.
- Limite global3 reproduções por conta, somando Android/TV e Windows. A API foi ajustada para contar múltiplos players de uma sessão Windows, mantendo o limite e o estado dos demais ao parar um deles.
- Pool HTTP compartilhado para metadados; cache-pause-initial=no sem mudar qualidade nem buffer de recuperação. Nenhum aumento de banda ou ganho real de tempo até primeiro frame foi medido.
- Consulta de atualização pelo endereço oficial windows-native-release.json, com download do ZIP no navegador, sem instalação silenciosa.

## Testes executados

- Rust no hostLinux:85 testes passaram,0 falharam,8 testes de rede externa ignorados. Inclui refresh401 com rotação/repetição única, bloqueio403, recusa409 da quarta reprodução, cancelamento de autorização em voo sem iniciar/revogar a posterior e prazo monotônico do watchdog.
- GitHubActions em runnerWindows:86 testes passaram,0 falharam,8 ignorados, incluindo roundtrip real WindowsDPAPI. Fluxo https://github.com/xitios660-ctrl/Baixar-/actions — branch guhflix-windows. Builds nativos Windows e cross-compilaçãoWindowsx64 aprovados.
- API em Mongo replica set DESCARTÁVEL: autenticação/RBAC/injeção, vencimento, pagamento duplicado atômico,3/4 reproduções concorrentes entre sessões,3/4 players concorrentes na mesma sessão, parar um player preservando os demais, heartbeat, expiração, bloqueio, rotação/reuso de refresh, revogação, logout e métricas passaram. Nenhum pagamento/cliente de produção criado para testes.
- Interface nativa executada no Linux em ambiente gráfico isolado com API de teste: login de cliente temporário ativo abriu a interface; bloqueio pelo administrador retornou a tela de acesso bloqueado automaticamente. Essas capturas são testes Linux, não fotos do Windows.
- EXE entregue conferido como PEAMD64/subsistemaGUI, com recursos de ícone/versão e apenas dependências de sistema além das DLLs originais do pacote. Buildrelease possui APIHTTPS de produção fixa e não contém GUHFLIX_TEST_API. DLLs libmpv-2.dll/vulkan-1.dll preservadas byte a byte a partir do ZIP fornecido.

## Limitações e validação pendente

Não foi feita reprodução real em PCWindows físico, nem medição de primeiro frame, navegação completa com catálogo, GPU, áudio/legendas, troca de episódios, reinício real com sessão persistente ou4K/60FPS. Teste DPAPIWindows e compilação não substituem essa homologação. A interface não é uma réplica pixel a pixel da prancha enviada. Perfis com históricos separados não estavam implementados no Android atual e não foram simulados no Windows. O pacote não contém credenciais MongoDB/administrador nem chave de assinatura de servidor. O EXE não tem certificado Authenticode fornecido; não declarar como assinado digitalmente.

## Instalar

Extraia TODO o ZIP, abra GUHFLIX.exe e use a conta de cliente do painel GUHFLIX. Não abra o EXE de dentro do ZIP nem mova as DLLs para outra pasta. Dados novos ficam em %APPDATA%\GUHFLIX; os dados SaimoTV antigos permanecem intactos e não são importados automaticamente. Fontes autorizadas devem estar disponíveis e a assinatura ativa. A API no plano gratuito do Render pode precisar iniciar após inatividade; o login aguarda prontidão por um período limitado e permite tentar novamente. Não houve contratação ou mudança de plano.
