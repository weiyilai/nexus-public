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
package org.sonatype.nexus.extdirect.internal;

import com.google.inject.servlet.ServletModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Servlet module for Ext.Direct Guice module.
 *
 * @since 3.38
 */
@Deprecated(since = "4/1/2025", forRemoval = true)
public abstract class ExtDirectServletModule
    extends ServletModule
{
  private static final Logger log = LoggerFactory.getLogger(ExtDirectServletModule.class);

  private final String mountPoint;

  protected ExtDirectServletModule(final String mountPoint) {
    this.mountPoint = mountPoint;
  }

  @Override
  protected void configureServlets() {
    bindSecurityFilter();
  }

  protected abstract void bindSecurityFilter();
}
