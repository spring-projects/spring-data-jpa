/*
 * Copyright 2024-2025 the original author or authors.
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
package org.springframework.data.jpa.repository.query;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Tuple;
import jakarta.persistence.metamodel.Metamodel;

import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.FieldSource;
import org.junit.jupiter.params.provider.ValueSource;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.ScrollPosition;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.support.JpqlQueryTemplates;
import org.springframework.data.jpa.util.TestMetaModel;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.projection.SpelAwareProxyProjectionFactory;
import org.springframework.data.repository.query.ParameterAccessor;
import org.springframework.data.repository.query.ReturnedType;
import org.springframework.data.repository.query.parser.PartTree;
import org.springframework.data.util.Lazy;

/**
 * Unit tests for {@link JpaQueryCreator}.
 *
 * @author Christoph Strobl
 */
class JpaQueryCreatorTests {

	private static final TestMetaModel ORDER = TestMetaModel.hibernateModel(Order.class, LineItem.class, Product.class);
	private static final TestMetaModel PERSON = TestMetaModel.hibernateModel(Person.class);

	static List<JpqlQueryTemplates> ignoreCaseTemplates = List.of(JpqlQueryTemplates.LOWER, JpqlQueryTemplates.UPPER);

	@Test // GH-3588
	void simpleProperty() {

		queryCreator(ORDER) //
				.forTree(Order.class, "findOrderByCountry") //
				.withParameters("AT") //
				.as(QueryCreatorTester::create) //
				.expectJpql("SELECT o FROM %s o WHERE o.country = ?1", Order.class.getName()) //
				.validateQuery();
	}

	@Test // GH-3588
	void simpleNullProperty() {

		queryCreator(ORDER) //
				.forTree(Order.class, "findOrderByCountry") //
				.withParameterTypes(String.class) //
				.as(QueryCreatorTester::create) //
				.expectJpql("SELECT o FROM %s o WHERE o.country IS NULL", Order.class.getName()) //
				.validateQuery();
	}

	@Test // GH-3588
	void negatingSimpleProperty() {

		queryCreator(ORDER) //
				.forTree(Order.class, "findOrderByCountryNot") //
				.withParameters("US") //
				.as(QueryCreatorTester::create) //
				.expectJpql("SELECT o FROM %s o WHERE o.country != ?1", Order.class.getName()) //
				.validateQuery();
	}

	@Test // GH-3588
	void negatingSimpleNullProperty() {

		queryCreator(ORDER) //
				.forTree(Order.class, "findOrderByCountryIsNot") //
				.withParameterTypes(String.class) //
				.as(QueryCreatorTester::create) //
				.expectJpql("SELECT o FROM %s o WHERE o.country IS NOT NULL", Order.class.getName()) //
				.validateQuery();
	}

	@Test // GH-3588
	void simpleAnd() {

		queryCreator(ORDER) //
				.forTree(Order.class, "findOrderByCountryAndDate") //
				.withParameters("GB", new Date()) //
				.as(QueryCreatorTester::create) //
				.expectJpql("SELECT o FROM %s o WHERE o.country = ?1 AND o.date = ?2", Order.class.getName()) //
				.validateQuery();
	}

	@Test // GH-3588
	void simpleOr() {

		queryCreator(ORDER) //
				.forTree(Order.class, "findOrderByCountryOrDate") //
				.withParameters("BE", new Date()) //
				.as(QueryCreatorTester::create) //
				.expectJpql("SELECT o FROM %s o WHERE o.country = ?1 OR o.date = ?2", Order.class.getName()) //
				.validateQuery();
	}

	@Test // GH-3588
	void simpleAndOr() {

		queryCreator(ORDER) //
				.forTree(Order.class, "findOrderByCountryAndDateOrCompleted") //
				.withParameters("IT", new Date(), Boolean.FALSE) //
				.as(QueryCreatorTester::create) //
				.expectJpql("SELECT o FROM %s o WHERE o.country = ?1 AND o.date = ?2 OR o.completed = ?3",
						Order.class.getName()) //
				.validateQuery();
	}

	@Test // GH-3588
	void distinct() {

		queryCreator(ORDER) //
				.forTree(Order.class, "findDistinctOrderByCountry") //
				.withParameters("AU") //
				.as(QueryCreatorTester::create) //
				.expectJpql("SELECT DISTINCT o FROM %s o WHERE o.country = ?1", Order.class.getName()) //
				.validateQuery();
	}

	@Test // GH-3588
	void count() {

		queryCreator(ORDER) //
				.forTree(Order.class, "countOrderByCountry") //
				.returing(Long.class) //
				.withParameters("AU") //
				.as(QueryCreatorTester::create) //
				.expectJpql("SELECT COUNT(o) FROM %s o WHERE o.country = ?1", Order.class.getName()) //
				.validateQuery();
	}

