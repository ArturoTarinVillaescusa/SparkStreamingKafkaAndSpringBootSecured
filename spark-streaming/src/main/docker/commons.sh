#!/bin/bash

function configure_kafka() {

  local vault_cluster=$1
  local vault_instance=$2
  local kafka_keystore_location_dir=$3

  getCert ${vault_cluster} ${vault_instance} ${vault_instance} JKS ${kafka_keystore_location_dir}
}

function generate_krb-conf () {
    local realm=$1
    local kdc_host=$2
    local kadmin_host=$3

    lw_realm=$(echo $REALM | tr '[:upper:]' '[:lower:]')

    cat << EOF > /tmp/krb5.conf.tmp
[libdefaults]
 default_ccache_name = /tmp/%{uid}/krb5cc_%{uid}
 default_realm = ${realm}
 dns_lookup_realm = false
 dns_lookup_kdc = false
 udp_preference_limit = 1
 renew_lifetime = 7d
[realms]
 ${realm} = {
   kdc = ${kdc_host}
   admin_server = ${kadmin_host}
   default_domain = ${lw_realm}
 }
[domain_realm]
 .${lw_realm} = ${realm}
 ${lw_realm} = ${realm}
EOF

}

function make_directory() {
	local dir=$1
	local module=$2

	mkdir -p $dir \
	&& echo "[$module] Created $dir directory" \
	|| echo "[$module] Something was wrong creating $dir directory or already exists"
}

function generate_core-site() {
  local fs_defaultfs="${1}"
  local hadoop_security_authorization="${2}"
  local hadoop_security_authentication="${3}"
  local hadoop_security_auth_to_local="${4}"

  cat << EOF > /tmp/core-site.xml.tmp
<?xml version="1.0"?>
<?xml-stylesheet type="text/xsl" href="configuration.xsl"?>

<configuration>
  <property>
    <name>fs.defaultFS</name>
    <value>hdfs://${fs_defaultfs}</value>
  </property>
  <property>
    <name>hadoop.security.authorization</name>
    <value>${hadoop_security_authorization}</value>
    <description>Values are simple or kerberos.</description>
  </property>
  <property>
    <name>hadoop.security.authentication</name>
    <value>${hadoop_security_authentication}</value>
  </property>
  <property>
    <name>hadoop.security.auth_to_local</name>
    <value>${hadoop_security_auth_to_local}</value>
  </property>
</configuration>
EOF

# Replace \n with real newlines
sed -i "s|\\n|\n|g" /tmp/core-site.xml.tmp

}

function generate_hdfs_site(){

  local dfs_permissions_enabled="${1}"
  local dfs_block_access_token_enable="${2}"
  local dfs_http_policy="${3}"
  local dfs_https_port="${4}"

cat << EOF > /tmp/hdfs-site.xml.tmp
<?xml version="1.0"?>
<?xml-stylesheet type="text/xsl" href="configuration.xsl"?>
<configuration>
  <property>
    <name>dfs.permissions.enabled</name>
    <value>${dfs_permissions_enabled}</value>
  </property>
  <property>
    <name>dfs.block.access.token.enable</name>
    <value>${dfs_block_access_token_enable}</value>
  </property>
  <property>
    <name>dfs.http.policy</name>
    <value>${dfs_http_policy}</value>
  </property>
    <property>
    <name>dfs.https.port</name>
    <value>${dfs_https_port}</value>
  </property>
</configuration>
EOF
}

function generate_application-conf(){
  local topic=$1
  local bootstrapServers=$2
  local shortTermEnabled=$3
  local shortTermDatasource=$4
  local shortTermPath=$5
  local shortTermIndex=$6
  local shortTermNodes=$7
  local shortTermPort=$8
  local longTermEnabled=$9
  local longTermDatasource=${10}
  local longTermPath=${11}

  cat << EOF > /tmp/application.conf.tmp
kafka {
  topic="${topic}"

  group.id = "groupId"

  key.deserializer = org.apache.kafka.common.serialization.StringDeserializer
  value.deserializer = org.apache.kafka.common.serialization.StringDeserializer

  bootstrap.servers = "${bootstrapServers}"
}

### Spark streaming job config ###
ssjob.seconds=5

ssjob.short-term.enabled=${shortTermEnabled}
ssjob.short-term.datasource="${shortTermDatasource}"
ssjob.short-term.parquet.path="${shorTermPath}"

ssjob.short-term.${shortTermDatasource}.es.resource="${shortTermIndex}"
ssjob.short-term.${shortTermDatasource}.es.nodes="${shortTermNodes}"
ssjob.short-term.${shortTermDatasource}.es.port="${shortTermPort}"


ssjob.long-term.enabled=${longTermEnabled}
ssjob.long-term.datasource="${longTermDatasource}"
ssjob.long-term.parquet.path="${longTermPath}"
EOF
}