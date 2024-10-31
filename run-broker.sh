#!/bin/sh

##################################################################
echo "Set Required Envirement variables"
# Change with the appropriate eventbox/admin [hostname:port] value
export EVENTBOX_ADMIN_HOST="http://localhost:8080"

echo "EVENTBOX_ADMIN_HOST=$EVENTBOX_ADMIN_HOST"

##################################################
# run the broker
echo "Run Broker"
echo "./mvnw clean compile exec:java"
./mvnw clean compile exec:java
