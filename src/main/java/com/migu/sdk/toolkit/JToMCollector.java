package com.migu.sdk.toolkit;

import io.vertx.core.json.JsonObject;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

/**
 * Created by lihan on 2017/10/25.
 */
public class JToMCollector implements
Collector<JsonObject, HashMap<String, JsonObject>, HashMap<String, JsonObject>> {

    public String key;

    public JToMCollector(String key) {
        this.key = key;
    }

    @Override
    public Supplier<HashMap<String, JsonObject>> supplier() {
        return HashMap::new;
    }

    @Override
    public BiConsumer<HashMap<String, JsonObject>, JsonObject> accumulator() {
        return (map, obj) -> {
            map.put(obj.getString(this.key), obj);
        };
    }

    @Override
    public BinaryOperator<HashMap<String, JsonObject>> combiner() {
        return (mapA, mapB) -> {
            mapA.putAll(mapB);
            return mapA;
        };
    }

    @Override
    public Function<HashMap<String, JsonObject>, HashMap<String, JsonObject>> finisher() {
        return Function.identity();
    }

    @Override
    public Set<Characteristics> characteristics() {
        return Collections.unmodifiableSet(EnumSet.of(Characteristics.IDENTITY_FINISH, Characteristics.CONCURRENT));
    }
}

