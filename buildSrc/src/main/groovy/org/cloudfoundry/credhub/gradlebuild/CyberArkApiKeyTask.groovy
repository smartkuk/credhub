package org.cloudfoundry.credhub.gradlebuild

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

class CyberArkApiKeyTask extends DefaultTask {
    String cyberArkApiKeyPath = "/tmp/conjur_api_key"
    String cyberArkApiKeyApplicationYmlDestinationPath

    @TaskAction
    def setCyberArkApiKey() {
        File cyberArkApiKeyFile = new File(cyberArkApiKeyPath)
        def apiKeyYmlFile = new File(cyberArkApiKeyApplicationYmlDestinationPath)

        if (cyberArkApiKeyFile.exists()) {
            def apiKey = cyberArkApiKeyFile.readLines()[0]
            apiKeyYmlFile.write("cyberark:\n  api-key: ${apiKey}")
        }
    }
}
