package io.smallrye.reactive.converters.tck;

class BoomException extends RuntimeException {

    BoomException(String message) {
        super(message);
    }

    BoomException() {
        super("BOOM");
    }
}
