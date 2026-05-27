import React from 'react'
import { StatusResponse } from '../api/ingestApi'

interface Props {
  jobId: string | null
  status: StatusResponse | null
  state: 'idle' | 'submitting' | 'polling' | 'done' | 'error'
  error: string | null
  onReset: () => void
}

export function StatusTracker({ jobId, status, state, error, onReset }: Props) {
  if (state === 'idle') return null

  const isSuccess = state === 'done' &&
    (status?.status === 'COMPLETED' || status?.status === 'COMPLETED_WITH_PROBLEMS')

  const isFailure = state === 'error' ||
    (state === 'done' && !isSuccess)

  return (
    <div className="mt-6 rounded-lg border p-4 space-y-3">
      <div className="flex items-center gap-2">
        {(state === 'submitting' || state === 'polling') && (
          <Spinner />
        )}
        <span className="font-medium text-sm">
          {state === 'submitting' && 'Skickar inleverans…'}
          {state === 'polling' && 'Behandlas i ETERNA…'}
          {isSuccess && '✓ Arkivering klar'}
          {isFailure && '✗ Arkivering misslyckades'}
        </span>
      </div>

      {status && state === 'polling' && (
        <div>
          <div className="h-2 bg-gray-200 rounded-full overflow-hidden">
            <div
              className="h-full bg-blue-500 transition-all duration-500"
              style={{ width: `${status.percentageCompleted ?? 0}%` }}
            />
          </div>
          <p className="text-xs text-gray-500 mt-1">
            {status.percentageCompleted?.toFixed(0) ?? 0}% — {status.status}
          </p>
        </div>
      )}

      {isSuccess && (
        <div className="text-sm space-y-1">
          <p className="text-green-700">
            <strong>Jobb-ID:</strong> {jobId}
          </p>
          {status?.aipId && (
            <p className="text-green-700">
              <strong>AIP-ID:</strong> {status.aipId}
            </p>
          )}
          {status?.status === 'COMPLETED_WITH_PROBLEMS' && (
            <p className="text-yellow-600 text-xs">Arkiverat med varningar — kontrollera ETERNA</p>
          )}
        </div>
      )}

      {isFailure && error && (
        <p className="text-sm text-red-600">{error}</p>
      )}

      {(state === 'done' || state === 'error') && (
        <button
          onClick={onReset}
          className="mt-2 text-sm text-blue-600 hover:underline"
        >
          Ny inleverans
        </button>
      )}
    </div>
  )
}

function Spinner() {
  return (
    <svg className="animate-spin h-4 w-4 text-blue-500" viewBox="0 0 24 24" fill="none">
      <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"/>
      <path className="opacity-75" fill="currentColor"
        d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"/>
    </svg>
  )
}
