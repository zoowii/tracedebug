package com.zoowii.tracedebug.controllers;

import com.zoowii.tracedebug.controllers.vo.StackVarSnapshotVo;
import com.zoowii.tracedebug.controllers.vo.ViewStackVariablesForm;
import com.zoowii.tracedebug.http.BeanPage;
import com.zoowii.tracedebug.http.BeanPaginator;
import com.zoowii.tracedebug.models.SpanDumpItemEntity;
import com.zoowii.tracedebug.models.SpanStackTraceEntity;
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

    @PostMapping("/list")
    public @ResponseBody Object listTraces(@RequestBody BeanPaginator paginator) {
        BeanPage<String> traceIdsPage = traceSpanService.listTraceIds(paginator);
        return traceIdsPage;
    }

    @GetMapping("/list_spans/{traceId}")
    public @ResponseBody Object listTraceSpans(@PathVariable("traceId") String traceId) {
        List<TraceSpanEntity> spanEntities = traceSpanService.findSpansByTraceId(traceId);
        return spanEntities;
    }

    @GetMapping("/span/{spanId}")
    public @ResponseBody Object getSpanInfo(@PathVariable("spanId") String spanId) {
        TraceSpanEntity traceSpanEntity = traceSpanService.findSpanBySpanId(spanId);
        return traceSpanEntity;
    }

    @GetMapping("/stack_trace/span/{spanId}")
    public @ResponseBody Object getSpanStackTrace(@PathVariable("spanId") String spanId) {
        List<SpanStackTraceEntity> spanStackTraceEntities = traceSpanService.listSpanStackTrace(spanId);
        return spanStackTraceEntities;
    }

    @PostMapping("/view_stack_variables/span")
    public @ResponseBody Object viewStackVariablesInSpan(@RequestBody ViewStackVariablesForm form) {
        log.info("viewStackVariablesInSpan form {}", form);
        StackVarSnapshotVo stackVarSnapshot = traceSpanService.listAllMergedSpanDumpsBySpanIdAndSeqInSpan(
                form.getSpanId(), form.getSeqInSpan()!=null?form.getSeqInSpan():0);
        return stackVarSnapshot;
    }

    // TODO: 找到在某个spanId,某个seqInSpan的基础上继续执行到某些breakpoints后的下一个spanId+seqInSpan

    // TODO: view breakpoints, add breakpoints to session, step over/step in/step out
}
