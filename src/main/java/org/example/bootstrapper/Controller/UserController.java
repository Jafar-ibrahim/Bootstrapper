package org.example.bootstrapper.Controller;

import lombok.extern.log4j.Log4j2;
import org.example.bootstrapper.Enum.Role;
import org.example.bootstrapper.LoadBalancer.LoadBalancer;
import org.example.bootstrapper.Model.Node;
import org.example.bootstrapper.Model.User;
import org.example.bootstrapper.Model.UserDTO;
import org.example.bootstrapper.Service.AuthenticationService;
import org.example.bootstrapper.Service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Log4j2
@RestController
@RequestMapping("/bootstrapper/users")
public class UserController {

    private final UserService userService;
    private final LoadBalancer loadBalancer;
    private final AuthenticationService authenticationService;

    @Autowired
    public UserController(UserService userService, LoadBalancer loadBalancer, AuthenticationService authenticationService) {
        this.userService = userService;
        this.loadBalancer = loadBalancer;
        this.authenticationService = authenticationService;
    }
    @PreAuthorize("@authenticationService.authenticateAdmin(#adminUsername, #adminPassword)")
    @PostMapping("/{username}")
    public ResponseEntity<String> addUser(@PathVariable("username") String username,
                                          @RequestHeader("password") String password,
                                          @RequestHeader("role") Role role,
                                          @RequestHeader("adminUsername") String adminUsername,
                                          @RequestHeader("adminPassword") String adminPassword) {

        log.info("Received request to register a new user with username: " + username+" and role: "+role);
        if(!authenticationService.authenticateAdmin(adminUsername, adminPassword)){
            log.error("User is not authorized");
            return new ResponseEntity<>("User is not authorized", HttpStatus.UNAUTHORIZED);
        }

        User user = new User(username, password, role);
        if (authenticationService.userExists(username) || authenticationService.adminExists(username)) {
            log.error("User already exists");
            return new ResponseEntity<>("User already exists", HttpStatus.CONFLICT);
        }
        if (role == Role.ADMIN ) {
            userService.addAdmin(user,adminUsername,adminPassword);
        }else {
            userService.addUser(user,adminUsername,adminPassword);
        }
        return new ResponseEntity<>("User has been added successfully with username: " + username+" and role: "+role, HttpStatus.CREATED);
    }

    @PreAuthorize("@authenticationService.authenticateAdmin(#adminUsername, #adminPassword)")
    @DeleteMapping ("/{username}")
    public ResponseEntity<String> deleteUser(@PathVariable("username") String username,
                                             @RequestHeader("adminUsername") String adminUsername,
                                             @RequestHeader("adminPassword") String adminPassword) {

        log.info("Received request to delete the user with username: " + username);
        if(!authenticationService.authenticateAdmin(adminUsername, adminPassword)){
            log.error("User is not authorized");
            return new ResponseEntity<>("User is not authorized", HttpStatus.UNAUTHORIZED);
        }

        log.info("Received request to delete the user with username: " + username+" by admin: "+adminUsername);
        userService.deleteUser(username,adminUsername,adminPassword);
        return new ResponseEntity<>("User with username ("+username+") has been deleted successfully", HttpStatus.OK);
    }
    @GetMapping("/{username}/node")
    public ResponseEntity<String> getNodeForUser(@PathVariable String username) {
        Node userNode = loadBalancer.getUserNode(username);
        if(userNode == null) {
            return new ResponseEntity<>("Could not find node Id for user: " + username, HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(String.valueOf(userNode.getNodeId()), HttpStatus.OK);
    }
    @PreAuthorize("@authenticationService.authenticateAdmin(#adminUsername, #adminPassword)")
    @GetMapping
    public ResponseEntity<List<UserDTO>> getAllUsers(@RequestHeader("adminUsername") String adminUsername,
                                                     @RequestHeader("adminPassword") String adminPassword) {
        List<UserDTO> users = userService.getAllUsers();
        return new ResponseEntity<>(users, HttpStatus.OK);
    }
}