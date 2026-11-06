# GUHFLIX Windows3.0.0

Aplicativo nativo Rust/egui/OpenGL/libmpv, personalizado sobre SaimoWin2.6.7 a partir do fonte público https://github.com/SaimoTV/SaimoWin (commit b22391e1a6a73902710ad330a15e5aa0ef2266cd). Personalização autorizada pelo usuário. Créditos e documentação da base preservados em README-BASE-Saimo.md e arquivos originais. Não é uma página WebView nem um launcher externo.

## Implementação GUHFLIX

Identidade preta/vermelha, marca, ícone, abertura nativa própria, início no catálogo, busca, favoritos, detalhes, temporadas/episódios, retomada, configurações e player da base. Login nativo contra https://guhflix-marley-api.onrender.com com conta de cliente do mesmo painel da TV. Conta/renovação/encerramento de sessões e logout. Refresh protegido via DPAPI por usuário do Windows, tokens de acesso somente em memória, instalação identificada por UUID e instância única para evitar corrida entre processos.

Cada player pede autorização antes de loadfile; chamadas de rede não bloqueiam desenho. Cancelamento/geração evita início tardio de pedido substituído. Heartbeat30s, watchdog monotônico105s na thread do mpv para interromper vídeo mesmo se UI ou rede parar; servidor reserva120s. Multitelas conta cada player separadamente no limite global3; depende do backend atualizado na branch guhflix-platform. Validação e bloqueio são feitos no servidor. Sem licença offline/contorno de autenticação.

Abertura do vídeo mantém cache-pause-initial=no, cache de recuperação e qualidade do provedor. Metadados usam pool HTTP compartilhado. Não há afirmação de ganho medido de banda/tempo até primeiro frame.

## Dados e atualização

Dados novos em `%APPDATA%\GUHFLIX`, sessão `guhflix-session.dpapi`; dados antigos SaimoTV permanecem intactos e não são copiados automaticamente. Atualização lê somente windows-native-release.json do site GUHFLIX e oferece ZIP oficial no navegador. Não instala silenciosamente. Preço atual R$30/30dias; conta precisa estar ativa. Perfis com histórico isolado não existem na versão Android atual nem foram inventados no Windows.

## Compilar

Rust stable compatível com Cargo.lock. Windows: `cargo test --locked` e `cargo build --release --locked`. No host Linux: instalar target x86_64-pc-windows-gnu e MinGW-w64; `cargo build --release --locked --target x86_64-pc-windows-gnu`. WINDRES configura o compilador de recursos. Colocar libmpv-2.dll e vulkan-1.dll fornecidas pelo usuário ao lado do GUHFLIX.exe. Não incluir sessões/credenciais no pacote. O executável não possui assinatura Authenticode fornecida pelo usuário.

`cargo test` contém testes HTTP de refresh/recusa/limite, cancelamento de playback e watchdog; teste DPAPI só executa no Windows. GUHFLIX_TEST_API só existe em build debug e aceita apenas HTTP loopback com porta, para fixtures. A build release ignora esse ambiente e usa API de produção fixa. Não publicar build debug.

Leia o relatório da entrega para distinguir testes executados de validação ainda pendente no Windows/TV e em streams reais.
