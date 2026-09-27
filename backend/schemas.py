from pydantic import BaseModel
from typing import List
from datetime import datetime

# NOVO SCHEMA: Baseado na planilha e no desconto
class OfertaCreate(BaseModel):
    codprod: int
    desconto_percentual: float
    data_limite: datetime

class CompraCreate(BaseModel):
    oferta_id: int
    usuario_nome: str
    qtd_desejada: int

class CompraItemOut(BaseModel):
    id: int
    usuario_nome: str
    qtd_unidades: int
    valor_total: float
    data_reserva: datetime
    class Config:
        from_attributes = True

class FardoGrupoOut(BaseModel):
    id: int
    unidades_reservadas: int
    status: str
    itens: List[CompraItemOut] = []
    class Config:
        from_attributes = True

class OfertaOut(BaseModel):
    id: int
    produto_nome: str
    qtd_por_fardo: int
    preco_fardo: float
    preco_unidade: float
    data_limite: datetime
    status: str
    fardos: List[FardoGrupoOut] = []
    class Config:
        from_attributes = True