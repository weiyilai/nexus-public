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

import org.sonatype.nexus.repository.search.sql.query.h2.H2SearchConditionFactory;
import org.sonatype.nexus.repository.search.sql.query.postgres.PostgresSearchConditionFactory;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.sonatype.nexus.repository.search.sql.ExpressionGroup;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Factory that provides the appropriate {@link SearchConditionFactory} implementation based on the detected database
 * type.
 * This delegates to either H2SearchConditionFactory or PostgresSearchConditionFactory at runtime after database
 * configuration is loaded.
 */
@Component
@Primary
@Singleton
public class SearchConditionFactoryImpl
    implements SearchConditionFactory
{
  private final DatabaseTypeDetector databaseTypeDetector;

  private final H2SearchConditionFactory h2Factory;

  private final PostgresSearchConditionFactory postgresFactory;

  @Inject
  public SearchConditionFactoryImpl(
      final DatabaseTypeDetector databaseTypeDetector,
      final H2SearchConditionFactory h2Factory,
      final PostgresSearchConditionFactory postgresFactory)
  {
    this.databaseTypeDetector = checkNotNull(databaseTypeDetector);
    this.h2Factory = checkNotNull(h2Factory);
    this.postgresFactory = checkNotNull(postgresFactory);
  }

  /**
   * Returns the appropriate SearchConditionFactory implementation based on detected database type.
   */
  private SearchConditionFactory getDelegate() {
    return databaseTypeDetector.isH2() ? h2Factory : postgresFactory;
  }

  @Override
  public SqlSearchQueryConditionGroup build(final ExpressionGroup expressionGroup) {
    return getDelegate().build(expressionGroup);
  }
}
