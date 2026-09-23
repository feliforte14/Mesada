# Mesada — Documento técnico de seguimiento

Registro vivo de requerimientos, decisiones técnicas y fuentes, actualizado a medida que se
avanza. Formato: una entrada por sesión de trabajo, ordenadas de más reciente a más vieja.

---

## 2026-09-26 — Módulo de hardware opcional (balanza Bluetooth) en la app

### Requerimiento
- **RF-05**: la app tiene que poder usarse íntegramente sin comprar ni conectar ningún
  hardware (registro manual, catálogo, onboarding/objetivos, temporizador, sugerencias,
  asistente de voz con gramos estimados) **o** con la balanza Bluetooth conectada, a
  elección del usuario — no como dos builds separados, sino como un toggle en la misma app.

### Decisión técnica y por qué
| Decisión | Alternativa descartada | Motivo |
|---|---|---|
| Interfaz `ScaleSource` (`hardware/ScaleSource.kt`) con un solo estado `DISABLED` explícito, en vez de nullable `ScaleSource?` en el contenedor de dependencias | `AppContainer.scaleSource: ScaleSource?` | Con `DISABLED` como estado de la propia interfaz, la UI (`KitchenScreen`) siempre tiene algo que observar y mostrar, sin `if (scaleSource != null)` repartido por la UI. |
| `BleScaleSource.start()` chequea `HardwareSettings.scaleEnabled` **antes** de tocar cualquier API de `BluetoothManager`/`BluetoothAdapter` | Siempre escanear y depender de que la UI no llame a `start()` si está apagado | Refuerza la garantía en el propio módulo de hardware, no solo en quien lo llama — si en el futuro se agrega otro caller, no puede accidentalmente activar Bluetooth con el toggle apagado. |
| Toggle persistido en `SharedPreferences` (`HardwareSettings`) en vez de `DataStore` | `androidx.datastore` | No había ninguna dependencia de DataStore en el proyecto todavía; para un solo booleano, `SharedPreferences` alcanza sin sumar una librería nueva. |
| Permisos BLE (`BLUETOOTH_SCAN`/`BLUETOOTH_CONNECT`/`ACCESS_FINE_LOCATION`) declarados en el manifest pero pedidos en runtime recién cuando el usuario prende el switch | Pedirlos todos al abrir la app | Coherente con RF-05: si el usuario nunca activa la balanza, la app no debe ni pedir permisos de Bluetooth. |
| Todas las excepciones de BLE (`SecurityException` por permiso revocado a mitad de conexión, adapter apagado, etc.) atrapadas dentro de `BleScaleSource` y traducidas a `ScaleConnectionState.DISCONNECTED` | Dejar que floten y decidir en el ViewModel/UI | El estado de conexión ya es observable por Flow; no tiene sentido que un error de hardware opcional tire la UI abajo — se degrada a "no conectado" y el resto de la app sigue andando. |

### Archivos tocados
- `hardware/ScaleSource.kt` (nuevo) — interfaz + `ScaleConnectionState` (`DISABLED/DISCONNECTED/CONNECTING/CONNECTED`).
- `hardware/BleScaleSource.kt` (nuevo) — cliente BLE real: escanea por nombre `"Mesada-Balanza"`,
  se conecta al servicio/característica con los mismos UUIDs que `firmware/03_lectura_ble.ino`,
  suscribe a notify y parsea los gramos recibidos como texto.
- `data/HardwareSettings.kt` (nuevo) — toggle persistido (`SharedPreferences`).
- `MesadaApp.kt` — `AppContainer` arma `hardwareSettings` + `scaleSource` (siempre un `BleScaleSource`,
  pero inerte si el toggle está apagado).
- `MesadaViewModel.kt` — expone `scaleEnabled/scaleConnection/scaleGrams` y `setScaleEnabled()`;
  arranca el `ScaleSource` en `init` si ya estaba activado, lo detiene en `onCleared`.
- `ui/screens/KitchenScreen.kt` — panel nuevo "Balanza Bluetooth" con switch y estado en texto.
- `ui/MesadaRoot.kt` — pide los permisos BLE correctos según versión de Android (`BLUETOOTH_SCAN`/
  `BLUETOOTH_CONNECT` en API 31+, `ACCESS_FINE_LOCATION` antes) solo al activar el switch.
- `AndroidManifest.xml` — permisos BLE + `<uses-feature android:required="false">` (no bloquea
  instalar en dispositivos sin BLE).

### Verificación
- `./gradlew :app:compileDebugKotlin` compila sin errores con los cambios.
- **No probado en dispositivo/emulador** ni con hardware real conectado — sigue pendiente lo mismo
  que ya estaba anotado en sesiones previas.

### Pendiente / próximos pasos anotados
- El panel de Cocina solo muestra el peso medido como información (`"X g en la bandeja"`); todavía
  no está conectado al flujo de registrar comida — ni `AddScreen` ni `agregar_alimento` del asistente
  usan `scaleGrams` para reemplazar la estimación por voz. Es el paso obvio que sigue.
