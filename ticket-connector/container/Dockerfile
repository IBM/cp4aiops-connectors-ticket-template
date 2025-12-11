FROM registry.access.redhat.com/ubi9/openjdk-21:latest as builder
WORKDIR /build

USER root
# Download and cache dependencies beforehand
COPY pom.xml /build/connector/
RUN cd /build/connector && mvn dependency:go-offline -B

COPY . /build/connector
# Skipping the test here, as tests will be run as part of CI/CD. No need to run this each time just to build the image
# Running "make test" if you are doing development and want to run the test
RUN cd /build/connector && rm -r src/main/liberty && mvn install -B -Dmaven.test.skip=true

## Final image
FROM icr.io/appcafe/websphere-liberty:25.0.0.9-kernel-java21-openj9-ubi-minimal
ARG VERBOSE=true
ENV LICENSE=accept

USER 0
RUN microdnf install -y procps-ng && microdnf clean all
USER 1001

COPY --chown=1001:0 container/import-certs.sh /opt/import-certs.sh
RUN chmod ug+x /opt/import-certs.sh
COPY --chown=1001:0 container/jvm.options /config/

# Add Liberty server configuration including all necessary features
COPY --chown=1001:0  container/server.xml /config/
RUN features.sh

COPY --from=builder --chown=1001:0 /build/connector/target/github.war /config/apps/

USER 1001

RUN configure.sh