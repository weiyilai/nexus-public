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
package org.sonatype.nexus.repository.search.sql.query.security;

/**
 * H2-specific implementation of CselToExpression.
 *
 * H2 stores paths with quotes but no braces:
 * Example: '/org/apache/commons/commons-compress/1.26.1/commons-compress-1.26.1.pom'
 * '/org/apache/commons/commons-compress/1.26.1/commons-compress-1.26.1.jar'
 */
public class H2CselToExpression
    extends CselToExpression
{
  private static final String H2_TOKEN_START_REGEX = "(^|)";

  private static final String H2_TOKEN_END_REGEX = "(|$)";

  @Override
  protected String getTokenStartRegex() {
    return H2_TOKEN_START_REGEX;
  }

  @Override
  protected String getTokenEndRegex() {
    return H2_TOKEN_END_REGEX;
  }
}
