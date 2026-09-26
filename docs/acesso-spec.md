# Spec — contas, convites e autorização (marco 2)

## Contexto e objetivo

Base verificada: commit `5cce24f`, com o catálogo do marco 1 concluído. Esta
entrega implementa o marco 2 do `ROADMAP.md`: um administrador cria convites,
convidados definem sua senha, entram na biblioteca e acessam somente cursos
publicados. Somente o administrador altera o catálogo. Não existe cadastro público.

## Decisões de arquitetura

- API Spring Boot 4.1.1 / Spring Security, PostgreSQL e Flyway continuam como
  fonte de verdade. Criar apenas novas migrações; não editar V1–V3 já aplicadas.
- Autenticação por **sessão do servidor e cookie**. Não usar JWT nem guardar
  credenciais em `localStorage`. O frontend futuro e a API devem compartilhar a
  mesma origem em produção, via Nginx.
- Manter proteção CSRF para métodos que mudam estado, inclusive login, logout,
  aceitação de convite e recuperação. Expor um modo documentado para a SPA obter
  o token CSRF antes de enviar esses pedidos. Usar os mecanismos do Spring
  Security compatíveis com a versão instalada; não desabilitar CSRF globalmente.
- Sessão com cookie `HttpOnly`, `Secure` em HTTPS e `SameSite=Lax` ou `Strict`.
  Permitir HTTP apenas no perfil local, sem enfraquecer produção. O login deve
  renovar o ID da sessão e o logout deve invalidá-la.
- Senhas: `PasswordEncoder` adaptativo do Spring Security, com hash e salt.
  Nunca persistir nem registrar senha em texto. Aceitar passphrases de 12 a 128
  caracteres; não truncar silenciosamente. Um convidado escolhe a própria senha.

## Modelo de dados

### Contas

Nova tabela `users`: ID UUID, e-mail normalizado único e obrigatório,
`password_hash` obrigatório, papel `ADMIN` ou `MEMBER`, `created_at` e
`password_changed_at` em `timestamptz`, e `credential_version BIGINT NOT NULL`
começando em 0. Normalizar e-mail de forma consistente na entrada, busca e
unicidade; preservar o e-mail de apresentação se necessário.
Não criar um usuário `MEMBER` ao emitir convite: ele nasce apenas no aceite.

Criar o primeiro administrador por uma operação de bootstrap **explícita**,
idempotente, documentada e alimentada por segredo/configuração do ambiente.
Nenhuma senha ou conta administrativa padrão pode estar no repositório, em uma
migração ou em logs. Se já existir administrador, o bootstrap não deve alterar
sua senha silenciosamente.

### Convites

Nova tabela `invitations`: ID UUID, e-mail normalizado, hash do token,
`expires_at`, `created_at`, `consumed_at`, `revoked_at` e `created_by_user_id`
referenciando o administrador. Registrar também `delivery_status` e instantes
de tentativa/envio, sem guardar o token bruto. Impedir dois convites pendentes
para o mesmo e-mail por unicidade no banco; ao reenviar, revogar o anterior.
O token bruto tem pelo menos 256 bits de
aleatoriedade criptográfica, é codificado de forma segura para URL e só aparece
no e-mail/link; no banco fica apenas seu SHA-256. A comparação usa o hash.

Convite expira 72 horas após criação e só pode ser usado uma vez. Aceitar o
convite cria `MEMBER` com hash da senha e marca o convite como consumido na
mesma transação. Duas aceitações simultâneas não podem criar duas contas.
Reenviar convite para o mesmo e-mail revoga o anterior e emite token novo.
Convidar e-mail de conta já ativa retorna conflito ao administrador.

### Recuperação de acesso

Nova tabela `password_reset_tokens`: ID UUID, `user_id`, hash do token,
`created_at`, `expires_at`, `consumed_at` e `revoked_at`. Token bruto aleatório
de pelo menos 256 bits, armazenado somente como hash, expira em 30 minutos e
tem uso único. Novo pedido revoga tokens anteriores ainda utilizáveis.
Confirmação atualiza o hash da senha e consome o token atomicamente; não cria
sessão automaticamente. Incrementar `credential_version` na troca; o principal
da sessão guarda a versão do login e uma verificação nas requisições
autenticadas rejeita/invalida sessões com versão antiga. Testar isso sem
depender da precisão de timestamps.

Usar `Clock` injetado para expiração e testes; definir a validade por instante
UTC (`now < expires_at`). Não colocar token, senha ou URL completa com token em
logs, respostas de erro, telemetria ou fixtures commitadas.

## Contrato HTTP e autorização

