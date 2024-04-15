package org.example.bootstrapper.Controller;

import lombok.extern.log4j.Log4j2;
import org.example.bootstrapper.Enum.Role;
import org.example.bootstrapper.model.User;
import org.example.bootstrapper.services.AuthenticationService;
import org.example.bootstrapper.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
@Log4j2
@RestController
@RequestMapping("/bootstrapper")
public class UserController {

    private final UserService userService;

    private final AuthenticationService authenticationService;

    @Autowired
    public UserController(UserService userService, AuthenticationService authenticationService){
        this.userService = userService;
        this.authenticationService = authenticationService;
    }

    @PostMapping("/add/customer")
    public String addCustomer(@RequestParam("accountNumber") String accountNumber,
                          @RequestParam("password") String password,
                          @RequestParam("adminUsername") String adminUsername,
                          @RequestParam("adminPassword") String adminPassword) {
        if(!authenticationService.authenticateAdmin(adminUsername, adminPassword)){
            return "User is not authorized";
        }
        log.info("Received request to register a new customer with account number: " + accountNumber);
        User user = new User(accountNumber, password, Role.NORMAL_USER);
        if (authenticationService.customerExists(user)) {
            return "Customer already exists";
        }
        userService.addUser(user,adminUsername,adminPassword);
        return "Customer has been added successfully";
    }

    @DeleteMapping ("/delete/customer")
    public String deleteCustomer(@RequestParam("accountNumber") String accountNumber,
                                 @RequestParam("adminUsername") String adminUsername,
                                 @RequestParam("adminPassword") String adminPassword) {

        if(!authenticationService.authenticateAdmin(adminUsername, adminPassword)){
            return "User is not authorized";
        }

        log.info("Received request to delete the customer with account number: " + accountNumber);
        userService.deleteUser(accountNumber,adminUsername,adminPassword);
        return "customer has been deleted successfully";
    }


    @PostMapping("/add/admin")
    public String addAdmin(@RequestParam("username") String username,
                           @RequestParam("password") String password) {
        log.info("Received request to register the admin with username: " + username);
        if(authenticationService.adminExists(username)){
            return "Admin already exists";
        }
        User admin = new User(username, password, Role.ADMIN);
        userService.addAdmin(admin);
        return "admin has been added successfully";
    }
}