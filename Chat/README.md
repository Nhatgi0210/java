# JavaChatGUI_DM (Ô 'Đến' hoạt động cho chat + file)
- `ALL` → broadcast (MSG)
- Tên user (vd: `khoa`) → gửi riêng (DM) và gửi file riêng

## Server lệnh
- `USER <name>`
- `MSG <text>`
- `DM <username> <text>`
- `FILE <target> <filename(with spaces)> <size>` + bytes
- `BYE`

## Cách chạy (NetBeans 26)
1) Run `ChatServer` (port 5000).
2) Run `ChatClientLauncher` → nhập host/port/username.
3) Trong GUI, điền **Đến:** = `ALL` hoặc tên người nhận → Gửi tin/Chọn file…
