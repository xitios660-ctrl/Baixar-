# Testes e limites da homologação

Data: 20/11/2026. Branch: feat/offline-30d-authority. Base de produção: c924c4425c13667533d960079ecc161aa97aac51. Nenhuma implantação, assinatura real, cobrança, pagamento ou conta de produção foi modificada.

## Executados

- Build TypeScript da API: passou.
- Suíte API original + licenças: 20 testes passaram. Alterações finais de modo activation-only e loopback: 4 testes direcionados passaram. Reexecução da ativação incluindo quatro dispositivos concorrentes: passou (três 200, um 409).
- Protocolo: emissão limitada a cinco dias quando restam cinco; limite de 30 dias quando há saldo maior; assinatura, raiz/delegação, prova de posse, bloqueado, vencido, dispositivo retirado ainda ocupando vaga, renovação após mudança administrativa confirmada e preservação do hash/pagamento: passaram em banco MongoDB descartável.
- Cliente Android: 31 testes direcionados passaram (licença, assinatura adulterada, outra chave/dispositivo, relógio monotônico, reinício simulado, retrocesso, expiração, filtros, paginação, política de startup/OTA/retry e parsers nativos).
- Servidor Android: 6 testes passaram (núcleo compartilhado, hash Argon2id gerado pelo Node e validado pelo Bouncy Castle, senha errada, parâmetros de custo abusivos, backup de 300 KB, senha errada e adulteração AES-GCM).
- Suíte Android completa: 74 casos executados, 70 passaram, 3 falharam por ConnectException ao acessar fontes externas em EpgTest e PosterTest, 1 ignorado. Essas falhas NÃO foram convertidas em sucesso nem os testes alterados para ocultá-las. Os parsers/guia originais que fazem essas chamadas não foram modificados nesta tarefa.
- Migração: réplica MongoDB descartável, 12 coleções, exportação real pela ferramenta, assinatura pela chave original, decifragem apenas pelo destino, hash original, bloqueio, vencimento e pagamento preservados, coleções/documentos originais iguais após exportação. Passou. Não acessou Atlas real.
- Túnel gratuito: binário oficial cloudflared 2026.11.1 teve SHA256 confirmado contra metadado oficial do release. Duas tentativas com serviço descartável somente de health não confirmaram resposta HTTPS pública na janela de teste. Logo, não há túnel público operacional entregue nem prova de acesso por outra rede. Nenhum dado de cliente foi exposto. Os processos temporários foram encerrados.
- APKs: build release, assinatura v2, certificado original, pacote/versão, integridade ZIP e igualdade dos assets cinematográficos com o código-fonte são verificados por tools/licensing/verify-apks.py. Consulte APK-MANIFEST.json e SHA256SUMS.txt dos arquivos efetivamente entregues.

## Testes físicos ainda necessários — impedem liberação aos clientes

| Caso | Situação |
|---|---|
| Instalar cliente por cima do Connect existente e preservar preferências | Pendente: nenhuma TV/ADB ou emulador conectado |
| Instalar servidor na TCL e abrir importador SAF | Pendente: disponibilidade do seletor de documentos varia por firmware |
| Login/ativação com conta real autorizada | Pendente: preparar servidor físico, delegação pública e snapshot final |
| Navegar e assistir com servidor desligado | Fluxo implementado; teste real pendente |
| Reiniciar aplicativo/TV e manter licença, favoritos e progresso | Algoritmo de checkpoint testado; Keystore/firmware real pendentes |
| Expirar e renovar na TV sem renovar sem pagamento | Backend testado em fixture; fluxo físico pendente |
| Alterar data e copiar arquivos entre duas TVs | Assinatura/binding/clock simulados passaram; hardware real pendente |
| Filmes, episódios, canais e codecs em fontes autorizadas | Parsers e filtros testados; reprodução audiovisual real não confirmada |
| Controle remoto, teclado virtual/físico e rolagem longa | Interface existente preservada; navegação e desempenho físico pendentes |
| Cliente de outra casa acessar servidor Android por HTTPS | Não comprovado; exige certificado confiável e NAT/CGNAT resolvido |
| Queda de internet, suspensão, reinício do serviço e recuperação de backup no Android | Cifragem e recuperação lógica testadas; serviço/SQLite/Keystore físicos pendentes |
| OTA para edição offline | Não publicado; apenas atualização manual homologada é preparada |

