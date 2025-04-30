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

import com.codahale.metrics.Clock;
import com.codahale.metrics.servlet.InstrumentedFilter;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.google.inject.servlet.ServletModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This AbstractModule is PURPOSEFULLY not marked as @Named, as it is already manually added to injector via
 * WebModule.java
 *
 * <a href="http://metrics.dropwizard.io">Dropwizard Metrics</a> guice configuration.
 *
 * Installs servlet endpoints:
 *
 * <ul>
 * <li>/service/metrics/ping</li>
 * <li>/service/metrics/threads</li>
 * <li>/service/metrics/data</li>
 * <li>/service/metrics/healthcheck</li>
 * <li>/service/metrics/prometheus</li>
 * </ul>
 *
 * Protected by {@code nexus:metrics:read} permission.
 *
 * @since 2.5
 */
public class MetricsModule
    extends AbstractModule
{
  private static final Logger log = LoggerFactory.getLogger(MetricsModule.class);

  @Override
  protected void configure() {
    // NOTE: AdminServletModule (metrics-guice integration) generates invalid links, so wire up servlets ourselves

    final Clock clock = Clock.defaultClock();
    bind(Clock.class).toInstance(clock);

    final JsonFactory jsonFactory = new JsonFactory(new ObjectMapper());
    bind(JsonFactory.class).toInstance(jsonFactory);

    install(new ServletModule()
    {
      @Override
      protected void configureServlets() {
        bind(MetricsForwardingServlet.class).in(Scopes.SINGLETON);

        serve(MetricsForwardingServlet.PATH).with(MetricsForwardingServlet.class);

        // record metrics for all webapp access
        filter("/*").through(new InstrumentedFilter());
      }
    });

    log.info("Metrics support configured");
  }
}
