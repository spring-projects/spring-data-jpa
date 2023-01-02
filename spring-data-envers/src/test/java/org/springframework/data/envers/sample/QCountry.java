/*
 * Copyright 2015-2023 the original author or authors.
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
package org.springframework.data.envers.sample;

import com.querydsl.core.types.Path;
import com.querydsl.core.types.PathMetadata;
import com.querydsl.core.types.dsl.EntityPathBase;
import com.querydsl.core.types.dsl.PathInits;
import com.querydsl.core.types.dsl.StringPath;

import static com.querydsl.core.types.PathMetadataFactory.forVariable;

/**
 * Query class for Country domain.
 *
 * @author Dmytro Iaroslavskyi
 */
public class QCountry extends EntityPathBase<Country> {

	private static final long serialVersionUID = -936338527;

	private static final PathInits INITS = PathInits.DIRECT2;

	public static final QCountry country = new QCountry("country");

	public final StringPath code = createString("code");
	public final StringPath name = createString("name");

	public QCountry(String variable) {
		this(Country.class, forVariable(variable), INITS);
	}

	@SuppressWarnings("all")
	public QCountry(Path<? extends Country> path) {
		this((Class) path.getType(), path.getMetadata(), path.getMetadata().isRoot() ? INITS : PathInits.DEFAULT);
	}

	public QCountry(PathMetadata metadata) {
		this(metadata, metadata.isRoot() ? INITS : PathInits.DEFAULT);
	}

	public QCountry(PathMetadata metadata, PathInits inits) {
		this(Country.class, metadata, inits);
	}

	public QCountry(Class<? extends Country> type, PathMetadata metadata, PathInits inits) {
		super(type, metadata, inits);
	}

}
