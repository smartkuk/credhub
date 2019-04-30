package org.cloudfoundry.credhub.data;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import org.cloudfoundry.credhub.CredhubTestApp;
import org.cloudfoundry.credhub.DatabaseProfileResolver;
import org.cloudfoundry.credhub.entities.EncryptionKeyCanary;
import org.cloudfoundry.credhub.repositories.EncryptionKeyCanaryRepository;
import org.cloudfoundry.credhub.utils.StringUtil;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.google.common.collect.Lists.newArrayList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertNotNull;

@RunWith(SpringRunner.class)
@ActiveProfiles(value = "unit-test", resolver = DatabaseProfileResolver.class)
@AutoConfigureTestDatabase(replace = Replace.NONE)
@SpringBootTest(classes = CredhubTestApp.class)
public class EncryptionKeyCanaryDataServiceTest {

  @Autowired
  private EncryptionKeyCanaryRepository encryptionKeyCanaryRepository;

  private EncryptionKeyCanaryDataService subject;

  @Before
  public void beforeEach() {
    encryptionKeyCanaryRepository.deleteAllInBatch();
    subject = new EncryptionKeyCanaryDataService(encryptionKeyCanaryRepository);
  }

  @Test
  public void save_savesTheEncryptionCanary() {
    final EncryptionKeyCanary encryptionKeyCanary = new EncryptionKeyCanary();
    encryptionKeyCanary.setNonce("test-nonce".getBytes(StringUtil.UTF_8));
    encryptionKeyCanary.setEncryptedCanaryValue("test-value".getBytes(StringUtil.UTF_8));
    subject.save(encryptionKeyCanary);

    final List<EncryptionKeyCanary> canaries = subject.findAll();

    assertThat(canaries, hasSize(1));

    final EncryptionKeyCanary actual = canaries.get(0);

    assertNotNull(actual.getUuid());
    assertThat(actual.getUuid(), equalTo(encryptionKeyCanary.getUuid()));
    assertThat(actual.getNonce(), equalTo("test-nonce".getBytes(StringUtil.UTF_8)));
    assertThat(actual.getEncryptedCanaryValue(), equalTo("test-value".getBytes(StringUtil.UTF_8)));
  }

  @Test
  public void findAll_whenThereAreNoCanaries_returnsEmptyList() {
    assertThat(subject.findAll(), hasSize(0));
  }

  @Test
  public void findAll_whenThereAreCanaries_returnsCanariesAsAList() {

    final EncryptionKeyCanary firstCanary = new EncryptionKeyCanary();
    final EncryptionKeyCanary secondCanary = new EncryptionKeyCanary();

    subject.save(firstCanary);
    subject.save(secondCanary);

    final List<EncryptionKeyCanary> canaries = subject.findAll();
    final List<UUID> uuids = canaries.stream().map(canary -> canary.getUuid())
      .collect(Collectors.toList());

    assertThat(canaries, hasSize(2));
    assertThat(uuids, containsInAnyOrder(firstCanary.getUuid(), secondCanary.getUuid()));
  }

  @Test
  public void delete_whenThereAreCanaries_deletesTheRequestedCanaries() {
    EncryptionKeyCanary firstCanary = new EncryptionKeyCanary();
    EncryptionKeyCanary secondCanary = new EncryptionKeyCanary();
    EncryptionKeyCanary thirdCanary = new EncryptionKeyCanary();

    firstCanary = subject.save(firstCanary);
    secondCanary = subject.save(secondCanary);
    thirdCanary = subject.save(thirdCanary);

    List<EncryptionKeyCanary> canaries = subject.findAll();

    List<UUID> uuids = canaries.stream().map(canary -> canary.getUuid())
      .collect(Collectors.toList());

    assertThat(canaries, hasSize(3));
    assertThat(uuids, containsInAnyOrder(firstCanary.getUuid(), secondCanary.getUuid(), thirdCanary.getUuid()));

    subject.delete(newArrayList(firstCanary.getUuid(), thirdCanary.getUuid()));

    canaries = subject.findAll();
    uuids = canaries.stream().map(canary -> canary.getUuid())
      .collect(Collectors.toList());

    assertThat(canaries, hasSize(1));
    assertThat(uuids, containsInAnyOrder(secondCanary.getUuid()));
  }

  @Test
  public void delete_whenThereAreNoCanaries_doesNothing() {
    assertThat(subject.findAll(), hasSize(0));

    subject.delete(newArrayList(UUID.randomUUID()));

    assertThat(subject.findAll(), hasSize(0));
  }
}
