package com.ipfix_scenario_ai.ipjfix_svc.adapters.spring;

import java.io.IOException;

import org.apache.olingo.server.api.OData;
import org.apache.olingo.server.api.ODataHttpHandler;
import org.apache.olingo.server.api.ServiceMetadata;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ipfix_scenario_ai.ipjfix_svc.adapters.odata.IpfixEdmProvider;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;





@RestController
@RequestMapping("/odata")
public class ODataController {

    private final ODataHttpHandler handler;

    public ODataController(IpfixEdmProvider edmProvider) {
        OData odata = OData.newInstance();
        ServiceMetadata serviceMetadata = odata.createServiceMetadata(
            edmProvider, 
            null
        );
        this.handler = odata.createHandler(serviceMetadata);
    }

    @RequestMapping("/**")
    public void handleODataRequest(HttpServletRequest request, 
                                HttpServletResponse response) throws IOException {
        handler.process(request, response);
    }
}

