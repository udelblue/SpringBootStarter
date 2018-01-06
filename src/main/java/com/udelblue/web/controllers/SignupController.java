package com.udelblue.web.controllers;

import java.io.IOException;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import com.udelblue.backend.persistence.domain.backend.Plan;
import com.udelblue.backend.persistence.domain.backend.Role;
import com.udelblue.backend.persistence.domain.backend.User;
import com.udelblue.backend.persistence.domain.backend.UserRole;
import com.udelblue.backend.service.PlanService;
import com.udelblue.backend.service.StripeService;
import com.udelblue.backend.service.UserService;
import com.udelblue.enums.PlansEnum;
import com.udelblue.enums.RolesEnum;
import com.udelblue.exceptions.StripeException;
import com.udelblue.utils.StripeUtils;
import com.udelblue.utils.UserUtils;
import com.udelblue.web.domain.frontend.BasicAccountPayload;
import com.udelblue.web.domain.frontend.ProAccountPayload;

@Controller
public class SignupController {

	@Autowired
	private PlanService planService;

	@Autowired
	private UserService userService;

	@Autowired
	private StripeService stripeService;

	/** The application logger */
	private static final Logger LOG = LoggerFactory.getLogger(SignupController.class);

	public static final String SIGNUP_URL_MAPPING = "/signup";

	public static final String PAYLOAD_MODEL_KEY_NAME = "payload";

	public static final String SUBSCRIPTION_VIEW_NAME = "registration/signup";

	public static final String DUPLICATED_USERNAME_KEY = "duplicatedUsername";

	public static final String DUPLICATED_EMAIL_KEY = "duplicatedEmail";

	public static final String SIGNED_UP_MESSAGE_KEY = "signedUp";

	public static final String ERROR_MESSAGE_KEY = "message";

	public static final String GENERIC_ERROR_VIEW_NAME = "error/genericError";

	@RequestMapping(value = SIGNUP_URL_MAPPING, method = RequestMethod.GET)
	public String signupGet(@RequestParam("planId") int planId, ModelMap model) {

		if (planId != PlansEnum.BASIC.getId() && planId != PlansEnum.PRO.getId()) {
			throw new IllegalArgumentException("Plan id is not valid");
		}
		model.addAttribute(PAYLOAD_MODEL_KEY_NAME, new ProAccountPayload());

		return SUBSCRIPTION_VIEW_NAME;
	}

	@RequestMapping(value = SIGNUP_URL_MAPPING, method = RequestMethod.POST)
	public String signUpPost(@RequestParam(name = "planId", required = true) int planId,
			@ModelAttribute(PAYLOAD_MODEL_KEY_NAME) @Valid ProAccountPayload payload, ModelMap model)
			throws IOException {

		if (planId != PlansEnum.BASIC.getId() && planId != PlansEnum.PRO.getId()) {
			model.addAttribute(SIGNED_UP_MESSAGE_KEY, "false");
			model.addAttribute(ERROR_MESSAGE_KEY, "Plan id does not exist");
			return SUBSCRIPTION_VIEW_NAME;
		}

		this.checkForDuplicates(payload, model);

		boolean duplicates = false;

		List<String> errorMessages = new ArrayList<>();

		if (model.containsKey(DUPLICATED_USERNAME_KEY)) {
			LOG.warn("The username already exists. Displaying error to the user");
			model.addAttribute(SIGNED_UP_MESSAGE_KEY, "false");
			errorMessages.add("Username already exist");
			duplicates = true;
		}

		if (model.containsKey(DUPLICATED_EMAIL_KEY)) {
			LOG.warn("The email already exists. Displaying error to the user");
			model.addAttribute(SIGNED_UP_MESSAGE_KEY, "false");
			errorMessages.add("Email already exist");
			duplicates = true;
		}

		if (duplicates) {
			model.addAttribute(ERROR_MESSAGE_KEY, errorMessages);
			return SUBSCRIPTION_VIEW_NAME;
		}

		// There are certain info that the user doesn't set, such as profile image URL,
		// Stripe customer id,
		// plans and roles
		LOG.debug("Transforming user payload into User domain object");
		User user = UserUtils.fromWebUserToDomainUser(payload);

		// Sets the Plan and the Roles (depending on the chosen plan)
		LOG.debug("Retrieving plan from the database");
		Plan selectedPlan = planService.findPlanById(planId);
		if (null == selectedPlan) {
			LOG.error("The plan id {} could not be found. Throwing exception.", planId);
			model.addAttribute(SIGNED_UP_MESSAGE_KEY, "false");
			model.addAttribute(ERROR_MESSAGE_KEY, "Plan id not found");
			return SUBSCRIPTION_VIEW_NAME;
		}
		user.setPlan(selectedPlan);

		User registeredUser = null;

		// By default users get the BASIC ROLE
		Set<UserRole> roles = new HashSet<>();
		if (planId == PlansEnum.BASIC.getId()) {
			roles.add(new UserRole(user, new Role(RolesEnum.BASIC)));
			registeredUser = userService.createUser(user, PlansEnum.BASIC, roles);
			// Auto logins the registered user
			Authentication auth = new UsernamePasswordAuthenticationToken(registeredUser, null,
					registeredUser.getAuthorities());
			SecurityContextHolder.getContext().setAuthentication(auth);
			LOG.info("User created successfully");
			return "redirect:/payload";
			/*
			 * model.addAttribute(SIGNED_UP_MESSAGE_KEY, "true");
			 * 
			 * return SUBSCRIPTION_VIEW_NAME;
			 */

		} else {
			roles.add(new UserRole(user, new Role(RolesEnum.BASIC)));
			registeredUser = userService.createUser(user, PlansEnum.BASIC, roles);
			Authentication auth = new UsernamePasswordAuthenticationToken(registeredUser, null,
					registeredUser.getAuthorities());
			SecurityContextHolder.getContext().setAuthentication(auth);
			return "redirect:/payment?planId=" + Integer.toString(planId);
		}

	}

	@ExceptionHandler({ StripeException.class })
	public ModelAndView signupException(HttpServletRequest request, Exception exception) {

		LOG.error("Request {} raised exception {}", request.getRequestURL(), exception);

		ModelAndView mav = new ModelAndView();
		mav.addObject("exception", exception);
		mav.addObject("url", request.getRequestURL());
		mav.addObject("timestamp", LocalDate.now(Clock.systemUTC()));
		mav.setViewName(GENERIC_ERROR_VIEW_NAME);
		return mav;
	}

	// --------------> Private methods

	/**
	 * Checks if the username/email are duplicates and sets error flags in the
	 * model. Side effect: the method might set attributes on Model
	 **/
	private void checkForDuplicates(BasicAccountPayload payload, ModelMap model) {

		// Username
		if (userService.findByUserName(payload.getUsername()) != null) {
			model.addAttribute(DUPLICATED_USERNAME_KEY, true);
		}
		if (userService.findByEmail(payload.getEmail()) != null) {
			model.addAttribute(DUPLICATED_EMAIL_KEY, true);
		}

	}
}
