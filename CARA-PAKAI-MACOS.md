# Cara Pakai Maestro macOS Driver

Maestro bisa dipakai buat testing aplikasi macOS desktop. Driver terdiri dari 2 bagian:
1. **macOS Sidecar** (Swift) — HTTP server yang jalan di host Mac, panggil Accessibility API + Core Graphics
2. **Kotlin Driver Client** — client yang komunikasi sama sidecar lewat HTTP

---

## Prasyarat

| Syarat | Versi |
|--------|-------|
| macOS | 13 (Ventura) ke atas |
| Xcode | 15+ (butuh Swift 5.9) |
| Java | 17+ |
| Gradle | bundled (./gradlew) |
| Accessibility permission | Terminal/IDE harus diizinkan di System Settings > Privacy & Security > Accessibility |
| Screen Recording permission | Terminal/IDE harus diizinkan di System Settings > Privacy & Security > Screen Recording |

Grant permission manual:

```
System Settings → Privacy & Security → Accessibility → [+] tambah Terminal.app
System Settings → Privacy & Security → Screen Recording → [+] tambah Terminal.app
```

---

## Build

### 1. Build Sidecar (Swift)

```bash
cd maestro/maestro-macos-sidecar
swift build
# Binary: .build/debug/MaestroMacOSDriver

# Production:
swift build -c release
# Binary: .build/release/MaestroMacOSDriver
```

### 2. Build Maestro CLI + Driver (Kotlin)

```bash
cd maestro
./gradlew :maestro-cli:installDist

# Atau install global:
./installLocally.sh
```

Setelah `installLocally.sh`, binary `maestro` tersedia di PATH.

---

## Konfigurasi

### Environment Variables

| Variable | Default | Keterangan |
|----------|---------|------------|
| `MAESTRO_MACOS_SIDECAR_PATH` | `../maestro-macos-sidecar/.build/debug/MaestroMacOSDriver` | Path ke binary sidecar |
| `MAESTRO_DRIVER_STARTUP_TIMEOUT` | `120000` (2 menit) | Timeout startup sidecar (ms) |
| `PORT` | `22088` | Port HTTP sidecar (jarang diganti) |

Development setup (dari root maestro):

```bash
export MAESTRO_MACOS_SIDECAR_PATH="$(pwd)/maestro-macos-sidecar/.build/debug/MaestroMacOSDriver"
```

Di luar development:

```bash
export MAESTRO_MACOS_SIDECAR_PATH="/path/ke/MaestroMacOSDriver"
```

---

## Cara Pakai

### Jalankan flow test dengan platform macOS

```bash
maestro test --platform macos flow.yaml
```

### Atau spesifik device

```bash
maestro test --device macos flow.yaml
```

### Atau start device dulu

```bash
# Terminal 1: start device
maestro start-device --platform macos

# Terminal 2: test
maestro test flow.yaml
```

### Studio (UI interactive)

```bash
maestro studio --platform macos
```

---

## Apa yang Terjadi saat Runtime

1. Maestro CLI detect `--platform macos`
2. `LocalMacOSSidecarInstaller` jalanin sidecar binary sebagai subprocess di port `22088`
3. Sidecar polling sampai `/status` OK (max 2 menit)
4. `MacOSDriverClient` siap — kirim HTTP request ke sidecar
5. Sidecar handle touch, swipe, input text, screenshot, view hierarchy via macOS API
6. Saat sesi selesai, sidecar di-kill otomatis

---

## Flow YAML yang Didukung

### launchApp / stopApp / killApp

```yaml
appId: org.reactjs.native.app
---
- launchApp
- tapOn: "Hello"
- assertVisible: "Hello"
- stopApp
```

`appId` = bundle identifier aplikasi macOS (cek di `Info.plist` atau Xcode).

### tapOn / longPressOn

```yaml
- tapOn: "Submit"
- longPressOn:
    text: "Delete"
    duration: 2000
```

### inputText / eraseText

```yaml
- tapOn: "Search"
- inputText: "halo dunia"
- eraseText: 5
- pressKey: Enter
```

### swipe / scroll

```yaml
- swipe:
    direction: UP
    duration: 500
- scroll:
    amount: 300
    direction: DOWN
```

### pressKey

