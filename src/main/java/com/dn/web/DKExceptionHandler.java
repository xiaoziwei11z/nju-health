package com.dn.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
@Component
public class DKExceptionHandler {
    @Autowired
    private MailService mailService;

    @ExceptionHandler(Exception.class)
    public void exception(Exception e){
        mailService.sendEmail("每日打卡出错！",e.getMessage());
    }
}
