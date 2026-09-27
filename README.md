# Comabel - Compra Coletiva 🛍️

Sistema completo de **Compra Coletiva** para empresas e colaboradores da **Comabel**.

---

## 🏗️ Arquitetura do Projeto

```text
compra_coletiva/
├── backend-spring/      # 🚀 Novo Backend em Java Spring Boot 3.3.4 (JPA, H2, Concorrência Segura, PDFs)
├── backend/             # 🐍 Backend legado em Python (FastAPI)
├── frontend/            # 💻 Frontend React + Vite + Tailwind + PWA + Capacitor Android
│   └── android/         # 📱 Projeto nativo Android gerado com Capacitor
├── iniciar-backend-spring.bat  # ▶️ Atalho para iniciar o Spring Boot no Windows
└── iniciar-frontend.bat        # ▶️ Atalho para iniciar o Frontend no Windows
```

---

## 🌟 O que mudou com o Java Spring Boot

1. **Concorrência Segura (Sem Race Conditions):** Uso de `@Transactional` com Pessimistic Lock (`LockModeType.PESSIMISTIC_WRITE`) no banco de dados para garantir que dois colaboradores reservando cotas no mesmo milissegundo nunca estourem o fardo.
2. **Persistência de Pedidos e Faturamento:** Os status dos pedidos faturados e cancelados agora são gravados na tabela `pedidos_consolidados`, eliminando a perda de dados ao reiniciar o servidor.
3. **Importação Automática do Excel:** Na primeira inicialização, o Spring Boot lê os arquivos `PRODUTO.xlsx` e `CLIENTE.xlsx` usando **Apache POI** e popula o banco de dados relacional.
4. **Encerramento Automático de Ofertas:** Rotina agendada com `@EnableScheduling` e `@Scheduled` rodando em background para encerrar ciclos expirados.
5. **Geração de PDFs:** Relatórios de estoque, compras por participante e comprovantes individuais gerados com **OpenPDF**.
6. **Console do Banco H2:** Acesso visual ao banco em `http://localhost:8080/h2-console` (JDBC URL: `jdbc:h2:file:./compra_coletiva_db`, Usuário: `sa`, Senha em branco).

---

## 🚀 Como Executar

### 1. Iniciar o Backend Java Spring Boot
Dê dois cliques em `iniciar-backend-spring.bat` ou pelo terminal:
```bash
cd backend-spring
.\mvnw.cmd spring-boot:run
```
O servidor estará ativo em: **`http://localhost:8080`**

### 2. Iniciar o Frontend Web
Dê dois cliques em `iniciar-frontend.bat` ou pelo terminal:
```bash
cd frontend
npm install
npm run dev
```
Acesse no navegador: **`http://localhost:5173`**

---

## 📱 Distribuição para Android

### Opção 1: PWA (Instalação Direta no Celular sem Loja)
1. Conecte o celular na mesma rede Wi-Fi do computador onde o backend e o frontend estão rodando.
2. Abra o Chrome no celular e acesse o endereço IP do seu computador na porta 5173 (ex: `http://192.168.1.100:5173`).
3. O navegador exibirá automaticamente a opção **"Adicionar à tela inicial"** ou **"Instalar aplicativo"**.
4. O app será instalado no Android como um aplicativo independente, em tela cheia, com ícone próprio e sem barra de navegação de navegador.

### Opção 2: Gerar APK Nativo Android com Capacitor
O projeto já conta com o Capacitor configurado e a pasta nativa `frontend/android`:

1. No diretório `frontend`, compile o projeto web e sincronize com a pasta Android:
   ```bash
   npm run cap:build
   ```
2. Abra o projeto no **Android Studio**:
   ```bash
   npm run cap:open
   ```
3. No Android Studio, clique em **Build > Build Bundle(s) / APK(s) > Build APK(s)** para gerar o arquivo `.apk` de instalação.
