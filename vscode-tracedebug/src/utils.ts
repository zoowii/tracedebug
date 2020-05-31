import * as fs from 'fs'

export function readFileText(path): Promise<string> {
	return new Promise((resolve, reject) => {
		fs.readFile(path, (err, data: Buffer) => {
			if(err) {
				reject(err)
				return
			}
			const text = data.toString()
			resolve(text)
		})
	})
}
