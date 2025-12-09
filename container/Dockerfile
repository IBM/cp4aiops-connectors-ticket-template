## Build application
FROM registry.access.redhat.com/ubi9/openjdk-21:latest as builder
WORKDIR /build
USER root

# Download and cache dependencies beforehand
COPY pom.xml /build/connector/
RUN cd /build/connector && mvn dependency:go-offline -B

COPY . /build/connector
RUN cd /build/connector && rm -r src/main/liberty && mvn install

## Final image
FROM --platform=linux/amd64 icr.io/appcafe/websphere-liberty:25.0.0.9-kernel-java21-openj9-ubi-minimal
ARG VERBOSE=true
ENV LICENSE=accept

USER 0
RUN microdnf install -y procps-ng && microdnf clean all
USER 1001

COPY container/import-certs.sh /opt/import-certs.sh
RUN chmod ug+x /opt/import-certs.sh
COPY container/jvm.options /config/

# Add Liberty server configuration including all necessary features
COPY container/server.xml /config/
RUN features.sh && configure.sh

COPY --from=builder /build/connector/target/ticket-template.war /config/apps/

USER 1001

RUN features.sh && configure.sh