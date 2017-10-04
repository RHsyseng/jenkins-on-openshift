# Jenkins master configuration

This directory contains the configuration for the Jenkins master. The files are used by an OpenShift source-2-image (S2I) build to customize your Jenkins instance.

Do not rely on the Jenkins web interface to configure Jenkins. Edit or add files to 'configuration' to customize the instance. It will be built and re-deployed with the changes.

> NOTE: the ../vars and ../src directories provide library functions
