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

import static org.assertj.core.api.Assertions.*;

import jakarta.persistence.EntityManager;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
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
import org.springframework.beans.factory.BeanFactory;
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
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.projection.SpelAwareProxyProjectionFactory;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.RepositoryFactoryBeanSupport;
import org.springframework.data.repository.query.ValueExpressionDelegate;
import org.springframework.data.util.Lazy;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.ReflectionUtils;

/**
 * @author Christoph Strobl
 * @author Mark Paluch
 */
class JpaRepositoryContributorUnitTests {

	private static Verifier<UserRepository> generated;

	@BeforeAll
	static void beforeAll() {

		TestJpaAotRepsitoryContext aotContext = new TestJpaAotRepsitoryContext(UserRepository.class, null);
		TestGenerationContext generationContext = new TestGenerationContext(UserRepository.class);

		new JpaRepositoryContributor(aotContext).contribute(generationContext);

		AbstractBeanDefinition emBeanDefinition = BeanDefinitionBuilder
				.rootBeanDefinition("org.springframework.orm.jpa.SharedEntityManagerCreator")
				.setFactoryMethod("createSharedEntityManager").addConstructorArgReference("entityManagerFactory")
				.setLazyInit(true).getBeanDefinition();

		RepositoryFactoryBeanSupport.FragmentCreationContext creationContext = new RepositoryFactoryBeanSupport.FragmentCreationContext() {
			@Override
			public RepositoryMetadata getRepositoryMetadata() {
				return aotContext.getRepositoryInformation();
			}

			@Override
			public ValueExpressionDelegate getValueExpressionDelegate() {
				return ValueExpressionDelegate.create();
			}

			@Override
			public ProjectionFactory getProjectionFactory() {
				return new SpelAwareProxyProjectionFactory();
			}
		};

		AbstractBeanDefinition aotGeneratedRepository = BeanDefinitionBuilder
				.genericBeanDefinition("org.springframework.data.jpa.repository.aot.generated.UserRepositoryImpl__Aot")
				.addConstructorArgReference("jpaSharedEM_entityManagerFactory").addConstructorArgValue(creationContext)
				.getBeanDefinition();

		/*
		 alter the RepositoryFactory so we can write generated classes into a supplier and then write some custom code for instantiation
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

		generated = generateContext(generationContext, UserRepository.class) //
				.registerBeansFrom(new ClassPathResource("infrastructure.xml"))
				.register("jpaSharedEM_entityManagerFactory", emBeanDefinition)
				.register("aotUserRepository", aotGeneratedRepository);
	}

	@BeforeEach
	void beforeEach() {

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

		generated.verify(fragment -> {

			User user = fragment.findByEmailAddress("luke@jedi.org");
			assertThat(user.getLastname()).isEqualTo("Skywalker");
		});
	}

	@Test
	void testFindDerivedFinderOptionalEntity() {

		generated.verify(fragment -> {

			Optional<User> user = fragment.findOptionalOneByEmailAddress("yoda@jedi.org");
			assertThat(user).isNotNull().containsInstanceOf(User.class)
					.hasValueSatisfying(it -> assertThat(it).extracting(User::getFirstname).isEqualTo("Yoda"));
		});
	}

	@Test
	void testDerivedCount() {

		generated.verify(fragment -> {

			Long value = fragment.countUsersByLastname("Skywalker");
			assertThat(value).isEqualTo(2L);
		});
	}

	@Test
	void testDerivedExists() {

		generated.verify(fragment -> {

			Boolean exists = fragment.existsUserByLastname("Skywalker");
			assertThat(exists).isTrue();
		});
	}

	@Test
	void testDerivedFinderWithoutArguments() {

		generated.run(methodInvoker -> {

			List<User> users = methodInvoker.invoke("findUserNoArgumentsBy").onBean("aotUserRepository");
			assertThat(users).hasSize(7).hasOnlyElementsOfType(User.class);
		});
	}

	@Test
	void testDerivedFinderReturningList() {

		generated.run(methodInvoker -> {

			List<User> users = methodInvoker.invoke("findByLastnameStartingWith", "S").onBean("aotUserRepository");
			assertThat(users).extracting(User::getEmailAddress).containsExactlyInAnyOrder("luke@jedi.org", "vader@empire.com",
					"kylo@new-empire.com", "han@smuggler.net");
		});
	}

	@Test
	void testLimitedDerivedFinder() {

		generated.run(methodInvoker -> {

			List<User> users = methodInvoker.invoke("findTop2ByLastnameStartingWith", "S").onBean("aotUserRepository");
			assertThat(users).hasSize(2);
		});
	}

	@Test
	void testSortedDerivedFinder() {

		generated.run(methodInvoker -> {

			List<User> users = methodInvoker.invoke("findByLastnameStartingWithOrderByEmailAddress", "S")
					.onBean("aotUserRepository");
			assertThat(users).extracting(User::getEmailAddress).containsExactly("han@smuggler.net", "kylo@new-empire.com",
					"luke@jedi.org", "vader@empire.com");
		});
	}

	@Test
	void testDerivedFinderWithLimitArgument() {

		generated.run(methodInvoker -> {

			List<User> users = methodInvoker.invoke("findByLastnameStartingWith", "S", Limit.of(2))
					.onBean("aotUserRepository");
			assertThat(users).hasSize(2);
		});
	}

	@Test
	void testDerivedFinderWithSort() {

		generated.run(methodInvoker -> {

			List<User> users = methodInvoker.invoke("findByLastnameStartingWith", "S", Sort.by("emailAddress"))
					.onBean("aotUserRepository");
			assertThat(users).extracting(User::getEmailAddress).containsExactly("han@smuggler.net", "kylo@new-empire.com",
					"luke@jedi.org", "vader@empire.com");
		});
	}

	@Test
	void testDerivedFinderWithSortAndLimit() {

		generated.run(methodInvoker -> {

			List<User> users = methodInvoker.invoke("findByLastnameStartingWith", "S", Sort.by("emailAddress"), Limit.of(2))
					.onBean("aotUserRepository");
			assertThat(users).extracting(User::getEmailAddress).containsExactly("han@smuggler.net", "kylo@new-empire.com");
		});
	}

	@Test
	void testDerivedFinderReturningListWithPageable() {

		generated.run(methodInvoker -> {

			List<User> users = methodInvoker
					.invoke("findByLastnameStartingWith", "S", PageRequest.of(0, 2, Sort.by("emailAddress")))
					.onBean("aotUserRepository");
			assertThat(users).extracting(User::getEmailAddress).containsExactly("han@smuggler.net", "kylo@new-empire.com");
		});
	}

	@Test
	void testDerivedFinderReturningPage() {

		generated.run(methodInvoker -> {

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

		generated.run(methodInvoker -> {

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

		generated.run(methodInvoker -> {

			User user = methodInvoker.invoke("findAnnotatedQueryByEmailAddress", "yoda@jedi.org").onBean("aotUserRepository");
			assertThat(user).isNotNull().extracting(User::getFirstname).isEqualTo("Yoda");
		});
	}

	@Test
	void testAnnotatedFinderReturningListWithQuery() {

		generated.run(methodInvoker -> {

			List<User> users = methodInvoker.invoke("findAnnotatedQueryByLastname", "S").onBean("aotUserRepository");
			assertThat(users).extracting(User::getEmailAddress).containsExactlyInAnyOrder("han@smuggler.net",
					"kylo@new-empire.com", "luke@jedi.org", "vader@empire.com");
		});
	}

	@Test
	void testAnnotatedFinderUsingNamedParameterPlaceholderReturningListWithQuery() {

		generated.run(methodInvoker -> {

			List<User> users = methodInvoker.invoke("findAnnotatedQueryByLastnameParamter", "S").onBean("aotUserRepository");
			assertThat(users).extracting(User::getEmailAddress).containsExactlyInAnyOrder("han@smuggler.net",
					"kylo@new-empire.com", "luke@jedi.org", "vader@empire.com");
		});
	}

	@Test
	void testAnnotatedMultilineFinderWithQuery() {

		generated.run(methodInvoker -> {

			List<User> users = methodInvoker.invoke("findAnnotatedMultilineQueryByLastname", "S").onBean("aotUserRepository");
			assertThat(users).extracting(User::getEmailAddress).containsExactlyInAnyOrder("han@smuggler.net",
					"kylo@new-empire.com", "luke@jedi.org", "vader@empire.com");
		});
	}

	@Test
	void testAnnotatedFinderWithQueryAndLimit() {

		generated.run(methodInvoker -> {

			List<User> users = methodInvoker.invoke("findAnnotatedQueryByLastname", "S", Limit.of(2))
					.onBean("aotUserRepository");
			assertThat(users).hasSize(2);
		});
	}

	@Test
	void testAnnotatedFinderWithQueryAndSort() {

		generated.run(methodInvoker -> {

			List<User> users = methodInvoker.invoke("findAnnotatedQueryByLastname", "S", Sort.by("emailAddress"))
					.onBean("aotUserRepository");
			assertThat(users).extracting(User::getEmailAddress).containsExactly("han@smuggler.net", "kylo@new-empire.com",
					"luke@jedi.org", "vader@empire.com");
		});
	}

	@Test
	void testAnnotatedFinderWithQueryLimitAndSort() {

		generated.run(methodInvoker -> {

			List<User> users = methodInvoker.invoke("findAnnotatedQueryByLastname", "S", Limit.of(2), Sort.by("emailAddress"))
					.onBean("aotUserRepository");
			assertThat(users).extracting(User::getEmailAddress).containsExactly("han@smuggler.net", "kylo@new-empire.com");
		});
	}

	@Test
	void testAnnotatedFinderReturningListWithPageable() {

		generated.run(methodInvoker -> {

			List<User> users = methodInvoker
					.invoke("findAnnotatedQueryByLastname", "S", PageRequest.of(0, 2, Sort.by("emailAddress")))
					.onBean("aotUserRepository");
			assertThat(users).extracting(User::getEmailAddress).containsExactly("han@smuggler.net", "kylo@new-empire.com");
		});
	}

	@Test
	void testAnnotatedFinderReturningPage() {

		generated.run(methodInvoker -> {

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

		generated.run(methodInvoker -> {

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

		generated.run(methodInvoker -> {

			List<UserDtoProjection> users = methodInvoker.invoke("findUserProjectionByLastnameStartingWith", "S")
					.onBean("aotUserRepository");
			assertThat(users).extracting(UserDtoProjection::getEmailAddress).containsExactlyInAnyOrder("han@smuggler.net",
					"kylo@new-empire.com", "luke@jedi.org", "vader@empire.com");
		});
	}

	@Test
	void testDerivedFinderReturningPageOfProjections() {

		generated.run(methodInvoker -> {

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

		generated.runTransactional(methodInvoker -> {

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

		generated.run(methodInvoker -> {

			Page<String> page = methodInvoker.target().findByNativeQueryWithPageable(PageRequest.of(0, 2));

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

	static <T> GeneratedContextBuilder<T> generateContext(TestGenerationContext generationContext, Class<T> facade) {
		return new GeneratedContextBuilder<T>(generationContext, facade, "aotUserRepository");
	}

	static class GeneratedContextBuilder<T> implements Verifier<T> {

		private final Class<T> facade;
		TestGenerationContext generationContext;
		Map<String, BeanDefinition> beanDefinitions = new LinkedHashMap<>();
		Resource xmlBeanDefinitions;
		Lazy<DefaultListableBeanFactory> lazyFactory;
		private final String beanName;

		public GeneratedContextBuilder(TestGenerationContext generationContext, Class<T> facade, String beanName) {

			this.facade = facade;
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
			this.beanName = beanName;
		}

		GeneratedContextBuilder<T> register(String name, BeanDefinition beanDefinition) {
			this.beanDefinitions.put(name, beanDefinition);
			return this;
		}

		GeneratedContextBuilder<T> registerBeansFrom(Resource xmlBeanDefinitions) {
			this.xmlBeanDefinitions = xmlBeanDefinitions;
			return this;
		}

		public Verifier<T> run(Consumer<GeneratedContext<T>> methodInvoker) {
			methodInvoker.accept(new GeneratedContext<>(lazyFactory, facade, beanName));
			return this;
		}
	}

	/**
	 * AOT Repository Fragment Verifier.
	 *
	 * @param <T>
	 */
	interface Verifier<T> {

		default Verifier<T> verify(Consumer<T> facadeConsumer) {
			run(ctx -> {
				facadeConsumer.accept(ctx.target());
			});
			return this;
		}

		default Verifier<T> verifyTransactional(Consumer<T> facadeConsumer) {

			run(ctx -> {

				PlatformTransactionManager txMgr = ctx.delegate.get().getBean(PlatformTransactionManager.class);
				new TransactionTemplate(txMgr).execute(action -> {

					facadeConsumer.accept(ctx.target());
					return "ok";
				});
			});

			return this;
		}

		Verifier<T> run(Consumer<GeneratedContext<T>> methodInvoker);

		default Verifier<T> runTransactional(Consumer<GeneratedContext<T>> methodInvoker) {

			run(ctx -> {

				PlatformTransactionManager txMgr = ctx.delegate.get().getBean(PlatformTransactionManager.class);
				new TransactionTemplate(txMgr).execute(action -> {
					run(methodInvoker);
					return "ok";
				});
			});

			return this;
		}

		default <R> void doWithBean(Class<R> type, Consumer<R> runit) {
			run(ctx -> {

				boolean isEntityManager = type == EntityManager.class;
				R bean = ctx.delegate.get().getBean(type);

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

	static class GeneratedContext<T> {

		private final Supplier<? extends BeanFactory> delegate;
		private final Lazy<T> facade;

		public GeneratedContext(Supplier<? extends BeanFactory> beanFactory, Class<T> facade, String beanName) {

			this.delegate = beanFactory;

			this.facade = Lazy.of(() -> {

				Object bean = beanFactory.get().getBean(beanName);
				return (T) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] { facade },
						(proxy, method, args) -> {

							Method target = ReflectionUtils.findMethod(bean.getClass(), method.getName(), method.getParameterTypes());

							if (target == null) {
								throw new NoSuchMethodException("Method [%s] is not implemented by [%s]".formatted(method, target));
							}

							try {
								return target.invoke(bean, args);
							} catch (ReflectiveOperationException e) {
								ReflectionUtils.handleReflectionException(e);
							}

							return null;
						});
			});

		}

		/**
		 * Interface facade proxyfor invoking methods.
		 *
		 * @return
		 */
		T target() {
			return facade.get();
		}

		InvocationBuilder invoke(String method, Object... arguments) {

			return new InvocationBuilder() {
				@Override
				public <T> T onBean(String beanName) {
					Object bean = delegate.get().getBean(beanName);
					return ReflectionTestUtils.invokeMethod(bean, method, arguments);
				}
			};
		}

		interface InvocationBuilder {
			<T> T onBean(String beanName);
		}

	}

}
