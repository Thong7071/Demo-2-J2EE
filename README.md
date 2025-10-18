Demo 2 — AI Chat RAG từ PDF (Spring Boot + Gemini)

Ứng dụng cung cấp giao diện chat kiểu GPT để hỏi đáp theo nội dung PDF. Quy trình RAG: nạp PDF → tách đoạn (chunk) → tạo vector embedding (Gemini) → truy xuất top-k ngữ cảnh.

✨ Tính năng

- Upload & Reindex tự động: bấm nút "+" chọn PDF; hệ thống tự upload, trích văn bản, cắt chunk và embedding.
- Chat kiểu GPT: ô nhập đơn giản; nút gửi mũi tên. Khi đang xử lý, nút gửi chuyển hình vuông (pending) và tự trở lại tròn khi xong.
- Gợi ý câu hỏi: hiển thị các quick prompts phía trên chatbox ngay sau khi reindex.
- Định dạng đầu ra: tự nhận bullets/đánh số, xuống dòng dễ đọc, hỗ trợ bold.

📦 Tech stack

- Java 17, Spring Boot 3 (Web, Validation, Actuator)
- Apache PDFBox
- Gemini APIs (chat & embedding)
- Frontend: HTML/CSS/JS nguyên bản

⚙️ Cấu hình & chạy

1. Cấu hình Gemini AI

Bạn cần có Gemini API Key từ Google AI Studio:

    1. Truy cập Google AI Studio
    2. Tạo API key mới
    3. Copy API key để sử dụng

2. Copy file config-template.properties thành .env:

   cp config-template.properties .env

3. Chạy ứng dụng

# Chạy với Maven (Windows)

mvn spring-boot:run

# Chạy với Maven (Linux/Mac)

./mvnw spring-boot:run

# Hoặc build và chạy

mvn clean package
java -jar target/demo-0.0.1-SNAPSHOT.jar

4. Truy cập ứng dụng
   Mở trình duyệt và truy cập: http://localhost:8080

📁 Cấu trúc dự án

```
src/
 ├─ main/java/com/example/demo/
 │   ├─ config/
 │   │   ├─ CorsConfig.java
 │   │   └─ (tuỳ chọn) SecurityConfig.java       tắt CSRF cho /api/
 │   ├─ controller/
 │   │   ├─ ChatController.java                  /api/rag/reindex, /ask
 │   │   └─ GlobalExceptionHandler.java
 │   ├─ model/
 │   │   ├─ AskRequest.java
 │   │   ├─ AskResponse.java
 │   │   └─ ReindexResponse.java
 │   └─ service/
 │       ├─ PdfLoader.java
 │       ├─ TextChunker.java
 │       ├─ GeminiRestService.java
 │       ├─ VectorMath.java, VectorStore.java
 │       ├─ IndexService.java
 │       └─ RagService.java
 └─ main/resources/
     ├─ static/index.html                         UI chat kiểu GPT + quick prompts
     └─ application.properties
```

🧪 Kịch bản kiểm thử nhanh

- "Tóm tắt ngắn tài liệu này (3–5 câu)."

- "Tóm tắt 5 ý chính quan trọng nhất của tài liệu."

- "Liệt kê 3 thuật ngữ/cụm từ khóa quan trọng và giải thích ngắn gọn."
