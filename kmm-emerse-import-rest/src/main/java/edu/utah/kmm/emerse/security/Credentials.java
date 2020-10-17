package edu.utah.kmm.emerse.security;

import org.springframework.beans.factory.annotation.Autowired;

/**
 * Stores encrypted credentials.
 */
public class Credentials {

    @Autowired()
    private Decryptor decryptor;

    private final String encryptedUsername;

    private final String encryptedPassword;

    public Credentials(String encryptedUsername, String encryptedPassword) {
        this.encryptedUsername = encryptedUsername;
        this.encryptedPassword = encryptedPassword;
    }

    public String getUsername() {
        return decryptor.decrypt(encryptedUsername);
    }

    public String getPassword() {
        return decryptor.decrypt(encryptedPassword);
    }
}
