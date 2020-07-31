package com.zoowii.tracedebug.controllers;

import com.zoowii.tracedebug.controllers.vo.*;
import com.zoowii.tracedebug.exceptions.SpanNotFoundException;
import com.zoowii.tracedebug.http.BeanPage;
import com.zoowii.tracedebug.http.BeanPaginator;
import com.zoowii.tracedebug.models.SpanDumpItemEntity;
import com.zoowii.tracedebug.models.SpanStackTraceEntity;
import com.zoowii.tracedebug.models.TraceSpanEntity;
import com.zoowii.tracedebug.services.TraceDebugService;
import com.zoowii.tracedebug.services.TraceSpanService;
import com.zoowii.tracedebug.spring.aspects.RequestLog;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@CrossOrigin
@RestController
@RequestMapping("/api/trace")
public class TraceController {
    @Resource
    private TraceSpanService traceSpanService;
    @Resource
    private TraceDebugService traceDebugService;

    /**
     * 开启新span时上报接口
     *
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
        if (StringUtils.isEmpty(traceId) || StringUtils.isEmpty(spanId)) {
            return "empty traceId or spanId";
        }
        TraceSpanEntity traceSpanEntity = traceSpanService.findSpanBySpanId(spanId);
        if (traceSpanEntity != null) {
            return "duplicate spanId";
        }
        traceSpanEntity = new TraceSpanEntity();
        traceSpanEntity.setTraceId(traceId);
        traceSpanEntity.setSpanId(spanId);
        traceSpanEntity.setStackDepth(stackDepth != null ? stackDepth : 0);
        traceSpanEntity.setModuleId(moduleId != null ? moduleId : "");
        traceSpanEntity.setClassname(classname != null ? classname : "");
        traceSpanEntity.setMethodName(methodName != null ? methodName : "");

        traceSpanEntity = traceSpanService.saveTraceSpan(traceSpanEntity);

        return traceSpanEntity;
    }

    /**
     * 某个span开启时的stack trace element的上报，每次只上报一项
     */
    @RequestLog
    @GetMapping("/add_span_stack_trace_element")
    public Object addSpanStackTraceElement(
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
        if (StringUtils.isEmpty(traceId) || StringUtils.isEmpty(spanId)) {
            return "empty traceId or spanId";
        }
        SpanStackTraceEntity spanStackTraceEntity = traceSpanService.findSpanStackTraceBySpanIdAndStackIndex(spanId, stackIndex);
        if (spanStackTraceEntity != null) {
            return "duplicate spanId and stackIndex";
        }
        spanStackTraceEntity = new SpanStackTraceEntity();
        spanStackTraceEntity.setTraceId(traceId);
        spanStackTraceEntity.setSpanId(spanId);
        spanStackTraceEntity.setStackIndex(stackIndex != null ? stackIndex : -1);
        spanStackTraceEntity.setModuleId(moduleId != null ? moduleId : "");
        spanStackTraceEntity.setClassname(classname != null ? classname : "");
        spanStackTraceEntity.setMethodName(methodName != null ? methodName : "");
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
    public Object dumpVarInSpan(
            @RequestParam("trace_id") String traceId,
            @RequestParam("span_id") String spanId,
            @RequestParam("seq_in_span") Integer seqInSpan,
            @RequestParam("name") String name,
            @RequestParam("value") String value,
            @RequestParam("line") Integer line) {
        if (StringUtils.isEmpty(traceId) || StringUtils.isEmpty(spanId) || StringUtils.isEmpty(name)) {
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
    public BeanPage<String> listTraces(@RequestBody BeanPaginator paginator) {
        BeanPage<String> traceIdsPage = traceSpanService.listTraceIds(paginator);
        return traceIdsPage;
    }

    @GetMapping("/list_spans/{traceId}")
    public List<TraceSpanEntity> listTraceSpans(@PathVariable("traceId") String traceId) {
        List<TraceSpanEntity> spanEntities = traceSpanService.findAllByTraceIdOrderByIdAsc(traceId);
        return spanEntities;
    }

    /**
     * 获取某个traceId的调用链路的各栈顶
     */
    @GetMapping("/list_top_calls/{traceId}")
    public List<TraceCallVo> listTraceTopCalls(@PathVariable("traceId") String traceId) {
        List<SpanDumpItemEntity> allSpanDumps = traceSpanService.findAllDumpsByTraceIdAndIdGreaterThanOrderByIdAsc(traceId, 0);
        List<SpanDumpItemEntity> resultDumps = new ArrayList<>();
        SpanDumpItemEntity before = null;
        for (SpanDumpItemEntity spanDumpItem : allSpanDumps) {
            // 连续两个一样的spanId的dump，只返回前一个
            try {
                if (before == null) {
                    resultDumps.add(spanDumpItem);
                    continue;
                }
                if (before.getSpanId() == null) {
                    continue;
                }
                if (before.getSpanId().equals(spanDumpItem.getSpanId())) {
                    continue;
                }
                resultDumps.add(spanDumpItem);
            } finally {
                before = spanDumpItem;
            }
        }
        List<TraceCallVo> result = resultDumps.stream()
                .map(dumpEntity -> {
                    SpanStackTraceEntity spanFirstStackTrace = traceSpanService.findFirstStackTraceBySpanId(
                            dumpEntity.getSpanId());
                    String moduleId = spanFirstStackTrace != null ? spanFirstStackTrace.getModuleId() : null;
                    String classname = spanFirstStackTrace != null ? spanFirstStackTrace.getClassname() : null;
                    String methodName = spanFirstStackTrace != null ? spanFirstStackTrace.getMethodName() : null;
                    String filename = spanFirstStackTrace != null ? spanFirstStackTrace.getFilename() : null;
                    Integer line = dumpEntity.getLine();

                    return new TraceCallVo(
                            dumpEntity.getTraceId(), dumpEntity.getSpanId(),
                            moduleId, classname, methodName, filename, line);
                }).collect(Collectors.toList());
        return result;
    }

    @GetMapping("/span/{spanId}")
    public Object getSpanInfo(@PathVariable("spanId") String spanId) {
        TraceSpanEntity traceSpanEntity = traceSpanService.findSpanBySpanId(spanId);
        return traceSpanEntity;
    }

    @RequestLog
    @PostMapping("/stack_trace/span")
    public Object getSpanStackTrace(@RequestBody ViewStackTraceForm form) {
        List<SpanStackTraceEntity> spanStackTraceEntities = traceSpanService.listSpanStackTrace(form.getSpanId(), form.getSeqInSpan());
        return spanStackTraceEntities;
    }

    @RequestLog
    @PostMapping("/view_stack_variables/span")
    public Object viewStackVariablesInSpan(@RequestBody ViewStackVariablesForm form) {
        StackVarSnapshotVo stackVarSnapshot = traceSpanService.listAllMergedSpanDumpsBySpanIdAndSeqInSpan(
                form.getSpanId(), form.getSeqInSpan() != null ? form.getSeqInSpan() : 0);
        return stackVarSnapshot;
    }

    // 找到在某个spanId,某个seqInSpan的基础上继续执行到某些breakpoints后的下一个spanId+seqInSpan
    @RequestLog(response = true)
    @PostMapping("/next_step_span_seq")
    public NextRequestResponseVo findNextStepSpanSeq(@RequestBody StepStackForm form)
            throws SpanNotFoundException {
        return traceDebugService.nextStep(form);
    }
}
