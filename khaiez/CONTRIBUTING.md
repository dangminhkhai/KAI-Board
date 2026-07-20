# Đóng góp

## Nguyên tắc

- Giữ nguyên hình học phím, vùng chạm, repeat, long-press, rung, âm thanh và thời điểm commit nếu yêu cầu không chủ đích thay đổi chúng.
- Không chạy I/O, mạng, tải model hoặc phân tích dữ liệu lớn trên luồng nhập liệu.
- Không log nội dung gõ, clipboard, từ đã học, prompt, phản hồi AI hoặc API key.
- Thay đổi hành vi phải có test hồi quy; thay đổi giao diện phải kiểm tra sáng/tối và kích thước màn hình.
- Thay đổi **Telex / composing / Backspace / InputConnection**:
  - Bắt buộc unit: tone `s f r x j`, shape `aa ee oo aw ow uw dd`, Latin restore, escape UniKey, BS visible vs raw.
  - Không coi unit test là đủ cho OEM: chạy regression trong [TESTING.md](TESTING.md) (ít nhất smoke 5 phút + Safe/Cafe/Google trên thiết bị).
  - Backspace co chuỗi Latin phải **prefix-shrink (chỉ xóa đuôi)**; không xóa cả từ rồi `commitText` từ ngắn kết thúc bằng modifier (lỗi Vivo `Safe`→`Saff`).
  - Xem [ARCHITECTURE.md](ARCHITECTURE.md) mục *Ghi editor*.

## Quy trình

1. Tạo nhánh ngắn theo tính năng hoặc lỗi.
2. Chỉ sửa phạm vi cần thiết; không trộn refactor không liên quan.
3. Chạy:

   ```powershell
   .\gradlew.bat testDebugUnitTest assembleDebug
   ```

4. Kiểm tra trên ít nhất một ô nhập thường và một ứng dụng thực tế (Zalo/Chrome/Notes).
5. Với Telex/IME: tick các mục liên quan trong [TESTING.md](TESTING.md) hoặc nêu rõ đã chạy smoke.
6. PR mô tả trước/sau, cách test, rủi ro và ảnh nếu thay đổi UI.

## Cài debug nhanh

```powershell
.\dev-install.cmd
.\dev-install.cmd -Serial <adb-serial>
.\dev-install.cmd -WithTests
```

Script dò JDK 17 từ `JAVA_HOME` hoặc Microsoft/Eclipse path; ADB từ `%LOCALAPPDATA%\Android\Sdk\platform-tools\`.

Không commit keystore, mật khẩu ký, `local.properties`, API key hoặc dữ liệu người dùng.
