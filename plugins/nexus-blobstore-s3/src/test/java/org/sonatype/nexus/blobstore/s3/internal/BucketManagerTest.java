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
package org.sonatype.nexus.blobstore.s3.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.blobstore.MockBlobStoreConfiguration;
import org.sonatype.nexus.blobstore.api.BlobStoreConfiguration;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.BucketLifecycleConfiguration;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import junitparams.JUnitParamsRunner;
import junitparams.NamedParameters;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.blobstore.s3.S3BlobStoreConfigurationHelper.BUCKET_KEY;
import static org.sonatype.nexus.blobstore.s3.S3BlobStoreConfigurationHelper.CONFIG_KEY;
import static org.sonatype.nexus.blobstore.s3.internal.S3BlobStoreException.ACCESS_DENIED_CODE;
import static org.sonatype.nexus.blobstore.s3.internal.S3BlobStoreException.BUCKET_OWNERSHIP_ERR_MSG;
import static org.sonatype.nexus.blobstore.s3.internal.S3BlobStoreException.ERROR_CODE_MESSAGES;
import static org.sonatype.nexus.blobstore.s3.internal.S3BlobStoreException.INSUFFICIENT_PERM_CREATE_BUCKET_ERR_MSG;
import static org.sonatype.nexus.blobstore.s3.internal.S3BlobStoreException.INVALID_ACCESS_KEY_ID_CODE;
import static org.sonatype.nexus.blobstore.s3.internal.S3BlobStoreException.INVALID_IDENTITY_ERR_MSG;
import static org.sonatype.nexus.blobstore.s3.internal.S3BlobStoreException.SIGNATURE_DOES_NOT_MATCH_CODE;

/**
 * {@link BucketManager} tests.
 */
