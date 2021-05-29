# A clojure alpine exists, but is missing openjfx / javafx classes
# FROM clojure:alpine AS build-env
FROM ubuntu:18.04 AS build-env

ENV DEBIAN_FRONTEND=noninteractive
RUN apt-get update && apt-get -y install openjdk-8-jdk xorg openbox wget

# newer openjfx will not work with the ubuntu setup here / source code in repo.
RUN apt-get --allow-downgrades -y install \
    openjfx=8u161-b12-1ubuntu2 \
    libopenjfx-java=8u161-b12-1ubuntu2 \
    libopenjfx-jni=8u161-b12-1ubuntu2

WORKDIR /usr/src/myapp
COPY project.clj /usr/src/myapp/
RUN wget https://raw.githubusercontent.com/technomancy/leiningen/stable/bin/lein
RUN chmod +x ./lein
RUN cp ./lein /usr/bin/
RUN lein deps
COPY . /usr/src/myapp

RUN cat /etc/*release*
RUN java -version

RUN mv "$(lein uberjar | sed -n 's/^Created \(.*standalone\.jar\)/\1/p')" myapp-standalone.jar

# Multi-stage build will not work here, as we are missing openjfx again.
#FROM openjdk:8-jre-alpine
#WORKDIR /myapp
#COPY --from=build-env /usr/src/myapp/myapp-standalone.jar /myapp/myapp.jar
#ENTRYPOINT ["java", "-jar", "/myapp/myapp.jar"]
COPY ./dist-skel/* /usr/src/myapp/
COPY ./conf /usr/src/myapp/conf/
COPY ./resources /usr/src/myapp/resources/
COPY ./js-src /usr/src/myapp/js-src/
COPY ./docs /usr/src/myapp/docs/

#RUN mkdir -p /myapp/docs
#RUN mkdir -p /root/docs
#COPY ./docs/* /myapp/docs/
#COPY ./docs/* /root/docs/

COPY ./ahubu.png /usr/src/myapp/

WORKDIR /usr/src/myapp
ENTRYPOINT ["java", "-jar", "/usr/src/myapp/myapp-standalone.jar"]