	@Test // GH-3588
	void countWithJoins() {

		queryCreator(ORDER) //
				.forTree(Order.class, "countOrderByLineItemsQuantityGreaterThan") //
				.returing(Long.class) //
				.withParameterTypes(Integer.class) //
				.as(QueryCreatorTester::create) //
				.expectJpql("SELECT COUNT(o) FROM %s o LEFT JOIN o.lineItems l WHERE l.quantity > ?1", Order.class.getName()) //
				.validateQuery();
	}

	@Test // GH-3588
	void countDistinct() {

		queryCreator(ORDER) //
				.forTree(Order.class, "countDistinctOrderByCountry") //
				.returing(Long.class) //
				.withParameters("AU") //
				.as(QueryCreatorTester::create) //
				.expectJpql("SELECT COUNT(DISTINCT o) FROM %s o WHERE o.country = ?1", Order.class.getName()) //
				.validateQuery();
	}

	@ParameterizedTest // GH-3588
	@FieldSource("ignoreCaseTemplates")
	void simplePropertyIgnoreCase(JpqlQueryTemplates ingnoreCaseTemplate) {

		queryCreator(ORDER) //
				.forTree(Order.class, "findOrderByCountryIgnoreCase") //
				.ingnoreCaseAs(ingnoreCaseTemplate) //
				.withParameters("BB") //
				.as(QueryCreatorTester::create) //
				.expectJpql("SELECT o FROM %s o WHERE %s(o.country) = %s(?1)", Order.class.getName(),
						ingnoreCaseTemplate.getIgnoreCaseOperator(), ingnoreCaseTemplate.getIgnoreCaseOperator()) //
				.validateQuery();
	}

	@ParameterizedTest // GH-3588
	@FieldSource("ignoreCaseTemplates")
	void simplePropertyAllIgnoreCase(JpqlQueryTemplates ingnoreCaseTemplate) {

		queryCreator(ORDER) //
				.forTree(Product.class, "findProductByNameAndProductTypeAllIgnoreCase") //
				.ingnoreCaseAs(ingnoreCaseTemplate) //
				.withParameters("spring", "data") //
				.as(QueryCreatorTester::create) //
				.expectJpql("SELECT p FROM %s p WHERE %s(p.name) = %s(?1) AND %s(p.productType) = %s(?2)",
						Product.class.getName(), ingnoreCaseTemplate.getIgnoreCaseOperator(),
						ingnoreCaseTemplate.getIgnoreCaseOperator(), ingnoreCaseTemplate.getIgnoreCaseOperator(),
						ingnoreCaseTemplate.getIgnoreCaseOperator()) //
				.validateQuery();
	}

	@ParameterizedTest // GH-3588
	@FieldSource("ignoreCaseTemplates")
	void simplePropertyMixedCase(JpqlQueryTemplates ingnoreCaseTemplate) {

		queryCreator(ORDER) //
				.forTree(Product.class, "findProductByNameAndProductTypeIgnoreCase") //
				.ingnoreCaseAs(ingnoreCaseTemplate) //
				.withParameters("spring", "data") //
				.as(QueryCreatorTester::create) //
				.expectJpql("SELECT p FROM %s p WHERE p.name = ?1 AND %s(p.productType) = %s(?2)", Product.class.getName(),
						ingnoreCaseTemplate.getIgnoreCaseOperator(), ingnoreCaseTemplate.getIgnoreCaseOperator(),
						ingnoreCaseTemplate.getIgnoreCaseOperator()) //
				.validateQuery();
	}

	@Test // GH-3588
	void lessThan() {

		queryCreator(ORDER) //
				.forTree(Order.class, "findOrderByDateLessThan") //
				.withParameterTypes(Date.class) //
				.as(QueryCreatorTester::create) //
				.expectJpql("SELECT o FROM %s o WHERE o.date < ?1", Order.class.getName()) //
				.validateQuery();
	}

	@Test // GH-3588
	void lessThanEqual() {

		queryCreator(ORDER) //
				.forTree(Order.class, "findOrderByDateLessThanEqual") //
				.withParameterTypes(Date.class) //
				.as(QueryCreatorTester::create) //
				.expectJpql("SELECT o FROM %s o WHERE o.date <= ?1", Order.class.getName()) //
				.validateQuery();
	}

	@Test // GH-3588
	void greaterThan() {

		queryCreator(ORDER) //
				.forTree(Order.class, "findOrderByDateGreaterThan") //
				.withParameterTypes(Date.class) //
				.as(QueryCreatorTester::create) //
				.expectJpql("SELECT o FROM %s o WHERE o.date > ?1", Order.class.getName()) //
				.validateQuery();
	}

