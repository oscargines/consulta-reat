#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Extract the ADR 2025 chapter 4.1 packaging instructions into SQLite."""
import re
import sqlite3
from pathlib import Path

from pypdf import PdfReader


ROOT = Path(__file__).parent
PDF_PATH = ROOT / "Assets" / "ADR 2025.pdf"
DB_PATH = ROOT / "app" / "src" / "main" / "assets" / "adr_embalajes_2025.db"

CODE = r"(?:P\d{3}(?:(?:\s+[a-z]|[a-z])\))?|IBC\d{2,3}|LP\d{2,3}|R\d{3})"
HEADING = re.compile(
    rf"^\s*(?P<code>{CODE})\s+INSTRUCCI[ÓO]N(?:ES)? DE EMBALAJE"
    rf"(?P<title>.*?)\s+(?P<repeat>{CODE})\s*$",
    re.IGNORECASE,
)


def canonical_code(value: str) -> str:
    value = re.sub(r"\(?\s*([a-z])\)", r"(\1)", value.strip(), flags=re.IGNORECASE)
    return value.upper()


def extract_instructions() -> list[dict[str, object]]:
    reader = PdfReader(PDF_PATH)
    instructions: dict[str, dict[str, object]] = {}
    current_code: str | None = None

    # PDF pages 402-512 contain ADR 4.1.4.1-4.1.4.3.
    for page_number in range(402, 513):
        lines = (reader.pages[page_number - 1].extract_text() or "").splitlines()
        if lines and re.fullmatch(r"\s*\d{3,4}\s*", lines[-1]):
            lines.pop()

        for line in lines:
            heading = HEADING.match(" ".join(line.split()))
            if heading:
                code = canonical_code(heading.group("code"))
                repeated_code = canonical_code(heading.group("repeat"))
                if code != repeated_code:
                    raise ValueError(
                        f"Instruction heading mismatch on PDF page {page_number}: "
                        f"{code} != {repeated_code}"
                    )

                record = instructions.setdefault(
                    code,
                    {
                        "code": code,
                        "title": heading.group("title").strip(" :-"),
                        "content": [],
                        "page_start": page_number - 2,
                        "page_end": page_number - 2,
                    },
                )
                record["page_end"] = page_number - 2
                current_code = code
                continue

            if current_code is not None:
                content = line.strip()
                if content:
                    instructions[current_code]["content"].append(content)
                    instructions[current_code]["page_end"] = page_number - 2

    if not instructions:
        raise ValueError("No packaging instructions found in ADR 2025 PDF")

    records = []
    for record in instructions.values():
        code = str(record["code"])
        section = "4.1.4.1" if code.startswith(("P", "R")) else (
            "4.1.4.2" if code.startswith("IBC") else "4.1.4.3"
        )
        records.append({
            **record,
            "title": str(record["title"]) or "Instrucción de embalaje",
            "content": "\n".join(record["content"]).strip(),
            "section": section,
        })
    return records


def create_database(records: list[dict[str, object]]) -> None:
    DB_PATH.parent.mkdir(parents=True, exist_ok=True)
    if DB_PATH.exists():
        DB_PATH.unlink()

    with sqlite3.connect(DB_PATH) as connection:
        connection.execute("""
            CREATE TABLE adr_instrucciones_embalaje (
                codigo TEXT PRIMARY KEY COLLATE NOCASE,
                titulo TEXT NOT NULL,
                contenido TEXT NOT NULL,
                seccion_adr TEXT NOT NULL,
                pagina_inicio INTEGER NOT NULL,
                pagina_fin INTEGER NOT NULL
            )
        """)
        connection.executemany("""
            INSERT INTO adr_instrucciones_embalaje (
                codigo, titulo, contenido, seccion_adr, pagina_inicio, pagina_fin
            ) VALUES (
                :code, :title, :content, :section, :page_start, :page_end
            )
        """, records)
        connection.execute(
            "CREATE INDEX idx_adr_instrucciones_seccion "
            "ON adr_instrucciones_embalaje(seccion_adr)"
        )
        connection.commit()


def main() -> None:
    records = extract_instructions()
    create_database(records)
    print(f"Created {DB_PATH} with {len(records)} packaging instructions.")


if __name__ == "__main__":
    main()
