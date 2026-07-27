package com.petrolprice.station_search_api.infrastructure.in.rest.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.slf4j.MDC;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ProblemDetail> handleMethodArgumentTypeMismatch(
        MethodArgumentTypeMismatchException exception
    ) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);

        problem.setTitle("Invalid query parameter");
        problem.setDetail("Invalid value '%s' for parameter '%s'".formatted(
            exception.getValue(),
            exception.getName()
        ));
        problem.setProperty("code", "INVALID_QUERY_PARAMETER");

        return ResponseEntity
            .badRequest()
            .body(problem);
    }

    @ExceptionHandler(InvalidStationSearchRequestException.class)
    public ProblemDetail handleInvalidSearchRequest(
        InvalidStationSearchRequestException exception
    ) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST,
            exception.getMessage()
        );

        problem.setTitle("Invalid station search");
        problem.setProperty("code", "INVALID_SEARCH_AREA");

        return problem;
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnexpectedException(
        Exception ex
    ) {
        log.error("Unexpected error", ex);

        ProblemDetail problem = ProblemDetail.forStatus(
            HttpStatus.INTERNAL_SERVER_ERROR
        );

        problem.setTitle("Internal Server Error");
        problem.setDetail("An unexpected error occurred");
        String traceId = MDC.get("traceId");
        if (traceId != null) {
            problem.setProperty("traceId", traceId);
        }

        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(problem);
    }

}
