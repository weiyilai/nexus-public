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
package org.sonatype.nexus.internal.node.datastore;

import java.util.Collections;
import java.util.Optional;

import org.sonatype.goodies.testsupport.Test5Support;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.common.node.NodeAccess;
import org.sonatype.nexus.datastore.api.DataStoreManager;
import org.sonatype.nexus.testdb.DataSessionConfiguration;
import org.sonatype.nexus.testdb.DatabaseExtension;
import org.sonatype.nexus.testdb.DatabaseTest;
import org.sonatype.nexus.testdb.TestDataSessionSupplier;
import org.sonatype.nexus.transaction.UnitOfWork;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

/**
 * Tests for local {@link NodeAccess}.
 */
@ExtendWith(DatabaseExtension.class)
class LocalNodeAccessTest
    extends Test5Support
{
  @DataSessionConfiguration(daos = NodeIdDAO.class)
  TestDataSessionSupplier sessionRule;

  @Mock
  private EventManager eventManager;

  private NodeAccess nodeAccess;

  private NodeIdStoreImpl store;

  @BeforeEach
  void setUp() throws Exception {
    store = new NodeIdStoreImpl(sessionRule);
    store.setDependencies(eventManager);
    nodeAccess = new LocalNodeAccess(store);

    UnitOfWork.beginBatch(() -> sessionRule.openSession(DataStoreManager.DEFAULT_DATASTORE_NAME));
  }

  @AfterEach
  void tearDown() throws Exception {
    if (nodeAccess != null) {
      nodeAccess.stop();
    }
    UnitOfWork.end();
  }

  @DatabaseTest
  void testGeneratesId() throws Exception {
    nodeAccess.start();

    Optional<String> nodeId = store.get();
    assertThat(nodeId.isPresent(), is(true));
  }

  @DatabaseTest
  void testUsesDatabaseId() throws Exception {
    store.set("foo");
    nodeAccess.start();

    assertThat(nodeAccess.getId(), is("foo"));
  }

  @DatabaseTest
  void localIsOldestNode() throws Exception {
    nodeAccess.start();
    assertThat(nodeAccess.isOldestNode(), is(true));
  }

  @DatabaseTest
  void getMemberAliasesKeyValueEqualToIdentity() throws Exception {
    nodeAccess.start();
    assertThat(nodeAccess.getMemberAliases(),
        equalTo(Collections.singletonMap(nodeAccess.getId(), nodeAccess.getId())));
  }
}
