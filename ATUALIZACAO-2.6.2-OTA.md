# GUHFLIX Connect / Android 2.6.2 · correção do atualizador

## Problema e causa reproduzida

O Connect 2.5.1 mostrava a atualização disponível, concluía o download e recusava um APK válido com a mensagem de assinatura inválida. A biblioteca apksig usa reflexão para interpretar estruturas ASN.1/PKCS#7. A otimização R8 removia anotações, assinaturas genéricas, campos e modelos utilizados por essa biblioteca. A verificação apenas pelo apksigner do computador não detectava a falha do verificador embarcado e otimizado.

O teste em tools/ota-regression/check.py processa a biblioteca com R8 antes de executá-la contra o APK real publicado. Reproduziu a recusa antes das regras de preservação e aceitou o mesmo arquivo após a correção. Também recusou um certificado esperado diferente e um APK com bytes de classes.dex modificados. São testes JVM/R8; não equivalem a uma atualização executada na TCL física.

## Correção

Preservados classes, campos, anotações e assinaturas genéricas da apksig. Permanecem as verificações de hash, assinatura criptográfica, igualdade da chave com a instalação, pacote e versão crescente. Não foi desativada nenhuma validação de segurança.

A busca de atualização consulta metadados com uma URL nova para evitar oferta antiga em cache. A tela Ajustes retoma uma instalação pendente quando o usuário retorna da permissão de instalação do Android. Falhas do verificador geram códigos técnicos no log GUHFLIX-OTA, sem sugerir que um arquivo não verificado seja instalado.

## Instalação de reparo

As versões antigas já instaladas contêm o verificador defeituoso. Não é possível corrigir esse código apenas alterando o manifesto do servidor. É necessário baixar uma vez o APK 2.6.2 pelo navegador/Downloader e instalar sobre o GUHFLIX Connect existente, sem desinstalar. O pacote com.guhflix.tv.connect e o certificado da versão 2.5.1 são preservados; portanto a atualização normal do Android mantém os dados do aplicativo. O Android continua solicitando confirmação de instalação.

Na TV: https://guhflix-tv-download.onrender.com/connect.html

## Verificações

- Regressão com R8 e APK real: erro anterior reproduzido; arquivo legítimo aceito após a correção; chave diferente e arquivo alterado recusados.
- Nove testes JVM de política OTA/retentativas por edição.
- Compilações release otimizadas, apksigner, identidade do pacote, versão, launcher, providers e manifesto OTA verificados.
- Campos e anotações da biblioteca preservados no resultado do R8.
- Não houve execução do instalador ou navegação em uma TV/celular físico. A confirmação final do fluxo no aparelho permanece pendente.
