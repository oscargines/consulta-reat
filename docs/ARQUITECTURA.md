# Arquitectura

Este documento describe la arquitectura de **Consulta REAT**, los flujos de datos principales y las decisiones de diseño.

## Resumen

Aplicación Android nativa en **Kotlin** con **Jetpack Compose** y arquitectura **MVVM**. El objetivo es consultar el Registro de Empresas y Actividades de Transporte (REAT) a partir de fuentes abiertas, mostrar los resultados de forma clara y guardar un historial local.

```
┌────────────────────────────────────────────────────────┐
│ UI (Compose)                                           │
│  PantallaPrincipal · ConsultaScreen · ResultadoScreen  │
│  HistorialScreen · BaremoScreen · AcercaDeScreen       │
│  AppNavigation                                         │
└──────────────▲──────────────────────▲──────────────────┘
               │ StateFlow (UiState)  │ eventos (onConsultar, ...)
┌──────────────┴──────────────────────┴──────────────────┐
│ ConsultaViewModel (MVVM)                               │
│  · ejecutarConsulta / parsearHtmlResultado             │
│  · enriquecerSiAplica (NAP) · guardarYAvisar           │
└──────────────▲──────────────────────▲──────────────────┘
               │                      │
┌──────────────┴──────────┐   ┌───────┴──────────────────┐
│ RgtRepository           │   │ HistorialCache (DataStore)│
│  · ejecutar (web/API)   │   │ · guardar / eliminar      │
│  · consultarNap         │   │ · detección de duplicados │
└──────────────┬──────────┘   └───────────────────────────┘
               │
┌──────────────┴─────────────────────────────────────────┐
│ Data layer                                             │
│  RgtClient (OkHttp) + RgtHtmlParser (jsoup)   (web)    │
│  ReatApiClient + ReatApiMapper                  (NAP)   │
└─────────────────────────────────────────────────────────┘
```

## Capas

### 1. UI (`ui/`)

- **`AppNavigation.kt`** – `NavHost` con las rutas `inicio`, `consultas`, `historial`, `baremo` y `acercade`. La barra inferior de navegación (Inicio, Consultas, `+`, Historial, Acerca de) solo se muestra fuera de la pantalla principal. El botón `+` abre una nueva consulta.
- **`PantallaPrincipal.kt`** – Logo, nombre y tarjetas de acceso (Consultas, Historial, Acerca de).
- **`ConsultaScreen.kt`** – Formulario (tipo de consulta, tipo de identificación, campo de texto) y `CaptchaWebView` para resolver el CAPTCHA de la web oficial cuando aparece. Muestra un indicador de carga durante las consultas.
- **`ResultadoScreen.kt`** – Tarjetas con estado (vigente / en trámite / caducada) y detalle expandible. Cada valor se pinta debajo de su etiqueta.
- **`HistorialScreen.kt`** – Lista de consultas guardadas con acceso al detalle y borrado.
- **`BaremoScreen.kt`** – Consulta local de infracciones, con búsqueda, filtros por índice/gravedad, agrupación por subíndice y detalle normativo.
- **`AcercaDeScreen.kt`** – Información legal, versión y enlaces a las fuentes.

### 2. ViewModel (`ui/viewmodel/`)

`ConsultaViewModel` expone un `StateFlow<UiState>`:

```kotlin
sealed interface UiState {
    object Idle
    object Loading
    data class Success(resultado: ConsultaResultado)
    data class CaptchaRequerido(request: ConsultaRequest)
    data class Error(message: String)
}
```

Flujos:
- **`ejecutarConsulta`** – lanza la consulta en el `viewModelScope`; pasa a `Loading`, procesa el `ConsultaOutcome` del repositorio y actualiza el estado.
- **`parsearHtmlResultado`** – procesa el HTML devuelto por el WebView (flujo CAPTCHA) y guarda en historial.
- **`enriquecerSiAplica`** – si el resultado no trae operadores/datos NAP, consulta NAP por el nombre de la empresa y completa el resultado.
- **`guardarYAvisar`** – persiste el resultado y avisa si es un duplicado.

### 3. Repositorio (`RgtRepository.kt`)

Orquesta las fuentes de datos y el historial. Decide qué fuente usar:

