# eventbox/broker
# VERSION : 1.0.0

FROM eclipse-temurin:11-alpine

#RUN addgroup -S eventbox && adduser -S broker -G eventbox
#USER eventbox:broker

ENV APP_NAME="eventbox-broker-1.0.0-fat.jar"
ENV EVENTBOX_ADMIN_HOST="http://host.docker.internal:8080"

RUN mkdir /opt/app
COPY target/$APP_NAME /opt/app

WORKDIR /opt/app
CMD ["sh","-c", "java -jar $APP_NAME"]
