import express from 'express'
import bodyParser from 'body-parser'
import { Ableron } from '@ableron/ableron'

const app = express()
const port = 8080
const ableron = new Ableron({
  requestHeadersForwardVary: ['Accept-Language']
}, console)

app.use(bodyParser.text({ type: 'text/*', limit: '5MB' }))

app.post('/verify', async (req, res) => {
  const transclusionResult = await ableron.resolveIncludes(req.body, req.headers)
  transclusionResult.getResponseHeadersToForward().forEach((headerValue, headerName) => res.set(headerName, headerValue))
  res
    .set('Content-Type', 'text/html; charset=utf-8')
    .set('Cache-Control', transclusionResult.calculateCacheControlHeaderValue(600))
    .status(transclusionResult.getStatusCodeOverride() || 200)
    .send(transclusionResult.getContent())
})

app.listen(port, () => {
  console.log(`Listening on port ${port}`)
})
