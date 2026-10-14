FROM eclipse-temurin:25-jdk AS build
WORKDIR /app

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && sed -i 's/\r$//' mvnw

COPY application-manager/pom.xml application-manager/
COPY database-manager/pom.xml database-manager/
COPY log-manager/pom.xml log-manager/
COPY notification-manager/pom.xml notification-manager/
COPY report-manager/pom.xml report-manager/
COPY time-manager/pom.xml time-manager/
COPY user-manager/pom.xml user-manager/
COPY workforce-api/pom.xml workforce-api/

RUN ./mvnw dependency:go-offline -B

COPY . .

RUN sed -i 's|localhost:5432|host.docker.internal:5432|g' workforce-api/src/main/resources/application-local.properties

RUN chmod +x mvnw && sed -i 's/\r$//' mvnw && \
    ./mvnw clean package -DskipTests \
    -Dcheckstyle.skip=true \
    -Dpmd.skip=true \
    -Dspotbugs.skip=true \
    -Djacoco.skip=true

FROM eclipse-temurin:25-jre
WORKDIR /app

COPY --from=build /app/workforce-api/target/*.jar app.jar

EXPOSE 8443
ENTRYPOINT ["java", "-jar", "app.jar"]
