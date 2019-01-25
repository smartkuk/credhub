package org.cloudfoundry.credhub.controller.v2;

import com.google.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.vault.authentication.ClientAuthentication;
import org.springframework.vault.authentication.TokenAuthentication;
import org.springframework.vault.client.VaultEndpoint;
import org.springframework.vault.config.AbstractVaultConfiguration;
import org.springframework.vault.core.VaultKeyValueOperations;
import org.springframework.vault.core.VaultKeyValueOperationsSupport;
import org.springframework.vault.core.VaultOperations;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/v1/vault")
public class VaultController {

    @Autowired
    private VaultOperations operations;

    @GetMapping(value = "", params = "key", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public String getVaultValue(
        @RequestParam("key") final String key
    ) {
        VaultKeyValueOperations value = operations.opsForKeyValue("secret", VaultKeyValueOperationsSupport.KeyValueBackend.KV_2);

        return new Gson().toJson(value.get(key).getData().values());
    }

    @Configuration
    class VaultConfiguration extends AbstractVaultConfiguration {

        @Override
        public VaultEndpoint vaultEndpoint() {
            VaultEndpoint vaultEndpoint = new VaultEndpoint();
            vaultEndpoint.setHost("127.0.0.1");
            vaultEndpoint.setPort(8200);
            vaultEndpoint.setScheme("http");

            return vaultEndpoint;
        }

        @Override
        public ClientAuthentication clientAuthentication() {
            String vaultToken = System.getenv("VAULT_TOKEN");

            return new TokenAuthentication(vaultToken);
        }
    }
}
