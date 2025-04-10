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
package org.sonatype.nexus.blobstore.internal;

import java.util.Iterator;
import java.util.Objects;
import java.util.Optional;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.blobstore.api.BlobId;
import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.blobstore.api.softdeleted.SoftDeletedBlob;
import org.sonatype.nexus.blobstore.api.softdeleted.SoftDeletedBlobIndex;
import org.sonatype.nexus.blobstore.api.softdeleted.SoftDeletedBlobsStore;
import org.sonatype.nexus.common.entity.Continuation;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

@Priority(Integer.MAX_VALUE)
@Named("default")
public class SoftDeletedBlobIndexImpl
    extends ComponentSupport
    implements SoftDeletedBlobIndex
{
  protected final SoftDeletedBlobsStore softDeletedBlobsStore;

  private final int deletedFileCacheLimit;

  protected String blobStoreName;

  private Iterator<BlobId> currentBatchIterator;

  private Continuation<SoftDeletedBlob> deletionContinuation;

  @Inject
  public SoftDeletedBlobIndexImpl(
      final SoftDeletedBlobsStore softDeletedBlobsStore,
      @Named("${nexus.blobstore.deletion.buffer.size:-1000}") final int deletedFileCacheLimit)
  {
    this.softDeletedBlobsStore = checkNotNull(softDeletedBlobsStore);
    checkArgument(deletedFileCacheLimit > 0);
    this.deletedFileCacheLimit = deletedFileCacheLimit;
  }

  @Override
  public void init(final BlobStore blobStore) {
    checkState(blobStoreName == null, "Previously initialized");
    checkNotNull(blobStore);
    this.blobStoreName = checkNotNull(blobStore.getBlobStoreConfiguration().getName());
  }

  @Override
  public final void createRecord(final BlobId blobId) {
    softDeletedBlobsStore.createRecord(blobId, blobStoreName);
  }

  @Override
  public final BlobId getNextAvailableRecord() {
    if (Objects.isNull(currentBatchIterator) || !currentBatchIterator.hasNext()) {
      populateInternalCache();
    }

    return Objects.nonNull(this.currentBatchIterator) ? currentBatchIterator.next() : null;
  }

  @Override
  public final void deleteRecord(final BlobId blobId) {
    softDeletedBlobsStore.deleteRecord(blobStoreName, blobId);
  }

  @Override
  public final void deleteAllRecords() {
    softDeletedBlobsStore.deleteAllRecords(blobStoreName);
  }

  @Override
  public final int size() {
    return softDeletedBlobsStore.count(blobStoreName);
  }

  private void populateInternalCache() {
    String token = Optional.ofNullable(deletionContinuation)
        .filter(value -> !value.isEmpty())
        .map(Continuation::nextContinuationToken)
        .orElse("0");

    deletionContinuation = softDeletedBlobsStore.readRecords(token,
        Math.abs(deletedFileCacheLimit), blobStoreName);

    if (!deletionContinuation.isEmpty()) {
      currentBatchIterator = this.deletionContinuation.stream()
          .map(value -> new BlobId(value.getBlobId(), value.getDatePathRef()))
          .iterator();
    }
    else {
      currentBatchIterator = null;
    }
  }
}
