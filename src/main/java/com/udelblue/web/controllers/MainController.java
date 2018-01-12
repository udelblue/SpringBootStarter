package com.udelblue.web.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class MainController {

	@RequestMapping("/")
	public String home() {
		return "index";
	}

	@RequestMapping("/about")
	public String about() {
		return "main/about";
	}

	@RequestMapping("/team")
	public String team() {
		return "main/team";
	}

	@RequestMapping("/features")
	public String features() {
		return "main/features";
	}

	@RequestMapping("/pricing")
	public String pricing() {
		return "main/pricing";
	}

}
