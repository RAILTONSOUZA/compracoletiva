import io
import pandas as pd
from typing import List, Optional
from datetime import datetime
from fastapi import FastAPI, Depends, HTTPException, Response
from fastapi.middleware.cors import CORSMiddleware
from sqlalchemy.orm import Session
import models, schemas, database

# Dependências para geração de PDFs (ReportLab)
from reportlab.lib.pagesizes import letter
from reportlab.platypus import SimpleDocTemplate, Paragraph, Spacer, Table, TableStyle
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.lib import colors

models.Base.metadata.create_all(bind=database.engine)

app = FastAPI(title="API Compra Coletiva - Comabel")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

PEDIDOS_STATUS_DB = {}

# ==========================================
# 📊 LEITURA DAS PLANILHAS (PRODUTO E CLIENTE)
# ==========================================
try:
    df_produtos = pd.read_excel("PRODUTO.xlsx").fillna("")
    PRODUTOS_LIST = df_produtos.to_dict(orient="records")
    print(f"✅ {len(PRODUTOS_LIST)} produtos carregados de PRODUTO.xlsx!")
except Exception as e:
    print(f"⚠️ Erro ao carregar PRODUTO.xlsx: {e}")
    PRODUTOS_LIST = []

try:
    df_clientes = pd.read_excel("CLIENTE.xlsx").fillna("")
    df_clientes["CPFCNPJ_CLEAN"] = df_clientes["CPFCNPJ"].astype(str).str.replace(r"\D", "", regex=True).str.zfill(11)
    CLIENTES_LIST = df_clientes.to_dict(orient="records")
    print(f"✅ {len(CLIENTES_LIST)} clientes/CPFs carregados de CLIENTE.xlsx!")
except Exception as e:
    print(f"⚠️ Erro ao carregar CLIENTE.xlsx: {e}")
    CLIENTES_LIST = []

from google_drive_service import sheets_service

from fastapi import FastAPI, HTTPException
from google_drive_service import sheets_service

app = FastAPI(title="Comabel Compra Coletiva API")

@app.get("/produtos")
def listar_produtos():
    produtos = sheets_service.obter_todos_produtos()
    return {"status": "sucesso", "total": len(produtos), "produtos": produtos}

@app.get("/ofertas")
def listar_ofertas():
    ofertas = sheets_service.obter_ofertas()
    return {"status": "sucesso", "total": len(ofertas), "ofertas": ofertas}

@app.post("/ofertas")
def criar_oferta(oferta: dict):
    sucesso = sheets_service.salvar_oferta(oferta)
    if not sucesso:
        raise HTTPException(status_code=500, detail="Erro ao salvar oferta na nuvem.")
    return {"status": "sucesso", "mensagem": "Oferta cadastrada no Google Sheets!"}
# ==========================================
# 🔑 VALIDAÇÃO DE CPF/CNPJ
# ==========================================
@app.post("/login-cpf/")
def validar_cpf_cliente(payload: dict):
    cpf_input = str(payload.get("cpfcnpj", "")).strip()
    cpf_digits = "".join(filter(str.isdigit, cpf_input))
    cpf_clean = cpf_digits.zfill(11) if len(cpf_digits) <= 11 else cpf_digits

    if not cpf_clean:
        raise HTTPException(status_code=400, detail="Informe um CPF ou CNPJ válido.")

    cliente_encontrado = next(
        (c for c in CLIENTES_LIST if c["CPFCNPJ_CLEAN"] == cpf_clean or c["CPFCNPJ_CLEAN"] == cpf_digits), 
        None
    )

    if not cliente_encontrado:
        raise HTTPException(
            status_code=404, 
            detail="CPF/CNPJ não encontrado na base de dados da Comabel."
        )

    return {
        "valido": True,
        "codigo": cliente_encontrado.get("CODIGO"),
        "cpfcnpj": cliente_encontrado.get("CPFCNPJ_CLEAN")
    }

