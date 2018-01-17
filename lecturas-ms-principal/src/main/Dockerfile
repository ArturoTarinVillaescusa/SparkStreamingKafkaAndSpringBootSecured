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
COPY docker/entrypoint.sh /
COPY docker/metadata/entrypoint.sh /metadata/entrypoint.sh
COPY docker/metadata/log4j.properties.template /metadata/log4j.properties.template
COPY docker/audit-entrypoint.sh /
COPY docker/kms_utils.sh /
COPY docker/commons.sh /
COPY docker/submission-healthcheck.sh /
COPY docker/lecturas-ms-principal-entrypoint.sh /

# dumb-init install
RUN wget -O /usr/local/bin/dumb-init https://github.com/Yelp/dumb-init/releases/download/v1.2.0/dumb-init_1.2.0_amd64
RUN chmod +x /usr/local/bin/dumb-init
RUN chmod a+x /entrypoint.sh /audit-entrypoint.sh /metadata/entrypoint.sh /commons.sh /kms_utils.sh /submission-healthcheck.sh

RUN adduser -D -u 1000 -g 'www' www
RUN chown -R www:www /var/lib/nginx
RUN chown -R www:www /www

VOLUME /tmp
ADD *.jar app.jar
ENV JAVA_OPTS=""
# Añado la máquina de Vault a /etc/hosts de la máquina virtual
RUN sh -c 'touch /app.jar' && sh -c 'touch /microservicio.config'

ENTRYPOINT ["sh", "-c", "exec /lecturas-ms-principal-entrypoint.sh"]
# ENTRYPOINT [ "sh", "-c", "echo 104.199.80.135  vault.service.watersupply.labs.platformprovider.com >> /etc/hosts && /lecturas-ms-principal-entrypoint.sh && cat /etc/hosts && ls -lrht / && java $JAVA_OPTS -Djavax.net.ssl.trustStore=/kafka-server-trustore.jks -Djava.security.egd=file:/dev/./urandom -jar /app.jar" ]
# java $JAVA_OPTS -Djava.security.auth.login.config=/tmp/microservicio.config --spring.cloud.stream.kafka.binder.configuration.security.protocol=SASL_PLAINTEXT -jar /app.jar -classpath com.platformprovider.watersupply.principal.PrincipalMsApplication



# http://docs.spring.io/autorepo/docs/spring-cloud-stream-binder-kafka-docs/1.1.0.BUILD-SNAPSHOT/reference/htmlsingle/#_example_security_configuration
# 3.4.2 Example: security configuration

# java -Djava.security.auth.login.config=/path.to/kafka_client_jaas.conf -jar log.jar \
#     --spring.cloud.stream.kafka.binder.brokers=secure.server:9092 \
#     --spring.cloud.stream.kafka.binder.zkNodes=secure.zookeeper:2181 \
#     --spring.cloud.stream.bindings.input.destination=stream.ticktock \
#     --spring.cloud.stream.kafka.binder.configuration.security.protocol=SASL_PLAINTEXT

