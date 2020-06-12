package com.zoowii.tracedebug.services;

import com.zoowii.tracedebug.controllers.vo.NextRequestResponseVo;
import com.zoowii.tracedebug.controllers.vo.StepStackForm;
import com.zoowii.tracedebug.exceptions.SpanNotFoundException;
import com.zoowii.tracedebug.models.SpanDumpItemEntity;
import com.zoowii.tracedebug.models.TraceSpanEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

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
                        // TODO: 继续运行直到遇到断点或者结束
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
