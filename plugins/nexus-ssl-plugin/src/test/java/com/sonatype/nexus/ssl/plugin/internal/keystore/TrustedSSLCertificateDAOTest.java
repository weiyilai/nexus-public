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
package com.sonatype.nexus.ssl.plugin.internal.keystore;

import java.util.List;
import java.util.Optional;

import org.sonatype.nexus.datastore.api.DataSession;
import org.sonatype.nexus.testdb.DataSessionConfiguration;
import org.sonatype.nexus.testdb.DatabaseExtension;
import org.sonatype.nexus.testdb.DatabaseTest;
import org.sonatype.nexus.testdb.TestDataSessionSupplier;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.sonatype.nexus.datastore.api.DataStoreManager.DEFAULT_DATASTORE_NAME;

@ExtendWith(DatabaseExtension.class)
class TrustedSSLCertificateDAOTest
{
  @DataSessionConfiguration(daos = TrustedSSLCertificateDAO.class)
  TestDataSessionSupplier sessionRule;

  private DataSession<?> session;

  private TrustedSSLCertificateDAO dao;

  @BeforeEach
  public void setup() {
    session = sessionRule.openSession(DEFAULT_DATASTORE_NAME);
    dao = session.access(TrustedSSLCertificateDAO.class);
  }

  @AfterEach
  public void cleanup() {
    session.close();
  }

  @DatabaseTest
  public void testCreateReadUpdateDeleteOperations() {
    TrustedSSLCertificateData entity = new TrustedSSLCertificateData();
    entity.setAlias("keystorename");
    entity.setPem("somePemFormat");
    // Save the
    boolean saveResult = dao.save(entity);
    assertThat(saveResult, is(true));

    Optional<TrustedSSLCertificateData> readBack = dao.find(entity.getAlias());
    assertThat(readBack.isPresent(), is(true));
    assertThat(readBack.get().getAlias(), is(entity.getAlias()));
    assertThat(readBack.get().getPem(), is(entity.getPem()));

    entity.setPem("somePemFormat2");
    boolean updateResult = dao.save(entity);
    assertThat(updateResult, is(true));

    Optional<TrustedSSLCertificateData> updated = dao.find(entity.getAlias());
    assertThat(updated.isPresent(), is(true));
    assertThat(updated.get().getAlias(), is(entity.getAlias()));
    assertThat(updated.get().getPem(), is(entity.getPem()));

    List<TrustedSSLCertificateData> all = dao.findAll();
    assertThat(all.size(), is(1));

    boolean deleteResult = dao.delete(entity.getAlias());
    assertThat(deleteResult, is(true));

    Optional<TrustedSSLCertificateData> deleted = dao.find(entity.getAlias());
    assertThat(deleted.isPresent(), is(false));
  }
}
