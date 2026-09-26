# API de acesso — contas, convites e autorização (marco 2)

Contrato HTTP implementado a partir de [acesso-spec.md](acesso-spec.md). Todas as rotas
ficam sob a mesma origem do frontend (Nginx em produção). Erros usam
`application/problem+json` (`type`, `title`, `status`, `detail`); os textos de erro nunca
contêm senha, token ou hash.

## Sessão e CSRF (como a SPA deve chamar a API)

- Autenticação por **sessão de servidor** com cookie `LIBRARY_SESSION` (`HttpOnly`,
  `SameSite=Lax`, `Secure` fora do perfil `local`). Não há JWT nem nada em `localStorage`.
- Todo `POST`/`PUT`/`PATCH`/`DELETE` exige CSRF, inclusive login, logout, aceite de
  convite e recuperação. Fluxo:
  1. `GET /api/auth/csrf` → `{"headerName":"X-CSRF-TOKEN","parameterName":"_csrf","token":"..."}`.
  2. Enviar o valor de `token` no cabeçalho `X-CSRF-TOKEN` (ou no campo de formulário `_csrf`).
  3. **Buscar um token novo depois do login e depois do logout**: o Spring Security troca o
     token nesses momentos, e o anterior passa a responder 403.
- O token vem mascarado (muda a cada chamada, mas continua válido durante a sessão) e fica
  guardado na sessão do servidor. `GET /api/auth/csrf` cria uma sessão anônima se ainda
  não existir; sessões anônimas expiram após 15 minutos sem uso, e o login devolve o timeout
  normal (8 h).
- Anônimo em rota protegida recebe **401**. Autenticado sem permissão, ou sem CSRF válido,
  recebe **403**. Por isso uma escrita anônima sem token CSRF responde 403 antes de chegar à
  autorização.
- Toda requisição autenticada relê a conta no banco: o papel vem do banco, e a sessão cai
  (401) se a senha foi trocada depois do login (`credential_version`) ou se a conta sumiu.

## Rotas

Nenhuma rota de cadastro público existe. Qualquer rota não listada é negada: 401 para
anônimo e 403 para autenticado.

| Rota | Acesso | Requisição | Respostas |
| --- | --- | --- | --- |
| `GET /actuator/health` | público | — | 200/503 `{"status":...}` (sem detalhes) |
| `GET /api/auth/csrf` | público | — | 200 `{headerName, parameterName, token}` |
| `POST /api/auth/login` | público + CSRF | `application/x-www-form-urlencoded`: `email`, `password` | 200 `{id, email, role}` e sessão com **novo ID**; 401 `invalid email or password` (igual para e-mail inexistente e senha errada); 403 sem CSRF; 429 |
| `POST /api/auth/logout` | autenticado + CSRF | — | 204 e sessão invalidada; 401 sem sessão; 403 sem CSRF |
| `GET /api/auth/me` | autenticado | — | 200 `{id, email, role}` (sem hash); 401 |
| `POST /api/auth/invitations/accept` | público + CSRF | JSON `{"token":"...","password":"..."}` | 204 (conta MEMBER criada, **sem** sessão); 400 token inválido/expirado/usado ou senha fora da política; 409 conta já existe; 429 |
| `POST /api/auth/password-resets/request` | público + CSRF | JSON `{"email":"..."}` | **sempre** 202 sem corpo para e-mail bem formado, exista a conta ou não; 400 e-mail malformado; 429 |
| `POST /api/auth/password-resets/confirm` | público + CSRF | JSON `{"token":"...","password":"..."}` | 204 (senha trocada, sessões antigas derrubadas, **sem** sessão nova); 400; 429 |
| `GET /api/courses` | autenticado | — | 200 `[{id, title, description, status}]` por título. MEMBER: só `PUBLISHED`; ADMIN: todos |
| `GET /api/courses/{id}` | autenticado | — | 200 `{id, title, description, status, modules:[{id, title, position, lessons:[{id, title, position}]}]}`; 404 se não existe **ou** é rascunho e quem pede é MEMBER; 400 UUID inválido |
| `POST /api/admin/invitations` | ADMIN + CSRF | JSON `{"email":"..."}` | 201 `{id, email, createdAt, expiresAt, deliveryStatus, deliveryAttemptedAt, deliveredAt}`; 400 e-mail malformado; 409 e-mail já tem conta |
| `GET /api/admin/invitations` | ADMIN | — | 200 convites em aberto (inclui `FAILED` e expirados sem reenvio) |
| `POST /api/admin/courses` | ADMIN + CSRF | JSON `{"title":"...","description":null}` | 201 `{id}` (rascunho); 400 título inválido |
| `POST /api/admin/courses/{id}/publish` | ADMIN + CSRF | — | 204; 404; 409 curso incompleto |
| `POST /api/admin/courses/{id}/modules` | ADMIN + CSRF | JSON `{"title":"..."}` | 201 `{id}`; 404; 409 curso publicado |
| `PUT /api/admin/courses/{id}/modules/order` | ADMIN + CSRF | JSON `{"ids":[...]}` (lista completa) | 204; 400 lista inválida; 404 |
| `POST /api/admin/modules/{id}/lessons` | ADMIN + CSRF | JSON `{"title":"..."}` | 201 `{id}`; 404 |
| `PUT /api/admin/modules/{id}/lessons/order` | ADMIN + CSRF | JSON `{"ids":[...]}` (lista completa) | 204; 400; 404 |

