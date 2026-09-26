# Plano de implementação — contas, convites e autorização

Implementar a [spec](acesso-spec.md) sobre o commit `5cce24f`. Conferir o estado
atual antes de editar; preservar qualquer mudança posterior do usuário. Fazer
etapas pequenas, com testes em cada uma. Não avançar para telas, vídeo, upload
ou progresso. Não marcar o marco 2 do `ROADMAP.md` antes de cumprir todo o aceite.

## 1. Migrações e modelo

1. Desenhar migrações Flyway posteriores à V3 para `users`, `invitations` e
   `password_reset_tokens`, com UUID, FKs, unicidade, datas e constraints.
   Não alterar migrações aplicadas. Definir índices para busca por e-mail e
   hash de token.
2. Mapear entidades e repositórios em pacotes focados. Tratar e-mail normalizado
   como valor único; separar o papel ADMIN/MEMBER do corpo de requisições.
3. Testar defaults, FKs, unicidade, incompatibilidades de papel e validação JPA
   contra PostgreSQL via Testcontainers.

## 2. Bootstrap e senhas

1. Implementar bootstrap explícito do primeiro ADMIN com segredo do ambiente,
   sem valor padrão. Testar primeira execução, repetição e caso de conflito.
2. Encapsular `PasswordEncoder` e política de senha em serviços pequenos;
   verificar hash, login com senha correta/incorreta e ausência de texto puro
   em banco/logs. Usar interfaces do projeto para bibliotecas de terceiros.
3. Injetar `Clock` e gerador criptográfico de tokens por construtor/parâmetro
   para testes determinísticos; não usar `Random` comum.

## 3. Convites e recuperação

1. Criar serviço de emissão, reenvio e aceite de convite. Salvar só o hash do
   token e tratar consumo em transação com bloqueio/atualização atômica.
2. Criar serviço de pedido e confirmação de recuperação com resposta pública
   uniforme. Revogar tokens anteriores; incrementar `credential_version` na
   troca e rejeitar sessões cujo principal guarda uma versão anterior.
3. Cobrir expiração no limite exato, segundo uso, token revogado/desconhecido,
   concorrência no aceite, e-mail já registrado e diferença entre ADMIN/MEMBER.
4. Criar interface de e-mail do projeto, adaptador SMTP e fake nomeada para
   testes. Configurar timeouts e ambiente local para inspecionar e-mails.
   Comprovar que falha de envio fica visível e que reenvio emite token novo.

## 4. Sessão e API

1. Configurar `SecurityFilterChain`: negação por padrão, sessões, CSRF para SPA,
   cookie seguro por ambiente, logout e tratamento consistente de 401/403.
   Usar autenticação do Spring Security e validar renovação de sessão no login.
   Implementar limitação configurável de tentativas nos fluxos públicos.
2. Expor as rotas de autenticação, convite e recuperação da spec com DTOs
   tipados, validação de entrada e erros sem vazamento de credenciais.
3. Criar endpoints de leitura do catálogo com filtro por `PUBLISHED` para MEMBER
   e acesso a rascunhos para ADMIN. Expor em `/api/admin/**` os serviços de
   criação, ordenação e publicação já existentes, sem duplicar suas regras.
4. Testar com MockMvc/Spring Security e PostgreSQL real: anônimo, MEMBER e ADMIN;
   leitura por lista e ID; tentativa de acesso direto a rascunho; CSRF ausente
   e presente; login/logout; conta recuperada e sessões antigas.

## 5. Verificação e entrega

1. Rodar formatador padrão de Java e `cd apps/api && ./mvnw test`. Testes de
   I/O externo usam fakes nomeadas; banco usa Testcontainers. Testar cada nova
   função e regressão. Funções pequenas (4–20 linhas), arquivos com menos de
   500 linhas, tipos explícitos, docstrings nas funções públicas com intenção
   e exemplo, conforme as instruções do projeto.
2. Revisar o diff para impedir mudanças em V1–V3, frontend e armazenamento de
   vídeo. Procurar segredos, senhas, tokens e links completos em código,
   fixtures e logs. Documentar variáveis de ambiente sem valores sensíveis.
3. Marcar o marco 2 do `ROADMAP.md` como concluído somente se todos os critérios
   da spec forem atendidos; informar limitações concretas no relatório final.
   Deixar mudanças sem commit para revisão.

## Pedido direto para o Claude Code

> Implemente `docs/acesso-spec.md` seguindo `docs/acesso-plano.md`. Preserve o
> catálogo e as migrações V1–V3. Avance por etapas, teste com PostgreSQL e
> Spring Security, rode `cd apps/api && ./mvnw test` e reporte decisões,
> arquivos alterados, testes e pendências. Não faça commit nem implemente telas,
> upload, vídeo ou progresso.
