/*
 * Copyright 2024 the original author or authors.
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
package org.springframework.data.jpa.domain;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaUpdate;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import java.io.Serializable;
import java.util.Arrays;
import java.util.stream.StreamSupport;

import org.springframework.lang.CheckReturnValue;

import org.jspecify.annotations.Nullable;
import org.springframework.lang.Contract;
import org.springframework.util.Assert;

/**
 * Specification in the sense of Domain Driven Design to handle Criteria Updates.
 * <p>
 * Specifications can be composed into higher order functions from other specifications using
 * {@link #and(UpdateSpecification)}, {@link #or(UpdateSpecification)} or factory methods such as
 * {@link #allOf(Iterable)}. Composition considers whether one or more specifications contribute to the overall
 * predicate by returning a {@link Predicate} or {@literal null}. Specifications returning {@literal null} are
 * considered to not contribute to the overall predicate and their result is not considered in the final predicate.
 *
 * @author Mark Paluch
 * @since 4.0
 */
@FunctionalInterface
public interface UpdateSpecification<T> extends Serializable {

	/**
	 * Simple static factory method to create a specification updating all objects.
	 *
	 * @param <T> the type of the {@link Root} the resulting {@literal UpdateSpecification} operates on.
	 * @return guaranteed to be not {@literal null}.
	 */
	static <T> UpdateSpecification<T> unrestricted() {
		return (root, query, builder) -> null;
	}

	/**
	 * Simple static factory method to add some syntactic sugar around a {@literal UpdateOperation}. For example:
	 *
	 * <pre class="code">
	 * UpdateSpecification&lt;User&gt; updateLastname = UpdateOperation
	 * 		.&lt;User&gt; update((root, update, criteriaBuilder) -> update.set("lastname", "Heisenberg"))
	 * 		.where(userHasFirstname("Walter").and(userHasLastname("White")));
	 *
	 * repository.update(updateLastname);
	 * </pre>
	 *
	 * @param <T> the type of the {@link Root} the resulting {@literal UpdateOperation} operates on.
	 * @param spec must not be {@literal null}.
	 * @return guaranteed to be not {@literal null}.
	 */
	static <T> UpdateOperation<T> update(UpdateOperation<T> spec) {

		Assert.notNull(spec, "UpdateSpecification must not be null");

		return spec;
	}

	/**
	 * Simple static factory method to add some syntactic sugar around a {@literal UpdateSpecification}.
	 *
	 * @param <T> the type of the {@link Root} the resulting {@literal UpdateSpecification} operates on.
	 * @param spec must not be {@literal null}.
	 * @return guaranteed to be not {@literal null}.
	 */
	static <T> UpdateSpecification<T> where(UpdateSpecification<T> spec) {

		Assert.notNull(spec, "UpdateSpecification must not be null");

		return spec;
	}

	/**
	 * Simple static factory method to add some syntactic sugar translating {@link PredicateSpecification} to
	 * {@link UpdateSpecification}.
	 *
	 * @param <T> the type of the {@link Root} the resulting {@literal UpdateSpecification} operates on.
	 * @param spec the {@link PredicateSpecification} to wrap.
	 * @return guaranteed to be not {@literal null}.
	 */
	static <T> UpdateSpecification<T> where(PredicateSpecification<T> spec) {

		Assert.notNull(spec, "PredicateSpecification must not be null");

		return where((root, update, criteriaBuilder) -> spec.toPredicate(root, criteriaBuilder));
	}

	/**
	 * ANDs the given {@link UpdateSpecification} to the current one.
	 *
	 * @param other the other {@link UpdateSpecification}.
	 * @return the conjunction of the specifications.
	 */
	@Contract("_ -> new")
	@CheckReturnValue
	default UpdateSpecification<T> and(UpdateSpecification<T> other) {

		Assert.notNull(other, "Other specification must not be null");

		return SpecificationComposition.composed(this, other, CriteriaBuilder::and);
	}

