# Recuperação MARY

A API usa o banco existente; não cria administrador, concede períodos ou altera pagamentos. O painel está em `/admin/`, na mesma origem HTTPS da API. Nenhum serviço adicional é necessário. As rotas administrativas continuam exigindo conta administrativa autenticada, e as mutações verificam a origem configurada.

Configurar `ADMIN_ORIGIN=https://gfilmes-mary-api.onrender.com` e `PWA_ORIGIN=https://gfilmes-mary-api.onrender.com`, preservando `MONGODB_URI`, `JWT_SECRET` e demais configurações privadas existentes. `PWA_ORIGIN` também controla o endereço retornado pelo pareamento. Não registrar valores privados no repositório.

Android Connect TV e Android mobile: candidato 2.7.4 / 2070004, mesma identidade de pacote e chave existentes. Endereço de API atualizado para MARY; download OTA permanece no portal existente. A edição legada 2.5.0 não é a destinatária deste lançamento.

OTA: verifica o manifesto HTTPS, versão, pacote, tamanho, SHA256 e assinatura igual à instalação existente. A instalação pede confirmação do Android e pode exigir permissão para instalar aplicativos dessa origem. Não é instalação silenciosa. O usuário pode verificar atualização na tela de acesso sem conseguir entrar na API antiga. Não alterar os manifestos públicos nem publicar APK até validar instalação e reprodução no aparelho.

Validação local: suíte API cobre autenticação, renovação atômica, expiração, bloqueio, revogação, isolamento e limite de três reproduções; fixtures não são contas de clientes em produção. Login e mídia em produção devem ser reportados separadamente dos testes locais. Não afirmar validação física TCL sem aparelho.
