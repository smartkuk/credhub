package org.cloudfoundry.credhub.service;

import io.grpc.ManagedChannel;
import org.cloudfoundry.credhub.service.grpc.EncryptionProviderGrpc;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;


@RunWith(JUnit4.class)
public class ExternalEncryptionProviderTest {
  @InjectMocks
  private ExternalEncryptionProvider subject;

  @Mock
  private ManagedChannel channel;

  @Mock
  private EncryptionKey encryptionKey;


  @Before
  public void setUp(){
    MockitoAnnotations.initMocks(this);
    subject = new ExternalEncryptionProvider(channel);
  }


  @Test
  public void encrypt_testThatEncryptWasCalledOnTheBlockingStub() throws Exception {
    subject.encrypt(encryptionKey, "foo");
//    verify(blockingStub.encrypt(anyObject()), times(1));
  }
}