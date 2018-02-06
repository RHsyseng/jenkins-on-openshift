### Testing

#### Requirements

- Properly subscribed RHEL7 nodes
- OpenShift Container Platform v3.7
- Permissions to use BuildConfig objects

#### Run

```
oc create -f tests/tests-openshift-template.yaml
oc new-app --template jenkins-on-openshift-testing
```

In Jenkins create a credential named `openshift-auth` with
an account that can create projects.  This will be used to login
into OpenShift and Ansible.

```
oc start-build jenkins-on-openshift-tests
```
