/*
 * Copyright 2025 the original author or authors.
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
package org.springframework.data.jpa.repository.aot;

import jakarta.persistence.EntityManager;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.List;

import org.mockito.Mockito;

import org.springframework.aot.test.generate.TestGenerationContext;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.DefaultBeanNameGenerator;
import org.springframework.context.annotation.ImportResource;
import org.springframework.core.env.Environment;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.test.tools.TestCompiler;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.data.expression.ValueExpressionParser;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.jpa.repository.sample.SampleConfig;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.projection.SpelAwareProxyProjectionFactory;
import org.springframework.data.repository.config.AnnotationRepositoryConfigurationSource;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.RepositoryComposition;
import org.springframework.data.repository.core.support.RepositoryFactoryBeanSupport;
import org.springframework.data.repository.core.support.RepositoryFragment;
import org.springframework.data.repository.query.QueryMethodValueEvaluationContextAccessor;
import org.springframework.data.repository.query.ValueExpressionDelegate;
import org.springframework.util.ReflectionUtils;

/**
 * Test Configuration Support Class for generated AOT Repository Fragments based on a Repository Interface.
 * <p>
 * This configuration generates the AOT repository, compiles sources and configures a BeanFactory to contain the AOT
 * fragment. Additionally, the fragment is exposed through a {@code repositoryInterface} JDK proxy forwarding method
 * invocations to the backing AOT fragment. Note that {@code repositoryInterface} is not a repository proxy.
 *
 * @author Mark Paluch
 */
@ImportResource("classpath:/infrastructure.xml")
public class AotFragmentTestConfigurationSupport implements BeanFactoryPostProcessor {

	private final Class<?> repositoryInterface;
	private final boolean registerFragmentFacade;
	private final TestJpaAotRepositoryContext<?> repositoryContext;

	public AotFragmentTestConfigurationSupport(Class<?> repositoryInterface) {
		this(repositoryInterface, SampleConfig.class, true);
	}

	public AotFragmentTestConfigurationSupport(Class<?> repositoryInterface, Class<?> configClass) {
		this(repositoryInterface, configClass, true);
	}

	public AotFragmentTestConfigurationSupport(Class<?> repositoryInterface, Class<?> configClass,
			boolean registerFragmentFacade, Class<?>... additionalFragments) {
		this.repositoryInterface = repositoryInterface;

		RepositoryComposition composition = RepositoryComposition
				.of((List) Arrays.stream(additionalFragments).map(RepositoryFragment::structural).toList());
		this.repositoryContext = new TestJpaAotRepositoryContext<>(repositoryInterface, composition,
				new AnnotationRepositoryConfigurationSource(AnnotationMetadata.introspect(configClass),
						EnableJpaRepositories.class, new DefaultResourceLoader(), new StandardEnvironment(),
						Mockito.mock(BeanDefinitionRegistry.class), DefaultBeanNameGenerator.INSTANCE));
		this.registerFragmentFacade = registerFragmentFacade;
	}

	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {

		TestGenerationContext generationContext = new TestGenerationContext(repositoryInterface);

		repositoryContext.setBeanFactory(beanFactory);

		new JpaRepositoryContributor(repositoryContext).contribute(generationContext);

		AbstractBeanDefinition aotGeneratedRepository = BeanDefinitionBuilder
				.genericBeanDefinition(repositoryInterface.getName() + "Impl__AotRepository")
				.addConstructorArgValue(new RuntimeBeanReference(EntityManager.class))
				.addConstructorArgValue(
						getCreationContext(repositoryContext, beanFactory.getBean(Environment.class), beanFactory))
				.getBeanDefinition();

		generationContext.writeGeneratedContent();

		TestCompiler.forSystem().withCompilerOptions("-parameters").with(generationContext).compile(compiled -> {
			beanFactory.setBeanClassLoader(compiled.getClassLoader());
			((BeanDefinitionRegistry) beanFactory).registerBeanDefinition("fragment", aotGeneratedRepository);
		});

		if (registerFragmentFacade) {

			BeanDefinition fragmentFacade = BeanDefinitionBuilder.rootBeanDefinition((Class) repositoryInterface, () -> {

			Object fragment = beanFactory.getBean("fragment");
			Object proxy = getFragmentFacadeProxy(fragment);

			return repositoryInterface.cast(proxy);
		}).getBeanDefinition();
			((BeanDefinitionRegistry) beanFactory).registerBeanDefinition("fragmentFacade", fragmentFacade);
		}
	}

	private Object getFragmentFacadeProxy(Object fragment) {

		return Proxy.newProxyInstance(repositoryInterface.getClassLoader(), new Class<?>[] { repositoryInterface },
				(p, method, args) -> {

					Method target = ReflectionUtils.findMethod(fragment.getClass(), method.getName(), method.getParameterTypes());

					if (target == null) {
						throw new NoSuchMethodException("Method [%s] is not implemented by [%s]".formatted(method, target));
					}

					try {
						return target.invoke(fragment, args);
					} catch (ReflectiveOperationException e) {
						ReflectionUtils.handleReflectionException(e);
					}

					return null;
				});
	}

	private RepositoryFactoryBeanSupport.FragmentCreationContext getCreationContext(
			TestJpaAotRepositoryContext<?> repositoryContext, Environment environment, ListableBeanFactory beanFactory) {

		RepositoryFactoryBeanSupport.FragmentCreationContext creationContext = new RepositoryFactoryBeanSupport.FragmentCreationContext() {
			@Override
			public RepositoryMetadata getRepositoryMetadata() {
				return repositoryContext.getRepositoryInformation();
			}

			@Override
			public ValueExpressionDelegate getValueExpressionDelegate() {

				QueryMethodValueEvaluationContextAccessor accessor = new QueryMethodValueEvaluationContextAccessor(environment,
						beanFactory);
				return new ValueExpressionDelegate(accessor, ValueExpressionParser.create());
			}

			@Override
			public ProjectionFactory getProjectionFactory() {
				return new SpelAwareProxyProjectionFactory();
			}
		};

		return creationContext;
	}

}
