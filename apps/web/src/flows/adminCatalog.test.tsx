import { screen, waitFor, within } from '@testing-library/react'
import { beforeEach, describe, expect, it } from 'vitest'
import { FakeLibraryApi, httpError } from '../test/FakeLibraryApi'
import { ADMIN_EMAIL, PASSWORD, publishedCourseFixture, renderLibraryApp } from '../test/renderLibraryApp'

let api: FakeLibraryApi

beforeEach(async () => {
  api = new FakeLibraryApi()
  api.addAccount('ADMIN', ADMIN_EMAIL, PASSWORD)
  await api.login(ADMIN_EMAIL, PASSWORD)
})

function draftFixture() {
  const course = publishedCourseFixture()
  return api.addCourse({ ...course, title: 'Curso rascunho', status: 'DRAFT' })
}

function moduleTitles(): string[] {
  return screen.getAllByRole('heading', { level: 3 }).map((heading) => heading.textContent ?? '')
}

describe('admin routes', () => {
  // Regressão: /admin sem subrota renderizava o Outlet vazio (página em branco dentro da moldura).
  it('sends bare /admin to the course organizer', async () => {
    renderLibraryApp(api, '/admin')

    expect(await screen.findByRole('heading', { name: 'Organizar cursos' })).toBeInTheDocument()
    expect(window.location.pathname).toBe('/admin/cursos')
  })

  it('shows 404 for unknown admin subroutes', async () => {
    renderLibraryApp(api, '/admin/nao-existe')

    expect(await screen.findByRole('heading', { name: 'Página não encontrada' })).toBeInTheDocument()
  })
})

describe('admin course list', () => {
  it('shows drafts and published with status in text', async () => {
    api.addCourse(publishedCourseFixture())
    draftFixture()
    renderLibraryApp(api, '/admin/cursos')

    const draftCard = (await screen.findByRole('link', { name: 'Curso rascunho' })).closest('li')!
    expect(within(draftCard).getByText('Rascunho')).toBeInTheDocument()
    const publishedCard = screen.getByRole('link', { name: 'Java moderno' }).closest('li')!
    expect(within(publishedCard).getByText('Publicado')).toBeInTheDocument()
  })

  it('creates a draft with null description and opens it', async () => {
    const { user } = renderLibraryApp(api, '/admin/cursos')
    await user.type(await screen.findByLabelText('Título'), '  Kotlin  ')

    await user.click(screen.getByRole('button', { name: 'Criar rascunho' }))

    expect(await screen.findByRole('heading', { name: 'Kotlin', level: 1 })).toBeInTheDocument()
    expect(api.callsTo('createCourse')[0].args).toEqual([{ title: 'Kotlin', description: null }])
    expect(window.location.pathname).toBe(`/admin/cursos/${api.courses[0].id}`)
  })

  it('validates a blank title before calling the API', async () => {
    const { user } = renderLibraryApp(api, '/admin/cursos')
    await user.type(await screen.findByLabelText('Título'), '   ')

    await user.click(screen.getByRole('button', { name: 'Criar rascunho' }))

    expect(screen.getByLabelText('Título')).toHaveAccessibleDescription('Informe um título.')
    expect(api.callsTo('createCourse')).toHaveLength(0)
  })
})

