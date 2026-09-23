# Mesada — Documento técnico de seguimiento

Registro vivo de requerimientos, decisiones técnicas y fuentes, actualizado a medida que se
avanza. Formato: una entrada por sesión de trabajo, ordenadas de más reciente a más vieja.

---

## 2026-09-24 — Onboarding de objetivos + revisión de robustez + catálogo

### Requerimientos funcionales agregados
- **RF-01**: la app debe calcular los objetivos diarios (kcal, proteína, hidratos, grasas) a
  partir de datos de la persona (peso, altura, edad, sexo biológico, nivel de actividad,
  objetivo de peso), en vez de arrancar con un default genérico fijo.
- **RF-02**: debe pedirse ese dato la primera vez que se abre la app (onboarding bloqueante:
  no se puede usar el resto de la app sin completarlo).
- **RF-03**: tiene que poder recalcularse más adelante (si cambia el peso o el objetivo) sin
  perder el resto de los datos ya registrados — botón "Recalcular" en la pantalla Cocina.
- **RF-04**: "empanada" debe existir como alimento del catálogo con valores fijos, porque el
  propio asistente de voz la usa como ejemplo de uso (`AssistantPanel.HINTS`) pero no estaba
  cargada — el usuario terminaba siempre en el camino de `agregar_personalizado` (estimado por
  Claude en cada pedido, sin consistencia entre sí).

### Requerimientos no funcionales
- **RNF-01 (memoria)**: `Assistant.history` no debe crecer sin límite — la app corre indefinidamente
  en un dispositivo kiosco que no se reinicia seguido (a diferencia de un celular común), así que
  una fuga de memoria lenta sí importa acá aunque en un uso típico de celular pasara desapercibida.
- **RNF-02 (persistencia sin pérdida de datos)**: agregar la tabla `profile` a la base Room no
  puede borrar `entries`/`days`/`goals` ya guardados — requiere una migración real (`MIGRATION_1_2`),
  no `fallbackToDestructiveMigration()`.
- **RNF-03 (consistencia del asistente)**: los alimentos que el asistente de voz vaya a usar como
  ejemplo en sus propios hints deben existir en el catálogo con valores fijos, para que la
  experiencia sea reproducible (mismo alimento → mismas macros siempre).

### Decisiones técnicas y por qué
| Decisión | Alternativa descartada | Motivo |
|---|---|---|
| Fórmula **Mifflin-St Jeor** para el metabolismo basal | Harris-Benedict | Es la que hoy recomiendan la mayoría de las guías clínicas por ser más precisa para población general — Harris-Benedict tiende a sobreestimar. |
| `ProfileEntity` separada de `GoalsEntity` | Guardar el cálculo ya hecho y listo, sin guardar los inputs | Si no se guardan peso/altura/edad/etc., no se puede "recalcular" después — solo se podría volver a pedir todo de cero. |
| `Migration(1, 2)` explícita con `CREATE TABLE` | `fallbackToDestructiveMigration()` | La destructiva borra `entries`/`days`/`goals` existentes en cualquier instalación que ya tenga datos — inaceptable ni siquiera en la etapa personal, una vez que hay registros reales cargados. |
| `sealed interface ProfileLoadState { Loading / Loaded }` en vez de `ProfileEntity?` directo | Exponer `StateFlow<ProfileEntity?>` a secas | Sin este distingo, hay un instante en el arranque donde "todavía no se consultó la base" y "se consultó y no hay perfil" son indistinguibles → parpadeo del onboarding en cada apertura de la app aunque el perfil ya exista. |
| Recorte manual de `Assistant.history` a 16 turnos | Dejarlo crecer y confiar en que `takeLast(8)` alcance | `takeLast(8)` solo acota lo que se **manda** a Claude en cada request: la lista en memoria seguía creciendo sin límite mientras el proceso viviera. |

### Tecnologías empleadas (nuevas en esta sesión)
- Room `Migration` API (`androidx.room.migration.Migration`, `SupportSQLiteDatabase`) para el
  versionado de esquema (`version = 1 → 2`).
- Sin dependencias nuevas: todo con lo que ya estaba en `build.gradle.kts`.

### Fuentes de información
- **Mifflin-St Jeor**: fórmula estándar de estimación de metabolismo basal, ampliamente citada en
  literatura de nutrición clínica desde su publicación original (Mifflin MD, St Jeor ST, et al.,
  *American Journal of Clinical Nutrition*, 1990). Aplicada de memoria por el asistente (Claude);
  **no se verificó contra la fuente primaria en esta sesión** — antes de un uso más allá de lo
  personal, vale la pena contrastarla contra una calculadora clínica de referencia.
