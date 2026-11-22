# Gfilmes Local 1.1.0 — família e APK por TV

Esta versão continua no pacote `com.gfilmes.tv.local`, com a assinatura Android original. É atualização manual da edição Local 1.0.0. O número da versão é 1.1.0, código 1001000. As versões de clientes que usam contas e o Gfilmes Servidor são outros pacotes e não são substituídas.

## Fluxo do titular e do cliente

1. Instalar o APK de preparação 1.1.0 por cima do Local, sem apagar dados.
2. Abrir **Gfilmes Local**. A tela **Autorizar esta TV** mostra um código de 64 caracteres, derivado da chave pública criada no Android Keystore do aparelho. Enviar o código ao titular e identificar qual acesso existente deverá usá-lo. O código não é senha.
3. O operador verifica no MongoDB original, com leitura autorizada, o estado atual do cliente e o vencimento. Não são criadas contas, renovações nem pagamentos. O registro reduzido usado para emitir autorização fica privado: `id`, `role`, `status`, `expiresAt`, `checkedAt` (UTC ISO). Não utilizar o snapshot antigo como confirmação de pagamento atual.
4. O operador emite uma autorização pública assinada para **uma TV e uma referência opaca de cliente**, com validade limitada ao vencimento verificado, e compila o APK personalizado. O arquivo não contém nomes de usuário, hashes de senha, URI MongoDB ou lista de clientes. Não publicar APKs personalizados no repositório público: a referência ainda é um identificador pseudônimo. A ferramenta não consulta nem altera o MongoDB e exige confirmação de estado atual com até cinco minutos de idade.
5. Instalar o APK personalizado por cima da preparação. Na abertura aparece **Quem vai assistir?**, com até seis perfis. Criar nome, avatar e PIN opcional de 4 a 8 números. O PIN é criado no aparelho, não é a senha antiga do MongoDB. Um novo acesso exige nova verificação e outro APK personalizado para a TV correspondente.
6. Em **Minha TV**, usar **Trocar perfil** ou **Editar meu perfil** para alterar nome/avatar/PIN ou excluir o perfil. Alterar/remover o PIN exige o PIN atual. Não há recuperação automática de PIN por servidor. Guardar o PIN; limpar dados apaga perfis e também pode invalidar a identidade do dispositivo.

O APK de preparação **não libera reprodução** e não inclui autorizações fictícias. Para concluir o primeiro APK personalizado faltam o código real da TV e a identificação do cliente a verificar. Não houve emissão de autorização para contas reais nesta entrega.

## O que é local

Catálogo, resolução de fontes, categorias, filtros, favoritos, histórico, progresso e player usam o código local existente. A fonte online de catálogo, imagens, metadados e vídeos continua exigindo internet e disponibilidade externa. Sem dependência da API de contas MARY, de licença remota, de Render ou de servidor rodando na TV. Sem serviço público, túnel, portas abertas ou chave privada no cliente.

PINs são derivados com PBKDF2, salt aleatório por perfil e comparação em tempo constante, sem PIN em texto claro. Cinco erros causam espera local de um minuto (limitação casual, não proteção contra extração/root; reiniciar o processo reinicia o contador). Favoritos e histórico usam IDs aleatórios separados, e o armazenamento de progresso do player fica separado por perfil. O perfil selecionado não é persistido como sessão autenticada; a interface pede seleção novamente ao abrir. Os dados globais da edição 1.0.0 ficam preservados e não são atribuídos automaticamente a um familiar novo.

A autorização usa RS256 e a chave pública do assinante Android original. O identificador do dispositivo deriva de uma chave privada não exportável do Android Keystore. Copiar o APK ou os arquivos de preferências para outra TV não satisfaz o vínculo. Aparelhos com root ou APK modificado podem contornar controles locais; não há DRM ou proteção absoluta.

O vencimento é verificado localmente na abertura, nas operações e no heartbeat **local** do player, sem pedido HTTP. Um checkpoint cifrado pelo Keystore e o relógio monotônico detectam recuos/alterações relevantes do relógio durante o uso e após reinício. Sem relógio confiável externo não é possível garantir defesa absoluta contra apagamento de dados, restauração do sistema ou adulteração de dispositivo. Pode ser necessário suporte após mudança legítima de data.

## Limites que permanecem

- Não é possível revogar à distância um APK sem comunicação online. Bloqueios/renovações do MongoDB posteriores à emissão não chegam ao APK; o vencimento assinado continua valendo até expirar.
- Cópias antigas da edição Local **1.0.0**, já distribuídas sem controle, continuam com o comportamento antigo. Esta atualização não as bloqueia remotamente nem garante que todos a instalem. Não distribuir a 1.0.0 se a exigência for exclusividade por TV.
- Sem fiscalização de telas simultâneas entre dispositivos desconectados. O operador precisa registrar as autorizações para administrar quantas TVs atende; esta entrega não promete contagem global de três dispositivos.
- O APK personalizado precisa ser entregue de modo privado ao cliente. A chave privada de assinatura fica no ambiente do operador, nunca no aplicativo.

## Compilação pelo operador

```sh
# Acesso verificado e atualizado, fora do repositório e com permissão 0600.
JAVA_HOME=/caminho/jdk node tools/licensing/issue-local-device.mjs /privado/acesso-verificado.json CODIGO_DA_TV /privado/autorizacao.jws
# Não coloque a assinatura ou o JSON em argumentos de shell; passe caminhos.
tv/gradlew -p tv :app:assembleRelease -PgfilmesLocal=true -PgfilmesLocalGrantFile=/privado/autorizacao.jws
```

Inspecionar assinatura, pacote, versão, vínculo e prazo do **APK personalizado resultante** antes de distribuir. O verificador `tools/verify-local-apk.py` desta entrega é específico do APK de preparação e exige autorização vazia. Não usar seu manifesto para um APK personalizado diferente.
