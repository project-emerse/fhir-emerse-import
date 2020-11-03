package edu.utah.kmm.emerse.fhir;

import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.util.Assert;

import java.util.HashMap;
import java.util.Map;

/**
 * Base class for automated registration beans of a specified type.
 *
 * @param <T> The bean type to register.
 */
public abstract class BaseRegistry<T> implements BeanPostProcessor {

    private final Map<String, T> registry = new HashMap<>();

    private final Class<T> clazz;

    protected BaseRegistry(Class<T> clazz) {
       this.clazz = clazz;
    }

    public T get(String name) {
        return registry.get(name.toUpperCase());
    }

    public void register(T entry) {
        String name = getName(entry).toUpperCase();
        Assert.isTrue(!registry.containsKey(name), () -> "Duplicate registry entry: " + name);
        registry.put(name, entry);
    }

    protected abstract String getName(T entry);

    public Object postProcessAfterInitialization(Object bean, String beanName) {
        if (clazz.isInstance(bean)) {
            register((T) bean);
        }

        return bean;
    }
}
