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
package org.springframework.data.jpa.repository.query;

import static org.springframework.data.jpa.repository.query.JpaQueryParsingToken.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class encapsulating common query transformations.
 *
 * @author Mark Paluch
 */
class QueryTransformers {

	static List<JpaQueryParsingToken> filterCountSelection(List<JpaQueryParsingToken> selection) {

		List<JpaQueryParsingToken> target = new ArrayList<>(selection.size());
		boolean skipNext = false;

		for (JpaQueryParsingToken token : selection) {

			if (skipNext) {
				skipNext = false;
				continue;
			}

			if (token.isA(TOKEN_AS)) {
				skipNext = true;
				continue;
			}
			target.add(token);
		}

		return target;
	}

}
