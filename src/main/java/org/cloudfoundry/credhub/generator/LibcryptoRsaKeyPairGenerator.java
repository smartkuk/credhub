package org.cloudfoundry.credhub.generator;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.bouncycastle.jcajce.provider.BouncyCastleFipsProvider;

@Component
public class LibcryptoRsaKeyPairGenerator {

  @Autowired
  public LibcryptoRsaKeyPairGenerator(){
    super();
  }

  public synchronized KeyPair generateKeyPair(final int keyLength)
    throws NoSuchProviderException, NoSuchAlgorithmException {
    final BouncyCastleFipsProvider bouncyCastleProvider = new BouncyCastleFipsProvider();
    Security.addProvider(bouncyCastleProvider);

    KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA", BouncyCastleFipsProvider.PROVIDER_NAME);
    generator.initialize(keyLength);
    return generator.generateKeyPair();
  }
}