	@Test // GH-3588
	void before() {

		queryCreator(ORDER) //
				.forTree(Order.class, "findOrderByDateBefore") //
				.withParameterTypes(Date.class) //
				.as(QueryCreatorTester::create) //
				.expectJpql("SELECT o FROM %s o WHERE o.date < ?1", Order.class.getName()) //
				.validateQuery();
	}

	@Test // GH-3588
	void after() {

		queryCreator(ORDER) //
				.forTree(Order.class, "findOrderByDateAfter") //
				.withParameterTypes(Date.class) //
				.as(QueryCreatorTester::create) //
				.expectJpql("SELECT o FROM %s o WHERE o.date > ?1", Order.class.getName()) //
				.validateQuery();
	}

	@Test // GH-3588
	void between() {

		queryCreator(ORDER) //
				.forTree(Order.class, "findOrderByDateBetween") //
				.withParameterTypes(Date.class, Date.class) //
				.as(QueryCreatorTester::create) //
				.expectJpql("SELECT o FROM %s o WHERE o.date BETWEEN ?1 AND ?2", Order.class.getName()) //
				.validateQuery();
	}

	@Test // GH-3588
	void isNull() {

		queryCreator(ORDER) //
				.forTree(Order.class, "findOrderByDateIsNull") //
				.as(QueryCreatorTester::create) //
				.expectJpql("SELECT o FROM %s o WHERE o.date IS NULL", Order.class.getName()) //
				.validateQuery();
	}

	@Test // GH-3588
	void isNotNull() {

		queryCreator(ORDER) //
				.forTree(Order.class, "findOrderByDateIsNotNull") //
				.as(QueryCreatorTester::create) //
				.expectJpql("SELECT o FROM %s o WHERE o.date IS NOT NULL", Order.class.getName()) //
				.validateQuery();
	}

	@ParameterizedTest // GH-3588
	@ValueSource(strings = { "", "spring", "%spring", "spring%", "%spring%" })
	void like(String parameterValue) {

		queryCreator(ORDER) //
				.forTree(Product.class, "findProductByNameLike") //
				.withParameters(parameterValue) //
				.as(QueryCreatorTester::create) //
				.expectJpql("SELECT p FROM %s p WHERE p.name LIKE ?1 ESCAPE '\\'", Product.class.getName()) //
				.expectPlaceholderValue("?1", parameterValue) //
				.validateQuery();
	}

	@Test // GH-3588
	void containingString() {

		queryCreator(ORDER) //
				.forTree(Product.class, "findProductByNameContaining") //
				.withParameters("spring") //
				.as(QueryCreatorTester::create) //
				.expectJpql("SELECT p FROM %s p WHERE p.name LIKE ?1 ESCAPE '\\'", Product.class.getName()) //
				.expectPlaceholderValue("?1", "%spring%") //
				.validateQuery();
	}

	@Test // GH-3588
	void notContainingString() {

		queryCreator(ORDER) //
				.forTree(Product.class, "findProductByNameNotContaining") //
				.withParameters("spring") //
				.as(QueryCreatorTester::create) //
				.expectJpql("SELECT p FROM %s p WHERE p.name NOT LIKE ?1 ESCAPE '\\'", Product.class.getName()) //
				.expectPlaceholderValue("?1", "%spring%") //
				.validateQuery();
	}

	@Test // GH-3588
	void in() {

		queryCreator(ORDER) //
				.forTree(Product.class, "findProductByNameIn") //
				.withParameters(List.of("spring", "data")) //
				.as(QueryCreatorTester::create) //
				.expectJpql("SELECT p FROM %s p WHERE p.name IN (?1)", Product.class.getName()) //
				.expectPlaceholderValue("?1", List.of("spring", "data")) //
				.validateQuery();
	}

	@Test // GH-3588
	void notIn() {

		queryCreator(ORDER) //
				.forTree(Product.class, "findProductByNameNotIn") //
				.withParameters(List.of("spring", "data")) //
				.as(QueryCreatorTester::create) //
				.expectJpql("SELECT p FROM %s p WHERE p.name NOT IN (?1)", Product.class.getName()) //
				.expectPlaceholderValue("?1", List.of("spring", "data")) //
				.validateQuery();
	}

	@Test // GH-3588
	void containingSingleEntryElementCollection() {

		queryCreator(ORDER) //
				.forTree(Product.class, "findProductByCategoriesContaining") //
				.withParameterTypes(String.class) //
				.as(QueryCreatorTester::create) //
				.expectJpql("SELECT p FROM %s p WHERE ?1 MEMBER OF p.categories", Product.class.getName()) //
				.validateQuery();
	}

