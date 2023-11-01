# DACS4 - Xây dựng và phát triển ứng dụng theo dõi tiến trình máy tính từ xa (Remote Desktop)

- Theo dõi tiến trình hiện tại
- Tạo file info.txt để lưu khi ấn nút enable logging gửi đến drive để lưu trữ
- Chức năng chụp ảnh và thống kê từ phía user theo dõi
- Kết nối nhiều client với server

  Task List
  - Giao diện phía client theo dõi
      + Đăng nhập / Đăng ký
      + Thực hiện nhận tiến trình và thống kê
      + Nút logging để lưu trữ lại tiến trình khi không hoạt động
  - Giao diện phía server
      + Trạng thái server
      + Area chứa các client đang theo dõi
      + Area chứa các client đang được theo dõi
      + Nút khởi tạo server và đóng server
  - Phía client đang được theo dõi
      +Không có giao diện hoặc thô sơ
      +Tạo file JAR để được chạy ngầm và được tự động chạy khi khởi động
