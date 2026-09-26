import type { HttpClient } from './HttpClient'
import type {
  CourseOutline,
  CourseSummary,
  CreatedResource,
  CurrentUser,
  InvitationView,
  NewCourse,
  TokenPassword,
} from './types'

/**
 * Operações da API usadas pela SPA, uma por endpoint de docs/acesso-api.md. As telas dependem só
 * desta interface; os testes usam `FakeLibraryApi`.
 *
 * Exemplo: `const outline = await api.readCourse(courseId)`.
 */
export interface LibraryApi {
  currentUser(): Promise<CurrentUser>
  login(email: string, password: string): Promise<CurrentUser>
  logout(): Promise<void>
  acceptInvitation(request: TokenPassword): Promise<void>
  requestPasswordReset(email: string): Promise<void>
  confirmPasswordReset(request: TokenPassword): Promise<void>
  listCourses(): Promise<CourseSummary[]>
  readCourse(courseId: string): Promise<CourseOutline>
  createCourse(course: NewCourse): Promise<CreatedResource>
  publishCourse(courseId: string): Promise<void>
  appendModule(courseId: string, title: string): Promise<CreatedResource>
  reorderModules(courseId: string, orderedIds: string[]): Promise<void>
  appendLesson(moduleId: string, title: string): Promise<CreatedResource>
  reorderLessons(moduleId: string, orderedIds: string[]): Promise<void>
  listInvitations(): Promise<InvitationView[]>
  issueInvitation(email: string): Promise<InvitationView>
}

const segment = encodeURIComponent

/**
 * Implementação real sobre {@link HttpClient}.
 *
 * Exemplo: `new HttpLibraryApi(new HttpClient(fetch.bind(window)))`.
 */
export class HttpLibraryApi implements LibraryApi {
  private readonly client: HttpClient

  constructor(client: HttpClient) {
    this.client = client
  }

  currentUser(): Promise<CurrentUser> {
    return this.client.getJson('/api/auth/me')
  }

  login(email: string, password: string): Promise<CurrentUser> {
    const form = new URLSearchParams({ email, password })
    return this.client.writeJson('POST', '/api/auth/login', { form })
  }

  logout(): Promise<void> {
    return this.client.writeWithoutBody('POST', '/api/auth/logout', null)
  }

  acceptInvitation(request: TokenPassword): Promise<void> {
    return this.client.writeWithoutBody('POST', '/api/auth/invitations/accept', { json: request })
  }

  requestPasswordReset(email: string): Promise<void> {
    return this.client.writeWithoutBody('POST', '/api/auth/password-resets/request', { json: { email } })
  }

  confirmPasswordReset(request: TokenPassword): Promise<void> {
    return this.client.writeWithoutBody('POST', '/api/auth/password-resets/confirm', { json: request })
  }

  listCourses(): Promise<CourseSummary[]> {
    return this.client.getJson('/api/courses')
  }

  readCourse(courseId: string): Promise<CourseOutline> {
    return this.client.getJson(`/api/courses/${segment(courseId)}`)
  }

  createCourse(course: NewCourse): Promise<CreatedResource> {
    return this.client.writeJson('POST', '/api/admin/courses', { json: course })
  }

  publishCourse(courseId: string): Promise<void> {
    return this.client.writeWithoutBody('POST', `/api/admin/courses/${segment(courseId)}/publish`, null)
  }

  appendModule(courseId: string, title: string): Promise<CreatedResource> {
    return this.client.writeJson('POST', `/api/admin/courses/${segment(courseId)}/modules`, { json: { title } })
  }

  reorderModules(courseId: string, orderedIds: string[]): Promise<void> {
    const path = `/api/admin/courses/${segment(courseId)}/modules/order`
    return this.client.writeWithoutBody('PUT', path, { json: { ids: orderedIds } })
  }

  appendLesson(moduleId: string, title: string): Promise<CreatedResource> {
    return this.client.writeJson('POST', `/api/admin/modules/${segment(moduleId)}/lessons`, { json: { title } })
  }

  reorderLessons(moduleId: string, orderedIds: string[]): Promise<void> {
    const path = `/api/admin/modules/${segment(moduleId)}/lessons/order`
    return this.client.writeWithoutBody('PUT', path, { json: { ids: orderedIds } })
  }

  listInvitations(): Promise<InvitationView[]> {
    return this.client.getJson('/api/admin/invitations')
  }

  issueInvitation(email: string): Promise<InvitationView> {
    return this.client.writeJson('POST', '/api/admin/invitations', { json: { email } })
  }
}
