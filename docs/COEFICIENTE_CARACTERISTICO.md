# Coeficiente característico

Este documento describe la detección de indicios de manipulación del tacógrafo implementada en la pantalla `CoeficienteCaracteristicoScreen`, siguiendo la metodología de la Escuela de Tráfico (módulo de Tacógrafos, apartados 2.1.1–2.2).

## Objetivo

Comparar los parámetros de calibración del tacógrafo entre la activación y el último calibrado o control periódico para detectar posibles alteraciones del coeficiente característico `W`, de la circunferencia efectiva `L` o de la relación entre ambos.

El resultado es un **indicio**, no una prueba de fraude. Un valor por encima del umbral obliga a comprobar el vehículo en un centro técnico autorizado.

## Datos de entrada

| Campo | Descripción |
| --- | --- |
| `Wa` | Coeficiente característico `W` (imp/km) en la activación o primer calibrado. |
| `La` | Circunferencia efectiva del neumático `L` (mm) en la activación. |
| `Wp` | Coeficiente característico `W` en el último calibrado o control periódico. |
| `Lp` | Circunferencia efectiva `L` en el último calibrado o control periódico. |

Los cuatro valores deben ser numéricos y mayores que cero. La pantalla acepta coma o punto como separador decimal.

## Selección del método

- **Método A — W constante:** se usa cuando `La` y `Lp` coinciden (mismo neumático, sin cambios de caja de cambios ni precintos).
- **Método B — cambio de neumáticos:** se usa cuando `La` y `Lp` son distintas. La circunferencia cambia y debe permanecer constante el producto `W × L`.
- **Método C — cambio de grupo o caja de cambios:** la relación `W × L` anterior deja de ser válida. Exige una nueva calibración que se toma como nuevo punto de partida. Si no consta, procede el traslado a centro técnico.
- **Método D — sensor de movimiento:** si el tacógrafo registra descanso con el vehículo en marcha, debe revisarse el sensor y la posible presencia de un imán. Con imán procede denuncia directa; sin imán, traslado a centro técnico.

La pantalla implementa los métodos A y B. Los métodos C y D requieren comprobaciones adicionales y se resumen en el diálogo de indicio.

## Fórmulas

### Método A: misma L

```text
Y = Wp − Wa
Z = (Y × 100) / Wa
```

### Método B: L distinta

```text
Y  = Wa × La
β  = Wp × Lp
α  = β − Y
Z  = (α × 100) / Y
```

Se utiliza el valor absoluto de `Z` para detectar tanto aumentos como reducciones.

## Criterio de valoración

| Resultado | Interpretación |
| --- | --- |
| `|Z| ≤ 4 %` | Dentro de los parámetros establecidos. |
| `|Z| > 4 %` | Indicio de manipulación. Traslado a centro técnico para la prueba de determinación de errores. |

El umbral del 4 % es un indicador de referencia, no un límite sancionador absoluto. Se justifica por los errores máximos de homologación (±1 %), el error máximo de distancia tras calibrado (±2 %, cap. III.2.1 del Anexo IB del Reglamento CEE 3821/85) y un margen adicional del 1 %. Por eso el 2 % no se usa como umbral de decisión independiente.

## Consecuencias de un indicio

- Realizar la prueba de determinación de errores en un centro técnico autorizado (art. 33.4 LOTT; diligencia debida según la disposición adicional 3.ª del RD 425/2005).
- Comprobar precintos, tamaño y dimensiones reales de los neumáticos, y posible cambio de grupo o caja de cambios.
- Si el tacógrafo registra descanso con el vehículo en marcha, revisar el sensor de movimiento y la posible presencia de un imán.
- Si el error se confirma, nuevo calibrado y denuncia al transportista. Los datos del centro técnico se incluyen en el boletín cuando el error se demuestra sobre los parámetros de la última intervención; no es necesario incluirlos cuando no hubo recalibrado tras el cambio de neumáticos o de grupo.

## Implementación

- **`ui/coeficiente/CoeficienteCaracteristicoViewModel.kt`**:
  - Mantiene el estado de los cuatro campos de entrada.
  - Expone la función pura `calcularResultado(wa, la, wp, lp)`, que selecciona el método A o B y devuelve un `CalculationResult` con los valores intermedios, `Z` y el estado (`DENTRO_DE_PARAMETROS` o `INDICIO_MANIPULACION`).
  - Valida que los valores sean positivos antes de calcular.
- **`ui/coeficiente/CoeficienteCaracteristicoScreen.kt`**:
  - Agrupa los campos en **Activación del tacógrafo** (`Wa`, `La`) y **Último calibrado periódico** (`Wp`, `Lp`).
  - Muestra el método aplicado, los valores intermedios y `Z`.
  - En caso de indicio, muestra las comprobaciones recomendadas y la acción requerida en centro técnico.
  - Incluye un aviso de que el resultado es orientativo y de que la decisión corresponde al agente actuante.

## Tests

Los casos cubiertos se encuentran en `app/src/test/java/com/oscar/consultareat/CoeficienteCaracteristicoTest.kt`:

- Misma `L`: se aplica el método A.
- Método A con `Wa=7660` y `Wp=7950`: `Z≈3,79 %`, dentro de parámetros.
- Método A con `Wa=7630` y `Wp=7950`: `Z≈4,19 %`, indicio.
- `L` distinta: se aplica el método B y se calculan `Y`, `β`, `α` y `Z`.
- Método B con `7630×2929` frente a `7650×3000`: `Z≈2,69 %`, dentro de parámetros.
- Método B con `7630×2929` frente a `7650×3050`: `Z≈4,40 %`, indicio.
- `Z` exactamente igual a 4 %: dentro de parámetros.
- `Z` negativa con valor absoluto superior a 4 %: indicio.

Para ejecutarlos:

```bash
./gradlew :app:testDebugUnitTest --tests "com.oscar.consultareat.CoeficienteCaracteristicoTest"
```

## Limitaciones

- La aplicación no sustituye la valoración del agente ni la prueba de determinación de errores.
- La metodología original cita el Reglamento (CEE) 3821/85, sustituido por el Reglamento (UE) 165/2014. Deben verificarse las equivalencias de capítulos y la normativa vigente.
- La pantalla no procesa ficheros `.ddd`/`.tgd`: los datos se introducen manualmente desde los registros de calibración.
