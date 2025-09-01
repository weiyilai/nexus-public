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
package org.sonatype.nexus.repository.content.fluent.internal;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import org.sonatype.goodies.testsupport.Test5Support;
import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.blobstore.api.BlobId;
import org.sonatype.nexus.blobstore.api.BlobMetrics;
import org.sonatype.nexus.blobstore.api.BlobRef;
import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.common.hash.HashAlgorithm;
import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.content.AssetBlob;
import org.sonatype.nexus.repository.content.facet.ContentFacetSupport;
import org.sonatype.nexus.repository.content.facet.ContentFacetStores;
import org.sonatype.nexus.repository.content.store.AssetBlobStore;
import org.sonatype.nexus.repository.content.store.AssetData;
import org.sonatype.nexus.repository.content.store.AssetStore;
import org.sonatype.nexus.repository.content.store.FormatStoreManager;
import org.sonatype.nexus.blobstore.api.BlobStoreManager;
import org.sonatype.nexus.repository.storage.BlobMetadataStorage;
import org.sonatype.nexus.repository.view.payloads.TempBlob;

import com.google.common.hash.HashCode;
import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.doAnswer;

/**
 * Tests for {@link FluentAssetBuilderImpl} focusing on NEXUS-46487 timestamp preservation functionality.
 */