Validação comum: corpo JSON malformado → 400 com texto fixo (o corpo não é ecoado);
campos obrigatórios ausentes → 400 citando só o nome do campo; 403 para MEMBER em
qualquer `/api/admin/**`.

### Convites

- Válido por **72 horas** (`now < expires_at`, em UTC) e de **uso único**. O aceite trava a
  linha do convite (`SELECT ... FOR UPDATE`) e cria o MEMBER na mesma transação; aceites
  simultâneos criam uma única conta.
- Um novo `POST /api/admin/invitations` para o mesmo e-mail **reenvia**: revoga o convite
  em aberto e emite token novo. É também o caminho para tratar `deliveryStatus = FAILED`.
- Falha de SMTP não é silenciosa: o convite fica `FAILED` (com `deliveryAttemptedAt`), a
  resposta mostra isso e o log registra `mail delivery failed` com o ID do convite.
- A senha é validada antes de consumir o token: uma senha fraca não gasta o convite.
- Emissão/reenvio e aceite do mesmo e-mail são serializados por um lock de transação do
  PostgreSQL (`pg_advisory_xact_lock` sobre o e-mail normalizado), pego antes de qualquer lock
  de linha. Um reenvio que começa enquanto um aceite ainda confirma espera e então recebe 409
  (a conta já existe); um aceite que começa durante um reenvio espera e encontra o convite
  antigo revogado (400).

### Recuperação de acesso

- Token válido por **30 minutos**, uso único; um novo pedido revoga o anterior.
- **Fila durável.** O endpoint só grava o pedido em `password_reset_requests` (mesmo
  INSERT com ou sem conta) e responde 202 depois do commit. Resposta e tempo não revelam
  se o e-mail existe. Se o processo parar depois do 202, o pedido continua `PENDING` e é
  atendido quando a aplicação voltar.
- Um worker agendado (`LIBRARY_PASSWORD_RESET_POLL_INTERVAL`, padrão 5 s) trava cada pedido
  vencido com `FOR UPDATE SKIP LOCKED`, que é seguro com várias instâncias. Na mesma
  transação ele busca a conta, emite o token (só o hash vai ao banco) e envia o e-mail.
  Depois marca o pedido como `SENT`, ou como `NO_ACCOUNT` quando não há conta.
