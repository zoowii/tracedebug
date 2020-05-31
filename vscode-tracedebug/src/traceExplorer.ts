import * as vscode from 'vscode';
// import * as fs from 'fs';
// import * as path from 'path';
import {TraceRpcClient} from './traceRpcClient';

const rpcClient = new TraceRpcClient();

export class TraceNodeProvider implements vscode.TreeDataProvider<vscode.TreeItem> {

	private _onDidChangeTreeData: vscode.EventEmitter<TraceNode | SpanNode | undefined> = new vscode.EventEmitter<TraceNode | SpanNode | undefined>();
	readonly onDidChangeTreeData: vscode.Event<TraceNode | SpanNode | undefined> = this._onDidChangeTreeData.event;

	constructor() {
		console.log('TraceNodeProvider constructed')
	}

	refresh(): void {
		console.log('traceNodeProvider refresh')
		this._onDidChangeTreeData.fire();
	}

	getTreeItem(element: TraceNode): vscode.TreeItem {
		return element;
	}

	getChildren(element?: TraceNode): Thenable<vscode.TreeItem[]> {
		console.log('traceNodeProvider getChildren called with element', element)

		if (element) {
			// 如果是trace node,则显示trace下各span
			if(element.contextValue === 'trace') {
				return this.getSpanNodeList((<TraceNode>element).traceId)
			} else {
				// 如果是span node，不显示下级
				return Promise.resolve([]);
			}
		} else {
			return this.getTraceNodeList();
		}

	}

	private async getSpanNodeList(traceId: string): Promise<vscode.TreeItem[]> {
		const res = await rpcClient.listSpansOfTrace(traceId)
		console.log('span list', res)
		const toNode = (spanItem: any, spanItemBefore: any): SpanNode => {
			// 点击span的时候要设置这个traceId的这个spanId作为待调试对象，并toast提示用户
			return new SpanNode(spanItem.spanId, spanItem.traceId, spanItemBefore?spanItemBefore.spanId:undefined, spanItem.moduleId, spanItem.classname,
				 spanItem.methodName, spanItem.stackDepth, vscode.TreeItemCollapsibleState.None, {
				command: 'extension.openTraceAndSpan',
				title: '',
				arguments: [spanItem]
			})
		}
		const spanNodes: Array<SpanNode> = []
		for(let i = 0;i<res.length;i++) {
			const item = toNode(res[i], i>0?res[i-1]:undefined)
			spanNodes.push(item)
		}
		return spanNodes
	}

	private async getTraceNodeList(): Promise<vscode.TreeItem[]> {
		const res = await rpcClient.listTraces()
		console.log('trace list', res)
		const toNode = (traceItem: any): TraceNode => {
			return new TraceNode(traceItem, traceItem, vscode.TreeItemCollapsibleState.Collapsed)
		}
		const traceNodes = res.data.map(toNode)
		return traceNodes
	}
}

export class TraceNode extends vscode.TreeItem {
	constructor(
		public readonly label: string,
		public traceId: string,
		public readonly collapsibleState: vscode.TreeItemCollapsibleState,
		public readonly command?: vscode.Command
	) {
		super(label, collapsibleState);
	}

	get tooltip(): string {
		return `${this.label}-${this.traceId}`;
	}

	get description(): string {
		return this.traceId;
	}

	// iconPath = {
	// 	light: path.join(__filename, '..', '..', 'resources', 'light', 'dependency.svg'),
	// 	dark: path.join(__filename, '..', '..', 'resources', 'dark', 'dependency.svg')
	// };

	contextValue = 'trace';
}

export class SpanNode extends vscode.TreeItem {
	constructor(public spanId: string, public traceId: string, public beforeSpanId: string, public moduleId: string,
		public classname: string, public methodName: string, public stackDepth: number,
		  public readonly collapsibleState: vscode.TreeItemCollapsibleState,
		public readonly command?: vscode.Command) {
		super(`span-${spanId}`, collapsibleState)
	}

	get tooltip(): string {
		return `${this.classname}-${this.methodName}`;
	}

	get description(): string {
		return `${this.moduleId}-${this.classname}-${this.methodName}`;
	}

	// iconPath = {
	// 	light: path.join(__filename, '..', '..', 'resources', 'light', 'dependency.svg'),
	// 	dark: path.join(__filename, '..', '..', 'resources', 'dark', 'dependency.svg')
	// };

	contextValue = 'span';
}