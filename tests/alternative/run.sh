#!/bin/bash

ansible-playbook -i hosts.ini ../ansible/main.yml

oc login localhost:8443 --insecure-skip-tls-verify --username developer --password developer
oc project dev
oc rollout status dc/jenkins

# wait a little while for jenkins to start

sleep 180

oc start-build bc/app-pipeline --wait

oc start-build bc/release-pipeline -w -e RELEASE_VERSION_TAG=1.0-1 -e UPGRADE_WITHOUT_ASKING=yes
