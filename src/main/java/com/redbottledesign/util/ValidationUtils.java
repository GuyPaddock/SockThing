package com.redbottledesign.util;

public class ValidationUtils
{
    public static <T> T getParameterAndAssertNotNull(T value, String name)
    {
        if (value == null)
            throw new IllegalArgumentException(name + "cannot be null.");

        return value;
    }
}
