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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.blobstore.DateBasedHelper;
import org.sonatype.nexus.common.time.UTC;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.blobstore.BlobStoreSupport.CONTENT_PREFIX;
import static org.sonatype.nexus.blobstore.api.BlobRef.DATE_TIME_PATH_FORMATTER;

/**
 * Walks the blob storage to find all files that have been created since a given duration.
 */
public class DateBasedWalkFile
    extends ComponentSupport
{
  // to support Unix and Windows file separators
  private static final String FILE_SEPARATOR = "[\\\\/]";

  private static final String TWO_DIGIT_MATCHER = "\\d{2}";

  private static final Pattern DATE_BASED_PATTERN = Pattern.compile(String.join(FILE_SEPARATOR,
      ".*", CONTENT_PREFIX, "(\\d{4}", TWO_DIGIT_MATCHER, TWO_DIGIT_MATCHER, TWO_DIGIT_MATCHER, TWO_DIGIT_MATCHER + ")",
      "(.*)\\.(bytes|properties)$"),
      Pattern.CASE_INSENSITIVE);

  private final Path contentDir;

  private final Duration duration;

  private final OffsetDateTime fromDateTime;

  public DateBasedWalkFile(final Path contentDir, final Duration duration) {
    this.fromDateTime = null;
    this.contentDir = contentDir;
    this.duration = checkNotNull(duration);
  }

  public DateBasedWalkFile(final Path contentDir, final OffsetDateTime fromDateTime) {
    this.duration = null;
    this.fromDateTime = checkNotNull(fromDateTime);
    this.contentDir = checkNotNull(contentDir);
  }

  public Map<String, OffsetDateTime> getBlobIdToDateRef() {
    OffsetDateTime now = UTC.now();
    OffsetDateTime from = Optional.ofNullable(duration).map(now::minus).orElse(this.fromDateTime);
    Path datePathPrefix = contentDir.resolve(DateBasedHelper.getDatePathPrefix(from, now));

    try {
      return getAllFiles(datePathPrefix, from);
    }
    catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public Map<String, OffsetDateTime> getBlobIdToDateRef(final Path prefix) {
    try {
      return getAllFiles(contentDir.resolve(prefix), this.fromDateTime);
    }
    catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  protected Map<String, OffsetDateTime> getAllFiles(
      final Path startPath,
      final OffsetDateTime fromDateTime) throws IOException
  {
    if (Files.exists(startPath)) {
      DateBasedFileVisitor visitor = new DateBasedFileVisitor(fromDateTime);

      Files.walkFileTree(startPath, visitor);

      return visitor.getBlobIds();
    }
    return Collections.emptyMap();
  }

  private class DateBasedFileVisitor
      extends SimpleFileVisitor<Path>
  {

    private final OffsetDateTime fromDateTime;

    private final Map<String, OffsetDateTime> blobIds = new HashMap<>();

    public DateBasedFileVisitor(final OffsetDateTime fromDateTime) {
      this.fromDateTime = checkNotNull(fromDateTime);
    }

    public Map<String, OffsetDateTime> getBlobIds() {
      return blobIds;
    }

    @Override
    public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) {
      Matcher matcher = DATE_BASED_PATTERN.matcher(file.toString());

      if (matcher.matches()) {
        getDateFromMatch(matcher.group(1))
            .filter(date -> !date.isBefore(this.fromDateTime.truncatedTo(ChronoUnit.MINUTES)))
            .map(date -> blobIds.put(matcher.group(2), date));
      }

      return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(final Path file, final IOException e) {
      log.error("Failed to access file: {} Error: {}", file, e.getMessage());
      return FileVisitResult.CONTINUE;
    }

    private Optional<OffsetDateTime> getDateFromMatch(final String matchPath) {
      try {
        LocalDateTime localDateTime = LocalDateTime.parse(matchPath, DATE_TIME_PATH_FORMATTER);
        return Optional.of(localDateTime.atOffset(ZoneOffset.UTC));
      }
      catch (DateTimeParseException ex) {
        // we don't care about the files that are not in the expected format
        log.debug("Incorrect date format in path: {}", matchPath);
      }

      return Optional.empty();
    }
  }
}
