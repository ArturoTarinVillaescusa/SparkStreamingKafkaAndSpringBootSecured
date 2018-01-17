#!/bin/bash
METADATA_JOB_ENABLE=${METADATA_JOB_ENABLE:=false}

# Select image mode
if [[ "$METADATA_JOB_ENABLE" == "true" ]];then
  METADATA_DEBUG=${METADATA_DEBUG:=false}
  DEBUG_OPT=""
  if [[ "$METADATA_DEBUG" == "true" ]]; then
    DEBUG_OPT="-x"
  fi
  bash ${DEBUG_OPT} /metadata/entrypoint.sh
else
  bash /audit-entrypoint.sh
fi
