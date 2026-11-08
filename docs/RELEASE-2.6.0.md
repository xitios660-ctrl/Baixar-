# GUHFLIX · atualização de 08/11/2026

- TV Connect e Android: 2.6.0, versão interna 2060000. TV mantém pacote e assinatura do Connect 2.5.1; celular usa com.guhflix.android e atualização independente.
- Windows: 3.1.0, compilado nativamente no Windows, player e DLLs originais preservados.
- Portal com downloads por dispositivo, instruções de instalação e catálogo iOS pelo Safari.
- Abertura GUHFLIX com tipografia ampliada, partículas e luzes vermelhas nos aplicativos.
- Recomendações por coleção, produtoras Marvel/DC e similaridade, sempre cruzadas com os títulos disponíveis no catálogo autorizado. Sem metadados, retorna gêneros relacionados.
- Player mantém o título e a posição ao repetir fontes: esperas de 5, 10, 20 e até 30 segundos, com cancelamento. Erros de DRM não são contornados.
- Catálogo web com banner, sinopse, carrosséis e recomendações. Escolha explícita da TV e vínculos isolados por conta.
- Painel com nova apresentação, indicador de conexão, navegação e opção explícita de teste de três dias, sem registrar pagamento.

## Validação executada

- API: build TypeScript e 7 testes de integração/lógica, incluindo autenticação, pagamentos idempotentes, limites de reprodução, vínculos, catálogo e recomendações.
- Catálogo real: consulta de Homem de Ferro retornou recomendações Marvel e continuações sem repetir o mesmo TMDB; sete carrosséis carregados. Não houve execução do stream.
- Web: 8 testes Playwright aprovados em WebKit/iPhone e Chromium/Android, com API e MongoDB locais e TV simulada.
- Painel: 2 testes Playwright aprovados em Chromium desktop e WebKit/iPhone; login, criação de teste, filtro, ausência de pagamento fictício e logout. Teste Node do proxy aprovado.
- Android: builds de release assinados, 9 testes JVM de OTA/retentativa por edição. Inspeção do APK final: assinatura, launcher, componentes, identificador, providers e manifesto OTA.
- Windows: 96 testes aprovados, 9 ignorados; teste adicional HLS sintético aprovado. CI 42448332395.

## Limites de validação

Os testes web usam contas e catálogo de teste locais, sem alterar clientes reais. A TV é simulada nos testes de comandos. Não foi executada instalação/reprodução em uma TCL ou celular físico. A CI Windows alcança a criação da janela, mas não tem OpenGL 2.0 para renderizar a interface completa. Não se afirma que todos os filmes foram reproduzidos: dependem das fontes autorizadas, da conta e da rede.

iOS é catálogo instalável pelo Safari e controle da TV; não é um IPA nem player local. Android móvel é um aplicativo nativo horizontal, com reprodução local, ainda sujeito à validação em dispositivo real. Windows requer OpenGL 2.0 e não possui assinatura Authenticode.

Não foram alterados valores, vencimentos, permissões ou pagamentos de contas reais. Chaves de assinatura e credenciais não integram o código-fonte publicado.
