#!/bin/bash
# __mail__ governance@platformprovider.com
# TODO remove comment when work
# set -e

### Commons properties
STORAGE_PATH=${STORAGE_PATH:=/root}
APPLICATION_JAR_DIR=${STORAGE_PATH}
BUNDLE_CA_LOCATION=${STORAGE_PATH}/ca-bundle.jks

### KAFKA CONFIGURATION ###
KAFKA_TOPIC=${KAFKA_TOPIC:=sec-kafka}
KAFKA_GROUP_ID=${KAFKA_GROUP_ID:=groupId}
KAFA_SEC_ENABLED=${KAFA_SEC_ENABLED:=true}
KAFKA_SERVERS=${KAFKA_SERVERS:=localhost:9092}
KAFKA_KEY_PASSWORD=${KAFKA_KEY_PASSWORD:=platformprovider}
KAFKA_TRUSTSTORE_LOCATION=${STORAGE_PATH}/kafka-server-trustore.jks
KAFKA_KEYSTORE_LOCATION=${STORAGE_PATH}/client_kafka.jks

###SPARK KERBEROS CONF WITH MESOS ###
METADATAJOB_CPUS=${METADATAJOB_CPUS:=2}
SPARK_USER=${SPARK_USER:=userGosec}
SPARK_HOME=/opt/sds/spark
# Work around the solution
# SPARK_MASTER=mesos://zk://master.mesos:2181/mesos
SPARK_MASTER="local[$METADATAJOB_CPUS]"
SPARK_JOB_EXECUTOR_IMAGE=${SPARK_JOB_EXECUTOR_IMAGE:=qa.platformprovider.com/platformprovider/spark-mesos-executor-env:0.1}
SPARK_SSL_ENABLED=${SPARK_SSL_ENABLED:=false}
SPARK_SSL_ENABLED_ALGORITHMS=${SPARK_SSL_ENABLED_ALGORITHMS:=TLS_RSA_WITH_AES_256_GCM_SHA384,TLS_DHE_RSA_WITH_AES_256_GCM_SHA384,TLS_ECDH_ECDSA_WITH_AES_256_GCM_SHA384,TLS_ECDH_RSA_WITH_AES_256_CBC_SHA384,TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384}
SPARK_SSL_KEYSTORE=${SPARK_SSL_KEYSTORE:=/root/keystore}
SPARK_SSL_KEYSTORE_PASSWORD=${SPARK_SSL_KEYSTORE_PMETADATAJOB_CPUSASSWORD:=platformprovider}
SPARK_SSL_TRUSTSTORE=${SPARK_SSL_TRUSTSTORE:=/root/truststore.jks}
SPARK_SSL_TRUSTSTORE_PASSWORD=${SPARK_SSL_TRUSTSTORE_PASSWORD:=platformprovider}
SPARK_SSL_PROTOCOL=${SPARK_SSL_PROTOCOL:=TLSv1.2}
SPARK_SSL_KEYPASSWORD=${SPARK_SSL_KEYPASSWORD=platformprovider}

