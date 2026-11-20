# Gfilmes — APKs de homologação offline

**Não é liberação para os clientes atuais.** Os APKs foram compilados e assinados com o certificado original. Instalação na TCL, reprodução audiovisual, importação SAF, reinício real e acesso HTTPS de outra rede continuam pendentes. O túnel gratuito não confirmou conectividade pública nos testes.

| Arquivo | Versão / código | Pacote | Android mínimo |
|---|---|---|---|
| [Cliente](Gfilmes-Cliente-3.0.0-homologacao.apk) | 3.0.0 / 3000000 | com.guhflix.tv.connect | 6.0 / API 23 |
| [Servidor](Gfilmes-Servidor-1.0.0-homologacao.apk) | 1.0.0 / 1000000 | com.gfilmes.activationserver | 8.0 / API 26 |
| [Recuperação online opcional](Gfilmes-Recuperacao-Online-3.0.1.apk) | 3.0.1 / 3000001 | com.guhflix.tv.connect | 6.0 / API 23 |

[Links de download conferidos novamente pela internet](VERIFIED-DOWNLOADS.json). As cópias baixadas tiveram tamanho, SHA256 e assinatura conferidos.

Assinatura v2 verificada nos três: **fe13bf8634b76a4450b70833a7a6eb6aa01429729371a33dccc75c8eaa72b76b**. [Hashes SHA256](SHA256SUMS.txt) · [Manifesto de verificação](APK-MANIFEST.json).

## Ordem simples de preparação

1. Instalar **Servidor** somente na TV do titular. Confirmar seletor de documentos SAF; exportar a chave **pública**. Nunca enviar chave privada, senha ou URI do banco.
2. Em computador seguro, usar as ferramentas do [guia operacional](../../docs/offline/OPERACAO.md) para aprovar a chave, exportar snapshot cifrado/assinado e importar no servidor. Reutiliza o administrador original, não cria outro.
3. Configurar certificado HTTPS confiável e acesso externo. Validar de outra rede. MARY e o Atlas original permanecem intactos. Computador/mini PC é a alternativa preparada quando a TV/rede não atenderem.
4. Instalar **Cliente** em uma TV de teste. Atualiza por cima apenas o pacote Connect com mesma assinatura. Selecionar SERVIDOR DE ATIVAÇÃO e informar a origem HTTPS preparada; entrar com conta existente autorizada.
5. Verificar prazo, catálogo, fontes autorizadas e reprodução. Parar o servidor, reabrir e reiniciar a TV para homologar o uso local. A licença limita-se ao menor prazo entre 30 dias e assinatura real; internet para vídeos continua necessária.
6. Renovar: verificar pagamento no painel escolhido como autoridade, confirmar mais 30 dias sem reduzir saldo existente e autenticar o cliente para nova emissão. Não administrar duas cópias divergentes das assinaturas.

O cliente instalado sozinho, apontando para a MARY atual ainda sem autoridade configurada, não completa a ativação offline. A preparação do servidor é obrigatória.

A **Recuperação online 3.0.1** é exclusivamente retorno da TV de teste ao modo online original, com mesma assinatura e sem apagar dados. Exige a API MARY funcionando e login quando necessário. Não é uma atualização offline e não foi publicada no OTA. Depois dela, a próxima edição offline precisará de código acima de 3000001.

[Resultados e testes físicos pendentes](../../docs/offline/TESTES.md) · [Configuração, segurança, migração, backup e rollback](../../docs/offline/OPERACAO.md).

Três dispositivos licenciados não significam fiscalização de três streams simultâneos offline. Bloqueio/retirada não revoga imediatamente uma licença já emitida. Alterações comuns do relógio e cópias simples são recusadas; sem hardware/tempo confiável conectado não existe proteção absoluta contra root ou congelamento do RTC.

Nenhum segredo acompanha a entrega. Nenhum serviço pago foi contratado. Produção, site de downloads e OTA não foram modificados.
