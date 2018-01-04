# Starter
Startup-ready web skeleton  
Application runs on port 8080 
config app by modifying application.properties  
config ui labels by modifying messages.properties  

## Run

##### Dev Profile

```sh
$ mvn spring-boot:run -Dspring.profiles.active=dev
```

##### Production Profile

```sh
$ mvn spring-boot:run -Dspring.profiles.active=prod
```

## Run with Docker Command Line

```sh
$ mvn clean package  
```

```sh
$ docker build -e JAR_FILE=target/starter-0.0.1-SNAPSHOT.jar -t udelblue/starter .  
```
