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
package org.sonatype.nexus.internal.metrics;

import jakarta.inject.Inject;

import org.sonatype.nexus.common.MediatorSupport;
import org.sonatype.nexus.common.QualifierUtil;

import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricRegistry;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Manages {@link Metric} registrations
 *
 * @since 3.6
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
public class MetricMediator
    extends MediatorSupport<Metric>
{
  private final MetricRegistry registry;

  @Inject
  public MetricMediator(final MetricRegistry registry) {
    super(Metric.class);
    this.registry = checkNotNull(registry);
  }

  @Override
  protected void add(final Metric metric) {
    if (metric == registry) {
      // Avoid adding registry to itself
      return;
    }
    log.debug("Registering: {}", metric);
    registry.register(QualifierUtil.value(metric).orElseGet(metric.getClass()::getName), metric);
  }
}
