# Spec — telas básicas (marco 3)

## Contexto e objetivo

O marco 2 já oferece sessão, CSRF, convites, recuperação de acesso e endpoints de
catálogo. O frontend em apps/web ainda é o template do Vite. Esta entrega implementa
o marco 3 do ROADMAP.md: o administrador organiza e publica cursos no navegador;
um membro convidado entra e navega apenas pelos cursos publicados.

A tela de cada aula mostra sua posição no curso e um estado claro de conteúdo
indisponível. Upload, reprodução e progresso pertencem aos marcos 4–6.

## Escopo e limites

- Substituir o template Vite por uma SPA React/TypeScript em português, responsiva,
  com URLs navegáveis diretamente e estados acessíveis de carregamento, vazio e erro.
- Consumir os endpoints existentes descritos em docs/acesso-api.md. A API continua
  sendo a fonte de verdade para permissões, ordem, publicação e validação.
- Implementar as páginas de aceite de convite e recuperação porque os e-mails do
  marco 2 já apontam para essas rotas. Um convite aceito não autentica o usuário.
- Não acrescentar edição ou exclusão de cursos, módulos ou aulas, despublicação,
  gerenciamento de usuários, player, upload, progresso ou migrations neste marco.
  Se algum contrato da API impedir o aceite, documentar a lacuna e fazer a menor
  correção necessária, com teste.

## Rotas e acesso

| Rota | Acesso | Conteúdo |
| --- | --- | --- |
| / | todos | Redireciona para /entrar, /cursos ou /admin/cursos conforme a sessão. |
| /entrar | público | Login e links para recuperação. |
| /convites/aceitar | público | Define senha usando o token do fragmento da URL. |
| /recuperar-acesso | público | Solicita link sem revelar se o e-mail existe. |
| /recuperar-acesso/nova-senha | público | Define nova senha usando o token do fragmento. |
| /cursos | autenticado | Cursos visíveis para o papel atual. |
| /cursos/:courseId | autenticado | Módulos e aulas do curso, na ordem recebida da API. |
| /cursos/:courseId/aulas/:lessonId | autenticado | Aula selecionada, com contexto do curso; sem vídeo. |
| /admin/cursos | ADMIN | Lista de cursos e criação de rascunho. |
| /admin/cursos/:courseId | ADMIN | Adição e reordenação de módulos/aulas; publicação. |
| /admin/convites | ADMIN | Emissão, listagem e reenvio de convites. |

Uma rota inexistente mostra página 404. Acesso direto e recarga de uma rota interna
devem funcionar; configurar fallback da SPA no desenvolvimento e documentar a
necessidade do mesmo fallback no Nginx da produção. O frontend oculta controles
administrativos do MEMBER, mas a autorização permanece obrigatória na API.

## Sessão, CSRF e cliente HTTP

- Na inicialização, consultar GET /api/auth/me antes de escolher a área exibida.
  401 significa sessão ausente/expirada; limpar o estado em memória e levar à
  página de login, preservando apenas a rota interna de destino. Não mostrar
  conteúdo protegido enquanto essa verificação está pendente.
- Login: obter GET /api/auth/csrf, enviar POST /api/auth/login como
  application/x-www-form-urlencoded com email e password; usar o usuário da
  resposta ou consultar /me. Logout: POST /api/auth/logout com CSRF, limpar o
  estado local e voltar ao login. O token CSRF anterior ao login/logout não vale
  depois da troca da sessão.
- Para cada operação que muda estado, buscar o CSRF atual e enviá-lo no cabeçalho
  indicado pela própria resposta. Usar cookies de mesma origem; não colocar
  senha, token de convite/recuperação ou dados de sessão no localStorage.
- No desenvolvimento, fazer proxy de /api do Vite para a API local, mantendo as
  chamadas do navegador em uma única origem. Em produção, frontend e API
  compartilharão a origem via Nginx. Não depender de CORS permissivo.
- Centralizar chamadas HTTP tipadas. Tratar 204 sem tentar ler JSON. Interpretar
  application/problem+json e preservar status para a interface decidir: 400
  validação, 401 sessão, 403 permissão/CSRF, 404 recurso, 409 conflito,
  429 limite de tentativas com Retry-After, e falha de rede.
- 403 da API tem mensagem conjunta para papel e CSRF: não presumir que todo 403
  autoriza repetição automática de uma escrita. Oferecer atualização/repetição
  explícita ao usuário quando apropriado. Impedir duplo envio enquanto uma
  operação estiver pendente.

## Fluxos públicos de acesso

### Login e saída

Formulário com rótulos, erros legíveis e senha mascarada. Credenciais erradas
exibem erro genérico. Após login, ADMIN vai para /admin/cursos e MEMBER para
/cursos, exceto quando há uma rota interna permitida a recuperar. Uma rota
/admin acessada por MEMBER mostra acesso negado ou volta à área de cursos.
Logout invalida a sessão e a navegação protegida deixa de aparecer.