# ==========================================
# ⚙️ ENCERRAMENTO DE OFERTAS
# ==========================================
def verificar_e_fechar_ofertas_expiradas(db: Session):
    agora = datetime.now()
    ofertas_expiradas = db.query(models.Oferta).filter(
        models.Oferta.status == "ATIVA",
        models.Oferta.data_limite <= agora
    ).all()

    for oferta in ofertas_expiradas:
        oferta.status = "ENCERRADA"
        for fardo in oferta.fardos:
            if fardo.status == "EM_ANDAMENTO":
                fardo.status = "CANCELADO"
    
    if ofertas_expiradas:
        db.commit()

# ==========================================
# 🚀 ROTAS DE OFERTAS E COMPRAS
# ==========================================
@app.post("/ofertas/", response_model=schemas.OfertaOut)
def criar_oferta(oferta: schemas.OfertaCreate, db: Session = Depends(database.get_db)):
    produto_tabela = next((p for p in PRODUTOS_LIST if p["CODPROD"] == oferta.codprod), None)
    
    if not produto_tabela:
        raise HTTPException(status_code=404, detail="Produto não encontrado na base de dados.")
    
    nome = str(produto_tabela["DESCRICAO"]).strip()
    qtd_por_fardo = int(produto_tabela["QTUNITCX"])
    preco_tabela = float(produto_tabela["CXPTABELA"])
    
    preco_fardo_com_desconto = preco_tabela * (1 - (oferta.desconto_percentual / 100.0))
    preco_unidade = preco_fardo_com_desconto / qtd_por_fardo
    
    # Adiciona explicitamente o [CODPROD] junto com o nome do produto
    nome_com_codigo = f"[{oferta.codprod}] {nome}"

    nova_oferta = models.Oferta(
        produto_nome=nome_com_codigo,
        qtd_por_fardo=qtd_por_fardo,
        preco_fardo=preco_fardo_com_desconto,
        preco_unidade=preco_unidade,
        data_limite=oferta.data_limite
    )
    db.add(nova_oferta)
    db.commit()
    db.refresh(nova_oferta)

    primeiro_fardo = models.FardoGrupo(oferta_id=nova_oferta.id)
    db.add(primeiro_fardo)
    db.commit()
    
    return nova_oferta

@app.get("/ofertas/", response_model=List[schemas.OfertaOut])
def listar_ofertas(db: Session = Depends(database.get_db)):
    verificar_e_fechar_ofertas_expiradas(db)
    return db.query(models.Oferta).all()
@app.get("/minhas-compras-ativas/{usuario_nome}")
def minhas_compras_ativas(usuario_nome: str, db: Session = Depends(database.get_db)):
    """Retorna os fardos pendentes (que não foram faturados nem cancelados) nos quais o comprador participou."""
    verificar_e_fechar_ofertas_expiradas(db)

    pedidos_consolidados = listar_pedidos_consolidados(db)
    pedidos_finalizados_ids = [
        p["codigo_pedido"] for p in pedidos_consolidados 
        if p["status_pedido"] in ["FATURADO", "CANCELADO"]
    ]

    ofertas = db.query(models.Oferta).all()
    compras_ativas = []

    for oferta in ofertas:
        for fardo in oferta.fardos:
            if fardo.status in ["EM_ANDAMENTO", "CONCLUIDO"]:
                minhas_unidades = sum(
                    item.qtd_unidades for item in fardo.itens 
                    if item.usuario_nome.strip().lower() == usuario_nome.strip().lower()
                )

                if minhas_unidades > 0:
                    dt_key = oferta.data_limite.isoformat()
                    # Verifica se pertence a um pedido que já foi Faturado ou Cancelado
                    status_ped = "EM_ANDAMENTO"
                    for p in pedidos_consolidados:
                        if any(i["oferta_id"] == oferta.id for i in p["itens"]):
                            status_ped = p["status_pedido"]
                            break

                    if status_ped not in ["FATURADO", "CANCELADO"]:
                        faltam = oferta.qtd_por_fardo - fardo.unidades_reservadas
                        compras_ativas.append({
                            "oferta_id": oferta.id,
                            "produto_nome": oferta.produto_nome,
                            "qtd_por_fardo": oferta.qtd_por_fardo,
                            "minhas_unidades": minhas_unidades,
                            "unidades_reservadas": fardo.unidades_reservadas,
                            "faltam_unidades": max(0, faltam),
                            "preco_unidade": oferta.preco_unidade,
                            "valor_investido": minhas_unidades * oferta.preco_unidade,
                            "data_limite": oferta.data_limite,
                            "status_fardo": fardo.status
                        })

    return compras_ativas
