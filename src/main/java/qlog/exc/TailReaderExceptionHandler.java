package qlog.exc;

import io.micronaut.context.annotation.Requires;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Produces;
import io.micronaut.http.server.exceptions.ExceptionHandler;
import io.micronaut.http.server.exceptions.response.ErrorContext;
import io.micronaut.http.server.exceptions.response.ErrorResponseProcessor;
import jakarta.inject.Singleton;

@SuppressWarnings("rawtypes")
@Produces
@Singleton
@Requires(classes = {TailReaderException.class, ExceptionHandler.class})
public class TailReaderExceptionHandler implements ExceptionHandler<TailReaderException, HttpResponse> {

    private final ErrorResponseProcessor<?> errorResponseProcessor;

    public TailReaderExceptionHandler(ErrorResponseProcessor<?> errorResponseProcessor) {
        this.errorResponseProcessor = errorResponseProcessor;
    }

    @SuppressWarnings("DuplicateBranchesInSwitch")
    @Override
    public HttpResponse handle(HttpRequest request, TailReaderException exception) {
        return errorResponseProcessor.processResponse(ErrorContext.builder(request)
                .cause(exception)
                .errorMessage(exception.getMessage())
                .build(), switch (exception) {
            case TailReaderFileNotFoundException ignored -> HttpResponse.notFound();
            case TailReaderIOException ignored -> HttpResponse.serverError();
            default -> HttpResponse.serverError();
        });
    }
}