class FluentAssetBuilderImplTest
    extends Test5Support
{
  @Mock
  private ContentFacetSupport contentFacet;

  private ContentFacetStores contentFacetStores;

  @Mock
  private AssetStore assetStore;

  @Mock
  private AssetBlobStore assetBlobStore;

  @Mock
  private BlobStoreManager blobStoreManager;

  @Mock
  private BlobStore blobStore;

  @Mock
  private FormatStoreManager formatStoreManager;

  @Mock
  private TempBlob tempBlob;

  @Mock
  private Blob blob;

  @Mock
  private BlobMetrics blobMetrics;

  @Mock
  private BlobRef blobRef;

  @Mock
  private BlobId blobId;

  @Mock
  private AssetData asset;

  @Mock
  private BlobMetadataStorage blobMetadataStorage;

  @Mock
  private AssetBlob existingAssetBlob;

  @Mock
  private AssetBlob newAssetBlob;

  private FluentAssetBuilderImpl underTest;

  private final OffsetDateTime originalTimestamp = OffsetDateTime.of(2025, 1, 15, 10, 30, 0, 0, ZoneOffset.UTC);

  private final Map<HashAlgorithm, HashCode> checksums = new HashMap<>();

  @BeforeEach
  void setup() {
    // Setup BlobStoreManager to return our mocks
    lenient().when(blobStoreManager.get("test-blobstore")).thenReturn(blobStore);
    lenient().when(blobStore.makeBlobPermanent(any(), any())).thenReturn(blob);

    // Setup FormatStoreManager to return our mocks
    when(formatStoreManager.assetStore("test-content")).thenReturn(assetStore);
    when(formatStoreManager.assetBlobStore("test-content")).thenReturn(assetBlobStore);
    when(formatStoreManager.contentRepositoryStore("test-content")).thenReturn(null); // Not needed for tests
    when(formatStoreManager.componentStore("test-content")).thenReturn(null); // Not needed for tests

    // Create real ContentFacetStores with mocked dependencies
    contentFacetStores = new ContentFacetStores(blobStoreManager, "test-blobstore", formatStoreManager, "test-content");
    lenient().when(contentFacet.stores()).thenReturn(contentFacetStores);
    lenient().when(contentFacet.contentRepositoryId()).thenReturn(1);

    // Setup basic blob properties
    lenient().when(tempBlob.getBlob()).thenReturn(blob);
    lenient().when(tempBlob.getHashes()).thenReturn(checksums);
    lenient().when(blob.getMetrics()).thenReturn(blobMetrics);
    lenient().when(blob.getId()).thenReturn(blobId);
    lenient().when(blobMetrics.getCreationTime())
        .thenReturn(new DateTime(originalTimestamp.toInstant().toEpochMilli()));

    // Setup blob headers for makePermanent method
    Map<String, String> blobHeaders = new HashMap<>();
    blobHeaders.put("REPO_NAME", "test-repo");
    blobHeaders.put("CREATED_BY", "test-user");
    blobHeaders.put("CREATED_BY_IP", "127.0.0.1");
    blobHeaders.put("BlobStore.content-type", "application/octet-stream");
    lenient().when(blob.getHeaders()).thenReturn(blobHeaders);

    // Setup facet for blobRef creation
    lenient().when(contentFacet.nodeName()).thenReturn("test-node");
    lenient().when(contentFacet.blobMetadataStorage()).thenReturn(blobMetadataStorage);
    lenient().when(contentFacet.checkContentType(any(), any())).thenReturn("application/octet-stream");
    lenient().when(blobId.asUniqueString()).thenReturn("test-blob-id");
    lenient().when(blobId.getBlobCreatedRef()).thenReturn(originalTimestamp);

    underTest = new FluentAssetBuilderImpl(contentFacet, assetStore, "/test-path");
    // Set up blob directly to simulate what happens in save() method
    underTest.blob(tempBlob);
  }

  @Test
  void save_preservesOriginalBlobCreatedTimestamp_whenAssetHasExistingBlob() {
    // Given: Asset with existing blob that has a timestamp
    when(asset.blob()).thenReturn(Optional.of(existingAssetBlob));
    when(existingAssetBlob.blobCreated()).thenReturn(originalTimestamp);
    when(assetBlobStore.readAssetBlob(any(BlobRef.class))).thenReturn(Optional.of(newAssetBlob));
    
    // Mock assetStore.save to actually call the updateAsset lambda (3rd parameter)
    doAnswer(invocation -> {
      Function<Asset, Asset> updateAsset = invocation.getArgument(2);
      return updateAsset.apply(asset);
    }).when(assetStore).save(any(), any(), any(), any());

    // When: save is called (which internally calls updateAssetBlob)
    underTest.save();

    // Then: Original timestamp is preserved on the new AssetBlob
    verify(assetBlobStore).setBlobCreated(eq(newAssetBlob), eq(originalTimestamp));
    verify(assetBlobStore).readAssetBlob(any(BlobRef.class));
  }

  @Test
  void save_doesNotSetBlobCreated_whenAssetHasNoExistingBlob() {
    // Given: Asset with no existing blob
    when(asset.blob()).thenReturn(Optional.empty());
    when(assetBlobStore.readAssetBlob(any(BlobRef.class))).thenReturn(Optional.of(newAssetBlob));
    
    // Mock assetStore.save to actually call the updateAsset lambda (3rd parameter)
    doAnswer(invocation -> {
      Function<Asset, Asset> updateAsset = invocation.getArgument(2);
      return updateAsset.apply(asset);
    }).when(assetStore).save(any(), any(), any(), any());

    // When: save is called (which internally calls updateAssetBlob)
    underTest.save();

    // Then: setBlobCreated is not called since there's no original timestamp, but readAssetBlob is still called
    verify(assetBlobStore).readAssetBlob(any(BlobRef.class));
  }

  @Test
  void save_createsNewAssetBlob_whenNotInStore() {
    // Given: Asset with existing blob, but AssetBlob not found in store (needs creation)
    when(asset.blob()).thenReturn(Optional.of(existingAssetBlob));
    when(existingAssetBlob.blobCreated()).thenReturn(originalTimestamp);
    when(assetBlobStore.readAssetBlob(any(BlobRef.class))).thenReturn(Optional.empty());
    
    // Mock assetStore.save to actually call the updateAsset lambda (3rd parameter)
    doAnswer(invocation -> {
      Function<Asset, Asset> updateAsset = invocation.getArgument(2);
      return updateAsset.apply(asset);
    }).when(assetStore).save(any(), any(), any(), any());

    // Setup for AssetBlob creation (createAssetBlob is void, doesn't return anything)

    // When: save is called (which internally calls updateAssetBlob)
    underTest.save();

    // Then: New AssetBlob is created and timestamp is preserved
    verify(assetBlobStore).readAssetBlob(any(BlobRef.class));
    verify(assetBlobStore).createAssetBlob(any());
    verify(assetBlobStore).setBlobCreated((AssetBlob) any(), eq(originalTimestamp));
  }

  @Test
  void save_doesNothingWhenBlobIsNull() {
    // Given: FluentAssetBuilderImpl with no blob set
    FluentAssetBuilderImpl builderWithoutBlob = new FluentAssetBuilderImpl(contentFacet, assetStore, "/test-path");
    // No blob field is set (remains null)

    // Mock assetStore.save to actually call the updateAsset lambda (3rd parameter)
    doAnswer(invocation -> {
      Function<Asset, Asset> updateAsset = invocation.getArgument(2);
      return updateAsset.apply(asset);
    }).when(assetStore).save(any(), any(), any(), any());

    // When: save is called
    builderWithoutBlob.save();

    // Then: No interactions with blob store occur (assetStore is still called for save)
    verifyNoInteractions(assetBlobStore);
  }

  @Test
  void save_usesNewTimestamp_whenCreatingBrandNewAssetBlob() {
    // Given: Asset with existing blob, AssetBlob needs to be created with new timestamp
    when(asset.blob()).thenReturn(Optional.of(existingAssetBlob));
    when(existingAssetBlob.blobCreated()).thenReturn(originalTimestamp);
    when(assetBlobStore.readAssetBlob(any(BlobRef.class))).thenReturn(Optional.empty());
    
    // Mock assetStore.save to actually call the updateAsset lambda (3rd parameter)
    doAnswer(invocation -> {
      Function<Asset, Asset> updateAsset = invocation.getArgument(2);
      return updateAsset.apply(asset);
    }).when(assetStore).save(any(), any(), any(), any());

    // When: save is called (which internally calls updateAssetBlob)
    underTest.save();

    // Then: AssetBlob is created with current timestamp, then original timestamp is restored
    verify(assetBlobStore).readAssetBlob(any(BlobRef.class));
    verify(assetBlobStore).createAssetBlob(any());
    verify(assetBlobStore).setBlobCreated((AssetBlob) any(), eq(originalTimestamp));
  }
}
