package com.example.ecomerce.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.jspecify.annotations.Nullable;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;
import tools.jackson.databind.DefaultTyping;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import tools.jackson.databind.jsontype.PolymorphicTypeValidator;

public class CustomRedisSerializer implements RedisSerializer<Object> {

    private final ObjectMapper objectMapper;

    public CustomRedisSerializer() {
        PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
                .allowIfBaseType(Object.class)
                .build();

        this.objectMapper = JsonMapper.builder()
                .activateDefaultTyping(ptv, DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build();
    }

    @Override
    public byte[] serialize(@Nullable Object value) throws SerializationException {
        if (value == null) {
            return null;
        }
        try {
            String[] pair = new String[]{value.getClass().getName(), objectMapper.writeValueAsString(value)};
            return objectMapper.writeValueAsBytes(pair);
        } catch (Exception e){
            throw new SerializationException("Could not serialize object!", e);
        }
    }

    @Override
    public @Nullable Object deserialize(byte @Nullable [] bytes) throws SerializationException {
        if (bytes == null || bytes.length == 0) return null;
        try {
            String[] pair = objectMapper.readValue(bytes, String[].class);
            Class<?> clazz = Class.forName(pair[0]);
            return objectMapper.readValue(pair[1], clazz);
        } catch (Exception e) {
            throw new SerializationException("Could not deserialize object", e);
        }
    }
}
