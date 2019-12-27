package com.example;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class Azure2Controller {

	@GetMapping("/azure2")
	public String azure2method() {
		return "from azure 2 project";
	}
	
	@GetMapping("/home")
	public String azure2home() {
		return "same endpoint as azure1 project";
	}
	
	@GetMapping("/home1")
	public String azure3home() {
		return "test ci in github11";
	}
}