	/**
	 * ANDs the given {@link UpdateSpecification} to the current one.
	 *
	 * @param other the other {@link PredicateSpecification}.
	 * @return the conjunction of the specifications.
	 */
	@Contract("_ -> new")
	@CheckReturnValue
	default UpdateSpecification<T> and(PredicateSpecification<T> other) {

		Assert.notNull(other, "Other specification must not be null");

		return SpecificationComposition.composed(this, where(other), CriteriaBuilder::and);
	}

	/**
	 * ORs the given specification to the current one.
	 *
	 * @param other the other {@link UpdateSpecification}.
	 * @return the disjunction of the specifications.
	 */
	@Contract("_ -> new")
	@CheckReturnValue
	default UpdateSpecification<T> or(UpdateSpecification<T> other) {

		Assert.notNull(other, "Other specification must not be null");

		return SpecificationComposition.composed(this, other, CriteriaBuilder::or);
	}

	/**
	 * ORs the given specification to the current one.
	 *
	 * @param other the other {@link PredicateSpecification}.
	 * @return the disjunction of the specifications.
	 */
	@Contract("_ -> new")
	@CheckReturnValue
	default UpdateSpecification<T> or(PredicateSpecification<T> other) {

		Assert.notNull(other, "Other specification must not be null");

		return SpecificationComposition.composed(this, where(other), CriteriaBuilder::or);
	}

	/**
	 * Negates the given {@link UpdateSpecification}.
	 *
	 * @param <T> the type of the {@link Root} the resulting {@literal UpdateSpecification} operates on.
	 * @param spec can be {@literal null}.
	 * @return guaranteed to be not {@literal null}.
	 */
	static <T> UpdateSpecification<T> not(UpdateSpecification<T> spec) {

		Assert.notNull(spec, "Specification must not be null");

		return (root, update, builder) -> {

			Predicate predicate = spec.toPredicate(root, update, builder);
			return predicate != null ? builder.not(predicate) : null;
		};
	}

	/**
	 * Applies an AND operation to all the given {@link UpdateSpecification}s. If {@code specifications} is empty, the
	 * resulting {@link UpdateSpecification} will be unrestricted applying to all objects.
	 *
	 * @param specifications the {@link UpdateSpecification}s to compose.
	 * @return the conjunction of the specifications.
	 * @see #and(UpdateSpecification)
	 * @see #allOf(Iterable)
	 */
	@SafeVarargs
	static <T> UpdateSpecification<T> allOf(UpdateSpecification<T>... specifications) {
		return allOf(Arrays.asList(specifications));
	}

	/**
	 * Applies an AND operation to all the given {@link UpdateSpecification}s. If {@code specifications} is empty, the
	 * resulting {@link UpdateSpecification} will be unrestricted applying to all objects.
	 *
	 * @param specifications the {@link UpdateSpecification}s to compose.
	 * @return the conjunction of the specifications.
	 * @see #and(UpdateSpecification)
	 * @see #allOf(UpdateSpecification[])
	 */
	static <T> UpdateSpecification<T> allOf(Iterable<UpdateSpecification<T>> specifications) {

		return StreamSupport.stream(specifications.spliterator(), false) //
				.reduce(UpdateSpecification.unrestricted(), UpdateSpecification::and);
	}

	/**
	 * Applies an OR operation to all the given {@link UpdateSpecification}s. If {@code specifications} is empty, the
	 * resulting {@link UpdateSpecification} will be unrestricted applying to all objects.
	 *
	 * @param specifications the {@link UpdateSpecification}s to compose.
	 * @return the disjunction of the specifications.
	 * @see #or(UpdateSpecification)
	 * @see #anyOf(Iterable)
	 */
	@SafeVarargs
	static <T> UpdateSpecification<T> anyOf(UpdateSpecification<T>... specifications) {
		return anyOf(Arrays.asList(specifications));
	}

