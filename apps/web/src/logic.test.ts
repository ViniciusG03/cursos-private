import { describe, expect, it } from 'vitest'
import { takeLinkToken, watchLinkTokenHashChanges } from './access/linkToken'
import { countPasswordCharacters, validateNewPassword } from './access/passwordRules'
import { describePublishBlockers, moveId, validateCatalogTitle } from './admin/catalogRules'
import { describeExpiration } from './admin/invitationPresentation'
import { parseRetryAfter } from './api/ApiError'
import { locateLesson } from './catalog/lessonNavigation'
import { resolvePostLoginPath } from './session/postLoginPath'
import { fakeInvitation } from './test/FakeLibraryApi'
import { publishedCourseFixture } from './test/renderLibraryApp'
import { describeTooManyAttempts } from './ui/describeApiError'

describe('resolvePostLoginPath', () => {
  it.each([
    ['/cursos/abc', 'MEMBER', '/cursos/abc'],
    ['/admin/convites', 'ADMIN', '/admin/convites'],
    ['/admin/cursos', 'MEMBER', '/cursos'],
    ['//evil.example', 'ADMIN', '/admin/cursos'],
    ['https://evil.example/cursos', 'MEMBER', '/cursos'],
    ['/cursos//evil.example', 'MEMBER', '/cursos'],
    ['/convites/aceitar', 'MEMBER', '/cursos'],
    ['/entrar', 'ADMIN', '/admin/cursos'],
    [undefined, 'MEMBER', '/cursos'],
  ] as const)('resolves %s for %s to %s', (requested, role, expected) => {
    expect(resolvePostLoginPath(requested, role)).toBe(expected)
  })
})

describe('takeLinkToken', () => {
  it('reads the token and removes the fragment from the address bar', () => {
    window.history.replaceState(null, '', '/convites/aceitar#token=abc123')

    const captured = takeLinkToken(window.location, window.history)

    expect(captured).toEqual({ pathname: '/convites/aceitar', token: 'abc123' })
    expect(window.location.hash).toBe('')
    expect(window.location.pathname).toBe('/convites/aceitar')
  })

  it('ignores fragments outside the token routes', () => {
    window.history.replaceState(null, '', '/cursos#token=abc')

    expect(takeLinkToken(window.location, window.history).token).toBeNull()
    expect(window.location.hash).toBe('#token=abc')
  })

  it('clears a fragment without token and reports the link as missing', () => {
    window.history.replaceState(null, '', '/recuperar-acesso/nova-senha#outro=1')

    expect(takeLinkToken(window.location, window.history).token).toBeNull()
    expect(window.location.hash).toBe('')
  })
})

describe('watchLinkTokenHashChanges', () => {
  // Regressão: link colado na mesma aba de /convites/aceitar deixava #token na barra (sem nova carga).
  it('reloads when a token arrives by hash change on a token route, and only then', () => {
    let reloads = 0
    const stop = watchLinkTokenHashChanges(window, () => (reloads += 1))
    window.history.replaceState(null, '', '/convites/aceitar#token=novo')
    window.dispatchEvent(new HashChangeEvent('hashchange'))
    window.history.replaceState(null, '', '/cursos#token=x')
    window.dispatchEvent(new HashChangeEvent('hashchange'))
    stop()

    expect(reloads).toBe(1)
  })
})

describe('password rules', () => {
  it('counts code points like the API', () => {
    expect(countPasswordCharacters('😀😀😀😀😀😀😀😀😀😀😀😀')).toBe(12)
    expect(validateNewPassword('😀😀😀😀😀😀😀😀😀😀😀😀', '😀😀😀😀😀😀😀😀😀😀😀😀').password).toBeNull()
  })

  it('rejects short, too long, blank and mismatched passwords', () => {
    expect(validateNewPassword('curta', 'curta').password).toContain('tem 5')
    expect(validateNewPassword('a'.repeat(129), 'a'.repeat(129)).password).toContain('tem 129')
    expect(validateNewPassword(' '.repeat(12), ' '.repeat(12)).password).toContain('Informe')
    expect(validateNewPassword('frase comprida ok', 'frase comprida ok!').confirmation).not.toBeNull()
  })
})

describe('catalog rules', () => {
  it('validates titles like CatalogTitle', () => {
    expect(validateCatalogTitle('   ', 200)).toBe('Informe um título.')
    expect(validateCatalogTitle(` ${'a'.repeat(200)} `, 200)).toBeNull()
    expect(validateCatalogTitle('a'.repeat(251), 250)).toContain('tem 251')
  })

  it('moves an id and always returns the complete list', () => {
    expect(moveId(['a', 'b', 'c'], 2, 'up')).toEqual(['a', 'c', 'b'])
    expect(moveId(['a', 'b', 'c'], 0, 'down')).toEqual(['b', 'a', 'c'])
    expect(() => moveId(['a', 'b'], 0, 'up')).toThrow(RangeError)
  })

  it('names empty modules by title instead of UUID', () => {
    const course = { id: 'c', ...publishedCourseFixture() }
    course.modules[1].lessons = []

    expect(describePublishBlockers(course)).toEqual(['Todo módulo precisa de ao menos uma aula. Sem aulas: Records.'])
    expect(describePublishBlockers({ ...course, modules: [] })).toEqual(['O curso precisa de ao menos um módulo.'])
  })
})

describe('locateLesson', () => {
  it('finds module, positions and neighbours across modules', () => {
    const located = locateLesson({ id: 'c', ...publishedCourseFixture() }, 'lesson-hello')!

    expect(located.module.title).toBe('Introdução')
    expect([located.moduleNumber, located.lessonNumber]).toEqual([1, 2])
    expect([located.previous?.id, located.next?.id]).toEqual(['lesson-setup', 'lesson-records'])
  })

  it('returns null for a lesson from another course', () => {
    expect(locateLesson({ id: 'c', ...publishedCourseFixture() }, 'lesson-elsewhere')).toBeNull()
  })
})

describe('messages', () => {
  it('describes Retry-After in seconds or minutes', () => {
    expect(parseRetryAfter('30')).toBe(30)
    expect(parseRetryAfter('Wed, 21 Oct 2015 07:28:00 GMT')).toBeNull()
    expect(describeTooManyAttempts(30)).toContain('30 segundos')
    expect(describeTooManyAttempts(90)).toContain('2 minutos')
    expect(describeTooManyAttempts(null)).toContain('alguns minutos')
  })

  it('marks expired invitations in text', () => {
    const invitation = fakeInvitation('1', 'a@x.com', 'SENT')

    expect(describeExpiration(invitation, Date.now())).toMatch(/^Expira em/)
    expect(describeExpiration(invitation, Date.now() + 73 * 3600 * 1000)).toMatch(/^Expirado em/)
  })
})
