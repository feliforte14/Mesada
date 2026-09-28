# Nomi — asistente de cocina y nutrición para tablet (Android · Kotlin · Compose)

App nativa **solo para tablet** (pantalla ancha, layouts de dos columnas): registro de comidas con macros,
alimentos y recetas propios, historial, ejercicio, recompensas, asistente por voz con Claude (tool use),
temporizador y sugerencias de comida según lo que falta del día.

> **Tablet-only.** El layout no es responsive a ancho de teléfono a propósito (ver Onboarding, Agregar).
> Probar siempre en un emulador de tablet (*Pixel Tablet*, API 34+) o en el dispositivo real, nunca en un
> emulador de teléfono — el diseño se ve roto/apretado ahí y no es un bug de la app.

## Cómo correrlo

1. Abrí la carpeta en **Android Studio**. Si pide generar el Gradle Wrapper, aceptá.
2. Copiá `local.properties.example` como `local.properties` y completá `sdk.dir` y `anthropic.apiKey`.
3. Corré en una tablet o en un **emulador de tablet** en horizontal (ej. *Pixel Tablet*, API 34+).
4. El emulador necesita micrófono habilitado (*Extended controls → Microphone → Virtual microphone uses host audio input*)
   y la app de Google instalada para el reconocimiento de voz.

> Las versiones de las dependencias se van actualizando con el Upgrade Assistant de Android Studio.
> Aceptalas de a una y compilá entre cambios para aislar cualquier rotura.

## Arquitectura

```
app/src/main/java/com/mesada/app/
├── MesadaApp.kt            Contenedor de dependencias (DB, repo, cliente de Claude)
├── MainActivity.kt         Pantalla siempre encendida + modo inmersivo
├── MesadaViewModel.kt      Estado de la UI, registro manual y orquestación voz ↔ asistente
├── data/
│   ├── Foods.kt            Catálogo, unidades (g / ml / unidades), macros
│   ├── Repository.kt       Único punto de acceso a datos; DayState/DaySummary derivados
│   └── db/Database.kt      Room: entries, days (pasos), goals, alimentos y recetas propios
├── domain/
│   ├── KitchenTimer.kt     Temporizador con alarma
│   ├── MealIdeas.kt        Recetas fijas + parseo de recetas propias
│   ├── GoalsCalculator.kt  BMR/objetivos, ActivityLevel, WeightGoal, kcal quemadas por pasos
│   └── Rewards.kt          Cálculo de logros (Recompensas) a partir de DayState/historial
├── export/
│   └── HistoryPdf.kt       Exporta el historial a PDF y lo manda por mail
├── assistant/
│   ├── ClaudeClient.kt     POST /v1/messages (OkHttp)
│   ├── AssistantTools.kt   Herramientas que Claude puede usar y su ejecución
│   └── Assistant.kt        Bucle de tool use + historial corto de la conversación
├── voice/
│   ├── SpeechInput.kt      SpeechRecognizer → Flow (parciales y resultado final)
│   └── Speaker.kt          TextToSpeech en es-AR
└── ui/
    ├── theme/Theme.kt      Colores, tipografía y formas (Material3); Palette con acentos por comida
    ├── MealStyle.kt        Ícono + color de identidad por comida, compartido entre Hoy y Evolución
    ├── Components.kt       Panel, CalorieRing, MacroBar, RoundButton, helpers de formato
    └── screens/            Onboarding, Hoy, Agregar, Cocina, Ejercicio, Evolución, Logros, Perfil
```

La balanza Bluetooth (BLE) se implementó y después se **pausó**: el código completo (hardware/, firmware/
del ESP32, integración en la UI) vive en la rama `feature/balanza-ble`, fuera de `main`.

**Flujo de voz:** micrófono → `SpeechInput` (texto) → `Assistant` manda el pedido + el estado del día a Claude →
Claude pide herramientas (`agregar_alimento`, `agregar_personalizado`, `quitar_alimento`, `resumen_del_dia`,
`iniciar_temporizador`, `cambiar_pasos`) → la app las ejecuta contra Room → Claude responde → `Speaker` lo lee.
Como la UI observa Room con `Flow`, la pantalla se actualiza sola cuando el asistente registra algo.

## Seguridad de la clave

Poner la clave en `local.properties` es **solo para desarrollo**: termina dentro del APK.
Para producción, levantá un backend mínimo que exponga `POST /v1/messages`, agregue la clave del lado del
servidor (y límites por dispositivo), y configurá `assistant.baseUrl` apuntando ahí con `anthropic.apiKey` vacío.

## Modo kiosco (dispositivo dedicado)

- **Rápido:** *Fijar pantalla* (Ajustes → Seguridad) deja la app anclada.
- **Real:** registrá la app como *Device Owner* y usá `startLockTask()` para que no se pueda salir.
  Con `adb shell dpm set-device-owner com.mesada.app/.AdminReceiver` (requiere agregar un `DeviceAdminReceiver`).

## Próximos pasos sugeridos

1. Backend proxy para la clave (Ktor o Cloud Functions).
2. Palabra de activación ("Hola Nomi") para no tocar la pantalla con las manos sucias.
3. Retomar la balanza Bluetooth (rama `feature/balanza-ble`) para cargar gramos automáticamente.
4. Escáner de códigos de barras con CameraX + ML Kit y base de Open Food Facts.
5. Tests: `AssistantTools`, `MealIdeas`, `GoalsCalculator` y `Rewards` son lógica pura, fáciles de cubrir con JUnit.
