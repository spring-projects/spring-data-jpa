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
package org.springframework.data.jpa.repository.aot.generated;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.aot.test.generate.TestGenerationContext;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.test.tools.TestCompiler;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.sample.User;
import org.springframework.data.util.Lazy;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.example.UserDtoProjection;
import com.example.UserRepository;

/**
 * @author Christoph Strobl
 */
class JpaRepositoryContributorUnitTests {

	private static Verifyer generated;

	@BeforeAll
	static void beforeAll() {

		TestJpaAotRepsitoryContext aotContext = new TestJpaAotRepsitoryContext(UserRepository.class, null);
		TestGenerationContext generationContext = new TestGenerationContext(UserRepository.class);

		new JpaRepsoitoryContributor(aotContext).contribute(generationContext);

		AbstractBeanDefinition emBeanDefinition = BeanDefinitionBuilder
				.rootBeanDefinition("org.springframework.orm.jpa.SharedEntityManagerCreator")
				.setFactoryMethod("createSharedEntityManager").addConstructorArgReference("entityManagerFactory")
				.setLazyInit(true).getBeanDefinition();

		AbstractBeanDefinition aotGeneratedRepository = BeanDefinitionBuilder
				.genericBeanDefinition("com.example.UserRepositoryImpl__Aot")
				.addConstructorArgReference("jpaSharedEM_entityManagerFactory").getBeanDefinition();


		/*
		 alter the RepositoryFactory so we can write generated calsses into a supplier and then write some custom code for instantiation
		 on JpaRepositoryFactoryBean

		 beanDefinition.getPropertyValues().addPropertyValue("aotImplementation", new Function<BeanFactory, Instance>() {

		 	public Instance apply(BeanFactory beanFactor) {
		 		EntityManager em = beanFactory.getBean(EntityManger.class);
		 		return new com.example.UserRepositoryImpl__Aot(em);
		 	}
		 });
		 */

		// register a dedicated factory that can read stuff
		// don't write to spring.factories or uas another name for it
		// maybe write the code directly to a repo fragment
		// repo does not have to be a bean, but can be a method called by some component
		// pass list to entiy manager to have stuff in memory have to list written out directly when creating the bean

		generated = generateContext(generationContext) //
				.registerBeansFrom(new ClassPathResource("infrastructure.xml"))
				.register("jpaSharedEM_entityManagerFactory", emBeanDefinition)
				.register("aotUserRepository", aotGeneratedRepository);
	}

	@BeforeEach
	public void beforeEach() {

		generated.doWithBean(EntityManager.class, em -> {

			em.createQuery("DELETE FROM %s".formatted(User.class.getName())).executeUpdate();

			User luke = new User("Luke", "Skywalker", "luke@jedi.org");
			em.persist(luke);

			User leia = new User("Leia", "Organa", "leia@resistance.gov");
			em.persist(leia);

			User han = new User("Han", "Solo", "han@smuggler.net");
			em.persist(han);

			User chewbacca = new User("Chewbacca", "n/a", "chewie@smuggler.net");
			em.persist(chewbacca);

			User yoda = new User("Yoda", "n/a", "yoda@jedi.org");
			em.persist(yoda);

			User vader = new User("Anakin", "Skywalker", "vader@empire.com");
			em.persist(vader);

			User kylo = new User("Ben", "Solo", "kylo@new-empire.com");
			em.persist(kylo);
		});
	}

	@Test
	void testFindDerivedFinderSingleEntity() {

		generated.verify(methodInvoker -> {

			User user = methodInvoker.invoke("findByEmailAddress", "luke@jedi.org").onBean("aotUserRepository");
			assertThat(user.getLastname()).isEqualTo("Skywalker");
		});
	}

	@Test
	void testFindDerivedFinderOptionalEntity() {

		generated.verify(methodInvoker -> {

			Optional<User> user = methodInvoker.invoke("findOptionalOneByEmailAddress", "yoda@jedi.org")
					.onBean("aotUserRepository");
			assertThat(user).isNotNull().containsInstanceOf(User.class)
					.hasValueSatisfying(it -> assertThat(it).extracting(User::getFirstname).isEqualTo("Yoda"));
		});
	}

