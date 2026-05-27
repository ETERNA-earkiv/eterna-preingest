export interface SchemaField {
  title?: string
  type: string
  format?: string
  enum?: string[]
}

export interface JsonSchema {
  type: string
  properties: Record<string, SchemaField>
  required?: string[]
}

export interface TypeSchema {
  metadataType: string
  label: { sv?: string; en?: string }
  schema: JsonSchema
}

export interface IngestSchema {
  record?: TypeSchema
  item?: TypeSchema
}

export interface SubmitRequest {
  parentId: string
  recordType: 'RECORD' | 'ITEM'
  fields: Record<string, string>
  files?: Array<{ filename: string; base64Data: string }>
}

export interface SubmitResponse {
  jobId: string
}

export interface StatusResponse {
  jobId: string
  status: string
  percentageCompleted: number
  aipId?: string
}

const BASE = '/api'

export async function fetchSchema(): Promise<IngestSchema> {
  const res = await fetch(`${BASE}/schema`)
  if (!res.ok) throw new Error('Kunde inte hämta schema')
  return res.json()
}

export async function submitRecord(request: SubmitRequest): Promise<SubmitResponse> {
  const res = await fetch(`${BASE}/records`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(request)
  })
  if (!res.ok) {
    const err = await res.json().catch(() => ({ error: res.statusText }))
    throw new Error(err.error || 'Inleverans misslyckades')
  }
  return res.json()
}

export async function getStatus(jobId: string): Promise<StatusResponse> {
  const res = await fetch(`${BASE}/records/${jobId}`)
  if (!res.ok) throw new Error('Kunde inte hämta status')
  return res.json()
}
