(function () {
    window.TRACE_ENDPOINT = 'http://localhost:8280/tracedebug';
    const traceId = traceStart()
    const spanId = spanStart(traceId)
    const hello = 'world'
    addSpanStackTrace(spanId)
    spanDump(spanId, 'hello', hello)
})();