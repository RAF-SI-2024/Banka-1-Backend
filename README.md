# Banka-1-Backend



## Docker Compose

This project uses docker compose for development. There are two compose files, `compose.dev.yaml` and `compose.full.yaml`.

### Caddy
Caddy is used as the reverse proxy for both the frontend and backend. 

#### HTTPS
Caddy automatically creates self-signed TLS certificates which might cause issues when testing).  
To fix this, when running Postman (or similar), follow the instructions [here](https://caddyserver.com/docs/running#local-https-with-docker).  
Browsers will show a self-signed certificate warning when accessing the frontend.  
I don't know a way to "fix" this warning. It can be safely ignored.


## For frontend developers

To run this project, you need to have Docker installed.

First, you need to build the docker images of the submodules. To do this, run the following command:
```shell
./gradlew bootBuildImage
```

After that has finished, do:
```shell
docker compose -f compose.local.yaml up -d
```


## Backend development

### ‼️ You cannot do both of these at once ‼️


For backend development, you need to have Docker and JDK 17 installed.
To run the backend infrastructure (DBs and message queues) in development, run the following command:
```shell
docker compose -f compose.dev.yaml up -d
```

To run the services run:
```shell
./gradlew notification-service:bootRun
./gradlew banking-service:bootRun
./gradlew user-service:bootRun
```
in the order above.

If you are working on a single service you can also run:
```shell
docker compose -f compose.local.yaml up -d
```
Afterwards, depending on which service you want to work on, you should do:
```shell
docker compose -f compose.local.yaml stop <service-name>
```
After that, you can run
```shell
./gradlew <service-name>:bootRun
```
If you change the [common](common) module, you need to rebuild the docker images.