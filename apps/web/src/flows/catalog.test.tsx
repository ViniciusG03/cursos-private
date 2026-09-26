import { screen, within } from '@testing-library/react'
import { beforeEach, describe, expect, it } from 'vitest'
import { ApiError } from '../api/ApiError'
import { FakeLibraryApi, httpError } from '../test/FakeLibraryApi'
import { MEMBER_EMAIL, PASSWORD, publishedCourseFixture, renderLibraryApp } from '../test/renderLibraryApp'

let api: FakeLibraryApi

beforeEach(async () => {
  api = new FakeLibraryApi()
  api.addAccount('MEMBER', MEMBER_EMAIL, PASSWORD)
  await api.login(MEMBER_EMAIL, PASSWORD)
})

describe('member catalog', () => {
  it('lists only published courses', async () => {
    api.addCourse(publishedCourseFixture())
    api.addCourse({ title: 'Rascunho secreto', description: null, status: 'DRAFT', modules: [] })
    renderLibraryApp(api, '/cursos')

    expect(await screen.findByRole('link', { name: 'Java moderno' })).toBeInTheDocument()
    expect(screen.queryByText('Rascunho secreto')).not.toBeInTheDocument()
    expect(screen.queryByText('Publicado')).not.toBeInTheDocument()
  })

  it('has a distinct empty state', async () => {
    renderLibraryApp(api, '/cursos')

    expect(await screen.findByText('Nenhum curso disponível ainda')).toBeInTheDocument()
  })

  it('shows a network failure with retry, distinct from empty and 404', async () => {
    api.addCourse(publishedCourseFixture())
    api.failNext('listCourses', ApiError.network())
    const { user } = renderLibraryApp(api, '/cursos')

    expect(await screen.findByRole('alert')).toHaveTextContent('Não foi possível falar com o servidor')
    await user.click(screen.getByRole('button', { name: 'Tentar novamente' }))

    expect(await screen.findByRole('link', { name: 'Java moderno' })).toBeInTheDocument()
  })

  it('opens a course URL directly and keeps the API order of modules and lessons', async () => {
    const course = api.addCourse(publishedCourseFixture())
    renderLibraryApp(api, `/cursos/${course.id}`)

    const modules = await screen.findAllByRole('heading', { level: 2 })
    expect(modules.map((heading) => heading.textContent)).toEqual(['Introdução', 'Records'])
    const firstModule = modules[0].closest('li')!
    expect(within(firstModule).getAllByRole('link').map((link) => link.textContent)).toEqual([
      'Instalando o JDK',
      'Olá, mundo',
    ])
  })

  it('shows a course without lessons as such', async () => {
    const course = api.addCourse({ title: 'Vazio', description: null, status: 'PUBLISHED', modules: [] })
    renderLibraryApp(api, `/cursos/${course.id}`)

    expect(await screen.findByText('Este curso ainda não tem aulas')).toBeInTheDocument()
  })

  it('respects the 404 for a known draft URL', async () => {
    const draft = api.addCourse({ title: 'Rascunho secreto', description: null, status: 'DRAFT', modules: [] })
    renderLibraryApp(api, `/cursos/${draft.id}`)

    expect(await screen.findByRole('heading', { name: 'Página não encontrada' })).toBeInTheDocument()
    expect(screen.queryByText('Rascunho secreto')).not.toBeInTheDocument()
  })

  it('treats a malformed course id (API 400) as not found', async () => {
    api.failNext('readCourse', httpError(400, "parameter 'courseId' must be a valid UUID"))
    renderLibraryApp(api, '/cursos/nao-e-uuid')

    expect(await screen.findByRole('heading', { name: 'Página não encontrada' })).toBeInTheDocument()
  })
})

describe('lesson page', () => {
  it('shows lesson, module and position without player or progress', async () => {
    const course = api.addCourse(publishedCourseFixture())
    renderLibraryApp(api, `/cursos/${course.id}/aulas/lesson-hello`)

    expect(await screen.findByRole('heading', { name: 'Olá, mundo' })).toBeInTheDocument()
    expect(screen.getByText('Módulo 1 de 2: Introdução')).toBeInTheDocument()
    expect(screen.getByText('Aula 2 de 2 neste módulo')).toBeInTheDocument()
    expect(screen.getByRole('status')).toHaveTextContent('ainda não está disponível')
    expect(document.querySelector('video')).toBeNull()
    expect(screen.queryByRole('button', { name: /conclu/i })).not.toBeInTheDocument()
    expect(screen.getByRole('link', { name: /Próxima: Records na prática/ })).toBeInTheDocument()
  })

  it('shows 404 for a lesson that does not belong to the course', async () => {
    const course = api.addCourse(publishedCourseFixture())
    renderLibraryApp(api, `/cursos/${course.id}/aulas/outra-aula`)

    expect(await screen.findByRole('heading', { name: 'Página não encontrada' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Voltar ao curso' })).toHaveAttribute('href', `/cursos/${course.id}`)
  })
})
