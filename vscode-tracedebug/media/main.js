(function () {
	if (!traceId) {
		return;
	}
	const vscode = acquireVsCodeApi();
	vscode.setState(traceId)

	function callApiGet(path) {
		const url = `${rpcEndpoint}${path}`
		return axios.get(url).then(function (response) {
			if (response.status !== 200) {
				throw new Error(response.data)
			}
			return response.data
		})
	}

	new Vue({
		el: '#app',
		data: {
			traceId: traceId,
			traceSpans: [],
			traceCalls: []
		},
		mounted() {
			this.loadTraceSpans()
			this.loadTraceTopCalls(0)
		},
		methods: {
			loadTraceSpans() {
				callApiGet(`/api/trace/list_spans/${traceId}`)
					.then(spans => {
						this.traceSpans = spans
					})
					.catch(e => {
						console.log('error', e)
					})
			},
			loadTraceTopCalls() {
				callApiGet(`/api/trace/list_top_calls/${traceId}`)
					.then(calls => {
						console.log('calls', calls)
						this.traceCalls = calls
					})
					.catch(e => {
						console.log('error', e)
					})
			}
		}
	});
})()
