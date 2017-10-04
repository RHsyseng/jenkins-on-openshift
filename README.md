# Jenkins on OpenShift

**Using Jenkins to Control Application Promotion between OpenShift Clusters**

## Overview

This repo has 3 primary components:

- application: code to deploy the example application
- ansible: configuration for the OpenShift environments and Jenkins pipeline bootstrapping
- jenkins: Jenkins master configuration and declarative Jenkinsfiles

## Layout

```
├── ansible                  # ansible playbooks to configure clusters, create openshift objects
├── app                      # target application being deployed
├── jenkins                  # Jenkins configuration
├── Jenkinsfile              # Main application pipeline
├── Jenkinsfile.release      # Production release pipeline
├── src                      # Jenkins library code
├── Vagrantfile              # Vagrantfile for running RHEL-based clients, oc and ansible-playbook
└── vars                     # Jenkins groovy method for Utils library
```

## Vagrant

The Vagrantfile is provided to bootstrap a local RHEL-based workstation pre-installed with client tools 'oc' and 'ansible-playbook'.

**Requirements**

- 'vagrant-triggers' plugin

        vagrant plugin install vagrant-triggers
- Install Red Hat Enterprise Linux vagrant box. [Download](https://developers.redhat.com/products/rhel/download/)
