# Changelog

## Versión 1.6.2

### Coeficiente característico: nuevo cálculo de fraude

- El cálculo pasa a utilizar Wa y La de la activación del tacógrafo, y Wp y Lp del último calibrado periódico.
- Se calculan gamma (Wa × La), beta (Wp × Lp), alfa (beta − gamma) y dseta (alfa × 100 / gamma).
- Se detecta fraude cuando el valor absoluto de dseta supera el 4 %.
- Si La y Lp coinciden, se muestra directamente ausencia de fraude.
- Se eliminan el selector de tamaño de rueda y la medición de altura del neumático.

### Firma digital

- Se incrementó el versionCode de 13 a 14.
- Se actualizó el versionName de "1.6.1" a "1.6.2".

### Notas adicionales

- Se añadieron tests unitarios para el nuevo cálculo del coeficiente característico.

## Versión 1.6.1

### Nuevo módulo de radiactivos - Clase 7

Se incorpora la clasificación y señalización de bultos de materia radiactiva conforme al ADR 2025:

- Búsqueda de códigos ONU de clase 7 y base local de radionúclidos (`adr_radionucleidos_2025_v2.db`).
- Clasificación en excluido, exento, exceptuado y regulado.
- Cálculo de categoría (I-BLANCA, II-AMARILLA y III-AMARILLA), TI y CSI/ISC.
- Etiquetas 7A, 7B, 7C y 7E con contenido, actividad, TI y CSI/ISC pintados sobre la imagen.
- Placa 7D del vehículo con el número UN y paneles naranja delanteros, traseros y laterales.
- Etiquetas orientativas con guiones cuando falta la dosis o el TI, o cuando el bulto no es transportable.
- Corrección del pintado interior de las etiquetas: texto visible, alineado con las líneas de datos y sin tapar la palabra `RADIOACTIVE` de la placa 7D.

### Disposiciones especiales ADR

- Nueva base `adr_disposiciones_especiales_2025.db` y consulta de las disposiciones especiales del capítulo 3.3 desde la ficha ONU.

### Firma digital

- Se incrementó el versionCode de 12 a 13.
- Se actualizó el versionName de "1.5.5" a "1.6.1".

### Notas adicionales

- Se actualizó la documentación técnica con `docs/RADIACTIVOS_CLASE_7.md`.
- Se añadieron tests para la clasificación de radiactivos y para las etiquetas orientativas.

## Versión 1.5.5

### Baremo Sancionador actualizado a la versión 7.4

Se ha actualizado el baremo sancionador a la versión oficial 7.4, actualizada hasta el 5 de octubre de 2026. Las principales novedades son:

**Nuevas infracciones añadidas (5 nuevos códigos):**

1. **EA02.04** (Grave - 801€): No devolver a la Administración una autorización o licencia de transporte, alguna de sus copias o documentación cuando debiera haber sido devuelta, siempre que el documento conserve apariencia de validez.

2. **EA02.05** (Grave - 401€): No conservar por parte de la Empresa los documentos de los DeCA Art. DT.8 LEY.

3. **EA02.06** (Grave - 401€): No proporcionar datos esenciales relativos a la conservación por parte de la Empresa de los DeCA.

4. **EA02.07** (Grave - 401€): No llevar la tarjeta del conductor en formato papel u obligatoria en los DeCA.

5. **EA03.03** (Leve - 301€): No llevar en el vehículo el documento electrónico de que se trate.

**Detalles de la actualización:**
- Total de infracciones: 1071 (incremento de 5 respecto a la versión anterior 1066)
- Se añadieron 5 nuevos códigos en el índice E (Transporte público discrecional de mercancías)
- No se removieron códigos de la versión anterior
- Se mantienen todos los 21 índices (A través Z) y 116 subíndices

### Firma digital
- Se incrementó el versionCode de 11 a 12
- Se actualizó el versionName de "1.5.4" a "1.5.5"

### Notas adicionales
- La base de datos `BasermoSancionador.db` fue migrada añadiendo las 5 nuevas infracciones manteniendo la compatibilidad con la estructura existente
- Todas las pruebas existentes pasaron exitosamente (BUILD SUCCESSFUL)