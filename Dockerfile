# Sử dụng một ảnh Java (OpenJDK 17) làm nền
FROM openjdk:21-jdk-slim

# Tạo thư mục làm việc
WORKDIR /app

# Copy các file build của Maven (để tối ưu cache)
COPY .mvn/ .mvn
COPY mvnw pom.xml ./

# Chạy build để tải dependencies
RUN ./mvnw dependency:go-offline

# Copy toàn bộ source code
COPY src ./src

# Đóng gói ứng dụng thành file .jar (bỏ qua tests)
RUN ./mvnw clean package -DskipTests

# Mở cổng 8080 mà Spring Boot thường chạy
EXPOSE 8080

# Lệnh để chạy ứng dụng khi container khởi động
# !!! THAY ĐỔI TÊN FILE .JAR Ở DƯỚI CHO ĐÚNG !!!
ENTRYPOINT ["java", "-jar", "target/openevent-0.0.1-SNAPSHOT.jar"]
