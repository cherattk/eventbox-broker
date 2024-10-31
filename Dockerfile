# eventbox/broker
# VERSION : 1.0.0

FROM eclipse-temurin:11-alpine

#RUN addgroup -S spring && adduser -S spring -G spring
#USER spring:spring

# Change with eventbox/admin [hostname:port-number]
ENV EVENTBOX_ADMIN_HOST="http://localhost:8080"

RUN mkdir /opt/app
COPY target/eventbox-broker-1.0.0-fat.jar /opt/app
WORKDIR /opt/app

CMD ["java", "-jar", "eventbox-broker-1.0.0-fat.jar"]

