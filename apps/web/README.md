# apps/web — SPA da biblioteca de cursos

React 19 + TypeScript + Vite + React Router. Interface em português para login, aceite de
convite, recuperação de acesso, catálogo (cursos, módulos e aulas) e administração do catálogo
e dos convites. O contrato HTTP está em [`docs/acesso-api.md`](../../docs/acesso-api.md).

Upload, player e progresso não fazem parte deste marco: a página da aula mostra só a posição no
curso e um aviso de conteúdo indisponível.

## Comandos

```sh
pnpm install          # dependências (o workspace usa pnpm; os scripts rodam com npm run)
npm run dev           # http://localhost:5173, com proxy de /api para http://localhost:8080
npm run build         # tsc -b + vite build
npm run lint          # oxlint
npm test              # vitest run (jsdom), não interativo
```

## Estrutura

| Pasta | Responsabilidade |
| --- | --- |
| `src/api` | Tipos do contrato, `HttpClient` (CSRF a cada escrita, 204/202 sem JSON, problem+json → `ApiError`) e `LibraryApi` |
| `src/session` | Sessão em memória (`/api/auth/me`), guardas de rota e destino seguro após login |
| `src/data` | Hooks de leitura (`useApiResource`) e de escrita sem duplo envio (`usePendingAction`) |
| `src/access` | Login, recuperação e páginas com token (`#token` é lido e apagado em `main.tsx`) |
| `src/catalog` | Lista de cursos, curso e aula |
| `src/admin` | Criação, módulos/aulas, reordenação por teclado, publicação e convites |
| `src/ui` | Layouts, formulários, avisos e estados de carregamento/vazio/erro |
| `src/test` | `FakeLibraryApi` (API em memória) e helpers; fluxos em `src/flows` |

## Sessão e segurança

- Sessão por cookie `HttpOnly` de mesma origem; nada de token, senha ou sessão em `localStorage`.
- Antes de cada `POST`/`PUT`, o cliente busca `GET /api/auth/csrf` e envia o token no cabeçalho
  indicado na resposta. Um 403 nunca é repetido automaticamente.
- Os links de e-mail trazem `#token=`. `main.tsx` lê o token e o remove da barra com
  `history.replaceState` antes de renderizar ou chamar a API. O token fica só na memória e vai
  apenas no corpo do `POST`. O documento declara `<meta name="referrer" content="no-referrer">` e
  não carrega recursos externos.

## Produção (Nginx)

Frontend e API compartilham a origem. O Nginx precisa:

```nginx
location /api/ {
    proxy_pass http://api:8080;
    proxy_set_header Host $host;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
}

location / {
    root /usr/share/nginx/html;
    # Fallback da SPA: /cursos/..., /admin/... e os links de e-mail abrem direto e sobrevivem à recarga.
    try_files $uri $uri/ /index.html;
    add_header Referrer-Policy no-referrer always;
}
```

## Verificação manual com API local e Mailpit

1. Suba PostgreSQL e Mailpit: `docker compose -f infra/compose.dev.yaml up -d`.
2. Na primeira vez, crie o ADMIN (veja "Bootstrap do primeiro administrador" em
   `docs/acesso-api.md`), com senha própria que não vai para o Git. Depois suba a API sem as
   variáveis de bootstrap: `cd apps/api && ./mvnw spring-boot:run -Dspring-boot.run.profiles=local`.
3. `cd apps/web && npm run dev` e abra <http://localhost:5173>.
4. Como ADMIN: crie um rascunho, adicione módulos e aulas, reordene com "Subir"/"Descer" (Tab +
   Enter), tente publicar com módulo vazio (409 explicado), complete e publique. Recarregue a
   página e confira a ordem.
5. Em "Convites", convide um e-mail. Abra <http://localhost:8025>, siga o link e confira que a
   barra de endereço perde o `#token`. Defina a senha, entre e confira que só aparecem cursos
   publicados; um rascunho aberto por URL mostra 404 e `/admin` mostra acesso negado.
6. Em "Esqueci minha senha", peça o link, abra-o pelo Mailpit e defina a nova senha. A sessão
   antiga cai (401) e o login volta à rota em que você estava.
