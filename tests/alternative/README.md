### Testing

This testing will use `oc cluster up` to quickly build an OpenShift cluster.

#### Requirements

Install RHEL 7.4 and follow [Host preparation](https://access.redhat.com/documentation/en-us/openshift_container_platform/3.7/html/installation_and_configuration/installing-a-cluster#host-registration)

#### Running

```
git clone https://github.com/RHsyseng/jenkins-on-openshift
oc cluster up --public-hostname='ip_address_here'

cd jenkins-on-openshift/tests
bash alternative/run.sh

```
