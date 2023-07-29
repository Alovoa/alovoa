#
# Build stage
#
FROM maven:3.6.0-jdk-11 AS build
COPY . /home/app
WORKDIR /home/app
RUN mvn -f /home/app/pom.xml clean install -DskipTests

#
# App stage
#
FROM openjdk:11-jre
COPY --from=build /home/app/target/alovoa-1.0.0.jar /home/app/alovoa-1.0.0.jar
WORKDIR /home/app
EXPOSE 8080
ENTRYPOINT ["java", "-XX:+HeapDumpOnOutOfMemoryError", "-Xmx128m", "-jar", "-Dfile.encoding=UTF-8", "-Dspring.profiles.active=prod", "alovoa-1.0.0.jar"]
