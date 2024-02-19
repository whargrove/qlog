package qlog.controllers;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.QueryValue;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.micronaut.serde.annotation.Serdeable.Serializable;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import qlog.TailReader;

import java.nio.file.Path;
import java.util.List;

@Controller("/queryLog")
public class QueryLogController {

    private final TailReader tailReader;

    @Inject
    public QueryLogController(TailReader tailReader) {
        this.tailReader = tailReader;
    }

    /**
     * Reads the "tail" of a file in /var/log.
     *
     * @param relativePath      The relativePath used to resolve a file. E.g., "syslog" resolves to
     *                          <code>/var/log/syslog</code>. The relativePath must be a non-empty
     *                          string.
     * @param filter            A filter to apply to the lines. If null, no filter is applied and
     *                          all lines are returned.
     * @param start             The starting line number to tail in the file. If 0, the tail
     *                          starts at the end of the file.
     * @param count             The number of lines to tail in the file. Must be a positive
     *                          integer. The maximum value allowed is 10,000. Specifying a value
     *                          greater than 10,000 will result in a 400 Bad Request. If the file
     *                          contains more lines than are specified in the count the response
     *                          will contain a continuation token that can be used to retrieve the
     *                          next set of lines.
     * @param continuationToken A token that can be used to retrieve the "next" set of lines from
     *                          the file. If the token is present, start and count are ignored and
     *                          the file is read from the last position of the previous request
     *                          using the same "count" parameter used in the previous request.
     *                          If null, the file is read according to the start and count
     *                          parameters and a continuationToken for this file will be returned
     *                          in the response metadata.
     * @return A 200 OK containing the requested lines from the file.
     */
    @Get
    @ExecuteOn(TaskExecutors.BLOCKING)
    public HttpResponse<QueryLog> queryLog(@QueryValue @NotBlank String relativePath,
                                           @QueryValue @Nullable String filter,
                                           @QueryValue(defaultValue = "0") @PositiveOrZero int start,
                                           @QueryValue(defaultValue = "1000") @Positive @Max(value = 10_000) int count,
                                           @QueryValue @Nullable String continuationToken) {
        var lines = this.tailReader.getLastNLines(Path.of("/var/log", relativePath), filter, start, count);
        // TODO: Get the next continuation token from the tailReader
        //       Return the continuation token in the metadata of the response.
        return HttpResponse.ok(new QueryLog(lines, null));
    }

    @Serializable
    public record QueryLog(List<String> data, @Nullable Metadata metadata) {
    }

    @Serializable
    public record Metadata(ContinuationToken continuationToken) {
    }

    @Serializable
    public record ContinuationToken(String token) {
    }

}