# ==========================================
# 🗑️ REMOVER COMPRA DE OFERTA ATIVA
# ==========================================
@app.delete("/comprar/{oferta_id}/{usuario_nome}")
def cancelar_participacao_compra(oferta_id: int, usuario_nome: str, db: Session = Depends(database.get_db)):
    """Permite cancelar apenas fardos em andamento de ofertas ativas."""
    verificar_e_fechar_ofertas_expiradas(db)

    oferta = db.query(models.Oferta).filter(models.Oferta.id == oferta_id).first()
    if not oferta:
        raise HTTPException(status_code=404, detail="Oferta não encontrada.")

    if oferta.status != "ATIVA" or oferta.data_limite <= datetime.now():
        raise HTTPException(
            status_code=400, 
            detail="O ciclo desta oferta já foi encerrado. Não é mais possível cancelar."
        )

    itens_removidos = 0
    for fardo in oferta.fardos:
        # Apenas permite remover de fardos que AINDA NÃO FORAM FECHADOS (CONCLUÍDOS)
        if fardo.status == "EM_ANDAMENTO":
            compras_usuario = db.query(models.CompraItem).filter(
                models.CompraItem.fardo_grupo_id == fardo.id,
                models.CompraItem.usuario_nome.ilike(usuario_nome.strip())
            ).all()

            for item in compras_usuario:
                fardo.unidades_reservadas -= item.qtd_unidades
                db.delete(item)
                itens_removidos += 1

    if itens_removidos == 0:
        raise HTTPException(
            status_code=400, 
            detail="Este fardo já foi fechado/concluído ou a oferta foi encerrada. Não é possível excluir."
        )

    db.commit()
    return {"message": "Participação cancelada com sucesso!", "itens_removidos": itens_removidos}
