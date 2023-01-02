/*
 * Copyright 2016-2023 the original author or authors.
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
package org.springframework.data.jpa.convert;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.data.domain.Example.*;

import jakarta.persistence.Id;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.metamodel.Attribute.PersistentAttributeType;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.ManagedType;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.Type;

import java.lang.reflect.Member;
import java.util.LinkedHashSet;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.domain.ExampleMatcher.GenericPropertyMatcher;
import org.springframework.data.jpa.repository.query.EscapeCharacter;
import org.springframework.util.ObjectUtils;

/**
 * Unit tests for {@link QueryByExamplePredicateBuilder}.
 *
 * @author Christoph Strobl
 * @author Mark Paluch
 * @author Oliver Gierke
 * @author Jens Schauder
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@SuppressWarnings({ "rawtypes", "unchecked" })
class QueryByExamplePredicateBuilderUnitTests {

	@Mock CriteriaBuilder cb;
	@Mock Root root;
	@Mock EntityType<Person> personEntityType;
	@Mock EntityType<Skill> skillEntityType;
	@Mock Expression expressionMock;
	@Mock Predicate truePredicate, dummyPredicate, andPredicate, orPredicate;
	@Mock Path dummyPath;
	@Mock Join from;

	private Set<SingularAttribute<? super Person, ?>> personEntityAttribtues;
	private Set<SingularAttribute<? super Skill, ?>> skillEntityAttribtues;

	private SingularAttribute<? super Person, Long> personIdAttribute;
	private SingularAttribute<? super Person, String> personFirstnameAttribute;
	private SingularAttribute<? super Person, Long> personAgeAttribute;
	private SingularAttribute<? super Person, Person> personFatherAttribute;
	private SingularAttribute<? super Person, Skill> personSkillAttribute;
	private SingularAttribute<? super Person, Address> personAddressAttribute;
	private SingularAttribute<? super Skill, String> skillNameAttribute;
	private SingularAttribute<? super Skill, Skill> skillNestedAttribute;

	@BeforeEach
	void setUp() {

		personIdAttribute = new SingularAttributeStub<>("id", PersistentAttributeType.BASIC, Long.class);
		personFirstnameAttribute = new SingularAttributeStub<>("firstname", PersistentAttributeType.BASIC, String.class);
		personAgeAttribute = new SingularAttributeStub<>("age", PersistentAttributeType.BASIC, Long.class);
		personFatherAttribute = new SingularAttributeStub<>("father", PersistentAttributeType.MANY_TO_ONE, Person.class,
				personEntityType);
		personSkillAttribute = new SingularAttributeStub<>("skill", PersistentAttributeType.EMBEDDED, Skill.class,
				skillEntityType);
		personAddressAttribute = new SingularAttributeStub<>("address", PersistentAttributeType.EMBEDDED, Address.class);
		skillNameAttribute = new SingularAttributeStub<>("name", PersistentAttributeType.BASIC, String.class);
		skillNestedAttribute = new SingularAttributeStub<>("nested", PersistentAttributeType.MANY_TO_ONE, Skill.class,
				skillEntityType);

		personEntityAttribtues = new LinkedHashSet<>();
		personEntityAttribtues.add(personIdAttribute);
		personEntityAttribtues.add(personFirstnameAttribute);
		personEntityAttribtues.add(personAgeAttribute);
		personEntityAttribtues.add(personFatherAttribute);
		personEntityAttribtues.add(personAddressAttribute);
		personEntityAttribtues.add(personSkillAttribute);

		skillEntityAttribtues = new LinkedHashSet<>();
		skillEntityAttribtues.add(skillNameAttribute);
		skillEntityAttribtues.add(skillNestedAttribute);

		doReturn(dummyPath).when(root).get(any(SingularAttribute.class));
		doReturn(dummyPath).when(root).get(anyString());

		doReturn(personEntityType).when(root).getModel();
		doReturn(personEntityAttribtues).when(personEntityType).getSingularAttributes();

		doReturn(skillEntityAttribtues).when(skillEntityType).getSingularAttributes();

		doReturn(dummyPredicate).when(cb).equal(any(Expression.class), any(String.class));
		doReturn(dummyPredicate).when(cb).equal(any(Expression.class), any(Long.class));
		doReturn(dummyPredicate).when(cb).like(any(Expression.class), any(String.class), anyChar());

		doReturn(expressionMock).when(cb).literal(any(Boolean.class));
		doReturn(truePredicate).when(cb).isTrue(eq(expressionMock));
		doReturn(andPredicate).when(cb).and(ArgumentMatchers.any());
		doReturn(orPredicate).when(cb).or(ArgumentMatchers.any());
	}

	@Test // DATAJPA-218
	void getPredicateShouldThrowExceptionOnNullRoot() {
		assertThatIllegalArgumentException().isThrownBy(
				() -> QueryByExamplePredicateBuilder.getPredicate(null, cb, of(new Person()), EscapeCharacter.DEFAULT));
	}

	@Test // DATAJPA-218
	void getPredicateShouldThrowExceptionOnNullCriteriaBuilder() {
		assertThatIllegalArgumentException().isThrownBy(
				() -> QueryByExamplePredicateBuilder.getPredicate(root, null, of(new Person()), EscapeCharacter.DEFAULT));
	}

	@Test // DATAJPA-218
	void getPredicateShouldThrowExceptionOnNullExample() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> QueryByExamplePredicateBuilder.getPredicate(root, null, null, EscapeCharacter.DEFAULT));
	}

	@Test // DATAJPA-218
	void emptyCriteriaListShouldResultInNullPredicate() {
		assertThat(QueryByExamplePredicateBuilder.getPredicate(root, cb, of(new Person()), EscapeCharacter.DEFAULT))
				.isNull();
	}

	@Test // DATAJPA-218
	void singleElementCriteriaShouldJustReturnIt() {

		Person p = new Person();
		p.firstname = "foo";

		assertThat(QueryByExamplePredicateBuilder.getPredicate(root, cb, of(p), EscapeCharacter.DEFAULT))
				.isEqualTo(dummyPredicate);
		verify(cb, times(1)).equal(any(Expression.class), eq("foo"));
	}

	@Test // DATAJPA-218
	void multiPredicateCriteriaShouldReturnCombinedOnes() {

		Person p = new Person();
		p.firstname = "foo";
		p.age = 2L;

		assertThat(QueryByExamplePredicateBuilder.getPredicate(root, cb, of(p), EscapeCharacter.DEFAULT))
				.isEqualTo(andPredicate);

		verify(cb, times(1)).equal(any(Expression.class), eq("foo"));
		verify(cb, times(1)).equal(any(Expression.class), eq(2L));
	}

	@Test // DATAJPA-879
	void orConcatenatesPredicatesIfMatcherSpecifies() {

		Person person = new Person();
		person.firstname = "foo";
		person.age = 2L;

		Example<Person> example = of(person, ExampleMatcher.matchingAny());

		assertThat(QueryByExamplePredicateBuilder.getPredicate(root, cb, example, EscapeCharacter.DEFAULT))
				.isEqualTo(orPredicate);

		verify(cb, times(1)).or(ArgumentMatchers.any());
	}

	@Test // DATAJPA-1372
	void considersSingularJoinedAttributes() {

		doReturn(from).when(root).join(anyString());
		doReturn(dummyPath).when(dummyPath).get(any(SingularAttribute.class));
		doReturn(dummyPath).when(dummyPath).get(anyString());

		Person person = new Person();
		person.skill = new Skill();
		person.skill.nested = new Skill();
		person.skill.nested.name = "foo";

		Example<Person> example = of(person,
				ExampleMatcher.matching().withMatcher("skill.nested.name", GenericPropertyMatcher::contains));

		assertThat(QueryByExamplePredicateBuilder.getPredicate(root, cb, example)).isEqualTo(dummyPredicate);

		verify(cb).like(dummyPath, "%foo%", '\\');
	}

	@Test // DATAJPA-1534
	void likePatternsGetEscapedContaining() {

		Person person = new Person();
		person.firstname = "f\\o_o";

		Example<Person> example = of( //
				person, //
				ExampleMatcher //
						.matchingAny() //
						.withStringMatcher(ExampleMatcher.StringMatcher.CONTAINING) //
		);

		QueryByExamplePredicateBuilder.getPredicate(root, cb, example, EscapeCharacter.DEFAULT);

		verify(cb, times(1)).like(any(Expression.class), eq("%f\\\\o\\_o%"), eq('\\'));
	}

	@Test // DATAJPA-1534
	void likePatternsGetEscapedStarting() {

		Person person = new Person();
		person.firstname = "f\\o_o";

		Example<Person> example = of( //
				person, //
				ExampleMatcher //
						.matchingAny() //
						.withStringMatcher(ExampleMatcher.StringMatcher.STARTING) //
		);

		QueryByExamplePredicateBuilder.getPredicate(root, cb, example, EscapeCharacter.DEFAULT);

		verify(cb, times(1)).like(any(Expression.class), eq("f\\\\o\\_o%"), eq('\\'));
	}

	@Test // DATAJPA-1534
	void likePatternsGetEscapedEnding() {

		Person person = new Person();
		person.firstname = "f\\o_o";

		Example<Person> example = of( //
				person, //
				ExampleMatcher //
						.matchingAny() //
						.withStringMatcher(ExampleMatcher.StringMatcher.ENDING) //
		);

		QueryByExamplePredicateBuilder.getPredicate(root, cb, example, EscapeCharacter.DEFAULT);

		verify(cb, times(1)).like(any(Expression.class), eq("%f\\\\o\\_o"), eq('\\'));
	}

	@SuppressWarnings("unused")
	static class Person {

		@Id Long id;
		String firstname;
		Long age;

		Person father;
		Address address;
		Skill skill;
	}

	@SuppressWarnings("unused")
	static class Address {

		String city;
		String country;
	}

	@SuppressWarnings("unused")
	static class Skill {

		@Id Long id;
		String name;
		Skill nested;
	}

	static class SingularAttributeStub<X, T> implements SingularAttribute<X, T> {

		private String name;
		private PersistentAttributeType attributeType;
		private Class<T> javaType;
		private Type<T> type;

		SingularAttributeStub(String name, PersistentAttributeType attributeType, Class<T> javaType) {
			this(name, attributeType, javaType, null);
		}

		SingularAttributeStub(String name, PersistentAttributeType attributeType, Class<T> javaType, Type<T> type) {
			this.name = name;
			this.attributeType = attributeType;
			this.javaType = javaType;
			this.type = type;
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public PersistentAttributeType getPersistentAttributeType() {
			return attributeType;
		}

		@Override
		public ManagedType<X> getDeclaringType() {
			return null;
		}

		@Override
		public Class<T> getJavaType() {
			return javaType;
		}

		@Override
		public Member getJavaMember() {
			return null;
		}

		@Override
		public boolean isAssociation() {
			return !attributeType.equals(PersistentAttributeType.BASIC)
					&& !attributeType.equals(PersistentAttributeType.EMBEDDED);
		}

		@Override
		public boolean isCollection() {
			return false;
		}

		@Override
		public jakarta.persistence.metamodel.Bindable.BindableType getBindableType() {
			return BindableType.SINGULAR_ATTRIBUTE;
		}

		@Override
		public Class<T> getBindableJavaType() {
			return javaType;
		}

		@Override
		public boolean isId() {
			return ObjectUtils.nullSafeEquals(name, "id");
		}

		@Override
		public boolean isVersion() {
			return false;
		}

		@Override
		public boolean isOptional() {
			return false;
		}

		@Override
		public Type<T> getType() {
			return type;
		}

	}
}
