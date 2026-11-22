# GFLIX — perfis animados no aplicativo existente, 1.4.0

**APK instalável da mesma edição Gfilmes Local.** Instale por cima da 1.0–1.3, sem desinstalar/limpar dados. Abra **Gfilmes Local**: selecione a pessoa em “Quem está assistindo?” e entre na home real. Pacote `com.gfilmes.tv.local`, versão 1.4.0, código 1004000, Android 6.0+, chave de assinatura original e APK v2 verificados. Não é site ou aplicativo de demonstração.

- 12 personagens GIF originais do ZIP incorporados, com loops, foco luminoso/3D, transição e pausa fora do seletor. Todos os 12 podem ser escolhidos ao criar/editar; até seis perfis por instalação.
- CRUD por controle remoto, Trocar perfil em Minha TV, histórico/progresso/favoritos/preferências por pessoa. Os IDs e dados dos perfis antigos são preservados. Perfis comuns sem senha.
- Infantil opcional: definir PIN do responsável ao configurar o primeiro infantil. No adulto, abrir detalhes e usar **Disponibilizar no infantil**. Somente aprovados aparecem; aprovação de série cobre seus episódios. TV ao vivo é bloqueada nesse modo, por não ter classificação confiável. Para sair/alterar restrições é preciso PIN, inclusive após reiniciar. O PIN não é login da criança nem senha de conta.
- Sem novos serviços, Render, autenticação de conta ou pedidos de animação a servidor. Internet continua necessária para as fontes de vídeo e catálogo. Não há sincronização de perfis entre TVs. As variantes com API/licenças de clientes não foram substituídas.

**49 testes JVM e 14 Chromium passaram.** Incluem mudança real de quadros dos 12 GIFs no navegador, pausa, DPAD/OK/Voltar, persistência, preferências/progresso usando os accessors nativos com SharedPreferences em memória, controle infantil e 720p/768p/1080p. Não houve teste físico de instalação, reprodução, controle remoto ou FPS/RAM na TCL.

[Manifesto, certificado e SHA-256](APK-MANIFEST.json) · [Resultados](TEST-RESULTS.json) · [Prévia Full HD com nomes fictícios](PREVIA-1920x1080-TESTE.png) · [Quadro A](AVATAR-QUADRO-A-TESTE.png) · [Quadro B](AVATAR-QUADRO-B-TESTE.png)

[Código-fonte compilado](https://github.com/xitios660-ctrl/Baixar-/tree/2d63877) · [GIFs e manifesto](../../tv/app/src/main/assets/gfilmes/avatars) · [Frames PNG originais](../../design/profiles/frames) · [Integração, instalação, testes e limitações](../../docs/local/PERFIS-ANIMADOS-1.4.md). O ZIP original foi preservado. Os dados dos testes não são contas do MongoDB.