	@Test
	void testDerivedCount() {

		generated.verify(methodInvoker -> {

			Long value = methodInvoker.invoke("countUsersByLastname", "Skywalker").onBean("aotUserRepository");
			assertThat(value).isEqualTo(2L);
		});
	}

	@Test
	void testDerivedExists() {

		generated.verify(methodInvoker -> {

			Boolean exists = methodInvoker.invoke("existsUserByLastname", "Skywalker").onBean("aotUserRepository");
			assertThat(exists).isTrue();
		});
	}

	@Test
	void testDerivedFinderWithoutArguments() {

		generated.verify(methodInvoker -> {

			List<User> users = methodInvoker.invoke("findUserNoArgumentsBy").onBean("aotUserRepository");
			assertThat(users).hasSize(7).hasOnlyElementsOfType(User.class);
		});
	}

	@Test
	void testDerivedFinderReturningList() {

		generated.verify(methodInvoker -> {

			List<User> users = methodInvoker.invoke("findByLastnameStartingWith", "S").onBean("aotUserRepository");
			assertThat(users).extracting(User::getEmailAddress).containsExactlyInAnyOrder("luke@jedi.org", "vader@empire.com",
					"kylo@new-empire.com", "han@smuggler.net");
		});
	}

	@Test
	void testLimitedDerivedFinder() {

		generated.verify(methodInvoker -> {

			List<User> users = methodInvoker.invoke("findTop2ByLastnameStartingWith", "S").onBean("aotUserRepository");
			assertThat(users).hasSize(2);
		});
	}

	@Test
	void testSortedDerivedFinder() {

		generated.verify(methodInvoker -> {

			List<User> users = methodInvoker.invoke("findByLastnameStartingWithOrderByEmailAddress", "S")
					.onBean("aotUserRepository");
			assertThat(users).extracting(User::getEmailAddress).containsExactly("han@smuggler.net", "kylo@new-empire.com",
					"luke@jedi.org", "vader@empire.com");
		});
	}

	@Test
	void testDerivedFinderWithLimitArgument() {

		generated.verify(methodInvoker -> {

			List<User> users = methodInvoker.invoke("findByLastnameStartingWith", "S", Limit.of(2))
					.onBean("aotUserRepository");
			assertThat(users).hasSize(2);
		});
	}

	@Test
	void testDerivedFinderWithSort() {

		generated.verify(methodInvoker -> {

			List<User> users = methodInvoker.invoke("findByLastnameStartingWith", "S", Sort.by("emailAddress"))
					.onBean("aotUserRepository");
			assertThat(users).extracting(User::getEmailAddress).containsExactly("han@smuggler.net", "kylo@new-empire.com",
					"luke@jedi.org", "vader@empire.com");
		});
	}

	@Test
	void testDerivedFinderWithSortAndLimit() {

		generated.verify(methodInvoker -> {

			List<User> users = methodInvoker.invoke("findByLastnameStartingWith", "S", Sort.by("emailAddress"), Limit.of(2))
					.onBean("aotUserRepository");
			assertThat(users).extracting(User::getEmailAddress).containsExactly("han@smuggler.net", "kylo@new-empire.com");
		});
	}

	@Test
	void testDerivedFinderReturningListWithPageable() {

		generated.verify(methodInvoker -> {

			List<User> users = methodInvoker
					.invoke("findByLastnameStartingWith", "S", PageRequest.of(0, 2, Sort.by("emailAddress")))
					.onBean("aotUserRepository");
			assertThat(users).extracting(User::getEmailAddress).containsExactly("han@smuggler.net", "kylo@new-empire.com");
		});
	}

