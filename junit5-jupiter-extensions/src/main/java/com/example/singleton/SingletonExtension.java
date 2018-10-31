package com.example.singleton;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.ExtensionContext.Store;
import org.junit.jupiter.api.extension.ExtensionContext.Store.CloseableResource;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

public class SingletonExtension implements ParameterResolver {

	public interface Resource<T> extends CloseableResource {

		@Override
		default void close() throws Exception {
			T instance = getInstance();
			if (instance instanceof AutoCloseable) {
				((AutoCloseable) instance).close();
			}
		}

		T getInstance();
	}

	public enum Layer {
		GLOBAL, CLASS, TEST, ANY
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.PARAMETER)
	public @interface Singleton {
		Class<? extends Resource> value();
		Layer layer() default Layer.GLOBAL;
		int parents() default 0;
	}

	private static final Namespace NAMESPACE = Namespace.create(SingletonExtension.class);

	@Override
	public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
		return parameterContext.isAnnotated(Singleton.class);
	}

	@Override
	public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
		Singleton singleton = parameterContext.findAnnotation(Singleton.class).orElseThrow(AssertionError::new);
		ExtensionContext context = context(singleton, extensionContext);
		Store store = context.getStore(NAMESPACE);
		Object key = context.getUniqueId() + '+' + singleton.value().getName();
		Resource resource = store.getOrComputeIfAbsent(key, k -> newResource(singleton, extensionContext), Resource.class);
		if (Resource.class.isAssignableFrom(parameterContext.getParameter().getType())) {
			return resource;
		}
		return resource.getInstance();
	}

	private ExtensionContext context(Singleton singleton, ExtensionContext extensionContext) {
		switch (singleton.layer()) {
			case GLOBAL: return extensionContext.getRoot();
			case CLASS: return extensionContext.getParent().orElseThrow(AssertionError::new);
			case TEST: return extensionContext;
			case ANY: return walkParents(singleton.parents(), extensionContext);
			default: throw new ParameterResolutionException("Can't get context for: " + singleton);
		}
	}

	private ExtensionContext walkParents(int parents, ExtensionContext extensionContext) {
		ExtensionContext current = extensionContext;
		for (int i = 0; i < parents; i++) {
			current = current.getParent().orElseThrow(AssertionError::new);
		}
		return current;
	}

	// TODO Use ReflectionSupport...
	private Resource newResource(Singleton singleton, ExtensionContext extensionContext) {
		Class<? extends Resource> resourceClass = singleton.value();
		try {
			try {
				// prefer constructor that takes an extension context argument
				return resourceClass.getConstructor(ExtensionContext.class).newInstance(extensionContext);
			}
			catch (NoSuchMethodException e) {
				// fall-back to no-arg constructor
				return resourceClass.getConstructor().newInstance();
			}
		}
		catch (ReflectiveOperationException e) {
			throw new AssertionError("Creating instance of resource " + resourceClass + " failed", e);
		}
	}
}
