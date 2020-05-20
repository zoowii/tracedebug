const rp = require('request-promise');

const endpoint = `http://localhost:8280/tracedebug`

export class TraceRpcClient {
	async getSpanStackTrace(spanId: string, seqInSpan: Number) {
		const url = `${endpoint}/api/trace/stack_trace/span`
		const res = await rp({
			method: 'POST',
			url: url,
			json: true,
			body: {
				spanId: spanId,
				seqInSpan: seqInSpan
			}
		})
		return res
	}
	async getStackVariables(spanId: string, seqInSpan: Number) {
		const url = `${endpoint}/api/trace/view_stack_variables/span`
		const reqData = {
			spanId, seqInSpan
		}
		const res = await rp({
			method: 'POST',
			url: url,
			body: reqData,
			json: true
		})
		return res
	}
	async getNextRequest(spanId: string, seqInSpan: Number, stepType: string, breakpoints) {
		const url = `${endpoint}/api/trace/next_step_span_seq`
		const reqData = {
			currentSpanId: spanId,
			currentSeqInSpan: seqInSpan,
			stepType: stepType
		}
		const res = await rp({
			method: 'POST',
			url: url,
			body: reqData,
			json: true
		})
		return res
	}
}
