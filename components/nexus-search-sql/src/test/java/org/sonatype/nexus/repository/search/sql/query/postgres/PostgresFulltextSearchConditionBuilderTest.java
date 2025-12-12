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

import org.sonatype.goodies.testsupport.Test5Support;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.sonatype.nexus.repository.search.sql.query.postgres.PostgresFulltextSearchConditionBuilder.isNotTokenized;
import static org.sonatype.nexus.repository.search.sql.query.postgres.PostgresFulltextSearchConditionBuilder.tokenizeTsQuery;
import static org.sonatype.nexus.repository.search.sql.query.postgres.PostgresFulltextSearchConditionBuilder.tsEscape;

class PostgresFulltextSearchConditionBuilderTest
    extends Test5Support
{
  @Test
  void testTokenizeTsQuery() {
    assertThat(tokenizeTsQuery("org.apache.tomcat"), is("'org' <-> 'apache' <-> 'tomcat'"));
    assertThat(tokenizeTsQuery("ORG.APACHE.TOMCAT"), is("'org' <-> 'apache' <-> 'tomcat'"));
    assertThat(tokenizeTsQuery("tomcat"), is("'tomcat'"));
    assertThat(tokenizeTsQuery("TOMCAT"), is("'tomcat'"));
  }

  @Test
  void testNotTokenized() {
    assertThat(isNotTokenized(tokenizeTsQuery("tomcat")), is(true));
    assertThat(isNotTokenized(tokenizeTsQuery("TOMCAT")), is(true));
    assertThat(isNotTokenized(tokenizeTsQuery("ORG.APACHE.TOMCAT")), is(false));
    assertThat(isNotTokenized(tokenizeTsQuery("org.apache.tomcat")), is(false));
  }

  @Test
  void testTsEscape() {
    assertThat(tsEscape("asdf"), is("'asdf'"));
    assertThat(tsEscape("as'df"), is("'as\\'df'"));
    assertThat(tsEscape("AS'DF"), is("'as\\'df'"));
    assertThat(tsEscape("as\\df"), is("'as\\\\df'"));
  }
}
