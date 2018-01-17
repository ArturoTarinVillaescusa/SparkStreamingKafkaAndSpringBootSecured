FROM frolvlad/alpine-oraclejdk8:slim
MAINTAINER Equipo WaterSupply <atarin@platformprovider.com>

ARG VERSION

ENV TERM xterm

RUN apk add --no-cache bash jq curl wget nginx

RUN mkdir -p /metadata
RUN mkdir -p /www

# No se si será necesario tenerlo para WaterSupply. Lo comento para poder generar la máquina
# COPY target/jobs-$VERSION.jar /www/jobs.jar
COPY docker/entrypoint.sh /
COPY docker/metadata/entrypoint.sh /metadata/entrypoint.sh
COPY docker/metadata/log4j.properties.template /metadata/log4j.properties.template
COPY docker/audit-entrypoint.sh /
COPY docker/kms_utils.sh /
COPY docker/commons.sh /
COPY docker/submission-healthcheck.sh /

# dumb-init install
RUN wget -O /usr/local/bin/dumb-init https://github.com/Yelp/dumb-init/releases/download/v1.2.0/dumb-init_1.2.0_amd64
RUN chmod +x /usr/local/bin/dumb-init
RUN chmod a+x /entrypoint.sh /audit-entrypoint.sh /metadata/entrypoint.sh /commons.sh /kms_utils.sh /submission-healthcheck.sh

RUN adduser -D -u 1000 -g 'www' www
RUN chown -R www:www /var/lib/nginx
RUN chown -R www:www /www

VOLUME /tmp
ADD *.jar app.jar
RUN sh -c 'touch /app.jar'
ENV JAVA_OPTS="-Xms1024m -Xmx1024m"
ENTRYPOINT [ "sh", "-c", "java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar /app.jar -classpath lecturas.producers.ProcesaFtpDeLecturasItron" ]
