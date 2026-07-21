# KAI Board — tài liệu nội bộ (`khaiez/`)

KAI Board là bàn phím Android (Kotlin) ưu tiên gõ Telex nhanh, riêng tư, ổn định. Hỗ trợ Telex trong ô URL, từ điển VI/EN offline tùy chọn, lexicon cá nhân có decay, gợi ý lệnh AI, Inline Autofill, chế độ riêng tư, Theme Extension JSON, emoji search, vuốt Space, clipboard, dịch ML Kit, voice và AI multi-provider.

## Trạng thái

Dự án đang **debug / phát triển nội bộ**. Chỉ dùng `assembleDebug` và `dev-install.cmd`. Không có quy trình ký hay phân phối store trong repo.

Phiên bản: **0.1.0-debug** (`versionCode 1`). Checklist thiết bị: [TESTING.md](TESTING.md).

## Yêu cầu

- JDK 17 (Microsoft hoặc Eclipse Temurin; `dev-install` tự dò `JAVA_HOME`).
- Android SDK: `compileSdk 37`, `targetSdk 35`, `minSdk 26`.
- Thiết bị/emulator; APK debug tách `arm64-v8a` (và `x86_64` khi build splits).

## Build debug

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug
.\dev-install.cmd
# hoặc một máy cụ thể:
.\dev-install.cmd -Serial <serial>
.\dev-install.cmd -WithTests
```

- Debug APK: `app/build/outputs/apk/debug/`
- Không đặt API key trong source, resource hay lệnh chia sẻ

Theme JSON: [`theme-packs/README.md`](../theme-packs/README.md).

## Mục lục tài liệu

| File | Nội dung |
| --- | --- |
| **[HANDOFF.md](HANDOFF.md)** | **Chuyển máy / resume session** — việc đã xong + next |
| [ARCHITECTURE.md](ARCHITECTURE.md) | Module, luồng gõ/Telex, ghi editor OEM, dữ liệu |
| [TESTING.md](TESTING.md) | Unit + **checklist tick** thiết bị / smoke / ma trận Telex |
| [CHANGELOG.md](CHANGELOG.md) | Lịch sử phát triển |
| [PRIVACY.md](PRIVACY.md) | Quyền riêng tư, backup, AI |
| [SECURITY.md](SECURITY.md) | Báo cáo lỗ hổng, secret |
| [ROADMAP.md](ROADMAP.md) | Gần nhất / ổn định debug |
| [CONTRIBUTING.md](CONTRIBUTING.md) | Guardrail PR và quy trình |
| [SKILLS.md](SKILLS.md) | Skill agent khi dev KAI Board |
| [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) | Thành phần bên thứ ba (bản trong `khaiez` nếu có; root có bản đầy đủ) |

README người dùng/repo: [../README.md](../README.md).

## Telex & OEM (tóm tắt)

- Engine: `TelexEngine` + `TelexWordComposer` (display / raw / lock).
- Backspace co chuỗi Latin: **chỉ xóa đuôi** trên `InputConnection` (tránh `Safe`→`Saff` trên Vivo).
- Modifier: tone `s f r x j`, shape `aa ee oo aw ow uw`, `dd`, `uow`.
- Chi tiết: [ARCHITECTURE.md](ARCHITECTURE.md), checklist: [TESTING.md](TESTING.md).
