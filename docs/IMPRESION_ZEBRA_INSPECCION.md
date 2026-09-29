# Documentación Técnica: Impresión de Acta de Inspección en Zebra ZQ521/RW420

## Resumen
Implementación completa del flujo de impresión de actas de inspección de transporte escolar directamente a impresoras térmicas Zebra (modelos ZQ521 y RW420) mediante Bluetooth y lenguaje CPCL.

---

## Arquitectura del Sistema

### Módulos Principales

```
app/
├── data/
│   ├── print/
│   │   ├── BluetoothPrinterPolicy.kt      # Validación modelos permitidos (ZQ521, RW420)
│   │   ├── ZebraPrinterProbe.kt           # Detección modelo real vía SGD
│   │   └── BluetoothPrinterUtils.kt       # Generación CPCL + envío Bluetooth
│   └── repository/
│       └── BluetoothPrinterStorage.kt     # Persistencia SQLite de impresoras
├── domain/
│   ├── ActaInspeccionData.kt              # Modelo datos acta
│   └── SavedBluetoothPrinter.kt           # Modelo impresora guardada
└── ui/
    ├── inspeccion/
    │   └── InspeccionTransEscolarScreen.kt # Pantalla inspección + botón imprimir
    └── impresora/
        └── BluetoothPrinterScreen.kt       # Gestión impresora (escaneo, validación, guardado)
```

---

## Flujo de Impresión

### 1. Consulta Exitosa (`UiState.Success`)
- Usuario realiza consulta de matrícula
- Parser extrae: `matricula`, `empresaTitular`, `numeroAutorizacion`
- Se muestra botón "Imprimir Acta de Inspección" (verde si hay impresora, gris si no)

### 2. Pulsar Botón Imprimir
```kotlin
val storage = BluetoothPrinterStorage(context)
if (storage.hasDefaultPrinter()) {
    val printer = storage.getDefaultPrinter()
    val actaData = ActaInspeccionData(
        matricula = resultado.matricula,
        empresaTitular = resultado.empresaTitular,
        numeroAutorizacion = resultado.numeroAutorizacion
    )
    printActaInspeccion(context, printer.mac, actaData)
} else {
    mostrarAvisoImpresora = true // Diálogo "Configurar impresora"
}
```

### 3. Generación CPCL (`BluetoothPrinterUtils.printActaInspeccion`)
```cpcl
! 0 200 200 <height> 1
PAGE-WIDTH 792
TEXT 7 3 <x> <y> B "ACTA DE INSPECCIÓN"
TEXT 7 3 <x> <y> B "TRANSPORTE ESCOLAR"
LINE <margen> <y> <ancho> <y> 2
TEXT 7 2 <x> <y> "Fecha: dd/MM/yyyy  Hora: HH:mm"
...
BOX <x> <y> <x+w> <y+h> 2  // Cajas para cada campo
TEXT 7 2 <x> <y> B "Matrícula"
TEXT 7 2 <x> <y+22> "1234ABC"
...
FORM
PRINT
```

### 4. Envío Bluetooth
- **Conexión compartida** (`openSharedBtConnection`): una sola apertura para lotes
- **Conexión individual**: apertura → envío → cierre (uso único)
- Configuración automática según modelo:
  - ZQ521/RW420: `device.languages=cpcl`, `media.type=continuous`, `ezpl.print_mode=tear_off`

El acta se genera con una única cabecera CPCL. Los comandos `TEXT` siguen la
sintaxis compatible con ambas impresoras (`TEXT font size x y texto`), sin el
parámetro `B` que provocaba que la RW420 no interpretase correctamente el trabajo.
La cabecera no se duplica en la función de envío y `FORM`/`PRINT` se emiten una
sola vez al final.

---

## Pantalla Gestión Impresora (`BluetoothPrinterScreen`)

