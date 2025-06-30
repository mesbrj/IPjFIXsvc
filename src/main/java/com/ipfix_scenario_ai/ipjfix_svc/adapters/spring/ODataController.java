package com.ipfix_scenario_ai.ipjfix_svc.adapters.spring;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.apache.olingo.server.api.OData;
import org.apache.olingo.server.api.ODataHttpHandler;
import org.apache.olingo.server.api.ServiceMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.http.ResponseEntity;

import com.ipfix_scenario_ai.ipjfix_svc.adapters.odata.IpfixEdmProvider;
import com.ipfix_scenario_ai.ipjfix_svc.adapters.odata.IpfixEntityCollectionProcessor;
import com.ipfix_scenario_ai.ipjfix_svc.adapters.odata.IpfixEntityProcessor;
import com.ipfix_scenario_ai.ipjfix_svc.adapters.ignite.UserRepository;
import com.ipfix_scenario_ai.ipjfix_svc.core.models.User;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/odata")
public class ODataController {
    
    private static final Logger logger = LoggerFactory.getLogger(ODataController.class);
    
    private final IpfixEdmProvider edmProvider;
    private final IpfixEntityCollectionProcessor entityCollectionProcessor;
    private final IpfixEntityProcessor entityProcessor;
    private final UserRepository userRepository;
    
    public ODataController(IpfixEdmProvider edmProvider,
                        IpfixEntityCollectionProcessor entityCollectionProcessor,
                        IpfixEntityProcessor entityProcessor,
                        UserRepository userRepository) {
        this.edmProvider = edmProvider;
        this.entityCollectionProcessor = entityCollectionProcessor;
        this.entityProcessor = entityProcessor;
        this.userRepository = userRepository;
    }
    
    // Debug endpoint to test UserRepository
    @GetMapping("/debug/users")
    public ResponseEntity<List<User>> debugUsers() {
        logger.info("Debug endpoint called - fetching all users");
        List<User> users = userRepository.findAll();
        logger.info("Found {} users: {}", users.size(), users);
        return ResponseEntity.ok(users);
    }
    
    @RequestMapping(value = "/**", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
    public void odata(HttpServletRequest request, HttpServletResponse response) throws IOException {
        
        logger.info("OData request received: {} {}", request.getMethod(), request.getRequestURI());
        logger.info("Query string: {}", request.getQueryString());
        logger.info("Context path: {}", request.getContextPath());
        logger.info("Servlet path: {}", request.getServletPath());
        logger.info("Path info: {}", request.getPathInfo());
        
        try {
            // Set proper content type for OData responses
            response.setHeader("OData-Version", "4.0");
            
            OData odata = OData.newInstance();
            
            logger.info("Creating ServiceMetadata with edmProvider: {}", edmProvider.getClass().getSimpleName());
            logger.info("Testing EdmProvider getSchemas method directly...");
            
            // Test our EDM provider directly
            try {
                var schemas = edmProvider.getSchemas();
                logger.info("EDM provider returned {} schemas", schemas.size());
                for (var schema : schemas) {
                    logger.info("Schema namespace: {}, entityTypes: {}", 
                        schema.getNamespace(), 
                        schema.getEntityTypes() != null ? schema.getEntityTypes().size() : 0);
                }
            } catch (Exception e) {
                logger.error("Error calling edmProvider.getSchemas()", e);
            }
            
            ServiceMetadata serviceMetadata = odata.createServiceMetadata(edmProvider, Collections.emptyList());
            logger.info("Created ServiceMetadata successfully");
            
            ODataHttpHandler handler = odata.createHandler(serviceMetadata);
            logger.info("Created OData handler: {}", handler.getClass().getSimpleName());
            
            // Initialize processors manually
            logger.info("Manually initializing processors...");
            entityCollectionProcessor.init(odata, serviceMetadata);
            entityProcessor.init(odata, serviceMetadata);
            logger.info("Processors initialized manually");
            
            // Register processors with detailed logging
            handler.register(entityCollectionProcessor);
            logger.info("Registered EntityCollectionProcessor: {}", entityCollectionProcessor.getClass().getSimpleName());
            
            handler.register(entityProcessor);
            logger.info("Registered EntityProcessor: {}", entityProcessor.getClass().getSimpleName());
            
            logger.info("Registered processors with OData handler");
            
            // Create a wrapper that properly handles the servlet path
            HttpServletRequestWrapper requestWrapper = new HttpServletRequestWrapper(request) {
                @Override
                public String getServletPath() {
                    return "/odata";
                }
                
                @Override
                public String getPathInfo() {
                    String uri = request.getRequestURI();
                    String contextPath = request.getContextPath();
                    if (uri.startsWith(contextPath + "/odata")) {
                        String pathInfo = uri.substring((contextPath + "/odata").length());
                        return pathInfo.isEmpty() ? "/" : pathInfo;
                    }
                    return uri;
                }
            };
            
            logger.debug("Processing OData request with wrapped servlet paths...");
            handler.process(requestWrapper, response);
            logger.debug("OData request processed");
            
        } catch (Exception e) {
            logger.error("Error processing OData request", e);
            response.setStatus(500);
            response.getWriter().write("Internal server error: " + e.getMessage());
        }
    }
}

