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

export interface TypeConfig {
  rootElement: string;
  namespace: string;
  wrapperElement: string;
  metadataType: string
  label: { sv?: string; en?: string }
  schema: JsonSchema
}

export interface RecordGroup {
  records: TypeConfig[];
}

export interface SchemaDefinition {
  recordsList: RecordGroup[];
}

export interface SubmitRequest {
  parentId: string
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
// @ts-ignore
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL;

export async function fetchSchema(): Promise<SchemaDefinition> {
  const res = await fetch(`${BASE}/schema`);
  if (!res.ok) throw new Error('Kunde inte hämta schema');
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