### Funcionalidades
1. **Escaneo Bluetooth** (10 seg) con permisos Android 12+ (BLUETOOTH_SCAN, BLUETOOTH_CONNECT, ACCESS_FINE_LOCATION)
2. **Dispositivos emparejados** como fallback si no hay ubicación/GPS
3. **Validación modelo real** vía SDK Zebra (SGD `device.product_name`)
4. **Solo modelos permitidos**: ZQ521, RW420 (normalizado: mayúsculas, sin espacios/guiones)
5. **Guardado** en SQLite (`bluetooth_printers.db`), con MAC única y modelo
6. **Migración** automática de la MAC guardada por versiones anteriores en SharedPreferences
7. **Estados UI**: escaneando, validando, guardado, error, sin resultados

### Modal de escaneo

Durante la detección se muestra un modal no cancelable con `CircularProgressIndicator`
centrado, mensaje de estado y cuenta atrás. El receptor Bluetooth actualiza el
listado observable y cierra el modal al recibir `ACTION_DISCOVERY_FINISHED`, sin
necesidad de salir y volver a entrar en la pantalla.

### Persistencia SQLite

Tabla `printers`:

| Campo | Descripción |
|-------|-------------|
| `id` | Identificador local |
| `nombre` | Nombre Bluetooth mostrado |
| `mac` | Dirección Bluetooth, única |
| `modelo` | Modelo detectado por SGD |
| `es_predeterminada` | Marca de impresora activa |

El guardado de una nueva impresora hace `upsert` por MAC y mantiene las demás
impresoras del listado. La selección predeterminada se almacena en la misma tabla.

### Modal de impresión

`printActaInspeccion` comunica estados a la UI mediante `PrintStatus`:

- `Connecting`: "Conectando con la impresora..."
- `Sending`: "Enviando formato..."
- `Completed`: "Impresión completada."
- `Failed`: muestra el detalle del error

Mientras se conecta o envía el formato, el modal muestra un indicador circular y
el botón queda deshabilitado para evitar impresiones duplicadas.

### Permisos Requeridos (AndroidManifest.xml)
```xml
<uses-permission android:name="android.permission.BLUETOOTH" android:maxSdkVersion="30" />
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN" android:maxSdkVersion="30" />
<uses-permission android:name="android.permission.BLUETOOTH_SCAN" />
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
```

---

## Modelo de Datos

### `ActaInspeccionData`
```kotlin
data class ActaInspeccionData(
    val matricula: String,
    val empresaTitular: String,
    val numeroAutorizacion: String,
    val tip: String,
    val unidad: String,
    val lugar: String,
    val fechaInspeccion: String = LocalDate.now().format("dd/MM/yyyy"),
    val horaInspeccion: String = LocalTime.now().minusMinutes(5).format("HH:mm")
)
```

## Datos previos a la impresión

Antes de iniciar la conexión Bluetooth se solicita un modal con:

- `TIP`, validado con el formato `^[A-Z][0-9]{5}[A-Z]$`.
- `Unidad`.
- `Lugar`, con valor sugerido `Estacionamiento Colegio Público de Llanes`.

Los tres valores se cargan desde `acta_inspeccion_preferences` y se guardan al
aceptar el modal. `Cancelar` cierra el modal sin iniciar la impresión.

La fecha se toma del sistema. La hora se toma del sistema restando cinco minutos.

## Plantilla JSON

La plantilla se encuentra en:

```text
app/src/main/assets/acta_inspeccion.json
```

Placeholders soportados:

```text
[[fecha]] [[hora]] [[lugar]] [[matricula]]
[[numero_autorizacion]] [[empresa]] [[tip]] [[unidad]]
[[dia]] [[mes]] [[anio]]
```

La plantilla contiene el encabezado legal, el justificante, los tres párrafos,
el cierre de lugar/fecha, el TIP y la unidad.

## Imagen del encabezado

Los recursos están incluidos en los assets de Android:

```text
app/src/main/assets/EscTrafico.png
app/src/main/assets/EscTrafico.pcx
```

La versión PNG se convierte a bitmap monocromo `EG` de CPCL y se centra en la
parte superior del documento. El PCX queda incluido como recurso compatible para
futuras optimizaciones específicas de Zebra.

