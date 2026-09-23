// Mesada — lectura estable de peso por Serial.
// Requiere el calibrationFactor obtenido en 01_calibracion.

#include "HX711.h"

const int PIN_DT = 4;
const int PIN_SCK = 5;

// TODO: reemplazar por el valor obtenido en 01_calibracion.ino
const float CALIBRATION_FACTOR = -7050.0;

HX711 scale;

void setup() {
  Serial.begin(115200);
  scale.begin(PIN_DT, PIN_SCK);
  scale.set_scale(CALIBRATION_FACTOR);
  scale.tare();
  Serial.println("Mesada — lectura de peso lista.");
}

void loop() {
  float grams = scale.get_units(10); // promedio de 10 lecturas
  if (grams < 0.5 && grams > -0.5) grams = 0; // ruido cerca de cero

  Serial.print(grams, 0);
  Serial.println(" g");

  delay(500);
}
