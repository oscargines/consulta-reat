# Inspección de Transporte Escolar

## Objetivo

La pantalla `InspeccionTransEscolarScreen` es una guía rápida para inspecciones de autobuses que puedan estar sometidos al **Real Decreto 443/2001, de 27 de abril**, sobre condiciones de seguridad en el transporte escolar y de menores.

La pantalla no sustituye la denuncia ni la comprobación documental oficial. El texto consolidado del BOE es informativo y debe contrastarse con la normativa vigente y con las instrucciones aplicables.

## Entrada y navegación

- Ruta: `inspeccion-trans-escolar`.
- Acceso: únicamente desde la tarjeta `Inspección Trans Escolar` de `PantallaPrincipal`.
- No se añade un acceso específico a la barra inferior.
- Título de la pantalla: `Inspección de Transporte Escolar`.

## Consulta del vehículo

La pantalla reutiliza el flujo común del REAT:

```text
Tipo de consulta: VEHICULO
Tipo de identificación: MATRICULA
Acción web: consultar_nif
```

También reutiliza:

- Captura OCR de matrícula mediante cámara.
- Detección y resolución manual del CAPTCHA en `CaptchaWebView`.
- `ConsultaViewModel`, `RgtRepository` y `RgtHtmlParser`.

Los campos se buscan en las secciones de vehículo y autorizaciones devueltas por el REAT. La interfaz muestra, cuando están disponibles:

- Matrícula.
- Fecha de matriculación.
- Tipo y número de autorización.
- Validez.
- Empresa o titular.
- Domicilio.

Como las etiquetas del HTML oficial pueden variar, la extracción usa coincidencias tolerantes para matrícula, matriculación, autorización, validez, empresa, titular, domicilio y dirección.

## Cálculo de antigüedad

La antigüedad no se calcula con la fecha del día de la inspección. Se calcula respecto al **1 de septiembre del inicio del curso escolar vigente**:

```text
Si la fecha actual es septiembre o posterior:
    inicio del curso = 1 de septiembre del año actual
Si no:
    inicio del curso = 1 de septiembre del año anterior
antigüedad = años completos desde la primera matriculación hasta ese día
```

Avisos:

- `0-10 años`: no se genera aviso de antigüedad.
- `Más de 10 y hasta 16 años`: se avisa de que solo puede utilizarse excepcionalmente.
- `Más de 16 años`: se avisa de que el vehículo no puede utilizarse para estos servicios con carácter general.
- Sin fecha reconocible: se avisa de que no se puede calcular la antigüedad.

Para vehículos de más de 10 años y hasta 16 años, el aviso recuerda las condiciones acumulativas del artículo 3: acreditar dedicación previa a esa misma clase de transporte o aportar certificado de desguace de otro vehículo dedicado al transporte escolar durante el curso actual o el anterior. Deben tenerse en cuenta las especialidades territoriales de la disposición adicional cuarta.

También se avisa cuando la respuesta REAT no contiene una autorización de transporte asociada a la matrícula.

## Checklist

El checklist usa una única casilla por requisito. Una casilla marcada significa **cumple**. Las casillas no marcadas se consideran incidencias a revisar cuando se pulsa `Comprobar`.

Los bloques cubren:

- Autorización y antigüedad: artículos 2 y 3.
- Características técnicas: artículo 4.
- Distintivo V-10: artículo 5.
- ITV: artículo 6.
- Conductores: artículo 7.
- Acompañante: artículo 8.
- Velocidad: artículo 9.
- Itinerario, paradas y acceso: artículo 10.
- Duración del viaje y descansos: artículo 11.
- Seguro: artículo 12.
- Documentación exigible por la entidad organizadora: artículo 13.

La comprobación general no sustituye los avisos automáticos de antigüedad y autorización, que se muestran en cuanto termina la consulta del vehículo.

## Modales del codificado

Al pulsar `Comprobar`, las incidencias se muestran una a una. Cada modal incluye:

- Código o código-precepto.
- Artículo y calificación.
- Hecho infringido.
- Cuantía de la multa.
- Responsable.
- Norma infringida.
- Precepto sancionador.
- Observaciones para redactar los hechos.

Las correspondencias específicas del RD 443/2001 incluyen autorización (`KA01.08`), acompañante (`KB01.01`), documentación de la entidad organizadora (`KC03.01`), antigüedad (`KF01.01`) y plazas de los menores (`KG01.01`). Las condiciones técnicas y de circulación utilizan los códigos del codificado DGT incorporado como referencia.

## Fuentes

- [Real Decreto 443/2001 consolidado – BOE](https://www.boe.es/buscar/act.php?id=BOE-A-2001-8503)
- [`CODIFICADO-DGT-20-MAYO-2026.pdf`](CODIFICADO-DGT-20-MAYO-2026.pdf)
- [Consulta pública del REAT](https://apps.fomento.gob.es/crgt/servlet/ServletController?modulo=datosconsulta&accion=inicio&lang=es&estilo=default)

## Verificación

```bash
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
```
