package com.redbottledesign.util;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public class LateBoundParameterizedType
implements ParameterizedType
{
    private final ParameterizedType parameterizedType;
    private final Type[] typeArguments;

    public LateBoundParameterizedType(ParameterizedType parameterizedType, Type... typeArguments)
    {
        this.parameterizedType  = parameterizedType;
        this.typeArguments      = typeArguments;
    }

    @Override
    public Type[] getActualTypeArguments()
    {
        return this.typeArguments;
    }

    @Override
    public Type getRawType()
    {
        return this.parameterizedType.getRawType();
    }

    @Override
    public Type getOwnerType()
    {
        return this.parameterizedType.getOwnerType();
    }
}