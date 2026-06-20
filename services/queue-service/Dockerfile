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
COPY services/websocket-server/pom.xml services/websocket-server/pom.xml

#COPY libs/common-utils/src libs/common-utils/src
#COPY libs/common-dto/src libs/common-dto/src
#COPY libs/common-security/src libs/common-security/src

COPY services/queue-service/src services/queue-service/src

RUN mvn package -DskipTests -pl services/queue-service -am

FROM eclipse-temurin:25-jre
WORKDIR /app
RUN mkdir -p /app/logs
COPY --from=build /app/services/queue-service/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]