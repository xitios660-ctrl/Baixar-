# GFLIX — perfis animados integrados, edição Local 1.4.0

## Tecnologia e integração real

O projeto existente é Android nativo Kotlin/Java, com catálogo e player Media3/ExoPlayer. A apresentação cinematográfica usa HTML/CSS/JavaScript **empacotados no próprio APK**, dentro de `CinemaActivity`, comunicando-se por `GfilmesNative` com o catálogo, preferências e player nativos existentes. Não foi instalado o HTML de demonstração do ZIP nem criado site, aplicativo separado ou serviço Render.

A edição atual é `com.gfilmes.tv.local`, nome de instalação **Gfilmes Local**, versão **1.4.0**, código **1004000**, Android 6.0+. O seletor continua no fluxo IntroActivity → CinemaActivity → perfil → home real com Filmes/Séries/Busca/TV ao vivo. A edição local não usa autenticação de conta; o login e licenciamento das outras variantes foram preservados, sem ativar este fluxo local nelas. O APK atualiza a instalação Local anterior com a mesma assinatura, não substitui outro pacote de clientes ou do Servidor.

## Avatares e design

- Os 12 GIFs do ZIP foram mantidos **byte a byte**, cada um com 152×152 pixels, 16 quadros distintos, duração de 110 ms por quadro e loop infinito de 1,76 s. O manifesto fornece SHA-256 por personagem. Não há emojis, personagens reconstruídos ou troca pela tela fictícia do protótipo.
- `tv/app/src/main/assets/gfilmes/avatars/gifs/`: GIFs originais incorporados. `posters/`: o primeiro PNG de cada personagem. `manifest.json`: identidade, nomes, dimensões, tempos e hashes.
- `design/profiles/frames/`: os 192 PNGs originais para revisão/manutenção; não duplicados no APK. `previsao-original.png`: a prancha do ZIP. O ZIP do anexo foi preservado sem modificações.
- Tela 16:9 com fundo preto/vermelho discreto, logo GFLIX vermelho, título “Quem está assistindo?”, avatares grandes, nomes, borda luminosa, foco com perspectiva/escala e transição curta para a home.
- O palco lógico existente de 1280×720 é escalado proporcionalmente para Full HD 1920×1080 e demais telas. Fontes e elementos da interface permanecem vetoriais; os personagens conservam a resolução original fornecida.
- O decodificador GIF do WebView reproduz os quadros; não há um timer JavaScript por avatar nem download de animações. IntersectionObserver limita a reprodução aos avatares visíveis. Ao sair do seletor, abrir PIN, pausar o Activity ou ocultar a página, as imagens passam para o PNG estático. Até seis perfis visíveis, até doze escolhas no editor. “Reduzir movimentos” usa imagens estáticas e foco acessível.
- O modo compatível existente da TCL foi mantido. Nenhuma promessa de FPS/RAM na TV física foi baseada no teste de navegador.

## Dados por pessoa

Criar, renomear, excluir com confirmação e selecionar até seis perfis; qualquer perfil pode usar um dos doze avatares. Perfis comuns não recebem senha. Os IDs anteriores são conservados, inclusive ao renomear ou trocar avatar, preservando a associação com favoritos e progresso.

Histórico/favoritos continuam usando chaves do perfil; o progresso do player continua capturado no Intent de quem iniciou o vídeo. As preferências pessoais (idioma/versão, legenda, próximo episódio, qualidade, áudio etc.) passam a usar `ajustes-profile-<id>`; pesquisas e último canal também ficam nesse escopo. A preferência de telemetria continua global, respeitando a escolha de privacidade do dispositivo. O arquivo global de configurações anterior não é apagado; as novas configurações por pessoa começam com os padrões da edição. Não foram atribuídos históricos globais antigos a familiares novos.

“Trocar perfil” permanece em Minha TV. Setas navegam em duas dimensões, OK seleciona sem submeter o editor ao trocar avatar, Voltar cancela editor/gerenciamento/diálogo ou oferece saída no seletor. Ao selecionar, o destino é a home e o player nativo reais.

