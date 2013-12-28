
package com.github.fireduck64.sockthing.output;

import java.math.BigInteger;
import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.fireduck64.sockthing.Config;
import com.github.fireduck64.sockthing.PoolUser;
import com.google.bitcoin.core.Address;
import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.core.Transaction;

public class OutputMonsterShareFees implements OutputMonster
{
    private static final Logger LOGGER = LoggerFactory.getLogger(OutputMonsterShareFees.class);

    protected List<Address> pay_to;
    protected NetworkParameters params;

    public OutputMonsterShareFees(Config conf, NetworkParameters params)
    throws com.google.bitcoin.core.AddressFormatException
    {
        this.params = params;
        conf.require("pay_to_address");

        pay_to = new LinkedList<Address>();

        for(String addr_str : conf.getList("pay_to_address"))
        {
            Address a = new Address(params,addr_str);
            pay_to.add(a);
        }

        if (LOGGER.isInfoEnabled())
        {
            LOGGER.info(String.format("Pool 'pay to' address is %s.", pay_to));
        }
    }

    @Override
    public void addOutputs(PoolUser pu, Transaction tx, BigInteger rewardValue, BigInteger feeValue)
    {
        feeValue = feeValue.divide(BigInteger.valueOf(2));
        BigInteger remaining = rewardValue.subtract(feeValue);

        BigInteger[] divmod = remaining.divideAndRemainder(BigInteger.valueOf(pay_to.size()));
        BigInteger per_output = divmod[0];
        BigInteger first_output = per_output.add(divmod[1]);

        boolean first=true;
        for(Address addr : pay_to)
        {
            if (first)
            {
                tx.addOutput(first_output, addr);
                first=false;
            }
            else
            {
                tx.addOutput(per_output, addr);
            }
        }

        try
        {
            Address user_addr = new Address(params, pu.getName());
            if (feeValue.compareTo(BigInteger.ZERO) > 0)
            {
                tx.addOutput(feeValue, user_addr);


            }
        }
        catch(com.google.bitcoin.core.AddressFormatException e)
        {
            throw new RuntimeException(e);
        }
    }
}
