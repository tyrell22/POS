package com.restaurant.pos.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(MethodArgumentNotValidException ex) {
    Map < String, String > errors = new HashMap <> ();
    ex.getBindingResult().getAllErrors().forEach((error) -> {
        String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
    errors.put(fieldName, errorMessage);
});

Map < String, Object > response = new HashMap <> ();
response.put("error", "Грешка при валидација");
response.put("details", errors);

return ResponseEntity.badRequest().body(response);
    }

@ExceptionHandler(RuntimeException.class)
public ResponseEntity < Map < String, String >> handleRuntimeException(RuntimeException ex) {
    Map < String, String > error = new HashMap <> ();
    error.put("error", ex.getMessage());
    return ResponseEntity.badRequest().body(error);
}

@ExceptionHandler(Exception.class)
public ResponseEntity < Map < String, String >> handleGeneralException(Exception ex) {
    Map < String, String > error = new HashMap <> ();
    error.put("error", "Настана неочекувана грешка");
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
}
}