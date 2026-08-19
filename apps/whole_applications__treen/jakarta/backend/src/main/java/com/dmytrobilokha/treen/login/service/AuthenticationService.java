package com.dmytrobilokha.treen.login.service;

import com.dmytrobilokha.treen.infra.db.DbException;
import com.dmytrobilokha.treen.infra.exception.InternalApplicationException;
import com.dmytrobilokha.treen.login.persistence.UserEntity;
import com.dmytrobilokha.treen.login.persistence.UserRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import javax.annotation.CheckForNull;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;

@ApplicationScoped
@Transactional(rollbackOn = Exception.class)
public class AuthenticationService implements AuthenticationServiceMXBean {

    private static final int HASHING_ITERATIONS = 350000;
    private static final int PBE_KEY_BITS = 128;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private UserRepository userRepository;

    public AuthenticationService() {
        //CDI
    }

    @Inject
    public AuthenticationService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @CheckForNull
    public UserEntity checkUserCredentials(String login, char[] password) throws InternalApplicationException {
        UserEntity userEntity;
        try {
            userEntity = userRepository.findUserByLogin(login);
        } catch (DbException e) {
            throw new InternalApplicationException("Db error while authenticating user", e);
        }
        if (userEntity == null) {
            //User with given login wasn't found
            return null;
        }
        var calculatedHash = calculatePasswordHash(password, userEntity.getPasswordSalt());
        if (!Arrays.equals(calculatedHash, userEntity.getPasswordHash())) {
            //Provided password is wrong
            return null;
        }
        return userEntity;
    }

    @Override
    public void createUser(String login, String password) throws InternalApplicationException {
        var salt = generateSalt();
        try {
            userRepository.insertUser(login, calculatePasswordHash(password.toCharArray(), salt), salt);
        } catch (DbException e) {
            throw new InternalApplicationException("Failed to store user in the DB", e);
        }
    }

    @Override
    public int changeUserPassword(String login, String password) throws InternalApplicationException {
        var salt = generateSalt();
        try {
            return userRepository.updateUserPassword(login, calculatePasswordHash(password.toCharArray(), salt), salt);
        } catch (DbException e) {
            throw new InternalApplicationException("Failed to store user in the DB", e);
        }
    }

    private byte[] generateSalt() {
        var salt = new byte[16];
        SECURE_RANDOM.nextBytes(salt);
        return salt;
    }

    private byte[] calculatePasswordHash(char[] password, byte[] salt) throws InternalApplicationException {
        SecretKeyFactory keyFactory;
        try {
            keyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512");
        } catch (NoSuchAlgorithmException e) {
            throw new InternalApplicationException("Failed to create SecretKeyFactory", e);
        }
        var spec = new PBEKeySpec(password, salt, HASHING_ITERATIONS, PBE_KEY_BITS);
        try {
            return keyFactory.generateSecret(spec).getEncoded();
        } catch (InvalidKeySpecException e) {
            throw new InternalApplicationException("Unable to calculate passwords hash", e);
        }
    }

}
