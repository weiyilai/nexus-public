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
package org.sonatype.nexus.repository.rest.internal.resources;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.repository.search.SearchRequest;
import org.sonatype.nexus.repository.search.SearchResponse;
import org.sonatype.nexus.repository.search.SearchService;
import org.sonatype.nexus.repository.search.SearchUtils;
import org.sonatype.nexus.repository.search.query.SearchFilter;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SearchResourceTest
    extends TestSupport
{
  @Mock
  private SearchUtils searchUtils;

  @Mock
  private SearchService searchService;

  private SearchResource underTest;

  @Before
  public void setUp() {
    underTest = new SearchResource(searchUtils, mock(), searchService, mock(), mock(), mock(), mock());
    when(searchUtils.getSearchFilters(any())).thenReturn(List.of(new SearchFilter("q", "example")));
    when(searchService.search(any(SearchRequest.class))).thenReturn(new SearchResponse());
  }

  @Test
  public void doSearch_withDirectionASC() {
    assertDoesNotThrow(() -> underTest.doSearch(null, null, "ASC", null, null));
  }

  @Test
  public void doSearch_withDirectionAsc() {
    assertDoesNotThrow(() -> underTest.doSearch(null, null, "asc", null, null));
  }

  @Test
  public void doSearch_withDirectionNull() {
    assertDoesNotThrow(() -> underTest.doSearch(null, null, null, null, null));
  }

  @Test
  public void doSearch_withDirectionEmptyString() {
    assertDoesNotThrow(() -> underTest.doSearch(null, null, "", null, null));
  }

  @Test
  public void doSearch_withDirectionAllWhitespace() {
    assertDoesNotThrow(() -> underTest.doSearch(null, null, "   \t", null, null));
  }
}
