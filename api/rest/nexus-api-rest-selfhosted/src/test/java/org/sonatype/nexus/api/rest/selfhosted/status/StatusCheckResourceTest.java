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
package org.sonatype.nexus.api.rest.selfhosted.status;

import com.codahale.metrics.health.HealthCheck.Result;
import com.codahale.metrics.health.HealthCheckRegistry;
import java.util.SortedMap;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class StatusCheckResourceTest
{
  @Mock
  private HealthCheckRegistry registry;

  private StatusCheckResource resource;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    resource = new StatusCheckResource(registry);
  }

  @Test
  public void returnsTheSystemStatusChecks() {
    SortedMap<String, Result> expectedStatusChecks = mock(SortedMap.class);
    when(registry.runHealthChecks()).thenReturn(expectedStatusChecks);
    SortedMap<String, Result> systemStatusChecks = resource.getSystemStatusChecks();
    assertEquals(expectedStatusChecks, systemStatusChecks);
  }
}
