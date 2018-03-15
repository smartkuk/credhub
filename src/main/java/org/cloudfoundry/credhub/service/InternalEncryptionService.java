package org.cloudfoundry.credhub.service;

import org.cloudfoundry.credhub.config.EncryptionKeyMetadata;
import org.cloudfoundry.credhub.constants.CipherTypes;
import org.cloudfoundry.credhub.entity.EncryptedValue;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.GCMParameterSpec;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;
import java.util.UUID;


public class InternalEncryptionService extends EncryptionService {
  public static final int GCM_TAG_LENGTH = 128;

  private final SecureRandom secureRandom;
  private final PasswordKeyProxyFactory passwordKeyProxyFactory;

  abstract CipherWrapper getCipher() throws NoSuchPaddingException, NoSuchAlgorithmException;

  abstract AlgorithmParameterSpec generateParameterSpec(byte[] nonce);

  @Override
  public EncryptedValue encrypt(EncryptionKey key, String value) throws Exception {
    return encrypt(key.getUuid(), key.getKey(), value);
  }

  public EncryptedValue encrypt(UUID canaryUuid, Key key, String value) throws Exception {
    byte[] nonce = generateNonce();
    AlgorithmParameterSpec parameterSpec = generateParameterSpec(nonce);
    CipherWrapper encryptionCipher = getCipher();

    encryptionCipher.init(Cipher.ENCRYPT_MODE, key, parameterSpec);

    byte[] encrypted = encryptionCipher.doFinal(value.getBytes(CHARSET));

    return new EncryptedValue(canaryUuid, encrypted, nonce);
  }

  @Override
  public String decrypt(EncryptionKey key, byte[] encryptedValue, byte[] nonce) throws Exception {
    return decrypt(key.getKey(), encryptedValue, nonce);
  }

  @Override
  CipherWrapper getCipher() throws NoSuchPaddingException, NoSuchAlgorithmException {
    return new CipherWrapper(Cipher.getInstance(CipherTypes.GCM.toString()));
  }

  @Override
  AlgorithmParameterSpec generateParameterSpec(byte[] nonce) {
    return new GCMParameterSpec(GCM_TAG_LENGTH, nonce);
  }

  @Override
  KeyProxy createKeyProxy(EncryptionKeyMetadata encryptionKeyMetadata) {
    return passwordKeyProxyFactory.createPasswordKeyProxy(encryptionKeyMetadata, this);
  }
}

