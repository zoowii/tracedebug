package com.zoowii.tracedebug.controllers;

import com.zoowii.tracedebug.controllers.vo.StackVarSnapshotVo;
import com.zoowii.tracedebug.controllers.vo.ViewStackVariablesForm;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.web.client.RestTemplate;

@Slf4j
public class TraceControllerTests {

    private String rootUri = "http://localhost:8280/tracedebug";

    private RestTemplate restTemplate = new RestTemplateBuilder()
            .rootUri(rootUri).build();

    @Test
    public void testViewStackVariables() {
        ViewStackVariablesForm form = new ViewStackVariablesForm();
        form.setSpanId("9de7f02b-ca66-4f52-9324-4da2012a5eae");
        form.setSeqInSpan(7);
        StackVarSnapshotVo response = restTemplate.postForObject(
                "/api/trace/view_stack_variables/span",
                form, StackVarSnapshotVo.class);
        log.info("view_stack_variables response {}", response);
    }
}
