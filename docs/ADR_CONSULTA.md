# Módulo ADR 2025

Guía técnica y de estudio del módulo de mercancías peligrosas: consulta de códigos ONU (Tabla B y Tabla A), panel naranja, etiquetas de peligro y cálculo de la regla de los 1000 puntos (ADR 1.1.3.6).

## Fuentes de estudio

Los documentos fuente están versionados en la carpeta `Assets/`:

| Documento | Contenido |
| --- | --- |
| `ADR 2025.pdf` | Texto oficial del ADR 2025 en español (capítulo 5.3: placas-etiquetas, panel naranja, marcas). |
| `5 2026-03-03-Manual inspección 2025 MMPP (1).pdf` | Manual de inspección del transporte de mercancías peligrosas 2025 (cuadro 1.1.3.6.3, cálculo 1.1.3.6.4). |
| `tabla_a_adr_2025_boe.xls` | Tabla A del ADR 2025 publicada por el BOE (fuente de la base de datos). |
| `tabla_b_adr_2025.docx` | Tabla B del ADR 2025 publicada por el BOE (fuente de la base de datos). |
| `PlacasEtiquetas/` | Imágenes originales de los modelos de etiquetas de peligro (clases 1 a 9) y del panel naranja de referencia. |

## Base de datos

- Fichero: `app/src/main/assets/adr_2025.db` (SQLite).
- Generador: `create_adr_db.py` (raíz del proyecto). Requiere `xlrd` y lee los ficheros de `Assets/`.
- Tablas:
  - `adr_tabla_b`: materia u objeto (nombre, N.º ONU, clase, nota, `nombre_normalized`).
  - `adr_tabla_a`: una fila por epígrafe de Tabla A (grupo de embalaje, etiquetas, disposiciones, categoría de transporte, N.º de peligro, `nombre_normalized`).
- La app copia la base al almacenamiento privado en el primer uso (`AdrDatabase`) y migra a la versión 2 añadiendo `nombre_normalized` si falta.
- Discrepancia conocida: la Tabla B del BOE no contiene 404 N.º ONU presentes en Tabla A (mayoría de clase 1). La búsqueda hace `UNION` con Tabla A para cubrirlos.

### Regenerar la base

```bash
pip install xlrd
python create_adr_db.py
```

## Consulta de códigos ONU

Pantalla: `ui/adr/AdrConsultaOnuScreen.kt`.

- Búsqueda alfanumérica por N.º ONU o nombre, sin distinguir mayúsculas ni acentos (`nombre_normalized`).
- `AdrRepository.findByNombreOrOnu` consulta Tabla B y, mediante `UNION`, las entradas de Tabla A cuyo ONU no esté en Tabla B. Si no hay resultados se muestra un mensaje al usuario.
- El detalle muestra los epígrafes de Tabla A con placa naranja, etiquetas de peligro y todos los campos de la columna.

### Panel naranja (ADR 5.3.2)

Dibujado en Compose con números dinámicos (`numeroPeligro` arriba, `numeroOnu` abajo, en negro):

- Proporción 40 x 30 cm (`aspectRatio(4f/3f)`).
- Marco negro proporcional: 15 mm sobre 40 cm de ancho (3,75 %).
- Banda divisoria horizontal negra del mismo grosor.
- Dígitos a 1/4 del ancho del panel (equivale a los 100 mm del ADR).

### Etiquetas de peligro (ADR 5.3.1)

- Imágenes reales en `app/src/main/assets/PlacasEtiquetas/` (copiadas de `Assets/PlacasEtiquetas/`).
- Mapeo clase/subclase a fichero en `etiquetaFileName`: 1 (genérica y 1.3–1.6), 2.1/2.2/2.3, 3, 4.1/4.2/4.3, 5.1/5.2, 6.1/6.2, 7 (webp genérica), 8, 9.
- Si falta la imagen se muestra el texto "Etiqueta no disponible" en un recuadro.
- Las etiquetas se centran horizontalmente y varias se colocan una al lado de otra.
- Formatos soportados por `BitmapFactory`: PNG y WEBP.

## Regla de los 1000 puntos (ADR 1.1.3.6)

Pantalla: `ui/adr/AdrCalculo1000PuntosScreen.kt`.

Flujo: buscar mercancía -> si el mismo ONU tiene varios epígrafes en Tabla A, diálogo para elegir grupo de embalaje -> diálogo de cantidad total (litros o kilogramos) -> se añade a la lista.

Reglas implementadas (manual de inspección MMPP, págs. 11-13):

1. Categorías de transporte según columna (15) de Tabla A.
2. Suma ponderada: cat. 1 x 50, cat. 2 x 3, cat. 3 x 1; no debe superar 1000.
3. Nota a): los ONU 0081, 0082, 0084, 0241, 0331, 0332, 0482, 1005 y 1017 (cat. 1) multiplican x 20.
4. Cat. 0: cantidad máxima 0, bloquea la exención.
5. Cat. 4: ilimitada, no computa.
6. Unidades: líquidos en litros; sólidos, gases licuados y disueltos en kg; gases comprimidos en litros (capacidad de agua).
7. Documentación (ADR 5.4.1): la carta de porte debe indicar la cantidad total y el valor calculado por categoría; la pantalla muestra el desglose.

Si el total supera 1000 la carga queda fuera de la exención y se aplican todas las disposiciones del ADR.

## Referencias legales

- [ADR 2025 (UNECE)](https://unece.org/transport/dangerous-goods/adr-2025-vol-i-and-ii)
- [RD 97/2014, de 14 de febrero](https://www.boe.es/buscar/act.php?id=BOE-A-2014-2110): operaciones de transporte de mercancías peligrosas por carretera en territorio español.
- [Directiva 2008/68/CE](https://eur-lex.europa.eu/legal-content/ES/TXT/?uri=CELEX%3A32008L0068): transporte terrestre de mercancías peligrosas.
- [Manual de inspección MMPP](https://www.transportes.gob.es/transporte-terrestre/inspeccion-y-seguridad-en-el-transporte)
