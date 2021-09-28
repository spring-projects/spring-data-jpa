package org.springframework.data.jpa.domain.sample;

import lombok.Data;

import java.io.Serializable;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.ManyToOne;

/**
 * Sample class for integration testing
 * {@link org.springframework.data.jpa.repository.support.JpaMetamodelEntityInformation}.
 *
 * @author Jens Schauder
 */
@Entity
@IdClass(SampleWithIdClassIncludingEntity.SampleWithIdClassPK.class)
@Data
public class SampleWithIdClassIncludingEntity {

	@Id Long first;
	@ManyToOne @Id OtherEntity second;

	@Data
	@SuppressWarnings("serial")
	public static class SampleWithIdClassPK implements Serializable {

		Long first;
		Long second;
	}

	@Entity
	@Data
	public static class OtherEntity {
		@Id Long otherId;
	}

	/**
	 * This class emulates a proxy at is returned from Hibernate.
	 */
	public static class OtherEntity$$PsudoProxy extends OtherEntity {}

}
