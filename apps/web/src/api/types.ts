// Modelos do contrato HTTP descrito em docs/acesso-api.md. Os nomes dos campos seguem a API.

export type UserRole = 'ADMIN' | 'MEMBER'

export interface CurrentUser {
  id: string
  email: string
  role: UserRole
}

export interface CsrfToken {
  headerName: string
  parameterName: string
  token: string
}

export type CourseStatus = 'DRAFT' | 'PUBLISHED'

export interface CourseSummary {
  id: string
  title: string
  description: string | null
  status: CourseStatus
}

export interface LessonOutline {
  id: string
  title: string
  position: number
}

export interface ModuleOutline {
  id: string
  title: string
  position: number
  lessons: LessonOutline[]
}

export interface CourseOutline extends CourseSummary {
  modules: ModuleOutline[]
}

export interface CreatedResource {
  id: string
}

export type InvitationDeliveryStatus = 'PENDING' | 'SENT' | 'FAILED'

export interface InvitationView {
  id: string
  email: string
  createdAt: string
  expiresAt: string
  deliveryStatus: InvitationDeliveryStatus
  deliveryAttemptedAt: string | null
  deliveredAt: string | null
}

export interface NewCourse {
  title: string
  description: string | null
}

/** Corpo de aceite de convite e de confirmação de recuperação: token do link e nova senha. */
export interface TokenPassword {
  token: string
  password: string
}