	@Test
	void testDerivedFinderReturningPage() {

		generated.verify(methodInvoker -> {

			Page<User> page = methodInvoker
					.invoke("findPageOfUsersByLastnameStartingWith", "S", PageRequest.of(0, 2, Sort.by("emailAddress")))
					.onBean("aotUserRepository");
			assertThat(page.getTotalElements()).isEqualTo(4);
			assertThat(page.getSize()).isEqualTo(2);
			assertThat(page.getContent()).extracting(User::getEmailAddress).containsExactly("han@smuggler.net",
					"kylo@new-empire.com");
		});
	}

	@Test
	void testDerivedFinderReturningSlice() {

		generated.verify(methodInvoker -> {

			Slice<User> slice = methodInvoker
					.invoke("findSliceOfUserByLastnameStartingWith", "S", PageRequest.of(0, 2, Sort.by("emailAddress")))
					.onBean("aotUserRepository");
			assertThat(slice.hasNext()).isTrue();
			assertThat(slice.getSize()).isEqualTo(2);
			assertThat(slice.getContent()).extracting(User::getEmailAddress).containsExactly("han@smuggler.net",
					"kylo@new-empire.com");
		});
	}

	@Test
	void testAnnotatedFinderReturningSingleValueWithQuery() {

		generated.verify(methodInvoker -> {

			User user = methodInvoker.invoke("findAnnotatedQueryByEmailAddress", "yoda@jedi.org").onBean("aotUserRepository");
			assertThat(user).isNotNull().extracting(User::getFirstname).isEqualTo("Yoda");
		});
	}

	@Test
	void testAnnotatedFinderReturningListWithQuery() {

		generated.verify(methodInvoker -> {

			List<User> users = methodInvoker.invoke("findAnnotatedQueryByLastname", "S").onBean("aotUserRepository");
			assertThat(users).extracting(User::getEmailAddress).containsExactlyInAnyOrder("han@smuggler.net",
					"kylo@new-empire.com", "luke@jedi.org", "vader@empire.com");
		});
	}

	@Test
	void testAnnotatedFinderUsingNamedParameterPlaceholderReturningListWithQuery() {

		generated.verify(methodInvoker -> {

			List<User> users = methodInvoker.invoke("findAnnotatedQueryByLastnameParamter", "S").onBean("aotUserRepository");
			assertThat(users).extracting(User::getEmailAddress).containsExactlyInAnyOrder("han@smuggler.net",
					"kylo@new-empire.com", "luke@jedi.org", "vader@empire.com");
		});
	}

	@Test
	void testAnnotatedMultilineFinderWithQuery() {

		generated.verify(methodInvoker -> {

			List<User> users = methodInvoker.invoke("findAnnotatedMultilineQueryByLastname", "S").onBean("aotUserRepository");
			assertThat(users).extracting(User::getEmailAddress).containsExactlyInAnyOrder("han@smuggler.net",
					"kylo@new-empire.com", "luke@jedi.org", "vader@empire.com");
		});
	}

	@Test
	void testAnnotatedFinderWithQueryAndLimit() {

		generated.verify(methodInvoker -> {

			List<User> users = methodInvoker.invoke("findAnnotatedQueryByLastname", "S", Limit.of(2))
					.onBean("aotUserRepository");
			assertThat(users).hasSize(2);
		});
	}

	@Test
	void testAnnotatedFinderWithQueryAndSort() {

		generated.verify(methodInvoker -> {

			List<User> users = methodInvoker.invoke("findAnnotatedQueryByLastname", "S", Sort.by("emailAddress"))
					.onBean("aotUserRepository");
			assertThat(users).extracting(User::getEmailAddress).containsExactly("han@smuggler.net", "kylo@new-empire.com",
					"luke@jedi.org", "vader@empire.com");
		});
	}

	@Test
	void testAnnotatedFinderWithQueryLimitAndSort() {

		generated.verify(methodInvoker -> {

			List<User> users = methodInvoker.invoke("findAnnotatedQueryByLastname", "S", Limit.of(2), Sort.by("emailAddress"))
					.onBean("aotUserRepository");
			assertThat(users).extracting(User::getEmailAddress).containsExactly("han@smuggler.net", "kylo@new-empire.com");
		});
	}

