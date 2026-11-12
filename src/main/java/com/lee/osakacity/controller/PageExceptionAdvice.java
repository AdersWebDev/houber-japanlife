package com.lee.osakacity.controller;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.io.IOException;

/**
 * 화면 주소에 잘못된 값이 들어온 경우(/detail/abc, /list?category=없는값 등)
 * 400 대신 404 페이지를 보여준다. (검색엔진 입장에서는 '없는 페이지')
 * API(/post/** 등)에는 적용하지 않는다.
 */
@ControllerAdvice(assignableTypes = MainController.class)
public class PageExceptionAdvice {

    @ExceptionHandler({MethodArgumentTypeMismatchException.class, MissingServletRequestParameterException.class})
    public void notFound(HttpServletResponse response) throws IOException {
        response.sendError(HttpServletResponse.SC_NOT_FOUND);
    }
}
