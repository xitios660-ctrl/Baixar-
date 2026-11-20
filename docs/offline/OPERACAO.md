# Gfilmes offline: homologação, configuração e limites

Esta branch não troca produção, MongoDB, site de downloads ou OTA. Os dois APKs são uma edição de homologação. Não distribuir aos clientes antes da lista de testes físicos em TESTES.md e da preparação da autoridade. Nenhum usuário foi criado em produção.

## O que é local

O cliente usa o catálogo e os resolvedores nativos existentes, busca combinada, categorias, temporadas, episódios e player. Os índices ficam em cache privado (atualização em 6 horas, fallback para cache antigo, limite total 96 MiB), páginas de 60 títulos e imagens sob demanda da interface existente. Favoritos, histórico e progresso continuam no armazenamento do aplicativo. Vídeos, canais e capas continuam exigindo internet e disponibilidade das respectivas fontes autorizadas. Não há download offline de filmes. As fontes já usadas no projeto foram preservadas; sua autorização de distribuição deve continuar sendo mantida pelo titular.

Uma licença válida dispensa o servidor de ativação para abrir, navegar e iniciar reprodução. Chamadas locais substituem acesso e heartbeat; o poll da nuvem fica desativado. A API MARY não é consultada por título. Metadados extras pela API e controle remoto por pareamento na nuvem não estão disponíveis nesta edição independente; não se inventa um código de pareamento. OTA é opcional e continua sem publicação da edição nova no manifesto de produção.

## Licença e ameaças

RSA 2048 / SHA-256 (RS256), alternativa compatível com Android Keystore. A raiz pública é o certificado ORIGINAL do aplicativo Connect. A chave privada original fica exclusivamente com o operador; o servidor cria outra chave não exportável no Android Keystore. O titular assina uma delegação pública para essa chave, limitada a 30 dias por licença e com validade própria de um ano. Cada licença inclui cliente, chave do dispositivo, emissão, vencimento real da assinatura, versão e identificador. Prazo = menor entre 30 dias, vencimento da assinatura e vencimento da delegação. A raiz não emite automaticamente licenças a clientes.

A TV cliente prova posse de sua chave com nonce descartável. A licença e o checkpoint do relógio ficam cifrados com AES-GCM e chave Keystore. Copiar preferências para outra TV não copia as chaves. Não desinstale o aplicativo para atualizar: a desinstalação apaga chaves e autorização. Restaurar backup do Android não transfere uma licença.

Durante a mesma inicialização, o relógio monotônico determina o prazo. Alteração de relógio superior a dois minutos ou retrocesso detectado depois de reiniciar bloqueia o uso até nova ativação. Depois de desligar totalmente, parte da contagem depende do RTC do aparelho. SEM contato com uma fonte de tempo confiável, hardware inviolável e atestação, não é possível garantir resistência absoluta a root, snapshots ou congelamento coordenado do relógio. Esta implementação não promete essa garantia. A tolerância de dois minutos não transforma a licença em ilimitada; o tempo monotônico continua contando.

São três dispositivos com licenças ainda válidas, não três streams simultâneos verificáveis offline. Remover/bloquear impede novas emissões; licenças já emitidas continuam até vencer e ocupam vaga até lá. Revogação imediata e auditoria de reprodução offline não são garantidas. Não autoriza contas vencidas ou bloqueadas na ativação.

## Preparar o servidor Android

1. Instale Gfilmes Servidor na TV do titular. Na versão 1.0.1, selecione a opção de configurar pelo celular: não depende de seletor de documentos SAF, ausente em algumas versões da TCL. A exportação pela opção antiga pode mostrar “Nenhum aplicativo pode executar esta ação”. Atualize por cima, sem desinstalar nem limpar os dados. Pacote diferente do cliente. Abra e exporte apenas **gfilmes-server-public.txt** pelos controles normais de arquivos do Android. Esse arquivo não contém segredo.
2. Em computador seguro com a chave ORIGINAL já existente, execute na raiz do projeto:

```sh
java tools/licensing/AuthorityTool.java approve tv /CAMINHO/server-public.txt /CAMINHO/authority.jws
```

