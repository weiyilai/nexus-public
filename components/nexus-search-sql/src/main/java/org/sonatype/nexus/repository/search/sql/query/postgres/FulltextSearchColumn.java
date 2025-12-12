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

import java.util.Optional;

import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A column which supports full text searches
 */
class FulltextSearchColumn
    extends PostgresSearchColumn
{

  private final String fulltextColumn;

  /**
   * A column which does not support sorting
   *
   * @param fulltextColumn the column used for full text search
   */
  FulltextSearchColumn(final String fulltextColumn) {
    this(fulltextColumn, null);
  }

  /**
   * A column which uses the provided column for sorting
   *
   * @param fulltextColumn the column used for full text search
   * @param sortColumnName the column used for sorting
   */
  FulltextSearchColumn(final String fulltextColumn, final String sortColumnName) {
    this(fulltextColumn, null, sortColumnName);
  }

  /**
   * A column which supports fulltext searches and a specified sort column
   *
   * @param fulltextColumn the column used for full text search
   * @param exactMatchColumn an alternative column which can be used for exact matches
   * @param sortColumnName the column used for sorting
   */
  FulltextSearchColumn(
      final String fulltextColumn,
      @Nullable final String exactMatchColumn,
      @Nullable final String sortColumnName)
  {
    super(Optional.ofNullable(exactMatchColumn), Optional.ofNullable(sortColumnName));
    this.fulltextColumn = checkNotNull(fulltextColumn);
  }

  @Override
  String getFullTextColumn() {
    return fulltextColumn;
  }

  @Override
  boolean supportsFulltext() {
    return true;
  }

  static Builder builder() {
    return new Builder();
  }

  static class Builder
  {
    private String columnName;

    private String sortColumnName;

    private String exactMatchColumn;

    Builder withColumn(final String name) {
      this.columnName = name;
      return this;
    }

    Builder withSortColumn(final String sortColumnName) {
      this.sortColumnName = sortColumnName;
      return this;
    }

    Builder withExactMatchColumn(final String exactMatchColumn) {
      this.exactMatchColumn = exactMatchColumn;
      return this;
    }

    FulltextSearchColumn build() {
      return new FulltextSearchColumn(checkNotNull(columnName), exactMatchColumn, sortColumnName);
    }
  }
}
