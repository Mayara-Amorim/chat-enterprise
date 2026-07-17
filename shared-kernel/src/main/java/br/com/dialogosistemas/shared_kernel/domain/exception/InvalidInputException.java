package br.com.dialogosistemas.shared_kernel.domain.exception;

public class InvalidInputException extends RuntimeException {

    public InvalidInputException(String message) {
        super(message);
    }
}
