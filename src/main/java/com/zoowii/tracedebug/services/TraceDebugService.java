package com.zoowii.tracedebug.services;

import com.zoowii.tracedebug.controllers.vo.BreakpointVo;
import com.zoowii.tracedebug.controllers.vo.NextRequestResponseVo;
import com.zoowii.tracedebug.controllers.vo.StepStackForm;
import com.zoowii.tracedebug.exceptions.SpanNotFoundException;
import com.zoowii.tracedebug.models.SpanDumpItemEntity;
import com.zoowii.tracedebug.models.SpanStackTraceEntity;
import com.zoowii.tracedebug.models.TraceSpanEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

@Slf4j
@Service
public class TraceDebugService {
    @Resource
    private TraceSpanService traceSpanService;

    public NextRequestResponseVo nextStep(StepStackForm form) throws SpanNotFoundException {
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
        SpanDumpItemEntity currentSpanAndSeqInSpanInfo = traceSpanService.findFirstBySpanIdAndSeqInSpanOrderByIdAsc(currentSpanId, currentSeqInSpan);
        if(currentSpanAndSeqInSpanInfo==null) {
            SpanDumpItemEntity spanFirstSeqInSpanId = traceSpanService.findFirstDumpBySpanId(currentSpanId);
            if(spanFirstSeqInSpanId==null) {
                throw new SpanNotFoundException("invalid span " + form.getCurrentSpanId());
            }
            return new NextRequestResponseVo(traceId, currentSpanId, spanFirstSeqInSpanId.getSeqInSpan());
        }

        // 之后的所有dumps(主要是spanId, seqInSpan的有序序列)
        List<SpanDumpItemEntity> spanDumpItemEntitiesAfter = traceSpanService.findAllDumpsByTraceIdAndIdGreaterThanOrderByIdAsc(
                traceId, currentSpanAndSeqInSpanInfo.getId());

        // a方法调用b方法时，调试的时候要根据dump的顺序从a进入b然后返回a

        final List<BreakpointVo> breakpoints = form.getBreakpoints();
        // 判断是否到某个断点的函数
        BiFunction<SpanDumpItemEntity, TraceSpanEntity, Boolean> inBreakpoints = (spanDump, spanEntity) -> {
            if(breakpoints==null) {
                return false;
            }
            // TODO: 找到span所在的filename. 应该直接可以从spanEntity中得到
            List<SpanStackTraceEntity> spanStackTraceEntities = traceSpanService.listSpanStackTrace(
                    spanDump.getSpanId(), spanDump.getSeqInSpan());
            if(spanStackTraceEntities.isEmpty()) {
                return false;
            }
            for(BreakpointVo bp : breakpoints) {
                if(bp.getFilename()==null) {
                    continue;
                }
                if(bp.getLine()==null || spanDump.getLine()==null) {
                    continue;
                }
                if(bp.getModuleId()!=null && !bp.getModuleId().equals(spanEntity.getModuleId())) {
                    continue;
                }
                String spanFilename = spanStackTraceEntities.get(0).getFilename();
                if(spanFilename==null) {
                    continue;
                }
                if(!spanFilename.endsWith(bp.getFilename())) {
                    continue;
                }
                // 如果行数间隔差距不超过1，算进入断点
                int lineDistance = Math.abs(spanDump.getLine() - bp.getLine());
                if(lineDistance<=1) {
                    return true;
                }
            }
            return false;
        };

        // 在spansAfterOrSelf中依次找到满足 (currentSpanId, currentSeqInSpan)之后并满足 stepType的下一个(spanId, seqInSpan)
        // 根据 stepType，要计算span的stackDepth判断是否暂停到断点
        for(SpanDumpItemEntity spanDumpEntity : spanDumpItemEntitiesAfter) {
            if(spanDumpEntity.getSpanId().equals(currentSpanId)) {
                if("step_out".equals(stepType)) {
                    continue; // step out 需要跳出当前span
                }
                // TODO: when is "continue" stepType
                return new NextRequestResponseVo(traceId, spanDumpEntity.getSpanId(),
                        spanDumpEntity.getSeqInSpan());
            } else {
                // 当前spanId之后的span的情况，从小到大依次处理各seqInSpan的情况，根据stepType和stackDepth处理
                // TODO: 根据breakpoints处理，也可能中途停下来
                TraceSpanEntity spanEntity = traceSpanService.findSpanBySpanId(spanDumpEntity.getSpanId());
                if(spanEntity==null) {
                    continue;
                }
                switch (stepType) {
                    case "step_out": {
                        if(spanEntity.getModuleId().equals(currentTraceSpanEntity.getModuleId())
                                && spanEntity.getStackDepth()>=currentTraceSpanEntity.getStackDepth()) {
                            continue;
                        }
                        return new NextRequestResponseVo(traceId, spanEntity.getSpanId(), 0);
                    }
                    case "step_over": {
                        if(spanEntity.getModuleId().equals(currentTraceSpanEntity.getModuleId())
                                && spanEntity.getStackDepth()>currentTraceSpanEntity.getStackDepth()) {
                            continue;
                        }
                        return new NextRequestResponseVo(traceId, spanEntity.getSpanId(), 0);
                    }
                    case "step_in": {
                        // default
                    } break;
                    case "continue": {
                        // 继续运行直到遇到断点或者结束
                        if(breakpoints!=null && inBreakpoints.apply(spanDumpEntity, spanEntity)) {
                            return new NextRequestResponseVo(traceId, spanDumpEntity.getSpanId(), spanDumpEntity.getSeqInSpan());
                        }
                    } break;
                    default: {
                        // default
                    }
                }
                return new NextRequestResponseVo(traceId, spanEntity.getSpanId(), spanDumpEntity.getSeqInSpan());
            }
        }

        return null;
    }
}