3. Importe authority.jws no servidor. A chave privada não é transferida para o cliente nem para a TV servidor. A delegação pública pode ser guardada com os documentos operacionais.
4. Exporte um snapshot com a ferramenta abaixo e importe o arquivo cifrado/assinado no servidor. Ele reutiliza os administradores e hashes existentes; não cria outra senha/admin.
5. Entre no painel local com uma conta administrativa importada. Configure um certificado HTTPS PKCS12 com chave e cadeia confiável para o nome público usado. A senha e o PKCS12 ficam cifrados localmente, não no APK. Certificado autoassinado não é aceito pelo cliente por padrão. Não desabilite validação TLS.
6. Mantenha data/hora automáticas corretas no servidor; a autoridade administrativa é responsável pelo UTC de emissão. Inicie manualmente a janela de ativação. Porta HTTPS **17862**. Verifique /health pelo endereço correto. Abrir a porta da TV NÃO resolve CGNAT e não cria endereço público estável. Sem caminho HTTPS público comprovado, a TV ainda não está pronta para ativar clientes de outras casas.

O Android pode suspender o processo e encerrar serviços dataSync (inclusive limite de tempo no Android 15). O serviço é foreground com notificação, iniciado pelo usuário; não promete inicialização silenciosa após boot nem execução 24h. Depois de reiniciar, abra o servidor e inicie novamente. Instale somente na TV do titular e limite acesso físico. O painel administrativo não possui endpoints públicos de alteração; login/ativação são as rotas expostas, com HTTPS, limite global de tentativas e tamanho de requisição limitado.

## Migração sem tocar no original

A ferramenta usa UM cliente MongoDB e transação read-only com readConcern snapshot. O limite de seleção inicial de servidor é 15 s para falhar de forma visível numa operação manual; leituras sequenciais reutilizam o pool padrão, sem criar um cliente por coleção. Não altera índices, documentos, senhas, datas nem pagamentos. Operador deve monitorar duração e número de conexões no Atlas antes de ampliar a exportação.

```sh
# Configure MONGODB_URI somente no ambiente PRIVADO do computador; não cole no chat ou GitHub.
JAVA_HOME=/CAMINHO/JDK node tools/licensing/export-atlas.mjs /CAMINHO/server-public.txt /FORA/DO/REPOSITORIO/migracao.json
```

Requer a chave original disponível pelas propriedades privadas existentes do projeto e as dependências de api instaladas. O snapshot guarda todas as coleções e EJSON canônico, contagens e data. O programa aborta a transação sem nenhuma escrita, cifra AES-GCM, encapsula a chave com RSA-OAEP SHA-1 (compatibilidade Keystore Android; assinatura separada SHA-256) e assina o pacote cifrado. Só a TV destino pode decifrá-lo; só pacote aprovado pela raiz pode ser importado. Uma conta read-only do Atlas é recomendada para a exportação. A ferramenta não cria essa conta nem modifica permissões.

O servidor recusa importação inicial sobre banco já inicializado. Banco local cifra cada registro e preserva o snapshot completo original, inclusive pagamentos, assinaturas, sessões e auditoria. O painel opera em uma cópia de trabalho. Renovar nela **não atualiza o Atlas automaticamente**. As confirmações administrativas ficam em registros locais separados; não representam cobrança efetivamente executada. Nenhum preço/plano ou pagamento da origem é alterado.

**Corte administrativo obrigatório:** mantenha MARY atual durante homologação. Antes de adotar a cópia local como autoridade, faça snapshot final consistente e concilie alterações ocorridas depois da exportação. Não administrar assinaturas simultaneamente em cópias divergentes. A importação inicial não sobrescreve um servidor já inicializado. Para repetir a preparação em TV de homologação sem licenças reais emitidas, preserve o backup, limpe somente os dados desse servidor de teste, gere nova chave, obtenha nova delegação e importe o snapshot final. Nunca zere um servidor que já emitiu licenças reais sem recuperar e conciliar o registro de vagas. Retorno antes do corte: manter aplicativos/serviço 2.x, sem mudança no Atlas. Para uma TV de teste que já recebeu cliente 3.0.0, o pacote opcional de recuperação 3.0.1 retorna ao modo online da MARY, com mesma assinatura/pacote e sem downgrade ou limpeza. Ele não é uma nova versão offline: não distribuir nem publicar no OTA. Depois de usá-lo, uma próxima edição offline precisará ter código maior que 3000001. Depois de renovações locais reais: exportar backup e conciliar manualmente alterações aprovadas antes de voltar, sem sobrescrever dados cegamente. Não foi executado corte nem sincronização bidirecional nesta entrega.

## Backup e recuperação