@app.post("/comprar/")
def realizar_compra(compra: schemas.CompraCreate, db: Session = Depends(database.get_db)):
    verificar_e_fechar_ofertas_expiradas(db)

    oferta = db.query(models.Oferta).filter(models.Oferta.id == compra.oferta_id).first()
    if not oferta:
        raise HTTPException(status_code=404, detail="Oferta não encontrada")

    if oferta.status == "ENCERRADA" or oferta.data_limite <= datetime.now():
        raise HTTPException(status_code=400, detail="Esta oferta já expirou!")

    # ------------------------------------------------------------------
    # 🔒 NOVO BLOQUEIO: Limite global por CICLO DE OFERTA
    # ------------------------------------------------------------------
    limite_maximo_por_usuario = oferta.qtd_por_fardo - 1
    
    total_ja_comprado_na_oferta = 0
    for fardo in oferta.fardos:
        for item in fardo.itens:
            if item.usuario_nome.strip().lower() == compra.usuario_nome.strip().lower():
                total_ja_comprado_na_oferta += item.qtd_unidades

    if (total_ja_comprado_na_oferta + compra.qtd_desejada) > limite_maximo_por_usuario:
        restante_permitido = limite_maximo_por_usuario - total_ja_comprado_na_oferta
        if restante_permitido > 0:
            raise HTTPException(
                status_code=400, 
                detail=f"Você já possui {total_ja_comprado_na_oferta} un. neste ciclo. Só é permitido comprar mais {restante_permitido} un."
            )
        else:
            raise HTTPException(
                status_code=400, 
                detail=f"Você já atingiu o limite máximo de {limite_maximo_por_usuario} unidades para este ciclo de oferta!"
            )
    # ------------------------------------------------------------------

    qtd_pendente = compra.qtd_desejada

    while qtd_pendente > 0:
        fardo_atual = db.query(models.FardoGrupo).filter(
            models.FardoGrupo.oferta_id == compra.oferta_id,
            models.FardoGrupo.status == "EM_ANDAMENTO"
        ).first()

        if not fardo_atual:
            fardo_atual = models.FardoGrupo(oferta_id=compra.oferta_id)
            db.add(fardo_atual)
            db.commit()
            db.refresh(fardo_atual)

        vagas_fardo = oferta.qtd_por_fardo - fardo_atual.unidades_reservadas
        # Como o limite global já foi validado lá em cima, só precisamos respeitar as vagas do fardo
        alocar = min(qtd_pendente, vagas_fardo)

        item = models.CompraItem(
            fardo_grupo_id=fardo_atual.id,
            usuario_nome=compra.usuario_nome,
            qtd_unidades=alocar,
            valor_total=alocar * oferta.preco_unidade
        )
        db.add(item)

        fardo_atual.unidades_reservadas += alocar
        qtd_pendente -= alocar

        if fardo_atual.unidades_reservadas == oferta.qtd_por_fardo:
            fardo_atual.status = "CONCLUIDO"
            # Cria o próximo fardo se ainda houver unidades pendentes para o cliente
            if qtd_pendente > 0:
                novo_fardo = models.FardoGrupo(oferta_id=compra.oferta_id)
                db.add(novo_fardo)

        db.commit()

    return {"message": "Compra realizada com sucesso!"}
# ==========================================
# 📦 CONSULTA E FATURAMENTO
# ==========================================
@app.put("/pedidos/faturar/{codigo_pedido}")
def faturar_pedido(codigo_pedido: str):
    PEDIDOS_STATUS_DB[codigo_pedido] = "FATURADO"
    return {"message": f"Pedido {codigo_pedido} faturado com sucesso!", "status": "FATURADO"}

@app.put("/pedidos/cancelar/{codigo_pedido}")
def cancelar_pedido(codigo_pedido: str):
    PEDIDOS_STATUS_DB[codigo_pedido] = "CANCELADO"
    return {"message": f"Pedido {codigo_pedido} cancelado com sucesso!", "status": "CANCELADO"}

@app.get("/pedidos-consolidados/")
def listar_pedidos_consolidados(db: Session = Depends(database.get_db)):
    verificar_e_fechar_ofertas_expiradas(db)

    ofertas_encerradas = db.query(models.Oferta).filter(models.Oferta.status == "ENCERRADA").all()
    
    ciclos = {}
    for oferta in ofertas_encerradas:
        dt_key = oferta.data_limite.isoformat()
        if dt_key not in ciclos:
            ciclos[dt_key] = []
        ciclos[dt_key].append(oferta)

    pedidos = []
    idx = 1
    for dt_str, lista_ofertas in ciclos.items():
        dt_obj = datetime.fromisoformat(dt_str)
        codigo_pedido = f"PED-{dt_obj.strftime('%Y%m%d')}-{idx:03d}"
        
        total_fardos_fechados = 0
        valor_total_pedido = 0.0
        itens_ofertas = []

        for oferta in lista_ofertas:
            fardos_concluidos = [f for f in oferta.fardos if f.status == "CONCLUIDO"]
            if fardos_concluidos:
                q_fardos = len(fardos_concluidos)
                val_o = q_fardos * oferta.preco_fardo
                total_fardos_fechados += q_fardos
                valor_total_pedido += val_o

                fardos_detalhe = []
                for f in fardos_concluidos:
                    compradores = [{"nome": item.usuario_nome, "qtd": item.qtd_unidades, "total": item.valor_total} for item in f.itens]
                    fardos_detalhe.append({"id": f.id, "compradores": compradores})

                itens_ofertas.append({
                    "oferta_id": oferta.id,
                    "produto_nome": oferta.produto_nome,
                    "qtd_por_fardo": oferta.qtd_por_fardo,
                    "preco_fardo": oferta.preco_fardo,
                    "preco_unidade": oferta.preco_unidade,
                    "fardos_concluidos_count": q_fardos,
                    "fardos_detalhes": fardos_detalhe
                })

        if total_fardos_fechados > 0:
            status_atual = PEDIDOS_STATUS_DB.get(codigo_pedido, "ENCERRADO")
            pedidos.append({
                "codigo_pedido": codigo_pedido,
                "data_encerramento": dt_obj,
                "status_pedido": status_atual,
                "ref_oferta_id": lista_ofertas[0].id,
                "total_fardos": total_fardos_fechados,
                "valor_total": valor_total_pedido,
                "itens": itens_ofertas
            })
            idx += 1

    pedidos.sort(key=lambda x: x["data_encerramento"], reverse=True)
    return pedidos

