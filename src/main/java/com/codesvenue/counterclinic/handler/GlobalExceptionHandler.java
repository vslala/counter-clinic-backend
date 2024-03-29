package com.codesvenue.counterclinic.handler;

import aj.org.objectweb.asm.Handle;
import com.codesvenue.counterclinic.user.controller.InputValidationException;
import com.codesvenue.counterclinic.walkinappointment.service.EmptyWalkInAppointmentException;
import com.codesvenue.counterclinic.walkinappointment.service.NoMoreAppointmentsLeftForTheDayException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.sql.SQLIntegrityConstraintViolationException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(InputValidationException.class)
    public ResponseEntity<HandlerResponse> handleInputValidationException(InputValidationException e) {
        StringBuilder errorMessage = new StringBuilder()
                .append(e.getMessage());
        e.getFieldErrors().forEach(msg ->
            errorMessage.append(" ")
                    .append(msg.getDefaultMessage()).append("\r\n")
        );

        HandlerResponse handlerResponse = new HandlerResponse();
        handlerResponse.setErrorCode("4000");
        handlerResponse.setMessage(errorMessage.toString());
        return ResponseEntity.badRequest().body(handlerResponse);
    }

    @ExceptionHandler(EmptyWalkInAppointmentException.class)
    public ResponseEntity<HandlerResponse> handleEmptyWalkInAppointment() {
        HandlerResponse handlerResponse = new HandlerResponse();
        handlerResponse.setErrorCode("0001");
        handlerResponse.setMessage("No Walk In Appointments for the day.");
        return ResponseEntity.badRequest().body(handlerResponse);
    }

    @ExceptionHandler(NoMoreAppointmentsLeftForTheDayException.class)
    public ResponseEntity<HandlerResponse> handleNoMoreAppointmentsLeftForTheDay() {
        HandlerResponse handlerResponse = new HandlerResponse();
        handlerResponse.setErrorCode("0002");
        handlerResponse.setMessage("No More Appointments Left For The Day.");
        return ResponseEntity.badRequest().body(handlerResponse);
    }

    @ExceptionHandler(SQLIntegrityConstraintViolationException.class)
    public ResponseEntity<HandlerResponse> handleUniqueConstraintViolation(SQLIntegrityConstraintViolationException e) {
        HandlerResponse handlerResponse = new HandlerResponse();
        handlerResponse.setErrorCode("0003");
        handlerResponse.setMessage(e.getMessage());
        return ResponseEntity.badRequest().body(handlerResponse);
    }
}
