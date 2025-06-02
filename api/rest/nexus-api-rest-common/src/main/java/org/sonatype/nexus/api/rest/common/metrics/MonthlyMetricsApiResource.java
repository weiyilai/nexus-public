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

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.sonatype.analytics.internal.metrics.AggregatedMetric;
import com.sonatype.analytics.internal.metrics.AggregatedMetricsStore;
import org.sonatype.nexus.rest.Resource;

import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;

import static com.sonatype.analytics.internal.AnalyticsConstants.COMPONENT_TOTAL_COUNT_KEY;
import static com.sonatype.analytics.internal.AnalyticsConstants.CONTENT_REQUEST_COUNT;
import static com.sonatype.analytics.internal.AnalyticsConstants.MAX_MONTH;

@Path("/monthly-metrics")
public class MonthlyMetricsApiResource
    implements Resource, MonthlyMetricsApiResourceDoc
{
  public static final String METRIC_DATE = "metricDate";

  private AggregatedMetricsStore aggregatedMetricsStore;

  @Inject
  public MonthlyMetricsApiResource(AggregatedMetricsStore aggregatedMetricsStore) {
    this.aggregatedMetricsStore = aggregatedMetricsStore;
  }

  @Override
  @RequiresPermissions("nexus:*")
  @RequiresAuthentication
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Response getLast12MonthsMetrics() {
    List<AggregatedMetric> requestMetrics =
        aggregatedMetricsStore.getLastNRecords(CONTENT_REQUEST_COUNT, MAX_MONTH, 12);
    List<AggregatedMetric> componentMetrics =
        aggregatedMetricsStore.getLastNRecords(COMPONENT_TOTAL_COUNT_KEY, MAX_MONTH, 12);
    List<AggregatedMetric> peakStorageMetrics =
        aggregatedMetricsStore.getLastNRecords("peak_storage", MAX_MONTH, 13);
    List<AggregatedMetric> responseSizeMetrics =
        aggregatedMetricsStore.getLastNRecords("response_size_data", MAX_MONTH, 13);

    Map<YearMonth, CombinedMetric> aggregatedMetrics =
        aggregateMetrics(componentMetrics, requestMetrics, peakStorageMetrics, responseSizeMetrics);

    // Sort the YearMonth keys in ascending order
    Map<YearMonth, CombinedMetric> sortedAggregatedMetrics = new TreeMap<>(aggregatedMetrics);

    List<Map<String, Object>> response = new ArrayList<>();

    YearMonth currentMonth = YearMonth.now();
    YearMonth startMonth = currentMonth.minusMonths(12); // Start from 12 months ago to include the current month

    CombinedMetric previousMetric = null;

    for (YearMonth month = startMonth; !month.isAfter(currentMonth); month = month.plusMonths(1)) {
      CombinedMetric currentMetric = sortedAggregatedMetrics.get(month);

      if (currentMetric != null) {
        response.add(createMetricMap(currentMetric, previousMetric));
        previousMetric = currentMetric;
      }
    }

    // Sort the response in descending order by metricDate
    response.sort((a, b) -> ((Comparable) b.get(METRIC_DATE)).compareTo(a.get(METRIC_DATE)));

    return Response.ok(response).build();
  }

  private Map<YearMonth, CombinedMetric> aggregateMetrics(
      List<AggregatedMetric> componentMetrics,
      List<AggregatedMetric> requestMetrics,
      List<AggregatedMetric> peakStorageMetrics,
      List<AggregatedMetric> responseSizeMetrics)
  {
    Map<YearMonth, CombinedMetric> aggregatedMetrics = new HashMap<>();

    for (AggregatedMetric metric : componentMetrics) {
      YearMonth month = YearMonth.from(metric.getMetricDate());
      aggregatedMetrics.put(month, new CombinedMetric(
          metric.getMetricValue(), 0, 0, 0, metric.getMetricDate()));
    }

    for (AggregatedMetric metric : requestMetrics) {
      YearMonth month = YearMonth.from(metric.getMetricDate());
      CombinedMetric combinedMetric =
          aggregatedMetrics.getOrDefault(month, new CombinedMetric(0, 0, 0, 0, metric.getMetricDate()));
      aggregatedMetrics.put(month, new CombinedMetric(
          combinedMetric.componentMetricValue(),
          metric.getMetricValue(),
          combinedMetric.peakStorageValue(),
          combinedMetric.responseSizeValue(),
          metric.getMetricDate()));
    }

    for (AggregatedMetric metric : peakStorageMetrics) {
      YearMonth month = YearMonth.from(metric.getMetricDate());
      CombinedMetric combinedMetric =
          aggregatedMetrics.getOrDefault(month, new CombinedMetric(0, 0, 0, 0, metric.getMetricDate()));
      aggregatedMetrics.put(month, new CombinedMetric(
          combinedMetric.componentMetricValue(),
          combinedMetric.requestMetricValue(),
          metric.getMetricValue(),
          combinedMetric.responseSizeValue(),
          metric.getMetricDate()));
    }

    for (AggregatedMetric metric : responseSizeMetrics) {
      YearMonth month = YearMonth.from(metric.getMetricDate());
      CombinedMetric combinedMetric =
          aggregatedMetrics.getOrDefault(month, new CombinedMetric(0, 0, 0, 0, metric.getMetricDate()));
      aggregatedMetrics.put(month, new CombinedMetric(
          combinedMetric.componentMetricValue(),
          combinedMetric.requestMetricValue(),
          combinedMetric.peakStorageValue(),
          metric.getMetricValue(),
          metric.getMetricDate()));
    }

    return aggregatedMetrics;
  }

  /**
   * Calculate percentage change between current and previous values
   * 
   * @param currentValue Current metric value
   * @param previousValue Previous metric value
   * @return Calculated percentage change or "N/A" if calculation not possible
   */
  private Object calculatePercentageChange(long currentValue, long previousValue) {
    if (currentValue == 0 || previousValue == 0) {
      return "N/A";
    }

    double percentageChange = ((double) (currentValue - previousValue) / previousValue) * 100;
    return Math.round(percentageChange);
  }

  /**
   * Create a metric map from a CombinedMetric with percentage changes
   */
  private Map<String, Object> createMetricMap(
      CombinedMetric currentMetric,
      CombinedMetric previousMetric)
  {
    Map<String, Object> metricMap = new HashMap<>();
    metricMap.put("requestCount", currentMetric.requestMetricValue());
    metricMap.put("componentCount", currentMetric.componentMetricValue());
    metricMap.put("peakStorage", currentMetric.peakStorageValue());
    metricMap.put("responseSize", currentMetric.responseSizeValue());
    metricMap.put(METRIC_DATE, currentMetric.metricDate());

    if (previousMetric == null) {
      metricMap.put("percentageChangeRequest", "N/A");
      metricMap.put("percentageChangeComponent", "N/A");
    }
    else {
      metricMap.put("percentageChangeRequest",
          calculatePercentageChange(currentMetric.requestMetricValue(), previousMetric.requestMetricValue()));
      metricMap.put("percentageChangeComponent",
          calculatePercentageChange(currentMetric.componentMetricValue(), previousMetric.componentMetricValue()));
    }

    return metricMap;
  }
}