- **Falha de envio.** Na mesma transação que ainda trava o pedido, o token emitido é revogado
  (um link que não chegou a ninguém não vale). O pedido registra `attempt_count`,
  `last_error` (destinatário e erro SMTP, nunca o link) e a próxima tentativa com espera
  dobrada (1 min, 2 min, 4 min...). Só então o lock é solto, então nenhum outro worker vê o
  pedido entre o envio e o registro do resultado. Na última tentativa
  (`LIBRARY_PASSWORD_RESET_MAX_ATTEMPTS`, padrão 5) o pedido fica `FAILED`. Cada falha gera o
  log `password reset delivery attempt failed` com `requestId`, `attemptCount` e `status`.
  Para receber o link, basta pedir de novo: cada pedido cria uma linha nova.
- **Erro inesperado** (banco, bug no adaptador): a transação inteira é desfeita, inclusive o
  token. A falha é registrada numa transação nova só se o pedido continua `PENDING` com o
  mesmo número de tentativas que o worker viu. Se outro worker já o tratou nesse intervalo, a
  falha antiga é descartada e o resultado dele fica.
- **Ordem de locks:** pedido → conta (`users`) → tokens. A confirmação trava a conta antes do
  token, na mesma ordem do worker, então confirmar um link enquanto outro pedido da mesma
  conta é processado não causa deadlock: um espera o outro. Se o worker emitir um token novo
  antes, o link antigo passa a responder 400, porque um novo pedido revoga o anterior.
- O e-mail sai antes do commit do worker. Se o commit falhar depois do envio, aquele link não
  vale, e o pedido é reprocessado com um token novo. O link válido é sempre o do último e-mail.
- Pedidos concluídos são apagados após `LIBRARY_PASSWORD_RESET_RETENTION` (padrão 30 dias).
- A confirmação troca o hash, incrementa `credential_version` e consome o token na mesma
  transação. Todas as sessões abertas antes recebem 401 na próxima requisição.

### Links enviados por e-mail

`{LIBRARY_PUBLIC_BASE_URL}/convites/aceitar#token=...` e
`{LIBRARY_PUBLIC_BASE_URL}/recuperar-acesso/nova-senha#token=...`.

`LIBRARY_PUBLIC_BASE_URL` precisa ser uma **origem**: esquema, host e porta opcional, sem
caminho, query, fragmento ou credenciais. Fora do perfil `local`, só `https` é aceito. A
regra vem do perfil ativo, não de uma variável, e a aplicação não sobe com um valor inválido.

O token vai no **fragmento** (`#`), que o navegador não envia ao servidor nem no
`Referer`. A tela do marco 3 deve ler o token do fragmento, enviá-lo no corpo do `POST` e
limpar o fragmento (`history.replaceState`). A API já responde `Referrer-Policy: no-referrer`;
o Nginx deve fazer o mesmo nas páginas do frontend.

### Limite de tentativas

Login, pedido de recuperação, confirmação de recuperação e aceite de convite contam
tentativas em janela fixa, por origem (IP). No login e no pedido de recuperação também há
limite por e-mail: estrito para o par e-mail + origem (um atacante em outro IP não bloqueia
o dono da conta) e um teto mais alto para o e-mail somando todas as origens. Toda tentativa conta, com ou sem sucesso. Acima do limite, a resposta é **429**
com `Retry-After` (segundos). Os contadores ficam na tabela `auth_attempt_windows`, com a
chave em SHA-256 (sem IP ou e-mail em texto), e as janelas vencidas são apagadas
periodicamente.

Atrás do Nginx, o IP vem de `X-Forwarded-For` somente quando o proxy está numa rede interna
(`server.forward-headers-strategy=native`). O Nginx deve definir esse cabeçalho.

## Configuração por ambiente

Nenhum segredo fica no Git. As variáveis abaixo estão ligadas explicitamente em
`application.yaml` (o relaxed binding do Spring removeria os hífens dos nomes, o que é fácil
de errar no deploy):

