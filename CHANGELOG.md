# Changelog

Todos los cambios notables del proyecto se documentan en este fichero.
El formato está basado en [Keep a Changelog](https://keepachangelog.com/es-ES/1.1.0/).

## [1.5.3] - 2026-10-08

### Añadido
- **Consulta ADR**: al pulsar el número de peligro de la placa naranja o su dato, se muestra la descripción del código correspondiente.
- **Instrucciones de embalaje ADR 4.1**: consulta en un modal el texto completo de los códigos P, IBC, LP y R, con sección y páginas del ADR 2025. El contenido se sirve desde una base SQLite indexada generada mediante `create_adr_packaging_db.py`.

## [1.5.2] - 2026-10-01

### Añadido
- **Coeficiente Característico**: nueva pantalla accesible desde la tarjeta "Coeficiente Característico" en la pantalla principal (entre ADR y Acerca de).
  - Cálculo del perímetro efectivo (L) teórico a partir del tamaño del neumático (formato 315/70 R 22.5).
  - 3 claves matemáticas exactas: altura goma ×2, llanta a mm (×25.4), diámetro total ×3.1416.
  - Detección de fraude: % Desvío = (L teórica / Lr) × 100 → ≥104% o ≤96% = fraude confirmado.
  - Selección del tamaño de rueda mediante desplegable con catálogo de tamaños estándar (315/70 R 22.5, 295/80 R 22.5, etc.): elimina la entrada manual que obligaba a corregir el cursor por los caracteres no numéricos.
  - Botón "No encuentro una numeración válida": modal en el que se mide y se introduce la altura total del neumático (mm, rango 400–1500). La L teórica se calcula como altura × 3.1416, se muestra en una tarjeta reeditable y entra en el mismo cálculo de desvío.
  - Diálogo de resultado con desglose: L teórica, L revisión, % Desvío, estado fraude/sin fraude.
  - En caso de fraude: instrucciones de medición (4 pasos), diagrama explicativo (Croquis_medicion.png), acción requerida (taller de verificación).

### Mejorado
- Pantalla scrolleable con padding inferior para que el teclado no tape el último campo (tamaño rueda).
- Corrección en parsing de tamaño de rueda: acepta formato con y sin separadores.

## [1.5.1] - 2026-09-29

### Añadido
- **Inspección de transporte escolar**: nueva tarjeta "Tipo de transporte" sobre la consulta por matrícula con cuatro opciones excluyentes (radiogroup): A) transporte público regular de uso especial escolar, B) regular de uso general, C) discrecional y D) privado complementario de viajeros.
- Para B, C y D se solicita por modal el número de plazas del vehículo y los pasajeros menores de 16 años, con teclado numérico.
- Al superar los umbrales del artículo 1 del RD 443/2001 (B: mitad o más de las plazas reservadas a menores; C: tres cuartas partes de viajeros menores; D: un tercio de viajeros menores) se muestra una alerta y el texto animado "TRANSPORTE ESCOLAR" en rojo.

## [1.5.0] - 2026-09-29

### Cambiado
- **Renombrado de la aplicación** a "Consulta Transportes": nombre en el lanzador, splash, pantalla principal y "Acerca de". El identificador de paquete no cambia para poder actualizar la app sin desinstalar.

### Añadido
- **Módulo ADR 2025** (documentación en `docs/ADR_CONSULTA.md`):
  - Consulta de mercancías por N.º ONU o nombre sobre Tabla B y Tabla A del ADR 2025, con base SQLite generada desde las tablas del BOE (`create_adr_db.py`, migración v2 con `nombre_normalized`).
  - Mensaje claro cuando no se encuentran resultados, incluidos los 404 ONU que faltan en Tabla B (cobertura vía `UNION` con Tabla A).
  - Detalle de cada epígrafe con placa naranja conforme a ADR 5.3.2 (proporción 40x30, marco negro proporcional, dígitos negros dinámicos) y etiquetas de peligro reales de las clases 1-9 desde `assets/PlacasEtiquetas/`, con aviso textual cuando falta algún modelo.
  - Calculadora de la regla de los 1000 puntos (ADR 1.1.3.6): búsqueda, selección de grupo de embalaje cuando un ONU tiene varios epígrafes, solicitud de cantidad en litros o kilogramos, factores 50/3/1, multiplicador x20 de la nota a), bloqueo con categoría 0, categorías 0 y 4 no computan, desglose por categoría y aviso de documentación (ADR 5.4.1).
- **Inspección interna** con lectura del DNIe por NFC y soporte de actas.
- **"Acerca de"** con sección de referencias legales agrupada (LOTT/ROTT, Reglamentos UE 1071/2009 y 1072/2009, ADR 2025, RD 97/2014, RD 443/2001, LSV, RGCir, RGVeh, RD 920/2017, Ley 37/2007, RGPD, LOPDGDD) con enlaces al BOE y EUR-Lex.
- Fuentes de estudio versionadas en `Assets/`: ADR 2025, manual de inspección MMPP 2025, tablas A y B del BOE y modelos de etiquetas.

### Mejorado
- Impresión Bluetooth (Zebra) y almacenamiento de impresoras.
- Correcciones en la búsqueda ONU (entradas de Tabla A sin correspondencia en Tabla B) y en el flujo de añadir mercancías al cálculo de puntos.

## [1.4.0] - 2026-09-28

### Añadido
- Impresión del acta de inspección en impresoras Zebra ZQ521/RW420.

## [1.3.0]

### Añadido
- Inspección de transporte escolar (RD 443/2001).
- Detección de matrícula por OCR con cámara.
