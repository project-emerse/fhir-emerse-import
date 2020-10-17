package edu.utah.kmm.emerse.epic

import ca.uhn.fhir.context.FhirContext
import edu.utah.kmm.emerse.auth.AuthenticatorRegistry
import edu.utah.kmm.emerse.auth.BasicAuthenticator
import edu.utah.kmm.emerse.fhir.FhirService
import edu.utah.kmm.emerse.jwt.JwtService
import edu.utah.kmm.emerse.oauth.AccessToken
import edu.utah.kmm.emerse.oauth.OAuth2Authenticator
import edu.utah.kmm.emerse.security.Credentials
import spock.lang.Specification

class EpicAuthenticatorFunctionalSpec extends Specification {
    private static final String CLIENT_ID = 'd4e00029-fb2f-4ec7-9657-f656ac41f48e'
    private static final String API_ROOT = 'https://epiciccode1.med.utah.edu/Interconnect-KMM-CDE/api'
    private static final String FHIR_ROOT = 'https://epiciccode1.med.utah.edu/Interconnect-KMM-CDE/api/FHIR/STU3'
    EpicService epicService
    JwtService jwtService
    EpicAuthenticator auth

    def setup() {
        epicService = new EpicService()
        epicService.clientId = CLIENT_ID
        epicService.apiRoot = API_ROOT
        Credentials epicServiceCredentials = Mock()
        1 * epicServiceCredentials.getUsername() >> 'user'
        1 * epicServiceCredentials.getPassword() >> 'pass'
        epicService.epicServiceCredentials = epicServiceCredentials
        epicService.init()
        jwtService = new JwtService()
        jwtService.home = System.getProperty('user.home')
        jwtService.privateKeyFile = 'emerse-it-cde.pkcs8.pem'
        jwtService.publicKeyFile = 'emerse-it-cde-pub.pem'
        jwtService.privateKeyPass = 'W2DGjR1U3iaxmZ6ASzcC/H0rcQ3jkfgTb3zBRF7QdR3WKVBi3aHIBu0BRWMKenW6'
        auth = new EpicAuthenticator()
        auth.epicService = epicService
        auth.jwtService = jwtService
        auth.clientId = CLIENT_ID
        FhirService fhirService = new FhirService()
        fhirService.fhirContext = FhirContext.forDstu3()
        fhirService.fhirRoot = FHIR_ROOT
        fhirService.authenticationType = 'EPIC'
        fhirService.extraHeaders = ''
        fhirService.authenticatorRegistry = new AuthenticatorRegistry(new BasicAuthenticator(), new OAuth2Authenticator(), auth)
        fhirService.init()
        auth.initialize(fhirService)
    }

    def 'get an access token'() {
        when:
        AccessToken accessToken = auth.generateAccessToken()

        then:
        notThrown(Exception)
        accessToken.access_token
        !accessToken.expired
        accessToken.expires_in
        accessToken.scope
        accessToken.token_type
    }

}