@app.get("/meus-pedidos/{usuario_nome}")
def meus_pedidos_concluidos(usuario_nome: str, db: Session = Depends(database.get_db)):
    todos_pedidos = listar_pedidos_consolidados(db)
    pedidos_faturados = [p for p in todos_pedidos if p["status_pedido"] == "FATURADO"]
    
    pedidos_funcionario = []
    for ped in pedidos_faturados:
        itens_usuario = []
        total_pedido_usuario = 0.0

        for item_oferta in ped["itens"]:
            qtd_total_item = 0
            val_total_item = 0.0

            for fardo in item_oferta["fardos_detalhes"]:
                for comp in fardo["compradores"]:
                    if comp["nome"].strip().lower() == usuario_nome.strip().lower():
                        qtd_total_item += comp["qtd"]
                        val_total_item += comp["total"]

            if qtd_total_item > 0:
                itens_usuario.append({
                    "produto_nome": item_oferta["produto_nome"],
                    "qtd_unidades": qtd_total_item,
                    "preco_unidade": item_oferta["preco_unidade"],
                    "valor_total": val_total_item
                })
                total_pedido_usuario += val_total_item

        if itens_usuario:
            pedidos_funcionario.append({
                "codigo_pedido": ped["codigo_pedido"],
                "data_encerramento": ped["data_encerramento"],
                "status_pedido": ped["status_pedido"],
                "valor_total_usuario": total_pedido_usuario,
                "itens": itens_usuario
            })

    return pedidos_funcionario

# ==========================================
# 📄 GERAÇÃO DE PDFS (COM CÓDIGO DO PRODUTO)
# ==========================================

style_sheet = getSampleStyleSheet()
style_celula_nome = ParagraphStyle('CelulaNome', parent=style_sheet['Normal'], fontName='Helvetica', fontSize=8, leading=10)
style_celula_header = ParagraphStyle('CelulaHeader', parent=style_sheet['Normal'], fontName='Helvetica-Bold', fontSize=9, leading=11, textColor=colors.white)

