/*
 * Copyright 2023-2024 the original author or authors.
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
package org.springframework.data.jpa;

import static org.assertj.core.api.Assertions.*;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.antlr.v4.runtime.RuntimeMetaData;
import org.hibernate.grammars.hql.HqlParser;
import org.junit.jupiter.api.Test;

import org.springframework.asm.ClassReader;
import org.springframework.asm.ClassVisitor;
import org.springframework.asm.MethodVisitor;
import org.springframework.asm.Opcodes;
import org.springframework.data.jpa.util.DisabledOnHibernate;
import org.springframework.lang.Nullable;

/**
 * Test to verify that we use the same Antlr version as Hibernate. We parse {@code org.hibernate.grammars.hql.HqlParser}
 * byte code to extract the constant that represents the Antlr version.
 * <p>
 * If this test fails, we should check and upgrade the Antlr version in our project.
 *
 * @author Mark Paluch
 */
class AntlrVersionTests {

	@Test
	@DisabledOnHibernate("6.2")
	void antlrVersionConvergence() throws Exception {

		ClassReader reader = new ClassReader(HqlParser.class.getName());
		ExpectedAntlrVersionVisitor visitor = new ExpectedAntlrVersionVisitor();
		reader.accept(visitor, 0);

		String expectedVersion = visitor.getExpectedVersion();
		String description = String.format("ANTLR version '%s' expected by Hibernate while our ANTLR version '%s'",
				expectedVersion, RuntimeMetaData.VERSION);

		assertThat(expectedVersion).isNotNull();
		assertThat(RuntimeMetaData.VERSION) //
				.describedAs(description) //
				.isEqualTo(expectedVersion);
	}

	private static class ExpectedAntlrVersionVisitor extends ClassVisitor {

		private static final Pattern versionPattern = Pattern.compile("\\d+\\.\\d+\\.\\d+");
		private static final int API = Opcodes.ASM10_EXPERIMENTAL;

		private @Nullable String expectedVersion;

		ExpectedAntlrVersionVisitor() {
			super(API);
		}

		@Nullable
		String getExpectedVersion() {
			return expectedVersion;
		}

		@Override
		public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
				String[] exceptions) {

			// static block
			if (!name.equals("<clinit>")) {
				return null;
			}

			return new MethodVisitor(API) {

				/**
				 * Intercept Load constant. First one is the generating version, second is the compile version.
				 */
				@Override
				public void visitLdcInsn(Object value) {

					if (value instanceof String && expectedVersion == null) {

						Matcher matcher = versionPattern.matcher(value.toString());
						if (matcher.matches()) {
							expectedVersion = value.toString();
						}
					}
					super.visitLdcInsn(value);
				}
			};
		}
	}
}
