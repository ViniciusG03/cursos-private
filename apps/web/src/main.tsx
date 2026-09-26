import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { takeLinkToken, watchLinkTokenHashChanges } from './access/linkToken'
import { HttpClient } from './api/HttpClient'
import { HttpLibraryApi } from './api/LibraryApi'
import { LibraryApp } from './app/LibraryApp'
import './styles.css'

// Antes de tudo: tira #token da barra de endereço, antes de qualquer requisição ou renderização.
const linkToken = takeLinkToken(window.location, window.history)
watchLinkTokenHashChanges(window, () => window.location.reload())
const api = new HttpLibraryApi(new HttpClient((input, init) => window.fetch(input, init)))

const rootElement = document.getElementById('root')
if (rootElement === null) {
  throw new Error('index.html must contain <div id="root">, got no element with id "root"')
}

createRoot(rootElement).render(
  <StrictMode>
    <LibraryApp api={api} linkToken={linkToken} />
  </StrictMode>,
)
