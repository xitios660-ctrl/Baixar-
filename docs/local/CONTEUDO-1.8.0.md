# Local 1.8.0 — resultados, episódios e primeira imagem

Relato recebido: buscas de Os Simpsons/Alive com timeout, fichas de #blackAF/#AMFAD sem sinopse, VOD #AMFAD e Band abrindo player sem imagem. Não houve acesso ao log da TV ou conexão ADB para reproduzir esses sintomas no equipamento.

## Diagnóstico e alterações

Abrir a ficha de uma série chamava `Titulos.resolver`, que não apenas lia episódios: também consultava informações extras de temporadas e tentava resolver fontes de addons. A ficha Local agora usa somente a série e os episódios publicados pela fonte, sem resolver vídeo nem expandir temporadas por metadados externos. Quando não há episódios, informa essa ausência explicitamente. O player continua resolvendo o conteúdo quando realmente solicitado. Isso reduz trabalho na abertura da série; não comprova a eliminação dos timeouts específicos relatados.

O filtro agora reutiliza o padrão de normalização de acentos em vez de recompilá-lo em cada título. Não muda gêneros/tipos/perfis nem regras de acesso.

Os players de VOD e canais cancelavam o prazo de “sem imagem” no estado READY. Esse estado pode existir antes da primeira imagem. Agora, para vídeo, a confirmação ocorre em `onRenderedFirstFrame`; o prazo existente de troca de fonte permanece ativo enquanto não chega a imagem. Fontes somente de áudio com faixa selecionada continuam iniciando em READY, sem exigir quadro de vídeo. A telemetria de início de vídeo passa a representar a primeira imagem. Isso melhora a detecção e a troca/indicação de erro; não repara uma fonte indisponível ou um vídeo cujo próprio conteúdo é preto.

A edição Local não fornece o serviço autenticado de metadados usado por `Detalhes`. Os arquivos de catálogo incorporados fornecem títulos, IDs, gêneros e capas, sem sinopses. A versão informa “Sinopse não fornecida pelo catálogo local”. Não foi adicionada chave privada de metadados ao APK, não foi reativada dependência da API nem se inventaram sinopses. Fornecimento de sinopses reais permanece pendente de uma fonte autorizada compatível.

## Evidências e limites

Em 23/11, consultas curl dos arquivos públicos `busca.txt` e `catalogo.txt` retornaram HTTP 200. O índice contém correspondências para Simpsons, Alive, #blackAF e #AMFAD. Outra requisição HTTP da mesma máquina recebeu erro: não se afirma disponibilidade uniforme por dispositivo. A raiz do site de canais respondeu 404; o arquivo `catalogo.txt` é a consulta relevante, e respondeu 200. Nenhum desses resultados prova funcionamento do vídeo.

Resultados finais de testes e APK estão na entrega. Browser usa ponte simulada. Sem teste de decodificação, primeira imagem física, áudio ou streaming contínuo na TCL. #AMFAD, Band e os títulos específicos não devem ser anunciados como reproduzindo até validação real. Não houve alteração de banco, fonte, credenciais, produção, Render ou OTA.
