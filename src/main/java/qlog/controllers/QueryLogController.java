package qlog.controllers;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.QueryValue;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.micronaut.serde.annotation.Serdeable.Serializable;
import jakarta.annotation.Nullable;
import qlog.TailReader;

import java.nio.file.Path;
import java.util.List;

@Controller("/queryLog")
public class QueryLogController {

    /**
     * Reads the "tail" of a file in /var/log.
     *
     * @param relativePath The relativePath used to resolve a file. E.g., "syslog" resolves to <code>/var/log/syslog</code>.
     * @param count        The number of lines to tail in the file.
     * @return A 200 OK containing the requested lines from the file.
     */
    @Get
    @ExecuteOn(TaskExecutors.BLOCKING)
    public HttpResponse<QueryLog> queryLog(@QueryValue String relativePath,
                                           @QueryValue(defaultValue = "42") int count,
                                           @QueryValue @Nullable String filter) {
        var lines = TailReader.getLastNLines(Path.of("/var/log", relativePath), count, filter);
        return HttpResponse.ok(new QueryLog(lines));
    }

    @Serializable
    public record QueryLog(List<String> data) {
    }

}
