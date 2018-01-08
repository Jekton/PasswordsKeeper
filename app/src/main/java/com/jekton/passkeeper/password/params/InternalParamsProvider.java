package com.jekton.passkeeper.password.params;

/**
 * @author Jekton
 */

class InternalParamsProvider implements CipherParamsProvider {

    @Override
    public void setExternalKey(boolean uesExternalKey) {
        throw new UnsupportedOperationException();
    }


    @Override
    public byte[] getIv() {
        return new byte[0];
    }


    @Override
    public byte[] getKey(String password) {
        return new byte[0];
    }
}
