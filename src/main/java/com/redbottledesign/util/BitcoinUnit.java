package com.redbottledesign.util;

import java.math.BigDecimal;

public enum BitcoinUnit
{
    SATOSHIS
    {
        @Override
        public BigDecimal toSatoshis(BigDecimal value)
        {
            return value;
        }

        @Override
        public BigDecimal toBitcoins(BigDecimal value)
        {
            return value.divide(SATOSHIS_PER_BITCOIN);
        }

        @Override
        public BigDecimal convert(BigDecimal value, BitcoinUnit unit)
        {
            return unit.toSatoshis(value);
        }
    },

    BITCOINS
    {
        @Override
        public BigDecimal toSatoshis(BigDecimal value)
        {
            return value.multiply(SATOSHIS_PER_BITCOIN);
        }

        @Override
        public BigDecimal toBitcoins(BigDecimal value)
        {
            return value;
        }

        @Override
        public BigDecimal convert(BigDecimal value, BitcoinUnit unit)
        {
            return unit.toBitcoins(value);
        }
    };

    protected static final BigDecimal SATOSHIS_PER_BITCOIN = BigDecimal.valueOf(100000000);

    public abstract BigDecimal toSatoshis(BigDecimal value);
    public abstract BigDecimal toBitcoins(BigDecimal value);
    public abstract BigDecimal convert(BigDecimal value, BitcoinUnit unit);
}