#!/bin/sh

# VALUES THAT CAN BE CHANGED
admin_host=http://localhost:80
broker_hostname=localhost
broker_port=8080
broker_token="broker-token"



############################################################
# DO NOT CHANGE the following Variables
############################################################
echo "Set Required Envirement variables"

# DO NOT CHANGE
export EVENTBOX_BROKER_HOST=$broker_hostname

# DO NOT CHANGE
export EVENTBOX_BROKER_PORT=$broker_port

# DO NOT CHANGE
export EVENTBOX_BROKER_AUTH_TOKEN="bearer-token"

# DO NOT CHANGE
export EVENTBOX_ADMIN_BINDINGMAP_ENDPOINT=$admin_host"/api/eventbinding"

# run the broker
echo "Run Broker"
./mvnw clean compile exec:java
