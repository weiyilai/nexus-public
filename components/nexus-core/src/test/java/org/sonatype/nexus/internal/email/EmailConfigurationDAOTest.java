/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
package org.sonatype.nexus.internal.email;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.crypto.secrets.Secret;
import org.sonatype.nexus.crypto.secrets.SecretsFactory;
import org.sonatype.nexus.datastore.api.DataSession;
import org.sonatype.nexus.datastore.mybatis.handlers.SecretTypeHandler;
import org.sonatype.nexus.email.EmailConfiguration;
import org.sonatype.nexus.testdb.DataSessionConfiguration;
import org.sonatype.nexus.testdb.DatabaseExtension;
import org.sonatype.nexus.testdb.DatabaseTest;
import org.sonatype.nexus.testdb.TestDataSessionSupplier;
import org.sonatype.nexus.testdb.TestTypeHandler;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.mock;
import static org.sonatype.nexus.datastore.api.DataStoreManager.DEFAULT_DATASTORE_NAME;

@ExtendWith(DatabaseExtension.class)
class EmailConfigurationDAOTest
    extends TestSupport
{
  private final SecretsFactory secretsFactory = mock(SecretsFactory.class);

  @TestTypeHandler
  SecretTypeHandler secretTypeHandler = new SecretTypeHandler(secretsFactory);

  @DataSessionConfiguration(daos = EmailConfigurationDAO.class)
  TestDataSessionSupplier sessionRule;

  private DataSession<?> session;

  private EmailConfigurationDAO dao;

  private Secret secret;

  @BeforeEach
  public void setup() {
    session = sessionRule.openSession(DEFAULT_DATASTORE_NAME);
    dao = session.access(EmailConfigurationDAO.class);
    secret = mock(Secret.class);
  }

  @AfterEach
  public void tearDown() {
    session.close();
  }

  private EmailConfigurationData createEmailConfigurationData(final Secret secret) {
    Mockito.when(secret.getId()).thenReturn("_1");
    Mockito.when(secretsFactory.from("_1")).thenReturn(secret);
    EmailConfigurationData entity = new EmailConfigurationData();
    entity.setEnabled(true);
    entity.setHost("localhost");
    entity.setPort(25);
    entity.setUsername("email_user");
    entity.setPassword(secret);
    entity.setFromAddress("foo@example.com");
    entity.setSubjectPrefix("PREFIX: ");
    entity.setStartTlsEnabled(true);
    entity.setStartTlsRequired(false);
    entity.setSslOnConnectEnabled(true);
    entity.setSslCheckServerIdentityEnabled(false);
    entity.setNexusTrustStoreEnabled(true);
    return entity;
  }

  @DatabaseTest
  public void testReadWriteSingleEmailConfiguration() {
    EmailConfigurationData entity = createEmailConfigurationData(secret);

    dao.set(entity);
    EmailConfiguration readEntity = dao.get().orElse(null);

    assertThat(readEntity, is(notNullValue()));
    assertThat(readEntity.isEnabled(), is(true));
    assertThat(readEntity.getHost(), is("localhost"));
    assertThat(readEntity.getPort(), is(25));
    assertThat(readEntity.getUsername(), is("email_user"));
    assertThat(readEntity.getPassword(), is(secret));
    assertThat(readEntity.getFromAddress(), is("foo@example.com"));
    assertThat(readEntity.getSubjectPrefix(), is("PREFIX: "));
    assertThat(readEntity.isStartTlsEnabled(), is(true));
    assertThat(readEntity.isStartTlsRequired(), is(false));
    assertThat(readEntity.isSslOnConnectEnabled(), is(true));
    assertThat(readEntity.isSslCheckServerIdentityEnabled(), is(false));
    assertThat(readEntity.isNexusTrustStoreEnabled(), is(true));
  }

  @DatabaseTest
  public void testUpdateEmailConfiguration() {
    EmailConfigurationData entity = createEmailConfigurationData(secret);
    Secret otherSecret = mock(Secret.class);
    Mockito.when(otherSecret.getId()).thenReturn("_2");
    Mockito.when(secretsFactory.from("_2")).thenReturn(otherSecret);

    dao.set(entity);
    EmailConfigurationData readEntity = dao.get().orElse(null);

    assertThat(readEntity, is(notNullValue()));
    readEntity.setEnabled(false);
    readEntity.setHost("remotehost");
    readEntity.setPort(26);
    readEntity.setUsername("email_user2");
    readEntity.setPassword(otherSecret);
    readEntity.setFromAddress("bar@example.com");
    readEntity.setSubjectPrefix("XYZ: ");
    readEntity.setStartTlsEnabled(false);
    readEntity.setStartTlsRequired(true);
    readEntity.setSslOnConnectEnabled(false);
    readEntity.setSslCheckServerIdentityEnabled(true);
    readEntity.setNexusTrustStoreEnabled(false);
    dao.set(readEntity);

    EmailConfigurationData updatedEntity = dao.get().orElse(null);

    assertThat(updatedEntity, is(notNullValue()));
    assertThat(updatedEntity.isEnabled(), is(false));
    assertThat(updatedEntity.getHost(), is("remotehost"));
    assertThat(updatedEntity.getPort(), is(26));
    assertThat(updatedEntity.getUsername(), is("email_user2"));
    assertThat(updatedEntity.getPassword(), is(otherSecret));
    assertThat(updatedEntity.getFromAddress(), is("bar@example.com"));
    assertThat(updatedEntity.getSubjectPrefix(), is("XYZ: "));
    assertThat(updatedEntity.isStartTlsEnabled(), is(false));
    assertThat(updatedEntity.isStartTlsRequired(), is(true));
    assertThat(updatedEntity.isSslOnConnectEnabled(), is(false));
    assertThat(updatedEntity.isSslCheckServerIdentityEnabled(), is(true));
    assertThat(updatedEntity.isNexusTrustStoreEnabled(), is(false));
  }

  @DatabaseTest
  public void testDeleteEmailConfiguration() {
    EmailConfigurationData entity = createEmailConfigurationData(secret);

    dao.set(entity);
    EmailConfigurationData readEntity = dao.get().orElse(null);
    assertThat(readEntity, is(notNullValue()));

    dao.clear();
    readEntity = dao.get().orElse(null);
    assertThat(readEntity, is(nullValue()));
  }
}
