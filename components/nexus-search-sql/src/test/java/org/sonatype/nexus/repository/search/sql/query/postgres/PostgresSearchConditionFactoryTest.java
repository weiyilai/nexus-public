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
package org.sonatype.nexus.repository.search.sql.query.postgres;

import java.util.Collection;
import java.util.Optional;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.repository.rest.sql.SearchField;
import org.sonatype.nexus.repository.search.sql.ExpressionGroup;
import org.sonatype.nexus.repository.search.sql.query.SqlSearchQueryCondition;
import org.sonatype.nexus.repository.search.sql.query.syntax.ExactTerm;
import org.sonatype.nexus.repository.search.sql.query.syntax.LenientTerm;
import org.sonatype.nexus.repository.search.sql.query.syntax.Operand;
import org.sonatype.nexus.repository.search.sql.query.syntax.SqlPredicate;
import org.sonatype.nexus.repository.search.sql.query.syntax.StringTerm;
import org.sonatype.nexus.repository.search.sql.query.syntax.Term;
import org.sonatype.nexus.repository.search.sql.query.syntax.TermCollection;
import org.sonatype.nexus.repository.search.sql.query.syntax.WildcardTerm;

import com.google.common.collect.Iterables;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;
import static org.sonatype.nexus.repository.search.sql.query.syntax.Operand.ANY;
import static org.sonatype.nexus.repository.search.sql.query.syntax.Operand.EQ;
import static org.sonatype.nexus.repository.search.sql.query.syntax.Operand.IN;
import static org.sonatype.nexus.repository.search.sql.query.syntax.Operand.NOT_EQ;
import static org.sonatype.nexus.repository.search.sql.query.syntax.Operand.REGEX;

