# A clojure alpine exists, but is missing openjfx / javafx classes
# FROM clojure:alpine AS build-env
FROM clojure AS build-env
WORKDIR /usr/src/myapp
COPY project.clj /usr/src/myapp/
RUN lein deps
RUN apt-get update
RUN apt-get -y install openjfx
RUN DEBIAN_FRONTEND=noninteractive apt-get -y install xorg openbox
COPY . /usr/src/myapp
RUN mv "$(lein uberjar | sed -n 's/^Created \(.*standalone\.jar\)/\1/p')" myapp-standalone.jar

# Multi-stage build will not work here, as we are missing openjfx again.
#FROM openjdk:8-jre-alpine
#WORKDIR /myapp
#COPY --from=build-env /usr/src/myapp/myapp-standalone.jar /myapp/myapp.jar
#ENTRYPOINT ["java", "-jar", "/myapp/myapp.jar"]

WORKDIR /usr/src/myapp
ENTRYPOINT ["java", "-jar", "/usr/src/myapp/myapp-standalone.jar"]