No painel autenticado, escolha Backup criptografado e uma senha forte com pelo menos 14 caracteres. PBKDF2-HMAC-SHA256, 310 mil iterações, salt aleatório e AES-256-GCM. Guarde arquivo e senha separadamente. O backup não exporta chaves Keystore. Ele contém os dados importados; não publicar em hospedador nem GitHub.

A recuperação exige senha e confirmação, substitui apenas banco LOCAL e exige novo login. Em outra TV, gere nova chave e peça nova delegação pública. Licenças existentes no cliente podem continuar até vencer; o registro de vagas é preservado no backup. Nunca reemita autorizações antes de recuperar esse registro.

## Alternativa computador / mini PC

É a opção preparada para maior disponibilidade; ainda exige internet e manutenção. Node 22+, dependências existentes e MongoDB original. Gere uma chave exclusiva fora do repositório:

```sh
node tools/licensing/create-desktop-issuer.mjs /DIRETORIO/PRIVADO
java tools/licensing/AuthorityTool.java approve tv /DIRETORIO/PRIVADO/server-public.txt /DIRETORIO/PRIVADO/authority.jws
npm ci --prefix api --include=dev
npm run build --prefix api
```

Configure privadamente MONGODB_URI, JWT_SECRET e ADMIN_ORIGIN existentes, GFILMES_LICENSE_KEY_FILE=/DIRETORIO/PRIVADO/issuer.pem e GFILMES_LICENSE_GRANT_FILE=/DIRETORIO/PRIVADO/authority.jws. Não copie segredos para APK. Use **GFILMES_ACTIVATION_ONLY=1** para bloquear catálogo, relay e administração pública nesta instância. Inicie `node api/dist/server.js`. A autenticação e as datas são lidas do mesmo banco, sem migração de senhas. Emissão usa transação MongoDB e mantém três vagas mesmo com ativações concorrentes.

Uma única opção gratuita preparada: **Cloudflare Quick Tunnel** no computador. Instale cloudflared apenas pela distribuição oficial. Execute `GFILMES_ACTIVATION_ONLY=1 node tools/licensing/quick-tunnel.mjs`. A ferramenta verifica /api/health.activationOnly antes de expor o serviço. Nunca expõe relay de vídeo. Endereço trycloudflare.com temporário muda ao reiniciar e não tem garantia de disponibilidade; não equivale a um domínio estável profissional. Não foi criado serviço pago, conta Cloudflare ou túnel público nesta entrega. Referência oficial: https://developers.cloudflare.com/cloudflare-one/networks/connectors/cloudflare-tunnel/do-more-with-tunnels/trycloudflare/ .

O computador continua precisando do Atlas durante ativação/renovação. O cliente licenciado não precisa de ambos durante sua licença. Para disponibilidade pública permanente sem endereço mutável será necessário um caminho HTTPS estável administrado pelo titular; a entrega não inventa esse endereço.

## Instalar e usar o cliente

1. Faça backup da instalação/dados atuais antes da homologação. A compatibilidade foi verificada contra o pacote Connect 2.8.1 / código 2080001 e seu certificado original, não contra uma instalação física. Os outros pacotes do projeto (com.guhflix.tv e com.guhflix.android) são aplicativos diferentes: este APK não os substitui nem migra automaticamente suas preferências. O cliente 3.0.0 usa o MESMO pacote e assinatura original do Connect. Atualizar por cima preserva dados, mas o Android não permite downgrade simples para 2.x. Não instalar em todos os clientes antes de testar numa TV reservada.
2. Abra o cliente, escolha SERVIDOR DE ATIVAÇÃO e informe somente a origem HTTPS configurada (nome, sem /api ou parâmetros).
3. Entre com usuário/senha existentes. Precisa ter assinatura paga vigente e conta não bloqueada. A autoridade emite licença por até 30 dias, nunca além do vencimento real. MARY atual não emite esse protocolo até ser configurada; não basta instalar e usar o endpoint antigo.
4. Navegue/reproduza com internet para as fontes. Desligue o servidor de homologação e repita abertura/reprodução; depois reinicie a TV e teste novamente. Confira validade e relógio.
5. Renovação: titular verifica pagamento, renova no sistema escolhido como autoridade, cliente conecta e autentica novamente depois do vencimento. Sem confirmação administrativa não há extensão automática.

Não altere a data da TV. Não desinstale para atualizar. Não se promete reprodução de uma fonte indisponível ou de formato incompatível.

## Servidor 1.0.1: configuração sem seletor de arquivos

