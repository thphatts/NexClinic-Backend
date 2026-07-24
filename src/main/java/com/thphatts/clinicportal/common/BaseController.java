package com.thphatts.clinicportal.common;

public abstract class BaseController {
    protected <T> ApiResponse<T> createdSuccessResponse(T data){
        return ApiResponse.success(data);
    }
}
