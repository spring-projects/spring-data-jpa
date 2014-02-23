/*
 * Copyright 2013-2014 the original author or authors.
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
package org.springframework.data.jpa.repository.query;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;
import org.springframework.data.jpa.repository.query.StringQuery.InParameterBinding;
import org.springframework.data.jpa.repository.query.StringQuery.LikeParameterBinding;
import org.springframework.data.jpa.repository.query.StringQuery.ParameterBinding;
import org.springframework.data.repository.query.parser.Part.Type;

/**
 * Unit tests for {@link StringQuery}.
 * 
 * @author Oliver Gierke
 */
public class StringQueryUnitTests {

	/**
	 * @see DATAJPA-341
	 */
	@Test
	public void doesNotConsiderPlainLikeABinding() {

		String source = "select from User u where u.firstname like :firstname";
		StringQuery query = new StringQuery(source);

		assertThat(query.hasParameterBindings(), is(true));
		assertThat(query.getQueryString(), is(source));

		List<ParameterBinding> bindings = query.getParameterBindings();
		assertThat(bindings, hasSize(1));

		LikeParameterBinding binding = (LikeParameterBinding) bindings.get(0);
		assertThat(binding.getType(), is(Type.LIKE));
		assertThat(binding.hasName("firstname"), is(true));
	}

	@Test
	public void detectsPositionalLikeBindings() {

		StringQuery query = new StringQuery("select u from User u where u.firstname like %?1% or u.lastname like %?2");

		assertThat(query.hasParameterBindings(), is(true));
		assertThat(query.getQueryString(), is("select u from User u where u.firstname like ?1 or u.lastname like ?2"));

		List<ParameterBinding> bindings = query.getParameterBindings();
		assertThat(bindings, hasSize(2));

		LikeParameterBinding binding = (LikeParameterBinding) bindings.get(0);
		assertThat(binding, is(notNullValue()));
		assertThat(binding.hasPosition(1), is(true));
		assertThat(binding.getType(), is(Type.CONTAINING));

		binding = (LikeParameterBinding) bindings.get(1);
		assertThat(binding, is(notNullValue()));
		assertThat(binding.hasPosition(2), is(true));
		assertThat(binding.getType(), is(Type.ENDING_WITH));
	}

	@Test
	public void detectsNamedLikeBindings() {

		StringQuery query = new StringQuery("select u from User u where u.firstname like %:firstname");

		assertThat(query.hasParameterBindings(), is(true));
		assertThat(query.getQueryString(), is("select u from User u where u.firstname like :firstname"));

		List<ParameterBinding> bindings = query.getParameterBindings();
		assertThat(bindings, hasSize(1));

		LikeParameterBinding binding = (LikeParameterBinding) bindings.get(0);
		assertThat(binding, is(notNullValue()));
		assertThat(binding.hasName("firstname"), is(true));
		assertThat(binding.getType(), is(Type.ENDING_WITH));
	}

	/**
	 * @see DATAJPA-461
	 */
	@Test
	public void detectsNamedInParameterBindings() {

		String queryString = "select u from User u where u.id in :ids";
		StringQuery query = new StringQuery(queryString);

		assertThat(query.hasParameterBindings(), is(true));
		assertThat(query.getQueryString(), is(queryString));

		List<ParameterBinding> bindings = query.getParameterBindings();
		assertThat(bindings, hasSize(1));

		assertNamedBinding(InParameterBinding.class, "ids", bindings.get(0));
	}

	/**
	 * @see DATAJPA-461
	 */
	@Test
	public void detectsMultipleNamedInParameterBindings() {

		String queryString = "select u from User u where u.id in :ids and u.name in :names and foo = :bar";
		StringQuery query = new StringQuery(queryString);

		assertThat(query.hasParameterBindings(), is(true));
		assertThat(query.getQueryString(), is(queryString));

		List<ParameterBinding> bindings = query.getParameterBindings();
		assertThat(bindings, hasSize(3));

		assertNamedBinding(InParameterBinding.class, "ids", bindings.get(0));
		assertNamedBinding(InParameterBinding.class, "names", bindings.get(1));
		assertNamedBinding(ParameterBinding.class, "bar", bindings.get(2));
	}

