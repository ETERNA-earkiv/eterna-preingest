import { useState, useEffect } from 'react'
import { fetchSchema, SchemaDefinition } from '../api/ingestApi'

export function useSchema() {
  const [schema, setSchema] = useState<SchemaDefinition | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    fetchSchema()
      .then(setSchema)
      .catch(e => setError(e.message))
      .finally(() => setLoading(false))
  }, [])

  return { schemaDefinition: schema, loading, error }
}
