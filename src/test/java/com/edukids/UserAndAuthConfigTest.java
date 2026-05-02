package com.edukids;

import com.edukids.entities.User;
import com.edukids.enums.Role;
import com.edukids.services.GoogleAuthService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UserAndAuthConfigTest {

    @Test
    void role_parsesLegacyAndDatabaseValues() {
        assertEquals(Role.ROLE_ADMIN, Role.fromDbValue("ROLE_ADMIN"));
        assertEquals(Role.ROLE_PARENT, Role.fromDbValue("Parent"));
        assertEquals(Role.ROLE_ELEVE, Role.fromDbValue("USER"));
    }

    @Test
    void role_rejectsUnknownValues() {
        assertThrows(IllegalArgumentException.class, () -> Role.fromDbValue("SUPER_USER"));
    }

    @Test
    void user_rolesRoundTripThroughJson() {
        User user = new User("child@example.com", "secret", "Ada", "Lovelace",
                List.of(Role.ROLE_ELEVE, Role.ROLE_PARENT));

        List<Role> roles = User.rolesFromJson(user.rolesToJson());

        assertEquals(List.of(Role.ROLE_ELEVE, Role.ROLE_PARENT), roles);
    }

    @Test
    void user_defaultsToStudentRoleWhenRolesAreMissing() {
        User user = new User();

        assertEquals(Role.ROLE_ELEVE, user.getPrimaryRole());
        assertEquals(List.of(Role.ROLE_ELEVE), User.rolesFromJson(""));
    }

    @Test
    void googleAuthService_detectsBundledClientSecret() {
        boolean resourceExists = GoogleAuthService.class.getResource("/client_secret.json") != null;

        assertEquals(resourceExists, new GoogleAuthService().isConfigured());
    }
}
