#!groovy

import com.redhat.*

def call(String secretName) {
    openshift.withCluster() {
        def secret = openshift.selector( "secret/${secretName}" ).object()
        new JenkinsUtils().createCredentialFromOpenShiftSecret("${secretName}", secret)
    }
}
