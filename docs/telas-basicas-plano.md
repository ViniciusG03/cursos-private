# Plano de implementação — telas básicas (marco 3)

Implementar docs/telas-basicas-spec.md sobre o estado atual do projeto. Conferir
as alterações existentes antes de editar e preservá-las. O frontend ainda é o
template do Vite; o contrato HTTP está em docs/acesso-api.md. Fazer entregas
pequenas e testáveis, sem antecipar vídeo, upload ou progresso. Não marcar o
marco 3 como concluído antes de cumprir todo o aceite.

## 1. Preparar o frontend

1. Inspecionar apps/web e remover somente a demonstração do Vite. Definir
   estrutura pequena por responsabilidade: roteamento, cliente HTTP, sessão,
   páginas de acesso, catálogo e administração.
2. Configurar proxy /api no Vite para a API local (porta 8080), sem CORS aberto.
   Usar URLs relativas em todas as chamadas. Configurar roteamento com suporte
   a links diretos e uma página 404; documentar o fallback exigido no Nginx.
3. Criar modelos TypeScript explícitos para usuário, curso, módulos, aulas,
   convite, CSRF e erro problem+json. Centralizar fetch, credenciais de mesma
   origem, leitura segura de 204 e mapeamento de status. Evitar any e duplicação.
4. Definir tokens visuais básicos, layout responsivo e componentes pequenos
   para formulário, estado vazio, carregamento, erro e aviso de sucesso.
   Substituir README do template por instruções reais do projeto.

## 2. Sessão e rotas protegidas

1. Ao iniciar, consultar /api/auth/me. Criar estado de sessão em memória com
   três estados explícitos: verificando, anônimo e autenticado. Redirecionar
   sem exibir conteúdo protegido antes da resposta.
2. Implementar obtenção de CSRF para cada escrita; login form-urlencoded,
   logout e revalidação de sessão. Após login/logout, não reutilizar token
   antigo. Preservar destino interno seguro quando um 401 interromper a
   navegação; nunca aceitar redirecionamento para domínio externo.
3. Proteger rotas ADMIN no cliente para navegação, mantendo a API como fonte
   de autorização. Tratar 401, 403 e 429 de forma diferente; não repetir
   automaticamente uma escrita só por receber 403.
4. Testar cliente HTTP, renovação de CSRF, 204, login inválido, logout, sessão
   expirada, MEMBER tentando rota ADMIN e redirecionamento após login.

## 3. Telas públicas de convite e recuperação

1. Implementar /convites/aceitar e /recuperar-acesso/nova-senha. Capturar
   #token, limpar imediatamente a URL com history.replaceState e manter o
   valor apenas na memória da página. Criar estados para link ausente, inválido,
   expirado/usado, envio pendente e sucesso.
2. Implementar /recuperar-acesso com resposta visual uniforme para e-mails
   existentes e inexistentes. Validar confirmação de senha no cliente para
   reduzir erros, mas usar sempre a validação final da API.
3. Aplicar Referrer-Policy no documento e não carregar recursos de terceiros
   nessas páginas. Testar que token e senha não entram em storage, logs,
   navegação posterior nem mensagens de erro.

## 4. Catálogo para MEMBER e ADMIN

1. Implementar /cursos e /cursos/:courseId com dados de GET /api/courses e
   GET /api/courses/{id}. Preservar ordem devolvida pela API; mostrar estados
   de lista vazia, carregamento, 404 e falha de rede.
2. Implementar /cursos/:courseId/aulas/:lessonId usando a hierarquia carregada.
   Validar que a aula pertence ao curso. Mostrar apenas dados disponíveis;
   não criar player ou progresso provisório.
3. Testar navegação direta e recarga, curso publicado para MEMBER, rascunho
   recusado por 404 e seleção de aula inexistente.

## 5. Administração do catálogo e convites

1. Implementar lista de cursos com estado DRAFT/PUBLISHED e formulário de
   criação. Após 201, abrir o curso usando o ID retornado.
2. No detalhe administrativo, adicionar módulos e aulas. Reordenar com
   controles de teclado e enviar a lista completa de IDs. Recarregar dados
   após sucesso ou falha para refletir a ordem confirmada pelo servidor.
3. Implementar publicação com confirmação e tratamento de 409 estrutural.
   Respeitar a regra existente: módulo só em rascunho; aula também pode ser
   acrescentada em curso publicado. Não expor edição/exclusão inexistente.
4. Implementar listagem/emissão/reenvio de convites. Mostrar status e expiração;
   permitir reenviar FAILED com aviso de revogação do link anterior.
5. Testar payloads e sequências de chamadas com fake de API nomeada; incluir
   erros 400/403/404/409/429 e bloqueio de duplo envio.

## 6. Verificação e entrega

1. Adicionar um único comando de testes do frontend, npm test, com execução
   não interativa. Cobrir funções novas e os fluxos importantes com testes
   rápidos e independentes. Rodar npm run build, npm run lint e npm test.
2. Com API local, PostgreSQL e Mailpit, verificar no navegador: bootstrap
   ADMIN, login, criação/reordenação/publicação, convite, aceite, login MEMBER
   e recuperação. Registrar passos manuais no README sem incluir segredos.
3. Inspecionar Network e URL para confirmar cookie de sessão, CSRF em escritas,
   ausência de token no fragmento depois da leitura e nenhum token em logs.
   Conferir navegação por teclado e layout em tela pequena.
4. Se tocar a API, adicionar testes de regressão e rodar cd apps/api &&
   ./mvnw test. Revisar o diff para impedir alterações em migrations aplicadas
   ou trabalho dos marcos 4–6.
5. Só então marcar o marco 3 no ROADMAP.md como concluído. Deixar alterações
   sem commit para revisão e relatar arquivos alterados, comandos executados,
   resultados e limitações concretas.

## Pedido direto para o Claude Code

> Implemente docs/telas-basicas-spec.md seguindo docs/telas-basicas-plano.md.
> Use o contrato de docs/acesso-api.md, preserve as alterações atuais e avance
> por etapas. Rode os testes, valide o fluxo local com Mailpit e reporte
> decisões, arquivos, resultados e pendências. Não faça commit e não implemente
> upload, player, progresso ou operações administrativas sem endpoint.
