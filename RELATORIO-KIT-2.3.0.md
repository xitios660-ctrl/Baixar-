# GUHFLIX2.3.0 — integração do kit visual

O ZIP enviado contém imagens conceituais, como informa LEIA-ME.txt. Não contém telas executáveis, ícones individuais de produção ou animações. As imagens completas não foram colocadas sobre telas como botões falsos, e os personagens/canais de exemplo não foram publicados como se fossem conteúdo disponível.

## Alterações implementadas

Logo nativo GUH branco/FLIX vermelho, inclinado, com arco vermelho; aplicado no cabeçalho, acesso e abertura. Ícone G vetorial próprio no launcher. Superfícies escuras com gradiente vinho, contorno discreto e cantos menores. Botões do player/detalhes com geometria alinhada à referência. Mantido o sistema CinemaScene2.2 (arte do catálogo, fundo panorâmico, entrada/dissolvência, foco e respeito ao movimento reduzido).

Nova ação “Verificar atualização” em Conta quando a assinatura está ativa. Consulta o release.json público por HTTPS com timeout e limite16KiB; compara versionCode instalado. Se houver versão superior, oferece abrir o site oficial. Sem navegador na TV, mostra o endereço para Downloader. Não instala silenciosamente nem depende de permissões extras. Falhas de rede exibem mensagem. Esse fluxo foi compilado, mas não executado na TV nesta etapa.

## Fluxos preservados / dependências

Busca, favoritos, catálogo, canais, eventos, temporadas, detalhes e player permanecem os componentes existentes ligados ao catálogo autorizado. A disponibilidade dos títulos e canais depende do provedor, sem catálogo de exemplo estático. Login/sessão e limite3 reproduções preservados. A verificação de atualização não altera esses limites.

Perfis individuais com favoritos/histórico separados NÃO foram implementados. Não há alegação de que todas as telas da prancha funcionam ou estão idênticas à imagem. Os estados de erro existentes continuam vinculados aos erros reais, sem simular manutenção ou um título indisponível. Não foram adicionados botões sem função para representar recursos ausentes.

## Verificações executadas

Compilação Java, DEX, Smali e aapt2; verificador estático; pacote com.guhflix.tv, versão2.3.0/código2030000; assinatura com mesmo certificado de atualização. Ícone vetorial e recursos compilados. URLs de produção preservadas. Fontes nativos reconstruíveis entregues sem chave privada.

## Pendências para concluir “tudo funcionando”

Implementar perfis com isolamento persistente correto; validar layout e animações no hardware; executar login real, navegação por controle, busca, favoritos, reprodução/áudio/legendas, troca de episódio, abertura de download de atualização e estados de erro. O ambiente atual não tem dispositivo Android conectado; o emulador não pôde iniciar por armazenamento insuficiente. Não alegar homologação, fidelidade visual integral,60FPS ou4K. Esta é uma entrega incremental compilada, não homologação de todas as funcionalidades da referência.
