//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package source;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.BiConsumer;
import org.apache.flink.api.common.time.Time;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.runtime.blob.TransientBlobService;
import org.apache.flink.runtime.dispatcher.DispatcherGateway;
import org.apache.flink.runtime.leaderelection.LeaderElectionService;
import org.apache.flink.runtime.resourcemanager.ResourceManagerGateway;
import org.apache.flink.runtime.rest.RestServerEndpointConfiguration;
import org.apache.flink.runtime.rest.handler.RestHandlerConfiguration;
import org.apache.flink.runtime.rest.handler.RestHandlerSpecification;
import org.apache.flink.runtime.rest.handler.job.JobSubmitHandler;
import org.apache.flink.runtime.rest.handler.legacy.metrics.MetricFetcher;
import org.apache.flink.runtime.rpc.FatalErrorHandler;
import org.apache.flink.runtime.webmonitor.WebMonitorEndpoint;
import org.apache.flink.runtime.webmonitor.WebMonitorExtension;
import org.apache.flink.runtime.webmonitor.WebMonitorUtils;
import org.apache.flink.runtime.webmonitor.retriever.GatewayRetriever;
import org.apache.flink.shaded.netty4.io.netty.channel.ChannelInboundHandler;
import org.apache.flink.util.ExceptionUtils;
import org.apache.flink.util.FlinkException;

public class DispatcherRestEndpoint_OOP extends WebMonitorEndpoint<DispatcherGateway> {
    private WebMonitorExtension webSubmissionExtension = WebMonitorExtension.empty();

    public DispatcherRestEndpoint_OOP(RestServerEndpointConfiguration endpointConfiguration, GatewayRetriever<DispatcherGateway> leaderRetriever, Configuration clusterConfiguration, RestHandlerConfiguration restConfiguration, GatewayRetriever<ResourceManagerGateway> resourceManagerRetriever, TransientBlobService transientBlobService, ExecutorService executor, MetricFetcher metricFetcher, LeaderElectionService leaderElectionService, FatalErrorHandler fatalErrorHandler) throws IOException {
        super(endpointConfiguration, leaderRetriever, clusterConfiguration, restConfiguration, resourceManagerRetriever, transientBlobService, executor, metricFetcher, leaderElectionService, fatalErrorHandler);
    }

    protected List<Tuple2<RestHandlerSpecification, ChannelInboundHandler>> initializeHandlers(CompletableFuture<String> localAddressFuture) {
        List<Tuple2<RestHandlerSpecification, ChannelInboundHandler>> handlers = super.initializeHandlers(localAddressFuture);
        Time timeout = this.restConfiguration.getTimeout();
        JobSubmitHandler jobSubmitHandler = new JobSubmitHandler(this.leaderRetriever, timeout, this.responseHeaders, this.executor, this.clusterConfiguration);
        if(this.restConfiguration.isWebSubmitEnabled()) {
            try {
                // 此处注册了JAR Upload和Run的处理方法
                this.webSubmissionExtension = WebMonitorUtils.loadWebSubmissionExtension(this.leaderRetriever, timeout, this.responseHeaders, localAddressFuture, this.uploadDir, this.executor, this.clusterConfiguration);
                handlers.addAll(this.webSubmissionExtension.getHandlers());
            } catch (FlinkException var6) {
                if(this.log.isDebugEnabled()) {
                    this.log.debug("Failed to load web based job submission extension.", var6);
                } else {
                    this.log.info("Failed to load web based job submission extension. Probable reason: flink-runtime-web is not in the classpath.");
                }
            }
        } else {
            this.log.info("Web-based job submission is not enabled.");
        }

        handlers.add(Tuple2.of(jobSubmitHandler.getMessageHeaders(), jobSubmitHandler));
        return handlers;
    }

    protected CompletableFuture<Void> shutDownInternal() {
        CompletableFuture<Void> shutdownFuture = super.shutDownInternal();
        CompletableFuture<Void> shutdownResultFuture = new CompletableFuture();
        shutdownFuture.whenComplete((ignored, throwable) -> {
            this.webSubmissionExtension.closeAsync().whenComplete((innerIgnored, innerThrowable) -> {
                if(innerThrowable != null) {
                    shutdownResultFuture.completeExceptionally(ExceptionUtils.firstOrSuppressed(innerThrowable, throwable));
                } else if(throwable != null) {
                    shutdownResultFuture.completeExceptionally(throwable);
                } else {
                    shutdownResultFuture.complete(null);
                }

            });
        });
        return shutdownResultFuture;
    }
}
