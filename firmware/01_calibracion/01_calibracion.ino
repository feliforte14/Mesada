// Mesada — calibración de la celda de carga.
// Correr con la bandeja vacía. El sketch tara a cero solo. Después escribí "t" por
// Serial para tarar de nuevo en cualquier momento, o poné un peso conocido y ajustá
// CALIBRATION_FACTOR_GUESS hasta que la lectura coincida con el peso real.

#include "HX711.h"

const int PIN_DT = 4;
const int PIN_SCK = 5;

// Punto de partida típico para celdas de barra de 5kg; se ajusta a mano según la lectura.
float calibrationFactor = -7050.0;

HX711 scale;

void setup() {
  Serial.begin(115200);
  Serial.println("Mesada — calibracion de celda de carga");
  Serial.println("Dejar la bandeja vacia. Tarando...");

  scale.begin(PIN_DT, PIN_SCK);
  scale.set_scale();
  scale.tare();

  Serial.println("Listo. Poné un peso conocido y compará contra la lectura.");
  Serial.println("Comandos: '+' / '-' ajustan el factor de calibracion, 't' tara de nuevo.");
}

void loop() {
  scale.set_scale(calibrationFactor);

  Serial.print("Lectura: ");
  Serial.print(scale.get_units(5), 1);
  Serial.print(" g   |   factor actual: ");
  Serial.println(calibrationFactor);

  if (Serial.available()) {
    char c = Serial.read();
    if (c == '+') calibrationFactor += 10;
    else if (c == '-') calibrationFactor -= 10;
    else if (c == 't') scale.tare();
  }

  delay(300);
}
