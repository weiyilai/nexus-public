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
package org.sonatype.nexus.repository.ossindex;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.goodies.packageurl.PackageUrl;
import org.sonatype.nexus.common.QualifierUtil;

import org.springframework.stereotype.Component;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Optional.empty;

/**
 * @since 3.26
 */
@Component
@Singleton
public class PackageUrlServiceImpl
    extends ComponentSupport
    implements PackageUrlService
{
  private final Map<String, PackageUrlMapping> packageUrlMappings;

  @Inject
  public PackageUrlServiceImpl(final List<PackageUrlMapping> packageUrlMappingsList) {
    this.packageUrlMappings = QualifierUtil.buildQualifierBeanMap(checkNotNull(packageUrlMappingsList));
  }

  @Override
  public Optional<PackageUrl> getPackageUrl(
      final String format,
      final String namespace,
      final String name,
      final String version)
  {
    PackageUrlMapping mapping = packageUrlMappings.get(format);
    if (mapping != null) {
      try {
        return mapping.buildPackageUrl(namespace, name, version);
      }
      catch (Exception e) {
        log.debug("Cannot determine package URL coordinates for {} namespace/name/version {}/{}/{}", format, namespace,
            name,
            version, e);
      }
    }
    return empty();
  }
}
