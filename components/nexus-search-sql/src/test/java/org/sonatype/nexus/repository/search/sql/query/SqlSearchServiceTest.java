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
package org.sonatype.nexus.repository.search.sql.query;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.repository.content.AssetInfo;
import org.sonatype.nexus.repository.content.store.FormatStoreManager;
import org.sonatype.nexus.repository.search.AssetSearchResult;
import org.sonatype.nexus.repository.search.ComponentSearchResult;
import org.sonatype.nexus.repository.search.SearchRequest;
import org.sonatype.nexus.repository.search.SortDirection;
import org.sonatype.nexus.repository.search.sql.ExpressionGroup;
import org.sonatype.nexus.repository.search.sql.SearchResult;
import org.sonatype.nexus.repository.search.sql.SqlSearchResultDecorator;
import org.sonatype.nexus.repository.search.sql.index.SqlSearchEventHandler;
import org.sonatype.nexus.repository.search.sql.store.SearchStore;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SqlSearchServiceTest
    extends TestSupport
{
  @Mock
  private SearchStore searchStore;

  @Mock
  private SqlSearchSortUtil sqlSearchSortUtil;

  @Mock
  private ExpressionBuilder expressionBuilder;

  @Mock
  private ExpressionGroup expressionGroup;

  @Mock
  private SearchRequestModifier requestModifier;

  @Mock
  private SearchConditionFactory queryFactory;

  @Mock
  private Map<String, FormatStoreManager> formatStoreManagersByFormat;

  @Mock
  private Set<SqlSearchResultDecorator> decorators;

  @Mock
  private SqlSearchEventHandler sqlSearchEventHandler;

  private SqlSearchService underTest;

  @Before
  public void setUp() {
    underTest = new SqlSearchService(searchStore, sqlSearchSortUtil, new ArrayList<>(), expressionBuilder,
        requestModifier, queryFactory, decorators, sqlSearchEventHandler);

    // Mock requestModifier to return the original request
    when(requestModifier.modify(any(SearchRequest.class))).thenAnswer(i -> i.getArgument(0));
  }

  /**
   * Test that the browse method returns an iterator that fetches results in batches
   */
  @Test
  public void testBrowseMethodWithBatching() {
    // Create a search request with limit of 2 to test small batches
    SearchRequest searchRequest = SearchRequest.builder().limit(2).build();

    // Mock the response from searchStore for the first batch
    List<SearchResult> firstBatchResults = Arrays.asList(createMockSearchResult("repo1", "component1", 1),
        createMockSearchResult("repo1", "component2", 2));

    // Mock the response from searchStore for the second batch
    List<SearchResult> secondBatchResults = Arrays.asList(createMockSearchResult("repo1", "component3", 3),
        createMockSearchResult("repo1", "component4", 4));

    // Mock the response from searchStore for the third batch (empty to end iteration)
    List<SearchResult> emptyBatchResults = Collections.emptyList();

    // Set up the sorting utilities
    when(sqlSearchSortUtil.getSortExpression(any())).thenReturn(Optional.empty());
    when(sqlSearchSortUtil.getSortDirection(any())).thenReturn(Optional.of(SortDirection.ASC));

    // Mock the expression builder and query factory
    SqlSearchQueryConditionGroup queryCondition = mock(SqlSearchQueryConditionGroup.class);
    Optional<ExpressionGroup> expression = Optional.of(expressionGroup);
    when(expressionBuilder.from(any(SearchRequest.class))).thenReturn(expression);
    when(queryFactory.build(expression.get())).thenReturn(queryCondition);

    // Set up the searchStore to return different results for different offset values
    // First call with offset 0 returns firstBatchResults
    when(searchStore.searchComponents(eq(2), eq(0), eq(queryCondition), isNull(), eq(SortDirection.ASC)))
        .thenReturn(firstBatchResults);

    // Second call with offset 2 returns secondBatchResults
    when(searchStore.searchComponents(eq(2), eq(2), eq(queryCondition), isNull(), eq(SortDirection.ASC)))
        .thenReturn(secondBatchResults);

    // Third call with offset 4 returns emptyBatchResults
    when(searchStore.searchComponents(eq(2), eq(4), eq(queryCondition), isNull(), eq(SortDirection.ASC)))
        .thenReturn(emptyBatchResults);

    // Execute the browse method to get the iterator
    Iterable<ComponentSearchResult> iterable = underTest.browse(searchRequest);
    Iterator<ComponentSearchResult> iterator = iterable.iterator();

    // Collect results from the iterator
    List<ComponentSearchResult> allResults = new ArrayList<>();
    while (iterator.hasNext()) {
      allResults.add(iterator.next());
    }

    // Verify results
    assertThat(allResults, hasSize(4));

    // Verify component names
    assertThat(allResults.get(0).getName(), equalTo("component1"));
    assertThat(allResults.get(1).getName(), equalTo("component2"));
    assertThat(allResults.get(2).getName(), equalTo("component3"));
    assertThat(allResults.get(3).getName(), equalTo("component4"));

    // Verify the store was called multiple times with different offsets
    verify(searchStore, times(1)).searchComponents(eq(2), eq(0), any(), any(), any());
    verify(searchStore, times(1)).searchComponents(eq(2), eq(2), any(), any(), any());
    verify(searchStore, times(1)).searchComponents(eq(2), eq(4), any(), any(), any());
  }

  @Test
  public void testBuildAssetSearchMapsBlobCreatedFromAssetCreated() {
    // Create specific timestamps for blobCreated and asset created
    OffsetDateTime blobCreatedTime = OffsetDateTime.now().minusDays(1);
    OffsetDateTime assetCreatedTime = OffsetDateTime.now().minusDays(2);

    AssetInfo assetInfo = mock(AssetInfo.class);
    when(assetInfo.assetId()).thenReturn(100);
    when(assetInfo.path()).thenReturn("/path/to/asset.jar");
    when(assetInfo.contentType()).thenReturn("application/java-archive");
    when(assetInfo.blobCreated()).thenReturn(blobCreatedTime);
    when(assetInfo.created()).thenReturn(assetCreatedTime);
    when(assetInfo.createdBy()).thenReturn("admin");
    when(assetInfo.createdByIp()).thenReturn("127.0.0.1");
    when(assetInfo.blobSize()).thenReturn(1024L);
    when(assetInfo.attributes()).thenReturn(mock(NestedAttributesMap.class));
    when(assetInfo.attributes().backing()).thenReturn(new HashMap<>());
    when(assetInfo.checksums()).thenReturn(new HashMap<>());

    SearchResult componentInfo = createMockSearchResult("maven-central", "my-artifact", 1);

    AssetSearchResult result = underTest.buildAssetSearch(assetInfo, "maven-central", componentInfo);

    // Verify that blobCreated comes from asset.created()
    Date expectedBlobCreated = Date.from(assetCreatedTime.toInstant());
    assertThat(result.getBlobCreated(), equalTo(expectedBlobCreated));

    // Also verify that lastModified comes from asset.blobCreated()
    Date expectedLastModified = Date.from(blobCreatedTime.toInstant());
    assertThat(result.getLastModified(), equalTo(expectedLastModified));

    // Verify other fields
    assertThat(result.getPath(), equalTo("/path/to/asset.jar"));
    assertThat(result.getRepository(), equalTo("maven-central"));
    assertThat(result.getContentType(), equalTo("application/java-archive"));
    assertThat(result.getUploader(), equalTo("admin"));
    assertThat(result.getUploaderIp(), equalTo("127.0.0.1"));
    assertThat(result.getFileSize(), equalTo(1024L));
  }

  /**
   * Helper method to create a mock SearchResult with specified values
   */
  private SearchResult createMockSearchResult(
      final String repositoryName,
      final String componentName,
      final Integer componentId)
  {
    SearchResult result = mock(SearchResult.class);
    when(result.repositoryName()).thenReturn(repositoryName);
    when(result.componentName()).thenReturn(componentName);
    when(result.componentId()).thenReturn(componentId);
    when(result.format()).thenReturn("maven");
    when(result.namespace()).thenReturn("group");
    when(result.version()).thenReturn("1.0");
    when(result.lastModified()).thenReturn(OffsetDateTime.now());
    return result;
  }
}