### METADATA properties
METADATA_CLUSTER=${METADATA_CLUSTER:=userland}
METADATA_INTANCE=${METADATA_INTANCE:=dg-job}
METADATA_KEYSTORE_PATH=${STORAGE_PATH}/${METADATA_INTANCE}.jks
METADATA_KEYTAB_PATH=${STORAGE_PATH}/${METADATA_INTANCE}.keytab
METADATA_JOB_ENABLE=${METADATA_JOB_ENABLE:=false}
METADATA_SINK_SECONDS=${METADATA_SINK_SECONDS:=5}
METADATA_TAG_FIELD=${METADATA_TAG_FIELD:=generic_type}
METADATA_INDEX_PROPERTY=${METADATA_INDEX_PROPERTY:=resource}
METADATA_INDEX=${METADATA_INDEX:=dg-metadata}
METADATA_DATASOURCE=${METADATA_DATASOURCE:=org.elasticsearch.spark.sql}
METADATA_ORG_ELASTICSEARCH_SPARK_SQL_NODES=${METADATA_ORG_ELASTICSEARCH_SPARK_SQL_NODES:=localhost}
METADATA_ORG_ELASTICSEARCH_SPARK_SQL_PORT=${METADATA_ORG_ELASTICSEARCH_SPARK_SQL_PORT:=9200}
METADATA_ORG_ELASTICSEARCH_SPARK_SQL_WAN_ONLY=${METADATA_ORG_ELASTICSEARCH_SPARK_SQL_WAN_ONLY:=false}
METADATA_CONF_DIR=${METADATA_CONF_DIR:=/etc/sds/dg-job}
METADATA_CONF_LOG=${METADATA_CONF_DIR}/log4j.properties
METADATAJOB_APP_NAME=PlatformProviderMetadataJob
METADATAJOB_CPUS=${METADATA_CPUS:=2}
METADATAJOB_EXECUTOR_MEM=${METADATA_EXECUTOR_MEM:=512}
METADATA_LOG_DIR=/var/log/sds/dg-job
METADATA_FILEBEAT_LEVEL_LOG=${METADATA_FILEBEAT_LEVEL_LOG:=INFO}
METADATA_FILE_LEVEL_LOG=${METADATA_FILE_LEVEL_LOG:=INFO}

###VAULT PROPERTIES COMMON
VAULT_PORT=${VAULT_PORT:=8200}
VAULT_TOKEN=${VAULT_TOKEN:=aaa-bbb-ccc}
VHOSTS=${VHOSTS:=localhost}
IFS=',' read -ra VAULT_HOSTS <<< "$VHOSTS"



function main() {
  source /commons.sh
  source /kms_utils.sh
  # Kafka
  configure_kafka ${METADATA_CLUSTER} ${METADATA_INTANCE} ${STORAGE_PATH}
  KAFKA_KEYSTORE_PASSWORD=${DG_JOB_KEYSTORE_PASS} # Intance id (uppercase)+ KEYSTORE_PASS, replacing - to _
  KAFKA_KEY_PASSWORD=$KAFKA_KEYSTORE_PASSWORD
  # Get ca bundle
  getCAbundle ${STORAGE_PATH} JKS
  KAFKA_TRUSTSTORE_PASSWORD=${DEFAULT_KEYSTORE_PASS}


  # Keytab
  getKrb ${METADATA_CLUSTER} ${METADATA_INTANCE} ${METADATA_INTANCE} ${STORAGE_PATH}


  # Generate config
  generate_metadata_application_conf \
    "$KAFKA_TOPIC" \
    "$KAFKA_GROUP_ID" \
    "$KAFKA_SERVERS" \
    "$KAFKA_TRUSTSTORE_LOCATION" \
    "$KAFKA_TRUSTSTORE_PASSWORD" \
    "$KAFKA_KEYSTORE_LOCATION" \
    "$KAFKA_KEYSTORE_PASSWORD" \
    "$KAFKA_KEY_PASSWORD" \
    "$METADATAJOB_APP_NAME" \
    "$METADATA_SINK_SECONDS" \
    "$METADATA_JOB_ENABLE" \
    "$METADATA_TAG_FIELD" \
    "$METADATA_INDEX_PROPERTY" \
    "$METADATA_INDEX" \
    "$METADATA_DATASOURCE" \
    "$METADATA_ORG_ELASTICSEARCH_SPARK_SQL_NODES" \
    "$METADATA_ORG_ELASTICSEARCH_SPARK_SQL_PORT" \
    "$METADATA_ORG_ELASTICSEARCH_SPARK_SQL_WAN_ONLY"


  # Put configuration in the correct place
  make_directory ${METADATA_CONF_DIR} "METADATAJOB"
  mv -f /tmp/application.properties.tmp ${METADATA_CONF_DIR}/application.properties
  bash /metadata/log4j.properties.template ${METADATA_FILEBEAT_LEVEL_LOG} ${METADATA_FILE_LEVEL_LOG} > ${METADATA_CONF_LOG}

  mv -f ${APPLICATION_JAR_DIR}/jobs.jar .

  make_directory ${METADATA_LOG_DIR} "METADATAJOB"



  # Put own key store and keytab as the job need
  mv ${METADATA_KEYSTORE_PATH} ${KAFKA_KEYSTORE_LOCATION}
  mv ${BUNDLE_CA_LOCATION} ${KAFKA_TRUSTSTORE_LOCATION}

  SPARK_KEYTAB=$METADATA_KEYTAB_PATH

  # TODO REMOVE IF WORK WITHOUT THIS LINES
  # cp $KAFKA_KEYSTORE_LOCATION .

  # cp $KAFKA_TRUSTSTORE_LOCATION .

  METADATA_EXECUTOR_MEM+=M


  if [ "false" = ${SPARK_SSL_ENABLED} ]; then
     SECURITY_OPTIONS=""
  else
     get_spark_security
     SECURITY_OPTIONS=""
  fi

  ${SPARK_HOME}/bin/spark-submit \
    --master ${SPARK_MASTER} \
    --name ${METADATAJOB_APP_NAME} \
    --class com.platformprovider.gosec.jobs.spark.streaming.MetadataJob \
    --driver-java-options "-Dlog4j.configuration=file:///${METADATA_CONF_LOG}" \
    jobs.jar \
    ${METADATA_CONF_DIR}/application.properties
}