@app.get("/pdf/pedido-geral/{oferta_id}")
def gerar_pdf_pedido_geral(oferta_id: int, db: Session = Depends(database.get_db)):
    pedidos = listar_pedidos_consolidados(db)
    pedido_ref = next((p for p in pedidos if any(item["oferta_id"] == oferta_id for item in p["itens"])), None)

    if not pedido_ref:
        raise HTTPException(status_code=404, detail="Pedido não encontrado.")

    codigo_pedido = pedido_ref["codigo_pedido"]
    data_encerramento_str = pedido_ref["data_encerramento"].strftime('%d/%m/%Y %H:%M')

    buffer = io.BytesIO()
    doc = SimpleDocTemplate(buffer, pagesize=letter, leftMargin=36, rightMargin=36, topMargin=36, bottomMargin=36)
    story = []

    story.append(Paragraph(f"<font color='#0052A5'><b>COMABEL - PEDIDO GERAL DE COMPRAS (ESTOQUE)</b></font>", style_sheet['Title']))
    story.append(Spacer(1, 8))
    story.append(Paragraph(f"<b>Número do Pedido:</b> <font color='#E30613'><b>{codigo_pedido}</b></font> ({pedido_ref['status_pedido']})", style_sheet['Heading2']))
    story.append(Paragraph(f"<b>Data de Encerramento:</b> {data_encerramento_str}", style_sheet['Normal']))
    story.append(Spacer(1, 15))

    data = [[
        Paragraph("<b>Código / Descrição do Produto</b>", style_celula_header),
        Paragraph("<b>Fardos Fechados</b>", style_celula_header),
        Paragraph("<b>Unidades Totais</b>", style_celula_header),
        Paragraph("<b>Preço Fardo</b>", style_celula_header),
        Paragraph("<b>Valor Total</b>", style_celula_header)
    ]]
    
    valor_total_pedido_geral = 0.0
    fardos_totais_ciclo = 0

    for item_oferta in pedido_ref["itens"]:
        q_fardos = item_oferta["fardos_concluidos_count"]
        unidades_totais = q_fardos * item_oferta["qtd_por_fardo"]
        valor_item_total = q_fardos * item_oferta["preco_fardo"]
        valor_total_pedido_geral += valor_item_total
        fardos_totais_ciclo += q_fardos

        data.append([
            Paragraph(f"<b>{item_oferta['produto_nome']}</b>", style_celula_nome),
            f"{q_fardos} fardo(s)",
            f"{unidades_totais} un",
            f"R$ {item_oferta['preco_fardo']:.2f}",
            f"R$ {valor_item_total:.2f}"
        ])

    data.append([
        Paragraph("<b>TOTAL GERAL DO PEDIDO</b>", style_celula_nome),
        f"{fardos_totais_ciclo} fardo(s)",
        "-",
        "-",
        f"R$ {valor_total_pedido_geral:.2f}"
    ])

    t = Table(data, colWidths=[230, 80, 70, 80, 80])
    t.setStyle(TableStyle([
        ('BACKGROUND', (0,0), (-1,0), colors.HexColor('#0052A5')),
        ('ALIGN', (1,0), (-1,-1), 'CENTER'),
        ('VALIGN', (0,0), (-1,-1), 'MIDDLE'),
        ('GRID', (0,0), (-1,-1), 0.5, colors.HexColor('#CBD5E1')),
        ('PADDING', (0,0), (-1,-1), 6),
        ('BACKGROUND', (0,-1), (-1,-1), colors.HexColor('#F1F5F9')),
        ('FONTNAME', (0,-1), (-1,-1), 'Helvetica-Bold'),
    ]))
    
    story.append(t)
    doc.build(story)
    buffer.seek(0)
    
    return Response(
        content=buffer.getvalue(), 
        media_type="application/pdf", 
        headers={"Content-Disposition": f"attachment; filename=Pedido_{codigo_pedido}_Estoque.pdf"}
    )

