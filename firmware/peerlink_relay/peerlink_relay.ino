/*
 * PeerLink Relay Firmware for ESP32
 *
 * Hardware: ESP-WROOM-32
 * Features:
 * - Wi-Fi Access Point (SSID: PeerLink_Relay_XXXX)
 * - UDP Relay on Port 8888
 * - Client Discovery & Tracking
 *
 * Dependencies:
 * - ArduinoJson (Install via Library Manager)
 */

#include <ArduinoJson.h>
#include <WiFi.h>
#include <WiFiUdp.h>
#include <map>

// Hardware Config (DevKit V1)
#define LED_PIN 2
const int UDP_PORT = 8888;
const unsigned long BROADCAST_INTERVAL = 2000; // 2 Seconds
const unsigned long CLIENT_TIMEOUT = 30000;    // 30 Seconds

// Globals
WiFiUDP udp;
unsigned long lastBroadcast = 0;
String mySSID;

struct ClientInfo {
  IPAddress ip;
  unsigned long lastSeen;
  String name;
};

std::map<String, ClientInfo> clients; // Map<DeviceId, Info>

void setup() {
  Serial.begin(115200);
  pinMode(LED_PIN, OUTPUT);
  digitalWrite(LED_PIN, HIGH); // ON during setup

  // 1. Setup AP
  uint8_t mac[6];
  WiFi.macAddress(mac);
  String macStr = String(mac[4], HEX) + String(mac[5], HEX);
  macStr.toUpperCase();

  mySSID = "PeerLink_" + macStr;

  WiFi.mode(WIFI_AP);
  // Using default password "peerlink123" - Android app is configured for this
  WiFi.softAP(mySSID.c_str(), "peerlink123");

  Serial.println("-------------------------");
  Serial.println("PeerLink Relay Online");
  Serial.println("SSID: " + mySSID);
  Serial.println("IP:   " + WiFi.softAPIP().toString());
  Serial.println("-------------------------");

  // 2. Setup UDP
  udp.begin(UDP_PORT);

  digitalWrite(LED_PIN, LOW); // Setup complete
}

void loop() {
  // 1. Broadcast Presence
  if (millis() - lastBroadcast > BROADCAST_INTERVAL) {
    broadcastPresence();
    lastBroadcast = millis();
    cleanupClients();
  }

  // 2. Handle Packets
  int packetSize = udp.parsePacket();
  if (packetSize) {
    digitalWrite(LED_PIN, HIGH); // Activity blink
    char packetBuffer[4096];
    int len = udp.read(packetBuffer, 4095);
    if (len > 0) {
      packetBuffer[len] = 0;
    }

    // Serial.printf("Received: %s\n", packetBuffer);
    handlePacket(String(packetBuffer), udp.remoteIP());
    digitalWrite(LED_PIN, LOW);
  }
}

void broadcastPresence() {
  // Send to Broadcast Address
  // In AP mode, broadcast is usually X.X.X.255
  // But strictly, 255.255.255.255 works too.

  StaticJsonDocument<256> doc;
  doc["type"] = "presence";
  doc["from"] = "ESP_RELAY";
  doc["payload"] = mySSID;

  String output;
  serializeJson(doc, output);

  udp.beginPacket(IPAddress(255, 255, 255, 255), UDP_PORT);
  udp.write((const uint8_t *)output.c_str(), output.length());
  udp.endPacket();
}

void handlePacket(String json, IPAddress senderIP) {
  StaticJsonDocument<1024> doc;
  DeserializationError error = deserializeJson(doc, json);

  if (error) {
    Serial.println("JSON Parse Error");
    return;
  }

  String type = doc["type"];
  String from = doc["from"];
  String to = doc["to"];

  // Register Client
  if (from != "null" && from.length() > 0) {
    String payload = doc["payload"] | "";
    // If packet is presence from phone, payload might be name
    String name = (type == "presence")
                      ? payload
                      : (clients.count(from) ? clients[from].name : "Unknown");

    clients[from] = {senderIP, millis(), name};
    // Serial.println("Registered/Updated: " + from);
  }

  // Handle Logic
  if (type == "chat") {
    // Forwarding
    if (to == "BROADCAST") {
      // Send to all except sender
      for (auto const &[id, info] : clients) {
        if (id != from) {
          forwardPacket(json, info.ip);
        }
      }
    } else {
      // Send to specific
      if (clients.count(to)) {
        forwardPacket(json, clients[to].ip);
      } else {
        Serial.println("Target not found: " + to);
      }
    }
  } else if (type == "get_clients") {
    sendClientList(senderIP);
  }
}

void forwardPacket(String json, IPAddress ip) {
  udp.beginPacket(ip, UDP_PORT);
  udp.write((const uint8_t *)json.c_str(), json.length());
  udp.endPacket();
}

void sendClientList(IPAddress targetIp) {
  // Format: "id,name;id,name"
  String payload = "";
  for (auto const &[id, info] : clients) {
    if (payload.length() > 0)
      payload += ";";
    payload += id + "," + info.name;
  }

  StaticJsonDocument<512> doc;
  doc["type"] = "clients_list";
  doc["from"] = "ESP_RELAY";
  doc["to"] = "REQUESTER";
  doc["payload"] = payload;

  String output;
  serializeJson(doc, output);

  udp.beginPacket(targetIp, UDP_PORT);
  udp.write((const uint8_t *)output.c_str(), output.length());
  udp.endPacket();
}

void cleanupClients() {
  // Remove timed out clients
  // Manual iterator needed for map removal
  auto it = clients.begin();
  while (it != clients.end()) {
    if (millis() - it->second.lastSeen > CLIENT_TIMEOUT) {
      Serial.println("Client Timeout: " + it->first);
      it = clients.erase(it);
    } else {
      ++it;
    }
  }
}
