package com.zoowii.tracedebug.controllers;

import com.zoowii.tracedebug.controllers.vo.*;
import com.zoowii.tracedebug.exceptions.SpanNotFoundException;
import com.zoowii.tracedebug.http.BeanPage;
import com.zoowii.tracedebug.http.BeanPaginator;
import com.zoowii.tracedebug.models.SpanDumpItemEntity;
import com.zoowii.tracedebug.models.SpanStackTraceEntity;
import com.zoowii.tracedebug.models.TraceSpanEntity;
import com.zoowii.tracedebug.services.TraceSpanService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/trace")
public class TraceController {
    @Resource
    private TraceSpanService traceSpanService;

    /**
     * 开启新span时上报接口
     * @return
     */
    @GetMapping("/span_start/{traceId}/{spanId}")
    public @ResponseBody Object startNewSpan(
            @PathVariable("traceId") String traceId,
            @PathVariable("spanId") String spanId,
            @RequestParam("stack_depth") Integer stackDepth,
            @RequestParam("module_id") String moduleId,
            @RequestParam("classname") String classname,
            @RequestParam("method") String methodName) {
        log.info("startNewSpan called");
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
        log.info("addSpanStackTraceElement called");
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
    @GetMapping("/span_dump/{spanId}")
    public @ResponseBody Object dumpVarInSpan(
            @PathVariable("spanId") String spanId,
            @RequestParam("seq_in_span") Integer seqInSpan,
            @RequestParam("name") String name,
            @RequestParam("value") String value,
            @RequestParam("line") Integer line) {
        log.info("dumpVarInSpan called");
        if(StringUtils.isEmpty(spanId) || StringUtils.isEmpty(name)) {
            return "empty spanId or name";
        }
        TraceSpanEntity traceSpanEntity = traceSpanService.findSpanBySpanId(spanId);
        if(traceSpanEntity==null) {
            return "can't find spanId " + spanId;
        }
        SpanDumpItemEntity spanDumpItemEntity = new SpanDumpItemEntity();
        spanDumpItemEntity.setTraceId(traceSpanEntity.getTraceId());
        spanDumpItemEntity.setSpanId(spanId);
        spanDumpItemEntity.setSeqInSpan(seqInSpan);
        spanDumpItemEntity.setName(name);
        spanDumpItemEntity.setValue(value);
        spanDumpItemEntity.setLine(line);
        spanDumpItemEntity = traceSpanService.saveSpanDumpItem(spanDumpItemEntity);
        return spanDumpItemEntity;
    }

    @PostMapping("/list")
    public @ResponseBody BeanPage<String> listTraces(@RequestBody BeanPaginator paginator) {
        log.info("listTraces form {}", paginator);
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

    @PostMapping("/stack_trace/span")
    public @ResponseBody Object getSpanStackTrace(@RequestBody ViewStackTraceForm form) {
        log.info("getSpanStackTrace form {}", form);
        List<SpanStackTraceEntity> spanStackTraceEntities = traceSpanService.listSpanStackTrace(form.getSpanId(), form.getSeqInSpan());
        return spanStackTraceEntities;
    }

    @PostMapping("/view_stack_variables/span")
    public @ResponseBody Object viewStackVariablesInSpan(@RequestBody ViewStackVariablesForm form) {
        log.info("viewStackVariablesInSpan form {}", form);
        StackVarSnapshotVo stackVarSnapshot = traceSpanService.listAllMergedSpanDumpsBySpanIdAndSeqInSpan(
                form.getSpanId(), form.getSeqInSpan()!=null?form.getSeqInSpan():0);
        return stackVarSnapshot;
    }

    // 找到在某个spanId,某个seqInSpan的基础上继续执行到某些breakpoints后的下一个spanId+seqInSpan
    @PostMapping("/next_step_span_seq")
    public @ResponseBody NextRequestResponseVo findNextStepSpanSeq(@RequestBody StepStackForm form)
            throws SpanNotFoundException {
        log.info("findNextStepSpanSeq form {}", form);

        if(StringUtils.isEmpty(form.getCurrentSpanId()) && !StringUtils.isEmpty(form.getTraceId())) {
            // 只提供了traceId，要找此traceId的第一个spanId
            TraceSpanEntity firstTraceSpan = traceSpanService.findFirstSpanOfTrace(form.getTraceId());
            if(firstTraceSpan == null) {
                throw new SpanNotFoundException("can't find trace " + form.getTraceId());
            }
            return new NextRequestResponseVo(firstTraceSpan.getTraceId(), firstTraceSpan.getSpanId(), 0);
        }

        int currentSeqInSpan = form.getCurrentSeqInSpan();
        String currentSpanId = form.getCurrentSpanId();
        String stepType = form.getStepType();
        TraceSpanEntity currentTraceSpanEntity = traceSpanService.findSpanBySpanId(currentSpanId);
        if(currentTraceSpanEntity == null) {
            throw new SpanNotFoundException("can't find span " + form.getCurrentSpanId());
        }
        String traceId = currentTraceSpanEntity.getTraceId();
        List<TraceSpanEntity> traceSpansInTrace = traceSpanService.findAllByTraceIdOrderByIdAsc(traceId);
        // 排除比当前spanId更早的span记录
        List<TraceSpanEntity> spansAfterOrSelf = new ArrayList<>();
        boolean foundCurrentSpan = false;
        for(TraceSpanEntity spanEntity : traceSpansInTrace) {
            if(foundCurrentSpan) {
                spansAfterOrSelf.add(spanEntity);
                continue;
            }
            if(spanEntity.getSpanId()!=null&&spanEntity.getSpanId().equals(currentSpanId)) {
                foundCurrentSpan = true;
                spansAfterOrSelf.add(spanEntity);
            }
        }

        // 在spansAfterOrSelf中依次找到满足 (currentSpanId, currentSeqInSpan)之后并满足 stepType的下一个(spanId, seqInSpan)
        // 根据 stepType，要计算span的stackDepth判断是否暂停到断点
        for(TraceSpanEntity spanEntity : spansAfterOrSelf) {
            if(spanEntity.getSpanId().equals(currentSpanId)) {
                if("step_out".equals(stepType)) {
                    continue; // step out 需要跳出当前span
                }
                // 只取这个span中比currentSeqInSpan大的项，然后根据stepType和stackDepth处理
                SpanDumpItemEntity currentSpanDumpItem = traceSpanService.findFirstBySpanIdAndSeqInSpan(currentSpanId, currentSeqInSpan);
                if(currentSpanDumpItem == null) {
                    continue;
                }
                List<SpanDumpItemEntity> spanDumpItemEntities = traceSpanService.findAllBySpanIdAndSeqInSpanGreaterThanOrderBySeqInSpan(
                        spanEntity.getSpanId(), currentSeqInSpan);
                // TODO: 排除掉同一批dump但是dump的是不同变量的情况（这种情况应该在上报时就用同一个seqInSpan)
                if(!spanDumpItemEntities.isEmpty()) {
                    // 断点调试的下一个暂停点
                    return new NextRequestResponseVo(traceId, spanEntity.getSpanId(),
                            spanDumpItemEntities.get(0).getSeqInSpan());
                } else {
                    continue;
                }
            } else {
                // 当前spanId之后的span的情况，从小到大依次处理各seqInSpan的情况，根据stepType和stackDepth处理
                // TODO: 根据breakpoints处理，也可能中途停下来
                switch (stepType) {
                    case "step_out": {
                        if(spanEntity.getStackDepth()>=currentTraceSpanEntity.getStackDepth()) {
                            continue;
                        }
                        return new NextRequestResponseVo(traceId, spanEntity.getSpanId(), 0);
                    }
                    case "step_over": {
                        if(spanEntity.getStackDepth()>currentTraceSpanEntity.getStackDepth()) {
                            continue;
                        }
                        return new NextRequestResponseVo(traceId, spanEntity.getSpanId(), 0);
                    }
                    case "step_in": {
                        // default
                    } break;
                    case "continue": {
                        // TODO: 继续运行直到遇到断点或者结束
                    } break;
                    default: {
                        // default
                    }
                }
                List<SpanDumpItemEntity> spanDumpItemEntities = traceSpanService.findAllBySpanIdAndSeqInSpanGreaterThanOrderBySeqInSpan(
                        spanEntity.getSpanId(), -1);
                return new NextRequestResponseVo(traceId, spanEntity.getSpanId(), 0);
            }
        }

        return null;
    }
}