### `SavedBluetoothPrinter`
```kotlin
data class SavedBluetoothPrinter(
    val id: Int,
    val nombre: String,
    val mac: String
)
```

---

## Corrección Extracción Datos (`RgtHtmlParser`)

### Antes
- `identidadValor` mezclaba matrícula y empresa
- No había campos separados

### Después
```kotlin
ParsedResult.Success(
    identidadLabel = "...",
    identidadValor = "...",
    matricula = "...",           // NUEVO: desde tabla vehículos
    empresaTitular = "...",      // NUEVO: desde sección "titular" / vehículos
    numeroAutorizacion = "...",  // NUEVO: desde tabla autorizaciones
    ...
)
```

Se propaga a `ConsultaResultado`, `HistorialCache`, `ConsultaCommand`, `ReatApiMapper`.

---

## Strings UI (`strings.xml`)

30+ cadenas para impresora:
- `bluetooth_printer_title`: "Configurar impresora Zebra"
- `bluetooth_printer_scan_action`: "Buscar"
- `bluetooth_printer_save_action`: "Guardar"
- `bluetooth_printer_validating`: "Validando modelo..."
- `bluetooth_printer_not_supported_model`: "Modelo no compatible (%1$s). Solo: %2$s"
- `bluetooth_printer_saved_status`: "Impresora guardada: %1$s"
- `bluetooth_printer_discovered_devices`: "Dispositivos encontrados"
- etc.

---

## Navegación (`AppNavigation.kt`)

```kotlin
private const val RUTA_IMPRESORA_BLUETOOTH = "impresora-bluetooth"

composable(RUTA_INSPECCION_TRANS_ESCOLAR) {
    InspeccionTransEscolarScreen(
        onAbrirConfiguracionImpresora = { navController.navigate(RUTA_IMPRESORA_BLUETOOTH) },
        onImprimirActa = { /* manejado en screen */ }
    )
}
composable(RUTA_IMPRESORA_BLUETOOTH) {
    BluetoothPrinterScreen(onBackClick = { navController.popBackStack() })
}
```

---

## Dependencias

### `app/build.gradle.kts`
```kotlin
dependencies {
    ...
    implementation(fileTree("libs") { include("*.jar") })  // ZSDK_ANDROID_API.jar
}
```

### `app/libs/`
- `ZSDK_ANDROID_API.jar` (SDK Zebra Android - copiado de SinCarnetAndroid)

---

## Especificación CPCL (Zebra ZQ521/RW420 @ 203 DPI)

| Parámetro | Valor |
|-----------|-------|
| Ancho papel | 792 dots (≈104 mm) |
| Márgenes | 20 dots laterales, 120 dots inferior |
| Fuente título | Font 7, Size 3, Alto 34 dots |
| Fuente cuerpo | Font 7, Size 2, Alto 22 dots |
| Comandos | `TEXT`, `BOX`, `LINE`, `FORM`, `PRINT` |
| Encoding | ISO-8859-1 |

---

## Testing Manual

### Casos de Prueba
1. ✅ App compila `assembleRelease` sin errores
2. ✅ Instalación `adb install -r` mantiene datos
3. ✅ Botón "Imprimir Acta" visible bajo "Comprobar"
4. ✅ Estados botón: verde (listo), gris "Configurar...", gris "Realiza consulta"
5. ✅ Pantalla impresora: escaneo, modal de progreso, validación ZQ521/RW420
6. ✅ Persistencia de múltiples impresoras y predeterminada en SQLite
7. ✅ Modal de estados durante conexión y envío
8. ⏳ Impresión real en ZQ521
9. ⏳ Impresión real en RW420

---

## Referencias
- SDK Zebra: `com.zebra.sdk.comm.BluetoothConnection`, `com.zebra.sdk.printer.SGD`
- CPCL Programming Guide (Zebra)
- Android Bluetooth Permissions (API 31+)
- SinCarnetAndroid: referencia implementación completa (`data/print/`, `presentation/BluetoothPrinterScreen.kt`)

---

**Fecha**: 2026-09-17
**Versión**: 1.3.0 (incremento minor por feature impresión)