	@Test // GH-3588
	void notContainingSingleEntryElementCollection() {

		queryCreator(ORDER) //
				.forTree(Product.class, "findProductByCategoriesNotContaining") //
				.withParameterTypes(String.class) //
				.as(QueryCreatorTester::create) //
				.expectJpql("SELECT p FROM %s p WHERE ?1 NOT MEMBER OF p.categories", Product.class.getName()) //
				.validateQuery();
	}

	@ParameterizedTest // GH-3588
	@FieldSource("ignoreCaseTemplates")
	void likeWithIgnoreCase(JpqlQueryTemplates ingnoreCaseTemplate) {

		queryCreator(ORDER) //
				.forTree(Product.class, "findProductByNameLikeIgnoreCase") //
				.ingnoreCaseAs(ingnoreCaseTemplate) //
				.withParameters("%spring%") //
				.as(QueryCreatorTester::create) //
				.expectJpql("SELECT p FROM %s p WHERE %s(p.name) LIKE %s(?1) ESCAPE '\\'", Product.class.getName(),
						ingnoreCaseTemplate.getIgnoreCaseOperator(), ingnoreCaseTemplate.getIgnoreCaseOperator()) //
				.expectPlaceholderValue("?1", "%spring%") //
				.validateQuery();
	}

	@ParameterizedTest // GH-3588
	@ValueSource(strings = { "", "spring", "%spring", "spring%", "%spring%" })
	void notLike(String parameterValue) {

		queryCreator(ORDER) //
				.forTree(Product.class, "findProductByNameNotLike") //
				.withParameters(parameterValue) //
				.as(QueryCreatorTester::create) //
				.expectJpql("SELECT p FROM %s p WHERE p.name NOT LIKE ?1 ESCAPE '\\'", Product.class.getName()) //
				.expectPlaceholderValue("?1", parameterValue) //
				.validateQuery();
	}

	@ParameterizedTest // GH-3588
	@FieldSource("ignoreCaseTemplates")
	void notLikeWithIgnoreCase(JpqlQueryTemplates ingnoreCaseTemplate) {

		queryCreator(ORDER) //
				.forTree(Product.class, "findProductByNameNotLikeIgnoreCase") //
				.ingnoreCaseAs(ingnoreCaseTemplate) //
				.withParameters("%spring%") //
				.as(QueryCreatorTester::create) //
				.expectJpql("SELECT p FROM %s p WHERE %s(p.name) NOT LIKE %s(?1) ESCAPE '\\'", Product.class.getName(),
						ingnoreCaseTemplate.getIgnoreCaseOperator(), ingnoreCaseTemplate.getIgnoreCaseOperator()) //
				.expectPlaceholderValue("?1", "%spring%") //
				.validateQuery();
	}

	@Test // GH-3588
	void startingWith() {

		queryCreator(ORDER) //
				.forTree(Product.class, "findProductByNameStartingWith") //
				.withParameters("spring") //
				.as(QueryCreatorTester::create) //
				.expectJpql("SELECT p FROM %s p WHERE p.name LIKE ?1 ESCAPE '\\'", Product.class.getName()) //
				.expectPlaceholderValue("?1", "spring%") //
				.validateQuery();
	}

	@ParameterizedTest // GH-3588
	@FieldSource("ignoreCaseTemplates")
	void startingWithIgnoreCase(JpqlQueryTemplates ingnoreCaseTemplate) {

		queryCreator(ORDER) //
				.forTree(Product.class, "findProductByNameStartingWithIgnoreCase") //
				.ingnoreCaseAs(ingnoreCaseTemplate) //
				.withParameters("spring") //
				.as(QueryCreatorTester::create) //
				.expectJpql("SELECT p FROM %s p WHERE %s(p.name) LIKE %s(?1) ESCAPE '\\'", Product.class.getName(),
						ingnoreCaseTemplate.getIgnoreCaseOperator(), ingnoreCaseTemplate.getIgnoreCaseOperator()) //
				.expectPlaceholderValue("?1", "spring%") //
				.validateQuery();
	}

	@Test // GH-3588
	void endingWith() {

		queryCreator(ORDER) //
				.forTree(Product.class, "findProductByNameEndingWith") //
				.withParameters("spring") //
				.as(QueryCreatorTester::create) //
				.expectJpql("SELECT p FROM %s p WHERE p.name LIKE ?1 ESCAPE '\\'", Product.class.getName()) //
				.expectPlaceholderValue("?1", "%spring") //
				.validateQuery();
	}

