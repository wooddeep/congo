package com.migu.sdk.toolkit;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

/**
 * Created by lihan on 2017/10/25.
 */
public class JsonCollector implements Collector <JsonObject, JsonArray, JsonArray> {

    @Override
    public Supplier<JsonArray> supplier() {
        return JsonArray::new;
    }

    @Override
    public BiConsumer<JsonArray, JsonObject> accumulator() {
        return JsonArray::add;
    }

    @Override
    public BinaryOperator<JsonArray> combiner() {
        return (jsonArrayA, jsonArrayB) -> {
            jsonArrayA.addAll(jsonArrayB);
            return jsonArrayA;
        };
    }

    @Override
    public Function<JsonArray, JsonArray> finisher() {
        return Function.identity();
    }

    @Override
    public Set<Characteristics> characteristics() {
        return Collections.unmodifiableSet(EnumSet.of(Characteristics.IDENTITY_FINISH, Characteristics.CONCURRENT));
    }
}
