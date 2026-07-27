package com.thphatts.clinicportal.controller;

import com.thphatts.clinicportal.common.ApiResponse;
import com.thphatts.clinicportal.common.BaseController;
import com.thphatts.clinicportal.dto.request.UserResponse;
import com.thphatts.clinicportal.dto.request.UserRequest;
import com.thphatts.clinicportal.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
@RequiredArgsConstructor
@RestController
@RequestMapping(value = "/api/v1/users")
public class UserController extends BaseController {
    private final UserService userService;
    @GetMapping
    public ApiResponse<List<UserResponse>> showListUsers(){
        return createdSuccessResponse(userService.index());
    }
    @PostMapping
    public ApiResponse<String> createNewUser(@Valid @RequestBody UserRequest rq){
        userService.create(rq);
        return createdSuccessResponse("Create a new user successfully");
    }
    @PutMapping("/{id}")
    public ResponseEntity<String> updateUser(@PathVariable("id") String id,@RequestBody UserRequest rq){
        userService.update(id,rq);
        return new ResponseEntity<>("Update a user successfully",HttpStatus.OK);
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteUser(@PathVariable("id") String id){
        userService.delete(id);
        return new ResponseEntity<>("delete a user successfully", HttpStatus.OK);
    }
}
