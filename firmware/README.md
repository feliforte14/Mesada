# Firmware balanza — ESP32 + HX711

Prototipo de la balanza integrada de Mesada. Lee peso de una celda de carga vía HX711
y lo transmite por BLE (más adelante) para que la app Android lo consuma.

## Hardware

- ESP32 DevKit (cualquier variante genérica con USB)
- Celda de carga tipo barra, 5 kg
- Módulo HX711

## Wiring

| HX711 | ESP32 |
|---|---|
| VCC | 3.3V |
| GND | GND |
| DT  | GPIO 4 (configurable, ver `PIN_DT` en el sketch) |
| SCK | GPIO 5 (configurable, ver `PIN_SCK` en el sketch) |

La celda de carga se conecta a los pads E+/E-/A+/A- del HX711 según el rotulado del
módulo específico que compres (varía entre fabricantes, seguí el que trae impreso).

## Librerías necesarias (Arduino IDE → Library Manager)

- **HX711** de Bogdan Necula (buscar "HX711" — es la más usada y mantenida)
- Soporte de placa ESP32 (Boards Manager → agregar URL del core `esp32` de Espressif)

## Orden de uso

1. `01_calibracion/01_calibracion.ino` — corré esto primero. Con la bandeja vacía, tarala
   a cero, después poné un peso conocido (ej. 100 g) y anotá el `calibration_factor` que
   te tira. Ese número va hardcodeado en el siguiente sketch.
2. `02_lectura_serial/02_lectura_serial.ino` — lectura estable de gramos por Serial cada
   500 ms, usando el `calibration_factor` que sacaste en el paso 1. Confirmá que el peso
   es estable y repetible antes de seguir.
3. `03_lectura_ble/03_lectura_ble.ino` — mismo que el paso 2, pero transmite el peso por
   BLE (GATT, característica notify) en vez de imprimirlo por Serial. Este es el que
   eventualmente habla con la app Android.

## Pendiente

- Definir UUID de servicio/característica BLE definitivos y documentarlos acá una vez
  que el cliente BLE de la app (Kotlin) esté implementado, para que ambos lados coincidan.
- Filtrado de ruido (promedio móvil) si la lectura BLE resulta más ruidosa que por Serial.
