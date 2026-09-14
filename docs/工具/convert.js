// S73: Markdown -> HTML converter for the course report (uses marked, GFM tables).
// Usage: node convert.js <input.md> <output.html>
const fs = require('fs')
const { marked } = require('marked')

const input = process.argv[2]
const output = process.argv[3]
if (!input || !output) {
  console.error('usage: node convert.js <input.md> <output.html>')
  process.exit(1)
}

marked.setOptions({ gfm: true, breaks: false })

const md = fs.readFileSync(input, 'utf8')
const body = marked.parse(md)

const html = `<!DOCTYPE html>
<html lang="zh-CN">
<head>
<meta charset="UTF-8">
<title>园区安防空地协同巡检集成平台课程设计报告</title>
<style>
body { font-family: "Microsoft YaHei", "SimSun", sans-serif; max-width: 900px; margin: 24px auto; padding: 0 16px; line-height: 1.7; color: #222; }
h1 { font-size: 24px; border-bottom: 2px solid #333; padding-bottom: 8px; }
h2 { font-size: 20px; border-bottom: 1px solid #999; padding-bottom: 4px; margin-top: 32px; }
h3 { font-size: 17px; margin-top: 24px; }
h4 { font-size: 15px; margin-top: 18px; }
table { border-collapse: collapse; margin: 12px 0; font-size: 13px; }
th, td { border: 1px solid #888; padding: 5px 8px; text-align: left; vertical-align: top; }
th { background: #f0f0f0; }
pre { background: #f6f6f6; border: 1px solid #ddd; padding: 10px; overflow-x: auto; font-size: 12px; }
code { background: #f2f2f2; padding: 1px 4px; font-size: 12px; }
blockquote { border-left: 4px solid #ccc; margin: 10px 0; padding: 4px 14px; color: #555; background: #fafafa; }
img { max-width: 100%; }
hr { border: none; border-top: 1px solid #ccc; margin: 24px 0; }
</style>
</head>
<body>
${body}
</body>
</html>
`

fs.writeFileSync(output, html, 'utf8')
console.log('written:', output, '(', fs.statSync(output).size, 'bytes )')
