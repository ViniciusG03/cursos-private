import { ApiError } from '../api/ApiError'
import type { LibraryApi } from '../api/LibraryApi'
import type {
  CourseOutline,
  CourseSummary,
  CreatedResource,
  CurrentUser,
  InvitationView,
  NewCourse,
  TokenPassword,
  UserRole,
} from '../api/types'

/** Chamada registrada pela fake: operação, argumentos e o fragmento da URL naquele instante. */
export interface RecordedCall {
  operation: keyof LibraryApi
  args: unknown[]
  hashAtCall: string
}

interface FakeAccount {
  user: CurrentUser
  password: string
}

/**
 * API em memória com as regras de docs/acesso-api.md relevantes para a SPA: sessão, papéis, 404 de
 * rascunho para MEMBER, 409 de publicação/módulo, lista completa na reordenação e convites.
 * Permite programar falhas e segurar chamadas para testar estados pendentes.
 *
 * Exemplo: `const api = new FakeLibraryApi(); api.addAccount('ADMIN', 'a@x.com', 'senha longa 123')`.
 */
export class FakeLibraryApi implements LibraryApi {
  readonly calls: RecordedCall[] = []
  readonly courses: CourseOutline[] = []
  readonly invitations: InvitationView[] = []
  readonly validInvitationTokens = new Map<string, string>()
  /** Token de recuperação → e-mail da conta cuja senha ele troca. */
  readonly validResetTokens = new Map<string, string>()
  /** Resultado do envio de e-mail dos próximos convites (FAILED simula SMTP fora do ar). */
  deliveryOutcome: InvitationView['deliveryStatus'] = 'SENT'
  private readonly accounts: FakeAccount[] = []
  private readonly failures = new Map<keyof LibraryApi, ApiError[]>()
  private readonly holds = new Map<keyof LibraryApi, Promise<void>>()
  private sessionUser: CurrentUser | null = null
  private nextId = 1

  addAccount(role: UserRole, email: string, password: string): CurrentUser {
    const user = { id: this.generateId(), email, role }
    this.accounts.push({ user, password })
    return user
  }

  signInAs(user: CurrentUser): void {
    this.sessionUser = user
  }

  /** Simula sessão derrubada no servidor (expiração ou troca de senha). */
  dropSession(): void {
    this.sessionUser = null
  }

  failNext(operation: keyof LibraryApi, error: ApiError): void {
    this.failures.set(operation, [...(this.failures.get(operation) ?? []), error])
  }

  /** Segura a próxima chamada da operação até `release()`. */
  hold(operation: keyof LibraryApi): () => void {
    let release = () => {}
    this.holds.set(operation, new Promise<void>((resolve) => (release = resolve)))
    return () => {
      this.holds.delete(operation)
      release()
    }
  }

  callsTo(operation: keyof LibraryApi): RecordedCall[] {
    return this.calls.filter((call) => call.operation === operation)
  }

  addCourse(course: Omit<CourseOutline, 'id'> & { id?: string }): CourseOutline {
    const created = { ...course, id: course.id ?? this.generateId() }
    this.courses.push(created)
    return created
  }

  async currentUser(): Promise<CurrentUser> {
    await this.enter('currentUser', [])
    return this.requireSession()
  }

  async login(email: string, password: string): Promise<CurrentUser> {
    await this.enter('login', [email, password])
    const account = this.accounts.find((candidate) => candidate.user.email === email && candidate.password === password)
    if (account === undefined) throw httpError(401, 'invalid email or password')
    this.sessionUser = account.user
    return account.user
  }

  async logout(): Promise<void> {
    await this.enter('logout', [])
    this.requireSession()
    this.sessionUser = null
  }

  async acceptInvitation(request: TokenPassword): Promise<void> {
    await this.enter('acceptInvitation', [request])
    const email = this.validInvitationTokens.get(request.token)
    if (email === undefined) throw httpError(400, 'invalid or expired token')
    this.validInvitationTokens.delete(request.token)
    this.addAccount('MEMBER', email, request.password)
  }

  async requestPasswordReset(email: string): Promise<void> {
    await this.enter('requestPasswordReset', [email])
    if (!email.includes('@')) throw httpError(400, 'email must be like name@example.com')
  }

  async confirmPasswordReset(request: TokenPassword): Promise<void> {
    await this.enter('confirmPasswordReset', [request])
    const account = this.accounts.find((candidate) => candidate.user.email === this.validResetTokens.get(request.token))
    if (account === undefined) throw httpError(400, 'invalid or expired token')
    this.validResetTokens.delete(request.token)
    account.password = request.password
    // Como credential_version na API: sessões abertas com a senha antiga deixam de valer.
    if (this.sessionUser?.id === account.user.id) this.sessionUser = null
  }

  async listCourses(): Promise<CourseSummary[]> {
    await this.enter('listCourses', [])
    const reader = this.requireSession()
    return this.courses
      .filter((course) => reader.role === 'ADMIN' || course.status === 'PUBLISHED')
      .map(({ id, title, description, status }) => ({ id, title, description, status }))
      .sort((left, right) => left.title.localeCompare(right.title))
  }

  async readCourse(courseId: string): Promise<CourseOutline> {
    await this.enter('readCourse', [courseId])
    const reader = this.requireSession()
    const course = this.findCourse(courseId)
    if (reader.role !== 'ADMIN' && course.status !== 'PUBLISHED') throw httpError(404, 'course not found')
    return structuredClone(course)
  }