public class PostgresSearchConditionFactoryTest
    extends TestSupport
{
  private PostgresSearchConditionFactory underTest = new PostgresSearchConditionFactory(new PostgresSearchDB());

  @Test
  public void testInPredicate() {
    SqlSearchQueryCondition condition = predicate(IN, SearchField.REPOSITORY_NAME,
        TermCollection.create(exact("repo1"), exact("repo2"), exact("repo3")));

    assertThat(condition.getSqlConditionFormat(), is("cs.search_repository_name IN "
        + "(#{filterParams.cs_search_repository_name0}, #{filterParams.cs_search_repository_name1}, #{filterParams.cs_search_repository_name2})"));
  }

  @Test
  public void testEQPredicate() {
    SqlSearchQueryCondition condition = predicate(EQ, SearchField.REPOSITORY_NAME, exact("repo1"));

    assertThat(condition.getSqlConditionFormat(),
        is("cs.search_repository_name = #{filterParams.cs_search_repository_name0}"));
  }

  @Test
  public void testNotEqPredicate() {
    SqlSearchQueryCondition condition = predicate(NOT_EQ, SearchField.REPOSITORY_NAME, exact("repo1"));

    assertThat(condition.getSqlConditionFormat(),
        is("cs.search_repository_name <> #{filterParams.cs_search_repository_name0}"));
  }

  @Test
  public void testRegexPredicate() {
    // TODO
    SqlSearchQueryCondition condition = predicate(REGEX, SearchField.REPOSITORY_NAME, exact("repo1.*"));

    assertThat(condition.getSqlConditionFormat(),
        is("cs.search_repository_name ~ #{filterParams.cs_search_repository_name0}"));
  }

  @Test
  public void testExactTerm() {
    SqlSearchQueryCondition condition = predicate(ANY, SearchField.FORMAT_FIELD_1, exact("test.bar"));

    assertThat(condition.getSqlConditionFormat(),
        is("cs.format_field_values_1 @@ (#{filterParams.cs_format_field_values_10}::tsquery)"));
    assertThat(condition.getValues(), allOf(aMapWithSize(1), hasEntry("cs_format_field_values_10", "'test.bar'")));
  }

  @Test
  public void testLenientTerm() {
    SqlSearchQueryCondition condition = predicate(ANY, SearchField.FORMAT_FIELD_1, lenient("test.bar"));

    assertThat(condition.getSqlConditionFormat(),
        is("cs.format_field_values_1 @@ ((#{filterParams.cs_format_field_values_11}::tsquery "
            + "|| #{filterParams.cs_format_field_values_10}::tsquery))"));
    assertThat(condition.getValues(), allOf(aMapWithSize(2), hasEntry("cs_format_field_values_11", "'test.bar'"),
        hasEntry("cs_format_field_values_10", "'test' <-> 'bar'")));

    // test with a term that does not require tokenization, this should match the exact case
    condition = predicate(ANY, SearchField.FORMAT_FIELD_1, lenient("test"));

    assertThat(condition.getSqlConditionFormat(),
        is("cs.format_field_values_1 @@ (#{filterParams.cs_format_field_values_10}::tsquery)"));
    assertThat(condition.getValues(), allOf(aMapWithSize(1), hasEntry("cs_format_field_values_10", "'test'")));
  }

  @Test
  public void testWildcardTerm() {
    SqlSearchQueryCondition condition = predicate(ANY, SearchField.FORMAT_FIELD_1, wildcard("test.bar"));

    assertThat(condition.getSqlConditionFormat(),
        is("cs.format_field_values_1 @@ ((#{filterParams.cs_format_field_values_10}::tsquery "
            + "|| #{filterParams.cs_format_field_values_11}::tsquery))"));
    assertThat(condition.getValues(), allOf(aMapWithSize(2), hasEntry("cs_format_field_values_10", "'test.bar':*"),
        hasEntry("cs_format_field_values_11", "'test' <-> 'bar':*")));

    // Test where tokenization is not required
    condition = predicate(ANY, SearchField.FORMAT_FIELD_1, wildcard("test"));

    assertThat(condition.getSqlConditionFormat(),
        is("cs.format_field_values_1 @@ (#{filterParams.cs_format_field_values_10}::tsquery)"));
    assertThat(condition.getValues(), allOf(aMapWithSize(1), hasEntry("cs_format_field_values_10", "'test':*")));

    // Test with tokenization disabled
    condition = predicate(ANY, SearchField.PATHS, wildcardNoTokenization("/org/apache/tomcat/"));

    assertThat(condition.getSqlConditionFormat(), is("cs.paths @@ (#{filterParams.cs_paths0}::tsquery)"));
    assertThat(condition.getValues(), allOf(aMapWithSize(1), hasEntry("cs_paths0", "'/org/apache/tomcat/':*")));
  }

  @Test
  public void testAnyFullTextPredicate() {
    SqlSearchQueryCondition condition =
        predicate(ANY, SearchField.FORMAT_FIELD_1, TermCollection.create(wildcard("repo1"), wildcard("repo2")));

    assertThat(condition.getSqlConditionFormat(),
        is("cs.format_field_values_1 @@ (#{filterParams.cs_format_field_values_10}::tsquery "
            + "|| #{filterParams.cs_format_field_values_11}::tsquery)"));
    assertThat(condition.getValues(), allOf(aMapWithSize(2), hasEntry("cs_format_field_values_11", "'repo2':*"),
        hasEntry("cs_format_field_values_10", "'repo1':*")));
  }

  @Test
  public void shouldBeATsQuery() {
    SqlSearchQueryCondition condition = predicate(EQ, SearchField.REPOSITORY_NAME, exact("repo1"));
    assertThat(condition.getSqlConditionFormat(),
        is("cs.search_repository_name = #{filterParams.cs_search_repository_name0}"));

    condition = predicate(IN, SearchField.REPOSITORY_NAME, exact("repo1"));
    assertThat(condition.getSqlConditionFormat(),
        is("cs.search_repository_name IN (#{filterParams.cs_search_repository_name0})"));

    condition = predicate(IN, SearchField.REPOSITORY_NAME, TermCollection.create(exact("repo1"), exact("repo2")));
    assertThat(condition.getSqlConditionFormat(), is(
        "cs.search_repository_name IN (#{filterParams.cs_search_repository_name0}, #{filterParams.cs_search_repository_name1})"));
  }

  @Test
  public void shouldUseStartsWithQueryForColumnVersionWithTwoOrMoreDots() {
    SqlSearchQueryCondition condition = predicate(EQ, SearchField.VERSION, wildcard("foo.bar.other1."));
    assertThat(condition.getSqlConditionFormat(), is("cs.version ^@ #{filterParams.cs_version0}"));
    assertThat(onlyValue(condition), is("foo.bar.other1."));
  }

  @Test
  public void shouldUseStartsWithQueryForColumnNameSpaceWithTwoOrMoreDots() {
    SqlSearchQueryCondition condition = predicate(EQ, SearchField.NAMESPACE, wildcard("foo.bar."));
    assertThat(condition.getSqlConditionFormat(), is("cs.namespace ^@ #{filterParams.cs_namespace0}"));
    assertThat(onlyValue(condition), is("foo.bar."));
  }

  @Test
  public void shouldUseStartsWithQueryForColumnNameWithTwoOrMoreDots() {
    SqlSearchQueryCondition condition = predicate(EQ, SearchField.NAME, wildcard("artifact.bar.word1."));
    assertThat(condition.getSqlConditionFormat(),
        is("cs.search_component_name ^@ #{filterParams.cs_search_component_name0}"));
    assertThat(onlyValue(condition), is("artifact.bar.word1."));
  }

  @Test
  public void shouldReturnAssetPredicates() {
    assertThat(values(assetPredicate(EQ, SearchField.FORMAT_FIELD_1, wildcard("foo.test")).get()),
        containsInAnyOrder("'foo.test':*", "'foo' <-> 'test':*"));
  }

  private Optional<SqlSearchQueryCondition> assetPredicate(final Operand op, final SearchField field, final Term term) {
    return underTest.build(new ExpressionGroup(null, new SqlPredicate(op, field, term))).getAssetCondition();
  }

  private SqlSearchQueryCondition predicate(final Operand op, final SearchField field, final Term term) {
    return underTest.build(new ExpressionGroup(new SqlPredicate(op, field, term), null)).getComponentCondition();
  }

  private String onlyValue(final SqlSearchQueryCondition condition) {
    return Iterables.getOnlyElement(condition.getValues().values());
  }

  private Collection<String> values(final SqlSearchQueryCondition condition) {
    return condition.getValues().values();
  }

  private static StringTerm exact(final String term) {
    return new ExactTerm(term);
  }

  private static StringTerm lenient(final String term) {
    return new LenientTerm(term);
  }

  private static StringTerm wildcard(final String term) {
    return new WildcardTerm(term);
  }

  private static StringTerm wildcardNoTokenization(final String term) {
    return new WildcardTerm(term, false);
  }
}