@app.get("/pdf/pedido-detalhado/{oferta_id}")
def gerar_pdf_pedido_detalhado(oferta_id: int, db: Session = Depends(database.get_db)):
    pedidos = listar_pedidos_consolidados(db)
    pedido_ref = next((p for p in pedidos if any(item["oferta_id"] == oferta_id for item in p["itens"])), None)

    if not pedido_ref:
        raise HTTPException(status_code=404, detail="Pedido não encontrado.")

    codigo_pedido = pedido_ref["codigo_pedido"]
    data_encerramento_str = pedido_ref["data_encerramento"].strftime('%d/%m/%Y %H:%M')

    buffer = io.BytesIO()
    doc = SimpleDocTemplate(buffer, pagesize=letter, leftMargin=36, rightMargin=36, topMargin=36, bottomMargin=36)
    story = []

    story.append(Paragraph(f"<font color='#0052A5'><b>COMABEL - RELATÓRIO DE COMPRAS POR PARTICIPANTE</b></font>", style_sheet['Title']))
    story.append(Spacer(1, 8))
    story.append(Paragraph(f"<b>Número do Pedido:</b> <font color='#E30613'><b>{codigo_pedido}</b></font> ({pedido_ref['status_pedido']})", style_sheet['Heading2']))
    story.append(Paragraph(f"<b>Data de Encerramento:</b> {data_encerramento_str}", style_sheet['Normal']))
    story.append(Spacer(1, 15))

    relatorio_funcionarios = {}

    for item_oferta in pedido_ref["itens"]:
        prod_nome = item_oferta["produto_nome"]
        preco_un = item_oferta["preco_unidade"]

        for fardo in item_oferta["fardos_detalhes"]:
            for comp in fardo["compradores"]:
                nome_func = comp["nome"]
                if nome_func not in relatorio_funcionarios:
                    relatorio_funcionarios[nome_func] = {}

                if prod_nome not in relatorio_funcionarios[nome_func]:
                    relatorio_funcionarios[nome_func][prod_nome] = {
                        "qtd": 0,
                        "preco_un": preco_un,
                        "total": 0.0
                    }

                relatorio_funcionarios[nome_func][prod_nome]["qtd"] += comp["qtd"]
                relatorio_funcionarios[nome_func][prod_nome]["total"] += comp["total"]

    for func_nome, produtos in relatorio_funcionarios.items():
        story.append(Paragraph(f"<font color='#E30613'><b>PARTICIPANTE: {func_nome.upper()}</b></font>", style_sheet['Heading2']))
        story.append(Spacer(1, 4))

        data_func = [[
            Paragraph("<b>Código / Descrição do Produto</b>", style_celula_header),
            Paragraph("<b>Qtd Comprada</b>", style_celula_header),
            Paragraph("<b>Preço Unid.</b>", style_celula_header),
            Paragraph("<b>Subtotal</b>", style_celula_header)
        ]]
        total_colaborador = 0.0

        for prod_nome, info in produtos.items():
            data_func.append([
                Paragraph(f"<b>{prod_nome}</b>", style_celula_nome),
                f"{info['qtd']} un",
                f"R$ {info['preco_un']:.2f}",
                f"R$ {info['total']:.2f}"
            ])
            total_colaborador += info['total']

        data_func.append([
            Paragraph("<b>TOTAL A PAGAR</b>", style_celula_nome),
            "-",
            "-",
            f"R$ {total_colaborador:.2f}"
        ])

        t_func = Table(data_func, colWidths=[260, 90, 90, 100])
        t_func.setStyle(TableStyle([
            ('BACKGROUND', (0,0), (-1,0), colors.HexColor('#0052A5')),
            ('ALIGN', (1,0), (-1,-1), 'CENTER'),
            ('VALIGN', (0,0), (-1,-1), 'MIDDLE'),
            ('GRID', (0,0), (-1,-1), 0.5, colors.HexColor('#CBD5E1')),
            ('PADDING', (0,0), (-1,-1), 6),
            ('BACKGROUND', (0,-1), (-1,-1), colors.HexColor('#F1F5F9')),
            ('FONTNAME', (0,-1), (-1,-1), 'Helvetica-Bold'),
        ]))
        
        story.append(t_func)
        story.append(Spacer(1, 15))

    doc.build(story)
    buffer.seek(0)
    
    return Response(
        content=buffer.getvalue(), 
        media_type="application/pdf", 
        headers={"Content-Disposition": f"attachment; filename=Relatorio_{codigo_pedido}_Participantes.pdf"}
    )

