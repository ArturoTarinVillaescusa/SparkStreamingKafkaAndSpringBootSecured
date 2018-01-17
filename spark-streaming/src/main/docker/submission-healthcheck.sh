#!/bin/bash

set -x

# Requires jq

submissionId=$1
marathon=$2



json=$(curl -X GET http://${marathon}:7077/v1/submissions/status/${submissionId})
echo ${json}
driverState=$(echo $json | jq ".driverState")
echo ${driverState}

temp=${driverState%\"}
temp=${temp#\"}
driverState=${temp}

if [[ "${driverState}" == "KILLED" ]] || [[ "${driverState}" == "FINISHED" ]] || [[ "${driverState}" == "FAILED" ]] || [[ "${driverState}" == "ERROR" ]]
then
  echo "Status NOT OK"
  exit 1
fi
echo "Status OK"
exit 0
