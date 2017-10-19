#!groovy

import com.redhat.*

// https://support.cloudbees.com/hc/en-us/articles/217630098-How-to-access-Changelogs-in-a-Pipeline-Job-

@NonCPS
def call(String filePath) {
    def file = currentBuild.changeSets.collect({
        it.items.collect {
            it.affectedFiles.find { file ->
                file.path == filePath
            }
        }
    })

    if (file) {
        return true
    } else return false
}
