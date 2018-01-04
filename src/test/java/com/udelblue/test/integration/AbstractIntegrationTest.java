package com.udelblue.test.integration;

import org.junit.rules.TestName;
import org.springframework.beans.factory.annotation.Autowired;

import com.udelblue.backend.persistence.domain.backend.Plan;
import com.udelblue.backend.persistence.domain.backend.Role;
import com.udelblue.backend.persistence.domain.backend.User;
import com.udelblue.backend.persistence.domain.backend.UserRole;
import com.udelblue.backend.persistence.repositories.PlanRepository;
import com.udelblue.backend.persistence.repositories.RoleRepository;
import com.udelblue.backend.persistence.repositories.UserRepository;
import com.udelblue.enums.PlansEnum;
import com.udelblue.enums.RolesEnum;
import com.udelblue.utils.UserUtils;

import java.util.HashSet;
import java.util.Set;


public abstract class AbstractIntegrationTest {

    @Autowired
    protected PlanRepository planRepository;

    @Autowired
    protected RoleRepository roleRepository;

    @Autowired
    protected UserRepository userRepository;

    protected Plan createPlan(PlansEnum plansEnum) {
        return new Plan(plansEnum);
    }

    protected Role createRole(RolesEnum rolesEnum) {
        return new Role(rolesEnum);
    }

    protected User createUser(String username, String email) {
        Plan basicPlan = createPlan(PlansEnum.BASIC);
        planRepository.save(basicPlan);

        User basicUser = UserUtils.createBasicUser(username, email);
        basicUser.setPlan(basicPlan);

        Role basicRole = createRole(RolesEnum.BASIC);
        roleRepository.save(basicRole);

        Set<UserRole> userRoles = new HashSet<>();
        UserRole userRole = new UserRole(basicUser, basicRole);
        userRoles.add(userRole);

        basicUser.getUserRoles().addAll(userRoles);
        basicUser = userRepository.save(basicUser);
        return basicUser;
    }

    protected User createUser(TestName testName) {
        return createUser(testName.getMethodName(), testName.getMethodName() + "@udelblue.com");
    }
}
