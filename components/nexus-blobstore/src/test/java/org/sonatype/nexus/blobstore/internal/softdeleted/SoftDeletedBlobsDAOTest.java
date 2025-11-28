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
package org.sonatype.nexus.blobstore.internal.softdeleted;

import java.util.Optional;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.blobstore.api.softdeleted.SoftDeletedBlob;
import org.sonatype.nexus.common.entity.Continuation;
import org.sonatype.nexus.common.time.UTC;
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
class SoftDeletedBlobsDAOTest
    extends TestSupport
{
  @DataSessionConfiguration(daos = SoftDeletedBlobsDAO.class)
  TestDataSessionSupplier sessionRule;

  private DataSession<?> session;

  private SoftDeletedBlobsDAO dao;

  private static final String FAKE_BLOB_STORE_NAME = "fakeBlobStore";

  @BeforeEach
  void setup() {
    session = sessionRule.openSession(DEFAULT_DATASTORE_NAME);
    dao = session.access(SoftDeletedBlobsDAO.class);
  }

  @AfterEach
  void cleanup() {
    session.close();
  }

  @DatabaseTest
  void testDAOOperations() {
    int limit = 100;
    Continuation<SoftDeletedBlob> emptyData = dao.readRecords(null, limit, FAKE_BLOB_STORE_NAME);
    assertThat(emptyData.isEmpty(), is(true));

    dao.createRecord(FAKE_BLOB_STORE_NAME, "blobID", UTC.now());
    Optional<SoftDeletedBlob> initialBlobID = dao.readRecords(null, limit, FAKE_BLOB_STORE_NAME).stream().findFirst();

    assertThat(initialBlobID.isPresent(), is(true));
    assertThat(initialBlobID.get().getBlobId(), is("blobID"));

    dao.deleteRecord(FAKE_BLOB_STORE_NAME, "blobID");
    Continuation<SoftDeletedBlob> newBlobs = dao.readRecords(null, limit, FAKE_BLOB_STORE_NAME);

    assertThat(newBlobs.isEmpty(), is(true));

    dao.createRecord(FAKE_BLOB_STORE_NAME, "blob1", UTC.now());
    dao.createRecord(FAKE_BLOB_STORE_NAME, "blob2", UTC.now());
    dao.createRecord(FAKE_BLOB_STORE_NAME, "blob3", UTC.now());

    assertThat(dao.readRecords(null, limit, FAKE_BLOB_STORE_NAME).size(), is(3));

    dao.deleteAllRecords(FAKE_BLOB_STORE_NAME, "100");

    assertThat(dao.readRecords(null, limit, FAKE_BLOB_STORE_NAME).size(), is(0));
  }
}
