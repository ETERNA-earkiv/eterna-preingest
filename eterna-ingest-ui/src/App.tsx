import React, { useState } from 'react'
import { useForm } from 'react-hook-form'
import { useSchema } from './hooks/useSchema'
import { useIngest } from './hooks/useIngest'
import { DynamicForm } from './components/DynamicForm'
import { FileUpload } from './components/FileUpload'
import { StatusTracker } from './components/StatusTracker'

interface FormValues {
  parentId: string
  recordType: 'RECORD' | 'ITEM'
  fields: Record<string, string>
}

export default function App() {
  const { schema, loading, error: schemaError } = useSchema()
  const { state, jobId, status, error: ingestError, submit, reset } = useIngest()
  const [attachedFiles, setAttachedFiles] = useState<any[]>([])
  const [activeTab, setActiveTab] = useState<'RECORD' | 'ITEM'>('RECORD')

  const { register, handleSubmit, formState: { errors }, reset: resetForm } = useForm<FormValues>({
    defaultValues: { recordType: 'RECORD', fields: {} }
  })

  const onSubmit = async (data: FormValues) => {
    await submit({
      parentId: data.parentId,
      recordType: activeTab,
      fields: data.fields ?? {},
      files: attachedFiles
    })
  }

  const handleReset = () => {
    reset()
    resetForm()
    setAttachedFiles([])
  }

  if (loading) return (
    <div className="min-h-screen flex items-center justify-center text-gray-500">
      Laddar schema…
    </div>
  )

  if (schemaError) return (
    <div className="min-h-screen flex items-center justify-center">
      <div className="text-red-600 text-center">
        <p className="font-medium">Kunde inte ladda schema</p>
        <p className="text-sm mt-1">{schemaError}</p>
      </div>
    </div>
  )

  const activeSchema = activeTab === 'RECORD' ? schema?.record : schema?.item
  const typeLabel = activeSchema?.label?.sv ?? activeTab

  return (
    <div className="min-h-screen bg-gray-50">
      <header className="bg-white border-b px-6 py-4 flex items-center justify-between">
        <h1 className="text-lg font-semibold text-gray-900">ETERNA Inleverans</h1>
        <span className="text-xs text-gray-400">eterna-ingest-service</span>
      </header>

      <main className="max-w-2xl mx-auto px-4 py-8">
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
          {/* Parent AIP */}
          <div>
            <label className="block text-sm font-medium text-gray-700">
              Mål-AIP i ETERNA <span className="text-red-500">*</span>
            </label>
            <input
              type="text"
              {...register('parentId', { required: 'Mål-AIP är obligatoriskt' })}
              placeholder="xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"
              className={`mt-1 block w-full rounded border px-3 py-2 text-sm
                ${errors.parentId ? 'border-red-500' : 'border-gray-300'}
                focus:outline-none focus:ring-2 focus:ring-blue-500 font-mono`}
            />
            {errors.parentId && (
              <p className="mt-1 text-xs text-red-600">{errors.parentId.message}</p>
            )}
          </div>

          {/* Typväljare (flikar) */}
          {schema?.record && schema?.item && (
            <div className="flex gap-2 border-b">
              {(['RECORD', 'ITEM'] as const).map(tab => {
                const s = tab === 'RECORD' ? schema.record : schema.item
                const label = s?.label?.sv ?? tab
                return (
                  <button
                    key={tab}
                    type="button"
                    onClick={() => setActiveTab(tab)}
                    className={`px-4 py-2 text-sm font-medium border-b-2 -mb-px
                      ${activeTab === tab
                        ? 'border-blue-500 text-blue-600'
                        : 'border-transparent text-gray-500 hover:text-gray-700'}`}
                  >
                    {label}
                  </button>
                )
              })}
            </div>
          )}

          {/* Dynamiska fält */}
          {activeSchema ? (
            <section>
              <h2 className="text-sm font-semibold text-gray-700 mb-3">{typeLabel}</h2>
              <DynamicForm
                schema={activeSchema.schema}
                register={register}
                errors={errors}
                prefix="fields"
              />
            </section>
          ) : (
            <p className="text-sm text-gray-500">Inga fält definierade för denna typ</p>
          )}

          {/* Bifogade filer */}
          <section>
            <h2 className="text-sm font-semibold text-gray-700 mb-3">Bifogade filer</h2>
            <FileUpload onFilesChange={setAttachedFiles} />
          </section>

          {/* Skicka-knapp */}
          <button
            type="submit"
            disabled={state === 'submitting' || state === 'polling'}
            className="w-full bg-blue-600 text-white py-2 px-4 rounded text-sm font-medium
                       hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
          >
            {state === 'submitting' || state === 'polling' ? 'Behandlas…' : 'Skicka till ETERNA'}
          </button>
        </form>

        <StatusTracker
          jobId={jobId}
          status={status}
          state={state}
          error={ingestError}
          onReset={handleReset}
        />
      </main>
    </div>
  )
}
