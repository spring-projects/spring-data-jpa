/*
 * Copyright 2011-2019 the original author or authors.
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
package org.springframework.data.jpa.repository.cdi;

import javax.inject.Inject;

/**
 * @author Oliver Gierke
 * @author Mark Paluch
 */
@Transactional
class RepositoryConsumer {

	@Inject PersonRepository unqualifiedRepo;
	@Inject @PersonDB PersonRepository qualifiedRepo;
	@Inject SamplePersonRepository samplePersonRepository;
	@Inject @UserDB QualifiedCustomizedUserRepository qualifiedCustomizedUserRepository;

	public void findAll() {
		unqualifiedRepo.findAll();
		qualifiedRepo.findAll();
	}

	public void save(Person person) {
		unqualifiedRepo.save(person);
		qualifiedRepo.save(person);
	}

	public int returnOne() {
		return samplePersonRepository.returnOne();
	}

	public void doSomethingOnUserDB() {
		qualifiedCustomizedUserRepository.doSomething();
	}

	public int returnOneUserDB() {
		return qualifiedCustomizedUserRepository.returnOne();
	}
}