	/**
	 * @see DATAJPA-461
	 */
	@Test
	public void detectsPositionalInParameterBindings() {

		String queryString = "select u from User u where u.id in ?1";
		StringQuery query = new StringQuery(queryString);

		assertThat(query.hasParameterBindings(), is(true));
		assertThat(query.getQueryString(), is(queryString));

		List<ParameterBinding> bindings = query.getParameterBindings();
		assertThat(bindings, hasSize(1));

		assertPositionalBinding(InParameterBinding.class, 1, bindings.get(0));
	}

	/**
	 * @see DATAJPA-461
	 */
	@Test
	public void detectsMultiplePositionalInParameterBindings() {

		String queryString = "select u from User u where u.id in ?1 and u.names in ?2 and foo = ?3";
		StringQuery query = new StringQuery(queryString);

		assertThat(query.hasParameterBindings(), is(true));
		assertThat(query.getQueryString(), is(queryString));

		List<ParameterBinding> bindings = query.getParameterBindings();
		assertThat(bindings, hasSize(3));

		assertPositionalBinding(InParameterBinding.class, 1, bindings.get(0));
		assertPositionalBinding(InParameterBinding.class, 2, bindings.get(1));
		assertPositionalBinding(ParameterBinding.class, 3, bindings.get(2));
	}

	/**
	 * @see DATAJPA-373
	 */
	@Test
	public void handlesMultipleNamedLikeBindingsCorrectly() {
		new StringQuery("select u from User u where u.firstname like %:firstname or foo like :bar");
	}

	@Test(expected = IllegalArgumentException.class)
	public void rejectsDifferentBindingsForRepeatedParameter() {
		new StringQuery("select u from User u where u.firstname like %?1 and u.lastname like ?1%");
	}

	/**
	 * @see DATAJPA-461
	 */
	@Test
	public void treatsGreaterThanBindingAsSimpleBinding() {

		StringQuery query = new StringQuery("select u from User u where u.createdDate > ?1");
		List<ParameterBinding> bindings = query.getParameterBindings();

		assertThat(bindings, hasSize(1));
		assertPositionalBinding(ParameterBinding.class, 1, bindings.get(0));
	}

	/**
	 * @see DATAJPA-473
	 */
	@Test
	public void removesLikeBindingsFromQueryIfQueryContainsSimpleBinding() {

		StringQuery query = new StringQuery("SELECT a FROM Article a WHERE a.overview LIKE %:escapedWord% ESCAPE '~'"
				+ " OR a.content LIKE %:escapedWord% ESCAPE '~' OR a.title = :word ORDER BY a.articleId DESC");

		List<ParameterBinding> bindings = query.getParameterBindings();

		assertThat(bindings, hasSize(2));
		assertNamedBinding(LikeParameterBinding.class, "escapedWord", bindings.get(0));
		assertNamedBinding(ParameterBinding.class, "word", bindings.get(1));
		assertThat(query.getQueryString(), is("SELECT a FROM Article a WHERE a.overview LIKE :escapedWord ESCAPE '~'"
				+ " OR a.content LIKE :escapedWord ESCAPE '~' OR a.title = :word ORDER BY a.articleId DESC"));
	}

	private void assertPositionalBinding(Class<? extends ParameterBinding> bindingType, Integer position,
			ParameterBinding expectedBinding) {

		assertThat(bindingType.isInstance(expectedBinding), is(true));
		assertThat(expectedBinding, is(notNullValue()));
		assertThat(expectedBinding.hasPosition(position), is(true));
	}

	private void assertNamedBinding(Class<? extends ParameterBinding> bindingType, String parameterName,
			ParameterBinding expectedBinding) {

		assertThat(bindingType.isInstance(expectedBinding), is(true));
		assertThat(expectedBinding, is(notNullValue()));
		assertThat(expectedBinding.hasName(parameterName), is(true));
	}
}
