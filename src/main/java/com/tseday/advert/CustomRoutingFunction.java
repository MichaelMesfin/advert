//package com.tseday.advert;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import org.apache.commons.logging.Log;
//import org.apache.commons.logging.LogFactory;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.cloud.function.context.FunctionCatalog;
//import org.springframework.cloud.function.context.FunctionProperties;
//import org.springframework.cloud.function.context.MessageRoutingCallback;
//import org.springframework.cloud.function.context.catalog.SimpleFunctionRegistry;
//import org.springframework.http.HttpHeaders;
//import org.springframework.http.MediaType;
//import org.springframework.messaging.Message;
//import org.springframework.messaging.support.MessageBuilder;
//import org.springframework.util.Assert;
//
//import java.util.Map;
//import java.util.function.Function;
//
//public class CustomRoutingFunction implements Function<Message<?>, Message<?>> {
//
//    private static final Logger LOG = LoggerFactory.getLogger(CustomRoutingFunction.class);
//
//    private final FunctionCatalog functionCatalog;
//
//    private final FunctionProperties functionProperties;
//
//    private final MessageRoutingCallback routingCallback;
//
//    private final ObjectMapper objectMapper;
//
//    public static final String DEFAULT_ROUTE_HANDLER = "defaultMessageRoutingHandler";
//
//    @Value("${response}")
//    private String response;
//
//
//    private static Log logger = LogFactory.getLog(CustomRoutingFunction.class);
//
//    public CustomRoutingFunction(FunctionCatalog functionCatalog,
//                                 FunctionProperties functionProperties,
//                                 MessageRoutingCallback routingCallback,
//                                 ObjectMapper objectMapper) {
//        this.functionCatalog = functionCatalog;
//        this.functionProperties = functionProperties;
//        this.routingCallback = routingCallback;
//        this.objectMapper = objectMapper;
//    }
//
//    @Override
//    public Message<?> apply(Message<?> input) {
//        try {
//
//            String functionDefinition = routingCallback.routingResult(input);
//            SimpleFunctionRegistry.FunctionInvocationWrapper function = this.functionFromDefinition(functionDefinition);
//
//            Object output = function.apply(input);
//
//            return MessageBuilder.withPayload(output)
//                    .copyHeaders(getHeadersToCopy("200"))
//                    .build();
//        } catch (Exception e) {
//            LOG.error(e.getMessage(), e);
//            return MessageBuilder.withPayload(e.getMessage())
//                    .copyHeaders(getHeadersToCopy("400"))
//                    .build();
//        }
//    }
//
//    private static Map<String, String> getHeadersToCopy(String v2) {
//        return Map.of(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE,
//                "statusCode", v2);
//    }
//
//
//    private SimpleFunctionRegistry.FunctionInvocationWrapper functionFromDefinition(String definition) {
//        SimpleFunctionRegistry.FunctionInvocationWrapper function = this.resolveFunction(definition);
//        Assert.notNull(function,
//                """
//                        Failed to lookup function to route based on the value of
//                        'spring.cloud.function.definition' property '
//                        """
//                        + functionProperties.getDefinition() + "'");
//        if (logger.isInfoEnabled()) {
//            logger.info("Resolved function from provided [definition] property " + functionProperties.getDefinition());
//        }
//        return function;
//    }
//
//    private SimpleFunctionRegistry.FunctionInvocationWrapper resolveFunction(String definition) {
//        SimpleFunctionRegistry.FunctionInvocationWrapper function = functionCatalog.lookup(definition);
//        if (function == null) {
//            function = functionCatalog.lookup(CustomRoutingFunction.DEFAULT_ROUTE_HANDLER);
//        }
//        return function;
//    }
//}