Documentar request/response, status e validação de cada rota implementada.
Usar JSON para a API, exceto se o login usar o formato de formulário nativo do
Spring Security; nesse caso documentar `application/x-www-form-urlencoded` para
o frontend. Usar os mecanismos de autenticação e logout do Spring Security,
sem montar um `SecurityContext` manual que não persista a sessão.

| Rota | Acesso e efeito |
| --- | --- |
| `GET /api/auth/csrf` | Público; entrega o token CSRF para a SPA. |
| `POST /api/auth/login` | Público com CSRF; autentica e cria sessão. Erro genérico para credenciais inválidas. |
| `POST /api/auth/logout` | Autenticado com CSRF; invalida sessão. |
| `GET /api/auth/me` | Autenticado; retorna ID, e-mail e papel, sem hash. |
| `POST /api/admin/invitations` | ADMIN; emite convite e envia e-mail. |
| `POST /api/auth/invitations/accept` | Público com CSRF; consome token e define senha. |
| `POST /api/auth/password-resets/request` | Público com CSRF; sempre responde de forma equivalente para e-mail existente ou inexistente. |
| `POST /api/auth/password-resets/confirm` | Público com CSRF; consome token e troca senha. |
| `GET /api/courses` | Autenticado; MEMBER vê apenas publicados; ADMIN pode ver rascunhos. |
| `GET /api/courses/{id}` | Autenticado; MEMBER vê apenas publicado; rascunho alheio responde 404. |
| `/api/admin/**` | ADMIN; expor as operações atuais de criar, organizar e publicar catálogo. |

Não criar rota de cadastro público. Padrão de segurança: negar qualquer rota
não declarada; apenas saúde básica e os fluxos públicos acima são anônimos.
Tentativa anônima recebe 401; usuário `MEMBER` em operação administrativa recebe
403. Não confiar no papel enviado pelo cliente. O backend verifica o papel a
cada requisição autenticada.

Limitar tentativas nos endpoints públicos de login, pedido de recuperação e
consumo de tokens, por origem e identidade quando disponível. Responder 429
após o limite configurável; o controle precisa ser verificável em testes e
não pode usar um mapa em memória sem limite de crescimento.

Os links de convite e recuperação apontam para uma base pública configurável
e para caminhos que o frontend implementará no marco 3. Nesta etapa, os
endpoints da API permitem completar os fluxos sem telas. Evitar vazamento do
token pelo `Referer` quando as telas forem criadas.

## E-mail e falhas

Definir uma interface pequena da aplicação para envio de mensagens e adaptar
`JavaMailSender`/SMTP por trás dela. Configurar host, porta, remetente,
credenciais, TLS e timeouts por ambiente; nenhum segredo no Git. Testes usam
uma classe fake nomeada e não fazem chamadas SMTP reais. No perfil local,
fornecer modo de inspecionar mensagens com um servidor SMTP de desenvolvimento.

Falha de envio não pode ser silenciosa: registrar um estado observável e
permitir reenvio, que revoga o token antigo. Nunca registrar o token bruto.
Pedidos públicos de recuperação não devem revelar se o e-mail existe.

## Critérios de aceite

- `cd apps/api && ./mvnw test` passa com PostgreSQL via Testcontainers.
- Flyway aplica novas versões sem alterar V1–V3; Hibernate valida entidades.
- Não há conta/credencial padrão nem cadastro público; bootstrap do ADMIN é
  explícito e idempotente.
- Convite enviado ao e-mail certo, expiração, revogação, reenvio e consumo único
  funcionam; aceite concorrente não cria duas contas.
- Banco contém somente hashes de senhas e tokens; senha definida pelo usuário
  autenticará apenas aquela conta.
- Login cria sessão, logout a invalida, recuperação troca senha sem revelar
  existência da conta e revoga a validade de sessões antigas.
- Testes HTTP cobrem 401/403, CSRF, ADMIN nas operações do catálogo, MEMBER sem
  permissão de alteração, MEMBER vendo apenas cursos publicados e ADMIN vendo
  rascunhos. Acesso direto por UUID não contorna a regra. Tentativas repetidas
  nos fluxos públicos são limitadas.
- URLs e corpos de erro não expõem tokens, hashes ou senhas. E-mail real é
  configurável sem segredos versionados.

## Referências técnicas

- [Spring Security — sessões](https://docs.spring.io/spring-security/reference/servlet/authentication/session-management.html)
- [Spring Security — CSRF para SPA](https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html)
- [Spring Security — armazenamento de senhas](https://docs.spring.io/spring-security/reference/features/authentication/password-storage.html)
- [Spring Boot — envio de e-mail](https://docs.spring.io/spring-boot/reference/io/email.html)
- [OWASP — recuperação de senha](https://cheatsheetseries.owasp.org/cheatsheets/Forgot_Password_Cheat_Sheet.html)
