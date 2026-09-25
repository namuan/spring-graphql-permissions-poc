import React from 'react'
import ReactDOM from 'react-dom/client'
import { keycloak } from './auth/keycloak'
import App from './App'
import './styles.css'

const root = document.getElementById('root')!

keycloak.init({
  onLoad: 'login-required',
  checkLoginIframe: false,
  pkceMethod: 'S256'
}).then(() => {
  ReactDOM.createRoot(root).render(
    <React.StrictMode>
      <App />
    </React.StrictMode>
  )
})
