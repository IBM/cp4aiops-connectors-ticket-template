## Build application
FROM registry.access.redhat.com/ubi8/openjdk-11:latest as builder
WORKDIR /build

# Download and cache dependencies beforehand
COPY --chown=jboss:jboss pom.xml /build/connector/
RUN cd /build/connector && mvn dependency:go-offline -B

COPY --chown=jboss:jboss . /build/connector
RUN cd /build/connector && rm -r src/main/liberty && mvn install

## Final image
FROM --platform=linux/amd64 icr.io/appcafe/websphere-liberty:23.0.0.12-kernel-java11-openj9-ubi
# ARG VERBOSE=true
# ENV LICENSE=accept

USER 0
RUN dnf install -y procps-ng && dnf clean all
USER 1001

COPY --chown=1001:0 container/import-certs.sh /opt/import-certs.sh
RUN chmod ug+x /opt/import-certs.sh
COPY --chown=1001:0 container/jvm.options /config/

# Add Liberty server configuration including all necessary features
COPY --chown=1001:0  container/server.xml /config/
RUN features.sh && configure.sh

COPY --from=builder --chown=1001:0 /build/connector/target/github-sample.war /config/apps/

USER 1001

RUN features.sh && configure.sh