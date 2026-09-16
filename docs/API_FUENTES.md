# API y fuentes de datos

Este documento detalla cómo se obtienen los datos en **Consulta REAT**: el flujo web de la consulta pública del REAT y la API de la plataforma NAP.

> Importante: no existe una API pública oficial del REAT que permita consultar por NIF, matrícula o autorización. Por eso la app usa el **flujo web** de la consulta pública del Ministerio y, además, la **API NAP** (catálogo de datos abiertos de transporte) para enriquecer resultados.

## 1. Flujo web del REAT

**Base URL**

```
https://apps.fomento.gob.es/crgt/servlet/ServletController
```

### Formulario inicial

```
GET /crgt/servlet/ServletController?modulo=datosconsulta&accion=inicio&lang=es&estilo=default
```

Devuelve la página con el formulario `#consultaForm`. La app lo detecta en la respuesta y, si el servidor lo exige, muestra el `CaptchaWebView`.

### Consulta

Se realiza un `POST` (form-urlencoded) a la base URL:

| Parámetro | Valores |
| --- | --- |
| `tpsolic` | `E` (autorizaciones), `P` (competencia profesional), `M` (vehículos) |
| `accion` | `consultar_nif`, `consultar_empre`, `consultar_auto`, `consultar_inter` |
| `consulta` | Valor a buscar (NIF, nombre, nº autorización, licencia o matrícula) |
| `g-recaptcha-response` | Token reCAPTCHA (vacío si no aplica) |

Headers requeridos: `Content-Type: application/x-www-form-urlencoded`, `Referer` (URL del formulario inicial) y un `User-Agent` descriptivo.

La inspección escolar utiliza esta misma operación con `tpsolic=M` y `accion=consultar_nif`, enviando la matrícula en `consulta`. La pantalla no crea un endpoint paralelo: reutiliza el cliente, parser, CAPTCHA e historial del flujo REAT.

### Respuesta

- Si la respuesta es un **formulario con CAPTCHA**, la app devuelve `RequiereCaptcha` y el usuario lo resuelve en un WebView.
- Si contiene resultados (`fichaIdentidad`, tablas de autorizaciones/vehículos, etc.), el HTML se parsea con **jsoup** en `RgtHtmlParser`.

### Detección de CAPTCHA en el WebView

Al enviar el formulario desde el WebView se comprueba `#g-recaptcha-response`:

- **Vacío** → el usuario debe resolver el CAPTCHA antes de enviar.
- **Relleno o inexistente** → se envía el formulario automáticamente.

## 2. API NAP (Plataforma de Datos Abiertos del Transporte)

NAP es un **catálogo de conjuntos de datos abiertos** de transporte (GTFS, NetEx, CSV, etc.). Permite buscar operadores por nombre y obtener los conjuntos de datos que publican. **No contiene datos del REAT** (ni NIF, matrículas o autorizaciones), por lo que se usa solo para **enriquecer** el resultado de una consulta por empresa.

### Configuración

| Propiedad | Dónde | Descripción |
| --- | --- | --- |
| `baseUrl` | `strings.xml` → `api_nap_base_url` | `https://nap.transportes.gob.es` |
| `apiKey` | `local.properties` → `NAP_API_KEY` | Clave personal (no versionada) |
| `userAgent` | `strings.xml` → `api_nap_user_agent` | Identificador de la app |

La clave se inyecta en `BuildConfig.NAP_API_KEY` en tiempo de compilación. Si está vacía, `esConfigurada = false` y la app omite el enriquecimiento NAP.

### Endpoint

```
GET /api/Operador/GetByName/{name}
```

Headers:
- `ApiKey: {tu-clave}`
- `Accept: application/json`
- `User-Agent: {userAgent}`

### Respuesta (formato documentado)

La API documentada devuelve un **array plano** (no un envelope):

```json
[
  {
    "operadorId": 1234,
    "nombre": "TRANSPORTES EJEMPLO SL",
    "url": "https://...",
    "conjuntosDatos": [
      {
        "conjuntoDatoId": 5678,
        "nombre": "Datos GTFS",
        "descripcion": "Horarios y paradas..."
      }
    ]
  }
]
```

- Sin resultados → `[]` con HTTP 200.
- El mapper (`ReatApiMapper`) transforma cada operador en un `DatoItem` de la sección "Operadores encontrados" y cada conjunto de datos en la sección "Conjuntos de datos asociados".

### Ejemplo con curl

```bash
curl -H "ApiKey: TU_CLAVE" \
     -H "Accept: application/json" \
     "https://nap.transportes.gob.es/api/Operador/GetByName/TRANSPORTES%20EJEMPLO"
```

## 3. Enriquecimiento NAP

En `ConsultaViewModel.enriquecerSiAplica`:

1. Si el resultado ya incluye operadores (`parsed.operadores` no vacío), no se toca.
2. Si no, y existe un nombre de empresa (`identidadValor`), se consulta NAP por ese nombre.
3. Si NAP devuelve un `Success`, se copian `operadores` y `conjuntosDatos` al resultado.

El fallo o ausencia de clave NAP **nunca bloquea** la consulta principal.

## 4. Fuentes oficiales

- [Consulta pública del REAT – Ministerio de Transportes](https://apps.fomento.gob.es/crgt/servlet/ServletController?modulo=datosconsulta&accion=inicio&lang=es&estilo=default)
- [NAP – Plataforma de Datos Abiertos del Transporte](https://nap.transportes.gob.es)
- [datos.gob.es](https://datos.gob.es)
- [Real Decreto 443/2001 consolidado – BOE](https://www.boe.es/buscar/act.php?id=BOE-A-2001-8503)
- [Codificado DGT de 20 de mayo de 2026](CODIFICADO-DGT-20-MAYO-2026.pdf) (documento de referencia incorporado al repositorio)
