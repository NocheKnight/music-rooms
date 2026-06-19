FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /app

COPY pom.xml .

COPY libs/common-utils/pom.xml libs/common-utils/pom.xml
COPY libs/common-dto/pom.xml libs/common-dto/pom.xml
COPY libs/common-security/pom.xml libs/common-security/pom.xml

COPY services/api-gateway/pom.xml services/api-gateway/pom.xml
COPY services/room-service/pom.xml services/room-service/pom.xml
COPY services/queue-service/pom.xml services/queue-service/pom.xml
COPY services/media-service/pom.xml services/media-service/pom.xml

#COPY libs/common-utils/src libs/common-utils/src
#COPY libs/common-dto/src libs/common-dto/src
#COPY libs/common-security/src libs/common-security/src

COPY services/media-service/src services/media-service/src

RUN mvn package -DskipTests -pl services/media-service -am

FROM eclipse-temurin:25-jre
WORKDIR /app

RUN apt-get update && apt-get install -y \
    ffmpeg \
    curl \
    && curl -L https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp \
           -o /usr/local/bin/yt-dlp \
    && chmod a+rx /usr/local/bin/yt-dlp \
    && apt-get clean

COPY --from=build /app/services/media-service/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]