#
# Build stage
#
FROM maven:3.9.9-eclipse-temurin-17 AS build
ENV HOME=/home/app
WORKDIR $HOME
COPY pom.xml $HOME
RUN mvn verify --fail-never
COPY . /home/app
RUN mvn clean package -Dmaven.test.skip=true

#
# App stage
#
FROM eclipse-temurin:17-jre
ENV HOME=/home/app
WORKDIR $HOME
COPY --from=build $HOME/target/alovoa-1.1.0.jar $HOME/alovoa-1.1.0.jar
ENTRYPOINT ["java", "-XX:+HeapDumpOnOutOfMemoryError", "-Xmx128m", "-jar", "-Dfile.encoding=UTF-8", "-Dspring.profiles.active=prod", "alovoa-1.1.0.jar"]
