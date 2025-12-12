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

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

import org.sonatype.nexus.repository.rest.sql.SearchField;
import org.sonatype.nexus.repository.search.sql.query.SearchDatabase;

import jakarta.inject.Singleton;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import static org.sonatype.nexus.repository.rest.sql.SearchField.*;

/**
 * A PostgreSQL implementation of {@link SearchDatabase}
 *
 * Instantiated by SearchDatabaseFactory when PostgreSQL is detected at runtime.
 */
@Qualifier("postgres")
@Component
@Singleton
public class PostgresSearchDB
    implements SearchDatabase
{
  private final Map<SearchField, PostgresSearchColumn> columns;

  public PostgresSearchDB() {
    this.columns = Collections.unmodifiableMap(columns());
  }

  public Optional<PostgresSearchColumn> getColumn(final SearchField field) {
    return Optional.ofNullable(columns.get(field));
  }

  @Override
  public Optional<String> getSortColumn(final SearchField field) {
    return Optional.ofNullable(field)
        .map(columns::get)
        .flatMap(PostgresSearchColumn::getSortColumnName);
  }

  private static Map<SearchField, PostgresSearchColumn> columns() {
    EnumMap<SearchField, PostgresSearchColumn> columns = new EnumMap<>(SearchField.class);

    columns.put(FORMAT, FulltextSearchColumn.builder()
        .withColumn(addComponentPrefix("tsvector_format"))
        .withExactMatchColumn(addComponentPrefix("format"))
        .withSortColumn(addComponentPrefix("format"))
        .build());
    columns.put(REPOSITORY_NAME, new PostgresSearchColumn(addComponentPrefix("search_repository_name")));

    columns.put(COMPONENT_ID, new PostgresSearchColumn(addComponentPrefix("component_id")));
    columns.put(COMPONENT_KIND, new PostgresSearchColumn(addComponentPrefix("component_kind")));
    columns.put(LAST_MODIFIED, new PostgresSearchColumn(addComponentPrefix("last_modified")));
    columns.put(NAMESPACE, FulltextSearchColumn.builder()
        .withColumn(addComponentPrefix("tsvector_namespace"))
        .withExactMatchColumn(addComponentPrefix("namespace"))
        .withSortColumn(addComponentPrefix("namespace"))
        .build());
    columns.put(NAME, FulltextSearchColumn.builder()
        .withColumn(addComponentPrefix("tsvector_search_component_name"))
        .withExactMatchColumn(addComponentPrefix("search_component_name"))
        .withSortColumn(addComponentPrefix("tsvector_search_component_name"))
        .build());
    columns.put(VERSION, FulltextSearchColumn.builder()
        .withColumn(addComponentPrefix("tsvector_version"))
        .withExactMatchColumn(addComponentPrefix("version"))
        .withSortColumn(addComponentPrefix("normalised_version"))
        .build());
    columns.put(PRERELEASE, new PostgresSearchColumn(addComponentPrefix("prerelease")));
    columns.put(KEYWORDS,
        new FulltextSearchColumn(addComponentPrefix("keywords"), addComponentPrefix("search_component_name"), null));
    columns.put(FORMAT_FIELD_1, FulltextSearchColumn.builder()
        .withColumn(addComponentPrefix("format_field_values_1"))
        .withSortColumn(ATTRIBUTES)
        .build());
    columns.put(FORMAT_FIELD_2, FulltextSearchColumn.builder()
        .withColumn(addComponentPrefix("format_field_values_2"))
        .withSortColumn(ATTRIBUTES)
        .build());
    columns.put(FORMAT_FIELD_3, FulltextSearchColumn.builder()
        .withColumn(addComponentPrefix("format_field_values_3"))
        .withSortColumn(ATTRIBUTES)
        .build());
    columns.put(FORMAT_FIELD_4, FulltextSearchColumn.builder()
        .withColumn(addComponentPrefix("format_field_values_4"))
        .withSortColumn(ATTRIBUTES)
        .build());
    columns.put(FORMAT_FIELD_5, FulltextSearchColumn.builder()
        .withColumn(addComponentPrefix("format_field_values_5"))
        .withSortColumn(ATTRIBUTES)
        .build());
    columns.put(FORMAT_FIELD_6, FulltextSearchColumn.builder()
        .withColumn(addComponentPrefix("format_field_values_6"))
        .withSortColumn(ATTRIBUTES)
        .build());
    columns.put(FORMAT_FIELD_7, FulltextSearchColumn.builder()
        .withColumn(addComponentPrefix("format_field_values_7"))
        .withSortColumn(ATTRIBUTES)
        .build());
    columns.put(MD5, new FulltextSearchColumn(addComponentPrefix("md5")));
    columns.put(SHA1, new FulltextSearchColumn(addComponentPrefix("sha1")));
    columns.put(SHA256, new FulltextSearchColumn(addComponentPrefix("sha256")));
    columns.put(SHA512, new FulltextSearchColumn(addComponentPrefix("sha512")));

    columns.put(TAGS, new FulltextSearchColumn(addComponentPrefix("tsvector_tags")));
    columns.put(PATHS, new FulltextSearchColumn(addComponentPrefix("paths")));
    columns.put(UPLOADERS, new FulltextSearchColumn(addComponentPrefix("uploaders")));
    columns.put(UPLOADER_IPS, new FulltextSearchColumn(addComponentPrefix("uploader_ips")));

    columns.put(ASSET_FORMAT_VALUE_1, new PostgresSearchColumn(addAssetPrefix("asset_format_value_1")));
    columns.put(ASSET_FORMAT_VALUE_2, new PostgresSearchColumn(addAssetPrefix("asset_format_value_2")));
    columns.put(ASSET_FORMAT_VALUE_3, new PostgresSearchColumn(addAssetPrefix("asset_format_value_3")));
    columns.put(ASSET_FORMAT_VALUE_4, new PostgresSearchColumn(addAssetPrefix("asset_format_value_4")));
    columns.put(ASSET_FORMAT_VALUE_5, new PostgresSearchColumn(addAssetPrefix("asset_format_value_5")));
    columns.put(ASSET_FORMAT_VALUE_6, new PostgresSearchColumn(addAssetPrefix("asset_format_value_6")));
    columns.put(ASSET_FORMAT_VALUE_7, new PostgresSearchColumn(addAssetPrefix("asset_format_value_7")));
    columns.put(ASSET_FORMAT_VALUE_8, new PostgresSearchColumn(addAssetPrefix("asset_format_value_8")));
    columns.put(ASSET_FORMAT_VALUE_9, new PostgresSearchColumn(addAssetPrefix("asset_format_value_9")));
    columns.put(ASSET_FORMAT_VALUE_10, new PostgresSearchColumn(addAssetPrefix("asset_format_value_10")));
    columns.put(ASSET_FORMAT_VALUE_11, new PostgresSearchColumn(addAssetPrefix("asset_format_value_11")));
    columns.put(ASSET_FORMAT_VALUE_12, new PostgresSearchColumn(addAssetPrefix("asset_format_value_12")));
    columns.put(ASSET_FORMAT_VALUE_13, new PostgresSearchColumn(addAssetPrefix("asset_format_value_13")));
    columns.put(ASSET_FORMAT_VALUE_14, new PostgresSearchColumn(addAssetPrefix("asset_format_value_14")));
    columns.put(ASSET_FORMAT_VALUE_15, new PostgresSearchColumn(addAssetPrefix("asset_format_value_15")));
    columns.put(ASSET_FORMAT_VALUE_16, new PostgresSearchColumn(addAssetPrefix("asset_format_value_16")));
    columns.put(ASSET_FORMAT_VALUE_17, new PostgresSearchColumn(addAssetPrefix("asset_format_value_17")));
    columns.put(ASSET_FORMAT_VALUE_18, new PostgresSearchColumn(addAssetPrefix("asset_format_value_18")));
    columns.put(ASSET_FORMAT_VALUE_19, new PostgresSearchColumn(addAssetPrefix("asset_format_value_19")));
    columns.put(ASSET_FORMAT_VALUE_20, new PostgresSearchColumn(addAssetPrefix("asset_format_value_20")));

    return columns;
  }

  /**
   * Add the component prefix to the field to avoid ambiguity in the searchComponents query.
   *
   * @see com.sonatype.nexus.distributed.internal.search.sql.store.SearchTableDAO#searchComponents
   */
  private static String addComponentPrefix(final String field) {
    return COMPONENT_PREFIX_ID + field;
  }

  /**
   * Add the asset prefix to the field to avoid ambiguity in the searchComponents query.
   *
   * @see com.sonatype.nexus.distributed.internal.search.sql.store.SearchTableDAO#searchComponents
   */
  private static String addAssetPrefix(final String field) {
    return ASSET_PREFIX_ID + field;
  }
}