## Perfil infantil e PIN do responsável

O titular autorizou PIN **somente no controle infantil**. Ao marcar Perfil infantil, o primeiro cadastro pede a definição de um PIN de responsável com 4 a 8 dígitos. Não é uma senha da criança nem da conta de cliente. O PIN é derivado com salt aleatório e PBKDF2; não é armazenado em texto claro. Cinco erros geram espera de um minuto, persistente entre processos.

Como o catálogo não tem classificação confiável para todos os títulos, a política é **aprovação explícita**, sem supor que animação/gênero seja seguro:

1. No perfil adulto, abrir os detalhes de um filme, série ou anime e escolher **Disponibilizar no infantil**. A aprovação de uma série permite seus episódios.
2. O infantil começa vazio até haver títulos aprovados. Home, busca, filtros, detalhes, favoritos, histórico e reprodução aplicam a restrição; a política do catálogo é nativa, não apenas visual.
3. **TV ao vivo fica bloqueada** no infantil porque não há classificação confiável de cada programa. Não foi adicionada uma lista arbitrária de canais supostamente infantis.
4. Ao entrar no infantil, o bloqueio de saída fica persistido. Selecionar adulto, criar/alterar/excluir perfis ou mudar aprovações exige o PIN. Reiniciar a TV/app não remove esse bloqueio. É possível voltar a escolher um perfil infantil sem PIN.
5. Para revogar um título, usar **Remover do infantil** nos detalhes do perfil adulto. A consulta/paginação infantil muda de escopo para não aproveitar o cache adulto.

O controle vale dentro deste aplicativo, não substitui os controles parentais do Android/Google TV nem bloqueia outros aplicativos/ajustes do sistema. Limpar dados, modificar o APK ou usar aparelho com root pode contornar controles locais. Não há recuperação de PIN pelo servidor nesta edição; guardar o PIN do responsável. Não há alteração de pagamentos, contas MongoDB ou autenticação do produto de clientes.

## Build e evidências

Build release local com JDK 21, SDK 35 e keystore original existente. As chaves privadas não são incluídas nem publicadas. O manifesto da entrega contém versão, pacote, tamanho, SHA-256, impressão do certificado e verificação APK v2; os assets do APK são comparados byte a byte com a fonte.

- **49 testes JVM passaram**, zero falhas/erros/ignorados. Incluem os 12 valores de avatar, persistência/IDs, tipo infantil, PIN parental, bloqueio após reinício, aprovação/revogação, exclusão de títulos sem aprovação, e teste dos próprios accessors `Preferencias`/`Progresso` com SharedPreferences em memória para duas pessoas.
- **14 testes Chromium passaram**. Cobrem CRUD, histórico sem mistura e respostas atrasadas, navegação por setas/OK/Voltar, GIFs originais carregados e quadros mudando nos doze personagens, pausa em segundo plano/saída, 720p, 1366×768, 1920×1080, e entrada infantil vazia → saída com PIN → aprovação no adulto → título disponível no infantil. Catálogo/bridge desses testes são sintéticos.
- Prévia Full HD é uma captura da interface real empacotada usando nomes de teste, não clientes do MongoDB. Dois recortes de quadros demonstram a mudança visual de um GIF em execução. Não foi reutilizada a prévia fictícia do ZIP como evidência do app.
- O primeiro compile encontrou uso de CoroutineScope no lugar de Context em um guard de anime; foi corrigido para `this@CinemaActivity`. Os builds seguintes e as suítes passaram.

**Pendentes físicos:** instalação/atualização na TCL, teclado e controle reais, reprodução audiovisual com as fontes, leitura visual na TV, desempenho/RAM/FPS do WebView durante uso prolongado, reinício da TV e salvamento real do ExoPlayer. Não há TV/ADB conectada neste ambiente. Testes de navegador e armazenamento em memória não foram apresentados como testes físicos.

Nenhuma mudança no Render, MongoDB, cobrança, serviços de produção ou OTA. Entrega em branch separada e APK manual da mesma edição Local.
