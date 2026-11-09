const base = new URL('./', document.baseURI)

export async function api(path, { method = 'GET', body, query } = {}) {
  const url = new URL(path, base)
  if (query) {
    url.search = new URLSearchParams(query).toString()
  }

  const options = { method, headers: { Accept: 'application/json' } }
  if (body !== undefined) {
    options.headers['Content-Type'] = 'application/json'
    options.body = JSON.stringify(body)
  }

  const response = await fetch(url, options)
  const result = await response.json().catch(() => null)
  if (!response.ok || result?.code !== 200) {
    const error = new Error(result?.message ?? `请求失败（HTTP ${response.status}）`)
    error.status = response.status
    throw error
  }
  return result.data
}
