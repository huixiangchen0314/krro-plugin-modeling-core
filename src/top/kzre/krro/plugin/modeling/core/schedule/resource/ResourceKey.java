package top.kzre.krro.plugin.modeling.core.schedule.resource;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * 类型安全的, 资源 Key
 */
public final class ResourceKey<V> {
    private final String name;
    // 由调度器单写
    private volatile V value;

    private ResourceKey(String name) {
        this.name = name;
    }

    /**
     * 设置值
     */
    public ResourceKey<V> value(V value) {
        this.value = value;
        return this;
    }

    /**
     * 取值
     */
    public V value() {
        V v = this.value;
        if (v == null) {
            throw new NullPointerException("value is null");
        }
        return v;
    }

    public void doIfPresent(Consumer<V> consumer) {
        if (value != null) {
            consumer.accept(value);
        }
    }

    public static <S> ResourceKey<S> of(String name) {
        return new ResourceKey<>(name);
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ResourceKey<?> that = (ResourceKey<?>) o;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name);
    }
}