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

import org.sonatype.nexus.repository.search.sql.ExpressionGroup;
import org.sonatype.nexus.repository.search.sql.query.SearchConditionFactory;
import org.sonatype.nexus.repository.search.sql.query.SqlSearchQueryCondition;
import org.sonatype.nexus.repository.search.sql.query.SqlSearchQueryConditionGroup;
import org.sonatype.nexus.repository.search.sql.query.postgres.PostgresSearchConditionBuilder.ConditionType;
import org.sonatype.nexus.repository.search.sql.query.syntax.Expression;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * {@link SearchConditionFactory} which produces queries for use with PostgreSQL.
 * Instantiated by SearchConditionFactoryImpl when PostgreSQL is detected at runtime.
 */
@Qualifier("postgres")
@Component
@Singleton
public class PostgresSearchConditionFactory
    implements SearchConditionFactory
{
  private final PostgresSearchDB db;

  @Inject
  public PostgresSearchConditionFactory(final PostgresSearchDB db) {
    this.db = checkNotNull(db);
  }

  @Override
  public SqlSearchQueryConditionGroup build(final ExpressionGroup expressionGroup) {
    final Expression componentFilters = expressionGroup.getComponentFilters();
    SqlSearchQueryCondition componentQueryCondition = null;
    if (componentFilters != null) {
      final PostgresFulltextSearchConditionBuilder componentFilterBuilder =
          new PostgresFulltextSearchConditionBuilder(db);
      componentQueryCondition = componentFilterBuilder.build(componentFilters);
    }

    final Expression assetFilters = expressionGroup.getAssetFilters();
    SqlSearchQueryCondition assetQueryCondition = null;
    if (assetFilters != null) {
      final PostgresFulltextSearchConditionBuilder assetFilterBuilder =
          new PostgresFulltextSearchConditionBuilder(db, ConditionType.ASSET_FILTER);
      assetQueryCondition = assetFilterBuilder.build(assetFilters);
    }

    return new SqlSearchQueryConditionGroup(componentQueryCondition, assetQueryCondition);
  }
}
