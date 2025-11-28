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
package org.sonatype.nexus.internal.node;

import org.sonatype.goodies.testsupport.Test5Support;
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
class DeploymentIdDAOTest
    extends Test5Support
{
  private static final String DEPLOYMENT_ID = "aslfsd";

  @DataSessionConfiguration(daos = DeploymentIdDAO.class)
  TestDataSessionSupplier sessionRule;

  private DataSession<?> session;

  private DeploymentIdDAO dao;

  @BeforeEach
  void setup() {
    session = sessionRule.openSession(DEFAULT_DATASTORE_NAME);
    dao = session.access(DeploymentIdDAO.class);
    dao.set(DEPLOYMENT_ID);
  }

  @AfterEach
  void cleanup() {
    session.close();
  }

  @DatabaseTest
  void testGet() {
    String deploymentId = dao.get().orElse(null);

    assertThat(deploymentId, is(DEPLOYMENT_ID));
  }

  @DatabaseTest
  void testSet() {
    dao.set("DEPLOYMENT_ID");
    String deploymentId = dao.get().orElse(null);

    assertThat(deploymentId, is("DEPLOYMENT_ID"));
  }
}
