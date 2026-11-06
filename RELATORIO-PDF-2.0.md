# GUHFLIX · execução do PDF — 06/11/2026

## Situação da entrega

Ícone melhorado incorporado ao APK 1.0.1; publicado no site próprio do Render e baixado de volta. O SHA-256 do download coincidiu com o arquivo assinado. Download: https://guhflix-tv-download.onrender.com/GUHFLIX-TV.apk.

O PDF foi lido integralmente. Foi implementada uma plataforma própria de API/admin e uma versão 2.0.0 candidata do APK. O aceite integral do PDF AINDA NÃO está concluído: falta conectar o backend ao Atlas de produção, configurar o administrador e validar o fluxo completo de reprodução no APK/TCL. O botão público continua entregando a versão 1.0.1, para não instalar uma versão dependente de um backend ainda pendente.

## API implementada

Node/TypeScript, Fastify, Zod, MongoDB, Argon2id, JWT curto, refresh rotativo com hashes no banco, RBAC, TOTP administrativo, limitação de tentativas, CORS de origem definida, health check e logs sem senhas/tokens. Inclui todos os endpoints mínimos do PDF, configuração inicial de administrador por token privado de uso único e gerenciamento de sessões pela própria conta.

Plano de R$ 30,00 / 30 dias exatos. Registro manual de pagamento confirmado, método, operador e período. Renovação atômica e idempotente, preservando bloqueio manual. Receita calculada somente sobre pagamentos confirmados; gráfico de até 90 dias com movimento. Nenhuma cobrança automática, webhook ou recebimento fictício ativado.

Vários dispositivos cadastrados e máximo de 3 reproduções concorrentes por conta. Controle com transação no documento do usuário; lease de 120s, stop, heartbeat, timeout e revogação. A quarta tentativa é recusada sem expulsar sessões anteriores. Relógio do servidor decide expiração.

Coleções e índices: users, subscriptions, payments, sessions, devices, idempotency_keys e audit_logs. Namespace de banco exclusivo GUHFLIX; na conta Guh Flix o usuário selecionou Project 0 / Cluster0. Exportação criptografada AES-GCM com snapshot consistente implementada e verificada localmente; agendamento, armazenamento externo e ensaio operacional de restauração ainda pendentes.

## Painel implementado e publicado

https://guhflix-admin.onrender.com

Página própria preta/vermelha, MFA, cards e gráficos com dados da API, busca/filtros/paginação, criar/editar cliente, redefinir senha, confirmar pagamento e +30 DIAS, bloquear/desbloquear, gerenciar sessões e auditoria. Cookie HttpOnly/Secure em proxy de mesma origem, compatível com a política de cookies do Safari. A publicação da interface e seu health foram verificados; login de produção ainda não está disponível sem a API/banco configurados.

Fonte do backend/painel: branch guhflix-platform em https://github.com/xitios660-ctrl/Baixar-. O site de download está na branch main do mesmo repositório. TV DO GUH AI, RR Motos e GANOH não foram alterados.

## APK 2.0.0 candidata

Login nativo, sessão com refresh criptografado por AES-GCM/AndroidKeyStore, validação na abertura e retomada, botão Conta no menu, logout, bloqueio por vencimento/manual, contato WhatsApp do PDF e revalidação da renovação sem pedir senha novamente. Backup dos dados do app desativado.

O player original só é inicializado após a autorização da API. Heartbeat de 35s e watchdog com relógio monotônico antes do fim do lease; perda de internet não libera uso indefinido. Tela de limite com gestão/revogação de sessões. O wrapper preserva o player, catálogo e autorização do provedor; não é DRM nem substitui as permissões dos streams.

“On Demand” foi substituído nos recursos/código de interface por “Séries/Filmes”. A espera do preview original do hero foi alterada de 5s para 1,5s; interrupção por troca de destaque e áudio silencioso da base foram preservados. Isso depende de URL de trailer autorizada no catálogo; não foi fabricada prévia a partir de episódio completo. Busca, temporadas, episódios, favoritos e retomada continuam na base nativa. Não foi implementado um novo algoritmo de recomendações.

## Testes executados

