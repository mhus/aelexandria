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

import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.spring.security.AuthenticationContext;
import de.mhus.commons.tools.MFile;
import de.mhus.commons.tools.MString;
import de.mhus.ae.aaa.AaaUser;
import de.mhus.ae.aaa.AaaUserRepositoryService;
import de.mhus.ae.aaa.LoginConfiguration;
import de.mhus.ae.aaa.UserDetailsManagerToUserRepositoryService;
import de.mhus.ae.config.Configuration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;

import static de.mhus.commons.tools.MLang.tryThis;

@Slf4j
@Component
public class AuthProvider {

    public static final String LOCAL_AUTH_PROVIDER_ID = "local";
    @Autowired
    private LoginConfiguration loginConfiguration;
    @Autowired
    private Configuration configuration;
    @Autowired
    private List<OAuth2AuthProvider> providerList;
    private String redirectUrl;
    private Map<String,OAuth2AuthProvider> providers;
    @Autowired
    private transient AuthenticationContext authContext;
    @Autowired
    private AaaUserRepositoryService userRepository;

    @PostConstruct
    public void init() {
        providers = providerList.stream()
                .collect(Collectors.toMap(OAuth2AuthProvider::getRegistrationId, p -> p));
        providers.put(LOCAL_AUTH_PROVIDER_ID, new LocalAuthProvider());
    }

    public Optional<OAuth2AuthProvider> getProvider(String providerId) {
        return Optional.ofNullable(providers.get(providerId));
    }

    public List<OAuth2AuthProvider> getAuthProviders() {
        return loginConfiguration.getOAuth2Providers()
                .stream()
                .filter(p -> providers.containsKey(p.getId()))
                .filter(p -> !p.getId().equals(LOCAL_AUTH_PROVIDER_ID))
                .map(p -> providers.get(p.getId()))
                .toList();
    }

    public String getProividerLoginUrl(OAuth2AuthProvider provider) {
        return MString.substitute(
                provider.getLoginUrlTemplate(),
                "baseUrl", "",
                "redirectUrl", getRedirectUrl());
    }

    public String getRedirectUrl() {
        if (redirectUrl == null) {
            redirectUrl = tryThis(() -> {
                VaadinServletRequest request = VaadinServletRequest.getCurrent();
                return request.getHttpServletRequest().getHeader("origin");
                // return request.getRequestURL().toString();
            }).orElse(null);
        }
        return redirectUrl;
    }

    public Optional<AaaUser> fetchUserFromContext() {
        try {
            AaaUser aaaUser;
            try {
                UserDetails userDetails = authContext.getAuthenticatedUser(UserDetails.class).get();
                if (userDetails == null)
                    return Optional.empty();
                var maybeAaaUser = userFromUserDetails(userDetails);
                if (maybeAaaUser == null || maybeAaaUser.isEmpty())
                    return Optional.empty();
                aaaUser = maybeAaaUser.get();
            } catch (ClassCastException e) {
                DefaultOidcUser oidcUser = authContext.getAuthenticatedUser(DefaultOidcUser.class).get();
                if (oidcUser == null)
                    return Optional.empty();
                var maybeAaaUser = userFromOidcUser(oidcUser);
                if (maybeAaaUser == null || maybeAaaUser.isEmpty())
                    return Optional.empty();
                aaaUser = maybeAaaUser.get();
                // accept user
                var accept = acceptUser(aaaUser);
                if (accept == null) {
                    LOGGER.debug("User {} not accepted", aaaUser.getEmail());
                    return Optional.empty();
                }
                // add default roles for match
                aaaUser = mergeRoles(aaaUser, accept.getDefaultRoles());
                // check if user home exists and create if not
                if (accept.getUserConfigPreset() != null) {
                    var presetDir = configuration.getPresetConfigurationDirectory(accept.getUserConfigPreset());
                    if (presetDir.exists()) {
                        var userConfigDir = configuration.getLocalUserConfigurationDirectory(aaaUser.getUserId());
                        if (!userConfigDir.exists()) {
                            LOGGER.info("Copy/Create user config from {} to {}", presetDir, userConfigDir);
                            MFile.copyDir(presetDir, userConfigDir);
                        }
                    } else {
                        LOGGER.warn("Preset directory {} not found", presetDir);
                    }
                }
                // create if not exists
                if (userRepository.getRepository().getUserByUserId(aaaUser.getUserId()).isEmpty()) {
                    aaaUser = userRepository.getRepository().createUser(aaaUser);
                } else {
                    aaaUser = userRepository.getRepository().updateUser(aaaUser);
                }
            }
            return Optional.of(aaaUser);
        } catch (NoSuchElementException e) {
            LOGGER.debug("No user authenticated with exception: {}", e.getMessage());
            // no user authenticated
            return Optional.empty();
        }
    }

    private AaaUser mergeRoles(AaaUser aaaUser, List<String> defaultRoles) {
        var roles = new HashSet<>(aaaUser.getRoles());
        roles.addAll(defaultRoles);
        return AaaUser.update(aaaUser, AaaUser.builder().roles(roles).build());
    }

    private LoginConfiguration.OAuthAccepted acceptUser(AaaUser aaaUser) {
        for ( var accept : loginConfiguration.getOAuth2Accept())
            if (accept.accept(aaaUser))
                return accept;
        return null;
    }

    private Optional<AaaUser> userFromOidcUser(DefaultOidcUser userDetails) {
        return providerList.stream().filter(p -> p.canHandle(userDetails))
                .map(p -> p.createAaaUser(userDetails)).findFirst();
    }

    private Optional<AaaUser> userFromUserDetails(UserDetails userDetails) {
        return Optional.of(UserDetailsManagerToUserRepositoryService.toAaaUser(userDetails, true));
    }

    private class LocalAuthProvider implements OAuth2AuthProvider {

        @Override
        public String getRegistrationId() {
            return AuthProvider.LOCAL_AUTH_PROVIDER_ID;
        }

        @Override
        public String getTitle() {
            return "Local";
        }

        @Override
        public String getLoginUrlTemplate() {
            return null;
        }

        @Override
        public OAuth2UserInfo createOAuth2UserInfo(Map<String, Object> attributes) {
            return null;
        }

        @Override
        public String getImageResourcePath() {
            return null;
        }

        @Override
        public boolean canHandle(DefaultOidcUser userDetails) {
            return false;
        }

        @Override
        public AaaUser createAaaUser(DefaultOidcUser userDetails) {
            return null;
        }

    }
}
