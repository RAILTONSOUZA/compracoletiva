// Configuração dinâmica da API para Web e Mobile Android (Capacitor/PWA)
const getApiBaseUrl = () => {
  if (import.meta.env.VITE_API_URL) {
    return import.meta.env.VITE_API_URL;
  }

  if (typeof window !== 'undefined' && window.location && window.location.hostname) {
    // Se acessado no navegador local do computador
    if (window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1') {
      return 'http://localhost:8080';
    }
    // Se acessado pelo navegador do celular na rede Wi-Fi
    return `http://${window.location.hostname}:8080`;
  }

  // Endereço fixo do servidor na rede para o aplicativo Android (Capacitor)
  return 'http://10.0.0.6:8080';
};

export const API_URL = getApiBaseUrl();
