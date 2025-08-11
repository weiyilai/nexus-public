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
package org.sonatype.nexus.repository.content.upgrades;

import java.sql.Connection;
import java.util.Optional;

import org.sonatype.nexus.upgrade.datastore.DatabaseMigrationStep;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import static org.springframework.beans.factory.config.BeanDefinition.SCOPE_SINGLETON;

@Component
@Scope(SCOPE_SINGLETON)
public class ConanCleanupMigrationStep_2_18
    implements DatabaseMigrationStep
{
  private static final Logger LOG = LoggerFactory.getLogger(ConanCleanupMigrationStep_2_18.class);

  private static final String DROP_INDEX = "DROP INDEX IF EXISTS idx_conan_component_coordinate_revisions;";

  private static final String DROP_CONSTRAINT =
      "ALTER TABLE IF EXISTS conan_component DROP CONSTRAINT IF EXISTS uk_conan_component_coordinate_revisions;";

  private static final String DROP_COLUMN_REVISION =
      "ALTER TABLE IF EXISTS conan_component DROP COLUMN IF EXISTS revision;";

  private static final String DROP_COLUMN_REVISION_TIME =
      "ALTER TABLE IF EXISTS conan_component DROP COLUMN IF EXISTS revision_time;";

  private static final String ADD_CONSTRAINT =
      "ALTER TABLE IF EXISTS conan_component " +
          "ADD CONSTRAINT IF NOT EXISTS uk_conan_component_coordinate UNIQUE (repository_id, namespace, name, version)";

  @Override
  public Optional<String> version() {
    return Optional.of("2.18");
  }

  /**
   * We had an index, as well as 2 columns (revision and revision_time) in the conan_component table, and a constraint
   * that utilized one of those columns. These were all from an early iteration of conan revision support, and are no
   * longer needed. This migration step removes the index, the columns, and the constraint, replacing it with a new
   * constraint that does not use the revision column.
   */
  @Override
  public void migrate(final Connection connection) throws Exception {
    LOG.info("Removing outdated indexes and columns from conan_component.");

    this.runStatement(connection, DROP_INDEX);
    this.runStatement(connection, DROP_CONSTRAINT);
    this.runStatement(connection, DROP_COLUMN_REVISION);
    this.runStatement(connection, DROP_COLUMN_REVISION_TIME);
    this.runStatement(connection, ADD_CONSTRAINT);

    LOG.info("Successfully removed outdated indexes and columns from conan_component.");
  }
}
