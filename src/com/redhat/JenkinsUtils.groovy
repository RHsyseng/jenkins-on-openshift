#!groovy
package com.redhat

import com.cloudbees.groovy.cps.NonCPS
import com.cloudbees.plugins.credentials.impl.*
import com.cloudbees.plugins.credentials.*
import com.cloudbees.plugins.credentials.domains.*
import groovy.json.JsonSlurperClassic
import jenkins.model.*
import jenkins.model.Jenkins
import hudson.security.*
import jenkins.model.JenkinsLocationConfiguration
import hudson.model.*

import java.util.logging.Level
import java.util.logging.Logger


@NonCPS
String createCredentialFromOpenShiftSecret(String id, HashMap secret) {

    String password = ""
    String username = ""

    try {

        /* for each item in the data object
         * check if the key name is password or token
         * if it is base64 decode.  Otherwise add the
         * value to username variable.
         */

        secret.data.each { key, value ->
            if ( key.toLowerCase().matches("password|token")) {
                password = new String(value.decodeBase64())
            }
            else {
                username = new String(value.decodeBase64())
            }
        }
        return createCredentials(id, username, password, "Secret Synced from OpenShift")
    }
    catch(all) {
        Logger.getLogger("com.redhat.Utils").log(Level.SEVERE, all.toString())
        throw all
    }
}

@NonCPS
String createCredentials(String id = null, String username, String password, String description) {
    try {
        if (id == null) {
            id = java.util.UUID.randomUUID().toString()
        }
        Credentials c = (Credentials) new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, id, description, username, password)
        SystemCredentialsProvider.getInstance().getStore().addCredentials(Domain.global(), c)
        return id
    }
    catch (all) {
        Logger.getLogger("com.redhat.Utils").log(Level.SEVERE, all.toString())
        throw all
    }
}
