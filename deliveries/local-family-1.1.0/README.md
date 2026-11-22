# Gfilmes Local 1.1.0 — preparação de acesso por TV

**Este APK mostra o código da TV. Ele não contém autorizações de clientes e não libera o catálogo antes de receber uma versão personalizada.**

Instale por cima do **Gfilmes Local 1.0.0**, sem desinstalar. Abra o ícone **Gfilmes Local**, envie ao titular o código e identifique o acesso existente. Depois da verificação do cliente no MongoDB, o operador poderá emitir e compilar o APK personalizado, assinado e vinculado àquela TV, sem Render ou API de contas. Não é necessário configurar o Gfilmes Servidor.

Após receber o APK autorizado, haverá seleção/criação de até seis perfis, avatar, PIN opcional, troca/edição e favoritos/histórico/progresso separados. Os PINs são definidos na TV e não são senhas extraídas do MongoDB. Nenhuma lista de clientes ou hash de senha foi incorporado. Vídeos online continuam exigindo internet.

Pacote `com.gfilmes.tv.local`, versão 1.1.0, código 1001000, Android 6.0+. Assinatura original e APK v2 conferidos. SHA-256 e certificado no [manifesto](APK-MANIFEST.json). Fonte `e408cf1`.

46 testes Android, 7 Chromium e 6 verificações de emissão passaram. Os testes usam fixtures, não representam reprodução audiovisual no aparelho. Instalação e validação na TCL e o primeiro APK autorizado real ainda estão pendentes.

A edição antiga 1.0.0 permanece sem controle e não pode ser bloqueada remotamente. MongoDB, pagamentos, clientes, produção e downloads/OTA atuais não foram alterados.

[Fluxo completo e limitações](../../docs/local/PERFIS-E-ACESSOS.md) · [Testes](../../docs/local/TESTES.md)
