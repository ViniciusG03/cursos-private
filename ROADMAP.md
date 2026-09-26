# Roadmap — biblioteca privada de cursos

Este documento acompanha a V1 do projeto. Avançamos um marco por vez: implementar,
verificar o critério de conclusão e só então iniciar o próximo.

## Objetivo da V1

O administrador publica cursos e convida usuários por e-mail. Todos os usuários
convidados podem acessar os cursos publicados, mas cada um tem seu próprio progresso.
O administrador envia vídeos grandes, aula por aula, e o upload pode ser retomado.

**Cenário de aceitação:** publicar um curso com um vídeo de mais de 1 hora e vários GB,
convidar duas pessoas, interromper e retomar o upload, reproduzir o vídeo com avanço
pela linha do tempo e confirmar que cada pessoa continua de onde parou.

## Decisões atuais

- Monorepo com `apps/api` (Spring Boot), `apps/web` (React, TypeScript e Vite) e `infra`.
- PostgreSQL guarda contas, convites, catálogo, metadados dos vídeos e progresso.
  Os arquivos de vídeo não ficam no banco.
- Vídeos e dados temporários de upload ficam no block volume, nunca no disco principal
  da VPS. O caminho de armazenamento será configurável por ambiente.
- Upload resumível com Uppy e tusd; evitar um protocolo de chunks próprio.
- Reprodução de MP4 com HTTP Range pelo Nginx, após autorização pela API usando
  `X-Accel-Redirect`. O Spring não envia o arquivo inteiro ao navegador.
- A organização de cursos, módulos e aulas é manual. Apenas o administrador publica
  e envia conteúdo. Não há cadastro público.
- O acesso aos arquivos ficará atrás de uma interface da aplicação, para permitir
  trocar o armazenamento local por R2 no futuro.

## Marcos

### 0. Fundação — concluído

- [x] Criar o monorepo e os projetos da API e do frontend.
- [x] Configurar PostgreSQL local com Docker Compose e variáveis em `.env` ignorado pelo Git.
- [x] Configurar JPA, Flyway e teste de contexto com PostgreSQL via Testcontainers.
- [x] Confirmar que a API inicia no perfil `local` e que `./mvnw test` passa.

### 1. Catálogo — concluído

- [x] Criar migrações Flyway para cursos, módulos e aulas, em passos revisáveis.
- [x] Definir ordem manual, rascunho/publicação e vínculos entre as tabelas.
- [x] Implementar entidades, repositórios e regras de organização do catálogo.
- [x] Testar as regras e validar o esquema com PostgreSQL nos testes.

**Concluído quando:** é possível representar um curso com módulos e aulas ordenados,
mantendo um curso incompleto como rascunho.

### 2. Contas, convites e autorização

- [ ] Modelar administrador, usuários e convites com prazo de validade e uso único.
- [ ] Permitir que o convidado defina sua própria senha; armazenar apenas o hash.
- [ ] Implementar login, logout e recuperação de acesso.
- [ ] Proteger operações administrativas e o acesso aos cursos publicados.
- [ ] Integrar o envio de convites por e-mail.

**Concluído quando:** não existe cadastro público, convidados conseguem entrar e
somente o administrador consegue alterar o catálogo.

### 3. Telas básicas

- [ ] Construir login e navegação pelos cursos, módulos e aulas.
- [ ] Criar telas administrativas para organizar e publicar o catálogo.
- [ ] Integrar o frontend à API e tratar estados de carregamento e erro.

**Concluído quando:** o administrador organiza um curso pelo navegador e um usuário
convidado vê apenas os cursos publicados.

### 4. Upload de vídeos

- [ ] Integrar Uppy no frontend e tusd na infraestrutura.
- [ ] Autorizar o upload apenas para o administrador e associá-lo à aula correta.
- [ ] Guardar metadados no PostgreSQL e arquivos temporários/finais no block volume.
- [ ] Tratar upload interrompido, conclusão, falha e limpeza de arquivos abandonados.

**Concluído quando:** um upload de vários GB retoma após interrupção e nenhum trecho
do vídeo é gravado no banco ou no disco principal da VPS.

### 5. Reprodução privada

- [ ] Autorizar cada solicitação de vídeo na API.
- [ ] Entregar o arquivo pelo Nginx com `X-Accel-Redirect` e suporte a HTTP Range.
- [ ] Integrar o player à página da aula e testar avanço para posições distantes.
- [ ] Definir os formatos de MP4 aceitos para reprodução nos navegadores escolhidos.

**Concluído quando:** um usuário autorizado reproduz e avança no vídeo; uma pessoa
sem acesso não consegue obter o arquivo diretamente.

### 6. Progresso individual

- [ ] Persistir a posição por usuário e aula, com atualização periódica e ao pausar.
- [ ] Retomar a aula da última posição salva e marcar aulas concluídas.
- [ ] Exibir o progresso no catálogo e na página do curso.

**Concluído quando:** duas pessoas assistem à mesma aula e retomam em posições
independentes, inclusive após sair e entrar novamente.

### 7. Produção e recuperação

- [ ] Publicar frontend, API, PostgreSQL, tusd e Nginx na VPS com HTTPS.
- [ ] Configurar segredos, caminhos de armazenamento e volumes por ambiente.
- [ ] Preparar backup de banco e vídeos; executar um teste de restauração.
- [ ] Validar reinício dos serviços, espaço em disco, logs e o cenário de aceitação da V1.

**Concluído quando:** a biblioteca funciona na VPS e os dados podem ser restaurados.

## Depois da V1

Avaliar R2, processamento com ffmpeg/faststart, HLS e outras melhorias com base no
uso real. Nenhum deles é requisito para publicar a primeira versão.
