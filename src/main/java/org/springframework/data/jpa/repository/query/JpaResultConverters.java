/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.jpa.repository.query;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Blob;
import java.sql.SQLException;

import org.springframework.core.convert.converter.Converter;
import org.springframework.util.StreamUtils;

/**
 * Container for additional JPA result {@link Converter}s.
 * 
 * @author Thomas Darimont
 * @since 1.6
 */
class JpaResultConverters {

	/**
	 * {@code private} to prevent instantiation.
	 */
	private JpaResultConverters() {}

	/**
	 * Converts the given {@link Blob} into a {@code byte[]}.
	 * 
	 * @author Thomas Darimont
	 */
	enum BlobToByteArrayConverter implements Converter<Blob, byte[]> {

		INSTANCE;

		@Override
		public byte[] convert(Blob source) {

			if (source == null) {
				return null;
			}

			try {

				InputStream blobStream = source.getBinaryStream();

				if (blobStream != null) {

					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					StreamUtils.copy(blobStream, baos);
					return baos.toByteArray();
				}

			} catch (SQLException e) {
				throw new RuntimeException(e);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}

			return null;
		}
	}
}
