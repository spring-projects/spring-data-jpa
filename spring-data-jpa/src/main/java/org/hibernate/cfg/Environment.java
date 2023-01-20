package org.hibernate.cfg;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.hibernate.HibernateException;
import org.hibernate.Version;
import org.hibernate.bytecode.spi.BytecodeProvider;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.ConfigHelper;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.springframework.data.jpa.support.hibernate.SpringByteCodeProvider;

public final class Environment implements AvailableSettings {

	private static final BytecodeProvider BYTECODE_PROVIDER_INSTANCE;
	private static final boolean ENABLE_REFLECTION_OPTIMIZER;

	private static final Properties GLOBAL_PROPERTIES;

	static {
		Version.logVersion();

		GLOBAL_PROPERTIES = new Properties();
		//Set USE_REFLECTION_OPTIMIZER to false to fix HHH-227
		GLOBAL_PROPERTIES.setProperty( USE_REFLECTION_OPTIMIZER, Boolean.FALSE.toString() );

		try {
			InputStream stream = ConfigHelper.getResourceAsStream( "/hibernate.properties" );
			try {
				GLOBAL_PROPERTIES.load(stream);
			}
			catch (Exception e) {
			}
			finally {
				try{
					stream.close();
				}
				catch (IOException ioe){
				}
			}
		}
		catch (HibernateException he) {
		}

		try {
			Properties systemProperties = System.getProperties();
			// Must be thread-safe in case an application changes System properties during Hibernate initialization.
			// See HHH-8383.
			synchronized (systemProperties) {
				GLOBAL_PROPERTIES.putAll(systemProperties);
			}
		}
		catch (SecurityException se) {
		}

		ENABLE_REFLECTION_OPTIMIZER = ConfigurationHelper.getBoolean(USE_REFLECTION_OPTIMIZER, GLOBAL_PROPERTIES);
		if ( ENABLE_REFLECTION_OPTIMIZER ) {
		}

		BYTECODE_PROVIDER_INSTANCE = buildBytecodeProvider( GLOBAL_PROPERTIES );
	}

	/**
	 * Should we use reflection optimization?
	 *
	 * @return True if reflection optimization should be used; false otherwise.
	 *
	 * @see #USE_REFLECTION_OPTIMIZER
	 * @see #getBytecodeProvider()
	 * @see BytecodeProvider#getReflectionOptimizer
	 *
	 * @deprecated Deprecated to indicate that the method will be moved to
	 * {@link org.hibernate.boot.spi.SessionFactoryOptions} /
	 * {@link org.hibernate.boot.SessionFactoryBuilder} - probably in 6.0.
	 * See <a href="https://hibernate.atlassian.net/browse/HHH-12194">HHH-12194</a> and
	 * <a href="https://hibernate.atlassian.net/browse/HHH-12193">HHH-12193</a> for details
	 */
	@Deprecated
	public static boolean useReflectionOptimizer() {
		return ENABLE_REFLECTION_OPTIMIZER;
	}

	/**
	 * @deprecated Deprecated to indicate that the method will be moved to
	 * {@link org.hibernate.boot.spi.SessionFactoryOptions} /
	 * {@link org.hibernate.boot.SessionFactoryBuilder} - probably in 6.0.
	 * See <a href="https://hibernate.atlassian.net/browse/HHH-12194">HHH-12194</a> and
	 * <a href="https://hibernate.atlassian.net/browse/HHH-12193">HHH-12193</a> for details
	 */
	@Deprecated
	public static BytecodeProvider getBytecodeProvider() {
		return BYTECODE_PROVIDER_INSTANCE;
	}

	/**
	 * Disallow instantiation
	 */
	private Environment() {
		throw new UnsupportedOperationException();
	}

	/**
	 * The {@link System#getProperties() system properties}, extended with all
	 * additional properties specified in {@code hibernate.properties}.
	 */
	public static Properties getProperties() {
		Properties copy = new Properties();
		copy.putAll(GLOBAL_PROPERTIES);
		return copy;
	}

	public static final String BYTECODE_PROVIDER_NAME_BYTEBUDDY = "bytebuddy";
	public static final String BYTECODE_PROVIDER_NAME_NONE = "none";
	public static final String BYTECODE_PROVIDER_NAME_DEFAULT = BYTECODE_PROVIDER_NAME_BYTEBUDDY;

	public static BytecodeProvider buildBytecodeProvider(Properties properties) {
		String provider = ConfigurationHelper.getString( BYTECODE_PROVIDER, properties, BYTECODE_PROVIDER_NAME_DEFAULT );
		return buildBytecodeProvider( provider );
	}

	private static BytecodeProvider buildBytecodeProvider(String providerName) {
		return new SpringByteCodeProvider();

//		if ( BYTECODE_PROVIDER_NAME_NONE.equals( providerName ) ) {
//			return new org.hibernate.bytecode.internal.none.BytecodeProviderImpl();
//		}
//		if ( BYTECODE_PROVIDER_NAME_BYTEBUDDY.equals( providerName ) ) {
//			return new org.hibernate.bytecode.internal.bytebuddy.BytecodeProviderImpl();
//		}


		// there is no need to support plugging in a custom BytecodeProvider via FQCN:
		// - the static helper methods on this class are deprecated
		// - it's possible to plug a custom BytecodeProvider directly into the ServiceRegistry
		//
		// This also allows integrators to inject a BytecodeProvider instance which has some
		// state; particularly useful to inject proxy definitions which have been prepared in
		// advance.
		// See also https://hibernate.atlassian.net/browse/HHH-13804 and how this was solved in
		// Quarkus.

//		return new org.hibernate.bytecode.internal.bytebuddy.BytecodeProviderImpl();
	}
}
