package controllers;

import akka.NotUsed;
import akka.actor.ActorSystem;
import akka.actor.Scheduler;
import akka.actor.Status;
import akka.stream.OverflowStrategy;
import akka.stream.javadsl.Source;
import akka.util.ByteString;
import com.fasterxml.jackson.databind.JsonNode;
import data.ExampleData;
import play.Configuration;
import play.api.http.HttpEntity;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Results;
import scala.concurrent.ExecutionContext;
import scala.concurrent.ExecutionContextExecutor;
import scala.concurrent.duration.Duration;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import play.libs.ws.*;

/**
 * This controller contains an action that demonstrates how to write
 * simple asynchronous code in a controller. It uses a timer to
 * asynchronously delay sending a response for 1 second.
 */
@Singleton
public class HospitalController extends Controller {

    private Configuration configuration;
    private final ActorSystem actorSystem;
    private final ExecutionContextExecutor exec;
    private final String user;
    private final String pass;
    private final String host;
    private final String path = "/govd/hospital/_search";
    @Inject WSClient ws;


    /**
     * @param actorSystem We need the {@link ActorSystem}'s
     * {@link Scheduler} to run code after a delay.
     * @param exec We need a Java {@link Executor} to apply the result
     * of the {@link CompletableFuture} and a Scala
     * {@link ExecutionContext} so we can use the Akka {@link Scheduler}.
     * An {@link ExecutionContextExecutor} implements both interfaces.
     */
    @Inject
    public HospitalController(ActorSystem actorSystem, ExecutionContextExecutor exec, Configuration configuration) {
      this.actorSystem = actorSystem;
      this.exec = exec;
      this.configuration = configuration;
      this.user = configuration.getString("play.elastic.user");
      this.pass = configuration.getString("play.elastic.pass");
      this.host = configuration.getString("play.elastic.host");
    }

    public CompletionStage<Result> hospitals(){
        String url = host + path + "/?size=1000";
        return ws.url(url).setAuth(user, pass, WSAuthScheme.BASIC).get().thenApply(response ->
                ok(response.asJson())
        );
    }

    public CompletionStage<Result> hospital(String name){

        String url = host + path + "?q=name:" + String.format("*%s*", name);
        return ws.url(url).setAuth(user, pass, WSAuthScheme.BASIC).get().thenApply(response ->
                ok(response.asJson())
        );
    }

    public Result hospitalStream() {

        // Prepare a chunked text stream
        Source<ByteString, ?> source = Source.<ByteString>actorRef(1024, OverflowStrategy.fail())
                .mapMaterializedValue(sourceActor -> {

                    for(int i = 0; i < 999; i++){
                        sourceActor.tell(ByteString.fromString(ExampleData.One), null);
                        sourceActor.tell(ByteString.fromString(ExampleData.Two), null);
                        sourceActor.tell(ByteString.fromString(ExampleData.Three), null);
                    }

                    sourceActor.tell(new Status.Success(NotUsed.getInstance()), null);
                    return null;
                });
        // Serves this stream with 200 OK
        return ok().chunked(source);
    }

    private CompletionStage<String> getFutureMessage(long time, TimeUnit timeUnit) {
        CompletableFuture<String> future = new CompletableFuture<>();
        actorSystem.scheduler().scheduleOnce(
            Duration.create(time, timeUnit),
            () -> future.complete("Hospital 1"),
            exec
        );
        return future;
    }

}