describe('admin course editor', () => {
  it('adds a module and a lesson to a draft', async () => {
    const course = api.addCourse({ title: 'Novo', description: null, status: 'DRAFT', modules: [] })
    const { user } = renderLibraryApp(api, `/admin/cursos/${course.id}`)

    await user.type(await screen.findByLabelText('Novo módulo'), 'Fundamentos')
    await user.click(screen.getByRole('button', { name: 'Adicionar módulo' }))
    await user.type(await screen.findByLabelText('Nova aula em "Fundamentos"'), 'Primeira aula')
    await user.click(screen.getByRole('button', { name: 'Adicionar aula' }))

    expect(await screen.findByText('1. Primeira aula')).toBeInTheDocument()
    expect(screen.getByRole('status')).toHaveTextContent('Aula "Primeira aula" adicionada')
    expect(api.courses[0].modules[0].lessons.map((lesson) => lesson.title)).toEqual(['Primeira aula'])
  })

  it('reorders modules by keyboard, sending the complete list and keeping focus on the moved item', async () => {
    const course = draftFixture()
    const { user } = renderLibraryApp(api, `/admin/cursos/${course.id}`)
    const moveDown = await screen.findByRole('button', { name: 'Descer módulo Introdução' })
    moveDown.focus()

    await user.keyboard('{Enter}')

    await waitFor(() => expect(moduleTitles()).toEqual(['1. Records', '2. Introdução']))
    expect(api.callsTo('reorderModules')[0].args).toEqual([course.id, ['module-records', 'module-intro']])
    await waitFor(() => expect(screen.getByRole('button', { name: 'Subir módulo Introdução' })).toHaveFocus())
    expect(screen.getByRole('status')).toHaveTextContent('"Introdução" agora está na posição 2.')
  })

  it('reorders lessons inside their module', async () => {
    const course = draftFixture()
    const { user } = renderLibraryApp(api, `/admin/cursos/${course.id}`)

    await user.click(await screen.findByRole('button', { name: 'Subir aula Olá, mundo' }))

    await screen.findByText('1. Olá, mundo')
    expect(api.callsTo('reorderLessons')[0].args).toEqual(['module-intro', ['lesson-hello', 'lesson-setup']])
  })

  it('reloads the server order when a reorder fails', async () => {
    const course = draftFixture()
    api.failNext('reorderModules', httpError(400, 'expected the complete list of ids'))
    const { user } = renderLibraryApp(api, `/admin/cursos/${course.id}`)

    await user.click(await screen.findByRole('button', { name: 'Descer módulo Introdução' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('A ordem salva foi recarregada')
    expect(moduleTitles()).toEqual(['1. Introdução', '2. Records'])
    await waitFor(() => expect(api.callsTo('readCourse')).toHaveLength(2))
  })

  it('publishes only after explicit confirmation', async () => {
    const course = draftFixture()
    const { user } = renderLibraryApp(api, `/admin/cursos/${course.id}`)

    await user.click(await screen.findByRole('button', { name: 'Publicar curso…' }))
    expect(api.callsTo('publishCourse')).toHaveLength(0)
    await user.click(screen.getByRole('button', { name: 'Confirmar publicação' }))

    expect(await screen.findByText(/Curso publicado: membros já podem vê-lo/)).toBeInTheDocument()
    expect(screen.queryByLabelText('Novo módulo')).not.toBeInTheDocument()
    expect(screen.getByText(/não aceita novos módulos/)).toBeInTheDocument()
    expect(screen.getByLabelText('Nova aula em "Introdução"')).toBeInTheDocument()
  })

  it('explains a 409 on publish with module titles', async () => {
    const course = api.addCourse({
      title: 'Incompleto',
      description: null,
      status: 'DRAFT',
      modules: [{ id: 'module-empty', title: 'Módulo vazio', position: 1, lessons: [] }],
    })
    const { user } = renderLibraryApp(api, `/admin/cursos/${course.id}`)

    await user.click(await screen.findByRole('button', { name: 'Publicar curso…' }))
    await user.click(screen.getByRole('button', { name: 'Confirmar publicação' }))

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'O curso ainda não pode ser publicado. Todo módulo precisa de ao menos uma aula. Sem aulas: Módulo vazio.',
    )
    expect(screen.getByText('Rascunho')).toBeInTheDocument()
  })

  it('does not send a second publish while the first is pending', async () => {
    const course = draftFixture()
    const release = api.hold('publishCourse')
    const { user } = renderLibraryApp(api, `/admin/cursos/${course.id}`)
    await user.click(await screen.findByRole('button', { name: 'Publicar curso…' }))

    await user.click(screen.getByRole('button', { name: 'Confirmar publicação' }))
    expect(screen.getByRole('button', { name: 'Publicando…' })).toBeDisabled()
    await user.click(screen.getByRole('button', { name: 'Publicando…' }))
    release()

    await screen.findByText(/Curso publicado: membros já podem vê-lo/)
    expect(api.callsTo('publishCourse')).toHaveLength(1)
  })

  it('shows the 403 explanation for a refused write', async () => {
    const course = draftFixture()
    api.failNext('appendModule', httpError(403, 'access denied'))
    const { user } = renderLibraryApp(api, `/admin/cursos/${course.id}`)

    await user.type(await screen.findByLabelText('Novo módulo'), 'Extra')
    await user.click(screen.getByRole('button', { name: 'Adicionar módulo' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Atualize a página e tente de novo')
    expect(api.callsTo('appendModule')).toHaveLength(1)
  })

  it('shows 404 for an unknown course', async () => {
    renderLibraryApp(api, '/admin/cursos/00000000-0000-4000-8000-999999999999')

    expect(await screen.findByRole('heading', { name: 'Página não encontrada' })).toBeInTheDocument()
  })
})
