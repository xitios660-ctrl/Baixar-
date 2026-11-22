# Gfilmes Local 1.2.0

APK genérico: abre o catálogo sem código de TV, assinatura de autorização, API de contas, MongoDB ou Gfilmes Servidor. Na primeira abertura, escolher **Assistir agora**; depois, selecionar o perfil. Também é possível criar até seis perfis com avatar e PIN opcional, com favoritos/histórico/progresso separados.

Instalar por cima do **Gfilmes Local** 1.0.0/1.1.0, sem desinstalar. Pacote `com.gfilmes.tv.local`, versão 1.2.0, código 1002000, Android 6.0+. Assinatura Android original preservada. É um pacote diferente de Gfilmes Connect e do Servidor; abrir especificamente **Gfilmes Local**.

Não é um APK de preparação e não exige outro APK personalizado. Internet e fontes de conteúdo disponíveis continuam necessárias para assistir. PIN é apenas proteção local de perfil; esta edição não valida pagamentos/assinaturas. Nenhuma lista de clientes ou credencial do MongoDB foi embarcada ou alterada.

Conferido código Saimo main `e3f99cb` (versão marcada 3.3.5). Adaptada correção de capas absolutas; não foi substituída toda a base nem importada Conta Saimo. Fontes atuais de catálogo responderam HTTP 200 nos quatro arquivos consultados (checagem de prefixo, não reprodução).

42 testes Android e 7 de interface passaram. Catálogo dos testes de interface e bridge são sintéticos. Instalação e reprodução audiovisual na TCL ainda dependem de teste físico. [Manifesto, assinatura e SHA-256](APK-MANIFEST.json) · [Testes](TEST-RESULTS.json).

Fonte `493cc04`. [Instalação e revisão do Saimo](../../docs/local/ABRIR-CATALOGO-1.2.md). Nenhuma alteração no site/OTA de produção ou nas contas existentes.
