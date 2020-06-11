package com.zoowii.tracedebug.controllers;

import com.zoowii.tracedebug.aspects.RequestLog;
import com.zoowii.tracedebug.controllers.vo.*;
import com.zoowii.tracedebug.exceptions.SpanNotFoundException;
import com.zoowii.tracedebug.http.BeanPage;
import com.zoowii.tracedebug.http.BeanPaginator;
import com.zoowii.tracedebug.models.SpanDumpItemEntity;
import com.zoowii.tracedebug.models.SpanStackTraceEntity;
import com.zoowii.tracedebug.models.TraceSpanEntity;
import com.zoowii.tracedebug.services.TraceDebugService;
import com.zoowii.tracedebug.services.TraceSpanService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/trace")
public class TraceController {
    @Resource
    private TraceSpanService traceSpanService;
    @Resource
    private TraceDebugService traceDebugService;

    /**
     * 开启新span时上报接口
     * @return
     */
    @RequestLog
    @GetMapping("/span_start/{traceId}/{spanId}")
    public Object startNewSpan(
            @PathVariable("traceId") String traceId,
            @PathVariable("spanId") String spanId,
            @RequestParam("stack_depth") Integer stackDepth,
            @RequestParam("module_id") String moduleId,
            @RequestParam("classname") String classname,
            @RequestParam("method") String methodName) {
        if(StringUtils.isEmpty(traceId) || StringUtils.isEmpty(spanId)) {
            return "empty traceId or spanId";
        }
        TraceSpanEntity traceSpanEntity = traceSpanService.findSpanBySpanId(spanId);
        if(traceSpanEntity != null) {
            return "duplicate spanId";
        }
        traceSpanEntity = new TraceSpanEntity();
        traceSpanEntity.setTraceId(traceId);
        traceSpanEntity.setSpanId(spanId);
        traceSpanEntity.setStackDepth(stackDepth!=null?stackDepth:0);
        traceSpanEntity.setModuleId(moduleId!=null?moduleId:"");
        traceSpanEntity.setClassname(classname!=null?classname:"");
        traceSpanEntity.setMethodName(methodName!=null?methodName:"");

        traceSpanEntity = traceSpanService.saveTraceSpan(traceSpanEntity);

        return traceSpanEntity;
    }

    /**
     * 某个span开启时的stack trace element的上报，每次只上报一项
     */
    @RequestLog
    @GetMapping("/add_span_stack_trace_element")
    public @ResponseBody Object addSpanStackTraceElement(
            @RequestParam("trace_id") String traceId,
            @RequestParam("span_id") String spanId,
            @RequestParam("stack_index") Integer stackIndex,
            @RequestParam("module_id") String moduleId,
            @RequestParam("classname") String classname,
            @RequestParam("method") String methodName,
            @RequestParam("line") Integer lineNumber,
            @RequestParam("filename") String filename
    ) {
        // 保持幂等性避免(spanId, stackIndex)重复插入
        if(StringUtils.isEmpty(traceId) || StringUtils.isEmpty(spanId)) {
            return "empty traceId or spanId";
        }
        SpanStackTraceEntity spanStackTraceEntity = traceSpanService.findSpanStackTraceBySpanIdAndStackIndex(spanId, stackIndex);
        if(spanStackTraceEntity!=null) {
            return "duplicate spanId and stackIndex";
        }
        spanStackTraceEntity = new SpanStackTraceEntity();
        spanStackTraceEntity.setTraceId(traceId);
        spanStackTraceEntity.setSpanId(spanId);
        spanStackTraceEntity.setStackIndex(stackIndex!=null?stackIndex:-1);
        spanStackTraceEntity.setModuleId(moduleId!=null?moduleId:"");
        spanStackTraceEntity.setClassname(classname!=null?classname:"");
        spanStackTraceEntity.setMethodName(methodName!=null?methodName:"");
        spanStackTraceEntity.setLine(lineNumber);
        spanStackTraceEntity.setFilename(filename);
        spanStackTraceEntity = traceSpanService.saveSpanStackTrace(spanStackTraceEntity);

        return spanStackTraceEntity;
    }

    /**
     * 某个span dump某个符号的值的上报接口
     */
    @RequestLog
    @GetMapping("/span_dump")
    public @ResponseBody Object dumpVarInSpan(
            @RequestParam("trace_id") String traceId,
            @RequestParam("span_id") String spanId,
            @RequestParam("seq_in_span") Integer seqInSpan,
            @RequestParam("name") String name,
            @RequestParam("value") String value,
            @RequestParam("line") Integer line) {
        if(StringUtils.isEmpty(traceId) || StringUtils.isEmpty(spanId) || StringUtils.isEmpty(name)) {
            return "empty traceId or spanId or name";
        }
        SpanDumpItemEntity spanDumpItemEntity = new SpanDumpItemEntity();
        spanDumpItemEntity.setTraceId(traceId);
        spanDumpItemEntity.setSpanId(spanId);
        spanDumpItemEntity.setSeqInSpan(seqInSpan);
        spanDumpItemEntity.setName(name);
        spanDumpItemEntity.setValue(value);
        spanDumpItemEntity.setLine(line);
        spanDumpItemEntity = traceSpanService.saveSpanDumpItem(spanDumpItemEntity);
        return spanDumpItemEntity;
    }

    @RequestLog
    @PostMapping("/list")
    public @ResponseBody BeanPage<String> listTraces(@RequestBody BeanPaginator paginator) {
        BeanPage<String> traceIdsPage = traceSpanService.listTraceIds(paginator);
        return traceIdsPage;
    }

    @GetMapping("/list_spans/{traceId}")
    public @ResponseBody List<TraceSpanEntity> listTraceSpans(@PathVariable("traceId") String traceId) {
        List<TraceSpanEntity> spanEntities = traceSpanService.findAllByTraceIdOrderByIdAsc(traceId);
        return spanEntities;
    }

    @GetMapping("/span/{spanId}")
    public @ResponseBody Object getSpanInfo(@PathVariable("spanId") String spanId) {
        TraceSpanEntity traceSpanEntity = traceSpanService.findSpanBySpanId(spanId);
        return traceSpanEntity;
    }

    @RequestLog
    @PostMapping("/stack_trace/span")
    public @ResponseBody Object getSpanStackTrace(@RequestBody ViewStackTraceForm form) {
        List<SpanStackTraceEntity> spanStackTraceEntities = traceSpanService.listSpanStackTrace(form.getSpanId(), form.getSeqInSpan());
        return spanStackTraceEntities;
    }

    @RequestLog
    @PostMapping("/view_stack_variables/span")
    public @ResponseBody Object viewStackVariablesInSpan(@RequestBody ViewStackVariablesForm form) {
        StackVarSnapshotVo stackVarSnapshot = traceSpanService.listAllMergedSpanDumpsBySpanIdAndSeqInSpan(
                form.getSpanId(), form.getSeqInSpan()!=null?form.getSeqInSpan():0);
        return stackVarSnapshot;
    }

    // 找到在某个spanId,某个seqInSpan的基础上继续执行到某些breakpoints后的下一个spanId+seqInSpan
    @RequestLog(response = true)
    @PostMapping("/next_step_span_seq")
    public @ResponseBody NextRequestResponseVo findNextStepSpanSeq(@RequestBody StepStackForm form)
            throws SpanNotFoundException {
        return traceDebugService.nextStep(form);
    }
}
