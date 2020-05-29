(function () {
    function getTraceEndpoint() {
        return window.TRACE_ENDPOINT
    }

    function uuidv4() {
        return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c) => {
            const r = Math.random() * 16 | 0, v = c === 'x' ? r : (r & 0x3 | 0x8)
            return v.toString(16)
        })
    }

    function callTraceApi(path) {
        const endpoint = getTraceEndpoint()
        const url = endpoint + path
        // 为了避免跨域，用访问图片的方式请求
        // TODO: 如果document.body还没ready就延迟到document.body ready后执行
        setTimeout(() => {
            const img = document.createElement('img')
            img.src = url
            document.body.append(img)
        }, 1000)
    }

    function generateSpanId() {
        return uuidv4()
    }

    function generateTraceId() {
        return uuidv4()
    }

    function getFilenameAndLineNumber() {
        const err = new Error()
        const stack = err.stack;
        const stackArr = stack.split('\n');
        let callerLogIndex = 3;
        if(stackArr.length<callerLogIndex+1) {
            return null
        }

        if (callerLogIndex !== 0) {
            const callerStackLine = stackArr[callerLogIndex];
            const filenameAndLine = callerStackLine.substring(callerStackLine.lastIndexOf('/') + 1, callerStackLine.lastIndexOf(':'))
            const filename = filenameAndLine.substring(0, filenameAndLine.indexOf(':'))
            const line = parseInt(filenameAndLine.substring(filenameAndLine.indexOf(':')+1))
            // TODO: filename可以用整个js文件的url
            return {filename: filename, line: line}
        } else {
            return null;
        }
    }
    window.traceStart = function() {
        return generateTraceId()
    }
    const spanTraceMapping = {} // spanId => traceId

    window.spanStart = function (traceId) {
        const spanId = generateSpanId()
        spanTraceMapping[spanId] = traceId

        const stackDepth = 1 // TODO
        const moduleId='test' // TODO
        const fileAndLine = getFilenameAndLineNumber();
        const classname = fileAndLine.filename
        const method = 'test' // TODO: get from error stack

        callTraceApi(`/api/trace/span_start/${traceId}/${spanId}?stack_depth=${stackDepth}`
            +`&module_id=${moduleId}&classname=${classname}&method=${method}`)
        return spanId
    }
    window.spanDump = function (spanId, varName, value) {
        // TODO: 因为很多js文件是其他工具产生的，所以这种情况考虑在参数中传入filename和源码line(编译的过程传入)
        // 纯原生js的话可以推断出来
        const fileAndLine = getFilenameAndLineNumber();
        console.log('fileAndLine=', fileAndLine)
        const seqInSpan = 1 // TODO: increment
        const line = fileAndLine.line
        callTraceApi(`/api/trace/span_dump/${spanId}?seq_in_span=${seqInSpan}&name=${varName}`
            +`&value=${value}&line=${line}`)
    }
    window.addSpanStackTrace = function (spanId) {
        const traceId = spanTraceMapping[spanId]
        if(!traceId || !spanId) {
            return
        }
        const fileAndLine = getFilenameAndLineNumber()
        const stackIndex = 1 // TODO
        const moduleId = 'test' // TODO
        const classname = '' // TODO
        const method = '' // TODO
        const line = fileAndLine.line
        const filename = fileAndLine.filename
        callTraceApi(`/api/trace/add_span_stack_trace_element?trace_id=${traceId}&span_id=${spanId}`
            +`&stack_index=${stackIndex}&module_id=${moduleId}&classname=${classname}`
            +`&method=${method}&line=${line}&filename=${filename}`)
    }
})();