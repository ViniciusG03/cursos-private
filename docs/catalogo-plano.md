# Plano de implementação — catálogo de cursos

Implementar a [spec](catalogo-spec.md) sobre o estado atual do repositório. O
último commit verificado é `e7c207b`, com V1 e V2. Antes de editar, conferir
`git status`, `ROADMAP.md` e as migrações; preservar mudanças posteriores do
usuário. Não reescrever V1/V2: Flyway já pode tê-las aplicado em bancos locais.

## 1. Esquema de aulas

1. Criar `apps/api/src/main/resources/db/migration/V3__create_lessons.sql` com os
   campos e restrições da spec. Nomear FK e unique de forma específica.
2. Criar teste de integração de esquema com PostgreSQL/Testcontainers que prove
   FK, `CHECK`, unicidade por módulo e o caso permitido de mesma posição em
   módulos distintos. Usar classes fake nomeadas para I/O externo quando houver
   mocks; aqui o PostgreSQL real de teste é intencional.
3. Rodar `cd apps/api && ./mvnw test` e resolver falhas antes de avançar.

## 2. Mapeamentos e leitura ordenada

1. Criar `Course`, `CourseModule`, `Lesson` e `CourseStatus` em pacotes pequenos e
   previsíveis sob `dev.vinicius.cursos.api.catalog`.
2. Criar repositórios Spring Data JPA com consultas ordenadas por `position` e
   operações necessárias para contar filhos e obter a última posição.
3. Cobrir persistência, leitura ordenada e validação do esquema por Hibernate
   em testes com PostgreSQL. Evitar depender da ordem implícita do banco.

## 3. Regras de organização

1. Implementar serviços com métodos públicos focados em: criar rascunho,
   acrescentar módulo, acrescentar aula, ler hierarquia e publicar.
2. Validar pais inexistentes, títulos em branco e publicação incompleta. Erros
   devem indicar o ID/valor problemático e o formato ou condição esperada.
3. Adicionar reordenação de módulos e aulas com lista completa de IDs. Garantir
   transação e uma estratégia que não colida com as constraints unique durante
   swaps; cobrir troca, ordem inversa, lista parcial, ID repetido e ID de outro
   pai. Testar que a ordem sobrevive a `flush`/recarga e que falha faz rollback.
4. Não abrir endpoints HTTP nem criar telas nesta entrega.

## 4. Verificação e entrega

1. Rodar o formatador padrão aplicável e `cd apps/api && ./mvnw test` como único
   comando de testes do projeto. Manter arquivos com menos de 500 linhas,
   funções pequenas e tipagem explícita. Toda função nova precisa de teste;
   funções públicas precisam de docstring com intenção e exemplo de uso,
   conforme as instruções do projeto.
2. Revisar `git diff` para garantir que vídeo, progresso, autenticação, frontend
   e migrações já aplicadas não foram alterados.
3. Marcar o marco 1 do `ROADMAP.md` como concluído apenas se todos os critérios de
   aceite da spec estiverem cobertos; explicar no relatório final qualquer item
   pendente e não marcar conclusão parcial como completa.

## Pedido direto para o Claude Code

> Implemente `docs/catalogo-spec.md` seguindo as etapas de
> `docs/catalogo-plano.md`. Preserve V1/V2 e qualquer mudança atual do usuário.
> Faça V3, mapeamentos, serviços e testes; rode `cd apps/api && ./mvnw test` e
> reporte os resultados e decisões. Não avance para vídeo, frontend ou contas.
