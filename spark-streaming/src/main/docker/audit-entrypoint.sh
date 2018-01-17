#!/bin/bash
echo "Starting entrypoint..."

set -m
set -x

source kms_utils.sh
source commons.sh

echo "Audit Job Entrypoint starting..."

function clean_up {
  # Try to kill the Spark Job before exiting
  echo "Killing Spark Driver: ${submissionId}"
  curl -X POST http://${MARATHON_LB}:7077/v1/submissions/kill/${submissionId}
  exit 1
}

trap clean_up SIGINT SIGTERM

# Create krb5.conf file
KERBEROS_REALM=${KERBEROS_REALM:=WATERSUPPLY.PLATFORMPROVIDER.COM}
KERBEROS_KDC_HOST=${KERBEROS_KDC_HOST:=ldap.labs.platformprovider.com:88}
KERBEROS_KADMIN_HOST=${KERBEROS_KADMIN_HOST:=ldap.labs.platformprovider.com:749}

generate_krb-conf "${KERBEROS_REALM}" "${KERBEROS_KDC_HOST}" "${KERBEROS_KADMIN_HOST}"
mv "/tmp/krb5.conf.tmp" "/www/krb5.conf"

# Create application.conf file
APP_CONF_KAFKA_TOPIC=${APP_CONF_KAFKA_TOPIC:=topic}
APP_CONF_KAFKA_BOOTSTRAP_SERVERS=${APP_CONF_KAFKA_BOOTSTRAP_SERVERS:=gosec1.node.default-cluster.paas.labs.platformprovider.com:9092,gosec2.node.default-cluster.paas.labs.platformprovider.com:9092,gosec3.node.default-cluster.paas.labs.platformprovider.com:9092}

APP_CONF_SHORT_TERM_ENABLE=${APP_CONF_SHORT_TERM_ENABLE:=false}
APP_CONF_SHORT_TERM_DATASOURCE=${APP_CONF_SHORT_TERM_DATASOURCE:="org.elasticsearch.spark.sql"}
APP_CONF_SHORT_TERM_PATH=${APP_CONF_SHORT_TERM_PATH:=/path/to/queriable-audit}
APP_CONF_SHORT_TERM_ELASTIC_INDEX=${APP_CONF_SHORT_TERM_ELASTIC_INDEX:=audit/events}
APP_CONF_SHORT_TERM_ELASTIC_NODES=${APP_CONF_SHORT_TERM_ELASTIC_NODES:=localhost}
APP_CONF_SHORT_TERM_ELASTIC_PORT=${APP_CONF_SHORT_TERM_ELASTIC_PORT:=9200}

APP_CONF_LONG_TERM_ENABLE=${APP_CONF_LONG_TERM_ENABLE:=true}
APP_CONF_LONG_TERM_DATASOURCE=${APP_CONF_LONG_TERM_DATASOURCE:="parquet"}
APP_CONF_LONG_TERM_PATH=${APP_CONF_LONG_TERM_PATH:=/path/to/long-term-audit}

generate_application-conf "${APP_CONF_KAFKA_TOPIC}" "${APP_CONF_KAFKA_BOOTSTRAP_SERVERS}" "${APP_CONF_SHORT_TERM_ENABLE}" "${APP_CONF_SHORT_TERM_DATASOURCE}" "${APP_CONF_SHORT_TERM_PATH}" "${APP_CONF_SHORT_TERM_ELASTIC_INDEX}" "${APP_CONF_SHORT_TERM_ELASTIC_NODES}" "${APP_CONF_SHORT_TERM_ELASTIC_PORT}" "${APP_CONF_LONG_TERM_ENABLE}" "${APP_CONF_LONG_TERM_DATASOURCE}" "${APP_CONF_LONG_TERM_PATH}"
mv "/tmp/application.conf.tmp" "/www/application.conf"

# Needed variables to generate a core-site.xml.

HDFS_FS_DEFAULTFS="${HDFS_FS_DEFAULTFS:=127.0.0.2:8020}"
HDFS_HADOOP_SECURITY_AUTHORIZATION="${HDFS_HADOOP_SECURITY_AUTHORIZATION:=true}"
HDFS_HADOOP_SECURITY_AUTHENTICATION="${HDFS_HADOOP_SECURITY_AUTHENTICATION:=kerberos}"

read -r -d '' auth_to_local_value << EOM
'RULE:[1:$1@$0](.*@WATERSUPPLY.PLATFORMPROVIDER.COM)s/@WATERSUPPLY.PLATFORMPROVIDER.COM//
RULE:[2:$1@$0](.*@DEMO.PLATFORMPROVIDER.COM)s/@WATERSUPPLY.PLATFORMPROVIDER.COM//
DEFAULT'
EOM

