package edu.utah.kmm.emerse.security;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.springframework.util.Assert;

import java.security.Security;

public class Decryptor {

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    private final StandardPBEStringEncryptor decryptor;

    public Decryptor(String env, String algorithm) {
        if ("none".equalsIgnoreCase(algorithm)) {
            decryptor = null;
        } else {
            String masterKey = System.getenv(env);
            Assert.notNull(masterKey, () -> "Could not retrieve master key from environment variable '" + env + "'");
            decryptor = new StandardPBEStringEncryptor();
            decryptor.setAlgorithm(algorithm);
            decryptor.setPassword(masterKey);
            decryptor.setKeyObtentionIterations(5000);
        }
   }

    public String decrypt(String encryptedString) {
        return decryptor == null ? encryptedString : decryptor.decrypt(encryptedString);
    }
}
