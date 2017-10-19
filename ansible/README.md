# Ansible

These are a set of reference playbooks to configure an application project namespace on a set of clusters.

1. Using the 'group_vars/\*.yml.example' files, rename each file to remove '.example'.  Make sure to minimally edit the following:
  - **all.yml**: `central_registry_hostname`
  - **[group].yml**: `clusterhost`
  - **[group].yml**: `*_users`


        ansible/
        ├── group_vars
        │   ├── all.yml
        │   ├── dev.yml
        │   ├── prod.yml
        │   ├── registry.yml
        │   └── stage.yml

2. Using the 'host_vars/[environment]-1.yml.example' files, rename each file to remove '.example'.  

   Depending on your [authentication method](https://docs.openshift.com/container-platform/3.6/install_config/configuring_authentication.html) to OpenShift either use: `openshift_username`/`openshift_password` or [`token`](https://docs.openshift.com/container-platform/3.6/cli_reference/get_started_cli.html#installing-the-cli).  If your configured authentication is external for example GitHub you will need to use a token.

   Since we are dealing with authentication information you may want to utilize [ansible-vault](https://docs.ansible.com/ansible/2.4/vault.html) to encrypt the host_vars.


        ansible/
        ├── host_vars
        │   ├── dev-1.yml.example
        │   ├── prod-1.yml.example
        │   ├── registry-1.yml.example
        │   └── stage-1.yml.example

3. Run the playbook:

```
ansible-playbook -i ansible/inventory.yml ansible/main.yml
```
