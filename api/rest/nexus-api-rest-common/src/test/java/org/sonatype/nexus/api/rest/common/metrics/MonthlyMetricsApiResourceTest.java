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

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.ws.rs.core.Response;

import com.sonatype.analytics.internal.metrics.AggregatedMetric;
import com.sonatype.analytics.internal.metrics.AggregatedMetricsStore;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

public class MonthlyMetricsApiResourceTest
{

  private static final String CONTENT_REQUEST_COUNT = "content_request_count";

  @Mock
  private AggregatedMetricsStore aggregatedMetricsStore;

  private MonthlyMetricsApiResource monthlyMetricsApiResource;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    monthlyMetricsApiResource = new MonthlyMetricsApiResource(aggregatedMetricsStore);
  }

  @Test
  public void testGetLast12MonthsMetrics() {
    // Create test metrics using Mockito
    List<AggregatedMetric> requestMetrics = Arrays.asList(
        createMockMetric("2025-03-01T00:00:00Z", 0),
        createMockMetric("2025-02-28T14:00:00Z", 0),
        createMockMetric("2025-01-01T16:00:00Z", 123890),
        createMockMetric("2024-12-01T17:00:00Z", 234567),
        createMockMetric("2024-11-01T17:00:00Z", 123456));

    List<AggregatedMetric> componentMetrics = Arrays.asList(
        createMockMetric("2025-03-01T00:00:00Z", 0),
        createMockMetric("2025-02-28T14:00:00Z", 345234),
        createMockMetric("2025-01-01T16:00:00Z", 435623),
        createMockMetric("2024-12-01T17:00:00Z", 432542),
        createMockMetric("2024-11-01T17:00:00Z", 0));

    List<AggregatedMetric> peakStorageMetrics = Arrays.asList(
        createMockMetric("2025-03-01T00:00:00Z", 1073741824), // 1 GB
        createMockMetric("2025-02-28T14:00:00Z", 1288490188), // ~1.2 GB
        createMockMetric("2025-01-01T16:00:00Z", 1610612736), // 1.5 GB
        createMockMetric("2024-12-01T17:00:00Z", 2147483648L), // 2 GB
        createMockMetric("2024-11-01T17:00:00Z", 3221225472L)); // 3 GB

    List<AggregatedMetric> responseSizeMetrics = Arrays.asList(
        createMockMetric("2025-03-01T00:00:00Z", 536870912), // 0.5 GB
        createMockMetric("2025-02-28T14:00:00Z", 644245094), // ~0.6 GB
        createMockMetric("2025-01-01T16:00:00Z", 805306368), // 0.75 GB
        createMockMetric("2024-12-01T17:00:00Z", 1073741824L), // 1 GB
        createMockMetric("2024-11-01T17:00:00Z", 1610612736L)); // 1.5 GB

    // Mock the store behavior
    when(aggregatedMetricsStore.getLastNRecords(CONTENT_REQUEST_COUNT, "month", 12)).thenReturn(requestMetrics);
    when(aggregatedMetricsStore.getLastNRecords("component_total_count", "month", 12)).thenReturn(componentMetrics);
    when(aggregatedMetricsStore.getLastNRecords("peak_storage", "month", 13)).thenReturn(peakStorageMetrics);
    when(aggregatedMetricsStore.getLastNRecords("response_size_data", "month", 13)).thenReturn(responseSizeMetrics);

    // Execute the method under test
    Response response = monthlyMetricsApiResource.getLast12MonthsMetrics();
    List<Map<String, Object>> responseBody = (List<Map<String, Object>>) response.getEntity();

    // Verify the results
    assertThat(responseBody.size(), is(5));

    assertThat(OffsetDateTime.parse("2025-03-01T00:00:00Z"),
        is(OffsetDateTime.parse(responseBody.get(0).get("metricDate").toString())));
    assertThat(responseBody.get(0).get("percentageChangeRequest"), is("N/A"));
    assertThat(responseBody.get(0).get("percentageChangeComponent"), is("N/A"));
    assertThat(responseBody.get(0).get("peakStorage"), is(1073741824L));
    assertThat(responseBody.get(0).get("responseSize"), is(536870912L));

    assertThat(OffsetDateTime.parse("2025-02-28T14:00:00Z"),
        is(OffsetDateTime.parse(responseBody.get(1).get("metricDate").toString())));
    assertThat(responseBody.get(1).get("percentageChangeRequest"), is("N/A"));
    assertThat(responseBody.get(1).get("percentageChangeComponent"), is(-21L));
    assertThat(responseBody.get(1).get("peakStorage"), is(1288490188L));
    assertThat(responseBody.get(1).get("responseSize"), is(644245094L));

    assertThat(OffsetDateTime.parse("2025-01-01T16:00:00Z"),
        is(OffsetDateTime.parse(responseBody.get(2).get("metricDate").toString())));
    assertThat(responseBody.get(2).get("percentageChangeRequest"), is(-47L));
    assertThat(responseBody.get(2).get("percentageChangeComponent"), is(1L));
    assertThat(responseBody.get(2).get("peakStorage"), is(1610612736L));
    assertThat(responseBody.get(2).get("responseSize"), is(805306368L));

    assertThat(OffsetDateTime.parse("2024-12-01T17:00:00Z"),
        is(OffsetDateTime.parse(responseBody.get(3).get("metricDate").toString())));
    assertThat(responseBody.get(3).get("percentageChangeRequest"), is(90L));
    assertThat(responseBody.get(3).get("percentageChangeComponent"), is("N/A"));
    assertThat(responseBody.get(3).get("peakStorage"), is(2147483648L));
    assertThat(responseBody.get(3).get("responseSize"), is(1073741824L));

    assertThat(OffsetDateTime.parse("2024-11-01T17:00:00Z"),
        is(OffsetDateTime.parse(responseBody.get(4).get("metricDate").toString())));
    assertThat(responseBody.get(4).get("percentageChangeRequest"), is("N/A"));
    assertThat(responseBody.get(4).get("percentageChangeComponent"), is("N/A"));
    assertThat(responseBody.get(4).get("peakStorage"), is(3221225472L));
    assertThat(responseBody.get(4).get("responseSize"), is(1610612736L));
  }

  private AggregatedMetric createMockMetric(String dateTime, long value) {
    AggregatedMetric metric = Mockito.mock(AggregatedMetric.class);
    when(metric.getMetricName()).thenReturn("test_metric");
    when(metric.getMetricValue()).thenReturn(value);
    when(metric.getMetricDate()).thenReturn(OffsetDateTime.parse(dateTime));
    when(metric.getPeriodType()).thenReturn("month");
    return metric;
  }
}
