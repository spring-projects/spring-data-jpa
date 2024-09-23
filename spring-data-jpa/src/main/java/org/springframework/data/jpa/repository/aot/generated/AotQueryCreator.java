/*
 * Copyright 2025 the original author or authors.
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
package org.springframework.data.jpa.repository.aot.generated;

import jakarta.persistence.metamodel.Metamodel;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.query.EscapeCharacter;
import org.springframework.data.jpa.repository.query.JpaParameters;
import org.springframework.data.jpa.repository.query.JpaQueryCreator;
import org.springframework.data.jpa.repository.query.ParameterMetadataProvider;
import org.springframework.data.jpa.repository.support.JpqlQueryTemplates;
import org.springframework.data.repository.aot.generate.AotRepositoryMethodGenerationContext;
import org.springframework.data.repository.query.ParametersSource;
import org.springframework.data.repository.query.ReturnedType;
import org.springframework.data.repository.query.parser.PartTree;

/**
 * @author Christoph Strobl
 * @since 2025/01
 */
public class AotQueryCreator {

	Metamodel metamodel;

	public AotQueryCreator(Metamodel metamodel) {
		this.metamodel = metamodel;
	}

	AotStringQuery createQuery(PartTree partTree, ReturnedType returnedType,
			AotRepositoryMethodGenerationContext context) {

		ParametersSource parametersSource = ParametersSource.of(context.getRepositoryInformation(), context.getMethod());
		JpaParameters parameters = new JpaParameters(parametersSource);
		ParameterMetadataProvider metadataProvider = new ParameterMetadataProvider(parameters, EscapeCharacter.DEFAULT,
				JpqlQueryTemplates.UPPER);

		JpaQueryCreator queryCreator = new JpaQueryCreator(partTree, returnedType, metadataProvider,
				JpqlQueryTemplates.UPPER, metamodel);
		AotStringQuery query = AotStringQuery.bindable(queryCreator.createQuery(), metadataProvider.getBindings());

		if (partTree.isLimiting()) {
			query.setLimit(partTree.getResultLimit());
		}
		query.setCountQuery(context.annotationValue(Query.class, "countQuery"));
		return query;
	}

}
