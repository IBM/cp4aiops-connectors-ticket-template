FROM registry.access.redhat.com/ubi8/ubi-minimal:latest

RUN microdnf install httpd httpd-tools mod_ssl git -y && \
  # Remove default ssl config
  rm -f /etc/httpd/conf.d/ssl.conf \
    /usr/share/doc/perl-IO-Socket-SSL/certs/client-key.enc \
    /usr/share/doc/perl-IO-Socket-SSL/certs/client-key.pem \
    /usr/share/doc/perl-IO-Socket-SSL/certs/server-ecc-key.pem \
    /usr/share/doc/perl-IO-Socket-SSL/certs/server-key.enc \
    /usr/share/doc/perl-IO-Socket-SSL/certs/server-key.pem \
    /usr/share/doc/perl-IO-Socket-SSL/certs/server2-key.pem \
    /usr/share/doc/perl-IO-Socket-SSL/certs/proxyca.pem \
    /usr/share/doc/perl-IO-Socket-SSL/certs/sub-server.pem \
    /usr/share/doc/perl-IO-Socket-SSL/certs/server-wildcard.pem \
    
    /usr/share/doc/perl-Net-SSLeay/examples/server_key.pem && \
  mkdir /conf && \
  mkdir -p /srv/git && \
  # Will be overwritten by secret mount in k8s
  htpasswd -bc /conf/.htpasswd default default && \
  # Permissions
  chmod ugo+rwx /srv/git && \
  chmod ugo+rwx /conf && \
  chmod ugo+rwx /run/httpd && \
  chmod ugo+rwx /etc/httpd && \
  chmod ugo+rwx /var/log/httpd && \
  chmod ugo+rwx /usr/lib64/httpd/modules && \
  chmod ugo+rwx /var/lib/httpd

# To fix "fatal: detected dubious ownership in repository at '/srv/git/cp4waiops-connectors/.git': /usr/libexec/git-core/git-http-backend"
# Add safe.directory for the new problematic locations
RUN git config --system --add safe.directory '*'

USER 1000

COPY --chown=1000:1000 httpd.conf /etc/httpd/conf/
COPY --chown=1000:1000 git-server.conf /etc/httpd/conf.d/
COPY liveness.sh /usr/local/bin/liveness

RUN mkdir -p /srv/git/cp4waiops-connectors && \
    chmod ugo+rwx /srv/git/cp4waiops-connectors

# TODO Generalized Base Image ?
COPY --chown=1000:1000 ./bundles/ /srv/git/cp4waiops-connectors/bundles/
WORKDIR /srv/git/cp4waiops-connectors

RUN git init && \
    git config user.email "support@ibm.com" && \
    git config user.name "IBM Support" && \
    git config --list && \
    git add . && \
    git commit -m "bundled-version" && \
    git checkout -b bundled-version

ENTRYPOINT ["/usr/sbin/httpd", "-DFOREGROUND"]
