import React from 'react'
import { UseFormRegister, FieldErrors } from 'react-hook-form'
import {JsonSchema, RecordGroup, SchemaField} from '../api/ingestApi'

interface Props {
  recordGroup: RecordGroup
  register: UseFormRegister<any>
  errors: FieldErrors
  prefix?: string
}

export function DynamicForm({ recordGroup, register, errors, prefix = 'fieldsMap' }: Props) {
  return (
    <div className="space-y-4">
      {Object.entries(recordGroup).map(([index, typeConfig]) => (
          <div key={index}>
            <h3>{typeConfig.label.sv}</h3>
            {Object.entries(typeConfig.schema.properties).map(([name, field]) => (
                <FieldInput
                    key={typeConfig.metadataType + '.' + name}
                    name={name}
                    field={field}
                    required={typeConfig.schema.required ? typeConfig.schema.required.includes(name) : false}
                    register={register}
                    error={(errors as any)?.[prefix]?.[typeConfig.metadataType]?.[name]}
                    prefix={prefix + '.' + typeConfig.metadataType}
                />
            ))}
          </div>
      ))}
    </div>
  )
}

function FieldInput({
  name, field, required, register, error, prefix
}: {
  name: string
  field: SchemaField
  required: boolean
  register: UseFormRegister<any>
  error: any
  prefix: string
}) {
  const fieldPath = `${prefix}.${name}`
  const label = field.title ?? name
  const inputClass = `mt-1 block w-full rounded border px-3 py-2 text-sm
    ${error ? 'border-red-500' : 'border-gray-300'} focus:outline-none focus:ring-2 focus:ring-blue-500`

  return (
    <div>
      <label className="block text-sm font-medium text-gray-700">
        {label}{required && <span className="text-red-500 ml-1">*</span>}
      </label>

      {field.enum ? (
        <select
          {...register(fieldPath, { required: required && `${label} är obligatoriskt` })}
          className={inputClass}
        >
          <option value="">— välj —</option>
          {field.enum.map(v => <option key={v} value={v}>{v}</option>)}
        </select>
      ) : field.type === 'boolean' ? (
        <input
          type="checkbox"
          {...register(fieldPath)}
          className="mt-1 h-4 w-4 rounded border-gray-300"
        />
      ) : (
        <input
          type={field.format === 'date' ? 'date'
               : field.format === 'date-time' ? 'datetime-local'
               : field.type === 'integer' || field.type === 'number' ? 'number'
               : 'text'}
          {...register(fieldPath, {
            required: required && `${label} är obligatoriskt`,
            valueAsNumber: field.type === 'integer' || field.type === 'number'
          })}
          className={inputClass}
          placeholder={label}
        />
      )}

      {error && (
        <p className="mt-1 text-xs text-red-600">{error.message as string}</p>
      )}
    </div>
  )
}