A simulação de relógio, verificação de assinatura e APK assinado não equivalem a comprovar instalação ou reprodução numa TCL. A TV servidor não foi validada como servidor público nem como serviço contínuo. Não é uma migração de produção concluída.

## Reproduzir sem produção

```sh
npm run build --prefix api
api/node_modules/.bin/tsx --test --test-concurrency=1 api/test/*.test.ts
JAVA_HOME=/seu/jdk node tools/licensing/test-migration.mjs
# Android com SDK/JDK e chave original configurados privadamente:
tv/gradlew -p tv :app:testReleaseUnitTest --tests 'com.gfilmes.license.*' --tests 'com.guhflix.shell.*' --tests 'br.com.saimo.tv.VodFormatoTest' --tests 'br.com.saimo.tv.PlaybackUrlTest' --tests 'br.com.saimo.tv.RemoteTest' :server:testReleaseUnitTest :app:assembleRelease :server:assembleRelease -PgfilmesOffline=true
python tools/licensing/verify-apks.py
```

A suíte completa também necessita de conectividade às fontes de guia/capas. As chaves de testes criptográficos são efêmeras; a chave privada original nunca é impressa. O teste de migração lê somente sua réplica descartável local, mesmo se houver MONGODB_URI no ambiente do operador.

## Correção 1.0.1: ausência de seletor na TCL

O titular abriu o Servidor 1.0.0 na TV e enviou foto da mensagem “Nenhum aplicativo pode executar esta ação”. Isso confirma a limitação física do fluxo SAF nessa instalação. A versão 1.0.1 oferece configuração por navegador na LAN e exibição direta da chave pública, sem usar o seletor.

Resultados novos: 10 testes do servidor passaram. Quatro verificam o HTTP real em sockets locais: chave exata no download, token errado, rota proibida, arquivo acima do limite, origem externa, erro de validação, backup indisponível antes de preparado, headers duplicados e chunked recusados. A proteção criptográfica continua no importador raiz/cifra já existente; os testes do transporte usam callbacks de fixture, sem clientes reais.

Teste em Chromium com viewport móvel de 430×932: download de gfilmes-server-public.txt, seleção/envio de arquivo pelo formulário, resposta de sucesso e nenhum erro JavaScript. Não é teste na TCL/Safari nem prova de outra rede. Assinatura e decifragem de pacote HTTPS foram testadas com conteúdo sintético, sem importar ou criar certificado público.

Serviço novo: janela foreground iniciada manualmente, 15 minutos, bind apenas IPv4 privado, URL aleatória, sem rotas de login/administração/ativação. Senhas de backup são digitadas na TV; arquivos de configuração TLS via LAN precisam ser cifrados/assinados previamente. A atualização mantém pacote, certificado e aliases Keystore. A instalação por cima e a conexão LAN no equipamento do titular aguardam validação real.

## Adaptador de exportação do conector

O teste `tools/licensing/test-connector-snapshot.mjs` passou com dados sintéticos locais: assinatura original validada, decifragem pelo destinatário, IDs/hash/status/data preservados e rejeição de origem instável/hash incompatível. Não acessa produção nem provisiona clientes.

A modalidade por conector verifica duas leituras completas iguais antes de cifrar. Ela não expõe opções de transação/readConcern no conector e não pode ser descrita como snapshot transacional. Metadados e relatórios de qualquer cópia real ficam privados, junto ao arquivo cifrado; não acompanham os arquivos públicos desta branch. A instalação/importação no equipamento do titular continua dependendo da confirmação na TV.
