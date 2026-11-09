package com.stattracker.ingestion.job;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

public class IngestionInProgressException extends ErrorResponseException {

    IngestionInProgressException() {
        super(HttpStatus.CONFLICT,
                ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "Another import is already running"),
                null);
    }
}