	@Test
	void testAnnotatedFinderReturningListWithPageable() {

		generated.verify(methodInvoker -> {

			List<User> users = methodInvoker
					.invoke("findAnnotatedQueryByLastname", "S", PageRequest.of(0, 2, Sort.by("emailAddress")))
					.onBean("aotUserRepository");
			assertThat(users).extracting(User::getEmailAddress).containsExactly("han@smuggler.net", "kylo@new-empire.com");
		});
	}

	@Test
	void testAnnotatedFinderReturningPage() {

		generated.verify(methodInvoker -> {

			Page<User> page = methodInvoker
					.invoke("findAnnotatedQueryPageOfUsersByLastname", "S", PageRequest.of(0, 2, Sort.by("emailAddress")))
					.onBean("aotUserRepository");
			assertThat(page.getTotalElements()).isEqualTo(4);
			assertThat(page.getSize()).isEqualTo(2);
			assertThat(page.getContent()).extracting(User::getEmailAddress).containsExactly("han@smuggler.net",
					"kylo@new-empire.com");
		});
	}

	@Test
	void testAnnotatedFinderReturningSlice() {

		generated.verify(methodInvoker -> {

			Slice<User> slice = methodInvoker
					.invoke("findAnnotatedQuerySliceOfUsersByLastname", "S", PageRequest.of(0, 2, Sort.by("emailAddress")))
					.onBean("aotUserRepository");
			assertThat(slice.hasNext()).isTrue();
			assertThat(slice.getSize()).isEqualTo(2);
			assertThat(slice.getContent()).extracting(User::getEmailAddress).containsExactly("han@smuggler.net",
					"kylo@new-empire.com");
		});
	}

	@Test
	void testDerivedFinderReturningListOfProjections() {

		generated.verify(methodInvoker -> {

			List<UserDtoProjection> users = methodInvoker.invoke("findUserProjectionByLastnameStartingWith", "S")
					.onBean("aotUserRepository");
			assertThat(users).extracting(UserDtoProjection::getEmailAddress).containsExactlyInAnyOrder("han@smuggler.net",
					"kylo@new-empire.com", "luke@jedi.org", "vader@empire.com");
		});
	}

	@Test
	void testDerivedFinderReturningPageOfProjections() {

		generated.verify(methodInvoker -> {

			Page<UserDtoProjection> page = methodInvoker
					.invoke("findUserProjectionByLastnameStartingWith", "S", PageRequest.of(0, 2, Sort.by("emailAddress")))
					.onBean("aotUserRepository");

			assertThat(page.getTotalElements()).isEqualTo(4);
			assertThat(page.getSize()).isEqualTo(2);
			assertThat(page.getContent()).extracting(UserDtoProjection::getEmailAddress).containsExactly("han@smuggler.net",
					"kylo@new-empire.com");
		});
	}

	// modifying

	@Test
	void testDerivedDeleteSingle() {

		generated.verifyInTx(methodInvoker -> {

			User result = methodInvoker.invoke("deleteByEmailAddress", "yoda@jedi.org").onBean("aotUserRepository");

			assertThat(result).isNotNull().extracting(User::getEmailAddress).isEqualTo("yoda@jedi.org");
		}).doWithBean(EntityManager.class, em -> {
			Object yodaShouldBeGone = em
					.createQuery("SELECT u FROM %s u WHERE u.emailAddress = 'yoda@jedi.org'".formatted(User.class.getName()))
					.getSingleResultOrNull();
			assertThat(yodaShouldBeGone).isNull();
		});
	}

	// native queries

	@Test
	void nativeQuery() {

		generated.verify(methodInvoker -> {

			Page<String> page = methodInvoker
				.invoke("findByNativeQueryWithPageable", PageRequest.of(0, 2))
				.onBean("aotUserRepository");

			assertThat(page.getTotalElements()).isEqualTo(7);
			assertThat(page.getSize()).isEqualTo(2);
			assertThat(page.getContent()).containsExactly("Anakin", "Ben");
		});
	}

