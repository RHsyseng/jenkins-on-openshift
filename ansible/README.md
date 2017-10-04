# Ansible

These are a set of reference playbooks to configure an application project namespace on a set of clusters.

1. Edit file `project_bootstrap.sh` and run on appropriate clusters.

        ./project_bootstrap.sh oc login DEVURL:PORT --token SESSION_TOKEN
        ./project_bootstrap.sh oc login STAGEURL:PORT --token SESSION_TOKEN
        ./project_bootstrap.sh oc login PRODURL:PORT --token SESSION_TOKEN
        ./project_bootstrap.sh oc login REGISTRYURL:PORT --token SESSION_TOKEN
1. The registry ansible token does not need as much privilege. Let's tweak that:

        oc policy add-role-to-user registry-admin system:serviceaccounts:lifecycle:ansible
        oc policy remove-role-from-user admin system:serviceaccounts:lifecycle:ansible
1. Using the 'host_vars/[environment].example' files, rename each file to remove '.example', add values from step 1 and edit as necessary for your team.

        ansible/
        ├── host_vars
        │   ├── dev
        │   ├── prod
        │   ├── registry
        │   └── stage
1. Edit 'group_vars/all' file as necessary for your team.

        ansible/
        ├── group_vars
            └── all
1. Run the playbook:

        ansible-playbook -i ansible/inventory.yml ansible/main.yml
