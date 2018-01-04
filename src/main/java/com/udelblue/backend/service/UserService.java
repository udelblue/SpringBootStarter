package com.udelblue.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.udelblue.backend.persistence.domain.backend.PasswordResetToken;
import com.udelblue.backend.persistence.domain.backend.Plan;
import com.udelblue.backend.persistence.domain.backend.User;
import com.udelblue.backend.persistence.domain.backend.UserRole;
import com.udelblue.backend.persistence.repositories.PasswordResetTokenRepository;
import com.udelblue.backend.persistence.repositories.PlanRepository;
import com.udelblue.backend.persistence.repositories.RoleRepository;
import com.udelblue.backend.persistence.repositories.UserRepository;
import com.udelblue.enums.PlansEnum;

import java.util.Set;

@Service
@Transactional(readOnly = true)
public class UserService {

	@Autowired
	private RoleRepository roleRepository;

	@Autowired
	private PlanRepository planRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private BCryptPasswordEncoder passwordEncoder;

	@Autowired
	private PasswordResetTokenRepository passwordResetTokenRepository;

	/** The application logger */
	private static final Logger LOG = LoggerFactory.getLogger(UserService.class);

	@Transactional
	public User createUser(User user, PlansEnum plansEnum, Set<UserRole> userRoles) {

		User localUser = userRepository.findByEmail(user.getEmail());

		if (localUser != null) {
			LOG.info("User with username {} and email {} already exist. Nothing will be done. ", user.getUsername(),
					user.getEmail());
		} else {

			String encryptedPassword = passwordEncoder.encode(user.getPassword());
			user.setPassword(encryptedPassword);

			Plan plan = new Plan(plansEnum);
			// It makes sure the plans exist in the database
			if (!planRepository.exists(plansEnum.getId())) {
				plan = planRepository.save(plan);
			}

			user.setPlan(plan);

			for (UserRole ur : userRoles) {
				roleRepository.save(ur.getRole());
			}

			user.getUserRoles().addAll(userRoles);

			localUser = userRepository.save(user);

		}

		return localUser;

	}

	/**
	 * Returns a user by username or null if a user could not be found.
	 * 
	 * @param username
	 *            The username to be found
	 * @return A user by username or null if a user could not be found.
	 */
	public User findByUserName(String username) {
		return userRepository.findByUsername(username);
	}

	/**
	 * Returns a user for the given email or null if a user could not be found.
	 * 
	 * @param email
	 *            The email associated to the user to find.
	 * @return a user for the given email or null if a user could not be found.
	 */
	public User findByEmail(String email) {
		return userRepository.findByEmail(email);
	}

	@Transactional
	public void updateUserPassword(long userId, String password) {
		password = passwordEncoder.encode(password);
		userRepository.updateUserPassword(userId, password);
		LOG.debug("Password updated successfully for user id {} ", userId);

		Set<PasswordResetToken> resetTokens = passwordResetTokenRepository.findAllByUserId(userId);
		if (!resetTokens.isEmpty()) {
			passwordResetTokenRepository.delete(resetTokens);
		}
	}

}
