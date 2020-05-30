(function () {
    function getTraceEndpoint() {
        return window.TRACE_ENDPOINT
    }

    function getTraceModuleId() {
        return window.TRACE_MODULE_ID || 'frontend'
    }

    function uuidv4() {
        return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c) => {
            const r = Math.random() * 16 | 0, v = c === 'x' ? r : (r & 0x3 | 0x8)
            return v.toString(16)
        })
    }

    let readyDone = false
    let jobListToDoAfterDocumentReader = []
    document.addEventListener('readystatechange', (event) => {
        readyDone = true
        for (const job of jobListToDoAfterDocumentReader) {
            try {
                job()
            } catch (e) {
                console.log(e)
            }
        }
        jobListToDoAfterDocumentReader = []
    });

    function callTraceApi(path) {
        const endpoint = getTraceEndpoint()
        const url = endpoint + path
        // 为了避免跨域，用访问图片的方式请求
        const job = () => {
            const img = document.createElement('img')
            img.src = url
            img.style = 'display: none'
            document.body.append(img)
            setTimeout(() => {
                // 3秒后删除这个img，避免添加了太多无用的元素
                img.remove()
            }, 3000)
        }
        if (readyDone) {
            job()
        } else {
            jobListToDoAfterDocumentReader.push(job)
        }
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
        if (stackArr.length < callerLogIndex + 1) {
            return null
        }

        if (callerLogIndex !== 0) {
            const callerStackLine = stackArr[callerLogIndex];
            const filenameAndLine = callerStackLine.substring(callerStackLine.lastIndexOf('/') + 1, callerStackLine.lastIndexOf(':'))
            const filename = filenameAndLine.substring(0, filenameAndLine.indexOf(':'))
            const line = parseInt(filenameAndLine.substring(filenameAndLine.indexOf(':') + 1))
            // TODO: filename可以用整个js文件的url
            let funcName = 'annoy'
            if (callerStackLine.indexOf('(') > callerStackLine.indexOf('at ')) {
                funcName = callerStackLine.substring(callerStackLine.indexOf('at ') + 3, callerStackLine.indexOf('(')).trim()
            }
            return {filename: filename, line: line, funcName: funcName}
        } else {
            return null;
        }
    }

    window.traceStart = function () {
        return generateTraceId()
    }
    const spanTraceMapping = {} // spanId => traceId
    const traceNeedLog = {} // traceId => boolean, 某个traceId是否被采样需要记录trace log
    const spanSeqGen = {} // spanId => seqGenerator

    window.spanStart = function (traceId) {
        const spanId = generateSpanId()
        spanTraceMapping[spanId] = traceId
        spanSeqGen[spanId] = 0
        traceNeedLog[traceId] = true // 目前所有traceId都是enabled

        const fileAndLine = getFilenameAndLineNumber();
        const stackDepth = 1 // TODO: 因为目前只传递一层函数栈，所以这里深度为 1
        const moduleId = getTraceModuleId()
        const classname = fileAndLine.filename
        const method = fileAndLine.funcName

        callTraceApi(`/api/trace/span_start/${traceId}/${spanId}?stack_depth=${stackDepth}`
            + `&module_id=${moduleId}&classname=${classname}&method=${method}`)
        return spanId
    }
    window.spanDump = function (spanId, varName, value, line) {
        const traceId = spanTraceMapping[spanId]
        if (!traceId || !spanId) {
            return
        }
        if (!traceNeedLog[traceId]) {
            return;
        }
        // 纯原生js的话可以推断出来
        const fileAndLine = getFilenameAndLineNumber();
        console.log('fileAndLine=', fileAndLine)
        const seqInSpan = spanSeqGen[spanId] || 0
        spanSeqGen[spanId] = seqInSpan + 1
        // 因为很多js文件是其他工具产生的，所以这种情况考虑在参数中传入filename和源码line(编译的过程传入)
        line = (line !== undefined) ? line : fileAndLine.line
        callTraceApi(`/api/trace/span_dump?trace_id=${traceId}&span_id=${spanId}&seq_in_span=${seqInSpan}&name=${varName}`
            + `&value=${value}&line=${line}`)
    }
    window.addSpanStackTrace = function (spanId, line) {
        const traceId = spanTraceMapping[spanId]
        if (!traceId || !spanId) {
            return
        }
        if (!traceNeedLog[traceId]) {
            return;
        }
        const fileAndLine = getFilenameAndLineNumber()
        const stackIndex = 0
        const moduleId = getTraceModuleId()
        const classname = ''
        const method = fileAndLine.funcName
        line = (line !== undefined) ? line : fileAndLine.line
        const filename = fileAndLine.filename
        callTraceApi(`/api/trace/add_span_stack_trace_element?trace_id=${traceId}&span_id=${spanId}`
            + `&stack_index=${stackIndex}&module_id=${moduleId}&classname=${classname}`
            + `&method=${method}&line=${line}&filename=${filename}`)
    }
})();