```yaml
- pressKey: Enter
- pressKey: Escape
- pressKey: Tab
- pressKey: Backspace
```

### assertVisible / assertNotVisible

```yaml
- assertVisible: "Hello"
- assertNotVisible: "Error"
```

Menggunakan AX hierarchy dari frontmost app.

### takeScreenshot

```yaml
- takeScreenshot
```

### waitForAnimationToEnd

```yaml
- waitForAnimationToEnd:
    timeout: 5000
```

Bandingkan 2 screenshot dengan jeda 500ms.

---

## Perintah yang BELUM Didukung

Perintah ini no-op atau throw error:

- `clearState` / `clearKeychain`
- `setLocation` / `setOrientation`
- `openLink`
- `startRecording` / `stopRecording`
- `setProxy` / `resetProxy`
- `setPermissions`
- `addMedia`
- `airplaneMode`
- `hideKeyboard`

---

## View Hierarchy (AX Tree)

Sidecar baca view hierarchy dari aplikasi lewat macOS Accessibility API (`AXUIElement`).

### Struktur response

```json
{
  "root": {
    "role": "AXApplication",
    "children": [{
      "role": "AXWindow",
      "title": "app",
      "frame": {"x": 0, "y": 0, "width": 470, "height": 748},
      "children": [{
        "role": "AXStaticText",
        "label": "Hello",
        "frame": {"x": 20, "y": 100, "width": 50, "height": 20}
      }]
    }]
  }
}
```

### Agar komponen React Native muncul di AX tree

1. **Container harus bridge ke AX** (sudah selesai — lihat `.claude/plans/ax-bridge-plan.md`)
2. **Set `accessibilityLabel`** di komponen yang mau di-test:

```tsx
<Text accessibilityLabel="Hello">Hello</Text>
```

3. **Set `accessibilityRole`** untuk peran semantik:

```tsx
<TouchableOpacity accessibilityRole="button" accessibilityLabel="Submit">
  <Text>Submit</Text>
</TouchableOpacity>
```

Role yang didukung: `text`, `button`, `link`, `image`, `switch`, `search`, `keyboardkey`, `header`, `summary`, `adjustable`, `tabbar`.

Mapping di `RCTUIKit.h` → `NSAccessibilityRole`:
- `text` / `header` → `AXStaticText`
- `button` / `keyboardkey` → `AXButton`
- `link` → `AXLink`
- `image` → `AXImage`
- `switch` → `AXCheckBox`
- `search` → `AXTextField`
- `adjustable` → `AXSlider`
- `tabbar` → `AXTabGroup`

---

## Debugging

### Cek sidecar jalan

```bash
curl http://localhost:22088/status
# {"status":"ok"}
```

### Cek view hierarchy

```bash
curl -X POST http://localhost:22088/viewHierarchy -d '{}' | jq .
```

### Cek screenshot

```bash
curl http://localhost:22088/screenshot?compressed=true > screenshot.jpg
```

### Cek device info

```bash
curl http://localhost:22088/deviceInfo
# {"widthPixels":1728,"heightPixels":1117,"widthPoints":864,"heightPoints":558,"scale":2,"osVersion":"15.0"}
```

### Debug log Maestro

```bash
maestro test flow.yaml --debug
```

### Kill sidecar nyangkut

```bash
pkill -f MaestroMacOSDriver
```

### Build clean sidecar

```bash
cd maestro-macos-sidecar
rm -rf .build
swift build
```

---

## Troubleshooting

| Masalah | Solusi |
|---------|--------|
| Sidecar tidak bisa start | Cek Swift build: `swift build` di `maestro-macos-sidecar/` |
| `MAESTRO_MACOS_SIDECAR_PATH` salah | Set env var ke path binary hasil `swift build` |
| View hierarchy kosong | Grant Accessibility permission ke Terminal.app |
| Screenshot kosong | Grant Screen Recording permission |
| Element tidak ditemukan | Pastikan `accessibilityLabel` di-set di RN component |
| Port 22088 sudah dipakai | Kill proses lama: `pkill -f MaestroMacOSDriver` atau set `PORT=22089` |
| Timeout startup | Naikkan timeout: `MAESTRO_DRIVER_STARTUP_TIMEOUT=300000` |
