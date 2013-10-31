package com.redbottledesign.bitcoin.pool.checkpoint;

import com.redbottledesign.util.ValidationUtils;

public class SimpleCheckpointItem<T>
implements CheckpointItem
{
    private final String checkpointId;
    private final String checkpointType;
    private final T value;

    public SimpleCheckpointItem(String checkpointId, T value)
    {
        this(
            checkpointId,
            ValidationUtils.getParameterAndAssertNotNull(value, "value").getClass().getSimpleName(),
            value);
    }

    public SimpleCheckpointItem(String checkpointId, String checkpointType, T value)
    {
        this.checkpointId   = ValidationUtils.getParameterAndAssertNotNull(checkpointId,     "checkpointId");
        this.checkpointType = ValidationUtils.getParameterAndAssertNotNull(checkpointType,   "checkpointType");
        this.value          = ValidationUtils.getParameterAndAssertNotNull(value,            "value");
    }

    @Override
    public String getCheckpointId()
    {
        return this.checkpointId;
    }

    @Override
    public String getCheckpointType()
    {
        return this.checkpointType;
    }

    public T getValue()
    {
        return this.value;
    }
}