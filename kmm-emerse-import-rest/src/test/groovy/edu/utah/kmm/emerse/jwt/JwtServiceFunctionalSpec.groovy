package edu.utah.kmm.emerse.jwt


import com.auth0.jwt.interfaces.DecodedJWT
import spock.lang.Specification

class JwtServiceFunctionalSpec extends Specification {

    private JwtService service

    def 'test loading private key pem'() {
        given:
        String clientId = '12345'
        String tokenUrl = 'http://something'

        and:
        service = new JwtService()
        service.home = System.getenv('EMERSE_HOME')
        service.home = service.home ? service.home : (System.getProperty('user.home') + '/.emerse')
        service.privateKeyFile = 'emerse-it-cde.pkcs8.pem'
        service.publicKeyFile = 'emerse-it-cde-pub.pem'
        service.privateKeyPass = 'W2DGjR1U3iaxmZ6ASzcC/H0rcQ3jkfgTb3zBRF7QdR3WKVBi3aHIBu0BRWMKenW6'
        int limit = 1

        when:
        String jwt = service.newJwt(clientId, tokenUrl)
        jwt = service.newJwt(clientId, tokenUrl)
        long t0 = System.nanoTime()
        (0..limit).each {
            jwt = service.newJwt(clientId, tokenUrl)
        }
        println ""
        println "${(System.nanoTime()-t0)/1e6/limit} ms"
        println jwt

        then:
        notThrown(Exception)

        when:
        t0 = System.nanoTime()
        DecodedJWT decoded = service.verify(jwt, clientId)
        (0..limit).each {
            decoded = service.verify(jwt, clientId)
        }
        println "${(System.nanoTime()-t0)/1e6/limit} ms"

        then:
        notThrown(Exception)
        decoded.getIssuer() == clientId
        decoded.getSubject() == clientId
        decoded.getAudience()?.get(0) == tokenUrl
        decoded.getId() =~ '[a-z0-9]{8}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{12}'
//        decoded.getExpiresAt() as String == Date.from(LocalDateTime.now().plus(5, ChronoUnit.MINUTES).atZone(ZoneId.systemDefault()).toInstant()) as String
    }

}
