/*
 * Copyright 2013 the original author or authors.
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
import org.springframework.data.jpa.repository.query.StringQuery.LikeBinding;
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

		assertThat(query.hasLikeBindings(), is(true));
		assertThat(query.getQuery(), is(source));

		List<LikeBinding> bindings = query.getLikeBindings();
		assertThat(bindings, hasSize(1));

		LikeBinding binding = bindings.get(0);
		assertThat(binding.getType(), is(Type.LIKE));
		assertThat(binding.hasName("firstname"), is(true));
	}

	@Test
	public void detectsPositionalLikeBindings() {

		StringQuery query = new StringQuery("select u from User u where u.firstname like %?1% or u.lastname like %?2");

		assertThat(query.hasLikeBindings(), is(true));
		assertThat(query.getQuery(), is("select u from User u where u.firstname like ?1 or u.lastname like ?2"));

		List<LikeBinding> bindings = query.getLikeBindings();
		assertThat(bindings, hasSize(2));

		LikeBinding binding = bindings.get(0);
		assertThat(binding, is(notNullValue()));
		assertThat(binding.hasPosition(1), is(true));
		assertThat(binding.getType(), is(Type.CONTAINING));

		binding = bindings.get(1);
		assertThat(binding, is(notNullValue()));
		assertThat(binding.hasPosition(2), is(true));
		assertThat(binding.getType(), is(Type.ENDING_WITH));
	}

	@Test
	public void detectsNamedLikeBindings() {

		StringQuery query = new StringQuery("select u from User u where u.firstname like %:firstname");

		assertThat(query.hasLikeBindings(), is(true));
		assertThat(query.getQuery(), is("select u from User u where u.firstname like :firstname"));

		List<LikeBinding> bindings = query.getLikeBindings();
		assertThat(bindings, hasSize(1));

		LikeBinding binding = bindings.get(0);
		assertThat(binding, is(notNullValue()));
		assertThat(binding.hasName("firstname"), is(true));
		assertThat(binding.getType(), is(Type.ENDING_WITH));
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
}
