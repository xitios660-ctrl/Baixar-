# Gfilmes Local 1.2.0 — catálogo sem autorização por TV

A versão 1.2.0 atende à correção do titular: um APK genérico, sem código de dispositivo, assinatura de acesso, API de contas ou configuração do Gfilmes Servidor. A versão 1.1.0 era de preparação; não é mais necessária para este fluxo.

## Instalação

1. Instalar `Gfilmes-Local-TV-1.2.0.apk` por cima de **Gfilmes Local** 1.0.0 ou 1.1.0, sem desinstalar. Pacote `com.gfilmes.tv.local`, versão 1.2.0, código 1002000, Android 6.0+. A chave de assinatura original é preservada.
2. Abrir **Gfilmes Local**. Se ainda não houver perfis, escolher **Assistir agora**: cria um perfil local “Minha TV”, sem PIN, e abre o catálogo. Não exige nome de cliente, MongoDB, código da TV, Render ou outro APK.
3. Também é possível criar até seis perfis com nome, avatar e PIN opcional de 4 a 8 números. Se já houver perfis, selecionar um; informar o PIN apenas se o próprio perfil tiver proteção.
4. Em Minha TV, usar Trocar perfil ou Editar meu perfil. Favoritos, histórico e progresso ficam separados por perfil. Os perfis criados na 1.1.0 são preservados. Dados globais da 1.0.0 continuam armazenados, sem atribuição automática a um novo familiar.
5. Filmes, séries e canais continuam exigindo internet e fontes online disponíveis. O modo compatível para a interface continua habilitado inicialmente.

Nesta edição não há controle de assinatura de clientes, vencimento ou exclusividade por TV. O PIN é uma proteção local do perfil, não uma validação de pagamento. Não foram copiados usuários, senhas ou hashes do MongoDB para o APK; o banco e o produto de contas continuam intactos. Atualização manual; nenhuma alteração no site/OTA de produção nesta entrega.

## Conferência do Saimo

Foi consultado o repositório público `SaimoTV/SaimoTV-Android`, branch main, commit **e3f99cb** (`chore: v3.3.5`, 20/11/2026). A cópia local anterior estava em `3d3cf26`, versão 2.6.7. A API pública de “latest release” do GitHub retornou 404: 3.3.5 é a versão marcada no código consultado, não um APK oficial cuja publicação tenhamos verificado.

O diff inclui 82 arquivos Java/Kotlin/build, cerca de 9,8 mil linhas novas, e altera SDK/dependências, Conta Saimo opcional, sincronização, listas próprias, perfis e player. Não foi feita uma troca integral da base do Gfilmes: isso também removeria suas listas padrão e traria funcionalidades de conta externas diferentes do pedido.

Foi adaptada a correção de `Generos.capa` que preserva uma URL absoluta de pôster em vez de prefixá-la com a base de imagens. A interface cinematográfica, os perfis locais recém-implementados, o catálogo atual e o player customizado do Gfilmes foram preservados. Não foram adicionadas novas fontes ou credenciais de provedores.

Os arquivos existentes `indice.txt`, `busca.txt`, `fichas.txt` de filmes e `canais.txt` de canais responderam HTTP 200 e sem prefixo HTML na verificação desta entrega. Foram lidos no máximo 2048 bytes por URL. Isso comprova somente a resposta inicial dos arquivos, não a integridade de todas as listas, o acesso a cada título ou reprodução de vídeo.

## Testes e limitações

Os testes de interface usam catálogo sintético e bridge simulada. Incluem abrir a instalação genérica, Assistir agora, chegar ao catálogo e enviar o comando ao player nativo sem chamar `deviceStatus` ou ativação. Também incluem criar perfil/PIN, confirmar PIN, rejeitar PIN incorreto e trocar perfil; regressões de catálogo grande, filtros, paginação, DPAD, layout e inicialização.

Os testes JVM incluem persistência, PIN e política local sem timer de autorização. Não há teste físico de instalação/reprodução na TCL disponível neste ambiente. Não afirmar que um título real foi assistido na TV apenas porque o build ou as fontes responderam.
