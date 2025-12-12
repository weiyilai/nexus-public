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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.annotation.Nonnull;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.QualifierUtil;
import org.sonatype.nexus.repository.rest.SearchMapping;
import org.sonatype.nexus.repository.rest.SearchMapping.FilterType;
import org.sonatype.nexus.repository.rest.SearchMappings;
import org.sonatype.nexus.repository.rest.internal.DefaultSearchMappings;
import org.sonatype.nexus.repository.search.SearchRequest;
import org.sonatype.nexus.repository.search.query.SearchFilter;
import org.sonatype.nexus.repository.search.sql.ExpressionGroup;
import org.sonatype.nexus.repository.search.sql.SqlSearchQueryContribution;
import org.sonatype.nexus.repository.search.sql.query.security.SqlSearchPermissionBuilder;
import org.sonatype.nexus.repository.search.sql.query.syntax.Expression;
import org.sonatype.nexus.repository.search.sql.query.syntax.SqlClause;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.repository.rest.sql.SearchField.ASSET_FORMAT_VALUE_1;
import static org.sonatype.nexus.repository.rest.sql.SearchField.ASSET_FORMAT_VALUE_2;

public class ExpressionBuilderTest
    extends TestSupport
{
  @Mock
  private SqlSearchQueryContribution defaultHandler;

  @Mock
  private SqlSearchQueryContribution groupRawHandler;

  @Mock
  private SqlSearchQueryContribution versionHandler;

  @Mock
  private SqlSearchQueryContribution architectureHandler;

  @Mock
  private SqlSearchQueryContribution osHandler;

  @Mock
  private SqlSearchPermissionBuilder permissions;

  private ExpressionBuilder underTest;

  private MockedStatic<QualifierUtil> mockedStatic;

  @Before
  public void setup() {
    mockedStatic = Mockito.mockStatic(QualifierUtil.class);
    Map<String, SqlSearchQueryContribution> handlers = getHandlersMap();
    Map<String, SearchMappings> searchMappings = getSearchMappingsMap();

    List<SqlSearchQueryContribution> handlersList = new ArrayList<>(handlers.values());
    List<SearchMappings> searchMappingsList = new ArrayList<>(searchMappings.values());

    when(defaultHandler.createPredicate(any())).thenReturn(Optional.empty());
    when(QualifierUtil.buildQualifierBeanMap(handlersList)).thenReturn(handlers);
    when(QualifierUtil.buildQualifierBeanMap(searchMappingsList)).thenReturn(searchMappings);
    underTest = new ExpressionBuilder(permissions, handlersList, searchMappingsList);
  }

  @After
  public void tearDown() {
    mockedStatic.close();
  }

  @Test
  public void shouldBuildQueryConditionsForSearchFilters() {
    SearchFilter groupRawFilter = new SearchFilter("group.raw", "junit org.mockito");
    SearchFilter versionFilter = new SearchFilter("version", "4.13 3.2.0");
    SearchRequest request = request(groupRawFilter, versionFilter);

    Expression versionExpression = mock(Expression.class);
    when(versionHandler.createPredicate(versionFilter)).thenReturn(Optional.of(versionExpression));

    Expression groupRawExpression = mock(Expression.class);
    when(groupRawHandler.createPredicate(groupRawFilter)).thenReturn(Optional.of(groupRawExpression));

    Expression expression = underTest.from(request).map(ExpressionGroup::getComponentFilters).orElse(null);

    assertThat(expression, is(notNullValue()));
    assertThat(expression, instanceOf(SqlClause.class));
    assertThat(((SqlClause) expression).expressions(), containsInAnyOrder(versionExpression, groupRawExpression));

    verify(groupRawHandler).createPredicate(groupRawFilter);
    verify(versionHandler).createPredicate(versionFilter);
    verify(permissions).build(request);

    verifyNoMoreInteractions(defaultHandler, versionHandler, groupRawHandler);
  }

  @Test
  public void shouldBuildQueryConditionsForAssetSearchFilters() {
    SearchFilter architectureFilter = new SearchFilter("architecture", "x86");
    SearchFilter osFilter = new SearchFilter("os", "Windows");
    SearchRequest request = request(architectureFilter, osFilter);

    Expression architectureExpression = mock(Expression.class);
    when(architectureHandler.createPredicate(architectureFilter)).thenReturn(Optional.of(architectureExpression));

    Expression osExpression = mock(Expression.class);
    when(osHandler.createPredicate(osFilter)).thenReturn(Optional.of(osExpression));

    Expression assetExpressions = underTest.from(request).map(ExpressionGroup::getAssetFilters).orElse(null);

    assertThat(assetExpressions, is(notNullValue()));
    assertThat(assetExpressions, instanceOf(SqlClause.class));
    assertThat(((SqlClause) assetExpressions).expressions(), containsInAnyOrder(architectureExpression, osExpression));

    verify(architectureHandler).createPredicate(architectureFilter);
    verify(osHandler).createPredicate(osFilter);
    verify(permissions).build(request);

    verifyNoMoreInteractions(architectureHandler, osHandler, permissions);
  }

  @Test
  public void shouldBuildQueryConditionForBlankValueFilter() {
    SearchFilter versionFilter = new SearchFilter("version", "4.13 3.2.0");
    SearchFilter groupRawFilter = new SearchFilter("group.raw", "");
    SearchRequest request = request(groupRawFilter, versionFilter);

    Expression versionExpression = mock(Expression.class);
    when(versionHandler.createPredicate(versionFilter)).thenReturn(Optional.of(versionExpression));

    Expression groupRawExpression = mock(Expression.class);
    when(groupRawHandler.createPredicate(groupRawFilter)).thenReturn(Optional.of(groupRawExpression));

    Expression expression = underTest.from(request).map(ExpressionGroup::getComponentFilters).orElse(null);

    assertThat(expression, notNullValue());
    assertThat(expression, instanceOf(SqlClause.class));
    assertThat(((SqlClause) expression).expressions(), containsInAnyOrder(versionExpression, groupRawExpression));

    verify(groupRawHandler).createPredicate(groupRawFilter);
    verify(versionHandler).createPredicate(versionFilter);
    verify(permissions).build(request);

    verifyNoMoreInteractions(defaultHandler, versionHandler, groupRawHandler);
  }

  private static SearchRequest request(final SearchFilter... filters) {
    return SearchRequest.builder().searchFilters(Arrays.asList(filters)).build();
  }

  @Nonnull
  private static Map<String, SearchMappings> getSearchMappingsMap() {
    SearchMappings searchMappings = () -> ImmutableList.<SearchMapping>builder()
        .addAll(new DefaultSearchMappings().get())
        .add(new SearchMapping("attributes.architecture", "architecture", "", ASSET_FORMAT_VALUE_1, FilterType.ASSET),
            new SearchMapping("attributes.os", "os", "", ASSET_FORMAT_VALUE_2, FilterType.ASSET))
        .build();
    return ImmutableMap.of("test", searchMappings);
  }

  @Nonnull
  private Map<String, SqlSearchQueryContribution> getHandlersMap() {
    Map<String, SqlSearchQueryContribution> handlers = new HashMap<>();

    handlers.put("default", defaultHandler);
    handlers.put("group.raw", groupRawHandler);
    handlers.put("version", versionHandler);
    handlers.put("architecture", architectureHandler);
    handlers.put("os", osHandler);
    return handlers;
  }
}
