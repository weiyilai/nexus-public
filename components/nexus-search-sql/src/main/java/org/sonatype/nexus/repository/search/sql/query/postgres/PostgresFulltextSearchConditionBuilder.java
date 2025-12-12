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
import java.util.StringJoiner;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.sonatype.nexus.common.text.Strings2;
import org.sonatype.nexus.repository.search.sql.query.syntax.ExactTerm;
import org.sonatype.nexus.repository.search.sql.query.syntax.Expression;
import org.sonatype.nexus.repository.search.sql.query.syntax.LenientTerm;
import org.sonatype.nexus.repository.search.sql.query.syntax.Operand;
import org.sonatype.nexus.repository.search.sql.query.syntax.StringTerm;
import org.sonatype.nexus.repository.search.sql.query.syntax.WildcardTerm;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Iterables;

/**
 * Single use builder for converting {@link Expression} into postgres specific syntax
 */
public class PostgresFulltextSearchConditionBuilder
    extends PostgresSearchConditionBuilder
{
  private static final String FOLLOWED_BY = " <-> ";

  private static final String FULLTEXT_MATCH = "%s @@ (%s)";

  private static final String NESTED_OR_TSQUERY = "(%s || %s::tsquery)";

  private static final String OR_TWO_TSQUERY = "(%s::tsquery || %s::tsquery)";

  private static final String QUOTE = "'";

  private static final String TSQUERY = "%s::tsquery";

  private static final String WILDCARD = ":*";

  private static final Predicate<String> CONTAINS_TWO_OR_MORE_DOTS = Pattern.compile("^.+\\..+\\..*$").asPredicate();

  PostgresFulltextSearchConditionBuilder(final PostgresSearchDB db) {
    super(db);
  }

  PostgresFulltextSearchConditionBuilder(final PostgresSearchDB db, final ConditionType conditionType) {
    super(db, conditionType);
  }

  @Override
  protected Optional<CharSequence> createQuery(
      final PostgresSearchColumn column,
      final Operand op,
      final Collection<StringTerm> terms)
  {
    if (requiresFulltext(column, terms)) {
      return createFullText(column, op, terms);
    }
    return super.createQuery(column, op, terms);
  }

  /*
   * Full text search is used for columns that support full text and the predicate has more than a single ExactTerm
   */
  private static boolean requiresFulltext(final PostgresSearchColumn column, final Collection<StringTerm> terms) {
    if (column.supportsFulltext()) {
      // Allow exact matching when the column allows it, there is a single term, and that term is an exact term
      return !column.supportsExact() || terms.size() != 1 || !(Iterables.getOnlyElement(terms) instanceof ExactTerm);
    }
    return false;
  }

  private Optional<CharSequence> createFullText(
      final PostgresSearchColumn column,
      final Operand operand,
      final Collection<StringTerm> terms)
  {
    if (terms.stream().map(StringTerm::get).allMatch(Strings2::isBlank)) {
      return Optional.empty();
    }

    String operator;
    switch (operand) {
      case EQ:
        operator = " && ";
        break;
      case ANY:
        operator = " || ";
        break;
      case REGEX:
        return super.createQuery(column, operand, terms);
      default:
        errors.withError("Unsupported operand " + operand + " for terms " + terms);
        return Optional.empty();
    }

    // case for columns with column name, if the term is a wildcard term and the term contains two or more dots
    if (column.supportsExact() && terms.size() == 1 && Iterables.getOnlyElement(terms) instanceof WildcardTerm term
        && CONTAINS_TWO_OR_MORE_DOTS.test(term.get())) {
      return super.createStartsWithQuery(column, term);
    }

    CharSequence query = createExpression(column, operator, terms, UnaryOperator.identity());

    return Optional.of(FULLTEXT_MATCH.formatted(column.getFullTextColumn(), query));
  }

  /**
   * Builds an expression for the provided column and collection of terms. The {@code formatted} argument is expected to
   * use {@link String#format} syntax and will be used as a template for each term with the placeholder as the argument.
   *
   * @param column the database column the expression is applied to
   * @param formatted A String.format text which which will be used for each term, the placeholder text is passed into
   *          the formatter
   * @param operator the operator for the predicate
   * @param terms a collection of terms to use
   * @param termMutator
   *
   * @return the query
   */
  private CharSequence createExpression(
      final PostgresSearchColumn column,
      final String operator,
      final Collection<StringTerm> terms,
      final UnaryOperator<String> termMutator)
  {
    StringJoiner predicate = new StringJoiner(operator);

    for (StringTerm term : terms) {
      String value = termMutator.apply(term.get());

      if (term instanceof LenientTerm) {
        predicate.add(handleLenient(value, column));
      }
      else if (term instanceof ExactTerm) {
        predicate.add(handleExact(value, column));
      }
      else if (term instanceof WildcardTerm wildcard) {
        predicate.add(handleWildcard(value, wildcard.isAllowTokenization(), column));
      }
      else {
        log.debug("Unexpected term type {}", term);
      }
    }

    return predicate.toString();
  }

  public static final Pattern TOKENIZER = Pattern.compile("[.\\-\\/\\\\ ]");

  /*
   * A lenient query is one which allows tokenization but does not use a wildcard.
   *
   * If we imagine a term of 'org.apache.tomcat' lenient produces a query string like:
   *
   * ({exact} || $$'org' <-> 'apache' <-> 'tomcat'$$::tsquery)
   *
   * Where {exact} is the value provided by handleExact
   */
  private String handleLenient(final String term, final PostgresSearchColumn column) {
    String tokenized = tokenizeTsQuery(term);

    if (isNotTokenized(tokenized)) {
      return handleExact(term, column);
    }

    String tokenizedParam = param(column.getFullTextColumn());
    parameters.put(tokenizedParam, tokenized);

    return NESTED_OR_TSQUERY.formatted(handleExact(term, column), placeholder(tokenizedParam));
  }

  /*
   * An exact query does not allow tokenization or wildcards
   *
   * If we imagine a term of 'org.apache.tomcat' exact produces a query string like:
   *
   * 'org.apache.tomcat'::tsquery
   */
  private String handleExact(final String term, final PostgresSearchColumn column) {
    String param = param(column.getFullTextColumn());
    parameters.put(param, tsEscape(term));

    return TSQUERY.formatted(placeholder(param));
  }

  /*
   * A wildcard query is one which allows tokenization and wildcards.
   *
   * If we imagine a term of 'org.apache.tomcat' wildcard produces a query string like:
   *
   * ($$'org.apache.tomcat':*$$::tsquery || $$'org' <-> 'apache' <-> 'tomcat:*'$$::tsquery)
   */
  private String handleWildcard(final String term, final boolean allowTokenization, final PostgresSearchColumn column) {
    String param = param(column.getFullTextColumn());
    parameters.put(param, tsEscape(term) + WILDCARD);

    String tokenized = tokenizeTsQuery(term);

    if (!allowTokenization || isNotTokenized(tokenized)) {
      return TSQUERY.formatted(placeholder(param));
    }

    String tokenizedParam = param(column.getFullTextColumn());
    parameters.put(tokenizedParam, tokenized + WILDCARD);

    return OR_TWO_TSQUERY.formatted(placeholder(param), placeholder(tokenizedParam));
  }

  public static String tsEscape(final String term) {
    return QUOTE + term.toLowerCase().replace("\\", "\\\\").replace(QUOTE, "\\'") + QUOTE;
  }

  @VisibleForTesting
  static String tokenizeTsQuery(final String term) {
    return Stream.of(TOKENIZER.split(term))
        .filter(Strings2::notBlank)
        .map(PostgresFulltextSearchConditionBuilder::tsEscape)
        .collect(Collectors.joining(FOLLOWED_BY));
  }

  @VisibleForTesting
  static boolean isNotTokenized(final String tokenizedTerm) {
    return !tokenizedTerm.contains(FOLLOWED_BY);
  }
}
