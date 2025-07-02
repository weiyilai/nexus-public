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
package org.sonatype.nexus.rapture.internal;

import org.sonatype.nexus.common.app.FeatureFlag;
import org.sonatype.nexus.rapture.internal.security.JwtAuthenticationFilter;

import com.google.inject.AbstractModule;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import static org.sonatype.nexus.common.app.FeatureFlags.FEATURE_SPRING_ONLY;
import static org.sonatype.nexus.common.app.FeatureFlags.JWT_ENABLED;
import static org.sonatype.nexus.security.FilterProviderSupport.filterKey;

/**
 * Rapture Guice module for JWT.
 *
 * @since 3.38
 */
@Deprecated
@ConditionalOnProperty(value = FEATURE_SPRING_ONLY, havingValue = "false", matchIfMissing = true)
@Component
@FeatureFlag(name = JWT_ENABLED)
public class RaptureJwtModule
    extends AbstractModule
{
  @Override
  protected void configure() {
    bind(filterKey(JwtAuthenticationFilter.NAME)).to(JwtAuthenticationFilter.class);
  }
}
