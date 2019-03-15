package org.cloudfoundry.conjur

import org.cloudfoundry.credhub.controllers.CredentialsController
import org.cloudfoundry.credhub.requests.BaseCredentialSetRequest
import org.cloudfoundry.credhub.views.CredentialView
import org.cloudfoundry.credhub.views.DataResponse
import org.cloudfoundry.credhub.views.FindCredentialResults
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.io.InputStream

@RestController
@RequestMapping(
    path = [ConjurCredentialsController.ENDPOINT],
    produces = arrayOf(MediaType.APPLICATION_JSON_UTF8_VALUE)
)
@Profile("conjur")
class ConjurCredentialsController(val conjurCredentialService: ConjurCredentialService) : CredentialsController {
    companion object {
        const val ENDPOINT = "/api/v1/data"
    }

    override fun generate(inputStream: InputStream): CredentialView {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    @RequestMapping(method = arrayOf(RequestMethod.PUT))
    @ResponseStatus(HttpStatus.OK)
    override fun set(requestBody: BaseCredentialSetRequest<*>): CredentialView {
        return conjurCredentialService.setCredential(requestBody)
    }

    override fun delete(credentialName: String) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getCredentialById(id: String): CredentialView {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getCredential(credentialName: String, numberOfVersions: Int?, current: Boolean): DataResponse {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun findByPath(path: String, expiresWithinDays: String): FindCredentialResults {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun findByNameLike(nameLike: String, expiresWithinDays: String): FindCredentialResults {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}
