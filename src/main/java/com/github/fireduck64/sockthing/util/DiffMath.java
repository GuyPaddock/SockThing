package com.github.fireduck64.sockthing.util;

import java.math.BigInteger;

import com.google.bitcoin.core.Sha256Hash;

public class DiffMath
{
    private static final int        RADIX_HEX      = 16;
    private static final BigInteger BIGINTEGER_TWO = BigInteger.valueOf(2);
    private static final BigInteger DIFFICULTY_ONE = BIGINTEGER_TWO.pow(256 - 32);

    public static Sha256Hash getTargetForDifficulty(int diff)
    {
        BigInteger diffTarget = DIFFICULTY_ONE.divide(BigInteger.valueOf(diff));

        String target = diffTarget.toString(RADIX_HEX).toLowerCase();

        while (target.length() < 64)
        {
            target = "0" + target;
        }

        return new Sha256Hash(target);
    }

    public static double getDifficultyForHash(Sha256Hash blockHash)
    {
        String hashString = blockHash.toString();
        BigInteger diffTarget;

        diffTarget = new BigInteger(hashString, RADIX_HEX);

        return DIFFICULTY_ONE.divide(diffTarget).doubleValue();
    }

    private static void printTarget(int diff)
    {
        String diffStr = "" + diff;

        while (diffStr.length() < 10)
            diffStr += " ";

        System.out.println(" " + diffStr + " - " + getTargetForDifficulty(diff));
    }

    private static void printHashDifficulty(String stringHash)
    {
        Sha256Hash hash = new Sha256Hash(stringHash);
        String diffStr = "" + stringHash;

        while (diffStr.length() < 10)
            diffStr += " ";

        System.out.println(" " + diffStr + " - " + getDifficultyForHash(hash));
    }

    public static void main(String args[])
    throws Exception
    {
        printTarget(1);
        printTarget(2);
        printTarget(3);
        printTarget(4);
        printTarget(32);
        printTarget(65536);

        String[] diffs = new String[] {
            "0000000100000000000000000000000000000000000000000000000000000000",
            "0000000080000000000000000000000000000000000000000000000000000000",
            "0000000055555555555555555555555555555555555555555555555555555555",
            "0000000040000000000000000000000000000000000000000000000000000000",
            "0000000008000000000000000000000000000000000000000000000000000000",
            "0000000000010000000000000000000000000000000000000000000000000000",
            "000000000019d6689c085ae165831e934ff763ae46a2a6c172b3f1b60a8ce26f",
            "00000000000000103da14a7e7a88313266bc4fd5654060a9ee03fa8a7116a78d",

            "00000000324fdf1c06467444e2707e2f4a924ff8deb7c12c853f56bb8d0854ba",
            "0000000000fdd6f0819ba4dc6e6c13020c99dbb7afbe6ca00e98c0efecbda0f1",
            "00000000001d0ee8fa3e564930d12b45c6b6d9266ec2ec7bf75c6def9d8249fb",
            "00000000067dafa73f09c60ac024d268e6ab7e5ca7b0a011e2818f2d476ff65e",
            "0000000000ca6056f3d2eed8d248d97cf9347490830d2598acc0333bdfc492a8",
            "000000000066f7680a954a76014f38d06be8ab69d2e945227faf3495c7dfd7f0"
        };

        for (String diff : diffs)
        {
            printHashDifficulty(diff);
        }
    }
}