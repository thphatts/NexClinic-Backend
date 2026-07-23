package com.thphatts.clinicportal.controller;

import com.thphatts.clinicportal.dto.response.UserResponse;
import com.thphatts.clinicportal.dto.request.UserRequest;
import com.thphatts.clinicportal.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = "/api/v1/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<List<UserResponse>> showListUsers(){
        return new ResponseEntity<>(userService.index(), HttpStatus.OK);
    }
    @PostMapping
    public ResponseEntity<String> createNewUser(@RequestBody UserRequest rq){
        userService.create(rq);
        return new ResponseEntity<>("Create a new user sucessfully", HttpStatus.CREATED);
    }
    @PutMapping("/{id}")
    public ResponseEntity<String> updateUser(@PathVariable("id") String id,@RequestBody UserRequest rq){
        userService.update(id,rq);
        return new ResponseEntity<>("Update a user sucessfully",HttpStatus.OK);
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteUser(@PathVariable("id") String id){
        userService.delete(id);
        return new ResponseEntity<>("delete a user sucessfully", HttpStatus.OK);
    }
}
