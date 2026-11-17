# Oracle: teste paralelo do GUHFLIX

**Estado: preparação; nenhuma conta ou VM Oracle criada e nenhum cliente migrado.**
O Render MARY, downloads, APKs, MongoDB e DNS de produção continuam intactos.
A pilha hospeda API, painel e catálogo web no mesmo hostname HTTPS.

## 1. Conta e recursos gratuitos

O titular cria a conta em https://www.oracle.com/cloud/free/ e preenche pessoalmente identidade, telefone e eventual verificação de cartão. Não compartilhar senha, cartão, chaves, códigos ou arquivos de credenciais pelo chat/repositório.

No console, confirmar as cotas reais e a indicação **Always Free eligible** antes de provisionar. Não autorizar upgrade pago. Na documentação consultada em 17/11/2026, A1 oferece 1.500 OCPU-h e 9.000 GB-h/mês (equivalente a 2 OCPUs e 12 GB), mais 200 GB de volumes e 10 TB de saída. A cota exibida na conta prevalece; o crédito de avaliação temporário não é gratuidade permanente. Pode faltar capacidade e máquinas ociosas podem ser recuperadas.

Provisionar Ubuntu 24.04 ARM64 na região principal, shape A1 elegível, dentro das cotas totais da conta e com volume de boot de 50 GB. Não criar banco Oracle, balanceador pago, serviços auxiliares ou armazenamento de filmes. Guardar a chave SSH em armazenamento privado do titular. Permitir TCP 80/443; SSH 22 apenas a partir do IP administrativo. Não publicar a porta 10000 nem abrir SSH para 0.0.0.0/0. Conferir as regras tanto na rede OCI quanto no firewall Ubuntu, sem alterar as regras de outras aplicações.

## 2. Origem de teste e banco existente

É necessário um hostname controlado pelo titular, por exemplo staging.seudominio.com, apontando ao IP público estável da VM. Não alterar o hostname atual do Render. Certificados automáticos precisam de DNS correto e entrada nas portas 80/443.

No Atlas, liberar somente o IP de saída da VM como /32, caso ainda não permitido. Preservar as outras entradas; não ampliar a lista para toda a internet. Usar o mesmo banco `guhflix`, URI e JWT_SECRET existentes. Não gerar outra chave JWT e não executar bootstrap, seed, restauração ou migração de dados. A inicialização normal apenas garante os índices já usados pelo produto.

## 3. Instalar a pilha

Instalar Docker Engine e o plugin Compose pelo procedimento oficial para Ubuntu:
https://docs.docker.com/engine/install/ubuntu/

Clonar o repositório e fazer checkout de um commit revisado contendo estes arquivos. Construir na própria arquitetura da VM: o módulo argon2 tem dependência nativa. Esta preparação foi testada localmente apenas na arquitetura efetivamente disponível; teste ARM é uma etapa separada.

Criar `/etc/guhflix` com acesso administrativo restrito e copiar os templates para `/etc/guhflix/oracle.env` e `/etc/guhflix/api.env` com permissão 600. Preencher os valores privados diretamente na VM, por meio seguro. Nunca versionar esses arquivos, imprimir `docker compose config` completo, env ou `docker inspect` com segredos. Não copiar BOOTSTRAP_TOKEN. Reutilizar TMDB_API_KEY e configurações opcionais de catálogo se presentes no serviço atual.

Em api.env, ADMIN_ORIGIN e PWA_ORIGIN devem ser exatamente a nova origem HTTPS, sem barra final. A instância Render mantém seus valores atuais. A aplicação lê `.env` via Compose, não precisa de dotenv.

Na raiz do checkout:

```sh
sudo docker compose --env-file /etc/guhflix/oracle.env -f deploy/oracle/compose.yaml config --quiet
sudo docker compose --env-file /etc/guhflix/oracle.env -f deploy/oracle/compose.yaml build
sudo docker compose --env-file /etc/guhflix/oracle.env -f deploy/oracle/compose.yaml up -d
sudo docker compose --env-file /etc/guhflix/oracle.env -f deploy/oracle/compose.yaml ps
python3 deploy/oracle/verify-public.py https://staging.seudominio.com
```

O Caddy encaminha os cabeçalhos de proxy ao serviço interno. A API não tem porta exposta no host, roda sem root e com filesystem somente leitura. O proxy não registra URLs de reprodução nem cookies em access logs. Certificados ficam em volumes persistentes; não usar `down -v` na operação normal. Logs de containers são rotacionados; ainda devem ser tratados como privados.

## 4. Validar antes de qualquer troca

O verificador público apenas lê health, catálogo web e painel, e confirma 401 sem sessão. Não cria usuários ou assinaturas e não prova reprodução. Depois, com conta dedicada de QA já autorizada:

- Login, vencimento, bloqueio e limite de três reproduções; não conceder acesso extra a clientes vencidos.
- Catálogos de filmes, séries, temporadas/episódios, gêneros, paginação e busca.
- Reprodução autorizada de filme, episódio e canal; avanço, pausa, heartbeat e encerramento de cada sessão de teste.
- Pareamento e controle do celular para a TV; tela cheia na TV física.
- Painel com administrador existente e origem HTTPS correta; não criar outro administrador.
- Reinício da VM/pilha, restauração de certificados, uso de memória, CPU e tráfego; backups existentes do Atlas preservados.

Cookies são específicos da origem: entrar novamente no hostname de teste é esperado. Os APKs atuais continuam apontando para o Render e não mudam automaticamente de servidor. Testar a Oracle no navegador não significa que o APK instalado foi validado. Uma troca posterior exige estratégia de hostname/API e pacote assinado com a chave original, quando necessária. Não publicar OTA antes de validar a versão correspondente.

## 5. Reversão e operação

Durante o teste, parar a pilha de teste com `docker compose ... stop` mantém o Render funcionando e preserva os volumes. Não deletar banco, usuários, dados ou recursos antigos. Só migrar produção depois dos resultados reais e da revisão da estratégia de clientes existentes; não inventar confirmação de instalação física.

Atualizar o sistema operacional e as imagens regularmente, manter backups e monitorar cotas da conta. Oracle gratuita não elimina indisponibilidade da fonte de vídeo, da internet ou limites/termos do provedor. Não fazer tráfego artificial para tentar impedir recuperação de máquina ociosa.
