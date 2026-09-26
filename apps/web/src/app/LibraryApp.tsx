import { BrowserRouter, Navigate, Route, Routes } from 'react-router'
import { LinkTokenProvider } from '../access/LinkTokenContext'
import type { CapturedLinkToken } from '../access/linkToken'
import { INVITATION_PATH, PASSWORD_RESET_CONFIRM_PATH } from '../access/linkToken'
import { LoginPage } from '../access/LoginPage'
import { RequestPasswordResetPage } from '../access/RequestPasswordResetPage'
import { TokenPasswordPage } from '../access/TokenPasswordPage'
import { INVITATION_FLOW, PASSWORD_RESET_FLOW } from '../access/tokenPasswordFlows'
import { AdminCourseEditorPage } from '../admin/AdminCourseEditorPage'
import { AdminCoursesPage } from '../admin/AdminCoursesPage'
import { AdminInvitationsPage } from '../admin/AdminInvitationsPage'
import type { LibraryApi } from '../api/LibraryApi'
import { LibraryApiProvider } from '../api/LibraryApiContext'
import { CourseDetailPage } from '../catalog/CourseDetailPage'
import { CourseListPage } from '../catalog/CourseListPage'
import { LessonPage } from '../catalog/LessonPage'
import { HomeRedirect, RequireAdmin, RequireSession } from '../session/RouteGuards'
import { SessionProvider } from '../session/SessionContext'
import { NotFoundContent } from '../ui/Feedback'
import { AppShell } from './AppShell'

interface LibraryAppProps {
  api: LibraryApi
  linkToken: CapturedLinkToken
}

/**
 * Raiz da SPA com dependências injetadas: API (real ou fake) e token já retirado da URL.
 *
 * Exemplo: `<LibraryApp api={api} linkToken={takeLinkToken(window.location, window.history)} />`.
 */
export function LibraryApp({ api, linkToken }: LibraryAppProps) {
  return (
    <LibraryApiProvider api={api}>
      <LinkTokenProvider value={linkToken}>
        <SessionProvider>
          <BrowserRouter>
            <LibraryRoutes />
          </BrowserRouter>
        </SessionProvider>
      </LinkTokenProvider>
    </LibraryApiProvider>
  )
}

function LibraryRoutes() {
  return (
    <Routes>
      <Route index element={<HomeRedirect />} />
      <Route path="entrar" element={<LoginPage />} />
      <Route path={INVITATION_PATH} element={<TokenPasswordPage key="invitation" flow={INVITATION_FLOW} />} />
      <Route path="recuperar-acesso" element={<RequestPasswordResetPage />} />
      <Route path={PASSWORD_RESET_CONFIRM_PATH} element={<TokenPasswordPage key="password-reset" flow={PASSWORD_RESET_FLOW} />} />
      <Route element={<RequireSession />}>
        <Route element={<AppShell />}>
          <Route path="cursos" element={<CourseListPage />} />
          <Route path="cursos/:courseId" element={<CourseDetailPage />} />
          <Route path="cursos/:courseId/aulas/:lessonId" element={<LessonPage />} />
          <Route path="admin" element={<RequireAdmin />}>
            <Route index element={<Navigate to="/admin/cursos" replace />} />
            <Route path="cursos" element={<AdminCoursesPage />} />
            <Route path="cursos/:courseId" element={<AdminCourseEditorPage />} />
            <Route path="convites" element={<AdminInvitationsPage />} />
            <Route path="*" element={<NotFoundContent backTo="/admin/cursos" backLabel="Voltar à administração" />} />
          </Route>
        </Route>
      </Route>
      <Route path="*" element={<StandaloneNotFound />} />
    </Routes>
  )
}

function StandaloneNotFound() {
  return (
    <main className="standalone">
      <NotFoundContent backTo="/" backLabel="Ir para o início" />
    </main>
  )
}
