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
package org.springframework.data.jpa.util;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Stream;

import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.util.ClassUtils;

/**
 * Simplified version of <a href=
 * "https://github.com/spring-projects/spring-boot/blob/main/spring-boot-project/spring-boot-tools/spring-boot-test-support/src/main/java/org/springframework/boot/testsupport/classpath/ModifiedClassPathClassLoader.java">ModifiedClassPathClassLoader</a>.
 *
 * @author Christoph Strobl
 */
class PackageExcludingClassLoader extends URLClassLoader {

	private final Set<String> excludedPackages;
	private final ClassLoader junitLoader;

	PackageExcludingClassLoader(URL[] urls, ClassLoader parent, Collection<String> excludedPackages,
			ClassLoader junitClassLoader) {

		super(urls, parent);
		this.excludedPackages = Set.copyOf(excludedPackages);
		this.junitLoader = junitClassLoader;
	}

	@Override
	public Class<?> loadClass(String name) throws ClassNotFoundException {

		if (name.startsWith("org.junit") || name.startsWith("org.hamcrest")) {
			return Class.forName(name, false, this.junitLoader);
		}

		String packageName = ClassUtils.getPackageName(name);
		if (this.excludedPackages.contains(packageName)) {
			throw new ClassNotFoundException(name);
		}
		return super.loadClass(name);
	}

	static PackageExcludingClassLoader get(Class<?> testClass, Method testMethod) {

		List<String> excludedPackages = readExcludedPackages(testClass, testMethod);

		if (excludedPackages.isEmpty()) {
			return null;
		}

		ClassLoader testClassClassLoader = testClass.getClassLoader();
		Stream<URL> urls = null;
		if (testClassClassLoader instanceof URLClassLoader urlClassLoader) {
			urls = Stream.of(urlClassLoader.getURLs());
		} else {
			urls = Stream.of(ManagementFactory.getRuntimeMXBean().getClassPath().split(File.pathSeparator))
					.map(PackageExcludingClassLoader::toURL);
		}

		return new PackageExcludingClassLoader(urls.toArray(URL[]::new), testClassClassLoader.getParent(), excludedPackages,
				testClassClassLoader);
	}

	private static List<String> readExcludedPackages(Class<?> testClass, Method testMethod) {

		return Stream.of( //
				AnnotatedElementUtils.findMergedAnnotation(testClass, ClassPathExclusions.class),
				AnnotatedElementUtils.findMergedAnnotation(testMethod, ClassPathExclusions.class) //
		).filter(Objects::nonNull) //
				.map(ClassPathExclusions::packages) //
				.collect(new CombingArrayCollector<String>());
	}

	private static URL toURL(String entry) {
		try {
			return new File(entry).toURI().toURL();
		} catch (Exception ex) {
			throw new IllegalArgumentException(ex);
		}
	}

	private static class CombingArrayCollector<T> implements Collector<T[], List<T>, List<T>> {

		@Override
		public Supplier<List<T>> supplier() {
			return ArrayList::new;
		}

		@Override
		public BiConsumer<List<T>, T[]> accumulator() {
			return (target, values) -> target.addAll(Arrays.asList(values));
		}

		@Override
		public BinaryOperator<List<T>> combiner() {
			return (r1, r2) -> {
				r1.addAll(r2);
				return r1;
			};
		}

		@Override
		public Function<List<T>, List<T>> finisher() {
			return i -> (List<T>) i;
		}

		@Override
		public Set<Characteristics> characteristics() {
			return Collections.unmodifiableSet(EnumSet.of(Characteristics.IDENTITY_FINISH));
		}
	}
}
