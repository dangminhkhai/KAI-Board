# Phát hành

## Chuẩn bị

1. Cập nhật `versionCode`, `versionName` và `CHANGELOG.md`.
2. Chạy test, build debug và kiểm thử thiết bị.
3. Đặt biến môi trường:
   - `KAI_RELEASE_STORE_FILE`
   - `KAI_RELEASE_STORE_PASSWORD`
   - `KAI_RELEASE_KEY_ALIAS`
   - `KAI_RELEASE_KEY_PASSWORD`
4. Build release bằng `build-release.cmd` hoặc Gradle task tương ứng.
5. Kiểm tra chữ ký, R8, icon, quyền, kích thước APK/AAB và crash buffer.

## Quy tắc

- Không commit keystore hoặc mật khẩu.
- Lưu keystore ở nơi được sao lưu an toàn; mất keystore có thể chặn cập nhật ứng dụng.
- Xác minh chính sách riêng tư, third-party notices và listing trước khi tải lên store.
- Phát hành theo staged rollout; giữ APK/AAB, mapping và commit tương ứng.

