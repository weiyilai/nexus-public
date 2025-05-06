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
package org.sonatype.nexus.api.rest.selfhosted.blobstore;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.WebApplicationException;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.api.rest.common.blobstore.model.BlobStoreConnectionXO;
import org.sonatype.nexus.api.rest.common.blobstore.model.BlobStoreQuotaResultXO;
import org.sonatype.nexus.blobstore.ConnectionChecker;
import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.blobstore.api.BlobStoreConnectionException;
import org.sonatype.nexus.blobstore.api.BlobStoreManager;
import org.sonatype.nexus.blobstore.quota.BlobStoreQuotaResult;
import org.sonatype.nexus.blobstore.quota.BlobStoreQuotaService;
import org.sonatype.nexus.repository.blobstore.BlobStoreConfigurationStore;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class BlobStoreResourceTest
    extends TestSupport
{
  @Mock
  BlobStoreManager manager;

  @Mock
  BlobStoreConfigurationStore store;

  @Mock
  BlobStoreQuotaService quotaService;

  @Mock
  ConnectionChecker connectionChecker;

  @Mock
  BlobStore noQuota;

  @Mock
  BlobStore passing;

  @Mock
  BlobStore failing;

  BlobStoreResource resource;

  @Before
  public void setup() {
    Mockito.when(quotaService.checkQuota(noQuota)).thenReturn(null);
    Mockito.when(quotaService.checkQuota(passing)).thenReturn(new BlobStoreQuotaResult(false, "passing", "test"));
    Mockito.when(quotaService.checkQuota(failing)).thenReturn(new BlobStoreQuotaResult(true, "failing", "test"));

    Mockito.when(manager.get(ArgumentMatchers.eq("passing"))).thenReturn(passing);
    Mockito.when(manager.get(ArgumentMatchers.eq("noQuota"))).thenReturn(noQuota);
    Mockito.when(manager.get(ArgumentMatchers.eq("failing"))).thenReturn(failing);

    Map<String, ConnectionChecker> connectionCheckers = new HashMap<>();
    connectionCheckers.put("azure cloud storage", connectionChecker);

    resource = new BlobStoreResource(manager, store, quotaService, connectionCheckers);
  }

  @Test
  public void passingTest() {
    BlobStoreQuotaResultXO resultXO = resource.quotaStatus("passing");
    assertFalse(resultXO.getIsViolation());
    assertEquals(resultXO.getBlobStoreName(), "passing");
  }

  @Test
  public void failingTest() {
    BlobStoreQuotaResultXO resultXO = resource.quotaStatus("failing");
    assertTrue(resultXO.getIsViolation());
    assertEquals(resultXO.getBlobStoreName(), "failing");
  }

  @Test
  public void noQuotaTest() {
    BlobStoreQuotaResultXO resultXO = resource.quotaStatus("noQuota");
    assertFalse(resultXO.getIsViolation());
    assertEquals(resultXO.getBlobStoreName(), "noQuota");
  }

  @Test
  public void verifyConnectionTest() {
    Mockito
        .when(connectionChecker.verifyConnection(ArgumentMatchers.any(String.class), ArgumentMatchers.any(Map.class)))
        .thenReturn(true);
    resource.verifyConnection(getBlobStoreConnectionXO());
  }

  @Test(expected = WebApplicationException.class)
  public void verifyConnectionTestFail() {
    Mockito
        .when(connectionChecker.verifyConnection(ArgumentMatchers.any(String.class), ArgumentMatchers.any(Map.class)))
        .thenThrow(new RuntimeException("Fake unsuccessful connection Exception"));
    resource.verifyConnection(getBlobStoreConnectionXO());
  }

  @Test
  public void verifyConnectionTestFailWithBlobStoreConnectionException() {
    Mockito
        .when(connectionChecker.verifyConnection(ArgumentMatchers.any(String.class), ArgumentMatchers.any(Map.class)))
        .thenThrow(new BlobStoreConnectionException("Fake BlobStoreConnectionException"));
    WebApplicationException e =
        assertThrows(WebApplicationException.class, () -> resource.verifyConnection(getBlobStoreConnectionXO()));
    assertEquals(400, e.getResponse().getStatus());
    assertEquals("Fake BlobStoreConnectionException", e.getResponse().getEntity());
  }

  private BlobStoreConnectionXO getBlobStoreConnectionXO() {
    Map<String, Object> connectionDetails = new HashMap<>();
    connectionDetails.put("accountName", "some account name");
    connectionDetails.put("accountKey", "some account key");
    connectionDetails.put("containerName", "some container name");
    Map<String, Map<String, Object>> attributes = new HashMap<>();
    attributes.put("azure cloud storage", connectionDetails);
    BlobStoreConnectionXO blobStoreConnectionXO = new BlobStoreConnectionXO();
    blobStoreConnectionXO.setName("blobstoreName");
    blobStoreConnectionXO.setType("azure cloud storage");
    blobStoreConnectionXO.setAttributes(attributes);
    return blobStoreConnectionXO;
  }
}
