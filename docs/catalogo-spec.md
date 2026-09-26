# Spec — catálogo de cursos (marco 1)

## Contexto e objetivo

Base: commit `e7c207b`. O Spring Boot, PostgreSQL, Flyway e Testcontainers já
estão configurados. `V1__create_courses.sql` e `V2__create_modules.sql` estão
commitadas. Este trabalho conclui o marco 1 do `ROADMAP.md`: representar cursos,
módulos e aulas em ordem manual, com cursos incompletos mantidos como rascunho.

## Escopo

- Criar `V3__create_lessons.sql`.
- Mapear o catálogo em entidades JPA e repositórios.
- Implementar operações de organização no domínio/aplicação, sem camada HTTP.
- Testar regras de negócio e restrições do esquema em PostgreSQL via Testcontainers.
- Atualizar o marco 1 do roadmap somente após cumprir os critérios de aceite.

Fora do escopo: frontend, controllers REST, contas, convites, autorização, upload,
armazenamento e reprodução de vídeos, progresso individual e implantação. Não
adicionar campos de vídeo ou progresso a `lessons` nesta etapa.

## Modelo de dados

### Cursos — V1 existente

`courses`: UUID como chave primária, título obrigatório (`VARCHAR(250)`), descrição
opcional e estado `DRAFT` ou `PUBLISHED` com padrão `DRAFT`. Não alterar a V1 já
aplicada. Títulos iguais são permitidos.

### Módulos — V2 existente

`modules`: UUID como chave primária, `course_id` obrigatório com FK para
`courses`, título obrigatório (`VARCHAR(200)`) e `position` inteira positiva.
`(course_id, position)` é único. Não alterar a V2 já aplicada. Módulos de cursos
diferentes podem ocupar a mesma posição.

### Aulas — V3 nova

`lessons`: UUID como chave primária, gerado por padrão no PostgreSQL;
`module_id` UUID obrigatório com FK para `modules`; `title VARCHAR(200) NOT NULL`;
`position INT NOT NULL` com `CHECK (position > 0)` e unicidade de
`(module_id, position)`. Não usar exclusão em cascata. Títulos iguais são
permitidos. A posição pertence ao módulo, não ao curso.

Uma aula poderá ter zero ou um vídeo no futuro, mas nenhum metadado de vídeo faz
parte da V3. O progresso futuro será por usuário e aula; não pertence a `lessons`.

## Modelo Java

- Entidades `Course`, `CourseModule` e `Lesson` com IDs `UUID`; usar
  `CourseModule` para evitar ambiguidade com `java.lang.Module`.
- Enum `CourseStatus` com valores `DRAFT` e `PUBLISHED`, persistido como texto.
- Associação unidirecional `CourseModule -> Course` e `Lesson -> CourseModule`,
  com carregamento lazy. Não criar cascatas JPA nem coleções bidirecionais apenas
  para ler a hierarquia; consultar filhos explicitamente e em ordem.
- Mapear nomes, tipos e restrições de modo compatível com `ddl-auto: validate`.
  Os defaults do banco permanecem válidos para inserções SQL diretas; a aplicação
  define explicitamente o estado inicial e gera IDs UUID de forma consistente.

## Regras de aplicação

1. Criar curso sempre em `DRAFT`; descrição pode estar ausente. Títulos fornecidos
   pela aplicação não podem ser vazios ou só espaços.
2. Adicionar módulo apenas a um curso existente e aula apenas a um módulo
   existente. Novos itens entram no final: posição 1 no primeiro item, depois
   `max(position) + 1` dentro do mesmo pai.
3. Listar módulos e aulas em `position` crescente. IDs e títulos não definem a
   ordem. Rascunhos podem ter zero módulos ou módulos sem aulas.
4. Reordenar módulos ou aulas recebendo a lista completa de IDs do mesmo pai, na
   ordem desejada. Rejeitar IDs ausentes, duplicados, de outro pai ou listas
   parciais; a operação inválida não altera posições. Uma operação válida deixa
   posições contíguas de 1 a N e preserva os IDs.
5. Publicar é uma transição explícita. Rejeitar publicação quando não há módulos
   ou quando algum módulo não tem aulas. Essa é a completude **estrutural** do
   catálogo; a futura regra de vídeo pronto será acrescentada antes de permitir
   consumo público. Publicar não acontece automaticamente ao criar uma aula.
6. Não implementar exclusão nesta entrega. As FKs permanecem sem `CASCADE`.

As operações que mudam várias posições ou publicam um curso devem ser
transacionais. A implementação de reordenação deve respeitar a unicidade imediata
já presente em V2 e V3; uma troca direta de posições pode falhar no PostgreSQL.
Usar posições temporárias sem colisão e fazer o flush entre as duas fases, ou
outra estratégia comprovada por teste real no PostgreSQL. Não editar migrações
já aplicadas para contornar o problema.

## Critérios de aceite

- `./mvnw test` passa em `apps/api` com PostgreSQL via Testcontainers.
- Flyway aplica V1, V2 e V3; Hibernate valida os mapeamentos.
- O banco rejeita aula sem módulo existente, posição não positiva e duas aulas
  na mesma posição do mesmo módulo; permite posição igual em módulos distintos.
- Um curso sem módulos e um curso com módulo sem aulas continuam em `DRAFT` e
  não podem ser publicados.
- Um curso com pelo menos um módulo e uma aula por módulo pode ser publicado.
- Consultas retornam módulos e aulas na ordem manual; reordenação real, inclusive
  troca de duas posições, persiste após recarregar do banco.
- Falhas de validação de reordenação não deixam atualização parcial.
