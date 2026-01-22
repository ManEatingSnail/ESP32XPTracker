# ESP32 XP Tracker

Sends real-time XP tracking data to an ESP32 device with TFT display.

## Hardware

- ESP32 development board
- ILI9341 2.8" 240x320 TFT display (SPI)

### Wiring

| ESP32 Pin | Display Pin |
|-----------|-------------|
| GPIO 23   | MOSI        |
| GPIO 18   | SCK         |
| GPIO 15   | CS          |
| GPIO 2    | DC          |
| GPIO 4    | RST         |
| 3.3V      | VCC         |
| GND       | GND         |

## Setup

### 1. Flash ESP32

1. Install [Arduino IDE](https://www.arduino.cc/en/software)
2. Add ESP32 board support:
    - File → Preferences → Additional Board Manager URLs:
```
   https://raw.githubusercontent.com/espressif/arduino-esp32/gh-pages/package_esp32_index.json
```
3. Install libraries (Sketch → Include Library → Manage Libraries):
    - `TFT_eSPI` by Bodmer
    - `ArduinoJson` by Benoit Blanchon
4. Configure TFT_eSPI:
    - Edit `Arduino/libraries/TFT_eSPI/User_Setup.h`
    - Uncomment `#define ILI9341_DRIVER`
    - Set pins to match wiring above
5. Download [esp32_xp_tracker.ino](https://github.com/ManEatingSnail/ESP32TrackerFirmware) from this repository
6. Update WiFi credentials in the sketch
7. Upload to ESP32
8. Note the IP address shown on screen

### 2. Configure Plugin

1. Enable "ESP32 XP Tracker" in RuneLite
2. Enter ESP32 IP address in plugin settings
3. (Optional) Check skills to ignore in "Ignored Skills"

## Features

- Real-time XP, XP/hour, actions/hour
- Progress bar and time to level
- Skill filtering
- Local network only (no external servers)

## Troubleshooting

**Can't connect:**
- Verify ESP32 and computer on same WiFi
- Check IP address matches ESP32 screen
- Test by opening `http://YOUR_ESP32_IP` in browser

**Wrong data:**
- Train for 30-60 seconds to stabilize
- Check skill isn't in "Ignored Skills"

## Privacy

- Local network only
- No external servers
- Only XP stats transmitted
- No account data sent
```