- Si hay **API NAP configurada** y el tipo de identificación es NOMBRE, primero intenta la API.
- En caso contrario, usa el **flujo web** del REAT.
- Si el flujo web devuelve un formulario con CAPTCHA, se notifica `RequiereCaptcha` para que la UI muestre el WebView.

### 4. Datos (`data/`)

- **`client/RgtClient.kt`** – realiza el POST al servidor de la consulta pública del REAT con OkHttp y devuelve un `ConsultaOutcome` (`Resultado`, `RequiereCaptcha` o `Error`).
- **`parser/RgtHtmlParser.kt`** – convierte el HTML de la respuesta en `ParsedResult` (identidad, autorizaciones, vehículos, competencia profesional, CAP, consejero, operadores y conjuntos de datos) usando jsoup.
- **`api/ReatApiClient.kt` + `ReatApiMapper.kt`** – cliente de la API NAP (solo consulta por nombre) y mapper JSON a `ParsedResult`.
- **`cache/HistorialCache.kt`** – historial en `DataStore` (Preferences) con límite de 20 registros, retención de 30 días y deduplicación.
- **`baremo/BaremoDatabase.kt`** – copia la base SQLite incluida en assets al almacenamiento privado de Android.
- **`baremo/BaremoRepository.kt`** – consulta índices e infracciones; aplica filtros SQL y normaliza la búsqueda libre para admitir acentos opcionales.

### Consulta local del baremo sancionador

El baremo se distribuye como `app/src/main/assets/BasermoSancionador.db`. `BaremoDatabase` lo copia al directorio privado de bases de datos de Android en el primer acceso. La versión incluida es la 7.3 y contiene 1.066 infracciones.

La pantalla muestra los índices como una lista seleccionable. Los resultados se ordenan por índice, subíndice y código, y se presentan agrupados por subíndice para facilitar la consulta cuando se aplican los filtros de gravedad.

## Flujo de una consulta

1. El usuario rellena el formulario y pulsa **Consultar**.
2. `ViewModel.ejecutarConsulta` → `Loading`.
3. `RgtRepository.ejecutar`:
   - **API NAP** (si está configurada y el identificador es nombre) → `Resultado` o `Error`.
   - **Flujo web** → POST al servidor REAT:
     - Si la respuesta contiene el formulario con CAPTCHA → `RequiereCaptcha`.
     - Si no, se parsea el HTML → `Resultado`.
4. En `Success` se intenta **enriquecer con NAP** si faltan operadores/datos.
5. Se **guarda en el historial** (con aviso si ya existía).
6. La UI navega a la pantalla de **Resultado**, mostrando tarjetas por sección.

## Flujo CAPTCHA (WebView)

Cuando la web oficial devuelve el formulario con CAPTCHA:

1. Se muestra `CaptchaWebView` con la página oficial cargada.
2. Se rellena automáticamente el formulario (tipo de solicitud, acción y valor de consulta) vía JavaScript.
3. Si no hay CAPTCHA relleno (`#g-recaptcha-response` vacío), se espera a que el usuario lo resuelva.
4. Tras enviar el formulario, la página navega a la URL de resultados; se extrae el HTML y se llama a `parsearHtmlResultado`.

## Navegación

| Ruta | Contenido | Barra inferior |
| --- | --- | --- |
| `inicio` | Pantalla principal (logo + accesos) | No |
| `consultas` | Formulario o resultado según `UiState` | Sí |
| `historial` | Historial de consultas | Sí |
| `baremo` | Consulta local del baremo sancionador | Sí |
| `acercade` | Información legal y fuentes | Sí |

## Persistencia

El historial se guarda con `DataStore` (Preferences) bajo el archivo `historial_nosql`, clave `documentos`:

- **Máximo 20 entradas** (las más antiguas se descartan).
- **TTL de 30 días**: los registros anteriores se limpian al cargar.
- **Deduplicación**: una consulta con el mismo valor y tipo no se añade dos veces (se muestra un aviso).

## Tests

- `app/src/test/.../RgtHtmlParserTest.kt` – parseo de HTML del REAT.
- `app/src/test/.../ReatApiMapperTest.kt` – mapper de la respuesta NAP.
- `app/src/test/.../BaremoRepositoryTest.kt` – búsqueda del baremo sin distinguir mayúsculas ni acentos.
- `ExampleUnitTest.kt` – test base del template.

```bash
./gradlew :app:testDebugUnitTest
```
