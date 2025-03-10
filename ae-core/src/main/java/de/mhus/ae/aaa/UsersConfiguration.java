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

package de.mhus.ae.aaa;

import de.mhus.commons.tree.MTree;
import de.mhus.commons.tree.TreeNodeList;
import de.mhus.ae.config.AbstractSingleConfig;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class UsersConfiguration extends AbstractSingleConfig {

    public enum ROLE {
        READ,
        WRITE,
        SETTINGS,
        LOCAL,
        ADMIN
    }

    public UsersConfiguration() {
        super("users");
    }

    public List<User> getUsers() {
        final var users = new ArrayList<User>();
        final var userConfig = config().getArray("users");
        if (userConfig.isPresent())
            userConfig.get().forEach(user -> {
                users.add(new User(
                        user.getString("id").get(),
                        user.getString("name").get(),
                        user.getString("password").get(),
                        MTree.getArrayValueStringList(user.getArray("roles").orElse(new TreeNodeList("", null)))
                ));
            });
        return users;
    }

    public boolean allowCreateUsers() {
        return config().getBoolean("allowCreateUsers", false);
    }

    public boolean allowUpdateUsers() {
        return config().getBoolean("allowUpdateUsers", false);
    }

    public boolean allowDeleteUsers() {
        return config().getBoolean("allowDeleteUsers", false);
    }

    public record User (String id, String name, String password, List<String> roles) {
    }
}
