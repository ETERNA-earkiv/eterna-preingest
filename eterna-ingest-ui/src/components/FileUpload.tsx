import React, { useCallback, useState } from 'react'

interface AttachedFile {
  filename: string
  base64Data: string
  size: number
}

interface Props {
  onFilesChange: (files: AttachedFile[]) => void
}

const MAX_SIZE_MB = 10

export function FileUpload({ onFilesChange }: Props) {
  const [files, setFiles] = useState<AttachedFile[]>([])
  const [error, setError] = useState<string | null>(null)

  const handleDrop = useCallback(
    async (e: React.DragEvent<HTMLDivElement>) => {
      e.preventDefault()
      await processFiles(Array.from(e.dataTransfer.files))
    },
    [files]
  )

  const handleInput = useCallback(
    async (e: React.ChangeEvent<HTMLInputElement>) => {
      if (e.target.files) {
        await processFiles(Array.from(e.target.files))
      }
    },
    [files]
  )

  const processFiles = async (incoming: File[]) => {
    setError(null)
    const oversized = incoming.filter(f => f.size > MAX_SIZE_MB * 1024 * 1024)
    if (oversized.length > 0) {
      setError(`Filer över ${MAX_SIZE_MB} MB stöds inte ännu: ${oversized.map(f => f.name).join(', ')}`)
      return
    }

    const converted = await Promise.all(incoming.map(async f => ({
      filename: f.name,
      base64Data: await toBase64(f),
      size: f.size
    })))

    const updated = [...files, ...converted]
    setFiles(updated)
    onFilesChange(updated)
  }

  const remove = (index: number) => {
    const updated = files.filter((_, i) => i !== index)
    setFiles(updated)
    onFilesChange(updated)
  }

  return (
    <div className="space-y-3">
      <div
        onDrop={handleDrop}
        onDragOver={e => e.preventDefault()}
        className="border-2 border-dashed border-gray-300 rounded-lg p-6 text-center
                   hover:border-blue-400 transition-colors cursor-pointer"
        onClick={() => document.getElementById('file-input')?.click()}
      >
        <p className="text-sm text-gray-500">
          Dra och släpp filer här, eller klicka för att välja
        </p>
        <p className="text-xs text-gray-400 mt-1">Max {MAX_SIZE_MB} MB per fil</p>
        <input
          id="file-input"
          type="file"
          multiple
          className="hidden"
          onChange={handleInput}
        />
      </div>

      {error && <p className="text-xs text-red-600">{error}</p>}

      {files.length > 0 && (
        <ul className="space-y-1">
          {files.map((f, i) => (
            <li key={i} className="flex items-center justify-between bg-gray-50 px-3 py-2 rounded text-sm">
              <span className="truncate">{f.filename}</span>
              <span className="text-gray-400 text-xs mr-2">{(f.size / 1024).toFixed(0)} KB</span>
              <button
                type="button"
                onClick={() => remove(i)}
                className="text-red-400 hover:text-red-600 text-xs"
              >
                Ta bort
              </button>
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}

function toBase64(file: File): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => {
      const result = reader.result as string
      resolve(result.split(',')[1])  // Ta bort "data:...;base64," prefix
    }
    reader.onerror = reject
    reader.readAsDataURL(file)
  })
}