	// old stuff below

	// TODO:
	void todo() {

		// Query q;
		// q.setMaxResults()
		// q.setFirstResult()

		// 1 build some more stuff from below
		// 2 set up boot sample project in data samples

		// query hints
		// first and max result for pagination
		// entity graphs
		// native queries
		// delete
		// @Modifying
		// flush / clear
	}

	static GeneratedContextBuilder generateContext(TestGenerationContext generationContext) {
		return new GeneratedContextBuilder(generationContext);
	}

	static class GeneratedContextBuilder implements Verifyer {

		TestGenerationContext generationContext;
		Map<String, BeanDefinition> beanDefinitions = new LinkedHashMap<>();
		Resource xmlBeanDefinitions;
		Lazy<DefaultListableBeanFactory> lazyFactory;

		public GeneratedContextBuilder(TestGenerationContext generationContext) {

			this.generationContext = generationContext;
			this.lazyFactory = Lazy.of(() -> {
				DefaultListableBeanFactory freshBeanFactory = new DefaultListableBeanFactory();
				TestCompiler.forSystem().with(generationContext).compile(compiled -> {

					freshBeanFactory.setBeanClassLoader(compiled.getClassLoader());
					if (xmlBeanDefinitions != null) {
						XmlBeanDefinitionReader beanDefinitionReader = new XmlBeanDefinitionReader(freshBeanFactory);
						beanDefinitionReader.loadBeanDefinitions(xmlBeanDefinitions);
					}

					for (Entry<String, BeanDefinition> entry : beanDefinitions.entrySet()) {
						freshBeanFactory.registerBeanDefinition(entry.getKey(), entry.getValue());
					}
				});
				return freshBeanFactory;
			});
		}

		GeneratedContextBuilder register(String name, BeanDefinition beanDefinition) {
			this.beanDefinitions.put(name, beanDefinition);
			return this;
		}

		GeneratedContextBuilder registerBeansFrom(Resource xmlBeanDefinitions) {
			this.xmlBeanDefinitions = xmlBeanDefinitions;
			return this;
		}

		public Verifyer verify(Consumer<GeneratedContext> methodInvoker) {
			methodInvoker.accept(new GeneratedContext(lazyFactory));
			return this;
		}

	}

	interface Verifyer {
		Verifyer verify(Consumer<GeneratedContext> methodInvoker);

		default Verifyer verifyInTx(Consumer<GeneratedContext> methodInvoker) {

			verify(ctx -> {

				PlatformTransactionManager txMgr = ctx.delegate.get().getBean(PlatformTransactionManager.class);
				new TransactionTemplate(txMgr).execute(action -> {
					verify(methodInvoker);
					return "ok";
				});
			});

			return this;
		}

		default <T> void doWithBean(Class<T> type, Consumer<T> runit) {
			verify(ctx -> {

				boolean isEntityManager = type == EntityManager.class;
				T bean = ctx.delegate.get().getBean(type);

				if (!isEntityManager) {
					runit.accept(bean);
				} else {

					PlatformTransactionManager txMgr = ctx.delegate.get().getBean(PlatformTransactionManager.class);
					new TransactionTemplate(txMgr).execute(action -> {
						runit.accept(bean);
						return "ok";
					});

				}
			});
		}
	}

	static class GeneratedContext {

		private Supplier<DefaultListableBeanFactory> delegate;

		public GeneratedContext(Supplier<DefaultListableBeanFactory> defaultListableBeanFactory) {
			this.delegate = defaultListableBeanFactory;
		}

		InvocationBuilder invoke(String method, Object... arguments) {

			return new InvocationBuilder() {
				@Override
				public <T> T onBean(String beanName) {
					DefaultListableBeanFactory defaultListableBeanFactory = delegate.get();

					Object bean = defaultListableBeanFactory.getBean(beanName);
					return ReflectionTestUtils.invokeMethod(bean, method, arguments);
				}
			};
		}

		interface InvocationBuilder {
			<T> T onBean(String beanName);
		}

	}

}
