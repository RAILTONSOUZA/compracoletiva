// Configuração dinâmica da API para Web e Mobile Android (Capacitor/PWA)
const getApiBaseUrl = () => {
  if (import.meta.env.VITE_API_URL) {
    return import.meta.env.VITE_API_URL;
  }
  
  // Em dispositivo mobile com Capacitor ou rede local, pode-se apontar para o IP da máquina
  // Por padrão usa a porta 8080 do backend Spring Boot
  return 'http://localhost:8080';
};

export const API_URL = getApiBaseUrl();
