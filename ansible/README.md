# Ansible

These are a set of reference playbooks to configure an application project namespace on a set of clusters.

1. Edit file `project_bootstrap.sh` and run on appropriate clusters.

        ./project_bootstrap.sh oc login DEVURL:8443 --token SESSION_TOKEN
        ./project_bootstrap.sh oc login STAGEURL:8443 --token SESSION_TOKEN
        ./project_bootstrap.sh oc login PRODURL:8443 --token SESSION_TOKEN
        ./project_bootstrap.sh oc login REGISTRYURL:8443 --token SESSION_TOKEN
1. Using the 'examplehost' file, create a layout like the following directory structure, and add values from step 1.

        ansible/
        ├── host_vars
        │   ├── dev
        │   ├── prod
        │   ├── registry
        │   └── stage
1. Edit 'group_vars' files with the appropriate permissions lists.

        ansible/
        ├── group_vars
        │   ├── all
        │   └── nonproduction
1. The registry ansible token does not need as much privilege. Let's tweak that:

        oc policy add-role-to-user registry-admin system:serviceaccounts:lifecycle:ansible
        oc policy remove-role-from-user admin system:serviceaccounts:lifecycle:ansible
1. Run the playbook:

        ansible-playbook -i ansible/inventory.yml ansible/main.yml
