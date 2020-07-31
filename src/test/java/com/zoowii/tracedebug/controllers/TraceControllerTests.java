package com.zoowii.tracedebug.controllers;

import com.zoowii.tracedebug.controllers.vo.NextRequestResponseVo;
import com.zoowii.tracedebug.controllers.vo.StackVarSnapshotVo;
import com.zoowii.tracedebug.controllers.vo.StepStackForm;
import com.zoowii.tracedebug.controllers.vo.ViewStackVariablesForm;
import com.zoowii.tracedebug.http.BeanPage;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class TraceControllerTests {

    private String rootUri = "http://localhost:8280/tracedebug";

    private RestTemplate restTemplate = new RestTemplateBuilder()
            .rootUri(rootUri).build();

    @Test
    public void testListTraces() {
        Map<String, Object> form = new HashMap<>();
        form.put("page", 1);
        form.put("pageSize", 10);
        BeanPage<?> response = restTemplate.postForObject(
                "/api/trace/list", form, BeanPage.class
        );
        log.info("list traces response {}", response);
    }

    @Test
    public void testViewStackVariables() {
        ViewStackVariablesForm form = new ViewStackVariablesForm();
        form.setSpanId("574e1fe0-acd0-4f19-8262-d094416ac08f");
        form.setSeqInSpan(7);
        StackVarSnapshotVo response = restTemplate.postForObject(
                "/api/trace/view_stack_variables/span",
                form, StackVarSnapshotVo.class);
        log.info("view_stack_variables response {}", response);
    }

    @Test
    public void testNextRequest() {
        StepStackForm form = new StepStackForm();
        form.setCurrentSpanId("3e94110f-e818-4558-81de-0e3bca29b31a");
        form.setCurrentSeqInSpan(3);
        form.setStepType("step_over");
        form.setBreakpoints(new ArrayList<>());
        NextRequestResponseVo response = restTemplate.postForObject(
                "/api/trace/next_step_span_seq", form, NextRequestResponseVo.class);
        log.info("next_step_span_seq response {}", response);
    }
}
