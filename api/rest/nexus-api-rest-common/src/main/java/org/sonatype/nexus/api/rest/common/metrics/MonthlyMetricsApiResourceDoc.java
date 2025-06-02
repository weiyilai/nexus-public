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
package org.sonatype.nexus.api.rest.common.metrics;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Example;
import io.swagger.annotations.ExampleProperty;

/**
 * Swagger documentation for {@link MonthlyMetricsApiResource}.
 */
@Api(value = "Monthly Metrics")
public interface MonthlyMetricsApiResourceDoc
{
  @ApiOperation(value = "Get the last 12 months of metrics.")
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Successful response", examples = @Example(value = {
          @ExampleProperty(mediaType = MediaType.APPLICATION_JSON,
              value = "[{\"requestCount\": 0, \"componentCount\": 0, \"metricDate\": \"2025-03-01T00:00:00Z\", \"percentageChangeRequest\": \"N/A\", \"percentageChangeComponent\": \"N/A\"}]")
      }))
  })
  Response getLast12MonthsMetrics();
}
