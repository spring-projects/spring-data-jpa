/*
 * Copyright 2014-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.jpa.convert.threeten;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Date;

import org.springframework.data.convert.Jsr310Converters.DateToLocalDateConverter;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.data.convert.Jsr310Converters.DateToLocalDateTimeConverter;
import org.springframework.data.convert.Jsr310Converters.DateToLocalTimeConverter;
import org.springframework.data.convert.Jsr310Converters.LocalDateTimeToDateConverter;
import org.springframework.data.convert.Jsr310Converters.LocalDateToDateConverter;
import org.springframework.data.convert.Jsr310Converters.LocalTimeToDateConverter;
import org.springframework.data.convert.Jsr310Converters.StringToZoneIdConverter;
import org.springframework.data.convert.Jsr310Converters.ZoneIdToStringConverter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;

/**
 * JPA 2.1 converters to turn JSR-310 types into legacy {@link Date}s. To activate these converters make sure your
 * persistence provider detects them by including this class in the list of mapped classes. In Spring environments, you
 * can simply register the package of this class (i.e. {@code org.springframework.data.jpa.convert.threeten}) as package
 * to be scanned on e.g. the {@link LocalContainerEntityManagerFactoryBean}.
 *
 * @author Oliver Gierke
 * @author Kevin Peters
 * @author Greg Turnquist
 */
public class Jsr310JpaConverters {

	@Converter(autoApply = true)
	public static class LocalDateConverter implements AttributeConverter<@Nullable LocalDate, @Nullable Date> {

		@Override
		public @Nullable Date convertToDatabaseColumn(@Nullable LocalDate date) {
			return date == null ? null : LocalDateToDateConverter.INSTANCE.convert(date);
		}

		@Override
		public @Nullable LocalDate convertToEntityAttribute(@Nullable Date date) {
			return date == null ? null : DateToLocalDateConverter.INSTANCE.convert(date);
		}
	}

	@Converter(autoApply = true)
	public static class LocalTimeConverter implements AttributeConverter<@Nullable LocalTime, @Nullable Date> {

		@Override
		public @Nullable Date convertToDatabaseColumn(@Nullable LocalTime time) {
			return time == null ? null : LocalTimeToDateConverter.INSTANCE.convert(time);
		}

		@Override
		public @Nullable LocalTime convertToEntityAttribute(@Nullable Date date) {
			return date == null ? null : DateToLocalTimeConverter.INSTANCE.convert(date);
		}
	}

	@Converter(autoApply = true)
	public static class LocalDateTimeConverter implements AttributeConverter<@Nullable LocalDateTime, @Nullable Date> {

		@Override
		public @Nullable Date convertToDatabaseColumn(@Nullable LocalDateTime date) {
			return date == null ? null : LocalDateTimeToDateConverter.INSTANCE.convert(date);
		}

		@Override
		public @Nullable LocalDateTime convertToEntityAttribute(@Nullable Date date) {
			return date == null ? null : DateToLocalDateTimeConverter.INSTANCE.convert(date);
		}
	}

	@Converter(autoApply = true)
	public static class InstantConverter implements AttributeConverter<@Nullable Instant, @Nullable Timestamp> {

		@Override
		public @Nullable Timestamp convertToDatabaseColumn(@Nullable Instant instant) {
			return instant == null ? null : InstantToTimestampConverter.INSTANCE.convert(instant);
		}

		@Override
		public @Nullable Instant convertToEntityAttribute(@Nullable Timestamp timestamp) {
			return timestamp == null ? null : TimestampToInstantConverter.INSTANCE.convert(timestamp);
		}
	}

	@Converter(autoApply = true)
	public static class ZoneIdConverter implements AttributeConverter<@Nullable ZoneId, @Nullable String> {

		@Override
		public @Nullable String convertToDatabaseColumn(@Nullable ZoneId zoneId) {
			return zoneId == null ? null : ZoneIdToStringConverter.INSTANCE.convert(zoneId);
		}

		@Override
		public @Nullable ZoneId convertToEntityAttribute(@Nullable String zoneId) {
			return zoneId == null ? null : StringToZoneIdConverter.INSTANCE.convert(zoneId);
		}
	}

	@ReadingConverter
	enum TimestampToInstantConverter implements org.springframework.core.convert.converter.Converter<Timestamp, Instant> {

		INSTANCE;

		@NonNull
		@Override
		public Instant convert(Timestamp source) {
			return source.toInstant();
		}
	}

	@WritingConverter
	enum InstantToTimestampConverter implements org.springframework.core.convert.converter.Converter<Instant, Timestamp> {

		INSTANCE;

		@NonNull
		@Override
		public Timestamp convert(Instant source) {
			return Timestamp.from(source);
		}
	}
}
