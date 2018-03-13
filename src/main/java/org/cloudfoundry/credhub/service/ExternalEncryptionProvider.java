package org.cloudfoundry.credhub.service;

import io.grpc.StatusRuntimeException;
import org.cloudfoundry.credhub.config.EncryptionKeyMetadata;
import org.cloudfoundry.credhub.entity.EncryptedValue;
import org.cloudfoundry.credhub.service.grpc.DecryptionResponse;
import org.cloudfoundry.credhub.service.grpc.EncryptionRequest;
import org.cloudfoundry.credhub.service.grpc.EncryptionResponse;

import java.security.Key;
import java.security.SecureRandom;
import java.util.UUID;

public class ExternalEncryptionProvider implements EncryptionProvider {

  @Override
  public EncryptedValue encrypt(EncryptionKey key, String value) throws Exception {

    return null;
  }

  @Override
  public EncryptedValue encrypt(EncryptionKey key, String value) throws Exception {
    EncryptionResponse response = encrypt(key.getEncryptionKeyName(), value);
    return new EncryptedValue(key.getUuid(),response.getData().toByteArray(),response.getNonce().toByteArray());
  }

  @Override
  public String decrypt(EncryptionKey key, byte[] encryptedValue, byte[] nonce) throws Exception {
    DecryptionResponse response = decrypt(new String(encryptedValue, CHARSET), key.getEncryptionKeyName(), new String(nonce, CHARSET));
    return response.getData();
  }

  @Override
  public KeyProxy createKeyProxy(EncryptionKeyMetadata encryptionKeyMetadata) {
    return new ExternalKeyProxy(encryptionKeyMetadata, this);
  }

  private EncryptionResponse encrypt(String value, String keyId) throws Exception {
    EncryptionRequest request = EncryptionRequest.newBuilder().setData(value).setKey(keyId).build();
    EncryptionResponse response;
    try {
      response = blockingStub.encrypt(request);
    } catch (StatusRuntimeException e) {
      logger.error("Error for request: " + request.getData(), e);
      throw(e);
    }
    return response;
  }

  @Override
  public String decrypt(Key key, byte[] encryptedValue, byte[] nonce) throws Exception {
    return null;
  }

  @Override
  public SecureRandom getSecureRandom() {
    return null;
  }
}
