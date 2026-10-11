# Changelog

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