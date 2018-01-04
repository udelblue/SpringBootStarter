package com.udelblue.test.integration;

import org.junit.rules.TestName;
import org.springframework.beans.factory.annotation.Autowired;

import com.udelblue.backend.persistence.domain.backend.Role;
import com.udelblue.backend.persistence.domain.backend.User;
import com.udelblue.backend.persistence.domain.backend.UserRole;
import com.udelblue.backend.service.UserService;
import com.udelblue.enums.PlansEnum;
import com.udelblue.enums.RolesEnum;
import com.udelblue.utils.UserUtils;

import java.util.HashSet;
import java.util.Set;


public abstract class AbstractServiceIntegrationTest {
    @Autowired
    protected UserService userService;

    protected User createUser(TestName testName) {
        String username = testName.getMethodName();
        String email = testName.getMethodName() + "@udelblue.com";

        Set<UserRole> userRoles = new HashSet<>();
        User basicUser = UserUtils.createBasicUser(username, email);
        userRoles.add(new UserRole(basicUser, new Role(RolesEnum.BASIC)));

        return userService.createUser(basicUser, PlansEnum.BASIC, userRoles);
    }
}