@RunWith(JUnitParamsRunner.class)
public class BucketManagerTest
    extends TestSupport
{
  @Mock
  private AmazonS3 s3;

  @Mock
  private BucketOwnershipCheckFeatureFlag featureFlag;

  @Mock
  private BucketOperations bucketOperations;

  private BucketManager underTest;

  @Before
  public void setup() {
    when(featureFlag.isDisabled()).thenReturn(false);
    underTest = new BucketManager(featureFlag, List.of(bucketOperations));
    underTest.setS3(s3);
  }

  @Test
  public void deleteStorageLocationRemovesBucketIfEmpty() {
    ObjectListing listingMock = mock(ObjectListing.class);
    when(listingMock.getObjectSummaries()).thenReturn(new ArrayList<>());
    when(s3.listObjects(any(ListObjectsRequest.class))).thenReturn(listingMock);
    underTest.setS3(s3);

    BlobStoreConfiguration cfg = new MockBlobStoreConfiguration();
    Map<String, Map<String, Object>> attr = ImmutableMap.of("s3", ImmutableMap
        .of("bucket", "mybucket", "expiration", "3"));
    cfg.setAttributes(attr);

    underTest.deleteStorageLocation(cfg);

    verify(s3).deleteBucket("mybucket");
  }

  @Test
  public void deleteStorageLocationDoesNotRemoveBucketIfNotEmpty() {
    ObjectListing listingMock = mock(ObjectListing.class);
    when(listingMock.getObjectSummaries()).thenReturn(ImmutableList.of(new S3ObjectSummary()));
    when(s3.listObjects(any(ListObjectsRequest.class))).thenReturn(listingMock);
    when(s3.getBucketLifecycleConfiguration(anyString())).thenReturn(mock(BucketLifecycleConfiguration.class));
    underTest.setS3(s3);

    BlobStoreConfiguration cfg = new MockBlobStoreConfiguration();
    Map<String, Map<String, Object>> attr = ImmutableMap.of("s3", ImmutableMap
        .of("bucket", "mybucket", "expiration", "3"));
    cfg.setAttributes(attr);

    underTest.deleteStorageLocation(cfg);

    verify(s3, times(0)).deleteBucket("mybucket");
  }

  @Test
  public void testOwnershipErrorIsNotThrownOnDisabledOwnershipCheck() {
    String bucketName = "bucketName";
    when(s3.doesBucketExistV2(anyString())).thenReturn(true);
    when(s3.getBucketPolicy(anyString())).thenThrow(AmazonClientException.class);
    when(featureFlag.isDisabled()).thenReturn(true);

    Map<String, Map<String, Object>> cfgAttributes = ImmutableMap.of(CONFIG_KEY,
        ImmutableMap.of(BUCKET_KEY, bucketName));
    BlobStoreConfiguration cfg = new MockBlobStoreConfiguration();
    cfg.setAttributes(cfgAttributes);
    underTest.setS3(s3);

    underTest.prepareStorageLocation(cfg);

    verify(s3, times(0)).getBucketPolicy(anyString());
  }

  @Test
  public void deleteCallsBucketOperationsImpl() {
    ObjectListing listingMock = mock(ObjectListing.class);
    when(listingMock.getObjectSummaries()).thenReturn(ImmutableList.of(new S3ObjectSummary()));
    when(s3.listObjects(any(ListObjectsRequest.class))).thenReturn(listingMock);
    underTest.setS3(s3);

    BlobStoreConfiguration cfg = new MockBlobStoreConfiguration();
    cfg.setName("my_s3_blob_store");
    Map<String, Map<String, Object>> attr = ImmutableMap.of("s3", ImmutableMap
        .of("bucket", "mybucket", "expiration", "3"));
    cfg.setAttributes(attr);

    underTest.deleteStorageLocation(cfg);

    verify(bucketOperations).delete("my_s3_blob_store", "mybucket", s3);
  }

  @Test
  @Parameters(named = "errorCodeAndMessageParams")
  public void errorThrownWhenBucketCannotBeCreated(
      final String errorCode,
      final String message)
  {
    String bucketName = "bucketName";
    when(s3.doesBucketExistV2(anyString())).thenReturn(false);
    AmazonS3Exception s3Exception = mock(AmazonS3Exception.class);
    when(s3Exception.getErrorCode()).thenReturn(errorCode);
    when(s3.createBucket(anyString())).thenThrow(s3Exception);

    Map<String, Map<String, Object>> cfgAttributes = ImmutableMap.of(CONFIG_KEY,
        ImmutableMap.of(BUCKET_KEY, bucketName));
    BlobStoreConfiguration cfg = new MockBlobStoreConfiguration();
    cfg.setAttributes(cfgAttributes);
    underTest.setS3(s3);

    Exception ex = assertThrows(S3BlobStoreException.class, () -> underTest.prepareStorageLocation(cfg));
    assertEquals(message, ex.getMessage());
  }

  @NamedParameters("errorCodeAndMessageParams")
  private Object[] errorCodeAndMessageParams() {
    return new Object[]{
        new Object[]{
            ACCESS_DENIED_CODE, INSUFFICIENT_PERM_CREATE_BUCKET_ERR_MSG
        },
        new Object[]{
            "Some_Unexpected_Code", "An unexpected error occurred creating bucket. Check the logs for more details."
        }
    };
  }

  @Test
  @Parameters(named = "errorCodeAndMessageInvalidPermissionsParams")
  public void errorCodeAndMessageInvalidPermissionsParams(
      final String errorCode,
      final String message)
  {
    String bucketName = "bucketName";
    AmazonS3Exception s3Exception = mock(AmazonS3Exception.class);
    when(s3Exception.getErrorCode()).thenReturn(errorCode);
    when(s3.doesBucketExistV2(anyString())).thenThrow(s3Exception);

    Map<String, Map<String, Object>> cfgAttributes = ImmutableMap.of(CONFIG_KEY,
        ImmutableMap.of(BUCKET_KEY, bucketName));
    BlobStoreConfiguration cfg = new MockBlobStoreConfiguration();
    cfg.setAttributes(cfgAttributes);
    underTest.setS3(s3);

    Exception ex = assertThrows(S3BlobStoreException.class, () -> underTest.prepareStorageLocation(cfg));
    assertEquals(message, ex.getMessage());
  }

  @NamedParameters("errorCodeAndMessageInvalidPermissionsParams")
  private Object[] errorCodeAndMessageInvalidPermissionsParams() {
    return new Object[]{
        new Object[]{
            "InvalidAccessKeyId", ERROR_CODE_MESSAGES.get(INVALID_ACCESS_KEY_ID_CODE)
        },
        new Object[]{
            "SignatureDoesNotMatch", ERROR_CODE_MESSAGES.get(SIGNATURE_DOES_NOT_MATCH_CODE)
        },
        new Object[]{
            "Some_Unexpected_Code",
            "An unexpected error occurred checking credentials. Check the logs for more details."
        }
    };
  }

  @Test
  @Parameters(named = "errorCodeAndMessageUserWithoutAccessParams")
  public void errorThrownIfUserDoesNotHaveAccessToAnExistingBucket(
      final String errorCode,
      final String message)
  {
    String bucketName = "bucketName";
    when(s3.doesBucketExistV2(anyString())).thenReturn(true);
    AmazonS3Exception s3Exception = mock(AmazonS3Exception.class);
    when(s3Exception.getErrorCode()).thenReturn(errorCode);
    when(s3.getBucketPolicy(anyString())).thenThrow(s3Exception);

    Map<String, Map<String, Object>> cfgAttributes = ImmutableMap.of(CONFIG_KEY,
        ImmutableMap.of(BUCKET_KEY, bucketName));
    BlobStoreConfiguration cfg = new MockBlobStoreConfiguration();
    cfg.setAttributes(cfgAttributes);
    underTest.setS3(s3);

    Exception ex = assertThrows(S3BlobStoreException.class, () -> underTest.prepareStorageLocation(cfg));
    assertEquals(message, ex.getMessage());
  }

  @NamedParameters("errorCodeAndMessageUserWithoutAccessParams")
  private Object[] errorCodeAndMessageUserWithoutAccessParams() {
    return new Object[]{
        new Object[]{
            "AccessDenied", BUCKET_OWNERSHIP_ERR_MSG
        },
        new Object[]{
            "Some_Unexpected_Code",
            "An unexpected error occurred checking bucket ownership. Check the logs for more details."
        },
        new Object[]{
            "MethodNotAllowed", INVALID_IDENTITY_ERR_MSG
        }
    };
  }
}