| Área | Evidência / resultado |
|---|---|
| APK 1.0.1 | Compilação, assinatura e estrutura verificadas; download do Render com hash idêntico |
| APK 2.0.0 | Compilação, assinatura e inspeção de estrutura; URL de produção no DEX, sem URL local de teste |
| API — banco real descartável | MongoDB replica set local, sem mocks de persistência |
| Criação inicial admin | Token incorreto recusado, criação válida aceita, segunda criação recusada |
| Autenticação/RBAC | Senha incorreta, usuário objeto/injeção e acesso administrativo indevido recusados; revogação de sessão de outra conta recusada |
| Sessão | Refresh entre instâncias da API, rotação, detecção de reutilização, revogação e logout verificados |
| Vencimento/renovação | Conta vencida recusada; reativação com mesma sessão; 30 dias e 3000 centavos; bloqueio manual preservado |
| Cliques duplicados | 6 renovações concorrentes com mesma chave geraram um único pagamento e um único período |
| Telas | 4 starts concorrentes: 3 aceitos / 1 recusado; stop e timeout liberam vaga; heartbeat verificado |
| Receita/auditoria | Totais e eventos conferidos contra os registros do teste |
| Backup | Snapshot criptografado, tag autenticada na descriptografia, pagamentos e auditoria presentes |
| Painel Chromium | MFA, criação de cliente, confirmação/renovação, modal de telas, bloquear/desbloquear e refresh após reload; sem erros JavaScript. Primeiro administrador também testado: fragmento privado removido da URL, código MFA conferido, criação única e login efetivo |
| Android 9 emulado | Instalação/abertura, login na API local, sessão após force-stop/reabertura, erro sem rede e tela de vencimento executados. Após renovação simulada no banco descartável, a mesma sessão voltou ao catálogo. Quarta reprodução exibiu limite; init do player com título sintético indisponível apresentou erro controlado, sem validar vídeo. Emulador sem KVM apresentou ANRs da System UI; não serve como medição de desempenho |
| Reprodução física TCL / 4K / 60 FPS | Não testadas |
| API+Atlas de produção / fluxo integral online | Pendente |

Foi encontrado e corrigido um crash no botão Voltar durante autorização/limite: o player original recebia teclas antes de inicializar os controles. As rotas dispatchKeyEvent, onKeyDown e onNewIntent agora delegam ao ciclo básico antes da inicialização; o teste de regressão foi repetido com log de AndroidRuntime limpo.

A build local de teste usa somente uma API descartável em 127.0.0.1 por adb reverse (também aceita 10.0.2.2 na ferramenta de build) e NÃO foi publicada. O APK candidato de produção usa HTTPS com validação padrão, sem bypass de certificado. As modificações no sistema descartável do emulador não fazem parte do APK.

## Estado atual de produção

A conta selecionada agora é Guh Flix, organização Marley’s Org - 2026-11-06, Project 0, Cluster0. O cluster e um usuário automático já existem. A criação de um novo usuário limitado pela ferramenta foi recusada por falta de confirmação; não foi contornada. A conexão fornecida depois pelo usuário ainda contém <db_password>, sem senha atual configurada no servidor.

A API foi implantada em https://guhflix-api.onrender.com (Render srv-dbmk1g0m7kps73bj9v00, plano gratuito, Virginia). Deploy live verificado. /health retornou HTTP 503 com state=configuration_required, de forma prevista e sem liberar acesso. Falta preencher MONGODB_URI privadamente no Environment do Render e liberar os IPs reais de saída desse serviço em Network Access do Atlas. Os segredos de autenticação da API foram gerados e configurados privadamente, fora do fonte.

O novo servidor tem inicialização protegida: atende 503 enquanto falta configuração/conexão e só encaminha requisições às rotas autenticadas depois de conectar e criar índices. Testes locais de prontidão HTTP e a suíte completa do MongoDB passaram (2 testes de integração). O namespace GUHFLIX e suas coleções serão inicializados após essa conexão.

Depois de /health retornar 200: configurar o primeiro administrador com o link privado de setup, executar o aceite online e validar reprodução no hardware antes de substituir o APK público. A versão 1.0.1 permanece no botão de download.

## Limitações restantes

Conexão Atlas da API e configuração do administrador pendentes, instalação/reinício da TCL e reprodução real não validados, catálogo/streams dependem das fontes autorizadas e codecs, previews reais ainda não exercitados, desempenho não medido, backups automáticos e alertas externos não ativados. Plano Render gratuito sujeito a suspensão/cold start. Não há offline de conteúdo. Código Kotlin original não recuperado: o fonte Android inclui Java novo, Smali e recursos reconstruíveis.
