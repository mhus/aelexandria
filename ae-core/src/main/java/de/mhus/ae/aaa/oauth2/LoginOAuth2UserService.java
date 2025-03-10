/*
 * kt2l-core - kt2l core implementation
 * Copyright © 2024 Mike Hummel (mh@mhus.de)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package de.mhus.ae.aaa.oauth2;

import de.mhus.ae.aaa.AaaUser;
import de.mhus.ae.aaa.AaaUserRepositoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Optional;

// https://www.baeldung.com/spring-security-5-oauth2-login
// https://github.com/callicoder/spring-boot-react-oauth2-social-login-demo/blob/master/spring-social/src/main/java/com/example/springsocial/security/oauth2/CustomOAuth2UserService.java
// !!! https://vaadin.com/blog/oauth-2-and-google-sign-in-for-a-vaadin-application

@Component
public class LoginOAuth2UserService extends DefaultOAuth2UserService {

    @Autowired
    private AaaUserRepositoryService userRepository;
    private AuthProvider authProvider;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest oAuth2UserRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(oAuth2UserRequest);

        try {
            return processOAuth2User(oAuth2UserRequest, oAuth2User);
        } catch (AuthenticationException ex) {
            throw ex;
        } catch (Exception ex) {
            // Throwing an instance of AuthenticationException will trigger the OAuth2AuthenticationFailureHandler
            throw new InternalAuthenticationServiceException(ex.getMessage(), ex.getCause());
        }
    }

    private OAuth2User processOAuth2User(OAuth2UserRequest oAuth2UserRequest, OAuth2User oAuth2User) {

        OAuth2UserInfo oAuth2UserInfo = authProvider.getProvider(oAuth2UserRequest.getClientRegistration().getRegistrationId()).orElseThrow(
                () -> new OAuth2AuthenticationProcessingException("Sorry! Login with " + oAuth2UserRequest.getClientRegistration().getRegistrationId() + " is not supported yet.")
        ).createOAuth2UserInfo(oAuth2User.getAttributes());

        if(StringUtils.isEmpty(oAuth2UserInfo.getEmail())) {
            throw new OAuth2AuthenticationProcessingException("Email not found from OAuth2 provider");
        }

        Optional<AaaUser> userOptional = userRepository.getRepository().getByEmail(oAuth2UserInfo.getEmail());
        AaaUser user;
        if(userOptional.isPresent()) {
            user = userOptional.get();
            if(!user.getProvider().equals(oAuth2UserRequest.getClientRegistration().getRegistrationId())) {
                throw new OAuth2AuthenticationProcessingException("Looks like you're signed up with " +
                        user.getProvider() + " account. Please use your " + user.getProvider() +
                        " account to login.");
            }
            user = updateExistingUser(user, oAuth2UserInfo);
        } else {
            user = registerNewUser(oAuth2UserRequest, oAuth2UserInfo);
        }

        return UserPrincipal.create(user, oAuth2User.getAttributes());
    }

    private AaaUser registerNewUser(OAuth2UserRequest oAuth2UserRequest, OAuth2UserInfo oAuth2UserInfo) {
        AaaUser user = AaaUser.builder()
                .provider(oAuth2UserRequest.getClientRegistration().getRegistrationId())
                .providerId(oAuth2UserInfo.getId())
                .userId(oAuth2UserInfo.getName())
                .email(oAuth2UserInfo.getEmail())
                .imageUrl(oAuth2UserInfo.getImageUrl())
                .build();
        userRepository.getRepository().createUser(user);
        return user;
    }

    private AaaUser updateExistingUser(AaaUser existingUser, OAuth2UserInfo oAuth2UserInfo) {

        AaaUser newUser = AaaUser.builder()
                .userId(oAuth2UserInfo.getName())
                .imageUrl(oAuth2UserInfo.getImageUrl())
                .build();
        return userRepository.getRepository().updateUser(newUser);
    }

}
