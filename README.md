#### EventBox/Broker:1.0.0

#### Build jar
```bash
./mvnw clean package
```

#### build docker image
```bash
docker build -t eventbox/broker:1.0.0 .
```

#### build container
```bash
docker container create --name eventbox-broker-1.0.0 --publish 8081:80 eventbox/broker:1.0.0
```

#### run container

$$\color{red}Important$$ : [eventbox/admin](https://github.com/cherattk/eventbox-admin?tab=readme-ov-file#eventboxadmin100) must be started before running the broker.

```bash
docker start -a eventbox-broker-1.0.0
```
