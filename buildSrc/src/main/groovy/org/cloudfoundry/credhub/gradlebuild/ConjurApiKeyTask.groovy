package org.cloudfoundry.credhub.gradlebuild

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

class ConjurApiKeyTask extends DefaultTask {
    String conjurApiKeyPath = "/tmp/conjur_api_key"
    String conjurApiKeyApplicationYmlDestinationPath

    @TaskAction
    def setConjurApiKey() {
        File conjurApiKeyFile = new File(conjurApiKeyPath)
        def apiKeyYmlFile = new File(conjurApiKeyApplicationYmlDestinationPath)

        if (conjurApiKeyFile.exists()) {
            def apiKey = conjurApiKeyFile.readLines()[0]
            apiKeyYmlFile.write("conjur:\n  api-key: ${apiKey}")
        }
    }
}
