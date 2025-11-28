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

package org.sonatype.nexus.internal.capability.storage;

import java.util.List;
import java.util.stream.StreamSupport;

import org.sonatype.nexus.datastore.api.DataSession;
import org.sonatype.nexus.testdb.DataSessionConfiguration;
import org.sonatype.nexus.testdb.DatabaseExtension;
import org.sonatype.nexus.testdb.DatabaseTest;
import org.sonatype.nexus.testdb.TestDataSessionSupplier;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;

import static java.util.stream.Collectors.toList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.sonatype.nexus.datastore.api.DataStoreManager.DEFAULT_DATASTORE_NAME;

@ExtendWith(DatabaseExtension.class)
class CapabilityStorageItemDAOTest
{
  @DataSessionConfiguration(daos = CapabilityStorageItemDAO.class)
  TestDataSessionSupplier sessionRule;

  private DataSession<?> session;

  private CapabilityStorageItemDAO dao;

  private CapabilityStorageImpl store;

  @BeforeEach
  public void setup() {
    session = sessionRule.openSession(DEFAULT_DATASTORE_NAME);
    dao = session.access(CapabilityStorageItemDAO.class);
    store = new CapabilityStorageImpl(sessionRule);
  }

  @AfterEach
  public void cleanup() {
    session.close();
  }

  @DatabaseTest
  public void itWillCreateReadUpdateDeleteAndBrowseCapabilityStorageItems() {
    // Create an item
    CapabilityStorageItemData entity =
        (CapabilityStorageItemData) store.newStorageItem(1, "type", true, "notes", ImmutableMap.of("foo", "bar"));
    dao.create(entity);

    // Read the item
    CapabilityStorageItem readEntity = dao.read(entity.getId()).orElse(null);
    assertNotNull(readEntity);
    assertEquals(1, readEntity.getVersion());
    assertEquals("type", readEntity.getType());
    assertTrue(readEntity.isEnabled());
    assertEquals("notes", readEntity.getNotes());
    assertEquals(ImmutableMap.of("foo", "bar"), readEntity.getProperties());

    // Update the item
    entity.setVersion(2);
    entity.setType("type2");
    entity.setEnabled(false);
    entity.setNotes("notes2");
    entity.setProperties(ImmutableMap.of("foo", "bar2"));
    dao.update(entity);

    // Read the updated item
    readEntity = dao.read(entity.getId()).orElse(null);
    assertNotNull(readEntity);
    assertEquals(2, readEntity.getVersion());
    assertEquals("type2", readEntity.getType());
    assertFalse(readEntity.isEnabled());
    assertEquals("notes2", readEntity.getNotes());
    assertEquals(ImmutableMap.of("foo", "bar2"), readEntity.getProperties());

    // Delete the item
    dao.delete(entity.getId());
    assertFalse(dao.read(entity.getId()).isPresent());

    // Create multiple items
    for (int i = 2; i <= 5; i++) {
      dao.create((CapabilityStorageItemData) store.newStorageItem(i, "type " + i, true, "notes " + i,
          ImmutableMap.of("foo", "bar " + i)));
    }

    // Browse the items
    List<CapabilityStorageItem> items = StreamSupport.stream(dao.browse().spliterator(), false).collect(toList());
    assertEquals(4, items.size());
  }
}