	/**
	 * Applies an OR operation to all the given {@link UpdateSpecification}s. If {@code specifications} is empty, the
	 * resulting {@link UpdateSpecification} will be unrestricted applying to all objects.
	 *
	 * @param specifications the {@link UpdateSpecification}s to compose.
	 * @return the disjunction of the specifications.
	 * @see #or(UpdateSpecification)
	 * @see #anyOf(Iterable)
	 */
	static <T> UpdateSpecification<T> anyOf(Iterable<UpdateSpecification<T>> specifications) {

		return StreamSupport.stream(specifications.spliterator(), false) //
				.reduce(UpdateSpecification.unrestricted(), UpdateSpecification::or);
	}

	/**
	 * Creates a WHERE clause for a query of the referenced entity in form of a {@link Predicate} for the given
	 * {@link Root} and {@link CriteriaUpdate}.
	 *
	 * @param root must not be {@literal null}.
	 * @param update the update criteria.
	 * @param criteriaBuilder must not be {@literal null}.
	 * @return a {@link Predicate}, may be {@literal null}.
	 */
	@Nullable
	Predicate toPredicate(Root<T> root, CriteriaUpdate<T> update, CriteriaBuilder criteriaBuilder);

	/**
	 * Simplified extension to {@link UpdateSpecification} that only considers the {@code UPDATE} part without specifying
	 * a predicate. This is useful to separate concerns for reusable specifications, for example:
	 *
	 * <pre class="code">
	 * UpdateSpecification&lt;User&gt; updateLastname = UpdateSpecification
	 * 		.&lt;User&gt; update((root, update, criteriaBuilder) -> update.set("lastname", "Heisenberg"))
	 * 		.where(userHasFirstname("Walter").and(userHasLastname("White")));
	 *
	 * repository.update(updateLastname);
	 * </pre>
	 *
	 * @param <T>
	 */
	@FunctionalInterface
	interface UpdateOperation<T> {

		/**
		 * ANDs the given {@link UpdateOperation} to the current one.
		 *
		 * @param other the other {@link UpdateOperation}.
		 * @return the conjunction of the specifications.
		 */
		@Contract("_ -> new")
		@CheckReturnValue
		default UpdateOperation<T> and(UpdateOperation<T> other) {

			Assert.notNull(other, "Other UpdateOperation must not be null");

			return (root, update, criteriaBuilder) -> {
				this.apply(root, update, criteriaBuilder);
				other.apply(root, update, criteriaBuilder);
			};
		}

		/**
		 * Creates a {@link UpdateSpecification} from this and the given {@link UpdateSpecification}.
		 *
		 * @param specification the {@link PredicateSpecification}.
		 * @return the conjunction of the specifications.
		 */
		@Contract("_ -> new")
		@CheckReturnValue
		default UpdateSpecification<T> where(PredicateSpecification<T> specification) {

			Assert.notNull(specification, "PredicateSpecification must not be null");

			return (root, update, criteriaBuilder) -> {
				this.apply(root, update, criteriaBuilder);
				return specification.toPredicate(root, criteriaBuilder);
			};
		}

		/**
		 * Creates a {@link UpdateSpecification} from this and the given {@link UpdateSpecification}.
		 *
		 * @param specification the {@link UpdateSpecification}.
		 * @return the conjunction of the specifications.
		 */
		@Contract("_ -> new")
		@CheckReturnValue
		default UpdateSpecification<T> where(UpdateSpecification<T> specification) {

			Assert.notNull(specification, "UpdateSpecification must not be null");

			return (root, update, criteriaBuilder) -> {
				this.apply(root, update, criteriaBuilder);
				return specification.toPredicate(root, update, criteriaBuilder);
			};
		}

		/**
		 * Accept the given {@link Root} and {@link CriteriaUpdate} to apply the update operation.
		 *
		 * @param root must not be {@literal null}.
		 * @param update the update criteria.
		 * @param criteriaBuilder must not be {@literal null}.
		 */
		void apply(Root<T> root, CriteriaUpdate<T> update, CriteriaBuilder criteriaBuilder);

	}

}
