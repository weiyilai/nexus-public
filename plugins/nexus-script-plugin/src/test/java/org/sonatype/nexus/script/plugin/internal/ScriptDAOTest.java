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
package org.sonatype.nexus.script.plugin.internal;

import org.sonatype.goodies.testsupport.Test5Support;
import org.sonatype.nexus.datastore.api.DataSession;
import org.sonatype.nexus.script.Script;
import org.sonatype.nexus.testdb.DataSessionConfiguration;
import org.sonatype.nexus.testdb.DatabaseExtension;
import org.sonatype.nexus.testdb.DatabaseTest;
import org.sonatype.nexus.testdb.TestDataSessionSupplier;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.sonatype.nexus.datastore.api.DataStoreManager.DEFAULT_DATASTORE_NAME;

@ExtendWith(DatabaseExtension.class)
class ScriptDAOTest
    extends Test5Support
{
  @DataSessionConfiguration(daos = ScriptDAO.class)
  TestDataSessionSupplier sessionRule;

  private DataSession<?> session;

  private ScriptDAO dao;

  @BeforeEach
  public void setUp() {
    session = sessionRule.openSession(DEFAULT_DATASTORE_NAME);
    dao = session.access(ScriptDAO.class);
  }

  @AfterEach
  public void tearDown() {
    session.close();
  }

  @DatabaseTest
  public void testCreateReadUpdateDelete() {
    ScriptData script = new ScriptData();
    script.setName("hello");
    script.setContent("log.info('hello')");

    dao.create(script);

    Script read = dao.read(script.getName()).orElse(null);

    assertThat(read, is(notNullValue()));
    assertThat(read.getName(), is(script.getName()));
    assertThat(read.getType(), is(script.getType()));
    assertThat(read.getContent(), is(script.getContent()));

    script.setContent("log.info('world')");
    dao.update(script);

    Script update = dao.read(script.getName()).orElse(null);

    assertThat(update, is(notNullValue()));
    assertThat(update.getName(), is(script.getName()));
    assertThat(update.getType(), is(script.getType()));
    assertThat(update.getContent(), is(script.getContent()));

    dao.delete(script.getName());

    assertThat(dao.read(script.getName()).isPresent(), is(false));
  }
}
