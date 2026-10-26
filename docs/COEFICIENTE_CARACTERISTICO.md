# Coeficiente característico

Este documento describe el cálculo del coeficiente característico del tacógrafo y la detección de fraude implementada en la pantalla `CoeficienteCaracteristicoScreen`.

## Objetivo

Detectar posibles manipulaciones del tacógrafo comparando la relación entre el coeficiente característico (`W`) y la circunferencia efectiva de los neumáticos (`L`) en dos momentos: la activación del tacógrafo y el último calibrado periódico.

## Datos de entrada

| Campo | Descripción |
| --- | --- |
| `Wa` | Coeficiente característico del tacógrafo en el momento de la activación. |
| `La` | Circunferencia efectiva de los neumáticos en el momento de la activación. |
| `Wp` | Coeficiente característico del último calibrado periódico. |
| `Lp` | Circunferencia efectiva de los neumáticos en el último calibrado periódico. |

Los cuatro valores deben ser numéricos y mayores que cero. La pantalla acepta coma o punto como separador decimal.

## Fórmula

Si `La` y `Lp` son distintas:

```text
gamma = Wa × La
beta  = Wp × Lp
alfa  = beta − gamma
dseta = (alfa × 100) / gamma
```

Donde:

- `gamma` representa el producto de referencia calculado con los datos de activación.
- `beta` representa el producto calculado con los datos del último calibrado periódico.
- `alfa` es la diferencia entre ambos productos.
- `dseta` es la variación porcentual de `alfa` respecto a `gamma`.

## Criterio de fraude

- Si `|dseta| ≤ 4`, no se detecta fraude.
- Si `|dseta| > 4`, se detecta fraude.

Se utiliza el valor absoluto de `dseta` para detectar tanto aumentos como reducciones de la circunferencia efectiva superiores al 4 %.

## Caso especial: La igual a Lp

Si las dos circunferencias son iguales (con una tolerancia de `1e-6`), no existe variación que analizar. En ese caso:

- No se calculan `gamma`, `beta`, `alfa` ni `dseta`.
- El resultado se muestra directamente como **sin fraude detectado**.

## Implementación

- **`ui/coeficiente/CoeficienteCaracteristicoViewModel.kt`**:
  - Mantiene el estado de los cuatro campos de entrada.
  - Expone la función pura `calcularResultado(wa, la, wp, lp)`, que devuelve un `CalculationResult` con los valores intermedios y el veredicto.
  - Valida que los valores sean positivos antes de calcular.
- **`ui/coeficiente/CoeficienteCaracteristicoScreen.kt`**:
  - Agrupa los campos en **Activación del tacógrafo** (`Wa`, `La`) y **Último calibrado periódico** (`Wp`, `Lp`).
  - Muestra el resultado en una tarjeta y en un diálogo con `gamma`, `beta`, `alfa` y `dseta`.
  - En caso de fraude, muestra la acción requerida (traslado a un taller de verificación).

## Tests

Los casos cubiertos se encuentran en `app/src/test/java/com/oscar/consultareat/CoeficienteCaracteristicoTest.kt`:

- Circunferencias iguales: sin fraude y sin valores intermedios.
- Cálculo correcto de `gamma`, `beta`, `alfa` y `dseta`.
- `dseta` exactamente igual a 4: sin fraude.
- `dseta` superior a 4: fraude.
- `dseta` negativa con valor absoluto superior a 4: fraude.
- `dseta` negativa dentro del margen: sin fraude.

Para ejecutarlos:

```bash
./gradlew :app:testDebugUnitTest --tests "com.oscar.consultareat.CoeficienteCaracteristicoTest"
```
