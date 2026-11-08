# Catálogo: contas, nomes e controle de várias TVs

Trocar conta aparece no topo do catálogo; Sair da conta permanece no rodapé. A ação para o player local, encerra a sessão do catálogo, fecha os diálogos e limpa os destinos selecionados. Não encerra a conta da TV nem interrompe automaticamente sua reprodução.

No vínculo, Nome desta TV é opcional (até 60 caracteres); a lista também oferece Renomear. O endpoint PATCH /api/connect/devices/:id usa o vínculo e a sessão do catálogo para impedir alteração de outra conta. O nome pertence ao vínculo desse catálogo com a TV.

Marque as TVs e toque Controlar TVs marcadas para enviar comandos ao grupo. Os comandos são enviados em paralelo; cada TV recebe um identificador independente e confirma o resultado. Falhas indicam o nome da TV. O controle mostra o estado da TV principal. Isso não sincroniza quadros nem garante que vídeos iniciem no mesmo instante. O limite de três reproduções simultâneas continua sendo verificado pelo servidor.

A seleção de grupo dura a sessão da página; ao recarregar, somente a TV principal salva é retomada, se o vínculo continuar válido. Neste aparelho limpa a seleção remota. Pedidos pendentes são cancelados ao sair da página e não continuam consultando comandos após troca de conta. A atualização periódica dos estados não recria os controles de seleção enquanto o diálogo está aberto.

Verificação: build TypeScript, dez testes da API e doze testes Playwright em WebKit/iPhone e Chromium/Android aprovados. Os testes cobrem nomes no vínculo, renomeação autorizada, recusa de outra conta, comandos confirmados por duas TVs simuladas, troca de conta sem destinos herdados e os fluxos existentes de catálogo/reprodução local. Não houve operação de duas TVs físicas neste ambiente. As contas de teste são locais; nenhum cadastro real foi alterado.
