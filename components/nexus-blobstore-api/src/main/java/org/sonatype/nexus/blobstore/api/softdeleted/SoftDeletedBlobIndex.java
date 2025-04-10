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
package org.sonatype.nexus.blobstore.api.softdeleted;

import javax.annotation.Nullable;

import org.sonatype.nexus.blobstore.api.BlobId;
import org.sonatype.nexus.blobstore.api.BlobStore;

/**
 * An index of soft-deleted blobs for a blobstore
 */
public interface SoftDeletedBlobIndex
{
  /**
   * Initializes the index for the provided blobstore
   */
  void init(BlobStore blobstore);

  /**
   * Add new record to deletion index
   *
   * @param blobId the {@link BlobId} that is referenced to the Blob that was soft-deleted
   */
  void createRecord(BlobId blobId);

  /**
   * Get the next BlobId currently present, may return {@code null} if no deletion records are present
   *
   * @return oldest {@link BlobId} or null
   */
  @Nullable
  BlobId getNextAvailableRecord();

  /**
   * Deletes specified record by {@link BlobId}
   *
   * @param blobId the {@link BlobId} of record to be deleted
   */
  void deleteRecord(BlobId blobId);

  /**
   * Deletes all records currently holding
   */
  void deleteAllRecords();

  /**
   * Returns the amount of soft deleted blobs records
   *
   * @return amount of soft deleted blobs
   */
  int size();
}
