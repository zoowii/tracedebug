package com.zoowii.tracedebug.controllers;

import com.zoowii.tracedebug.models.TraceSpanEntity;
import com.zoowii.tracedebug.services.TraceSpanService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@Slf4j
@Controller
@RequestMapping("/api/trace")
public class TraceController {
    @Resource
    private TraceSpanService traceSpanService;

    @GetMapping("/spans/{traceId}")
    public @ResponseBody Object listTraceSpans(@PathVariable("traceId") String traceId) {
        List<TraceSpanEntity> spanEntities = traceSpanService.findSpansByTraceId(traceId);
        return spanEntities;
    }
    // TODO: view span stacktrace, view breakpoints, add breakpoints to session, step over/step in/step out
}