| Variável | Obrigatória | Uso |
| --- | --- | --- |
| `LIBRARY_PUBLIC_BASE_URL` | sim | Origem pública do frontend, ex.: `https://cursos.exemplo.com` (HTTPS obrigatório fora do perfil `local`) |
| `LIBRARY_MAIL_FROM` | sim | Remetente dos e-mails |
| `SPRING_MAIL_HOST` / `SPRING_MAIL_PORT` | sim | Servidor SMTP |
| `SPRING_MAIL_USERNAME` / `SPRING_MAIL_PASSWORD` | conforme o SMTP | Credenciais SMTP (segredo) |
| `LIBRARY_MAIL_SMTP_AUTH` | não (padrão `true`) | `mail.smtp.auth` |
| `LIBRARY_MAIL_SMTP_STARTTLS` | não (padrão `true`) | STARTTLS habilitado **e obrigatório** |
| `LIBRARY_ATTEMPT_WINDOW` | não (padrão `15m`) | Janela do limite de tentativas |
| `LIBRARY_ATTEMPT_MAX_PER_CLIENT` | não (padrão `30`) | Máximo por IP na janela |
| `LIBRARY_ATTEMPT_MAX_PER_IDENTITY` | não (padrão `10`) | Máximo por e-mail + origem na janela |
| `LIBRARY_ATTEMPT_MAX_PER_IDENTITY_ALL_CLIENTS` | não (padrão `100`) | Máximo por e-mail somando todas as origens |
| `LIBRARY_PASSWORD_RESET_POLL_INTERVAL` | não (padrão `5s`) | Intervalo do worker da fila de recuperação |
| `LIBRARY_PASSWORD_RESET_MAX_ATTEMPTS` | não (padrão `5`) | Tentativas de envio antes de `FAILED` |
| `LIBRARY_PASSWORD_RESET_RETRY_BACKOFF` | não (padrão `1m`) | Espera antes da 2ª tentativa (dobra a cada falha) |
| `LIBRARY_PASSWORD_RESET_RETENTION` | não (padrão `30d`) | Retenção dos pedidos concluídos |
| `LIBRARY_ADMIN_BOOTSTRAP_ENABLED` / `_EMAIL` / `_PASSWORD` | só no bootstrap | Ver abaixo |

Timeouts SMTP de conexão, leitura e escrita: 5 s (`application.yaml`). Sem
`SPRING_MAIL_HOST`, `LIBRARY_PUBLIC_BASE_URL` ou `LIBRARY_MAIL_FROM`, a aplicação não sobe.

O indicador de saúde de e-mail está desligado (`management.health.mail.enabled=false`): uma
queda do SMTP não deve derrubar a API, e a falha já aparece em cada convite.

## Bootstrap do primeiro administrador

Não existe conta padrão. O ADMIN é criado por uma execução explícita:

```sh
LIBRARY_ADMIN_BOOTSTRAP_ENABLED=true \
LIBRARY_ADMIN_BOOTSTRAP_EMAIL=admin@exemplo.com \
LIBRARY_ADMIN_BOOTSTRAP_PASSWORD='<passphrase de 12 a 128 caracteres>' \
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

- Se não existe ADMIN, cria a conta com o hash da senha e registra `adminId` no log (nunca
  a senha).
- Se o ADMIN já existe com o mesmo e-mail, não faz nada: **não troca a senha**. Para trocar,
  use a recuperação de acesso.
- E-mail diferente do ADMIN existente, e-mail de um MEMBER, senha fora da política ou
  e-mail/senha ausentes derrubam a inicialização com a causa na mensagem.
- Depois da execução, remova as variáveis `LIBRARY_ADMIN_BOOTSTRAP_*` do ambiente. A V1
  tem um único ADMIN, garantido por índice único no banco.

## Desenvolvimento local

O perfil `local` sobe PostgreSQL e **Mailpit** pelo `infra/compose.dev.yaml`. Os e-mails
de convite e recuperação ficam em <http://localhost:8025> e não são entregues de verdade.
Nesse perfil o cookie de sessão aceita HTTP e os links apontam para
`http://localhost:5173`.

## Senhas

- Passphrases de 12 a 128 caracteres, contados em code points (acentos e emojis contam
  como um). Nada é truncado nem aparado.
- Hash PBKDF2-HMAC-SHA256 com salt (`{pbkdf2@SpringSecurity_v5_8}...`) via
  `DelegatingPasswordEncoder`. BCrypt foi descartado porque ignora o que passa de 72 bytes.
