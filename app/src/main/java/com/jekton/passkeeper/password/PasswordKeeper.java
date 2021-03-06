package com.jekton.passkeeper.password;


import android.support.v4.util.Pair;

import com.jekton.passkeeper.util.CipherUtil;
import com.jekton.passkeeper.util.FileUtil;
import com.orhanobut.logger.Logger;

import java.nio.charset.Charset;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;


/**
 * @author Jekton
 */

class PasswordKeeper {

    public interface OnPasswordChangedListener {
        void onDecodeFail();
        void onPasswordChanged(List<Pair<String, String>> passwords);
    }


    private static final String TAG = "PasswordKeeper";
    private final String MAGIC = "PassKeeper";
    private final String STORED_PASSWORD_SEPARATOR = "\r\n";

    private final OnPasswordChangedListener mListener;
    private final String mPasswordFile;
    private final List<Pair<String, String>> mPasswords;

    private boolean mModified;
    private String mPassword;


    public PasswordKeeper(OnPasswordChangedListener listener, String passwordFile) {
        mListener = listener;
        mPasswordFile = passwordFile;
        mPasswords = new ArrayList<>();
    }


    public void destroy() {
        mPassword = null;
        mPasswords.clear();
    }


    public String getPasswordFile() {
        return mPasswordFile;
    }


    public void setPassword(String password) {
        mPassword = password;
    }


    public boolean isPasswordSet() {
        return mPassword != null;
    }


    public boolean storePasswords()
            throws NoSuchAlgorithmException, IllegalBlockSizeException,
            InvalidKeyException, BadPaddingException,
            InvalidAlgorithmParameterException, NoSuchPaddingException {
        if (!mModified || mPassword == null) return true;

        String data = encode();
        byte[] encrypt = CipherUtil.encrypt(data.getBytes(Charset.forName("UTF-8")),
                                            CipherParamsKeeper.getKey(mPassword),
                                            CipherParamsKeeper.getIv());
        boolean success = FileUtil.writeFile(mPasswordFile, encrypt);
        if (success) {
            mModified = false;
        }
        return success;
    }


    public boolean addPassword(String key, String password) {
        if (findPassword(key) == null) {
            mPasswords.add(new Pair<>(key, password));
            mModified = true;
            mListener.onPasswordChanged(mPasswords);
            return true;
        }
        return false;
    }


    public boolean updatePassword(String key, String password) {
        int index = findPasswordIndex(key);
        if (index < mPasswords.size()) {
            mPasswords.set(index, new Pair<>(key, password));
            mModified = true;
            return true;
        }
        return false;
    }


    public boolean removePassword(String key) {
        int index = findPasswordIndex(key);
        if (index < mPasswords.size()) {
            mPasswords.remove(index);
            mModified = true;
            mListener.onPasswordChanged(mPasswords);
            return true;
        } else {
            return false;
        }
    }


    public void loadPasswords()
            throws NoSuchAlgorithmException, IllegalBlockSizeException,
            InvalidKeyException, BadPaddingException,
            InvalidAlgorithmParameterException, NoSuchPaddingException {
        if (mPassword == null) return;

        byte[] data = FileUtil.readFile(mPasswordFile);
        if (data.length == 0) {
            mListener.onPasswordChanged(mPasswords);
            return;
        }

        byte[] key = CipherParamsKeeper.getKey(mPassword);
        byte[] decrypted = CipherUtil.decrypt(data, key, CipherParamsKeeper.getIv());
        decode(decrypted);
        mListener.onPasswordChanged(mPasswords);
    }


    private void decode(byte[] data) {
        String str = new String(data, Charset.forName("UTF-8"));
        str = str.trim();
        String[] passwords = str.split(STORED_PASSWORD_SEPARATOR);
        if (passwords.length % 2 != 1) {
            Logger.e("Password data corrupted");
            mListener.onDecodeFail();
            return;
        }
        if (!passwords[0].equals(MAGIC)) {
            Logger.e("Unexpected magic, expect " + MAGIC + ", but got " + passwords[0]);
            mListener.onDecodeFail();
            return;
        }

        mPasswords.clear();
        for (int i = 1; i < passwords.length; i += 2) {
            Pair<String, String> pair = new Pair<>(passwords[i], passwords[i + 1]);
            mPasswords.add(pair);
        }
    }


    private String encode() {
        StringBuilder builder = new StringBuilder();
        builder.append(MAGIC);
        for (Pair<String, String> pair : mPasswords) {
            builder.append(STORED_PASSWORD_SEPARATOR);
            builder.append(pair.first);
            builder.append(STORED_PASSWORD_SEPARATOR);
            builder.append(pair.second);
        }
        return builder.toString();
    }


    private int findPasswordIndex(String key) {
        int index;
        for (index = 0; index < mPasswords.size(); ++index) {
            if (mPasswords.get(index).first.equals(key)) {
                break;
            }
        }
        return index;
    }


    private Pair<String, String> findPassword(String key) {
        int index = findPasswordIndex(key);
        if (index < mPasswords.size()) {
            return mPasswords.get(index);
        }
        return null;
    }
}
