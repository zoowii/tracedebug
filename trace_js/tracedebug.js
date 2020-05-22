(function () {
    function getTraceEndpoint() {
        return window.TRACE_ENDPOINT
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
    window.spanStart = function () {
        // TODO: generate spanId

        // TODO: 因为很多js文件是其他工具产生的，所以这种情况考虑在参数中传入filename和源码line(编译的过程传入)
        // 纯原生js的话可以推断出来
        const fileAndLine = getFilenameAndLineNumber();
        console.log('fileAndLine=',fileAndLine)
        // TODO: use gif load to call log api
    };
    window.spanDump = function () {
        // TODO
    };
    window.addSpanStackTrace = function () {
        // TODO
    }
})();