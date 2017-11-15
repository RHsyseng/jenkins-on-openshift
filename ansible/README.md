# Ansible playbooks

This is a reference Ansible playbook to configure a set of application project
namespaces on a set of OpenShift clusters, covering an application's life-cycle
through separate projects for development, staging and production, using a
common shared container image registry.

A Jenkins instance will be deployed in the development cluster that will drive
the life-cycle of the application through the environments.

## Deployment steps

1. Configure the environments: using the 'group_vars/\*.yml.example' files,
   rename each file to remove '.example', e.g.

        for i in *.example; do cp $i $(basename $i .example); done

   The directory should look like this:

        .
        ├── group_vars
        │   ├── all.yml
        │   ├── dev.yml
        │   ├── prod.yml
        │   ├── registry.yml
        │   └── stage.yml

   Edit these files and adjust the variables to your needs. At the very least
   you will have to set these:

   - **all.yml**: `central_registry_hostname`
   - **[group].yml**: `clusterhost`

   See the [Variables](#variables) section below for more details.

2. Configure authentication to each environment: using the
   'host_vars/[environment]-1.yml.example' files, rename each file to remove
   '.example'. The directory should look like this:

        .
        ├── host_vars
        │   ├── dev-1.yml
        │   ├── prod-1.yml
        │   ├── registry-1.yml
        │   └── stage-1.yml

   Then edit each of the files and set the respective authentication
   information. See the [host_vars](#host-vars) section below for more details.

3. Run the playbook:

        ansible-playbook -i inventory.yml main.yml

## Variables                                        <a id="variables"/>

Here is a description of the variables that are used in the playbooks. You
should adjust the values of these variables to suit your environment, clusters
and application.

### `group_vars/all.yml`

This file specifies variables that are common through all the environments:

| Name                      | Description                                                                    |
|---------------------------|--------------------------------------------------------------------------------|
| central_registry_hostname | The hostname[:port] of the central registry where all images will be stored.   |
| source_repo_url           | git repository URL of the pipelines to deploy                                  |
| source_repo_branch        | git branch to use for pipeline deployment                                      |
| app_template_path         | Relative path within the git repo where the application template is stored     |
| app_name                  | Name of the application                                                        |
| app_base_tag              | Base ImageStreamTag that the application will use                              |
| validate_certs            | Whether to validate the TLS certificates during cluster/registry communications |
| notify_email_list         | Email notifications from pipelines: destination                                |
| notify_email_from         | Email notifications from pipelines: from                                       |
| notify_email_replyto      | Email notifications from pipelines: reply-to                                   |

### `group_vars/[environment].yml`

There is a `.yml` file for each environment: development (`dev`), staging (`stage`),
`production`, and shared `registry`. Each of these files contains variables that
describe their respective cluster/project details:

* `clusterhost` specifies the hostname[:port] to contact the OpenShift cluster
   where the environment is hosted. Do not include the protocol (`http[s]://`).

* The variables `project_name`, `project_display_name` and `project_description`
  describe the project where the respective environment is hosted.

* A set of lists of users / groups that need permissions on the project. There's
  a list for each role: `admin`, `editor` and `viewer`. If you want to grant
  permissions to specific users, add them here. If you want to *remove*
  permissions from certain users, add them to the respective `deprecated_*`
  list.

### `host_vars/[environment]-1.yml`                       <a id="host-vars"/>

Here is where you configure authentication to each of the environments.

Depending on the
[authentication method](https://docs.openshift.com/container-platform/3.6/install_config/configuring_authentication.html)
of the OpenShift cluster, authentications can be provided either as `openshift_username`/`openshift_password` **or** as an authentication [`token`](https://docs.openshift.com/container-platform/3.6/cli_reference/get_started_cli.html#installing-the-cli).

A `token` obtained from `oc whoami -t` always works, and it's the only option if your configured authentication method requires an external login (for example GitHub).

**NOTE**: since we are dealing with authentication information you may want to
utilize [ansible-vault](https://docs.ansible.com/ansible/2.4/vault.html) to
encrypt these files in host_vars.

### How/where to set variables

The various variables are stored in different files depending on the scope they
have, and therefore they are meant to be configured through the
[group](group_vars/) or [host](host_vars/) variable files in
`{host,group}_vars/*.yml`.

However, Ansible's
[variable precedence](http://docs.ansible.com/ansible/2.4/playbooks_variables.html#variable-precedence-where-should-i-put-a-variable)
rules apply here, so it is possible to set or override some variable values in
different places.

For example: if you want to disable TLS certificate validation for the staging
environment/cluster only you can do so in its respective `group_vars/stage.yml`,
adding `validate_certs: false` there while keeping the default `validate_certs:
true` in the `all.yml` file.

You can also override values in the inventory file, and/or via the
`--extra-vars` option of the `ansible-playbook` command. See the Ansible
documentation for more details.

## Example setups

Here are some sample values for the configuration variables to address specific
needs.

### TLS/SSL certificate validation

The `validate_certs` variable is a Boolean that enables or disables TLS
certificate validation for the clusters and the registry.

It's important to keep in mind that the playbooks provided here only interact
with the configured OpenShift clusters through their API, and do not / can not
interfere with the cluster's own configuration.

Therefore: if for some reason TLS certificate validation has to be disabled for
a cluster, the cluster administrator must also take measures so that the cluster
operates accordingly.

In particular: image push/pull is performed by the container run time in each of
the nodes in the cluster. If you have to disable `validate_certs` for the
registry that is being used (`central_registry_hostname`), then the nodes will
also have to have that registry configured as an insecure registry, for example
by having it listed as an insecure registry in `/etc/containers/registries.conf`
or in `/etc/sysconfig/docker`.

### Single cluster / shared clusters

The playbook is designed to operate on four separate OpenShift clusters (one
project on each), each hosting one of the environments: development, staging,
production, registry.

It is possible to share the same cluster among various environments (potentially
all 4 running on the same cluster, on separate projects) by just pointing them
to the same `clusterhost`. This is particularly useful during local testing,
where you can run the whole stack on a single all-in-one cluster powered by
[minishift](https://github.com/minishift/minishift) or
[`oc cluster up`](https://github.com/openshift/origin/blob/master/docs/cluster_up_down.md).

However, if the *registry* project shares cluster with some other project(s)
some extra care must be taken so that the images in the registry's namespace can
be accessed by the other projects that share the same cluster.

With regard to the ability of the other projects to use images that belong to
the registry's namespace, you must grant the relevant project's service accounts
*view* permissions to the registry's project. You can do this by adding their
service accounts' groups to the `viewer_groups` in `group_vars/registry.yml`:

        viewer_groups:
          - system:serviceaccounts:dev
          - system:serviceaccounts:stage
          - system:serviceaccounts:prod

Note that these are groups, so it's `system:serviceaccounts` (with an
*s*). Adjust the names according to the `project_name` of the respective
environment.

If the `dev` project shares the same cluster with the `registry` project,
there's one additional requirement: images are built on this project, so its
*builder* service account needs privileges to *push* to the registry's
namespace. One way to achieve this is by adding that service account to the list of users with an *editor* role in `regsitry.yml`:

        editor_users:
          - system:serviceaccount:dev:builder