	@ParameterizedTest // GH-3588
	@FieldSource("ignoreCaseTemplates")
	void endingWithIgnoreCase(JpqlQueryTemplates ingnoreCaseTemplate) {

		queryCreator(ORDER) //
				.forTree(Product.class, "findProductByNameEndingWithIgnoreCase") //
				.ingnoreCaseAs(ingnoreCaseTemplate) //
				.withParameters("spring") //
				.as(QueryCreatorTester::create) //
				.expectJpql("SELECT p FROM %s p WHERE %s(p.name) LIKE %s(?1) ESCAPE '\\'", Product.class.getName(),
						ingnoreCaseTemplate.getIgnoreCaseOperator(), ingnoreCaseTemplate.getIgnoreCaseOperator()) //
				.expectPlaceholderValue("?1", "%spring") //
				.validateQuery();
	}

	@Test // GH-3588
	void greaterThanEqual() {

		queryCreator(ORDER) //
				.forTree(Order.class, "findOrderByDateGreaterThanEqual") //
				.withParameterTypes(Date.class) //
				.as(QueryCreatorTester::create) //
				.expectJpql("SELECT o FROM %s o WHERE o.date >= ?1", Order.class.getName()) //
				.validateQuery();
	}

	@Test // GH-3588
	void isTrue() {

		queryCreator(ORDER) //
				.forTree(Order.class, "findOrderByCompletedIsTrue") //
				.as(QueryCreatorTester::create) //
				.expectJpql("SELECT o FROM %s o WHERE o.completed = TRUE", Order.class.getName()) //
				.validateQuery();
	}

	@Test // GH-3588
	void isFalse() {

		queryCreator(ORDER) //
				.forTree(Order.class, "findOrderByCompletedIsFalse") //
				.as(QueryCreatorTester::create) //
				.expectJpql("SELECT o FROM %s o WHERE o.completed = FALSE", Order.class.getName()) //
				.validateQuery();
	}

	@Test // GH-3588
	void empty() {

		queryCreator(ORDER) //
				.forTree(Order.class, "findOrderByLineItemsEmpty") //
				.as(QueryCreatorTester::create) //
				.expectJpql("SELECT o FROM %s o WHERE o.lineItems IS EMPTY", Order.class.getName()) //
				.validateQuery();
	}

	@Test // GH-3588
	void notEmpty() {

		queryCreator(ORDER) //
				.forTree(Order.class, "findOrderByLineItemsNotEmpty") //
				.as(QueryCreatorTester::create) //
				.expectJpql("SELECT o FROM %s o WHERE o.lineItems IS NOT EMPTY", Order.class.getName()) //
				.validateQuery();
	}

	@Test // GH-3588
	void sortBySingle() {

		queryCreator(ORDER) //
				.forTree(Order.class, "findOrderByCountryOrderByDate") //
				.withParameters("CA") //
				.as(QueryCreatorTester::create) //
				.expectJpql("SELECT o FROM %s o WHERE o.country = ?1 ORDER BY o.date asc", Order.class.getName()) //
				.validateQuery();
	}

	@Test // GH-3588
	void sortByMulti() {

		queryCreator(ORDER) //
				.forTree(Order.class, "findOrderByOrderByCountryAscDateDesc") //
				.withParameters() //
				.as(QueryCreatorTester::create) //
				.expectJpql("SELECT o FROM %s o ORDER BY o.country asc, o.date desc", Order.class.getName()) //
				.validateQuery();
	}

	@Disabled("should we support this?")
	@ParameterizedTest // GH-3588
	@FieldSource("ignoreCaseTemplates")
	void sortBySingleIngoreCase(JpqlQueryTemplates ingoreCase) {

		String jpql = queryCreator(ORDER) //
				.forTree(Order.class, "findOrderByOrderByCountryAscAllIgnoreCase") //
				.render();

		assertThat(jpql).isEqualTo("SELECT o FROM %s o ORDER BY %s(o.date) asc", Order.class.getName(),
				ingoreCase.getIgnoreCaseOperator());
	}

	@Test // GH-3588
	void matchSimpleJoin() {

		queryCreator(ORDER) //
				.forTree(Order.class, "findOrderByLineItemsQuantityGreaterThan") //
				.withParameterTypes(Integer.class) //
				.as(QueryCreatorTester::create) //
				.expectJpql("SELECT o FROM %s o LEFT JOIN o.lineItems l WHERE l.quantity > ?1", Order.class.getName()) //
				.validateQuery();
	}

