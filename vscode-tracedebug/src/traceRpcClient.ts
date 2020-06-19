const rp = require('request-promise');
import * as fs from 'fs'
import * as path from 'path'
import * as os from 'os'
import {readFileText} from './utils'

const endpoint = `http://localhost:8280/tracedebug`

const fileNameResolveCache: {} = {} // 缓存的filename resolve信息, moduleId+filename => real file path

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
	async getStackVariables(spanId ?: string, seqInSpan ?: Number) {
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
	async listTraces() {
		const url = `${endpoint}/api/trace/list`
		const reqData = {
			page: 1,
			pageSize: 20
		}
		const res = await rp({
			method: 'POST',
			url: url,
			body: reqData,
			json: true
		})
		return res
	}
	async listSpansOfTrace(traceId: string) {
		const url = `${endpoint}/api/trace/list_spans/${traceId}`
		const res = await rp({
			method: 'GET',
			url: url,
			json: true
		})
		return res
	}
	async getNextRequest(traceId: string | undefined, spanId : string | undefined,
		 seqInSpan: Number | undefined, stepType: string, breakpoints: Array<object>) {
		const url = `${endpoint}/api/trace/next_step_span_seq`
		const reqData = {
			traceId: traceId,
			currentSpanId: spanId,
			currentSeqInSpan: seqInSpan,
			stepType: stepType,
			breakpoints: breakpoints
		}
		const res = await rp({
			method: 'POST',
			url: url,
			body: reqData,
			json: true
		})
		return res
	}
	async resolveFilenameWithCache(moduleId: string, classname: string, filename: string): Promise<string | undefined> {
		const key = moduleId + '@' + filename
		if(fileNameResolveCache[key]) {
			return fileNameResolveCache[key]
		}
		const fileRealPath = await this.resolveFilename(moduleId, classname, filename)
		if(!fileRealPath) {
			return fileRealPath
		}
		fileNameResolveCache[key] = fileRealPath
		return fileRealPath
	}
	async resolveFilename(moduleId: string, classname: string, filename: string): Promise<string | undefined> {
		// TODO: 从用户主目录加载模块映射文件，没找到就提示用户创建. 也可以通过命令来修改模块映射文件
		// TODO: 增加命令打开或者创建模块映射文件
		const traceModulesConfigPath = path.resolve(path.join(os.homedir(), `/.trace_modules`))
		if(!fs.existsSync(traceModulesConfigPath)) {
			return undefined
		}
		const modulesConfigStr = await readFileText(traceModulesConfigPath)
		try {
			const modulesConfig = JSON.parse(modulesConfigStr)
			if(!modulesConfig || !modulesConfig[moduleId]) {
				return undefined
			}
			const module = modulesConfig[moduleId]
			if(module['files'] && module['files'][filename]) {
				return module['files'][filename]
			}
			const workspace = module['workspace']
			return path.join(workspace, filename)
		} catch(e) {
			throw e;
		}
	}
}

let currentTraceId: string = '' // ''  'test' is for development
let currentSpanId: string | undefined = undefined

export function setCurrentTraceId(traceId: string, spanId?: string) {
	currentTraceId = traceId
	currentSpanId = spanId
}

export function getCurrentTraceId(): string {
	return currentTraceId
}

export function getCurrentSpanId(): string | undefined {
	return currentSpanId
}

export function getRpcEndpoint(): string {
	return endpoint
}
