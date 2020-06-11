package com.zoowii.tracedebug.services;

import com.zoowii.tracedebug.controllers.vo.StackVarSnapshotVo;
import com.zoowii.tracedebug.controllers.vo.VarValueVo;
import com.zoowii.tracedebug.daos.SpanDumpItemRepository;
import com.zoowii.tracedebug.daos.SpanStackTraceRepository;
import com.zoowii.tracedebug.daos.TraceSpanRepository;
import com.zoowii.tracedebug.http.BeanPage;
import com.zoowii.tracedebug.http.BeanPaginator;
import com.zoowii.tracedebug.models.SpanDumpItemEntity;
import com.zoowii.tracedebug.models.SpanStackTraceEntity;
import com.zoowii.tracedebug.models.TraceSpanEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class TraceSpanService {
    @Resource
    private TraceSpanRepository traceSpanRepository;
    @Resource
    private SpanDumpItemRepository spanDumpItemRepository;
    @Resource
    private SpanStackTraceRepository spanStackTraceRepository;

    public List<TraceSpanEntity> findAllByTraceIdOrderByIdAsc(String traceId) {
        return traceSpanRepository.findAllByTraceIdOrderByIdAsc(traceId);
    }

    public BeanPage<String> listTraceIds(BeanPaginator paginator) {
        List<Map<String, Object>> items = traceSpanRepository.findAllDistinctTraceIds(paginator.getOffset(), paginator.getLimit());
        List<String> traceIds = items.stream().map(x->x.get("trace_id").toString()).collect(Collectors.toList());
        long total = traceSpanRepository.countDistinctTraceIds();
        return new BeanPage<>(traceIds, total);
    }

    public TraceSpanEntity findSpanBySpanId(String spanId) {
        return traceSpanRepository.findBySpanId(spanId);
    }

    public TraceSpanEntity findFirstSpanOfTrace(String traceId) {
        return traceSpanRepository.findFirstByTraceIdOrderByIdAsc(traceId);
    }

    public List<SpanStackTraceEntity> listSpanStackTrace(String spanId, Integer seqInSpan) {
        List<SpanStackTraceEntity> spanStackTraceEntities = spanStackTraceRepository.findAllBySpanIdOrderByStackIndexAsc(spanId);
        if(spanStackTraceEntities.isEmpty()) {
            return spanStackTraceEntities;
        }
        if(seqInSpan != null) {
            SpanDumpItemEntity spanDumpItemEntity = spanDumpItemRepository.findFirstBySpanIdAndSeqInSpanOrderByIdAsc(spanId, seqInSpan);
            if(spanDumpItemEntity != null) {
                spanStackTraceEntities.get(0).setLine(spanDumpItemEntity.getLine());
            }
        }
        return spanStackTraceEntities;
    }

    public List<SpanDumpItemEntity> listSpanDumpItems(String spanId) {
        return spanDumpItemRepository.findAllBySpanIdOrderBySeqInSpan(spanId);
    }

    public List<SpanDumpItemEntity> findAllBySpanIdAndSeqInSpanGreaterThanOrderBySeqInSpan(String spanId, int seqInSpan) {
        return spanDumpItemRepository.findAllBySpanIdAndSeqInSpanGreaterThanOrderBySeqInSpan(spanId, seqInSpan);
    }

    public SpanDumpItemEntity findFirstBySpanIdAndSeqInSpan(String spanId, int seqInSpan) {
        return spanDumpItemRepository.findFirstBySpanIdAndSeqInSpanOrderByIdAsc(spanId, seqInSpan);
    }

    public SpanDumpItemEntity findFirstDumpBySpanId(String spanId) {
        return spanDumpItemRepository.findFirstBySpanIdOrderByIdAsc(spanId);
    }

    public List<SpanDumpItemEntity> findAllDumpsByTraceIdAndIdGreaterThanOrderByIdAsc(String traceId, long id) {
        return spanDumpItemRepository.findAllByTraceIdAndIdGreaterThanOrderByIdAsc(traceId, id);
    }

    /**
     * 根据spanId,当前seqInSpan(null就表示0), line找出当前span的variables快照的值
     * 同一个span中，大seqInSpan会覆盖小seqInSpan中的变量值，也包含之前的变量，后端需要做合并
     */
    public StackVarSnapshotVo listAllMergedSpanDumpsBySpanIdAndSeqInSpan(String spanId, int seqInSpan) {
        List<SpanDumpItemEntity> spanDumpItemsOfSpan = spanDumpItemRepository.findAllBySpanIdOrderBySeqInSpan(spanId);
        spanDumpItemsOfSpan = spanDumpItemsOfSpan.stream()
                .filter(x -> x.getSeqInSpan()!=null && x.getSeqInSpan()<=seqInSpan)
                .collect(Collectors.toList());
        Map<String, String> mergedValues = new HashMap<>();
        for(SpanDumpItemEntity item : spanDumpItemsOfSpan) {
            mergedValues.put(item.getName(), item.getValue());
        }
        StackVarSnapshotVo stackVarSnapshot = new StackVarSnapshotVo();
        stackVarSnapshot.setVariableValues(new ArrayList<>());
        for(String name : mergedValues.keySet()) {
            String value = mergedValues.get(name);
            stackVarSnapshot.getVariableValues().add(new VarValueVo(name, value));
        }
        return stackVarSnapshot;
    }

    public TraceSpanEntity saveTraceSpan(TraceSpanEntity record) {
        return traceSpanRepository.save(record);
    }

    public SpanStackTraceEntity saveSpanStackTrace(SpanStackTraceEntity record) {
        return spanStackTraceRepository.save(record);
    }

    public SpanDumpItemEntity saveSpanDumpItem(SpanDumpItemEntity record) {
        return spanDumpItemRepository.save(record);
    }

    public SpanStackTraceEntity findSpanStackTraceBySpanIdAndStackIndex(String spanId, Integer stackIndex) {
        return spanStackTraceRepository.findFirstBySpanIdAndStackIndex(spanId, stackIndex);
    }

}