	@Test // GH-3588
	void matchSimpleNestedJoin() {

		queryCreator(ORDER) //
				.forTree(Order.class, "findOrderByLineItemsProductNameIs") //
				.withParameters("spring") //
				.as(QueryCreatorTester::create) //
				.expectJpql("SELECT o FROM %s o LEFT JOIN o.lineItems l LEFT JOIN l.product p WHERE p.name = ?1",
						Order.class.getName()) //
				.validateQuery();
	}

	@Test // GH-3588
	void matchMultiOnNestedJoin() {

		queryCreator(ORDER) //
				.forTree(Order.class, "findOrderByLineItemsQuantityGreaterThanAndLineItemsProductNameIs") //
				.withParameters(10, "spring") //
				.as(QueryCreatorTester::create) //
				.expectJpql(
						"SELECT o FROM %s o LEFT JOIN o.lineItems l LEFT JOIN l.product p WHERE l.quantity > ?1 AND p.name = ?2",
						Order.class.getName()) //
				.validateQuery();
	}

	@Test // GH-3588
	void matchSameEntityMultipleTimes() {

		queryCreator(ORDER) //
				.forTree(Order.class, "findOrderByLineItemsProductNameIsAndLineItemsProductNameIsNot") //
				.withParameters("spring", "sukrauq") //
				.as(QueryCreatorTester::create) //
				.expectJpql(
						"SELECT o FROM %s o LEFT JOIN o.lineItems l LEFT JOIN l.product p WHERE p.name = ?1 AND p.name != ?2",
						Order.class.getName()) //
				.validateQuery();
	}

	@Test // GH-3588
	void matchSameEntityMultipleTimesViaDifferentProperties() {

		queryCreator(ORDER) //
				.forTree(Order.class, "findOrderByLineItemsProductNameIsAndLineItemsProduct2NameIs") //
				.withParameters(10, "spring") //
				.as(QueryCreatorTester::create) //
				.expectJpql(
						"SELECT o FROM %s o LEFT JOIN o.lineItems l LEFT JOIN l.product p LEFT JOIN l.product2 join_0 WHERE p.name = ?1 AND join_0.name = ?2",
						Order.class.getName()) //
				.validateQuery();
	}

	@Test // GH-3588
	void dtoProjection() {

		queryCreator(ORDER) //
				.forTree(Product.class, "findProjectionByNameIs") //
				.returing(DtoProductProjection.class) //
				.withParameters("spring") //
				.as(QueryCreatorTester::create) //
				.expectJpql("SELECT new %s(p.name, p.productType) FROM %s p WHERE p.name = ?1",
						DtoProductProjection.class.getName(), Product.class.getName()) //
				.validateQuery();
	}

	@Test // GH-3588
	void interfaceProjection() {

		queryCreator(ORDER) //
				.forTree(Product.class, "findProjectionByNameIs") //
				.returing(InterfaceProductProjection.class) //
				.withParameters("spring") //
				.as(QueryCreatorTester::create) //
				.expectJpql("SELECT p.name name, p.productType productType FROM %s p WHERE p.name = ?1",
						Product.class.getName()) //
				.validateQuery();
	}

	@ParameterizedTest // GH-3588
	@ValueSource(classes = { Tuple.class, Map.class })
	void tupleProjection(Class<?> resultType) {

		queryCreator(PERSON) //
				.forTree(Person.class, "findProjectionByFirstnameIs") //
				.returing(resultType) //
				.withParameters("chris") //
				.as(QueryCreatorTester::create) //
				.expectJpql("SELECT p.id id, p.firstname firstname, p.lastname lastname FROM %s p WHERE p.firstname = ?1",
						Person.class.getName()) //
				.validateQuery();
	}

	@ParameterizedTest // GH-3588
	@ValueSource(classes = { Long.class, List.class, Person.class })
	void delete(Class<?> resultType) {

		queryCreator(PERSON) //
				.forTree(Person.class, "deletePersonByFirstname") //
				.returing(resultType) //
				.withParameters("chris") //
				.as(QueryCreatorTester::create) //
				.expectJpql("SELECT p FROM %s p WHERE p.firstname = ?1", Person.class.getName()) //
				.validateQuery();
	}

	@Test // GH-3588
	void exists() {

		queryCreator(PERSON) //
				.forTree(Person.class, "existsPersonByFirstname") //
				.returing(Long.class).withParameters("chris") //
				.as(QueryCreatorTester::create) //
				.expectJpql("SELECT p.id id FROM %s p WHERE p.firstname = ?1", Person.class.getName()) //
				.validateQuery();
	}

	QueryCreatorBuilder queryCreator(Metamodel metamodel) {
		return new DefaultCreatorBuilder(metamodel);
	}

