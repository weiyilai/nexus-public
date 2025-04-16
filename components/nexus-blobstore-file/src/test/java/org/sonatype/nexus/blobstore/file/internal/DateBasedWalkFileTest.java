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
package org.sonatype.nexus.blobstore.file.internal;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.blobstore.api.BlobRef;
import org.sonatype.nexus.common.time.UTC;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.sonatype.nexus.blobstore.BlobStoreSupport.CONTENT_PREFIX;

/**
 * Unit Tests for {@link DateBasedWalkFile}.
 */
public class DateBasedWalkFileTest
    extends TestSupport
{
  private static final String BYTES_FILE_NOW = "now";

  private static final String BYTES_FILE_1_MIN_OLD = "1minOld";

  private static final String BYTES_FILE_3_MIN_OLD = "3minOld";

  private static final String BYTES_FILE_10_MIN_OLD = "10minOld";

  private static final String BYTES_FILE_1_H_OLD = "1hOld";

  private static final String BYTES_FILE_3_H_OLD = "3hOld";

  private static final String BYTES_FILE_10_H_OLD = "10hOld";

  private static final String BYTES_FILE_1_D_OLD = "1dOld";

  private static final String BYTES_FILE_3_D_OLD = "3dOld";

  private static final String BYTES_FILE_10_D_OLD = "10dOld";

  private static final String BYTES_FILE_1_M_OLD = "1mOld";

  private static final String BYTES_FILE_3_M_OLD = "3mOld";

  private static final String BYTES_FILE_10_M_OLD = "10mOld";

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  public OffsetDateTime blobCreated;

  public File contentDir;

  @Before
  public void setup() throws IOException {
    blobCreated = UTC.now();
    contentDir = new File(temporaryFolder.getRoot().getPath(), CONTENT_PREFIX);

    // create 4 files: now, 1 min ago, 3 min ago, and 10 min ago
    File storageDirNow = new File(contentDir, getDatePath(blobCreated));
    assertThat(storageDirNow.mkdirs(), is(Boolean.TRUE));
    assertThat(new File(storageDirNow, BYTES_FILE_NOW + ".bytes").createNewFile(), is(Boolean.TRUE));

    File storageDir1MinOld = new File(contentDir, getDatePath(blobCreated.minusMinutes(1)));
    assertThat(storageDir1MinOld.mkdirs(), is(Boolean.TRUE));
    assertThat(new File(storageDir1MinOld, BYTES_FILE_1_MIN_OLD + ".bytes").createNewFile(), is(Boolean.TRUE));

    File storageDir3MinOld = new File(contentDir, getDatePath(blobCreated.minusMinutes(3)));
    assertThat(storageDir3MinOld.mkdirs(), is(Boolean.TRUE));
    assertThat(new File(storageDir3MinOld, BYTES_FILE_3_MIN_OLD + ".bytes").createNewFile(), is(Boolean.TRUE));

    File storageDir10MinOld = new File(contentDir, getDatePath(blobCreated.minusMinutes(10)));
    assertThat(storageDir10MinOld.mkdirs(), is(Boolean.TRUE));
    assertThat(new File(storageDir10MinOld, BYTES_FILE_10_MIN_OLD + ".bytes").createNewFile(), is(Boolean.TRUE));

    // create 3 files: 1 hour ago, 3 hours ago, and 10 hours ago
    File storageDir1hOld = new File(contentDir, getDatePath(blobCreated.minusHours(1)));
    assertThat(storageDir1hOld.mkdirs(), is(Boolean.TRUE));
    assertThat(new File(storageDir1hOld, BYTES_FILE_1_H_OLD + ".bytes").createNewFile(), is(Boolean.TRUE));

    File storageDir3hOld = new File(contentDir, getDatePath(blobCreated.minusHours(3)));
    assertThat(storageDir3hOld.mkdirs(), is(Boolean.TRUE));
    assertThat(new File(storageDir3hOld, BYTES_FILE_3_H_OLD + ".bytes").createNewFile(), is(Boolean.TRUE));

    File storageDir10hOld = new File(contentDir, getDatePath(blobCreated.minusHours(10)));
    assertThat(storageDir10hOld.mkdirs(), is(Boolean.TRUE));
    assertThat(new File(storageDir10hOld, BYTES_FILE_10_H_OLD + ".bytes").createNewFile(), is(Boolean.TRUE));

    // create 3 files: 1 day ago, 3 days ago, and 10 days ago
    File storageDir1dOld = new File(contentDir, getDatePath(blobCreated.minusDays(1)));
    assertThat(storageDir1dOld.mkdirs(), is(Boolean.TRUE));
    assertThat(new File(storageDir1dOld, BYTES_FILE_1_D_OLD + ".bytes").createNewFile(), is(Boolean.TRUE));

    File storageDir3dOld = new File(contentDir, getDatePath(blobCreated.minusDays(3)));
    assertThat(storageDir3dOld.mkdirs(), is(Boolean.TRUE));
    assertThat(new File(storageDir3dOld, BYTES_FILE_3_D_OLD + ".bytes").createNewFile(), is(Boolean.TRUE));

    File storageDir10dOld = new File(contentDir, getDatePath(blobCreated.minusDays(10)));
    assertThat(storageDir10dOld.mkdirs(), is(Boolean.TRUE));
    assertThat(new File(storageDir10dOld, BYTES_FILE_10_D_OLD + ".bytes").createNewFile(), is(Boolean.TRUE));

    // create 3 files: 1 month ago, 3 months ago, and 10 months ago
    File storageDir1mOld = new File(contentDir, getDatePath(blobCreated.minusMonths(1)));
    assertThat(storageDir1mOld.mkdirs(), is(Boolean.TRUE));
    assertThat(new File(storageDir1mOld, BYTES_FILE_1_M_OLD + ".bytes").createNewFile(), is(Boolean.TRUE));

    File storageDir3mOld = new File(contentDir, getDatePath(blobCreated.minusMonths(3)));
    assertThat(storageDir3mOld.mkdirs(), is(Boolean.TRUE));
    assertThat(new File(storageDir3mOld, BYTES_FILE_3_M_OLD + ".bytes").createNewFile(), is(Boolean.TRUE));

    File storageDir10mOld = new File(contentDir, getDatePath(blobCreated.minusMonths(10)));
    assertThat(storageDir10mOld.mkdirs(), is(Boolean.TRUE));
    assertThat(new File(storageDir10mOld, BYTES_FILE_10_M_OLD + ".bytes").createNewFile(), is(Boolean.TRUE));
  }

  @Test
  public void testWalkFilesWithDifferentDuration() throws Exception {
    // find all files that have been created 5 min ago
    Duration fiveMin = Duration.ofMinutes(5);
    DateBasedWalkFile walkFile = new DateBasedWalkFile(contentDir.toPath(), fiveMin);
    List<String> blobIds = new ArrayList<>(walkFile.getBlobIdToDateRef().keySet());
    assertThat(blobIds, containsInAnyOrder(BYTES_FILE_NOW, BYTES_FILE_1_MIN_OLD, BYTES_FILE_3_MIN_OLD));

    // find all files that have been created 5 hours ago
    Duration fiveHours = Duration.ofHours(5);
    walkFile = new DateBasedWalkFile(contentDir.toPath(), fiveHours);
    blobIds = new ArrayList<>(walkFile.getBlobIdToDateRef().keySet());
    assertThat(blobIds, containsInAnyOrder(
        BYTES_FILE_NOW, BYTES_FILE_1_MIN_OLD, BYTES_FILE_3_MIN_OLD, BYTES_FILE_10_MIN_OLD,
        BYTES_FILE_1_H_OLD, BYTES_FILE_3_H_OLD));

    // find all files that have been created 5 days ago
    Duration fiveDays = Duration.ofDays(5);
    walkFile = new DateBasedWalkFile(contentDir.toPath(), fiveDays);
    blobIds = new ArrayList<>(walkFile.getBlobIdToDateRef().keySet());
    assertThat(blobIds, containsInAnyOrder(
        BYTES_FILE_NOW, BYTES_FILE_1_MIN_OLD, BYTES_FILE_3_MIN_OLD, BYTES_FILE_10_MIN_OLD,
        BYTES_FILE_1_H_OLD, BYTES_FILE_3_H_OLD, BYTES_FILE_10_H_OLD,
        BYTES_FILE_1_D_OLD, BYTES_FILE_3_D_OLD));

    // find all files that have been created 5 months ago
    Duration fiveMonths = Duration.between(blobCreated.minusMonths(5), blobCreated);
    walkFile = new DateBasedWalkFile(contentDir.toPath(), fiveMonths);
    blobIds = new ArrayList<>(walkFile.getBlobIdToDateRef().keySet());
    assertThat(blobIds, containsInAnyOrder(
        BYTES_FILE_NOW, BYTES_FILE_1_MIN_OLD, BYTES_FILE_3_MIN_OLD, BYTES_FILE_10_MIN_OLD,
        BYTES_FILE_1_H_OLD, BYTES_FILE_3_H_OLD, BYTES_FILE_10_H_OLD,
        BYTES_FILE_1_D_OLD, BYTES_FILE_3_D_OLD, BYTES_FILE_10_D_OLD,
        BYTES_FILE_1_M_OLD, BYTES_FILE_3_M_OLD));
  }

  @Test
  public void testWalkFilesWithDifferentDates() throws Exception {
    // find all files that have been created 5 min ago
    Duration fiveMin = Duration.ofMinutes(5);
    DateBasedWalkFile walkFile = new DateBasedWalkFile(contentDir.toPath(), blobCreated.minus(fiveMin));
    Collection<String> blobIds = walkFile.getBlobIdToDateRef().keySet();
    assertThat(blobIds, containsInAnyOrder(BYTES_FILE_NOW, BYTES_FILE_1_MIN_OLD, BYTES_FILE_3_MIN_OLD));

    // find all files that have been created 5 hours ago
    Duration fiveHours = Duration.ofHours(5);
    walkFile = new DateBasedWalkFile(contentDir.toPath(), blobCreated.minus(fiveHours));
    blobIds = walkFile.getBlobIdToDateRef().keySet();
    assertThat(blobIds, containsInAnyOrder(
        BYTES_FILE_NOW, BYTES_FILE_1_MIN_OLD, BYTES_FILE_3_MIN_OLD, BYTES_FILE_10_MIN_OLD,
        BYTES_FILE_1_H_OLD, BYTES_FILE_3_H_OLD));

    // find all files that have been created 5 days ago
    Duration fiveDays = Duration.ofDays(5);
    walkFile = new DateBasedWalkFile(contentDir.toPath(), blobCreated.minus(fiveDays));
    blobIds = walkFile.getBlobIdToDateRef().keySet();
    assertThat(blobIds, containsInAnyOrder(
        BYTES_FILE_NOW, BYTES_FILE_1_MIN_OLD, BYTES_FILE_3_MIN_OLD, BYTES_FILE_10_MIN_OLD,
        BYTES_FILE_1_H_OLD, BYTES_FILE_3_H_OLD, BYTES_FILE_10_H_OLD,
        BYTES_FILE_1_D_OLD, BYTES_FILE_3_D_OLD));

    // find all files that have been created 5 months ago
    walkFile = new DateBasedWalkFile(contentDir.toPath(), blobCreated.minusMonths(5));
    blobIds = walkFile.getBlobIdToDateRef().keySet();
    assertThat(blobIds, containsInAnyOrder(
        BYTES_FILE_NOW, BYTES_FILE_1_MIN_OLD, BYTES_FILE_3_MIN_OLD, BYTES_FILE_10_MIN_OLD,
        BYTES_FILE_1_H_OLD, BYTES_FILE_3_H_OLD, BYTES_FILE_10_H_OLD,
        BYTES_FILE_1_D_OLD, BYTES_FILE_3_D_OLD, BYTES_FILE_10_D_OLD,
        BYTES_FILE_1_M_OLD, BYTES_FILE_3_M_OLD));
  }

  @Test
  public void testWalkFilesWithPrefix() {
    // find all files that have been created 5 hours ago
    Duration fiveHours = Duration.ofHours(5);
    DateBasedWalkFile walkFile = new DateBasedWalkFile(contentDir.toPath(), blobCreated.minus(fiveHours));
    Path prefix = Path.of(getDatePath(blobCreated.minusHours(3)));
    Collection<String> blobIds = walkFile.getBlobIdToDateRef(prefix).keySet();
    assertThat(blobIds, containsInAnyOrder(BYTES_FILE_3_H_OLD));
  }

  @Test
  public void testWalkFilesWithPrefixExactMinutes() {
    Duration threeMinutes = Duration.ofMinutes(3);
    DateBasedWalkFile walkFile = new DateBasedWalkFile(contentDir.toPath(), blobCreated.minus(threeMinutes));
    Path prefix = Path.of(getDatePath(blobCreated.minus(threeMinutes)));
    Collection<String> blobIds = walkFile.getBlobIdToDateRef(prefix).keySet();
    assertThat(blobIds, containsInAnyOrder(BYTES_FILE_3_MIN_OLD));
  }

  private static String getDatePath(final OffsetDateTime blobCreated) {
    return blobCreated.format(BlobRef.DATE_TIME_PATH_FORMATTER);
  }
}
