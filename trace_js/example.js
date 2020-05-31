(function () {
    window.TRACE_ENDPOINT = 'http://localhost:8280/tracedebug';
    window.TRACE_MODULE_ID = 'test_frontend'
    const traceId = traceStart()
    const spanId = spanStart(traceId)
    let hello = 'world'
    function testFunc() {
        addSpanStackTrace(spanId)
        spanDump(spanId, 'hello', hello)
    }
    testFunc()
    hello = hello + hello
    spanDump(spanId, 'hello', hello)
})();