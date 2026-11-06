# GUHFLIX — entrega e validação

## Resultado
APK Android TV nativo reconstruído da base autorizada Saimo TV 2.6.7. Pacote próprio `com.guhflix.tv`, versão 1.0.0, mínimo Android 6.0 (API 23). A TCL informada usa Android 9, dentro dessa faixa; isso não substitui teste no aparelho. A entrega é uma versão candidata: não há validação de execução suficiente para declarar o objetivo integralmente concluído.

## Implementado
- Identidade original GUHFLIX: ícone, banner, marca, preto #080808 e vermelho #E50914.
- Abertura nativa animada com perspectiva, extrusão, partículas e iluminação vermelha; OK permite pular.
- Layouts de início e detalhes redesenhados, banner maior, tipografia ampliada, capas maiores, gradientes e indicadores de foco.
- Transições de escala e elevação ao navegar por controle remoto.
- Pacote, autoridades de providers e esquema de busca próprios para coexistir com o original.
- Assinatura própria; player e integrações continuam dentro do APK, sem depender da instalação do aplicativo original.

## Recursos preservados da base
Media3/ExoPlayer, catálogo e parsers, busca, favoritos, histórico/continuar assistindo, detalhes, temporadas, episódios, seleção de fontes e controles nativos de reprodução. O suporte existente a trailers MP4/HLS foi mantido quando o catálogo fornece uma URL. A preservação foi constatada na estrutura/código; estes fluxos não foram confirmados em execução nesta entrega.

## Testes efetivamente realizados
| Verificação | Resultado |
|---|---|
| Inspeção do APK e decompilação | Concluída: app nativo, componentes e player identificados |
| Compilação Java e reconstrução Smali/recursos | Concluída |
| Recompilação pelo build.py entregue | Concluída |
| Assinatura e alinhamento | APK gerado; apksigner validou v1, v2 e v3 |
| Pacote próprio, launcher e providers | Passou na inspeção do manifesto |
| XML, IDs críticos, integração da extensão, DEX e licença | Passou em test_static.py |
| Acesso ao índice padrão do catálogo via HTTP no ambiente | HTTP 200 em /indice.txt; não comprova reprodução |
| Inicialização do app em emulador | Não executada: Android do emulador não concluiu boot; serviço PackageManager indisponível |
| Controle remoto, retorno, busca e detalhes em execução | Não testados |
| Catálogo carregado dentro do app | Não testado |
| Reprodução, pausa, avanço e troca de episódios | Não testados |
| Recuperação de erros em execução | Não testada |
| TCL física, memória, 60 FPS e 4K | Não testados |

## Limitações e dependências
Não há transferência automática de conta, listas privadas, favoritos ou ponto salvo do Saimo instalado: pacotes distintos têm dados distintos. Configure as fontes autorizadas no GUHFLIX. Disponibilidade dos servidores, credenciais eventualmente necessárias e codecs continuam dependências da base. Nenhuma autenticação, DRM ou proteção de streams foi contornada. Não foi implementado download offline. Não há garantia de fluidez a 60 FPS ou reprodução 4K sem medir no hardware real.

Os fontes entregues são Java novo, recursos e Smali reconstruível; o código Kotlin original de alto nível não estava no APK. Créditos técnicos e licenças obrigatórias permanecem. Alterações visuais exigem revisão no controle remoto da TCL antes de distribuição de produção. A chave privada de assinatura não está incluída no ZIP; atualizações devem usar a mesma chave, ou a instalação anterior precisará ser removida.

## Reproduzir verificações
Veja README.md do fonte para compilar. Para validar a estrutura: `python3 test_static.py --apk /caminho/GUHFLIX-TV.apk`. Para assinatura: `apksigner verify --verbose GUHFLIX-TV.apk`.
