#### EventBox/Broker:1.0.0

#### Set required environment variable
```bash
export EVENTBOX_ADMIN_HOST="http://eventbox-admin-hostname:port-number"
```

#### $$\color{red}Important$$ : [eventbox/admin](https://github.com/cherattk/eventbox-admin?tab=readme-ov-file#eventboxadmin100) MUST be started before running the broker.

#### Run without building fat jar
```bash
./mvnw clean compile exec:java
```

#### Build fat jar
```bash
./mvnw clean package
```

#### Run fat jar
```bash
java -jar target/eventbox-broker-1.0.0-fat.jar
```