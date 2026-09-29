# Consulta Transportes

![Logo](app/src/main/assets/logo%20grande.jpg)

Aplicación Android de apoyo a la inspección del transporte por carretera: consulta del **Registro de Empresas y Actividades de Transporte (REAT)**, consulta del **ADR 2025** (códigos ONU, etiquetas y regla de los 1000 puntos) y guías de inspección, a partir de **fuentes abiertas y públicas**.

La aplicación consulta los datos públicos del REAT publicados por el Ministerio de Transportes y Movilidad Sostenible y enriquece los resultados con los conjuntos de datos abiertos de la plataforma **NAP (Plataforma de Datos Abiertos del Transporte)**. No está afiliada a ningún organismo oficial y no garantiza la exactitud de los datos: verifica siempre la información en la fuente oficial.

## Características

- **Consulta pública del REAT** por NIF, nombre/razón social, nº de autorización, licencia internacional o matrícula.
- Tres tipos de consulta: **Autorizaciones**, **Competencia Profesional** y **Vehículos**.
- **Flujo web automático** contra la consulta pública oficial del REAT (apps.fomento.gob.es) con detección de CAPTCHA: si la web lo solicita, se muestra una vista WebView para resolverlo manualmente.
- **Enriquecimiento con datos abiertos NAP**: cuando una consulta devuelve datos de una empresa, se completan los operadores y conjuntos de datos publicados en NAP.
- **Resultados en tarjetas** con estado visual (vigente / en trámite / caducada) y detalle expandible.
- **Historial local** de consultas (hasta 20 registros, 30 días de retención, con detección de duplicados).
- **Consulta ADR 2025**: búsqueda de mercancías por N.º ONU o nombre (Tablas A y B), placa naranja conforme a ADR 5.3.2 y etiquetas de peligro de las clases 1 a 9.
- **Regla de los 1000 puntos (ADR 1.1.3.6)**: cálculo de exenciones con selección de grupo de embalaje, cantidades en litros o kilogramos y desglose por categoría de transporte.
- **Baremo sancionador** local, versión 7.3, con 1.066 infracciones, filtros por índice y gravedad, búsqueda textual y detalle normativo.
- **Inspección de Transporte Escolar**: consulta de autobuses por matrícula, importación de datos del REAT, cálculo de antigüedad al 1 de septiembre y guía rápida del Real Decreto 443/2001.
- **Inspección interna** con lectura del DNIe por NFC.
- **Acerca de** con información legal, referencias normativas y enlaces a las fuentes de datos.

## Tecnologías

| Capa | Tecnología |
| --- | --- |
| UI | Jetpack Compose (Material 3), Navigation Compose, StateFlow |
| Datos | OkHttp, jsoup (parseo HTML), DataStore Preferences (historial), SQLite local (baremo sancionador) |
| Arquitectura | MVVM (ViewModel + StateFlow + Repository) |
| Lenguaje | Kotlin (minSdk 31, target/compile SDK 37) |
| Tests | JUnit (unit tests del parser y del mapper NAP) |

## Requisitos previos

- Android Studio (o JDK 17+ y Android SDK).
- SDK de Android con `compileSdk` 37 y `minSdk` 31.
- Una conexión a Internet para realizar las consultas.

## Cómo compilar y ejecutar

```bash
# Compilar el APK de debug
./gradlew :app:assembleDebug

# Ejecutar los tests unitarios
./gradlew :app:testDebugUnitTest
```

El APK se genera en `app/build/outputs/apk/debug/app-debug.apk`. También puedes abrir el proyecto en Android Studio y pulsar **Run**.

## Configuración

### Clave de la API NAP (opcional)

El enriquecimiento con datos abiertos NAP requiere una clave de API. La clave **no se versiona** en el repositorio: se lee en tiempo de compilación desde `local.properties` (que no se sube a git).

1. Obtén tu clave en [NAP – Plataforma de Datos Abiertos del Transporte](https://nap.transportes.gob.es).
2. Añádela a tu `local.properties`:

```properties
NAP_API_KEY=tu-clave-aqui
```

Si no se configura ninguna clave, la app funciona igual: realiza las consultas del REAT por el flujo web y simplemente omite el enriquecimiento NAP.

### Firma del APK de release (opcional)

La firma de release se activa **solo si** existe `release.keystore` en la raíz del proyecto y se definen las propiedades en `gradle.properties`:

```properties
RELEASE_STORE_PASSWORD=...
RELEASE_KEY_PASSWORD=...
RELEASE_KEY_ALIAS=consulta-reat-key
```

## Estructura del proyecto

```
app/src/main/java/com/oscar/consultareat/
├── data/
│   ├── adr/           # Base SQLite ADR 2025, repositorio y modelos
│   ├── api/          # Cliente y mapper de la API NAP
│   ├── cache/        # Historial local (DataStore)
│   ├── client/       # Cliente del flujo web del REAT (OkHttp)
│   ├── datasource/   # Orígenes de datos de inspección interna
│   ├── pdf/          # Generación de documentos PDF
│   ├── print/        # Impresión Bluetooth (Zebra)
│   ├── repository/   # Almacenamiento de impresoras
│   └── parser/       # Parser del HTML del REAT (jsoup)
├── domain/           # Modelos de dominio y comandos de consulta
├── ui/
│   ├── inicio/       # Pantalla principal (logo + accesos)
│   ├── consulta/     # Formulario de consulta + WebView de CAPTCHA
│   ├── resultado/    # Resultados en tarjetas con estado
│   ├── historial/    # Historial de consultas
│   ├── baremo/       # Consulta local del baremo sancionador
│   ├── adr/          # Consulta ONU, panel naranja y 1000 puntos
│   ├── inspeccion/    # Inspección escolar (RD 443/2001) e interna (DNIe NFC)
│   ├── acercade/     # Información legal y fuentes
│   ├── theme/        # Tema y paleta de colores
│   └── viewmodel/    # ConsultaViewModel + UiState
├── RgtRepository.kt       # Orquesta datos y cache
├── SplashActivity.kt      # Pantalla de arranque
├── MainActivity.kt        # Punto de entrada de la UI
└── ConsultaREATApplication.kt
```

Las bases de datos `BasermoSancionador.db` y `adr_2025.db` se incluyen en `app/src/main/assets/` y se copian al almacenamiento privado de la aplicación en el primer acceso. La base ADR se regenera con `create_adr_db.py`.

## Documentación técnica

- [Arquitectura](docs/ARQUITECTURA.md): capas, flujo de datos, navegación y decisiones de diseño.
- [API y fuentes](docs/API_FUENTES.md): detalle del flujo web del REAT y de la API NAP.
- [Inspección de transporte escolar](docs/INSPECCION_TRANSPORTE_ESCOLAR.md): flujo, cálculo de antigüedad, checklist y correspondencias sancionadoras.
- [Módulo ADR 2025](docs/ADR_CONSULTA.md): fuentes de estudio, base de datos, panel naranja, etiquetas y regla de los 1000 puntos.
- [Historial de cambios](CHANGELOG.md).

## Fuentes de datos

- [Consulta pública del REAT](https://apps.fomento.gob.es/crgt/servlet/ServletController?modulo=datosconsulta&accion=inicio&lang=es&estilo=default) – Ministerio de Transportes y Movilidad Sostenible.
- [NAP – Plataforma de Datos Abiertos del Transporte](https://nap.transportes.gob.es).
- [datos.gob.es](https://datos.gob.es) – Catálogo nacional de datos abiertos.

## Licencia

[MIT](LICENSE)