- `BleScaleSource` asume que el dispositivo BLE ya está emparejado/en rango y con el nombre exacto
  `"Mesada-Balanza"` (fijado en el firmware) — no se probó el escaneo real, solo revisado por lectura.
- Sin manejo de reconexión automática si la balanza se apaga y se prende de nuevo a mitad de sesión
  más allá de lo que ya hace `onConnectionStateChange` (vuelve a `DISCONNECTED`, no reintenta el scan solo).

---

## 2026-09-25 — Diseño del producto físico (tablet + balanza) y firmware inicial

Sesión sin cambios en la app Android. Foco en el hardware: se subió el proyecto a
GitHub (`github.com/feliforte14/Mesada`, repo previamente vacío, primer commit con
los 36 archivos existentes) y se avanzó el diseño del producto físico que hasta ahora
solo estaba anotado como pendiente en el README ("Balanza Bluetooth (BLE)").

### Decisiones de diseño físico y por qué
| Decisión | Alternativa descartada | Motivo |
|---|---|---|
| Balanza y mástil de la tablet como estructuras **mecánicamente separadas**, aunque compartan gabinete | Un solo cuerpo moldeado/soldado entre bandeja y mástil | Las celdas de carga son sensibles a microvibración: si el mástil transmite su peso o el toque en pantalla a la misma plancha que sostiene la celda, la lectura de peso se ensucia con cada interacción táctil. |
| Mástil **desmontable** (plug + socket con gasket de goma/silicona amortiguante) | Mástil fijo atornillado al gabinete | Permite lavar la bandeja de pesado por separado (uso en cocina = contacto con comida) sin desarmar toda la unidad; el gasket evita que "desmontable" reintroduzca acople rígido de vibración. |
| Boceto conceptual generado con Gemini (imagen, no CAD) para validar la idea antes de modelar | Pasar directo a CAD/3D | Más rápido para iterar sobre la disposición general (bandeja + mástil + socket) antes de comprometerse a medidas reales. |

### Prototipo de hardware — alcance definido
Lista de compras y plan de armado para un primer prototipo **funcional, no estético**
(sin gabinete desmontable todavía — eso queda para una iteración mecánica posterior,
una vez validada la lectura de peso):
- ESP32 DevKit, celda de carga de barra 5kg, módulo HX711, protoboard, jumpers.
- Bandeja provisoria (tabla de cortar / MDF / acrílico), sin gabinete definitivo.
- Secuencia de armado: (1) validar el ESP32 solo con Blink, (2) cablear HX711↔celda
  según el rotulado del módulo comprado (varía por fabricante, no hay un pinout
  universal), (3) cablear HX711↔ESP32, (4) calibrar con peso conocido, (5) recién
  después agregar BLE — separado a propósito para no mezclar problemas de sensor
  con problemas de conectividad al debuggear.

### Firmware inicial (nuevo, `firmware/`)
Carpeta nueva en la raíz del repo, fuera del proyecto Gradle (no es código Android,
es firmware Arduino/ESP32 independiente):
- `firmware/README.md` — pinout HX711↔ESP32, librerías requeridas (`HX711` de Bogdan
  Necula, core de placas ESP32), orden de uso de los tres sketches.
- `firmware/01_calibracion/` — sketch interactivo por Serial para tarar y encontrar
  el `calibration_factor` de la celda de carga específica que se compre (`+`/`-`
  ajustan en vivo, `t` tara). Este factor **no es portable entre celdas individuales**,
  hay que recalibrar por cada unidad física.
- `firmware/02_lectura_serial/` — lectura estable de gramos por Serial (promedio de
  10 muestras, umbral de ruido cerca de cero), usando el factor ya calibrado.
- `firmware/03_lectura_ble/` — mismo sensado, transmitido por BLE (servicio GATT con
  característica notify). UUIDs de servicio/característica son **provisorios**,
  quedan pendientes de coordinar con el cliente BLE del lado Android.

### Pendiente / próximos pasos anotados
- Implementar el cliente BLE en Kotlin (decidir dónde vive en la arquitectura —
  candidato: paquete nuevo `hardware/` junto a `assistant/` y `voice/`).
- Fijar UUIDs de servicio/característica BLE definitivos en ambos lados (firmware y app).
- `calibration_factor` de arranque en los sketches (-7050) es un valor típico de
  referencia para celdas de 5kg, no calibrado — se ajusta recién con la celda real
  en mano, siguiendo `01_calibracion`.
- Iteración mecánica del gabinete desmontable (grosor real del conector/espiga,
  el boceto de Gemini lo mostró demasiado fino tipo plug USB-C para soportar el peso
  del mástil + tablet) — queda para después de validar el sensado.
- Diseño físico no llegó a CAD ni a medidas reales todavía, solo boceto conceptual.

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