function generate_metadata_application_conf(){
  local kafka_topic=$1
  local kafka_group_id=$2
  local kafka_servers=$3
  local kafka_truststore_location=$4
  local kafka_truststore_password=$5
  local kafka_keystore_location=$6
  local kafka_keystore_password=$7
  local kafka_key_password=$8
  local metadatajob_app_name=$9
  local metadatajob_sink_seconds=${10}
  local ssjob_metadata_enabled=${11}
  local ssjob_metadata_tagField=${12}
  local ssjob_metadata_indexProperty=${13}
  local ssjob_metadata_index=${14}
  local ssjob_metadata_datasource=${15}
  local ssjob_metadata_org_elasticsearch_spark_sql_nodes=${16}
  local ssjob_metadata_org_elasticsearch_spark_sql_port=${17}
  local ssjob_metadata_org_elasticsearch_spark_sql_nodes_wan_only=${18}

  cat << EOF > /tmp/application.properties.tmp

kafka.topic=${kafka_topic}
kafka.group.id=${kafka_group_id}
kafka.key.deserializer=org.apache.kafka.common.serialization.StringDeserializer
kafka.value.deserializer=org.apache.kafka.common.serialization.StringDeserializer
kafka.bootstrap.servers=${kafka_servers}
kafka.security.protocol= SSL
kafka.ssl.truststore.location=${kafka_truststore_location}
kafka.ssl.truststore.password=${kafka_truststore_password}
kafka.ssl.keystore.location=${kafka_keystore_location}
kafka.ssl.keystore.password=${kafka_keystore_password}
kafka.ssl.key.password=${kafka_key_password}

ssjob.appName=${metadatajob_app_name}
ssjob.seconds=${metadatajob_sink_seconds}

ssjob.metadata.enabled=${ssjob_metadata_enabled}
ssjob.metadata.tagField=${ssjob_metadata_tagField}
ssjob.metadata.indexProperty=${ssjob_metadata_indexProperty}
ssjob.metadata.index=${ssjob_metadata_index}
ssjob.metadata.datasource=${ssjob_metadata_datasource}
ssjob.metadata.org.elasticsearch.spark.sql.es.port=${ssjob_metadata_org_elasticsearch_spark_sql_port}
ssjob.metadata.org.elasticsearch.spark.sql.es.nodes.%value=${ssjob_metadata_org_elasticsearch_spark_sql_nodes}
ssjob.metadata.org.elasticsearch.spark.sql.es.nodes.wan.only=${ssjob_metadata_org_elasticsearch_spark_sql_nodes_wan_only}
ssjob.metadata.org.elasticsearch.spark.sql.resource=${ssjob_metadata_index}/mappingType
EOF

}

main
