# GUHFLIX Samsung Tizen 1.0.0 — desenvolvimento

Aplicativo TV Web empacotado, com interface própria, foco por controle, login de cliente, catálogo paginado, busca, detalhes, episódios, recomendações, AVPlay, retentativas e vínculo com o catálogo do celular. Não é um redirecionamento para o site nem um APK Android.

Alvo de projeto: Tizen TV 4.0 ou superior. É necessário confirmar modelo/ano da Samsung. Modelos anteriores ao Tizen e aparelhos com outro sistema não estão cobertos. O WGT disponibilizado é um arquivo de desenvolvimento SEM assinatura Samsung. Não foi instalado em TV nem validado pelo Tizen Studio. Para instalar, crie um projeto TV Web básico no Tizen Studio, copie os arquivos fornecidos e assine com um perfil Samsung autorizado ao DUID da TV. A chave Android não serve.

A API /api/samsung oferece somente rotas de cliente explicitamente permitidas, sem cookies ou administração. Autenticação e limite de telas usam as regras existentes. Origens file:// e null são tratadas apenas no adaptador. A senha não é salva; o token de renovação fica no armazenamento privado do aplicativo. AVPlay usa concessões temporárias do servidor, não URLs de provedor fornecidas pelo usuário. Não há contorno de DRM nem conversão de codecs.

Testes: sintaxe JavaScript; XML e referências do pacote; fluxo de login, catálogo, detalhe, despacho AVPlay e logout em Chromium com AVPlay simulado; dez testes da API, incluindo preflight, login nativo sem cookies, recusa de cookies e de rotas administrativas. Isso não comprova reprodução ou comandos numa Samsung real. A instalação ainda exige certificado/DUID e testes no aparelho. A publicação na loja Samsung não foi feita.

Fontes oficiais: https://developer.samsung.com/smarttv/develop/getting-started/setting-up-sdk/creating-certificates.html ; https://developer.samsung.com/smarttv/develop/getting-started/using-sdk/tv-device.html ; https://developer.samsung.com/smarttv/develop/guides/multimedia/media-playback/using-avplay.html
