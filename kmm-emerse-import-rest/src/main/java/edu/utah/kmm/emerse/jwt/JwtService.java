package edu.utah.kmm.emerse.jwt;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import edu.utah.kmm.emerse.security.Decryptor;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

public class JwtService {

    private static final String RSA = "RSA";

    @Value("${EMERSE_HOME:${user.home}/.emerse}")
    private String home;

    @Value("${app.jwt.pk}")
    private String privateKeyFile;

    @Value("${app.jwt.pubk}")
    private String publicKeyFile;

    @Value("${app.jwt.pk.pass}")
    private String privateKeyPass;

    @Autowired
    private Decryptor decryptor;

    private final Function<PemObject, Supplier<RSAPublicKey>> rsaPublicKey = pem -> () -> {
        try {
            X509EncodedKeySpec spec = new X509EncodedKeySpec(pem.getContent());
            KeyFactory kf = KeyFactory.getInstance(RSA);
            return (RSAPublicKey) kf.generatePublic(spec);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    };

    private final Function<PemObject, Supplier<RSAPrivateKey>> rsaPrivateKey = pem -> () -> {
        try  {
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(pem.getContent());
            KeyFactory kf = KeyFactory.getInstance(RSA);
            return (RSAPrivateKey) kf.generatePrivate(spec);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    };

    private final BiFunction<Supplier<RSAPublicKey>, Supplier<RSAPrivateKey>, Algorithm> algorithm = (pub, priv) ->
            Algorithm.RSA384(pub.get(), priv.get());

    public File getFile(String name) {
        return Paths.get(home, name).toFile();
    }

    public Algorithm getAlgorithm() {
        try (PemReader pubReader = new PemReader(
                new FileReader(Paths.get(home, publicKeyFile).toFile()));
             PemReader privReader = new PemReader(
                     new FileReader(Paths.get(home, privateKeyFile).toFile()))
        ){
            return algorithm.apply(
                    rsaPublicKey.apply(pubReader.readPemObject()),
                    rsaPrivateKey.apply(privReader.readPemObject()));
        } catch(IOException e){
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public String newJwt(String clientId, String tokenUrl) {
        return JWT.create()
                .withIssuer(clientId)
                .withSubject(clientId)
                .withAudience(tokenUrl)
                .withJWTId(UUID.randomUUID().toString())
                .withExpiresAt(Date.from(
                        LocalDateTime.now()
                                .plus(5, ChronoUnit.MINUTES)
                                .atZone(ZoneId.systemDefault())
                                .toInstant()))
                .sign(getAlgorithm());
    }

    public DecodedJWT verify(String jwt, String clientId) {
        JWTVerifier verifier = JWT.require(getAlgorithm())
                .withIssuer(clientId)
                .build();
        return verifier.verify(jwt);
    }
}
