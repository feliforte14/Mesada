# Mesada — asistente de cocina para pantalla táctil (Android · Kotlin · Compose)

App nativa para una tablet montada en la cocina: registro de comidas con macros, asistente por voz
con Claude (tool use), temporizador y sugerencias de cena según lo que falta del día.

## Cómo correrlo

1. Abrí la carpeta en **Android Studio** (Ladybug o más nuevo). Si pide generar el Gradle Wrapper, aceptá.
2. Copiá `local.properties.example` como `local.properties` y completá `sdk.dir` y `anthropic.apiKey`.
3. Corré en una tablet o en un emulador de tablet en horizontal (ej. *Pixel Tablet*, API 34+).
4. El emulador necesita micrófono habilitado (*Extended controls → Microphone → Virtual microphone uses host audio input*)
   y la app de Google instalada para el reconocimiento de voz.

> Las versiones de las dependencias son de fines de 2024 y compatibles entre sí. Android Studio te va a sugerir
> actualizarlas; hacelo de a una y compilá entre cambios.

## Arquitectura

```
app/src/main/java/com/mesada/app/
├── MesadaApp.kt            Contenedor de dependencias (DB, repo, cliente de Claude)
├── MainActivity.kt         Pantalla siempre encendida + modo inmersivo
├── MesadaViewModel.kt      Estado de la UI, registro manual y orquestación voz ↔ asistente
├── data/
│   ├── Foods.kt            Catálogo, unidades (g / ml / unidades), macros
│   ├── Repository.kt       Único punto de acceso a datos; DayState derivado
│   └── db/Database.kt      Room: entries, days (pasos), goals
├── domain/
│   ├── KitchenTimer.kt     Temporizador con alarma
│   └── MealIdeas.kt        Sugerencias según kcal y proteína restantes
├── assistant/
│   ├── ClaudeClient.kt     POST /v1/messages (OkHttp)
│   ├── AssistantTools.kt   Herramientas que Claude puede usar y su ejecución
│   └── Assistant.kt        Bucle de tool use + historial corto de la conversación
├── voice/
│   ├── SpeechInput.kt      SpeechRecognizer → Flow (parciales y resultado final)
│   └── Speaker.kt          TextToSpeech en es-AR
└── ui/                     Compose: Hoy, Agregar, Cocina y panel del asistente
```

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
2. Palabra de activación ("Hola Mesada") para no tocar la pantalla con las manos sucias.
3. Balanza Bluetooth (BLE) para cargar gramos automáticamente.
4. Escáner de códigos de barras con CameraX + ML Kit y base de Open Food Facts.
5. Tests: `AssistantTools` y `MealIdeas` son lógica pura, fáciles de cubrir con JUnit.