@app.get("/pdf/comprovante-funcionario/{codigo_pedido}/{usuario_nome}")
def gerar_pdf_comprovante_funcionario(codigo_pedido: str, usuario_nome: str, db: Session = Depends(database.get_db)):
    verificar_e_fechar_ofertas_expiradas(db)

    pedidos = listar_pedidos_consolidados(db)
    pedido_selecionado = next((p for p in pedidos if p["codigo_pedido"] == codigo_pedido), None)

    if not pedido_selecionado:
        raise HTTPException(status_code=404, detail="Pedido não encontrado.")

    dt_encerramento = pedido_selecionado["data_encerramento"]

    itens_usuario = []
    total_comprado = 0.0

    for item_oferta in pedido_selecionado["itens"]:
        qtd_total = 0
        val_total = 0.0
        for fardo in item_oferta["fardos_detalhes"]:
            for comprador in fardo["compradores"]:
                if comprador["nome"].strip().lower() == usuario_nome.strip().lower():
                    qtd_total += comprador["qtd"]
                    val_total += comprador["total"]

        if qtd_total > 0:
            itens_usuario.append({
                "produto": item_oferta["produto_nome"],
                "qtd": qtd_total,
                "preco_un": item_oferta["preco_unidade"],
                "total": val_total
            })
            total_comprado += val_total

    if not itens_usuario:
        raise HTTPException(status_code=404, detail="Nenhum item encontrado para este participante neste pedido.")

    buffer = io.BytesIO()
    doc = SimpleDocTemplate(buffer, pagesize=letter, leftMargin=36, rightMargin=36, topMargin=36, bottomMargin=36)
    story = []

    story.append(Paragraph(f"<font color='#0052A5'><b>COMABEL - COMPROVANTE DE COMPRA COLETIVA</b></font>", style_sheet['Title']))
    story.append(Spacer(1, 8))
    story.append(Paragraph(f"<b>Número do Pedido:</b> <font color='#E30613'><b>{codigo_pedido}</b></font> (FATURADO)", style_sheet['Heading2']))
    story.append(Paragraph(f"<b>Participante:</b> {usuario_nome.upper()}", style_sheet['Normal']))
    story.append(Paragraph(f"<b>Data Encerramento:</b> {dt_encerramento.strftime('%d/%m/%Y %H:%M')}", style_sheet['Normal']))
    story.append(Spacer(1, 15))

    data = [[
        Paragraph("<b>Código / Descrição do Produto</b>", style_celula_header),
        Paragraph("<b>Quantidade</b>", style_celula_header),
        Paragraph("<b>Preço Unid.</b>", style_celula_header),
        Paragraph("<b>Subtotal</b>", style_celula_header)
    ]]

    for item in itens_usuario:
        data.append([
            Paragraph(f"<b>{item['produto']}</b>", style_celula_nome),
            f"{item['qtd']} un",
            f"R$ {item['preco_un']:.2f}",
            f"R$ {item['total']:.2f}"
        ])

    data.append([
        Paragraph("<b>TOTAL A PAGAR</b>", style_celula_nome),
        "-",
        "-",
        f"R$ {total_comprado:.2f}"
    ])

    t = Table(data, colWidths=[260, 90, 90, 100])
    t.setStyle(TableStyle([
        ('BACKGROUND', (0,0), (-1,0), colors.HexColor('#0052A5')),
        ('ALIGN', (1,0), (-1,-1), 'CENTER'),
        ('VALIGN', (0,0), (-1,-1), 'MIDDLE'),
        ('GRID', (0,0), (-1,-1), 0.5, colors.HexColor('#CBD5E1')),
        ('PADDING', (0,0), (-1,-1), 6),
        ('BACKGROUND', (0,-1), (-1,-1), colors.HexColor('#F1F5F9')),
        ('FONTNAME', (0,-1), (-1,-1), 'Helvetica-Bold'),
    ]))

    story.append(t)
    doc.build(story)
    buffer.seek(0)

    nome_arquivo = f"Comprovante_{codigo_pedido}_{usuario_nome.replace(' ', '_')}.pdf"
    return Response(
        content=buffer.getvalue(), 
        media_type="application/pdf", 
        headers={"Content-Disposition": f"attachment; filename={nome_arquivo}"}
    )