#!groovy

import com.redhat.*

def call(String secretName) {

    openshift.withCluster() {
        // Grab the secret object and call the createCredentialFromOpenShiftSecret.
        // This will create a credential the same name as the OpenShift secret.

        def secret = openshift.selector( "secret/${secretName}" ).object()
        new JenkinsUtils().createCredentialFromOpenShiftSecret("${secretName}", secret)
    }
}
