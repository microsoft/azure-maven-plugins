package com.microsoft.itlib;

import com.microsoft.itlib.AbstractSigner;

public class ConcreteSigner extends AbstractSigner {

  @Override
  public byte[] getSignature(byte[] content){
    return   new byte[] { (byte) 0x90, (byte) 0x0d, (byte) 0xc0, (byte) 0xde };    
  }
}
