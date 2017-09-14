#!/bin/bash

# Script to manually bootstrap OpenShift clusters with project and serviceaccount
# Usage: ./bootstrap.sh [full oc login command]
# run against each cluster

PROJECT_NAME=${PROJECT_NAME:-lifecycle}
PROJECT_DISP=${PROJECT_DISP:-Software lifecycle}
PROJECT_DESC=${PROJECT_DESC:-Sofware Development Lifecycle (SDLC)}
TOKEN_NAME=${TOKEN_NAME:-automation}

set -x

$@

oc project ${PROJECT_NAME}
if [[ $? -ne 0 ]]; then
  oc new-project ${PROJECT_NAME} --display-name "${PROJECT_DISP}" --description "${PROJECT_DESC}"
fi
oc sa ${TOKEN_NAME}
if [[ $? -ne 0 ]]; then
  oc create serviceaccount ${TOKEN_NAME}  -n ${PROJECT_NAME}
fi
oc policy add-role-to-user admin system:serviceaccounts:${PROJECT_NAME}:${TOKEN_NAME} -n ${PROJECT_NAME}

set +x
echo "Copy+paste this service account token:"
echo ""
oc sa get-token ${TOKEN_NAME} -n ${PROJECT_NAME}
echo ""