- **Valores nutricionales del catálogo** (`Foods.kt`, incluidos los 4 alimentos agregados hoy:
  empanada, milanesa, dulce de leche, factura): estimados por el asistente a partir de tablas
  nutricionales típicas que tiene en su entrenamiento (aprox. USDA FoodData Central para los
  genéricos, estimaciones propias para las preparaciones argentinas sin entrada directa en USDA
  como la empanada). **No son de una fuente verificable citable** — quedó pendiente en el roadmap
  una pasada sistemática contra una fuente oficial (USDA FoodData Central o tablas INTA) antes de
  confiar en ellos para algo más que una estimación aproximada de uso personal.
- **Niveles de actividad y sus factores multiplicadores** (1.2 / 1.375 / 1.55 / 1.725 / 1.9):
  son los factores de actividad estándar que acompañan a Mifflin-St Jeor en la mayoría de las
  calculadoras de TDEE (Total Daily Energy Expenditure) de uso general — misma salvedad que
  arriba, no verificado contra una fuente primaria específica en esta sesión.

### Archivos tocados
- `data/db/Database.kt` — `ProfileEntity`, `MesadaDao.profile()/profileOnce()/upsertProfile()`, `MIGRATION_1_2`, `version = 2`.
- `domain/GoalsCalculator.kt` (nuevo) — `Sex`, `ActivityLevel`, `WeightGoal`, `GoalsCalculator.compute()`.
- `data/Repository.kt` — `observeProfile()`, `saveProfile()`.
- `MesadaViewModel.kt` — `ProfileLoadState`, `profileState`, `editingProfile`, `editProfile()/cancelEditProfile()/saveProfile()`.
- `ui/screens/OnboardingScreen.kt` (nuevo).
- `ui/MesadaRoot.kt` — ruteo entre onboarding y `MesadaMain` según `profileState`.
- `ui/screens/KitchenScreen.kt` — botón "Recalcular" en el panel de objetivos.
- `assistant/Assistant.kt` — recorte de `history`.
- `data/Foods.kt` — 4 alimentos nuevos (empanada, milanesa, dulce de leche, factura).

### Pendiente / próximos pasos anotados
- Verificar la fórmula Mifflin-St Jeor y los factores de actividad contra una fuente primaria.
- Pasada sistemática de precisión nutricional del catálogo completo (28 alimentos) contra
  USDA FoodData Central o tablas INTA.
- No probado aún en dispositivo/emulador real (queda para la próxima sesión — se estaba
  descargando el AVD Pixel Tablet API 34+ Google Play al cierre de esta sesión).

---

## 2026-09-23 — Lectura inicial del proyecto

Primera revisión completa del código existente (~1930 líneas, 19 archivos Kotlin) antes de
seguir desarrollando. Sin cambios de código en esta sesión, solo diagnóstico.

### Estado encontrado
Proyecto ya bastante implementado (no solo scaffold): catálogo de 24 alimentos, Room con 3
entidades (`entries`, `days`, `goals`), integración con Claude (Messages API + tool use, 6
herramientas), reconocimiento de voz + TTS en es-AR, UI completa en Compose (3 pantallas +
panel de asistente). Sin repo git todavía, sin Gradle Wrapper generado, sin `local.properties`.

### Decisiones de arquitectura ya presentes (no tomadas en esta sesión, documentadas por referencia)
- 3 capas: `data/` (Room + repositorio), `domain/` (lógica pura: temporizador, sugerencias),
  `assistant/` + `voice/` (integración externa), `ui/` (Compose).
- DI manual simple (`AppContainer` en `MesadaApp.kt`) — decisión explícita del propio autor
  original en comentario de código: "suficiente para un prototipo; migrable a Hilt".
- Clave de Anthropic en `local.properties` → `BuildConfig` — marcado en el propio README como
  "solo para desarrollo", con el paso de backend proxy ya anotado como pendiente.

### Roadmap de producto discutido (sin implementar)
Dos caminos divergentes según el objetivo:
- **Uso personal** (actual): alcanza tal cual está.
- **Producto vendible**: primero validar como software (el cliente usa su propia tablet,
  modelo de suscripción), no como hardware propio — bloqueante real antes de cualquier segundo
  usuario: backend proxy para la clave de Anthropic (hoy viaja dentro del APK).

Hardware para uso personal: tablet Android 10-11" con Google Play de fábrica (Galaxy Tab A9+,
Redmi Pad SE o Lenovo Tab M11) + soporte abatible para debajo de un mueble de cocina. Balanza:
evaluado DIY (ESP32 + HX711 + BLE propio) por sobre reverse-engineering de una balanza
comercial — priorizado para después de estabilizar la app en la tablet real.
