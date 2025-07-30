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
package org.sonatype.nexus.bootstrap.metrics;

import java.lang.management.ManagementFactory;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.codahale.metrics.jvm.BufferPoolMetricSet;
import com.codahale.metrics.jvm.FileDescriptorRatioGauge;
import com.codahale.metrics.jvm.GarbageCollectorMetricSet;
import com.codahale.metrics.jvm.JvmAttributeGaugeSet;
import com.codahale.metrics.jvm.MemoryUsageGaugeSet;
import com.codahale.metrics.jvm.ThreadStatesGaugeSet;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import static com.codahale.metrics.MetricRegistry.name;

@Configuration
public class MetricsConfiguration
{
  @Primary
  @Qualifier("nexus")
  @Bean
  public MetricRegistry nexusMetricRegistry() {
    MetricRegistry registry = SharedMetricRegistries.getOrCreate("nexus");
    registry.register(name("jvm", "vm"), new JvmAttributeGaugeSet());
    registry.register(name("jvm", "memory"), new MemoryUsageGaugeSet());
    registry.register(name("jvm", "buffers"), new BufferPoolMetricSet(ManagementFactory.getPlatformMBeanServer()));
    registry.register(name("jvm", "fd_usage"), new FileDescriptorRatioGauge());
    registry.register(name("jvm", "thread-states"), new ThreadStatesGaugeSet());
    registry.register(name("jvm", "garbage-collectors"), new GarbageCollectorMetricSet());

    return registry;
  }

  @Bean
  public HealthCheckRegistry healthCheckRegistry() {
    return new HealthCheckRegistry();
  }
}
