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

import java.io.InputStream;

import org.sonatype.nexus.blobstore.api.BlobStoreException;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.TransferManagerBuilder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * Uploads a file with the TransferManager.
 *
 * @since 3.7
 * @deprecated replaced with {@link MultipartUploader}
 */
@ConditionalOnProperty(name = "nexus.s3.uploaderName", havingValue = "transfer-manager-uploader")
@Deprecated
@Component
@Qualifier("transfer-manager-uploader")
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class TransferManagerUploader
    implements S3Uploader
{
  @Override
  public void upload(final AmazonS3 s3, final String bucket, final String key, final InputStream contents) {
    try {
      TransferManager transferManager = TransferManagerBuilder.standard().withS3Client(s3).build();
      transferManager.upload(bucket, key, contents, new ObjectMetadata())
          .waitForCompletion();
    }
    catch (InterruptedException e) {
      throw new BlobStoreException("error uploading blob", e, null);
    }
  }
}
