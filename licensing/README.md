# GUHFLIX/Gfilmes | Licença offline de até 30 dias

**Estado desta branch:** implementação parcial, em revisão. **Não há APK novo nem
central de assinaturas pronta para clientes. Não publicar no Render nem fazer OTA.**

## Escopo desta etapa

- \`OfflinePermit.java\`: verificador de licença RSA-SHA256, com chave **pública**,
  preso à instalação UUID da TV e ao vencimento, sem guardar senha no APK.
- \`LicenseIssuer.java\`: núcleo emissor de licenças para um **servidor do
  administrador**, com chave **privada**. Não faz autenticação de usuários nem
  acessa o banco sozinho. Nunca incluir esse código ou chave nos APKs dos clientes.
- \`OfflinePermitSmoke.java\`: testes de assinatura, edição do token, outros
  dispositivos, expiração, relógio alterado e limites de 30 dias.

### Contrato assinado

\`GFL1.<base64url(payload UTF-8)>.<base64url(SHA256withRSA signature)>\`

O segundo campo contém:
\`1|userObjectId|installationUuid|issuedAtSec|notBeforeSec|expiresAtSec|serialUuid\`

A assinatura cobre os bytes ASCII de \`GFL1.<base64url(payload)>\`, sem pontos
adicionais. \`expiresAtSec = min(vencimento pago, emissão + 30 dias)\`.
A licença não concede pagamento novo nem estende assinatura vencida.

A TV deve usar o **certificado público da central previamente confiável**,
vinculado de forma segura durante a implantação ou por pareamento verificado.
Não aceitar uma chave enviada arbitrariamente pelo mesmo servidor no ato
da validação da licença.

## O que precisa ser construído para um APK instalável

1. **Central na TV do administrador:** Android app separado (package diferente)
   que gera RSA >=2048 no Android Keystore e mantém a chave privada somente
   lá; interface de operador autenticado; banco persistente e sincronização
   autorizada dos cadastros e vencimentos existentes; emissão e renovação apenas
   após checagem de pagamento.
2. **Acesso externo seguro:** clientes em outras casas precisam alcançar a
   central por um endereço HTTPS protegido (roteamento público ou túnel com
   autenticação). GitHub Pages sozinho não processa logins. Sem conexão à
   central no dia da ativação ou renovação, estas ações aguardam.
3. **APK do cliente:** depois de login online, receber e validar a licença
   assinada; salvar token de forma local protegida; verificar expiração e
   possíveis voltas do relógio a cada abertura. Falhas de rede temporárias
   só podem liberar navegação e reprodução se já existir token íntegro
   e válido para essa instalação.
4. **Catálogo e player fora da API:** alterar \`CinemaActivity.kt\`,
   \`CloudRemote.kt\`, \`Gate.java\` e \`CloudHttp.kt\` para que navegação,
   detalhes e reprodução autorizada consultem diretamente os catálogos/fontes
   permitidos, com cache local e mensagens de erro. Sem isso, o app continuará
   consultando a API MARY mesmo com licença válida.
5. **Vencimento e revogação:** bloquear ao passar do vencimento; não criar
   tolerância que prolongue uma assinatura vencida. **Bloqueio imediato e
   limites simultâneos não podem ser aplicados com precisão quando todos
   os clientes estão offline em relação à central.** Decidir essa política
   antes da liberação.
6. **Segurança e teste:** não confiar em relógio isoladamente; armazenar
   marca de maior hora observada; detectar rollback e exigir revalidação.
   O relógio e o armazenamento de uma TV com root não são à prova de
   adulteração. Testar TVs com data errada, reinício, desligamento da
   central, expiração, conta bloqueada, reinstalação e vídeo indisponível.
7. **Distribuição:** compilar e assinar APKs com os certificados originais.
   Validar instalação por cima das versões atuais, sem apagar dados. Só
   após isso publicar \`release-connect.json\` e links OTA. Nunca renomear
   APK antigo para versão nova.

## Invariantes de segurança

- Chave privada, JWT, senhas e URI do MongoDB **nunca** no APK nem no GitHub.
- Token é vinculado à instalação; roubar um token não libera outra TV.
- Emissão é sempre posterior a autenticação e confirmação de assinatura ativa.
- Não confiar em qualquer licença sem verificação criptográfica e data válida.
- O modo offline não substitui internet para streaming; só evita a necessidade
  de falar com a **API própria** a cada abertura.
- Não criar administrador ou banco novo no lugar do MongoDB existente sem
  migração auditável e validação dos dados.
- As chaves de teste geradas nos testes são transitórias, nunca produção.

## Testar o protocolo sem SDK Android

\`\`\`sh
mkdir -p /tmp/gfl-license-tests
javac -encoding UTF-8 -d /tmp/gfl-license-tests \
  tv/app/src/main/java/com/guhflix/shell/OfflinePermit.java \
  tv/app/src/test/java/com/guhflix/shell/OfflinePermitSmoke.java \
  licensing/station-core/src/com/guhflix/station/LicenseIssuer.java
java -cp /tmp/gfl-license-tests com.guhflix.shell.OfflinePermitSmoke
\`\`\`

**Não confundir:** este teste prova exclusivamente o formato criptográfico
da licença. Não prova que o login, catálogo, TV ao vivo, banco ou instalação
Android estejam prontos.
