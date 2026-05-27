import { useState, useEffect } from 'react'
import { fetchSchema, IngestSchema } from '../api/ingestApi'

export function useSchema() {
  const [schema, setSchema] = useState<IngestSchema | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    fetchSchema()
      .then(setSchema)
      .catch(e => setError(e.message))
      .finally(() => setLoading(false))
  }, [])

  return { schema, loading, error }
}
