package org.springframework.data.jpa.domain.sample;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.ManyToOne;

import java.io.Serializable;

/**
 * Sample class for integration testing
 * {@link org.springframework.data.jpa.repository.support.JpaMetamodelEntityInformation}.
 *
 * @author Jens Schauder
 */
@Entity
@IdClass(SampleWithIdClassIncludingEntity.SampleWithIdClassPK.class)
public class SampleWithIdClassIncludingEntity {

	@Id Long first;
	@ManyToOne
	@Id OtherEntity second;

	public SampleWithIdClassIncludingEntity() {}

	public Long getFirst() {
		return this.first;
	}

	public OtherEntity getSecond() {
		return this.second;
	}

	public void setFirst(Long first) {
		this.first = first;
	}

	public void setSecond(OtherEntity second) {
		this.second = second;
	}

	public String toString() {
		return "SampleWithIdClassIncludingEntity(first=" + this.getFirst() + ", second=" + this.getSecond() + ")";
	}

	@SuppressWarnings("serial")
	public static class SampleWithIdClassPK implements Serializable {

		Long first;
		Long second;

		public SampleWithIdClassPK() {}

		public Long getFirst() {
			return this.first;
		}

		public Long getSecond() {
			return this.second;
		}

		public void setFirst(Long first) {
			this.first = first;
		}

		public void setSecond(Long second) {
			this.second = second;
		}

		public String toString() {
			return "SampleWithIdClassIncludingEntity.SampleWithIdClassPK(first=" + this.getFirst() + ", second="
					+ this.getSecond() + ")";
		}
	}

	@Entity
	public static class OtherEntity {
		@Id Long otherId;

		public OtherEntity() {}

		public Long getOtherId() {
			return this.otherId;
		}

		public void setOtherId(Long otherId) {
			this.otherId = otherId;
		}

		public String toString() {
			return "SampleWithIdClassIncludingEntity.OtherEntity(otherId=" + this.getOtherId() + ")";
		}
	}

	/**
	 * This class emulates a proxy at is returned from Hibernate.
	 */
	public static class OtherEntity$$PsudoProxy extends OtherEntity {}

}
