#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Extract ADR Tabla A (XLS) and Tabla B (DOCX) and create SQLite database.
"""
import xlrd
import zipfile
import xml.etree.ElementTree as ET
import sqlite3
import os
import re
from pathlib import Path

def clean_text(text):
    """Clean text: normalize whitespace, fix encoding issues."""
    if text is None:
        return ''
    # Replace multiple whitespace with single space
    text = re.sub(r'\s+', ' ', str(text))
    # Fix common encoding issues from xlrd (latin-1 / cp1252 misread as utf-8)
    # Common mis-encoded characters:
    text = text.replace('\u00e1', 'á').replace('\u00e9', 'é').replace('\u00ed', 'í').replace('\u00f3', 'ó').replace('\u00fa', 'ú')
    text = text.replace('\u00c1', 'Á').replace('\u00c9', 'É').replace('\u00cd', 'Í').replace('\u00d3', 'Ó').replace('\u00da', 'Ú')
    text = text.replace('\u00f1', 'ñ').replace('\u00d1', 'Ñ')
    text = text.replace('\u00bf', '¿').replace('\u00a1', '¡')
    # Additional fixes for common mojibake patterns (latin-1 bytes read as utf-8)
    text = text.replace('Ã¡', 'á').replace('Ã©', 'é').replace('Ã­', 'í').replace('Ã³', 'ó').replace('Ãº', 'ú')
    text = text.replace('Ã', 'Á').replace('Ã‰', 'É').replace('Ã', 'Í').replace('Ã"', 'Ó').replace('Ãš', 'Ú')
    text = text.replace('Ã±', 'ñ').replace('Ã‘', 'Ñ')
    text = text.replace('Ã¼', 'ü').replace('Ãœ', 'Ü')
    # Fix specific patterns seen in the data (e.g., AM�NICO -> AMONICO)
    text = text.replace('�', '')
    return text.strip()

def normalize_text(text):
    """Normalize text for search: remove accents, lowercase, trim."""
    if text is None:
        return ''
    # Remove accents
    import unicodedata
    text = unicodedata.normalize('NFD', str(text))
    text = ''.join(c for c in text if unicodedata.category(c) != 'Mn')
    # Lowercase and trim
    text = text.lower().strip()
    # Collapse whitespace
    text = re.sub(r'\s+', ' ', text)
    return text

def extract_tabla_a(xls_path):
    """Extract data from Tabla A XLS file."""
    wb = xlrd.open_workbook(xls_path)
    sheet = wb.sheet_by_index(0)
    
    # Column indices based on the header structure
    cols = {
        'numero_onu': 0,
        'nombre': 1,
        'clase': 2,
        'codigo_clasificacion': 3,
        'grupo_embalaje': 4,
        'etiquetas': 5,
        'disposiciones_especiales': 6,
        'cantidades_limitadas': 7,
        'cantidades_exceptuadas': 8,
        'instrucciones_embalaje': 9,
        'disposiciones_embalaje': 10,
        'embalaje_comun': 11,
        'instrucciones_transporte': 12,
        'disposiciones_cisterna': 13,
        'codigo_cisterna': 14,
        'disposiciones_cisterna_esp': 15,
        'vehiculos_cisterna': 16,
        'categoria_transporte': 17,
        'disposiciones_transporte_bultos': 18,
        'disposiciones_transporte_granel': 19,
        'disposiciones_carga_descarga': 20,
        'disposiciones_explotacion': 21,
        'numero_peligro': 22,
    }
    
    data = []
    for row_idx in range(4, sheet.nrows):  # Data starts at row 4
        row = {}
        for col_name, col_idx in cols.items():
            val = sheet.cell_value(row_idx, col_idx)
            row[col_name] = clean_text(val)
        
        # Skip empty rows
        if not row['numero_onu'] and not row['nombre']:
            continue
            
        # Add normalized name for search
        row['nombre_normalized'] = normalize_text(row['nombre'])
            
        data.append(row)
    
    print(f"Tabla A: extracted {len(data)} rows")
    return data

def extract_tabla_b(docx_path):
    """Extract data from Tabla B DOCX file."""
    z = zipfile.ZipFile(docx_path)
    root = ET.fromstring(z.read('word/document.xml'))
    ns = {'w': 'http://schemas.openxmlformats.org/wordprocessingml/2006/main'}
    trs = root.findall('.//w:tbl/w:tr', ns)
    
    data = []
    for i, tr in enumerate(trs):
        if i == 0:  # Skip header
            continue
        cells = tr.findall('./w:tc', ns)
        if len(cells) < 4:
            continue
        vals = [clean_text(' '.join(''.join(tc.itertext()).split())) for tc in cells[:4]]
        row = {
            'nombre': vals[0],
            'numero_onu': vals[1],
            'clase': vals[2],
            'nota': vals[3],
        }
        if not row['nombre'] and not row['numero_onu']:
            continue
        # Add normalized name for search
        row['nombre_normalized'] = normalize_text(row['nombre'])
        data.append(row)
    
    print(f"Tabla B: extracted {len(data)} rows")
    return data

def create_database(tabla_a, tabla_b, db_path):
    """Create SQLite database with both tables."""
    # Remove existing database
    if os.path.exists(db_path):
        os.remove(db_path)
    
    conn = sqlite3.connect(db_path)
    cursor = conn.cursor()
    
    # Create Tabla A
    cursor.execute('''
        CREATE TABLE adr_tabla_a (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            numero_onu TEXT NOT NULL,
            nombre TEXT NOT NULL,
            nombre_normalized TEXT NOT NULL,
            clase TEXT,
            codigo_clasificacion TEXT,
            grupo_embalaje TEXT,
            etiquetas TEXT,
            disposiciones_especiales TEXT,
            cantidades_limitadas TEXT,
            cantidades_exceptuadas TEXT,
            instrucciones_embalaje TEXT,
            disposiciones_embalaje TEXT,
            embalaje_comun TEXT,
            instrucciones_transporte TEXT,
            disposiciones_cisterna TEXT,
            codigo_cisterna TEXT,
            disposiciones_cisterna_esp TEXT,
            vehiculos_cisterna TEXT,
            categoria_transporte TEXT,
            disposiciones_transporte_bultos TEXT,
            disposiciones_transporte_granel TEXT,
            disposiciones_carga_descarga TEXT,
            disposiciones_explotacion TEXT,
            numero_peligro TEXT
        )
    ''')
    
    # Create Tabla B
    cursor.execute('''
        CREATE TABLE adr_tabla_b (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            nombre TEXT NOT NULL,
            nombre_normalized TEXT NOT NULL,
            numero_onu TEXT NOT NULL,
            clase TEXT,
            nota TEXT
        )
    ''')
    
    # Create indexes
    cursor.execute('CREATE INDEX idx_tabla_a_onu ON adr_tabla_a(numero_onu)')
    cursor.execute('CREATE INDEX idx_tabla_a_nombre ON adr_tabla_a(nombre)')
    cursor.execute('CREATE INDEX idx_tabla_a_nombre_normalized ON adr_tabla_a(nombre_normalized)')
    cursor.execute('CREATE INDEX idx_tabla_a_categoria ON adr_tabla_a(categoria_transporte)')
    cursor.execute('CREATE INDEX idx_tabla_b_onu ON adr_tabla_b(numero_onu)')
    cursor.execute('CREATE INDEX idx_tabla_b_nombre ON adr_tabla_b(nombre)')
    cursor.execute('CREATE INDEX idx_tabla_b_nombre_normalized ON adr_tabla_b(nombre_normalized)')
    
    # Insert Tabla A data
    for row in tabla_a:
        cursor.execute('''
            INSERT INTO adr_tabla_a (
                numero_onu, nombre, nombre_normalized, clase, codigo_clasificacion, grupo_embalaje,
                etiquetas, disposiciones_especiales, cantidades_limitadas, cantidades_exceptuadas,
                instrucciones_embalaje, disposiciones_embalaje, embalaje_comun,
                instrucciones_transporte, disposiciones_cisterna, codigo_cisterna,
                disposiciones_cisterna_esp, vehiculos_cisterna, categoria_transporte,
                disposiciones_transporte_bultos, disposiciones_transporte_granel,
                disposiciones_carga_descarga, disposiciones_explotacion, numero_peligro
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        ''', (
            row['numero_onu'], row['nombre'], row['nombre_normalized'], row['clase'], row['codigo_clasificacion'],
            row['grupo_embalaje'], row['etiquetas'], row['disposiciones_especiales'],
            row['cantidades_limitadas'], row['cantidades_exceptuadas'],
            row['instrucciones_embalaje'], row['disposiciones_embalaje'], row['embalaje_comun'],
            row['instrucciones_transporte'], row['disposiciones_cisterna'], row['codigo_cisterna'],
            row['disposiciones_cisterna_esp'], row['vehiculos_cisterna'], row['categoria_transporte'],
            row['disposiciones_transporte_bultos'], row['disposiciones_transporte_granel'],
            row['disposiciones_carga_descarga'], row['disposiciones_explotacion'], row['numero_peligro']
        ))
    
    # Insert Tabla B data
    for row in tabla_b:
        cursor.execute('''
            INSERT INTO adr_tabla_b (nombre, nombre_normalized, numero_onu, clase, nota)
            VALUES (?, ?, ?, ?, ?)
        ''', (row['nombre'], row['nombre_normalized'], row['numero_onu'], row['clase'], row['nota']))
    
    conn.commit()
    
    # Verify
    cursor.execute('SELECT COUNT(*) FROM adr_tabla_a')
    count_a = cursor.fetchone()[0]
    cursor.execute('SELECT COUNT(*) FROM adr_tabla_b')
    count_b = cursor.fetchone()[0]
    cursor.execute('SELECT COUNT(DISTINCT numero_onu) FROM adr_tabla_a')
    unique_onu_a = cursor.fetchone()[0]
    cursor.execute('SELECT COUNT(DISTINCT numero_onu) FROM adr_tabla_b')
    unique_onu_b = cursor.fetchone()[0]
    
    print(f"Database created: {db_path}")
    print(f"Tabla A rows: {count_a}, unique ONU: {unique_onu_a}")
    print(f"Tabla B rows: {count_b}, unique ONU: {unique_onu_b}")
    
    # Check some sample data
    cursor.execute('SELECT numero_onu, nombre, categoria_transporte FROM adr_tabla_a WHERE categoria_transporte != "" LIMIT 5')
    print("\nSample Tabla A with transport category:")
    for r in cursor.fetchall():
        print(f"  {r}")
    
    cursor.execute('SELECT numero_onu, nombre, nota FROM adr_tabla_b WHERE nota != "" LIMIT 5')
    print("\nSample Tabla B with notes:")
    for r in cursor.fetchall():
        print(f"  {r}")
    
    conn.close()

def main():
    base = Path(__file__).parent
    xls_path = base / 'Assets' / 'tabla_a_adr_2025_boe.xls'
    docx_path = base / 'Assets' / 'tabla_b_adr_2025.docx'
    db_path = base / 'app' / 'src' / 'main' / 'assets' / 'adr_2025.db'
    
    print("Extracting Tabla A from XLS...")
    tabla_a = extract_tabla_a(xls_path)
    
    print("Extracting Tabla B from DOCX...")
    tabla_b = extract_tabla_b(docx_path)
    
    print("Creating SQLite database...")
    create_database(tabla_a, tabla_b, db_path)
    
    print("Done!")

if __name__ == '__main__':
    main()