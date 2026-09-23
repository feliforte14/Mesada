// Mesada — lectura de peso transmitida por BLE (GATT notify).
// Requiere el calibrationFactor obtenido en 01_calibracion.
// UUIDs provisorios — hay que coordinarlos con el cliente BLE de la app Android
// una vez que se implemente (ver TODO en firmware/README.md).

#include "HX711.h"
#include <BLEDevice.h>
#include <BLEServer.h>
#include <BLEUtils.h>
#include <BLE2902.h>

const int PIN_DT = 4;
const int PIN_SCK = 5;

// TODO: reemplazar por el valor obtenido en 01_calibracion.ino
const float CALIBRATION_FACTOR = -7050.0;

#define SERVICE_UUID        "5b1e0001-1a2b-4c3d-9e8f-abc123456789"
#define WEIGHT_CHAR_UUID     "5b1e0002-1a2b-4c3d-9e8f-abc123456789"

HX711 scale;
BLECharacteristic *weightCharacteristic;
bool deviceConnected = false;

class ServerCallbacks : public BLEServerCallbacks {
  void onConnect(BLEServer *server) override { deviceConnected = true; }
  void onDisconnect(BLEServer *server) override {
    deviceConnected = false;
    server->startAdvertising(); // volver a anunciar para que la app pueda reconectar
  }
};

void setup() {
  Serial.begin(115200);

  scale.begin(PIN_DT, PIN_SCK);
  scale.set_scale(CALIBRATION_FACTOR);
  scale.tare();

  BLEDevice::init("Mesada-Balanza");
  BLEServer *server = BLEDevice::createServer();
  server->setCallbacks(new ServerCallbacks());

  BLEService *service = server->createService(SERVICE_UUID);
  weightCharacteristic = service->createCharacteristic(
      WEIGHT_CHAR_UUID,
      BLECharacteristic::PROPERTY_READ | BLECharacteristic::PROPERTY_NOTIFY);
  weightCharacteristic->addDescriptor(new BLE2902());

  service->start();
  server->getAdvertising()->start();

  Serial.println("Mesada — balanza BLE lista, esperando conexion...");
}

void loop() {
  float grams = scale.get_units(10);
  if (grams < 0.5 && grams > -0.5) grams = 0;

  if (deviceConnected) {
    char payload[16];
    snprintf(payload, sizeof(payload), "%.0f", grams);
    weightCharacteristic->setValue(payload);
    weightCharacteristic->notify();
  }

  Serial.print(grams, 0);
  Serial.println(" g");

  delay(500);
}
