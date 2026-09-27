import gspread
from google.oauth2.service_account import Credentials

SCOPES = [
    "https://www.googleapis.com/auth/spreadsheets",
    "https://www.googleapis.com/auth/drive"
]
CREDENTIALS_FILE = "credentials.json"

def testar_conexao():
    print("🔄 Autenticando com o Google...")
    try:
        creds = Credentials.from_service_account_file(CREDENTIALS_FILE, scopes=SCOPES)
        client = gspread.authorize(creds)
        print("✅ Autenticação realizada com sucesso!")

        # Nome exato da planilha no Google Drive
        NOME_DA_PLANILHA = "PRODUTO"
        print(f"🔄 Abrindo a planilha '{NOME_DA_PLANILHA}'...")
        
        spreadsheet = client.open(NOME_DA_PLANILHA)
        sheet = spreadsheet.sheet1

        # Obtém todos os valores como lista de linhas
        todas_linhas = sheet.get_all_values()

        if not todas_linhas:
            print("⚠️ A planilha foi encontrada, mas está vazia.")
            return

        cabecalho = todas_linhas[0]
        linhas_dados = todas_linhas[1:]

        print(f"\n🎉 SUCESSO! Conexão estabelecida com o Google Sheets!")
        print(f"📊 Colunas do cabeçalho: {cabecalho}")
        print(f"📦 Total de produtos/linhas carregados: {len(linhas_dados)}")
        print("--------------------------------------------------")
        print("Exemplo dos primeiros produtos:")
        for idx, linha in enumerate(linhas_dados[:3], start=1):
            print(f"Produto {idx}: {linha}")
        print("--------------------------------------------------")

    except gspread.exceptions.SpreadsheetNotFound:
        print(f"\n❌ ERRO: A planilha '{NOME_DA_PLANILHA}' não foi encontrada no Google Drive.")
        print("💡 Verifique se o nome exato no Drive é 'PRODUTO' e se você a compartilhou com o e-mail da Service Account.")
    except Exception as e:
        print(f"\n❌ ERRO DURANTE O TESTE: {e}")

if __name__ == "__main__":
    testar_conexao()