  async createCourse(course: NewCourse): Promise<CreatedResource> {
    await this.enterAsAdmin('createCourse', [course])
    return { id: this.addCourse({ ...course, status: 'DRAFT', modules: [] }).id }
  }

  async publishCourse(courseId: string): Promise<void> {
    await this.enterAsAdmin('publishCourse', [courseId])
    const course = this.findCourse(courseId)
    if (course.modules.length === 0 || course.modules.some((module) => module.lessons.length === 0)) {
      throw httpError(409, `course ${courseId} cannot be published`)
    }
    course.status = 'PUBLISHED'
  }

  async appendModule(courseId: string, title: string): Promise<CreatedResource> {
    await this.enterAsAdmin('appendModule', [courseId, title])
    const course = this.findCourse(courseId)
    if (course.status !== 'DRAFT') throw httpError(409, 'cannot append module to published course')
    const module = { id: this.generateId(), title, position: course.modules.length + 1, lessons: [] }
    course.modules.push(module)
    return { id: module.id }
  }

  async reorderModules(courseId: string, orderedIds: string[]): Promise<void> {
    await this.enterAsAdmin('reorderModules', [courseId, orderedIds])
    const course = this.findCourse(courseId)
    course.modules = reorderByIds(course.modules, orderedIds)
  }

  async appendLesson(moduleId: string, title: string): Promise<CreatedResource> {
    await this.enterAsAdmin('appendLesson', [moduleId, title])
    const module = this.findModule(moduleId)
    const lesson = { id: this.generateId(), title, position: module.lessons.length + 1 }
    module.lessons.push(lesson)
    return { id: lesson.id }
  }

  async reorderLessons(moduleId: string, orderedIds: string[]): Promise<void> {
    await this.enterAsAdmin('reorderLessons', [moduleId, orderedIds])
    const module = this.findModule(moduleId)
    module.lessons = reorderByIds(module.lessons, orderedIds)
  }

  async listInvitations(): Promise<InvitationView[]> {
    await this.enterAsAdmin('listInvitations', [])
    return structuredClone(this.invitations)
  }

  async issueInvitation(email: string): Promise<InvitationView> {
    await this.enterAsAdmin('issueInvitation', [email])
    if (this.accounts.some((account) => account.user.email === email)) throw httpError(409, 'account exists')
    const view = fakeInvitation(this.generateId(), email, this.deliveryOutcome)
    const existing = this.invitations.findIndex((invitation) => invitation.email === email)
    if (existing >= 0) this.invitations.splice(existing, 1)
    this.invitations.push(view)
    return view
  }

  private async enter(operation: keyof LibraryApi, args: unknown[]): Promise<void> {
    this.calls.push({ operation, args, hashAtCall: window.location.hash })
    await this.holds.get(operation)
    const failure = this.failures.get(operation)?.shift()
    if (failure !== undefined) throw failure
  }

  private async enterAsAdmin(operation: keyof LibraryApi, args: unknown[]): Promise<void> {
    await this.enter(operation, args)
    if (this.requireSession().role !== 'ADMIN') throw httpError(403, 'access denied')
  }

  private requireSession(): CurrentUser {
    if (this.sessionUser === null) throw httpError(401, 'authentication required')
    return this.sessionUser
  }

  private findCourse(courseId: string): CourseOutline {
    const course = this.courses.find((candidate) => candidate.id === courseId)
    if (course === undefined) throw httpError(404, `course ${courseId} not found`)
    return course
  }

  private findModule(moduleId: string) {
    const module = this.courses.flatMap((course) => course.modules).find((candidate) => candidate.id === moduleId)
    if (module === undefined) throw httpError(404, `module ${moduleId} not found`)
    return module
  }

  private generateId(): string {
    return `00000000-0000-4000-8000-${String(this.nextId++).padStart(12, '0')}`
  }
}

/**
 * Erro HTTP como o cliente real produziria.
 *
 * Exemplo: `api.failNext('login', httpError(429, 'too many', 30))`.
 */
export function httpError(status: number, detail: string, retryAfterSeconds: number | null = null): ApiError {
  return new ApiError('http', status, detail, retryAfterSeconds)
}

/**
 * Convite de exemplo com validade de 72 h a partir de agora.
 *
 * Exemplo: `fakeInvitation('1', 'ana@x.com', 'FAILED')`.
 */
export function fakeInvitation(id: string, email: string, status: InvitationView['deliveryStatus']): InvitationView {
  const now = Date.now()
  return {
    id,
    email,
    createdAt: new Date(now).toISOString(),
    expiresAt: new Date(now + 72 * 3600 * 1000).toISOString(),
    deliveryStatus: status,
    deliveryAttemptedAt: new Date(now).toISOString(),
    deliveredAt: status === 'SENT' ? new Date(now).toISOString() : null,
  }
}

function reorderByIds<T extends { id: string; position: number }>(items: T[], orderedIds: string[]): T[] {
  const sameSet = orderedIds.length === items.length && items.every((item) => orderedIds.includes(item.id))
  if (!sameSet) throw httpError(400, 'expected the complete list of ids')
  return orderedIds.map((id, index) => ({ ...items.find((item) => item.id === id)!, position: index + 1 }))
}
