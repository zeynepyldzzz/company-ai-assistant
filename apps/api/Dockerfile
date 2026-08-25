# syntax=docker/dockerfile:1

# ---------- 1. asama: derleme ----------
FROM eclipse-temurin:21-jdk AS builder
WORKDIR /build

# Once YALNIZCA bagimlilik tanimlari kopyalanir. Kaynak kod degistiginde Docker
# bu katmani cache'ten alir ve tum bagimliliklari yeniden indirmez; aksi halde
# tek satirlik bir kod degisikligi dakikalarca suren bir indirme baslatir.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw -B dependency:go-offline

# En sik degisen sey en son katmanda olmali.
COPY src/ src/

# Testler BILEREK atlanir. Testler ayakta bir Postgres istiyor
# (bkz. .github/workflows/ci.yml -> services.postgres), docker build sirasinda
# ise veritabani yok. Kalite kapisi CI'daki `mvnw verify`; imaj yalnizca paketler.
RUN ./mvnw -B -DskipTests package

# Surum numarasi pom.xml'de degistiginde asagidaki COPY kirilmasin diye
# sabit bir isme alinir.
RUN mv target/*.jar app.jar

# ---------- 2. asama: calistirma ----------
# JDK degil JRE: derleyici ve build araclari production imajinda yer almaz,
# imaj kucuk kalir ve saldiri yuzeyi daralir.
FROM eclipse-temurin:21-jre
WORKDIR /app

# Root olarak calistirmamak icin ayri kullanici.
RUN groupadd --system spring && useradd --system --gid spring spring
USER spring:spring

COPY --from=builder --chown=spring:spring /build/app.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
