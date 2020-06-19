import * as vscode from 'vscode';
import * as path from 'path'
import { getRpcEndpoint } from './traceRpcClient'

export class TraceInfoPanel {
	public static currentPanel: TraceInfoPanel | undefined
	public static readonly viewType = 'traceInfo'
	private traceId: string
	private readonly _panel: vscode.WebviewPanel;
	private readonly _extensionPath: string;
	private _disposables: vscode.Disposable[] = [];

	public static createOrShow(extensionPath: string, traceId: string) {
		const column = vscode.window.activeTextEditor
			? vscode.window.activeTextEditor.viewColumn
			: undefined;

		// If we already have a panel, show it.
		if (TraceInfoPanel.currentPanel) {
			TraceInfoPanel.currentPanel.traceId = traceId
			TraceInfoPanel.currentPanel._update()
			TraceInfoPanel.currentPanel._panel.reveal(column);
			return;
		}

		// Otherwise, create a new panel.
		const panel = vscode.window.createWebviewPanel(
			TraceInfoPanel.viewType,
			'Trace ' + traceId,
			column || vscode.ViewColumn.One,
			{
				// Enable javascript in the webview
				enableScripts: true,

				// And restrict the webview to only loading content from our extension's `media` directory.
				localResourceRoots: [vscode.Uri.file(path.join(extensionPath, 'media'))]
			}
		);

		TraceInfoPanel.currentPanel = new TraceInfoPanel(panel, extensionPath, traceId);
	}

	public static revive(panel: vscode.WebviewPanel,
		 extensionPath: string, traceId: string) {
			TraceInfoPanel.currentPanel = new TraceInfoPanel(panel, extensionPath, traceId);
	}

	private constructor(panel: vscode.WebviewPanel,
		 extensionPath: string, traceId: string) {
		this._panel = panel;
		this._extensionPath = extensionPath;
		this.traceId = traceId

		// Set the webview's initial html content
		this._update();

		// Listen for when the panel is disposed
		// This happens when the user closes the panel or when the panel is closed programatically
		this._panel.onDidDispose(() => this.dispose(), null, this._disposables);

		// Update the content based on view changes
		this._panel.onDidChangeViewState(
			e => {
				if (this._panel.visible) {
					this._update();
				}
			},
			null,
			this._disposables
		);

		// Handle messages from the webview
		this._panel.webview.onDidReceiveMessage(
			message => {
				switch (message.command) {
					case 'alert':
						vscode.window.showErrorMessage(message.text);
						return;
				}
			},
			null,
			this._disposables
		);
	}

	public doRefactor() {
		// Send a message to the webview webview.
		// You can send any JSON serializable data.
		this._panel.webview.postMessage({ command: 'refactor' });
	}

	public dispose() {
		TraceInfoPanel.currentPanel = undefined;

		// Clean up our resources
		this._panel.dispose();

		while (this._disposables.length) {
			const x = this._disposables.pop();
			if (x) {
				x.dispose();
			}
		}
	}

	private _update() {
		const webview = this._panel.webview;

		this._updateForTrace(webview, this.traceId)

		// Vary the webview's content based on where it is located in the editor.
		// switch (this._panel.viewColumn) {
		// 	case vscode.ViewColumn.Two:
		// 		this._updateForCat(webview, 'Compiling Cat');
		// 		return;

		// 	case vscode.ViewColumn.Three:
		// 		this._updateForCat(webview, 'Testing Cat');
		// 		return;

		// 	case vscode.ViewColumn.One:
		// 	default:
		// 		this._updateForCat(webview, 'Coding Cat');
		// 		return;
		// }
	}

	private _updateForTrace(webview: vscode.Webview, traceId: string) {
		this._panel.title = `Trace ${traceId}`;
		this._panel.webview.html = this._getHtmlForWebview(webview, traceId);
	}

	private _getHtmlForWebview(webview: vscode.Webview, traceId: string) {
		// Local path to main script run in the webview
		const scriptPathOnDisk = vscode.Uri.file(
			path.join(this._extensionPath, 'media', 'main.js')
		);

		// And the uri we use to load this script in the webview
		const scriptUri = webview.asWebviewUri(scriptPathOnDisk);
		const vueScriptUri = webview.asWebviewUri(vscode.Uri.file(
			path.join(this._extensionPath, 'media', 'vue.min.js')
		))
		const cssUri = webview.asWebviewUri(vscode.Uri.file(
			path.join(this._extensionPath, 'media', 'styles.css')
		))
		const bootstrapCssUri = webview.asWebviewUri(vscode.Uri.file(
			path.join(this._extensionPath, 'media', 'bootstrap.min.css')
		))
		const axiosScriptUri = webview.asWebviewUri(vscode.Uri.file(
			path.join(this._extensionPath, 'media', 'axios.min.js')
		))

		console.log(`vueScriptUri`, vueScriptUri)
		console.log(`scriptUri`, scriptUri)
		console.log(`cssUri`, cssUri)

		// for dev

		// Use a nonce to whitelist which scripts can be run
		const nonce = getNonce();

		return `<!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <!-- <meta http-equiv="Content-Security-Policy" content="default-src 'none'; img-src ${webview.cspSource} https:; script-src 'nonce-${nonce}';"> -->
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
				<title>Trace View</title>
				<link href="${bootstrapCssUri}" rel="stylesheet">
				<link rel="stylesheet" nonce="${nonce}" href="${cssUri}" />
				<script src="${axiosScriptUri}"></script>
				<script src="${vueScriptUri}"></script>
            </head>
			<body>
				<div class="container" id="app">
					<h1>Trace</h1>
					<div class="alert alert-primary" role="alert">
					${traceId}
					</div>
					<div class="card">
						<div class="card-body">
							<h5 class="card-title">Spans</h5>
							<div>
								<span class="badge badge-warning" style="margin-right: 10px;"
								 v-for="(span, index) in traceSpans" :key="index">
									 {{span.spanId}}
									 <br/>
									 {{span.classname}}
								</span>
							</div>
						</div>
					</div>

					<div class="card">
						<div class="card-body">
							<h5 class="card-title">Call Traces</h5>
							<div>
								<div class="badge badge-warning" style="margin: 10px; display: block; "
								 v-for="(call, index) in traceCalls" :key="index">
								 {{call.spanId}}
								 <br/>
								 {{call.moduleId}}
								 <br />
								 {{call.filename}}:{{call.line}}
								 </div>
							</div>
						</div>
					</div>
				</div>
				<script>
					var traceId = '${traceId}';
					var rpcEndpoint = '${getRpcEndpoint()}';
				</script>
                <script nonce="${nonce}" src="${scriptUri}"></script>
            </body>
            </html>`;
	}
}

function getNonce() {
	let text = '';
	const possible = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789';
	for (let i = 0; i < 32; i++) {
		text += possible.charAt(Math.floor(Math.random() * possible.length));
	}
	return text;
}