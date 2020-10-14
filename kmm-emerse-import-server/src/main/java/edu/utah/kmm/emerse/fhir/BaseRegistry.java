package edu.utah.kmm.emerse.fhir;

import org.springframework.util.Assert;

import java.util.HashMap;
import java.util.Map;

public abstract class BaseRegistry<T> {

    private final Map<String, T> registry = new HashMap<>();

    private final Class<T> clazz;

    protected BaseRegistry(Class<T> clazz, T... items) {
       this.clazz = clazz;

       for (T item: items) {
           register(item);
       }
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

}
