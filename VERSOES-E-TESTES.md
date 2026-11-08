# GUHFLIX · versões de 08/11/2026

| Dispositivo | Versão | Uso |
| --- | --- | --- |
| Windows 10/11 x64 | 3.1.0 | Catálogo e reprodução nativos |
| Android TV / Google TV | Connect 2.6.0 | Catálogo, player e vínculo com o iPhone |
| Android celular/tablet | 2.6.0 | Aplicativo nativo na horizontal |
| iPhone / iPad | Web atualizada | Catálogo instalável pelo Safari e controle da TV |

## Melhorias

Nova abertura animada GUHFLIX, recomendações por coleção e universo (incluindo Marvel quando os metadados identificam a produtora), carrosséis e banner no catálogo web, painel renovado e portal de downloads por plataforma. As recomendações só incluem títulos presentes no catálogo autorizado.

Se as fontes falham, o player permanece no filme e tenta novamente após 5, 10, 20 e até 30 segundos. É possível cancelar ou tentar imediatamente. Isso não garante que uma fonte indisponível volte a funcionar; autenticação e DRM continuam respeitados.

## Testes executados

- API: compilação TypeScript e 7 testes aprovados.
- Catálogo: 8 testes em WebKit/iPhone e Chromium/Android aprovados, incluindo recomendações Marvel, busca, episódios, vínculo, TV escolhida, revogação e troca de conta.
- Catálogo real: consulta de Homem de Ferro confirmou recomendações Marvel e continuações sem duplicações, além de sete carrosséis. Sem execução do stream.
- Painel: 2 testes de navegador aprovados (desktop e iPhone), incluindo login, filtro, teste de três dias sem pagamento e logout; teste do proxy aprovado.
- Android/TV: 9 testes JVM por edição, compilação release e verificação criptográfica dos APKs, launcher, componentes e atualização própria.
- Windows: 96 testes aprovados, 9 ignorados; 1 teste adicional HLS sintético aprovado com a DLL original. Compilação nativa CI 42448332395.
- Portal: navegação e instruções nas três apresentações desktop/iPhone/Android, downloads e ausência de erro de script verificados.

Downloads públicos: os dois APKs e o ZIP Windows foram baixados por completo e tiveram tamanho e SHA-256 conferidos; os APKs responderam HTTP 206 ao download parcial. Consulte [verificação dos arquivos](DOWNLOADS-verificados.json).

## Limitações

Não houve teste de instalação ou reprodução em TCL/celular físico nesta atualização. Nos testes de navegador, a API e o MongoDB são reais e locais, mas catálogo e TV são fixtures. A CI Windows não tem OpenGL 2.0 para apresentar toda a interface. Filmes comerciais, GPU, áudio físico e legendas precisam ser confirmados nos aparelhos. A disponibilidade de cada vídeo depende do provedor autorizado e da conexão.

iOS oferece catálogo e controle da televisão pelo Safari; não inclui IPA nem reprodução no próprio iPhone. Android celular utiliza interface nativa na horizontal. Windows exige OpenGL 2.0 e o executável não possui Authenticode.

Use um acesso de cliente ativo. O administrador serve para o painel. Até três reproduções simultâneas continuam sendo verificadas no servidor. Nenhuma conta real foi renovada ou alterada durante os testes.

O Connect 2.6.0 atualiza o Connect 2.5.1. O antigo GUHFLIX 2.4.2 permanece um aplicativo separado. No Windows, extraia o ZIP completo em uma pasta nova.

## Código-fonte

- [TV, Android, API, painel e catálogo web](GUHFLIX-2.6.0-fonte.zip)
- [Windows 3.1.0](GUHFLIX-Windows-3.1.0-fonte.zip)

Os arquivos privados de assinatura e senhas não estão nos downloads.
