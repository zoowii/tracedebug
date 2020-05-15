package com.zoowii.tracedebug.services;

import com.zoowii.tracedebug.daos.SpanDumpItemRepository;
import com.zoowii.tracedebug.daos.SpanStackTraceRepository;
import com.zoowii.tracedebug.daos.TraceSpanRepository;
import com.zoowii.tracedebug.models.TraceSpanEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Slf4j
@Service
public class TraceSpanService {
    @Resource
    private TraceSpanRepository traceSpanRepository;
    @Resource
    private SpanDumpItemRepository spanDumpItemRepository;
    @Resource
    private SpanStackTraceRepository spanStackTraceRepository;

    public List<TraceSpanEntity> findSpansByTraceId(String traceId) {
        return traceSpanRepository.findAllByTraceId(traceId);
    }
}
