package io.bootique.cayenne.jcache;

import com.google.inject.Binder;
import com.google.inject.BindingAnnotation;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.binder.LinkedBindingBuilder;
import com.google.inject.multibindings.Multibinder;
import io.bootique.cayenne.CayenneModule;
import io.bootique.jcache.JCacheModule;
import org.apache.cayenne.cache.invalidation.CacheInvalidationModule;
import org.apache.cayenne.cache.invalidation.CacheInvalidationModuleExtender;
import org.apache.cayenne.cache.invalidation.InvalidationHandler;
import org.apache.cayenne.jcache.JCacheConstants;

import javax.cache.CacheManager;
import javax.cache.configuration.Configuration;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Set;

/**
 * Bootique DI module integrating bootique-jcache to Cayenne.
 *
 * @since 0.18
 */
public class CayenneJCacheModule implements Module {

    /**
     * @param binder DI binder passed to the Module that invokes this method.
     * @return an instance of {@link CayenneJCacheModuleExtender} that can be used to load Cayenne cache
     * custom extensions.
     * @since 0.19
     */
    public static CayenneJCacheModuleExtender extend(Binder binder) {
        return new CayenneJCacheModuleExtender(binder);
    }

    /**
     * @param binder DI binder passed to the Module that invokes this method.
     * @return a {@link Multibinder} for cache configurations.
     * @deprecated since 0.19 call {@link #extend(Binder)} and then call
     * {@link CayenneJCacheModuleExtender#setDefaultCacheConfiguration(Configuration)}.
     */
    @Deprecated
    public static LinkedBindingBuilder<Configuration<?, ?>> contributeDefaultCacheConfiguration(Binder binder) {
        return JCacheModule.contributeConfiguration(binder).addBinding(JCacheConstants.DEFAULT_CACHE_NAME);
    }

    @Override
    public void configure(Binder binder) {
        extend(binder).initAllExtensions();

        CayenneModule.extend(binder).addModule(Key.get(org.apache.cayenne.di.Module.class, DefinedInCayenneJCache.class));
    }

    @Singleton
    @Provides
    @DefinedInCayenneJCache
    org.apache.cayenne.di.Module provideDiJCacheModule(CacheManager cacheManager, Set<InvalidationHandler> invalidationHandlers) {
        // return module composition
        return b -> {
            createInvalidationModule(invalidationHandlers).configure(b);
            createOverridesModule(cacheManager).configure(b);
        };
    }

    protected org.apache.cayenne.di.Module createInvalidationModule(Set<InvalidationHandler> invalidationHandlers) {
        CacheInvalidationModuleExtender extender = CacheInvalidationModule.extend();
        invalidationHandlers.forEach(extender::addHandler);
        return extender.module();
    }

    protected org.apache.cayenne.di.Module createOverridesModule(CacheManager cacheManager) {
        return b -> b.bind(CacheManager.class).toInstance(cacheManager);
    }

    @Target({ElementType.PARAMETER, ElementType.FIELD, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    @BindingAnnotation
    @interface DefinedInCayenneJCache {
    }

}
