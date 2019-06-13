package edu.utah.kmm.emerse.security;

import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.jasypt.encryption.pbe.config.EnvironmentStringPBEConfig;
import org.springframework.util.Assert;

import java.security.Security;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

public class Decryptor {

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    private final StandardPBEStringEncryptor decryptor;

    public Decryptor(String env) {
        this(env, "PBEWITHSHA256AND128BITAES-CBC-BC");
    }

    public Decryptor(String env, String algorithm) {
        String masterKey = System.getenv(env);
        Assert.notNull(masterKey, () -> "Could not retrieve master key from environment variable '" + env + "'");
        decryptor = new StandardPBEStringEncryptor();
        decryptor.setAlgorithm(algorithm);
        decryptor.setPassword(masterKey);
        decryptor.setKeyObtentionIterations(5000);
   }

    public String decrypt(String encryptedString) {
        return decryptor.decrypt(encryptedString);
    }
}
