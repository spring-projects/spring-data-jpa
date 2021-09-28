package org.springframework.data.jpa.domain.sample;

import java.io.Serializable;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;

@Entity
@IdClass(SampleWithIdClass.SampleWithIdClassPK.class)
@Access(AccessType.FIELD)
public class SampleWithIdClass {

	@Id Long first;
	@Id Long second;

	@SuppressWarnings("serial")
	static class SampleWithIdClassPK implements Serializable {

		Long first;
		Long second;

		/*
		 * (non-Javadoc)
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(Object obj) {

			if (obj == this) {
				return true;
			}

			if (!(obj instanceof SampleWithIdClassPK)) {
				return false;
			}

			SampleWithIdClassPK that = (SampleWithIdClassPK) obj;

			return this.first.equals(that.first) && this.second.equals(that.second);
		}

		/*
		 * (non-Javadoc)
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode() {
			return first.hashCode() + second.hashCode();
		}
	}
}
