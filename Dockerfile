FROM openjdk:21-jdk-slim

WORKDIR /app

# Tüm dosyaları kopyala
COPY . .

# Dosyaların kopyalandığını kontrol et
RUN ls -la

# mvnw'nin varlığını kontrol et
RUN ls -la mvnw

# Permission ver
RUN chmod +x ./mvnw

# Uygulamayı derle
RUN ./mvnw package -DskipTests -q

# Environment variable
ENV QUARKUS_PROFILE=dev

# Uygulamayı çalıştır
CMD ["java", "-jar", "target/quarkus-app/quarkus-run.jar"]

EXPOSE 8080