1. Abra “Ver chave PÚBLICA / configurar pelo celular”. Mantenha TV e celular na mesma rede Wi-Fi. A TV mostra um endereço local temporário e a impressão digital SHA256 da chave pública.
2. Digite o endereço completo no navegador normal do celular, incluindo a barra final. Não exponha essa porta no roteador. A janela dura no máximo 15 minutos e encerra após reinício; novo início gera outro endereço secreto. Isolamento de clientes no roteador/rede de convidados pode impedir o acesso.
3. Compare a impressão digital exibida no celular com a da TV antes de aprovar a chave. Baixe `gfilmes-server-public.txt` e envie somente esse arquivo público ao operador.
4. No mesmo navegador, selecione o arquivo de autorização pública assinada e envie. Depois faça o mesmo com a migração cifrada/assinada já preparada. Volte à TV e escolha atualizar estado. Migração não sobrescreve um banco local já inicializado.
5. A página usa HTTP somente dentro da LAN para a chave pública e pacotes previamente cifrados/assinados. Não possui login administrativo ou campos de senha. O servidor recusa arquivos sem autorização da raiz ou destinados a outra chave. Isso NÃO substitui o HTTPS público necessário à ativação dos clientes.
6. O backup criado no painel pode ser baixado cifrado nessa janela local; a senha é digitada somente na TV. Para recuperar, prepare a recuperação na TV com a senha e autorização administrativa e envie o arquivo cifrado pelo celular. Recuperação é de uso único e fica limitada à janela/sessão administrativa; a senha é apagada da memória ao terminar ou fechar a janela.
7. Para importar o PKCS12 sem o seletor, o operador usa `seal-server-config.mjs`: defina privadamente GFILMES_TLS_P12_FILE e GFILMES_TLS_PASSWORD_FILE (arquivo com senha), execute `JAVA_HOME=/JDK node tools/licensing/seal-server-config.mjs /public-key.txt /FORA/DO/REPOSITORIO/config-cifrada.json` e envie o resultado como “Certificado em pacote cifrado e assinado”. Certificado e senha ficam dentro do pacote cifrado para a TV e aprovado pela chave original; nunca envie o PKCS12 ou a senha em claro pelo portal.
8. Ao terminar, escolha “Encerrar configuração local” na TV. A chave Keystore, o banco e as licenças são preservados pela atualização 1.0.0 → 1.0.1.

A porta de configuração local é 17861 e fica vinculada apenas ao IPv4 privado da TV. Não oferece catálogo, reprodução, login de clientes ou edição de assinaturas. A porta de ativação HTTPS continua 17862, com os requisitos de certificado/rede anteriores. Alcance da LAN na TCL ainda exige validação física; testes em loopback não provam acesso de outra casa.

## Exportação privada por conector, sem URI do banco no ambiente

Quando só houver acesso pelo conector Atlas, use consultas `find` paginadas com ordenação por `_id`, sem modificar dados. Faça duas leituras completas e recuse a preparação se qualquer coleção, contagem ou registro divergir. Preserve a representação EJSON dos identificadores/datas. Isso verifica estabilidade observada, mas **não é um snapshot transacional**; não substitui a conciliação antes do corte administrativo.

O adaptador `tools/licensing/seal-connector-snapshot.mjs` recebe o JSON pelo stdin (nunca como argumento de shell), exige declaração de método não transacional e verificação das 12 coleções, valida IDs, contas existentes, hashes e datas, normaliza EJSON canônico e gera pacote AES-GCM cifrado para a chave da TV, com assinatura da raiz original. Recusa origem instável e saída dentro do repositório público. Nenhum administrador é criado, nenhum hash é substituído e nenhuma chamada de escrita é feita à origem.

O programa deve receber stdin sem eco; não cole dados de clientes em terminal com eco ou em logs. O arquivo resultante e o relatório permanecem em diretório privado, com permissão 0600. Use os controles normais de exportação de arquivos do cliente para entregar ao titular, sem publicar o pacote, mesmo cifrado, em repositório/hospedagem pública. Clientes desktop compatíveis oferecem “Salvar como…” no visualizador de arquivo. Não invente uma URL pública para contornar indisponibilidade desse controle.

Teste isolado: `JAVA_HOME=/JDK node tools/licensing/test-connector-snapshot.mjs`. Verifica assinatura original, decifragem pelo destinatário efêmero, preservação de IDs/hash/bloqueio/vencimento, recusa de leituras divergentes e hash incompatível. Não acessa produção.
