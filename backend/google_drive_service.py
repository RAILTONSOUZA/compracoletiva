import gspread
from google.oauth2.service_account import Credentials

SCOPES = [
    "https://www.googleapis.com/auth/spreadsheets",
    "https://www.googleapis.com/auth/drive"
]
CREDENTIALS_FILE = "credentials.json"
NOME_PLANILHA = "PRODUTO"

class GoogleSheetsService:
    def __init__(self):
        try:
            self.creds = Credentials.from_service_account_file(CREDENTIALS_FILE, scopes=SCOPES)
            self.client = gspread.authorize(self.creds)
            print("✅ Conexão com Google Sheets inicializada no backend!")
        except Exception as e:
            print(f"❌ Erro ao inicializar Google Sheets API: {e}")

    def obter_todos_produtos(self):
        try:
            spreadsheet = self.client.open(NOME_PLANILHA)
            sheet = spreadsheet.sheet1
            produtos = sheet.get_all_records()
            return produtos
        except Exception as e:
            print(f"❌ Erro ao buscar produtos do Google Sheets: {e}")
            return []

    def obter_ofertas(self):
        try:
            spreadsheet = self.client.open(NOME_PLANILHA)
            try:
                sheet = spreadsheet.worksheet("OFERTAS")
            except gspread.exceptions.WorksheetNotFound:
                sheet = spreadsheet.add_worksheet(title="OFERTAS", rows="100", cols="10")
                sheet.append_row(["id", "codprod", "descricao", "preco_fardo", "qtd_fardo", "data_limite", "status"])
                return []
            return sheet.get_all_records()
        except Exception as e:
            print(f"❌ Erro ao buscar ofertas: {e}")
            return []

    def salvar_oferta(self, oferta_data):
        try:
            spreadsheet = self.client.open(NOME_PLANILHA)
            try:
                sheet = spreadsheet.worksheet("OFERTAS")
            except gspread.exceptions.WorksheetNotFound:
                sheet = spreadsheet.add_worksheet(title="OFERTAS", rows="100", cols="10")
                sheet.append_row(["id", "codprod", "descricao", "preco_fardo", "qtd_fardo", "data_limite", "status"])

            proximo_id = len(sheet.get_all_values())
            nova_linha = [
                proximo_id,
                oferta_data.get("codprod", ""),
                oferta_data.get("descricao", ""),
                oferta_data.get("preco_fardo", 0),
                oferta_data.get("qtd_fardo", 1),
                oferta_data.get("data_limite", ""),
                "ATIVA"
            ]
            sheet.append_row(nova_linha)
            return True
        except Exception as e:
            print(f"❌ Erro ao salvar oferta: {e}")
            return False

# 🚀 INSTÂNCIA GLOBAL (Permite o import direto no main.py)
sheets_service = GoogleSheetsService()