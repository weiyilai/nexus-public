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
package org.sonatype.nexus.quartz.internal.store;

import java.sql.Connection;
import java.sql.SQLException;

import org.sonatype.goodies.testsupport.Test5Support;
import org.sonatype.nexus.testdb.DataSessionConfiguration;
import org.sonatype.nexus.testdb.DatabaseExtension;
import org.sonatype.nexus.testdb.DatabaseTest;
import org.sonatype.nexus.testdb.TestDataSessionSupplier;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;

/**
 * Test the {@link ConfigStoreConnectionProvider}.
 */
@ExtendWith(DatabaseExtension.class)
class ConfigStoreConnectionProviderTest
    extends Test5Support
{
  @DataSessionConfiguration(postgresql = false)
  TestDataSessionSupplier sessionRule;

  private ConfigStoreConnectionProvider underTest;

  @BeforeEach
  public void setUp() {
    underTest = new ConfigStoreConnectionProvider(sessionRule);
    underTest.initialize();
  }

  @AfterEach
  public void tearDown() {
    underTest.shutdown();
    underTest = null;
  }

  @DatabaseTest
  public void testCanOpenConnection() throws SQLException {
    try (Connection connection = underTest.getConnection()) {
      if (System.getProperty("test.postgres") != null) {
        assertThat(connection.getMetaData().getURL(), startsWith("jdbc:postgresql:"));
      }
      else {
        assertThat(connection.getMetaData().getURL(), is("jdbc:h2:mem:nexus"));
      }
    }
  }
}