if [ -z "${HADOOP_SECURITY_AUTH_TO_LOCAL}" ];
then
  HDFS_HADOOP_SECURITY_AUTH_TO_LOCAL=${auth_to_local_value}
else
  HDFS_HADOOP_SECURITY_AUTH_TO_LOCAL=${HDFS_HADOOP_SECURITY_AUTH_TO_LOCAL/&#39;/\'}
fi

generate_core-site "${HDFS_FS_DEFAULTFS}" "${HDFS_HADOOP_SECURITY_AUTHORIZATION}" "${HDFS_HADOOP_SECURITY_AUTHENTICATION}" "${HDFS_HADOOP_SECURITY_AUTH_TO_LOCAL}"
mv "/tmp/core-site.xml.tmp" "/www/core-site.xml"

# Needed variables to generate a hdfs-site.xml.
HDFS_DFS_PERMISSIONS_ENABLED="${HDFS_DFS_PERMISSIONS_ENABLED:=false}"
HDFS_DFS_BLOCK_ACCESS_TOKEN_ENABLE="${HDFS_DFS_BLOCK_ACCESS_TOKEN_ENABLE:=True}"
HDFS_DFS_HTTP_POLICY="${HDFS_DFS_HTTP_POLICY:=HTTPS_ONLY}"
HDFS_DFS_HTTPS_PORT="${HDFS_DFS_HTTPS_PORT:=50070}"

generate_hdfs_site "${HDFS_DFS_PERMISSIONS_ENABLED}" "${HDFS_DFS_BLOCK_ACCESS_TOKEN_ENABLE}" "${HDFS_DFS_HTTP_POLICY}" "${HDFS_DFS_HTTPS_PORT}"
mv "/tmp/hdfs-site.xml.tmp" "/www/hdfs-site.xml"

# Run web server to store configuration files for Spark
WEBHOST=${WEBHOST:=${MARATHON_APP_ID//\/}.marathon.mesos}

# Configure Nginx
mv /etc/nginx/nginx.conf /etc/nginx/nginx.conf.orig
cat << EOF > /etc/nginx/nginx.conf
user                www;
worker_processes    1;

error_log           /var/log/nginx/error.log warn;
pid                 /var/run/nginx.pid;

events {
  worker_connections 1024;
}

http {
  include           /etc/nginx/mime.types;
  default_type      application/octet-stream;
  sendfile          on;
  access_log        /var/log/nginx/access.log;
  keepalive_timeout 3000;
  server {
    listen          ${PORT0};
    root            /www;
    index           index.html index.htm;
    server_name     ${WEBHOST};
    client_max_body_size  32m;
    error_page      500 502 503 504 /50x.html;
    location = /50x.html {
      root          /var/lib/nginx/html;
    }
  }
  # server
}

EOF

/usr/sbin/nginx

KERBEROS_VAULT_PATH=${KERBEROS_VAULT_PATH:=/v1/people/kerberos/platformprovider}
KAFKA_VAULT_CERT_PATH=${KAFKA_VAULT_CERT_PATH:=/v1/userland/certificates/spark-1}
KAFKA_VAULT_CERT_PASS_PATH=${KAFKA_VAULT_CERT_PASS_PATH:=/v1/userland/passwords/spark-1/keystore}
KAFKA_VAULT_KEY_PASS_PATH=${KAFKA_VAULT_KEY_PASS_PATH:=/v1/userland/passwords/spark-1/keystore}


MARATHON_LB=${MARATHON_LB:=marathon-lb.marathon.mesos}
STRING_VAULT_HOST=${VAULT_HOST:=vault.service.default-cluster.paas.labs.platformprovider.com}
OLD_IFS=$IFS
IFS=',' read -r -a VAULT_HOSTS <<< "$STRING_VAULT_HOST"
IFS=$OLD_IFS
VAULT_PORT=${VAULT_PORT:=8200}
VAULT_TOKEN=${VAULT_TOKEN:=1111111-2222-3333-4444-5555555555555}
SPARK_HOME=/usr/local/spark/spark-2.1.0-bin-hadoop2.7
SPARK_SEC_OPTIONS=""
if [[ "$SECURED_MESOS" == "true" ]]
then
  AUDIT_MESOS_ROLE=${AUDIT_MESOS_ROLE:=platformprovider}
  #Get Mesos secrets from Vault
  getPass "userland" "audit" "mesos"
  # This should populate AUDIT_MESOS_USER and AUDIT_MESOS_PASS
  #SPARK_SEC_OPTIONS="--conf spark.mesos-principal=${AUDIT_MESOS_USER} --conf spark.mesos.secret=${AUDIT_MESOS_PASS} --conf spark.mesos.role=${AUDIT_MESOS_ROLE}"
  SPARK_SEC_JSON_OPTIONS="\"spark.mesos.principal\" : \"${AUDIT_MESOS_USER}\", \"spark.mesos.secret\" : \"${AUDIT_MESOS_PASS}\", \"spark.mesos.role\" : \"${AUDIT_MESOS_ROLE}\","
else
  SPARK_SEC_JSON_OPTIONS="\"spark.mesos.role\" : \"*\","
fi

cat << EOF > request.json
{
  "action" : "CreateSubmissionRequest",
  "appArgs" : [ "http://${WEBHOST}:${PORT0}/application.conf" ],
  "appResource" : "http://${WEBHOST}:${PORT0}/jobs.jar",
  "clientSparkVersion" : "2.1.0",
  "environmentVariables" : {
    "SPARK_SCALA_VERSION" : "2.1.0"
  },
  "mainClass" : "com.platformprovider.aguas.spark.main.ParseItronJsonsArrivingFromMicroserviceKafkaStreamSink",
  "sparkProperties" : {
    "spark.secret.vault.token" : "$VAULT_TOKEN",
    "spark.mesos.driverEnv.HDFS_CONF_URI" : "http://${WEBHOST}:${PORT0}",
    "spark.jars" : "http://${WEBHOST}:${PORT0}/jobs.jar",
    "spark.mesos.executor.docker.volumes" : "/etc/pki/ca-trust/extracted/java/cacerts/:/etc/ssl/certs/java/cacerts:ro",
    "spark.driver.supervise" : "false",
    "spark.app.name" : "auditJob",
    "spark.executorEnv.KAFKA_VAULT_KEY_PASS_PATH" : "${KAFKA_VAULT_KEY_PASS_PATH}",
    ${SPARK_SEC_JSON_OPTIONS}
    "spark.mesos.driverEnv.KAFKA_VAULT_KEY_PASS_PATH" : "${KAFKA_VAULT_KEY_PASS_PATH}",
    "spark.mesos.executor.docker.image" : "master.mesos:5001/platformprovider/spark-krb-dispatcher-support:2.1.0",
    "spark.mesos.driverEnv.KERBEROS_VAULT_PATH" : "${KERBEROS_VAULT_PATH}",
    "spark.submit.deployMode" : "cluster",
    "spark.master" : "mesos://${MARATHON_LB}:7077",
    "spark.executorEnv.KAFKA_VAULT_CERT_PASS_PATH" : "${KAFKA_VAULT_CERT_PASS_PATH}",
    "spark.secret.vault.host" : "https://${STRING_VAULT_HOST}:${VAULT_PORT}",
    "spark.mesos.driverEnv.KAFKA_VAULT_CERT_PASS_PATH" : "${KAFKA_VAULT_CERT_PASS_PATH}",
    "spark.executorEnv.KAFKA_VAULT_CERT_PATH" : "${KAFKA_VAULT_CERT_PATH}",
    "spark.mesos.executor.home" : "/opt/spark/dist",
    "spark.mesos.driverEnv.KAFKA_VAULT_CERT_PATH" : "${KAFKA_VAULT_CERT_PATH}",
    "spark.executor.memory" : "${SPARK_EXECUTOR_MEMORY:=1G}",
    "spark.executor.cores" : "${SPARK_EXECUTOR_CORES:=1}",
    "spark.cores.max" : "${SPARK_MAX_CORES:=2}",
    "spark.secret.kafka.security.protocol" : "SSL"
  }
}
EOF


set -x

#Submit job to spark using curl
# Wait thirty seconds before submitting to ensure DNS is ready
sleep 30
cat request.json
json=$(curl -X POST -d @request.json http://${MARATHON_LB}:7077/v1/submissions/create)
submissionId=$(echo ${json} | jq ".submissionId")
temp=${submissionId%\"}
temp=${temp#\"}
submissionId=${temp}

# /lecturas-ms-principal-entrypoint.sh

# echo "Resultado del comando find / -name *.jks"
# find / -name *.jks
# echo "Resultado del comando ls -lrht /"
# ls -lrht /
# echo "Resultado del comando ls -lrht /tmp"
# ls -lrht /tmp
# echo "Resultado del comando ls -lrht /root"
# ls -lrht /root
# echo "Resultado del comando ls -lrht /www"
# ls -lrht /www

echo "Resultado del comando tree /"
tree /

# Health check to kill container if the audit job fails
while true; do
  ./submission-healthcheck.sh ${submissionId} ${MARATHON_LB}
  ec=$?
  if [ "$ec" -ne "0" ]
  then
    echo "Stopping entrypoint"
    exit 1
  fi
  sleep 10
done
