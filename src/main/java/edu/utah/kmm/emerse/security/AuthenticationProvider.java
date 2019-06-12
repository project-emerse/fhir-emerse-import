package edu.utah.kmm.emerse.security;

import edu.utah.kmm.emerse.database.DatabaseService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.authentication.WebAuthenticationDetails;

import java.util.Collections;

/**
 * Provides authentication support for the framework. Takes provided authentication credentials and
 * authenticates them against the database.
 */
public class AuthenticationProvider implements org.springframework.security.authentication.AuthenticationProvider {
    
    private static final Log log = LogFactory.getLog(AuthenticationProvider.class);

    @Autowired
    private DatabaseService databaseService;

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
    
    /**
     * Produces a trusted <code>UsernamePasswordAuthenticationToken</code> if authentication was
     * successful.
     *
     * @param authentication The authentication context.
     * @return Authentication object if authentication succeeded.
     * @throws AuthenticationException Exception on authentication failure.
     */
    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        WebAuthenticationDetails details = (WebAuthenticationDetails) authentication.getDetails();
        String username = (String) authentication.getPrincipal();
        String password = (String) authentication.getCredentials();

        if (log.isDebugEnabled()) {
            log.debug("User: " + username);
            log.debug("Details, RA: " + (details == null ? "null" : details.getRemoteAddress()));
        }
        
        if (username == null || password == null) {
            throw new BadCredentialsException("Missing security credentials.");
        }

        if (!databaseService.authenticate(username, password)) {
            throw new BadCredentialsException("Invalid login credentials.");
        }

        User principal = new User(username, password, Collections.emptyList());
        authentication = new UsernamePasswordAuthenticationToken(principal, principal.getPassword(),
                principal.getAuthorities());
        ((UsernamePasswordAuthenticationToken) authentication).setDetails(details);
        return authentication;
    }
    
}
