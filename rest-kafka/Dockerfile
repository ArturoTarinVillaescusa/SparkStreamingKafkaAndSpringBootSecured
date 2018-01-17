FROM frolvlad/alpine-oraclejdk8:slim
MAINTAINER Equipo WaterSupply <atarin@platformprovider.com>

ARG VERSION

ENV TERM xterm

# Añado openssl porque lo necesita kms_utils.sh, y apt-get y dpkg por si es necesario instalar más cosas
RUN apk add --no-cache bash jq curl wget nginx openssl dpkg

RUN mkdir -p /metadata
RUN mkdir -p /www

# No se si será necesario tenerlo para WaterSupply. Lo comento para poder generar la máquina
# COPY target/jobs-$VERSION.jar /www/jobs.jar
COPY src/main/docker/entrypoint.sh /
COPY src/main/docker/metadata/entrypoint.sh /metadata/entrypoint.sh
COPY src/main/docker/metadata/log4j.properties.template /metadata/log4j.properties.template
COPY src/main/docker/audit-entrypoint.sh /
COPY src/main/docker/kms_utils.sh /
COPY src/main/docker/commons.sh /
COPY src/main/docker/submission-healthcheck.sh /
COPY src/main/docker/lecturas-ms-principal-entrypoint.sh /
COPY target/sparkexample-jar-with-dependencies.jar /app.jar
# dumb-init install
RUN wget -O /usr/local/bin/dumb-init https://github.com/Yelp/dumb-init/releases/download/v1.2.0/dumb-init_1.2.0_amd64
RUN chmod +x /usr/local/bin/dumb-init
RUN chmod a+x /entrypoint.sh /audit-entrypoint.sh /metadata/entrypoint.sh /commons.sh /kms_utils.sh /submission-healthcheck.sh




ENTRYPOINT ["sh", "-c", "exec /lecturas-ms-principal-entrypoint.sh"]