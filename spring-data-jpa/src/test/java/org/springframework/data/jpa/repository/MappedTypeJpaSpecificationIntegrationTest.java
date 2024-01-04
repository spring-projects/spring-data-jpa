package org.springframework.data.jpa.repository;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.data.jpa.domain.Specification.*;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.domain.sample.AbstractMappedType;
import org.springframework.data.jpa.domain.sample.AbstractMappedType_;
import org.springframework.data.jpa.domain.sample.ConcreteType1;
import org.springframework.data.jpa.domain.sample.ConcreteType1_;
import org.springframework.data.jpa.repository.sample.ConcreteRepository1;
import org.springframework.data.jpa.repository.sample.MappedTypeRepository;
import org.springframework.data.jpa.repository.sample.SampleConfig;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = SampleConfig.class)
public class MappedTypeJpaSpecificationIntegrationTest {

	@Autowired
	@Qualifier("concreteRepository1") private MappedTypeRepository<?> mappedTypeRepository;

	@Autowired private ConcreteRepository1 concreteRepository1;

	@Test
	void testUseMappedTypeRepository() {
		concreteRepository1.save(new ConcreteType1("Test"));

		List<? extends AbstractMappedType> findings = mappedTypeRepository.findAll(mappedTypeAttribute1Equals("Test"));
		assertThat(findings).isNotEmpty();
	}

	@Test
	void testUseConcreteRepository() {
		concreteRepository1.save(new ConcreteType1("Test"));

		List<? extends AbstractMappedType> findings = concreteRepository1
				.findAll(where(mappedTypeAttribute1Equals("Test")));
		assertThat(findings).isNotEmpty();
	}

	@Test
	void testUseConcreteRepositoryAndCombineSpecifications() {
		concreteRepository1.save(new ConcreteType1("Test"));

		List<? extends AbstractMappedType> firstFindings = concreteRepository1
				.findAll(where(concreteTypeIdIsGreaterOrEqualThan0()).and(mappedTypeAttribute1Equals("Test")));
		List<? extends AbstractMappedType> secondFindings = concreteRepository1
				.findAll(where(mappedTypeAttribute1Equals("Test")).and(concreteTypeIdIsGreaterOrEqualThan0()));

		assertThat(firstFindings).isEqualTo(secondFindings);
	}

	private static Specification<ConcreteType1> concreteTypeIdIsGreaterOrEqualThan0() {
		return (root, query, criteriaBuilder) -> criteriaBuilder.greaterThan(root.get(ConcreteType1_.ID), 0);
	}

	private static Specification<? extends AbstractMappedType> mappedTypeAttribute1Equals(String attribute1) {
		return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get(AbstractMappedType_.ATTRIBUTE1),
				attribute1);
	}
}
