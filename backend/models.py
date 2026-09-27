from sqlalchemy import Column, Integer, String, Float, ForeignKey, DateTime
from sqlalchemy.orm import relationship
from datetime import datetime
from database import Base

class Oferta(Base):
    __tablename__ = "ofertas"

    id = Column(Integer, primary_key=True, index=True)
    produto_nome = Column(String, index=True)
    qtd_por_fardo = Column(Integer)
    preco_fardo = Column(Float)
    preco_unidade = Column(Float)
    data_limite = Column(DateTime)
    status = Column(String, default="ATIVA")  # ATIVA ou ENCERRADA

    fardos = relationship("FardoGrupo", back_populates="oferta")

class FardoGrupo(Base):
    __tablename__ = "fardos_grupos"

    id = Column(Integer, primary_key=True, index=True)
    oferta_id = Column(Integer, ForeignKey("ofertas.id"))
    unidades_reservadas = Column(Integer, default=0)
    status = Column(String, default="EM_ANDAMENTO")  # EM_ANDAMENTO, CONCLUIDO ou CANCELADO

    oferta = relationship("Oferta", back_populates="fardos")
    itens = relationship("CompraItem", back_populates="fardo_grupo")

class CompraItem(Base):
    __tablename__ = "compras_itens"

    id = Column(Integer, primary_key=True, index=True)
    fardo_grupo_id = Column(Integer, ForeignKey("fardos_grupos.id"))
    usuario_nome = Column(String)
    qtd_unidades = Column(Integer)
    valor_total = Column(Float)
    data_reserva = Column(DateTime, default=datetime.utcnow)

    fardo_grupo = relationship("FardoGrupo", back_populates="itens")