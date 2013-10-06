package com.github.fireduck64.sockthing.util;
import java.math.BigInteger;

import com.google.bitcoin.core.Sha256Hash;

public class DiffMath
{
  private static final int RADIX_HEX = 16;
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

  public static double getDifficultyForTarget(Sha256Hash blockHash)
  {
    String        hashString  = blockHash.toString();
    BigInteger    diffTarget;

    diffTarget = new BigInteger(hashString, RADIX_HEX);

    return DIFFICULTY_ONE.divide(diffTarget).doubleValue();
  }

  private static void printTarget(int diff)
  {
    String diffStr = "" + diff;

    while (diffStr.length() < 10)
      diffStr +=" ";

    System.out.println(" " + diffStr + " - " + getTargetForDifficulty(diff));
  }

  private static void printDiff(String stringHash)
  {
    Sha256Hash  hash      = new Sha256Hash(stringHash);
    String      diffStr  = "" + stringHash;

    while (diffStr.length() < 10)
      diffStr+=" ";

    System.out.println(" " + diffStr + " - " + getDifficultyForTarget(hash));
  }

  public static void main(String args[])
  {
    printTarget(1);
    printTarget(2);
    printTarget(3);
    printTarget(4);
    printTarget(32);
    printTarget(65536);

    printDiff("0000000100000000000000000000000000000000000000000000000000000000");
    printDiff("0000000080000000000000000000000000000000000000000000000000000000");
    printDiff("0000000055555555555555555555555555555555555555555555555555555555");
    printDiff("0000000040000000000000000000000000000000000000000000000000000000");
    printDiff("0000000008000000000000000000000000000000000000000000000000000000");
    printDiff("0000000000010000000000000000000000000000000000000000000000000000");
    printDiff("000000000019d6689c085ae165831e934ff763ae46a2a6c172b3f1b60a8ce26f");
    printDiff("00000000000000103da14a7e7a88313266bc4fd5654060a9ee03fa8a7116a78d");
  }
}