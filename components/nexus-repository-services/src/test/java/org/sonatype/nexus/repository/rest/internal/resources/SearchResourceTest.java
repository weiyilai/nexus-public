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

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import org.sonatype.goodies.testsupport.Test5Support;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.group.GroupFacet;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.search.SearchRequest;
import org.sonatype.nexus.repository.search.SearchResponse;
import org.sonatype.nexus.repository.search.SearchService;
import org.sonatype.nexus.repository.search.SearchUtils;
import org.sonatype.nexus.repository.search.query.SearchFilter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SearchResourceTest
    extends Test5Support
{
  @Mock
  private SearchUtils searchUtils;

  @Mock
  private SearchService searchService;

  @Mock
  private RepositoryManager repositoryManager;

  private SearchResource underTest;

  @BeforeEach
  void setUp() {
    underTest =
        new SearchResource(searchUtils, mock(), searchService, mock(), mock(), mock(), repositoryManager, mock());
    lenient().when(searchUtils.getSearchFilters(any())).thenReturn(List.of(new SearchFilter("q", "example")));
    lenient().when(searchService.search(any(SearchRequest.class))).thenReturn(new SearchResponse());
  }

  @Test
  void doSearch_withDirectionASC() {
    assertDoesNotThrow(() -> underTest.doSearch(null, null, "ASC", null, null));
  }

  @Test
  void doSearch_withDirectionAsc() {
    assertDoesNotThrow(() -> underTest.doSearch(null, null, "asc", null, null));
  }

  @Test
  void doSearch_withDirectionNull() {
    assertDoesNotThrow(() -> underTest.doSearch(null, null, null, null, null));
  }

  @Test
  void doSearch_withDirectionEmptyString() {
    assertDoesNotThrow(() -> underTest.doSearch(null, null, "", null, null));
  }

  @Test
  void doSearch_withDirectionAllWhitespace() {
    assertDoesNotThrow(() -> underTest.doSearch(null, null, "   \t", null, null));
  }

  @Test
  void doSearch_withLastUpdatedSort() {
    assertDoesNotThrow(() -> underTest.doSearch(null, "last_updated", "asc", null, null));
  }

  @Test
  void doSearch_withLastUpdatedSortDesc() {
    assertDoesNotThrow(() -> underTest.doSearch(null, "last_updated", "desc", null, null));
  }

  @Test
  void testTryExtractRepositoryFromSearch() {
    // empty
    assertThat(underTest.tryExtractRepositoryFromSearch(List.of()).apply("foo"), is("foo"));

    // simple case, not a group, no bar repository
    Repository foo = mockRepository("foo");

    Function<String, String> nameSupplier =
        underTest.tryExtractRepositoryFromSearch(List.of(new SearchFilter("repository_name", "foo bar")));
    assertThat(nameSupplier.apply("foo"), is("foo"));
    assertThat(nameSupplier.apply("bar"), is("bar"));

    Repository bar = mockRepository("bar");
    GroupFacet group = mock();
    when(bar.optionalFacet(GroupFacet.class)).thenReturn(Optional.of(group));
    List<Repository> members = List.of(foo, mockRepository("someRepo"));
    when(group.leafMembers()).thenReturn(members);

    nameSupplier = underTest.tryExtractRepositoryFromSearch(List.of(new SearchFilter("repository_name", "foo bar")));
    assertThat(nameSupplier.apply("foo"), is("foo"));
    assertThat(nameSupplier.apply("bar"), is("bar"));
    assertThat(nameSupplier.apply("someRepo"), is("bar"));
  }

  private Repository mockRepository(final String repoName) {
    Repository repo = mock();
    lenient().when(repo.getName()).thenReturn(repoName);
    lenient().when(repositoryManager.get(repoName)).thenReturn(repo);

    return repo;
  }
}
