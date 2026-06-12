import http from 'node:http'
import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const rootDir = path.resolve(__dirname, '..')
const distDir = path.join(rootDir, 'dist')
const port = Number(process.env.PORT || 5173)
const backendOrigin = process.env.BACKEND_ORIGIN || 'http://localhost:8002'

const mimeTypes = {
  '.css': 'text/css; charset=utf-8',
  '.html': 'text/html; charset=utf-8',
  '.ico': 'image/x-icon',
  '.js': 'text/javascript; charset=utf-8',
  '.json': 'application/json; charset=utf-8',
  '.png': 'image/png',
  '.svg': 'image/svg+xml',
  '.webp': 'image/webp'
}

function serveStatic(request, response) {
  const url = new URL(request.url, `http://${request.headers.host}`)
  const safePath = path.normalize(decodeURIComponent(url.pathname)).replace(/^(\.\.[/\\])+/, '')
  let filePath = path.join(distDir, safePath)

  if (!filePath.startsWith(distDir)) {
    response.writeHead(403)
    response.end('Forbidden')
    return
  }

  if (!path.extname(filePath)) {
    filePath = path.join(distDir, 'index.html')
  }

  fs.readFile(filePath, (error, content) => {
    if (error) {
      fs.readFile(path.join(distDir, 'index.html'), (fallbackError, fallbackContent) => {
        if (fallbackError) {
          response.writeHead(404)
          response.end('Not found')
          return
        }
        response.writeHead(200, { 'Content-Type': mimeTypes['.html'] })
        response.end(fallbackContent)
      })
      return
    }

    response.writeHead(200, {
      'Content-Type': mimeTypes[path.extname(filePath)] || 'application/octet-stream'
    })
    response.end(content)
  })
}

function proxyToBackend(request, response) {
  const target = new URL(request.url, backendOrigin)
  const proxyRequest = http.request(
    target,
    {
      method: request.method,
      headers: {
        ...request.headers,
        host: target.host
      }
    },
    (proxyResponse) => {
      response.writeHead(proxyResponse.statusCode || 500, proxyResponse.headers)
      proxyResponse.pipe(response)
    }
  )

  proxyRequest.on('error', (error) => {
    response.writeHead(502, { 'Content-Type': 'application/json; charset=utf-8' })
    response.end(JSON.stringify({ message: `后端代理失败：${error.message}` }))
  })

  request.pipe(proxyRequest)
}

http
  .createServer((request, response) => {
    if (request.url.startsWith('/api') || request.url.startsWith('/files')) {
      proxyToBackend(request, response)
      return
    }
    serveStatic(request, response)
  })
  .listen(port, '127.0.0.1', () => {
    console.log(`Frontend preview server running at http://127.0.0.1:${port}/`)
  })
