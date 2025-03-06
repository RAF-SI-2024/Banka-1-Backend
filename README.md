# Banka-1-Backend



## For frontend developers

To run this project, you need to have Docker installed.

First, you need to build the docker images of the submodules. To do this, run the following command:
```shell
./gradlew bootBuildImage
```

After that has finished, do:
```shell
docker compose -f compose.full.yaml up -d
```


## Backend development

For backend development, you need to have Docker and JDK 17 installed.
To run the backend infrastructure (DBs and message queues) in development, run the following command:
```shell
docker compose -f compose.dev.yaml up -d
```

To run the services run:
```shell
./gradlew eureka:bootRun
./gradlew api-gateway:bootRun
./gradlew notification-service:bootRun
./gradlew banking-service:bootRun
./gradlew user-service:bootRun
```
in the order above.

If you are working on a single service you can also run:
```shell
docker compose -f compose.full.yaml up -d
```
Afterwards, depending on which service you want to work on, you should do:
```shell
docker compose -f compose.full.yaml stop <service-name>
```

If you change the [common](common) module, you need to rebuild the docker images.