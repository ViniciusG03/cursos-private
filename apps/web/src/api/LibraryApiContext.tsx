import { createContext, useContext } from 'react'
import type { ReactNode } from 'react'
import type { LibraryApi } from './LibraryApi'

const LibraryApiContext = createContext<LibraryApi | null>(null)

/**
 * Injeta a implementação da API na árvore (real em main.tsx, fake nos testes).
 *
 * Exemplo: `<LibraryApiProvider api={new FakeLibraryApi()}>...</LibraryApiProvider>`.
 */
export function LibraryApiProvider({ api, children }: { api: LibraryApi; children: ReactNode }) {
  return <LibraryApiContext.Provider value={api}>{children}</LibraryApiContext.Provider>
}

/**
 * API injetada pelo provider mais próximo.
 *
 * Exemplo: `const api = useLibraryApi()`.
 */
// oxlint-disable-next-line react/only-export-components -- hook acompanha o provider do mesmo contexto
export function useLibraryApi(): LibraryApi {
  const api = useContext(LibraryApiContext)
  if (api === null) {
    throw new Error('useLibraryApi requires <LibraryApiProvider>, got no provider above this component')
  }
  return api
}