	JpaQueryCreator queryCreator(PartTree tree, ReturnedType returnedType, Metamodel metamodel, Object... arguments) {
		return queryCreator(tree, returnedType, metamodel, JpqlQueryTemplates.UPPER, arguments);
	}

	JpaQueryCreator queryCreator(PartTree tree, ReturnedType returnedType, Metamodel metamodel,
			JpqlQueryTemplates templates, Object... arguments) {

		ParameterMetadataProvider parameterMetadataProvider = new ParameterMetadataProvider(
				StubJpaParameterParameterAccessor.accessor(arguments), EscapeCharacter.DEFAULT, templates);
		return queryCreator(tree, returnedType, metamodel, templates, parameterMetadataProvider);
	}

	JpaQueryCreator queryCreator(PartTree tree, ReturnedType returnedType, Metamodel metamodel,
			JpqlQueryTemplates templates, Class<?>... argumentTypes) {

		ParameterMetadataProvider parameterMetadataProvider = new ParameterMetadataProvider(
				StubJpaParameterParameterAccessor.accessor(argumentTypes), EscapeCharacter.DEFAULT, templates);
		return queryCreator(tree, returnedType, metamodel, templates, parameterMetadataProvider);
	}

	JpaQueryCreator queryCreator(PartTree tree, ReturnedType returnedType, Metamodel metamodel,
			JpqlQueryTemplates templates, JpaParametersParameterAccessor parameterAccessor) {

		EntityManager entityManager = mock(EntityManager.class);
		when(entityManager.getMetamodel()).thenReturn(metamodel);

		ParameterMetadataProvider parameterMetadataProvider = new ParameterMetadataProvider(parameterAccessor,
				EscapeCharacter.DEFAULT, templates);
		return new JpaQueryCreator(tree, returnedType, parameterMetadataProvider, templates, entityManager);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private JpaParametersParameterAccessor accessor(Class<?>... argumentTypes) {

		return StubJpaParameterParameterAccessor.accessor(argumentTypes);
	}

	@jakarta.persistence.Entity
	static class Order {

		@Id Long id;
		Date date;
		String country;
		Boolean completed;

		@OneToMany List<LineItem> lineItems;
	}

	@jakarta.persistence.Entity
	static class LineItem {

		@Id Long id;

		@ManyToOne Product product;
		@ManyToOne Product product2;
		int quantity;

	}

	@jakarta.persistence.Entity
	static class Person {
		@Id Long id;
		String firstname;
		String lastname;
	}

	@jakarta.persistence.Entity
	static class Product {

		@Id Long id;

		String name;
		String productType;

		@ElementCollection List<String> categories;
	}

	static class DtoProductProjection {

		String name;
		String productType;

		DtoProductProjection(String name, String productType) {
			this.name = name;
			this.productType = productType;
		}
	}

	interface InterfaceProductProjection {
		String getName();

		String getProductType();
	}

	static class QueryCreatorTester {

		QueryCreatorBuilder builder;
		Lazy<String> jpql;

		private QueryCreatorTester(QueryCreatorBuilder builder) {
			this.builder = builder;
			this.jpql = Lazy.of(builder::render);
		}

		static QueryCreatorTester create(QueryCreatorBuilder builder) {
			return new QueryCreatorTester(builder);
		}

		QueryCreatorTester expectJpql(String jpql, Object... args) {

			assertThat(this.jpql.get()).isEqualTo(jpql, args);
			return this;
		}

		QueryCreatorTester expectPlaceholderValue(String placeholder, Object value) {
			return expectBindingAt(builder.bindingIndexFor(placeholder), value);
		}

		QueryCreatorTester expectBindingAt(int position, Object value) {

			Object current = builder.bindableParameters().getBindableValue(position - 1);
			assertThat(current).isEqualTo(value);
			return this;
		}

		QueryCreatorTester validateQuery() {

			if (builder instanceof DefaultCreatorBuilder dcb && dcb.metamodel instanceof TestMetaModel tmm) {
				return validateQuery(tmm.entityManager());
			}

			throw new IllegalStateException("No EntityManager found, plase provide one via [verify(EntityManager)]");
		}

		QueryCreatorTester validateQuery(EntityManager entityManager) {

			if (builder instanceof DefaultCreatorBuilder dcb) {
				entityManager.createQuery(this.jpql.get(), dcb.returnedType.getReturnedType());
			} else {
				entityManager.createQuery(this.jpql.get());
			}
			return this;
		}

	}

	interface QueryCreatorBuilder {

		QueryCreatorBuilder returing(ReturnedType returnedType);

		QueryCreatorBuilder forTree(Class<?> root, String querySource);

		QueryCreatorBuilder withParameters(Object... arguments);

		QueryCreatorBuilder withParameterTypes(Class<?>... argumentTypes);

		QueryCreatorBuilder ingnoreCaseAs(JpqlQueryTemplates queryTemplate);

		default <T> T as(Function<QueryCreatorBuilder, T> transformer) {
			return transformer.apply(this);
		}

		default String render() {
			return render(null);
		}

		ParameterAccessor bindableParameters();

		int bindingIndexFor(String placeholder);

		String render(@Nullable Sort sort);

		QueryCreatorBuilder returing(Class<?> type);
	}

	private class DefaultCreatorBuilder implements QueryCreatorBuilder {

		private static final ProjectionFactory PROJECTION_FACTORY = new SpelAwareProxyProjectionFactory();

		private final Metamodel metamodel;
		private ReturnedType returnedType;
		private PartTree partTree;
		private Object[] arguments;
		private Class<?>[] argumentTypes;
		private JpqlQueryTemplates queryTemplates;
		private Lazy<JpaQueryCreator> queryCreator = Lazy.of(this::initJpaQueryCreator);
		private Lazy<JpaParametersParameterAccessor> parameterAccessor = Lazy.of(this::initParameterAccessor);

		public DefaultCreatorBuilder(Metamodel metamodel) {
			this.metamodel = metamodel;
			arguments = new Object[0];
			queryTemplates = JpqlQueryTemplates.UPPER;
		}

		@Override
		public QueryCreatorBuilder returing(ReturnedType returnedType) {
			this.returnedType = returnedType;
			return this;
		}

		@Override
		public QueryCreatorBuilder returing(Class<?> type) {

			if (this.returnedType != null) {
				return returing(ReturnedType.of(type, returnedType.getDomainType(), PROJECTION_FACTORY));
			}

			return returing(ReturnedType.of(type, type, PROJECTION_FACTORY));
		}

		@Override
		public QueryCreatorBuilder forTree(Class<?> root, String querySource) {

			this.partTree = new PartTree(querySource, root);
			if (returnedType == null) {
				returnedType = ReturnedType.of(root, root, PROJECTION_FACTORY);
			}
			return this;
		}

		@Override
		public QueryCreatorBuilder withParameters(Object... arguments) {
			this.arguments = arguments;
			return this;
		}

		@Override
		public QueryCreatorBuilder withParameterTypes(Class<?>... argumentTypes) {
			this.argumentTypes = argumentTypes;
			return this;
		}

		@Override
		public QueryCreatorBuilder ingnoreCaseAs(JpqlQueryTemplates queryTemplate) {
			this.queryTemplates = queryTemplate;
			return this;
		}

		@Override
		public String render(@Nullable Sort sort) {
			return queryCreator.get().createQuery(sort != null ? sort : Sort.unsorted());
		}

		@Override
		public int bindingIndexFor(String placeholder) {

			return queryCreator.get().getBindings().stream().filter(binding -> {

				if (binding.getIdentifier().hasPosition() && placeholder.startsWith("?")) {
					return binding.getPosition() == Integer.parseInt(placeholder.substring(1));
				}

				if (!binding.getIdentifier().hasName()) {
					return false;
				}

				return binding.getIdentifier().getName().equals(placeholder);
			}).findFirst().map(ParameterBinding::getPosition).orElse(-1);
		}

		@Override
		public ParameterAccessor bindableParameters() {

			return new ParameterAccessor() {
				@Override
				public @Nullable ScrollPosition getScrollPosition() {
					return null;
				}

				@Override
				public Pageable getPageable() {
					return null;
				}

				@Override
				public Sort getSort() {
					return null;
				}

				@Override
				public @Nullable Class<?> findDynamicProjection() {
					return null;
				}

				@Override
				public @Nullable Object getBindableValue(int index) {

					ParameterBinding parameterBinding = queryCreator.get().getBindings().get(index);
					return parameterBinding.prepare(parameterAccessor.get().getBindableValue(index));
				}

				@Override
				public boolean hasBindableNullValue() {
					return false;
				}

				@Override
				public Iterator<Object> iterator() {
					return null;
				}
			};

		}

		JpaParametersParameterAccessor initParameterAccessor() {

			if (arguments.length > 0 || argumentTypes == null) {
				return StubJpaParameterParameterAccessor.accessor(arguments);
			}
			return StubJpaParameterParameterAccessor.accessor(argumentTypes);
		}

		JpaQueryCreator initJpaQueryCreator() {

			if (arguments.length > 0 || argumentTypes == null) {
				return queryCreator(partTree, returnedType, metamodel, queryTemplates, parameterAccessor.get());
			}
			return queryCreator(partTree, returnedType, metamodel, queryTemplates, parameterAccessor.get());
		}
	}
}
