package com.microsoft.itlib;

public abstract class AbstractSigner {
  public abstract byte[] getSignature(byte[] content);
}