### Convite

Ler token de #token=... na rota /convites/aceitar, removê-lo imediatamente da
URL com history.replaceState antes de outras chamadas ou renderização que
possa gerar navegação. Manter o token somente na memória da página e enviá-lo
apenas no corpo de POST /api/auth/invitations/accept. Exibir formulário para
definir e confirmar a senha de 12–128 caracteres, respeitando a validação real
da API. Após 204, orientar a pessoa a entrar; não criar sessão automaticamente.
Se o link estiver ausente, expirado, revogado ou usado, mostrar estado seguro
sem ecoar o token e indicar que precisa de novo convite.

### Recuperação

Em /recuperar-acesso, POST /api/auth/password-resets/request e mensagem de
confirmação idêntica para qualquer e-mail bem formado, exista a conta ou não.
Em /recuperar-acesso/nova-senha, limpar o fragmento como no convite e enviar
token e nova senha somente no corpo de POST /api/auth/password-resets/confirm.
Após 204, orientar novo login. Não registrar URL completa, fragmento, token
ou senha em logs, analytics ou mensagens de erro. Configurar política de
referrer no documento e evitar recursos externos nas páginas com token.

## Catálogo do membro

- GET /api/courses alimenta a lista; GET /api/courses/{id} alimenta o detalhe.
  Mostrar título, descrição, módulos e aulas na ordem de position recebida,
  inclusive ao abrir uma URL diretamente.
- Aula selecionada precisa pertencer ao curso carregado; caso contrário, mostrar
  404. Exibir título, módulo e posição. Não mostrar player falso, botão de
  concluir nem progresso antes dos marcos correspondentes.
- Lista vazia, curso sem aulas, carregamento, falha de rede e 404 têm estados
  distintos. MEMBER nunca vê rascunho, inclusive por URL conhecida; respeitar
  o 404 devolvido pela API, sem tentar contornar a permissão no cliente.

## Administração

- /admin/cursos usa GET /api/courses para mostrar rascunhos e publicados.
  Criar curso com título e descrição opcional via POST /api/admin/courses;
  navegar ao curso criado. Não oferecer edição que não exista na API.
- /admin/cursos/:id usa GET /api/courses/{id}. Adicionar módulo via
  POST /api/admin/courses/{id}/modules; adicionar aula via
  POST /api/admin/modules/{id}/lessons.
- Reordenar módulos e aulas com controles acessíveis de subir/descer (arrastar
  pode ser acrescentado, mas não é obrigatório). Enviar sempre a lista completa
  de IDs ao PUT de ordem correspondente. Atualizar a visão após sucesso;
  em falha, manter/recarregar a ordem confirmada pelo servidor.
- Publicar via POST /api/admin/courses/{id}/publish, com confirmação clara.
  Mostrar o motivo do 409 quando a estrutura estiver incompleta. O backend
  permite acrescentar aula a curso publicado e impede acrescentar módulo a
  curso publicado; a interface deve refletir exatamente essas regras.
  Publicação neste marco é estrutural e não significa que haja vídeo.
- /admin/convites lista GET /api/admin/invitations e cria ou reenvia pelo
  POST /api/admin/invitations com o mesmo e-mail. Mostrar status SENT, FAILED,
  PENDING e expiração sem expor token; após falha, permitir novo envio.
  Explicar que reenviar invalida o link anterior.

## Interface e acessibilidade

Interface simples e consistente, em português, utilizável em desktop e celular.
Usar elementos semânticos, rótulos de formulário, foco visível, teclado para
reordenação, mensagens de erro associadas aos campos e avisos de resultado
anunciados de modo acessível. Não depender apenas de cor para distinguir
rascunho, publicado ou falha. Exibir estado pendente e impedir ações repetidas.

## Critérios de aceite

- No navegador, ADMIN entra, cria um rascunho, adiciona módulos e aulas,
  reordena ambos, publica e vê a nova ordem após recarregar.
- ADMIN envia convite pelo navegador; o e-mail aparece no Mailpit local.
  Convidado abre o link, define a senha, entra e vê apenas publicados.
- MEMBER recebe 404 para rascunho conhecido e não acessa /admin; a API
  continua respondendo 403 às operações administrativas.
- Pedido e confirmação de recuperação funcionam a partir dos links de e-mail.
  As duas páginas removem #token da barra antes de enviar requisições.
- Login/logout, recarga de rota interna, expiração da sessão, 400/401/403/404/
  409/429, falha de rede e telas vazias têm comportamento verificável.
- Em apps/web, os comandos npm run build, npm run lint e npm test passam.
  Testes de componentes/fluxos usam API fake nomeada; uma verificação manual
  com API local e Mailpit cobre o caminho completo. Se a API mudar, executar
  também cd apps/api && ./mvnw test.
- Nenhum token ou senha fica persistido no browser ou aparece em logs/URLs após
  a leitura inicial do link. Nenhum upload, player ou progresso é implementado.
