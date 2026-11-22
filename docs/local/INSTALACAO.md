> **Atualização 1.3.0:** perfis agora são pessoas, sem PIN. Veja [PERFIS-SEM-SENHA-1.3.md](PERFIS-SEM-SENHA-1.3.md). As instruções de PIN abaixo são históricas.

> **Versão atual 1.2.0:** use [ABRIR-CATALOGO-1.2.md](ABRIR-CATALOGO-1.2.md). Abre sem código de TV ou APK personalizado. As orientações abaixo são históricas.

> **Atualização 1.1.0:** esta página descreve a edição anterior 1.0.0. Para perfis e acesso por TV, siga [PERFIS-E-ACESSOS.md](PERFIS-E-ACESSOS.md). O novo APK exige autorização individual antes do catálogo.

# Gfilmes Local — abrir direto, sem senha ou servidor de contas

Esta edição atende ao pedido posterior do titular de abandonar a configuração de API/Servidor na TV e usar o catálogo diretamente. Reaproveita a interface cinematográfica, fontes de catálogo, filtros, busca, temporadas, episódios e player nativo do projeto existente.

## Instalar

1. Baixe Gfilmes-Local-TV-1.0.0.apk e permita a instalação pelo aplicativo usado para baixar, quando o Android solicitar.
2. Abra o ícone **Gfilmes Local** na TV. A abertura animada leva diretamente ao catálogo; não existe etapa de usuário/senha, licença, autorização ou importação do MongoDB nesta edição.
3. Conecte a TV à internet para atualizar as listas, carregar capas e reproduzir vídeos/canais online. Não há download de filmes para assistir sem internet.
4. Use o controle remoto normalmente: Filme/Série → Filmes, Séries ou Animes; lupa com filtros; TV ao vivo; Minha TV para favoritos e preferências. O botão Voltar retorna entre detalhe/biblioteca/escolha de tipo.
5. Se a interface tiver problema visual, use Menu e o modo compatível existente. Na edição local, o modo compatível já é o padrão inicial, como na configuração usada para a TCL.

Nome: Gfilmes Local. Versão: 1.0.0 / código 1000000. Pacote: **com.gfilmes.tv.local**. Android mínimo: 6.0 / API 23. Assinatura: chave Android original existente, conferida pelo certificado, sem novo keystore.

É um pacote separado dos aplicativos anteriores. Ele não instala por cima de com.guhflix.tv.connect, com.guhflix.tv ou do Gfilmes Servidor. Favoritos/histórico da instalação antiga não são transferidos automaticamente; continuam preservados nela. Novos favoritos e progresso são guardados no pacote local. Abra especificamente o ícone Gfilmes Local para não voltar à tela de login do app anterior.

## O que funciona localmente

- Perfil “Minha TV” local, sem representar uma conta de cliente no banco.
- Catálogo nativo obtido das mesmas fontes configuradas, com cache e fallback existentes.
- Busca combinada por título, tipo, gênero e ano; ordenação e rolagem progressiva; grade com DOM limitado.
- Temporadas/episódios resolvidos nativamente; player e histórico/progresso locais.
- Interface preta/vermelha, logos, fontes e imagens cinematográficas do projeto.

A API MARY, o MongoDB, o servidor de ativação e o Gfilmes Servidor não participam da abertura, navegação ou autorização de reprodução desta edição. O transporte HTTP de contas tem bloqueio explícito, o endereço de API fica vazio no BuildConfig, o polling de nuvem é desativado e o servidor de controle LAN do cliente não é iniciado. O player não usa timer de autorização nem heartbeat nesta edição.

Fontes online, capas e guia continuam sendo serviços externos. Disponibilidade, autorização de uso, resolução, formato e estabilidade do vídeo dependem dessas fontes e da rede/dispositivo. Não foram adicionados sites de filmes ou credenciais privadas de provedores ao APK.

## Diferenças de operação

Esta edição não aplica assinatura, pagamento, vencimento, três telas ou emissão de licença: é a edição local solicitada pelo titular, isolada do produto com contas. Ela não altera regras, usuários, vencimentos ou pagamentos da produção. Controle de assinatura e edição local sem API são modelos distintos.

Pareamento pelo catálogo do celular, controle de nuvem, servidor LAN e metadados extras fornecidos pela API não estão disponíveis neste pacote. A tela Minha TV apresenta favoritos e preferências, sem botões de login ou código de vínculo. Atualizações desta edição são manuais por APK; ela não consulta nem instala o OTA do pacote com contas.

O Servidor anterior pode ficar fechado. Não é necessário importar autorização ou migração para usar Gfilmes Local. Os arquivos privados de migração anteriormente preparados foram preservados fora do repositório público, sem implantação ou corte administrativo.

## Validação e limites

Consulte TESTES.md e o manifesto do APK. Build e assinatura não substituem testes físicos: instalação, reprodução real e medição de tráfego na TCL ainda dependem do aparelho do titular. O ambiente não possui TV conectada por ADB. Não foi substituído o site/OTA de produção; o arquivo é entregue como artefato desta branch separada.
