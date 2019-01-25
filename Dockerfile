# A clojure alpine exists, but is missing openjfx / javafx classes
# FROM clojure:alpine AS build-env
FROM clojure AS build-env
WORKDIR /usr/src/myapp
COPY project.clj /usr/src/myapp/
RUN lein deps
COPY . /usr/src/myapp
RUN mv "$(lein uberjar | sed -n 's/^Created \(.*standalone\.jar\)/\1/p')" myapp-standalone.jar

FROM openjdk:8-jre-alpine
WORKDIR /myapp
COPY --from=build-env /usr/src/myapp/myapp-standalone.jar /myapp/myapp.jar
ENTRYPOINT ["java", "-jar", "/myapp/myapp.jar"]
