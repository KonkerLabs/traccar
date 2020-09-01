FROM openjdk:8-jre-alpine

ENV TRACCAR_VERSION 4.8.4

WORKDIR /opt/traccar

COPY out /opt/traccar

EXPOSE 8082
EXPOSE 5000-5200

# ENTRYPOINT ["java", "-Xms512m", "-Xmx512m", "-Djava.net.preferIPv4Stack=true"]
ENTRYPOINT ["java", "-Xms1024m", "-Xmx1024m", "-Djava.net.preferIPv4Stack=true"]

CMD ["-jar", "tracker-server.jar", "conf/traccar.xml"]

