import { useState, useCallback } from 'react'
import { submitRecord, getStatus, SubmitRequest, StatusResponse } from '../api/ingestApi'

type IngestState = 'idle' | 'submitting' | 'polling' | 'done' | 'error'

export function useIngest() {
  const [state, setState] = useState<IngestState>('idle')
  const [jobId, setJobId] = useState<string | null>(null)
  const [status, setStatus] = useState<StatusResponse | null>(null)
  const [error, setError] = useState<string | null>(null)

  const submit = useCallback(async (request: SubmitRequest) => {
    setState('submitting')
    setError(null)
    try {
      const response = await submitRecord(request)
      setJobId(response.jobId)
      setState('polling')
      await poll(response.jobId)
    } catch (e: any) {
      setError(e.message)
      setState('error')
    }
  }, [])

  const poll = useCallback(async (id: string) => {
    const TERMINAL = ['COMPLETED', 'FAILED', 'FAILED_DURING_INGEST',
                      'COMPLETED_WITH_PROBLEMS', 'STOPPED']
    const MAX_POLLS = 360  // max 30 min med 5s intervall
    let count = 0

    const tick = async (): Promise<void> => {
      if (count++ > MAX_POLLS) {
        setError('Timeout: ingestionen tog för lång tid')
        setState('error')
        return
      }
      const s = await getStatus(id)
      setStatus(s)
      if (TERMINAL.includes(s.status)) {
        setState('done')
        return
      }
      await new Promise(r => setTimeout(r, 5000))
      return tick()
    }

    try {
      await tick()
    } catch (e: any) {
      setError(e.message)
      setState('error')
    }
  }, [])

  const reset = useCallback(() => {
    setState('idle')
    setJobId(null)
    setStatus(null)
    setError(null)
  }, [])

  return { state, jobId, status, error, submit, reset }
}
