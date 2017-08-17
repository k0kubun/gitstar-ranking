package com.github.k0kubun.github_ranking.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

public class ApiResponse<T>
{
    public enum Type {
        BOOLEAN,
        INTEGER;

        @JsonValue
        public String toString()
        {
            switch (this) {
                case BOOLEAN:
                    return "boolean";
                case INTEGER:
                    return "integer";
                default:
                    throw new RuntimeException("unhandled ApiResponse.Type in toString()");
            }
        }
    }

    private Type type;
    private T result;

    public ApiResponse(Type type, T result)
    {
        this.type = type;
        this.result = result;
    }

    @JsonProperty("type")
    public Type getType()
    {
        return type;
    }

    @JsonProperty("result")
    public T getResult()
    {
        return result;
    }
}
