#!groovy

import com.redhat.*

// https://support.cloudbees.com/hc/en-us/articles/217630098-How-to-access-Changelogs-in-a-Pipeline-Job-

@NonCPS
def call(String match) {
    def file = currentBuild.changeSets.collect({
        it.items.collect {
            it.affectedFiles.find { file ->
                file.path.contains(match)
            }
        }
    })


    println( "${file}" )

    if( !file.size().asBoolean()) {
        return false
    } else {
        return (file[0][0].asBoolean())
    }
}
