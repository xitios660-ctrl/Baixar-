# GUHFLIX2.4.2 — espera inicial de reprodução

Alterações: bufferForPlaybackMs reduzido de2500 para1500ms nos dois caminhos do player (hardware de menor e maior memória). bufferForPlaybackAfterRebufferMs mantido5000ms; min/max buffer e limites32/96MiB preservados. Não foi adicionado cache de vídeos nem download antecipado. Menos buffer inicial pode antecipar o início quando a fonte é rápida, mas dá menor margem na primeira partida em redes instáveis.

Verificar atualização agora usa executor exclusivo, separado da fila serial de login/renovação/autorizações/heartbeat. Consultas de release lentas não ocupam essa fila. Autenticação continua serial, sem corrida de refresh tokens. Limite3telas, validação antes do player e watchdog permanecem.

Executado: compilação e assinatura; verificador estático; APK final decodificado novamente para conferir os argumentos1500/5000ms em ambos os caminhos e o executor separado. Não houve medição de tempo até primeiro frame, throughput, reprodução ou travamentos em aparelho. Não afirmar ganho de segundos/percentual de tempo real medido.

O app não aumenta a banda contratada. Fonte/provedor, Wi-Fi/Ethernet, DNS, roteamento e servidor de licença influenciam o início. A API no plano gratuito Render ainda pode suspender após inatividade; não houve contratação ou mudança de plano. Não alterados resolução/qualidade, credenciais, catálogos nem banco.

Correção da faixa sobreposta2.4.1 preservada. Perfis separados e homologação completa continuam pendentes.
